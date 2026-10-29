# Authentication & Authorization Flow

## Overview

The system uses **Auth0** to handle authentication (who you are) and a custom **Role-Based Access Control (RBAC)** layer to handle authorization (what you can do). Every request goes through two gates before reaching any business logic.

```
Client Request
     │
     ▼
[1] Auth0 JWT Filter       → validates the token and extracts the identity
     │
     ▼
[2] @PreAuthorize check    → calls SecurityService to evaluate permission
     │
     ▼
Controller / Use Case
```

---

## Step 1 — Authentication (Spring Security OAuth2 Resource Server)

Every request must carry a valid **Bearer token** in the `Authorization` header. Auth0 access tokens are standard JWTs, validated by Spring Security's resource server (`spring-boot-starter-security-oauth2-resource-server`, enabled in `SecurityConfig` with `.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`) before the request reaches Spring MVC.

What it validates, from `application.properties`:
- **Signature:** against the keys published at the issuer's JWKS (`https://financial-plan.us.auth0.com/.well-known/jwks.json`, discovered from the issuer on the first request)
- **Expiry** (`exp`/`nbf`)
- **Issuer:** `spring.security.oauth2.resourceserver.jwt.issuer-uri` = `${AUTH0_ISSUER_URI:https://financial-plan.us.auth0.com/}` (the trailing slash is part of Auth0's issuer)
- **Audience:** `spring.security.oauth2.resourceserver.jwt.audiences` = `${AUTH0_AUDIENCE:https://financial-plan-api}`, so tokens issued for other APIs of the same tenant are rejected

The resulting `Authentication` is a `JwtAuthenticationToken` whose `getName()` is the `sub` claim (e.g. `auth0|abc123`). `JwtValidationSpec` covers these rules (valid token, wrong audience, wrong issuer, expired, unknown key) against the real properties.

DPoP-bound tokens are not supported: the Nuxt proxy sends plain `Authorization: Bearer` tokens.

The `sub` claim is the unique identifier that Auth0 assigns to each user. It is used throughout the system to link an Auth0 identity to a local `User` record.

> If the token is missing, expired, tampered with, or issued for another issuer/audience, the resource server rejects the request with **401 Unauthorized** before it reaches any controller.

All routes require authentication — configured in `SecurityConfig`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/**").authenticated()
)
```

---

## Step 2 — Authorization (@PreAuthorize + SecurityService)

Passing JWT validation only proves *who* the caller is. It does not grant access to any endpoint. Each controller method carries a `@PreAuthorize` annotation that calls one of the three methods in `SecurityService`.

> If any method returns `false`, Spring throws `AuthorizationDeniedException`, which `GlobalHandlerException` maps to **403 Forbidden**.

### Three authorization strategies

| Method | Used when | Space scope |
|---|---|---|
| `userHasPermissionForURL` | Space is sent via `X-Space-Id` header (e.g. GroupMenu, EndpointPermission controllers) | Single space from header |
| `userHasPermissionInSpace` | Space ID is available in the URL path variable (e.g. `/spaces/{id}/...`) | Single space from path |
| `userHasPermissionForRole` | Space must be derived from a role ID (e.g. `/roles/{id}`) | Single space derived from role |

---

## How `userHasPermissionForURL` Works

Called on endpoints where the space context is provided via the `X-Space-Id` request header.

```
authentication.getName()  →  auth0Sub (the JWT sub claim)
        │
        ▼
userRepository.findByAuth0Sub(auth0Sub)
        │
        ├─ user not found  →  deny (false)
        │
        ▼
isInternalManagementRequest(method, path)?
        │
        ├─ yes  →  user.isMasterAdmin()  (no space check needed)
        │
        ▼
request.getHeader("X-Space-Id")
        │
        ├─ header missing  →  deny (false)
        │
        ▼
spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
        │
        ├─ user not a member of that space  →  deny (false)
        │
        ▼
roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
    Set.of(membership.getRole().getId()), API)
        │
        ▼
allowedPermissions.anyMatch(p -> p.matchesRequest(method, path))
        │
        ├─ no match  →  deny (false)
        └─ match found  →  allow (true)
```

**Important:** only the role the user holds in the space indicated by `X-Space-Id` is considered. A role the user holds in a different space does not grant access here. A header that is missing or not a number denies access (`false`) instead of raising an error.

### The header is the only source of the Space for data endpoints

Validating membership of the `X-Space-Id` Space is only half of the guarantee: the use case must then operate **on that same Space**. Data controllers (bank accounts, categories/subcategories, credit cards, credit card transactions and recurrings, invoices, bills and bill instances, transactions, reports) therefore receive the Space with `@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId` and pass it to the service; they no longer accept `spaceId` as a query param or in a request body (report filters keep the field, but `scopedTo(spaceId)` overwrites it with the header value).

Every service that receives an entity id (path variable or body reference such as `bankAccountId`, `categoryId`, `subCategoryId`, `creditCardId`, `installmentGroupId`) loads the entity and checks `belongsToSpace(spaceId)` on the domain model. An entity of another Space is reported exactly like a missing one, so ids from other Spaces cannot be probed: `NotFoundException` (404) when it is the resource addressed by the URL, plain `DomainException` (422) when it is a reference inside the request body. Ownership is resolved through the aggregate: `SubCategory` → `Category`, `Transaction` → `BankAccount`, `CreditCardTransaction`/`CreditCardTransactionRecurring`/`CreditCardInvoicePayment` → `CreditCard`.

The author of a transaction, credit card transaction or recurring is always the authenticated user (`authentication.getName()` → `UserRepository.findByAuth0Sub`), never a `userId` from the body. `POST /users` likewise takes `auth0Sub` from the JWT.

Endpoints that still receive a `spaceId` directly (`/roles?spaceId=`, `/roles/{id}/assign-user/{userId}?spaceId=`, `/spaces/{id}/...`, `/menu-structure`, `/dashboard-widgets`) validate membership of that same id (`userHasPermissionInSpace(#spaceId)` or a membership lookup in the service), so they are not affected.

---

## How `userHasPermissionInSpace` Works

Used on space-scoped endpoints where the space ID is available directly in the URL (e.g. `PUT /spaces/{id}`, `GET /spaces/{id}/members`).

```
spaceId  →  extracted from path variable by the controller
        │
        ▼
spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
        │
        ├─ spaceId is null OR user is not a member  →  deny (false)
        │
        ▼
roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
    Set.of(membership.getRole().getId()), API)
        │
        ▼
allowedPermissions.anyMatch(p -> p.matchesRequest(method, path))
```

---

## How `userCanInviteToSpace` Works

Used by `GET /spaces/{id}/members/search?email=` (the "add member" dialog). It has no `endpoint_permissions` row of its own: it runs the same check as `userHasPermissionInSpace`, but against `POST /spaces/{id}/invites`, so only roles allowed to send invites in that Space can look up an e-mail. The response is `{ "name": "Joã* S****" }` (`User.maskedName()`), with no id or e-mail, and `404` when nobody has that e-mail. This replaced the former `GET /users/search`, which any logged user could call and which returned id, name and e-mail.

**This check is a business rule, not a protection against e-mail enumeration.** Every user creates their own Space during onboarding and becomes its OWNER, with permission to invite, so anyone can sign up and use the search against their own Space. What limits the exposure is the masked response; the `200`/`404` answer still tells whether an e-mail has an account, and only rate limiting (or removing the search) addresses that.

---

## How `userHasPermissionForRole` Works

Used on role management endpoints (e.g. `PUT /roles/{id}`, `DELETE /roles/{id}`) where the URL only carries a role ID but the space must be known to authorize the caller.

```
roleId  →  roleRepository.findById(roleId)
        │
        ├─ role not found  →  deny (false)
        │
        ▼
role.getSpace().getId()  →  delegates to userHasPermissionInSpace(spaceId)
```

---

## Key Concepts

### User ↔ Auth0 Link

The `User` domain model has an `auth0Sub` field (unique, not null) that stores the `sub` claim from Auth0. This is set once when the user is registered:

```
POST /users  { "auth0Sub": "auth0|abc123", "name": "...", ... }
```

Every subsequent request resolves identity via `userRepository.findByAuth0Sub(auth0Sub)`.

### Space & SpaceMember

A `Space` is the top-level organizational unit (e.g. "My Family", "My Company"). A user can belong to **multiple spaces**. Each membership is represented by a `SpaceMember` record that links a `User`, a `Space`, and a `Role`:

```
User ──< SpaceMember >── Space
                │
               Role
```

When assigning a role to a user, the system enforces that the role belongs to the same space as the membership:

```
PUT /spaces/{id}/members/{userId}   { "roleId": 3 }
```

### Role (scoped to a Space)

A `Role` belongs to a specific `Space`. The system prevents a role from one space being assigned to a membership in a different space.

When a `Space` is created, an `OWNER` role is automatically created for it and a `SpaceMember` record linking the creator to that role is saved. This ensures every space always has at least one owner.

### EndpointPermission & RoleEndpointPermission

Each `EndpointPermission` record defines one rule applied to an HTTP endpoint:

| Field | Example | Description |
|---|---|---|
| `endpoint` | `/group-menus.*` | Java regex matched against the request URI |
| `permittedMethods` | `GET,POST` | CSV of allowed HTTP methods |
| `sequence` | `1` | Priority — lower number wins when multiple rules match |
| `type` | `API` | Whether this rule guards a backend endpoint (`API`) or a frontend page (`FRONT_PAGE`) |
| `group` | `INTERNAL_MANAGEMENT` | Optional grouping; the `INTERNAL_MANAGEMENT` group restricts access to master admins only |

Access is granted or denied via **`RoleEndpointPermission`** — a join entity between `Role` and `EndpointPermission` that carries an `ALLOW` or `DENY` type. The repository method `findAllowedEndpointPermissionsByRoleIdsAndType` returns only the `ALLOW` entries for the given role IDs.

---

## Frontend → Backend: X-Space-Id Header

All frontend requests to the backend include the `X-Space-Id` header set to the ID of the space the user is currently working in. This is managed in two places:

- **`plugins/space-header.client.ts`** — wraps the global `$fetch` so every client call to `/api/*` carries `X-Space-Id` from `useSpaceStore().activeSpace`, i.e. the Space **this tab** is showing.
- **`server/utils/backendHeaders.ts`** (`buildBackendHeaders`) — Nitro utility used by all server routes. It forwards the `X-Space-Id` sent by the tab and falls back to the `activeSpaceId` cookie only when the request has none (SSR). Non-numeric values are dropped.

The active space ID is persisted in the `activeSpaceId` cookie by `useSpaceStore.setActiveSpace()` whenever the user switches spaces. The cookie is shared by every tab, which is why the per-tab header takes precedence: otherwise a tab still showing Space A would read and write Space B after another tab switched to it. Since the backend takes the Space exclusively from this header, the frontend no longer sends `spaceId` in query params or bodies of data endpoints.

---

## Frontend Menu Filtering (GET /menu-structure)

The `/menu-structure` endpoint does not use `@PreAuthorize`. Instead it returns a menu tailored to the caller's roles — pages the user cannot access simply do not appear.

```
authentication.getName()  →  auth0Sub
        │
        ▼
Find user  →  collect all SpaceMembers for that user
        │
        ├─ no memberships  →  return empty list
        │
        ▼
Collect all role names from all memberships (union across all spaces)
        │
        ▼
Find all EndpointPermission where type = FRONT_PAGE
  AND the role has an ALLOW RoleEndpointPermission for it
        │
        ▼
Load all GroupMenus with their children
        │
        ▼
For each child: keep it only if its endpoint
  matches any permitted FRONT_PAGE rule (regex)
        │
        ▼
Discard groups with zero remaining children
        │
        ▼
Return filtered GroupMenuStructureDto list
```

---

## Error Responses

| Situation | HTTP Status | Handled by |
|---|---|---|
| Missing or invalid JWT | 401 Unauthorized | Spring Security resource server |
| Missing `X-Space-Id` header / not a member / rule denies | 403 Forbidden | GlobalHandlerException |
| Resource addressed by the URL does not exist or belongs to another Space (`NotFoundException`) | 404 Not Found | GlobalHandlerException |
| Business rule violated, or an id in the body is invalid / from another Space | 422 Unprocessable Entity | GlobalHandlerException |
| Concurrent edit conflict | 423 Locked | GlobalHandlerException |

---

## Setup Sequence (First-Time Configuration)

To make a protected endpoint accessible, follow this order:

1. **Create a User** with `auth0Sub` matching their Auth0 account
2. **Create a Space** — this automatically creates an `OWNER` role and a `SpaceMember` linking the creator to it
3. *(Optional)* **Create additional Roles** linked to the space (e.g. `ADMIN`, `MEMBER`)
4. *(Optional)* **Add more members** via `PUT /spaces/{id}/members/{userId}` with the desired `roleId`
5. **Create EndpointPermission records** (type `API`) with the routes that should be controllable
6. **Create RoleEndpointPermission records** linking those permissions to roles with `ALLOW` or `DENY`
7. *(Optional)* **Create GroupMenu / GroupMenuChildren** and **EndpointPermission records** (type `FRONT_PAGE`) with matching `RoleEndpointPermission` entries to control which pages appear in the menu for each role

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

## Step 1 — Authentication (Auth0 JWT Filter)

Every request must carry a valid **Bearer token** in the `Authorization` header. The `Auth0AuthenticationFilter` (provided by `com.auth0:auth0-springboot-api`) intercepts all requests before they reach Spring MVC.

What it does:
- Validates the JWT signature against the Auth0 public keys
- Extracts the `sub` claim from the token (e.g. `auth0|abc123`)
- Populates the Spring `SecurityContext` with an `Authentication` object

The `sub` claim is the unique identifier that Auth0 assigns to each user. It is used throughout the system to link an Auth0 identity to a local `User` record.

> If the token is missing, expired, or tampered with, the filter rejects the request with **401 Unauthorized** before it reaches any controller.

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

**Important:** only the role the user holds in the space indicated by `X-Space-Id` is considered. A role the user holds in a different space does not grant access here.

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

- **`utils/api.ts`** (`$api` fetch instance) — reads `activeSpaceId` from the cookie and adds the header for legacy client-side calls.
- **`server/utils/backendHeaders.ts`** (`buildBackendHeaders`) — Nitro utility that reads `activeSpaceId` from the cookie server-side and returns the headers object used by all Nitro server routes.

The active space ID is persisted in the `activeSpaceId` cookie by `useSpaceStore.setActiveSpace()` whenever the user switches spaces.

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
| Missing or invalid JWT | 401 Unauthorized | Auth0AuthenticationFilter |
| Missing `X-Space-Id` header / not a member / rule denies | 403 Forbidden | GlobalHandlerException |
| Business rule violated | 422 Unprocessable Entity | GlobalHandlerException |
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

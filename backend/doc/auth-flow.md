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

Passing JWT validation only proves *who* the caller is. It does not grant access to any endpoint. Each controller method carries this annotation:

```java
@PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
```

Spring evaluates this SpEL expression before executing the method. It calls `SecurityService.userHasPermissionForURL()`, which performs the permission check described below.

> If the method returns `false`, Spring throws `AuthorizationDeniedException`, which `GlobalHandlerException` maps to **403 Forbidden**.

---

## How SecurityService Evaluates a Request

```
authentication.getName()  →  auth0Sub (the JWT sub claim)
        │
        ▼
userRepository.findByAuth0Sub(auth0Sub)
        │
        ├─ user not found  →  deny (false)
        ├─ user has no Role assigned  →  deny (false)
        │
        ▼
endpointPermissionRepository.findByType(API)   ← ordered by sequence asc
        │
        ▼
Find first EndpointPermission where:
  - HTTP method is in permittedMethods (CSV, case-insensitive)
  - request path matches endpoint (treated as a Java regex)
        │
        ├─ no match found  →  deny (false)   ← secure by default
        │
        ▼
Check if user's role name is in permittedRoles (CSV)
        │
        ├─ role not listed  →  deny (false)
        └─ role listed      →  allow (true)
```

The **sequence field controls priority**. When multiple `EndpointPermission` records match the same request, the one with the lowest sequence number wins. This allows you to create a restrictive rule with sequence=1 that overrides a broader rule with sequence=2.

---

## Key Concepts

### User ↔ Auth0 Link

The `User` domain model has an `auth0Sub` field (unique, not null) that stores the `sub` claim from Auth0. This is set once when the user is registered:

```
POST /users  { "auth0Sub": "auth0|abc123", "name": "...", ... }
```

Every subsequent request resolves identity via `userRepository.findByAuth0Sub(auth0Sub)`.

### Role (scoped to a Family)

A `Role` belongs to a specific `Family`. When assigning a role to a user, the system enforces that the role and the user belong to the same family:

```
PUT /roles/{roleId}/assign-user/{userId}
```

This prevents a role from one family being assigned to a user in another family.

### EndpointPermission

Each `EndpointPermission` record defines one rule. It has two types:

| Type | Purpose |
|---|---|
| `API` | Controls access to backend REST endpoints (evaluated by `SecurityService`) |
| `FRONT_PAGE` | Controls which pages appear in the frontend navigation menu |

Key fields:

| Field | Example | Description |
|---|---|---|
| `endpoint` | `/roles.*` | Java regex matched against the request URI |
| `permittedMethods` | `GET,POST` | CSV of allowed HTTP methods |
| `permittedRoles` | `ADMIN,MANAGER` | CSV of role names that can access this rule |
| `sequence` | `1` | Priority — lower number wins when multiple rules match |
| `type` | `API` | Whether this rule guards a backend endpoint or a frontend page |

---

## Frontend Menu Filtering (GET /menu-structure)

The `/menu-structure` endpoint does not use `@PreAuthorize`. Instead it returns a menu tailored to the caller's role — pages the user cannot access simply do not appear.

```
authentication.getName()  →  auth0Sub
        │
        ▼
Find user  →  get role name
        │
        ▼
Find all EndpointPermission where type = FRONT_PAGE
  AND permittedRoles contains the role name
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

This means the same `EndpointPermission` table drives both backend security and frontend navigation visibility, keeping the access model in a single place.

---

## Error Responses

| Situation | HTTP Status | Handled by |
|---|---|---|
| Missing or invalid JWT | 401 Unauthorized | Auth0AuthenticationFilter |
| User has no role / rule denies access | 403 Forbidden | GlobalHandlerException |
| Business rule violated | 422 Unprocessable Entity | GlobalHandlerException |
| Concurrent edit conflict | 423 Locked | GlobalHandlerException |

---

## Setup Sequence (First-Time Configuration)

To make a protected endpoint accessible, follow this order:

1. **Create a Family** — all users and roles are scoped to it
2. **Create a User** with `auth0Sub` matching their Auth0 account
3. **Create a Role** linked to the family (e.g. `ADMIN`)
4. **Assign the Role to the User** via `PUT /roles/{id}/assign-user/{userId}`
5. **Create EndpointPermission records** (type `API`) with the routes and roles that should have access
6. *(Optional)* **Create GroupMenu / GroupMenuChildren** and **EndpointPermission records** (type `FRONT_PAGE`) to control which pages appear in the menu for each role

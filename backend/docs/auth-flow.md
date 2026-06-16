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
        │
        ▼
spaceMemberRepository.findByUserId(userId)   ← all space memberships for the user
        │
        ├─ no memberships  →  deny (false)
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
Check if ANY of the user's role names (across all spaces) is in permittedRoles (CSV)
        │
        ├─ no role listed  →  deny (false)
        └─ at least one listed  →  allow (true)
```

The **sequence field controls priority**. When multiple `EndpointPermission` records match the same request, the one with the lowest sequence number wins. This allows you to create a restrictive rule with sequence=1 that overrides a broader rule with sequence=2.

A user who belongs to multiple spaces may carry different roles in each. Access is granted if the matched rule permits **any** of the user's roles across all their space memberships.

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
POST /spaces/{spaceId}/members/{userId}   { "roleId": 3 }
```

### Role (scoped to a Space)

A `Role` belongs to a specific `Space`. The system prevents a role from one space being assigned to a membership in a different space.

When a `Space` is created, an `OWNER` role is automatically created for it and a `SpaceMember` record linking the creator to that role is saved. This ensures every space always has at least one owner.

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
  AND permittedRoles contains at least one of the user's role names
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
| User has no space memberships / rule denies access | 403 Forbidden | GlobalHandlerException |
| Business rule violated | 422 Unprocessable Entity | GlobalHandlerException |
| Concurrent edit conflict | 423 Locked | GlobalHandlerException |

---

## Setup Sequence (First-Time Configuration)

To make a protected endpoint accessible, follow this order:

1. **Create a User** with `auth0Sub` matching their Auth0 account
2. **Create a Space** — this automatically creates an `OWNER` role and a `SpaceMember` linking the creator to it
3. *(Optional)* **Create additional Roles** linked to the space (e.g. `ADMIN`, `MEMBER`)
4. *(Optional)* **Add more members** via `POST /spaces/{id}/members/{userId}` with the desired `roleId`
5. **Create EndpointPermission records** (type `API`) with the routes and roles that should have access
6. *(Optional)* **Create GroupMenu / GroupMenuChildren** and **EndpointPermission records** (type `FRONT_PAGE`) to control which pages appear in the menu for each role

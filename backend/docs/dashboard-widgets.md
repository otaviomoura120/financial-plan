# Dashboard Widget Permissions

`GET /dashboard-widgets?spaceId=` — returns `List<String>`, the widget keys the caller's role is
`ALLOW`ed to see on the frontend home page. Implemented by `GetDashboardWidgetPermissionsService`,
exposed by `DashboardWidgetController`. No `@PreAuthorize` — same posture as `MenuStructureController`
(any authenticated user, filtering happens inside the service via space-membership resolution, not
via a URL-matching permission check).

## Why a third `EndpointPermissionType`

`API` gates a real HTTP route and `FRONT_PAGE` gates a sidebar navigation item — both match `endpoint`
as a **regex** against something real (a request path, a `GroupMenuChildren.endpoint`). The home
dashboard's cards aren't routes or menu items — they're sections of a single page — so neither fits.
`WIDGET` (`domain/enums/EndpointPermissionType.java`) was added instead of shoehorning them into either
existing type. For `WIDGET` rows, `endpoint` is a **plain string key** (e.g. `dashboard:due-this-week`),
never evaluated with `.matches()` — it's compared for exact membership only, by the frontend, against
constants in `frontend/utils/dashboardWidgets.ts`. There is no `GroupMenuChildren` counterpart for
widgets; the resolved response is a bare list of allowed keys, not a tree.

## The 5 widget permissions (group `Dashboard`)

| `name` | `endpoint` (key) | Gates |
|---|---|---|
| Totais Sumarizados | `dashboard:summary-tiles` | The 4 hero stat tiles as a row |
| Contas a Pagar Vencendo | `dashboard:due-this-week` | Bills+invoices-due-this-week card, and the "A Pagar" tile |
| Saldo Atual das Contas | `dashboard:account-balances` | Account balances card, and the "Saldo Total" tile |
| Gastos por Categoria e Subcategoria | `dashboard:category-spending` | Category spending donut card, and the "Gasto" tile |
| Total de Gasto em Cada Cartão | `dashboard:credit-card-spending` | Credit card spending card, and the "Faturas" tile |

Frontend gating semantics (not enforced here — this endpoint only resolves the ALLOW set):
`dashboard:summary-tiles` gates the hero row's existence, but each tile inside it is **additionally**
gated by its own underlying widget's key, so a role can see 0-4 tiles even with `summary-tiles`
allowed. See `frontend/docs/dashboard.md`.

## `GetDashboardWidgetPermissionsService`

Mirrors `application/menu/GetMenuStructureService` exactly for the user→space→role resolution chain,
with no menu-tree walk on top:

```java
User user = userRepository.findByAuth0Sub(auth0Sub);              // null → []
SpaceMember membership = spaceMemberRepository
        .findBySpaceIdAndUserId(spaceId, user.getId());           // null → []
List<EndpointPermission> allowed = roleEndpointPermissionRepository
        .findAllowedEndpointPermissionsByRoleIdsAndType(
                Set.of(membership.getRole().getId()), EndpointPermissionType.WIDGET);
return allowed.stream().map(EndpointPermission::getEndpoint).toList();
```

No master-admin special case (unlike `GetMenuStructureService`, which unions in `internal_management`
`FRONT_PAGE` rows for master admins) — there is no internal-management concept for dashboard widgets,
every role (including master admins) only sees what its own `RoleEndpointPermission` rows grant.

## ALLOW/DENY defaults — same mechanism as every other permission type, verified in code

This is the one point worth being explicit about, because it's easy to assume a bespoke rule exists
when it doesn't:

- **New space → OWNER auto-`ALLOW`.** `CreateSpaceService.createOwnerMembership()` builds the new
  space's OWNER role directly (bypassing `CreateRoleService` on purpose) and sets `ALLOW` for **every**
  non-`internal_management` `EndpointPermission` returned by `endpointPermissionRepository.findAll()` —
  it does not filter by `type`. The moment the 5 `WIDGET` rows exist in the table, any newly created
  space's OWNER gets them as `ALLOW` automatically, with zero code change to this service.
- **New role (`POST /roles`) → `DENY` everything.** `CreateRoleService.createDefaultPermissions()`
  does the opposite, unconditionally: every non-`internal_management` `EndpointPermission` gets a
  `DENY` row for the new role, again with no filtering by `type`. An ADMIN/MEMBER role created through
  the app starts with the 5 widgets denied, exactly like any other permission, until a human flips them
  via `PATCH /roles/{id}/permissions/{permissionId}` (the Roles screen).
- Neither service needed to change for `WIDGET` to behave this way — both already iterate
  `EndpointPermissionRepository.findAll()` generically. The only prerequisite is that the 5 rows exist
  in `endpoint_permissions` before a space/role is created.

## Seed / permissions

`seed.sql` section 15 inserts the 5 `WIDGET` `endpoint_permissions` rows and, for parity with already-
seeded dev databases (which have OWNER/ADMIN/MEMBER from earlier sections), idempotent `ALLOW` inserts
for all three — same idiom as section 7 (`/credit-cards`): each widget is a brand-new `name`, so it
needs its own `role_endpoint_permissions` insert per role rather than reusing an existing `ep.name IN
(...)` allow-list join. This section is purely to keep the **seed script** self-consistent; it does not
replace the `CreateSpaceService` mechanism above, which is what actually fires for real new spaces.
`permitted_methods` is `NULL` on these rows (not applicable — nothing here matches an HTTP method).

In a real (non-seeded) environment, the 5 rows can instead be created by an admin through the existing
"Permissões de Endpoint" screen (`POST /endpoint-permissions`, type dropdown now includes "Widget").

## Admin UI reuse

No new admin UI was built. The existing role-permission dialog (`RolePermissionsDialog.vue`, driven by
`GET /roles/{id}/permissions` / `PATCH /roles/{id}/permissions/{permissionId}`) already groups
permissions by `group` and renders a per-row ALLOW/DENY switch — the 5 new rows show up under a
"Dashboard" group with zero structural changes, only a label/color addition for the `WIDGET` type.

Frontend counterpart: `frontend/docs/dashboard.md`.

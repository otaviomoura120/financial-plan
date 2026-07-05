# Payment Methods

Status: **implemented and working** (backend already existed; this doc covers the frontend
piece — Grupo F1 of `backend/IMPLEMENTATION_PLAN.md`). This is the pilot page for the CRUD
pattern used by Bank Accounts / Categories / Transactions (F2-F4): a simple `name` + `active`
entity, one page + one dialog + one set of Nitro proxy routes.

## Backend

`PaymentMethodController` (`/payment-methods`), already implemented with `@PreAuthorize`
(`@securityService.userHasPermissionForURL`). See `backend/src/main/java/.../infrastructure/controller/PaymentMethodController.java`.

| Method | Path | Body / Query | Response |
|---|---|---|---|
| GET | `/payment-methods` | `spaceId` (query) | `PaymentMethodResponse[]` |
| POST | `/payment-methods` | `{ spaceId, name }` | `PaymentMethodResponse` |
| PUT | `/payment-methods/{id}` | `{ version, name }` | `PaymentMethodResponse` |
| DELETE | `/payment-methods/{id}` | — | `204 No Content` |

`PaymentMethodResponse`: `{ id, version, name, active }`.

`DELETE` is a **soft delete** — `DeletePaymentMethodService` calls `PaymentMethod.deactivate()`
and persists it (`active=false`); the row is never removed. `GET` still returns inactive
records, so the frontend must keep showing them (with an "Inativo" status) rather than
filtering them out.

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/payment-methods/index.vue` | Main page — table with search + client-side pagination, create/edit/delete actions |
| `components/dialogs/AddEditPaymentMethodDialog.vue` | Single dialog reused for create and edit (`payment-method` prop null → create mode) |
| `components/dialogs/ConfirmDialog.vue` | Generic confirm dialog, reused as-is before deleting |
| `server/api/payment-methods/index.get.ts` | Proxies `GET /payment-methods?spaceId=` |
| `server/api/payment-methods/index.post.ts` | Proxies `POST /payment-methods` |
| `server/api/payment-methods/[id].put.ts` | Proxies `PUT /payment-methods/{id}` |
| `server/api/payment-methods/[id].delete.ts` | Proxies `DELETE /payment-methods/{id}` |

All Nitro routes follow the exact pattern of `server/api/roles/*`: `useAuth0(event).getAccessToken()`
for the token, `buildBackendHeaders(event, accessToken)` for headers, `config.public.apiBaseUrl`
as `baseURL`.

## Page behavior

- `pages/payment-methods/index.vue` fetches the list on mount and on every `spaceStore.activeSpace`
  change (`watch(..., { immediate: true })`), same as `pages/roles/index.vue`.
- Client-side search (`name`, case-insensitive substring) and pagination (`TablePagination`,
  `itemsPerPage = 10`) — no server-side filtering, matching the Roles page.
- Table columns: **Nome**, **Status** (chip: `success`/"Ativo" or `secondary`/"Inativo"), **Ações**
  (Editar, Excluir).
- "Excluir" is disabled once `active === false` (nothing left to deactivate). On confirm, it
  calls `DELETE` and then flips `active` to `false` **in place** in the local array — the row
  is not removed from the table (see soft-delete note above).
- Errors surface via `useApiError()` + `ApiErrorAlert` (fetch failures) and `useSnackbar()` (the
  delete action's success/error toast), same composables as Roles.

## Form (`AddEditPaymentMethodDialog.vue`)

Single field: **Nome** (`AppTextField`, required). Create sends `{ spaceId: activeSpace.id, name }`;
edit sends `{ version, name }` to `PUT /payment-methods/{id}` (optimistic locking — a stale
`version` triggers a `423 Locked` from the backend, surfaced through `ApiErrorAlert`).

## Navigation

No static navigation edit was needed — the sidebar entry for "Meios de Pagamento" is already
seeded via `group_menus`/`group_menu_children` in `backend/docs/seed.sql` and is driven by
`GET /menu-structure` (`useMenuStore`), gated by the `FRONT_PAGE` `EndpointPermission` for this
route already being `ALLOW` for the user's roles.

## Manual verification

No frontend test suite exists in this project (see `frontend/CLAUDE.md` / the `verify` skill) —
this page was verified manually: `pnpm dev`, open `/payment-methods`, create a payment method,
edit its name, delete it (confirm it turns "Inativo" instead of disappearing), confirm search
and pagination work with more than 10 rows.

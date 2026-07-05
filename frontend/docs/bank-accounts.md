# Bank Accounts

Status: **implemented and working** (backend already existed; this doc covers the frontend
piece — Grupo F2 of `backend/IMPLEMENTATION_PLAN.md`). Same CRUD pattern as
[`payment-methods.md`](./payment-methods.md) (F1, the pilot), with one extra rule: `balance`
is never directly editable — it only changes as a side effect of transactions (see
`backend/IMPLEMENTATION_PLAN.md`, "Spec por entidade" → `BankAccount`).

## Backend

`BankAccountController` (`/bank-accounts`), already implemented with `@PreAuthorize`
(`@securityService.userHasPermissionForURL`). See `backend/src/main/java/.../infrastructure/controller/BankAccountController.java`.

| Method | Path | Body / Query | Response |
|---|---|---|---|
| GET | `/bank-accounts` | `spaceId` (query) | `BankAccountResponse[]` |
| POST | `/bank-accounts` | `{ spaceId, name, bankName, initialBalance }` | `BankAccountResponse` |
| PUT | `/bank-accounts/{id}` | `{ version, name, bankName }` — **no `balance` field** | `BankAccountResponse` |
| DELETE | `/bank-accounts/{id}` | — | `204 No Content` |

`BankAccountResponse`: `{ id, version, spaceId, name, bankName, balance, active, createdDate }`.

Same soft-delete behavior as Payment Methods: `DeleteBankAccountService` calls
`BankAccount.deactivate()` (`active=false`), the row is never removed, and `GET` keeps
returning it.

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/bank-accounts/index.vue` | Main page — table with search (name + bank), client-side pagination, create/edit/delete |
| `components/dialogs/AddEditBankAccountDialog.vue` | Single dialog reused for create and edit |
| `components/dialogs/ConfirmDialog.vue` | Generic confirm dialog, reused as-is before deleting |
| `server/api/bank-accounts/index.get.ts` | Proxies `GET /bank-accounts?spaceId=` |
| `server/api/bank-accounts/index.post.ts` | Proxies `POST /bank-accounts` |
| `server/api/bank-accounts/[id].put.ts` | Proxies `PUT /bank-accounts/{id}` |
| `server/api/bank-accounts/[id].delete.ts` | Proxies `DELETE /bank-accounts/{id}` |

Nitro routes follow the same `useAuth0(event).getAccessToken()` + `buildBackendHeaders` +
`config.public.apiBaseUrl` pattern as `server/api/payment-methods/*` / `server/api/roles/*`.

## Page behavior

- Fetches on mount and on every `spaceStore.activeSpace` change, same as Payment Methods/Roles.
- Client-side search matches `name` **or** `bankName` (case-insensitive substring).
- Table columns: **Nome**, **Banco**, **Saldo** (formatted as BRL via `Intl.NumberFormat`),
  **Status** (chip), **Ações** (Editar, Excluir).
- "Excluir" disabled once `active === false`; on confirm, `active` flips to `false` in place
  in the local array (row stays visible, matching the soft-delete behavior above).

## Form (`AddEditBankAccountDialog.vue`)

- **Create mode**: `Nome`, `Banco`, `Saldo inicial` (`AppTextField type="number"`, sent as
  `initialBalance`).
- **Edit mode**: `Nome`, `Banco` only — the balance field is replaced by a **read-only,
  disabled** field showing the current `balance` with a hint explaining it only changes via
  transactions. No `balance`/`initialBalance` field is ever sent on `PUT`.
- Optimistic locking: `PUT` sends `version`; a stale value triggers `423 Locked`, surfaced via
  `ApiErrorAlert`.

## Navigation

No static navigation edit needed — the sidebar entry ("Contas Bancárias") is already seeded
in `backend/docs/seed.sql` (`group_menus`/`group_menu_children`) and appears once the
`FRONT_PAGE` `EndpointPermission` for `/bank-accounts` is `ALLOW` for the user's roles.

## Manual verification

No frontend test suite exists in this project — verified manually: `pnpm dev`, open
`/bank-accounts`, create an account with an initial balance, confirm the balance shown matches,
edit name/bank (balance field is read-only in edit mode), deactivate it and confirm it stays in
the list marked "Inativo" instead of disappearing.

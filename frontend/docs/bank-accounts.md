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
| PATCH | `/bank-accounts/{id}/status` | `{ active: boolean }` | `BankAccountResponse` |
| DELETE | `/bank-accounts/{id}` | — | `204 No Content` |

`BankAccountResponse`: `{ id, version, spaceId, name, bankName, balance, active, createdDate }`.

Same hard-delete behavior as Payment Methods: `DeleteBankAccountService` checks for linked
`Transaction`s (as origin or destination) via `TransactionRepository.existsByBankAccountId`
and rejects with a `422` if any exist; otherwise the row is removed for good. Activating/
deactivating is a separate, reversible action via `PATCH .../status`
(`UpdateBankAccountStatusService`).

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/bank-accounts/index.vue` | Main page — table with search (name + bank), client-side pagination, create/edit/delete |
| `components/dialogs/AddEditBankAccountDialog.vue` | Single dialog reused for create and edit |
| `components/dialogs/ConfirmDialog.vue` | Generic confirm dialog, reused as-is before deleting |
| `server/api/bank-accounts/index.get.ts` | Proxies `GET /bank-accounts?spaceId=` |
| `server/api/bank-accounts/index.post.ts` | Proxies `POST /bank-accounts` |
| `server/api/bank-accounts/[id].put.ts` | Proxies `PUT /bank-accounts/{id}` |
| `server/api/bank-accounts/[id]/status.patch.ts` | Proxies `PATCH /bank-accounts/{id}/status` |
| `server/api/bank-accounts/[id].delete.ts` | Proxies `DELETE /bank-accounts/{id}` — catches backend errors and re-throws via `createError({statusCode, statusMessage, data})` so the real `DomainException` message reaches the client |

Nitro routes follow the same `useAuth0(event).getAccessToken()` + `buildBackendHeaders` +
`config.public.apiBaseUrl` pattern as `server/api/payment-methods/*` / `server/api/roles/*`.

## Page behavior

- Fetches on mount and on every `spaceStore.activeSpace` change, same as Payment Methods/Roles.
- Client-side search matches `name` **or** `bankName` (case-insensitive substring).
- Table columns: **Nome**, **Banco**, **Saldo** (formatted as BRL via `Intl.NumberFormat`),
  **Status** (chip), **Ações** (Editar, Ativar/Inativar, Excluir).
- "Ativar/Inativar" (toggle icon) is always enabled and flips `active` via `PATCH .../status`,
  with its own confirmation dialog; row updates in place.
- "Excluir" is always enabled and performs a real hard delete (`confirm-color="error"`,
  irreversible warning); on success the row is removed from the local array. A `422` from the
  backend (linked transactions) surfaces the real error message via the snackbar/alert.

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
edit name/bank (balance field is read-only in edit mode), toggle it inactive/active, try
deleting an account used by a transaction (expect the real backend error message), delete one
with no dependencies (row disappears).

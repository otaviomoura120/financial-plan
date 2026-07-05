# Transactions

Status: **implemented and working** (backend already existed; this doc covers the frontend
piece — Grupo F4 of `backend/IMPLEMENTATION_PLAN.md`). This is the main day-to-day page of the
app and the most complex form so far — it depends on F1 (Payment Methods), F2 (Bank Accounts)
and F3 (Categories/SubCategories) to populate its selects.

## Backend

`TransactionController` (`/transactions`), already implemented with `@PreAuthorize`. See
`backend/src/main/java/.../infrastructure/controller/TransactionController.java`.

| Method | Path | Body / Query | Response |
|---|---|---|---|
| GET | `/transactions` | `spaceId` (required), `from`, `to`, `userId`, `bankAccountId`, `categoryId`, `subCategoryId`, `paymentMethodId`, `type` (all optional) | `TransactionResponse[]` |
| POST | `/transactions` | `CreateTransactionRequest` | `TransactionResponse` |
| PUT | `/transactions/{id}` | `UpdateTransactionRequest` | `TransactionResponse` |
| DELETE | `/transactions/{id}` | — | `204 No Content` |

`TransactionResponse`: `{ id, version, type, userId, bankAccountId, destinationBankAccountId,
categoryId, subCategoryId, paymentMethodId, amount, transactionDate, description, createdDate }`
— **flat IDs only**, no nested/denormalized names. The frontend joins against the already-fetched
Bank Accounts/Categories/Payment Methods lists to render human-readable labels.

`type` is one of `INCOME` / `EXPENSE` / `TRANSFER`. Validation branches on it
(`Transaction.validate()` in the domain):
- `TRANSFER`: requires `destinationBankAccountId` (different from `bankAccountId`); `categoryId`/
  `paymentMethodId` are not used.
- `INCOME`/`EXPENSE`: requires `categoryId` and `paymentMethodId`; `subCategoryId` stays optional.

**No soft delete here** — `Transaction` has no `active` field; `DELETE` is a real hard delete
(and reverts the balance effect on the bank account(s) involved). The frontend removes the row
from the local list immediately, same as the (hard-delete) Roles page — unlike the F1-F3
soft-delete pages.

`CreateTransactionRequest` requires `userId` explicitly (not inferred server-side) — the
frontend sends `spaceStore.dbUser.id` (the logged-in user, already loaded by the onboarding
flow — see `frontend/CLAUDE.md`). `UpdateTransactionRequest` has no `userId` (can't reassign
who a transaction belongs to).

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/transactions/index.vue` | Main page — period filter, table, create/edit/delete |
| `components/dialogs/AddEditTransactionDialog.vue` | Create/edit form with conditional fields by `type` |
| `components/dialogs/ConfirmDialog.vue` | Reused before deleting |
| `server/api/transactions/index.get.ts` | Proxies `GET /transactions` (forwards all query params as-is) |
| `server/api/transactions/index.post.ts` | Proxies `POST /transactions` |
| `server/api/transactions/[id].put.ts` | Proxies `PUT /transactions/{id}` |
| `server/api/transactions/[id].delete.ts` | Proxies `DELETE /transactions/{id}` |

## Page behavior (`pages/transactions/index.vue`)

- On mount / `spaceStore.activeSpace` change, fetches **four** things in parallel: the
  transactions for the current filter, and the full Bank Accounts / Categories / Payment
  Methods lists (needed both for the table's name lookups and to populate the dialog's selects).
- Period filter: `De`/`Até` date inputs, defaulting to the **first and last day of the current
  month**. Refetching is an explicit "Filtrar" button click (not reactive on every keystroke),
  to avoid firing a request per date-picker interaction.
- Table is sorted by `transactionDate` descending (client-side, since the backend doesn't sort).
  Columns: Data, Tipo (chip, colored by type), Conta, "Categoria / Destino" (shows `→ conta
  destino` for `TRANSFER`, otherwise `categoria / subcategoria`), Forma de Pagamento (`—` for
  `TRANSFER`), Descrição, Valor (signed and colored: green `+` for `INCOME`, red `-` for
  `EXPENSE`, neutral for `TRANSFER`), Ações.
- Deleting shows a warning that the account balance will be reverted (matches the backend's
  `TransactionBalanceEffectService.revert()` behavior on delete) and removes the row on success
  (hard delete, no "Inativo" state — see backend note above).

## Form (`AddEditTransactionDialog.vue`)

- `Tipo` (select: Receita/Despesa/Transferência), `Valor`, then a conditional block:
  - `TRANSFER`: `Conta de origem` + `Conta de destino` (same bank account list, destination
    excludes whichever is picked as origin).
  - `INCOME`/`EXPENSE`: `Conta`, `Categoria`, `Subcategoria` (cascades from the selected
    category's `subCategories`, cleared automatically if the category changes), `Forma de
    pagamento`.
  - `Data` (native date input) and optional `Descrição` always shown.
- Select options include inactive Bank Accounts/Categories/SubCategories/Payment Methods too
  (suffixed "(inativo)") — this matters when **editing** an old transaction that references
  something since deactivated; the option list still needs to show and keep that value selected.
- Switching `Tipo` away from `TRANSFER` clears `destinationBankAccountId`; switching to
  `TRANSFER` clears `categoryId`/`subCategoryId`/`paymentMethodId` (mirrors the backend's
  branching validation, avoiding a 422 from stale fields).

## Navigation

No static navigation edit needed — already seeded (`group_menus`/`group_menu_children` for
`/transactions` in `backend/docs/seed.sql`).

## Manual verification

No frontend test suite exists in this project — verified manually per the plan: `pnpm dev`,
open `/transactions`, create an `INCOME`, `EXPENSE`, and `TRANSFER`, confirm the balances of the
account(s) involved change accordingly (checked on `/bank-accounts`), edit a transaction (e.g.
change its amount) and confirm the balance adjusts by the delta, delete a transaction and
confirm the balance reverts.

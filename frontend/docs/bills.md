# Bills (Contas a Pagar)

Status: **implemented and working**.

The module underlying model is split in two entities:

- **`Bill`** — the primary entity, an actual account payable. Always carries its own `space`, `name`, `category`, `subCategory`, `amount`, `dueDate`, `referenceMonth`, `status` (`PENDING`/`PAID`). Optionally references the `BillRecurring` that generated it (`billRecurringId`, `null` for a one-off/standalone bill).
- **`BillRecurring`** — pure recurrence configuration (default `name`/`category`/`subCategory`/`amount`, anchor `startDate`, `active`). Only created when the user checks "Conta recorrente" — a one-off bill never creates one, it only ever produces a standalone `Bill`.

This is a rename from an earlier model where `Bill` was the primary entity and `BillInstance` was the occurrence — the roles were inverted specifically so that `Bill.space` is always a direct field (no join through an optional relation), which also fixed a real bug risk in the Reports "Contas Pendentes" section (see `backend/docs/recurring-bills.md`).

**Known gap**: `BillRecurring` never got a hard-delete / activate-deactivate refactor — `DELETE /bills/{id}` only soft-deactivates it (no "Ativar" to undo). A `Bill` occurrence, on the other hand, has a real (if soft) delete via `DELETE /bills/instances/{id}` — but only while `PENDING`; a `PAID` one can't be deleted or edited, only reversed via undo-payment.

## Backend

- `BillController` (`/bills`) — CRUD for `BillRecurring` (create, list, update basic fields, update schedule, deactivate).
- `BillInstanceController` (`/bills/instances`) — CRUD for `Bill` occurrences: list (with on-demand generation for recurring ones), create standalone, full edit, delete, pay, undo-payment.

Both `@PreAuthorize`-protected; see `backend/docs/seed.sql` section 12 for the endpoint_permission entries.

| Method | Path | Body / Query | Response |
|---|---|---|---|
| GET | `/bills` | `spaceId` (query) | `BillResponse[]` |
| POST | `/bills` | `{ spaceId, name, categoryId, subCategoryId, defaultAmount, startDate }` | `BillResponse` |
| PUT | `/bills/{id}` | `{ version, name, categoryId, subCategoryId, defaultAmount }` | `BillResponse` |
| PUT | `/bills/{id}/schedule` | `{ version, startDate }` | `BillResponse` |
| DELETE | `/bills/{id}` | — | `204` (soft-deactivate the `BillRecurring`) |
| GET | `/bills/instances` | `spaceId, from?, to?` (query) | `BillInstanceResponse[]` (triggers on-demand generation of recurring bills up to `to` / today) |
| POST | `/bills/instances` | `{ spaceId, name, categoryId, subCategoryId, amount, dueDate }` | `BillInstanceResponse` (creates a standalone `Bill`, no `BillRecurring`) |
| PUT | `/bills/instances/{id}` | `{ version, name, categoryId, subCategoryId, amount, dueDate }` | `BillInstanceResponse` (rejected `422` if already `PAID`) |
| DELETE | `/bills/instances/{id}` | — | `204` (soft-delete, rejected `422` if already `PAID`) |
| POST | `/bills/instances/{id}/pay` | `{ bankAccountId, paidDate }` | `BillInstanceResponse` |
| POST | `/bills/instances/{id}/undo-payment` | — | `204` |

`BillResponse` (`BillRecurring`): `{ id, version, spaceId, name, categoryId, subCategoryId, defaultAmount, startDate, active, createdDate }`.

`BillInstanceResponse` (`Bill`): `{ id, version, billRecurringId, name, categoryId, subCategoryId, referenceMonth, dueDate, amount, status (PENDING|PAID), paidDate, paymentTransactionId, bankAccountId, createdDate }`. `billRecurringId` is `null` for a standalone bill.

Note: `PayBillInstanceRequest` has **no** `categoryId` — category/subCategory are always taken from the `Bill` being paid (whatever was set at creation/edit time), never overridden at payment time. Rejected with `422` if the bill has no category at all.

A recurring `BillRecurring` only produces `Bill` rows lazily, the first time a period covering them is requested via `GET /bills/instances`; a standalone bill is created directly (`POST /bills/instances`) with no lazy-generation step.

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/bills/index.vue` | Home screen — lists `Bill` occurrences (date-range filter), edit/pay/undo-payment/delete. Header actions: "Adicionar Conta" and "Configurações de Recorrência" |
| `components/dialogs/AddEditBillDialog.vue` | Create mode (home screen "Adicionar Conta") branches internally: `POST /bills` if the "recorrente" checkbox is on, `POST /bills/instances` otherwise. Edit mode (used only from `RecurrenceSettingsDialog`) edits a `BillRecurring`'s `name`/`categoryId`/`subCategoryId`/`defaultAmount` |
| `components/dialogs/EditBillInstanceDialog.vue` | Full edit of a single `Bill` occurrence (`name`/`categoryId`/`subCategoryId`/`amount`/`dueDate`) — only while `PENDING`, never touches the `BillRecurring` |
| `components/dialogs/RecurrenceSettingsDialog.vue` | Modal (not a page) listing `BillRecurring` rows via `GET /bills`; row actions: Editar (`AddEditBillDialog` edit mode), Editar Agenda (`UpdateBillScheduleDialog`), Desativar (`ConfirmDialog` + `DELETE /bills/{id}`) |
| `components/dialogs/UpdateBillScheduleDialog.vue` | Dedicated action to change only `startDate` on an existing `BillRecurring` |
| `components/dialogs/PayBillInstanceDialog.vue` | Pay dialog — `bankAccountId`, `paidDate` only (no category field; the bill's own category/subCategory are used server-side) |
| `components/dialogs/ConfirmDialog.vue` | Reused for delete, deactivate and undo-payment confirmations |
| `server/api/bills/*` | `BillRecurring` CRUD + schedule proxy routes |
| `server/api/bills/instances/*` | `Bill` list/create/update/delete/pay/undo-payment proxy routes |

`pages/bills/instances.vue` was removed — its content (the occurrence list) was absorbed into `pages/bills/index.vue`, which is now the module's only page. `RecurrenceSettingsDialog` replaced what used to be a standalone "Contas do Mês" page navigation with a modal.

## Page behavior

### `/bills` (home)
Date range filter (`from`/`to`, defaults to current month, same pattern as `/transactions`). Table columns: Conta, Categoria, Subcategoria, Vencimento, Valor, Status. Row actions only while `status === 'PENDING'`: Editar (`EditBillInstanceDialog`), Marcar como Paga (`PayBillInstanceDialog`), Excluir (`ConfirmDialog`, soft-delete). While `PAID`: only Desfazer Pagamento is shown. No inline editing anymore (the old inline amount edit on the instances table was replaced by the full `EditBillInstanceDialog`).

### "Configurações de Recorrência" (modal)
Opened from the home screen's header button. Lists every `BillRecurring` in the space — editing name/category/subCategory/default amount or the anchor `startDate` here only affects `Bill` rows generated **after** the change; already-generated ones (pending or paid) keep their old values, since each `Bill` snapshots them at generation time.

## Manual verification

No automated frontend test suite exists in this project — verified manually via `pnpm dev`:
- Create a standalone bill (checkbox off) with category + subcategory — confirm it shows on `/bills` immediately with no `BillRecurring` created for it (check "Configurações de Recorrência" stays empty for it).
- Edit that bill (name/category/subcategory/amount/due date) from the home screen — confirm it updates in place.
- Delete a `PENDING` bill — confirm it disappears and does not reappear when re-filtering dates.
- Mark a bill as paid — confirm the debit in `/bank-accounts` and that the category/subcategory on the resulting transaction match the bill's own; confirm edit/delete are no longer available on it.
- Undo the payment — confirm the balance reverts and the bill returns to `PENDING`, editable again.
- Create a recurring bill (checkbox on) — confirm it appears in "Configurações de Recorrência" and that its first occurrence is generated lazily the first time the covering period is viewed on `/bills`.
- Edit the recurrence's schedule/defaults mid-cycle — confirm already-generated occurrences keep their old data while future ones (a month not yet visited) pick up the new values.
- Deactivate a recurring bill — confirm it stops generating new occurrences but already-generated ones remain untouched.

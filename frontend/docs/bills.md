# Bills (Contas a Pagar)

Status: **implemented and working** (backend already existed; this doc covers the frontend
piece — Grupos FAP1/FAP2 of `backend/IMPLEMENTATION_PLAN.md`).

**Same gap as Credit Cards** (see `credit-cards.md`): `Bill` never went through the hard-delete /
activate-deactivate refactor. `domain/Bill.java` only has `deactivate()` (no `activate()`), and
the single `DELETE /bills/{id}` does a soft-deactivate via `DeactivateBillService`. `ListBillsService`
returns every bill (active and inactive). "Excluir" here is therefore a one-way, irreversible-in-
the-UI soft delete — no "Ativar/Inativar" toggle like `/bank-accounts` has.

## Backend

- `BillController` (`/bills`) — CRUD (basic fields + dedicated schedule update) + list.
- `BillInstanceController` (`/bills/instances`) — monthly occurrences: list (with on-demand
  generation), amount edit, pay, undo-payment.

Both already implemented with `@PreAuthorize`.

| Method | Path | Body / Query | Response |
|---|---|---|---|
| GET | `/bills` | `spaceId` (query) | `BillResponse[]` |
| POST | `/bills` | `{ spaceId, name, categoryId, defaultAmount, startDate, recurring }` | `BillResponse` |
| PUT | `/bills/{id}` | `{ version, name, categoryId, defaultAmount }` | `BillResponse` |
| PUT | `/bills/{id}/schedule` | `{ version, recurring, startDate }` | `BillResponse` |
| DELETE | `/bills/{id}` | — | `204` (soft-deactivate) |
| GET | `/bills/instances` | `spaceId, from?, to?` (query) | `BillInstanceResponse[]` (triggers on-demand generation up to `to` / today) |
| PUT | `/bills/instances/{id}/amount` | `{ version, amount }` | `BillInstanceResponse` (rejected with `422` if already `PAID`) |
| POST | `/bills/instances/{id}/pay` | `{ bankAccountId, categoryId?, paymentMethodId, paidDate }` | `BillInstanceResponse` |
| POST | `/bills/instances/{id}/undo-payment` | — | `204` |

`BillResponse`: `{ id, version, spaceId, name, categoryId, defaultAmount, startDate, recurring, active, createdDate }`.

`BillInstanceResponse`: `{ id, version, billId, billName, referenceMonth, dueDate, amount, status (PENDING|PAID), paidDate, paymentTransactionId, bankAccountId, createdDate }`.

Note: `categoryId` in `PayBillInstanceRequest` is **optional** — the backend falls back to the
bill's own `categoryId` if omitted, and rejects with `422` only if neither is present.

A non-recurring (`recurring=false`) bill already gets 1 `BillInstance` created on `POST /bills`
(same `dueDate` as `startDate`); a recurring bill only gets instances generated lazily, the first
time a period covering them is requested via `GET /bills/instances`.

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/bills/index.vue` | Bill CRUD table (FAP1) — link to "Contas do Mês" |
| `pages/bills/instances.vue` | Monthly `BillInstance` list, inline amount edit, pay, undo-payment (FAP2) |
| `components/dialogs/AddEditBillDialog.vue` | Create/edit dialog — create mode also asks `startDate`/`recurring`; edit mode only touches `name`/`categoryId`/`defaultAmount` |
| `components/dialogs/UpdateBillScheduleDialog.vue` | Dedicated action to change `recurring`/`startDate` on an existing bill |
| `components/dialogs/PayBillInstanceDialog.vue` | Pay dialog (`bankAccountId`, `categoryId` clearable, `paymentMethodId`, `paidDate`) |
| `components/dialogs/ConfirmDialog.vue` | Reused for delete and undo-payment confirmations |
| `server/api/bills/*` | Bill CRUD + schedule proxy routes |
| `server/api/bills/instances/*` | Instance list/amount/pay/undo-payment proxy routes |

`/bills/instances` is not in the sidebar (only `/bills` is seeded) — reached via the "Contas do
Mês" button on `/bills`.

## Page behavior

### `/bills` (FAP1)
Same pattern as Credit Cards: search by name, client-side pagination, create/edit dialog, a
separate "Editar Agenda" action opening `UpdateBillScheduleDialog`, delete with `ConfirmDialog`
(irreversible). Table shows category name (resolved from `GET /categories`), default amount,
recorrente (chip), start date, status.

### `/bills/instances` (FAP2)
Period filter defaults to the current month (same `from`/`to` pattern as `/transactions`).
Amount is editable **inline** only while `status === 'PENDING'` (same inline-edit interaction as
`ManageSubCategoriesDialog.vue`, adapted to a numeric field) via `PUT .../amount`. "Marcar como
Paga" opens `PayBillInstanceDialog` (only for `PENDING`); "Desfazer Pagamento" opens a
`ConfirmDialog` with an explicit reversal warning (only for `PAID`).

## Manual verification

No frontend test suite exists in this project — verified manually: `pnpm dev`, create a
non-recurring bill and confirm 1 instance already shows up in `/bills/instances`; create a
recurring bill; edit the schedule of a recurring bill already in progress and confirm already-
generated instances (pending or paid) keep their old due date, only future ones change; open a
future month never visited before and confirm on-demand generation; edit the amount of a pending
instance; mark it as paid and check the debit in `/bank-accounts`; undo the payment and confirm
the balance reverts and the instance returns to `PENDING`.

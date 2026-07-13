# Credit Cards

Status: **implemented and working** (backend already existed; this doc covers the frontend
piece — Grupos FCC1/FCC2/FCC3 of `backend/IMPLEMENTATION_PLAN.md`).

**Important gap inherited from the backend:** `CreditCard` never went through the hard-delete /
activate-deactivate refactor (`REF1-4`/`DEL1-4`) that `BankAccount`/`Category`/`SubCategory`
got later. `domain/CreditCard.java` only has `deactivate()` (no `activate()`),
and the single `DELETE /credit-cards/{id}` does a soft-deactivate via `DeactivateCreditCardService`
— there is no `PATCH .../status` and no real hard delete. `ListCreditCardsService` returns every
card (active and inactive), so the "Excluir" action here is a **one-way, irreversible-in-the-UI**
soft delete, unlike the "Ativar/Inativar" + "Excluir definitivamente" pair used in `/bank-accounts`.

## Backend

- `CreditCardController` (`/credit-cards`) — CRUD + list.
- `CreditCardTransactionController` (`/credit-card-transactions`) — lançamentos + parcelamento + antecipação.
- `CreditCardInvoiceController` (`/credit-cards/invoices`, `/credit-cards/{id}/invoices/{referenceMonth}/...`) — fatura, pagamento, reversão.

All already implemented with `@PreAuthorize`.

| Method | Path | Body / Query | Response |
|---|---|---|---|
| GET | `/credit-cards` | `spaceId` (query) | `CreditCardResponse[]` |
| POST | `/credit-cards` | `{ spaceId, name, limit, closingDay, dueDay, bankAccountId? }` | `CreditCardResponse` |
| PUT | `/credit-cards/{id}` | `{ version, name, limit, closingDay, dueDay, bankAccountId? }` | `CreditCardResponse` |
| DELETE | `/credit-cards/{id}` | — | `204` (soft-deactivate) |
| GET | `/credit-card-transactions` | `spaceId, creditCardId?, categoryId?, subCategoryId?, from?, to?, referenceMonth?` (query) | `CreditCardTransactionResponse[]` |
| GET | `/credit-card-transactions/installment-groups/{installmentGroupId}` | — | `CreditCardTransactionResponse[]` (sorted by `installmentNumber`) |
| POST | `/credit-card-transactions/installment-groups/{id}/anticipate` | `{ targetReferenceMonth, installmentsToAnticipate }` | `CreditCardTransactionResponse[]` |
| POST | `/credit-card-transactions` | `{ creditCardId, userId, categoryId, subCategoryId, amount, purchaseDate, description, totalInstallments }` | `CreditCardTransactionResponse` |
| PUT | `/credit-card-transactions/{id}` | `{ version, categoryId, subCategoryId, amount, purchaseDate, description }` | `CreditCardTransactionResponse` |
| DELETE | `/credit-card-transactions/{id}` | `includeFuture?` (query, default `false`) | `204` (rejected with `422` if the reference month is already paid; when `includeFuture=true` on a grouped purchase, also deletes every later installment of the group, rejecting the whole batch if any of them is already paid) |
| GET | `/credit-cards/invoices` | `spaceId, creditCardId?, from?, to?` (query) | `CreditCardInvoiceResponse[]` |
| POST | `/credit-cards/{id}/invoices/{referenceMonth}/pay` | `{ bankAccountId, categoryId, subCategoryId, paidDate }` | `CreditCardInvoicePaymentResponse` |
| POST | `/credit-cards/{id}/invoices/{referenceMonth}/undo-payment` | — | `204` |

`CreditCardResponse`: `{ id, version, spaceId, name, limit, closingDay, dueDay, active, createdDate, bankAccountId, bankAccountName }`.

`bankAccountId` is an **optional** link to a `BankAccount` of the same space (422 if it belongs
to another space). It exists so the "Conta" filter of the category report can reach card
purchases — see `backend/docs/report-by-category.md` (RPTC6) and
[`reports-by-category.md`](./reports-by-category.md). `bankAccountName` is denormalized into the
response so the list/report don't need an extra lookup.

`CreditCardTransactionResponse`: `{ id, version, creditCardId, userId, categoryId, subCategoryId, amount, purchaseDate, description, referenceMonth, installmentGroupId, installmentNumber, totalInstallments, anticipated, originalReferenceMonth, createdDate, totalAmount }`. `totalAmount` is the sum of every installment's `amount` in the group — computed on read (not persisted) via `findByInstallmentGroupId`, equal to `amount` itself for a single (`totalInstallments <= 1`) purchase — shown in the UI as a reference to the original purchase total next to the per-installment `amount`.

`CreditCardInvoiceResponse`: `{ creditCardId, creditCardName, referenceMonth, closingDate, dueDate, totalAmount, paid, paidDate, paidAmount, paymentTransactionId }` — an invoice is never materialized until paid; open invoices are computed on the fly by grouping `CreditCardTransaction` rows by the stored `referenceMonth`.

Note: `categoryId` is **required** in `PayCreditCardInvoiceRequest` (no fallback to a default
category — unlike `PayBillInstanceRequest`, see `bills.md`).

`referenceMonth` on `GET /credit-card-transactions` is an exact-match filter (not a range) added
so the frontend can fetch exactly the transactions belonging to one invoice — this is the same
join key (`creditCardId` + `referenceMonth`) that `ListCreditCardInvoicesService` already used
internally via `findByCreditCardIdAndReferenceMonth` to group transactions into invoices; the new
param just reuses the spaceId-scoped `findByFilter` specification instead.

`from`/`to` on the same endpoint filter by `referenceMonth` too (invoice month), not by
`purchaseDate` — each installment of a parceled purchase carries its own `referenceMonth`, so
filtering by period shows one installment per month, not every installment bunched into the
purchase's month.

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/credit-cards/index.vue` | Card CRUD table (FCC1) — opens lançamentos/fatura per card as modals |
| `components/dialogs/CreditCardTransactionsDialog.vue` | Lançamentos of a single card, filtered by period (FCC2) — near-fullscreen modal, replaces the old `pages/credit-cards/[id]/transactions.vue` route |
| `components/dialogs/CreditCardInvoicesDialog.vue` | Invoice list, payment and undo-payment for a single card (FCC3) — near-fullscreen modal, replaces the old `pages/credit-cards/[id]/invoices.vue` route |
| `components/dialogs/InvoiceTransactionsDialog.vue` | Read-only listing of every `CreditCardTransaction` belonging to one invoice (`creditCardId` + `referenceMonth`), sorted ascending by `purchaseDate` — opened from a row action inside `CreditCardInvoicesDialog` |
| `components/dialogs/AddEditCreditCardDialog.vue` | Create/edit dialog for `CreditCard` |
| `components/dialogs/AddEditCreditCardTransactionDialog.vue` | Create/edit dialog for `CreditCardTransaction`, with an installments field only shown when creating |
| `components/dialogs/AnticipateInstallmentsDialog.vue` | Anticipates the last N installments of a group into the current open invoice |
| `components/dialogs/PayCreditCardInvoiceDialog.vue` | Pay-invoice dialog (`bankAccountId`, `categoryId`, `subCategoryId`, `paidDate`) |
| `components/dialogs/ConfirmDialog.vue` | Reused for delete and undo-payment confirmations (binary yes/no) |
| `components/dialogs/DeleteInstallmentDialog.vue` | 3-option delete confirmation ("somente esta parcela" / "esta e as futuras" / cancelar) shown instead of `ConfirmDialog` when deleting a row with `totalInstallments > 1` |
| `server/api/credit-cards/*` | Card CRUD proxy routes |
| `server/api/credit-card-transactions/*` | Transaction CRUD + installment-group + anticipate proxy routes |
| `server/api/credit-cards/invoices/index.get.ts`, `server/api/credit-cards/[id]/invoices/[referenceMonth]/*` | Invoice list/pay/undo-payment proxy routes |

There is no longer a dedicated route for lançamentos/fatura per card — `/credit-cards` (the only
`FRONT_PAGE` / `group_menu_children` entry for this module) opens both as modals sized to occupy
~95% of the viewport (`100%` on mobile), so they read as a near-fullscreen view without dropping
the dialog's rounded corners/backdrop. Both dialogs fetch their data reactively when
`isDialogVisible` becomes `true`, instead of on route mount.

## Page/dialog behavior

### `/credit-cards` (FCC1)
Same pattern as Payment Methods/Bank Accounts: search by name, client-side pagination,
create/edit dialog, delete with `ConfirmDialog` (irreversible, no "Ativar/Inativar" — see the
gap note above). Row actions: Ver Lançamentos, Ver Fatura, Editar, Excluir — the first two open
`CreditCardTransactionsDialog`/`CreditCardInvoicesDialog` (see below) instead of navigating.
The table has a "Conta" column showing `bankAccountName` (`—` when the card has no linked
account), and `AddEditCreditCardDialog` has a clearable "Conta bancária (opcional)" select
(options fetched from `/api/bank-accounts` when the dialog opens) with a hint explaining the
link is used by the category report filters.

### `CreditCardTransactionsDialog` (FCC2)
Resolves the card's name via `GET /credit-cards?spaceId=` filtered by the `creditCardId` prop.
Period filter defaults to the current month and, since `from`/`to` now filter by `referenceMonth`,
only shows the installment(s) whose invoice falls in the selected period. Table shows date,
category/subcategory, description, amount (with a "Total: R$ ..." caption below it for parceled
rows, `totalInstallments > 1`, showing `totalAmount`), and an `"N/total"` chip when the row belongs
to an installment group, plus an "Antecipada" badge when `anticipated`. "Antecipar parcelas" is
only shown for rows with `installmentNumber < totalInstallments` and opens
`AnticipateInstallmentsDialog`, which loads the full group via `GET .../installment-groups/{id}`
to compute how many future installments are eligible before calling the anticipate endpoint.
Create/edit follow the standard dialog pattern. Delete: a single (`totalInstallments <= 1`) row
uses the standard `ConfirmDialog`; a row that belongs to an installment group instead opens
`DeleteInstallmentDialog`, which asks whether to delete just this installment or this one and
every later one in the group (`DELETE .../{id}?includeFuture=true`) — the removed rows are pruned
from the local list by `installmentGroupId` + `installmentNumber >= this row's`. A `422` from a
paid month surfaces the real backend message in either case.

### `CreditCardInvoicesDialog` (FCC3)
Lists months (open/paid) with totals. "Pagar Fatura" opens `PayCreditCardInvoiceDialog` (only for
open invoices). "Desfazer Pagamento" opens a `ConfirmDialog` with an explicit reversal warning
(only for paid invoices) and calls the undo-payment endpoint directly (no intermediate fetch of
the linked `Transaction` — the backend handles the balance reversal). A new "Ver Itens da Fatura"
row action opens `InvoiceTransactionsDialog`, a normal-sized (non-fullscreen) nested dialog listing
every transaction of that invoice, read-only, ordered ascending by `purchaseDate` — fetched via
`GET /credit-card-transactions?...&referenceMonth=` (see the endpoint table above).

## Manual verification

No frontend test suite exists in this project — verified manually: `pnpm dev`, create a card with
distinct `closingDay`/`dueDay`, launch purchases that fall in two different invoice months (one
before and one after closing), launch a 6x installment purchase and confirm 6 rows summing to the
total, anticipate the last 2 installments into the current open invoice, pay an invoice and check
the debit in `/bank-accounts`, try editing/deleting a transaction from an already-paid month
(expect rejection), undo the payment and confirm the balance reverts and the invoice reopens.

Also verify: "Ver Lançamentos"/"Ver Fatura" from `/credit-cards` open as near-fullscreen modals
(not page navigation) on both desktop and a mobile viewport; "Ver Itens da Fatura" on an invoice
row opens `InvoiceTransactionsDialog` stacked on top of `CreditCardInvoicesDialog` with the
correct transactions for that `referenceMonth`, sorted by date ascending.

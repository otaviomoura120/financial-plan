# Credit Cards

Status: **implemented and working** (backend already existed; this doc covers the frontend
piece — Grupos FCC1/FCC2/FCC3 of `backend/IMPLEMENTATION_PLAN.md`).

**Important gap inherited from the backend:** `CreditCard` never went through the hard-delete /
activate-deactivate refactor (`REF1-4`/`DEL1-4`) that `BankAccount`/`Category`/`SubCategory`/
`PaymentMethod` got later. `domain/CreditCard.java` only has `deactivate()` (no `activate()`),
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
| POST | `/credit-cards` | `{ spaceId, name, limit, closingDay, dueDay }` | `CreditCardResponse` |
| PUT | `/credit-cards/{id}` | `{ version, name, limit, closingDay, dueDay }` | `CreditCardResponse` |
| DELETE | `/credit-cards/{id}` | — | `204` (soft-deactivate) |
| GET | `/credit-card-transactions` | `spaceId, creditCardId?, categoryId?, subCategoryId?, from?, to?` (query) | `CreditCardTransactionResponse[]` |
| GET | `/credit-card-transactions/installment-groups/{installmentGroupId}` | — | `CreditCardTransactionResponse[]` (sorted by `installmentNumber`) |
| POST | `/credit-card-transactions/installment-groups/{id}/anticipate` | `{ targetReferenceMonth, installmentsToAnticipate }` | `CreditCardTransactionResponse[]` |
| POST | `/credit-card-transactions` | `{ creditCardId, userId, categoryId, subCategoryId, amount, purchaseDate, description, totalInstallments }` | `CreditCardTransactionResponse` |
| PUT | `/credit-card-transactions/{id}` | `{ version, categoryId, subCategoryId, amount, purchaseDate, description }` | `CreditCardTransactionResponse` |
| DELETE | `/credit-card-transactions/{id}` | — | `204` (rejected with `422` if the reference month is already paid) |
| GET | `/credit-cards/invoices` | `spaceId, creditCardId?, from?, to?` (query) | `CreditCardInvoiceResponse[]` |
| POST | `/credit-cards/{id}/invoices/{referenceMonth}/pay` | `{ bankAccountId, categoryId, paymentMethodId, paidDate }` | `CreditCardInvoicePaymentResponse` |
| POST | `/credit-cards/{id}/invoices/{referenceMonth}/undo-payment` | — | `204` |

`CreditCardResponse`: `{ id, version, spaceId, name, limit, closingDay, dueDay, active, createdDate }`.

`CreditCardTransactionResponse`: `{ id, version, creditCardId, userId, categoryId, subCategoryId, amount, purchaseDate, description, referenceMonth, installmentGroupId, installmentNumber, totalInstallments, anticipated, originalReferenceMonth, createdDate }`.

`CreditCardInvoiceResponse`: `{ creditCardId, creditCardName, referenceMonth, closingDate, dueDate, totalAmount, paid, paidDate, paidAmount, paymentTransactionId }` — an invoice is never materialized until paid; open invoices are computed on the fly by grouping `CreditCardTransaction` rows by the stored `referenceMonth`.

Note: `categoryId` is **required** in `PayCreditCardInvoiceRequest` (no fallback to a default
category — unlike `PayBillInstanceRequest`, see `bills.md`).

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/credit-cards/index.vue` | Card CRUD table (FCC1) — links to lançamentos/fatura per card |
| `pages/credit-cards/[id]/transactions.vue` | Lançamentos of a single card, filtered by period (FCC2) |
| `pages/credit-cards/[id]/invoices.vue` | Invoice list, payment and undo-payment for a single card (FCC3) |
| `components/dialogs/AddEditCreditCardDialog.vue` | Create/edit dialog for `CreditCard` |
| `components/dialogs/AddEditCreditCardTransactionDialog.vue` | Create/edit dialog for `CreditCardTransaction`, with an installments field only shown when creating |
| `components/dialogs/AnticipateInstallmentsDialog.vue` | Anticipates the last N installments of a group into the current open invoice |
| `components/dialogs/PayCreditCardInvoiceDialog.vue` | Pay-invoice dialog (`bankAccountId`, `categoryId`, `paymentMethodId`, `paidDate`) |
| `components/dialogs/ConfirmDialog.vue` | Reused for delete and undo-payment confirmations |
| `server/api/credit-cards/*` | Card CRUD proxy routes |
| `server/api/credit-card-transactions/*` | Transaction CRUD + installment-group + anticipate proxy routes |
| `server/api/credit-cards/invoices/index.get.ts`, `server/api/credit-cards/[id]/invoices/[referenceMonth]/*` | Invoice list/pay/undo-payment proxy routes |

These pages are not in the sidebar (only `/credit-cards` is seeded as a `FRONT_PAGE` /
`group_menu_children` entry) — they're reached by navigating from a row of the cards table.

## Page behavior

### `/credit-cards` (FCC1)
Same pattern as Payment Methods/Bank Accounts: search by name, client-side pagination,
create/edit dialog, delete with `ConfirmDialog` (irreversible, no "Ativar/Inativar" — see the
gap note above). Row actions: Ver Lançamentos, Ver Fatura, Editar, Excluir.

### `/credit-cards/[id]/transactions` (FCC2)
Resolves the card's name via `GET /credit-cards?spaceId=` filtered by the route `id`. Period
filter defaults to the current month. Table shows date, category/subcategory, description,
amount, and an `"N/total"` chip when the row belongs to an installment group (`totalInstallments > 1`),
plus an "Antecipada" badge when `anticipated`. "Antecipar parcelas" is only shown for rows with
`installmentNumber < totalInstallments` and opens `AnticipateInstallmentsDialog`, which loads the
full group via `GET .../installment-groups/{id}` to compute how many future installments are
eligible before calling the anticipate endpoint. Create/edit/delete follow the standard dialog +
`ConfirmDialog` pattern; a `422` from a paid month surfaces the real backend message.

### `/credit-cards/[id]/invoices` (FCC3)
Lists months (open/paid) with totals. "Pagar Fatura" opens `PayCreditCardInvoiceDialog` (only for
open invoices). "Desfazer Pagamento" opens a `ConfirmDialog` with an explicit reversal warning
(only for paid invoices) and calls the undo-payment endpoint directly (no intermediate fetch of
the linked `Transaction` — the backend handles the balance reversal).

## Manual verification

No frontend test suite exists in this project — verified manually: `pnpm dev`, create a card with
distinct `closingDay`/`dueDay`, launch purchases that fall in two different invoice months (one
before and one after closing), launch a 6x installment purchase and confirm 6 rows summing to the
total, anticipate the last 2 installments into the current open invoice, pay an invoice and check
the debit in `/bank-accounts`, try editing/deleting a transaction from an already-paid month
(expect rejection), undo the payment and confirm the balance reverts and the invoice reopens.

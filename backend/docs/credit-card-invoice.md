# Credit Card Invoice Cycle

## Why there is no "CreditCardInvoice" entity

A monthly invoice is never materialized as its own row while it's open. An open invoice is always computed **in memory** by grouping `CreditCardTransaction` rows by their stored `referenceMonth` for a given `creditCardId` (see the parcelamento model in `APP_OVERVIEW.md`, section "CreditCardTransaction"). Only when an invoice is **paid** does a row get written — to `CreditCardInvoicePayment`. Existence of that row *is* the "paid" status; there is no separate status field or scheduler that flips a state.

This means:
- Adding, editing, deleting, or anticipating installments never needs to touch a separate invoice record — they just change which `(creditCardId, referenceMonth)` bucket a `CreditCardTransaction` falls into.
- `CreditCardInvoiceCycle` (pure calculator, no repository) is only consulted for **display metadata** — `closingDate`/`dueDate` — derived from a group's `referenceMonth`; it is never re-run to decide which group a transaction belongs to after creation (that happens once, at creation time — see CC4/CC5 in `APP_OVERVIEW.md`).

## Listing invoices — `ListCreditCardInvoicesService`

`GET /credit-cards/invoices?spaceId=&creditCardId=&from=&to=`

1. Resolve the candidate `CreditCard`s: `creditCardRepository.findBySpaceId(spaceId)`, optionally narrowed to one card if `creditCardId` is given (a mismatched id simply yields no results — no separate "wrong space" error).
2. For each card, fetch **every** `CreditCardTransaction` (`findByCreditCardId`, no date filter — an installment's `referenceMonth` can be months away from its `purchaseDate`) and group them by `referenceMonth`.
3. For each group, compute `closingDate`/`dueDate` via `CreditCardInvoiceCycle`, sum the amounts, and check `CreditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth` to mark it `paid`/open.
4. Keep only groups whose `dueDate` falls within `[from, to]` (same convention later used for pending items in Reports — see RPT1).

Since an invoice group can only exist when at least one `CreditCardTransaction` exists for that month, there is no "phantom empty invoice" case to filter out.

To let the frontend list the actual transactions composing one invoice (rather than just the aggregate row above), `GET /credit-card-transactions` accepts an optional `referenceMonth` query param — an exact-match filter added to `CreditCardTransactionRepository.findByFilter`/`buildSpecification`, alongside the existing `spaceId`/`creditCardId`/`categoryId`/`subCategoryId`/`from`/`to` filters. It reuses the same `(creditCardId, referenceMonth)` join key this section's grouping already relies on, but goes through the spaceId-scoped `findByFilter` specification instead of the unscoped `findByCreditCardIdAndReferenceMonth` used internally here and by `PayCreditCardInvoiceService`/`UndoCreditCardInvoicePaymentService` (which stays untouched).

## Paying an invoice — `PayCreditCardInvoiceService`

`POST /credit-cards/{id}/invoices/{referenceMonth}/pay` — body: `{bankAccountId, categoryId, subCategoryId, paidDate}`.

1. Resolve the `CreditCard` (404-style `DomainException` if missing) and the authenticated user — the payer is **derived from the session**, not the request body: the controller passes `authentication.getName()` (the Auth0 sub) through, and the service resolves it via `UserRepository.findByAuth0Sub(...)`, the same pattern already used by `AcceptInviteService`/`ListMyInvitesService` for the same reason (an invoice payment isn't something the client should be able to attribute to an arbitrary `userId`).
2. Reject if `CreditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth` already returns a row (`"Invoice already paid"`).
3. Sum `CreditCardTransactionRepository.findByCreditCardIdAndReferenceMonth(creditCardId, referenceMonth)` — this already includes installments anticipated *into* this month from other purchases (they carry this `referenceMonth` once anticipated, see CC5b). Reject if the sum is not positive (`"Invoice has no transactions to pay"`).
4. Compute `dueDate` via `CreditCardInvoiceCycle.resolveDueDate(...)`.
5. **Create the Transaction before the payment row** (order matters — see below): calls `CreateTransactionService.execute(request, TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, creditCard.getId())` — an internal overload of the same public service used by `POST /transactions`, so it goes through the exact same FK validation and `TransactionBalanceEffectService.apply()` (debits `bankAccountId`) as any other `EXPENSE`. The public `CreateTransactionRequest`/`POST /transactions` contract is untouched — only this internal overload accepts a `sourceType`/`sourceId`, so a normal API client can never forge one.
6. Only now build and `save()` the `CreditCardInvoicePayment`, with `paymentTransactionId` already set to the just-created transaction's id.

**Why the Transaction is created first:** `CreditCardInvoicePaymentRepository` intentionally has no `update()` method (see CC3) — it's written once, fully formed. If the payment row were created first, `paymentTransactionId` would have to be patched in afterwards, which the repository doesn't support. Creating the `Transaction` first sidesteps that entirely.

**Why `sourceId` is the `creditCardId`, not the payment's own id:** the same ordering constraint rules out using the not-yet-existing `CreditCardInvoicePayment.id` as `sourceId` (it doesn't exist yet when the `Transaction` is created). `sourceId = creditCardId` is known upfront, and nothing in this codebase ever needs to walk from `Transaction.sourceId` back to a specific `CreditCardInvoicePayment` — `UndoCreditCardInvoicePaymentService` always starts from the payment (looked up by `creditCardId` + `referenceMonth`) and reaches the `Transaction` via `paymentTransactionId`, never the other way around.

The resulting `Transaction` has `sourceType = CREDIT_CARD_INVOICE_PAYMENT`, which — per the P1 guard — makes it immutable through the normal `PUT`/`DELETE /transactions/{id}` endpoints (`Transaction.isLinkedToSource()` returns `true`); it can only be reversed through `UndoCreditCardInvoicePaymentService` below.

## Undoing a payment — `UndoCreditCardInvoicePaymentService`

`POST /credit-cards/{id}/invoices/{referenceMonth}/undo-payment`

1. Look up the `CreditCardInvoicePayment` by `(creditCardId, referenceMonth)` — `DomainException("Credit card invoice payment not found")` if there is none (covers both "never paid" and "already undone").
2. Fetch the linked `Transaction` via `payment.getPaymentTransactionId()`.
3. Call `TransactionBalanceEffectService.revert(transaction)` and `TransactionRepository.delete(transaction.getId())` **directly** — deliberately bypassing the public `DeleteTransactionService`, since that service rejects any transaction with `sourceType != null` (see P1). This dedicated undo path is the one place allowed to remove a linked transaction.
4. `creditCardInvoicePaymentRepository.deleteById(payment.getId())` — the invoice immediately goes back to "open" the next time `ListCreditCardInvoicesService` groups this month's transactions (nothing else to clean up, since existence of the payment row *is* the paid flag).

`@Transactional` end to end: the balance revert, the transaction delete, and the payment delete all commit or roll back together.

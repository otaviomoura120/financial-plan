# Credit Card Invoice Cycle

## Why there is no "CreditCardInvoice" entity

A monthly invoice is never materialized as its own row while it's open. An open invoice is always computed **in memory** by grouping `CreditCardTransaction` rows by their stored `referenceMonth` for a given `creditCardId` (see the parcelamento model in `APP_OVERVIEW.md`, section "CreditCardTransaction"). Only when an invoice is **paid** does a row get written — to `CreditCardInvoicePayment`. Existence of that row *is* the "paid" status; there is no separate status field or scheduler that flips a state.

This means:
- Adding, editing, deleting, or anticipating installments never needs to touch a separate invoice record — they just change which `(creditCardId, referenceMonth)` bucket a `CreditCardTransaction` falls into.
- `CreditCardInvoiceCycle` (pure calculator, no repository) is only consulted for **display metadata** — `closingDate`/`dueDate` — derived from a group's `referenceMonth`; it is never re-run to decide which group a transaction belongs to after creation (that happens once, at creation time — see CC4/CC5 in `APP_OVERVIEW.md`).

## Choosing the invoice at creation time

Around the closing day a bank may put a purchase in the current invoice or in the next one with no
observable rule, so the computed `referenceMonth` is a good default but not always the truth. Both
`CreateCreditCardTransactionRequest` and `UpdateCreditCardTransactionRequest` therefore carry an
optional `referenceMonth`, and `CreditCardInvoiceCycle` gained a three-argument overload:

```java
resolveReferenceMonth(LocalDate purchaseDate, int closingDay, LocalDate chosenReferenceMonth)
```

`null` keeps the existing behaviour. A non-null value is normalized to the first day of its month and
accepted **only** if it equals the computed invoice or the one right after it — anything else raises
`DomainException("Reference month must be the current or the next invoice")`. Restricting it to those
two keeps the stored `referenceMonth` inside the cycle the purchase could plausibly belong to, instead
of turning the field into a free-form month.

Nothing else in the invoice model changes: because an invoice is just a `(creditCardId, referenceMonth)`
grouping, choosing the invoice *is* choosing that stored value. Installments still anchor on the first
one and walk forward with `plusMonths(i-1)`, so a chosen invoice shifts the whole group. The existing
`rejectIfAnyMonthAlreadyPaid` guard covers a choice that lands on an already-paid invoice for free.

**On update.** `UpdateCreditCardTransactionService` used to always recompute `referenceMonth` from
`purchaseDate`, which would silently discard a manual choice on the first edit. It now returns the
transaction's current `referenceMonth` untouched when the request sends that same value back (the
frontend always does), and only validates through the overload when the value actually changes. As a
side effect this also stops a later installment from being dragged back to its group's first invoice on
edit — the old recompute ignored `installmentNumber`. Anticipated rows keep their earlier short-circuit
and are never re-derived here; moving those is the anticipation flow's job.

No new endpoint — `POST`/`PUT /credit-card-transactions` just carry an extra optional field — so
`seed.sql`'s `endpoint_permissions` need no change.

**Frontend:** `AddEditCreditCardTransactionDialog` shows a "Fatura" select with the two candidate
months (computed client-side from the card's `closingDay`, mirroring `resolveReferenceMonth`), defaulted
to the computed one. It is hidden for recurring subscriptions and, in edit mode, for installments and
anticipated rows — those still send their current `referenceMonth` back so the backend preserves it.

## Credit transactions (cashback / bank benefit)

A `CreditCardTransaction` can be a **credit** instead of a purchase — a value that *reduces* the invoice balance (cashback, a bank benefit/refund). It shows up negative on the invoice.

**Modeling — one boolean, magnitude stays positive.** The domain carries a `boolean credit` flag; `amount` is still always stored **positive** (the existing `validate()` rule "Amount must be positive" is unchanged and applies to the magnitude). The sign is applied only when aggregating or displaying, through the rich method:

```java
public BigDecimal getSignedAmount() {
    if (credit) {
        return amount.negate();
    }
    return amount;
}
```

This avoids double-negation: every consumer that needs the net value calls `getSignedAmount()`; anything showing the raw magnitude keeps using `getAmount()` plus the `credit` flag.

**A credit is always a single entry.** `validate()` rejects `credit == true` with `totalInstallments != 1` (`"Credit transaction must be a single installment"`), and `CreateCreditCardTransactionService` forces `totalInstallments = 1` when `credit` is set, ignoring any parcelas sent. Recurring credits are not supported (the recurring generation path always creates purchases). The flag is set **only at creation** — `UpdateCreditCardTransactionService`/`update(...)` never touch it, so editing a credit only changes value/category/date/description and preserves its nature.

**Where the sign is honored (net) vs. ignored:**
- `ListCreditCardInvoicesService` (invoice `totalAmount`) and `PayCreditCardInvoiceService` (amount actually paid) sum `getSignedAmount()` — so the total, the paid `EXPENSE` transaction, and the Reports **projected balance** (which reads the invoice `totalAmount` via `resolvePendingCreditCardInvoices`) all net out credits automatically.
- **Not** applied: `GenerateCategoryReportService` still sums `getAmount()` — the by-category report intentionally does not net credits for now (a credit appears there as an expense-magnitude line). This is a known, deliberate limitation.

**Persistence & transport:** a `credit` column on `CreditCardTransactionEntityJpa` (created by `ddl-auto=update`, defaulting to `false`/`0` for existing rows); `credit` added to `CreateCreditCardTransactionRequest` and `CreditCardTransactionResponse`. `UpdateCreditCardTransactionRequest` is unchanged. No new endpoint — the existing `POST /credit-card-transactions` just carries the extra field — so `seed.sql`'s `endpoint_permissions` need no change.

**Frontend:** the lançamento dialog has a "Lançar como crédito (abate da fatura)" checkbox (creation only, mutually exclusive with parcelas and recurring); the transaction lists and invoice-items view render credits negative (green) with a "Crédito" chip.

## Recurring subscriptions — `CreditCardTransactionRecurring`

A subscription (Netflix, Spotify, a gym plan) is a **template**, not an occurrence: `CreditCardTransactionRecurring` holds `creditCard`/`user`/`category`/`subCategory?`/`description`/`defaultAmount`/`startDate`/`active` and produces one real `CreditCardTransaction` per month. Unlike an invoice — which is never materialized because there is always something to group by the time anyone asks — a future subscription charge has nothing to group, so it must be written ahead of time (same reasoning as `Bill` in `recurring-bills.md`).

Each generated charge is a single-installment purchase: `totalInstallments = 1`, `credit = false`, a fresh `installmentGroupId` per month (they are independent purchases, not a parcelled one), `amount` snapshotted from `defaultAmount` at generation time.

### Generating — `EnsureRecurringCreditCardTransactionsGeneratedService`

1. Start from the month after the latest existing charge of that recurrence (`findByCreditCardTransactionRecurringId`, max purchase month), or from `startDate`'s month if none exist yet — never before `startDate`.
2. Walk forward one month at a time up to `capMonth`, which is at least `current month + credit-cards.recurring.horizon-months` (default 12) and honours a further `to` filter up to `credit-cards.recurring.max-horizon-months` (default 60). **That ceiling matters:** `resolveCapMonth` had no upper bound before, so `GET /credit-card-transactions?to=9999-12-31` would insert tens of thousands of rows per subscription inside one request transaction.
3. `purchaseDate` anchors to `startDate`'s day-of-month, clamped to the target month's length (Jan 31 → Feb 28/29), and `referenceMonth` comes from `CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, closingDay)` — so with `closingDay = 5` a day-10 subscription lands in the **next** month's invoice.
4. Idempotent by construction: `findByCreditCardTransactionRecurringIdAndPurchaseMonth` is checked before every insert. Note this is the **only** duplicate guard — unlike `bills`, which has a DB `@UniqueConstraint(bill_recurring_id, reference_month)` behind it. That is why the invoice listing and the report services deliberately stay read-only: a second concurrent writer racing the nightly job could pass the check simultaneously and produce a genuine duplicate charge.

Two entry points: `execute(spaceId, upToDate)` (lazy, from `ListCreditCardTransactionsService`) and `executeForRecurring(recurring)` (one template, up to the default horizon), used by `CreateCreditCardTransactionRecurringService`, `UpdateCreditCardTransactionRecurringService` and the nightly batch below. `UpdateCreditCardTransactionRecurringService` calls it **after** rewriting current/future charges, so the rewrite settles first and only genuinely-new months get created — this is what makes moving `startDate` backwards take effect immediately instead of waiting for the next run.

### Keeping the horizon full — batch + scheduler

Lazy generation only fires when someone opens the card's transaction screen, so on an idle space the horizon drifts backwards and subscriptions silently vanish from future invoices and reports. `RecurringCreditCardTransactionsGenerationScheduler` (`infrastructure/scheduler/`, enabled by `@EnableScheduling` on `FinancialPlanApplication`) runs `credit-cards.recurring.generation-cron` and delegates to `GenerateRecurringCreditCardTransactionsBatchService`, which walks `CreditCardTransactionRecurringRepository.findAllActive()` across every space and calls `executeForRecurring` per template.

Exactly the shape of the bills job (`recurring-bills.md`): the batch service is not `@Transactional`, so each per-template call crosses a bean boundary and commits on its own — a failing subscription is logged at ERROR and skipped instead of aborting the batch. Daily rather than monthly on purpose, since it is idempotent and a monthly job would miss its window if the app were down that day.

The cron defaults to **03:30**, half an hour after the bills job at 03:00. Spring's scheduler pool is single-threaded by default, so two jobs on the same expression would serialize unpredictably and neither log would tell you which one owned a long run.

### A month whose invoice was already paid is dropped, permanently

`createTransactionIfMissing` skips a month when a `CreditCardInvoicePayment` already exists for the computed `referenceMonth` — correct, and it must stay: the invoice total was computed and paid from exactly the rows in that bucket, and `CreateCreditCardTransactionService` refuses to insert into a paid invoice for the same reason.

The consequence is worth knowing. The skip returns **without persisting anything**, so that month never joins the `max(purchase month)` set the cursor is built from. Once a later month exists, the cursor starts past the hole and the skipped month is never retried — not on the next nightly run, and not even after `UndoCreditCardInvoicePaymentService` reopens the invoice. It is a permanent silent gap, not an infinite retry. A `LOGGER.debug` in the skip branch records it so a support question about a missing subscription charge is answerable from the logs; recovery is manual (add that month's charge via `POST /credit-card-transactions`).

Shifting the purchase forward to the next open invoice — what a real issuer does with a post-closing purchase — would remove the gap, but it moves money the user believes belongs to a month they already budgeted. That is a product decision, not a scheduler detail.

## Listing invoices — `ListCreditCardInvoicesService`

`GET /credit-cards/invoices?spaceId=&creditCardId=&from=&to=`

1. Resolve the candidate `CreditCard`s: `creditCardRepository.findBySpaceId(spaceId)`, optionally narrowed to one card if `creditCardId` is given (a mismatched id simply yields no results — no separate "wrong space" error).
2. For each card, fetch **every** `CreditCardTransaction` (`findByCreditCardId`, no date filter — an installment's `referenceMonth` can be months away from its `purchaseDate`) and group them by `referenceMonth`.
3. For each group, compute `closingDate`/`dueDate` via `CreditCardInvoiceCycle`, sum the **signed** amounts (`CreditCardTransaction::getSignedAmount` — credits count as negative, see "Credit transactions" below), and check `CreditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth` to mark it `paid`/open. A month with more credits than debits legitimately produces a negative `totalAmount`.
4. Keep only groups whose `dueDate` falls within `[from, to]` (same convention later used for pending items in Reports — see RPT1).

Since an invoice group can only exist when at least one `CreditCardTransaction` exists for that month, there is no "phantom empty invoice" case to filter out.

To let the frontend list the actual transactions composing one invoice (rather than just the aggregate row above), `GET /credit-card-transactions` accepts an optional `referenceMonth` query param — an exact-match filter added to `CreditCardTransactionRepository.findByFilter`/`buildSpecification`, alongside the existing `spaceId`/`creditCardId`/`categoryId`/`subCategoryId`/`from`/`to` filters. It reuses the same `(creditCardId, referenceMonth)` join key this section's grouping already relies on, but goes through the spaceId-scoped `findByFilter` specification instead of the unscoped `findByCreditCardIdAndReferenceMonth` used internally here and by `PayCreditCardInvoiceService`/`UndoCreditCardInvoicePaymentService` (which stays untouched).

## Paying an invoice — `PayCreditCardInvoiceService`

`POST /credit-cards/{id}/invoices/{referenceMonth}/pay` — body: `{bankAccountId, paidDate}`.

1. Resolve the `CreditCard` (404-style `DomainException` if missing) and the authenticated user — the payer is **derived from the session**, not the request body: the controller passes `authentication.getName()` (the Auth0 sub) through, and the service resolves it via `UserRepository.findByAuth0Sub(...)`, the same pattern already used by `AcceptInviteService`/`ListMyInvitesService` for the same reason (an invoice payment isn't something the client should be able to attribute to an arbitrary `userId`).
2. Reject if `CreditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth` already returns a row (`"Invoice already paid"`).
3. Sum the **signed** amounts of `CreditCardTransactionRepository.findByCreditCardIdAndReferenceMonth(creditCardId, referenceMonth)` (`getSignedAmount`, so credits subtract) — this already includes installments anticipated *into* this month from other purchases (they carry this `referenceMonth` once anticipated, see CC5b). Reject if the net sum is not positive (`"Invoice has no transactions to pay"`); this now also covers a credit-only or net-negative invoice, which cannot be "paid" as an expense.
4. Compute `dueDate` via `CreditCardInvoiceCycle.resolveDueDate(...)`.
5. Resolve the space's **system category** via `ResolveSystemCategoryService.execute(creditCard.getSpace().getId(), SystemCategory.CREDIT_CARD_INVOICE_PAYMENT)` — the request carries no category, so the payment transaction is always stamped with the reserved "Pagamento de Fatura" / "Fatura de Cartão" pair. See "System categories" below.
6. **Create the Transaction before the payment row** (order matters — see below): calls `CreateTransactionService.execute(request, TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, creditCard.getId())` — an internal overload of the same public service used by `POST /transactions`, so it goes through the exact same FK validation and `TransactionBalanceEffectService.apply()` (debits `bankAccountId`) as any other `EXPENSE`. The public `CreateTransactionRequest`/`POST /transactions` contract is untouched — only this internal overload accepts a `sourceType`/`sourceId`, so a normal API client can never forge one.
7. Only now build and `save()` the `CreditCardInvoicePayment`, with `paymentTransactionId` already set to the just-created transaction's id.

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

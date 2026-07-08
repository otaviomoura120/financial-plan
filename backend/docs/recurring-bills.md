# Recurring Bills Cycle

## Why `BillInstance` is materialized, unlike a credit card invoice

Unlike `CreditCardTransaction`/`CreditCardInvoicePayment` (see `credit-card-invoice.md`), a bill's monthly occurrence is **not** computed in memory by grouping other rows — it is its own persisted row, `BillInstance`, with its own `status` (`PENDING`/`PAID`). This is a deliberate difference, not an inconsistency:

- A credit card invoice only exists conceptually once at least one purchase has been made in that month — there is always a `CreditCardTransaction` to group by the time anyone asks about it.
- A bill instance must be visible, editable (`amount`) and payable **before** any `Transaction` exists for it — the user opens the "bills this month" screen and expects to see (and possibly pay) an instance that has no other row to derive itself from. There is nothing to group.

So `BillInstance` needs an explicit `status` field and its own lifecycle (`PENDING` → `PAID`, or back to `PENDING` via undo), materialized ahead of payment by `EnsureBillInstancesGeneratedService`.

## Generating instances — `EnsureBillInstancesGeneratedService`

Called internally by `ListBillInstancesService` (and later by Reports, see `RPT1`) — never exposed as its own endpoint.

1. For every `Bill` in the space that is `active` and `recurring` (a non-recurring `Bill` never needs this — it already got its single `BillInstance` at creation time, see `CreateBillService`), find the last month a `BillInstance` already exists for (`findByBillId`, max `referenceMonth`), or fall back to the month before `Bill.startDate` if none exist yet.
2. Walk forward one month at a time from there, creating a `PENDING` instance for each missing month, until reaching `capMonth = min(month of upToDate, current month + 1)` — the `+1` cap exists so a bill due next month is already visible today, without ever generating years of instances in one call.
3. Each generated instance's `dueDate` anchors to `Bill.startDate`'s day-of-month, clamped to the target month's length (same clamping idea as `CreditCardInvoiceCycle`); `amount` copies `Bill.defaultAmount` at generation time (later independently editable via `UpdateBillInstanceAmountService`, while still `PENDING`).
4. Idempotent by construction: before creating a month's instance, it checks `BillInstanceRepository.findByBillIdAndReferenceMonth` and skips if one already exists — safe to call on every list/report request.

## Listing bill instances — `ListBillInstancesService`

`GET /bills/instances?spaceId=&from=&to=`

1. Calls `EnsureBillInstancesGeneratedService.execute(spaceId, to ?? today)` first, so the requested period is always fully materialized before querying.
2. Returns `BillInstanceRepository.findBySpaceAndPeriod(spaceId, from, to)` — filtered by `dueDate` within `[from, to]` (both optional), isolated to the space via a subquery on `Bill.space` (same `spaceId`-isolation-from-day-one lesson learned from `T9b`/`CreditCardTransaction`).

## Paying an instance — `PayBillInstanceService`

`POST /bills/instances/{id}/pay` — body: `{bankAccountId, categoryId?, paymentMethodId, paidDate}`.

1. Resolve the `BillInstance` (`DomainException` if missing) and reject if it is not `PENDING` (`"Bill instance is already paid"`).
2. Resolve the authenticated user the same way `PayCreditCardInvoiceService` does — from `authentication.getName()` via `UserRepository.findByAuth0Sub(...)`, never from the request body.
3. Resolve the category: use `categoryId` from the request if informed (`DomainException` if it doesn't exist); otherwise fall back to `Bill.category`; if neither is available, reject (`"No category informed and the bill has no default category"`).
4. **Create the Transaction before touching the instance's status** — same ordering reasoning as `PayCreditCardInvoiceService`: calls `CreateTransactionService.execute(request, TransactionSourceType.BILL_INSTANCE_PAYMENT, bill.getId())` (`sourceId` is the parent `Bill`'s id, not the instance's own id, so it's known upfront and nothing ever needs to walk backwards from it — `UndoBillInstancePaymentService` always starts from the `BillInstance` and reaches the `Transaction` via `paymentTransactionId`).
5. Only then call `billInstance.markAsPaid(paidDate, paymentTransactionId, bankAccountId)` and persist it.

The resulting `Transaction` has `sourceType = BILL_INSTANCE_PAYMENT`, making it immutable through `PUT`/`DELETE /transactions/{id}` (`Transaction.isLinkedToSource()`); it can only be reversed through `UndoBillInstancePaymentService` below.

## Undoing a payment — `UndoBillInstancePaymentService`

`POST /bills/instances/{id}/undo-payment`

1. Look up the `BillInstance` by id (`DomainException` if missing) and reject if it is not `PAID` (covers both "never paid" and "already undone").
2. Fetch the linked `Transaction` via `billInstance.getPaymentTransactionId()`.
3. Call `TransactionBalanceEffectService.revert(transaction)` and `TransactionRepository.delete(transaction.getId())` **directly** — bypassing `DeleteTransactionService`, which rejects any transaction with `sourceType != null`. This dedicated undo path is the one place allowed to remove a linked transaction.
4. `billInstance.revertToPending()` clears `paidDate`/`paymentTransactionId`/`bankAccountId` and flips `status` back to `PENDING` — the instance immediately shows as pending again in `ListBillInstancesService`, with nothing else to clean up.

`@Transactional` end to end: the balance revert, the transaction delete, and the instance status change all commit or roll back together.

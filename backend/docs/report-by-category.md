# Report by Category

`POST /reports/by-category` — body: `CategoryReportFilterRequest`, response: `CategoryReportResponse`. Implemented by `GenerateCategoryReportService`, guarded by the same `@PreAuthorize` expression as `POST /reports` in `ReportController`.

## Why a second report endpoint

`POST /reports` (see `APP_OVERVIEW.md`, section "3. Financial Report") lists regular `Transaction` rows flat and only surfaces credit card spending as *pending invoices*. This report answers a different question — "where did the money go in this period?" — so it:

- groups everything by **Category → SubCategory**, with the individual items embedded in each subgroup;
- pulls credit card purchases (`CreditCardTransaction`) into the listing **by invoice month** (`referenceMonth`), each one telling *which card* it belongs to;
- summarizes `totalIncome`, `totalExpense` and `balance = totalIncome - totalExpense` for the period.

## Request

`CategoryReportFilterRequest` = the 8 fields of `ReportFilterRequest` (`spaceId, from, to, userId, bankAccountId, categoryId, subCategoryId, type`) **plus `creditCardId`**. Only `spaceId` is mandatory (`DomainException` → 422).

## Response shape

```
CategoryReportResponse
├── totalIncome / totalExpense / balance
└── groups: CategoryReportGroupResponse[]            (ordered by categoryName asc, null bucket last)
    ├── categoryId / categoryName                    (null => "no category" bucket)
    ├── totalIncome / totalExpense / total           (total = income - expense, signed)
    ├── incomePercentage / expensePercentage         (share of the report totals — RPTC9)
    └── subGroups: CategoryReportSubGroupResponse[]  (ordered by subCategoryName asc, null bucket last)
        ├── subCategoryId / subCategoryName          (null => "no subcategory" bucket)
        ├── totalIncome / totalExpense / total
        └── items: CategoryReportItemResponse[]      (ordered by date desc)
```

`CategoryReportItemResponse`: `{ id, source (TRANSACTION|CREDIT_CARD), type, date, description, amount, userId, bankAccountId, creditCardId, creditCardName, installmentNumber, totalInstallments, totalAmount, referenceMonth }`. `id`s can collide across the two sources — consumers must key on `source + id`. For `CREDIT_CARD` items, `type` is always `EXPENSE`; the item still enters/leaves the report based on its `referenceMonth` (RPTC1), but `date` keeps showing the original `purchaseDate` as a reference of when the purchase happened, and `bankAccountId` comes from the card's linked account (nullable); `creditCardId`/`creditCardName` satisfy the "which card" requirement. `totalAmount` is the sum of every installment's `amount` in the group (computed on read via `findByInstallmentGroupId`, not persisted) for `CREDIT_CARD` items — equal to `amount` itself for a single (`totalInstallments <= 1`) card purchase, and the full original purchase total for an installment purchase, shown as a reference alongside the per-installment `amount`; it's `null` for `TRANSACTION` items. `referenceMonth` is `purchase.getReferenceMonth()` for `CREDIT_CARD` items (the same value used to filter the item into the report, per RPTC1) and `null` for `TRANSACTION` items — lets the frontend show both "when it was bought" (`date`) and "which invoice it's on" (`referenceMonth`) side by side, since they can differ.

Grouping and sums happen **in memory** (stream + `LinkedHashMap` after sorting), same philosophy as `ListCreditCardInvoicesService` — no SQL `GROUP BY` anywhere in this codebase.

## Invariants

- **RPTC1 — invoice-month accrual.** Credit card purchases enter the report by `CreditCardTransaction.referenceMonth` (via the same spaceId-scoped `findByFilter` specification used by `GET /credit-card-transactions`, whose `from`/`to` range is matched against `referenceMonth`, not `purchaseDate`). Each installment of a parceled purchase carries its own `referenceMonth`, so a 3x purchase shows up as one item per month across three consecutive periods, matching which invoice it's actually billed on — not bunched into the month of the original purchase. `date` in the response item still shows `purchaseDate` (unchanged), purely as a reference of when the purchase happened.
- **RPTC2 — no double counting.** Regular transactions with `sourceType = CREDIT_CARD_INVOICE_PAYMENT` are excluded. The expense already entered through RPTC1; also counting the invoice payment would count it twice. `BILL_INSTANCE_PAYMENT` transactions stay in — they *are* the real expense.
- **RPTC3 — no transfers.** `TRANSFER` transactions are excluded entirely (they carry no category and are already excluded from the sums of `POST /reports`). `type=TRANSFER` therefore yields an empty report.
- **RPTC4 — no payment method concept (removed).** `PaymentMethod` and `paymentMethodId` were removed from the whole app. There used to be a `paymentMethodId = -1` sentinel here to isolate credit card items via the payment-method filter; it's gone. Credit card purchases now simply show up by default alongside regular transactions whenever no filter narrows them out (see RPTC5/RPTC7 below) — the only way to see *only* card purchases is `creditCardId` (RPTC7), and the only way to see *only* regular transactions is... there isn't one anymore; use the flat report (`POST /reports`) instead if that's needed.
- **RPTC5 — type vs card items.** Card items are implicitly expenses (`CreditCardTransaction` has no type field), so they are only included when `type` is `null` or `EXPENSE`.
- **RPTC6 — bank account reaches the card.** `CreditCard` gained an optional `bankAccount` relation (`bank_account_id` FK, validated on create/update to belong to the card's space). When `bankAccountId` is filtered, a card item is kept only if its card's linked account matches; a card with no linked account is excluded. Applied in memory after the query (the linkage lives on the card, not the purchase).
- **RPTC7 — specific card filter.** `creditCardId` narrows the report to that card's purchases and excludes regular transactions altogether (filtering by a card means "this card's spending"). It is passed straight to `CreditCardTransactionRepository.findByFilter`, which already supported it.
- **RPTC8 — intentionally different totals.** For the same period, totals here diverge from `POST /reports`: this report is on an invoice-month (accrual) basis — a purchase's cost is spread across the months it's actually billed on; `/reports` is on a bank-movement basis (it counts invoice payments when they happen and knows nothing of individual purchases or installments). Both are correct answers to different questions.
- **RPTC9 — percentages.** Per group, `expensePercentage = group.totalExpense / report.totalExpense × 100` and `incomePercentage` analogously, `BigDecimal` scale 2 `HALF_UP`; `0.00` when the report total is zero (no division by zero). Subgroups carry no percentages.

The `userId` filter applies to both sources — `CreditCardTransactionRepository.findByFilter` gained a `userId` parameter for this (its only other caller, `ListCreditCardTransactionsService`, passes `null`).

## Seed / permissions

`seed.sql` section 1 gained the `/reports/by-category` API row and section 2 the FRONT_PAGE row — both reuse `name = 'Relatórios'` so ADMIN/MEMBER inherit ALLOW from the section-5 name lists on a fresh database (same trick as `/bank-accounts`). Section 4 adds the "Relatório por Categoria" sidebar item under "Financeiro", and the idempotent **section 13** covers databases that ran section 5 before this endpoint existed. Endpoint matching is a full-string regex, so `/reports/by-category` never collides with the `/reports` row.

Frontend counterpart: `frontend/docs/reports-by-category.md`.

# System categories

Entries the application generates by itself — credit card invoice payments and transfers between
accounts — used to sit awkwardly in the category model. The invoice payment forced the user to pick
a category that no report ever read again (the purchases *inside* the invoice are what carry real
categories), and a transfer was the one transaction type exempt from the category rule, so it fell
into the "Sem categoria" bucket.

Both now get a **reserved category**, assigned automatically, that the user never types and cannot
pick by hand.

## The reserved names

`domain/enums/SystemCategory` is the single place these names live:

| Code | Category | Subcategory |
|---|---|---|
| `CREDIT_CARD_INVOICE_PAYMENT` | `Pagamento de Fatura` | `Fatura de Cartão` |
| `TRANSFER` | `Transferência` | — (none; `subCategoryId` stays null) |

`TRANSFER` deliberately has no reserved subcategory: one named after its own category would add no
information, and `subCategoryId` is optional everywhere.

## Why the name is the key, and not a new column

Categories belong to a `Space`, so there is one physical row per space — the FK on `Transaction`
keeps pointing at the space's own category and multi-tenancy is untouched. What is global is the
**name**, held as a constant in the enum above; lookups are `(spaceId, name)`.

There is no `system_code` column. Two things hold the name up as a key:

1. **System categories are locked** — `UpdateCategoryService` and friends reject any rename,
   status change or delete, so the name cannot drift.
2. **Names are now unique** — `CategoryNameValidator` rejects a duplicate category name within a
   space, and a duplicate subcategory name within a category. Neither existed before (there is no
   `@UniqueConstraint` on `categories` either). Comparison is `trim()` + `equalsIgnoreCase`, done
   in Java rather than delegated to the MySQL collation; accents are significant, so
   "Transferencia" and "Transferência" are different names.

`Category.isSystem()` is therefore derived (`SystemCategory.fromCategoryName(name) != null`) and
`SubCategory.isSystem()` follows its parent. Residual risk accepted: renaming a row directly in SQL
would make it stop being recognised as a system category — the ensure below self-heals by
recreating it on the next read.

## Provisioning — `EnsureSystemCategoriesService`

Mirrors the existing lazy-ensure idiom of `EnsureRecurringBillsGeneratedService`: `@Transactional`
on the method, `execute(Long spaceId)`, idempotent via an existence check.

Invoked from two places:

- the first line of `ListCategoriesService.execute(spaceId)` — every frontend consumer goes through
  `GET /categories`, so this one hook covers all of them, including spaces that already existed;
- `CreateSpaceService.execute`, right after the space is saved, for eager provisioning.

`CreateSpaceService.execute` is **not** `@Transactional` (atomicity there is per-repository), so the
provisioning must not be something the space creation depends on — it isn't: the ensure is
idempotent and the `ListCategoriesService` hook covers any failure.

**Adoption.** If a space already has a hand-made category carrying a reserved name, the ensure
**adopts** that row instead of creating a second one. That keeps the operation idempotent and avoids
a duplicate; the side effect is that the category becomes locked. Rare, and intentional.

`ResolveSystemCategoryService.execute(spaceId, code)` runs the ensure and returns a
`SystemCategoryPair(categoryId, subCategoryId)` — this is what `PayCreditCardInvoiceService` and the
transfer branch of `CreateTransactionService`/`UpdateTransactionService` call.

## Guards

`domain/SystemCategoryPolicy` (a stateless static helper, same shape as `CreditCardInvoiceCycle`)
rejects a system category reaching a user-owned entry:

- `rejectManualSelection(category, subCategory, sourceType)` — used by `CreateTransactionService`.
  A non-null `sourceType` means the application generated the row, so it passes; the public
  `execute(request)` overload passes `sourceType = null` and is therefore blocked.
- `rejectSystemSelection(category, subCategory)` — used everywhere a user-supplied `categoryId`
  reaches persistence: `UpdateTransactionService`, `CreateBillService`,
  `UpdateBillRecurringService`, `CreateBillInstanceService`, `UpdateBillService`,
  `CreateCreditCardTransactionService`, `UpdateCreditCardTransactionService`,
  `CreateCreditCardTransactionRecurringService`, `UpdateCreditCardTransactionRecurringService`.

Editing/deactivating/deleting a system category or subcategory is rejected in
`UpdateCategoryService`, `UpdateCategoryStatusService`, `DeleteCategoryService`,
`UpdateSubCategoryService`, `UpdateSubCategoryStatusService` and `DeleteSubCategoryService`;
`CreateSubCategoryService` also refuses to hang a user subcategory under a system category.

## Effect on the reports

**`GenerateCategoryReportService` did not change.** It already excluded
`sourceType = CREDIT_CARD_INVOICE_PAYMENT` (RPTC2) and `TRANSFER` (RPTC3), which is what prevents
double counting — the purchases inside the invoice enter the report on their own, with their own
categories. Those two exclusions remain essential because historical rows were **not** migrated:
invoice payments made before this change still carry the category the user picked by hand, and old
transfers still have a null category.

So a system category never contributes to the by-category report. It exists as a label and as a
filter key in the statement (`POST /reports`), which is why `pages/reports/index.vue` keeps system
categories in its dropdown while `pages/reports/by-category/index.vue` filters them out — offering
an option there that can never return a row would be misleading.

## Frontend

`CategoryResponse` and `SubCategoryResponse` expose a derived `boolean system`. Entry dialogs filter
system categories out of their `categoryItems` computed — never at fetch time, because the id→name
lookups still need them to render the label in the statement. `pages/categories/index.vue` shows a
"Sistema" chip and disables edit/toggle/delete; `ManageSubCategoriesDialog` does the same for the
subcategory rows and hides the "Adicionar" row.

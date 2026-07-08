# Reports

Status: **implemented and working** (backend already existed; this doc covers the frontend
piece — Grupo F5 of `backend/IMPLEMENTATION_PLAN.md`). Read-only page — no create/edit/delete,
reuses the transaction row formatting from [`transactions.md`](./transactions.md) (F4).

## Backend

`ReportController` (`/reports`), already implemented with `@PreAuthorize`. Unlike the other
F1-F4 endpoints, this is a single `POST` (not `GET` + query params) because the filter shape
mirrors `ReportFilterRequest` closely and is generated fresh on every call — no persisted
report entity.

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/reports` | `ReportFilterRequest` | `ReportResponse` |

`ReportFilterRequest`: `{ spaceId, from, to, userId, bankAccountId, categoryId, subCategoryId,
paymentMethodId, type }` — only `spaceId` is enforced server-side (`GenerateReportService`
throws if missing); everything else, including `from`/`to`, is optional as far as the backend
is concerned. `from`/`to` being "obrigatórios" is a **frontend-only** UX decision (per the
plan) — enforced client-side via `VForm` validation before the request is sent.

`ReportResponse`: `{ transactions: TransactionResponse[], totalIncome, totalExpense, balance,
currentBalance, pendingCreditCardInvoices, pendingCreditCardTotal, pendingBillInstances,
pendingBillTotal, projectedBalance }`.
`totalIncome`/`totalExpense` are summed via `Transaction.isIncome()`/`isExpense()`, which
naturally exclude `TRANSFER` — it still appears in the `transactions` list, just not in the
totals. `balance = totalIncome - totalExpense`.

### Projected balance (Grupo FRPT1, added after Cartão de Crédito / Contas a Pagar)

- `currentBalance` — sum of active `BankAccount.balance` in the space (or just the filtered
  account, if `bankAccountId` is set).
- `pendingCreditCardInvoices` (`{creditCardId, creditCardName, referenceMonth, dueDate, amount}[]`)
  / `pendingCreditCardTotal` — open (unpaid) credit card invoices whose `dueDate` falls inside
  `[from, to]`. Since installments are materialized at purchase time, this already includes
  future installments of a parceled purchase without any extra generation step.
- `pendingBillInstances` (`{billInstanceId, billId, billName, referenceMonth, dueDate, amount}[]`)
  / `pendingBillTotal` — `PENDING` `BillInstance` rows whose `dueDate` falls inside `[from, to]`
  (the backend runs `EnsureBillInstancesGeneratedService` first, so a never-visited future month
  is generated on demand).
- `projectedBalance = currentBalance - pendingCreditCardTotal - pendingBillTotal`.

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/reports/index.vue` | The entire feature — filter form, summary cards, transaction table (no dialogs, no row actions) |
| `server/api/reports/index.post.ts` | Proxies `POST /reports` |

Filter dropdowns reuse the same list endpoints as F1-F4 (`/api/bank-accounts`,
`/api/categories`, `/api/payment-methods`), plus `GET /api/spaces/{id}/members` (already built
for `pages/users/index.vue`) for the "Membro" filter.

## Page behavior

- On mount / `spaceStore.activeSpace` change: fetches all filter dropdown data, then
  immediately generates a report for the default period (current month) so the page isn't
  empty on first load.
- Filter form (`Filtros` card): `De`/`Até` (required dates, defaulting to the current month),
  `Membro`, `Tipo` (Todos/Receita/Despesa/Transferência), `Conta`, `Categoria` → `Subcategoria`
  (cascading, same pattern as F4), `Forma de pagamento` — all optional except the date range.
  "Gerar Relatório" validates the form (blocks on missing `from`/`to`) and re-POSTs.
- Summary cards use the template's existing `CardStatisticsVerticalSimple` component (reused,
  not reinvented) for **Total de Receitas** (green), **Total de Despesas** (red), and **Saldo do
  Período** (neutral/`primary` if non-negative, `error` only if negative) — deliberately calm,
  not alarming, per `PRODUCT.md`'s "errors are informative, not alarming" principle applied to
  a negative balance.
- Transaction table columns and formatting are identical to F4's list (date, type chip,
  account, category/destination, payment method, description, signed/colored amount) minus the
  "Ações" column — this page has no edit/delete.
- A second `VRow` below the period cards shows **Saldo Atual** (`currentBalance`) and **Saldo
  Previsto** (`projectedBalance`, `success`/`error` by sign, same calm-not-alarming convention as
  "Saldo do Período").
- Two more cards ("Faturas Pendentes" / "Contas Pendentes") render below, each only when its
  list is non-empty, listing card/bill name, reference month, due date, amount, and a total row
  — no row actions (read-only, same as the transaction table on this page).

## Manual verification

No frontend test suite exists in this project — verified manually: `pnpm dev`, open
`/reports`, confirm the default (current month) report loads with correct totals, change the
period and a couple of filters and regenerate, confirm a `TRANSFER` transaction shows in the
table but doesn't affect `totalIncome`/`totalExpense`/`balance`. Filter a period covering future
pending invoices/bill instances and confirm `projectedBalance` matches
`currentBalance - pendingCreditCardTotal - pendingBillTotal`, and that both pending lists list
the right rows.

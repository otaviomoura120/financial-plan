# Report by Category (Relatório por Categoria)

Status: **implemented and working**. Read-only page — no create/edit/delete. Reuses the same
filter form as [`reports.md`](./reports.md), plus a credit-card filter and a "Agrupar por
categoria" toggle. Backend counterpart is documented in `backend/docs/report-by-category.md`
(invariants RPTC1-RPTC9). There is no `PaymentMethod` concept anywhere in the app anymore —
it used to have a payment-method filter here with a hardcoded "Cartão de Crédito" sentinel
option; that filter was removed along with the rest of the `PaymentMethod` feature. Credit
card purchases now simply show up by default alongside regular transactions; the `creditCardId`
filter (a separate, still-existing dropdown) narrows to one specific card's purchases.

## Backend

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/reports/by-category` | `CategoryReportFilterRequest` | `CategoryReportResponse` |

`CategoryReportFilterRequest`: the same 8 fields as `ReportFilterRequest`
(`spaceId, from, to, userId, bankAccountId, categoryId, subCategoryId, type`)
**plus `creditCardId`**. Only `spaceId` is enforced server-side; `from`/`to` being
"obrigatórios" is the same frontend-only UX decision as `/reports`.

Special filter semantics (full rationale in the backend doc):

- `creditCardId` set — only purchases of that card, and regular transactions are excluded
  altogether. When no filter narrows either source, credit card purchases and regular
  transactions both appear together by default.
- `type` — card items only appear for `null` (Todos) or `EXPENSE`; `TRANSFER` is excluded from
  this report entirely (the Tipo select has no "Transferência" option here).
- `bankAccountId` reaches card purchases through the card's linked bank account
  (`CreditCard.bankAccountId`, set on the credit-card form).

`CategoryReportResponse`:

```
{
  totalIncome, totalExpense, balance,
  groups: [{
    categoryId, categoryName,            // null => "Sem categoria"
    totalIncome, totalExpense, total,    // total = income - expense (signed)
    incomePercentage, expensePercentage, // share of the report totals, scale 2
    subGroups: [{
      subCategoryId, subCategoryName,    // null => "Sem subcategoria"
      totalIncome, totalExpense, total,
      items: [{
        id, source,                      // 'TRANSACTION' | 'CREDIT_CARD' (ids may collide across sources)
        type, date, description, amount,
        userId, bankAccountId,
        creditCardId, creditCardName,    // filled for CREDIT_CARD items — "which card"
        installmentNumber, totalInstallments
      }]
    }]
  }]
}
```

Credit-card items enter by **purchase date** (`purchaseDate`), never by invoice closing or
payment date; the matching `CREDIT_CARD_INVOICE_PAYMENT` transactions are excluded to avoid
double counting.

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/reports/by-category/index.vue` | The entire feature — filter form, summary cards, grouped/flat table |
| `server/api/reports/by-category.post.ts` | Proxies `POST /reports/by-category` |

Filter dropdowns reuse `/api/bank-accounts`, `/api/categories`,
`/api/spaces/{id}/members`, plus `/api/credit-cards` for the "Cartão de Crédito" filter.

## Page behavior

- On mount / `spaceStore.activeSpace` change: fetches filter data then auto-generates the
  report for the current month (same as `/reports`).
- Filters: `De`/`Até` (required), `Membro`, `Tipo` (Todos/Receita/Despesa — no Transferência),
  `Conta`, `Categoria` → `Subcategoria` (cascading),
  **Cartão de Crédito** (specific card, with a hint that it narrows to that card's purchases),
  and the **Agrupar por categoria** checkbox (default on).
- Summary cards (`CardStatisticsVerticalSimple`): Total de Receitas, Total de Despesas, Saldo
  do Período — same conventions as `/reports`. No current/projected balance on this page.
- **Grouped mode** (checkbox on): a `VTable` with two expansion levels kept in two
  `Set<string>` refs (`expandedCategories` keyed `categoryId ?? 'none'`, `expandedSubGroups`
  keyed `catKey:subId ?? 'none'`, both reset on each generation). Category rows show name
  ("Sem categoria" fallback), a "% do Período" chip (`expensePercentage` "das despesas" when
  the group has expenses, otherwise `incomePercentage` "das receitas"), green income, red
  expense, and the signed total. Expanding a category reveals subcategory rows (same totals,
  no percentage); expanding a subcategory reveals a nested compact table of items: date,
  origin, description, signed/colored amount.
- **Origin column**: `CREDIT_CARD` items render a chip with the `tabler-credit-card` icon and
  the card name, plus an `n/total` installment chip when parceled; `TRANSACTION` items render
  the bank account name resolved via the local lookup map.
- **Flat mode** (checkbox off): presentation-only — the same response is flattened client-side
  into a single table (date, category/subcategory, origin, description, amount) sorted by date
  desc. No new request is made when toggling.
- Item row `:key` is `` `${item.source}-${item.id}` `` because ids can collide across the two
  sources.

## Manual verification

No frontend test suite — verified manually: `pnpm dev`, open `/reports/by-category` (menu item
"Relatório por Categoria" under Financeiro; requires seed.sql section 13 on an existing DB).
Create a regular transaction, a card purchase inside the period (invoice due next month) and
pay an invoice, then confirm: the purchase appears under its category with the card chip; the
invoice-payment transaction does **not** appear; totals and percentages match. Exercise the
filters: with no filter, both the transaction and the card purchase appear together; the card
filter narrows to only that card's purchases and excludes regular transactions; Tipo=Receita
hides card items; Conta keeps only purchases of cards linked to that account. Toggle "Agrupar
por categoria" and confirm the flat list is date-sorted with the same items.

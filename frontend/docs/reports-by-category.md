# Report by Category (Relatório por Categoria)

Status: **implemented and working**. Read-only page — no create/edit/delete. Reuses the same
filter form as [`reports.md`](./reports.md), plus a credit-card filter, a hardcoded
"Cartão de Crédito" payment-method option, and a "Agrupar por categoria" toggle. Backend
counterpart is documented in `backend/docs/report-by-category.md` (invariants RPTC1-RPTC9).

## Backend

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/reports/by-category` | `CategoryReportFilterRequest` | `CategoryReportResponse` |

`CategoryReportFilterRequest`: the same 9 fields as `ReportFilterRequest`
(`spaceId, from, to, userId, bankAccountId, categoryId, subCategoryId, paymentMethodId, type`)
**plus `creditCardId`**. Only `spaceId` is enforced server-side; `from`/`to` being
"obrigatórios" is the same frontend-only UX decision as `/reports`.

Special filter semantics (full rationale in the backend doc):

- `paymentMethodId = -1` is a **hardcoded sentinel** meaning "Cartão de Crédito" — the report
  returns only credit-card purchase items. A real (positive) `paymentMethodId` returns only
  regular transactions (card purchases have no payment method).
- `creditCardId` set — only purchases of that card.
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
        userId, bankAccountId, paymentMethodId,
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

Filter dropdowns reuse `/api/bank-accounts`, `/api/categories`, `/api/payment-methods`,
`/api/spaces/{id}/members`, plus `/api/credit-cards` for the new "Cartão de Crédito" filter.

## Page behavior

- On mount / `spaceStore.activeSpace` change: fetches filter data then auto-generates the
  report for the current month (same as `/reports`).
- Filters: `De`/`Até` (required), `Membro`, `Tipo` (Todos/Receita/Despesa — no Transferência),
  `Conta`, `Categoria` → `Subcategoria` (cascading), `Forma de pagamento` (real methods plus
  the hardcoded **Cartão de Crédito** option, value `-1`, with a persistent hint),
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
  "account · payment method" resolved via the local lookup maps.
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
filters: a real payment method hides card items; "Cartão de Crédito" shows only card items;
the card filter narrows to one card; Tipo=Receita hides card items; Conta keeps only purchases
of cards linked to that account. Toggle "Agrupar por categoria" and confirm the flat list is
date-sorted with the same items.

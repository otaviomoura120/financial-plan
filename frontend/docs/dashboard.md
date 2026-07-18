# Dashboard (Home — `/`)

Status: **implemented and working**. The home page (`pages/index.vue`) is a set of 4 independent
"widget" cards plus a row of 4 hero stat tiles, all reusing existing report/bills/credit-card/bank-
account endpoints — **no new financial-data backend endpoint was created for this feature**, only a
permissions endpoint (see "RBAC" below).

## Backend

No new financial-data endpoint. Each widget composes existing, already-documented endpoints:

| Widget | Endpoint(s) used |
|---|---|
| Contas a Pagar Vencendo | `GET /bills/instances`, `GET /credit-cards/invoices` (see `bills.md`, `credit-cards.md`) |
| Saldo Atual das Contas | `GET /bank-accounts` (see `bank-accounts.md`) |
| Gastos por Categoria e Subcategoria | `POST /reports/by-category` (see `reports-by-category.md`) |
| Total de Gasto em Cada Cartão | `GET /credit-cards`, `GET /credit-cards/invoices` (see `credit-cards.md`) |

The one new backend piece is `GET /dashboard-widgets?spaceId=`, which decides which of these cards a
role is even allowed to see — full contract in `backend/docs/dashboard-widgets.md`.

## Frontend — file map

| File | Purpose |
|---|---|
| `pages/index.vue` | Orchestration only — no data fetching of its own. Fetches allowed widget keys, renders the hero row + 4 widget rows behind `v-if`, holds the 4 hero-tile values fed by each widget's emitted totals |
| `components/dashboard/DashboardDueThisWeekCard.vue` | Bills + unpaid credit card invoices due within a rolling window (7/15/30 days, chip filter). `VTimeline`, urgency-colored dots |
| `components/dashboard/DashboardAccountBalancesCard.vue` | Grid of active bank account balances. No filter — it's a live snapshot |
| `components/dashboard/DashboardCategorySpendingCard.vue` | Expense donut by category (Este Mês / Mês Passado / Últimos 30 Dias / Este Ano chip filter), expandable subcategory breakdown, link to `/reports/by-category` |
| `components/dashboard/DashboardCreditCardSpendingCard.vue` | Per-card spend vs. limit (Fatura Atual / Fatura Anterior chip filter), progress bar colored by usage tier |
| `utils/dashboardWidgets.ts` | The 5 widget key constants, auto-imported, must match the backend seed's `endpoint` values exactly |
| `server/api/dashboard-widgets/index.get.ts` | Proxies `GET /dashboard-widgets` |

## Page behavior — each widget is self-sufficient

Unlike `pages/reports/index.vue` (one page-level `Promise.all`), **every dashboard card fetches its own
data**, reacting to its own `spaceId` prop and its own local filter — closer to how
`CreditCardInvoicesDialog.vue`/`CreditCardTransactionsDialog.vue` fetch reactively when opened. This was
a deliberate choice once each card got its own period filter: a single page-level fetch can't serve 4
independently-changing date ranges cleanly. Consequences:

- Each card owns its own `isLoading`/`useApiError()`/`ApiErrorAlert` — there's no single page-level
  loading state for the widgets themselves (only for the initial "which widgets am I allowed to see"
  fetch, see RBAC below).
- Each filterable card emits `update:total` (number) and `update:period-label` (string) whenever its
  data or filter changes; `pages/index.vue` only listens and forwards those into the 4 hero tiles —
  it does not compute any total itself. This means **the hero tiles have no independent data source**:
  if a card is unmounted (permission denied), its corresponding tile has nothing feeding it, which is
  why the tile is also gated by that same widget's key (see RBAC below), not just by
  `dashboard:summary-tiles`.
- Filters are chip groups (`VChipGroup mandatory`) rendered in a `VCardText` row directly under the
  card's `title`, **not** in the `VCard`'s `#append` slot — an earlier version put them in `#append` and
  the title truncated/collided with the chips at typical card widths (confirmed visually, see the two
  screenshots that prompted the fix). Any new filterable card should follow the `VCardText` pattern, not
  `#append`.

### Due-this-week merge (`DashboardDueThisWeekCard.vue`)

Bills (`Bill.status === 'PENDING'`) and unpaid credit card invoices (`!paid`) are fetched with the same
`from`/`to` window and merged into one discriminated list (`{ type: 'bill' | 'invoice', ... }`), sorted
by `dueDate` ascending. An invoice has no numeric id, so its list key is
`` `invoice-${creditCardId}-${referenceMonth}` ``.

### Current/previous invoice selection (`DashboardCreditCardSpendingCard.vue`)

Fetches a wide 3-month window (`1st of last month` → `last day of next month`) regardless of the
selected filter, so both "Fatura Atual" and "Fatura Anterior" are always already in memory — switching
the chip only re-picks from data already fetched, no extra request. Per card, invoices are sorted by
`dueDate` descending; index `0` = "Fatura Atual", index `1` = "Fatura Anterior". A card with no invoice
at the selected index still renders (R$ 0,00, zeroed progress bar) rather than disappearing — cards are
never hidden just because a cycle has no spend yet.

### Category donut palette (`DashboardCategorySpendingCard.vue`)

The app's Vuetify theme (`plugins/vuetify/theme.ts`) has only 6 semantic colors, and `primary`/`success`
are the **same hex** (`#3E6B4F`) — reusing theme colors as categorical series would collide and would
also repurpose reserved status colors (success/warning/error) as arbitrary identity, which is wrong.
Instead this card defines its own small **validated categorical palette** (6 hues, fixed order = ranking
by `totalExpense` desc, never cycled; separate light/dark variants; a neutral grey for the "Outros"
fold-bucket beyond the top 6 categories) — validated with the `dataviz` skill's
`scripts/validate_palette.js` (lightness band, chroma floor, CVD-safe adjacent contrast, contrast vs.
surface, both light and dark). If this palette is ever changed, re-run that validator before shipping —
don't eyeball new hex values.

## RBAC — which widgets a role can see

`pages/index.vue` fetches `GET /api/dashboard-widgets?spaceId=` once per space change (same `watch` that
already exists for `spaceStore.activeSpace`), stores the result as `Set<string>`, and gates the template
with `v-if="allowedWidgets.has(KEY)"` — a denied widget is fully unmounted (no placeholder), the
surrounding grid reflows.

Two-level gating for the hero row specifically:
- The whole row needs `dashboard:summary-tiles` allowed.
- Each tile **inside** that row is additionally gated by the key of the widget that feeds its data
  (e.g. the "A Pagar" tile needs `dashboard:due-this-week`, not just `summary-tiles`) — a role can end
  up seeing 0 to 4 tiles. This mirrors the data-coupling explained above: there's no reason to show a
  tile whose only data source isn't mounted.
- The category-spending/credit-card-spending `VRow` (the only row holding two independently-gated
  widgets side by side) has its own `v-if` on "either is allowed", and each `VCol`'s `md` width falls
  back to `12` when its sibling is absent, so a lone remaining widget doesn't stay pinned at `7`/`5`
  columns.

While the permissions fetch is in flight (or no space is active yet), the page shows a single centered
`VProgressCircular` instead of any widget — this avoids a flash of content a role isn't allowed to see.

Full backend contract, the `EndpointPermissionType.WIDGET` rationale, and the ALLOW/DENY default
mechanics: `backend/docs/dashboard-widgets.md`.

## Manual verification

No frontend test suite exists in this project — verified manually via `pnpm dev`:
- Open `/` with a role that has all 5 widgets `ALLOW`ed — confirm all 4 hero tiles and all 4 cards
  render, matching the data shown on their respective dedicated pages (`/bills`, `/bank-accounts`,
  `/reports/by-category`, `/credit-cards`).
- Toggle each card's period filter and confirm its own data refetches/recomputes without affecting the
  other cards; for the credit-card widget confirm switching Atual/Anterior does **not** trigger a new
  network request (data was already fetched).
- Via the Roles screen, `DENY` one widget (e.g. `dashboard:due-this-week`) for a test role; as a user
  with that role, confirm the card disappears entirely, its hero tile disappears too, the other 3 tiles
  remain, and the layout closes the gap.
- `DENY` `dashboard:summary-tiles` alone; confirm the whole hero row disappears even though the 4
  detail cards below remain visible.
- Confirm dark mode: the category donut's colors and tooltip switch to the dark-mode palette, not an
  automatic filter/invert of the light one.

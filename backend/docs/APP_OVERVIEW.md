# Financial Plan — App Overview

## Purpose & Vision

**Financial Plan** is a personal and family financial control platform. Its goal is to give users full visibility and control over their financial life: tracking every income and expense, managing bank accounts, organizing spending by category, and generating reports to understand where money is going.

This is the **first module** of a broader platform. The current scope covers:
- Bank account management (balance tracking)
- Income and expense recording (transactions)
- Category and subcategory organization
- Payment method tracking (cash, card, pix, etc.)
- Financial reports with filters and totals

Future modules planned: debts, credit/debit cards, bills, recurring payments, budgets, and investments. The architecture is explicitly designed to accommodate new modules without restructuring existing ones.

---

## Architecture Overview

| Layer | Purpose |
|---|---|
| `domain/` | Pure business models (POJOs), repository interfaces (contracts), enums, exceptions |
| `application/` | Use cases — one service class per operation; DTOs as Java records |
| `infrastructure/` | REST controllers, JPA entities, repository implementations, security config |

**Stack:** Java + Spring Boot 4.0.6 · Spring Data JPA · MySQL 8 · Auth0 (authentication) · Gradle · Groovy/Spock (tests)

**Key design decisions:**
- Domain models have zero JPA/Spring annotations; they are pure POJOs with a `validate()` method and business methods
- JPA entities (`*EntityJpa`) live only in infrastructure and are mapped to/from domain models inside `*RepositoryImpl`
- All entities use version-based **optimistic locking** — `setVersion()` throws `ObjectOptimisticLockingFailureException` on mismatch
- DTOs are always Java `record` types
- Exception mapping: `DomainException` → 422, `ObjectOptimisticLockingFailureException` → 423, generic → 500

---

## Core Concepts

### Space
A **Space** is a collaborative financial workspace — think "Family Budget" or "Personal Finances". All financial data (bank accounts, categories, transactions) is scoped to a space. A user can belong to multiple spaces.

### User
Authenticated via **Auth0**. The `auth0Sub` field links the app user to the Auth0 identity. One user can be a member of multiple spaces with different roles in each.

### SpaceMember + Role
Every user in a space has a **Role** (e.g., `OWNER`, `VIEWER`). The `OWNER` role is special and protected. Roles control which API endpoints a user can call, evaluated dynamically via `EndpointPermission` records.

### BankAccount
Represents a real financial account (bank, digital wallet, etc.). Tracks a running `balance`. Supports `credit(amount)` and `debit(amount)` operations. Scoped to a Space.

### Category + SubCategory
Two-level classification hierarchy for transactions. Example: Category = "Food", SubCategory = "Groceries". Both are scoped to a Space and can be deactivated (soft delete).

### PaymentMethod
Defines how a transaction was paid — cash, credit card, debit card, pix, check, etc. Scoped to a Space.

### Transaction
The **central entity** of the system. Represents a single financial event:
- `type`: `INCOME`, `EXPENSE` or `TRANSFER`
- `amount`: positive `BigDecimal`
- `transactionDate`: `LocalDate` of when it happened
- Links: `userId`, `bankAccountId`, `destinationBankAccountId`, `categoryId`, `subCategoryId`, `paymentMethodId`
- `description`: optional notes
- `sourceType` / `sourceId`: optional traceability of an external origin (`TransactionSourceType`: `CREDIT_CARD_INVOICE_PAYMENT`, `BILL_INSTANCE_PAYMENT`). Both are `null` for a transaction created directly through `POST /transactions`; they are only populated when a transaction is generated automatically by paying a credit card invoice or a bill instance (credit card/bills modules)

For `INCOME`/`EXPENSE`, `categoryId` and `paymentMethodId` are required and `destinationBankAccountId` must be null. For `TRANSFER`, `destinationBankAccountId` is required and must differ from `bankAccountId`; `categoryId`/`paymentMethodId` are not required. `TRANSFER` moves money between two bank accounts within the same space and is excluded from `totalIncome`/`totalExpense`/`balance` in Reports (see below), though it still appears in the transaction list.

`Transaction.isLinkedToSource()` returns `true` when `sourceType` is set. `UpdateTransactionService`/`DeleteTransactionService` reject (`DomainException`, HTTP 422) any edit or delete attempt on a linked transaction through the regular `/transactions` endpoints — reverting the balance effect of that kind of transaction is only allowed through a dedicated "undo payment" action in the module that created it (no cascade from the generic Transaction flow).

### EndpointPermission & RoleEndpointPermission
`EndpointPermission` defines one access rule for an HTTP endpoint or a frontend page:
- `endpoint`: Java regex matched against the request URI
- `permittedMethods`: comma-separated HTTP methods (`GET,POST,PUT`)
- `type`: `API` (backend route) or `FRONT_PAGE` (frontend route)
- `group`: optional grouping; `INTERNAL_MANAGEMENT` restricts to master admins

`RoleEndpointPermission` is a join entity linking a `Role` to an `EndpointPermission` with an `ALLOW` or `DENY` decision. On every protected request, `SecurityService` resolves the user's role in the **active space** (identified by the `X-Space-Id` request header) and checks whether that role has an `ALLOW` entry for the matching rule.

### GroupMenu / GroupMenuChildren
Hierarchical UI navigation menu. The menu structure is served to the frontend filtered by the authenticated user's permissions, so each user sees only the sections they can access.

### CreditCard
First entity of the new **credit card module** (in progress). Tenancy is direct, same pattern as `BankAccount`/`PaymentMethod`:
- `name`, `limit` (informative only — never blocks a purchase, same philosophy as `BankAccount`'s negative balance), `closingDay`/`dueDay` (1-31, day-of-month of the invoice's closing and due date), `active`
- `validate()` requires `name`, `space`, a positive `limit`, and `closingDay`/`dueDay` within 1-31
- `update(name, limit, closingDay, dueDay)` and `deactivate()`; optimistic locking via `setVersion`

`CreditCardInvoiceCycle` is a stateless calculator (no repository, no persisted state) used to derive which monthly invoice a purchase belongs to and when that invoice is due:
- `resolveClosingDate(YearMonth, closingDay)` — the closing date within that month, clamped to the month's last day (handles `closingDay=31` in a 28/30-day month)
- `resolveReferenceMonth(purchaseDate, closingDay)` — first day of the invoice month a purchase falls into: the purchase's own month if made on or before that month's closing date, otherwise the next month
- `resolveDueDate(referenceMonth, closingDay, dueDay)` — if `dueDay <= closingDay` the due date falls in the month **after** `referenceMonth` (the due date always trails the closing date by construction), otherwise within `referenceMonth` itself; always clamped to the resulting month's length

CRUD is exposed via `CreditCardController` (`/credit-cards`, see REST API Reference below), protected by `@PreAuthorize` from the start — unlike the core module, this credit card controller never had the T10 authorization gap. `DELETE /credit-cards/{id}` maps to `DeactivateCreditCardService`, which soft-deletes (`active=false`, same as `BankAccount`/`PaymentMethod` before their DEL2/DEL3 hard-delete rework) rather than removing the row — no hard delete/`existsBy` FK-guard exists yet for this entity. `seed.sql` grants `'Cartões de Crédito'` `ALLOW` to OWNER/ADMIN/MEMBER and adds its sidebar entry under the existing `'Contas e Pagamentos'` group.

---

## Key Flows

### 1. User Onboarding
```
POST /users          → create User (linked to Auth0 sub)
POST /spaces         → create Space → auto-creates OWNER role → adds creator as SpaceMember(OWNER)
POST /bank-accounts  → add bank account to the space
POST /categories     → add categories (and subcategories)
POST /payment-methods → add payment methods
```

### 2. Recording a Transaction
```
POST /transactions
  body: { type, userId, bankAccountId, destinationBankAccountId, categoryId, subCategoryId, paymentMethodId, amount, transactionDate, description }
  → CreateTransactionService validates that every referenced FK exists (userId, bankAccountId always;
    destinationBankAccountId only for TRANSFER; categoryId/paymentMethodId only for INCOME/EXPENSE;
    subCategoryId only when informed) → 422 DomainException if any is missing
  → Transaction.validate() checks required fields per type and amount > 0
  → TransactionBalanceEffectService.apply() credits/debits the bank account(s) involved (see transaction-balance-effect.md)
  → stored and returned as TransactionResponse (all inside one @Transactional boundary)

Editing a transaction (PUT /transactions/{id}) reverts the old balance effect and re-applies the new one — same
service, same rules for FK validation — so changing type/amount/bankAccountId/destinationBankAccountId always
leaves the affected bank account(s) with the correct resulting balance.
```

### 3. Financial Report
```
POST /reports
  body: { spaceId, from, to, userId?, bankAccountId?, categoryId?, subCategoryId?, paymentMethodId?, type? }
  → spaceId is required (422 DomainException if missing)
  → queries transactions matching all filters, scoped to bank accounts of that space
  → returns: { transactions[], totalIncome, totalExpense, balance }
```
`spaceId` is mandatory and enforces multi-tenant isolation: `Transaction` has no `spaceId` column of its own (only
`bankAccountId`), so `TransactionRepositoryImpl.findByFilter` restricts results with
`bankAccountId IN (SELECT id FROM bank_accounts WHERE space_id = :spaceId)` — a report for one space can never
return transactions whose bank account belongs to another space, regardless of the other filters supplied.

`TRANSFER` transactions are included in `transactions[]` but excluded from `totalIncome`/`totalExpense`/`balance` —
`Transaction.isIncome()`/`isExpense()` both return `false` for `TRANSFER`, so the sums naturally skip it without any
extra branching in `GenerateReportService`. Also delete a transaction reverts its `TransactionBalanceEffectService`
balance effect (see transaction-balance-effect.md), so recorded balances stay consistent for reporting purposes.

### 4. Access Control Flow
```
Incoming request  (must carry Authorization: Bearer <token> and X-Space-Id: <spaceId>)
  → Spring Security extracts Auth0 JWT  →  401 if invalid
  → @PreAuthorize calls SecurityService
      → userHasPermissionForURL  (reads X-Space-Id header)
          → resolves SpaceMember for (spaceId, userId)
          → checks RoleEndpointPermission ALLOW for that role + matching endpoint regex
      OR
      → userHasPermissionInSpace  (space from URL path variable)
      OR
      → userHasPermissionForRole  (space derived from role ID)
  → allowed or 403
```
All 5 core financial controllers (`BankAccountController`, `CategoryController`, `PaymentMethodController`,
`TransactionController`, `ReportController` — including their new `GET` list endpoints) now use
`@PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")` on every method, the same
pattern as `EndpointPermissionController`/`GroupMenuController`. Each endpoint has a corresponding `API` row in
`endpoint_permissions` (see `seed.sql`, section 1) reusing the same `name` as the entity's existing `FRONT_PAGE` row
(`'Contas Bancárias'`, `'Categorias'`, `'Formas de Pagamento'`, `'Transações'`, `'Relatórios'`), so ADMIN/MEMBER
inherit `ALLOW` automatically through the existing `ep.name IN (...)` joins — no change needed to the ADMIN/MEMBER
seed blocks. OWNER gets `ALLOW` on everything via the existing `CROSS JOIN`.

There is no isolated Spock test for `@PreAuthorize` itself in this project (see `SecurityServiceSpec.groovy`, which
tests the underlying matching logic generically). Verify manually per role before treating this as fully closed:
call each new/existing endpoint with a MEMBER/ADMIN token that has no `ALLOW` for it and confirm `403`, then with a
token that does and confirm success.

### 5. Managing Roles & Permissions
```
POST /roles           → create role in a space
PUT  /roles/{id}/assign-user/{userId}?spaceId=  → change member's role
POST /endpoint-permissions  → define which roles can access which endpoints
```

---

## REST API Reference

### Users `/users`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create user |
| PUT | `/{id}` | Update user profile |
| DELETE | `/{id}` | Deactivate user |

### Spaces `/spaces`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create space (caller becomes OWNER) |
| PUT | `/{id}` | Rename space |
| DELETE | `/{id}` | Delete space |
| GET | `/user/{userId}` | List all spaces a user belongs to |
| POST | `/{id}/members/{userId}` | Add member to space |
| DELETE | `/{id}/members/{userId}` | Remove member from space |

### Transactions `/transactions`
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/?spaceId=&userId=&bankAccountId=&categoryId=&subCategoryId=&paymentMethodId=&type=&from=&to=` | List transactions of a space (spaceId required, all other filters optional; no `from`/`to` = all transactions) |
| POST | `/` | Record income, expense or transfer |
| PUT | `/{id}` | Update transaction |
| DELETE | `/{id}` | Delete transaction |

### Categories `/categories`
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/?spaceId=` | List categories of a space, each with its subcategories populated |
| POST | `/` | Create category |
| PUT | `/{id}` | Rename category |
| PATCH | `/{id}/status` | Activate/deactivate category (`{active: boolean}`) |
| DELETE | `/{id}` | Delete category (hard delete; rejects with 422 if linked subcategories or transactions exist) |
| POST | `/subcategories` | Create subcategory |
| PUT | `/subcategories/{id}` | Rename subcategory |
| PATCH | `/subcategories/{id}/status` | Activate/deactivate subcategory (`{active: boolean}`) |
| DELETE | `/subcategories/{id}` | Delete subcategory (hard delete; rejects with 422 if linked transactions exist) |

### Bank Accounts `/bank-accounts`
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/?spaceId=` | List bank accounts of a space |
| POST | `/` | Create bank account |
| PUT | `/{id}` | Update account metadata |
| PATCH | `/{id}/status` | Activate/deactivate account (`{active: boolean}`) |
| DELETE | `/{id}` | Delete account (hard delete; rejects with 422 if linked transactions exist) |

### Payment Methods `/payment-methods`
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/?spaceId=` | List payment methods of a space |
| POST | `/` | Create payment method |
| PUT | `/{id}` | Rename payment method |
| PATCH | `/{id}/status` | Activate/deactivate payment method (`{active: boolean}`) |
| DELETE | `/{id}` | Delete payment method (hard delete; rejects with 422 if linked transactions exist) |

### Credit Cards `/credit-cards`
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/?spaceId=` | List credit cards of a space |
| POST | `/` | Create credit card (`name`, `limit`, `closingDay`, `dueDay`) |
| PUT | `/{id}` | Update name/limit/closingDay/dueDay |
| DELETE | `/{id}` | Deactivate credit card (soft delete — `active=false`, no hard delete yet) |

### Roles `/roles`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create role in a space |
| PUT | `/{id}` | Update role name/description |
| DELETE | `/{id}` | Delete role |
| GET | `/?spaceId={id}` | List roles for a space |
| PUT | `/{id}/assign-user/{userId}` | Change a member's role |

### Reports `/reports`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Generate filtered financial report |

### Endpoint Permissions `/endpoint-permissions`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Define access rule |
| PUT | `/{id}` | Update access rule |
| DELETE | `/{id}` | Remove access rule |
| GET | `/` | List all access rules |

### Group Menu `/group-menus`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create menu group |
| PUT | `/{id}` | Update menu group |
| DELETE | `/{id}` | Delete menu group |
| GET | `/` | List all menu groups with children |
| POST | `/{id}/children` | Add menu item to group |
| PUT | `/children/{childId}` | Update menu item |
| DELETE | `/children/{childId}` | Remove menu item |

### Menu Structure `/menu-structure`
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/` | Return role-filtered navigation menu for authenticated user |

---

## Adding a New Module

Follow this checklist when extending the platform with a new financial module (e.g., debts, recurring bills):

1. **Domain** — add new model class in `domain/` (POJO, `validate()`, business methods). Add repository interface in `domain/repository/`.
2. **Application** — create a package under `application/<module>/` with service classes (one per use case) and record DTOs.
3. **Infrastructure** — add JPA entity in `infrastructure/repository/jpa/`, repository implementation in `infrastructure/repository/`, and REST controller in `infrastructure/controller/`.
4. **Permissions** — seed `EndpointPermission` records for the new endpoints.
5. **Menu** — optionally add `GroupMenu` / `GroupMenuChildren` entries for UI navigation.
6. **Tests** — write Groovy/Spock unit tests for all new services. Run `./gradlew test` and the ArchUnit suite (`ArchitectureTest`) to confirm no layer rules were broken.

Spaces already provide the multi-tenancy boundary — new entities should reference a `Space` to keep data isolated per group.

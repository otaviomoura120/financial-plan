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
- `type`: `INCOME` or `EXPENSE`
- `amount`: positive `BigDecimal`
- `transactionDate`: `LocalDate` of when it happened
- Links: `userId`, `bankAccountId`, `categoryId`, `subCategoryId`, `paymentMethodId`
- `description`: optional notes

### EndpointPermission
Defines access control rules. Each record holds:
- `endpoint`: regex pattern matched against the request URL
- `permittedMethods`: comma-separated HTTP methods (`GET,POST,PUT`)
- `permittedRoles`: comma-separated role names that may access it
- `type`: `API` (backend route) or `FRONT_PAGE` (frontend route)

On every protected request, `SecurityService` checks the authenticated user's role in the target space against the matching `EndpointPermission`.

### GroupMenu / GroupMenuChildren
Hierarchical UI navigation menu. The menu structure is served to the frontend filtered by the authenticated user's permissions, so each user sees only the sections they can access.

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
  body: { type, userId, bankAccountId, categoryId, subCategoryId, paymentMethodId, amount, transactionDate, description }
  → Transaction.validate() checks all IDs present and amount > 0
  → stored and returned as TransactionResponse
```

### 3. Financial Report
```
POST /reports
  body: { from, to, userId?, bankAccountId?, categoryId?, subCategoryId?, paymentMethodId?, type? }
  → queries transactions matching all filters
  → returns: { transactions[], totalIncome, totalExpense, balance }
```

### 4. Access Control Flow
```
Incoming request
  → Spring Security extracts Auth0 JWT
  → SecurityService.userHasPermissionForURL(method, path, auth)
      → finds SpaceMember for user
      → finds matching EndpointPermission by regex
      → checks role is in permittedRoles
  → allowed or 403
```

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
| POST | `/` | Record income or expense |
| PUT | `/{id}` | Update transaction |
| DELETE | `/{id}` | Delete transaction |

### Categories `/categories`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create category |
| PUT | `/{id}` | Rename category |
| DELETE | `/{id}` | Deactivate category |
| POST | `/subcategories` | Create subcategory |
| PUT | `/subcategories/{id}` | Rename subcategory |
| DELETE | `/subcategories/{id}` | Deactivate subcategory |

### Bank Accounts `/bank-accounts`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create bank account |
| PUT | `/{id}` | Update account metadata |
| DELETE | `/{id}` | Deactivate account |

### Payment Methods `/payment-methods`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create payment method |
| PUT | `/{id}` | Rename payment method |
| DELETE | `/{id}` | Deactivate payment method |

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

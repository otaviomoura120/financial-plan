# Transaction Balance Effect

`TransactionBalanceEffectService` (`application/transaction/TransactionBalanceEffectService.java`) centralizes how a `Transaction` affects `BankAccount` balances, so the create/update/delete services in `application/transaction/` don't each duplicate "which account(s) does this type touch".

## `apply(transaction)`
Called when a transaction starts affecting balances (e.g. on create).

| Type | Effect |
|---|---|
| `INCOME` | `bankAccountId` is credited |
| `EXPENSE` | `bankAccountId` is debited |
| `TRANSFER` | `bankAccountId` (origin) is debited, `destinationBankAccountId` (destination) is credited |

## `revert(transaction)`
Mirrors the inverse of `apply` for the same transaction — used when undoing its effect (e.g. before an update replaces the old values, or on delete).

| Type | Effect |
|---|---|
| `INCOME` | `bankAccountId` is debited |
| `EXPENSE` | `bankAccountId` is credited |
| `TRANSFER` | `bankAccountId` (origin) is credited, `destinationBankAccountId` (destination) is debited |

## Notes
- Both methods read the current `BankAccount` via `BankAccountRepository.findById`, call `credit()`/`debit()` on the domain object, and persist it back via `update()`.
- No "insufficient balance" guard exists anywhere in this flow — negative balances are allowed by design (see `BankAccount.credit()`/`debit()`), so `revert()` is never blocked.
- This service does not itself call `TransactionRepository` — it only touches `BankAccountRepository`.

## Wiring into the Transaction lifecycle

- **`CreateTransactionService`** (`@Transactional`): after FK validation and `Transaction.validate()`, calls `apply(transaction)` before persisting. If any FK is missing, `apply` is never reached.
- **`UpdateTransactionService`** (`@Transactional`): takes a full in-memory snapshot of the transaction as loaded from the repository (before `Transaction.update(...)` mutates it), validates the new FKs, then calls `revert(old)` followed by `Transaction.update(...)` + `Transaction.validate()` + `apply(updated)`. This correctly handles changes to `type`, `amount`, `bankAccountId` and/or `destinationBankAccountId` — e.g. moving a transaction from one bank account to another reverts the effect on the old account and applies it to the new one. FK validation runs before `revert`, so an invalid new bank account/category/payment method/sub-category aborts the update without touching any balance.
- **`DeleteTransactionService`** (`@Transactional`): loads the transaction by id (`DomainException("Transaction not found")` if it doesn't exist), calls `revert(transaction)`, then deletes it — restoring the affected bank account(s) to the balance they had before the transaction ever existed.

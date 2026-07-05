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
- This service does not itself call `TransactionRepository` — it only touches `BankAccountRepository`. Wiring it into create/update/delete (so it actually runs during the transaction lifecycle) is done by the services that consume it.

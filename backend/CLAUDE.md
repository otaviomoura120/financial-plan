## Project Overview
- **Name:** financial-plan — family financial planning system
- **Stack:** Java + Spring Boot 4.0.6, Spring Data JPA, MySQL 8, Gradle, Groovy/Spock for tests
- **Root package:** `com.devhouse.financial_plan`
- **Database:** MySQL, schema `financial_plan`, port 3306 (configured in `application.properties`)
- **DDL:** versioned by Flyway in `src/main/resources/db/migration`; Hibernate runs with `ddl-auto=validate` (overridable with `JPA_DDL_AUTO`). How it works and how to write migrations: `docs/database-migrations.md`

## Layer structure
```
src/main/java/com/devhouse/financial_plan/
├── domain/                      # Rich models, repository interfaces, enums, exceptions
│   ├── repository/              # Repository interfaces (contracts)
│   ├── exception/               # DomainException, NoStacktraceException
│   └── enums/                   # TransactionType (INCOME, EXPENSE)
├── application/                 # Use cases (one service per operation) + DTOs (records)
│   ├── bankaccount/
│   ├── category/
│   ├── family/
│   ├── paymentmethod/
│   ├── report/
│   ├── transaction/
│   └── user/
└── infrastructure/              # REST controllers, JPA entities, repository implementations
    ├── controller/
    ├── repository/
    │   └── jpa/                 # *EntityJpa — JPA entities separate from domain models
    └── config/                  # GlobalHandlerException
```

## Implementation patterns
- **Domain models** have no JPA/Spring annotations; they are POJOs with all-args constructor, `validate()`, and business methods
- **Repositories** are interfaces in the domain (`domain/repository/`) and implemented in infrastructure (`infrastructure/repository/*RepositoryImpl.java`)
- **JPA entities** live in `infrastructure/repository/jpa/*EntityJpa.java` — the `RepositoryImpl` handles domain ↔ JPA entity mapping
- **Application services** follow the pattern: receive record DTO → build domain object → `validate()` → call repository → return response DTO
- **Optimistic locking** is done manually: `setVersion()` in the domain throws `ObjectOptimisticLockingFailureException` if versions diverge
- **DTOs** are always Java `record`

## Exception handling (GlobalHandlerException)
| Exception | HTTP Status |
|---|---|
| `DomainException` | 422 Unprocessable Entity |
| `ObjectOptimisticLockingFailureException` | 423 Locked |
| `Exception` (generic) | 500 Internal Server Error |

## Tests
- **Architecture:** `src/test/java/.../ArchitectureTest.java` (ArchUnit JUnit5) — enforces layer isolation
- **Unit tests:** must be written in Groovy/Spock under `src/test/groovy/...`
- **Command:** `./gradlew test`
- **Important:** `RepositoryImpl` classes return `null`/empty list — when writing service tests, mock the domain repository interface

## Rules for develop in this project
- Always respect the archtecture, following the SOLID principals
- Always verify with the arch test if no arch rule was broken
- Don't create anemic models, always create rich models, simple and concise
- All the app is divided in application / domain / infrasctructure
- always provide unit tests, write it using spock with groovy. Create tests that really test the flow, don't create dummy tests
- always run all tests before finish the work in progress
- any change to a JPA entity (new table, column, index or type) needs a new Flyway migration `V<next>__<description>.sql`; never edit a migration that was already applied

## JAVA following pattern
- Dont use 'var' for object declaration, use the right Object reference
- ALWAYS create the IF's like this:
if (sss == sss) {
    ...
}
don't use inline ifs like: 
if (sss == sss) ....
- don't create "spaguetti code", divide in small blocks of function, maximum of 25 lines

## !!!!!!IMPORTANT!!!!! rules
- Don't access any .env file
- Don't commit any file by your own

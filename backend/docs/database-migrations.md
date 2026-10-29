# Database migrations (Flyway)

The MySQL schema is versioned with **Flyway**. Hibernate no longer creates or changes tables: it only checks, at startup, that the JPA mappings match the schema (`ddl-auto=validate`).

## Configuration

| Property (`application.properties`) | Value | Purpose |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `${JPA_DDL_AUTO:validate}` | Hibernate validates the mappings against the schema and fails startup on a mismatch |
| `spring.flyway.baseline-on-migrate` | `true` | A database that already has tables but no Flyway history is adopted instead of rejected |
| `spring.flyway.baseline-version` | `1` | That adopted database is recorded as already being at `V1` |
| `spring.datasource.*` | `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` | Flyway uses the same datasource as the app |

Dependencies (`build.gradle`): `spring-boot-starter-flyway` and `flyway-mysql` (required by Flyway 10+ for MySQL). Their versions come from the Spring Boot BOM.

Migrations live in `src/main/resources/db/migration` (Flyway's default location).

## What happens at startup

```
App starts
  → Flyway connects with the app datasource
  → reads the flyway_schema_history table
      ├─ table does not exist, schema empty      → runs V1, V2, ... in order
      ├─ table does not exist, schema has tables → baseline: records V1 as applied (V1 is NOT run), then runs V2+
      └─ table exists                            → runs only the versions not applied yet
  → Hibernate starts and validates the mappings against the resulting schema
      └─ mismatch (missing table/column, wrong type) → startup fails with a SchemaManagementException
```

Flyway always runs **before** Hibernate, so the schema validated by Hibernate already includes the latest migrations.

`flyway_schema_history` records, for each migration: version, description, script name, checksum, who ran it, when, and whether it succeeded. This table is the version control of the database: query it to see which version a database is on.

## Existing migrations

| Version | File | Content |
|---|---|---|
| V1 | `V1__baseline_schema.sql` | Full schema (19 tables, FKs and indexes) as of 2026-10-21, generated from the JPA mappings with the MySQL 8 dialect, so it matches what `ddl-auto=update` would create on an empty database |
| V2 | `V2__add_unique_email_to_users.sql` | Unique constraint on `users.email`. Fails on a database that already has duplicated e-mails, which must be resolved by hand first |

### Databases that existed before Flyway

Local databases created by the old `ddl-auto=update` are **baselined**: on the first startup with Flyway, `V1` is recorded as applied without running. Two consequences:

- If that database diverged from the mappings (an old column type, a missing column), Hibernate validation reports it on startup. Fix it with a new migration, or by hand on a local database.
- Leftover columns from removed or renamed fields (`update` never dropped anything) do not fail validation. Drop them with a migration if you want them gone.

## Creating a new migration

Every change to a JPA entity (`infrastructure/repository/jpa/*EntityJpa`) that affects the schema needs a migration in the same change. This includes new tables, new or renamed columns, type or nullability changes, indexes, and FKs.

1. Create `src/main/resources/db/migration/V<next>__<description>.sql`:
   - `<next>` is the next integer after the highest existing version (`V2`, `V3`, ...).
   - **Two** underscores after the version; snake_case description, e.g. `V2__add_notes_to_transactions.sql`.
2. Write plain MySQL DDL/DML, e.g.:
   ```sql
   alter table transactions add column notes varchar(255);
   ```
3. Match the type Hibernate expects for the mapping, otherwise validation fails. Defaults used in V1: `varchar(255)` for `String`, `decimal(38,2)` for `BigDecimal`, `datetime(6)` for `Instant`, `bit` for `boolean`, and `enum ('A','B')` for `@Enumerated(EnumType.STRING)`. When in doubt, look at how similar columns are declared in `V1__baseline_schema.sql`.
4. Start the app against your local database: Flyway applies the migration and Hibernate validates it.
5. Data changes (backfills, data fixes) are also migrations, e.g. filling a new non-null column before adding the constraint.

### Rules

- **Never edit a migration that has already been applied** on any shared database. Flyway stores a checksum of each script and refuses to start (`Validate failed: Migration checksum mismatch`) if the file changes. Correct mistakes with a new migration.
- **Never rename or delete** applied migrations, for the same reason.
- **There is no automatic rollback.** Flyway Community does not run "undo" scripts. To revert something, write a new migration that reverses it.
- **MySQL DDL is not transactional.** If a migration with several statements fails halfway, the statements that already ran stay applied and the version is recorded as failed. Fix the database by hand (undo the partial changes), then run `flyway repair` (or delete the failed row from `flyway_schema_history`) before starting again. Prefer small migrations, one change each.
- Do not use `JPA_DDL_AUTO=update` together with Flyway on a shared database. Hibernate would change the schema without a migration, and the next environment would diverge. It is acceptable only as a temporary local experiment, followed by writing the real migration.

## Seed data (`docs/seed.sql`)

`seed.sql` is **not** a Flyway migration. It holds the configuration data the app needs to work: endpoint permissions, roles, menus and system categories. It is still kept by hand (see the note on controller endpoint changes).

- **New, empty database (e.g. first production deploy):** start the app once so Flyway creates the schema, then run `seed.sql` once. Without it no role has any `ALLOW` permission and every request returns `403`.
- **Endpoint or menu changes:** keep updating `seed.sql` as before. Databases that already have the data need the corresponding `INSERT`/`UPDATE` applied by hand, or written as a migration if it must reach every environment automatically.

## Environment variables per environment

| Variable | Local default | Production |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/financial_plan` | production database URL |
| `DB_USERNAME` | `root` | dedicated user, not `root` |
| `DB_PASSWORD` | `root` | secret from the hosting environment |
| `JPA_DDL_AUTO` | `validate` | `validate` (do not override) |

The database user needs DDL privileges (`CREATE`, `ALTER`, `DROP`, `INDEX`, `REFERENCES`) on the schema, because Flyway runs the migrations with the app's datasource. To keep the runtime user restricted to `SELECT/INSERT/UPDATE/DELETE`, configure a separate migration user with `spring.flyway.user` / `spring.flyway.password`.

## Troubleshooting

| Error on startup | Cause | Fix |
|---|---|---|
| `Schema-validation: missing column [x] in table [y]` | Entity changed without a migration, or a local database diverged from the mappings | Add a migration that creates the column |
| `Schema-validation: wrong column type encountered in column [x]` | Migration type differs from what Hibernate expects | New migration with `alter table ... modify column ...` using the expected type |
| `Validate failed: Migration checksum mismatch for migration version N` | An applied migration was edited | Restore the original file; put the change in a new migration |
| `Detected failed migration to version N` | A migration failed halfway (see the MySQL DDL rule) | Undo the partial changes, run `flyway repair`, start again |
| `Found non-empty schema(s) ... but no schema history table` | `baseline-on-migrate` was turned off | Keep `spring.flyway.baseline-on-migrate=true` |

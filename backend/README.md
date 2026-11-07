# Apteka-Bazy-Danych

## Fresh MariaDB database

Flyway initializes an empty database using `V01__init.sql` (schema from
`newdb2.sql`) and `V02__testData.sql` (seed data from `NewDb.sql`). The schema
uses InnoDB and `utf8mb4_unicode_ci` and contains 10 application tables.

These migrations replace the original V01/V02 definitions. Start with an empty
database, including no existing `flyway_schema_history`; they are not an upgrade
path for a database that already applied the old migrations. Do not repair the
old migration checksums to reuse that database. Configure the datasource for
the fresh database, then start the application to run Flyway automatically.

The Docker Compose MariaDB service persists its database in the `mariadb_data`
volume. Restarting its container does not create an empty database.

Backend APIs and services still require separate adaptation to the new schema
(for example, `Lek` is now `Produkt`).

## OpenAPI contracts

The ten resource specifications in `api/` are generated independently. Run
`mvn generate-sources` to generate Spring interfaces and DTOs. Maven uses one
execution per specification and resource tags to generate separate interfaces
in `target/generated-sources/openapi`. The first execution cleans that directory;
subsequent executions preserve previously generated files.

The version 2.0.0 contracts use `/api/v1`, database field names, `/produkty`
instead of `/leki`, and `/zamowienia/{id_zamowienia}/pozycje` for order items.
Each collection supports GET/POST; each individual resource supports GET/PUT/DELETE.
PUT updates supplied fields and preserves omitted fields. POST order requests
include items; the backend calculates price snapshots and order totals. Payments
and customer addresses have separate resources. Passwords are write-only.

These are breaking contract changes. Existing controllers, services, and frontend
clients need separate adaptation before the application can use the new API.

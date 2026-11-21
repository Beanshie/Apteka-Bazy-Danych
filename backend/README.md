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

Backend services and controllers implement the new schema (for example,
`Lek` is now `Produkt`).

## OpenAPI contracts

The ten resource specifications in `api/` are generated independently. Run
`mvn generate-sources` to generate Spring interfaces and DTOs. Maven uses one
execution per specification and resource tags to generate separate interfaces
in `target/generated-sources/openapi`. The first execution cleans that directory;
subsequent executions preserve previously generated files.

The version 2.0.0 contracts use `/api/v1`, database field names, `/produkty`
instead of `/leki`, and `/zamowienia/{id_zamowienia}/pozycje` for order items.
Each collection supports GET/POST; each individual resource supports GET/PUT/DELETE.
PUT replaces all writable fields. Required fields must be supplied; nullable
fields are cleared when omitted or null. A null or omitted password retains its hash.
Order requests and responses contain header fields only. POST creates an empty
order with total zero; add and fetch items through the nested item endpoints.
Each request is a separate transaction, so a failed item request leaves its order
intact. PUT changes header fields while preserving item IDs and price snapshots. Payments
and customer addresses have separate resources. Passwords are write-only.

These are breaking contract changes. Frontend clients need adaptation to the new API.

## Backend behavior and tests

Each controller endpoint handles invalid arguments (400), integrity conflicts
(409), and other database failures (500) with local try/catch blocks. Database
errors are logged without exposing their details in responses. Missing records and
references return 404 through controller-local handling of NoSuchElementException. Spring handles request validation and malformed JSON.
Deleting an absent target returns 204; nested operations still require an existing parent order.

Each resource has a JDBC service with only its API operations: create, get, list,
update, and delete. SQL, validation, parameter binding, and business rules are inline
in those operations. Ordinary reads reuse get(); there are no CRUD or business
helper methods. Services throw HTTP-independent exceptions. Controllers own HTTP
responses. V03 database triggers own stock coordination, ownership checks,
price snapshots, and total calculations for order and item mutations.
Updates replace the full writable object; nullable fields may be cleared.
Null or omitted passwords preserve the existing hash. Account passwords are hashed with BCrypt and never included in responses.
MariaDB sessions and API timestamp conversions use UTC.

Creating an order header does not reserve stock. Adding items reserves stock for
active orders and calculates totals using catalog price snapshots. Cancellation
releases stock while retaining items and totals; reactivation reserves it again.
Header status changes through either the service or direct SQL follow these rules.
Only transitions across ANULOWANE affect reservations. Deletion uses a single
header DELETE: the order BEFORE DELETE trigger restores active-order stock, and
foreign-key cascades remove items and payments. Cancelled orders release no stock.
Stock-restoration overflow rolls back the entire deletion.
Prescription-required items accept customer-owned NOWA or ZWERYFIKOWANA
prescriptions. Reactivation revalidates these rules; ordinary header edits and
cancellation can retain fulfilled or rejected prescriptions. Changing the customer
checks ownership of attached prescriptions. Payment and order statuses stay independent.
Triggers lock referenced rows rather than the entire product catalog. Header and
item operations can acquire row locks in different orders; deadlocks are surfaced
through existing database error handling, without automatic retries. Authentication
and payment-provider integration are not included.

Run `mvn test` for service/controller tests. MariaDB integration tests run in an
isolated Testcontainers database using Flyway; they are skipped if Docker is unavailable.

Service methods use typed filter arguments and BeanPropertyRowMapper DTO mapping,
with inline row mapping for order and payment timestamps to preserve UTC.
Only order-item methods take an `idZamowienia` parent identifier. INSERT and UPDATE
statements list their columns explicitly; creation defaults are supplied by each
service. Validation operates directly on DTOs; maps are used only for SQL parameters.

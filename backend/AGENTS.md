# Backend agent guide

## Scope and structure

This guide applies to the backend directory and its descendants. The application
uses Java 25, Spring Boot 4, Spring JDBC, MariaDB, and Flyway. Use the Maven wrapper
from this directory.

- `api/`: OpenAPI specifications for the ten resources and authentication.
- `src/main/java/pk/bd/apteka/controller/`: implementations of generated API
  interfaces and HTTP response handling.
- `src/main/java/pk/bd/apteka/service/`: parameterized SQL, transactions, and
  business validation using `NamedParameterJdbcTemplate`.
- `src/main/java/pk/bd/apteka/config/`: session security and credentialed CORS
  configuration, including `SecurityConfig`.
- `src/main/java/pk/bd/apteka/security/`: credential-free `SessionIdentity`.
- `src/main/resources/db/migration/`: schema, seed data, and database triggers.
- `src/test/java/pk/bd/apteka/`: unit, controller, service, and trigger tests.
- `target/generated-sources/openapi/`: generated API interfaces and DTOs.

Check the current implementation, migrations, and tests when changing behavior.

## API and Java conventions

- Change API contracts in `api/*.yaml`, then regenerate sources through Maven.
  Do not edit generated files under `target/`. Generation cleans the output once
  and preserves earlier resources during subsequent specification executions.
- Preserve the `/api/v1` API and existing Polish resource and field names unless
  the task requests a contract change. PUT replaces writable fields; retain each
  resource's documented null and omission behavior.
- Keep services independent of HTTP. Controllers map service exceptions to
  responses and log unexpected database failures without exposing SQL details.
- Use bound SQL parameters, explicit INSERT/UPDATE columns, and the existing
  DTO mapping conventions. Match surrounding formatting; avoid unrelated rewrites.
- Preserve BCrypt password hashing and write-only password fields. Null or
  omitted passwords on update retain the existing hash.
- Preserve UTC database sessions and UTC conversion of order/payment timestamps.

## Authentication

- `api/auth.yaml` defines `/auth/csrf`, `/auth/login`, `/auth/me`, and `/auth/logout`
  under `/api/v1`. Customer credentials use email; employee credentials use login.
  Preserve existing BCrypt hashes and reject inactive staff with the same 401 as
  unknown accounts and incorrect passwords. Credential queries use bound parameters.
  Enforce the login password limit of 72 UTF-8 bytes without trimming passwords;
  retain a BCrypt comparison against a dummy hash for unknown accounts.
- `AuthService` owns credential lookup and account reloads; keep it independent of
  HTTP. Sessions store `SessionIdentity` and an authenticated security context,
  never passwords or hashes. Do not log login bodies or rejected password values.
  Preserve no-store headers on authentication responses and controller-local
  validation exception handling that avoids logging rejected passwords.
- Login explicitly invokes the session authentication strategy (ID rotation and
  CSRF reset) and saves the context through `SecurityContextRepository`. Logout
  uses Spring logout handlers to clear CSRF, authentication, session, and cookie.
  `/auth/me` reloads account details; deleted accounts or inactive staff invalidate
  the session and return 401. Password changes do not revoke existing sessions.
  Failed login and authentication database failures preserve an existing session;
  database failures return 500 rather than invalidating it.
- Granted authorities capture the role at login. `/auth/me` returns fresh account
  details without updating those authorities. Any future role authorization must
  explicitly address role changes in existing sessions.
- Use in-memory HttpSession with cookie-only tracking. Preserve HttpOnly,
  SameSite=Lax, host-only cookies scoped to `/api/v1`, configurable idle timeout,
  and the configurable Secure flag. Enable Secure for HTTPS deployment.
- Only authentication POSTs require `X-CSRF-TOKEN` from `/auth/csrf`. Fetch a fresh
  token after login/logout. Query/form parameters cannot replace the header; a
  missing or invalid header returns 403. Only GET `/auth/me` requires authentication.
  Existing resource endpoints remain public and need no CSRF token; do not
  introduce role/ownership restrictions without a separate task.
- Preserve explicit credentialed CORS origins; never combine credentials with a
  wildcard origin. Disable default login pages, HTTP Basic, and request redirects.
- Verify with `./mvnw -Dtest=AuthSessionTest,AuthSecurityTest,ApiContextTest test`.
  The real HTTP/MariaDB session tests require Docker; security configuration and
  database-error tests also run without Docker. Keep existing seed data unchanged.

## Database and order invariants

- Add new numbered Flyway migrations for database changes. Do not rewrite
  migrations already applied to a database or repair checksums to hide changes.
  Keep migrations compatible with MariaDB and the existing InnoDB constraints.
- `V03__szczegol_zamowienia_triggers.sql` owns item price defaults, insert/update
  prescription validation, stock changes, order totals, and header address
  ownership. Item and order mutations rely on these
  checks inside their SQL statements. Do not duplicate database validation or
  stock/total updates in Java services.
- Header BEFORE INSERT/UPDATE triggers validate that the address belongs to the
  customer, using shared locks on the referenced customer and address. Missing
  customers and NULL references remain database constraint failures. Header
  updates validate ownership only when the customer or address changes, so item
  total adjustments do not repeat these lookups.
- On insert, a NULL `cena_jednostkowa` asks the BEFORE INSERT trigger to copy
  `Produkt.cena`; an explicitly supplied price is preserved. Avoid selecting from
  `Produkt` in an INSERT whose trigger also updates `Produkt`: MariaDB rejects
  that combination.
- The BEFORE INSERT trigger locks the referenced order, product, and optional
  prescription, in that order. Every supplied prescription must belong to the
  order's customer and be NOWA or ZWERYFIKOWANA, including optional prescriptions
  and cancelled-order inserts. Products requiring prescriptions must receive one.
- On update, NULL `cena_jednostkowa` preserves the old snapshot if the product is
  unchanged or selects the new product's current price. Direct SQL can supply an
  explicit non-NULL price. The BEFORE UPDATE trigger locks old/new orders and
  products in ascending ID order, followed by the prescription. It always checks
  prescription ownership; status and prescription requirements are revalidated
  when the product, prescription, or parent changes, or quantity increases.
  Unchanged items and reductions with unchanged associations can retain their
  existing prescription status.
- Item triggers maintain totals and stock using atomic arithmetic updates.
  `chk_produkt_stan` in V01 enforces nonnegative stock. Cancelled (`ANULOWANE`)
  orders retain their totals but do not reserve stock.
- Item deletion executes a DELETE restricted by item and parent order IDs, with
  stock/total changes handled by the AFTER DELETE trigger. It performs no Java
  order/product locking. Only when no item is deleted does it query whether the
  parent exists: missing parents return HTTP 404, while absent items under an
  existing parent return HTTP 204. Successful deletion needs no follow-up query.
- Orders expose header fields only. Create an empty header (total zero), then use
  `/zamowienia/{id_zamowienia}/pozycje` for item CRUD. Order get/list responses do
  not embed items. Header updates preserve item IDs, quantities, price snapshots,
  and totals; do not delete/reinsert items or refresh their prices on header edits.
  Separate header and item requests have separate transactions: a failed item
  request leaves the previously created header intact.
- Header status transitions across `ANULOWANE` adjust reservations through the
  AFTER UPDATE trigger, using atomic arithmetic on referenced products in ascending
  ID order. Cancellation restores stock; reactivation reserves it. Repeated status
  updates and transitions between active statuses do not change reservations.
  Stock-restoration INT overflow rejects and rolls back the entire header change.
- The header BEFORE UPDATE trigger validates prescription ownership on customer
  changes. Reactivation also validates prescription requirements and NOWA or
  ZWERYFIKOWANA status. Ordinary header edits and cancellation can retain fulfilled
  or rejected prescriptions. Header item checks and stock adjustments must skip
  total-only updates issued by item triggers, avoiding recursive item-table reads.
- Delete an order with a single header DELETE. Its BEFORE DELETE trigger restores
  active-order stock using atomic arithmetic in ascending product ID order, then
  foreign-key cascades remove items and payments. Cancelled orders release no
  stock. Cascades do not execute the item-delete trigger; keep that trigger for
  standalone item deletion. Stock overflow rolls back the entire deletion.
  Order creation needs no Java row locks; header UPDATE/DELETE acquire their own
  database row locks, so deletion needs no preliminary lookup or explicit lock.
  Do not lock the product catalog or duplicate trigger validation in Java.
- Keep multi-step order/item mutations transactional, including prescription
  ownership and status checks. Review existing lock order before changing
  concurrent mutations. A rejected mutation must leave items, totals, and stock
  unchanged.
- Item UPDATE/DELETE statements lock their target rows before their triggers lock
  the parent order; header status changes and order deletion lock the parent
  first. Concurrent mutations can therefore surface database deadlocks. Verify committed
  results and rollback of rejected operations; no automatic retry is currently
  provided.
- Preserve the trigger error-code contract with `OrderItemConstraintErrors`:
  `30001` means insufficient stock, `30002` excessive order total, `30003` wrong
  prescription customer, `30004` invalid prescription status, and `30005` a
  required prescription missing. These become `IllegalArgumentException`
  business errors (HTTP 400). Code `30006` means a referenced prescription does
  not exist; `30007` means a product referenced by an update does not exist. Both
  become `NoSuchElementException` (HTTP 404). Code `30008` means an address belongs
  to another customer (HTTP 400), and `30009` means an address is missing (HTTP 404).
  Preserve the separate handling of other integrity and database failures.
- Deploy changes that transfer responsibilities between services and triggers
  together, with application writes stopped during the transition.

## Build and verification

```sh
./mvnw generate-sources
./mvnw test
```

For changes affecting orders, item mutations, or triggers, run:

```sh
./mvnw -Dtest=SzczegolZamowieniaTriggersTest,SzczegolZamowieniaServiceTest,ZamowienieServiceTest,AptekaApplicationTests,ZamowienieControllerTest test
```

Integration tests use isolated MariaDB 11.4 Testcontainers databases initialized
by Flyway. Docker must be available; these tests are skipped when it is absent.
Inspect test results and report skips separately from passes. Database changes
should be checked through direct SQL tests as well as the affected service tests,
including rollback and concurrency behavior when relevant.

Before editing, inspect `git status` and preserve unrelated workspace changes.
Run checks appropriate to the change and `git diff --check`; documentation-only
changes do not require application tests. Report what changed, which checks ran,
and any failures or limitations. Do not claim tests passed when they were skipped
or not run.

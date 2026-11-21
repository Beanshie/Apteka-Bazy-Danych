# Backend agent guide

## Scope and structure

This guide applies to the backend directory and its descendants. The application
uses Java 25, Spring Boot 4, Spring JDBC, MariaDB, and Flyway. Use the Maven wrapper
from this directory.

- `api/`: OpenAPI specifications for the ten resources.
- `src/main/java/pk/bd/apteka/controller/`: implementations of generated API
  interfaces and HTTP response handling.
- `src/main/java/pk/bd/apteka/service/`: parameterized SQL, transactions, and
  business validation using `NamedParameterJdbcTemplate`.
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

## Database and order invariants

- Add new numbered Flyway migrations for database changes. Do not rewrite
  migrations already applied to a database or repair checksums to hide changes.
  Keep migrations compatible with MariaDB and the existing InnoDB constraints.
- `V03__szczegol_zamowienia_triggers.sql` owns item price defaults, insert/update
  prescription validation, stock changes, and order totals. Item creation and
  updates rely on these checks inside their SQL statements; preserve the existing
  Java validation of full order replacement. Do not duplicate stock/total updates
  in Java services.
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
- Preserve full order replacement as deletion of old items, update of the header,
  then insertion of replacement items. This uses the old status for stock release
  and the new status for reservation, generates fresh item IDs, refreshes prices,
  and revalidates prescriptions. A temporary stock return exceeding the INT limit
  rejects the replacement and must roll back completely.
- Explicitly delete items before deleting their parent order. Foreign-key
  cascades do not execute the item-delete trigger. Header-only status changes
  through raw SQL do not adjust reservations; service status changes use the
  replacement flow.
- Keep multi-step order/item mutations transactional, including prescription
  ownership and status checks. Review existing lock order before changing
  concurrent mutations. A rejected mutation must leave items, totals, and stock
  unchanged.
- Item UPDATE/DELETE statements lock their target rows before their triggers lock
  the parent order; full order replacement locks the parent first. Concurrent
  mutations/replacements can therefore surface database deadlocks. Verify committed
  results and rollback of rejected operations; no automatic retry is currently
  provided.
- Preserve the trigger error-code contract with `OrderItemConstraintErrors`:
  `30001` means insufficient stock, `30002` excessive order total, `30003` wrong
  prescription customer, `30004` invalid prescription status, and `30005` a
  required prescription missing. These become `IllegalArgumentException`
  business errors (HTTP 400). Code `30006` means a referenced prescription does
  not exist; `30007` means a product referenced by an update does not exist. Both
  become `NoSuchElementException` (HTTP 404). Preserve the separate handling of
  other integrity and database failures.
- Deploy changes that transfer responsibilities between services and triggers
  together, with application writes stopped during the transition.

## Build and verification

```sh
./mvnw generate-sources
./mvnw test
```

For changes affecting orders, item mutations, or triggers, run:

```sh
./mvnw -Dtest=SzczegolZamowieniaTriggersTest,SzczegolZamowieniaServiceTest,ZamowienieServiceTest,AptekaApplicationTests test
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

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

The ten resource specifications and `auth.yaml` in `api/` are generated independently. Run
`./mvnw generate-sources` to generate Spring interfaces and DTOs. Maven uses one
execution per specification and resource tags to generate separate interfaces
in `target/generated-sources/openapi`. The first execution cleans that directory;
subsequent executions preserve previously generated files.

With the backend running on its default port 8080, open
[Swagger UI](http://localhost:8080/api/v1/swagger-ui.html) or
[OpenAPI JSON](http://localhost:8080/api/v1/v3/api-docs).
Adjust the host and port if configured differently. Both documentation endpoints are public.
The springdoc starter supplies matching Swagger libraries, including annotations;
do not override them with an older `swagger-annotations` dependency.

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

Resource controller endpoints handle invalid arguments (400), integrity conflicts
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
through existing database error handling, without automatic retries.
Payment-provider integration is not included.

Run `./mvnw test` for service/controller tests. MariaDB integration tests run in an
isolated Testcontainers database using Flyway; they are skipped if Docker is unavailable.

Service methods use typed filter arguments and BeanPropertyRowMapper DTO mapping,
with inline row mapping for order and payment timestamps to preserve UTC.
Only order-item methods take an `idZamowienia` parent identifier. INSERT and UPDATE
statements list their columns explicitly; creation defaults are supplied by each
service. Validation operates directly on DTOs; maps are used only for SQL parameters.

## Authentication sessions

The backend provides cookie-based login for `KLIENT` (email) and `PRACOWNIK`
(employee login). Existing CRUD endpoints remain publicly accessible; this change
does not enforce roles or customer ownership. Only `GET /api/v1/auth/me` requires
authentication. Staff roles are reported as `FARMACEUTA`, `KIEROWNIK`, or `ADMIN`;
customers have a null role. Inactive employees cannot sign in. Account creation
continues through the existing CRUD endpoints.

The credentials, session, and cookie have different jobs:

| Data | Stored in | Purpose |
| --- | --- | --- |
| BCrypt password hash | Database | Verify the password during login. |
| Authenticated session | Backend memory | Retain the account reference and granted role without credentials. |
| `JSESSIONID` cookie | Browser | Send the opaque session ID with subsequent requests. |

Spring Security restores the saved authentication into `SecurityContextHolder`
for each request carrying a valid session cookie.

All authentication routes are under `/api/v1`:

1. `GET /auth/csrf` returns `{ "token": "...", "headerName": "X-CSRF-TOKEN" }`
   and establishes an anonymous session if needed. Retain its session cookie.
2. `POST /auth/login` with that token in `X-CSRF-TOKEN` and JSON such as
   `{ "typ_konta": "KLIENT", "identyfikator": "you@example.com", "haslo": "your-password" }`.
   Use `PRACOWNIK` and the employee login for staff. Passwords are checked against
   existing BCrypt hashes; login accepts at most 72 UTF-8 bytes. Successful login
   rotates the session ID and returns `id`, `typ_konta`, `identyfikator`, `imie`,
   `nazwisko`, and `rola`, without credentials. Fetch a new CSRF token after login.
3. `GET /auth/me` returns current account data. A missing or expired authenticated
   session returns 401. Deleted accounts and inactive employees invalidate their
   session on this request; account edits are reflected in its response.
4. `POST /auth/logout` with the current CSRF token invalidates the session and
   expires its cookie, returning 204. It also accepts an anonymous session with a
   valid token. Fetch a new token before another login or logout.

Malformed login requests return 400. Unknown accounts, incorrect passwords, and
inactive employees all return the same empty 401 response. A failed login preserves
any existing authenticated session. Missing or invalid CSRF tokens return 403,
including when a session has expired; fetch a new token
to start again. Send the token in `X-CSRF-TOKEN`; query or form parameters cannot
replace this header. Database failures during login or `/auth/me` return an empty
500 response without invalidating an existing authenticated session. CSRF
protection applies only to login/logout; existing CRUD requests need no CSRF header.

The browser stores an opaque `JSESSIONID` cookie: HttpOnly, SameSite=Lax,
host-only, path `/api/v1`, with no persistent expiry. Session IDs are never
accepted through URLs. Server-side sessions are in application memory, expire
after 30 idle minutes, and are lost on restart. Multiple backend instances need
sticky routing; this implementation does not provide shared session storage.
There is no remember-me support or automatic revocation on password changes.

For browser requests, use `fetch(url, { credentials: 'include', ... })` or Axios
`withCredentials: true` and attach the returned token as the `X-CSRF-TOKEN`
header for login/logout. Same-origin `/api` requests through the existing Vite
proxy also work. Frontend screens and API client code are unchanged.

Configuration:

- `SESSION_TIMEOUT` sets the idle timeout (default `30m`).
- `SESSION_COOKIE_SECURE=true` is required for HTTPS deployment. Its local default
  is `false` so cookies work over HTTP. Use HTTPS for the public application.
- `CORS_ALLOWED_ORIGINS` is a comma-separated list of explicit browser origins,
  defaulting to `http://localhost:5173`. Credentialed CORS is enabled for this list;
  do not use a wildcard. SameSite=Lax supports the existing local setup and
  same-site deployments; hosting the frontend on an unrelated site requires a
  separate cookie policy change.

The existing seed password values do not provide a documented usable plaintext
password. Create accounts with known passwords through the existing services or
API when testing login; authentication tests create their own BCrypt-backed accounts.

Run `./mvnw -Dtest=AuthSessionTest,AuthSecurityTest,ApiContextTest test` for session,
browser-cookie, CSRF, CORS, and registration checks. `AuthSessionTest` needs Docker;
`AuthSecurityTest` exercises security settings and database-error responses without it.

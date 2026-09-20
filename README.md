# ResolveHub Lite

Java 21 / Spring Boot 3.5.16 support-ticket API. Customers create tickets, support
agents claim and resolve them, and administrators inspect an aggregated report.
PostgreSQL owns durable state and concurrency; Flyway owns schema changes.

## Delivery status

HTTP Basic authenticates every non-public request. User-management APIs are out
of scope. Run `./mvnw clean verify` locally before release.

## Start locally

Prerequisites: JDK 21, a working Docker Engine with Compose, and network access
on the first Maven/container run. Use the checked-in Maven wrapper.

1. Copy `.env.example` to a private `.env` and replace every password placeholder.
   Do not commit `.env`. Existing users are not reset by changing seed variables.
2. Export the variables from your trusted local file in your shell. Compose reads
   `.env` itself; Maven does not. For a shell-compatible file you reviewed:

   ```bash
   set -a
   source .env
   set +a
   ```

3. Start PostgreSQL and wait for its health check:

   ```bash
   docker compose up -d --wait postgres
   ```

4. Start the application with development-user seeding:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

5. Check `http://localhost:8080/api/info` and `/actuator/health`. With the `dev`
   profile, open `http://localhost:8080/swagger-ui.html` for interactive API
   documentation.
   Stop the application with Ctrl-C; `docker compose stop postgres` preserves
   the database. Do not delete the volume to repair an application error.

### Configuration

| Variable/property | Purpose |
| --- | --- |
| `RESOLVEHUB_DB_NAME` | Compose database name, default `resolvehub` |
| `RESOLVEHUB_DB_USERNAME` | Database login, default `resolvehub` |
| `RESOLVEHUB_DB_PASSWORD` | Required database password |
| `RESOLVEHUB_DB_PORT` | Compose host port, default `5432` |
| `RESOLVEHUB_DB_URL` | JDBC URL, default `jdbc:postgresql://localhost:5432/resolvehub`; override when changing port/name |
| `RESOLVEHUB_CUSTOMER_PASSWORD` | Password for `customer` |
| `RESOLVEHUB_AGENT_ONE_PASSWORD` | Password for `agent-one` |
| `RESOLVEHUB_AGENT_TWO_PASSWORD` | Password for `agent-two` |
| `RESOLVEHUB_ADMIN_PASSWORD` | Password for `admin` |
| `RESOLVEHUB_OPENAPI_ENABLED` | Override Swagger/OpenAPI availability; defaults to `true` in `dev` and `false` otherwise |
| `resolvehub.seed.enabled` | Disable development seeding with `false`; only active in `dev` |
| `resolvehub.paging.default-size` | Default page size, `20` |
| `resolvehub.paging.max-size` | Maximum page size, `100` |

Seed passwords must be nonblank, not the example placeholder, and at most 72
UTF-8 bytes for BCrypt. No real passwords are checked in. Non-development
profiles do not seed users. User-management APIs are out of scope.

## API

HTTP Basic is stateless. Use HTTPS outside localhost. Only GET `/api/info` and
GET `/actuator/health` are public. `/actuator/info` requires authentication;
other Actuator endpoints are not exposed. Each request carries Basic credentials.
No application login session is created. See the curl workflow for non-browser clients.

### Swagger UI

Swagger UI is available at `http://localhost:8080/swagger-ui.html` while the
`dev` profile is active. Its generated OpenAPI JSON is available at
`/v3/api-docs`. Both routes are disabled by default in other profiles;
`RESOLVEHUB_OPENAPI_ENABLED` is an explicit override for controlled environments.

To execute secured operations from Swagger UI:

1. Open Swagger; unauthenticated visitors receive the browser's built-in
   username/password prompt, not a separate login page.
2. Enter a database username/password. The browser reuses these Basic credentials.
3. Execute operations directly. There is no Authorize button or lock icon.
   Permissions still apply.
4. Basic credentials are cached by the browser; there is no reliable application
   logout. Use a private window and close all private windows after testing,
   or clear the browser's cached authentication to switch users.

Swagger assets and OpenAPI JSON require authentication. Missing credentials
return 401 with a Basic challenge, never a login redirect. Use HTTPS outside
localhost because HTTP Basic credentials are sent on each authenticated request.

Swagger UI is a developer convenience, not a product frontend or a substitute
for `./mvnw clean verify`.

| Method | Path | Permission | Success |
| --- | --- | --- | --- |
| POST | `/api/v1/tickets` | CUSTOMER self; ADMIN with `customerId` | 201 + Location |
| GET | `/api/v1/tickets` | CUSTOMER owned; agent OPEN/assigned; ADMIN all | 200 page |
| GET | `/api/v1/tickets/{id}` | Same visibility | 200 |
| POST | `/api/v1/tickets/{id}/claim` | SUPPORT_AGENT only | 200 |
| PATCH | `/api/v1/tickets/{id}/status` | Assigned agent or ADMIN | 200 |
| GET | `/api/v1/reports/summary` | ADMIN only | 200 |

Errors: 400 input, 401 authentication, 403 access, 404 missing resource, 409
duplicate/state/concurrency conflict, sanitized 500 unexpected failure.

Create request:

```json
{"title":"Cannot access dashboard","description":"Dashboard access fails after signing into the application.","priority":"HIGH","tags":[" Access ","access","dashboard"]}
```

The response contains an application-generated UUID/reference, normalized tags,
`OPEN` status, customer ID, and timestamps. It does not expose the entity,
password hash, or internal optimistic-lock version. An illustrative excerpt:

```json
{"referenceNumber":"RH-APPLICATION-GENERATED-UUID","title":"Cannot access dashboard","status":"OPEN","priority":"HIGH","tags":["access","dashboard"],"assignedAgentId":null,"resolvedAt":null}
```

Resolve request:

```json
{"status":"RESOLVED","resolutionSummary":"The access configuration was corrected and verified."}
```

PATCH cannot claim a ticket or reopen it. Claiming is the only API operation
that moves OPEN to IN_PROGRESS. Titles, descriptions and summaries are trimmed
before validation. Tags are trimmed, lowercased and unique; at most five may
be supplied. Summary maps contain every status/priority, including zero values.

Listing accepts `status`, `priority`, `assignedAgentId`, `search`, `page`, `size`
and one `sort=field,direction`. Search matches literal case-insensitive text in
title/reference. Allowed sort fields: `createdAt`, `priority`, `status`, `title`.
Default: `createdAt,desc`; a field without direction defaults to ascending.
Priority ascending means CRITICAL, HIGH, MEDIUM, LOW; descending reverses it.
Every order uses ascending ID as a stable tie-breaker. The page envelope has
`content`, `number`, `size`, `totalElements`, `totalPages`.

## Java structure and design

Under `src/main/java/com/codewalnut/resolvehub`:

See [the complete Maven/Java layout](docs/project-structure.md) for application,
tests, generated files and agent-only documentation folders.

```text
config       Spring wiring, security and Clock
controller   HTTP adapters
domain       enums, transition policy, reference generator, tag normalization
dto          immutable request/response records
entity       JPA entities and guarded resolution state
exception    business exceptions and safe HTTP errors
mapper       entity-to-DTO conversion
repository   JPA queries, aggregate projections, visibility specifications
security     database authentication, tracing, security error adapters
service      transactions, ownership checks and workflows
```

Tests mirror these packages. Production Java has no comments or field injection;
test names use `given..._when..._then...`. Interfaces are used for genuine
boundaries (`ReferenceNumberGenerator`, `TicketTransitionPolicy`), not empty
service/implementation pairs. Lombok is available but no annotations are needed
for the current records and explicit constructors.

`LinkedHashSet` preserves normalized tag encounter order; `EnumMap` represents
complete report categories with defensive copies. Streams only normalize tags
and map bounded query results, never filter an entire database in memory.
The report uses a read-only repeatable-read transaction for a consistent
snapshot across aggregate queries. Claiming atomically updates the eligible
row and increments `@Version`. Resolution uses optimistic version checking.

## Tests and documentation

```bash
./mvnw clean verify
./mvnw -Dtest=TicketClaimConcurrencyIntegrationTest,TicketOptimisticLockRepositoryTest test
```

The full suite requires Docker and PostgreSQL Testcontainers. There is no H2
fallback, disabled test, or container-unavailable skip. Tests use an isolated
container, not your development Compose database. JDK 21 is the target. Set
JAVA_HOME explicitly if your default Java differs.

- [Approved specification](docs/spec.md)
- [Implementation plan](docs/plans/resolvehub-lite-implementation-plan.md)
- [Task status](docs/plans/resolvehub-lite-tasks.md)
- [Concurrency](docs/concurrency-notes.md)
- [Debugging](docs/debugging-notes.md)
- [Diagrams](docs/diagram/diagrams.md)
- [Curl workflows](docs/curl-commands.md)
- Development Swagger UI: `http://localhost:8080/swagger-ui.html`

## Limits

No product frontend, user-management APIs, attachments, notifications, OAuth/JWT, or
deployment automation. Development seeding is intended for one local startup,
not concurrent multi-instance provisioning. Business event logs occur inside
transactions and are not a durable audit trail; correlate them with the final
request status. Do not enable SQL bind/body logging with real ticket data.
The interactive demonstration remains pending. Generated review and verification
reports are local-only and are not included in the repository.

PostgreSQL is published only on `127.0.0.1`. Compose's initialization user is a
database superuser, suitable only for this local PoC. Before production, provision
a restricted runtime role and a separate migration/schema-owner role; configure
`SPRING_FLYWAY_USER` and `SPRING_FLYWAY_PASSWORD` for migrations and retain
`RESOLVEHUB_DB_USERNAME` / `RESOLVEHUB_DB_PASSWORD` for runtime access. Runtime
permissions should be limited to the needed schema usage and table DML, without
superuser, role/database creation or schema ownership. Existing databases and
credentials were not changed by this remediation.

Boot 3.5.16 is the final OSS 3.5 release. Production requires commercial support
or a separately approved migration to an OSS-supported Boot generation.

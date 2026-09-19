
# Spec: Operability and Delivery Evidence

Status: Approved for implementation on 2026-09-11  
Module ID: `operability-evidence`  
Depends on: all preceding modules

## Assumptions

1. A valid incoming `X-Trace-Id` is a UUID string.
2. Only Actuator health and info are exposed; health and `/api/info` are public.
3. Curl commands are selected instead of a Postman collection to fit the PoC
   time box.
4. Pull-request creation is a separate explicitly authorized action.
5. Documentation records real command output only after commands are run.
6. Swagger UI is a development-only API exploration tool, not a product
   frontend or a replacement for automated tests.

## Objective

Make requests diagnosable and prove, with reproducible evidence, that the
approved ResolveHub Lite behavior is secure, concurrent and test-driven.

## Requirements

### Trace IDs and logging

- **OE-REQ-001:** A valid incoming `X-Trace-Id` shall be retained; a missing or
  invalid value shall be replaced with a generated UUID.
- **OE-REQ-002:** The trace id shall be stored in MDC, returned as a response
  header, included in error responses and cleared in a `finally` path.
- **OE-REQ-003:** Each request log shall contain trace id, method, path, status,
  duration and authenticated username when available.
- **OE-REQ-004:** Business logs shall cover ticket creation, successful claim,
  rejected concurrent claim, resolution and unexpected failure.
- **OE-REQ-005:** Logs shall exclude passwords, hashes, Authorization headers,
  complete request bodies and other sensitive fields.

### Error and health contract

- **OE-REQ-006:** `@RestControllerAdvice` shall return timestamp, status, error,
  message, path, traceId and validation `fieldErrors` when applicable.
- **OE-REQ-007:** Expected mappings are 400 invalid input, 401 authentication,
  403 access, 404 missing resources, 409 duplicate/transition/concurrency and
  sanitized 500 unexpected errors.
- **OE-REQ-008:** Responses shall never expose SQL messages, stack traces,
  internal class names or credentials.
- **OE-REQ-009:** Actuator shall expose health and info, include PostgreSQL
  health and not expose all endpoints publicly.

### Verification and documentation

- **OE-REQ-010:** Automated tests shall cover the identity, ticket-intake,
  ticket-claiming and ticket-resolution vertical slices and shall be runnable
  through the checked-in Maven wrapper.
- **OE-REQ-011:** `docs/concurrency-notes.md` shall explain the race, database
  protection, limits of volatile/JVM synchronization and repeatable test design.
- **OE-REQ-012:** `docs/debugging-notes.md` shall document authorization,
  concurrent claim and invalid-transition investigations with trace-backed
  evidence and regression tests.
- **OE-REQ-013:** README shall document purpose, prerequisites, Compose,
  variables, commands, users/roles, APIs, examples and limitations without
  secrets.
- **OE-REQ-014:** Documentation shall include current component, ER and
  concurrent-claim sequence Mermaid diagrams.
- **OE-REQ-015:** A curl command file shall demonstrate the required role and
  ticket workflows.
- **OE-REQ-016:** A ten-minute demonstration script and self-review shall cover
  the required runtime behaviors, design decisions, corrected agent assumption
  and limitations.
- **OE-REQ-017:** A focused PR description may be prepared only after full
  verification and explicit authorization, and shall report checks honestly.
- **OE-REQ-018:** The development profile shall expose an OpenAPI 3 description
  and Swagger UI while non-development profiles keep both disabled by default.
- **OE-REQ-019:** The OpenAPI description shall explain native browser HTTP Basic
  authentication without exposing authorization controls or weakening runtime
  security.
- **OE-REQ-020:** OpenAPI shall describe the supported ResolveHub API paths,
  application metadata and validation-aware request/response schemas.

## Commands

- Trace/error/health tests:
  `./mvnw -Dtest=TraceIdIntegrationTest,ErrorContractIntegrationTest,ActuatorSecurityIntegrationTest test`
- Full automated verification: `./mvnw verify`
- Runtime startup: `docker compose up -d postgres && ./mvnw spring-boot:run`
- Runtime shutdown: `docker compose down`
- OpenAPI verification: `./mvnw -Dtest=OpenApiIntegrationTest test`

The startup command is documentation only until shell sequencing and lifecycle
handling are reviewed for the target environment.

## Project structure

- `security` — request trace and authenticated-username filters.
- `exception` — stable error response factory and global exception advice.
- `config` — Actuator exposure, safe application metadata and OpenAPI wiring.
- `dto` — error and info response contracts.
- `docs` — evidence, debugging, concurrency and diagrams.
- `scripts/resolvehub-demo.sh` or `docs/curl-commands.md` — safe demonstration
  commands without credentials.

## Code style

Trace state is always cleaned up:

```java
try {
    chain.doFilter(request, response);
} finally {
    MDC.remove(TRACE_ID_KEY);
}
```

Logs use parameterized messages and sanitized identifiers. Evidence uses exact
test names and captured results; it never claims an unexecuted check.
Production code contains no comments; tests use
`given..._when..._then...` names and optional Arrange/Act/Assert phase comments.

## Acceptance criteria

- **OE-AC-001:** Given a valid UUID trace header, when any endpoint is called,
  then the same id appears in the response and relevant logs.
- **OE-AC-002:** Given a missing or invalid trace header, when an endpoint is
  called, then a generated UUID appears in the response header and error body
  when applicable.
- **OE-AC-003:** Given a completed or failed request, when processing exits, then
  no previous request trace id remains in MDC.
- **OE-AC-004:** Given a validation or business failure, when the API responds,
  then the status and safe error fields match the contract and include trace id.
- **OE-AC-005:** Given an unexpected exception, when handled, then HTTP 500
  contains no SQL, stack, class or credential details.
- **OE-AC-006:** Given no authentication, when health or info is requested, then
  it is accessible; unapproved Actuator endpoints are not publicly exposed.
- **OE-AC-007:** Given application health with PostgreSQL available, when health
  is requested, then database health contributes successfully.
- **OE-AC-008:** Given completed implementation, when `./mvnw verify` runs, then
  every required unit, repository, security, API and concurrency test passes
  without skips.
- **OE-AC-009:** Given the automated test suite and supporting documentation,
  when reviewed, then the required identity, intake, claiming, resolution,
  debugging and concurrency coverage is present.
- **OE-AC-010:** Given README, diagrams and curl commands, when compared with the
  final implementation, then paths, roles, schema and flows match.
- **OE-AC-011:** Given repository history and self-review, when reviewed, then at
  least one incorrect agent assumption and its correction are recorded.
- **OE-AC-012:** Given the development profile, when `/v3/api-docs` and
  `/swagger-ui.html` are requested anonymously, then HTTP 401 includes the Basic
  challenge with no login redirect. Authenticated callers can load both routes.
- **OE-AC-013:** Given a non-development profile without an explicit override,
  when documentation paths are requested, then OpenAPI and Swagger UI are not
  registered.
- **OE-AC-014:** Given the generated OpenAPI document, when it is inspected,
  then it contains the supported ticket, information and reporting paths without
  security schemes, operation security requirements, CSRF endpoints or
  user-management paths. Swagger has no Authorize button or lock icons.
- **OE-AC-015:** Given Swagger UI, when an authenticated user executes API
  operations, then the same role authorization rules enforced for curl clients
  remain in effect.

## TDD seams

| Acceptance criterion | Public seam | Planned proof |
| --- | --- | --- |
| OE-AC-001–OE-AC-005 | REST API and logs | MockMvc integration tests with captured logs |
| OE-AC-006, OE-AC-007 | Actuator HTTP API | security/health integration tests |
| OE-AC-008 | Maven lifecycle | `./mvnw verify` output |
| OE-AC-009–OE-AC-011 | Tests and versioned documentation | automated verification and manual review |
| OE-AC-012–OE-AC-015 | OpenAPI JSON, Swagger UI and secured business APIs | MockMvc integration tests and local browser workflow |

## Boundaries

- **Always:** sanitize errors/logs, clear MDC, run commands before recording
  their output and keep diagrams synchronized with code.
- **Ask first:** expose another Actuator endpoint, include additional user data
  in logs, create a commit/push/PR or run a destructive Docker/database action.
- **Never:** fabricate evidence, reveal secrets, log Authorization headers,
  expose stack traces or claim an unexecuted test passed.

## Out of scope

Product frontend UI, comments, attachments, notifications, messaging,
microservices, SLA scheduling, JWT/OAuth2, cloud deployment, Kubernetes and
user-management APIs.
Swagger UI is limited to development API exploration.

## Success criteria

All OE acceptance criteria are proven with real test/runtime evidence and the
deliverables accurately describe the final application.

## Open questions

1. Approve UUID as the only valid incoming trace-id format?
2. Approve curl documentation instead of a Postman collection?
3. Which application metadata may `/api/info` expose?

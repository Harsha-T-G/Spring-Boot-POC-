
# ResolveHub Lite Implementation Plan

Status: Approved for autonomous in-scope execution on 2026-09-11  
Traces to: `CAPABILITY-MAP.md` and `docs/spec.md`

## Outcome

Deliver one Java 21 Spring Boot API with PostgreSQL-backed ticket workflows,
database authentication, role authorization, safe concurrent claiming,
traceable errors and reproducible TDD evidence.

## Dependency graph

```text
PLAN-001 agentic and application foundation
    ↓
PLAN-002 PostgreSQL schema and persistence
    ↓
PLAN-003 database authentication and authorization
    ↓
PLAN-004 ticket intake and visibility
    ↓
PLAN-005 concurrent claiming
    ↓
PLAN-006 ticket resolution
    ↓
PLAN-007 summary reporting
    ↓
PLAN-008 observability, verification and delivery evidence
```

Production changes are integrated sequentially so failures remain attributable.
Documentation can be updated alongside the active slice.

## PLAN-001: Agentic and application foundation

Requirements: PF-REQ-001–PF-REQ-003, PF-REQ-008–PF-REQ-010  
Acceptance: PF-AC-001, PF-AC-004, PF-AC-006

1. Preserve approved specs, vendored SDD/TDD skills and stack guidelines.
2. Create Maven project, wrapper, Spring Boot entry point, safe configuration,
   Docker Compose and Testcontainers support.
3. Write a failing context/database test before migration behavior.
4. Verify Java 21 release compilation using the checked-in wrapper.

Checkpoint: the project compiles, test infrastructure reaches PostgreSQL and no
secret or H2 configuration exists.

## PLAN-002: PostgreSQL schema and persistence

Requirements: PF-REQ-004–PF-REQ-007  
Acceptance: PF-AC-002, PF-AC-003, PF-AC-005

1. RED: require V1/V2 tables, constraints, indexes and canonical roles.
2. GREEN: add forward-only user/role and ticket/tag Flyway migrations.
3. Add JPA entities matching the schema and verify `ddl-auto=validate`.
4. Add unique-username, unique-reference and optimistic-lock repository tests.

Checkpoint: an empty database migrates once and all mapped entities validate.

## PLAN-003: Identity and access

Requirements: IA-REQ-001–IA-REQ-010  
Acceptance: IA-AC-001–IA-AC-007

1. RED: unit-test database user mapping and disabled/unknown behavior.
2. GREEN: implement role/user repositories and database UserDetailsService.
3. RED: test public endpoints, default 401, role 403 and BCrypt authentication.
4. GREEN: add SecurityFilterChain, handlers, method security and dev-only
   environment-backed user seeding.

Checkpoint: four development roles authenticate from PostgreSQL; unknown,
invalid and disabled credentials fail safely.

## PLAN-004: Ticket intake and visibility

Requirements: TI-REQ-001–TI-REQ-014  
Acceptance: TI-AC-001–TI-AC-011

1. RED/GREEN unit cycles for tag normalization and reference generation.
2. RED/GREEN repository cycles for constraints, filters, pagination and stable
   sorting.
3. RED/GREEN service cycles for ownership, enabled targets and transactional
   creation.
4. RED/GREEN MockMvc cycles for 201/Location, validation, visibility, 403 and
   404 behavior.

Checkpoint: customers/admins create correctly and each role sees only its
approved ticket set.

## PLAN-005: Concurrent claiming

Requirements: TC-REQ-001–TC-REQ-009  
Acceptance: TC-AC-001–TC-AC-007

1. RED: claim endpoint behavior for success, forbidden roles and conflicts.
2. GREEN: transactional service and conditional update incrementing version.
3. RED: two HTTP claims released with CountDownLatch.
4. GREEN/refactor: exactly one 200, one 409 and one durable assignee.
5. Document why database atomicity protects multiple instances.

Checkpoint: repeated concurrent test runs preserve one valid IN_PROGRESS ticket.

## PLAN-006: Ticket resolution

Requirements: TR-REQ-001–TR-REQ-010  
Acceptance: TR-AC-001–TR-AC-007

1. RED/GREEN policy tests for valid and invalid transitions.
2. RED/GREEN service tests for summary length, Clock timestamps and ownership.
3. RED/GREEN API tests for assigned agent, ADMIN, another agent, invalid
   transition and concurrent conflict.
4. Verify failed transactions leave ticket state unchanged.

Checkpoint: only approved actors resolve valid claimed tickets atomically.

## PLAN-007: Summary reporting

Requirements: SR-REQ-001–SR-REQ-010  
Acceptance: SR-AC-001–SR-AC-006

1. RED/GREEN PostgreSQL aggregation and latest-three queries.
2. Map bounded projections to defensive EnumMaps and response lists.
3. RED/GREEN ADMIN, non-ADMIN and empty-database API behavior.

Checkpoint: report values are correct without loading all tickets.

## PLAN-008: Observability, verification and evidence

Requirements: OE-REQ-001–OE-REQ-020  
Acceptance: OE-AC-001–OE-AC-015

1. RED/GREEN trace-header, MDC cleanup, request logging and safe error tests.
2. RED/GREEN public health/info and restricted Actuator behavior.
3. Run focused suites and `./mvnw clean verify`.
4. Record four authentic RED/GREEN cycles.
5. Complete README, curl commands, concurrency/debugging notes, diagrams,
   demonstration script and self-review.
6. Inspect production source for comments and sensitive data.
7. Prepare but do not create a commit, push or pull request without explicit
   authorization.
8. RED/GREEN a development-only OpenAPI description and Swagger UI with
   documented Basic authentication and unchanged runtime security enforcement.

Checkpoint: all required tests pass without skips and documentation matches the
verified implementation.

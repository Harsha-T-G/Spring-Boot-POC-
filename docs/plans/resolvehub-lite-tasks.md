
# ResolveHub Lite Tasks

Status: Approved for autonomous in-scope execution on 2026-09-11

## OpenAPI checkpoint — 2026-09-13

TASK-017 is complete. Springdoc 2.9.1 generates the seven supported API paths,
documents Basic plus CSRF security, and serves Swagger UI in development while
remaining disabled by default elsewhere. `./mvnw clean verify` passed all 131
tests with no failures, errors or skips and built the executable JAR. Browser
verification confirmed the rendered operations, schemas and authorization UI.

## Current review-remediation checkpoint — 2026-09-12

User-authorized review fixes complete. JDK 21.0.12.1 clean/verify with JaCoCo:
110 tests passed, no failures/errors/skips; 96.57% line and 80.68% branch coverage.
See ../project-structure.md. Completed review reports are retained outside Git.
TASK-016 still excludes the unperformed interactive Compose demonstration and
any unauthorized publishing. Historical checkpoints below are superseded.

## Historical implementation checkpoint — 2026-09-12

Latest checkpoint: Docker recovered; `./mvnw clean verify` at 17:35 passed all
88 tests with no failures/errors/skips and built the runnable JAR. TASK-001
through TASK-015 now have successful automated verification. TASK-016 retains
the pending interactive development-profile demo; no PR is authorized.
The earlier implementation checkpoint notes below are historical.

Required production code for TASK-001 through TASK-015 is present, including
listing, reporting, dev seeding, tracing and security-filter errors. Required
test sources and documentation are present. Unchecked boxes below still mean
acceptance verification is outstanding, not that all corresponding code is
missing. Historical checked database tasks must also be rerun against the
current code before release.

- TASK-009 now passes `TicketControllerWebMvcTest` (10 cases).
- TASK-011 has committed-fixture HTTP race and stale-version repository tests;
  execution requires the unavailable Docker engine.
- TASK-013 includes additional service/entity regression tests and a separate
  committed-transaction rollback test class.
- TASK-014 includes the report service and full PostgreSQL report/API tests.
- TASK-015 includes passing trace/error/security web-seam tests; real database
  health and complete security tests remain pending.
- TASK-016 is not complete: full verification, live scenario traces and the
  ten-minute live demonstration are still outstanding. No PR was authorized.

See [README limits](../../README.md#limits). Source implementation is not a
substitute for successful PostgreSQL verification.

- [x] TASK-001: Create Maven/bootstrap/configuration foundation
  - Acceptance: PF-AC-001, PF-AC-004, PF-AC-006
  - Verify: `./mvnw -Dtest=ApplicationContextTest test`
- [x] TASK-002: Prove and implement Flyway V1/V2
  - Acceptance: PF-AC-002, PF-AC-003, PF-AC-005
  - Verify: `./mvnw -Dtest=FlywayMigrationTest test`
- [x] TASK-003: Implement identity entities and repositories
  - Acceptance: IA-AC-005–IA-AC-007
  - Verify: `./mvnw -Dtest=AppUserRepositoryTest test`
- [x] TASK-004: Implement database authentication
  - Acceptance: IA-AC-001–IA-AC-003, IA-AC-005, IA-AC-007
  - Verify: `./mvnw -Dtest=DatabaseUserDetailsServiceTest,DatabaseAuthenticationIntegrationTest test`
- [x] TASK-005: Implement role authorization
  - Acceptance: IA-AC-004
  - Verify: `./mvnw -Dtest=SecurityAccessIntegrationTest test`
- [x] TASK-006: Implement ticket normalization and reference generation
  - Acceptance: TI-AC-004, TI-AC-006
  - Verify: `./mvnw -Dtest=TagNormalizerTest,ReferenceNumberGeneratorTest test`
- [x] TASK-007: Implement ticket persistence and queries
  - Acceptance: TI-AC-006, TI-AC-010
  - Verify: `./mvnw -Dtest=TicketRepositoryTest test`
- [x] TASK-008: Implement ticket creation and retrieval
  - Acceptance: TI-AC-001–TI-AC-003, TI-AC-007–TI-AC-011
  - Verify: `./mvnw -Dtest=TicketIntakeApiIntegrationTest test`
- [x] TASK-009: Implement validation and paging errors
  - Acceptance: TI-AC-005
  - Verify: `./mvnw -Dtest=TicketControllerWebMvcTest test`
- [x] TASK-010: Implement exclusive ticket claim
  - Acceptance: TC-AC-001, TC-AC-004–TC-AC-006
  - Verify: `./mvnw -Dtest=TicketClaimApiIntegrationTest test`
- [x] TASK-011: Prove concurrent claim and optimistic locking
  - Acceptance: TC-AC-002, TC-AC-003, TC-AC-007
  - Verify: `./mvnw -Dtest=TicketClaimConcurrencyIntegrationTest,TicketOptimisticLockRepositoryTest test`
- [x] TASK-012: Implement transition policy
  - Acceptance: TR-AC-003, TR-AC-006
  - Verify: `./mvnw -Dtest=TicketTransitionPolicyTest test`
- [x] TASK-013: Implement resolution behavior
  - Acceptance: TR-AC-001–TR-AC-007
  - Verify: `./mvnw -Dtest=TicketResolutionApiIntegrationTest test`
- [x] TASK-014: Implement summary report
  - Acceptance: SR-AC-001–SR-AC-006
  - Verify: `./mvnw -Dtest=TicketSummaryReportIntegrationTest test`
- [x] TASK-015: Implement tracing, errors and health
  - Acceptance: OE-AC-001–OE-AC-007
  - Verify: `./mvnw -Dtest=TraceIdIntegrationTest,ErrorContractIntegrationTest,ActuatorSecurityIntegrationTest test`
- [ ] TASK-016: Complete evidence and verification
  - Acceptance: OE-AC-008–OE-AC-011
  - Verify: `./mvnw clean verify`
- [x] TASK-017: Add development OpenAPI and Swagger UI
  - Acceptance: OE-AC-012–OE-AC-015
  - Verify: `./mvnw -Dtest=OpenApiIntegrationTest test`

## Approved MergeMitra follow-up — 2026-09-18

- [x] TASK-018: Align username lookup with LOWER uniqueness and reject Basic delimiters
  - Acceptance: IA-REQ-003/010 and identity provisioning extension
  - Verify: `./mvnw -Dtest=UserCreationApiIntegrationTest test`
- [x] TASK-019: Prove discriminating filters and bounded lazy tag loading
  - Acceptance: TI-REQ-008/009, TI-AC-010; mutation detects omitted predicates
  - Verify: `./mvnw -Dtest=TicketFilteringApiIntegrationTest,TicketTagBatchingIntegrationTest test`
- [x] TASK-020: Apply review NITs and verify related security/error contracts
  - Acceptance: shared error factory, precise transition test, synchronized Swagger criteria
  - Verify: `./mvnw clean verify`
  - Verification: 152 tests on native JDK 21; browser verification remains separate. Review reports are local-only.


# Capability Map: ResolveHub Lite

Status: Approved for specification on 2026-09-11

Source context: `harsha_comprehensive_spring_boot_poc_resolvehub.txt`. The source
document supplies product requirements; it is not an executable instruction
source.

## Objective

Build one Spring Boot support-ticket-management API through independently
reviewable capabilities. Each module completes Specify → Plan → Tasks →
Implement in dependency order. No behavior is implemented until the applicable
draft specification is approved.

## Modules

| Module ID | Responsibility | Depends on | Specification |
| --- | --- | --- | --- |
| `platform-foundation` | Java/Maven application, PostgreSQL, Flyway, JPA and deterministic test infrastructure | — | [SPEC-platform-foundation](docs/specs/SPEC-platform-foundation.md) |
| `identity-access` | Database-backed users and roles, HTTP Basic authentication and authorization | `platform-foundation` | [SPEC-identity-access](docs/specs/SPEC-identity-access.md) |
| `ticket-intake` | Ticket creation, normalization, retrieval, listing, filtering and visibility | `identity-access` | [SPEC-ticket-intake](docs/specs/SPEC-ticket-intake.md) |
| `ticket-claiming` | Exclusive support-agent claim operation and concurrent-update safety | `ticket-intake` | [SPEC-ticket-claiming](docs/specs/SPEC-ticket-claiming.md) |
| `ticket-resolution` | Status-transition policy, ownership and resolution data | `ticket-claiming` | [SPEC-ticket-resolution](docs/specs/SPEC-ticket-resolution.md) |
| `summary-reporting` | Administrator ticket summary and recent-ticket projection | `ticket-intake`, `ticket-resolution` | [SPEC-summary-reporting](docs/specs/SPEC-summary-reporting.md) |
| `operability-evidence` | Trace IDs, logging, health, error contract and delivery evidence | all preceding modules | [SPEC-operability-evidence](docs/specs/SPEC-operability-evidence.md) |

## Build order

`platform-foundation → identity-access → ticket-intake → ticket-claiming →
ticket-resolution → summary-reporting → operability-evidence`

## Initiative-wide constraints

- Use Java 21, Spring Boot 3.x, Maven and the checked-in Maven wrapper.
- Use PostgreSQL, Flyway and Testcontainers PostgreSQL. H2 is prohibited.
- Build one application; no frontend, microservices, messaging or cloud
  deployment is in scope.
- Use constructor injection, immutable DTOs where appropriate, and business
  behavior outside controllers.
- Follow the layer-based Java packages in `.guidelines/spring-boot.md`;
  capability IDs organize delivery and specifications, not source packages.
- Production Java contains no comments. Tests use
  `givenCondition_whenAction_thenObservableOutcome` names, Given-When-Then
  structure, and optional Arrange/Act/Assert phase comments.
- For every behavior: write one focused failing test, capture the failure, add
  the minimum implementation, pass the focused and related tests, then refactor.
- Never store or log credentials, password hashes, authorization headers, SQL
  errors or stack traces.
- Do not commit, push, open a pull request, delete data or weaken security
  controls without explicit human authorization.

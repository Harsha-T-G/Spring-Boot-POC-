
# Spec: Platform Foundation

Status: Approved for implementation on 2026-09-11  
Module ID: `platform-foundation`  
Depends on: none

## Assumptions

1. The project uses Maven and a checked-in `./mvnw` wrapper.
2. Spring Boot 3.5.16 is the final OSS 3.x baseline compatible with Java 21.
   Production requires commercial support or a separately approved Boot 4 migration.
3. PostgreSQL is available locally through Docker Compose and in tests through
   Testcontainers.
4. Database migrations define the schema; Hibernate validates but never creates
   or updates it.
5. A nullable `resolution_summary` column is included in the ticket schema
   because the resolution API requires that value even though the source domain
   field list omitted it.

## Objective

Provide a reproducible Spring Boot and PostgreSQL foundation on which all other
ResolveHub Lite capabilities can be developed and tested.

## In scope

- Java 21 Spring Boot Maven project and wrapper.
- Spring Web, Validation, Data JPA, Security, Actuator and Flyway.
- PostgreSQL driver, Testcontainers PostgreSQL, JUnit 5, Spring Security Test,
  MockMvc support and focused Lombok usage.
- Docker Compose PostgreSQL configuration.
- Flyway V1 user/role schema and V2 ticket/tag schema.
- JPA validation, environment-backed configuration and a `Clock` bean.
- Empty-database startup and repeatable migration verification.

## Out of scope

Authentication behavior, ticket workflows, reports, frontend, messaging,
deployment and optional Swagger configuration.

## Requirements

- **PF-REQ-001:** The project shall compile and run on Java 21 using `./mvnw`.
- **PF-REQ-002:** The application shall use PostgreSQL; no runtime or test
  profile may use H2 or another in-memory database.
- **PF-REQ-003:** Docker Compose shall provide a local PostgreSQL service whose
  credentials are supplied through environment configuration.
- **PF-REQ-004:** Flyway V1 shall create `app_users`, `roles` and
  `app_user_roles` with primary keys, foreign keys and case-insensitive username
  uniqueness.
- **PF-REQ-005:** Flyway V2 shall create `tickets` and `ticket_tags` with primary
  keys, foreign keys, unique ticket references, versioning, valid constraints
  and useful indexes.
- **PF-REQ-006:** Hibernate shall run with `ddl-auto=validate`.
- **PF-REQ-007:** The application shall start against an empty PostgreSQL
  database and repeated startup shall not reapply successful migrations.
- **PF-REQ-008:** Secrets and real credentials shall not appear in tracked
  configuration. Safe variable names and placeholders may appear in
  `.env.example`.
- **PF-REQ-009:** Application time shall be supplied through an injected
  `java.time.Clock`.
- **PF-REQ-010:** Test infrastructure shall use PostgreSQL Testcontainers and
  shall be deterministic and isolated.

## Commands

Planned repository commands:

- Start database: `docker compose up -d postgres`
- Run application: `./mvnw spring-boot:run`
- Focused foundation tests:
  `./mvnw -Dtest=ApplicationContextTest,FlywayMigrationTest,FlywayRestartIntegrationTest test`
- Full verification: `./mvnw verify`
- Stop database: `docker compose down`

## Project structure

- `src/main/java/com/codewalnut/resolvehub` — application root using the
  layer-based packages `config`, `controller`, `domain`, `dto`, `entity`,
  `exception`, `mapper`, `repository`, `security`, `service` and `validation`
  only as needed.
- `src/main/resources/db/migration` — immutable Flyway migrations.
- `src/main/resources/application.yml` — non-secret defaults.
- `src/test/java/com/codewalnut/resolvehub` — mirrors production packages and
  uses `support` for shared Testcontainers and fixtures.
- `compose.yaml` — local PostgreSQL service.
- `docs/specs` — approved and draft capability contracts.

## Code style

Use constructor injection and explicit immutable dependencies:

```java
@Configuration
class TimeConfiguration {
    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
```

Configuration property names are descriptive. No field injection or
`Instant.now()` inside business logic is permitted. Production Java contains no
comments. Tests use `given..._when..._then...` method names and Given-When-Then
structure.

## Acceptance criteria

- **PF-AC-001:** Given Java 21 and the repository checkout, when
  `./mvnw verify` runs, then the project compiles and its foundation tests pass.
- **PF-AC-002:** Given an empty PostgreSQL database, when the application starts,
  then V1 and V2 run successfully and Hibernate validates the resulting schema.
- **PF-AC-003:** Given a database with successful migrations, when the
  application restarts, then Flyway reports no pending replay of V1 or V2.
- **PF-AC-004:** Given the test suite, when database tests run, then they connect
  to a PostgreSQL Testcontainer and no H2 dependency or URL is used.
- **PF-AC-005:** Given duplicate usernames differing only by case or duplicate
  ticket references, when they are persisted, then PostgreSQL rejects them.
- **PF-AC-006:** Given tracked project files, when they are inspected, then no
  real password, token or credential is present.

## TDD seams

| Acceptance criterion | Public seam | Planned proof |
| --- | --- | --- |
| PF-AC-001 | Maven wrapper | `ApplicationContextTest` |
| PF-AC-002, PF-AC-003 | Flyway + PostgreSQL | `FlywayMigrationTest` |
| PF-AC-004 | Test datasource | Testcontainer connection metadata |
| PF-AC-005 | Repository/database constraint | Repository integration tests |
| PF-AC-006 | Repository inspection | Secret-pattern and configuration review |

## Boundaries

- **Always:** use the wrapper, PostgreSQL, Flyway and safe configuration.
- **Ask first:** select exact Spring Boot version, add dependencies, modify an
  applied migration or change database ports.
- **Never:** use H2, `ddl-auto=create/update`, hard-code secrets, or run
  destructive database commands without approval.

## Success criteria

All PF acceptance criteria pass and downstream modules can rely on a validated,
versioned PostgreSQL schema.

## Open questions

1. What Maven `groupId`, `artifactId` and base Java package should be used?
2. Which exact supported Spring Boot 3.x release should be pinned?
3. Which local PostgreSQL port and safe environment-variable names are preferred?

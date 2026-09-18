
# Spring Boot Guidelines

ResolveHub Lite follows the layer-based Java structure established in the Task
14 Product Catalog exercise. Capability specifications describe delivery
slices; they do not become top-level Java packages.

## Package layout

```text
src/main/java/com/codewalnut/resolvehub/
  config/         configuration properties and Spring wiring
  controller/     HTTP adapters only
  domain/         enums, policies and framework-light business concepts
  dto/            request, response and error payloads
  entity/         JPA persistence models
  exception/      application exceptions and global exception handling
  mapper/         entity/DTO mapping when non-trivial or repeated
  repository/     Spring Data JPA repositories, projections and specifications
  security/       authentication, authorization and request-security filters
  service/        business use cases, transactions and orchestration
  validation/     reusable Bean Validation constraints and validators
```

Create a package or class only when the current approved task needs it. Do not
add empty placeholders.

Tests mirror these packages under
`src/test/java/com/codewalnut/resolvehub/`, with reusable PostgreSQL containers,
fixtures and test builders under `support/`.

## Layer ownership

- Controllers validate HTTP input, delegate to one service operation, construct
  status/headers and return DTOs. They contain no business rules, repository
  calls or security ownership decisions.
- Services own business rules, transaction boundaries, authorization requiring
  domain data and orchestration.
- Domain policies own rules such as ticket transition validity.
- Repositories own persistence queries, pagination, projections and atomic
  database operations. They contain no application orchestration.
- Entities model persistence and protect valid state; they are never serialized
  directly.
- DTOs define REST contracts and are records where practical.
- Mappers isolate repeated or non-trivial entity/DTO conversion.
- Configuration classes wire dependencies and bind typed properties; they
  contain no business behavior.
- The global exception handler maps known failures to the approved safe error
  envelope and does not leak implementation details.
- Security configuration is default-deny. Method security protects sensitive
  service operations.

## Persistence and transactions

- Flyway owns the PostgreSQL schema; Hibernate uses `ddl-auto=validate`.
- Use `@Transactional` on application service write operations and
  `@Transactional(readOnly = true)` for suitable reads.
- Filtering, sorting, pagination and aggregation occur in PostgreSQL.
- Use `@Version` and the approved atomic update for concurrent ticket changes.
- Do not use H2 in any profile.

## Testing levels

- Plain JUnit 5 and Mockito tests verify service and domain-policy behavior.
- `@WebMvcTest` verifies controller validation, serialization, status, headers
  and exception mapping where a focused web slice is enough.
- `@DataJpaTest` with PostgreSQL Testcontainers verifies queries, constraints,
  pagination and optimistic locking.
- `@SpringBootTest` with MockMvc and PostgreSQL Testcontainers verifies
  authentication, authorization, transactions and complete API behavior.
- Test classes and packages mirror the production layer they exercise.
- All test methods and optional comments follow `.guidelines/java.md`.

## Prohibited shortcuts

No field injection, H2, `WebSecurityConfigurerAdapter`, plaintext passwords,
JPA entities at REST boundaries, synchronized controllers, in-memory filtering
of full database result sets, sensitive logging or production-code comments.

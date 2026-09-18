
# ResolveHub Lite Domain Context

## Source ranking

1. Current user instructions.
2. Approved `docs/spec.md` and linked capability specifications.
3. The ResolveHub Lite problem statement as historical product context.
4. This glossary.
5. Tests and implementation as evidence of current behavior.
6. Agent notes and prior summaries, which require verification.

## Terms

- **Application user** — database-backed identity with UUID, case-insensitive
  username, BCrypt password hash, enabled flag, roles and creation instant.
- **CUSTOMER** — creates tickets for self and sees only owned tickets.
- **SUPPORT_AGENT** — sees open and assigned tickets, exclusively claims open
  tickets and resolves tickets assigned to that agent.
- **ADMIN** — sees all tickets, creates for enabled customers, resolves eligible
  claimed tickets and reads the summary report.
- **Ticket** — PostgreSQL-backed support request identified by UUID and a unique
  readable reference. The JPA entity is never returned directly.
- **OPEN** — unclaimed initial ticket state.
- **IN_PROGRESS** — claimed ticket state with exactly one assigned agent.
- **RESOLVED** — terminal state with resolution summary and resolved instant.
- **Claim** — atomic OPEN-to-IN_PROGRESS transition assigning the authenticated
  support agent. Losing concurrent claims are conflicts.
- **Resolution** — IN_PROGRESS-to-RESOLVED transition by the assigned agent or
  ADMIN.
- **Specification** — approved product contract in `docs/specs/`.
- **Plan item** — dependency-ordered implementation slice in `docs/plans/`.
- **TDD evidence** — authentic failing and passing command outcomes recorded
  during implementation.

## Important distinctions

- Authentication proves identity; authorization decides whether that identity
  may perform an operation.
- Bean Validation checks request shape; services enforce ownership and state
  rules; PostgreSQL enforces durable constraints.
- Capability modules organize delivery. Java packages organize technical layers.
- JVM synchronization protects one process only; database atomicity protects
  concurrent application instances.
- A green unit test does not prove an API/database contract; each boundary uses
  the appropriate test seam.

## Commands and links

- Test: `./mvnw test`
- Verify: `./mvnw clean verify`
- Run: `./mvnw spring-boot:run`
- Contract index: `docs/spec.md`
- Plan: `docs/plans/resolvehub-lite-implementation-plan.md`
- Tasks: `docs/plans/resolvehub-lite-tasks.md`

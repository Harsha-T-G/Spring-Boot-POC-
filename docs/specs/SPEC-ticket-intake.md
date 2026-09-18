
# Spec: Ticket Intake and Visibility

Status: Approved for implementation on 2026-09-11  
Module ID: `ticket-intake`  
Depends on: `identity-access`

## Assumptions

1. `POST /api/v1/tickets` accepts an optional `customerId` only to support the
   stated ADMIN-create-for-customer behavior. A CUSTOMER omits it or supplies
   only their own id.
2. Default page size is 20 and maximum page size is 100; both are configurable.
3. Search matches reference number or title case-insensitively.
4. `resolutionSummary` exists as nullable ticket data but is not set during
   intake.
5. Reference numbers are generated behind `ReferenceNumberGenerator`; their
   final format is approved before implementation.

## Objective

Allow valid ticket creation and role-filtered retrieval without exposing
persistence entities or moving filtering and pagination into memory.

## Domain contract

A ticket has UUID id, unique reference number, title, description, priority,
status, customer id, optional assigned-agent id, immutable outward tag set,
creation/update instants, optional resolution instant and summary, and a version.

Priorities: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.  
Statuses used here: `OPEN`, with later modules adding transitions.

## Requirements

- **TI-REQ-001:** A CUSTOMER may create a ticket only for themselves; an ADMIN
  may create one for any enabled customer.
- **TI-REQ-002:** New tickets shall be `OPEN`, unassigned and unresolved.
- **TI-REQ-003:** Title length shall be 5–120 and description length 20–1000.
- **TI-REQ-004:** A ticket may contain at most five tags. Tags shall be trimmed,
  lower-cased, de-duplicated and stored transactionally with the ticket.
  Normalization precedes the 100-character per-tag validation. The supplied
  count is checked before deduplication, and requests retain defensive copies.
- **TI-REQ-005:** `ReferenceNumberGenerator` shall supply a readable application
  reference and PostgreSQL shall enforce uniqueness.
- **TI-REQ-006:** `Clock` shall supply `createdAt` and `updatedAt`.
- **TI-REQ-007:** Creation shall return HTTP 201, a Location header and a
  response DTO without internal persistence details.
- **TI-REQ-008:** Listing shall support status, priority, assigned-agent and
  reference/title search filters plus page, size and approved sort fields.
- **TI-REQ-009:** PostgreSQL shall perform filtering, sorting and pagination.
- **TI-REQ-010:** CUSTOMER sees only owned tickets; SUPPORT_AGENT sees open
  tickets and tickets assigned to that agent; ADMIN sees all tickets.
- **TI-REQ-011:** Existing-but-inaccessible ticket retrieval returns HTTP 403;
  missing tickets return HTTP 404.
- **TI-REQ-012:** Invalid page, size, sort field or request payload returns HTTP
  400 using the shared error contract.
- **TI-REQ-013:** Read operations shall use read-only transactions when
  appropriate.
- **TI-REQ-014:** Returned collections shall not expose mutable internal state.

## REST contract

- `POST /api/v1/tickets` — CUSTOMER or ADMIN.
- `GET /api/v1/tickets` — authenticated, role-filtered list.
- `GET /api/v1/tickets/{id}` — authenticated, role-filtered item.

Create fields: `title`, `description`, `priority`, `tags` and the draft optional
`customerId` for ADMIN behavior.

Approved sort field names: `createdAt`, `priority`, `status` and `title`.

## Commands

- Unit behavior:
  `./mvnw -Dtest=TagNormalizerTest,ReferenceNumberGeneratorTest test`
- Repository behavior:
  `./mvnw -Dtest=TicketRepositoryTest test`
- API behavior:
  `./mvnw -Dtest=TicketIntakeApiIntegrationTest test`
- Related verification: `./mvnw test`

## Project structure

- `controller` — `TicketController` HTTP adapter.
- `service` — transactional ticket creation and visibility-aware reads.
- `domain` — priorities, statuses, tag normalization and reference abstraction.
- `entity` — Ticket and tag persistence models.
- `repository` — Ticket repository and database query specifications.
- `dto` — immutable request, response, search and page contracts.
- `mapper` — repeated Ticket entity/DTO conversion.
- `validation` — reusable request constraints when Bean Validation annotations
  alone are insufficient.

## Code style

DTOs are immutable and validation is located at the boundary:

```java
public record CreateTicketRequest(
        String title,
        String description,
        TicketPriority priority,
        Set<String> tags,
        UUID customerId) {
}
```

Controllers delegate behavior; entities are not serialized. Stream operations
may normalize tags and map bounded results, but never perform database
filtering. Production code contains no comments; tests use
`given..._when..._then...` names and Given-When-Then structure.

## Acceptance criteria

- **TI-AC-001:** Given a CUSTOMER and valid input, when a ticket is created,
  then it is persisted atomically as OPEN for that customer and HTTP 201
  includes Location and a safe response.
- **TI-AC-002:** Given an ADMIN and an enabled customer, when a valid ticket is
  created for that customer, then ownership is assigned to the selected
  customer.
- **TI-AC-003:** Given an invalid or disabled target customer, when creation is
  requested, then no ticket or tags are persisted and HTTP 404 or 409 follows
  the approved error rule.
- **TI-AC-004:** Given mixed-case, duplicate and padded tags, when a ticket is
  created, then at most five normalized unique lower-case tags are persisted.
- **TI-AC-005:** Given invalid title, description, priority, tag count, page,
  size or sort field, when requested, then HTTP 400 identifies the invalid
  field where applicable.
- **TI-AC-006:** Given two tickets, when their references are generated and
  persisted, then each has a readable unique reference.
- **TI-AC-007:** Given a CUSTOMER, when listing or fetching tickets, then only
  that customer's tickets are visible and another existing ticket returns 403.
- **TI-AC-008:** Given a SUPPORT_AGENT, when listing tickets, then open tickets
  and that agent's assigned tickets are visible, but another agent's non-open
  tickets are not.
- **TI-AC-009:** Given an ADMIN, when listing or fetching tickets, then all
  tickets are visible.
- **TI-AC-010:** Given filters, sorting and pagination, when tickets are listed,
  then the database applies them and returns deterministic bounded results.
- **TI-AC-011:** Given a nonexistent ticket id, when it is fetched, then HTTP
  404 is returned.

## TDD seams

| Acceptance criterion | Public seam | Planned proof |
| --- | --- | --- |
| TI-AC-001–TI-AC-003 | REST API + PostgreSQL | MockMvc integration tests |
| TI-AC-004 | Tag normalization policy | unit test followed by creation integration test |
| TI-AC-005 | REST API | validation and query-parameter tests |
| TI-AC-006 | ReferenceNumberGenerator + repository | unit/boundary and unique-constraint tests |
| TI-AC-007–TI-AC-011 | REST/repository APIs | role-based API and PostgreSQL query tests |

## Boundaries

- **Always:** validate DTOs, use transactions, query in PostgreSQL, map entities
  to DTOs and use injected `Clock`.
- **Ask first:** change reference format, page limits, sort semantics, ownership
  rules or request fields.
- **Never:** expose JPA entities, filter all tickets with Streams, return mutable
  internal collections or allow customers to create for another customer.

## Success criteria

All TI acceptance criteria pass with PostgreSQL Testcontainers and no ticket
intake behavior depends on claiming, resolution or reporting implementation.

## Open questions

1. Approve optional `customerId` on the create request for ADMIN behavior?
2. What reference-number format is preferred?
3. For an invalid/disabled ADMIN target, should the response be 404 or 409?
4. Should priority sorting be lexical or business-ranked
   (`CRITICAL → HIGH → MEDIUM → LOW`)?
5. Approve default page size 20 and maximum 100?

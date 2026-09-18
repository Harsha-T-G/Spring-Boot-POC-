
# Spec: Summary Reporting

Status: Approved for implementation on 2026-09-11  
Module ID: `summary-reporting`  
Depends on: `ticket-intake`, `ticket-resolution`

## Assumptions

1. Count maps include every defined enum key with zero for absent categories.
2. Recent tickets are ordered by `createdAt DESC` with a deterministic id
   tie-breaker and limited to three in PostgreSQL.
3. Aggregation queries execute in PostgreSQL; Java Streams map only bounded
   query projections into immutable responses.

## Objective

Provide an ADMIN-only operational summary without loading all tickets into
application memory.

## Requirements

- **SR-REQ-001:** `GET /api/v1/reports/summary` shall be accessible only to
  ADMIN.
- **SR-REQ-002:** The response shall include total ticket count.
- **SR-REQ-003:** The response shall include counts for every
  `TicketStatus` in `EnumMap<TicketStatus, Long>` form.
- **SR-REQ-004:** The response shall include counts for every
  `TicketPriority` in `EnumMap<TicketPriority, Long>` form.
- **SR-REQ-005:** The response shall include unassigned ticket count.
- **SR-REQ-006:** The response shall include exactly the three most recent
  tickets when at least three exist, otherwise all available tickets.
- **SR-REQ-007:** PostgreSQL shall perform counts, grouping, ordering and limits.
- **SR-REQ-008:** Returned collections and maps shall not expose mutable internal
  state.
- **SR-REQ-009:** The report read shall use an appropriate read-only
  transaction.
- **SR-REQ-010:** Empty data shall produce total zero, zero-filled maps,
  unassigned zero and an empty recent list.

## Commands

- Repository aggregation:
  `./mvnw -Dtest=TicketSummaryReportIntegrationTest,TicketSummaryServiceTest test`
- Report API and security:
  `./mvnw -Dtest=TicketSummaryReportIntegrationTest test`
- Related verification: `./mvnw test`

## Project structure

- `controller` — ADMIN summary endpoint.
- `service` — summary orchestration and bounded mapping.
- `repository` — aggregate and recent-ticket query projections.
- `dto` — immutable summary and recent-ticket response records.

## Code style

The response makes collection choices explicit:

```java
public record TicketSummaryResponse(
        long total,
        EnumMap<TicketStatus, Long> byStatus,
        EnumMap<TicketPriority, Long> byPriority,
        long unassigned,
        List<TicketResponse> recentTickets) {
}
```

Construction must defensively copy or expose immutable views. No
`parallelStream()` or side effects inside stream operations are allowed.
Production code contains no comments; tests use
`given..._when..._then...` names and Given-When-Then structure.

## Acceptance criteria

- **SR-AC-001:** Given tickets across statuses and priorities, when ADMIN
  requests the report, then HTTP 200 returns accurate total, grouped and
  unassigned counts.
- **SR-AC-002:** Given more than three tickets, when the report is requested,
  then only the three most recently created tickets are returned in stable
  newest-first order.
- **SR-AC-003:** Given no tickets, when ADMIN requests the report, then zero
  totals, zero-filled enum maps and an empty recent list are returned.
- **SR-AC-004:** Given CUSTOMER or SUPPORT_AGENT credentials, when the report is
  requested, then HTTP 403 is returned.
- **SR-AC-005:** Given no credentials, when the report is requested, then HTTP
  401 is returned.
- **SR-AC-006:** Given a report request, when query behavior is inspected, then
  filtering, aggregation and top-three limiting occur in PostgreSQL rather than
  over an in-memory full-ticket collection.

## TDD seams

| Acceptance criterion | Public seam | Planned proof |
| --- | --- | --- |
| SR-AC-001–SR-AC-003, SR-AC-006 | Repository and REST APIs | Testcontainers repository + MockMvc tests |
| SR-AC-004, SR-AC-005 | REST security boundary | MockMvc security integration tests |

## Boundaries

- **Always:** aggregate in PostgreSQL, bound recent results and return safe
  response collections.
- **Ask first:** change report fields, date ranges or grouping semantics.
- **Never:** load every ticket and aggregate with Streams or expose the endpoint
  to non-ADMIN users.

## Success criteria

All SR acceptance criteria pass for populated and empty databases with
PostgreSQL Testcontainers.

## Open questions

1. Approve zero-filled enum maps instead of omitting absent keys?
2. Approve id as the deterministic tie-breaker for equal creation instants?

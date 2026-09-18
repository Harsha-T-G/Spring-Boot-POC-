
# Spec: Ticket Resolution

Status: Approved for implementation on 2026-09-11  
Module ID: `ticket-resolution`  
Depends on: `ticket-claiming`

## Assumptions

1. The claim endpoint is the only supported path for `OPEN → IN_PROGRESS`.
2. `PATCH /api/v1/tickets/{id}/status` supports `IN_PROGRESS → RESOLVED` for
   this PoC; other requested target states are conflicts.
3. `resolutionSummary` is persisted as nullable ticket data and becomes required
   only for resolution.
4. ADMIN may resolve a claimed IN_PROGRESS ticket even when not the assigned
   agent; SUPPORT_AGENT must be the assigned agent.

## Objective

Enforce ticket state transitions, ownership and resolution information as a
transactional domain rule outside the controller.

## Requirements

- **TR-REQ-001:** `TicketTransitionPolicy` shall determine whether a requested
  transition is allowed.
- **TR-REQ-002:** The valid workflow is `OPEN → IN_PROGRESS → RESOLVED`.
- **TR-REQ-003:** A ticket may enter IN_PROGRESS only through a successful claim.
- **TR-REQ-004:** Only the assigned SUPPORT_AGENT or an ADMIN may resolve an
  IN_PROGRESS ticket.
- **TR-REQ-005:** Resolution requires a summary of 20–500 characters.
- **TR-REQ-006:** Successful resolution shall persist `RESOLVED` status,
  resolution summary, `resolvedAt` and `updatedAt` from injected `Clock` in one
  transaction.
- **TR-REQ-007:** Invalid transitions shall return HTTP 409 through
  `InvalidTicketTransitionException`.
- **TR-REQ-008:** Unauthorized ownership shall return HTTP 403 through the
  approved access-denial path.
  For assigned tickets, reject another agent before disclosing transition
  details, including when the ticket is already RESOLVED.
- **TR-REQ-009:** Failed validation, authorization or concurrency shall leave the
  stored ticket unchanged.
- **TR-REQ-010:** Concurrent update conflicts shall return HTTP 409 without
  exposing implementation details.

## REST contract

`PATCH /api/v1/tickets/{id}/status`

```json
{
  "status": "RESOLVED",
  "resolutionSummary": "The access configuration was corrected and verified."
}
```

The response is a safe Ticket DTO and never the JPA entity.

## Commands

- Policy and summary validation:
  `./mvnw -Dtest=TicketTransitionPolicyTest,TicketRequestValidationTest,TicketEntityTest test`
- API behavior:
  `./mvnw -Dtest=TicketResolutionApiIntegrationTest,TicketResolutionRollbackIntegrationTest,TicketResolutionConcurrencyIntegrationTest test`
- Related verification: `./mvnw test`

## Project structure

- `domain` — transition policy and status rules.
- `service` — transactional resolution use case and ownership checks.
- `controller` — status endpoint.
- `dto` — status request and ticket response contracts.
- `entity` and `repository` — versioned Ticket persistence.
- `exception` — invalid transition, access and concurrency failures.

## Code style

The transition rule is an explicit substitutable policy:

```java
interface TicketTransitionPolicy {
    void verifyAllowed(TicketStatus current, TicketStatus requested);
}
```

The policy is tested through observable outcomes. Private methods and internal
call counts are not test seams. Production code contains no comments; tests use
`given..._when..._then...` names and Given-When-Then structure.

## Acceptance criteria

- **TR-AC-001:** Given a claimed IN_PROGRESS ticket and its assigned agent, when
  a valid resolution is requested, then HTTP 200 returns RESOLVED with the
  supplied summary and Clock-derived `resolvedAt`.
- **TR-AC-002:** Given the same ticket and an ADMIN, when valid resolution is
  requested, then resolution succeeds.
- **TR-AC-003:** Given an unassigned or OPEN ticket, when RESOLVED is requested,
  then HTTP 409 is returned and stored state is unchanged.
- **TR-AC-004:** Given a ticket assigned to another agent, when a SUPPORT_AGENT
  attempts resolution, then HTTP 403 is returned and stored state is unchanged.
- **TR-AC-005:** Given a summary shorter than 20, longer than 500 or absent, when
  resolution is requested, then HTTP 400 is returned and state is unchanged.
- **TR-AC-006:** Given a RESOLVED ticket, when another transition is requested,
  then HTTP 409 is returned.
- **TR-AC-007:** Given a concurrent version conflict during resolution, when the
  transaction completes, then HTTP 409 is returned and no partial resolution
  data is stored.

## TDD seams

| Acceptance criterion | Public seam | Planned proof |
| --- | --- | --- |
| TR-AC-001, TR-AC-002 | REST API + Clock | fixed-Clock MockMvc integration tests |
| TR-AC-003, TR-AC-006 | TicketTransitionPolicy and REST API | unit tests followed by API tests |
| TR-AC-004 | REST API/security boundary | role/ownership integration test |
| TR-AC-005 | request validation/domain rule | unit and API validation tests |
| TR-AC-007 | repository/API boundary | version-conflict integration test |

## Boundaries

- **Always:** validate transitions before mutation, use Clock and keep the
  operation transactional.
- **Ask first:** add reopening, cancellation or any new status.
- **Never:** mutate status in the controller, call `Instant.now()` in business
  logic or partially store failed resolution data.

## Success criteria

All TR acceptance criteria pass and the transition policy can be explained
without reference to controller or persistence implementation details.

## Open questions

1. Approve PATCH as resolution-only for the PoC?
2. Approve ADMIN override for any claimed IN_PROGRESS ticket?
3. Approve adding `resolutionSummary` to the Ticket persistence model?

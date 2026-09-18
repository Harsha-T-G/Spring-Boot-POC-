
# Spec: Ticket Claiming

Status: Approved for implementation on 2026-09-11  
Module ID: `ticket-claiming`  
Depends on: `ticket-intake`

## Assumptions

1. Claiming performs `OPEN → IN_PROGRESS` and assigns the authenticated support
   agent in one transaction.
2. Claiming uses one atomic PostgreSQL conditional update that increments the
   version when the ticket is still OPEN and unassigned.
3. `@Version` remains on the Ticket entity for all concurrent ticket updates.
4. A zero-row conditional update is followed by a safe existence/state check to
   distinguish a missing ticket from a claim conflict.

## Objective

Guarantee that only one SUPPORT_AGENT can claim an open ticket, including when
multiple application requests race.

## Requirements

- **TC-REQ-001:** Only SUPPORT_AGENT may call
  `POST /api/v1/tickets/{id}/claim`.
- **TC-REQ-002:** A successful claim shall assign the authenticated agent,
  change status to `IN_PROGRESS`, update the modification time and increment the
  version atomically.
- **TC-REQ-003:** A ticket may have at most one assigned agent.
- **TC-REQ-004:** Two concurrent claims shall produce exactly one HTTP 200 and
  one HTTP 409.
- **TC-REQ-005:** A missing ticket shall return HTTP 404; a non-open or already
  assigned ticket shall return HTTP 409.
- **TC-REQ-006:** Claiming shall be transactional and shall not leave partial
  assignment or status changes.
- **TC-REQ-007:** Concurrency safety shall be implemented with PostgreSQL
  atomicity and versioning, never a synchronized controller or JVM-only lock.
- **TC-REQ-008:** Rejected concurrency shall map to
  `TicketAlreadyClaimedException` or `ConcurrentTicketUpdateException` and the
  shared safe error response.
- **TC-REQ-009:** The concurrent integration test shall use two configured
  SUPPORT_AGENT users and deterministic coordination without `Thread.sleep`.

## Commands

- Focused claim behavior:
  `./mvnw -Dtest=TicketClaimApiIntegrationTest test`
- Concurrent proof:
  `./mvnw -Dtest=TicketClaimConcurrencyIntegrationTest test`
- Repository version proof:
  `./mvnw -Dtest=TicketOptimisticLockRepositoryTest test`
- Related verification: `./mvnw test`

## Project structure

- `controller` — claim HTTP endpoint.
- `service` — transactional claim orchestration.
- `domain` — claim result or policy concepts when behavior justifies them.
- `repository` — conditional claim update and version mapping.
- `exception` — claim and concurrent-update conflicts.
- `src/test/java/.../controller`, `.../service` and `.../repository` — matching
  API, unit, repository and concurrent integration tests.

## Code style

The persistence contract reports the state change rather than leaking SQL:

```java
interface TicketClaimStore {
    boolean claimOpenTicket(UUID ticketId, UUID agentId, Instant updatedAt);
}
```

The service converts a false result into a domain conflict after distinguishing
the missing-ticket case. Production code contains no comments. Tests use
`given..._when..._then...` names, Given-When-Then order and explicit phase
comments for the multi-stage concurrency setup.

## Acceptance criteria

- **TC-AC-001:** Given an OPEN unassigned ticket and authenticated
  SUPPORT_AGENT, when claimed, then HTTP 200 returns the ticket assigned to that
  agent in IN_PROGRESS state.
- **TC-AC-002:** Given the same open ticket and two authenticated agents released
  concurrently, when both claim it, then exactly one receives 200 and one
  receives 409.
- **TC-AC-003:** Given the concurrent claim completes, when the database is
  inspected, then exactly one agent is assigned, status is IN_PROGRESS and no
  partial state exists.
- **TC-AC-004:** Given an already claimed or non-open ticket, when another claim
  is requested, then HTTP 409 is returned without changing its owner.
- **TC-AC-005:** Given a CUSTOMER or ADMIN, when the claim endpoint is requested,
  then HTTP 403 is returned.
- **TC-AC-006:** Given a nonexistent ticket, when an agent claims it, then HTTP
  404 is returned.
- **TC-AC-007:** Given two stale JPA representations of one ticket, when both
  attempt incompatible updates, then PostgreSQL/JPA detects the optimistic-lock
  conflict.

## TDD seams

| Acceptance criterion | Public seam | Planned proof |
| --- | --- | --- |
| TC-AC-001, TC-AC-004–TC-AC-006 | REST API | MockMvc + PostgreSQL integration tests |
| TC-AC-002, TC-AC-003 | REST API under concurrency | ExecutorService and CountDownLatch integration test |
| TC-AC-007 | Repository API | optimistic-lock Testcontainers test |

## Boundaries

- **Always:** coordinate concurrency tests deterministically, verify HTTP results
  and final database state, and increment version during atomic updates.
- **Ask first:** replace the atomic update with a different concurrency strategy
  or change conflict semantics.
- **Never:** use `synchronized`, `volatile` or `Thread.sleep` as the correctness
  mechanism.

## Success criteria

All TC acceptance criteria pass repeatedly, and concurrency safety remains valid
when more than one application instance accesses the database.

## Open questions

1. Approve the atomic conditional update plus `@Version` strategy?
2. Should repeated claim by the already assigned same agent be idempotent 200 or
   conflict 409? The draft follows the source context and returns 409.

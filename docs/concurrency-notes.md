# Concurrency notes

## Race and protection

Two requests can both observe OPEN before either has written. A naive
read/check/save sequence without a lock permits the last write to replace the
first agent. ResolveHub instead calls `TicketRepository.claimOpenTicket` inside
`TicketClaimService`'s transaction. Its single conditional update checks both
OPEN and a null assignee, then writes agent, IN_PROGRESS, updatedAt and
`version + 1` together.

PostgreSQL locks the row while changing it. At the default read-committed
isolation, a competing updater waits and rechecks its predicate against the
committed row. One update returns 1; the loser returns 0. If the ticket exists,
the loser throws `TicketAlreadyClaimedException` mapped to 409; otherwise 404.
The winner's transaction commits before the controller returns success.

`@Version` additionally protects resolution. Hibernate writes using the old
version in the predicate and increments it. Saving a stale snapshot raises an
optimistic-lock exception, converted to a sanitized 409. The custom
`ConcurrentTicketUpdateException` wraps conflicts detected during resolution
flush; advice also covers translated conflicts raised at commit.

The rejected atomic claim is NOT an optimistic-lock exception: zero affected
rows are its conflict signal. The separate repository test proves the genuine
optimistic-lock path. `clearAutomatically` prevents stale managed state after
the bulk JPQL update. Database state checks prohibit OPEN with an assignee and
RESOLVED without an assignee, timestamp and valid summary.

## Why not volatile or synchronized?

`volatile` provides visibility for a JVM field, not an atomic database
check-and-write. `synchronized` protects only threads using the same monitor
in one JVM. Neither protects another app instance or independent database
client. The shared PostgreSQL row is the correct arbitration point.

## Repeatable tests

```bash
./mvnw -Dtest=TicketClaimConcurrencyIntegrationTest,TicketOptimisticLockRepositoryTest test
```

The HTTP concurrency test creates committed fixtures and launches two workers
with distinct database-backed Basic credentials. A ready latch ensures both
workers exist; a start latch releases both requests. Futures have timeouts;
there are no sleeps. It asserts the unordered status pair is exactly 200/409,
then reads the ticket through the ADMIN API and checks the winning agent,
IN_PROGRESS, fixed updatedAt and no resolution. The scheduler chooses the
winner; the correctness property does not depend on simultaneous CPU timing.

The repository test loads two detached copies of an already claimed ticket in
separate transactions. It commits the first resolution, rejects the stale
second snapshot, and reads back the first summary and a version exactly one
greater than the claimed snapshot. Fixture cleanup
targets only generated users' tickets and those users in the isolated test DB.

## Verification status

Docker recovered on 2026-09-12. The HTTP race test executed successfully against
PostgreSQL: exactly one 200 and one 409, followed by a successful ADMIN read
asserting the winning assignee and IN_PROGRESS state. The optimistic-lock test
also passed after replacing its incorrect hard-coded version assumptions with
increments from the actual persisted baseline. No production locking code was
changed. See [debugging](debugging-notes.md) for the investigation notes.

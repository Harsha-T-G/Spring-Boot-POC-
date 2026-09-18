# Debugging notes

## Evidence boundary

Database scenarios below ran against PostgreSQL Testcontainers after Docker
recovery on 2026-09-12. Captured traces are recorded below. No credentials, SQL,
request bodies or password hashes belong in debugging evidence.

## 1. Customer receives 403 for another customer's ticket

- Symptom: authenticated request is forbidden, not unauthenticated.
- Reproduce: `SecurityAccessIntegrationTest.givenAnotherCustomersTicket_whenCustomerReads_thenReturnTraceableForbidden`.
- Caller: the fixture's `customer`, role CUSTOMER; owner: distinct `otherCustomer`.
- Root cause: `TicketService.verifyVisible` requires owner ID equality for
  CUSTOMER. Authentication succeeds, then the ownership check rejects access.
- Test trace input: `93c84e42-010c-48bd-915c-b6f10d8ae2cb`.
- Expected response: JSON 403 with the same trace; request log identifies the
  authenticated fixture username and 403, without exposing the ticket body.
- Captured output: 17:33:37.708, trace `93c84e42-010c-48bd-915c-b6f10d8ae2cb`,
  GET ticket `38bc1ae7-f7fb-42d2-99de-489a010f5ca4`, status 403,
  username `scenario-d186c7a0-a3f9-4595-bc62-2b4af800e684`. Test passed.

## 2. Two agents claim one ticket

- Symptom: one request wins; the other is a normal business conflict.
- Reproduce: `TicketClaimConcurrencyIntegrationTest`.
- Root cause: the losing conditional UPDATE matches zero rows after the winner
  commits. This path raises `TicketAlreadyClaimedException`, not a Hibernate
  optimistic-lock exception. See `TicketOptimisticLockRepositoryTest` for the
  separate stale-version path during resolution.
- Expected response: exactly one 200 and one 409; final API read has exactly
  the winner's ID and IN_PROGRESS. The loser must not replace the assignee.
- Captured race: ticket `6329a3d2-81f0-4382-8e19-a338c8d9af29`.
  Winner trace `6c2ffee2-c0b2-4dc0-8353-9d2c932c9875`: `ticket_claimed`, then
  POST status 200 at 17:33:41.052. Loser trace
  `a22d41c5-c78b-41fc-941c-680d5e426641`: `ticket_claim_rejected`, then POST
  status 409 at 17:33:41.066. ADMIN read trace
  `1994cbf3-240a-4cb2-b42c-4caefee91778` returned 200. The test passed its
  exact winning-assignee, status and timestamp assertions.

## 3. OPEN cannot resolve directly

- Symptom: a valid-length resolution request receives 409.
- Reproduce: `TicketResolutionRollbackIntegrationTest.givenOpenTicket_whenInvalidResolutionRequested_thenKeepAllStateUnchanged`.
- Root cause: `DefaultTicketTransitionPolicy` prohibits OPEN to RESOLVED;
  claiming must establish an assignee first. The rejection precedes mutation.
- Test trace input: `24ff716d-e726-4bfd-87d9-05c2f45f2e41`.
- Expected response/state: 409; a subsequent ADMIN read still shows OPEN,
  original updatedAt, null assignee/summary/resolvedAt.
- Captured output: 17:33:44.055, trace `24ff716d-e726-4bfd-87d9-05c2f45f2e41`,
  PATCH ticket `e092e5af-bbd0-49e8-a82e-5049b2556b7a` status 409. The subsequent
  API-read assertions passed, proving unchanged state. The same class also
  passed unauthorized-agent rollback, ADMIN resolution and repeated-resolution
  rejection. Source log: `/private/tmp/resolvehub-docker-recovery-verify.log`.

## Actual trace-backed local error checks

On 2026-09-12, `ErrorContractIntegrationTest` produced these condensed log lines:

```text
unexpected_failure traceId=bd9a3bfb-a9f8-4d84-8ae7-e42d1e038bc2 exceptionType=IllegalStateException
request traceId=bd9a3bfb-a9f8-4d84-8ae7-e42d1e038bc2 method=GET path=/failure status=500 durationMs=3 username=anonymous
request traceId=d2fa5cd1-beb3-4477-aae3-106647ddc731 method=GET path=/conflict status=409 durationMs=68 username=anonymous
```

These are real local web-seam tests, not PostgreSQL claim/authorization traces.
The test exception deliberately contains a fake SQL/password marker; the API
returns only `An unexpected error occurred`, and application error logging
records the exception type, not its sensitive message. `TraceIdIntegrationTest`
also proves MDC is cleared even when the filter chain throws.

## Corrected assumptions

The existing transition policy permits OPEN to IN_PROGRESS for claiming.
Assuming that it alone validated the resolution endpoint was wrong: an ADMIN
PATCH requesting IN_PROGRESS reached `ticket.resolve`. The reproduction test
initially failed with `Expecting code to raise a throwable`; an explicit
RESOLVED-only endpoint guard now rejects it. The entity separately guards its
state and summary before mutation. Another reproduced defect allowed padding
to satisfy DTO lengths before the service trimmed the input; normalization now
precedes Bean Validation.

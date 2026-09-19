# Curl demonstration commands

Run against the local development profile after following README. `curl --user
username` prompts for the password so it is not pasted into shell history or
arguments. Do not use verbose/trace curl flags with credentials. Commands below
change only the local demo database and are examples, not commands already run.

## Public metadata, health, authentication

```bash
curl -i http://localhost:8080/api/info
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/v1/tickets
curl -i --user customer http://localhost:8080/api/v1/tickets
```

Expected: 200, 200, 401, 200 respectively.

## Customer creates and reads

HTTP Basic is sufficient for writes. CSRF tokens are not used.

```bash
curl -i --user customer -X POST http://localhost:8080/api/v1/tickets \
  -H 'Content-Type: application/json' \
  -H 'X-Trace-Id: d56b327c-e583-4344-a9a6-9c36c321157e' \
  --data '{"title":"Cannot access dashboard","description":"Dashboard access fails after signing into the application.","priority":"HIGH","tags":[" Access ","access","dashboard"]}'
```

Expected: 201, Location, trace header, OPEN and unique normalized tags. Copy the
returned `id` into `ticket_id` (an identifier only, not a secret):

```bash
ticket_id='paste-returned-uuid'
curl -i --user customer "http://localhost:8080/api/v1/tickets/$ticket_id"
curl -i --user customer 'http://localhost:8080/api/v1/tickets?status=OPEN&priority=HIGH&search=dashboard&page=0&size=10&sort=createdAt,desc'
curl -i --user customer 'http://localhost:8080/api/v1/tickets?size=101'
```

Expected: 200, filtered page, 400.

## Invalid transition, claim, access denial and resolve

```bash
curl -i --user admin -X PATCH "http://localhost:8080/api/v1/tickets/$ticket_id/status" \
  -H 'Content-Type: application/json' \
  --data '{"status":"RESOLVED","resolutionSummary":"The access configuration was corrected and verified."}'
curl -i --user agent-one -X POST "http://localhost:8080/api/v1/tickets/$ticket_id/claim"
curl -i --user agent-two "http://localhost:8080/api/v1/tickets/$ticket_id"
curl -i --user agent-two -X POST "http://localhost:8080/api/v1/tickets/$ticket_id/claim"
curl -i --user agent-one -X PATCH "http://localhost:8080/api/v1/tickets/$ticket_id/status" \
  -H 'Content-Type: application/json' \
  --data '{"status":"RESOLVED","resolutionSummary":"The access configuration was corrected and verified."}'
```

Expected: 409 while OPEN; 200 IN_PROGRESS; 403; 409; 200 RESOLVED with resolvedAt.
Read again as `customer` to confirm ownership access remains available.

## Administrator creates for a customer and reads the report

Use the `customerId` returned by the first create response:

```bash
customer_id='paste-customer-uuid'
curl -i --user admin -X POST http://localhost:8080/api/v1/tickets \
  -H 'Content-Type: application/json' \
  --data "{\"title\":\"Admin-created ticket\",\"description\":\"An administrator recorded this customer support request.\",\"priority\":\"LOW\",\"customerId\":\"$customer_id\"}"
curl -i --user admin http://localhost:8080/api/v1/reports/summary
curl -i --user customer http://localhost:8080/api/v1/reports/summary
```

Expected: 201 for the supplied customer, 200 report, 403.

## Concurrent claims

The controlled demonstration is automated so no password is needed in parallel
shell arguments and both requests share a start latch:

```bash
./mvnw -Dtest=TicketClaimConcurrencyIntegrationTest test
```

It submits two real MockMvc HTTP requests with Basic authentication backed by
PostgreSQL Testcontainers, expects 200/409, and reads the final ticket. This
requires Docker and does not use the running development database. A manual
alternative is two terminals claiming the same new OPEN ticket, but manual
timing alone is not the repeatable regression proof.

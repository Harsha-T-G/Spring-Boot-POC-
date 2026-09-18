# Ten-minute demonstration

Status: prepared, not yet performed on the final implementation. Docker recovery
and a successful full verification are prerequisites. Do not present the plan
below as a completed live demonstration.

| Minute | Demonstrate | Explain |
| --- | --- | --- |
| 0–1 | Compose health, application startup, Swagger UI and `/api/info` | Java layers, Flyway V1/V2, Hibernate validate, dev-only OpenAPI |
| 1–2 | Public health, protected 401, valid Basic login | BCrypt DB identities, roles, stateless security |
| 2–3 | Customer creates a ticket with duplicate/cased tags | DTO validation, Set normalization, transaction, Location |
| 3–4 | Owner reads and lists filtered tickets | Database visibility, pagination and stable sorting |
| 4–5 | OPEN resolution rejected, then agent claims | Transition policy and dedicated claim operation |
| 5–6 | Other agent denied, assigned agent resolves | Ownership, fixed-clock tests, atomic state changes |
| 6–7 | Run the concurrent claim test and show 200/409 proof | Conditional UPDATE, version increment, why JVM locks fail |
| 7–8 | ADMIN summary; CUSTOMER denied report | SQL grouping, EnumMap zeroes, bounded Streams |
| 8–9 | Correlate a response trace with request/business logs | MDC finally cleanup, sanitized errors, no secrets |
| 9–10 | Run the full Maven verification lifecycle | Test coverage, corrected assumptions, review and limits |

Use [curl commands](curl-commands.md), [diagrams](diagram/diagrams.md) and
[concurrency notes](concurrency-notes.md). If Docker still fails, demonstrate
the available web/unit test results honestly and identify the unfinished
database verification; do not describe the final row as successful.

For interactive requests, open `http://localhost:8080/swagger-ui.html` and sign
in through the browser's native credential prompt. Swagger reuses Basic credentials and obtains CSRF tokens
automatically, without Authorize or lock controls. Demonstrate ADMIN-only
`POST /api/v1/users` and non-admin 403. Use a private window and close it afterward
to avoid retaining the demonstration credentials in the browser.

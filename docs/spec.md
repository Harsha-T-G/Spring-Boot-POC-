
# ResolveHub Lite Specification Index

Status: Approved for implementation on 2026-09-11

The user authorized implementation with autonomous in-scope decisions. The
following capability specifications are the product contract:

1. [Platform foundation](specs/SPEC-platform-foundation.md)
2. [Identity and access](specs/SPEC-identity-access.md)
3. [Ticket intake](specs/SPEC-ticket-intake.md)
4. [Ticket claiming](specs/SPEC-ticket-claiming.md)
5. [Ticket resolution](specs/SPEC-ticket-resolution.md)
6. [Summary reporting](specs/SPEC-summary-reporting.md)
7. [Operability and evidence](specs/SPEC-operability-evidence.md)

## Approved implementation decisions

- Maven coordinates: `com.codewalnut:resolvehub-lite`.
- Base package: `com.codewalnut.resolvehub`.
- Spring Boot: 3.5.16, updated during the user-authorized security remediation.
  This is the final OSS 3.5 release; a production support strategy or separately
  approved Boot 4 migration is still needed.
- Database configuration uses PostgreSQL environment variables and port 5432 by
  default.
- Development identities are `customer`, `agent-one`, `agent-two` and `admin`;
  their passwords come only from environment variables.
- `/api/info` exposes application name and version only.
- ADMIN ticket creation uses optional `customerId`; it is required for ADMIN and
  cannot select another user for CUSTOMER.
- Ticket references use `RH-` followed by an uppercase UUID.
- Missing target customers return 404; disabled target customers return 409.
- Pagination defaults to 20 and is limited to 100.
- Priority sorting uses CRITICAL, HIGH, MEDIUM, LOW business order.
- Claiming uses an atomic conditional PostgreSQL update and `@Version`.
- Every repeated claim returns 409.
- The PATCH status endpoint performs resolution only.
- ADMIN may resolve any claimed IN_PROGRESS ticket.
- `resolutionSummary` is persisted on Ticket and required for resolution.
- Summary maps contain every enum key with zero values where absent.
- UUID is the accepted incoming trace-id format.
- HTTP Basic remains stateless. Authenticated GET `/api/csrf` supplies a masked
  CSRF token; state changes require its cookie and `X-XSRF-TOKEN` header.
  Authentication precedes CSRF validation so invalid credentials remain 401.
- Unauthorized agents are rejected before transition details for assigned tickets.
- Tags are normalized before length validation; supplied count is checked before
  deduplication, and request collections are defensively copied.
- Delivery uses a curl command file.
- HTTP Basic challenges require the browser's native credential prompt before Swagger access.
  Swagger has no authorization controls; writes acquire CSRF tokens automatically.
  No custom login page or authentication session is used. ADMIN-only POST `/api/v1/users` provisions
  enabled users with existing roles; see identity-access for its validation contract.
- The `dev` profile enables authenticated OpenAPI JSON and Swagger UI for interactive API
  exploration; documentation is disabled by default in other profiles and does
  not weaken Basic authentication, authorization or CSRF on business routes.

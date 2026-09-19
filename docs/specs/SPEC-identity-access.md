
# Spec: Identity and Access

Status: Approved for implementation on 2026-09-11  
Module ID: `identity-access`  
Depends on: `platform-foundation`

## Assumptions

1. HTTP Basic is the authentication mechanism. The user clarified on 2026-09-13
   that Swagger must use the browser's built-in credential prompt, not a login page.
2. Development users are provisioned idempotently in the `dev` profile from
   environment-supplied passwords and stored only as BCrypt hashes.
3. Flyway creates identity tables and role records; it does not contain
   plaintext development passwords.
4. `GET /api/info` returns non-sensitive application metadata.

## Objective

Authenticate database-backed users and enforce CUSTOMER, SUPPORT_AGENT and
ADMIN permissions consistently at HTTP and service boundaries.

## Users and permissions

- **CUSTOMER:** create tickets for self and view only owned tickets.
- **SUPPORT_AGENT:** view open or assigned tickets, claim open tickets and
  resolve assigned tickets.
- **ADMIN:** view all tickets, create for enabled customers, resolve eligible
  tickets and view the summary report.

## Requirements

- **IA-REQ-001:** Users shall be persisted with UUID id, username, BCrypt
  password hash, enabled flag, role set and creation instant.
- **IA-REQ-002:** Supported roles are exactly `CUSTOMER`, `SUPPORT_AGENT` and
  `ADMIN`.
- **IA-REQ-003:** Spring Security shall use `SecurityFilterChain` and a
  database-backed `UserDetailsService` or equivalent.
- **IA-REQ-004:** Sensitive service operations shall use method security in
  addition to URL-level authentication.
- **IA-REQ-005:** `GET /api/info` and `GET /actuator/health` shall be public.
  Every other endpoint shall require authentication unless an approved spec
  explicitly says otherwise.
- **IA-REQ-006:** Missing or invalid credentials shall produce HTTP 401;
  authenticated callers lacking permission shall receive HTTP 403.
- **IA-REQ-007:** Disabled users shall not authenticate.
- **IA-REQ-008:** Development data shall include one CUSTOMER, two
  SUPPORT_AGENT users and one ADMIN without tracked plaintext passwords.
- **IA-REQ-009:** Passwords, hashes and Authorization headers shall never be
  returned or logged.
- **IA-REQ-010:** Username uniqueness shall be enforced case-insensitively in
  PostgreSQL. Lookups use the same PostgreSQL LOWER normalization as the unique
  index; distinct lowercase Unicode usernames must not make each other's login
  ambiguous.
- **IA-REQ-011:** HTTP Basic is stateless. CSRF tokens, CSRF cookies and
  user-management APIs are out of scope. Invalid credentials return 401.
- **IA-REQ-012:** Authentication enforces the BCrypt limit of 72 UTF-8 bytes.
  Overlong authentication performs a dummy BCrypt comparison and fails with 401
  rather than permitting prefix-only matching.

## Approved Swagger login extension

- Anonymous Swagger UI, API documentation and direct API calls return 401 with
  `WWW-Authenticate: Basic realm="ResolveHub"`, including HTML Accept. The browser
  supplies its native credential dialog before showing Swagger. There is no form
  login, login redirect or application authentication session. HTTPS is required
  outside localhost. Invalid credentials remain 401.
- Swagger has no Authorize button or operation lock icons. Requests use the
  browser-managed same-origin Basic credentials. No passwords or tokens are stored
  in web storage.
- User-management APIs remain out of scope. Development identities are seeded in
  the `dev` profile from environment variables.
- Verify: `./mvnw -Dtest=SwaggerBasicAuthenticationIntegrationTest,StatelessBasicSecurityIntegrationTest test`
  then `./mvnw clean verify`.

## Commands

- Focused identity tests:
  `./mvnw -Dtest=AppUserRepositoryTest,DatabaseUserDetailsServiceTest,DatabaseAuthenticationIntegrationTest,SecurityAccessIntegrationTest,SecurityErrorWebMvcTest,StatelessBasicSecurityIntegrationTest,DevelopmentUserSeederIntegrationTest test`
- Related verification: `./mvnw test`

## Project structure

- `entity` — `AppUserEntity` and `RoleEntity` persistence models.
- `domain` — canonical application roles.
- `repository` — user and role repositories.
- `security` — database user-details service and development-user provisioning.
- `config` — SecurityFilterChain and typed seed properties.
- `src/test/java/.../security` and `.../repository` — matching focused tests.

## Code style

Security dependencies are explicit and immutable:

```java
@Service
class DatabaseUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    DatabaseUserDetailsService(AppUserRepository users) {
        this.users = users;
    }
}
```

JPA entities are never exposed from controllers. Lombok `@Data` is prohibited on
user entities. Production code contains no comments; tests use
`given..._when..._then...` names and optional Arrange/Act/Assert phase comments.

## Acceptance criteria

- **IA-AC-001:** Given no credentials, when a protected endpoint is requested,
  then the response is HTTP 401.
- **IA-AC-002:** Given invalid credentials or a disabled user, when a protected
  endpoint is requested, then authentication fails with HTTP 401.
- **IA-AC-003:** Given any unauthenticated caller, when `/api/info` or
  `/actuator/health` is requested, then authentication is not required.
- **IA-AC-004:** Given an authenticated CUSTOMER, when an ADMIN-only operation
  is requested, then the response is HTTP 403.
- **IA-AC-005:** Given a configured development user password, when provisioning
  completes, then the stored value is a BCrypt hash and the raw value is not
  logged or returned.
- **IA-AC-006:** Given usernames that differ only by case, when both are
  persisted, then PostgreSQL rejects the duplicate.
- **IA-AC-007:** Given each development role, when valid credentials are used,
  then the corresponding role is loaded from PostgreSQL.

## TDD seams

| Acceptance criterion | Public seam | Planned proof |
| --- | --- | --- |
| IA-AC-001–IA-AC-004 | REST API | MockMvc security integration tests |
| IA-AC-005, IA-AC-007 | Database-backed UserDetailsService | service/repository integration tests |
| IA-AC-006 | Repository API | PostgreSQL Testcontainers test |

## Boundaries

- **Always:** use BCrypt, constructor injection, database roles and explicit
  authentication/authorization tests.
- **Ask first:** change public endpoints, add a role, change password
  provisioning or alter a security rule.
- **Never:** use `WebSecurityConfigurerAdapter`, plaintext storage, hard-coded
  passwords, or sensitive logging.

## Success criteria

Identity acceptance criteria pass and downstream specs can identify the
authenticated user and trusted role set.

## Open questions

1. What configurable usernames and environment-variable names should the four
   development users use?
2. Should `/api/info` expose only name/version, or additional non-sensitive build
   information?

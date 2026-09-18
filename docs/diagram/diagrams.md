# ResolveHub diagrams

These diagrams document code structure, schema and intended transaction
ordering.

## Components

All Java layers below run inside one Spring Boot application, not separate
deployments. Dependency arrows point toward the called component.

```mermaid
flowchart LR
    client["HTTP client"] --> trace["TraceIdFilter"]
    subgraph application["ResolveHub Lite"]
        trace --> security["SecurityFilterChain and database authentication"]
        security --> controllers["Info, Ticket and Report controllers"]
        controllers --> services["Transactional ticket and summary services"]
        services --> policies["Transition policy, reference generator, Clock"]
        services --> repositories["JPA repositories and specifications"]
        security --> users["DatabaseUserDetailsService"]
        users --> repositories
        controllers --> errors["GlobalExceptionHandler"]
        security --> securityErrors["SecurityErrorHandler"]
        errors --> envelope["ApiErrorFactory"]
        securityErrors --> envelope
    end
    repositories --> postgres[("PostgreSQL 16")]
    flyway["Flyway V1 and V2"] --> postgres
```

## Entity relationships

`appUsers`, `appUserRoles` and `ticketTags` correspond to SQL tables
`app_users`, `app_user_roles` and `ticket_tags`. The username unique index is
on `LOWER(username)`, not a case-sensitive username constraint. Selected
columns are shown; the migration files are the complete schema.

```mermaid
erDiagram
    appUsers ||--o{ appUserRoles : has
    roles ||--o{ appUserRoles : grants
    appUsers ||--o{ tickets : owns
    appUsers |o--o{ tickets : assigned
    tickets ||--o{ ticketTags : contains
    appUsers {
        uuid id PK
        string username
        string password_hash
        boolean enabled
        timestamp created_at
    }
    roles {
        uuid id PK
        string name UK
    }
    appUserRoles {
        uuid user_id PK, FK
        uuid role_id PK, FK
    }
    tickets {
        uuid id PK
        string reference_number UK
        uuid customer_id FK
        uuid assigned_agent_id FK
        string status
        string priority
        string resolution_summary
        timestamp resolved_at
        long version
    }
    ticketTags {
        uuid ticket_id PK, FK
        string tag PK
    }
```

## Competing claims

Both authenticated SUPPORT_AGENT requests can arrive together. This example
shows A winning; B can equally win. PostgreSQL serializes conflicting row
updates. B's predicate is rechecked after A commits; no retry can overwrite A.

```mermaid
sequenceDiagram
    participant AgentA
    participant AgentB
    participant RequestA
    participant RequestB
    participant PostgreSQL
    AgentA->>RequestA: POST ticket claim
    AgentB->>RequestB: POST same ticket claim
    RequestA->>PostgreSQL: Conditional OPEN update
    RequestB->>PostgreSQL: Competing conditional update
    PostgreSQL-->>RequestA: One row updated
    RequestA->>PostgreSQL: Read assigned ticket
    RequestA->>PostgreSQL: Commit transaction
    PostgreSQL-->>RequestB: Zero rows updated
    RequestB->>PostgreSQL: Check ticket exists
    PostgreSQL-->>RequestB: Ticket exists
    RequestB-->>AgentB: 409 Conflict
    RequestA-->>AgentA: 200 IN_PROGRESS
```

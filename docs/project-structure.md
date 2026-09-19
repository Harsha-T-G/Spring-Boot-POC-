# Java project structure

ResolveHub uses conventional Maven layout and a layered Spring Boot
architecture. It is one application, not one Java module per capability
specification.

```text
SpringBootPOC/
  pom.xml
  mvnw / mvnw.cmd / .mvn/
  compose.yaml
  src/
    main/
      java/com/codewalnut/resolvehub/
        ResolveHubApplication.java
        config/       Spring beans, security-chain and OpenAPI wiring
        controller/   HTTP endpoints, validation and response status
        service/      use cases, ownership and transactions
        repository/   JPA queries, filters and atomic database updates
        entity/       database mappings and guarded state changes
        domain/       enums, transition policies and normalization
        dto/          request and response records
        mapper/       entity-to-response conversion
        exception/    business failures and safe HTTP error mapping
        security/     authentication and trace filters
      resources/
        application.yml
        db/migration/ V1 and V2 Flyway migrations
    test/
      java/com/codewalnut/resolvehub/
        controller/ domain/ dto/ entity/ exception/
        repository/ security/ service/ support/
      resources/
  docs/               specifications, plans and execution evidence
  .guidelines/        agreed Java and test conventions
  .agents/skills/     repository-local SDD/TDD instructions
  target/             generated output; not application source
```

Folders beside `src` are agent workflow/documentation, not Java packages.
`entity`, `mapper`, `exception` and `security` have distinct responsibilities;
ECC's short example does not prohibit them. No unused `util`, `validation`,
service-interface or `impl` packages are required.

HTTP calls flow controller → service → repository. Controllers never query
the database, entities never become API responses, and services own transactions.
Tests mirror production packages, use Given-When-Then names and optional
Arrange/Act/Assert phase comments. Production Java remains comment-free.

Reference: Task14-Product-Catalog and installed ECC java-coding-standards /
springboot-patterns. Repository guidelines win over illustrative skill snippets.

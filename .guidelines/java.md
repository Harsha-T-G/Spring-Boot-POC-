
# Java Guidelines

These rules are adapted from the established Task 14 Product Catalog exercise
and apply to all ResolveHub Lite Java source and tests.

## Production code

- Production code contains no line, block or Javadoc comments. Use descriptive
  names, focused methods and explicit types so the code explains itself.
- Remove commented-out code, unused imports and dead branches.
- Use Java 21 language features only where they improve clarity.
- Prefer constructor injection and immutable dependencies. Field injection is
  prohibited.
- Keep services stateless and methods focused on one responsibility.
- Use descriptive role suffixes: `Controller`, `Service`, `Repository`,
  `Entity`, `Request`, `Response`, `Mapper`, `Policy` and `Exception`.
- Name methods with verbs, collections in plural and booleans with `is`,
  `has`, `can` or `should`.
- Use constants or enums instead of magic values.
- Prefer composition over inheritance.
- Create interfaces only for a genuine abstraction or replacement boundary.
  Do not create ceremonial `Service`/`ServiceImpl` pairs.
- Use Lombok selectively. Do not use `@Data` on JPA entities or security models.

## Test code

- Every test method name follows
  `givenCondition_whenAction_thenObservableOutcome`.
- Every test is organized in Given → When → Then order and covers one scenario.
- Optional phase comments are allowed only when they make the phases clearer.
  Use `// Arrange`, `// Act` and `// Assert`, matching the Product Catalog
  exercise. Combine `// Act & Assert` only when one operation naturally carries
  the assertion.
- Do not add narrative comments that merely repeat the code.
- Use descriptive local variables and expected values defined independently of
  the production algorithm.
- Assert observable state, returned values, persisted data, status codes and
  error messages/codes. Do not test private methods or internal call counts.
- Prefer real implementations, then fakes or stubs. Mock only slow,
  nondeterministic or external boundaries.
- Do not mock records, DTOs, enums or straightforward value objects.
- Keep tests independent and deterministic. Never use `Thread.sleep` to
  coordinate concurrency.
- A behavior test must fail for the expected reason before production code is
  added, then pass after the minimum implementation.
- Do not disable, weaken or delete a failing test to make the build green.

## Formatting

Order imports as Java standard library, third-party/Spring, then project imports,
with the formatter's blank-line convention. Keep test and production packages
aligned.

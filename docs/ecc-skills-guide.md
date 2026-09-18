# ECC Skills: A Practical Guide for AI-Assisted Development

ECC (Everything Claude Code) is an open-source engineering toolbox for AI coding
agents. It adds reusable workflows for planning, implementation, testing,
security, review and verification. Although the project began around Claude
Code, it also supports Codex and provides adapters for Cursor, OpenCode, Gemini
and other agent harnesses. The official source is
[affaan-m/ECC](https://github.com/affaan-m/ECC).

This guide was checked against ECC 2.2.1. ECC changes frequently, so review the
current repository README before installing a newer release.

## What is an ECC skill?

An ECC **skill** is a reusable set of instructions stored in a `SKILL.md` file.
Its YAML header tells an agent the skill's name, purpose and when it applies; the
body defines the workflow, constraints and expected verification. Examples
include `springboot-patterns`, `springboot-tdd`, `springboot-security` and
`springboot-verification`.

ECC itself is larger than a single skill. It contains skills, specialist agent
roles, commands, rules, hooks, memory tools and security checks. Installation
makes the supported components discoverable by an agent, but it does not mean
that every skill runs on every request. The agent selects a relevant skill, or
the user names one explicitly.

The main value is consistency. Instead of repeatedly prompting an agent to
plan, test, review and verify, a team can reuse a reviewed workflow:

```text
requirement -> specification -> plan -> failing test -> implementation
            -> refactor -> focused tests -> full verification -> review
```

## Installation for common coding agents

Use an official source, inspect a dry run where available, and choose **one
installation method per agent**. Stacking a plugin install and a full manual
install in the same agent can duplicate skills, commands or hooks.

The guided installer supports Claude Code and Codex together:

```bash
npx ecc-universal@2.2.1 install --guided
```

| Agent | Recommended installation |
| --- | --- |
| Claude Code | Run `npx ecc-universal@2.2.1 setup`, or enter `/plugin marketplace add https://github.com/affaan-m/ECC` and then `/plugin install ecc@ecc` inside Claude Code. Do not use both paths. |
| Codex App/CLI | Run `codex plugin marketplace add affaan-m/ECC`, then `codex plugin add ecc@ecc`, and confirm with `codex plugin list --json`. Codex separately asks whether native hooks should be trusted. |
| Cursor | Clone the repository and run `./install.sh --profile minimal --target cursor` from the repository root. This creates a project-local `.cursor/` adapter; available agent features can vary by Cursor version. |
| Gemini CLI | From a reviewed clone, run `./install.sh --profile minimal --target gemini`. |
| OpenCode | From a reviewed clone, run `npm install`, `npm run build:opencode`, and `./install.sh --profile full --target opencode --enable-hooks`. |

For Cursor, Gemini and the other adapter-based agents, first obtain the source:

```bash
git clone https://github.com/affaan-m/ECC.git
cd ECC
```

Claude Code has the most complete ECC integration. Codex has native plugin and
skill support but owns its hook trust decisions. Other adapters have partial
feature parity, so users should verify which skills, hooks and agent roles their
tool actually loads.

## How to use ECC

After installation, open the target repository and give the agent a concrete
outcome. Name the desired skills when the workflow matters. For example:

```text
Use ECC's springboot-patterns, springboot-tdd and springboot-verification
skills. Implement ticket claiming from the approved specification. Write and
run a failing test first, make the minimum change, and run the full Maven
verification before reporting completion.
```

The usual working sequence is:

1. The agent reads repository instructions such as `AGENTS.md`, the active
   specification and the relevant ECC skill.
2. SDD converts a requirement into explicit requirements, Given/When/Then
   acceptance criteria, an approved implementation plan and small tasks.
3. TDD implements one observable behavior at a time through RED, GREEN and
   REFACTOR.
4. Stack-specific skills supply conventions for architecture, security and
   testing without replacing the project's approved contract.
5. Verification skills run focused checks followed by the repository's complete
   build, and the agent reports failures honestly instead of hiding or skipping
   them.

Skills should guide engineering work, not become product requirements. Stable
business rules belong in specifications; permanent repository rules belong in
`AGENTS.md`; feature plans belong in `docs/plans`; skills describe the reusable
method. This separation keeps SDD uniform across features and commits.

## Example: ResolveHub Lite Spring Boot POC

ResolveHub Lite is a support-ticket API built with Java 21, Spring Boot,
PostgreSQL, Flyway and Testcontainers. Its development demonstrates how skills
can turn a broad exercise into a traceable implementation.

For SDD, the problem was decomposed into platform foundation, identity and
access, ticket intake, ticket claiming, ticket resolution, summary reporting,
and operability. Each capability received a specification under `docs/specs`
with stable requirement and acceptance-criterion IDs. After approval, the work
was traced into an implementation plan and small tasks under `docs/plans`.
`AGENTS.md` makes those approved specifications authoritative, which prevents a
new or updated skill from silently changing the product contract.

For TDD, each behavioral slice started with a failing test. One cycle introduced
pagination and sort validation: the policy test failed because the production
policy did not exist, the smallest implementation was added, and related tests
were rerun. A later Unicode validation defect was first reproduced with boundary
and API tests; the implementation was then aligned with PostgreSQL character
counting, focused tests passed, and the complete Maven verification was run.
Concurrency tests also proved that two agents attempting to claim one ticket
produce one winner and one conflict through database-backed arbitration rather
than JVM locking.

ECC contributed `java-coding-standards` and `springboot-patterns` as design
guidance, `springboot-security` for authentication and CSRF review, and
`springboot-verification`, `verification-loop` and `delivery-gate` for completion
checks. Project-local SDD and TDD skills remained authoritative for the workflow.

The timeline matters: the POC began with copied project-local SDD and TDD skill
files; the native ECC plugin was installed later. ECC therefore strengthened
later implementation, review and verification, but it should not be reported as
having been natively installed from the first commit. For a new project, the
better sequence is to install ECC first, select only the needed skills, establish
the repository's instruction precedence, approve the specification, and then
start the first TDD cycle.

## Practical recommendations

- Start with a minimal profile and add only skills relevant to the technology and
  task.
- Keep one installation path per coding agent and use dry runs before broad
  configuration changes.
- Explicitly name important skills in the prompt; do not assume installation
  automatically applies them.
- Keep specifications stable and versioned. Change the spec deliberately when
  behavior changes.
- Require observable tests and a full repository verification command before
  accepting an agent's completion claim.
- Treat skills as reviewed third-party code: use official sources, pin a release
  when reproducibility matters, and review hooks before trusting them.


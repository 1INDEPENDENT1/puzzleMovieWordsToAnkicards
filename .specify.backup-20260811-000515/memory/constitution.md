<!--
Sync Impact Report
Version change: none -> 1.0.0
Modified principles: initial constitution
Added sections: Core Principles, Project Constraints, Development Workflow, Governance
Removed sections: none
Templates requiring updates:
- .specify/templates/constitution-template.md: checked, no update required
- .specify/templates/plan-template.md: not present in this checkout
- .specify/templates/spec-template.md: not present in this checkout
- .specify/templates/tasks-template.md: not present in this checkout
- .specify/templates/commands/*.md: not present in this checkout
Runtime guidance checked:
- PROJECT_CONTEXT.md: checked, aligned
- AGENTS.md: updated to point agents to the constitution and project context
Follow-up TODOs: none
-->
# PuzzleMovies Vocabulary Study App Constitution

## Core Principles

### I. Ask Before Inventing Requirements
Agents MUST NOT invent missing product, architecture, data, or user-experience
requirements. If the available specs, `PROJECT_CONTEXT.md`, code, or current task do
not provide enough information, the agent MUST ask the user before deciding.

Rationale: The project depends on user-specific learning workflows and external
PuzzleMovies behavior. Guessing can create work that looks plausible but violates the
user's intent.

### II. Reflect Briefly, Then Continue By Task
Agents MUST reflect on the current task, relevant risks, and existing context before
editing. Reflection MUST stay bounded: once the next concrete step is clear, the agent
SHOULD keep moving through the active task instead of drifting into open-ended
analysis.

Rationale: Careful reasoning matters, but progress is measured by completing the
specified task with enough validation.

### III. Respect Task And Class Boundaries
Agents MUST keep changes inside the scope of the current instruction, active spec, or
explicit task. If a proposed change touches a class, package, endpoint, schema, export
format, persistence shape, authentication/session behavior, or scheduling algorithm
beyond that scope, the agent MUST ask the user before changing it.

Rationale: The application has intentionally separated responsibilities. Broad edits
can silently alter user flows or future planned work.

### IV. Preserve The Current Architecture
Agents MUST follow the project structure and responsibilities documented in
`PROJECT_CONTEXT.md`. The Spring Boot web application is the primary interface. Agents
MUST NOT reintroduce the historical CLI path or obsolete top-level `cli`, `http`,
`parser`, `export`, `model`, or `util` production packages unless the user explicitly
requests that architecture change.

Rationale: Earlier CLI work is historical context. The authoritative export flow now
lives in the web application.

### V. Protect User Data And Study Value
Agents MUST NOT store or log PuzzleMovies passwords. The application may store only the
PuzzleMovies cookie/session header needed for access. Export and review changes MUST
preserve vocabulary completeness: blank translations, missing context, duplicate
entries, and unavailable movie metadata must not cause useful study items to disappear.

Rationale: The app handles account access and personal vocabulary. Trust and data
preservation are core product requirements.

## Project Constraints

- Java 17, Spring Boot 3.2.x, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL,
  Java `HttpClient`, Jsoup, and `org.drugov:lingua-core` are the current baseline.
- PostgreSQL is the required persistent database. H2 and in-memory database substitutes
  MUST NOT become the application runtime path.
- Generated export files belong on the filesystem; persisted metadata belongs in
  PostgreSQL.
- The export format MUST remain an Anki-ready two-column TSV unless an active spec or
  user instruction changes it.
- Review-card work MUST create cards from application-owned structured export records,
  not by parsing arbitrary external Anki templates.
- Agents MUST read `PROJECT_CONTEXT.md` before substantial implementation work.

## Development Workflow

- Start from the active user request, then check `PROJECT_CONTEXT.md`, relevant specs,
  and nearby code before editing.
- Prefer existing package boundaries, Spring patterns, and test style over new
  abstractions.
- Add or update tests when behavior, contracts, persistence, scheduling, parsing,
  formatting, or UI flows change.
- Ask before making risky or broad changes, especially in authentication, sessions,
  database schema, public endpoints, export output, review scheduling, or unrelated
  classes.
- Keep generated directories such as `target/` and `exports/` out of source changes
  unless the task is explicitly about generated output.
- When implementation differs from a spec, update the related spec or documentation
  only when the user has authorized that change or the task explicitly requires it.

## Governance

This constitution governs future specs, plans, tasks, and agent implementation work for
this repository. If this constitution conflicts with older feature specs, the
constitution governs process and safety while the active user request governs product
intent.

Amendments require an explicit user request or clear user approval. Every amendment
MUST update the version, last amended date, and Sync Impact Report. Version changes use
semantic versioning:

- MAJOR: removes or redefines an existing principle in a way that changes governance.
- MINOR: adds a new principle or materially expands required behavior.
- PATCH: clarifies wording without changing required behavior.

Compliance review is required before completing substantial implementation work:
agents MUST confirm that the change stays within task boundaries, respects current
architecture, protects credentials and vocabulary data, and has appropriate validation.

**Version**: 1.0.0 | **Ratified**: 2026-07-15 | **Last Amended**: 2026-07-15

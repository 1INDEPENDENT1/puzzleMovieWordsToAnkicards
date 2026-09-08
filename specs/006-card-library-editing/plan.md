# Implementation Plan: Card Library Editing

**Branch**: `006-card-library-editing` | **Date**: 2026-08-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/006-card-library-editing/spec.md`

**Note**: This template is filled in by the `$speckit-plan` command; its definition describes the execution workflow.

## Summary

Add a signed-in learner's card library: a paginated, server-rendered table of active
review cards showing original text, translation, example, total/correct/incorrect
answer counts, and a nullable correct-answer percentage. Add content editing from both
the library and the current review card. Statistics remain derived from immutable
review attempts; edits are owner-scoped, validated, optimistic-lock protected, and do
not change answer history or scheduling.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.2.5, Spring MVC, Thymeleaf, Spring Data JPA, Bean Validation

**Storage**: PostgreSQL for review cards, attempts, optimistic version, and manual-content marker; no export-file format changes

**Testing**: Maven, JUnit 5, Mockito, Spring MVC MockMvc, Testcontainers PostgreSQL repository coverage

**Target Platform**: Server-rendered web application in modern desktop and mobile browsers

**Project Type**: Spring Boot web application

**Performance Goals**: Render a page of up to 25 active cards with aggregate statistics in one owner-scoped data query; keep every active card reachable through deterministic pagination

**Constraints**: Preserve the two-column TSV export; preserve all answer attempts and scheduling fields during content edits; keep optional translation/example values; no new runtime dependency or frontend framework

**Scale/Scope**: One learner's active cards, initially paginated in fixed 25-row pages; one new library page, two edit entry points, and a small extension to the existing review screen

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Evidence |
|------|--------|----------|
| Scope and architecture | Pass | Uses the existing Spring MVC/Thymeleaf review feature, its model, services, repositories, and static assets; no CLI or parallel frontend is introduced. |
| User data and vocabulary value | Pass | Ownership is enforced for list and update operations. Blank optional content remains valid, and edits preserve history and schedule. No passwords or session-token behavior changes. |
| Persistence and export constraints | Pass | PostgreSQL remains the review store. The plan adds only card-edit safety metadata and leaves TSV generation and files untouched. |
| Change boundaries | Pass | Changes are limited to review-card persistence, review services/repositories/controllers, review/card-library UI, and focused tests. |
| Clarifications | Pass | The approved feature specification bounds editable fields and statistics. Research resolves concurrency and import-refresh safety without changing the learner-facing scope. |

**Post-design re-check**: Pass. The Phase 1 model uses an append-only attempt history and owner-scoped queries; no change affects authentication credentials, export output, or scheduling decisions.

## Project Structure

### Documentation (this feature)

```text
specs/006-card-library-editing/
├── plan.md              # This file ($speckit-plan command output)
├── research.md          # Phase 0 output ($speckit-plan command)
├── data-model.md        # Phase 1 output ($speckit-plan command)
├── quickstart.md        # Phase 1 output ($speckit-plan command)
├── contracts/           # Phase 1 output ($speckit-plan command)
└── tasks.md             # Phase 2 output ($speckit-tasks command - NOT created by $speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
src/
├── main/
│   ├── java/com/puzzlemovies/export/
│   │   ├── model/                 # ReviewCard and ReviewAttempt persistence model
│   │   ├── repo/                  # owner-scoped card and attempt queries
│   │   ├── service/               # card library, edit, and existing review logic
│   │   └── web/                   # MVC controller and page/JSON DTOs
│   └── resources/
│       ├── templates/             # card-library and review Thymeleaf pages
│       ├── static/css/app.css     # responsive table and edit UI styles
│       ├── static/js/reviews.js   # current-card edit interaction
│       └── schema.sql             # safe PostgreSQL evolution for review cards
└── test/java/com/puzzlemovies/export/
    ├── repo/                      # PostgreSQL aggregate and migration tests
    ├── service/                   # statistics and protected edit tests
    └── web/                       # MVC and static-asset behavior checks
```

**Structure Decision**: Extend the established review feature in the existing Spring
Boot project. Keep the library server-rendered, with small JSON updates only for the
current-card editor; this matches the existing review queue and avoids a second UI
application.

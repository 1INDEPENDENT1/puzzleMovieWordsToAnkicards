# Implementation Plan: Match Words With Context Examples

**Branch**: `codex/005-match-word-examples` | **Date**: 2026-08-12 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/005-match-word-examples/spec.md`

## Summary

Preserve every saved word and example in the in-app review flow by retaining complete normalized word-to-example associations during an export. Build contextual review cards from the first two source-ordered matches per word, keep unmatched and overflow examples as standalone cards, and allow one example to be contextual for multiple words. Keep existing TSV export, review endpoints, scheduling, answer history, and duplicate-prevention behavior unchanged.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.x, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL JDBC driver, Jsoup, lingua-core, JUnit 5, Mockito, Testcontainers PostgreSQL  
**Storage**: PostgreSQL for existing review cards/history; filesystem for existing TSV; process-local completed-export cache for structured review sources  
**Testing**: JUnit 5 unit tests, Spring MVC tests, existing Testcontainers PostgreSQL support where persistence behavior needs integration coverage  
**Target Platform**: Local JVM Spring Boot web application run through Maven  
**Project Type**: Single server-rendered Spring Boot web application  
**Performance Goals**: A 50-item mixed source set creates/imports review candidates without loss or duplicate active cards; existing review session remains responsive without page reloads  
**Constraints**: Preserve two-column TSV output; do not parse TSV or arbitrary Anki/rendered HTML for review sources; retain blank translations and missing metadata; no scheduler, schema, endpoint, or new dependency changes; source order must deterministically select the two contextual cards per word  
**Scale/Scope**: Existing modest, local vocabulary collections; completed-export review sources remain process-local and require a fresh export after restart

## Constitution Check

*GATE: Passed before Phase 0 research. Re-checked after Phase 1 design.*

- **Requirement clarity**: PASS. The clarification session resolves the per-word limit, overflow behavior, source-order selection, and multi-word duplication behavior.
- **Task and class boundaries**: PASS. Changes are confined to export-to-review source creation and its tests. They do not alter authentication, scheduling, answer history, persistent schema, public endpoint shapes, or TSV semantics.
- **Architecture**: PASS. The design stays within the existing Spring Boot package boundaries and uses application-owned structured data rather than reviving the historical CLI or adding a frontend framework.
- **Data protection and study value**: PASS. No credential handling changes. Blank translations, unmatched examples, overflow examples, and multi-word associations are explicitly preserved.
- **Validation**: PASS. Unit and MVC coverage will exercise association correctness, source ordering, all card forms, blank translations, duplicate import behavior, and compatibility with existing export/review behavior.

## Project Structure

### Documentation (this feature)

```text
specs/005-match-word-examples/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── review-card-import.md
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/puzzlemovies/export/
│   │   ├── export/      # normalized associations and unchanged TSV record building
│   │   ├── review/      # structured review-source-to-draft mapping
│   │   ├── service/     # completed-export cache and card import boundary
│   │   └── web/         # existing review import controller flow
│   └── resources/
│       ├── static/js/   # existing review client
│       └── templates/   # existing review page
└── test/java/com/puzzlemovies/export/
    ├── export/
    ├── review/
    ├── service/
    └── web/
```

**Structure Decision**: Continue the single Spring Boot application. Keep normalized matching in `export`, structured review-source/draft creation in `review`, completed-export ownership in `service`, and retain the existing `web` contract/UI because its fields already represent contextual and standalone cards.

## Phase 0: Research

Research is complete in [research.md](research.md). It establishes complete association retention, structured process-local review sources, explicit creation rules, and no HTTP/schema/scheduler changes.

## Phase 1: Design

- [data-model.md](data-model.md): transient associations, structured review source, card mappings, and invariants.
- [contracts/review-card-import.md](contracts/review-card-import.md): unchanged endpoint contract with clarified content semantics.
- [quickstart.md](quickstart.md): automated and end-to-end validation scenarios.

## Constitution Check (Post-Design)

**PASS.** The final design preserves current package responsibilities, user data protections, PostgreSQL and filesystem responsibilities, TSV compatibility, and the existing review scheduler/history. It adds only app-owned transient source data required to preserve study material accurately.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| None | N/A | N/A |

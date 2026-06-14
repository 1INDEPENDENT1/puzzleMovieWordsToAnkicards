# Implementation Plan: Merge Export Logic Into Web App

**Branch**: `003-merge-export-logic` | **Date**: 2026-06-13 | **Spec**: [spec.md](C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/spec.md)
**Input**: Feature specification from `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/spec.md`

## Summary

Unify the duplicate export implementations by making the Spring Boot web export flow the single authoritative path for parsing PuzzleMovies dictionary pages, deduplicating vocabulary, matching word cards to phrase examples, formatting two-column Anki-ready TSV output, and reporting meaningful export phases plus final row counts. Preserve the existing login, session storage, async export job, progress, and download UI while replacing the simplified web parser/writer with the richer legacy export behavior backed by the local `lingua-core` lemmatization dependency.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.x, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL JDBC driver, Java HttpClient, Jsoup, org.drugov:lingua-core 0.1.0, JUnit 5, Mockito, Testcontainers PostgreSQL  
**Storage**: PostgreSQL for users/session tokens/export jobs; filesystem for generated export files; no persistence for parsed dictionary rows beyond generated files  
**Testing**: JUnit 5 unit tests for parsing/deduplication/matching/formatting; Spring Boot tests for export service and web status contract; Testcontainers retained for PostgreSQL-backed integration tests  
**Target Platform**: Local JVM web application started via Maven or executable JAR  
**Project Type**: Single Spring Boot web application  
**Performance Goals**: Typical vocabulary account completes improved export within 10 minutes; matching and deduplication operate in memory for modest PuzzleMovies account sizes; progress remains visible during multi-page exports  
**Constraints**: Web interface remains the only primary user-facing export interface; no command-line export path remains authoritative; PostgreSQL remains required; passwords are never stored or logged; output defaults to two-column TSV; normalization failures must not fail the export  
**Scale/Scope**: Single-user/local usage with 1-5 concurrent export jobs and modest dictionary sizes; source service availability remains an external dependency

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Gate: PASS. Constitution file currently contains placeholders only and defines no enforceable project rules.
- No constitutional conflicts identified with the planned Spring Boot web consolidation.

## Project Structure

### Documentation (this feature)

```text
C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- openapi.yaml
`-- tasks.md
```

### Source Code (repository root)

```text
C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   `-- com/puzzlemovies/export/
|   |   |       |-- config/
|   |   |       |-- export/
|   |   |       |-- model/
|   |   |       |-- puzzlemovies/
|   |   |       |-- repo/
|   |   |       |-- service/
|   |   |       `-- web/
|   |   `-- resources/
|   |       |-- templates/
|   |       `-- static/
|   `-- test/
|       `-- java/com/puzzlemovies/export/
|           |-- export/
|           |-- puzzlemovies/
|           |-- service/
|           `-- web/
`-- pom.xml
```

**Structure Decision**: Continue as a single Spring Boot application. New and migrated export domain logic belongs under `src/main/java/com/puzzlemovies/export/export`; web job orchestration remains in `service`; PuzzleMovies HTTP access remains in `puzzlemovies`; obsolete top-level legacy packages are removed only after the web flow passes equivalent behavior tests.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

## Phase 0: Research

Research is captured in [research.md](C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/research.md). All design uncertainties from clarification have explicit decisions.

## Phase 1: Design

Design artifacts:

- [data-model.md](C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/data-model.md)
- [contracts/openapi.yaml](C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/contracts/openapi.yaml)
- [quickstart.md](C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/quickstart.md)

## Constitution Check (Post-Design)

- Gate: PASS. Generated design artifacts preserve the single web application approach, define testable behavior, and introduce no conflicts with the placeholder constitution.

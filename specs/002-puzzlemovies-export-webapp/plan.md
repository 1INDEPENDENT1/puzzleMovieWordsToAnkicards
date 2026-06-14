# Implementation Plan: PuzzleMovies Export Web App

**Branch**: `002-puzzlemovies-export-webapp` | **Date**: 2026-02-17 | **Spec**: `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/002-puzzlemovies-export-webapp/spec.md`
**Input**: Feature specification from `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/002-puzzlemovies-export-webapp/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Build a Spring Boot 3.x web app that lets users log in to puzzle-movies.com, start exports for words/phrases/combined, track progress, and download TSV files. Persist users, session tokens, and export jobs in PostgreSQL using Spring Data JPA; run exports via server-side background tasks and expose progress via a status endpoint and UI page.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.x (Spring MVC, Thymeleaf, Spring Data JPA), PostgreSQL JDBC driver, Java HttpClient, Jsoup, org.drugov:lingua-core  
**Storage**: PostgreSQL (required; no H2/in-memory)  
**Testing**: JUnit 5, Spring Boot Test, MockMvc, Testcontainers (PostgreSQL)  
**Target Platform**: JVM server (local web app)  
**Project Type**: single (web app)  
**Performance Goals**: Typical exports complete within 10 minutes; progress page refreshes without blocking exports  
**Constraints**: Spring Boot 3.x + Maven + executable JAR; must start via `mvn spring-boot:run` and `java -jar`; no CLI-only interface; use Spring MVC + server-rendered pages (Thymeleaf preferred); store only PuzzleMovies session cookie header; never store/log passwords; no JDBC/manual SQL; refuse startup without PostgreSQL  
**Scale/Scope**: Single-user/local usage; 1-5 concurrent exports; modest dataset sizes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Gate: PASS (constitution file contains placeholders only; no enforceable rules defined)

## Project Structure

### Documentation (this feature)

```text
specs/002-puzzlemovies-export-webapp/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/
│   │   └── ...
│   └── resources/
│       ├── templates/
│       └── static/
└── test/
    └── java/
```

**Structure Decision**: Single Spring Boot web application in the Maven `src/main` and `src/test` layout.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

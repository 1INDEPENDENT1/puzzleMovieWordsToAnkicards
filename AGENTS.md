# puzzleMovieWordsToAnkicards Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-06-13

## Active Technologies
- Java 17 + Spring Boot 3.2.x, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL JDBC driver, Java HttpClient, Jsoup, org.drugov:lingua-core 0.1.0, JUnit 5, Mockito, Testcontainers PostgreSQL (003-merge-export-logic)
- PostgreSQL for users/session tokens/export jobs; filesystem for generated export files; no persistence for parsed dictionary rows beyond generated files (003-merge-export-logic)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 17+

## Code Style

Java 17+: Follow standard conventions

## Recent Changes
- 003-merge-export-logic: Consolidated duplicate export logic into the web flow with typed parsing, deduplication, contextual examples, two-column TSV output, lingua-core matching, export phases, and final row counts


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->

# puzzleMovieWordsToAnkicards Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-06-15

## Active Technologies
- Java 17 + Spring Boot 3.2.x, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL JDBC driver, Java HttpClient, Jsoup, org.drugov:lingua-core 0.1.0, JUnit 5, Mockito, Testcontainers PostgreSQL (003-merge-export-logic)
- PostgreSQL for users/session tokens/export jobs; filesystem for generated export files; no persistence for parsed dictionary rows beyond generated files (003-merge-export-logic)
- Java 17 + Spring Boot 3.2.x, Spring MVC, Thymeleaf, Spring Data JPA, PostgreSQL JDBC driver, JUnit 5, Mockito, Testcontainers PostgreSQL; no new runtime dependencies for in-app review cards (004-custom-anki-review)
- PostgreSQL for users/session tokens/export jobs/review cards/review attempts/scheduling state; filesystem remains for generated export files (004-custom-anki-review)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 17+
- `mvn test`
- `mvn spring-boot:run`

## Code Style

Java 17+: Follow standard conventions

## Recent Changes
- 003-merge-export-logic: Consolidated duplicate export logic into the web flow with typed parsing, deduplication, contextual examples, two-column TSV output, lingua-core matching, export phases, and final row counts
- 004-custom-anki-review: Planned first-class in-app Anki-style review cards with binary scheduling, review history, lookup actions, responsive card UI, sweep transitions, and reduced-motion support


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->

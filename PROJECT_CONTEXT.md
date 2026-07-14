# Project Context: PuzzleMovies Vocabulary Study App

Last reviewed: 2026-07-14

## Mission

This project helps PuzzleMovies users turn the vocabulary they save while watching movies into study material. The current product signs in to puzzle-movies.com, fetches the user's saved words and phrases, enriches word cards with contextual phrase examples, deduplicates and normalizes the vocabulary, and exports an Anki-ready two-column TSV file.

The longer-term direction is to reduce dependence on external Anki templates by adding an in-app Anki-style review mode with app-owned review cards, binary scheduling, lookup actions, and polished card transitions.

## Current State

- Implemented primary app: Spring Boot web application.
- Implemented main feature: login, export menu, async export jobs, progress polling, downloadable TSV output.
- Implemented export behavior: typed word/phrase parsing, duplicate merging, blank translation preservation, contextual examples, two-column TSV formatting, progress phases, row counts.
- Planned but not implemented in production source yet: first-class in-app review cards from `specs/004-custom-anki-review`.
- Historical note: `specs/001-puzzlemovies-dict-export` describes an older CLI exporter. That CLI path was intentionally superseded by the web app and removed/consolidated by `specs/003-merge-export-logic`. Do not recreate top-level `cli`, `http`, `parser`, `export`, `model`, or `util` packages as active production paths.

## Technology Stack

- Java 17.
- Spring Boot 3.2.5.
- Spring MVC controllers and server-rendered Thymeleaf templates.
- Spring Data JPA with Hibernate.
- PostgreSQL only for persistent data; H2/in-memory databases are intentionally forbidden by the specs.
- Java `HttpClient` for PuzzleMovies HTTP requests.
- Jsoup 1.17.2 for parsing PuzzleMovies HTML pages.
- `org.drugov:lingua-core:0.1.0` with `LuceneLemmatizer` for local linguistic normalization.
- JUnit 5, Spring Boot Test, Mockito-style tests, and Testcontainers PostgreSQL dependency for test support.
- Maven build, executable JAR packaging.

## How To Run

Main commands:

```powershell
mvn test
mvn spring-boot:run
```

The app listens on port `8080` by default. Start at:

```text
http://localhost:8080/login
```

The app requires PostgreSQL. `src/main/resources/application.yml` currently contains local development datasource settings and an export output directory. Prefer environment-specific overrides for real work, and do not spread real credentials into docs, logs, commits, or examples.

## Runtime Configuration

- `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password`: PostgreSQL connection.
- `spring.jpa.hibernate.ddl-auto`: currently `update` for local schema evolution.
- `export.output-dir`: filesystem location for generated TSV files.
- `export.puzzle-movies.base-url`: PuzzleMovies base URL, currently `https://puzzle-movies.com`.

Generated files are stored on disk and referenced from `ExportJob`; parsed dictionary rows are transient and are not persisted beyond the generated export file.

## Root Directory Guide

- `src/`: main application and test source.
- `src/main/java/com/puzzlemovies/export/`: production Java package for the Spring Boot app.
- `src/main/resources/templates/`: Thymeleaf pages.
- `src/main/resources/static/`: CSS and JavaScript assets.
- `src/test/java/com/puzzlemovies/export/`: unit and integration-style tests.
- `specs/`: Spec Kit feature artifacts. Read these before major changes.
- `.agents/`: local agent skills used in this workspace.
- `.specify/`: Spec Kit templates/configuration.
- `.codex/`: local Codex runtime/cache/session state; do not treat as application source.
- `exports/`: generated TSV output files; do not treat as source.
- `target/`: Maven build output; generated.
- `.idea/`: IDE metadata.
- `AGENTS.md`: short generated development guideline file.
- `pom.xml`: Maven build and dependency definition.
- `set-codex.sh`: local helper script.

## Java Package Guide

- `com.puzzlemovies.export`: Spring Boot entrypoint.
- `config`: application beans and configuration properties.
  - `AsyncConfig`: export task executor.
  - `HttpClientConfig`: shared Java `HttpClient`.
  - `LemmatizerConfig`: `lingua-core` lemmatizer bean.
  - `ExportProperties`: `export.*` configuration binding.
- `model`: JPA entities and enums.
  - `User`: local user identity by email.
  - `PuzzleSessionToken`: stored PuzzleMovies cookie header only; passwords must never be stored.
  - `ExportJob`: export status/progress/output metadata.
  - `ExportType`: `WORDS`, `PHRASES`, `COMBINED`.
  - `ExportStatus`: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`.
  - `ExportPhase`: `PENDING`, `FETCHING_WORDS`, `FETCHING_PHRASES`, `PARSING`, `DEDUPLICATING`, `MATCHING`, `WRITING`, `COMPLETED`, `FAILED`.
- `repo`: Spring Data repositories for users, session tokens, and export jobs.
- `puzzlemovies`: HTTP clients and PuzzleMovies-specific errors.
  - `PuzzleMoviesAuthClient`: signs in and extracts session cookies.
  - `PuzzleMoviesDictionaryClient`: fetches paginated word/phrase HTML pages and detects expired sessions.
- `export`: transient export domain logic.
  - `DictionaryParser`: parses HTML into typed words and phrases.
  - `VocabularyNormalizer`: lemma/cleaned-token normalization with fallback behavior.
  - `VocabularyDeduplicator`: deterministic duplicate merging.
  - `DictionaryMatcher`: matches words to phrase examples, capped at two examples.
  - `ExportRecordBuilder`: builds word/phrase rows and HTML-ish back content.
  - `AnkiExportFormatter`: sanitizes and writes two-column TSV content.
- `service`: application use cases.
  - `AuthService`: stores users and PuzzleMovies session tokens after successful login.
  - `ExportService`: creates and runs async exports end to end.
- `web`: MVC controllers and session helpers.
  - `LoginController`: `/login`.
  - `ExportMenuController`: `/exports/menu`.
  - `ExportController`: start export, progress page, status JSON.
  - `ExportDownloadController`: completed TSV download.
  - `SessionUserResolver`: maps HTTP session to current user.
  - `GlobalExceptionHandler`: simple web error handling.

## Main User Flow

1. User opens `/login`.
2. User submits PuzzleMovies email/password.
3. App authenticates through PuzzleMovies and stores only the resulting cookie header in PostgreSQL.
4. User opens `/exports/menu` and chooses `WORDS`, `PHRASES`, or `COMBINED`.
5. `ExportService` creates an `ExportJob` and runs it asynchronously.
6. App fetches words and/or phrases from PuzzleMovies.
7. HTML pages are parsed into typed `DictionaryWord` and `DictionaryPhrase` values.
8. Vocabulary is deduplicated and normalized.
9. Words are matched to up to two phrase examples unless the export is phrase-only.
10. Records are formatted as two-column TSV and written under `exports/`.
11. Progress page polls `/exports/{id}/status` and shows phase, percent, and final row count.
12. Completed jobs can be downloaded from `/exports/{id}/download`.

## HTTP Endpoints

- `GET /login`: render login page.
- `POST /login`: submit PuzzleMovies credentials.
- `GET /exports/menu`: choose export type.
- `POST /exports`: start an export job.
- `GET /exports/{id}`: render export progress page.
- `GET /exports/{id}/status`: return export job JSON status.
- `GET /exports/{id}/download`: download completed TSV or show not-ready response.

Planned review endpoints in `specs/004-custom-anki-review/contracts/openapi.yaml`:

- `GET /reviews`.
- `POST /reviews/cards`.
- `GET /reviews/next`.
- `POST /reviews/cards/{id}/answer`.

These review endpoints should not be assumed to exist until the 004 tasks are implemented.

## Data Model Summary

Persisted now:

- `users`: email and timestamps.
- `puzzle_session_tokens`: one session token/cookie header per user.
- `export_jobs`: user, type, status, phase, progress, timestamps, errors, output file metadata, row count.

Transient during export:

- `DictionaryWord`: source text, translations, matching identity, fallback flag.
- `DictionaryPhrase`: source text, translations, movie metadata, matching tokens, dedupe identity, fallback flag.
- `PhraseExample`: phrase selected as context for a word.
- `ExportRecord`: front text, back content, internal record kind.

Planned review model:

- `ReviewCard`: persistent app-owned card with original text, instance, translation/back content, source context, scheduling state, due time, interval, ease, counts, and stable duplicate key.
- `ReviewAttempt`: append-only answer history.
- `ReviewSession`: short-lived review run state.
- `LookupAction`: derived dictionary/translation/pronunciation links.

## Spec Roadmap

- `001-puzzlemovies-dict-export`: original vocabulary-to-Anki concept, initially framed as a CLI. Useful for domain intent, not current architecture.
- `002-puzzlemovies-export-webapp`: migration to Spring Boot web app with login, export jobs, PostgreSQL, progress, and TSV download.
- `003-merge-export-logic`: completed consolidation of richer legacy export behavior into the web flow. Treat this as the authoritative export behavior.
- `004-custom-anki-review`: planned in-app review mode with binary scheduling, review cards, attempts, lookup actions, responsive UI, sweep animations, and reduced-motion support. Tasks are currently unchecked in `specs/004-custom-anki-review/tasks.md`.

## Testing Notes

Existing tests cover:

- Auth service behavior.
- PuzzleMovies auth and dictionary HTTP clients.
- Dictionary parsing.
- Vocabulary normalization and fallback.
- Deduplication.
- Word-to-phrase matching.
- Export record building.
- TSV formatting.
- Export service behavior.
- Architecture check that obsolete top-level export packages are absent.

Run the full suite with:

```powershell
mvn test
```

For review-card work, add tests before implementation as specified in `specs/004-custom-anki-review/tasks.md`: repository tests, scheduler tests, import service tests, review service tests, MVC tests, and static asset smoke checks.

## Agent Guidance

- Prefer existing Spring Boot patterns and package boundaries.
- Keep the web app as the primary interface.
- Do not reintroduce a CLI as the main product path.
- Do not store or log PuzzleMovies passwords.
- Store only PuzzleMovies cookie/session headers.
- Keep PostgreSQL as the required database.
- Keep export files on the filesystem and metadata in PostgreSQL.
- Preserve two-column TSV output: front text and back content.
- Preserve vocabulary even when translations, examples, or movie metadata are missing.
- When adding review cards, generate them from app-owned structured export records, not by parsing arbitrary Anki templates.
- Keep accessibility and reduced-motion behavior in mind for the planned review UI.
- Avoid editing generated directories (`target/`, `exports/`) unless the task is explicitly about generated output.

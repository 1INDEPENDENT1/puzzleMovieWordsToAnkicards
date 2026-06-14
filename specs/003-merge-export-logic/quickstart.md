# Quickstart: Merge Export Logic Into Web App

## Prerequisites

- Java 17 available on `PATH`.
- Maven available on `PATH`, or use the local bundled Maven at `C:\Users\George\.codex\tools\apache-maven-3.9.11\bin\mvn.cmd`.
- PostgreSQL running and reachable by the app configuration.
- Local Maven dependency `org.drugov:lingua-core:0.1.0` installed or otherwise resolvable from the local Maven repository.
- A PuzzleMovies account with saved words and/or phrases for manual end-to-end verification.

## Build and Test

```powershell
mvn test
```

If `mvn` is not on `PATH`:

```powershell
& 'C:\Users\George\.codex\tools\apache-maven-3.9.11\bin\mvn.cmd' test
```

Expected coverage for this feature:

- Typed parsing of word pages and phrase pages.
- Deduplication of words and phrases.
- Merge of duplicate conflicting translations into a single back field.
- Lemmatized word-to-phrase matching with fallback to cleaned tokens.
- Two-example cap per word.
- Two-column TSV formatting that protects row boundaries.
- Export service status, phase, row count, and failure behavior.
- Representative-size 1,000-row builder/formatter smoke check for the 10-minute success criterion.

## Run Locally

```powershell
mvn spring-boot:run
```

Open:

```text
http://localhost:8080/login
```

## Manual Verification Flow

1. Sign in with a PuzzleMovies account.
2. Open the export menu.
3. Start a `COMBINED` export.
4. Confirm the progress page shows status, percent, and a meaningful phase.
5. Wait for completion.
6. Confirm the completed page/status shows a final row count.
7. Download the TSV.
8. Confirm the file has exactly two columns: front text and back content.
9. Confirm word rows include up to two contextual phrase examples when matching phrases exist.
10. Confirm phrase rows are present as standalone rows in the combined export.

## Export Type Checks

- `WORDS`: output includes word rows only; saved phrases may enrich word backs as contextual examples.
- `PHRASES`: output includes standalone phrase rows only.
- `COMBINED`: output includes word rows with examples plus standalone phrase rows.

## Failure Checks

- Expired PuzzleMovies session marks the export failed with a clear message.
- Empty dictionaries complete without crashing and produce a valid empty or minimal file according to selected export type.
- Normalization failures do not fail the export; affected entries use cleaned original tokens for matching/deduplication.

## Consolidation Check

The web flow is the authoritative export path. Obsolete standalone production packages under `src/main/java/cli`, `http`, `parser`, `export`, `model`, and `util` have been removed, and `ExportArchitectureTest` verifies they stay absent.

## Implementation Summary

- Typed export parsing now produces `DictionaryWord` and `DictionaryPhrase` transient models.
- Web exports deduplicate vocabulary, match words to up to two phrase examples, and write exactly two TSV columns.
- `WORDS` exports word rows enriched by phrases, `PHRASES` exports phrase rows only, and `COMBINED` exports both.
- Progress status JSON and the progress page expose phase and final row count.

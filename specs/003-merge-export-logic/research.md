# Research: Merge Export Logic Into Web App

## Decision: Make the web export flow the only authoritative export path

**Rationale**: The current codebase has a Spring Boot web export path and a disconnected legacy command-line export path. Keeping both active would preserve duplication and make future behavior changes risky. The web flow already owns user login, session storage, background jobs, progress, and download handling, so it is the right place to host the richer export behavior.

**Alternatives considered**:
- Keep CLI and web flows in parallel: rejected because it perpetuates duplicate business logic.
- Move export logic into a separate module first: deferred because the current project is a single application and the immediate goal is consolidation inside the web app.

## Decision: Use typed in-memory export domain models instead of JPA entities for dictionary rows

**Rationale**: Parsed words, phrases, phrase examples, and export records are transient products of one export job. Persisting them would add storage churn and schema complexity without a user requirement. The existing persisted boundary remains `ExportJob`, `User`, and `PuzzleSessionToken`.

**Alternatives considered**:
- Persist parsed dictionary rows: rejected because rows are only needed to generate the output file.
- Reuse the current generic `DictionaryEntry`: rejected because it loses word/phrase distinction and phrase context metadata.

## Decision: Use `org.drugov.lingua.morph.Lemmatizer` for matching identities

**Rationale**: The project already depends on the local `org.drugov:lingua-core:0.1.0` library. Its public `Lemmatizer` API provides tokenization and English/Russian lemmatization, which directly supports inflected-form matching between saved words and saved phrases. This replaces the legacy hand-written normalizer for matching/deduplication identity.

**Alternatives considered**:
- Keep legacy `TextNormalizer`: rejected because it only lowercases and strips punctuation, so it cannot handle inflected forms as well.
- Use the legacy `org.drugov.TextLemmaParser` API: rejected because the newer `org.drugov.lingua.morph.Lemmatizer` interface is cleaner and better suited as an application dependency.

## Decision: Fall back to cleaned original tokens when normalization fails

**Rationale**: A single unknown or malformed token should not fail a full vocabulary export. Falling back to a cleaned original token preserves export completeness and still gives deterministic deduplication/matching behavior for affected entries.

**Alternatives considered**:
- Fail the entire export on normalization failure: rejected because it is too brittle for user-generated vocabulary.
- Skip failed tokens: rejected because it may silently reduce matching and deduplication quality.

## Decision: Export type behavior follows clarified web semantics

**Rationale**: The user clarified that `WORDS` may use saved phrases as contextual examples but must not include those phrases as standalone rows; `PHRASES` exports only phrase rows; `COMBINED` exports word rows with examples plus standalone phrase rows. This keeps user choices meaningful while preserving context quality for word exports.

**Alternatives considered**:
- Restrict examples to combined exports only: rejected because word-only exports would lose the main learning benefit.
- Remove separate export types: rejected because the existing UI already exposes the three choices and users may want narrower exports.

## Decision: Keep two-column Anki-ready TSV output

**Rationale**: The clarified output contract is `front` plus `back` content. This matches common Anki import workflows and the legacy formatter shape. Type distinctions remain implicit in content/formatting rather than requiring a third column.

**Alternatives considered**:
- Add an item-type column: rejected because it complicates the user's Anki import setup.
- Vary column shape by export type: rejected because it makes testing and importing less predictable.

## Decision: Merge conflicting duplicate translations into one field

**Rationale**: The clarified behavior requires preserving distinct non-blank translations from duplicate groups. Combining translations into one back-content field avoids losing user data while still producing one row per normalized vocabulary item.

**Alternatives considered**:
- Keep first translation only: rejected because it can discard valid alternate translations.
- Export conflicts as separate rows: rejected because it violates the duplicate-merge requirement.

## Decision: Add export phases and final row counts to progress reporting

**Rationale**: Long exports need enough feedback to reassure the user that work is progressing. Phase labels and final row counts give useful information without making the progress UI overly detailed.

**Alternatives considered**:
- Keep percent/status only: rejected because it is less informative for multi-stage export work.
- Show detailed internal counts for every step during export: deferred because it adds UI/API complexity beyond the clarified requirement.

## Decision: Remove legacy top-level packages only after equivalent behavior is covered

**Rationale**: Legacy packages currently contain the richer parsing, matching, deduplication, and formatting behavior. They should be used as reference behavior during migration, then removed once the web package has equivalent tests and functionality.

**Alternatives considered**:
- Delete legacy code first: rejected because it increases risk of losing behavior details.
- Leave legacy packages permanently: rejected because it keeps duplicate business logic in the project.

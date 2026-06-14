# Data Model: Merge Export Logic Into Web App

## Entity: DictionaryWord

**Represents**: A saved vocabulary word parsed from PuzzleMovies dictionary pages for a single export job.

**Fields**:
- `sourceText` (string, required): The word shown on the front of the card.
- `translations` (ordered set of strings, optional): Distinct non-blank translations gathered from merged duplicate word rows.
- `matchingIdentity` (string, required): Normalized identity used for deduplication and phrase matching.
- `normalizationFallbackUsed` (boolean): Whether matching identity used cleaned original text because lemmatization could not process the token.

**Validation Rules**:
- `sourceText` must be non-blank after trimming.
- `matchingIdentity` must be non-blank; use cleaned original token fallback if needed.
- Blank translations are allowed and must not cause row exclusion.

**Uniqueness**:
- Word duplicates are grouped by `matchingIdentity`.
- Duplicate groups produce one exported word row.

## Entity: DictionaryPhrase

**Represents**: A saved phrase or sentence parsed from PuzzleMovies dictionary pages for a single export job.

**Fields**:
- `sourceText` (string, required): The phrase shown as standalone front text or contextual example text.
- `translations` (ordered set of strings, optional): Distinct non-blank translations gathered from merged duplicate phrase rows.
- `movieTitle` (string, optional): Movie context title when provided by the source page.
- `movieUrl` (string, optional): Movie context URL when provided by the source page.
- `matchingTokens` (ordered set of strings, required): Normalized identities available for matching word rows to this phrase.
- `dedupeIdentity` (string, required): Normalized identity used to merge duplicate phrases.
- `normalizationFallbackUsed` (boolean): Whether any matching identity used cleaned original text because lemmatization could not process the token.

**Validation Rules**:
- `sourceText` must be non-blank after trimming.
- `dedupeIdentity` must be non-blank.
- `matchingTokens` may be empty only when no token can be extracted; such a phrase still exports as a standalone phrase row when selected.
- Blank translations are allowed.

**Uniqueness**:
- Phrase duplicates are grouped by `dedupeIdentity`.
- Duplicate groups produce one standalone phrase row when phrase rows are included by the export type.

## Entity: PhraseExample

**Represents**: A phrase selected as contextual content for one exported word.

**Fields**:
- `wordIdentity` (string, required): Matching identity of the word this example supports.
- `phraseSourceText` (string, required): Original phrase text.
- `phraseTranslations` (ordered set of strings, optional): Translations for the phrase example.
- `movieTitle` (string, optional): Movie context title.
- `movieUrl` (string, optional): Movie context URL.

**Validation Rules**:
- A word may have at most two `PhraseExample` values in the exported record.
- Examples must come from saved phrases owned by the exporting user/session.

## Entity: ExportRecord

**Represents**: A single two-column row in the downloadable Anki-ready file.

**Fields**:
- `front` (string, required): Word or phrase source text.
- `back` (string, required, may be blank): Translation content plus contextual examples when present.
- `recordKind` (enum: `WORD`, `PHRASE`, internal): Used by the export builder for ordering/testing; not exported as a separate column.

**Validation Rules**:
- Output row must contain exactly two columns.
- Tabs, carriage returns, and line breaks must not corrupt row boundaries.
- HTML used inside the back field must be escaped except for intentionally generated formatting tags.

## Entity: ExportJob

**Represents**: A user-started export process.

**Existing Fields Retained**:
- `id`
- `user`
- `type`
- `status`
- `progressPercent`
- `startedAt`
- `completedAt`
- `errorMessage`
- `outputFilePath`
- `outputFileName`
- `rowCount`
- `createdAt`
- `updatedAt`

**New/Changed Fields**:
- `phase` (enum/string, required while running): User-visible current export phase.

**State Transitions**:
- `PENDING` -> `RUNNING`
- `RUNNING` -> `COMPLETED`
- `RUNNING` -> `FAILED`

**Phase Transitions**:
- `PENDING`
- `FETCHING_WORDS`
- `FETCHING_PHRASES`
- `PARSING`
- `DEDUPLICATING`
- `MATCHING`
- `WRITING`
- `COMPLETED`
- `FAILED`

**Validation Rules**:
- `phase` must be compatible with `status`.
- `rowCount` must be present after successful completion.
- `outputFilePath` and `outputFileName` must be present after successful completion.

## Relationships

- One `ExportJob` produces many transient `DictionaryWord` and `DictionaryPhrase` values during generation.
- `DictionaryWord.matchingIdentity` maps to zero, one, or two `PhraseExample` values.
- One `DictionaryPhrase` can be used as a contextual example for multiple words.
- `ExportRecord` values are produced from selected words and/or phrases according to export type:
  - `WORDS`: word records only; phrases may contribute contextual examples.
  - `PHRASES`: standalone phrase records only.
  - `COMBINED`: word records with examples plus standalone phrase records.

## Derived Data Rules

- Translation merge order should be deterministic and preserve distinct non-blank translations.
- Blank translation entries do not add text but do not remove the vocabulary item.
- Normalization failure uses cleaned original tokens for identity.
- Contextual examples are selected deterministically from matching phrases and capped at two per word.

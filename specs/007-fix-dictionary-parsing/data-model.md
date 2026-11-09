# Data Model: Dictionary Fetch, Parse and Completion

**Scope**: Transient dictionary/source data and a projection of existing ExportJob. No database schema changes, data migrations, changes to ReviewCard/ReviewAttempt, card identity or scheduling.

## DictionarySection

Values: `WORD`, `PHRASE`; mapped to external `item=word|phrase`. ExportType remains WORDS/PHRASES/COMBINED. WORDS fetches phrases for existing context matching; PHRASES does not fetch words; COMBINED fetches both.

## SourcePosition

| Field | Type | Rule |
| --- | --- | --- |
| section | DictionarySection | Required; verified against response active tab |
| pageNumber | int | Positive requested page, validated against response paginator |
| rowNumber | int | 1-based ordinal within dictionary table, before field parsing |
| sourceItemId | optional String | Source row identifier if available; transient technical provenance only |

Distinct positions remain distinct even when text is identical. Same DOM row reached through multiple selectors emits one position. These identifiers never become card content keys.

## DictionaryPage and PageInspection

`DictionaryPage(section, pageNumber, sourceUri, html, inspection)` is transient. `sourceUri` contains only allowed public dictionary parameters. No cookie/header/password fields.

`PageInspection` contains selected section, declared source total, current page, candidate next page and recognized/authenticated shell flags. Only active dictionary table rows count toward source completeness; unrelated tables do not count. Page links are parsed structurally; requests retain explicit section and permitted origin. A terminal page has no valid forward link, and a stable counter mismatch is recorded separately after traversal (FR-L07), rather than treated as a fetch failure. Zero-row responses with positive total do not become EMPTY.

Use `DictionaryPageInspector.inspect(section, pageNumber, html)` from both client validation and parser setup so recognition rules have one owner. It does not extract vocabulary fields or apply linguistic normalization.

## RawDictionaryEntry and SourceExample

| Raw field | Meaning |
| --- | --- |
| position | Required SourcePosition |
| sourceText | Displayed original, without hidden duplicate, word translation or movie labels |
| translations | Ordered available translation values; empty allowed |
| examples | Ordered optional SourceExample values; no invented values |
| sourceLabel | Optional source label, e.g. `из субтитров` |
| movieTitle / movieUrl | Optional observed context; relative href resolved against source base |

`SourceExample` has `sourceText`, optional own translations, source label/movie context. Word inline example text is preserved separately; absent fields stay absent. Saved PHRASE original/translation are the phrase's own fields, not a second copy of a WORD inline example.

WORD source comes from visible original element, PHRASE source from phrase English element. Concatenate existing text nodes and normalize layout whitespace; do not insert separators around every inline span. `to`, articles, case and punctuation survive. Source field extraction must be independently testable before normalizer.

## DictionaryParseResult<T>

Proposed record: `DictionaryParseResult<T>(List<T> records, List<RawDictionaryEntry> acceptedSourceEntries, List<ParseIssue> issues, int sourceRecordCount)`.

- `T` is existing DictionaryWord or DictionaryPhrase; these retain their existing matching/dedupe semantics.
- `ParseIssue(position, reason)` uses safe enumerated reasons for a rejected source record. No full raw row, password, cookie or arbitrary exception body.
- `unparsedRecordCount` is count of distinct rejected SourcePositions, not issue count.
- Invariant before semantic deduplication: `sourceRecordCount = acceptedSourceEntries.size + unparsedRecordCount`.
- Missing translation/example/movie metadata does not create a rejection.
- Unknown whole-page structure throws a source-structure error; it is not a per-row partial result.
- Existing normalizer fallback behavior is preserved. A record that cannot enter typed processing must be accounted for explicitly, never silently dropped by `continue`.
- Accepted raw entries retain source-only inline context independently from typed matcher inputs. They do not create new phrase associations automatically.

`DictionaryParser.parseWordPages(List<DictionaryPage>)` and `parsePhrasePages(List<DictionaryPage>)` return these results. Existing list-returning helpers can be removed after all callers/tests move to the result contract; tests must not continue using a facade that hides parse issues. `parseSourceEntries(DictionaryPage)` exposes the raw extraction boundary for independent expectations.

## ExportCompletionInfo

Projection fields: `completeness: FULL|PARTIAL`, `unparsedRecordCount: int`, `sourceCountDiscrepancies: List<SourceCountDiscrepancy>`, `warningMessage: nullable String`. It is not a new persisted entity or enum column.

| Existing job state | Completion projection | Availability |
| --- | --- | --- |
| PENDING/RUNNING | No usable completion yet | Neither download nor card creation |
| COMPLETED, no partial marker | FULL, count=0, warning=null | Existing download and process-local review rules |
| COMPLETED, valid partial marker | PARTIAL, rejected>0 and/or nonempty sourceCountDiscrepancies, generated warning | Both actions permitted after warning presentation; review requires structured source |
| FAILED | No usable artifact | Neither action |
| COMPLETED, malformed marker | Completion metadata error | Block both actions; never silently infer FULL |

COMPLETED means writing a usable artifact finished; UI must render PARTIAL distinctly from full success. Only at least one accepted source record can yield usable PARTIAL; all-rejected is an error, not EMPTY. A full confirmed empty dataset may yield existing empty export behavior with rowCount=0.

### Persistence without schema change

`ExportCompletionMetadataCodec` encodes partial completion in existing ExportJob.errorMessage:

`DICT_META_V1:{"completeness":"PARTIAL","unparsedRecordCount":2}`

`ExportCompletionMetadataCodec.decode(ExportJob)` validates state, the entire `DICT_META_` marker family, known version, valid JSON and a nonnegative integral rejected count; PARTIAL requires rejected>0 or at least one valid sourceCountDiscrepancy. The optional V1 array stores unique sections with nonnegative, unequal declaredTotal/retrievedTotal. Legacy V1 with positive rejected count remains valid. Unknown versions, nonintegral/missing counts and malformed JSON fail closed for both actions; they never fall back to legacy FULL. Application code owns all encoding/decoding. Public DTO/UI never displays this storage envelope as error text; warning is generated from trusted copy and count. FULL new jobs use null; legacy rows remain untouched with prior behavior. This does not assert legacy data was parsed by the corrected implementation.

Do not add columns, PARTIAL persisted enum value, sidecar files or migrations. Do not include vocabulary payloads or source HTTP bodies in metadata. Preserve the existing 4000-character limit.

## GeneratedExportData

One immutable process-local aggregate per job: existing export records, existing ReviewExportSource, accepted raw source entries and ExportCompletionInfo. This may replace the two current internal cache maps while preserving public accessors and ownership checks.

Lifecycle: created only after complete fetch, parse accounting, existing dedupe/matching and successful TSV write; cached before job completion is published. Failed jobs expose no generated result. Restart loses structured source as today: downloads use persisted job/file metadata, review import requires a new export and returns existing 409. No TSV reparse.

ReviewCardDraftFactory and ReviewCardImportService retain existing identities, override rules, matching cap/source order and scheduling/history behavior. Adding accepted raw context to GeneratedExportData does not change those algorithms.

## WarningPresentation

Existing HTTP session may hold a presentation marker keyed by owned job id and fingerprint of decoded completion metadata. Rendering the progress page with the partial warning registers that presentation; direct download/import without it redirects to the warning-bearing progress page before the action. A different job or changed metadata does not reuse the marker. It is presentation state only, not a new credential, permission or persisted entity.

## State Transitions

`PENDING → RUNNING → COMPLETED(FULL)` for complete accepted input, including confirmed empty.

`PENDING → RUNNING → COMPLETED(PARTIAL)` for complete traversal with accepted records and rejected individual records and/or a stable source counter discrepancy.

`PENDING → RUNNING → FAILED` for fetch/auth/page recognition/completeness failure, all-rejected input, write failure or other unrecoverable processing failure.

Source total and raw accepted/rejected counts are separate from final TSV rowCount. A valid partial download does not become a failure simply because review cache was lost after restart.

## SourceCountDiscrepancy (approved 2026-11-09)

Transient record `(section, declaredTotal, retrievedTotal)` uses source rows before parsing/dedupe. Unique sections only, counts nonnegative and unequal. It does not claim the missing counter difference is a set of parse rejections. Stored in the same versioned completion envelope, no schema changes. Example: `DICT_META_V1:{"completeness":"PARTIAL","unparsedRecordCount":0,"sourceCountDiscrepancies":[{"section":"PHRASE","declaredTotal":766,"retrievedTotal":756}]}`.

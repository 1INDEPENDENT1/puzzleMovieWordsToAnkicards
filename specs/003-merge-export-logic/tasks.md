# Tasks: Merge Export Logic Into Web App

**Input**: Design documents from `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Required by FR-014. Unit and service/web tests must be added before implementation tasks for each story where practical.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Repository root: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards`
- Main Java: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java`
- Test Java: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java`
- Feature docs: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm project dependencies and establish the shared package structure for the unified web export flow.

- [X] T001 Verify `org.drugov:lingua-core:0.1.0`, Spring Boot test, Mockito, and Testcontainers dependencies are present in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/pom.xml`
- [X] T002 [P] Create reusable sample dictionary HTML and expected export row fixtures in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/ExportTestFixtures.java`
- [X] T003 [P] Create reusable web status JSON and session/job fixtures in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/web/WebExportTestFixtures.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain types, normalization adapter, and export job phase support that all user stories depend on.

**CRITICAL**: No user story work should begin until this phase is complete.

- [X] T004 [P] Create `ExportPhase` enum with PENDING, FETCHING_WORDS, FETCHING_PHRASES, PARSING, DEDUPLICATING, MATCHING, WRITING, COMPLETED, FAILED in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ExportPhase.java`
- [X] T005 Add `phase` field, getter, setter, default initialization, and status-compatible updates to `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ExportJob.java`
- [X] T006 [P] Create transient `DictionaryWord` record/class per data-model fields in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/DictionaryWord.java`
- [X] T007 [P] Create transient `DictionaryPhrase` record/class per data-model fields in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/DictionaryPhrase.java`
- [X] T008 [P] Create `PhraseExample` record/class per data-model fields in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/PhraseExample.java`
- [X] T009 [P] Create two-column `ExportRecord` record/class with internal `RecordKind` in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/ExportRecord.java`
- [X] T010 [P] Add Spring bean configuration for `org.drugov.lingua.morph.Lemmatizer` using `LuceneLemmatizer` in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/config/LemmatizerConfig.java`
- [X] T011 [P] Add unit tests for `VocabularyNormalizer` lemma identity and cleaned-token fallback in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/VocabularyNormalizerTest.java`
- [X] T012 Create `VocabularyNormalizer` wrapper with cleaned-token fallback behavior in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/VocabularyNormalizer.java`
- [X] T013 Update `ExportController.ExportJobStatus` to expose `phase` and `rowCount` per contract in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/ExportController.java`

**Checkpoint**: Domain primitives, normalization, and job status contract are ready.

---

## Phase 3: User Story 1 - Produce Full Anki Export From Web (Priority: P1) MVP

**Goal**: A connected user can run a combined web export and download a complete two-column Anki-ready TSV containing word rows and standalone phrase rows.

**Independent Test**: Use mocked word/phrase pages, run a combined export through `ExportService`, and verify the output file has exactly two columns with expected word and phrase rows.

### Tests for User Story 1

- [X] T014 [P] [US1] Add typed word parsing tests for table/card HTML rows in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/DictionaryParserTest.java`
- [X] T015 [US1] Add typed phrase parsing tests preserving movie title and URL in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/DictionaryParserTest.java`
- [X] T016 [P] [US1] Add two-column TSV formatter tests for tabs/newlines/HTML escaping in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/AnkiExportFormatterTest.java`
- [X] T017 [US1] Add combined export service tests asserting output rows, phase changes, row count, and clear failed job state when dictionary page retrieval fails in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java`

### Implementation for User Story 1

- [X] T018 [US1] Replace generic `DictionaryEntry` parsing with typed `parseWords` and `parsePhrases` methods in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/DictionaryParser.java`
- [X] T019 [US1] Create `AnkiExportFormatter` that formats exactly two TSV columns in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/AnkiExportFormatter.java`
- [X] T020 [US1] Create `ExportRecordBuilder` for selected words/phrases without contextual matching yet in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/ExportRecordBuilder.java`
- [X] T021 [US1] Refactor `ExportService` to use typed parser, record builder, formatter, phases, and final row count in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ExportService.java`
- [X] T022 [US1] Update progress template to display phase and row count fields in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/progress.html`
- [X] T023 [US1] Update progress polling JavaScript to render `phase` and `rowCount` from status JSON in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/js/progress.js`

**Checkpoint**: Combined web export produces a complete two-column TSV and progress shows phase/count.

---

## Phase 4: User Story 2 - Include Contextual Phrase Examples (Priority: P2)

**Goal**: Word export rows include up to two phrase examples matched through linguistic normalization, including inflected word forms.

**Independent Test**: Use sample words and phrases where phrases contain inflected forms; verify word rows include one or two examples and never more than two.

### Tests for User Story 2

- [X] T024 [P] [US2] Add matcher unit tests for exact, inflected, and no-match phrase examples in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/DictionaryMatcherTest.java`
- [X] T025 [P] [US2] Add record builder tests for two-example cap and movie context rendering in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/ExportRecordBuilderTest.java`
- [X] T026 [US2] Add word-only export service test proving phrases enrich word rows but are not standalone rows in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java`

### Implementation for User Story 2

- [X] T027 [US2] Create `DictionaryMatcher` that indexes phrase matching tokens and returns deterministic capped examples in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/DictionaryMatcher.java`
- [X] T028 [US2] Update `ExportRecordBuilder` to include contextual examples in word back content in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/ExportRecordBuilder.java`
- [X] T029 [US2] Update `ExportService` WORDS flow to fetch phrases for examples without exporting standalone phrase rows in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ExportService.java`
- [X] T030 [US2] Update `AnkiExportFormatter` back-field rendering to preserve generated example HTML safely in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/AnkiExportFormatter.java`

**Checkpoint**: Word rows include up to two normalized contextual phrase examples.

---

## Phase 5: User Story 3 - Preserve Vocabulary Completeness (Priority: P3)

**Goal**: Exports retain unmatched items, blank translations, and merged duplicate entries with all distinct non-blank translations preserved.

**Independent Test**: Use sample pages with duplicates, conflicting translations, missing translations, unmatched words, and unmatched phrases; verify row preservation and deterministic merge behavior.

### Tests for User Story 3

- [X] T031 [P] [US3] Add deduplication tests for duplicate words and conflicting translations in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/VocabularyDeduplicatorTest.java`
- [X] T032 [US3] Add deduplication tests for duplicate phrases with metadata preservation in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/VocabularyDeduplicatorTest.java`
- [X] T033 [P] [US3] Add formatter/builder tests for blank translations and unmatched items in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/ExportRecordBuilderTest.java`
- [X] T034 [US3] Add phrase-only and empty-dictionary export service tests in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java`

### Implementation for User Story 3

- [X] T035 [US3] Create `VocabularyDeduplicator` for words and phrases with deterministic translation merging in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/VocabularyDeduplicator.java`
- [X] T036 [US3] Integrate `VocabularyDeduplicator` into `ExportService` before matching and record building in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ExportService.java`
- [X] T037 [US3] Update `ExportRecordBuilder` to keep blank-translation rows and merge multiple translations into back content in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/ExportRecordBuilder.java`
- [X] T038 [US3] Ensure `DictionaryParser` does not discard rows solely because translation is blank in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/DictionaryParser.java`

**Checkpoint**: Duplicate, blank, unmatched, phrase-only, and empty export cases preserve vocabulary completeness.

---

## Phase 6: User Story 4 - Remove Duplicate Business Logic (Priority: P4)

**Goal**: The project has one authoritative web export flow and no active duplicate standalone export business logic.

**Independent Test**: Search the codebase and verify no production code remains in obsolete top-level `cli`, `http`, `parser`, `export`, `model`, or `util` packages after equivalent web tests pass.

### Tests for User Story 4

- [X] T039 [US4] Add architecture/consolidation test asserting obsolete top-level export packages are absent from production sources in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/ExportArchitectureTest.java`

### Implementation for User Story 4

- [X] T040 [US4] Delete obsolete CLI entry point in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/cli/Main.java`
- [X] T041 [US4] Delete obsolete top-level HTTP client package files in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/http`
- [X] T042 [US4] Delete obsolete top-level parser package files in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/parser`
- [X] T043 [US4] Delete obsolete top-level export package files in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/export`
- [X] T044 [US4] Delete obsolete top-level model package files in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/model`
- [X] T045 [US4] Delete obsolete top-level util package files in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util`
- [X] T046 [US4] Delete `DictionaryEntry.java` if unused after typed export migration, or document the specific retained purpose in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/DictionaryEntry.java`
- [X] T047 [US4] Update feature quickstart/consolidation notes after duplicate removal in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/quickstart.md`

**Checkpoint**: Legacy export business logic is no longer active or duplicated outside the web flow.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, documentation cleanup, and regression safety.

- [X] T048 [P] Update `AGENTS.md` to remove stale placeholder/redundant technology lines while preserving manual additions in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/AGENTS.md`
- [X] T049 Run full test suite plus a representative-size export timing smoke check for the 10-minute success criterion, then record any remaining environment prerequisites in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/quickstart.md`
- [X] T050 [P] Add a short implementation summary to `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/003-merge-export-logic/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup completion and blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational; provides MVP combined web export.
- **User Story 2 (Phase 4)**: Depends on User Story 1 record building and typed phrase parsing.
- **User Story 3 (Phase 5)**: Depends on User Story 1 parser/formatter and integrates before matching from User Story 2 when both are present.
- **User Story 4 (Phase 6)**: Depends on User Stories 1-3 passing behavior tests.
- **Polish (Phase 7)**: Depends on all desired user stories.

### User Story Dependencies

- **US1**: MVP and prerequisite for export pipeline shape.
- **US2**: Builds on US1; independently testable through matcher and word-only export behavior.
- **US3**: Builds on US1; should be integrated before final acceptance of US2 in end-to-end export because deduplication occurs before matching.
- **US4**: Must be last because legacy code is reference material until equivalent behavior is covered.

### Within Each User Story

- Tests should be written before implementation and should fail before the corresponding implementation task.
- Domain records before parser/builder services.
- Parser/deduplicator/matcher before `ExportService` integration.
- `ExportService` integration before UI/status updates.
- Duplicate legacy packages are deleted only after new web flow behavior tests pass.

### Parallel Opportunities

- T002 and T003 can run in parallel.
- T004, T006, T007, T008, T009, T010, and T011 can run in parallel after T001.
- T014 and T016 can run in parallel; T015 writes the same parser test file as T014 and should be sequenced with it.
- T024 and T025 can run in parallel.
- T031 and T033 can run in parallel; T032 writes the same deduplicator test file as T031 and should be sequenced with it.
- T040-T045 can run in parallel after T039 passes and no references remain.
- T048 and T050 can run in parallel during polish.

---

## Parallel Example: User Story 1

```text
Task: "T014 [P] [US1] Add typed word parsing tests for table/card HTML rows in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/DictionaryParserTest.java"
Task: "T016 [P] [US1] Add two-column TSV formatter tests for tabs/newlines/HTML escaping in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/AnkiExportFormatterTest.java"
```

## Parallel Example: User Story 2

```text
Task: "T024 [P] [US2] Add matcher unit tests for exact, inflected, and no-match phrase examples in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/DictionaryMatcherTest.java"
Task: "T025 [P] [US2] Add record builder tests for two-example cap and movie context rendering in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/ExportRecordBuilderTest.java"
```

## Parallel Example: User Story 3

```text
Task: "T031 [P] [US3] Add deduplication tests for duplicate words and conflicting translations in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/VocabularyDeduplicatorTest.java"
Task: "T033 [P] [US3] Add formatter/builder tests for blank translations and unmatched items in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/export/ExportRecordBuilderTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational primitives and job phase contract.
3. Complete Phase 3: Combined web export with typed parser and two-column TSV.
4. Stop and validate: mocked combined export produces complete rows, status exposes phase, and download works.

### Incremental Delivery

1. Setup + Foundational -> shared export domain ready.
2. US1 -> complete Anki-ready combined export from web.
3. US2 -> contextual examples via linguistic matching.
4. US3 -> completeness guarantees for duplicates, blanks, unmatched items, and empty dictionaries.
5. US4 -> remove obsolete duplicate standalone business logic.
6. Polish -> full suite, documentation, and quickstart verification.

### Parallel Team Strategy

With multiple developers:

1. One developer handles Foundational domain/config work.
2. Another writes parser/formatter tests for US1.
3. After US1 base pipeline lands, matching and deduplication tests can be developed in parallel.
4. Duplicate legacy removal waits until the unified web export path is verified.

---

## Notes

- [P] tasks use different files or are safe to perform in parallel after their prerequisites.
- Story labels map to user stories in `spec.md`.
- Each user story includes an independent test criterion.
- The legacy top-level packages are intentionally retained until US4 so they can serve as reference behavior during migration.

# Tasks: Match Words With Context Examples

**Input**: Design documents from `specs/005-match-word-examples/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/review-card-import.md`, `quickstart.md`

**Tests**: Required. FR-012 explicitly requires automated checks for contextual cards, standalone examples, blank translations, and unrelated-association prevention. Write the listed tests before their implementation tasks and verify the focused test fails first.

**Organization**: Tasks are grouped by user story so each increment is independently testable after the shared export-to-review source foundation is complete.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can be completed in parallel because it changes a different file and has no incomplete dependency.
- **[Story]**: User story served by the task.

## Phase 1: Setup (Shared Test Support)

**Purpose**: Add reusable, ordered source fixtures without changing runtime behavior.

- [X] T001 [P] Add ordered word, phrase, and normalized-token fixture builders for complete-association tests in `src/test/java/com/puzzlemovies/export/export/ExportTestFixtures.java`
- [X] T002 [P] Add structured review-source fixture builders for contextual, standalone, and blank-translation scenarios in `src/test/java/com/puzzlemovies/export/review/ReviewTestFixtures.java`

---

## Phase 2: Foundational (Structured Export-to-Review Source)

**Purpose**: Create the shared transient types that retain complete association data separately from the capped TSV rendering path.

**⚠️ CRITICAL**: Complete this phase before implementing any user story.

- [X] T003 [P] Add an immutable complete-association result type that preserves per-word source-ordered candidate examples in `src/main/java/com/puzzlemovies/export/export/CompleteWordExampleMatches.java`
- [X] T004 [P] Add an immutable process-local review-source type containing selected words, examples, and complete associations in `src/main/java/com/puzzlemovies/export/review/ReviewExportSource.java`
- [X] T005 Update the constructor/validation tests for the new structured review source in `src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java`

**Checkpoint**: The transient data model is available; story work can now use structured data without parsing TSV or rendered HTML.

---

## Phase 3: User Story 1 - Study a Word in Its Matched Context (Priority: P1) 🎯 MVP

**Goal**: Generate one contextual review card per retained word/example pair, with the original word and example visible before reveal and their available translations on the back.

**Independent Test**: Given a word and one or two normalized matching examples, importing a completed export creates contextual cards whose `originalText`, `instanceText`, and translation content match the structured source; a word without a match remains word-only.

### Tests for User Story 1

- [X] T006 [P] [US1] Add failing tests for complete normalized matching and source-order preservation without applying the two-example cap in `src/test/java/com/puzzlemovies/export/export/DictionaryMatcherTest.java`
- [X] T007 [P] [US1] Add failing tests for contextual and word-only structured draft mapping, including independently preserved word and example translations, in `src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java`
- [X] T008 [P] [US1] Add a failing completed-export cache test that verifies structured review data is retained while the existing TSV `ExportRecord` list is unchanged in `src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java`
- [X] T009 [P] [US1] Add a failing MVC test that imports structured review sources and exposes a contextual card through the existing review endpoints in `src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`

### Implementation for User Story 1

- [X] T010 [US1] Add a complete-association matching operation that reuses normalized identities/tokens, preserves deduplicated phrase source order, and leaves the existing capped export matcher behavior intact in `src/main/java/com/puzzlemovies/export/export/DictionaryMatcher.java`
- [X] T011 [US1] Add structured contextual and word-only draft creation from `ReviewExportSource` without parsing rendered HTML or TSV in `src/main/java/com/puzzlemovies/export/review/ReviewCardDraftFactory.java`
- [X] T012 [US1] Build and retain `ReviewExportSource` beside the existing completed-export records while preserving two-column TSV generation in `src/main/java/com/puzzlemovies/export/service/ExportService.java`
- [X] T013 [US1] Change review-card import to retrieve the structured completed-export source and pass it to the draft factory, while preserving existing 401/404/409 behavior, in `src/main/java/com/puzzlemovies/export/web/ReviewController.java`

**Checkpoint**: A fresh completed export can create word-only or contextual review cards; contextual fronts show word plus example, and revealed backs retain both available translations.

---

## Phase 4: User Story 2 - Study an Unmatched Example Independently (Priority: P1)

**Goal**: Preserve unmatched and per-word-overflow examples as standalone review cards, including examples with no translation.

**Independent Test**: Given unmatched phrases, a third phrase for one word, and blank phrase translations, importing a completed export creates standalone example cards with only the example on the front and the available phrase translation on the back.

### Tests for User Story 2

- [X] T014 [P] [US2] Add failing draft-factory tests for unmatched examples, overflow examples, and blank example translations as standalone cards in `src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java`
- [X] T015 [P] [US2] Add failing export-service tests proving WORDS and COMBINED exports retain source examples for review without adding rows or changing the generated TSV in `src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java`
- [X] T016 [P] [US2] Add failing import-service coverage that identical standalone overflow drafts upsert to one active per-user study card on repeat import in `src/test/java/com/puzzlemovies/export/service/ReviewCardImportServiceTest.java`

### Implementation for User Story 2

- [X] T017 [US2] Extend structured draft selection to create a standalone draft for unmatched examples and for any example that overflows a matched word's two-context limit, while retaining word-only cards, in `src/main/java/com/puzzlemovies/export/review/ReviewCardDraftFactory.java`
- [X] T018 [US2] Ensure the cached review source includes all deduplicated fetched phrases required for standalone review cards, including WORDS exports, without altering record construction or TSV output in `src/main/java/com/puzzlemovies/export/service/ExportService.java`
- [X] T019 [US2] Verify the existing review-card content-key upsert remains the duplicate-prevention boundary for standalone examples without changing scheduling state in `src/main/java/com/puzzlemovies/export/service/ReviewCardImportService.java`

**Checkpoint**: No saved example is lost merely because it has no matching word, exceeds a word's contextual limit, or lacks a translation.

---

## Phase 5: User Story 3 - Preserve Correct Associations (Priority: P2)

**Goal**: Ensure only normalized-token matches create context, retain exactly the first two source-ordered examples per word, and allow examples to be contextual for multiple words.

**Independent Test**: Given exact/inflected matches, unrelated phrases, a three-example word, and one phrase matching multiple words, only valid contextual cards are created; the first two per word are contextual, overflow is standalone, and no unrelated instance text appears.

### Tests for User Story 3

- [X] T020 [P] [US3] Add failing matcher tests for unrelated-token exclusion, deterministic first-two selection input, and one phrase associated with multiple saved words in `src/test/java/com/puzzlemovies/export/export/DictionaryMatcherTest.java`
- [X] T021 [P] [US3] Add failing structured-draft tests for multi-word contextual duplication plus a standalone overflow card in `src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java`
- [X] T022 [P] [US3] Add failing MVC assertions that contextual cards expose an instance while standalone example cards expose no word instance through the unchanged queue contract in `src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`

### Implementation for User Story 3

- [X] T023 [US3] Harden complete-association indexing to reject unrelated phrases, preserve each word's source order, and retain a valid phrase association for every matched word in `src/main/java/com/puzzlemovies/export/export/DictionaryMatcher.java`
- [X] T024 [US3] Apply the two-context limit independently per word and generate contextual drafts for every eligible word/example association while idempotently emitting standalone overflow drafts in `src/main/java/com/puzzlemovies/export/review/ReviewCardDraftFactory.java`
- [X] T025 [US3] Preserve contextual-versus-standalone field semantics in review queue mapping without changing endpoint shapes, scheduling, or answer history in `src/main/java/com/puzzlemovies/export/service/ReviewService.java`

**Checkpoint**: Associations are reliable, deterministic, and independently reviewable without inventing context for unrelated words.

---

## Phase 6: Polish & Cross-Cutting Validation

**Purpose**: Confirm compatibility, usability, and complete acceptance coverage.

- [X] T026 [P] Add a 50-item mixed-source regression test covering contextual cards, standalone cards, no lost sources, and no duplicate active cards in `src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java`
- [X] T027 [P] Add review-page/static-asset assertions confirming contextual fronts render optional instances and standalone fronts omit them in `src/test/java/com/puzzlemovies/export/web/ReviewStaticAssetsTest.java`
- [X] T028 Run the full Maven suite and execute the end-to-end checks in `specs/005-match-word-examples/quickstart.md`
- [X] T029 Re-read the implementation against FR-001 through FR-016 and record any deviations before handoff in `specs/005-match-word-examples/spec.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies; T001 and T002 can run in parallel.
- **Phase 2 (Foundational)**: Depends on Phase 1; T003 and T004 can run in parallel. T005 depends on the new source model.
- **Phase 3 (US1)**: Depends on Phase 2. T006–T009 can be written in parallel; implement T010 → T011 → T012 → T013.
- **Phase 4 (US2)**: Depends on Phase 3's structured source integration. T014–T016 can be written in parallel; implement T017 → T018, then verify T019.
- **Phase 5 (US3)**: Depends on Phase 4. T020–T022 can be written in parallel; implement T023 → T024 → T025.
- **Phase 6 (Polish)**: Depends on the intended user-story phases. T026 and T027 can run in parallel; T028 and T029 close validation.

### User Story Dependencies

- **US1 (P1)**: Requires only the shared source model; this is the MVP.
- **US2 (P1)**: Builds on US1's structured completed-export handoff to preserve examples the old capped/rendered path loses.
- **US3 (P2)**: Builds on both P1 increments to validate reliability, deterministic limits, and multi-word handling.

### Parallel Opportunities

- T001/T002 and T003/T004 are independent file additions.
- Within US1, test tasks T006–T009 can be prepared in parallel.
- Within US2, test tasks T014–T016 can be prepared in parallel.
- Within US3, test tasks T020–T022 can be prepared in parallel.
- T026 and T027 are independent cross-cutting validations.

## Parallel Example: User Story 1

```text
Task: "Add complete-association matching tests in src/test/java/com/puzzlemovies/export/export/DictionaryMatcherTest.java"
Task: "Add structured contextual-draft tests in src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java"
Task: "Add completed-export source-cache tests in src/test/java/com/puzzlemovies/export/service/ExportServiceTest.java"
Task: "Add review endpoint semantic tests in src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java"
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phases 1 and 2 to make full associations and a structured review source available.
2. Complete US1 through T013.
3. Run the focused matcher, draft-factory, export-service, and review-controller tests.
4. Verify one matched word appears with one example on the review front and both translations on reveal.

### Incremental Delivery

1. Deliver US1 for correct contextual-word review cards.
2. Deliver US2 to guarantee unmatched/overflow examples and blank translations remain studyable.
3. Deliver US3 to prove deterministic, normalized, multi-word matching and negative-match safety.
4. Run the cross-cutting 50-item regression and full quickstart validation before handoff.

---
description: "Implementation tasks for Card Library Editing"
---

# Tasks: Card Library Editing

**Input**: Design documents from `/specs/006-card-library-editing/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, and
`contracts/card-library.openapi.yaml`

**Tests**: Required. The feature specification explicitly requires automated checks for
library visibility, statistics, ownership, edits from both entry points, validation,
and preservation of answer history and scheduling.

**Organization**: Tasks are grouped by user story so each delivered increment remains
independently testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other tasks after its stated dependencies because it
  changes a different file.
- **[Story]**: The user story that the task implements (`US1`, `US2`, or `US3`).
- Every task below includes its exact affected file path.

## Phase 1: Setup (Shared Test Support)

**Purpose**: Prepare reusable review-card and attempt test data without changing product
behavior.

- [X] T001 [P] Extend card and attempt builders for owned cards, suspended cards, explicit versions, and KNOWN/UNKNOWN histories in `src/test/java/com/puzzlemovies/export/review/ReviewTestFixtures.java`
- [X] T002 [P] Add reusable web DTO/card builders with editable content and version values in `src/test/java/com/puzzlemovies/export/web/ReviewWebTestFixtures.java`

---

## Phase 2: Foundational (Persistence and Import Safety)

**Purpose**: Add the shared card-edit safety model required by every story.

**⚠️ CRITICAL**: Complete this phase before implementation of any user story.

- [X] T003 Add optimistic version and manual-content-override fields, mappings, accessors, and defaults to `src/main/java/com/puzzlemovies/export/model/ReviewCard.java`
- [X] T004 Add migration coverage for existing review-card rows and new edit-safety columns in `src/test/java/com/puzzlemovies/export/repo/ReviewSchemaMigrationTest.java`
- [X] T005 Add an idempotent PostgreSQL upgrade for the new review-card columns in `src/main/resources/schema.sql`
- [X] T006 Write importer tests proving customized card content survives a later refresh while uncustomized cards still refresh in `src/test/java/com/puzzlemovies/export/service/ReviewCardImportServiceTest.java`
- [X] T007 Update import refresh behavior to preserve learner-customized original, instance, and translation fields in `src/main/java/com/puzzlemovies/export/service/ReviewCardImportService.java`

**Checkpoint**: Card rows can safely track stale edits and retain a learner correction across source refreshes.

---

## Phase 3: User Story 1 - Browse My Vocabulary Cards (Priority: P1) 🎯 MVP

**Goal**: Give a signed-in learner a paginated, readable table of every active card with
accurate answer statistics.

**Independent Test**: Create owned cards with zero, known-only, and mixed answer
histories; open `/cards` and verify their content, nullable accuracy, statistics,
pagination, and ownership isolation.

### Tests for User Story 1

- [X] T008 [P] [US1] Add PostgreSQL repository tests for owner-scoped active-card aggregation, zero-attempt rows, KNOWN/UNKNOWN counts, deterministic ordering, and page boundaries in `src/test/java/com/puzzlemovies/export/repo/ReviewCardRepositoryTest.java`
- [X] T009 [P] [US1] Add service tests for total/correct/incorrect statistics and null correct percentage when there are no attempts in `src/test/java/com/puzzlemovies/export/service/ReviewServiceTest.java`
- [X] T010 [P] [US1] Add MVC tests for `/cards` rendering, unsigned access, pagination, and cross-user isolation in `src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`
- [X] T011 [P] [US1] Add static-asset checks for the card table, unavailable-value messaging, and pagination hooks in `src/test/java/com/puzzlemovies/export/web/ReviewStaticAssetsTest.java`

### Implementation for User Story 1

- [X] T012 [US1] Add an owner-scoped aggregate projection and 25-row pageable query that excludes suspended cards in `src/main/java/com/puzzlemovies/export/repo/ReviewCardRepository.java`
- [X] T013 [US1] Add card-library DTOs and percentage calculation that derive statistics only from review attempts in `src/main/java/com/puzzlemovies/export/web/ReviewDtos.java`
- [X] T014 [US1] Add a read-only card-library service method that invokes the aggregate query and exposes a deterministic page result in `src/main/java/com/puzzlemovies/export/service/ReviewService.java`
- [X] T015 [US1] Add the authenticated `GET /cards` MVC action, including invalid-page handling and the learner-only library model, in `src/main/java/com/puzzlemovies/export/web/ReviewController.java`
- [X] T016 [US1] Create the server-rendered card table with empty values, no-history percentage state, and page navigation in `src/main/resources/templates/cards.html`
- [X] T017 [US1] Add responsive table overflow, readable long-text, empty-state, and pagination styling in `src/main/resources/static/css/app.css`
- [X] T018 [US1] Add a visible library navigation link from the review page in `src/main/resources/templates/reviews.html`

**Checkpoint**: A learner can browse all active cards and accurately inspect their study performance without editing a card.

---

## Phase 4: User Story 2 - Correct Card Content in the Library (Priority: P1)

**Goal**: Let a learner edit original text, translation, and example from a library row
while preserving history, schedule, and ownership boundaries.

**Independent Test**: Edit each permitted field from `/cards`, then verify saved values
in the table and later review view while attempts, totals, percentage, and due state are
unchanged; invalid, stale, and foreign updates must save nothing.

### Tests for User Story 2

- [X] T019 [P] [US2] Add service tests for trimmed validation, allowed optional blanks, manual-override marking, schedule/history preservation, and stale edit detection in `src/test/java/com/puzzlemovies/export/service/ReviewServiceTest.java`
- [X] T020 [P] [US2] Add MVC contract tests for `PATCH /cards/{id}` success, 400 validation, 401 unsigned, 404 foreign/missing, and 409 stale responses in `src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`
- [X] T021 [P] [US2] Add static-asset checks for table-row edit controls, editable fields, cancel action, and update feedback in `src/test/java/com/puzzlemovies/export/web/ReviewStaticAssetsTest.java`

### Implementation for User Story 2

- [X] T022 [US2] Add version-bearing editable-card and content-update request/response DTOs in `src/main/java/com/puzzlemovies/export/web/ReviewDtos.java`
- [X] T023 [US2] Implement owner-scoped card-content update validation and optimistic-lock conflict mapping without changing attempts or scheduling fields in `src/main/java/com/puzzlemovies/export/service/ReviewService.java`
- [X] T024 [US2] Add the authenticated `PATCH /cards/{id}` endpoint and 400/404/409 exception responses in `src/main/java/com/puzzlemovies/export/web/ReviewController.java`
- [X] T025 [US2] Add the library edit form or dialog with the card version token and cancel-without-request behavior in `src/main/resources/templates/cards.html`
- [X] T026 [US2] Implement library edit submission, in-place row refresh, validation display, and stale-conflict reload guidance in `src/main/resources/static/js/cards.js`
- [X] T027 [US2] Add accessible edit-dialog, inline validation, and conflict-feedback styles in `src/main/resources/static/css/app.css`

**Checkpoint**: A learner can safely correct content in the library, and no edit can alter another learner's card, history, statistics, or schedule.

---

## Phase 5: User Story 3 - Edit the Card I Am Studying (Priority: P2)

**Goal**: Let a learner correct the displayed review card without recording an answer or
changing their place in the review queue.

**Independent Test**: Open a due card, reveal its answer, edit and save it, then verify
the visible card updates in place while its ID, reveal state, reviewed count, and queue
position remain intact; cancelling makes no request.

### Tests for User Story 3

- [X] T028 [P] [US3] Add MVC and DTO assertions that a current-card edit returns the updated versioned review view without calling the answer flow in `src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`
- [X] T029 [P] [US3] Add static-asset checks for current-card edit controls, preserved reveal behavior, and no-answer edit hooks in `src/test/java/com/puzzlemovies/export/web/ReviewStaticAssetsTest.java`

### Implementation for User Story 3

- [X] T030 [US3] Include the card version in current review-card views and ensure refreshed lookup actions use saved content in `src/main/java/com/puzzlemovies/export/service/ReviewService.java`
- [X] T031 [US3] Add the current-card edit controls and version token to the initial review markup in `src/main/resources/templates/reviews.html`
- [X] T032 [US3] Extend dynamically rendered review-card markup with the same edit controls in `src/main/resources/static/js/reviews.js`
- [X] T033 [US3] Implement current-card edit open, cancel, PATCH save, in-place content/version refresh, and stale-conflict feedback without invoking answer or next-card endpoints in `src/main/resources/static/js/reviews.js`
- [X] T034 [US3] Add responsive current-card editor and reduced-motion-safe status styling in `src/main/resources/static/css/app.css`

**Checkpoint**: Current-card corrections are visible immediately and ordinary review actions still create exactly one answer and advance the queue normally.

---

## Phase 6: Polish & Cross-Cutting Validation

**Purpose**: Validate the finished feature against its contract, accessibility needs, and
end-to-end behavior.

- [X] T035 [P] Reconcile implemented paths, request/response fields, and error status behavior with `specs/006-card-library-editing/contracts/card-library.openapi.yaml`
- [X] T036 [P] Verify desktop/mobile table readability, keyboard-accessible editing, and missing-value wording against `specs/006-card-library-editing/quickstart.md`
- [X] T037 Run the full automated suite and complete the manual card-library and in-review editing scenarios in `pom.xml` and `specs/006-card-library-editing/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1** has no implementation dependency and prepares reusable test support.
- **Phase 2** depends on Phase 1 and blocks all stories because editing needs durable
  versioning and import protection.
- **US1** depends on Phase 2 and is the MVP browse-only increment.
- **US2** depends on Phase 2. It reuses the card model but can be implemented after or
  alongside US1; it integrates with the US1 table for its library entry point.
- **US3** depends on Phase 2 and the shared US2 update contract. It must follow US2.
- **Phase 6** depends on every desired story being complete.

### User Story Completion Order

`Setup → Foundational → US1 (browse) → US2 (library edit) → US3 (in-review edit) → Polish`

### Within Each User Story

- Complete the listed tests before their implementation tasks and confirm they fail for
  the intended missing behavior.
- Complete repository/model-facing work before service work, then controller/UI work.
- At each checkpoint, run the story's focused tests before beginning the next phase.

## Parallel Opportunities

- T001 and T002 can run in parallel.
- After T003, T004 and T006 can be prepared in parallel; T005 and T007 then verify and
  implement the shared migration/import behavior.
- T008–T011 can run in parallel after the Phase 2 model work is available.
- T019–T021 can run in parallel after the US1 library route and table exist.
- T028 and T029 can run in parallel after the shared update contract from US2 exists.
- T035 and T036 can run in parallel after all story checkpoints.

## Parallel Example: User Story 1

```text
Task: "Add repository aggregation coverage in src/test/java/com/puzzlemovies/export/repo/ReviewCardRepositoryTest.java"
Task: "Add service statistics coverage in src/test/java/com/puzzlemovies/export/service/ReviewServiceTest.java"
Task: "Add library MVC coverage in src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java"
Task: "Add library static-asset coverage in src/test/java/com/puzzlemovies/export/web/ReviewStaticAssetsTest.java"
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete the shared version/import foundation.
2. Implement and validate the browse-only card library through T018.
3. Demonstrate that the learner can inspect every active card and accurate statistics.

### Incremental Delivery

1. Add safe persistence prerequisites.
2. Deliver the table and statistics (US1).
3. Add safe library corrections (US2).
4. Reuse the same update contract for in-review corrections (US3).
5. Run the quickstart scenarios and full test suite.

## Notes

- `[P]` marks tasks that can safely proceed in parallel once their stated dependencies
  are complete.
- User-story labels provide requirement traceability.
- Preserve the existing two-column export and append-only review attempts throughout.

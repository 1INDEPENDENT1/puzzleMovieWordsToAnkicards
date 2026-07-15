# Tasks: Custom Anki-Style Review Cards

**Input**: Design documents from `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/004-custom-anki-review/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Required by FR-019. Unit, service, repository, and web tests must be added before implementation tasks for each story where practical.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Repository root: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards`
- Main Java: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java`
- Test Java: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java`
- Static resources: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static`
- Templates: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates`
- Feature docs: `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/004-custom-anki-review`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish shared review test fixtures and confirm the project remains on the existing Spring Boot stack with no new runtime dependencies.

- [X] T001 Verify no new runtime dependencies are required and existing Spring Boot/JPA/Testcontainers dependencies are present in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/pom.xml`
- [X] T002 [P] Create reusable review-card, review-attempt, and user fixtures in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/review/ReviewTestFixtures.java`
- [X] T003 [P] Create reusable review web/session fixtures in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/web/ReviewWebTestFixtures.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persistent review entities, repositories, and shared DTOs that every user story depends on.

**CRITICAL**: No user story work should begin until this phase is complete.

### Tests for Foundational Layer

- [X] T004 [P] Add repository tests for due-card ordering, active-card duplicate key constraints, and owner scoping in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/repo/ReviewCardRepositoryTest.java`
- [X] T005 [P] Add repository tests for append-only review attempt ownership and ordering in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/repo/ReviewAttemptRepositoryTest.java`

### Implementation for Foundational Layer

- [X] T006 [P] Create `ReviewCardState` enum with NEW, LEARNING, REVIEW, RELEARNING, SUSPENDED in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ReviewCardState.java`
- [X] T007 [P] Create `ReviewAnswer` enum with KNOWN and UNKNOWN in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ReviewAnswer.java`
- [X] T008 Create `ReviewCard` JPA entity with scheduling fields, timestamps, user relationship, and unique user/contentKey constraint in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ReviewCard.java`
- [X] T009 Create `ReviewAttempt` JPA entity linked to user and review card with previous/next scheduling fields in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ReviewAttempt.java`
- [X] T010 [P] Create `ReviewCardRepository` with owner-scoped lookup, duplicate-key lookup, due-card query, due-count query, and next-due query in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/repo/ReviewCardRepository.java`
- [X] T011 [P] Create `ReviewAttemptRepository` with card/user-scoped attempt history queries in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/repo/ReviewAttemptRepository.java`
- [X] T012 [P] Create review response records for card view, lookup action, counts, queue response, answer request, scheduling result, and answer response in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/ReviewDtos.java`

**Checkpoint**: Review persistence and response shapes are ready for user-story implementation.

---

## Phase 3: User Story 1 - Study Generated Vocabulary In App (Priority: P1) MVP

**Goal**: A learner can create app-owned review cards from generated vocabulary, open `/reviews`, reveal a card, answer it with one of two buttons, and see the next due card or an empty state.

**Independent Test**: Create sample generated records, refresh review cards, open a review session, verify the card front/back data appears, submit an answer, and confirm the response contains updated counts and the next card.

### Tests for User Story 1

- [X] T013 [P] [US1] Add review-card draft mapping tests for word and phrase `ExportRecord` values in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java`
- [X] T014 [P] [US1] Add review-card import service tests for contentKey generation, duplicate prevention, and blank translation preservation in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ReviewCardImportServiceTest.java`
- [X] T015 [US1] Add review service tests for due queue loading, empty state counts, next-card selection, and render-time lookup action generation without persisted URLs in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ReviewServiceTest.java`
- [X] T016 [US1] Add Spring MVC tests for GET `/reviews`, POST `/reviews/cards`, GET `/reviews/next`, and POST `/reviews/cards/{id}/answer` happy paths in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`

### Implementation for User Story 1

- [X] T017 [US1] Create `ReviewCardDraft` record for original text, instance text, translation text, source context, and record kind in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/review/ReviewCardDraft.java`
- [X] T018 [US1] Create `ReviewCardDraftFactory` to map application-generated `ExportRecord` values into review-card drafts without parsing arbitrary Anki templates in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/review/ReviewCardDraftFactory.java`
- [X] T019 [US1] Refactor `ExportService` to expose the structured generated `ExportRecord` values needed for review-card creation without parsing TSV/Anki templates, adding separate persistence for raw generated rows, or changing TSV download behavior in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ExportService.java`
- [X] T020 [US1] Create `ReviewCardImportService` to upsert active review cards for a user from generated review-card drafts in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ReviewCardImportService.java`
- [X] T021 [US1] Create `ReviewService` to load due cards, build card views, track session counts, and answer a card through the scheduler facade in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ReviewService.java`
- [X] T022 [US1] Create `ReviewController` for `/reviews`, `/reviews/cards`, `/reviews/next`, and `/reviews/cards/{id}/answer` with signed-in user checks in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/ReviewController.java`
- [X] T023 [US1] Create review page template with card front, hidden back, reveal control, two answer buttons, progress counts, and empty state in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/reviews.html`
- [X] T024 [US1] Add Review navigation/action links to the export menu in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/menu.html`
- [X] T025 [US1] Add completed-export review-card creation action to `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/progress.html`
- [X] T026 [US1] Create review JavaScript for reveal, answer POST, response rendering, and next-card replacement in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/js/reviews.js`

**Checkpoint**: MVP review flow works from generated app data with front/back reveal and two answer buttons.

---

## Phase 4: User Story 2 - Schedule Reviews With Two Buttons (Priority: P1)

**Goal**: The two answer choices update scheduling state in an explainable Anki-like way, record immutable review attempts, and make future due-card selection reflect prior history.

**Independent Test**: Answer equivalent cards with KNOWN and UNKNOWN across new, learning, review, and relearning states; verify due times, intervals, state transitions, lapse counts, and attempt history.

### Tests for User Story 2

- [X] T027 [P] [US2] Add binary scheduler unit tests for NEW, LEARNING, REVIEW, and RELEARNING transitions, including UNKNOWN retry due in 10 minutes and KNOWN graduation/re-graduation to a 1 day interval due tomorrow in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/review/BinaryReviewSchedulerTest.java`
- [X] T028 [P] [US2] Add scheduler tests for repeated KNOWN interval growth, minimum one-day increase for mature review cards, UNKNOWN lapse counting, ease/difficulty reduction, and repeated short retry behavior in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/review/BinaryReviewSchedulerTest.java`
- [X] T029 [US2] Add review service tests proving attempts are saved with previous/next state and counts update after answers in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ReviewServiceTest.java`
- [X] T030 [US2] Add Spring MVC tests for invalid answer, unauthorized answer, wrong-owner answer, and stale/not-due answer responses in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`

### Implementation for User Story 2

- [X] T031 [US2] Create `ReviewScheduleResult` record describing previous state, next state, next due time, interval, ease, and lapse changes in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/review/ReviewScheduleResult.java`
- [X] T032 [US2] Create `BinaryReviewScheduler` implementing the configured KNOWN/UNKNOWN transitions from research: 10 minute learning/relearning retry, tomorrow/1 day graduation, review interval growth by ease/difficulty multiplier, minimum one-day mature-card increase, lapse recording, and ease bounds in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/review/BinaryReviewScheduler.java`
- [X] T033 [US2] Update `ReviewService` to apply `BinaryReviewScheduler`, persist `ReviewAttempt`, mutate `ReviewCard`, and return scheduling results in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ReviewService.java`
- [X] T034 [US2] Update `ReviewCardRepository` due-card queries to exclude suspended and future cards while ordering due cards deterministically in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/repo/ReviewCardRepository.java`
- [X] T035 [US2] Update `ReviewController` answer handling to return 400, 401, 404, and 409 states per contract in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/ReviewController.java`

**Checkpoint**: Two-button scheduling is adaptive, persisted, and visible through review responses.

---

## Phase 5: User Story 3 - Preserve Useful Study Context (Priority: P2)

**Goal**: Review cards preserve words, phrases, examples, blank translations, and source context in a way that remains readable on the card front/back.

**Independent Test**: Generate cards from words with phrase examples, standalone phrases, blank translations, repeated original text in different instances, and long text; verify stored fields and rendered views preserve the intended context.

### Tests for User Story 3

- [X] T036 [P] [US3] Add card draft tests for extracting phrase examples and translations from generated word back content in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java`
- [X] T037 [P] [US3] Add import service tests proving same original text with different instances creates separate cards while exact duplicates upsert in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ReviewCardImportServiceTest.java`
- [X] T038 [US3] Add review view tests for blank translation, long instance text, phrase-only cards, source context rendering, and available lookup actions for original text/pronunciation/instance sentence in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`

### Implementation for User Story 3

- [X] T039 [US3] Enhance `ReviewCardDraftFactory` to preserve contextual examples, standalone phrase cards, blank translations, and source context from generated records in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/review/ReviewCardDraftFactory.java`
- [X] T040 [US3] Update `ReviewCardImportService` contentKey normalization to include original text, instance text, translation/back content, source context, and record kind in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ReviewCardImportService.java`
- [X] T041 [US3] Update `ReviewService` card-view mapping to generate lookup actions at render time for original dictionary/translation, original pronunciation, and instance full-sentence translation when fields are present; omit unavailable actions, avoid persisting full URLs, and preserve blank translation state explicitly in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ReviewService.java`
- [X] T042 [US3] Update `reviews.html` to render original text, instance text, translation/back content, source context, and lookup actions without overlapping controls in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/reviews.html`

**Checkpoint**: Review cards retain useful learning context rather than collapsing to isolated words.

---

## Phase 6: User Story 4 - Make Review Feel Polished And Fast (Priority: P3)

**Goal**: The review screen feels responsive, gives clear KNOWN/UNKNOWN transition feedback, supports reduced motion, and remains usable across common desktop and mobile sizes.

**Independent Test**: Complete a multi-card review session on desktop and mobile-sized screens, observe distinct answer transitions, verify reduced-motion behavior, and confirm no reloads or lost answers occur.

### Tests for User Story 4

- [X] T043 [P] [US4] Add JavaScript static smoke test or documented DOM fixture test for reveal and answer-rendering behavior in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/web/ReviewStaticAssetsTest.java`
- [X] T044 [US4] Add MVC regression test ensuring the review template includes reduced-motion-friendly hooks, progress elements, and answer state classes in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java`

### Implementation for User Story 4

- [X] T045 [US4] Add responsive review card layout, answer button states, lookup action styling, and progress styling in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/css/app.css`
- [X] T046 [US4] Add KNOWN and UNKNOWN sweep transition classes plus `prefers-reduced-motion` fallback styles in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/css/app.css`
- [X] T047 [US4] Update `reviews.js` to apply distinct success/difficulty transitions, avoid double-submit during animation, and recover cleanly on failed answer requests in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/js/reviews.js`
- [X] T048 [US4] Update `reviews.html` button labels, aria-live status, disabled states, and progress text for keyboard and screen-reader use in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/reviews.html`

**Checkpoint**: Review sessions feel polished, responsive, and accessible without introducing a frontend framework.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final contract alignment, documentation, and regression verification.

- [X] T049 [P] Update `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/004-custom-anki-review/contracts/openapi.yaml` if implementation response fields differ from the planned review DTO names
- [X] T050 [P] Update review quickstart with final run, card creation, scheduling smoke check, and reduced-motion verification notes in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/004-custom-anki-review/quickstart.md`
- [X] T051 [P] Update `AGENTS.md` with implemented 004 review-card details while preserving manual additions in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/AGENTS.md`
- [X] T052 Run `mvn test` and fix any regressions in `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java`
- [ ] T053 Run `mvn spring-boot:run` and complete the quickstart review flow manually from `C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/004-custom-anki-review/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup completion and blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational; provides MVP in-app review.
- **User Story 2 (Phase 4)**: Depends on US1 service/controller shape and completes the scheduler behavior.
- **User Story 3 (Phase 5)**: Depends on US1 card creation; can proceed after the MVP import path exists.
- **User Story 4 (Phase 6)**: Depends on US1 UI/JS and benefits from US2 answer responses.
- **Polish (Phase 7)**: Depends on all desired user stories.

### User Story Dependencies

- **US1**: MVP and required for visible review sessions.
- **US2**: Builds on US1 answer flow; required for trustworthy long-term review.
- **US3**: Builds on US1 card import; independently testable through stored fields and rendered card views.
- **US4**: Builds on US1 UI and US2 answer feedback; independently testable through UI/static behavior and manual quickstart checks.

### Within Each User Story

- Tests should be written before implementation and should fail before the corresponding implementation task.
- Entities and repositories before services.
- Services before controllers.
- Controllers before templates and static JavaScript integration.
- CSS/animation polish after stable DOM structure exists.

### Parallel Opportunities

- T002 and T003 can run in parallel.
- T004 and T005 can run in parallel.
- T006, T007, T010, T011, and T012 can run in parallel after test fixtures exist.
- T013 and T014 can run in parallel; T015 and T016 share service/controller files and should be sequenced with related implementation.
- T027 and T028 write the same scheduler test file and should be sequenced, but can be planned independently from T030.
- T036 and T037 can run in parallel.
- T045 and T047 touch CSS and JS separately and can run in parallel after T023 and T026.
- T049, T050, and T051 can run in parallel during polish.

---

## Parallel Example: User Story 1

```text
Task: "T013 [P] [US1] Add review-card draft mapping tests for word and phrase ExportRecord values in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java"
Task: "T014 [P] [US1] Add review-card import service tests for contentKey generation, duplicate prevention, and blank translation preservation in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ReviewCardImportServiceTest.java"
```

## Parallel Example: User Story 2

```text
Task: "T027 [P] [US2] Add binary scheduler unit tests for NEW, LEARNING, REVIEW, and RELEARNING transitions, including UNKNOWN retry due in 10 minutes and KNOWN graduation/re-graduation to a 1 day interval due tomorrow in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/review/BinaryReviewSchedulerTest.java"
Task: "T030 [US2] Add Spring MVC tests for invalid answer, unauthorized answer, wrong-owner answer, and stale/not-due answer responses in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/web/ReviewControllerTest.java"
```

## Parallel Example: User Story 3

```text
Task: "T036 [P] [US3] Add card draft tests for extracting phrase examples and translations from generated word back content in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/review/ReviewCardDraftFactoryTest.java"
Task: "T037 [P] [US3] Add import service tests proving same original text with different instances creates separate cards while exact duplicates upsert in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/test/java/com/puzzlemovies/export/service/ReviewCardImportServiceTest.java"
```

## Parallel Example: User Story 4

```text
Task: "T045 [US4] Add responsive review card layout, answer button states, lookup action styling, and progress styling in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/css/app.css"
Task: "T047 [US4] Update reviews.js to apply distinct success/difficulty transitions, avoid double-submit during animation, and recover cleanly on failed answer requests in C:/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/js/reviews.js"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational persistence and DTOs.
3. Complete Phase 3: Review-card creation, `/reviews` page, reveal/answer flow, and next-card JSON.
4. Stop and validate: sample generated cards can be studied in app with no arbitrary Anki template parsing.

### Incremental Delivery

1. Setup + Foundational -> review persistence and DTOs ready.
2. US1 -> complete visible in-app review MVP.
3. US2 -> adaptive binary scheduling and durable attempt history.
4. US3 -> richer context preservation for examples, phrases, blanks, and duplicates.
5. US4 -> polished responsive animation and accessibility.
6. Polish -> contract alignment, docs, full tests, and manual quickstart.

### Parallel Team Strategy

With multiple developers:

1. One developer handles Foundational model/repository work.
2. Another writes US1 import/service/controller tests.
3. After the US1 service shape lands, one developer can implement scheduling while another improves context extraction.
4. UI animation polish waits until the review template and answer response contract stabilize.

---

## Notes

- [P] tasks use different files or are safe to perform in parallel after their prerequisites.
- Story labels map to user stories in `spec.md`.
- Each user story includes an independent test criterion.
- Review cards are created from application-owned generated records, not arbitrary Anki card templates.


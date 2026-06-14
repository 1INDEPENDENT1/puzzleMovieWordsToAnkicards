---

description: "Task list for PuzzleMovies Export Web App implementation"
---

# Tasks: PuzzleMovies Export Web App

**Input**: Design documents from `/specs/002-puzzlemovies-export-webapp/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not requested in the feature specification; no test tasks included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create Spring Boot Maven project dependencies and plugins in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/pom.xml`
- [x] T002 [P] Create application entrypoint in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/ExportWebAppApplication.java`
- [x] T003 [P] Configure base Spring Boot properties in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/application.yml`
- [x] T004 [P] Add shared layout template in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/layout.html`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

- [x] T005 Create export configuration properties in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/config/ExportProperties.java`
- [x] T006 [P] Configure async task executor in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/config/AsyncConfig.java`
- [x] T007 [P] Provide shared HttpClient bean in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/config/HttpClientConfig.java`
- [x] T008 [P] Add global error handler in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/GlobalExceptionHandler.java`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Connect PuzzleMovies Account (Priority: P1) 🎯 MVP

**Goal**: Let users log in with puzzle-movies.com credentials and store a reusable session token.

**Independent Test**: Submit valid/invalid credentials on `/login`; on success, session token is stored and user is redirected to `/exports/menu` without storing a password.

### Implementation for User Story 1

- [x] T009 [P] [US1] Create User entity in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/User.java`
- [x] T010 [P] [US1] Create PuzzleSessionToken entity in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/PuzzleSessionToken.java`
- [x] T011 [P] [US1] Create UserRepository in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/repo/UserRepository.java`
- [x] T012 [P] [US1] Create PuzzleSessionTokenRepository in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/repo/PuzzleSessionTokenRepository.java`
- [x] T013 [P] [US1] Implement PuzzleMoviesAuthClient in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/puzzlemovies/PuzzleMoviesAuthClient.java`
- [x] T014 [US1] Implement AuthService (authenticate + store session token) in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/AuthService.java`
- [x] T015 [US1] Add session binding helper in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/SessionUserResolver.java`
- [x] T016 [US1] Implement LoginController in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/LoginController.java`
- [x] T017 [US1] Add login page template in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/login.html`
- [x] T018 [US1] Add export menu controller (render-only stub) in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/ExportMenuController.java`
- [x] T019 [US1] Add minimal export menu template in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/menu.html`

**Checkpoint**: User Story 1 should be functional and testable independently

---

## Phase 4: User Story 2 - Start and Track an Export (Priority: P2)

**Goal**: Allow users to start an export job and view progress updates.

**Independent Test**: From `/exports/menu`, start an export and see `/exports/{id}` show status and progress that updates on refresh.

### Implementation for User Story 2

- [x] T020 [P] [US2] Create ExportJob entity in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ExportJob.java`
- [x] T021 [P] [US2] Create ExportType enum in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ExportType.java`
- [x] T022 [P] [US2] Create ExportStatus enum in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/ExportStatus.java`
- [x] T023 [P] [US2] Create ExportJobRepository in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/repo/ExportJobRepository.java`
- [x] T024 [P] [US2] Implement PuzzleMoviesDictionaryClient in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/puzzlemovies/PuzzleMoviesDictionaryClient.java`
- [x] T025 [P] [US2] Implement DictionaryParser in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/export/DictionaryParser.java`
- [x] T026 [US2] Implement ExportService with async job runner in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/service/ExportService.java`
- [x] T027 [US2] Implement ExportController for start/progress/status in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/ExportController.java`
- [x] T028 [US2] Update export menu form to start jobs in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/menu.html`
- [x] T029 [US2] Add progress page with polling in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/progress.html`
- [x] T030 [US2] Add polling script in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/js/progress.js`

**Checkpoint**: User Stories 1 and 2 should be functional and independently testable

---

## Phase 5: User Story 3 - Download Export File (Priority: P3)

**Goal**: Allow users to download a completed TSV export.

**Independent Test**: After an export completes, clicking download returns a TSV file; attempting before completion shows a clear message.

### Implementation for User Story 3

- [x] T031 [US3] Implement download controller in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/web/ExportDownloadController.java`
- [x] T032 [US3] Show download link when ready in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/progress.html`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T033 [P] Add export-not-ready template for 409 responses in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/templates/export-not-ready.html`
- [x] T034 [P] Add basic styling in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/resources/static/css/app.css`
- [x] T035 Update quickstart environment notes in `/mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/002-puzzlemovies-export-webapp/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Integrates with US1 session
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Depends on export job completion from US2

### Parallel Opportunities

- Setup: T002, T003, T004 can run in parallel
- Foundational: T006, T007, T008 can run in parallel
- US1: T009, T010, T011, T012, T013 can run in parallel
- US2: T020, T021, T022, T023, T024, T025 can run in parallel
- Polish: T033, T034 can run in parallel

---

## Parallel Example: User Story 1

```bash
Task: "Create User entity in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/User.java"
Task: "Create PuzzleSessionToken entity in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/model/PuzzleSessionToken.java"
Task: "Create UserRepository in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/repo/UserRepository.java"
Task: "Create PuzzleSessionTokenRepository in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/repo/PuzzleSessionTokenRepository.java"
Task: "Implement PuzzleMoviesAuthClient in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/com/puzzlemovies/export/puzzlemovies/PuzzleMoviesAuthClient.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Validate independently
3. Add User Story 2 → Validate independently
4. Add User Story 3 → Validate independently
5. Add Polish tasks as needed

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Avoid vague tasks and cross-story coupling that breaks independence

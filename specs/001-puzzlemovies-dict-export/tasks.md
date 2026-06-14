# Tasks: Puzzle-Movies Dictionary Export

**Input**: Design documents from `/specs/001-puzzlemovies-dict-export/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: 
- Parser tests (unit):
-  - WordParser parses 1 sample word row
-  - PhraseParser parses 1 sample phrase row
-  ExportFormatter test:
- - no tabs/newlines break TSV.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/java/`, `src/test/java/` at repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create Maven project and basic metadata in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/pom.xml
- [X] T002 [P] Add dependency declarations (Jsoup, lingua-core) in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/pom.xml
- [X] T003 [P] Create source package structure under /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Create CLI option model in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/model/CliOptions.java
- [X] T005 Create token file persistence utility in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/TokenStore.java
- [X] T006 Create HTTP client provider and request helpers in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/http/HttpClientProvider.java
- [X] T007 Implement authentication flow (guest cookies + sign-in) in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/http/AuthService.java
- [X] T008 Implement dictionary page fetcher with pagination in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/http/DictionaryClient.java
- [X] T009 Create error types and fail-fast handler in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/AppException.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Export vocabulary to Anki file (Priority: P1) 🎯 MVP

**Goal**: Export dictionary words and phrases into a single Anki-importable TSV/CSV file.

**Independent Test**: Run the CLI with a valid token and confirm a combined export file is created.

### Implementation for User Story 1

- [X] T010 [P] [US1] Create word and phrase models in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/model/DictionaryWord.java
- [X] T011 [P] [US1] Create phrase model in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/model/DictionaryPhrase.java
- [X] T012 [P] [US1] Create export record model in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/model/ExportRecord.java
- [X] T013 [P] [US1] Implement word HTML parser in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/parser/WordParser.java
- [X] T014 [P] [US1] Implement phrase HTML parser in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/parser/PhraseParser.java
- [X] T015 [US1] Implement export formatter (TSV/CSV) in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/export/ExportFormatter.java
- [X] T016 [US1] Implement export writer (single combined file) in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/export/ExportWriter.java
- [X] T017 [US1] Wire CLI entry point for export flow in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/cli/Main.java

**Checkpoint**: User Story 1 export file generation works end-to-end

---

## Phase 4: User Story 2 - Match words to contextual examples (Priority: P2)

**Goal**: Match words to phrases and include up to two contextual examples per word.

**Independent Test**: Export with overlapping words/phrases and verify examples appear on word entries.

### Implementation for User Story 2

- [X] T018 [P] [US2] Implement phrase token normalization utilities in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/TextNormalizer.java
- [X] T019 [P] [US2] Implement word index builder in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/WordIndex.java
- [X] T020 [US2] Implement matching logic with two-example cap in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/Matcher.java
- [X] T021 [US2] Update export formatting to include examples in back HTML in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/export/ExportFormatter.java

**Checkpoint**: Word entries include up to two contextual examples when matches exist

---

## Phase 5: User Story 3 - Export without context when needed (Priority: P3)

**Goal**: Ensure unmatched items and missing translations are still exported correctly.

**Independent Test**: Export with items lacking matches/translations and confirm they still appear.

### Implementation for User Story 3

- [X] T022 [US3] Implement de-duplication rules for words/phrases in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/Deduplicator.java
- [X] T023 [US3] Ensure phrases are always included as standalone records in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/export/ExportWriter.java
- [X] T024 [US3] Ensure missing translations are exported as blank fields in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/export/ExportFormatter.java

**Checkpoint**: Unmatched and missing-translation items are preserved in the export

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T025 [P] Update usage docs in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/specs/001-puzzlemovies-dict-export/quickstart.md
- [X] T026 Add CLI help/usage output in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/cli/Main.java
- [X] T027 Add consistent error messages for auth/fetch/parse failures in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/AppException.java

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Builds on US1 export flow
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Builds on US1 export flow

### Parallel Opportunities

- Phase 1: T002, T003
- Phase 3: T010, T011, T012, T013, T014
- Phase 4: T018, T019
- Phase 6: T025, T026 (different files)

---

## Parallel Example: User Story 2

```bash
Task: "Implement phrase token normalization utilities in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/TextNormalizer.java"
Task: "Implement word index builder in /mnt/c/Users/George/javaProjects/puzzleMovieWordsToAnkicards/src/main/java/util/WordIndex.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Verify a combined export file is created

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Validate export file
3. Add User Story 2 → Validate examples in back HTML
4. Add User Story 3 → Validate unmatched/missing translation handling
5. Finish Polish tasks

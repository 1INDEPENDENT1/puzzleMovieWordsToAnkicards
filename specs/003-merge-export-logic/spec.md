# Feature Specification: Merge Export Logic Into Web App

**Feature Branch**: `003-merge-export-logic`  
**Created**: 2026-06-12  
**Status**: Draft  
**Input**: User description: "Create a clean Spec Kit task from the discussion: the project has duplicate legacy export business logic outside the web application. The web app must become the single primary interface while preserving the richer export behavior from the legacy exporter, including typed word/phrase parsing, duplicate handling, contextual examples, blank translation preservation, downloadable Anki-ready output, and linguistic normalization through the existing local lemmatization capability."

## Clarifications

### Session 2026-06-13

- Q: How should improved logic behave for WORDS, PHRASES, and COMBINED export types? -> A: WORDS exports word rows and may use saved phrases only as contextual examples. PHRASES exports only phrase rows. COMBINED exports word rows with examples plus standalone phrase rows.
- Q: How should duplicate entries with conflicting translations be handled? -> A: Conflicting translations should be merged into one combined translation field.
- Q: What downloadable row shape should the improved export use? -> A: Use two columns: front text and back content; keep type distinction implicit in formatting/content.
- Q: What should happen when linguistic normalization cannot process a token? -> A: Continue export and fall back to cleaned original tokens for matching and deduplication.
- Q: What progress details should users see for the improved export? -> A: Show export phases and final row counts in addition to percent/status.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Produce Full Anki Export From Web (Priority: P1)

As a user who has connected a PuzzleMovies account, I can start a combined export from the web interface and receive an Anki-ready file containing both words and phrases, so I can study my vocabulary without using a separate command-line workflow.

**Why this priority**: This restores the core product value in the primary interface and removes the need for duplicate export paths.

**Independent Test**: Can be tested by using a sample account dictionary with words and phrases, starting a combined export, and verifying that the downloaded file contains the expected complete vocabulary rows.

**Acceptance Scenarios**:

1. **Given** a connected account with saved words and phrases, **When** the user starts a combined export, **Then** the completed download includes word rows with contextual examples plus standalone phrase rows.
2. **Given** the export completes, **When** the user downloads the file, **Then** it can be imported as an Anki-compatible two-column tabular file without manual cleanup.
3. **Given** the user starts the export from the web interface, **When** the export is running, **Then** the user can track progress without using or seeing any command-line workflow.
4. **Given** the improved export is running, **When** the user views progress, **Then** the user sees the current export phase in addition to percent and status.

---

### User Story 2 - Include Contextual Phrase Examples (Priority: P2)

As a learner, I want exported word cards to include relevant phrase examples from my saved phrases, so I can learn words in movie context rather than as isolated translations.

**Why this priority**: Contextual examples are the main quality improvement from the legacy export behavior and directly improve study value.

**Independent Test**: Can be tested with sample data where a saved word appears in multiple saved phrases, then verifying that the word row includes no more than two matching examples.

**Acceptance Scenarios**:

1. **Given** a saved word appears in one saved phrase, **When** the export is generated, **Then** the word row includes that phrase as a contextual example.
2. **Given** a saved word appears in more than two saved phrases, **When** the export is generated, **Then** the word row includes at most two contextual examples.
3. **Given** a saved phrase uses an inflected form of a saved word, **When** the export is generated, **Then** the phrase can still match the word through linguistic normalization.

---

### User Story 3 - Preserve Vocabulary Completeness (Priority: P3)

As a user, I want the export to preserve all useful vocabulary entries even when data is incomplete or duplicated, so I do not lose study material during export.

**Why this priority**: Users trust export as a backup and study source; silent loss of rows or translations would undermine the feature.

**Independent Test**: Can be tested with sample dictionary pages containing duplicate entries, missing translations, unmatched words, and unmatched phrases.

**Acceptance Scenarios**:

1. **Given** duplicate words or phrases exist in the source dictionary, **When** the export is generated, **Then** duplicates are merged into a single export entry per normalized vocabulary item.
2. **Given** a word or phrase has no translation, **When** the export is generated, **Then** the item is still exported with a blank translation field.
3. **Given** a phrase does not match any exported word, **When** the export is generated, **Then** the phrase still appears as a standalone row.
4. **Given** a word has no matching phrase example, **When** the export is generated, **Then** the word still appears without contextual examples.

---

### User Story 4 - Remove Duplicate Business Logic (Priority: P4)

As a maintainer, I want the project to have one authoritative export flow, so future fixes and behavior changes do not have to be made in multiple disconnected places.

**Why this priority**: Consolidation reduces maintenance risk after the user-facing behavior is preserved.

**Independent Test**: Can be tested by inspecting the codebase after implementation and confirming that export behavior is reachable from one primary web flow and no standalone duplicate export path remains active.

**Acceptance Scenarios**:

1. **Given** the new web export flow passes the behavior tests, **When** duplicate legacy export code is reviewed, **Then** obsolete standalone export files are removed or made clearly non-authoritative.
2. **Given** a future maintainer searches for export parsing, matching, or formatting behavior, **When** they inspect the project, **Then** there is one clear location for each responsibility.

### Edge Cases

- Source pages contain empty dictionaries for words, phrases, or both.
- Source pages contain duplicate vocabulary with conflicting translations that must be preserved in a single merged entry.
- Source pages contain words or phrases with punctuation, mixed case, HTML markup, or both English and Russian text.
- Source pages contain phrase metadata such as movie title or movie URL for some rows but not others.
- Source service returns a signed-out page or an expired-session response during export.
- Export contains many pages of vocabulary and must remain trackable until completion.
- Linguistic normalization cannot process a token cleanly; export should continue using cleaned original tokens for affected matching and deduplication.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a single primary web export flow for words, phrases, and combined vocabulary exports.
- **FR-002**: The system MUST preserve the existing connected-account and export-progress experience while improving the generated export content.
- **FR-003**: The system MUST parse word entries and phrase entries as distinct vocabulary types.
- **FR-004**: The system MUST preserve phrase context metadata when source data provides it.
- **FR-005**: The system MUST include all selected words in word and combined exports even when no contextual examples are found.
- **FR-006**: The system MUST include selected phrases as standalone rows in phrase and combined exports.
- **FR-007**: The system MUST match words to phrases using normalized vocabulary forms so inflected word forms can match their base vocabulary item.
- **FR-008**: The system MUST include no more than two contextual phrase examples per exported word.
- **FR-009**: The system MUST merge duplicate words and duplicate phrases into single export entries.
- **FR-010**: The system MUST retain entries with missing translations and leave their translation fields blank.
- **FR-011**: The system MUST generate a downloadable Anki-compatible two-column tabular file with front text and back content, without tabs or line breaks corrupting row boundaries.
- **FR-012**: The system MUST report export failure clearly when the source service session is expired or vocabulary pages cannot be retrieved.
- **FR-013**: The system MUST keep obsolete standalone export behavior from remaining as a second active business-logic path after the web flow is complete.
- **FR-014**: The system MUST provide automated checks for parsing, duplicate handling, contextual matching, missing translations, and export formatting.
- **FR-015**: For word-only exports, the system MAY use saved phrases to enrich word rows with contextual examples but MUST NOT include those phrases as standalone rows.
- **FR-016**: When duplicate entries have different non-blank translations, the system MUST preserve those translations by combining them in the merged export entry.
- **FR-017**: When linguistic normalization cannot process a token, the system MUST continue the export and use a cleaned original token as the fallback identity for matching and deduplication.
- **FR-018**: The system MUST show the current export phase during an active export and display final row counts after completion.

### Key Entities *(include if feature involves data)*

- **Dictionary Word**: A saved vocabulary word with source text, optional translation, and normalized matching identity.
- **Dictionary Phrase**: A saved phrase or sentence with source text, optional translation, optional movie context, and normalized matching tokens.
- **Phrase Example**: A phrase selected as context for a word, including phrase text, optional translation, and optional movie context.
- **Export Record**: A single downloadable row containing front text and back content; word and phrase distinctions are reflected implicitly through row content and formatting.
- **Export Job**: A user-started export process with status, progress, completion state, and a downloadable output reference.
- **Export Phase**: A user-visible stage of export work, such as fetching, parsing, matching, writing, completed, or failed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A combined export created from representative sample data includes 100% of unique selected words and phrases.
- **SC-002**: At least 95% of words that have a matching saved phrase in representative sample data receive at least one contextual example.
- **SC-003**: No exported word row contains more than two contextual examples.
- **SC-004**: Sample exports containing duplicate entries produce exactly one row per normalized duplicate group while preserving all distinct non-blank translations from that group.
- **SC-005**: Sample entries with missing translations remain present in the export with blank translation fields.
- **SC-006**: A user can start, track, complete, and download the improved export from the web interface in under 10 minutes for a typical vocabulary account.
- **SC-007**: After consolidation, maintainers can identify one authoritative export parsing, matching, and formatting flow during code review.
- **SC-008**: During sample exports, the progress view shows a meaningful phase before completion and a final exported row count after completion.

## Assumptions

- The web interface remains the only primary user-facing export interface.
- The existing account connection, session storage, export status, progress, and download concepts remain in scope.
- The existing local linguistic normalization capability is available to support base-form matching for English and Russian vocabulary.
- TSV remains the default downloadable format for this task; CSV selection can be handled in a later task unless explicitly reintroduced.
- Direct editing of source vocabulary on PuzzleMovies is out of scope.
- Direct import into Anki is out of scope; the app only produces an import-ready file.

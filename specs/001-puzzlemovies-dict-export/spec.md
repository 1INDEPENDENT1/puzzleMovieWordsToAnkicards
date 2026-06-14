# Feature Specification: Puzzle-Movies Dictionary Export

**Feature Branch**: `001-puzzlemovies-dict-export`  
**Created**: 2026-01-18  
**Status**: Draft  
**Input**: User description: "This application helps Puzzle-Movies users turn their personal vocabulary (words and phrases collected from movies) into Anki cards. Instead of manually copying words and examples, the app automatically fetches the user’s dictionary from Puzzle-Movies, matches words with contextual sentence examples, and exports the result into an Anki-importable file."

## Clarifications

### Session 2026-01-18

- Q: How many contextual examples should be included per word when multiple phrases match? → A: 2 examples per word.
- Q: Should phrases that do not match any word still be exported? → A: Include all phrases as standalone entries.
- Q: Should the user choose TSV or CSV at export time? → A: Let the user choose TSV or CSV at export time.
- Q: How should duplicate words or phrases be handled? → A: Merge duplicates into one entry.
- Q: Should items without translations be exported? → A: Export items with missing translations and leave the translation blank.
- Q: Should the export be a single file or separate files for words and phrases? → A: Export a single combined file with words and phrases.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Export vocabulary to Anki file (Priority: P1)

As a Puzzle-Movies user, I want to export my saved vocabulary into an Anki-importable file so I can study it without manual copying.

**Why this priority**: This is the core value of the feature and the primary reason users will open the app.

**Independent Test**: Can be fully tested by providing a sample dictionary and confirming a downloadable Anki-ready file is produced.

**Acceptance Scenarios**:

1. **Given** the user has saved words and phrases in their dictionary, **When** they start an export, **Then** the app produces a downloadable Anki-importable file.
2. **Given** the export completes, **When** the user downloads the file, **Then** it contains entries for their dictionary items.

---

### User Story 2 - Match words to contextual examples (Priority: P2)

As a user, I want exported words to include matching sentence examples from my phrases so I can learn in context.

**Why this priority**: Context improves study quality and is a key differentiator of the export.

**Independent Test**: Can be tested with a dictionary that includes overlapping words and phrases to confirm matches appear in export rows.

**Acceptance Scenarios**:

1. **Given** a word appears in one or more phrases, **When** the export is generated, **Then** the word is paired with at least one contextual example.
2. **Given** multiple phrases match a word, **When** the export is generated, **Then** up to two relevant examples are included.

---

### User Story 3 - Export without context when needed (Priority: P3)

As a user, I want my vocabulary still exported even if no matching context exists so I don't lose items.

**Why this priority**: Ensures completeness and avoids empty exports when matching is not possible.

**Independent Test**: Can be tested with a dictionary where some items have no matching phrases and confirming those items still appear.

**Acceptance Scenarios**:

1. **Given** no matching phrase exists for a word, **When** the export is generated, **Then** the word appears without a contextual example.
2. **Given** a phrase does not match any word, **When** the export is generated, **Then** the phrase still appears as a standalone entry.

---

### Edge Cases

- How does the system handle an empty dictionary (no words or phrases)?
- What happens when dictionary data is missing translations?
- How does the system handle duplicate words or phrases with different translations after merging?
- What happens when more than two phrases match a single word?
- What happens when matching fails for all items?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST fetch the user's dictionary words and phrases from Puzzle-Movies for export.
- **FR-002**: The system MUST match words to phrases where the word appears to provide contextual examples.
- **FR-003**: The system MUST export an Anki-importable TSV or CSV file and provide it via a download link or button.
- **FR-004**: The system MUST include dictionary items in the export even when no matching context exists.
- **FR-005**: The system MUST include translations when available for words and phrases.
- **FR-006**: The system MUST include no more than two contextual examples per word when multiple matches exist.
- **FR-007**: The system MUST export all phrases as standalone entries even when they do not match any word.
- **FR-008**: The system MUST let users choose TSV or CSV format at export time.
- **FR-009**: The system MUST merge duplicate words or phrases into a single export entry.
- **FR-010**: The system MUST export items even when translations are missing, leaving translation fields blank.
- **FR-011**: The system MUST export a single combined file containing both word and phrase entries.

### Key Entities *(include if feature involves data)*

- **Dictionary Word**: A word entry with source text, translation, and optional metadata required for export.
- **Dictionary Phrase**: A phrase/sentence entry with source text, translation, and optional metadata required for export.
- **Export Record**: A row in the generated file containing word/phrase, translation, and optional context.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can complete a full export (words + phrases + matching) in under 5 minutes for a 1,000-item dictionary on a typical broadband connection.
- **SC-002**: 90% of export attempts result in a downloadable file without requiring a retry.
- **SC-003**: At least 85% of exported words include a contextual example when a matching phrase exists.
- **SC-004**: Users report successful completion of the export task on first attempt in at least 90% of usability tests.

## Scope

### In Scope

- Automated retrieval of user dictionary words and phrases.
- Matching words to phrases for contextual examples.
- Exporting Anki-importable files.

### Out of Scope

- Editing dictionary entries in the source service.
- Direct import into Anki within the app.
- Creating or managing the dictionary in the source service.

## Dependencies

- Availability of the source service and the user's dictionary data.

## Assumptions

- The dictionary data includes source text and translations for most words and phrases.
- Users expect the export to include entries even when no matching context is found.

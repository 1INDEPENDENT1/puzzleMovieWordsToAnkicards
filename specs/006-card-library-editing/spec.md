# Feature Specification: Card Library Editing

**Feature Branch**: `codex/006-card-library-editing`  
**Created**: 2026-08-22  
**Status**: Draft  
**Input**: User description: "I want to make visual interface all my data in table in site words - original, translate, their example, statistic about this word, my percent of right answers. And ability to change cards in that table and in the current card during studying."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse My Vocabulary Cards (Priority: P1)

As a learner, I can open a card library and see all of my study cards in a readable table, so I can review my vocabulary and learning results in one place.

**Why this priority**: A visible inventory of cards and performance data is the foundation for finding what to study or correct.

**Independent Test**: Can be tested by giving a learner several cards with different content and answer histories, then opening the library and verifying that every card is represented with its requested fields and performance statistics.

**Acceptance Scenarios**:

1. **Given** a learner has study cards, **When** they open their card library, **Then** they see one row for each of their cards with original text, translation, example or usage instance, study statistics, and correct-answer percentage.
2. **Given** a learner has cards with and without examples or translations, **When** they view the library, **Then** every card remains represented and unavailable optional values are clearly shown as unavailable rather than hidden.
3. **Given** a learner has answered a card, **When** they view its library row, **Then** the displayed statistics and correct-answer percentage reflect the recorded answers for that card.
4. **Given** a learner has not yet answered a card, **When** they view its library row, **Then** its statistics clearly indicate that no answer history exists and do not present an invented percentage.
5. **Given** another learner has cards, **When** the first learner opens their library, **Then** cards and statistics belonging to the other learner are not shown.

---

### User Story 2 - Correct Card Content in the Library (Priority: P1)

As a learner, I can edit a card from its table row, so I can correct an original term, translation, or example without leaving the site.

**Why this priority**: Vocabulary data may need personal corrections, and the library is the natural place to make them.

**Independent Test**: Can be tested by editing each editable content field for a card in the library, saving it, and confirming the updated values appear in the table and later study presentation while its review history remains intact.

**Acceptance Scenarios**:

1. **Given** a card row is visible, **When** the learner chooses to edit it, **Then** they can change the card's original text, translation, and example or usage instance.
2. **Given** the learner saves valid changes, **When** they return to the library, **Then** the row shows the saved content and the same card retains its existing review statistics.
3. **Given** the learner cancels an edit, **When** they return to the table, **Then** the card remains unchanged.
4. **Given** required card identity content would become blank or invalid, **When** the learner attempts to save it, **Then** the system explains the problem and preserves the last saved card.

---

### User Story 3 - Edit the Card I Am Studying (Priority: P2)

As a learner, I can edit the current review card while studying, so I can immediately fix a mistake I notice without losing my place in the session.

**Why this priority**: Corrections are often noticed during recall, when the card context is most useful.

**Independent Test**: Can be tested by opening a review session, editing the displayed card, saving, and verifying that the updated content is shown before the learner records an answer and that the session continues with the same card.

**Acceptance Scenarios**:

1. **Given** a learner is viewing a current review card, **When** they choose to edit it, **Then** they can change its original text, translation, and example or usage instance.
2. **Given** the learner saves changes while studying, **When** editing closes, **Then** the current card displays the updated content and no answer is recorded merely by editing.
3. **Given** the learner cancels editing while studying, **When** they return to the card, **Then** its visible content and session position remain unchanged.
4. **Given** a learner edits the current card, **When** they later open the card library, **Then** the library shows the same saved content.

### Edge Cases

- A card has no translation or no example; the learner can preserve that absence or add content without the card disappearing.
- A card has no recorded answers; its correct-answer percentage remains unavailable until the first answer is recorded.
- An edited original term or example contains punctuation, mixed scripts, or a long phrase; the saved value is displayed without corruption.
- A learner attempts to save invalid card content from either editing entry point; the existing saved card and its answer history remain unchanged.
- The card is updated in the library while a review session has it open; a subsequent view of that card must not silently overwrite a newer saved version.
- A learner opens a library with enough cards that all rows cannot fit on one screen; they can still reach every one of their cards without the page becoming unreadable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide each signed-in learner with a visual card library containing all of that learner's active study cards.
- **FR-002**: Each library row MUST show the card's original text, translation, example or usage instance, review statistics, and correct-answer percentage.
- **FR-003**: Review statistics MUST be calculated from recorded review answers and identify at least the total answers, correct answers, and incorrect answers for the card.
- **FR-004**: The correct-answer percentage MUST be calculated as correct recorded answers divided by all recorded answers for that card, and MUST be unavailable when no answers have been recorded.
- **FR-005**: The library MUST keep cards with blank optional translation or example values visible and clearly communicate the missing value.
- **FR-006**: The library MUST provide a usable way to reach every card when the learner has more cards than can fit in one table view.
- **FR-007**: The system MUST allow a learner to edit their own card's original text, translation, and example or usage instance from the card library.
- **FR-008**: The system MUST allow a learner to edit the same content fields for the current card from an active study session.
- **FR-009**: Saving a card edit MUST make the new content available in both the library and future or current study views of that card.
- **FR-010**: Card edits MUST NOT alter recorded review answers, derived review statistics, correct-answer percentage, or the card's review scheduling state.
- **FR-011**: Cancelling or failing validation of an edit MUST leave the previously saved card content and review history unchanged.
- **FR-012**: Editing a current study card MUST NOT itself count as answering, skip the card, or otherwise advance the study session.
- **FR-013**: The system MUST prevent a learner from viewing or editing any other learner's cards or their statistics.
- **FR-014**: The system MUST preserve existing export and in-app review flows while adding the card library and editing capabilities.
- **FR-015**: The system MUST provide automated checks for library visibility, statistics calculation, learner isolation, edits from both entry points, cancellation or validation failures, and preservation of answer history and scheduling state.

### Key Entities *(include if feature involves data)*

- **Card Library**: The learner-specific visual inventory of active study cards and their derived learning performance.
- **Study Card**: A learner-owned vocabulary item with editable original text, optional translation, optional example or usage instance, and existing review state.
- **Card Statistics**: Derived counts of recorded correct and incorrect answers, total answers, and the learner's correct-answer percentage for one card.
- **Review Attempt**: An existing recorded response that supplies the card statistics and must remain unchanged by content edits.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A learner with up to 100 cards can open the card library and find the original text, translation, example, and performance data for any card in under 30 seconds.
- **SC-002**: In a controlled set of cards with answer histories, 100% of displayed total, correct, incorrect, and correct-answer percentage values match the recorded answers.
- **SC-003**: A learner can save a valid content correction from either the library or an active study card in under 60 seconds.
- **SC-004**: In automated editing checks, 100% of saved card edits leave the card's prior answer count, correct-answer percentage, and next review state unchanged.
- **SC-005**: In access checks, 100% of requests to view or edit another learner's card are rejected and reveal no card content or statistics.
- **SC-006**: Learners can complete their study session after editing a current card without an edit creating a duplicate answer or advancing the queue.

## Assumptions

- The card library covers application-owned in-app study cards, not arbitrary externally uploaded Anki files or unprocessed PuzzleMovies rows.
- "Statistic about this word" means total recorded answers, correct answers, and incorrect answers; the requested percentage is calculated from the same answer history.
- The first version permits editing card learning content only: original text, translation, and example or usage instance. It does not include card deletion, manual statistics changes, answer-history changes, or manual scheduling changes.
- Optional translations and examples may remain blank; original text remains the required study target.
- Existing sign-in identifies the learner and controls access to their own cards.
- The table may use standard navigation appropriate to the number of cards, provided every card remains reachable and the requested data remains readable.

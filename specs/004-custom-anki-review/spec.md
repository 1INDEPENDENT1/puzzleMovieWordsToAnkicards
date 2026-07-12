# Feature Specification: Custom Anki-Style Review Cards

**Feature Branch**: `codex/004-custom-anki-review`  
**Created**: 2026-06-15  
**Status**: Draft  
**Input**: User description: "now we can get files with our parser in the project, but it's hard to parse, because ankicards could be different, so i decided the we need to make our own ankicards realisation in the project. Here is front realization im my anki cards ... and here is back ... and styles ... this is just example, let's create something like this, with sweep animation, algorithms, and two buttons 'I know' and 'I don't know'. Analyze how to make good anki algorithm. how to make animation and style. And how to integrate it in our project."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Study Generated Vocabulary In App (Priority: P1)

As a learner who has generated vocabulary cards from PuzzleMovies data, I can open an in-app review session and study cards without importing them into another flashcard app, so I can use one workflow for export and practice.

**Why this priority**: The main product gap is that arbitrary Anki templates are difficult to parse reliably; owning the card experience removes that dependency while preserving study value.

**Independent Test**: Can be tested by creating a small set of generated vocabulary records, opening a review session, and confirming that due cards appear with front, answer reveal, and two response buttons.

**Acceptance Scenarios**:

1. **Given** generated vocabulary records exist, **When** the user opens review mode, **Then** the user sees the next due card front with the original text and usage instance.
2. **Given** a card front is visible, **When** the user reveals the answer, **Then** the card back shows the original text, the instance, translation or back content, and helpful lookup actions.
3. **Given** the answer is visible, **When** the user selects "I know" or "I don't know", **Then** the current card leaves the screen with a clear motion transition and the next card appears.
4. **Given** no cards are due, **When** the user opens review mode, **Then** the user sees a clear empty-state message with the next expected review availability when known.

---

### User Story 2 - Schedule Reviews With Two Buttons (Priority: P1)

As a learner, I want a simple two-button review flow, so I can quickly mark whether I know a card without choosing from multiple grading options.

**Why this priority**: The requested experience is intentionally simpler than classic multi-grade review, and the scheduling behavior must still protect long-term retention.

**Independent Test**: Can be tested by answering the same card as "I know" and "I don't know" in controlled sessions, then verifying that the next due time changes in opposite directions.

**Acceptance Scenarios**:

1. **Given** a new card is answered "I know", **When** scheduling is calculated, **Then** the card receives a longer next interval than a card answered "I don't know".
2. **Given** a mature card is answered "I know", **When** scheduling is calculated, **Then** the next interval increases compared with its previous successful interval.
3. **Given** any card is answered "I don't know", **When** scheduling is calculated, **Then** the card is scheduled for a short retry and its future growth is reduced.
4. **Given** the user reviews multiple cards, **When** the session ends, **Then** every answer is recorded with enough history to explain the current due state.

---

### User Story 3 - Preserve Useful Study Context (Priority: P2)

As a learner, I want each review card to preserve the word, phrase, sentence, and translation context produced by the export flow, so I can learn vocabulary in context rather than as isolated words.

**Why this priority**: The existing custom Anki template is valuable because it combines original text, contextual instance, translation, and external lookup links.

**Independent Test**: Can be tested with cards generated from words with phrase examples, standalone phrases, blank translations, and long instances.

**Acceptance Scenarios**:

1. **Given** a word card has a contextual phrase example, **When** the card is studied, **Then** the phrase is visible as the usage instance.
2. **Given** a phrase card has no separate word field, **When** the card is studied, **Then** the phrase can still be shown as the original study target.
3. **Given** a card has a blank translation, **When** the answer is revealed, **Then** the card still appears and clearly preserves the blank translation state.
4. **Given** a card contains long text, **When** it is viewed on a small screen, **Then** the text remains readable without overlapping controls.

---

### User Story 4 - Make Review Feel Polished And Fast (Priority: P3)

As a learner, I want the review screen to feel responsive and pleasant, so repeated study sessions do not feel clumsy or tiring.

**Why this priority**: This feature is a daily-use learning surface; motion, spacing, and feedback affect whether users continue studying.

**Independent Test**: Can be tested by completing a review session on desktop and mobile-sized screens while observing motion, button availability, readability, and session progress.

**Acceptance Scenarios**:

1. **Given** a card is answered, **When** it leaves the screen, **Then** the motion direction and feedback make the selected answer obvious.
2. **Given** the user prefers reduced motion, **When** a card is answered, **Then** the transition remains clear without relying on large sweeping animation.
3. **Given** a review session is active, **When** the user studies cards, **Then** progress information remains visible without distracting from the current card.
4. **Given** network access to external lookup services is unavailable, **When** the user studies cards, **Then** core review still works.

### Edge Cases

- A generated card has only original text and no instance or translation.
- A generated card has an instance that is the same as the original text.
- The same original text appears in several contexts and should not overwrite separate review history when represented as separate cards.
- The same generated card is imported or produced more than once and should not create duplicate active review cards without user intent.
- A user exits during a card transition or after revealing the answer but before pressing a response button.
- A user opens review mode from multiple browser tabs or devices.
- The review queue contains many due cards.
- External lookup links contain spaces, punctuation, mixed case, Cyrillic text, or special characters.
- Motion or color feedback cannot be perceived by the user, so state changes must also be understandable through text and layout.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide an in-app review mode for vocabulary cards generated by the application.
- **FR-002**: The system MUST define its own review-card data shape instead of relying on parsing arbitrary external flashcard templates.
- **FR-003**: Each review card MUST support original text, optional usage instance, optional translation or back content, and optional source context.
- **FR-004**: The card front MUST show the study target and available usage instance before the answer is revealed.
- **FR-005**: The card back MUST show the study target, usage instance, translation or back content, and lookup actions when the needed text is available.
- **FR-006**: Lookup actions MUST open relevant external dictionary, translation, or pronunciation pages without blocking the review session.
- **FR-007**: The review flow MUST provide exactly two answer choices for scheduling decisions: "I know" and "I don't know".
- **FR-008**: The system MUST record every completed review answer with card identity, answer choice, reviewed time, and resulting due state.
- **FR-009**: The scheduling behavior MUST make "I know" increase or maintain the card's review interval and make "I don't know" schedule a shorter retry than a successful answer.
- **FR-010**: The scheduling behavior MUST distinguish at least new cards, learning cards, review cards, and cards recently answered incorrectly.
- **FR-011**: The scheduling behavior MUST use prior review history so repeated success produces longer intervals and repeated difficulty slows interval growth.
- **FR-012**: The system MUST show only cards that are due or intentionally introduced as new cards for the current session.
- **FR-013**: The system MUST prevent accidental duplicate active review cards when the same generated vocabulary record is processed again.
- **FR-014**: The system MUST animate card transitions after an answer is chosen, with a clearly different visual result for "I know" and "I don't know".
- **FR-015**: The system MUST provide a reduced-motion experience that preserves review clarity without large movement.
- **FR-016**: The review screen MUST remain usable on desktop and mobile-sized screens.
- **FR-017**: The review screen MUST show session progress, including reviewed count and remaining due count.
- **FR-018**: The system MUST preserve existing export/download behavior while adding in-app review as an additional study path.
- **FR-019**: The system MUST provide automated checks for review-card creation, duplicate prevention, answer recording, scheduling outcomes, and primary review-session flows.

### Key Entities *(include if feature involves data)*

- **Review Card**: A study item owned by the app, containing original text, optional instance, optional translation or back content, optional context, and active/inactive state.
- **Review Session**: A learner's current study run, including due cards presented, cards completed, and remaining count.
- **Review Attempt**: A recorded answer for a card, including the chosen button, review time, and scheduling result.
- **Scheduling State**: The current learning status for a card, including due time, interval, difficulty or ease, lapse count, and review count.
- **Lookup Action**: A contextual external action, such as dictionary, translation, or pronunciation lookup, generated from card text.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A learner can open a review session from generated vocabulary and answer the first card in under 10 seconds.
- **SC-002**: In controlled sample data, 100% of generated review cards preserve original text and at least one of instance, translation, or back content when available.
- **SC-003**: In scheduling tests, cards answered "I know" receive a later next due time than equivalent cards answered "I don't know" in 100% of cases.
- **SC-004**: In repeated-success scheduling tests, a card's interval grows after at least three consecutive successful reviews.
- **SC-005**: In repeated-difficulty scheduling tests, a card answered "I don't know" becomes due again within a short retry window.
- **SC-006**: A 50-card review session can be completed without page reloads, lost answers, or duplicated active cards.
- **SC-007**: The review screen remains readable and operable at common desktop and mobile viewport sizes.
- **SC-008**: Users can complete review sessions even when external lookup pages are unavailable.

## Assumptions

- The first version uses a binary answer model only; multi-grade answers such as "Again", "Hard", "Good", and "Easy" are out of scope unless reintroduced later.
- The built-in review mode is added alongside Anki-ready export rather than replacing it.
- Review cards are created from the app's generated vocabulary records, not by importing and interpreting arbitrary user-provided Anki templates.
- Default lookup actions are inspired by the user's current cards: dictionary/translation lookup for original text, pronunciation lookup for original text, and translation lookup for the instance.
- A good first scheduling model can be Anki-like without matching Anki exactly: success increases future spacing, failure creates a near retry and slows future growth, and review history remains explainable.
- Visual polish matters, but the review must remain accessible without depending only on animation, color, or external services.

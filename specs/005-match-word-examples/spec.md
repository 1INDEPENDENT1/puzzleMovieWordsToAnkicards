# Feature Specification: Match Words With Context Examples

**Feature Branch**: `codex/005-match-word-examples`  
**Created**: 2026-07-29  
**Status**: Draft  
**Input**: User description: "We made cards for our words, but the cards don't use examples parsed from the site, only word and translation. We have examples and need to use them with the original word in cards. Implement logic for comparing words and examples. Examine task 004. Show the original word and its examples when they match. When an example has no matching word, show the example alone. Show translations on the other side of the card."

## Clarifications

### Session 2026-08-12

- Q: How many matched examples may a saved word retain as separate study cards? → A: Up to 2 examples per word
- Q: If a word matches more than two examples, should the extra matched examples become standalone example cards? → A: Keep extra examples as standalone cards
- Q: Which two matched examples should be retained as contextual cards when a word matches more than two? → A: First two in source order
- Q: If an example is contextual for one matched word but exceeds the cap for another, should it also become a standalone card? → A: Yes; retain contextual cards for every eligible matched word and a standalone card for each overflow case.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Study a Word in Its Matched Context (Priority: P1)

As a learner, I want a word card to show the saved word together with a sentence or phrase that contains it, so I can recall the word in the context where I encountered it.

**Why this priority**: Context is the core learning value missing from isolated word-and-translation cards.

**Independent Test**: Create a word and a saved example that are identified as a match, create review cards, and verify that the card front presents the word and its example before the answer is revealed.

**Acceptance Scenarios**:

1. **Given** a saved word is matched to one or more saved examples, **When** review cards are created, **Then** each resulting contextual card shows the original word and one matched example on its front.
2. **Given** a contextual card is on the front, **When** the learner reveals the answer, **Then** the back shows the word translation and the matched example translation when each is available.
3. **Given** the same word is matched to several different examples, **When** cards are created, **Then** the examples remain separately studyable rather than being merged into an unreadable single card.

---

### User Story 2 - Study an Unmatched Example Independently (Priority: P1)

As a learner, I want a saved example that cannot be linked to a saved word to remain available as a standalone card, so useful context is not lost because a comparison did not find a pair.

**Why this priority**: Saved phrases and sentences are valuable study material even when no saved word can be associated with them.

**Independent Test**: Create an example that matches none of the saved words, create review cards, and verify that its front contains only the example and its answer contains its translation when available.

**Acceptance Scenarios**:

1. **Given** a saved example has no matching saved word, **When** review cards are created, **Then** a standalone card is created with the example as its sole front-side study text.
2. **Given** a standalone example card is revealed, **When** the example has a translation, **Then** that translation is visible on the back.
3. **Given** a standalone example has no translation, **When** it is revealed, **Then** the card remains reviewable and does not display invented translation text.

---

### User Story 3 - Preserve Correct Associations (Priority: P2)

As a learner, I want only reliable word-to-example associations to appear together, so unrelated examples do not make cards misleading.

**Why this priority**: Incorrect context can cause a learner to attach a translation or usage to the wrong word.

**Independent Test**: Use words with exact and normalized variants plus unrelated examples, and verify that only examples associated with the intended word appear on that word's cards.

**Acceptance Scenarios**:

1. **Given** an example contains a normalized or inflected form associated with a saved word, **When** comparison is performed, **Then** the example is eligible to appear with that word.
2. **Given** an example does not contain a form associated with a saved word, **When** cards are created, **Then** it is not shown as that word's context.
3. **Given** a source item is duplicated, **When** cards are created again, **Then** duplicate handling continues to preserve one active card per distinct study context.
4. **Given** a word is matched to more than two examples, **When** contextual cards are created, **Then** the first two examples in source order are retained as contextual cards and the remaining examples are retained as standalone cards.
5. **Given** an example matches multiple words, **When** it is within the two-context limit for one or more words and exceeds the limit for another, **Then** it appears as a contextual card for each eligible word and as a standalone card for the overflow case.

### Edge Cases

- A word has no matching examples but still has a translation.
- An example has no matching word and no translation.
- A word is associated with more examples than the card-creation limit permits.
- An example contains more than one saved word.
- Original text or translations include punctuation, line breaks, markup-sensitive characters, or non-Latin text.
- The word translation is blank while the matched example translation is present, or the reverse.
- The same original word has multiple different contextual examples.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST compare saved words with tokens or normalized forms available from saved examples to identify word-to-example associations.
- **FR-002**: The system MUST create contextual study cards for each retained association between a saved word and a saved example.
- **FR-003**: The front of a contextual study card MUST show the original word and its associated example before answer reveal.
- **FR-004**: The back of a contextual study card MUST show the translation of the original word and the translation of its associated example whenever those translations are available.
- **FR-005**: The system MUST keep a saved example that has no associated saved word as a standalone study card.
- **FR-006**: The front of a standalone example card MUST show the example without an unrelated word label or word translation.
- **FR-007**: The back of a standalone example card MUST show its translation when available and otherwise preserve the card as having no translation.
- **FR-008**: The system MUST NOT present an example as context for a word unless the comparison identifies that association.
- **FR-009**: The system MUST preserve distinct examples for the same word as separate study contexts.
- **FR-010**: The system MUST preserve existing review scheduling, answer history, export/download behavior, and duplicate-prevention behavior while adding or correcting context presentation.
- **FR-011**: The system MUST preserve source vocabulary and examples when translations or source metadata are missing.
- **FR-012**: The system MUST provide automated checks for matched contextual cards, unmatched standalone-example cards, absent translations, and prevention of unrelated associations.
- **FR-013**: The system MUST retain at most two matched examples per saved word as separate contextual study cards.
- **FR-014**: When a saved example matches a word but exceeds that word's two-context limit, the system MUST retain it as a standalone example card.
- **FR-015**: When more than two examples match a saved word, the system MUST retain the first two examples in source order as that word's contextual study cards.
- **FR-016**: An example that matches multiple saved words MUST be retained as a contextual study card for every word for which it is within that word's two-context limit; if it exceeds the limit for another matched word, the system MUST additionally retain it as a standalone example card.

### Key Entities *(include if feature involves data)*

- **Saved Word**: Original vocabulary text with its available translation and comparison identity.
- **Saved Example**: A saved phrase or sentence with its original text, optional translation, source context, and comparison tokens.
- **Word-to-Example Association**: A reliable relationship between a saved word and a saved example that may form a contextual study card.
- **Contextual Study Card**: A review card whose front contains an original word and one associated example, and whose back contains their available translations.
- **Standalone Example Card**: A review card made from an example without an associated saved word; its front is the example and its back is its available translation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a controlled set of matched words and examples, 100% of contextual cards show both the intended word and associated example on their front.
- **SC-002**: In a controlled set of unmatched examples, 100% remain available as standalone example cards.
- **SC-003**: In controlled sample data with available translations, 100% of revealed contextual cards show the available word and example translations on the back.
- **SC-004**: In controlled negative-match tests, 0 unrelated examples are presented as context for a word.
- **SC-005**: A learner can distinguish a contextual word card from a standalone example card without revealing the answer in 100% of reviewed sample cases.
- **SC-006**: Creating and reviewing a 50-item mixed set of matched and unmatched source data completes without lost source items or duplicate active cards.

## Assumptions

- A word can have up to two separate contextual cards, one for each retained matched example.
- An example may be associated with more than one word when the source data identifies more than one valid association; each contextual card retains its own word and example.
- A saved word with no matched example remains available as an existing word-only card, because the requirement to preserve vocabulary completeness still applies; the standalone-example behavior applies to examples that have no associated word.
- At most two matched examples are retained per word unless a later feature changes that limit.
- Examples that would exceed a word's two-context limit remain available as standalone example cards.
- When more than two examples match a word, source order determines which two remain contextual cards.
- An example may be duplicated across contextual cards for multiple matched words and may additionally have a standalone card when it overflows a matched word's two-context limit.
- Translation text is shown only on the revealed side of a review card; the front remains focused on recall from the original language.
- This feature refines the context data passed into the existing in-app review flow from feature 004; it does not replace the binary scheduling or review-session experience.

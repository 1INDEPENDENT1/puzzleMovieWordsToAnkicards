# Research: Match Words With Context Examples

**Feature**: [Match Words With Context Examples](spec.md)  
**Date**: 2026-08-12

## Decision: Preserve complete associations separately from the export display cap

**Decision**: Generate a complete, source-ordered word-to-example association set for review-card creation, then select the first two matches per word only when producing contextual review cards. Keep the existing two-example export rendering behavior unchanged.

**Rationale**: `DictionaryMatcher` currently stops after two examples, so unmatched examples and overflow matches disappear before review-card creation. The feature requires those examples to remain studyable while keeping the two-card contextual limit. The parser and deduplicator already provide deterministic list order, and the matcher already uses normalized identities/tokens.

**Alternatives considered**:

- Raise or remove the export matcher cap: rejected because it changes the established Anki TSV rendering behavior and can create overcrowded exported cards.
- Re-match from rendered HTML: rejected because the rendered export back is presentation data, not an authoritative association model.
- Use substring matching: rejected because it increases false matches, especially for short or non-Latin terms.

## Decision: Carry application-owned structured review sources alongside export records

**Decision**: During an export, retain a process-local structured review-source payload next to the existing cached `ExportRecord` list. The review import endpoint consumes prebuilt review drafts from that payload; it does not parse the TSV or render-time HTML.

**Rationale**: The existing review flow already intentionally relies on completed-export data held in memory for the current app process. A structured source can represent word-only cards, contextual word/example pairs, unmatched examples, and overflow examples without relying on HTML structure.

**Alternatives considered**:

- Parse the generated TSV: rejected by the project constraint against treating arbitrary Anki/template output as review input, and TSV strips review-specific structure.
- Persist raw parser rows or source pages: rejected because this feature needs no new durable data and must preserve the current completed-export lifecycle.
- Modify the scheduler or review schema: rejected because card-content generation is independent of scheduling and answer history.

## Decision: Use explicit card-creation rules with idempotent standalone examples

**Decision**: Create review drafts as follows:

1. A word with no matches creates one word-only card.
2. For each word, its first two source-ordered matches create contextual cards, one draft per word/example pair.
3. An example with no matches creates one standalone example card.
4. An example that overflows at least one matched word's two-card limit creates one standalone example card; it may also create contextual cards for every word where it is retained.
5. Repeated identical standalone drafts resolve to the existing per-user content-key upsert, preserving one active card per distinct standalone study context.

**Rationale**: This implements the clarified preservation and duplication rules while retaining the existing duplicate-prevention contract. A contextual draft has a word plus an instance; a standalone draft has the example alone. Two otherwise identical standalone copies add no distinct study context, so existing deduplication correctly merges them.

**Alternatives considered**:

- Make every phrase standalone even when it is already a retained context: rejected because it adds unnecessary duplicate review work and conflicts with the feature's standalone-only cases.
- Discard overflow examples: rejected because the clarification explicitly preserves them.
- Let one example belong to only one word: rejected because a source example may legitimately contain multiple saved words.

## Decision: Retain the existing review endpoint and display contract

**Decision**: Keep `POST /reviews/cards`, `GET /reviews`, `GET /reviews/next`, and `POST /reviews/cards/{id}/answer` unchanged. The content semantics of `ReviewCardView` distinguish contextual cards through `originalText` plus `instanceText`, and standalone examples through example-only `originalText` with no instance.

**Rationale**: The existing DTO and review UI already have fields for this display. No new public endpoint, database column, scheduler rule, or frontend framework is required.

**Alternatives considered**:

- Add a context-specific endpoint: rejected because import remains the same completed-export operation.
- Add a card-type column: rejected because `recordKind`, `originalText`, and optional `instanceText` already describe the display form without affecting scheduling.

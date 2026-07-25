# Research: Custom Anki-Style Review Cards

## Decision: Own the review-card model instead of parsing arbitrary Anki templates

**Rationale**: The user's core pain is that Anki cards can be shaped many different ways, making parser behavior brittle. The existing export flow already produces normalized `ExportRecord` values with front/back content, contextual examples, and translations. Creating review cards from that controlled output gives the app stable fields for display, scheduling, deduplication, and lookup actions.

**Alternatives considered**:

- Continue parsing user-provided Anki HTML/templates: rejected because templates vary too widely and behavior is hard to test.
- Store raw generated TSV rows only: rejected because scheduling, lookup actions, and context-specific review history need structured fields.
- Replace Anki export entirely: rejected because existing Anki-ready download remains useful and is explicitly preserved by the spec.

## Decision: Start with a binary Anki-like scheduler, not full FSRS

**Rationale**: The requested UX has exactly two buttons: "I know" and "I don't know". Modern Anki supports FSRS, and the Anki manual describes FSRS as a more accurate alternative to legacy SM-2, but FSRS is designed around richer review history, desired retention, and parameter optimization. A first in-app scheduler should be explainable and reliable with limited local review history: success increases spacing, failure schedules a short retry and slows future growth.

Suggested initial behavior:

- New card + "I don't know": learning state, due again in 10 minutes, interval remains 0 days, lapse count increments if applicable.
- New card + "I know": review state, due tomorrow, interval becomes 1 day.
- Learning/relearning + "I don't know": due again in 10 minutes and difficulty/ease worsens.
- Learning/relearning + "I know": graduate or re-graduate to review with a 1 day interval.
- Review + "I know": next interval grows by the card's ease/difficulty multiplier, with a minimum one-day increase for mature cards.
- Review + "I don't know": move to relearning, due again in 10 minutes, record lapse, and reduce future interval growth.

**Alternatives considered**:

- Full FSRS implementation now: rejected for first release because it requires more review history, optimization logic, and a more detailed grade model than the requested two buttons.
- Direct SM-2 clone: rejected because the four-button grade model does not map cleanly to the requested binary UX.
- Fixed intervals only: rejected because repeated successes and failures would not adapt to the learner.

**Reference context**: Anki's manual notes that FSRS schedules based on desired retention, optimizer parameters, and review history; it also recommends learning/relearning steps shorter than one day and clearly treats failed recall as a failing answer rather than a passing answer. See the Anki Manual deck options page: https://docs.ankiweb.net/deck-options.html

## Decision: Persist current scheduling state and immutable review attempts

**Rationale**: The app needs fast due-card queries and an explainable history. Keeping a current scheduling state on each card avoids recalculating from all attempts every time, while immutable attempts preserve auditability and enable later algorithm improvements.

**Alternatives considered**:

- Store only current due date: rejected because later tuning and debugging need the answer history.
- Derive all state from attempts on every request: rejected as unnecessary complexity for normal review-page loading.

## Decision: Use deterministic duplicate identity from generated card content

**Rationale**: Re-running an export should not create duplicate active review cards. A stable duplicate key per user and card content allows upsert behavior while preserving separate cards for the same word in meaningfully different contexts.

**Recommended identity input**: user, original text, instance text, translation/back content, and source context when present. Normalize whitespace and case where appropriate, but do not collapse different instances into one card unless they are truly identical.

**Alternatives considered**:

- Original text only: rejected because the same word can appear in multiple useful contexts.
- Export job id plus row number: rejected because the same vocabulary would duplicate across repeated exports.

## Decision: Generate lookup actions from fields at render time

**Rationale**: Lookup links are derived from current card text and do not need persistence. Generating them when rendering the review page keeps cards portable and allows URL rules to change later without migrating stored card rows.

**Default lookup actions**:

- Original text to dictionary/translation lookup.
- Original text to pronunciation lookup.
- Instance text to full-sentence translation lookup when instance text is present.

**Alternatives considered**:

- Persist full URLs: rejected because external URL formats may change and URLs can always be rebuilt.
- Require network checks before showing links: rejected because review must work offline except for the external pages themselves.

## Decision: Use CSS sweep transitions with reduced-motion fallback

**Rationale**: The feature needs a polished answer transition without requiring new frontend dependencies. Existing static CSS and JavaScript are enough for card reveal, answer submission, and a sweeping left/right or down/up transition. `prefers-reduced-motion` should switch to a fade/status change so the state remains understandable without large movement.

**Alternatives considered**:

- Add a frontend animation framework: rejected because this app currently uses simple Thymeleaf templates, CSS, and small JavaScript files.
- No animation: rejected because the spec explicitly calls for sweep animation and visible answer feedback.

## Decision: Expose review through server-rendered pages plus small JSON answer/status endpoints

**Rationale**: The existing application is server-rendered and already uses JSON status polling for export progress. Review can follow the same shape: Thymeleaf renders the session page, JavaScript posts answer choices, and the server returns the next due card plus updated counts.

**Alternatives considered**:

- Full single-page app: rejected as out of proportion for the current app.
- Pure form POST with full reload for every answer: rejected because the success criteria call for sessions without page reloads and polished transitions.

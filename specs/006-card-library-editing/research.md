# Research: Card Library Editing

## Decision: Derive library statistics from immutable review attempts

**Rationale**: `ReviewCard.reviewCount` tracks completed reviews but cannot distinguish
correct from incorrect responses, and `lapseCount` is not the count of all incorrect
answers. A user-scoped aggregate over `ReviewAttempt.answer` provides total, `KNOWN`,
and `UNKNOWN` counts while preserving zero-attempt cards through a left join. The
correct-answer percentage is `KNOWN / total * 100` and is absent when total is zero.

**Alternatives considered**:

- Use `reviewCount` and `lapseCount`: rejected because they cannot accurately produce
  correct and incorrect totals.
- Persist copied statistics on the card: rejected because the append-only attempts are
  the source of truth and copies could drift.
- Load attempts per row: rejected because it creates avoidable per-card queries.

## Decision: Use a fixed 25-row, owner-scoped paginated table

**Rationale**: The feature requires every active card to be reachable, not all cards to
be rendered at once. A fixed page size makes the table readable on desktop and mobile,
places a predictable upper bound on the aggregation query, and supports the specified
100-card usage target. The page is ordered deterministically by normalized original
text and card identifier.

**Alternatives considered**:

- Render the full library: rejected because it degrades readability and query size as
  the collection grows.
- Require search before access: rejected because it would hide cards and was not
  requested.
- Add client-side table framework: rejected because the existing app is server-rendered
  and needs no new dependency for pagination.

## Decision: Update only the three learner-editable content fields

**Rationale**: The requested fields map directly to `originalText`, `translationText`,
and `instanceText`. The edit service accepts a dedicated request rather than binding a
persistent entity, so ownership, duplicate key, source context, answer counts, and
scheduling fields cannot be submitted or changed accidentally. Original text is
trimmed and required; optional fields accept blank values.

**Alternatives considered**:

- Bind `ReviewCard` directly: rejected because it exposes scheduling and identity
  fields to a user edit request.
- Allow editing answer history or scheduling: rejected because the approved scope
  explicitly excludes it.
- Make source context editable: rejected because it is not a requested editable field.

## Decision: Use optimistic locking for edits and answers

**Rationale**: A card can be open in the review UI while it is edited in the library,
and answering also mutates the card's schedule. A persisted optimistic version supplied
with the edit request prevents a stale review page or duplicate browser tab from
silently overwriting a newer update. A conflict returns an explicit response so the UI
can ask the learner to reload the current content.

**Alternatives considered**:

- Last-write-wins updates: rejected because it violates the specification's stale-view
  edge case.
- Pessimistic database locks: rejected because a learner may leave an edit form open,
  unnecessarily blocking review and library use.

## Decision: Preserve manual content edits during later source refreshes

**Rationale**: Existing imports locate cards by a content-derived key and refresh their
display fields. Without a manual-content marker, a later re-import can silently erase a
learner correction. A card-level marker set only by learner edits lets import preserve
the corrected fields while retaining the stable import identity, review history, and
scheduling state.

**Alternatives considered**:

- Recompute the duplicate key when edited: rejected because it risks duplicate-card
  conflicts and disconnects a card from its import identity.
- Keep current refresh-overwrites-edit behavior: rejected because saved corrections
  would not remain reliable in future use.
- Build a full field-by-field source merge system: rejected as beyond the feature;
  preserving learner-owned editable content is sufficient here.

## Decision: Keep the library server-rendered and edit the current card with JSON

**Rationale**: The existing review page is server-rendered and its JavaScript already
updates the current card after an answer. A new `/cards` page can render the table and
pagination through the existing layout, while one `PATCH /cards/{id}` contract returns
an updated card view for both the library editor and current review-card editor. Saving
does not call the answer or next-card endpoints, preserving queue position and reviewed
count.

**Alternatives considered**:

- Full single-page card library: rejected as out of proportion to the current
  Thymeleaf application.
- Full page reload for a current-card edit: rejected because it risks losing the
  learner's revealed state and is unnecessary.
- Reuse the import endpoint for editing: rejected because import and learner edits
  have different validation and safety semantics.

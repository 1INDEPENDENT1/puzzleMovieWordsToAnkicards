# Data Model: Card Library Editing

## ReviewCard (existing, extended)

The persistent, learner-owned study card remains the source of editable content and
current scheduling state.

### Existing fields used by this feature

- `id`: stable card identity.
- `user`: card owner; every read and update is scoped to this user.
- `originalText`: required editable study target, maximum 1,000 characters after trim.
- `instanceText`: optional editable example or usage instance, maximum 4,000 characters.
- `translationText`: optional editable translation, maximum 8,000 characters.
- `contentKey`: stable import-deduplication identity; never changed by learner edits.
- `state`, `dueAt`, `intervalDays`, `easeFactor`, `reviewCount`, `lapseCount`,
  `lastReviewedAt`: scheduling fields; never changed by learner edits.
- `createdAt`, `updatedAt`: existing timestamps.

### New fields

- `version`: optimistic-lock value. It is returned with editable views and must match
  when an edit is saved. A mismatch is a stale-edit conflict.
- `manualContentOverride`: whether a learner has saved editable card content. When it
  is true, a later import refresh must not overwrite `originalText`, `instanceText`, or
  `translationText`.

### Validation and mutation rules

- A learner edit changes only `originalText`, `instanceText`, and `translationText`.
- The edit service trims `originalText` and rejects a blank result.
- Blank optional text is retained as an empty or absent optional value consistently
  with the existing display behavior.
- A successful learner edit sets `manualContentOverride` to true and advances
  `version`/`updatedAt`.
- An import can continue to create cards and update uncustomized cards. It must leave
  learner-edited content intact when `manualContentOverride` is true.

## ReviewAttempt (existing, unchanged)

Append-only record of one completed `KNOWN` or `UNKNOWN` answer. It remains the source
for all library performance values and is never created, altered, or deleted by a
content edit.

## CardLibraryItem (read model)

Owner-scoped projection for one active table row. It is not a persisted entity.

### Fields

- Card identity, `version`, original text, translation, and instance text.
- `totalAnswers`: count of the card's recorded attempts.
- `correctAnswers`: count of `KNOWN` attempts.
- `incorrectAnswers`: count of `UNKNOWN` attempts.
- `correctAnswerPercentage`: nullable percentage in the inclusive 0–100 range,
  calculated from correct and total answers; absent if `totalAnswers` is zero.

### Query rules

- Include cards owned by the signed-in learner whose state is not `SUSPENDED`.
- Use an attempt aggregate with a left join so cards with no attempts stay visible.
- Sort deterministically by original text, then ID, and return a fixed 25-row page.

## CardContentUpdateRequest (boundary model)

Input for both edit entry points.

### Fields

- `originalText`: required learner-supplied text.
- `instanceText`: optional learner-supplied example.
- `translationText`: optional learner-supplied translation.
- `version`: required optimistic-lock value observed when the learner opened editing.

### Outcomes

- Valid owner-scoped update: returns the updated card view and its new version.
- Invalid content: returns a validation error and does not persist a partial edit.
- Missing or foreign card: returns not found without disclosing ownership.
- Stale version: returns a conflict without overwriting the newer card.

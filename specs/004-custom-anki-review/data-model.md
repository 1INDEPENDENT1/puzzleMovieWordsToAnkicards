# Data Model: Custom Anki-Style Review Cards

## ReviewCard

Persistent study item generated from application-owned vocabulary output.

### Fields

- `id`: UUID primary identity.
- `user`: Owning user.
- `originalText`: Required study target shown on front and back.
- `instanceText`: Optional contextual phrase or sentence.
- `translationText`: Optional translation or back content.
- `sourceContext`: Optional source label, movie title, or origin metadata.
- `contentKey`: Required stable duplicate key scoped to user.
- `state`: Current scheduling state: `NEW`, `LEARNING`, `REVIEW`, `RELEARNING`, `SUSPENDED`.
- `dueAt`: Next time the card can appear in review.
- `intervalDays`: Current review interval in days; `0` for same-day learning steps.
- `easeFactor`: Current interval growth factor for successful review cards.
- `reviewCount`: Number of completed review attempts.
- `lapseCount`: Number of failed review attempts after initial learning.
- `lastReviewedAt`: Last completed attempt time.
- `createdAt`: Creation time.
- `updatedAt`: Last mutation time.

### Validation Rules

- `originalText` must be non-blank after trimming.
- `contentKey` must be unique per user among active cards.
- `easeFactor` must remain within configured minimum and maximum bounds.
- `intervalDays` must be zero or positive.
- `dueAt` must be present for active cards.
- Blank translations are allowed and must not block card creation.

### Relationships

- Many `ReviewCard` rows belong to one `User`.
- One `ReviewCard` has many `ReviewAttempt` rows.

### State Transitions

```text
NEW --I know--> REVIEW
NEW --I don't know--> LEARNING
LEARNING --I know--> REVIEW
LEARNING --I don't know--> LEARNING
REVIEW --I know--> REVIEW
REVIEW --I don't know--> RELEARNING
RELEARNING --I know--> REVIEW
RELEARNING --I don't know--> RELEARNING
ANY ACTIVE STATE --manual suspend--> SUSPENDED
SUSPENDED --manual restore--> previous active state or REVIEW
```

## ReviewAttempt

Immutable record of a completed answer.

### Fields

- `id`: UUID primary identity.
- `reviewCard`: Reviewed card.
- `user`: Owning user for access checks and reporting.
- `answer`: `KNOWN` or `UNKNOWN`.
- `reviewedAt`: Time the learner chose an answer.
- `previousState`: Scheduling state before the answer.
- `nextState`: Scheduling state after the answer.
- `previousDueAt`: Due time before the answer.
- `nextDueAt`: Due time after the answer.
- `previousIntervalDays`: Interval before the answer.
- `nextIntervalDays`: Interval after the answer.
- `responseMillis`: Optional time from card display to answer.

### Validation Rules

- `answer`, `reviewedAt`, `previousState`, `nextState`, and `nextDueAt` are required.
- Attempts are append-only after creation.
- Attempt user must match the reviewed card owner.

## ReviewSession

Short-lived study run. This can be represented as request/session state rather than a required database table in the first release.

### Fields

- `id`: Session identifier when persisted or stored in browser state.
- `user`: Active user.
- `startedAt`: Time the session began.
- `reviewedCount`: Number of cards answered during this session.
- `remainingDueCount`: Number of currently due cards after the latest answer.
- `currentCard`: Card currently shown when one is available.

### Validation Rules

- Session must be associated with a signed-in user.
- Only due cards or intentionally introduced new cards can be presented.

## LookupAction

Derived display action, not necessarily persisted.

### Fields

- `type`: `DICTIONARY`, `PRONUNCIATION`, or `INSTANCE_TRANSLATION`.
- `label`: User-visible action text.
- `url`: Fully encoded external URL.
- `sourceText`: Text used to generate the URL.

### Validation Rules

- URL text must be encoded safely.
- Actions requiring missing source text must be omitted.

## AnswerChoice

Scheduling decision submitted by the learner.

### Values

- `KNOWN`: The learner recalled the card and selected "I know".
- `UNKNOWN`: The learner did not recall the card and selected "I don't know".

## Scheduling Defaults

- `NEW + KNOWN`: `REVIEW`, due in 1 day, interval 1 day.
- `NEW + UNKNOWN`: `LEARNING`, due in 10 minutes, interval 0 days.
- `LEARNING + KNOWN`: `REVIEW`, due in 1 day, interval 1 day.
- `LEARNING + UNKNOWN`: `LEARNING`, due in 10 minutes, interval 0 days.
- `REVIEW + KNOWN`: `REVIEW`, due after increased interval, at least one day later than the previous interval for mature cards.
- `REVIEW + UNKNOWN`: `RELEARNING`, due in 10 minutes, lapse count increments, future growth factor decreases.
- `RELEARNING + KNOWN`: `REVIEW`, due in at least 1 day or a reduced prior interval.
- `RELEARNING + UNKNOWN`: `RELEARNING`, due in 10 minutes.

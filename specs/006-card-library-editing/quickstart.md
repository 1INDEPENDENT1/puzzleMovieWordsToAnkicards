# Quickstart: Card Library Editing Validation

## Prerequisites

- PostgreSQL configured through the existing application configuration.
- A signed-in learner with at least three generated review cards: one with no attempts,
  one with a `KNOWN` answer, and one with both `KNOWN` and `UNKNOWN` answers.

## Run

From the repository root:

```powershell
mvn test
mvn spring-boot:run
```

Open `http://localhost:8080/login`, sign in, create review cards from a completed
export if required, then open `/cards`.

## Validate the card library

1. Confirm that each active card appears through the table pages, including cards with
   blank translation or example values.
2. Confirm each row displays original text, translation, example, total/correct/
   incorrect counts, and a percentage only when the total is nonzero.
3. Cross-check the displayed values against the card's recorded `KNOWN` and `UNKNOWN`
   review attempts. The calculation is defined in [data-model.md](data-model.md).
4. If more than 25 active cards exist, use pagination until a later card is reachable.

## Validate editing from the library

1. Edit original text, translation, and example for a card, then save.
2. Confirm the table row displays the updated values and the answer counts, percentage,
   and next-review state are unchanged.
3. Start an edit and cancel it; confirm no saved content changes.
4. Attempt to save a blank original text; confirm a validation message and no change.
5. Open the same edit in two browser tabs, save one, then save the stale tab. Confirm
   the second save reports a conflict and leaves the newer content intact.

## Validate editing during review

1. Open `/reviews` with a due card and reveal its answer.
2. Edit the current card and save. Confirm the current card reflects the change without
   recording an answer, changing the reviewed count, or advancing the queue.
3. Cancel an in-review edit and confirm the card and reveal state remain usable.
4. Answer the card normally and confirm the regular review transition still works.

## Validate isolation and import safety

1. Access a card identifier belonging to another learner. The behavior must be the same
   as a missing card: no content or statistics is returned.
2. Re-run the source import after saving a learner edit. Confirm the customized text is
   preserved while the existing card identity, attempts, and schedule remain intact.

Endpoint details and expected error responses are in
[contracts/card-library.openapi.yaml](contracts/card-library.openapi.yaml).

## Validation results (2026-09-07)

- All 82 Maven tests passed, with zero failures, errors, or skips, using Java 21 and
  a temporary PostgreSQL 17 instance. The suite includes actual aggregate queries,
  persisted edits, optimistic-lock rejection, owner isolation, and migration reruns
  on existing rows, including manually corrected legacy cards.
- Browser scenarios passed in headless Chrome at 1440×1000 and 390×844 using 30
  disposable fixture cards. Verified both table pages, zero/known/mixed statistics,
  optional blank values, literal HTML-like text, cancellation without requests,
  validation, two-tab conflicts, and saved content across both entry points.
- In-review saves preserved card ID, reveal state, reviewed/due counts, and enabled
  answer controls. A subsequent answer made exactly one POST and advanced once.
  The dynamically rendered next card could also be edited before reveal.
- Keyboard opening, initial field focus, Escape cancellation, mobile table scrolling,
  editor sizing, and reduced-motion mode passed. Desktop/mobile screenshots were
  inspected. A shared `templates/layout.html` fix keeps the document head inside the
  layout fragment, restoring the stylesheet, page title, and mobile viewport.
- JavaScript syntax checks and `git diff --check` passed. Existing export and review
  regression tests passed. No external PuzzleMovies sign-in was needed for these
  fixture-based checks.

Repository tests use Testcontainers when Docker is available. They can also run
against a dedicated disposable PostgreSQL database:

```powershell
mvn "-Dreview.test.jdbc-url=jdbc:postgresql://127.0.0.1:55436/postgres" "-Dreview.test.username=postgres" test
```

These database tests create and drop their schema; use a test database only. Without
Docker or an explicit test JDBC URL, PostgreSQL-dependent tests are skipped.

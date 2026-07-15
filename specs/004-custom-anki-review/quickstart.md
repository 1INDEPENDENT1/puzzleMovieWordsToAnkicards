# Quickstart: Custom Anki-Style Review Cards

## Prerequisites

- Java 17 is installed.
- PostgreSQL is available and configured as in the current application setup.
- The app has at least one signed-in user with PuzzleMovies session data.

## Run The App

```powershell
mvn spring-boot:run
```

If `mvn` is not on `PATH`, use the local bundled Maven:

```powershell
& 'C:\Users\George\.codex\tools\apache-maven-3.9.11\bin\mvn.cmd' spring-boot:run
```

Open the local app and sign in through the existing PuzzleMovies login flow.

## Prepare Review Cards

1. Open the export menu.
2. Run a combined export so the app has generated vocabulary records with words, phrases, examples, and translations.
3. From the completed export progress page, choose the action to create/refresh review cards.
4. Confirm the app redirects to the review screen.

Note: Review cards are created from application-owned structured export records kept for the completed export while the app process is running. If the app was restarted after an export, run a fresh export before creating cards.

## Review Flow

1. Open `/reviews`.
2. Confirm the front of the card shows the original text and any instance text.
3. Reveal the answer.
4. Confirm the back shows original text, instance text, translation/back content, and lookup actions when available.
5. Press `I know`.
6. Confirm the card leaves with the success sweep transition and the next due card appears.
7. Reveal another answer and press `I don't know`.
8. Confirm the card leaves with the difficulty transition and the next due card appears.
9. Continue until no due cards remain.

## Reduced Motion Check

1. Enable reduced motion in the operating system or browser.
2. Review at least two cards.
3. Confirm answer feedback remains clear without large sweeping movement.

## Scheduling Smoke Checks

- A new card answered `I know` should become due later than an equivalent new card answered `I don't know`.
- A review card answered `I know` repeatedly should receive growing intervals.
- A review card answered `I don't know` should become due again in a short retry window.

## Test Commands

```powershell
mvn test
```

If needed:

```powershell
& 'C:\Users\George\.codex\tools\apache-maven-3.9.11\bin\mvn.cmd' test
```

Recommended focused tests after implementation:

- Review-card creation and duplicate prevention.
- Binary scheduler transitions for new, learning, review, and relearning cards.
- Review attempt persistence.
- Lookup URL encoding for punctuation, spaces, and Cyrillic text.
- Review page and answer endpoint behavior.

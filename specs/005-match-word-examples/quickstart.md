# Quickstart: Match Words With Context Examples

## Prerequisites

- Java 17 and Maven are available.
- PostgreSQL is running with the app's configured datasource.
- A PuzzleMovies user can sign in and has saved words and phrases/examples.

## Validate Automated Behavior

Run the full suite:

```powershell
mvn test
```

The focused checks for this feature should demonstrate:

1. Exact and normalized/inflected matches create contextual drafts with the word on the front and example as the instance.
2. A word's first two source-ordered matches become contextual cards.
3. A third match remains as a standalone example card.
4. One example matching multiple words creates separate contextual cards for each eligible word.
5. An unmatched example becomes a standalone card, including when its translation is blank.
6. An unrelated example never appears as a word card's instance.
7. Re-importing the same completed export does not create duplicate active cards.
8. Existing exporter formatting and review scheduling tests continue to pass.

## End-to-End Review Check

1. Start the app:

   ```powershell
   mvn spring-boot:run
   ```

2. Sign in and run a fresh combined export containing saved words and examples.
3. Create/refresh review cards from that completed export.
4. On `/reviews`, verify a contextual card shows the original word and one matched example before reveal.
5. Reveal the card and verify the available word and example translations appear on the back.
6. Continue until a standalone example appears; verify it shows only the example on the front and only its available translation on the back.
7. Answer cards with both existing scheduling buttons and confirm the next card and counts update without a page reload.
8. Download the same export and confirm its TSV still has two columns and retains the established export layout.

See [data-model.md](data-model.md) for source-to-draft mapping and [contracts/review-card-import.md](contracts/review-card-import.md) for the review interface semantics.

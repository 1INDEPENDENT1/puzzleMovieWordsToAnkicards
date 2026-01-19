# Data Model

## Entities

### AuthToken
- **Represents**: Stored authenticated cookie header string for Puzzle-Movies access.
- **Fields**:
  - `cookieHeader` (string, required)
  - `updatedAt` (timestamp)
- **Relationships**: None (local single-user storage).

### DictionaryWord
- **Represents**: A single word entry from the dictionary.
- **Fields**:
  - `english` (string, required)
  - `russian` (string, optional)
  - `normalizedKey` (string, required; lowercase + punctuation-stripped)
- **Uniqueness**: `normalizedKey` after de-duplication.

### DictionaryPhrase
- **Represents**: A phrase/sentence entry with movie context.
- **Fields**:
  - `english` (string, required)
  - `russian` (string, optional)
  - `movieTitle` (string, optional)
  - `movieUrl` (string, optional)
  - `normalizedTokens` (list of strings)

### PhraseExample
- **Represents**: A contextual example selected for a word.
- **Fields**:
  - `wordKey` (string, required)
  - `phraseEnglish` (string, required)
  - `phraseRussian` (string, optional)
  - `movieTitle` (string, optional)
  - `movieUrl` (string, optional)

### ExportRecord
- **Represents**: A row in the export file.
- **Fields**:
  - `front` (string, required)  # word or phrase
  - `backHtml` (string, required)  # translation + examples
  - `recordType` (enum: word, phrase)

## Relationships

- `DictionaryWord.normalizedKey` maps to up to two `PhraseExample` entries.
- Each `DictionaryPhrase` can appear in multiple `PhraseExample` entries.
- `ExportRecord` is produced from `DictionaryWord` + `PhraseExample` and from standalone `DictionaryPhrase`.

## Validation Rules

- `cookieHeader` must be non-empty and include at least one `name=value` pair.
- `english` fields must be non-empty after trimming.
- `movieUrl` must be absolute HTTPS when present.
- No more than two `PhraseExample` entries per `DictionaryWord`.

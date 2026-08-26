# Data Model: Match Words With Context Examples

This feature introduces transient, application-owned review-source data during a completed export. It does not change PostgreSQL tables, `ReviewCard`, `ReviewAttempt`, scheduling state, or export-file storage.

## Complete Word-to-Example Association

An ordered transient relationship between one deduplicated saved word and one deduplicated saved example.

| Field | Source | Rules |
|---|---|---|
| `wordIdentity` | `DictionaryWord.matchingIdentity` | Required normalized identity used for matching. |
| `wordSourceText` | `DictionaryWord.sourceText` | Required original word text. |
| `wordTranslations` | `DictionaryWord.translations` | Ordered, optional translations. |
| `example` | `DictionaryPhrase` | Required saved example with original text, optional translations, and optional movie context. |
| `sourceOrder` | Deduplicated phrase sequence | Determines the first two retained contexts for each word. |

The association is valid only when the example's normalized matching tokens contain the word identity. An example may have associations with multiple words.

## Generated Review Source

A process-local payload created beside the existing completed-export `ExportRecord` cache. It contains enough structured data to build review drafts without parsing TSV or HTML.

| Collection | Contains | Creation rule |
|---|---|---|
| Words | Deduplicated words selected by export type | Supports word-only cards and contextual-card fronts/backs. |
| Examples | Deduplicated phrases selected by export type | Supports standalone examples and context fields. |
| Complete associations | All ordered valid word/example pairs | Supports contextual selection and overflow detection. |

The payload lives only while the application process retains the completed export. The existing behavior remains: importing cards after a restart requires a fresh export.

## Review Draft Mapping

The existing `ReviewCardDraft` remains the import boundary and maps to the existing `ReviewCard` fields.

| Draft form | `originalText` | `instanceText` | `translationText` | `sourceContext` |
|---|---|---|---|---|
| Word-only | Original word | `null` | Word translations, possibly blank | `null` |
| Contextual | Original word | Original matched example | Word translations followed by example translations; either component may be blank | Example movie/source context when present |
| Standalone example | Original example | `null` | Example translations, possibly blank | Example movie/source context when present |

## Card-Creation Lifecycle

```text
deduplicated words + examples
          |
          v
complete ordered associations
          |
          +--> first two per word ------> contextual drafts (one per word/example pair)
          |
          +--> no association ----------> standalone example draft
          |
          +--> third and later per word -> standalone example draft
          |
          +--> no match for a word -----> word-only draft
          v
existing content-key upsert --> one active card per distinct user study context
```

For an example associated with multiple words, contextual drafts are independently created for each word where that example is in the first two matches. If the same example overflows one or more words, its identical standalone draft is idempotently merged by the existing content key.

## Validation and Invariants

- Original word and example text must remain non-blank after parsing/deduplication.
- Blank translations and missing movie context must not prevent draft creation.
- Only normalized-token associations can produce contextual drafts; unrelated examples never populate `instanceText` for a word.
- No more than two contextual drafts are produced per word, in source order.
- Export records and generated TSV output retain their current two-column format and behavior.
- Existing review-card content-key normalization continues to prevent duplicate active cards on repeat imports.

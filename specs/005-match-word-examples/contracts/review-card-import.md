# Review Card Import Contract: Context Examples

**Applies to**: Existing `POST /reviews/cards` import operation and the existing review-card view returned by `GET /reviews/next` and answer responses.

## Request and Responses

The HTTP interface remains unchanged from [feature 004's review contract](../../004-custom-anki-review/contracts/openapi.yaml):

- `POST /reviews/cards` accepts the optional completed `sourceExportId` and redirects to `/reviews`.
- An unavailable process-local source for an otherwise completed export remains a `409` response.
- `GET /reviews/next` and `POST /reviews/cards/{id}/answer` continue returning `ReviewCardView` values in their existing queue responses.

## Content Semantics

| Card form | `originalText` | `instanceText` | `translationText` |
|---|---|---|---|
| Word-only | Saved word | `null` | Available word translations, or empty |
| Contextual | Saved word | Matched saved example | Available word and example translations, separated for display, or empty |
| Standalone example | Saved example | `null` | Available example translations, or empty |

Clients identify a contextual card by a non-empty `instanceText`. A standalone example must not include an unrelated word label or word translation.

## Creation Semantics

- A saved word receives contextual cards for its first two valid associated examples in source order.
- A saved example can appear in contextual cards for more than one word.
- An unmatched example, or one that overflows at least one word's two-context limit, is also imported as a standalone example card.
- The existing per-user content key preserves one active card for an identical card form on re-import. Different word/example contextual pairs remain distinct because their original and instance texts differ.

## Compatibility

No endpoint, request field, response field, database schema, review state, scheduling rule, or TSV field changes. Existing review clients continue to render `originalText`, optional `instanceText`, and revealed `translationText`.

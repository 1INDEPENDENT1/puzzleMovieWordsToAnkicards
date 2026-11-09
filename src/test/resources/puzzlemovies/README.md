# PuzzleMovies dictionary regression corpus (feature 007)

These are sanitized source specimens, not exports from `DictionaryParser`.
Original user files are unchanged and must not be committed as fixtures.

## Real source provenance

- `words-real.html`: all 36 dictionary rows from the attachment identified in
  `specs/007-fix-dictionary-parsing/spec.md` (original SHA-256
  `c1ed170b89e2d9a6572bb5f8868b0ef5fd197bd52623227368e8a4c2a63d86ef`).
  The attachment lacks section tabs; the matching real tabs from root `words.html`
  were added to make the source section/count explicit. Table/paginator remain
  from the attachment. Thus this is a documented composition of real specimens.
- `phrases-real.html`: all 36 saved phrase rows, section tabs and paginator from
  root `phrases.html` (original SHA-256
  `290381207a261133caaa9e9dd016d97b99d4fca3d7a486b3c47405ffe8534246`).
- Root `words.html` SHA-256:
  `55a24451534f21cd579d5a2e74d5717f1252f275375b4005ffa40827d5308a3c`.
- Both first pages show real source totals WORD=1489 and PHRASE=766. These first
  pages alone are deliberately not a complete traversal.
- `pages/word-real-page-2.html` (36 rows) and `word-real-page-42.html` (13 rows),
  `pages/phrase-real-page-2.html` (**35 observed rows**),
  `phrase-real-page-22.html` (10 rows), and `phrase-real-page-23.html` (0 rows,
  positive total=766: out of range, never an empty dictionary) were captured
  read-only on 2026-11-09 with Java HttpClient. Page identities/last page numbers
  came from the real paginator. `pages/live-provenance.json` records safe source
  URIs, counts and HTTP status. All selected responses retained their requested
  active section and known source table. Missing intermediate pages were not
  invented; no complete live traversal claim is made. In particular PHRASE page2
  has 35 rows, so do not infer a total by multiplying a constant page size.

Authenticated capture kept credentials, cookie headers, authentication responses,
and full page bodies only in process memory. Only dictionary tabs, source table
and paginator were persisted after sanitization. A Python urllib attempt first
failed with URLError; Java HttpClient subsequently succeeded.

## Sanitization

Global profile/header/footer, all scripts, stylesheets, comments, tracking and
media/control elements were excluded. All event handlers, src attributes,
sensitive data attributes and source IDs were stripped; only structural
`data-page` and `id=total_words_in_dict` remain. Public movie/episode hrefs and
pagination hrefs are retained without credentials. Inline `display:none` remains
where it identifies the hidden duplicate; real hidden/visible classes, articles,
`to`, table role classes, inline text nodes and punctuation remain. Whitespace
was not pretty-printed into the inline English spans. No private profile shell
or wholesale raw HTML is stored here.

## Independent expectations

`words-expected.json` and `phrases-expected.json` are ordered arrays of 36 records:

| Field | Meaning |
| --- | --- |
| position | One-based DOM source row position |
| original | Original source text, normalized whitespace only |
| translation | Separate source translation; nullable |
| example / exampleTranslation | Both null on the real WORD specimen; no unobserved subfields invented |
| sourceLabel | Available source label, separately from movie title |
| movieTitle | Available full movie/episode title, nullable |
| movieUrl | Absolute public source URL resolved against https://puzzle-movies.com, nullable |

Expectations were extracted separately with Python BeautifulSoup, without the
production Java parser. All WORD pairs were additionally compared against the
independently authored 36-row spec table. All 72 originals/translations and
available metadata/order were then verified by a second implementation using
Python standard-library HTMLParser on the original sources and sanitized sources.
The complete PHRASE list was reviewed, including literal source oddities such as
`he hadit out...` and incomplete quotations, without editorial corrections.
First WORD pairs: `to mince` / `смягчать`, `a doofus` / `придурок`.
First PHRASE pairs: `Man, heck of a win!` / `Вот это победа!` and
`You don't mince words around here, do you?` /
`А вы вокруг да около не ходите, верно?`; movie `Тачки 3 / Cars 3`,
`https://puzzle-movies.com/films/cars-3-2017`.
WORD row19 deliberately retains literal backslashes before quotation marks.

## Explicitly derived cases

All files containing `derived` below are synthetic edits of the sanitized real
shell; none claim a live account state or an observed inline WORD template.

| Fixture | Derivation / invariant |
| --- | --- |
| `pages/word-derived-page-{1,2,3}.html` | Original WORD rows1–6 split 2/page; selected total6; current1/2/3 with coherent structural links |
| `pages/phrase-derived-page-{1,2,3}.html` | Original PHRASE rows1–6 split 2/page; selected total6; current1/2/3 with coherent structural links |
| `edge-cases/word-zero-derived.html`, `phrase-zero-derived.html` | Approved zero-data case: selected total0, empty known table, current page1, no continuation |
| `edge-cases/words-missing-optional-derived.html` | Two source rows; row1 translation element removed; context remains absent |
| `edge-cases/phrases-missing-optional-derived.html` | Two source rows; row1 translation, source label and movie removed |
| `edge-cases/words-malformed-derived.html` | Two source rows; row1 visible original empty; hidden duplicate remains, to prevent fallback to it |
| `edge-cases/phrases-malformed-derived.html` | Two source rows; row1 original empty |
| `edge-cases/words-inline-example-derived.html` | One source row with synthetic plain inline cell `You mince words, sometimes!`; no fabricated nested translation/movie |
| `edge-cases/words-duplicate-rows-derived.html` | Two independent identical source rows; must count both before semantic dedupe |
| `edge-cases/words-overlap-selectors-derived.html` | One real row additionally marked dictionary__item/entry; must not be reached twice |

No real empty account or nonempty inline WORD context was observed. The explicitly
approved derived zero case and labeled inline case do not remove those limits.

## Verification performed

- Java21 Maven `test-compile`: PASS (exit0) after both support helpers were added.
- Independent HTMLParser comparison: all fields/order on 36 WORD + 36 PHRASE PASS.
- Structural sanitization audit: all 22 HTML resources PASS (no scripts, media,
  event handlers, sensitive data attributes or private IDs).
- No production parser or export service was used to calculate expected values.

Sanitized fixtures also trim trailing layout spaces and redundant final blank lines; internal line breaks and inline text-node boundaries remain intact. Independent field expectations are rechecked after this formatting cleanup.

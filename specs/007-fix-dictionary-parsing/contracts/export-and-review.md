# Export and Review Interface Contract

Applies to existing web endpoints. No new public endpoint, third TSV column, persisted enum value, database column or review-card identity is introduced.

## Source Retrieval Contract

`fetchWordPages(cookieHeader)` and `fetchPhrasePages(cookieHeader)` return `List<DictionaryPage>` or throw a safe categorized source failure. Cookie is used only for the request; it is absent from results/diagnostics.

Each request is `GET /dictionary?item=word|phrase&noredirect=&page=N` at configured PuzzleMovies origin. Parameter order is immaterial; each parameter occurs once. Active tab and row shape must match requested section. The observed `/dictionary/phrases` response is a word dataset and must fail the phrase response check.

Inspection validates recognized shell, selected tab count, current page and structural paginator. Forward links supply page numbers only; no cross-origin href is executed. The next requested page must be current+1, page identity is tracked, source count includes rows later rejected by field parsing. Remove the fixed 50-page truncation. Declared record count is not a traversal bound: the source counter can disagree with returned HTML. Follow structural current+1 continuation, reject repeated page identities/cycles, and stop at the confirmed structural terminal. On terminal page compare aggregate source rows with the stable declared total; a mismatch is a separate durable warning (FR-L07), not a successful FULL result. Changed totals, repeated pages, cycles, unknown structure, absent/contradictory continuation or reaching a safety cap with unresolved continuation fail explicitly.

Non-200, null/blank body, I/O failure and signed-out response are not valid end-of-pagination. Restore thread interrupt on InterruptedException. Map auth/session, HTTP/network, unrecognized structure, inconsistent pagination and changing source to safe separate categories; a 403 alone need not prove expired credentials. Never expose raw response/headers or arbitrary exception messages.

## Parsing Contract

`parseSourceEntries(DictionaryPage)` extracts observed raw fields in source order; it does not derive expected values from normalized identities. `parseWordPages` / `parsePhrasePages` return the generic result in [data-model.md](../data-model.md).

Technical duplicate selectors cannot emit the same row twice. Identical text in independent source rows stays independent until existing semantic dedupe. WORD visible original and own translation are distinct from inline context. PHRASE English/Russian are distinct from source/movie metadata in the first cell.

An absent optional field produces absence, not a rejected record. Missing original in an otherwise recognized source row creates one rejected position. Unknown page layout is a page failure, not zero records. Arbitrary unconfirmed markup is never used as evidence for invented examples or translations.

## GET /exports/{id}/status

Owner/session behavior stays unchanged: unauthenticated → existing 401, foreign/missing job → 404.

Existing fields remain: `id`, `status`, `progressPercent`, `phase`, `startedAt`, `completedAt`, `rowCount`, `errorMessage`.

Add projected fields:

| Field | Contract |
| --- | --- |
| completeness | `FULL` or `PARTIAL` for a valid completed result; null while running/failed |
| unparsedRecordCount | Nonnegative count before dedupe; 0 for FULL; PARTIAL can have 0 only with sourceCountDiscrepancies |
| sourceCountDiscrepancies | Array of section/declaredTotal/retrievedTotal, empty without a source-counter warning |
| warningMessage | Trusted readable partial warning with count; null for FULL |
| downloadAvailable | True only for an owned valid completed job with an existing output file |
| reviewCreationAvailable | True only for an owned valid completed job with current structured review source |

For PARTIAL retain `status=COMPLETED`, `phase=COMPLETED`, `progressPercent=100`; these indicate termination, not completeness. UI must label result as partial and show warning/count before both actions. `rowCount` remains final TSV rows after existing processing. `errorMessage=null` for partial; the internal versioned metadata is never exposed in its place. Invalid completion metadata produces an explicit safe error and disables actions, not FULL.

No raw vocabulary, cookie or password appears in status. Existing clients must be updated alongside this additive DTO contract so that they do not render an ordinary full-success message for PARTIAL.

## GET /exports/{id}

SSR and polling use the same completion projection and availability rules. Render warning and count without relying on JavaScript; then register WarningPresentation in the current session for this job/metadata fingerprint. Dynamic updates insert/update a warning element even when the original running page had no error/warning node. Use text-safe rendering for messages.

PARTIAL completion stops polling as completed work. Expose the usual download link and create-cards form after the warning. No new generic approval dialog is added. An inaccessible review cache does not disable a valid TSV download.

## GET /exports/{id}/download

- Preserve ownership/session checks and existing not-ready 409 behavior.
- Valid FULL uses the existing TSV download response.
- Valid PARTIAL with warning presentation uses the same two-column TSV response containing accepted records after existing dedupe/formatting.
- Valid PARTIAL without presentation redirects with 303 to `/exports/{id}` where the warning/count and usual download action are shown. Do not stream bytes before warning presentation.
- FAILED source fetch, incomplete traversal, invalid metadata or missing file never yields a downloadable partial success.
- Filename and media type remain the existing `.tsv` and `text/tab-separated-values`. No warning row or third column is appended to the data file.

## POST /reviews/cards

Request retains optional `sourceExportId` and existing full-export behavior. For PARTIAL, missing warning presentation redirects with 303 to the owning export progress page without performing import. After the warning has been rendered, the existing form creates cards from accepted app-owned structured source and redirects to `/reviews`.

- Foreign/missing export → 404; failed/not-ready export → existing 409.
- Completed FULL/PARTIAL with lost process-local structured source → existing 409 requiring a fresh export.
- Never reconstruct source from TSV or feed rejected records into card creation.
- Preserve existing first-two matching/source-order semantics, overflow/unmatched behavior, content key, manual overrides and history/scheduling.
- No warning text is inserted into card front/back or identity fields.

## Error and Empty Matrix

| Input | Expected result |
| --- | --- |
| Recognized authorized shell, correct section, total=0, zero rows, no conflicting continuation | Full empty result; zero-data fixture uses real sanitized shell with approved synthetic boundary |
| Signed-out page, even with HTTP 200 | Session/auth failure; no download/import |
| HTTP 503 first/later required page or network failure | Fetch failure with safe section/page/status if known; no download/import |
| Unknown HTML, blank body, positive total with zero source rows | Explicit fetch/structure/pagination failure; never empty success |
| Recognized pages with a mix of accepted/rejected records | PARTIAL with exact rejected count; both actions after warning |
| Every source record rejected | Parse failure; not a correctly empty dictionary |
| Normalized duplicates | Existing semantic merge; no change to rules; count is not mistaken for parse rejection |

## Compatibility Boundary

Keep current routes, authentication permissions, session ownership checks, export types, TSV shape and review answer contracts. WarningPresentation is per-job presentation bookkeeping, not a new authentication mechanism. Metadata stays in existing PostgreSQL ExportJob storage; no migration or card data repair is part of this feature.

## Approved counter mismatch extension — 2026-11-09

Status additionally exposes `sourceCountDiscrepancies: [{section, declaredTotal, retrievedTotal}]`. PARTIAL may have unparsedRecordCount=0 when this array is nonempty. The trusted warning contains each section and both counts before either action; server presentation and owner/cache rules are unchanged. Source/network/auth/shape failures and changed totals still fail. A stable mismatch after successful traversal is allowed by the user's explicit answer; it is never counted as rejected parser rows.

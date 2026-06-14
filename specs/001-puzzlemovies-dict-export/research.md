# Research

## Decision: Use Java HttpClient for HTTPS requests
**Rationale**: Built into Java 17+, supports HTTPS and header management without extra runtime dependencies.
**Alternatives considered**: Apache HttpClient, OkHttp.

## Decision: Parse HTML with Jsoup selectors
**Rationale**: Provides reliable CSS selector parsing for the specified dictionary HTML structure.
**Alternatives considered**: HtmlUnit, regex-based parsing.

## Decision: Store auth token as a single-line cookie header file
**Rationale**: Aligns with the requirement to persist only session cookies and keep credentials ephemeral.
**Alternatives considered**: Encrypted keystore, OS keychain, environment variables.

## Decision: Pagination terminates on empty rows or non-200 responses
**Rationale**: Matches the dictionary page contract and avoids infinite loops on redirects or empty pages.
**Alternatives considered**: Fixed page limit, cursor-based pagination.

## Decision: Word-to-phrase matching uses normalized token index with a two-example cap
**Rationale**: Supports contextual examples while limiting export size and keeping Anki rows manageable.
**Alternatives considered**: Unlimited matches, single best match only.

## Decision: Use local lingua-core library for lemmatization
**Rationale**: Already implemented, tested, and available as a local Maven dependency. Avoids reimplementing linguistic logic.
**Alternatives considered**: Raw token matching, third-party NLP libraries.
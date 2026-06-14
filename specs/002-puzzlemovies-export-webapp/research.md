# Phase 0 Research: PuzzleMovies Export Web App

## Decisions

### Decision: Use Spring Data JPA with PostgreSQL for persistence
- Rationale: Required by spec; provides transactional safety and entity lifecycle management for users, sessions, and export jobs.
- Alternatives considered: JDBC/manual SQL (forbidden by spec), H2/in-memory (forbidden by spec).

### Decision: Run exports asynchronously with a Spring-managed task executor
- Rationale: Keeps UI responsive while long-running exports occur; allows progress updates via database state.
- Alternatives considered: Inline synchronous processing (blocks request), Quartz/scheduler (heavier than needed), external job queue (out of scope for single-app deployment).

### Decision: Store export output on disk and persist file metadata on ExportJob
- Rationale: Large TSV files are better stored on disk than in the database; simple download controller can stream files.
- Alternatives considered: Storing blobs in PostgreSQL (heavier, increases DB size), generating on-demand for each download (recomputes and delays users).

### Decision: Use a polling status endpoint for progress updates
- Rationale: Minimal complexity while still providing live updates; works with server-rendered pages and periodic refresh.
- Alternatives considered: WebSocket/SSE (more complex infrastructure), client-side SPA polling (not required by spec).

### Decision: Authenticate and scrape PuzzleMovies using Java HttpClient + Jsoup
- Rationale: HttpClient handles session cookies; Jsoup simplifies HTML parsing for words/phrases pages.
- Alternatives considered: Jsoup-only requests (less control over cookies and headers), browser automation (explicitly forbidden).

### Decision: Use Testcontainers for PostgreSQL integration tests
- Rationale: Ensures tests run against PostgreSQL while complying with “no H2/in-memory” constraint; matches production behavior.
- Alternatives considered: H2 (forbidden), mocking repositories only (misses DB integration risks).

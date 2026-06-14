# Feature Specification: PuzzleMovies Export Web App

**Feature Branch**: `002-puzzlemovies-export-webapp`  
**Created**: 2026-01-19  
**Status**: Draft  
**Input**: User description: "Application Framework (MANDATORY) - Existing CLI exporter is baseline; migrate to Spring Boot web app - The application MUST be a Spring Boot 3.x web application. - Java 17 - MUST: use lingua-core (org.drugov:lingua-core) - MUST NOT: final deliverable as CLI - Build tool: Maven - Packaging: executable JAR - The application MUST start via `mvn spring-boot:run` and `java -jar target/*.jar`. - CLI-only tools are FORBIDDEN (no `Main`-driven console app as the primary interface). Web stack - Use Spring MVC (controllers with @Controller/@RestController). - Provide server-side UI pages (Thymeleaf preferred) for: - Login (email + password for puzzle-movies.com) - Export menu (words / phrases / combined) - Export progress page - Download endpoint for TSV Persistence (MANDATORY) - Database: PostgreSQL (only) - ORM: Spring Data JPA (Hibernate) - H2/in-memory databases are FORBIDDEN. - JDBC/manual SQL access is FORBIDDEN unless explicitly required. - The application MUST NOT start without a PostgreSQL connection. Minimum entities - User - PuzzleSessionToken (stores cookie header string only; never store or log password) - ExportJob (status, progress, timestamps, output file reference)"

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Connect PuzzleMovies Account (Priority: P1)

As a user, I can sign in with my puzzle-movies.com email and password to connect my account, so I can export my words and phrases without re-entering credentials each time.

**Why this priority**: Users cannot export anything until their PuzzleMovies account is connected.

**Independent Test**: Can be fully tested by submitting valid/invalid credentials and verifying that a session is stored without storing the password.

**Acceptance Scenarios**:

1. **Given** I am on the login page, **When** I submit valid puzzle-movies.com credentials, **Then** I am redirected to the export menu and my session is stored for future use.
2. **Given** I am on the login page, **When** I submit invalid credentials, **Then** I see the failure message returned by puzzle-movies.com and no session is stored.
3. **Given** puzzle-movies.com responds without confirming a completed sign-in, **When** I submit credentials, **Then** I remain on the login page, see guidance to resolve the account issue, and no session is stored.

---

### User Story 2 - Start and Track an Export (Priority: P2)

As a user, I can choose what to export (words, phrases, or combined) and start an export job while seeing its progress, so I know when the file will be ready.

**Why this priority**: Starting and tracking the export is the core value of the application once connected.

**Independent Test**: Can be tested by starting an export and verifying that a job is created with visible status and progress updates.

**Acceptance Scenarios**:

1. **Given** my account is connected, **When** I select an export type and start the export, **Then** a new export job starts and I see a progress page.
2. **Given** an export is in progress, **When** I refresh the progress page, **Then** I see the latest status and progress without restarting the export.

---

### User Story 3 - Download Export File (Priority: P3)

As a user, I can download the completed export as a TSV file, so I can import it into other tools.

**Why this priority**: The export has no value without a downloadable file.

**Independent Test**: Can be tested by completing an export and verifying the TSV download works.

**Acceptance Scenarios**:

1. **Given** an export job is complete, **When** I click download, **Then** a TSV file is downloaded.

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right edge cases.
-->

- What happens when stored session information is expired or invalid?
- How does the system handle a failed export due to connectivity issues with puzzle-movies.com?
- What happens if a user starts a new export while another export is still running?
- How does the system handle very large exports that take a long time?
- What happens when puzzle-movies.com returns a response body that does not confirm a usable signed-in session?

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

- **FR-001**: The system MUST present a login page that accepts a puzzle-movies.com email and password.
- **FR-002**: The system MUST authenticate the provided credentials with puzzle-movies.com and establish a session for the user only when the sign-in response confirms a completed login and a usable session token.
- **FR-003**: The system MUST store only the session token needed to access puzzle-movies.com and MUST NOT store or display the user’s password.
- **FR-003a**: The system MUST stop the login flow and keep the user on the login page when puzzle-movies.com reports a sign-in failure or returns a response that does not confirm a completed login.
- **FR-003b**: The system MUST display the failure message returned by puzzle-movies.com when one is provided, and otherwise show a clear recovery hint that the user may need to resolve the account directly in puzzle-movies.com before retrying.
- **FR-004**: The system MUST provide an export menu with options: words, phrases, and combined.
- **FR-005**: The system MUST allow a user to start an export job from the menu and view a progress page for that job.
- **FR-006**: The system MUST display export job status, progress, start time, and completion time on the progress page.
- **FR-007**: The system MUST make a completed export downloadable as a TSV file.
- **FR-008**: The system MUST prevent download of an export that has not completed and provide a clear message explaining why.
- **FR-009**: The system MUST persist users, sessions, and export jobs so they are available after a restart.
- **FR-010**: The system MUST refuse to operate if persistent storage is unavailable at startup.

### Key Entities *(include if feature involves data)*

- **User**: Represents a person using the app; includes email and timestamps.
- **PuzzleSessionToken**: Stores the session token string for puzzle-movies.com and its association to a user; includes timestamps and optional expiration.
- **ExportJob**: Represents an export request by a user; includes type (words/phrases/combined), status, progress, timestamps, and a reference to the output file.

## Assumptions

- Each TSV row represents a single exported word or phrase, with enough fields to distinguish type and original text.
- Users primarily run one export at a time; if multiple exports are allowed, each is tracked independently.
- Standard web usability and accessibility expectations apply (clear error messages, form validation, and consistent navigation).

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

- **SC-001**: At least 90% of users can connect their PuzzleMovies account on the first attempt without support.
- **SC-002**: At least 90% of export jobs complete successfully for typical accounts within 10 minutes.
- **SC-003**: At least 95% of completed exports are successfully downloaded on the first try.
- **SC-004**: Users can start an export within 2 minutes of arriving at the export menu.

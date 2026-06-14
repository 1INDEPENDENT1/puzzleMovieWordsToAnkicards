# Phase 1 Data Model: PuzzleMovies Export Web App

## Entities

### User
- Fields:
  - id (UUID or BIGINT, primary key)
  - email (string, unique, required)
  - createdAt (timestamp, required)
  - updatedAt (timestamp, required)
- Relationships:
  - one-to-one with PuzzleSessionToken
  - one-to-many with ExportJob
- Validation:
  - email required, valid format, unique

### PuzzleSessionToken
- Fields:
  - id (UUID or BIGINT, primary key)
  - userId (FK -> User.id, required)
  - cookieHeader (string, required)  # raw Cookie header string
  - expiresAt (timestamp, optional)
  - createdAt (timestamp, required)
  - updatedAt (timestamp, required)
- Relationships:
  - many-to-one with User
- Validation:
  - cookieHeader required, non-empty

### ExportJob
- Fields:
  - id (UUID or BIGINT, primary key)
  - userId (FK -> User.id, required)
  - type (enum: WORDS, PHRASES, COMBINED, required)
  - status (enum: PENDING, RUNNING, COMPLETED, FAILED, required)
  - progressPercent (integer 0-100, required)
  - startedAt (timestamp, optional)
  - completedAt (timestamp, optional)
  - errorMessage (string, optional)
  - outputFilePath (string, optional)
  - outputFileName (string, optional)
  - rowCount (integer, optional)
  - createdAt (timestamp, required)
  - updatedAt (timestamp, required)
- Relationships:
  - many-to-one with User
- Validation:
  - progressPercent between 0 and 100
  - outputFilePath required when status is COMPLETED
  - completedAt required when status is COMPLETED or FAILED

## State Transitions

- ExportJob status:
  - PENDING -> RUNNING
  - RUNNING -> COMPLETED
  - RUNNING -> FAILED

## Notes

- Passwords are never stored or logged.
- Session token is stored as a cookie header string only.

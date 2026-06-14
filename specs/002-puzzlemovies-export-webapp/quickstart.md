# Quickstart: PuzzleMovies Export Web App

## Prerequisites

- Java 17
- Maven 3.9+
- PostgreSQL 13+

## Configure Database

Create a database and user in PostgreSQL, then set environment variables:

- `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/puzzlemovies`
- `SPRING_DATASOURCE_USERNAME=puzzlemovies`
- `SPRING_DATASOURCE_PASSWORD=your_password`

Optional export directory:

- `EXPORT_OUTPUT_DIR=/absolute/path/to/exports`

The application will fail fast if the PostgreSQL connection details are missing or invalid.

## Run

From the repository root:

```bash
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn -DskipTests package
java -jar target/*.jar
```

## Open the App

Visit `http://localhost:8080/login` in your browser and sign in with your puzzle-movies.com credentials.

# Quickstart

## Prerequisites

- Java 17+
- Maven
- Network access to https://puzzle-movies.com

## Run (local CLI)

```bash
# Example usage
java -jar puzzle-movies-exporter.jar \
  --email user@example.com \
  --password "********" \
  --token-file ./puzzle_token.txt \
  --format tsv \
  --output ./puzzle-export.tsv
```

## Help

```bash
java -jar puzzle-movies-exporter.jar --help
```

## Token Reuse

If `--token-file` exists, the tool will attempt to use it first. On auth failure, it will prompt for credentials and overwrite the token file.

## Output

- A single TSV or CSV file containing combined word and phrase entries
- Words include up to two contextual examples in the back field (HTML)
- Phrases are included as standalone entries
- Missing translations are exported as blank fields

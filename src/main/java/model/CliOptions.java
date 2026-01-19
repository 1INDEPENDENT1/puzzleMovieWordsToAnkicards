package model;

import java.nio.file.Path;
import java.util.Locale;

public record CliOptions(
        String email,
        String password,
        Path tokenFile,
        ExportFormat format,
        Path outputFile
) {
    public enum ExportFormat {
        TSV,
        CSV;

        public static ExportFormat fromString(String value) {
            if (value == null) {
                return TSV;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "csv" -> CSV;
                case "tsv" -> TSV;
                default -> throw new IllegalArgumentException("Unsupported format: " + value);
            };
        }
    }
}

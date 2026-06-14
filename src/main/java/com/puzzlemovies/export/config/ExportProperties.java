package com.puzzlemovies.export.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "export")
public class ExportProperties {
    @NotBlank
    private String outputDir;

    private final PuzzleMovies puzzleMovies = new PuzzleMovies();

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public PuzzleMovies getPuzzleMovies() {
        return puzzleMovies;
    }

    public static class PuzzleMovies {
        @NotBlank
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}

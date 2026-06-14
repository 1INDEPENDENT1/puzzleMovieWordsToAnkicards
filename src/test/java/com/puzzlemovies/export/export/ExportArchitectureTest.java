package com.puzzlemovies.export.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ExportArchitectureTest {
    @Test
    void obsoleteTopLevelExportPackagesAreAbsent() {
        List<Path> obsoletePaths = List.of(
                Path.of("src/main/java/cli"),
                Path.of("src/main/java/http"),
                Path.of("src/main/java/parser"),
                Path.of("src/main/java/export"),
                Path.of("src/main/java/model"),
                Path.of("src/main/java/util"));

        for (Path path : obsoletePaths) {
            assertFalse(Files.exists(path), path + " should not remain as active production source");
        }
    }
}

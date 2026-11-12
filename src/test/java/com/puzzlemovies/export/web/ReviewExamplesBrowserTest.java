package com.puzzlemovies.export.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "REVIEW_BROWSER", matches = ".+")
class ReviewExamplesBrowserTest {
    @Test
    void rendersPagedExamplesAndKeepsTranslationsWithinTheirPresentation(@TempDir Path directory) throws Exception {
        var resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var servletContext = new MockServletContext();
        var exchange = JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
        for (int count : List.of(0, 100)) {
            var context = new WebContext(exchange, Locale.ENGLISH, Map.of(
                    "card", new FixtureCard("card", 1, "Give up", "", "сдаваться", null, "NEW",
                            List.of(), count, "initial", 1),
                    "counts", new FixtureCounts(0, 1, null)));
            Files.writeString(directory.resolve("review-" + count + ".html"), engine.process("reviews", context));
        }
        var library = new PageImpl<>(List.of(new FixtureLibraryItem("card", 1, "Give up", "", "сдаваться",
                4, 3, 1, 100)));
        Files.writeString(directory.resolve("cards.html"), engine.process("cards",
                new WebContext(exchange, Locale.ENGLISH, Map.of("library", library))));

        Path log = directory.resolve("browser.log");
        var runner = new ProcessBuilder("node", "src/test/resources/browser/review-examples.mjs",
                directory.toString(), System.getenv("REVIEW_BROWSER")).redirectErrorStream(true)
                .redirectOutput(log.toFile()).start();
        try {
            assertTrue(runner.waitFor(60, TimeUnit.SECONDS), () -> "Browser timed out: " + read(log));
            assertEquals(0, runner.exitValue(), () -> read(log));
        } finally {
            runner.destroyForcibly();
        }
    }

    private String read(Path log) {
        try {
            return Files.readString(log);
        } catch (java.io.IOException error) {
            return error.toString();
        }
    }

    public record FixtureCard(String id, long version, String originalText, String instanceText,
                              String translationText, String sourceContext, String state,
                              List<Object> lookupActions, int exampleCount,
                              String presentationId, long effectiveContentVersion) {
    }
    public record FixtureCounts(int reviewedCount, int remainingDueCount, java.time.Instant nextDueAt) {}

    public record FixtureLibraryItem(String id, long version, String originalText, String instanceText,
                                     String translationText, long totalAnswers, long correctAnswers,
                                     long incorrectAnswers, int exampleCount) {
        public double correctAnswerPercentage() {
            return 75.0;
        }
    }
}

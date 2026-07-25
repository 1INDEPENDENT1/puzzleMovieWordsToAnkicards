package com.puzzlemovies.export.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewStaticAssetsTest {
    @Test
    void reviewJavaScriptSupportsRevealAnswerRenderingAndRecovery() throws Exception {
        String js = Files.readString(Path.of("src/main/resources/static/js/reviews.js"));

        assertTrue(js.contains("reveal-button"));
        assertTrue(js.contains("answer was not saved") || js.contains("Answer was not saved"));
        assertTrue(js.contains("answered-known"));
        assertTrue(js.contains("answered-unknown"));
    }

    @Test
    void reviewCssContainsReducedMotionAndAnswerStateHooks() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/static/css/app.css"));

        assertTrue(css.contains("prefers-reduced-motion"));
        assertTrue(css.contains(".review-card.answered-known"));
        assertTrue(css.contains(".review-card.answered-unknown"));
    }

    @Test
    void reviewTemplateContainsProgressAriaAndAnswerHooks() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/reviews.html"));

        assertTrue(html.contains("aria-live"));
        assertTrue(html.contains("reviewed-count"));
        assertTrue(html.contains("remaining-count"));
        assertTrue(html.contains("data-answer=\"UNKNOWN\""));
        assertTrue(html.contains("data-answer=\"KNOWN\""));
    }
}

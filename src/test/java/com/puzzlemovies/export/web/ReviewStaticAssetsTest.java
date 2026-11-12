package com.puzzlemovies.export.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewStaticAssetsTest {
    @Test
    void sharedLayoutRetainsStylesViewportAndPageTitle() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/layout.html"));
        assertTrue(html.contains("<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\" th:fragment=\"layout(title, content)\""));
        assertTrue(html.contains("th:replace=\"${title}\""));
        assertTrue(html.contains("name=\"viewport\""));
        assertTrue(html.contains("@{/css/app.css}"));
    }

    @Test
    void currentCardEditorOffersOnlyWordFields() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/reviews.html"));
        assertTrue(html.contains("data-version"));
        assertTrue(html.contains("data-edit-current"));
        assertTrue(html.contains("id=\"review-editor\""));
        assertFalse(html.contains("name=\"instanceText\""));
    }

    @Test
    void libraryEditorHasAccessibleFieldsCancelAndConflictFeedback() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/cards.html"));
        String js = Files.readString(Path.of("src/main/resources/static/js/cards.js"));
        assertTrue(html.contains("<dialog"));
        assertTrue(html.contains("name=\"originalText\""));
        assertTrue(html.contains("name=\"translationText\""));
        assertFalse(html.contains("name=\"instanceText\""));
        assertTrue(html.contains("data-edit-card"));
        assertTrue(html.contains("data-edit-examples"));
        assertTrue(html.contains("id=\"cancel-edit\""));
        assertTrue(js.contains("method: 'PATCH'"));
        assertTrue(js.contains("response.status === 409"));
        assertTrue(js.contains("dialog.close()"));
        assertTrue(js.contains("textContent"));
    }

    @Test
    void libraryIncludesReadableStatisticsAndPageNavigation() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/cards.html"));
        assertTrue(html.contains("<table"));
        assertTrue(html.contains("No translation saved"));
        assertTrue(html.contains("item.exampleCount()"));
        assertTrue(html.contains("No answers yet"));
        assertTrue(html.contains("totalAnswers()"));
        assertTrue(html.contains("correctAnswers()"));
        assertTrue(html.contains("incorrectAnswers()"));
        assertTrue(html.contains("correctAnswerPercentage()"));
        assertTrue(html.contains("aria-label=\"Card library pages\""));
    }

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
        assertTrue(html.contains("card.exampleCount()"));
        assertTrue(html.contains("card.presentationId()"));
        assertTrue(html.contains("aria-labelledby=\"review-examples-title\""));
    }
}

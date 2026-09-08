package com.puzzlemovies.export.web;

import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.export.CompleteWordExampleMatches;
import com.puzzlemovies.export.export.DictionaryPhrase;
import com.puzzlemovies.export.export.DictionaryWord;
import com.puzzlemovies.export.export.PhraseExample;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.ExportType;
import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.review.ReviewCardDraftFactory;
import com.puzzlemovies.export.review.ReviewCardDraft;
import com.puzzlemovies.export.review.ReviewExportSource;
import com.puzzlemovies.export.review.ReviewTestFixtures;
import com.puzzlemovies.export.service.ExportService;
import com.puzzlemovies.export.service.ReviewCardImportService;
import com.puzzlemovies.export.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ReviewControllerTest {
    @Test
    void contentPatchReturnsVersionedCardWithoutAnsweringOrAdvancingSession() throws Exception {
        User user = ReviewTestFixtures.user("edit@example.com");
        TestContext context = context(user);
        var card = ReviewWebTestFixtures.editableCard(user, 5);
        var cardView = new ReviewService(null, null, null).toView(card);
        when(context.reviewService.updateContent(eq(user), eq(card.getId()), any()))
                .thenReturn(new ReviewDtos.CardContentUpdateResponse(cardView));
        var session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("reviewedCount", 7);
        context.mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/cards/{id}", card.getId())
                        .session(session).contentType("application/json").content(ReviewWebTestFixtures.contentUpdate(4)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.card.id").value(card.getId().toString()))
                .andExpect(jsonPath("$.card.version").value(5));
        assertEquals(7, session.getAttribute("reviewedCount"));
        verify(context.reviewService).updateContent(user, card.getId(),
                new ReviewDtos.CardContentUpdateRequest("Walk", "Walk home.", "идти", 4L));
        org.mockito.Mockito.verifyNoMoreInteractions(context.reviewService);
    }

    @Test
    void contentPatchMapsValidationOwnershipAndConcurrencyErrors() throws Exception {
        var user = ReviewTestFixtures.user("edit@example.com");
        var context = context(user);
        var id = UUID.randomUUID();
        var patch = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/cards/{id}", id)
                .contentType("application/json").content(ReviewWebTestFixtures.contentUpdate(0));
        when(context.reviewService.updateContent(eq(user), eq(id), any()))
                .thenThrow(new ReviewService.InvalidCardContentException("Original text is required"))
                .thenThrow(new ReviewService.CardNotFoundException())
                .thenThrow(new ReviewService.StaleCardException());
        context.mvc.perform(patch).andExpect(status().isBadRequest());
        context.mvc.perform(patch).andExpect(status().isNotFound());
        context.mvc.perform(patch).andExpect(status().isConflict());
        context(null).mvc.perform(patch).andExpect(status().isUnauthorized());
        context.mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/cards/{id}", id)
                .contentType("application/json").content("{malformed"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void libraryIsOwnerScopedAndAcceptsOnlyNonnegativePages() throws Exception {
        User user = ReviewTestFixtures.user("library@example.com");
        TestContext context = context(user);
        var page = new org.springframework.data.domain.PageImpl<ReviewDtos.CardLibraryItem>(List.of());
        when(context.reviewService.cardLibrary(user, 1)).thenReturn(page);
        context.mvc.perform(get("/cards").param("page", "1"))
                .andExpect(status().isOk()).andExpect(view().name("cards"))
                .andExpect(model().attribute("library", page));
        verify(context.reviewService).cardLibrary(user, 1);
        context.mvc.perform(get("/cards").param("page", "-1")).andExpect(status().isBadRequest());
        context.mvc.perform(get("/cards").param("page", "abc")).andExpect(status().isBadRequest());
        context(null).mvc.perform(get("/cards")).andExpect(status().isUnauthorized());
    }

    @Test
    void getReviewsRendersDueCardAndCounts() throws Exception {
        User user = ReviewTestFixtures.user("learner@example.com");
        TestContext context = context(user);
        ReviewDtos.ReviewQueueResponse queue = queueWithCard();
        when(context.reviewService.nextDueCard(eq(user), eq(0))).thenReturn(queue);

        context.mvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews"))
                .andExpect(model().attribute("card", queue.card()))
                .andExpect(model().attribute("counts", queue.counts()));
    }

    @Test
    void postCardsCreatesReviewCardsFromCompletedExportAndRedirects() throws Exception {
        User user = ReviewTestFixtures.user("learner@example.com");
        TestContext context = context(user);
        ExportJob job = new ExportJob();
        job.setId(UUID.randomUUID());
        job.setUser(user);
        job.setType(ExportType.COMBINED);
        job.setStatus(ExportStatus.COMPLETED);
        DictionaryWord word = new DictionaryWord("Run", Set.of("бежать"), "run", false);
        DictionaryPhrase phrase = new DictionaryPhrase("I am running home.", Set.of("Я бегу домой."),
                "Arrival", null, Set.of("run"), "i am run home", false);
        PhraseExample example = new PhraseExample("run", phrase.sourceText(), phrase.translations(), phrase.movieTitle(), phrase.movieUrl());
        ReviewExportSource source = new ReviewExportSource(List.of(word), List.of(phrase),
                new CompleteWordExampleMatches(Map.of("run", List.of(example))));
        when(context.exportJobRepository.findByIdAndUser(job.getId(), user)).thenReturn(Optional.of(job));
        when(context.exportService.generatedReviewSourceForCompletedExport(user, job.getId())).thenReturn(Optional.of(source));
        when(context.importService.importDrafts(eq(user), any()))
                .thenReturn(new ReviewCardImportService.ImportResult(1, 0));

        context.mvc.perform(post("/reviews/cards").param("sourceExportId", job.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"));

        ArgumentCaptor<List<ReviewCardDraft>> drafts = ArgumentCaptor.forClass(List.class);
        verify(context.importService).importDrafts(eq(user), drafts.capture());
        assertEquals("Run", drafts.getValue().get(0).originalText());
        assertEquals("I am running home.", drafts.getValue().get(0).instanceText());
    }

    @Test
    void nextEndpointReturnsQueueJson() throws Exception {
        User user = ReviewTestFixtures.user("learner@example.com");
        TestContext context = context(user);
        when(context.reviewService.nextDueCard(eq(user), eq(0))).thenReturn(queueWithCard());

        context.mvc.perform(get("/reviews/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.originalText").value("Run"))
                .andExpect(jsonPath("$.counts.remainingDueCount").value(1));
    }

    @Test
    void answerEndpointReturnsSchedulingResult() throws Exception {
        User user = ReviewTestFixtures.user("learner@example.com");
        TestContext context = context(user);
        UUID cardId = UUID.randomUUID();
        ReviewDtos.ReviewAnswerResponse response = new ReviewDtos.ReviewAnswerResponse(
                new ReviewDtos.SchedulingResult(
                        cardId,
                        ReviewAnswer.KNOWN,
                        com.puzzlemovies.export.model.ReviewCardState.NEW,
                        com.puzzlemovies.export.model.ReviewCardState.REVIEW,
                        Instant.parse("2026-07-17T10:00:00Z"),
                        1),
                null,
                new ReviewDtos.ReviewCounts(1, 0, null));
        when(context.reviewService.answerCard(eq(user), eq(cardId), eq(ReviewAnswer.KNOWN), eq(50L), eq(0)))
                .thenReturn(response);

        context.mvc.perform(post("/reviews/cards/{id}/answer", cardId)
                        .contentType("application/json")
                        .content("{\"answer\":\"KNOWN\",\"responseMillis\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.answer").value("KNOWN"))
                .andExpect(jsonPath("$.counts.reviewedCount").value(1));
    }

    @Test
    void answerEndpointMapsInvalidWrongOwnerAndStaleStates() throws Exception {
        User user = ReviewTestFixtures.user("learner@example.com");
        TestContext context = context(user);
        UUID invalid = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        UUID stale = UUID.randomUUID();
        when(context.reviewService.answerCard(eq(user), eq(invalid), any(), any(), eq(0)))
                .thenThrow(new ReviewService.InvalidAnswerException());
        when(context.reviewService.answerCard(eq(user), eq(missing), any(), any(), eq(0)))
                .thenThrow(new ReviewService.CardNotFoundException());
        when(context.reviewService.answerCard(eq(user), eq(stale), any(), any(), eq(0)))
                .thenThrow(new ReviewService.CardNotAnswerableException());

        context.mvc.perform(post("/reviews/cards/{id}/answer", invalid)
                        .contentType("application/json")
                        .content("{\"answer\":\"KNOWN\"}"))
                .andExpect(status().isBadRequest());
        context.mvc.perform(post("/reviews/cards/{id}/answer", missing)
                        .contentType("application/json")
                        .content("{\"answer\":\"KNOWN\"}"))
                .andExpect(status().isNotFound());
        context.mvc.perform(post("/reviews/cards/{id}/answer", stale)
                        .contentType("application/json")
                        .content("{\"answer\":\"KNOWN\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reviewModelSupportsBlankTranslationsLongInstancesPhraseCardsAndLookupActions() throws Exception {
        User user = ReviewTestFixtures.user("learner@example.com");
        TestContext context = context(user);
        ReviewDtos.ReviewCardView card = new ReviewDtos.ReviewCardView(
                UUID.randomUUID(),
                "The moon is bright.",
                "A very long sentence that should stay inside the review card without overlapping controls.",
                "",
                "Moon",
                com.puzzlemovies.export.model.ReviewCardState.NEW,
                Instant.now(),
                List.of(new ReviewDtos.LookupAction(
                        ReviewDtos.LookupActionType.INSTANCE_TRANSLATION,
                        "Translate sentence",
                        "https://translate.google.com/?text=A%20very%20long%20sentence",
                        "A very long sentence")), 0);
        ReviewDtos.ReviewQueueResponse queue = new ReviewDtos.ReviewQueueResponse(
                card,
                new ReviewDtos.ReviewCounts(0, 1, null));
        when(context.reviewService.nextDueCard(eq(user), eq(0))).thenReturn(queue);

        context.mvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("card", card));
    }

    @Test
    void reviewTemplateIncludesProgressAriaAndAnswerHooks() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/reviews.html"));

        org.junit.jupiter.api.Assertions.assertTrue(html.contains("aria-live"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("reviewed-count"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("remaining-count"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("data-answer=\"UNKNOWN\""));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("data-answer=\"KNOWN\""));
    }

    @Test
    void endpointsRejectUnsignedUsers() throws Exception {
        TestContext context = context(null);

        context.mvc.perform(get("/reviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCardsRejectsMissingIncompleteOrUnavailableExports() throws Exception {
        User user = ReviewTestFixtures.user("learner@example.com");
        TestContext context = context(user);
        UUID missingId = UUID.randomUUID();
        UUID runningId = UUID.randomUUID();
        UUID unavailableId = UUID.randomUUID();
        ExportJob running = new ExportJob();
        running.setId(runningId);
        running.setUser(user);
        running.setType(ExportType.COMBINED);
        running.setStatus(ExportStatus.RUNNING);
        ExportJob completed = new ExportJob();
        completed.setId(unavailableId);
        completed.setUser(user);
        completed.setType(ExportType.COMBINED);
        completed.setStatus(ExportStatus.COMPLETED);
        when(context.exportJobRepository.findByIdAndUser(missingId, user)).thenReturn(Optional.empty());
        when(context.exportJobRepository.findByIdAndUser(runningId, user)).thenReturn(Optional.of(running));
        when(context.exportJobRepository.findByIdAndUser(unavailableId, user)).thenReturn(Optional.of(completed));
        when(context.exportService.generatedReviewSourceForCompletedExport(user, unavailableId)).thenReturn(Optional.empty());

        context.mvc.perform(post("/reviews/cards").param("sourceExportId", missingId.toString()))
                .andExpect(status().isNotFound());
        context.mvc.perform(post("/reviews/cards").param("sourceExportId", runningId.toString()))
                .andExpect(status().isConflict());
        context.mvc.perform(post("/reviews/cards").param("sourceExportId", unavailableId.toString()))
                .andExpect(status().isConflict());
    }

    private ReviewDtos.ReviewQueueResponse queueWithCard() {
        return new ReviewDtos.ReviewQueueResponse(
                new ReviewDtos.ReviewCardView(
                        UUID.randomUUID(),
                        "Run",
                        "I am running home.",
                        "бежать",
                        "Arrival",
                        com.puzzlemovies.export.model.ReviewCardState.NEW,
                        Instant.now(),
                        List.of(), 0),
                new ReviewDtos.ReviewCounts(0, 1, null));
    }

    private TestContext context(User user) {
        ReviewService reviewService = mock(ReviewService.class);
        ExportService exportService = mock(ExportService.class);
        ExportJobRepository exportJobRepository = mock(ExportJobRepository.class);
        ReviewCardImportService importService = mock(ReviewCardImportService.class);
        SessionUserResolver resolver = mock(SessionUserResolver.class);
        when(resolver.resolve(any())).thenReturn(Optional.ofNullable(user));
        ReviewController controller = new ReviewController(
                reviewService,
                new ReviewCardDraftFactory(),
                importService,
                exportService,
                exportJobRepository,
                resolver);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/templates/");
        viewResolver.setSuffix(".html");
        return new TestContext(
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setViewResolvers(viewResolver)
                        .build(),
                reviewService,
                exportService,
                exportJobRepository,
                importService);
    }

    private record TestContext(MockMvc mvc,
                               ReviewService reviewService,
                               ExportService exportService,
                               ExportJobRepository exportJobRepository,
                               ReviewCardImportService importService) {
    }
}

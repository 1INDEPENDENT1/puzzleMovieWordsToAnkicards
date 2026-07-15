package com.puzzlemovies.export.web;

import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.ExportType;
import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.review.ReviewCardDraftFactory;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ReviewControllerTest {
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
        List<ExportRecord> records = List.of(ReviewTestFixtures.wordRecordWithExample());
        when(context.exportJobRepository.findByIdAndUser(job.getId(), user)).thenReturn(Optional.of(job));
        when(context.exportService.generatedRecordsForCompletedExport(user, job.getId())).thenReturn(Optional.of(records));
        when(context.importService.importDrafts(eq(user), any()))
                .thenReturn(new ReviewCardImportService.ImportResult(1, 0));

        context.mvc.perform(post("/reviews/cards").param("sourceExportId", job.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"));
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
                        "A very long sentence")));
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
        when(context.exportService.generatedRecordsForCompletedExport(user, unavailableId)).thenReturn(Optional.empty());

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
                        List.of()),
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

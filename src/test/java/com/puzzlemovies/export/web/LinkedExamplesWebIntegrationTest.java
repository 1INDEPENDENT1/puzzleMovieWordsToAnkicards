package com.puzzlemovies.export.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.puzzlemovies.export.ExportWebAppApplication;
import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.service.ReviewCardImportService;
import com.puzzlemovies.export.service.ReviewService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puzzlemovies.export.export.RawDictionaryEntry;
import com.puzzlemovies.export.export.SourcePosition;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.ExportType;
import com.puzzlemovies.export.model.PuzzleSessionToken;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.repo.PuzzleSessionTokenRepository;
import com.puzzlemovies.export.repo.UserRepository;
import com.puzzlemovies.export.service.ExportCompletionInfo;
import com.puzzlemovies.export.service.ExportSnapshotStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "linked.test.jdbc-url", matches = ".+")
class LinkedExamplesWebIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PuzzleSessionTokenRepository tokens;
    @Autowired ExportJobRepository jobs;
    @Autowired ExportSnapshotStore snapshots;
    @Autowired SessionUserResolver resolver;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getProperty("linked.test.jdbc-url"));
        properties.add("spring.datasource.username", () -> System.getProperty("linked.test.username", "postgres"));
        properties.add("spring.datasource.password", () -> System.getProperty("linked.test.password", ""));
    }

    @Test
    void importsOneCardWithAllExamplesAndEnforcesPerPresentationReveal() throws Exception {
        var owner = user();
        var session = session(owner);
        var rows = new ArrayList<RawDictionaryEntry>();
        rows.add(entry(DictionarySection.WORD, 1, "give up", "сдаваться", null));
        for (int i = 1; i <= 100; i++) {
            rows.add(entry(DictionarySection.PHRASE, i, "Give it, please, up.", "Сдавайся " + i, "Film " + i));
        }
        UUID job = snapshot(owner, rows);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/reviews/cards").session(session).param("sourceExportId", job.toString()))
                    .andExpect(redirectedUrl("/reviews"));
        }
        mvc.perform(get("/reviews").session(session)).andExpect(status().isOk());
        mvc.perform(get("/cards").session(session)).andExpect(status().isOk());
        var first = read(mvc.perform(get("/reviews/next").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.remainingDueCount").value(1))
                .andExpect(jsonPath("$.card.exampleCount").value(100)).andReturn()).path("card");
        var second = read(mvc.perform(get("/reviews/next").session(session)).andReturn()).path("card");
        String card = first.path("id").asText();
        String presentation = first.path("presentationId").asText();
        assertNotEquals(presentation, second.path("presentationId").asText());

        String prefix = "/reviews/cards/" + card;
        var example = read(mvc.perform(get(prefix + "/examples").session(session).param("presentationId", presentation)
                        .param("page", "1")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.items[0].position").value(26))
                .andExpect(jsonPath("$.items[0].movieTitle").value("Film 26"))
                .andExpect(jsonPath("$.items[0].translationText").doesNotExist()).andReturn()).path("items").get(0);
        String translation = prefix + "/examples/" + example.path("exampleId").asText() + "/translation";
        String version = example.path("linkVersion").asText();
        mvc.perform(post(translation).session(session).contentType("application/json")
                        .content(revealBody(presentation, version)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MAIN_ANSWER_NOT_REVEALED"));
        mvc.perform(post(prefix + "/reveal").session(session).contentType("application/json")
                        .content("{\"presentationId\":\"" + presentation + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.translationText").value("сдаваться"));
        mvc.perform(post(translation).session(session).contentType("application/json")
                        .content(revealBody(presentation, version)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.translationText").value("Сдавайся 26"));
        mvc.perform(post(translation).session(session).contentType("application/json")
                        .content(revealBody(second.path("presentationId").asText(), version)))
                .andExpect(status().isConflict());
        mvc.perform(get("/cards/" + card + "/examples").session(session(user()))).andExpect(status().isNotFound());

        mvc.perform(post(prefix + "/answer").session(session).contentType("application/json")
                        .content("{\"answer\":\"KNOWN\",\"responseMillis\":1000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.reviewedCount").value(1));
        mvc.perform(post(translation).session(session).contentType("application/json")
                        .content(revealBody(presentation, version)))
                .andExpect(status().isConflict());
        mvc.perform(post("/reviews/cards").session(session).param("sourceExportId", job.toString()))
                .andExpect(redirectedUrl("/reviews"));
        mvc.perform(get("/reviews/next").session(session)).andExpect(jsonPath("$.counts.reviewedCount").value(1));
        var again = snapshot(owner, rows);
        mvc.perform(post("/reviews/cards").session(session).param("sourceExportId", again.toString()))
                .andExpect(redirectedUrl("/reviews"));
        mvc.perform(get("/reviews/next").session(session)).andExpect(jsonPath("$.counts.remainingDueCount").value(0));
    }

    @Test
    void removedLinkMakesOldPresentationStaleAndNewPresentationReturnNotFound() throws Exception {
        var owner = user();
        var session = session(owner);
        var job = snapshot(owner, List.of(
                entry(DictionarySection.WORD, 1, "give up", "сдаваться", null),
                entry(DictionarySection.PHRASE, 1, "Give it up.", "Сдавайся.", "Film")));
        mvc.perform(post("/reviews/cards").session(session).param("sourceExportId", job.toString()))
                .andExpect(redirectedUrl("/reviews"));
        var card = read(mvc.perform(get("/reviews/next").session(session)).andReturn()).path("card");
        String cardId = card.path("id").asText();
        String oldPresentation = card.path("presentationId").asText();
        String examples = "/reviews/cards/" + cardId + "/examples";
        var item = read(mvc.perform(get(examples).session(session).param("presentationId", oldPresentation))
                .andExpect(status().isOk()).andReturn()).path("items").get(0);
        String url = examples + "/" + item.path("exampleId").asText() + "/translation";
        mvc.perform(patch("/cards/" + cardId).session(session).contentType("application/json")
                        .content("{\"originalText\":\"give in\",\"translationText\":\"yield\",\"version\":" + card.path("version").asText() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.card.exampleCount").value(0));
        mvc.perform(post(url).session(session).contentType("application/json")
                        .content(revealBody(oldPresentation, item.path("linkVersion").asText())))
                .andExpect(status().isConflict());
        String fresh = read(mvc.perform(get("/reviews/next").session(session)).andReturn())
                .path("card").path("presentationId").asText();
        mvc.perform(post(url).session(session).contentType("application/json")
                        .content(revealBody(fresh, item.path("linkVersion").asText())))
                .andExpect(status().isNotFound());
    }

    @Test
    void restartsBeforeAndAfterPublishingWithoutLosingSnapshotOrReviewHistory() {
        var owner = user();
        var job = snapshot(owner, List.of(
                entry(DictionarySection.WORD, 1, "give up", "сдаваться", null),
                entry(DictionarySection.PHRASE, 1, "Give it up.", "Сдавайся.", "Film")));
        UUID cardId;
        try (var restarted = restart()) {
            var importer = restarted.getBean(ReviewCardImportService.class);
            assertEquals(1, importer.importSnapshot(owner, job).created());
            var reviews = restarted.getBean(ReviewService.class);
            var card = reviews.nextDueCard(owner, 0).card();
            cardId = card.id();
            assertEquals(1, card.exampleCount());
            reviews.updateContent(owner, cardId, new ReviewDtos.CardContentUpdateRequest("give up", "бросать", card.version()));
            reviews.answerCard(owner, cardId, ReviewAnswer.KNOWN, null, 0);
        }

        try (var restarted = restart()) {
            assertTrue(restarted.getBean(ReviewCardImportService.class).importSnapshot(owner, job).alreadyPublished());
            var reviews = restarted.getBean(ReviewService.class);
            var page = reviews.cardLibrary(owner, 0);
            assertEquals(1, page.getTotalElements());
            var card = page.getContent().getFirst();
            assertEquals(cardId, card.id());
            assertEquals("бросать", card.translationText());
            assertEquals(1, card.totalAnswers());
            assertEquals(1, card.exampleCount());
            assertNull(reviews.nextDueCard(owner, 0).card());
        }
    }

    private ConfigurableApplicationContext restart() {
        return new SpringApplicationBuilder(ExportWebAppApplication.class).web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + System.getProperty("linked.test.jdbc-url"),
                        "--spring.datasource.username=" + System.getProperty("linked.test.username", "postgres"),
                        "--spring.datasource.password=" + System.getProperty("linked.test.password", ""));
    }

    private User user() {
        var user = new User();
        user.setEmail(UUID.randomUUID() + "@example.com");
        user = users.save(user);
        var token = new PuzzleSessionToken();
        token.setUser(user);
        token.setCookieHeader("FAKE_SESSION=TEST");
        tokens.save(token);
        return user;
    }

    private MockHttpSession session(User user) {
        var session = new MockHttpSession();
        resolver.bind(session, user);
        return session;
    }

    private UUID snapshot(User user, List<RawDictionaryEntry> rows) {
        var job = new ExportJob();
        job.setUser(user);
        job.setType(ExportType.COMBINED);
        job.setStatus(ExportStatus.COMPLETED);
        snapshots.complete(job, rows, ExportCompletionInfo.full());
        return job.getId();
    }

    private RawDictionaryEntry entry(DictionarySection section, int index, String original, String translation, String film) {
        return new RawDictionaryEntry(new SourcePosition(section, 1, index, section + "-" + index),
                original, List.of(translation), List.of(), null, film, null);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String revealBody(String presentation, String version) {
        return "{\"presentationId\":\"" + presentation + "\",\"linkVersion\":" + version + "}";
    }
}

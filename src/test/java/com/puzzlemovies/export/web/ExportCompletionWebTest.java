package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.*;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.service.ExportService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExportCompletionWebTest {
    @TempDir Path directory;
    ExportJobRepository jobs;
    ExportService service;
    SessionUserResolver resolver;
    MockMvc mvc;
    User user;
    ExportJob job;

    @BeforeEach void setup() throws Exception {
        user = new User(); user.setId(UUID.randomUUID()); user.setEmail("owner@example.com");
        jobs = mock(ExportJobRepository.class); service = mock(ExportService.class);
        resolver = mock(SessionUserResolver.class);
        when(resolver.resolve(any())).thenReturn(Optional.of(user));
        job = new ExportJob(); job.setId(UUID.randomUUID()); job.setUser(user);
        job.setType(ExportType.WORDS); job.setStatus(ExportStatus.COMPLETED);
        job.setPhase(ExportPhase.COMPLETED); job.setProgressPercent(100); job.setRowCount(1);
        Path file = directory.resolve("words.tsv"); Files.writeString(file, "word\ttranslation\n");
        job.setOutputFilePath(file.toString()); job.setOutputFileName("words.tsv");
        when(jobs.findByIdAndUser(job.getId(), user)).thenReturn(Optional.of(job));
        var templates = new ClassLoaderTemplateResolver(); templates.setPrefix("templates/");
        templates.setSuffix(".html"); templates.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(templates);
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine);
        mvc = MockMvcBuilders.standaloneSetup(new ExportController(service, jobs, resolver),
                new ExportDownloadController(jobs, resolver)).setViewResolvers(views).build();
    }

    void partial(int count) { job.setErrorMessage("DICT_META_V1:{\"completeness\":\"PARTIAL\",\"unparsedRecordCount\":" + count + "}"); }

    @Test void counterWarningIsShownBeforeActionsWithoutInventingParseFailures() throws Exception {
        var completion = com.puzzlemovies.export.service.ExportCompletionInfo.withWarnings(0, List.of(
                new com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy(
                        com.puzzlemovies.export.puzzlemovies.DictionarySection.PHRASE, 766, 756)));
        job.setErrorMessage(com.puzzlemovies.export.service.ExportCompletionMetadataCodec.encode(completion));
        var session = new MockHttpSession();
        mvc.perform(get("/exports/{id}/status", job.getId()).session(session))
                .andExpect(jsonPath("$.completeness").value("PARTIAL"))
                .andExpect(jsonPath("$.unparsedRecordCount").value(0))
                .andExpect(jsonPath("$.sourceCountDiscrepancies[0].declaredTotal").value(766))
                .andExpect(jsonPath("$.sourceCountDiscrepancies[0].retrievedTotal").value(756));
        mvc.perform(get("/exports/{id}/download", job.getId()).session(session)).andExpect(status().isSeeOther());
        String html = mvc.perform(get("/exports/{id}", job.getId()).session(session)).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("766")); assertTrue(html.contains("756"));
        assertFalse(html.contains("could not be parsed"));
        assertTrue(html.indexOf("Source counter mismatch") < html.indexOf("id=\"download-link\""));
        mvc.perform(get("/exports/{id}/download", job.getId()).session(session)).andExpect(status().isOk());
    }

    @Test void partialStatusProjectsWarningAndNeverLeaksStorageEnvelope() throws Exception {
        partial(2);
        mvc.perform(get("/exports/{id}/status", job.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.completeness").value("PARTIAL"))
                .andExpect(jsonPath("$.unparsedRecordCount").value(2))
                .andExpect(jsonPath("$.warningMessage").value(org.hamcrest.Matchers.containsString("2")))
                .andExpect(jsonPath("$.errorMessage").isEmpty())
                .andExpect(jsonPath("$.downloadAvailable").value(true))
                .andExpect(jsonPath("$.reviewCreationAvailable").value(false))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("DICT_META_"))));
    }

    @Test void partialDownloadRequiresServerRenderedWarningPerSessionJobAndFingerprint() throws Exception {
        partial(2); var session = new MockHttpSession();
        mvc.perform(get("/exports/{id}/download", job.getId()).session(session).param("warningPresented", "true"))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/exports/" + job.getId()));
        mvc.perform(get("/exports/{id}/status", job.getId()).session(session));
        mvc.perform(get("/exports/{id}/download", job.getId()).session(session)).andExpect(status().isSeeOther());
        String html = mvc.perform(get("/exports/{id}", job.getId()).session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Partial export"));
        assertTrue(html.indexOf("id=\"warning-text\"") < html.indexOf("id=\"download-link\""));
        assertTrue(html.indexOf("id=\"warning-text\"") < html.indexOf("id=\"review-create\""));
        assertTrue(html.contains("data-warning-presented=\"true\""));
        assertFalse(html.contains("DICT_META_"));
        mvc.perform(get("/exports/{id}/download", job.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string("word\ttranslation\n"));
        mvc.perform(get("/exports/{id}/download", job.getId())).andExpect(status().isSeeOther());
        UUID previous = job.getId(); job.setId(UUID.randomUUID());
        when(jobs.findByIdAndUser(job.getId(), user)).thenReturn(Optional.of(job));
        mvc.perform(get("/exports/{id}/download", job.getId()).session(session)).andExpect(status().isSeeOther());
        job.setId(previous); partial(3);
        mvc.perform(get("/exports/{id}/download", job.getId()).session(session)).andExpect(status().isSeeOther());
    }

    @Test void fullDownloadRemainsAvailableWithoutPresentationAndSsrRendersAction() throws Exception {
        mvc.perform(get("/exports/{id}/download", job.getId())).andExpect(status().isOk());
        mvc.perform(get("/exports/{id}/status", job.getId())).andExpect(jsonPath("$.completeness").value("FULL"))
                .andExpect(jsonPath("$.unparsedRecordCount").value(0));
        String html = mvc.perform(get("/exports/{id}", job.getId())).andReturn().getResponse().getContentAsString();
        var doc = org.jsoup.Jsoup.parse(html);
        assertFalse(doc.getElementById("download-link").attr("style").contains("none"));
        assertNotNull(doc.getElementById("error-text")); assertNotNull(doc.getElementById("warning-text"));
    }

    @Test void corruptMarkerDisablesActionsWithoutExposingIt() throws Exception {
        job.setErrorMessage("DICT_META_V9:private sentinel");
        mvc.perform(get("/exports/{id}/status", job.getId())).andExpect(jsonPath("$.downloadAvailable").value(false))
                .andExpect(jsonPath("$.reviewCreationAvailable").value(false))
                .andExpect(jsonPath("$.errorMessage").isNotEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private sentinel"))));
        mvc.perform(get("/exports/{id}/download", job.getId())).andExpect(status().isConflict());
        String html = mvc.perform(get("/exports/{id}", job.getId())).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("DICT_META_")); assertFalse(html.contains("private sentinel"));
    }

    @Test void absentFileDisablesOnlyDownloadAndFailedJobsHaveNoActions() throws Exception {
        Files.delete(Path.of(job.getOutputFilePath()));
        mvc.perform(get("/exports/{id}/status", job.getId())).andExpect(jsonPath("$.downloadAvailable").value(false));
        mvc.perform(get("/exports/{id}/download", job.getId())).andExpect(status().isConflict());
        job.setStatus(ExportStatus.FAILED); job.setErrorMessage("Dictionary fetch failed");
        mvc.perform(get("/exports/{id}/status", job.getId())).andExpect(jsonPath("$.completeness").isEmpty())
                .andExpect(jsonPath("$.reviewCreationAvailable").value(false));
    }

    @Test void missingSnapshotKeepsPartialFileAvailableAndWarningVisible() throws Exception {
        partial(4);
        when(service.reviewCreationAvailable(job)).thenReturn(true);
        mvc.perform(get("/exports/{id}/status", job.getId())).andExpect(jsonPath("$.reviewCreationAvailable").value(true));
        when(service.reviewCreationAvailable(job)).thenReturn(false);
        var session = new MockHttpSession();
        String html = mvc.perform(get("/exports/{id}", job.getId()).session(session))
                .andReturn().getResponse().getContentAsString();
        var doc = org.jsoup.Jsoup.parse(html);
        assertTrue(doc.getElementById("warning-text").text().contains("4"));
        assertFalse(doc.getElementById("download-link").attr("style").contains("none"));
        assertTrue(doc.getElementById("review-create").attr("style").contains("none"));
        mvc.perform(get("/exports/{id}/download", job.getId()).session(session)).andExpect(status().isOk());
    }

    @Test void allExportRoutesPreserveOwnerAndSessionChecks() throws Exception {
        for (String suffix : List.of("", "/status", "/download")) {
            mvc.perform(get("/exports/{id}" + suffix, UUID.randomUUID())).andExpect(status().isNotFound());
        }
        when(resolver.resolve(any())).thenReturn(Optional.empty());
        for (String suffix : List.of("", "/status", "/download")) {
            mvc.perform(get("/exports/{id}" + suffix, job.getId())).andExpect(status().isUnauthorized());
        }
    }
}

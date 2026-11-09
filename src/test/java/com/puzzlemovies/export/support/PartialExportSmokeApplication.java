package com.puzzlemovies.export.support;

import com.puzzlemovies.export.ExportWebAppApplication;
import org.jsoup.Jsoup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Explicit test-classpath entrypoint. Never packaged or discovered by ordinary app startup. */
public final class PartialExportSmokeApplication {
    public static void main(String[] args) {
        if (!Arrays.asList(args).contains("--spring.profiles.active=controlled-partial"))
            throw new IllegalArgumentException("Explicit controlled-partial profile is required");
        SpringApplication.run(new Class<?>[]{ExportWebAppApplication.class, ControlledTransport.class}, args);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Profile("controlled-partial")
    public static class ControlledTransport {
        // Only the fixture launcher submits in the background: production startup and routes are unchanged.
        @Bean @Primary
        com.puzzlemovies.export.service.ExportService controlledExportService(
                com.puzzlemovies.export.repo.ExportJobRepository jobs,
                com.puzzlemovies.export.repo.PuzzleSessionTokenRepository tokens,
                com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryClient dictionary,
                com.puzzlemovies.export.export.DictionaryParser parser,
                com.puzzlemovies.export.export.VocabularyDeduplicator dedupe,
                com.puzzlemovies.export.export.DictionaryMatcher matcher,
                com.puzzlemovies.export.export.ExportRecordBuilder builder,
                com.puzzlemovies.export.export.AnkiExportFormatter formatter,
                com.puzzlemovies.export.config.ExportProperties properties,
                @org.springframework.beans.factory.annotation.Qualifier("exportTaskExecutor") java.util.concurrent.Executor executor) {
            return new com.puzzlemovies.export.service.ExportService(jobs,tokens,dictionary,parser,dedupe,matcher,builder,formatter,properties) {
                @Override
                @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
                public com.puzzlemovies.export.model.ExportJob startExport(com.puzzlemovies.export.model.User user, com.puzzlemovies.export.model.ExportType type) {
                    var token = tokens.findByUser(user).orElseThrow();
                    var job = new com.puzzlemovies.export.model.ExportJob();
                    job.setUser(user); job.setType(type); job.setStatus(com.puzzlemovies.export.model.ExportStatus.PENDING);
                    job = jobs.saveAndFlush(job);
                    var id = job.getId();
                    executor.execute(() -> runExportAsync(id,type,token.getCookieHeader()));
                    return job;
                }
            };
        }

        @Bean @Primary
        @SuppressWarnings({"unchecked", "rawtypes"})
        HttpClient controlledHttpClient(@org.springframework.beans.factory.annotation.Value("${controlled.counter-mismatch:false}") boolean counterMismatch) throws Exception {
            var client = mock(HttpClient.class);
            String words = fixture("words-real.html", 3, !counterMismatch);
            String phrases = fixture("phrases-real.html", 2, false);
            if (counterMismatch) {
                var doc = Jsoup.parse(phrases);
                doc.selectFirst(".movies-tabs__item.is-active .movies-tabs__item-title span").text("3");
                phrases = doc.outerHtml();
            }
            String phraseResponse = phrases;
            when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
                HttpRequest request = invocation.getArgument(0);
                if (!"controlled.invalid".equals(request.uri().getHost()))
                    throw new AssertionError("Controlled transport refuses unexpected origin");
                var response = mock(HttpResponse.class);
                when(response.statusCode()).thenReturn(200);
                when(response.uri()).thenReturn(request.uri());
                if ("/api2/user/signin".equals(request.uri().getPath())) {
                    when(response.body()).thenReturn("{\"error\":false,\"login\":\"controlled\",\"logged_in_cookie_name\":\"FAKE_SESSION\",\"logged_in_cookie\":\"FAKE_TEST_ONLY\"}");
                    when(response.headers()).thenReturn(HttpHeaders.of(Map.of("set-cookie", List.of("FAKE_SESSION=FAKE_TEST_ONLY; Path=/")), (k,v) -> true));
                } else if ("/dictionary".equals(request.uri().getPath())) {
                    boolean word = request.uri().getQuery().contains("item=word");
                    if (word) Thread.sleep(8000); // Lets the browser observe RUNNING before terminal polling.
                    when(response.body()).thenReturn(word ? words : phraseResponse);
                    when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (k,v) -> true));
                } else throw new AssertionError("Controlled transport refuses unexpected path");
                return response;
            });
            return client;
        }

        private String fixture(String name, int count, boolean rejectLast) throws Exception {
            try (var stream = PartialExportSmokeApplication.class.getResourceAsStream("/puzzlemovies/" + name)) {
                if (stream == null) throw new IllegalStateException("Missing controlled fixture");
                var doc = Jsoup.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                var rows = doc.select("tr.dict__video__list-table__row");
                for (int i = count; i < rows.size(); i++) rows.get(i).remove();
                doc.selectFirst(".movies-tabs__item.is-active .movies-tabs__item-title span").text(Integer.toString(count));
                doc.select(".paginator-style-2").remove();
                if (rejectLast) rows.get(count - 1).select(".translate_line_without_popup").empty();
                return doc.outerHtml();
            }
        }
    }
}

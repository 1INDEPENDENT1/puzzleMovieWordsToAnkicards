package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.*;
import com.puzzlemovies.export.service.ExportCompletionInfo;
import com.puzzlemovies.export.service.ExportCompletionMetadataCodec;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("postgresAvailable")
class ExportJobCompletionPersistenceTest {
    static PostgreSQLContainer<?> postgres;
    @Autowired EntityManager em;

    static boolean postgresAvailable() {
        return System.getProperty("review.test.jdbc-url") != null
                || org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (System.getProperty("review.test.jdbc-url") != null) {
            registry.add("spring.datasource.url", () -> System.getProperty("review.test.jdbc-url"));
            registry.add("spring.datasource.username", () -> System.getProperty("review.test.username", "postgres"));
            registry.add("spring.datasource.password", () -> System.getProperty("review.test.password", ""));
        } else {
            postgres = new PostgreSQLContainer<>("postgres:17-alpine");
            postgres.start();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @AfterAll static void stopContainer() { if (postgres != null) postgres.stop(); }

    @Test void partialWarningAndExactCountSurviveFlushAndContextClear() {
        var job = persist(ExportCompletionMetadataCodec.encode(ExportCompletionInfo.partial(7)));
        String envelope = job.getErrorMessage();
        em.flush();
        em.clear();
        var loaded = em.find(ExportJob.class, job.getId());
        assertNotSame(job, loaded);
        var completion = ExportCompletionMetadataCodec.decode(loaded);
        assertEquals(ExportCompletionInfo.Completeness.PARTIAL, completion.completeness());
        assertEquals(7, completion.unparsedRecordCount());
        assertTrue(completion.warningMessage().contains("7"));
        assertFalse(completion.warningMessage().contains("DICT_META_"));
        assertEquals(envelope, loaded.getErrorMessage());
        Object[] stored = (Object[]) em.createNativeQuery("select status, phase, error_message from export_jobs where id = :id")
                .setParameter("id", loaded.getId()).getSingleResult();
        assertArrayEquals(new Object[]{"COMPLETED", "COMPLETED", envelope}, stored);
    }

    @Test void malformedPersistedMetadataFailsClosedAfterReload() {
        var job = persist("DICT_META_V1:{\"completeness\":\"PARTIAL\",\"unparsedRecordCount\":1.5}");
        em.flush();
        em.clear();
        var loaded = em.find(ExportJob.class, job.getId());
        assertThrows(IllegalStateException.class, () -> ExportCompletionMetadataCodec.decode(loaded));
    }

    @Test void sourceCounterWarningSurvivesReloadWithoutBecomingRejectedCount() {
        var expected = ExportCompletionInfo.withWarnings(0, java.util.List.of(
                new com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy(
                        com.puzzlemovies.export.puzzlemovies.DictionarySection.PHRASE, 766, 756)));
        var job = persist(ExportCompletionMetadataCodec.encode(expected));
        em.flush(); em.clear();
        assertEquals(expected, ExportCompletionMetadataCodec.decode(em.find(ExportJob.class, job.getId())));
    }

    @Test void legacyAndFullRowsDecodeWithoutRewritingStoredValues() {
        var legacy = persist("Historical completed job note");
        var full = persist(null);
        em.flush();
        em.clear();
        var loaded = em.find(ExportJob.class, legacy.getId());
        assertEquals(ExportCompletionInfo.full(), ExportCompletionMetadataCodec.decode(loaded));
        assertEquals(ExportCompletionInfo.full(), ExportCompletionMetadataCodec.decode(em.find(ExportJob.class, full.getId())));
        em.flush();
        em.clear();
        assertEquals("Historical completed job note", em.find(ExportJob.class, legacy.getId()).getErrorMessage());
        assertNull(em.find(ExportJob.class, full.getId()).getErrorMessage());
        assertArrayEquals(new String[]{"PENDING", "RUNNING", "COMPLETED", "FAILED"},
                java.util.Arrays.stream(ExportStatus.values()).map(Enum::name).toArray(String[]::new));
    }

    private ExportJob persist(String metadata) {
        var user = new User();
        user.setEmail(java.util.UUID.randomUUID() + "@example.invalid");
        em.persist(user);
        var job = new ExportJob();
        job.setUser(user);
        job.setType(ExportType.COMBINED);
        job.setStatus(ExportStatus.COMPLETED);
        job.setProgressPercent(100);
        job.setErrorMessage(metadata);
        job.setOutputFileName("controlled.tsv");
        job.setOutputFilePath("controlled.tsv");
        job.setRowCount(2);
        em.persist(job);
        return job;
    }
}

package com.puzzlemovies.export.model;

import com.puzzlemovies.export.export.RawDictionaryEntry;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vocabulary_import_snapshots", uniqueConstraints = @UniqueConstraint(
        name = "uk_vocabulary_import_snapshots_user_job", columnNames = {"user_id", "export_job_id"}))
public class VocabularyImportSnapshot {
    public static final int SCHEMA_VERSION = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "export_job_id", nullable = false, updatable = false)
    private ExportJob exportJob;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, updatable = false)
    private int schemaVersion = SCHEMA_VERSION;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Payload payload;

    @Column(nullable = false, updatable = false, length = 64)
    private String contentChecksum;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

    protected VocabularyImportSnapshot() {
    }

    public VocabularyImportSnapshot(User user, ExportJob exportJob, Payload payload, String contentChecksum) {
        this.user = user;
        this.exportJob = exportJob;
        this.payload = payload;
        this.contentChecksum = contentChecksum;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public ExportJob getExportJob() { return exportJob; }
    public int getSchemaVersion() { return schemaVersion; }
    public Payload getPayload() { return payload; }
    public String getContentChecksum() { return contentChecksum; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public record Payload(List<RawDictionaryEntry> entries, List<String> warnings) {
        public Payload {
            entries = List.copyOf(entries);
            warnings = List.copyOf(warnings);
        }
    }
}

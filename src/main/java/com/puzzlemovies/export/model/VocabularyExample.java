package com.puzzlemovies.export.model;

import com.puzzlemovies.export.export.SourcePosition;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "vocabulary_examples", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vocabulary_examples_user_source", columnNames = {"user_id", "source_key"}),
        @UniqueConstraint(name = "uk_vocabulary_examples_user_id", columnNames = {"user_id", "id"})
}, indexes = @Index(name = "ix_vocabulary_examples_user_order", columnList = "user_id,first_seen_order,id"))
public class VocabularyExample {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, updatable = false, length = 64)
    private String sourceKey;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String sourceIdentity;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String sourceOriginal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> sourceTranslations;

    @Column(columnDefinition = "text")
    private String movieTitle;

    @Column(columnDefinition = "text")
    private String movieUrl;

    @Column(columnDefinition = "text")
    private String sourceLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Provenance provenance;

    @Column(nullable = false, updatable = false)
    private long firstSeenOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private MatchingSentences matchingSentences;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected VocabularyExample() {
    }

    public VocabularyExample(User user, String sourceKey, String sourceIdentity, long firstSeenOrder) {
        this.user = user;
        this.sourceKey = sourceKey;
        this.sourceIdentity = sourceIdentity;
        this.firstSeenOrder = firstSeenOrder;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSourceKey() { return sourceKey; }
    public String getSourceIdentity() { return sourceIdentity; }
    public String getSourceOriginal() { return sourceOriginal; }
    public List<String> getSourceTranslations() { return List.copyOf(sourceTranslations); }
    public String getMovieTitle() { return movieTitle; }
    public String getMovieUrl() { return movieUrl; }
    public String getSourceLabel() { return sourceLabel; }
    public long getFirstSeenOrder() { return firstSeenOrder; }

    public boolean replaceSource(String original, List<String> translations, String sourceLabel, String movieTitle,
                                 String movieUrl, Provenance provenance, MatchingSentences matchingSentences) {
        boolean changed = !Objects.equals(this.sourceOriginal, original)
                || !Objects.equals(this.sourceTranslations, translations)
                || !Objects.equals(this.sourceLabel, sourceLabel)
                || !Objects.equals(this.movieTitle, movieTitle)
                || !Objects.equals(this.movieUrl, movieUrl);
        if (changed) {
            this.sourceOriginal = original;
            this.sourceTranslations = List.copyOf(translations);
            this.sourceLabel = sourceLabel;
            this.movieTitle = movieTitle;
            this.movieUrl = movieUrl;
            this.provenance = provenance;
        }
        this.matchingSentences = matchingSentences;
        return changed;
    }

    public void setMatchingSentences(MatchingSentences matchingSentences) {
        this.matchingSentences = matchingSentences;
    }

    public record Provenance(int schemaVersion, UUID snapshotId, SourcePosition position,
                             String inlineSourceId, Integer inlineIndex) {
    }

    public record MatchingSentences(int normalizationVersion, List<List<String>> sentences, boolean fallbackUsed) {
        public MatchingSentences {
            sentences = sentences.stream().map(List::copyOf).toList();
        }
    }
}

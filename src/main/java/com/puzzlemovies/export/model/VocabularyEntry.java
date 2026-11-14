package com.puzzlemovies.export.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "vocabulary_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vocabulary_entries_user_source", columnNames = {"user_id", "source_key"}),
        @UniqueConstraint(name = "uk_vocabulary_entries_user_id", columnNames = {"user_id", "id"})
})
public class VocabularyEntry {
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
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String sourceOriginal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> sourceTranslations;

    @Column(columnDefinition = "text")
    private String originalOverride;

    @Column(columnDefinition = "text")
    private String translationOverride;

    @Column(nullable = false, columnDefinition = "text")
    private String matchingForm = "";

    @Column(nullable = false)
    private int normalizationVersion;

    @Column(nullable = false)
    private long contentRevision;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected VocabularyEntry() {
    }

    public VocabularyEntry(User user, String sourceKey, String sourceIdentity, String sourceOriginal,
                           List<String> sourceTranslations) {
        this.user = user;
        this.sourceKey = sourceKey;
        this.sourceIdentity = sourceIdentity;
        this.sourceOriginal = sourceOriginal;
        this.sourceTranslations = List.copyOf(new LinkedHashSet<>(sourceTranslations));
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
    public User getUser() { return user; }
    public long getVersion() { return version; }
    public String getSourceKey() { return sourceKey; }
    public String getSourceIdentity() { return sourceIdentity; }
    public String getSourceOriginal() { return sourceOriginal; }
    public List<String> getSourceTranslations() { return List.copyOf(sourceTranslations); }
    public String getOriginalOverride() { return originalOverride; }
    public String getTranslationOverride() { return translationOverride; }
    public long getContentRevision() { return contentRevision; }
    public String getEffectiveOriginal() { return originalOverride == null ? sourceOriginal : originalOverride; }
    public String getEffectiveTranslation() {
        return translationOverride == null ? String.join("; ", sourceTranslations) : translationOverride;
    }

    public void setOriginalOverride(String originalOverride) {
        if (!Objects.equals(this.originalOverride, originalOverride)) {
            this.originalOverride = originalOverride;
            touchContent();
        }
    }

    public void setTranslationOverride(String translationOverride) {
        if (!Objects.equals(this.translationOverride, translationOverride)) {
            this.translationOverride = translationOverride;
            touchContent();
        }
    }

    public void mergeTranslations(List<String> translations) {
        var merged = new LinkedHashSet<>(sourceTranslations);
        if (merged.addAll(translations)) {
            sourceTranslations = List.copyOf(merged);
            touchContent();
        }
    }

    public void setMatchingForm(String matchingForm, int normalizationVersion) {
        this.matchingForm = matchingForm;
        this.normalizationVersion = normalizationVersion;
    }

    public void touchContent() {
        contentRevision++;
    }
}

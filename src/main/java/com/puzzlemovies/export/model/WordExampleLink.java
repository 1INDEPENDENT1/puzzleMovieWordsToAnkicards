package com.puzzlemovies.export.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "word_example_links", uniqueConstraints = @UniqueConstraint(
        name = "uk_word_example_links_user_pair", columnNames = {"user_id", "entry_id", "example_id"}),
        indexes = {
                @Index(name = "ix_word_example_links_user_entry", columnList = "user_id,entry_id"),
                @Index(name = "ix_word_example_links_user_example", columnList = "user_id,example_id")
        })
public class WordExampleLink {
    public enum MatchKind { LEMMA_SEQUENCE, SOURCE_INLINE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false, updatable = false)
    private VocabularyEntry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "example_id", nullable = false, updatable = false)
    private VocabularyExample example;

    @Version
    @Column(nullable = false)
    private long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MatchKind matchKind;

    @Column(nullable = false)
    private int normalizationVersion;

    @Column(columnDefinition = "text")
    private String originalOverride;

    @Column(columnDefinition = "text")
    private String translationOverride;

    @Column(nullable = false)
    private boolean customized;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected WordExampleLink() {
    }

    public WordExampleLink(User user, VocabularyEntry entry, VocabularyExample example,
                           MatchKind kind, int normalizationVersion) {
        this.user = user;
        this.entry = entry;
        this.example = example;
        setMatch(kind, normalizationVersion);
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
    public long getVersion() { return version; }
    public VocabularyEntry getEntry() { return entry; }
    public VocabularyExample getExample() { return example; }
    public MatchKind getMatchKind() { return matchKind; }
    public boolean isCustomized() { return customized; }
    public String getOriginalOverride() { return originalOverride; }
    public String getTranslationOverride() { return translationOverride; }
    public String getEffectiveOriginal() {
        return originalOverride == null ? example.getSourceOriginal() : originalOverride;
    }

    public String getEffectiveTranslation() {
        return translationOverride == null ? String.join("; ", example.getSourceTranslations()) : translationOverride;
    }

    public void setMatch(MatchKind matchKind, int normalizationVersion) {
        this.matchKind = matchKind;
        this.normalizationVersion = normalizationVersion;
    }

    public void touchSource() {
        updatedAt = Instant.now();
    }

    public void setOriginalOverride(String originalOverride) {
        this.originalOverride = originalOverride;
        customized = originalOverride != null || translationOverride != null;
    }

    public void setTranslationOverride(String translationOverride) {
        this.translationOverride = translationOverride;
        customized = originalOverride != null || translationOverride != null;
    }
}

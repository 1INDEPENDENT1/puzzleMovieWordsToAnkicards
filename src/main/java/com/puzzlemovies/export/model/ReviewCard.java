package com.puzzlemovies.export.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_cards",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_cards_user_entry", columnNames = {"user_id", "vocabulary_entry_id"}),
                @UniqueConstraint(name = "uk_review_cards_user_id", columnNames = {"user_id", "id"})
        })
public class ReviewCard {
    @Version
    @Column(nullable = false)
    private long version;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_entry_id", nullable = false, updatable = false)
    private VocabularyEntry vocabularyEntry;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewCardState state = ReviewCardState.NEW;

    @NotNull
    @Column(nullable = false)
    private Instant dueAt;

    @Column(nullable = false)
    private int intervalDays;

    @Column(nullable = false)
    private double easeFactor = 2.5;

    @Column(nullable = false)
    private int reviewCount;

    @Column(nullable = false)
    private int lapseCount;

    private Instant lastReviewedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.dueAt == null) {
            this.dueAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public VocabularyEntry getVocabularyEntry() {
        return vocabularyEntry;
    }

    public void setVocabularyEntry(VocabularyEntry vocabularyEntry) {
        this.vocabularyEntry = vocabularyEntry;
    }

    public ReviewCardState getState() {
        return state;
    }

    public void setState(ReviewCardState state) {
        this.state = state;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(int intervalDays) {
        this.intervalDays = intervalDays;
    }

    public double getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(double easeFactor) {
        this.easeFactor = easeFactor;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getLapseCount() {
        return lapseCount;
    }

    public void setLapseCount(int lapseCount) {
        this.lapseCount = lapseCount;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(Instant lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

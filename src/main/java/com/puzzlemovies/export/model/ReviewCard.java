package com.puzzlemovies.export.model;

import com.puzzlemovies.export.export.ExportRecord;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_cards",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_cards_user_content_key",
                columnNames = {"user_id", "content_key"}))
public class ReviewCard {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String originalText;

    @Column(length = 4000)
    private String instanceText;

    @Column(length = 8000)
    private String translationText;

    @Column(length = 2000)
    private String sourceContext;

    @NotBlank
    @Column(name = "content_key", nullable = false, length = 128)
    private String contentKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExportRecord.RecordKind recordKind;

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

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getInstanceText() {
        return instanceText;
    }

    public void setInstanceText(String instanceText) {
        this.instanceText = instanceText;
    }

    public String getTranslationText() {
        return translationText;
    }

    public void setTranslationText(String translationText) {
        this.translationText = translationText;
    }

    public String getSourceContext() {
        return sourceContext;
    }

    public void setSourceContext(String sourceContext) {
        this.sourceContext = sourceContext;
    }

    public String getContentKey() {
        return contentKey;
    }

    public void setContentKey(String contentKey) {
        this.contentKey = contentKey;
    }

    public ExportRecord.RecordKind getRecordKind() {
        return recordKind;
    }

    public void setRecordKind(ExportRecord.RecordKind recordKind) {
        this.recordKind = recordKind;
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

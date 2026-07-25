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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_attempts")
public class ReviewAttempt {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_card_id", nullable = false)
    private ReviewCard reviewCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewAnswer answer;

    @NotNull
    @Column(nullable = false)
    private Instant reviewedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewCardState previousState;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewCardState nextState;

    private Instant previousDueAt;

    @NotNull
    @Column(nullable = false)
    private Instant nextDueAt;

    @Column(nullable = false)
    private int previousIntervalDays;

    @Column(nullable = false)
    private int nextIntervalDays;

    private Long responseMillis;

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ReviewCard getReviewCard() {
        return reviewCard;
    }

    public void setReviewCard(ReviewCard reviewCard) {
        this.reviewCard = reviewCard;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ReviewAnswer getAnswer() {
        return answer;
    }

    public void setAnswer(ReviewAnswer answer) {
        this.answer = answer;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public ReviewCardState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(ReviewCardState previousState) {
        this.previousState = previousState;
    }

    public ReviewCardState getNextState() {
        return nextState;
    }

    public void setNextState(ReviewCardState nextState) {
        this.nextState = nextState;
    }

    public Instant getPreviousDueAt() {
        return previousDueAt;
    }

    public void setPreviousDueAt(Instant previousDueAt) {
        this.previousDueAt = previousDueAt;
    }

    public Instant getNextDueAt() {
        return nextDueAt;
    }

    public void setNextDueAt(Instant nextDueAt) {
        this.nextDueAt = nextDueAt;
    }

    public int getPreviousIntervalDays() {
        return previousIntervalDays;
    }

    public void setPreviousIntervalDays(int previousIntervalDays) {
        this.previousIntervalDays = previousIntervalDays;
    }

    public int getNextIntervalDays() {
        return nextIntervalDays;
    }

    public void setNextIntervalDays(int nextIntervalDays) {
        this.nextIntervalDays = nextIntervalDays;
    }

    public Long getResponseMillis() {
        return responseMillis;
    }

    public void setResponseMillis(Long responseMillis) {
        this.responseMillis = responseMillis;
    }
}

package com.puzzlemovies.export.service;

import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.ReviewCardState;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ReviewCardRepository;
import com.puzzlemovies.export.review.ReviewCardDraft;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class ReviewCardImportService {
    private final ReviewCardRepository reviewCardRepository;

    public ReviewCardImportService(ReviewCardRepository reviewCardRepository) {
        this.reviewCardRepository = reviewCardRepository;
    }

    @Transactional
    public ImportResult importDrafts(User user, List<ReviewCardDraft> drafts) {
        int created = 0;
        int updated = 0;
        Instant now = Instant.now();
        for (ReviewCardDraft draft : drafts) {
            String contentKey = contentKey(draft);
            ReviewCard card = reviewCardRepository.findByUserAndContentKey(user, contentKey)
                    .orElseGet(() -> {
                        ReviewCard createdCard = new ReviewCard();
                        createdCard.setUser(user);
                        createdCard.setContentKey(contentKey);
                        createdCard.setState(ReviewCardState.NEW);
                        createdCard.setDueAt(now);
                        return createdCard;
                    });
            boolean isNew = card.getId() == null;
            card.setOriginalText(draft.originalText());
            card.setInstanceText(draft.instanceText());
            card.setTranslationText(draft.translationText());
            card.setSourceContext(draft.sourceContext());
            card.setRecordKind(draft.recordKind());
            reviewCardRepository.save(card);
            if (isNew) {
                created++;
            } else {
                updated++;
            }
        }
        return new ImportResult(created, updated);
    }

    public String contentKey(ReviewCardDraft draft) {
        String identity = String.join("\u001f",
                draft.recordKind().name(),
                normalize(draft.originalText()),
                normalize(draft.instanceText()),
                normalize(draft.translationText()),
                normalize(draft.sourceContext()));
        return sha256(identity);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public record ImportResult(int created, int updated) {
    }
}

package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ReviewCard;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReviewCardRepositoryTest {
    @Test
    void reviewCardHasOwnerScopedDuplicateConstraint() {
        Table table = ReviewCard.class.getAnnotation(Table.class);

        assertNotNull(table);
        assertEquals("review_cards", table.name());
        assertEquals("uk_review_cards_user_content_key", table.uniqueConstraints()[0].name());
        assertEquals("user_id", table.uniqueConstraints()[0].columnNames()[0]);
        assertEquals("content_key", table.uniqueConstraints()[0].columnNames()[1]);
    }

    @Test
    void repositoryExposesOwnerScopedDueAndDuplicateQueries() throws Exception {
        assertMethod("findByIdAndUser", 2);
        assertMethod("findByUserAndContentKey", 2);
        assertMethod("findDueCards", 3);
        assertMethod("countDueCards", 2);
        assertMethod("findNextDueAt", 2);
    }

    private void assertMethod(String name, int parameterCount) {
        for (Method method : ReviewCardRepository.class.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return;
            }
        }
        throw new AssertionError("Missing method " + name);
    }
}

package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ReviewAttempt;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReviewAttemptRepositoryTest {
    @Test
    void reviewAttemptUsesAppendOnlyTableWithoutUpdateLifecycleHook() {
        Table table = ReviewAttempt.class.getAnnotation(Table.class);

        assertNotNull(table);
        assertEquals("review_attempts", table.name());
        for (Method method : ReviewAttempt.class.getDeclaredMethods()) {
            if (method.getName().equals("onUpdate")) {
                throw new AssertionError("ReviewAttempt should not expose an update lifecycle hook");
            }
        }
    }

    @Test
    void repositoryExposesCardAndOwnerScopedHistoryQueries() {
        assertMethod("findByReviewCardAndUserOrderByReviewedAtAsc", 2);
        assertMethod("findByUserOrderByReviewedAtAsc", 1);
    }

    private void assertMethod(String name, int parameterCount) {
        for (Method method : ReviewAttemptRepository.class.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return;
            }
        }
        throw new AssertionError("Missing method " + name);
    }
}

package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.VocabularyExample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VocabularyExampleRepository extends JpaRepository<VocabularyExample, UUID> {
    List<VocabularyExample> findByUser_IdOrderByFirstSeenOrderAscIdAsc(UUID userId);
}

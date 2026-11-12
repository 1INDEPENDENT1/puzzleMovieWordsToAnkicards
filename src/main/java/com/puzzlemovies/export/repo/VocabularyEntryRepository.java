package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.VocabularyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VocabularyEntryRepository extends JpaRepository<VocabularyEntry, UUID> {
    List<VocabularyEntry> findByUser_IdOrderByIdAsc(UUID userId);
}

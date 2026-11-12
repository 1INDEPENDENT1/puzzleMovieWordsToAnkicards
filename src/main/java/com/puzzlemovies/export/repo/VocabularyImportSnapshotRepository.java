package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.VocabularyImportSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VocabularyImportSnapshotRepository extends JpaRepository<VocabularyImportSnapshot, UUID> {
    Optional<VocabularyImportSnapshot> findByUser_IdAndExportJob_Id(UUID userId, UUID exportJobId);
}

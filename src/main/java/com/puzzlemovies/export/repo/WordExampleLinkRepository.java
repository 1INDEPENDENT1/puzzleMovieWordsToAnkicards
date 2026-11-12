package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.WordExampleLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WordExampleLinkRepository extends JpaRepository<WordExampleLink, UUID> {
    List<WordExampleLink> findByUser_Id(UUID userId);

    @EntityGraph(attributePaths = "example")
    List<WordExampleLink> findByUser_IdAndEntry_IdOrderByExample_FirstSeenOrderAscExample_IdAsc(UUID userId, UUID entryId);

    @EntityGraph(attributePaths = "example")
    Page<WordExampleLink> findByUser_IdAndEntry_IdOrderByExample_FirstSeenOrderAscExample_IdAsc(
            UUID userId, UUID entryId, Pageable pageable);

    long countByUser_IdAndEntry_Id(UUID userId, UUID entryId);

    Optional<WordExampleLink> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<WordExampleLink> findByUser_IdAndEntry_IdAndExample_Id(UUID userId, UUID entryId, UUID exampleId);
}

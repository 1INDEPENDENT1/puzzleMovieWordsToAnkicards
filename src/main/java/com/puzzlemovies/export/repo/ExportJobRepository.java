package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {
    Optional<ExportJob> findByIdAndUser(UUID id, User user);
}

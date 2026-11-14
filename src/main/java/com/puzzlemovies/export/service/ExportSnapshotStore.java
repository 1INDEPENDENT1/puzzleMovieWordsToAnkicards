package com.puzzlemovies.export.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puzzlemovies.export.export.RawDictionaryEntry;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.VocabularyImportSnapshot;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.repo.VocabularyImportSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExportSnapshotStore {
    private final ExportJobRepository jobs;
    private final VocabularyImportSnapshotRepository snapshots;
    private final ObjectMapper mapper;

    public ExportSnapshotStore(ExportJobRepository jobs, VocabularyImportSnapshotRepository snapshots,
                               ObjectMapper mapper) {
        this.jobs = jobs;
        this.snapshots = snapshots;
        this.mapper = mapper;
    }

    @Transactional
    public void complete(ExportJob job, List<RawDictionaryEntry> entries, ExportCompletionInfo completion) {
        var payload = new VocabularyImportSnapshot.Payload(entries,
                completion.warningMessage() == null ? List.of() : List.of(completion.warningMessage()));
        jobs.save(job);
        snapshots.save(new VocabularyImportSnapshot(job.getUser(), job, payload, checksum(mapper, payload)));
    }

    @Transactional(readOnly = true)
    public Optional<VocabularyImportSnapshot> find(UUID owner, UUID job) {
        return snapshots.findByUser_IdAndExportJob_Id(owner, job);
    }

    public void verify(VocabularyImportSnapshot snapshot) {
        if (!checksum(mapper, snapshot.getPayload()).equals(snapshot.getContentChecksum())) {
            throw new IllegalStateException("Import snapshot checksum mismatch");
        }
    }

    public static String checksum(ObjectMapper mapper, VocabularyImportSnapshot.Payload payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writeValueAsBytes(payload)));
        } catch (NoSuchAlgorithmException | JsonProcessingException ex) {
            throw new IllegalStateException("Could not verify import snapshot", ex);
        }
    }
}

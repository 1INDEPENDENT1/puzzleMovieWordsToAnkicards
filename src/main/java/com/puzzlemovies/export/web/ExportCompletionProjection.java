package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.service.ExportCompletionInfo;
import com.puzzlemovies.export.service.ExportCompletionMetadataCodec;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Shared boundary projection: storage metadata is never user-facing error text. */
record ExportCompletionProjection(ExportCompletionInfo completion, String errorMessage,
                                  boolean downloadAvailable) {
    static ExportCompletionProjection from(ExportJob job) {
        try {
            ExportCompletionInfo info = ExportCompletionMetadataCodec.decode(job);
            String error = job.getStatus() == ExportStatus.COMPLETED ? null : job.getErrorMessage();
            if (error != null && error.startsWith("DICT_META_")) {
                error = "Export completion metadata is unavailable. Run a fresh export.";
            }
            return new ExportCompletionProjection(info, error, info != null && hasFile(job));
        } catch (IllegalStateException ex) {
            return new ExportCompletionProjection(null,
                    "Export completion metadata is unavailable. Run a fresh export.", false);
        }
    }

    private static boolean hasFile(ExportJob job) {
        try {
            return job.getOutputFilePath() != null && Files.isRegularFile(Path.of(job.getOutputFilePath()));
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    boolean partial() {
        return completion != null && completion.completeness() == ExportCompletionInfo.Completeness.PARTIAL;
    }
}

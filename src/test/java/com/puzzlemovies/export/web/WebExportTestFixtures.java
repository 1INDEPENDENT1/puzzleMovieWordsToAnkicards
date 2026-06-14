package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportPhase;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.ExportType;

import java.util.UUID;

public final class WebExportTestFixtures {
    private WebExportTestFixtures() {
    }

    public static ExportJob completedJob(int rowCount) {
        ExportJob job = new ExportJob();
        job.setId(UUID.randomUUID());
        job.setType(ExportType.COMBINED);
        job.setStatus(ExportStatus.COMPLETED);
        job.setPhase(ExportPhase.COMPLETED);
        job.setProgressPercent(100);
        job.setRowCount(rowCount);
        return job;
    }
}

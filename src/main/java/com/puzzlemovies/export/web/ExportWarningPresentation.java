package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.ExportJob;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Records server presentation for this session, job, and persisted metadata version. */
final class ExportWarningPresentation {
    private static final String KEY = ExportWarningPresentation.class.getName() + ".";
    private ExportWarningPresentation() {}

    static void record(HttpSession session, ExportJob job) {
        session.setAttribute(KEY + job.getId(), fingerprint(job));
    }

    static boolean wasPresented(HttpSession session, ExportJob job) {
        return fingerprint(job).equals(session.getAttribute(KEY + job.getId()));
    }

    private static String fingerprint(ExportJob job) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(job.getErrorMessage()).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to identify export warning", ex);
        }
    }
}

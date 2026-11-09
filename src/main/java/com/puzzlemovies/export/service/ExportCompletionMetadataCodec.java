package com.puzzlemovies.export.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
public final class ExportCompletionMetadataCodec {
    private static final String PREFIX = "DICT_META_V1:";
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private ExportCompletionMetadataCodec() {}
    public static String encode(ExportCompletionInfo info) {
        if (info.completeness() == ExportCompletionInfo.Completeness.FULL) return null;
        var node = JSON.createObjectNode().put("completeness", "PARTIAL")
                .put("unparsedRecordCount", info.unparsedRecordCount());
        if (!info.sourceCountDiscrepancies().isEmpty()) {
            var counts = node.putArray("sourceCountDiscrepancies");
            for (var count : info.sourceCountDiscrepancies()) counts.addObject().put("section", count.section().name())
                    .put("declaredTotal", count.declaredTotal()).put("retrievedTotal", count.retrievedTotal());
        }
        return PREFIX + node;
    }
    public static ExportCompletionInfo decode(ExportJob job) {
        if (job.getStatus() != ExportStatus.COMPLETED) return null;
        String value = job.getErrorMessage();
        if (value == null || !value.startsWith("DICT_META_")) return ExportCompletionInfo.full();
        try {
            if (!value.startsWith(PREFIX) || value.length() > 4000) throw new IllegalArgumentException();
            var node = JSON.readTree(value.substring(PREFIX.length()));
            var count = node.get("unparsedRecordCount");
            if (!"PARTIAL".equals(node.path("completeness").asText()) || count == null || !count.isIntegralNumber()
                    || !count.canConvertToInt() || count.intValue() < 0) throw new IllegalArgumentException();
            var discrepancies = new java.util.ArrayList<com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy>();
            var counts = node.get("sourceCountDiscrepancies");
            if (counts != null) {
                if (!counts.isArray() || counts.size() > 2) throw new IllegalArgumentException();
                for (var item : counts) {
                    var declared = item.path("declaredTotal"); var retrieved = item.path("retrievedTotal");
                    if (!declared.isIntegralNumber() || !declared.canConvertToInt()
                            || !retrieved.isIntegralNumber() || !retrieved.canConvertToInt()) throw new IllegalArgumentException();
                    discrepancies.add(new com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy(
                            com.puzzlemovies.export.puzzlemovies.DictionarySection.valueOf(item.path("section").asText()),
                            declared.intValue(), retrieved.intValue()));
                }
            }
            return new ExportCompletionInfo(ExportCompletionInfo.Completeness.PARTIAL, count.intValue(), discrepancies, null);
        } catch (Exception ignored) {
            throw new IllegalStateException("Export completion metadata is unavailable. Run a fresh export.");
        }
    }
}

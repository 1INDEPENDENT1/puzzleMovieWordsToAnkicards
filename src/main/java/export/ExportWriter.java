package export;

import model.CliOptions;
import model.DictionaryPhrase;
import model.DictionaryWord;
import model.ExportRecord;
import model.PhraseExample;
import util.AppException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ExportWriter {
    public void writeCombined(Path output,
                              List<DictionaryWord> words,
                              List<DictionaryPhrase> phrases,
                              Map<String, List<PhraseExample>> examples,
                              CliOptions.ExportFormat format) {
        ExportFormatter formatter = new ExportFormatter();
        List<ExportRecord> records = formatter.buildRecords(words, phrases, examples);
        String content = formatter.format(records, format);
        write(output, content);
    }

    public void write(Path output, String content) {
        if (output == null) {
            throw AppException.input("Output file path is required.");
        }
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw AppException.io("Failed to write export file: " + output, ex);
        }
    }
}

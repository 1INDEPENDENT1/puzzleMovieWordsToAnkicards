package com.puzzlemovies.export.review;

import com.puzzlemovies.export.export.ExportRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReviewCardDraftFactory {
    public List<ReviewCardDraft> createDrafts(List<ExportRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<ReviewCardDraft> drafts = new ArrayList<>();
        for (ExportRecord record : records) {
            drafts.addAll(createDrafts(record));
        }
        return drafts;
    }

    public List<ReviewCardDraft> createDrafts(ExportRecord record) {
        if (record.recordKind() == ExportRecord.RecordKind.PHRASE) {
            return List.of(new ReviewCardDraft(
                    record.front(),
                    null,
                    htmlToText(record.back()),
                    null,
                    record.recordKind()));
        }
        return wordDrafts(record);
    }

    private List<ReviewCardDraft> wordDrafts(ExportRecord record) {
        Document document = Jsoup.parseBodyFragment(record.back());
        List<Element> examples = document.select("div.example");
        String baseTranslation = textWithoutExamples(record.back());
        if (examples.isEmpty()) {
            return List.of(new ReviewCardDraft(
                    record.front(),
                    null,
                    baseTranslation,
                    null,
                    record.recordKind()));
        }

        List<ReviewCardDraft> drafts = new ArrayList<>();
        for (Element example : examples) {
            String instanceText = firstOwnLine(example);
            String exampleTranslation = htmlToText(example.select(".translation").html());
            String sourceContext = htmlToText(example.select(".movie").html());
            String translationText = combine(baseTranslation, exampleTranslation);
            drafts.add(new ReviewCardDraft(
                    record.front(),
                    instanceText,
                    translationText,
                    sourceContext,
                    record.recordKind()));
        }
        return drafts;
    }

    private String textWithoutExamples(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        document.select("div.example").remove();
        return htmlToText(document.body().html());
    }

    private String firstOwnLine(Element element) {
        String ownText = element.ownText();
        if (ownText != null && !ownText.isBlank()) {
            return ownText.trim();
        }
        return htmlToText(element.html());
    }

    private String combine(String first, String second) {
        String cleanFirst = first == null ? "" : first.trim();
        String cleanSecond = second == null ? "" : second.trim();
        if (cleanFirst.isEmpty()) {
            return cleanSecond;
        }
        if (cleanSecond.isEmpty()) {
            return cleanFirst;
        }
        return cleanFirst + "\n" + cleanSecond;
    }

    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document document = Jsoup.parseBodyFragment(html);
        document.select("br").append("\\n");
        String text = document.body().wholeText();
        return text.replace("\\n", "\n")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n")
                .trim();
    }
}

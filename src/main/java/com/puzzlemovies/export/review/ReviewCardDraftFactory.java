package com.puzzlemovies.export.review;

import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.export.DictionaryPhrase;
import com.puzzlemovies.export.export.DictionaryWord;
import com.puzzlemovies.export.export.PhraseExample;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ReviewCardDraftFactory {
    private static final int CONTEXT_EXAMPLE_CAP = 2;

    public List<ReviewCardDraft> createDrafts(ReviewExportSource source) {
        if (source == null) {
            return List.of();
        }

        List<ReviewCardDraft> drafts = new ArrayList<>();
        Set<ReviewCardDraft> standaloneExamples = new LinkedHashSet<>();
        Set<String> associatedExampleKeys = new LinkedHashSet<>();

        for (DictionaryWord word : source.words()) {
            List<PhraseExample> matches = source.completeMatches().forWord(word.matchingIdentity());
            for (int index = 0; index < matches.size(); index++) {
                PhraseExample example = matches.get(index);
                associatedExampleKeys.add(exampleKey(example.phraseSourceText(), example.phraseTranslations(),
                        example.movieTitle(), example.movieUrl()));
                if (index < CONTEXT_EXAMPLE_CAP) {
                    drafts.add(contextualDraft(word, example));
                } else {
                    standaloneExamples.add(standaloneDraft(example));
                }
            }
            if (matches.isEmpty()) {
                drafts.add(wordOnlyDraft(word));
            }
        }

        for (DictionaryPhrase example : source.examples()) {
            if (!associatedExampleKeys.contains(exampleKey(example.sourceText(), example.translations(),
                    example.movieTitle(), example.movieUrl()))) {
                standaloneExamples.add(standaloneDraft(example));
            }
        }
        drafts.addAll(standaloneExamples);
        return List.copyOf(drafts);
    }

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

    private ReviewCardDraft wordOnlyDraft(DictionaryWord word) {
        return new ReviewCardDraft(
                word.sourceText(),
                null,
                joinTranslations(word.translations()),
                null,
                ExportRecord.RecordKind.WORD);
    }

    private ReviewCardDraft contextualDraft(DictionaryWord word, PhraseExample example) {
        return new ReviewCardDraft(
                word.sourceText(),
                example.phraseSourceText(),
                combine(joinTranslations(word.translations()), joinTranslations(example.phraseTranslations())),
                example.movieTitle(),
                ExportRecord.RecordKind.WORD);
    }

    private ReviewCardDraft standaloneDraft(DictionaryPhrase example) {
        return new ReviewCardDraft(
                example.sourceText(),
                null,
                joinTranslations(example.translations()),
                example.movieTitle(),
                ExportRecord.RecordKind.PHRASE);
    }

    private ReviewCardDraft standaloneDraft(PhraseExample example) {
        return new ReviewCardDraft(
                example.phraseSourceText(),
                null,
                joinTranslations(example.phraseTranslations()),
                example.movieTitle(),
                ExportRecord.RecordKind.PHRASE);
    }

    private String joinTranslations(Set<String> translations) {
        return translations == null || translations.isEmpty() ? "" : String.join("\n", translations);
    }

    private String exampleKey(String sourceText, Set<String> translations, String movieTitle, String movieUrl) {
        return String.join("\u001f",
                sourceText == null ? "" : sourceText.trim(),
                joinTranslations(translations),
                movieTitle == null ? "" : movieTitle.trim(),
                movieUrl == null ? "" : movieUrl.trim());
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

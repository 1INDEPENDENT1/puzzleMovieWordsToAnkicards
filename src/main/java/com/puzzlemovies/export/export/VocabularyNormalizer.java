package com.puzzlemovies.export.export;

import org.drugov.lingua.morph.Lemmatizer;
import org.springframework.stereotype.Component;

import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Component
public class VocabularyNormalizer {
    public static final int MATCHING_VERSION = 1;

    private static final Pattern WORD = Pattern.compile(
            "\\p{N}+(?:\\.\\p{N}+)+|[\\p{L}\\p{N}][\\p{L}\\p{M}\\p{N}]*(?:['’][\\p{L}\\p{M}\\p{N}]+)*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]+[\\p{Pe}\\p{Pf}\"']*\\s+");
    private static final Pattern ABBREVIATION = Pattern.compile(
            "(?:\\b(?:Mr|Mrs|Ms|Dr|Prof|Sr|Jr|St|vs|e\\.g|i\\.e)|\\b[A-HJ-Z])\\.$");
    private static final Set<String> LEADING_PREFIXES = Set.of("to", "a", "an", "the");

    private final Lemmatizer lemmatizer;

    public VocabularyNormalizer(Lemmatizer lemmatizer) {
        this.lemmatizer = lemmatizer;
    }

    public NormalizedText normalizeSingle(String text) {
        return normalize(List.of(text == null ? "" : text), true);
    }

    public NormalizedText normalizePhrase(String text) {
        return normalize(splitSentences(text), false);
    }

    public static String canonicalSourceText(String text) {
        return WHITESPACE.matcher(Normalizer.normalize(text, Normalizer.Form.NFC))
                .replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }

    private NormalizedText normalize(List<String> sourceSentences, boolean removePrefix) {
        List<List<String>> sentences = new ArrayList<>();
        boolean fallbackUsed = false;
        for (String sentence : sourceSentences) {
            String canonical = Normalizer.normalize(sentence, Normalizer.Form.NFC).replace('’', '\'').toLowerCase(Locale.ROOT);
            List<String> sourceTokens = WORD.matcher(canonical).results().map(result -> result.group()).toList();
            if (sourceTokens.isEmpty()) {
                continue;
            }
            // The library tokenizer can omit numbers and split contractions. Use the lexical positions
            // above if its stream differs, so omitted or split tokens cannot change a matching gap.
            List<String> tokens = sourceTokens;
            try {
                List<String> libraryTokens = lemmatizer.tokenize(canonical);
                if (sourceTokens.equals(libraryTokens)) {
                    tokens = libraryTokens;
                } else {
                    fallbackUsed = true;
                }
            } catch (RuntimeException ignored) {
                fallbackUsed = true;
            }
            int first = removePrefix && tokens.size() > 1 && LEADING_PREFIXES.contains(tokens.getFirst()) ? 1 : 0;
            List<String> lemmas = new ArrayList<>();
            for (String token : tokens.subList(first, tokens.size())) {
                try {
                    String lemma = lemmatizer.lemma(token);
                    if (lemma != null) {
                        String cleaned = lemma.strip().toLowerCase(Locale.ROOT);
                        if (WORD.matcher(cleaned).matches()) {
                            lemmas.add(cleaned);
                            continue;
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Preserve this position even if the lemma is unavailable.
                }
                lemmas.add(token);
                fallbackUsed = true;
            }
            sentences.add(lemmas);
        }

        List<String> orderedTokens = sentences.stream().flatMap(List::stream).toList();
        return new NormalizedText(String.join(" ", orderedTokens), new LinkedHashSet<>(orderedTokens),
                fallbackUsed || orderedTokens.isEmpty(), sentences);
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        TreeSet<Integer> boundaries = new TreeSet<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        iterator.setText(text);
        for (int end = iterator.next(); end != BreakIterator.DONE; end = iterator.next()) {
            boundaries.add(end);
        }
        // English BreakIterator may miss a sentence ending before a lowercase start.
        var matcher = SENTENCE_END.matcher(text);
        while (matcher.find()) {
            boundaries.add(matcher.end());
        }

        List<String> sentences = new ArrayList<>();
        int start = 0;
        for (int end : boundaries) {
            String sentence = text.substring(start, end).strip();
            if (end < text.length() && ABBREVIATION.matcher(sentence).find()) {
                continue;
            }
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            start = end;
        }
        return sentences;
    }

    public record NormalizedText(String identity, Set<String> tokens, boolean fallbackUsed, List<List<String>> sentences) {
        public NormalizedText {
            tokens = Collections.unmodifiableSet(new LinkedHashSet<>(tokens));
            sentences = sentences.stream().map(List::copyOf).toList();
        }
    }
}

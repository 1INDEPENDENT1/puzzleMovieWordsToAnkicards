package com.puzzlemovies.export.service;

import com.puzzlemovies.export.export.DictionaryMatcher;
import com.puzzlemovies.export.export.RawDictionaryEntry;
import com.puzzlemovies.export.export.SourcePosition;
import com.puzzlemovies.export.export.VocabularyNormalizer;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.model.VocabularyEntry;
import com.puzzlemovies.export.model.VocabularyExample;
import com.puzzlemovies.export.model.VocabularyImportSnapshot;
import com.puzzlemovies.export.model.WordExampleLink;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.repo.ReviewCardRepository;
import com.puzzlemovies.export.repo.UserRepository;
import com.puzzlemovies.export.repo.VocabularyEntryRepository;
import com.puzzlemovies.export.repo.VocabularyExampleRepository;
import com.puzzlemovies.export.repo.WordExampleLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.puzzlemovies.export.export.VocabularyNormalizer.MATCHING_VERSION;
import static com.puzzlemovies.export.model.WordExampleLink.MatchKind.LEMMA_SEQUENCE;
import static com.puzzlemovies.export.model.WordExampleLink.MatchKind.SOURCE_INLINE;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class LinkedVocabularyService {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private final UserRepository users;
    private final VocabularyEntryRepository entries;
    private final VocabularyExampleRepository examples;
    private final WordExampleLinkRepository links;
    private final ReviewCardRepository cards;
    private final VocabularyNormalizer normalizer;
    private final DictionaryMatcher matcher;

    public LinkedVocabularyService(UserRepository users, VocabularyEntryRepository entries,
                                   VocabularyExampleRepository examples, WordExampleLinkRepository links,
                                   ReviewCardRepository cards, VocabularyNormalizer normalizer, DictionaryMatcher matcher) {
        this.users = users;
        this.entries = entries;
        this.examples = examples;
        this.links = links;
        this.cards = cards;
        this.normalizer = normalizer;
        this.matcher = matcher;
    }

    public User lockOwner(UUID userId) {
        return users.lockById(userId).orElseThrow();
    }

    /** Publishes rows under the caller's owner lock and snapshot transaction. */
    public int publish(User lockedUser, UUID snapshotId, List<RawDictionaryEntry> rows, Instant now) {
        UUID userId = lockedUser.getId();
        var ownedEntries = byKey(entries.findByUser_IdOrderByIdAsc(userId), VocabularyEntry::getSourceKey);
        var ownedExamples = byKey(examples.findByUser_IdOrderByFirstSeenOrderAscIdAsc(userId),
                VocabularyExample::getSourceKey);
        long nextOrder = ownedExamples.values().stream().mapToLong(VocabularyExample::getFirstSeenOrder).max()
                .orElse(-1) + 1;
        var changedExamples = new HashSet<UUID>();
        var inlinePairs = new HashSet<ExamplePair>();
        for (RawDictionaryEntry row : rows) {
            if (row.position().section() == DictionarySection.PHRASE) {
                var source = new ExampleSource(row.sourceText(), row.translations(), row.sourceLabel(),
                        row.movieTitle(), row.movieUrl(), row.position(), row.position().sourceItemId(), null, null);
                var example = upsertExample(lockedUser, snapshotId, source, ownedExamples, nextOrder, changedExamples);
                nextOrder = Math.max(nextOrder, example.getFirstSeenOrder() + 1);
                continue;
            }
            VocabularyEntry entry = upsertEntry(lockedUser, row, ownedEntries);
            for (int index = 0; index < row.examples().size(); index++) {
                var inline = row.examples().get(index);
                var source = new ExampleSource(inline.sourceText(), inline.translations(), inline.sourceLabel(),
                        inline.movieTitle(), inline.movieUrl(), row.position(), inline.stableSourceId(),
                        entry.getSourceIdentity(), index);
                var example = upsertExample(lockedUser, snapshotId, source, ownedExamples, nextOrder, changedExamples);
                nextOrder = Math.max(nextOrder, example.getFirstSeenOrder() + 1);
                inlinePairs.add(new ExamplePair(entry.getId(), example.getId()));
            }
        }
        rebuildLinks(lockedUser, List.copyOf(ownedEntries.values()), List.copyOf(ownedExamples.values()),
                inlinePairs, changedExamples);
        var existingCardEntries = new HashSet<>(cards.findEntryIdsByUserId(userId));
        var newCards = new ArrayList<ReviewCard>();
        for (VocabularyEntry entry : ownedEntries.values()) {
            if (!existingCardEntries.contains(entry.getId())) {
                ReviewCard card = new ReviewCard();
                card.setUser(lockedUser);
                card.setVocabularyEntry(entry);
                card.setDueAt(now);
                newCards.add(card);
            }
        }
        cards.saveAll(newCards);
        return newCards.size();
    }

    /** Recomputes automatic links under the caller's owner lock after an original-text edit. */
    public void rematch(UUID userId) {
        User owner = users.getReferenceById(userId);
        rebuildLinks(owner, entries.findByUser_IdOrderByIdAsc(userId),
                examples.findByUser_IdOrderByFirstSeenOrderAscIdAsc(userId), Set.of(), Set.of());
    }

    private VocabularyEntry upsertEntry(User user, RawDictionaryEntry row, Map<String, VocabularyEntry> ownedEntries) {
        String identity = VocabularyNormalizer.canonicalSourceText(row.sourceText());
        String key = sourceKey(identity);
        VocabularyEntry entry = ownedEntries.get(key);
        if (entry == null) {
            entry = entries.save(new VocabularyEntry(user, key, identity, row.sourceText(), row.translations()));
            ownedEntries.put(key, entry);
        } else {
            checkIdentity(identity, entry.getSourceIdentity());
            entry.mergeTranslations(row.translations());
        }
        return entry;
    }

    private VocabularyExample upsertExample(User user, UUID snapshotId, ExampleSource source,
                                            Map<String, VocabularyExample> ownedExamples, long order,
                                            Set<UUID> changedExamples) {
        String identity = exampleIdentity(source);
        String key = sourceKey(identity);
        VocabularyExample example = ownedExamples.get(key);
        boolean newExample = example == null;
        if (newExample) {
            example = new VocabularyExample(user, key, identity, order);
        } else {
            checkIdentity(identity, example.getSourceIdentity());
        }
        boolean changed = example.replaceSource(source.original(), source.translations(), source.label(),
                source.movieTitle(), source.movieUrl(), new VocabularyExample.Provenance(
                        VocabularyImportSnapshot.SCHEMA_VERSION, snapshotId, source.position(),
                        source.inlineIndex() == null ? null : source.stableId(), source.inlineIndex()),
                matchingSentences(normalizer.normalizePhrase(source.original())));
        if (newExample) {
            example = examples.save(example);
            ownedExamples.put(key, example);
        } else if (changed) {
            changedExamples.add(example.getId());
        }
        return example;
    }

    private void rebuildLinks(User user, List<VocabularyEntry> ownedEntries, List<VocabularyExample> ownedExamples,
                              Set<ExamplePair> newInlinePairs, Set<UUID> changedExamples) {
        var desired = new HashMap<ExamplePair, WordExampleLink.MatchKind>();
        newInlinePairs.forEach(pair -> desired.put(pair, SOURCE_INLINE));
        var entriesById = new HashMap<UUID, VocabularyEntry>();
        var examplesById = new HashMap<UUID, VocabularyExample>();
        var wordForms = ownedEntries.stream().map(entry -> {
            entriesById.put(entry.getId(), entry);
            var form = normalizer.normalizeSingle(entry.getEffectiveOriginal());
            entry.setMatchingForm(form.identity(), MATCHING_VERSION);
            return form;
        }).toList();
        var exampleForms = ownedExamples.stream().map(example -> {
            examplesById.put(example.getId(), example);
            var form = normalizer.normalizePhrase(example.getSourceOriginal());
            example.setMatchingSentences(matchingSentences(form));
            return form;
        }).toList();
        var indexes = matcher.matchingExampleIndexes(wordForms, exampleForms);
        for (int i = 0; i < ownedEntries.size(); i++) {
            UUID entryId = ownedEntries.get(i).getId();
            for (int exampleIndex : indexes.get(i)) {
                desired.putIfAbsent(new ExamplePair(entryId, ownedExamples.get(exampleIndex).getId()), LEMMA_SEQUENCE);
            }
        }

        var touched = new HashSet<VocabularyEntry>();
        for (WordExampleLink link : links.findByUser_Id(user.getId())) {
            var pair = new ExamplePair(link.getEntry().getId(), link.getExample().getId());
            var kind = desired.remove(pair);
            if (link.getMatchKind() == SOURCE_INLINE) {
                kind = SOURCE_INLINE;
            }
            if (kind != null) {
                link.setMatch(kind, MATCHING_VERSION);
            } else if (!link.isCustomized()) {
                links.delete(link);
                touched.add(link.getEntry());
                continue;
            }
            if (changedExamples.contains(pair.exampleId())) {
                link.touchSource();
                touched.add(link.getEntry());
            }
        }
        desired.forEach((pair, kind) -> {
            var entry = entriesById.get(pair.entryId());
            links.save(new WordExampleLink(user, entry, examplesById.get(pair.exampleId()), kind, MATCHING_VERSION));
            touched.add(entry);
        });
        touched.forEach(VocabularyEntry::touchContent);
    }

    private static VocabularyExample.MatchingSentences matchingSentences(VocabularyNormalizer.NormalizedText form) {
        return new VocabularyExample.MatchingSentences(MATCHING_VERSION, form.sentences(), form.fallbackUsed());
    }

    private static String exampleIdentity(ExampleSource source) {
        if (source.stableId() != null && !source.stableId().isBlank()) {
            return source.inlineIndex() == null
                    ? encode(List.of("section-id", source.position().section().name(), source.stableId()))
                    : encode(List.of("inline-id", source.parentIdentity(), source.stableId()));
        }
        var parts = new ArrayList<>(List.of("source-content", normalizeSpaces(source.original()),
                normalizeSpaces(source.movieTitle()), normalizeSpaces(source.movieUrl()), normalizeSpaces(source.label())));
        source.translations().stream().map(LinkedVocabularyService::normalizeSpaces).forEach(parts::add);
        return encode(parts);
    }

    private static String normalizeSpaces(String text) {
        return text == null ? "" : WHITESPACE.matcher(Normalizer.normalize(text, Normalizer.Form.NFC))
                .replaceAll(" ").strip();
    }

    private static String encode(List<String> parts) {
        StringBuilder identity = new StringBuilder();
        parts.forEach(part -> identity.append(part.length()).append(':').append(part));
        return identity.toString();
    }

    private static String sourceKey(String identity) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void checkIdentity(String incomingIdentity, String existingIdentity) {
        if (!existingIdentity.equals(incomingIdentity)) {
            throw new IllegalStateException("Vocabulary source identity collision");
        }
    }

    private static <T> Map<String, T> byKey(List<T> values, Function<T, String> key) {
        Map<String, T> indexed = new LinkedHashMap<>();
        values.forEach(value -> indexed.put(key.apply(value), value));
        return indexed;
    }

    private record ExamplePair(UUID entryId, UUID exampleId) {
    }

    private record ExampleSource(String original, List<String> translations, String label, String movieTitle,
                                 String movieUrl, SourcePosition position, String stableId,
                                 String parentIdentity, Integer inlineIndex) {
    }
}

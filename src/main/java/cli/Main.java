package cli;

import export.ExportFormatter;
import export.ExportWriter;
import http.AuthService;
import http.DictionaryClient;
import http.HttpClientProvider;
import model.CliOptions;
import model.DictionaryPhrase;
import model.DictionaryWord;
import model.ExportRecord;
import model.PhraseExample;
import parser.PhraseParser;
import parser.WordParser;
import util.AppException;
import util.Deduplicator;
import util.Matcher;
import util.TextNormalizer;
import util.TokenStore;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            run(args);
        } catch (AppException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) {
        CliOptions options = parseArgs(args);

        HttpClientProvider httpClientProvider = new HttpClientProvider();
        HttpClient client = httpClientProvider.create();
        TokenStore tokenStore = new TokenStore();

        String cookieHeader = tokenStore.readToken(options.tokenFile()).orElse(null);
        AuthService authService = new AuthService(client, httpClientProvider);
        DictionaryClient dictionaryClient = new DictionaryClient(client, httpClientProvider);

        if (cookieHeader == null || cookieHeader.isBlank()) {
            cookieHeader = authenticate(options, authService, tokenStore);
        }

        List<String> wordPages;
        List<String> phrasePages;
        try {
            wordPages = dictionaryClient.fetchAllPages(cookieHeader, DictionaryClient.ItemType.WORD);
            phrasePages = dictionaryClient.fetchAllPages(cookieHeader, DictionaryClient.ItemType.PHRASE);
        } catch (AppException ex) {
            if (ex.getCode() == AppException.ErrorCode.AUTH) {
                cookieHeader = authenticate(options, authService, tokenStore);
                wordPages = dictionaryClient.fetchAllPages(cookieHeader, DictionaryClient.ItemType.WORD);
                phrasePages = dictionaryClient.fetchAllPages(cookieHeader, DictionaryClient.ItemType.PHRASE);
            } else {
                throw ex;
            }
        }

        WordParser wordParser = new WordParser();
        PhraseParser phraseParser = new PhraseParser();
        List<DictionaryWord> words = new ArrayList<>();
        for (String page : wordPages) {
            words.addAll(wordParser.parsePage(page));
        }
        List<DictionaryPhrase> phrases = new ArrayList<>();
        for (String page : phrasePages) {
            phrases.addAll(phraseParser.parsePage(page));
        }

        TextNormalizer normalizer = new TextNormalizer();
        Deduplicator deduplicator = new Deduplicator(normalizer);
        words = deduplicator.dedupeWords(words);
        phrases = deduplicator.dedupePhrases(phrases);

        Matcher matcher = new Matcher(normalizer);
        Map<String, List<PhraseExample>> examples = matcher.match(words, phrases);

        ExportFormatter formatter = new ExportFormatter();
        List<ExportRecord> records = formatter.buildRecords(words, phrases, examples);

        ExportWriter writer = new ExportWriter();
        writer.write(options.outputFile(), formatter.format(records, options.format()));

        System.out.println("Exported " + records.size() + " records to " + options.outputFile());
    }

    private static String authenticate(CliOptions options, AuthService authService, TokenStore tokenStore) {
        if (options.email() == null || options.email().isBlank() || options.password() == null || options.password().isBlank()) {
            throw AppException.input("Email/password required to authenticate.");
        }
        String cookieHeader = authService.authenticate(options.email(), options.password());
        tokenStore.writeToken(options.tokenFile(), cookieHeader);
        return cookieHeader;
    }

    private static CliOptions parseArgs(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String key = arg.substring(2);
            if (key.equals("help") || key.equals("h")) {
                printUsage();
                System.exit(0);
            }
            String value = (i + 1 < args.length) ? args[++i] : null;
            values.put(key, value);
        }

        String email = values.get("email");
        String password = values.get("password");
        String tokenFile = values.getOrDefault("token-file", "./puzzle_token.txt");
        String format = values.getOrDefault("format", "tsv");
        String output = values.getOrDefault("output", "./puzzle-export.tsv");

        CliOptions.ExportFormat exportFormat;
        try {
            exportFormat = CliOptions.ExportFormat.fromString(format);
        } catch (IllegalArgumentException ex) {
            throw AppException.input(ex.getMessage());
        }

        return new CliOptions(email, password, Path.of(tokenFile), exportFormat, Path.of(output));
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar puzzle-movies-exporter.jar [options]");
        System.out.println("  --email <email>        Puzzle-Movies account email");
        System.out.println("  --password <password>  Puzzle-Movies account password");
        System.out.println("  --token-file <path>    Token storage path (default: ./puzzle_token.txt)");
        System.out.println("  --format <tsv|csv>     Export format (default: tsv)");
        System.out.println("  --output <path>        Output file path (default: ./puzzle-export.tsv)");
        System.out.println("  --help                 Show this help message");
    }
}

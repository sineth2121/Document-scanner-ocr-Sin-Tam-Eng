package util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unicode-aware find/replace helpers for English, Sinhala, Tamil, and mixed OCR text.
 * Java's default {@code \b} and {@code \w} only treat ASCII as word characters.
 */
public final class SearchTextUtils {

    private static final int PATTERN_FLAGS = Pattern.UNICODE_CHARACTER_CLASS
            | Pattern.UNICODE_CASE
            | Pattern.CASE_INSENSITIVE;

    /** Letters, combining marks (e.g. Sinhala vowel signs), digits, underscore. */
    private static final String WORD_CHARS = "\\p{L}\\p{M}\\p{N}_";

    private static final Pattern WORD_TOKEN_PATTERN =
            Pattern.compile("[" + WORD_CHARS + "]+", PATTERN_FLAGS);

    private SearchTextUtils() {
    }

    /**
     * Whole-word pattern with Unicode boundaries (works for Sinhala, Tamil, English).
     */
    public static Pattern wholeWordPattern(String term) {
        String quoted = Pattern.quote(normalizeForSearch(term));
        String regex = "(?<![" + WORD_CHARS + "])" + quoted + "(?![" + WORD_CHARS + "])";
        return Pattern.compile(regex, PATTERN_FLAGS);
    }

    public static List<int[]> findWholeWordMatches(String text, String term) {
        List<int[]> matches = new ArrayList<>();
        if (text == null || text.isEmpty() || term == null || term.isBlank()) {
            return matches;
        }
        String normalizedTerm = normalizeForSearch(term.trim());
        if (normalizedTerm.isEmpty()) {
            return matches;
        }

        Matcher matcher = wholeWordPattern(normalizedTerm).matcher(text);
        while (matcher.find()) {
            matches.add(new int[]{matcher.start(), matcher.end()});
        }
        return matches;
    }

    public static int countWordTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        Matcher m = WORD_TOKEN_PATTERN.matcher(normalizeForSearch(text));
        while (m.find()) {
            count++;
        }
        return count;
    }

    public static Set<String> uniqueWordTokens(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        Matcher m = WORD_TOKEN_PATTERN.matcher(normalizeForSearch(text));
        while (m.find()) {
            tokens.add(m.group());
        }
        return tokens;
    }

    /**
     * True if {@code text[start,end)} matches {@code searchTerm} (Unicode case-insensitive).
     */
    public static boolean regionMatchesSearchTerm(String text, int start, int end, String searchTerm) {
        if (text == null || searchTerm == null) {
            return false;
        }
        String term = normalizeForSearch(searchTerm.trim());
        if (term.isEmpty() || start < 0 || end > text.length() || start >= end) {
            return false;
        }
        String normalizedText = normalizeForSearch(text);
        int len = end - start;
        if (len != term.length()) {
            return false;
        }
        return normalizedText.regionMatches(true, start, term, 0, term.length());
    }

    /**
     * NFC normalization so composed Sinhala/Tamil characters match consistently after OCR.
     */
    public static String normalizeForSearch(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }
}

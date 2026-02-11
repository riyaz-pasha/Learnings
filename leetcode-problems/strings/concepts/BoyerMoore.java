import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class BoyerMooreBadChar {

    private final int ALPHABET_SIZE = 256;

    public List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        int[] badChar = this.buildBadCharTable(pattern);

        int textLen = text.length();
        int patternLen = pattern.length();

        int shift = 0;

        while (shift <= (textLen - patternLen)) {
            int patternIndex = patternLen - 1;

            while (patternIndex >= 0 && pattern.charAt(patternIndex) == text.charAt(shift + patternIndex)) {
                patternIndex--;
            }

            if (patternIndex < 0) {
                result.add(shift);

                // For a full match, the shift is determined by a separate rule (often good
                // suffix, but a simple default is 1 or more,
                // e.g., m - badChar[text.charAt(shift + m)] to avoid infinite loops if the
                // pattern repeats)
                shift += ((shift + patternLen) < textLen)
                        ? patternLen - badChar[text.charAt(shift + patternLen)]
                        : 1;
            } else {
                int badCharShift = patternIndex - badChar[text.charAt(shift + patternIndex)];
                shift += Math.max(1, badCharShift);
            }

        }

        return result;
    }

    private int[] buildBadCharTable(String pattern) {
        int[] badChar = new int[ALPHABET_SIZE];

        Arrays.fill(badChar, -1);

        for (int index = 0; index < pattern.length(); index++) {
            badChar[pattern.charAt(index)] = index;
        }

        return badChar;
    }

    /*
     * 1. The Bad Character Rule
     * The Bad Character Rule tells us how far to shift the pattern to the right
     * when a mismatch occurs. When a character in the text doesn't match the
     * corresponding character in the pattern, this rule uses the mismatched
     * character (the "bad character") to determine the next alignment. .
     * 
     * How it works:
     * - You compare the pattern and text from right to left.
     * 
     * - If a mismatch occurs at text[i] and pattern[j], where text[i] is the
     * "bad character," you find the last occurrence of text[i] in the pattern.
     * 
     * - Let's say the last occurrence is at index k in the pattern.
     * 
     * - You shift the pattern to the right so that the last occurrence of text[i]
     * in the pattern (pattern[k]) aligns with the bad character in the text
     * (text[i]).
     * 
     * - The shift amount is j - k.
     * 
     * - If the bad character doesn't appear in the pattern at all, you shift the
     * entire pattern past the bad character in the text, so the shift amount is j+1
     * 
     * To make this fast, the algorithm pre-computes a table for the pattern,
     * mapping each character in the alphabet to its rightmost position.
     */

}


/*
 * Boyer-Moore String Search (Bad Character Rule Only)
 *
 * Problem:
 * Find all occurrences of a pattern inside a text.
 *
 * Key Idea:
 * - Compare pattern with text from RIGHT to LEFT.
 * - On mismatch, shift pattern using "bad character rule":
 *
 *   shift = max(1, j - lastOccur[mismatchedChar])
 *
 * where:
 *   j = mismatch index in pattern
 *   lastOccur[c] = last index of character c in pattern
 *
 * Example:
 * text    = "ABAAABCD"
 * pattern = "ABC"
 *
 * We build lastOccur for pattern:
 * A -> 0
 * B -> 1
 * C -> 2
 *
 * Time Complexity:
 * - Best/Average: ~ O(n/m) (fast in practice)
 * - Worst case: O(n * m) (because we are using only bad-char rule)
 *
 * Space Complexity: O(256) = O(1)
 */
class BoyerMooreBadCharacter {

    private static final int CHAR_SIZE = 256; // ASCII size

    public List<Integer> search(String text, String pattern) {

        List<Integer> matches = new ArrayList<>();

        int n = text.length();
        int m = pattern.length();

        if (m == 0 || n < m) return matches;

        // ---------------------------------------------------------
        // Step 1: Preprocess pattern to build last occurrence table
        // ---------------------------------------------------------
        int[] lastOccur = buildLastOccur(pattern);

        // ---------------------------------------------------------
        // Step 2: Start matching pattern with text
        // ---------------------------------------------------------
        int shift = 0; // shift of pattern relative to text

        while (shift <= n - m) {

            int j = m - 1; // start comparing from rightmost character

            // Move left while characters match
            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                j--;
            }

            // If j < 0, we matched full pattern
            if (j < 0) {
                matches.add(shift);

                /*
                 * If pattern matched at shift,
                 * shift pattern forward by 1 (or more optimizations possible)
                 */
                shift += 1;
            } else {

                /*
                 * Mismatch happened at index j in pattern.
                 * textChar is the mismatched character in text.
                 */
                char textChar = text.charAt(shift + j);

                /*
                 * lastOccur[textChar] gives last index of this character in pattern.
                 * If character is not in pattern, lastOccur is -1.
                 *
                 * Bad Character Shift:
                 * shift by max(1, j - lastOccur[textChar])
                 *
                 * Why?
                 * - We try to align the mismatched character in text with its last
                 *   occurrence in pattern.
                 * - If character does not exist in pattern => shift beyond mismatch.
                 */
                int lastIndex = lastOccur[textChar];

                shift += Math.max(1, j - lastIndex);
            }
        }

        return matches;
    }

    /*
     * Builds last occurrence array:
     * lastOccur[c] = last index where character c appears in pattern.
     *
     * If a character is not present, it remains -1.
     */
    private int[] buildLastOccur(String pattern) {

        int[] lastOccur = new int[CHAR_SIZE];
        Arrays.fill(lastOccur, -1);

        for (int i = 0; i < pattern.length(); i++) {
            lastOccur[pattern.charAt(i)] = i;
        }

        return lastOccur;
    }

    // Example usage
    public static void main(String[] args) {

        BoyerMooreBadCharacter bm = new BoyerMooreBadCharacter();

        String text = "ABAAABCD";
        String pattern = "ABC";

        List<Integer> result = bm.search(text, pattern);

        System.out.println("Pattern found at indices: " + result);
    }
}

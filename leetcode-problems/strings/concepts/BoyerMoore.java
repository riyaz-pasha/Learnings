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

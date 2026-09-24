import java.util.ArrayList;
import java.util.List;

class KMP {

    public int search(String text, String pattern) {
        int textLength = text.length();
        int patternLength = pattern.length();

        int[] lps = this.buildLPS(pattern);
        int textIndex = 0, patternIndex = 0;

        while (textIndex < textLength) {

            if (text.charAt(textIndex) == pattern.charAt(patternIndex)) {
                textIndex++;
                patternIndex++;
            }

            if (patternIndex == patternLength) {
                // Found a match!
                return textIndex - patternIndex;
            } else if (textIndex < textLength && text.charAt(textIndex) != pattern.charAt(patternIndex)) {
                // Mismatch after patternIndex matches
                if (patternIndex != 0) {
                    // This is the core of KMP. Shift the pattern using the LPS array.
                    patternIndex = lps[patternIndex - 1];
                } else {
                    // No prefix to fall back on, just move to the next character in the text.
                    textIndex++;
                }
            }
        }

        return -1;
    }

    private int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];

        int length = 0; // Length of the previous longest prefix suffix
        int index = 1;
        lps[0] = 0; // The first element is always 0. Bacause no prefix or suffix to match

        while (index < m) {
            if (pattern.charAt(index) == pattern.charAt(length)) {
                length++;
                lps[index] = length;
                index++;
            } else {
                if (length != 0) {
                    // This is the "look-back" step
                    length = lps[length - 1];
                } else {
                    lps[index] = 0;
                    index++;
                }
            }
        }

        return lps;
    }

}

/*
 * KMP (Knuth-Morris-Pratt) Pattern Matching Algorithm
 *
 * Problem:
 * Find all occurrences of pattern in text.
 *
 * Key Idea:
 * - Avoid re-checking characters in text.
 * - Use LPS array (Longest Prefix which is also Suffix).
 *
 * lps[i] = length of the longest proper prefix of pattern[0..i]
 *          which is also a suffix of pattern[0..i]
 *
 * Example:
 * pattern = "ababaca"
 * lps     = [0,0,1,2,3,0,1]
 *
 * Time Complexity:
 * - LPS construction: O(m)
 * - Pattern search:   O(n)
 * Total: O(n + m)
 *
 * Space Complexity: O(m) for lps array
 */
class KMP2 {

    public List<Integer> search(String text, String pattern) {

        List<Integer> matches = new ArrayList<>();

        int n = text.length();
        int m = pattern.length();

        if (m == 0 || n < m) return matches;

        // Step 1: Build LPS array
        int[] lps = buildLPS(pattern);

        // Step 2: Search using two pointers
        int i = 0; // pointer for text
        int j = 0; // pointer for pattern

        while (i < n) {

            // Characters match -> move both pointers
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
            }

            // Full match found
            if (j == m) {

                // match ends at i-1, starts at i-m
                matches.add(i - m);

                /*
                 * Instead of restarting pattern from 0,
                 * jump using LPS to continue searching for overlapping matches.
                 */
                j = lps[j - 1];
            }

            // Mismatch after some matches
            else if (i < n && text.charAt(i) != pattern.charAt(j)) {

                /*
                 * If mismatch occurs:
                 *
                 * Case 1: j != 0
                 *   - we don't move i
                 *   - we shift pattern using LPS
                 *
                 * Case 2: j == 0
                 *   - no prefix matched, move i forward
                 */
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }

        return matches;
    }

    /*
     * Builds the LPS (Longest Prefix Suffix) array
     *
     * Example:
     * pattern = "ababaca"
     * lps     = [0,0,1,2,3,0,1]
     *
     * Meaning:
     * lps[4] = 3 because "ababa"
     * longest prefix == suffix is "aba" (length 3)
     */
    private int[] buildLPS(String pattern) {

        int m = pattern.length();
        int[] lps = new int[m];

        int len = 0; // length of current longest prefix-suffix
        int i = 1;   // start from second char

        lps[0] = 0;  // first char always 0

        while (i < m) {

            if (pattern.charAt(i) == pattern.charAt(len)) {

                len++;
                lps[i] = len;
                i++;
            } else {

                /*
                 * mismatch case:
                 *
                 * If len != 0, reduce len using previously computed lps.
                 * We do NOT move i yet.
                 */
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    // no prefix possible
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }
}

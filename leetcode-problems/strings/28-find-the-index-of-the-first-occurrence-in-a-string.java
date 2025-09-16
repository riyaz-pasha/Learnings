/*
 * Given two strings needle and haystack, return the index of the first
 * occurrence of needle in haystack, or -1 if needle is not part of haystack.
 * 
 * Example 1:
 * Input: haystack = "sadbutsad", needle = "sad"
 * Output: 0
 * Explanation: "sad" occurs at index 0 and 6.
 * The first occurrence is at index 0, so we return 0.
 * 
 * Example 2:
 * Input: haystack = "leetcode", needle = "leeto"
 * Output: -1
 * Explanation: "leeto" did not occur in "leetcode", so we return -1.
 */

import java.util.HashMap;
import java.util.Map;

class IndexOfFirstOccurrence {

    public int strStr(String haystack, String needle) {
        int n = haystack.length();
        int m = needle.length();
        int[] lps = this.buildLPS(needle);

        int textIndex = 0;
        int patternIndex = 0;
        while (textIndex < n) {
            if (haystack.charAt(textIndex) == needle.charAt(patternIndex)) {
                textIndex++;
                patternIndex++;
            }
            if (patternIndex == m) {
                return textIndex - m;
            } else if (textIndex < n && haystack.charAt(textIndex) != needle.charAt(patternIndex)) {
                if (patternIndex != 0) {
                    patternIndex = lps[patternIndex - 1];
                } else {
                    textIndex++;
                }
            }
        }
        return -1;
    }

    private int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int index = 1;
        int length = 0;
        lps[0] = 0;

        while (index < m) {
            if (pattern.charAt(index) == pattern.charAt(length)) {
                length++;
                lps[index] = length;
                index++;
            } else {
                if (length != 0) {
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

class IndexOfFirstOccurrenceBoyerMoore {

    public int strStr(String haystack, String needle) {
        int n = haystack.length();
        int m = needle.length();
        if (m == 0)
            return 0; // Edge case: empty pattern
        if (n < m)
            return -1;

        // Build last occurrence table
        Map<Character, Integer> lastOccurrence = buildLastOccurrence(needle);

        int shift = 0;
        while (shift <= n - m) {
            int j = m - 1;
            // Compare from right to left
            while (j >= 0 && needle.charAt(j) == haystack.charAt(shift + j)) {
                j--;
            }
            if (j < 0) {
                return shift; // found match
            }
            char badChar = haystack.charAt(shift + j);
            int lastIndex = lastOccurrence.getOrDefault(badChar, -1);
            int badCharShift = j - lastIndex;
            if (badCharShift < 1)
                badCharShift = 1; // must shift at least 1
            shift += badCharShift;
        }
        return -1;
    }

    private Map<Character, Integer> buildLastOccurrence(String pattern) {
        Map<Character, Integer> map = new HashMap<>();
        for (int i = 0; i < pattern.length(); i++) {
            map.put(pattern.charAt(i), i); // record last index of char
        }
        return map;
    }

}

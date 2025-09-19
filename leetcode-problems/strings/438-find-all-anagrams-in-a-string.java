import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
 * Given two strings s and p, return an array of all the start indices of p's
 * anagrams in s. You may return the answer in any order.
 * 
 * Example 1:
 * Input: s = "cbaebabacd", p = "abc"
 * Output: [0,6]
 * Explanation:
 * The substring with start index = 0 is "cba", which is an anagram of "abc".
 * The substring with start index = 6 is "bac", which is an anagram of "abc".
 * 
 * Example 2:
 * Input: s = "abab", p = "ab"
 * Output: [0,1,2]
 * Explanation:
 * The substring with start index = 0 is "ab", which is an anagram of "ab".
 * The substring with start index = 1 is "ba", which is an anagram of "ab".
 * The substring with start index = 2 is "ab", which is an anagram of "ab".
 */

class FindAllAnagramsInAString {

    public List<Integer> findAnagrams(String s, String t) {
        // build a freq map of t
        if (s == null || t == null || s.length() == 0 || s.length() < t.length()) {
            return List.of();
        }

        Map<Character, Integer> freqMap = new HashMap<>();
        for (char ch : t.toCharArray()) {
            freqMap.merge(ch, 1, Integer::sum);
        }

        int windowSize = t.length();
        Map<Character, Integer> windowMap = new HashMap<>();
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < s.length(); i++) {
            char newChar = s.charAt(i);
            if (freqMap.containsKey(newChar)) {
                windowMap.merge(newChar, 1, Integer::sum);
            }
            if (i < windowSize - 1) {
                continue;
            }
            if (windowMap.equals(freqMap)) {
                result.add(i - windowSize + 1);
            }
            char oldChar = s.charAt(i - windowSize + 1);
            windowMap.computeIfPresent(oldChar, (_, val) -> val == 1 ? null : val - 1);
        }

        return result;
    }

}

class FindAnagrams {

    // Solution 1: Sliding Window with Character Count (Most Efficient)
    // Time: O(n), Space: O(1) - since we only have 26 lowercase letters
    public List<Integer> findAnagrams1(String s, String p) {
        List<Integer> result = new ArrayList<>();
        if (s.length() < p.length())
            return result;

        int[] pCount = new int[26]; // Count of characters in p
        int[] windowCount = new int[26]; // Count of characters in current window

        // Initialize character counts for pattern p
        for (char c : p.toCharArray()) {
            pCount[c - 'a']++;
        }

        int windowSize = p.length();

        // Initialize the first window
        for (int i = 0; i < windowSize; i++) {
            windowCount[s.charAt(i) - 'a']++;
        }

        // Check if first window is an anagram
        if (Arrays.equals(pCount, windowCount)) {
            result.add(0);
        }

        // Slide the window
        for (int i = windowSize; i < s.length(); i++) {
            // Add new character to window
            windowCount[s.charAt(i) - 'a']++;
            // Remove character that's no longer in window
            windowCount[s.charAt(i - windowSize) - 'a']--;

            // Check if current window is an anagram
            if (Arrays.equals(pCount, windowCount)) {
                result.add(i - windowSize + 1);
            }
        }

        return result;
    }

    // Solution 2: Sliding Window with HashMap
    // Time: O(n), Space: O(k) where k is number of unique characters in p
    public List<Integer> findAnagrams2(String s, String p) {
        List<Integer> result = new ArrayList<>();
        if (s.length() < p.length())
            return result;

        Map<Character, Integer> pMap = new HashMap<>();
        Map<Character, Integer> windowMap = new HashMap<>();

        // Build frequency map for p
        for (char c : p.toCharArray()) {
            pMap.put(c, pMap.getOrDefault(c, 0) + 1);
        }

        int windowSize = p.length();

        // Initialize first window
        for (int i = 0; i < windowSize; i++) {
            char c = s.charAt(i);
            windowMap.put(c, windowMap.getOrDefault(c, 0) + 1);
        }

        if (windowMap.equals(pMap)) {
            result.add(0);
        }

        // Slide the window
        for (int i = windowSize; i < s.length(); i++) {
            // Add new character
            char newChar = s.charAt(i);
            windowMap.put(newChar, windowMap.getOrDefault(newChar, 0) + 1);

            // Remove old character
            char oldChar = s.charAt(i - windowSize);
            windowMap.put(oldChar, windowMap.get(oldChar) - 1);
            if (windowMap.get(oldChar) == 0) {
                windowMap.remove(oldChar);
            }

            if (windowMap.equals(pMap)) {
                result.add(i - windowSize + 1);
            }
        }

        return result;
    }

    // Solution 3: Optimized Sliding Window with Match Count
    // Time: O(n), Space: O(1)
    public List<Integer> findAnagrams3(String s, String p) {
        List<Integer> result = new ArrayList<>();
        if (s.length() < p.length())
            return result;

        int[] count = new int[26];

        // Count characters in p (positive) and first window of s (negative)
        for (int i = 0; i < p.length(); i++) {
            count[p.charAt(i) - 'a']++;
            count[s.charAt(i) - 'a']--;
        }

        int matches = 0; // Number of characters with correct frequency
        for (int i = 0; i < 26; i++) {
            if (count[i] == 0)
                matches++;
        }

        if (matches == 26)
            result.add(0);

        // Slide the window
        for (int i = p.length(); i < s.length(); i++) {
            // Add new character
            int newIdx = s.charAt(i) - 'a';
            if (count[newIdx] == 0)
                matches--;
            count[newIdx]--;
            if (count[newIdx] == 0)
                matches++;

            // Remove old character
            int oldIdx = s.charAt(i - p.length()) - 'a';
            if (count[oldIdx] == 0)
                matches--;
            count[oldIdx]++;
            if (count[oldIdx] == 0)
                matches++;

            if (matches == 26) {
                result.add(i - p.length() + 1);
            }
        }

        return result;
    }

    // Solution 4: Brute Force with Sorting (Less Efficient)
    // Time: O(n * m * log m) where n = s.length, m = p.length
    // Space: O(m)
    public List<Integer> findAnagrams4(String s, String p) {
        List<Integer> result = new ArrayList<>();
        if (s.length() < p.length())
            return result;

        String sortedP = sortString(p);

        for (int i = 0; i <= s.length() - p.length(); i++) {
            String substring = s.substring(i, i + p.length());
            if (sortString(substring).equals(sortedP)) {
                result.add(i);
            }
        }

        return result;
    }

    private String sortString(String str) {
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    // Test the solutions
    public static void main(String[] args) {
        FindAnagrams solution = new FindAnagrams();

        // Test case 1
        String s1 = "cbaebabacd";
        String p1 = "abc";
        System.out.println("Input: s = \"" + s1 + "\", p = \"" + p1 + "\"");
        System.out.println("Solution 1: " + solution.findAnagrams1(s1, p1));
        System.out.println("Solution 2: " + solution.findAnagrams2(s1, p1));
        System.out.println("Solution 3: " + solution.findAnagrams3(s1, p1));
        System.out.println("Solution 4: " + solution.findAnagrams4(s1, p1));
        System.out.println();

        // Test case 2
        String s2 = "abab";
        String p2 = "ab";
        System.out.println("Input: s = \"" + s2 + "\", p = \"" + p2 + "\"");
        System.out.println("Solution 1: " + solution.findAnagrams1(s2, p2));
        System.out.println("Solution 2: " + solution.findAnagrams2(s2, p2));
        System.out.println("Solution 3: " + solution.findAnagrams3(s2, p2));
        System.out.println("Solution 4: " + solution.findAnagrams4(s2, p2));
    }

}

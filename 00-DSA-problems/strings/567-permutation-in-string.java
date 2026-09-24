import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
/*
 * Given two strings s1 and s2, return true if s2 contains a permutation of s1,
 * or false otherwise.
 * 
 * In other words, return true if one of s1's permutations is the substring of
 * s2.
 * 
 * Example 1:
 * Input: s1 = "ab", s2 = "eidbaooo"
 * Output: true
 * Explanation: s2 contains one permutation of s1 ("ba").
 * 
 * Example 2:
 * Input: s1 = "ab", s2 = "eidboaoo"
 * Output: false
 */

class PermutationInStringSolution {

    public boolean checkInclusion(String s1, String s2) {
        // build freq map of s1
        // add characters to a charMap until window is filled
        // once it is filled we compare charcters in charMap with freqMap and if it's
        // matching returns true

        Map<Character, Integer> s1FreqMap = new HashMap<>();
        for (char ch : s1.toCharArray()) {
            s1FreqMap.merge(ch, 1, Integer::sum);
        }

        int windowStart = 0;
        int windowLen = 0;
        Map<Character, Integer> windowMap = new HashMap<>();
        for (int windowEnd = 0; windowEnd < s2.length(); windowEnd++) {
            char ch = s2.charAt(windowEnd);
            windowMap.merge(ch, 1, Integer::sum);
            windowLen++;
            if (windowLen == s1.length()) {
                boolean isFound = true;
                for (Map.Entry<Character, Integer> entry : s1FreqMap.entrySet()) {
                    if (!windowMap.containsKey(entry.getKey())
                            || !windowMap.get(entry.getKey()).equals(entry.getValue())) {
                        isFound = false;
                        break;
                    }
                }
                if (isFound) {
                    return true;
                }
                char startChar = s2.charAt(windowStart);
                windowMap.compute(startChar, (_, val) -> val == 1 ? 0 : val - 1);
                windowLen--;
                windowStart++;
            }
        }
        return false;
    }

}

class PermutationInString {

    /**
     * Sliding Window Solution with HashMap
     * Time Complexity: O(|s1| + |s2|)
     * Space Complexity: O(|s1|)
     */
    public boolean checkInclusion(String s1, String s2) {
        if (s1.length() > s2.length()) {
            return false;
        }

        // Count characters in s1
        Map<Character, Integer> s1Count = new HashMap<>();
        for (char c : s1.toCharArray()) {
            s1Count.put(c, s1Count.getOrDefault(c, 0) + 1);
        }

        int windowSize = s1.length();
        Map<Character, Integer> windowCount = new HashMap<>();

        // Initialize the first window
        for (int i = 0; i < windowSize; i++) {
            char c = s2.charAt(i);
            windowCount.put(c, windowCount.getOrDefault(c, 0) + 1);
        }

        // Check if first window matches
        if (windowCount.equals(s1Count)) {
            return true;
        }

        // Slide the window
        for (int i = windowSize; i < s2.length(); i++) {
            // Add new character to window
            char newChar = s2.charAt(i);
            windowCount.put(newChar, windowCount.getOrDefault(newChar, 0) + 1);

            // Remove character going out of window
            char oldChar = s2.charAt(i - windowSize);
            windowCount.put(oldChar, windowCount.get(oldChar) - 1);
            if (windowCount.get(oldChar) == 0) {
                windowCount.remove(oldChar);
            }

            // Check if current window matches
            if (windowCount.equals(s1Count)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Optimized Array-based Solution (for lowercase letters only)
     * Time Complexity: O(|s1| + |s2|)
     * Space Complexity: O(1) - fixed size array
     */
    public boolean checkInclusionArray(String s1, String s2) {
        if (s1.length() > s2.length()) {
            return false;
        }

        int[] s1Count = new int[26];
        int[] windowCount = new int[26];

        // Count characters in s1 and initialize first window
        for (int i = 0; i < s1.length(); i++) {
            s1Count[s1.charAt(i) - 'a']++;
            windowCount[s2.charAt(i) - 'a']++;
        }

        // Check if first window matches
        if (Arrays.equals(s1Count, windowCount)) {
            return true;
        }

        // Slide the window
        for (int i = s1.length(); i < s2.length(); i++) {
            // Add new character and remove old character
            windowCount[s2.charAt(i) - 'a']++;
            windowCount[s2.charAt(i - s1.length()) - 'a']--;

            if (Arrays.equals(s1Count, windowCount)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Most Optimized Solution using matches counter
     * Time Complexity: O(|s1| + |s2|)
     * Space Complexity: O(1)
     */
    public boolean checkInclusionOptimal(String s1, String s2) {
        if (s1.length() > s2.length()) {
            return false;
        }

        int[] s1Count = new int[26];
        int[] windowCount = new int[26];

        // Count characters in s1
        for (char c : s1.toCharArray()) {
            s1Count[c - 'a']++;
        }

        int matches = 0; // Number of characters with matching frequencies

        // Process the sliding window
        for (int i = 0; i < s2.length(); i++) {
            int charIndex = s2.charAt(i) - 'a';

            // Add current character to window
            windowCount[charIndex]++;
            if (windowCount[charIndex] == s1Count[charIndex]) {
                matches++;
            } else if (windowCount[charIndex] == s1Count[charIndex] + 1) {
                matches--;
            }

            // Remove character going out of window (if window size exceeded)
            if (i >= s1.length()) {
                int oldCharIndex = s2.charAt(i - s1.length()) - 'a';
                if (windowCount[oldCharIndex] == s1Count[oldCharIndex]) {
                    matches--;
                } else if (windowCount[oldCharIndex] == s1Count[oldCharIndex] + 1) {
                    matches++;
                }
                windowCount[oldCharIndex]--;
            }

            // Check if we have a permutation
            if (matches == 26) {
                return true;
            }
        }

        return false;
    }

    /**
     * Alternative solution using frequency difference tracking
     * Time Complexity: O(|s1| + |s2|)
     * Space Complexity: O(1)
     */
    public boolean checkInclusionFreqDiff(String s1, String s2) {
        if (s1.length() > s2.length()) {
            return false;
        }

        int[] freq = new int[26];

        // Initialize frequency array with s1 counts (negative) and first window of s2
        // (positive)
        for (int i = 0; i < s1.length(); i++) {
            freq[s1.charAt(i) - 'a']--;
            freq[s2.charAt(i) - 'a']++;
        }

        if (allZero(freq))
            return true;

        // Slide the window
        for (int i = s1.length(); i < s2.length(); i++) {
            // Add new character
            freq[s2.charAt(i) - 'a']++;
            // Remove old character
            freq[s2.charAt(i - s1.length()) - 'a']--;

            if (allZero(freq))
                return true;
        }

        return false;
    }

    private boolean allZero(int[] freq) {
        for (int f : freq) {
            if (f != 0)
                return false;
        }
        return true;
    }

    // Test method
    public static void main(String[] args) {
        PermutationInString solution = new PermutationInString();

        // Test cases
        System.out.println("HashMap Solution:");
        System.out.println("Test 1: " + solution.checkInclusion("ab", "eidbaooo")); // Expected: true
        System.out.println("Test 2: " + solution.checkInclusion("ab", "eidboaoo")); // Expected: false
        System.out.println("Test 3: " + solution.checkInclusion("adc", "dcda")); // Expected: true
        System.out.println("Test 4: " + solution.checkInclusion("hello", "ooolleoooleh")); // Expected: false

        System.out.println("\nArray Solution:");
        System.out.println("Test 1: " + solution.checkInclusionArray("ab", "eidbaooo")); // Expected: true
        System.out.println("Test 2: " + solution.checkInclusionArray("ab", "eidboaoo")); // Expected: false
        System.out.println("Test 3: " + solution.checkInclusionArray("adc", "dcda")); // Expected: true

        System.out.println("\nOptimal Solution:");
        System.out.println("Test 1: " + solution.checkInclusionOptimal("ab", "eidbaooo")); // Expected: true
        System.out.println("Test 2: " + solution.checkInclusionOptimal("ab", "eidboaoo")); // Expected: false
        System.out.println("Test 3: " + solution.checkInclusionOptimal("adc", "dcda")); // Expected: true

        System.out.println("\nFrequency Difference Solution:");
        System.out.println("Test 1: " + solution.checkInclusionFreqDiff("ab", "eidbaooo")); // Expected: true
        System.out.println("Test 2: " + solution.checkInclusionFreqDiff("ab", "eidboaoo")); // Expected: false
        System.out.println("Test 3: " + solution.checkInclusionFreqDiff("adc", "dcda")); // Expected: true
    }
}

/**
 * Algorithm Explanations:
 * 
 * 1. HashMap Solution:
 * - Use HashMap to count character frequencies
 * - Maintain a sliding window of size |s1|
 * - Compare window frequency with s1 frequency
 * 
 * 2. Array Solution:
 * - Use fixed-size array for lowercase letters (a-z)
 * - More efficient than HashMap for this specific case
 * - Uses Arrays.equals() for comparison
 * 
 * 3. Optimal Solution:
 * - Track number of characters with matching frequencies
 * - Only need to check if matches == 26 (all characters match)
 * - Avoids expensive array comparisons
 * 
 * 4. Frequency Difference Solution:
 * - Use single array to track frequency differences
 * - s1 characters are negative, window characters are positive
 * - All zeros means perfect match
 * 
 * All solutions use sliding window technique:
 * Time Complexity: O(|s1| + |s2|)
 * Space Complexity: O(1) for array-based, O(|s1|) for HashMap
 */

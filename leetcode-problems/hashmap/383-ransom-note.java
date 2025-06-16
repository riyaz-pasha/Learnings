import java.util.*;
/*
 * Given two strings ransomNote and magazine, return true if ransomNote can be
 * constructed by using the letters from magazine and false otherwise.
 * 
 * Each letter in magazine can only be used once in ransomNote.
 * 
 * Example 1:
 * Input: ransomNote = "a", magazine = "b"
 * Output: false
 * 
 * Example 2:
 * Input: ransomNote = "aa", magazine = "ab"
 * Output: false
 * 
 * Example 3:
 * Input: ransomNote = "aa", magazine = "aab"
 * Output: true
 */

class RansomNote {

    // Solution 1: Using HashMap - Most intuitive approach
    // Time: O(m + n), Space: O(k) where k is unique characters in magazine
    public boolean canConstruct1(String ransomNote, String magazine) {
        Map<Character, Integer> charCount = new HashMap<>();

        // Count characters in magazine
        for (char c : magazine.toCharArray()) {
            charCount.put(c, charCount.getOrDefault(c, 0) + 1);
        }

        // Check if ransom note can be constructed
        for (char c : ransomNote.toCharArray()) {
            int count = charCount.getOrDefault(c, 0);
            if (count == 0) {
                return false;
            }
            charCount.put(c, count - 1);
        }

        return true;
    }

    // Solution 2: Using int array (most efficient for lowercase letters)
    // Time: O(m + n), Space: O(1) - constant space for 26 letters
    public boolean canConstruct2(String ransomNote, String magazine) {
        // Assuming only lowercase English letters
        int[] letterCounts = new int[26];

        // Count letters in magazine
        for (char c : magazine.toCharArray()) {
            letterCounts[c - 'a']++;
        }

        // Check ransom note
        for (char c : ransomNote.toCharArray()) {
            if (--letterCounts[c - 'a'] < 0) {
                return false;
            }
        }

        return true;
    }

    // Solution 3: Two-pointer approach with sorting
    // Time: O(m log m + n log n), Space: O(m + n) for char arrays
    public boolean canConstruct3(String ransomNote, String magazine) {
        char[] ransomChars = ransomNote.toCharArray();
        char[] magazineChars = magazine.toCharArray();

        Arrays.sort(ransomChars);
        Arrays.sort(magazineChars);

        int i = 0, j = 0;
        while (i < ransomChars.length && j < magazineChars.length) {
            if (ransomChars[i] == magazineChars[j]) {
                i++;
                j++;
            } else if (ransomChars[i] > magazineChars[j]) {
                j++;
            } else {
                return false;
            }
        }

        return i == ransomChars.length;
    }

    // Solution 4: Single pass with character removal (modifies strings)
    // Time: O(m * n) worst case, Space: O(n) for StringBuilder
    public boolean canConstruct4(String ransomNote, String magazine) {
        StringBuilder sb = new StringBuilder(magazine);

        for (char c : ransomNote.toCharArray()) {
            int index = sb.indexOf(String.valueOf(c));
            if (index == -1) {
                return false;
            }
            sb.deleteCharAt(index);
        }

        return true;
    }

    // Solution 5: Optimized array approach with early termination
    // Time: O(m + n), Space: O(1)
    public boolean canConstruct5(String ransomNote, String magazine) {
        if (ransomNote.length() > magazine.length()) {
            return false; // Early termination
        }

        int[] counts = new int[26];

        // Single pass: increment for magazine, decrement for ransom note
        for (int i = 0; i < magazine.length(); i++) {
            counts[magazine.charAt(i) - 'a']++;
            if (i < ransomNote.length()) {
                counts[ransomNote.charAt(i) - 'a']--;
            }
        }

        // Process remaining ransom note characters
        for (int i = magazine.length(); i < ransomNote.length(); i++) {
            counts[ransomNote.charAt(i) - 'a']--;
        }

        // Check if any count is negative
        for (int count : counts) {
            if (count < 0) {
                return false;
            }
        }

        return true;
    }

    // Test method
    public static void main(String[] args) {
        RansomNote solution = new RansomNote();

        // Test cases
        System.out.println(solution.canConstruct2("a", "b")); // false
        System.out.println(solution.canConstruct2("aa", "ab")); // false
        System.out.println(solution.canConstruct2("aa", "aab")); // true
        System.out.println(solution.canConstruct2("aab", "baa")); // true
        System.out.println(solution.canConstruct2("aaa", "aa")); // false
    }

}

/*
 * Analysis of Solutions:
 * 
 * 1. HashMap Approach (canConstruct1):
 * - Most readable and intuitive
 * - Works with any character set
 * - Good for interview discussions
 * 
 * 2. Array Approach (canConstruct2):
 * - Most efficient for lowercase English letters
 * - Constant space complexity
 * - Best performance for the given constraints
 * 
 * 3. Sorting Approach (canConstruct3):
 * - Different algorithmic approach
 * - Higher time complexity due to sorting
 * - Good to show alternative thinking
 * 
 * 4. String Manipulation (canConstruct4):
 * - Less efficient but demonstrates StringBuilder usage
 * - O(m*n) time complexity in worst case
 * - Not recommended for large inputs
 * 
 * 5. Optimized Array (canConstruct5):
 * - Single pass optimization
 * - Early termination for impossible cases
 * - Slightly more complex but very efficient
 * 
 * Recommendation: Use canConstruct2 (array approach) for best performance
 * with lowercase letters, or canConstruct1 (HashMap) for general cases.
 */

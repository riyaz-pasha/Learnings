import java.util.*;
/*
 * Given two strings s and t, return true if t is an anagram of s, and false
 * otherwise.
 * 
 * Example 1:
 * Input: s = "anagram", t = "nagaram"
 * Output: true
 * 
 * Example 2:
 * Input: s = "rat", t = "car"
 * Output: false
 */

class ValidAnagram {

    // Solution 1: Sorting approach - Most intuitive
    // Time: O(n log n), Space: O(n) for char arrays
    public boolean isAnagram1(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        char[] sChars = s.toCharArray();
        char[] tChars = t.toCharArray();

        Arrays.sort(sChars);
        Arrays.sort(tChars);

        return Arrays.equals(sChars, tChars);
    }

    // Solution 2: Frequency counting with HashMap
    // Time: O(n), Space: O(k) where k is unique characters
    public boolean isAnagram2(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Integer> charCount = new HashMap<>();

        // Count characters in s
        for (char c : s.toCharArray()) {
            charCount.put(c, charCount.getOrDefault(c, 0) + 1);
        }

        // Subtract characters in t
        for (char c : t.toCharArray()) {
            int count = charCount.getOrDefault(c, 0);
            if (count == 0) {
                return false; // Character not in s or already used up
            }
            charCount.put(c, count - 1);
        }

        return true;
    }

    // Solution 3: Array frequency counting - Most efficient
    // Time: O(n), Space: O(1) - constant space for 26 letters
    public boolean isAnagram3(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] charCount = new int[26];

        // Single pass: increment for s, decrement for t
        for (int i = 0; i < s.length(); i++) {
            charCount[s.charAt(i) - 'a']++;
            charCount[t.charAt(i) - 'a']--;
        }

        // Check if all counts are zero
        for (int count : charCount) {
            if (count != 0) {
                return false;
            }
        }

        return true;
    }

    // Solution 4: Two-pass array counting
    // Time: O(n), Space: O(1)
    public boolean isAnagram4(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] charCount = new int[26];

        // Count characters in s
        for (char c : s.toCharArray()) {
            charCount[c - 'a']++;
        }

        // Subtract characters in t
        for (char c : t.toCharArray()) {
            if (--charCount[c - 'a'] < 0) {
                return false; // More occurrences in t than in s
            }
        }

        return true;
    }

    // Solution 5: Optimized with early termination
    // Time: O(n), Space: O(1)
    public boolean isAnagram5(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        // Quick check for same reference
        if (s == t) {
            return true;
        }

        int[] charCount = new int[26];
        int nonZeroCount = 0; // Track number of non-zero elements

        for (int i = 0; i < s.length(); i++) {
            char sChar = s.charAt(i);
            char tChar = t.charAt(i);

            // Update count for s character
            if (charCount[sChar - 'a']++ == 0) {
                nonZeroCount++;
            } else if (charCount[sChar - 'a'] == 0) {
                nonZeroCount--;
            }

            // Update count for t character
            if (charCount[tChar - 'a']-- == 0) {
                nonZeroCount++;
            } else if (charCount[tChar - 'a'] == 0) {
                nonZeroCount--;
            }
        }

        return nonZeroCount == 0;
    }

    // Solution 6: XOR approach (creative but less practical)
    // Time: O(n), Space: O(1)
    // Note: This only works if characters appear even number of times total
    public boolean isAnagram6(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int xor = 0;

        // XOR all characters from both strings
        for (int i = 0; i < s.length(); i++) {
            xor ^= s.charAt(i) ^ t.charAt(i);
        }

        // If anagrams, XOR should be 0
        // BUT this approach has false positives!
        // Example: "ab" and "ba" -> XOR = 0, but need frequency check

        // Still need frequency validation for correctness
        return xor == 0 && isAnagram3(s, t);
    }

    // Solution 7: Stream API approach (functional style)
    // Time: O(n log n), Space: O(n)
    public boolean isAnagram7(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        return s.chars()
                .sorted()
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString()
                .equals(
                        t.chars()
                                .sorted()
                                .collect(StringBuilder::new,
                                        StringBuilder::appendCodePoint,
                                        StringBuilder::append)
                                .toString());
    }

    // Solution 8: Prime number multiplication (mathematical approach)
    // Time: O(n), Space: O(1)
    // Note: Risk of integer overflow for large strings
    public boolean isAnagram8(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        // Prime numbers for each letter a-z
        long[] primes = { 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41,
                43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101 };

        long sProduct = 1;
        long tProduct = 1;

        for (int i = 0; i < s.length(); i++) {
            sProduct *= primes[s.charAt(i) - 'a'];
            tProduct *= primes[t.charAt(i) - 'a'];

            // Check for overflow (primitive overflow detection)
            if (sProduct < 0 || tProduct < 0) {
                // Fallback to safe method
                return isAnagram3(s, t);
            }
        }

        return sProduct == tProduct;
    }

    // Solution 9: Character sum comparison (flawed approach - for educational
    // purposes)
    // Time: O(n), Space: O(1)
    // WARNING: This approach has false positives!
    public boolean isAnagram9(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int sSum = 0, tSum = 0;

        for (int i = 0; i < s.length(); i++) {
            sSum += s.charAt(i);
            tSum += t.charAt(i);
        }

        // This is INCORRECT! Example: "ac" and "bb" have same sum
        // Keeping for educational purposes to show why this doesn't work
        return sSum == tSum && isAnagram3(s, t); // Need proper validation
    }

    // Test method
    public static void main(String[] args) {
        ValidAnagram solution = new ValidAnagram();

        // Test cases
        System.out.println(solution.isAnagram3("anagram", "nagaram")); // true
        System.out.println(solution.isAnagram3("rat", "car")); // false
        System.out.println(solution.isAnagram3("listen", "silent")); // true
        System.out.println(solution.isAnagram3("evil", "vile")); // true
        System.out.println(solution.isAnagram3("a", "ab")); // false
        System.out.println(solution.isAnagram3("ab", "ba")); // true
        System.out.println(solution.isAnagram3("", "")); // true
    }

}

/*
 * Analysis of Solutions:
 * 
 * 1. Sorting Approach (isAnagram1):
 * - Most intuitive and easy to understand
 * - Good for interviews to show basic understanding
 * - Time: O(n log n), Space: O(n)
 * 
 * 2. HashMap Frequency (isAnagram2):
 * - Clear logic with frequency counting
 * - Works with any character set
 * - Time: O(n), Space: O(k) where k = unique chars
 * 
 * 3. Array Frequency - Single Pass (isAnagram3):
 * - Most efficient for lowercase letters
 * - Optimal time and space complexity
 * - Time: O(n), Space: O(1)
 * 
 * 4. Array Frequency - Two Pass (isAnagram4):
 * - Similar to solution 3 but with early termination
 * - Slightly more readable
 * - Time: O(n), Space: O(1)
 * 
 * 5. Optimized Early Termination (isAnagram5):
 * - Advanced optimization with non-zero tracking
 * - Complex but very efficient
 * - Time: O(n), Space: O(1)
 * 
 * 6. XOR Approach (isAnagram6):
 * - Creative but flawed without additional validation
 * - Educational value for bit manipulation
 * - Time: O(n), Space: O(1)
 * 
 * 7. Stream API (isAnagram7):
 * - Functional programming style
 * - Clean but less efficient due to sorting
 * - Time: O(n log n), Space: O(n)
 * 
 * 8. Prime Multiplication (isAnagram8):
 * - Mathematical approach using unique prime factorization
 * - Risk of overflow with large inputs
 * - Time: O(n), Space: O(1)
 * 
 * 9. Character Sum (isAnagram9):
 * - Demonstrates why simple sum comparison fails
 * - Educational purposes only
 * - Time: O(n), Space: O(1) but INCORRECT
 * 
 * Best Solutions:
 * - Production: isAnagram3 (array frequency, single pass)
 * - Interview: isAnagram1 (sorting) then optimize to isAnagram3
 * - Readability: isAnagram4 (array frequency, two pass)
 * 
 * Key Insights:
 * - Early length check is crucial for optimization
 * - Array approach beats HashMap for limited character sets
 * - Single pass is more efficient than multiple passes
 * - Avoid mathematical shortcuts that can have false positives
 */

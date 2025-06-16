import java.util.*;
/*
 * You are given a string s and an array of strings words. All the strings of
 * words are of the same length.
 * 
 * A concatenated string is a string that exactly contains all the strings of
 * any permutation of words concatenated.
 * 
 * For example, if words = ["ab","cd","ef"], then "abcdef", "abefcd", "cdabef",
 * "cdefab", "efabcd", and "efcdab" are all concatenated strings. "acdbef" is
 * not a concatenated string because it is not the concatenation of any
 * permutation of words.
 * Return an array of the starting indices of all the concatenated substrings in
 * s. You can return the answer in any order.
 * 
 * Example 1:
 * Input: s = "barfoothefoobarman", words = ["foo","bar"]
 * Output: [0,9]
 * Explanation:
 * The substring starting at 0 is "barfoo". It is the concatenation of
 * ["bar","foo"] which is a permutation of words.
 * The substring starting at 9 is "foobar". It is the concatenation of
 * ["foo","bar"] which is a permutation of words.
 * 
 * Example 2:
 * Input: s = "wordgoodgoodgoodbestword", words = ["word","good","best","word"]
 * Output: []
 * Explanation:
 * There is no concatenated substring.
 * 
 * Example 3:
 * Input: s = "barfoofoobarthefoobarman", words = ["bar","foo","the"]
 * Output: [6,9,12]
 * Explanation:
 * The substring starting at 6 is "foobarthe". It is the concatenation of
 * ["foo","bar","the"].
 * The substring starting at 9 is "barthefoo". It is the concatenation of
 * ["bar","the","foo"].
 * The substring starting at 12 is "thefoobar". It is the concatenation of
 * ["the","foo","bar"].
 */

class SubstringConcatenationAllWords {

    // Solution 1: Brute Force with HashMap
    // Time Complexity: O(n * m * len), Space Complexity: O(m)
    // where n = s.length(), m = words.length, len = word length
    public List<Integer> findSubstringBruteForce(String s, String[] words) {
        List<Integer> result = new ArrayList<>();
        if (s == null || words == null || words.length == 0) {
            return result;
        }

        int wordLen = words[0].length();
        int totalLen = words.length * wordLen;
        int n = s.length();

        // Create frequency map of words
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }

        // Check each possible starting position
        for (int i = 0; i <= n - totalLen; i++) {
            if (isValidConcatenation(s, i, wordLen, wordCount)) {
                result.add(i);
            }
        }

        return result;
    }

    // Helper method for brute force
    private boolean isValidConcatenation(String s, int start, int wordLen, Map<String, Integer> wordCount) {
        Map<String, Integer> seen = new HashMap<>();
        int totalWords = wordCount.values().stream().mapToInt(Integer::intValue).sum();

        for (int i = 0; i < totalWords; i++) {
            int wordStart = start + i * wordLen;
            String word = s.substring(wordStart, wordStart + wordLen);

            seen.put(word, seen.getOrDefault(word, 0) + 1);

            // If word not in original list or count exceeds required
            if (!wordCount.containsKey(word) || seen.get(word) > wordCount.get(word)) {
                return false;
            }
        }

        return seen.equals(wordCount);
    }

    // Solution 2: Optimized Brute Force
    // Time Complexity: O(n * m * len), Space Complexity: O(m)
    public List<Integer> findSubstringOptimized(String s, String[] words) {
        List<Integer> result = new ArrayList<>();
        if (s == null || words == null || words.length == 0) {
            return result;
        }

        int wordLen = words[0].length();
        int totalLen = words.length * wordLen;
        int n = s.length();

        if (n < totalLen)
            return result;

        // Create frequency map of words
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }

        // Check each possible starting position
        for (int i = 0; i <= n - totalLen; i++) {
            Map<String, Integer> seen = new HashMap<>();
            int j = 0;

            // Check each word in the potential concatenation
            while (j < words.length) {
                int wordStart = i + j * wordLen;
                String word = s.substring(wordStart, wordStart + wordLen);

                if (!wordCount.containsKey(word)) {
                    break;
                }

                seen.put(word, seen.getOrDefault(word, 0) + 1);

                if (seen.get(word) > wordCount.get(word)) {
                    break;
                }

                j++;
            }

            if (j == words.length) {
                result.add(i);
            }
        }

        return result;
    }

    // Solution 3: Sliding Window Approach (Optimal)
    // Time Complexity: O(n * len), Space Complexity: O(m)
    public List<Integer> findSubstring(String s, String[] words) {
        List<Integer> result = new ArrayList<>();
        if (s == null || words == null || words.length == 0) {
            return result;
        }

        int wordLen = words[0].length();
        int totalWords = words.length;
        int totalLen = totalWords * wordLen;
        int n = s.length();

        if (n < totalLen)
            return result;

        // Create frequency map of words
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }

        // Try starting from each possible offset (0 to wordLen-1)
        for (int offset = 0; offset < wordLen; offset++) {
            slidingWindow(s, offset, wordLen, totalWords, wordCount, result);
        }

        return result;
    }

    // Sliding window helper method
    private void slidingWindow(String s, int offset, int wordLen, int totalWords,
            Map<String, Integer> wordCount, List<Integer> result) {
        Map<String, Integer> seen = new HashMap<>();
        int left = offset;
        int count = 0; // number of valid words in current window

        // Move through string with step = wordLen
        for (int right = offset; right + wordLen <= s.length(); right += wordLen) {
            String word = s.substring(right, right + wordLen);

            if (wordCount.containsKey(word)) {
                seen.put(word, seen.getOrDefault(word, 0) + 1);
                count++;

                // If we have too many of this word, shrink window from left
                while (seen.get(word) > wordCount.get(word)) {
                    String leftWord = s.substring(left, left + wordLen);
                    seen.put(leftWord, seen.get(leftWord) - 1);
                    count--;
                    left += wordLen;
                }

                // If we have exactly the right number of words, we found a match
                if (count == totalWords) {
                    result.add(left);
                    // Move left pointer to start looking for next match
                    String leftWord = s.substring(left, left + wordLen);
                    seen.put(leftWord, seen.get(leftWord) - 1);
                    count--;
                    left += wordLen;
                }
            } else {
                // Reset window if we encounter a word not in the list
                seen.clear();
                count = 0;
                left = right + wordLen;
            }
        }
    }

    // Solution 4: Sliding Window with Detailed Tracking
    // Time Complexity: O(n * len), Space Complexity: O(m)
    public List<Integer> findSubstringDetailed(String s, String[] words) {
        List<Integer> result = new ArrayList<>();
        if (s == null || words == null || words.length == 0) {
            return result;
        }

        int wordLen = words[0].length();
        int totalWords = words.length;
        int totalLen = totalWords * wordLen;
        int n = s.length();

        System.out.println("Input string: \"" + s + "\"");
        System.out.println("Words: " + Arrays.toString(words));
        System.out.println("Word length: " + wordLen + ", Total words: " + totalWords);
        System.out.println();

        if (n < totalLen)
            return result;

        // Create frequency map of words
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        System.out.println("Word frequency map: " + wordCount);
        System.out.println();

        // Try starting from each possible offset
        for (int offset = 0; offset < wordLen; offset++) {
            System.out.println("=== Sliding window starting at offset " + offset + " ===");
            slidingWindowDetailed(s, offset, wordLen, totalWords, wordCount, result);
            System.out.println();
        }

        return result;
    }

    // Detailed sliding window helper
    private void slidingWindowDetailed(String s, int offset, int wordLen, int totalWords,
            Map<String, Integer> wordCount, List<Integer> result) {
        Map<String, Integer> seen = new HashMap<>();
        int left = offset;
        int count = 0;

        for (int right = offset; right + wordLen <= s.length(); right += wordLen) {
            String word = s.substring(right, right + wordLen);
            System.out.printf("Processing word '%s' at position %d\n", word, right);

            if (wordCount.containsKey(word)) {
                seen.put(word, seen.getOrDefault(word, 0) + 1);
                count++;
                System.out.printf("  Valid word! Count: %d, Window: [%d, %d]\n", count, left, right + wordLen);

                while (seen.get(word) > wordCount.get(word)) {
                    String leftWord = s.substring(left, left + wordLen);
                    System.out.printf("  Too many '%s', removing '%s' from left at %d\n", word, leftWord, left);
                    seen.put(leftWord, seen.get(leftWord) - 1);
                    count--;
                    left += wordLen;
                }

                if (count == totalWords) {
                    System.out.printf("  *** MATCH FOUND at index %d! ***\n", left);
                    result.add(left);
                    String leftWord = s.substring(left, left + wordLen);
                    seen.put(leftWord, seen.get(leftWord) - 1);
                    count--;
                    left += wordLen;
                }
            } else {
                System.out.printf("  Invalid word '%s', resetting window\n", word);
                seen.clear();
                count = 0;
                left = right + wordLen;
            }

            System.out.printf("  Current window: [%d, %d], seen: %s\n", left, right + wordLen, seen);
        }
    }

    // Test method
    public static void main(String[] args) {
        SubstringConcatenationAllWords solution = new SubstringConcatenationAllWords();

        // Test case 1
        String s1 = "barfoothefoobarman";
        String[] words1 = { "foo", "bar" };
        System.out.println("=== Test Case 1 ===");
        System.out.println("Brute Force: " + solution.findSubstringBruteForce(s1, words1));
        System.out.println("Optimized: " + solution.findSubstringOptimized(s1, words1));
        System.out.println("Sliding Window: " + solution.findSubstring(s1, words1));
        System.out.println();

        // Test case 2
        String s2 = "wordgoodgoodgoodbestword";
        String[] words2 = { "word", "good", "best", "word" };
        System.out.println("=== Test Case 2 ===");
        System.out.println("Sliding Window: " + solution.findSubstring(s2, words2));
        System.out.println();

        // Test case 3
        String s3 = "barfoofoobarthefoobarman";
        String[] words3 = { "bar", "foo", "the" };
        System.out.println("=== Test Case 3 ===");
        System.out.println("Sliding Window: " + solution.findSubstring(s3, words3));
        System.out.println();

        // Detailed visualization for test case 1
        System.out.println("=== Detailed Visualization ===");
        solution.findSubstringDetailed(s1, words1);

        // Edge cases
        System.out.println("=== Edge Cases ===");
        System.out.println("Empty string: " + solution.findSubstring("", new String[] { "a" }));
        System.out.println("Empty words: " + solution.findSubstring("abc", new String[] {}));
        System.out.println("Single word: " + solution.findSubstring("aaaa", new String[] { "aa" }));
        System.out.println("Duplicate words: " + solution.findSubstring("ababab", new String[] { "ab", "ab" }));
    }

}

/*
 * ALGORITHM EXPLANATION:
 * 
 * This problem requires finding all substrings that are concatenations of ALL
 * words in any permutation.
 * 
 * KEY INSIGHTS:
 * 1. All words have the same length
 * 2. We need to find permutations, not combinations
 * 3. Each word in the input array must be used exactly as many times as it
 * appears
 * 
 * APPROACH COMPARISON:
 * 
 * 1. BRUTE FORCE O(n * m * len):
 * - Check every possible starting position
 * - For each position, extract words and verify against frequency map
 * - Simple but inefficient
 * 
 * 2. OPTIMIZED BRUTE FORCE O(n * m * len):
 * - Same approach but with early termination
 * - Stop checking as soon as invalid word found
 * 
 * 3. SLIDING WINDOW O(n * len):
 * - Key optimization: instead of checking every position, we slide by word
 * length
 * - For each offset (0 to wordLen-1), maintain a sliding window
 * - Use frequency counting to track valid concatenations
 * 
 * SLIDING WINDOW DETAILED EXPLANATION:
 * 
 * The key insight is that we only need to check wordLen different starting
 * offsets:
 * - If wordLen = 3, we only check positions 0, 3, 6, 9... then 1, 4, 7, 10...
 * then 2, 5, 8, 11...
 * - This reduces the problem from checking n positions to checking wordLen
 * sliding windows
 * 
 * For each sliding window:
 * 1. Maintain a frequency map of words in current window
 * 2. Expand window by adding words from right
 * 3. Contract window from left when we have too many of a word
 * 4. Record match when window contains exactly the required words
 * 
 * EXAMPLE WALKTHROUGH: s = "barfoothefoobarman", words = ["foo","bar"]
 * 
 * Offset 0 (positions 0, 3, 6, 9, 12, 15):
 * - Position 0: "bar" (valid), "foo" (valid) → MATCH at index 0
 * - Position 6: "the" (invalid) → reset window
 * - Position 9: "foo" (valid), "bar" (valid) → MATCH at index 9
 * 
 * Offset 1 (positions 1, 4, 7, 10, 13, 16):
 * - No valid matches found
 * 
 * Offset 2 (positions 2, 5, 8, 11, 14):
 * - No valid matches found
 * 
 * TIME COMPLEXITY ANALYSIS:
 * - We have wordLen different sliding windows
 * - Each character in the string is processed at most twice (once when adding,
 * once when removing)
 * - Total: O(wordLen * n) = O(n * len) where len is word length
 * 
 * SPACE COMPLEXITY:
 * - HashMap to store word frequencies: O(m) where m is number of unique words
 * - Seen map for current window: O(m)
 * - Total: O(m)
 * 
 * WHY SLIDING WINDOW IS OPTIMAL:
 * - Avoids redundant checking of overlapping substrings
 * - Reuses computation from previous windows
 * - Efficiently handles duplicate words in the input array
 * - Early termination when invalid words encountered
 * 
 * The sliding window approach is the most efficient solution for this problem.
 */
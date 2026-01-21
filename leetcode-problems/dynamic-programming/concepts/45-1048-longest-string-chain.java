/*
 * LONGEST STRING CHAIN - COMPLETE GUIDE
 * ======================================
 * 
 * PROBLEM UNDERSTANDING:
 * - wordA is predecessor of wordB if we can insert exactly ONE letter anywhere
 *   in wordA to get wordB (order of other characters preserved)
 * - Find the longest chain of words following this predecessor relationship
 * - Return the LENGTH of the longest chain (not the chain itself)
 * 
 * CRITICAL INSIGHTS (How to think through this):
 * ==============================================
 * 
 * INSIGHT #1: Length relationship
 * - If wordA is predecessor of wordB, then len(wordB) = len(wordA) + 1
 * - This means we can organize words by length!
 * - Chains always go: length n → length n+1 → length n+2 ...
 * 
 * INSIGHT #2: This is a DP problem (similar to LIS and Divisible Subset)
 * - We're building chains where each word extends the previous
 * - dp[word] = length of longest chain ending at this word
 * - We can process words by increasing length
 * 
 * INSIGHT #3: How to check if wordA is predecessor of wordB?
 * - len(wordB) must equal len(wordA) + 1
 * - Try removing each character from wordB
 * - If any removal gives us wordA, then wordA is a predecessor
 * 
 * INSIGHT #4: Process order matters!
 * - Sort by length: process shorter words first
 * - When we process a word, all its potential predecessors are already done
 * - This is BOTTOM-UP DP
 * 
 * COMPARISON TO PREVIOUS PROBLEMS:
 * ================================
 * 
 * Like LIS (Longest Increasing Subsequence):
 * - Both build chains with specific relationships
 * - Both use DP to track longest chain ending at each position
 * 
 * Like Largest Divisible Subset:
 * - Both need to check relationship between pairs
 * - Both benefit from smart ordering of elements
 * 
 * Unlike those problems:
 * - We use a HashMap instead of array (words, not indices)
 * - We process by length groups, not sequential order
 * - Checking predecessor is string manipulation, not arithmetic
 * 
 * INTERVIEW DISCOVERY PROCESS:
 * ============================
 * 
 * Interviewer: "How would you approach this?"
 * 
 * You: "Let me start with examples..."
 * [Draw: a → ba → bda → bdca]
 * 
 * You: "I notice the lengths increase by 1 each time: 1,2,3,4"
 * 
 * You: "This reminds me of Longest Increasing Subsequence"
 * 
 * You: "But here the relationship is: can I add one letter?"
 * 
 * You: "If I sort by length, I can build up from smaller words"
 * 
 * You: "I'll use DP: dp[word] = longest chain ending at word"
 * 
 * You: "For each word, I try removing each character and check
 *       if that shorter word exists in my DP map"
 * 
 * ALGORITHM (Final approach):
 * ===========================
 * 1. Sort words by length (process shorter words first)
 * 2. Use HashMap: dp[word] = longest chain ending at word
 * 3. For each word:
 *    - Start with dp[word] = 1 (word by itself is a chain)
 *    - Try removing each character to get potential predecessors
 *    - If predecessor exists, dp[word] = max(dp[word], dp[predecessor] + 1)
 * 4. Return maximum value in dp map
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LongestStringChain {
    
    /*
     * APPROACH 1: DP WITH HASHMAP (Optimal Solution)
     * ==============================================
     * This is THE solution for interviews. Clean and efficient.
     * 
     * TIME COMPLEXITY: O(n * L²)
     * - n = number of words
     * - L = maximum length of a word
     * - For each word (n), we try removing each character (L)
     * - Creating substring takes O(L)
     * - HashMap operations: O(1) average
     * - Sorting: O(n log n)
     * - Overall: O(n log n + n * L²) = O(n * L²) when L > log n
     * 
     * SPACE COMPLEXITY: O(n)
     * - HashMap stores at most n entries
     * 
     * WHY HASHMAP INSTEAD OF ARRAY?
     * - Words are strings, not indices
     * - HashMap allows O(1) lookup by word
     * - More intuitive than mapping words to indices
     */
    public int longestStrChain(String[] words) {
        // Edge case: empty or null array
        if (words == null || words.length == 0) {
            return 0;
        }
        
        // STEP 1: Sort by length (shorter words first)
        // This ensures when we process a word, all potential predecessors
        // have already been processed
        Arrays.sort(words, (a, b) -> a.length() - b.length());
        
        // STEP 2: Initialize DP map
        // Key: word, Value: length of longest chain ending at this word
        Map<String, Integer> dp = new HashMap<>();
        
        // Track the maximum chain length found
        int maxChainLength = 1;
        
        // STEP 3: Process each word in order of increasing length
        for (String word : words) {
            // Base case: this word by itself forms a chain of length 1
            int currentMax = 1;
            
            // STEP 4: Try removing each character to find predecessors
            // For word "abc", we try: "bc", "ac", "ab"
            for (int i = 0; i < word.length(); i++) {
                // Create predecessor by removing character at position i
                // substring(0, i) gives characters before i
                // substring(i + 1) gives characters after i
                String predecessor = word.substring(0, i) + word.substring(i + 1);
                
                // If this predecessor exists and has a chain
                if (dp.containsKey(predecessor)) {
                    // We can extend that chain by adding current word
                    currentMax = Math.max(currentMax, dp.get(predecessor) + 1);
                }
            }
            
            // STEP 5: Store the result for this word
            dp.put(word, currentMax);
            
            // Update global maximum
            maxChainLength = Math.max(maxChainLength, currentMax);
        }
        
        return maxChainLength;
    }
    
    /*
     * EXAMPLE WALKTHROUGH: ["a","b","ba","bca","bda","bdca"]
     * ========================================================
     * 
     * After sorting by length: ["a","b","ba","bca","bda","bdca"]
     * 
     * Process "a" (length 1):
     *   Try removing char 0: "" (not in map)
     *   dp["a"] = 1, maxChain = 1
     *   State: {a:1}
     * 
     * Process "b" (length 1):
     *   Try removing char 0: "" (not in map)
     *   dp["b"] = 1, maxChain = 1
     *   State: {a:1, b:1}
     * 
     * Process "ba" (length 2):
     *   Remove index 0: "a" → found! dp["a"] = 1
     *     currentMax = max(1, 1+1) = 2
     *   Remove index 1: "b" → found! dp["b"] = 1
     *     currentMax = max(2, 1+1) = 2
     *   dp["ba"] = 2, maxChain = 2
     *   State: {a:1, b:1, ba:2}
     * 
     * Process "bca" (length 3):
     *   Remove index 0: "ca" (not in map)
     *   Remove index 1: "ba" → found! dp["ba"] = 2
     *     currentMax = max(1, 2+1) = 3
     *   Remove index 2: "bc" (not in map)
     *   dp["bca"] = 3, maxChain = 3
     *   State: {a:1, b:1, ba:2, bca:3}
     * 
     * Process "bda" (length 3):
     *   Remove index 0: "da" (not in map)
     *   Remove index 1: "ba" → found! dp["ba"] = 2
     *     currentMax = max(1, 2+1) = 3
     *   Remove index 2: "bd" (not in map)
     *   dp["bda"] = 3, maxChain = 3
     *   State: {a:1, b:1, ba:2, bca:3, bda:3}
     * 
     * Process "bdca" (length 4):
     *   Remove index 0: "dca" (not in map)
     *   Remove index 1: "bca" → found! dp["bca"] = 3
     *     currentMax = max(1, 3+1) = 4
     *   Remove index 2: "bda" → found! dp["bda"] = 3
     *     currentMax = max(4, 3+1) = 4
     *   Remove index 3: "bdc" (not in map)
     *   dp["bdca"] = 4, maxChain = 4
     *   State: {a:1, b:1, ba:2, bca:3, bda:3, bdca:4}
     * 
     * Final answer: 4
     * One valid chain: a → ba → bda → bdca
     */
    
    /*
     * APPROACH 2: DP WITH PREDECESSOR TRACKING (Returns actual chain)
     * ===============================================================
     * If interviewer asks "can you return the actual chain?"
     * We need to track parents, similar to Divisible Subset problem.
     */
    public List<String> longestStrChainWithPath(String[] words) {
        if (words == null || words.length == 0) {
            return new ArrayList<>();
        }
        
        Arrays.sort(words, (a, b) -> a.length() - b.length());
        
        // dp[word] = length of longest chain ending at word
        Map<String, Integer> dp = new HashMap<>();
        // parent[word] = previous word in the chain
        Map<String, String> parent = new HashMap<>();
        
        String maxWord = words[0]; // Word where longest chain ends
        int maxChainLength = 1;
        
        for (String word : words) {
            dp.put(word, 1);
            parent.put(word, null); // No parent initially
            
            for (int i = 0; i < word.length(); i++) {
                String predecessor = word.substring(0, i) + word.substring(i + 1);
                
                if (dp.containsKey(predecessor)) {
                    if (dp.get(predecessor) + 1 > dp.get(word)) {
                        dp.put(word, dp.get(predecessor) + 1);
                        parent.put(word, predecessor);
                    }
                }
            }
            
            if (dp.get(word) > maxChainLength) {
                maxChainLength = dp.get(word);
                maxWord = word;
            }
        }
        
        // Reconstruct the chain
        List<String> result = new ArrayList<>();
        String current = maxWord;
        while (current != null) {
            result.add(current);
            current = parent.get(current);
        }
        
        Collections.reverse(result);
        return result;
    }
    
    /*
     * APPROACH 3: ALTERNATIVE - CHECK ALL PAIRS (Less efficient)
     * ==========================================================
     * This is what you might think of first. Mention it then optimize.
     * 
     * For each pair of words, check if one is predecessor of other.
     * Then use DP similar to LIS.
     * 
     * Time: O(n² * L) - checking all pairs, each check takes O(L)
     * This is WORSE than approach 1 when n > L
     */
    public int longestStrChain_Naive(String[] words) {
        if (words == null || words.length == 0) {
            return 0;
        }
        
        int n = words.length;
        Arrays.sort(words, (a, b) -> a.length() - b.length());
        
        // dp[i] = longest chain ending at words[i]
        int[] dp = new int[n];
        Arrays.fill(dp, 1);
        
        int maxLen = 1;
        
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // Check if words[j] is predecessor of words[i]
                if (isPredecessor(words[j], words[i])) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }
        
        return maxLen;
    }
    
    // Helper: Check if word1 is predecessor of word2
    private boolean isPredecessor(String word1, String word2) {
        // Length must differ by exactly 1
        if (word2.length() != word1.length() + 1) {
            return false;
        }
        
        // Try removing each character from word2
        for (int i = 0; i < word2.length(); i++) {
            String removed = word2.substring(0, i) + word2.substring(i + 1);
            if (removed.equals(word1)) {
                return true;
            }
        }
        
        return false;
    }
    
    /*
     * INTERVIEW STRATEGY:
     * ===================
     * 
     * 1. CLARIFY (1-2 minutes):
     *    "So I need to find the longest chain where each word adds exactly
     *     one character to the previous word?"
     *    "The order of other characters must stay the same?"
     *    "Do I return the length or the actual chain?"
     * 
     * 2. EXAMPLES (2 minutes):
     *    Draw: a → ba → bda → bdca
     *    "Notice lengths: 1, 2, 3, 4 - always increasing by 1"
     * 
     * 3. PATTERN RECOGNITION (3 minutes):
     *    "This is similar to Longest Increasing Subsequence"
     *    "Key insight: if I sort by length, I can build up chains"
     *    "Use DP: dp[word] = longest chain ending at word"
     * 
     * 4. ALGORITHM (2 minutes):
     *    "1. Sort by length
     *     2. For each word, try removing each character
     *     3. If that shorter word exists, extend its chain
     *     4. Track maximum"
     * 
     * 5. COMPLEXITY (1 minute):
     *    "Time: O(n * L²) where n=words, L=max length
     *     Space: O(n) for the HashMap"
     * 
     * 6. CODE (10 minutes):
     *    Write clean code with comments
     * 
     * 7. TEST (3 minutes):
     *    Walk through ["a","ba","bda","bdca"]
     *    Edge case: ["abcd","dbqca"] → 1 (no valid chain)
     * 
     * COMMON MISTAKES:
     * ================
     * 
     * 1. Not sorting by length
     *    - You'd process words in random order
     *    - Predecessors might not be computed yet
     * 
     * 2. Trying to add characters instead of remove
     *    - Adding is harder (where to add? 26 choices per position)
     *    - Removing is simpler (just try each position)
     * 
     * 3. Using array instead of HashMap
     *    - Words are strings, not integers
     *    - HashMap is more natural here
     * 
     * 4. Checking all pairs
     *    - O(n²) pairs to check
     *    - The HashMap approach is more efficient
     * 
     * 5. Not handling empty strings
     *    - Edge case: what if we remove all characters?
     *    - This won't be in map (problem states positive length)
     */
    
    // Comprehensive test cases
    public static void main(String[] args) {
        LongestStringChain solution = new LongestStringChain();
        
        System.out.println("=== Test Case 1 ===");
        String[] words1 = {"a","b","ba","bca","bda","bdca"};
        System.out.println("Input: " + Arrays.toString(words1));
        System.out.println("Output: " + solution.longestStrChain(words1)); // 4
        System.out.println("Chain: " + solution.longestStrChainWithPath(words1));
        System.out.println("Explanation: a → ba → bda → bdca");
        System.out.println();
        
        System.out.println("=== Test Case 2 ===");
        String[] words2 = {"xbc","pcxbcf","xb","cxbc","pcxbc"};
        System.out.println("Input: " + Arrays.toString(words2));
        System.out.println("Output: " + solution.longestStrChain(words2)); // 5
        System.out.println("Chain: " + solution.longestStrChainWithPath(words2));
        System.out.println("Explanation: xb → xbc → cxbc → pcxbc → pcxbcf");
        System.out.println();
        
        System.out.println("=== Test Case 3 ===");
        String[] words3 = {"abcd","dbqca"};
        System.out.println("Input: " + Arrays.toString(words3));
        System.out.println("Output: " + solution.longestStrChain(words3)); // 1
        System.out.println("Explanation: No valid chain (order changes)");
        System.out.println();
        
        System.out.println("=== Test Case 4: Long chain ===");
        String[] words4 = {"a","ab","abc","abcd","abcde"};
        System.out.println("Input: " + Arrays.toString(words4));
        System.out.println("Output: " + solution.longestStrChain(words4)); // 5
        System.out.println("Chain: " + solution.longestStrChainWithPath(words4));
        System.out.println();
        
        System.out.println("=== Test Case 5: Multiple chains ===");
        String[] words5 = {"a","b","ab","ba","abc","bac"};
        System.out.println("Input: " + Arrays.toString(words5));
        System.out.println("Output: " + solution.longestStrChain(words5)); // 3
        System.out.println("Chain: " + solution.longestStrChainWithPath(words5));
        System.out.println("Explanation: Multiple chains of length 3 exist");
        System.out.println();
        
        System.out.println("=== Test Case 6: No connections ===");
        String[] words6 = {"abc","def","ghi"};
        System.out.println("Input: " + Arrays.toString(words6));
        System.out.println("Output: " + solution.longestStrChain(words6)); // 1
        System.out.println("Explanation: Same length, no predecessors");
        System.out.println();
        
        System.out.println("=== Test Case 7: Single word ===");
        String[] words7 = {"hello"};
        System.out.println("Input: " + Arrays.toString(words7));
        System.out.println("Output: " + solution.longestStrChain(words7)); // 1
        System.out.println();
        
        // Compare naive vs optimized
        System.out.println("=== Performance Comparison ===");
        String[] perfTest = {"a","ab","abc","abcd","b","bc","bcd","c","cd","d"};
        long start = System.nanoTime();
        int result1 = solution.longestStrChain(perfTest);
        long time1 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result2 = solution.longestStrChain_Naive(perfTest);
        long time2 = System.nanoTime() - start;
        
        System.out.println("HashMap approach: " + result1 + " (time: " + time1 + " ns)");
        System.out.println("Naive approach: " + result2 + " (time: " + time2 + " ns)");
        System.out.println("Speedup: " + (double)time2/time1 + "x");
    }
}

/*
 * COMPLEXITY DEEP DIVE:
 * =====================
 * 
 * Why O(n * L²)?
 * 
 * Sorting: O(n log n)
 * - n words to sort
 * - Comparison is O(L) in worst case
 * - Total: O(n * L * log n)
 * 
 * Main Loop: O(n * L²)
 * - Iterate through n words: O(n)
 * - For each word, try L positions: O(L)
 * - Creating substring: O(L)
 * - HashMap lookup: O(L) for hashing string
 * - Total: O(n * L * L) = O(n * L²)
 * 
 * Overall: O(n log n * L + n * L²)
 * When L > log n (typical): O(n * L²)
 * 
 * Can we do better?
 * - We MUST examine each word: Ω(n)
 * - We MUST try removing characters: Ω(L) per word
 * - Creating substrings is inherently O(L)
 * - So O(n * L²) is likely optimal for this approach
 * 
 * Space Complexity:
 * - HashMap: O(n) words
 * - Each key is a string: O(L) per string
 * - Total: O(n * L)
 * - But we usually count space as O(n) assuming L is small
 * 
 * WHEN IS THIS APPROACH BETTER THAN NAIVE?
 * ========================================
 * 
 * HashMap approach: O(n * L²)
 * Naive (all pairs): O(n² * L)
 * 
 * HashMap is better when: n * L² < n² * L
 * Simplify: L < n
 * 
 * So when average word length < number of words, HashMap wins!
 * This is usually true in practice.
 * 
 * Example:
 * - 1000 words, average length 10
 * - HashMap: 1000 * 100 = 100,000 ops
 * - Naive: 1000² * 10 = 10,000,000 ops
 * - HashMap is 100x faster!
 * 
 * KEY TAKEAWAYS:
 * ==============
 * 
 * 1. Sorting by length enables bottom-up DP
 * 2. HashMap is natural for string-based DP
 * 3. Removing characters is easier than adding
 * 4. Similar pattern to LIS but adapted to strings
 * 5. Time complexity depends on both n and L
 * 
 * VARIATIONS TO CONSIDER:
 * =======================
 * 
 * Q: "What if we can add/remove any number of characters?"
 * A: This becomes edit distance / Levenshtein distance
 * 
 * Q: "What if we can also rearrange letters?"
 * A: Need anagram checking, more complex
 * 
 * Q: "What if we want all longest chains, not just one?"
 * A: Track multiple parents per word
 * 
 * Q: "What about case-insensitive?"
 * A: Convert to lowercase first
 */

class LongestStringChain2 {

    public static void main(String[] args) {
        LongestStringChain2 solver = new LongestStringChain2();

        String[] words1 = {"a", "b", "ba", "bca", "bda", "bdca"};
        System.out.println("Ex 1: " + solver.longestStrChain(words1)); // Expected: 4

        String[] words2 = {"xbc", "pcxbcf", "xb", "cxbc", "pcxbc"};
        System.out.println("Ex 2: " + solver.longestStrChain(words2)); // Expected: 5

        String[] words3 = {"abcd", "dbqca"};
        System.out.println("Ex 3: " + solver.longestStrChain(words3)); // Expected: 1
    }

    /**
     * Calculates the length of the longest string chain.
     * * Time Complexity: O(N * L^2)
     * - Sorting: O(N log N)
     * - Loop: N iterations
     * - Inner Logic: L iterations (generating substrings), and string creation/hashing takes O(L).
     * Total inner loop is O(L^2).
     * Given L is typically small (<= 16 in standard problem constraints), this is highly efficient.
     * * Space Complexity: O(N * L)
     * - To store the map of words and their chain lengths.
     */
    public int longestStrChain(String[] words) {
        if (words == null || words.length == 0) return 0;

        // STEP 1: Sort by length.
        // We must process shorter words first to ensure that when we process a word,
        // we have already computed the results for its potential predecessors.
        Arrays.sort(words, (a, b) -> a.length() - b.length());

        // Map to store the max chain length for each word found so far.
        // Key: Word, Value: Max Chain Length ending at this word.
        Map<String, Integer> dp = new HashMap<>();
        
        int maxChain = 1;

        // STEP 2: Iterate through sorted words
        for (String word : words) {
            int currentChainLen = 1;
            
            // Optimization: "Backward Look"
            // Instead of trying to add characters to find a successor, 
            // we remove characters to find a valid predecessor.
            // A word of length L has exactly L possible predecessors.
            for (int i = 0; i < word.length(); i++) {
                
                // Create a potential predecessor by deleting the character at index 'i'.
                // StringBuilder is faster for manipulation, but substring is cleaner to write.
                StringBuilder sb = new StringBuilder(word);
                sb.deleteCharAt(i);
                String predecessor = sb.toString();

                // If this predecessor exists in our map, we can extend that chain.
                if (dp.containsKey(predecessor)) {
                    int previousLen = dp.get(predecessor);
                    currentChainLen = Math.max(currentChainLen, previousLen + 1);
                }
            }
            
            // Store the best result for the current word
            dp.put(word, currentChainLen);
            
            // Update global maximum
            maxChain = Math.max(maxChain, currentChainLen);
        }

        return maxChain;
    }
}

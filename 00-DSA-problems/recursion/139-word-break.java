import java.util.*;

/**
 * Word Break Problem - All Solution Variants
 * 
 * Problem: Determine if string s can be segmented into dictionary words.
 * Dictionary words can be reused multiple times.
 */
class WordBreakAllSolutions {

    // ==================== SOLUTION 1: Dynamic Programming (Bottom-Up) - OPTIMAL
    // ====================
    /**
     * Approach: Build solution from smaller subproblems using tabulation
     * dp[i] = true if s[0...i-1] can be segmented
     * 
     * Time Complexity: O(n² × m) where n = s.length, m = avg word length in dict
     * - Two nested loops O(n²), substring comparison O(m)
     * Space Complexity: O(n) for dp array + O(k×m) for HashSet where k = dict size
     * 
     * Pros: Most efficient, no recursion overhead, easy to understand
     * Cons: None - this is the standard optimal solution
     */
    public boolean wordBreak1(String s, List<String> wordDict) {
        int n = s.length();
        Set<String> wordSet = new HashSet<>(wordDict); // O(1) lookup
        boolean[] dp = new boolean[n + 1];
        dp[0] = true; // Empty string is always valid

        // Build solution for each position
        for (int i = 1; i <= n; i++) {
            // Try all possible last words ending at position i
            for (int j = 0; j < i; j++) {
                // If s[0...j-1] is valid AND s[j...i-1] is in dictionary
                if (dp[j] && wordSet.contains(s.substring(j, i))) {
                    dp[i] = true;
                    break; // Found valid segmentation for position i
                }
            }
        }

        return dp[n];
    }

    // ==================== SOLUTION 2: DP with Word Length Optimization
    // ====================
    /**
     * Approach: Same as Solution 1 but only check substrings up to max word length
     * 
     * Time Complexity: O(n × L × m) where L = max word length (usually L << n)
     * - Outer loop O(n), inner loop O(L), substring O(m)
     * Space Complexity: O(n + k×m) for dp array and HashSet
     * 
     * Pros: Much faster in practice when max word length is small
     * Cons: Requires preprocessing to find max length
     */
    public boolean wordBreak2(String s, List<String> wordDict) {
        int n = s.length();
        Set<String> wordSet = new HashSet<>(wordDict);

        // Find max word length for optimization
        int maxLen = 0;
        for (String word : wordDict) {
            maxLen = Math.max(maxLen, word.length());
        }

        boolean[] dp = new boolean[n + 1];
        dp[0] = true;

        for (int i = 1; i <= n; i++) {
            // Only check back maxLen positions instead of all positions
            for (int j = Math.max(0, i - maxLen); j < i; j++) {
                if (dp[j] && wordSet.contains(s.substring(j, i))) {
                    dp[i] = true;
                    break;
                }
            }
        }

        return dp[n];
    }

    // ==================== SOLUTION 3: Recursion with Memoization (Top-Down DP)
    // ====================
    /**
     * Approach: Recursive solution with memoization to avoid recomputation
     * 
     * Time Complexity: O(n² × m) - Each position computed once, substring
     * comparison O(m)
     * Space Complexity: O(n) for memoization + O(n) for recursion stack
     * 
     * Pros: Intuitive recursive thinking, only computes needed subproblems
     * Cons: Recursion stack overhead, slightly slower than bottom-up
     */
    public boolean wordBreak3(String s, List<String> wordDict) {
        Set<String> wordSet = new HashSet<>(wordDict);
        Boolean[] memo = new Boolean[s.length()]; // null = not computed, true/false = result
        return canBreak(s, 0, wordSet, memo);
    }

    private boolean canBreak(String s, int start, Set<String> wordSet, Boolean[] memo) {
        // Base case: reached end of string
        if (start == s.length()) {
            return true;
        }

        // Return memoized result if already computed
        if (memo[start] != null) {
            return memo[start];
        }

        // Try all possible words starting at current position
        for (int end = start + 1; end <= s.length(); end++) {
            String word = s.substring(start, end);
            if (wordSet.contains(word) && canBreak(s, end, wordSet, memo)) {
                memo[start] = true;
                return true;
            }
        }

        memo[start] = false;
        return false;
    }

    // ==================== SOLUTION 4: BFS Approach ====================
    /**
     * Approach: Treat as graph problem - each valid word transition is an edge
     * 
     * Time Complexity: O(n² × m) - Visit each position once, check all substrings
     * Space Complexity: O(n) for queue and visited set
     * 
     * Pros: Different perspective, useful for finding shortest segmentation
     * Cons: More complex, similar performance to DP
     */
    public boolean wordBreak4(String s, List<String> wordDict) {
        Set<String> wordSet = new HashSet<>(wordDict);
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[s.length()]; // Avoid revisiting positions

        queue.offer(0); // Start from position 0

        while (!queue.isEmpty()) {
            int start = queue.poll();

            // Skip if already processed this position
            if (visited[start]) {
                continue;
            }
            visited[start] = true;

            // Try all possible words from current position
            for (int end = start + 1; end <= s.length(); end++) {
                if (wordSet.contains(s.substring(start, end))) {
                    if (end == s.length()) {
                        return true; // Reached the end
                    }
                    queue.offer(end); // Add next position to explore
                }
            }
        }

        return false;
    }

    // ==================== SOLUTION 5: Trie-Based DP ====================
    /**
     * Approach: Build Trie from dictionary for efficient word matching
     * 
     * Time Complexity: O(n² + k×m) where k = dict size, m = avg word length
     * - Trie building: O(k×m), DP: O(n²)
     * Space Complexity: O(k×m) for Trie + O(n) for dp array
     * 
     * Pros: Efficient when dictionary is large, no repeated substring creation
     * Cons: More complex implementation, overhead of Trie structure
     */
    public boolean wordBreak5(String s, List<String> wordDict) {
        TrieNode root = buildTrie(wordDict);
        int n = s.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true;

        for (int i = 0; i < n; i++) {
            if (!dp[i])
                continue;

            // Traverse Trie from current position
            TrieNode node = root;
            for (int j = i; j < n; j++) {
                char c = s.charAt(j);
                if (node.children[c - 'a'] == null) {
                    break; // No more matches possible
                }
                node = node.children[c - 'a'];
                if (node.isWord) {
                    dp[j + 1] = true;
                }
            }
        }

        return dp[n];
    }

    // Trie Node definition
    class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isWord = false;
    }

    private TrieNode buildTrie(List<String> wordDict) {
        TrieNode root = new TrieNode();
        for (String word : wordDict) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                int index = c - 'a';
                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }
                node = node.children[index];
            }
            node.isWord = true;
        }
        return root;
    }

    // ==================== SOLUTION 6: Brute Force Backtracking (No Memoization)
    // ====================
    /**
     * Approach: Pure recursion without memoization - for educational purposes
     * 
     * Time Complexity: O(2^n) - Exponential, explores all possible segmentations
     * Space Complexity: O(n) for recursion stack
     * 
     * Pros: Simple to understand, shows the problem structure
     * Cons: VERY SLOW - only for small inputs, will timeout on large strings
     * WARNING: This is inefficient and included only for comparison
     */
    public boolean wordBreak6(String s, List<String> wordDict) {
        Set<String> wordSet = new HashSet<>(wordDict);
        return backtrack(s, 0, wordSet);
    }

    private boolean backtrack(String s, int start, Set<String> wordSet) {
        if (start == s.length()) {
            return true;
        }

        for (int end = start + 1; end <= s.length(); end++) {
            if (wordSet.contains(s.substring(start, end))) {
                if (backtrack(s, end, wordSet)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ==================== Test All Solutions ====================
    public static void main(String[] args) {
        WordBreakAllSolutions solver = new WordBreakAllSolutions();

        System.out.println("=".repeat(80));
        System.out.println("WORD BREAK PROBLEM - ALL SOLUTION VARIANTS");
        System.out.println("=".repeat(80));

        // Test cases
        String[][] testCases = {
                { "leetcode", "leet,code" },
                { "applepenapple", "apple,pen" },
                { "catsandog", "cats,dog,sand,and,cat" },
                { "cars", "car,ca,rs" },
                { "aaaaaaa", "aaaa,aaa" },
                { "abcd", "a,abc,b,cd" }
        };

        String[] expected = { "true", "true", "false", "true", "true", "true" };

        for (int t = 0; t < testCases.length; t++) {
            String s = testCases[t][0];
            List<String> wordDict = Arrays.asList(testCases[t][1].split(","));

            System.out.println("\n" + "=".repeat(80));
            System.out.println("Test Case " + (t + 1) + ":");
            System.out.println("String: \"" + s + "\"");
            System.out.println("Dictionary: " + wordDict);
            System.out.println("Expected: " + expected[t]);
            System.out.println("-".repeat(80));

            // Test all solutions
            long start, end;

            // Solution 1: DP Bottom-Up
            start = System.nanoTime();
            boolean result1 = solver.wordBreak1(s, wordDict);
            end = System.nanoTime();
            System.out.printf("Solution 1 (DP Bottom-Up):           %5s | Time: %.4f ms%n",
                    result1, (end - start) / 1_000_000.0);

            // Solution 2: DP with Optimization
            start = System.nanoTime();
            boolean result2 = solver.wordBreak2(s, wordDict);
            end = System.nanoTime();
            System.out.printf("Solution 2 (DP Optimized):           %5s | Time: %.4f ms%n",
                    result2, (end - start) / 1_000_000.0);

            // Solution 3: Recursion + Memoization
            start = System.nanoTime();
            boolean result3 = solver.wordBreak3(s, wordDict);
            end = System.nanoTime();
            System.out.printf("Solution 3 (Top-Down DP):            %5s | Time: %.4f ms%n",
                    result3, (end - start) / 1_000_000.0);

            // Solution 4: BFS
            start = System.nanoTime();
            boolean result4 = solver.wordBreak4(s, wordDict);
            end = System.nanoTime();
            System.out.printf("Solution 4 (BFS):                    %5s | Time: %.4f ms%n",
                    result4, (end - start) / 1_000_000.0);

            // Solution 5: Trie-Based
            start = System.nanoTime();
            boolean result5 = solver.wordBreak5(s, wordDict);
            end = System.nanoTime();
            System.out.printf("Solution 5 (Trie-Based DP):          %5s | Time: %.4f ms%n",
                    result5, (end - start) / 1_000_000.0);

            // Solution 6: Brute Force (only for short strings)
            if (s.length() <= 15) {
                start = System.nanoTime();
                boolean result6 = solver.wordBreak6(s, wordDict);
                end = System.nanoTime();
                System.out.printf("Solution 6 (Brute Force):            %5s | Time: %.4f ms%n",
                        result6, (end - start) / 1_000_000.0);
            } else {
                System.out.println("Solution 6 (Brute Force):            SKIPPED (too slow for long strings)");
            }

            // Verify all solutions match
            boolean allMatch = result1 == result2 && result2 == result3 &&
                    result3 == result4 && result4 == result5;
            System.out.println("\nAll solutions match: " + (allMatch ? "✓" : "✗"));
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPARISON SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("Solution 1 (DP Bottom-Up):      BEST - Standard optimal solution");
        System.out.println("Solution 2 (DP Optimized):      FASTEST - Optimized for short dictionary words");
        System.out.println("Solution 3 (Top-Down DP):       Good - Intuitive recursive approach");
        System.out.println("Solution 4 (BFS):               Alternative - Useful for path finding");
        System.out.println("Solution 5 (Trie-Based):        Specialized - Best for large dictionaries");
        System.out.println("Solution 6 (Brute Force):       AVOID - Educational only, exponential time");
        System.out.println("=".repeat(80));

        // Performance test with longer string
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PERFORMANCE TEST (longer string)");
        System.out.println("=".repeat(80));

        String longString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 50 'a's
        List<String> longDict = Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa");

        System.out.println("String: " + longString.length() + " characters");
        System.out.println("Dictionary: " + longDict);
        System.out.println();

        start = System.nanoTime();
        boolean r1 = solver.wordBreak1(longString, longDict);
        end = System.nanoTime();
        System.out.printf("Solution 1 (DP): %5s in %.4f ms%n", r1, (end - start) / 1_000_000.0);

        start = System.nanoTime();
        boolean r2 = solver.wordBreak2(longString, longDict);
        end = System.nanoTime();
        System.out.printf("Solution 2 (Optimized): %5s in %.4f ms%n", r2, (end - start) / 1_000_000.0);

        System.out.println("\nSolution 2 is typically faster due to max length optimization!");
        System.out.println("=".repeat(80));
    }
}

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Longest String Chain
 * Given an array of words, find the length of the longest possible word chain.
 * A word A is a predecessor of word B if you can insert exactly ONE letter 
 * anywhere in A to get B.
 * 
 * Constraints:
 * 1 <= words.length <= 1000
 * 1 <= words[i].length <= 16
 * words[i] consists only of lowercase English letters.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this problem tests your ability to choose the right 
 * direction of evaluation.
 * 
 * Q: "Should I take a word and try adding all 26 lowercase letters to it to 
 *     find its successors?"
 * A: No! This is a common junior trap. For a word of length L, adding a character 
 *    means checking (L + 1) * 26 possible combinations. 
 *    Instead, we should do the REVERSE: Take a word and DELETE one character 
 *    at a time to see if the resulting smaller word exists in our given array. 
 *    This requires at most L operations per word. Since L <= 16, this is insanely fast.
 * 
 * Q: "Can the chain jump around the input array?"
 * A: Yes. The sequence is formed by choosing words from the array in any order. 
 *    This tells us immediately that for a Bottom-Up DP approach, we MUST sort 
 *    the array by string length first to establish a valid topological order.
 * 
 * CRITICAL SENIOR INSIGHT:
 * "This problem is structurally identical to 'Longest Increasing Subsequence' (LIS). 
 * But instead of strictly greater numbers, our relationship is 'differs by 1 char'. 
 * Because the max string length is capped at a tiny 16 characters, string 
 * manipulation (substring creation) inside the DP loop is highly optimal and 
 * completely avoids the need to build a complex Adjacency List graph."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "If I want to know the longest chain ending at word 'W', I look at all 
 * valid predecessors (words formed by deleting exactly 1 character from 'W').
 * 
 * DP State: dp[W] = The length of the longest chain ending at word W.
 * Recurrence: dp[W] = 1 + Max(dp[P]) for all valid predecessors P.
 * 
 * Because evaluating a word requires knowing the optimal chains of smaller 
 * words, we have overlapping subproblems -> Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: words = ["a", "b", "ba", "bca", "bda", "bdca"]
 * Sorted by length: ["a", "b", "ba", "bca", "bda", "bdca"]
 * 
 * Let's trace Tabulation (Bottom-Up):
 * 1. "a": Predecessor "" not in dict. dp["a"] = 1.
 * 2. "b": Predecessor "" not in dict. dp["b"] = 1.
 * 3. "ba": 
 *    - Drop 'b' -> "a". Found in dict! dp["a"] = 1.
 *    - Drop 'a' -> "b". Found in dict! dp["b"] = 1.
 *    dp["ba"] = Math.max(1, 1 + dp["a"], 1 + dp["b"]) = 2.
 * 4. "bca":
 *    - Drop 'b' -> "ca" (Not in dict)
 *    - Drop 'c' -> "ba". Found! dp["ba"] = 2.
 *    - Drop 'a' -> "bc" (Not in dict)
 *    dp["bca"] = 1 + dp["ba"] = 3.
 * 
 * 5. "bdca":
 *    - Drop 'c' -> "bda". Found! dp["bda"] = 3.
 *    dp["bdca"] = 1 + 3 = 4.
 * 
 * Max Chain Length = 4.
 */
public class LongestStringChain {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Try starting a chain from every single word. For each word, recursively 
     * drop one character at a time and check if it continues the chain.
     * 
     * Time Complexity: O(N * 2^L) - Where N is words.length, L is word length.
     * Space Complexity: O(N + L) - Set storage + recursion depth.
     */
    public int longestStrChainRecursive(String[] words) {
        if (words == null || words.length == 0) return 0;
        
        Set<String> wordSet = new HashSet<>(Arrays.asList(words));
        int maxChain = 0;
        
        for (String word : words) {
            maxChain = Math.max(maxChain, solveRecursive(word, wordSet));
        }
        
        return maxChain;
    }

    private int solveRecursive(String word, Set<String> wordSet) {
        int maxLength = 1;
        
        // Loop through the word, dropping exactly one character at index 'i'
        for (int i = 0; i < word.length(); i++) {
            String predecessor = word.substring(0, i) + word.substring(i + 1);
            
            // PHYSICAL CHECK: Is this predecessor an actual word from our array?
            if (wordSet.contains(predecessor)) {
                maxLength = Math.max(maxLength, 1 + solveRecursive(predecessor, wordSet));
            }
        }
        
        return maxLength;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: The brute force approach repeatedly calculates the same sub-chains. 
     * We cache the longest chain originating from each string.
     * 
     * Time Complexity: O(N * L^2) - N words, L drops per word, L time to build substring.
     * Space Complexity: O(N) - For the HashSet and HashMap.
     */
    public int longestStrChainMemo(String[] words) {
        if (words == null || words.length == 0) return 0;
        
        Set<String> wordSet = new HashSet<>(Arrays.asList(words));
        Map<String, Integer> memo = new HashMap<>();
        int maxChain = 0;
        
        for (String word : words) {
            maxChain = Math.max(maxChain, solveMemo(word, wordSet, memo));
        }
        
        return maxChain;
    }

    private int solveMemo(String word, Set<String> wordSet, Map<String, Integer> memo) {
        if (memo.containsKey(word)) {
            return memo.get(word);
        }
        
        int maxLength = 1;
        
        for (int i = 0; i < word.length(); i++) {
            String predecessor = word.substring(0, i) + word.substring(i + 1);
            
            if (wordSet.contains(predecessor)) {
                maxLength = Math.max(maxLength, 1 + solveMemo(predecessor, wordSet, memo));
            }
        }
        
        memo.put(word, maxLength);
        return maxLength;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Sort the words by length first! This guarantees that when we evaluate 
     * a word of length L, all possible predecessors (length L-1) have ALREADY 
     * been evaluated and their max chains are stored in our DP map.
     * 
     * Time Complexity: O(N log N + N * L^2) - Sorting dominates if N is large.
     * Space Complexity: O(N) - For the HashMap storing the DP states.
     */
    public int longestStrChainTabulation(String[] words) {
        if (words == null || words.length == 0) return 0;
        
        // 1. Sort the words by length ascending. 
        // This is our Topological Sort! We guarantee smaller words are processed first.
        Arrays.sort(words, (a, b) -> a.length() - b.length());
        
        // dp map signifies: dp.get(W) = "The longest chain ending at word W"
        Map<String, Integer> dp = new HashMap<>();
        int globalMaxChain = 1;
        
        for (String word : words) {
            
            // By default, a word forms a valid chain of length 1 by itself
            int currentMaxChain = 1;
            
            // --- DETAILED TABULATION EXPLANATION ---
            // We simulate stripping away one character at a time to check for predecessors
            for (int i = 0; i < word.length(); i++) {
                
                // Construct the string without the character at index 'i'
                String predecessor = word.substring(0, i) + word.substring(i + 1);
                
                // If this predecessor actually exists in our dictionary, it MUST 
                // have already been evaluated because we sorted by length!
                if (dp.containsKey(predecessor)) {
                    // We link our current word onto the predecessor's optimal chain
                    currentMaxChain = Math.max(currentMaxChain, dp.get(predecessor) + 1);
                }
            }
            
            // Lock the optimal answer for this word into the DP table
            dp.put(word, currentMaxChain);
            
            // Track the absolute longest chain found anywhere in the array
            globalMaxChain = Math.max(globalMaxChain, currentMaxChain);
        }
        
        return globalMaxChain;
    }

    /**
     * ========================================================================
     * APPROACH 4: Note on Space Optimization (L4/L5 Target)
     * ========================================================================
     * Standard 1D grid DP problems optimize space by keeping only the previous row. 
     * Here, a word of length L ONLY relies on words of length L-1. 
     * 
     * We *could* optimize space by using an array of HashMaps indexed by string length 
     * and discarding maps older than L-1. However, because N <= 1000, storing all 
     * 1000 words in a single HashMap requires trivially small memory (~a few KB). 
     * 
     * In an interview, explicitly stating: "We could drop historical string lengths 
     * to save memory, but a unified HashMap is practically O(N) and much cleaner 
     * to implement" demonstrates pragmatic senior engineering judgment.
     */

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new LongestStringChain();
        
        record TestCase(String[] words, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new String[]{"a", "b", "ba", "bca", "bda", "bdca"}, 4),
            // Traced in comments: "a" -> "ba" -> "bda" -> "bdca"
            
            new TestCase(new String[]{"xbc", "pcxbcf", "xb", "cxbc", "pcxbc"}, 5),
            // Chain: "xb" -> "xbc" -> "cxbc" -> "pcxbc" -> "pcxbcf"
            
            new TestCase(new String[]{"abcd", "dbqca"}, 1)
            // No valid predecessor links, length is 1
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Words   : " + Arrays.toString(tc.words));
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.longestStrChainRecursive(tc.words));
            System.out.println("Memoization       : " + solver.longestStrChainMemo(tc.words));
            System.out.println("Tabulation        : " + solver.longestStrChainTabulation(tc.words));
            System.out.println();
        }
    }
}

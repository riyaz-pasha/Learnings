import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Word Break II
 * Given a string s and a dictionary of strings wordDict, add spaces to s to 
 * construct a sentence where each word is a valid dictionary word. Return all 
 * such possible sentences in any order.
 * 
 * Constraints:
 * 1 <= s.length <= 20
 * 1 <= wordDict.length <= 1000
 * 1 <= wordDict[i].length <= 10
 * s and wordDict[i] consist of only lowercase English letters.
 * All the strings of wordDict are unique.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, recognizing the fundamental difference between this 
 * and "Word Break I" is the most important step.
 * 
 * Q: "Word Break I just asked for a boolean (can it be segmented?). This asks 
 *     for ALL paths. Does this mean the worst-case time complexity is inherently 
 *     exponential regardless of Dynamic Programming?"
 * A: Yes! This is a massive senior insight. If s = "aaaaa" and dict = ["a", "aa", "aaa"], 
 *    the number of valid segmentations grows exponentially like O(2^N). No amount 
 *    of DP can change the fact that we MUST physically construct and return 
 *    O(2^N) strings. 
 * 
 * Q: "If it's exponential anyway, why use Memoization over pure Backtracking?"
 * A: Pure backtracking explores dead ends repeatedly. If the suffix "code" 
 *    cannot be broken down, pure backtracking will rediscover that failure 
 *    every single time it arrives at that suffix via a different prefix. 
 *    Memoization prevents us from re-evaluating dead ends and caches the 
 *    exact list of valid suffixes so we can just append them.
 * 
 * CRITICAL SENIOR INSIGHT - TABULATION PITFALL:
 * "For 'return all paths' problems, Bottom-Up Tabulation is actually often 
 * WORSE than Top-Down Memoization. Tabulation forces you to compute and store 
 * all possible combinations for every prefix, even if that prefix eventually 
 * hits a dead end and never reaches the end of the string. Top-Down DFS naturally 
 * prunes dead branches. We will implement Tabulation to show mastery, but 
 * Memoization is the industry standard for this specific problem."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We want to find all valid sentences starting from index `i`.
 * 
 * To do this, we carve out a substring from `i` to `j`. 
 * If this substring is a valid dictionary word, we recursively ask: 
 * 'Give me all valid sentences that can be made from the REST of the string (from `j` to end).'
 * 
 * Once the recursion returns those valid suffix sentences, we simply prepend 
 * our current valid word to all of them.
 * 
 * We cache the list of valid sentences returned for each index `i`."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: s = "catsanddog", dict = ["cat", "cats", "and", "sand", "dog"]
 * 
 * DFS(0) - evaluating prefixes starting at 0:
 * -> Prefix "cat" (valid). Call DFS(3) for "sanddog".
 *    -> DFS(3): Prefix "sand" (valid). Call DFS(7) for "dog".
 *       -> DFS(7): Prefix "dog" (valid). Call DFS(10).
 *          -> DFS(10): End of string. Returns [""] (a list with one empty string to signify success).
 *       <- DFS(7) receives [""]. Prepends "dog " -> Returns ["dog"].
 *    <- DFS(3) receives ["dog"]. Prepends "sand " -> Returns ["sand dog"].
 * 
 * -> Prefix "cats" (valid). Call DFS(4) for "anddog".
 *    -> DFS(4): Prefix "and" (valid). Call DFS(7) for "dog".
 *       -> DFS(7) is ALREADY CACHED! Returns ["dog"] instantly!
 *    <- DFS(4) receives ["dog"]. Prepends "and " -> Returns ["and dog"].
 * 
 * <- DFS(0) receives ["sand dog"] from "cat", and ["and dog"] from "cats".
 * Returns ["cat sand dog", "cats and dog"].
 */
public class WordBreakII {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Backtracking Brute Force)
     * ========================================================================
     * Idea: Traverse the string. Whenever a valid dictionary word is found, 
     * add it to a temporary path, and recursively process the remaining suffix. 
     * If we reach the end, convert the path to a space-separated string.
     * 
     * Time Complexity: O(2^N) - Exponential branching at every character.
     * Space Complexity: O(N) - Recursion stack and path storage. (Excluding output).
     */
    public List<String> wordBreakRecursive(String s, List<String> wordDict) {
        Set<String> dict = new HashSet<>(wordDict);
        List<String> result = new ArrayList<>();
        backtrack(s, dict, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(String s, Set<String> dict, int startIndex, List<String> currentPath, List<String> result) {
        // BASE CASE REASONING:
        // If we reach the end of the string, it means the sequence of words in 
        // our currentPath successfully consumed every character.
        if (startIndex == s.length()) {
            result.add(String.join(" ", currentPath));
            return;
        }

        // Try carving every possible prefix starting from 'startIndex'
        for (int endIndex = startIndex + 1; endIndex <= s.length(); endIndex++) {
            String word = s.substring(startIndex, endIndex);
            
            if (dict.contains(word)) {
                // UNIVERSE: Include this valid word in our path
                currentPath.add(word);
                
                // Recurse to process the remaining suffix
                backtrack(s, dict, endIndex, currentPath, result);
                
                // BACKTRACK: Remove the word so we can try alternative segmentations
                currentPath.remove(currentPath.size() - 1);
            }
        }
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (DFS + Memoization) - OPTIMAL
     * ========================================================================
     * Idea: Instead of passing a path forward, we request the list of valid 
     * suffixes backward. We cache the List of Strings for each index.
     * 
     * Time Complexity: O(2^N) strictly in the worst case (e.g., "aaaaa"), but 
     * drastically reduced to O(N^2 + Paths) in realistic scenarios by avoiding 
     * dead-end re-evaluation.
     * Space Complexity: O(2^N * N) - To store all combinations in the memo map.
     */
    public List<String> wordBreakMemo(String s, List<String> wordDict) {
        Set<String> dict = new HashSet<>(wordDict);
        
        // Map from 'startIndex' -> List of valid sentences for the suffix
        Map<Integer, List<String>> memo = new HashMap<>();
        
        return solveMemo(s, dict, 0, memo);
    }

    private List<String> solveMemo(String s, Set<String> dict, int startIndex, Map<Integer, List<String>> memo) {
        // Return cached result if we've already evaluated this suffix
        if (memo.containsKey(startIndex)) {
            return memo.get(startIndex);
        }

        List<String> validSentences = new ArrayList<>();

        // BASE CASE:
        // If we reached the end, return a list containing an empty string. 
        // This acts as a successful "anchor" for the words to attach to as they bubble up.
        if (startIndex == s.length()) {
            validSentences.add("");
            return validSentences;
        }

        for (int endIndex = startIndex + 1; endIndex <= s.length(); endIndex++) {
            String currentWord = s.substring(startIndex, endIndex);
            
            if (dict.contains(currentWord)) {
                // Ask the oracle (recursion) for all valid ways to break the REST of the string
                List<String> suffixWays = solveMemo(s, dict, endIndex, memo);
                
                // For every valid suffix sequence returned, prepend our current word
                for (String suffix : suffixWays) {
                    if (suffix.equals("")) {
                        // If it's the very last word, no trailing space
                        validSentences.add(currentWord);
                    } else {
                        // Otherwise, chain it with a space
                        validSentences.add(currentWord + " " + suffix);
                    }
                }
            }
        }

        // Cache the result before returning
        memo.put(startIndex, validSentences);
        return validSentences;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 1D)
     * ========================================================================
     * Idea: dp[i] holds a List of all valid sentences that successfully segment 
     * the prefix of length `i`. We build this up to length `s.length()`.
     * 
     * Time Complexity: O(2^N) worst case.
     * Space Complexity: O(2^N * N) worst case.
     */
    public List<String> wordBreakTabulation(String s, List<String> wordDict) {
        Set<String> dict = new HashSet<>(wordDict);
        int n = s.length();
        
        // dp[i] signifies: "All fully assembled valid sentences for the prefix of length i"
        // We use an array of Lists. (Safe cast because we control the generic array)
        @SuppressWarnings("unchecked")
        List<String>[] dp = new ArrayList[n + 1];
        for (int i = 0; i <= n; i++) {
            dp[i] = new ArrayList<>();
        }
        
        // BASE CASE REASONING:
        // Prefix of length 0 has exactly 1 valid segmentation (the empty string).
        dp[0].add("");

        // 'i' represents the length of the current prefix we are evaluating
        for (int i = 1; i <= n; i++) {
            
            // 'j' places a split point to carve a word ending at 'i'
            for (int j = 0; j < i; j++) {
                
                // PHYSICAL CHECK:
                // 1. Did the prefix up to 'j' actually form valid sentences?
                // 2. Is the remaining chunk from 'j' to 'i' a valid dictionary word?
                if (!dp[j].isEmpty()) {
                    String chunk = s.substring(j, i);
                    
                    if (dict.contains(chunk)) {
                        // For every valid sentence in dp[j], append the new chunk!
                        for (String previousSentence : dp[j]) {
                            if (previousSentence.isEmpty()) {
                                dp[i].add(chunk);
                            } else {
                                dp[i].add(previousSentence + " " + chunk);
                            }
                        }
                    }
                }
            }
        }

        return dp[n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Note on Space Optimization
     * ========================================================================
     * For "return all paths" problems, true O(1) or purely reduced space 
     * optimization is fundamentally impossible because the OUTPUT size itself 
     * dictates the memory requirements, which can be exponential (O(2^N)).
     * 
     * In Tabulation, we theoretically could limit our inner loop to only look back 
     * as far as the longest word in `wordDict` (like we did in Word Break I). 
     * However, even if we collapse the array to a rolling window of size `maxWordLength`, 
     * the actual Lists of strings inside that window will still grow exponentially 
     * large. Therefore, standard Memoization (Approach 2) is universally preferred 
     * by senior engineers for this specific problem.
     */

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new WordBreakII();
        
        record TestCase(String s, List<String> wordDict) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("catsanddog", Arrays.asList("cat", "cats", "and", "sand", "dog")),
            // Expected: ["cats and dog", "cat sand dog"]
            
            new TestCase("pineapplepenapple", Arrays.asList("apple", "pen", "applepen", "pine", "pineapple")),
            // Expected: ["pine apple pen apple", "pineapple pen apple", "pine applepen apple"]
            
            new TestCase("catsandog", Arrays.asList("cats", "dog", "sand", "and", "cat"))
            // Expected: [] (No valid break possible)
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("String    : \"" + tc.s + "\"");
            System.out.println("Dictionary: " + tc.wordDict);
            
            System.out.println("\nRecursive (Backtracking):");
            for (String sentence : solver.wordBreakRecursive(tc.s, tc.wordDict)) System.out.println("  " + sentence);
            
            System.out.println("\nMemoization (DFS + Memo):");
            for (String sentence : solver.wordBreakMemo(tc.s, tc.wordDict)) System.out.println("  " + sentence);
            
            System.out.println("\nTabulation 1D:");
            for (String sentence : solver.wordBreakTabulation(tc.s, tc.wordDict)) System.out.println("  " + sentence);
            System.out.println();
        }
    }
}

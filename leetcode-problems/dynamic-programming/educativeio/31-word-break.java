import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Word Break
 * Given a string 's' and a dictionary of strings 'wordDict', check if 's' can 
 * be segmented into a space-separated sequence of one or more dictionary words.
 * Return true if possible, else false.
 * 
 * Note: The same word in the dictionary may be used multiple times.
 * 
 * Constraints:
 * 1 <= s.length <= 250
 * 1 <= wordDict.length <= 1000
 * 1 <= wordDict[i].length <= 20
 * s and wordDict[i] consist of only lowercase English letters.
 * All strings of wordDict are unique.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, mention data structure selection immediately:
 * 
 * Q: "Searching through a List of 1000 words repeatedly will take O(N). Can I 
 *     convert wordDict into a HashSet for O(1) lookups?"
 * A: Yes, this is a mandatory optimization before starting the DP.
 * 
 * Q: "What if there are multiple valid segmentations?"
 * A: We only need to return a boolean (`true`), so the first valid path we find 
 *    allows us to short-circuit and stop searching.
 * 
 * CRITICAL SENIOR INSIGHT: 
 * "The prompt states `wordDict[i].length <= 20`. This is a massive constraint 
 * hint! It means when looking backwards to form a word, we NEVER need to look 
 * further back than 20 characters. This allows us to optimize our inner loop 
 * from O(N) to O(1) (specifically O(20)), and even optimize our space complexity!"
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given index `i` in the string, I want to know: 'Can the prefix of 
 * the string up to index `i` be successfully broken into dictionary words?'
 * 
 * To answer this, I look backwards from `i` to some previous index `j`.
 * If TWO conditions are met:
 *   1. The prefix up to `j` was successfully broken into words.
 *   2. The substring from `j` to `i` is a valid word in the dictionary.
 * Then I know for an absolute fact that the prefix up to `i` is also valid!
 * 
 * Because calculating the validity of a long prefix requires knowing the 
 * validity of smaller prefixes, we have overlapping subproblems -> DP."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: s = "leetcode", wordDict = ["leet", "code"]
 * 
 * Let dp[i] represent whether the prefix of length 'i' can be segmented.
 * Initialization: dp[0] = true (an empty string requires 0 words, technically valid).
 * 
 * i=1 ("l"): Look back to j=0. Is "l" in dict? No. dp[1] = false.
 * i=2 ("le"): Look back to j=0,1. No words. dp[2] = false.
 * ...
 * i=4 ("leet"): 
 *   j=0: dp[0] is TRUE. Substring(0,4) is "leet". Is "leet" in dict? YES!
 *   We found a valid split! dp[4] = true.
 * 
 * i=8 ("leetcode"):
 *   We check j=7, j=6, j=5... all have dp[j] == false.
 *   j=4: dp[4] is TRUE. Substring(4,8) is "code". Is "code" in dict? YES!
 *   We found a valid split! dp[8] = true.
 * 
 * Final answer: dp[8] -> true.
 */
public class WordBreak {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Start at index 0. Try every possible prefix length. If the prefix 
     * is in the dictionary, recursively check if the remaining suffix is valid.
     * 
     * Time Complexity: O(2^N) - In the worst case (e.g., s="aaaaa", dict=["a","aa"]),
     * we explore every single combination of splits.
     * Space Complexity: O(N) - Maximum depth of the recursion tree.
     */
    public boolean wordBreakRecursive(String s, List<String> wordDict) {
        if (s == null || s.length() == 0) return false;
        Set<String> wordSet = new HashSet<>(wordDict);
        return solveRecursive(s, wordSet, 0);
    }

    private boolean solveRecursive(String s, Set<String> wordSet, int startIndex) {
        // BASE CASE REASONING:
        // If our starting index has reached the exact end of the string, it means 
        // every single character prior to this was successfully consumed by a valid 
        // dictionary word. The entire string has been broken! Return true.
        if (startIndex == s.length()) {
            return true;
        }

        // We try carving out a substring starting from 'startIndex' and ending at 'endIndex'.
        for (int endIndex = startIndex + 1; endIndex <= s.length(); endIndex++) {
            
            String currentSubstring = s.substring(startIndex, endIndex);
            
            // PHYSICAL CHECK:
            // 1. Is this carved piece a valid word?
            // 2. If yes, can the REST of the string (the suffix) also be broken?
            if (wordSet.contains(currentSubstring) && solveRecursive(s, wordSet, endIndex)) {
                // If both are true, we found a winning path! Short-circuit and return true.
                return true;
            }
        }

        // If we tried all possible ending indices and none led to a full break, 
        // this specific path is a dead end. Return false.
        return false;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache whether the suffix starting at `startIndex` can be broken.
     * 
     * Time Complexity: O(N^3) - State space is N. Inner substring loop is N. 
     * String hashing/substring creation takes O(N).
     * Space Complexity: O(N) - For the memo array and recursion stack.
     */
    public boolean wordBreakMemo(String s, List<String> wordDict) {
        if (s == null || s.length() == 0) return false;
        Set<String> wordSet = new HashSet<>(wordDict);
        
        // Use Boolean object array so null signifies "uncalculated state"
        Boolean[] memo = new Boolean[s.length()];
        
        return solveMemo(s, wordSet, 0, memo);
    }

    private boolean solveMemo(String s, Set<String> wordSet, int startIndex, Boolean[] memo) {
        if (startIndex == s.length()) return true;

        if (memo[startIndex] != null) {
            return memo[startIndex];
        }

        for (int endIndex = startIndex + 1; endIndex <= s.length(); endIndex++) {
            String currentWord = s.substring(startIndex, endIndex);
            
            if (wordSet.contains(currentWord) && solveMemo(s, wordSet, endIndex, memo)) {
                memo[startIndex] = true;
                return true;
            }
        }

        memo[startIndex] = false;
        return false;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 1D)
     * ========================================================================
     * Idea: Build a 1D boolean array where dp[i] represents if the prefix of 
     * length 'i' can be validly segmented.
     * 
     * Time Complexity: O(N^3) or O(N^2 * maxWordLength). 
     * Space Complexity: O(N) for the DP array.
     */
    public boolean wordBreakTabulation(String s, List<String> wordDict) {
        Set<String> wordSet = new HashSet<>(wordDict);
        
        // Find the maximum word length in the dictionary to optimize our inner loop!
        // The problem constrains this to <= 20.
        int maxWordLength = 0;
        for (String word : wordDict) {
            maxWordLength = Math.max(maxWordLength, word.length());
        }

        // dp[i] signifies: "Can the prefix of string 's' of exact length 'i' 
        // be broken down completely into dictionary words?"
        boolean[] dp = new boolean[s.length() + 1];

        // BASE CASE REASONING:
        // An empty string (length 0) requires 0 words to be "segmented". 
        // It is mathematically valid and acts as the anchor for our first word.
        dp[0] = true;

        // Outer loop: Slowly reveal the string, one character at a time.
        // 'i' represents the length of the prefix we are currently trying to validate.
        for (int i = 1; i <= s.length(); i++) {
            
            // Inner loop: We place a "split point" at index 'j' somewhere BEFORE 'i'.
            // This splits the prefix 'i' into two parts:
            // Part 1: The earlier prefix of length 'j'.
            // Part 2: The remaining word from index 'j' to 'i'.
            
            // OPTIMIZATION: We don't need to look all the way back to index 0! 
            // We only need to look back as far as the longest word in the dictionary.
            // If the max word is 20 chars long, any 'j' further back than i - 20 
            // would create a word longer than 20 chars, which is impossible to match!
            int lookBackLimit = Math.max(0, i - maxWordLength);
            
            for (int j = i - 1; j >= lookBackLimit; j--) {
                
                // --- DETAILED TABULATION EXPLANATION ---
                
                // CHECK 1: Did we successfully segment the string up to the split point 'j'?
                boolean isPrefixValid = dp[j];
                
                if (isPrefixValid) {
                    
                    // CHECK 2: Is the remaining chunk of characters a valid dictionary word?
                    // We extract the chunk starting at 'j' and ending at 'i'.
                    String chunk = s.substring(j, i);
                    
                    if (wordSet.contains(chunk)) {
                        
                        // BOTH CONDITIONS MET!
                        // 1. The history up to 'j' is clean and fully segmented.
                        // 2. The new chunk perfectly forms a recognized word.
                        // Therefore, the entire prefix up to 'i' is officially valid!
                        dp[i] = true;
                        
                        // Because we only care IF it can be segmented (not how many ways),
                        // we can instantly stop looking for other split points 'j'.
                        // We found a winning combination for prefix 'i', so break the inner loop.
                        break;
                    }
                }
            }
        }

        // The answer to the problem lies at the very end of the array, 
        // representing the full length of the string.
        return dp[s.length()];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, look closely at our optimized inner loop:
     * `for (int j = i - 1; j >= Math.max(0, i - maxWordLength); j--)`
     * 
     * To calculate `dp[i]`, we ONLY look back `maxWordLength` steps! 
     * Any boolean flag older than `i - maxWordLength` is entirely dead memory.
     * 
     * We can collapse our O(N) boolean array into an O(maxWordLength) rolling array.
     * Since maxWordLength <= 20, this reduces our Space Complexity to O(20) = O(1)!
     * 
     * Time Complexity: O(N * maxWordLength)
     * Space Complexity: O(maxWordLength) = O(1) strictly constant space.
     */
    public boolean wordBreakSpaceOptimized(String s, List<String> wordDict) {
        Set<String> wordSet = new HashSet<>(wordDict);
        
        int maxWordLength = 0;
        for (String word : wordDict) {
            maxWordLength = Math.max(maxWordLength, word.length());
        }
        
        // The rolling window size must be maxWordLength + 1 to hold the current 
        // calculation plus the history we need to look back at.
        int windowSize = maxWordLength + 1;
        boolean[] rollingDp = new boolean[windowSize];
        
        // BASE CASE REASONING:
        // Length 0 is always true. 
        rollingDp[0] = true;
        
        for (int i = 1; i <= s.length(); i++) {
            
            boolean canBreakCurrent = false;
            int lookBackLimit = Math.max(0, i - maxWordLength);
            
            for (int j = i - 1; j >= lookBackLimit; j--) {
                
                // We use Modulo arithmetic to perfectly map the absolute index 'j' 
                // into our small circular rolling array.
                boolean isPrefixValid = rollingDp[j % windowSize];
                
                if (isPrefixValid) {
                    String chunk = s.substring(j, i);
                    if (wordSet.contains(chunk)) {
                        canBreakCurrent = true;
                        break;
                    }
                }
            }
            
            // We store the result for length 'i' in the rolling array.
            rollingDp[i % windowSize] = canBreakCurrent;
            
            // Note: If we had to overwrite an old 'true' with a 'false' because 
            // the current prefix is invalid, this assignment correctly handles it.
        }

        // Return the boolean value mapped to the absolute end of the string.
        return rollingDp[s.length() % windowSize];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new WordBreak();
        
        record TestCase(String s, List<String> wordDict, boolean expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("leetcode", Arrays.asList("leet", "code"), true),
            new TestCase("applepenapple", Arrays.asList("apple", "pen"), true),
            new TestCase("catsandog", Arrays.asList("cats", "dog", "sand", "and", "cat"), false),
            new TestCase("a", Arrays.asList("b"), false),
            new TestCase("aaaaaaa", Arrays.asList("aaaa", "aaa"), true)
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("String    : \"" + tc.s + "\"");
            System.out.println("Dictionary: " + tc.wordDict);
            System.out.println("Expected  : " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.wordBreakRecursive(tc.s, tc.wordDict));
            System.out.println("Memoization       : " + solver.wordBreakMemo(tc.s, tc.wordDict));
            System.out.println("Tabulation 1D     : " + solver.wordBreakTabulation(tc.s, tc.wordDict));
            System.out.println("Space Optimized   : " + solver.wordBreakSpaceOptimized(tc.s, tc.wordDict));
            System.out.println();
        }
    }
}

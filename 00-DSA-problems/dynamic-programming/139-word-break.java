import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class WordBreak {

    public boolean wordBreak(String s, List<String> wordDict) {
        Set<String> words = new HashSet<>(wordDict);
        return canSegment(s, words);
    }

    private boolean canSegment(String s, Set<String> words) {
        if (s.isEmpty())
            return true;
        for (int i = 1; i <= s.length(); i++) {
            String prefix = s.substring(0, i);
            String suffix = s.substring(i);
            if (words.contains(prefix) && canSegment(suffix, words)) {
                return true;
            }
        }
        return false;
    }

}

class WordBreak2 {

    public boolean wordBreak(String s, List<String> wordDict) {
        Set<String> words = new HashSet<>(wordDict);
        Map<String, Boolean> memo = new HashMap<>();
        return canSegment(s, words, memo);
    }

    private boolean canSegment(String s, Set<String> words, Map<String, Boolean> memo) {
        if (memo.containsKey(s))
            return memo.get(s);
        if (s.isEmpty())
            return true;
        for (int i = 1; i <= s.length(); i++) {
            String prefix = s.substring(0, i);
            String suffix = s.substring(i);
            if (words.contains(prefix) && canSegment(suffix, words, memo)) {
                memo.put(s, true);
                return true;
            }
        }
        memo.put(s, false);
        return false;
    }

}

class WordBreak3 {

    // ⏱️ Time & Space Complexity
    // Time: O(n²), where n = s.length()
    // (due to substring calls inside two nested loops)
    // Space: O(n + k), where n is for dp[], and k is the size of the dictionary set
    public boolean wordBreak(String s, List<String> wordDict) {
        Set<String> wordSet = new HashSet<>(wordDict);
        int n = s.length();

        boolean[] dp = new boolean[n + 1];
        dp[0] = true;

        for (int i = 1; i <= n; i++) {
            for (int j = 0; j < i; j++) {
                String word = s.substring(j, i);
                if (dp[j] && wordSet.contains(word)) {
                    dp[i] = true;
                    break;
                }
            }
        }

        return dp[n];
    }

}

/*
 * Problem: Word Break
 * Given a string 's' and a dictionary 'wordDict', determine if 's' can be segmented
 * into a space-separated sequence of one or more dictionary words.
 *
 * Key Constraints:
 * - Words in the dictionary can be reused.
 * - Order matters: we must match the full string.
 *
 * Approach: Dynamic Programming (Bottom-Up)
 * 
 * Idea:
 * - Use a boolean array dp[] where dp[i] indicates whether the substring s[0..i-1]
 *   can be segmented using words from wordDict.
 * - dp[0] = true, meaning an empty string is trivially segmentable.
 *
 * Transition:
 * - For each index i from 1 to s.length(), check every j < i.
 * - If dp[j] is true and s[j..i-1] is in the dictionary, then dp[i] = true.
 * - Break early to avoid unnecessary checks once a valid segmentation is found.
 *
 * Why it works:
 * - We're building up the solution from smaller substrings to larger ones.
 * - At every point, we ask: "Is there a previous cut (at j) that leads to a valid
 *   segmentation of the rest (s[j..i-1])?"
 * 
 * Optimization:
 * - Use a HashSet for wordDict to allow O(1) lookups.
 *
 * Time Complexity: O(n^2) where n = length of the string (due to nested loops and substring)
 * Space Complexity: O(n) for the dp array (+ O(k) for the word set where k is total characters in dict)
 *
 * Example:
 * s = "applepenapple"
 * wordDict = ["apple", "pen"]
 * dp builds up like:
 * dp[0] = true
 * dp[5] = true ("apple")
 * dp[8] = true ("pen")
 * dp[13] = true ("apple")
 * Final answer: dp[13] = true => "apple pen apple" is valid
 *
 * Easy way to remember:
 * - Build solutions from the left
 * - At each step i, check all cuts j < i
 * - If left part is valid (dp[j]) and right part is in dict → mark dp[i] = true
 */

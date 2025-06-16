import java.util.*;
/*
 * Given two strings s and t, return true if s is a subsequence of t, or false
 * otherwise.
 * 
 * A subsequence of a string is a new string that is formed from the original
 * string by deleting some (can be none) of the characters without disturbing
 * the relative positions of the remaining characters. (i.e., "ace" is a
 * subsequence of "abcde" while "aec" is not).
 * 
 * Example 1:
 * Input: s = "abc", t = "ahbgdc"
 * Output: true
 * 
 * Example 2:
 * Input: s = "axc", t = "ahbgdc"
 * Output: false
 */


class SubsequenceSolutions {
    
    // Solution 1: Two Pointers (RECOMMENDED for single query)
    // Time: O(n), Space: O(1) where n = length of t
    public boolean isSubsequence1(String s, String t) {
        int i = 0, j = 0;
        
        while (i < s.length() && j < t.length()) {
            if (s.charAt(i) == t.charAt(j)) {
                i++; // Move pointer in s only when characters match
            }
            j++; // Always move pointer in t
        }
        
        // If we've matched all characters in s
        return i == s.length();
    }
    
    // Solution 2: Recursive approach
    // Time: O(n), Space: O(n) due to recursion stack
    public boolean isSubsequence2(String s, String t) {
        return isSubsequenceRecursive(s, t, 0, 0);
    }
    
    private boolean isSubsequenceRecursive(String s, String t, int i, int j) {
        // Base cases
        if (i == s.length()) return true;  // All characters in s are matched
        if (j == t.length()) return false; // Reached end of t but not s
        
        if (s.charAt(i) == t.charAt(j)) {
            // Characters match, advance both pointers
            return isSubsequenceRecursive(s, t, i + 1, j + 1);
        } else {
            // Characters don't match, advance only t's pointer
            return isSubsequenceRecursive(s, t, i, j + 1);
        }
    }
    
    // Solution 3: Using indexOf for each character
    // Time: O(n * m), Space: O(1) where n = length of s, m = length of t
    public boolean isSubsequence3(String s, String t) {
        int lastIndex = -1;
        
        for (char c : s.toCharArray()) {
            // Find the character after the last found position
            lastIndex = t.indexOf(c, lastIndex + 1);
            if (lastIndex == -1) {
                return false; // Character not found
            }
        }
        
        return true;
    }
    
    // Solution 4: Dynamic Programming (overkill for this problem but educational)
    // Time: O(n * m), Space: O(n * m)
    public boolean isSubsequence4(String s, String t) {
        int m = s.length(), n = t.length();
        
        // dp[i][j] = true if s[0...i-1] is subsequence of t[0...j-1]
        boolean[][] dp = new boolean[m + 1][n + 1];
        
        // Empty string is subsequence of any string
        for (int j = 0; j <= n; j++) {
            dp[0][j] = true;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = dp[i][j - 1];
                }
            }
        }
        
        return dp[m][n];
    }
    
    // Solution 5: Optimized for multiple queries (Follow-up)
    // Preprocess t to create character position maps
    // Time: Preprocessing O(n), Query O(m * log n) where n = length of t, m = length of s
    public class SubsequenceChecker {
        private Map<Character, List<Integer>> charPositions;
        
        public SubsequenceChecker(String t) {
            charPositions = new HashMap<>();
            
            // Build position map for each character in t
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                charPositions.computeIfAbsent(c, k -> new ArrayList<>()).add(i);
            }
        }
        
        public boolean isSubsequence(String s) {
            int currentPos = -1;
            
            for (char c : s.toCharArray()) {
                List<Integer> positions = charPositions.get(c);
                if (positions == null) {
                    return false; // Character not in t
                }
                
                // Binary search for the first position > currentPos
                int nextPos = binarySearchNext(positions, currentPos);
                if (nextPos == -1) {
                    return false; // No valid position found
                }
                
                currentPos = nextPos;
            }
            
            return true;
        }
        
        private int binarySearchNext(List<Integer> positions, int target) {
            int left = 0, right = positions.size() - 1;
            int result = -1;
            
            while (left <= right) {
                int mid = left + (right - left) / 2;
                if (positions.get(mid) > target) {
                    result = positions.get(mid);
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            }
            
            return result;
        }
    }
    
    // Test method
    public static void main(String[] args) {
        SubsequenceSolutions solution = new SubsequenceSolutions();
        
        // Test cases
        String[][] testCases = {
            {"abc", "ahbgdc"},
            {"axc", "ahbgdc"},
            {"", "ahbgdc"},
            {"abc", ""},
            {"", ""},
            {"ace", "abcde"},
            {"aec", "abcde"},
            {"b", "abc"},
            {"ac", "abc"}
        };
        
        System.out.println("Testing basic solutions:\n");
        
        for (String[] test : testCases) {
            String s = test[0];
            String t = test[1];
            
            System.out.println("s = \"" + s + "\", t = \"" + t + "\"");
            System.out.println("Solution 1: " + solution.isSubsequence1(s, t));
            System.out.println("Solution 2: " + solution.isSubsequence2(s, t));
            System.out.println("Solution 3: " + solution.isSubsequence3(s, t));
            System.out.println("Solution 4: " + solution.isSubsequence4(s, t));
            System.out.println();
        }
        
        // Test optimized solution for multiple queries
        System.out.println("Testing optimized solution for multiple queries:");
        String t = "ahbgdc";
        SubsequenceChecker checker = solution.new SubsequenceChecker(t);
        
        String[] queries = {"abc", "axc", "ac", "adc"};
        for (String s : queries) {
            System.out.println("s = \"" + s + "\": " + checker.isSubsequence(s));
        }
    }
    
}

/*
ANALYSIS:

Solution 1 - Two Pointers (RECOMMENDED):
- Pros: Optimal time O(n) and space O(1), simple and intuitive
- Cons: None for single query
- Best for: Single query problems, interviews

Solution 2 - Recursive:
- Pros: Clean and easy to understand
- Cons: O(n) space due to call stack, potential stack overflow
- Best for: Educational purposes

Solution 3 - Using indexOf:
- Pros: Leverages built-in string methods, concise
- Cons: Worse time complexity O(n*m) in worst case
- Best for: Quick prototyping

Solution 4 - Dynamic Programming:
- Pros: Systematic approach, handles all DP variations
- Cons: Overkill for this problem, O(n*m) time and space
- Best for: Learning DP concepts

Solution 5 - Optimized for Multiple Queries:
- Pros: Efficient when answering many queries on same t
- Cons: Complex implementation, requires preprocessing
- Best for: Follow-up scenarios with multiple queries

Time/Space Complexity Summary:
1. Two Pointers: O(n) time, O(1) space
2. Recursive: O(n) time, O(n) space
3. indexOf: O(n*m) time, O(1) space
4. DP: O(n*m) time, O(n*m) space
5. Multiple Queries: O(n) preprocessing, O(m log n) per query

For most cases, Solution 1 (Two Pointers) is the best choice.
*/

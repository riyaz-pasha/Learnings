/*
 * Suggest valid dictionary words within edit distance of 1 or 2 from the input
 * string.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WordSuggestionWithTypos {

    class TrieNode {

        Map<Character, TrieNode> children;
        String word;

        public TrieNode() {
            this.children = new HashMap<>();
        }

    }

    private final TrieNode root;

    public WordSuggestionWithTypos(List<String> dict) {
        this.root = new TrieNode();
        for (String word : dict) {
            this.insert(word);
        }
    }

    private void insert(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node = node.children.computeIfAbsent(ch, _ -> new TrieNode());
        }
        node.word = word;
    }

    public List<String> getSuggestions(String input, int maxDistance) {
        List<String> result = new ArrayList<>();

        this.dfs(input, root, 0, maxDistance, result);

        return result;
    }

    private void dfs(String input, TrieNode node, int index, int remainingEdits, List<String> result) {
        if (remainingEdits < 0) {
            return;
        }
        if (node.word != null && index == input.length()) {
            result.add(node.word);
        }

        /*
         * Case 3: Insertion (skip a character in input)
         * You're inserting a character into the input string to match a character in
         * the Trie.
         * ➤ Interpretation:
         * You move forward in the input string but stay at the same Trie node,
         * simulating the idea of "inserting" a character into the input to try and
         * match the current Trie path later.
         * 
         * 📘 Example:
         * - Input: "helo"
         * - Dictionary word: "hello"
         * - Operation: Insert 'l' at position 3 of "helo" to get "hello".
         * State at mismatch:
         * - Input index = 2 ('l')
         * - Trie node expects 'l' but input is 'o'
         * → We assume that the user forgot a character → we skip ahead in input
         * (index+1) without consuming any character in the Trie.
         * So we say:
         * - Let’s try to skip this character in the input (simulate an insertion) and
         * see if the rest matches
         */
        if (index < input.length()) {
            this.dfs(input, node, index + 1, remainingEdits - 1, result);
        }

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            char ch = entry.getKey();
            TrieNode childNode = entry.getValue();

            // 1: Match
            if (index < input.length() && input.charAt(index) == ch) {
                this.dfs(input, childNode, index + 1, remainingEdits, result);
            }

            // 2: Substitution
            if (index < input.length() && input.charAt(index) != ch) {
                this.dfs(input, childNode, index + 1, remainingEdits - 1, result);
            }

            /*
             * Case 4: Deletion (try a character not in input)
             * You're deleting a character from the input string to match a character in the
             * Trie.
             * 
             * ➤ Interpretation:
             * You move forward in the Trie without moving in the input string, simulating
             * the idea of a deletion (i.e., there’s an extra char in the Trie word that
             * shouldn’t be there).
             * 
             * 📘 Example:
             * Input: "helo"
             * - Dictionary word: "helllo"
             * - Operation: Delete one 'l' from "helllo" to match "helo"
             * State at mismatch:
             * - Input index = 3 ('o')
             * - Trie node is at 'l' (extra letter)
             * → We say:
             * Maybe this extra 'l' in the dictionary shouldn't be there. Let's try moving
             * ahead in the Trie (child node), and keep the input index same (we didn’t
             * consume input char), simulating a deletion from Trie word.
             */
            dfs(input, childNode, index, remainingEdits - 1, result);
        }
    }

    /*
     * Time Complexity:
     * Depends on:
     * - Branching factor B (average children per node),
     * - Max depth D,
     * - Edit limit K,
     * - So worst case: O(B^K × D) — but fast in practice for small K.
     * 
     * Space Complexity:
     * - Trie: O(N * L) for N words of length L
     * - Call stack: O(L + K)
     */
    private void dfs2(String input, TrieNode node, int index, int remainingEdits, List<String> result) {
        if (node.word != null && index == input.length() && remainingEdits >= 0) {
            result.add(node.word);
        }

        /*
         * Insertion : skip a character in input
         * The user accidentally typed an extra character (i.e., the dictionary word
         * doesn’t have this character), so we choose to skip this character in the
         * input to continue matching.
         * Let’s say:
         * Input: "helo"
         * Dictionary word: "hello"
         * Current position: index = 2 (looking at 'l')
         * Trie node has 'l' as next child
         * But we assume 'l' in input was a mistake (maybe a typo)
         * We try skipping it, hoping the rest might still match.
         */
        if (index < input.length() && remainingEdits > 0) {
            this.dfs2(input, node, index + 1, remainingEdits - 1, result);
        }

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            char ch = entry.getKey();
            TrieNode childNode = entry.getValue();
            if (index < input.length()) {
                if (ch == input.charAt(index)) {
                    // Match
                    this.dfs2(input, childNode, index + 1, remainingEdits, result);
                } else if (remainingEdits > 0) {
                    // Substitution
                    this.dfs2(input, childNode, index + 1, remainingEdits - 1, result);
                }
            }

            // Deletion: skip character in Trie
            if (remainingEdits > 0) {
                this.dfs2(input, childNode, index, remainingEdits - 1, result);
            }
        }

    }

}

/*
 * Edit distance is the minimum number of operations required to convert one
 * word to another, where operations include:
 * - Insert a character
 * - Delete a character
 * - Replace a character
 */

class EditDistance {

    // This version is exponential in time: O(3^n) in worst case due to repeated
    // subproblems.
    public int editDistance(String s1, String s2) {
        return this.helper(s1, s2, s1.length() - 1, s2.length() - 1);
    }

    private int helper(String s1, String s2, int i, int j) {
        if (i < 0) {
            return j + 1; // insert all remaining in word2
        }
        if (j < 0) {
            return i + 1; // delete all remaining in word1
        }

        if (s1.charAt(i) == s2.charAt(j)) {
            return this.helper(s1, s2, i - 1, j - 1);
        }
        int insert = this.helper(s1, s2, i, j - 1); // inserting at word1 so word1 remains there and word2 decreases
        int delete = this.helper(s1, s2, i - 1, j); // deleting at word1 so word1 moves left there and word2 remains
        int replace = this.helper(s1, s2, i - 1, j - 1);
        return 1 + Math.min(insert, Math.min(delete, replace));
    }

}

class EditDistanceMemoized {

    /*
     * 🧠 Time & Space Complexity
     * Time: O(m * n) because each subproblem (i, j) is solved once.
     * Space: O(m * n) for memo table + recursion stack.
     */
    public int minDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] memo = new int[m][n];

        // Initialize memo with -1 (meaning not calculated)
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }

        return helper(word1, word2, m - 1, n - 1, memo);
    }

    private int helper(String word1, String word2, int i, int j, int[][] memo) {
        // Base cases
        if (i < 0)
            return j + 1; // Insert remaining
        if (j < 0)
            return i + 1; // Delete remaining

        if (memo[i][j] != -1)
            return memo[i][j];

        if (word1.charAt(i) == word2.charAt(j)) {
            memo[i][j] = helper(word1, word2, i - 1, j - 1, memo);
        } else {
            int insert = helper(word1, word2, i, j - 1, memo);
            int delete = helper(word1, word2, i - 1, j, memo);
            int replace = helper(word1, word2, i - 1, j - 1, memo);

            memo[i][j] = 1 + Math.min(insert, Math.min(delete, replace));
        }

        return memo[i][j];
    }

}

class EditDistanceBottomUp {

    /*
     * Time & Space Complexity
     * Time: O(m * n)
     * Space: O(m * n)
     */
    public int minDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] dp = new int[m + 1][n + 1];

        // Base cases: dp[i][0] = i deletes, dp[0][j] = j inserts
        for (int i = 0; i <= m; i++)
            dp[i][0] = i;
        for (int j = 0; j <= n; j++)
            dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // no operation needed
                } else {
                    dp[i][j] = 1 + Math.min(
                            dp[i - 1][j], // delete
                            Math.min(
                                    dp[i][j - 1], // insert
                                    dp[i - 1][j - 1] // replace
                            ));
                }
            }
        }

        return dp[m][n];
    }

}

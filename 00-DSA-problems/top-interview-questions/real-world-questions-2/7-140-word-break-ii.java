import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class WordBreak {

    public List<String> wordBreak(String s, List<String> wordDict) {
        // Example:
        // s = "catsanddog"
        // dict = ["cat", "cats", "and", "sand", "dog"]

        // Convert list → set for O(1) lookup
        Set<String> dict = new HashSet<>(wordDict);

        // Memo: startIndex -> list of sentences
        Map<Integer, List<String>> memo = new HashMap<>();

        return dfs(s, dict, 0, memo);
    }

    /**
     * DFS(start):
     * Returns all valid sentences that can be formed from index `start`
     *
     * ------------------------------------------
     * 🌟 WALKTHROUGH (for "catsanddog")
     * ------------------------------------------
     *
     * dfs(0):
     * try "cat" → valid → dfs(3)
     * try "cats" → valid → dfs(4)
     *
     * dfs(3): ("sanddog")
     * try "sand" → valid → dfs(7)
     *
     * dfs(7): ("dog")
     * try "dog" → valid → dfs(10)
     *
     * dfs(10):
     * reached end → return [""]
     *
     * Now build back:
     * dfs(7) → ["dog"]
     * dfs(3) → ["sand dog"]
     * dfs(0) → ["cat sand dog"]
     *
     * Similarly:
     * dfs(4): ("anddog")
     * "and" → dfs(7) → reuse memo → ["dog"]
     *
     * dfs(4) → ["and dog"]
     * dfs(0) → ["cats and dog"]
     *
     * Final result:
     * ["cat sand dog", "cats and dog"]
     */
    private List<String> dfs(
            String s,
            Set<String> dict,
            int start,
            Map<Integer, List<String>> memo) {

        // 🔁 If already computed → return from memo
        if (memo.containsKey(start)) {
            return memo.get(start);
        }

        List<String> result = new ArrayList<>();

        // 🧱 Base case:
        // If we reached end → return empty string as base
        if (start == s.length()) {
            result.add(""); // acts as terminator for building sentences
            return result;
        }

        // 🔍 Try all substrings starting from `start`
        for (int end = start + 1; end <= s.length(); end++) {

            String word = s.substring(start, end);

            // ❌ Skip if not in dictionary
            if (!dict.contains(word))
                continue;

            // ✅ Valid word found → solve remaining string
            List<String> subSentences = dfs(s, dict, end, memo);

            // 🔗 Combine current word with results from suffix
            for (String sub : subSentences) {

                // If suffix is empty → just add word
                if (sub.isEmpty()) {
                    result.add(word);
                } else {
                    result.add(word + " " + sub);
                }
            }
        }

        // 💾 Save result for this start index
        memo.put(start, result);

        return result;
    }

    public List<String> wordBreak2(String s, List<String> wordDict) {
        Set<String> dict = new HashSet<>(wordDict);

        // dp[i] = list of sentences that can be formed from s[0..i)
        List<List<String>> dp = new ArrayList<>();

        for (int i = 0; i <= s.length(); i++) {
            dp.add(new ArrayList<>());
        }

        dp.get(0).add(""); // base case

        for (int end = 1; end <= s.length(); end++) {
            for (int start = 0; start < end; start++) {

                String word = s.substring(start, end);

                if (!dict.contains(word))
                    continue;

                for (String prev : dp.get(start)) {
                    if (prev.isEmpty()) {
                        dp.get(end).add(word);
                    } else {
                        dp.get(end).add(prev + " " + word);
                    }
                }
            }
        }

        return dp.get(s.length());
    }
}

class WordBreakTrie {

    // Trie Node
    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isWord = false;
    }

    // Build Trie from dictionary
    private TrieNode buildTrie(List<String> wordDict) {
        TrieNode root = new TrieNode();

        for (String word : wordDict) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                int idx = c - 'a';

                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }

                node = node.children[idx];
            }
            node.isWord = true;
        }

        return root;
    }

    public List<String> wordBreak(String s, List<String> wordDict) {
        TrieNode root = buildTrie(wordDict);

        // Memo: start index → list of sentences
        Map<Integer, List<String>> memo = new HashMap<>();

        return dfs(s, 0, root, memo);
    }

    /**
     * DFS using Trie traversal
     */
    private List<String> dfs(String s, int start, TrieNode root,
            Map<Integer, List<String>> memo) {

        // 🔁 Memo check
        if (memo.containsKey(start)) {
            return memo.get(start);
        }

        List<String> result = new ArrayList<>();

        // 🧱 Base case
        if (start == s.length()) {
            result.add("");
            return result;
        }

        TrieNode node = root;

        // 🚀 Traverse Trie instead of substring
        for (int end = start; end < s.length(); end++) {

            char c = s.charAt(end);
            int idx = c - 'a';

            // ❌ No matching prefix → stop early
            if (node.children[idx] == null) {
                break;
            }

            node = node.children[idx];

            // ✅ Found a valid word in Trie
            if (node.isWord) {

                String word = s.substring(start, end + 1);

                // Recurse for remaining string
                List<String> subSentences = dfs(s, end + 1, root, memo);

                for (String sub : subSentences) {
                    if (sub.isEmpty()) {
                        result.add(word);
                    } else {
                        result.add(word + " " + sub);
                    }
                }
            }
        }

        memo.put(start, result);
        return result;
    }
}

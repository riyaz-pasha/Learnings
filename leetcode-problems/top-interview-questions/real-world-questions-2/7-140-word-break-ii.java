import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class WordBreak {

    public List<String> wordBreak(String s, List<String> wordDict) {
        Set<String> dict = new HashSet<>(wordDict);

        // Memo: startIndex -> list of sentences from this index
        Map<Integer, List<String>> memo = new HashMap<>();

        return dfs(s, dict, 0, memo);
    }

    /**
     * Returns all valid sentences that can be formed
     * starting from index `start`
     */
    private List<String> dfs(
            String s,
            Set<String> dict,
            int start,
            Map<Integer, List<String>> memo) {
        // If already computed, return cached result
        if (memo.containsKey(start)) {
            return memo.get(start);
        }

        List<String> result = new ArrayList<>();

        // Base case: reached end of string
        if (start == s.length()) {
            result.add(""); // Important: empty sentence
            return result;
        }

        // Try every possible word starting at `start`
        for (int end = start + 1; end <= s.length(); end++) {
            String word = s.substring(start, end);

            if (!dict.contains(word))
                continue;

            // Get all sentences from the remaining suffix
            List<String> subSentences = dfs(s, dict, end, memo);

            for (String sub : subSentences) {
                // Append current word properly
                if (sub.isEmpty()) {
                    result.add(word);
                } else {
                    result.add(word + " " + sub);
                }
            }
        }

        memo.put(start, result);
        return result;
    }
}

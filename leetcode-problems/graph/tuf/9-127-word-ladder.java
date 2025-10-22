
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class WordLadder {

    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        Set<String> dict = new HashSet<>(wordList);
        if (!dict.contains(endWord)) {
            return 0;
        }

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.offer(beginWord);
        visited.add(beginWord);

        int len = 0;
        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                String word = queue.poll();
                if (word.equals(endWord)) {
                    return len + 1;
                }

                for (int pos = 0; pos < word.length(); pos++) {
                    for (int ch = 'a'; ch <= 'z'; ch++) {
                        char[] arr = word.toCharArray();
                        arr[pos] = (char) ch;

                        String str = new String(arr);
                        if (dict.contains(str) && !visited.contains(str)) {
                            queue.offer(str);
                            visited.add(str);
                        }
                    }
                }
            }

            len++;
        }

        return 0;
    }
    /*
     * Complexity (exact / careful)
     * Let:
     * N = number of words in wordList (|dict|)
     * L = length of each word (assume words equal length)
     * 
     * Single-source BFS (your approach)
     * Time:
     * For each dequeued word we try L positions × 26 letters → 26 * L candidate
     * strings.
     * Building each candidate string costs O(L) (constructing String from char[]).
     * In the worst case we may process up to O(N) words.
     * So a tight worst-case bound: O(N * 26 * L * L) = O(26 * N * L^2).
     * 
     * Many implementations simplify to O(N * L * 26) treating string ops as O(1) or
     * using amortized string hashing, but the precise cost includes the O(L) string
     * creation.
     * 
     * Space:
     * O(N * L) to store the dictionary words (hash set).
     * O(N * L) additional for visited and queue in worst case (storing up to all
     * words).
     * So auxiliary space = O(N * L).
     * 
     * Bidirectional BFS (recommended)
     * Time:
     * The search frontier grows from both ends; if the branching factor is b ≈ 26*L
     * and the shortest path length is d, bidirectional BFS can explore roughly
     * O(b^{d/2}) nodes instead of O(b^d).
     * 
     * More practically, worst-case bound similar to single-direction but often much
     * faster; tight worst-case asymptotic is still O(26 * N * L^2) but constant
     * factors are far smaller.
     * 
     * Space:
     * Still O(N * L) worst-case for sets and queue/frontiers.
     */

}

class Solution {

    /*
     * Why this is better in practice
     * Reduces total visited nodes by expanding the smaller side.
     * Removing from dict acts as visited and reduces membership checks.
     * Avoids toCharArray() and new String() inside inner loops more than necessary
     * (we still need new String(arr) to check dict).
     */
    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        // Edge cases
        if (beginWord == null || endWord == null)
            return 0;
        if (beginWord.equals(endWord))
            return 1;

        Set<String> dict = new HashSet<>(wordList);
        if (!dict.contains(endWord))
            return 0;

        Set<String> beginSet = new HashSet<>();
        Set<String> endSet = new HashSet<>();

        beginSet.add(beginWord);
        endSet.add(endWord);
        dict.remove(endWord); // avoid revisiting the end from dict
        dict.remove(beginWord);

        int level = 1;

        while (!beginSet.isEmpty() && !endSet.isEmpty()) {
            // Always expand the smaller frontier for efficiency
            if (beginSet.size() > endSet.size()) {
                Set<String> tmp = beginSet;
                beginSet = endSet;
                endSet = tmp;
            }

            Set<String> nextLevel = new HashSet<>();

            for (String word : beginSet) {
                char[] arr = word.toCharArray();
                for (int i = 0; i < arr.length; i++) {
                    char old = arr[i];
                    for (char ch = 'a'; ch <= 'z'; ch++) {
                        if (ch == old)
                            continue;
                        arr[i] = ch;
                        String cand = new String(arr);

                        if (endSet.contains(cand)) {
                            return level + 1; // found connection
                        }

                        if (dict.contains(cand)) {
                            nextLevel.add(cand);
                            dict.remove(cand); // mark visited, prevents re-adding
                        }
                    }
                    arr[i] = old; // restore
                }
            }

            beginSet = nextLevel;
            level++;
        }

        return 0;
    }
}

// ============================================================================
// Solution 1: Standard BFS with HashSet (RECOMMENDED)
// Time: O(M² × N), Space: O(M × N)
// ============================================================================
class Solution1 {
    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        Set<String> wordSet = new HashSet<>(wordList);

        // If endWord is not in the dictionary, no solution exists
        if (!wordSet.contains(endWord)) {
            return 0;
        }

        Queue<String> queue = new LinkedList<>();
        queue.offer(beginWord);
        int level = 1;

        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                String currentWord = queue.poll();

                // Try changing each character
                char[] chars = currentWord.toCharArray();
                for (int j = 0; j < chars.length; j++) {
                    char originalChar = chars[j];

                    // Try all 26 letters
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == originalChar)
                            continue;

                        chars[j] = c;
                        String newWord = new String(chars);

                        if (newWord.equals(endWord)) {
                            return level + 1;
                        }

                        if (wordSet.contains(newWord)) {
                            queue.offer(newWord);
                            wordSet.remove(newWord); // Mark as visited
                        }
                    }

                    chars[j] = originalChar; // Restore
                }
            }
            level++;
        }

        return 0;
    }
}

// ============================================================================
// Solution 2: Bidirectional BFS (OPTIMIZED)
// Time: O(M² × N) - but faster in practice, Space: O(M × N)
// ============================================================================
class Solution2 {
    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        Set<String> wordSet = new HashSet<>(wordList);
        if (!wordSet.contains(endWord)) {
            return 0;
        }

        Set<String> beginSet = new HashSet<>();
        Set<String> endSet = new HashSet<>();
        beginSet.add(beginWord);
        endSet.add(endWord);

        int level = 1;

        while (!beginSet.isEmpty() && !endSet.isEmpty()) {
            // Always expand the smaller set
            if (beginSet.size() > endSet.size()) {
                Set<String> temp = beginSet;
                beginSet = endSet;
                endSet = temp;
            }

            Set<String> nextLevel = new HashSet<>();

            for (String word : beginSet) {
                char[] chars = word.toCharArray();

                for (int i = 0; i < chars.length; i++) {
                    char oldChar = chars[i];

                    for (char c = 'a'; c <= 'z'; c++) {
                        chars[i] = c;
                        String newWord = new String(chars);

                        if (endSet.contains(newWord)) {
                            return level + 1;
                        }

                        if (wordSet.contains(newWord)) {
                            nextLevel.add(newWord);
                            wordSet.remove(newWord);
                        }
                    }

                    chars[i] = oldChar;
                }
            }

            beginSet = nextLevel;
            level++;
        }

        return 0;
    }
}

// ============================================================================
// Solution 3: BFS with Pre-computed Pattern Dictionary
// Time: O(M² × N), Space: O(M² × N)
// ============================================================================
class Solution3 {
    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        if (!wordList.contains(endWord)) {
            return 0;
        }

        // Pre-compute pattern to words mapping
        Map<String, List<String>> patternMap = new HashMap<>();
        wordList.add(beginWord);

        for (String word : wordList) {
            for (int i = 0; i < word.length(); i++) {
                String pattern = word.substring(0, i) + "*" + word.substring(i + 1);
                patternMap.computeIfAbsent(pattern, k -> new ArrayList<>()).add(word);
            }
        }

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.offer(beginWord);
        visited.add(beginWord);

        int level = 1;

        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                String word = queue.poll();

                if (word.equals(endWord)) {
                    return level;
                }

                for (int j = 0; j < word.length(); j++) {
                    String pattern = word.substring(0, j) + "*" + word.substring(j + 1);

                    for (String neighbor : patternMap.getOrDefault(pattern, new ArrayList<>())) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.offer(neighbor);
                        }
                    }
                }
            }

            level++;
        }

        return 0;
    }
}

// ============================================================================
// Test Class
// ============================================================================
class WordLadderTest {
    public static void main(String[] args) {
        // Test case 1
        String beginWord1 = "hit";
        String endWord1 = "cog";
        List<String> wordList1 = Arrays.asList("hot", "dot", "dog", "lot", "log", "cog");

        Solution1 sol1 = new Solution1();
        System.out.println("Test 1 - Expected: 5, Got: " + sol1.ladderLength(beginWord1, endWord1, wordList1));

        // Test case 2
        String beginWord2 = "hit";
        String endWord2 = "cog";
        List<String> wordList2 = Arrays.asList("hot", "dot", "dog", "lot", "log");

        System.out.println("Test 2 - Expected: 0, Got: " + sol1.ladderLength(beginWord2, endWord2, wordList2));
    }
}

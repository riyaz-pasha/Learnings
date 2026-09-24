import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class WordLadderII {

    /*
     * Why this works
     * BFS finds the minimum number of transformations (shortest depth).
     * During BFS we only record parents for nodes discovered at the current minimal
     * depth, ensuring the parent graph forms a DAG of shortest paths.
     * Backtracking traverses that DAG from endWord back to beginWord to enumerate
     * all shortest sequences.
     * 
     * Complexity (precise-ish)
     * Let N = |wordList|, L = length of words.
     * Time: worst-case ~ O(N * L * 26 * L) when counting string-construction cost
     * (new String(arr) is O(L)). Practical runtime is much better because BFS
     * prunes early and each word is processed at most once.
     * Space: O(N * L) for dictionary, parents map, and BFS frontier; plus output
     * space for result lists.
     */

    public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
        List<List<String>> results = new ArrayList<>();
        Set<String> dict = new HashSet<>(wordList);
        if (!dict.contains(endWord))
            return results; // no possible transformation
        if (beginWord.equals(endWord)) {
            results.add(List.of(beginWord));
            return results;
        }

        // parents: child -> list of parents (words that can reach child on a shortest
        // path)
        Map<String, List<String>> parents = new HashMap<>();

        // BFS frontier: current level words
        Set<String> current = new HashSet<>();
        current.add(beginWord);

        boolean found = false;
        while (!current.isEmpty() && !found) {
            // remove these from dict so we don't revisit them in future levels
            dict.removeAll(current);

            Set<String> nextLevel = new HashSet<>();
            for (String word : current) {
                char[] arr = word.toCharArray();
                for (int i = 0; i < arr.length; i++) {
                    char old = arr[i];
                    for (char ch = 'a'; ch <= 'z'; ch++) {
                        if (ch == old)
                            continue;
                        arr[i] = ch;
                        String cand = new String(arr);
                        if (!dict.contains(cand))
                            continue; // only consider not-yet-visited words
                        // record that 'word' is a parent of 'cand'
                        parents.computeIfAbsent(cand, k -> new ArrayList<>()).add(word);
                        // add candidate to next level
                        nextLevel.add(cand);
                        if (cand.equals(endWord)) {
                            found = true; // record that we've reached end at this BFS depth
                        }
                    }
                    arr[i] = old;
                }
            }

            current = nextLevel;
        }

        if (!found)
            return results;

        // Backtrack from endWord to beginWord using parents map to build all shortest
        // paths
        LinkedList<String> path = new LinkedList<>();
        path.add(endWord);
        backtrack(endWord, beginWord, parents, path, results);
        return results;
    }

    private void backtrack(String word, String beginWord, Map<String, List<String>> parents,
            LinkedList<String> path, List<List<String>> results) {
        if (word.equals(beginWord)) {
            List<String> valid = new ArrayList<>(path);
            Collections.reverse(valid);
            results.add(valid);
            return;
        }
        List<String> ps = parents.get(word);
        if (ps == null)
            return;
        for (String p : ps) {
            path.addLast(p);
            backtrack(p, beginWord, parents, path, results);
            path.removeLast();
        }
    }

    // Example test
    public static void main(String[] args) {
        WordLadderII solver = new WordLadderII();
        String begin = "hit";
        String end = "cog";
        List<String> words = List.of("hot", "dot", "dog", "lot", "log", "cog");
        List<List<String>> ladders = solver.findLadders(begin, end, words);
        System.out.println(ladders);
        // Expected:
        // [["hit","hot","dot","dog","cog"], ["hit","hot","lot","log","cog"]]
    }
}

// ============================================================================
// Solution 1: BFS + DFS (Build Graph then Backtrack) - RECOMMENDED
// Time: O(N × M² + N × K), Space: O(N × M)
// where N = wordList size, M = word length, K = number of paths
// ============================================================================
class Solution1 {
    public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
        List<List<String>> result = new ArrayList<>();
        Set<String> wordSet = new HashSet<>(wordList);

        if (!wordSet.contains(endWord)) {
            return result;
        }

        // Step 1: BFS to build the graph and find shortest distance
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> distance = new HashMap<>();

        bfs(beginWord, endWord, wordSet, graph, distance);

        // Step 2: DFS to find all paths
        List<String> path = new ArrayList<>();
        path.add(beginWord);
        dfs(beginWord, endWord, graph, distance, path, result);

        return result;
    }

    private void bfs(String beginWord, String endWord, Set<String> wordSet,
            Map<String, List<String>> graph, Map<String, Integer> distance) {
        for (String word : wordSet) {
            graph.put(word, new ArrayList<>());
        }
        graph.put(beginWord, new ArrayList<>());

        Queue<String> queue = new LinkedList<>();
        queue.offer(beginWord);
        distance.put(beginWord, 0);

        boolean found = false;

        while (!queue.isEmpty() && !found) {
            int size = queue.size();
            Set<String> visited = new HashSet<>();

            for (int i = 0; i < size; i++) {
                String currentWord = queue.poll();
                int currentDist = distance.get(currentWord);

                char[] chars = currentWord.toCharArray();
                for (int j = 0; j < chars.length; j++) {
                    char originalChar = chars[j];

                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == originalChar)
                            continue;

                        chars[j] = c;
                        String newWord = new String(chars);

                        if (wordSet.contains(newWord)) {
                            graph.get(currentWord).add(newWord);

                            if (!distance.containsKey(newWord)) {
                                distance.put(newWord, currentDist + 1);

                                if (newWord.equals(endWord)) {
                                    found = true;
                                } else {
                                    visited.add(newWord);
                                }
                            }
                        }
                    }

                    chars[j] = originalChar;
                }
            }

            queue.addAll(visited);
        }
    }

    private void dfs(String currentWord, String endWord, Map<String, List<String>> graph,
            Map<String, Integer> distance, List<String> path, List<List<String>> result) {
        if (currentWord.equals(endWord)) {
            result.add(new ArrayList<>(path));
            return;
        }

        for (String neighbor : graph.get(currentWord)) {
            if (distance.get(neighbor) == distance.get(currentWord) + 1) {
                path.add(neighbor);
                dfs(neighbor, endWord, graph, distance, path, result);
                path.remove(path.size() - 1);
            }
        }
    }
}

// ============================================================================
// Solution 2: BFS with Level-by-Level Path Building
// Time: O(N × M² × K), Space: O(N × K)
// More intuitive but uses more memory
// ============================================================================
class Solution2 {
    public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
        List<List<String>> result = new ArrayList<>();
        Set<String> wordSet = new HashSet<>(wordList);

        if (!wordSet.contains(endWord)) {
            return result;
        }

        Queue<List<String>> queue = new LinkedList<>();
        List<String> initial = new ArrayList<>();
        initial.add(beginWord);
        queue.offer(initial);

        Set<String> visited = new HashSet<>();
        visited.add(beginWord);

        boolean found = false;

        while (!queue.isEmpty() && !found) {
            int size = queue.size();
            Set<String> levelVisited = new HashSet<>();

            for (int i = 0; i < size; i++) {
                List<String> path = queue.poll();
                String currentWord = path.get(path.size() - 1);

                char[] chars = currentWord.toCharArray();
                for (int j = 0; j < chars.length; j++) {
                    char originalChar = chars[j];

                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == originalChar)
                            continue;

                        chars[j] = c;
                        String newWord = new String(chars);

                        if (!wordSet.contains(newWord) || visited.contains(newWord)) {
                            chars[j] = originalChar;
                            continue;
                        }

                        List<String> newPath = new ArrayList<>(path);
                        newPath.add(newWord);

                        if (newWord.equals(endWord)) {
                            result.add(newPath);
                            found = true;
                        } else {
                            queue.offer(newPath);
                            levelVisited.add(newWord);
                        }
                    }

                    chars[j] = originalChar;
                }
            }

            visited.addAll(levelVisited);
        }

        return result;
    }
}

// ============================================================================
// Solution 3: Optimized BFS + Backtracking (Most Efficient)
// Time: O(N × M²), Space: O(N × M)
// ============================================================================
class Solution3 {

    public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
        List<List<String>> result = new ArrayList<>();
        Set<String> wordSet = new HashSet<>(wordList);

        if (!wordSet.contains(endWord)) {
            return result;
        }

        // Build adjacency list
        Map<String, List<String>> neighbors = new HashMap<>();
        wordSet.add(beginWord);

        for (String word : wordSet) {
            neighbors.put(word, new ArrayList<>());
        }

        for (String word : wordSet) {
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char old = chars[i];
                for (char c = 'a'; c <= 'z'; c++) {
                    chars[i] = c;
                    String newWord = new String(chars);
                    if (wordSet.contains(newWord) && !newWord.equals(word)) {
                        neighbors.get(word).add(newWord);
                    }
                }
                chars[i] = old;
            }
        }

        // BFS to find shortest paths
        Map<String, Integer> distance = new HashMap<>();
        distance.put(beginWord, 0);
        Queue<String> queue = new LinkedList<>();
        queue.offer(beginWord);

        while (!queue.isEmpty()) {
            String word = queue.poll();
            for (String neighbor : neighbors.get(word)) {
                if (!distance.containsKey(neighbor)) {
                    distance.put(neighbor, distance.get(word) + 1);
                    queue.offer(neighbor);
                }
            }
        }

        // DFS backtracking to construct paths
        if (!distance.containsKey(endWord)) {
            return result;
        }

        List<String> path = new ArrayList<>();
        path.add(beginWord);
        backtrack(beginWord, endWord, neighbors, distance, path, result);

        return result;
    }

    private void backtrack(String current, String endWord, Map<String, List<String>> neighbors,
            Map<String, Integer> distance, List<String> path, List<List<String>> result) {
        if (current.equals(endWord)) {
            result.add(new ArrayList<>(path));
            return;
        }

        for (String neighbor : neighbors.get(current)) {
            if (distance.get(neighbor) == distance.get(current) + 1) {
                path.add(neighbor);
                backtrack(neighbor, endWord, neighbors, distance, path, result);
                path.remove(path.size() - 1);
            }
        }
    }
}

// ============================================================================
// Test Class
// ============================================================================
class WordLadderIITest {
    public static void main(String[] args) {
        Solution1 sol = new Solution1();

        // Test case 1
        String beginWord1 = "hit";
        String endWord1 = "cog";
        List<String> wordList1 = Arrays.asList("hot", "dot", "dog", "lot", "log", "cog");

        System.out.println("Test 1:");
        List<List<String>> result1 = sol.findLadders(beginWord1, endWord1, wordList1);
        for (List<String> path : result1) {
            System.out.println(path);
        }
        System.out.println("Expected: [hit,hot,dot,dog,cog] and [hit,hot,lot,log,cog]\n");

        // Test case 2
        String beginWord2 = "hit";
        String endWord2 = "cog";
        List<String> wordList2 = Arrays.asList("hot", "dot", "dog", "lot", "log");

        System.out.println("Test 2:");
        List<List<String>> result2 = sol.findLadders(beginWord2, endWord2, wordList2);
        System.out.println("Result: " + result2);
        System.out.println("Expected: []");
    }
}

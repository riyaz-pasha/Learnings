/**
 * ============================================================================
 * WORD LADDER II - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We are playing a word game. We start with a `beginWord` (e.g., "hit") and 
 * want to reach an `endWord` (e.g., "cog"). 
 * The rules:
 *   1. We can only change exactly ONE letter at a time.
 *   2. Every intermediate word we create MUST exist in the provided dictionary.
 * Our goal is to find ALL possible ways to reach the `endWord` in the FEWEST 
 * number of steps. We need to return the complete sequences.
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: What if the `endWord` is not in the dictionary?
 * A: Then it is impossible to reach. We should immediately return an empty list.
 * 
 * Q: Can the `beginWord` be in the dictionary?
 * A: Yes, it might be, but it doesn't count towards the transformations. We 
 *    don't need to transform to it.
 * 
 * Q: Why do we need ALL shortest paths?
 * A: Because there might be ties! "hit" -> "hot" -> "dot" -> "dog" -> "cog" 
 *    is 5 steps. But "hit" -> "hot" -> "lot" -> "log" -> "cog" is ALSO 5 steps. 
 *    We must return both.
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - WHY NOT JUST DFS? If we just use Backtracking (DFS), we will blindly wander 
 *   through the dictionary, finding extremely long and winding paths before 
 *   we ever find the shortest one. It will cause a Time Limit Exceeded (TLE).
 * - WHY NOT JUST BFS? Breadth-First Search (BFS) is perfect for finding the 
 *   SHORTEST path. But storing and copying full lists of words inside a BFS 
 *   queue consumes a massive amount of memory.
 * - THE TWO-PHASE SOLUTION: 
 *   Phase 1 (BFS): Explore layer-by-layer to find the shortest distance. As we 
 *   explore, we build a "Graph" (Adjacency List) mapping each word to its valid 
 *   children in the next layer.
 *   Phase 2 (DFS): Once the graph is built, we use Backtracking (DFS) to trace 
 *   all paths from `beginWord` to `endWord` using ONLY the optimal edges we saved.
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Explain the BFS + DFS architecture. Interviewers love candidates 
 *   who can break down a complex problem into modular helper functions.
 * - Step 2: Implement the BFS. Explain the "Layer-by-Layer" technique and why 
 *   removing words from the dictionary layer-by-layer prevents infinite loops 
 *   while still allowing multiple parents to share a child.
 * - Step 3: Implement the DFS to reconstruct the paths.
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * beginWord = "hit", endWord = "cog", dict = ["hot","dot","dog","lot","log","cog"]
 * 
 * BFS Layer 0: ["hit"]
 * BFS Layer 1: ["hot"]                  (Graph: hit -> hot)
 * BFS Layer 2: ["dot", "lot"]           (Graph: hot -> dot, hot -> lot)
 * BFS Layer 3: ["dog", "log"]           (Graph: dot -> dog, lot -> log)
 * BFS Layer 4: ["cog"]                  (Graph: dog -> cog, log -> cog)
 * 
 * The BFS stops because we reached "cog". 
 * The DFS then trivially follows the graph from "hit" down to "cog".
 */

import java.util.*;

public class WordLadderII {

    public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
        List<List<String>> result = new ArrayList<>();
        Set<String> dict = new HashSet<>(wordList);

        // Immediate impossible check
        if (!dict.contains(endWord)) {
            return result;
        }

        /*
         * ============================================================
         * THE GRAPH (ADJACENCY LIST)
         * ============================================================
         * We will use this to store ONLY the edges that lead to a shortest path.
         * Key: A word.
         * Value: A list of valid words in the NEXT layer.
         */
        Map<String, List<String>> graph = new HashMap<>();

        // Phase 1: Build the optimal graph using BFS
        boolean found = buildGraphBFS(beginWord, endWord, dict, graph);

        // Phase 2: If the endWord was reached, reconstruct paths using DFS
        if (found) {
            List<String> currentPath = new ArrayList<>();
            currentPath.add(beginWord); // Paths must start with beginWord
            backtrackDFS(beginWord, endWord, graph, currentPath, result);
        }

        return result;
    }

    private boolean buildGraphBFS(String beginWord, String endWord, Set<String> dict, Map<String, List<String>> graph) {
        
        /*
         * ============================================================
         * THE LAYER-BY-LAYER BFS
         * ============================================================
         * Instead of a standard Queue, we use a Set for the current layer.
         * This allows us to cleanly remove an entire layer of words from the 
         * dictionary at once, which is the secret to making this algorithm fast!
         */
        Set<String> currentLayer = new HashSet<>();
        currentLayer.add(beginWord);
        
        boolean reachedEndWord = false;

        while (!currentLayer.isEmpty() && !reachedEndWord) {
            
            /*
             * ------------------------------------------------------------
             * PREVENTING CYCLES (THE MENTAL SHIFT)
             * ------------------------------------------------------------
             * Why do we remove the entire currentLayer from the dict NOW?
             * 
             * If we have "hot", we don't want it to ever transition back to "hit". 
             * By removing the current layer from the available dictionary, we ensure 
             * that our search strictly moves FORWARD to unseen words.
             * 
             * Why not remove words the moment we see them (like standard BFS)?
             * Because MULTIPLE words in the current layer might need to transition 
             * to the SAME word in the next layer! 
             * Example: "dot" -> "dog" AND "log" -> "dog". 
             * If "dot" removes "dog" from the dict, "log" won't see it!
             * Removing layer-by-layer perfectly solves this.
             */
            dict.removeAll(currentLayer);

            Set<String> nextLayer = new HashSet<>();

            // For every word currently in our BFS frontier...
            for (String currentWord : currentLayer) {
                
                // Try changing every single character, one at a time
                char[] chars = currentWord.toCharArray();
                
                for (int i = 0; i < chars.length; i++) {
                    char originalChar = chars[i];
                    
                    // Replace with 'a' through 'z'
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == originalChar) continue;
                        
                        chars[i] = c;
                        String neighbor = new String(chars);
                        
                        /*
                         * --------------------------------------------------------
                         * VALID NEIGHBOR FOUND
                         * --------------------------------------------------------
                         * If this transformed word exists in the remaining dictionary,
                         * it represents a valid step FORWARD.
                         */
                        if (dict.contains(neighbor)) {
                            
                            // Did we reach the target?
                            if (neighbor.equals(endWord)) {
                                reachedEndWord = true;
                            }
                            
                            // Add to the next layer to be explored in the next while-loop iteration
                            nextLayer.add(neighbor);
                            
                            // Add this DIRECTED EDGE to our graph
                            // computeIfAbsent is a clean Java 8 way to initialize a list if it doesn't exist
                            graph.computeIfAbsent(currentWord, k -> new ArrayList<>()).add(neighbor);
                        }
                    }
                    // Backtrack the character change to test the next position
                    chars[i] = originalChar;
                }
            }
            
            // Move forward! The next layer becomes the current layer.
            currentLayer = nextLayer;
        }
        
        return reachedEndWord;
    }

    private void backtrackDFS(String currentWord, String endWord, Map<String, List<String>> graph, 
                              List<String> currentPath, List<List<String>> result) {
        
        /*
         * ============================================================
         * BASE CASE
         * ============================================================
         * We successfully traversed the optimal graph from beginWord 
         * and landed exactly on endWord.
         * 
         * We make a COPY of our current path and add it to the final result.
         */
        if (currentWord.equals(endWord)) {
            result.add(new ArrayList<>(currentPath));
            return;
        }
        
        /*
         * ============================================================
         * DEAD END CHECK
         * ============================================================
         * If the current word doesn't have any outbound edges in our graph, 
         * it's a dead end. We just return.
         */
        if (!graph.containsKey(currentWord)) {
            return;
        }

        /*
         * ============================================================
         * THE DECISION LOOP (CHOOSE, EXPLORE, UNCHOOSE)
         * ============================================================
         * We look at all the pre-calculated, optimal neighbors for this word.
         */
        for (String neighbor : graph.get(currentWord)) {
            
            // CHOOSE: "Let's try walking down this path."
            currentPath.add(neighbor);
            
            // EXPLORE: Recursively walk towards the endWord
            backtrackDFS(neighbor, endWord, graph, currentPath, result);
            
            // UNCHOOSE / BACKTRACK: "We finished exploring that neighbor. 
            // Remove it from our path so we can try the NEXT neighbor."
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        WordLadderII solver = new WordLadderII();

        // Test Case 1: Standard case with multiple paths
        String beginWord1 = "hit";
        String endWord1 = "cog";
        List<String> wordList1 = List.of("hot", "dot", "dog", "lot", "log", "cog");
        
        System.out.println("--- Test Case 1 ---");
        System.out.println("Begin: " + beginWord1 + " | End: " + endWord1);
        List<List<String>> res1 = solver.findLadders(beginWord1, endWord1, wordList1);
        for (List<String> path : res1) {
            System.out.println(path);
        }
        // Expected:
        // [hit, hot, dot, dog, cog]
        // [hit, hot, lot, log, cog]
        System.out.println();

        // Test Case 2: endWord not in dictionary
        String beginWord2 = "hit";
        String endWord2 = "cog";
        List<String> wordList2 = List.of("hot", "dot", "dog", "lot", "log");
        
        System.out.println("--- Test Case 2 ---");
        System.out.println("Begin: " + beginWord2 + " | End: " + endWord2);
        List<List<String>> res2 = solver.findLadders(beginWord2, endWord2, wordList2);
        System.out.println("Valid Paths: " + res2.size()); 
        // Expected: 0
    }
}

/**
 * ============================================================
 * 🔥 WORD LADDER II (ALL SHORTEST PATHS)
 * ============================================================
 *
 * Key Idea:
 * 1. BFS → Find shortest distance & build parent graph
 * 2. DFS → Reconstruct all paths using parent map
 *
 * WHY BFS + DFS?
 * - BFS ensures shortest path
 * - DFS helps reconstruct all paths
 *
 * Time Complexity:
 *   BFS: O(N * L * 26)
 *   DFS: O(P * L)
 *   where:
 *     N = number of words
 *     L = word length
 *     P = number of shortest paths
 *
 * Space Complexity:
 *   O(N * L) for graph + recursion
 *
 * ============================================================
 */

public class WordLadderII {

    public static void main(String[] args) {
        String beginWord = "hit";
        String endWord = "cog";

        List<String> wordList = List.of("hot", "dot", "dog", "lot", "log", "cog");

        System.out.println(findLadders(beginWord, endWord, wordList));
    }

    public static List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {

        Set<String> dict = new HashSet<>(wordList);

        // If endWord is not in dictionary → impossible
        if (!dict.contains(endWord)) return List.of();

        // 🔥 Parent graph (child -> list of parents)
        Map<String, List<String>> parentMap = new HashMap<>();

        // BFS queue
        Queue<String> queue = new ArrayDeque<>();
        queue.offer(beginWord);

        // Visited words (global)
        Set<String> visited = new HashSet<>();
        visited.add(beginWord);

        boolean found = false; // stop BFS when endWord found

        // ============================================================
        // 🟢 BFS PHASE
        // ============================================================
        while (!queue.isEmpty() && !found) {

            int size = queue.size();

            // Track words visited in this level only
            Set<String> levelVisited = new HashSet<>();

            for (int i = 0; i < size; i++) {
                String word = queue.poll();

                char[] arr = word.toCharArray();

                // Try all 26 possibilities
                for (int j = 0; j < arr.length; j++) {
                    char original = arr[j];

                    for (char c = 'a'; c <= 'z'; c++) {
                        arr[j] = c;
                        String next = new String(arr);

                        // If valid transformation
                        if (dict.contains(next) && !visited.contains(next)) {

                            // Mark for this level
                            if (!levelVisited.contains(next)) {
                                queue.offer(next);
                                levelVisited.add(next);
                            }

                            // Build parent mapping (reverse graph)
                            parentMap.computeIfAbsent(next, k -> new ArrayList<>())
                                     .add(word);

                            // If reached endWord → mark found
                            if (next.equals(endWord)) {
                                found = true;
                            }
                        }
                    }

                    arr[j] = original; // restore
                }
            }

            // Add this level's visited to global visited
            visited.addAll(levelVisited);
        }

        // ============================================================
        // 🔴 DFS BACKTRACKING PHASE
        // ============================================================
        List<List<String>> result = new ArrayList<>();

        if (!found) return result;

        List<String> path = new ArrayList<>();
        path.add(endWord);

        // Start DFS from endWord → beginWord
        dfs(endWord, beginWord, parentMap, path, result);

        return result;
    }

    /**
     * DFS to reconstruct all paths
     */
    private static void dfs(String word,
                            String beginWord,
                            Map<String, List<String>> parentMap,
                            List<String> path,
                            List<List<String>> result) {

        // Base case: reached beginWord
        if (word.equals(beginWord)) {
            List<String> validPath = new ArrayList<>(path);
            Collections.reverse(validPath); // reverse path
            result.add(validPath);
            return;
        }

        // If no parent → dead end
        if (!parentMap.containsKey(word)) return;

        for (String parent : parentMap.get(word)) {
            path.add(parent);
            dfs(parent, beginWord, parentMap, path, result);
            path.remove(path.size() - 1); // backtrack
        }
    }
}

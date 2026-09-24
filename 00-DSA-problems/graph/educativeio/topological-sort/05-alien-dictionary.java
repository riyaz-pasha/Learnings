import java.util.*;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * ALIEN DICTIONARY (LeetCode 269)
 * ============================================================================
 * 
 * STATEMENT:
 * You are given a list of words written in an alien language, where the words 
 * are sorted lexicographically by the rules of this language. Surprisingly, 
 * the aliens also use English lowercase letters, but possibly in a different order.
 * 
 * Given a list of words written in the alien language, return a string of unique 
 * letters sorted in the lexicographical order of the alien language as derived 
 * from the list of words.
 * 
 * If there is no solution (i.e., no valid lexicographical ordering), return an 
 * empty string "". If multiple valid orderings exist, you may return any of them.
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We are provided a dictionary of words that are already sorted, but the 
 * underlying alphabet's order is completely unknown. By looking at how adjacent 
 * words are sorted, we can deduce character-to-character relationships. 
 * For example, if "wrt" is placed before "wrf", the first difference happens at 
 * the 3rd letter. This tells us definitively that 't' comes before 'f' in this 
 * alien alphabet.
 * Our goal is to collect all these relative ordering rules and piece them together 
 * to find a complete alphabetical order. If any of these rules contradict each 
 * other (like a circular dependency: A comes before B, and B comes before A), 
 * a valid alphabet is impossible. 
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: What if there are multiple characters with no direct dependencies? 
 *    A: They can be placed anywhere in the final string as long as existing 
 *       rules are not broken. Returning any valid permutation is acceptable.
 * 2. Q: How do we handle situations where a longer word is followed by its exact 
 *       prefix? (e.g., ["abc", "ab"])
 *    A: This fundamentally breaks lexicographical rules. In this case, we 
 *       should instantly return "".
 * 3. Q: What if the dictionary contains duplicate words?
 *    A: Duplicate words do not provide any new ordering information. They 
 *       can just be skipped during processing.
 * 4. Q: Will there be any characters outside of lowercase English letters?
 *    A: No, the problem specifies only English lowercase letters are used.
 * 5. Q: What if a character appears in the words but has no relationships?
 *    A: It must still be included in the final output string, just its relative 
 *       placement won't matter compared to the constrained characters.
 * 6. Q: What should be returned if the input array is empty?
 *    A: Though constraints say words.length >= 1, the standard practice is to 
 *       return "" for an empty list.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Acknowledge the core concept: "This is a dependency resolution problem. 
 *    We need to map relationships between entities to find an order, which 
 *    means we need a Directed Graph and Topological Sort."
 * 2. Define the Graph Components out loud:
 *    - Nodes: Every unique character present in the dictionary.
 *    - Directed Edges: From character A to character B if A must precede B.
 * 3. Walk through the Graph Construction phase:
 *    - Explain that we only need to compare *adjacent* words to gather all 
 *      necessary rules. Find the first mismatched character to create a directed edge.
 *    - Explicitly state that you will check for the prefix edge-case to avoid bugs.
 * 4. Choose your sorting algorithm:
 *    - Offer either Kahn's Algorithm (BFS using in-degrees) or a Depth First 
 *      Search (DFS with a 3-color state system for cycle detection).
 *    - Kahn's algorithm is typically favored here as it's easier to verify 
 *      missing nodes at the end by just comparing string length to graph size.
 * 5. Time & Space Complexity: 
 *    - Time: O(C), where C is the total length of all words combined.
 *    - Space: O(1) or O(U + E) where U is unique characters (max 26) and E is 
 *      edges (max 26^2). Space is strictly bounded to a constant.
 * 
 * ============================================================================
 */
public class AlienDictionary {

    /**
     * ========================================================================
     * SOLUTION 1: KAHN'S ALGORITHM (Breadth-First Search / In-Degree)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * In-degree represents the number of letters that MUST come before a specific 
     * letter. If a letter has an in-degree of 0, it means nothing is forcing it 
     * to be later in the alphabet, so we can append it to our answer immediately.
     * When we append it, we "remove" its constraint on subsequent letters by 
     * reducing their in-degrees. If any of those drop to 0, they become available.
     * 
     * VISUAL TRACING & BREAKDOWN:
     * words = ["wrt", "wrf", "er", "ett", "rftt"]
     * 
     * 1. Extract Unique Characters: w, r, t, f, e
     * 
     * 2. Compare Adjacent Words to build Graph:
     *    wrt vs wrf -> mismatch at 't' & 'f' -> t before f (t -> f)
     *    wrf vs er  -> mismatch at 'w' & 'e' -> w before e (w -> e)
     *    er  vs ett -> mismatch at 'r' & 't' -> r before t (r -> t)
     *    ett vs rftt-> mismatch at 'e' & 'r' -> e before r (e -> r)
     * 
     * 3. Resulting Graph:
     *    w -> e -> r -> t -> f
     * 
     * 4. In-Degrees:
     *    w: 0, e: 1, r: 1, t: 1, f: 1
     * 
     * 5. Execution:
     *    - Queue: [w]. Pop w. Result: "w"
     *    - Reduce e's in-degree to 0. Queue: [e]. Pop e. Result: "we"
     *    - Reduce r's in-degree to 0. Queue: [r]. Pop r. Result: "wer"
     *    - Reduce t's in-degree to 0. Queue: [t]. Pop t. Result: "wert"
     *    - Reduce f's in-degree to 0. Queue: [f]. Pop f. Result: "wertf"
     * 
     * Output: "wertf"
     */
    public String alienOrderBFS(String[] words) {
        // Map to hold adjacency list (using Set to prevent duplicate edges)
        Map<Character, Set<Character>> adj = new HashMap<>();
        // Map to hold in-degrees
        Map<Character, Integer> inDegree = new HashMap<>();
        
        // 1. Initialize data structures for ALL unique characters
        for (String word : words) {
            for (char c : word.toCharArray()) {
                adj.putIfAbsent(c, new HashSet<>());
                inDegree.putIfAbsent(c, 0);
            }
        }
        
        // 2. Build the Graph
        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];
            
            // EDGE CASE: Prefix rule violation (e.g., "abc" before "ab")
            if (word1.length() > word2.length() && word1.startsWith(word2)) {
                return "";
            }
            
            int len = Math.min(word1.length(), word2.length());
            for (int j = 0; j < len; j++) {
                char out = word1.charAt(j);
                char in = word2.charAt(j);
                
                // Find the first mismatched character
                if (out != in) {
                    // Only add edge and increment in-degree if it's a NEW edge
                    if (!adj.get(out).contains(in)) {
                        adj.get(out).add(in);
                        inDegree.put(in, inDegree.get(in) + 1);
                    }
                    break; // Stop comparing current word pair after first mismatch
                }
            }
        }
        
        // 3. Perform BFS (Topological Sort)
        Queue<Character> queue = new LinkedList<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        StringBuilder result = new StringBuilder();
        while (!queue.isEmpty()) {
            char current = queue.poll();
            result.append(current);
            
            for (char neighbor : adj.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        // 4. Validate output length against unique characters (Cycle Detection)
        // If they don't match, it means some characters were stuck in a cycle.
        if (result.length() != inDegree.size()) {
            return "";
        }
        
        return result.toString();
    }

    /**
     * ========================================================================
     * SOLUTION 2: DEPTH-FIRST SEARCH (Graph Coloring / Cycle Detection)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * In DFS, we try to go as deep into the dependency chain as possible. 
     * When we hit a character that has NO outgoing edges (nothing must come 
     * after it), we know it belongs at the very END of the alphabet. 
     * By adding characters to our result list only after completely finishing 
     * their DFS traversal, we naturally build the alphabet in reverse order.
     * We just reverse the string at the end.
     * 
     * CYCLE DETECTION:
     * We use a `visiting` map. 
     * - Key not present = Unvisited
     * - Key is TRUE = Currently visiting (in current recursive path)
     * - Key is FALSE = Fully visited and safely added to the result
     * Hitting a node that is marked as TRUE means we looped back on ourselves!
     */
    public String alienOrderDFS(String[] words) {
        Map<Character, Set<Character>> adj = new HashMap<>();
        
        // 1. Initialize for all unique characters
        for (String word : words) {
            for (char c : word.toCharArray()) {
                adj.putIfAbsent(c, new HashSet<>());
            }
        }
        
        // 2. Build the graph
        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];
            
            if (word1.length() > word2.length() && word1.startsWith(word2)) {
                return ""; // Prefix rule violation
            }
            
            int len = Math.min(word1.length(), word2.length());
            for (int j = 0; j < len; j++) {
                char out = word1.charAt(j);
                char in = word2.charAt(j);
                if (out != in) {
                    adj.get(out).add(in);
                    break; 
                }
            }
        }
        
        // 3. DFS Traversal for Topological Sort
        Map<Character, Boolean> visiting = new HashMap<>(); 
        StringBuilder result = new StringBuilder();
        
        // Iterate through all unique nodes
        for (char c : adj.keySet()) {
            // If hasCycle returns true, an impossible alphabet order was found
            if (hasCycle(c, adj, visiting, result)) {
                return ""; 
            }
        }
        
        // Reverse because DFS gives us post-order
        return result.reverse().toString();
    }
    
    private boolean hasCycle(char current, Map<Character, Set<Character>> adj, 
                             Map<Character, Boolean> visiting, StringBuilder result) {
        
        if (visiting.containsKey(current)) {
            // If true, it's currently in the stack = CYCLE!
            // If false, it's already safely processed.
            return visiting.get(current); 
        }
        
        // Mark node as visiting
        visiting.put(current, true);
        
        // Traverse all characters that must come after this one
        for (char next : adj.get(current)) {
            if (hasCycle(next, adj, visiting, result)) {
                return true;
            }
        }
        
        // Mark node as fully processed (safe)
        visiting.put(current, false);
        // Add to result string at the end of its DFS path
        result.append(current);
        
        return false;
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING (Modern Java Records & Streams)
     * ========================================================================
     */
    
    record TestCase(String[] words, boolean expectValid, String description) {}

    public static void main(String[] args) {
        AlienDictionary solver = new AlienDictionary();

        var testCases = List.of(
            new TestCase(
                new String[]{"wrt","wrf","er","ett","rftt"}, 
                true, 
                "Standard sorted words"
            ),
            new TestCase(
                new String[]{"z","x"}, 
                true, 
                "Two single-letter words"
            ),
            new TestCase(
                new String[]{"z","x","z"}, 
                false, 
                "Cycle detection (z -> x -> z)"
            ),
            new TestCase(
                new String[]{"abc","ab"}, 
                false, 
                "Prefix rule violation (Longer word first)"
            ),
            new TestCase(
                new String[]{"a","b","a"}, 
                false, 
                "Cycle detection with multiple letters"
            )
        );

        System.out.println("Running test cases for Alien Dictionary...");
        System.out.println("-".repeat(70));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            
            String resBFS = solver.alienOrderBFS(tc.words());
            String resDFS = solver.alienOrderDFS(tc.words());
            
            boolean bfsPassed = tc.expectValid() ? !resBFS.isEmpty() : resBFS.isEmpty();
            boolean dfsPassed = tc.expectValid() ? !resDFS.isEmpty() : resDFS.isEmpty();
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("Words: %s\n", Arrays.toString(tc.words()));
            System.out.printf("BFS Result: \"%s\" | Passed? %s\n", resBFS, bfsPassed ? "✅" : "❌");
            System.out.printf("DFS Result: \"%s\" | Passed? %s\n", resDFS, dfsPassed ? "✅" : "❌");
            System.out.println("-".repeat(70));
        });
    }
}

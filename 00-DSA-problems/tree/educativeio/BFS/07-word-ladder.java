/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given two words, `src` and `dest`, and a list of `words`, return the number of 
 * words in the shortest transformation sequence from `src` to `dest`.
 * - Every pair of consecutive words differs by a single character.
 * - All words in the sequence (except possibly `src`) must be in `words`.
 * - If no sequence exists, return 0.
 * 
 * Constraints:
 * - 1 <= src.length <= 10
 * - src.length == dest.length == words[i].length
 * - src != dest
 * - 1 <= words.length <= 5000
 * - No duplicates in words, lowercase English letters only.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Does the required length include both the `src` and `dest` words?"
 *   Yes, standard Word Ladder defines the length as the number of nodes in the path, not edges.
 * - "Is `dest` guaranteed to be in the `words` list?"
 *   If it's not, we can instantly return 0 and skip all processing. (Massive time-saver).
 * - "Can we modify the `words` list or do we need to treat it as read-only?"
 *   If we can mutate a HashSet built from it (by removing words as we visit them), we save space on a separate `visited` set.
 * - "What if the dictionary is small but the words are 1,000 characters long?"
 *   This fundamentally changes how we find neighbors (iterating the dictionary vs mutating the string).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We need the *shortest* path in a graph where nodes are words and edges connect words differing 
 * by 1 character. The graph's edges are not explicitly given; we have to discover them on the fly.
 * 
 * --- APPROACH 1: Depth-First Search (DFS) (The "Brute Force" Trap) ---
 * 1. What I'd naturally try: Start at `src`. Change one letter. If it's in `words`, recursively 
 *    dive down that path until I hit `dest`. Backtrack to find shorter paths.
 * 2. Why it works: It will eventually stumble across the path.
 * 3. Why it's too slow: DFS dives deep. It might find a valid 4,000-word transformation before 
 *    it checks the 2-word transformation right next to it. We must exhaust ALL paths to guarantee 
 *    the minimum, leading to massive redundant exploration.
 * 4. What work is repeated: We visit the same words through astronomically many different convoluted paths.
 * 5. Time Complexity: O(V!) theoretically — practically guaranteed Time Limit Exceeded.
 * 6. Space Complexity: O(V) — for the recursion stack and visited set.
 * 
 * --- APPROACH 2: Standard BFS + Dictionary Iteration ---
 * 1. What I'd try next: Breadth-First Search. This guarantees the first time we hit `dest`, it is 
 *    the absolute shortest path. For each popped word, compare it against all 5,000 words in the 
 *    dictionary. If they differ by 1 char, enqueue it.
 * 2. Why it works: BFS expands layer by layer.
 * 3. Why it's sub-optimal: Comparing a word against 5,000 other words takes O(N * L) where N=5000, 
 *    L=10. We do this for every node we pop. 
 * 4. Time Complexity: O(V * N * L) = O(N^2 * L). With N=5000, N^2 is 25,000,000 operations.
 * 5. Space Complexity: O(N) — for the BFS Queue.
 * 
 * --- APPROACH 3: Standard BFS + Character Substitution (The Standard Optimal) ---
 * 1. The Pivot: Instead of scanning the 5000-word dictionary, look at the word length (L <= 10). 
 *    A 10-letter word has exactly 10 * 26 = 260 possible 1-letter mutations. 
 *    260 string generations << 5000 dictionary comparisons.
 * 2. How it works: Pop a word. For each of its 10 characters, swap it with 'a'-'z'. Check if that 
 *    mutated string exists in a HashSet of our dictionary (O(1) lookup).
 * 3. Time Complexity: O(N * L^2) — In the worst case we visit all N words. For each, we generate 
 *    26*L strings. Constructing each string takes O(L) time. 5000 * 10 * 10 = 500,000 operations (50x faster).
 * 4. Space Complexity: O(N * L) — Storing N words of length L in a HashSet and Queue.
 * 
 * [The Core Observation for the Ultimate Optimal]
 * Approach 3 is fast, but BFS branches out exponentially. If branching factor is B and depth is D, 
 * we explore B^D nodes. But if we know the exact destination, we can search from `src` forward AND 
 * `dest` backward simultaneously.
 * 
 * --- APPROACH 4: Bidirectional BFS (The Senior Solution) ---
 * 1. How it works: Maintain two active sets of words (`beginSet` and `endSet`). Always expand the 
 *    *smaller* set. If a generated word is found in the opposite set, the two expanding ripples 
 *    have touched, and we return the total depth!
 * 2. Time Complexity: O(N * L^2) worst case, but practically O(B^(D/2)). It drastically reduces the 
 *    exponential explosion by meeting in the middle.
 * 3. Space Complexity: O(N * L) — to store the HashSets.
 * 
 * [Which one I'd write in an interview]
 * Approach 4 (Bidirectional BFS). It proves you understand not just how to implement standard BFS, 
 * but how to optimize search spaces aggressively. It is the gold standard for "Word Ladder".
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Target `dest` is not in the dictionary: Return 0 immediately.
 * - Start word `src` is 1 step away from `dest`: Handled naturally, returns 2.
 * - No valid path exists: Search spaces exhaust without intersecting, returns 0.
 * - Dictionary contains `src`: We must remove it from the unvisited set to prevent cycles back to start.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * Removing a word from the dictionary `Set` the moment you enqueue/generate it acts as your `visited` logic. 
 * You don't need a separate `Set<String> visited`. If it's no longer in the dictionary, it's either 
 * invalid or already processed.
 * 
 * [Dry Run - Bidirectional BFS]
 * src: "hit", dest: "cog", dict: ["hot","dot","dog","lot","log","cog"]
 * 
 * Init: 
 *   beginSet = {"hit"}, endSet = {"cog"}
 *   dict = {"hot","dot","dog","lot","log"} (removed "cog" and "hit")
 *   depth = 1
 * 
 * Iteration 1:
 *   beginSet (size 1) vs endSet (size 1). Expand beginSet.
 *   Mutate "hit" -> finds "hot" in dict.
 *   nextBeginSet = {"hot"}. dict removes "hot".
 *   depth = 2.
 * 
 * Iteration 2:
 *   beginSet={"hot"} (size 1) vs endSet={"cog"} (size 1). Expand endSet (tie doesn't matter).
 *   Mutate "cog" -> finds "dog", "log" in dict.
 *   nextEndSet = {"dog", "log"}. dict removes "dog", "log".
 *   depth = 3.
 * 
 * Iteration 3:
 *   beginSet={"hot"} (size 1) vs endSet={"dog","log"} (size 2). 
 *   OPTIMIZATION KICKS IN: Expand beginSet because it's smaller!
 *   Mutate "hot" -> finds "dot", "lot" in dict.
 *   nextBeginSet = {"dot", "lot"}. dict removes "dot", "lot".
 *   depth = 4.
 * 
 * Iteration 4:
 *   beginSet={"dot","lot"} (size 2) vs endSet={"dog","log"} (size 2).
 *   Mutate "dot" -> "dog"! "dog" IS IN `endSet`. 
 *   INTERSECTION FOUND! Return depth + 1 = 5.
 * 
 * [Pitfalls]
 * - Creating a new String by using `substring` repeatedly in a loop is slow. 
 *   Converting the word to a `char[]`, modifying the array, and doing `new String(charArray)` is much faster.
 * - Forgetting to increment the depth/step count before jumping to the next BFS level.
 * 
 * [Pattern Recognition]
 * When you see: "Shortest transformation sequence", "Unweighted shortest path", "Known start and target".
 * Think: Bidirectional Breadth-First Search (Bi-BFS).
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: How would you print all actual shortest paths (Word Ladder II)?
 * A: Standard/Bi-BFS gives the length. To get paths, we must build an adjacency list (graph) 
 *    during the BFS, mapping `child -> [parents]`. Once `dest` is found, we run a DFS backtracking 
 *    from `dest` to `src` using that map to construct the lists.
 * 
 * Q: What if the word length L was 1,000, but N was only 50?
 * A: Character substitution (L * 26) = 26,000 checks. Dictionary iteration (N * L) = 50 * 1000 = 50,000 
 *    checks. They are closer, but if L was 10,000, substitution becomes 260,000 while iteration 
 *    is 500,000. Actually, if N is very small, a custom comparison `isOneEditAway(w1, w2)` might 
 *    be faster than allocating thousands of massive strings.
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedList;
import java.util.Queue;

public class WordLadder {

    /**
     * APPROACH 4: Bidirectional BFS (The Optimal Way)
     */
    public static int ladderLengthOptimal(String src, String dest, List<String> words) {
        // Load words into a HashSet for O(1) lookups.
        Set<String> wordDict = new HashSet<>(words);
        
        // Edge case: If destination isn't in the dictionary, it's impossible.
        if (!wordDict.contains(dest)) {
            return 0;
        }

        // We use Sets instead of Queues because we need fast O(1) lookups to check 
        // if the two search frontiers have intersected.
        Set<String> beginSet = new HashSet<>();
        Set<String> endSet = new HashSet<>();
        
        beginSet.add(src);
        endSet.add(dest);
        
        // Remove start and end from dictionary so we don't revisit them
        wordDict.remove(src);
        wordDict.remove(dest);

        // Sequence includes the starting word, so length starts at 1
        int steps = 1;

        while (!beginSet.isEmpty() && !endSet.isEmpty()) {
            // OPTIMIZATION: Always expand the smaller of the two frontiers.
            // This tightly bounds the exponential branching factor.
            if (beginSet.size() > endSet.size()) {
                Set<String> temp = beginSet;
                beginSet = endSet;
                endSet = temp;
            }

            // 'nextSet' will hold the next layer of words for the chosen frontier
            Set<String> nextSet = new HashSet<>();

            for (String word : beginSet) {
                char[] chars = word.toCharArray();
                
                // Character substitution: Try changing every character to 'a'-'z'
                for (int i = 0; i < chars.length; i++) {
                    char originalChar = chars[i];
                    
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == originalChar) continue;
                        
                        chars[i] = c;
                        String mutatedWord = new String(chars);

                        // Did the ripples touch? If the mutated word is in the opposite set,
                        // we've bridged the gap!
                        if (endSet.contains(mutatedWord)) {
                            return steps + 1;
                        }

                        // If it's a valid intermediate dictionary word, prep it for next level
                        if (wordDict.contains(mutatedWord)) {
                            nextSet.add(mutatedWord);
                            // Removing acts as marking it "visited"
                            wordDict.remove(mutatedWord);
                        }
                    }
                    // Backtrack the character change for the next loop iteration
                    chars[i] = originalChar;
                }
            }
            
            // Move our frontier forward
            beginSet = nextSet;
            steps++;
        }

        // Search frontiers exhausted without touching
        return 0;
    }

    /**
     * APPROACH 3: Standard BFS (For conceptual comparison)
     */
    public static int ladderLengthStandardBFS(String src, String dest, List<String> words) {
        Set<String> wordDict = new HashSet<>(words);
        if (!wordDict.contains(dest)) return 0;
        
        Queue<String> queue = new LinkedList<>();
        queue.offer(src);
        wordDict.remove(src); // act as visited
        
        int steps = 1;
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            
            // Process the current BFS level
            for (int k = 0; k < levelSize; k++) {
                String current = queue.poll();
                
                if (current.equals(dest)) {
                    return steps;
                }
                
                char[] chars = current.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    char original = chars[i];
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == original) continue;
                        chars[i] = c;
                        String nextWord = new String(chars);
                        
                        if (wordDict.contains(nextWord)) {
                            queue.offer(nextWord);
                            wordDict.remove(nextWord); // mark visited
                        }
                    }
                    chars[i] = original;
                }
            }
            steps++;
        }
        return 0;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test 1: Standard happy path
        String src1 = "hit";
        String dest1 = "cog";
        List<String> words1 = Arrays.asList("hot", "dot", "dog", "lot", "log", "cog");
        System.out.println("Test 1 (Bi-BFS):  " + ladderLengthOptimal(src1, dest1, words1)); // Exp: 5
        
        // Test 2: Destination not in dictionary
        String src2 = "hit";
        String dest2 = "cog";
        List<String> words2 = Arrays.asList("hot", "dot", "dog", "lot", "log");
        System.out.println("Test 2 (Missing): " + ladderLengthOptimal(src2, dest2, words2)); // Exp: 0

        // Test 3: No valid path exists (Disconnected graph)
        String src3 = "hot";
        String dest3 = "dog";
        List<String> words3 = Arrays.asList("hot", "dog");
        System.out.println("Test 3 (No path): " + ladderLengthOptimal(src3, dest3, words3)); // Exp: 0

        // Test 4: One step away
        String src4 = "a";
        String dest4 = "c";
        List<String> words4 = Arrays.asList("a", "b", "c");
        System.out.println("Test 4 (1 step):  " + ladderLengthOptimal(src4, dest4, words4)); // Exp: 2
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Shortest Path in Unweighted Graph = Breadth-First Search.
 * - Key observation: Instead of scanning the N-length dictionary to find neighbors, 
 *   mutate the L-length string 26*L times and check existence in O(1).
 * - Most common trap: Forgetting to reset the mutated character (`chars[i] = originalChar`) 
 *   inside the nested loops, causing cumulative string corruption.
 * - Mental trigger: "Shortest transformation" + "Known Start & End" -> Bidirectional BFS.
 */

import java.util.*;

/**
 * Word Ladder using Bi-Directional BFS (Most Optimal Approach)
 *
 * ================================
 * 🧠 CORE IDEA (HOW TO THINK)
 * ================================
 *
 * This problem is asking:
 * "Find the shortest transformation sequence"
 *
 * 👉 That directly maps to:
 *    SHORTEST PATH in an UNWEIGHTED GRAPH → BFS
 *
 * Each word = node
 * Edge exists if 2 words differ by exactly 1 character
 *
 *
 * ================================
 * 🚀 WHY BI-DIRECTIONAL BFS?
 * ================================
 *
 * Normal BFS:
 * -----------
 * Start from src and expand level by level until dest is found.
 *
 * If:
 *   b = branching factor (neighbors per node)
 *   d = shortest path length
 *
 * Then BFS explores:
 *   O(b^d) nodes
 *
 * WHY?
 * Because tree grows exponentially:
 *   Level 0 → 1
 *   Level 1 → b
 *   Level 2 → b^2
 *   ...
 *   Level d → b^d
 *
 * 👉 Most cost is in LAST levels (very expensive)
 *
 *
 * Bi-Directional BFS:
 * -------------------
 * Instead of searching from ONE side:
 *
 *   src → → → → → → dest
 *
 * We search from BOTH sides:
 *
 *   src → → →   ← ← ← dest
 *            meet in middle
 *
 * Each side explores only HALF depth (d/2):
 *
 *   Time = O(b^(d/2)) + O(b^(d/2))
 *        ≈ O(b^(d/2))
 *
 * 🔥 EXPONENTIAL SAVING:
 * Example:
 *   b = 10, d = 6
 *
 *   Normal BFS  → 10^6 = 1,000,000 nodes
 *   Bi-BFS      → 2 * 10^3 = 2,000 nodes
 *
 * 👉 ~500x faster
 *
 *
 * ================================
 * 🎯 KEY OPTIMIZATION TRICK
 * ================================
 *
 * Always expand the SMALLER SET:
 *
 *   if (begin.size() > end.size()) → swap
 *
 * WHY?
 * Because branching happens from that side.
 * Expanding smaller side = fewer nodes generated.
 *
 *
 * ================================
 * ⚙️ ALGORITHM FLOW
 * ================================
 *
 * 1. Convert words list → HashSet (O(1) lookup)
 * 2. Maintain:
 *    - begin set (forward BFS)
 *    - end set   (backward BFS)
 * 3. Expand smaller side
 * 4. Generate neighbors by changing one char
 * 5. If neighbor found in opposite set → DONE
 * 6. Continue level by level
 *
 *
 * ================================
 * ⏱ COMPLEXITY
 * ================================
 *
 * Time:  ~ O(N * L) in practice (much faster than BFS)
 * Space: O(N)
 *
 * Where:
 *   N = number of words
 *   L = word length
 *
 */
public class WordLadderBiDirectional {

    public int ladderLength(String src, String dest, List<String> words) {

        // Convert list to set for O(1) lookup
        Set<String> wordSet = new HashSet<>(words);

        // If destination is not in dictionary → no solution
        if (!wordSet.contains(dest)) return 0;

        // Two frontiers for bi-directional search
        Set<String> begin = new HashSet<>();
        Set<String> end = new HashSet<>();

        begin.add(src);
        end.add(dest);

        // Visited set to avoid cycles
        Set<String> visited = new HashSet<>();

        // Level = number of transformations (including src)
        int level = 1;

        while (!begin.isEmpty() && !end.isEmpty()) {

            /**
             * 🚨 IMPORTANT OPTIMIZATION
             *
             * Always expand the smaller frontier
             *
             * WHY?
             * - Suppose begin has 100 nodes, end has 5 nodes
             * - Expanding begin → generates 100 * 26 * L nodes
             * - Expanding end → generates only 5 * 26 * L nodes
             *
             * 👉 So we always expand smaller side to reduce work
             */
            if (begin.size() > end.size()) {
                Set<String> temp = begin;
                begin = end;
                end = temp;
            }

            // Store next level nodes
            Set<String> nextLevel = new HashSet<>();

            // Expand current frontier
            for (String word : begin) {

                char[] chars = word.toCharArray();

                // Try changing each character
                for (int i = 0; i < chars.length; i++) {

                    char original = chars[i];

                    // Try all 26 possible characters
                    for (char c = 'a'; c <= 'z'; c++) {

                        if (c == original) continue;

                        chars[i] = c;
                        String newWord = new String(chars);

                        /**
                         * 🎯 MEETING POINT CONDITION
                         *
                         * If this word is present in the opposite frontier,
                         * it means:
                         *
                         *   src → ... → newWord ← ... ← dest
                         *
                         * We found connection → shortest path guaranteed
                         */
                        if (end.contains(newWord)) {
                            return level + 1;
                        }

                        /**
                         * If valid word and not visited,
                         * add to next level
                         */
                        if (wordSet.contains(newWord) && !visited.contains(newWord)) {
                            nextLevel.add(newWord);
                            visited.add(newWord);
                        }
                    }

                    // Restore original character
                    chars[i] = original;
                }
            }

            // Move to next level
            begin = nextLevel;

            /**
             * Level increases after exploring one full layer
             *
             * NOTE:
             * level represents number of words in path so far
             */
            level++;
        }

        // No transformation sequence found
        return 0;
    }
}

import java.util.*;

/**
 * ============================================================
 * PROBLEM: WORD LADDER
 * ============================================================
 *
 * Given two words:
 *
 *     src
 *     dest
 *
 * and a list of words:
 *
 *     words
 *
 * return the number of words in the SHORTEST transformation
 * sequence from src to dest.
 *
 *
 * A valid transformation sequence:
 *
 *     src -> word1 -> word2 -> ... -> dest
 *
 * must satisfy:
 *
 * 1. The final word is `dest`.
 *
 * 2. Every consecutive pair differs by EXACTLY ONE character.
 *
 * 3. Every word in the sequence, except `src`, must exist
 *    in the `words` list.
 *
 *
 * The `src` does NOT have to be present in `words`.
 *
 *
 * If no transformation is possible:
 *
 *     return 0
 *
 *
 * ============================================================
 * EXAMPLE
 * ============================================================
 *
 *     src  = "hit"
 *     dest = "cog"
 *
 *     words = [
 *         "hot",
 *         "dot",
 *         "dog",
 *         "lot",
 *         "log",
 *         "cog"
 *     ]
 *
 *
 * One shortest transformation is:
 *
 *     hit -> hot -> dot -> dog -> cog
 *
 *
 * Number of words:
 *
 *     5
 *
 *
 * Therefore:
 *
 *     answer = 5
 *
 *
 * Notice that we count WORDS, not transformations.
 *
 *
 * Number of transitions:
 *
 *     hit -> hot       = 1
 *     hot -> dot       = 2
 *     dot -> dog       = 3
 *     dog -> cog       = 4
 *
 *
 * But number of words is:
 *
 *     hit, hot, dot, dog, cog
 *
 *     = 5
 *
 *
 * ============================================================
 * IMPORTANT DIFFERENCE FROM MANY SHORTEST-PATH PROBLEMS
 * ============================================================
 *
 * In a normal graph problem, the graph might be explicitly
 * given:
 *
 *     A -> B
 *     B -> C
 *     C -> D
 *
 *
 * Here, there is NO explicit graph.
 *
 * We need to construct the graph conceptually.
 *
 *
 * Every word is a NODE.
 *
 * Two words have an EDGE if they differ by exactly ONE
 * character.
 *
 *
 * Example:
 *
 *     hot
 *     dot
 *
 * differ only at the first character:
 *
 *     h o t
 *     d o t
 *     ^
 *     different
 *
 *
 * Therefore:
 *
 *     hot <-> dot
 *
 *
 * Similarly:
 *
 *     dot
 *     dog
 *
 *     d o t
 *     d o g
 *         ^
 *         different
 *
 *
 * Therefore:
 *
 *     dot <-> dog
 *
 *
 * ============================================================
 * GRAPH VISUALIZATION
 * ============================================================
 *
 * For:
 *
 *     hit
 *     hot
 *     dot
 *     dog
 *     cog
 *
 *
 * The graph looks like:
 *
 *
 *     hit
 *      |
 *      |
 *     hot
 *      |
 *      |
 *     dot
 *      |
 *      |
 *     dog
 *      |
 *      |
 *     cog
 *
 *
 * There may be MANY other connections in a larger dictionary.
 *
 *
 * The problem becomes:
 *
 *     Find shortest path from src to dest.
 *
 *
 * Since every transformation has the same cost:
 *
 *     one word transformation = 1 step
 *
 *
 * this is an UNWEIGHTED SHORTEST PATH problem.
 *
 *
 * Therefore:
 *
 *     BFS
 *
 * is the natural algorithm.
 *
 *
 * ============================================================
 * WHY BFS?
 * ============================================================
 *
 * BFS explores the graph level by level.
 *
 *
 * Example:
 *
 * Distance 1:
 *
 *     all words one character away from src
 *
 *
 * Distance 2:
 *
 *     all words two transformations away
 *
 *
 * Distance 3:
 *
 *     all words three transformations away
 *
 *
 * ...
 *
 *
 * Therefore, the FIRST time we reach `dest`, we have found
 * the shortest transformation sequence.
 *
 *
 * ============================================================
 * BUT HOW DO WE FIND NEIGHBORS?
 * ============================================================
 *
 * This is the main challenge.
 *
 *
 * Suppose:
 *
 *     current = "hot"
 *
 *
 * We need to find all words that differ from "hot" by exactly
 * one character.
 *
 *
 * We could compare "hot" with every word in `words`.
 *
 *
 * Example:
 *
 *     hot vs dot
 *     hot vs dog
 *     hot vs lot
 *     hot vs log
 *     hot vs cog
 *     ...
 *
 *
 * If there are 5000 words, this could become expensive.
 *
 *
 * Instead, we generate all POSSIBLE one-character changes.
 *
 *
 * ============================================================
 * GENERATING NEIGHBORS
 * ============================================================
 *
 * Suppose:
 *
 *     current = "hot"
 *
 *
 * Position 0:
 *
 *     _ot
 *
 * Replace the first character with:
 *
 *     aot
 *     bot
 *     cot
 *     dot
 *     eot
 *     ...
 *     zot
 *
 *
 * Position 1:
 *
 *     h_t
 *
 *     hat
 *     hbt
 *     hct
 *     ...
 *     hot
 *     ...
 *
 *
 * Position 2:
 *
 *     ho_
 *
 *     hoa
 *     hob
 *     hoc
 *     ...
 *     hot
 *     ...
 *
 *
 * For each position, try all 26 lowercase letters.
 *
 *
 * Therefore:
 *
 *     word length * 26
 *
 * candidate words are generated.
 *
 *
 * Constraints:
 *
 *     word length <= 10
 *
 * Therefore at most:
 *
 *     10 * 26 = 260
 *
 * candidates per BFS state.
 *
 *
 * ============================================================
 * WHY CHECK AGAINST A HASHSET?
 * ============================================================
 *
 * Most generated combinations are NOT valid words.
 *
 *
 * Example:
 *
 *     hot
 *
 * Generated:
 *
 *     aot
 *     bot
 *     cot
 *     dot
 *     ...
 *
 *
 * We only want candidates that exist in `words`.
 *
 *
 * Therefore:
 *
 *     Set<String> dictionary
 *
 * allows:
 *
 *     dictionary.contains(candidate)
 *
 * in O(1) average time.
 *
 *
 * ============================================================
 * EXAMPLE: GENERATING NEIGHBORS OF "hot"
 * ============================================================
 *
 * Suppose:
 *
 *     words = {
 *         hot,
 *         dot,
 *         lot,
 *         dog
 *     }
 *
 *
 * Current:
 *
 *     hot
 *
 *
 * Change position 0:
 *
 *     hot
 *     ^
 *
 * Possible:
 *
 *     aot
 *     bot
 *     cot
 *     dot  <-- VALID
 *     ...
 *     lot  <-- VALID
 *
 *
 * Change position 1:
 *
 *     h_t
 *
 * Possible:
 *
 *     hat
 *     hbt
 *     ...
 *
 * None may be valid.
 *
 *
 * Change position 2:
 *
 *     ho_
 *
 * Possible:
 *
 *     hoa
 *     hob
 *     ...
 *
 *
 * Only dictionary words are added to BFS.
 *
 *
 * ============================================================
 * BFS WALKTHROUGH
 * ============================================================
 *
 * Example:
 *
 *     src  = "hit"
 *     dest = "cog"
 *
 *     words = [
 *         "hot",
 *         "dot",
 *         "dog",
 *         "lot",
 *         "log",
 *         "cog"
 *     ]
 *
 *
 * Initial:
 *
 *     Queue:
 *
 *         [hit]
 *
 *     Distance:
 *
 *         1
 *
 *
 * Why distance = 1?
 *
 * Because the sequence currently contains:
 *
 *     hit
 *
 * One word.
 *
 *
 * ============================================================
 * LEVEL 1
 * ============================================================
 *
 * Current:
 *
 *     hit
 *
 *
 * Generate all one-character changes.
 *
 *
 * Among them:
 *
 *     hot
 *
 * differs by one character:
 *
 *
 *     h i t
 *     h o t
 *       ^
 *
 *
 * Therefore:
 *
 *     hot
 *
 * is a valid neighbor.
 *
 *
 * Queue:
 *
 *     [hot]
 *
 *
 * Sequence length:
 *
 *     2
 *
 *     hit -> hot
 *
 *
 * ============================================================
 * LEVEL 2
 * ============================================================
 *
 * Current:
 *
 *     hot
 *
 *
 * Valid neighbors include:
 *
 *     dot
 *     lot
 *
 *
 * So:
 *
 *     Queue:
 *
 *     [dot, lot]
 *
 *
 * We now have:
 *
 *
 *             hit
 *              |
 *             hot
 *            /   \
 *          dot   lot
 *
 *
 * Both are two transformations away from `hit`.
 *
 *
 * ============================================================
 * LEVEL 3
 * ============================================================
 *
 * Process:
 *
 *     dot
 *
 *
 * Generate:
 *
 *     dog
 *
 *
 * because:
 *
 *     d o t
 *     d o g
 *         ^
 *
 *
 * Queue:
 *
 *     [lot, dog]
 *
 *
 * We now have:
 *
 *
 *             hit
 *              |
 *             hot
 *            /   \
 *          dot   lot
 *          |
 *         dog
 *
 *
 * ============================================================
 * LEVEL 4
 * ============================================================
 *
 * Process:
 *
 *     dog
 *
 *
 * Generate:
 *
 *     cog
 *
 *
 * because:
 *
 *     d o g
 *     c o g
 *     ^
 *
 *
 * `cog` is the target.
 *
 *
 * Therefore:
 *
 *     hit -> hot -> dot -> dog -> cog
 *
 *
 * Number of words:
 *
 *     5
 *
 *
 * Answer:
 *
 *     5
 *
 *
 * ============================================================
 * WHY DO WE NEED VISITED?
 * ============================================================
 *
 * The graph can contain cycles.
 *
 *
 * Example:
 *
 *     hot <-> dot
 *
 *
 * If we don't track visited:
 *
 *     hot
 *      |
 *     dot
 *      |
 *     hot
 *      |
 *     dot
 *      |
 *     ...
 *
 *
 * BFS could repeatedly process the same words.
 *
 *
 * Therefore we maintain:
 *
 *     Set<String> visited
 *
 *
 * Once a word is discovered:
 *
 *     visited.add(word)
 *
 *
 * We never enqueue it again.
 *
 *
 * ============================================================
 * EVEN BETTER:
 * REMOVE FROM DICTIONARY WHEN VISITED
 * ============================================================
 *
 * We can simplify the implementation.
 *
 * Instead of maintaining two sets:
 *
 *     dictionary
 *     visited
 *
 *
 * we can remove a word from the dictionary as soon as
 * we discover it.
 *
 *
 * Example:
 *
 *     dictionary = {
 *         hot,
 *         dot,
 *         dog,
 *         lot,
 *         log,
 *         cog
 *     }
 *
 *
 * We discover:
 *
 *     hot
 *
 *
 * Remove it:
 *
 *     dictionary.remove("hot")
 *
 *
 * Now it cannot be discovered again.
 *
 *
 * This gives us:
 *
 *     dictionary = unvisited words
 *
 *
 * The source can also be removed from the dictionary if it
 * happens to be present.
 *
 *
 * ============================================================
 * IMPORTANT:
 * WHY DOES `src` NOT NEED TO BE IN THE DICTIONARY?
 * ============================================================
 *
 * The problem explicitly says:
 *
 *     src does not need to be present in words.
 *
 *
 * That's fine.
 *
 * We start BFS with:
 *
 *     src
 *
 *
 * We only require subsequent transformed words to exist
 * in the dictionary.
 *
 *
 * Example:
 *
 *     src = "hit"
 *
 *     words = ["hot", "dot", "dog", "cog"]
 *
 *
 * This is perfectly valid:
 *
 *     hit -> hot -> dot -> dog -> cog
 *
 *
 * even though "hit" isn't in `words`.
 *
 *
 * ============================================================
 * IMPORTANT:
 * WHAT IF DEST IS NOT IN WORDS?
 * ============================================================
 *
 * The target must be part of the transformation sequence.
 *
 * Since every word except `src` must be in `words`,
 * if:
 *
 *     dest not in words
 *
 * then reaching `dest` is impossible.
 *
 *
 * We can immediately return:
 *
 *     0
 *
 *
 * ============================================================
 * STEP-BY-STEP VISUAL
 * ============================================================
 *
 * Let's visualize:
 *
 *
 *             hit
 *              |
 *              | change i -> o
 *              v
 *             hot
 *            /   \
 *           /     \
 *          v       v
 *         dot     lot
 *          |
 *          |
 *          v
 *         dog
 *          |
 *          |
 *          v
 *         cog
 *
 *
 * BFS explores:
 *
 *     Level 1:
 *
 *         hit
 *
 *     Level 2:
 *
 *         hot
 *
 *     Level 3:
 *
 *         dot, lot
 *
 *     Level 4:
 *
 *         dog, log
 *
 *     Level 5:
 *
 *         cog
 *
 *
 * Therefore:
 *
 *     answer = 5
 *
 *
 * ============================================================
 * ANOTHER IMPORTANT QUESTION:
 * WHY NOT BUILD THE ENTIRE GRAPH FIRST?
 * ============================================================
 *
 * We could compare every pair of words:
 *
 *     word1 vs word2
 *     word1 vs word3
 *     ...
 *
 *
 * For N words:
 *
 *     O(N^2)
 *
 * comparisons.
 *
 *
 * With up to 5000 words:
 *
 *     5000 * 5000
 *
 * can become expensive.
 *
 *
 * We don't need to build the entire graph.
 *
 * BFS only needs neighbors of the CURRENT word.
 *
 * So we generate potential neighbors on demand.
 *
 *
 * ============================================================
 * COMPLEXITY
 * ============================================================
 *
 * Let:
 *
 *     N = number of words
 *     L = length of each word
 *
 *
 * For every BFS state:
 *
 *     L positions
 *
 * and for every position:
 *
 *     26 possible characters
 *
 *
 * Therefore:
 *
 *     O(L * 26)
 *
 * candidates are generated per word.
 *
 *
 * At most N words are processed.
 *
 *
 * Therefore:
 *
 *     O(N * L * 26)
 *
 *
 * Since 26 is constant:
 *
 *     O(N * L)
 *
 *
 * With:
 *
 *     N <= 5000
 *     L <= 10
 *
 * this is very manageable.
 *
 *
 * Space:
 *
 *     O(N)
 *
 * for the dictionary and BFS queue.
 *
 *
 * ============================================================
 * CORRECTNESS INTUITION
 * ============================================================
 *
 * Each BFS edge represents exactly ONE character change.
 *
 *
 * Therefore:
 *
 *     BFS level 1 = one transformation
 *     BFS level 2 = two transformations
 *     BFS level 3 = three transformations
 *     ...
 *
 *
 * Since BFS always processes smaller distances before larger
 * distances, the first time we reach `dest`, we have found
 * the minimum number of transformations.
 *
 *
 * Since `dest` itself is included in the count:
 *
 *
 * Example:
 *
 *     hit -> hot -> dot -> dog -> cog
 *
 *
 * transformations:
 *
 *     4
 *
 * words:
 *
 *     5
 *
 *
 * Therefore the BFS answer starts at:
 *
 *     1
 *
 * for `src`, and increments for every level.
 *
 *
 * ============================================================
 * IMPLEMENTATION
 * ============================================================
 */
public class WordLadder {

    /**
     * Returns the number of words in the shortest
     * transformation sequence from src to dest.
     *
     * Returns 0 if no transformation is possible.
     *
     * @param src starting word
     * @param dest target word
     * @param words allowed transformation words
     * @return number of words in shortest sequence, or 0
     */
    public int ladderLength(
            String src,
            String dest,
            List<String> words) {

        /*
         * Convert the word list into a HashSet.
         *
         * This gives us O(1) average lookup:
         *
         *     dictionary.contains(word)
         *
         *
         * We will also REMOVE words once they are discovered.
         * Therefore the set effectively acts as our visited set.
         */
        Set<String> dictionary = new HashSet<>(words);

        /*
         * The destination must exist in the dictionary.
         *
         * Example:
         *
         *     src  = "hit"
         *     dest = "cog"
         *
         * If:
         *
         *     cog
         *
         * is not in words,
         *
         * then no valid sequence can end at cog.
         */
        if (!dictionary.contains(dest)) {
            return 0;
        }

        /*
         * BFS queue.
         *
         * Start from src.
         */
        Queue<String> queue = new ArrayDeque<>();

        queue.offer(src);

        /*
         * `length` represents the NUMBER OF WORDS in the
         * current transformation sequence.
         *
         * Initially:
         *
         *     src
         *
         * is already one word.
         *
         * Therefore:
         *
         *     length = 1
         */
        int length = 1;

        /*
         * src does not necessarily exist in dictionary.
         *
         * If it does exist, remove it because we have already
         * visited it.
         *
         * This prevents returning to src later.
         */
        dictionary.remove(src);

        /*
         * Standard BFS.
         */
        while (!queue.isEmpty()) {

            /*
             * Number of words currently at this BFS level.
             *
             * Every word at this level has the same transformation
             * distance from src.
             */
            int levelSize = queue.size();

            /*
             * Process every word at the current level.
             */
            for (int i = 0; i < levelSize; i++) {

                /*
                 * Take the next word.
                 */
                String current = queue.poll();

                /*
                 * If current is the destination, we have found
                 * the shortest transformation sequence.
                 *
                 * Because BFS processes levels in increasing
                 * order, this is guaranteed to be minimum.
                 */
                if (current.equals(dest)) {
                    return length;
                }

                /*
                 * Convert current word to a character array.
                 *
                 * This allows us to modify one character at
                 * a time.
                 *
                 * Example:
                 *
                 *     "hot"
                 *
                 * becomes:
                 *
                 *     ['h', 'o', 't']
                 */
                char[] chars = current.toCharArray();

                /*
                 * Try changing every character position.
                 *
                 * Example for "hot":
                 *
                 * position 0:
                 *
                 *     _ot
                 *
                 * position 1:
                 *
                 *     h_t
                 *
                 * position 2:
                 *
                 *     ho_
                 */
                for (int position = 0;
                     position < chars.length;
                     position++) {

                    /*
                     * Remember the original character.
                     *
                     * We need to restore it after trying all
                     * 26 possibilities.
                     */
                    char original = chars[position];

                    /*
                     * Try every lowercase English character.
                     *
                     *     'a' -> 'z'
                     */
                    for (char ch = 'a'; ch <= 'z'; ch++) {

                        /*
                         * Don't waste time generating the same
                         * word as the current word.
                         */
                        if (ch == original) {
                            continue;
                        }

                        /*
                         * Change exactly ONE character.
                         *
                         * Example:
                         *
                         * current = "hot"
                         *
                         * position = 0
                         *
                         * ch = 'd'
                         *
                         * candidate:
                         *
                         *     "dot"
                         */
                        chars[position] = ch;

                        /*
                         * Convert the modified character array
                         * back into a String.
                         */
                        String candidate = new String(chars);

                        /*
                         * Check whether this candidate is an
                         * allowed word.
                         *
                         * `remove()` is doing TWO things:
                         *
                         * 1. Check whether candidate exists.
                         * 2. Remove it immediately if it exists.
                         *
                         * HashSet.remove() returns:
                         *
                         *     true  -> candidate existed
                         *     false -> candidate did not exist
                         *
                         *
                         * Removing immediately marks the word
                         * as visited.
                         */
                        if (dictionary.remove(candidate)) {

                            /*
                             * We found a valid next word.
                             *
                             * Add it to BFS queue.
                             */
                            queue.offer(candidate);
                        }
                    }

                    /*
                     * Restore the original character before
                     * moving to the next position.
                     *
                     * Example:
                     *
                     * original:
                     *
                     *     h
                     *
                     * During the loop we tried:
                     *
                     *     a, b, c, ..., z
                     *
                     * Now restore:
                     *
                     *     h
                     */
                    chars[position] = original;
                }
            }

            /*
             * We have processed one complete BFS level.
             *
             * Therefore every word we process from this point
             * onward will contain ONE MORE WORD in its
             * transformation sequence.
             *
             *
             * Example:
             *
             *     Level 1:
             *
             *         hit
             *
             *     length = 1
             *
             *
             *     Level 2:
             *
             *         hot
             *
             *     length = 2
             *
             *
             *     Level 3:
             *
             *         dot, lot
             *
             *     length = 3
             */
            length++;
        }

        /*
         * BFS exhausted all reachable words.
         *
         * We never reached dest.
         *
         * Therefore no valid transformation sequence exists.
         */
        return 0;
    }
}

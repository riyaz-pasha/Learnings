/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: LONGEST WORD IN DICTIONARY
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Can the array contain duplicate words?" 
 *   (If yes, we might want to deduplicate via a Set to avoid redundant Trie insertions.)
 * - "What happens if no valid word exists? For instance, input is ['ap', 'app'] but missing 'a'?"
 *   (Clarifies the base case constraint: a valid word must be buildable from a 1-character prefix. Expect to return "".)
 * - "Are all characters strictly lowercase English letters?"
 *   (Confirms we can use a fixed `Node[26]` array instead of a heavier `HashMap<Character, Node>`.)
 * - "Is the input array already sorted?"
 *   (If it were sorted lexicographically, a completely different single-pass Stack/Set approach becomes optimal.)
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * We need to find the longest string where *every* leading substring (prefix) also exists 
 * in the given array. The binding constraint is doing this without redundantly scanning 
 * prefixes or creating thousands of intermediate substrings for every single word.
 * 
 * APPROACH 1: HashSet + Substring Checking (The Brute Force)
 * 1. What I'd naturally try: Dump all words into a `HashSet<String>`. Then loop through 
 *    each word, generate every prefix (`substring(0, i)`), and check if it's in the Set.
 * 2. Why it works: We exhaustively verify the condition for every candidate word.
 * 3. Why it's costly: Substring creation. For a word of length L, we create L substrings, 
 *    each taking O(L) to copy and hash. 
 * 4. What work is repeated: If we check "apple" and "apply", we independently verify 
 *    "a", "ap", "app" twice, generating those strings from scratch both times.
 * 5. Time Complexity: O(N * L^2) 
 *    — N words, L prefixes per word, each prefix taking O(L) time to create and hash.
 * 6. Space Complexity: O(N * L) 
 *    — For storing N strings in the HashSet.
 * 
 * APPROACH 2: Sort + Dynamic HashSet (The Intermediate Improvement)
 * 1. What I'd naturally try next: To avoid generating L substrings per word, what if we 
 *    build words incrementally? If I sort the array lexicographically, smaller prefixes 
 *    come first.
 * 2. Why it works: I iterate through the sorted words. If a word is length 1, OR its 
 *    prefix of length L-1 is already in my "valid" HashSet, it means the entire chain exists! 
 *    I add it to the valid Set and update my longest word.
 * 3. Why it's costly: Sorting strings is expensive.
 * 4. What property removes the bottleneck: The realization that sorting requires comparing 
 *    full strings against each other (O(N L log N)). We don't actually need the whole 
 *    array sorted—we just need a data structure that naturally links prefixes together.
 * 5. Time Complexity: O(N * L log N) 
 *    — Dominated by sorting N strings of max length L. The HashSet check is now just O(L).
 * 6. Space Complexity: O(N * L) 
 *    — Storing valid words in the HashSet.
 * 
 * APPROACH 3: Prefix Tree / Trie + DFS (The Optimal Solution)
 * 1. The defining observation: The problem literally defines a Prefix Tree. In a Trie, 
 *    a word whose every prefix exists is simply a continuous path from the root where 
 *    *every single node* has `isEndOfWord == true`.
 * 2. Why it works: We insert all words. Then, we run a Depth-First Search (DFS) from the 
 *    root, but we *only* step onto nodes marked as `isEndOfWord`. 
 * 3. Time Complexity: O(Sum of all L) 
 *    — Building the Trie processes each character exactly once. The DFS visits each valid 
 *    node exactly once. O(N * L) worst case, completely avoiding sorting overhead.
 * 4. Space Complexity: O(Sum of all L) 
 *    — For the Trie nodes. The DFS recursion stack goes at most L deep.
 * 
 * [What I'd write in an interview]
 * I would implement the Trie + DFS (Approach 3). It elegantly enforces the lexicographical 
 * tie-breaker implicitly: if we traverse children from 'a' to 'z' and only update our 
 * `longest` result when a path is *strictly* longer, the first one we find of any given 
 * length is guaranteed to be the lexicographically smallest.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - Unbroken chains missing the first link: `["p", "app", "appl"]` (Root to 'a' is missing. Returns "").
 * - Tie-breaking identical lengths: `["a", "b", "c"]` (Must return "a", not "c").
 * - Array with only invalid words: `["ab", "abc"]` (Returns "").
 * - Single valid word: `["a"]` (Returns "a").
 * 
 * 
 * 4. KEY INSIGHTS, EXAMPLES & DRY RUN
 * ----------------------------------------------------------------------------
 * [Key Insight]
 * "The Unbroken Chain Invariant": In standard Trie searches, you just care if the final 
 * node is a word. Here, we enforce that *every* step we take must be a word. If a node 
 * is `isEndOfWord == false`, that path is dead, even if valid words exist deeper down.
 * 
 * [ASCII Diagram: Valid vs Invalid Path]
 * Inserted: "a", "ap", "app", "b", "bat"
 * 
 *         (root)
 *        /      \
 *     'a'*(T)   'b'*(T)
 *      |         |
 *     'p'*(T)   'a' (F) <-- Chain broken! DFS stops here.
 *      |         |
 *     'p'*(T)   't'*(T) <-- Valid word, but unreachable because 'ba' wasn't in array.
 * 
 * [Dry Run of DFS on above Trie]
 * 1. Root -> loop 'a' to 'z'.
 * 2. 'a' exists and isEnd=T. Path="a". (length 1 > 0). Max updated to "a". Recurse!
 * 3. 'a' -> 'p' exists and isEnd=T. Path="ap". (length 2 > 1). Max updated to "ap". Recurse!
 * 4. 'ap' -> 'p' exists and isEnd=T. Path="app". (length 3 > 2). Max updated to "app". Recurse!
 * 5. 'app' has no children. Backtrack to root.
 * 6. Root -> 'b' exists and isEnd=T. Path="b". (length 1 is NOT > 3). Max remains "app". Recurse!
 * 7. 'b' -> 'a' exists, but isEnd=FALSE! Path breaks. DFS stops.
 * 8. Return "app".
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: How would you return ALL words tied for the maximum length instead of just one?
 * A: Instead of a single `String maxWord`, I'd maintain a `List<String> maxWords`. 
 *    During DFS, if `path.length() > maxLength`, I'd clear the list, add the word, 
 *    and update `maxLength`. If `path.length() == maxLength`, I'd just append to the list.
 * 
 * Q: What if the characters included uppercase, numbers, and symbols?
 * A: I would change `TrieNode[] children = new TrieNode[26]` to a 
 *    `HashMap<Character, TrieNode> children`. To maintain the lexicographical 
 *    tie-breaker during DFS, I'd need to sort the keys (or use a `TreeMap`) before 
 *    iterating over a node's children.
 */

public class LongestWordStudyNote {

    // ============================================================================
    // TRIE IMPLEMENTATION
    // ============================================================================
    
    /**
     * The TrieNode stores character paths. 
     * We don't need to explicitly store the character; the index implies it.
     */
    static class TrieNode {
        TrieNode[] children;
        boolean isEndOfWord;
        
        // Optional but helpful: storing the exact word at the end node 
        // saves us from passing a StringBuilder around during DFS.
        String word; 

        public TrieNode() {
            this.children = new TrieNode[26];
            this.isEndOfWord = false;
            this.word = "";
        }
    }

    private TrieNode root;
    private String longestValidWord;

    public String longestWord(String[] words) {
        root = new TrieNode();
        longestValidWord = "";

        // 1. Build the Trie with all words
        // Time: O(Sum of all characters)
        for (String word : words) {
            insert(word);
        }

        // 2. Perform DFS to find the longest unbroken chain of prefixes
        // Time: O(Sum of all characters)
        dfs(root);

        return longestValidWord;
    }

    /**
     * Standard Trie insertion.
     */
    private void insert(String word) {
        TrieNode curr = root;
        for (int i = 0; i < word.length(); i++) {
            int index = word.charAt(i) - 'a';
            
            if (curr.children[index] == null) {
                curr.children[index] = new TrieNode();
            }
            
            curr = curr.children[index];
        }
        
        // Mark the endpoint and store the actual string here for easy retrieval
        curr.isEndOfWord = true;
        curr.word = word;
    }

    /**
     * Depth-First Search to explore valid prefix chains.
     */
    private void dfs(TrieNode node) {
        // Evaluate the current valid node. 
        // We only update if strictly longer. Because we traverse children 'a' to 'z', 
        // the FIRST word of length X we see is guaranteed to be lexicographically smallest.
        if (node.word.length() > longestValidWord.length()) {
            longestValidWord = node.word;
        }

        // Branch out to all 26 possible children (alphabetical order)
        for (int i = 0; i < 26; i++) {
            TrieNode child = node.children[i];
            
            // THE PITFALL AVOIDED:
            // We only continue the DFS if the child exists AND it forms a valid word.
            // If `isEndOfWord` is false, it means the prefix chain is broken, 
            // so we completely ignore any potential words deeper down this branch.
            if (child != null && child.isEndOfWord) {
                dfs(child);
            }
        }
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        LongestWordStudyNote solution = new LongestWordStudyNote();
        
        System.out.println("--- Standard Case ---");
        String[] words1 = {"w","wo","wor","worl","world"};
        // Expected: "world"
        System.out.println("Result: " + solution.longestWord(words1));

        System.out.println("\n--- Tie-Breaker Case ---");
        String[] words2 = {"a", "banana", "app", "appl", "ap", "apply", "apple"};
        // Both "apply" and "apple" are length 5.
        // 'e' comes before 'y', so "apple" should win.
        System.out.println("Result: " + solution.longestWord(words2));
        
        System.out.println("\n--- Broken Chain Case ---");
        String[] words3 = {"a", "ab", "abcd"};
        // "abcd" is missing "abc", so it's invalid. The longest valid chain is "ab".
        System.out.println("Result: " + solution.longestWord(words3));

        System.out.println("\n--- Edge Case: No 1-letter prefixes ---");
        String[] words4 = {"ap", "app", "appl"};
        // Missing "a". No valid unbroken chain starts from the root.
        // Expected: ""
        System.out.println("Result: '" + solution.longestWord(words4) + "'");
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Trie (Prefix Tree) + Depth-First Search.
 * Key observation: A valid string is just a path in a Trie where every single 
 *                  node visited has `isEndOfWord == true`.
 * To memorize:     To break ties lexicographically in a Trie, process children 
 *                  indices from 0 to 25, and use `>` (strictly greater) when 
 *                  updating the global max.
 * Common trap:     Forgetting to store or pass the current string during DFS, 
 *                  or incorrectly trying to validate prefixes using `substring` 
 *                  in a loop, causing O(N*L^2) string thrashing.
 * Mental trigger:  "Every prefix must exist" -> Trie with an unbroken `isEnd` chain.
 * ============================================================================
 */


import java.util.*;

class TrieNode {

    TrieNode[] children = new TrieNode[26];
    boolean isEnd = false;
    String word;
}

public class Solution {

    public static String longestWord(String[] words) {

        TrieNode root = new TrieNode();

        for (String word : words) {
            TrieNode node = root;

            for (char ch : word.toCharArray()) {
                int index = ch - 'a';
                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }
                node = node.children[index];
            }

            node.isEnd = true;
            node.word = word;
        }

        Queue<TrieNode> queue = new ArrayDeque<>();
        for (TrieNode child : root.children) {
            if (child != null && child.isEnd) {
                queue.offer(child);
            }
        }

        String result = new String();
        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                TrieNode current = queue.poll();
                if (i == 0) {
                    result = current.word;
                }

                for (TrieNode child : current.children) {
                    if (child != null && child.isEnd) {
                        queue.offer(child);
                    }
                }
            }
        }
        return result;
    }
}


import java.util.*;

/**
 * Trie + BFS (Memory Optimized)
 *
 * Key Idea:
 * - BFS ensures increasing length
 * - Only traverse nodes with isEnd = true
 * - Maintain lexicographic order via iteration (a → z)
 */
class Solution {

    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isEnd = false;
    }

    public static String longestWord(String[] words) {

        TrieNode root = new TrieNode();

        // Step 1: Build Trie
        for (String word : words) {
            TrieNode node = root;

            for (char ch : word.toCharArray()) {
                int index = ch - 'a';

                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }

                node = node.children[index];
            }

            node.isEnd = true;
        }

        // Step 2: BFS
        Queue<Pair> queue = new ArrayDeque<>();

        // Start from valid 1-length words
        for (int i = 0; i < 26; i++) {
            if (root.children[i] != null && root.children[i].isEnd) {
                char ch = (char) ('a' + i);
                queue.offer(new Pair(root.children[i], String.valueOf(ch)));
            }
        }

        String result = "";

        while (!queue.isEmpty()) {

            int size = queue.size();

            for (int i = 0; i < size; i++) {

                Pair current = queue.poll();

                // First element in level = lexicographically smallest
                if (i == 0) {
                    result = current.word;
                }

                // Traverse children in lexicographic order
                for (int j = 0; j < 26; j++) {
                    TrieNode child = current.node.children[j];

                    if (child != null && child.isEnd) {
                        char ch = (char) ('a' + j);
                        queue.offer(new Pair(child, current.word + ch));
                    }
                }
            }
        }

        return result;
    }

    // Helper record (Java 21+ style)
    record Pair(TrieNode node, String word) {}
}

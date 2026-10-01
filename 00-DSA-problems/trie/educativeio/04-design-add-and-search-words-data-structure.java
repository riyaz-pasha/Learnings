/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: DESIGN ADD AND SEARCH WORDS DATA STRUCTURE
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Can duplicate words be added? If so, should `Get Words()` return them multiple times, or is a unique list expected?"
 *   (Affects whether we use a boolean `isEndOfWord` or an `int count` in the Trie).
 * - "Does `Get Words()` need to return the words in a specific order, like lexicographical?"
 *   (If lexicographical, a Trie naturally provides this via in-order DFS traversal).
 * - "What is the expected read vs. write ratio? Are we calling `Search` way more than `Add`?"
 *   (If writes are rare but searches are heavily wildcarded, we might cache results or optimize the tree structure).
 * - "Does the wildcard `.` only match exactly one character, or can it match zero characters?"
 *   (Validates we are building a fixed-length matching engine, not a full regex engine like `*`).
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * We need to store words and search for them. The binding constraint is the wildcard 
 * character `.`, which can be any letter. Standard hash-based lookups break completely 
 * because `.ad` hashes completely differently than `bad` or `cad`. 
 * 
 * APPROACH 1: List + Brute Force String Matching
 * 1. What I'd naturally try: Just keep all added words in an `ArrayList<String>`.
 * 2. Why it works: Adding is instant. For searching, I can iterate through the list. If 
 *    the lengths match, I check character by character, treating `.` as an automatic match.
 * 3. Why it's too slow: If I have 10,000 words that end in "ing", a search for ".......ing" 
 *    forces me to scan and compare against every single one of those words.
 * 4. What work is repeated: We repeatedly compare the same prefixes. If I check "bad" and 
 *    then "bat", I process 'b' and 'a' twice.
 * 5. Time Complexity:
 *    - Add: O(1) — just appending to a list.
 *    - Search: O(N * L) — comparing the query of length L against all N stored words.
 *    - Get Words: O(1) — returning the list.
 * 6. Space Complexity: O(N * L) — storing N completely separate string objects.
 * 
 * APPROACH 2: Length-Partitioned HashMap
 * 1. What I'd naturally try next: The wildcard `.` exactly replaces one character, so a 
 *    query of length 5 will ONLY ever match words of length 5. I could group words in 
 *    a `HashMap<Integer, List<String>>` where the key is the word length.
 * 2. Why it works: It instantly prunes out all words of the wrong length.
 * 3. Why it's still too slow: If I insert 1,000 words of length 5, a search for "b.tch" 
 *    still requires me to scan all 1,000 length-5 words.
 * 4. What work is repeated: We are still blindly iterating over words that share no common 
 *    characters with the search query. We haven't solved the shared-prefix problem.
 * 5. Time Complexity:
 *    - Add: O(1) — appending to the appropriate length bucket.
 *    - Search: O(K * L) — where K is the number of words of that exact length. Better, but worst-case still O(N * L).
 *    - Get Words: O(N) — iterating over all buckets to combine the lists.
 * 6. Space Complexity: O(N * L) — storing N string objects.
 * 
 * APPROACH 3: Trie + Depth-First Search (The Optimal Solution)
 * 1. The defining observation: Strings share prefixes. A Trie naturally merges shared 
 *    prefixes into single paths. When we encounter a `.`, instead of knowing exactly 
 *    which child to go to, we can simply explore *all* children that currently exist.
 * 2. Why it works: For standard letters, it's a lightning-fast O(1) step down the tree. 
 *    For `.`, it branches, but it instantly dies/prunes out paths if the Trie doesn't 
 *    have any words continuing down that branch.
 * 3. Time Complexity:
 *    - Add: O(L) — stepping down L nodes in the tree.
 *    - Search: 
 *      - Without dots: O(L) — just walking down the Trie.
 *      - With dots: O(26^D * L) — where D is the number of dots. Here, D <= 3, so worst 
 *        case is 26^3 (17,576) nodes visited, which is extremely fast. Pruning makes it 
 *        even faster in reality.
 *    - Get Words: O(N * L) — doing a full DFS of the Trie to construct all words.
 * 4. Space Complexity: O(N * L) — for the Trie nodes. No recursion stack risk since 
 *    max word length is 25 (stack depth max 25).
 * 
 * [What I'd write in an interview]
 * I would implement Approach 3. It directly addresses the wildcard branching constraint. 
 * Since the problem also demands `Get Words()`, doing a DFS on the Trie to collect 
 * them is a fantastic way to demonstrate tree traversal mastery, though keeping a 
 * separate `List<String>` alongside the Trie is a highly practical O(1) alternative 
 * I'd mention to the interviewer. I'll implement the DFS approach here.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - All dots: Searching for "..." when "cat" and "dog" exist. (DFS explores all paths 
 *   of length 3).
 * - Prefix match but not full word: Searching "ca" when "cat" was inserted. (DFS reaches 
 *   'a', but `isEndOfWord` is false -> returns false).
 * - Search word longer than Trie depth: Searching "c.t." when only "cat" is present. 
 *   (DFS hits a null child and safely backtracks/returns false).
 * - No words added yet: Searching anything returns false without NullPointerExceptions.
 * 
 * 
 * 4. KEY INSIGHT, EXAMPLES & DRY RUN
 * ----------------------------------------------------------------------------
 * [Key Insight]
 * "A dot is just a multi-way intersection." In a normal Trie search, you take the exact 
 * street the character tells you to. When you see a `.`, you pause, look at all available 
 * streets branching from your current node, and send a scout down *every single one* 
 * (using recursion). If any scout reports back `true`, you return `true`.
 * 
 * [Dry Run: Add "bad", Add "mad", Search ".ad"]
 * 1. Search ".ad", index = 0, node = root.
 *    - char is '.'. Loop through all 26 possible children of root.
 *    - child[0] ('a') is null. Skip.
 *    - child[1] ('b') exists! Recurse -> dfs("ad", child['b'], index 1).
 * 2. dfs(index 1) on 'b' node:
 *    - char is 'a'. child['a'] exists. Recurse -> dfs("ad", child['a'], index 2).
 * 3. dfs(index 2) on 'a' node:
 *    - char is 'd'. child['d'] exists. Recurse -> dfs("ad", child['d'], index 3).
 * 4. dfs(index 3) on 'd' node:
 *    - index == length of string (3 == 3). 
 *    - return node.isEndOfWord (which is TRUE).
 * 5. Returns true up the call stack! We don't even need to explore "mad".
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: What if `Get Words()` is called thousands of times per second?
 * A: Doing a DFS of the entire Trie every time is too slow. I would maintain a separate 
 *    `List<String> allWords` variable in the class. On `Add Word()`, I just add it to the 
 *    list. `Get Words()` then simply returns this list in O(1) time.
 * 
 * Q: How would you handle a `*` wildcard that matches 0 or more characters?
 * A: The DFS logic changes. When hitting `*`, I'd need to branch into two primary choices:
 *    1) The `*` matches 0 characters: recursively call `dfs(word, node, index + 1)`.
 *    2) The `*` matches 1 character: recursively call `dfs(word, child, index)` for all 
 *       non-null children (keeping the index the same so `*` can match the next one too).
 */

import java.util.ArrayList;
import java.util.List;

public class WordDictionaryStudyNote {

    static class WordDictionary {

        /**
         * TrieNode structure.
         * e.g., If we add "cat", root has a child at [2] ('c'). 
         * That child has a child at [0] ('a'), which has a child at [19] ('t').
         * The 't' node will have isEndOfWord = true.
         */
        private static class TrieNode {
            TrieNode[] children = new TrieNode[26];
            boolean isEndOfWord = false;
        }

        private final TrieNode root;

        public WordDictionary() {
            root = new TrieNode();
        }

        /**
         * Standard Trie insertion.
         * Iterates over the word, creating nodes if they don't exist.
         */
        public void addWord(String word) {
            TrieNode curr = root;
            for (int i = 0; i < word.length(); i++) {
                int index = word.charAt(i) - 'a';
                if (curr.children[index] == null) {
                    curr.children[index] = new TrieNode();
                }
                curr = curr.children[index];
            }
            // Mark the endpoint as a valid dictionary word
            curr.isEndOfWord = true;
        }

        /**
         * Initiates the DFS search to support wildcard matching.
         */
        public boolean search(String word) {
            return searchDFS(word, root, 0);
        }

        /**
         * HELPER: Recursive Depth-First Search for wildcard handling.
         * @param word  The string we are searching for (e.g., ".ad")
         * @param node  The current node in the Trie we are standing on
         * @param index The current character index we are checking in the word
         */
        private boolean searchDFS(String word, TrieNode node, int index) {
            // Base Case: If we've processed all characters in the word, 
            // check if the current node is actually the end of a recorded word.
            if (index == word.length()) {
                return node.isEndOfWord;
            }

            char ch = word.charAt(index);

            if (ch == '.') {
                // WILDCARD BRANCHING
                // We don't know which letter to pick, so we must try EVERY existing path.
                for (int i = 0; i < 26; i++) {
                    TrieNode child = node.children[i];
                    // If a path exists, send a scout (recurse) down it
                    if (child != null) {
                        // If any branch finds the rest of the string, return true immediately
                        if (searchDFS(word, child, index + 1)) {
                            return true;
                        }
                    }
                }
                // If all 26 branches fail (or are null), this path is a dead end.
                return false;
            } else {
                // NORMAL CHARACTER MATCHING
                int childIndex = ch - 'a';
                TrieNode child = node.children[childIndex];
                
                // If the exact letter path doesn't exist, search fails.
                if (child == null) {
                    return false;
                }
                
                // Path exists, continue down this exact branch.
                return searchDFS(word, child, index + 1);
            }
        }

        /**
         * Returns all words currently added to the dictionary by doing 
         * an in-order DFS traversal of the Trie.
         * 
         * Note: Maintaining a List<String> on `addWord` is O(1) and much faster,
         * but this traversal demonstrates how to extract strings from a Trie.
         */
        public List<String> getWords() {
            List<String> result = new ArrayList<>();
            // Use a StringBuilder to maintain the current path being explored
            collectWordsDFS(root, new StringBuilder(), result);
            return result;
        }

        /**
         * HELPER: Backtracking DFS to collect all words in the Trie.
         */
        private void collectWordsDFS(TrieNode node, StringBuilder currentPath, List<String> result) {
            // Base case/Execution step: if this node marks a word, save the path
            if (node.isEndOfWord) {
                result.add(currentPath.toString());
            }

            // Explore all 26 possible children
            for (int i = 0; i < 26; i++) {
                if (node.children[i] != null) {
                    // 1. Choose: Append the character for this branch
                    char ch = (char) (i + 'a');
                    currentPath.append(ch);
                    
                    // 2. Explore: Recurse deeper
                    collectWordsDFS(node.children[i], currentPath, result);
                    
                    // 3. Un-choose (Backtrack): Remove the character so we can try the next loop iteration
                    currentPath.deleteCharAt(currentPath.length() - 1);
                }
            }
        }
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        WordDictionary dict = new WordDictionary();
        
        System.out.println("--- Basic Operations ---");
        dict.addWord("bad");
        dict.addWord("dad");
        dict.addWord("mad");
        
        System.out.println("Search 'pad': " + dict.search("pad")); // false
        System.out.println("Search 'bad': " + dict.search("bad")); // true
        
        System.out.println("\n--- Wildcard Operations ---");
        System.out.println("Search '.ad': " + dict.search(".ad")); // true (matches bad, dad, mad)
        System.out.println("Search 'b..': " + dict.search("b..")); // true (matches bad)
        
        System.out.println("\n--- Edge Case: Prefix doesn't count ---");
        System.out.println("Search 'ba': " + dict.search("ba"));   // false (must be full word)
        
        System.out.println("\n--- Edge Case: Too many dots ---");
        System.out.println("Search '....': " + dict.search("...."));// false (no 4-letter words)

        System.out.println("\n--- Get All Words ---");
        // Expected: [bad, dad, mad] in alphabetical order because of 0-25 loop traversal
        System.out.println("All words: " + dict.getWords());
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Trie + Depth First Search (Backtracking).
 * Key observation: Iteration (`curr = curr.children[idx]`) works for exact chars, 
 *                  but wildcards require recursion to explore multiple `children` 
 *                  at the same tree depth simultaneously.
 * To memorize:     The DFS signature: `boolean dfs(String word, TrieNode node, int index)`.
 *                  The base case checks `index == word.length()`, returning `node.isEnd`.
 * Common trap:     Returning `false` immediately if one of the recursive dot searches
 *                  returns false. You must only return `true` if it matches, and 
 *                  continue the loop to try other paths if it doesn't.
 * Mental trigger:  "Trie + '.' wildcard? -> DFS branch all non-null children."
 * ============================================================================
 */


import java.util.*;

/**
 * WordDictionary (Trie + DFS Backtracking)
 *
 * ----------------------------------------
 * 🧠 Core Idea:
 * ----------------------------------------
 * 1. Use Trie to efficiently store words (prefix-based structure)
 * 2. Use DFS (backtracking) during search to handle '.' wildcard
 *
 * Why Trie?
 * - Fast prefix traversal → O(L)
 * - Natural fit for dictionary problems
 *
 * Why DFS?
 * - '.' means "match ANY character"
 * - So we must explore ALL possible paths → branching
 *
 * ----------------------------------------
 * 🧩 Mental Model for Search:
 * ----------------------------------------
 * Example: search("b..")
 *
 *        root
 *          |
 *          b
 *        / | \
 *       a  o  e ...
 *      /   |   \
 *     d    t    g ...
 *
 * → At '.', we branch into ALL children
 *
 * ----------------------------------------
 * ⏱ Complexity:
 * ----------------------------------------
 * addWord: O(L)
 *
 * search:
 *   worst-case: O(26^d * L)
 *   where d = number of '.' (max 3)
 *
 * getWords: O(N * L)
 *
 * ----------------------------------------
 * 💾 Space:
 * ----------------------------------------
 * O(N * L) for Trie
 */
public class WordDictionary {

    /**
     * Trie Node Definition
     *
     * children[i] → next node for character ('a' + i)
     * isEnd → marks end of a valid word
     */
    static class TrieNode {
        TrieNode[] children = new TrieNode[26]; // fixed size → faster than HashMap
        boolean isEnd;
    }

    private final TrieNode root;

    public WordDictionary() {
        this.root = new TrieNode(); // root is empty starting point
    }

    /**
     * ----------------------------------------
     * Add Word to Trie
     * ----------------------------------------
     *
     * Traverse character by character:
     * - Create node if not exists
     * - Move forward
     * - Mark last node as word end
     *
     * Example:
     * addWord("bad")
     *
     * root → b → a → d (isEnd = true)
     *
     * TC: O(L)
     */
    public void addWord(String word) {
        TrieNode node = root;

        for (char ch : word.toCharArray()) {
            int index = ch - 'a';

            // Create node if path doesn't exist
            if (node.children[index] == null) {
                node.children[index] = new TrieNode();
            }

            // Move to next node
            node = node.children[index];
        }

        // Mark end of word
        node.isEnd = true;
    }

    /**
     * ----------------------------------------
     * Search Word (supports '.' wildcard)
     * ----------------------------------------
     *
     * Delegates to DFS helper
     */
    public boolean search(String word) {
        return dfs(word, 0, root);
    }

    /**
     * ----------------------------------------
     * DFS (Backtracking) Search
     * ----------------------------------------
     *
     * index → current position in word
     * node  → current Trie node
     *
     * ----------------------------------------
     * 🧠 Key Logic:
     *
     * 1. If index == word.length:
     *      → we matched all characters
     *      → return if it's a valid word
     *
     * 2. If current char is NOT '.':
     *      → follow only ONE path
     *
     * 3. If current char is '.':
     *      → explore ALL possible children (branching)
     *
     * ----------------------------------------
     */
    private boolean dfs(String word, int index, TrieNode node) {

        // Base Case:
        // If we reached end of word → check if current node marks a valid word
        if (index == word.length()) {
            return node.isEnd;
        }

        char ch = word.charAt(index);

        /**
         * ----------------------------------------
         * Case 1: Normal character
         * ----------------------------------------
         * Just move to the corresponding child
         */
        if (ch != '.') {
            TrieNode next = node.children[ch - 'a'];

            // If path doesn't exist → word not found
            if (next == null) return false;

            // Continue DFS on that path
            return dfs(word, index + 1, next);
        }

        /**
         * ----------------------------------------
         * Case 2: Wildcard '.'
         * ----------------------------------------
         * Try ALL possible children
         *
         * Important:
         * - This is where branching happens
         * - Worst-case exponential, but limited (max 3 dots)
         */
        for (TrieNode child : node.children) {

            // Only explore existing nodes
            if (child != null) {

                // If any path returns true → we found a match
                if (dfs(word, index + 1, child)) {
                    return true; // early exit (optimization)
                }
            }
        }

        // No valid path found
        return false;
    }

    /**
     * ----------------------------------------
     * Get All Words
     * ----------------------------------------
     *
     * Perform DFS traversal on Trie
     *
     * Build words using StringBuilder (backtracking)
     *
     * TC: O(N * L)
     */
    public List<String> getWords() {
        List<String> result = new ArrayList<>();

        // Use StringBuilder to build words efficiently
        StringBuilder path = new StringBuilder();

        collectWords(root, path, result);

        return result;
    }

    /**
     * DFS to collect all words
     *
     * path → current word being built
     */
    private void collectWords(TrieNode node, StringBuilder path, List<String> result) {

        // If current node marks end → add word
        if (node.isEnd) {
            result.add(path.toString());
        }

        // Explore all children
        for (int i = 0; i < 26; i++) {

            if (node.children[i] != null) {

                // Choose
                path.append((char) ('a' + i));

                // Explore
                collectWords(node.children[i], path, result);

                // Backtrack (VERY IMPORTANT)
                path.deleteCharAt(path.length() - 1);
            }
        }
    }
}

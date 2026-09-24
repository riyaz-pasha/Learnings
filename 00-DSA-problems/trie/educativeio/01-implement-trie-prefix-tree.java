/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: IMPLEMENT TRIE (PREFIX TREE)
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Are the strings strictly lowercase a-z, or should I handle uppercase, numbers, or full Unicode?"
 *   (Prevents assuming a fixed array size of 26; Unicode requires a HashMap for children.)
 * - "Can we insert an empty string, and if so, how should search handle it?"
 *   (Clarifies if the root node itself can be a valid word.)
 * - "Is the data structure heavily read-heavy or write-heavy?"
 *   (If read-heavy, a static array of size 26 is much faster. If memory-constrained and sparse, a HashMap is better.)
 * - "Do we need thread safety?"
 *   (Prevents assuming a single-threaded environment; if concurrent, we'd need ConcurrentHashMap or locks.)
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * We need to store a set of strings and query them. Exact matches are easy, but 
 * the binding constraint is efficiently querying *prefixes*. A standard hash set 
 * falls over because checking if a prefix exists requires scanning the whole set.
 * 
 * APPROACH 1: HashSet (The Brute Force)
 * 1. What I'd naturally try: Add all words to a `HashSet<String>`.
 * 2. Why it works: Exact matches are instantaneous via hashing. 
 * 3. Why it's too slow: Prefix searching is the bottleneck. To implement `startsWith(prefix)`, 
 *    we have to iterate through every word in the set and check if it starts with the prefix.
 * 4. What work is repeated: We repeatedly compare the same prefix characters against many 
 *    different words that share no common prefix.
 * 5. Time Complexity: 
 *    - Insert: O(L) — hashing the string of length L.
 *    - Search: O(L) — hashing the string of length L.
 *    - StartsWith: O(N * L) — checking every single string (N strings) up to length L.
 * 6. Space Complexity: O(N * L) — storing N completely separate strings in memory.
 * 
 * APPROACH 2: Sorted Array + Binary Search (The Intermediate Improvement)
 * 1. What I'd naturally try next: To speed up prefix searches without a custom tree, I 
 *    could keep the words in a dynamically sorted `ArrayList<String>`.
 * 2. Why it works: In a sorted list, all words sharing a prefix are grouped together. 
 *    We can binary search the prefix!
 * 3. Why it's too slow: While search is fast, insertion becomes agonizingly slow.
 * 4. What property removes the bottleneck: The realization that keeping things sorted in a 
 *    flat array requires shifting elements. We need a structure that naturally "sorts" 
 *    by prefix without shifting data.
 * 5. Time Complexity: 
 *    - Insert: O(N * L) — shifting N elements in an array to maintain sort order.
 *    - Search / StartsWith: O(L * log N) — binary searching N elements, doing string comparisons of length L.
 * 6. Space Complexity: O(N * L) — storing all strings in an array.
 * 
 * APPROACH 3: The Prefix Tree / Trie (The Optimal Solution)
 * 1. The defining observation: Strings with the same prefix share identical characters 
 *    at the start. If we store characters as nodes in a tree, "car" and "cat" can share 
 *    the nodes 'c' -> 'a', and only branch at 'r' and 't'.
 * 2. Why it works: Looking up a prefix simply means walking down the tree. If we reach the 
 *    end of the prefix string and haven't fallen off the tree, the prefix exists. 
 * 3. Time Complexity:
 *    - Insert: O(L) — because we only process the L characters of the string once, stepping down the tree.
 *    - Search: O(L) — because we just follow L pointers. Independent of N (total words)!
 *    - StartsWith: O(L) — exact same as search, just stopping early.
 * 4. Space Complexity:
 *    - O(N * L) — in the absolute worst case where no words share any prefixes.
 *    - Space is vastly smaller in practice because overlapping prefixes merge. 
 *    - No recursion stack is used (iterative traversal).
 * 
 * [What I'd write in an interview]
 * I would immediately write Approach 3 (Trie) using fixed-size arrays (Node[26]) for children. 
 * It's the industry standard for this exact problem, the code is surprisingly brief, and 
 * it avoids the hashing overhead of using HashMaps at every character node.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - Overlapping word inserts: Inserting "app", then "apple", then "app" again. (Must not 
 *   overwrite or erase the `isEndOfWord` flag for "app").
 * - Search prefix that matches exactly a full word: searching "cat" as a prefix when "cat" 
 *   is in the tree. (Should return true).
 * - Searching a word that only exists as a prefix: searching "app" when only "apple" 
 *   was inserted. (search="app" must be false, startsWith="app" must be true).
 * - Empty string insertions/searches: Properly handling `""` at the root node.
 * 
 * 
 * 4. KEY INSIGHTS & EXAMPLES
 * ----------------------------------------------------------------------------
 * [ASCII Diagram: The State of the Trie after inserting "cat", "car", "do"]
 * 
 *          (root)
 *          /    \
 *        'c'    'd'
 *        /        \
 *      'a'        'o'* 
 *      / \
 *   'r'* 't'*
 * 
 * * indicates `isEndOfWord = true`
 * 
 * [Dry Run: insert("cat"), search("ca"), startsWith("ca")]
 * 1. insert("cat"): 
 *    - curr = root
 *    - char 'c': root.children['c'] is null. Create Node. curr = root.children['c'].
 *    - char 'a': curr.children['a'] is null. Create Node. curr = curr.children['a'].
 *    - char 't': curr.children['t'] is null. Create Node. curr = curr.children['t'].
 *    - End of string. Mark curr.isEndOfWord = true.
 * 2. search("ca"):
 *    - curr = root
 *    - char 'c': found. curr moves to 'c' node.
 *    - char 'a': found. curr moves to 'a' node.
 *    - End of string. Return curr.isEndOfWord. (It's false, so return false).
 * 3. startsWith("ca"):
 *    - Exactly the same traversal as search.
 *    - End of string. We don't care about isEndOfWord. Just return true since we didn't crash.
 * 
 * [Pattern Recognition]
 * "When you see string matching involving prefixes, auto-completions, or finding the longest 
 * common prefix among many strings, think Trie."
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: How would you modify this to support a wildcard search like "c.t" where '.' matches anything?
 * A: Instead of iterative traversal, I'd use DFS (recursion). When hitting a '.', iterate 
 *    through all 26 possible children, recursively calling the search on the next character.
 * 
 * Q: What if memory is a huge issue and the tree is very sparse?
 * A: I would change `TrieNode[26]` to a `HashMap<Character, TrieNode>`. It slows down execution 
 *    slightly due to hashing overhead but avoids allocating 26 pointers for nodes that only 
 *    have 1 child. Furthermore, we could use a Radix Tree (Patricia Trie) to compress paths 
 *    where nodes only have one child (e.g., compressing c->a->t into a single edge "cat").
 * 
 * Q: How would you implement delete(word)?
 * A: I'd search for the word. If found, unmark `isEndOfWord`. If that node now has no 
 *    children, I can safely delete it, propagating the deletion upwards until a node has 
 *    other children or is marked as the end of another word.
 */

public class TrieStudyNote {

    /**
     * The node structure for our Prefix Tree.
     * We don't actually store the character inside the node. The *index* in the 
     * array implies the character. 
     */
    static class TrieNode {
        // e.g. children[0] represents 'a', children[25] represents 'z'.
        // If we insert "cat", root.children[2] will point to a new TrieNode.
        TrieNode[] children;
        
        // e.g. If we insert "app", the node reached after 'p' will have this set to true.
        // If we later insert "apple", this 'p' node remains true, and 'e' is also marked true.
        boolean isEndOfWord;

        public TrieNode() {
            // 26 lowercase English letters.
            this.children = new TrieNode[26];
            this.isEndOfWord = false;
        }
    }

    static class Trie {
        private final TrieNode root;

        public Trie() {
            root = new TrieNode();
        }

        /**
         * Inserts a word into the trie.
         */
        public void insert(String word) {
            TrieNode curr = root;
            
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                int index = ch - 'a'; // Maps 'a' to 0, 'b' to 1, etc.
                
                // If the path doesn't exist, build the bridge.
                if (curr.children[index] == null) {
                    curr.children[index] = new TrieNode();
                }
                
                // Move our pointer down the tree
                curr = curr.children[index];
            }
            
            // At the end of the string, flag this specific node as a valid endpoint.
            curr.isEndOfWord = true;
        }

        /**
         * Returns true if the complete word is in the trie.
         */
        public boolean search(String word) {
            TrieNode node = searchPrefixNode(word);
            // It's only a match if the path existed AND someone explicitly 
            // ended a word at this exact node.
            return node != null && node.isEndOfWord;
        }

        /**
         * Returns true if there is any previously inserted string that starts with the prefix.
         */
        public boolean startsWith(String prefix) {
            TrieNode node = searchPrefixNode(prefix);
            // As long as the path exists (node != null), it's a valid prefix.
            // We don't care if it's the end of a full word or not.
            return node != null;
        }

        /**
         * HELPER METHOD
         * Both `search` and `startsWith` do the exact same traversal. 
         * This helper extracts the duplication. It returns the final node if the 
         * path exists, or null if we fall off the tree.
         */
        private TrieNode searchPrefixNode(String prefix) {
            TrieNode curr = root;
            
            for (int i = 0; i < prefix.length(); i++) {
                char ch = prefix.charAt(i);
                int index = ch - 'a';
                
                // If at any point the next letter isn't in our tree, the prefix/word fails.
                if (curr.children[index] == null) {
                    return null;
                }
                
                curr = curr.children[index];
            }
            
            return curr;
        }
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        Trie trie = new Trie();
        
        System.out.println("--- Basic Operations ---");
        trie.insert("apple");
        System.out.println("Search 'apple': " + trie.search("apple"));   // true
        System.out.println("Search 'app': " + trie.search("app"));       // false (only a prefix, not a word)
        System.out.println("StartsWith 'app': " + trie.startsWith("app"));// true
        
        System.out.println("\n--- Edge Case: Inserting a prefix later ---");
        trie.insert("app");
        System.out.println("Search 'app' now: " + trie.search("app"));   // true (now it IS a word)
        
        System.out.println("\n--- Edge Case: Branching ---");
        trie.insert("apply");
        System.out.println("Search 'apply': " + trie.search("apply"));   // true
        System.out.println("Search 'apple': " + trie.search("apple"));   // true (still exists)
        
        System.out.println("\n--- Edge Case: Not found ---");
        System.out.println("Search 'bat': " + trie.search("bat"));       // false
        System.out.println("StartsWith 'b': " + trie.startsWith("b"));    // false
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Prefix-based State Machine / 26-ary Tree.
 * Key observation: Strings sharing a prefix can share physical memory nodes.
 * To memorize:     The TrieNode struct (`Node[] children`, `boolean isEnd`).
 * Common trap:     Forgetting to actually check `isEndOfWord` at the end of `search`, 
 *                  accidentally returning true just because the path existed.
 * Mental trigger:  "Fast prefix lookup? Autocomplete? -> Trie."
 * ============================================================================
 */


import java.util.*;

/**
 * Trie Implementation (Array-based)
 *
 * Why Array?
 * - Faster than HashMap (O(1) direct indexing)
 * - Memory predictable (26 children per node)
 *
 * Time Complexity:
 * - Insert: O(L)
 * - Search: O(L)
 * - Prefix: O(L)
 *
 * Space Complexity:
 * - O(N * 26) worst-case
 */
public class Trie {

    /**
     * Trie Node
     */
    static class TrieNode {
        TrieNode[] children = new TrieNode[26]; // a-z
        boolean isEndOfWord; // marks complete word
    }

    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    /**
     * Insert word into Trie
     *
     * Idea:
     * - Walk char by char
     * - Create node if absent
     */
    public void insert(String word) {
        TrieNode current = root;

        for (char ch : word.toCharArray()) {
            int index = ch - 'a'; // map 'a' -> 0

            if (current.children[index] == null) {
                current.children[index] = new TrieNode(); // create path
            }

            current = current.children[index]; // move forward
        }

        current.isEndOfWord = true; // mark complete word
    }

    /**
     * Search full word
     *
     * Must end at isEndOfWord = true
     */
    public boolean search(String word) {
        TrieNode node = traverse(word);
        return node != null && node.isEndOfWord;
    }

    /**
     * Search prefix
     *
     * Only path existence matters
     */
    public boolean startsWith(String prefix) {
        return traverse(prefix) != null;
    }

    /**
     * Helper method for traversal
     */
    private TrieNode traverse(String str) {
        TrieNode current = root;

        for (char ch : str.toCharArray()) {
            int index = ch - 'a';

            if (current.children[index] == null) {
                return null; // path broken
            }

            current = current.children[index];
        }

        return current;
    }
}

import java.util.*;

/**
 * Trie using HashMap
 *
 * Pros:
 * - Flexible character set
 *
 * Cons:
 * - Slightly slower than array
 */
public class TrieMap {

    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd;
    }

    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        TrieNode current = root;

        for (char ch : word.toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode());
        }

        current.isEnd = true;
    }

    public boolean search(String word) {
        TrieNode node = traverse(word);
        return node != null && node.isEnd;
    }

    public boolean startsWith(String prefix) {
        return traverse(prefix) != null;
    }

    private TrieNode traverse(String str) {
        TrieNode current = root;

        for (char ch : str.toCharArray()) {
            current = current.children.get(ch);

            if (current == null) return null;
        }

        return current;
    }
}

/**
 * Trie (Prefix Tree) for lowercase English words: 'a' - 'z'.
 *
 * Supported operations:
 *  - insert(word)
 *  - search(word)
 *  - startsWith(prefix)
 *  - delete(word)
 *
 * Assumption:
 *  - Input contains only lowercase English letters.
 *
 * Let L = length of the input word/prefix.
 */
public class Trie {

    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    /**
     * Inserts a word into the Trie.
     *
     * Time:
     *   O(L), where L = word.length()
     *
     * Space:
     *   O(L) worst case for newly created nodes.
     *   O(1) auxiliary space.
     */
    public void insert(String word) {
        validateInput(word);

        TrieNode current = root;

        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);

            // TrieNode handles whether the child already exists.
            current = current.getOrCreateChild(ch);
        }

        current.markAsWordEnd();
    }

    /**
     * Searches for an exact word in the Trie.
     *
     * Important:
     *   "app" and "apple" are different words.
     *
     * Time:
     *   O(L), where L = word.length()
     *
     * Space:
     *   O(1) auxiliary space.
     */
    public boolean search(String word) {
        validateInput(word);

        TrieNode node = findNode(word);

        return node != null && node.isWordEnd();
    }

    /**
     * Checks whether at least one inserted word starts with the
     * given prefix.
     *
     * Example:
     *   insert("apple")
     *   startsWith("app") -> true
     *
     * Time:
     *   O(L), where L = prefix.length()
     *
     * Space:
     *   O(1) auxiliary space.
     */
    public boolean startsWith(String prefix) {
        validateInput(prefix);

        return findNode(prefix) != null;
    }

    /**
     * Deletes a word from the Trie.
     *
     * Important:
     *   We only remove nodes that are no longer needed by any
     *   other word.
     *
     * Example:
     *
     *   insert("app")
     *   insert("apple")
     *
     *   delete("app")
     *
     *   "app" is no longer a word,
     *   but the nodes must remain because "apple" still exists.
     *
     * Time:
     *   O(L), where L = word.length()
     *
     * Space:
     *   O(L) auxiliary space due to recursion depth.
     */
    public boolean delete(String word) {
        validateInput(word);

        // First check whether the word actually exists.
        TrieNode node = findNode(word);

        if (node == null || !node.isWordEnd()) {
            return false;
        }

        deleteRecursive(root, word, 0);

        return true;
    }

    /**
     * Finds the TrieNode corresponding to the given string.
     *
     * Returns null if the string does not exist as a path.
     *
     * Time:
     *   O(L)
     *
     * Space:
     *   O(1)
     */
    private TrieNode findNode(String text) {
        TrieNode current = root;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            current = current.getChild(ch);

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Recursively removes unnecessary nodes after deleting a word.
     *
     * Returns true when the current node itself can safely be removed
     * from its parent.
     *
     * A node can be removed only when:
     *
     *   1. It is not the end of another word.
     *   2. It has no children.
     *
     * Time:
     *   O(L)
     *
     * Space:
     *   O(L) recursion depth.
     */
    private boolean deleteRecursive(
            TrieNode current,
            String word,
            int depth
    ) {
        // We reached the node representing the complete word.
        if (depth == word.length()) {
            current.unmarkAsWordEnd();

            // Parent can remove this node only if it has no children.
            return !current.hasChildren();
        }

        char ch = word.charAt(depth);
        TrieNode child = current.getChild(ch);

        boolean shouldDeleteChild =
                deleteRecursive(child, word, depth + 1);

        if (shouldDeleteChild) {
            current.removeChild(ch);
        }

        // Current node can be deleted when:
        //   - it is not the end of another word
        //   - it has no remaining children
        //
        // Note:
        // We never actually remove the root node.
        return !current.isWordEnd() && !current.hasChildren();
    }

    /**
     * Validates the input according to this Trie implementation's
     * supported alphabet.
     *
     * Time:
     *   O(L)
     *
     * Space:
     *   O(1)
     */
    private void validateInput(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch < 'a' || ch > 'z') {
                throw new IllegalArgumentException(
                        "Only lowercase English letters (a-z) are supported."
                );
            }
        }
    }

    /**
     * A single node inside the Trie.
     *
     * This class is private because users of Trie should not need
     * to know how the Trie is internally implemented.
     */
    private static final class TrieNode {

        private static final int ALPHABET_SIZE = 26;

        private final TrieNode[] children;
        private boolean isWordEnd;

        private TrieNode() {
            children = new TrieNode[ALPHABET_SIZE];
        }

        /**
         * Returns the child for the given character.
         *
         * Does NOT create a node.
         *
         * Time: O(1)
         * Space: O(1)
         */
        private TrieNode getChild(char ch) {
            return children[toIndex(ch)];
        }

        /**
         * Returns the child for the given character.
         *
         * If the child does not exist, it creates it.
         *
         * This is intentionally kept inside TrieNode because
         * TrieNode owns the children and their creation.
         *
         * Time: O(1)
         * Space: O(1) for the created node
         */
        private TrieNode getOrCreateChild(char ch) {
            int index = toIndex(ch);

            if (children[index] == null) {
                children[index] = new TrieNode();
            }

            return children[index];
        }

        /**
         * Removes a child.
         *
         * Time: O(1)
         * Space: O(1)
         */
        private void removeChild(char ch) {
            children[toIndex(ch)] = null;
        }

        /**
         * Checks whether this node has at least one child.
         *
         * Time:
         *   O(26) -> O(1), because alphabet size is fixed.
         *
         * Space:
         *   O(1)
         */
        private boolean hasChildren() {
            for (TrieNode child : children) {
                if (child != null) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Marks this node as the end of a complete word.
         *
         * Time: O(1)
         * Space: O(1)
         */
        private void markAsWordEnd() {
            isWordEnd = true;
        }

        /**
         * Removes the word-ending marker.
         *
         * Time: O(1)
         * Space: O(1)
         */
        private void unmarkAsWordEnd() {
            isWordEnd = false;
        }

        /**
         * Checks whether this node represents the end of a word.
         *
         * Time: O(1)
         * Space: O(1)
         */
        private boolean isWordEnd() {
            return isWordEnd;
        }

        /**
         * Converts 'a' - 'z' into indices 0 - 25.
         *
         * Time: O(1)
         * Space: O(1)
         */
        private int toIndex(char ch) {
            if (ch < 'a' || ch > 'z') {
                throw new IllegalArgumentException(
                        "Only lowercase English letters (a-z) are supported."
                );
            }

            return ch - 'a';
        }
    }
}

import java.util.HashMap;
import java.util.Map;

/**
 * Trie (Prefix Tree) implementation using HashMap for children.
 *
 * Unlike the array-based version, this implementation does not assume
 * a fixed alphabet such as 'a' - 'z'.
 *
 * Example:
 *
 *   insert("apple")
 *   insert("app")
 *   search("app")        -> true
 *   search("appl")       -> false
 *   startsWith("ap")     -> true
 *   delete("app")        -> true
 *
 * Let L = length of the input word/prefix.
 */
public class Trie {

    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    /**
     * Inserts a word into the Trie.
     *
     * Time:
     *   O(L) average
     *
     *   HashMap get/put is O(1) average, so we perform
     *   at most one map operation for each character.
     *
     * Space:
     *   O(L) worst case for newly created nodes.
     *
     *   Auxiliary space:
     *   O(1) excluding newly created TrieNodes.
     */
    public void insert(String word) {
        validateInput(word);

        TrieNode current = root;

        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);

            // TrieNode owns the logic for finding or creating
            // the child corresponding to this character.
            current = current.getOrCreateChild(ch);
        }

        current.markAsWordEnd();
    }

    /**
     * Searches for an exact word.
     *
     * Example:
     *
     *   insert("apple")
     *
     *   search("app")   -> false
     *   search("apple") -> true
     *
     * Time:
     *   O(L) average
     *
     * Space:
     *   O(1) auxiliary space.
     */
    public boolean search(String word) {
        validateInput(word);

        TrieNode node = findNode(word);

        return node != null && node.isWordEnd();
    }

    /**
     * Checks whether at least one inserted word starts
     * with the given prefix.
     *
     * Example:
     *
     *   insert("apple")
     *
     *   startsWith("app") -> true
     *   startsWith("xyz") -> false
     *
     * Time:
     *   O(L) average
     *
     * Space:
     *   O(1) auxiliary space.
     */
    public boolean startsWith(String prefix) {
        validateInput(prefix);

        return findNode(prefix) != null;
    }

    /**
     * Deletes a word from the Trie.
     *
     * Nodes are removed only when they are no longer required
     * by another word.
     *
     * Example:
     *
     *   insert("app")
     *   insert("apple")
     *
     *   delete("app")
     *
     *   "app" is removed as a word,
     *   but the shared nodes remain because "apple" still exists.
     *
     * Time:
     *   O(L) average
     *
     * Space:
     *   O(L) auxiliary space due to recursion.
     */
    public boolean delete(String word) {
        validateInput(word);

        TrieNode node = findNode(word);

        // Word does not exist.
        if (node == null || !node.isWordEnd()) {
            return false;
        }

        deleteRecursive(root, word, 0);

        return true;
    }

    /**
     * Finds the TrieNode representing the given string.
     *
     * Returns null when the character path does not exist.
     *
     * Time:
     *   O(L) average
     *
     * Space:
     *   O(1)
     */
    private TrieNode findNode(String text) {
        TrieNode current = root;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            current = current.getChild(ch);

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Recursively removes unnecessary nodes after a word is deleted.
     *
     * Returns true when the current node can be safely removed
     * from its parent.
     *
     * A node can be removed when:
     *
     *   1. It is not the end of another word.
     *   2. It has no children.
     *
     * Time:
     *   O(L) average
     *
     * Space:
     *   O(L) recursion depth.
     */
    private boolean deleteRecursive(
            TrieNode current,
            String word,
            int depth
    ) {
        // We have reached the node representing the complete word.
        if (depth == word.length()) {
            current.unmarkAsWordEnd();

            // This node can be removed if it has no children.
            return !current.hasChildren();
        }

        char ch = word.charAt(depth);

        TrieNode child = current.getChild(ch);

        boolean shouldDeleteChild =
                deleteRecursive(child, word, depth + 1);

        if (shouldDeleteChild) {
            current.removeChild(ch);
        }

        // Current node can be removed if it:
        //   - is not the end of another word
        //   - has no remaining children
        return !current.isWordEnd() && !current.hasChildren();
    }

    /**
     * This implementation supports any character.
     *
     * We only reject null because null cannot represent a word.
     *
     * Time:
     *   O(1)
     *
     * Space:
     *   O(1)
     */
    private void validateInput(String text) {
        if (text == null) {
            throw new IllegalArgumentException(
                    "Word/prefix cannot be null."
            );
        }
    }

    /**
     * Internal node of the Trie.
     *
     * The class is private because callers should interact
     * with the Trie, not with individual nodes.
     */
    private static final class TrieNode {

        /**
         * Each character maps to the child node that represents it.
         */
        private final Map<Character, TrieNode> children;

        /**
         * True when a complete word ends at this node.
         *
         * Example:
         *
         *   insert("app")
         *
         *             a -> p -> p
         *                     ^
         *                     |
         *                 isWordEnd=true
         */
        private boolean isWordEnd;

        private TrieNode() {
            children = new HashMap<>();
        }

        /**
         * Returns the child for the given character.
         *
         * Does not create a child.
         *
         * Time:
         *   O(1) average
         *
         * Space:
         *   O(1)
         */
        private TrieNode getChild(char ch) {
            return children.get(ch);
        }

        /**
         * Returns the child for the given character.
         *
         * Creates the child if it does not already exist.
         *
         * This method belongs here because TrieNode owns
         * the children map.
         *
         * Time:
         *   O(1) average
         *
         * Space:
         *   O(1) for a newly created node.
         */
        private TrieNode getOrCreateChild(char ch) {
            TrieNode child = children.get(ch);

            if (child == null) {
                child = new TrieNode();
                children.put(ch, child);
            }

            return child;
        }

        /**
         * Removes the child corresponding to the character.
         *
         * Time:
         *   O(1) average
         *
         * Space:
         *   O(1)
         */
        private void removeChild(char ch) {
            children.remove(ch);
        }

        /**
         * Checks whether this node has any children.
         *
         * HashMap.size() is O(1).
         *
         * Time:
         *   O(1)
         *
         * Space:
         *   O(1)
         */
        private boolean hasChildren() {
            return !children.isEmpty();
        }

        /**
         * Marks this node as the end of a complete word.
         *
         * Time:
         *   O(1)
         *
         * Space:
         *   O(1)
         */
        private void markAsWordEnd() {
            isWordEnd = true;
        }

        /**
         * Removes the word-ending marker.
         *
         * Time:
         *   O(1)
         *
         * Space:
         *   O(1)
         */
        private void unmarkAsWordEnd() {
            isWordEnd = false;
        }

        /**
         * Checks whether this node represents
         * the end of a complete word.
         *
         * Time:
         *   O(1)
         *
         * Space:
         *   O(1)
         */
        private boolean isWordEnd() {
            return isWordEnd;
        }
    }
}

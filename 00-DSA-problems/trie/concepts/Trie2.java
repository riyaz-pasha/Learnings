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

class Trie2 {

    private final TrieNode root;

    public Trie2() {
        root = new TrieNode();
    }

    public void insert(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            if (!node.containsKey(ch)) {
                node.put(ch, new TrieNode());
            }
            node = node.get(ch);
        }
        node.setEnd();
    }

    public boolean search(String word) {
        TrieNode node = searchPrefix(word);
        return node != null && node.isEnd();
    }

    public boolean startsWith(String prefix) {
        return searchPrefix(prefix) != null;
    }

    private TrieNode searchPrefix(String prefix) {
        TrieNode node = root;
        for (char ch : prefix.toCharArray()) {
            if (!node.containsKey(ch)) {
                return null;
            }
            node = node.get(ch);
        }
        return node;
    }

}

class TrieNode {

    private final TrieNode[] links;
    private boolean isEnd;

    TrieNode() {
        links = new TrieNode[26];
        isEnd = false;
    }

    private int getIndex(char ch) {
        return ch - 'a';
    }

    public boolean containsKey(char ch) {
        return links[getIndex(ch)] != null;
    }

    public void put(char ch, TrieNode node) {
        links[getIndex(ch)] = node;
    }

    public TrieNode get(char ch) {
        return links[getIndex(ch)];
    }

    public void setEnd() {
        isEnd = true;
    }

    public boolean isEnd() {
        return isEnd;
    }

}

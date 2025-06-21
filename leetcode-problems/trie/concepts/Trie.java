class Trie {

    private TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    // Insert method (same as before)
    public void insert(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            int index = ch - 'a';
            if (node.children[index] == null) {
                node.children[index] = new TrieNode();
            }
            node = node.children[index];
        }
        node.isEndOfWord = true;
    }

    // Search method (same as before)
    public boolean search(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            int index = ch - 'a';
            if (node.children[index] == null)
                return false;
            node = node.children[index];
        }
        return node.isEndOfWord;
    }

    // Prefix method (same as before)
    public boolean startsWith(String prefix) {
        TrieNode node = root;
        for (char ch : prefix.toCharArray()) {
            int index = ch - 'a';
            if (node.children[index] == null)
                return false;
            node = node.children[index];
        }
        return true;
    }

    // 🔥 Delete method
    public void delete(String word) {
        deleteHelper(root, word, 0);
    }

    // Recursive helper method
    private boolean deleteHelper(TrieNode node, String word, int depth) {
        if (node == null)
            return false;

        // Step 1: Base case - end of the word
        if (depth == word.length()) {
            // If word does not exist
            if (!node.isEndOfWord)
                return false;

            // Unmark the end of word
            node.isEndOfWord = false;

            // If node has no children, it's safe to delete
            return isNodeEmpty(node);
        }

        // Step 2: Recurse for the child
        int index = word.charAt(depth) - 'a';
        TrieNode child = node.children[index];

        boolean shouldDeleteChild = deleteHelper(child, word, depth + 1);

        // Step 3: If child should be deleted, remove reference
        if (shouldDeleteChild) {
            node.children[index] = null;

            // Check if current node is now useless
            return !node.isEndOfWord && isNodeEmpty(node);
        }

        return false;
    }

    // Utility to check if a node has any children
    private boolean isNodeEmpty(TrieNode node) {
        for (TrieNode child : node.children) {
            if (child != null)
                return false;
        }
        return true;
    }

}

class TrieNode {

    TrieNode[] children = new TrieNode[26]; // For 26 lowercase English letters
    boolean isEndOfWord = false; // Marks the end of a word

}

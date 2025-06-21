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

# DIY: Implement Trie

## Problem statement

Implement a **Trie** (prefix tree) with `insert(string)`, `search(string)`, and `searchPrefix(string)`. Inputs are lowercase strings, length under 100.

### Input

```java
"makeup"
```

### Output

```java
true
```

## Coding exercise

Implement the `Trie` class.

This is exactly the structure from [Feature #1: Store and Fetch Words](01-feature-1-store-and-fetch-words.md), just under the more standard name `Trie` instead of `WordDictionary`.

## Solution

```java
import java.util.HashMap;

class Node {
    public HashMap<Character, Node> children = new HashMap<>();
    public boolean isWord = false;
}

class Trie {
    private final Node root;

    public Trie() {
        root = new Node();
    }

    public void insert(String word) {
        Node current = root;
        for (char c : word.toCharArray()) {
            current.children.putIfAbsent(c, new Node());
            current = current.children.get(c);
        }
        current.isWord = true;
    }

    public boolean search(String word) {
        Node node = walk(word);
        return node != null && node.isWord;
    }

    public boolean searchPrefix(String prefix) {
        return walk(prefix) != null;
    }

    private Node walk(String s) {
        Node current = root;
        for (char c : s.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return null;
            }
            current = current.children.get(c);
        }
        return current;
    }

    public static void main(String[] args) {
        Trie trie = new Trie();
        trie.insert("makeup");

        System.out.println(trie.search("makeup"));     // true
        System.out.println(trie.search("make"));        // false
        System.out.println(trie.searchPrefix("make"));  // true
    }
}
```

## Complexity measures

Let **l** be the length of the input string.

- **`insert()`:** `O(l)` time, `O(l)` space.
- **`search()` / `searchPrefix()`:** `O(l)` time, `O(1)` space.

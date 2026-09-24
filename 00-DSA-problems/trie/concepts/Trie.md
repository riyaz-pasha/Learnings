## ✅ TrieNode & Trie Class using `HashMap<Character, TrieNode>`

```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord = false;
}

public class Trie {
    private final TrieNode root = new TrieNode();

    // Insert
    public void insert(String word) {
        TrieNode current = root;
        for (char ch : word.toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        current.isEndOfWord = true;
    }

    // Search
    public boolean search(String word) {
        TrieNode current = root;
        for (char ch : word.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) return false;
        }
        return current.isEndOfWord;
    }

    // Delete
    public boolean delete(String word) {
        return delete(root, word, 0);
    }

    private boolean delete(TrieNode current, String word, int index) {
        if (index == word.length()) {
            if (!current.isEndOfWord) return false; // word not found
            current.isEndOfWord = false;
            return current.children.isEmpty(); // true if no children -> delete this node
        }

        char ch = word.charAt(index);
        TrieNode child = current.children.get(ch);
        if (child == null) return false;

        boolean shouldDeleteCurrentNode = delete(child, word, index + 1);

        if (shouldDeleteCurrentNode) {
            current.children.remove(ch);
            return current.children.isEmpty() && !current.isEndOfWord;
        }

        return false;
    }
}
```

---

## 🔍 Explanation of Operations

### 1. `insert(String word)`

* For each character, create a new `TrieNode` if it doesn't exist.
* Mark `isEndOfWord = true` for the last node.

**Time Complexity:** `O(L)`
**Space Complexity:** `O(L)` (new nodes for new characters)
Where `L` = length of word.

---

### 2. `search(String word)`

* Traverse character-by-character.
* Return `true` only if `isEndOfWord == true` at the end.

**Time Complexity:** `O(L)`
**Space Complexity:** `O(1)` (no extra space used)

---

### 3. `delete(String word)`

* Recursively backtrack from the end of the word.
* Only delete nodes that are not shared with other words.

**Time Complexity:** `O(L)`
**Space Complexity:** `O(L)` (stack space for recursion)

---

## 📊 Time & Space Complexity Summary

| Operation | Time Complexity | Space Complexity |
| --------- | --------------- | ---------------- |
| Insert    | O(L)            | O(L) (new nodes) |
| Search    | O(L)            | O(1)             |
| Delete    | O(L)            | O(L) (recursion) |

---

## ⚖️ Pros and Cons

### ✅ Pros

* **Fast prefix-based operations**: Great for autocomplete, prefix search.
* **Efficient retrieval**: Search time doesn’t depend on number of words.
* **Memory-optimized with HashMap**: Dynamic size and avoids fixed 26-length arrays.

### ❌ Cons

* **Higher memory usage** than other structures (like HashSet) for small datasets.
* **Delete is non-trivial**: Needs careful backtracking.
* **Slower than array-based Trie** if all inputs are lowercase a-z (due to hashing overhead).

---

## 🌍 Real-World Applications

1. **Autocomplete systems** – search suggestions (Google, IDEs)
2. **Spell checkers** – fast word lookup and suggestion
3. **IP routing (longest prefix match)** – networks & routers
4. **Word games & puzzles** – fast prefix verification
5. **Search engines** – efficient indexing for text
6. **Dictionary or phonebook lookup** – fast search by prefixes

---

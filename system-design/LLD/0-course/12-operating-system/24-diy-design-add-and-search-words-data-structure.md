# DIY: Design Add and Search Words Data Structure

## Problem statement

Design a data structure, `WordDictionary`, that supports:

- `WordDictionary()` — initializes the object.
- `void addWord(word)` — adds `word` to the data structure. You cannot add a word that's already present.
- `boolean search(word)` — returns `true` if any word in the dictionary matches `word`. `word` may contain `.` characters, each of which can match any single letter.
- `List<String> getWords()` — returns all words currently in the data structure.

### Input / Output

```java
addWord("bad");   // no return value
addWord("dad");
addWord("mad");
search("pad");    // false
search("bad");    // true
search(".ad");    // true
getWords();       // [bad, dad, mad]
```

## Coding exercise

Implement `WordDictionary`.

This is the exact same pattern as [Feature #6: File Management System](06-feature-6-file-management-system.md) — there, the OS needed to add file/directory names and support wildcard searches over them; here it's the bare pattern with no story attached, just words instead of file names. The approach is identical: a trie where each node maps characters to children, with recursive search branching into every child whenever a `.` wildcard is encountered.

## Solution

```java
import java.util.*;

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd = false;
}

class WordDictionary {
    private final TrieNode root = new TrieNode();

    public WordDictionary() {
    }

    public void addWord(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd = true;
    }

    public boolean search(String word) {
        return searchHelper(root, word, 0);
    }

    private boolean searchHelper(TrieNode node, String word, int idx) {
        if (idx == word.length()) return node.isEnd;
        char c = word.charAt(idx);
        if (c == '.') {
            for (TrieNode child : node.children.values()) {
                if (searchHelper(child, word, idx + 1)) return true;
            }
            return false;
        }
        TrieNode child = node.children.get(c);
        return child != null && searchHelper(child, word, idx + 1);
    }

    public List<String> getWords() {
        List<String> result = new ArrayList<>();
        collect(root, new StringBuilder(), result);
        return result;
    }

    private void collect(TrieNode node, StringBuilder path, List<String> result) {
        if (node.isEnd) result.add(path.toString());
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            path.append(entry.getKey());
            collect(entry.getValue(), path, result);
            path.deleteCharAt(path.length() - 1);
        }
    }

    public static void main(String[] args) {
        WordDictionary dict = new WordDictionary();
        dict.addWord("bad");
        dict.addWord("dad");
        dict.addWord("mad");

        System.out.println(dict.search("pad"));
        // false
        System.out.println(dict.search("bad"));
        // true
        System.out.println(dict.search(".ad"));
        // true
        System.out.println(dict.getWords());
        // [bad, dad, mad]
    }
}
```

## Complexity measures

Let **m** be the length of a word and **n** be the number of stored words.

- **addWord:** `O(m)` time, `O(m)` space in the worst case (no shared prefix).
- **search:** `O(26^m)` time in the worst case (all wildcards), `O(m)` recursion stack space.
- **getWords:** `O(n × m)` time and space to walk and collect every stored word.

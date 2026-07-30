# DIY: Word Search II

## Problem statement

You're given a list of strings to find in an `n x n` 2D grid of characters. As before, only up, down, left, and right neighbors count as adjacent, and no cell may be reused within a single string's trace. Return the strings from the input list that were actually found in the grid.

### Input

```java
char[][] grid = {
    {'C', 'O', 'L', 'I', 'M'},
    {'I', 'N', 'L', 'M', 'O'},
    {'A', 'L', 'I', 'E', 'O'},
    {'R', 'T', 'A', 'S', 'N'},
    {'S', 'I', 'T', 'A', 'C'}
};
String[] words = {"REINDEER", "IN", "RAIN"};
```

### Output

```java
[IN, RAIN]
```

(The order of the strings in the output doesn't matter.)

## Coding exercise

Implement `findStrings(grid, words)`.

This is the exact same pattern as [Feature #2: Search for Maximum Number of Words in the Boggle Grid](02-feature-2-search-for-maximum-number-of-words-in-the-boggle-grid.md) — there the story was a difficult-mode computer player; here it's the bare multi-word search with no story attached. The approach is identical: insert every candidate word into a Trie, then run a single DFS sweep over the grid, following the Trie and the grid together so a dead prefix stops exploration immediately instead of repeating the search once per word.

## Solution

```java
import java.util.*;

class Solution {
    static final int ALPHABET_SIZE = 26;

    static class TrieNode {
        TrieNode[] children = new TrieNode[ALPHABET_SIZE];
        boolean isWord = false;
        String word = null;
    }

    private static void insert(TrieNode root, String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int i = c - 'A';
            if (node.children[i] == null) {
                node.children[i] = new TrieNode();
            }
            node = node.children[i];
        }
        node.isWord = true;
        node.word = word;
    }

    public static List<String> findStrings(char[][] grid, String[] words) {
        TrieNode root = new TrieNode();
        for (String word : words) {
            insert(root, word);
        }

        List<String> found = new ArrayList<>();
        int rows = grid.length, cols = grid[0].length;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                dfs(grid, row, col, root, found);
            }
        }
        return found;
    }

    private static void dfs(char[][] grid, int row, int col, TrieNode node, List<String> found) {
        int rows = grid.length, cols = grid[0].length;
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }
        char letter = grid[row][col];
        if (letter == '#') {
            return;
        }
        TrieNode next = node.children[letter - 'A'];
        if (next == null) {
            return;
        }

        if (next.isWord && next.word != null) {
            found.add(next.word);
            next.word = null;
        }

        grid[row][col] = '#';
        dfs(grid, row + 1, col, next, found);
        dfs(grid, row - 1, col, next, found);
        dfs(grid, row, col + 1, next, found);
        dfs(grid, row, col - 1, next, found);
        grid[row][col] = letter;
    }

    public static void main(String[] args) {
        char[][] grid = {
            {'C', 'O', 'L', 'I', 'M'},
            {'I', 'N', 'L', 'M', 'O'},
            {'A', 'L', 'I', 'E', 'O'},
            {'R', 'T', 'A', 'S', 'N'},
            {'S', 'I', 'T', 'A', 'C'}
        };
        String[] words = {"REINDEER", "IN", "RAIN"};
        System.out.println(findStrings(grid, words));
        // [IN, RAIN]
    }
}
```

The Trie lets one DFS sweep answer for every word at once: at each grid cell we only descend into a neighbor if the Trie has a child for that neighbor's letter, so a word like `REINDEER` that has no path in this grid gets abandoned the moment its prefix stops matching, instead of being searched in full.

## Complexity measures

Let **n** be the number of cells in the grid, **l** be the length of the longest word, and **m** be the total number of characters across all words.

- **Time:** `O(n × 3ˡ)` — DFS starts from every cell, following the Trie alongside it; in the worst case (no shared prefixes) the Trie can't prune anything.
- **Space:** `O(m)` — the Trie holds at most one node per character across all the words.

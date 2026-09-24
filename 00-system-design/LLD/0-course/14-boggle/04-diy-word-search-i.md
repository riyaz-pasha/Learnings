# DIY: Word Search I

## Problem statement

You're given an `n x n` 2D grid of characters. Find whether a specific string can be traced through the grid by combining adjacent characters. Only up, down, left, and right neighbors count as adjacent — no diagonals, and no cell may be reused within the same trace.

### Input

```java
char[][] grid = {
    {'H', 'O', 'L', 'I', 'K'},
    {'O', 'M', 'L', 'M', 'E'},
    {'O', 'E', 'I', 'A', 'Y'},
    {'R', 'T', 'A', 'S', 'O'},
    {'S', 'I', 'T', 'T', 'R'}
};
String word = "MAYOR";
```

### Output

```java
true
```

## Coding exercise

Implement `findString(grid, word)`.

This is the exact same pattern as [Feature #1: Search for a Single Word in the Boggle Grid](01-feature-1-search-for-a-single-word-in-the-boggle-grid.md) — there the story was an easy-mode computer player; here it's the bare search function with no story attached. The approach is identical: DFS from every cell, backtracking with a visited marker, matching one character of the target word per step.

## Solution

```java
class Solution {
    public static boolean findString(char[][] grid, String word) {
        int rows = grid.length, cols = grid[0].length;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (dfs(grid, row, col, word, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean dfs(char[][] grid, int row, int col, String word, int index) {
        if (index == word.length()) {
            return true;
        }
        int rows = grid.length, cols = grid[0].length;
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return false;
        }
        if (grid[row][col] != word.charAt(index)) {
            return false;
        }

        char original = grid[row][col];
        grid[row][col] = '#';

        boolean found = dfs(grid, row + 1, col, word, index + 1)
                || dfs(grid, row - 1, col, word, index + 1)
                || dfs(grid, row, col + 1, word, index + 1)
                || dfs(grid, row, col - 1, word, index + 1);

        grid[row][col] = original;
        return found;
    }

    public static void main(String[] args) {
        char[][] grid = {
            {'H', 'O', 'L', 'I', 'K'},
            {'O', 'M', 'L', 'M', 'E'},
            {'O', 'E', 'I', 'A', 'Y'},
            {'R', 'T', 'A', 'S', 'O'},
            {'S', 'I', 'T', 'T', 'R'}
        };
        System.out.println(findString(grid, "MAYOR"));
        // true
    }
}
```

`dfs` walks the grid one character of the target word at a time: it bails out the moment the current cell is out of bounds or doesn't match the next needed character, otherwise it marks the cell used and tries all four directions for the next character, unmarking on the way back out so a different path can reuse the cell.

## Complexity measures

Let **n** be the number of cells in the grid and **l** be the length of the word.

- **Time:** `O(n × 3ˡ)` — DFS starts from every cell, and each step beyond the first has at most three fresh directions to explore.
- **Space:** `O(l)` — the recursion stack goes as deep as the word is long.

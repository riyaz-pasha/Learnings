# DIY: Longest Increasing Path in a Matrix

## Problem statement

Given an `m x n` matrix, return the length of the longest strictly increasing path in it. From each cell, you can move in one of four directions — left, right, up, or down — but not diagonally, and never outside the matrix's boundaries.

### Input

```java
matrix = {
    {3, 4, 5},
    {3, 2, 6},
    {2, 2, 1}
}
```

### Output

```
4
```

The longest increasing path is `{3, 4, 5, 6}`.

## Coding exercise

Implement `longestIncreasingPath(matrix)`, returning the length of the longest strictly increasing path.

This is the exact same pattern as [Feature #4: Maximum Routers](04-feature-4-maximum-routers.md) — there, a packet could only forward to a strictly-higher-ID neighbor; here it's the bare pattern, no networking story. Run DFS with memoization from every cell, following only strictly increasing neighbors, and cache each cell's result so no cell is ever recomputed.

## Solution

```java
class Solution {
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    public static int longestIncreasingPath(int[][] matrix) {
        if (matrix.length == 0) {
            return 0;
        }

        int rows = matrix.length, cols = matrix[0].length;
        int[][] cache = new int[rows][cols];
        int longest = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                longest = Math.max(longest, dfs(matrix, i, j, cache));
            }
        }
        return longest;
    }

    private static int dfs(int[][] matrix, int i, int j, int[][] cache) {
        if (cache[i][j] != 0) {
            return cache[i][j];
        }

        int best = 1;
        for (int[] d : DIRECTIONS) {
            int ni = i + d[0], nj = j + d[1];
            if (ni >= 0 && nj >= 0 && ni < matrix.length && nj < matrix[0].length
                    && matrix[ni][nj] > matrix[i][j]) {
                best = Math.max(best, 1 + dfs(matrix, ni, nj, cache));
            }
        }

        cache[i][j] = best;
        return best;
    }

    public static void main(String[] args) {
        int[][] matrix = {
            {3, 4, 5},
            {3, 2, 6},
            {2, 2, 1}
        };
        System.out.println(longestIncreasingPath(matrix));
        // 4
    }
}
```

## Complexity measures

Let **m** be the number of rows and **n** the number of columns.

- **Time:** `O(m x n)` — memoization guarantees each cell's real DFS work happens exactly once.
- **Space:** `O(m x n)` — for the cache array and, in the worst case, the recursion stack.

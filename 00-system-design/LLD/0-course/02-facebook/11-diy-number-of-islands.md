# DIY: Number of Islands

## Problem statement

Given an `m x n` 2D grid of `"1"` (land) and `"0"` (water), return the number of islands. An island is formed by connecting adjacent land cells horizontally or vertically; all four edges of the grid are surrounded by water.

### Input

```java
grid = {
  {"1","1","0","0","0"},
  {"1","1","0","0","0"},
  {"0","0","1","0","0"},
  {"0","0","0","1","1"}
}
```

### Output

```java
3
```

## Coding exercise

Implement `numIslands(islands)`.

Same idea as [Feature #1: Friend Circles](01-feature-1-friend-circles.md) — counting connected components — just on a grid instead of an adjacency matrix, so "neighbors" means up/down/left/right instead of matrix rows.

## Solution

```java
class Solution {

    public static int numIslands(String[][] grid) {
        if (grid == null || grid.length == 0) {
            return 0;
        }

        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int islands = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].equals("1") && !visited[r][c]) {
                    dfs(grid, visited, r, c);
                    islands++;
                }
            }
        }

        return islands;
    }

    private static void dfs(String[][] grid, boolean[][] visited, int r, int c) {
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length) {
            return;
        }
        if (visited[r][c] || !grid[r][c].equals("1")) {
            return;
        }

        visited[r][c] = true;

        dfs(grid, visited, r + 1, c);
        dfs(grid, visited, r - 1, c);
        dfs(grid, visited, r, c + 1);
        dfs(grid, visited, r, c - 1);
    }

    public static void main(String[] args) {
        String[][] grid = {
                {"1", "1", "0", "0", "0"},
                {"1", "1", "0", "0", "0"},
                {"0", "0", "1", "0", "0"},
                {"0", "0", "0", "1", "1"}
        };
        System.out.println(numIslands(grid)); // 3
    }
}
```

## Complexity measures

Let **m x n** be the grid's dimensions.

- **Time:** `O(m × n)` — every cell is visited at most once.
- **Space:** `O(m × n)` — the `visited` array, plus up to `O(m × n)` recursion depth in the worst case (one giant island).

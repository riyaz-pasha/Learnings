# DIY: Max Area of an Island

## Problem statement

Given an m x n binary matrix `grid` containing `0`s and `1`s, return the maximum contiguous area occupied by an island.

Each cell represents land (`1`) or water (`0`). A group of four-directionally adjacent land cells constitutes an island. The area of an island is its number of land cells.

Note: you may assume all four edges of the grid are surrounded by water.

### Input

```java
// Example 1:
// [[0, 1, 1],
//  [1, 0, 1],
//  [1, 1, 1]]
//
// Example 2:
// []
```

### Output

```java
// Example 1: 7
// Example 2: 0
```

## Coding exercise

Implement the `maxAreaOfIsland(grid)` function, where `grid` is the binary matrix containing `0`s and `1`s. The function returns an integer representing the maximum contiguous area occupied by an island.

This is exactly [Feature #7: Maximum Contiguous Area](07-feature-7-maximum-contiguous-area.md) — the same DFS flood-fill over connected components, just renamed away from the "signal coverage" framing to land and water.

## Solution

```java
class Solution {
    public static int maxAreaOfIsland(int[][] grid) {
        if (grid == null || grid.length == 0) {
            return 0;
        }
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int maxArea = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 1 && !visited[r][c]) {
                    maxArea = Math.max(maxArea, explore(grid, visited, r, c));
                }
            }
        }
        return maxArea;
    }

    private static int explore(int[][] grid, boolean[][] visited, int r, int c) {
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length) {
            return 0;
        }
        if (visited[r][c] || grid[r][c] == 0) {
            return 0;
        }
        visited[r][c] = true;

        int area = 1;
        area += explore(grid, visited, r + 1, c);
        area += explore(grid, visited, r - 1, c);
        area += explore(grid, visited, r, c + 1);
        area += explore(grid, visited, r, c - 1);
        return area;
    }

    public static void main(String[] args) {
        int[][] grid1 = {
            {0, 1, 1},
            {1, 0, 1},
            {1, 1, 1}
        };
        System.out.println(maxAreaOfIsland(grid1)); // 7

        int[][] grid2 = {};
        System.out.println(maxAreaOfIsland(grid2)); // 0
    }
}
```

## Solution walkthrough

Every land cell in `grid1` — `(0,1), (0,2), (1,0), (1,2), (2,0), (2,1), (2,2)` — turns out to be 4-directionally reachable from every other one: `(0,1)-(0,2)-(1,2)-(2,2)-(2,1)-(2,0)-(1,0)` forms one connected chain. So a single DFS flood from any of them covers all 7 land cells, giving the maximum area of `7`. An empty grid has no cells to explore at all, so the answer is `0`.

## Complexity measures

Let **m** and **n** be the number of rows and columns.

### Time Complexity

`O(m * n)` — every cell is visited at most once across the whole scan.

### Space Complexity

`O(m * n)` — for the `visited` grid, plus the DFS recursion stack in the worst case where the entire grid is one island.

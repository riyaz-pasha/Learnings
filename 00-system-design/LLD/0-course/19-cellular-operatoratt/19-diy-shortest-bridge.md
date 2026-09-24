# DIY: Shortest Bridge

## Problem statement

You are given an n x n binary matrix `grid` containing `0`s and `1`s. Each cell represents land (`1`) or water (`0`). A group of four-directionally adjacent land cells constitutes an island. There are **exactly two islands** in the grid.

You may change `0`s to `1`s to connect the two islands into one. Return the smallest number of `0`s you must flip to connect them.

You may assume all four edges of the grid are surrounded by water.

### Constraints

- `n == grid.length == grid[i].length`
- `2 <= n <= 100`
- `grid[i][j]` is either `0` or `1`
- There are exactly two islands in `grid`

### Input

```java
// Example 1:
// [[1, 0, 0],
//  [0, 0, 0],
//  [0, 0, 1]]
//
// Example 2:
// [[1, 0],
//  [0, 1]]
```

### Output

```java
// Example 1: 3
// Example 2: 1
```

## Coding exercise

Implement the `shortestBridge(grid)` function, where `grid` is the binary matrix containing exactly two islands. The function returns an integer representing the minimum number of `0`s that must be flipped to join them.

This builds directly on the connected-component flood-fill from [Feature #7: Maximum Contiguous Area](07-feature-7-maximum-contiguous-area.md): the first phase here uses the exact same DFS to find and mark one island's cells. What's new is the second phase — instead of just measuring the island, we grow outward from it one layer of water at a time (a multi-source breadth-first search) until we bump into the second island, which mirrors the BFS shortest-path expansion from [Feature #3: Power Up the Station](03-feature-3-power-up-the-station.md), just over grid cells instead of lock states.

## Solution

```java
import java.util.*;

class Solution {
    public static int shortestBridge(int[][] grid) {
        int n = grid.length;
        Deque<int[]> queue = new ArrayDeque<>();

        // Phase 1: DFS-flood the first island found, marking its cells as
        // visited (2) and seeding the BFS queue with all of its cells.
        outer:
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (grid[r][c] == 1) {
                    dfsMarkIsland(grid, r, c, queue);
                    break outer;
                }
            }
        }

        // Phase 2: multi-source BFS, expanding outward one water ring at a
        // time until we touch the second island.
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int steps = 0;
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                int[] cell = queue.poll();
                for (int[] d : dirs) {
                    int nr = cell[0] + d[0];
                    int nc = cell[1] + d[1];
                    if (nr < 0 || nr >= n || nc < 0 || nc >= n || grid[nr][nc] == 2) {
                        continue;
                    }
                    if (grid[nr][nc] == 1) {
                        return steps; // reached the second island
                    }
                    grid[nr][nc] = 2; // mark this water cell as visited
                    queue.add(new int[]{nr, nc});
                }
            }
            steps++;
        }
        return -1; // unreachable (shouldn't happen per problem constraints)
    }

    private static void dfsMarkIsland(int[][] grid, int r, int c, Deque<int[]> queue) {
        int n = grid.length;
        if (r < 0 || r >= n || c < 0 || c >= n || grid[r][c] != 1) {
            return;
        }
        grid[r][c] = 2; // mark visited
        queue.add(new int[]{r, c});
        dfsMarkIsland(grid, r + 1, c, queue);
        dfsMarkIsland(grid, r - 1, c, queue);
        dfsMarkIsland(grid, r, c + 1, queue);
        dfsMarkIsland(grid, r, c - 1, queue);
    }

    public static void main(String[] args) {
        int[][] grid1 = {
            {1, 0, 0},
            {0, 0, 0},
            {0, 0, 1}
        };
        System.out.println(shortestBridge(grid1)); // 3

        int[][] grid2 = {
            {1, 0},
            {0, 1}
        };
        System.out.println(shortestBridge(grid2)); // 1
    }
}
```

## Solution walkthrough

Phase 1 finds the first `1` it encounters scanning top-to-bottom, left-to-right, then DFS-floods every cell connected to it — marking each visited cell as `2` and enqueueing it as a BFS seed. Phase 2 treats every cell of that first island as being at "distance 0" simultaneously, and expands outward one ring of water at a time; the moment any expanding ring touches a cell that's still `1` (untouched, meaning it belongs to the second island), the current ring count (`steps`) is the answer.

For `grid1`, the two single-cell islands sit at `(0,0)` and `(2,2)` — 4 rows/columns apart diagonally, so it takes 3 rings of water expansion to bridge them (flip `(0,1), (0,2), (1,2)`, or symmetric equivalents), matching the expected `3`. For `grid2`, the islands at `(0,0)` and `(1,1)` are diagonal neighbors, one water cell apart either way — `(0,1)` or `(1,0)` — so a single flip bridges them, matching the expected `1`.

## Complexity measures

Let **n** be the grid's side length.

### Time Complexity

`O(n^2)` — the initial island search and DFS-flood together visit each cell at most once, and the BFS expansion afterward likewise visits each remaining cell at most once.

### Space Complexity

`O(n^2)` — in the worst case, the BFS queue and the DFS recursion stack can each hold up to `O(n^2)` cells.

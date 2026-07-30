# DIY: Rotting Oranges

## Problem statement

You're given an `m x n` grid where each cell holds one of three values: `0` (empty cell), `1` (fresh orange), or `2` (rotten orange). Every minute, any fresh orange 4-directionally adjacent to a rotten orange becomes rotten too. Return the minimum number of minutes until no fresh orange remains, or `-1` if that's impossible.

### Input

```java
grid = {
    {0, 1, 2},
    {1, 1, 0},
    {1, 1, 1}
}
```

### Output

```
4
```

## Coding exercise

Implement `orangesRotting(grid)`, returning the minutes needed for every fresh orange to rot, or `-1` if some fresh orange can never be reached.

This is the exact same pattern as [Feature #9: Update Configuration](09-feature-9-update-configuration.md) — there, an already-updated router's configuration spread to its neighbors, one minute at a time; here it's the bare pattern, no networking story. Multi-source BFS from every initially rotten orange, expanding one layer (one minute) at a time; if fresh oranges remain once the BFS runs dry, return `-1`.

## Solution

```java
import java.util.*;

class Solution {
    public static int orangesRotting(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        Queue<int[]> queue = new ArrayDeque<>();
        int freshCount = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 2) {
                    queue.add(new int[]{r, c});
                } else if (grid[r][c] == 1) {
                    freshCount++;
                }
            }
        }
        if (freshCount == 0) {
            return 0;
        }

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int minutes = -1;

        while (!queue.isEmpty()) {
            int size = queue.size();
            minutes++;
            for (int i = 0; i < size; i++) {
                int[] cell = queue.poll();
                for (int[] d : directions) {
                    int nr = cell[0] + d[0], nc = cell[1] + d[1];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] == 1) {
                        grid[nr][nc] = 2;
                        freshCount--;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return freshCount == 0 ? minutes : -1;
    }

    public static void main(String[] args) {
        int[][] grid = {
            {0, 1, 2},
            {1, 1, 0},
            {1, 1, 1}
        };
        System.out.println(orangesRotting(grid));
        // 4
    }
}
```

## Complexity measures

Let **n** be the number of cells in the grid.

- **Time:** `O(n)` — building the initial queue takes `O(n)`, and BFS visits every cell at most once more.
- **Space:** `O(n)` — the queue can hold up to every cell in the grid across the traversal.

# DIY: Minimum Path Sum

## Problem statement

Given an `m x n` grid filled with non-negative integers, find a path from the top-left cell to the bottom-right cell that minimizes the sum of all the numbers along the path. You can only move **right** or **down** at each step.

### Constraints

- `m == grid.length`
- `n == grid[i].length`
- `1 <= m, n <= 200`
- `0 <= grid[i][j] <= 100`

### Input

```java
[[1, 3, 7], [8, 5, 4], [4, 9, 1]]
```

### Output

```java
14
```

## Coding exercise

Implement `minPathSum(grid)`, returning the minimum path sum.

This is the exact same pattern as [Feature #8: Optimal Path](08-feature-8-optimal-path.md) — there, Uber found the cheapest route for a driver to reach a passenger across a grid-shaped map; here it's the bare pattern with no story attached. Fill the first row and column with running sums, then fill every other cell as the minimum of its top/left neighbor plus its own cost.

## Solution

```java
class Solution {
    public static int minPathSum(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        for (int col = 1; col < n; col++) {
            grid[0][col] += grid[0][col - 1];
        }

        for (int row = 1; row < m; row++) {
            grid[row][0] += grid[row - 1][0];
        }

        for (int row = 1; row < m; row++) {
            for (int col = 1; col < n; col++) {
                grid[row][col] += Math.min(grid[row - 1][col], grid[row][col - 1]);
            }
        }

        return grid[m - 1][n - 1];
    }

    public static void main(String[] args) {
        int[][] grid = {
            {1, 3, 7},
            {8, 5, 4},
            {4, 9, 1}
        };
        System.out.println(minPathSum(grid));
        // 14
    }
}
```

## Complexity measures

Let **m** and **n** be the number of rows and columns in the grid.

- **Time:** `O(m × n)` — every cell is visited exactly once.
- **Space:** `O(1)` extra space — the grid is updated in place.

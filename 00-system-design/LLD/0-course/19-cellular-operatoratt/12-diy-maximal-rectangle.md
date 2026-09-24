# DIY: Maximal rectangle

## Problem statement

Given an m x n binary matrix containing `0`s and `1`s, find the largest rectangle containing only `1`s and return its area.

### Input

```java
// matrix = {{"1","0","1","0","0"},
//           {"1","0","1","1","1"},
//           {"1","1","1","1","1"},
//           {"1","0","0","1","0"}}
```

### Output

```java
// 6
```

## Coding exercise

Implement the `maximalRectangle(matrix)` function, where `matrix` is the binary matrix containing `0`s and `1`s. The function returns an integer representing the maximum area covered by `1`s in a rectangular shape.

This is [Feature #2: Low Coverage Area](02-feature-2-low-coverage-area.md) itself, unrenamed — the exact same per-row `dp`-as-histogram plus largest-rectangle-in-histogram technique from [DIY: Largest Rectangle in Histogram](11-diy-largest-rectangle-in-histogram.md), just phrased directly in terms of the binary matrix instead of a mall coverage grid.

## Solution

```java
import java.util.*;

class Solution {
    public static int maximalRectangle(char[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            return 0;
        }
        int cols = matrix[0].length;
        int[] dp = new int[cols];
        int maxArea = 0;

        for (char[] row : matrix) {
            for (int i = 0; i < cols; i++) {
                dp[i] = row[i] == '1' ? dp[i] + 1 : 0;
            }
            maxArea = Math.max(maxArea, largestRectangleArea(dp));
        }
        return maxArea;
    }

    private static int largestRectangleArea(int[] heights) {
        Deque<Integer> stack = new ArrayDeque<>();
        int maxArea = 0;
        int n = heights.length;

        for (int i = 0; i <= n; i++) {
            int h = (i == n) ? 0 : heights[i];
            while (!stack.isEmpty() && heights[stack.peek()] >= h) {
                int height = heights[stack.pop()];
                int left = stack.isEmpty() ? -1 : stack.peek();
                int width = i - left - 1;
                maxArea = Math.max(maxArea, height * width);
            }
            stack.push(i);
        }
        return maxArea;
    }

    public static void main(String[] args) {
        char[][] matrix = {
            {'1', '0', '1', '0', '0'},
            {'1', '0', '1', '1', '1'},
            {'1', '1', '1', '1', '1'},
            {'1', '0', '0', '1', '0'}
        };
        System.out.println(maximalRectangle(matrix)); // 6
    }
}
```

Tracing the rows (confirmed by running the code above): after row 0, `dp = [1,0,1,0,0]` (max row area `1`); after row 1, `dp = [2,0,2,1,1]` (max row area `3`, from columns 2–4 at height >= 1); after row 2, `dp = [3,1,3,2,2]` — columns 2, 3, and 4 are all at height >= 2, giving a `width = 3, height = 2` rectangle of area `6`, the overall maximum; after row 3, `dp = [4,0,0,3,0]` (max row area `4`, from the single column-0 bar of height 4). The largest rectangle across all rows is `6`, matching the expected output.

## Complexity measures

Let **m** and **n** be the number of rows and columns.

### Time Complexity

`O(m * n)` — the `dp` update and the histogram scan are each `O(n)` per row, over `m` rows.

### Space Complexity

`O(n)` — `dp` and the monotonic stack each hold at most one entry per column.

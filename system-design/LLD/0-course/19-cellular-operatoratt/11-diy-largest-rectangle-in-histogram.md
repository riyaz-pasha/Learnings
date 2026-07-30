# DIY: Largest Rectangle in Histogram

## Problem statement

We are given an array of n non-negative integers representing a histogram's bar heights, where the width of each bar is 1. Find the area of the largest rectangle in the histogram.

### Input

```java
// heights = {2,1,5,6,2,3}
```

### Output

```java
// 10
```

## Coding exercise

Implement the `largestRectangleArea(heights)` function, where `heights` is the array of integers representing histogram bar heights. The function returns an integer representing the largest rectangular area.

This is the core subroutine inside [Feature #2: Low Coverage Area](02-feature-2-low-coverage-area.md) — that feature runs this exact algorithm once per row, on a `dp` array built from stacked `1`s, to extend it from a 1D histogram into a 2D "largest rectangle in a binary grid."

## Solution

```java
import java.util.*;

class Solution {
    public static int largestRectangleArea(int[] heights) {
        Deque<Integer> stack = new ArrayDeque<>(); // indices, strictly increasing heights
        int maxArea = 0;
        int n = heights.length;

        for (int i = 0; i <= n; i++) {
            int h = (i == n) ? 0 : heights[i]; // sentinel flushes remaining bars at the end
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
        int[] heights = {2, 1, 5, 6, 2, 3};
        System.out.println(largestRectangleArea(heights)); // 10
    }
}
```

Walking through `{2,1,5,6,2,3}`: the winning rectangle is the pair of bars `{5,6}` at indices 2–3, each with height at least `5`, giving `width = 2, height = 5, area = 10`. The monotonic stack finds this because when it encounters the `2` at index 4, it pops index 3 (height `6`, width `1`, area `6`) and then index 2 (height `5`; at that point the stack's new top is index 1 with height `1`, so `left = 1`, `width = 4 - 1 - 1 = 2`, `area = 5 * 2 = 10`) — the maximum.

## Complexity measures

Let **n** be the number of bars.

### Time Complexity

`O(n)` — every index is pushed onto the stack exactly once and popped at most once.

### Space Complexity

`O(n)` — the stack holds at most `n` indices.

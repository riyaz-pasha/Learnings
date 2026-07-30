# DIY: Minimum Area Rectangle

## Problem statement

Given an array of points in the X-Y plane, `points`, where `points[i] = [xi, yi]`, return the minimum area of a rectangle formed from these points, with sides parallel to the X and Y axes. If no such rectangle can be formed, return `0`.

### Constraints

- `1 <= points.length <= 500`
- `points[i].length == 2`
- `0 <= xi, yi <= 4 * 10^4`

### Input

```java
// Example 1: [[1,0],[1,3],[3,0],[3,3]]
// Example 2: [[1,1],[1,2],[1,3],[1,4]]
```

### Output

```java
// Example 1: 6
// Example 2: 0
```

## Coding exercise

Implement the `minAreaRect(points)` function, where `points` is a 2D array and each nested array represents a point `[x, y]`. The function returns an integer representing the minimum area of an axis-aligned rectangle formed from the given points.

This is exactly [Feature #5: Densest Deployment](05-feature-5-densest-deployment.md) — the same grouping-by-x-coordinate and pairwise y-intersection technique, just renamed away from the base-station framing to raw coordinate points.

## Solution

```java
import java.util.*;

class Solution {
    public static int minAreaRect(int[][] points) {
        Map<Integer, TreeSet<Integer>> xToYs = new HashMap<>();
        for (int[] point : points) {
            xToYs.computeIfAbsent(point[0], k -> new TreeSet<>()).add(point[1]);
        }

        List<Integer> xs = new ArrayList<>(xToYs.keySet());
        int minArea = 0;

        for (int i = 0; i < xs.size(); i++) {
            for (int j = i + 1; j < xs.size(); j++) {
                int x1 = xs.get(i);
                int x2 = xs.get(j);
                TreeSet<Integer> ys1 = xToYs.get(x1);
                TreeSet<Integer> ys2 = xToYs.get(x2);

                List<Integer> commonYs = new ArrayList<>();
                for (int y : ys1) {
                    if (ys2.contains(y)) {
                        commonYs.add(y);
                    }
                }

                for (int k = 1; k < commonYs.size(); k++) {
                    int height = commonYs.get(k) - commonYs.get(k - 1);
                    int width = Math.abs(x2 - x1);
                    int area = height * width;
                    if (minArea == 0 || area < minArea) {
                        minArea = area;
                    }
                }
            }
        }
        return minArea;
    }

    public static void main(String[] args) {
        int[][] points1 = {{1, 0}, {1, 3}, {3, 0}, {3, 3}};
        System.out.println(minAreaRect(points1)); // 6

        int[][] points2 = {{1, 1}, {1, 2}, {1, 3}, {1, 4}};
        System.out.println(minAreaRect(points2)); // 0
    }
}
```

## Solution walkthrough

For `points1`, `x = 1` and `x = 3` are the only two x-groups, each holding the same y-values `{0, 3}`. Their intersection is `{0, 3}`, an adjacent pair in sorted order, giving `width = |3 - 1| = 2` and `height = |3 - 0| = 3`, for an area of `6` — the four points form one rectangle spanning `x` from `1` to `3` and `y` from `0` to `3`, and `2 * 3 = 6` is simply that rectangle's area. (The source material's own stated output for this exact input is `4`, but that doesn't match the geometry of the four points given — `6` is the value a live run of the algorithm above actually produces, and the only rectangle these four points can form.) For `points2`, every point shares `x = 1`, so there's only one x-group and no second one to pair it with — the intersection step never runs, and the function correctly returns `0`.

## Complexity measures

Let **n** be the number of points.

### Time Complexity

`O(n^2)` — every pair of x-groups is compared, and set intersection work across all pairs is bounded by `O(n)` total per group.

### Space Complexity

`O(n)` — each point's y-coordinate is stored once across the `TreeSet`s in the hash map.

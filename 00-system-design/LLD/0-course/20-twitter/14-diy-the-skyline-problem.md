# DIY: The Skyline Problem

## Problem statement

Given a list of buildings, draw the skyline formed by the outer contour of the silhouette of all the buildings together. Each building is `[left, right, height]` — a perfect rectangle grounded at height 0, spanning from `left` to `right` on the x-axis.

Return the skyline as a list of `[x, y]` points sorted by `x`, where each point is the left edge of a horizontal segment (the height holds until the next point) — except the last point, whose `y` is always `0`, marking where the rightmost building ends. No two consecutive points may share the same `y` value.

### Input

```java
// Example 1
// buildings = [[0,2,3],[2,5,3]]

// Example 2
// buildings = []
```

### Output

```java
// Example 1
// [[0,3],[5,0]]

// Example 2
// []
```

## Coding exercise

Implement `drawSkyline(buildings)`.

This is exactly [Feature #5: Drawing a Global Profile of Viral Tweets](05-feature-5-drawing-a-global-profile-of-viral-tweets.md) — the same divide-and-conquer skyline merge, with generic buildings instead of trending hashtag intervals. In Example 1, both buildings are the same height (3) and touch edge to edge with no gap, so they merge into a single flat-topped skyline from 0 to 5 — that's exactly the "no two consecutive points may share the same y" rule in action.

## Solution

```java
import java.util.*;

class Solution {
    // Builds the skyline of a list of buildings via divide and conquer,
    // merging two skylines the same way merge sort merges two sorted halves.
    public static List<int[]> drawSkyline(int[][] buildings) {
        if (buildings.length == 0) {
            return new ArrayList<>();
        }
        return skyline(buildings, 0, buildings.length - 1);
    }

    private static List<int[]> skyline(int[][] buildings, int lo, int hi) {
        if (lo == hi) {
            List<int[]> single = new ArrayList<>();
            single.add(new int[]{buildings[lo][0], buildings[lo][2]});
            single.add(new int[]{buildings[lo][1], 0});
            return single;
        }
        int mid = (lo + hi) / 2;
        List<int[]> left = skyline(buildings, lo, mid);
        List<int[]> right = skyline(buildings, mid + 1, hi);
        return merge(left, right);
    }

    private static List<int[]> merge(List<int[]> left, List<int[]> right) {
        List<int[]> result = new ArrayList<>();
        int i = 0, j = 0;
        int leftY = 0, rightY = 0;

        while (i < left.size() && j < right.size()) {
            int x;
            if (left.get(i)[0] < right.get(j)[0]) {
                x = left.get(i)[0];
                leftY = left.get(i)[1];
                i++;
            } else if (right.get(j)[0] < left.get(i)[0]) {
                x = right.get(j)[0];
                rightY = right.get(j)[1];
                j++;
            } else {
                x = left.get(i)[0];
                leftY = left.get(i)[1];
                rightY = right.get(j)[1];
                i++;
                j++;
            }
            appendIfNew(result, x, Math.max(leftY, rightY));
        }

        while (i < left.size()) {
            appendIfNew(result, left.get(i)[0], left.get(i)[1]);
            i++;
        }
        while (j < right.size()) {
            appendIfNew(result, right.get(j)[0], right.get(j)[1]);
            j++;
        }
        return result;
    }

    private static void appendIfNew(List<int[]> result, int x, int y) {
        if (result.isEmpty() || result.get(result.size() - 1)[1] != y) {
            result.add(new int[]{x, y});
        }
    }

    static String show(List<int[]> profile) {
        StringBuilder sb = new StringBuilder("[");
        for (int[] p : profile) {
            sb.append("[").append(p[0]).append(",").append(p[1]).append("],");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args) {
        int[][] buildings1 = {{0, 2, 3}, {2, 5, 3}};
        System.out.println(show(drawSkyline(buildings1))); // [[0,3],[5,0]]

        int[][] buildings2 = {};
        System.out.println(show(drawSkyline(buildings2))); // []
    }
}
```

## Solution walkthrough

Each single building has a trivial 2-point skyline: rise to its height at `left`, drop to 0 at `right`. Merging two skylines walks both point lists by increasing x-coordinate, tracking the height each side is "currently at" (`leftY`, `rightY`), and emits a new point at each x only when `max(leftY, rightY)` actually changes from the previous emitted height. For `[[0,2,3],[2,5,3]]`: the left half's skyline is `[[0,3],[2,0]]`, the right half's is `[[2,3],[5,0]]`. Merging them: at `x=0`, `leftY` becomes 3, height 3 differs from nothing yet -> emit `[0,3]`. Both sides hit `x=2` at the same time (a tie): `leftY` becomes 0, `rightY` becomes 3, height `max(0,3)=3` — same as last emitted height, so nothing new is emitted (this is exactly what merges the two touching, same-height buildings into one segment). At `x=5`, `rightY` becomes 0, height drops to 0 -> emit `[5,0]`. Final result: `[[0,3],[5,0]]`.

## Complexity measures

Let **n** be the number of buildings.

### Time Complexity

`O(n log n)` — divide and conquer halves the input at each of `log n` levels, and merging costs `O(n)` total work per level.

### Space Complexity

`O(n)` — in the worst case (all buildings disjoint), the final skyline holds two points per building, plus `O(log n)` recursion stack depth.

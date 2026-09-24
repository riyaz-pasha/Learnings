# DIY: Trapping Rainwater

## Problem statement

You're given `n` non-negative integers representing an elevation map, where each bar has a width of 1. Compute how much water it can trap after raining.

### Input

```java
{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}
```

### Output

```java
6
```

## Coding exercise

Implement `trapWater(elevationMap)`, returning the total amount of water trapped.

This is the exact same pattern as [Feature #2: Path Cost](02-feature-2-path-cost.md) — there, Uber calculated water pooled between city checkpoints; here it's the bare pattern with no story attached. Build `leftMax` and `rightMax` arrays, then sum `min(leftMax[i], rightMax[i]) - elevation[i]` at each index.

## Solution

```java
class Solution {
    public static int trapWater(int[] elevationMap) {
        int n = elevationMap.length;
        if (n == 0) return 0;

        int[] leftMax = new int[n];
        int[] rightMax = new int[n];

        leftMax[0] = elevationMap[0];
        for (int i = 1; i < n; i++) {
            leftMax[i] = Math.max(leftMax[i - 1], elevationMap[i]);
        }

        rightMax[n - 1] = elevationMap[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i + 1], elevationMap[i]);
        }

        int water = 0;
        for (int i = 0; i < n; i++) {
            water += Math.min(leftMax[i], rightMax[i]) - elevationMap[i];
        }
        return water;
    }

    public static void main(String[] args) {
        int[] elevationMap = {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1};
        System.out.println(trapWater(elevationMap));
        // 6
    }
}
```

## Complexity measures

Let **n** be the length of the elevation map.

- **Time:** `O(n)` — three linear passes over the array.
- **Space:** `O(n)` — for the `leftMax` and `rightMax` arrays.

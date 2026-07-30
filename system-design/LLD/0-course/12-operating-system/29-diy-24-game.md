# DIY: 24 Game

## Problem statement

Given 4 cards, each with a number in `[1, 9]`, determine whether an arithmetic expression built by interleaving them with `+`, `-`, `*`, `/` can evaluate to exactly `24`.

Rules:
- `/` is real division, not integer division.
- Every operation combines exactly two numbers (`-` is never unary).
- Numbers can't be concatenated.

### Input

```java
cards = [4, 1, 8, 7]
```

### Output

```java
true
```

## Coding exercise

Implement `game24(cards)`.

This is the exact same pattern as [Feature #10: Decode a Message](10-feature-10-decode-a-message.md) — there, the target digest was a variable value derived from a proprietary function; here it's the classic, fixed target of `24`. The approach is identical: repeatedly combine any two numbers from the current list with every operator, replace them with the result, and recurse until either one number remains (check it against the target) or every combination is exhausted.

## Solution

```java
import java.util.*;

class Solution {
    private static final double EPSILON = 1e-6;

    public static boolean game24(int[] cards) {
        List<Double> nums = new ArrayList<>();
        for (int c : cards) nums.add(c * 1.0);
        return solve(nums);
    }

    private static boolean solve(List<Double> nums) {
        if (nums.size() == 1) {
            return Math.abs(nums.get(0) - 24.0) < EPSILON;
        }

        int n = nums.size();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                List<Double> nextNums = new ArrayList<>();
                for (int k = 0; k < n; k++) {
                    if (k != i && k != j) nextNums.add(nums.get(k));
                }
                for (double combined : combine(nums.get(i), nums.get(j))) {
                    nextNums.add(combined);
                    if (solve(nextNums)) return true;
                    nextNums.remove(nextNums.size() - 1);
                }
            }
        }
        return false;
    }

    private static List<Double> combine(double a, double b) {
        List<Double> results = new ArrayList<>();
        results.add(a + b);
        results.add(a - b);
        results.add(a * b);
        if (Math.abs(b) > EPSILON) results.add(a / b);
        return results;
    }

    public static void main(String[] args) {
        System.out.println(game24(new int[]{4, 1, 8, 7}));
        // true - (8 - 4) * (7 - 1) = 24
        System.out.println(game24(new int[]{1, 2, 1, 2}));
        // false - no combination reaches 24
    }
}
```

## Complexity measures

The input is always exactly 4 numbers, so this is a constant-size search.

- **Time:** `O(1)` — a fixed-size input yields a fixed-size search tree over pairs and operators.
- **Space:** `O(1)` — recursion depth is bounded by the fixed 3 combine-steps needed to go from 4 numbers to 1.

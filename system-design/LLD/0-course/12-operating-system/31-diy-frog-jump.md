# DIY: Frog Jump

## Problem statement

A river is divided into units, some of which have stones. A frog starts on the first stone and must reach the last stone by jumping only on stones (never into the water). The first jump must be `1` unit. If the frog's last jump was `k` units, the next jump must be `k - 1`, `k`, or `k + 1` units (and always forward, so `0` isn't a valid jump size once past the start).

Given the sorted positions of the stones, determine whether the frog can reach the last one.

### Input

```java
// Example 1
stones = [0, 1, 3, 5, 6, 8, 12, 17]

// Example 2
stones = [0, 1, 2, 3, 7, 11, 18]
```

### Output

```java
// Example 1
true

// Example 2
false
```

## Coding exercise

Implement `frogJump(stones)`.

This is the exact same pattern as [Feature #12: Priority Validation](12-feature-12-priority-validation.md) — there, the OS needed to check whether a sequence of priority increments was reachable from `0` under the `k-1`/`k`/`k+1` increment rule; here it's the bare stepping-stone version with no story attached. The approach is identical: a `HashMap<Integer, Set<Integer>>` from stone position to the set of jump sizes that could land there, built up in a single left-to-right pass.

## Solution

```java
import java.util.*;

class Solution {
    // Determines whether the frog can reach the last stone under the k-1/k/k+1 jump rule.
    public static boolean frogJump(int[] stones) {
        if (stones.length == 0) return false;
        if (stones.length == 1) return true;
        if (stones[1] != 1) return false; // first jump must always be 1.

        Map<Integer, Set<Integer>> reachedBy = new HashMap<>();
        for (int s : stones) reachedBy.put(s, new HashSet<>());
        reachedBy.get(0).add(0);

        for (int i = 0; i < stones.length; i++) {
            int cur = stones[i];
            for (int k : reachedBy.get(cur)) {
                for (int step = k - 1; step <= k + 1; step++) {
                    if (step > 0 && reachedBy.containsKey(cur + step)) {
                        reachedBy.get(cur + step).add(step);
                    }
                }
            }
        }
        return !reachedBy.get(stones[stones.length - 1]).isEmpty();
    }

    public static void main(String[] args) {
        System.out.println(frogJump(new int[]{0, 1, 3, 5, 6, 8, 12, 17}));
        // true
        System.out.println(frogJump(new int[]{0, 1, 2, 3, 7, 11, 18}));
        // false
    }
}
```

For every stone we've confirmed the frog can reach, we record every jump size that got it there. Standing on that stone, we try the three legal next jump sizes; whichever ones land exactly on another stone in the array get recorded against that stone. By the time we've processed every stone in order, the last stone's set of "jump sizes that reached it" tells us whether it's reachable at all.

## Complexity measures

Let **n** be the number of stones.

- **Time:** `O(n²)` — each stone may be reached by up to `O(n)` distinct jump sizes, each spawning 3 constant-time checks.
- **Space:** `O(n²)` — in the worst case, each stone's set of reaching jump sizes grows to `O(n)`.

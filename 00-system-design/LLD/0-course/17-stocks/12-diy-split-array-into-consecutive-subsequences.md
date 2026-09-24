# DIY: Split Array into Consecutive Subsequences

## Problem statement

Given an array sorted in ascending order, determine if it's possible to split the array into one or more subsequences such that each subsequence consists of consecutive integers and has a length of at least 3.

### Input

```java
{1, 2, 3, 3, 4, 4, 5, 5}
```

### Output

```java
true
```

## Coding exercise

Implement `isPossible(arr)`.

The closest match in this chapter is [Feature #3: Goals Fulfilled](03-feature-3-goals-fulfilled.md) — the two problems are identical. "Goals Fulfilled" frames it as figuring out whether a shared intern log could plausibly split into per-intern runs of at least 3 trades each; strip away the story and it's exactly this: can a sorted array be partitioned into consecutive runs of length ≥ 3? Same greedy two-map solution applies unchanged.

## Solution

```java
import java.util.*;

class Solution {
    // Returns true if the sorted `arr` can be split entirely into
    // subsequences of 3+ consecutive integers.
    public static boolean isPossible(int[] arr) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        for (int n : arr) {
            frequencyMap.merge(n, 1, Integer::sum);
        }

        Map<Integer, Integer> needsNext = new HashMap<>();

        for (int n : arr) {
            if (frequencyMap.getOrDefault(n, 0) == 0) {
                continue; // Already claimed by an earlier subsequence.
            }
            if (needsNext.getOrDefault(n, 0) > 0) {
                // Extend an existing subsequence that was waiting for n.
                needsNext.merge(n, -1, Integer::sum);
                needsNext.merge(n + 1, 1, Integer::sum);
            } else if (frequencyMap.getOrDefault(n + 1, 0) > 0
                    && frequencyMap.getOrDefault(n + 2, 0) > 0) {
                // Start a brand-new subsequence n, n+1, n+2.
                frequencyMap.merge(n + 1, -1, Integer::sum);
                frequencyMap.merge(n + 2, -1, Integer::sum);
                needsNext.merge(n + 3, 1, Integer::sum);
            } else {
                return false; // n can't extend or start any valid run.
            }
            frequencyMap.merge(n, -1, Integer::sum);
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(isPossible(new int[]{1, 2, 3, 3, 4, 4, 5, 5}));
        // true
    }
}
```

Tracing the example `{1, 2, 3, 3, 4, 4, 5, 5}`: the first `1` starts a new run `1,2,3` (consuming one `2` and one `3`, and marking that this run now wants a `4` next). The next `2` and `3` are already consumed, so they're skipped. The second `3` has nothing waiting for it and no `4,5` pair free to start fresh (only one of each remains after the first run's consumption) — but it does have `needsNext[3]`? No: the run that started at `1` wants `4` next, not `3`, so the second `3` starts its *own* new run `3,4,5`, consuming the remaining `4` and `5`. Both runs (`1,2,3` and `3,4,5`) complete cleanly, so the answer is `true`.

## Complexity measures

Let **n** be the length of `arr`.

### Time Complexity

`O(n)` — each element triggers a constant number of hash-map operations.

### Space Complexity

`O(n)` — `frequencyMap` and `needsNext` together hold at most one entry per distinct value in `arr`.

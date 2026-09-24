# DIY: Divide Array in Sets of K Consecutive Numbers

## Problem statement

You're given an array of integers `nums` and a positive integer `k`. Determine whether it's possible to divide the array's elements into groups of `k` consecutive numbers.

### Input

```java
// nums = {3, 2, 1, 2, 3, 4, 3, 4, 5, 9, 10, 11}, k = 3
```

### Output

```java
true
```

## Coding exercise

Implement `isDivisionPossible(nums, k)`.

This is the exact same problem as [Feature #1: Hand of Straights](01-feature-1-hand-of-straights.md), just stripped of the poker framing — instead of a hand of playing cards, it's a bare array of integers, but "split everything into groups of `k` consecutive values" is precisely the same requirement. The greedy-from-the-smallest-remaining-value algorithm carries over unchanged.

## Solution

```java
import java.util.*;

class Solution {
    public static boolean isDivisionPossible(int[] nums, int k) {
        if (nums.length % k != 0) {
            return false;
        }

        TreeMap<Integer, Integer> count = new TreeMap<>();
        for (int n : nums) {
            count.merge(n, 1, Integer::sum);
        }

        while (!count.isEmpty()) {
            int first = count.firstKey();
            for (int v = first; v < first + k; v++) {
                Integer occurrences = count.get(v);
                if (occurrences == null) {
                    return false;
                }
                if (occurrences == 1) {
                    count.remove(v);
                } else {
                    count.put(v, occurrences - 1);
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {
        int[] nums = {3, 2, 1, 2, 3, 4, 3, 4, 5, 9, 10, 11};
        System.out.println(isDivisionPossible(nums, 3));
        // true
    }
}
```

Just like Feature #1, we count each value's occurrences in a sorted map, then repeatedly peel off a group of `k` consecutive values starting from whatever value is currently smallest — since that value can only ever belong to the run starting at itself. Sorted, `nums` is `1,2,2,3,3,3,4,4,5,9,10,11`, which splits cleanly into `{1,2,3}`, `{2,3,4}`, `{3,4,5}`, and `{9,10,11}` — four groups of three, so the answer is `true`.

## Complexity measures

Let **n** be the length of `nums`.

- **Time:** `O(n log n + n × k)` — the sorted map costs `O(n log n)` to build, and the grouping loop does `O(n × k / k) = O(n)` map lookups at `O(log n)` each in the worst case, giving `O(n log n + n × k)` overall.
- **Space:** `O(n)` — the frequency map holds at most one entry per distinct value in `nums`.

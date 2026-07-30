# DIY: Random Pick Index

## Problem statement

You are given an array of integers that may contain duplicates. Given a `target` value known to exist in the array, return a uniformly random index among all the indices holding that value. The solution should stay memory-efficient even for very large arrays.

### Input

```java
numbers = {1, 2, 2, 3, 3, 3, 4}
target = 3
```

### Output

```java
4
```

(Any of indices 3, 4, or 5 — the positions holding `3` — is a valid answer, each equally likely.)

## Coding exercise

Implement the `Solution` class: its constructor takes the `numbers` array, and its `pick(target)` method returns a randomly chosen index of `target`.

Use reservoir sampling: scan the array once, and each time `target` is seen, replace the current answer with probability `1 / (count seen so far)` — this yields a uniform pick using only `O(1)` extra space, with no need to pre-store every matching index. This pattern doesn't map onto one specific Amazon feature from this chapter, but it's the same "pick fairly among candidates without extra bookkeeping" idea that shows up whenever a system needs to sample uniformly from a stream it can't fully store.

## Solution

```java
import java.util.*;

class Solution {
    private final int[] nums;
    private final Random random = new Random();

    public Solution(int[] nums) {
        this.nums = nums;
    }

    // Reservoir sampling: O(1) extra space, one pass per pick() call.
    public int pick(int target) {
        int count = 0;
        int result = -1;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == target) {
                count++;
                // Replace the running answer with probability 1/count,
                // which leaves every matching index equally likely overall.
                if (random.nextInt(count) == 0) {
                    result = i;
                }
            }
        }
        return result;
    }

    public static void main(String[] args) {
        int[] numbers = {1, 2, 2, 3, 3, 3, 4};
        Solution sol = new Solution(numbers);
        System.out.println(sol.pick(3));
        // 3, 4, or 5 (indices holding the value 3) — e.g. 4
    }
}
```

## Complexity measures

Let **n** be the number of elements in `nums`.

- **Time:** `O(n)` per `pick()` call — reservoir sampling requires one full scan since matching indices aren't pre-stored.
- **Space:** `O(1)` extra — no index lists are kept; only the input array and a couple of counters.

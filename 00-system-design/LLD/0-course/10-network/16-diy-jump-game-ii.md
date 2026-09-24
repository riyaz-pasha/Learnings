# DIY: Jump Game II

## Problem statement

Given an array of non-negative integers `nums`, you start at the first index. Each element represents your maximum jump length from that position. Return the minimum number of jumps needed to reach the last index (it's guaranteed to be reachable).

### Input

```java
nums = {4, 1, 1, 3, 1, 1, 1}
```

### Output

```
2
```

Jump from index 0 (reach up to 4) straight to index 3, then from index 3 (reach up to 3 more) straight to the last index.

## Coding exercise

Implement `jump(nums)`, returning the minimum number of jumps to reach the last index.

This is the exact same pattern as [Feature #3: Minimum Hops](03-feature-3-minimum-hops.md) — there, each router's forward reach determined the fewest transmissions to relay a packet down a chain; here it's the bare pattern, no networking story. Greedily track the furthest index reachable with the jumps committed so far, and the furthest index reachable if we use one more jump, incrementing the jump count only when we've exhausted the current jump's range.

## Solution

```java
class Solution {
    public static int jump(int[] nums) {
        if (nums.length < 2) {
            return 0;
        }

        int maxReach = nums[0];
        int currReach = nums[0];
        int jumps = 1;

        for (int i = 1; i < nums.length; i++) {
            if (currReach < i) {
                jumps++;
                currReach = maxReach;
            }
            maxReach = Math.max(maxReach, nums[i] + i);
        }
        return jumps;
    }

    public static void main(String[] args) {
        int[] nums = {4, 1, 1, 3, 1, 1, 1};
        System.out.println(jump(nums));
        // 2
    }
}
```

## Complexity measures

Let **n** be the length of `nums`.

- **Time:** `O(n)` — a single pass over the array.
- **Space:** `O(1)` — only a few scalar variables are tracked.

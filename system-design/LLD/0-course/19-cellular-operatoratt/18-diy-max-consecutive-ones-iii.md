# DIY: Max Consecutive Ones III

## Problem statement

Given a binary array `nums` and an integer `k`, return the maximum number of consecutive `1`s in the array if you can flip at most `k` `0`s.

### Input

```java
// nums = [0,0,1,1,0,0,1,1,1,0,1,1,0,0,0,1,1,1,1]
// k = 3
```

### Output

```java
// 10
```

## Coding exercise

Implement the `longestOnes(nums, k)` function, where `nums` is the binary array and `k` is the number of allowed flips. The function returns an integer representing the maximum number of consecutive `1`s achievable.

This is exactly [Feature #8: Maximum Signal Strength](08-feature-8-maximum-signal-strength.md) — the same bounded sliding window over zeros, just renamed away from the "signal repeaters in a mall aisle" framing to a plain binary array with flips.

## Solution

```java
class Solution {
    public static int longestOnes(int[] nums, int k) {
        int left = 0;
        int zeros = 0;
        int maxLen = 0;

        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) {
                zeros++;
            }
            while (zeros > k) {
                if (nums[left] == 0) {
                    zeros--;
                }
                left++;
            }
            maxLen = Math.max(maxLen, right - left + 1);
        }
        return maxLen;
    }

    public static void main(String[] args) {
        int[] nums = {0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1};
        System.out.println(longestOnes(nums, 3)); // 10
    }
}
```

## Solution walkthrough

Tracing the sliding window directly (confirmed by running the code above): as `right` sweeps forward, the window keeps expanding until its zero count would exceed `3`, at which point `left` advances just enough to drop back to `3` zeros. The winning window turns out to be indices `2` through `11`: `nums[2..11] = [1,1,0,0,1,1,1,0,1,1]`, which has length `10` and contains exactly `3` zeros (at indices 4, 5, and 9) — flip those 3 zeros and the whole stretch becomes consecutive `1`s. No later window in the array beats this length of `10`, matching the expected output.

## Complexity measures

Let **n** be the length of `nums`.

### Time Complexity

`O(n)` — `right` sweeps the array once, and `left` only ever moves forward, so total pointer movement is linear.

### Space Complexity

`O(1)` — only a constant number of counters and pointers are used.

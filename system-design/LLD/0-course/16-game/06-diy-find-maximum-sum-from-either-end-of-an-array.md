# DIY: Find Maximum Sum from Either End of an Array

## Problem statement

You're given an array of integers `nums` and a positive integer `k` (with `k` always less than or equal to `nums.length`). Find the maximum sum obtainable from `k` elements of the array — but you can't pick them at random. You must remove elements one at a time from either the left or right end of the array.

### Input

```java
// nums = {5, 2, 3, 4, 1, 6, 1}, k = 3
```

### Output

```java
12
```

(Achieved by picking `5` from the left end, then `1` and `6` from the right end.)

## Coding exercise

Implement `maxSum(nums, k)`.

This is the same problem as [Feature #2: Maximum Points You Can Obtain from Cards](02-feature-2-maximum-points-you-can-obtain-from-cards.md), just without the Fizzle card-game framing. "Pick `k` elements one at a time from either end" is exactly the same constraint as before, so the same sliding-window-from-the-right trick applies directly.

## Solution

```java
class Solution {
    public static int maxSum(int[] nums, int k) {
        int n = nums.length;

        int windowSum = 0;
        for (int i = n - k; i < n; i++) {
            windowSum += nums[i]; // Start by taking all k elements from the right.
        }

        int best = windowSum;
        int left = 0;
        int right = n - k;
        for (int i = 0; i < k; i++) {
            windowSum += nums[left] - nums[right]; // Move one pick from right to left.
            left++;
            right++;
            best = Math.max(best, windowSum);
        }
        return best;
    }

    public static void main(String[] args) {
        int[] nums = {5, 2, 3, 4, 1, 6, 1};
        System.out.println(maxSum(nums, 3));
        // 12
    }
}
```

Whatever `k` elements we *don't* pick always form one contiguous block sitting somewhere in the middle of the array, so instead of enumerating left/right splits directly, we slide a window of size `k` from the right end of the array to the left end one step at a time, tracking its running sum. Each window position corresponds to one valid split (some count from the left, the rest from the right), and the best sum any window achieves is the answer. Here that's `12`, from taking `5` off the left and then `1, 6` off the right.

## Complexity measures

Let **k** be the number of elements picked.

- **Time:** `O(k)` — building the initial window sum costs `O(k)`, and the window then slides `k` more times, each an `O(1)` update.
- **Space:** `O(1)` — only a constant number of running variables are used.

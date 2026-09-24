# DIY: 3Sum Closest

## Problem statement

You are given an integer array `nums` and an integer `target`. Find the three numbers in `nums` whose sum is closest to `target`, and return that sum.

### Constraints

- `3 <= nums.length <= 1000`
- `-1000 <= nums[i] <= 1000`
- `-10^4 <= target <= 10^4`

### Input

```java
nums = {-2, 3, 2, -4}
target = 2
```

### Output

```java
1
```

(`-4 + 3 + 2 = 1`, which is the closest achievable sum to `target = 2`.)

## Coding exercise

Implement `threeSumClosest(nums, target)`, returning the sum of the three numbers closest to `target`.

This is the same three-sum family as [Feature #2: Suggest Items for Special Offer](02-feature-2-suggest-items-for-special-offer.md) — but with a twist: instead of matching an exact target exactly, you're hunting for the *closest* achievable sum. Sort the array, fix one number, then slide two pointers inward from the ends, tracking whichever sum has come nearest to `target` so far.

## Solution

```java
import java.util.*;

class Solution {
    public static int threeSumClosest(int[] nums, int target) {
        int[] arr = nums.clone();
        Arrays.sort(arr);
        int n = arr.length;

        int closest = arr[0] + arr[1] + arr[2];

        for (int i = 0; i < n - 2; i++) {
            int left = i + 1, right = n - 1;
            while (left < right) {
                int sum = arr[i] + arr[left] + arr[right];

                if (Math.abs(sum - target) < Math.abs(closest - target)) {
                    closest = sum;
                }

                if (sum == target) {
                    return sum; // can't get any closer than an exact match
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }

        return closest;
    }

    public static void main(String[] args) {
        int[] nums = {-2, 3, 2, -4};
        System.out.println(threeSumClosest(nums, 2));
        // 1
    }
}
```

## Complexity measures

Let **n** be the number of elements in `nums`.

- **Time:** `O(n²)` — sorting is `O(n log n)`, then each of the n anchors runs an `O(n)` two-pointer scan.
- **Space:** `O(log n)` to `O(n)` — space used by the sort; no other auxiliary storage grows with input size.

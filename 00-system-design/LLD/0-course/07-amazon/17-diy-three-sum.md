# DIY: Three Sum

## Problem statement

You are given a list `numbers` containing positive and negative integers. Find all unique triplets `(a, b, c)` in the list that add up to zero: `a + b + c = 0`.

### Input

```java
numbers = {3, 0, 6, 2, 5, -8, -1}
```

### Output

```java
{{-8, 2, 6}, {-8, 3, 5}}
```

(Each triplet sums to zero; duplicate triplets are not repeated.)

## Coding exercise

Implement `threeSum(numbers)`, returning the list of triplets that sum to zero.

This is the exact same pattern as [Feature #2: Suggest Items for Special Offer](02-feature-2-suggest-items-for-special-offer.md) — there, Amazon looked for three items whose prices matched an offer amount; here it's the bare pattern with no story attached. Sort the array first, then fix one number and slide two pointers inward from the ends of the remaining range, skipping duplicates as you go.

## Solution

```java
import java.util.*;

class Solution {
    public static int[][] threeSum(int[] numbers) {
        int[] nums = numbers.clone();
        Arrays.sort(nums);

        List<int[]> result = new ArrayList<>();
        int n = nums.length;

        for (int i = 0; i < n - 2; i++) {
            // Skip duplicate anchors to avoid duplicate triplets.
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            int left = i + 1, right = n - 1;
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                if (sum == 0) {
                    result.add(new int[]{nums[i], nums[left], nums[right]});
                    left++;
                    right--;
                    while (left < right && nums[left] == nums[left - 1]) left++;
                    while (left < right && nums[right] == nums[right + 1]) right--;
                } else if (sum < 0) {
                    left++; // sum too small, grow it
                } else {
                    right--; // sum too big, shrink it
                }
            }
        }

        return result.toArray(new int[0][]);
    }

    public static void main(String[] args) {
        int[] numbers = {3, 0, 6, 2, 5, -8, -1};
        int[][] result = threeSum(numbers);
        for (int[] t : result) {
            System.out.println(Arrays.toString(t));
        }
        // [-8, 2, 6]
        // [-8, 3, 5]
    }
}
```

## Complexity measures

Let **n** be the number of elements in `numbers`.

- **Time:** `O(n²)` — sorting is `O(n log n)`, then each of the n anchors runs an `O(n)` two-pointer scan.
- **Space:** `O(log n)` to `O(n)` — space used by the sort; the output list is not counted as auxiliary space.

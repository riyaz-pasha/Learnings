# DIY: Next Permutation

## Problem statement

Given a list of numbers, rearrange them into the next lexicographically greater permutation. If no such arrangement is possible, rearrange it into the lowest possible order (i.e., sort it in ascending order).

Note: the replacement must be done in place, using only constant extra memory.

### Input

```java
[4, 5, 2, 6, 7, 3, 1]
```

### Output

```java
[4, 5, 2, 7, 1, 3, 6]
```

## Coding exercise

Implement the `nextPermutation(nums)` function, where `nums` is the list of numbers to rearrange. It returns the list rearranged into the next lexicographically greater permutation.

This is exactly [Feature #5: Mutating a Virus](05-feature-5-mutating-a-virus.md) — same pivot-and-reverse technique, applied to a plain array instead of a virus's nucleotide sequence.

## Solution

```java
class Solution {
    public static int[] nextPermutation(int[] nums) {
        int index = nums.length - 2;
        while (index >= 0 && nums[index] >= nums[index + 1]) {
            index--;
        }

        if (index >= 0) {
            int j = nums.length - 1;
            while (nums[j] <= nums[index]) {
                j--;
            }
            int tmp = nums[index];
            nums[index] = nums[j];
            nums[j] = tmp;
        }

        reverse(nums, index + 1, nums.length - 1);
        return nums;
    }

    private static void reverse(int[] nums, int left, int right) {
        while (left < right) {
            int tmp = nums[left];
            nums[left] = nums[right];
            nums[right] = tmp;
            left++;
            right--;
        }
    }

    public static void main(String[] args) {
        int[] result = nextPermutation(new int[]{4, 5, 2, 6, 7, 3, 1});
        java.util.Arrays.stream(result).forEach(n -> System.out.print(n + " "));
        System.out.println(); // 4 5 2 7 1 3 6
    }
}
```

Tracing `[4, 5, 2, 6, 7, 3, 1]`: scanning from the right, `nums[3] = 6 < nums[4] = 7`, so `index = 3` is the pivot (everything after it, `[7, 3, 1]`, is descending). Scanning from the right for the first value greater than `6`, we land on `7` at position 4. Swapping positions 3 and 4 gives `[4, 5, 2, 7, 6, 3, 1]`. The suffix after the pivot, `[6, 3, 1]`, is still descending, so reversing it gives `[1, 3, 6]`. Final result: `[4, 5, 2, 7, 1, 3, 6]`.

## Complexity measures

Let **n** be the length of the array.

### Time Complexity

`O(n)` — finding the pivot, finding the swap target, and reversing the suffix each take at most one pass over the array.

### Space Complexity

`O(1)` — the rearrangement is done in place.

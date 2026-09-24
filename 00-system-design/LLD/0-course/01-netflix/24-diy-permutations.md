# DIY: Permutations

## Problem statement

Given an array `nums` of unique integers, return all possible permutations, in any order.

**Constraints:** `1 <= nums.length <= 6`, `-10 <= nums[i] <= 10`, all integers unique.

### Input

```java
[1, 2, 3]
```

### Output

```java
[[1,2,3], [1,3,2], [2,1,3], [2,3,1], [3,1,2], [3,2,1]]
```

## Coding exercise

Implement `permute(nums)`.

Identical pattern to [Feature #11: Generate Movie Viewing Orders](11-feature-11-generate-movie-viewing-orders.md) — swap-based backtracking, fixing one position at a time.

## Solution

```java
import java.util.*;

class Solution {
    public static List<List<Integer>> permute(int[] nums) {
        List<Integer> numsList = new ArrayList<>();
        for (int n : nums) numsList.add(n);

        List<List<Integer>> output = new ArrayList<>();
        backtrack(0, numsList.size(), numsList, output);
        return output;
    }

    private static void backtrack(int first, int size, List<Integer> nums, List<List<Integer>> output) {
        if (first == size) {
            output.add(new ArrayList<>(nums));
            return;
        }

        for (int i = first; i < size; i++) {
            Collections.swap(nums, first, i);
            backtrack(first + 1, size, nums, output);
            Collections.swap(nums, first, i); // undo
        }
    }

    public static void main(String[] args) {
        System.out.println(permute(new int[]{1, 2, 3}));
        // [[1,2,3], [1,3,2], [2,1,3], [2,3,1], [3,1,2], [3,2,1]]
    }
}
```

## Complexity measures

Let **n** be `nums.length`.

- **Time:** `O(n!)` — that many distinct permutations exist, each produced exactly once.
- **Space:** `O(n)` for the recursion stack (excluding the `O(n! × n)` needed to store the output).

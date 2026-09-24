# DIY: Two Sum

## Problem statement

You are given a list of integers `numbers` and an integer `target`. Find the indices of the two numbers that add up to `target`.

You cannot use the same element twice — if `target / 2` sits at index `i`, then `[i, i]` is not a valid answer. Assume exactly one solution exists. The order of the two indices in the output does not matter.

### Input

```java
numbers = {83, 97, 25}
target = 108
```

### Output

```java
{0, 2}
```

(`numbers[0] + numbers[2] = 83 + 25 = 108`.)

## Coding exercise

Implement `twoSum(numbers, target)`, returning the indices of the pair that sums to `target`.

This is the exact same pattern as [Feature #1: Suggest Items for Free Delivery](01-feature-1-suggest-items-for-free-delivery.md) — there, Amazon looked for two items whose prices summed to the free-delivery threshold; here it's the bare pattern with no story attached. Walk the list once, hashing each number's complement so a matching pair resolves in a single pass.

## Solution

```java
import java.util.*;

class Solution {
    public static int[] twoSum(int[] numbers, int target) {
        // Map from a number we've already seen to its index.
        Map<Integer, Integer> seen = new HashMap<>();

        for (int i = 0; i < numbers.length; i++) {
            int need = target - numbers[i];
            if (seen.containsKey(need)) {
                return new int[]{seen.get(need), i};
            }
            seen.put(numbers[i], i);
        }

        return new int[]{-1, -1}; // no valid pair (won't happen per problem guarantee)
    }

    public static void main(String[] args) {
        int[] numbers = {83, 97, 25};
        System.out.println(Arrays.toString(twoSum(numbers, 108)));
        // [0, 2]
    }
}
```

## Complexity measures

Let **n** be the number of elements in `numbers`.

- **Time:** `O(n)` — a single pass, with `O(1)` hash-map lookups and inserts.
- **Space:** `O(n)` — the hash map holds up to n seen numbers.

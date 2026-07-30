# DIY: Validate the Binary Search Tree

## Problem statement

You're given an integer array. Implement `inOrderBST(array, n)` to check whether the array represents a valid in-order traversal of a binary search tree — that is, whether it's sorted in strictly increasing order.

### Constraints

- Number of nodes is in the range `[1, 10^4]`.
- `-2^31 <= Node.value <= 2^31 - 1`.

### Input

```java
array = {8, 12, 15, 21, 24, 32, 45}
```

### Output

```
Valid BST
```

(If the array weren't sorted, the output would be `Not valid BST`.)

## Coding exercise

Implement `inOrderBST(array, n)`, where `array` is the in-order traversal to validate and `n` is its length.

This is the exact same pattern as [Feature #4: Validate Sorted Participants Data](04-feature-4-validate-sorted-participants-data.md) — there, Zoom checked participant names for alphabetical order after a network transfer; here it's the bare pattern over integers instead of strings. Walk the array once, and the moment any element isn't strictly greater than the one before it, the sequence can't be a valid BST's in-order traversal.

## Solution

```java
class Solution {
    public static boolean inOrderBST(int[] array, int n) {
        if (n == 0 || n == 1) {
            return true; // trivially sorted
        }
        for (int i = 1; i < n; i++) {
            if (array[i - 1] >= array[i]) {
                return false; // found a non-increasing pair
            }
        }
        return true;
    }

    public static void main(String[] args) {
        int[] array = {8, 12, 15, 21, 24, 32, 45};
        System.out.println(inOrderBST(array, array.length) ? "Valid BST" : "Not valid BST");
        // Valid BST
    }
}
```

## Complexity measures

Let **n** be the length of the array.

- **Time:** `O(n)` — a single linear scan comparing each element to the one before it.
- **Space:** `O(1)` — the check happens in place with no extra structures.

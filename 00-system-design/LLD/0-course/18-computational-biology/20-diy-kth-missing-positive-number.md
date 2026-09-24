# DIY: Kth Missing Positive Number

## Problem statement

You are given an array `A` of positive integers only and an integer `k`. The array is sorted in strictly increasing order. Find the `k`th positive integer missing from this array.

**Constraints:** `1 <= A[i] <= 1000`; `1 <= k <= 1000`; `A[i] <= A[j]` for `1 <= i < j <= A.length`.

### Input

```java
// Sample Input 1:
A = [1, 2, 3, 4]
k = 2

// Sample Input 2:
A = [2, 3, 5, 9, 10]
k = 4

// Sample Input 3:
A = [1, 97, 101, 654, 798, 989, 1000]
k = 100
```

### Output

```java
// Sample Output 1:
6

// Sample Output 2:
7

// Sample Output 3:
103
```

## Coding exercise

Implement the `findKthPositive(A, k)` function, where `A` is an integer array of positive numbers and `k` is an integer. The function returns the `k`th positive integer missing from `A`.

This is exactly [Feature #9: Kth Missing Gene](09-feature-9-kth-missing-gene.md) — same binary search over the "how many are missing before this position" function, just without the DNA framing.

## Solution

```java
class Solution {
    public static int findKthPositive(int[] A, int k) {
        int left = 0;
        int right = A.length - 1;

        while (left <= right) {
            int pivot = (left + right) / 2;
            if (A[pivot] - pivot - 1 < k) {
                left = pivot + 1;
            } else {
                right = pivot - 1;
            }
        }
        return left + k;
    }

    public static void main(String[] args) {
        System.out.println(findKthPositive(new int[]{1, 2, 3, 4}, 2));                          // 6
        System.out.println(findKthPositive(new int[]{2, 3, 5, 9, 10}, 4));                       // 7
        System.out.println(findKthPositive(new int[]{1, 97, 101, 654, 798, 989, 1000}, 100));    // 103
    }
}
```

Tracing `A = [2, 3, 5, 9, 10]`, `k = 4`: the full missing sequence relative to the positive integers is `1, 4, 6, 7, 8, 11, ...` — `1` is missing before `2`, `4` is missing before `5`, and `6, 7, 8` are missing before `9`. The binary search homes in on the boundary where the "missing count so far" first reaches `4`, and `left + k` evaluates to `7` — the 4th value in that missing sequence, confirming the expected output.

## Complexity measures

Let **n** be the number of elements in `A`.

### Time Complexity

`O(log n)` — binary search halves the search space each step.

### Space Complexity

`O(1)` — only a constant number of index variables are used.

# DIY: Longest Consecutive Sequence

## Problem statement

Given an unsorted array of integers, find the length of the longest run of consecutive integers.

### Input

```java
{9, 1, 4, 7, 3, -1, 0, 5, 8, -1, 6}
```

### Output

```java
7
```

(`[3, 4, 5, 6, 7, 8, 9]` — 7 consecutive integers.)

## Coding exercise

Implement `LCS(arr)`.

The exact algorithm from [Feature #7: Longest Busy Period](07-feature-7-longest-busy-period.md) — a HashSet, extending forward only from genuine sequence starts (where `n - 1` isn't in the set).

## Solution

```java
import java.util.HashSet;
import java.util.Set;

class Solution {
    public static int LCS(int[] arr) {
        Set<Integer> numbers = new HashSet<>();
        for (int n : arr) {
            numbers.add(n);
        }

        int longest = 0;

        for (int n : numbers) {
            if (numbers.contains(n - 1)) {
                continue;
            }

            int current = n;
            int length = 1;
            while (numbers.contains(current + 1)) {
                current++;
                length++;
            }

            longest = Math.max(longest, length);
        }

        return longest;
    }

    public static void main(String[] args) {
        int[] arr = {9, 1, 4, 7, 3, -1, 0, 5, 8, -1, 6};
        System.out.println(LCS(arr)); // 7
    }
}
```

## Complexity measures

Let **n** be the size of `arr`.

- **Time:** `O(n)`.
- **Space:** `O(n)`.

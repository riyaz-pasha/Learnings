# DIY: Queue Reconstruction by Height

## Problem statement

Given an array `people`, where `people[i] = [hi, ki]` means the `i`th person has height `hi` and there are `ki` people standing in front of them in the queue whose height is greater than or equal to `hi`, reconstruct and return the queue.

### Input

```java
people = [[6, 0], [5, 0], [4, 0], [3, 2], [2, 2], [1, 4]]
```

### Output

```java
[[4, 0], [5, 0], [2, 2], [3, 2], [1, 4], [6, 0]]
```

## Coding exercise

Implement `reconstructQueue(people)`.

This is the exact same pattern as [Feature #15: Queue Reconstruction by Priority](15-feature-15-queue-reconstruction-by-priority.md) — there, the OS needed to reconstruct a process queue from priority and "count of higher-or-equal-priority processes ahead" pairs; here it's the bare version with height instead of priority. The approach is identical: sort by height descending (ties broken by `k` ascending), then insert each person at index `k` in the growing output list.

## Solution

```java
import java.util.*;

class Solution {
    public static int[][] reconstructQueue(int[][] people) {
        Arrays.sort(people, (a, b) -> a[0] == b[0] ? a[1] - b[1] : b[0] - a[0]);

        List<int[]> result = new ArrayList<>();
        for (int[] p : people) {
            result.add(p[1], p);
        }
        return result.toArray(new int[result.size()][]);
    }

    public static void main(String[] args) {
        int[][] people = {{6, 0}, {5, 0}, {4, 0}, {3, 2}, {2, 2}, {1, 4}};
        for (int[] p : reconstructQueue(people)) {
            System.out.print(Arrays.toString(p) + " ");
        }
        // [4, 0] [5, 0] [2, 2] [3, 2] [1, 4] [6, 0]
    }
}
```

Placing taller people first guarantees their final position is never disturbed by anyone shorter inserted afterward — a shorter person's `k` only counts people at least as tall, so inserting them anywhere doesn't change the relative order among taller people already placed. By the time we insert `[hi, ki]`, every taller (or equally tall, earlier-tied) person is already in the list, so inserting at index `ki` is always correct.

## Complexity measures

Let **n** be the number of people.

- **Time:** `O(n²)` — sorting is `O(n log n)`, but inserting at an arbitrary `ArrayList` index shifts later elements, making the total insertion cost `O(n²)` in the worst case.
- **Space:** `O(n)` — the sorted array and result list.

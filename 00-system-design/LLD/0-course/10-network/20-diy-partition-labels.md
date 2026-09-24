# DIY: Partition Labels

## Problem statement

Given a string `s` of lowercase English letters, partition it into as many parts as possible so that each letter appears in at most one part. Return an array of integers representing the size of each part, in order.

### Input

```java
s = "caedbdedda"
```

### Output

```
{1, 9}
```

The partitions are `"c"` and `"aedbdedda"`.

## Coding exercise

Implement `partitionLabels(s)`, returning the sizes of the partitions.

This is the exact same pattern as [Feature #7: Divide Files Over the Network](07-feature-7-divide-files-over-the-network.md) — there, a file string was split across worker nodes so no file crossed a boundary; here it's the bare pattern, no networking story. Track each letter's last occurrence, then extend the current chunk's boundary to the furthest last-occurrence seen so far, closing the chunk once the scan catches up to that boundary.

## Solution

```java
import java.util.*;

class Solution {
    public static List<Integer> partitionLabels(String s) {
        int[] lastOccurrence = new int[26];
        for (int i = 0; i < s.length(); i++) {
            lastOccurrence[s.charAt(i) - 'a'] = i;
        }

        List<Integer> partitions = new ArrayList<>();
        int start = 0, end = 0;

        for (int i = 0; i < s.length(); i++) {
            end = Math.max(end, lastOccurrence[s.charAt(i) - 'a']);
            if (i == end) {
                partitions.add(end - start + 1);
                start = i + 1;
            }
        }
        return partitions;
    }

    public static void main(String[] args) {
        System.out.println(partitionLabels("caedbdedda"));
        // [1, 9]
    }
}
```

## Complexity measures

Let **n** be the length of `s`.

- **Time:** `O(n)` — one pass to compute last occurrences, one more to cut partitions.
- **Space:** `O(1)` — the `lastOccurrence` array is fixed at 26 entries regardless of input length (the output list doesn't count toward extra space).

# DIY: Reorganizing a String

## Problem statement

Given a string, rearrange it so no two adjacent letters are identical. Return any valid rearrangement, or an empty string if none exists.

### Input

```java
str = "abaacdda"
```

### Output

```java
"abacadad"   // one possible valid rearrangement
```

## Coding exercise

Implement `reorganize(str)`.

This is exactly the algorithm from [Feature #6: Reorganizing Search Results](06-feature-6-reorganizing-search-results.md) — greedy max-heap placement of the most frequent remaining character, with a one-step cooldown. The only difference: on failure this returns `""` instead of the original string.

## Solution

```java
import java.util.*;

class Solution {

    public static String reorganize(String str) {
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : str.toCharArray()) {
            freqMap.merge(c, 1, Integer::sum);
        }

        int n = str.length();
        int maxFreq = freqMap.values().stream().max(Integer::compareTo).orElse(0);
        if (maxFreq > (n + 1) / 2) {
            return "";
        }

        PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> b[1] - a[1]);
        for (Map.Entry<Character, Integer> entry : freqMap.entrySet()) {
            maxHeap.offer(new int[]{entry.getKey(), entry.getValue()});
        }

        StringBuilder result = new StringBuilder();
        int[] prev = null;

        while (!maxHeap.isEmpty()) {
            int[] current = maxHeap.poll();
            result.append((char) current[0]);
            current[1]--;

            if (prev != null && prev[1] > 0) {
                maxHeap.offer(prev);
            }
            prev = current;
        }

        return result.toString();
    }

    public static void main(String[] args) {
        System.out.println(reorganize("abaacdda")); // a valid rearrangement, e.g. "adabacad"
        System.out.println(reorganize("aaab"));      // "" (impossible)
    }
}
```

## Complexity measures

Let **n** be the string's length and **k** the number of distinct characters.

- **Time:** `O(n log k)`.
- **Space:** `O(k)`.

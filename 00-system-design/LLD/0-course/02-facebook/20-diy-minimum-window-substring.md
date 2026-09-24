# DIY: Minimum Window Substring

## Problem statement

Given strings `S` and `T`, find the smallest substring of `S` that contains every character of `T` (including duplicate counts). Order doesn't matter. Return `""` if no such window exists.

### Input

```java
// Example 1
S = "ABAACBBA"
T = "ABC"

// Example 2
S = "ABAACBAB"
T = "ABCC"
```

### Output

```java
// Example 1
"ACB"

// Example 2
""
```

(Example 2 has no valid window — `S` only contains one `'C'`, but `T` needs two.)

## Coding exercise

Implement `minWindow(S, T)`.

This is the exact algorithm underlying [Feature #8: Overlapping Topics](08-feature-8-overlapping-topics.md), operating on characters directly instead of an array of topic strings — the classic **Minimum Window Substring** problem.

## Solution

```java
import java.util.HashMap;
import java.util.Map;

class Solution {

    public static String minWindow(String s, String t) {
        if (s.isEmpty() || t.isEmpty()) {
            return "";
        }

        Map<Character, Integer> required = new HashMap<>();
        for (char c : t.toCharArray()) {
            required.merge(c, 1, Integer::sum);
        }

        Map<Character, Integer> windowCounts = new HashMap<>();
        int formed = 0;
        int distinctRequired = required.size();

        int left = 0;
        int bestLen = Integer.MAX_VALUE;
        int bestLeft = 0;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            windowCounts.merge(c, 1, Integer::sum);
            if (required.containsKey(c) && windowCounts.get(c).intValue() == required.get(c).intValue()) {
                formed++;
            }

            while (formed == distinctRequired) {
                if (right - left + 1 < bestLen) {
                    bestLen = right - left + 1;
                    bestLeft = left;
                }

                char leftChar = s.charAt(left);
                windowCounts.put(leftChar, windowCounts.get(leftChar) - 1);
                if (required.containsKey(leftChar) && windowCounts.get(leftChar) < required.get(leftChar)) {
                    formed--;
                }
                left++;
            }
        }

        return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestLeft, bestLeft + bestLen);
    }

    public static void main(String[] args) {
        System.out.println(minWindow("ABAACBBA", "ABC"));  // ACB
        System.out.println(minWindow("ABAACBAB", "ABCC")); // ""
    }
}
```

## Complexity measures

Let **n** be the length of `S` and **m** the length of `T`.

- **Time:** `O(n + m)` — building `required` is `O(m)`; `left` and `right` each traverse `S` at most once.
- **Space:** `O(m)` for the character-count maps (bounded by the alphabet size in practice).

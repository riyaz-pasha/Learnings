# DIY: Longest Substring with At Most K Distinct Characters

## Problem statement

Given a string, find the length of the longest substring `T` that contains at most `k` distinct characters.

### Input

```java
s = "cdaba", k = 3
```

### Output

```java
4
```

The substring `"daba"` is the longest string in the above input with at most 3 distinct characters.

## Coding exercise

Implement the `lengthOfLongestSubstringKDistinct(s, k)` function, where `s` is the string and `k` is the integer. The function returns the length of the longest substring that has at most `k` distinct characters.

This is exactly [Feature #2: Detect Virus](02-feature-2-detect-virus.md) — same sliding-window-with-a-hash-map technique, just returning the *length* of the window here instead of the substring itself.

## Solution

```java
import java.util.*;

class Solution {
    public static int lengthOfLongestSubstringKDistinct(String s, int k) {
        if (s.length() == 0 || k == 0) {
            return 0;
        }

        Map<Character, Integer> lastPos = new HashMap<>();
        int left = 0, longest = 0;

        for (int right = 0; right < s.length(); right++) {
            lastPos.put(s.charAt(right), right);

            if (lastPos.size() > k) {
                int minPos = Collections.min(lastPos.values());
                lastPos.values().remove(minPos);
                left = minPos + 1;
            }

            longest = Math.max(longest, right - left + 1);
        }
        return longest;
    }

    public static void main(String[] args) {
        System.out.println(lengthOfLongestSubstringKDistinct("cdaba", 3)); // 4
    }
}
```

Tracing `"cdaba"`, `k = 3`: the window grows to `c`, `cd`, `cda` (3 distinct characters, at the limit) — adding the next `b` would make 4 distinct characters, so the map evicts whichever character has the smallest last-seen position (`c`, at position 0), and `left` jumps to 1. The window is now `daba` (positions 1-4), which holds exactly 3 distinct characters (`d`, `a`, `b`) and has length 4 — the longest seen, so `4` is returned.

## Complexity measures

Let **n** be the length of the string and **k** the distinct-character limit.

### Time Complexity

`O(n)` best case, `O(n·k)` worst case — each window shrink can scan up to `k` map entries to find the minimum position.

### Space Complexity

`O(k)` — the map holds at most `k + 1` entries at any point.

# DIY: Longest Substring without Repeating Characters

## Problem statement

Given a string `str`, find the longest substring without repeating characters, and return its length as well.

Note: if the input string is empty, return its length, which is 0.

### Input

```java
// Sample Input 1:
"abcabcbb"

// Sample Input 2:
""
```

### Output

The output is a list of strings containing the longest substring (if any) and its length, converted to a string.

```java
// Sample Output 1:
["abc", "3"]

// Sample Output 2:
["0"]
```

## Coding exercise

Implement the `findLongestSubstring(str)` function, where `str` is the input string. The function returns a list of strings containing the longest substring without repeating characters and its length, formatted as shown above.

This is exactly [Feature #6: Identify a Species](06-feature-6-identify-a-species.md) — same sliding-window-with-last-seen-positions technique; the only difference is the output format (this version also reports the length, and returns `["0"]` for an empty input instead of an empty string).

## Solution

```java
import java.util.*;

class Solution {
    public static List<String> findLongestSubstring(String str) {
        List<String> result = new ArrayList<>();
        if (str.isEmpty()) {
            result.add("0");
            return result;
        }

        Map<Character, Integer> lastSeen = new HashMap<>();
        int stCurr = 0, longest = 0, start = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (lastSeen.containsKey(c) && lastSeen.get(c) >= stCurr) {
                stCurr = lastSeen.get(c) + 1;
            }
            lastSeen.put(c, i);

            int currLen = i - stCurr + 1;
            if (currLen > longest) {
                longest = currLen;
                start = stCurr;
            }
        }

        result.add(str.substring(start, start + longest));
        result.add(String.valueOf(longest));
        return result;
    }

    public static void main(String[] args) {
        System.out.println(findLongestSubstring("abcabcbb")); // [abc, 3]
        System.out.println(findLongestSubstring(""));          // [0]
    }
}
```

Tracing `"abcabcbb"`: the window grows to `"abc"` (length 3) before the second `a` at index 3 forces `stCurr` to jump to 1 (past the first `a`'s position 0); the window `"bca"` still has length 3, not longer than the recorded best. The pattern continues — no window ever exceeds length 3 — so `"abc"` (the first one found) and `3` are returned.

## Complexity measures

Let **n** be the length of the string.

### Time Complexity

`O(n)` — each character is visited exactly once.

### Space Complexity

`O(min(n, k))` where `k` is the size of the character set — the map holds at most one entry per distinct character seen.

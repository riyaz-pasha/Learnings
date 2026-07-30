# DIY: Longest Palindromic Substring

## Problem statement

Given a string `s`, return the longest palindromic substring in `s`.

### Input

```java
s = "bccd"
```

### Output

```java
"cc"
```

## Coding exercise

Implement the `longestPalindrome(s)` function, where `s` is the string. The function returns the longest palindromic substring from `s`.

This is exactly [Feature #3: Locate Protein](03-feature-3-locate-protein.md) — same expand-around-center technique. It's also the sibling of [DIY: Palindromic Substrings](18-diy-palindromic-substrings.md), which uses the identical center-expansion loop but *counts* every palindromic substring found instead of tracking only the longest.

## Solution

```java
class Solution {
    private static int expand(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return right - left - 1;
    }

    public static String longestPalindrome(String s) {
        if (s == null || s.length() < 1) {
            return "";
        }
        int start = 0, end = 0;
        for (int i = 0; i < s.length(); i++) {
            int len1 = expand(s, i, i);
            int len2 = expand(s, i, i + 1);
            int len = Math.max(len1, len2);
            if (len > end - start + 1) {
                start = i - (len - 1) / 2;
                end = i + len / 2;
            }
        }
        return s.substring(start, end + 1);
    }

    public static void main(String[] args) {
        System.out.println(longestPalindrome("bccd")); // cc
    }
}
```

Tracing `"bccd"`: at `i = 0` (`b`), both expansions give length 1. At `i = 1` (`c`), the odd expansion gives 1, but the even expansion `expand(s, 1, 2)` compares `s[1] = 'c'` and `s[2] = 'c'` — they match, giving length 2, and expanding further would compare `s[0] = 'b'` with `s[3] = 'd'`, which don't match, so it stops at length 2. That beats the previous best (length 1), so `start = 1`, `end = 2` — `"cc"`. No later center beats it, so `"cc"` is the final answer.

## Complexity measures

Let **n** be the length of the string.

### Time Complexity

`O(n²)` — `O(n)` candidate centers, each expansion up to `O(n)` in the worst case.

### Space Complexity

`O(1)` — only a few index variables are used.

# DIY: Palindromic Substrings

## Problem statement

Given a string `s`, implement an algorithm that returns the number of palindromic substrings in it.

Note: a string is a palindrome when it reads the same backward as forward, and a substring is a contiguous sequence of characters within the string.

Consider the string `"abb"`. Its substrings are `"a"`, `"b"`, `"b"`, and `"bb"`. All four are palindromes, so the algorithm should return `4`.

**Constraints:** `1 <= s.length <= 1000`; `s` consists of lowercase English letters.

### Input

```java
// Input 1:
"abcd"

// Input 2:
"aabb"
```

### Output

```java
// Output 1:
4

// Output 2:
6
```

## Coding exercise

Implement the `countSubstrings(s)` function, where `s` is a string. The function returns an integer representing the number of palindromic substrings.

There's no feature in this chapter that counts palindromic substrings directly, but this problem is a close cousin of [DIY: Longest Palindromic Substring](13-diy-longest-palindromic-substring.md), which maps to [Feature #3: Locate Protein](03-feature-3-locate-protein.md). Both use the exact same expand-around-center loop over all `2n - 1` centers — the only difference is what we do with each expansion's result: Feature #3 keeps the *longest* one found, while this problem *counts every* palindrome found along the way (every successful expansion step is itself a palindromic substring).

## Solution

```java
class Solution {
    public static int countSubstrings(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            count += countFrom(s, i, i);     // odd-length palindromes centered at i
            count += countFrom(s, i, i + 1); // even-length palindromes centered between i, i+1
        }
        return count;
    }

    private static int countFrom(String s, int left, int right) {
        int count = 0;
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            count++;
            left--;
            right++;
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println(countSubstrings("abcd")); // 4
        System.out.println(countSubstrings("aabb")); // 6
    }
}
```

Tracing `"aabb"`: each single character contributes 4 one-letter palindromes (`a`, `a`, `b`, `b`). The center between index 0 and 1 (`"aa"`) matches once, contributing 1; the center between index 2 and 3 (`"bb"`) also matches once, contributing 1. The center between index 1 and 2 (`a` vs. `b`) doesn't match at all. Total: `4 + 1 + 1 = 6`, matching the expected output.

## Complexity measures

Let **n** be the length of the string.

### Time Complexity

`O(n²)` — `O(n)` candidate centers, each expansion up to `O(n)` in the worst case.

### Space Complexity

`O(1)` — only a running count and a few index variables are used.

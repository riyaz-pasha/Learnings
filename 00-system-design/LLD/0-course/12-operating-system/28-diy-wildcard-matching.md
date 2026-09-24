# DIY: Wildcard Matching

## Problem statement

Given an input string `s` and a pattern `p`, implement wildcard pattern matching supporting:

- `?` — matches any single character.
- `*` — matches any sequence of characters, including an empty one.

The match must cover the entire input string.

### Input

```java
String s = "aa";
String p = "a?";
```

### Output

```java
true
```

## Coding exercise

Implement `isMatch(s, p)`.

This is a close sibling of [Feature #9: File Search](09-feature-9-file-search.md) — both use a bottom-up 2D DP table over two strings, but the wildcard here behaves differently from regex `*`: instead of repeating the single character before it, `*` matches **any sequence** (of any length, including zero) on its own, more like a shell glob pattern (`ls *.txt`) than a regex.

## Solution

```java
class Solution {
    // Full-string wildcard match: '?' matches one char, '*' matches any sequence (incl. empty).
    public static boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;

        // A leading run of '*' can match the empty string.
        for (int j = 1; j <= n; j++) {
            dp[0][j] = dp[0][j - 1] && p.charAt(j - 1) == '*';
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char pc = p.charAt(j - 1);
                if (pc == '*') {
                    // Either skip the '*' (matches empty), or let it also absorb s[i-1].
                    dp[i][j] = dp[i - 1][j] || dp[i][j - 1];
                } else if (pc == '?' || pc == s.charAt(i - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
            }
        }
        return dp[m][n];
    }

    public static void main(String[] args) {
        System.out.println(isMatch("aa", "a?"));
        // true
        System.out.println(isMatch("adceb", "*a*b"));
        // true
        System.out.println(isMatch("acdcb", "a*c?b"));
        // false
    }
}
```

The key difference from regex matching is the `*` transition: since `*` can absorb any sequence, `dp[i][j] = dp[i-1][j] || dp[i][j-1]` says "either this `*` already matched everything up through `s[i-1]` and we just extend it to also cover `s[i-1]` (`dp[i-1][j]`), or the `*` matches nothing more and we fall back to whatever matched before it in the pattern (`dp[i][j-1]`)." A `?` or literal character match is the same one-to-one step as regular expression matching: `dp[i][j] = dp[i-1][j-1]`.

## Complexity measures

Let **m** be the length of `s` and **n** be the length of `p`.

- **Time:** `O(m × n)` — every DP cell is filled in constant time.
- **Space:** `O(m × n)` — the DP table.

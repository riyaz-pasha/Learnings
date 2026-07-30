# DIY: Regular Expression Matching

## Problem statement

Given an input string `s` and a pattern `p`, implement regular expression matching supporting:

- `.` — matches any single character.
- `*` — matches zero or more of the preceding character.

The match must cover the entire input string, not just part of it.

### Input

```java
String s = "aa";
String p = "a*";
```

### Output

```java
true
```

## Coding exercise

Implement `regex(s, p)`.

This is the exact same pattern as [Feature #9: File Search](09-feature-9-file-search.md) — there, the OS needed to find file names matching a regex pattern; here it's the bare full-string matching function with no story attached. The approach is identical: a bottom-up 2D DP table where `dp[i][j]` means "does `s[0..i)` match `p[0..j)`", filled using the standard character-match and `*`-as-zero-or-more-repetitions rules.

## Solution

```java
class Solution {
    // Full-string regex match supporting '.' and '*'.
    public static boolean regex(String s, String p) {
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;

        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*' && j >= 2) {
                dp[0][j] = dp[0][j - 2];
            }
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char sc = s.charAt(i - 1);
                char pc = p.charAt(j - 1);
                if (pc == sc || pc == '.') {
                    dp[i][j] = dp[i - 1][j - 1];
                } else if (pc == '*') {
                    char prev = p.charAt(j - 2);
                    dp[i][j] = dp[i][j - 2]; // zero occurrences.
                    if (prev == sc || prev == '.') {
                        dp[i][j] = dp[i][j] || dp[i - 1][j]; // one more occurrence.
                    }
                }
            }
        }
        return dp[m][n];
    }

    public static void main(String[] args) {
        System.out.println(regex("aa", "a*"));
        // true
        System.out.println(regex("mississippi", "mis*is*p*."));
        // false
    }
}
```

`dp[i][j]` builds on smaller subproblems: a plain character (or `.`) match carries forward `dp[i-1][j-1]`; a `*` can either consume zero occurrences of the character it repeats (fall back to `dp[i][j-2]`) or, if that character matches the current position in `s`, consume one more occurrence and fall back to `dp[i-1][j]`.

## Complexity measures

Let **m** be the length of `s` and **n** be the length of `p`.

- **Time:** `O(m × n)` — every DP cell is filled in constant time.
- **Space:** `O(m × n)` — the DP table.

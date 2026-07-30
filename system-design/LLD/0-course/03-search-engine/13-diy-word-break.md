# DIY: Word Break

## Problem statement

Given a non-empty string `s` and a list of unique strings `subs`, determine if `s` can be broken into a space-separated sequence of one or more strings from `subs`. Words from `subs` can be reused.

### Input

```java
s = "magically"
subs = {"ag", "al", "icl", "mag", "magic", "ly", "lly"}
```

### Output

```java
true
```

(`"magic" + "al" + "ly" = "magically"`)

## Coding exercise

Implement `stringBreak(s, subs)`.

Exactly [Feature #3: Add White Spaces to Create Words](03-feature-3-add-white-spaces-to-create-words.md) — bottom-up DP where `dp[i]` tracks whether `s[0..i)` can be fully split into dictionary words.

## Solution

```java
import java.util.*;

class Solution {
    public static boolean stringBreak(String s, String[] subs) {
        Set<String> dictSet = new HashSet<>(Arrays.asList(subs));

        boolean[] dp = new boolean[s.length() + 1];
        dp[0] = true;

        for (int i = 1; i <= s.length(); i++) {
            for (int j = 0; j < i; j++) {
                if (dp[j] && dictSet.contains(s.substring(j, i))) {
                    dp[i] = true;
                    break;
                }
            }
        }

        return dp[s.length()];
    }

    public static void main(String[] args) {
        String[] subs = {"ag", "al", "icl", "mag", "magic", "ly", "lly"};
        System.out.println(stringBreak("magically", subs)); // true
        System.out.println(stringBreak("magically!", subs)); // false
    }
}
```

## Complexity measures

Let **n** be the length of `s`.

- **Time:** `O(n³)`.
- **Space:** `O(n)`.

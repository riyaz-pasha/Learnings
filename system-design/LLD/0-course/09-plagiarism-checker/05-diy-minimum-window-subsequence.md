# DIY: Minimum Window Subsequence

## Problem statement

Given strings `S` and `T`, find the minimum (contiguous) substring `W` of `S` such that `T` is a subsequence of `W`.

If no window in `S` covers all characters of `T` as a subsequence, return an empty string `""`. If there are multiple minimum-length windows, return the one with the left-most starting index.

### Input

```java
S = "abcdebdde"
T = "bde"
```

### Output

```java
"bcde"
```

Both `"bcde"` and `"bdde"` are minimum-length windows containing `T` as a subsequence, but `"bcde"` starts earlier, so it's the answer.

## Coding exercise

Implement `minWindow(S, T)`, returning the smallest substring of `S` that contains `T` as a subsequence.

This is the exact same problem as [Feature #2: Return Match](02-feature-2-return-match.md) — there, we found the tightest window of a suspected cheater's tokens that still contained a copied student's tokens; here it's the bare pattern with no story attached. Scan forward to find where a subsequence match completes, then shrink backward from that point to find the tightest possible start, keeping the smallest window seen so far.

## Solution

```java
class Solution {

    public static String minWindow(String S, String T) {
        String window = "";
        int j = 0;
        int min = S.length() + 1;

        for (int i = 0; i < S.length(); i++) {
            if (S.charAt(i) == T.charAt(j)) {
                j++;
                if (j == T.length()) {
                    // Just completed a forward match ending at i. Shrink it from the back.
                    int end = i + 1;
                    j--;
                    while (j >= 0) {
                        if (S.charAt(i) == T.charAt(j)) {
                            j--;
                        }
                        i--;
                    }
                    j++;
                    i++;
                    if (end - i < min) {
                        min = end - i;
                        window = S.substring(i, end);
                    }
                }
            }
        }
        return window;
    }

    public static void main(String[] args) {
        String S = "abcdebdde";
        String T = "bde";

        System.out.println(minWindow(S, T));
        // bcde
    }
}
```

## Complexity measures

Let **s** be the length of `S` and **t** be the length of `T`.

- **Time:** `O(s * t)` — in the worst case, every position in `S` can trigger a full backward shrink pass over up to `t` characters.
- **Space:** `O(1)` — beyond the returned substring, only a fixed handful of index variables are used.

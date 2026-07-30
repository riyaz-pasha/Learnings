# DIY: Decode Ways

## Problem statement

A message containing letters from `A-Z` can be encoded into a string of numbers using the mapping:

```
'A' -> "1"
'B' -> "2"
...
'Z' -> "26"
```

To decode an encoded message, all the digits must be grouped and then mapped back into letters, using the reverse of the mapping above (there may be multiple ways of doing this). For example, `"11203"` can be mapped into:

- `"AATC"`, with the grouping `(1 1 20 3)`
- `"KTC"`, with the grouping `(11 20 3)`

Note that the grouping `(1 12 03)` is invalid because `"03"` cannot be mapped into `"C"`, since `"3"` is different from `"03"`.

Given a string `str` that contains only digits, return the number of ways it can be decoded.

**Note:** `str` contains only digits and may contain leading zero(s).

### Input

```java
// Sample Input 1
"12"

// Sample Input 2
"0"

// Sample Input 3
"226"

// Sample Input 4
"08"
```

### Output

```java
// Sample Output 1
2

// Sample Output 2
0

// Sample Output 3
3

// Sample Output 4
0
```

## Coding exercise

Implement `numDecodings(str)`.

This is the exact same pattern as [Feature #4: Ways to Decode Message](04-feature-4-ways-to-decode-message.md) — there, a cryptanalysis tool had to count how many plaintexts a numeric ciphertext could decode to under a simple `A=1, ..., Z=26` cipher; here it's the bare counting problem with no story attached. At each index, recurse on consuming either a 1-digit or a valid (`10`–`26`) 2-digit code, and memoize on the starting index to avoid recomputing overlapping sub-problems.

## Solution

```java
import java.util.*;

class Solution {
    public static int numDecodings(String str) {
        if (str == null || str.isEmpty()) return 0;
        int[] memo = new int[str.length() + 1];
        Arrays.fill(memo, -1);
        return decode(str, 0, memo);
    }

    private static int decode(String s, int index, int[] memo) {
        if (index == s.length()) {
            return 1;
        }
        if (s.charAt(index) == '0') {
            return 0;
        }
        if (memo[index] != -1) {
            return memo[index];
        }

        int ways = decode(s, index + 1, memo);

        if (index + 1 < s.length()) {
            int twoDigit = Integer.parseInt(s.substring(index, index + 2));
            if (twoDigit <= 26) {
                ways += decode(s, index + 2, memo);
            }
        }

        memo[index] = ways;
        return ways;
    }

    public static void main(String[] args) {
        System.out.println(numDecodings("12"));
        // 2
        System.out.println(numDecodings("0"));
        // 0
        System.out.println(numDecodings("226"));
        // 3
        System.out.println(numDecodings("08"));
        // 0
    }
}
```

The recursion at index `i` either consumes one digit as a code (invalid if it's `'0'`, since no code starts with `0`) or two digits together, but only when that 2-digit value is `10`–`26`. Reaching the end of the string counts as one complete valid decoding. Since "how many ways to decode from index `i`" gets asked repeatedly along different paths, we cache each index's answer in `memo` the first time it's computed.

## Complexity measures

Let **n** be the length of `str`.

- **Time:** `O(n)` — memoization guarantees each index is computed exactly once, with `O(1)` work beyond its cached recursive calls.
- **Space:** `O(n)` — the memo array holds `n` entries, and the recursion stack can go `n` deep (e.g., a string of all `1`s or `2`s).

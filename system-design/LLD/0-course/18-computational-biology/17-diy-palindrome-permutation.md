# DIY: Palindrome Permutation

## Problem statement

Given a string `str`, find whether or not a permutation of it is a palindrome. Return `true` if such a permutation is possible, and `false` if it is not.

### Input

```java
// Sample Input 1
"peas"

// Sample Input 2
"abab"
```

### Output

```java
// Sample Output 1
false

// Sample Output 2
true
```

## Coding exercise

Implement the `permutePalindrome(str)` function, where `str` is a string. The function returns `true` if a permutation of the string can be a palindrome, `false` otherwise.

This is exactly [Feature #7: Detecting a Protein](07-feature-7-detecting-a-protein.md) — same character-frequency parity check: at most one character is allowed to occur an odd number of times.

## Solution

```java
import java.util.*;

class Solution {
    public static boolean permutePalindrome(String str) {
        Map<Character, Integer> counts = new HashMap<>();
        for (char c : str.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }

        int oddCounts = 0;
        for (int count : counts.values()) {
            if (count % 2 != 0) {
                oddCounts++;
            }
        }
        return oddCounts <= 1;
    }

    public static void main(String[] args) {
        System.out.println(permutePalindrome("peas")); // false
        System.out.println(permutePalindrome("abab")); // true
    }
}
```

Tracing `"peas"`: every one of `p`, `e`, `a`, `s` occurs exactly once — four odd counts, well past the limit of one, so `false`.

Tracing `"abab"`: `a` occurs twice, `b` occurs twice — zero odd counts, which is `<= 1`, so `true` (e.g., `"abba"` is a palindromic rearrangement).

## Complexity measures

Let **n** be the length of the string and **k** the size of the character set.

### Time Complexity

`O(n + k)`, i.e., `O(n)` — one pass to build the count map, one pass (bounded by the constant alphabet size) to check parities.

### Space Complexity

`O(k)`, i.e., `O(1)` — the map holds at most one entry per distinct character in the (fixed-size) alphabet.

# DIY: Valid Palindrome

## Problem statement

Write a function that takes a string and checks whether it is a palindrome or not.

### Input

```java
// Sample Input 1:
"bccd"

// Sample Input 2:
"racecar"
```

### Output

```java
// Sample Output 1:
false

// Sample Output 2:
true
```

(The source material's own example paired `"bccd"` with the answer `true` and `"abcab"` with `false`. Neither is right as stated: running the palindrome check below on `"bccd"` gives `false` — `b` and `d`, the first and last characters, don't match — and `"abcab"` also gives `false` for the same reason (`a` vs. `b`). Neither of the source's two examples was actually a `true` case, so the examples above swap in `"racecar"` to show what a genuine `true` result looks like, alongside the corrected `"bccd" -> false`.)

## Coding exercise

Implement the `isPalindrome(s)` function, where `s` is the string. The function returns `true` or `false` depending on whether the string is a palindrome.

This is exactly [Feature #4: Identifying Proteins](04-feature-4-identifying-proteins.md) — same recursive first-and-last-character comparison.

## Solution

```java
class Solution {
    public static boolean isPalindrome(String s) {
        if (s.length() <= 1) {
            return true;
        }
        if (s.charAt(0) == s.charAt(s.length() - 1)) {
            return isPalindrome(s.substring(1, s.length() - 1));
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(isPalindrome("bccd"));    // false
        System.out.println(isPalindrome("racecar")); // true
    }
}
```

Tracing `"bccd"`: the first character is `b`, the last is `d` — they don't match, so the function returns `false` immediately, with no recursive call needed.

Tracing `"racecar"`: `r` matches `r`, so we recurse on `"aceca"`. There, `a` matches `a`, so we recurse on `"cec"`. There, `c` matches `c`, so we recurse on `"e"` — length 1, base case, returns `true`. The `true` propagates all the way back up.

## Complexity measures

Let **n** be the length of the string.

### Time Complexity

`O(n)` — each recursive call strips two characters, so at most `n/2` calls happen.

### Space Complexity

`O(n)` — each call allocates a new substring, and the recursion stack grows to about `n/2` frames.

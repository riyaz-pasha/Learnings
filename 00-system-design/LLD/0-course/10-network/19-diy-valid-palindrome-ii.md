# DIY: Valid Palindrome II

## Problem statement

Given a string `s`, return `true` if it can be made a palindrome by deleting at most one character.

### Input

```java
s = "dead"
```

### Output

```
true
```

We can delete either the `e` or the `a` to make the remaining string a palindrome.

## Coding exercise

Implement `validPalindrome(s)`, returning whether `s` can become a palindrome by removing at most one character.

This is the exact same pattern as [Feature #6: Transmission Error](06-feature-6-transmission-error.md) — there, a request/response round trip was allowed at most one diverging router; here it's the bare pattern, no networking story. Walk two pointers inward from both ends; the moment they disagree, check whether skipping the left character or skipping the right character restores a palindrome for what's left.

## Solution

```java
class Solution {
    public static boolean validPalindrome(String s) {
        int left = 0, right = s.length() - 1;

        while (left < right) {
            if (s.charAt(left) != s.charAt(right)) {
                return isPalindrome(s, left + 1, right) || isPalindrome(s, left, right - 1);
            }
            left++;
            right--;
        }
        return true;
    }

    private static boolean isPalindrome(String s, int left, int right) {
        while (left < right) {
            if (s.charAt(left) != s.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(validPalindrome("dead"));
        // true
    }
}
```

## Complexity measures

Let **n** be the length of `s`.

- **Time:** `O(n)` — the two-pointer scan and each of the at-most-two extra palindrome checks together still only look at each character a constant number of times.
- **Space:** `O(1)` — only pointers are used, no extra data structure grows with the input.

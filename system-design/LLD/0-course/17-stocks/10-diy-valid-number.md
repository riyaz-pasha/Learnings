# DIY: Valid Number

## Problem statement

Validate if a given string can be interpreted as a decimal number or not.

A valid decimal number may contain:

- Digits — `0-9`
- An exponent marker — `"e"` (or `"E"`)
- A positive or negative sign — `"+"`/`"-"`
- A decimal point — `"."`

Context matters: a sign is only legal at the very start of the number or immediately after the exponent marker; there can be at most one decimal point and it can't appear after the exponent marker; and the exponent marker (if present) must be both preceded and followed by at least one digit.

```
"0"          => true
" 0.1 "      => true
".55"        => true
"abc"        => false
"1 a"        => false
" -90e3   "  => true
" 1e"        => false
"e3"         => false
" 6e-1"      => true
" 99e2.5 "   => false
" --6 "      => false
```

### Input

```java
s = "53.5e93"
```

### Output

```java
true
```

## Coding exercise

Implement `isNumberValid(s)`.

The closest match in this chapter is [Feature #1: Validate Price](01-feature-1-validate-price.md) — both are string-validation problems solved by tracking "what have we legally seen so far" as we scan left to right. Validate Price is the simpler cousin (mandatory sign, no exponent, no leading whitespace to strip); this exercise adds the full generality — optional sign, optional leading/trailing whitespace, and an optional exponent part — but the core idea of rejecting the moment we see something that doesn't fit the current position is the same.

## Solution

```java
class Solution {
    // Returns true if s (after trimming whitespace) is a valid decimal
    // number, optionally signed, optionally with a fractional part, and
    // optionally followed by an exponent (e.g. "1e", "-90e3", ".55", "6e-1").
    public static boolean isNumberValid(String s) {
        s = s.trim();
        if (s.isEmpty()) {
            return false;
        }

        boolean seenDigit = false, seenDot = false, seenExp = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                seenDigit = true;
            } else if (c == '+' || c == '-') {
                // A sign is only legal at the very start, or right after 'e'/'E'.
                if (i > 0 && s.charAt(i - 1) != 'e' && s.charAt(i - 1) != 'E') {
                    return false;
                }
            } else if (c == '.') {
                if (seenDot || seenExp) {
                    return false; // At most one dot, and never after the exponent.
                }
                seenDot = true;
            } else if (c == 'e' || c == 'E') {
                if (seenExp || !seenDigit) {
                    return false; // At most one exponent, and it needs digits before it.
                }
                seenExp = true;
                seenDigit = false; // Reset: now require at least one digit after 'e'.
            } else {
                return false; // Any other character makes the string invalid.
            }
        }
        return seenDigit; // Must end having seen a digit (not mid-exponent with none yet).
    }

    public static void main(String[] args) {
        String[] tests = {
            "0", " 0.1 ", ".55", "abc", "1 a", " -90e3   ",
            " 1e", "e3", " 6e-1", " 99e2.5 ", " --6 ", "53.5e93"
        };
        for (String s : tests) {
            System.out.println("\"" + s + "\" -> " + isNumberValid(s));
        }
        // "0" -> true
        // " 0.1 " -> true
        // ".55" -> true
        // "abc" -> false
        // "1 a" -> false
        // " -90e3   " -> true
        // " 1e" -> false
        // "e3" -> false
        // " 6e-1" -> true
        // " 99e2.5 " -> false
        // " --6 " -> false
        // "53.5e93" -> true
    }
}
```

The three booleans (`seenDigit`, `seenDot`, `seenExp`) act as a compact state machine: a sign is only valid at a state boundary (start, or right after `e`), a dot can't appear twice or after the exponent has begun, and the exponent itself both requires a digit before it and resets the digit tracker so a bare `"1e"` (no digit following `e`) gets caught at the end.

## Complexity measures

Let **n** be the length of the input string.

### Time Complexity

`O(n)` — the trimmed string is scanned exactly once.

### Space Complexity

`O(1)` — only a few boolean flags are used regardless of input length (the `trim()` call does allocate a new string of length up to `n`, but that's the only non-constant use, and it's still linear rather than adding a new order of growth).

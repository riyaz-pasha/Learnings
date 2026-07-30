# DIY: Remove All Adjacent Duplicates In String

## Problem statement

Given a string `s` consisting of lowercase English letters, repeatedly remove adjacent duplicate letters — one *pair* at a time — until no adjacent pair of identical letters remains.

### Input

```java
// s = "abbaaca"
```

### Output

```java
"aca"
```

## Coding exercise

This is [Feature #3: Balloon Splash](03-feature-3-balloon-splash.md)'s exact stack algorithm, just fixed at `k = 2` instead of a general splash size — remove any run the instant it reaches length 2. Implement `removeDuplicates(s)`.

## Solution

Since `k` is fixed at 2, we don't even need to track run lengths explicitly — a plain character stack works: push each character, unless it matches the character already on top, in which case that's a completed pair, so pop instead of pushing.

```java
class Solution {
    public static String removeDuplicates(String s) {
        StringBuilder stack = new StringBuilder();
        for (char c : s.toCharArray()) {
            int top = stack.length() - 1;
            if (top >= 0 && stack.charAt(top) == c) {
                stack.deleteCharAt(top); // Completed pair — splash it.
            } else {
                stack.append(c);
            }
        }
        return stack.toString();
    }

    public static void main(String[] args) {
        System.out.println(removeDuplicates("abbaaca"));
        // aca
    }
}
```

Walking `"abbaaca"`: `a` pushes → `a`; `b` pushes → `ab`; the next `b` matches the top → pop → `a`; `a` matches the top → pop → `` (empty); `a` pushes → `a`; `c` pushes → `ac`; `a` pushes → `aca`. Nothing left to cancel, so the final result is `"aca"` — matching the expected output.

## Complexity measures

Let **n** be the length of `s`.

- **Time:** `O(n)` — each character is pushed once and popped at most once.
- **Space:** `O(n)` — the stack can hold up to all `n` characters in the worst case (no cancellations).

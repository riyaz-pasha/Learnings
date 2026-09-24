# DIY: Valid Parenthesis String

## Problem statement

Given a string `s` containing only `(`, `)`, and `*`, determine whether it's valid. `*` is a wildcard that can be treated as `(`, as `)`, or as an empty string. A string is valid if every `(` can be matched with a later `)` (using each `*` as at most one of the three interpretations that makes the string work).

### Input

```java
// Example 1
String s = "()";
// Example 2
String s = "(*)";
// Example 3
String s = "((*)";
// Example 4
String s = "(()";
```

### Output

```java
// Example 1
true
// Example 2
true
// Example 3
true
// Example 4
false
```

## Coding exercise

Implement `checkValidString(s)`.

This builds directly on [Feature #9: Validate Program Brackets](09-feature-9-validate-program-brackets.md) — same bracket-matching family, but a plain stack no longer works because a `*` doesn't commit to being an opener, closer, or nothing until we know what makes the rest of the string valid. Instead of tracking one exact count of "unmatched open parens," we track a **range** of possible counts.

## Solution

```java
class Solution {
    public static boolean checkValidString(String s) {
        int low = 0;  // fewest open parens possible so far (treating '*' as ')' or empty whenever it helps).
        int high = 0; // most open parens possible so far (treating '*' as '(' whenever it helps).

        for (char c : s.toCharArray()) {
            if (c == '(') {
                low++;
                high++;
            } else if (c == ')') {
                low--;
                high--;
            } else { // '*'
                low--;  // treat as ')' or empty: one fewer open paren than before.
                high++; // treat as '(': one more open paren than before.
            }

            if (high < 0) {
                return false; // even in the best case, we have an unmatched ')'.
            }
            if (low < 0) {
                low = 0; // can't have a negative count of open parens; clamp back to 0.
            }
        }
        return low == 0; // must be possible to end with exactly zero unmatched opens.
    }

    public static void main(String[] args) {
        System.out.println(checkValidString("()"));
        // true
        System.out.println(checkValidString("(*)"));
        // true
        System.out.println(checkValidString("((*)"));
        // true
        System.out.println(checkValidString("(()"));
        // false
    }
}
```

`low` and `high` track the minimum and maximum number of unmatched `(` that could be currently open, given every possible way of interpreting the `*` characters seen so far. A `(` increases both bounds; a `)` decreases both. A `*` widens the range — it *could* be a `)` (decrease), an empty string (no change), or a `(` (increase), so the worst case for `low` is treating it as a closer, and the best case for `high` is treating it as an opener.

If `high` ever dips below zero, then even the most generous interpretation of every `*` so far can't produce enough open parens to match the closers we've seen — the string is invalid, no matter what comes next. If `low` dips below zero, that just means the *least* generous interpretation over-closed; since we're free to reinterpret earlier `*` as empty instead, we clamp `low` back to `0` rather than failing. At the end, the string is valid only if `0` is within reach — i.e., `low == 0`, meaning some assignment of the `*` wildcards leaves no unmatched parens.

## Complexity measures

Let **n** be the length of `s`.

- **Time:** `O(n)` — a single left-to-right pass, tracking only two integers.
- **Space:** `O(1)` — no stack needed, since we only ever need the range of possible open-paren counts, not their exact positions.

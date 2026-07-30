# DIY: Minimum Remove to Make Valid Parentheses

## Problem statement

Given a string containing matched and unmatched parentheses (plus possibly other characters), remove the minimum number of parentheses so that the resulting string has only matching parentheses.

### Input

```java
str = "ab)cca(spo)(sc(s)("
```

### Output

```java
"abcca(spo)sc(s)"
```

## Coding exercise

Implement `minRemoveParentheses(s)`.

This is the exact same pattern as [Feature #5: Recover Files](05-feature-5-recover-files.md) — there, the OS needed to strip corrupted, unmatched start/end delimiters from a downloaded file; here it's the bare `(`/`)` version with no story attached. The approach is identical: use a stack to track unmatched parentheses by index, then rebuild the string skipping those indices.

## Solution

```java
import java.util.*;

class Solution {
    public static String minRemoveParentheses(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> toRemove = new HashSet<>();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                stack.push(i);
            } else if (c == ')') {
                if (!stack.isEmpty()) {
                    stack.pop(); // Matched - both indices are now safe.
                } else {
                    toRemove.add(i); // No opener to match this closer.
                }
            }
        }
        // Any '(' left on the stack never found a closer.
        while (!stack.isEmpty()) {
            toRemove.add(stack.pop());
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (!toRemove.contains(i)) {
                result.append(s.charAt(i));
            }
        }
        return result.toString();
    }

    public static void main(String[] args) {
        System.out.println(minRemoveParentheses("ab)cca(spo)(sc(s)("));
        // abcca(spo)sc(s)
    }
}
```

Scanning left to right, every `(` gets pushed onto the stack. Every `)` either matches the most recent unmatched `(` (pop it, both are fine) or has nothing to match (mark its own index for removal). Whatever `(` indices remain on the stack once the scan ends never found a partner, so they're marked for removal too. A final pass rebuilds the string, skipping every marked index — this time we can go left to right since we're working with a plain index set rather than reconstructing order from a stack.

## Complexity measures

Let **n** be the length of the string.

- **Time:** `O(n)` — one pass to find unmatched parentheses, one pass to build the result.
- **Space:** `O(n)` — in the worst case, every character is an unmatched `(`, filling the stack and removal set.

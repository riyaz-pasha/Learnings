# DIY: Valid Parentheses

## Problem statement

Given a string `s` containing only the characters `(`, `)`, `[`, `]`, `{`, and `}`, determine whether it's valid — every opening bracket must be closed by the same type of bracket, and brackets must close in the correct order.

### Input

```java
String s = "(){[{()}]}";
```

### Output

```java
true
```

## Coding exercise

Implement `isValid(s)`.

This is the exact same pattern as [Feature #9: Validate Program Brackets](09-feature-9-validate-program-brackets.md) — there, the compiler needed to verify bracket nesting in program source before compiling; here it's the bare bracket-matching pattern with no story attached, and no other characters to skip over (the input consists of brackets only). The stack-based approach transfers over unchanged.

## Solution

```java
import java.util.*;

class Solution {
    public static boolean isValid(String s) {
        Map<Character, Character> matches = new HashMap<>();
        matches.put(')', '(');
        matches.put(']', '[');
        matches.put('}', '{');

        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else {
                if (stack.isEmpty() || stack.pop() != matches.get(c)) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    public static void main(String[] args) {
        System.out.println(isValid("(){[{()}]}"));
        // true
        System.out.println(isValid("(]"));
        // false
    }
}
```

## Complexity measures

Let **n** be the length of `s`.

- **Time:** `O(n)` — a single left-to-right pass.
- **Space:** `O(n)` — the worst case (all opening brackets) pushes every character onto the stack.

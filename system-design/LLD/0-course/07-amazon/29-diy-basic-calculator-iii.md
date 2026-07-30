# DIY: Basic Calculator III

## Problem statement

You are given a string `s` representing a mathematical expression made up of non-negative integers, the operators `+`, `-`, `*`, `/`, and parentheses `(` `)`. Evaluate the expression and return its integer value. Integer division truncates toward zero. The expression is guaranteed to be valid.

### Input

```java
s = "3*(5+5)/2+(6/2)"
```

### Output

```java
18
```

(`3*(5+5)/2 = 3*10/2 = 15`, and `(6/2) = 3`, so `15 + 3 = 18`.)

## Coding exercise

Implement `calculate(s)`.

This is the same calculator family as [Feature #10: Calculate the Total Cost of the Shopping Cart Items](10-feature-10-calculate-the-total-cost-of-the-shopping-cart-items.md) — a harder variant that now has to handle nested parentheses. Reuse the stack-based left-to-right scan from Basic Calculator II, but whenever an opening parenthesis is hit, recurse into a fresh evaluation of the sub-expression inside it, treating the recursive call's return value as if it were a single number. A shared cursor position (tracked outside the recursive calls) lets each recursive call pick up scanning exactly where the nested one left off, including past the parenthesis's closing `)`.

## Solution

```java
import java.util.*;

class Solution {
    // Shared scan position across recursive calls, so a nested call can
    // "consume" characters and the caller resumes right after them.
    private static int pos = 0;

    public static int calculate(String s) {
        pos = 0;
        return evaluate(s);
    }

    private static int evaluate(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        int num = 0;
        char sign = '+';

        while (pos < s.length()) {
            char c = s.charAt(pos);
            pos++;

            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            }
            if (c == '(') {
                // Recurse to evaluate the parenthesized sub-expression; it
                // consumes up to and including its own closing ')'.
                num = evaluate(s);
            }
            if ((!Character.isDigit(c) && c != ' ') || pos == s.length()) {
                switch (sign) {
                    case '+': stack.push(num); break;
                    case '-': stack.push(-num); break;
                    case '*': stack.push(stack.pop() * num); break;
                    case '/': stack.push(stack.pop() / num); break;
                }
                sign = c;
                num = 0;
                if (c == ')') {
                    break; // this level is done; let the caller resume
                }
            }
        }

        int result = 0;
        for (int n : stack) {
            result += n;
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(calculate("3*(5+5)/2+(6/2)"));
        // 18
    }
}
```

## Complexity measures

Let **n** be the length of the string.

- **Time:** `O(n)` — every character is consumed exactly once across all recursive calls combined, since `pos` only ever advances.
- **Space:** `O(n)` — recursion depth is bounded by the nesting depth of parentheses, plus the per-level stack of pending `+`/`-` terms.

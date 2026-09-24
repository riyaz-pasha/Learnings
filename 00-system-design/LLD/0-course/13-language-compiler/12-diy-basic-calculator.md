# DIY: Basic Calculator

## Problem statement

Implement a basic calculator that evaluates a simple mathematical expression given as a string. The calculator must handle the `+` and `-` operators and `()` parentheses. The string can contain integers and spaces (which should be ignored).

### Input

```java
String expression = "((2 - 1) + (7 - 3))";
```

### Output

```java
5
```

## Coding exercise

Implement `calculate(expression)`.

This is the exact same pattern as [Feature #2: Evaluate the Arithmetic Expression](02-feature-2-evaluate-the-arithmetic-expression.md) — there, the compiler needed to fold a constant expression during compilation; here it's the bare pattern with no story attached, plus the added wrinkle of ignoring spaces (which the stack-based approach handles for free, since spaces simply don't match any of the branches).

## Solution

```java
import java.util.*;

class Solution {
    public static int calculate(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        int result = 0;
        int number = 0;
        int sign = 1;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                number = number * 10 + (c - '0');
            } else if (c == '+') {
                result += sign * number;
                number = 0;
                sign = 1;
            } else if (c == '-') {
                result += sign * number;
                number = 0;
                sign = -1;
            } else if (c == '(') {
                stack.push(result);
                stack.push(sign);
                result = 0;
                sign = 1;
            } else if (c == ')') {
                result += sign * number;
                number = 0;
                result *= stack.pop(); // sign in front of this parenthesis.
                result += stack.pop(); // running total before this parenthesis.
            }
            // spaces fall through every branch above and are simply ignored.
        }
        return result + sign * number;
    }

    public static void main(String[] args) {
        System.out.println(calculate("((2 - 1) + (7 - 3))"));
        // 5
    }
}
```

The walkthrough is identical to Feature #2: `-` negates the operand to its right rather than being treated as binary subtraction, which makes every operator effectively addition and lets the parenthesis-boundary bookkeeping (pushing/popping the outer `result` and `sign`) work cleanly regardless of nesting depth.

## Complexity measures

Let **n** be the length of the expression string.

- **Time:** `O(n)` — a single left-to-right scan with constant work per character.
- **Space:** `O(n)` — the stack can grow to hold one running total and sign per level of parenthesis nesting.

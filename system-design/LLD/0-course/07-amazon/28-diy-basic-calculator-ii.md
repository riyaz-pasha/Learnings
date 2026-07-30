# DIY: Basic Calculator II

## Problem statement

You are given a string `s` representing a mathematical expression made up of non-negative integers and the operators `+`, `-`, `*`, and `/`, separated by any number of spaces. Evaluate the expression and return its integer value. Integer division truncates toward zero. You may not use a built-in expression evaluator. There are no parentheses.

### Input

```java
s = "2+3/5"
```

```java
s = "6- 7*3"
```

### Output

```java
2
```

```java
-15
```

(`3/5` truncates to `0`, so `2+3/5 = 2`. For the second example, `*` binds tighter than `-`, so `6 - 7*3 = 6 - 21 = -15`.)

## Coding exercise

Implement `calculate(s)`.

This is the exact same pattern as [Feature #10: Calculate the Total Cost of the Shopping Cart Items](10-feature-10-calculate-the-total-cost-of-the-shopping-cart-items.md) — there, Amazon revamped the shopping cart to compute a total bill including discounts expressed as an arithmetic expression; here it's the bare pattern with no story attached. Since there are no parentheses, a single left-to-right scan with a stack handles operator precedence: push `+` numbers as-is and `-` numbers negated, but for `*` and `/`, pop the last pushed number, apply the operator immediately, and push the result back. Summing the stack at the end gives the answer.

## Solution

```java
import java.util.*;

class Solution {
    public static int calculate(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        int num = 0;
        char sign = '+'; // operator pending application to `num`

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            }
            // On hitting an operator (or the end of the string), settle the
            // number we just built using the *previous* pending operator.
            if ((!Character.isDigit(c) && c != ' ') || i == s.length() - 1) {
                switch (sign) {
                    case '+': stack.push(num); break;
                    case '-': stack.push(-num); break;
                    case '*': stack.push(stack.pop() * num); break;
                    case '/': stack.push(stack.pop() / num); break;
                }
                sign = c;
                num = 0;
            }
        }

        int result = 0;
        for (int n : stack) {
            result += n;
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(calculate("2+3/5"));
        // 2
        System.out.println(calculate("6- 7*3"));
        // -15
    }
}
```

## Complexity measures

Let **n** be the length of the string.

- **Time:** `O(n)` — a single left-to-right scan, with `O(1)` amortized work per character.
- **Space:** `O(n)` — the stack can hold up to one entry per `+`/`-` term in the expression.

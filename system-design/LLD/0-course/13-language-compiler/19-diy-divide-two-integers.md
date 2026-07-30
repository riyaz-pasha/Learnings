# DIY: Divide Two Integers

## Problem statement

Given two integers, `dividend` and `divisor`, divide them without using multiplication, division, or the modulo operator, and return the quotient (truncated toward zero). The divisor is guaranteed to be non-zero. Assume the environment can only store 32-bit signed integers, and clamp the result to that range if it would otherwise overflow.

### Input

```java
int dividend = 8;
int divisor = 2;
```

### Output

```java
4
```

## Coding exercise

Implement `divide(dividend, divisor)`.

This is the exact same pattern as [Feature #8: Divide in Power Save Mode](08-feature-8-divide-in-power-save-mode.md) — there, the compiler needed division that worked without the hardware `/` instruction on power-constrained devices; here it's the bare pattern with no story attached. The bit-shift doubling approach transfers over unchanged, including the special case for `Integer.MIN_VALUE / -1` overflowing a 32-bit result.

## Solution

```java
class Solution {
    public static int divide(int dividend, int divisor) {
        if (dividend == Integer.MIN_VALUE && divisor == -1) {
            return Integer.MAX_VALUE;
        }

        boolean negative = (dividend < 0) != (divisor < 0);
        long a = Math.abs((long) dividend);
        long b = Math.abs((long) divisor);
        int quotient = 0;

        while (a >= b) {
            long doubledDivisor = b;
            int multiple = 1;
            while (a >= (doubledDivisor << 1)) {
                doubledDivisor <<= 1;
                multiple <<= 1;
            }
            a -= doubledDivisor;
            quotient += multiple;
        }

        return negative ? -quotient : quotient;
    }

    public static void main(String[] args) {
        System.out.println(divide(8, 2));
        // 4
        System.out.println(divide(-7, 2));
        // -3
        System.out.println(divide(Integer.MIN_VALUE, -1));
        // 2147483647
    }
}
```

## Complexity measures

Let **n** be the magnitude of the dividend.

- **Time:** `O(log n)` — the divisor is doubled until it exceeds what's left of the dividend, at each step of the outer loop, and the outer loop itself removes at least half of what's left of the dividend each time.
- **Space:** `O(1)` — a fixed number of variables.

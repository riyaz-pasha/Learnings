# DIY: Pow(x, n)

## Problem statement

Implement `pow(x, n)`, which raises `x` to the integer power `n`. `n` can be negative, in which case the result is `1 / x^(-n)`.

### Input

```java
double base = 2;
int power = 4;
```

### Output

```java
16.0
```

## Coding exercise

Implement `pow(base, power)`.

This is the exact same pattern as [Feature #7: Exponentiation for Mobile Devices](07-feature-7-exponentiation-for-mobile-devices.md) — there, the compiler needed to emulate exponentiation on hardware that doesn't support it directly; here it's the bare `pow` function with no story attached. Fast exponentiation by squaring transfers over unchanged: negative exponents are converted to `(1/base)^|power|` up front, and the recursive halving does the rest in `O(log power)` multiplications.

## Solution

```java
class Solution {
    private static double quickPowRec(double base, int power) {
        if (power == 0) {
            return 1.0;
        }
        double half = quickPowRec(base, power / 2);
        if (power % 2 == 0) {
            return half * half;
        } else {
            return half * half * base;
        }
    }

    public static double pow(double base, int power) {
        if (power < 0) {
            base = 1 / base;
            power = -power;
        }
        return quickPowRec(base, power);
    }

    public static void main(String[] args) {
        System.out.println(pow(2, 4));
        // 16.0
        System.out.println(pow(2, -2));
        // 0.25
    }
}
```

## Complexity measures

Let **n** be the magnitude of `power`.

- **Time:** `O(log n)` — the exponent is halved at every recursive call.
- **Space:** `O(log n)` — one recursion frame per halving.

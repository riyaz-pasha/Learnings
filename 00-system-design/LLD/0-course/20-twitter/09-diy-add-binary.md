# DIY: Add Binary

## Problem statement

You're given two binary numbers as strings. Implement a function that adds them together and returns the result, also as a binary string.

### Input

```java
// a = "1010100"
// b = "0100011"
```

### Output

```java
// "1110111"
```

## Coding exercise

Implement `addBinary(a, b)`.

This is exactly [Feature #1: Add Likes](01-feature-1-add-likes.md) — the same digit-by-digit column addition with a carry, just with base 2 instead of base 10 (each column's digit is `(x1 + x2 + carry) % 2`, and the carry becomes `(x1 + x2 + carry) / 2`).

## Solution

```java
class Solution {
    public static String addBinary(String a, String b) {
        StringBuilder res = new StringBuilder();
        int carry = 0;
        int p1 = a.length() - 1;
        int p2 = b.length() - 1;

        while (p1 >= 0 || p2 >= 0) {
            int x1 = (p1 >= 0) ? a.charAt(p1) - '0' : 0;
            int x2 = (p2 >= 0) ? b.charAt(p2) - '0' : 0;

            int value = (x1 + x2 + carry) % 2;
            carry = (x1 + x2 + carry) / 2;

            res.append(value);
            p1--;
            p2--;
        }

        if (carry != 0) {
            res.append(carry);
        }

        return res.reverse().toString();
    }

    public static void main(String[] args) {
        System.out.println(addBinary("1010100", "0100011")); // 1110111
    }
}
```

## Solution walkthrough

`a = 1010100` is 84 in decimal, and `b = 0100011` is 35, so we expect `84 + 35 = 119`, which is `1110111` in binary — matching the output above. The algorithm walks both strings from the rightmost bit backward, computing each result bit as `(x1 + x2 + carry) % 2` and the next carry as `(x1 + x2 + carry) / 2`, exactly like decimal column addition but with base 2 instead of base 10. Once both strings run out, a leftover carry becomes one final leading `1` bit.

## Complexity measures

Let **n1** and **n2** be the lengths of the two input strings.

### Time Complexity

`O(max(n1, n2))` — one pass over the longer string, constant work per bit.

### Space Complexity

`O(max(n1, n2))` — the result can be at most one bit longer than the longer input.

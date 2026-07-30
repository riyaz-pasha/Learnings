# DIY: Multiply Strings

## Problem statement

You're given two non-negative integers, represented as strings, each up to 200 digits long. Return their product, also as a string.

Note: the source material's own problem statement suggests "convert the given strings to an integer... and then use the multiplication operator," but that can't actually work here — the stated constraint allows each input to be up to 200 digits, far beyond what any built-in numeric type (even a 64-bit `long`) can hold. The real exercise (and the one worth practicing, since it shows up often as "Multiply Strings" on its own) is to multiply the two numbers digit by digit, exactly the way you would by hand on paper, never converting either whole string to a number.

### Input

```java
// firstNumber = "5"
// secondNumber = "6"
```

### Output

```java
// "30"
```

## Coding exercise

Implement `multiply(num1, num2)`, which accepts two strings storing non-negative integers and returns their product as a string.

This is the string-arithmetic sibling of [Feature #1: Add Likes](01-feature-1-add-likes.md) — both avoid ever converting the input strings to a numeric type. Addition only ever touches two digits (plus a carry) at a time; multiplication of two arbitrary-length numbers needs the full grade-school long-multiplication layout, where digit `i` of the first number times digit `j` of the second number always lands in result positions `i + j` and `i + j + 1`.

## Solution

```java
class Solution {
    // Multiplies two non-negative integers given as strings, digit by digit,
    // without ever converting either one to a numeric type.
    public static String multiply(String num1, String num2) {
        if (num1.equals("0") || num2.equals("0")) {
            return "0";
        }

        int m = num1.length();
        int n = num2.length();
        // A product of an m-digit number and an n-digit number never needs
        // more than m + n digits.
        int[] result = new int[m + n];

        for (int i = m - 1; i >= 0; i--) {
            int d1 = num1.charAt(i) - '0';
            for (int j = n - 1; j >= 0; j--) {
                int d2 = num2.charAt(j) - '0';
                int mul = d1 * d2;

                // Digit i of num1 times digit j of num2 contributes to result
                // position i+j+1 (its own place value) and carries into i+j.
                int p1 = i + j;
                int p2 = i + j + 1;
                int sum = mul + result[p2];

                result[p2] = sum % 10;
                result[p1] += sum / 10;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int digit : result) {
            if (!(sb.length() == 0 && digit == 0)) {
                sb.append(digit);
            }
        }
        return sb.length() == 0 ? "0" : sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(multiply("5", "6"));     // 30
        System.out.println(multiply("123", "456")); // 56088
        System.out.println(multiply("999", "999")); // 998001
        System.out.println(multiply("0", "12345")); // 0
    }
}
```

## Solution walkthrough

Multiplying `num1`'s digit at position `i` by `num2`'s digit at position `j` produces a value whose place value lands at result index `i + j + 1`, with any overflow (10s place and up) carrying into index `i + j` — exactly like the partial products you'd write out and shift when multiplying by hand. Because every pair of digit positions contributes into the result array immediately (rather than building up separate shifted partial-product rows and adding them at the end), the carries accumulate correctly across all `m * n` digit pairs in a single pass, and the final array just needs its leading zeros trimmed off (unless the whole result is zero, in which case we return `"0"` directly).

## Complexity measures

Let **m** and **n** be the lengths of the two input strings.

### Time Complexity

`O(m * n)` — every pair of digits (one from each number) is multiplied exactly once.

### Space Complexity

`O(m + n)` — the result array holds at most `m + n` digits.

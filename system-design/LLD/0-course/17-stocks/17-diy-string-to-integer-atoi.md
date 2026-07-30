# DIY: String to Integer (atoi)

## Problem statement

Given a string, convert it to a 32-bit signed integer. The string may have leading whitespace. The first non-whitespace character may or may not be numeric:

- If it isn't numeric, it may be a `+` or `-` sign, determining whether the final result is negative or positive (assume positive if neither is present).
- If it is numeric, read subsequent characters until the next non-digit character or the end of the string, then convert those digits into an integer.
- If the first non-whitespace character is neither numeric nor a sign, the result is `0`.

> Only the space character `' '` counts as whitespace. Don't ignore any characters other than leading whitespace or the remainder of the string after the digits.

### Input

```java
// Sample Input 1
"123"
// Sample Input 2
"    -123"
// Sample Input 3
"9612 with words"
// Sample Input 4
"words and 345"
// Sample Input 5
"-93283472332"
```

### Output

```java
// Sample Output 1
123
// Sample Output 2
-123
// Sample Output 3
9612
// Sample Output 4
0
// Sample Output 5
-2147483648
```

## Coding exercise

Implement `myAtoi(str)`.

The closest match in this chapter is [Feature #7: Process Transactions](07-feature-7-process-transactions.md) — this exercise *is* that feature, without the log-line framing. Same rules (skip leading spaces, honor one optional sign, read digits, clamp on overflow), same solution.

## Solution

```java
class Solution {
    // Parses the leading integer in str (skipping leading whitespace,
    // honoring an optional sign), clamped to the 32-bit signed int range.
    public static int myAtoi(String str) {
        int i = 0, n = str.length();
        while (i < n && str.charAt(i) == ' ') {
            i++;
        }
        if (i == n) {
            return 0;
        }

        int sign = 1;
        if (str.charAt(i) == '+' || str.charAt(i) == '-') {
            sign = str.charAt(i) == '-' ? -1 : 1;
            i++;
        }

        long result = 0;
        while (i < n && Character.isDigit(str.charAt(i))) {
            int digit = str.charAt(i) - '0';
            result = result * 10 + digit;
            if (sign == 1 && result > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (sign == -1 && -result < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            i++;
        }
        return (int) (sign * result);
    }

    public static void main(String[] args) {
        String[] inputs = {"123", "    -123", "9612 with words", "words and 345", "-93283472332"};
        for (String s : inputs) {
            System.out.println("\"" + s + "\" -> " + myAtoi(s));
        }
        // "123" -> 123
        // "    -123" -> -123
        // "9612 with words" -> 9612
        // "words and 345" -> 0
        // "-93283472332" -> -2147483648
    }
}
```

`"words and 345"` returns `0` because the first non-whitespace character (`'w'`) is neither a digit nor a sign, so the digit-reading loop never executes and `result` stays `0`. `"-93283472332"` overflows a 32-bit int on the way down, so the loop's running `long` catches `-result < Integer.MIN_VALUE` partway through and clamps to `-2147483648` before finishing the string.

## Complexity measures

Let **n** be the length of the input string.

### Time Complexity

`O(n)` — the string is scanned left to right exactly once.

### Space Complexity

`O(1)` — a constant number of index and accumulator variables are used.

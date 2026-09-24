# DIY: Integer to English Words

## Problem statement

Convert a non-negative integer `n` to its English word representation.

### Input

```java
123
```

### Output

```java
"One Hundred Twenty Three"
```

## Coding exercise

Implement `numberToWords(n)`, returning the English word representation of `n`.

This is the exact same pattern as [Feature #4: Fare in Words](04-feature-4-fare-in-words.md) — there, Uber converted a ride fare to words for text-to-speech; here it's the bare pattern with no story attached. Split the number into 3-digit groups, convert each group with a hundreds/tens/ones lookup, and append the matching scale word (Thousand/Million/Billion).

## Solution

```java
class Solution {
    private static final String[] BELOW_20 = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };
    private static final String[] THOUSANDS = {"", "Thousand", "Million", "Billion"};

    public static String numberToWords(int n) {
        if (n == 0) return "Zero";

        StringBuilder result = new StringBuilder();
        int group = 0;

        while (n > 0) {
            if (n % 1000 != 0) {
                String chunk = threeDigitsToWords(n % 1000);
                String scale = THOUSANDS[group].isEmpty() ? "" : " " + THOUSANDS[group];
                result.insert(0, chunk + scale + " ");
            }
            n /= 1000;
            group++;
        }

        return result.toString().trim();
    }

    private static String threeDigitsToWords(int n) {
        StringBuilder sb = new StringBuilder();
        if (n >= 100) {
            sb.append(BELOW_20[n / 100]).append(" Hundred ");
            n %= 100;
        }
        if (n >= 20) {
            sb.append(TENS[n / 10]).append(" ");
            n %= 10;
        }
        if (n > 0) {
            sb.append(BELOW_20[n]).append(" ");
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        System.out.println(numberToWords(123));
        // One Hundred Twenty Three
    }
}
```

## Complexity measures

Let **n** be the number of digits in the input.

- **Time:** `O(n)` — the number is processed in constant-size groups of 3 digits.
- **Space:** `O(1)` — aside from the output string, only fixed-size lookup tables are used.

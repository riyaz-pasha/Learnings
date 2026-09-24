# DIY: Letter Combinations of a Phone Number

## Problem statement

Given a string of digits from 2–9, return every possible letter combination the number could represent on an old phone keypad (digit `1` maps to nothing):

```
2: abc   3: def   4: ghi   5: jkl   6: mno   7: pqrs   8: tuv   9: wxyz
```

**Constraints:** `0 <= digits.length <= 4`, each digit is in `['2', '9']`.

### Input

```java
"23"
```

### Output

```java
["ad", "ae", "af", "bd", "be", "bf", "cd", "ce", "cf"]
```

## Coding exercise

Implement `letterCombinations(digits)`.

Same shape as [Feature #9: Movie Combinations of a Genre](09-feature-9-movie-combinations-of-a-genre.md): pick one option (a letter) for the current position, recurse into the rest, undo, try the next option. Here "one genre's movie list" becomes "one digit's letter options."

## Solution

```java
import java.util.*;

class Solution {
    private static final String[] DIGIT_TO_LETTERS = {
            "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };

    public static List<String> letterCombinations(String digits) {
        List<String> combinations = new ArrayList<>();
        if (digits == null || digits.isEmpty()) {
            return combinations;
        }
        backtrack(digits, 0, new StringBuilder(), combinations);
        return combinations;
    }

    private static void backtrack(String digits, int index, StringBuilder current, List<String> combinations) {
        if (index == digits.length()) {
            combinations.add(current.toString());
            return;
        }

        String letters = DIGIT_TO_LETTERS[digits.charAt(index) - '0'];
        for (char letter : letters.toCharArray()) {
            current.append(letter);
            backtrack(digits, index + 1, current, combinations);
            current.deleteCharAt(current.length() - 1); // undo
        }
    }

    public static void main(String[] args) {
        System.out.println(letterCombinations("23"));
        // [ad, ae, af, bd, be, bf, cd, ce, cf]
    }
}
```

## Complexity measures

Let **n** be the length of `digits`, and let each digit map to up to 4 letters (digits `7` and `9`).

- **Time:** `O(4^n × n)` — up to `4^n` combinations, each of length `n` to build.
- **Space:** `O(n)` for the recursion stack, plus `O(4^n × n)` to store all combinations.

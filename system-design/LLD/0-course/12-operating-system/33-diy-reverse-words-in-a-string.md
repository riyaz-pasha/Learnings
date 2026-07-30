# DIY: Reverse Words in a String

## Problem statement

Given a string `s`, reverse the order of its words. The string may have multiple spaces between words and leading/trailing spaces — the result should have exactly one space between words, with no leading or trailing spaces.

### Input

```java
"  the sky   is blue"
```

### Output

```java
"blue is sky the"
```

## Coding exercise

Implement `reverseWords(s)`.

This is the exact same pattern as [Feature #13: Reverse Commands](13-feature-13-reverse-commands.md) — there, a logging bug prepended words instead of appending them, corrupting a command history log; here it's the bare pattern with no story attached. The approach is identical: trim, split on any run of whitespace, reverse the word list, and rejoin with single spaces.

## Solution

```java
import java.util.*;

class Solution {
    public static String reverseWords(String s) {
        s = s.trim();
        List<String> words = new ArrayList<>(Arrays.asList(s.split("\\s+")));
        Collections.reverse(words);
        return String.join(" ", words);
    }

    public static void main(String[] args) {
        System.out.println(reverseWords("  the sky   is blue"));
        // blue is sky the
    }
}
```

`trim()` removes the leading/trailing spaces, `split("\\s+")` breaks the remaining string into words while collapsing any run of whitespace between them, `Collections.reverse` flips the word order, and `String.join(" ", ...)` glues them back together with exactly one space each.

## Complexity measures

Let **n** be the length of the string.

- **Time:** `O(n)` — `trim`, `split`, `reverse`, and `join` each run in `O(n)`.
- **Space:** `O(n)` — the word list produced by `split`.

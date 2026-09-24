# DIY: Count a Word in the Comments

## Problem statement

You're given a list of strings representing lines of C++ source code, and a target word. Identify which parts of the code are comments (inline `//` and block `/* ... */`, following the same rules as before), then count how many times the target word appears **inside those comments only**.

### Input

```java
String[] source = {
    "/* Example code for feature */",
    "int main() {",
    " /*",
    " This is a",
    " block comment in the code",
    " */",
    " int value = 10; // This is an inline comment",
    " int sum = value + /* this is also a block */ value;",
    " char code = 'c';",
    " return 0;",
    "}"
};
String word = "code";
```

### Output

```java
2
```

Note: the source's own worked example claims the answer is `3` — but that count only holds if you also count the `code` in `char code = 'c';`, which is a plain variable declaration, **not** a comment. Restricting the count to comment text only (as the problem statement requires) gives `2`: one `code` in `/* Example code for feature */`, and one in `block comment in the code`.

## Coding exercise

Implement `countWord(source, word)`.

This is the exact same pattern as [Feature #1: Remove Comments](01-feature-1-remove-comments.md) — there, the compiler needed to strip comments out entirely; here it needs to isolate just the comment text and search it. The approach reuses the same state machine (a `block` flag tracking whether we're inside a block comment), except instead of keeping the *non-comment* characters, we keep the *comment* characters, then search that text for the target word.

## Solution

```java
import java.util.*;

class Solution {
    public static int countWord(String[] source, String word) {
        List<String> comments = extractComments(source);
        int count = 0;
        for (String comment : comments) {
            int idx = 0;
            while ((idx = comment.indexOf(word, idx)) != -1) {
                count++;
                idx += word.length();
            }
        }
        return count;
    }

    // Same state machine as removeComments, but collects the comment text instead of discarding it.
    private static List<String> extractComments(String[] source) {
        List<String> comments = new ArrayList<>();
        boolean block = false;
        for (String line : source) {
            StringBuilder lineComment = new StringBuilder();
            int i = 0;
            int n = line.length();
            while (i < n) {
                if (!block && i + 1 < n && line.charAt(i) == '/' && line.charAt(i + 1) == '/') {
                    lineComment.append(line.substring(i + 2));
                    break;
                } else if (!block && i + 1 < n && line.charAt(i) == '/' && line.charAt(i + 1) == '*') {
                    block = true;
                    i += 2;
                } else if (block && i + 1 < n && line.charAt(i) == '*' && line.charAt(i + 1) == '/') {
                    block = false;
                    i += 2;
                } else if (block) {
                    lineComment.append(line.charAt(i));
                    i++;
                } else {
                    i++; // not inside a comment: skip (this is source code, not comment text).
                }
            }
            comments.add(lineComment.toString());
        }
        return comments;
    }

    public static void main(String[] args) {
        String[] source = {
            "/* Example code for feature */",
            "int main() {",
            " /*",
            " This is a",
            " block comment in the code",
            " */",
            " int value = 10; // This is an inline comment",
            " int sum = value + /* this is also a block */ value;",
            " char code = 'c';",
            " return 0;",
            "}"
        };
        System.out.println(countWord(source, "code"));
        // 2
    }
}
```

The `extractComments` helper mirrors `removeComments` character for character, except every branch that used to discard a character now appends it, and vice versa — so it produces exactly the comment text that `removeComments` throws away.

## Complexity measures

Let **n** be the total length of the source code and **k** be the length of `word`.

- **Time:** `O(n + n·k)` — `O(n)` to extract the comment text, and up to `O(n·k)` for the `indexOf` scans across all comments (bounded by `O(n)` per comment in practice).
- **Space:** `O(n)` — the extracted comment text is stored before searching it.

# DIY: String Compression

## Problem statement

Given a list of characters, compress it in place using this rule: for each group of consecutive repeating characters, if the group's length is `1`, keep just the character; otherwise, keep the character followed by the group's length written out digit by digit (a group of `11` becomes the two characters `1` then `1`).

The compression must use constant extra space — the result must overwrite the input list itself, not be built into a new one. Return the new length after compression.

### Input

```java
chars = ['a', 'a', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'c']
```

### Output

```java
6
```

(The list compresses in place to `a2b11c` — 6 characters. `11` is written as two separate digit characters `1` and `1` since groups of 10 or more span multiple characters.)

## Coding exercise

Implement `stringCompression(chars)`.

This is a tighter variant of [Feature #8: Compress File II](08-feature-8-compress-file-ii.md) — there, the file compressor rewrote a character list into `[char, count-digits...]` form using `remove`/`insert` calls that could cost `O(n²)` in the worst case; here the same run-length idea is implemented with a **read/write two-pointer scan** instead, avoiding any shifting and running in true `O(n)` time.

## Solution

```java
import java.util.*;

class Solution {
    // Compresses runs of repeated characters in place; returns the new length.
    public static int stringCompression(List<Character> chars) {
        int write = 0, read = 0;
        int n = chars.size();

        while (read < n) {
            char ch = chars.get(read);
            int count = 0;
            while (read < n && chars.get(read) == ch) {
                read++;
                count++;
            }
            chars.set(write++, ch);
            if (count > 1) {
                for (char digit : String.valueOf(count).toCharArray()) {
                    chars.set(write++, digit);
                }
            }
        }

        // Drop the leftover tail beyond the new compressed length.
        while (chars.size() > write) {
            chars.remove(chars.size() - 1);
        }
        return write;
    }

    public static void main(String[] args) {
        List<Character> chars = new ArrayList<>(Arrays.asList(
            'a', 'a', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'c'
        ));
        int len = stringCompression(chars);
        System.out.println(len);
        // 6
        System.out.println(chars);
        // [a, 2, b, 1, 1, c]
    }
}
```

Two pointers do all the work: `read` scans ahead to find the extent of each run of identical characters (counting its length as it goes), while `write` lays down the compressed form — the character itself, then its count's digits if the run was longer than one — always at or behind `read`, so nothing already read is ever overwritten before it's used. Once the scan finishes, `write` marks the new logical length; any leftover elements past that point (never touched by `write`) are trimmed off.

## Complexity measures

Let **n** be the length of the input list.

- **Time:** `O(n)` — each character is visited once by `read`, and `write` never revisits a position.
- **Space:** `O(1)` — no auxiliary data structure scales with input size; the digit string for a single count is bounded by the input size limit.

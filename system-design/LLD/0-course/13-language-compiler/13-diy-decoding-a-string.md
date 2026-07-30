# DIY: Decoding a String

## Problem statement

Given an encoded string, return its decoded string. The encoding rule is `k[pattern]`, meaning `pattern` should be repeated `k` times. `pattern` may itself contain more nested `k[pattern]` encodings. You can assume `k` is always a positive integer, and that digits in the string only ever appear as part of a repeat count.

### Input

```java
String s = "abc3[cd]xyz";
```

### Output

```java
"abccdcdcdxyz"
```

A nested example:

```java
String s = "3[a2[b]]";
```

```java
"abbabbabb"
```

## Coding exercise

Implement `decodeString(s)`.

This is the exact same pattern as [Feature #3: Loop Unrolling](03-feature-3-loop-unrolling.md) — there, the compiler needed to expand `n[statements]` blocks representing loops; here it's the bare string-decoding version of the identical nested-bracket structure. The two-stack approach (one stack for pending repeat counts, one for the string built up at each outer nesting level) transfers over unchanged.

## Solution

```java
import java.util.*;

class Solution {
    public static String decodeString(String s) {
        Deque<Integer> countStack = new ArrayDeque<>();
        Deque<StringBuilder> stringStack = new ArrayDeque<>();
        StringBuilder current = new StringBuilder();
        int k = 0;

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                k = k * 10 + (c - '0');
            } else if (c == '[') {
                countStack.push(k);
                stringStack.push(current);
                current = new StringBuilder();
                k = 0;
            } else if (c == ']') {
                int repeat = countStack.pop();
                StringBuilder decoded = stringStack.pop();
                for (int i = 0; i < repeat; i++) {
                    decoded.append(current);
                }
                current = decoded;
            } else {
                current.append(c);
            }
        }
        return current.toString();
    }

    public static void main(String[] args) {
        System.out.println(decodeString("abc3[cd]xyz"));
        // abccdcdcdxyz
        System.out.println(decodeString("3[a2[b]]"));
        // abbabbabb
    }
}
```

On hitting `[`, we push the pending repeat count and the string built so far onto their respective stacks, then start a fresh buffer for the bracket's contents. On `]`, we repeat the current buffer the popped number of times, append it onto the string that was waiting one level up, and continue from there — so deeply nested brackets resolve from the inside out.

## Complexity measures

Let **n** be the length of the encoded string and **maxK** be the largest repeat count that appears.

- **Time:** `O(n × maxK)` — in the worst case, decoding involves copying a substring up to `maxK` times at each nesting level.
- **Space:** `O(n)` — the two stacks together hold at most one entry per nesting level, and the decoded output can be significantly longer than the input.

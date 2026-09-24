# DIY: String Transforms into Another String

## Problem statement

Given two strings of the same length `str1` and `str2`, determine whether you can transform `str1` into `str2` by doing zero or more conversions.

Note: in one conversion, you can convert all occurrences of one character in `str1` to any other lowercase English character.

Return `true` if and only if you can transform `str1` into `str2`.

### Input

```java
inputs = ("aabcc", "ccdee")
```

### Output

```java
true
```

## Coding exercise

Implement the `canConvert(str1, str2)` function, where `str1` is the string that needs to be converted to `str2`. The function returns `true` or `false` depending on whether the conversion is possible.

This is exactly [Feature #1: Mutate DNA](01-feature-1-mutate-dna.md) — same problem, same story, just renamed. Every character of `str1` maps to exactly one character of `str2` (a single conversion replaces *all* occurrences of one letter at once, so a source character can't split into two different targets), and any cyclic dependency among those mappings can be broken as long as a 26th, unused letter is available as a scratch value.

## Solution

```java
import java.util.*;

class Solution {
    public static boolean canConvert(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        }
        if (str1.equals(str2)) {
            return true;
        }

        Map<Character, Character> edges = new HashMap<>();
        for (int i = 0; i < str1.length(); i++) {
            char from = str1.charAt(i);
            char to = str2.charAt(i);
            Character existing = edges.get(from);
            if (existing != null && existing != to) {
                return false; // Same source character would need two different targets.
            }
            edges.put(from, to);
        }

        Set<Character> targets = new HashSet<>(edges.values());
        return targets.size() < 26; // A spare letter is needed to break any cycle.
    }

    public static void main(String[] args) {
        System.out.println(canConvert("aabcc", "ccdee")); // true
        System.out.println(canConvert("aabaa", "aadac")); // false
        System.out.println(canConvert("ac", "ca"));       // true
    }
}
```

Tracing `("aabcc", "ccdee")`: the mapping built is `a -> c`, `b -> d`, `c -> e` — every source character maps to exactly one target, so no conflict is found. The targets used are `{c, d, e}`, only 3 out of 26 letters, so there's plenty of spare room to break any cycle if one existed (here there isn't one anyway, since `a -> c -> e` is a chain, not a cycle). The function returns `true`.

For `("aabaa", "aadac")`: position 0 maps `a -> a`, position 1 maps `a -> a` (consistent so far), position 2 maps `b -> d`, position 3 maps `a -> a` (still consistent) — wait, position 3 in `str1` is `a` again (`"aabaa"`), and `str2` at position 3 is `a` as well (`"aadac"` → positions are `a, a, d, a, c`), so that's consistent. But position 4: `str1[4] = 'a'` needs to map to `str2[4] = 'c'` — yet `a` was already mapped to `a` from position 0. That's a conflict (`a` can't become both `a` and `c`), so the function correctly returns `false`.

## Complexity measures

Let **n** be the length of the two strings.

### Time Complexity

`O(n)` — both strings are scanned once while building the mapping.

### Space Complexity

`O(1)` — the mapping holds at most 26 entries, one per letter of the alphabet.

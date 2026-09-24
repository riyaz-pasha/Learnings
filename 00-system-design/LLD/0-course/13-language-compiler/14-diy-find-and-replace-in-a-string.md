# DIY: Find and Replace in a String

## Problem statement

You're given a string `s`, along with parallel arrays `indices`, `sources`, and `targets`. For every `i`, if `sources[i]` appears in `s` starting exactly at position `indices[i]`, replace that occurrence with `targets[i]`. Return the resulting string. `sources[i]` and `targets[i]` may have different lengths.

### Input

```java
String s = "vmokgggqzp";
int[] indices = {3, 5, 1};
String[] sources = {"kg", "ggq", "mo"};
String[] targets = {"s", "so", "bfr"};
```

### Output

```java
"vbfrssozp"
```

## Coding exercise

Implement `findAndReplace(s, indices, sources, targets)`.

This is the exact same pattern as [Feature #4: Optimization by Replacement](04-feature-4-optimization-by-replacement.md) — there, the compiler needed to swap slow function calls for faster equivalents at given positions in a code line; here it's the bare string-replacement version. Both need the same right-to-left processing order so that earlier replacements' indices don't shift out from under later ones... except it's actually the other way round: replacements must be applied in *descending* index order precisely so that a replacement never invalidates the positions of replacements still to come.

## Solution

```java
import java.util.*;

class Solution {
    public static String findAndReplace(String s, int[] indices, String[] sources, String[] targets) {
        List<int[]> order = new ArrayList<>();
        for (int i = 0; i < indices.length; i++) {
            order.add(new int[]{indices[i], i});
        }
        order.sort((a, b) -> b[0] - a[0]); // descending by index.

        StringBuilder sb = new StringBuilder(s);
        for (int[] pair : order) {
            int idx = pair[0];
            int j = pair[1];
            String src = sources[j];
            String tgt = targets[j];
            if (idx + src.length() <= sb.length() && sb.substring(idx, idx + src.length()).equals(src)) {
                sb.replace(idx, idx + src.length(), tgt);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(findAndReplace(
            "vmokgggqzp",
            new int[]{3, 5, 1},
            new String[]{"kg", "ggq", "mo"},
            new String[]{"s", "so", "bfr"}
        ));
        // vbfrssozp
    }
}
```

Sorting by descending index and applying replacements from right to left means every replacement only ever touches the portion of the string to its right (already processed) or exactly at its own position — the indices to its left are never disturbed, so they stay valid until we get to them.

## Complexity measures

Let **l** be the length of `s` and **n** be the number of replacement operations.

- **Time:** `O(l × n)` — sorting takes `O(n log n)`, and each of the `n` substring checks/replacements can touch up to `O(l)` characters.
- **Space:** `O(l)` — the mutable copy of the string being built.

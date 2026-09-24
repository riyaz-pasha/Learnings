# DIY: Word Break II

## Problem statement

An extension of the previous challenge: instead of just checking whether `s` can be broken into words from `subs`, return **every possible way** to do it (words may repeat).

### Input

```java
s = "magically"
subs = {"ag", "al", "icl", "mag", "magic", "ly", "lly"}
```

### Output

```java
["magic al ly"]
```

`"magic"(0-4) + "al"(5-6) + "ly"(7-8)` reconstructs `"magically"` exactly. (Other-looking splits like `"mag icl ly"` or `"magic lly"` don't actually work — `"mag"` leaves the remainder `"ically"`, whose next three letters are `i,c,a`, not `icl`; and `"magic"` leaves `"ally"`, which starts with `a`, not `ll`. `"magic al ly"` is the only valid decomposition.)

## Coding exercise

Implement `stringBreak(s, subs)`.

Exactly [Feature #4: Suggest Possible Queries After Adding White Spaces](04-feature-4-suggest-possible-queries-after-adding-white-spaces.md) — memoized recursion building every valid sentence from the suffix solutions.

## Solution

```java
import java.util.*;

class Solution {

    public static String[] stringBreak(String s, String[] subs) {
        Set<String> dictSet = new HashSet<>(Arrays.asList(subs));
        Map<String, List<String>> memo = new HashMap<>();
        return helper(s, dictSet, memo).toArray(new String[0]);
    }

    private static List<String> helper(String query, Set<String> dictSet, Map<String, List<String>> memo) {
        if (memo.containsKey(query)) {
            return memo.get(query);
        }

        List<String> result = new ArrayList<>();
        if (query.isEmpty()) {
            result.add("");
            return result;
        }

        for (int end = 1; end <= query.length(); end++) {
            String prefix = query.substring(0, end);
            if (!dictSet.contains(prefix)) {
                continue;
            }

            String suffix = query.substring(end);
            for (String suffixSentence : helper(suffix, dictSet, memo)) {
                result.add(suffixSentence.isEmpty() ? prefix : prefix + " " + suffixSentence);
            }
        }

        memo.put(query, result);
        return result;
    }

    public static void main(String[] args) {
        String[] subs = {"ag", "al", "icl", "mag", "magic", "ly", "lly"};
        System.out.println(Arrays.toString(stringBreak("magically", subs))); // [magic al ly]
    }
}
```

## Complexity measures

Let **n** be the length of `s` and **w** the size of `subs`.

- **Time:** `O(n² + 2ⁿ + w)`.
- **Space:** `O(n × 2ⁿ + l)`.

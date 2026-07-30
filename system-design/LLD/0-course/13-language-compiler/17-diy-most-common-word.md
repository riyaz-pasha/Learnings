# DIY: Most Common Word

## Problem statement

Given a piece of text and a list of banned words, find the most frequent word in the text that isn't on the banned list. Words are case-insensitive, and punctuation should be treated as a separator, not part of a word. You can assume the answer is unique.

### Input

```java
String code = "int main() {\n"
    + "  int value = getValue();\n"
    + "  int sum = value + getRandom();\n"
    + "  int subs = value - getRandom();\n"
    + "  return 0;\n"
    + "}";
String[] banned = {"int", "main", "return"};
```

### Output

```java
"value"
```

## Coding exercise

Implement `mostCommonToken(code, banned)`.

This is the exact same pattern as [Feature #6: Most Common Token](06-feature-6-most-common-token.md) — there, the compiler needed to find the most frequently used variable or function name while ignoring language keywords; here it's the bare pattern with no story attached, applied to natural-language text and banned words instead of source code and keywords. The normalize-then-count approach carries over unchanged: strip everything that isn't alphanumeric down to spaces, split into tokens, and tally frequencies while skipping the banned set.

## Solution

```java
import java.util.*;

class Solution {
    public static String mostCommonToken(String code, String[] banned) {
        String normalized = code.toLowerCase().replaceAll("[^a-z0-9]", " ");
        String[] tokens = normalized.trim().split("\\s+");

        Set<String> bannedWords = new HashSet<>(Arrays.asList(banned));
        Map<String, Integer> count = new HashMap<>();
        for (String token : tokens) {
            if (token.isEmpty() || bannedWords.contains(token)) {
                continue;
            }
            count.put(token, count.getOrDefault(token, 0) + 1);
        }

        String best = null;
        int bestCount = -1;
        for (Map.Entry<String, Integer> entry : count.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    public static void main(String[] args) {
        String code = "int main() {\n"
            + "  int value = getValue();\n"
            + "  int sum = value + getRandom();\n"
            + "  int subs = value - getRandom();\n"
            + "  return 0;\n"
            + "}";
        System.out.println(mostCommonToken(code, new String[]{"int", "main", "return"}));
        // value
    }
}
```

`value` appears three times (`int value = getValue();`, `int sum = value + ...`, `int subs = value - ...`), more than any other non-banned token — including `getRandom`, which appears twice.

## Complexity measures

Let **n** be the length of the text and **m** be the number of banned words.

- **Time:** `O(n + m)` — `O(n)` to normalize and tokenize the text, `O(m)` to build the banned-word set.
- **Space:** `O(n + m)` — the frequency map and the banned-word set.

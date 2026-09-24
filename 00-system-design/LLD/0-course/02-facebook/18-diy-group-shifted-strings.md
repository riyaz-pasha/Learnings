# DIY: Group Shifted Strings

## Problem statement

Given an array of lowercase strings, group all strings that are shifted versions of each other (every character incremented by the same amount, wrapping `z -> a`).

### Input

```java
{"acd", "dfg", "wyz", "yab", "mop", "bdfh", "b", "y", "moqs"}
```

### Output

```java
{
  {"acd", "dfg", "wyz", "yab", "mop"},
  {"bdfh", "moqs"},
  {"b", "y"}
}
```

## Coding exercise

Implement `groupStrings(strs)`.

Identical to [Feature #6: Combine Similar Messages](06-feature-6-combine-similar-messages.md) — build a signature from consecutive-character differences (with wrap-around handling), and group by that signature in a HashMap.

## Solution

```java
import java.util.*;

class Solution {

    public static List<List<String>> groupStrings(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();

        for (String s : strs) {
            String key = generateKey(s);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        return new ArrayList<>(groups.values());
    }

    private static String generateKey(String word) {
        StringBuilder key = new StringBuilder();
        for (int i = 1; i < word.length(); i++) {
            int diff = (word.charAt(i) - word.charAt(i - 1) + 26) % 26;
            key.append(diff).append(",");
        }
        return key.toString();
    }

    public static void main(String[] args) {
        String[] strs = {"acd", "dfg", "wyz", "yab", "mop", "bdfh", "b", "y", "moqs"};
        List<List<String>> result = groupStrings(strs);
        result.forEach(System.out::println);
    }
}
```

## Complexity measures

Let **n** be the number of strings and **l** the average string length.

- **Time:** `O(n × l)`.
- **Space:** `O(n)` in the worst case.

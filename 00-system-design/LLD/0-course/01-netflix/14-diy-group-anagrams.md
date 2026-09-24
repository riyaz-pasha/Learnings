# DIY: Group Anagrams

## Problem statement

You're given a list of words or phrases. Group the ones that are **anagrams** of each other — words made of exactly the same letters, just rearranged.

### Input

```java
{"word", "sword", "drow", "rowd", "iced", "dice"}
```

### Output

```java
{{"word", "drow", "rowd"}, {"sword"}, {"iced", "dice"}}
```

(Order of the groups, and order within a group, doesn't matter.)

## Coding exercise

Implement `groupAnagrams(strs)`, returning a list of groups.

This is the exact same pattern as [Feature #1: Group Similar Titles](01-feature-1-group-similar-titles.md) — there, Netflix grouped misspelled movie titles by anagram; here it's the bare pattern with no story attached. Build a per-word signature (26-letter frequency count) and group by that signature in a HashMap.

## Solution

```java
import java.util.*;

class Solution {
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();

        for (String s : strs) {
            int[] count = new int[26];
            for (char c : s.toCharArray()) {
                count[c - 'a']++;
            }

            StringBuilder keyBuilder = new StringBuilder();
            for (int freq : count) {
                keyBuilder.append('#').append(freq);
            }

            groups.computeIfAbsent(keyBuilder.toString(), k -> new ArrayList<>()).add(s);
        }

        return new ArrayList<>(groups.values());
    }

    public static void main(String[] args) {
        String[] words = {"word", "sword", "drow", "rowd", "iced", "dice"};
        System.out.println(groupAnagrams(words));
        // [[word, drow, rowd], [sword], [iced, dice]]
    }
}
```

## Complexity measures

Let **n** be the number of words and **k** the length of the longest word.

- **Time:** `O(n × k)` — each word is scanned once to build its signature.
- **Space:** `O(n × k)` — every word is stored once, across all groups.

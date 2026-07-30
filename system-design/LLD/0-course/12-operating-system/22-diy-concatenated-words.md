# DIY: Concatenated Words

## Problem statement

Given an array of strings `words` with unique elements, return all the words that are a **concatenated word** — a combination of at least two other words from the same array.

### Input

```java
words = {"cat", "dog", "cats", "lion", "catsdog"}
```

### Output

```java
{"catsdog"}
```

(`catsdog` is the only word made up of `cats` and `dog`, both of which exist in the array.)

## Coding exercise

Implement `findAllConcatenatedWords(words)`.

This is the exact same pattern as [Feature #4: Compress File](04-feature-4-compress-file.md) — there, the OS wanted to identify concatenated words so it could replace them with smaller words' IDs to save space; here it's the bare pattern with no story attached. The approach is identical: for each word, recursively try every prefix/suffix split, requiring the prefix to already be a known word before checking whether the suffix is a word (or itself splittable), and memoize results to avoid recomputing the same sub-check repeatedly.

## Solution

```java
import java.util.*;

class Solution {
    public static List<String> findAllConcatenatedWords(String[] words) {
        List<String> result = new ArrayList<>();
        Set<String> wordSet = new HashSet<>(Arrays.asList(words));
        Map<String, Boolean> cache = new HashMap<>();

        for (String word : words) {
            if (!word.isEmpty() && canForm(word, wordSet, cache)) {
                result.add(word);
            }
        }
        return result;
    }

    private static boolean canForm(String word, Set<String> wordSet, Map<String, Boolean> cache) {
        if (cache.containsKey(word)) return cache.get(word);

        for (int i = 1; i < word.length(); i++) {
            String prefix = word.substring(0, i);
            String suffix = word.substring(i);
            if (wordSet.contains(prefix) && (wordSet.contains(suffix) || canForm(suffix, wordSet, cache))) {
                cache.put(word, true);
                return true;
            }
        }
        cache.put(word, false);
        return false;
    }

    public static void main(String[] args) {
        String[] words = {"cat", "dog", "cats", "lion", "catsdog"};
        System.out.println(findAllConcatenatedWords(words));
        // [catsdog]
    }
}
```

## Complexity measures

Let **n** be the number of words and **m** be the average word length.

- **Time:** `O(n × m²)` — each word has `O(m)` split points, each costing `O(m)` for the substring comparisons.
- **Space:** `O(n × m²)` — the word set, cache, and the substrings generated across all splits.

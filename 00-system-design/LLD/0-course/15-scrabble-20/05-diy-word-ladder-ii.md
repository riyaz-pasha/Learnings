# DIY: Word Ladder II

## Problem statement

You're given a list of words, a `startingWord`, and an `endingWord`. Find **all** shortest transformation sequences from `startingWord` to `endingWord`, under the following constraints:

- All the words are the same length.
- There are no duplicates in the given list.
- `startingWord` and `endingWord` are both non-empty.
- `startingWord` and `endingWord` are not the same.
- A transition can only occur between two words that differ by exactly one character.
- Return an empty list if no such transformation sequence exists.
- All words contain only lowercase alphabetic characters.

### Input

```java
wordList = {"hot", "dot", "dog", "lot", "log", "cog"}
start = "hit"
target = "cog"
```

### Output

```java
{
  {"hit", "hot", "dot", "dog", "cog"},
  {"hit", "hot", "lot", "log", "cog"}
}
```

`hit` can be converted to `cog` by following either of these two sequences — both take the same minimum number of moves.

## Coding exercise

Implement `wordLadder2(startingWord, endingWord, wordList)`. The function returns every shortest transformation sequence from `startingWord` to `endingWord`.

This is the exact same pattern as [Feature #2: Possible Results](02-feature-2-possible-results.md) — there the story was a Scrabble computer player showing every shortest transformation for a human player to choose from; here it's the bare "all shortest paths" function with no game attached. Compare it against [DIY: Word Ladder I](04-diy-word-ladder-i.md) above: that one only needs the *distance*, so plain BFS suffices; this one needs every *path* achieving that distance, so BFS is used to fix the layers (and record a parent map) before a backward DFS reconstructs every complete path. A naive DFS straight over the word graph would be far too slow — most of its exploration would land on paths far longer than the shortest one, and it's exactly the kind of approach that looks correct but doesn't terminate quickly on anything but toy inputs.

## Solution

```java
import java.util.*;

class Solution {
    public static List<List<String>> wordLadder2(String startingWord, String endingWord, String[] wordList) {
        List<List<String>> results = new ArrayList<>();
        Set<String> wordSet = new HashSet<>(Arrays.asList(wordList));
        if (!wordSet.contains(endingWord)) {
            return results; // endingWord isn't even a valid word in the list.
        }

        Map<String, List<String>> parents = new HashMap<>();
        Set<String> currentLevel = new HashSet<>();
        currentLevel.add(startingWord);
        Set<String> visited = new HashSet<>();
        visited.add(startingWord);
        boolean found = false;

        while (!currentLevel.isEmpty() && !found) {
            Set<String> nextLevel = new HashSet<>();
            Set<String> visitedThisLevel = new HashSet<>();

            for (String word : currentLevel) {
                char[] chars = word.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    char original = chars[i];
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == original) {
                            continue;
                        }
                        chars[i] = c;
                        String next = new String(chars);
                        if (wordSet.contains(next) && !visited.contains(next)) {
                            nextLevel.add(next);
                            visitedThisLevel.add(next);
                            parents.computeIfAbsent(next, k -> new ArrayList<>()).add(word);
                            if (next.equals(endingWord)) {
                                found = true;
                            }
                        }
                    }
                    chars[i] = original;
                }
            }

            visited.addAll(visitedThisLevel);
            currentLevel = nextLevel;
        }

        if (!found) {
            return results; // no transformation sequence exists.
        }

        List<String> path = new LinkedList<>();
        path.add(endingWord);
        backtrack(endingWord, startingWord, parents, path, results);
        results.sort((a, b) -> String.join(",", a).compareTo(String.join(",", b)));
        return results;
    }

    private static void backtrack(String word, String startingWord, Map<String, List<String>> parents,
                                   List<String> path, List<List<String>> results) {
        if (word.equals(startingWord)) {
            results.add(new ArrayList<>(path));
            return;
        }
        for (String parent : parents.getOrDefault(word, Collections.emptyList())) {
            path.add(0, parent);
            backtrack(parent, startingWord, parents, path, results);
            path.remove(0);
        }
    }

    public static void main(String[] args) {
        String[] wordList = {"hot", "dot", "dog", "lot", "log", "cog"};
        List<List<String>> results = wordLadder2("hit", "cog", wordList);
        for (List<String> path : results) {
            System.out.println(path);
        }
        // [hit, hot, dot, dog, cog]
        // [hit, hot, lot, log, cog]
    }
}
```

`wordLadder2` expands the search one full BFS layer at a time — trying all 26 letters at every position of every word in the current layer — and records every word-to-word edge it discovers in `parents`, stopping the moment `endingWord` shows up in a freshly built layer. Since BFS explores level by level, that's guaranteed to be the shortest possible distance, and because `parents` may hold multiple entries per word, the backward `backtrack` walk naturally reconstructs every shortest path, not just one.

## Complexity measures

Let **m** be the length of each word and **n** be the total number of words in `wordList`.

- **Time:** `O((m × 26) × n)` — the BFS layering considers up to **n** words, each tested against 26 letters at each of its **m** positions; the backward DFS over the parent map adds only `O(m × n)`.
- **Space:** `O(m² × n)` — the `parents` map and `visited`/level sets together hold up to `O(m × n)` word references, each of length `O(m)`.

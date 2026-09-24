# DIY: Word Ladder I

## Problem statement

You're given an array of words, a `startingWord`, and an `endingWord`. Find the minimum number of transitions required to convert `startingWord` into `endingWord`, under the following constraints:

- All the words are the same length.
- There are no duplicates in the given array.
- `startingWord` and `endingWord` are both non-empty.
- `startingWord` and `endingWord` are not the same.
- A transition can only occur between two words that differ by exactly one character.
- Return an empty result if no such transformation sequence exists.
- All words contain only lowercase alphabetic characters.

### Input

```java
wordList = {"hot", "dot", "dog", "lot", "log", "cog"}
start = "hit"
target = "cog"
```

### Output

```java
4
```

One shortest transformation is `"hit" -> "hot" -> "dot" -> "dog" -> "cog"` — it takes **4** steps to reach the last word.

## Coding exercise

Implement `wordLadder(startingWord, endingWord, wordList)`. The function returns the number of moves in the shortest transformation sequence from `startingWord` to `endingWord`.

This is the exact same pattern as [Feature #1: Minimum Moves](01-feature-1-minimum-moves.md) — there the story was a Scrabble computer player converting one word to another; here it's the bare shortest-path function with no game attached. The approach is identical: build a map from each word's one-letter-wildcard states to the words that produce them, then BFS outward from `startingWord`, returning the level at which `endingWord` is first reached.

## Solution

```java
import java.util.*;

class Solution {
    public static int wordLadder(String startingWord, String endingWord, String[] wordList) {
        if (startingWord.equals(endingWord)) {
            return 0;
        }

        Map<String, List<String>> statesList = new HashMap<>();
        for (String word : wordList) {
            for (int i = 0; i < word.length(); i++) {
                String state = word.substring(0, i) + "*" + word.substring(i + 1);
                statesList.computeIfAbsent(state, k -> new ArrayList<>()).add(word);
            }
        }

        Queue<Map.Entry<String, Integer>> queue = new LinkedList<>();
        queue.offer(new AbstractMap.SimpleEntry<>(startingWord, 0));
        Set<String> visited = new HashSet<>();
        visited.add(startingWord);

        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> entry = queue.poll();
            String currentWord = entry.getKey();
            int level = entry.getValue();

            for (int i = 0; i < currentWord.length(); i++) {
                String state = currentWord.substring(0, i) + "*" + currentWord.substring(i + 1);
                for (String neighbor : statesList.getOrDefault(state, Collections.emptyList())) {
                    if (neighbor.equals(endingWord)) {
                        return level + 1;
                    }
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(new AbstractMap.SimpleEntry<>(neighbor, level + 1));
                    }
                }
            }
        }
        return -1; // endingWord is unreachable from startingWord.
    }

    public static void main(String[] args) {
        String[] wordList = {"hot", "dot", "dog", "lot", "log", "cog"};
        System.out.println(wordLadder("hit", "cog", wordList));
        // 4
    }
}
```

`wordLadder` runs a plain BFS over the implicit word graph: every word's neighbors are found in `O(m)` per wildcard state instead of scanning the whole word list, and the BFS level at which `endingWord` first appears is exactly the minimum number of one-letter moves needed to reach it.

## Complexity measures

Let **m** be the length of each word and **n** be the total number of words in `wordList`.

- **Time:** `O(m² × n)` — building the wildcard-state map and running BFS each do `O(m)` substring work per word, across up to **n** words.
- **Space:** `O(m² × n)` — the wildcard-state map stores **m** keys of length **m** for each of the **n** words.

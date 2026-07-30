# DIY: Alien Dictionary

## Problem statement

You are given a list of `words` written in an alien language, where the strings in `words` are sorted lexicographically according to the rules of that language. The aliens also use English lowercase letters, but possibly in a different order.

Given the vector of `words`, return a string of the unique letters, sorted in lexicographically increasing order according to the alien language.

If there are multiple valid orderings, you can return any one of them. If there is no valid ordering, return an empty string `""`.

### Constraints

- `1 <= words.length <= 100`
- `1 <= words[i].length <= 20`
- All characters in `words[i]` are English lowercase letters.

### Input

```java
// Sample Example 1
words = ["xro", "xma", "per", "pert", "oxh", "olv"]

// Sample Example 2
words = ["mdx", "mars", "avgd", "dkae"]
```

### Output

```java
// Sample Example 1
"artevxhmplo"

// Sample Example 2
""
```

## Coding exercise

Implement `alienOrder(words)`.

This is the exact same pattern as [Feature #3: Find Dictionary](03-feature-3-find-dictionary.md) — there, a receiver had to reverse-engineer an unknown encryption dictionary from a batch of sorted training messages; here it's the bare "reconstruct the alphabet from a sorted word list" problem with no story attached. Extract an `x-before-y` relation from the first differing letter of each adjacent word pair, build a directed graph over the letters, and topologically sort it with Kahn's algorithm (BFS driven by indegree counts). A leftover letter with unresolved indegree means a cycle, and thus no valid ordering.

## Solution

```java
import java.util.*;

class Solution {
    public static String alienOrder(String[] words) {
        Map<Character, Set<Character>> adjacency = new HashMap<>();
        Map<Character, Integer> indegree = new HashMap<>();

        for (String word : words) {
            for (char c : word.toCharArray()) {
                indegree.putIfAbsent(c, 0);
                adjacency.putIfAbsent(c, new HashSet<>());
            }
        }

        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i];
            String w2 = words[i + 1];
            int minLen = Math.min(w1.length(), w2.length());

            if (w1.length() > w2.length() && w1.startsWith(w2)) {
                return ""; // Longer word can't come before its own prefix.
            }

            for (int j = 0; j < minLen; j++) {
                char c1 = w1.charAt(j);
                char c2 = w2.charAt(j);
                if (c1 != c2) {
                    if (!adjacency.get(c1).contains(c2)) {
                        adjacency.get(c1).add(c2);
                        indegree.put(c2, indegree.get(c2) + 1);
                    }
                    break;
                }
            }
        }

        Queue<Character> queue = new ArrayDeque<>();
        for (char c : indegree.keySet()) {
            if (indegree.get(c) == 0) {
                queue.add(c);
            }
        }

        StringBuilder result = new StringBuilder();
        while (!queue.isEmpty()) {
            char c = queue.poll();
            result.append(c);
            for (char next : adjacency.get(c)) {
                indegree.put(next, indegree.get(next) - 1);
                if (indegree.get(next) == 0) {
                    queue.add(next);
                }
            }
        }

        return result.length() == indegree.size() ? result.toString() : "";
    }

    public static void main(String[] args) {
        System.out.println(alienOrder(new String[]{"xro", "xma", "per", "pert", "oxh", "olv"}));
        // artevxhmplo

        System.out.println("[" + alienOrder(new String[]{"mdx", "mars", "avgd", "dkae"}) + "]");
        // []
    }
}
```

Every unique letter becomes a graph node. Comparing adjacent words gives us directed edges (`x -> y` meaning "x sorts before y"), tracked as both an adjacency list and an indegree count per letter. We seed a queue with every letter that starts at indegree `0` (nothing must come before it), then repeatedly pop a letter, append it to the result, and decrement its neighbors' indegrees — pushing any neighbor that drops to `0`. If the final result string is shorter than the number of unique letters, some letters still had unresolved dependencies on each other — a cycle — so no valid ordering exists.

## Complexity measures

Let **n** be the number of words, **c** the total number of letters across all words, and **u** the number of unique letters (at most 26).

- **Time:** `O(c)` — extracting relations costs `O(c)` in the worst case; the topological sort is a BFS over `u` nodes and at most `min(u², n - 1)` edges, which is dominated by `O(c)` for a fixed small alphabet.
- **Space:** `O(1)` for a fixed 26-letter alphabet — the adjacency list and indegree map are bounded by `O(u + min(u², n))`, which collapses to a constant here.

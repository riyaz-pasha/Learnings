# DIY: Suggest Relevant Sentences

## Problem statement

Design a system that guesses how a string will end, based on historical query data. `input()` receives a continuous stream of string chunks (not necessarily one character at a time), ending when `"#"` arrives. Each call returns the top 3 suggestions matching the prefix typed so far. Once a query completes (on `"#"`), it's added to the historical data too.

### Input

```java
sentences = {"beautiful", "best quotes", "best friend", "best birthday wishes", "instagram", "internet"}
times     = {30, 14, 21, 10, 10, 15}

input("in")
input("sta")
input("#")
```

### Output

```java
["internet", "instagram"]   // after "in": both historical matches, ranked by popularity
["instagram"]                 // after "insta": only one match
[]                             // after "#": "insta" is now recorded for future queries
```

## Coding exercise

Implement `SuggestionSystem(sentences, times)` and `input(chunk)`.

Same design as [Feature #2: Design Search Autocomplete System](02-feature-2-design-search-autocomplete-system.md) — a trie of historical queries ranked by popularity, DFS-collected and sorted for the top 3. The only difference: `input()` here receives a **chunk of characters at a time** instead of one character, so the buffer just appends the whole chunk instead of a single char.

## Solution

```java
import java.util.*;

class Node {
    HashMap<Character, Node> children = new HashMap<>();
    boolean isEnd = false;
    String sentence;
    int rank = 0;
}

class SuggestionSystem {
    private final Node root = new Node();
    private final StringBuilder keyword = new StringBuilder();

    public SuggestionSystem(String[] sentences, int[] times) {
        for (int i = 0; i < sentences.length; i++) {
            addRecord(sentences[i], times[i]);
        }
    }

    private void addRecord(String sentence, int count) {
        Node current = root;
        for (char c : sentence.toCharArray()) {
            current.children.putIfAbsent(c, new Node());
            current = current.children.get(c);
        }
        current.isEnd = true;
        current.sentence = sentence;
        current.rank += count;
    }

    public String[] input(String chunk) {
        if (chunk.equals("#")) {
            addRecord(keyword.toString(), 1);
            keyword.setLength(0);
            return new String[]{};
        }

        keyword.append(chunk);
        return search(keyword.toString());
    }

    private String[] search(String prefix) {
        Node current = root;
        for (char c : prefix.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return new String[]{};
            }
            current = current.children.get(c);
        }

        List<Node> matches = new ArrayList<>();
        dfs(current, matches);
        matches.sort((a, b) -> a.rank != b.rank ? b.rank - a.rank : a.sentence.compareTo(b.sentence));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(3, matches.size()); i++) {
            result.add(matches.get(i).sentence);
        }
        return result.toArray(new String[0]);
    }

    private void dfs(Node node, List<Node> matches) {
        if (node.isEnd) {
            matches.add(node);
        }
        for (Node child : node.children.values()) {
            dfs(child, matches);
        }
    }

    public static void main(String[] args) {
        String[] sentences = {"beautiful", "best quotes", "best friend", "best birthday wishes", "instagram", "internet"};
        int[] times = {30, 14, 21, 10, 10, 15};
        SuggestionSystem system = new SuggestionSystem(sentences, times);

        System.out.println(Arrays.toString(system.input("in")));   // [internet, instagram]
        System.out.println(Arrays.toString(system.input("sta")));  // [instagram]
        System.out.println(Arrays.toString(system.input("#")));    // []
    }
}
```

## Complexity measures

Let **n** be the number of historical sentences, **l** the average sentence length, **q** the number of matches for a prefix, and **m** the prefix length.

- **Constructor:** `O(n × l)`.
- **`input()`:** `O(m + q + q log q)`.
- **Space:** `O(n × l)`.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class SearchTerm implements Comparable<SearchTerm> {
    String term;
    int frequency;

    public SearchTerm(String term, int frequency) {
        this.term = term;
        this.frequency = frequency;
    }

    @Override
    public int compareTo(SearchTerm other) {
        if (this.frequency == other.frequency) {
            return this.term.compareTo(other.term);
        }
        return Integer.compare(this.frequency, other.frequency);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SearchTerm) {
            return this.term.equals(((SearchTerm) o).term);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.term.hashCode();
    }
}

class TrieNode {
    Map<Character, TrieNode> children;
    PriorityQueue<SearchTerm> top3;

    public TrieNode() {
        this.children = new HashMap<>();
        this.top3 = new PriorityQueue<>();
    }

    public void updateTop3(SearchTerm searchTerm) {
        this.top3.remove(searchTerm); // Remove old version if exists
        this.top3.offer(searchTerm);
        if (this.top3.size() > 3) {
            this.top3.poll();
        }
    }
}

class AutocompleteSearchSystem {
    TrieNode root;
    Map<String, Integer> frequencyMap;
    StringBuilder currentInput;

    public AutocompleteSearchSystem(List<SearchTerm> searchTerms) {
        this.root = new TrieNode();
        this.frequencyMap = new HashMap<>();
        this.currentInput = new StringBuilder();

        for (SearchTerm st : searchTerms) {
            frequencyMap.put(st.term, st.frequency);
            insert(st.term);
        }
    }

    private void insert(String term) {
        int freq = frequencyMap.get(term);
        SearchTerm newTerm = new SearchTerm(term, freq);
        TrieNode current = root;
        for (char ch : term.toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode());
            current.updateTop3(newTerm);
        }
    }

    public List<String> input(char c) {
        if (c == '#') {
            String fullTerm = currentInput.toString();
            frequencyMap.put(fullTerm, frequencyMap.getOrDefault(fullTerm, 0) + 1);
            insert(fullTerm);
            currentInput.setLength(0); // reset prefix buffer
            return new ArrayList<>();
        }

        currentInput.append(c);
        TrieNode current = root;
        for (char ch : currentInput.toString().toCharArray()) {
            if (!current.children.containsKey(ch)) {
                return new ArrayList<>();
            }
            current = current.children.get(ch);
        }

        // Sort top3 in descending frequency and lexicographically
        PriorityQueue<SearchTerm> pq = new PriorityQueue<>((a, b) -> {
            if (a.frequency == b.frequency)
                return a.term.compareTo(b.term);
            return b.frequency - a.frequency;
        });
        pq.addAll(current.top3);

        List<String> result = new ArrayList<>();
        while (!pq.isEmpty())
            result.add(pq.poll().term);
        return result;
    }
}

/*
 * | Operation | Time Complexity | Notes |
 * | --------------- | --------------- | ---------------------------- |
 * | `input(char c)` | O(P) | Traverse current prefix `P`, heap of 3 |
 * | `input('#')` | O(L) | Insert `L`-length new term |
 * | `insert(term)` | O(L) | Per character insert + heap update |
 * | `updateTop3()` | O(1) | Max 3-size heap |
 * 
 */

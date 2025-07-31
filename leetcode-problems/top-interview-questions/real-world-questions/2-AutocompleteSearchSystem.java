/*
 * Design a system that takes typed characters and returns the top 3 most
 * frequently searched terms that start with the current prefix.
 */

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

}

class TrieNode {

    Map<Character, TrieNode> children;
    PriorityQueue<SearchTerm> top3;

    public TrieNode() {
        this.children = new HashMap<>();
        this.top3 = new PriorityQueue<>();
    }

    /*
     * Time Complexity:
     * - removeIf(...) → max 3 elements to check: O(3) = O(1).
     * - offer() and poll() in heap of size 3 → O(log 3) = O(1).
     * 
     * ✅ Total time: O(1)
     * 
     * Space Complexity:
     * - Only updates the fixed-size min-heap (size ≤ 3) → O(1).
     * 
     * ✅ Total space: O(1)
     */
    public void updateTop3(SearchTerm searchTerm) {
        this.top3.removeIf(term -> term.term.equals(searchTerm.term));
        this.top3.offer(searchTerm);
        if (this.top3.size() > 3) {
            this.top3.poll();
        }
    }

}

class AutocompleteSearchSystem {

    TrieNode root;

    AutocompleteSearchSystem(List<SearchTerm> searchTerms) {
        this.root = new TrieNode();
        for (SearchTerm searchTerm : searchTerms) {
            this.insert(searchTerm);
        }
    }

    /*
     * Time Complexity:
     * L = length of the search term.
     * For each of L characters:
     * - children.computeIfAbsent() → O(1) (HashMap access).
     * - updateTop3():
     * -- Remove by term.term.equals(...) → O(3) (max 3 elements).
     * -- Add + poll in PriorityQueue of size 3 → O(log 3) ≈ O(1).
     * 
     * ✅ Total time: O(L)
     * 
     * Space Complexity:
     * Each node stores:
     * - A map of children: up to 26 entries.
     * - A heap of size 3.
     * For N terms, total number of nodes created is O(N × L) in the worst case (all
     * terms are unique).
     * 
     * ✅ Total space: O(N × L)
     */
    private void insert(SearchTerm searchTerm) {
        // take each character from search node and add it to the trie
        // while adding at each node check can this be in top 3 and add it if needed
        TrieNode current = root;
        for (char ch : searchTerm.term.toCharArray()) {
            current = current.children.computeIfAbsent(ch, _ -> new TrieNode());
            current.updateTop3(searchTerm);
        }
    }

    /*
     * Time Complexity:
     * - Traverse prefix of length P: O(P).
     * - Iterate through top 3 suggestions → O(3) = O(1).
     * 
     * ✅ Total time: O(P)
     * 
     * Space Complexity:
     * - Output list of top 3 terms → O(3) = O(1).
     * - If you sort the heap or copy it, it can add an extra O(3 log 3) time and
     * space, but that's negligible.
     * 
     * ✅ Total space: O(1) (not counting output list)
     */
    public List<String> getTopSuggestions(String prefix) {
        List<String> suggestions = new ArrayList<>();
        TrieNode current = root;

        for (char ch : prefix.toCharArray()) {
            if (!current.children.containsKey(ch)) {
                return suggestions;
            }
            current = current.children.get(ch);
        }

        for (SearchTerm searchTerm : current.top3) {
            suggestions.add(searchTerm.term);
        }
        return suggestions;
    }

}

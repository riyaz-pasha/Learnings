import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Design an auto-correct system that suggests corrections for misspelled words
based on:
1) Edit distance
2) Word frequency

================================================================================
INTERVIEW FLOW (WHAT I SAY OUT LOUD)
--------------------------------------------------------------------------------
1) We should NOT compare against the entire dictionary
2) Use a Trie to efficiently generate candidates
3) Traverse the Trie while tracking edit distance
4) Rank results using edit distance + frequency
================================================================================
*/

class AutoCorrectSystem {

    /* ========================= TRIE NODE ========================= */

    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        String word;        // full word at terminal node
        int frequency;      // frequency of word
    }

    private final TrieNode root = new TrieNode();

    /* ========================= CONSTRUCTOR ========================= */

    public AutoCorrectSystem(Map<String, Integer> dictionary) {
        for (Map.Entry<String, Integer> entry : dictionary.entrySet()) {
            insert(entry.getKey(), entry.getValue());
        }
    }

    private void insert(String word, int frequency) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node = node.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        node.word = word;
        node.frequency = frequency;
    }

    /* ========================= PUBLIC API ========================= */

    public List<String> suggest(String input, int maxEdits, int topN) {

        PriorityQueue<Candidate> minHeap =
            new PriorityQueue<>(Comparator.comparingDouble(c -> c.score));

        dfs(input, 0, root, maxEdits, minHeap, topN);

        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll().word);
        }
        Collections.reverse(result);
        return result;
    }

    /* ========================= DFS WITH EDIT DISTANCE ========================= */

    private void dfs(
            String input,
            int index,
            TrieNode node,
            int remainingEdits,
            PriorityQueue<Candidate> heap,
            int topN
    ) {

        if (remainingEdits < 0) return;

        // End of input word
        if (index == input.length()) {
            if (node.word != null) {
                double score = score(remainingEdits, node.frequency);
                addCandidate(heap, new Candidate(node.word, score), topN);
            }
            // Allow deletions from Trie
            for (TrieNode child : node.children.values()) {
                dfs(input, index, child, remainingEdits - 1, heap, topN);
            }
            return;
        }

        char currentChar = input.charAt(index);

        // Case 1: Match
        if (node.children.containsKey(currentChar)) {
            dfs(input, index + 1, node.children.get(currentChar),
                remainingEdits, heap, topN);
        }

        // Case 2: Substitution
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            if (entry.getKey() != currentChar) {
                dfs(input, index + 1, entry.getValue(),
                    remainingEdits - 1, heap, topN);
            }
        }

        // Case 3: Insertion (skip input char)
        dfs(input, index + 1, node, remainingEdits - 1, heap, topN);

        // Case 4: Deletion (skip trie char)
        for (TrieNode child : node.children.values()) {
            dfs(input, index, child, remainingEdits - 1, heap, topN);
        }
    }

    /* ========================= SCORING ========================= */

    private double score(int remainingEdits, int frequency) {
        int usedEdits = Math.abs(remainingEdits);
        return -usedEdits + Math.log(frequency + 1);
    }

    private void addCandidate(
            PriorityQueue<Candidate> heap,
            Candidate c,
            int topN
    ) {
        heap.offer(c);
        if (heap.size() > topN) {
            heap.poll();
        }
    }

    /* ========================= HELPER ========================= */

    static class Candidate {
        String word;
        double score;

        Candidate(String word, double score) {
            this.word = word;
            this.score = score;
        }
    }

    /* ========================= DEMO ========================= */

    public static void main(String[] args) {

        Map<String, Integer> dict = Map.of(
            "hello", 5000,
            "help", 3000,
            "hell", 1000,
            "helmet", 800,
            "hero", 2000
        );

        AutoCorrectSystem ac = new AutoCorrectSystem(dict);

        System.out.println(ac.suggest("helo", 2, 3));
        // Expected: ["hello", "help", "hell"]
    }
}

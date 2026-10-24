import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

class AutocompleteSystemOptimized {

    private static final int SUGGESTION_LIMIT = 3;

    private final Map<String, Integer> frequencyBySentence = new HashMap<>();
    private final StringBuilder currentPrefix = new StringBuilder();
    private final TrieNode root = new TrieNode();

    private final class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();

        // Every sentence sharing this node's prefix is indexed here.
        // The comparator uses the current frequency and lexical tie-breaker.
        private final TreeSet<String> candidates = new TreeSet<>((first, second) -> {
            int byFrequency = Integer.compare(
                    frequencyBySentence.get(second),
                    frequencyBySentence.get(first)
            );

            if (byFrequency != 0) {
                return byFrequency;
            }

            // Different sentences with equal frequencies must remain distinct.
            return first.compareTo(second);
        });

        private boolean isSentence;
    }

    public AutocompleteSystemOptimized(String[] sentences, int[] times) {
        for (int i = 0; i < sentences.length; i++) {
            frequencyBySentence.merge(sentences[i], times[i], Integer::sum);
        }

        // Populate all candidate sets only after the initial frequencies
        // have been aggregated, so every set is built with final initial ranks.
        for (String sentence : frequencyBySentence.keySet()) {
            addSentenceToTrie(sentence);
        }
    }

    public List<String> input(char c) {
        if (c == '#') {
            String completedSentence = currentPrefix.toString();

            if (frequencyBySentence.containsKey(completedSentence)) {
                // Critical ordering rule: remove before mutating comparator data.
                removeSentenceFromTrie(completedSentence);
            }

            frequencyBySentence.merge(completedSentence, 1, Integer::sum);
            addSentenceToTrie(completedSentence);

            currentPrefix.setLength(0);
            return List.of();
        }

        currentPrefix.append(c);

        TrieNode prefixNode = findNode(currentPrefix);
        if (prefixNode == null) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>(SUGGESTION_LIMIT);

        // TreeSet iteration follows ranking order, so we stop after three.
        for (String sentence : prefixNode.candidates) {
            suggestions.add(sentence);

            if (suggestions.size() == SUGGESTION_LIMIT) {
                break;
            }
        }

        return List.copyOf(suggestions);
    }

    private TrieNode findNode(CharSequence prefix) {
        TrieNode node = root;

        for (int i = 0; i < prefix.length(); i++) {
            node = node.children.get(prefix.charAt(i));

            if (node == null) {
                return null;
            }
        }

        return node;
    }

    private void addSentenceToTrie(String sentence) {
        TrieNode node = root;
        node.candidates.add(sentence);

        for (char c : sentence.toCharArray()) {
            node = node.children.computeIfAbsent(c, ignored -> new TrieNode());
            node.candidates.add(sentence);
        }

        node.isSentence = true;
    }

    private void removeSentenceFromTrie(String sentence) {
        TrieNode node = root;
        node.candidates.remove(sentence);

        for (char c : sentence.toCharArray()) {
            node = node.children.get(c);
            node.candidates.remove(sentence);
        }
    }
}

/*
 * Complexity:
 *
 * Let:
 *   N = number of distinct sentences,
 *   L = length of the current or committed sentence,
 *   Kp = number of sentences sharing a particular prefix p.
 *
 * Initialization:
 *   O(T log N) in a conservative bound, where T is the total number of
 *   sentence-character occurrences. Each sentence is inserted into the
 *   ordered sets for its prefixes, and each set operation is logarithmic
 *   in the number of candidates at that node.
 *
 * Ordinary input character:
 *   O(L + 3) expected, hence O(L).
 *   - O(L) to navigate from the root to the prefix node.
 *   - O(1) to retrieve at most three candidates once the node is found.
 *
 * Commit of a sentence of length L:
 *   O(L log N) as a conservative upper bound.
 *   - Remove the old sentence from each of its L + 1 prefix sets.
 *   - Increment its frequency.
 *   - Reinsert it into those same sets.
 *
 * Extra space:
 *   O(T + sum of Kp over all Trie prefix nodes).
 *   This can be O(N * L^2) in the worst case when many long sentences
 *   share long prefixes. The frequency map and Trie nodes themselves
 *   require O(T + N) space.
 *
 * Why it is faster:
 *   Each prefix node already knows its candidates in the required order.
 *   A query never traverses the entire matching subtree or sorts matches.
 *
 * Important correctness conditions:
 *   1. Remove a sentence from every affected TreeSet before changing its
 *      frequency, because the comparator depends on the frequency map.
 *   2. Reinsert it after the frequency update.
 *   3. Include the full sentence as a lexical tie-breaker; otherwise two
 *      equal-frequency sentences might compare as equal and one can vanish
 *      from the TreeSet.
 *   4. The root's candidate set is maintained too, even though normal input
 *      queries ask for non-empty prefixes.
 */



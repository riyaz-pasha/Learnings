// we have sentances and frequencies map
// user query is not given all at once. each individual character is given so we have to maintain it in code
// and for each character entered we need to return top 3 sentances.

// case incensitive?
// size of sentances. length of sentance

// total call?

// for we use trie to store sentance and at each trie node we store top 3 sentances at that node.

// fir we traverse all the sentance and build trie node. also create a hashmap to store sentance frequency.

// we create a query variable to store the user entered characters

// when user enters a character other than # then we append it to query and start reaching that prefix.
// once we reached the prefix we return top 3

// when user enters # we first update the sentance frequency 
// then we traverse each character 1 by 1 . at each character node we check if this can be added to top3 or not.
// once we are done we resets the query and return empty list

// at trie node we maintain a min heap with (freq,sentance)
// we push the sentance to the queue. if the size of the min heap > 3 then we pop element from it.
// first preference for freq then lexicographical sentance

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class AutocompleteSystem {

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        List<String> top = new ArrayList<>();
    }

    private final TrieNode root = new TrieNode();
    private final Map<String, Integer> freq = new HashMap<>();

    private TrieNode currNode = root;
    private StringBuilder currQuery = new StringBuilder();

    public AutocompleteSystem(String[] sentences, int[] times) {
        for (int i = 0; i < sentences.length; i++) {
            freq.put(sentences[i], freq.getOrDefault(sentences[i], 0) + times[i]);
            insert(sentences[i]);
        }
    }

    public List<String> input(char c) {
        if (c == '#') {
            String sentence = currQuery.toString();
            freq.put(sentence, freq.getOrDefault(sentence, 0) + 1);

            insert(sentence);

            // reset state
            currQuery.setLength(0);
            currNode = root;

            return new ArrayList<>();
        }

        currQuery.append(c);

        if (currNode == null) {
            return new ArrayList<>();
        }

        currNode = currNode.children.get(c);

        if (currNode == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(currNode.top);
    }

    private void insert(String sentence) {
        TrieNode node = root;

        for (char ch : sentence.toCharArray()) {
            node.children.putIfAbsent(ch, new TrieNode());
            node = node.children.get(ch);

            updateTop(node, sentence);
        }
    }

    private void updateTop(TrieNode node, String sentence) {
        if (!node.top.contains(sentence)) {
            node.top.add(sentence);
        }

        node.top.sort((a, b) -> {
            int fa = freq.get(a);
            int fb = freq.get(b);

            if (fa != fb) return Integer.compare(fb, fa); // higher freq first
            return a.compareTo(b); // lexicographically smaller first
        });

        if (node.top.size() > 3) {
            node.top.remove(node.top.size() - 1);
        }
    }
}



/**
 * PROBLEM ANALYSIS: DESIGN SEARCH AUTOCOMPLETE SYSTEM
 * ====================================================
 * 
 * PROBLEM UNDERSTANDING:
 * Design a search autocomplete system that:
 * 1. Suggests top 3 historical hot sentences based on current input
 * 2. Updates the system when user completes a sentence
 * 3. Handles real-time character-by-character input
 * 
 * Operations:
 * - Constructor: Initialize with sentences and their frequencies (hot degree)
 * - input(c): 
 *   - If c is a letter: add to current search, return top 3 suggestions
 *   - If c is '#': save current search, reset, return empty list
 * 
 * Ranking Rules:
 * 1. Higher hot degree comes first
 * 2. If tie, lexicographically smaller sentence comes first
 * 
 * KEY CHALLENGES:
 * 1. Efficiently find top K sentences with prefix matching
 * 2. Handle real-time character-by-character search
 * 3. Update frequencies dynamically
 * 4. Optimize for frequent queries vs infrequent updates
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Clarify requirements
 * - "Do we return at most 3 or exactly 3?" (At most 3)
 * - "Can sentences contain special characters?" (Usually just letters and spaces)
 * - "Is the input case-sensitive?" (Usually yes)
 * - "What's the expected dataset size?" (Affects data structure choice)
 * 
 * Step 2: Identify the core problem
 * - This is essentially: Prefix Search + Top K + Dynamic Updates
 * - Need a data structure that supports:
 *   a) Prefix matching
 *   b) Frequency tracking
 *   c) Fast retrieval of top K
 * 
 * Step 3: Consider data structures
 * - Trie: Perfect for prefix matching
 * - HashMap: Track sentence -> frequency
 * - Heap/PriorityQueue: Get top K
 * - Combination: Trie + HashMap is powerful
 * 
 * Step 4: Design approaches
 * - Approach 1: HashMap + Linear Search (Simple)
 * - Approach 2: Trie with sentences at nodes (Memory intensive)
 * - Approach 3: Trie + HashMap (OPTIMAL - Balanced)
 * - Approach 4: Inverted Index (For very large datasets)
 * 
 * Step 5: Optimize for the operations
 * - input() called frequently -> needs to be fast
 * - Constructor called once -> can do more preprocessing
 * - Trade-off: precompute vs compute on-demand
 */

/**
 * APPROACH 1: HASHMAP + LINEAR SEARCH (SIMPLE)
 * =============================================
 * 
 * INTUITION:
 * - Store all sentences with frequencies in a HashMap
 * - On input(), filter sentences by prefix and sort
 * - Simple but not optimal for large datasets
 * 
 * PROS:
 * - Very simple to implement
 * - Easy to understand
 * - Good starting point in interview
 * 
 * CONS:
 * - O(n) for each input character (n = total sentences)
 * - Sorting overhead for every query
 * - Not scalable
 */
class AutocompleteSystem1 {
    
    private Map<String, Integer> sentenceFreq;
    private StringBuilder currentInput;
    
    /**
     * Initialize with historical data
     * Time: O(n) where n = number of sentences
     */
    public AutocompleteSystem1(String[] sentences, int[] times) {
        sentenceFreq = new HashMap<>();
        currentInput = new StringBuilder();
        
        for (int i = 0; i < sentences.length; i++) {
            sentenceFreq.put(sentences[i], times[i]);
        }
    }
    
    /**
     * Process input character
     * Time: O(n log n) where n = number of matching sentences
     */
    public List<String> input(char c) {
        if (c == '#') {
            // Save current sentence
            String sentence = currentInput.toString();
            sentenceFreq.put(sentence, sentenceFreq.getOrDefault(sentence, 0) + 1);
            currentInput = new StringBuilder();
            return new ArrayList<>();
        }
        
        // Add character to current input
        currentInput.append(c);
        String prefix = currentInput.toString();
        
        // Find all sentences with this prefix
        List<String> candidates = new ArrayList<>();
        for (String sentence : sentenceFreq.keySet()) {
            if (sentence.startsWith(prefix)) {
                candidates.add(sentence);
            }
        }
        
        // Sort by frequency (desc) then lexicographically (asc)
        Collections.sort(candidates, (a, b) -> {
            int freqA = sentenceFreq.get(a);
            int freqB = sentenceFreq.get(b);
            if (freqA != freqB) {
                return freqB - freqA;  // Higher frequency first
            }
            return a.compareTo(b);  // Lexicographically smaller first
        });
        
        // Return top 3
        return candidates.subList(0, Math.min(3, candidates.size()));
    }
}

/**
 * APPROACH 2: TRIE (OPTIMAL - RECOMMENDED FOR INTERVIEWS)
 * ========================================================
 * 
 * INTUITION:
 * - Build a Trie for efficient prefix matching
 * - Store frequency in HashMap separately
 * - On each input, traverse Trie and collect matching sentences
 * 
 * KEY OPTIMIZATION:
 * - Trie reduces search space dramatically
 * - Only search subtree of current prefix
 * - Much better than linear search through all sentences
 * 
 * PROS:
 * - Efficient prefix matching O(p) where p = prefix length
 * - Scales well with number of sentences
 * - Industry standard for autocomplete
 * 
 * CONS:
 * - More complex implementation
 * - Higher space complexity
 * - Still need to collect and sort matching sentences
 */
class AutocompleteSystem2 {
    
    // Trie node
    class TrieNode {
        Map<Character, TrieNode> children;
        boolean isEnd;
        
        TrieNode() {
            children = new HashMap<>();
            isEnd = false;
        }
    }
    
    private TrieNode root;
    private Map<String, Integer> sentenceFreq;
    private StringBuilder currentInput;
    
    /**
     * Initialize with historical data
     * Time: O(n * m) where n = sentences, m = avg sentence length
     */
    public AutocompleteSystem2(String[] sentences, int[] times) {
        root = new TrieNode();
        sentenceFreq = new HashMap<>();
        currentInput = new StringBuilder();
        
        for (int i = 0; i < sentences.length; i++) {
            insertSentence(sentences[i]);
            sentenceFreq.put(sentences[i], times[i]);
        }
    }
    
    /**
     * Insert sentence into Trie
     */
    private void insertSentence(String sentence) {
        TrieNode node = root;
        for (char c : sentence.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEnd = true;
    }
    
    /**
     * Process input character
     * Time: O(p + m log m) where p = prefix length, m = matching sentences
     */
    public List<String> input(char c) {
        if (c == '#') {
            String sentence = currentInput.toString();
            insertSentence(sentence);
            sentenceFreq.put(sentence, sentenceFreq.getOrDefault(sentence, 0) + 1);
            currentInput = new StringBuilder();
            return new ArrayList<>();
        }
        
        currentInput.append(c);
        
        // Find all sentences with current prefix
        List<String> candidates = new ArrayList<>();
        String prefix = currentInput.toString();
        
        // Navigate to prefix node
        TrieNode node = root;
        for (char ch : prefix.toCharArray()) {
            if (!node.children.containsKey(ch)) {
                return new ArrayList<>();  // No matches
            }
            node = node.children.get(ch);
        }
        
        // DFS to collect all sentences from this node
        collectSentences(node, prefix, candidates);
        
        // Sort candidates
        Collections.sort(candidates, (a, b) -> {
            int freqA = sentenceFreq.get(a);
            int freqB = sentenceFreq.get(b);
            if (freqA != freqB) {
                return freqB - freqA;
            }
            return a.compareTo(b);
        });
        
        // Return top 3
        return candidates.subList(0, Math.min(3, candidates.size()));
    }
    
    /**
     * DFS to collect all sentences in subtree
     */
    private void collectSentences(TrieNode node, String prefix, List<String> result) {
        if (node.isEnd) {
            result.add(prefix);
        }
        
        for (char c : node.children.keySet()) {
            collectSentences(node.children.get(c), prefix + c, result);
        }
    }
}

/**
 * APPROACH 3: TRIE WITH TOP-K AT EACH NODE (SPACE-TIME TRADEOFF)
 * ===============================================================
 * 
 * INTUITION:
 * - Store top K sentences at EVERY node in the Trie
 * - Precompute during construction and updates
 * - Query becomes O(1) - just return stored top K
 * 
 * PROS:
 * - Ultra-fast queries O(p) where p = prefix length
 * - No sorting needed during query
 * - Best for read-heavy workloads
 * 
 * CONS:
 * - High space complexity O(n * m * k)
 * - Updates are expensive O(m * k log k)
 * - Not suitable if data changes frequently
 */
class AutocompleteSystem3 {
    
    class TrieNode {
        Map<Character, TrieNode> children;
        // Store top 3 sentences at this node
        List<String> topSentences;
        
        TrieNode() {
            children = new HashMap<>();
            topSentences = new ArrayList<>();
        }
    }
    
    private TrieNode root;
    private Map<String, Integer> sentenceFreq;
    private StringBuilder currentInput;
    
    public AutocompleteSystem3(String[] sentences, int[] times) {
        root = new TrieNode();
        sentenceFreq = new HashMap<>();
        currentInput = new StringBuilder();
        
        for (int i = 0; i < sentences.length; i++) {
            sentenceFreq.put(sentences[i], times[i]);
            insertAndUpdate(sentences[i]);
        }
    }
    
    /**
     * Insert sentence and update top K at each node
     */
    private void insertAndUpdate(String sentence) {
        TrieNode node = root;
        for (char c : sentence.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
            
            // Update top sentences at this node
            updateTopSentences(node, sentence);
        }
    }
    
    /**
     * Update top K sentences at a node
     */
    private void updateTopSentences(TrieNode node, String sentence) {
        if (!node.topSentences.contains(sentence)) {
            node.topSentences.add(sentence);
        }
        
        // Sort and keep top 3
        Collections.sort(node.topSentences, (a, b) -> {
            int freqA = sentenceFreq.get(a);
            int freqB = sentenceFreq.get(b);
            if (freqA != freqB) {
                return freqB - freqA;
            }
            return a.compareTo(b);
        });
        
        if (node.topSentences.size() > 3) {
            node.topSentences = node.topSentences.subList(0, 3);
        }
    }
    
    public List<String> input(char c) {
        if (c == '#') {
            String sentence = currentInput.toString();
            sentenceFreq.put(sentence, sentenceFreq.getOrDefault(sentence, 0) + 1);
            insertAndUpdate(sentence);
            currentInput = new StringBuilder();
            return new ArrayList<>();
        }
        
        currentInput.append(c);
        
        // Navigate to prefix node
        TrieNode node = root;
        for (char ch : currentInput.toString().toCharArray()) {
            if (!node.children.containsKey(ch)) {
                return new ArrayList<>();
            }
            node = node.children.get(ch);
        }
        
        // Return precomputed top sentences
        return new ArrayList<>(node.topSentences);
    }
}

/**
 * RECOMMENDED SOLUTION (APPROACH 2 - BALANCED)
 * =============================================
 * 
 * This is the most practical solution that balances:
 * - Implementation complexity
 * - Time efficiency
 * - Space efficiency
 * - Maintainability
 */
class AutocompleteSystem4 {
    
    class TrieNode {
        Map<Character, TrieNode> children;
        boolean isEnd;
        
        TrieNode() {
            children = new HashMap<>();
            isEnd = false;
        }
    }
    
    private TrieNode root;
    private Map<String, Integer> sentenceFreq;
    private StringBuilder currentInput;
    private TrieNode currentNode;  // Optimization: track current position
    
    /**
     * Initialize the system with historical sentences and their frequencies
     * 
     * @param sentences - array of historical search sentences
     * @param times - corresponding hot degrees (frequencies)
     * 
     * Time: O(n * m) where n = number of sentences, m = average length
     * Space: O(n * m) for Trie
     */
    public AutocompleteSystem4(String[] sentences, int[] times) {
        root = new TrieNode();
        sentenceFreq = new HashMap<>();
        currentInput = new StringBuilder();
        currentNode = root;
        
        // Build initial Trie and frequency map
        for (int i = 0; i < sentences.length; i++) {
            insertSentence(sentences[i]);
            sentenceFreq.put(sentences[i], times[i]);
        }
    }
    
    /**
     * Insert a sentence into the Trie
     */
    private void insertSentence(String sentence) {
        TrieNode node = root;
        for (char c : sentence.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEnd = true;
    }
    
    /**
     * Process one character of user input
     * 
     * @param c - input character or '#' to end current search
     * @return top 3 matching sentences or empty list if c is '#'
     * 
     * Time: O(p + m log m) where p = prefix length, m = matching sentences
     * Space: O(m) for storing candidates
     */
    public List<String> input(char c) {
        // End of sentence - save and reset
        if (c == '#') {
            String sentence = currentInput.toString();
            
            // Insert new sentence into Trie
            insertSentence(sentence);
            
            // Update frequency
            sentenceFreq.put(sentence, sentenceFreq.getOrDefault(sentence, 0) + 1);
            
            // Reset for next search
            currentInput = new StringBuilder();
            currentNode = root;
            
            return new ArrayList<>();
        }
        
        // Add character to current search
        currentInput.append(c);
        
        // Navigate Trie - if path doesn't exist, no matches
        if (currentNode != null && currentNode.children.containsKey(c)) {
            currentNode = currentNode.children.get(c);
        } else {
            currentNode = null;
            return new ArrayList<>();
        }
        
        // Collect all sentences with current prefix
        List<String> candidates = new ArrayList<>();
        String prefix = currentInput.toString();
        collectAllSentences(currentNode, prefix, candidates);
        
        // Sort by frequency (desc) then lexicographically (asc)
        Collections.sort(candidates, (a, b) -> {
            int freqA = sentenceFreq.get(a);
            int freqB = sentenceFreq.get(b);
            
            // Higher frequency first
            if (freqA != freqB) {
                return freqB - freqA;
            }
            
            // If same frequency, lexicographically smaller first
            return a.compareTo(b);
        });
        
        // Return top 3 results
        return candidates.subList(0, Math.min(3, candidates.size()));
    }
    
    /**
     * DFS to collect all complete sentences from current node
     */
    private void collectAllSentences(TrieNode node, String prefix, List<String> result) {
        if (node == null) {
            return;
        }
        
        // If this is end of a sentence, add it
        if (node.isEnd) {
            result.add(prefix);
        }
        
        // Recursively collect from children
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            collectAllSentences(entry.getValue(), prefix + entry.getKey(), result);
        }
    }
}

/**
 * APPROACH 4: OPTIMIZED WITH PRIORITY QUEUE
 * ==========================================
 * 
 * Use heap to maintain only top K during collection
 * More space efficient when there are many matches
 */
class AutocompleteSystemOptimized {
    
    class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
    }
    
    class Pair {
        String sentence;
        int freq;
        
        Pair(String s, int f) {
            sentence = s;
            freq = f;
        }
    }
    
    private TrieNode root;
    private Map<String, Integer> sentenceFreq;
    private StringBuilder currentInput;
    
    public AutocompleteSystemOptimized(String[] sentences, int[] times) {
        root = new TrieNode();
        sentenceFreq = new HashMap<>();
        currentInput = new StringBuilder();
        
        for (int i = 0; i < sentences.length; i++) {
            insertSentence(sentences[i]);
            sentenceFreq.put(sentences[i], times[i]);
        }
    }
    
    private void insertSentence(String sentence) {
        TrieNode node = root;
        for (char c : sentence.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEnd = true;
    }
    
    public List<String> input(char c) {
        if (c == '#') {
            String sentence = currentInput.toString();
            insertSentence(sentence);
            sentenceFreq.put(sentence, sentenceFreq.getOrDefault(sentence, 0) + 1);
            currentInput = new StringBuilder();
            return new ArrayList<>();
        }
        
        currentInput.append(c);
        String prefix = currentInput.toString();
        
        // Navigate to prefix
        TrieNode node = root;
        for (char ch : prefix.toCharArray()) {
            if (!node.children.containsKey(ch)) {
                return new ArrayList<>();
            }
            node = node.children.get(ch);
        }
        
        // Use min heap to keep top 3
        PriorityQueue<Pair> minHeap = new PriorityQueue<>((a, b) -> {
            if (a.freq != b.freq) {
                return a.freq - b.freq;  // Min heap by frequency
            }
            return b.sentence.compareTo(a.sentence);  // Max lexicographically
        });
        
        // Collect sentences and maintain top 3
        List<String> allMatches = new ArrayList<>();
        collectAllSentences(node, prefix, allMatches);
        
        for (String s : allMatches) {
            minHeap.offer(new Pair(s, sentenceFreq.get(s)));
            if (minHeap.size() > 3) {
                minHeap.poll();
            }
        }
        
        // Extract results in reverse order
        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(0, minHeap.poll().sentence);
        }
        
        return result;
    }
    
    private void collectAllSentences(TrieNode node, String prefix, List<String> result) {
        if (node.isEnd) {
            result.add(prefix);
        }
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            collectAllSentences(entry.getValue(), prefix + entry.getKey(), result);
        }
    }
}

/**
 * TEST CASES AND EXAMPLES
 * ========================
 */
class TestAutocompleteSystem {
    
    public static void testSystem(String testName, AutocompleteSystem system) {
        System.out.println("\n=== " + testName + " ===");
        
        // Simulate user typing "i" then " " then "a" then "#"
        System.out.println("User types 'i':");
        System.out.println(system.input('i'));
        
        System.out.println("\nUser types ' ' (space):");
        System.out.println(system.input(' '));
        
        System.out.println("\nUser types 'a':");
        System.out.println(system.input('a'));
        
        System.out.println("\nUser types '#' (complete):");
        System.out.println(system.input('#'));
        
        System.out.println("\nUser types 'i' again:");
        System.out.println(system.input('i'));
        
        System.out.println("\nUser types ' ':");
        System.out.println(system.input(' '));
        
        System.out.println("\nUser types 'a':");
        System.out.println(system.input('a'));
        
        System.out.println("\nUser types '#':");
        System.out.println(system.input('#'));
    }
    
    public static void main(String[] args) {
        System.out.println("=== AUTOCOMPLETE SYSTEM TEST ===");
        
        // Test Case 1: Basic functionality
        String[] sentences1 = {
            "i love you",
            "island",
            "iroman",
            "i love leetcode"
        };
        int[] times1 = {5, 3, 2, 2};
        
        AutocompleteSystem system1 = new AutocompleteSystem(sentences1, times1);
        
        System.out.println("\nTest 1: Basic autocomplete");
        System.out.println("Initial sentences: " + Arrays.toString(sentences1));
        System.out.println("Initial frequencies: " + Arrays.toString(times1));
        
        testSystem("Test 1", system1);
        
        // Test Case 2: Duplicate handling
        System.out.println("\n\n=== Test 2: Same sentence multiple times ===");
        String[] sentences2 = {"hello", "hello world", "help"};
        int[] times2 = {5, 3, 1};
        
        AutocompleteSystem system2 = new AutocompleteSystem(sentences2, times2);
        
        System.out.println("Typing 'h':");
        System.out.println(system2.input('h'));
        
        System.out.println("\nTyping 'e':");
        System.out.println(system2.input('e'));
        
        System.out.println("\nTyping 'l':");
        System.out.println(system2.input('l'));
        
        System.out.println("\nTyping 'l':");
        System.out.println(system2.input('l'));
        
        System.out.println("\nTyping 'o':");
        System.out.println(system2.input('o'));
        
        System.out.println("\nTyping '#':");
        System.out.println(system2.input('#'));
        
        // Now "hello" should have frequency 6
        System.out.println("\nTyping 'h' again:");
        System.out.println(system2.input('h'));
        
        // Test Case 3: Edge cases
        System.out.println("\n\n=== Test 3: No matches ===");
        String[] sentences3 = {"abc", "def"};
        int[] times3 = {1, 1};
        
        AutocompleteSystem system3 = new AutocompleteSystem(sentences3, times3);
        
        System.out.println("Typing 'x' (no match):");
        System.out.println(system3.input('x'));
        
        System.out.println("\nTyping 'y' (no match):");
        System.out.println(system3.input('y'));
        
        System.out.println("\nTyping '#':");
        System.out.println(system3.input('#'));
        
        // Demonstrate ranking
        System.out.println("\n\n=== Test 4: Ranking demonstration ===");
        String[] sentences4 = {
            "aa",
            "ab",
            "abc"
        };
        int[] times4 = {1, 1, 1};  // Same frequency
        
        AutocompleteSystem system4 = new AutocompleteSystem(sentences4, times4);
        
        System.out.println("Typing 'a' (all have same frequency):");
        List<String> result = system4.input('a');
        System.out.println(result);
        System.out.println("Expected order: [aa, ab, abc] (lexicographic)");
    }
}

/**
 * COMPLEXITY ANALYSIS
 * ===================
 * 
 * Approach 1 - HashMap + Linear Search:
 * - Constructor: O(n)
 * - input():     O(n log n) - check all sentences, sort matches
 * - Space:       O(n * m) - store all sentences
 * 
 * Approach 2 - Trie (RECOMMENDED):
 * - Constructor: O(n * m) - build Trie
 * - input():     O(p + k + k log k) where:
 *                p = prefix length (navigate Trie)
 *                k = matching sentences (collect and sort)
 * - Space:       O(n * m) - Trie nodes
 * 
 * Approach 3 - Trie with Top-K at nodes:
 * - Constructor: O(n * m * k log k) - update top K at each node
 * - input():     O(p) - just navigate and return stored top K
 * - Space:       O(n * m * k) - store top K at each node
 * 
 * Approach 4 - Optimized with Priority Queue:
 * - Constructor: O(n * m)
 * - input():     O(p + k log 3) - heap maintains only top 3
 * - Space:       O(n * m)
 * 
 * Where:
 * - n = number of sentences
 * - m = average sentence length
 * - p = current prefix length
 * - k = number of matching sentences
 * 
 * 
 * INTERVIEW STRATEGY
 * ==================
 * 
 * 1. CLARIFY THE PROBLEM:
 *    "Let me make sure I understand:
 *     - User types character by character
 *     - We return top 3 suggestions after each character
 *     - '#' marks end of sentence
 *     - Ranking is by frequency, then lexicographically
 *     - Is that correct?"
 * 
 * 2. DISCUSS APPROACHES (START SIMPLE):
 *    "I can think of a few approaches:
 *     
 *     Simple: Store sentences in HashMap, filter by prefix on each input.
 *     - Easy to implement
 *     - But O(n) for each character, not efficient
 *     
 *     Better: Use a Trie for prefix matching.
 *     - O(p) to navigate to prefix
 *     - Only search subtree of matches
 *     - This is the standard approach for autocomplete
 *     
 *     Advanced: Precompute top K at each Trie node.
 *     - Ultra-fast queries O(p)
 *     - But expensive updates and high memory
 *     
 *     I recommend the Trie approach - good balance of efficiency and simplicity."
 * 
 * 3. EXPLAIN THE TRIE SOLUTION:
 *    "Here's how it works:
 *     
 *     Construction:
 *     - Build Trie from all sentences
 *     - Store frequencies in separate HashMap
 *     
 *     Input processing:
 *     - Navigate Trie following input characters
 *     - When we reach current prefix, DFS to collect all completions
 *     - Sort by frequency and lexicographic order
 *     - Return top 3
 *     
 *     On '#':
 *     - Insert new sentence into Trie
 *     - Update frequency
 *     - Reset current input"
 * 
 * 4. WALK THROUGH EXAMPLE:
 *    Initial: ["i love you", "island", "iroman", "i love leetcode"]
 *             Frequencies: [5, 3, 2, 2]
 *    
 *    Trie structure:
 *          root
 *           |
 *           i
 *          / \
 *         s  (space)
 *        ...  |
 *           love
 *          /    \
 *        you  leetcode
 *    
 *    User types 'i':
 *    - Navigate to 'i' node
 *    - Collect: ["i love you", "island", "iroman", "i love leetcode"]
 *    - Sort: ["i love you"(5), "island"(3), "i love leetcode"(2)]
 *    - Return top 3
 *    
 *    User types ' ' (space):
 *    - Navigate to 'i' -> ' ' node
 *    - Collect: ["i love you", "i love leetcode"]
 *    - Sort and return
 * 
 * 5. CODE INCREMENTALLY:
 *    - First: Define TrieNode structure
 *    - Second: Implement insert
 *    - Third: Implement search/collect
 *    - Fourth: Add sorting logic
 *    - Finally: Handle '#' case
 * 
 * 6. DISCUSS OPTIMIZATIONS:
 *    "Some optimizations we could add:
 *     
 *     1. Cache current Trie position between inputs
 *        - Avoid re-navigating from root each time
 *        - Just move one level down per character
 *     
 *     2. Limit collection to top K during DFS
 *        - Use min-heap of size 3
 *        - Avoids collecting and sorting all matches
 *     
 *     3. Precompute top K at nodes (if reads >> writes)
 *        - O(1) query time
 *        - Trade memory for speed
 *     
 *     4. Use separate thread for updates
 *        - Don't block queries during insert"
 * 
 * 7. EDGE CASES TO HANDLE:
 *    ✓ Empty input
 *    ✓ No matching sentences
 *    ✓ Less than 3 matches
 *    ✓ Duplicate sentences (frequency update)
 *    ✓ Special characters in sentences
 *    ✓ Very long sentences
 *    ✓ Case sensitivity
 * 
 * 8. FOLLOW-UP QUESTIONS:
 *    Q: "How would you handle very large datasets?"
 *    A: "Could use:
 *        - Distributed Trie across multiple servers
 *        - Caching layer for popular prefixes
 *        - Approximate algorithms (LSH, MinHash)
 *        - Database with full-text search (Elasticsearch)"
 *    
 *    Q: "What about typos?"
 *    A: "Could use:
 *        - Edit distance (Levenshtein)
 *        - Fuzzy search in Trie
 *        - Phonetic matching (Soundex)
 *        - Machine learning ranking"
 *    
 *    Q: "How to personalize suggestions?"
 *    A: "Could:
 *        - Weight by user's search history
 *        - Use collaborative filtering
 *        - A/B test different ranking algorithms"
 *    
 *    Q: "What about multi-language support?"
 *    A: "Would need:
 *        - Unicode support in Trie
 *        - Language-specific tokenization
 *        - Locale-aware sorting"
 * 
 * 9. PRODUCTION CONSIDERATIONS:
 *    - Thread safety (concurrent reads/writes)
 *    - Persistence (save/load Trie)
 *    - Memory limits (LRU cache for Trie nodes)
 *    - Monitoring (query latency, cache hit rate)
 *    - A/B testing (ranking algorithms)
 * 
 * 10. COMMON MISTAKES:
 *     ✗ Not resetting currentInput on '#'
 *     ✗ Wrong sorting comparator (frequency vs lexicographic)
 *     ✗ Not handling no matches case
 *     ✗ Inefficient string concatenation in DFS
 *     ✗ Not updating Trie when adding new sentence
 *     ✗ Off-by-one in returning top K
 * 
 * RECOMMENDED APPROACH FOR INTERVIEW:
 * ====================================
 * Start with Approach 1 (HashMap) to show understanding,
 * then immediately pivot to Approach 2 (Trie) as the
 * optimal solution. Mention Approach 3 as an optimization
 * for read-heavy workloads if time permits.
 */

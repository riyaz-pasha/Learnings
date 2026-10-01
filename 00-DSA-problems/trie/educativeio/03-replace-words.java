/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: REPLACE WORDS (Prefix Matching)
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Can the dictionary contain redundant prefixes, where one prefix is a prefix of another (e.g., 'ca' and 'cat')?"
 *   (Crucial: confirms we must actively stop at the shortest match and not just any match.)
 * - "Is the dictionary static while we process many sentences, or do we get a new dictionary per sentence?"
 *   (If the dictionary is reused across millions of sentences, the upfront cost of building a Trie is negligible. If it's one-off, HashSet might be competitive.)
 * - "What if the prefix is exactly the same length as the word?"
 *   (Confirms 'replace' just means keeping the word as-is but functionally substituting it.)
 * - "Are there punctuation marks in the sentence, or strictly lowercase letters and spaces?"
 *   (Prevents writing complex regex or string cleaning logic if we only need `sentence.split(" ")`.)
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * We need to map words in a sentence to their shortest matching prefix from a dictionary. 
 * The binding constraint is doing this efficiently for every word without redundantly 
 * checking non-matching prefixes or creating excessive intermediate strings.
 * 
 * APPROACH 1: Nested Loops (The Brute Force)
 * 1. What I'd naturally try: Split the sentence into an array of words. For each word, 
 *    loop through every single prefix in the dictionary and check `word.startsWith(prefix)`.
 *    Keep track of the shortest valid prefix found.
 * 2. Why it works: It exhaustively checks all possibilities, guaranteeing we find the shortest.
 * 3. Why it's too slow: For every word, we scan the entire dictionary. If the dictionary 
 *    has 1,000 words and the sentence has 100 words, we do 100,000 `startsWith` checks.
 * 4. What work is repeated: We repeatedly check words against dictionary prefixes that don't 
 *    even share the same first letter!
 * 5. Time Complexity: O(N * D * L) 
 *    — where N is words in the sentence, D is dictionary size, and L is max prefix length. 
 *    We do N*D comparisons, each taking up to L characters.
 * 6. Space Complexity: O(N * M) 
 *    — storing the split sentence array and the result, where M is max word length.
 * 
 * APPROACH 2: HashSet with Prefix Slicing (The Intermediate Improvement)
 * 1. What I'd naturally try next: To avoid scanning the whole dictionary, let's put the 
 *    dictionary into a `HashSet`. Then, for a word like "cattle", I check if "c", "ca", 
 *    "cat", etc., exist in the set.
 * 2. Why it works: Hash lookups are O(1). By checking substrings of increasing length 
 *    (1 to word.length), the *first* match we hit in the HashSet is guaranteed to be the shortest!
 * 3. Why it's costly: Substring creation. In modern Java, `word.substring(0, i)` creates a 
 *    brand new String object, copying the characters. Doing this for every character of 
 *    every word causes massive garbage collection overhead.
 * 4. What property removes the bottleneck: The realization that checking "ca" after checking 
 *    "c" re-processes the 'c'. We need a structure where we can check "c", then just take 
 *    one step to check "ca" without creating new strings.
 * 5. Time Complexity: O(D * L + N * M^2) 
 *    — O(D * L) to hash the dictionary. For N words of length M, we make M substrings, 
 *    each taking O(M) time to copy and hash.
 * 6. Space Complexity: O(D * L + N * M) 
 *    — Storing all dictionary strings in a HashSet, plus the output sentence.
 * 
 * APPROACH 3: The Trie / Prefix Tree (The Optimal Solution)
 * 1. The defining observation: A Trie naturally models strings character by character. 
 *    If we insert the dictionary into a Trie, processing a word in the sentence just 
 *    means walking down the Trie. 
 * 2. Why it works: The moment we step on a TrieNode marked `isEndOfWord`, we stop! 
 *    That is inherently the shortest prefix. If we fall off the tree before hitting 
 *    an end node, no prefix exists. No substrings are created.
 * 3. Time Complexity: 
 *    - O(D * L + N * M) — where D is dict size, L is max prefix length, N is sentence words, 
 *      M is max sentence word length. We build the tree O(D*L), then process exactly the 
 *      characters of the sentence O(N*M), stopping early when possible.
 * 4. Space Complexity: 
 *    - O(D * L + S) — where S is total sentence length. We store at most D*L nodes in the 
 *      Trie, plus a StringBuilder for the output string.
 * 
 * [What I'd write in an interview]
 * I would write the Trie approach. It is the textbook data structure for prefix matching. 
 * While the HashSet approach works and is easier to code, the Trie demonstrates an 
 * understanding of how to avoid the hidden O(M^2) cost of string slicing in Java.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - Multiple prefixes for one word: Dict has "a", "aa", "aaa". Word is "aaaa". 
 *   (Must stop at "a" immediately).
 * - No matching prefix: Word is "xyz", Trie falls off immediately. (Return original word).
 * - Exact match: Dict has "apple", word is "apple". (Functions correctly).
 * - Prefix is longer than the sentence word: Dict has "apples", word is "app". 
 *   (Trie will finish the word without hitting `isEndOfWord`, returns original word).
 * - Single letter word & single letter prefix. (Boundary limits).
 * 
 * 
 * 4. KEY INSIGHTS, EXAMPLES & DRY RUN
 * ----------------------------------------------------------------------------
 * [Key Insight]
 * Short-circuit evaluation in a Trie: In a standard Trie `search()`, we must reach the 
 * end of the input word. In this problem, we return `true` (and the prefix) the very 
 * moment we see `isEndOfWord = true`, completely ignoring the rest of the input word.
 * 
 * [Dry Run: Dict=["cat", "bat", "rat"], Word="cattle"]
 * 1. Process 'c': root -> 'c' (node exists, isEndOfWord=false)
 * 2. Process 'a': 'c' -> 'a' (node exists, isEndOfWord=false)
 * 3. Process 't': 'a' -> 't' (node exists, isEndOfWord=TRUE!)
 * 4. Stop immediately. We don't care about 't','l','e'. 
 * 5. Return the substring built so far: "cat".
 * 
 * [Pitfalls]
 * - Creating a TrieNode that stores a String `prefix` at every node to avoid using a StringBuilder. 
 *   This works but bloats memory. Better to just pass an index back, or use the original 
 *   word's substring(0, length) since we only do it ONCE per word, not M times.
 * - Forgetting to re-append spaces between words when building the final sentence.
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: How would you change this if we needed the LONGEST matching prefix?
 * A: Instead of stopping at the first `isEndOfWord` node, keep traversing the Trie until 
 *    we fall off or the word ends. Maintain a `lastSeenMatchIndex` variable. Update it 
 *    every time we pass an `isEndOfWord` node. When the traversal ends, return the 
 *    substring up to `lastSeenMatchIndex`.
 * 
 * Q: What if the sentence is too massive to fit in memory (e.g., a 10GB text file)?
 * A: I would change the signature to process an `InputStream` and an `OutputStream`. 
 *    I'd read characters one by one. When hitting a space, I evaluate the buffer against 
 *    the Trie, write the result to the OutputStream, clear the buffer, and continue. 
 *    The Trie easily fits in memory, so memory usage drops to near-zero.
 */

import java.util.Arrays;
import java.util.List;

public class ReplaceWordsStudyNote {

    // ============================================================================
    // TRIE IMPLEMENTATION
    // ============================================================================
    static class TrieNode {
        TrieNode[] children;
        boolean isEndOfWord;

        public TrieNode() {
            // 26 lowercase English letters, as per constraints
            this.children = new TrieNode[26];
            this.isEndOfWord = false;
        }
    }

    static class Trie {
        TrieNode root;

        public Trie() {
            root = new TrieNode();
        }

        /**
         * Inserts a dictionary prefix into the Trie.
         */
        public void insert(String word) {
            TrieNode curr = root;
            for (int i = 0; i < word.length(); i++) {
                int index = word.charAt(i) - 'a';
                if (curr.children[index] == null) {
                    curr.children[index] = new TrieNode();
                }
                curr = curr.children[index];
            }
            curr.isEndOfWord = true; // Mark the end of the dictionary prefix
        }

        /**
         * Returns the shortest prefix of the word that exists in the Trie.
         * If no prefix exists, returns the original word.
         */
        public String findShortestPrefix(String word) {
            TrieNode curr = root;
            
            // Walk down the Trie using the characters of the sentence word
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                int index = ch - 'a';

                // If at any point we hit a node marked as the end of a prefix,
                // we've found the SHORTEST prefix. We stop immediately!
                if (curr.isEndOfWord) {
                    // Creating ONE substring at the very end is efficient O(i)
                    return word.substring(0, i);
                }

                // If the path breaks (e.g., we look for 'w' but only 'c','b','r' exist),
                // no prefix matches. Break early.
                if (curr.children[index] == null) {
                    break;
                }

                // Move deeper into the Trie
                curr = curr.children[index];
            }
            
            // If we loop through the whole word without hitting isEndOfWord, 
            // or if the path broke, return the word unchanged.
            return word;
        }
    }

    // ============================================================================
    // MAIN ALGORITHM
    // ============================================================================
    public static String replaceWords(List<String> dictionary, String sentence) {
        Trie trie = new Trie();
        
        // 1. Build the Trie with all dictionary roots
        for (String prefix : dictionary) {
            trie.insert(prefix);
        }

        // 2. Split the sentence into words
        String[] words = sentence.split(" ");
        StringBuilder result = new StringBuilder();

        // 3. Process each word
        for (int i = 0; i < words.length; i++) {
            // Find the replacement (or the word itself if no replacement exists)
            String replacement = trie.findShortestPrefix(words[i]);
            result.append(replacement);
            
            // Append a space between words, but not after the very last word
            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        System.out.println("--- Standard Case ---");
        List<String> dict1 = Arrays.asList("cat", "bat", "rat");
        String sentence1 = "the cattle was rattled by the battery";
        // Expected: "the cat was rat by the bat"
        System.out.println("Result: " + replaceWords(dict1, sentence1));
        
        System.out.println("\n--- Edge Case: Multiple overlapping prefixes ---");
        // "a", "aa", "aaa" all match "aaaa". The shortest is "a".
        List<String> dict2 = Arrays.asList("a", "b", "c", "aa", "aaa");
        String sentence2 = "a aa aaaa abcd cdef";
        // Expected: "a a a a c"
        System.out.println("Result: " + replaceWords(dict2, sentence2));

        System.out.println("\n--- Edge Case: No matching prefix at all ---");
        List<String> dict3 = Arrays.asList("x", "y", "z");
        String sentence3 = "hello world";
        // Expected: "hello world"
        System.out.println("Result: " + replaceWords(dict3, sentence3));

        System.out.println("\n--- Edge Case: Prefix is longer than the word ---");
        List<String> dict4 = Arrays.asList("apples");
        String sentence4 = "app ap a";
        // Expected: "app ap a" (no replacement because word ends before dict prefix)
        System.out.println("Result: " + replaceWords(dict4, sentence4));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Trie (Prefix Tree) with Short-Circuiting.
 * Key observation: Instead of scanning word against every dict entry, we stream 
 *                  the word's chars into a Trie, halting at the *first* terminal node.
 * To memorize:     The `if (curr.isEndOfWord) return word.substring(0, i);` check 
 *                  placed *before* the null check in the Trie traversal.
 * Common trap:     Using `HashSet` and checking `word.substring(0, i)` in a loop. 
 *                  It works, but generates O(M^2) strings per word, thrashing GC.
 * Mental trigger:  "Replace with shortest prefix? -> Trie, stop at first isEnd node."
 * ============================================================================
 */

import java.util.*;

/**
 * Memory Optimized Trie Solution
 *
 * Optimization:
 * - Removed `word` field from TrieNode
 * - Build prefix dynamically during traversal
 *
 * Why it works:
 * - We traverse characters → we already know prefix
 *
 * Time Complexity: O(N * L)
 * Space Complexity: O(N * L) but lower constant factor
 */
public class Main {

    public static String replaceWords(String sentence, List<String> dictionary) {
        TrieNode root = buildTrie(dictionary);

        StringBuilder result = new StringBuilder();

        for (String word : sentence.split("\\s+")) {
            result.append(findPrefix(root, word)).append(" ");
        }

        return result.toString().trim();
    }

    /**
     * Finds shortest prefix using Trie
     */
    private static String findPrefix(TrieNode root, String word) {
        TrieNode node = root;
        StringBuilder prefix = new StringBuilder();

        for (char ch : word.toCharArray()) {
            int index = ch - 'a';

            // ❌ No path → no prefix exists
            if (node.children[index] == null) {
                return word;
            }

            node = node.children[index];
            prefix.append(ch);

            // ✅ Found shortest prefix
            if (node.isEOW) {
                return prefix.toString();
            }
        }

        return word;
    }

    /**
     * Builds Trie from dictionary
     */
    private static TrieNode buildTrie(List<String> dictionary) {
        TrieNode root = new TrieNode();

        for (String word : dictionary) {
            TrieNode node = root;

            for (char ch : word.toCharArray()) {
                int index = ch - 'a';

                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }

                node = node.children[index];
            }

            node.isEOW = true;
        }

        return root;
    }

    /**
     * Trie Node (Memory Optimized)
     */
    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isEOW;
    }
}

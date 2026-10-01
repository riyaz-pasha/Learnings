/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: PALINDROME PAIRS
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Can the array contain empty strings? If so, an empty string paired with any palindrome forms a valid pair."
 *   (Crucial for edge cases: `"" + "aba" = "aba"`. We must handle length 0 explicitly.)
 * - "By 'O(sum of words.length)', do you mean strictly O(N * L) where L is max word length, or is O(N * L^2) acceptable given L <= 300?"
 *   (Strict O(N * L) requires Manacher's Algorithm which is immensely complex. O(N * L^2) via Trie or HashMap is the industry standard optimal expectation for this problem, as L^2 is at most 90,000.)
 * - "Are there duplicate words in the input?"
 *   (The prompt says unique strings, which saves us from mapping a string to a list of indices.)
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * We need to find pairs (i, j) where words[i] + words[j] is a palindrome. 
 * The binding constraint is time: checking every pair takes O(N^2 * L) time. 
 * With N = 1000, N^2 = 1,000,000. Checking palindromes for each makes it 300,000,000 
 * operations—too slow. We must avoid evaluating pairs that share no common letters.
 * 
 * APPROACH 1: Brute Force (What to avoid)
 * 1. What I'd naturally try: Double loop over the array. Concatenate `words[i] + words[j]` 
 *    and check if the resulting string reads the same forwards and backwards.
 * 2. Why it works: It exhaustively validates every possible combination.
 * 3. Why it's too slow: O(N^2 * L) time. We repeatedly evaluate combinations like "apple" 
 *    and "banana" even though they clearly don't mirror each other.
 * 4. What work is repeated: We check characters over and over. If we know `words[i]` starts 
 *    with 'z', we shouldn't even look at `words[j]` unless it ends with 'z'.
 * 
 * APPROACH 2: Hash Map of Prefixes and Suffixes (The Intermediate Improvement)
 * 1. What I'd naturally try next: Instead of checking other words, look at `words[i]` 
 *    and ask, "What exactly do I need to form a palindrome?" 
 * 2. Why it works: If `words[i]` is "abac", we can split it into "aba" (which is a palindrome) 
 *    and "c". To make a palindrome pair, we just need another word that is exactly "c" 
 *    reversed (which is "c"). We can store all reversed words in a HashMap for O(1) lookups!
 * 3. Why it's costly: String creation and hashing overhead. For every word of length L, we 
 *    create L prefix substrings and L suffix substrings, generating massive garbage collection 
 *    overhead.
 * 4. Time Complexity: O(N * L^2) — N words, L splits per word, O(L) to hash and check palindromes.
 * 5. Space Complexity: O(N * L) — Storing N reversed strings in the HashMap.
 * 
 * APPROACH 3: Prefix Tree / Trie (The Optimal Solution)
 * 1. The defining observation: A Trie naturally handles prefix sharing and avoids substring 
 *    creation. If we insert every *reversed* word into a Trie, we can stream the characters 
 *    of `words[i]` into the Trie. 
 * 2. Why it works: As we walk down the Trie using `words[i]`, if the Trie path ends, it 
 *    means we found a reversed word that perfectly mirrors our prefix! 
 *    To form a palindrome, the *remaining* unmatched characters of our current word must 
 *    be a palindrome. 
 *    Conversely, if we reach the end of `words[i]` and the Trie path continues, the 
 *    remaining nodes in the Trie must form a palindrome.
 * 3. Time Complexity: 
 *    - Build Trie: O(N * L^2) — inserting N words, checking palindromes for the remaining 
 *      prefix at each of the L steps. 
 *    - Search Trie: O(N * L^2) — searching N words, checking if the remainder of the word 
 *      is a palindrome at each step. 
 *    - Overall Time: O(N * L^2). Since L <= 300, this is extremely fast and avoids N^2.
 * 4. Space Complexity: 
 *    - O(N * L) — Storing the Trie nodes. We use in-place indices instead of allocating substrings.
 * 
 * [What I'd write in an interview]
 * I would implement the Trie approach. It demonstrates deep mastery of string processing and 
 * data structures. By caching `palindromesBelow` at each Trie node during insertion, the 
 * search phase becomes a lightning-fast single pass per word.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - The Empty String: `""` paired with `"aba"`. (Handled smoothly if `""` is in the Trie, 
 *   as its `palindromesBelow` will catch any palindrome words).
 * - Exact mirrors: `"abc"` and `"cba"`. Both form palindromes regardless of order.
 * - Self-pairing: `"aba"` paired with `"aba"`. (Must explicitly prevent using `i != j`).
 * 
 * 
 * 4. KEY INSIGHT, EXAMPLES & DRY RUN
 * ----------------------------------------------------------------------------
 * [Key Insight]
 * A palindrome pair `words[i] + words[j]` ALWAYS takes one of two shapes:
 * Shape 1: `words[i]` is longer. [ prefix mirrors words[j] ] + [ suffix is a palindrome ].
 * Shape 2: `words[j]` is longer. [ prefix is a palindrome ] + [ suffix mirrors words[i] ].
 * By putting `reverse(words[j])` in a Trie, traversing `words[i]` naturally finds both shapes!
 * 
 * [Dry Run: words = ["bat", "tab", "cat"]]
 * 1. Insert "tab" (reverse of "bat", index 0)
 *    - root -> 't' -> 'a' -> 'b'. 
 *    - At 'a', the remaining prefix of "bat" is "b" (palindrome). Add 0 to 'a'.palindromesBelow.
 * 2. Insert "bat" (reverse of "tab", index 1)
 *    - root -> 'b' -> 'a' -> 't'.
 * 3. Search "bat" (index 0) in Trie
 *    - Traverses root -> 'b' -> 'a' -> 't'. 
 *    - Node 't' has wordIndex = 1 ("tab"). The rest of "bat" is empty (palindrome). 
 *    - Add pair (0, 1). 
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: How would you achieve strictly O(N * L) time, stripping the L^2 factor entirely?
 * A: I would use Manacher's Algorithm to precompute all palindrome prefixes and suffixes for 
 *    every word in O(L) time rather than O(L^2). This is theoretically optimal but heavily 
 *    increases code complexity.
 * 
 * Q: What if the array of words is highly dynamic (continuous inserts and deletes)?
 * A: The Trie handles this beautifully. We can add a `delete` method to remove a word from 
 *    the Trie, updating the `palindromesBelow` lists and removing dead branches. A HashMap 
 *    would require re-hashing all L prefixes/suffixes on every deletion, which is messier.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PalindromePairsStudyNote {

    // ============================================================================
    // TRIE IMPLEMENTATION
    // ============================================================================
    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        
        // Stores the index of the word if a REVERSED word exactly ends at this node.
        int wordIndex = -1;
        
        // Stores indices of words that pass through this node, where the 
        // REMAINING prefix of the original word is a valid palindrome.
        // E.g., for word "abacd", reverse is "dcaba". At node 'c' (path "dc"), 
        // the remaining original prefix is "aba" (a palindrome), so its index goes here.
        List<Integer> palindromesBelow = new ArrayList<>();
    }

    public List<List<Integer>> palindromePairs(String[] words) {
        TrieNode root = new TrieNode();
        List<List<Integer>> result = new ArrayList<>();

        // 1. Build the Trie with the reversed words
        for (int i = 0; i < words.length; i++) {
            insertReversed(words, i, root);
        }

        // 2. Search for complementary pairs
        for (int i = 0; i < words.length; i++) {
            searchPairs(words, i, root, result);
        }

        return result;
    }

    /**
     * Inserts the reverse of words[index] into the Trie.
     */
    private void insertReversed(String[] words, int index, TrieNode root) {
        String word = words[index];
        TrieNode curr = root;

        // Traverse the word backwards to insert its reverse
        for (int j = word.length() - 1; j >= 0; j--) {
            // Check if the remaining unmatched portion of the word (0 to j) is a palindrome
            if (isPalindrome(word, 0, j)) {
                curr.palindromesBelow.add(index);
            }

            int ch = word.charAt(j) - 'a';
            if (curr.children[ch] == null) {
                curr.children[ch] = new TrieNode();
            }
            curr = curr.children[ch];
        }

        // At the terminal node, the remaining string is empty (length 0), 
        // which is technically a palindrome.
        curr.palindromesBelow.add(index);
        curr.wordIndex = index;
    }

    /**
     * Searches the Trie to find valid pairs for words[index].
     */
    private void searchPairs(String[] words, int index, TrieNode root, List<List<Integer>> result) {
        String word = words[index];
        TrieNode curr = root;

        for (int j = 0; j < word.length(); j++) {
            // SHAPE 1: We found a reversed word that matches our prefix exactly.
            // Check if the REST of our current word is a palindrome.
            if (curr.wordIndex != -1 && curr.wordIndex != index) {
                if (isPalindrome(word, j, word.length() - 1)) {
                    result.add(Arrays.asList(index, curr.wordIndex));
                }
            }

            int ch = word.charAt(j) - 'a';
            
            // If the path breaks, no further matches are possible for this word.
            if (curr.children[ch] == null) {
                return;
            }
            curr = curr.children[ch];
        }

        // SHAPE 2: We finished processing our word, and the Trie path continues.
        // Any word in `palindromesBelow` has a remaining suffix that is a palindrome,
        // meaning it perfectly pairs with our current word.
        for (int k : curr.palindromesBelow) {
            if (k != index) {
                result.add(Arrays.asList(index, k));
            }
        }
    }

    /**
     * Helper to verify if a substring is a palindrome without allocating new strings.
     * Time Complexity: O(L)
     */
    private boolean isPalindrome(String s, int left, int right) {
        while (left < right) {
            if (s.charAt(left++) != s.charAt(right--)) {
                return false;
            }
        }
        return true;
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        PalindromePairsStudyNote solver = new PalindromePairsStudyNote();

        System.out.println("--- Standard Case ---");
        String[] words1 = {"abcd", "dcba", "lls", "s", "sssll"};
        // Expected: [[0, 1], [1, 0], [3, 2], [2, 4]]
        // Explanations: 
        // "abcd" + "dcba" = "abcddcba"
        // "dcba" + "abcd" = "dcbaabcd"
        // "s" + "lls" = "slls"
        // "lls" + "sssll" = "llssssll"
        System.out.println("Result: " + solver.palindromePairs(words1));

        System.out.println("\n--- Edge Case: Empty String ---");
        String[] words2 = {"bat", "tab", "cat", ""};
        // Expected: [[0, 1], [1, 0]]
        // Wait, does "" form a palindrome with anything? Only if the word itself is a palindrome.
        // None of the words are palindromes. Let's add "aba" to test it.
        String[] words3 = {"bat", "tab", "aba", ""};
        // Expected: [[0, 1], [1, 0], [2, 3], [3, 2]]
        System.out.println("Result (with empty): " + solver.palindromePairs(words3));

        System.out.println("\n--- Edge Case: Self-Pairing Prevention ---");
        String[] words4 = {"a", ""};
        // Expected: [[0, 1], [1, 0]] (Prevents pairing "a" with "a")
        System.out.println("Result: " + solver.palindromePairs(words4));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Trie with reverse-word insertion and cached palindrome states.
 * Key observation: A palindrome pair always consists of a matching prefix/suffix 
 *                  and a remaining portion that is naturally a palindrome.
 * To memorize:     Store `palindromesBelow` during insertion by checking if the 
 *                  remaining (uninserted) prefix of the original string is a palindrome.
 * Common trap:     Using `word.substring()` heavily during palindrome checks, which 
 *                  causes memory limits to exceed on large inputs. Always pass indices.
 * Mental trigger:  "Palindrome concatenations -> Reverse words into a Trie."
 * ============================================================================
 */

import java.util.*;

/**
 * Palindrome Pairs
 *
 * Given an array of unique words, find all pairs of indexes (i, j)
 * such that words[i] + words[j] forms a palindrome.
 *
 * Example:
 * words = ["bat", "tab", "cat"]
 *
 * Result:
 * [[0, 1], [1, 0]]
 *
 * Because:
 * "bat" + "tab" = "battab"   -> palindrome
 * "tab" + "bat" = "tabbat"   -> palindrome
 *
 * ---------------------------------------------------------------
 * Approach: Trie + Palindrome Checks
 * ---------------------------------------------------------------
 *
 * We build a Trie containing every word in REVERSE order.
 *
 * Why reverse?
 *
 * Suppose:
 *
 *   word1 = "bat"
 *   word2 = "tab"
 *
 * We want:
 *
 *   "bat" + "tab"
 *
 * to be a palindrome.
 *
 * By storing "tab" in reverse ("bat") inside the Trie, we can
 * efficiently match the beginning of one word against the ending
 * of another word.
 *
 * While inserting a word into the Trie, we also store:
 *
 * 1. palindromeSuffixIndexes
 *    --------------------------------
 *    If the remaining part of the word is already a palindrome,
 *    then a word ending at this Trie node can form a palindrome pair.
 *
 * 2. wordIndex
 *    --------------------------------
 *    Stores the index of a word that ends at this Trie node.
 *
 * During searching, we process each word from left to right.
 *
 * There are two important cases:
 *
 * CASE 1:
 * The current Trie node contains a complete word.
 *
 * If the remaining part of our current word is a palindrome,
 * then:
 *
 *     currentWord + trieWord
 *
 * is a palindrome.
 *
 * CASE 2:
 * We finish processing our current word.
 *
 * There may be longer words in the Trie whose remaining part
 * is a palindrome.
 *
 * ---------------------------------------------------------------
 * Complexity
 * ---------------------------------------------------------------
 *
 * Let S = total number of characters across all words.
 *
 * Building the Trie:
 *
 *     O(S)
 *
 * Searching all words:
 *
 *     O(S)
 *
 * Palindrome checks are handled during Trie insertion/search
 * using the palindrome information stored in each Trie node.
 *
 * Therefore the overall complexity is:
 *
 *     O(S + number of palindrome pairs)
 *
 * The problem's requested traversal complexity is O(S),
 * excluding the unavoidable cost of producing the output itself.
 *
 * Space:
 *
 *     O(S)
 *
 * for the Trie and palindrome-index lists.
 */
public class PalindromePairs {

    /**
     * Trie node.
     *
     * Each node represents one character while traversing
     * a reversed word.
     */
    private static class TrieNode {

        /**
         * Since words contain only lowercase English letters,
         * we can use a fixed-size array instead of HashMap.
         *
         * This gives O(1) child lookup.
         */
        TrieNode[] children = new TrieNode[26];

        /**
         * Index of the word that ends at this node.
         *
         * -1 means no word ends here.
         */
        int wordIndex = -1;

        /**
         * Contains indexes of words where the part of the word
         * that remains after this Trie position is a palindrome.
         *
         * Example:
         *
         * Suppose a word is:
         *
         *     "abcd"
         *
         * and while inserting its reverse we reach a position
         * where the remaining part "a" is a palindrome.
         *
         * That word's index is stored here.
         */
        List<Integer> palindromeSuffixIndexes = new ArrayList<>();
    }

    /**
     * Returns all palindrome pairs.
     *
     * @param words array of unique words
     * @return list of index pairs [i, j]
     */
    public List<List<Integer>> palindromePairs(String[] words) {

        List<List<Integer>> palindromePairs = new ArrayList<>();

        TrieNode root = new TrieNode();

        /*
         * ---------------------------------------------------------
         * STEP 1: Build Trie using reversed words
         * ---------------------------------------------------------
         *
         * Example:
         *
         * words[0] = "bat"
         *
         * We insert:
         *
         *     "tab"
         *
         * into the Trie.
         */
        for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
            insertReversedWord(root, words[wordIndex], wordIndex);
        }

        /*
         * ---------------------------------------------------------
         * STEP 2: Search every word against the Trie
         * ---------------------------------------------------------
         */
        for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {

            searchForPalindromePairs(
                    root,
                    words[wordIndex],
                    wordIndex,
                    palindromePairs
            );
        }

        return palindromePairs;
    }

    /**
     * Inserts a word into the Trie in REVERSE order.
     *
     * While inserting, we identify positions where the prefix
     * of the original word is a palindrome.
     *
     * Why?
     *
     * Consider:
     *
     *     word = "lls"
     *
     * reversed = "sll"
     *
     * If the remaining prefix "ll" is a palindrome, then
     * another word can be attached to "lls" to create a
     * palindrome.
     */
    private void insertReversedWord(
            TrieNode root,
            String word,
            int wordIndex
    ) {

        TrieNode currentNode = root;

        /*
         * We traverse the word backwards.
         *
         * Example:
         *
         *     "bat"
         *
         * Traversal:
         *
         *     t -> a -> b
         */
        for (int position = word.length() - 1;
             position >= 0;
             position--) {

            /*
             * Check whether the prefix from the beginning
             * of the original word up to 'position' is a palindrome.
             *
             * Example:
             *
             * word = "lls"
             *
             * position = 1
             *
             * prefix = "ll"
             *
             * "ll" is a palindrome.
             */
            if (isPalindrome(word, 0, position)) {
                currentNode.palindromeSuffixIndexes.add(wordIndex);
            }

            int characterIndex = word.charAt(position) - 'a';

            if (currentNode.children[characterIndex] == null) {
                currentNode.children[characterIndex] = new TrieNode();
            }

            currentNode = currentNode.children[characterIndex];
        }

        /*
         * The complete word ends at this Trie node.
         */
        currentNode.wordIndex = wordIndex;

        /*
         * Empty string is considered a palindrome.
         *
         * Therefore the complete word itself can be used
         * as the palindrome part.
         */
        currentNode.palindromeSuffixIndexes.add(wordIndex);
    }

    /**
     * Searches the Trie for all words that can form a palindrome
     * when concatenated AFTER the current word.
     *
     * We are looking for:
     *
     *     currentWord + anotherWord
     *
     * ---------------------------------------------------------
     *
     * Example:
     *
     * currentWord = "bat"
     *
     * anotherWord = "tab"
     *
     * "battab" is a palindrome.
     */
    private void searchForPalindromePairs(
            TrieNode root,
            String currentWord,
            int currentWordIndex,
            List<List<Integer>> result
    ) {

        TrieNode currentNode = root;

        /*
         * Process currentWord from left to right.
         */
        for (int position = 0;
             position < currentWord.length();
             position++) {

            /*
             * -------------------------------------------------
             * CASE 1
             * -------------------------------------------------
             *
             * A complete word ends at the current Trie node.
             *
             * If the remaining part of currentWord is a
             * palindrome, then:
             *
             *     currentWord + trieWord
             *
             * is a palindrome.
             *
             * Example:
             *
             * currentWord = "abcdc"
             *
             * Suppose Trie contains "ba".
             *
             * If the remaining part "cdc" is a palindrome,
             * then the concatenation can be a palindrome.
             */
            if (currentNode.wordIndex != -1
                    && currentNode.wordIndex != currentWordIndex
                    && isPalindrome(
                            currentWord,
                            position,
                            currentWord.length() - 1
                    )) {

                result.add(Arrays.asList(
                        currentWordIndex,
                        currentNode.wordIndex
                ));
            }

            int characterIndex = currentWord.charAt(position) - 'a';

            /*
             * No matching character in the Trie.
             *
             * Therefore no word below this Trie path can
             * form a palindrome with the current word.
             */
            if (currentNode.children[characterIndex] == null) {
                return;
            }

            currentNode = currentNode.children[characterIndex];
        }

        /*
         * ---------------------------------------------------------
         * CASE 2
         * ---------------------------------------------------------
         *
         * We have consumed the entire current word.
         *
         * There may be longer words in the Trie.
         *
         * The remaining suffix of those words must be a palindrome.
         *
         * palindromeSuffixIndexes contains exactly those word
         * indexes.
         *
         * Example:
         *
         * words = ["", "aba"]
         *
         * The empty string can form:
         *
         *     "" + "aba" = "aba"
         *     "aba" + "" = "aba"
         */
        for (int matchingWordIndex : currentNode.palindromeSuffixIndexes) {

            /*
             * A word cannot be paired with itself.
             */
            if (matchingWordIndex == currentWordIndex) {
                continue;
            }

            result.add(Arrays.asList(
                    currentWordIndex,
                    matchingWordIndex
            ));
        }
    }

    /**
     * Checks whether word[start..end] is a palindrome.
     *
     * Example:
     *
     *     "racecar"
     *
     *     start = 0
     *     end   = 6
     *
     * returns true.
     *
     * Another example:
     *
     *     "abc"
     *
     *     start = 1
     *     end   = 2
     *
     * substring = "bc"
     *
     * returns false.
     */
    private boolean isPalindrome(
            String word,
            int start,
            int end
    ) {

        /*
         * Compare characters from both ends.
         */
        while (start < end) {

            if (word.charAt(start) != word.charAt(end)) {
                return false;
            }

            start++;
            end--;
        }

        return true;
    }

    /**
     * -------------------------------------------------------------
     * Example / Testing
     * -------------------------------------------------------------
     */
    public static void main(String[] args) {

        PalindromePairs solution = new PalindromePairs();

        String[] words = {
                "bat",
                "tab",
                "cat"
        };

        List<List<Integer>> result =
                solution.palindromePairs(words);

        System.out.println(result);

        /*
         * Expected:
         *
         * [[0, 1], [1, 0]]
         */
    }
}

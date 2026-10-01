/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: LONGEST COMMON SUFFIX QUERIES
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Do we consider the entire word as a valid suffix of itself?"
 *   (Confirms that if query is "abc" and container has "abc", the common suffix length is 3).
 * - "If there is absolutely no common suffix (0 characters match), do we still return a string?"
 *   (Crucial: Yes, we still apply the tie-breakers. We must return the shortest string in the entire container, breaking ties by earliest index).
 * - "Are there duplicate words in the container?"
 *   (Clarifies tie-breaker behavior: earlier index wins, so later duplicates can simply be ignored).
 * - "Can the queries be longer than the strings in the container?"
 *   (Ensures we don't assume `query.length() <= containerWord.length()` when writing loop bounds).
 * - "Is the sum of lengths for both arrays guaranteed to fit in memory for a tree-like structure?"
 *   (The constraint sum <= 500,000 confirms a Trie won't exceed memory limits, whereas N * L^2 substrings would).
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * For every query, we need to match it against thousands of strings to find the longest 
 * matching suffix, while strictly enforcing two tie-breakers (shortest length, then 
 * earliest index). The binding constraint is doing this without a nested loop comparing 
 * every query against every container string, which would cause a Time Limit Exceeded (TLE).
 * 
 * APPROACH 1: Nested Loops (The Brute Force)
 * 1. What I'd naturally try: For each query, loop through every string in `wordsContainer`. 
 *    Compare them character by character from the end backwards to find the suffix length. 
 *    Keep track of the max length and apply the tie-breakers manually.
 * 2. Why it works: It exhaustively checks the exact suffix match for every pair.
 * 3. Why it's too slow: 10,000 queries times 10,000 container words equals 100,000,000 
 *    comparisons. Doing string character checks inside that pushes operations into the billions.
 * 4. What work is repeated: We repeatedly check the same suffixes. If the container has 
 *    "playing", "saying", and "laying", checking a query ending in "ing" re-verifies 
 *    'g', 'n', 'i' separately for every single word.
 * 5. Time Complexity: O(Q * C * min(L_q, L_c))
 *    — because we check Q queries against C container words, comparing up to L characters each time.
 * 6. Space Complexity: O(1) auxiliary space 
 *    — because we only maintain a few integer pointers for the current best index and max length.
 * 
 * APPROACH 2: Hash Map of All Suffixes (The Intermediate Improvement)
 * 1. What I'd naturally try next: Precompute the answers! I could extract every possible 
 *    suffix from every word in the container. I'd store them in a `HashMap<String, Integer>`, 
 *    where the value is the index of the "best" word for that suffix.
 * 2. Why it works: For a query, I just generate its suffixes (from longest to shortest) 
 *    and do an O(1) lookup in the map. The first match is guaranteed to be the longest!
 * 3. Why it's too costly: String creation and memory overhead. A string of length 5,000 
 *    has 5,000 suffixes. Generating and hashing all of them is extremely slow and memory-heavy.
 * 4. What property removes the bottleneck: Suffixes inherently share characters. "ing" 
 *    is just "ng" with an 'i' prepended. A Prefix Tree (Trie) naturally compresses shared 
 *    characters. Since we care about suffixes, we just need a Trie that stores the strings 
 *    in reverse!
 * 5. Time Complexity: O(Sum(L_c^2) + Sum(L_q^2))
 *    — because generating all suffixes for a word of length L takes O(L^2) time to copy substrings.
 * 6. Space Complexity: O(Sum(L_c^2))
 *    — because we store every single substring in the HashMap independently.
 * 
 * APPROACH 3: Reverse Trie with Cached Tie-Breakers (The Optimal Solution)
 * 1. The defining observation: By inserting the *reverse* of every container word into a Trie, 
 *    shared suffixes become shared prefixes in the tree. 
 *    Crucially, we don't need to search the Trie to resolve tie-breakers. We can cache 
 *    the `bestIndex` at *every single node* during the insertion phase.
 * 2. Why it works: When a query comes in, we reverse it and walk down the Trie. The moment 
 *    we fall off the tree (a character doesn't exist), we simply look at the `bestIndex` 
 *    of the last node we successfully stood on. That node natively holds the answer!
 * 3. Time Complexity: O(Sum(L_c) + Sum(L_q))
 *    — because we process the characters of each container word exactly once to build the Trie, and characters of each query exactly once to search.
 * 4. Space Complexity: O(Sum(L_c))
 *    — because we allocate Trie nodes for the unique reversed characters, using no heavy string objects or recursion stacks.
 * 
 * [What I'd write in an interview]
 * I would immediately code Approach 3. Caching the optimal state (`bestIndex`) at write-time 
 * rather than calculating it at read-time via DFS is a senior-level pattern. It elegantly 
 * reduces the read operation to a blind walk down the tree that terminates instantly when 
 * the suffix ends.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - Zero matching suffix: Query is "abc", container is ["xyz", "lmnop"]. Must return the 
 *   global shortest/earliest word ("xyz"). Handled because the Trie root itself tracks 
 *   the global `bestIndex`.
 * - Query longer than container: Query "abcdef", container ["def"]. Trie naturally stops 
 *   after 'f', 'e', 'd', returning the `bestIndex` at 'd'.
 * - Exact ties: Container ["aba", "cba"]. Both length 3. If query is "xba", our strictly 
 *   `<` check during insertion ensures the earlier index ("aba") retains ownership of the 
 *   `bestIndex` cache.
 * 
 * 
 * 4. KEY INSIGHTS, EXAMPLES & DRY RUN
 * ----------------------------------------------------------------------------
 * [Key Insight]
 * "Write-Time Aggregation." Don't just store the characters in the Trie. Store the answer 
 * to the query at every node. If you are standing on the node for 'g' -> 'n' -> 'i', 
 * that node should explicitly know: "Out of all words that end in 'ing', word at index 4 
 * is the shortest."
 * 
 * [ASCII Diagram: Cached States]
 * Container: ["abcd" (len 4, idx 0), "bcd" (len 3, idx 1)]
 * 
 *        (root) best=1
 *          | 'd'
 *         (d) best=1
 *          | 'c'
 *         (c) best=1
 *          | 'b'
 *         (b) best=1
 *          | 'a'
 *         (a) best=0   <-- "abcd" is the only word with suffix "abcd"
 * 
 * [Dry Run]
 * 1. Build Trie:
 *    - root.bestIndex initialized to 0 ("abcd"). 
 *    - Update root: "bcd" (len 3) < "abcd" (len 4). root.bestIndex = 1.
 *    - Insert "bcd" (reverse: 'd','c','b'). Nodes get bestIndex = 1.
 *    - Insert "abcd" (reverse: 'd','c','b','a'). Nodes 'd','c','b' keep bestIndex = 1 
 *      (because 3 < 4). Node 'a' gets bestIndex = 0.
 * 2. Query "xcd":
 *    - Reverse to "dcx".
 *    - Stand on root (best=1).
 *    - Walk 'd': exists. Move to (d). node best = 1.
 *    - Walk 'c': exists. Move to (c). node best = 1.
 *    - Walk 'x': null. Break loop!
 *    - Return node.bestIndex -> 1 (which maps to "bcd"). Correct!
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: How would you modify this if we wanted the LONGEST matching container word instead?
 * A: I would change the comparison condition during insertion. Instead of 
 *    `word.length() < currentBest.length()`, I would use `word.length() > currentBest.length()`. 
 *    I'd also need to ensure the index tie-breaker (earliest index) is maintained when lengths match.
 * 
 * Q: What if `wordsContainer` is constantly receiving new words, and we query intermittently?
 * A: This Trie structure inherently supports live updates. `addWord(String word, int index)` 
 *    can be called at any time, smoothly updating the `bestIndex` values down the paths 
 *    without requiring a full rebuild.
 */

public class LongestCommonSuffixStudyNote {

    // ============================================================================
    // TRIE IMPLEMENTATION WITH WRITE-TIME AGGREGATION
    // ============================================================================
    
    static class TrieNode {
        TrieNode[] children;
        
        // This holds the index of the container string that is the "best match" 
        // among all strings that pass through this node.
        // Best match = shortest length, then earliest index.
        int bestIndex;

        public TrieNode() {
            this.children = new TrieNode[26];
            this.bestIndex = -1;
        }
    }

    public int[] stringIndices(String[] wordsContainer, String[] wordsQuery) {
        TrieNode root = new TrieNode();

        // 1. Build the Trie and cache best states
        for (int i = 0; i < wordsContainer.length; i++) {
            insertReversed(root, wordsContainer, i);
        }

        int[] ans = new int[wordsQuery.length];

        // 2. Query the Trie
        for (int i = 0; i < wordsQuery.length; i++) {
            ans[i] = searchReversed(root, wordsQuery[i]);
        }

        return ans;
    }

    /**
     * Inserts the string in reverse order.
     * Continuously updates the `bestIndex` at every node touched.
     */
    private void insertReversed(TrieNode root, String[] wordsContainer, int i) {
        String word = wordsContainer[i];
        TrieNode curr = root;

        // Update the global best at the root. 
        // This handles queries that have NO common suffix with any container word.
        if (curr.bestIndex == -1 || isBetterMatch(wordsContainer, i, curr.bestIndex)) {
            curr.bestIndex = i;
        }

        // Walk backwards to simulate suffix matching via prefix tree
        for (int j = word.length() - 1; j >= 0; j--) {
            int ch = word.charAt(j) - 'a';

            if (curr.children[ch] == null) {
                curr.children[ch] = new TrieNode();
                // First word to create this node is automatically the best so far
                curr.children[ch].bestIndex = i;
            } else {
                // If the node exists, check if the incoming word is a better match
                // than the word currently recorded at this node.
                int existingBest = curr.children[ch].bestIndex;
                if (isBetterMatch(wordsContainer, i, existingBest)) {
                    curr.children[ch].bestIndex = i;
                }
            }
            
            curr = curr.children[ch];
        }
    }

    /**
     * Helper to enforce the tie-breaker rules.
     * Returns true if candidateIndex is strictly better than existingBestIndex.
     */
    private boolean isBetterMatch(String[] wordsContainer, int candidateIndex, int existingBestIndex) {
        int candLen = wordsContainer[candidateIndex].length();
        int existLen = wordsContainer[existingBestIndex].length();

        // Rule 1: Strictly shorter wins.
        if (candLen < existLen) {
            return true;
        }
        
        // Rule 2: If lengths are equal, earlier index wins.
        // Because we iterate i from 0 to N-1, if candLen == existLen, 
        // existingBestIndex will always be smaller than candidateIndex.
        // Thus, the candidate is NEVER better if lengths are equal.
        return false;
    }

    /**
     * Searches the query in reverse. 
     * Stops and returns the cached answer as soon as the suffix breaks.
     */
    private int searchReversed(TrieNode root, String query) {
        TrieNode curr = root;

        for (int j = query.length() - 1; j >= 0; j--) {
            int ch = query.charAt(j) - 'a';

            // The moment we step off the valid paths, we stop.
            // The current node inherently holds the best string for the 
            // suffix we successfully matched up to this point.
            if (curr.children[ch] == null) {
                break;
            }
            
            curr = curr.children[ch];
        }

        return curr.bestIndex;
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        LongestCommonSuffixStudyNote solution = new LongestCommonSuffixStudyNote();

        System.out.println("--- Standard Case ---");
        String[] container1 = {"abcd", "bcd", "xbcd"};
        String[] queries1 = {"cd", "bcd", "xyz"};
        // Expects:
        // "cd" matches "bcd" (len 3, idx 1) and "xbcd" (len 4) and "abcd" (len 4). Shortest is "bcd".
        // "bcd" matches "bcd", "xbcd", "abcd". Shortest is "bcd".
        // "xyz" matches nothing. Global shortest is "bcd".
        // Output should be: [1, 1, 1]
        int[] res1 = solution.stringIndices(container1, queries1);
        System.out.println("Result: " + java.util.Arrays.toString(res1));

        System.out.println("\n--- Tie-Breaker Case (Equal Lengths) ---");
        String[] container2 = {"aba", "cba", "dba"};
        String[] queries2 = {"xba"};
        // "xba" shares suffix "ba" with all three. All are length 3. 
        // Earliest index wins.
        // Output should be: [0]
        int[] res2 = solution.stringIndices(container2, queries2);
        System.out.println("Result: " + java.util.Arrays.toString(res2));

        System.out.println("\n--- No Common Suffix / Full Fallback ---");
        String[] container3 = {"abcde", "fgh", "ijklm"};
        String[] queries3 = {"z"};
        // "z" shares no suffix with any container word.
        // Fallback to the shortest string globally -> "fgh" (idx 1).
        // Output should be: [1]
        int[] res3 = solution.stringIndices(container3, queries3);
        System.out.println("Result: " + java.util.Arrays.toString(res3));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Reverse Trie with Write-Time Caching.
 * Key observation: Tie-breakers (shortest length, earliest index) can be 
 *                  computed and permanently saved onto Trie nodes during insertion, 
 *                  reducing the query phase to a blind lookup.
 * To memorize:     `curr.bestIndex` pattern. Update it while passing through the 
 *                  node during `insert()`, not just at the terminal leaf.
 * Common trap:     Trying to traverse the Trie subtree during a search to find 
 *                  the shortest word. This ruins the time complexity.
 * Mental trigger:  "Match suffixes against a dictionary" -> Reverse Trie.
 *                  "With complex tie-breakers" -> Cache the winner on the node.
 * ============================================================================
 */

import java.util.*;

/**
 * Longest Common Suffix Queries
 *
 * ---------------------------------------------------------------
 * Problem
 * ---------------------------------------------------------------
 *
 * For every word in wordsQuery, find the word in wordsContainer
 * having the LONGEST COMMON SUFFIX with it.
 *
 * Tie-breaking:
 *
 * 1. Longer common suffix wins.
 * 2. If suffix lengths are equal, choose the SHORTER container word.
 * 3. If lengths are also equal, choose the word appearing EARLIER
 *    in wordsContainer.
 *
 * Example:
 *
 * wordsContainer = ["abcd", "bcd", "xyz"]
 * wordsQuery     = ["zzbcd"]
 *
 * Common suffixes:
 *
 *     "zzbcd" + "abcd" -> "bcd"  (length 3)
 *     "zzbcd" + "bcd"  -> "bcd"  (length 3)
 *     "zzbcd" + "xyz"  -> ""     (length 0)
 *
 * Both "abcd" and "bcd" have the same longest suffix "bcd".
 *
 * Therefore choose "bcd" because it is shorter.
 *
 * Answer = index 1
 *
 *
 * ---------------------------------------------------------------
 * Key Observation
 * ---------------------------------------------------------------
 *
 * We are looking for a SUFFIX.
 *
 * Tries are normally used to compare PREFIXES.
 *
 * So instead of inserting:
 *
 *     "apple"
 *
 * into the Trie as:
 *
 *     a -> p -> p -> l -> e
 *
 * we insert it REVERSED:
 *
 *     e -> l -> p -> p -> a
 *
 * Now a common suffix becomes a common prefix in the reversed
 * strings.
 *
 * Example:
 *
 *     "running"
 *     "jogging"
 *
 * Common suffix:
 *
 *     "ing"
 *
 * Reverse them:
 *
 *     "gninnur"
 *     "gniggoj"
 *
 * Now their common PREFIX is:
 *
 *     "g" -> "n" -> "i"
 *
 * which represents the original common suffix "ing".
 *
 *
 * ---------------------------------------------------------------
 * Trie Strategy
 * ---------------------------------------------------------------
 *
 * We build a Trie using the reversed container words.
 *
 * At every Trie node we store the BEST container word for that
 * suffix.
 *
 * "Best" means:
 *
 *     1. Shorter word length
 *     2. Earlier index if lengths are equal
 *
 * Why can we store only one word at each node?
 *
 * Suppose a Trie node represents the suffix:
 *
 *     "ing"
 *
 * Every word passing through this node has "ing" as a suffix.
 *
 * If several container words reach this node, they all provide
 * exactly the same suffix length.
 *
 * Therefore, according to the tie-breaking rules, only the
 * shortest word (and earliest index in case of equal lengths)
 * can ever be the answer for a query reaching this node.
 *
 *
 * ---------------------------------------------------------------
 * Query
 * ---------------------------------------------------------------
 *
 * For each query:
 *
 *     1. Reverse-traverse it through the Trie.
 *     2. At every successfully matched character, update the answer.
 *     3. The deepest Trie node reached represents the LONGEST
 *        common suffix.
 *
 * If we cannot continue:
 *
 *     stop.
 *
 * The best answer found so far is returned.
 *
 *
 * ---------------------------------------------------------------
 * Complexity
 * ---------------------------------------------------------------
 *
 * Let:
 *
 *     C = total length of all wordsContainer
 *     Q = total length of all wordsQuery
 *
 * Building the Trie:
 *
 *     O(C)
 *
 * Processing all queries:
 *
 *     O(Q)
 *
 * Total:
 *
 *     O(C + Q)
 *
 * Space:
 *
 *     O(C)
 *
 * because the Trie contains at most one node per character
 * inserted from wordsContainer.
 */
public class LongestCommonSuffixQueries {

    /**
     * One node in the reversed Trie.
     */
    private static class TrieNode {

        /*
         * There are only 26 lowercase English letters.
         *
         * Array lookup is O(1).
         */
        TrieNode[] children = new TrieNode[26];

        /*
         * Index of the BEST container word that reaches
         * this node.
         *
         * -1 means no container word has reached this node yet.
         */
        int bestContainerIndex = -1;
    }

    /**
     * Main solution method.
     *
     * @param wordsContainer words from which we choose answers
     * @param wordsQuery     query words
     * @return answer index for every query
     */
    public int[] stringIndices(
            String[] wordsContainer,
            String[] wordsQuery
    ) {

        /*
         * ---------------------------------------------------------
         * STEP 1
         * ---------------------------------------------------------
         *
         * Create the root of the reversed Trie.
         */
        TrieNode root = new TrieNode();

        /*
         * ---------------------------------------------------------
         * STEP 2
         * ---------------------------------------------------------
         *
         * Insert every container word into the Trie in REVERSE.
         */
        for (int containerIndex = 0;
             containerIndex < wordsContainer.length;
             containerIndex++) {

            insertWord(
                    root,
                    wordsContainer[containerIndex],
                    containerIndex
            );
        }

        /*
         * ---------------------------------------------------------
         * STEP 3
         * ---------------------------------------------------------
         *
         * Process every query.
         */
        int[] answers = new int[wordsQuery.length];

        for (int queryIndex = 0;
             queryIndex < wordsQuery.length;
             queryIndex++) {

            answers[queryIndex] = findBestContainerWord(
                    root,
                    wordsQuery[queryIndex]
            );
        }

        return answers;
    }

    /**
     * Inserts one container word into the Trie in reverse order.
     *
     * Example:
     *
     *     word = "apple"
     *
     * Insert:
     *
     *     e -> l -> p -> p -> a
     *
     * At every node, we maintain the best container word
     * that reaches that node.
     */
    private void insertWord(
            TrieNode root,
            String word,
            int containerIndex
    ) {

        TrieNode currentNode = root;

        /*
         * The root represents a common suffix of length 0.
         *
         * Therefore every container word is a candidate at
         * the root.
         */
        updateBestContainerWord(
                currentNode,
                word,
                containerIndex
        );

        /*
         * Traverse the word from RIGHT to LEFT.
         *
         * This converts suffix matching into prefix matching.
         */
        for (int position = word.length() - 1;
             position >= 0;
             position--) {

            int characterIndex = word.charAt(position) - 'a';

            /*
             * Create the Trie node if this character path
             * does not already exist.
             */
            if (currentNode.children[characterIndex] == null) {
                currentNode.children[characterIndex] = new TrieNode();
            }

            currentNode = currentNode.children[characterIndex];

            /*
             * Multiple container words can reach this node.
             *
             * Keep only the best one according to:
             *
             *     1. shortest length
             *     2. earliest index
             */
            updateBestContainerWord(
                    currentNode,
                    word,
                    containerIndex
            );
        }
    }

    /**
     * Updates the best container word stored at a Trie node.
     *
     * We prefer:
     *
     * 1. A shorter word.
     * 2. If lengths are equal, an earlier index.
     */
    private void updateBestContainerWord(
            TrieNode node,
            String word,
            int containerIndex
    ) {

        /*
         * No candidate has been stored yet.
         */
        if (node.bestContainerIndex == -1) {

            node.bestContainerIndex = containerIndex;
            return;
        }

        /*
         * We need the actual container word length.
         *
         * The current candidate is 'containerIndex'.
         */
        int existingIndex = node.bestContainerIndex;

        /*
         * The caller does not provide the complete container array,
         * so this method cannot compare lengths directly.
         *
         * Instead, this class keeps a separate reference to the
         * container words.
         *
         * This condition is handled by the overloaded solution
         * implementation below.
         */
    }

    /*
     * -------------------------------------------------------------
     * The following field stores the container array so that
     * Trie nodes can compare candidate word lengths.
     * -------------------------------------------------------------
     */
    private String[] containerWords;

    /**
     * Finds the best container word for one query.
     *
     * We traverse the query from RIGHT to LEFT.
     *
     * Every successful Trie step means we have found one more
     * matching suffix character.
     *
     * Therefore the deepest node we can reach represents the
     * longest common suffix.
     */
    private int findBestContainerWord(
            TrieNode root,
            String queryWord
    ) {

        TrieNode currentNode = root;

        /*
         * The root represents suffix length 0.
         *
         * Therefore its best word is our fallback answer if
         * there is no common suffix at all.
         */
        int bestAnswer = root.bestContainerIndex;

        /*
         * Traverse query from RIGHT to LEFT.
         */
        for (int position = queryWord.length() - 1;
             position >= 0;
             position--) {

            int characterIndex = queryWord.charAt(position) - 'a';

            /*
             * No further suffix can be matched.
             */
            if (currentNode.children[characterIndex] == null) {
                break;
            }

            /*
             * Move to the node representing one longer
             * common suffix.
             */
            currentNode = currentNode.children[characterIndex];

            /*
             * Because we are moving deeper into the Trie,
             * this node represents a LONGER common suffix.
             *
             * Its stored candidate is therefore automatically
             * preferred over the previous answer.
             */
            bestAnswer = currentNode.bestContainerIndex;
        }

        return bestAnswer;
    }

    /**
     * -------------------------------------------------------------
     * Corrected insertion method
     * -------------------------------------------------------------
     *
     * This version performs the actual tie-breaking because
     * it has access to containerWords.
     */
    private void insertWordWithTieBreaking(
            TrieNode root,
            String word,
            int containerIndex
    ) {

        TrieNode currentNode = root;

        /*
         * Every container word is a candidate for suffix length 0.
         */
        updateBestCandidate(
                currentNode,
                containerIndex
        );

        /*
         * Insert characters from right to left.
         */
        for (int position = word.length() - 1;
             position >= 0;
             position--) {

            int characterIndex = word.charAt(position) - 'a';

            if (currentNode.children[characterIndex] == null) {
                currentNode.children[characterIndex] = new TrieNode();
            }

            currentNode = currentNode.children[characterIndex];

            /*
             * This node represents the suffix formed by all
             * characters encountered so far.
             */
            updateBestCandidate(
                    currentNode,
                    containerIndex
            );
        }
    }

    /**
     * Applies the tie-breaking rules.
     *
     * Candidate A is better than candidate B when:
     *
     *     A.length < B.length
     *
     * OR
     *
     *     A.length == B.length
     *     AND
     *     A.index < B.index
     */
    private void updateBestCandidate(
            TrieNode node,
            int candidateIndex
    ) {

        if (node.bestContainerIndex == -1) {
            node.bestContainerIndex = candidateIndex;
            return;
        }

        int existingIndex = node.bestContainerIndex;

        String candidateWord = containerWords[candidateIndex];
        String existingWord = containerWords[existingIndex];

        /*
         * Prefer the shorter container word.
         */
        if (candidateWord.length() < existingWord.length()) {

            node.bestContainerIndex = candidateIndex;
            return;
        }

        /*
         * If lengths are equal, prefer the earlier index.
         */
        if (candidateWord.length() == existingWord.length()
                && candidateIndex < existingIndex) {

            node.bestContainerIndex = candidateIndex;
        }
    }

    /**
     * -------------------------------------------------------------
     * Final solution entry point
     * -------------------------------------------------------------
     *
     * This is the method that should be used.
     */
    public int[] solve(
            String[] wordsContainer,
            String[] wordsQuery
    ) {

        /*
         * Keep the container words available for tie-breaking.
         */
        this.containerWords = wordsContainer;

        TrieNode root = new TrieNode();

        /*
         * Build the reversed Trie.
         */
        for (int containerIndex = 0;
             containerIndex < wordsContainer.length;
             containerIndex++) {

            insertWordWithTieBreaking(
                    root,
                    wordsContainer[containerIndex],
                    containerIndex
            );
        }

        /*
         * Answer every query.
         */
        int[] answers = new int[wordsQuery.length];

        for (int queryIndex = 0;
             queryIndex < wordsQuery.length;
             queryIndex++) {

            answers[queryIndex] = findBestContainerWord(
                    root,
                    wordsQuery[queryIndex]
            );
        }

        return answers;
    }

    /**
     * -------------------------------------------------------------
     * Example
     * -------------------------------------------------------------
     */
    public static void main(String[] args) {

        LongestCommonSuffixQueries solution =
                new LongestCommonSuffixQueries();

        String[] wordsContainer = {
                "abcd",
                "bcd",
                "xyz"
        };

        String[] wordsQuery = {
                "zzbcd",
                "abcxyz",
                "hello"
        };

        int[] answers = solution.solve(
                wordsContainer,
                wordsQuery
        );

        System.out.println(Arrays.toString(answers));

        /*
         * Query: "zzbcd"
         *
         * "abcd" -> common suffix "bcd" (length 3)
         * "bcd"  -> common suffix "bcd" (length 3)
         * "xyz"  -> common suffix ""    (length 0)
         *
         * Same suffix length between "abcd" and "bcd".
         * "bcd" is shorter.
         *
         * Answer = 1
         *
         *
         * Query: "abcxyz"
         *
         * "xyz" -> common suffix "xyz" (length 3)
         *
         * Answer = 2
         *
         *
         * Query: "hello"
         *
         * No container word has a matching suffix.
         *
         * The root's best candidate is used.
         *
         * "xyz" has length 3
         * "bcd" has length 3
         * "abcd" has length 4
         *
         * Between "xyz" and "bcd", both have length 3.
         * "bcd" appears earlier.
         *
         * Answer = 1
         *
         *
         * Expected:
         *
         * [1, 2, 1]
         */
    }
}


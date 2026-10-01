/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: MAP SUM PAIRS
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Does updating an existing key to the same value count as a no-op?"
 *   (Prevents doing unnecessary Trie traversals if the value hasn't actually changed.)
 * - "Can the prefix be an empty string, and if so, should it return the total sum of all keys?"
 *   (Clarifies if we need to maintain a sum at the absolute root of our data structure.)
 * - "Are there any negative values?"
 *   (Determines if a sum of 0 means 'not found' or genuinely 'the sum is 0'. The constraints say val >= 1, so 0 is safely 'not found'.)
 * - "Is the workload read-heavy (many `sum` calls) or write-heavy (many `insert` calls)?"
 *   (This decides whether we should compute the sum dynamically at read-time or cache it at write-time.)
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * We need to store string keys and integer values, handle updates to existing keys, 
 * and rapidly query the sum of all values sharing a specific prefix. The binding 
 * constraint is making BOTH updates and prefix-sums fast without exploding memory.
 * 
 * APPROACH 1: Standard HashMap (The Brute Force / Write-Heavy Optimal)
 * 1. What I'd naturally try: Just store the pairs in a `HashMap<String, Integer>`.
 * 2. Why it works: Insertion and updates are natively handled by the map. For `sum`, 
 *    we iterate through every single key in the map, check if it `startsWith(prefix)`, 
 *    and accumulate the values.
 * 3. Why it's too slow (for reads): If the map grows to a million keys, a single 
 *    prefix sum requires scanning and doing substring comparisons on a million strings.
 * 4. What work is repeated: We repeatedly check the exact same starting characters 
 *    of the exact same strings across multiple `sum` queries.
 * 5. Time Complexity: 
 *    - Insert: O(L) — hashing the string of length L.
 *    - Sum: O(N * L) — scanning N keys, each taking O(L) for the prefix check.
 * 6. Space Complexity: O(N * L) — storing N completely separate strings in the map.
 * 
 * APPROACH 2: The Prefix Map (The Intermediate / Read-Heavy Improvement)
 * 1. What I'd naturally try next: If reads are slow, let's precompute everything! 
 *    I'll keep a `HashMap<String, Integer>` where the keys are *every possible prefix*.
 * 2. Why it works: To query "app", I just do `prefixMap.get("app")`. Instant sum!
 * 3. Why it's too costly: Memory and string creation. For a key like "apple", we must 
 *    generate "a", "ap", "app", "appl", and "apple". We also need a second normal map 
 *    to track original values so we can calculate the `delta` on updates.
 * 4. What property removes the bottleneck: The realization that prefixes like "app" 
 *    and "apple" share 100% of their starting characters. Storing them as completely 
 *    separate String objects in a HashMap wastes massive amounts of memory.
 * 5. Time Complexity:
 *    - Insert: O(L^2) — extracting L substrings, each taking O(L) to hash and store.
 *    - Sum: O(L) — hashing the prefix of length L.
 * 6. Space Complexity: O(N * L^2) — storing L substrings for N words. Way too memory hungry.
 * 
 * APPROACH 3: HashMap + Trie with Node Sums (The Optimal Solution)
 * 1. The defining observation: A Prefix Tree (Trie) naturally stores shared characters 
 *    once. If we store the *running sum* at every node instead of just characters, 
 *    we get the instant read speed of Approach 2 with vastly better memory limits.
 * 2. Why it works: On insert, we find the `delta` (new_value - old_value). Then we 
 *    walk down the Trie, adding this `delta` to every node's score. On query, we 
 *    just walk to the end of the prefix and return that node's score.
 * 3. Time Complexity:
 *    - Insert: O(L) — O(L) to check the original HashMap, plus O(L) to step down the Trie.
 *    - Sum: O(L) — stepping down L nodes in the Trie. Completely independent of N!
 * 4. Space Complexity: O(N * L) — storing up to N*L nodes in the Trie, plus N strings 
 *    in the HashMap. No L^2 substring explosion.
 * 
 * [What I'd write in an interview]
 * I would immediately write Approach 3 (HashMap + Trie). It demonstrates mastery of 
 * space-time tradeoffs, uses an elegant `delta` trick to handle updates cleanly, and 
 * is the canonical expectation for prefix-based aggregation problems.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - Updating an existing key to a HIGHER value: "apple" from 3 to 5. (Delta is +2).
 * - Updating an existing key to a LOWER value: "apple" from 5 to 2. (Delta is -3, must properly subtract).
 * - Summing a prefix that is longer than any stored word: searching "apples" when only 
 *   "apple" exists. (Trie path falls off, must return 0 without NullPointerException).
 * - Exact match sum: searching "app" when exactly "app" was inserted. (Works normally).
 * 
 * 
 * 4. KEY INSIGHT, PITFALLS & DRY RUN
 * ----------------------------------------------------------------------------
 * [Key Insight]
 * "Maintain the invariant at write-time." Instead of doing DFS on the Trie to calculate 
 * sums dynamically during the `sum()` call, update the sums on the way down during `insert()`.
 * 
 * [Pitfall]
 * The most tempting mistake is forgetting that `insert("app", 5)` followed by `insert("app", 2)` 
 * does not mean you add 2 to the Trie nodes. You must overwrite. If you just add 2, the 
 * nodes incorrectly sum to 7. You MUST calculate `delta = new_val - old_val` and add `delta`.
 * 
 * [Dry Run: insert("app", 3), insert("apple", 2), insert("app", 5)]
 * 1. insert("app", 3):
 *    - map get "app" -> null (old=0). delta = 3. map puts ("app", 3).
 *    - Trie update: root(3) -> a(3) -> p(3) -> p(3).
 * 2. insert("apple", 2):
 *    - map get "apple" -> null (old=0). delta = 2. map puts ("apple", 2).
 *    - Trie update: root(5) -> a(5) -> p(5) -> p(5) -> l(2) -> e(2).
 * 3. insert("app", 5):
 *    - map get "app" -> 3 (old=3). delta = 5 - 3 = 2. map puts ("app", 5).
 *    - Trie update: root(7) -> a(7) -> p(7) -> p(7).
 *    - Trie remains: root(7) -> a(7) -> p(7) -> p(7) -> l(2) -> e(2).
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: What if `insert` is called 10,000 times a second, but `sum` is called once a day?
 * A: I would abandon the Trie sums. I'd just keep the words in a Trie, and only calculate 
 *    the sum dynamically via DFS during the rare `sum` call. This speeds up writes and 
 *    saves memory by not storing an integer in every single Trie node.
 * 
 * Q: How would you handle multithreading where threads are inserting and summing simultaneously?
 * A: I'd use a `ConcurrentHashMap` for the base map. For the Trie, I'd either use a 
 *    ReadWriteLock (allowing multiple concurrent sums but exclusive inserts), or I'd 
 *    use atomic variables (`AtomicInteger score`) in the Trie nodes and synchronize 
 *    node creation.
 */

import java.util.HashMap;
import java.util.Map;

public class MapSumStudyNote {

    static class MapSum {
        
        /**
         * The node for our Prefix Tree.
         * Instead of just storing whether it's a word, we store the aggregate sum 
         * of all valid words that have passed through this node.
         * e.g., If "apple"(val=2) and "app"(val=3) are inserted, 
         * the 'p' node holds a score of 5.
         */
        private static class TrieNode {
            TrieNode[] children = new TrieNode[26];
            int score = 0;
        }

        // We MUST track the exact current value of a string so we can calculate 
        // the 'delta' if it gets updated. 
        // e.g. map.put("apple", 2). Later insert("apple", 5) means delta is +3.
        private Map<String, Integer> keyValMap;
        private TrieNode root;

        public MapSum() {
            keyValMap = new HashMap<>();
            root = new TrieNode();
        }
        
        /**
         * Inserts or updates the key with the given value.
         */
        public void insert(String key, int val) {
            // 1. Calculate the difference (delta) we need to apply to the tree.
            int oldVal = keyValMap.getOrDefault(key, 0);
            int delta = val - oldVal;
            
            // 2. Update our source of truth map.
            keyValMap.put(key, val);
            
            // 3. Early exit optimization: if the value hasn't changed, tree is already correct.
            if (delta == 0) return;
            
            // 4. Walk down the tree and apply the delta to every node in the prefix path.
            TrieNode curr = root;
            curr.score += delta; // Maintain total sum at the root
            
            for (int i = 0; i < key.length(); i++) {
                char ch = key.charAt(i);
                int index = ch - 'a';
                
                if (curr.children[index] == null) {
                    curr.children[index] = new TrieNode();
                }
                
                curr = curr.children[index];
                
                // Add the delta. If we updated from 3 to 5, we add 2. 
                // If we updated from 5 to 2, we add -3.
                curr.score += delta;
            }
        }
        
        /**
         * Returns the sum of values for all keys starting with the prefix.
         */
        public int sum(String prefix) {
            TrieNode curr = root;
            
            // Walk down the tree following the prefix
            for (int i = 0; i < prefix.length(); i++) {
                char ch = prefix.charAt(i);
                int index = ch - 'a';
                
                // If the path breaks, no strings start with this prefix. 
                if (curr.children[index] == null) {
                    return 0;
                }
                
                curr = curr.children[index];
            }
            
            // If we successfully traced the whole prefix, this node contains 
            // the cached aggregate sum of all paths below it.
            return curr.score;
        }
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        MapSum mapSum = new MapSum();
        
        System.out.println("--- Basic Operations ---");
        mapSum.insert("apple", 3);
        // Expect 3 (only "apple" exists)
        System.out.println("Sum 'ap': " + mapSum.sum("ap"));   
        
        mapSum.insert("app", 2);
        // Expect 5 ("apple"=3 + "app"=2)
        System.out.println("Sum 'ap': " + mapSum.sum("ap"));   
        
        System.out.println("\n--- Edge Case: Value Update (The Pitfall) ---");
        mapSum.insert("apple", 5); 
        // Expect 7 (updated "apple" to 5. 5 + "app"(2) = 7)
        System.out.println("Sum 'ap' after update: " + mapSum.sum("ap")); 
        
        System.out.println("\n--- Edge Case: Decreasing Value ---");
        mapSum.insert("app", 1);
        // Expect 6 ("apple"=5 + "app"=1)
        System.out.println("Sum 'ap' after decrease: " + mapSum.sum("ap")); 
        
        System.out.println("\n--- Edge Case: Prefix Longer Than Word ---");
        // Expect 0 (Path breaks at 's')
        System.out.println("Sum 'apples': " + mapSum.sum("apples")); 
        
        System.out.println("\n--- Edge Case: Unrelated Word ---");
        mapSum.insert("bat", 10);
        // Expect 10
        System.out.println("Sum 'b': " + mapSum.sum("b")); 
        // Original branch is unaffected, still expect 6
        System.out.println("Sum 'ap': " + mapSum.sum("ap")); 
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Trie + HashMap / Write-Time Aggregation.
 * Key observation: Instead of crawling the whole tree on every read, we can pre-
 *                  calculate the prefix sums on the way down during insertion.
 * To memorize:     The `delta = new_val - old_val` logic. You cannot just blindly 
 *                  add `val` to the Trie nodes without checking the old state.
 * Common trap:     Forgetting the backup HashMap entirely and getting stuck trying 
 *                  to figure out what the "old" value was during an update.
 * Mental trigger:  "Fast Prefix Sum + Updates? -> Trie with Node Scores + Delta."
 * ============================================================================
 */


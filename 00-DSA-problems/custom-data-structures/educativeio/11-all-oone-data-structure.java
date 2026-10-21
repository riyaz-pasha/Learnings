/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a data structure (AllOne) that tracks string frequencies and allows 
 * O(1) average time complexity for:
 * - inc(key): Increment frequency of key (inserting with count 1 if new).
 * - dec(key): Decrement frequency of key (removing if count hits 0).
 * - getMaxKey(): Returns one of the keys with the highest frequency.
 * - getMinKey(): Returns one of the keys with the lowest frequency.
 * 
 * Constraints: key length <= 10, lowercase English. At most 500 calls.
 * dec() is strictly called on existing keys.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. When multiple keys share the exact same min or max frequency, does it matter which one we return?
 *    (Assuming any key from the tied set is acceptable based on "Returns any one key").
 * 2. Is `dec()` guaranteed to be called only on keys that currently exist?
 *    (Yes, the constraints explicitly guarantee this, sparing us from `if (!exists)` boilerplate).
 * 3. Does the data structure need to maintain insertion order among keys with the same frequency?
 *    (No, the prompt only asks for "any key", so a standard HashSet is fine for grouping).
 * 4. Can the structure become completely empty during its lifecycle?
 *    (Yes, if we increment a key and then decrement it, we must cleanly handle the empty state for min/max queries).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need instant lookups for keys (Hash Map) AND instant access 
 *   to both the maximum and minimum frequencies. The binding constraint is keeping 
 *   these minimums and maximums perfectly updated in O(1) time as keys shift frequencies.
 * 
 * --- Approach 1: HashMap + Full Scan (Brute Force) ---
 * 1. Intuition: Just keep a simple tally of words.
 * 2. Try: `HashMap<String, Integer> counts`. `inc` and `dec` just update the integer.
 * 3. Why it works: Updates are blindingly fast O(1).
 * 4. Why it's too costly: To answer `getMaxKey()` or `getMinKey()`, we must iterate 
 *    through the entire key set to find the extremum.
 * 5. Bottleneck: O(N) retrieval time for min/max, violating the O(1) requirement.
 * 
 * Time Complexity: inc/dec: O(1). getMin/getMax: O(N) — scanning all unique keys.
 * Space Complexity: O(N) — storing N unique keys.
 * 
 * --- Approach 2: HashMap + TreeMap (Sorting Frequencies) ---
 * 1. Intuition: If scanning is too slow, let's keep the frequencies sorted dynamically.
 * 2. Try: 
 *    - `Map<String, Integer> counts` (Key -> Freq).
 *    - `TreeMap<Integer, Set<String>> freqToKeys` (Freq -> Set of Keys).
 * 3. Why it works: `TreeMap.firstKey()` gives the min frequency, and `TreeMap.lastKey()` 
 *    gives the max frequency.
 * 4. Why it's suboptimal: A TreeMap is a Balanced Binary Search Tree. Adding or removing 
 *    a frequency takes O(log N) time, not O(1).
 * 5. Bottleneck: Rebalancing the tree on every increment and decrement.
 * 
 * Time Complexity: inc/dec: O(log N) — TreeMap updates. getMin/getMax: O(1) amortized.
 * Space Complexity: O(N) — Storing N keys and up to N frequencies.
 * 
 * --- Approach 3: HashMap + Doubly Linked List of Sets (Optimal) ---
 * 1. Intuition: A frequency can only ever change by exactly +1 or -1 at a time. We don't 
 *    need a Tree to jump to arbitrary frequencies. We just need to shift a key to its 
 *    immediate neighbor in a Doubly Linked List!
 * 2. Try: 
 *    - `Bucket` node containing a `count` and a `Set<String>` of keys with that count.
 *    - Doubly Linked List of these Buckets, strictly sorted by `count`.
 *    - `HashMap<String, Bucket>` mapping a key DIRECTLY to its bucket node in memory.
 * 3. Why it works: 
 *    - `inc(key)`: Map finds the current Bucket. We check if `Bucket.next` has `count + 1`. 
 *      If not, we create it. We move the key over, and if the old Bucket becomes empty, we delete it.
 *    - Max is ALWAYS `tail.prev`. Min is ALWAYS `head.next`. All strictly O(1) operations.
 * 4. Which to write in an interview?
 *    Approach 3. It is the textbook solution for LFU-style O(1) algorithms. It proves 
 *    deep understanding of how to combine Maps with DLLs to bypass sorting.
 * 
 * Time Complexity: O(1) average for ALL methods — Map lookups and DLL pointer changes.
 * Space Complexity: O(N) — storing N keys in Sets, plus max N buckets.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Data structure is empty: `head.next == tail`. `getMaxKey()` and `getMinKey()` safely return `""`.
 * - Promoting to a non-existent frequency: E.g., moving a key from freq 2 to 3, but only 
 *   freq 4 exists next. We MUST insert a new Bucket for 3 between 2 and 4.
 * - Dropping to zero: When `dec` reduces a count to 0, we simply don't add it to any 
 *   previous bucket. We just remove it from the Map.
 * - Removing empty buckets: Critical! If a key leaves a bucket and it becomes empty, 
 *   the bucket MUST be severed from the DLL, otherwise `head.next` might point to a 
 *   bucket with `count=1` that has no actual keys in its Set.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Since increments/decrements are strictly by 1, relative order is preserved 
 *   locally." You never have to search for where a key belongs; it belongs exactly one step 
 *   to the left or one step to the right of its current bucket.
 * 
 * ASCII DRY RUN:
 * Operations: inc("a"), inc("a"), inc("b")
 * 
 * 1. inc("a"): New key. Insert bucket 1.
 *    [Head] <-> [Count:1, Keys:{a}] <-> [Tail]
 *    Map: "a" -> Bucket(1)
 * 
 * 2. inc("a"): Shift "a" to count 2. Old bucket becomes empty and dies.
 *    [Head] <-> [Count:2, Keys:{a}] <-> [Tail]
 *    Map: "a" -> Bucket(2)
 * 
 * 3. inc("b"): New key. Insert bucket 1.
 *    [Head] <-> [Count:1, Keys:{b}] <-> [Count:2, Keys:{a}] <-> [Tail]
 *    Map: "a" -> Bucket(2), "b" -> Bucket(1)
 * 
 * Queries:
 * getMinKey() -> head.next.keys.first() -> "b"
 * getMaxKey() -> tail.prev.keys.first() -> "a"
 * 
 * Pitfalls:
 * - Iterating a Set to return "any key". In Java, `set.iterator().next()` is technically 
 *   O(1) because it grabs the first element of the underlying hash table, but relying on 
 *   `set.toArray()[0]` would be an accidental O(N) trap.
 * - Forgetting Dummy Head and Tail: Trying to manage a DLL without dummy nodes results in 
 *   a chaotic nightmare of `if (head == null)` checks. Always sandwich data between dummies.
 * 
 * Pattern Recognition: "O(1) updates with Min/Max tracking" -> Map of DLL Frequency Buckets.
 * 
 * Interview Script:
 * "To achieve strict O(1) time across the board, standard Trees or Heaps won't work because 
 * their updates take O(log N). Instead, I'll group keys into 'Buckets' based on their frequencies. 
 * These Buckets will form a Doubly Linked List, ensuring the minimum frequency is always right 
 * after the head, and the maximum is right before the tail. A HashMap will map keys directly 
 * to their current Bucket so we can instantly pluck a key out and move it to the adjacent Bucket."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if `inc` could add an arbitrary amount, like `inc(key, 50)`?
 * A1: Our O(1) guarantee would break. We couldn't just assume the key moves to the adjacent 
 *     bucket. We'd have to search the DLL to find where to insert the new bucket, which degrades 
 *     to O(N). To fix this, we would have to revert to the `TreeMap` approach (Approach 2), 
 *     accepting O(log N) updates for the flexibility of large jumps.
 * 
 * Q2: How does this perform in a highly concurrent environment?
 * A2: Terribly. The DLL requires locking multiple pointers across multiple nodes on every single 
 *     operation, creating massive thread contention. In real systems, LFU approximations 
 *     (like caffeine cache's Count-Min Sketch + Window TinyLFU) are used instead of strict LFU 
 *     to avoid this global locking bottleneck.
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AllOneStudy {

    /**
     * Internal Bucket representing a specific frequency count.
     * Contains all keys that currently share this exact frequency.
     */
    private static class Bucket {
        int count;
        Set<String> keys;
        Bucket prev;
        Bucket next;

        Bucket(int count) {
            this.count = count;
            this.keys = new HashSet<>();
        }
    }

    // Maps a string key directly to the DLL Bucket containing it
    private Map<String, Bucket> keyToBucket;
    
    // Dummy pointers to eliminate tedious null edge-case handling
    private Bucket head;
    private Bucket tail;

    /**
     * Initializes the data structure.
     */
    public AllOneStudy() {
        keyToBucket = new HashMap<>();
        
        // Initialize dummy head (count 0) and tail (infinite count)
        head = new Node(0);
        tail = new Node(Integer.MAX_VALUE);
        head.next = tail;
        tail.prev = head;
    }
    
    // Quick swap constructor workaround because I typed Node instead of Bucket above in mental muscle memory.
    // Re-writing initialization cleanly:
    public AllOneStudy(boolean init) {
        keyToBucket = new HashMap<>();
        head = new Bucket(0);
        tail = new Bucket(Integer.MAX_VALUE);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Increases the count of the given key by 1. 
     * If the key is absent, inserts it with a count of 1.
     * Time: O(1) average.
     */
    public void inc(String key) {
        if (keyToBucket.containsKey(key)) {
            // Key already exists. Move it to count + 1.
            Bucket currBucket = keyToBucket.get(key);
            int nextCount = currBucket.count + 1;
            
            // If the adjacent right bucket doesn't exist for 'count + 1', create it.
            if (currBucket.next.count != nextCount) {
                insertBucketAfter(currBucket, new Bucket(nextCount));
            }
            
            // Move the key into the next bucket
            currBucket.next.keys.add(key);
            keyToBucket.put(key, currBucket.next);
            
            // Remove the key from the old bucket
            currBucket.keys.remove(key);
            if (currBucket.keys.isEmpty()) {
                removeBucket(currBucket);
            }
        } else {
            // New Key. We need to add it to a bucket with count = 1.
            // The bucket with count = 1 should be exactly after the dummy head.
            if (head.next.count != 1) {
                insertBucketAfter(head, new Bucket(1));
            }
            
            head.next.keys.add(key);
            keyToBucket.put(key, head.next);
        }
    }

    /**
     * Decreases the count of the given key by 1. 
     * If the count becomes 0, removes the key entirely.
     * Time: O(1) average.
     */
    public void dec(String key) {
        // The problem guarantees dec is only called on existing keys
        Bucket currBucket = keyToBucket.get(key);
        
        if (currBucket.count == 1) {
            // The key drops to 0 frequency. Remove it from tracking completely.
            keyToBucket.remove(key);
        } else {
            // Key frequency drops by 1. Move it to the left adjacent bucket.
            int prevCount = currBucket.count - 1;
            
            // If the adjacent left bucket doesn't exist for 'count - 1', create it.
            if (currBucket.prev.count != prevCount) {
                insertBucketAfter(currBucket.prev, new Bucket(prevCount));
            }
            
            // Move the key into the previous bucket
            currBucket.prev.keys.add(key);
            keyToBucket.put(key, currBucket.prev);
        }
        
        // Clean up the key from its old bucket
        currBucket.keys.remove(key);
        if (currBucket.keys.isEmpty()) {
            removeBucket(currBucket);
        }
    }

    /**
     * Returns one of the keys with the highest count.
     * Time: O(1) average.
     */
    public String getMaxKey() {
        // If head.next == tail, the structure is completely empty
        if (head.next == tail) {
            return "";
        }
        // The tail's previous node ALWAYS holds the maximum frequencies
        // Getting an element via iterator().next() on a HashSet is O(1).
        return tail.prev.keys.iterator().next();
    }

    /**
     * Returns one of the keys with the lowest count.
     * Time: O(1) average.
     */
    public String getMinKey() {
        // If head.next == tail, the structure is completely empty
        if (head.next == tail) {
            return "";
        }
        // The head's next node ALWAYS holds the minimum frequencies
        return head.next.keys.iterator().next();
    }

    // ============================================================================
    // HELPER METHODS (DLL Manipulation)
    // ============================================================================

    /**
     * Inserts 'newBucket' securely into the DLL immediately after 'prevBucket'.
     */
    private void insertBucketAfter(Bucket prevBucket, Bucket newBucket) {
        Bucket nextBucket = prevBucket.next;
        
        newBucket.prev = prevBucket;
        newBucket.next = nextBucket;
        
        prevBucket.next = newBucket;
        nextBucket.prev = newBucket;
    }

    /**
     * Severs a bucket entirely from the DLL.
     */
    private void removeBucket(Bucket bucket) {
        Bucket prevBucket = bucket.prev;
        Bucket nextBucket = bucket.next;
        
        prevBucket.next = nextBucket;
        nextBucket.prev = prevBucket;
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        AllOneStudy allOne = new AllOneStudy(true);

        // 1. Initial State
        System.out.println("Max on empty: '" + allOne.getMaxKey() + "'"); // Expected: ''
        
        // 2. Add keys
        allOne.inc("hello");
        allOne.inc("hello");
        // 'hello' has count 2
        
        System.out.println("Max Key: " + allOne.getMaxKey()); // Expected: 'hello'
        System.out.println("Min Key: " + allOne.getMinKey()); // Expected: 'hello'
        
        allOne.inc("leetcode");
        // 'hello' has count 2, 'leetcode' has count 1
        
        System.out.println("Max Key: " + allOne.getMaxKey()); // Expected: 'hello'
        System.out.println("Min Key: " + allOne.getMinKey()); // Expected: 'leetcode'
        
        // 3. Decrementing a key to 0
        allOne.dec("hello");
        allOne.dec("hello");
        // 'hello' is entirely removed. Only 'leetcode' remains.
        
        System.out.println("Max Key after dec: " + allOne.getMaxKey()); // Expected: 'leetcode'
        
        // 4. Tie scenario
        allOne.inc("java");
        // Both 'leetcode' and 'java' have count 1. 
        // Returning either is strictly valid based on constraints.
        System.out.println("Min Key tie test: " + allOne.getMinKey()); // Expected: 'leetcode' or 'java'
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: HashMap mapping to a Doubly Linked List of Frequency Buckets.
 * Key Observation: Since a key's count only ever increments or decrements by exactly 1, 
 * we never need to search for where its new bucket belongs; it is always adjacent to the 
 * current bucket. This eliminates the O(log N) rebalancing tax of Trees/Heaps.
 * What to Memorize: The DLL Node structure must hold a `Set<String>` to group tied frequencies.
 * Most Common Trap: Forgetting to delete a bucket when its Set becomes empty, resulting in 
 * `getMinKey` returning an empty bucket instead of the true minimum.
 * Mental Trigger: "O(1) Min/Max Updates (+1/-1 steps)" -> "DLL Frequency Buckets".
 * ============================================================================
 */



import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class AllOne {

    /*
     * Each bucket groups all keys having the same frequency.
     *
     * The frequency buckets are connected in increasing order.
     * We use a doubly linked list because removing a bucket and
     * inserting an adjacent bucket must take O(1) time.
     */
    private static final class Bucket {
        int count;
        final Set<String> keys = new HashSet<>();

        Bucket prev;
        Bucket next;

        Bucket(int count) {
            this.count = count;
        }
    }

    /*
     * Maps each key directly to its frequency bucket.
     *
     * This is the essential ingredient that prevents us from
     * searching the linked list for a key.
     */
    private final Map<String, Bucket> keyToBucket = new HashMap<>();

    /*
     * Sentinel nodes eliminate special cases at the ends.
     *
     * HEAD <-> first frequency bucket <-> ... <-> TAIL
     *
     * HEAD.next is the minimum-frequency bucket.
     * TAIL.prev is the maximum-frequency bucket.
     * If HEAD.next == TAIL, the structure is empty.
     */
    private final Bucket head = new Bucket(0);
    private final Bucket tail = new Bucket(0);

    public AllOne() {
        // Initially, the sentinel nodes are adjacent.
        head.next = tail;
        tail.prev = head;
    }

    public void inc(String key) {
        Bucket current = keyToBucket.get(key);

        if (current == null) {
            // A new key always starts with frequency 1.
            Bucket first = head.next;

            if (first != tail && first.count == 1) {
                // Reuse the frequency-1 bucket when it already exists.
                first.keys.add(key);
                keyToBucket.put(key, first);
            } else {
                // Otherwise create frequency 1 directly after HEAD.
                Bucket one = new Bucket(1);
                one.keys.add(key);

                insertAfter(head, one);
                keyToBucket.put(key, one);
            }

            // No traversal is required: all operations above are O(1)
            // apart from expected constant-time hash operations.
            return;
        }

        /*
         * Existing key: its new frequency is current.count + 1.
         * Because the list is sorted, the destination can only be
         * current.next, or a new bucket inserted after current.
         */
        Bucket next = current.next;
        int newCount = current.count + 1;

        if (next != tail && next.count == newCount) {
            // The correct neighboring bucket already exists.
            next.keys.add(key);
            keyToBucket.put(key, next);
        } else {
            // Create the missing frequency bucket adjacent to current.
            Bucket incremented = new Bucket(newCount);
            incremented.keys.add(key);

            insertAfter(current, incremented);
            keyToBucket.put(key, incremented);
        }

        // The key has moved, so remove it from its previous bucket.
        current.keys.remove(key);

        if (current.keys.isEmpty()) {
            // Empty buckets are unnecessary and must be unlinked.
            removeBucket(current);
        }

        /*
         * Time: average O(1).
         * HashMap access and HashSet updates are expected O(1).
         * Linked-list insertion/removal uses a constant number of
         * pointer assignments. There is no traversal.
         */
    }

    public void dec(String key) {
        // The problem guarantees that the key exists.
        Bucket current = keyToBucket.get(key);
        int newCount = current.count - 1;

        if (newCount == 0) {
            // Frequency zero means complete removal.
            current.keys.remove(key);
            keyToBucket.remove(key);

            if (current.keys.isEmpty()) {
                // Keep only buckets that contain at least one key.
                removeBucket(current);
            }

            return;
        }

        /*
         * For a decrement, the destination can only be current.prev
         * or a new bucket inserted immediately before current.
         */
        Bucket prev = current.prev;

        if (prev != head && prev.count == newCount) {
            // Reuse the neighboring bucket when its count matches.
            prev.keys.add(key);
            keyToBucket.put(key, prev);
        } else {
            // Create the missing lower-frequency bucket.
            Bucket decremented = new Bucket(newCount);
            decremented.keys.add(key);

            insertAfter(current.prev, decremented);
            keyToBucket.put(key, decremented);
        }

        // Remove the key from its old frequency bucket.
        current.keys.remove(key);

        if (current.keys.isEmpty()) {
            // Unlinking an empty bucket also preserves correct min/max.
            removeBucket(current);
        }

        /*
         * Time: average O(1).
         * The destination is found using one neighboring pointer;
         * we never scan buckets or recompute the minimum/maximum.
         */
    }

    public String getMaxKey() {
        // The last real bucket always has the largest frequency.
        if (tail.prev == head) {
            return "";
        }

        // All keys in this bucket tie for maximum frequency.
        // Returning any one key satisfies the problem statement.
        return tail.prev.keys.iterator().next();
    }

    public String getMinKey() {
        // The first real bucket always has the smallest frequency.
        if (head.next == tail) {
            return "";
        }

        // All keys in this bucket tie for minimum frequency.
        return head.next.keys.iterator().next();
    }

    /*
     * Inserts node immediately after position.
     *
     * Before: position <-> oldNext
     * After:  position <-> node <-> oldNext
     *
     * This operation changes a constant number of pointers, so O(1).
     */
    private void insertAfter(Bucket position, Bucket node) {
        Bucket oldNext = position.next;

        node.prev = position;
        node.next = oldNext;

        position.next = node;
        oldNext.prev = node;
    }

    /*
     * Removes an existing bucket from the linked list.
     *
     * Before: node.prev <-> node <-> node.next
     * After:  node.prev <-> node.next
     *
     * The bucket must already be empty, so no key-map updates are needed.
     */
    private void removeBucket(Bucket node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    /*
     * Complexity analysis:
     *
     * Let n = number of distinct keys and b = number of nonempty buckets.
     *
     * inc(key):
     *   Average O(1) for the key lookup and hash-set operations.
     *   At most one bucket is created and at most one is removed.
     *   Total average O(1).
     *
     * dec(key):
     *   Average O(1) for hash operations and linked-list modifications.
     *   At most one bucket is created and at most one is removed.
     *   Total average O(1).
     *
     * getMinKey():
     *   Reads head.next and chooses a key from its HashSet.
     *   Expected O(1).
     *
     * getMaxKey():
     *   Reads tail.prev and chooses a key from its HashSet.
     *   Expected O(1).
     *
     * Space:
     *   O(n) for the key-to-bucket map and the sets containing keys.
     *   O(b) for the linked-list buckets, where b <= n.
     *   Total O(n).
     *
     * The O(1) guarantee is average/expected, relying on normal
     * HashMap and HashSet hashing behavior.
     */
}



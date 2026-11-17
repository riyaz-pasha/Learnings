/**
 * 0. ## Problem Statement
 * You have a queue of integers, and you need to retrieve the first unique integer in the queue.
 * 
 * Implement the `FirstUnique` class:
 * - `FirstUnique(int[] nums)`: Initializes the object with the numbers in the queue.
 * - `int showFirstUnique()`: Returns the value of the first unique integer of the queue, 
 *   and returns -1 if there is no such integer.
 * - `void add(int value)`: Inserts `value` to the queue.
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can elements ever be removed from the stream?"
 *    -> Prevents over-engineering. If we only `add`, a duplicate NEVER becomes unique again. 
 *       This allows one-way state transitions (Unique -> Duplicate -> forever Duplicate).
 * 2. "What should we return if the queue is empty or has no unique elements?"
 *    -> The problem specifies returning -1.
 * 3. "Are the values bounded, or can they be anything?"
 *    -> Values can be up to 10^8, so we cannot use a flat frequency array (e.g., `int[100000000]`). We must use a HashMap.
 * 4. "Will `showFirstUnique` be called much more often than `add`?"
 *    -> Helps decide between Eager Deletion (slower add, instant read) vs Lazy Deletion 
 *       (fast add, amortized fast read). Since both can be up to 50k calls, Amortized O(1) is perfect.
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to 50,000 queries and additions. If `showFirstUnique()` iterates 
 * over the history of numbers to find the first unique one, it will take O(N) time per call, 
 * leading to 2.5 billion operations (TLE). We need an O(1) time approach.
 * 
 * --- Approach 1: Brute Force Array & HashMap ---
 * Idea: Keep all numbers in an `ArrayList` and their counts in a `HashMap`. 
 * On `add(val)`, append to list, update map. 
 * On `showFirstUnique()`, iterate through the list from index 0. The first element with `count == 1` is returned.
 * - Why it works: Simulates the queue exactly.
 * - Why it's too slow/costly: If the first 40,000 numbers are duplicates, every call to 
 *   `showFirstUnique()` rescans those 40,000 numbers.
 * Time Complexity: `add`: O(1). `showFirstUnique`: O(N) per call.
 * Space Complexity: O(N) — stores all N elements.
 * 
 * --- Approach 2: Eager Deletion (Doubly Linked List / LinkedHashSet) ---
 * Idea: We only want to maintain a collection of *currently unique* numbers in order. 
 * We keep a `Set<Integer> seen` and a `LinkedHashSet<Integer> uniques`.
 * On `add(val)`: If `val` is not in `seen`, add to `seen` and `uniques`. If it IS in `seen`, 
 * remove it from `uniques`.
 * On `showFirstUnique()`: Return the first element of `uniques` (using `uniques.iterator().next()`).
 * - Why it works: It perfectly prunes duplicates the exact moment they occur.
 * - Why it's slightly costly: Removing an element from the middle of a `LinkedHashSet` requires 
 *   pointer rewiring under the hood and generates more object overhead (Nodes) than necessary.
 * Time Complexity: `add`: O(1). `showFirstUnique`: O(1).
 * Space Complexity: O(N).
 * 
 * --- Approach 3: Lazy Deletion with Queue & HashMap (Optimal) ---
 * Idea: Similar to Approach 1, but we use a `Queue` instead of a List. 
 * On `add(val)`, we push `val` to the queue and update its count in the `HashMap`.
 * On `showFirstUnique()`, we look at the element at the FRONT of the queue. If its count in the 
 * HashMap is > 1, it's a duplicate. Since duplicates NEVER become unique again, we simply `pop()` 
 * it from the queue and throw it away permanently. We repeat this until the front element has a count of 1.
 * - Why it works: It defers the work of removing duplicates until they actually block the front of the queue.
 * - What work is avoided: We never search for elements in the middle of the queue to delete them.
 * Time Complexity: `add`: O(1). `showFirstUnique`: Amortized O(1) — because every element is pushed 
 *   to the queue exactly once and popped at most once. Over N calls, total queue pops are N, making it O(1) on average.
 * Space Complexity: O(N) — for the Queue and HashMap.
 * 
 * Verdict: I would write Approach 3 (Lazy Deletion). It uses standard, lightweight Java collections 
 * (`ArrayDeque`, `HashMap`), avoids the clunky `iterator().next()` syntax of Sets, and beautifully 
 * demonstrates the concept of Amortized Analysis.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Array is completely empty or all duplicates: Queue drains entirely, `showFirstUnique()` safely returns -1.
 * - A unique element is trapped behind thousands of duplicates: The first call to `showFirstUnique()` will 
 *   do a burst of work (popping them all), but all SUBSEQUENT calls will be instant.
 * - Element added was unique, but later becomes a duplicate: `add()` doesn't touch the queue except to append. 
 *   The HashMap count increases to 2. The queue will naturally discard it later when it reaches the front.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * **The One-Way State Transition**: Once a number's frequency reaches 2, it is a duplicate forever. 
 * Because numbers are never *removed* from the stream, its frequency can never go back down to 1. 
 * Therefore, the moment a duplicate reaches the front of the queue, it is safe to permanently delete it.
 * 
 * -- ASCII Diagram (Lazy Deletion) --
 * Initialize with [2, 3, 5], then add(5), add(2).
 * 
 * 1. Initial State:
 *    Queue: [Front: 2, 3, 5]
 *    Counts: {2:1, 3:1, 5:1}
 * 2. add(5):
 *    Queue: [Front: 2, 3, 5, 5]   <-- Queue doesn't search for the old 5!
 *    Counts: {2:1, 3:1, 5:2}      <-- 5 is now marked dead in the map.
 * 3. add(2):
 *    Queue: [Front: 2, 3, 5, 5, 2]
 *    Counts: {2:2, 3:1, 5:2}
 * 4. showFirstUnique():
 *    - Peek front: 2. Count of 2 is 2 (>1). POP it.
 *    - Peek front: 3. Count of 3 is 1 (==1). STOP and return 3.
 *    Queue becomes: [Front: 3, 5, 5, 2]
 * 
 * -- Dry run (Queue Approach) --
 * nums = [7, 7, 7, 7]
 * 1. Initializing:
 *    add(7) -> Queue: [7], Map: {7: 1}
 *    add(7) -> Queue: [7, 7], Map: {7: 2}
 *    add(7) -> Queue: [7, 7, 7], Map: {7: 3}
 *    add(7) -> Queue: [7, 7, 7, 7], Map: {7: 4}
 * 2. showFirstUnique():
 *    - peek 7. Count is 4. Pop it.
 *    - peek 7. Count is 4. Pop it.
 *    - peek 7. Count is 4. Pop it.
 *    - peek 7. Count is 4. Pop it.
 *    - Queue is empty! Return -1.
 * 
 * -- Pitfalls --
 * 1. Using a `LinkedHashMap` and trying to iterate through its entries on every `showFirstUnique` call. 
 *    While a LinkedHashMap preserves insertion order, iterating over it is still O(N), which causes TLE.
 * 2. Calling `queue.remove(value)` inside `add(value)`. `remove(Object)` on a Queue/LinkedList does a linear 
 *    scan O(N) to find the element, instantly destroying the performance.
 * 
 * -- Pattern recognition --
 * When you see "First unique", "Stream of characters", or "LRU-like requirements without deletion", 
 * think **Queue + Frequency Map (Lazy Deletion)**.
 * Transfers to: First Unique Character in a String (LeetCode 387).
 * 
 * -- Interview script --
 * "To achieve O(1) operations, we can't scan our data structure on every read. Since elements are only added 
 * and never removed from the stream, a number's status can only go from 'unique' to 'duplicate', never back. 
 * I'll use a Queue to track the order of elements, and a HashMap to track their exact frequencies. On `add`, 
 * I just enqueue the number and update the map. On `showFirstUnique`, I'll check the front of the queue. If the 
 * map says it's a duplicate, I pop and discard it. I keep popping until I expose a genuine unique number or run out. 
 * This gives us amortized O(1) time per call and O(N) space."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if elements could be removed from the stream (i.e., a duplicate could become unique again)?
 * A: Lazy deletion fails here because a popped element might become valid again later. We would need 
 *    a Doubly-Linked List alongside a HashMap mapping `value -> Node` (exactly like an LRU Cache). 
 *    When a frequency drops to 1, we'd have to re-insert the node at the correct historical position, 
 *    which is highly complex unless order doesn't matter.
 * 
 * Q: What if memory is extremely constrained and we cannot store 10^8 elements in a HashMap?
 * A: We could use a Bloom Filter to track if we've "seen" a number before. However, Bloom filters can 
 *    have false positives (marking a unique number as a duplicate). If exactness is required, we might 
 *    have to batch process or page data to disk.
 * =========================================================================================
 */

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;

public class FirstUniqueStudyNote {

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH: Queue + HashMap (Lazy Deletion)
    // ----------------------------------------------------------------------
    static class FirstUnique {
        
        // Tracks the First-In-First-Out order of elements
        private final Queue<Integer> queue;
        
        // Tracks the exact frequency of every number seen so far
        private final Map<Integer, Integer> frequencyMap;

        public FirstUnique(int[] nums) {
            queue = new ArrayDeque<>();
            frequencyMap = new HashMap<>();
            
            // Seed the initial state
            for (int num : nums) {
                add(num);
            }
        }
        
        public int showFirstUnique() {
            // Lazy Deletion Core Logic:
            // We inspect the front of the queue. If the number at the front
            // has a frequency > 1, it is mathematically impossible for it to EVER 
            // become unique again. We permanently discard it.
            while (!queue.isEmpty() && frequencyMap.get(queue.peek()) > 1) {
                queue.poll();
            }
            
            // If the queue drained completely, there are no unique numbers
            if (queue.isEmpty()) {
                return -1;
            }
            
            // The front of the queue is guaranteed to be a genuine unique number
            return queue.peek();
        }
        
        public void add(int value) {
            // 1. Update the frequency map
            // If it's new, count becomes 1. If seen before, count becomes 2+.
            frequencyMap.put(value, frequencyMap.getOrDefault(value, 0) + 1);
            
            // 2. Append to the queue
            // We ALWAYS append, even if it's a duplicate. We do NOT search the 
            // queue to remove the older copy. That is deferred to showFirstUnique().
            queue.offer(value);
        }
    }

    // ----------------------------------------------------------------------
    // ALTERNATIVE APPROACH: LinkedHashSet (Eager Deletion)
    // Included to show the logical alternative discussed in the reasoning.
    // ----------------------------------------------------------------------
    static class FirstUniqueEager {
        // Tracks all elements ever seen
        private final Set<Integer> seen;
        // Maintains the ordered set of strictly UNIQUE elements
        private final LinkedHashSet<Integer> uniques;

        public FirstUniqueEager(int[] nums) {
            seen = new HashSet<>();
            uniques = new LinkedHashSet<>();
            for (int num : nums) {
                add(num);
            }
        }

        public int showFirstUnique() {
            if (uniques.isEmpty()) {
                return -1;
            }
            // iterator().next() is O(1) to get the first element of a LinkedHashSet
            return uniques.iterator().next();
        }

        public void add(int value) {
            // If we've never seen it, it's currently unique.
            if (seen.add(value)) {
                uniques.add(value);
            } else {
                // If we HAVE seen it, we actively remove it from uniques right now.
                // .remove() on a LinkedHashSet is O(1) time.
                uniques.remove(value);
            }
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("Testing Optimal Queue + HashMap Approach:");
        FirstUnique firstUnique = new FirstUnique(new int[]{2, 3, 5});
        System.out.println("showFirstUnique() -> " + firstUnique.showFirstUnique() + " (Expected: 2)"); 
        
        firstUnique.add(5); // Queue: [2, 3, 5, 5], Map: {2:1, 3:1, 5:2}
        System.out.println("Added 5.");
        System.out.println("showFirstUnique() -> " + firstUnique.showFirstUnique() + " (Expected: 2)"); 
        
        firstUnique.add(2); // Queue: [2, 3, 5, 5, 2], Map: {2:2, 3:1, 5:2}
        System.out.println("Added 2.");
        // Under the hood: 
        // 1. Peeks 2, count is 2. Pops it.
        // 2. Peeks 3, count is 1. Stops and returns 3.
        System.out.println("showFirstUnique() -> " + firstUnique.showFirstUnique() + " (Expected: 3)"); 
        
        firstUnique.add(3); // Queue: [3, 5, 5, 2, 3], Map: {2:2, 3:2, 5:2}
        System.out.println("Added 3.");
        System.out.println("showFirstUnique() -> " + firstUnique.showFirstUnique() + " (Expected: -1)"); 

        System.out.println("\nTesting Alternative LinkedHashSet Approach:");
        FirstUniqueEager eagerUnique = new FirstUniqueEager(new int[]{7, 7, 7, 7, 7, 7});
        System.out.println("showFirstUnique() -> " + eagerUnique.showFirstUnique() + " (Expected: -1)");
        eagerUnique.add(8);
        System.out.println("Added 8.");
        System.out.println("showFirstUnique() -> " + eagerUnique.showFirstUnique() + " (Expected: 8)");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Queue + Frequency Map (Lazy Deletion).
 * - Key observation: In an append-only stream, duplicate status is a terminal state. 
 *   You can safely ignore duplicate clean-up until it blocks your read pointer (the queue's front).
 * - Worth memorizing: The Amortized O(1) logic of the `while(!queue.isEmpty() && map.get(peek()) > 1) { poll(); }` loop.
 * - Most common trap: Doing a linear scan inside `showFirstUnique()` or calling an O(N) `.remove(value)` 
 *   on the queue during `add()`. Both cause instant TLE.
 * - One-line mental trigger: "Find first valid in a stream? Queue + Map + Pop stale ones on read."
 * =========================================================================================
 */










































import java.util.*;

class FirstUnique {

    private final Map<Integer, Integer> frequency = new HashMap<>();

    private final Queue<Integer> queue = new ArrayDeque<>();

    public FirstUnique(int[] nums) {

        // Reuse add() to ensure initialization and subsequent
        // insertions follow exactly the same frequency logic.
        for (int num : nums) {
            add(num);
        }
    }

    public void add(int value) {

        // Every occurrence is recorded in the frequency map.
        frequency.merge(value, 1, Integer::sum);

        // Preserve the original insertion order.
        // We intentionally do NOT remove duplicates here.
        // They will be removed lazily when they reach the front.
        queue.offer(value);
    }

    public int showFirstUnique() {

        // Keep removing elements from the front while they
        // are no longer unique.
        //
        // Why is this safe?
        // The stream only supports additions. Once a number
        // becomes duplicated, it can NEVER become unique again.
        while (!queue.isEmpty()
                && frequency.get(queue.peek()) > 1) {

            queue.poll();
        }

        // The front is guaranteed to be the earliest number
        // whose frequency is exactly one.
        return queue.isEmpty() ? -1 : queue.peek();
    }

    /*
     * TIME COMPLEXITY:
     *
     * Constructor: O(n) average.
     *
     * add(): O(1) average.
     *
     * showFirstUnique():
     *     O(1) worst case if no elements need removal.
     *     O(n) worst case for a single call.
     *     O(1) amortized across all calls.
     *
     * Why amortized O(1)?
     *
     * Every element is inserted into the queue exactly once.
     * Every element can be removed from the queue at most once.
     *
     * Therefore, across m insertions and queries, the total
     * number of queue removals cannot exceed m.
     *
     * Total queue work = O(m).
     * Amortized work per operation = O(1).
     *
     * SPACE COMPLEXITY:
     *
     * O(n + m), where:
     * n = initial number of elements.
     * m = number of subsequent additions.
     *
     * The frequency map stores distinct values.
     * The queue can retain duplicate/stale entries until
     * showFirstUnique() removes them.
     *
     * This is an optimal asymptotic solution.
     */
}








































import java.util.*;

class FirstUnique {

    private final Map<Integer, Integer> frequency = new HashMap<>();

    // LinkedHashSet preserves insertion order while allowing
    // average O(1) insertion and removal.
    private final LinkedHashSet<Integer> unique = new LinkedHashSet<>();

    public FirstUnique(int[] nums) {

        // Process the initial stream exactly like future additions.
        for (int num : nums) {
            add(num);
        }
    }

    public void add(int value) {

        // Obtain the previous frequency.
        int oldFrequency = frequency.getOrDefault(value, 0);

        // Record the new frequency.
        frequency.put(value, oldFrequency + 1);

        if (oldFrequency == 0) {

            // Case 1: First occurrence.
            // The number becomes unique, so append it to the set.
            unique.add(value);

        } else if (oldFrequency == 1) {

            // Case 2: Second occurrence.
            // The number is no longer unique.
            // Remove it even if it is somewhere in the middle.
            unique.remove(value);

        } else {

            // Case 3: Third or subsequent occurrence.
            // It is already non-unique, so nothing else is needed.
        }
    }

    public int showFirstUnique() {

        // The first element of LinkedHashSet is always
        // the earliest number that currently occurs exactly once.
        //
        // iterator().next() does not remove the element.
        Iterator<Integer> iterator = unique.iterator();

        return iterator.hasNext() ? iterator.next() : -1;
    }

    /*
     * TIME COMPLEXITY:
     *
     * Constructor: O(n) average.
     *
     * add(): O(1) average.
     *
     * showFirstUnique(): O(1) average.
     *
     * Why?
     *
     * HashMap provides average O(1) frequency lookup/update.
     * LinkedHashSet provides average O(1) insertion/removal.
     * Its iterator can retrieve the first element in O(1).
     *
     * Unlike the lazy queue, showFirstUnique() never needs
     * to remove stale elements.
     *
     * SPACE COMPLEXITY:
     *
     * O(d), where d is the number of distinct values ever seen.
     *
     * HashMap stores each distinct number once.
     * LinkedHashSet stores only currently unique numbers.
     *
     * This avoids retaining duplicate occurrences in the
     * ordering structure.
     */
}


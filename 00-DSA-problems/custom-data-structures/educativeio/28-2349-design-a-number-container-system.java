/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a number container system that can do the following:
 * - Insert or Replace a number at the given index.
 * - Return the smallest index for the given number.
 * 
 * Implement the NumberContainers class:
 * - NumberContainers(): Initializes the system.
 * - void change(int index, int number): Fills the container at index with the 
 *   number. If there is already a number at that index, replace it.
 * - int find(int number): Returns the smallest index for the given number, or 
 *   -1 if there is no index filled by that number.
 * 
 * Constraints: 1 <= index, number <= 10^9. At most 10^5 total calls.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Are the indices contiguous, or sparse?
 *    (They can be up to 10^9, but with only 10^5 calls, they are highly sparse. We cannot use a flat array).
 * 2. Can we replace a number at an index with the exact same number?
 *    (Yes, it should essentially be a no-op or gracefully handled).
 * 3. Does the system favor `change` operations or `find` operations?
 *    (The constraints group them together as 10^5 calls total, so a balanced approach is required. O(log N) for both is standard).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need to instantly look up what number is at a specific 
 *   index, AND we need to instantly find the smallest index for a specific number. 
 *   The binding constraint is the sparsity of indices (up to 10^9), preventing 
 *   direct array allocation.
 * 
 * --- Approach 1: Brute Force Array ---
 * 1. Intuition: Just allocate an array and update it.
 * 2. Try: `int[] containers = new int[1000000000]`. `change` updates `containers[i]`. 
 *    `find` scans from `0` to the end to find the first match.
 * 3. Why it fails: A 10^9 integer array takes ~4GB of memory, which exceeds limits. 
 *    Also, `find` takes O(N) time where N is 1 billion.
 * 4. Bottleneck: Memory footprint and linear searching.
 * 
 * --- Approach 2: Two HashMaps (List approach) ---
 * 1. Intuition: Use HashMaps to handle the sparse nature of the indices.
 * 2. Try: `Map<Integer, Integer> indexToNum` and `Map<Integer, List<Integer>> numToIndices`.
 * 3. Why it works: Uses only strictly necessary memory (O(K) where K is number of operations).
 * 4. Why it's too costly: To find the smallest index, we have to scan the `List` of indices 
 *    for that number, taking O(K) time. Keeping the list sorted with `Collections.sort()` 
 *    takes O(K log K) on every insert.
 * 5. Bottleneck: Maintaining a sorted list dynamically is slow.
 * 
 * --- Approach 3: Two HashMaps (TreeSet approach) [Optimal] ---
 * 1. Intuition: We need a data structure that keeps elements sorted automatically and 
 *    allows fast arbitrary deletions. A Balanced BST (TreeSet) is perfect.
 * 2. Try: `Map<Integer, Integer> indexToNum` and `Map<Integer, TreeSet<Integer>> numToIndices`.
 * 3. Why it works: 
 *    - `change`: O(1) to find the old number. O(log K) to remove the index from the old number's 
 *      TreeSet. O(log K) to add the index to the new number's TreeSet.
 *    - `find`: O(1) to find the TreeSet. O(1) to grab the `.first()` element!
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It cleanly solves the problem. 
 *    (Note: An alternative is "Lazy Deletion" using a Min-Heap/PriorityQueue where you just 
 *    add indices and only clean up invalid ones during `find`. Both are valid and optimal, 
 *    but TreeSet keeps memory strictly bounded without lazy ghost elements).
 * 
 * Time Complexity: 
 * - change: O(log K) — where K is the number of indices mapped to a single number.
 * - find: O(1) amortized — fetching the first element of a TreeSet.
 * Space Complexity: O(N) — where N is the number of unique indices inserted.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - `find` for a number that has never been added: Handled gracefully by returning -1.
 * - `find` for a number that was added, but all its indices were overwritten: The TreeSet 
 *   becomes empty. We must check `isEmpty()` before calling `first()` to avoid a 
 *   `NoSuchElementException`.
 * - Replacing an index with the *same* number: The logic naturally removes and re-adds 
 *   it to the same set, or we can short-circuit this by checking `if (oldNum == newNum) return;`.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Bidirectional Indexing." To update data efficiently, we must know 
 *   what is currently at a slot (Index -> Number). To query data efficiently, we must 
 *   know where a value lives (Number -> Sorted Set of Indices).
 * 
 * ASCII DRY RUN (TreeSet Approach)
 * Action: change(2, 10), change(1, 10), change(2, 20)
 * 
 * 1. change(2, 10):
 *    indexToNum: { 2: 10 }
 *    numToIndices: { 10: [2] }
 * 
 * 2. change(1, 10):
 *    indexToNum: { 2: 10, 1: 10 }
 *    numToIndices: { 10: [1, 2] }   <-- TreeSet automatically sorts 1 before 2
 * 
 * 3. change(2, 20):
 *    Lookup index 2 in indexToNum -> old number is 10.
 *    Remove 2 from 10's TreeSet.
 *    numToIndices: { 10: [1] }
 *    Add 2 to 20's TreeSet.
 *    numToIndices: { 10: [1], 20: [2] }
 *    indexToNum: { 2: 20, 1: 10 }
 * 
 * Pitfalls:
 * - Not removing the old index: If you blindly add the index to the new number's set 
 *   without deleting it from the old number's set, `find(oldNumber)` might return an 
 *   index that no longer holds that number!
 * - Using a standard `PriorityQueue` (Min-Heap) without lazy deletion: If you try to 
 *   use a PriorityQueue and call `pq.remove(index)`, it triggers an O(N) linear search 
 *   across the heap array. TreeSets guarantee O(log N) removal.
 * 
 * Pattern Recognition: "Sparse array updates + Min/Max lookup" -> Map + TreeSet.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you optimize this if we were extremely memory-constrained?
 * A1: TreeSets have high memory overhead per node (left, right, parent, color pointers). 
 *     We could use the Lazy Min-Heap approach. We just push `index` to a Min-Heap. On 
 *     `find`, we peek at the top. If `indexToNum.get(top) != targetNum`, we pop and repeat. 
 *     This saves the node overhead but might temporarily bloat memory with "ghost" nodes 
 *     if an index is overwritten repeatedly.
 * 
 * Q2: What if we wanted to find the LARGEST index instead of the smallest?
 * A2: TreeSets natively support this in O(1) time using `last()`. A PriorityQueue would 
 *     have to be flipped to a Max-Heap.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class NumberContainersStudy {

    static class NumberContainers {
        // Maps an index to the number currently stored there
        private Map<Integer, Integer> indexToNumber;
        
        // Maps a number to a balanced binary search tree of all indices holding it.
        // A TreeSet natively keeps elements sorted, providing O(1) access to the minimum.
        private Map<Integer, TreeSet<Integer>> numberToIndices;

        /**
         * Initializes the number container system.
         */
        public NumberContainers() {
            indexToNumber = new HashMap<>();
            numberToIndices = new HashMap<>();
        }

        /**
         * Fills the container at index with the number. 
         * If there is already a number at that index, replace it.
         * Time Complexity: O(log K) where K is the number of indices for the old/new number.
         */
        public void change(int index, int number) {
            // Check if this index is already occupied by a number
            if (indexToNumber.containsKey(index)) {
                int oldNumber = indexToNumber.get(index);
                
                // Short-circuit optimization: if replacing with the same number, do nothing
                if (oldNumber == number) {
                    return;
                }
                
                // Remove the index from the old number's sorted set
                TreeSet<Integer> oldIndices = numberToIndices.get(oldNumber);
                oldIndices.remove(index);
                
                // Optional cleanup: If the set is empty, we could remove the key from the map 
                // to save memory, though leaving an empty TreeSet is computationally harmless.
                if (oldIndices.isEmpty()) {
                    numberToIndices.remove(oldNumber);
                }
            }
            
            // Update the index to point to the new number
            indexToNumber.put(index, number);
            
            // Add the index to the new number's sorted set, initializing if necessary
            numberToIndices.computeIfAbsent(number, k -> new TreeSet<>()).add(index);
        }

        /**
         * Returns the smallest index for the given number, or -1 if none exists.
         * Time Complexity: O(1) - accessing the first element of a TreeSet.
         */
        public int find(int number) {
            // If the number has never been added, or all its indices were overwritten
            if (!numberToIndices.containsKey(number) || numberToIndices.get(number).isEmpty()) {
                return -1;
            }
            
            // TreeSet keeps indices sorted in ascending order. 
            // first() instantly retrieves the smallest index.
            return numberToIndices.get(number).first();
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        NumberContainers nc = new NumberContainers();

        // find(10) -> -1, no indices hold 10
        System.out.println("Find 10: " + nc.find(10)); // Expected: -1

        // Setup various indices for 10
        nc.change(2, 10);
        nc.change(1, 10);
        nc.change(3, 10);
        nc.change(5, 10);

        // find(10) -> 1
        System.out.println("Find 10 after adds: " + nc.find(10)); // Expected: 1

        // Overwrite the index 1 with 20
        nc.change(1, 20);

        // find(10) -> 2 (because 1 was overwritten)
        System.out.println("Find 10 after overwrite: " + nc.find(10)); // Expected: 2

        // find(20) -> 1
        System.out.println("Find 20: " + nc.find(20)); // Expected: 1
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Map (Index -> Number) + Map (Number -> TreeSet of Indices).
 * Key Observation: Sparse systems (1 to 10^9 domain with few actual updates) require 
 * HashMaps to avoid massive memory allocations. To support fast arbitrary deletions 
 * when an index is overwritten, and fast min-queries, a TreeSet beautifully balances 
 * the demands in strict O(log K) mutation and O(1) query time.
 * What to Memorize: `map.computeIfAbsent(num, k -> new TreeSet<>()).add(index);`
 * Most Common Trap: Using a PriorityQueue (Min-Heap) and calling `pq.remove(index)`. 
 * PriorityQueues are not designed for arbitrary deletion and degrade to O(N) time for it. 
 * If using PQs, you MUST use the lazy-deletion technique (checking validity on pop).
 * Mental Trigger: "Update index + Find smallest" -> "HashMap + TreeSet".
 * ============================================================================
 */























































import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

class NumberContainersTreeSet {

    // index -> current number; this is the authoritative assignment.
    private final Map<Integer, Integer> indexToNumber = new HashMap<>();

    // number -> all indices currently assigned to that number, in sorted order.
    private final Map<Integer, TreeSet<Integer>> numberToIndices = new HashMap<>();

    public void change(int index, int number) {
        Integer oldNumber = indexToNumber.get(index);

        // If the index already had a value, remove it from the old number's set.
        if (oldNumber != null) {
            if (oldNumber == number) {
                // Nothing changed. Avoid unnecessary tree operations.
                return;
            }

            TreeSet<Integer> oldIndices = numberToIndices.get(oldNumber);
            oldIndices.remove(index);

            // Optional cleanup: do not retain empty sets for numbers with no indices.
            if (oldIndices.isEmpty()) {
                numberToIndices.remove(oldNumber);
            }
        }

        // Update the authoritative mapping.
        indexToNumber.put(index, number);

        // Create the sorted set on first use, then insert this index.
        numberToIndices.computeIfAbsent(number, ignored -> new TreeSet<>())
                       .add(index);

        // Time: O(log n) for TreeSet insertion/removal, plus expected O(1)
        // for hash-map operations. Here n is the number of occupied indices.
        // Space: O(n) for the index map and up to O(n) set entries overall.
    }

    public int find(int number) {
        TreeSet<Integer> indices = numberToIndices.get(number);

        // TreeSet.first() returns the minimum without scanning the set.
        // An absent or empty set means no index currently contains this number.
        return indices == null || indices.isEmpty() ? -1 : indices.first();

        // Time: O(1) for first() and expected O(1) for the hash-map lookup.
        // Space: O(1) auxiliary.
    }
}














































import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class NumberContainers {

    /*
     * SOURCE OF TRUTH
     *
     * indexToNumber[index] tells us which number is CURRENTLY assigned
     * to an index.
     *
     * This map is essential for identifying stale heap entries.
     * We never trust an index in a heap without checking this map.
     */
    private final Map<Integer, Integer> indexToNumber = new HashMap<>();

    /*
     * REVERSE LOOKUP
     *
     * numberToIndices[number] is a min-heap containing indices that have
     * been assigned to this number at some point.
     *
     * It can contain stale indices because we intentionally avoid removing
     * arbitrary elements from heaps when change() replaces a value.
     */
    private final Map<Integer, PriorityQueue<Integer>> numberToIndices =
            new HashMap<>();

    public NumberContainers() {
        // Both maps start empty. No array allocation is needed because
        // indices can be as large as 10^9.
    }

    public void change(int index, int number) {
        /*
         * Step 1: Update the source of truth.
         *
         * If this index previously stored a different number, overwriting
         * the map entry immediately makes its old heap entries stale.
         *
         * Example:
         *   Before: indexToNumber = {1 -> 10}
         *   change(1, 20)
         *   After:  indexToNumber = {1 -> 20}
         *
         * The heap for 10 might still contain 1, but the map now tells
         * us that index 1 no longer belongs to number 10.
         */
        indexToNumber.put(index, number);

        /*
         * Step 2: Insert the index into the new number's min-heap.
         *
         * computeIfAbsent creates the heap on the first assignment of
         * a number. The PriorityQueue orders integer indices naturally,
         * so peek() always returns the smallest stored index.
         *
         * If the index was assigned to the same number repeatedly,
         * duplicate heap entries are harmless. find() can discard them
         * once the first valid occurrence is reached and returned.
         */
        numberToIndices
                .computeIfAbsent(number, ignored -> new PriorityQueue<>())
                .offer(index);

        /*
         * Expected time: O(log H) for heap insertion, where H is the
         * size of this number's heap. Hash-map operations are expected O(1).
         *
         * Additional space: O(1) per call, although every call can add
         * another heap entry. Total heap space can grow to O(q), where
         * q is the total number of change() calls.
         */
    }

    public int find(int number) {
        /*
         * Step 1: Find the heap for this number.
         *
         * No heap means no index has ever been assigned to this number.
         */
        PriorityQueue<Integer> indices = numberToIndices.get(number);

        if (indices == null) {
            return -1;
        }

        /*
         * Step 2: Repeatedly discard stale entries from the heap's top.
         *
         * The heap gives us the smallest candidate, but not necessarily
         * the smallest CURRENTLY valid index.
         *
         * An entry is valid exactly when indexToNumber still maps that
         * index to the requested number.
         *
         * Why inspect only the top?
         * If the top is stale, it cannot be a valid answer. Removing it
         * exposes the next-smallest candidate. Once the top is valid,
         * every remaining index is at least as large, so the answer is
         * guaranteed to be the smallest valid index.
         */
        while (!indices.isEmpty()) {
            int smallestIndex = indices.peek();

            Integer currentNumber = indexToNumber.get(smallestIndex);

            if (currentNumber != null && currentNumber == number) {
                // The smallest candidate is still valid.
                return smallestIndex;
            }

            // The index was reassigned, or its entry is an obsolete duplicate.
            // Lazy deletion: remove stale entries only when they reach the top.
            indices.poll();
        }

        /*
         * The heap has been exhausted without finding a valid index.
         * Every candidate was stale, so no index currently contains number.
         */
        return -1;

        /*
         * Time:
         *   - A single find() may remove many stale entries.
         *   - If it removes s entries from a heap of size H, its cost is
         *     O((s + 1) log H) in the worst case.
         *   - However, each heap entry is inserted once and removed at
         *     most once. Across q operations, total heap-cleanup work
         *     is O(q log q).
         *
         * Amortized time per operation: O(log q), including heap cleanup.
         * A particular find() can still take O(q log q) in the worst case.
         *
         * Space: O(n + q), where n is the number of distinct occupied
         * indices and q is the total number of change() calls. Stale entries
         * consume space until they are removed from their respective heaps.
         */
    }
}


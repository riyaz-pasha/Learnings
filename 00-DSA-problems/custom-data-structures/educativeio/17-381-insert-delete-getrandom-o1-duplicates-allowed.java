/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * RandomizedCollection is a multiset (allows duplicates) supporting:
 * - insert(val): Inserts val. Returns true if val was NOT already present.
 * - remove(val): Removes one instance of val. Returns true if it was present.
 * - getRandom(): Returns a random element. Probability is linearly related 
 *   to the number of occurrences of that value.
 * 
 * All methods must run in average O(1) time complexity.
 * Constraints: -2^31 <= val <= 2^31 - 1. Up to 2 * 10^5 calls.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Does `getRandom()` need to be cryptographically secure? 
 *    (Standard pseudo-random is fine, saves performance over SecureRandom).
 * 2. If a number appears 3 times, should `getRandom` return it exactly 3 times as often as a number appearing 1 time?
 *    (Yes, the probability must be strictly proportional to frequency. This means we must store duplicate values individually).
 * 3. Does the order of returned elements matter when we remove a value?
 *    (No, the multiset inherently has no predictable order, giving us freedom to swap elements around internally).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need O(1) indexed random access (which requires an Array) 
 *   AND O(1) existence checks/removals (which requires a Hash Map). 
 *   The binding constraint is deleting from the middle of an array takes O(N) time.
 * 
 * --- Approach 1: ArrayList only (Brute Force) ---
 * 1. Intuition: Just append to an `ArrayList`. 
 * 2. Try: `insert` appends to list. `getRandom` picks a random index. `remove` calls `list.remove(Object)`.
 * 3. Why it works: Naturally handles duplicates and proportional probabilities.
 * 4. Why it's too costly: `list.remove(Object)` takes O(N) time because it linearly searches 
 *    for the object, and then shifts all subsequent elements to the left to close the gap.
 * 5. Bottleneck: O(N) search and O(N) array shifting.
 * 
 * Time Complexity: insert/getRandom: O(1). remove: O(N).
 * Space Complexity: O(N).
 * 
 * --- Approach 2: HashMap to single Index (The "No Duplicates" Solution) ---
 * 1. Intuition: If we know exactly where the element is, we don't have to search for it.
 * 2. Try: `Map<Integer, Integer> map` mapping value -> array index.
 *    To remove in O(1), we take the element at the target index, swap it with the very 
 *    last element in the array, and then delete the last element (Swap-and-Pop).
 * 3. Why it fails here: The problem explicitly allows DUPLICATES. If we insert `5` three times, 
 *    a `Map<Integer, Integer>` will overwrite the index. We lose track of the other two `5`s.
 * 
 * --- Approach 3: HashMap to Set of Indices (Optimal Multiset) ---
 * 1. Intuition: Instead of mapping a value to a single index, map it to a SET of indices.
 * 2. Try: `Map<Integer, LinkedHashSet<Integer>> map` mapping value -> multiple array indices.
 * 3. Why it works: 
 *    - `insert`: Append to `ArrayList` (O(1)), add the new index to the value's Set (O(1)).
 *    - `getRandom`: Randomly pick an index from the `ArrayList` (O(1)). Because duplicates 
 *      are appended as separate elements, their probability is naturally proportional!
 *    - `remove`: Get the Set of indices for the value. Pick one. Swap the `ArrayList` element 
 *      at that index with the last element in the `ArrayList`. Update the Map for the swapped 
 *      element so it points to the new index. Pop the last element. (All O(1)).
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It is the definitive algorithm for this problem and demonstrates mastery 
 *    of composite data structures and the swap-and-pop technique.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Removing the element that is ALREADY at the end of the list: 
 *   If the random index we pick from the Set happens to be `list.size() - 1`, we must not 
 *   execute the full swap logic, or we risk corrupting the Set tracking the last element.
 * - Emptying a Set: 
 *   When a value's Set becomes empty, we must remove the key entirely from the Map so 
 *   `insert` can correctly return `true` (indicating it was completely absent) next time.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Swap-and-Pop." 
 *   To delete from an array in O(1) time without leaving holes, overwrite the target 
 *   element with the element at the very end of the array, then shrink the array by 1.
 * 
 * ASCII DRY RUN (Remove Operation)
 * List = [10, 20, 10, 30]
 * Map  = { 10:[0, 2], 20:[1], 30:[3] }
 * 
 * Action: remove(10)
 * Step 1: Get an index for 10. Let's pick 0. TargetIdx = 0.
 * Step 2: Identify the last element. LastIdx = 3, LastVal = 30.
 * Step 3: Overwrite TargetIdx with LastVal. 
 *         List becomes [30, 20, 10, 30]
 * Step 4: Update the map for LastVal (30). 
 *         Remove its old index (3), add its new index (0). 
 *         Map = { 10:[0, 2], 20:[1], 30:[0] }
 * Step 5: Remove the used index (0) from 10's set.
 *         Map = { 10:[2], 20:[1], 30:[0] }
 * Step 6: Pop the last element from the list.
 *         List becomes [30, 20, 10]
 * 
 * Pitfalls:
 * - Using `HashSet` instead of `LinkedHashSet`. A standard `HashSet` iterator can take O(Capacity) 
 *   to find the first element if the set had many elements that were removed. `LinkedHashSet` 
 *   guarantees strict O(1) retrieval for `iterator().next()`.
 * - Updating the map in the wrong order. You must remove the target index from the `val`'s set 
 *   AFTER fetching it, and update the `lastVal`'s set carefully. If `val == lastVal`, messing 
 *   up the order of `set.remove()` and `set.add()` will corrupt the indices.
 * 
 * Pattern Recognition: "O(1) Indexed Random Access + Deletes" -> Array + Hash Map + Swap-and-Pop.
 * 
 * Interview Script snippet:
 * "To support O(1) random selection with duplicates, I must use an ArrayList. But removing 
 * from the middle of an ArrayList is O(N). I can bypass this using the 'swap and pop' technique: 
 * overwrite the element to be deleted with the tail element, then remove the tail. To know 
 * exactly where elements are without searching, I'll use a HashMap. Since there are duplicates, 
 * the Map will point values to a Set of their current array indices."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if `getRandom` did NOT need to be proportional to frequency? (e.g., unique values only)
 * A1: The multiset collapses to a standard set. We could just use `Map<Integer, Integer>` 
 *     instead of mapping to a `LinkedHashSet`. (This is exactly LeetCode 380: Insert Delete GetRandom O(1)).
 * 
 * Q2: How does memory scale if we have millions of identical duplicates? e.g., insert(5) 10 million times.
 * A2: Our approach creates a `LinkedHashSet` containing 10 million integers. Java `Integer` 
 *     autoboxing and Set Node overhead will consume huge amounts of memory (~300MB just for one number). 
 *     If duplicates heavily outnumber unique elements, we should use a Frequency Map `(val -> count)` 
 *     and a custom balanced tree (like a Fenwick tree) to weight random choices, trading strict O(1) 
 *     time for O(log U) time to vastly save memory.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RandomizedCollectionStudy {

    static class RandomizedCollection {
        // Maps a value to a Set of indices where it currently resides in the list.
        // LinkedHashSet is used for strict O(1) iterator().next() performance.
        private Map<Integer, Set<Integer>> valToIndices;
        
        // Stores all inserted elements. 
        // Duplicates are appended multiple times, natively solving the probability requirement.
        private List<Integer> elements;
        
        private Random random;

        /** 
         * Initializes the empty RandomizedCollection object.
         */
        public RandomizedCollection() {
            valToIndices = new HashMap<>();
            elements = new ArrayList<>();
            random = new Random();
        }

        /** 
         * Inserts an item val into the multiset. 
         * Returns true if the item is not present, false otherwise.
         * Time: O(1) average.
         */
        public boolean insert(int val) {
            // Check if the element is new
            boolean isNewElement = !valToIndices.containsKey(val);
            
            // Ensure a Set exists for this value
            valToIndices.computeIfAbsent(val, k -> new LinkedHashSet<>());
            
            // The new index will be the current size of the list
            int newIndex = elements.size();
            
            // Add the index to the Map and append the value to the List
            valToIndices.get(val).add(newIndex);
            elements.add(val);
            
            return isNewElement;
        }

        /** 
         * Removes an item val from the multiset if present. 
         * Returns true if the item is present, false otherwise.
         * Time: O(1) average.
         */
        public boolean remove(int val) {
            // O(1) existence check
            if (!valToIndices.containsKey(val)) {
                return false;
            }

            // Retrieve the set of indices where this value lives
            Set<Integer> targetIndices = valToIndices.get(val);
            
            // 1. Pick any index to remove. 
            // LinkedHashSet guarantees O(1) retrieval here.
            int indexToRemove = targetIndices.iterator().next();
            
            // Remove the index from the Set
            targetIndices.remove(indexToRemove);
            
            // If the Set is now empty, clean up the Map
            if (targetIndices.isEmpty()) {
                valToIndices.remove(val);
            }

            // 2. Identify the last element in the List
            int lastIndex = elements.size() - 1;
            int lastVal = elements.get(lastIndex);

            // 3. Swap and Pop (only if the element to remove isn't already the last element)
            if (indexToRemove != lastIndex) {
                // Overwrite the removed element's slot with the last element's value
                elements.set(indexToRemove, lastVal);
                
                // The last element has now moved. We must update its tracking Set.
                Set<Integer> lastValIndices = valToIndices.get(lastVal);
                
                // Remove its old index (lastIndex) and add its new index (indexToRemove)
                lastValIndices.remove(lastIndex);
                lastValIndices.add(indexToRemove);
            }
            
            // Physically shrink the array by removing the tail element
            elements.remove(lastIndex);

            return true;
        }

        /** 
         * Returns a random element from the current multiset of elements.
         * Probability is linearly related to the number of same values.
         * Time: O(1)
         */
        public int getRandom() {
            // random.nextInt(N) is uniformly distributed from 0 to N-1.
            // Since duplicates exist multiple times in the list, their 
            // chance of being picked scales perfectly with their frequency.
            int randomIndex = random.nextInt(elements.size());
            return elements.get(randomIndex);
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        RandomizedCollection collection = new RandomizedCollection();

        // 1. Basic inserts with duplicate tracking
        System.out.println("Insert 1: " + collection.insert(1)); // Expected: true
        System.out.println("Insert 1: " + collection.insert(1)); // Expected: false (already present)
        System.out.println("Insert 2: " + collection.insert(2)); // Expected: true
        
        // Collection is now physically [1, 1, 2]
        
        // 2. Get Random (Probability: 1 is 66%, 2 is 33%)
        System.out.println("Random element: " + collection.getRandom()); // 1 or 2
        
        // 3. Removal
        System.out.println("Remove 1: " + collection.remove(1)); // Expected: true
        
        // Collection is now logically [2, 1] or [1, 2]
        // The swap-and-pop logic ensures no gaps are left.
        
        // 4. Verify state
        System.out.println("Remove 1 again: " + collection.remove(1)); // Expected: true
        System.out.println("Remove 1 again: " + collection.remove(1)); // Expected: false (none left)
        
        System.out.println("Random element (only 2 left): " + collection.getRandom()); // Expected: 2
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: List + Map of Indices + Swap-and-Pop.
 * Key Observation: O(1) random access demands an array. Removing from an array in O(1) 
 * demands swapping the victim with the tail and popping. To support duplicates, the map 
 * must track a Set of indices for every value.
 * What to Memorize: `if (indexToRemove != lastIndex)` check. If you blindly swap 
 * when `indexToRemove == lastIndex`, you will corrupt the Set tracking the last element 
 * because you delete `lastIndex` and immediately re-add `indexToRemove` (which are the same).
 * Most Common Trap: Using `HashSet`. After many deletes, `iterator().next()` can degrade to 
 * O(Capacity) as it steps over tombstone buckets. `LinkedHashSet` prevents this.
 * Mental Trigger: "O(1) Array Removal" -> "Swap and Pop".
 * ============================================================================
 */










































import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

class RandomizedCollection {

    /*
     * CORE IDEA
     * ---------
     * values stores EVERY occurrence, including duplicates.
     *
     * Example:
     * values = [10, 20, 10]
     *
     * Each array position is one occurrence, so selecting a random
     * index gives each occurrence equal probability.
     *
     * ArrayList:
     * - get(index): O(1)
     * - add(value): amortized O(1)
     * - remove(lastIndex): O(1)
     *
     * Removing from the middle would cost O(n) because elements shift.
     * We avoid that by replacing the removed position with the last value.
     */
    private final List<Integer> values;

    /*
     * Maps each distinct value to ALL its positions in values.
     *
     * Example:
     * values = [10, 20, 10, 30, 10]
     *
     * valueToIndices = {
     *     10 -> {0, 2, 4},
     *     20 -> {1},
     *     30 -> {3}
     * }
     *
     * A HashSet is used because:
     * - We need to find an occurrence's index quickly.
     * - We need to add and remove indices quickly.
     * - We do not need the indices to be ordered.
     *
     * HashMap/HashSet operations are average O(1).
     */
    private final Map<Integer, Set<Integer>> valueToIndices;

    /*
     * A single Random object can be reused across calls.
     *
     * Random.nextInt(bound) returns a uniformly random index in
     * [0, bound), so every array occurrence has equal probability.
     */
    private final Random random;

    public RandomizedCollection() {
        values = new ArrayList<>();
        valueToIndices = new HashMap<>();
        random = new Random();

        /*
         * Constructor:
         * Time: O(1) expected, ignoring implementation-specific allocation.
         * Space: O(1) initially, excluding the empty structures' overhead.
         */
    }

    public boolean insert(int val) {

        /*
         * If the value has no set, it has never appeared in the collection.
         * This determines the return value BEFORE inserting the new occurrence.
         *
         * true  -> val was absent before this call.
         * false -> val was already present, but we still insert another copy.
         *
         * containsKey is average O(1).
         */
        boolean wasAbsent = !valueToIndices.containsKey(val);

        /*
         * The new occurrence will occupy the next available array index.
         *
         * IMPORTANT:
         * Store the index before appending so it refers to the correct position.
         */
        int newIndex = values.size();

        /*
         * Append the occurrence.
         * ArrayList.add is amortized O(1); occasional resizing can take O(n),
         * but the amortized cost over many insertions remains O(1) per insertion.
         */
        values.add(val);

        /*
         * Obtain the set for val, creating it if this is its first occurrence.
         *
         * Add the new index to the set. Since this is a new array position,
         * the index is not already present in val's set.
         *
         * Average time for map lookup/creation and set insertion: O(1).
         */
        valueToIndices
                .computeIfAbsent(val, ignored -> new HashSet<>())
                .add(newIndex);

        /*
         * Average time: O(1).
         * Extra space: O(1) per inserted occurrence, amortized, across
         * the array and index set.
         */
        return wasAbsent;
    }

    public boolean remove(int val) {

        /*
         * If val has no index set, no occurrence exists.
         * Nothing should change.
         *
         * Average lookup time: O(1).
         */
        Set<Integer> indices = valueToIndices.get(val);

        if (indices == null) {
            return false;
        }

        /*
         * Choose any occurrence of val.
         *
         * HashSet has no guaranteed ordering, but ordering does not matter:
         * the problem asks to remove ONE occurrence, not a particular one.
         *
         * Getting an iterator and its first element is O(1) on average.
         */
        int removeIndex = indices.iterator().next();

        /*
         * Identify the final array position and the value currently stored there.
         *
         * These values must be saved BEFORE modifying the array.
         */
        int lastIndex = values.size() - 1;
        int lastValue = values.get(lastIndex);

        /*
         * FIRST remove the index of the occurrence we are deleting.
         *
         * This ordering is important when val == lastValue.
         * We must remove the target occurrence's index before moving the last
         * occurrence, otherwise we can accidentally lose the surviving copy's
         * index or leave a stale index behind.
         */
        indices.remove(removeIndex);

        /*
         * If removeIndex == lastIndex, the target is already at the end.
         * No replacement is needed.
         *
         * Otherwise, move the last occurrence into the removed position.
         */
        if (removeIndex != lastIndex) {

            /*
             * The last occurrence of lastValue is about to leave lastIndex.
             * Remove that old index from lastValue's set.
             *
             * This also works if val == lastValue:
             * the target index was removed above, and this removes the
             * distinct lastIndex belonging to the moved occurrence.
             */
            Set<Integer> lastValueIndices = valueToIndices.get(lastValue);
            lastValueIndices.remove(lastIndex);

            /*
             * Overwrite the target position with the last value.
             *
             * ArrayList.set is O(1). No shifting occurs.
             */
            values.set(removeIndex, lastValue);

            /*
             * The moved occurrence now lives at removeIndex.
             * Update the mapping to reflect its new position.
             *
             * This is essential: every index stored in the map must point
             * to the current position of the corresponding value.
             */
            lastValueIndices.add(removeIndex);
        }

        /*
         * Remove the final array entry.
         *
         * We already moved its value if a move was necessary, so this
         * operation cannot disturb any other array position.
         *
         * ArrayList.remove(lastIndex) is O(1).
         */
        values.remove(lastIndex);

        /*
         * If val has no remaining occurrences, remove its map entry.
         * Otherwise, retain the set for future insertions and removals.
         *
         * This also handles the val == lastValue case correctly:
         * the surviving occurrence, if any, has already been reindexed.
         */
        if (indices.isEmpty()) {
            valueToIndices.remove(val);
        }

        /*
         * Average time: O(1).
         * We perform only a constant number of average-O(1) map/set operations
         * and O(1) array operations.
         */
        return true;
    }

    public int getRandom() {

        /*
         * The problem guarantees that values is nonempty for this call.
         *
         * Pick an index uniformly from [0, values.size()).
         * Do not pick a random map key: that would give distinct values
         * equal probabilities rather than weighting them by occurrence count.
         *
         * Time: O(1).
         * Extra space: O(1).
         */
        int randomIndex = random.nextInt(values.size());

        return values.get(randomIndex);
    }

    /*
     * COMPLEXITY SUMMARY
     * ------------------
     *
     * Let n = the total number of occurrences currently in the collection.
     * Let d = the number of distinct values.
     *
     * insert(val):
     *   Average O(1) time; ArrayList append is amortized O(1).
     *   O(1) additional space per occurrence, amortized.
     *
     * remove(val):
     *   Average O(1) time.
     *   Only one array position is overwritten and the final entry is removed.
     *
     * getRandom():
     *   O(1) time and O(1) extra space.
     *
     * Overall space:
     *   O(n + d) = O(n) for a nonempty collection, because the array stores
     *   n occurrences and the index sets collectively store n indices.
     *   The map has d keys.
     *
     * Hash-based operations have expected/average O(1) complexity,
     * not a worst-case guarantee for every possible hash-table operation.
     */
}



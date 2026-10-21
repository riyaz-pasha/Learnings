/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Implement a Random Set data structure that can perform:
 * - Constructor(): Initializes the Random Set object.
 * - Insert(data): Adds data if it doesn't exist (returns true), else false.
 * - Delete(data): Removes data if it exists (returns true), else false.
 * - GetRandom(): Returns a random element from the set.
 * 
 * Target: O(1) average time for EACH operation.
 * Constraints: Data fits in 32-bit int, up to 2 * 10^5 calls. GetRandom() 
 * is always called on a non-empty set.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Does the set need to maintain the insertion order of elements? 
 *    (Crucial: If order doesn't matter, we can use the "Swap and Pop" trick to delete in O(1). If it matters, O(1) is impossible).
 * 2. Does `GetRandom()` need to be cryptographically secure, or is pseudo-random enough?
 *    (Determines whether we use `java.security.SecureRandom` or the standard `java.util.Random` for performance).
 * 3. Are the probabilities for `GetRandom()` required to be strictly uniform?
 *    (Yes, every element currently in the set must have an equal 1/N chance of being returned).
 * 4. Are we expecting an extremely high churn rate (billions of inserts/deletes)?
 *    (Helps decide if the backing array needs custom shrinking logic to prevent memory bloat over a long uptime).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement: We need three conflicting capabilities: 
 *   1. O(1) existence checks (Hash Set)
 *   2. O(1) indexed random access (Array)
 *   3. O(1) removal without leaving "holes" in our indices.
 * 
 * --- Approach 1: The Standard HashSet ---
 * 1. Intuition: A `HashSet` is literally designed for O(1) inserts and deletes.
 * 2. Try: `HashSet<Integer> set = new HashSet<>()`. 
 *    `Insert`: `set.add(data)`. 
 *    `Delete`: `set.remove(data)`.
 *    `GetRandom`: Convert the set to an array `set.toArray()`, then pick a random index.
 * 3. Why it works: Set logic is naturally handled.
 * 4. Why it's too costly: Sets have no concept of an "index." You can't ask a HashSet for 
 *    its "5th element". Converting it to an array or iterating through an iterator takes O(N) time.
 * 5. Bottleneck: `GetRandom` takes O(N) time.
 * 
 * Time Complexity: Insert: O(1), Delete: O(1), GetRandom: O(N) — converting set to array.
 * Space Complexity: O(N) — storing N elements in the Set.
 * 
 * --- Approach 2: The Standard ArrayList ---
 * 1. Intuition: To get a random element in O(1), we must use an array/list. 
 *    `GetRandom` becomes `list.get(random.nextInt(list.size()))`.
 * 2. Try: `ArrayList<Integer> list = new ArrayList<>()`.
 * 3. Why it works: Solves the `GetRandom` O(1) requirement perfectly.
 * 4. Why it's too costly: 
 *    - To `Insert(data)`, we must first check if it exists: `list.contains(data)` takes O(N).
 *    - To `Delete(data)`, `list.remove(Object)` searches for the element O(N) AND shifts 
 *      all subsequent elements down to fill the gap O(N).
 * 5. Bottleneck: `Insert` and `Delete` take O(N).
 * 
 * Time Complexity: Insert: O(N), Delete: O(N), GetRandom: O(1).
 * Space Complexity: O(N) — storing N elements in the List.
 * 
 * --- Approach 3: HashMap + ArrayList + "Swap and Pop" (Optimal) ---
 * 1. Intuition: We need the fast lookup of a Hash structure AND the fast indexing of an Array.
 *    Let's use both. The Map tracks `Value -> Index in Array`. The List tracks the `Values`.
 * 2. Try: 
 *    - `Insert`: Map checks existence in O(1). If new, append to List (O(1)) and record its index in Map.
 *    - `GetRandom`: Pick random index from List (O(1)).
 *    - `Delete`: The hard part. Removing from the middle of a List is O(N). 
 * 3. The Breakthrough: Order doesn't matter! To remove an element from the middle of an array 
 *    in O(1) time, simply OVERWRITE it with the VERY LAST element in the array, then delete the last element.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It is the definitive algorithm for this specific problem.
 * 
 * Time Complexity: O(1) average for all operations — HashMap lookups and Array tail-mutations are O(1).
 * Space Complexity: O(N) — Storing N elements in the List and N key-value pairs in the Map.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Deleting the very last element: If the element to delete is ALREADY the last element, 
 *   swapping it with itself is harmless, but you must ensure you don't overwrite its map 
 *   entry with a stale index before deleting it. Order of map operations matters.
 * - Empty Set: `GetRandom()` on an empty set would throw `IllegalArgumentException` from `random.nextInt(0)`. 
 *   (Constraints guarantee it won't happen, but good to know).
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & PITFALLS
 * ============================================================================
 * > Key Insight: "Swap-and-Pop." 
 *   If you need to delete from an array in O(1) time and order does not matter, take the 
 *   element at the end of the array, paste it over the element you want to delete, and 
 *   shrink the array by 1. 
 * 
 * ASCII DRY RUN (Delete Operation)
 * Initial State: 
 * List = [10, 20, 30, 40]
 * Map  = {10:0, 20:1, 30:2, 40:3}
 * 
 * Action: Delete(20). (Index of 20 is 1).
 * 
 * Step 1: Find the last element (40).
 * Step 2: Overwrite index 1 with 40.
 * List = [10, 40, 30, 40]
 * Map  = {10:0, 20:1, 30:2, 40:1}  <-- Note: 40's index is updated to 1
 * 
 * Step 3: Remove the last element from the List, and remove 20 from the Map.
 * List = [10, 40, 30]
 * Map  = {10:0, 30:2, 40:1} 
 * 
 * Pitfalls:
 * - Map update order during Delete: Update the Map entry for the `lastElement` BEFORE 
 *   you remove the `data` from the Map. Why? If the element to delete IS the last element, 
 *   removing it first and then updating it will accidentally re-insert it into the Map!
 * 
 * Pattern Recognition: "O(1) Array Removal without Order" -> Swap-and-Pop.
 * 
 * Interview Script snippet:
 * "To achieve O(1) random access, I absolutely must use an array backing. But arrays have 
 * O(N) deletes because of shifting. I can bypass the shifting by taking the last element 
 * of the array, swapping it into the slot of the deleted element, and popping the tail. 
 * To know exactly where an element is located without an O(N) search, I'll use a HashMap 
 * that stores the element's current index. When I swap, I'll update the moved element's 
 * index in the map."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if duplicate elements WERE allowed? (LeetCode 381: RandomizedCollection)
 * A1: The HashMap would change from `Map<Integer, Integer>` to `Map<Integer, Set<Integer>>`. 
 *     Instead of storing a single index, it stores a Set of all indices where that number 
 *     appears in the List. When deleting, we take any index from that Set, perform the 
 *     Swap-and-Pop, and update the Sets accordingly.
 * 
 * Q2: What if we do a billion inserts and deletes, and end up with only 10 items left?
 * A2: Our `ArrayList` capacity might remain bloated at 1,000,000, causing a memory leak. 
 *     To fix this, we'd add custom shrinking logic: if `list.size() < list.capacity() / 4`, 
 *     we allocate a new smaller array and copy the elements over (similar to HashMap resizing).
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomSetStudy {

    static class RandomSet {
        // The list holds the actual values to allow O(1) random access via index.
        private final List<Integer> values;
        
        // The map holds: Key = The Value, Value = The Index of that value in the list.
        private final Map<Integer, Integer> valueToIndex;
        
        // A single random instance reused for efficiency.
        private final Random random;

        /** 
         * Constructor: Initializes the Random Set object. 
         */
        public RandomSet() {
            values = new ArrayList<>();
            valueToIndex = new HashMap<>();
            random = new Random();
        }

        /**
         * Inserts data. Returns TRUE if successfully added, FALSE if already existed.
         * Time: O(1) amortized
         */
        public boolean insert(int data) {
            // O(1) check
            if (valueToIndex.containsKey(data)) {
                return false; 
            }

            // Append to the end of the array
            values.add(data);
            
            // The index of the newly added element is simply the last index (size - 1).
            valueToIndex.put(data, values.size() - 1);
            
            return true;
        }

        /**
         * Deletes data. Returns TRUE if successfully deleted, FALSE if it didn't exist.
         * Time: O(1)
         */
        public boolean delete(int data) {
            // O(1) check
            if (!valueToIndex.containsKey(data)) {
                return false;
            }

            // 1. Locate the target element and the last element
            int targetIndex = valueToIndex.get(data);
            int lastIndex = values.size() - 1;
            int lastElement = values.get(lastIndex);

            // 2. Perform the "Swap" 
            // We move the lastElement into the spot of the element we want to delete.
            values.set(targetIndex, lastElement);
            
            // 3. Update the map to reflect the new location of the lastElement
            // VERY IMPORTANT: Do this BEFORE removing `data` from the map.
            // If `data` is exactly the `lastElement`, we want the final state to have it removed.
            valueToIndex.put(lastElement, targetIndex);

            // 4. "Pop" the last element off the list and remove the original data from the map
            values.remove(lastIndex);
            valueToIndex.remove(data);

            return true;
        }

        /**
         * Returns a random element from the set in O(1) time.
         */
        public int getRandom() {
            // random.nextInt(N) generates a number from 0 (inclusive) to N (exclusive).
            // This guarantees a uniform distribution over the list.
            int randomIndex = random.nextInt(values.size());
            return values.get(randomIndex);
        }
    }

    // ============================================================================
    // MAIN METHOD: Testing Approach + Edge Cases
    // ============================================================================
    public static void main(String[] args) {
        RandomSet randomSet = new RandomSet();

        // 1. Basic inserts
        System.out.println("Insert 10: " + randomSet.insert(10)); // true
        System.out.println("Insert 20: " + randomSet.insert(20)); // true
        System.out.println("Insert 30: " + randomSet.insert(30)); // true
        
        // 2. Duplicate insert check
        System.out.println("Insert 20 again: " + randomSet.insert(20)); // false
        
        // 3. Get Random verification
        System.out.println("Random element: " + randomSet.getRandom()); // Will be 10, 20, or 30
        
        // 4. Delete middle element (triggers the Swap and Pop)
        // Array: [10, 20, 30] -> Delete(20) -> Swap 20 and 30 -> [10, 30, 20] -> Pop -> [10, 30]
        System.out.println("Delete 20: " + randomSet.delete(20)); // true
        
        // 5. Verify deletion
        System.out.println("Delete 20 again: " + randomSet.delete(20)); // false
        
        // 6. EDGE CASE: Delete the very last element
        // Array currently [10, 30]. Delete 30.
        System.out.println("Delete 30 (last element): " + randomSet.delete(30)); // true
        
        // To prove the state is clean, getRandom should now exclusively return 10
        System.out.println("Random element (only 10 left): " + randomSet.getRandom()); // 10
    }
}

/**
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core Pattern: List + Map with "Swap-and-Pop".
 * Key Observation: You can't achieve O(1) random access without an Array. You can't 
 * delete from an Array in O(1) unless you abandon ordering and swap from the tail.
 * What to Memorize: `values.set(indexToDelete, lastElement); map.put(lastElement, indexToDelete); list.remove(lastIndex); map.remove(valToDelete);`
 * Most Common Trap: Calling `map.remove(val)` BEFORE `map.put(lastElement, index)`. 
 * If the element being deleted happens to be the last element, you will accidentally 
 * put it right back into the map!
 * Mental Trigger: "O(1) Random + Delete" -> "Map to Index + Swap-and-Pop".
 * ============================================================================
 */










import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RandomizedSet {

    // Stores values contiguously, allowing random access by index in O(1).
    private final ArrayList<Integer> values;

    // Maps each stored value to its exact position in values.
    // This lets us find an element's position in O(1) average time.
    private final Map<Integer, Integer> indices;

    public RandomizedSet() {
        // Start with an empty array and an empty map.
        // TIME: O(1) initialization, ignoring internal allocation details.
        // SPACE: O(1) initial logical state, excluding collection overhead.
        values = new ArrayList<>();
        indices = new HashMap<>();
    }

    public boolean insert(int data) {
        // STEP 1: Check whether the value already exists.
        // WHY: The structure is a set, so duplicate values are forbidden.
        // TIME: O(1) average HashMap lookup.
        // SPACE: O(1) auxiliary space.
        if (indices.containsKey(data)) {
            return false;
        }

        // STEP 2: Append the new value to the end of the array.
        // WHY: Appending avoids shifting existing elements.
        // TIME: O(1) amortized; an occasional resize can take O(n).
        values.add(data);

        // STEP 3: Record the index where we appended the value.
        // WHY: Future deletions must locate this value without scanning.
        // TIME: O(1) average HashMap insertion.
        // SPACE: O(1) additional entry, amortized.
        indices.put(data, values.size() - 1);

        // OVERALL TIME: O(1) average/amortized.
        // OVERALL EXTRA SPACE: O(1) per newly inserted distinct value.
        return true;
    }

    public boolean delete(int data) {
        // STEP 1: Find the element's index using the map.
        // WHY: Searching the array would cost O(n).
        // TIME: O(1) average.
        // SPACE: O(1) auxiliary space.
        Integer index = indices.get(data);

        // A null result means the value does not exist.
        // All valid indices are non-negative, so null is unambiguous.
        if (index == null) {
            return false;
        }

        // STEP 2: Identify the last element in the array.
        // WHY: We can remove the last position without shifting elements.
        // TIME: O(1).
        int lastIndex = values.size() - 1;
        int lastValue = values.get(lastIndex);

        // STEP 3: Fill the deleted position with the last element,
        // but only if the deleted element is not already the last one.
        // WHY: ArrayList.remove(index) could shift O(n) elements.
        // Replacing the element lets us remove the last position instead.
        if (index != lastIndex) {
            values.set(index, lastValue);

            // STEP 4: Update the moved value's map entry.
            // WHY: lastValue now lives at 'index', not 'lastIndex'.
            // Without this update, the map invariant would be broken.
            // TIME: O(1) average.
            indices.put(lastValue, index);
        }

        // STEP 5: Remove the old last position.
        // WHY: It is now either the original deleted value or a duplicate
        // of the moved value that we copied into the deleted position.
        // TIME: O(1), because this removes the final ArrayList element.
        values.remove(lastIndex);

        // STEP 6: Remove the deleted value from the map.
        // WHY: It must no longer be recognized as a member of the set.
        // TIME: O(1) average.
        indices.remove(data);

        // OVERALL TIME: O(1) average.
        // OVERALL EXTRA SPACE: O(1) auxiliary space for this operation.
        return true;
    }

    public int getRandom() {
        // PRECONDITION: At least one element exists.
        // The problem guarantees this whenever getRandom() is called.

        // STEP 1: Choose a uniformly random valid array index.
        // WHY: Every stored element occupies exactly one array position.
        // nextInt(bound) returns an integer in [0, bound).
        // TIME: O(1).
        // SPACE: O(1) auxiliary space.
        int randomIndex =
                ThreadLocalRandom.current().nextInt(values.size());

        // STEP 2: Return the element at that position.
        // WHY: ArrayList supports direct indexed access.
        // TIME: O(1).
        // OVERALL TIME: O(1); OVERALL EXTRA SPACE: O(1).
        return values.get(randomIndex);
    }
}


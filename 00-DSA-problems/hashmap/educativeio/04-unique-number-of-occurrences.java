/**
 * ============================================================================
 * U N I Q U E   N U M B E R   O F   O C C U R R E N C E S
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "What is the possible range of the array elements?"
 *    (Crucial because if the range is small, like -1000 to 1000, we can use an array instead of a HashMap to save memory and hashing overhead).
 * 2. "What is the maximum length of the array?"
 *    (Tells us the maximum possible frequency of any single element, bounding the size of our frequency-checking structures).
 * 3. "Is it guaranteed the input array will have at least one element?"
 *    (Prevents edge cases around empty arrays; constraints say length >= 1).
 * 4. "Are we optimizing for strict memory limits or execution speed?"
 *    (Determines whether we sort in-place to minimize space, or use hash structures/arrays for O(N) time).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Requirement: We need to map every distinct number to its frequency, and then ensure 
 * > no two distinct numbers share the same frequency. The bottleneck is how we group identical 
 * > numbers and how we check their counts for duplicates.
 *
 * ### Approach 1: Sort and Count (The No-Extra-Space Brute Force)
 * 1. What I'd naturally try: I don't want to use extra memory, so I'll put all identical numbers 
 *    next to each other by sorting the input array. Then, I'll count them up as I walk through it, 
 *    storing those counts in a secondary list. Finally, I'll sort that secondary list to check if 
 *    any adjacent counts are identical.
 * 2. Why it works: Sorting inherently groups duplicates. Counting contiguous blocks gives exact 
 *    frequencies. Sorting the frequencies easily reveals duplicate counts.
 * 3. Why it's too slow/costly: 
 *    - Time Complexity: O(N log N) — because sorting the main array dominates the runtime, and sorting 
 *      the frequency array takes at most O(N log N) as well.
 *    - Space Complexity: O(log N) to O(N) — because primitive array sorting in Java (Dual-Pivot Quicksort) 
 *      takes O(log N) stack space, plus we allocate a list for frequencies.
 * 4. What work is being repeated: We are comparing elements against each other multiple times just to 
 *    group them. We don't actually care about the *order* of elements, only their counts.
 * 5. What property removes the bottleneck: If we trade space for time, we can group and count in a 
 *    single pass using a Hash Map, bypassing sorting entirely.
 *
 * ### Approach 2: HashMap and HashSet (The Standard Developer Way)
 * 1. What I'd naturally try: Walk through the array once and build a frequency map 
 *    (`HashMap<Integer, Integer>`). Then, extract all the values (the frequencies) and toss them 
 *    into a `HashSet`. If the HashSet is smaller than the HashMap, there were duplicate frequencies.
 * 2. Why it works: HashMaps provide O(1) average lookup/update for counting. HashSets inherently 
 *    reject duplicate values, acting as a perfect filter for duplicate frequencies.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we visit each element exactly once to build the map, and then 
 *      we iterate over the map's values (at most N elements) to populate the set.
 *    - Space Complexity: O(N) — because in the worst case (all elements unique), we store N key-value 
 *      pairs in the HashMap, and N integers in the HashSet.
 * 4. What work is being repeated: We are calculating hash codes, resolving collisions, and allocating 
 *    Node objects in the heap for Integer wrappers. This creates hidden overhead.
 * 5. What property removes the bottleneck: The constraints explicitly bound the values! `nums[i]` is 
 *    between -1000 and 1000 (a range of 2001). `nums.length` is at most 1000 (max frequency is 1000). 
 *    We can use raw memory arrays instead of heavy Object-based Hash structures.
 *
 * ### Approach 3: Bounded Arrays (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Use an `int[2001]` to tally occurrences, mapping `nums[i]` to index 
 *    `nums[i] + 1000`. Then use a `boolean[1001]` to track which frequencies we've seen.
 * 2. Why it works: Array indices serve as perfect, collision-free hash keys. It's exactly Approach 2, 
 *    but executed on bare metal.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we iterate the array once to count, and then do a fixed-size 
 *      loop (size 2001) to check frequencies.
 *    - Space Complexity: O(1) — because we allocate exactly one array of size 2001 and one of size 
 *      1001, regardless of whether N is 10 or 1000. It requires a fixed 12 kilobytes of memory.
 * 
 * > **Interview Strategy:** In a real interview, I would briefly mention Approach 2 as the generic 
 * > solution, but immediately point out the small constraints and write Approach 3. It shows strong 
 * > awareness of constraints and mechanical sympathy (understanding heap vs. stack overhead).
 *
 * 
 * ## 3. Edge cases
 * - Array with a single element: e.g., `[5]`. Frequency is 1. Always returns true.
 * - Array with all identical elements: e.g., `[-1, -1, -1]`. Frequency is 3. Always returns true.
 * - Array with exactly mirroring frequencies: e.g., `[1, 2]`. Frequencies are 1, 1. Returns false.
 * - Negative numbers: Ensures our `+ 1000` offset correctly avoids `ArrayIndexOutOfBoundsException`.
 *
 * 
 * ## 4. Key Insight & Dry Run
 * **Key Insight:** "Array values as Indices." When the domain of possible inputs is small and 
 * continuous, a direct addressing array is infinitely faster and lighter than a HashMap.
 *
 * **ASCII Diagram (Approach 3 mapping):**
 * Input: `[0, 0, -1]`
 * Index mapping: `value + 1000`
 * countArr[999]  (for -1) = 1
 * countArr[1000] (for 0)  = 2
 * 
 * **Dry Run of Approach 3:**
 * Input: `[1, 2, 2, 1, 1, 3]`
 * 
 * 1. Build counts (offset +1000):
 *    - nums[0]=1 -> countArr[1001] = 1
 *    - nums[1]=2 -> countArr[1002] = 1
 *    - nums[2]=2 -> countArr[1002] = 2
 *    - nums[3]=1 -> countArr[1001] = 2
 *    - nums[4]=1 -> countArr[1001] = 3
 *    - nums[5]=3 -> countArr[1003] = 1
 *    Result: countArr[1001]=3, countArr[1002]=2, countArr[1003]=1. All others are 0.
 * 
 * 2. Check frequencies using `seenFreq` (size 1001):
 *    - i=0 to 1000: count=0. Skip (we ignore 0 counts).
 *    - i=1001: count=3. seenFreq[3] is false. Set seenFreq[3] = true.
 *    - i=1002: count=2. seenFreq[2] is false. Set seenFreq[2] = true.
 *    - i=1003: count=1. seenFreq[1] is false. Set seenFreq[1] = true.
 *    Loop finishes without collisions. Return true.
 *
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the constraints were unbounded (`-2^31 <= nums[i] <= 2^31 - 1`)?
 *   **A:** The bounded array approach would cause an OutOfMemoryError. We must fall back to 
 *   Approach 2 (HashMap + HashSet).
 * - **Q:** What if you had to do this in O(1) extra space on unbounded data?
 *   **A:** We would use Approach 1 (Sort and Count). By sorting the array in place (ignoring 
 *   the system stack for quicksort), we can process contiguous chunks. We'd have to verify 
 *   frequency uniqueness without a HashSet, perhaps by re-sorting the array of gathered frequencies.
 *
 * ============================================================================
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UniqueOccurrencesStudy {

    /**
     * Interface to test different implementations uniformly.
     */
    interface UniqueOccurrencesSolver {
        boolean uniqueOccurrences(int[] nums);
    }

    // ========================================================================
    // APPROACH 3: BOUNDED ARRAYS (The Optimal Interview Solution)
    // ========================================================================
    static class ArrayOptimizedSolver implements UniqueOccurrencesSolver {
        @Override
        public boolean uniqueOccurrences(int[] nums) {
            // Constraint: -1000 <= nums[i] <= 1000 (range of 2001 possible values)
            // count[0] represents -1000, count[1000] represents 0, count[2000] represents 1000
            int[] counts = new int[2001];
            
            // 1. Tally occurrences mapping values to indices
            for (int num : nums) {
                // Offset by 1000 to handle negative values cleanly
                counts[num + 1000]++;
            }
            
            // Constraint: nums.length <= 1000, so max possible frequency is 1000.
            // seenFreq[5] = true means we already processed a number that appeared 5 times.
            boolean[] seenFreq = new boolean[1001];
            
            // 2. Check for duplicate frequencies
            for (int count : counts) {
                // We don't care about numbers that never appeared (count == 0)
                if (count > 0) {
                    // If we've already seen this frequency, it's not unique!
                    if (seenFreq[count]) {
                        return false;
                    }
                    // Mark this frequency as seen
                    seenFreq[count] = true;
                }
            }
            
            return true; // All counts were distinct
        }
    }

    // ========================================================================
    // APPROACH 2: HASHMAP + HASHSET (The General-Purpose Solution)
    // ========================================================================
    static class HashMapSolver implements UniqueOccurrencesSolver {
        @Override
        public boolean uniqueOccurrences(int[] nums) {
            // Concrete example: Map stores {1=3, 2=2, 3=1} for [1,2,2,1,1,3]
            Map<Integer, Integer> freqMap = new HashMap<>();
            
            // 1. Count frequencies using a Map
            for (int num : nums) {
                freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
            }
            
            // 2. Load all frequencies into a HashSet
            // Set inherently rejects duplicates. If there are duplicates, 
            // the Set's size will be smaller than the Map's size.
            Set<Integer> uniqueFreqs = new HashSet<>(freqMap.values());
            
            // 3. Compare sizes
            return freqMap.size() == uniqueFreqs.size();
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        UniqueOccurrencesSolver[] solvers = {
            new ArrayOptimizedSolver(),
            new HashMapSolver()
        };

        // Test cases
        // 1. General case (True)
        int[] test1 = {1, 2, 2, 1, 1, 3}; // Freqs: 3, 2, 1 -> True
        
        // 2. Duplicate frequencies (False)
        int[] test2 = {1, 2}; // Freqs: 1, 1 -> False
        
        // 3. Negative numbers and all unique (True)
        int[] test3 = {-3, 0, 1, -3, 1, 1, 1, -3, 10, 0}; // Freqs: 10(1), 0(2), -3(3), 1(4) -> True
        
        // 4. Edge: Single element
        int[] test4 = {5}; // Freqs: 5(1) -> True
        
        // 5. Edge: All duplicates
        int[] test5 = {4, 4, 4}; // Freqs: 4(3) -> True

        for (int i = 0; i < solvers.length; i++) {
            System.out.println("Testing " + solvers[i].getClass().getSimpleName() + "...");
            System.out.println("  Test 1: " + (solvers[i].uniqueOccurrences(test1) == true ? "PASS" : "FAIL"));
            System.out.println("  Test 2: " + (solvers[i].uniqueOccurrences(test2) == false ? "PASS" : "FAIL"));
            System.out.println("  Test 3: " + (solvers[i].uniqueOccurrences(test3) == true ? "PASS" : "FAIL"));
            System.out.println("  Test 4: " + (solvers[i].uniqueOccurrences(test4) == true ? "PASS" : "FAIL"));
            System.out.println("  Test 5: " + (solvers[i].uniqueOccurrences(test5) == true ? "PASS" : "FAIL"));
            System.out.println();
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Two-pass tracking (Value -> Frequency, then Frequency -> Uniqueness).
 * - Key observation: When input ranges are tightly constrained (like -1000 to 1000), 
 *   using direct array mapping is significantly faster and uses strictly O(1) space.
 * - What's worth memorizing: The trick of offsetting negative numbers to fit into a 
 *   0-indexed array (`arr[num + 1000]`).
 * - Most common trap: Forgetting that frequencies themselves can be 0 when iterating 
 *   over the `count` array, which would accidentally mark 0 as a "seen" frequency if 
 *   not filtered out with `if (count > 0)`.
 * - Mental trigger: "Small constraints? Ditch the HashMap, build an Array-Map."
 * ============================================================================
 */


import java.util.*;

class Solution {
    public boolean uniqueOccurrences(int[] nums) {

        // Step 1: Count frequency of each number
        Map<Integer, Integer> freqMap = new HashMap<>();

        for (int num : nums) {
            // Increment count
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }

        // Step 2: Check if frequencies are unique
        Set<Integer> seenFrequencies = new HashSet<>();

        for (int freq : freqMap.values()) {

            // If already present → duplicate frequency
            if (!seenFrequencies.add(freq)) {
                return false;
            }
        }

        return true;
    }
}

import java.util.*;

class Solution {
    public boolean uniqueOccurrences(int[] nums) {

        /*
         * 🧠 PROBLEM UNDERSTANDING:
         * We need to check:
         * → Every number should have a UNIQUE frequency.
         *
         * Example:
         * nums = [1,1,2,2,2,3]
         * Frequencies:
         * 1 → 2 times
         * 2 → 3 times
         * 3 → 1 time
         * Frequencies = [2, 3, 1] → All unique → TRUE
         *
         * If any frequency repeats → FALSE
         */

        /*
         * ⚡ OPTIMIZATION INSIGHT:
         * Constraint: nums[i] ∈ [-1000, 1000]
         *
         * Instead of HashMap (which uses hashing and extra memory),
         * we can use a fixed-size array.
         *
         * Range size = 2001 (from -1000 to +1000)
         *
         * Trick:
         * Map value "num" to index → num + offset
         */

        int offset = 1000;

        /*
         * freq[i] → stores frequency of number (i - offset)
         *
         * Example:
         * freq[1000] → represents number 0
         * freq[1001] → represents number 1
         * freq[999]  → represents number -1
         */
        int[] freq = new int[2001];

        /*
         * 🔹 STEP 1: COUNT FREQUENCIES
         *
         * For each number:
         * → Shift using offset
         * → Increment frequency
         *
         * Time: O(N)
         */
        for (int num : nums) {
            freq[num + offset]++;
        }

        /*
         * 🔹 STEP 2: CHECK UNIQUE FREQUENCIES
         *
         * We now have frequencies stored.
         * Example freq array (non-zero values only):
         * [0,0,2,0,3,0,1,...]
         *
         * We need to ensure:
         * → No two frequencies are the same
         *
         * Use HashSet:
         * → If adding fails → duplicate frequency found
         */

        Set<Integer> seen = new HashSet<>();

        /*
         * 🔍 TRAVERSAL:
         * We iterate over all possible values (2001 elements)
         * but only process non-zero frequencies.
         *
         * Time: O(2001) ≈ O(1) (constant)
         */
        for (int f : freq) {

            // Skip numbers that don't exist in input
            if (f == 0) continue;

            /*
             * If frequency already seen → duplicate → return false
             *
             * HashSet.add() returns:
             * true  → if element was NOT present
             * false → if element already exists
             */
            if (!seen.add(f)) {
                return false;
            }
        }

        /*
         * If we reach here → all frequencies are unique
         */
        return true;
    }
}

/*
⏱ Complexity Analysis (Very Important)
Time Complexity
Step	Cost
Counting frequencies	O(N)
Iterating freq array	O(2001) ≈ O(1)
HashSet operations	O(1) avg
Total	O(N)


Space Complexity
Structure	Space
freq array	O(2001) ≈ O(1)
HashSet	O(K) (unique frequencies)
Total	O(1) (constant space)
*/


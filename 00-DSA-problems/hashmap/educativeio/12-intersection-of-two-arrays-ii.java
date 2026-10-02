/**
 * ============================================================================
 * 350. INTERSECTION OF TWO ARRAYS II
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Are the input arrays already sorted?"
 *    (If they are, we can use a two-pointer approach for O(1) auxiliary space without hashing).
 * 2. "What are the bounds on the array elements?"
 *    (The constraint 0 <= nums[i] <= 1000 is critical; it means we can use a direct-addressed array instead of a heavier HashMap).
 * 3. "Is memory severely constrained, or are the arrays so large they reside on disk?"
 *    (This completely changes the architecture to external sorting and streaming via two-pointers).
 * 4. "Can we mutate the input arrays?"
 *    (If we want to sort them in-place to save memory, we need to ensure the caller doesn't need the original order preserved).
 *
 *
 * ## 2. The reasoning journey
 * > Core Constraint: Unlike a standard set intersection, we must respect frequency. If `nums1` has two '5's 
 * > and `nums2` has three '5's, our result must contain exactly two '5's. The bottleneck is keeping track 
 * > of how many times we've seen each element without recounting.
 *
 * ### Approach 1: Mark and Sweep (The Brute Force)
 * 1. What I'd naturally try: For every element in `nums1`, scan `nums2` for a match. When a match is found, 
 *    add it to the result, and *overwrite* that element in `nums2` with a dummy value (like -1) so it can't 
 *    be matched again by the next duplicate in `nums1`.
 * 2. Why it works: It perfectly pairs up elements one by one, respecting frequencies by destroying used elements.
 * 3. Why it's too costly:
 *    - Time Complexity: O(N * M) — because for each of the N elements in `nums1`, we linearly scan up to M elements in `nums2`.
 *    - Space Complexity: O(1) auxiliary — because we modify the input array in-place and only use loop indices (ignoring the output array).
 * 4. What work is being repeated: We repeatedly scan `nums2` to find matches. We are blind to the overall 
 *    "inventory" of numbers available.
 * 5. What property removes the bottleneck: "Supply and Demand". If we inventory the entire supply of `nums1` 
 *    upfront, we can just check if demand from `nums2` can be fulfilled instantly.
 *
 * ### Approach 2: Frequency HashMap (The Standard Developer Way)
 * 1. What I'd naturally try: Build a `HashMap<Integer, Integer>` to count frequencies of `nums1` (the supply). 
 *    Then, iterate through `nums2` (the demand). If `map.get(num) > 0`, we add `num` to our result and decrement 
 *    the map's count.
 * 2. Why it works: HashMaps reduce the O(M) search per element to O(1), and tracking the integer count 
 *    perfectly limits the intersection to the minimum overlap.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + M) — because we traverse `nums1` once to populate the map, and `nums2` once to query it.
 *    - Space Complexity: O(N) auxiliary — because we store up to N unique key-value pairs in the HashMap (Node objects, Integer wrappers).
 * 4. What work is being repeated: We are using a complex object-based data structure (HashMap) that computes 
 *    hash codes and handles collisions, even though our domain is tiny (0 to 1000).
 * 5. What property removes the bottleneck: The constraints bound the values strictly between 0 and 1000. 
 *    This means indices of a primitive array can serve as perfect hash keys.
 *
 * ### Approach 3: Bounded Frequency Array (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Replace the `HashMap` with an `int[1001]` array. `counts[num]++` builds the supply. 
 *    When iterating `nums2`, if `counts[num] > 0`, we add to the result and decrement `counts[num]--`.
 * 2. Why it works: It executes the exact same logic as the HashMap but uses contiguous memory. Array lookups 
 *    bypass all Java Object overhead.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + M) — because we iterate through both input arrays exactly once.
 *    - Space Complexity: O(1) auxiliary — because we allocate a fixed 1001-size integer array (about 4 KB), 
 *      which does not scale with the size of N or M.
 *
 * > **Interview Strategy:** Start by explicitly acknowledging the constraints ("Since the values are bounded 
 * > up to 1000..."). Write Approach 3. It's incredibly fast, uses less code than a HashMap, and shows 
 * > mechanical sympathy. Then, immediately pivot to discuss what you would do if the arrays were sorted 
 * > (Approach 4), as this is the most famous follow-up for this specific problem.
 *
 *
 * ## 3. Edge cases
 * - No intersection at all: `[1, 2]` and `[3, 4]`. Returns `[]`.
 * - One array is entirely duplicates: `[5, 5, 5]` and `[5]`. Returns `[5]`.
 * - Differing lengths: `nums1` has 10 elements, `nums2` has 1000 elements. (Optimization hint: Always build 
 *   the frequency map/array from the *smaller* array to save space if using unbounded approaches).
 *
 *
 * ## 4. Key Insight, Pitfalls & Dry Run
 * 
 * **Key Insight: The "Decrement" Filter**
 * For basic intersection (no duplicates), we used a `boolean[]` and flipped `true` to `false`. 
 * For frequency intersection, we just upgrade the boolean to an `int[]` counter. Decrementing the counter 
 * (`count[num]--`) acts as an automatic filter. Once the supply hits 0, any further identical numbers in 
 * `nums2` are correctly ignored.
 *
 * **Pitfall: ArrayList to int[] conversion**
 * In Java, converting a `List<Integer>` to an `int[]` at the end of the method is tedious and slow (requires 
 * iterating and unboxing). Instead, pre-allocate a primitive `int[]` sized to `Math.min(nums1.length, nums2.length)`, 
 * use a tracking `index`, and use `Arrays.copyOf()` at the end.
 *
 * **Dry Run of Approach 3 (Frequency Array):**
 * `nums1 = [4, 9, 5, 4]`, `nums2 = [9, 4, 9, 8, 4]`
 * 1. Build Supply (Iterate nums1):
 *    - count[4] = 2
 *    - count[5] = 1
 *    - count[9] = 1
 * 2. Match Demand (Iterate nums2):
 *    - num = 9: count[9] > 0? YES (1). Add `9`. count[9] becomes 0.
 *    - num = 4: count[4] > 0? YES (2). Add `4`. count[4] becomes 1.
 *    - num = 9: count[9] > 0? NO (0). Skip. (Perfectly handles the duplicate '9' in nums2!)
 *    - num = 8: count[8] > 0? NO (0). Skip.
 *    - num = 4: count[4] > 0? YES (1). Add `4`. count[4] becomes 0.
 * Result: `[9, 4, 4]`. (Order doesn't matter).
 *
 *
 * ## 5. Follow-ups (The Famous LeetCode Trilogy)
 * - **Q1: What if the given arrays are already sorted? How would you optimize your algorithm?**
 *   **A:** I would drop the frequency array/map entirely and use Two Pointers. Pointer `i` on `nums1`, 
 *   pointer `j` on `nums2`. 
 *   If `nums1[i] < nums2[j]`, `i++`. 
 *   If `nums1[i] > nums2[j]`, `j++`. 
 *   If they equal, add to result, and increment *both*. 
 *   This runs in O(N+M) time and strictly O(1) space. (See `TwoPointerSolver`).
 * 
 * - **Q2: What if nums1's size is small compared to nums2's size? Which algorithm is better?**
 *   **A:** If `nums1` is much smaller (e.g., N=10, M=1,000,000), building a HashMap of `nums1` is optimal 
 *   because the space complexity drops to O(N). If the arrays are *sorted*, we can iterate over the 
 *   10 elements of `nums1` and use Binary Search on `nums2` to find matches in O(N log M) time, 
 *   which is 10 * 20 = 200 operations (way faster than O(N+M) = 1,000,010 operations).
 * 
 * - **Q3: What if elements of nums2 are stored on disk, and memory is limited such that you cannot load all elements into memory at once?**
 *   **A:** 
 *   - If `nums1` fits in memory: Load `nums1` into a HashMap (or Frequency Array), then stream `nums2` 
 *     from disk chunk by chunk, checking against the map.
 *   - If *neither* fits in memory: Use an external sort on both files (sorting them on disk). Then 
 *     read a small chunk of each into memory and use the Two-Pointer approach to find intersections.
 *
 * ============================================================================
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IntersectionOfArraysIIStudy {

    interface IntersectionSolver {
        int[] intersect(int[] nums1, int[] nums2);
    }

    // ========================================================================
    // APPROACH 3: BOUNDED FREQUENCY ARRAY (The Optimal Interview Solution)
    // ========================================================================
    static class FrequencyArraySolver implements IntersectionSolver {
        @Override
        public int[] intersect(int[] nums1, int[] nums2) {
            // Constraint: 0 <= nums[i] <= 1000
            int[] counts = new int[1001];
            
            // Build the supply ledger from the first array
            for (int num : nums1) {
                counts[num]++;
            }
            
            // The maximum possible intersection size is the length of the smaller array.
            int[] tempResult = new int[Math.min(nums1.length, nums2.length)];
            int idx = 0;
            
            // Process the demand from the second array
            for (int num : nums2) {
                // If supply exists, we have a valid intersection pair!
                if (counts[num] > 0) {
                    tempResult[idx++] = num;
                    
                    // Decrement supply so we don't reuse this character
                    counts[num]--; 
                }
            }
            
            // Trim the array to the exact number of elements found
            return Arrays.copyOf(tempResult, idx);
        }
    }

    // ========================================================================
    // APPROACH 2: HASH MAP (The General Purpose / Unbounded Solution)
    // ========================================================================
    static class HashMapSolver implements IntersectionSolver {
        @Override
        public int[] intersect(int[] nums1, int[] nums2) {
            // Optimization for unbounded arrays: Always hash the smaller array to save space
            if (nums1.length > nums2.length) {
                return intersect(nums2, nums1);
            }
            
            Map<Integer, Integer> map = new HashMap<>();
            for (int num : nums1) {
                map.put(num, map.getOrDefault(num, 0) + 1);
            }
            
            int[] tempResult = new int[nums1.length];
            int idx = 0;
            
            for (int num : nums2) {
                int count = map.getOrDefault(num, 0);
                if (count > 0) {
                    tempResult[idx++] = num;
                    map.put(num, count - 1);
                }
            }
            
            return Arrays.copyOf(tempResult, idx);
        }
    }

    // ========================================================================
    // APPROACH 4: TWO POINTERS (The Optimal Solution if arrays are SORTED)
    // Used here to demonstrate the answer to Follow-up #1
    // ========================================================================
    static class TwoPointerSolver implements IntersectionSolver {
        @Override
        public int[] intersect(int[] nums1, int[] nums2) {
            // If the problem stated the arrays were already sorted, we'd skip this O(N log N) step.
            Arrays.sort(nums1);
            Arrays.sort(nums2);
            
            int[] tempResult = new int[Math.min(nums1.length, nums2.length)];
            int idx = 0;
            
            int i = 0; // Pointer for nums1
            int j = 0; // Pointer for nums2
            
            while (i < nums1.length && j < nums2.length) {
                if (nums1[i] < nums2[j]) {
                    // nums1 is lagging behind, catch it up
                    i++;
                } else if (nums1[i] > nums2[j]) {
                    // nums2 is lagging behind, catch it up
                    j++;
                } else {
                    // Found a match!
                    tempResult[idx++] = nums1[i];
                    // Advance both pointers since both elements are "consumed"
                    i++;
                    j++;
                }
            }
            
            return Arrays.copyOf(tempResult, idx);
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        IntersectionSolver[] solvers = {
            new FrequencyArraySolver(),
            new HashMapSolver(),
            new TwoPointerSolver()
        };

        // Format: {arr1, arr2} -> Expected output array
        int[][][] testCases = {
            {{1, 2, 2, 1}, {2, 2}},                 // Exact frequency match -> [2, 2]
            {{4, 9, 5}, {9, 4, 9, 8, 4}},           // Misaligned frequencies -> [9, 4]
            {{1, 2, 3}, {4, 5, 6}},                 // No intersection -> []
            {{5, 5, 5}, {5, 5}},                    // Overlapping duplicates -> [5, 5]
            {{0, 1000}, {1000, 0, 0}}               // Boundary limits -> [0, 1000]
        };

        int[][] expectedResults = {
            {2, 2},
            {4, 9},
            {},
            {5, 5},
            {0, 1000}
        };

        for (IntersectionSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                int[] nums1 = testCases[i][0];
                int[] nums2 = testCases[i][1];
                int[] expected = expectedResults[i];
                
                int[] result = solver.intersect(nums1, nums2);
                
                // Problem states order does not matter, so we sort before comparison
                Arrays.sort(expected);
                Arrays.sort(result);
                
                if (!Arrays.equals(expected, result)) {
                    System.out.printf("  [FAIL] Test %d. Expected: %s, Got: %s%n", 
                        i + 1, Arrays.toString(expected), Arrays.toString(result));
                    allPassed = false;
                }
            }
            
            if (allPassed) {
                System.out.println("  [SUCCESS] All test cases passed!\n");
            }
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Supply and Demand matching using Frequency counting.
 * - Key observation: Bounded constraints (<= 1000) allow the use of a primitive 
 *   array, replacing the HashMap for a massive speed increase.
 * - What's worth memorizing: The decrement filter (`count[num]--`). It elegantly 
 *   prevents over-counting duplicates without needing complex conditional logic.
 * - Most common trap: Trying to use `.remove()` on a List or Set inside a loop, 
 *   which alters data structure sizes and can cause O(N) shifts or ConcurrentModificationExceptions.
 * - Mental trigger: "Intersection with counts? Frequency Array. Extremely large? 
 *   Sort and Two-Pointer."
 * ============================================================================
 */


import java.util.*;

/**
 * 🟢 PROBLEM: Intersection of Two Arrays (UNIQUE Elements)
 *
 * Given two arrays, return ONLY UNIQUE common elements.
 *
 * Example:
 * nums1 = [1,2,2,1]
 * nums2 = [2,2]
 *
 * Output: [2]   (NOT [2,2])
 *
 * ------------------------------------------------------------
 *
 * 🧠 CORE IDEA (Interview Thinking)
 *
 * 1. We don't care about duplicates → so we use SET
 * 2. A set automatically:
 *      - stores only unique values
 *      - provides O(1) lookup
 *
 * ------------------------------------------------------------
 *
 * 🚀 APPROACH:
 *
 * Step 1: Put all elements of nums1 into a HashSet
 * Step 2: Iterate nums2
 * Step 3: If element exists in set1 → add to result set
 * Step 4: Convert result set → array
 *
 * ------------------------------------------------------------
 *
 * ⏱ TIME COMPLEXITY:
 * O(n + m)
 * - n = nums1 length
 * - m = nums2 length
 *
 * Because:
 * - inserting into set → O(1)
 * - lookup → O(1)
 *
 * 🧠 SPACE COMPLEXITY:
 * O(n)
 * - storing elements of nums1
 *
 */
class Solution {

    public int[] intersection(int[] nums1, int[] nums2) {

        // 🟡 Step 1: Store nums1 elements in a set
        // Why? → Fast lookup + uniqueness
        Set<Integer> set1 = new HashSet<>();

        for (int num : nums1) {
            set1.add(num);  // duplicates automatically ignored
        }

        // 🟡 Step 2: Result set (to ensure unique intersection)
        Set<Integer> resultSet = new HashSet<>();

        // 🟡 Step 3: Traverse nums2
        for (int num : nums2) {

            // If element exists in nums1
            if (set1.contains(num)) {

                // Add to result (duplicates automatically avoided)
                resultSet.add(num);
            }
        }

        // 🟡 Step 4: Convert Set → int[]
        // Java streams used for clean conversion
        return resultSet.stream()
                        .mapToInt(Integer::intValue)
                        .toArray();
    }
}


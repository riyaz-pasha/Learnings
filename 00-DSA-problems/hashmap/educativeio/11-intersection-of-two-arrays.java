/**
 * ============================================================================
 * L E E T C O D E   3 4 9 :   I N T E R S E C T I O N   O F   T W O   A R R A Y S
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Are the arrays sorted?"
 *    (If they are, we can use a two-pointer approach in O(N+M) time without extra memory. If not, sorting them costs O(N log N)).
 * 2. "What is the range of the numbers?"
 *    (Crucial: The constraint 0 <= nums[i] <= 1000 means we can use a direct-mapped Boolean array instead of a HashMap).
 * 3. "Is one array significantly larger than the other?"
 *    (If arr1 has 10 elements and arr2 has 100,000, we should load arr1 into memory/hash and stream arr2, or binary search arr2 if sorted).
 * 4. "Can we modify the input arrays?"
 *    (If we want to sort in-place to save space, we need to know if the caller still needs the original ordering).
 * 5. "If a number appears multiple times in both arrays, does it appear multiple times in the result?"
 *    (The prompt explicitly states: "Each element in the result must be unique.")
 *
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need to find elements that exist in both collections, but we must strictly 
 * > return each intersecting element exactly once, regardless of how many times it duplicates. 
 * > The bottleneck is searching for the element and ensuring the result is unique.
 *
 * ### Approach 1: Nested Loops + HashSet (The Brute Force)
 * 1. What I'd naturally try: For every element in `arr1`, scan the entirety of `arr2`. If it matches, 
 *    add it to a `HashSet` to guarantee we don't add it twice.
 * 2. Why it works: It manually verifies every combination of elements and relies on the Set for uniqueness.
 * 3. Why it's too costly:
 *    - Time Complexity: O(N * M) — because for each of the N elements in arr1, we potentially scan all M elements in arr2.
 *    - Space Complexity: O(U) — because we store up to U unique intersecting elements in a HashSet to build the result.
 * 4. What work is being repeated: If `arr1` is `[5, 5, 5]` and `arr2` has `5` at the very end, we scan `arr2` three 
 *    separate times looking for the exact same number.
 * 5. What property removes the bottleneck: Lookup speed. If we record what exists in `arr1` first, we can check 
 *    `arr2` elements against that record instantly.
 *
 * ### Approach 2: Two HashSets (The Standard Developer Way)
 * 1. What I'd naturally try: Dump `arr1` into `HashSet<Integer> set1`. Then iterate through `arr2`. 
 *    If `set1.contains(arr2[i])`, add it to a second set `intersectSet` to ensure uniqueness.
 * 2. Why it works: HashSets reduce the O(M) scan to an O(1) lookup.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + M) — because we traverse `arr1` once to populate the set, and `arr2` once to check it.
 *    - Space Complexity: O(N + U) — because we store N elements in the first set, and U unique intersections in the result set.
 * 4. What work is being repeated: We are dealing with boxing/unboxing `int` to `Integer`, computing hash codes, 
 *    and resolving collisions. For small bounded integers (0 to 1000), this is structural overkill.
 * 5. What property removes the bottleneck: The constraints explicitly bound the values to a maximum of 1000. 
 *    We can map these values directly to memory addresses using a primitive array!
 *
 * ### Approach 3: Bounded Boolean Array (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Replace `HashSet` with `boolean[1001] seen`. Iterate `arr1` and set `seen[num] = true`. 
 *    Then iterate `arr2`, check the array, and gather the result.
 * 2. Why it works: Array indices act as perfect, collision-free hash keys. But wait! How do we ensure uniqueness? 
 *    If `arr2` has `[2, 2]` and `seen[2]` is true, we might add it twice. 
 *    *The Trick:* Once we find a match, we instantly set `seen[num] = false`. The second `2` will be ignored!
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + M) — because we iterate through both input arrays exactly once.
 *    - Space Complexity: O(1) auxiliary — because we allocate a fixed 1001-size boolean array (about 1 KB) 
 *      regardless of how large N and M scale. (Output array space is excluded from auxiliary complexity).
 *
 * > **Interview Strategy:** In a real interview, explicitly point out the constraints. Say, "Normally I would 
 * > use two HashSets, but because the values are bound between 0 and 1000, I can optimize the space and speed 
 * > dramatically by using a Boolean array." Write Approach 3.
 *
 * 
 * ## 3. Edge cases
 * - No intersection: `[1, 2, 3]` and `[4, 5, 6]`. (Returns empty array).
 * - Massive duplicates: `[1, 1, 1, 1]` and `[1, 1]`. (Must return exactly `[1]`).
 * - One array is empty: (Constraints say length >= 1, but if it were 0, return empty array immediately).
 * - Values at the boundary: `0` and `1000`. (Ensure the array is size 1001 to prevent OutOfBounds).
 *
 * 
 * ## 4. Key Insight, Dry Run & Pitfalls
 * 
 * **Key Insight: The "Read & Erase" pattern**
 * To enforce uniqueness without a `Set`, simply erase the data the moment you use it. 
 * If `seen[num] == true`, add to result, then instantly `seen[num] = false`. Subsequent duplicates in `arr2` 
 * will safely miss the condition.
 * 
 * **Dry Run of Approach 3:**
 * arr1 = `[4, 9, 5]`, arr2 = `[9, 4, 9, 8, 4]`
 * 1. Build Phase:
 *    - seen[4] = true
 *    - seen[9] = true
 *    - seen[5] = true
 * 2. Intersect Phase (arr2):
 *    - num = 9: seen[9] is TRUE. Add to result `[9]`. Set seen[9] = FALSE.
 *    - num = 4: seen[4] is TRUE. Add to result `[9, 4]`. Set seen[4] = FALSE.
 *    - num = 9: seen[9] is FALSE (we erased it!). Skip.
 *    - num = 8: seen[8] is FALSE. Skip.
 *    - num = 4: seen[4] is FALSE (we erased it!). Skip.
 * Result = `[9, 4]`.
 * 
 * **Pitfall: Dynamic Array Sizing**
 * We don't know how big the intersection will be. Since we must return an `int[]`, we can't just use a 
 * standard array without sizing it correctly. 
 * *Solution:* Pre-allocate a temporary array of the maximum possible size (1001, or `Math.min(N, M)`), 
 * fill it using a tracking `index`, and use `Arrays.copyOf()` at the very end to trim the fat.
 *
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the arrays were already sorted?
 *   **A:** I would use the Two-Pointer approach. Pointer `i` on `arr1`, `j` on `arr2`. If `arr1[i] < arr2[j]`, 
 *   increment `i`. If `arr1[i] > arr2[j]`, increment `j`. If they match, add to result, then increment both 
 *   while skipping duplicates. Time: O(N+M), Space: strictly O(1) with no bounded array needed.
 * - **Q:** What if `arr1`'s size is extremely small compared to `arr2` (e.g., N=10, M=1,000,000) and they are sorted?
 *   **A:** The Two-Pointer approach takes O(N+M) = 1,000,010 operations. Instead, we can iterate over the 
 *   smaller `arr1` and perform a Binary Search for each element inside `arr2`. This takes O(N log M) = 
 *   10 * 20 = 200 operations. Massive improvement.
 * - **Q:** What if the elements cannot fit in memory?
 *   **A:** We would use external sorting on both files on disk, then read them chunk by chunk using 
 *   the Two-Pointer approach.
 *
 * ============================================================================
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntersectionOfArraysStudy {

    interface IntersectionSolver {
        int[] intersection(int[] arr1, int[] arr2);
    }

    // ========================================================================
    // APPROACH 3: BOUNDED BOOLEAN ARRAY (The Optimal Interview Solution)
    // ========================================================================
    static class BooleanArraySolver implements IntersectionSolver {
        @Override
        public int[] intersection(int[] arr1, int[] arr2) {
            // Constraint: 0 <= arr[i] <= 1000
            boolean[] seen = new boolean[1001];
            
            // 1. Mark presence of all elements in arr1
            for (int num : arr1) {
                seen[num] = true;
            }
            
            // 2. Find intersection
            // The max possible intersection size is the length of the smaller array.
            // Using a temporary array avoids the overhead of an ArrayList.
            int[] tempResult = new int[Math.min(arr1.length, arr2.length)];
            int idx = 0;
            
            for (int num : arr2) {
                if (seen[num]) {
                    tempResult[idx++] = num;
                    
                    // The "Read & Erase" Trick!
                    // Setting to false immediately guarantees we never add this number again,
                    // flawlessly handling duplicates in arr2 without needing a Set.
                    seen[num] = false; 
                }
            }
            
            // 3. Trim the array to the exact number of elements found
            return Arrays.copyOf(tempResult, idx);
        }
    }

    // ========================================================================
    // APPROACH 2: TWO HASHSETS (The General Purpose / Unbounded Solution)
    // Good to know if values could be anything (e.g., negative, or up to 10^9)
    // ========================================================================
    static class HashSetSolver implements IntersectionSolver {
        @Override
        public int[] intersection(int[] arr1, int[] arr2) {
            Set<Integer> set1 = new HashSet<>();
            for (int num : arr1) {
                set1.add(num);
            }
            
            Set<Integer> intersect = new HashSet<>();
            for (int num : arr2) {
                // If it's in the first set, add it to our unique result set.
                if (set1.contains(num)) {
                    intersect.add(num);
                }
            }
            
            // Convert Set to int[] (Standard Java boilerplate)
            int[] result = new int[intersect.size()];
            int idx = 0;
            for (int num : intersect) {
                result[idx++] = num;
            }
            return result;
        }
    }

    // ========================================================================
    // APPROACH 1.5: TWO POINTERS (The Best Solution if Inputs are SORTED)
    // ========================================================================
    static class TwoPointerSolver implements IntersectionSolver {
        @Override
        public int[] intersection(int[] arr1, int[] arr2) {
            // In a real interview, only use this if arrays are pre-sorted, 
            // OR if you want to optimize strictly for O(1) space on unbounded data.
            Arrays.sort(arr1);
            Arrays.sort(arr2);
            
            int[] temp = new int[Math.min(arr1.length, arr2.length)];
            int idx = 0;
            int i = 0; 
            int j = 0;
            
            while (i < arr1.length && j < arr2.length) {
                if (arr1[i] < arr2[j]) {
                    i++;
                } else if (arr1[i] > arr2[j]) {
                    j++;
                } else {
                    // Match found! Only add if it's the first time we've seen it.
                    if (idx == 0 || temp[idx - 1] != arr1[i]) {
                        temp[idx++] = arr1[i];
                    }
                    i++;
                    j++;
                }
            }
            
            return Arrays.copyOf(temp, idx);
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        IntersectionSolver[] solvers = {
            new BooleanArraySolver(),
            new HashSetSolver(),
            new TwoPointerSolver()
        };

        // Format: {arr1, arr2} -> Expected output array
        int[][][] testCases = {
            {{1, 2, 2, 1}, {2, 2}},                 // Basic duplicate case -> [2]
            {{4, 9, 5}, {9, 4, 9, 8, 4}},           // Mixed order case -> [9, 4] or [4, 9]
            {{1, 2, 3}, {4, 5, 6}},                 // No intersection -> []
            {{1, 1, 1}, {1, 1}},                    // Heavy duplicates -> [1]
            {{0, 1000}, {1000, 0, 500}}             // Boundary limits -> [0, 1000]
        };

        int[][] expectedResults = {
            {2},
            {4, 9},
            {},
            {1},
            {0, 1000}
        };

        for (IntersectionSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                int[] arr1 = testCases[i][0];
                int[] arr2 = testCases[i][1];
                int[] expected = expectedResults[i];
                
                int[] result = solver.intersection(arr1, arr2);
                
                // Order doesn't matter, so sort both before comparing
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
 * - Core pattern: Marking seen elements, then clearing them upon discovery.
 * - Key observation: When constraints tightly bound the data (e.g., <= 1000), 
 *   a direct-mapped array completely removes the need for HashSets, bypassing 
 *   hashing overhead and garbage collection.
 * - What's worth memorizing: The "Read & Erase" trick (`seen[num] = false;`). 
 *   This beautifully handles duplicates in the second array without requiring 
 *   a secondary `HashSet` for the results.
 * - Most common trap: Forgetting to handle duplicate hits when iterating the 
 *   second array.
 * - Mental trigger: "Finding unique intersection bounded <= 1000? Use boolean array 
 *   and flip true back to false."
 * ============================================================================
 */


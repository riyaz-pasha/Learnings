/**
 * ============================================================================
 * L E E T C O D E   5 2 5 :   C O N T I G U O U S   A R R A Y
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Can the array contain anything other than 0s and 1s?"
 *    (Ensures we don't have to handle ignored characters. The problem confirms strictly 0s and 1s).
 * 2. "What should we return if the array has no balanced subarray (e.g., all 1s)?"
 *    (Return 0, as the maximum length is zero).
 * 3. "Is it permissible to mutate the input array?"
 *    (If memory is extremely tight, we could theoretically modify the array to store our -1s, though it's generally frowned upon and doesn't actually save O(N) map space).
 * 4. "If there are multiple subarrays of the same maximum length, does it matter which one we return?"
 *    (The prompt only asks for the *length*, not the subarray bounds, so ties don't matter).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need to find the longest contiguous sequence where `count(0) == count(1)`. 
 * > Comparing two separate running counts continuously is messy. The bottleneck is finding a way 
 * > to mathematically define "balance" in a single variable.
 * 
 * ### Approach 1: Mark and Sweep (Brute Force)
 * 1. What I'd naturally try: For every possible starting index `i`, loop through every ending index `j`. 
 *    Keep a count of 0s and 1s. If at any point they are equal, record the length `j - i + 1` and update the max.
 * 2. Why it works: It tests every single contiguous subarray exhaustively.
 * 3. Why it's too costly:
 *    - Time Complexity: O(N^2) — For an array of size 10^5, N^2 is 10^10 operations. This guarantees a Time Limit Exceeded (TLE) error.
 *    - Space Complexity: O(1) auxiliary — We only need integer counters.
 * 4. What work is being repeated: We recalculate the balance of the same segments repeatedly. If we know the 
 *    balance from index 0 to 5, we shouldn't have to recalculate the balance of 1 to 5 from scratch.
 * 5. What property removes the bottleneck: The "Zero-Sum" trick. If we replace all `0`s with `-1`s, a balanced 
 *    subarray will have a sum of exactly 0!
 * 
 * ### Approach 2: Prefix Sum + HashMap (The Standard Optimal Way)
 * 1. What I'd naturally try: Treat 0s as -1s. Keep a running sum. If my sum at index 2 is `4`, and my sum at 
 *    index 8 is `4`, what happened between index 3 and 8? The net change was 0! That means there were an equal 
 *    number of 1s and -1s. I will use a `HashMap<Integer, Integer>` to map `RunningSum -> First Seen Index`.
 * 2. Why it works: It leverages the mathematical property of prefix sums. `Prefix[j] - Prefix[i] == 0` means 
 *    the segment sum is 0.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we traverse the array exactly once, and HashMap lookups are O(1).
 *    - Space Complexity: O(N) — because in the worst case (all 1s), the running sum is always unique, and 
 *      we store N entries in the Map.
 * 4. What work is being repeated: HashMaps in Java compute hash codes, handle collisions, and allocate heap 
 *    memory for `Integer` objects. This is heavy machinery for tracking simple sums.
 * 5. What property removes the bottleneck: The running sum is mathematically bounded! Since the array size is N, 
 *    the sum can never be less than -N (all 0s) and never be greater than +N (all 1s). We can use a raw array!
 * 
 * ### Approach 3: Shifted Array (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Replace the HashMap with an `int[]` of size `2 * N + 1`. Because array indices 
 *    can't be negative (to handle sums like -5), I will shift everything by `N`. A sum of `-5` goes to index 
 *    `N - 5`. A sum of `0` goes to index `N`.
 * 2. Why it works: Array indices provide true, collision-free O(1) lookups directly from CPU cache, bypassing 
 *    the Java garbage collector entirely.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — Single pass, direct memory access.
 *    - Space Complexity: O(N) — We allocate an array of size `2N + 1`. While Big-O is the same as the HashMap, 
 *      the constant factor is drastically smaller (primitives vs objects).
 * 
 * > **Interview Strategy:** This problem is the direct cousin of LeetCode 523 (Continuous Subarray Sum). 
 * > Start by explaining the "Change 0 to -1" trick out loud. Then code Approach 2 (HashMap) because it's 
 * > the most readable. If the interviewer asks for optimization, immediately drop the HashMap for the Shifted Array.
 * 
 * 
 * ## 3. Edge cases
 * - All zeros or all ones: `[1, 1, 1, 1]`. The map just keeps growing, length stays 0. Works perfectly.
 * - Entire array is balanced: `[0, 1, 0, 1]`. Must initialize the map with `(0, -1)` to catch the balance 
 *   starting from the very beginning.
 * - Single element: `[0]`. Loop runs once, sum is -1. Max length remains 0.
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight:** "Replace and Track." 
 * By changing the target from "Count of A == Count of B" to "Sum of segment == 0" (by treating A as 1 and B as -1), 
 * you transform a two-variable problem into a classic 1-D Prefix Sum problem.
 * 
 * **Dry Run of Approach 2:**
 * `nums = [0, 1, 0, 0, 1]`
 * Mental translation: `[-1, 1, -1, -1, 1]`
 * Init: map = `{0: -1}`, maxLen = 0, sum = 0
 * 
 * | idx | val | sum | Seen in Map? | Distance (`idx - map.get(sum)`) | Map Update (if unseen) | maxLen |
 * |-----|-----|-----|--------------|---------------------------------|------------------------|--------|
 * |  0  | -1  | -1  | No           | -                               | put(-1, 0)             | 0      |
 * |  1  |  1  |  0  | YES (at -1)  | 1 - (-1) = 2                    | -                      | 2      |
 * |  2  | -1  | -1  | YES (at 0)   | 2 - 0 = 2                       | -                      | 2      |
 * |  3  | -1  | -2  | No           | -                               | put(-2, 3)             | 2      |
 * |  4  |  1  | -1  | YES (at 0)   | 4 - 0 = 4                       | -                      | 4      |
 * 
 * Result: 4. (The subarray from index 1 to 4: `[1, 0, 0, 1]`).
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the array had three values (0, 1, 2) and we wanted a subarray with an equal number of all three?
 *   **A:** We can't use the simple -1/1 sum trick anymore. Instead, we track the *relative differences*. The state 
 *   becomes a tuple: `(count0 - count1, count1 - count2)`. We store this tuple in the HashMap. If we see the exact 
 *   same tuple again, the segments grew uniformly! 
 * - **Q:** Why do we only put the sum in the map if it's NOT already there?
 *   **A:** Because we want the *maximum* length. The first time we see a sum is the furthest back in time (the lowest 
 *   index). Keeping the oldest index guarantees `currentIndex - oldIndex` yields the largest possible distance.
 * 
 * ============================================================================
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ContiguousArrayStudy {

    interface ContiguousArraySolver {
        int findMaxLength(int[] nums);
    }

    // ========================================================================
    // APPROACH 3: SHIFTED ARRAY (The Optimal Hardware-Friendly Solution)
    // ========================================================================
    static class ArraySolver implements ContiguousArraySolver {
        @Override
        public int findMaxLength(int[] nums) {
            int n = nums.length;
            // The running sum ranges from -N to +N. Total possible values = 2N + 1.
            int[] seenIndices = new int[2 * n + 1];
            
            // We need a dummy value to represent "unseen". We can't use 0 because 
            // 0 is a valid array index. Let's use -2.
            Arrays.fill(seenIndices, -2);
            
            // Critical initialization: The running sum of 0 happens at virtual index -1.
            // Since our array is shifted by 'n', a sum of 0 is stored at index 'n'.
            seenIndices[n] = -1;
            
            int maxLen = 0;
            int sum = 0;
            
            for (int i = 0; i < n; i++) {
                // The core trick: treat 0 as -1
                sum += (nums[i] == 0) ? -1 : 1;
                
                // Shift the sum to safely index into our non-negative array
                int shiftedSum = sum + n;
                
                if (seenIndices[shiftedSum] != -2) {
                    // We've seen this sum before! Calculate distance.
                    int distance = i - seenIndices[shiftedSum];
                    if (distance > maxLen) {
                        maxLen = distance;
                    }
                } else {
                    // First time seeing this sum, record its index
                    seenIndices[shiftedSum] = i;
                }
            }
            
            return maxLen;
        }
    }

    // ========================================================================
    // APPROACH 2: PREFIX SUM + HASHMAP (The Standard / Most Readable Solution)
    // ========================================================================
    static class HashMapSolver implements ContiguousArraySolver {
        @Override
        public int findMaxLength(int[] nums) {
            // Map stores <RunningSum, First Seen Index>
            Map<Integer, Integer> sumToIndex = new HashMap<>();
            
            // Base case: A sum of 0 conceptually exists just before the array starts.
            sumToIndex.put(0, -1);
            
            int maxLen = 0;
            int sum = 0;
            
            for (int i = 0; i < nums.length; i++) {
                sum += (nums[i] == 0) ? -1 : 1;
                
                if (sumToIndex.containsKey(sum)) {
                    // If we've seen this sum before, the segment between that first
                    // occurrence and now nets to 0 (equal 1s and 0s).
                    int prevIndex = sumToIndex.get(sum);
                    maxLen = Math.max(maxLen, i - prevIndex);
                } else {
                    // Only record the FIRST time we see a sum to maximize future lengths
                    sumToIndex.put(sum, i);
                }
            }
            
            return maxLen;
        }
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (Conceptual Baseline)
    // ========================================================================
    static class BruteForceSolver implements ContiguousArraySolver {
        @Override
        public int findMaxLength(int[] nums) {
            int maxLen = 0;
            
            for (int i = 0; i < nums.length; i++) {
                int zeroes = 0;
                int ones = 0;
                
                for (int j = i; j < nums.length; j++) {
                    if (nums[j] == 0) zeroes++;
                    else ones++;
                    
                    if (zeroes == ones) {
                        maxLen = Math.max(maxLen, j - i + 1);
                    }
                }
            }
            return maxLen;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        ContiguousArraySolver[] solvers = {
            new ArraySolver(),
            new HashMapSolver(),
            new BruteForceSolver() // Only run on small datasets
        };

        // Format: {nums_array}, {expected_result}
        int[][][] testCases = {
            {{0, 1}, {2}},                            // Standard base case
            {{0, 1, 0}, {2}},                         // Standard odd length
            {{0, 1, 0, 0, 1}, {4}},                   // Internal balanced segment
            {{0, 0, 0, 1, 1, 1}, {6}},                // Entire array balanced
            {{1, 1, 1, 1}, {0}},                      // No zeroes
            {{0, 0, 0}, {0}},                         // No ones
            {{0}, {0}},                               // Single element
            {{1, 0, 1, 0, 1, 1, 1, 0, 0, 1}, {8}}     // Complex long pattern
        };

        for (ContiguousArraySolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                int[] nums = testCases[i][0];
                int expected = testCases[i][1][0];
                
                int result = solver.findMaxLength(nums);
                
                if (result != expected) {
                    System.out.printf("  [FAIL] Test %d. Expected: %d, Got: %d%n", 
                        i + 1, expected, result);
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
 * - Core pattern: Prefix Sum Tracking (State Map).
 * - Key observation: Finding equal counts of two items is geometrically identical 
 *   to mapping them to +1 and -1 and finding a segment sum of 0.
 * - What's worth memorizing: The base case initialization `map.put(0, -1)`. Without 
 *   this, a valid balanced array starting at index 0 will be completely missed.
 * - Most common trap: Updating the Map with a new index when you see a sum again. 
 *   You want to maximize length, so always keep the EARLIEST index you ever saw.
 * - Mental trigger: "Contiguous subarray equality? Change to 1 and -1, look for 
 *   duplicate Prefix Sums."
 * ============================================================================
 */


import java.util.HashMap;
import java.util.Map;

/**
 * Problem: Contiguous Array
 *
 * Given a binary array containing only 0s and 1s,
 * find the maximum length of a contiguous subarray
 * containing an equal number of 0s and 1s.
 *
 * ---------------------------------------------------------
 * CORE IDEA
 * ---------------------------------------------------------
 *
 * We want:
 *
 *      number of 0s == number of 1s
 *
 * Convert the problem into a SUM problem:
 *
 *      0 -> -1
 *      1 -> +1
 *
 * Example:
 *
 *      [0, 1, 0, 1]
 *
 * becomes:
 *
 *      [-1, +1, -1, +1]
 *
 * Now:
 *
 *      equal number of 0s and 1s
 *                    ↓
 *              subarray sum = 0
 *
 * So the actual problem becomes:
 *
 *      Find the longest contiguous subarray
 *      whose sum is 0.
 *
 *
 * ---------------------------------------------------------
 * PREFIX SUM OBSERVATION
 * ---------------------------------------------------------
 *
 * Suppose:
 *
 *      prefixSum[j] == prefixSum[i]
 *
 * Then:
 *
 *      sum(i + 1 ... j)
 *          = prefixSum[j] - prefixSum[i]
 *          = 0
 *
 * Therefore, the subarray between i and j
 * contains equal numbers of 0s and 1s.
 *
 *
 * So we need to find:
 *
 *      SAME PREFIX SUM
 *             +
 *      MAXIMUM DISTANCE BETWEEN THEIR INDICES
 *
 *
 * ---------------------------------------------------------
 * WHY HASHMAP?
 * ---------------------------------------------------------
 *
 * Store:
 *
 *      prefixSum -> FIRST index where we saw it
 *
 * Example:
 *
 *      prefixSum = 2
 *      first seen at index 3
 *
 *      map.put(2, 3)
 *
 * If we later see prefixSum = 2 at index 10:
 *
 *      subarray length = 10 - 3 = 7
 *
 * Why store the FIRST occurrence?
 *
 * Because we want the LONGEST subarray.
 *
 * If prefixSum was seen at:
 *
 *      index 3
 *      index 7
 *      index 10
 *
 * and we are currently at index 15,
 *
 * using index 3 gives:
 *
 *      15 - 3 = 12
 *
 * which is longer than:
 *
 *      15 - 7 = 8
 *      15 - 10 = 5
 *
 * Therefore:
 *
 *      NEVER overwrite an existing prefix sum index.
 *
 *
 * ---------------------------------------------------------
 * IMPORTANT INITIALIZATION
 * ---------------------------------------------------------
 *
 *      map.put(0, -1);
 *
 * This represents:
 *
 *      "Before the array starts, prefix sum = 0."
 *
 * Why is this necessary?
 *
 * Consider:
 *
 *      nums = [0, 1]
 *
 * Converted:
 *
 *      [-1, +1]
 *
 * Prefix sums:
 *
 *      index -1 -> 0
 *      index  0 -> -1
 *      index  1 -> 0
 *
 * At index 1, prefixSum becomes 0 again.
 *
 * The previous index of prefixSum 0 is -1.
 *
 * Therefore:
 *
 *      length = 1 - (-1)
 *             = 2
 *
 * So [0, 1] is correctly identified as
 * a valid subarray of length 2.
 *
 *
 * ---------------------------------------------------------
 * IMPORTANT INTERVIEW PATTERN
 * ---------------------------------------------------------
 *
 * Whenever you see:
 *
 *      "longest subarray"
 *      "equal number of X and Y"
 *
 * Think:
 *
 *      1. Give X and Y opposite values.
 *      2. Convert the condition into SUM = 0.
 *      3. Use prefix sum.
 *      4. Same prefix sum means zero-sum subarray.
 *      5. Store the earliest index.
 *
 *
 * ---------------------------------------------------------
 * TIME COMPLEXITY
 * ---------------------------------------------------------
 *
 * We scan the array once.
 *
 * HashMap operations are O(1) average.
 *
 * Time:  O(n)
 *
 *
 * ---------------------------------------------------------
 * SPACE COMPLEXITY
 * ---------------------------------------------------------
 *
 * In the worst case, we may store O(n) different
 * prefix sums in the HashMap.
 *
 * Space: O(n)
 *
 */
public class Solution {

    public int findMaxLength(int[] nums) {

        /*
         * prefixSum -> earliest index where this prefix sum occurred
         *
         * Example:
         *
         *     prefixSum = 0 -> -1
         *     prefixSum = 1 -> 2
         *     prefixSum = -1 -> 0
         */
        Map<Integer, Integer> firstOccurrence = new HashMap<>();

        /*
         * Virtual position before the array starts.
         *
         * We say:
         *
         *     prefixSum = 0 at index -1
         *
         * This allows us to correctly calculate the length
         * of a valid subarray starting at index 0.
         */
        firstOccurrence.put(0, -1);

        int prefixSum = 0;
        int maxLength = 0;

        for (int i = 0; i < nums.length; i++) {

            /*
             * Convert the problem:
             *
             *     1 -> +1
             *     0 -> -1
             *
             * Why?
             *
             * Suppose a subarray has:
             *
             *     3 ones
             *     3 zeros
             *
             * Its transformed sum is:
             *
             *     (+1 +1 +1) + (-1 -1 -1)
             *     = 0
             *
             * Therefore:
             *
             *     sum == 0
             *          ⇔
             *     equal number of 0s and 1s
             */
            prefixSum += (nums[i] == 1) ? 1 : -1;

            /*
             * Have we seen this prefix sum before?
             *
             * If yes:
             *
             *     current prefixSum
             *          =
             *     previous prefixSum
             *
             * Therefore the sum between the two positions is 0.
             *
             * Hence that subarray contains equal 0s and 1s.
             */
            if (firstOccurrence.containsKey(prefixSum)) {

                int previousIndex = firstOccurrence.get(prefixSum);

                /*
                 * The subarray starts immediately after previousIndex
                 * and ends at i.
                 *
                 * Length:
                 *
                 *     i - previousIndex
                 *
                 * Example:
                 *
                 *     previousIndex = 2
                 *     currentIndex  = 7
                 *
                 *     length = 7 - 2 = 5
                 */
                int currentLength = i - previousIndex;

                /*
                 * We are looking for the LONGEST valid subarray,
                 * so keep the maximum length found so far.
                 */
                maxLength = Math.max(maxLength, currentLength);

            } else {

                /*
                 * This is the FIRST time we have seen this prefix sum.
                 *
                 * Store this index.
                 *
                 * IMPORTANT:
                 *
                 * We do NOT update an existing prefix sum.
                 *
                 * The earliest index gives us the maximum possible
                 * subarray length when this prefix sum appears again.
                 */
                firstOccurrence.put(prefixSum, i);
            }
        }

        return maxLength;
    }
}

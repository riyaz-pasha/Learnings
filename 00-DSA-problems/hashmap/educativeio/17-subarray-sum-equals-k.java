/**
 * ============================================================================
 * L E E T C O D E   5 6 0 :   S U B A R R A Y   S U M   E Q U A L S   K
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Can the array contain negative numbers?"
 *    (Crucial! If all numbers are positive, we could use an O(1) space Sliding Window. Since negatives exist, the window sum can shrink when growing, breaking the monotonic property. Sliding window is completely disqualified.)
 * 2. "Can 'k' be zero or negative?"
 *    (Yes, which means we are literally looking for sub-segments that net to 0 or a negative value).
 * 3. "Is it possible for the sum to overflow a standard 32-bit integer?"
 *    (Constraints say max length 20,000 and max value 1,000. Max sum is 20,000,000, which easily fits in a standard integer. No `long` needed for the running sum).
 * 4. "Do we just need the count, or the actual starting/ending indices of the subarrays?"
 *    (Returning just the count means we only need to track 'how many times' we've seen a state, not 'where' we saw it).
 *
 *
 * ## 2. The reasoning journey
 * > Core Constraint: We need to count contiguous blocks of numbers that sum to exactly `k`.
 * > The bottleneck is that checking every single contiguous block takes too long for an array of size 20,000.
 *
 * ### Approach 1: Running Sum Brute Force
 * 1. What I'd naturally try: For every starting index `i`, I'll start a running sum. I'll add elements one by 
 *    one (moving `j` to the right). Every time my running sum hits `k`, I increment a counter.
 * 2. Why it works: It exhaustively tests every possible contiguous subarray.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N^2) — because for each of the N starting positions, we scan to the end of the array. 
 *      For N=20,000, this is 400,000,000 operations, which will likely Time Limit Exceed (TLE) in an interview.
 *    - Space Complexity: O(1) auxiliary — because we only need an integer counter and a running sum variable.
 * 4. What work is being repeated: We are constantly recalculating the sums of the exact same overlapping sub-segments.
 * 5. What property removes the bottleneck: The algebraic definition of a subarray sum. A subarray from index `i` 
 *    to `j` is just the sum from `0` to `j` MINUS the sum from `0` to `i-1`.
 *
 * ### Approach 2: Prefix Sum Array (The Intermediate Step)
 * 1. What I'd naturally try: Build a `prefix[]` array where `prefix[x]` is the sum of everything up to index `x`. 
 *    Then, use two loops to check all pairs `(i, j)` to see if `prefix[j] - prefix[i-1] == k`.
 * 2. Why it works: It formalizes the "range sum as a difference of prefixes" concept.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N^2) — still comparing all pairs.
 *    - Space Complexity: O(N) — storing the prefix sums.
 * 4. What work is being repeated: We are linearly searching backward through the prefix array looking for a 
 *    specific value.
 *    Algebraic trick: We are checking if `Prefix[j] - Prefix[i] == k`.
 *    Rearrange it: `Prefix[j] - k == Prefix[i]`.
 *    This means at index `j`, we don't need to check all previous indices! We just need to know *how many times* 
 *    the value `(Prefix[j] - k)` occurred in the past!
 * 5. What property removes the bottleneck: A Frequency HashMap. It transforms a linear O(N) backward search 
 *    into an instant O(1) lookup.
 *
 * ### Approach 3: Prefix Sum + HashMap (The Algorithmic Optimum)
 * 1. What I'd naturally try: Keep a running prefix sum. At each element, calculate `sum - k`. If this value 
 *    exists in our HashMap, it means there is a valid prefix we can chop off to leave exactly `k`. Add the 
 *    frequency of that past prefix to our counter. Then, log our current running sum into the map.
 * 2. Why it works: It elegantly turns a range query problem into a "have I seen this specific target before?" problem.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we traverse the array exactly once, and HashMap `put`/`get` are O(1).
 *    - Space Complexity: O(N) — because in the worst case (all positive numbers), every prefix sum is unique, 
 *      resulting in N entries in the map.
 *
 * > **Interview Strategy:** Start by warning about the "Sliding Window Trap" (Pitfall section below). Proving you 
 * > know *why* sliding window fails shows deep algorithmic maturity. Then, write out the algebra: 
 * > `CurrentSum - OldSum = k  ==>  OldSum = CurrentSum - k`. Write Approach 3 based on this math.
 *
 *
 * ## 3. Edge cases
 * - `k = 0`: The map perfectly tracks when `runningSum - 0 == runningSum`.
 * - Negative numbers netting to 0: `[1, -1, 1, -1]`, `k=0`. The map frequency values will increment properly.
 * - Subarray starts exactly at index 0: The most critical edge case. (e.g. `nums = [3, 4]`, `k = 7`). When we 
 *   process `4`, `runningSum` is 7. `7 - 7 = 0`. We need to chop off a prefix of sum `0`. If `0` isn't in the map, 
 *   we miss this valid subarray! We MUST initialize the map with `{0: 1}`.
 *
 *
 * ## 4. Key Insight, Diagrams & Pitfalls
 *
 * **Key Insight: "The Prefix Chop"**
 * If the current running sum is 14, and we want a subarray sum of 8, we just need to chop off a previous segment 
 * that summed to 6. How many times have we seen a running sum of 6? That's exactly how many valid subarrays end here!
 *
 * **ASCII Diagram:**
 * Array:     [ 3,  4,  7,  2, -3,  1,  4,  2 ]     Target (k) = 7
 * PrefixSum:   3   7  14  16  13  14  18  20
 * 
 * Look at index 5 (val=1, PrefixSum=14). We want k=7.
 * Do we have any past prefixes equal to (14 - 7) = 7?
 * Yes! At index 1, the PrefixSum was 7. 
 * Therefore, chopping off everything up to index 1 leaves the subarray [7, 2, -3, 1], which equals 7!
 *
 * **Pitfall: The Sliding Window Trap**
 * If an interviewer asks this, 80% of candidates immediately try a Sliding Window (expanding right, shrinking left).
 * Sliding window ONLY works when the array is monotonic (all positive numbers). If you have negatives (e.g., `-3`), 
 * adding a number might *shrink* your window sum, and removing a number might *grow* it. You lose the logic of 
 * when to expand/shrink.
 *
 * **Dry Run of Approach 3:**
 * `nums = [1, 2, 3, -3, 3]`, `k = 3`
 * Initial State: map = `{0: 1}`, count = 0, sum = 0
 * 
 * | idx | val | sum | target (sum - k) | In Map? (+count) | Map Update |
 * |-----|-----|-----|------------------|------------------|------------|
 * |  0  |  1  |  1  | 1 - 3 = -2       | No (0)           | put(1, 1)  |
 * |  1  |  2  |  3  | 3 - 3 = 0        | YES (1) -> cnt=1 | put(3, 1)  |
 * |  2  |  3  |  6  | 6 - 3 = 3        | YES (1) -> cnt=2 | put(6, 1)  |
 * |  3  | -3  |  3  | 3 - 3 = 0        | YES (1) -> cnt=3 | put(3, 2)  |
 * |  4  |  3  |  6  | 6 - 3 = 3        | YES (2) -> cnt=5 | put(6, 2)  |
 * Result: 5 valid subarrays.
 *
 *
 * ## 5. Follow-ups
 * - **Q:** What if the array only contained non-negative numbers (0 and positive)?
 *   **A:** I would ditch the O(N) space HashMap and use the Sliding Window technique. It would run in O(N) time 
 *   and strictly O(1) space.
 * - **Q:** What if we wanted to find the maximum *length* of a subarray summing to `k`?
 *   **A:** Instead of storing the *frequency* of the prefix sum in the HashMap, we store the *earliest index* 
 *   where we saw that sum. When we find `sum - k`, we do `maxLength = Math.max(maxLength, currentIndex - map.get(sum - k))`.
 * - **Q:** What if we wanted the number of subarrays divisible by `k`?
 *   **A:** We use the exact same HashMap structure, but instead of tracking running sums, we track the frequencies 
 *   of `(runningSum % k)`. If we see the exact same modulo again, the segment between them is divisible by `k`.
 *
 * ============================================================================
 */

import java.util.HashMap;
import java.util.Map;

public class SubarraySumEqualsKStudy {

    interface SubarraySumSolver {
        int subarraySum(int[] nums, int k);
    }

    // ========================================================================
    // APPROACH 3: PREFIX SUM + HASHMAP (The Optimal Interview Solution)
    // ========================================================================
    static class OptimalMapSolver implements SubarraySumSolver {
        @Override
        public int subarraySum(int[] nums, int k) {
            // Tracks the frequency of each prefix sum we've encountered.
            // Key: Prefix Sum. Value: How many times we've seen it.
            Map<Integer, Integer> prefixCounts = new HashMap<>();
            
            // CRITICAL BASE CASE:
            // Represents the empty prefix sum before the array starts.
            // If the running sum itself equals k, we need to chop off a prefix of 0.
            // By initializing {0: 1}, we successfully count subarrays starting at index 0.
            prefixCounts.put(0, 1);
            
            int count = 0;
            int runningSum = 0;
            
            for (int num : nums) {
                runningSum += num;
                
                // What prefix sum do we need to chop off to get exactly 'k'?
                int targetPrefix = runningSum - k;
                
                // If we have seen this prefix sum in the past, chopping it off leaves
                // a valid subarray ending exactly at the current element.
                // We add the NUMBER OF TIMES we've seen it, because each occurrence
                // represents a distinct starting point for a valid subarray.
                if (prefixCounts.containsKey(targetPrefix)) {
                    count += prefixCounts.get(targetPrefix);
                }
                
                // Finally, record the current running sum into our map so future
                // elements can chop off the prefix ending at the current element.
                prefixCounts.put(runningSum, prefixCounts.getOrDefault(runningSum, 0) + 1);
            }
            
            return count;
        }
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (Conceptual Baseline)
    // ========================================================================
    static class BruteForceSolver implements SubarraySumSolver {
        @Override
        public int subarraySum(int[] nums, int k) {
            int count = 0;
            
            // Fix the starting point of the subarray
            for (int start = 0; start < nums.length; start++) {
                int runningSum = 0;
                
                // Expand the subarray to the right
                for (int end = start; end < nums.length; end++) {
                    runningSum += nums[end];
                    
                    // If the segment matches our target, count it
                    if (runningSum == k) {
                        count++;
                    }
                }
            }
            
            return count;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        SubarraySumSolver[] solvers = {
            new OptimalMapSolver(),
            new BruteForceSolver()
        };

        // Format: {nums array}, k, expected_count
        int[][][] testCases = {
            {{1, 1, 1}, {2}, {2}},                     // Standard case
            {{1, 2, 3}, {3}, {2}},                     // [1,2] and [3]
            {{1, 2, 3, -3, 3}, {3}, {5}},              // Mixed negatives
            {{-1, -1, 1}, {0}, {1}},                   // Zero sum target
            {{3, 4, 7, 2, -3, 1, 4, 2}, {7}, {4}},     // Complex interleaving
            {{1}, {0}, {0}},                           // No valid subarrays
            {{0, 0, 0, 0, 0}, {0}, {15}}               // Combinatorics blast (All possible subarrays valid)
        };

        for (SubarraySumSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                int[] nums = testCases[i][0];
                int k = testCases[i][1][0];
                int expected = testCases[i][2][0];
                
                int result = solver.subarraySum(nums, k);
                
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
 * - Core pattern: Prefix Sum Tracking using a Frequency HashMap.
 * - Key observation: A subarray sum is mathematically the difference between two 
 *   prefix sums (`sum(j) - sum(i-1) = k`). This rearranges to `sum(i-1) = sum(j) - k`.
 *   This equation transforms an O(N^2) range search into an O(N) backward lookup.
 * - What's worth memorizing: `map.put(0, 1)`. If you forget this initialization, 
 *   you will fail to count any valid subarray that begins at index 0.
 * - Most common trap: Trying to use a Sliding Window (Two Pointers) approach. 
 *   Sliding window strictly requires monotonically increasing sums (no negative numbers).
 * - Mental trigger: "Number of contiguous subarrays matching X? Prefix Sum + Map."
 * ============================================================================
 */

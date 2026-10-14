/**
 * STANDALONE STUDY NOTE: ZERO ARRAY TRANSFORMATION I
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can we choose any subset of indices within [l_i, r_i] independently for each query?" (Yes, confirming we can selectively decrement elements that still need reduction).
 * - "Do all queries have to be processed sequentially in the given order?" (Yes, constraints specify processing queries sequentially, but since range decrements commute in total effect, we can aggregate overall coverage).
 * - "What are the maximum constraints for nums length and queries length?" (Both up to 10^5, which means an O(N * Q) brute force approach will time out and we need an O(N + Q) solution).
 * - "Can elements in nums be 0 initially?" (Yes, 0 <= nums[i] <= 10^5, meaning elements already at 0 require 0 decrements).
 * - "What should be returned if nums can be successfully transformed into all zeros?" (Return true; otherwise false).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to determine if a series of range decrement queries can reduce every element in `nums` to 0. 
 * The binding constraint is that updating array values individually for every query takes O(N * Q) time, 
 * which fails at scale when N, Q <= 10^5.
 * 
 * --- APPROACH 1: BRUTE FORCE (Direct Range Simulation) ---
 * 1. What I'd naturally try: Loop through each query [l, r], and for each query, iterate through every index 
 *    from l to r in `nums` to decrement its value by 1. After all queries, check if all elements are 0.
 * 2. Why it works: It perfectly simulates the exact operations described in the problem statement.
 * 3. Why it's too slow/costly: If Q queries each span length N, the time complexity is O(Q * N), leading to 
 *    up to 10^10 operations, which guarantees a Time Limit Exceeded (TLE).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(Q * N) — because each query triggers a linear scan over up to N elements.
 *    - Space Complexity: O(1) auxiliary space if modified in place.
 * 
 * --- APPROACH 2: INTERMEDIATE (Prefix Sum Array of Query Frequencies) ---
 * 1. The Observation: We don't need to actually decrement values during each query. We only care about the *total number 
 *    of times* each index is covered by a query range. If an index `i` is covered by `k` queries, it can be decremented 
 *    at most `k` times. Thus, if `nums[i] <= k` for all `i`, a Zero Array is possible.
 * 2. How it leads to the next step: Instead of updating every index in `[l, r]` with a loop, we can use a *Difference Array* 
 *    (or Delta Array) to record range start (`+1` at `l`) and range end (`-1` at `r + 1`) in O(1) time per query.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + Q) — O(Q) to populate the difference array and O(N) to compute prefix sums.
 *    - Space Complexity: O(N) — for the difference array of size N + 1.
 * 
 * --- APPROACH 3: OPTIMAL (Difference Array + Prefix Sum Verification) ---
 * 1. The Observation: A difference array `diff` of size `n + 1` allows us to apply range increments in O(1) time. 
 *    For each query `[l, r]`, we do `diff[l]++` and `diff[r + 1]--`. Then, a single prefix sum pass transforms `diff` 
 *    into the exact coverage count for each index.
 * 2. Which one I'd write in an interview: Approach 3 (Difference Array). It is the canonical pattern for offline 
 *    range-update queries, running in linear O(N + Q) time and O(N) space.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + Q) — where Q is the number of queries and N is the length of `nums`. Blazing fast.
 *    - Space Complexity: O(N) — auxiliary space for the difference array `diff` of size `n + 1`.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - All elements in nums are already 0: Returns true instantly.
 * - Single element array: `nums = [5], queries = [[0, 0], [0, 0], ...]` -> Correctly accumulates counts.
 * - Insufficient coverage: `nums = [5, 0, 0], queries = [[0, 0]]` -> Position 0 needs 5 decrements but has only 1 query coverage -> Returns false.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * The Difference Array (Delta Array) Technique:
 * 
 * Queries: [[0, 2], [1, 3]] on length n = 4
 * 
 * Step 1: Mark range boundaries in `diff` array (size n + 1 = 5)
 * - Query [0, 2]: diff[0]++, diff[2+1]--  --> diff = [1, 0, 0, -1, 0]
 * - Query [1, 3]: diff[1]++, diff[3+1]--  --> diff = [1, 1, 0, -1, -1]
 * 
 * Step 2: Running prefix sum to get actual coverage per index:
 * Index 0: sum = 1
 * Index 1: sum = 1 + 1 = 2
 * Index 2: sum = 2 + 0 = 2
 * Index 3: sum = 2 + (-1) = 1
 * 
 * Coverage Array: [1, 2, 2, 1]
 * Compare against `nums[i]`: If `nums[i] <= coverage[i]` for all i, return true!
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [1, 0, 1], queries = [[0, 2]]
 * 
 * - n = 3
 * - diff = new int[4] -> [0, 0, 0, 0]
 * - Process query [0, 2]:
 *   - diff[0]++ -> diff[0] = 1
 *   - diff[2 + 1]-- -> diff[3] = -1
 *   - diff becomes [1, 0, 0, -1]
 * - Compute prefix sum & verify against nums ([1, 0, 1]):
 *   - i = 0: currentCoverage = 0 + 1 = 1. nums[0] (1) <= 1? Yes.
 *   - i = 1: currentCoverage = 1 + 0 = 1. nums[1] (0) <= 1? Yes.
 *   - i = 2: currentCoverage = 1 + 0 = 1. nums[2] (1) <= 1? Yes.
 * - Loop finishes successfully. Return true.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "Instead of simulating each query with a nested loop which would take O(Q * N) time and time out, 
 * we can use a Difference Array to track range coverages in O(1) time per query. For each query `[l, r]`, 
 * we mark the start of coverage by incrementing `diff[l]` by 1, and the end by decrementing `diff[r + 1]` by 1. 
 * After processing all queries, we compute the prefix sums of the difference array to find the exact number 
 * of queries covering each index. If every element in `nums` is less than or equal to its total query coverage, 
 * we return true; otherwise, false. This gives us an optimal O(N + Q) time complexity and O(N) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if each query also had a specific decrement weight `val` instead of always decrementing by 1?
 * A: We would simply add `val` to `diff[l]` and subtract `val` from `diff[r + 1]`, keeping the exact same 
 *    difference array structure (this is Zero Array Transformation II).
 * 
 * Q: What if we needed to find the *minimum* number of queries required to transform nums into a Zero Array?
 * A: We could binary search on the number of queries `k` from `0` to `Q`, using our difference array feasibility 
 *    check function as the predicate, taking O((N + Q) log Q) time.
 */

public class ZeroArrayTransformationI {

    /**
     * APPROACH 3: OPTIMAL (Difference Array / Prefix Sum Technique)
     * Time: O(N + Q), Space: O(N)
     */
    public boolean isZeroArray(int[] nums, int[][] queries) {
        int n = nums.length;
        
        // [Why needed]: Difference array of size n + 1 to handle range updates in O(1) time.
        // Size n + 1 prevents ArrayIndexOutOfBounds when marking r + 1 for queries ending at n - 1.
        int[] diff = new int[n + 1];

        // Phase 1: Record query range boundaries in the difference array
        for (int[] query : queries) {
            int l = query[0];
            int r = query[1];

            // [What it does]: +1 at index 'l' marks the start of query coverage.
            diff[l]++;
            // -1 at index 'r + 1' marks the termination of query coverage.
            diff[r + 1]--;
        }

        // Phase 2: Compute prefix sums to determine the actual total coverage count for each index
        int currentCoverage = 0;
        for (int i = 0; i < n; i++) {
            currentCoverage += diff[i];

            // [Why needed]: If the number of queries covering index i is less than nums[i], 
            // we do not have enough reduction capacity to bring this element down to zero.
            if (nums[i] > currentCoverage) {
                return false;
            }
        }

        return true;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Difference Array (Delta Array) + Prefix Sum accumulation.
     * 2. Key observation: Range updates can be recorded in O(1) time using `diff[l]++` and `diff[r + 1]--`, 
     *    then expanded into full point values via prefix sums.
     * 3. Memorize vs Understand: Understand why `diff` array size must be `n + 1` to safely accommodate `r + 1`.
     * 4. Most common trap: Forgetting to check bounds or failing to recognize that range increments commute, 
     *    allowing offline precomputation.
     * 5. Mental trigger: "Range increment queries + array reduction" -> Difference array of size `n + 1` + Prefix sum verification.
     */
    public static void main(String[] args) {
        ZeroArrayTransformationI solution = new ZeroArrayTransformationI();

        // Test Case 1: Standard Success Case
        int[] nums1 = {1, 0, 1};
        int[][] queries1 = {{0, 2}};
        System.out.println("Test 1 (Standard): " + solution.isZeroArray(nums1, queries1)); // Expected: true

        // Test Case 2: Standard Failure Case
        int[] nums2 = {4, 3, 2, 1};
        int[][] queries2 = {{1, 3}, {0, 2}};
        System.out.println("Test 2 (Insufficient coverage): " + solution.isZeroArray(nums2, queries2)); // Expected: false
    }
}



class Solution {
    public boolean isZeroArray(int[] nums, int[][] queries) {
        int n = nums.length;

        // IDEA:
        // We don't need to simulate the decrements.
        // For each index i, we only need to know how many queries cover it.
        //
        // If coverage[i] >= nums[i] for every index, the answer is true.
        //
        // A difference array lets us count range coverage efficiently:
        // each query contributes only two boundary updates.

        // n + 1 gives us a safe position at r + 1,
        // even when a query ends at the last array index (n - 1).
        int[] diff = new int[n + 1];

        // STEP 1: Record the boundaries of every query.
        for (int[] query : queries) {
            int l = query[0];
            int r = query[1];

            // Coverage starts increasing at index l.
            diff[l]++;

            // Coverage stops increasing after index r.
            // The decrement at r + 1 cancels this query's contribution
            // when we later calculate prefix sums.
            diff[r + 1]--;
        }

        // STEP 2: Reconstruct coverage using a prefix sum.
        // This variable represents the number of queries covering i.
        int availableDecrements = 0;

        for (int i = 0; i < n; i++) {
            // Apply the coverage change at the current index.
            availableDecrements += diff[i];

            // Each query covering i can decrement nums[i] once.
            // If too few queries cover this index, it cannot reach zero.
            if (availableDecrements < nums[i]) {
                return false;
            }
        }

        // Every element has enough available decrements to become zero.
        return true;
    }
}



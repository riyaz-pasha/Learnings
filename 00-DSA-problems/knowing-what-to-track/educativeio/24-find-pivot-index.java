/**
 * STANDALONE STUDY NOTE: FIND PIVOT INDEX (FIND THE MIDDLE INDEX IN ARRAY)
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "What should be returned if multiple pivot indices exist?" (The prompt states to return the *leftmost* pivot index).
 * - "What if no pivot index exists in the array?" (Return -1, as specified in the prompt).
 * - "What is the left sum if the pivot is at index 0?" (0, since no elements exist to the left of the left edge).
 * - "Can numbers in the array be negative?" (Yes, constraints specify `nums[i]` can range from -1000 to 1000, 
 *   meaning prefix sums are not monotonically increasing).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find an index where the sum of elements strictly to its left equals the sum of elements strictly to its right. 
 * The binding constraint is that recomputing the left sum and right sum from scratch for every single index takes 
 * O(N^2) time, which can be optimized down to linear O(N) time using prefix/suffix sum maintenance.
 * 
 * --- APPROACH 1: BRUTE FORCE (Recomputing Sums per Index) ---
 * 1. What I'd naturally try: For every index `i`, iterate through all elements from `0` to `i - 1` to compute the left sum, 
 *    and from `i + 1` to `n - 1` to compute the right sum, then check if they are equal.
 * 2. Why it works: It exhaustively checks every index against its left and right split sums.
 * 3. Why it's too slow/costly: Re-scanning subarrays for every index results in a quadratic time complexity of O(N^2).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^2) — because for each of the N indices, we sum up to N elements.
 *    - Space Complexity: O(1) — constant extra space.
 * 
 * --- APPROACH 2: INTERMEDIATE (Total Sum & Rolling Prefix Sum) ---
 * 1. The Observation: We don't need to recompute sums from scratch. If we compute the *total sum* of the entire array 
 *    up front, then as we iterate through each index `i`:
 *    - The `rightSum` can be calculated instantly as: `totalSum - leftSum - nums[i]`.
 *    - We check if `leftSum == rightSum`. If so, `i` is our pivot!
 *    - Then, we add `nums[i]` to `leftSum` and move to the next index.
 * 2. How it leads to the next step: This reduces the check at each index to O(1) time after an initial O(N) pass 
 *    to find the total sum.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — two linear passes (one to sum the array, one to find the pivot).
 *    - Space Complexity: O(1) — because we only use scalar integer variables (`totalSum`, `leftSum`).
 * 
 * --- APPROACH 3: OPTIMAL (Single-Pass Total Sum & Running Left/Right Balance) ---
 * 1. The Observation: We can combine the total sum computation and the pivot search into a single pass if we want, 
 *    or stick to the classic two-pass approach (Sum array first, then check). The two-pass approach is exceptionally 
 *    clean, intuitive, and eliminates off-by-one errors.
 * 2. Which one I'd write in an interview: Approach 2 / 3 (Total Sum + Rolling Left Sum). It runs in O(N) time and O(1) space, 
 *    handling negative numbers seamlessly.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — where N is the length of `nums`. Pass 1 computes total sum O(N); Pass 2 finds pivot O(N).
 *    - Space Complexity: O(1) — auxiliary space is constant.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Pivot at index 0: `nums = [1, -1, 0]` -> Left sum is 0, right sum is `-1 + 0 = -1`? Wait, for `nums = [1, -1, 0]`:
 *   - Total sum = 0.
 *   - i = 0: leftSum = 0, rightSum = 0 - 0 - 1 = -1 (Not equal).
 *   - i = 1: leftSum = 1, rightSum = 0 - 1 - (-1) = 0 (Not equal).
 *   - i = 2: leftSum = 1 + (-1) = 0, rightSum = 0 - 0 - 0 = 0 -> EQUAL! Returns 2.
 * - Pivot at left edge: `nums = [0, 1, -1]` -> i = 0 has leftSum = 0, rightSum = 0 -> Returns 0.
 * - No pivot exists: `nums = [1, 2, 3]` -> Returns -1.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Total Sum Balance Principle:
 * 
 * Array: [1, 7, 3, 6, 5, 6]  (Total Sum = 28)
 * 
 * i = 0 (nums[0] = 1):
 * - leftSum = 0
 * - rightSum = totalSum (28) - leftSum (0) - nums[0] (1) = 27
 * - 0 == 27? No. leftSum becomes 0 + 1 = 1.
 * 
 * i = 3 (nums[3] = 6):
 * - leftSum = 1 + 7 + 3 = 11
 * - rightSum = 28 - 11 - 6 = 11
 * - 11 == 11? YES! Pivot found at index 3.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [1, 7, 3, 6, 5, 6]
 * 
 * - Step 1: Compute totalSum = 1 + 7 + 3 + 6 + 5 + 6 = 28
 * - Step 2: Initialize leftSum = 0
 * - Step 3: Iterate `i` from 0 to 5:
 *   - i = 0 (val = 1):
 *     - rightSum = 28 - 0 - 1 = 27. leftSum (0) == rightSum (27)? False.
 *     - leftSum += 1 -> leftSum = 1
 *   - i = 1 (val = 7):
 *     - rightSum = 28 - 1 - 7 = 20. leftSum (1) == rightSum (20)? False.
 *     - leftSum += 7 -> leftSum = 8
 *   - i = 2 (val = 3):
 *     - rightSum = 28 - 8 - 3 = 17. leftSum (8) == rightSum (17)? False.
 *     - leftSum += 3 -> leftSum = 11
 *   - i = 3 (val = 6):
 *     - rightSum = 28 - 11 - 6 = 11. leftSum (11) == rightSum (11)? TRUE!
 *     - Return index 3.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To find the pivot index efficiently without recomputing sums from scratch for every index, we can first compute 
 * the total sum of all elements in the array. Then, we iterate through the array while maintaining a running `leftSum`. 
 * At each index `i`, the sum of elements strictly to the right can be derived instantly as `totalSum - leftSum - nums[i]`. 
 * If `leftSum` equals `rightSum`, we immediately return `i` as our leftmost pivot index. If we finish the loop without 
 * finding a match, we return -1. This gives us O(N) time complexity and O(1) auxiliary space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the array is streamed over time, meaning you don't have the total sum up front?
 * A: If streamed, we would need to maintain prefix sums dynamically or buffer the stream, though standard array 
 *    inputs allow full pre-computation.
 */

public class FindPivotIndex {

    /**
     * APPROACH 2: OPTIMAL (Total Sum + Rolling Left Sum)
     * Time: O(N), Space: O(1)
     */
    public int pivotIndex(int[] nums) {
        int totalSum = 0;
        
        // Phase 1: Compute the total sum of all elements in the array
        for (int num : nums) {
            totalSum += num;
        }

        int leftSum = 0;

        // Phase 2: Iterate through each index and check if leftSum equals rightSum
        for (int i = 0; i < nums.length; i++) {
            // [What it does]: Calculate rightSum dynamically using totalSum and current leftSum
            int rightSum = totalSum - leftSum - nums[i];

            // [Why needed]: If leftSum equals rightSum, we found our leftmost pivot index.
            if (leftSum == rightSum) {
                return i;
            }

            // [What it does]: Add the current element to leftSum for the next iteration
            leftSum += nums[i];
        }

        // If no pivot index exists, return -1
        return -1;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Total sum precomputation + rolling left sum / derived right sum.
     * 2. Key observation: Right sum at index `i` is always `totalSum - leftSum - nums[i]`, avoiding O(N^2) subarray scans.
     * 3. Memorize vs Understand: Understand why `leftSum` is updated *after* the equality check (because elements strictly 
     *    to the left exclude `nums[i]`).
     * 4. Most common trap: Updating `leftSum` before checking equality, which would incorrectly include `nums[i]` in the left sum.
     * 5. Mental trigger: "Find pivot index where left sum equals right sum" -> Compute total sum + single loop with `totalSum - leftSum - nums[i]`.
     */
    public static void main(String[] args) {
        FindPivotIndex solution = new FindPivotIndex();

        // Test Case 1: Standard Example
        int[] nums1 = {1, 7, 3, 6, 5, 6};
        System.out.println("Test 1 (Standard): " + solution.pivotIndex(nums1)); // Expected: 3

        // Test Case 2: Pivot at Left Edge
        int[] nums2 = {1, -1, 0};
        System.out.println("Test 2 (Edge pivot): " + solution.pivotIndex(nums2)); // Expected: 2

        // Test Case 3: No Pivot Exists
        int[] nums3 = {1, 2, 3};
        System.out.println("Test 3 (No pivot): " + solution.pivotIndex(nums3)); // Expected: -1

        // Test Case 4: Single Element
        int[] nums4 = {5};
        System.out.println("Test 4 (Single element): " + solution.pivotIndex(nums4)); // Expected: 0
    }
}


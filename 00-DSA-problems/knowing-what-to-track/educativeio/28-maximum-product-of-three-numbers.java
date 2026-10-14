/**
 * STANDALONE STUDY NOTE: MAXIMUM PRODUCT OF THREE NUMBERS
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can the array contain negative numbers?" (Yes, constraints specify nums[i] ranges from -1000 to 1000, 
 *   which introduces the negative-product edge case where two large negative numbers multiply to a large positive value).
 * - "Is the array guaranteed to have at least 3 elements?" (Yes, constraints state 3 <= nums.length <= 10^4).
 * - "Should we return an integer representing the maximum product?" (Yes).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find three elements in an array whose product is maximized. The binding constraint is that 
 * simply picking the three largest numbers fails when there are large negative numbers present (since two negatives 
 * multiply to a positive).
 * 
 * --- APPROACH 1: BRUTE FORCE (Three Nested Loops) ---
 * 1. What I'd naturally try: Use three nested loops to check every possible triplet combination `(i, j, k)` 
 *    and track the maximum product found.
 * 2. Why it works: It exhaustively examines every triplet.
 * 3. Why it's too slow/costly: Checking all triplets takes O(N^3) time, which will time out for N = 10^4.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^3) — cubic combinations loop.
 *    - Space Complexity: O(1) — constant extra space.
 * 
 * --- APPROACH 2: INTERMEDIATE (Sorting Based Case Analysis) ---
 * 1. The Observation: After sorting the array in ascending order:
 *    - The largest numbers are always at the very end (`nums[n-1]`, `nums[n-2]`, `nums[n-3]`).
 *    - The most negative numbers are at the very beginning (`nums[0]`, `nums[1]`). 
 *    Therefore, the maximum product can *only* come from two scenarios:
 *    1. The product of the three largest numbers: `nums[n-1] * nums[n-2] * nums[n-3]` (standard case for positives/mixed).
 *    2. The product of the two smallest (most negative) numbers and the single largest number: `nums[0] * nums[1] * nums[n-1]` 
 *       (handles cases where two large negatives multiply to a massive positive number).
 * 2. How it leads to the next step: Sort the array, compute both candidates, and return the maximum of the two.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N log N) — dominated by the sorting step.
 *    - Space Complexity: O(log N) or O(1) auxiliary space depending on the sorting algorithm implementation.
 * 
 * --- APPROACH 3: OPTIMAL (Single-Pass Linear Scan for Extremes) ---
 * 1. The Observation: We don't actually need to sort the whole array. We only care about the top 3 largest 
 *    elements and the bottom 2 smallest elements. We can track these 5 values in a single linear pass.
 * 2. Which one I'd write in an interview: Approach 2 (Sorting) is exceptionally clean, concise, and runs in a fraction 
 *    of a millisecond for N <= 10^4. Approach 3 (Single-Pass) is the ultimate O(N) optimization if streaming or performance 
 *    is heavily constrained. Let's provide the clean sorting-based approach as primary, with single-pass logic explained.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - All negative numbers: `nums = [-1, -2, -3]` -> Product of three largest (which are the least negative, e.g., -1 * -2 * -3 = -6).
 * - Mixed numbers with large negatives: `nums = [-100, -98, 1, 2, 3]` -> `-100 * -98 * 3 = 29400` wins over `1 * 2 * 3 = 6`.
 * - All positive numbers: `nums = [1, 2, 3, 4]` -> `2 * 3 * 4 = 24` wins over `1 * 2 * 4 = 8`.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * The Two Candidate Scenarios after Sorting:
 * 
 * Sorted Array: [ -100, -98,   1, 2, 3,   10 ]
 *                 ^     ^              ^  ^  ^
 *               nums[0] nums[1]      n-3 n-2 n-1
 * 
 * Candidate A (Three largest):  `nums[n-1] * nums[n-2] * nums[n-3]` = 3 * 2 * 10 = 60
 * Candidate B (Two smallest + largest): `nums[0] * nums[1] * nums[n-1]` = (-100) * (-98) * 10 = 98,000
 * 
 * Return max(Candidate A, Candidate B) -> 98,000!
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [-4, -3, -2, 1, 5]
 * 
 * - Step 1: Sort array -> `[-4, -3, -2, 1, 5]`
 * - Step 2: Calculate Candidate A (three largest):
 *   `a = nums[4] * nums[3] * nums[2] = 5 * 1 * (-2) = -10`
 * - Step 3: Calculate Candidate B (two smallest * largest):
 *   `b = nums[0] * nums[1] * nums[4] = (-4) * (-3) * 5 = 60`
 * - Step 4: Return `max(-10, 60)` -> `60`.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "A naive approach would look at the three largest numbers, but that fails when there are large negative numbers 
 * whose product becomes a massive positive number. To handle both cases correctly, we can sort the array in ascending 
 * order. The maximum product will always be the maximum between two possibilities: either the product of the three 
 * largest numbers at the end of the array, or the product of the two smallest negative numbers at the beginning 
 * combined with the single largest positive number at the end. This gives us O(N log N) time complexity and O(1) space, 
 * or O(N) if we track the extremes in a single linear pass."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: Can you solve this in O(N) time without sorting?
 * A: Yes! We can loop through the array once and maintain variables for the 3 largest numbers (`mx1, mx2, mx3`) 
 *    and the 2 smallest numbers (`mi1, mi2`). Then return `max(mi1 * mi2 * mx1, mx1 * mx2 * mx3)`.
 */

import java.util.Arrays;

public class MaximumProductOfThreeNumbers {

    /**
     * APPROACH 2: OPTIMAL (Sorting + Case Analysis)
     * Time: O(N log N), Space: O(log N) auxiliary sorting stack space
     */
    public int maximumProduct(int[] nums) {
        // [Why needed]: Sorting allows us to instantly access the smallest and largest elements.
        Arrays.sort(nums);
        
        int n = nums.length;

        // [What it does]: Candidate A: Product of the three largest numbers at the end of the array
        int optionA = nums[n - 1] * nums[n - 2] * nums[n - 3];

        // [What it does]: Candidate B: Product of the two smallest (most negative) numbers 
        // and the single largest number at the end of the array.
        int optionB = nums[0] * nums[1] * nums[n - 1];

        // [Why needed]: Return the maximum of the two scenarios to safely handle negative values.
        return Math.max(optionA, optionB);
    }

    /**
     * APPROACH 3: SINGLE-PASS LINEAR SCAN (O(N) Time Optimization)
     * Time: O(N), Space: O(1)
     */
    public int maximumProductLinear(int[] nums) {
        int min1 = Integer.MAX_VALUE, min2 = Integer.MAX_VALUE;
        int max1 = Integer.MIN_VALUE, max2 = Integer.MIN_VALUE, max3 = Integer.MIN_VALUE;

        for (int x : nums) {
            // Track two smallest numbers
            if (x < min1) {
                min2 = min1;
                min1 = x;
            } else if (x < min2) {
                min2 = x;
            }

            // Track three largest numbers
            if (x > max1) {
                max3 = max2;
                max2 = max1;
                max1 = x;
            } else if (x > max2) {
                max3 = max2;
                max2 = x;
            } else if (x > max3) {
                max3 = x;
            }
        }

        return Math.max(min1 * min2 * max1, max1 * max2 * max3);
    }

    /**
     * SUMMARY
     * 1. Core pattern: Extremum tracking (Top 3 maxes + Bottom 2 mins).
     * 2. Key observation: Negative numbers come in pairs to form positive products; hence, `nums[0] * nums[1] * nums[n-1]` 
     *    must be compared against `nums[n-1] * nums[n-2] * nums[n-3]`.
     * 3. Memorize vs Understand: Understand why sorting reveals both candidate scenarios instantly.
     * 4. Most common trap: Assuming the answer is always the product of the three largest numbers, which fails on arrays 
     *    like `[-100, -98, 1, 2, 3]`.
     * 5. Mental trigger: "Maximum product of three numbers" -> Sort array -> `max(last 3 product, first 2 * last product)`.
     */
    public static void main(String[] args) {
        MaximumProductOfThreeNumbers solution = new MaximumProductOfThreeNumbers();

        // Test Case 1: Standard Positive Array
        int[] nums1 = {1, 2, 3};
        System.out.println("Test 1 (1, 2, 3): " + solution.maximumProduct(nums1)); // Expected: 6

        // Test Case 2: Mixed Array with Large Negatives
        int[] nums2 = {-100, -98, 1, 2, 3};
        System.out.println("Test 2 (Negative mix): " + solution.maximumProduct(nums2)); // Expected: 29400

        // Test Case 3: All Negatives
        int[] nums3 = {-1, -2, -3};
        System.out.println("Test 3 (All negatives): " + solution.maximumProduct(nums3)); // Expected: -6
        
        // Test Case 4: Single-pass verification
        int[] nums4 = {1, 2, 3, 4};
        System.out.println("Test 4 (Linear scan check): " + solution.maximumProductLinear(nums4)); // Expected: 24
    }
}


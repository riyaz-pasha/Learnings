/**
 * ============================================================================
 * VALID SUBARRAYS (FIRST ELEMENT IS MINIMUM) - INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given an array, count the number of contiguous subarrays where the 
 * first element is less than or equal to all other elements in that subarray.
 * 
 * Simple Explanation for Interviewer:
 * "We are trying to find how far each element can stretch to the right while 
 * remaining the smallest element in that window. As soon as we hit an element 
 * strictly smaller than our starting number, the window breaks. The total 
 * number of valid subarrays starting at index `i` is simply the distance from 
 * `i` to that first strictly smaller element."
 * 
 * Core Idea & Intuition:
 * This problem is a direct application of the "Next Smaller Element" pattern. 
 * If the Next Strictly Smaller Element to the right of `nums[i]` is at index `j`, 
 * then the subarrays `nums[i..i]`, `nums[i..i+1]`, ..., `nums[i..j-1]` are all 
 * valid. The number of such subarrays is exactly `j - i`. 
 * A Monotonic Stack perfectly maps to this logic, allowing us to resolve the 
 * next smaller element for every index in a single pass.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can subarrays be of length 1?"
 *    Why: Establishes the baseline. 
 *    Impact: Yes, a single element is trivially less than or equal to itself, 
 *    so every element forms at least one valid subarray.
 * 
 * 2. Q: "How do we handle duplicate values?"
 *    Why: The problem states "less than or equal to". 
 *    Impact: A duplicate does not invalidate the subarray. For example, in 
 *    `[2, 2]`, the first 2 is `<= 2`. We only break on strictly smaller values.
 * 
 * 3. Q: "Will the final count fit into a standard 32-bit integer?"
 *    Why: Array length N is 1000. The maximum number of subarrays is N*(N+1)/2, 
 *    which is 500,500. This easily fits into a 32-bit `int`. (If N was 10^5, 
 *    we would absolutely need a `long`).
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example:
 * Input: [3, 1, 2, 4]
 * 
 * Valid Subarrays starting at each index:
 * - Starts at 3 (idx 0): [3] -> Breaks at 1. (Count: 1)
 * - Starts at 1 (idx 1): [1], [1, 2], [1, 2, 4] -> Never breaks. (Count: 3)
 * - Starts at 2 (idx 2): [2], [2, 4] -> Never breaks. (Count: 2)
 * - Starts at 4 (idx 3): [4] -> Never breaks. (Count: 1)
 * Output: 1 + 3 + 2 + 1 = 7.
 * 
 * Visualizing the Right-to-Left Monotonic Stack:
 * (Stack stores indices. We pop when stack top is >= current element)
 * 
 * i | Val | Stack before | Next Smaller Idx (j) | Subarrays (j-i) | Stack after
 * -----------------------------------------------------------------------------
 * 3 |  4  | []           | 4 (End of array)     | 4 - 3 = 1       | [3]
 * 2 |  2  | [3] (val 4)  | 4 (End of array)*    | 4 - 2 = 2       | [2]
 * 1 |  1  | [2] (val 2)  | 4 (End of array)*    | 4 - 1 = 3       | [1]
 * 0 |  3  | [1] (val 1)  | 1 (val 1 is < 3)     | 1 - 0 = 1       | [1, 0]
 * 
 * *For values 2 and 1, the stack top was >= the current value, so we popped 
 * it until the stack was empty, meaning no smaller element exists to the right.
 * Total Count: 1 + 2 + 3 + 1 = 7.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Pinpoint the condition—subarray breaks on the first smaller element.
 * 2. Clarify: Array length is 1000. Subarrays of length 1 count.
 * 3. Brute Force: Propose iterating `i` and `j`. Point out that since N=1000, 
 *    O(N^2) takes only ~1 million operations and is completely viable.
 * 4. Optimize: Mention that this is fundamentally a "Next Smaller Element" 
 *    problem, which screams Monotonic Stack (O(N)).
 * 5. Code: Write the O(N) stack solution to demonstrate advanced data structure 
 *    knowledge.
 * 6. Dry-run: Trace `[3, 1, 2, 4]` step-by-step.
 * 7. Complexity: O(N) Time, O(N) Space.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Strictly increasing: `[1, 2, 3]` -> No elements are smaller. Every index 
 *   extends to the end of the array. Total = 6.
 * - Strictly decreasing: `[3, 2, 1]` -> Every element breaks immediately on 
 *   the next element. Total = 3 (only length 1 subarrays).
 * - All identical: `[2, 2, 2]` -> Handled smoothly. 2 is not strictly smaller 
 *   than 2, so the window extends fully. Total = 6.
 * 
 * Common Mistakes:
 * - Using `>` instead of `>=` when popping from the stack. If we only pop 
 *   elements strictly greater than `nums[i]`, equal elements stay in the stack. 
 *   This incorrectly treats identical elements as "blockers". We MUST pop `>=`.
 * - Returning an `int` when constraints specify N=10^5 (if the problem scale 
 *   were larger). Always consider `long` for subarray counting problems!
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class ValidSubarrays {

    public static void main(String[] args) {
        int[] nums1 = {3, 1, 2, 4}; // Expected: 7
        int[] nums2 = {2, 2, 2};    // Expected: 6
        int[] nums3 = {3, 2, 1};    // Expected: 3

        System.out.println("--- Brute Force (Two Pointers) ---");
        System.out.println("Count: " + validSubarraysBruteForce(nums1));

        System.out.println("\n--- Optimal Approach (Monotonic Stack) ---");
        System.out.println("Count: " + validSubarraysOptimal(nums1));
        System.out.println("Count: " + validSubarraysOptimal(nums2));
        System.out.println("Count: " + validSubarraysOptimal(nums3));
    }

    /**
     * SOLUTION 1: BRUTE FORCE (Two Pointers)
     * ------------------------------------------------------------------------
     * Idea: For every starting index `i`, expand a window to the right using `j`. 
     * Stop the window as soon as `nums[j] < nums[i]`. Every valid `j` adds 1 
     * to the total count of valid subarrays.
     * 
     * Time Complexity: O(N^2). Perfectly acceptable for N=1000.
     * Space Complexity: O(1).
     */
    public static int validSubarraysBruteForce(int[] nums) {
        int count = 0;
        int n = nums.length;
        
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                // If we encounter a strictly smaller element, the run is over.
                if (nums[j] < nums[i]) {
                    break;
                }
                // Otherwise, the subarray nums[i..j] is valid.
                count++;
            }
        }
        
        return count;
    }

    /**
     * SOLUTION 2: OPTIMAL (Monotonic Stack)
     * ------------------------------------------------------------------------
     * Idea: We iterate from right to left, using a stack to maintain the indices 
     * of a strictly increasing sequence of values.
     * When evaluating `nums[i]`, we pop all elements from the stack that are 
     * GREATER THAN OR EQUAL TO `nums[i]`. 
     * The index left at the top of the stack is the Next Strictly Smaller Element.
     * The number of valid subarrays starting at `i` is the distance between `i` 
     * and this Next Smaller Element.
     * 
     * Time Complexity: O(N). Each index is pushed and popped at most once.
     * Space Complexity: O(N) to store indices in the worst-case scenario 
     * (e.g., a strictly increasing array).
     */
    public static int validSubarraysOptimal(int[] nums) {
        if (nums == null || nums.length == 0) return 0;

        int n = nums.length;
        int count = 0;
        
        // Stack stores indices of elements.
        Deque<Integer> stack = new ArrayDeque<>();
        
        // Traverse from right to left
        for (int i = n - 1; i >= 0; i--) {
            // We are looking for the next STRICTLY SMALLER element.
            // Therefore, we pop elements that are >= current element.
            while (!stack.isEmpty() && nums[stack.peek()] >= nums[i]) {
                stack.pop();
            }
            
            // If the stack is empty, no smaller element exists to the right.
            // The valid window stretches to the very end of the array.
            int nextSmallerIndex = stack.isEmpty() ? n : stack.peek();
            
            // The number of valid subarrays starting at i is the distance 
            // to the next strictly smaller element.
            count += (nextSmallerIndex - i);
            
            // Push current index onto the stack for elements further left to evaluate.
            stack.push(i);
        }
        
        return count;
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * | Approach        | Time   | Space | Trade-offs                 |
     * |-----------------|--------|-------|----------------------------|
     * | Brute Force     | O(N^2) | O(1)  | Easy to write. Passes N=1K.|
     * | Monotonic Stack | O(N)   | O(N)  | Scales to N=100K flawlessly|
     * 
     * Interview Recommendation: 
     * Call out that O(N^2) is acceptable given N=1000, but immediately present 
     * the O(N) Monotonic Stack solution. It demonstrates an understanding of 
     * advanced structural patterns rather than just nested loop logic.
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the constraints were N=10^5 and we needed to return the sum 
     *      of the elements in all valid subarrays?"
     * A1: We would still use the Monotonic Stack to find the bounds, but we 
     *     would compute a Prefix Sum array. We can then use mathematical formulas 
     *     to quickly calculate the sum of all overlapping subarrays in O(1) time 
     *     per window. (Also, we would MUST switch our counters to `long`).
     * 
     * Q2: "Can this be done Left-to-Right?"
     * A2: Yes! When iterating left to right, we maintain a Monotonic Increasing 
     *     stack. Every time we encounter a smaller element `nums[i]`, it forces 
     *     us to pop `stack.peek()`. The moment it is popped, we know its window 
     *     just closed at `i`, so we can add `i - popped_index` to the total count. 
     *     At the end of the loop, any remaining elements in the stack extend to `N`.
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - The "Distance to the Next Smaller/Greater Element" is mathematically 
     *   identical to "The number of subarrays governed by this element".
     * - Always clarify duplicate handling. `<=`, `<`, `>=`, and `>` fundamentally 
     *   alter whether equal elements trigger a stack pop.
     */
}

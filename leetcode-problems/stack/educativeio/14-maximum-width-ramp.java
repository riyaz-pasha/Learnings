/**
 * ============================================================================
 * MAXIMUM WIDTH RAMP - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: A ramp in an integer array is a pair of indices (i, j) where 
 * i < j and nums[i] <= nums[j]. The width of this ramp is j - i. Find the 
 * maximum width of any ramp in the array. If none exist, return 0.
 * 
 * Simple Explanation for Interviewer:
 * "We are looking for two elements in the array where the left element is 
 * smaller than or equal to the right element, and the physical distance between 
 * them (j - i) is as large as possible. We want to maximize that distance."
 * 
 * Core Idea & Intuition:
 * - Brute Force compares every possible pair, which takes O(N^2) time.
 * - Sorting gives us pairs ordered by value. As we iterate through the sorted 
 *   values, any previously seen index is valid (because the values are sorted). 
 *   By tracking the *minimum* index seen so far, we can find the max width in O(N log N).
 * - The optimal O(N) approach uses a Monotonic Stack. If we have a left index `a` 
 *   and a later index `b` where `nums[b] > nums[a]`, `b` will *never* be a better 
 *   starting point than `a` because `a` is further left AND has a smaller value. 
 *   Therefore, the potential starting points form a strictly decreasing sequence.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can the array contain negative numbers?"
 *    Why: Ensures there are no issues with sorting comparators or specific logic.
 *    Impact: Prompt states non-negative (0 to 50000).
 * 2. Q: "What if the array is sorted in strictly descending order?"
 *    Why: Edge case where no ramp exists.
 *    Impact: Should return 0.
 * 3. Q: "Are duplicate values allowed?"
 *    Why: Determines if `nums[i] <= nums[j]` strictly requires `<=` over `<`.
 *    Impact: Yes, so ramps of width > 0 can exist for flat arrays (e.g., [2, 2, 2]).
 * 4. Q: "What is the expected time and space complexity?"
 *    Why: Constraints are up to 50,000. O(N^2) will cause a Time Limit Exceeded (TLE). 
 *    We must use an O(N log N) or O(N) approach.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: nums = [6, 0, 8, 2, 1, 5]
 * Output: 4 (i = 1, j = 5 => nums[1] = 0 <= nums[5] = 5, width = 5 - 1 = 4)
 * 
 * Example 2 (Strictly Descending):
 * Input: nums = [9, 8, 7, 6]
 * Output: 0
 * 
 * Visualizing the Monotonic Stack Approach:
 * Target: Maximize (j - i) where nums[i] <= nums[j].
 * Array: [6, 0, 8, 2, 1, 5]
 * 
 * Step 1 (Build strictly decreasing stack of candidates for 'i'):
 * - i=0, val=6 -> Push 0. Stack: [0]
 * - i=1, val=0 -> 0 < 6. Push 1. Stack: [0, 1]
 * - i=2, val=8 -> 8 > 0. Ignore.
 * - ...all remaining values are > 0. Stack remains [0, 1].
 * 
 * Step 2 (Scan from right to left to find best 'j'):
 * - j=5, val=5 -> Compare with Stack Top (1, val=0). 5 >= 0! 
 *   Valid ramp! Width = 5 - 1 = 4. 
 *   Pop 1. Stack is now [0].
 *   Compare with Stack Top (0, val=6). 5 < 6. Stop.
 * - j=4, val=1 -> Compare with Stack Top (0, val=6). 1 < 6. Stop.
 * ...
 * Max width = 4.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm the definition of a ramp and the goal to maximize width.
 * 2. Clarify: Array limits, handling duplicates, impossible cases.
 * 3. Initial Approach (Brute Force): Mention nested loops (O(N^2)).
 * 4. Better Approach (Sorting): Mention pairing values with indices, sorting, and 
 *    tracking the minimum index (O(N log N)).
 * 5. Optimal Approach (Monotonic Stack): Explain the greedy choice. We only care 
 *    about strictly decreasing start points. Build the stack left-to-right, then 
 *    evaluate right-to-left.
 * 6. Code: Write the Monotonic Stack solution.
 * 7. Dry-run: Trace the stack with [6, 0, 8, 2, 1, 5].
 * 8. Complexity Analysis: Discuss why it is O(N) even though there is a nested loop 
 *    (each index is pushed/popped exactly once).
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - All elements identical: [2, 2, 2, 2] -> Should return 3 (i=0, j=3).
 * - Strictly descending: [5, 4, 3, 2] -> Should return 0.
 * - Array of length 2: Minimum possible constraint size.
 * 
 * Common Mistakes:
 * - Popping from the stack and forgetting that a popped index `i` doesn't need 
 *   to be evaluated again. (Since we scan `j` from right to left, the FIRST valid 
 *   `j` we find for `i` provides the maximum possible width. Any subsequent `j` 
 *   will be further left, resulting in a smaller width!). This is the genius of 
 *   the algorithm.
 * - Iterating left-to-right in the second phase instead of right-to-left.
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;

public class MaximumWidthRamp {

    public static void main(String[] args) {
        int[] nums1 = {6, 0, 8, 2, 1, 5};
        int[] nums2 = {9, 8, 1, 0, 1, 9, 4, 0, 4, 1};

        System.out.println("--- Sorting Approach (O(N log N)) ---");
        System.out.println("Ramp width (nums1): " + maxWidthRampSorting(nums1)); // Output: 4
        System.out.println("Ramp width (nums2): " + maxWidthRampSorting(nums2)); // Output: 7

        System.out.println("\n--- Optimal Stack Approach (O(N)) ---");
        System.out.println("Ramp width (nums1): " + maxWidthRampStack(nums1));   // Output: 4
        System.out.println("Ramp width (nums2): " + maxWidthRampStack(nums2));   // Output: 7

        System.out.println("\n--- Right-Max Array Approach (O(N)) ---");
        System.out.println("Ramp width (nums1): " + maxWidthRampRightMax(nums1)); // Output: 4
    }

    /**
     * SOLUTION 1: BRUTE FORCE (Conceptual - O(N^2))
     * ------------------------------------------------------------------------
     * Idea: Loop i from 0 to N. Loop j from i+1 to N. Check if nums[i] <= nums[j],
     * and update max width.
     * Skip coding this in an interview unless specifically asked. It will TLE.
     */
    public static int maxWidthRampBruteForce(int[] nums) {
        int max = 0;
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] <= nums[j]) {
                    max = Math.max(max, j - i);
                }
            }
        }
        return max;
    }

    /**
     * SOLUTION 2: SORTING (O(N log N))
     * ------------------------------------------------------------------------
     * Idea: Store pairs of (value, index) in a 2D array or object list.
     * Sort the list based on the values.
     * Now, as we iterate through the sorted list, any element we've already seen 
     * is guaranteed to have a value <= the current element's value.
     * To maximize the width (j - i), we just need to keep track of the minimum 
     * index (i) we have seen so far, and subtract it from the current index (j).
     * 
     * Time Complexity: O(N log N) due to sorting.
     * Space Complexity: O(N) for storing the pairs.
     */
    public static int maxWidthRampSorting(int[] nums) {
        int n = nums.length;
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        // Sort indices based on their corresponding values in nums
        // If values are equal, sort by index to maintain stability
        Arrays.sort(indices, (a, b) -> {
            if (nums[a] == nums[b]) {
                return Integer.compare(a, b);
            }
            return Integer.compare(nums[a], nums[b]);
        });

        int maxWidth = 0;
        int minIndex = n; // Tracks the minimum index seen so far in sorted order

        for (int i : indices) {
            // i represents the index 'j' (the right bound) in our ramp
            maxWidth = Math.max(maxWidth, i - minIndex);
            minIndex = Math.min(minIndex, i); // update the lowest 'i' (left bound)
        }

        return maxWidth;
    }

    /**
     * SOLUTION 3: MONOTONIC STACK (Optimal O(N))
     * ------------------------------------------------------------------------
     * Idea: 
     * 1. Build a Monotonic Decreasing Stack of indices. Iterate i from 0 to N-1.
     *    If nums[i] < nums[stack.peek()], push i. 
     *    Why? We are collecting the best possible candidates for the left pointer `i`.
     *    If a number is larger than the stack top, it's a worse candidate because 
     *    it is BOTH larger (harder to satisfy nums[i] <= nums[j]) AND further right 
     *    (produces a smaller width).
     * 2. Traverse the array backwards (j from N-1 down to 0).
     *    If nums[j] >= nums[stack.peek()], we found a valid ramp!
     *    Calculate the width, pop the stack, and repeat. 
     *    Why pop? Because since we are moving j backwards, the CURRENT j is the 
     *    furthest right it will ever be for this specific stack top. Any future j 
     *    would be smaller, yielding a smaller width. So the stack top is fully resolved!
     * 
     * Time Complexity: O(N). Each index is pushed and popped at most once.
     * Space Complexity: O(N) for the stack.
     */
    public static int maxWidthRampStack(int[] nums) {
        int n = nums.length;
        Deque<Integer> stack = new ArrayDeque<>();

        // Pass 1: Build the strictly decreasing stack of potential start indices (i)
        for (int i = 0; i < n; i++) {
            if (stack.isEmpty() || nums[stack.peek()] > nums[i]) {
                stack.push(i);
            }
        }

        int maxWidth = 0;

        // Pass 2: Iterate backwards to find the best end index (j)
        for (int j = n - 1; j >= 0; j--) {
            // While a valid ramp exists for the stack's top element
            while (!stack.isEmpty() && nums[stack.peek()] <= nums[j]) {
                maxWidth = Math.max(maxWidth, j - stack.pop());
            }
        }

        return maxWidth;
    }

    /**
     * SOLUTION 4: RIGHT-MAX ARRAY (Optimal O(N))
     * ------------------------------------------------------------------------
     * Idea: Create an array `rightMax` where `rightMax[i]` contains the maximum 
     * value in the subarray `nums[i...n-1]`.
     * Then, use two pointers `i` and `j` starting at 0.
     * If `nums[i] <= rightMax[j]`, it means there IS some element at or after `j` 
     * that is `>= nums[i]`. Thus, we can safely expand our window by incrementing `j`.
     * If `nums[i] > rightMax[j]`, it's impossible to find a valid right bound 
     * for the current `i`, so we must increment `i`.
     * 
     * Time Complexity: O(N) - One pass to build rightMax, one pass with two pointers.
     * Space Complexity: O(N) to store the rightMax array.
     */
    public static int maxWidthRampRightMax(int[] nums) {
        int n = nums.length;
        int[] rightMax = new int[n];

        // Fill rightMax array
        rightMax[n - 1] = nums[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i + 1], nums[i]);
        }

        int left = 0;
        int right = 0;
        int maxWidth = 0;

        // Two Pointers
        while (right < n) {
            // As long as there's a valid element to the right, expand the window
            while (right < n && nums[left] <= rightMax[right]) {
                maxWidth = Math.max(maxWidth, right - left);
                right++;
            }
            // Move left pointer to find a smaller starting value
            left++;
        }

        return maxWidth;
    }

    /**
     * ========================================================================
     * 8. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time     | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Brute Force    | O(N^2)   | O(1)  | Trivial, but fails on time.| Mention only.
     * Sorting        | O(NlogN) | O(N)  | Easy to understand/code.   | Good backup.
     * Monotonic Stack| O(N)     | O(N)  | Clever, relies on pop logic| ⭐ STRONGLY REC.
     * Right-Max Array| O(N)     | O(N)  | Avoids stacks, very clean. | Great alternative.
     * 
     * ========================================================================
     * 9. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "Can we do this in O(1) space?"
     * A1: No known O(1) space and O(N) time solution exists for this problem because 
     *     we need to "remember" state across the array (either sorted indices, a stack, 
     *     or a suffix-max array) to avoid O(N^2) comparisons.
     * 
     * Q2: "What if we wanted the minimum width ramp > 0?"
     * A2: The monotonic stack logic reverses completely. We would need sorting, or a 
     *     sliding window if constraints permit. The 'minimum distance' fundamentally 
     *     changes the greedy choice that makes the stack work here.
     * 
     * ========================================================================
     * 10. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: When trying to MAXIMIZE distance (j - i) under a condition, 
     *   a Monotonic Stack combined with iterating BACKWARDS is a legendary pattern.
     * - The "Aha!" Moment: Realizing that once you find a valid right-side element `j` 
     *   for a stack element `i` (when scanning right-to-left), you can POP `i` 
     *   permanently because moving `j` further left will only SHRINK the width.
     */
}

import java.util.*;

/**
 * ====================================================================================
 * PROBLEM STATEMENT: MAXIMUM WIDTH RAMP (LeetCode 962)
 * ====================================================================================
 * A ramp in an integer array `nums` is a pair (i, j) for which i < j and nums[i] <= nums[j].
 * The width of such a ramp is (j - i).
 * 
 * Goal: Return the maximum width of a ramp in `nums`. If there is no ramp, return 0.
 * 
 * Example:
 *   nums = [6, 0, 8, 2, 1, 5]
 * 
 *   Valid Ramps:
 *   - (1, 5): nums[1] = 0 <= nums[5] = 5 -> width = 5 - 1 = 4
 *   - (3, 5): nums[3] = 2 <= nums[5] = 5 -> width = 5 - 3 = 2
 *   - (1, 2): nums[1] = 0 <= nums[2] = 8 -> width = 2 - 1 = 1
 *   ...
 *   Maximum Width Ramp: (1, 5) with width = 4.
 * ====================================================================================
 */

public class MaximumWidthRamp {

    /**
     * Finds the maximum width ramp in O(N) time and O(N) space using a Monotonic Stack.
     *
     * @param nums Array of integers
     * @return The maximum width (j - i) such that i < j and nums[i] <= nums[j]
     */
    public static int maxWidthRamp(int[] nums) {
        int n = nums.length;

        // Monotonic Stack storing candidates for the left index 'i'
        Deque<Integer> stack = new ArrayDeque<>();

        /*
         * ============================================================================
         * STEP 1: BUILD A MONOTONIC DECREASING STACK OF LEFT BOUNDARIES ('i')
         * ============================================================================
         * We iterate left-to-right (0 to n-1) and push an index `i` ONLY IF its value 
         * is strictly smaller than the value at the index on top of the stack:
         * 
         *     nums[i] < nums[stack.peek()]
         * 
         * WHY DO WE DISCARD INDICES THAT ARE GREATER OR EQUAL? (Mathematical Proof)
         * ----------------------------------------------------------------------------
         * Suppose index k > index i, and nums[k] >= nums[i].
         * Could index k EVER yield a wider ramp than index i for any future right index j?
         * 
         * 1. Target Condition: To form a valid ramp with j, we need nums[j] >= nums[candidate].
         *    Since nums[i] <= nums[k], any j that satisfies nums[j] >= nums[k] will 
         *    AUTOMATICALLY satisfy nums[j] >= nums[i]. Thus, k offers NO relaxed value condition.
         * 
         * 2. Width Condition: Since k > i, for any right index j (where j > k > i):
         *    (j - i) > (j - k)
         * 
         * Conclusion: Index `i` is strictly superior to index `k` in both value requirement 
         * and maximum achievable width. Therefore, index `k` is DOMINATED by `i` and can 
         * be safely ignored.
         * 
         * Resulting Stack Structure:
         * - Indices in `stack` are strictly increasing: i0 < i1 < i2 ...
         * - Values in `nums` at these indices are strictly decreasing: 
         *   nums[i0] > nums[i1] > nums[i2] ...
         * 
         * Example for nums = [6, 0, 8, 2, 1, 5]:
         * - i = 0 (val 6): push -> stack = [0]
         * - i = 1 (val 0): 0 < 6 -> push -> stack = [0, 1]
         * - i = 2 (val 8): 8 >= 0 -> skip
         * - i = 3 (val 2): 2 >= 0 -> skip
         * - i = 4 (val 1): 1 >= 0 -> skip
         * - i = 5 (val 5): 5 >= 0 -> skip
         * Final Stack (top to bottom): [1, 0]  (representing values [0, 6])
         */
        for (int i = 0; i < n; i++) {
            if (stack.isEmpty() || nums[i] < nums[stack.peek()]) {
                stack.push(i);
            }
        }

        int maxWidth = 0;

        /*
         * ============================================================================
         * STEP 2: TRAVERSE RIGHT-TO-LEFT FOR RIGHT BOUNDARIES ('j')
         * ============================================================================
         * Now we iterate `j` backwards from `n - 1` down to `0`.
         * 
         * For the current `j`, we check if it can form a valid ramp with the index `i` 
         * at the top of the stack (`nums[j] >= nums[stack.peek()]`).
         * 
         * If VALID (`nums[j] >= nums[i]`):
         * 1. Calculate width: `j - i` and update `maxWidth`.
         * 2. POP `i` FROM THE STACK (`stack.pop()`).
         * 3. Repeat while `stack` is not empty and `nums[j] >= nums[stack.peek()]`.
         * 
         * WHY ARE WE ALLOWED TO POP `i` GREEDILY? (Crucial Invariant)
         * ----------------------------------------------------------------------------
         * Because we are scanning `j` from RIGHT to LEFT:
         * - The FIRST time we find a valid `j` for candidate `i`, this `j` is the 
         *   FARTHEST POSSIBLE RIGHT INDEX that `i` will ever encounter!
         * - Any future right boundary `j'` will have `j' < j`.
         * - Therefore, for this specific left index `i`:
         *   Width with current j:  `j - i`
         *   Width with future j': `j' - i`  (which is strictly smaller than `j - i`)
         * 
         * Since `i` has already achieved its MAXIMUM POSSIBLE RAMP WIDTH with the 
         * current `j`, `i` can NEVER contribute to a larger `maxWidth` in subsequent 
         * iterations. We can safely throw `i` away (pop it).
         */
        for (int j = n - 1; j >= 0; j--) {

            while (!stack.isEmpty() && nums[j] >= nums[stack.peek()]) {
                int i = stack.pop();
                maxWidth = Math.max(maxWidth, j - i);
            }

            // OPTIMIZATION (Early Exit):
            // If the remaining possible width (j) is less than or equal to `maxWidth`,
            // no future j' < j can produce a larger ramp. We can break early!
            if (j <= maxWidth) {
                break;
            }
        }

        return maxWidth;
    }

    public static void main(String[] args) {
        int[] nums = {6, 0, 8, 2, 1, 5};

        System.out.println("Maximum Width Ramp: " + maxWidthRamp(nums)); // Output: 4
    }
}

/**
 * ====================================================================================
 * EXECUTION WALKTHROUGH FOR `nums = [6, 0, 8, 2, 1, 5]`
 * ====================================================================================
 *
 * Array:   index:  0  1  2  3  4  5
 *          value:  6  0  8  2  1  5
 *
 * ------------------------------------------------------------------------------------
 * STEP 1: Build Decreasing Stack
 * ------------------------------------------------------------------------------------
 * i = 0: val = 6. Stack empty -> push(0).          Stack = [0]
 * i = 1: val = 0. 0 < 6       -> push(1).          Stack = [0, 1]
 * i = 2: val = 8. 8 >= 0      -> skip.             Stack = [0, 1]
 * i = 3: val = 2. 2 >= 0      -> skip.             Stack = [0, 1]
 * i = 4: val = 1. 1 >= 0      -> skip.             Stack = [0, 1]
 * i = 5: val = 5. 5 >= 0      -> skip.             Stack = [0, 1]
 *
 * Final Stack State (top to bottom): [1, 0]
 * Corresponding Values:             [0, 6]
 *
 * ------------------------------------------------------------------------------------
 * STEP 2: Scan j from Right to Left
 * ------------------------------------------------------------------------------------
 *
 * j = 5 (nums[5] = 5):
 * ------------------------------------------------------------------------------------
 *   - Check stack top: index 1 (val 0).
 *     Is nums[5] >= nums[1] ? (5 >= 0) -> TRUE!
 *     pop index 1.
 *     width = 5 - 1 = 4.
 *     maxWidth = max(0, 4) = 4.
 *     Stack is now [0].
 *
 *   - Check stack top: index 0 (val 6).
 *     Is nums[5] >= nums[0] ? (5 >= 6) -> FALSE!
 *     Loop stops for j = 5.
 *
 * j = 4 (nums[4] = 1):
 * ------------------------------------------------------------------------------------
 *   - Check stack top: index 0 (val 6).
 *     Is nums[4] >= nums[0] ? (1 >= 6) -> FALSE!
 *     Loop stops for j = 4.
 *
 * j = 3 (nums[3] = 2):
 * ------------------------------------------------------------------------------------
 *   - Check stack top: index 0 (val 6).
 *     Is nums[3] >= nums[0] ? (2 >= 6) -> FALSE!
 *     Loop stops for j = 3.
 *
 * j = 2 (nums[2] = 8):
 * ------------------------------------------------------------------------------------
 *   - Check stack top: index 0 (val 6).
 *     Is nums[2] >= nums[0] ? (8 >= 6) -> TRUE!
 *     pop index 0.
 *     width = 2 - 0 = 2.
 *     maxWidth = max(4, 2) = 4.
 *     Stack is now [].
 *
 * Stack is now empty. Remaining iterations for j break early.
 *
 * Final Result: 4
 *
 * ====================================================================================
 * COMPLEXITY & EDGE CASE ANALYSIS
 * ====================================================================================
 * Time Complexity: O(N)
 * - Step 1 takes O(N) as each index is pushed to the stack at most once.
 * - Step 2 takes O(N) because `j` decrements N times, and each index in the stack 
 *   is popped AT MOST ONCE across the entire loop execution.
 * - Total operations <= 2 * N -> O(N) linear time.
 *
 * Space Complexity: O(N)
 * - Auxiliary space used by `stack` stores at most N indices in the worst-case 
 *   (e.g., strictly decreasing array like [5, 4, 3, 2, 1]).
 *
 * Key Edge Cases Handled:
 * 1. Strictly Decreasing Array (e.g., [5, 4, 3, 2, 1]):
 *    - Stack becomes [0, 1, 2, 3, 4].
 *    - No valid ramps formed -> returns 0 correctly.
 * 2. Strictly Increasing Array (e.g., [1, 2, 3, 4, 5]):
 *    - Stack becomes [0].
 *    - j = 4 matches with i = 0 -> returns 4 (n - 1) correctly.
 * 3. All Equal Elements (e.g., [3, 3, 3, 3]):
 *    - Stack becomes [0].
 *    - j = 3 matches with i = 0 -> returns 3 correctly.
 * ====================================================================================
 */

/**
 * STANDALONE STUDY NOTE: CONCATENATION OF ARRAY
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Should the returned array be a new array of length 2n?" (Yes, prompt specifies `ans` is of length 2n).
 * - "Can we assume `nums` is non-null and has length n >= 1?" (Yes, per constraints).
 * - "Is the operation 0-indexed?" (Yes, `0 <= i < n`, where `ans[i] == nums[i]` and `ans[i + n] == nums[i]`).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to create an array `ans` of size 2n by duplicating the input array `nums` back-to-back. 
 * The binding constraint is doing this efficiently in a single pass without redundant nested loops.
 * 
 * --- APPROACH 1: BRUTE FORCE (Nested Loops / Manual Copy) ---
 * 1. What I'd naturally try: Write two separate loops: one to copy elements into the first half of `ans`, 
 *    and a second loop to copy them again into the second half.
 * 2. Why it works: It populates both halves of the target array correctly.
 * 3. Why it's redundant: We can achieve the exact same result in a single pass by copying each element to 
 *    both index `i` and index `i + n` simultaneously.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we iterate through the array.
 *    - Space Complexity: O(N) — to allocate the new array of size 2n.
 * 
 * --- APPROACH 2: OPTIMAL (Single-Pass Dual Index Assignment) ---
 * 1. The Observation: For every index `i` from 0 to `n - 1`, we need to set both `ans[i]` and `ans[i + n]` 
 *    to `nums[i]`. We can do this in a single loop of length `n`.
 * 2. Which one I'd write in an interview: Approach 2 (Single-Pass Dual Index Assignment). It is concise, optimal, 
 *    and cuts the loop iterations in half compared to two separate sequential loops.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — where N is the length of `nums`. We visit each index exactly once.
 *    - Space Complexity: O(N) — auxiliary space allocated for the result array `ans` of size 2n.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Minimum array length (n = 1): `nums = [5]` -> `ans = [5, 5]`.
 * - Larger arrays: Handled seamlessly in linear O(N) time.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Dual-Index Assignment Pattern:
 * 
 * nums: [1, 2, 1]  (n = 3)
 * ans:  [ ,  ,  ,  ,  ,  ]  (length = 6)
 * 
 * i = 0: nums[0] = 1 -> ans[0] = 1, ans[0 + 3] = ans[3] = 1
 * i = 1: nums[1] = 2 -> ans[1] = 2, ans[1 + 3] = ans[4] = 2
 * i = 2: nums[2] = 1 -> ans[2] = 1, ans[2 + 3] = ans[5] = 1
 * 
 * Result: [1, 2, 1, 1, 2, 1]
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [1, 2, 1], n = 3
 * 
 * - Initialize ans = new int[6]
 * - i = 0: ans[0] = nums[0] (1), ans[3] = nums[0] (1)
 * - i = 1: ans[1] = nums[1] (2), ans[4] = nums[1] (2)
 * - i = 2: ans[2] = nums[2] (1), ans[5] = nums[2] (1)
 * 
 * Final ans = [1, 2, 1, 1, 2, 1].
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To construct the concatenated array of length 2n, we can allocate a new integer array of size `2 * n`. 
 * Then, in a single loop from 0 to `n - 1`, we assign `nums[i]` to both `ans[i]` and `ans[i + n]` simultaneously. 
 * This allows us to duplicate the array back-to-back in a single linear pass, achieving O(N) time complexity 
 * and O(N) space complexity for the output array."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: Can this be done using built-in Java array copying utilities?
 * A: Yes, we can use `System.arraycopy(nums, 0, ans, 0, n)` followed by `System.arraycopy(nums, 0, ans, n, n)`. 
 *    While cleaner, the explicit loop is equally fast and straightforward.
 */

import java.util.Arrays;

public class ConcatenationOfArray {

    /**
     * APPROACH 2: OPTIMAL (Single-Pass Dual Index Assignment)
     * Time: O(N), Space: O(N)
     */
    public int[] getConcatenation(int[] nums) {
        int n = nums.length;
        
        // [Why needed]: Result array must be of length 2n as specified by the prompt.
        int[] ans = new int[2 * n];

        // Single pass over the nums array
        for (int i = 0; i < n; i++) {
            // [What it does]: Assign nums[i] to its original position and its mirrored position shifted by n.
            ans[i] = nums[i];
            ans[i + n] = nums[i];
        }

        return ans;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Single-pass dual index mapping (`ans[i] = nums[i]` and `ans[i + n] = nums[i]`).
     * 2. Key observation: Duplicating an array back-to-back requires filling two indices per iteration in a single loop.
     * 3. Memorize vs Understand: Understand how `i + n` correctly targets the second half of the 2n result array.
     * 4. Most common trap: Allocating an array of size `n` instead of `2 * n`.
     * 5. Mental trigger: "Concatenate array to itself" -> `int[2 * n]` + single loop with `ans[i] = ans[i + n] = nums[i]`.
     */
    public static void main(String[] args) {
        ConcatenationOfArray solution = new ConcatenationOfArray();

        // Test Case 1: Standard Example
        int[] nums1 = {1, 2, 1};
        System.out.println("Test 1 (1, 2, 1): " + Arrays.toString(solution.getConcatenation(nums1))); 
        // Expected: [1, 2, 1, 1, 2, 1]

        // Test Case 2: Another Example
        int[] nums2 = {1, 3, 2, 1};
        System.out.println("Test 2 (1, 3, 2, 1): " + Arrays.toString(solution.getConcatenation(nums2))); 
        // Expected: [1, 3, 2, 1, 1, 3, 2, 1]
    }
}


class Solution {
    public int[] getConcatenation(int[] nums) {
        int n = nums.length;

        // The result must contain nums twice.
        int[] ans = new int[2 * n];

        for (int i = 0; i < n; i++) {
            // Copy the element into the first half.
            ans[i] = nums[i];

            // Copy the same element into the second half.
            ans[i + n] = nums[i];
        }

        return ans;
    }
}


class Solution {
    public int[] getConcatenation(int[] nums) {
        int n = nums.length;
        int[] ans = new int[2 * n];

        // Copy nums into the first half.
        for (int i = 0; i < n; i++) {
            ans[i] = nums[i];
        }

        // Copy nums into the second half.
        for (int i = 0; i < n; i++) {
            ans[i + n] = nums[i];
        }

        return ans;
    }
}



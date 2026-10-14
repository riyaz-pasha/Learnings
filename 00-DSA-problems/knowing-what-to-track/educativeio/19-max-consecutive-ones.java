/**
 * STANDALONE STUDY NOTE: MAX CONSECUTIVE ONES
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can the array be empty or null?" (Constraints state 1 <= nums.length <= 10^3, ensuring at least one element exists).
 * - "Are the array elements strictly 0 or 1?" (Yes, binary array constraint confirms this).
 * - "What should be returned if the array contains only 0s?" (Returns 0).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find the length of the longest contiguous subsegment of 1s in a binary array. 
 * The binding constraint is doing this in a single linear pass with constant extra memory.
 * 
 * --- APPROACH 1: BRUTE FORCE (Subarray Window Checking) ---
 * 1. What I'd naturally try: Check every possible starting and ending index in the array, verify if all elements 
 *    in that window are 1s, and keep track of the maximum length found.
 * 2. Why it works: It checks every possible contiguous segment.
 * 3. Why it's too slow/costly: Checking all subarrays takes O(N^2) or O(N^3) time, which is unnecessarily redundant.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^2) — quadratic checks over all window pairs.
 *    - Space Complexity: O(1) — constant extra variables.
 * 
 * --- APPROACH 2: OPTIMAL (Single-Pass Running Counter) ---
 * 1. The Observation: We can track consecutive 1s on the fly with a single linear scan. 
 *    Maintain two variables: `currentCount` and `maxCount`. 
 *    - When we see a `1`, we increment `currentCount` and update `maxCount = max(maxCount, currentCount)`.
 *    - When we see a `0`, we reset `currentCount = 0`.
 * 2. Which one I'd write in an interview: Approach 2 (Single-Pass Counter). It runs in O(N) time and O(1) space, 
 *    representing the optimal way to scan sequential streams.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we visit each element of the array exactly once.
 *    - Space Complexity: O(1) — because we only use two integer scalar variables.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Array of all 1s: `[1, 1, 1, 1]` -> Returns 4.
 * - Array of all 0s: `[0, 0, 0]` -> Returns 0.
 * - Single element: `[1]` -> Returns 1; `[0]` -> Returns 0.
 * - Alternating zeros and ones: `[1, 0, 1, 1, 0, 1]` -> Returns 2.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Running Stream Counter:
 * 
 * Array: [1, 1, 0, 1, 1, 1]
 * 
 * i=0: val=1 -> current=1, max=1
 * i=1: val=1 -> current=2, max=2
 * i=2: val=0 -> current=0, max=2 (Reset!)
 * i=3: val=1 -> current=1, max=2
 * i=4: val=1 -> current=2, max=2
 * i=5: val=1 -> current=3, max=3
 * 
 * Final Max = 3.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [1, 1, 0, 1, 1, 1]
 * 
 * - Init: maxCount = 0, currentCount = 0
 * - i = 0 (1): currentCount = 1, maxCount = max(0, 1) = 1
 * - i = 1 (1): currentCount = 2, maxCount = max(1, 2) = 2
 * - i = 2 (0): currentCount = 0, maxCount = 2
 * - i = 3 (1): currentCount = 1, maxCount = max(2, 1) = 2
 * - i = 4 (1): currentCount = 2, maxCount = max(2, 2) = 2
 * - i = 5 (1): currentCount = 3, maxCount = max(2, 3) = 3
 * 
 * Result: 3.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To find the maximum number of consecutive 1s, we can make a single linear pass over the binary array. 
 * We maintain a running counter for the current streak of 1s and a global maximum counter. Whenever we encounter a 1, 
 * we increment our current streak and update our maximum. Whenever we encounter a 0, we reset our current streak 
 * to zero. This gives us O(N) time complexity and O(1) auxiliary space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if you were allowed to flip at most one 0 to a 1?
 * A: That transforms the problem into "Max Consecutive Ones II", which can be solved using a variable-length 
 *    sliding window that allows at most one zero inside the active window.
 */

public class MaxConsecutiveOnes {

    /**
     * APPROACH 2: OPTIMAL (Single-Pass Running Counter)
     * Time: O(N), Space: O(1)
     */
    public int findMaxConsecutiveOnes(int[] nums) {
        int maxCount = 0;
        int currentCount = 0;

        // Single linear pass over the binary array
        for (int num : nums) {
            if (num == 1) {
                // [What it does]: Increment current streak and update max record
                currentCount++;
                maxCount = Math.max(maxCount, currentCount);
            } else {
                // [What it does]: Streak broken by a 0, reset current count
                currentCount = 0;
            }
        }

        return maxCount;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Running streak counter with reset on delimiter (0).
     * 2. Key observation: Contiguous sequences can be tracked on the fly without storing subarrays.
     * 3. Memorize vs Understand: Understand how `currentCount` resets to 0 when encountering a 0.
     * 4. Most common trap: Forgetting to update `maxCount` immediately after incrementing `currentCount`.
     * 5. Mental trigger: "Max consecutive ones" -> Single loop + `currentCount` + `maxCount`.
     */
    public static void main(String[] args) {
        MaxConsecutiveOnes solution = new MaxConsecutiveOnes();

        // Test Case 1: Standard Example
        int[] nums1 = {1, 1, 0, 1, 1, 1};
        System.out.println("Test 1 (Standard): " + solution.findMaxConsecutiveOnes(nums1)); // Expected: 3

        // Test Case 2: All 1s
        int[] nums2 = {1, 1, 1, 1};
        System.out.println("Test 2 (All 1s): " + solution.findMaxConsecutiveOnes(nums2)); // Expected: 4

        // Test Case 3: All 0s
        int[] nums3 = {0, 0, 0};
        System.out.println("Test 3 (All 0s): " + solution.findMaxConsecutiveOnes(nums3)); // Expected: 0

        // Test Case 4: Single Element
        int[] nums4 = {1};
        System.out.println("Test 4 (Single element 1): " + solution.findMaxConsecutiveOnes(nums4)); // Expected: 1
    }
}


/**
 * STANDALONE STUDY NOTE: SHORTEST UNSORTED CONTINUOUS SUBARRAY
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "What should be returned if the array is already sorted?" (Return 0, as specified in the prompt).
 * - "What is the minimum length of the array?" (Constraints state 1 <= nums.length <= 10^4).
 * - "Can elements be negative?" (Yes, values range from -10^5 to 10^5).
 * - "Is there a follow-up requirement for time complexity?" (Yes, the interviewer will typically ask for an optimal O(N) time solution).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find the shortest continuous subarray that, if sorted, makes the entire array sorted. 
 * The binding constraint is achieving an O(N) time complexity with O(1) space, avoiding expensive O(N log N) sorting checks.
 * 
 * --- APPROACH 1: BRUTE FORCE (All Subarray Sorting Check) ---
 * 1. What I'd naturally try: Try every possible starting index `i` and ending index `j`, copy the subarray `nums[i...j]`, 
 *    sort it, place it back into a copy of the array, and check if the entire array is now sorted. 
 *    Track the minimum length `j - i + 1` among all valid subarrays.
 * 2. Why it works: It exhaustively examines every possible contiguous window.
 * 3. Why it's too slow/costly: Checking all subarrays takes O(N^3) time (or O(N^2 log N) if we optimize the sort check), 
 *    which will time out for N = 10^4.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^3) — nested loops plus sorting overhead per window.
 *    - Space Complexity: O(N) — for array copies.
 * 
 * --- APPROACH 2: INTERMEDIATE (Sorting + Two-Pointer Boundary Comparison) ---
 * 1. The Observation: If we make a copy of `nums` and sort it, the elements that differ from the original array 
 *    give away the exact boundaries of the unsorted subarray!
 * 2. How it leads to the next step: 
 *    - Create a sorted copy of `nums` called `sortedNums`.
 *    - Scan from left to right to find the first index `left` where `nums[left] != sortedNums[left]`.
 *    - Scan from right to left to find the first index `right` where `nums[right] != sortedNums[right]`.
 *    - If no such indices exist, the array is already sorted (return 0). Otherwise, return `right - left + 1`.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N log N) — dominated by sorting the array copy.
 *    - Space Complexity: O(N) — extra space allocated for the sorted copy.
 * 
 * --- APPROACH 3: OPTIMAL (Single-Pass Min/Max Boundary Tracking in O(N) Time) ---
 * 1. The Observation: How can we do this in O(N) time without sorting? 
 *    An element is out of order if it violates the local minimum or maximum from its opposite direction:
 *    - If we scan from left to right, any element smaller than the running maximum we've seen so far *must* be part 
 *    of the unsorted window (since it's out of place relative to the left side). The last such violation gives our right boundary (`right`).
 *    - If we scan from right to left, any element greater than the running minimum we've seen from the right *must* also 
 *    be part of the unsorted window. The last such violation gives our left boundary (`left`).
 * 2. Which one I'd write in an interview: Approach 3 (Min/Max Boundary Tracking). It achieves O(N) time complexity 
 *    and O(1) auxiliary space, satisfying the follow-up requirement elegantly.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — two linear scans (one left-to-right, one right-to-left).
 *    - Space Complexity: O(1) — only scalar variables used for tracking bounds and min/max states.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Already sorted array: `nums = [1, 2, 3, 4]` -> Returns 0.
 * - Entire array unsorted (reversed): `nums = [4, 3, 2, 1]` -> Returns 4.
 * - Single element array: `nums = [1]` -> Returns 0.
 * - Unsorted chunk in the middle: `nums = [2, 6, 4, 8, 10, 9, 15]` -> Subarray `[6, 4, 8, 10, 9]` needs sorting -> Returns 5.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Min/Max Out-of-Order Boundary Detection:
 * 
 * Array: [2, 6, 4, 8, 10, 9, 15]
 * 
 * Pass 1 (Left to Right): Track running max. 
 * - 2 (max=2)
 * - 6 (max=6)
 * - 4 < max(6) -> VIOLATION! Mark right boundary = index 2.
 * - 8 (max=8)
 * - 10 (max=10)
 * - 9 < max(10) -> VIOLATION! Mark right boundary = index 5.
 * Right boundary settles at index 5.
 * 
 * Pass 2 (Right to Left): Track running min.
 * - 15 (min=15)
 * - 9 (min=9)
 * - 10 > min(9) -> VIOLATION! Mark left boundary = index 4.
 * - ... tracking back to 6 > min(4) -> Left boundary settles at index 1.
 * 
 * Subarray range: index 1 to 5 -> Length = 5 - 1 + 1 = 5.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [2, 6, 4, 8, 10, 9, 15]
 * 
 * - Init: n = 7, maxVal = Integer.MIN_VALUE, right = -1, minVal = Integer.MAX_VALUE, left = -1
 * - Pass 1 (Left -> Right):
 *   - i = 0 (2): maxVal = max(-inf, 2) = 2
 *   - i = 1 (6): maxVal = max(2, 6) = 6
 *   - i = 2 (4): 4 < maxVal (6) -> Violation! right = 2
 *   - i = 3 (8): maxVal = max(6, 8) = 8
 *   - i = 4 (10): maxVal = max(8, 10) = 10
 *   - i = 5 (9): 9 < maxVal (10) -> Violation! right = 5
 *   - i = 6 (15): maxVal = max(10, 15) = 15
 *   (right = 5)
 * 
 * - Pass 2 (Right -> Left):
 *   - i = 6 (15): minVal = min(inf, 15) = 15
 *   - i = 5 (9): minVal = min(15, 9) = 9
 *   - i = 4 (10): 10 > minVal (9) -> Violation! left = 4
 *   - i = 3 (8): minVal = min(9, 8) = 8
 *   - i = 2 (4): minVal = min(8, 4) = 4
 *   - i = 1 (6): 6 > minVal (4) -> Violation! left = 1
 *   - i = 0 (2): minVal = min(4, 2) = 2
 *   (left = 1)
 * 
 * - Final Check: If right == -1, return 0. Otherwise, return right - left + 1 -> 5 - 1 + 1 = 5.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To find the shortest continuous unsorted subarray in O(N) time and O(1) space without sorting, we can track 
 * out-of-order elements using running maximum and minimum boundaries. In our first pass from left to right, we maintain 
 * a running maximum. Any element smaller than this running maximum is out of place relative to the left side, so we update 
 * our `right` boundary to that index. In our second pass from right to left, we maintain a running minimum. Any element 
 * larger than this running minimum is out of place relative to the right side, so we update our `left` boundary. 
 * If no violations occur, the array is already sorted, and we return 0. Otherwise, we return `right - left + 1`."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the array has duplicate values? Does this min/max boundary approach still work?
 * A: Yes! The strict inequalities (`nums[i] < maxVal` and `nums[i] > minVal`) correctly handle plateaus and duplicates 
 *    without expanding the window incorrectly.
 */

public class ShortestUnsortedContinuousSubarray {

    /**
     * APPROACH 3: OPTIMAL (O(N) Min/Max Boundary Tracking)
     * Time: O(N), Space: O(1)
     */
    public int findUnsortedSubarray(int[] nums) {
        int n = nums.length;
        if (n <= 1) {
            return 0;
        }

        int maxVal = Integer.MIN_VALUE;
        int right = -1;

        // Phase 1: Scan left to right to find the right boundary of the unsorted window
        for (int i = 0; i < n; i++) {
            if (nums[i] >= maxVal) {
                maxVal = nums[i];
            } else {
                // [What it does]: Found an element smaller than the running maximum from the left. 
                // This element is out of order, so it must be included in our unsorted window.
                right = i;
            }
        }

        // If right remains -1, no violations were found; the array is already sorted.
        if (right == -1) {
            return 0;
        }

        int minVal = Integer.MAX_VALUE;
        int left = -1;

        // Phase 2: Scan right to left to find the left boundary of the unsorted window
        for (int i = n - 1; i >= 0; i--) {
            if (nums[i] <= minVal) {
                minVal = nums[i];
            } else {
                // [What it does]: Found an element larger than the running minimum from the right. 
                // This element is out of order, so it extends our left boundary.
                left = i;
            }
        }

        // [Why needed]: The length of the shortest continuous subarray is (right - left + 1)
        return right - left + 1;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Dual-pass min/max boundary tracking for out-of-order elements.
     * 2. Key observation: Left-to-right max scan finds the right boundary; right-to-left min scan finds the left boundary.
     * 3. Memorize vs Understand: Understand why `nums[i] < maxVal` triggers a right boundary update (out of place from left).
     * 4. Most common trap: Trying to use sorting (O(N log N)) when the follow-up explicitly demands linear O(N) time.
     * 5. Mental trigger: "Shortest unsorted continuous subarray" -> Left-to-right max check + Right-to-left min check.
     */
    public static void main(String[] args) {
        ShortestUnsortedContinuousSubarray solution = new ShortestUnsortedContinuousSubarray();

        // Test Case 1: Standard Example
        int[] nums1 = {2, 6, 4, 8, 10, 9, 15};
        System.out.println("Test 1 (Standard): " + solution.findUnsortedSubarray(nums1)); // Expected: 5 ([6, 4, 8, 10, 9])

        // Test Case 2: Already Sorted Array
        int[] nums2 = {1, 2, 3, 4};
        System.out.println("Test 2 (Already sorted): " + solution.findUnsortedSubarray(nums2)); // Expected: 0

        // Test Case 3: Entire Array Unsorted
        int[] nums3 = {4, 3, 2, 1};
        System.out.println("Test 3 (Fully reversed): " + solution.findUnsortedSubarray(nums3)); // Expected: 4

        // Test Case 4: Single Element
        int[] nums4 = {1};
        System.out.println("Test 4 (Single element): " + solution.findUnsortedSubarray(nums4)); // Expected: 0
    }
}


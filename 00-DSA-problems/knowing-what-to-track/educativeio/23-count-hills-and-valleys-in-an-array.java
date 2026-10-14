/**
 * STANDALONE STUDY NOTE: COUNT HILLS AND VALLEYS IN AN ARRAY
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can the array contain fewer than 3 elements?" (Constraints state 3 <= nums.length <= 100, so a hill or valley can always theoretically exist).
 * - "How are consecutive duplicate elements handled?" (The prompt states equal adjacent values are ignored by looking outward to nearest different values, and a plateau/multi-element peak is counted only once).
 * - "Can elements at the very beginning or end of the array be hills or valleys?" (No, a hill or valley requires both a left and right non-equal neighbor, which endpoints lack).
 * - "What is the return type?" (An integer representing the total count of hills and valleys).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to count how many elements in an array are strictly greater than both their closest non-equal left 
 * and right neighbors (hills), or strictly smaller than both (valleys), counting plateaus only once. 
 * The binding constraint is that scanning outward for every single index to find the nearest non-equal element 
 * leads to redundant O(N^2) work.
 * 
 * --- APPROACH 1: BRUTE FORCE (Outward Scanning per Index) ---
 * 1. What I'd naturally try: For every index `i`, scan left until finding a non-equal element `l`, and scan right 
 *    until finding a non-equal element `r`. Then check if `nums[i]` is strictly greater than (or smaller than) both.
 * 2. Why it works: It directly follows the literal wording of the problem statement.
 * 3. Why it's too slow/costly: If the array has long plateaus of identical numbers, scanning outward for every index 
 *    takes quadratic time O(N^2).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^2) — because outward scans can traverse up to N elements for multiple indices.
 *    - Space Complexity: O(1) — constant extra space.
 * 
 * --- APPROACH 2: OPTIMAL (Consecutive Duplicate Compression / Two-Pointer Stream Scan) ---
 * 1. The Observation: The outward-scanning requirement for plateaus is completely equivalent to *removing consecutive duplicates* 
 *    from the array! Once consecutive duplicates are compressed into a single representative element, every element 
 *    at index `i` (from 1 to length - 2) automatically has distinct left and right neighbors.
 * 2. How it leads to the next step: We can either explicitly build a compressed array (or list) omitting adjacent duplicates, 
 *    or simply iterate through the array with an index pointer that skips elements equal to their predecessor. 
 *    For each valid middle element, check if it's greater than both neighbors (hill) or smaller than both (valley).
 * 3. Which one I'd write in an interview: Approach 2 (Stream Scan with duplicate skipping). It runs in O(N) time 
 *    and O(1) auxiliary space, requiring zero extra allocations.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we make a single linear pass over the `nums` array.
 *    - Space Complexity: O(1) — because we only use scalar counter variables and indices.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - No hills or valleys (strictly monotonic or flat): `nums = [1, 2, 3, 4, 5]` or `[2, 2, 2]` -> Returns 0.
 * - Minimum length array ($n = 3$): `nums = [2, 4, 1]` -> Returns 1 (hill at 4).
 * - Multiple alternating hills and valleys: `nums = [2, 4, 1, 3, 1, 2]` -> Correctly counts multiple peaks/troughs.
 * - Long plateaus: `nums = [2, 2, 4, 4, 4, 1, 1, 3]` -> Compressed to `[2, 4, 1, 3]`, counts correctly.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Plateau Compression Principle:
 * 
 * Original Array: [2, 4, 4, 4, 1, 1, 3]
 * 
 * Step 1: Collapse adjacent duplicates
 * Compressed:     [2,    4,    1,    3]
 * 
 * Step 2: Check local peaks and troughs on compressed array:
 * - '4': 2 < 4 > 1 (Hill! Count = 1)
 * - '1': 4 > 1 < 3 (Valley! Count = 2)
 * 
 * Total Hills & Valleys = 2.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [2, 4, 1, 1, 3]
 * 
 * - Initialize count = 0
 * - Iterate `i` from 1 to `nums.length - 2` (indices 1 through 3):
 *   - i = 1 (val = 4): 
 *     - Find left non-equal: index 0 (val = 2)
 *     - Find right non-equal: index 2 (val = 1) [Wait, if we use the duplicate-skipping pointer method:]
 *   - Let's trace the duplicate-skipping pointer approach:
 *     - We maintain a left reference `left = 0`.
 *     - As we iterate `i` from 1 to `n - 2`:
 *       - If `nums[i] == nums[i + 1]`, continue (skip plateau elements).
 *       - Otherwise (meaning `nums[i]` is the rightmost element of a plateau, or a single unique element):
 *         - Check if `nums[i]` is greater than both `nums[left]` and `nums[i + 1]` (Hill) 
 *           OR smaller than both `nums[left]` and `nums[i + 1]` (Valley).
 *         - If yes, increment `count++`.
 *         - Update `left = i`.
 * 
 * Let's dry run this exact pointer algorithm on `nums = [2, 4, 1, 1, 3]`:
 * - `left = 0` (nums[0] = 2)
 * - i = 1 (nums[1] = 4):
 *   - nums[1] == nums[2] (4 == 1)? False.
 *   - Check hill/valley for nums[1] (4): left neighbor is nums[left] (2), right neighbor is nums[2] (1).
 *   - Is 4 > 2 and 4 > 1? YES (Hill)! count = 1.
 *   - `left = 1`.
 * - i = 2 (nums[2] = 1):
 *   - nums[2] == nums[3] (1 == 1)? TRUE! `i` increments or we continue. Wait, if `nums[i] == nums[i+1]`, we skip so that `i` stops at the last duplicate of the plateau!
 *   Let's refine the loop: `for (int i = 1; i < n - 1; i++) { if (nums[i] == nums[i + 1]) continue; ... }`
 *   - Let's trace with `nums = [2, 4, 1, 1, 3]` ($n = 5$):
 *     - i = 1 (val = 4): nums[1] == nums[2] (4 == 1)? False.
 *       - Left neighbor: we need the previous distinct element. If we track `left` as the index of the previous distinct element:
 *         - Left val = nums[left] (2), Current val = nums[1] (4), Right val = nums[2] (1).
 *         - 4 > 2 and 4 > 1 -> Hill! `count++` (count = 1).
 *         - Update `left = 1`.
 *     - i = 2 (val = 1): nums[2] == nums[3] (1 == 1)? TRUE! `continue` (skip first 1).
 *     - i = 3 (val = 1): nums[3] == nums[4] (1 == 3)? False.
 *       - Left neighbor = nums[left] (nums[1] = 4). Current val = nums[3] (1). Right neighbor = nums[4] (3).
 *       - Is 1 < 4 and 1 < 3? TRUE (Valley)! `count++` (count = 2).
 *       - Update `left = 3`.
 * Result: 2. This pointer logic is extremely clean and runs in O(N) time with O(1) space!
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To count hills and valleys while ignoring equal adjacent values, we can observe that plateaus act as a single 
 * representative element. We can iterate through the array using a pointer `i` from index 1 up to `n - 2`. 
 * If `nums[i]` is equal to `nums[i + 1]`, we skip it because it's part of a plateau. When we reach the defining element 
 * of that plateau or a unique element, we compare it against our last recorded distinct left neighbor and the immediate 
 * right neighbor `nums[i + 1]`. If it is strictly greater than both or strictly smaller than both, we increment our count 
 * and update our left neighbor pointer. This gives us O(N) time complexity and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the array is circular (i.e., the first and last elements wrap around)?
 * A: We would need to account for wrap-around neighbors when checking indices 0 and `n - 1`, modifying the boundary 
 *    conditions accordingly.
 * 
 * Q: How would you modify this to return the indices of all hills and valleys instead of just the count?
 * A: We would store the indices in an `List<Integer>` whenever a hill or valley condition is met during the iteration.
 */

public class CountHillsAndValleysInAnArray {

    /**
     * APPROACH 2: OPTIMAL (Single-Pass Plateau Compression Pointer)
     * Time: O(N), Space: O(1)
     */
    public int countHillValley(int[] nums) {
        int count = 0;
        int left = 0; // Tracks the index of the most recent distinct non-equal element to the left
        int n = nums.length;

        // Iterate through elements from index 1 up to n - 2
        for (int i = 1; i < n - 1; i++) {
            // [What it does]: If the current element equals the next element, it's part of a plateau. 
            // We skip it so we evaluate only the defining representative of the plateau.
            if (nums[i] == nums[i + 1]) {
                continue;
            }

            // [Why needed]: Check if nums[i] is a Hill (strictly greater than both neighbors)
            boolean isHill = (nums[i] > nums[left]) && (nums[i] > nums[i + 1]);

            // [Why needed]: Check if nums[i] is a Valley (strictly smaller than both neighbors)
            boolean isValley = (nums[i] < nums[left]) && (nums[i] < nums[i + 1]);

            if (isHill || isValley) {
                count++;
            }

            // [What it does]: Update the left pointer to the current element before moving forward, 
            // since it is now the closest distinct non-equal left neighbor for subsequent elements.
            left = i;
        }

        return count;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Plateau compression via state-tracking pointers (`left` and `i`).
     * 2. Key observation: Equal adjacent values can be skipped by checking `nums[i] == nums[i + 1]`, reducing the problem 
     *    to examining local peaks and troughs on compressed data.
     * 3. Memorize vs Understand: Understand why `left` is updated to `i` only when a distinct neighbor transition is evaluated.
     * 4. Most common trap: Forgetting to skip plateaus (`nums[i] == nums[i + 1]`), which would count multi-element peaks multiple times.
     * 5. Mental trigger: "Count hills and valleys with plateaus" -> Single loop + plateau skipping (`nums[i] == nums[i+1]`) + `left` pointer.
     */
    public static void main(String[] args) {
        CountHillsAndValleysInAnArray solution = new CountHillsAndValleysInAnArray();

        // Test Case 1: Standard Example with Plateau
        int[] nums1 = {2, 4, 1, 1, 3};
        // 4 is a hill (left 2, right 1). 1 is a valley (left 4, right 3). Total = 2.
        System.out.println("Test 1 (2, 4, 1, 1, 3): " + solution.countHillValley(nums1)); // Expected: 2

        // Test Case 2: Plateau at Peak
        int[] nums2 = {6, 6, 5, 5, 4, 1};
        // No hills or valleys (strictly decreasing or flat). Total = 0.
        System.out.println("Test 2 (6, 6, 5, 5, 4, 1): " + solution.countHillValley(nums2)); // Expected: 0

        // Test Case 3: Multiple Hills and Valleys
        int[] nums3 = {2, 4, 1, 3, 1, 2};
        System.out.println("Test 3 (Complex): " + solution.countHillValley(nums3)); // Expected: 4
    }
}



class Solution {
    public int countHillValley(int[] nums) {
        int count = 0;

        // Start at index 1 because the first element cannot be a hill or valley.
        // The last element also cannot qualify because it lacks a right neighbor.
        for (int i = 1; i < nums.length - 1; i++) {

            // Ignore duplicate values immediately to the left.
            // We need the nearest value that differs from nums[i].
            if (nums[i] == nums[i - 1]) {
                continue;
            }

            // Find the nearest different value to the right.
            int right = i + 1;

            while (right < nums.length && nums[right] == nums[i]) {
                right++;
            }

            // If no different right neighbor exists, this index cannot qualify.
            if (right == nums.length) {
                continue;
            }

            // Compare with the nearest different neighbors.
            boolean isHill =
                    nums[i] > nums[i - 1] && nums[i] > nums[right];

            boolean isValley =
                    nums[i] < nums[i - 1] && nums[i] < nums[right];

            // Count the index if it is either a hill or a valley.
            if (isHill || isValley) {
                count++;
            }
        }

        return count;
    }
}



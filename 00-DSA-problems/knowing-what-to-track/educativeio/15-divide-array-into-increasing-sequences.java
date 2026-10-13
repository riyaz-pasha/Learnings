/**
 * STANDALONE STUDY NOTE: DIVIDE ARRAY INTO INCREASING SEQUENCES (PARTITION INTO DISJOINT INCREASING SUBSEQUENCES)
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Are the subsequences required to be *strictly* increasing (no duplicate values allowed within the same subsequence)?" 
 *   (Yes, standard definition of increasing subsequence means adjacent elements cannot be equal).
 * - "Must every element in the array be used exactly once?" (Yes, it's a partition of all elements into disjoint subsets).
 * - "Is the input array guaranteed to be sorted in non-decreasing order?" (Yes, the constraints explicitly state that nums is sorted, 
 *   which is a massive hint that we can exploit consecutive groupings).
 * - "What is the minimum length required for each subsequence?" (At least k).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to partition a sorted array into one or more disjoint strictly increasing subsequences, each of length at least k. 
 * The binding constraint is that since subsequences must be *strictly* increasing, any two identical numbers (duplicates) 
 * cannot reside in the same subsequence. They must be split across different subsequences.
 * 
 * --- APPROACH 1: BRUTE FORCE (Backtracking / Subsequence Simulation) ---
 * 1. What I'd naturally try: Try to greedily or recursively assign each element of `nums` into one of several active 
 *    increasing subsequences, ensuring that each final subsequence has length >= k.
 * 2. Why it works: It explores actual partition assignments.
 * 3. Why it's too slow/costly: The number of ways to partition elements into multiple overlapping sequence states grows 
 *    exponentially, leading to TLE (Time Limit Exceeded).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(2^N) or worse due to exponential state space search.
 *    - Space Complexity: O(N) for recursion and tracking subsequence states.
 * 
 * --- APPROACH 2: INTERMEDIATE (Greedy Heap / Simulated Bucket Placement) ---
 * 1. What I'd naturally try: Maintain a collection of active increasing subsequences using a priority queue. 
 *    For each number in `nums`, find an existing subsequence whose tail is less than the current number, append it, 
 *    and at the end check if all subsequences have length >= k.
 * 2. Why it works: It actively builds valid subsequences.
 * 3. Why it's too slow/costly: While better than pure backtracking, maintaining heap states and handling tie-breaking 
 *    takes O(N log N) time and is overly complex when a direct mathematical property governs feasibility.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N log N) — due to heap operations for active subsequences.
 *    - Space Complexity: O(N) — to store active subsequence tails and lengths in a priority queue.
 * 
 * --- APPROACH 3: OPTIMAL (Maximum Frequency Counting Reduction) ---
 * 1. The Observation: Because the array is already sorted, all identical elements appear contiguously. 
 *    Let max_freq be the maximum frequency of any single value in the array. 
 *    - Since a subsequence must be *strictly* increasing, all max_freq instances of that most frequent value 
 *      must be placed into *different* subsequences. 
 *    - Therefore, we need at least max_freq disjoint subsequences.
 *    - Since every subsequence requires at least k elements, the total number of elements required is at least max_freq * k.
 *    - If total elements (n) is less than max_freq * k, it's mathematically impossible. Otherwise, it is always possible!
 * 2. How it leads to the next step: We iterate through the sorted array once to find the maximum frequency of any 
 *    consecutive duplicate group, multiply it by k, and compare it against `nums.length`.
 * 3. Which one I'd write in an interview: Approach 3. It transforms a complex partitioning problem into an O(N) 
 *    counting problem with O(1) extra space, showcasing deep analytical reduction skills.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we make a single linear pass to find the maximum frequency of consecutive elements.
 *    - Space Complexity: O(1) — because we only use scalar variables to track counts, requiring zero auxiliary memory structures.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Array length smaller than k (`nums.length < k`): Automatically returns false since no subsequence can reach length k.
 * - All elements identical (`nums = [2, 2, 2, 2], k = 3`): Max frequency = 4. 4 * 3 = 12 > 4 -> Returns false.
 * - All elements unique (`nums = [1, 2, 3, 4], k = 2`): Max frequency = 1. 1 * 2 = 2 <= 4 -> Returns true.
 * - Exact fit (`nums = [1, 2, 2, 3, 3, 3], k = 3`): Max frequency = 3 (for '3'). 3 * 3 = 9 > 6 -> Wait, if nums has 6 elements 
 *   and max freq is 3 with k=3, 3 * 3 = 9 > 6, so returns false. Correct, because you need 3 sequences of length 3 = 9 elements!
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Pigeonhole Frequency Bottleneck:
 * 
 * Sorted Array: [1, 2, 2, 2, 3, 4]  (k = 3)
 * Group frequencies:
 * - 1 appears 1 time
 * - 2 appears 3 times  <-- Maximum Frequency (max_freq = 3)
 * - 3 appears 1 time
 * - 4 appears 1 time
 * 
 * Since the three 2s cannot occupy the same strictly increasing subsequence, we must create at least 3 separate subsequences:
 * Subsequence 1: [1, 2, 3] (length 3 >= k)
 * Subsequence 2: [2, 4]    (length 2 < k) -> FAILS minimum length requirement!
 * 
 * Feasibility Check Formula: max_freq * k <= nums.length
 * 3 * 3 = 9 <= 6 -> FALSE.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [1, 2, 2, 3, 3, 3, 4, 4, 5], k = 3
 * 
 * - Initialize maxFreq = 0, currentFreq = 0
 * - Iterate through array:
 *   - i = 0 (val = 1): currentFreq = 1, maxFreq = max(0, 1) = 1
 *   - i = 1 (val = 2): currentFreq = 1
 *   - i = 2 (val = 2): currentFreq = 2, maxFreq = max(1, 2) = 2
 *   - i = 3 (val = 3): currentFreq = 1
 *   - i = 4 (val = 3): currentFreq = 2
 *   - i = 5 (val = 3): currentFreq = 3, maxFreq = max(2, 3) = 3
 *   - i = 6 (val = 4): currentFreq = 1
 *   - i = 7 (val = 4): currentFreq = 2
 *   - i = 8 (val = 5): currentFreq = 1
 * 
 * Final maxFreq = 3.
 * Check condition: maxFreq * k <= nums.length
 * 3 * 3 = 9. Total elements = 9.
 * 9 <= 9 is TRUE! Returns true.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "Since the array is already sorted in non-decreasing order, all duplicate elements are clustered contiguously. 
 * Because each subsequence must be strictly increasing, any two identical numbers cannot belong to the same subsequence. 
 * Therefore, if an element appears max_freq times, we are forced to create at least max_freq disjoint subsequences. 
 * Since every subsequence requires at least k elements, we need a total of at least max_freq * k elements. 
 * Thus, feasibility reduces to checking whether max_freq * k <= nums.length. This allows us to solve the problem 
 * in O(N) time with O(1) auxiliary space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the input array was NOT sorted?
 * A: We would have to count element frequencies using a `HashMap` or an auxiliary frequency array first to find 
 *    max_freq, which still takes O(N) time, preserving the optimal time complexity.
 * 
 * Q: How would you actually construct and return the partitioned subsequences instead of just returning a boolean?
 * A: We could use a greedy queue or list of active subsequences. When processing element `v`, we append `v` to the 
 *    first available active subsequence whose last element is strictly less than `v`. If none exist, we start a new subsequence.
 */

public class PartitionArrayIntoDisjointIncreasingSubsequences {

    /**
     * APPROACH 3: OPTIMAL (Maximum Frequency Counting Reduction)
     * Time: O(N), Space: O(1)
     */
    public boolean canDivideIntoSubsequences(int[] nums, int k) {
        if (nums == null || nums.length == 0) {
            return false;
        }

        int n = nums.length;
        int maxFreq = 0;
        int currentFreq = 0;

        // Phase 1: Find the maximum frequency of any consecutive identical element group
        // Because nums is sorted, identical elements are always adjacent.
        for (int i = 0; i < n; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) {
                currentFreq++;
            } else {
                currentFreq = 1; // Reset frequency for a new distinct value
            }
            maxFreq = Math.max(maxFreq, currentFreq);
        }

        // Phase 2: Feasibility check
        // maxFreq represents the minimum number of disjoint strictly increasing subsequences required 
        // (since all instances of the most frequent element must be in separate subsequences).
        // k is the minimum length required per subsequence.
        // Therefore, total elements needed = maxFreq * k.
        return maxFreq * k <= n;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Pigeonhole frequency bottleneck reduction (`max_freq * k <= n`).
     * 2. Key observation: Sorted arrays cluster duplicates together; strictly increasing rules force duplicates 
     *    into separate subsequences.
     * 3. Memorize vs Understand: Understand why `max_freq` determines the minimum number of independent subsequences needed.
     * 4. Most common trap: Forgetting that subsequences must be *strictly* increasing, meaning equal elements cannot share a subsequence.
     * 5. Mental trigger: "Partition into disjoint increasing subsequences" -> Count max consecutive frequency + Check `max_freq * k <= n`.
     */
    public static void main(String[] args) {
        PartitionArrayIntoDisjointIncreasingSubsequences solution = new PartitionArrayIntoDisjointIncreasingSubsequences();

        // Test Case 1: Standard Feasible Case
        int[] nums1 = {1, 2, 2, 3, 3, 3, 4, 4, 5};
        int k1 = 3;
        System.out.println("Test 1 (Feasible): " + solution.canDivideIntoSubsequences(nums1, k1)); // Expected: true

        // Test Case 2: Infeasible Case (Max freq requires too many elements)
        int[] nums2 = {1, 2, 2, 3, 3, 3, 4, 4, 5};
        int k2 = 4;
        System.0ut.println("Test 2 (Infeasible due to k=4): " + solution.canDivideIntoSubsequences(nums2, k2)); // Expected: false

        // Test Case 3: All Unique Elements
        int[] nums3 = {1, 2, 3, 4, 5};
        int k3 = 2;
        System.out.println("Test 3 (All unique): " + solution.canDivideIntoSubsequences(nums3, k3)); // Expected: true

        // Test Case 4: Array too short for k
        int[] nums4 = {1, 2};
        int k4 = 3;
        System.out.println("Test 4 (Array shorter than k): " + solution.canDivideIntoSubsequences(nums4, k4)); // Expected: false
    }
}


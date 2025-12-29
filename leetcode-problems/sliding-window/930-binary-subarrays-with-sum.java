import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BinarySubarraySum {
    
    /**
     * APPROACH 1: Prefix Sum with HashMap (Optimal)
     * 
     * INTUITION:
     * - Use prefix sums to convert subarray sum problem to difference problem
     * - If prefixSum[j] - prefixSum[i] = goal, then subarray [i+1...j] has sum goal
     * - Track how many times each prefix sum has occurred
     * 
     * REASONING:
     * - prefixSum[i] = sum of elements from index 0 to i
     * - For subarray [i+1...j]: sum = prefixSum[j] - prefixSum[i]
     * - We want: prefixSum[j] - prefixSum[i] = goal
     * - Rearrange: prefixSum[i] = prefixSum[j] - goal
     * - So count how many times (prefixSum[j] - goal) appeared before
     * 
     * KEY INSIGHT:
     * For each position j, count how many positions i exist where
     * prefixSum[i] = prefixSum[j] - goal
     * 
     * TIME COMPLEXITY: O(n) - single pass through array
     * SPACE COMPLEXITY: O(n) - HashMap stores prefix sums
     */
    public int numSubarraysWithSum_PrefixSum(int[] nums, int goal) {
        Map<Integer, Integer> prefixCount = new HashMap<>();
        // Base case: prefix sum of 0 (empty prefix) occurs once
        prefixCount.put(0, 1);
        
        int prefixSum = 0;
        int count = 0;
        
        for (int num : nums) {
            // Calculate current prefix sum
            prefixSum += num;
            
            // Check if (prefixSum - goal) exists
            // If yes, those positions can be starting points for subarrays ending here
            int target = prefixSum - goal;
            count += prefixCount.getOrDefault(target, 0);
            
            // Add current prefix sum to map
            prefixCount.put(prefixSum, prefixCount.getOrDefault(prefixSum, 0) + 1);
        }
        
        return count;
    }
    
    /**
     * APPROACH 2: Sliding Window Transformation (Clever!)
     * 
     * INTUITION:
     * - Direct sliding window is hard because we need to count subarrays
     * - Transform: count(sum = goal) = count(sum ≤ goal) - count(sum ≤ goal-1)
     * - Use sliding window to count subarrays with sum ≤ X
     * 
     * REASONING:
     * - Subarrays with sum exactly goal = 
     *   (subarrays with sum ≤ goal) - (subarrays with sum ≤ goal-1)
     * - Sliding window easily counts "at most X" problems
     * - This transforms "exactly X" into two "at most X" problems
     * 
     * WHY THIS WORKS:
     * - If goal = 2:
     *   - Sum ≤ 2: includes sums {0, 1, 2}
     *   - Sum ≤ 1: includes sums {0, 1}
     *   - Difference: only sum = 2
     * 
     * TIME COMPLEXITY: O(n) - two passes (one for each helper call)
     * SPACE COMPLEXITY: O(1) - only using a few variables
     */
    public int numSubarraysWithSum_SlidingWindow(int[] nums, int goal) {
        // Handle edge case: goal = 0
        if (goal == 0) {
            return countAtMost(nums, 0);
        }
        return countAtMost(nums, goal) - countAtMost(nums, goal - 1);
    }
    
    /**
     * Helper: Count subarrays with sum at most 'target'
     * 
     * For each position right:
     * - Expand window to include nums[right]
     * - Shrink window if sum exceeds target
     * - All subarrays ending at right (starting from any position in [left, right])
     *   have sum ≤ target, so add (right - left + 1) to count
     */
    private int countAtMost(int[] nums, int target) {
        if (target < 0) return 0;
        
        int left = 0;
        int sum = 0;
        int count = 0;
        
        for (int right = 0; right < nums.length; right++) {
            sum += nums[right];
            
            // Shrink window if sum exceeds target
            while (sum > target) {
                sum -= nums[left];
                left++;
            }
            
            // All subarrays from [left...right], [left+1...right], ... [right...right]
            // have sum ≤ target, so add (right - left + 1)
            count += right - left + 1;
        }
        
        return count;
    }
    
    /**
     * APPROACH 3: Three Pointers / Sliding Window Direct
     * 
     * INTUITION:
     * - Maintain window with sum = goal
     * - Count how many ways we can form such a window
     * - Track prefix zeros to count variations
     * 
     * REASONING:
     * - When window [left, right] has sum = goal
     * - If there are k zeros at the start of window, we have k+1 valid subarrays
     * - Example: [0,0,1,1] with goal=2
     *   - Can start at index 0: [0,0,1,1]
     *   - Can start at index 1: [0,1,1]
     *   - Can start at index 2: [1,1]
     *   - That's 3 subarrays (2 leading zeros + 1)
     * 
     * TIME COMPLEXITY: O(n) - each element visited at most twice
     * SPACE COMPLEXITY: O(1) - only variables
     */
    public int numSubarraysWithSum_ThreePointers(int[] nums, int goal) {
        int left = 0;
        int prefixZeros = 0;
        int sum = 0;
        int count = 0;
        
        for (int right = 0; right < nums.length; right++) {
            sum += nums[right];
            
            // Shrink window if sum exceeds goal
            while (left < right && (nums[left] == 0 || sum > goal)) {
                if (nums[left] == 0) {
                    prefixZeros++;
                } else {
                    prefixZeros = 0;
                }
                sum -= nums[left];
                left++;
            }
            
            // If sum equals goal, count all valid starting positions
            if (sum == goal) {
                count += prefixZeros + 1;
            }
        }
        
        return count;
    }
    
    /**
     * APPROACH 4: Prefix Sum Array (Straightforward)
     * 
     * INTUITION:
     * - Build prefix sum array explicitly
     * - For each pair (i, j), check if prefixSum[j] - prefixSum[i] = goal
     * 
     * REASONING:
     * - prefixSum[i] = sum of nums[0...i-1]
     * - Sum of subarray [i...j] = prefixSum[j+1] - prefixSum[i]
     * - Count all pairs where this equals goal
     * 
     * TIME COMPLEXITY: O(n²) - nested loops
     * SPACE COMPLEXITY: O(n) - prefix sum array
     */
    public int numSubarraysWithSum_PrefixArray(int[] nums, int goal) {
        int n = nums.length;
        int[] prefixSum = new int[n + 1];
        
        // Build prefix sum array
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i];
        }
        
        int count = 0;
        
        // Check all pairs
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (prefixSum[j + 1] - prefixSum[i] == goal) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * APPROACH 5: Brute Force (For Understanding)
     * 
     * INTUITION:
     * - Try every possible subarray
     * - Calculate sum and check if it equals goal
     * 
     * REASONING:
     * - Simple nested loop
     * - Good for verification and understanding
     * 
     * TIME COMPLEXITY: O(n²) - try all subarrays
     * SPACE COMPLEXITY: O(1) - no extra space
     */
    public int numSubarraysWithSum_BruteForce(int[] nums, int goal) {
        int count = 0;
        
        for (int start = 0; start < nums.length; start++) {
            int sum = 0;
            for (int end = start; end < nums.length; end++) {
                sum += nums[end];
                if (sum == goal) {
                    count++;
                }
                // Optimization: if sum exceeds goal in binary array, no point continuing
                if (sum > goal) {
                    break;
                }
            }
        }
        
        return count;
    }
    
    /**
     * APPROACH 6: Detailed Explanation Version (Prefix Sum)
     * 
     * Same as Approach 1 but with extensive comments
     */
    public int numSubarraysWithSum_Explained(int[] nums, int goal) {
        // Map: prefix_sum -> count of times it occurred
        Map<Integer, Integer> prefixCount = new HashMap<>();
        
        // IMPORTANT: Initialize with 0 -> 1
        // This handles subarrays starting from index 0
        // When prefixSum = goal, we need prefixSum - goal = 0 to exist
        prefixCount.put(0, 1);
        
        int prefixSum = 0;   // Running sum from start
        int count = 0;       // Number of valid subarrays found
        
        // Process each element
        for (int i = 0; i < nums.length; i++) {
            // STEP 1: Update prefix sum
            prefixSum += nums[i];
            
            // STEP 2: Check how many previous positions can form valid subarrays
            // We want: prefixSum[current] - prefixSum[previous] = goal
            // Rearrange: prefixSum[previous] = prefixSum[current] - goal
            int target = prefixSum - goal;
            
            // STEP 3: Add count of all positions with prefix sum = target
            // Each such position can be a starting point for a subarray ending here
            if (prefixCount.containsKey(target)) {
                count += prefixCount.get(target);
            }
            
            // STEP 4: Record current prefix sum for future positions
            prefixCount.put(prefixSum, prefixCount.getOrDefault(prefixSum, 0) + 1);
        }
        
        return count;
    }
    
    /**
     * Visualization helper
     */
    public void visualizeSolution(int[] nums, int goal) {
        System.out.println("Input: nums = " + Arrays.toString(nums) + ", goal = " + goal);
        System.out.println("Finding all subarrays with sum = " + goal + "\n");
        
        List<String> subarrays = new ArrayList<>();
        
        for (int start = 0; start < nums.length; start++) {
            int sum = 0;
            for (int end = start; end < nums.length; end++) {
                sum += nums[end];
                if (sum == goal) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    for (int k = start; k <= end; k++) {
                        sb.append(nums[k]);
                        if (k < end) sb.append(",");
                    }
                    sb.append("] (indices ").append(start).append("-").append(end).append(")");
                    subarrays.add(sb.toString());
                }
            }
        }
        
        System.out.println("Found " + subarrays.size() + " subarrays:");
        for (String subarray : subarrays) {
            System.out.println("  " + subarray);
        }
        System.out.println();
    }
    
    /**
     * Comprehensive test cases
     */
    public static void main(String[] args) {
        BinarySubarraySum solution = new BinarySubarraySum();
        
        // Test Case 1
        int[] nums1 = {1, 0, 1, 0, 1};
        int goal1 = 2;
        System.out.println("=== Test Case 1 ===");
        solution.visualizeSolution(nums1, goal1);
        System.out.println("Prefix Sum: " + solution.numSubarraysWithSum_PrefixSum(nums1, goal1));
        System.out.println("Sliding Window: " + solution.numSubarraysWithSum_SlidingWindow(nums1, goal1));
        System.out.println("Three Pointers: " + solution.numSubarraysWithSum_ThreePointers(nums1, goal1));
        System.out.println("Prefix Array: " + solution.numSubarraysWithSum_PrefixArray(nums1, goal1));
        System.out.println("Brute Force: " + solution.numSubarraysWithSum_BruteForce(nums1, goal1));
        System.out.println();
        
        // Test Case 2: All zeros
        int[] nums2 = {0, 0, 0, 0, 0};
        int goal2 = 0;
        System.out.println("=== Test Case 2 (All zeros) ===");
        solution.visualizeSolution(nums2, goal2);
        System.out.println("Prefix Sum: " + solution.numSubarraysWithSum_PrefixSum(nums2, goal2));
        System.out.println("Sliding Window: " + solution.numSubarraysWithSum_SlidingWindow(nums2, goal2));
        System.out.println("Three Pointers: " + solution.numSubarraysWithSum_ThreePointers(nums2, goal2));
        System.out.println("Prefix Array: " + solution.numSubarraysWithSum_PrefixArray(nums2, goal2));
        System.out.println("Brute Force: " + solution.numSubarraysWithSum_BruteForce(nums2, goal2));
        System.out.println();
        
        // Test Case 3: Goal = 0 with mixed array
        int[] nums3 = {1, 0, 0, 1, 0};
        int goal3 = 0;
        System.out.println("=== Test Case 3 (Goal = 0, mixed) ===");
        solution.visualizeSolution(nums3, goal3);
        System.out.println("Prefix Sum: " + solution.numSubarraysWithSum_PrefixSum(nums3, goal3));
        System.out.println("Sliding Window: " + solution.numSubarraysWithSum_SlidingWindow(nums3, goal3));
        System.out.println("Three Pointers: " + solution.numSubarraysWithSum_ThreePointers(nums3, goal3));
        System.out.println("Brute Force: " + solution.numSubarraysWithSum_BruteForce(nums3, goal3));
        System.out.println();
        
        // Test Case 4: All ones
        int[] nums4 = {1, 1, 1, 1};
        int goal4 = 2;
        System.out.println("=== Test Case 4 (All ones) ===");
        solution.visualizeSolution(nums4, goal4);
        System.out.println("Prefix Sum: " + solution.numSubarraysWithSum_PrefixSum(nums4, goal4));
        System.out.println("Sliding Window: " + solution.numSubarraysWithSum_SlidingWindow(nums4, goal4));
        System.out.println("Three Pointers: " + solution.numSubarraysWithSum_ThreePointers(nums4, goal4));
        System.out.println("Brute Force: " + solution.numSubarraysWithSum_BruteForce(nums4, goal4));
        System.out.println();
        
        // Test Case 5: Single element
        int[] nums5 = {1};
        int goal5 = 1;
        System.out.println("=== Test Case 5 (Single element) ===");
        solution.visualizeSolution(nums5, goal5);
        System.out.println("Prefix Sum: " + solution.numSubarraysWithSum_PrefixSum(nums5, goal5));
        System.out.println("Sliding Window: " + solution.numSubarraysWithSum_SlidingWindow(nums5, goal5));
        System.out.println("Three Pointers: " + solution.numSubarraysWithSum_ThreePointers(nums5, goal5));
        System.out.println();
        
        // Test Case 6: Goal larger than possible
        int[] nums6 = {0, 0, 1, 0, 1};
        int goal6 = 5;
        System.out.println("=== Test Case 6 (Goal > sum) ===");
        solution.visualizeSolution(nums6, goal6);
        System.out.println("Prefix Sum: " + solution.numSubarraysWithSum_PrefixSum(nums6, goal6));
        System.out.println("Sliding Window: " + solution.numSubarraysWithSum_SlidingWindow(nums6, goal6));
        System.out.println("Three Pointers: " + solution.numSubarraysWithSum_ThreePointers(nums6, goal6));
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * COMPREHENSIVE ANALYSIS - BINARY SUBARRAYS WITH SUM
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * 1. PROBLEM TYPE: Subarray Counting (Different from Length Problems!)
 *    
 *    Previous problems: Find LENGTH of longest/shortest subarray
 *    This problem: COUNT how many subarrays satisfy condition
 *    
 *    Key difference: We need to count ALL valid subarrays, not just find one
 * 
 * 2. WHY PREFIX SUM IS PERFECT HERE:
 *    
 *    Core equation: sum[i...j] = prefixSum[j] - prefixSum[i-1]
 *    
 *    We want: sum[i...j] = goal
 *    Therefore: prefixSum[j] - prefixSum[i-1] = goal
 *    Rearrange: prefixSum[i-1] = prefixSum[j] - goal
 *    
 *    Example walkthrough with nums = [1,0,1,0,1], goal = 2:
 *    
 *    Index:        -1   0   1   2   3   4
 *    Value:             1   0   1   0   1
 *    PrefixSum:     0   1   1   2   2   3
 *    
 *    At index 2 (prefixSum = 2):
 *      - Want: prefixSum - goal = 2 - 2 = 0
 *      - Found 0 at index -1
 *      - Subarray [0...2] = [1,0,1] has sum 2 ✓
 *    
 *    At index 3 (prefixSum = 2):
 *      - Want: prefixSum - goal = 2 - 2 = 0
 *      - Found 0 at index -1
 *      - Subarray [0...3] = [1,0,1,0] has sum 2 ✓
 *    
 *    At index 4 (prefixSum = 3):
 *      - Want: prefixSum - goal = 3 - 2 = 1
 *      - Found 1 at index 0 and index 1 (2 times!)
 *      - Subarrays [1...4] and [2...4] both have sum 2 ✓✓
 *    
 *    Total: 4 subarrays
 * 
 * 3. SLIDING WINDOW TRANSFORMATION (Mind-Bending!):
 *    
 *    Direct sliding window for "exactly X" is hard for counting problems.
 *    But we can transform it!
 *    
 *    count(sum = goal) = count(sum ≤ goal) - count(sum ≤ goal-1)
 *    
 *    Why? Set theory:
 *    ┌─────────────────────────────────────────────────────────────────────┐
 *    │ Subarrays with sum ≤ goal    = {0, 1, 2, ..., goal}                │
 *    │ Subarrays with sum ≤ goal-1  = {0, 1, 2, ..., goal-1}              │
 *    │ Difference                   = {goal}                                │
 *    └─────────────────────────────────────────────────────────────────────┘
 *    
 *    For "at most X", sliding window counts easily:
 *    - At each position right, count all subarrays ending at right
 *    - That's (right - left + 1) subarrays
 *    
 *    Example: Window [left=2, right=4]
 *    - Subarrays ending at 4: [2,3,4], [3,4], [4]
 *    - Count = 4 - 2 + 1 = 3
 * 
 * 4. APPROACH COMPARISON TABLE:
 *    
 *    ┌──────────────────┬───────────┬─────────┬─────────────────────────┐
 *    │ Approach         │ Time      │ Space   │ Best For                │
 *    ├──────────────────┼───────────┼─────────┼─────────────────────────┤
 *    │ Prefix Sum       │ O(n)      │ O(n)    │ ⭐ Interviews (clear)   │
 *    │ Sliding Window   │ O(n)      │ O(1)    │ Space optimization      │
 *    │ Three Pointers   │ O(n)      │ O(1)    │ Alternative O(1) space  │
 *    │ Prefix Array     │ O(n²)     │ O(n)    │ Understanding           │
 *    │ Brute Force      │ O(n²)     │ O(1)    │ Verification            │
 *    └──────────────────┴───────────┴─────────┴─────────────────────────┘
 * 
 * 5. SPECIAL CASE: GOAL = 0
 *    
 *    When goal = 0, we're counting subarrays with only zeros.
 *    
 *    Formula: n consecutive zeros give n*(n+1)/2 subarrays
 *    
 *    Example: [0,0,0] (3 zeros)
 *    - Subarrays: [0], [0], [0], [0,0], [0,0], [0,0,0]
 *    - Count = 3*4/2 = 6
 *    
 *    General: [0,0,0,0,0] (5 zeros)
 *    - Count = 5*6/2 = 15 ✓ (matches Example 2)
 *    
 *    This is why goal=0 is often an edge case to handle specially.
 * 
 * 6. PREFIX SUM PATTERN - Universal Template:
 *    
 *    ```java
 *    Map<Integer, Integer> prefixCount = new HashMap<>();
 *    prefixCount.put(0, 1);  // CRITICAL: Handle subarrays from index 0
 *    
 *    int prefixSum = 0;
 *    int count = 0;
 *    
 *    for (int num : array) {
 *        prefixSum += num;
 *        
 *        // Count previous positions that form valid subarrays
 *        int target = prefixSum - goal;
 *        count += prefixCount.getOrDefault(target, 0);
 *        
 *        // Record current prefix sum
 *        prefixCount.put(prefixSum, prefixCount.getOrDefault(prefixSum, 0) + 1);
 *    }
 *    ```
 *    
 *    This template works for:
 *    - Binary subarrays with sum (this problem)
 *    - Subarray sum equals K (general version)
 *    - Contiguous array (sum = 0 with equal 0s and 1s)
 * 
 * 7. WHY prefixCount.put(0, 1)?
 *    
 *    This is the MOST confusing part for beginners!
 *    
 *    Example: nums = [1, 0, 1], goal = 2
 *    
 *    At index 2:
 *      - prefixSum = 2
 *      - target = prefixSum - goal = 2 - 2 = 0
 *      - We need to find how many times we've seen 0
 *      - But 0 represents "no elements yet" (before index 0)
 *      - This accounts for subarray [0...2] (entire array)
 *      - Without put(0,1), we'd miss this subarray!
 *    
 *    Think of it as: "empty prefix sum = 0, occurs once at start"
 * 
 * 8. COMMON MISTAKES:
 *    
 *    ✗ Forgetting prefixCount.put(0, 1)
 *    ✗ Adding to map BEFORE counting (wrong order)
 *    ✗ Not handling goal = 0 specially in some approaches
 *    ✗ Confusing "count subarrays" with "find length"
 *    ✗ In sliding window: forgetting it counts "at most", not "exactly"
 * 
 * 9. INTERVIEW STRATEGY:
 *    
 *    Level 1 (5 min): Recognize it's a subarray counting problem
 *    Level 2 (10 min): Explain prefix sum approach with example
 *    Level 3 (15 min): Code Approach 1 (Prefix Sum with HashMap)
 *    Level 4 (20 min): Explain why prefixCount.put(0, 1) is needed
 *    Level 5 (25 min): Discuss Approach 2 (Sliding Window transform)
 *    Level 6 (30 min): Handle edge cases and optimize
 *    
 *    Recommended: Start with Approach 1, mention Approach 2 for optimization
 * 
 * 10. RELATED PROBLEMS (Same Pattern):
 *     
 *     Exact same technique:
 *     • Subarray Sum Equals K (general version, not just binary)
 *     • Contiguous Array (equal 0s and 1s)
 *     • Subarray Sums Divisible by K
 *     
 *     Sliding window transform:
 *     • Count of Range Sum (uses prefix sum + sorting)
 *     • Subarrays with K Different Integers
 *     
 *     All use the same fundamental patterns!
 * 
 * 11. OPTIMIZATION NOTES:
 *     
 *     For binary arrays specifically:
 *     - Prefix sums are bounded: 0 to n
 *     - Could use array instead of HashMap if n is small
 *     - array[prefixSum] = count
 *     - Slightly faster but same complexity
 *     
 *     For general arrays (Subarray Sum Equals K):
 *     - Prefix sums can be any value
 *     - Must use HashMap
 *     - This problem is easier due to binary constraint
 * 
 * 12. COMPLEXITY DEEP DIVE:
 *     
 *     Prefix Sum Approach:
 *     - Time: O(n) - single pass, HashMap ops are O(1) average
 *     - Space: O(n) - worst case, all different prefix sums
 *     - Best space: O(1) when all elements same (few prefix sums)
 *     - Average space: O(√n) for random binary array
 *     
 *     Sliding Window Approach:
 *     - Time: O(n) - two passes (countAtMost called twice)
 *     - Space: O(1) - only a few variables
 *     - Trade-off: 2x passes but no HashMap
 *     
 *     Choose based on constraints:
 *     - n < 10^5: Either approach works
 *     - n > 10^5: Consider sliding window for better cache locality
 *     - Memory limited: Use sliding window
 */

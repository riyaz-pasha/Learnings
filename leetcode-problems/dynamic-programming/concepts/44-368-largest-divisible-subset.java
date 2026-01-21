/*
 * LARGEST DIVISIBLE SUBSET - COMPLETE GUIDE
 * ==========================================
 * 
 * PROBLEM UNDERSTANDING:
 * - Given distinct positive integers
 * - Find largest subset where EVERY PAIR is divisible
 * - Either answer[i] % answer[j] == 0 OR answer[j] % answer[i] == 0
 * - Return the actual subset, not just its size
 * 
 * CRITICAL INSIGHT (The "Aha!" moment):
 * =====================================
 * This problem looks similar to Longest Increasing Subsequence (LIS)!
 * 
 * KEY OBSERVATION #1: Transitivity Property
 * If we SORT the array, the divisibility forms a chain:
 * - If a % b == 0 and b % c == 0, then a % c == 0
 * - Example: 8 % 4 == 0, 4 % 2 == 0, therefore 8 % 2 == 0
 * 
 * KEY OBSERVATION #2: After sorting
 * - For sorted array [a, b, c] where a < b < c
 * - If c is divisible by b, and b is divisible by a
 * - Then c is divisible by a (transitivity!)
 * - So we only need to check: "Is current element divisible by the last element?"
 * 
 * WHY SORTING WORKS:
 * [1, 2, 4, 8] - sorted, forms a chain where each divides the next
 * [8, 2, 4, 1] - unsorted, we'd have to check all pairs
 * 
 * After sorting, the problem becomes:
 * "Find the longest chain where each element is divisible by the previous"
 * This is exactly like LIS, but with divisibility instead of <
 * 
 * INTERVIEW APPROACH - HOW TO DISCOVER THIS SOLUTION:
 * ===================================================
 * 
 * Step 1: Understand the constraint (2 minutes)
 * - Draw examples: [1,2,3], [1,2,4,8]
 * - Notice: 1 divides everything, so it's often included
 * - In [1,2,4,8]: 1|2, 2|4, 4|8, and by transitivity 1|4, 1|8, 2|8
 * 
 * Step 2: Recognize the pattern (3 minutes)
 * - "Wait, if I sort [1,2,4,8], I can build a chain"
 * - "Each element only needs to be divisible by the previous one"
 * - "This is like LIS with divisibility!"
 * 
 * Step 3: Think about DP state (2 minutes)
 * - dp[i] = length of largest subset ending at nums[i]
 * - For each i, check all j < i where nums[i] % nums[j] == 0
 * - This is the same pattern as LIS!
 * 
 * Step 4: Track the actual subset (2 minutes)
 * - Unlike LIS where we only need length, here we need elements
 * - Keep a parent/previous pointer to reconstruct the path
 * 
 * THINGS TO SAY IN INTERVIEW:
 * "I notice this is similar to Longest Increasing Subsequence"
 * "If I sort first, I can use DP similar to LIS"
 * "The transitivity property means I only need to check adjacent elements in my chain"
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LargestDivisibleSubset {
    
    /*
     * APPROACH 1: DYNAMIC PROGRAMMING (Optimal)
     * ==========================================
     * This is THE solution for interviews. Clear, efficient, and intuitive.
     * 
     * ALGORITHM:
     * 1. Sort the array (critical step!)
     * 2. Use DP similar to LIS to find longest divisible chain
     * 3. Track parent pointers to reconstruct the actual subset
     * 
     * STATE DEFINITION:
     * dp[i] = length of largest divisible subset ending at index i
     * parent[i] = index of previous element in the subset (for reconstruction)
     * 
     * TRANSITION:
     * For each i, check all j < i:
     *   if (nums[i] % nums[j] == 0):
     *     dp[i] = max(dp[i], dp[j] + 1)
     *     if we update dp[i], set parent[i] = j
     * 
     * WHY nums[i] % nums[j] and not the other way?
     * Because array is sorted! nums[i] >= nums[j], so only nums[i] % nums[j] makes sense
     * 
     * RECONSTRUCTION:
     * - Find index with maximum dp value
     * - Follow parent pointers backward to build the result
     * 
     * EXAMPLE WALKTHROUGH: [1,2,4,8]
     * 
     * After sorting: [1,2,4,8] (already sorted)
     * 
     * Initial: dp = [1,1,1,1], parent = [-1,-1,-1,-1]
     * 
     * i=0 (num=1): dp[0]=1, parent[0]=-1
     *   No elements before, subset = {1}
     * 
     * i=1 (num=2): Check j=0
     *   2 % 1 == 0 ✓
     *   dp[1] = max(1, dp[0]+1) = 2, parent[1]=0
     *   Subset = {1,2}
     * 
     * i=2 (num=4): Check j=0,1
     *   j=0: 4 % 1 == 0 ✓ → dp[2] = 2, parent[2]=0
     *   j=1: 4 % 2 == 0 ✓ → dp[2] = max(2, dp[1]+1) = 3, parent[2]=1
     *   Subset = {1,2,4}
     * 
     * i=3 (num=8): Check j=0,1,2
     *   j=0: 8 % 1 == 0 ✓ → dp[3] = 2, parent[3]=0
     *   j=1: 8 % 2 == 0 ✓ → dp[3] = 3, parent[3]=1
     *   j=2: 8 % 4 == 0 ✓ → dp[3] = max(3, dp[2]+1) = 4, parent[3]=2
     *   Subset = {1,2,4,8}
     * 
     * Final: dp = [1,2,3,4], parent = [-1,0,1,2]
     * Max at index 3, trace back: 3→2→1→0 gives [8,4,2,1] or reversed [1,2,4,8]
     */
    public List<Integer> largestDivisibleSubset(int[] nums) {
        if (nums == null || nums.length == 0) {
            return new ArrayList<>();
        }
        
        int n = nums.length;
        
        // STEP 1: Sort the array (crucial for the DP approach to work)
        Arrays.sort(nums);
        
        // STEP 2: Initialize DP arrays
        // dp[i] = length of largest divisible subset ending at index i
        int[] dp = new int[n];
        // parent[i] = index of previous element in the subset chain
        int[] parent = new int[n];
        
        // Base case: each element forms a subset of size 1
        Arrays.fill(dp, 1);
        Arrays.fill(parent, -1); // -1 means no parent (start of chain)
        
        // Track the index with maximum subset length
        int maxLen = 1;
        int maxIdx = 0;
        
        // STEP 3: Build DP table
        for (int i = 1; i < n; i++) {
            // Check all previous elements
            for (int j = 0; j < i; j++) {
                // Key check: can we extend the subset ending at j?
                // Since array is sorted, nums[i] >= nums[j]
                // So we only check if nums[i] is divisible by nums[j]
                if (nums[i] % nums[j] == 0) {
                    // If extending from j gives a longer subset
                    if (dp[j] + 1 > dp[i]) {
                        dp[i] = dp[j] + 1;
                        parent[i] = j; // Track where we came from
                    }
                }
            }
            
            // Update max if we found a longer subset
            if (dp[i] > maxLen) {
                maxLen = dp[i];
                maxIdx = i;
            }
        }
        
        // STEP 4: Reconstruct the subset by following parent pointers
        List<Integer> result = new ArrayList<>();
        int curr = maxIdx;
        
        // Trace back from maxIdx following parent pointers
        while (curr != -1) {
            result.add(nums[curr]);
            curr = parent[curr];
        }
        
        // We built the list backward, so reverse it (optional, any order works)
        Collections.reverse(result);
        
        return result;
    }
    
    /*
     * APPROACH 2: OPTIMIZED WITH EARLY TERMINATION
     * =============================================
     * Same logic but with minor optimizations for practical performance.
     * Only mention this if interviewer asks about optimizations.
     */
    public List<Integer> largestDivisibleSubset_Optimized(int[] nums) {
        if (nums == null || nums.length == 0) {
            return new ArrayList<>();
        }
        
        int n = nums.length;
        Arrays.sort(nums);
        
        int[] dp = new int[n];
        int[] parent = new int[n];
        Arrays.fill(dp, 1);
        Arrays.fill(parent, -1);
        
        int maxLen = 1;
        int maxIdx = 0;
        
        for (int i = 1; i < n; i++) {
            for (int j = i - 1; j >= 0; j--) {
                if (nums[i] % nums[j] == 0 && dp[j] + 1 > dp[i]) {
                    dp[i] = dp[j] + 1;
                    parent[i] = j;
                    
                    // Early termination: if we found a chain as long as
                    // the best possible from here, we can stop
                    if (dp[i] >= maxLen) {
                        break;
                    }
                }
            }
            
            if (dp[i] > maxLen) {
                maxLen = dp[i];
                maxIdx = i;
            }
        }
        
        List<Integer> result = new ArrayList<>();
        int curr = maxIdx;
        while (curr != -1) {
            result.add(nums[curr]);
            curr = parent[curr];
        }
        
        // Result can be in any order, but ascending is nicer
        Collections.reverse(result);
        return result;
    }
    
    /*
     * APPROACH 3: RECURSIVE WITH MEMOIZATION (Educational)
     * ====================================================
     * Shows the recursive nature of the problem.
     * Good to understand, but DP is better for interviews.
     */
    public List<Integer> largestDivisibleSubset_Recursive(int[] nums) {
        if (nums == null || nums.length == 0) {
            return new ArrayList<>();
        }
        
        Arrays.sort(nums);
        int n = nums.length;
        
        // Memoization: memo[i][j] = largest subset starting from i with last element j
        Map<String, List<Integer>> memo = new HashMap<>();
        
        List<Integer> result = new ArrayList<>();
        
        // Try starting from each index
        for (int i = 0; i < n; i++) {
            List<Integer> subset = helper(nums, i, -1, memo);
            if (subset.size() > result.size()) {
                result = subset;
            }
        }
        
        return result;
    }
    
    private List<Integer> helper(int[] nums, int curr, int lastIdx, 
                                  Map<String, List<Integer>> memo) {
        if (curr >= nums.length) {
            return new ArrayList<>();
        }
        
        String key = curr + "," + lastIdx;
        if (memo.containsKey(key)) {
            return new ArrayList<>(memo.get(key));
        }
        
        // Option 1: Skip current element
        List<Integer> skip = helper(nums, curr + 1, lastIdx, memo);
        
        // Option 2: Take current element (if divisible)
        List<Integer> take = new ArrayList<>();
        if (lastIdx == -1 || nums[curr] % nums[lastIdx] == 0) {
            List<Integer> rest = helper(nums, curr + 1, curr, memo);
            take.add(nums[curr]);
            take.addAll(rest);
        }
        
        List<Integer> result = take.size() > skip.size() ? take : skip;
        memo.put(key, result);
        
        return new ArrayList<>(result);
    }
    
    /*
     * INTERVIEW STRATEGY & TALKING POINTS:
     * ====================================
     * 
     * 1. CLARIFY (1-2 minutes):
     *    "So I need to find the largest subset where all pairs are divisible?"
     *    "The integers are distinct and positive?"
     *    "Can I return any valid answer if there are multiple?"
     * 
     * 2. EXAMPLE WALKTHROUGH (2 minutes):
     *    Draw [1,2,4,8]: "1 divides 2, 2 divides 4, 4 divides 8"
     *    "Notice they form a chain, and by transitivity all pairs work"
     * 
     * 3. KEY INSIGHT (3 minutes):
     *    "If I sort the array first, this becomes like LIS"
     *    "The transitivity property means I only check consecutive elements"
     *    "After sorting: if a|b and b|c, then a|c automatically"
     * 
     * 4. SOLUTION OUTLINE (2 minutes):
     *    "I'll use DP similar to Longest Increasing Subsequence"
     *    "dp[i] = length of longest chain ending at i"
     *    "Keep parent pointers to reconstruct the actual subset"
     * 
     * 5. CODE (10 minutes):
     *    Write the DP solution with clear comments
     *    Explain the reconstruction step carefully
     * 
     * 6. COMPLEXITY ANALYSIS (2 minutes):
     *    Time: O(n²) - nested loops
     *    Space: O(n) - dp and parent arrays
     * 
     * 7. TEST (3 minutes):
     *    Walk through [1,2,3]: "1|2 and 1|3 but 2∤3 and 3∤2, so max is 2"
     *    Edge case: [1] → [1]
     * 
     * COMMON MISTAKES TO AVOID:
     * ========================
     * 
     * 1. Forgetting to sort
     *    - Without sorting, you'd need to check all pairs O(n²) for each subset
     *    - Sorting enables the DP approach
     * 
     * 2. Checking both directions
     *    - After sorting, nums[i] >= nums[j] for j < i
     *    - Only need to check nums[i] % nums[j], not both ways
     * 
     * 3. Not tracking parent pointers
     *    - The problem asks for the actual subset, not just length
     *    - Must reconstruct the path
     * 
     * 4. Returning dp[n-1]
     *    - The longest subset might not end at the last element
     *    - Must track maxIdx across all positions
     * 
     * 5. Off-by-one in reconstruction
     *    - Make sure to include the starting element (when parent == -1)
     * 
     * FOLLOW-UP QUESTIONS TO ANTICIPATE:
     * ==================================
     * 
     * Q: "Can you optimize this further?"
     * A: "The O(n²) is already optimal for this problem since we need to
     *     check all pairs potentially. We could add early termination
     *     heuristics but won't improve worst case."
     * 
     * Q: "What if we need all possible largest subsets?"
     * A: "We'd need to track all parent paths, not just one. Store a list
     *     of parents at each position instead of a single parent."
     * 
     * Q: "Why does sorting work?"
     * A: "Divisibility has a transitivity property. If a|b and b|c, then a|c.
     *     Sorting creates an order where we can build chains incrementally."
     * 
     * Q: "What if numbers can be negative?"
     * A: "Divisibility with negatives is trickier. We'd need to handle signs
     *     carefully, possibly work with absolute values."
     */
    
    // Test cases with detailed explanations
    public static void main(String[] args) {
        LargestDivisibleSubset solution = new LargestDivisibleSubset();
        
        System.out.println("=== Test Case 1: [1,2,3] ===");
        int[] nums1 = {1, 2, 3};
        List<Integer> result1 = solution.largestDivisibleSubset(nums1);
        System.out.println("Input: " + Arrays.toString(nums1));
        System.out.println("Output: " + result1); // [1,2] or [1,3]
        System.out.println("Explanation: 1|2 but 2∤3, so we pick [1,2] or [1,3]");
        System.out.println();
        
        System.out.println("=== Test Case 2: [1,2,4,8] ===");
        int[] nums2 = {1, 2, 4, 8};
        List<Integer> result2 = solution.largestDivisibleSubset(nums2);
        System.out.println("Input: " + Arrays.toString(nums2));
        System.out.println("Output: " + result2); // [1,2,4,8]
        System.out.println("Explanation: Forms a complete divisible chain");
        System.out.println();
        
        System.out.println("=== Test Case 3: [1,2,4,8,16,32] ===");
        int[] nums3 = {1, 2, 4, 8, 16, 32};
        List<Integer> result3 = solution.largestDivisibleSubset(nums3);
        System.out.println("Input: " + Arrays.toString(nums3));
        System.out.println("Output: " + result3); // [1,2,4,8,16,32]
        System.out.println("Explanation: Powers of 2 form perfect chains");
        System.out.println();
        
        System.out.println("=== Test Case 4: [1,3,9,18] ===");
        int[] nums4 = {1, 3, 9, 18};
        List<Integer> result4 = solution.largestDivisibleSubset(nums4);
        System.out.println("Input: " + Arrays.toString(nums4));
        System.out.println("Output: " + result4); // [1,3,9,18]
        System.out.println("Explanation: Multiples of 3");
        System.out.println();
        
        System.out.println("=== Test Case 5: [5,9,18,54,108,540,90,180,360,720] ===");
        int[] nums5 = {5, 9, 18, 54, 108, 540, 90, 180, 360, 720};
        List<Integer> result5 = solution.largestDivisibleSubset(nums5);
        System.out.println("Input: " + Arrays.toString(nums5));
        System.out.println("Output: " + result5);
        System.out.println("Explanation: Multiple possible chains exist");
        System.out.println();
        
        System.out.println("=== Edge Case: Single element [42] ===");
        int[] nums6 = {42};
        List<Integer> result6 = solution.largestDivisibleSubset(nums6);
        System.out.println("Input: " + Arrays.toString(nums6));
        System.out.println("Output: " + result6); // [42]
        System.out.println();
        
        System.out.println("=== Edge Case: No divisibility [2,3,5,7,11] ===");
        int[] nums7 = {2, 3, 5, 7, 11};
        List<Integer> result7 = solution.largestDivisibleSubset(nums7);
        System.out.println("Input: " + Arrays.toString(nums7));
        System.out.println("Output: " + result7); // Any single element
        System.out.println("Explanation: All primes, no divisibility");
        System.out.println();
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * ====================
 * 
 * Time Complexity: O(n²)
 * - Sorting: O(n log n)
 * - DP: O(n²) - nested loops, each pair checked once
 * - Reconstruction: O(n)
 * - Overall: O(n²)
 * 
 * Space Complexity: O(n)
 * - dp array: O(n)
 * - parent array: O(n)
 * - result list: O(n)
 * - Overall: O(n)
 * 
 * WHY IS THIS OPTIMAL?
 * - We need to consider relationships between elements
 * - In worst case, every element could divide every other
 * - So O(n²) is necessary to examine all pairs
 * - We can't do better than this for the general case
 * 
 * COMPARISON TO LIS:
 * ==================
 * Similarities:
 * - Both use DP with O(n²) approach
 * - Both have dp[i] = property of sequence ending at i
 * - Both check previous elements for extension
 * - Both require tracking parents to reconstruct
 * 
 * Differences:
 * - LIS: comparison is nums[i] > nums[j]
 * - This: comparison is nums[i] % nums[j] == 0
 * - LIS can be optimized to O(n log n) with binary search
 * - This cannot (divisibility doesn't have same properties)
 * 
 * REAL-WORLD APPLICATIONS:
 * ========================
 * - Finding compatible versions (v2.0 compatible with v1.0)
 * - Scheduling tasks with dependencies
 * - Finding compatible screen resolutions
 * - Package dependency resolution
 */

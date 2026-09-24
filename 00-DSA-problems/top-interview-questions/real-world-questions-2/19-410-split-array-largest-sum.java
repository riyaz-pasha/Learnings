import java.util.*;

/**
 * PROBLEM ANALYSIS: SPLIT ARRAY LARGEST SUM
 * ==========================================
 * 
 * PROBLEM UNDERSTANDING:
 * - Given: Array of integers and k (number of splits)
 * - Goal: Split array into k non-empty contiguous subarrays
 * - Objective: Minimize the maximum sum among all subarrays
 * - Return: The minimized largest sum
 * 
 * Example:
 * nums = [7,2,5,10,8], k = 2
 * Possible splits:
 * - [7] [2,5,10,8] → max(7, 25) = 25
 * - [7,2] [5,10,8] → max(9, 23) = 23
 * - [7,2,5] [10,8] → max(14, 18) = 18 ← optimal
 * - [7,2,5,10] [8] → max(24, 8) = 24
 * Answer: 18
 * 
 * KEY INSIGHTS:
 * 1. We want to "balance" the subarrays to minimize the maximum
 * 2. The answer is bounded: min = max(nums), max = sum(nums)
 * 3. For a given max sum M, we can check if k splits is possible
 * 4. This suggests binary search on the answer!
 * 5. Also solvable with dynamic programming
 * 
 * CRITICAL REALIZATION:
 * - Binary Search on Answer: "Can we split with max sum ≤ M?"
 * - If yes with M, try smaller M
 * - If no with M, try larger M
 * - This is a classic "minimize the maximum" problem pattern
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Understand the constraints
 * - Subarrays must be contiguous (can't rearrange)
 * - Need exactly k subarrays (all non-empty)
 * - Want to minimize the maximum sum
 * 
 * Step 2: Identify the pattern
 * "Minimize the maximum" → Binary Search on Answer
 * "Split array optimally" → Dynamic Programming
 * 
 * Step 3: Binary Search intuition
 * - Answer range: [max element, total sum]
 * - For a candidate answer M, check if achievable
 * - Binary search to find minimum valid M
 * 
 * Step 4: DP intuition
 * - dp[i][j] = min largest sum using first i elements with j subarrays
 * - Try all possible positions for the j-th split
 * 
 * APPROACHES:
 * 1. Binary Search + Greedy - O(n log(sum)) time, O(1) space [OPTIMAL]
 * 2. Dynamic Programming - O(n² * k) time, O(n * k) space
 * 3. DP Optimized - O(n² * k) time, O(n) space
 * 4. Brute Force - O(n^k) time (too slow)
 */

/**
 * APPROACH 1: BINARY SEARCH ON ANSWER (OPTIMAL - RECOMMENDED)
 * ============================================================
 * 
 * INTUITION:
 * Instead of searching for the split positions, search for the answer!
 * 
 * KEY INSIGHT:
 * If we can split array with max sum = M, we can also split with M+1, M+2, etc.
 * This monotonic property enables binary search.
 * 
 * ALGORITHM:
 * 1. Binary search on answer range [max(nums), sum(nums)]
 * 2. For each candidate mid:
 *    - Check if we can split into ≤ k subarrays with max sum ≤ mid
 *    - Use greedy: keep adding elements until sum would exceed mid
 * 3. If possible with mid, try smaller (search left)
 * 4. If not possible, try larger (search right)
 * 
 * WHY GREEDY WORKS FOR VALIDATION:
 * To minimize number of splits with max sum ≤ M:
 * - Greedily make each subarray as large as possible (≤ M)
 * - This minimizes the number of subarrays needed
 * - If we need ≤ k subarrays, then M is valid
 * 
 * TIME: O(n * log(sum)) where sum = total of all elements
 *       - Binary search: log(sum - max)
 *       - Each check: O(n)
 * SPACE: O(1)
 */
class Solution {
    
    public int splitArray(int[] nums, int k) {
        // Binary search bounds
        int left = 0;   // Minimum possible answer (max element)
        int right = 0;  // Maximum possible answer (sum of all)
        
        for (int num : nums) {
            left = Math.max(left, num);    // At least one subarray contains max element
            right += num;                   // At most, all in one subarray
        }
        
        // Binary search for minimum valid maximum sum
        while (left < right) {
            int mid = left + (right - left) / 2;
            
            // Check if we can split with max sum = mid
            if (canSplit(nums, k, mid)) {
                // mid is valid, try to find smaller
                right = mid;
            } else {
                // mid is too small, need larger max sum
                left = mid + 1;
            }
        }
        
        return left;
    }
    
    /**
     * Check if we can split array into at most k subarrays
     * with each subarray sum ≤ maxSum
     * 
     * Greedy approach: Make each subarray as large as possible
     */
    private boolean canSplit(int[] nums, int k, int maxSum) {
        int subarrays = 1;  // Start with one subarray
        int currentSum = 0;
        
        for (int num : nums) {
            // Try to add current number to current subarray
            if (currentSum + num <= maxSum) {
                currentSum += num;
            } else {
                // Need to start a new subarray
                subarrays++;
                currentSum = num;
                
                // If we need more than k subarrays, maxSum is too small
                if (subarrays > k) {
                    return false;
                }
            }
        }
        
        return true;
    }
}

/**
 * APPROACH 2: DYNAMIC PROGRAMMING (ALTERNATIVE)
 * ==============================================
 * 
 * INTUITION:
 * Build solution incrementally:
 * - dp[i][j] = minimum largest sum to split nums[0..i-1] into j subarrays
 * 
 * STATE DEFINITION:
 * dp[i][j] = min largest sum using first i elements with j subarrays
 * 
 * TRANSITION:
 * For position i and j subarrays, try all possible positions for j-th split:
 * dp[i][j] = min(max(dp[p][j-1], sum(nums[p..i-1]))) for all valid p
 * 
 * Where p is the start of the j-th subarray.
 * 
 * BASE CASE:
 * dp[0][0] = 0 (no elements, no subarrays)
 * dp[i][1] = sum(nums[0..i-1]) (all elements in one subarray)
 * 
 * TIME: O(n² * k)
 *       - States: O(n * k)
 *       - Each state: O(n) transitions
 * SPACE: O(n * k)
 */
class SolutionDP {
    
    public int splitArray(int[] nums, int k) {
        int n = nums.length;
        
        // Prefix sums for quick range sum calculation
        long[] prefixSum = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i];
        }
        
        // dp[i][j] = min largest sum for first i elements with j subarrays
        long[][] dp = new long[n + 1][k + 1];
        
        // Initialize with infinity
        for (int i = 0; i <= n; i++) {
            Arrays.fill(dp[i], Long.MAX_VALUE);
        }
        
        // Base case: 0 elements, 0 subarrays
        dp[0][0] = 0;
        
        // Fill DP table
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= Math.min(i, k); j++) {
                // Try all positions for the j-th subarray to start
                for (int p = j - 1; p < i; p++) {
                    // j-th subarray is nums[p..i-1]
                    long subarraySum = prefixSum[i] - prefixSum[p];
                    
                    // Maximum of: previous max, current subarray sum
                    long maxSum = Math.max(dp[p][j - 1], subarraySum);
                    
                    dp[i][j] = Math.min(dp[i][j], maxSum);
                }
            }
        }
        
        return (int) dp[n][k];
    }
}

/**
 * APPROACH 3: DP WITH SPACE OPTIMIZATION
 * =======================================
 * 
 * Optimize space by using rolling array
 * Only need previous row of DP table
 */
class SolutionDPOptimized {
    
    public int splitArray(int[] nums, int k) {
        int n = nums.length;
        
        // Prefix sums
        long[] prefixSum = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i];
        }
        
        // Only need current and previous row
        long[] dp = new long[n + 1];
        long[] newDp = new long[n + 1];
        
        // Initialize: all elements in one subarray
        for (int i = 1; i <= n; i++) {
            dp[i] = prefixSum[i];
        }
        
        // Build up from 2 to k subarrays
        for (int j = 2; j <= k; j++) {
            Arrays.fill(newDp, Long.MAX_VALUE);
            
            for (int i = j; i <= n; i++) {
                // Try all split positions
                for (int p = j - 1; p < i; p++) {
                    long subarraySum = prefixSum[i] - prefixSum[p];
                    long maxSum = Math.max(dp[p], subarraySum);
                    newDp[i] = Math.min(newDp[i], maxSum);
                }
            }
            
            // Swap arrays
            long[] temp = dp;
            dp = newDp;
            newDp = temp;
        }
        
        return (int) dp[n];
    }
}

/**
 * APPROACH 4: BINARY SEARCH WITH DETAILED COMMENTS
 * =================================================
 * 
 * More verbose version for understanding
 */
class SolutionBinarySearchDetailed {
    
    public int splitArray(int[] nums, int k) {
        /*
         * Step 1: Determine search range
         * 
         * Lower bound: Maximum element
         * - At minimum, one subarray must contain the largest element
         * - So answer ≥ max(nums)
         * 
         * Upper bound: Sum of all elements
         * - At maximum, all elements in one subarray (when k=1)
         * - So answer ≤ sum(nums)
         */
        int maxElement = 0;
        int totalSum = 0;
        
        for (int num : nums) {
            maxElement = Math.max(maxElement, num);
            totalSum += num;
        }
        
        /*
         * Step 2: Binary search on the answer
         * 
         * We're looking for the minimum value M such that:
         * "We can split nums into k subarrays with max sum ≤ M"
         * 
         * This has the monotonic property:
         * - If achievable with M, also achievable with M+1, M+2, ...
         * - We want the smallest such M
         */
        int left = maxElement;
        int right = totalSum;
        
        while (left < right) {
            int mid = left + (right - left) / 2;
            
            /*
             * Check: Can we split with max sum = mid?
             * 
             * Use greedy approach to verify:
             * - Make each subarray as large as possible (≤ mid)
             * - Count how many subarrays we need
             * - If we need ≤ k subarrays, mid is valid
             */
            if (isPossible(nums, k, mid)) {
                // mid works, but maybe we can do better (smaller)
                right = mid;
            } else {
                // mid is too small, we need a larger max sum
                left = mid + 1;
            }
        }
        
        return left;
    }
    
    /**
     * Greedy check: Can we split nums into ≤ k subarrays
     * with each subarray sum ≤ maxSum?
     */
    private boolean isPossible(int[] nums, int k, int maxSum) {
        int subarrayCount = 1;  // We need at least 1 subarray
        int currentSum = 0;
        
        for (int num : nums) {
            /*
             * Try to add num to current subarray
             * 
             * If adding num would exceed maxSum:
             * - Start a new subarray with num
             * - Increment subarray count
             * 
             * Note: We know num ≤ maxSum because maxSum ≥ max(nums)
             */
            if (currentSum + num <= maxSum) {
                currentSum += num;
            } else {
                // Start new subarray
                subarrayCount++;
                currentSum = num;
                
                // Early termination: if we already need > k subarrays
                if (subarrayCount > k) {
                    return false;
                }
            }
        }
        
        return true;
    }
}

/**
 * TEST CASES
 * ==========
 */
class TestSplitArray {
    
    public static void runTest(String testName, int[] nums, int k, int expected) {
        System.out.println("\n" + testName);
        System.out.println("Array: " + Arrays.toString(nums));
        System.out.println("k (splits): " + k);
        
        Solution sol = new Solution();
        int result = sol.splitArray(nums, k);
        
        System.out.println("Result: " + result);
        System.out.println("Expected: " + expected);
        System.out.println("Status: " + (result == expected ? "✓ PASS" : "✗ FAIL"));
        
        // Show example split
        if (nums.length <= 10) {
            demonstrateSplit(nums, k, result);
        }
    }
    
    private static void demonstrateSplit(int[] nums, int k, int maxSum) {
        System.out.println("\nExample split with max sum = " + maxSum + ":");
        
        int subarrays = 0;
        int currentSum = 0;
        int start = 0;
        
        for (int i = 0; i < nums.length; i++) {
            if (currentSum + nums[i] <= maxSum) {
                currentSum += nums[i];
            } else {
                // Print previous subarray
                System.out.print("  Subarray " + (subarrays + 1) + ": [");
                for (int j = start; j < i; j++) {
                    System.out.print(nums[j] + (j < i - 1 ? "," : ""));
                }
                System.out.println("] sum = " + currentSum);
                
                subarrays++;
                currentSum = nums[i];
                start = i;
            }
        }
        
        // Print last subarray
        System.out.print("  Subarray " + (subarrays + 1) + ": [");
        for (int j = start; j < nums.length; j++) {
            System.out.print(nums[j] + (j < nums.length - 1 ? "," : ""));
        }
        System.out.println("] sum = " + currentSum);
    }
    
    public static void main(String[] args) {
        System.out.println("=== SPLIT ARRAY LARGEST SUM - COMPREHENSIVE TESTING ===");
        
        // Test Case 1: Basic example
        runTest(
            "Test 1: Basic case",
            new int[]{7, 2, 5, 10, 8},
            2,
            18
        );
        // [7,2,5] [10,8] → max(14, 18) = 18
        
        // Test Case 2: Single element per subarray
        runTest(
            "Test 2: k = n (one element each)",
            new int[]{1, 2, 3, 4, 5},
            5,
            5
        );
        
        // Test Case 3: All in one subarray
        runTest(
            "Test 3: k = 1 (all together)",
            new int[]{1, 2, 3, 4, 5},
            1,
            15
        );
        
        // Test Case 4: Equal distribution possible
        runTest(
            "Test 4: Perfect distribution",
            new int[]{1, 4, 4, 4, 4, 4, 4},
            3,
            9
        );
        // [1,4,4] [4,4] [4,4] → all sum to 9
        
        // Test Case 5: Large numbers
        runTest(
            "Test 5: Large values",
            new int[]{10, 20, 30, 40, 50},
            3,
            60
        );
        
        // Test Case 6: Single element
        runTest(
            "Test 6: Single element",
            new int[]{100},
            1,
            100
        );
        
        // Test Case 7: Two elements, two splits
        runTest(
            "Test 7: Two elements",
            new int[]{1, 2147483647},
            2,
            2147483647
        );
        
        // Test Case 8: Complex case
        runTest(
            "Test 8: Complex distribution",
            new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9},
            3,
            17
        );
        
        // Compare approaches
        System.out.println("\n\n=== APPROACH COMPARISON ===");
        int[] testArray = {7, 2, 5, 10, 8};
        int testK = 2;
        
        System.out.println("Test array: " + Arrays.toString(testArray));
        System.out.println("k = " + testK);
        
        long start, end;
        
        start = System.nanoTime();
        Solution sol1 = new Solution();
        int result1 = sol1.splitArray(testArray, testK);
        end = System.nanoTime();
        System.out.println("\nBinary Search: " + result1 + 
            " (Time: " + (end - start) / 1000 + " μs)");
        
        start = System.nanoTime();
        SolutionDP sol2 = new SolutionDP();
        int result2 = sol2.splitArray(testArray, testK);
        end = System.nanoTime();
        System.out.println("Dynamic Programming: " + result2 + 
            " (Time: " + (end - start) / 1000 + " μs)");
        
        // Demonstrate binary search process
        System.out.println("\n\n=== BINARY SEARCH STEP-BY-STEP ===");
        demonstrateBinarySearch(new int[]{7, 2, 5, 10, 8}, 2);
    }
    
    private static void demonstrateBinarySearch(int[] nums, int k) {
        System.out.println("Array: " + Arrays.toString(nums));
        System.out.println("k = " + k);
        
        int left = 0, right = 0;
        for (int num : nums) {
            left = Math.max(left, num);
            right += num;
        }
        
        System.out.println("\nSearch range: [" + left + ", " + right + "]");
        System.out.println("(min = max element, max = sum of all)\n");
        
        int iteration = 1;
        while (left < right) {
            int mid = left + (right - left) / 2;
            
            // Count subarrays needed
            int subarrays = 1, currentSum = 0;
            for (int num : nums) {
                if (currentSum + num <= mid) {
                    currentSum += num;
                } else {
                    subarrays++;
                    currentSum = num;
                }
            }
            
            boolean possible = subarrays <= k;
            
            System.out.printf("Iteration %d: mid = %d, subarrays needed = %d, ", 
                iteration, mid, subarrays);
            System.out.println(possible ? "✓ possible" : "✗ too small");
            
            if (possible) {
                right = mid;
            } else {
                left = mid + 1;
            }
            
            System.out.println("  New range: [" + left + ", " + right + "]");
            iteration++;
        }
        
        System.out.println("\nFinal answer: " + left);
    }
}

/**
 * COMPLEXITY ANALYSIS
 * ===================
 * 
 * Approach 1 - Binary Search (OPTIMAL):
 * Time:  O(n * log(sum - max))
 *        - Binary search iterations: O(log(sum - max))
 *        - Each validation: O(n)
 *        - Where sum = total of all elements
 * Space: O(1) - only variables
 * 
 * Approach 2 - Dynamic Programming:
 * Time:  O(n² * k)
 *        - States: n * k
 *        - Each state: O(n) transitions
 * Space: O(n * k) - DP table
 * 
 * Approach 3 - DP Space Optimized:
 * Time:  O(n² * k)
 * Space: O(n) - only store two rows
 * 
 * When to use which:
 * - Binary Search: When sum is reasonable, ALWAYS preferred
 * - DP: When you need the actual split positions (can track)
 * 
 * 
 * INTERVIEW STRATEGY
 * ==================
 * 
 * 1. CLARIFY THE PROBLEM:
 *    Q: "Can subarrays be empty?"
 *    A: No, all must be non-empty
 *    
 *    Q: "Must I use exactly k splits or at most k?"
 *    A: Exactly k non-empty subarrays
 *    
 *    Q: "Can I rearrange elements?"
 *    A: No, subarrays must be contiguous
 *    
 *    Q: "What if k > n?"
 *    A: Usually guaranteed k ≤ n
 * 
 * 2. RECOGNIZE THE PATTERN:
 *    "This is a 'minimize the maximum' problem.
 *     Classic pattern: Binary Search on the Answer!"
 * 
 * 3. EXPLAIN THE INSIGHT:
 *    "Instead of searching for split positions,
 *     I'll search for the answer directly.
 *     
 *     The answer is bounded:
 *     - Minimum: largest element (one subarray has it)
 *     - Maximum: sum of all (all in one subarray)
 *     
 *     For any candidate answer M, I can check if it's
 *     achievable using a greedy approach."
 * 
 * 4. DESCRIBE THE ALGORITHM:
 *    "I'll binary search on [max, sum]:
 *     
 *     1. For each mid value, check if we can split
 *        array into ≤ k subarrays with max sum ≤ mid
 *     
 *     2. Validation uses greedy:
 *        - Make each subarray as large as possible
 *        - Count how many subarrays we need
 *        - If ≤ k, mid is valid
 *     
 *     3. If valid, try smaller (search left)
 *        If not, try larger (search right)"
 * 
 * 5. WALK THROUGH EXAMPLE:
 *    nums = [7,2,5,10,8], k = 2
 *    
 *    Range: [10, 32]
 *    
 *    Try mid = 21:
 *      [7,2,5] = 14 ✓
 *      [10,8] = 18 ✓
 *      Need 2 subarrays ✓
 *      Valid! Try smaller.
 *    
 *    Try mid = 15:
 *      [7,2,5] = 14 ✓
 *      [10] = 10, [8] = 8
 *      Need 3 subarrays ✗
 *      Too small! Try larger.
 *    
 *    Try mid = 18:
 *      [7,2,5] = 14 ✓
 *      [10,8] = 18 ✓
 *      Need 2 subarrays ✓
 *      Valid! This is the answer.
 * 
 * 6. PROVE WHY GREEDY WORKS:
 *    "For validation, greedy is optimal:
 *     
 *     To minimize the number of subarrays:
 *     - Make each as large as possible (≤ M)
 *     - If we can do it with ≤ k subarrays, M works
 *     - Any other strategy uses more subarrays"
 * 
 * 7. MENTION DP ALTERNATIVE (if time):
 *    "There's also a DP solution:
 *     dp[i][j] = min largest sum for first i elements, j splits
 *     
 *     But it's O(n² * k) vs O(n log sum).
 *     Binary search is more elegant."
 * 
 * 8. EDGE CASES:
 *    ✓ k = 1 (all in one subarray)
 *    ✓ k = n (one element per subarray)
 *    ✓ Single element array
 *    ✓ All elements equal
 *    ✓ Large numbers (avoid overflow)
 *    ✓ Negative numbers (if allowed)
 * 
 * 9. OPTIMIZATION NOTES:
 *    - Early termination in validation loop
 *    - Use long if sum might overflow
 *    - Can precompute prefix sums for DP version
 * 
 * 10. COMMON MISTAKES:
 *     ✗ Wrong binary search bounds
 *     ✗ Using left <= right instead of left < right
 *     ✗ Greedy validation counts wrong
 *     ✗ Forgetting subarrays must be non-empty
 *     ✗ Off-by-one in subarray counting
 *     ✗ Integer overflow with large sums
 * 
 * FOLLOW-UP QUESTIONS:
 * ====================
 * 
 * Q: "What if we want to minimize the sum instead of maximum?"
 * A: Different problem - would need different approach
 *    Likely DP or greedy with different objective
 * 
 * Q: "What if elements can be rearranged?"
 * A: Would sort array first, then becomes easier
 *    Can distribute more evenly
 * 
 * Q: "How to find the actual split positions?"
 * A: Use DP and backtrack, or modify binary search
 *    to construct solution during final validation
 * 
 * Q: "What if k can vary (find optimal k)?"
 * A: Try all k from 1 to n, find best trade-off
 *    Or use elbow method to find optimal k
 * 
 * Q: "What about 2D array splitting?"
 * A: Much more complex - would need different approach
 *    Possibly recursive or advanced DP
 * 
 * RELATED PROBLEMS:
 * =================
 * - Capacity To Ship Packages Within D Days
 * - Koko Eating Bananas
 * - Minimum Speed to Arrive on Time
 * - Divide Chocolate
 * - Painter's Partition Problem
 * 
 * All follow the "Binary Search on Answer" pattern!
 * 
 * RECOMMENDED SOLUTION:
 * Approach 1 (Binary Search) is optimal and elegant.
 * It demonstrates advanced problem-solving by recognizing
 * the pattern and applying binary search creatively.
 * This is the expected solution in top-tier interviews.
 */

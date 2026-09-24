/**
 * NUMBER OF LONGEST INCREASING SUBSEQUENCES - COMPREHENSIVE ANALYSIS
 * ===================================================================
 * 
 * PROBLEM UNDERSTANDING:
 * ---------------------
 * Given: An integer array nums
 * Find: COUNT of all longest increasing subsequences (LIS)
 * 
 * Key Distinctions from Standard LIS:
 * - Standard LIS: Find LENGTH of longest increasing subsequence
 * - This Problem: Find COUNT of how many such subsequences exist
 * 
 * Example 1: [1, 3, 5, 4, 7]
 * - LIS length = 4
 * - Possible LIS: [1, 3, 5, 7] and [1, 3, 4, 7]
 * - Count = 2
 * 
 * Example 2: [2, 2, 2, 2, 2]
 * - LIS length = 1 (any single element)
 * - Count = 5 (each element is an LIS of length 1)
 * 
 * Example 3: [1, 2, 4, 3, 5, 4, 7, 2]
 * - LIS length = 5
 * - Possible: [1, 2, 4, 5, 7], [1, 2, 3, 5, 7], [1, 2, 3, 4, 7]
 * - Count = 3
 * 
 * INTERVIEW APPROACH - HOW TO THINK STEP BY STEP:
 * ================================================
 * 
 * STEP 1: RECOGNIZE THE PATTERN
 * "This is an extension of LIS problem. I need both length AND count."
 * 
 * STEP 2: BUILD ON LIS FOUNDATION
 * Standard LIS uses:
 * - dp[i] = length of LIS ending at index i
 * 
 * For this problem, we need:
 * - length[i] = length of LIS ending at index i
 * - count[i] = number of LIS ending at index i
 * 
 * STEP 3: DERIVE THE RECURRENCE
 * For each position i, look at all j < i where nums[j] < nums[i]:
 * 
 * Case 1: length[j] + 1 > length[i]
 *   → Found a longer subsequence through j
 *   → Update: length[i] = length[j] + 1
 *   → Reset: count[i] = count[j] (inherit all paths from j)
 * 
 * Case 2: length[j] + 1 == length[i]
 *   → Found another way to achieve same length
 *   → Keep: length[i] unchanged
 *   → Add: count[i] += count[j] (add more paths)
 * 
 * Case 3: length[j] + 1 < length[i]
 *   → This path is shorter, ignore it
 * 
 * STEP 4: COMBINE RESULTS
 * - Find max_length = max of all length[i]
 * - Sum count[i] for all i where length[i] == max_length
 * 
 * WHAT TO SAY IN INTERVIEW:
 * "I'll extend the LIS DP approach. Instead of just tracking length,
 * I'll also track how many ways we can achieve that length. When I find
 * a longer sequence, I reset the count. When I find an equal length
 * sequence through a different path, I add to the count."
 * 
 * COMPLEXITY ANALYSIS:
 * -------------------
 * Time Complexity: O(n²) - nested loops over array
 * Space Complexity: O(n) - for length and count arrays
 * 
 * Can we optimize to O(n log n)? 
 * - Yes, using segment tree or BIT, but much more complex
 * - For interviews, O(n²) is usually acceptable
 * - Mention optimization only if asked
 * 
 * EDGE CASES TO CONSIDER:
 * ----------------------
 * 1. Empty array: [] → return 0
 * 2. Single element: [5] → return 1
 * 3. All same elements: [2,2,2,2] → return 4
 * 4. Strictly increasing: [1,2,3,4] → return 1
 * 5. Strictly decreasing: [4,3,2,1] → return 4 (each is LIS of length 1)
 * 6. Multiple LIS of same length: [1,3,5,4,7] → return 2
 */

class NumberOfLongestIncreasingSubsequence {
    
    /**
     * METHOD 1: STANDARD DYNAMIC PROGRAMMING APPROACH - O(n²)
     * =======================================================
     * 
     * DETAILED WALKTHROUGH with [1, 3, 5, 4, 7]:
     * 
     * Initial State:
     * Index:    0   1   2   3   4
     * nums:    [1,  3,  5,  4,  7]
     * length:  [1,  1,  1,  1,  1]  (every element is LIS of length 1)
     * count:   [1,  1,  1,  1,  1]  (one way to form it)
     * 
     * Processing i=1 (nums[1]=3):
     *   j=0: nums[0]=1 < nums[1]=3
     *     length[0]+1 = 2 > length[1]=1
     *     → length[1] = 2, count[1] = count[0] = 1
     * After i=1: length=[1,2,1,1,1], count=[1,1,1,1,1]
     * 
     * Processing i=2 (nums[2]=5):
     *   j=0: nums[0]=1 < nums[2]=5
     *     length[0]+1 = 2 > length[2]=1
     *     → length[2] = 2, count[2] = count[0] = 1
     *   j=1: nums[1]=3 < nums[2]=5
     *     length[1]+1 = 3 > length[2]=2
     *     → length[2] = 3, count[2] = count[1] = 1
     * After i=2: length=[1,2,3,1,1], count=[1,1,1,1,1]
     * 
     * Processing i=3 (nums[3]=4):
     *   j=0: nums[0]=1 < nums[3]=4
     *     length[0]+1 = 2 > length[3]=1
     *     → length[3] = 2, count[3] = count[0] = 1
     *   j=1: nums[1]=3 < nums[3]=4
     *     length[1]+1 = 3 > length[3]=2
     *     → length[3] = 3, count[3] = count[1] = 1
     *   j=2: nums[2]=5 NOT < nums[3]=4, skip
     * After i=3: length=[1,2,3,3,1], count=[1,1,1,1,1]
     * 
     * Processing i=4 (nums[4]=7):
     *   j=0: nums[0]=1 < nums[4]=7
     *     length[0]+1 = 2 > length[4]=1
     *     → length[4] = 2, count[4] = count[0] = 1
     *   j=1: nums[1]=3 < nums[4]=7
     *     length[1]+1 = 3 > length[4]=2
     *     → length[4] = 3, count[4] = count[1] = 1
     *   j=2: nums[2]=5 < nums[4]=7
     *     length[2]+1 = 4 > length[4]=3
     *     → length[4] = 4, count[4] = count[2] = 1
     *   j=3: nums[3]=4 < nums[4]=7
     *     length[3]+1 = 4 == length[4]=4 ← KEY MOMENT!
     *     → count[4] += count[3] = 1 + 1 = 2
     * After i=4: length=[1,2,3,3,4], count=[1,1,1,1,2]
     * 
     * Final Step:
     * max_length = 4
     * Result = sum of count[i] where length[i] = 4 = count[4] = 2
     * 
     * The two LIS are: [1,3,5,7] and [1,3,4,7]
     */
    public static int findNumberOfLIS(int[] nums) {
        // Edge case: empty array
        if (nums == null || nums.length == 0) {
            return 0;
        }
        
        int n = nums.length;
        
        // length[i] = length of longest increasing subsequence ending at i
        int[] length = new int[n];
        
        // count[i] = number of LIS ending at i
        int[] count = new int[n];
        
        // Base case: every element forms an LIS of length 1
        for (int i = 0; i < n; i++) {
            length[i] = 1;
            count[i] = 1;
        }
        
        // Fill the DP arrays
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // Can extend subsequence ending at j to include i?
                if (nums[j] < nums[i]) {
                    // Found a longer subsequence through j
                    if (length[j] + 1 > length[i]) {
                        length[i] = length[j] + 1;
                        count[i] = count[j]; // Reset count, inherit from j
                    }
                    // Found another way to achieve same length
                    else if (length[j] + 1 == length[i]) {
                        count[i] += count[j]; // Add more ways
                    }
                    // else: length[j] + 1 < length[i], ignore (shorter path)
                }
            }
        }
        
        // Find the maximum length
        int maxLength = 0;
        for (int len : length) {
            maxLength = Math.max(maxLength, len);
        }
        
        // Count all subsequences with maximum length
        int result = 0;
        for (int i = 0; i < n; i++) {
            if (length[i] == maxLength) {
                result += count[i];
            }
        }
        
        return result;
    }
    
    /**
     * METHOD 2: WITH DETAILED TRACKING (FOR DEBUGGING/UNDERSTANDING)
     * ==============================================================
     * 
     * This method includes detailed printing to help understand
     * how the algorithm works. Useful for interviews to explain logic.
     */
    public static int findNumberOfLISWithTracking(int[] nums) {
        if (nums == null || nums.length == 0) {
            return 0;
        }
        
        int n = nums.length;
        int[] length = new int[n];
        int[] count = new int[n];
        
        for (int i = 0; i < n; i++) {
            length[i] = 1;
            count[i] = 1;
        }
        
        System.out.println("Initial state:");
        printState(nums, length, count);
        
        for (int i = 1; i < n; i++) {
            System.out.println("\nProcessing i=" + i + " (nums[" + i + "]=" + nums[i] + "):");
            
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {
                    System.out.println("  j=" + j + ": nums[" + j + "]=" + nums[j] + 
                                     " < nums[" + i + "]=" + nums[i]);
                    
                    if (length[j] + 1 > length[i]) {
                        System.out.println("    Found longer: length[" + j + "]+1=" + 
                                         (length[j]+1) + " > length[" + i + "]=" + length[i]);
                        length[i] = length[j] + 1;
                        count[i] = count[j];
                        System.out.println("    Updated: length[" + i + "]=" + length[i] + 
                                         ", count[" + i + "]=" + count[i]);
                    } else if (length[j] + 1 == length[i]) {
                        System.out.println("    Found equal: length[" + j + "]+1=" + 
                                         (length[j]+1) + " == length[" + i + "]=" + length[i]);
                        count[i] += count[j];
                        System.out.println("    Added: count[" + i + "]=" + count[i]);
                    }
                }
            }
            
            printState(nums, length, count);
        }
        
        int maxLength = 0;
        for (int len : length) {
            maxLength = Math.max(maxLength, len);
        }
        
        int result = 0;
        System.out.println("\nFinding result (maxLength=" + maxLength + "):");
        for (int i = 0; i < n; i++) {
            if (length[i] == maxLength) {
                System.out.println("  length[" + i + "]=" + maxLength + 
                                 ", adding count[" + i + "]=" + count[i]);
                result += count[i];
            }
        }
        
        return result;
    }
    
    private static void printState(int[] nums, int[] length, int[] count) {
        System.out.print("  nums:   [");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i] + (i < nums.length-1 ? ", " : ""));
        }
        System.out.println("]");
        
        System.out.print("  length: [");
        for (int i = 0; i < length.length; i++) {
            System.out.print(length[i] + (i < length.length-1 ? ", " : ""));
        }
        System.out.println("]");
        
        System.out.print("  count:  [");
        for (int i = 0; i < count.length; i++) {
            System.out.print(count[i] + (i < count.length-1 ? ", " : ""));
        }
        System.out.println("]");
    }
    
    /**
     * METHOD 3: SPACE-OPTIMIZED VERSION (NOT REALLY APPLICABLE HERE)
     * ==============================================================
     * 
     * Unlike some DP problems, we can't easily optimize space here because:
     * - We need to keep all length[i] values to find maximum
     * - We need to keep all count[i] values to sum them up
     * - We need random access to previous values
     * 
     * So O(n) space is optimal for this problem.
     * 
     * This is worth mentioning in an interview to show you've considered
     * optimizations even if they're not applicable.
     */
    
    /**
     * METHOD 4: HANDLING OVERFLOW (IMPORTANT FOR LARGE INPUTS)
     * ========================================================
     * 
     * If the count can be very large, we might need to handle overflow.
     * Typically, problems ask for result modulo 10^9 + 7.
     */
    public static int findNumberOfLISWithMod(int[] nums) {
        if (nums == null || nums.length == 0) {
            return 0;
        }
        
        final int MOD = 1_000_000_007;
        int n = nums.length;
        int[] length = new int[n];
        long[] count = new long[n]; // Use long to prevent overflow
        
        for (int i = 0; i < n; i++) {
            length[i] = 1;
            count[i] = 1;
        }
        
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {
                    if (length[j] + 1 > length[i]) {
                        length[i] = length[j] + 1;
                        count[i] = count[j];
                    } else if (length[j] + 1 == length[i]) {
                        count[i] = (count[i] + count[j]) % MOD;
                    }
                }
            }
        }
        
        int maxLength = 0;
        for (int len : length) {
            maxLength = Math.max(maxLength, len);
        }
        
        long result = 0;
        for (int i = 0; i < n; i++) {
            if (length[i] == maxLength) {
                result = (result + count[i]) % MOD;
            }
        }
        
        return (int) result;
    }
    
    /**
     * METHOD 5: PRINT ACTUAL SUBSEQUENCES (ADVANCED)
     * ==============================================
     * 
     * If asked to print all longest increasing subsequences.
     * This is more complex and typically not asked in interviews,
     * but shows deep understanding.
     */
    public static void printAllLIS(int[] nums) {
        if (nums == null || nums.length == 0) {
            System.out.println("Empty array");
            return;
        }
        
        int n = nums.length;
        int[] length = new int[n];
        int[] count = new int[n];
        
        for (int i = 0; i < n; i++) {
            length[i] = 1;
            count[i] = 1;
        }
        
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {
                    if (length[j] + 1 > length[i]) {
                        length[i] = length[j] + 1;
                        count[i] = count[j];
                    } else if (length[j] + 1 == length[i]) {
                        count[i] += count[j];
                    }
                }
            }
        }
        
        int maxLength = 0;
        for (int len : length) {
            maxLength = Math.max(maxLength, len);
        }
        
        System.out.println("All Longest Increasing Subsequences:");
        for (int i = 0; i < n; i++) {
            if (length[i] == maxLength) {
                backtrack(nums, length, i, maxLength, new java.util.ArrayList<>());
            }
        }
    }
    
    private static void backtrack(int[] nums, int[] length, int index, 
                                  int remainingLength, java.util.List<Integer> current) {
        current.add(0, nums[index]);
        
        if (remainingLength == 1) {
            System.out.println(current);
            current.remove(0);
            return;
        }
        
        for (int j = 0; j < index; j++) {
            if (nums[j] < nums[index] && length[j] == remainingLength - 1) {
                backtrack(nums, length, j, remainingLength - 1, current);
            }
        }
        
        current.remove(0);
    }
    
    /**
     * TEST CASES
     * ==========
     */
    public static void main(String[] args) {
        // Test Case 1: Standard case with multiple LIS
        System.out.println("=== Test Case 1: [1, 3, 5, 4, 7] ===");
        int[] arr1 = {1, 3, 5, 4, 7};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr1));
        System.out.println("\nDetailed tracking:");
        findNumberOfLISWithTracking(arr1);
        printAllLIS(arr1);
        
        // Test Case 2: All same elements
        System.out.println("\n\n=== Test Case 2: [2, 2, 2, 2, 2] ===");
        int[] arr2 = {2, 2, 2, 2, 2};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr2));
        
        // Test Case 3: Strictly increasing
        System.out.println("\n=== Test Case 3: [1, 2, 3, 4, 5] ===");
        int[] arr3 = {1, 2, 3, 4, 5};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr3));
        
        // Test Case 4: Strictly decreasing
        System.out.println("\n=== Test Case 4: [5, 4, 3, 2, 1] ===");
        int[] arr4 = {5, 4, 3, 2, 1};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr4));
        
        // Test Case 5: Complex case
        System.out.println("\n=== Test Case 5: [1, 2, 4, 3, 5, 4, 7, 2] ===");
        int[] arr5 = {1, 2, 4, 3, 5, 4, 7, 2};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr5));
        printAllLIS(arr5);
        
        // Test Case 6: LeetCode Example
        System.out.println("\n=== Test Case 6: [1, 3, 5, 4, 7] ===");
        int[] arr6 = {1, 3, 5, 4, 7};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr6));
        
        // Test Case 7: LeetCode Example
        System.out.println("\n=== Test Case 7: [2, 2, 2, 2, 2] ===");
        int[] arr7 = {2, 2, 2, 2, 2};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr7));
        
        // Test Case 8: Single element
        System.out.println("\n=== Test Case 8: [5] ===");
        int[] arr8 = {5};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr8));
        
        // Test Case 9: Empty array
        System.out.println("\n=== Test Case 9: [] ===");
        int[] arr9 = {};
        System.out.println("Number of LIS: " + findNumberOfLIS(arr9));
    }
}

/**
 * INTERVIEW STRATEGY AND COMMON PITFALLS:
 * ========================================
 * 
 * HOW TO APPROACH IN AN INTERVIEW:
 * 
 * 1. CLARIFY (2 minutes):
 *    - "Just to confirm, we need the COUNT of all LIS, not just the length?"
 *    - "Is the sequence strictly increasing or can it have equal elements?"
 *    - "What should I return for an empty array?"
 * 
 * 2. EXAMPLES (3 minutes):
 *    - Draw out [1,3,5,4,7] on whiteboard
 *    - Show both LIS: [1,3,5,7] and [1,3,4,7]
 *    - Count = 2
 *    - This helps YOU understand and shows interviewer you're thorough
 * 
 * 3. BRUTE FORCE (2 minutes):
 *    - "I could generate all 2^n subsequences and count those that are LIS"
 *    - "But that's exponential, let me think of something better"
 * 
 * 4. OPTIMIZATION (5 minutes):
 *    - "This is similar to standard LIS"
 *    - "In LIS, we track length. Here, I also need to track count"
 *    - Explain the two cases: longer vs equal length
 *    - Draw state transitions
 * 
 * 5. CODE (15 minutes):
 *    - Write clean code with good variable names
 *    - Add comments for tricky parts
 *    - Handle edge cases
 * 
 * 6. TEST (3 minutes):
 *    - Walk through your example
 *    - Test edge cases mentally
 *    - Look for off-by-one errors
 * 
 * COMMON MISTAKES:
 * ===============
 * 
 * 1. FORGETTING TO RESET COUNT:
 *    ❌ if (length[j] + 1 > length[i]) {
 *          length[i] = length[j] + 1;
 *          count[i] += count[j];  // WRONG! Should assign, not add
 *       }
 *    
 *    ✓ if (length[j] + 1 > length[i]) {
 *          length[i] = length[j] + 1;
 *          count[i] = count[j];  // CORRECT! Reset count
 *      }
 * 
 * 2. INITIALIZING COUNT TO 0:
 *    ❌ count[i] = 0;  // WRONG! Every element is an LIS of length 1
 *    ✓ count[i] = 1;  // CORRECT!
 * 
 * 3. NOT SUMMING ALL MAX-LENGTH SUBSEQUENCES:
 *    ❌ return count[n-1];  // WRONG! Max might not end at last element
 *    ✓ Sum all count[i] where length[i] == maxLength
 * 
 * 4. OVERFLOW ISSUES:
 *    - For large inputs, count can overflow
 *    - Use long or apply modulo
 * 
 * 5. CONFUSING WITH LCS:
 *    - LCS = Longest Common Subsequence (different problem!)
 *    - LIS = Longest Increasing Subsequence (this problem)
 * 
 * TIME MANAGEMENT:
 * ===============
 * 30-minute interview:
 * - 2 min: Understand problem
 * - 3 min: Examples
 * - 2 min: Brute force
 * - 5 min: DP approach explanation
 * - 15 min: Code
 * - 3 min: Test
 * 
 * 45-minute interview:
 * - Add 10 min for optimizations discussion
 * - Add 5 min for printing actual subsequences
 * 
 * FOLLOW-UP QUESTIONS YOU MIGHT GET:
 * ==================================
 * 
 * Q: "Can you optimize to O(n log n)?"
 * A: "Yes, using segment tree or Binary Indexed Tree to maintain
 *     (length, count) pairs, but it's quite complex. The O(n²)
 *     solution is more practical for most inputs."
 * 
 * Q: "What if we want to print all LIS?"
 * A: "I'd need to add parent tracking and backtracking. Time
 *     complexity would be O(n² + k*L) where k is number of LIS
 *     and L is their length."
 * 
 * Q: "How would you handle very large counts?"
 * A: "Use long instead of int, or apply modulo 10^9+7 if the
 *     problem specifies it."
 * 
 * Q: "What if we want longest non-decreasing subsequence?"
 * A: "Change nums[j] < nums[i] to nums[j] <= nums[i]"
 * 
 * RELATED PROBLEMS:
 * ================
 * 1. Longest Increasing Subsequence (LeetCode 300)
 * 2. Longest Bitonic Subsequence
 * 3. Russian Doll Envelopes (LeetCode 354)
 * 4. Maximum Length of Pair Chain (LeetCode 646)
 * 5. Delete Columns to Make Sorted III (LeetCode 960)
 */

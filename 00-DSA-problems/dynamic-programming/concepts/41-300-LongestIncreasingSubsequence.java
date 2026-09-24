
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * LONGEST INCREASING SUBSEQUENCE (LIS) - COMPLETE GUIDE
 * =====================================================
 * 
 * PROBLEM UNDERSTANDING:
 * - Given an array, find the LENGTH of the longest strictly increasing subsequence
 * - Subsequence: elements maintain relative order but don't need to be contiguous
 * - Strictly increasing: each element must be greater than the previous (not equal)
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ===============================================
 * 
 * Step 1: Understand with examples
 * [10,9,2,5,3,7,101,18]
 * - We can pick [2,5,7,101] or [2,3,7,101] or [2,3,7,18]
 * - All have length 4, which is the maximum
 * 
 * Step 2: Identify it's a subsequence problem (not subarray)
 * - This hints at DP or recursive solutions
 * - We need to make "include/exclude" decisions for each element
 * 
 * Step 3: Think about brute force
 * - Try all possible subsequences: 2^n possibilities
 * - For each, check if increasing and track max length
 * - Too slow, but helps understand the problem
 * 
 * Step 4: Find overlapping subproblems (DP indicator)
 * - "What's the LIS ending at index i?" is asked multiple times
 * - This suggests we can cache results
 * 
 * Step 5: Define DP state
 * - dp[i] = length of LIS ending at index i (must include nums[i])
 * - For each position, we look back at all previous positions
 * 
 * Step 6: Optimize if time permits
 * - Can we avoid looking at all previous elements?
 * - Binary search on a "tails" array can help
 */

class LongestIncreasingSubsequence {
    
    /*
     * APPROACH 1: DYNAMIC PROGRAMMING - O(n²)
     * =======================================
     * This is the BEST approach to explain in an interview first.
     * It's intuitive and demonstrates clear understanding.
     * 
     * INTUITION:
     * - For each position i, we ask: "What's the longest increasing 
     *   subsequence that ENDS at position i?"
     * - To answer this, we look at all positions j < i where nums[j] < nums[i]
     * - We can extend any LIS ending at j by adding nums[i]
     * - We take the maximum among all such extensions
     * 
     * STATE DEFINITION:
     * dp[i] = length of longest increasing subsequence ending at index i
     * 
     * RECURRENCE RELATION:
     * dp[i] = max(dp[j] + 1) for all j < i where nums[j] < nums[i]
     * dp[i] = 1 if no such j exists (nums[i] starts a new subsequence)
     * 
     * BASE CASE:
     * Every single element is an increasing subsequence of length 1
     * So dp[i] = 1 initially for all i
     * 
     * ANSWER:
     * max(dp[i]) for all i (LIS can end at any position)
     */
    public int lengthOfLIS_DP(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        int n = nums.length;
        
        // dp[i] represents the length of LIS ending at index i
        int[] dp = new int[n];
        
        // Base case: each element alone is a subsequence of length 1
        for (int i = 0; i < n; i++) {
            dp[i] = 1;
        }
        
        // For each position i, look at all previous positions j
        for (int i = 1; i < n; i++) {
            // Check all elements before position i
            for (int j = 0; j < i; j++) {
                // If nums[j] < nums[i], we can extend the LIS ending at j
                if (nums[j] < nums[i]) {
                    // We can either keep current dp[i] or extend from dp[j]
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
        }
        
        // The answer is the maximum value in dp array
        // because LIS can end at any position
        int maxLength = 0;
        for (int length : dp) {
            maxLength = Math.max(maxLength, length);
        }
        
        return maxLength;
    }
    
    /*
     * APPROACH 2: BINARY SEARCH + GREEDY - O(n log n)
     * ===============================================
     * This is an OPTIMIZATION to mention if interviewer asks for better.
     * Start with DP approach, then say "we can optimize this".
     * 
     * KEY INSIGHT (The "Aha!" moment):
     * - If we're building an increasing subsequence, we want each element
     *   to be as SMALL as possible to have more room for future elements
     * - Example: If we have [1, 5] and see 3, we should replace 5 with 3
     *   to get [1, 3] because this gives us better chances later
     * 
     * ALGORITHM:
     * - Maintain a "tails" array where tails[i] = smallest tail element
     *   of all increasing subsequences of length i+1
     * - For each number, use binary search to find where it fits
     * - If it's larger than all elements, append it (longer subsequence found)
     * - Otherwise, replace the first element that's >= it (improve that length)
     * 
     * WHY THIS WORKS:
     * - We're not tracking actual subsequences, just the best "tails"
     * - The length of tails array = length of LIS
     * - Replacing elements maintains the invariant: tails is always sorted
     * 
     * EXAMPLE WALKTHROUGH: [10,9,2,5,3,7,101,18]
     * 
     * num=10: tails=[10], len=1
     * num=9:  tails=[9], len=1    (replace 10 with 9, smaller is better)
     * num=2:  tails=[2], len=1    (replace 9 with 2)
     * num=5:  tails=[2,5], len=2  (5 > 2, so append)
     * num=3:  tails=[2,3], len=2  (replace 5 with 3, smaller tail better)
     * num=7:  tails=[2,3,7], len=3 (7 > 3, so append)
     * num=101:tails=[2,3,7,101], len=4 (append)
     * num=18: tails=[2,3,7,18], len=4 (replace 101 with 18)
     * 
     * Result: length = 4
     */
    public int lengthOfLIS_BinarySearch(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        // tails[i] = smallest ending element of all increasing subsequences
        // of length i+1
        // This array is always sorted!
        int[] tails = new int[nums.length];
        int len = 0; // Current length of tails array (also the LIS length so far)
        
        for (int num : nums) {
            // Binary search to find the position where num should go
            // We're looking for the leftmost position where tails[pos] >= num
            int left = 0, right = len;
            
            while (left < right) {
                int mid = left + (right - left) / 2;
                if (tails[mid] < num) {
                    left = mid + 1; // num is larger, search right half
                } else {
                    right = mid; // tails[mid] >= num, search left half
                }
            }
            
            // After binary search, left is the position to insert/replace
            tails[left] = num;
            
            // If left == len, we're extending the subsequence
            if (left == len) {
                len++;
            }
            // Otherwise, we're replacing an element to keep a smaller tail
        }
        
        return len;
    }
    
    /*
     * APPROACH 3: RECURSION WITH MEMOIZATION
     * ======================================
     * This demonstrates understanding of the recursive nature.
     * Good to mention briefly, but DP is cleaner for interviews.
     * 
     * RECURSIVE THINKING:
     * - For each element, we have two choices: include it or skip it
     * - If we include nums[i], we need nums[i] > previous element
     * - We recursively solve for remaining elements
     * 
     * MEMOIZATION:
     * - State: (current index, previous element index)
     * - We cache results to avoid recomputation
     */
    public int lengthOfLIS_Recursive(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        // memo[i][j] = LIS starting from index i with previous element at j
        // j = nums.length means no previous element (start of subsequence)
        Integer[][] memo = new Integer[nums.length][nums.length + 1];
        
        return helper(nums, 0, -1, memo);
    }
    
    private int helper(int[] nums, int curr, int prev, Integer[][] memo) {
        // Base case: reached end of array
        if (curr == nums.length) {
            return 0;
        }
        
        // Check memo (prev+1 because prev can be -1)
        if (memo[curr][prev + 1] != null) {
            return memo[curr][prev + 1];
        }
        
        // Option 1: Skip current element
        int skip = helper(nums, curr + 1, prev, memo);
        
        // Option 2: Take current element (only if it's valid)
        int take = 0;
        if (prev == -1 || nums[curr] > nums[prev]) {
            take = 1 + helper(nums, curr + 1, curr, memo);
        }
        
        // Store and return the maximum
        memo[curr][prev + 1] = Math.max(skip, take);
        return memo[curr][prev + 1];
    }
    
    /*
     * INTERVIEW STRATEGY:
     * ===================
     * 
     * 1. START with clarifying questions (2 minutes):
     *    - "Just to confirm, we need strictly increasing (not equal)?"
     *    - "The subsequence doesn't need to be contiguous, right?"
     *    - "What's the expected size of the input?"
     * 
     * 2. WALK THROUGH an example (2 minutes):
     *    - Use the given example
     *    - Show how [2,3,7,101] is formed
     *    - This demonstrates understanding
     * 
     * 3. EXPLAIN the DP approach first (5 minutes):
     *    - "I'll start with a dynamic programming solution"
     *    - Clearly state the dp[i] definition
     *    - Walk through the recurrence relation
     *    - Mention time: O(n²), space: O(n)
     * 
     * 4. CODE the DP solution (8 minutes):
     *    - Write clean, commented code
     *    - Test with the example as you write
     * 
     * 5. OPTIMIZE if time permits (5 minutes):
     *    - "We can optimize this to O(n log n) using binary search"
     *    - Explain the key insight about smaller tails
     *    - Code it if interviewer is interested
     * 
     * 6. TEST with edge cases (3 minutes):
     *    - Empty array
     *    - Single element
     *    - All decreasing
     *    - All equal
     *    - Already sorted
     * 
     * COMMON MISTAKES TO AVOID:
     * - Confusing subsequence with subarray (contiguous)
     *   Solution: Emphasize "relative order maintained, not contiguous"
     * 
     * - Forgetting to initialize dp[i] = 1
     *   Solution: Remember each element alone is a valid subsequence
     * 
     * - Returning dp[n-1] instead of max(dp)
     *   Solution: LIS can end at ANY position, not just the last
     * 
     * - In binary search, using wrong comparison
     *   Solution: We want the leftmost position where tails[pos] >= num
     */
    
    // Test cases
    public static void main(String[] args) {
        LongestIncreasingSubsequence solution = new LongestIncreasingSubsequence();
        
        // Test case 1
        int[] nums1 = {10, 9, 2, 5, 3, 7, 101, 18};
        System.out.println("Test 1: " + java.util.Arrays.toString(nums1));
        System.out.println("DP Solution: " + solution.lengthOfLIS_DP(nums1)); // 4
        System.out.println("Binary Search: " + solution.lengthOfLIS_BinarySearch(nums1)); // 4
        System.out.println("Recursive: " + solution.lengthOfLIS_Recursive(nums1)); // 4
        System.out.println();
        
        // Test case 2
        int[] nums2 = {0, 1, 0, 3, 2, 3};
        System.out.println("Test 2: " + java.util.Arrays.toString(nums2));
        System.out.println("DP Solution: " + solution.lengthOfLIS_DP(nums2)); // 4
        System.out.println("Binary Search: " + solution.lengthOfLIS_BinarySearch(nums2)); // 4
        System.out.println();
        
        // Test case 3
        int[] nums3 = {7, 7, 7, 7, 7, 7, 7};
        System.out.println("Test 3: " + java.util.Arrays.toString(nums3));
        System.out.println("DP Solution: " + solution.lengthOfLIS_DP(nums3)); // 1
        System.out.println("Binary Search: " + solution.lengthOfLIS_BinarySearch(nums3)); // 1
        System.out.println();
        
        // Edge case: empty array
        int[] nums4 = {};
        System.out.println("Test 4 (empty): " + java.util.Arrays.toString(nums4));
        System.out.println("DP Solution: " + solution.lengthOfLIS_DP(nums4)); // 0
        System.out.println();
        
        // Edge case: single element
        int[] nums5 = {5};
        System.out.println("Test 5 (single): " + java.util.Arrays.toString(nums5));
        System.out.println("DP Solution: " + solution.lengthOfLIS_DP(nums5)); // 1
        System.out.println();
        
        // Edge case: strictly decreasing
        int[] nums6 = {10, 9, 8, 7, 6, 5};
        System.out.println("Test 6 (decreasing): " + java.util.Arrays.toString(nums6));
        System.out.println("DP Solution: " + solution.lengthOfLIS_DP(nums6)); // 1
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * ====================
 * 
 * Approach 1 (DP):
 * - Time: O(n²) - two nested loops over the array
 * - Space: O(n) - dp array of size n
 * 
 * Approach 2 (Binary Search):
 * - Time: O(n log n) - n elements × log n binary search
 * - Space: O(n) - tails array of size n
 * 
 * Approach 3 (Recursion with Memo):
 * - Time: O(n²) - n × n states in memo table
 * - Space: O(n²) - memo table + O(n) recursion stack
 * 
 * WHEN TO USE WHICH:
 * - Interview: Start with DP (Approach 1), it's most intuitive
 * - Production: Use Binary Search (Approach 2) for large inputs
 * - Learning: Study all three to understand the problem deeply
 */

/*
 Problem:
 --------
 Given an integer array nums, return the length of the longest strictly increasing subsequence.

 Example:
   nums = [10,9,2,5,3,7,101,18]
   LIS = [2,3,7,101] => length = 4

 Interview Goals:
 ----------------
 1. Start with brute force thinking.
 2. Convert brute force to Dynamic Programming.
 3. Optimize DP using Binary Search (Patience Sorting idea).
 4. Clearly explain tradeoffs and complexities.

 ---------------------------------------------------------
 HOW TO THINK ABOUT THIS IN AN INTERVIEW:
 ---------------------------------------------------------
 1. Understand the problem:
    - We need a subsequence (not necessarily contiguous).
    - It must be strictly increasing.
    - We only need the LENGTH, not the actual sequence.

 2. First thought (Brute Force):
    - Try all subsequences => 2^n possibilities => too slow.

 3. DP Insight:
    - Let dp[i] = length of LIS that ends at index i.
    - For each i, look at all j < i:
        if nums[j] < nums[i],
        then dp[i] = max(dp[i], dp[j] + 1).
    - Final answer = max(dp[i]).

    Time: O(n^2), Space: O(n)
    This is usually expected first in interviews.

 4. Optimization Insight:
    - We don't need the actual LIS, only the length.
    - Maintain an array "tails" where:
        tails[k] = the smallest ending value of an increasing subsequence of length (k + 1).
    - For each number:
        - Use binary search to place it in tails.
    - Length of tails = answer.

    Time: O(n log n), Space: O(n)
    This is the optimized version.

 ---------------------------------------------------------
 */

class LongestIncreasingSubsequence2 {

    public static void main(String[] args) {

        int[] nums1 = {10, 9, 2, 5, 3, 7, 101, 18};
        int[] nums2 = {0, 1, 0, 3, 2, 3};
        int[] nums3 = {7, 7, 7, 7, 7, 7, 7};

        System.out.println("O(n^2) DP Solution:");
        System.out.println(lisDP(nums1)); // 4
        System.out.println(lisDP(nums2)); // 4
        System.out.println(lisDP(nums3)); // 1

        System.out.println("\nO(n log n) Optimized Solution:");
        System.out.println(lisBinarySearch(nums1)); // 4
        System.out.println(lisBinarySearch(nums2)); // 4
        System.out.println(lisBinarySearch(nums3)); // 1
    }

    /*
     ---------------------------------------------------------
     APPROACH 1: O(n^2) Dynamic Programming
     ---------------------------------------------------------

     Idea:
     -----
     dp[i] = length of LIS that ends exactly at index i.

     Transition:
     -----------
     For every i:
       - Start with dp[i] = 1 (at least the element itself).
       - For every j < i:
           if nums[j] < nums[i]:
               dp[i] = max(dp[i], dp[j] + 1)

     Answer:
     --------
     max(dp[i]) for all i.

     Time Complexity:
     ----------------
     O(n^2)

     Space Complexity:
     -----------------
     O(n)

     Interview Tip:
     --------------
     Always start with this version. It's intuitive and easy to explain.
     */

    public static int lisDP(int[] nums) {

        // Edge case: empty array
        if (nums == null || nums.length == 0) {
            return 0;
        }

        int n = nums.length;

        // dp[i] = LIS length ending at index i
        int[] dp = new int[n];

        // Every element is an LIS of length 1 by itself
        Arrays.fill(dp, 1);

        int maxLen = 1;

        // Build the dp array
        for (int i = 0; i < n; i++) {

            // Look at all previous elements
            for (int j = 0; j < i; j++) {

                // Only extend if strictly increasing
                if (nums[j] < nums[i]) {

                    // If extending j gives a longer subsequence
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }

            // Track global maximum
            maxLen = Math.max(maxLen, dp[i]);
        }

        return maxLen;
    }

    /*
     ---------------------------------------------------------
     APPROACH 2: O(n log n) Binary Search (Patience Sorting)
     ---------------------------------------------------------

     Idea:
     -----
     We maintain an array "tails" where:
       tails[k] = the smallest possible ending value of
                  an increasing subsequence of length (k + 1).

     Invariant:
     ----------
     - tails is always sorted.
     - tails.size() = length of LIS so far.

     For each number x in nums:
       - Use binary search on tails:
           Find the first index i such that tails[i] >= x.
       - Replace tails[i] = x.
       - If no such index exists, append x.

     Why this works:
     ---------------
     - We are greedily keeping subsequences as "flexible" as possible.
     - Smaller ending values allow more future extensions.

     Important Nuance:
     -----------------
     - tails does NOT store the actual LIS.
     - It only helps us compute the length.

     Time Complexity:
     ----------------
     O(n log n)

     Space Complexity:
     -----------------
     O(n)

     Interview Tip:
     --------------
     Explain this only after DP.
     Emphasize that this returns the LENGTH, not the sequence.
     */

    public static int lisBinarySearch(int[] nums) {

        // Edge case: empty array
        if (nums == null || nums.length == 0) {
            return 0;
        }

        // This list will store the "tails"
        List<Integer> tails = new ArrayList<>();

        for (int num : nums) {

            // Binary search to find the first element >= num
            int left = 0;
            int right = tails.size() - 1;

            while (left <= right) {
                int mid = left + (right - left) / 2;

                if (tails.get(mid) < num) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }

            // left is the position to replace or append
            if (left == tails.size()) {
                tails.add(num);
            } else {
                tails.set(left, num);
            }
        }

        // Length of tails = length of LIS
        return tails.size();
    }
}

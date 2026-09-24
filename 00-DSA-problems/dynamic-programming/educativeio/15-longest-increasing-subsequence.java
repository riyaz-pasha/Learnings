import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Longest Increasing Subsequence (LIS)
 * The Longest Increasing Subsequence is the longest subsequence from a given 
 * array where the elements are sorted in strictly increasing order.
 * Given an integer array nums, find the length of the LIS.
 * 
 * Constraints:
 * 1 <= nums.length <= 1000
 * -10^4 <= nums[i] <= 10^4
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, clarifying the definition of "subsequence" is critical:
 * 
 * Q: "Does the sequence need to be contiguous?"
 * A: No, that would be a "subarray". A "subsequence" allows us to skip elements 
 *    as long as we maintain the original relative order.
 * 
 * Q: "What does 'strictly increasing' mean for duplicates?"
 * A: It means `nums[i] < nums[j]` for `i < j`. If we have `[2, 2]`, the LIS 
 *    length is 1, not 2. We cannot include equal elements.
 * 
 * Q: "Are negative numbers allowed?"
 * A: Yes, constraints show -10^4 <= nums[i]. This doesn't change our DP logic 
 *    because we only care about relative order/values, not absolute sums.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given element 'i', I have to decide whether to INCLUDE it in my 
 * growing subsequence or EXCLUDE it. 
 * 
 * To INCLUDE it, it MUST be strictly greater than the last element I added. 
 * This means my decision relies on two pieces of state:
 * 1. The current index I am looking at.
 * 2. The index of the PREVIOUS element I decided to include.
 * 
 * Because we can arrive at the same state (same current index, same previous 
 * included element) through different combinations of skipped elements, we have 
 * overlapping subproblems. This screams Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: nums = [10, 9, 2, 5, 3, 7, 101, 18]
 * 
 * Let's trace the 1D Tabulation array. 
 * dp[i] means "The length of the longest increasing subsequence that strictly ENDS at index i".
 * Initially, every element is an LIS of length 1 (just itself).
 * dp = [1, 1, 1, 1, 1, 1, 1, 1]
 * 
 * i=0 (10): dp[0] = 1
 * i=1 (9):  Is 9 > 10? No. dp[1] = 1
 * i=2 (2):  Is 2 > 10? No. Is 2 > 9? No. dp[2] = 1
 * i=3 (5):  Is 5 > 2? YES. dp[3] = max(dp[3], dp[2] + 1) = 2.  (Sequence: [2, 5])
 * i=4 (3):  Is 3 > 2? YES. dp[4] = max(dp[4], dp[2] + 1) = 2.  (Sequence: [2, 3])
 * i=5 (7):  Is 7 > 2, 5, 3? YES to all.
 *           Check against 2: dp[5] = max(1, 1+1) = 2
 *           Check against 5: dp[5] = max(2, 2+1) = 3  (Seq: [2, 5, 7])
 *           Check against 3: dp[5] = max(3, 2+1) = 3  (Seq: [2, 3, 7])
 * 
 * We continue this process. The maximum value in the `dp` array at the end is our answer.
 */
public class LongestIncreasingSubsequence {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Traverse the array. For each element, we can skip it. If it is 
     * strictly greater than our previously picked element, we can also try 
     * picking it.
     * 
     * Time Complexity: O(2^n) - We make up to 2 branching decisions per element.
     * Space Complexity: O(n) - Maximum depth of the recursion tree.
     */
    public int lengthOfLISRecursive(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        // Start with current index 0, and 'previous index' as -1 (meaning no element picked yet)
        return solveRecursive(nums, 0, -1);
    }

    private int solveRecursive(int[] nums, int currentIndex, int previousIndex) {
        // BASE CASE REASONING:
        // If we have advanced our pointer past the end of the array, there are 
        // literally no more numbers to evaluate. The length of a subsequence 
        // formed from an empty array is 0.
        if (currentIndex == nums.length) {
            return 0;
        }

        // Choice 1: Exclude the current element.
        // We move to the next index, and the 'previous' included element stays the same.
        int exclude = solveRecursive(nums, currentIndex + 1, previousIndex);

        // Choice 2: Include the current element (if valid).
        int include = 0;
        // It is valid if we haven't picked anything yet (previousIndex == -1) 
        // OR if the current number is strictly greater than our last picked number.
        if (previousIndex == -1 || nums[currentIndex] > nums[previousIndex]) {
            // We count this element (+1) and move forward, updating our 'previous' 
            // picked index to be the current one.
            include = 1 + solveRecursive(nums, currentIndex + 1, currentIndex);
        }

        return Math.max(exclude, include);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the results of [currentIndex][previousIndex] to avoid 
     * exploring the same decision trees repeatedly.
     * 
     * Time Complexity: O(n^2) - State space is n * n.
     * Space Complexity: O(n^2) - For the 2D memo array + call stack.
     */
    public int lengthOfLISMemo(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        int n = nums.length;
        
        // memo[currentIndex][previousIndex + 1]
        // We use +1 for the previousIndex dimension because previousIndex can be -1,
        // and we cannot use negative indices in a Java array. 
        int[][] memo = new int[n][n + 1];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(nums, 0, -1, memo);
    }

    private int solveMemo(int[] nums, int currentIndex, int previousIndex, int[][] memo) {
        // BASE CASE (Same physical logic as recursion)
        if (currentIndex == nums.length) return 0;

        // Shift previousIndex by +1 to safely access the array
        if (memo[currentIndex][previousIndex + 1] != -1) {
            return memo[currentIndex][previousIndex + 1];
        }

        int exclude = solveMemo(nums, currentIndex + 1, previousIndex, memo);
        
        int include = 0;
        if (previousIndex == -1 || nums[currentIndex] > nums[previousIndex]) {
            include = 1 + solveMemo(nums, currentIndex + 1, currentIndex, memo);
        }

        memo[currentIndex][previousIndex + 1] = Math.max(exclude, include);
        return memo[currentIndex][previousIndex + 1];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 1D)
     * ========================================================================
     * Idea: Instead of keeping a 2D array of (current, previous), we can simplify 
     * the logic dramatically. We can maintain a 1D array where dp[i] represents 
     * the LIS strictly ending at index 'i'.
     * 
     * Time Complexity: O(n^2) - Nested loops comparing every element with its predecessors.
     * Space Complexity: O(n) - 1D array, vastly more memory efficient than Approach 2!
     */
    public int lengthOfLISTabulation(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        int n = nums.length;
        
        // dp[i] answers the question: "What is the longest strictly increasing 
        // subsequence that perfectly ends exactly at index 'i'?"
        int[] dp = new int[n];
        
        // BASE CASE REASONING:
        // By default, every single element in the array is an increasing subsequence 
        // of length 1 (consisting of just itself). We seed the array with 1s.
        Arrays.fill(dp, 1);
        
        int maxLength = 1;

        // Outer loop (i): We want to find the LIS ending at the current item 'i'.
        for (int i = 1; i < n; i++) {
            
            // Inner loop (j): We look back at EVERY item 'j' that came before 'i'.
            // We are asking: "Can I take the subsequence that ended at 'j', and 
            // just append my current item 'i' onto the end of it?"
            for (int j = 0; j < i; j++) {
                
                // PHYSICAL CHECK: Is my current item 'i' strictly greater than 
                // the previous item 'j'? 
                // If it is NOT greater, I cannot append it (it would break the sequence).
                if (nums[i] > nums[j]) {
                    
                    // IT FITS! If I append 'i' to the subsequence ending at 'j', 
                    // the new length will be: dp[j] (the length up to j) + 1 (for item i).
                    // We compare this potential new length against whatever the best 
                    // length for dp[i] is so far, and keep the maximum.
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            
            // As we compute the LIS ending at 'i', we update our global maximum.
            // The absolute longest subsequence could end anywhere in the array.
            maxLength = Math.max(maxLength, dp[i]);
        }

        return maxLength;
    }

    /**
     * ========================================================================
     * APPROACH 4: Binary Search with Patience Sorting (L4/L5 Expectation)
     * ========================================================================
     * Idea: The O(n^2) DP approach is great, but top-tier companies expect the 
     * O(n log n) approach for LIS. 
     * 
     * We build an active array `tails`, where `tails[i]` stores the SMALLEST tail 
     * of all increasing subsequences of length `i+1`.
     * 
     * If the current number `x` is larger than all elements in `tails`, we append it 
     * (the LIS just got longer).
     * If `x` is smaller, we find the first element in `tails` that is >= `x` and 
     * overwrite it. This keeps the potential subsequences extending into the future 
     * as favorable (small) as possible.
     * 
     * Time Complexity: O(n log n) - Binary search takes O(log n) inside an O(n) loop.
     * Space Complexity: O(n) - For the 'tails' array.
     */
    public int lengthOfLISBinarySearch(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        int[] tails = new int[nums.length];
        int size = 0; // Tracks the length of the LIS found so far
        
        for (int x : nums) {
            // Binary search to find the insertion point of 'x' in the 'tails' array
            int left = 0, right = size;
            
            while (left < right) {
                int mid = left + (right - left) / 2;
                if (tails[mid] < x) {
                    left = mid + 1; // x is larger, belongs further right
                } else {
                    right = mid;    // x is smaller or equal, pull boundary left
                }
            }
            
            // 'left' now points to the first element in tails that is >= x.
            // Overwrite it with x (making that subsequence tail smaller and more favorable).
            tails[left] = x;
            
            // If left == size, it means x was larger than ALL elements in tails.
            // We just appended it to the end, meaning our LIS grew by 1.
            if (left == size) {
                size++;
            }
        }
        
        return size;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new LongestIncreasingSubsequence();
        
        // Helper record for clean testing
        record TestCase(int[] nums, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{10, 9, 2, 5, 3, 7, 101, 18}, 4), // [2, 3, 7, 101]
            new TestCase(new int[]{0, 1, 0, 3, 2, 3}, 4),           // [0, 1, 2, 3]
            new TestCase(new int[]{7, 7, 7, 7, 7, 7, 7}, 1),        // [7] (must be strictly increasing)
            new TestCase(new int[]{1}, 1),
            new TestCase(new int[]{4, 10, 4, 3, 8, 9}, 3)           // [4, 8, 9]
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Array   : " + Arrays.toString(tc.nums));
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.lengthOfLISRecursive(tc.nums));
            System.out.println("Memoization       : " + solver.lengthOfLISMemo(tc.nums));
            System.out.println("Tabulation 1D     : " + solver.lengthOfLISTabulation(tc.nums));
            System.out.println("Binary Search     : " + solver.lengthOfLISBinarySearch(tc.nums));
            System.out.println();
        }
    }
}

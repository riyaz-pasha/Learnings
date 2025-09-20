/*
 * Problem Statement:
 * Given an array of ‘N’ positive integers, we need to return the maximum sum of
 * the subsequence such that no two elements of the subsequence are adjacent
 * elements in the array.
 * 
 * Note: A subsequence of an array is a list with elements of the array where
 * some elements are deleted ( or not deleted at all) and the elements should be
 * in the same order in the subsequence as in the array.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class MaxSumOfNonAdjacentElements {

    /*
     * Time Complexity: O(2^n)
     * 
     * Space Complexity: O(N)
     * Reason: We are using a recursion stack space(O(N)) and an array (again O(N)).
     * Therefore total space complexity will be O(N) + O(N) ≈ O(N)
     */
    public int findMaxSum(int[] arr) {
        int n = arr.length;
        return this.findMaxSum(arr, n - 1);
    }

    private int findMaxSum(int[] arr, int index) {
        if (index < 0) {
            return 0;
        }
        int skip = this.findMaxSum(arr, index - 1);
        int pick = arr[index] + this.findMaxSum(arr, index - 2);
        return Math.max(pick, skip);
    }

    public int findMaxSum2(int[] arr) {
        return this.findMaxSum2(arr, 0);
    }

    private int findMaxSum2(int[] arr, int index) {
        if (index >= arr.length) {
            return 0;
        }
        int skip = this.findMaxSum2(arr, index + 1);
        int pick = arr[index] + this.findMaxSum2(arr, index + 2);
        return Math.max(pick, skip);
    }

}

class MaxSumOfNonAdjacentElementsMemo {

    /*
     * Time Complexity: O(N)
     * Reason: The overlapping subproblems will return the answer in constant time
     * O(1). Therefore the total number of new subproblems we solve is ‘n’. Hence
     * total time complexity is O(N).
     * 
     * Space Complexity: O(N)
     * Reason: We are using a recursion stack space(O(N)) and an array (again O(N)).
     * Therefore total space complexity will be O(N) + O(N) ≈ O(N)
     */
    public int findMaxSum(int[] arr) {
        int n = arr.length;
        Integer[] memo = new Integer[n];
        return this.findMaxSum(arr, memo, n - 1);
    }

    private int findMaxSum(int[] arr, Integer[] memo, int index) {
        if (index < 0) {
            return 0;
        }
        if (memo[index] != null) {
            return memo[index];
        }
        int skip = this.findMaxSum(arr, memo, index - 1);
        int pick = arr[index] + this.findMaxSum(arr, memo, index - 2);
        return memo[index] = Math.max(pick, skip);
    }

}

class MaxSumOfNonAdjacentElementsDp {

    /*
     * Time Complexity: O(N)
     * Reason: We are running a simple iterative loop
     * 
     * Space Complexity: O(N)
     * Reason: We are using an external array of size ‘n+1’.
     */
    public int findMaxSum(int[] arr) {
        if (arr == null || arr.length == 0)
            return 0;
        int n = arr.length;
        if (n == 1)
            return arr[0];
        int[] dp = new int[n];
        dp[0] = arr[0];
        dp[1] = Math.max(arr[0], arr[1]);
        for (int index = 2; index < n; index++) {
            int pick = arr[index] + dp[index - 2];
            int skip = dp[index - 1];
            dp[index] = Math.max(pick, skip);
        }
        return dp[n - 1];
    }

}

class MaxSumOfNonAdjacentElementsDpSpaceOptimised {

    /*
     * Time Complexity: O(N)
     * Reason: We are running a simple iterative loop
     * 
     * Space Complexity: O(1)
     * Reason: We are not using any extra space.
     */
    public int findMaxSum(int[] arr) {
        if (arr == null || arr.length == 0)
            return 0;
        if (arr.length == 1)
            return arr[0];

        int excl = 0; // max sum excluding previous
        int incl = arr[0]; // max sum including previous

        for (int i = 1; i < arr.length; i++) {
            int newExcl = Math.max(incl, excl);
            incl = arr[i] + excl;
            excl = newExcl;
        }
        return Math.max(incl, excl);
    }

}

class MaxSumNonAdjacent {

    // Solution 1: Dynamic Programming with O(n) space
    public static int maxSumDP(int[] arr) {
        if (arr == null || arr.length == 0)
            return 0;
        if (arr.length == 1)
            return arr[0];

        int n = arr.length;
        int[] dp = new int[n];

        // Base cases
        dp[0] = arr[0];
        dp[1] = Math.max(arr[0], arr[1]);

        // Fill the dp array
        for (int i = 2; i < n; i++) {
            dp[i] = Math.max(dp[i - 1], dp[i - 2] + arr[i]);
        }

        return dp[n - 1];
    }

    // Solution 2: Space Optimized DP with O(1) space
    public static int maxSumOptimized(int[] arr) {
        if (arr == null || arr.length == 0)
            return 0;
        if (arr.length == 1)
            return arr[0];

        int prev2 = arr[0]; // dp[i-2]
        int prev1 = Math.max(arr[0], arr[1]); // dp[i-1]

        for (int i = 2; i < arr.length; i++) {
            int current = Math.max(prev1, prev2 + arr[i]);
            prev2 = prev1;
            prev1 = current;
        }

        return prev1;
    }

    // Solution 3: Recursive with Memoization
    public static int maxSumRecursive(int[] arr) {
        if (arr == null || arr.length == 0)
            return 0;

        Integer[] memo = new Integer[arr.length];
        return helper(arr, 0, memo);
    }

    private static int helper(int[] arr, int index, Integer[] memo) {
        if (index >= arr.length)
            return 0;

        if (memo[index] != null)
            return memo[index];

        // Two choices: include current element or exclude it
        int include = arr[index] + helper(arr, index + 2, memo);
        int exclude = helper(arr, index + 1, memo);

        memo[index] = Math.max(include, exclude);
        return memo[index];
    }

    // Solution 4: Alternative approach - Track include/exclude states
    public static int maxSumIncludeExclude(int[] arr) {
        if (arr == null || arr.length == 0)
            return 0;

        int include = arr[0]; // Max sum including previous element
        int exclude = 0; // Max sum excluding previous element

        for (int i = 1; i < arr.length; i++) {
            int newExclude = Math.max(include, exclude);
            include = exclude + arr[i];
            exclude = newExclude;
        }

        return Math.max(include, exclude);
    }

    // Helper method to print the actual subsequence (for demonstration)
    public static List<Integer> getMaxSumSubsequence(int[] arr) {
        if (arr == null || arr.length == 0)
            return new ArrayList<>();
        if (arr.length == 1)
            return Arrays.asList(arr[0]);

        int n = arr.length;
        int[] dp = new int[n];

        dp[0] = arr[0];
        dp[1] = Math.max(arr[0], arr[1]);

        for (int i = 2; i < n; i++) {
            dp[i] = Math.max(dp[i - 1], dp[i - 2] + arr[i]);
        }

        // Backtrack to find the actual subsequence
        List<Integer> result = new ArrayList<>();
        int i = n - 1;

        while (i >= 0) {
            if (i == 0 || (i >= 2 && dp[i] == dp[i - 2] + arr[i])) {
                result.add(arr[i]);
                i -= 2;
            } else {
                i--;
            }
        }

        Collections.reverse(result);
        return result;
    }

    // Test methods
    public static void main(String[] args) {
        // Test cases
        int[][] testCases = {
                { 2, 1, 4, 9 }, // Expected: 11 (2 + 9)
                { 5, 5, 10, 100, 10, 5 }, // Expected: 110 (5 + 100 + 5)
                { 1, 2, 3 }, // Expected: 4 (1 + 3)
                { 2, 7, 9, 3, 1 }, // Expected: 12 (2 + 9 + 1)
                { 5 }, // Expected: 5
                { 1, 2 }, // Expected: 2
                { 10, 5, 2, 7, 8 } // Expected: 15 (10 + 7) or (10 + 8)
        };

        System.out.println("Testing Maximum Sum Non-Adjacent Subsequence Solutions:");
        System.out.println("=".repeat(60));

        for (int i = 0; i < testCases.length; i++) {
            int[] arr = testCases[i];
            System.out.println("Test Case " + (i + 1) + ": " + Arrays.toString(arr));

            int result1 = maxSumDP(arr);
            int result2 = maxSumOptimized(arr);
            int result3 = maxSumRecursive(arr);
            int result4 = maxSumIncludeExclude(arr);

            System.out.println("DP Solution: " + result1);
            System.out.println("Optimized DP: " + result2);
            System.out.println("Recursive + Memo: " + result3);
            System.out.println("Include/Exclude: " + result4);
            System.out.println("Subsequence: " + getMaxSumSubsequence(arr));

            // Verify all solutions give same result
            if (result1 == result2 && result2 == result3 && result3 == result4) {
                System.out.println("✓ All solutions match!");
            } else {
                System.out.println("✗ Solutions don't match!");
            }
            System.out.println("-".repeat(40));
        }

        // Performance comparison for large array
        System.out.println("\nPerformance Test (Array size: 100000):");
        int[] largeArr = new int[100000];
        Random rand = new Random(42);
        for (int i = 0; i < largeArr.length; i++) {
            largeArr[i] = rand.nextInt(100) + 1;
        }

        long start = System.nanoTime();
        int result = maxSumOptimized(largeArr);
        long end = System.nanoTime();

        System.out.println("Result: " + result);
        System.out.println("Time taken: " + (end - start) / 1_000_000.0 + " ms");
    }
}

/*
 * Time Complexity Analysis:
 * - Solution 1 (DP): O(n) time, O(n) space
 * - Solution 2 (Optimized): O(n) time, O(1) space
 * - Solution 3 (Recursive + Memo): O(n) time, O(n) space
 * - Solution 4 (Include/Exclude): O(n) time, O(1) space
 * 
 * Space Complexity:
 * - Best: O(1) for solutions 2 and 4
 * - Standard: O(n) for solutions 1 and 3
 * 
 * Algorithm Explanation:
 * For each element at index i, we have two choices:
 * 1. Include arr[i]: Then we can't include arr[i-1], so max sum = arr[i] +
 * dp[i-2]
 * 2. Exclude arr[i]: Then max sum = dp[i-1]
 * 
 * We take the maximum of these two choices: dp[i] = max(dp[i-1], dp[i-2] +
 * arr[i])
 */

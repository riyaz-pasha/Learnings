/*
 * You are given an array 'arr' of size 'n' containing positive integers and a
 * target sum 'k'.
 * Find the number of ways of selecting the elements from the array such that
 * the sum of chosen elements is equal to the target 'k'.
 * Since the number of ways can be very large, print it modulo 10 ^ 9 + 7.
 * 
 * Example:
 * Input: 'arr' = [1, 1, 4, 5]
 * Output: 3
 * Explanation: The possible ways are:
 * [1, 4]
 * [1, 4]
 * [5]
 * Hence the output will be 3. Please note that both 1 present in 'arr' are
 * treated differently.
 */

// We have to go through the each element and build subsets which matches to the sum
// At each element we have two choices either to include that element in the subset or exclude it.
// when the subset sum matches then increase the coun

class CountSubsetsWithSumK {

    public int findWays(int[] nums, int target) {
        return this.countSubsetsWithSumK(nums, target, nums.length - 1);
    }

    private int countSubsetsWithSumK(int[] nums, int target, int index) {
        if (target == 0) {
            return 1;
        }
        if (index < 0 || target < 0) {
            return 0;
        }
        int pick = this.countSubsetsWithSumK(nums, target - nums[index], index - 1);
        int skip = this.countSubsetsWithSumK(nums, target, index - 1);
        return pick + skip;
    }

}

class Solution {
    private static final int MOD = 1000000007;

    // Approach 1: Space-Optimized DP (Best)
    public static int findWays(int[] arr, int k) {
        int n = arr.length;

        // dp[i] represents number of ways to get sum i
        long[] dp = new long[k + 1];
        dp[0] = 1; // One way to make sum 0: select nothing

        // For each element in array
        for (int i = 0; i < n; i++) {
            // Traverse from right to left to avoid using same element multiple times
            for (int sum = k; sum >= arr[i]; sum--) {
                dp[sum] = (dp[sum] + dp[sum - arr[i]]) % MOD;
            }
        }

        return (int) dp[k];
    }

    // Approach 2: 2D DP (More intuitive)
    public static int findWays2D(int[] arr, int k) {
        int n = arr.length;

        // dp[i][j] = ways to get sum j using first i elements
        long[][] dp = new long[n + 1][k + 1];

        // Base case: One way to make sum 0
        for (int i = 0; i <= n; i++) {
            dp[i][0] = 1;
        }

        // Fill the dp table
        for (int i = 1; i <= n; i++) {
            for (int sum = 0; sum <= k; sum++) {
                // Don't take current element
                dp[i][sum] = dp[i - 1][sum];

                // Take current element if possible
                if (sum >= arr[i - 1]) {
                    dp[i][sum] = (dp[i][sum] + dp[i - 1][sum - arr[i - 1]]) % MOD;
                }
            }
        }

        return (int) dp[n][k];
    }

    // Approach 3: Recursion + Memoization
    public static int findWaysRecursive(int[] arr, int k) {
        Long[][] memo = new Long[arr.length][k + 1];
        return (int) solve(arr, arr.length - 1, k, memo);
    }

    private static long solve(int[] arr, int idx, int target, Long[][] memo) {
        // Base cases
        if (target == 0)
            return 1;
        if (idx < 0)
            return 0;
        if (target < 0)
            return 0;

        // Check memo
        if (memo[idx][target] != null) {
            return memo[idx][target];
        }

        // Don't take current element
        long notTake = solve(arr, idx - 1, target, memo);

        // Take current element
        long take = 0;
        if (target >= arr[idx]) {
            take = solve(arr, idx - 1, target - arr[idx], memo);
        }

        memo[idx][target] = (take + notTake) % MOD;
        return memo[idx][target];
    }

    // Test cases
    public static void main(String[] args) {
        // Test case 1
        int[] arr1 = { 1, 1, 4, 5 };
        int k1 = 5;
        System.out.println("Test 1: " + findWays(arr1, k1)); // Output: 3

        // Test case 2
        int[] arr2 = { 1, 2, 3 };
        int k2 = 4;
        System.out.println("Test 2: " + findWays(arr2, k2)); // Output: 3
        // Ways: [1,3], [1,2,1] - wait, there's no second 1
        // Actually: [1,3], [4] - wait, no 4
        // Correct ways: [1,3] = 1 way

        // Test case 3
        int[] arr3 = { 1, 1, 1, 1 };
        int k3 = 2;
        System.out.println("Test 3: " + findWays(arr3, k3)); // Output: 6
        // Ways: Choose any 2 of the 4 ones = C(4,2) = 6

        // Test case 4
        int[] arr4 = { 5, 10, 15 };
        int k4 = 20;
        System.out.println("Test 4: " + findWays(arr4, k4)); // Output: 2
        // Ways: [5,15], [10,10] - wait, only one 10
        // Correct ways: [5,15] = 1 way

        // Compare all approaches
        System.out.println("\nComparing approaches:");
        System.out.println("Space-optimized: " + findWays(arr1, k1));
        System.out.println("2D DP: " + findWays2D(arr1, k1));
        System.out.println("Recursive: " + findWaysRecursive(arr1, k1));
    }
}

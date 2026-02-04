
import java.util.ArrayList;
import java.util.List;

/**
 * Burst Balloons - Interval Dynamic Programming
 *
 * We use the "last balloon burst" strategy.
 */
class BurstBalloons {

    /**
     * Time Complexity:
     * - There are O(n^2) subproblems (intervals)
     * - For each interval, we try O(n) possible last balloons
     * => O(n^3)
     *
     * Space Complexity:
     * - DP table: O(n^2)
     * - Recursion stack: O(n)
     */
    public int maxSum(List<Integer> nums) {

        // Add virtual balloons with value 1 at both ends
        List<Integer> updated = new ArrayList<>();
        updated.add(1); // left boundary
        updated.addAll(nums);
        updated.add(1); // right boundary

        int n = updated.size();

        // dp[left][right] = max coins for interval (left, right)
        Integer[][] dp = new Integer[n][n];

        return maxCoins(updated, dp, 0, n - 1);
    }

    /**
     * Computes max coins for bursting balloons
     * strictly between indices start and end.
     */
    private int maxCoins(List<Integer> nums, Integer[][] dp, int start, int end) {

        // Base case:
        // No balloon to burst between start and end
        if (start + 1 >= end) {
            return 0;
        }

        // Memoization check
        if (dp[start][end] != null) {
            return dp[start][end];
        }

        int max = 0;

        // Try each balloon as the LAST one to burst
        for (int i = start + 1; i < end; i++) {

            // Coins from left subinterval
            int left = maxCoins(nums, dp, start, i);

            // Coins from right subinterval
            int right = maxCoins(nums, dp, i, end);

            // Coins gained by bursting i last
            int coins = nums.get(start) * nums.get(i) * nums.get(end);

            max = Math.max(max, left + right + coins);
        }

        dp[start][end] = max;
        return max;
    }
}

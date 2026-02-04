// Given
// m houses [0,m-1]
// n colors [1,n]
// some houses already painted so paint the remaining houses
// int[] houses
// houses[i]=0 -> not painted
// houses[i]=c -> painted with color c
// int[][] cost
// cost[i][c] -> cost to paint house i with color c
// int target -> number of neighborhoods
// A neighborhood is a maximal contiguous group of houses with the same color.

// Solution

// At every house I have n choices I can paint with any of the colors
// I can paint with color 1,n and increase the neighborhoods count
// If the house is already painted in that case i can only use the select that
// color
// neighborhoods count depends on the previous neighborhoods count and color. if color not
// matching increase neighborhoods count or else maintain same neighborhoods count.
// 

class PaintHouseIII {

    private static final int INF = 1_000_000_000;

    int[] houses;
    int[][] cost;
    int m, n, target;

    // memo[i][prevColor][neighborhoods]
    Integer[][][] memo;

    /**
     * Time Complexity:
     *   O(m * n * target * n)
     *
     * Space Complexity:
     *   O(m * n * target)
     */
    public int minCost(int[] houses, int[][] cost, int m, int n, int target) {
        this.houses = houses;
        this.cost = cost;
        this.m = m;
        this.n = n;
        this.target = target;

        // prevColor ranges from 0..n (0 = no previous color)
        memo = new Integer[m][n + 1][target + 1];

        int ans = dfs(0, 0, 0);
        return ans == INF ? -1 : ans;
    }

    /**
     * dfs(i, prevColor, neighborhoods)
     *
     * i              -> current house index
     * prevColor      -> color of previous house (0 means none)
     * neighborhoods  -> neighborhoods formed so far
     */
    private int dfs(int i, int prevColor, int neighborhoods) {

        // Too many neighborhoods → invalid
        if (neighborhoods > target) {
            return INF;
        }

        // All houses processed
        if (i == m) {
            return (neighborhoods == target) ? 0 : INF;
        }

        if (memo[i][prevColor][neighborhoods] != null) {
            return memo[i][prevColor][neighborhoods];
        }

        int minCost = INF;

        // ----------------------------
        // Case 1: House already painted
        // ----------------------------
        if (houses[i] != 0) {
            int color = houses[i];
            int newNeighborhoods =
                    (color == prevColor) ? neighborhoods : neighborhoods + 1;

            minCost = dfs(i + 1, color, newNeighborhoods);
        }

        // ----------------------------
        // Case 2: House not painted
        // ----------------------------
        else {
            for (int color = 1; color <= n; color++) {

                int newNeighborhoods =
                        (color == prevColor) ? neighborhoods : neighborhoods + 1;

                int nextCost = dfs(i + 1, color, newNeighborhoods);

                if (nextCost != INF) {
                    minCost = Math.min(
                            minCost,
                            cost[i][color - 1] + nextCost
                    );
                }
            }
        }

        memo[i][prevColor][neighborhoods] = minCost;
        return minCost;
    }
}


class PaintHouseIII2 {

    private static final int INF = 1_000_000_000;

    /**
     * Time Complexity: O(m * n * n * target)
     * Space Complexity: O(m * n * target)
     */
    public int minCost(int[] houses, int[][] cost, int m, int n, int target) {

        // dp[i][c][t]:
        // min cost to paint houses[0..i],
        // house i has color c,
        // with exactly t neighborhoods
        int[][][] dp = new int[m][n + 1][target + 1];

        // Initialize all states to INF
        for (int i = 0; i < m; i++) {
            for (int c = 0; c <= n; c++) {
                for (int t = 0; t <= target; t++) {
                    dp[i][c][t] = INF;
                }
            }
        }

        // Base case: house 0
        if (houses[0] != 0) {
            int c = houses[0];
            dp[0][c][1] = 0;
        } else {
            for (int c = 1; c <= n; c++) {
                dp[0][c][1] = cost[0][c - 1];
            }
        }

        // Fill DP table
        for (int i = 1; i < m; i++) {
            for (int c = 1; c <= n; c++) {

                // If house is already painted and color doesn't match
                if (houses[i] != 0 && houses[i] != c)
                    continue;

                int paintCost = (houses[i] == 0) ? cost[i][c - 1] : 0;

                for (int pc = 1; pc <= n; pc++) {
                    for (int t = 1; t <= target; t++) {

                        if (dp[i - 1][pc][t] == INF)
                            continue;

                        if (pc == c) {
                            // Same neighborhood
                            dp[i][c][t] = Math.min(
                                    dp[i][c][t],
                                    dp[i - 1][pc][t] + paintCost);
                        } else if (t + 1 <= target) {
                            // New neighborhood
                            dp[i][c][t + 1] = Math.min(
                                    dp[i][c][t + 1],
                                    dp[i - 1][pc][t] + paintCost);
                        }
                    }
                }
            }
        }

        // Find answer
        int ans = INF;
        for (int c = 1; c <= n; c++) {
            ans = Math.min(ans, dp[m - 1][c][target]);
        }

        return ans == INF ? -1 : ans;
    }

}

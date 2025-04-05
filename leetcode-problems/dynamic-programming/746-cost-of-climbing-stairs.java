class ClimbingStairsCost {

    // O(n) time and space
    public int minCostClimbingStairs(int[] cost) {
        int n = cost.length;
        int[] dp = new int[n + 2];
        dp[n] = 0;
        dp[n + 1] = 0;
        for (int i = n - 1; i >= 0; i--) {
            dp[i] = cost[i] + Math.min(dp[i + 1], dp[i + 2]);
        }
        return Math.min(dp[0], dp[1]);
    }

    // O(n) time and O(1) space
    public int minCostClimbingStairs2(int[] cost) {
        int n = cost.length;
        int oneStep = 0, twoSteps = 0;
        for (int i = n - 1; i >= 0; i--) {
            int minCost = cost[i] + Math.min(oneStep, twoSteps);
            twoSteps = oneStep;
            oneStep = minCost;
        }
        return Math.min(oneStep, twoSteps);
    }
}

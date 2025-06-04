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

    public int minCostClimbingStairs3(int[] cost) {
        int n = cost.length;
        if (n == 0)
            return 0;
        if (n == 1)
            return cost[0];

        int[] dp = new int[n];
        dp[0] = cost[0];
        dp[1] = cost[1];

        for (int i = 2; i < n; i++) {
            dp[i] = cost[i] + Math.min(dp[i - 1], dp[i - 2]);
        }

        return Math.min(dp[n - 1], dp[n - 2]);
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

    public int minCostClimbingStairs4(int[] cost) {
        int n = cost.length;
        int a = cost[0], b = cost[1];

        for (int i = 2; i < n; i++) {
            int c = cost[i] + Math.min(a, b);
            a = b;
            b = c;
        }

        return Math.min(a, b);
    }

}

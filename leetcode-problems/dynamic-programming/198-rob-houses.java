class RobHouses {
    // O(n) time and space
    public int rob2(int[] nums) {
        int n = nums.length;
        int[] dp = new int[n + 2];
        dp[n] = 0;
        dp[n + 1] = 0;
        for (int i = n - 1; i >= 0; i--) {
            dp[i] = Math.max(dp[i + 1], dp[i + 2] + nums[i]);
        }
        return dp[0];
    }

    // O(n) time and O(1) space
    public int rob(int[] nums) {
        int n = nums.length;
        int prev1 = 0;
        int prev2 = 0;
        int temp = 0;
        for (int i = n - 1; i >= 0; i--) {
            temp = Math.max(prev1, prev2 + nums[i]);
            prev2 = prev1;
            prev1 = temp;
        }
        return prev1;
    }

    public int robRecursive(int[] nums) {
        return robRecursive(nums, nums.length - 1);
    }

    private int robRecursive(int[] nums, int i) {
        if (i < 0) return 0;
        return Math.max(nums[i] + robRecursive(nums, i - 2), robRecursive(nums, i - 1));
    }
}

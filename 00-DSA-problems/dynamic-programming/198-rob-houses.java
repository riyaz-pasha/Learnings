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

    public int rob3(int[] nums) {
        int n = nums.length;
        if (n == 0)
            return 0;
        if (n == 1)
            return nums[0];
        if (n == 2)
            return Math.max(nums[0], nums[1]);

        int[] dp = new int[n];
        dp[0] = nums[0];
        dp[1] = Math.max(nums[0], nums[1]);

        for (int i = 2; i < n; i++) {
            dp[i] = Math.max(dp[i - 1], nums[i] + dp[i - 2]);
        }

        return dp[n - 1];
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

    public int rob4(int[] nums) {
        int n = nums.length;
        if (n == 0)
            return 0;
        if (n == 1)
            return nums[0];
        int prev1 = Math.max(nums[0], nums[1]);
        int prev2 = nums[0];

        for (int i = 2; i < n; i++) {
            int curr = Math.max(prev1, nums[i] + prev2);
            prev2 = prev1;
            prev1 = curr;
        }

        return prev1;
    }

    public int robRecursive(int[] nums) {
        return robRecursive(nums, nums.length - 1);
    }

    private int robRecursive(int[] nums, int i) {
        if (i < 0)
            return 0;
        return Math.max(nums[i] + robRecursive(nums, i - 2), robRecursive(nums, i - 1));
    }
}

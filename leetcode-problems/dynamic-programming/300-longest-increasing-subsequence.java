import java.util.Arrays;

class LongestIncreasingSubsequence {
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[][] memo = new int[n][n + 1];
        for (int i = 0; i < n; i++) {
            Arrays.fill(memo[i], -1);
        }
        return helper(nums, 0, -1, memo);
    }

    private int helper(int[] nums, int i, int prevIndex, int[][] memo) {
        if (i == nums.length) {
            return 0;
        }
        if (memo[i][prevIndex + 1] != -1) {
            return memo[i][prevIndex + 1];
        }
        int pick = 0;
        if (prevIndex == -1 || nums[i] > nums[prevIndex]) {
            pick = 1 + helper(nums, i + 1, i, memo);
        }
        int skip = helper(nums, i + 1, prevIndex, memo);
        return memo[i][prevIndex] = Math.max(pick, skip);
    }
}

class LongestIncreasingSubsequence2 {
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[] lens = new int[n];
        Arrays.fill(lens, 1);

        for (int i = n - 2; i >= 0; i--) {
            for (int j = i + 1; j < n; j++) {
                if (nums[i] < nums[j]) {
                    lens[i] = Math.max(lens[i], 1 + lens[j]);
                }
            }
        }
        int max = 0;
        for (int len : lens) {
            max = Math.max(max, len);
        }
        return max;
    }
}

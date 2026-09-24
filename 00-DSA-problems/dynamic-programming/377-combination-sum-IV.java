import java.util.Arrays;

// Given an array of distinct integers nums and a target integer target, 
// return the number of possible combinations that add up to target.

class CombinationSumIV {

    // Time: O(target × nums.length)
    // Space: O(target) for memoization
    public int combinationSum4(int[] nums, int target) {
        int[] dp = new int[target + 1];
        Arrays.sort(nums);

        // There's exactly 1 way to reach target 0 (use no numbers)
        dp[0] = 1;

        for (int i = 1; i <= target; i++) {
            for (int j = 0; j < nums.length && nums[j] <= i; j++) {
                dp[i] += dp[i - nums[j]];
            }
        }
        return dp[target];
    }

    /*
     * We are given nums = [1, 2, 3], and target = 4.
     * 
     * We use a dp array where dp[i] represents the number of combinations to make
     * sum i.
     * Initialize dp[0] = 1 because there's one way to make sum 0 — by choosing
     * nothing.
     * 
     * Start computing from dp[1] to dp[4]:
     * 
     * ------------------------------------------------
     * i = 1:
     * Try num = 1: 1 - 1 = 0 → dp[1] += dp[0] → dp[1] = 1
     * Try num = 2: 1 - 2 < 0 → skip
     * Try num = 3: 1 - 3 < 0 → skip
     * Result: dp[1] = 1
     * 
     * ------------------------------------------------
     * i = 2:
     * Try num = 1: 2 - 1 = 1 → dp[2] += dp[1] = 1 → dp[2] = 1
     * Try num = 2: 2 - 2 = 0 → dp[2] += dp[0] = 1 → dp[2] = 2
     * Try num = 3: 2 - 3 < 0 → skip
     * Result: dp[2] = 2
     * 
     * // Combinations for 2: [1,1], [2]
     * 
     * ------------------------------------------------
     * i = 3:
     * Try num = 1: 3 - 1 = 2 → dp[3] += dp[2] = 2 → dp[3] = 2
     * Try num = 2: 3 - 2 = 1 → dp[3] += dp[1] = 1 → dp[3] = 3
     * Try num = 3: 3 - 3 = 0 → dp[3] += dp[0] = 1 → dp[3] = 4
     * Result: dp[3] = 4
     * 
     * // Combinations for 3: [1,1,1], [1,2], [2,1], [3]
     * 
     * ------------------------------------------------
     * i = 4:
     * Try num = 1: 4 - 1 = 3 → dp[4] += dp[3] = 4 → dp[4] = 4
     * Try num = 2: 4 - 2 = 2 → dp[4] += dp[2] = 2 → dp[4] = 6
     * Try num = 3: 4 - 3 = 1 → dp[4] += dp[1] = 1 → dp[4] = 7
     * Result: dp[4] = 7
     * 
     * // Combinations for 4:
     * // [1,1,1,1], [1,1,2], [1,2,1], [2,1,1], [2,2], [1,3], [3,1]
     * 
     * Final Answer: dp[4] = 7
     */

}

class Solution2 {

    public int combinationSumUnique(int[] nums, int target) {
        return backtrack(nums, target, 0);
    }

    private int backtrack(int[] nums, int remain, int start) {
        if (remain == 0)
            return 1;
        if (remain < 0)
            return 0;

        int count = 0;
        for (int i = start; i < nums.length; i++) {
            count += backtrack(nums, remain - nums[i], i); // reuse allowed
        }
        return count;
    }

}

class Solution3 {

    private int[] dp;
    private int[] nums;

    public int combinationSum4(int[] nums, int target) {
        this.nums = nums;
        this.dp = new int[target + 1];
        Arrays.fill(this.dp, -1);
        this.dp[0] = 1;
        Arrays.sort(this.nums); // Optional optimization
        return solve(target);
    }

    private int solve(int remaining) {
        if (remaining < 0) {
            return 0;
        }
        if (dp[remaining] != -1) {
            return dp[remaining];
        }
        int count = 0;
        for (int num : nums) {
            if (remaining >= num) {
                count += solve(remaining - num);
            } else {
                break; // Optimization due to sorted nums
            }
        }
        dp[remaining] = count;
        return count;
    }

}

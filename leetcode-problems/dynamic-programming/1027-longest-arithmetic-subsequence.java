import java.util.HashMap;
import java.util.Map;

class LongestArithmeticSubsequence {

    // Time: O(n²) because we check all pairs.
    // Space: O(n²) in the worst case, due to DP storage.
    public int longestArithSeqLength(int[] nums) {
        int n = nums.length;
        if (n <= 2)
            return n;

        // dp[i]: Map from difference -> length of arithmetic subsequence ending at i
        Map<Integer, Integer>[] dp = new HashMap[n];
        int maxLen = 2; // Minimum arithmetic subsequence length

        for (int i = 0; i < n; i++) {
            dp[i] = new HashMap<>();

            for (int j = 0; j < i; j++) {
                int diff = nums[i] - nums[j];

                // Length of subsequence with this difference ending at j
                int len = dp[j].getOrDefault(diff, 1);

                // Extend subsequence ending at j by nums[i]
                dp[i].put(diff, len + 1);

                maxLen = Math.max(maxLen, dp[i].get(diff));
            }
        }

        return maxLen;
    }

}

/**
 * LONGEST ARITHMETIC SUBSEQUENCE - ANY DIFFERENCE
 * 
 * Given an array nums of integers, return the length of the longest arithmetic 
 * subsequence in nums.
 * 
 * Unlike the previous problem, here we DON'T know the difference beforehand.
 * We need to consider ALL possible differences between pairs of elements.
 * 
 * ===============================================================================
 * KEY INSIGHT:
 * ===============================================================================
 * For any arithmetic subsequence, we need at least 2 elements to determine the 
 * difference. So we can:
 * 1. Consider every pair of elements (i, j) where i < j
 * 2. The difference = nums[j] - nums[i]
 * 3. Use DP to extend this arithmetic sequence beyond j
 * 
 * DP State: dp[i][diff] = length of longest arithmetic subsequence ending at 
 * index i with difference 'diff'
 * 
 * ===============================================================================
 * ALGORITHM APPROACH:
 * ===============================================================================
 * 1. Use array of HashMaps: dp[i] = Map<difference, length>
 * 2. For each pair (i, j) where i < j:
 *    - Calculate diff = nums[j] - nums[i]
 *    - dp[j][diff] = dp[i][diff] + 1 (or 2 if dp[i][diff] doesn't exist)
 * 3. Track maximum length across all positions and differences
 * 
 * ===============================================================================
 * EXAMPLE TRACE: nums = [3, 6, 9, 12]
 * ===============================================================================
 * 
 * Initial: dp = [{}, {}, {}, {}], maxLen = 2
 * 
 * i=0, j=1: nums[0]=3, nums[1]=6
 * - diff = 6 - 3 = 3
 * - dp[0][3] doesn't exist, so dp[1][3] = 2
 * - dp = [{}, {3: 2}, {}, {}], maxLen = 2
 * 
 * i=0, j=2: nums[0]=3, nums[2]=9
 * - diff = 9 - 3 = 6
 * - dp[0][6] doesn't exist, so dp[2][6] = 2
 * - dp = [{}, {3: 2}, {6: 2}, {}], maxLen = 2
 * 
 * i=1, j=2: nums[1]=6, nums[2]=9
 * - diff = 9 - 6 = 3
 * - dp[1][3] = 2, so dp[2][3] = dp[1][3] + 1 = 3
 * - dp = [{}, {3: 2}, {6: 2, 3: 3}, {}], maxLen = 3
 * 
 * i=0, j=3: nums[0]=3, nums[3]=12
 * - diff = 12 - 3 = 9
 * - dp[0][9] doesn't exist, so dp[3][9] = 2
 * - dp = [{}, {3: 2}, {6: 2, 3: 3}, {9: 2}], maxLen = 3
 * 
 * i=1, j=3: nums[1]=6, nums[3]=12
 * - diff = 12 - 6 = 6
 * - dp[1][6] doesn't exist, so dp[3][6] = 2
 * - dp = [{}, {3: 2}, {6: 2, 3: 3}, {9: 2, 6: 2}], maxLen = 3
 * 
 * i=2, j=3: nums[2]=9, nums[3]=12
 * - diff = 12 - 9 = 3
 * - dp[2][3] = 3, so dp[3][3] = dp[2][3] + 1 = 4
 * - dp = [{}, {3: 2}, {6: 2, 3: 3}, {9: 2, 6: 2, 3: 4}], maxLen = 4
 * 
 * Result: 4 (sequence: [3, 6, 9, 12] with difference = 3)
 * 
 * ===============================================================================
 * EXAMPLE TRACE: nums = [9, 4, 7, 2, 10]
 * ===============================================================================
 * 
 * Let's trace some key pairs:
 * 
 * i=0, j=1: diff = 4-9 = -5, dp[1][-5] = 2
 * i=0, j=2: diff = 7-9 = -2, dp[2][-2] = 2
 * i=1, j=2: diff = 7-4 = 3, dp[2][3] = 2
 * i=1, j=3: diff = 2-4 = -2, dp[3][-2] = 2
 * i=2, j=3: diff = 2-7 = -5, dp[3][-5] = 2
 * 
 * Key insight: i=0,j=2 gives diff=-2, and i=1,j=3 also gives diff=-2
 * But they don't form a sequence: 9->7 (diff=-2), 4->2 (diff=-2)
 * However: i=2,j=4: diff = 10-7 = 3, dp[4][3] = dp[2][3] + 1 = 3
 * This gives sequence: [4, 7, 10] with difference = 3
 * 
 * Maximum length will be 3.
 */

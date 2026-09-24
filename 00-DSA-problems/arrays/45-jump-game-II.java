/*
 * You are given a 0-indexed array of integers nums of length n. You are
 * initially positioned at nums[0].
 * 
 * Each element nums[i] represents the maximum length of a forward jump from
 * index i. In other words, if you are at nums[i], you can jump to any nums[i +
 * j] where:
 * 
 * 0 <= j <= nums[i] and
 * i + j < n
 * Return the minimum number of jumps to reach nums[n - 1]. The test cases are
 * generated such that you can reach nums[n - 1].
 * 
 * Example 1:
 * Input: nums = [2,3,1,1,4]
 * Output: 2
 * Explanation: The minimum number of jumps to reach the last index is 2. Jump 1
 * step from index 0 to 1, then 3 steps to the last index.
 * 
 * Example 2:
 * Input: nums = [2,3,0,1,4]
 * Output: 2
 */

class JumpGameII {

    // Solution 1: Greedy Approach (Most Optimal)
    // Time: O(n), Space: O(1)
    public int jump(int[] nums) {
        if (nums.length <= 1)
            return 0;

        int jumps = 0;
        int currentEnd = 0;
        int farthest = 0;

        // We don't need to check the last element since we're already trying to reach
        // it
        for (int i = 0; i < nums.length - 1; i++) {
            // Update the farthest point we can reach
            farthest = Math.max(farthest, i + nums[i]);

            // If we've reached the end of current jump range
            if (i == currentEnd) {
                jumps++;
                currentEnd = farthest;

                // Early termination if we can already reach the end
                if (currentEnd >= nums.length - 1)
                    break;
            }
        }

        return jumps;
    }

    // Solution 2: BFS Approach
    // Time: O(n), Space: O(1)
    public int jumpBFS(int[] nums) {
        if (nums.length <= 1)
            return 0;

        int jumps = 0;
        int currentLevelEnd = 0;
        int nextLevelEnd = 0;

        for (int i = 0; i < nums.length - 1; i++) {
            nextLevelEnd = Math.max(nextLevelEnd, i + nums[i]);

            if (i == currentLevelEnd) {
                jumps++;
                currentLevelEnd = nextLevelEnd;
            }
        }

        return jumps;
    }

    // Solution 3: Dynamic Programming (Less efficient but intuitive)
    // Time: O(n²), Space: O(n)
    public int jumpDP(int[] nums) {
        int n = nums.length;
        if (n <= 1)
            return 0;

        int[] dp = new int[n];
        // Initialize with maximum values
        for (int i = 1; i < n; i++) {
            dp[i] = Integer.MAX_VALUE;
        }

        for (int i = 0; i < n - 1; i++) {
            if (dp[i] == Integer.MAX_VALUE)
                continue;

            // Try all possible jumps from current position
            for (int j = 1; j <= nums[i] && i + j < n; j++) {
                dp[i + j] = Math.min(dp[i + j], dp[i] + 1);
            }
        }

        return dp[n - 1];
    }

    // Solution 4: Optimized DP
    // Time: O(n), Space: O(n)
    public int jumpOptimizedDP(int[] nums) {
        int n = nums.length;
        if (n <= 1)
            return 0;

        int[] jumps = new int[n];
        jumps[0] = 0;

        for (int i = 1; i < n; i++) {
            jumps[i] = Integer.MAX_VALUE;
            for (int j = 0; j < i; j++) {
                if (j + nums[j] >= i && jumps[j] != Integer.MAX_VALUE) {
                    jumps[i] = Math.min(jumps[i], jumps[j] + 1);
                    break; // Since we're looking for minimum, first valid jump is optimal
                }
            }
        }

        return jumps[n - 1];
    }

    // Test cases
    public static void main(String[] args) {
        JumpGameII solution = new JumpGameII();

        // Test case 1
        int[] nums1 = { 2, 3, 1, 1, 4 };
        System.out.println("Test 1 - Greedy: " + solution.jump(nums1)); // Expected: 2
        System.out.println("Test 1 - BFS: " + solution.jumpBFS(nums1)); // Expected: 2
        System.out.println("Test 1 - DP: " + solution.jumpDP(nums1)); // Expected: 2

        // Test case 2
        int[] nums2 = { 2, 3, 0, 1, 4 };
        System.out.println("Test 2 - Greedy: " + solution.jump(nums2)); // Expected: 2
        System.out.println("Test 2 - BFS: " + solution.jumpBFS(nums2)); // Expected: 2
        System.out.println("Test 2 - DP: " + solution.jumpDP(nums2)); // Expected: 2

        // Test case 3 - Single element
        int[] nums3 = { 0 };
        System.out.println("Test 3 - Greedy: " + solution.jump(nums3)); // Expected: 0

        // Test case 4 - Two elements
        int[] nums4 = { 1, 2 };
        System.out.println("Test 4 - Greedy: " + solution.jump(nums4)); // Expected: 1

        // Test case 5 - Longer array
        int[] nums5 = { 1, 1, 1, 1, 1 };
        System.out.println("Test 5 - Greedy: " + solution.jump(nums5)); // Expected: 4
    }

}

/*
 * Algorithm Explanations:
 * 
 * 1. GREEDY APPROACH (RECOMMENDED):
 * - Think of it as levels in BFS where each level represents positions
 * reachable with same number of jumps
 * - Track the farthest position reachable at current level
 * - When we finish exploring current level, increment jumps and move to next
 * level
 * - Time: O(n), Space: O(1)
 * 
 * 2. BFS APPROACH:
 * - Similar to greedy but more explicit about level boundaries
 * - Each "level" contains all positions reachable with the same number of jumps
 * - Time: O(n), Space: O(1)
 * 
 * 3. DYNAMIC PROGRAMMING:
 * - dp[i] represents minimum jumps to reach position i
 * - For each position, try all possible jumps and update reachable positions
 * - Time: O(n²), Space: O(n)
 * 
 * 4. OPTIMIZED DP:
 * - Same as DP but breaks early when first valid jump is found
 * - Still O(n²) in worst case but performs better in practice
 * 
 * Key Insights:
 * - The greedy approach works because we always want to make jumps that
 * maximize our reach
 * - We don't need to consider all possible paths, just the optimal one at each
 * step
 * - The problem guarantees a solution exists, so we don't need to handle
 * unreachable cases
 */

import java.util.HashMap;
import java.util.Map;

/*
 * You are given an integer array nums and an integer target.
 * 
 * You want to build an expression out of nums by adding one of the symbols '+'
 * and '-' before each integer in nums and then concatenate all the integers.
 * 
 * For example, if nums = [2, 1], you can add a '+' before 2 and a '-' before 1
 * and concatenate them to build the expression "+2-1".
 * Return the number of different expressions that you can build, which
 * evaluates to target.
 * 
 * Example 1:
 * 
 * Input: nums = [1,1,1,1,1], target = 3
 * Output: 5
 * Explanation: There are 5 ways to assign symbols to make the sum of nums be
 * target 3.
 * -1 + 1 + 1 + 1 + 1 = 3
 * +1 - 1 + 1 + 1 + 1 = 3
 * +1 + 1 - 1 + 1 + 1 = 3
 * +1 + 1 + 1 - 1 + 1 = 3
 * +1 + 1 + 1 + 1 - 1 = 3
 * Example 2:
 * 
 * Input: nums = [1], target = 1
 * Output: 1
 */

class TargetSumSolution {

    public int findTargetSumWays(int[] nums, int target) {
        Map<String, Integer> memo = new HashMap<>();
        return countWays(nums, memo, target, nums.length - 1);
    }

    private int countWays(int[] nums, Map<String, Integer> memo, int target, int index) {
        if (index < 0)
            return target == 0 ? 1 : 0;
        String key = index + "-" + target;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }
        int ways = countWays(nums, memo, target - nums[index], index - 1)
                + countWays(nums, memo, target + nums[index], index - 1);
        memo.put(key, ways);
        return ways;
    }

}

class TargetSum {

    // Solution 1: Dynamic Programming with Subset Sum (Most Efficient)
    // Time Complexity: O(n * sum) where sum is total of all nums
    // Space Complexity: O(sum)
    public int findTargetSumWays(int[] nums, int target) {
        int sum = 0;
        for (int num : nums) {
            sum += num;
        }

        // Mathematical insight:
        // Let P = sum of positive numbers, N = sum of negative numbers
        // P - N = target
        // P + N = sum
        // Therefore: P = (target + sum) / 2
        // Problem reduces to: count subsets with sum = (target + sum) / 2

        // Edge cases
        if (target > sum || target < -sum || (target + sum) % 2 != 0) {
            return 0;
        }

        int subsetSum = (target + sum) / 2;
        return countSubsets(nums, subsetSum);
    }

    private int countSubsets(int[] nums, int target) {
        int[] dp = new int[target + 1];
        dp[0] = 1; // One way to make sum 0: select nothing

        for (int num : nums) {
            // Traverse backwards to avoid using same element twice
            for (int j = target; j >= num; j--) {
                dp[j] += dp[j - num];
            }
        }

        return dp[target];
    }

    // Solution 2: 2D Dynamic Programming
    // Time Complexity: O(n * sum) where sum is total of all nums
    // Space Complexity: O(n * sum)
    public int findTargetSumWays2D(int[] nums, int target) {
        int sum = 0;
        for (int num : nums) {
            sum += num;
        }

        if (target > sum || target < -sum || (target + sum) % 2 != 0) {
            return 0;
        }

        int subsetSum = (target + sum) / 2;
        int n = nums.length;

        // dp[i][j] = number of ways to get sum j using first i elements
        int[][] dp = new int[n + 1][subsetSum + 1];
        dp[0][0] = 1;

        for (int i = 1; i <= n; i++) {
            for (int j = 0; j <= subsetSum; j++) {
                // Don't include current number
                dp[i][j] = dp[i - 1][j];

                // Include current number if possible
                if (j >= nums[i - 1]) {
                    dp[i][j] += dp[i - 1][j - nums[i - 1]];
                }
            }
        }

        return dp[n][subsetSum];
    }

    // Solution 3: Backtracking/DFS with Memoization
    // Time Complexity: O(n * sum) with memoization
    // Space Complexity: O(n * sum) for memo + O(n) for recursion stack
    public int findTargetSumWaysBacktrack(int[] nums, int target) {
        return backtrack(nums, 0, 0, target, new java.util.HashMap<>());
    }

    private int backtrack(int[] nums, int index, int current, int target,
            java.util.HashMap<String, Integer> memo) {
        if (index == nums.length) {
            return current == target ? 1 : 0;
        }

        String key = index + "," + current;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        // Try adding positive
        int positive = backtrack(nums, index + 1, current + nums[index], target, memo);

        // Try adding negative
        int negative = backtrack(nums, index + 1, current - nums[index], target, memo);

        int result = positive + negative;
        memo.put(key, result);

        return result;
    }

    // Solution 4: Pure Backtracking (Brute Force - for understanding)
    // Time Complexity: O(2^n) - exponential
    // Space Complexity: O(n) for recursion stack
    public int findTargetSumWaysBruteForce(int[] nums, int target) {
        return bruteForceDFS(nums, 0, 0, target);
    }

    private int bruteForceDFS(int[] nums, int index, int current, int target) {
        if (index == nums.length) {
            return current == target ? 1 : 0;
        }

        int positive = bruteForceDFS(nums, index + 1, current + nums[index], target);
        int negative = bruteForceDFS(nums, index + 1, current - nums[index], target);

        return positive + negative;
    }

    // Solution 5: DP with offset (handles negative indices)
    // Time Complexity: O(n * sum)
    // Space Complexity: O(sum)
    public int findTargetSumWaysOffset(int[] nums, int target) {
        int sum = 0;
        for (int num : nums) {
            sum += num;
        }

        if (target > sum || target < -sum) {
            return 0;
        }

        // Use offset to handle negative sums
        int offset = sum;
        int[] dp = new int[2 * sum + 1];
        dp[offset] = 1; // 0 + offset

        for (int num : nums) {
            int[] next = new int[2 * sum + 1];
            for (int i = 0; i < dp.length; i++) {
                if (dp[i] > 0) {
                    // Add positive
                    if (i + num < next.length) {
                        next[i + num] += dp[i];
                    }
                    // Add negative
                    if (i - num >= 0) {
                        next[i - num] += dp[i];
                    }
                }
            }
            dp = next;
        }

        return dp[target + offset];
    }

    // Test cases
    public static void main(String[] args) {
        TargetSum solution = new TargetSum();

        // Test Case 1
        int[] nums1 = { 1, 1, 1, 1, 1 };
        int target1 = 3;
        System.out.println("Example 1:");
        System.out.println("Input: nums = [1,1,1,1,1], target = 3");
        System.out.println("Output (DP Optimized): " + solution.findTargetSumWays(nums1, target1));
        System.out.println("Output (2D DP): " + solution.findTargetSumWays2D(nums1, target1));
        System.out.println("Output (Backtrack): " + solution.findTargetSumWaysBacktrack(nums1, target1));
        System.out.println("Expected: 5\n");

        // Test Case 2
        int[] nums2 = { 1 };
        int target2 = 1;
        System.out.println("Example 2:");
        System.out.println("Input: nums = [1], target = 1");
        System.out.println("Output: " + solution.findTargetSumWays(nums2, target2));
        System.out.println("Expected: 1\n");

        // Additional Test Cases
        int[] nums3 = { 1, 2, 3, 4, 5 };
        int target3 = 3;
        System.out.println("Additional Test:");
        System.out.println("Input: nums = [1,2,3,4,5], target = 3");
        System.out.println("Output: " + solution.findTargetSumWays(nums3, target3));

        // Edge case: target = 0
        int[] nums4 = { 0, 0, 0, 0, 1 };
        int target4 = 1;
        System.out.println("\nEdge Case (with zeros):");
        System.out.println("Input: nums = [0,0,0,0,1], target = 1");
        System.out.println("Output: " + solution.findTargetSumWays(nums4, target4));

        // Performance comparison
        int[] largNums = new int[20];
        for (int i = 0; i < 20; i++) {
            largNums[i] = 1;
        }

        System.out.println("\nPerformance Test (20 ones, target = 10):");

        long start = System.nanoTime();
        int result1 = solution.findTargetSumWays(largNums, 10);
        long end = System.nanoTime();
        System.out.println("DP Optimized: " + result1 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.findTargetSumWaysBacktrack(largNums, 10);
        end = System.nanoTime();
        System.out.println("Backtracking with Memo: " + result2 + " (Time: " + (end - start) / 1000 + " μs)");
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: DP with Subset Sum Transformation (RECOMMENDED)
 * - Time Complexity: O(n * S)
 * n = length of nums array
 * S = sum of all elements in nums
 * We iterate through n elements and S possible sums
 * - Space Complexity: O(S)
 * Single 1D DP array of size (target sum + 1)
 * 
 * Solution 2: 2D Dynamic Programming
 * - Time Complexity: O(n * S)
 * - Space Complexity: O(n * S)
 * 2D DP table of size n × S
 * 
 * Solution 3: Backtracking with Memoization
 * - Time Complexity: O(n * S)
 * With memoization, each unique state is computed once
 * - Space Complexity: O(n * S) + O(n)
 * HashMap for memoization + recursion stack depth
 * 
 * Solution 4: Brute Force Backtracking
 * - Time Complexity: O(2^n)
 * Exponential - tries all possible combinations
 * - Space Complexity: O(n)
 * Recursion stack depth
 * 
 * Solution 5: DP with Offset
 * - Time Complexity: O(n * S)
 * - Space Complexity: O(S)
 * Handles negative indices by using offset
 * 
 * KEY INSIGHTS:
 * 1. Mathematical Transformation:
 * - Let P = sum of positive numbers, N = sum of negative numbers
 * - P - N = target and P + N = total sum
 * - Solving: P = (target + sum) / 2
 * - Problem becomes: count subsets that sum to P
 * 
 * 2. Edge Cases:
 * - If |target| > sum, impossible to reach
 * - If (target + sum) is odd, impossible (need integer division)
 * - Handle zeros carefully (they contribute multiple ways)
 * 
 * 3. Why Solution 1 is Best:
 * - Optimal time and space complexity
 * - Clean mathematical transformation
 * - Single pass through data with minimal space
 * 
 * 4. Subset Sum Pattern:
 * - dp[j] += dp[j - num] counts ways to form sum j
 * - Traverse backwards to avoid counting same element twice
 */

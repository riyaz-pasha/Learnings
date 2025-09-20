/*
 * You are a professional robber planning to rob houses along a street. Each
 * house has a certain amount of money stashed. All houses at this place are
 * arranged in a circle. That means the first house is the neighbor of the last
 * one. Meanwhile, adjacent houses have a security system connected, and it will
 * automatically contact the police if two adjacent houses were broken into on
 * the same night.
 * 
 * Given an integer array nums representing the amount of money of each house,
 * return the maximum amount of money you can rob tonight without alerting the
 * police.
 * 
 * Example 1:
 * Input: nums = [2,3,2]
 * Output: 3
 * Explanation: You cannot rob house 1 (money = 2) and then rob house 3 (money =
 * 2), because they are adjacent houses.
 * 
 * Example 2:
 * Input: nums = [1,2,3,1]
 * Output: 4
 * Explanation: Rob house 1 (money = 1) and then rob house 3 (money = 3).
 * Total amount you can rob = 1 + 3 = 4.
 * 
 * Example 3:
 * Input: nums = [1,2,3]
 * Output: 3
 */

import java.util.ArrayList;
import java.util.List;

class HouseRobberII {

    /*
     * at each house we have two options
     * 1 we can skip robbing this house. -> then we can rob next house
     * 2. we can rob this house -> then we can not rob next house -> we can only rob
     * next to next house
     * at every house we know what's the max amount we can rob till that house.
     * we just need to return at the last house
     * but in this problem as first and last houses are connected we can not rob
     * both
     * we can consider either of them
     * so we need to divide our space in two ways
     * [first,....,last-1]
     * [first+1,....,last]
     * 
     * Time Complexity: O(N )
     * Reason: We are running a simple iterative loop, two times. Therefore total
     * time complexity will be O(N) + O(N) ≈ O(N)
     * 
     * Space Complexity: O(1)
     * Reason: We are not using extra space.
     */

    public long rob(int[] nums) {
        int n = nums.length;
        if (n == 1) {
            return nums[0];
        }
        List<Integer> withFirstHouse = new ArrayList<>();
        List<Integer> withLastHouse = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i != 0) {
                withLastHouse.add(nums[i]);
            }
            if (i != n - 1) {
                withFirstHouse.add(nums[i]);
            }
        }
        long ans1 = this.rob(withFirstHouse);
        long ans2 = this.rob(withLastHouse);
        return Math.max(ans1, ans2);
    }

    private long rob(List<Integer> nums) {
        Long[] memo = new Long[nums.size()];
        return this.rob(nums, memo, nums.size() - 1);
    }

    private long rob(List<Integer> nums, Long[] memo, int index) {
        if (index < 0) {
            return 0;
        }
        if (memo[index] != null) {
            return memo[index];
        }
        long rob = nums.get(index) + this.rob(nums, memo, index - 2);
        long skip = this.rob(nums, memo, index - 1);
        return memo[index] = Math.max(rob, skip);
    }

}

class HouseRobberCircular {

    // Main solution: Handle circular constraint by considering two scenarios
    public static int rob(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;
        if (nums.length == 1)
            return nums[0];
        if (nums.length == 2)
            return Math.max(nums[0], nums[1]);

        // Two scenarios for circular array:
        // 1. Rob houses 0 to n-2 (exclude last house)
        // 2. Rob houses 1 to n-1 (exclude first house)
        int scenario1 = robLinear(nums, 0, nums.length - 2);
        int scenario2 = robLinear(nums, 1, nums.length - 1);

        return Math.max(scenario1, scenario2);
    }

    // Helper method: Rob houses in linear array (from start to end index)
    private static int robLinear(int[] nums, int start, int end) {
        int prev2 = 0; // dp[i-2]
        int prev1 = 0; // dp[i-1]

        for (int i = start; i <= end; i++) {
            int current = Math.max(prev1, prev2 + nums[i]);
            prev2 = prev1;
            prev1 = current;
        }

        return prev1;
    }

    // Alternative solution with explicit DP array (for better understanding)
    public static int robWithDP(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;
        if (nums.length == 1)
            return nums[0];
        if (nums.length == 2)
            return Math.max(nums[0], nums[1]);

        // Scenario 1: Include house 0, exclude house n-1
        int[] dp1 = new int[nums.length - 1];
        dp1[0] = nums[0];
        dp1[1] = Math.max(nums[0], nums[1]);

        for (int i = 2; i < dp1.length; i++) {
            dp1[i] = Math.max(dp1[i - 1], dp1[i - 2] + nums[i]);
        }

        // Scenario 2: Exclude house 0, include house n-1
        int[] dp2 = new int[nums.length - 1];
        dp2[0] = nums[1];
        if (nums.length > 2) {
            dp2[1] = Math.max(nums[1], nums[2]);
        }

        for (int i = 2; i < dp2.length; i++) {
            dp2[i] = Math.max(dp2[i - 1], dp2[i - 2] + nums[i + 1]);
        }

        return Math.max(dp1[dp1.length - 1], dp2[dp2.length - 1]);
    }

    // Recursive solution with memoization
    public static int robRecursive(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;
        if (nums.length == 1)
            return nums[0];
        if (nums.length == 2)
            return Math.max(nums[0], nums[1]);

        // Two scenarios
        Integer[] memo1 = new Integer[nums.length - 1];
        Integer[] memo2 = new Integer[nums.length - 1];

        int scenario1 = robRecursiveHelper(nums, 0, nums.length - 2, 0, memo1);
        int scenario2 = robRecursiveHelper(nums, 1, nums.length - 1, 0, memo2);

        return Math.max(scenario1, scenario2);
    }

    private static int robRecursiveHelper(int[] nums, int start, int end, int index, Integer[] memo) {
        if (start + index > end)
            return 0;
        if (memo[index] != null)
            return memo[index];

        int include = nums[start + index] + robRecursiveHelper(nums, start, end, index + 2, memo);
        int exclude = robRecursiveHelper(nums, start, end, index + 1, memo);

        memo[index] = Math.max(include, exclude);
        return memo[index];
    }

    // Method to get the actual houses robbed (for demonstration)
    public static List<Integer> getRobbedHouses(int[] nums) {
        if (nums == null || nums.length == 0)
            return new ArrayList<>();
        if (nums.length == 1)
            return Arrays.asList(0);
        if (nums.length == 2) {
            return Arrays.asList(nums[0] > nums[1] ? 0 : 1);
        }

        // Get results for both scenarios
        List<Integer> houses1 = getRobbedHousesLinear(nums, 0, nums.length - 2);
        List<Integer> houses2 = getRobbedHousesLinear(nums, 1, nums.length - 1);

        // Calculate sums
        int sum1 = houses1.stream().mapToInt(i -> nums[i]).sum();
        int sum2 = houses2.stream().mapToInt(i -> nums[i]).sum();

        return sum1 >= sum2 ? houses1 : houses2;
    }

    private static List<Integer> getRobbedHousesLinear(int[] nums, int start, int end) {
        if (start > end)
            return new ArrayList<>();

        int len = end - start + 1;
        int[] dp = new int[len];
        dp[0] = nums[start];
        if (len > 1) {
            dp[1] = Math.max(nums[start], nums[start + 1]);
        }

        for (int i = 2; i < len; i++) {
            dp[i] = Math.max(dp[i - 1], dp[i - 2] + nums[start + i]);
        }

        // Backtrack to find houses
        List<Integer> result = new ArrayList<>();
        int i = len - 1;

        while (i >= 0) {
            if (i == 0 || (i >= 2 && dp[i] == dp[i - 2] + nums[start + i])) {
                result.add(start + i);
                i -= 2;
            } else {
                i--;
            }
        }

        Collections.reverse(result);
        return result;
    }

    public static void main(String[] args) {
        // Test cases
        int[][] testCases = {
                { 2, 3, 2 }, // Expected: 3
                { 1, 2, 3, 1 }, // Expected: 4
                { 1, 2, 3 }, // Expected: 3
                { 5, 1, 3, 9 }, // Expected: 10 (5 + 5 doesn't work in circle)
                { 2, 7, 9, 3, 1 }, // Expected: 11
                { 1 }, // Expected: 1
                { 1, 2 }, // Expected: 2
                { 2, 1, 1, 2 } // Expected: 3
        };

        System.out.println("House Robber II - Circular Array Solutions:");
        System.out.println("=".repeat(50));

        for (int i = 0; i < testCases.length; i++) {
            int[] nums = testCases[i];
            System.out.println("Test Case " + (i + 1) + ": " + Arrays.toString(nums));

            int result1 = rob(nums);
            int result2 = robWithDP(nums);
            int result3 = robRecursive(nums);
            List<Integer> robbedHouses = getRobbedHouses(nums);

            System.out.println("Optimized Solution: " + result1);
            System.out.println("DP Array Solution: " + result2);
            System.out.println("Recursive Solution: " + result3);
            System.out.println("Houses Robbed (indices): " + robbedHouses);

            // Calculate and show the sum
            int sum = robbedHouses.stream().mapToInt(idx -> nums[idx]).sum();
            System.out.println("Money from robbed houses: " + sum);

            // Verify all solutions match
            if (result1 == result2 && result2 == result3) {
                System.out.println("✓ All solutions match!");
            } else {
                System.out.println("✗ Solutions don't match!");
            }
            System.out.println("-".repeat(30));
        }

        // Edge cases
        System.out.println("\nEdge Cases:");
        System.out.println("Empty array: " + rob(new int[] {}));
        System.out.println("Null array: " + rob(null));

        // Performance test
        System.out.println("\nPerformance Test (Array size: 10000):");
        int[] largeArr = new int[10000];
        Random rand = new Random(42);
        for (int i = 0; i < largeArr.length; i++) {
            largeArr[i] = rand.nextInt(100) + 1;
        }

        long start = System.nanoTime();
        int result = rob(largeArr);
        long end = System.nanoTime();

        System.out.println("Result: " + result);
        System.out.println("Time taken: " + (end - start) / 1_000_000.0 + " ms");
    }
}

/*
 * Algorithm Explanation:
 * The key insight is that in a circular array, we cannot rob both the first and
 * last house.
 * So we consider two scenarios:
 * 
 * 1. Rob houses from index 0 to n-2 (exclude last house)
 * 2. Rob houses from index 1 to n-1 (exclude first house)
 * 
 * For each scenario, we solve the linear house robber problem and take the
 * maximum.
 * 
 * Time Complexity: O(n)
 * Space Complexity: O(1) for optimized version, O(n) for DP array version
 * 
 * Key Differences from Linear House Robber:
 * - Must handle the circular constraint
 * - Split into two linear subproblems
 * - Take maximum of both scenarios
 * - Edge cases are more complex due to circular nature
 */
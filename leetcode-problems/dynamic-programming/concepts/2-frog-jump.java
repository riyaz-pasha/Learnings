/*
 * Problem Statement:
 * 
 * Given a number of stairs and a frog, the frog wants to climb from the 0th
 * stair to the (N-1)th stair. At a time the frog can climb either one or two
 * steps. A height[N] array is also given. Whenever the frog jumps from a stair
 * i to stair j, the energy consumed in the jump is abs(height[i]- height[j]),
 * where abs() means the absolute difference. We need to return the minimum
 * energy that can be used by the frog to jump from stair 0 to stair N-1.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class FrogjumpRecursion {

    public int minimumEnergy(int[] heights) {
        return this.minimumEnergy(heights.length - 1, heights);
    }

    private int minimumEnergy(int index, int[] heights) {
        if (index == 0) {
            return 0;
        }
        int jump1 = Math.abs(heights[index] - heights[index - 1]) + this.minimumEnergy(index - 1, heights);
        int jump2 = Integer.MAX_VALUE;
        if (index > 1) {
            jump2 = Math.abs(heights[index] - heights[index - 2]) + this.minimumEnergy(index - 2, heights);
        }
        return Math.min(jump1, jump2);
    }

}

class FrogjumpMemoTopDown {

    /*
     * Time Complexity: O(N)
     * Reason: The overlapping subproblems will return the answer in constant time
     * O(1). Therefore the total number of new subproblems we solve is ‘n’. Hence
     * total time complexity is O(N).
     * 
     * Space Complexity: O(N)
     * Reason: We are using a recursion stack space(O(N)) and an array (again O(N)).
     * Therefore total space complexity will be O(N) + O(N) ≈ O(N)
     */
    public int minimumEnergy(int[] heights) {
        int n = heights.length;
        Integer[] memo = new Integer[n];
        return this.minimumEnergy(n - 1, heights, memo);
    }

    private int minimumEnergy(int index, int[] heights, Integer[] memo) {
        if (index == 0) {
            return 0;
        }
        if (memo[index] != null) {
            return memo[index];
        }
        int jump1 = Math.abs(heights[index] - heights[index - 1]) + this.minimumEnergy(index - 1, heights, memo);
        int jump2 = Integer.MAX_VALUE;
        if (index > 1) {
            jump2 = Math.abs(heights[index] - heights[index - 2]) + this.minimumEnergy(index - 2, heights, memo);
        }
        return memo[index] = Math.min(jump1, jump2);
    }

}

class FrogJumpDP {

    /*
     * Time Complexity: O(N)
     * Reason: We are running a simple iterative loop
     * 
     * Space Complexity: O(N)
     * Reason: We are using an external array of size ‘n+1’.
     */
    public int minimumEnergy(int[] heights) {
        int n = heights.length;
        int[] dp = new int[n];
        dp[0] = 0;
        for (int i = 1; i < n; i++) {
            int jump1 = dp[i - 1] + Math.abs(heights[i] - heights[i - 1]);
            int jump2 = Integer.MAX_VALUE;
            if (i > 1) {
                jump2 = dp[i - 2] + Math.abs(heights[i] - heights[i - 2]);
            }
            dp[i] = Math.min(jump1, jump2);
        }
        return dp[n - 1];
    }

}

class FrogJumpDPSpaceOptimised {

    /*
     * Time Complexity: O(N)
     * Reason: We are running a simple iterative loop
     * 
     * Space Complexity: O(1)
     * Reason: We are not using any extra space.
     */
    public int minimumEnergy(int[] heights) {
        int n = heights.length;
        int[] dp = new int[n];
        dp[0] = 0;
        int prev = 0;
        int prev2 = 0;
        for (int i = 1; i < n; i++) {
            int jump1 = prev + Math.abs(heights[i] - heights[i - 1]);
            int jump2 = Integer.MAX_VALUE;
            if (i > 1) {
                jump2 = prev2 + Math.abs(heights[i] - heights[i - 2]);
            }
            int current = Math.min(jump1, jump2);
            prev2 = prev;
            prev = current;
        }
        return prev;
    }

}

class FrogJump {

    // Solution 1: Recursive (Brute Force) - Exponential Time
    // Time: O(2^n), Space: O(n) for recursion stack
    public int frogJumpRecursive(int[] height) {
        return frogJumpHelper(height, height.length - 1);
    }

    private int frogJumpHelper(int[] height, int index) {
        // Base case: if we're at stair 0, no energy needed
        if (index == 0)
            return 0;

        // Jump from previous stair (index-1)
        int oneStep = frogJumpHelper(height, index - 1) + Math.abs(height[index] - height[index - 1]);

        // Jump from two stairs back (index-2) if possible
        int twoStep = Integer.MAX_VALUE;
        if (index > 1) {
            twoStep = frogJumpHelper(height, index - 2) + Math.abs(height[index] - height[index - 2]);
        }

        return Math.min(oneStep, twoStep);
    }

    // Solution 2: Memoization (Top-Down DP)
    // Time: O(n), Space: O(n)
    public int frogJumpMemo(int[] height) {
        int n = height.length;
        int[] memo = new int[n];
        Arrays.fill(memo, -1);
        return frogJumpMemoHelper(height, n - 1, memo);
    }

    private int frogJumpMemoHelper(int[] height, int index, int[] memo) {
        if (index == 0)
            return 0;

        if (memo[index] != -1)
            return memo[index];

        int oneStep = frogJumpMemoHelper(height, index - 1, memo) +
                Math.abs(height[index] - height[index - 1]);

        int twoStep = Integer.MAX_VALUE;
        if (index > 1) {
            twoStep = frogJumpMemoHelper(height, index - 2, memo) +
                    Math.abs(height[index] - height[index - 2]);
        }

        memo[index] = Math.min(oneStep, twoStep);
        return memo[index];
    }

    // Solution 3: Tabulation (Bottom-Up DP)
    // Time: O(n), Space: O(n)
    public int frogJumpTabulation(int[] height) {
        int n = height.length;
        if (n == 1)
            return 0;

        int[] dp = new int[n];
        dp[0] = 0; // No energy needed to stay at stair 0
        dp[1] = Math.abs(height[1] - height[0]); // Energy to reach stair 1

        for (int i = 2; i < n; i++) {
            // Option 1: Jump from previous stair
            int oneStep = dp[i - 1] + Math.abs(height[i] - height[i - 1]);

            // Option 2: Jump from two stairs back
            int twoStep = dp[i - 2] + Math.abs(height[i] - height[i - 2]);

            dp[i] = Math.min(oneStep, twoStep);
        }

        return dp[n - 1];
    }

    // Solution 4: Space Optimized DP (Most Efficient)
    // Time: O(n), Space: O(1)
    public int frogJumpOptimized(int[] height) {
        int n = height.length;
        if (n == 1)
            return 0;

        int prev2 = 0; // dp[i-2]
        int prev1 = Math.abs(height[1] - height[0]); // dp[i-1]

        if (n == 2)
            return prev1;

        for (int i = 2; i < n; i++) {
            int oneStep = prev1 + Math.abs(height[i] - height[i - 1]);
            int twoStep = prev2 + Math.abs(height[i] - height[i - 2]);

            int current = Math.min(oneStep, twoStep);

            // Update for next iteration
            prev2 = prev1;
            prev1 = current;
        }

        return prev1;
    }

    // Bonus: Solution with path tracking
    public static class Result {
        int minEnergy;
        List<Integer> path;

        Result(int energy, List<Integer> path) {
            this.minEnergy = energy;
            this.path = new ArrayList<>(path);
        }
    }

    public Result frogJumpWithPath(int[] height) {
        int n = height.length;
        if (n == 1)
            return new Result(0, Arrays.asList(0));

        int[] dp = new int[n];
        int[] parent = new int[n]; // To track the path

        dp[0] = 0;
        parent[0] = -1;

        if (n > 1) {
            dp[1] = Math.abs(height[1] - height[0]);
            parent[1] = 0;
        }

        for (int i = 2; i < n; i++) {
            int oneStep = dp[i - 1] + Math.abs(height[i] - height[i - 1]);
            int twoStep = dp[i - 2] + Math.abs(height[i] - height[i - 2]);

            if (oneStep < twoStep) {
                dp[i] = oneStep;
                parent[i] = i - 1;
            } else {
                dp[i] = twoStep;
                parent[i] = i - 2;
            }
        }

        // Reconstruct path
        List<Integer> path = new ArrayList<>();
        int current = n - 1;
        while (current != -1) {
            path.add(current);
            current = parent[current];
        }
        Collections.reverse(path);

        return new Result(dp[n - 1], path);
    }

    // Test all solutions
    public static void main(String[] args) {
        FrogJump solution = new FrogJump();

        // Test case 1
        int[] height1 = { 30, 10, 60, 10, 60, 50 };
        System.out.println("Test Case 1: " + Arrays.toString(height1));
        System.out.println("Recursive: " + solution.frogJumpRecursive(height1));
        System.out.println("Memoization: " + solution.frogJumpMemo(height1));
        System.out.println("Tabulation: " + solution.frogJumpTabulation(height1));
        System.out.println("Optimized: " + solution.frogJumpOptimized(height1));

        Result pathResult1 = solution.frogJumpWithPath(height1);
        System.out.println("With Path - Energy: " + pathResult1.minEnergy +
                ", Path: " + pathResult1.path);
        System.out.println();

        // Test case 2
        int[] height2 = { 10, 20, 30, 10 };
        System.out.println("Test Case 2: " + Arrays.toString(height2));
        System.out.println("Recursive: " + solution.frogJumpRecursive(height2));
        System.out.println("Memoization: " + solution.frogJumpMemo(height2));
        System.out.println("Tabulation: " + solution.frogJumpTabulation(height2));
        System.out.println("Optimized: " + solution.frogJumpOptimized(height2));

        Result pathResult2 = solution.frogJumpWithPath(height2);
        System.out.println("With Path - Energy: " + pathResult2.minEnergy +
                ", Path: " + pathResult2.path);
        System.out.println();

        // Test case 3 - Edge case
        int[] height3 = { 10 };
        System.out.println("Test Case 3 (Single stair): " + Arrays.toString(height3));
        System.out.println("Optimized: " + solution.frogJumpOptimized(height3));

        // Test case 4 - Two stairs
        int[] height4 = { 10, 50 };
        System.out.println("Test Case 4 (Two stairs): " + Arrays.toString(height4));
        System.out.println("Optimized: " + solution.frogJumpOptimized(height4));
    }

    // Helper method to demonstrate the logic step by step
    public void explainSolution(int[] height) {
        int n = height.length;
        System.out.println("Explaining solution for: " + Arrays.toString(height));
        System.out.println("Heights: " + Arrays.toString(height));

        int[] dp = new int[n];
        dp[0] = 0;
        System.out.println("dp[0] = 0 (starting point)");

        if (n > 1) {
            dp[1] = Math.abs(height[1] - height[0]);
            System.out.println("dp[1] = |" + height[1] + " - " + height[0] + "| = " + dp[1]);
        }

        for (int i = 2; i < n; i++) {
            int oneStep = dp[i - 1] + Math.abs(height[i] - height[i - 1]);
            int twoStep = dp[i - 2] + Math.abs(height[i] - height[i - 2]);

            System.out.println("For stair " + i + ":");
            System.out.println("  One step: " + dp[i - 1] + " + |" + height[i] + " - " +
                    height[i - 1] + "| = " + oneStep);
            System.out.println("  Two step: " + dp[i - 2] + " + |" + height[i] + " - " +
                    height[i - 2] + "| = " + twoStep);

            dp[i] = Math.min(oneStep, twoStep);
            System.out.println("  dp[" + i + "] = min(" + oneStep + ", " + twoStep + ") = " + dp[i]);
        }

        System.out.println("Final answer: " + dp[n - 1]);
        System.out.println();
    }
}
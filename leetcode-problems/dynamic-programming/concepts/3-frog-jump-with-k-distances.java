class FrogjumpWithKDistanceMemoTopDown {

    /*
     * Time Complexity: O(N *K)
     * Reason: The overlapping subproblems will return the answer in constant time.
     * Therefore the total number of new subproblems we solve is ‘n’. At every new
     * subproblem, we are running another loop for K times. Hence total time
     * complexity is O(N * K).
     * 
     * Space Complexity: O(N)
     * Reason: We are using a recursion stack space(O(N)) and an array (again O(N)).
     * Therefore total space complexity will be O(N) + O(N) ≈ O(N)
     */
    public int minimumEnergy(int[] heights, int k) {
        int n = heights.length;
        Integer[] memo = new Integer[n];
        return minimumEnergy(memo, heights, k, n - 1);
    }

    private int minimumEnergy(Integer[] memo, int[] heights, int k, int index) {
        if (index == 0) {
            return 0;
        }
        if (memo[index] != null) {
            return memo[index];
        }
        int minValue = Integer.MAX_VALUE;
        for (int i = 1; i <= k && index - i >= 0; i++) {
            minValue = Math.min(
                    minValue,
                    Math.abs(heights[index] - heights[index - i])
                            + this.minimumEnergy(memo, heights, k, index - i));
        }
        return memo[index] = minValue;
    }

}

class FrogjumpWithKDistanceDPBottomUp {

    /*
     * Time Complexity: O(N*K)
     * Reason: We are running two nested loops, where outer loops run from 1 to n-1
     * and the inner loop runs from 1 to K
     * 
     * Space Complexity: O(N)
     * Reason: We are using an external array of size ‘n’’.
     */
    public int minimumEnergy(int[] heights, int k) {
        int n = heights.length;
        int[] dp = new int[n];
        dp[0] = 0;
        for (int index = 1; index < n; index++) {
            int minValue = Integer.MAX_VALUE;

            for (int steps = 1; steps <= k; steps++) {
                minValue = Math.min(
                        minValue,
                        Math.abs(heights[index] - heights[index - steps]) + dp[index - steps]);
            }
            dp[index] = minValue;
        }
        return dp[n - 1];
    }

}

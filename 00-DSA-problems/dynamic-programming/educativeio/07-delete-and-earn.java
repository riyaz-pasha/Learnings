import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Delete and Earn
 * Given an integer array nums, maximize the total points you can earn.
 * Choosing to delete nums[i] earns you nums[i] points, but you MUST also 
 * delete every element equal to nums[i] - 1 and nums[i] + 1.
 * Return the maximum number of points you can earn.
 * 
 * Constraints:
 * 1 <= nums.length <= 2 * 10^4
 * 1 <= nums[i] <= 10^4
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In a senior interview, showing pattern recognition is key. Before coding, ask:
 * 
 * Q: "Are the numbers only positive?"
 * A: Yes, 1 <= nums[i] <= 10^4. We don't have to worry about negative numbers 
 *    ruining our total sum.
 * 
 * Q: "If there are duplicates, say three 2s, and I pick one 2, do the others 
 *     get deleted?"
 * A: No, the problem says you delete nums[i] - 1 and nums[i] + 1. The remaining 
 *    duplicates of 2 are still there to be picked! In fact, picking one 2 means 
 *    you've already destroyed all 1s and 3s. So you might as well pick ALL 2s.
 * 
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "If I pick a number 'x', I should take ALL instances of 'x' to maximize my 
 * points, which will yield `x * count(x)` points. By doing this, I am blocked 
 * from taking ANY 'x - 1' or 'x + 1'.
 * 
 * Does this sound familiar? It is exactly the 'House Robber' problem!
 * Instead of houses on a street, we have 'values' on a number line. 
 * If I 'rob' the value 'x', I cannot 'rob' its adjacent values 'x - 1' and 'x + 1'.
 * 
 * STEP 1: Preprocess the array into a 'points' array where the index is the 
 *         number itself, and the value is the total points we can get from it 
 *         (i.e., index * count_of_index).
 * STEP 2: Apply the House Robber Dynamic Programming logic to this new array."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: nums = [2, 2, 3, 3, 3, 4]
 * 
 * Preprocessing into 'points' array (size = max(nums) + 1 = 5):
 * Index (Value) :  0   1   2   3   4
 * Points        :  0   0   4   9   4
 * (Because there are two 2s: 2*2=4. Three 3s: 3*3=9. One 4: 4*1=4.)
 * 
 * Now, run House Robber on [0, 0, 4, 9, 4]:
 * - Rob 2? We get 4 points. Can't rob 3.
 * - Rob 3? We get 9 points. Can't rob 2 or 4.
 * 
 * Trace DP:
 * max(0) = 0
 * max(1) = 0
 * max(2) = max(max(1), max(0) + 4) = 4
 * max(3) = max(max(2), max(1) + 9) = 9
 * max(4) = max(max(3), max(2) + 4) = max(9, 4+4) = 9
 * 
 * Final Answer: 9 (Rob all 3s, destroying 2s and 4s).
 */
public class DeleteAndEarn {

    /**
     * Helper method to map the input array to our House Robber 'points' array.
     * Time Complexity: O(N) where N is nums.length
     * Space Complexity: O(M) where M is the maximum value in nums.
     */
    private int[] createPointsArray(int[] nums) {
        // Using Java Streams for clean, senior-level syntax to find the max
        int maxVal = Arrays.stream(nums).max().orElse(0);
        
        int[] points = new int[maxVal + 1];
        for (int num : nums) {
            points[num] += num;
        }
        return points;
    }

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: After mapping to the points array, recursively decide whether 
     * to pick the current number or skip it.
     * 
     * Time Complexity: O(2^M) where M is max(nums) - Exponential branching.
     * Space Complexity: O(M) - Call stack depth.
     */
    public int deleteAndEarnRecursive(int[] nums) {
        int[] points = createPointsArray(nums);
        return solveRecursive(points, points.length - 1);
    }

    private int solveRecursive(int[] points, int index) {
        // BASE CASE REASONING:
        // If we have moved backward past the value 0, there are literally no 
        // numbers left on our number line to pick. Therefore, the points 
        // we can earn from non-existent numbers is exactly 0.
        if (index < 0) {
            return 0;
        }
        
        // BASE CASE REASONING:
        // If we are exactly at index 0, this represents the value 0 in our 
        // original array. Picking 0 yields 0 points (0 * count = 0). We return 
        // points[0] to formally complete the logic (which happens to be 0).
        if (index == 0) {
            return points[0];
        }

        // We either skip this value (take max of previous), 
        // or we take this value (points[index]) + max of two values back (index - 2)
        int skip = solveRecursive(points, index - 1);
        int take = solveRecursive(points, index - 2) + points[index];

        return Math.max(skip, take);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the results of the recursive calls so we evaluate each 
     * value on the number line exactly once.
     * 
     * Time Complexity: O(N + M) - N to build points array, M for memoized recursion.
     * Space Complexity: O(M) - Memo array and recursion stack.
     */
    public int deleteAndEarnMemo(int[] nums) {
        int[] points = createPointsArray(nums);
        int[] memo = new int[points.length];
        Arrays.fill(memo, -1);
        
        return solveMemo(points, points.length - 1, memo);
    }

    private int solveMemo(int[] points, int index, int[] memo) {
        // BASE CASE REASONING (same physical logic as brute force):
        // Reached below 0 -> no values exist -> 0 points.
        if (index < 0) return 0;
        // Reached 0 -> only value 0 exists -> points[0] (which is 0).
        if (index == 0) return points[0];

        if (memo[index] != -1) {
            return memo[index];
        }

        int skip = solveMemo(points, index - 1, memo);
        int take = solveMemo(points, index - 2, memo) + points[index];

        memo[index] = Math.max(skip, take);
        return memo[index];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Build an array dp[] where dp[i] represents the max points earned 
     * considering numbers up to 'i'.
     * 
     * Time Complexity: O(N + M)
     * Space Complexity: O(M) for the points array and the dp array.
     */
    public int deleteAndEarnTabulation(int[] nums) {
        int[] points = createPointsArray(nums);
        int m = points.length;
        
        if (m == 1) return points[0]; // If max value was 0 (edge case)

        int[] dp = new int[m];
        
        // BASE CASE REASONING:
        // dp[0] means the max points from evaluating only the number 0. (Result = 0)
        dp[0] = points[0];
        
        // dp[1] means evaluating the numbers 0 and 1. We cannot pick both because 
        // they are adjacent (1 - 1 = 0). So we pick the maximum between them.
        dp[1] = Math.max(points[0], points[1]);

        for (int i = 2; i < m; i++) {
            dp[i] = Math.max(dp[i - 1], dp[i - 2] + points[i]);
        }

        return dp[m - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: Since dp[i] only looks back at dp[i-1] and dp[i-2], we don't need 
     * an entire O(M) dp array. We can reduce it to two variables.
     * Note: We still need the O(M) points array, so total space is still O(M).
     * However, the DP step itself becomes O(1) space, saving memory overhead.
     * 
     * Time Complexity: O(N + M) - O(N) to build points, O(M) to traverse.
     * Space Complexity: O(M) - Purely for the points array preprocessing.
     */
    public int deleteAndEarnSpaceOptimized(int[] nums) {
        int[] points = createPointsArray(nums);
        
        // BASE CASE REASONING:
        // 'twoBack' tracks the maximum points up to value 0.
        // 'oneBack' tracks the maximum points up to value 1 (the max of 0 and 1).
        // If max(nums) is 0, we can safely just return points[0].
        if (points.length == 1) return points[0];
        
        var twoBack = points[0];
        var oneBack = Math.max(points[0], points[1]);

        for (int i = 2; i < points.length; i++) {
            var currentMax = Math.max(oneBack, twoBack + points[i]);
            
            // Shift the sliding window forward
            twoBack = oneBack;
            oneBack = currentMax;
        }

        return oneBack;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new DeleteAndEarn();
        
        List<int[]> testCases = Arrays.asList(
            new int[]{3, 4, 2},             // Expected: 6 (Take 4, destroys 3, take 2)
            new int[]{2, 2, 3, 3, 3, 4},    // Expected: 9 (Take 3s, destroys 2s and 4s)
            new int[]{1},                   // Expected: 1
            new int[]{1, 1, 1, 2, 4, 5, 5, 5, 6}, // Expected: 18 (Take 1s(3), 4(4), 5s(15) -> wait, take 5 destroys 4 and 6. Max: 1s(3) + 4(4) + 6(6) = 13. Or 1s(3) + 5s(15) = 18).
            new int[]{10}                   // Expected: 10
        );
        
        for (int i = 0; i < testCases.size(); i++) {
            int[] nums = testCases.get(i);
            System.out.println("---- Test Case " + (i + 1) + ": " + Arrays.toString(nums) + " ----");
            
            // Limit recursive call printing to small max values to avoid long waits on test cases
            int maxVal = Arrays.stream(nums).max().orElse(0);
            if (maxVal <= 30) {
                System.out.println("Recursive (Brute) : " + solver.deleteAndEarnRecursive(nums));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Max Value Too High)");
            }
            
            System.out.println("Memoization       : " + solver.deleteAndEarnMemo(nums));
            System.out.println("Tabulation        : " + solver.deleteAndEarnTabulation(nums));
            System.out.println("Space Optimized   : " + solver.deleteAndEarnSpaceOptimized(nums));
            System.out.println();
        }
    }
}

import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Target Sum
 * Given an integer array nums and an integer target, assign either '+' or '-' 
 * to each element to build an expression.
 * Return the number of different expressions that evaluate to the target.
 * 
 * Constraints:
 * 1 <= nums.length <= 20
 * 0 <= nums[i] <= 1000
 * 0 <= sum(nums[i]) <= 1000
 * -1000 <= target <= 1000
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, point out the constraints early:
 * 
 * Q: "Can the array contain zeros?"
 * A: Yes (0 <= nums[i]). This is important because +0 and -0 evaluate to the 
 *    same sum but represent TWO distinct assignments! Our algorithm must naturally 
 *    account for this doubling effect.
 * 
 * Q: "What if the target is completely out of bounds?"
 * A: Since the maximum possible sum of all elements is bounded (<= 1000), if 
 *    the absolute value of the target is greater than the total sum of the array, 
 *    it is physically impossible to reach. We should short-circuit and return 0.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At every single number in the array, I am forced to make a choice:
 *  1. Add it (giving it a '+' sign)
 *  2. Subtract it (giving it a '-' sign)
 * 
 * I want to count the TOTAL number of valid paths that lead to my target.
 * Since multiple different paths might arrive at the exact same running sum 
 * at the exact same index (e.g., +1-1 and -1+1 both leave me at sum 0 at index 2), 
 * we have overlapping subproblems. This means Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. SENIOR INSIGHT: THE MATHEMATICAL REDUCTION (For Approaches 3 & 4)
 * ----------------------------------------------------------------------------
 * While we can solve this using standard DP with offsets for negative numbers, 
 * there is a mathematically elegant way to reduce this to a "Subset Sum" problem.
 * 
 * Let P be the subset of numbers given a '+' sign.
 * Let N be the subset of numbers given a '-' sign.
 * 
 * We know two facts:
 * 1. Sum(P) - Sum(N) = target    (This is the problem requirement)
 * 2. Sum(P) + Sum(N) = totalSum  (Every number is in exactly one of the sets)
 * 
 * Let's add the two equations together:
 * 2 * Sum(P) = target + totalSum
 * Sum(P) = (target + totalSum) / 2
 * 
 * BOOM. The problem has entirely changed. We no longer care about '+' or '-'. 
 * We simply need to find the number of subsets in the array that sum up to 
 * exactly `(target + totalSum) / 2`. 
 * If `(target + totalSum)` is odd, it's impossible, return 0.
 */
public class TargetSum {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Traverse the array and physically try both '+' and '-' for each number.
     * Time Complexity: O(2^n) - 2 choices for each of the n elements.
     * Space Complexity: O(n) - Maximum depth of the recursion tree.
     */
    public int findTargetSumWaysRecursive(int[] nums, int target) {
        if (nums == null || nums.length == 0) return 0;
        return solveRecursive(nums, nums.length - 1, target);
    }

    private int solveRecursive(int[] nums, int index, int currentTarget) {
        // BASE CASE REASONING:
        // If we have exhausted all numbers (index < 0), we must check our work.
        // Did our series of '+' and '-' bring our target exactly down to 0?
        // If YES, we found 1 valid way. If NO, this path was a failure (0 ways).
        if (index < 0) {
            return currentTarget == 0 ? 1 : 0;
        }

        // Choice 1: We gave this number a '+' sign. 
        // So we subtract it from our required target.
        int add = solveRecursive(nums, index - 1, currentTarget - nums[index]);
        
        // Choice 2: We gave this number a '-' sign.
        // So we add it back to our required target.
        int subtract = solveRecursive(nums, index - 1, currentTarget + nums[index]);

        // The total ways to reach the target from this state is the sum of ways 
        // from both parallel universes.
        return add + subtract;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache states. But wait, 'currentTarget' can be negative!
     * Since the maximum total sum is 1000, the sum ranges from -1000 to +1000.
     * We can offset our sum by +1000 to map everything to positive indices [0 to 2000].
     * 
     * Time Complexity: O(n * totalSum)
     * Space Complexity: O(n * totalSum) for the memo array.
     */
    public int findTargetSumWaysMemo(int[] nums, int target) {
        int totalSum = Arrays.stream(nums).sum();
        
        // If target is physically impossible to reach even if all signs match
        if (Math.abs(target) > totalSum) return 0;
        
        // Array size is 2001 because sum goes from -1000 to 1000.
        // We shift by 1000, so index 0 represents sum -1000, index 1000 represents sum 0.
        Integer[][] memo = new Integer[nums.length][totalSum * 2 + 1];
        
        return solveMemo(nums, nums.length - 1, target, totalSum, memo);
    }

    private int solveMemo(int[] nums, int index, int currentTarget, int offset, Integer[][] memo) {
        // BASE CASE REASONING (same physical logic as brute force)
        if (index < 0) {
            return currentTarget == 0 ? 1 : 0;
        }

        // Map the current target to a positive array index
        int memoIndex = currentTarget + offset;
        
        if (memo[index][memoIndex] != null) {
            return memo[index][memoIndex];
        }

        int add = solveMemo(nums, index - 1, currentTarget - nums[index], offset, memo);
        int subtract = solveMemo(nums, index - 1, currentTarget + nums[index], offset, memo);

        memo[index][memoIndex] = add + subtract;
        return memo[index][memoIndex];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up DP (Tabulation with Math Reduction)
     * ========================================================================
     * Idea: We use the Mathematical Reduction described in section 3.
     * We just need to find the number of subsets that sum to P.
     * 
     * Time Complexity: O(n * subsetSum)
     * Space Complexity: O(n * subsetSum)
     */
    public int findTargetSumWaysTabulation(int[] nums, int target) {
        int totalSum = 0;
        for (int num : nums) totalSum += num;

        // If target is out of bounds, or if the required subset sum would be a decimal
        if (Math.abs(target) > totalSum || (target + totalSum) % 2 != 0) {
            return 0;
        }

        int subsetTarget = (target + totalSum) / 2;
        int n = nums.length;
        
        // dp[i][j] signifies: "The total number of valid ways to make the sum 'j' 
        // using a combination of the first 'i' items."
        int[][] dp = new int[n + 1][subsetTarget + 1];

        // BASE CASE REASONING:
        // dp[0][0] (Row 0, Col 0): "How many ways can I make a sum of 0 using 0 items?"
        // Exactly 1 way: By picking the empty set. 
        // All other dp[0][j] remain 0, because you can't make a positive sum with 0 items.
        dp[0][0] = 1;

        // Row 'i' represents how many items from the start of the list we are considering.
        for (int i = 1; i <= n; i++) {
            
            // The item we are currently holding in our hands.
            int currentItem = nums[i - 1];

            // Column 'j' represents the exact target sum we are trying to construct right now.
            // Note: We start j from 0 (not 1) because nums[i] can be 0, and we must 
            // process it to count the multiple ways to achieve sum 0.
            for (int j = 0; j <= subsetTarget; j++) {

                // PHYSICAL CHECK: Is the item I'm holding small enough to fit into sum 'j'?
                if (currentItem <= j) {
                    
                    // YES, it fits! I can reach this sum via two different universes:
                    
                    // UNIVERSE 1 (Exclude): I completely ignore this item.
                    // How many ways did I successfully make the sum 'j' using ONLY 
                    // the previous items? (Looking directly up in the spreadsheet).
                    int waysByExcluding = dp[i - 1][j];

                    // UNIVERSE 2 (Include): I force this item into my subset.
                    // This eats up 'currentItem' amount of my target sum.
                    // So I must look to the past: How many ways was I able to make 
                    // the SMALLER remaining sum (j - currentItem)?
                    int waysByIncluding = dp[i - 1][j - currentItem];

                    // Because we want the TOTAL number of ways, we add both universes together.
                    dp[i][j] = waysByExcluding + waysByIncluding;
                    
                } else {
                    // NO, it doesn't fit. The item is strictly larger than our target sum 'j'.
                    // Our ONLY option is to exclude it. The number of ways to make sum 'j' 
                    // is strictly equal to however many ways we could do it previously.
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        return dp[n][subsetTarget];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, row 'i' only ever looks at row 'i-1'. 
     * We condense the 2D spreadsheet into a single 1D array representing just 
     * one row of targets. We loop backwards to ensure we only use each item once.
     * 
     * Time Complexity: O(n * subsetSum)
     * Space Complexity: O(subsetSum) - Massively reduced memory footprint.
     */
    public int findTargetSumWaysSpaceOptimized(int[] nums, int target) {
        int totalSum = 0;
        for (int num : nums) totalSum += num;

        if (Math.abs(target) > totalSum || (target + totalSum) % 2 != 0) {
            return 0;
        }

        int subsetTarget = (target + totalSum) / 2;
        int[] dp = new int[subsetTarget + 1];

        // BASE CASE REASONING:
        // 1 way to make a sum of 0 (using the empty set).
        dp[0] = 1;

        for (int i = 0; i < nums.length; i++) {
            int currentItem = nums[i];

            // Traverse BACKWARDS from our max target down to our item's value.
            // Why backwards? Because if we went forwards, we might add 'currentItem' 
            // to a smaller sum, update the array, and then add 'currentItem' AGAIN 
            // to that new sum later in the loop. Going backwards ensures we only 
            // read clean states from the "previous row".
            for (int j = subsetTarget; j >= currentItem; j--) {
                
                // The new total ways to make sum 'j' is:
                // (The ways we could already make 'j' WITHOUT this item) 
                // PLUS 
                // (The ways we could make the smaller sum 'j - currentItem')
                dp[j] = dp[j] + dp[j - currentItem];
                
            }
        }

        return dp[subsetTarget];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new TargetSum();
        
        record TestCase(int[] nums, int target, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{1, 1, 1, 1, 1}, 3, 5),   // 5 different ways
            new TestCase(new int[]{1}, 1, 1),               // 1 way (+1)
            new TestCase(new int[]{1}, 2, 0),               // Impossible target
            new TestCase(new int[]{1, 2, 3}, 8, 0),         // Target larger than total sum
            new TestCase(new int[]{0, 0, 0, 0, 0}, 0, 32)   // Zeros create 2^5 = 32 valid combinations
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Array   : " + Arrays.toString(tc.nums));
            System.out.println("Target  : " + tc.target);
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.findTargetSumWaysRecursive(tc.nums, tc.target));
            System.out.println("Memoization       : " + solver.findTargetSumWaysMemo(tc.nums, tc.target));
            System.out.println("Tabulation        : " + solver.findTargetSumWaysTabulation(tc.nums, tc.target));
            System.out.println("Space Optimized   : " + solver.findTargetSumWaysSpaceOptimized(tc.nums, tc.target));
            System.out.println();
        }
    }
}

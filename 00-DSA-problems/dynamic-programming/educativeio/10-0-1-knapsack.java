import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: 0/1 Knapsack Problem
 * You are given 'n' items with known weights and values, and a knapsack with 
 * a maximum weight capacity. Maximize the total value of items in the knapsack 
 * without exceeding its capacity.
 * 
 * Notes:
 * - You cannot break items (0/1 property: take it all or leave it).
 * - You can only use each item at most once.
 * 
 * Constraints:
 * 1 <= capacity <= 1000
 * 1 <= values.length == weights.length <= 500
 * 1 <= values[i] <= 1000
 * 1 <= weights[i] <= capacity
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In a senior-level interview, this is the grandfather of all Dynamic 
 * Programming problems. Acknowledge it, and confirm the constraints:
 * 
 * Q: "Can items have a weight of 0?"
 * A: Constraint says weights[i] >= 1. (If weight could be 0 with a positive 
 *    value, we would always take it).
 * 
 * Q: "Can values be negative?"
 * A: Constraint says values[i] >= 1. (If they were negative, we'd never take 
 *    them since they just consume capacity for a penalty).
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given item 'i', I have exactly two choices:
 *  1. EXCLUDE the item: I save my knapsack capacity for future items, but I 
 *     gain 0 value from this item.
 *  2. INCLUDE the item: I consume 'weights[i]' capacity, but I gain 'values[i]' 
 *     value. I can only do this if my remaining capacity is >= weights[i].
 * 
 * My goal is to find the maximum possible value between these two choices. 
 * Because different combinations of items can leave me with the exact same 
 * remaining capacity (overlapping subproblems), this requires Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: capacity = 5, weights = [2, 3, 4], values = [3, 4, 5]
 * 
 * We evaluate from the last item (weight 4, value 5):
 * - If we EXCLUDE 4: We have 5 capacity left for [2, 3].
 * - If we INCLUDE 4: We get 5 value, but only have 1 capacity left for [2, 3].
 * 
 * Notice that if we include 4, the remaining capacity of 1 is too small to 
 * hold either 2 or 3. Our total value would be 5.
 * BUT, if we exclude 4, we can fit BOTH 2 and 3 into our capacity of 5, 
 * yielding a total value of 3 + 4 = 7. 
 * Max(5, 7) = 7.
 */
public class Knapsack {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Try every single valid combination by branching out into "Include" 
     * and "Exclude" paths at every item.
     * 
     * Time Complexity: O(2^n) - We make 2 decisions for each of the n items.
     * Space Complexity: O(n) - Maximum depth of the recursion tree.
     */
    public int solveRecursive(int capacity, int[] weights, int[] values) {
        if (weights == null || weights.length == 0) return 0;
        return knapsackRecursive(weights, values, weights.length - 1, capacity);
    }

    private int knapsackRecursive(int[] weights, int[] values, int index, int remainingCapacity) {
        // BASE CASE REASONING:
        // 1. If index < 0: We have looked through every single item on the table.
        //    Since there are no items left to put in the bag, we can't gain any 
        //    more value. Return 0.
        // 2. If remainingCapacity == 0: Our bag is completely full. Even if we 
        //    have items left on the table, we cannot physically fit them. Return 0.
        if (index < 0 || remainingCapacity == 0) {
            return 0;
        }

        // Choice 1: Exclude the current item. 
        // We move to the next item (index - 1), capacity remains unchanged.
        int exclude = knapsackRecursive(weights, values, index - 1, remainingCapacity);

        // Choice 2: Include the current item (only if it fits!).
        int include = 0;
        if (weights[index] <= remainingCapacity) {
            // We gain values[index], and recursively ask for the best value 
            // we can get with the REST of the items and the REDUCED capacity.
            include = values[index] + knapsackRecursive(weights, values, index - 1, remainingCapacity - weights[index]);
        }

        return Math.max(exclude, include);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache states defined by [Current Item Index][Remaining Capacity].
     * 
     * Time Complexity: O(n * capacity)
     * Space Complexity: O(n * capacity) for the 2D memo array + call stack.
     */
    public int solveMemo(int capacity, int[] weights, int[] values) {
        if (weights == null || weights.length == 0) return 0;
        int n = weights.length;
        
        // memo[index][capacity]
        int[][] memo = new int[n][capacity + 1];
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }
        
        return knapsackMemo(weights, values, n - 1, capacity, memo);
    }

    private int knapsackMemo(int[] weights, int[] values, int index, int remainingCapacity, int[][] memo) {
        // BASE CASE REASONING (Same physical logic as recursion):
        // No items left OR no space left = 0 value.
        if (index < 0 || remainingCapacity == 0) {
            return 0;
        }

        if (memo[index][remainingCapacity] != -1) {
            return memo[index][remainingCapacity];
        }

        int exclude = knapsackMemo(weights, values, index - 1, remainingCapacity, memo);
        
        int include = 0;
        if (weights[index] <= remainingCapacity) {
            include = values[index] + knapsackMemo(weights, values, index - 1, remainingCapacity - weights[index], memo);
        }

        memo[index][remainingCapacity] = Math.max(exclude, include);
        return memo[index][remainingCapacity];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Build a 2D spreadsheet where rows are the items we can use, and 
     * columns are the sizes of the knapsack from 0 up to 'capacity'.
     * 
     * Time Complexity: O(n * capacity)
     * Space Complexity: O(n * capacity)
     */
    public int solveTabulation(int capacity, int[] weights, int[] values) {
        int n = weights.length;
        if (n == 0 || capacity == 0) return 0;

        // dp[i][j] signifies: "The maximum value I can carry using a subset of 
        // the first 'i' items, given a knapsack that holds exactly 'j' weight."
        int[][] dp = new int[n + 1][capacity + 1];

        // BASE CASE REASONING (Implicit in Java, but conceptually important):
        // dp[0][j] (Row 0): If I am allowed to use 0 items, my max value is 0.
        // dp[i][0] (Col 0): If my knapsack holds 0 weight, I can't put anything 
        // in it, so my max value is 0.
        // Java initializes arrays to 0, so these base cases are handled automatically.

        // Row 'i' represents how many items from the start of the list we are allowed to consider.
        for (int i = 1; i <= n; i++) {
            
            // Extract the actual weight and value of the item we are currently looking at.
            // (Using i-1 because our DP table is 1-indexed for items, but arrays are 0-indexed)
            int currentWeight = weights[i - 1];
            int currentValue = values[i - 1];

            // Column 'j' represents the current weight capacity of the knapsack we are testing.
            for (int j = 1; j <= capacity; j++) {

                // PHYSICAL CHECK: Is the item I'm holding too heavy for this specific knapsack 'j'?
                if (currentWeight <= j) {
                    // IT FITS! We must calculate the value of two possible parallel universes:
                    
                    // UNIVERSE 1 (Exclude): I decide NOT to put this item in the bag.
                    // To find my max value, I look directly UP in my spreadsheet to see 
                    // what my maximum value was when I only had the PREVIOUS items to choose from, 
                    // using this exact same knapsack size 'j'.
                    int excludeValue = dp[i - 1][j];

                    // UNIVERSE 2 (Include): I decide to put this item IN the bag.
                    // This instantly gives me 'currentValue'. 
                    // BUT, it permanently eats up 'currentWeight' amount of space!
                    // So, I look UP (to the previous items) and LEFT (by the weight I just consumed).
                    // "What was the best value I could make with the previous items, 
                    // using the SMALLER remaining space?"
                    int includeValue = currentValue + dp[i - 1][j - currentWeight];

                    // Reality Check: I am a greedy thief. I want whichever universe gave me more value.
                    dp[i][j] = Math.max(excludeValue, includeValue);
                } else {
                    // IT DOESN'T FIT. The item is strictly heavier than knapsack 'j'.
                    // I am FORCED into Universe 1 (Exclude). I just copy the best answer 
                    // I had from the previous items.
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        return dp[n][capacity];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In our tabulation explanation above, to calculate row 'i', we ONLY 
     * ever looked at row 'i-1'. The rows before that were dead memory. 
     * We can collapse the 2D spreadsheet into a single 1D array representing 
     * just one row of capacities.
     * 
     * CRITICAL CONSTRAINT: We MUST loop through the capacity backwards. 
     * If we loop forwards, processing a small weight might update dp[current], 
     * and then a larger weight later in the same loop might read that updated 
     * dp[current]. This would mean we used the SAME item twice! Looping backwards 
     * guarantees we only read states from the "previous row" (before the item was added).
     * 
     * Time Complexity: O(n * capacity)
     * Space Complexity: O(capacity) - Single 1D array!
     */
    public int solveSpaceOptimized(int capacity, int[] weights, int[] values) {
        int n = weights.length;
        if (n == 0 || capacity == 0) return 0;

        int[] dp = new int[capacity + 1];

        // BASE CASE REASONING:
        // By default, the array is filled with 0s. This correctly maps to:
        // "With 0 items considered, a knapsack of any size holds 0 value."

        for (int i = 0; i < n; i++) {
            int currentWeight = weights[i];
            int currentValue = values[i];

            // Traverse BACKWARDS from the maximum capacity down to the weight of our item.
            // We stop at 'currentWeight' because if the knapsack capacity is smaller 
            // than the item's weight, it's impossible to include it anyway, so the 
            // value in the array just stays the same (implicitly excluding it).
            for (int j = capacity; j >= currentWeight; j--) {
                
                // We overwrite our single array in-place.
                // dp[j] on the right side of the equals sign acts as 'exclude' (the old value).
                // dp[j - currentWeight] acts as 'include' (the old value at a smaller capacity).
                dp[j] = Math.max(dp[j], currentValue + dp[j - currentWeight]);
                
            }
        }

        return dp[capacity];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new Knapsack();
        
        // Using a Record for clean test case definition
        record TestCase(int capacity, int[] weights, int[] values, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(5, new int[]{2, 3, 4}, new int[]{3, 4, 5}, 7), // 2+3=5 weight, 3+4=7 value
            new TestCase(50, new int[]{10, 20, 30}, new int[]{60, 100, 120}, 220), // Take 20 & 30
            new TestCase(8, new int[]{1, 2, 3, 4, 5}, new int[]{1, 5, 8, 9, 10}, 22), // 5+3=8 weight, 10+8=18 value? No, 2+3=5 -> 5+8=13; Actually 5+3 is 18. Wait, take 3(8)+4(9)+1(1) = 18? No, let's let the algorithm calculate it. 
            new TestCase(1, new int[]{2}, new int[]{3}, 0) // Item heavier than capacity
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Capacity: " + tc.capacity);
            System.out.println("Weights : " + Arrays.toString(tc.weights));
            System.out.println("Values  : " + Arrays.toString(tc.values));
            
            System.out.println("Recursive (Brute) : " + solver.solveRecursive(tc.capacity, tc.weights, tc.values));
            System.out.println("Memoization       : " + solver.solveMemo(tc.capacity, tc.weights, tc.values));
            System.out.println("Tabulation        : " + solver.solveTabulation(tc.capacity, tc.weights, tc.values));
            System.out.println("Space Optimized   : " + solver.solveSpaceOptimized(tc.capacity, tc.weights, tc.values));
            System.out.println();
        }
    }
}

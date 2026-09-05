import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Dungeon Game
 * A knight starts at the top-left corner (0,0) and must rescue a princess at 
 * the bottom-right corner (m-1,n-1). 
 * He can only move RIGHT or DOWN.
 * Cells contain demons (negative values) or magic orbs (positive values).
 * The knight's health must NEVER drop to 0 or below.
 * Find the MINIMUM initial health the knight needs to start with.
 * 
 * Constraints:
 * 1 <= m, n <= 200
 * -1000 <= dungeon[i][j] <= 1000
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * This is a notorious "trap" problem in L4/L5 interviews. Most candidates 
 * instantly try to solve it starting from the top-left and moving forward.
 * 
 * Q: "Can I just track my minimum health drop as I walk forward from (0,0)?"
 * A: No! This breaks the 'Optimal Substructure' rule of Dynamic Programming. 
 *    If you walk forward, you have to track TWO states: 
 *    1. Your current accumulated health.
 *    2. The minimum health you dropped to along the way.
 *    A path that drops you to 1 HP but leaves you with 500 HP might be better 
 *    or worse than a path that drops you to 10 HP but leaves you with 12 HP, 
 *    entirely depending on the demons ahead of you. You cannot safely discard 
 *    subproblems.
 * 
 * CRITICAL SENIOR INSIGHT: THE REVERSE DP
 * "Because our survival depends entirely on the obstacles AHEAD of us, we must 
 * evaluate this problem in REVERSE. We start at the Princess (m-1, n-1) and 
 * ask: 'What health do I need BEFORE stepping into this room to survive it?' 
 * We work our way backward to the start."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given cell (row, col), looking backwards from the end, I need to know 
 * the minimum health required to exit this room and survive the rest of the journey. 
 * Since I can only move DOWN or RIGHT, I look at the cell below me and the cell 
 * to my right, and I greedily pick the one that requires LESS health.
 * 
 * Let this chosen exit health be 'minHealthOnExit'.
 * To survive the current room, my health BEFORE entering must be at least:
 * `minHealthOnExit - dungeon[row][col]`.
 * 
 * If the room has a demon (-50), it INCREASES the health I need.
 * If the room has an orb (+50), it DECREASES the health I need.
 * However, my health can NEVER be 0 or negative. So if a massive orb drops my 
 * required entry health to -10, I still MUST have at least 1 HP to enter it alive.
 * 
 * Equation: Math.max(1, minHealthOnExit - dungeon[row][col])"
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: 
 * Dungeon = 
 * [-2(A), -3(B),  3(C)]
 * [-5(D), -10(E), 1(F)]
 * [10(G),  30(H), -5(I)]  <-- Princess at I (-5)
 * 
 * Trace backward from Princess (I):
 * To survive I (-5), we must exit alive (need 1 HP).
 * So before entering I, we need: max(1, 1 - (-5)) = 6 HP.
 * 
 * At H (30), we can only move RIGHT to I (needs 6).
 * So before entering H, we need: max(1, 6 - 30) = 1 HP.
 * 
 * At F (1), we can only move DOWN to I (needs 6).
 * So before entering F, we need: max(1, 6 - 1) = 5 HP.
 * 
 * At E (-10), we can move RIGHT to F (needs 5) or DOWN to H (needs 1).
 * We greedily choose DOWN (1 is less than 5).
 * Before entering E, we need: max(1, 1 - (-10)) = 11 HP.
 */
public class DungeonGame {

    // A safe infinity value to prevent integer overflow when doing Math.min
    private static final int INF = Integer.MAX_VALUE / 2;

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force - Reverse)
     * ========================================================================
     * Idea: Write a recursive function that returns the minimum initial health 
     * required at (row, col) to reach the bottom-right corner safely.
     * 
     * Time Complexity: O(2^(m+n)) - Exponential branching.
     * Space Complexity: O(m+n) - Maximum depth of the recursion tree.
     */
    public int calculateMinimumHPRecursive(int[][] dungeon) {
        if (dungeon == null || dungeon.length == 0) return 1;
        return solveRecursive(dungeon, 0, 0);
    }

    private int solveRecursive(int[][] dungeon, int row, int col) {
        int m = dungeon.length;
        int n = dungeon[0].length;

        // BASE CASE REASONING (Out of Bounds):
        // If we step outside the dungeon walls (right or down), this is an 
        // illegal path. We return "Infinity" so our Math.min() logic in the 
        // valid cells will completely ignore this direction.
        if (row >= m || col >= n) {
            return INF;
        }

        // BASE CASE REASONING (The Princess Room):
        // If we are EXACTLY in the princess room, we have reached the end. 
        // We require 1 HP to stay alive after dealing with whatever is in this room.
        // So the health needed BEFORE entering this room is:
        if (row == m - 1 && col == n - 1) {
            return Math.max(1, 1 - dungeon[row][col]);
        }

        // Parallel Universe 1: We decide to step DOWN. How much health do we need?
        int healthIfGoingDown = solveRecursive(dungeon, row + 1, col);

        // Parallel Universe 2: We decide to step RIGHT. How much health do we need?
        int healthIfGoingRight = solveRecursive(dungeon, row, col + 1);

        // We are the knight. We greedily pick the path that demands LESS health from us.
        int minHealthOnExit = Math.min(healthIfGoingDown, healthIfGoingRight);

        // Calculate health needed before entering the CURRENT room
        return Math.max(1, minHealthOnExit - dungeon[row][col]);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the minimum health needed at each (row, col) so we don't 
     * re-evaluate the same sub-grids multiple times.
     * 
     * Time Complexity: O(m * n) - We evaluate each cell exactly once.
     * Space Complexity: O(m * n) - For the 2D memo array + call stack.
     */
    public int calculateMinimumHPMemo(int[][] dungeon) {
        if (dungeon == null || dungeon.length == 0) return 1;
        
        int m = dungeon.length;
        int n = dungeon[0].length;
        int[][] memo = new int[m][n];
        for (int[] r : memo) Arrays.fill(r, -1);
        
        return solveMemo(dungeon, 0, 0, memo);
    }

    private int solveMemo(int[][] dungeon, int row, int col, int[][] memo) {
        int m = dungeon.length;
        int n = dungeon[0].length;

        // BASE CASES (Same physical logic as brute force)
        if (row >= m || col >= n) return INF;
        if (row == m - 1 && col == n - 1) return Math.max(1, 1 - dungeon[row][col]);

        if (memo[row][col] != -1) {
            return memo[row][col];
        }

        int down = solveMemo(dungeon, row + 1, col, memo);
        int right = solveMemo(dungeon, row, col + 1, memo);

        int minExitHealth = Math.min(down, right);
        memo[row][col] = Math.max(1, minExitHealth - dungeon[row][col]);
        
        return memo[row][col];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build a spreadsheet from the bottom-right back to the top-left.
     * dp[i][j] signifies: "The absolute minimum HP needed BEFORE entering cell (i, j)".
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     */
    public int calculateMinimumHPTabulation(int[][] dungeon) {
        int m = dungeon.length;
        int n = dungeon[0].length;
        
        // We make the DP table slightly larger (m+1 x n+1) to act as a physical 
        // "wall" around the bottom and right edges of the dungeon.
        int[][] dp = new int[m + 1][n + 1];

        // BASE CASE REASONING (The Outer Walls):
        // If the knight steps out of bounds, he dies. So we fill the invisible 
        // boundary walls with "Infinity" to ensure they are never chosen as a valid path.
        for (int i = 0; i <= m; i++) Arrays.fill(dp[i], INF);
        
        // BASE CASE REASONING (The Magical Rescue Condition):
        // We need exactly 1 HP to be alive AFTER rescuing the princess. 
        // We simulate this by placing a "dummy" exit requirement of 1 HP immediately 
        // to the right and immediately below the princess cell.
        dp[m][n - 1] = 1;
        dp[m - 1][n] = 1;

        // Outer loop: We walk BACKWARDS from the bottom row up to the top row.
        for (int i = m - 1; i >= 0; i--) {
            
            // Inner loop: We walk BACKWARDS from the rightmost column to the left.
            for (int j = n - 1; j >= 0; j--) {
                
                // --- DETAILED TABULATION EXPLANATION ---
                // We are currently standing OUTSIDE room (i, j), trying to figure out 
                // how much health we need to survive it.
                
                // UNIVERSE 1: What if we plan to exit this room by moving DOWN?
                // We look DIRECTLY DOWN in our spreadsheet (dp[i + 1][j]).
                // This tells us the health required to survive the REST of the journey that way.
                int healthIfGoingDown = dp[i + 1][j];
                
                // UNIVERSE 2: What if we plan to exit this room by moving RIGHT?
                // We look DIRECTLY RIGHT in our spreadsheet (dp[i][j + 1]).
                int healthIfGoingRight = dp[i][j + 1];
                
                // We are smart. We look at both doors, and we plan to exit through 
                // the one that requires the LEAST amount of health.
                int minHealthOnExit = Math.min(healthIfGoingDown, healthIfGoingRight);
                
                // Now we factor in the monster or magic orb inside the CURRENT room.
                // If it's a demon (-30), we need MORE health to survive (minHealthOnExit - (-30) = +30).
                // If it's an orb (+30), we need LESS health, it heals us (minHealthOnExit - 30).
                int healthNeededBeforeEntering = minHealthOnExit - dungeon[i][j];
                
                // PHYSICAL LIMIT CHECK:
                // Can our required entering health ever be 0 or negative? NO!
                // Even if there is a massive healing orb (+1000) inside, we cannot 
                // walk into the room with 0 health, because we would be dead before 
                // we could even touch the orb. We ALWAYS enforce a minimum of 1 HP.
                dp[i][j] = Math.max(1, healthNeededBeforeEntering);
            }
        }

        // The answer sits in the very first cell we evaluated in reality.
        return dp[0][0];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, notice that to calculate row 'i', we ONLY look at:
     * - The cell to the RIGHT (current row 'i', evaluated just moments ago).
     * - The cell BELOW (previous row 'i+1', sitting in the exact same column index).
     * 
     * We don't need a full 2D grid! We only need a single 1D array representing 
     * the columns, acting as a scanning line moving upwards.
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n) - Extremely memory efficient!
     */
    public int calculateMinimumHPSpaceOptimized(int[][] dungeon) {
        if (dungeon == null || dungeon.length == 0) return 1;
        
        int m = dungeon.length;
        int n = dungeon[0].length;
        
        // This single array represents the boundary conditions immediately BELOW our current row.
        int[] dp = new int[n + 1];
        
        // BASE CASE REASONING (Seeding the bottom boundary):
        // Fill the dummy row below the dungeon with Infinity, except for the cell 
        // directly under the princess, which requires 1 HP to simulate survival.
        Arrays.fill(dp, INF);
        dp[n - 1] = 1;

        // Traverse upwards row by row
        for (int i = m - 1; i >= 0; i--) {
            
            // Traverse leftwards column by column
            for (int j = n - 1; j >= 0; j--) {
                
                // MAGIC OF THE 1D ARRAY:
                // dp[j] (right side of equals) currently holds the value from row 'i+1' (BELOW).
                // dp[j+1] was just calculated a microsecond ago and holds the value for the cell to the RIGHT.
                int minHealthOnExit = Math.min(dp[j], dp[j + 1]);
                
                // Calculate required health and overwrite dp[j] for the row above us to use later.
                dp[j] = Math.max(1, minHealthOnExit - dungeon[i][j]);
                
            }
            // CRITICAL EDGE CASE HANDLING:
            // After finishing the bottom-most row, the dummy "1 HP" exit condition 
            // under the princess (dp[n-1]) must be reset to Infinity. Otherwise, 
            // the rows above will mistakenly think they can exit the dungeon early 
            // by stepping out of bounds on the right edge!
            dp[n] = INF; 
        }

        return dp[0];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new DungeonGame();
        
        record TestCase(int[][] dungeon, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[][]{
                {-2, -3, 3},
                {-5, -10, 1},
                {10, 30, -5}
            }, 7), // Path: Right, Right, Down, Down.
            new TestCase(new int[][]{
                {0}
            }, 1), // Empty room, just need to be alive.
            new TestCase(new int[][]{
                {100}
            }, 1), // Massive orb, but still need 1 HP to enter.
            new TestCase(new int[][]{
                {-200}
            }, 201), // Massive demon, need 201 HP to survive.
            new TestCase(new int[][]{
                {1, -3, 3},
                {0, -2, 0},
                {-3, -3, -3}
            }, 3)
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Expected Min HP: " + tc.expected);
            
            // Avoid brute force timeout on very large matrices
            if (tc.dungeon.length <= 10 && tc.dungeon[0].length <= 10) {
                System.out.println("Recursive (Brute) : " + solver.calculateMinimumHPRecursive(tc.dungeon));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Grid too large for O(2^(m+n)))");
            }
            
            System.out.println("Memoization       : " + solver.calculateMinimumHPMemo(tc.dungeon));
            System.out.println("Tabulation 2D     : " + solver.calculateMinimumHPTabulation(tc.dungeon));
            System.out.println("Space Optimized   : " + solver.calculateMinimumHPSpaceOptimized(tc.dungeon));
            System.out.println();
        }
    }
}

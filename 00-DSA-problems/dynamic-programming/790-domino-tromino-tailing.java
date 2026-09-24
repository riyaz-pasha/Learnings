class DominoAndTrominoTiling1 {

    public int numTilings(int n) {
        final int MOD = 1_000_000_007;

        if (n == 1)
            return 1;
        if (n == 2)
            return 2;
        if (n == 3)
            return 5;

        long[] dp = new long[n + 1];
        dp[0] = 1;
        dp[1] = 1;
        dp[2] = 2;
        dp[3] = 5;

        for (int i = 4; i <= n; i++) {
            long verticalDominoOrTrominoExtension = dp[i - 1]; // covers vertical and part of tromino mirror logic
            long trominoShapes = dp[i - 3]; // trominoes that start fresh from 3-wide section

            dp[i] = (2 * verticalDominoOrTrominoExtension % MOD + trominoShapes) % MOD;
        }

        return (int) dp[n];
    }

}

class DominoAndTrominoTiling2 {

    final int MOD = 1_000_000_007;
    Long[][] memo;

    public int numTilings(int n) {
        // memo[i][0] = number of ways to tile from column i with no overhang
        // memo[i][1] = number of ways to tile from column i with overhang
        memo = new Long[n + 1][2];
        return (int) countTilings(0, n, false);
    }

    private long countTilings(int col, int n, boolean hasOverhang) {
        if (col == n) {
            return hasOverhang ? 0 : 1; // valid tiling only if no overhang left
        }
        if (col > n)
            return 0;

        int overhangFlag = hasOverhang ? 1 : 0;
        if (memo[col][overhangFlag] != null) {
            return memo[col][overhangFlag];
        }

        long totalWays = 0;

        if (hasOverhang) {
            // Complete the overhang in two ways:
            long fixWithHorizontalDomino = countTilings(col + 1, n, false);
            long extendOverhangWithTromino = countTilings(col + 1, n, true);
            totalWays = (fixWithHorizontalDomino + extendOverhangWithTromino) % MOD;
        } else {
            // 1. Place vertical domino
            long placeVerticalDomino = countTilings(col + 1, n, false);

            // 2. Place two horizontal dominoes
            long placeTwoHorizontals = countTilings(col + 2, n, false);

            // 3. Place L-tromino (two orientations), causing overhang
            long placeTrominoWithOverhang = (2 * countTilings(col + 2, n, true)) % MOD;

            totalWays = (placeVerticalDomino + placeTwoHorizontals + placeTrominoWithOverhang) % MOD;
        }

        memo[col][overhangFlag] = totalWays;
        return totalWays;
    }

}

class DominoAndTrominoTiling4 {

    private static final int MOD = 1_000_000_007;
    private Long[][] memo;

    public int numTilings(int n) {
        // memo[col][0] → no overhang
        // memo[col][1] → has overhang
        memo = new Long[n + 1][2];
        return (int) dfs(0, n, false);
    }

    private long dfs(int col, int n, boolean hasOverhang) {

        // ===== BASE CASES =====

        // Reached exactly the end
        if (col == n) {
            return hasOverhang ? 0 : 1;
        }

        // Overshot board
        if (col > n) return 0;

        int state = hasOverhang ? 1 : 0;
        if (memo[col][state] != null) {
            return memo[col][state];
        }

        long ways = 0;

        if (!hasOverhang) {
            // 1) Vertical domino
            ways = (ways + dfs(col + 1, n, false)) % MOD;

            // 2) Two horizontal dominoes
            ways = (ways + dfs(col + 2, n, false)) % MOD;

            // 3) Tromino (2 orientations) → creates overhang
            ways = (ways + 2 * dfs(col + 2, n, true)) % MOD;
        } else {
            // 1) Fix overhang using tromino
            ways = (ways + dfs(col + 1, n, false)) % MOD;

            // 2) Extend overhang using domino + tromino
            ways = (ways + dfs(col + 1, n, true)) % MOD;
        }

        memo[col][state] = ways;
        return ways;
    }
}


class DominoAndTrominoTiling3 {

    private static final int MOD = 1_000_000_007;
    private Long[][] memo;

    public int numTilings(int n) {
        // memo[col][0] = no overhang at column `col`
        // memo[col][1] = overhang exists at column `col`
        memo = new Long[n + 1][2];
        return (int) countTilingsFrom(0, false, n);
    }

    private long countTilingsFrom(int col, boolean hasOverhang, int boardWidth) {
        if (col == boardWidth)
            return hasOverhang ? 0 : 1;
        if (col > boardWidth)
            return 0;

        int overhangState = hasOverhang ? 1 : 0;
        if (memo[col][overhangState] != null)
            return memo[col][overhangState];

        long ways;
        if (hasOverhang) {
            ways = handleOverhangAt(col, boardWidth);
        } else {
            ways = handleFullyAlignedAt(col, boardWidth);
        }

        return memo[col][overhangState] = ways;
    }

    // 🧩 When there's an overhang, fix it with a horizontal domino or extend it
    // with a mirrored tromino
    private long handleOverhangAt(int col, int n) {
        long fixWithHorizontalDomino = countTilingsFrom(col + 1, false, n);
        long extendWithMirroredTromino = countTilingsFrom(col + 1, true, n);
        return (fixWithHorizontalDomino + extendWithMirroredTromino) % MOD;
    }

    // 🧩 When perfectly aligned, explore all placements: vertical domino,
    // horizontal pair, or tromino
    private long handleFullyAlignedAt(int col, int n) {
        long placeVerticalDomino = countTilingsFrom(col + 1, false, n);
        long placeTwoHorizontalDominoes = countTilingsFrom(col + 2, false, n);
        long placeLShapedTromino = (2 * countTilingsFrom(col + 2, true, n)) % MOD;

        return (placeVerticalDomino + placeTwoHorizontalDominoes + placeLShapedTromino) % MOD;
    }

}

/**
 * DOMINO AND TROMINO TILING
 * 
 * PROBLEM UNDERSTANDING:
 * ---------------------
 * We have a 2 x n board to tile using:
 * 1. DOMINO (2x1): Can be vertical or horizontal
 *    Vertical:  [#]    Horizontal: [##]
 *               [#]
 * 
 * 2. TROMINO (L-shape): Can be rotated 4 ways
 *    [#]     [##]    [#]     [##]
 *    [##]    [#]     [##]    [#]
 * 
 * Count ways to tile 2 x n board. Return answer modulo 10^9 + 7.
 * 
 * CRITICAL INSIGHT:
 * ----------------
 * This is a STATE-BASED DYNAMIC PROGRAMMING problem!
 * 
 * Unlike simple Fibonacci, we need to track the SHAPE of the boundary
 * between what's tiled and what's not.
 * 
 * KEY OBSERVATION:
 * When we tile from left to right, the boundary between filled and unfilled
 * can have different shapes:
 * 
 * STATE 0 (Fully Covered):      STATE 1 (Gap on Top):      STATE 2 (Gap on Bottom):
 *   [####|..]                      [###|...]                   [###|...]
 *   [####|..]                      [####|..]                   [###|...]
 *   ↑ boundary is flat            ↑ top needs 1 more          ↑ bottom needs 1 more
 * 
 * INTERVIEW APPROACH - HOW TO SOLVE THIS:
 * ---------------------------------------
 * 
 * Step 1: UNDERSTAND THE TILES
 * "Let me draw out what tiles we have:
 *  - Domino: 2x1 block (vertical or horizontal)
 *  - Tromino: L-shaped (4 orientations)
 *  
 *  The trominos are the tricky part - they create uneven boundaries!"
 * 
 * Step 2: RECOGNIZE THIS ISN'T SIMPLE FIBONACCI
 * "If we only had dominoes, this would be:
 *  - n=1: 1 way (vertical)
 *  - n=2: 2 ways (two vertical or two horizontal)
 *  - n=3: 3 ways (following Fibonacci)
 *  
 *  But trominos change everything! They create partial fills."
 * 
 * Step 3: IDENTIFY STATES
 * "The key insight: track the state of the rightmost edge:
 *  - State 0: Both cells in column are filled (flat edge)
 *  - State 1: Top cell extends 1 unit right, bottom is flush
 *  - State 2: Bottom cell extends 1 unit right, top is flush
 *  
 *  These states capture all possible boundary shapes."
 * 
 * Step 4: DEFINE TRANSITIONS
 * From each state, what tiles can we place?
 * 
 * From State 0 (flat edge):
 *   - Place vertical domino → State 0
 *   - Place two horizontal dominoes → State 0
 *   - Place tromino (4 orientations) → State 1 or State 2
 * 
 * From State 1 (gap on bottom):
 *   - Fill the gap + use tromino → State 0 or State 2
 * 
 * From State 2 (gap on top):
 *   - Fill the gap + use tromino → State 0 or State 1
 * 
 * Step 5: BUILD DP SOLUTION
 * dp[i][0] = ways to tile up to column i with flat edge
 * dp[i][1] = ways to tile up to column i with gap on top
 * dp[i][2] = ways to tile up to column i with gap on bottom
 */


class DominoAndTrominoTiling {
    
    private static final int MOD = 1_000_000_007;
    
    /**
     * APPROACH 1: STATE-BASED DYNAMIC PROGRAMMING
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * THIS IS THE STANDARD SOLUTION
     * 
     * STATES:
     * dp[i][0] = ways to completely fill 2 x i board
     * dp[i][1] = ways to fill with one cell protruding on top
     * dp[i][2] = ways to fill with one cell protruding on bottom
     * 
     * TRANSITIONS:
     * State 0 → State 0:
     *   - Place vertical domino (takes 1 column)
     *   - Place two horizontal dominoes (takes 2 columns)
     * 
     * State 0 → State 1/2:
     *   - Place tromino in upper-right orientation → State 1
     *   - Place tromino in lower-right orientation → State 2
     * 
     * State 1 → State 0:
     *   - Fill gap with tromino in lower-left orientation
     * 
     * State 1 → State 2:
     *   - Fill gap with horizontal domino, extend with tromino
     * 
     * State 2 → State 0:
     *   - Fill gap with tromino in upper-left orientation
     * 
     * State 2 → State 1:
     *   - Fill gap with horizontal domino, extend with tromino
     */
    public int numTilings(int n) {
        if (n == 1) return 1;
        if (n == 2) return 2;
        
        // dp[i][j] where j is the state (0, 1, or 2)
        long[][] dp = new long[n + 1][3];
        
        // Base cases
        dp[0][0] = 1; // Empty board: 1 way (do nothing)
        dp[1][0] = 1; // 2x1: one vertical domino
        dp[1][1] = 0; // Can't have partial fill with n=1
        dp[1][2] = 0;
        
        dp[2][0] = 2; // 2x2: two ways (VV or HH)
        dp[2][1] = 1; // 2x2 with gap on bottom: one tromino way
        dp[2][2] = 1; // 2x2 with gap on top: one tromino way
        
        // Fill the dp table
        for (int i = 3; i <= n; i++) {
            // State 0: Fully covered
            // Can come from:
            // - State 0 at i-1 + vertical domino
            // - State 0 at i-2 + two horizontal dominoes
            // - State 1 at i-1 + tromino to fill gap
            // - State 2 at i-1 + tromino to fill gap
            dp[i][0] = (dp[i-1][0] + dp[i-2][0] + dp[i-1][1] + dp[i-1][2]) % MOD;
            
            // State 1: Gap on top (bottom protrudes)
            // Can come from:
            // - State 0 at i-2 + tromino
            // - State 2 at i-1 + extend with domino + tromino
            dp[i][1] = (dp[i-2][0] + dp[i-1][2]) % MOD;
            
            // State 2: Gap on bottom (top protrudes)
            // Can come from:
            // - State 0 at i-2 + tromino
            // - State 1 at i-1 + extend with domino + tromino
            dp[i][2] = (dp[i-2][0] + dp[i-1][1]) % MOD;
        }
        
        return (int) dp[n][0];
    }
    
    /**
     * APPROACH 2: SPACE-OPTIMIZED VERSION
     * Time Complexity: O(n)
     * Space Complexity: O(1) - only store last two states
     * 
     * Since we only need dp[i-1] and dp[i-2], we can use variables
     */
    public int numTilingsSpaceOptimized(int n) {
        if (n == 1) return 1;
        if (n == 2) return 2;
        
        // prev2 = dp[i-2], prev1 = dp[i-1], curr = dp[i]
        long prev2_0 = 1, prev2_1 = 0, prev2_2 = 0;
        long prev1_0 = 2, prev1_1 = 1, prev1_2 = 1;
        
        for (int i = 3; i <= n; i++) {
            long curr_0 = (prev1_0 + prev2_0 + prev1_1 + prev1_2) % MOD;
            long curr_1 = (prev2_0 + prev1_2) % MOD;
            long curr_2 = (prev2_0 + prev1_1) % MOD;
            
            // Shift for next iteration
            prev2_0 = prev1_0;
            prev2_1 = prev1_1;
            prev2_2 = prev1_2;
            
            prev1_0 = curr_0;
            prev1_1 = curr_1;
            prev1_2 = curr_2;
        }
        
        return (int) prev1_0;
    }
    
    /**
     * APPROACH 3: MATHEMATICAL PATTERN (ADVANCED)
     * 
     * After working out the recurrence, we can derive:
     * f(n) = 2*f(n-1) + f(n-3)
     * 
     * Where f(n) is the number of ways to tile 2 x n board.
     * 
     * Base cases:
     * f(0) = 1
     * f(1) = 1
     * f(2) = 2
     * 
     * This comes from analyzing the state transitions and simplifying.
     * 
     * PROOF SKETCH:
     * If we track fully-filled states only and account for partial states
     * implicitly, we get this recurrence relation.
     */
    public int numTilingsMath(int n) {
        if (n == 0) return 1;
        if (n == 1) return 1;
        if (n == 2) return 2;
        
        long[] dp = new long[n + 1];
        dp[0] = 1;
        dp[1] = 1;
        dp[2] = 2;
        
        for (int i = 3; i <= n; i++) {
            dp[i] = (2 * dp[i-1] % MOD + dp[i-3]) % MOD;
        }
        
        return (int) dp[n];
    }
    
    /**
     * APPROACH 4: DETAILED STATE MACHINE VISUALIZATION
     * 
     * This approach makes the state transitions crystal clear
     * by explicitly naming each transition type.
     */
    public int numTilingsDetailed(int n) {
        if (n == 1) return 1;
        if (n == 2) return 2;
        
        long[][] dp = new long[n + 1][3];
        dp[0][0] = 1;
        dp[1][0] = 1;
        dp[2][0] = 2;
        dp[2][1] = 1;
        dp[2][2] = 1;
        
        for (int i = 3; i <= n; i++) {
            // To reach fully-covered state at column i:
            long fromPrevFullViaVertical = dp[i-1][0];      // Add vertical domino
            long fromPrev2FullViaHorizontal = dp[i-2][0];   // Add two horizontal dominoes
            long fromPrevTopGap = dp[i-1][1];               // Fill gap with tromino
            long fromPrevBottomGap = dp[i-1][2];            // Fill gap with tromino
            
            dp[i][0] = (fromPrevFullViaVertical + fromPrev2FullViaHorizontal + 
                        fromPrevTopGap + fromPrevBottomGap) % MOD;
            
            // To reach top-gap state at column i:
            long fromPrev2FullViaTromino = dp[i-2][0];      // Place tromino
            long fromPrevBottomGapExtend = dp[i-1][2];      // Extend with domino+tromino
            
            dp[i][1] = (fromPrev2FullViaTromino + fromPrevBottomGapExtend) % MOD;
            
            // To reach bottom-gap state at column i:
            long fromPrev2FullViaTromino2 = dp[i-2][0];     // Place tromino
            long fromPrevTopGapExtend = dp[i-1][1];         // Extend with domino+tromino
            
            dp[i][2] = (fromPrev2FullViaTromino2 + fromPrevTopGapExtend) % MOD;
        }
        
        return (int) dp[n][0];
    }
    
    /**
     * COMMON MISTAKES TO AVOID:
     * -------------------------
     * 
     * 1. TREATING THIS AS SIMPLE FIBONACCI
     *    ❌ f(n) = f(n-1) + f(n-2)
     *    ✓ Need to track partial states due to trominos
     *    
     * 2. FORGETTING TROMINO ROTATIONS
     *    - Trominos can be placed in 4 different orientations
     *    - Each creates different state transitions
     *    
     * 3. NOT TRACKING PARTIAL FILLS
     *    - After placing a tromino, one cell extends beyond
     *    - Must track these "gap" states
     *    
     * 4. WRONG BASE CASES
     *    ❌ dp[1][1] = 1 (can't have partial with n=1)
     *    ✓ dp[1][1] = 0
     *    ✓ dp[2][1] = 1 (one tromino creates this)
     *    
     * 5. MISSING STATE TRANSITIONS
     *    - Must consider ALL ways to reach each state
     *    - State 0 can come from States 0, 1, and 2
     *    
     * 6. FORGETTING MODULO
     *    - Apply modulo at every addition
     *    - Use long to prevent overflow before modulo
     *    
     * 7. INDEXING CONFUSION
     *    - dp[i] means "ways to tile first i columns"
     *    - Be consistent with 0-indexing vs 1-indexing
     */
    
    /**
     * INTERVIEW COMMUNICATION STRATEGY:
     * ---------------------------------
     * 
     * PHASE 1: UNDERSTAND THE PROBLEM (3-4 min)
     * "Let me make sure I understand the tiles:
     *  
     *  Domino (2x1):
     *    [#]     or    [##]
     *    [#]
     *  
     *  Tromino (L-shape, 4 rotations):
     *    [#]     [##]    [#]     [##]
     *    [##]    [#]     [##]    [#]
     *  
     *  For n=1: Only one vertical domino fits → 1 way
     *  For n=2: Two vertical OR two horizontal → 2 ways
     *  For n=3: This gets interesting with trominos..."
     * 
     * PHASE 2: RECOGNIZE THE PATTERN (4-5 min)
     * "Initially, this looks like Fibonacci (placing dominoes).
     *  But trominos create partial fills!
     *  
     *  The key insight: When we tile left-to-right, the boundary
     *  between filled and unfilled can have different shapes:
     *  
     *  State 0: Fully covered (flat boundary)
     *    [####|...]
     *    [####|...]
     *  
     *  State 1: Bottom cell protrudes (gap on top)
     *    [###|...]
     *    [####|..]
     *  
     *  State 2: Top cell protrudes (gap on bottom)
     *    [####|..]
     *    [###|...]
     *  
     *  We need to track these states!"
     * 
     * PHASE 3: EXPLAIN THE APPROACH (3 min)
     * "I'll use dynamic programming with state tracking:
     *  
     *  dp[i][0] = ways to fully tile first i columns
     *  dp[i][1] = ways to tile with gap on top at column i
     *  dp[i][2] = ways to tile with gap on bottom at column i
     *  
     *  Transitions:
     *  - State 0 can come from placing dominoes or filling gaps
     *  - States 1 and 2 come from placing trominos
     *  
     *  Time: O(n), Space: O(n) [can optimize to O(1)]"
     * 
     * PHASE 4: CODE (15-18 min)
     * - Set up base cases carefully
     * - Implement state transitions
     * - Apply modulo throughout
     * 
     * PHASE 5: TEST (4-5 min)
     * Walk through n=3:
     * - Calculate dp[3][0], dp[3][1], dp[3][2]
     * - Verify answer is 5
     */
    
    /**
     * DETAILED WALKTHROUGH: n = 3
     * ---------------------------
     * 
     * Base cases:
     * dp[0][0] = 1
     * dp[1][0] = 1, dp[1][1] = 0, dp[1][2] = 0
     * dp[2][0] = 2, dp[2][1] = 1, dp[2][2] = 1
     * 
     * For i = 3:
     * 
     * dp[3][0] (fully covered):
     *   = dp[2][0] + dp[1][0] + dp[2][1] + dp[2][2]
     *   = 2 + 1 + 1 + 1
     *   = 5
     * 
     * dp[3][1] (gap on top):
     *   = dp[1][0] + dp[2][2]
     *   = 1 + 1
     *   = 2
     * 
     * dp[3][2] (gap on bottom):
     *   = dp[1][0] + dp[2][1]
     *   = 1 + 1
     *   = 2
     * 
     * Answer: dp[3][0] = 5 ✓
     * 
     * The 5 ways for n=3:
     * 1. [V][V][V]         - Three vertical dominoes
     * 2. [V][HH]           - Vertical + two horizontal
     * 3. [HH][V]           - Two horizontal + vertical
     * 4. [Tromino1][Dom]   - Tromino + fill
     * 5. [Tromino2][Dom]   - Tromino + fill (different orientation)
     */
    
    /**
     * VISUAL EXPLANATION OF STATES:
     * -----------------------------
     * 
     * STATE 0 → STATE 0 (via vertical domino):
     *   [####][#]...        Add vertical domino
     *   [####][#]...
     * 
     * STATE 0 → STATE 0 (via two horizontal dominoes):
     *   [####][##]...       Add two horizontal
     *   [####][##]...
     * 
     * STATE 0 → STATE 1 (via tromino):
     *   [####][#]...        Place tromino
     *   [####][##]..        Bottom protrudes
     * 
     * STATE 0 → STATE 2 (via tromino):
     *   [####][##]..        Place tromino
     *   [####][#]...        Top protrudes
     * 
     * STATE 1 → STATE 0 (fill gap):
     *   [###.][##]..        Fill gap with tromino
     *   [####][#]...
     * 
     * STATE 1 → STATE 2 (extend):
     *   [###.][##]..        Fill + extend
     *   [####][##]..
     */
    
    /**
     * FOLLOW-UP QUESTIONS:
     * --------------------
     * 
     * Q1: Can you optimize space to O(1)?
     * A: Yes! We only need the last two columns' states.
     *    Use 6 variables: prev2[0,1,2] and prev1[0,1,2]
     * 
     * Q2: What if the board is 3 x n instead of 2 x n?
     * A: Much more complex! Need to track more states.
     *    Number of states grows exponentially with board height.
     * 
     * Q3: What if we had different tile shapes?
     * A: Would need to:
     *    - Identify all possible boundary states
     *    - Define transitions for each tile placement
     *    - May need more than 3 states
     * 
     * Q4: How did you derive the mathematical recurrence f(n) = 2*f(n-1) + f(n-3)?
     * A: By analyzing the state transitions and collapsing them:
     *    - States 1 and 2 are symmetric
     *    - Can eliminate intermediate states algebraically
     *    - Results in simpler recurrence
     * 
     * Q5: Can we use matrix exponentiation for very large n?
     * A: Yes! The recurrence can be expressed as matrix multiplication.
     *    Would give O(log n) time with matrix exponentiation.
     */
    
    /**
     * WHY STATE TRACKING IS NECESSARY:
     * --------------------------------
     * 
     * WITHOUT TROMINOS (only dominoes):
     * - Simple Fibonacci: f(n) = f(n-1) + f(n-2)
     * - n=1: 1 way (V)
     * - n=2: 2 ways (VV or HH)
     * - n=3: 3 ways (VVV, VHH, HHV)
     * 
     * WITH TROMINOS:
     * - n=3 should have 5 ways, not 3!
     * - Trominos create partial fills that dominoes can't capture
     * - Need to track these intermediate states
     * 
     * Example: After placing tromino at position 0-1:
     *   [#][#]....
     *   [##]....
     * The boundary is uneven - must track this!
     */
    
    /**
     * COMPARISON TO SIMILAR PROBLEMS:
     * -------------------------------
     * 
     * 1. FIBONACCI/CLIMBING STAIRS:
     *    - Simple state: just count ways
     *    - This problem: track boundary shape
     * 
     * 2. TILE RECTANGLE:
     *    - If only 1xn tiles, just Fibonacci
     *    - Different tile shapes require state tracking
     * 
     * 3. PAINT FENCE:
     *    - Track color of previous fence post
     *    - Similar idea of state-dependent transitions
     * 
     * 4. HOUSE ROBBER:
     *    - Track whether previous house robbed
     *    - Binary state vs our 3-state system
     */
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        DominoAndTrominoTiling solution = new DominoAndTrominoTiling();
        
        // Test Case 1: Small case
        System.out.println("Test 1: n = 1");
        System.out.println("Expected: 1");
        System.out.println("Result: " + solution.numTilings(1));
        System.out.println();
        
        // Test Case 2: Small case
        System.out.println("Test 2: n = 2");
        System.out.println("Expected: 2");
        System.out.println("Result: " + solution.numTilings(2));
        System.out.println();
        
        // Test Case 3: Example case
        System.out.println("Test 3: n = 3");
        System.out.println("Expected: 5");
        System.out.println("State-based: " + solution.numTilings(3));
        System.out.println("Math formula: " + solution.numTilingsMath(3));
        System.out.println("Space optimized: " + solution.numTilingsSpaceOptimized(3));
        System.out.println();
        
        // Test Case 4: Larger case
        System.out.println("Test 4: n = 4");
        System.out.println("Expected: 11");
        System.out.println("Result: " + solution.numTilings(4));
        System.out.println();
        
        // Test Case 5: Even larger
        System.out.println("Test 5: n = 5");
        System.out.println("Result: " + solution.numTilings(5));
        System.out.println();
        
        // Test Case 6: Test modulo for large n
        System.out.println("Test 6: n = 30");
        System.out.println("Result: " + solution.numTilings(30));
        System.out.println("(Testing modulo arithmetic)");
        System.out.println();
        
        // Test Case 7: Very large n
        System.out.println("Test 7: n = 1000");
        long start = System.currentTimeMillis();
        int result = solution.numTilings(1000);
        long time = System.currentTimeMillis() - start;
        System.out.println("Result: " + result);
        System.out.println("Time: " + time + "ms");
        System.out.println();
        
        // Detailed trace for n=3
        System.out.println("=== DETAILED TRACE FOR n=3 ===");
        traceStates(3);
        
        // Compare all approaches
        System.out.println("\n=== COMPARING APPROACHES ===");
        int n = 10;
        System.out.println("n = " + n);
        System.out.println("State-based: " + solution.numTilings(n));
        System.out.println("Math formula: " + solution.numTilingsMath(n));
        System.out.println("Space optimized: " + solution.numTilingsSpaceOptimized(n));
        System.out.println("Detailed: " + solution.numTilingsDetailed(n));
    }
    
    /**
     * Helper method to trace state transitions
     */
    private static void traceStates(int n) {
        if (n < 1) return;
        
        long[][] dp = new long[n + 1][3];
        dp[0][0] = 1;
        
        System.out.println("Base: dp[0][0] = 1");
        
        if (n >= 1) {
            dp[1][0] = 1;
            System.out.println("Base: dp[1] = [1, 0, 0]");
        }
        
        if (n >= 2) {
            dp[2][0] = 2;
            dp[2][1] = 1;
            dp[2][2] = 1;
            System.out.println("Base: dp[2] = [2, 1, 1]");
        }
        
        for (int i = 3; i <= n; i++) {
            dp[i][0] = (dp[i-1][0] + dp[i-2][0] + dp[i-1][1] + dp[i-1][2]) % MOD;
            dp[i][1] = (dp[i-2][0] + dp[i-1][2]) % MOD;
            dp[i][2] = (dp[i-2][0] + dp[i-1][1]) % MOD;
            
            System.out.println("\nCalculating dp[" + i + "]:");
            System.out.println("  State 0: " + dp[i-1][0] + " + " + dp[i-2][0] + 
                             " + " + dp[i-1][1] + " + " + dp[i-1][2] + " = " + dp[i][0]);
            System.out.println("  State 1: " + dp[i-2][0] + " + " + dp[i-1][2] + " = " + dp[i][1]);
            System.out.println("  State 2: " + dp[i-2][0] + " + " + dp[i-1][1] + " = " + dp[i][2]);
        }
        
        System.out.println("\nFinal answer: dp[" + n + "][0] = " + dp[n][0]);
    }
}

/**
 * FINAL INTERVIEW CHECKLIST:
 * --------------------------
 * ✓ Understand both tile types (domino and tromino)
 * ✓ Recognize need for state tracking (not simple Fibonacci)
 * ✓ Define three states (fully covered, gap on top, gap on bottom)
 * ✓ Identify all state transitions
 * ✓ Set correct base cases
 * ✓ Apply modulo at every addition
 * ✓ Test with small examples (n=1,2,3)
 * ✓ Discuss space optimization
 * 
 * TIME TO SOLVE: 30-35 minutes
 * - 4 min: Understanding tiles and problem
 * - 5 min: Recognizing need for states
 * - 3 min: Defining states and transitions
 * - 3 min: Explaining approach
 * - 15 min: Coding with careful state tracking
 * - 5 min: Testing and verification
 * 
 * DIFFICULTY: Medium-Hard
 * (Medium if you know state DP, Hard if trying Fibonacci first)
 * 
 * KEY TAKEAWAYS:
 * 1. Trominos create uneven boundaries - MUST track states
 * 2. Three states: fully covered, gap on top, gap on bottom
 * 3. State 0 is what we care about (fully tiled board)
 * 4. Can optimize space to O(1) using rolling variables
 * 5. Alternative: mathematical recurrence f(n) = 2*f(n-1) + f(n-3)
 * 
 * MOST IMPORTANT INSIGHT:
 * This is NOT simple Fibonacci because trominos create partial fills
 * that span column boundaries. State tracking is essential!
 */

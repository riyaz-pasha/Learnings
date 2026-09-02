/**
 * ============================================================================
 * MINIMUM MOVES TO SPREAD STONES OVER GRID - COMPREHENSIVE GUIDE
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We have a 3x3 grid containing exactly 9 stones in total. However, the stones 
 * are unevenly distributed. Some cells might have multiple stones, and some 
 * might be completely empty. Our goal is to move the stones around (one step 
 * at a time, up, down, left, or right) until every single cell has exactly ONE 
 * stone. We need to find the absolute minimum number of moves to achieve this.
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Can a stone pass through a cell that already has a stone?
 * A: Yes! A move simply shifts a stone to an adjacent cell. Passing through 
 *    occupied cells is perfectly fine and costs exactly 1 move per step.
 * 
 * Q: What is the maximum grid size and number of stones?
 * A: The problem restricts the grid strictly to 3x3 and 9 stones. 
 *    This is a massive hint! Because the grid is so tiny, the maximum number 
 *    of empty cells is 8 (if all 9 stones are stacked in one cell). 
 *    Factorial time complexity (8! = 40,320 operations) will run instantaneously.
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - MANHATTAN DISTANCE: The minimum number of moves to transfer a stone from 
 *   cell A to cell B is their Manhattan distance: |r1 - r2| + |c1 - c2|.
 * - SOURCES AND TARGETS: We can ignore cells that already have exactly 1 stone. 
 *   We only care about "Targets" (empty cells) and "Sources" (extra stones). 
 *   If a cell has 3 stones, it has 2 *extra* stones, so we treat it as 2 distinct sources.
 * - THE MATCHING PROBLEM: Since all stones are identical, our task is simply to 
 *   pair every Target with a unique Source such that the sum of their Manhattan 
 *   distances is minimized.
 * - BACKTRACKING: Since the number of extra stones is at most 8, we can use 
 *   DFS/Backtracking to try every possible pairing combination (permutation) 
 *   of Targets to Sources and find the one with the lowest total distance.
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Explain the Manhattan distance concept. Show that actual pathfinding 
 *   (like BFS) is unnecessary because stones can pass through each other.
 * - Step 2: Separate the grid into Lists of `targets` and `sources`. Emphasize 
 *   that a cell with 'N' stones contributes 'N-1' elements to the `sources` list.
 * - Step 3: Write the DFS to permute all possible matches between targets and 
 *   sources. Track the `used` sources with a boolean array.
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Grid: 
 * [1, 0, 1]
 * [0, 2, 2]
 * [1, 1, 1]
 * 
 * Targets (0s): (0,1), (1,0)
 * Sources (>1): (1,1), (1,2) 
 * Note: Cell (1,1) has 2 stones, meaning 1 extra. Cell (1,2) has 1 extra.
 * 
 * Permutation 1:
 * Match Target(0,1) to Source(1,1) -> dist = 1
 * Match Target(1,0) to Source(1,2) -> dist = 2
 * Total = 3
 * 
 * Permutation 2:
 * Match Target(0,1) to Source(1,2) -> dist = 2
 * Match Target(1,0) to Source(1,1) -> dist = 1
 * Total = 3
 * 
 * Min moves = 3.
 */

import java.util.*;

public class SpreadStones {

    // Modern Java Feature: Record for immutable, clean coordinate carriers.
    record Point(int r, int c) {}

    public int minimumMoves(int[][] grid) {
        
        List<Point> targets = new ArrayList<>();
        List<Point> sources = new ArrayList<>();

        /*
         * ============================================================
         * STEP 1: EXTRACT SOURCES AND TARGETS
         * ============================================================
         * 
         * We iterate through the 3x3 grid.
         * 
         * If grid[r][c] == 0:
         *      It's missing a stone. Add its coordinate to 'targets'.
         * 
         * If grid[r][c] > 1:
         *      It has 'extra' stones. If a cell has 3 stones, it needs to keep 
         *      1 for itself, meaning it has 2 extra to give away. 
         *      We add its coordinate to 'sources' exactly (count - 1) times.
         */
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (grid[r][c] == 0) {
                    targets.add(new Point(r, c));
                } else if (grid[r][c] > 1) {
                    int extraStones = grid[r][c] - 1;
                    for (int i = 0; i < extraStones; i++) {
                        sources.add(new Point(r, c));
                    }
                }
            }
        }

        // The number of empty cells will exactly match the number of extra stones.
        boolean[] usedSources = new boolean[sources.size()];
        
        // Start the backtracking process from the 0th target.
        return backtrack(0, targets, sources, usedSources);
    }

    private int backtrack(int targetIndex, List<Point> targets, List<Point> sources, boolean[] usedSources) {
        
        /*
         * ============================================================
         * BASE CASE
         * ============================================================
         * 
         * If we have found a match for every target in our list, 
         * it means all empty cells have been assigned an extra stone.
         * The cost for any remaining moves is 0.
         */
        if (targetIndex == targets.size()) {
            return 0;
        }

        int minTotalMoves = Integer.MAX_VALUE;
        Point currentTarget = targets.get(targetIndex);

        /*
         * ============================================================
         * THE DECISION LOOP
         * ============================================================
         * 
         * We must decide: "Which source stone should we move to fill 
         * this specific target cell?"
         * 
         * We iterate through all available extra stones in our 'sources' list.
         */
        for (int i = 0; i < sources.size(); i++) {
            
            // If this specific extra stone has already been assigned to a 
            // previous target in our current recursive path, we skip it.
            if (usedSources[i]) {
                continue;
            }

            /*
             * ========================================================
             * CHOOSE
             * ========================================================
             * 
             * We are trying to pair the current target with source[i].
             * We mark source[i] as used.
             */
            usedSources[i] = true;

            /*
             * ========================================================
             * EXPLORE
             * ========================================================
             * 
             * 1. Calculate the distance (cost) to move this stone.
             * 2. Recursively find the minimum cost to fulfill all the 
             *    REMAINING targets (targetIndex + 1).
             * 3. Add them together to get the total cost of this specific 
             *    branch/permutation.
             */
            Point source = sources.get(i);
            int distance = Math.abs(currentTarget.r() - source.r()) + 
                           Math.abs(currentTarget.c() - source.c());
                           
            int remainingMoves = backtrack(targetIndex + 1, targets, sources, usedSources);
            int totalCostForThisPath = distance + remainingMoves;
            
            // Update our global minimum for this target
            minTotalMoves = Math.min(minTotalMoves, totalCostForThisPath);

            /*
             * ========================================================
             * UNCHOOSE / BACKTRACK
             * ========================================================
             * 
             * We finished exploring what happens if target paired with source[i].
             * Now we UNDO that choice, making source[i] available again, 
             * so the loop can try pairing the target with the NEXT available source.
             */
            usedSources[i] = false;
        }

        // Return the absolute best result found after trying all permutations for this target
        return minTotalMoves;
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        SpreadStones solver = new SpreadStones();
        
        // Test Case 1: Simple 1-step move
        int[][] grid1 = {
            {1, 1, 0},
            {1, 1, 1},
            {1, 2, 1}
        };
        System.out.println("--- Test Case 1 ---");
        System.out.println("Expected: 3 | Result: " + solver.minimumMoves(grid1));
        
        // Test Case 2: Clustered stones
        // Total stones = 9. 0s = 8. All extra stones are in center (1,1).
        int[][] grid2 = {
            {0, 0, 0},
            {0, 9, 0},
            {0, 0, 0}
        };
        // Expected: 
        // 4 corners take 2 moves each = 8
        // 4 edges take 1 move each = 4
        // Total = 12
        System.out.println("\n--- Test Case 2 ---");
        System.out.println("Expected: 12 | Result: " + solver.minimumMoves(grid2));

        // Test Case 3: Already solved
        int[][] grid3 = {
            {1, 1, 1},
            {1, 1, 1},
            {1, 1, 1}
        };
        System.out.println("\n--- Test Case 3 ---");
        System.out.println("Expected: 0 | Result: " + solver.minimumMoves(grid3));
    }
}

/**
 * ================================================================
 * 🔥 Minimum Moves to Spread Stones in 3x3 Grid
 * ================================================================
 *
 * 🧠 CORE IDEA:
 * -------------
 * Convert grid problem → Assignment problem
 *
 *   EXTRA stones  → must go to → EMPTY cells
 *
 * Each assignment has cost = Manhattan Distance
 *
 * We try ALL assignments and pick minimum cost.
 *
 * ================================================================
 *
 * 🧩 WHY BACKTRACKING?
 * -------------------
 * Max cells = 9
 * Max extras ≤ 8
 * → Worst permutations = 8! = 40320 → manageable
 *
 * ================================================================
 */

public class MinimumMovesToSpreadStones {

    /**
     * Java 24 record for clean coordinate handling
     */
    record Cell(int row, int col) {}

    public static int minimumMoves(int[][] grid) {

        /**
         * STEP 1: Collect EXTRA stones and EMPTY cells
         *
         * extras → each extra stone is stored separately
         * empties → positions needing stones
         */
        List<Cell> extras = new ArrayList<>();
        List<Cell> empties = new ArrayList<>();

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {

                int stones = grid[r][c];

                if (stones == 0) {
                    // This cell needs a stone
                    empties.add(new Cell(r, c));
                }

                else if (stones > 1) {
                    /**
                     * IMPORTANT:
                     * If cell has 3 stones → 2 are extra
                     * So we push SAME coordinate multiple times
                     */
                    for (int k = 0; k < stones - 1; k++) {
                        extras.add(new Cell(r, c));
                    }
                }
            }
        }

        /**
         * STEP 2: Backtracking setup
         *
         * used[i] → whether empties[i] is already assigned
         */
        boolean[] used = new boolean[empties.size()];

        /**
         * Start recursive assignment
         */
        return backtrack(0, extras, empties, used);
    }

    /**
     * ============================================================
     * 🔁 BACKTRACKING FUNCTION
     * ============================================================
     *
     * idx → which extra stone we are assigning
     *
     * At each step:
     *   Try assigning current stone to every unused empty cell
     *
     * ============================================================
     */
    private static int backtrack(int idx,
                                 List<Cell> extras,
                                 List<Cell> empties,
                                 boolean[] used) {

        /**
         * BASE CASE:
         * All stones assigned → no cost left
         */
        if (idx == extras.size()) {
            return 0;
        }

        int minMoves = Integer.MAX_VALUE;

        // Current extra stone
        Cell stone = extras.get(idx);

        /**
         * Try placing this stone into every empty cell
         */
        for (int i = 0; i < empties.size(); i++) {

            // Skip if already used
            if (used[i]) continue;

            // Choose this empty cell
            used[i] = true;

            Cell empty = empties.get(i);

            /**
             * COST calculation:
             * Manhattan distance = |r1 - r2| + |c1 - c2|
             *
             * WHY Manhattan?
             * Because movement allowed only in 4 directions
             */
            int cost = Math.abs(stone.row - empty.row)
                     + Math.abs(stone.col - empty.col);

            /**
             * RECURSION:
             * Assign next stone
             */
            int nextCost = backtrack(idx + 1, extras, empties, used);

            /**
             * Combine current cost + future cost
             */
            int totalCost = cost + nextCost;

            /**
             * Update minimum
             */
            minMoves = Math.min(minMoves, totalCost);

            /**
             * BACKTRACK:
             * Undo assignment
             */
            used[i] = false;
        }

        return minMoves;
    }

    /**
     * ============================================================
     * 🔍 DRY RUN EXAMPLE
     * ============================================================
     *
     * grid =
     * [1,1,0]
     * [1,1,1]
     * [1,2,1]
     *
     * extras = [(2,1)]
     * empties = [(0,2)]
     *
     * Only one assignment:
     * cost = |2-0| + |1-2| = 2 + 1 = 3
     *
     * Answer = 3
     *
     * ============================================================
     */

    public static void main(String[] args) {

        int[][] grid = {
            {1, 1, 0},
            {1, 1, 1},
            {1, 2, 1}
        };

        System.out.println(minimumMoves(grid));
    }
}

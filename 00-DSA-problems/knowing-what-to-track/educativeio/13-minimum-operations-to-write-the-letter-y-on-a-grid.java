/**
 * STANDALONE STUDY NOTE: MINIMUM OPERATIONS TO WRITE THE LETTER Y
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Is `n` always guaranteed to be an odd integer greater than or equal to 3?" (Yes, constraints state 3 <= n <= 49 and n is odd, ensuring a unique center cell exists at coordinates `(n/2, n/2)`).
 * - "Can grid values be changed to any integer from 0, 1, or 2?" (Yes, exactly those three values).
 * - "What constitutes a valid 'Letter Y' configuration?" (All cells on the Y shape must have the same value `v1`, all cells outside the Y shape must have the same value `v2`, and `v1 != v2`).
 * - "How many possible valid (v1, v2) value pairs are there?" (Since v1 and v2 are chosen from {0, 1, 2} and v1 != v2, there are 3 choices for v1 and 2 choices for v2, giving 3 * 2 = 6 possible color configurations).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to transform an n x n grid into a "Letter Y" shape where Y cells share one color and non-Y cells 
 * share a different color, minimizing the number of operations (cell value changes). The binding constraint is 
 * that checking every possible assignment of colors to every cell is intractable; instead, we must leverage the 
 * extremely small number of global color combinations.
 * 
 * --- APPROACH 1: BRUTE FORCE (Exhaustive Grid Recolor Search) ---
 * 1. What I'd naturally try: Try every possible way to assign values (0, 1, or 2) to every cell in the n x n grid, 
 *    check which ones form a valid Letter Y, and find the one that requires the minimum edits from the original grid.
 * 2. Why it works: It tests every possible grid configuration.
 * 3. Why it's too slow/costly: An n x n grid has 3^(n^2) possible configurations. For n = 49, n^2 = 2401, 
 *    making 3^2401 completely impossible to compute.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(3^(N^2) * N^2) — exponential time over total cells.
 *    - Space Complexity: O(N^2) — recursion or grid state storage.
 * 
 * --- APPROACH 2: INTERMEDIATE (Pre-calculate Y vs Non-Y Frequency Counts) ---
 * 1. The Observation: There are only 6 possible valid configurations for `(v1, v2)` where `v1` is the color of the Y 
 *    shape and `v2` is the color of the non-Y cells (`v1 != v2`, with both in `{0, 1, 2}`). 
 *    Instead of guessing the whole grid, we can pre-calculate two frequency histograms: 
 *    - `yCounts[val]`: How many cells *in* the Y shape currently have value `val`.
 *    - `nonYCounts[val]`: How many cells *outside* the Y shape currently have value `val`.
 * 2. How it leads to the next step: For any chosen pair of distinct values `(v1, v2)`, the number of changes required is:
 *    `(Total Y cells - yCounts[v1]) + (Total Non-Y cells - nonYCounts[v2])`. 
 *    We can simply loop through all 6 valid pairs, compute the cost for each, and return the minimum.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N^2) — a single linear pass over the grid to count frequencies, followed by checking 6 constant pairs.
 *    - Space Complexity: O(1) auxiliary space (aside from storing the input grid).
 * 
 * --- APPROACH 3: OPTIMAL (Single-Pass Frequency Counting + 6-Pair Iteration) ---
 * 1. The Observation: We don't even need separate arrays for Y and non-Y. As we iterate through the grid cells `(r, c)` 
 *    using nested loops, we can mathematically classify whether `(r, c)` belongs to the Y shape using simple coordinate rules:
 *    - Top-left diagonal: `r == c` for `r <= n/2`.
 *    - Top-right diagonal: `r + c == n - 1` for `r <= n/2`.
 *    - Vertical stem: `c == n / 2` for `r >= n/2`.
 * 2. How it leads to the next step: For each cell, we increment its respective count in our frequency trackers. 
 *    After visiting all cells, we test the 6 valid `(v1, v2)` permutations and pick the minimum cost.
 * 3. Which one I'd write in an interview: Approach 3. It's concise, executes in linear time O(N^2), uses O(1) space, 
 *    and cleanly isolates geometric coordinate logic from the optimization logic.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^2) — because we visit each of the N x N grid cells exactly once.
 *    - Space Complexity: O(1) — because our frequency arrays are fixed size of 3 (`int[3]`), taking constant memory.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Minimum grid size (n = 3): Handled correctly by center index `n/2 = 1`.
 * - Grid already fully matching a valid Y: Cost evaluates to 0.
 * - All cells same value: Handled correctly since `v1 != v2` ensures we never pick an invalid uniform grid.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Letter Y Geometry on an n x n Grid (e.g., n = 5):
 * 
 *   (0,0)       (0,4)
 *     Y . . . Y       -> r == c (left diagonal) & r + c == n - 1 (right diagonal) for r <= n/2
 *     . Y . Y .
 *     . . Y . .       -> Center cell at (2,2) where r == n/2
 *     . . Y . .       -> Vertical stem where c == n/2 for r >= n/2
 *     . . Y . .
 * 
 * Total Y cells count = (number of cells satisfying Y conditions).
 * Total Non-Y cells count = (N * N - Total Y cells).
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: n = 3, grid = 
 * [[0, 1, 0],
 *  [1, 0, 1],
 *  [0, 1, 0]]
 * 
 * Center = 3 / 2 = 1.
 * 
 * Cell classification:
 * - (0,0): r=0, c=0 -> r==c (0<=1) -> Y cell (value = 0)
 * - (0,1): r=0, c=1 -> c==1 && r>=1 (False) -> Non-Y cell (value = 1)
 * - (0,2): r=0, c=2 -> r+c=2 (0<=1) -> Y cell (value = 0)
 * - (1,0): r=1, c=0 -> False -> Non-Y cell (value = 1)
 * - (1,1): r=1, c=1 -> r==c (1<=1) -> Y cell (value = 0)
 * - (1,2): r=1, c=2 -> False -> Non-Y cell (value = 1)
 * - (2,0): r=2, c=0 -> False -> Non-Y cell (value = 0)
 * - (2,1): r=2, c=1 -> c==1 && r>=1 (2>=1) -> Y cell (value = 1)
 * - (2,2): r=2, c=2 -> c==1 (False), r==c (2<=1 False) -> Wait, center is (2,2). 
 *   Let's check center: r=2, c=2. Is it in Y? 
 *   Stem condition: c == n/2 (2 == 1) -> False.
 *   Let's re-verify Y conditions for n=3:
 *   - Top-left diagonal: r == c for r <= 1 -> (0,0), (1,1).
 *   - Top-right diagonal: r + c == 2 for r <= 1 -> (0,2), (1,1).
 *   - Vertical stem: c == 1 for r >= 1 -> (1,1), (2,1).
 *   So Y cells are (0,0), (0,2), (1,1), (2,1). Total 4 Y cells.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "Since the grid size n is odd, the Letter Y has a precise geometric definition consisting of two top diagonals 
 * meeting at the center `(n/2, n/2)` and a vertical stem extending to the bottom border. There are only 3 possible 
 * values ({0, 1, 2}), meaning there are only 6 valid pairs of distinct colors for the Y cells (`v1`) and non-Y cells (`v2`). 
 * We can make a single pass over the grid to classify each cell as either part of Y or non-Y, and tally their current value 
 * frequencies. Then, we iterate through all 6 valid color pairs, calculate the number of changes needed for each, 
 * and return the minimum operations. This runs in O(N^2) time and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the allowed values were not just 0, 1, 2, but K distinct colors?
 * A: The number of color pairs would increase from 6 to K * (K - 1). We would loop over all K*(K-1) pairs 
 *    instead of hardcoding 6 pairs, keeping the time complexity at O(K^2 + N^2).
 * 
 * Q: How would you parallelize this if n was 100,000?
 * A: We could use Java's parallel streams to partition the grid rows across multiple threads when computing 
 *    the initial frequency histograms, while the final 6-pair evaluation remains instantaneous.
 */

public class MinimumOperationsToWriteTheLetterY {

    /**
     * APPROACH 3: OPTIMAL (Single-Pass Classification & 6-Pair Minimum Search)
     * Time: O(N^2), Space: O(1)
     */
    public int minimumOperationsToWriteY(int[][] grid) {
        int n = grid.length;
        int center = n / 2;

        // [Why needed]: Frequency histograms for cells belonging to the Y shape and non-Y cells.
        // Index 0, 1, 2 correspond to grid values 0, 1, and 2 respectively.
        int[] yCounts = new int[3];
        int[] nonYCounts = new int[3];
        int totalYCells = 0;
        int totalNonYCells = 0;

        // Phase 1: Single pass over the grid to classify cells and count frequencies
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                // [What it does]: Determine if cell (r, c) belongs to the Letter Y shape.
                // Condition 1: Top-left diagonal (r == c for r <= center)
                // Condition 2: Top-right diagonal (r + c == n - 1 for r <= center)
                // Condition 3: Vertical stem (c == center for r >= center)
                boolean isY = (r <= center && (r == c || r + c == n - 1)) || 
                              (r >= center && c == center);

                if (isY) {
                    yCounts[grid[r][c]]++;
                    totalYCells++;
                } else {
                    nonYCounts[grid[r][c]]++;
                    totalNonYCells++;
                }
            }
        }

        int minOperations = Integer.MAX_VALUE;

        // Phase 2: Test all 6 valid color pairs (v1 for Y cells, v2 for non-Y cells)
        // Rule: v1 != v2, where v1, v2 in {0, 1, 2}
        for (int v1 = 0; v1 < 3; v1++) {
            for (int v2 = 0; v2 < 3; v2++) {
                if (v1 == v2) {
                    continue; // Y cells and non-Y cells must have different values
                }

                // [What it does]: Operations = (Total Y cells - cells already having value v1) + 
                //                              (Total Non-Y cells - non-Y cells already having value v2)
                int operations = (totalYCells - yCounts[v1]) + (totalNonYCells - nonYCounts[v2]);

                minOperations = Math.min(minOperations, operations);
            }
        }

        return minOperations;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Geometric coordinate classification + Fixed-domain permutation search (6 pairs).
     * 2. Key observation: Since values are restricted to {0, 1, 2}, there are only 6 possible valid coloring configurations 
     *    for Y vs Non-Y.
     * 3. Memorize vs Understand: Understand the 3-part Y geometry condition: `(r <= center && (r == c || r + c == n - 1)) || (r >= center && c == center)`.
     * 4. Most common trap: Forgetting that Y cells and non-Y cells must be assigned *different* values (`v1 != v2`).
     * 5. Mental trigger: "Grid shape modification with few colors" -> Frequency counts + Test all valid color pair permutations.
     */
    public static void main(String[] args) {
        MinimumOperationsToWriteTheLetterY solution = new MinimumOperationsToWriteTheLetterY();

        // Test Case 1: Standard Example
        int[][] grid1 = {
            {1, 2, 2},
            {1, 1, 0},
            {0, 1, 0}
        };
        System.out.println("Test 1 (Standard): " + solution.minimumOperationsToWriteY(grid1)); // Expected: 3

        // Test Case 2: Another configuration
        int[][] grid2 = {
            {0, 1, 0, 1, 0},
            {2, 1, 0, 1, 2},
            {2, 2, 1, 2, 2},
            {2, 2, 1, 2, 2},
            {2, 2, 1, 2, 2}
        };
        System.out.println("Test 2 (5x5 Grid): " + solution.minimumOperationsToWriteY(grid2)); // Expected: 12
    }
}


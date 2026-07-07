import java.util.*;

/**
 * ============================================================================
 * MOCK GOOGLE INTERVIEW — "ROTATING THE BOX"
 * ============================================================================
 * This single file walks through the entire interview process end-to-end,
 * exactly as it should be presented in a real onsite/virtual Google interview.
 *
 * Problem source lineage: this is LeetCode 1861 "Rotating the Box".
 * ============================================================================
 */
public class RotatingTheBox {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM (in my own words)
     * ========================================================================
     *
     * We are given a 2D character grid `boxGrid` of size m rows x n columns.
     * Each cell is one of:
     *   '#'  -> a stone (movable under gravity)
     *   '*'  -> a fixed obstacle (never moves)
     *   '.'  -> empty space
     *
     * Physically, imagine this grid is a box viewed from the side. We rotate
     * the entire box 90 degrees CLOCKWISE. Because of this rotation, "down"
     * in the real world is now pointing toward what used to be the LAST
     * COLUMN of the original grid. Gravity then pulls every stone in that
     * new "down" direction until it hits the new floor, an obstacle, or
     * another stone that has already come to rest. Obstacles never move —
     * only stones respond to gravity. Horizontal drift during the rotation
     * itself is explicitly ignored ("no inertia") — the only motion we must
     * simulate is the *straight-line fall* after rotation.
     *
     * We must return the grid AFTER rotation and AFTER gravity settles,
     * which will have dimensions n rows x m columns (since rotating an
     * m x n grid 90 degrees produces an n x m grid).
     *
     * KEY GIVEN GUARANTEE: In the *input* grid, every stone is already
     * "resting" — i.e., directly below it (in the original orientation)
     * is either another stone, an obstacle, or the bottom of the box.
     * This means the input itself is a stable, physically valid pre-rotation
     * configuration — we don't need to validate it.
     *
     * INPUT:  char[][] boxGrid, dimensions m x n
     * OUTPUT: char[][] result,  dimensions n x m
     * ASSUMPTIONS: grid contains only '#', '*', '.' characters.
     */


    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (I'd ask these before coding)
     * ========================================================================
     *
     * Q1: What are the bounds on m and n?
     *     ASSUMED: 1 <= m, n <= 500 (typical LeetCode-style constraint;
     *     large enough that O(m*n) is required, O(m^2 * n^2) is not safe).
     *
     * Q2: Can the grid be empty, or a single row / single column?
     *     ASSUMED: Yes, m == 1 or n == 1 are valid inputs and must be handled.
     *
     * Q3: Is the character set guaranteed to be exactly {'#', '*', '.'},
     *     with no whitespace or other symbols?
     *     ASSUMED: Yes, guaranteed by problem statement.
     *
     * Q4: Are stones/obstacles counted or treated identically regardless of
     *     "type" (i.e., is every '#' interchangeable with every other '#')?
     *     ASSUMED: Yes — stones are indistinguishable from one another;
     *     we only care about *how many* stones occupy a segment, not which
     *     specific stone ends up where.
     *
     * Q5: Do we rotate exactly once (90 degrees), or could rotations be
     *     applied repeatedly / compounded in a follow-up?
     *     ASSUMED: Exactly one 90-degree clockwise rotation for this problem
     *     (follow-ups explore repeated rotation — see Section 12).
     *
     * Q6: Should the original input grid be mutated in place, or must we
     *     return a new grid and leave the input untouched?
     *     ASSUMED: Return a new grid; do not mutate the caller's reference
     *     (I will still show an in-place-friendly technique for efficiency,
     *     but copy defensively at the API boundary).
     *
     * Q7: Is there any concurrency requirement (e.g., multiple grids
     *     processed in parallel, thread safety)?
     *     ASSUMED: No — single-threaded, single-grid processing is sufficient
     *     (raised again as a follow-up).
     *
     * Q8: How should ties be handled when two stones "compete" for the same
     *     landing slot?
     *     ASSUMED: Not applicable — stones stack in fall order and never
     *     truly tie, since they are processed in a strict fall sequence
     *     (nearest obstacle/floor fills first). I'll demonstrate this in the
     *     boundary example in Section 3.
     */


    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * EXAMPLE 1 (Normal case):
     *   Input (1x3):
     *     # . #
     *   After rotating the box clockwise, "down" points toward the original
     *   last column. Simulating fall-to-the-right within the row first:
     *     . # #
     *   Then rotating 90 degrees clockwise (1x3 -> 3x1):
     *     .
     *     #
     *     #
     *
     * EXAMPLE 2 (Edge case — single empty row, no stones or obstacles):
     *   Input (1x4):
     *     . . . .
     *   No stones exist, so gravity has nothing to do. Rotating produces a
     *   4x1 grid of all empty cells:
     *     .
     *     .
     *     .
     *     .
     *
     * EXAMPLE 3 (Boundary / "tie-breaking" case — multiple stones stacked
     * against an obstacle in the middle of a row, plus a stone already
     * resting against the far wall):
     *   Input (1x7):
     *     # # * . # . #
     *   Walking right-to-left and letting stones fall rightwards, bounded by
     *   the obstacle at index 2 and the right wall at index 6:
     *     - Rightmost segment (indices 3..6): stones at 4 and 6 (two stones,
     *       one empty slot) settle fully against the right wall -> . . # #
     *       occupying indices 3,4,5,6 as: . . # #
     *     - Left segment (indices 0..1, bounded by obstacle at index 2):
     *       two stones settle fully against the obstacle -> # # (no room to
     *       move, they were already resting there).
     *   Full row after gravity: # # * . . # #
     *   This demonstrates that stones never "tie" for a slot — they fill
     *   greedily from the boundary (obstacle or wall) inward, in the exact
     *   order they are encountered, so there is no ambiguity to break.
     */


    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     * Paradigms considered but NOT included, with justification:
     *
     *  - Divide & Conquer: Rows are fully independent of one another and each
     *    row is solved with a single linear pass; there is no recursive
     *    subdivision that yields a better asymptotic result than the direct
     *    linear scan, so D&C adds complexity without benefit.
     *
     *  - Dynamic Programming: There are no overlapping subproblems or
     *    optimal-substructure optimization goals here (we are not optimizing
     *    a cost function) — this is a direct physical simulation, not an
     *    optimization problem, so DP is not applicable.
     *
     *  - Tree / Graph Traversal: There is no graph structure, adjacency, or
     *    connectivity question being asked; cells interact only along a
     *    single row in one direction, which is best modeled as a simple
     *    array scan, not a traversal problem.
     *
     *  - Heap / Priority Queue: There is no need to repeatedly extract a
     *    min/max element or maintain a dynamic priority ordering; the fall
     *    order is already fixed by grid position.
     *
     *  - Binary Search: There is no monotonic predicate over a sorted search
     *    space to exploit; landing positions are determined by direct
     *    scanning, not by search.
     *
     *  - Trie / Segment Tree: No prefix-matching or range-query workload
     *    exists here; we never need "sum/min/max over an arbitrary range"
     *    queries, so these advanced structures would be pure overhead.
     *
     *  - Hashing-based: There's no need to deduplicate, group, or look up by
     *    key — positions are already indexed by array coordinates, so a
     *    hash map would add overhead with zero benefit.
     *
     * Approaches that ARE meaningfully applicable are implemented below.
     */

    // ------------------------------------------------------------------------
    // APPROACH 1: Brute Force / Naive Repeated-Pass Simulation
    // ------------------------------------------------------------------------
    /*
     * CORE IDEA:
     * Physically simulate gravity the "dumb" way: repeatedly scan every row
     * left-to-right looking for a stone immediately followed by an empty
     * space to its right (a "#." pattern), and swap them. Keep repeating
     * full passes over the entire grid until an entire pass produces no
     * swaps (i.e., the configuration has stabilized). Finally rotate.
     *
     * DATA STRUCTURE / PARADIGM: Plain 2D array, iterative fixed-point
     * simulation (no auxiliary structure).
     *
     * TIME COMPLEXITY: O((m*n)^2) worst case — in the worst case (e.g. a
     * single stone must travel across an entire near-empty row), a full
     * O(m*n) pass is needed to move it just one step, and it may need to
     * move O(n) steps, repeated for every row -> O(m * n^2) just for
     * gravity, and each "pass" itself costs O(m*n), giving a quadratic-in-
     * grid-size blow-up in the worst adversarial layout.
     *
     * SPACE COMPLEXITY: O(n*m) for the output grid only; O(1) extra
     * auxiliary space during simulation.
     *
     * PROS:
     *   - Extremely intuitive; mirrors the physical process literally.
     *   - Very easy to reason about correctness (nothing clever going on).
     *
     * CONS:
     *   - Asymptotically far worse than necessary.
     *   - Repeated full-grid passes are wasteful and would likely time out
     *     on realistic constraints (m, n up to 500).
     *
     * WHEN TO USE: Only as a warm-up / sanity-check implementation, or when
     * explaining the problem to build intuition before optimizing. Never
     * ship this as the final answer in an interview or in production.
     */
    static char[][] rotateBoxBruteForce(char[][] boxGrid) {
        int rows = boxGrid.length;
        int cols = boxGrid[0].length;

        // Defensive copy so we never mutate the caller's array.
        char[][] grid = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            grid[r] = Arrays.copyOf(boxGrid[r], cols);
        }

        // Repeat full passes until a pass makes zero swaps (fixed point).
        boolean anySwapMade = true;
        while (anySwapMade) {
            anySwapMade = false;
            for (int r = 0; r < rows; r++) {
                // Scan left-to-right; a stone followed by empty space swaps.
                for (int c = 0; c < cols - 1; c++) {
                    if (grid[r][c] == '#' && grid[r][c + 1] == '.') {
                        grid[r][c] = '.';
                        grid[r][c + 1] = '#';
                        anySwapMade = true;
                    }
                }
            }
        }

        return rotateClockwise(grid);
    }


    // ------------------------------------------------------------------------
    // APPROACH 2: Greedy Segment Counting
    // ------------------------------------------------------------------------
    /*
     * CORE IDEA:
     * Within each row, obstacles ('*') break the row into independent
     * segments. Within a single segment, the *relative order does not
     * matter for stones* — only the COUNT of stones in that segment
     * matters, because they are indistinguishable. So for each segment we
     * simply count how many '#' characters it contains, then greedily
     * rewrite the segment so that all counted stones are pushed against the
     * segment's right boundary (obstacle or wall), and everything to the
     * left of them becomes '.'.
     *
     * DATA STRUCTURE / PARADIGM: Greedy reconstruction using simple
     * counters; no sorting needed because stones are fungible (this is why
     * a "sorting-based" approach is not meaningfully distinct here — sorting
     * identical elements is a no-op, so counting subsumes it).
     *
     * TIME COMPLEXITY: O(m*n) — every cell is visited a constant number of
     * times (once to count, once to rewrite).
     *
     * SPACE COMPLEXITY: O(1) extra space beyond the O(n*m) output grid.
     *
     * PROS:
     *   - Conceptually very clean: "count stones per segment, refill from
     *     the right."
     *   - Naturally generalizes to counting-based variants (e.g., if stones
     *     had weights or types that mattered, though not applicable here).
     *
     * CONS:
     *   - Requires a little bit more bookkeeping (tracking segment
     *     boundaries explicitly) than the pointer-based approach below.
     *   - Two logical passes per row (though still linear overall).
     *
     * WHEN TO USE: Great alternative if the interviewer asks "what if stones
     * had different types that need to preserve relative order" — then you
     * would need to *track* which stone goes where, and this two-pass
     * structure (count, then place) adapts more naturally than the
     * single-pass pointer trick.
     */
    static char[][] rotateBoxGreedyCounting(char[][] boxGrid) {
        int rows = boxGrid.length;
        int cols = boxGrid[0].length;
        char[][] grid = new char[rows][cols];

        for (int r = 0; r < rows; r++) {
            int segmentStart = 0; // inclusive start of current segment
            for (int c = 0; c <= cols; c++) {
                // A segment ends either at an obstacle or the row's end.
                boolean isBoundary = (c == cols) || (boxGrid[r][c] == '*');
                if (isBoundary) {
                    int stoneCount = 0;
                    for (int k = segmentStart; k < c; k++) {
                        if (boxGrid[r][k] == '#') {
                            stoneCount++;
                        }
                    }
                    // Fill the segment: empties first (left side), then
                    // stones pushed against the right boundary.
                    int segmentLength = c - segmentStart;
                    for (int k = 0; k < segmentLength - stoneCount; k++) {
                        grid[r][segmentStart + k] = '.';
                    }
                    for (int k = segmentLength - stoneCount; k < segmentLength; k++) {
                        grid[r][segmentStart + k] = '#';
                    }
                    if (c < cols) {
                        grid[r][c] = '*'; // place the obstacle itself
                    }
                    segmentStart = c + 1;
                }
            }
        }

        return rotateClockwise(grid);
    }


    // ------------------------------------------------------------------------
    // APPROACH 3 (OPTIMAL): Two-Pointer / Write-Pointer Compaction
    // ------------------------------------------------------------------------
    /*
     * CORE IDEA:
     * Scan each row from RIGHT to LEFT exactly once, maintaining a
     * "next available landing slot" write-pointer. When we see an obstacle,
     * the write-pointer resets to just left of it (obstacles are immovable
     * walls). When we see a stone, we immediately place it at the current
     * write-pointer position and decrement the pointer by one. When we see
     * empty space, we simply continue scanning (the write-pointer does NOT
     * move, since no stone has claimed that slot yet). After settling every
     * row this way, rotate the whole grid 90 degrees clockwise to produce
     * the final n x m answer.
     *
     * DATA STRUCTURE / PARADIGM: Classic two-pointer technique (a single
     * read-scan combined with a lagging write-pointer), the same family of
     * technique used in "move zeroes" / in-place array compaction problems.
     *
     * TIME COMPLEXITY: O(m*n) — a single right-to-left pass per row, then a
     * single O(m*n) rotation pass. Overall O(m*n), which is optimal since
     * every cell must be read at least once.
     *
     * SPACE COMPLEXITY: O(1) extra space if done in-place on a working copy
     * of the grid (excluding the required O(n*m) output).
     *
     * PROS:
     *   - Optimal time complexity, single pass per row.
     *   - Extremely fast to code correctly under interview time pressure.
     *   - In-place friendly (mutates a scratch copy directly).
     *
     * CONS:
     *   - Slightly less obvious at first glance than the counting approach
     *     (the "reset write-pointer at obstacles" trick has to be seen once
     *     to become intuitive).
     *
     * WHEN TO USE: This is the approach I would write in a real interview —
     * it is simplest to implement correctly, asymptotically optimal, and
     * requires no auxiliary counting pass.
     */
    static char[][] rotateBoxTwoPointer(char[][] boxGrid) {
        int rows = boxGrid.length;
        int cols = boxGrid[0].length;

        // Work on a defensive copy; we mutate this copy in place.
        char[][] grid = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            grid[r] = Arrays.copyOf(boxGrid[r], cols);
        }

        for (int r = 0; r < rows; r++) {
            // writePointer = the rightmost column index still available
            // for the next stone we encounter while scanning right-to-left.
            int writePointer = cols - 1;

            for (int c = cols - 1; c >= 0; c--) {
                char cell = grid[r][c];
                if (cell == '*') {
                    // Obstacles are immovable; the next available slot is
                    // now strictly to the left of this obstacle.
                    writePointer = c - 1;
                } else if (cell == '#') {
                    // A stone falls all the way to writePointer.
                    // Clear the original spot first (safe even if
                    // c == writePointer, since we overwrite it right after).
                    grid[r][c] = '.';
                    grid[r][writePointer] = '#';
                    writePointer--;
                }
                // If cell == '.', do nothing: no stone has claimed this
                // slot yet, so the write-pointer stays where it is.
            }
        }

        return rotateClockwise(grid);
    }


    // ------------------------------------------------------------------------
    // APPROACH 4: Monotonic-Stack-Style Simulation (alternate implementation)
    // ------------------------------------------------------------------------
    /*
     * CORE IDEA:
     * Functionally equivalent to Approach 3, but expressed using an explicit
     * stack to track "pending stones waiting to be placed" as we scan
     * left-to-right instead of maintaining a single integer pointer scanning
     * right-to-left. Whenever we hit an obstacle or the row's right wall, we
     * pop all pending stones off the stack and place them immediately to the
     * left of that boundary, filling right-to-left.
     *
     * DATA STRUCTURE / PARADIGM: Explicit stack (LIFO), conceptually in the
     * same family as monotonic-stack problems, though here the stack simply
     * accumulates "how many stones are pending" rather than enforcing a
     * monotonic ordering invariant.
     *
     * TIME COMPLEXITY: O(m*n) — every cell is pushed/considered once and
     * popped at most once.
     *
     * SPACE COMPLEXITY: O(n) auxiliary space per row for the stack in the
     * worst case (an entire row of stones), in addition to O(n*m) output.
     *
     * PROS:
     *   - Useful mental model if the interviewer later asks a variant where
     *     stones have distinguishing properties and must be processed in a
     *     strict FIFO/LIFO order relative to one another.
     *
     * CONS:
     *   - Uses O(n) extra space per row for the stack, which the two-pointer
     *     approach avoids entirely — strictly dominated by Approach 3 for
     *     this exact problem.
     *
     * WHEN TO USE: Mostly pedagogical here; prefer Approach 3 unless a
     * follow-up variant genuinely requires explicit ordered bookkeeping.
     */
    static char[][] rotateBoxStackBased(char[][] boxGrid) {
        int rows = boxGrid.length;
        int cols = boxGrid[0].length;
        char[][] grid = new char[rows][cols];

        for (int r = 0; r < rows; r++) {
            Deque<Integer> pendingStones = new ArrayDeque<>(); // stack of stone markers

            for (int c = 0; c < cols; c++) {
                char cell = boxGrid[r][c];
                if (cell == '#') {
                    pendingStones.push(1); // remember "one stone is pending"
                } else if (cell == '*') {
                    // Flush any pending stones so they land just left of
                    // this obstacle (rightmost pending slot first).
                    int landingSpot = c - 1;
                    while (!pendingStones.isEmpty()) {
                        pendingStones.pop();
                        grid[r][landingSpot] = '#';
                        landingSpot--;
                    }
                    // Anything left between the flush point and here is empty.
                    for (int k = landingSpot; k > (c - 1 - countStonesUpTo(boxGrid, r, c)); k--) {
                        // no-op guard; real filling handled below for clarity
                    }
                    grid[r][c] = '*';
                }
            }
            // Flush remaining pending stones against the right wall.
            int landingSpot = cols - 1;
            while (!pendingStones.isEmpty()) {
                pendingStones.pop();
                grid[r][landingSpot] = '#';
                landingSpot--;
            }
            // Fill any untouched cells (those left as '.') explicitly.
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != '#' && grid[r][c] != '*') {
                    grid[r][c] = '.';
                }
            }
        }

        return rotateClockwise(grid);
    }

    // Small helper used only by the stack-based approach for clarity of the
    // (intentionally slightly verbose) flushing logic above.
    private static int countStonesUpTo(char[][] boxGrid, int row, int endExclusive) {
        int count = 0;
        for (int k = 0; k < endExclusive; k++) {
            if (boxGrid[row][k] == '#') {
                count++;
            }
        }
        return count;
    }


    /* ========================================================================
     * SHARED HELPER: 90-degree clockwise rotation of an m x n grid -> n x m
     * ========================================================================
     * newGrid[j][m - 1 - i] = oldGrid[i][j]
     * This is used identically by every approach above, since gravity is
     * simulated in the ORIGINAL orientation first (falling toward increasing
     * column index == the new "down" direction), and rotation is applied
     * last as a pure geometric relabeling.
     */
    private static char[][] rotateClockwise(char[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        char[][] rotated = new char[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotated[j][rows - 1 - i] = grid[i][j];
            }
        }
        return rotated;
    }


    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                          | Time         | Space          | Best For                              | Limitations                                   |
     * |------------------------------------|--------------|----------------|---------------------------------------|------------------------------------------------|
     * | 1. Brute Force Repeated Passes     | O((m*n)^2)   | O(1) extra     | Building intuition / warm-up          | Far too slow for real constraints              |
     * | 2. Greedy Segment Counting         | O(m*n)       | O(1) extra     | Variants needing per-segment metadata | Slightly more bookkeeping than Approach 3      |
     * | 3. Two-Pointer Compaction (OPTIMAL)| O(m*n)       | O(1) extra     | The actual interview answer           | None significant for this exact problem        |
     * | 4. Monotonic-Stack-Style           | O(m*n)       | O(n) per row   | Variants needing explicit ordering    | Extra space vs. Approach 3, more code           |
     */


    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ========================================================================
     * I would present APPROACH 3 (Two-Pointer / Write-Pointer Compaction).
     *
     * WHY:
     *   - It achieves the optimal O(m*n) time / O(1) extra space bound —
     *     nothing asymptotically better is possible since every cell must
     *     be examined at least once.
     *   - It is the FASTEST to code correctly under interview time pressure:
     *     a single right-to-left scan per row with one integer pointer,
     *     no auxiliary data structures, no second pass for counting.
     *   - It directly mirrors how an interviewer expects a "gravity
     *     simulation" problem to be solved: recognize that gravity direction
     *     maps to a simple compaction problem (same family as "move zeroes
     *     to the end"), then bolt on a generic rotation helper.
     *   - It is easy to verbally narrate while coding, which matters a lot
     *     in a live interview setting.
     */


    /* ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (production-quality)
     * ========================================================================
     * This is a polished, fully-commented restatement of Approach 3, written
     * as the version I would actually submit as my final interview answer.
     */
    static char[][] rotateTheBox(char[][] boxGrid) {
        // --- Input validation guard clauses (good interview hygiene) ---
        if (boxGrid == null || boxGrid.length == 0 || boxGrid[0].length == 0) {
            throw new IllegalArgumentException("boxGrid must be non-null and non-empty");
        }

        int originalRowCount = boxGrid.length;
        int originalColCount = boxGrid[0].length;

        // Defensive copy: never mutate the caller's input array. We do all
        // gravity simulation on this working copy.
        char[][] settledGrid = new char[originalRowCount][originalColCount];
        for (int row = 0; row < originalRowCount; row++) {
            if (boxGrid[row].length != originalColCount) {
                throw new IllegalArgumentException("boxGrid rows must have uniform length");
            }
            settledGrid[row] = Arrays.copyOf(boxGrid[row], originalColCount);
        }

        // --- Step 1: Simulate gravity toward increasing column index. ---
        // Rationale: after a 90-degree CLOCKWISE rotation, the box's new
        // "floor" corresponds to what was originally the LAST column, so
        // stones must be pushed rightward within each original row BEFORE
        // we perform the geometric rotation.
        for (int row = 0; row < originalRowCount; row++) {
            // nextLandingSlot tracks the rightmost column still free for the
            // next stone we encounter scanning from the right wall inward.
            int nextLandingSlot = originalColCount - 1;

            for (int col = originalColCount - 1; col >= 0; col--) {
                char currentCell = settledGrid[row][col];

                if (currentCell == '*') {
                    // Obstacles are immovable "sub-floors": any stone found
                    // further left can fall no further than just left of
                    // this obstacle.
                    nextLandingSlot = col - 1;

                } else if (currentCell == '#') {
                    // This stone falls to the next available slot. Clear its
                    // old position first; this is safe even when
                    // col == nextLandingSlot because we immediately rewrite
                    // that same index to '#' on the next line.
                    settledGrid[row][col] = '.';
                    settledGrid[row][nextLandingSlot] = '#';
                    nextLandingSlot--;
                }
                // currentCell == '.' requires no action: empty space simply
                // gets "backfilled" once a stone from further left claims
                // this slot, or remains empty if none does.
            }
        }

        // --- Step 2: Perform the actual 90-degree clockwise rotation. ---
        // Mapping: rotated[col][originalRowCount - 1 - row] = settledGrid[row][col]
        char[][] rotatedResult = new char[originalColCount][originalRowCount];
        for (int row = 0; row < originalRowCount; row++) {
            for (int col = 0; col < originalColCount; col++) {
                rotatedResult[col][originalRowCount - 1 - row] = settledGrid[row][col];
            }
        }

        return rotatedResult;
    }


    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE (using Example 3 from Section 3)
     * ========================================================================
     * Input row (1x7): # # * . # . #
     * Indices:          0 1 2 3 4 5 6
     *
     * STEP 1 — Gravity pass, scanning col from 6 down to 0:
     *
     *   nextLandingSlot starts at 6 (originalColCount - 1 = 6)
     *
     *   col=6, cell='#'  -> clear[6]='.', place at slot 6 -> row[6]='#'
     *                       nextLandingSlot becomes 5
     *                       row so far: # # * . # . #   (col6 rewritten same value)
     *   col=5, cell='.'  -> no action. nextLandingSlot stays 5.
     *   col=4, cell='#'  -> clear[4]='.', place at slot 5 -> row[5]='#'
     *                       nextLandingSlot becomes 4
     *                       row so far: # # * . . # #
     *   col=3, cell='.'  -> no action. nextLandingSlot stays 4.
     *   col=2, cell='*'  -> obstacle: nextLandingSlot resets to col-1 = 1
     *                       row so far unchanged: # # * . . # #
     *   col=1, cell='#'  -> clear[1]='.', place at slot 1 -> row[1]='#'
     *                       nextLandingSlot becomes 0
     *                       row so far: # . * . . # #  (temporarily, since
     *                       we cleared index1 then rewrote index1 -> same
     *                       net effect, value unchanged: '#')
     *   col=0, cell='#'  -> clear[0]='.', place at slot 0 -> row[0]='#'
     *                       nextLandingSlot becomes -1
     *                       row so far: # # * . . # #
     *
     *   Final settled row: # # * . . # #   <-- matches Section 3's prediction
     *
     * STEP 2 — Rotate 90 degrees clockwise (1x7 -> 7x1):
     *   rotated[col][originalRowCount-1-row] = settledGrid[row][col]
     *   Since originalRowCount = 1, row = 0 always, so
     *   rotated[col][0] = settledGrid[0][col] for col = 0..6
     *
     *   rotated[0][0] = '#'
     *   rotated[1][0] = '#'
     *   rotated[2][0] = '*'
     *   rotated[3][0] = '.'
     *   rotated[4][0] = '.'
     *   rotated[5][0] = '#'
     *   rotated[6][0] = '#'
     *
     * FINAL OUTPUT (7x1):
     *   #
     *   #
     *   *
     *   .
     *   .
     *   #
     *   #
     */


    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - All four approaches produce identical, correct output; they differ
     *   only in time/space efficiency and code shape.
     * - Approach 1 (naive repeated passes) is correct but asymptotically
     *   unacceptable — O((m*n)^2) — and exists purely as a warm-up mental
     *   model.
     * - Approach 2 (greedy segment counting) and Approach 4 (stack-based)
     *   both achieve O(m*n) time but carry either extra bookkeeping or
     *   extra O(n) space per row versus the two-pointer method.
     * - Approach 3 (two-pointer compaction) is the optimal and recommended
     *   solution: O(m*n) time, O(1) extra space, one pass per row.
     * - KEY ASSUMPTION carried through every approach: the input is
     *   guaranteed already-stable in its original orientation (every stone
     *   already rests on something), so we never need to validate physical
     *   consistency of the input itself — only simulate the NEW gravity
     *   direction after rotation.
     * - KNOWN LIMITATION: all approaches here assume a rectangular
     *   (non-jagged) grid; the production-quality solution in Section 9
     *   explicitly guards against jagged input rows and throws for invalid
     *   input, but the naive/greedy/stack versions omit this for brevity.
     */


    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "What if the box is rotated 90 degrees clockwise K times in a row —
     *     can you avoid re-simulating gravity from scratch each time?"
     *     (Tests whether you notice rotating 4 times returns to the
     *     original orientation, and whether gravity settling is idempotent
     *     after the first application in a given orientation.)
     *
     * 2. "What if the grid is extremely large (say, 10^5 x 10^5) and must be
     *     streamed row-by-row rather than held fully in memory?"
     *     (Tests whether you recognize each row is independent, enabling a
     *     streaming/row-at-a-time solution with O(n) memory instead of
     *     O(m*n).)
     *
     * 3. "What if stones have different weights, and heavier stones must
     *     end up below lighter ones within the same segment after
     *     falling?"
     *     (Tests adaptability — this breaks the "stones are fungible"
     *     assumption behind Approaches 2 and 3, and would likely need a
     *     stable sort or the stack-based approach adapted to preserve
     *     weight order.)
     *
     * 4. "Can you solve this with O(1) additional memory, truly in-place,
     *     without allocating any new 2D array at all (not even for the
     *     rotated output)?"
     *     (Tests understanding that in-place rotation of a non-square
     *     matrix, m != n, is fundamentally impossible without allocating a
     *     new array, unlike square-matrix in-place rotation.)
     *
     * 5. "How would you unit test this function comprehensively?"
     *     (Tests engineering maturity: expect answers covering empty grids,
     *     single row/column, no stones, no obstacles, all obstacles,
     *     obstacles adjacent to walls, and large random grids compared
     *     against the brute-force approach as an oracle.)
     *
     * 6. "What if concurrent threads need to rotate many independent boxes
     *     simultaneously — what would you change?"
     *     (Tests concurrency awareness: since each grid/each row is
     *     independent, this parallelizes trivially — e.g., process rows in
     *     parallel via a fork-join pool or stream parallelism, with no
     *     shared mutable state between rows.)
     */


    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. FORGETTING THE ORDER OF OPERATIONS: Many candidates try to rotate
     *    FIRST and then simulate gravity in the rotated grid by scanning
     *    columns top-to-bottom. This is *equivalent* but noticeably harder
     *    to get right (column-major scanning is more error-prone in Java
     *    than row-major), and it's easy to rotate in the wrong direction
     *    (counter-clockwise vs clockwise) when reasoning about the rotated
     *    grid directly. Doing gravity in the ORIGINAL orientation first
     *    (fall right-to-left scan) and rotating LAST is simpler and safer.
     *
     * 2. OFF-BY-ONE ON THE ROTATION FORMULA: The clockwise rotation mapping
     *    is rotated[col][rows - 1 - row] = original[row][col]. Candidates
     *    frequently swap row/col or forget the "rows - 1 -" term, silently
     *    producing a counter-clockwise or vertically-flipped result that
     *    only manifests as a bug on non-square grids (a square-grid test
     *    case can accidentally still "look right").
     *
     * 3. RESETTING THE WRITE-POINTER INCORRECTLY AT OBSTACLES: A very
     *    common bug is setting nextLandingSlot = col instead of col - 1
     *    when an obstacle is encountered — this incorrectly allows a stone
     *    to be placed ON TOP of (i.e., overwriting) the obstacle itself.
     *
     * 4. ASSUMING STONES CARE ABOUT IDENTITY/ORDER: Because stones are
     *    visually distinct characters at different starting positions, some
     *    candidates over-engineer a solution that explicitly tracks "which"
     *    stone moved where (e.g., sorting stone indices, tagging them).
     *    Since stones are fungible ('#' is indistinguishable from '#'),
     *    only the COUNT per segment matters — recognizing this early is
     *    what unlocks the simple, optimal one-pass solution.
     */


    /* ========================================================================
     * MAIN — Demonstration / self-test harness
     * ========================================================================
     */
    public static void main(String[] args) {
        // Example 1: Normal case
        char[][] example1 = { {'#', '.', '#'} };
        printGrid("Example 1 - Input", example1);
        printGrid("Example 1 - Output (optimal)", rotateTheBox(example1));

        // Cross-check all four approaches agree on Example 1.
        assertGridsEqual(rotateTheBox(example1), rotateBoxBruteForce(example1));
        assertGridsEqual(rotateTheBox(example1), rotateBoxGreedyCounting(example1));
        assertGridsEqual(rotateTheBox(example1), rotateBoxTwoPointer(example1));
        assertGridsEqual(rotateTheBox(example1), rotateBoxStackBased(example1));

        // Example 2: Edge case - all empty
        char[][] example2 = { {'.', '.', '.', '.'} };
        printGrid("Example 2 - Input", example2);
        printGrid("Example 2 - Output (optimal)", rotateTheBox(example2));

        // Example 3: Boundary / stacking case
        char[][] example3 = { {'#', '#', '*', '.', '#', '.', '#'} };
        printGrid("Example 3 - Input", example3);
        printGrid("Example 3 - Output (optimal)", rotateTheBox(example3));

        assertGridsEqual(rotateTheBox(example3), rotateBoxBruteForce(example3));
        assertGridsEqual(rotateTheBox(example3), rotateBoxGreedyCounting(example3));
        assertGridsEqual(rotateTheBox(example3), rotateBoxTwoPointer(example3));
        assertGridsEqual(rotateTheBox(example3), rotateBoxStackBased(example3));

        // Multi-row example for good measure.
        char[][] multiRow = {
            {'#', '#', '*', '.', '*', '.', '.'},
            {'#', '.', '#', '#', '.', '.', '#'},
            {'.', '#', '.', '#', '.', '#', '#'}
        };
        printGrid("Multi-row - Input", multiRow);
        printGrid("Multi-row - Output (optimal)", rotateTheBox(multiRow));

        assertGridsEqual(rotateTheBox(multiRow), rotateBoxBruteForce(multiRow));
        assertGridsEqual(rotateTheBox(multiRow), rotateBoxGreedyCounting(multiRow));
        assertGridsEqual(rotateTheBox(multiRow), rotateBoxTwoPointer(multiRow));
        assertGridsEqual(rotateTheBox(multiRow), rotateBoxStackBased(multiRow));

        System.out.println("All approaches agree on all test cases. ✔");
    }

    // --- Test utility helpers ---

    private static void printGrid(String label, char[][] grid) {
        System.out.println(label + ":");
        for (char[] row : grid) {
            StringBuilder rowBuilder = new StringBuilder();
            for (char cell : row) {
                rowBuilder.append(cell).append(' ');
            }
            System.out.println("  " + rowBuilder.toString().trim());
        }
        System.out.println();
    }

    private static void assertGridsEqual(char[][] expected, char[][] actual) {
        if (expected.length != actual.length) {
            throw new AssertionError("Row count mismatch");
        }
        for (int i = 0; i < expected.length; i++) {
            if (!Arrays.equals(expected[i], actual[i])) {
                throw new AssertionError(
                    "Mismatch at row " + i +
                    ": expected=" + Arrays.toString(expected[i]) +
                    " actual=" + Arrays.toString(actual[i])
                );
            }
        }
    }
}

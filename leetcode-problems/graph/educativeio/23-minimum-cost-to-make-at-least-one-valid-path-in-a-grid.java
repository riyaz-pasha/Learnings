/**
 * ============================================================================
 * MINIMUM COST TO MAKE AT LEAST ONE VALID PATH IN A GRID - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Are the edge weights exactly and strictly 0 and 1?
 *    -> Why: This defines our algorithm choice. If weights are only 0 and 1, we can use 0-1 BFS instead of a heavy PriorityQueue.
 * Q: Can the original grid contain signs pointing completely out of bounds (e.g., an 'Up' sign at row 0)?
 *    -> Why: Prevents out-of-bounds exceptions when following the "0-cost" natural path. (Rule: Yes, they can).
 * Q: Can we visit the same cell multiple times if we find a cheaper path to it later?
 *    -> Why: Dictates how we track the "visited" state. A standard boolean array isn't enough; we need a cost array to allow cheaper re-entries.
 * Q: Will the grid always be at least 1x1?
 *    -> Why: Establishes our base case `(m == 1 && n == 1)`. 
 *
 *
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need to find a path from top-left to bottom-right. Following the grid's arrow 
 * costs 0. Going against the arrow costs 1. We are minimizing total cost. The binding constraint is 
 * avoiding the O(V log V) overhead of Dijkstra's algorithm on a dense grid where weights are extremely constrained.
 *
 * Approach 1: Dijkstra's Algorithm (Priority Queue)
 * - What I'd naturally try: Model the grid as a graph. Edges following the arrow have weight 0, other valid 
 *   adjacent cells have weight 1. Push `(0, 0, cost=0)` into a `PriorityQueue`. Always pop the cell with the 
 *   lowest current cost, evaluate its neighbors, and push them to the PQ.
 * - Why it works: Dijkstra is the standard algorithm for Single-Source Shortest Path (SSSP) with non-negative weights.
 * - Why it's suboptimal: A `PriorityQueue` takes O(log N) time to insert/extract. In a grid of size M*N, this 
 *   adds logarithmic overhead to every single move.
 * - Time Complexity: O(M * N * log(M * N)) — because we process M*N cells, and heap operations take log(V).
 * - Space Complexity: O(M * N) — because the `PriorityQueue` and `cost` tracking array scale with grid size.
 *
 * Approach 2: 0-1 BFS with a Deque (The Optimum)
 * - What property removes the bottleneck: The edge weights are STRICTLY 0 or 1. If we are at cost `C`, the only 
 *   possible next costs are `C` or `C + 1`. We don't need a heavy PriorityQueue to sort values like `C + 50`. 
 *   We just need a Double-Ended Queue (`Deque`). 
 * - What I'd try: Push `(0,0)` to a Deque. When evaluating neighbors:
 *   - If the move costs 0 (following the arrow), push it to the FRONT of the deque.
 *   - If the move costs 1 (changing the arrow), push it to the BACK of the deque.
 *   This beautifully guarantees that the deque is ALWAYS sorted by cost. We naturally process all 0-cost reachable 
 *   cells before touching the 1-cost cells.
 * - Time Complexity: O(M * N) — because each cell is processed and updated at most a constant number of times. 
 *   Pushing/popping from a Deque is strictly O(1).
 * - Space Complexity: O(M * N) — because we store a `cost` array of size M*N and the Deque holds at most O(M*N) elements.
 * - Decision: Approach 2 (0-1 BFS) is the undisputed champion here. It demonstrates mastery of advanced graph 
 *   traversal techniques specifically tuned to the constraints of the problem.
 *
 *
 * 3. EDGE CASES
 * ------------------------------------------------
 * - Grid is 1x1: The start is the target. Requires 0 moves/changes. Return 0 instantly.
 * - Perfect Path: The arrows natively guide you from start to finish. Returns 0.
 * - Spiral/Serpentine trap: A long winding 0-cost path exists, but a 1-cost shortcut exists. 0-1 BFS correctly 
 *   takes the long winding 0-cost path because 0-cost moves are always bumped to the front of the queue!
 * - Arrows pointing off the board: Evaluated safely. We calculate the theoretical out-of-bounds coordinate, 
 *   the bounds check rejects it, and we proceed to check the 1-cost valid neighbors.
 *
 *
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight: "The 0-1 BFS Priority Illusion"
 * By adding 0-cost moves to the front of the queue, you are effectively teleporting to the end of a free path. 
 * It ensures you explore every single cell you can reach for "free" before you spend 1 dollar to break a wall.
 *
 * ASCII Diagram (The 0-cost Teleportation):
 * Grid: 
 * [Right(1), Right(1), Down(3)]
 * [Left(2),  Left(2),  Right(1)]
 * 
 * If we pop (0,0), we push (0,1) to the FRONT (cost 0) and (1,0) to the BACK (cost 1).
 * Deque: [(0,1|c=0), (1,0|c=1)]
 * We immediately process (0,1), which pushes (0,2) to the FRONT.
 * Deque: [(0,2|c=0), (1,0|c=1)]
 * We've explored the whole top row without ever touching the 1-cost move at the back!
 *
 * Dry Run:
 * Grid 2x2:
 * [1, 1]  (R, R) -> (0,1) points R (off board).
 * [2, 2]  (L, L) -> Target is (1,1).
 * 
 * Init: Deque = [(0,0)], cost = [[0, ∞], [∞, ∞]]
 * 1. Pop (0,0). Cost = 0.
 *    - Dir 1 (R): Matches grid. Cost 0. Next (0,1). cost[0][1]=0. Add FRONT. 
 *    - Dir 2 (L): Out of bounds.
 *    - Dir 3 (D): Cost 1. Next (1,0). cost[1][0]=1. Add BACK.
 *    - Dir 4 (U): Out of bounds.
 *    Deque: [(0,1), (1,0)]
 * 2. Pop (0,1). Cost = 0.
 *    - Dir 1 (R): Out of bounds.
 *    - Dir 2 (L): Cost 1. Next (0,0). cost[0][0] is 0. 1 < 0 is False. Ignore.
 *    - Dir 3 (D): Cost 1. Next (1,1). cost[1][1]=1. Add BACK.
 *    - Dir 4 (U): Out of bounds.
 *    Deque: [(1,0), (1,1)]
 * 3. Pop (1,0). Cost = 1.
 *    - All directions evaluated. No cheaper paths found.
 * 4. Pop (1,1). Cost = 1. TARGET REACHED! (Wait, cost to (1,1) is 1. Correct).
 *
 * Pitfalls:
 * - Marking a cell as "visited" with a boolean array upon popping. Since we can reach a cell via a 5-cost path 
 *   and later via a 2-cost path, we MUST use a `cost[][]` integer array and only enqueue if `new_cost < cost[r][c]`.
 * - Forgetting that the problem is 1-indexed for directions (1=R, 2=L, 3=D, 4=U) but arrays are 0-indexed.
 *
 * Pattern Recognition: 
 * - When you see: "Grid or Graph with edge weights strictly 0 and 1."
 * - Think: 0-1 BFS using a Deque.
 *
 *
 * 5. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if the modification cost was variable (e.g., changing to Left costs 2, changing to Down costs 5)?
 *  -> 0-1 BFS breaks because weights are no longer just 0 and 1. We must revert to Dijkstra's Algorithm with a PriorityQueue.
 * 
 * F2: What if we have a maximum budget `K` and want to find the shortest path in terms of steps?
 *  -> The graph state expands to 3D: `(row, col, budget)`. We use a standard BFS where we only queue valid neighbors 
 *     if they don't exceed `K` modifications, and we return the first path to hit the target.
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class MinCostValidPath {

    /**
     * OPTIMAL APPROACH: 0-1 Breadth-First Search
     * Time Complexity: O(M * N) — Every cell is added to the Deque at most a few times, and operations are O(1).
     * Space Complexity: O(M * N) — For the minCost tracking array and the Deque.
     */
    public int minCost(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        
        // Edge Case: 1x1 grid requires no movement.
        if (m == 1 && n == 1) {
            return 0;
        }
        
        // Map directions exactly as stated in the problem description:
        // Index 0 -> 1: Right (0, 1)
        // Index 1 -> 2: Left (0, -1)
        // Index 2 -> 3: Down (1, 0)
        // Index 3 -> 4: Up (-1, 0)
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        
        // minCost[r][c] keeps track of the absolute minimum cost found so far to reach cell (r, c).
        // Initialized to infinity (or a sufficiently large number like Integer.MAX_VALUE).
        // e.g., On a 2x2 grid, minCost will start as [[MAX, MAX], [MAX, MAX]]
        int[][] minCost = new int[m][n];
        for (int[] row : minCost) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }
        
        // Deque stores the coordinates we need to process.
        // We use an int[] array where index 0 is row, index 1 is col.
        Deque<int[]> deque = new ArrayDeque<>();
        
        // Start at top-left corner
        deque.offerFirst(new int[]{0, 0});
        minCost[0][0] = 0;
        
        while (!deque.isEmpty()) {
            int[] current = deque.pollFirst();
            int r = current[0];
            int c = current[1];
            
            // Try all 4 possible directions
            for (int d = 0; d < 4; d++) {
                int nextR = r + dirs[d][0];
                int nextC = c + dirs[d][1];
                
                // Bounds check: if it points off the grid, we simply ignore this path
                if (nextR >= 0 && nextR < m && nextC >= 0 && nextC < n) {
                    
                    // The cost is 0 if the grid's sign (which is 1-indexed) matches our direction index (d + 1).
                    // Otherwise, we have to pay 1 to change the sign.
                    int costToMove = (grid[r][c] == d + 1) ? 0 : 1;
                    int newCost = minCost[r][c] + costToMove;
                    
                    // If we found a strictly cheaper way to reach the neighboring cell...
                    if (newCost < minCost[nextR][nextC]) {
                        minCost[nextR][nextC] = newCost;
                        
                        // THE 0-1 BFS MAGIC:
                        // If it's a free move, teleport it to the FRONT of the queue to evaluate it immediately.
                        // If it costs 1, put it at the BACK so we evaluate it only after exhausting all free moves.
                        if (costToMove == 0) {
                            deque.offerFirst(new int[]{nextR, nextC});
                        } else {
                            deque.offerLast(new int[]{nextR, nextC});
                        }
                    }
                }
            }
        }
        
        return minCost[m - 1][n - 1];
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        MinCostValidPath solver = new MinCostValidPath();

        System.out.println("--- Testing Min Cost to Make At Least One Valid Path ---");

        // Test Case 1: Standard snake path requiring 3 modifications
        // Grid:
        // [1, 1, 1, 1]
        // [2, 2, 2, 2]
        // [1, 1, 1, 1]
        // [2, 2, 2, 2]
        int[][] grid1 = {
            {1, 1, 1, 1},
            {2, 2, 2, 2},
            {1, 1, 1, 1},
            {2, 2, 2, 2}
        };
        // Expected: 3 (Modify end of row 0 to Down, end of row 1 to Down, end of row 2 to Down)
        System.out.println("Test Case 1 (Expected 3): " + solver.minCost(grid1));

        // Test Case 2: Perfect path exists naturally
        // [1, 1, 3]
        // [3, 2, 2]
        // [1, 1, 4] -> Target at (2,2)
        int[][] grid2 = {
            {1, 1, 3},
            {3, 2, 2},
            {1, 1, 4}
        };
        // Expected: 0 (The path naturally winds to the end)
        System.out.println("Test Case 2 (Expected 0): " + solver.minCost(grid2));

        // Test Case 3: 1x1 grid (Base case)
        int[][] grid3 = {{1}};
        // Expected: 0
        System.out.println("Test Case 3 (Expected 0): " + solver.minCost(grid3));

        // Test Case 4: The 0-cost Teleportation Trap
        // [1, 1]
        // [2, 2]
        int[][] grid4 = {
            {1, 1},
            {2, 2}
        };
        // Expected: 1 (Modify (0,1) to Down)
        System.out.println("Test Case 4 (Expected 1): " + solver.minCost(grid4));
    }
}

/**
 * ============================================================================
 * SNAKES AND LADDERS - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: If a ladder takes me to a cell that has another ladder/snake, do I take that one too?
 *    -> Why: Prevents infinite loops. (Rule: No, only one jump per move).
 * Q: Can I choose NOT to take a snake or ladder to set up a better roll next time?
 *    -> Why: Clarifies rules. (Rule: No, forced moves).
 * Q: Do I need an exact roll to land on the final square (n^2)? 
 *    -> Why: Affects the bounds check of our dice roll loop. (Rule: No, just a roll that gets you there).
 * Q: Are squares 1 and n^2 guaranteed NOT to be the start of a snake or ladder?
 *    -> Why: Prevents edge cases right at the start or end of the game.
 * Q: How is the board represented? Is board[row][col] the destination square number or an offset?
 *    -> Why: Clarifies how to parse the matrix. (Rule: It's the exact 1-indexed destination square).
 *
 * 
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The core challenge: We are finding the shortest path in an unweighted directed graph, where nodes 
 * are squares (1 to n^2), edges are dice rolls (1-6), and the 2D matrix introduces a complex 
 * "boustrophedon" (ox-plowing) coordinate mapping. The binding constraint is reaching the end in 
 * the MINIMUM moves, which dictates our algorithm choice.
 * 
 * Approach 1: The Brute Force (DFS / Backtracking)
 * - What I'd try: Start at square 1. Recursively try rolling 1, 2, 3, 4, 5, 6. If I land on a ladder/snake, 
 *   take it. Keep track of the minimum rolls to reach n^2.
 * - Why it works: It exhaustively explores every possible sequence of dice rolls.
 * - Why it's too slow: The graph has cycles (snakes going backwards). To avoid infinite loops, we'd need a 
 *   'visited' set, but DFS on an unweighted graph with visited sets struggles to find the *shortest* path 
 *   without exploring massive numbers of sub-optimal paths. Time complexity becomes O(6^(N^2)) in the worst 
 *   case of revisiting nodes with different path lengths. Space is O(N^2) for the recursion stack.
 * - What work is repeated: We might reach square 15 in 10 moves, explore its branches, then later reach 
 *   square 15 in 3 moves, and have to re-explore all its branches with the new shorter path context.
 * 
 * Approach 2: Dijkstra's Algorithm
 * - What I'd try: Treat it as a shortest path problem. Use a PriorityQueue ordered by `moves`.
 * - Why it works: Dijkstra's guarantees the shortest path. 
 * - Why it's overkill: Every edge (dice roll) has the exact same weight (1 move). Dijkstra's adds an O(log V) 
 *   overhead to every insertion/extraction via the PriorityQueue. 
 * - Time: O(V + E log V) -> O(N^2 + 6*N^2 * log(N^2)). Space: O(N^2).
 * 
 * Approach 3: Level-by-Level Breadth-First Search (The Optimum)
 * - Observation: Since every edge costs exactly 1 move, the *first* time we reach a node in a breadth-first 
 *   search, we are guaranteed to have found the absolute shortest path to it.
 * - What I'd try: Use a standard FIFO Queue. Push square 1. Process all reachable squares in "Wave 1", 
 *   then "Wave 2". The moment we dequeue n^2 (or enqueue it), we return the wave number.
 * - Time: O(N^2). We visit each of the N^2 squares at most once. For each, we check up to 6 next moves.
 * - Space: O(N^2). The Queue and the `visited` array both scale linearly with the total number of squares.
 * - Decision: This is what I'd write in a 45-minute interview. It maps perfectly to the problem constraints 
 *   and avoids the logarithmic overhead of Dijkstra.
 *
 * 
 * 3. ADDITIONAL INSIGHTS
 * ------------------------------------------------
 * Key Insight:
 * Separating the "Graph Traversal" from the "Coordinate Mapping" is crucial. Don't try to traverse the 
 * 2D array using row/col increments. Traverse logically from `curr = 1` to `curr = n^2`, and use a helper 
 * method `getCoordinates(square)` strictly to check for snakes/ladders. 
 * 
 * Pitfalls (Common Traps):
 * 1. Array index out of bounds: The squares are 1-indexed (1 to n^2), but arrays are 0-indexed. 
 * 2. Marking 'visited' at the wrong time: In BFS, mark nodes as visited the moment they are ENQUEUED, 
 *    not when they are dequeued. Otherwise, multiple nodes might queue the same target, bloating the queue.
 * 3. Double jumping: If a ladder takes you to a snake, you stop there. You don't take the snake.
 * 4. Coordinate math: Boustrophedon logic is notoriously easy to get off-by-one.
 * 
 * Edge Cases to Consider:
 * - n = 2 (The smallest board, 4 squares).
 * - A board completely filled with snakes leading back to 1 (Unreachable, must return -1).
 * - Landing on a ladder that perfectly drops you on n^2.
 * 
 * Pattern Recognition: 
 * - Minimum steps/moves + Unweighted choices (dice rolls, knight moves) -> BFS. 
 * - Similar problems: "Knight Dialer", "Word Ladder", "Minimum Genetic Mutation".
 * 
 * Dry Run (BFS on a 3x3 board):
 * Board:
 * [ -1, -1, -1]  <- squares 7, 8, 9 (row 0)
 * [ -1,  3, -1]  <- squares 6, 5, 4 (row 1, reversed!) -> square 5 is a snake to 3
 * [ -1, -1, -1]  <- squares 1, 2, 3 (row 2)
 * 
 * Q = [1], visited = {1}
 * Move 0:
 *   Pop 1: Roll 1..6.
 *   - Roll 1 (sq 2): No ladder. dest = 2. Q = [2], visited={1,2}
 *   - Roll 2 (sq 3): No ladder. dest = 3. Q = [2,3], visited={1,2,3}
 *   - Roll 3 (sq 4): No ladder. dest = 4. Q = [2,3,4], visited={1,2,3,4}
 *   - Roll 4 (sq 5): Snake to 3! dest = 3. 3 is visited. Ignore.
 *   - Roll 5 (sq 6): No ladder. dest = 6. Q = [2,3,4,6], visited={1,2,3,4,6}
 *   - Roll 6 (sq 7): No ladder. dest = 7. Q = [2,3,4,6,7], visited={1...7}
 * Move 1:
 *   Pop 2..7. When we pop 4, we roll 5 to get to square 9. We hit n^2! Return moves + 1 = 2.
 * 
 * Interview Script Snippet:
 * "Since we want the minimum number of rolls and every roll counts as exactly one move, this is 
 * fundamentally a shortest-path problem on an unweighted directed graph. I'll use Breadth-First 
 * Search. I'll represent my position by the 1D square number (1 to N^2) rather than 2D coordinates 
 * to make the dice-roll logic trivial (`curr + 1` to `curr + 6`). I'll isolate the messy 1D-to-2D 
 * Boustrophedon transformation into a pure helper method. That keeps the BFS clean."
 * 
 * 5. SUMMARY
 * ------------------------------------------------
 * - Core pattern: BFS for unweighted shortest path.
 * - Key observation: Map 1D logical game state to 2D physical board state on demand.
 * - Memorize: Boustrophedon mapping: `rowFromBottom = (id - 1) / n`, `col = (id - 1) % n`. Reverse `col` if `rowFromBottom` is odd.
 * - One-line trigger: "Unweighted minimum moves -> Queue-based BFS with Enqueue-time visited checks."
 */

import java.util.LinkedList;
import java.util.Queue;

public class SnakesAndLadders {

    // Record to hold our 2D coordinates cleanly (Java 14+ feature)
    record Coordinate(int row, int col) {}

    /**
     * Optimal Approach: Breadth-First Search
     * Time Complexity: O(N^2) - We visit each cell at most once.
     * Space Complexity: O(N^2) - For the visited array and Queue.
     */
    public int snakesAndLadders(int[][] board) {
        int n = board.length;
        int target = n * n;
        
        // 1-indexed visited array maps cleanly to the square numbers (1 to n^2)
        boolean[] visited = new boolean[target + 1];
        Queue<Integer> queue = new LinkedList<>();
        
        // Start at square 1
        queue.offer(1);
        visited[1] = true;
        
        int moves = 0;
        
        // Standard BFS level-by-level traversal
        while (!queue.isEmpty()) {
            int size = queue.size();
            
            // Process all squares at the current "move" depth
            for (int i = 0; i < size; i++) {
                int curr = queue.poll();
                
                // If we've reached the end, return the number of moves taken to get here
                if (curr == target) {
                    return moves;
                }
                
                // Roll the dice: 1 through 6
                for (int dice = 1; dice <= 6; dice++) {
                    int nextSquare = curr + dice;
                    
                    // Don't fall off the board
                    if (nextSquare > target) {
                        break;
                    }
                    
                    // Translate logical 1D square to 2D board coordinates
                    Coordinate coord = getCoordinates(nextSquare, n);
                    
                    // Determine where we actually land (taking snake/ladder if present)
                    int destination = board[coord.row()][coord.col()] != -1 
                                      ? board[coord.row()][coord.col()] 
                                      : nextSquare;
                    
                    // IMPORTANT: Check visited on the DESTINATION, not the intermediate 'nextSquare'
                    // Mark visited upon ENQUEUE to prevent duplicate work
                    if (!visited[destination]) {
                        visited[destination] = true;
                        queue.offer(destination);
                    }
                }
            }
            // Increment move counter after completing a full "wave" of possibilities
            moves++;
        }
        
        // If the queue exhausts and we haven't hit target, it's unreachable
        return -1;
    }

    /**
     * Converts a 1-indexed board square number to its 2D matrix coordinates.
     * Handles the "boustrophedon" (alternating left-right / right-left) layout.
     */
    private Coordinate getCoordinates(int square, int n) {
        // Convert to 0-indexed for easier math
        int zeroIndexedId = square - 1;
        
        // Which row are we in, counting from the bottom? (0 is the very bottom row)
        int rowFromBottom = zeroIndexedId / n;
        
        // Which col are we in, counting strictly left-to-right?
        int col = zeroIndexedId % n;
        
        // If the row from bottom is odd, the board goes right-to-left. Reverse the column.
        if (rowFromBottom % 2 == 1) {
            col = n - 1 - col;
        }
        
        // Map "row from bottom" to standard 2D array row (0 is the very top row)
        int actualRow = n - 1 - rowFromBottom;
        
        return new Coordinate(actualRow, col);
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        SnakesAndLadders solver = new SnakesAndLadders();

        // Test Case 1: Standard Example
        // -1, -1, -1, -1, -1, -1
        // -1, -1, -1, -1, -1, -1
        // -1, -1, -1, -1, -1, -1
        // -1, 35, -1, -1, 13, -1
        // -1, -1, -1, -1, -1, -1
        // -1, 15, -1, -1, -1, -1
        int[][] board1 = {
            {-1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1},
            {-1, 35, -1, -1, 13, -1},
            {-1, -1, -1, -1, -1, -1},
            {-1, 15, -1, -1, -1, -1}
        };
        System.out.println("Test Case 1 (Expected 4): " + solver.snakesAndLadders(board1));

        // Test Case 2: Minimal Board (2x2)
        int[][] board2 = {
            {-1, -1},
            {-1,  3}
        };
        System.out.println("Test Case 2 (Expected 1): " + solver.snakesAndLadders(board2));

        // Test Case 3: Unreachable Board (Snakes trap you)
        // Square 2 drops to 1, square 3 drops to 1. Cannot progress.
        int[][] board3 = {
            {-1, -1, -1},
            {-1, -1, -1},
            {-1,  1,  1} 
        };
        System.out.println("Test Case 3 (Expected -1): " + solver.snakesAndLadders(board3));

        // Test Case 4: Chained Snakes/Ladders (Testing rules)
        // Problem states we only take the FIRST snake/ladder.
        // E.g. Land on 2 -> go to 5. 5 has a ladder to 8. We DO NOT take 5->8 in the same turn.
        int[][] board4 = {
            {-1, -1, -1}, // sq 7, 8, 9 
            {-1,  8, -1}, // sq 6, 5, 4 (5 goes to 8)
            {-1,  5, -1}  // sq 1, 2, 3 (2 goes to 5)
        };
        // Move 1: Roll 1 -> sq 2 -> Ladder to 5.
        // Move 2: From 5, roll 4 -> sq 9. (Total 2 moves).
        System.out.println("Test Case 4 (Expected 2): " + solver.snakesAndLadders(board4));
    }
}

import java.util.*;

/**
 * ============================================================
 *              SNAKES AND LADDERS (FULL GUIDE)
 * ============================================================
 *
 * 🎯 Problem Type:
 * - Shortest Path in Unweighted Graph
 *
 * 🎯 Core Mapping:
 * - Each square = node
 * - Dice roll (1–6) = edges
 * - Snake/Ladder = forced jump (single edge redirection)
 *
 * 🎯 Best Approach:
 * - BFS (because each move costs 1)
 *
 * 🎯 Why not DFS?
 * - DFS explores deep paths first → NOT shortest guaranteed
 *
 * ------------------------------------------------------------
 * 🔥 This file contains:
 * 1. BFS with Flattening (Optimal, clean, interview-ready)
 * 2. DFS (Brute force with pruning, for understanding)
 * ------------------------------------------------------------
 */
public class SnakesAndLaddersFullGuide {

    /**
     * ============================================================
     *  🚀 APPROACH 1: BFS + FLATTENING (BEST SOLUTION)
     * ============================================================
     *
     * 🧠 IDEA:
     * - Convert 2D board → 1D array
     * - Run BFS from square 1 → n²
     *
     * WHY FLATTEN?
     * - Avoid repeated row/col computation
     * - Simplifies logic → fewer bugs
     *
     * TIME:  O(n^2)
     * SPACE: O(n^2)
     */
    public int snakesAndLadders_BFS(int[][] board) {
        int n = board.length;

        // Step 1: Flatten board into 1D array
        int[] flat = flattenBoard(board);

        // Step 2: BFS setup
        Queue<Integer> queue = new ArrayDeque<>();
        boolean[] visited = new boolean[n * n + 1];

        queue.offer(1);      // Start from square 1
        visited[1] = true;

        int moves = 0;       // Number of dice throws

        /**
         * BFS Level Order:
         * Each level = 1 dice throw
         */
        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int curr = queue.poll();

                // 🎯 Reached destination
                if (curr == n * n) {
                    return moves;
                }

                /**
                 * Try all possible dice rolls (1 → 6)
                 */
                for (int dice = 1; dice <= 6; dice++) {
                    int next = curr + dice;

                    if (next > n * n) break;

                    /**
                     * Apply snake/ladder ONLY ONCE
                     * (Important: No chaining)
                     */
                    if (flat[next] != -1) {
                        next = flat[next];
                    }

                    /**
                     * Avoid cycles (VERY IMPORTANT)
                     */
                    if (!visited[next]) {
                        visited[next] = true;
                        queue.offer(next);
                    }
                }
            }

            moves++; // One dice throw completed
        }

        return -1; // Not reachable
    }

    /**
     * ============================================================
     * 🔥 FLATTEN BOARD (2D → 1D)
     * ============================================================
     *
     * 🧠 KEY OBSERVATION:
     * - Board is zig-zag (boustrophedon)
     * - Bottom row: left → right
     * - Next row: right → left
     *
     * 🎯 Goal:
     * - Create mapping:
     *   flat[i] = destination if snake/ladder exists
     *           = -1 otherwise
     *
     * Example:
     * square 1 → board[n-1][0]
     *
     * TIME: O(n^2)
     */
    private int[] flattenBoard(int[][] board) {
        int n = board.length;

        int[] flat = new int[n * n + 1]; // 1-based indexing

        int index = 1;
        boolean leftToRight = true;

        /**
         * Traverse from bottom row → top row
         */
        for (int r = n - 1; r >= 0; r--) {

            if (leftToRight) {
                for (int c = 0; c < n; c++) {
                    flat[index++] = board[r][c];
                }
            } else {
                for (int c = n - 1; c >= 0; c--) {
                    flat[index++] = board[r][c];
                }
            }

            // Flip direction for next row
            leftToRight = !leftToRight;
        }

        return flat;
    }

    /**
     * ============================================================
     * ⚠️ APPROACH 2: DFS (NOT OPTIMAL)
     * ============================================================
     *
     * 🧠 WHY DFS FAILS:
     * - DFS explores deep paths first
     * - May find non-optimal solution first
     *
     * BUT:
     * - Useful to understand problem as graph traversal
     * - Can be used with pruning (still inefficient)
     *
     * TIME: Exponential (Worst case)
     * SPACE: O(n^2) recursion + visited
     */
    public int snakesAndLadders_DFS(int[][] board) {
        int n = board.length;
        int[] flat = flattenBoard(board);

        boolean[] visited = new boolean[n * n + 1];

        // Use array to simulate "global min"
        int[] result = new int[]{Integer.MAX_VALUE};

        dfs(1, flat, visited, 0, result, n * n);

        return result[0] == Integer.MAX_VALUE ? -1 : result[0];
    }

    /**
     * DFS Helper
     *
     * @param curr     current square
     * @param flat     flattened board
     * @param visited  to avoid cycles
     * @param moves    current moves count
     * @param result   global minimum (mutable)
     * @param target   destination (n^2)
     */
    private void dfs(int curr, int[] flat, boolean[] visited,
                     int moves, int[] result, int target) {

        /**
         * 🎯 BASE CASE: reached destination
         */
        if (curr == target) {
            result[0] = Math.min(result[0], moves);
            return;
        }

        /**
         * ✂️ PRUNING:
         * If already worse than best answer → stop
         */
        if (moves >= result[0]) return;

        visited[curr] = true;

        /**
         * Explore all dice possibilities
         */
        for (int dice = 1; dice <= 6; dice++) {
            int next = curr + dice;

            if (next > target) break;

            if (flat[next] != -1) {
                next = flat[next];
            }

            if (!visited[next]) {
                dfs(next, flat, visited, moves + 1, result, target);
            }
        }

        /**
         * BACKTRACK
         */
        visited[curr] = false;
    }

    /**
     * ============================================================
     * 🧠 INTERVIEW SUMMARY
     * ============================================================
     *
     * If interviewer asks:
     *
     * Q: Why BFS?
     * A: Because all edges have equal weight → shortest path
     *
     * Q: Why flatten?
     * A: Avoid repeated coordinate calculations → cleaner code
     *
     * Q: Why visited[]?
     * A: Prevent infinite loops due to snakes
     *
     * Q: Why no chaining?
     * A: Problem explicitly says only one jump per move
     *
     * ============================================================
     */
}


/**
 * ============================================================
 * 🚀 GRAPH COMPRESSION VERSION (INTERVIEW ADVANCED)
 * ============================================================
 *
 * Key Idea:
 * ----------
 * Precompute final landing for each square
 *
 * final[i] = i (normal)
 * final[i] = destination (snake/ladder)
 *
 * Then BFS becomes super clean:
 * curr → final[curr + dice]
 *
 * ------------------------------------------------------------
 * Time:  O(n^2)
 * Space: O(n^2)
 * ------------------------------------------------------------
 */
public class SnakesAndLaddersCompressed {

    public int snakesAndLadders(int[][] board) {
        int n = board.length;

        int[] flat = flatten(board);

        /**
         * 🔥 GRAPH COMPRESSION STEP
         */
        int[] nextState = new int[n * n + 1];

        for (int i = 1; i <= n * n; i++) {
            nextState[i] = (flat[i] == -1) ? i : flat[i];
        }

        /**
         * Standard BFS
         */
        Queue<Integer> queue = new ArrayDeque<>();
        boolean[] visited = new boolean[n * n + 1];

        queue.offer(1);
        visited[1] = true;

        int moves = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int curr = queue.poll();

                if (curr == n * n) return moves;

                /**
                 * Only logic left: transitions
                 */
                for (int dice = 1; dice <= 6; dice++) {
                    int next = curr + dice;

                    if (next > n * n) break;

                    // 🔥 Single lookup instead of condition
                    next = nextState[next];

                    if (!visited[next]) {
                        visited[next] = true;
                        queue.offer(next);
                    }
                }
            }

            moves++;
        }

        return -1;
    }

    /**
     * Flatten board (same as before)
     */
    private int[] flatten(int[][] board) {
        int n = board.length;

        int[] flat = new int[n * n + 1];
        int idx = 1;
        boolean leftToRight = true;

        for (int r = n - 1; r >= 0; r--) {
            if (leftToRight) {
                for (int c = 0; c < n; c++) {
                    flat[idx++] = board[r][c];
                }
            } else {
                for (int c = n - 1; c >= 0; c--) {
                    flat[idx++] = board[r][c];
                }
            }
            leftToRight = !leftToRight;
        }

        return flat;
    }
}

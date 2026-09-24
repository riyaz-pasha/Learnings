import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/*
 * ==========================================================================================
 * THE MAZE — STUDY NOTES & REASONING JOURNEY
 * ==========================================================================================
 *
 * 1. CLARIFYING QUESTIONS (What to ask before coding)
 * ------------------------------------------------------------------------------------------
 * Q: Can the ball stop on the destination if the destination is on the edge of the maze, 
 *    even if there's no wall there?
 *    Why: Clarifies that grid boundaries act exactly like walls. We don't just look for '1's.
 * 
 * Q: Am I allowed to mutate the input maze array to track my visited state?
 *    Why: If yes, we can change '0's to '2's and save O(M*N) memory. If no, we must allocate 
 *    a separate boolean[][] array. (Note: For this specific problem, modifying the grid 
 *    is dangerous because rolling mechanics treat '2' as an obstacle or get confused. A 
 *    separate boolean[][] is vastly safer).
 * 
 * Q: Are we looking for the *shortest* number of rolls, or just checking *if* it's possible?
 *    Why: Determines if we can use a basic DFS (which is easy to write recursively but might 
 *    find a long meandering path) or if we strictly need BFS (which naturally finds the 
 *    shortest path). The problem asks "whether it can stop", returning a boolean, so both work.
 * 
 *
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------------------------------------------------
 * What is actually being asked?
 * We are navigating a grid, but our movement isn't step-by-step. It's "slide until you crash".
 * We need to find if there is a sequence of "slides" that ends exactly on the destination coordinates.
 * 
 * The Binding Constraint:
 * M, N <= 100. The maze has 10,000 cells. 
 * 
 * Approach 1: Naive Simulation (What I'd naturally try)
 * I would place the ball at the start, pick a direction, roll it until it hits a wall, and 
 * then recursively try the next 4 directions from that new stopping point. 
 * 
 * Why it fails: Infinite loops! The ball can roll Left, hit a wall, roll Right, hit a wall, 
 * roll Left... forever. Time is exponential/infinite. Space is O(M*N) for the call stack 
 * before it crashes with a StackOverflowError.
 * 
 * The Bottleneck / Repeated Work:
 * We are simulating rolls from the *exact same stopping positions* multiple times. The maze 
 * is static. If we roll into corner (0, 5) and explore all options, doing it again 5 minutes 
 * later gives the exact same result.
 * 
 * The Observation that removes the bottleneck:
 * We only care about STOPPING POINTS. The graph isn't "every cell in the grid". The graph 
 * only consists of the cells where the ball can naturally come to rest. The "edges" between 
 * these nodes are the sliding actions.
 * 
 * Approach 2: BFS / DFS on "Stopping Points" with a Visited Set (Optimal)
 * We treat every stopping point as a Node. We use a Queue for BFS. 
 * 1. Pop a stopping point.
 * 2. Slide the ball in 4 directions to find the adjacent "stopping points".
 * 3. If a stopping point hasn't been visited, mark it visited and push to the queue.
 * 
 * Complexity:
 * - Time: O(M * N * max(M, N)). Why? There are at most M*N stopping points (nodes). From each 
 *   stopping point, we process 4 directions. In each direction, the ball can roll at most 
 *   max(M, N) cells before hitting a wall. 100 * 100 * 100 = 1,000,000 operations. Easily 
 *   fast enough for a 1-second limit.
 * - Space: O(M * N) for the `visited` array and the Queue/Call Stack. 10,000 booleans is tiny.
 *
 *
 * 3. KEY INSIGHT & ASCII DIAGRAM
 * ------------------------------------------------------------------------------------------
 * Insight: Do NOT mark cells as visited WHILE rolling over them.
 * 
 * Look at this setup:
 *     (Start) S . . D . W (Wall)
 *             W . . . . W
 *             W W S2. . W 
 * 
 * If S rolls RIGHT toward the Wall, it passes OVER D. If you mark everything in its path as 
 * "visited", D becomes visited. But S didn't *stop* at D; it stopped at the dot before W.
 * Later, if S2 rolls UP, it might actually be able to STOP at D (if there's a wall above D). 
 * But if S already marked D as visited while rolling *past* it, S2 will ignore it, and 
 * you'll return FALSE incorrectly!
 * 
 * RULE: Only mark the **final stopping positions** as visited.
 *
 *
 * 4. TEMPTING WRONG APPROACHES / PITFALLS
 * ------------------------------------------------------------------------------------------
 * Pitfall 1: Checking `if (r == dest[0] && c == dest[1])` IN THE MIDDLE of the `while` loop 
 *            that slides the ball.
 * Why it's tempting: "If my ball touches the destination, I win!"
 * Why it fails: The problem strictly states: "The destination is considered reached only if 
 *               the ball can stop exactly on that cell."
 * 
 * Pitfall 2: Treating it like a standard maze BFS (moving 1 step at a time).
 * Why it's tempting: It's the standard LeetCode 200 (Number of Islands) muscle memory.
 * Why it fails: A ball cannot turn 90 degrees in the middle of an empty corridor. It MUST 
 *               exhaust its momentum first.
 *
 *
 * 5. REUSABLE PATTERN: "State-Space Graph Traversal"
 * ------------------------------------------------------------------------------------------
 * When you see: "Movement that is continuous until an obstacle is hit" or "You can only 
 * make a decision at specific points (intersections)".
 * 
 * Think: Graph Traversal where Nodes = Decision Points (not every grid cell). Abstract the 
 * continuous movement into a single O(N) edge-traversal function.
 * 
 * Transfers to: 
 * - The Maze II & III (shortest path/holes)
 * - Sokoban (pushing boxes until they hit walls)
 * - Sliding block puzzles (2048, Ice Slider games).
 *
 *
 * 6. INTERVIEW TALK-THROUGH
 * ------------------------------------------------------------------------------------------
 * "To solve this, I'm going to model it as a graph traversal. The catch is that the nodes 
 * in our graph aren't every empty cell in the grid. Because the ball can't stop in the middle 
 * of a corridor, the nodes are only the valid stopping positions against walls or boundaries. 
 * 
 * I'll use Breadth-First Search. I'll maintain a queue of stopping positions and a boolean 
 * visited matrix to prevent infinite loops. When I pop a position, I'll simulate rolling the 
 * ball in all four cardinal directions. The simulation will run a while-loop until it hits a 
 * wall or boundary, then back up one step to find the resting place. If this resting place 
 * hasn't been visited, I'll mark it visited and enqueue it. If the resting place is exactly 
 * our destination, I can return true.
 * 
 * This gives us O(M*N * max(M,N)) time and O(M*N) space."
 *
 *
 * 7. INTERVIEWER FOLLOW-UPS
 * ------------------------------------------------------------------------------------------
 * F: "What if the maze was 10,000 x 10,000, but there were very few walls (sparse maze)?"
 * A: Our BFS time complexity O(M*N * max(M,N)) would TLE because the 'rolling' while-loop 
 *    takes O(10,000) steps every single time. Instead of rolling step-by-step, we could 
 *    pre-process the walls using HashMaps mapping a row index to a TreeSet of column indices 
 *    (and vice-versa). Then, a roll is just a binary search `treeset.lower(current_col)` to 
 *    instantly find the next wall in O(log W) time!
 * 
 * F: "What if we wanted to find the shortest distance traveled by the ball?"
 * A: (This is 'The Maze II'). We can't use standard BFS because edge weights (the roll lengths) 
 *    are no longer uniform (1 step). We would switch to Dijkstra's Algorithm, using a 
 *    PriorityQueue ordered by total distance traveled. We'd also replace `boolean[][] visited` 
 *    with an `int[][] distances` array to track the shortest known distance to each stopping point.
 */

public class TheMaze {

    // Cardinal directions: Up, Down, Left, Right
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    /**
     * OPTIMAL APPROACH: Breadth-First Search (BFS) on Stopping Points
     */
    public boolean hasPath(int[][] maze, int[] start, int[] destination) {
        // Edge cases
        if (maze == null || maze.length == 0 || maze[0].length == 0) return false;
        
        int m = maze.length;
        int n = maze[0].length;

        // Visited array: Tracks ONLY the cells where the ball has come to a complete STOP.
        boolean[][] visited = new boolean[m][n];
        
        // Deque is the modern, preferred implementation for a Queue in Java 
        // (faster than LinkedList, no node allocation overhead).
        Queue<int[]> queue = new ArrayDeque<>();
        
        queue.offer(start);
        visited[start[0]][start[1]] = true;

        // DRY RUN SNAPSHOT: 
        // queue = [[start_row, start_col]]
        // visited[start_row][start_col] = true

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currRow = current[0];
            int currCol = current[1];

            // If the popped stopping point is the destination, we found a valid path!
            if (currRow == destination[0] && currCol == destination[1]) {
                return true;
            }

            // From the current stopping point, try pushing the ball in all 4 directions
            for (int[] dir : DIRECTIONS) {
                int nextRow = currRow;
                int nextCol = currCol;

                // SIMULATE THE ROLL
                // Keep moving in the current direction as long as we are:
                // 1. Within maze boundaries
                // 2. Not hitting a wall (maze[r][c] == 0)
                while (nextRow >= 0 && nextRow < m && 
                       nextCol >= 0 && nextCol < n && 
                       maze[nextRow][nextCol] == 0) {
                    nextRow += dir[0];
                    nextCol += dir[1];
                }

                // BACK UP ONE STEP
                // The while loop breaks when `nextRow, nextCol` is ON a wall or OUT of bounds.
                // The ball actually stops one step before that invalid position.
                nextRow -= dir[0];
                nextCol -= dir[1];

                // Check if this new stopping point is unvisited.
                // NOTICE: We do not care if we passed over visited/unvisited cells during the roll.
                if (!visited[nextRow][nextCol]) {
                    visited[nextRow][nextCol] = true;
                    queue.offer(new int[]{nextRow, nextCol});
                }
            }
        }

        // If the queue empties and we never stopped on the destination, it's impossible.
        return false;
    }

    /*
     * ==========================================================================================
     * SUMMARY
     * ==========================================================================================
     * Pattern: State-Space BFS / Macro-step BFS
     * Key Observation: Nodes are ONLY the stopping points. The path there is just an edge calculation.
     * Memorize vs Understand: 
     *   - Understand WHY we only mark stopping points as visited. 
     *   - Memorize the idiom for rolling: `while(valid) { forward } back_up_one_step;`
     * Most Common Trap: Marking intermediate cells as visited while the ball rolls over them, 
     *                   which accidentally blocks valid perpendicular paths.
     * Mental Trigger: "Move until obstacle" -> "Graph where nodes = walls/stops".
     */

    // ------------------------------------------------------------------------------------------
    // MAIN METHOD: Cross-checking edge cases and representative inputs
    // ------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        TheMaze solver = new TheMaze();

        // 1. STANDARD CASE: Path exists
        int[][] maze1 = {
            {0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0},
            {1, 1, 0, 1, 1},
            {0, 0, 0, 0, 0}
        };
        int[] start1 = {0, 4};
        int[] dest1 = {4, 4};
        System.out.println("Test 1 (Expected TRUE): " + solver.hasPath(maze1, start1, dest1));

        // 2. UNREACHABLE CASE: Destination blocked off / requires stopping in mid-air
        int[] start2 = {0, 4};
        int[] dest2 = {3, 2}; // Dest is inside an open corridor without a wall to stop against
        System.out.println("Test 2 (Expected FALSE): " + solver.hasPath(maze1, start2, dest2));

        // 3. TRICKY CASE: Pass through destination without stopping
        // S . . D . W
        // The ball starts at 0, rolls right, passes D at 3, but stops at 4 (before the wall at 5).
        int[][] maze3 = {
            {0, 0, 0, 0, 0, 1}
        };
        int[] start3 = {0, 0};
        int[] dest3 = {0, 3}; 
        System.out.println("Test 3 (Pass through, Expected FALSE): " + solver.hasPath(maze3, start3, dest3));
        
        // But if dest is at index 4 (right before the wall), it stops there.
        int[] dest4 = {0, 4};
        System.out.println("Test 4 (Stops at wall, Expected TRUE): " + solver.hasPath(maze3, start3, dest4));
    }
}

/**
 * THE MAZE - Study Notes & Solution
 * 
 * CLARIFYING QUESTIONS BEFORE WRITING CODE:
 * 1. "Does rolling *over* the destination count as reaching it?" 
 *    -> Prevents the assumption that standard cell-by-cell grid BFS works. The ball MUST stop on it.
 * 2. "Are the outer boundaries of the grid considered walls?" 
 *    -> Confirms that the ball won't roll infinitely out of bounds; the grid edges act as stopping points.
 * 3. "Is it possible for the maze to be completely empty (all 0s)?"
 *    -> Ensures we don't assume walls exist internally; ball would just bounce boundary to boundary.
 * 4. "Can the start and destination be the same cell?"
 *    -> The prompt says no, but asking ensures you know whether to check `if (start == dest)` before any loops.
 * 
 * ----------------------------------------------------------------------------------------------------
 * THE REASONING JOURNEY: HOW TO DISCOVER THIS
 * 
 * 1. The Core Constraint & The Wrong Mental Model:
 *    When we see a 2D grid, our brain immediately screams: "Standard BFS/DFS! Check 4 adjacent cells!"
 *    But if we do that, we are treating EVERY cell as a decision point. 
 *    In this problem, the ball lacks friction. Once it moves, it loses agency. It cannot choose to turn 
 *    at cell (1,1) if there's no wall stopping it. 
 * 
 * 2. The Paradigm Shift (The Key Insight):
 *    Instead of treating the grid as a graph where Nodes = Cells and Edges = 1-step moves,
 *    we must treat it as a graph where:
 *    - NODES = Valid stopping points (cells adjacent to walls/boundaries).
 *    - EDGES = The entire rolling action in a specific direction.
 * 
 * 3. Brute Force (Naïve Recursion without memory):
 *    - Try rolling UP. We hit a wall. From there, try rolling UP, DOWN, LEFT, RIGHT.
 *    - Why it fails: The ball will roll LEFT to a wall, then RIGHT to the opposite wall, then LEFT...
 *      It creates an infinite loop. 
 *    - Cost: Infinite time, $O(M \times N)$ space before StackOverflow.
 * 
 * 4. Optimal Approach (BFS/DFS on Stopping Points):
 *    - To break infinite loops, we need to remember where we've STOPPED.
 *    - Do we need to remember where we've *rolled over*? No. A cell we merely rolled over 
 *      could later be crossed perpendicularly. We only care about visited *stopping points*.
 *    - Start at `start`. Roll in all 4 directions. 
 *    - For each direction, advance `(r, c)` until you hit a wall. Back up one step. 
 *    - Is this new `(r, c)` in our `visited` set? If no, add it to the Queue/Stack and mark it visited.
 *    - Stop and return TRUE if `(r, c) == destination`.
 * 
 *    - Time Complexity: $O(M \cdot N \cdot (M + N))$
 *      Why? There are at most $O(M \cdot N)$ valid stopping points (nodes).
 *      For each node, we check 4 directions. 
 *      Rolling in a direction takes at most $\max(M, N)$ steps to hit a wall.
 *    - Space Complexity: $O(M \cdot N)$
 *      We need a `boolean[][] visited` array and a Queue/Stack, both bounded by the grid size.
 * 
 * ----------------------------------------------------------------------------------------------------
 * ASCII DRY RUN & VISUALIZATION
 * 
 * Grid:
 * 0 0 1 0 0 (S = Start, D = Dest, 1 = Wall)
 * 0 0 0 0 0
 * 0 0 0 1 0
 * 1 1 0 1 1
 * 0 0 0 0 D
 * 
 * Tempting mistake: Rolling right from S(0,0) -> hits wall at (0,2). Stops at (0,1). 
 * Now at (0,1), we roll Down. We pass through (4,1). 
 * If D was at (3,1), we would roll OVER it. We don't stop, because there's no wall below (3,1). 
 * The destination at (3,1) would be UNREACHABLE from that roll.
 * 
 * ----------------------------------------------------------------------------------------------------
 * PITFALLS & TEMPTING MISTAKES
 * 1. The "Off-By-One" Roll: 
 *    `while (maze[r+dr][c+dc] == 0) { r+=dr; c+=dc; }` is dangerous because you have to check bounds 
 *    inside the while loop condition, which gets messy. 
 *    Better pattern: `while (valid(r, c)) { advance }` -> then back up one step: `r -= dr; c -= dc;`.
 * 2. Marking "Rolled Over" cells as visited:
 *    If you mark every cell the ball traverses as visited, you might block a future perpendicular path.
 *    (Actually, in Maze I boolean reachability, it surprisingly doesn't break correctness because if a path 
 *    was reachable via crossing, it's reachable via the origin of the cross. BUT it completely breaks 
 *    Maze II where you need the shortest distance. Best practice: only mark STOPPING points as visited).
 * 3. Modifying the original input grid to save space:
 *    Changing `maze[r][c] = 2` to mark visited is a common trick, but in an interview, ask first! 
 *    Usually, inputs are considered read-only unless specified.
 */

import java.util.LinkedList;
import java.util.Queue;
import java.util.Arrays;

public class TheMaze {

    // Helper class for coordinates makes Queue operations significantly more readable 
    // than using int[] arrays, and avoids the "new int[]{r, c}" object churn if we reused them.
    // However, for algorithmic simplicity in interviews, int[] is perfectly fine. We'll use int[].
    
    // The 4 cardinal directions: {row_change, col_change}
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    /**
     * Standard BFS Approach
     * 
     * Why BFS? 
     * For a simple "is it reachable" (boolean), DFS and BFS are equivalent. 
     * However, BFS naturally extends to "The Maze II" (finding the shortest path/fewest rolls), 
     * so it's a better muscle memory pattern to build for grid traversal problems.
     */
    public boolean hasPath(int[][] maze, int[] start, int[] destination) {
        int m = maze.length;
        int n = maze[0].length;
        
        // visited[i][j] means we have STOPPED at cell (i, j).
        boolean[][] visited = new boolean[m][n];
        
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(start);
        visited[start[0]][start[1]] = true;
        
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currRow = current[0];
            int currCol = current[1];
            
            // Early exit: We stopped exactly on the destination.
            if (currRow == destination[0] && currCol == destination[1]) {
                return true;
            }
            
            // Try rolling in all 4 directions from the current stopping point
            for (int[] dir : DIRECTIONS) {
                int r = currRow;
                int c = currCol;
                
                // ROLL! 
                // We advance as long as the NEXT cell is within bounds and not a wall.
                // We check 'r + dir[0]' to peek ahead.
                while (r + dir[0] >= 0 && r + dir[0] < m && 
                       c + dir[1] >= 0 && c + dir[1] < n && 
                       maze[r + dir[0]][c + dir[1]] == 0) {
                    r += dir[0];
                    c += dir[1];
                }
                
                // Now, (r, c) is the exact cell where the ball stops.
                // If we haven't stopped here before, it's a new state to explore.
                if (!visited[r][c]) {
                    visited[r][c] = true;
                    queue.offer(new int[]{r, c});
                }
            }
        }
        
        return false; // Queue is empty, never hit destination
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * INTERVIEW OUT LOUD:
     * "To solve this, I need to recognize that this isn't a standard cell-by-cell BFS. 
     * The ball doesn't make decisions at every cell; it only makes decisions when it hits a wall. 
     * Therefore, my graph nodes aren't all the empty cells—they are only the stopping points. 
     * My edges are the act of rolling in a direction until blocked.
     * 
     * I'll use a queue to do a Breadth-First Search. From my current stopping point, I'll loop 
     * through the 4 directions. For each direction, I'll use a while-loop to simulate rolling 
     * until a wall or boundary is hit. I'll then check if that final stopping cell has been visited. 
     * If not, I mark it visited and push it to the queue. If it matches the destination, I return true.
     * 
     * This guarantees O(M*N*(M+N)) time, because there are at most M*N stopping points, and rolling 
     * takes at most max(M,N) steps. The space is O(M*N) for the visited array and the queue."
     * ------------------------------------------------------------------------------------------------
     */

    public static void main(String[] args) {
        TheMaze solver = new TheMaze();
        
        // TEST CASE 1: Path exists
        // Ball can go DOWN to bottom, then RIGHT to dest.
        int[][] maze1 = {
            {0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0},
            {1, 1, 0, 1, 1},
            {0, 0, 0, 0, 0}
        };
        int[] start1 = {0, 4}; 
        int[] dest1 = {4, 4};
        System.out.println("Test 1 (Expect true): " + solver.hasPath(maze1, start1, dest1));
        
        // TEST CASE 2: Rolls OVER destination but cannot stop
        // Destination is (3,2). Ball rolls from (0,4) -> (0,2) -> (4,2). It passes (3,2) but doesn't stop.
        int[] dest2 = {3, 2};
        System.out.println("Test 2 (Expect false): " + solver.hasPath(maze1, start1, dest2));
        
        // TEST CASE 3: Boxed in
        int[][] maze3 = {
            {0, 0, 0},
            {0, 1, 0},
            {0, 0, 0}
        };
        int[] start3 = {0, 0};
        int[] dest3 = {2, 2};
        // Ball stops at corners: (0,2), (2,0), (2,2)
        System.out.println("Test 3 (Expect true): " + solver.hasPath(maze3, start3, dest3));
    }
}

/**
 * ----------------------------------------------------------------------------------------------------
 * SUMMARY & CHEAT SHEET
 * 
 * Core Pattern: "Graph State Re-definition" (Nodes aren't cells, nodes are stopping points).
 * 
 * Key Observation: You must peek ahead (`r + dr`) inside the while loop to stop exactly ON the 
 * empty space, rather than rolling into the wall and having to back up.
 * 
 * When you see X, think Y:
 * - When you see "moves until blocked", "sliding on ice", or "bouncing lazers":
 *   Think: The nodes are the stopping points, not the intermediate cells.
 *   Transfers to: "The Maze II", "The Maze III", "Minimum Moves to Move a Box to Their Target Location".
 * 
 * Most Common Trap: Using `visited[r][c] = true` INSIDE the while-loop while rolling. 
 * This breaks "The Maze II" and is conceptually wrong, because you CAN roll through a cell 
 * horizontally even if you've rolled through it vertically before. Only mark the STOPPING cell.
 * 
 * One-Line Mental Trigger: "While empty, roll forward; when stopped, BFS."
 */

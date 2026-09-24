import java.util.*;

/*
 * ==========================================================================================
 *                                WALLS AND GATES
 * ==========================================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ==========================================================================================
 * A. What is the problem REALLY asking?
 * The problem gives us a 2D grid containing:
 *   - Gates (0)
 *   - Walls (-1)
 *   - Empty rooms (represented by Integer.MAX_VALUE, or 2^31 - 1)
 * 
 * We need to determine the shortest distance from every empty room to its NEAREST gate.
 * 
 * The important part is that Y (the shortest distance to a gate) is really asking us to
 * compute a "Global Shortest Path" problem where there are MULTIPLE possible targets (gates).
 * We are essentially mapping a "distance field" across the entire grid.
 * 
 * B. What are the inputs and outputs?
 * Input: `int[][] rooms` (the grid).
 * Output: None directly (we modify the grid in-place).
 * Constraints: Dimensions m, n <= 250. 
 * This means the grid has up to 250 * 250 = 62,500 cells.
 * 
 * C. What words in the problem should trigger an algorithmic thought?
 * - "grid" / "adjacent cells" -> Think: Graph traversal (where cells are nodes, adjacencies are edges).
 * - "minimum number of moves" / "nearest" -> Think: Shortest path -> Breadth-First Search (BFS).
 * - "update rooms in place" -> Think: We can use the grid itself as our "visited" tracker.
 * 
 * 
 * 2. CONSTRAINTS -> ALGORITHM
 * ==========================================================================================
 * Let N be the total number of cells (m * n). Here, N <= 62,500.
 * 
 * N             Roughly acceptable Time Complexity
 * ------------------------------------------------
 * N <= 20       O(2^N) / O(N!) (Backtracking)
 * N <= 100      O(N^3) (Floyd-Warshall)
 * N <= 1,000    O(N^2) (Nested traversals)
 * N <= 100,000  O(N log N) / O(N) (Linear traversals, BFS, DFS)
 * 
 * If N = 10? We could run a BFS from every single empty room. (O(N^2) = 100 operations).
 * If N = 62,500? O(N^2) would be roughly 3.9 billion operations. 
 * In Java, 1 second handles roughly 10^8 operations. 3.9 billion will likely Time Limit Exceed (TLE).
 * Therefore, we absolutely NEED an algorithm that is close to O(N).
 * 
 * 
 * 3. INTERVIEW CLARIFICATION QUESTIONS
 * ==========================================================================================
 * Q1: "Are diagonal movements allowed?"
 *     Why ask: Changes the neighbor generation logic. (Usually no, but crucial to verify).
 *     If YES: Generate 8 neighbors. If NO: Generate 4 neighbors.
 * 
 * Q2: "Can an empty room be completely blocked off by walls from all gates?"
 *     Why ask: Clarifies edge cases for unreachability.
 *     If YES: The problem states it should remain Integer.MAX_VALUE. Good to know.
 * 
 * Q3: "What if there are no gates in the grid? Or no empty rooms?"
 *     Why ask: Identifies early-exit base cases.
 *     If no gates: We do nothing.
 * 
 * Q4: "Is it possible for the grid to have dimensions 0x0?"
 *     Why ask: Standard boundary validation.
 * 
 * 
 * 4. BUILD A MENTAL MODEL BEFORE CODING
 * ==========================================================================================
 * Objects: 
 *   - Nodes: Each cell in the grid.
 *   - Edges: The boundary between adjacent cells (Up, Down, Left, Right).
 *   - Blockers: Walls (-1) sever the edges.
 *   - Sources/Targets: Gates (0).
 * 
 * Operation:
 *   - Finding the shortest path through an unweighted graph.
 * 
 * Valid solution:
 *   - Every `INF` cell is replaced by an integer `d` where `d` is the exact minimum number
 *     of edge crossings to reach a `0` without crossing a `-1`.
 * 
 * 
 * 5. START WITH THE NAIVE HUMAN SOLUTION
 * ==========================================================================================
 * If I had no clever trick, how would I solve this?
 * "I need to find the nearest gate for each empty room."
 * 
 * The naive approach:
 * 1. Scan the grid.
 * 2. Every time I find an empty room (`INF`), I want to find its nearest gate.
 * 3. To find the nearest gate, I start a Breadth-First Search (BFS) starting exactly at this room.
 * 4. The BFS expands level by level (distance 1, distance 2...)
 * 5. The FIRST time the BFS hits a gate (0), I record that distance and stop the BFS.
 * 6. Repeat this entire process for EVERY single empty room.
 * 
 * Why is this correct? 
 * BFS on an unweighted graph explores strictly level-by-level. By definition, the first 
 * target you encounter in BFS is the closest target. Repeating this for every room guarantees correctness.
 * 
 * 
 * 6. FIND THE BOTTLENECK
 * ==========================================================================================
 * Where exactly is the time going?
 * Let's visualize the naive approach.
 * 
 * Grid: 
 * INF  INF  INF   0
 * INF  -1   INF  INF
 * 
 * - BFS from Room(0,0): Explores (0,1), (0,2), finds Gate(0,3). Dist = 3.
 * - BFS from Room(0,1): Explores (0,2), finds Gate(0,3). Dist = 2.
 * - BFS from Room(0,2): Finds Gate(0,3). Dist = 1.
 * 
 * What work are we repeating?
 * We repeatedly traverse the SAME edges and the SAME rooms over and over again!
 * When we searched from Room(0,0), we already walked past Room(0,1) and Room(0,2). 
 * We threw away perfectly good information!
 * 
 * The bottleneck is the multiple independent O(N) searches.
 * Total time: O(EmptyRooms * N) = O(N^2). Too slow for N = 62,500.
 * 
 * 
 * 7. IDENTIFY THE STRUCTURE OF THE INPUT
 * ==========================================================================================
 * Property: Path symmetry in undirected graphs.
 * Consequence: The shortest path from Room -> Gate is EXACTLY the same as Gate -> Room.
 * Algorithmic opportunity: 
 * Instead of starting from many rooms looking for a few gates, what if we start from the 
 * gates and look for the rooms?
 * 
 * 
 * 8. TURN OBSERVATIONS INTO QUESTIONS
 * ==========================================================================================
 * Q1: If I reverse the search (start at a gate), what happens?
 * Answer: I can run a BFS from a gate. Whenever I reach an empty room, I update its distance.
 * 
 * Q2: Does this solve the bottleneck?
 * Answer: If there is 1 gate, yes! O(N) total time. BUT, if there are many gates, I have 
 * to run a BFS from Gate 1, then a BFS from Gate 2... 
 * I am STILL overlapping searches. A room might be updated by Gate 1, and then updated 
 * again by Gate 2 if Gate 2 is closer. Total time: O(Gates * N). Still O(N^2) worst case!
 * 
 * Q3: How do I prevent gates from doing overlapping work?
 * Answer: I need the BFS to expand from ALL gates SIMULTANEOUSLY.
 * 
 * Q4: How is simultaneous BFS even possible?
 * Answer: In standard BFS, we start with ONE node in the queue at distance 0.
 * But the queue doesn't care if nodes belong to the same origin! 
 * If we put ALL gates in the queue at the very beginning, the BFS will process everything 
 * at distance 1 from ANY gate, then everything at distance 2 from ANY gate.
 * 
 * 
 * 9. BRUTE FORCE -> BETTER -> OPTIMAL (See implementations below)
 * ==========================================================================================
 * 
 * 10. FOR THE OPTIMAL SOLUTION, EXPLAIN THE DISCOVERY
 * ==========================================================================================
 * The leap to "Multi-Source BFS" is the core trick.
 * Think of it like dropping stones in a pond. 
 * - Naive approach: Drop a stone, wait for the ripple to reach the edge. Repeat for next stone.
 * - Multi-source BFS: Drop ALL stones into the pond at the EXACT SAME TIME.
 * 
 * The ripples expand together. If a ripple from Stone A meets a ripple from Stone B, they 
 * simply stop (because that water is already disturbed).
 * 
 * Translated to code:
 * 1. Find all 0s (gates) and put them in the Queue.
 * 2. Pop a cell. Look at its neighbors.
 * 3. If a neighbor is INF, its shortest distance is `current_cell_value + 1`.
 * 4. Update the neighbor, and push it to the Queue.
 * 5. If a neighbor is already a number < INF, it means another gate's ripple reached it 
 *    first (or at the same time). We ignore it!
 * 
 * Why is this guaranteed to be the absolute shortest path?
 * Because BFS processes strictly in order of distance. The very first time an INF cell is 
 * reached by ANY path, that path is guaranteed to be the shortest possible path across the 
 * entire grid.
 * 
 * 
 * 11. DISTINGUISH MEMORIZATION FROM REASONING
 * ==========================================================================================
 * What I should memorize:
 * "Multi-Source BFS structure: Add all starting points to queue BEFORE starting the while loop."
 * 
 * What I should understand:
 * The optimization comes from processing independent starting points concurrently so they 
 * share a single "visited" state. This prevents paths from overlapping and crossing, 
 * reducing O(Sources * N) to O(N).
 * 
 * 
 * 12. COMMON WRONG APPROACHES
 * ==========================================================================================
 * Mistake: Using Depth-First Search (DFS) from gates.
 * Why it looks reasonable: DFS also visits neighbors, right?
 * Why it fails: DFS goes deep before going wide. It might find a path of length 15 to a room 
 * that is right next to another gate! To fix this, DFS would have to continue searching 
 * even after finding a path, constantly overwriting distances if it finds shorter ones.
 * This devolves into exploring ALL possible paths, which is exponentially slow O(3^N).
 * 
 * Counterexample for DFS:
 * 0   INF  INF
 * INF -1   INF
 * 0   INF  INF
 * DFS from top-left 0 might snake all the way around the -1, assigning a huge distance 
 * to the bottom-right INF, entirely missing that there's a 0 right next to it until later.
 * BFS inherently prevents this.
 * 
 * 
 * 13. EDGE CASES
 * ==========================================================================================
 * - No gates: The queue is initially empty. The while loop never runs. Rooms remain INF. Correct.
 * - No empty rooms: Queue has gates, but neighbors are never INF. Loop finishes fast. Correct.
 * - Unreachable rooms: A room surrounded by walls. The BFS ripples will never reach it. 
 *   It remains INF. Correct.
 * 
 * 
 * 14. DATA STRUCTURE DECISION
 * ==========================================================================================
 * - Queue<int[]> instead of custom class: `int[]` of length 2 `[row, col]` is standard, 
 *   fast, and avoids writing boilerplate object classes.
 * - ArrayDeque vs LinkedList: `ArrayDeque` is generally faster in Java for Queue operations 
 *   because it avoids node allocation overhead.
 * - Visited Set?: We don't need a `boolean[][] visited`! The grid ITSELF acts as our visited 
 *   set. If `rooms[r][c] != INF`, it has already been visited (either it's a wall, a gate, 
 *   or a room we already updated).
 * 
 * 
 * 15. RECURSION VS ITERATION
 * ==========================================================================================
 * BFS is almost exclusively Iterative (using a Queue).
 * Why? Recursive functions use the call stack, which acts as a LIFO Stack, naturally 
 * resulting in DFS (Depth-First). Emulating BFS via recursion is highly unnatural and inefficient.
 * Always use Iteration + Queue for BFS.
 * 
 * 
 * 16. JAVA-SPECIFIC CONSIDERATIONS
 * ==========================================================================================
 * - Integer.MAX_VALUE is exactly 2^31 - 1, which matches the problem description for INF.
 * - Mutating the input array (`rooms`) in-place is allowed and expected here.
 * - When checking boundaries, always do `r >= 0 && r < m && c >= 0 && c < n` FIRST to 
 *   short-circuit and prevent `ArrayIndexOutOfBoundsException`.
 * 
 * 
 * 17. DETAILED DRY RUN (Multi-Source BFS)
 * ==========================================================================================
 * Grid:
 * INF  -1  0  INF
 * 
 * Initial state:
 * Queue: [ (0,2) ]  <- The only gate
 * 
 * Iteration 1:
 * Pop (0,2). Value is 0.
 * Neighbors of (0,2):
 * - Left (0,1): is -1. Ignore.
 * - Right (0,3): is INF. 
 *   -> Update (0,3) to 0 + 1 = 1.
 *   -> Push (0,3) to Queue.
 * 
 * Queue: [ (0,3) ]
 * 
 * Iteration 2:
 * Pop (0,3). Value is 1.
 * Neighbors of (0,3):
 * - Left (0,2): is 0 (not INF). Ignore.
 * - No other neighbors.
 * 
 * Queue is empty. 
 * Final Grid:
 * INF -1  0  1
 * 
 * Notice (0,0) remained INF because the wall blocked it. Correct!
 * 
 * 
 * 18. RECOGNITION PATTERN
 * ==========================================================================================
 * WHEN YOU SEE THIS TYPE OF PROBLEM:
 * 1. A grid with "obstacles" and "empty spaces".
 * 2. A request for "shortest distance" or "minimum time".
 * 3. MULTIPLE starting points (multiple targets, multiple sources, multiple infections).
 * 
 * -> THINK: Multi-Source BFS.
 * Similar problems: 
 * - Rotting Oranges (minimum time for all oranges to rot).
 * - 01 Matrix (distance to nearest 0).
 * - Map of Highest Peak.
 * 
 * 
 * 19. INTERVIEW THINKING SCRIPT
 * ==========================================================================================
 * ME: "The problem asks for the shortest path from every room to a gate. Since it's a 
 * shortest path problem on an unweighted grid, Breadth-First Search is the right tool."
 * 
 * INTERVIEWER: "Sounds good. How would you apply BFS?"
 * 
 * ME: "The naive way is to run a BFS from every empty room looking for a gate. But that's 
 * O(N^2) because we re-traverse the same paths. We can optimize by flipping the perspective: 
 * start BFS from the gates looking for rooms."
 * 
 * INTERVIEWER: "Will that fix the time complexity?"
 * 
 * ME: "Only if we do it carefully. If we run a separate BFS from each gate, it's still O(N^2) 
 * in the worst case (like a grid full of gates). But if we put ALL gates into the queue at 
 * the very beginning, we can run a Multi-Source BFS. They will expand simultaneously, and the 
 * first time any 'ripple' touches an empty room, we know it's the absolute shortest path. 
 * That brings the time down to O(M * N)."
 * 
 * 
 * 20. HOW TO RECOGNIZE THE ALGORITHM IN A NEW PROBLEM
 * ==========================================================================================
 * - If I see "shortest path" in a grid -> think BFS.
 * - If the input has MULTIPLE targets -> ask whether doing BFS simultaneously from all 
 *   targets simplifies the problem.
 * - If brute force repeatedly traverses the same state looking for different goals -> 
 *   consider reversing the search (Search from Goals -> Starts).
 * 
 * 
 * 21. SOLUTION COMPARISON
 * ==========================================================================================
 * Approach                | Time      | Space    | Trade-off & Recommendation
 * --------------------------------------------------------------------------------------
 * BFS from every Room     | O((MN)^2) | O(MN)    | Too slow. Only mention to show reasoning.
 * BFS from every Gate     | O((MN)^2) | O(MN)    | Still too slow in worst case.
 * Multi-Source BFS        | O(MN)     | O(MN)    | 🥇 Optimal. Present this in interview.
 * 
 * The key insight to remember: Treat all targets as a single collective starting point.
 * 
 * 
 * 22. FOLLOW-UP QUESTIONS
 * ==========================================================================================
 * Q: "What if different empty rooms have different movement costs (weighted edges)?"
 * A: BFS no longer guarantees shortest path. We would need to transition to Dijkstra's Algorithm 
 *    using a PriorityQueue instead of a standard Queue.
 * 
 * Q: "What if the grid is too large to fit in memory?"
 * A: We would have to divide the grid into chunks. Boundary cells would need to pass their 
 *    distance values to neighboring chunks, requiring multiple passes until distances stabilize 
 *    (similar to Bellman-Ford or a distributed shortest path algorithm).
 * 
 * Q: "Can we optimize space complexity?"
 * A: The space complexity is O(M*N) because in the worst case (all gates), the queue holds 
 *    M*N elements. We cannot reduce this without sacrificing O(M*N) time, as BFS inherently 
 *    requires a queue proportional to the width of the frontier.
 * 
 * 
 * 23. FINAL TAKEAWAYS
 * ==========================================================================================
 * - Pattern: Multi-Source Breadth First Search.
 * - Key Observation: BFS doesn't care if the queue has one starting node or 50 starting nodes. 
 *   It processes whatever is in the queue strictly by distance level.
 * - Reasoning Habit: When a search feels redundant (Many-to-One), try reversing it (One-to-Many). 
 *   If it's Many-to-Many, group one side together and search simultaneously (Group-to-Many).
 * - Common Trap: Forgetting to check if a neighbor is actually an INF room before adding 
 *   it to the queue, which causes infinite loops or overwrites shorter paths.
 * - Interview Sentence: "I'll use a Multi-Source BFS, starting concurrently from all gates, 
 *   to guarantee O(M * N) time."
 * - Memory Hook: "Drop all stones in the pond at once."
 * 
 * ==========================================================================================
 */

public class WallsAndGates {

    // Constant for an empty room to improve readability
    private static final int EMPTY = Integer.MAX_VALUE;
    private static final int GATE = 0;
    
    // Direction vectors: Up, Down, Left, Right
    private static final int[][] DIRECTIONS = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    /**
     * BRUTE FORCE APPROACH (For educational purposes)
     * Time: O((M*N)^2)
     * Space: O(M*N) for queue
     */
    public void wallsAndGates_BruteForce(int[][] rooms) {
        if (rooms == null || rooms.length == 0) return;
        int m = rooms.length;
        int n = rooms[0].length;

        // Iterate through every cell. If it's an empty room, search for a gate.
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                if (rooms[r][c] == EMPTY) {
                    rooms[r][c] = bfsFindNearestGate(rooms, r, c);
                }
            }
        }
    }

    private int bfsFindNearestGate(int[][] rooms, int startRow, int startCol) {
        int m = rooms.length;
        int n = rooms[0].length;
        Queue<int[]> queue = new ArrayDeque<>();
        boolean[][] visited = new boolean[m][n];

        queue.offer(new int[]{startRow, startCol, 0}); // row, col, distance
        visited[startRow][startCol] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0], c = curr[1], dist = curr[2];

            // If we found a gate, return the distance
            if (rooms[r][c] == GATE) {
                return dist;
            }

            for (int[] dir : DIRECTIONS) {
                int nr = r + dir[0];
                int nc = c + dir[1];

                // Bounds check, wall check, and visited check
                if (nr >= 0 && nr < m && nc >= 0 && nc < n && 
                    rooms[nr][nc] != -1 && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{nr, nc, dist + 1});
                }
            }
        }
        return EMPTY; // Gate not found
    }

    /**
     * OPTIMAL APPROACH: MULTI-SOURCE BFS
     * Time: O(M * N)
     * Space: O(M * N)
     */
    public void wallsAndGates_Optimal(int[][] rooms) {
        if (rooms == null || rooms.length == 0) return;
        
        int m = rooms.length;
        int n = rooms[0].length;
        
        // Use ArrayDeque for optimal queue performance in Java
        Queue<int[]> queue = new ArrayDeque<>();
        
        // STEP 1: Find all gates and add them to the queue as our starting frontier.
        // This is the core of Multi-Source BFS.
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                if (rooms[r][c] == GATE) {
                    queue.offer(new int[]{r, c});
                }
            }
        }
        
        // STEP 2: Process the queue level by level (ripple effect).
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0];
            int col = curr[1];
            
            // Check all 4 adjacent directions
            for (int[] dir : DIRECTIONS) {
                int nextRow = row + dir[0];
                int nextCol = col + dir[1];
                
                // If out of bounds, skip
                if (nextRow < 0 || nextRow >= m || nextCol < 0 || nextCol >= n) {
                    continue;
                }
                
                // If it's NOT an empty room (it's a wall, a gate, or an already visited room), skip.
                // This acts as our "visited" check, saving us from using a boolean[][] matrix!
                if (rooms[nextRow][nextCol] != EMPTY) {
                    continue;
                }
                
                // Update the empty room with the shortest distance.
                // Since this is BFS, the FIRST time we reach this room, it is guaranteed
                // to be the shortest path from ANY gate.
                rooms[nextRow][nextCol] = rooms[row][col] + 1;
                
                // Add the newly updated room to the queue to continue the ripple
                queue.offer(new int[]{nextRow, nextCol});
            }
        }
    }

    // ==========================================================================================
    // MAIN METHOD FOR TESTING & DEMONSTRATION
    // ==========================================================================================
    public static void main(String[] args) {
        WallsAndGates solution = new WallsAndGates();

        // Helper to clone grid for testing multiple approaches
        System.out.println("--- Test Case 1: Standard Grid ---");
        int[][] grid1 = {
            {EMPTY,  -1,     GATE,  EMPTY},
            {EMPTY,  EMPTY,  EMPTY, -1},
            {EMPTY,  -1,     EMPTY, -1},
            {GATE,   -1,     EMPTY, EMPTY}
        };
        int[][] grid1Copy = deepCopy(grid1);

        solution.wallsAndGates_BruteForce(grid1);
        System.out.println("Brute Force Result:");
        printGrid(grid1);

        solution.wallsAndGates_Optimal(grid1Copy);
        System.out.println("Optimal Result:");
        printGrid(grid1Copy);

        System.out.println("\n--- Test Case 2: Unreachable Rooms ---");
        int[][] grid2 = {
            {GATE,  -1,    EMPTY},
            {-1,    -1,    -1},
            {EMPTY, -1,    GATE}
        };
        solution.wallsAndGates_Optimal(grid2);
        printGrid(grid2);
        // Expect the EMPTY at (0,2) and (2,0) to remain INF because walls block them completely.

        System.out.println("\n--- Test Case 3: No Gates ---");
        int[][] grid3 = {
            {EMPTY, EMPTY},
            {EMPTY, EMPTY}
        };
        solution.wallsAndGates_Optimal(grid3);
        printGrid(grid3);
        // Expect all to remain INF
    }

    // Utility method to deep copy a 2D array
    private static int[][] deepCopy(int[][] original) {
        int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = original[i].clone();
        }
        return result;
    }

    // Utility method to neatly print the grid
    private static void printGrid(int[][] grid) {
        for (int[] row : grid) {
            for (int val : row) {
                if (val == EMPTY) {
                    System.out.print("INF\t");
                } else if (val == -1) {
                    System.out.print("-1\t");
                } else {
                    System.out.print(val + "\t");
                }
            }
            System.out.println();
        }
    }
}

import java.util.*;

/*
===========================================================
PROBLEM: Walls and Gates
===========================================================

Cell values:
- -1  => WALL (blocked)
- 0   => GATE (source)
- INF => EMPTY ROOM (Integer.MAX_VALUE)

Goal:
Fill each empty room with distance to nearest gate.

IMPORTANT:
- Modify in-place
- If unreachable → remain INF

===========================================================
APPROACHES INCLUDED:
1. Brute Force DFS from every room
2. BFS from every room
3. OPTIMAL: Multi-Source BFS (Best)
===========================================================
*/

class WallsAndGates {

    private static final int INF = Integer.MAX_VALUE;
    private static final int[][] DIRS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    /*
    ===========================================================
    1. BRUTE FORCE DFS (Worst)
    ===========================================================

    Idea:
    - For every empty room, try DFS to find nearest gate

    Problems:
    - Repeated work
    - Explores same paths many times

    Time Complexity:
    O((m*n) * (m*n))  → VERY BAD

    Space:
    O(m*n) recursion stack
    ===========================================================
    */
    public void wallsAndGatesDFS(int[][] rooms) {
        int m = rooms.length;
        int n = rooms[0].length;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {

                if (rooms[i][j] == INF) {
                    boolean[][] visited = new boolean[m][n];
                    int distance = dfs(i, j, rooms, visited);

                    if (distance != Integer.MAX_VALUE) {
                        rooms[i][j] = distance;
                    }
                }
            }
        }
    }

    private int dfs(int i, int j, int[][] rooms, boolean[][] visited) {
        int m = rooms.length, n = rooms[0].length;

        if (i < 0 || j < 0 || i >= m || j >= n
                || rooms[i][j] == -1
                || visited[i][j]) {
            return Integer.MAX_VALUE;
        }

        if (rooms[i][j] == 0) return 0;

        visited[i][j] = true;

        int min = Integer.MAX_VALUE;

        for (int[] d : DIRS) {
            int next = dfs(i + d[0], j + d[1], rooms, visited);

            if (next != Integer.MAX_VALUE) {
                min = Math.min(min, 1 + next);
            }
        }

        visited[i][j] = false;

        return min;
    }

    /*
    ===========================================================
    2. BFS FROM EACH ROOM (Better but still bad)
    ===========================================================

    Idea:
    - From each empty room, BFS until gate found

    Time:
    O((m*n) * (m*n)) worst case

    Better than DFS but still inefficient
    ===========================================================
    */
    public void wallsAndGatesBFSEachRoom(int[][] rooms) {
        int m = rooms.length, n = rooms[0].length;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {

                if (rooms[i][j] == INF) {
                    rooms[i][j] = bfsFromRoom(i, j, rooms);
                }
            }
        }
    }

    private int bfsFromRoom(int i, int j, int[][] rooms) {
        int m = rooms.length, n = rooms[0].length;

        Queue<int[]> q = new ArrayDeque<>();
        boolean[][] visited = new boolean[m][n];

        q.offer(new int[]{i, j});
        visited[i][j] = true;

        int distance = 0;

        while (!q.isEmpty()) {
            int size = q.size();

            while (size-- > 0) {
                int[] cur = q.poll();
                int r = cur[0], c = cur[1];

                if (rooms[r][c] == 0) {
                    return distance;
                }

                for (int[] d : DIRS) {
                    int nr = r + d[0];
                    int nc = c + d[1];

                    if (nr >= 0 && nc >= 0 && nr < m && nc < n
                            && rooms[nr][nc] != -1
                            && !visited[nr][nc]) {

                        visited[nr][nc] = true;
                        q.offer(new int[]{nr, nc});
                    }
                }
            }

            distance++;
        }

        return INF;
    }

    /*
    ===========================================================
    3. OPTIMAL: MULTI-SOURCE BFS (BEST SOLUTION)
    ===========================================================

    🔥 KEY IDEA:
    - Push ALL gates into queue initially
    - Expand BFS simultaneously

    Why this works:
    - First time we reach a room → shortest distance
    - Avoid repeated work

    Time Complexity:
    O(m*n)

    Space:
    O(m*n) for queue

    ===========================================================
    */
    public void wallsAndGates(int[][] rooms) {
        int m = rooms.length, n = rooms[0].length;

        Queue<int[]> q = new ArrayDeque<>();

        // Step 1: Add all gates to queue
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {

                if (rooms[i][j] == 0) {
                    q.offer(new int[]{i, j});
                }
            }
        }

        // Step 2: BFS
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1];

            for (int[] d : DIRS) {
                int nr = r + d[0];
                int nc = c + d[1];

                // Only process EMPTY rooms
                if (nr >= 0 && nc >= 0 && nr < m && nc < n
                        && rooms[nr][nc] == INF) {

                    // Distance update
                    rooms[nr][nc] = rooms[r][c] + 1;

                    q.offer(new int[]{nr, nc});
                }
            }
        }
    }

    /*
    ===========================================================
    INTERVIEW SUMMARY
    ===========================================================

    ❌ DFS from each room → Too slow
    ❌ BFS from each room → Still slow

    ✅ Multi-source BFS:
       - Push all gates
       - Expand outward
       - First reach = shortest path

    PATTERN:
    👉 "Distance to nearest X" → Think MULTI-SOURCE BFS

    Similar Problems:
    - 01 Matrix
    - Rotten Oranges
    - Nearest Exit
    - Fire Spread problems

    ===========================================================
    */
}


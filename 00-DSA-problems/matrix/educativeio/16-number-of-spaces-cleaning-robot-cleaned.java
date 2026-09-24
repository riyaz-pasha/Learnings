/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n binary grid representing a room. 
 * '0' is an empty space and '1' is an object (obstacle).
 * A cleaning robot starts at the top-left (0, 0) facing right.
 * It moves forward to clean spaces. If it hits a wall or an obstacle, it turns 
 * 90 degrees clockwise without moving. 
 * It continues this behavior indefinitely. If the robot ever reaches a state 
 * (row, column, and direction) that it has already experienced, it means it is 
 * stuck in an infinite loop. 
 * We need to return the total number of unique spaces it has cleaned.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the room be completely enclosed by obstacles right at the start?
 *    (Yes. If room is [[0, 1], [1, 1]], the robot will just spin in a circle 
 *    at (0, 0) and eventually realize it's looping. Total cleaned: 1).
 * 2. Can the robot visit the same cell multiple times?
 *    (Yes! It can cross paths or revisit a cell facing a *different* direction. 
 *    We only stop if it visits the same cell facing the *exact same* direction).
 * 3. Does turning count as a move that consumes a step?
 *    (Yes, hitting a wall causes a turn, but the robot stays in the same cell. 
 *    The new direction at the same cell becomes its next state).
 * 4. Can we modify the input grid?
 *    (If we want to achieve O(1) auxiliary space, we can modify the grid using 
 *    bit manipulation. Otherwise, O(m*n) space is perfectly acceptable).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. 1x1 Room: [[0]] 
 *    - Starts at (0,0) facing right. Hits wall -> turns down -> hits wall -> 
 *      turns left -> hits wall -> turns up -> hits wall -> turns right. 
 *    - Loop detected. Cleaned: 1.
 * 2. Hallway (1xN): [[0, 0, 0, 1, 0]]
 *    - Moves right to index 2. Hits obstacle at index 3. Turns down, left, up, 
 *      then moves left back to 0. 
 *    - Cleaned: 3 spaces.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Standard Simulation (O(M*N) Space):
 *    - The robot's state is defined by 3 variables: `row`, `col`, and `direction`.
 *    - There are only 4 directions (0: Right, 1: Down, 2: Left, 3: Up).
 *    - We can use a 3D boolean array `visited[m][n][4]` to track seen states.
 *    - We can use a 2D boolean array `cleaned[m][n]` to track unique cleanings.
 *    - Loop until we reach a `visited` state. Count spaces newly marked in `cleaned`.
 *    - Time: O(M * N) since max possible states is M * N * 4.
 *    - Space: O(M * N) for the tracking arrays.
 * 
 * 2. In-Place Bitmasking Simulation (O(1) Space - The Optimal Approach):
 *    - The grid only holds 0s and 1s. We have 30 unused bits in each integer!
 *    - We can use the 2nd, 3rd, 4th, and 5th bits of `room[i][j]` to store if 
 *      the robot has visited that cell facing Right, Down, Left, or Up.
 *    - To check if a cell is an obstacle, we just check if `room[r][c] == 1`.
 *    - To mark a state as visited: `room[r][c] |= (1 << (direction + 2))`.
 *    - To check if a state is visited: `(room[r][c] & (1 << (direction + 2))) != 0`.
 *    - A cell is cleaned if its value is strictly greater than 1.
 *    - Time: O(M * N), Space: O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Simulation)
 * ============================================================================
 * Room: 
 * [0, 0]
 * [0, 1]
 * 
 * 1. Start (0, 0, Right). Mark visited. Cleaned count = 1.
 *    Next is (0, 1). Safe. Move.
 * 2. At (0, 1, Right). Mark visited. Cleaned count = 2.
 *    Next is (0, 2). Wall! Turn Down. State becomes (0, 1, Down).
 * 3. At (0, 1, Down). Mark visited. 
 *    Next is (1, 1). Obstacle! Turn Left. State becomes (0, 1, Left).
 * 4. At (0, 1, Left). Mark visited.
 *    Next is (0, 0). Safe. Move.
 * 5. At (0, 0, Left). Mark visited. (Already cleaned).
 *    Next is (0, -1). Wall! Turn Up. State becomes (0, 0, Up).
 * 6. At (0, 0, Up). Mark visited.
 *    Next is (-1, 0). Wall! Turn Right. State becomes (0, 0, Right).
 * 7. At (0, 0, Right). Already visited this exact state! Loop detected. Break.
 * 
 * Total cleaned = 2.
 */

import java.util.*;

public class CleaningRobot {

    public static void main(String[] args) {
        int[][] room1 = {
            {0, 0, 0},
            {1, 1, 0},
            {0, 0, 0}
        };

        int[][] room2 = {
            {0, 1},
            {1, 1}
        };

        System.out.println("--- Solution 1: Standard Simulation ---");
        System.out.println("Room 1 Cleaned: " + numberOfCleanRoomsStandard(cloneGrid(room1))); 
        System.out.println("Room 2 Cleaned: " + numberOfCleanRoomsStandard(cloneGrid(room2))); 

        System.out.println("\n--- Solution 2: Optimal Bitmasking (O(1) Space) ---");
        System.out.println("Room 1 Cleaned: " + numberOfCleanRoomsOptimal(cloneGrid(room1))); 
        System.out.println("Room 2 Cleaned: " + numberOfCleanRoomsOptimal(cloneGrid(room2))); 
    }

    /**
     * SOLUTION 1: Standard Simulation
     * 
     * Idea: Traverse the matrix maintaining current (row, col, direction).
     * Mark states in a 3D visited array and keep a separate 2D cleaned array.
     * 
     * Time Complexity: O(M * N) - Maximum of 4 * M * N states.
     * Space Complexity: O(M * N) - For visited and cleaned arrays.
     */
    public static int numberOfCleanRoomsStandard(int[][] room) {
        if (room == null || room.length == 0) return 0;
        
        int m = room.length;
        int n = room[0].length;
        
        boolean[][][] visitedStates = new boolean[m][n][4];
        boolean[][] cleaned = new boolean[m][n];
        
        // Direction vectors: Right, Down, Left, Up
        int[] dr = {0, 1, 0, -1};
        int[] dc = {1, 0, -1, 0};
        
        int r = 0, c = 0, dir = 0;
        int uniqueCleanedCount = 0;
        
        // Loop runs until we hit an identical (row, col, dir) state
        while (!visitedStates[r][c][dir]) {
            // Mark this specific state as visited
            visitedStates[r][c][dir] = true;
            
            // If we are stepping on this cell for the first time
            if (!cleaned[r][c]) {
                cleaned[r][c] = true;
                uniqueCleanedCount++;
            }
            
            // Calculate the theoretical next step
            int nextR = r + dr[dir];
            int nextC = c + dc[dir];
            
            // Check if blocked by boundary or an obstacle
            if (nextR < 0 || nextR >= m || nextC < 0 || nextC >= n || room[nextR][nextC] == 1) {
                // Blocked: Turn 90 degrees clockwise.
                // Do NOT change r and c. The new direction at the same cell will 
                // be registered in the next while loop iteration.
                dir = (dir + 1) % 4;
            } else {
                // Clear path: Move forward.
                r = nextR;
                c = nextC;
            }
        }
        
        return uniqueCleanedCount;
    }

    /**
     * SOLUTION 2: Optimal Bitmasking (In-Place)
     * 
     * Idea: Instead of creating new arrays, we encode the visited directions 
     * directly into the 'room' matrix using unused higher-order bits.
     * Bit 0: Obstacle (1) or Empty (0).
     * Bits 2, 3, 4, 5: Visited while facing Right, Down, Left, Up.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(1) auxiliary - Truly in-place.
     */
    public static int numberOfCleanRoomsOptimal(int[][] room) {
        if (room == null || room.length == 0) return 0;
        
        int m = room.length;
        int n = room[0].length;
        
        int[] dr = {0, 1, 0, -1};
        int[] dc = {1, 0, -1, 0};
        
        int r = 0, c = 0, dir = 0;
        int uniqueCleanedCount = 0;
        
        while (true) {
            // Determine the bitmask for the current direction (shifted by 2 to avoid the 1st bit)
            int dirMask = 1 << (dir + 2);
            
            // If this state mask is already applied to this cell, we are in a loop
            if ((room[r][c] & dirMask) != 0) {
                break;
            }
            
            // If the cell is empty (0) or only has our direction masks (but no obstacle 1),
            // and this is the FIRST time we are adding a direction mask (value was 0),
            // it means we just cleaned a brand new cell.
            if (room[r][c] == 0) {
                uniqueCleanedCount++;
            }
            
            // Mark this direction as visited for this cell
            room[r][c] |= dirMask;
            
            int nextR = r + dr[dir];
            int nextC = c + dc[dir];
            
            // A cell is an obstacle ONLY if its 0th bit is exactly 1 (which means room[r][c] == 1 initially).
            // We use bitwise AND with 1 to extract the original obstacle state.
            if (nextR < 0 || nextR >= m || nextC < 0 || nextC >= n || (room[nextR][nextC] & 1) == 1) {
                // Turn
                dir = (dir + 1) % 4;
            } else {
                // Move
                r = nextR;
                c = nextC;
            }
        }
        
        return uniqueCleanedCount;
    }
    
    /**
     * Helper method to deeply clone a 2D array for testing so that in-place 
     * mutation in one solution doesn't affect the input for the next solution.
     */
    private static int[][] cloneGrid(int[][] original) {
        int[][] cloned = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            cloned[i] = original[i].clone();
        }
        return cloned;
    }
    
    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if the robot can optionally clean, but wants to clean the MAXIMUM 
     *    possible spaces (e.g., pathfinding instead of a fixed algorithm)?
     *    Answer: That becomes a variation of the Traveling Salesperson Problem 
     *    (TSP) or a Hamiltonian Path problem on a grid. You would need backtracking 
     *    or dynamic programming with state compression (Bitmask DP), and it would 
     *    only be computationally feasible for very small grids.
     * 
     * 2. What if we want to restore the grid to its original state after Solution 2?
     *    Answer: We can iterate through the grid at the end. For any cell where 
     *    `cell > 1`, we simply reset it to `0`. Any cell that is `1` remains `1` 
     *    because we never applied masks to obstacle cells (the robot never steps on them).
     */
}


/**
 * ============================================================
 * 🔥 Robot Room Cleaner — Simulation + Cycle Detection
 * ============================================================
 *
 * IDEA:
 * -----
 * Simulate robot movement until we revisit SAME (row, col, direction)
 *
 * WHY direction matters?
 * ----------------------
 * Same cell but different direction => different future path
 *
 * STATE:
 * -------
 * (row, col, dir)
 *
 * DIRECTIONS:
 * ------------
 * 0 = right
 * 1 = down
 * 2 = left
 * 3 = up
 *
 * MOVEMENT:
 * ----------
 * - Try forward
 * - If blocked → turn right
 *
 * TIME:
 * ------
 * O(m * n * 4)  ≈ O(mn)
 *
 * SPACE:
 * -------
 * O(mn)
 *
 */
public class RobotCleaner {

    // Direction vectors: right, down, left, up
    private static final int[][] DIRS = {
        {0, 1},   // →
        {1, 0},   // ↓
        {0, -1},  // ←
        {-1, 0}   // ↑
    };

    public static int cleanRoom(int[][] room) {

        int m = room.length;
        int n = room[0].length;

        // Stores visited states (r, c, dir)
        Set<String> visitedStates = new HashSet<>();

        // Stores cleaned cells (r, c)
        Set<String> cleanedCells = new HashSet<>();

        int r = 0, c = 0, dir = 0; // start at (0,0), facing right

        while (true) {

            String state = r + "," + c + "," + dir;

            // 🔥 Cycle detected → stop
            if (visitedStates.contains(state)) {
                return cleanedCells.size();
            }

            visitedStates.add(state);

            // Mark cleaned
            cleanedCells.add(r + "," + c);

            // Try moving forward
            int nr = r + DIRS[dir][0];
            int nc = c + DIRS[dir][1];

            // Check valid move
            if (nr >= 0 && nr < m && nc >= 0 && nc < n && room[nr][nc] == 0) {
                r = nr;
                c = nc;
            } else {
                // 🔁 Turn right
                dir = (dir + 1) % 4;
            }
        }
    }

    public static void main(String[] args) {

        int[][] room = {
            {0, 0, 0},
            {1, 0, 1},
            {0, 0, 0}
        };

        System.out.println(cleanRoom(room)); // Output depends on path
    }
}

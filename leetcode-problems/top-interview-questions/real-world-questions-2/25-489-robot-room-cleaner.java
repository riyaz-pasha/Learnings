import java.util.*;

/**
 * ROBOT ROOM CLEANER - COMPREHENSIVE SOLUTION GUIDE
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * You have a robot cleaner in a room with unknown layout. The room is represented
 * as a grid where:
 * - 0 represents an obstacle (wall)
 * - 1 represents an empty cell (cleanable)
 * 
 * Robot API (given, cannot be modified):
 * - boolean move(): Moves forward if possible, returns true if successful
 * - void turnLeft(): Turns 90 degrees left (no movement)
 * - void turnRight(): Turns 90 degrees right (no movement)
 * - void clean(): Cleans current cell
 * 
 * Robot starts at unknown position (r, c) facing unknown direction.
 * Goal: Clean entire room, return to starting position and direction.
 * 
 * CRITICAL CONSTRAINTS:
 * ====================
 * 1. You DON'T know the room layout
 * 2. You DON'T know robot's starting position
 * 3. You DON'T know robot's starting direction
 * 4. Robot can only sense if move() succeeded (hit wall or moved)
 * 5. Must clean ALL reachable cells
 * 6. Should not clean same cell twice (efficiency)
 * 
 * KEY INSIGHTS FOR INTERVIEWS:
 * ============================
 * 1. This is a BACKTRACKING problem (DFS in unknown space)
 * 2. Need to track visited cells using RELATIVE coordinates
 * 3. Must track robot's direction to know where it's facing
 * 4. After exploring, must BACKTRACK to previous position AND direction
 * 5. The room is like a maze - explore all paths, backtrack when stuck
 * 
 * APPROACH:
 * =========
 * Use DFS with backtracking:
 * - Use relative coordinates (start = (0,0))
 * - Try all 4 directions from each cell
 * - Mark visited cells to avoid re-cleaning
 * - After exploring direction, backtrack (go back AND restore direction)
 * 
 * Why relative coordinates?
 * - We don't know actual position, but we can track relative to start
 * - Start position = (0, 0) in our coordinate system
 * - Move up → (0, -1), down → (0, 1), left → (-1, 0), right → (1, 0)
 */

/**
 * Robot interface (provided by problem, don't modify)
 * This is just for reference - you won't implement this in interview
 */
interface Robot {
    // Returns true if next cell is open and robot moves into the cell.
    // Returns false if next cell is obstacle and robot stays in current cell.
    boolean move();
    
    // Robot will stay in the same cell after calling turnLeft/turnRight.
    // Each turn will be 90 degrees.
    void turnLeft();
    void turnRight();
    
    // Clean the current cell.
    void clean();
}

class RobotRoomCleaner {
    
    /**
     * APPROACH 1: DFS WITH BACKTRACKING (OPTIMAL)
     * ============================================
     * Time Complexity: O(N - M) where N = total cells, M = obstacles
     *                  We visit each reachable cell exactly once
     * Space Complexity: O(N - M) for visited set and recursion stack
     * 
     * ALGORITHM EXPLANATION:
     * =====================
     * 1. Define directions: up=0, right=1, down=2, left=3 (clockwise)
     * 2. Use relative coordinates (start = 0,0)
     * 3. For each cell:
     *    a. Clean it
     *    b. Mark as visited
     *    c. Try all 4 directions (explore)
     *    d. Backtrack after each direction
     * 4. Backtracking = go back to previous cell AND restore direction
     * 
     * DIRECTION TRACKING:
     * ===================
     * We track which way robot is facing (0=up, 1=right, 2=down, 3=left)
     * 
     * Visual representation:
     *        0 (up)
     *         ↑
     *         |
     * 3 (left)←+→ 1 (right)
     *         |
     *         ↓
     *        2 (down)
     * 
     * Direction vectors:
     * up(0):    dx=-1, dy=0  (move up in grid)
     * right(1): dx=0,  dy=1  (move right)
     * down(2):  dx=1,  dy=0  (move down)
     * left(3):  dx=0,  dy=-1 (move left)
     */
    
    // Direction vectors: up, right, down, left (clockwise order)
    // This order is CRITICAL - matches with turnRight() which rotates clockwise
    private static final int[][] DIRECTIONS = {
        {-1, 0},  // 0: up
        {0, 1},   // 1: right
        {1, 0},   // 2: down
        {0, -1}   // 3: left
    };
    
    private Robot robot;
    private Set<String> visited; // Track visited cells using relative coordinates
    
    public void cleanRoom(Robot robot) {
        this.robot = robot;
        this.visited = new HashSet<>();
        
        // Start DFS from (0,0) facing direction 0 (arbitrary choice)
        // We don't know actual position, so we use relative coordinates
        dfs(0, 0, 0);
    }
    
    /**
     * DFS to explore and clean the room
     * 
     * @param x current x position (relative)
     * @param y current y position (relative)
     * @param dir current direction robot is facing (0=up, 1=right, 2=down, 3=left)
     */
    private void dfs(int x, int y, int dir) {
        // Clean current cell
        robot.clean();
        
        // Mark as visited using string key for HashSet
        visited.add(x + "," + y);
        
        // Try all 4 directions (relative to current direction)
        for (int i = 0; i < 4; i++) {
            // Calculate new direction after i turns
            // (dir + i) % 4 gives us the absolute direction after i right turns
            int newDir = (dir + i) % 4;
            
            // Calculate next position based on new direction
            int newX = x + DIRECTIONS[newDir][0];
            int newY = y + DIRECTIONS[newDir][1];
            
            String nextKey = newX + "," + newY;
            
            // If not visited and can move to this cell
            if (!visited.contains(nextKey) && robot.move()) {
                // Recursively clean from new position
                dfs(newX, newY, newDir);
                
                // BACKTRACK: return to previous cell
                // Why? We need to explore other directions from this cell
                goBack();
            }
            
            // Turn right to try next direction
            // After 4 turns, robot faces original direction again
            robot.turnRight();
        }
        
        // After exploring all 4 directions, robot is back to original direction
        // This is important for maintaining direction consistency
    }
    
    /**
     * CRITICAL BACKTRACKING FUNCTION
     * ==============================
     * Returns robot to previous cell facing the SAME direction it came from
     * 
     * Steps:
     * 1. Turn 180 degrees (2 right turns)
     * 2. Move forward (now going back)
     * 3. Turn 180 degrees again (to face original direction)
     * 
     * Why this works:
     * - After exploring a cell, we're facing original direction (due to 4 turns)
     * - We need to go BACK to where we came from
     * - Turn around, move, turn back = return to previous position and direction
     * 
     * Example:
     * Current: At (1,0) facing RIGHT
     * After goBack(): At (0,0) facing RIGHT (same direction!)
     */
    private void goBack() {
        robot.turnRight();  // Turn 90° right
        robot.turnRight();  // Turn 90° right again (now facing opposite direction)
        robot.move();       // Move back to previous cell
        robot.turnRight();  // Turn 90° right
        robot.turnRight();  // Turn 90° right again (now facing original direction)
    }
    
    /**
     * APPROACH 2: ITERATIVE DFS WITH EXPLICIT STACK
     * ==============================================
     * Time Complexity: O(N - M)
     * Space Complexity: O(N - M)
     * 
     * Same idea as recursive, but uses explicit stack.
     * More complex to implement, rarely needed in interviews.
     * Included for completeness.
     */
    public void cleanRoomIterative(Robot robot) {
        this.robot = robot;
        Set<String> visited = new HashSet<>();
        
        // Stack stores: [x, y, direction, step_in_direction]
        // step_in_direction: which of 4 directions we're currently trying
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0, 0, 0});
        
        robot.clean();
        visited.add("0,0");
        
        while (!stack.isEmpty()) {
            int[] state = stack.peek();
            int x = state[0], y = state[1], dir = state[2], step = state[3];
            
            if (step == 4) {
                // Tried all 4 directions, backtrack
                stack.pop();
                if (!stack.isEmpty()) {
                    goBack();
                }
                continue;
            }
            
            // Try next direction
            state[3]++; // Increment step for next iteration
            
            int newDir = (dir + step) % 4;
            int newX = x + DIRECTIONS[newDir][0];
            int newY = y + DIRECTIONS[newDir][1];
            String nextKey = newX + "," + newY;
            
            if (!visited.contains(nextKey) && robot.move()) {
                robot.clean();
                visited.add(nextKey);
                stack.push(new int[]{newX, newY, newDir, 0});
            }
            
            robot.turnRight();
        }
    }
    
    /**
     * APPROACH 3: SPIRAL PATTERN CLEANING (ALTERNATIVE THINKING)
     * ===========================================================
     * This is NOT the optimal solution but shows alternative thinking.
     * Uses a spiral pattern to explore the room.
     * 
     * Problems:
     * - Doesn't guarantee cleaning all cells in complex layouts
     * - More complex to handle backtracking
     * - Not recommended for interviews
     * 
     * Included to show it's possible to think differently, but DFS is better.
     */
    
    /**
     * TESTING AND VISUALIZATION
     * =========================
     * Since we can't actually run the robot, here's how to trace through:
     * 
     * Example room:
     * 1 1 1
     * 1 1 0
     * 1 1 1
     * 
     * Robot starts at (1,1) facing up:
     * 
     * Step-by-step trace:
     * 1. Clean (1,1), mark visited
     * 2. Try UP: move to (0,1), clean, explore from there...
     * 3. From (0,1): try UP (fail-wall), RIGHT (move to 0,2), DOWN (move to 1,2-fail), LEFT (move to 0,0)...
     * 4. Eventually backtrack to (1,1)
     * 5. Try RIGHT from (1,1): hit wall
     * 6. Try DOWN from (1,1): move to (2,1)...
     * 7. Continue until all cells visited
     */
    
    // Mock implementation for testing (not part of solution)
    static class MockRobot implements Robot {
        private int[][] room;
        private int x, y, dir;
        private Set<String> cleaned = new HashSet<>();
        
        // Direction: 0=up, 1=right, 2=down, 3=left
        private static final int[][] DIRS = {{-1,0},{0,1},{1,0},{0,-1}};
        
        public MockRobot(int[][] room, int startX, int startY, int startDir) {
            this.room = room;
            this.x = startX;
            this.y = startY;
            this.dir = startDir;
        }
        
        public boolean move() {
            int newX = x + DIRS[dir][0];
            int newY = y + DIRS[dir][1];
            
            if (newX < 0 || newX >= room.length || 
                newY < 0 || newY >= room[0].length || 
                room[newX][newY] == 0) {
                return false;
            }
            
            x = newX;
            y = newY;
            return true;
        }
        
        public void turnLeft() {
            dir = (dir + 3) % 4; // -1 mod 4 = 3
        }
        
        public void turnRight() {
            dir = (dir + 1) % 4;
        }
        
        public void clean() {
            cleaned.add(x + "," + y);
        }
        
        public Set<String> getCleaned() {
            return cleaned;
        }
    }
    
    // Test cases
    public static void main(String[] args) {
        RobotRoomCleaner solution = new RobotRoomCleaner();
        
        System.out.println("=== ROBOT ROOM CLEANER TEST CASES ===\n");
        
        // Test Case 1: Simple 3x3 room with obstacle
        System.out.println("Test 1: 3x3 room with obstacle");
        int[][] room1 = {
            {1, 1, 1},
            {1, 1, 0},
            {1, 1, 1}
        };
        System.out.println("Room layout (1=clean, 0=wall):");
        printRoom(room1);
        MockRobot robot1 = new MockRobot(room1, 1, 1, 0);
        solution.cleanRoom(robot1);
        System.out.println("Cleaned cells: " + robot1.getCleaned().size());
        System.out.println("Expected: 8 cells (all except obstacle)");
        System.out.println();
        
        // Test Case 2: L-shaped room
        System.out.println("Test 2: L-shaped room");
        int[][] room2 = {
            {1, 1, 0},
            {1, 1, 1},
            {0, 0, 1}
        };
        System.out.println("Room layout:");
        printRoom(room2);
        MockRobot robot2 = new MockRobot(room2, 1, 1, 0);
        solution.cleanRoom(robot2);
        System.out.println("Cleaned cells: " + robot2.getCleaned().size());
        System.out.println("Expected: 5 cells");
        System.out.println();
        
        // Test Case 3: Single cell
        System.out.println("Test 3: Single cell room");
        int[][] room3 = {{1}};
        System.out.println("Room layout:");
        printRoom(room3);
        MockRobot robot3 = new MockRobot(room3, 0, 0, 0);
        solution.cleanRoom(robot3);
        System.out.println("Cleaned cells: " + robot3.getCleaned().size());
        System.out.println("Expected: 1 cell");
        System.out.println();
        
        // Test Case 4: Corridor
        System.out.println("Test 4: Horizontal corridor");
        int[][] room4 = {{1, 1, 1, 1, 1}};
        System.out.println("Room layout:");
        printRoom(room4);
        MockRobot robot4 = new MockRobot(room4, 0, 2, 1);
        solution.cleanRoom(robot4);
        System.out.println("Cleaned cells: " + robot4.getCleaned().size());
        System.out.println("Expected: 5 cells");
        System.out.println();
        
        System.out.println("=== KEY CONCEPTS ===");
        System.out.println("1. Use DFS with backtracking");
        System.out.println("2. Track visited with relative coordinates");
        System.out.println("3. Maintain robot direction throughout");
        System.out.println("4. Backtrack = go back + restore direction");
        System.out.println("5. Try all 4 directions from each cell");
    }
    
    private static void printRoom(int[][] room) {
        for (int[] row : room) {
            for (int cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }
}

/**
 * DETAILED ALGORITHM WALKTHROUGH
 * ==============================
 * 
 * Let's trace through a simple example step-by-step:
 * 
 * Room:
 *   0 1 2
 * 0 1 1 1
 * 1 1 1 0
 * 2 1 0 1
 * 
 * Robot starts at (1,1) facing UP (direction 0)
 * In our relative coordinates: (0,0)
 * 
 * EXECUTION TRACE:
 * ================
 * 
 * Call: dfs(0, 0, 0) - Start position
 * -------------------------------
 * - Clean (0,0) ✓
 * - Mark visited: {(0,0)}
 * - Try direction 0 (UP):
 *   - Next cell: (-1,0) - our (0,0) in relative coords
 *   - Not visited, move() succeeds
 *   - Call: dfs(-1, 0, 0)
 * 
 *     Call: dfs(-1, 0, 0) - One cell up
 *     -------------------------------
 *     - Clean (-1,0) ✓
 *     - Mark visited: {(0,0), (-1,0)}
 *     - Try direction 0 (UP): wall, skip
 *     - Turn right (now facing RIGHT)
 *     - Try direction 1 (RIGHT):
 *       - Next cell: (-1,1)
 *       - Not visited, move() succeeds
 *       - Call: dfs(-1, 1, 1)
 *       
 *         Call: dfs(-1, 1, 1) - Top right
 *         -------------------------------
 *         - Clean (-1,1) ✓
 *         - Mark visited: {(0,0), (-1,0), (-1,1)}
 *         - Try all 4 directions (all walls or visited)
 *         - Return
 *         
 *       - goBack() to (-1,0)
 *     - Turn right (now facing DOWN)
 *     - Try direction 2 (DOWN): already visited (0,0)
 *     - Turn right (now facing LEFT)
 *     - Try direction 3 (LEFT): wall
 *     - Turn right (now facing UP again - original direction)
 *     - Return
 *     
 *   - goBack() to (0,0)
 * 
 * - Turn right (now facing RIGHT)
 * - Try direction 1 (RIGHT):
 *   - Next cell: (0,1)
 *   - Not visited, move() succeeds
 *   - Call: dfs(0, 1, 1)
 *   
 *     Call: dfs(0, 1, 1) - One cell right
 *     -------------------------------
 *     - Clean (0,1) ✓
 *     - Mark visited: {(0,0), (-1,0), (-1,1), (0,1)}
 *     - Try direction 1 (RIGHT): wall
 *     - Turn right, try direction 2 (DOWN):
 *       - Next cell: (1,1)
 *       - Not visited, move() succeeds
 *       - Call: dfs(1, 1, 2)
 *       
 *         Call: dfs(1, 1, 2) - Bottom right
 *         -------------------------------
 *         - Clean (1,1) ✓
 *         - Mark visited: {(0,0), (-1,0), (-1,1), (0,1), (1,1)}
 *         - Try all 4 directions (all walls or visited)
 *         - Return
 *         
 *       - goBack() to (0,1)
 *     - Continue exploring...
 *     - Return
 *     
 *   - goBack() to (0,0)
 * 
 * - Turn right (now facing DOWN)
 * - Try direction 2 (DOWN):
 *   - Next cell: (1,0)
 *   - Not visited, move() succeeds
 *   - Call: dfs(1, 0, 2)
 *   
 *     Call: dfs(1, 0, 2) - One cell down
 *     -------------------------------
 *     - Clean (1,0) ✓
 *     - Mark visited: {(0,0), (-1,0), (-1,1), (0,1), (1,1), (1,0)}
 *     - Try all 4 directions...
 *     - Return
 *     
 *   - goBack() to (0,0)
 * 
 * - Turn right (now facing LEFT)
 * - Try direction 3 (LEFT):
 *   - Next cell: (0,-1)
 *   - Not visited, move() succeeds
 *   - Call: dfs(0, -1, 3)
 *   ... and so on
 * 
 * Final visited set contains all reachable cells!
 * Robot ends at starting position facing starting direction.
 */

/**
 * INTERVIEW STRATEGY GUIDE
 * ========================
 * 
 * 1. CLARIFY THE PROBLEM (3 minutes)
 *    Q: "Do I know the room layout?" A: No
 *    Q: "Do I know starting position?" A: No
 *    Q: "Can I use coordinates?" A: Yes, but relative to start
 *    Q: "Should I return to start?" A: Not required, but good practice
 *    Q: "Can cells be visited multiple times?" A: Physically yes, but we track visited
 * 
 * 2. EXPLAIN HIGH-LEVEL APPROACH (5 minutes)
 *    "This is a DFS/backtracking problem in an unknown space"
 *    "I'll use relative coordinates - start is (0,0)"
 *    "For each cell: clean it, explore all 4 directions, backtrack"
 *    "Key insight: after exploring, must return to exact same position AND direction"
 * 
 * 3. DISCUSS DIRECTION TRACKING (3 minutes)
 *    "I'll track which way robot is facing: 0=up, 1=right, 2=down, 3=left"
 *    "This matches turnRight() which rotates clockwise"
 *    "Direction vectors help calculate next position"
 * 
 * 4. EXPLAIN BACKTRACKING (5 minutes)
 *    "After exploring a direction, must backtrack"
 *    "Backtrack = turn 180°, move, turn 180° again"
 *    "This ensures robot returns to same position AND direction"
 *    [Draw diagram showing this]
 * 
 * 5. CODE THE SOLUTION (15 minutes)
 *    - Start with main structure (DFS function)
 *    - Add direction tracking
 *    - Implement backtracking logic
 *    - Add visited set
 * 
 * 6. TRACE THROUGH EXAMPLE (5 minutes)
 *    Use simple 2x2 room
 *    Show how DFS explores and backtracks
 * 
 * 7. DISCUSS COMPLEXITY (2 minutes)
 *    Time: O(N - M) where N = cells, M = obstacles
 *    Space: O(N - M) for visited set and recursion
 * 
 * 
 * COMMON MISTAKES TO AVOID
 * ========================
 * 
 * 1. FORGETTING TO TRACK DIRECTION
 *    ❌ Just track position
 *    ✓  Track both position AND direction
 *    
 *    Why? Need to know which way robot faces to calculate next position
 * 
 * 2. INCOMPLETE BACKTRACKING
 *    ❌ Just move back:
 *        robot.turnRight(); robot.turnRight(); robot.move();
 *    ✓  Move back AND restore direction:
 *        robot.turnRight(); robot.turnRight(); robot.move();
 *        robot.turnRight(); robot.turnRight();
 *    
 *    Why? After backtracking, must face same direction as before
 * 
 * 3. USING ABSOLUTE COORDINATES
 *    ❌ Try to track actual grid position
 *    ✓  Use relative coordinates (start = 0,0)
 *    
 *    Why? Don't know actual starting position
 * 
 * 4. NOT TURNING BETWEEN DIRECTIONS
 *    ❌ Try all directions without turning robot
 *    ✓  Turn right after each direction attempt
 *    
 *    Why? Robot needs to physically face each direction to try it
 * 
 * 5. WRONG DIRECTION INDEXING
 *    ❌ Random order of directions
 *    ✓  Clockwise order: up, right, down, left
 *    
 *    Why? Matches turnRight() behavior
 * 
 * 
 * FOLLOW-UP QUESTIONS TO PREPARE FOR
 * ==================================
 * 
 * Q: "What if robot can sense obstacles without moving?"
 * A: Could optimize by checking before moving, but same algorithm
 * 
 * Q: "What if room is very large?"
 * A: Visited set could be large, might use coordinate compression
 *    But for reachable cells, can't do better than visiting each once
 * 
 * Q: "Can you use BFS instead of DFS?"
 * A: Possible but awkward - need to track how to reach each cell
 *    DFS with backtracking is more natural for this problem
 * 
 * Q: "What if there are multiple disconnected areas?"
 * A: This solution only cleans connected component containing start
 *    To clean all, would need multiple starting points (not possible with one robot)
 * 
 * Q: "How do you handle a very deep recursion?"
 * A: Could use iterative approach with explicit stack (shown in code)
 *    In practice, rooms aren't deep enough to cause stack overflow
 * 
 * Q: "What's the maximum number of moves?"
 * A: Each cell visited once: N-M moves forward
 *    Each cell requires 4 direction checks: 4*(N-M) turns
 *    Backtracking: (N-M) goBack operations = 2*(N-M) moves
 *    Total: ~7*(N-M) robot operations
 * 
 * 
 * KEY TAKEAWAYS
 * =============
 * 
 * 1. This is a classic DFS + backtracking problem
 * 2. Relative coordinates are key (don't know actual position)
 * 3. Must track direction to calculate next position
 * 4. Backtracking must restore both position AND direction
 * 5. Visiting each cell exactly once is optimal
 * 6. Drawing diagrams helps immensely in interviews
 * 
 * This problem tests:
 * - Graph traversal (DFS)
 * - Backtracking
 * - Coordinate systems
 * - State management (position + direction)
 * 
 * Good luck! 🤖
 */

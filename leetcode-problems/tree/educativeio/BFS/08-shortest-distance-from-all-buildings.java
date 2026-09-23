/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given an m x n integer grid where each cell contains:
 * - 0 representing empty land that can be freely traversed,
 * - 1 representing a building that cannot be passed through,
 * - 2 representing an obstacle that cannot be passed through.
 * 
 * You want to place a house on an empty land cell (0) such that the sum of 
 * shortest distances from the house to all buildings is minimized. 
 * Movement is restricted to four directions: up, down, left, and right.
 * 
 * Return the minimum total travel distance for such a placement. 
 * If no valid empty land cell can reach all buildings, return -1.
 * 
 * Constraints:
 * - m == grid.length, n == grid[i].length
 * - 1 <= m, n <= 50
 * - grid[i][j] is either 0, 1, or 2
 * - There will be at least 1 building in grid
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Can there be 0 empty land cells on the grid?"
 *   Prevents assuming a valid answer always exists; we must handle returning -1 gracefully.
 * - "Can buildings or obstacles be passed through?"
 *   Clarifies routing rules (No, they act as solid walls).
 * - "Are we allowed to mutate the input grid?"
 *   Dictates whether we can use in-place state tracking or if we need auxiliary memory.
 * - "Is it possible for a building to be completely walled off by obstacles (2)?"
 *   Confirms we need to actively check for reachability and fail early if a building is isolated.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We need to find the shortest paths between two sets of coordinates (Empty Lands and Buildings). 
 * If there are many empty lands, running a pathfinding algorithm from every single one will 
 * result in massive redundant exploration of the same grid.
 * 
 * --- APPROACH 1: BFS from Every Empty Land (The "Literal Translation") ---
 * 1. What I'd naturally try: Iterate through the grid. Every time I find a '0', launch a 
 *    Breadth-First Search (BFS). In that BFS, search outward until I've found every '1'. 
 *    Keep a running sum of the distances. Track the minimum sum across all '0's.
 * 2. Why it works: It perfectly simulates the problem description (placing a house and finding 
 *    distances to buildings).
 * 3. Why it's too slow: There can be up to 2,500 cells. If 2,400 are '0's and 100 are '1's, 
 *    we run 2,400 full-grid BFS traversals. We are constantly re-exploring the same paths.
 * 4. What work is being repeated: We traverse from Empty A to Building X, and then later traverse 
 *    from Empty B to Building X, re-walking much of the same intermediate empty space.
 * 5. Time Complexity: O(E * (M * N)) where E is the number of empty lands. In the worst case, 
 *    this is O((M * N)^2).
 * 6. Space Complexity: O(M * N) — because each BFS requires a Queue and a `boolean[][] visited` 
 *    array of the grid's size.
 * 
 * --- APPROACH 2: BFS from Every Building (Reversing the Perspective) ---
 * 1. What I'd try next: Instead of sending explorers out from every empty lot to find buildings, 
 *    send explorers out from the buildings to tag the empty lots! I'll keep a global `int[][] distSum` 
 *    and an `int[][] reachCount`. I launch a BFS from each '1'. When a BFS reaches a '0', it adds 
 *    the distance to `distSum` and increments `reachCount`.
 * 2. Why it works: The distance from A to B is the same as B to A. After all buildings have run 
 *    their BFS, any '0' where `reachCount[i][j] == totalBuildings` is a valid spot. We just pick 
 *    the one with the lowest `distSum`.
 * 3. Why it's better: There are usually fewer buildings than empty lands. But more importantly, 
 *    it gives us a central repository (`distSum`) that accumulates answers incrementally.
 * 4. The Bottleneck: It still requires allocating a new `boolean[][] visited` matrix for every 
 *    single building's BFS. Furthermore, if Building 1 cannot reach a specific '0', that '0' is 
 *    already doomed. Yet, Building 2, 3, and 4 will blindly visit it and update it anyway.
 * 5. Time Complexity: O(B * (M * N)) where B is the number of buildings.
 * 6. Space Complexity: O(M * N) — for the Queue, `distSum`, `reachCount`, and the `visited` array.
 * 
 * --- APPROACH 3: Cascading Elimination BFS (The Optimal Way) ---
 * 1. The Core Property: If a '0' cannot be reached by Building A, we should NEVER let Building B 
 *    waste time stepping on it. 
 * 2. How it works: Instead of a `visited` matrix, we mutate the grid. We start looking for `0`. 
 *    Building 1 does its BFS and changes every '0' it touches to `-1`. 
 *    Building 2 does its BFS, but it only steps on `-1`, changing them to `-2`. 
 *    Building 3 only steps on `-2`, changing them to `-3`.
 * 3. Why it's optimal: 
 *    - No `visited` array is ever allocated.
 *    - Massive early pruning: The search space shrinks with every building. 
 *    - Instant Failure Detection: If Building K cannot find ANY cells with value `-(K-1)`, then 
 *      no cell can reach all buildings, and we can abort the entire algorithm instantly.
 * 4. Time Complexity: O(B * M * N) — Worst case is still bounding the same, but the practical 
 *    number of visited nodes drops exponentially with each iteration due to elimination.
 * 5. Space Complexity: O(M * N) — because of the `distSum` matrix and the BFS Queue. We eliminated 
 *    both `reachCount` and `visited` matrices.
 * 
 * [Which one I'd write in an interview]
 * Approach 3 (Cascading Elimination). It shows a deep mastery of state-tracking. By using grid 
 * values as generation counters (`0`, `-1`, `-2`), you solve the visited-state problem and the 
 * pruning problem simultaneously.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Isolated Building: A '1' surrounded by '2's. The BFS from this building will hit 0 valid empty 
 *   lands. Approach 3 detects this instantly and returns -1.
 * - Completely Blocked Empty Lands: Empty lands exist, but walls split the map into two unconnected 
 *   zones, each with buildings. Approach 3 will fail early when a building in Zone B tries to reach 
 *   the empty lands tagged by Zone A and finds none.
 * - Only 1 Building on the map: The code gracefully handles it, returning the minimum distance to 
 *   the closest adjacent empty land.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * Reverse the perspective (Multi-source -> Multi-target). To find the best house for all buildings, 
 * have all buildings broadcast their distance to the empty lots. Combine this with the "Walk State" 
 * trick (decrementing target cell values) to get in-place visitation tracking.
 * 
 * [Diagram & Dry Run]
 * Grid: 
 * 1 0 2
 * 0 0 0
 * 2 0 1
 * 
 * Init: emptyLandValue = 0, dist = all 0s.
 * 
 * BFS for Top-Left Building (1):
 * - Steps on (0,1). Changes to -1. dist(0,1) += 1.
 * - Steps on (1,0). Changes to -1. dist(1,0) += 1.
 * - Steps on (1,1). Changes to -1. dist(1,1) += 2.
 * - Steps on (1,2). Changes to -1. dist(1,2) += 3.
 * - Steps on (2,1). Changes to -1. dist(2,1) += 3.
 * Grid is now:
 *  1 -1  2
 * -1 -1 -1
 *  2 -1  1
 * 
 * emptyLandValue decrements to -1.
 * 
 * BFS for Bottom-Right Building (1):
 * - Looks ONLY for '-1'.
 * - Steps on (2,1). Changes to -2. dist(2,1) += 1. (Total = 4)
 * - Steps on (1,2). Changes to -2. dist(1,2) += 1. (Total = 4)
 * - Steps on (1,1). Changes to -2. dist(1,1) += 2. (Total = 4)
 * - Steps on (0,1). Changes to -2. dist(0,1) += 3. (Total = 4)
 * - Steps on (1,0). Changes to -2. dist(1,0) += 3. (Total = 4)
 * 
 * Minimum distance among all '-2' cells is 4. Returns 4.
 * 
 * [Pitfalls]
 * - Mutating the grid but forgetting to restore it if the problem requires the grid to remain 
 *   intact (Ask the interviewer! If grid must be preserved, use a `visited` array).
 * - Not checking `if (minDistance == Integer.MAX_VALUE)` after a building's BFS. This misses 
 *   the massive optimization of early-aborting when a building is isolated.
 * 
 * [Pattern Recognition]
 * When you see: "Minimize total distance to multiple targets on a grid".
 * Think: BFS from the targets, accumulating distances on the candidates.
 * 
 * [Interview Script]
 * "If we run a BFS from every empty lot, we'll repeat a ton of work. It's much more efficient 
 * to reverse the perspective: run a BFS from each building and accumulate distances on the empty 
 * lots. To optimize this further and avoid allocating a visited matrix every time, I'll use a 
 * cascading walk-state. The first building only walks on 0s and changes them to -1. The second 
 * building only walks on -1s and changes them to -2. This acts as both a visited flag and a 
 * strict filter: if Building 1 couldn't reach a lot, Building 2 won't even look at it. This 
 * guarantees we only ever process lots that are valid for ALL buildings processed so far."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if the grid is massive (10,000 x 10,000) but buildings are clustered in a small 50x50 area?
 * A: Our BFS would uselessly expand into the vast empty space. We could bound the BFS to a bounding 
 *    box of the buildings (plus a margin), or use A* if we were looking for a specific path.
 * 
 * Q: What if empty land had different movement costs (e.g., swamps take 3 steps)?
 * A: Standard BFS fails because edge weights are no longer uniform (1). We would have to upgrade 
 *    the search to Dijkstra's Algorithm using a PriorityQueue to ensure shortest paths.
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.LinkedList;
import java.util.Queue;

public class ShortestDistanceAllBuildings {

    /**
     * APPROACH 3: Cascading Elimination BFS (Optimal)
     */
    public static int shortestDistance(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return -1;
        }

        int m = grid.length;
        int n = grid[0].length;
        
        // dist[i][j] will accumulate the total distance from all buildings to cell (i, j)
        int[][] dist = new int[m][n];
        
        // emptyLandValue starts at 0. 
        // Building 1 changes valid 0s to -1.
        // Building 2 changes valid -1s to -2. etc.
        int emptyLandValue = 0;
        int minDistance = Integer.MAX_VALUE;
        
        // 4-directional movement (Up, Right, Down, Left)
        int[][] dirs = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                
                // When we find a building, launch a BFS
                if (grid[i][j] == 1) {
                    
                    // Reset minDistance for this building's traversal
                    minDistance = Integer.MAX_VALUE;
                    
                    Queue<int[]> queue = new LinkedList<>();
                    queue.offer(new int[]{i, j});
                    int steps = 0;

                    while (!queue.isEmpty()) {
                        int levelSize = queue.size();
                        steps++;
                        
                        for (int k = 0; k < levelSize; k++) {
                            int[] curr = queue.poll();
                            
                            for (int[] dir : dirs) {
                                int nx = curr[0] + dir[0];
                                int ny = curr[1] + dir[1];
                                
                                // We ONLY step on cells that match the current emptyLandValue.
                                // This automatically ignores walls (1, 2) AND empty lands 
                                // that failed to be reached by previous buildings.
                                if (nx >= 0 && nx < m && ny >= 0 && ny < n && grid[nx][ny] == emptyLandValue) {
                                    
                                    // Mark as visited by decrementing its grid value.
                                    // This preps it for the NEXT building's BFS.
                                    grid[nx][ny]--; 
                                    
                                    // Accumulate the distance
                                    dist[nx][ny] += steps;
                                    
                                    // Track the minimum total distance found in THIS generation
                                    minDistance = Math.min(minDistance, dist[nx][ny]);
                                    
                                    queue.offer(new int[]{nx, ny});
                                }
                            }
                        }
                    }
                    
                    // EARLY EXIT: If this building could not reach a single valid empty land,
                    // minDistance remains MAX_VALUE. This means NO cell can reach all buildings.
                    if (minDistance == Integer.MAX_VALUE) {
                        return -1;
                    }
                    
                    // Prep the required grid value for the next building
                    emptyLandValue--;
                }
            }
        }

        return minDistance == Integer.MAX_VALUE ? -1 : minDistance;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        
        // Test Case 1: Standard layout from Dry Run
        // 1 0 2
        // 0 0 0
        // 2 0 1
        int[][] grid1 = {
            {1, 0, 2},
            {0, 0, 0},
            {2, 0, 1}
        };
        System.out.println("Test 1 (Standard): " + shortestDistance(grid1)); 
        // Expected: 4

        // Test Case 2: Impossible due to isolation
        // 1 2 0
        // 2 0 0
        // 0 0 1
        // The top-left building is walled off by '2's.
        int[][] grid2 = {
            {1, 2, 0},
            {2, 0, 0},
            {0, 0, 1}
        };
        System.out.println("Test 2 (Isolated): " + shortestDistance(grid2)); 
        // Expected: -1

        // Test Case 3: Only one building
        // 0 0
        // 0 1
        int[][] grid3 = {
            {0, 0},
            {0, 1}
        };
        System.out.println("Test 3 (Single Building): " + shortestDistance(grid3)); 
        // Expected: 1 (Either (0,1) or (1,0) are 1 step away)
        
        // Test Case 4: No valid empty lands
        // 1 2
        // 2 1
        int[][] grid4 = {
            {1, 2},
            {2, 1}
        };
        System.out.println("Test 4 (No empty land): " + shortestDistance(grid4)); 
        // Expected: -1
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Multi-Source Accumulation (BFS from targets, not start points).
 * - Key observation: If Building K can't reach an empty plot, Building K+1 shouldn't 
 *   even check it. Decrementing grid values handles visited-state AND valid-state simultaneously.
 * - Most common trap: Forgetting to check if a building was completely isolated, resulting 
 *   in doing useless BFS runs for subsequent buildings.
 * - Mental trigger: "Min distance to ALL targets" -> Reverse BFS from targets + state elimination.
 */



import java.util.*;

/**
 * ============================================================
 * 01. BUILDINGS AND OBSTACLES
 * ============================================================
 *
 * Problem:
 *
 * Given a grid:
 *
 *     0 = empty land
 *     1 = building
 *     2 = obstacle
 *
 * We want to choose an empty cell (0) where we can build a house.
 *
 * The house must be reachable from ALL buildings.
 *
 * For every possible house position, calculate:
 *
 *     distance(building1, house)
 *   + distance(building2, house)
 *   + ...
 *   + distance(buildingK, house)
 *
 * Return the minimum possible sum.
 *
 * If no empty cell can reach ALL buildings:
 *
 *     return -1
 *
 *
 * ============================================================
 * 02. EXAMPLE
 * ============================================================
 *
 * Example:
 *
 *     grid =
 *
 *       1  0  2  0  1
 *       0  0  0  0  0
 *       0  0  1  0  0
 *
 *
 * Buildings:
 *
 *       B           B
 *       |           |
 *
 *       1  0  2  0  1
 *       0  0  0  0  0
 *       0  0  1  0  0
 *                 B
 *
 * We need to find an empty cell where the sum of the shortest
 * distances to all 3 buildings is minimum.
 *
 *
 * ============================================================
 * 03. FIRST THOUGHT: BFS FROM EVERY EMPTY CELL
 * ============================================================
 *
 * We could do:
 *
 *     for every 0 cell:
 *         BFS
 *         find distance to every building
 *         calculate total
 *
 * This works conceptually.
 *
 * But suppose there are many empty cells.
 *
 * Grid can be:
 *
 *     50 x 50 = 2500 cells
 *
 * In the worst case, we could perform BFS from almost every cell.
 *
 * That's potentially:
 *
 *     O((m*n) * (m*n))
 *
 * which is O((m*n)^2).
 *
 * We can do much better.
 *
 *
 * ============================================================
 * 04. BETTER IDEA: BFS FROM EACH BUILDING
 * ============================================================
 *
 * Instead of starting BFS from every possible house:
 *
 *     house -> all buildings
 *
 * Start BFS from every building:
 *
 *     building1 -> all empty cells
 *     building2 -> all empty cells
 *     building3 -> all empty cells
 *     ...
 *
 * BFS gives us the shortest distance from that building to every
 * reachable empty cell.
 *
 * We accumulate these distances.
 *
 *
 * Suppose we have:
 *
 *          B1
 *          |
 *          |
 *          X
 *         / \
 *        /   \
 *       X     X
 *
 * BFS from B1 gives:
 *
 *     distance(B1, X1)
 *     distance(B1, X2)
 *     distance(B1, X3)
 *     ...
 *
 * Then BFS from B2 gives another set of distances.
 *
 * We add them together.
 *
 *
 * ============================================================
 * 05. TWO ARRAYS
 * ============================================================
 *
 * We maintain:
 *
 *     totalDistance[r][c]
 *
 * Stores:
 *
 *     sum of distances from ALL buildings processed so far
 *
 *
 * And:
 *
 *     reachableBuildings[r][c]
 *
 * Stores:
 *
 *     number of buildings that can reach this cell
 *
 *
 * Example with 3 buildings:
 *
 *             totalDistance
 *
 *             5   7   10
 *             3   4   8
 *             9   6   2
 *
 *
 *             reachableBuildings
 *
 *             3   3   2
 *             3   3   1
 *             2   3   3
 *
 * A cell is a valid house location ONLY if:
 *
 *     reachableBuildings[r][c] == numberOfBuildings
 *
 * Then:
 *
 *     totalDistance[r][c]
 *
 * is the total distance to ALL buildings.
 *
 *
 * ============================================================
 * 06. WHY BFS?
 * ============================================================
 *
 * Movement is allowed only:
 *
 *     UP
 *     DOWN
 *     LEFT
 *     RIGHT
 *
 * Every movement costs exactly 1.
 *
 * Therefore this is an unweighted graph.
 *
 * BFS guarantees the shortest path.
 *
 *
 * Example:
 *
 *              B
 *              |
 *              1
 *             / \
 *            2   2
 *            |   |
 *            3   3
 *
 * BFS discovers cells in increasing distance:
 *
 *     distance 0
 *     distance 1
 *     distance 2
 *     distance 3
 *
 * Therefore the first time BFS reaches a cell, we have found
 * the shortest distance from the starting building.
 *
 *
 * ============================================================
 * 07. IMPORTANT: BUILDINGS AND OBSTACLES CANNOT BE TRAVERSED
 * ============================================================
 *
 * We start BFS from a building.
 *
 * But while expanding BFS, we can only move into EMPTY LAND.
 *
 * That means:
 *
 *     grid[nr][nc] == 0
 *
 * is the only cell we add to the BFS queue.
 *
 * We do NOT walk through:
 *
 *     1 = building
 *     2 = obstacle
 *
 * This is important because a building or obstacle blocks movement.
 *
 *
 * ============================================================
 * 08. IMPORTANT: WE MUST HANDLE DISCONNECTED AREAS
 * ============================================================
 *
 * Consider:
 *
 *     1  0  2  0  1
 *
 * The obstacle might completely separate the two buildings.
 *
 * Then some empty cells may be reachable from one building but
 * not from the other.
 *
 *
 * Example:
 *
 *     B1   0   2   0   B2
 *
 * From B1:
 *
 *     reachableBuildings = 1
 *
 * From B2:
 *
 *     reachableBuildings = 1
 *
 * But no cell gets:
 *
 *     reachableBuildings = 2
 *
 * Therefore no valid house exists.
 *
 * We return -1.
 *
 *
 * ============================================================
 * 09. OPTIMIZATION: DO NOT USE A SEPARATE DISTANCE MATRIX
 * ============================================================
 *
 * A normal BFS often uses:
 *
 *     int[][] distance
 *
 * But we don't actually need to keep the entire distance matrix.
 *
 * BFS can process the grid level by level.
 *
 * Example:
 *
 *     queue = cells at distance 0
 *
 *     process them
 *
 *     queue = cells at distance 1
 *
 *     process them
 *
 *     queue = cells at distance 2
 *
 * Therefore we keep:
 *
 *     distance
 *
 * representing the current BFS level.
 *
 * When processing all nodes in one level:
 *
 *     distance++
 *
 *
 * ============================================================
 * 10. BFS LEVEL-BY-LEVEL
 * ============================================================
 *
 * Suppose:
 *
 *     queue = [building]
 *
 * Initially:
 *
 *     distance = 0
 *
 * Process building:
 *
 *     its empty neighbors are distance 1
 *
 * Then:
 *
 *     distance = 1
 *
 * Process those cells.
 *
 * Their new neighbors are distance 2.
 *
 * And so on.
 *
 *
 * Visually:
 *
 *                 B
 *                 0
 *              1  1  1
 *              2  2  2
 *              3  3  3
 *
 * Every number represents the shortest distance from B.
 *
 *
 * ============================================================
 * 11. WALKTHROUGH
 * ============================================================
 *
 * Consider:
 *
 *     1  0  0
 *     0  0  0
 *     0  0  1
 *
 * There are two buildings:
 *
 *     B1 at (0,0)
 *     B2 at (2,2)
 *
 *
 * BFS FROM B1:
 *
 *     0  1  2
 *     1  2  3
 *     2  3  4
 *
 * So:
 *
 *     totalDistance =
 *
 *     0  1  2
 *     1  2  3
 *     2  3  4
 *
 *     reachableBuildings =
 *
 *     1  1  1
 *     1  1  1
 *     1  1  1
 *
 *
 * BFS FROM B2:
 *
 * Distances:
 *
 *     4  3  2
 *     3  2  1
 *     2  1  0
 *
 * Add them to totalDistance.
 *
 * Final:
 *
 *     4  4  4
 *     4  4  4
 *     4  4  4
 *
 * Every empty cell has total distance 4.
 *
 * Therefore answer = 4.
 *
 *
 * ============================================================
 * 12. MORE INTERESTING EXAMPLE
 * ============================================================
 *
 *     1  0  2  0  1
 *     0  0  0  0  0
 *     0  0  1  0  0
 *
 * Buildings:
 *
 *     B1 = (0,0)
 *     B2 = (0,4)
 *     B3 = (2,2)
 *
 * BFS from B1 calculates distances to all reachable empty cells.
 *
 * BFS from B2 adds its distances.
 *
 * BFS from B3 adds its distances.
 *
 * Suppose a particular cell:
 *
 *     X = (1,1)
 *
 * has:
 *
 *     distance from B1 = 2
 *     distance from B2 = 4
 *     distance from B3 = 2
 *
 * Therefore:
 *
 *     totalDistance[X] = 2 + 4 + 2
 *                      = 8
 *
 * If another valid cell has total:
 *
 *     7
 *
 * then that cell is better.
 *
 * We simply take the minimum over all cells that were reachable
 * from ALL buildings.
 *
 *
 * ============================================================
 * 13. WHY DO WE PROCESS BUILDINGS ONE BY ONE?
 * ============================================================
 *
 * Suppose there are K buildings.
 *
 * For each building:
 *
 *     BFS(building)
 *
 * Each BFS visits at most:
 *
 *     m * n
 *
 * cells.
 *
 * Therefore total work:
 *
 *     K * m * n
 *
 * Since:
 *
 *     K <= m * n
 *
 * worst case is:
 *
 *     O((m*n)^2)
 *
 * This is acceptable for m,n <= 50.
 *
 * Maximum cells:
 *
 *     50 * 50 = 2500
 *
 * So even the theoretical worst case is manageable.
 *
 *
 * ============================================================
 * 14. WHY NOT BFS FROM EMPTY CELLS?
 * ============================================================
 *
 * Both approaches can find the answer.
 *
 * Approach A:
 *
 *     BFS from every empty cell
 *
 *     O(E * m*n)
 *
 * where E = number of empty cells.
 *
 * Approach B:
 *
 *     BFS from every building
 *
 *     O(B * m*n)
 *
 * where B = number of buildings.
 *
 * We also accumulate results while doing the BFS.
 *
 * More importantly, this approach gives us a clean way to detect
 * cells that are unreachable from one or more buildings.
 *
 *
 * ============================================================
 * 15. CORRECTNESS
 * ============================================================
 *
 * For each building:
 *
 *     BFS finds the shortest distance from that building to every
 *     reachable empty cell.
 *
 * Therefore:
 *
 *     totalDistance[r][c]
 *
 * is exactly:
 *
 *     distance(building1, (r,c))
 *   + distance(building2, (r,c))
 *   + ...
 *
 * for all buildings that can reach (r,c).
 *
 * reachableBuildings[r][c] tells us exactly how many buildings
 * can reach the cell.
 *
 * Therefore a cell is a valid house location iff:
 *
 *     reachableBuildings[r][c] == numberOfBuildings
 *
 * Among those valid cells, we return the minimum totalDistance.
 *
 * Hence the result is the minimum possible sum of shortest
 * distances.
 *
 *
 * ============================================================
 * 16. EDGE CASES
 * ============================================================
 *
 * Case 1:
 *
 *     Only one building.
 *
 * We simply find the nearest reachable empty cell.
 *
 *
 * Case 2:
 *
 *     No empty cell can reach every building.
 *
 * Return:
 *
 *     -1
 *
 *
 * Case 3:
 *
 *     The starting building is surrounded by obstacles.
 *
 * Its BFS cannot reach any empty cell.
 *
 * Therefore no house location can use that building.
 *
 *
 * Case 4:
 *
 *     One building and at least one reachable empty cell.
 *
 * Answer is the shortest distance from that building to an
 * empty cell.
 *
 *
 * Case 5:
 *
 *     Obstacles divide the grid into disconnected regions.
 *
 * reachableBuildings[] detects this.
 *
 *
 * ============================================================
 * 17. COMPLEXITY
 * ============================================================
 *
 * Let:
 *
 *     M = m * n
 *     B = number of buildings
 *
 * Each building performs one BFS.
 *
 * Each BFS can visit O(M) cells.
 *
 * Time:
 *
 *     O(B * M)
 *
 * In terms of m and n:
 *
 *     O(B * m * n)
 *
 * Worst case:
 *
 *     O((m*n)^2)
 *
 * Space:
 *
 *     O(m*n)
 *
 * for:
 *
 *     queue
 *     totalDistance
 *     reachableBuildings
 *
 *
 * ============================================================
 * 18. IMPLEMENTATION
 * ============================================================
 */
public class BuildingsAndObstacles {

    private static final int[][] DIRECTIONS = {
        {-1, 0}, // up
        {1, 0},  // down
        {0, -1}, // left
        {0, 1}   // right
    };

    /**
     * Returns the minimum total distance from an empty land cell
     * to all buildings.
     *
     * Returns -1 if no empty cell can reach every building.
     */
    public int shortestDistance(int[][] grid) {

        int rows = grid.length;
        int cols = grid[0].length;

        /*
         * totalDistance[r][c]
         *
         * Stores the sum of shortest distances from every building
         * processed so far to cell (r,c).
         */
        int[][] totalDistance = new int[rows][cols];

        /*
         * reachableBuildings[r][c]
         *
         * Number of buildings that were able to reach this cell.
         */
        int[][] reachableBuildings = new int[rows][cols];

        int numberOfBuildings = 0;

        /*
         * --------------------------------------------------------
         * Step 1:
         * Count the buildings.
         * --------------------------------------------------------
         */
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                if (grid[r][c] == 1) {
                    numberOfBuildings++;
                }
            }
        }

        /*
         * --------------------------------------------------------
         * Step 2:
         * Run BFS from every building.
         * --------------------------------------------------------
         */
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                if (grid[r][c] == 1) {

                    bfsFromBuilding(
                        grid,
                        r,
                        c,
                        totalDistance,
                        reachableBuildings
                    );
                }
            }
        }

        /*
         * --------------------------------------------------------
         * Step 3:
         * Find the best empty cell.
         *
         * A valid house location must:
         *
         *     1. Be empty land (grid[r][c] == 0)
         *     2. Be reachable from ALL buildings
         *
         * Therefore:
         *
         *     reachableBuildings[r][c] == numberOfBuildings
         * --------------------------------------------------------
         */
        int answer = Integer.MAX_VALUE;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                if (grid[r][c] == 0
                        && reachableBuildings[r][c] == numberOfBuildings) {

                    answer = Math.min(
                        answer,
                        totalDistance[r][c]
                    );
                }
            }
        }

        /*
         * If answer was never updated, no valid empty cell exists.
         */
        return answer == Integer.MAX_VALUE ? -1 : answer;
    }

    /**
     * BFS starting from one building.
     *
     * We only walk through empty land cells (0).
     *
     * Every time we reach an empty cell:
     *
     *     totalDistance[r][c] += distance
     *     reachableBuildings[r][c]++
     *
     * This accumulates the contribution of this building.
     */
    private void bfsFromBuilding(
        int[][] grid,
        int startRow,
        int startCol,
        int[][] totalDistance,
        int[][] reachableBuildings
    ) {

        int rows = grid.length;
        int cols = grid[0].length;

        /*
         * We need to know which cells were already visited
         * during THIS building's BFS.
         *
         * We cannot reuse reachableBuildings as "visited"
         * because it represents visits from different buildings.
         */
        boolean[][] visited = new boolean[rows][cols];

        Queue<int[]> queue = new ArrayDeque<>();

        /*
         * Start BFS at the building.
         *
         * Distance from building to itself = 0.
         */
        queue.offer(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;

        int distance = 0;

        /*
         * --------------------------------------------------------
         * BFS LEVEL ORDER
         * --------------------------------------------------------
         *
         * Every iteration processes one distance level.
         *
         * Example:
         *
         * distance = 0
         *     building
         *
         * distance = 1
         *     neighboring cells
         *
         * distance = 2
         *     cells two steps away
         *
         * etc.
         */
        while (!queue.isEmpty()) {

            int levelSize = queue.size();

            /*
             * Process every cell at the current distance.
             */
            for (int i = 0; i < levelSize; i++) {

                int[] current = queue.poll();

                int row = current[0];
                int col = current[1];

                /*
                 * Explore all four directions.
                 */
                for (int[] direction : DIRECTIONS) {

                    int nextRow = row + direction[0];
                    int nextCol = col + direction[1];

                    /*
                     * Ignore cells outside the grid.
                     */
                    if (nextRow < 0 || nextRow >= rows
                            || nextCol < 0 || nextCol >= cols) {
                        continue;
                    }

                    /*
                     * Already visited during this BFS.
                     */
                    if (visited[nextRow][nextCol]) {
                        continue;
                    }

                    /*
                     * We can ONLY move through empty land.
                     *
                     * Therefore:
                     *
                     *     0 -> allowed
                     *
                     *     1 -> building, blocked
                     *     2 -> obstacle, blocked
                     */
                    if (grid[nextRow][nextCol] != 0) {
                        continue;
                    }

                    /*
                     * Mark as visited before adding to queue.
                     */
                    visited[nextRow][nextCol] = true;

                    /*
                     * This cell is reachable from the current
                     * building at:
                     *
                     *     distance + 1
                     */
                    totalDistance[nextRow][nextCol] += distance + 1;

                    /*
                     * One more building can reach this cell.
                     */
                    reachableBuildings[nextRow][nextCol]++;

                    /*
                     * Continue BFS from this cell.
                     */
                    queue.offer(new int[]{nextRow, nextCol});
                }
            }

            /*
             * We have finished the current distance level.
             *
             * Therefore all newly discovered cells are one step
             * farther away.
             */
            distance++;
        }
    }

    /*
     * ============================================================
     * Example usage
     * ============================================================
     *
     * int[][] grid = {
     *     {1, 0, 2, 0, 1},
     *     {0, 0, 0, 0, 0},
     *     {0, 0, 1, 0, 0}
     * };
     *
     * Buildings:
     *
     *     (0,0)
     *     (0,4)
     *     (2,2)
     *
     * Then:
     *
     *     BuildingsAndObstacles solution =
     *         new BuildingsAndObstacles();
     *
     *     int answer = solution.shortestDistance(grid);
     *
     * The answer is the minimum sum of shortest distances from
     * one empty cell to all three buildings.
     */
}


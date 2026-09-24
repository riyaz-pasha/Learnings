import java.util.ArrayDeque;
import java.util.Queue;

/*
 * ==========================================================================================
 *            SHORTEST PATH IN BINARY MATRIX: THE REASONING JOURNEY
 * ==========================================================================================
 *
 * 1. THE ESSENCE AND THE BINDING CONSTRAINT
 * ------------------------------------------------------------------------------------------
 * What is actually being asked?
 * We are navigating a grid from top-left (0,0) to bottom-right (N-1, N-1). We can move in
 * 8 directions. We can only step on 0s. We need the MINIMUM number of steps.
 *
 * The binding constraint:
 * N is up to 100. This means the grid has up to 10,000 cells. While that sounds small,
 * the number of possible *paths* through 10,000 cells branching in 8 directions is
 * astronomically huge (exponential). Any algorithm that tries to explore "every possible
 * path" to find the shortest one will completely freeze on a grid this size.
 *
 *
 * 2. THE NATURAL TRY: BRUTE FORCE (DEPTH-FIRST SEARCH)
 * ------------------------------------------------------------------------------------------
 * What would I naturally try?
 * "I need to find a path. I'll start at (0,0), try going in a valid direction, and keep
 * walking. If I hit a dead end, I'll backtrack and try another direction. If I reach the
 * end, I'll write down how many steps it took. I'll do this for EVERY path and just pick
 * the smallest number."
 *
 * Why does it work?
 * It literally checks every possible way to reach the destination without looping.
 *
 * Why is it too slow?
 * Consider a completely empty 10x10 grid. From almost every cell, you have 7 valid options
 * (excluding the one you just came from). You will wander in zig-zags, spirals, and loops
 * before finally stumbling into the target. The time complexity is roughly O(8^(N^2)).
 *
 * What work is being repeated?
 * Imagine exploring a path that takes 50 zig-zag steps to reach cell (5,5), and then exploring
 * all possible paths from (5,5) to the end. Later, you find a straight line that reaches
 * (5,5) in just 7 steps. You will once again explore ALL possible paths from (5,5) to the end!
 * We are repeatedly exploring the grid from the same intermediate cells.
 *
 *
 * 3. THE PIVOT: DISCOVERING THE OPTIMAL APPROACH
 * ------------------------------------------------------------------------------------------
 * What property/observation removes that bottleneck?
 * The key property is that EVERY STEP COSTS EXACTLY 1.
 *
 * Think about dropping a stone in a calm pond. The ripples expand outward in perfect circles.
 * The ripple doesn't reach a point 5 meters away, then suddenly jump back to a point 2 meters
 * away. It expands uniformly.
 *
 * The Question to ask yourself:
 * "Since all steps cost the same, what if I explore all paths of length 1, then all paths
 * of length 2, and so on? What does that guarantee?"
 *
 * The Correctness Intuition:
 * If I expand outward uniformly (length 1, length 2, length 3...), the VERY FIRST TIME my
 * search touches a cell, it is mathematically impossible for there to be a shorter path to
 * that cell. Why? Because if there were a shorter path, my expanding ripple would have
 * touched it during an earlier step!
 *
 * How does this lead to the next approach?
 * Because the first time we touch a cell is guaranteed to be the shortest path to it,
 * we NEVER need to process a cell more than once. When we visit a cell, we mark it "visited".
 * If we see it again via another path, we just ignore it.
 * This drops our work from "exploring all paths" (Exponential) to "looking at each cell once"
 * (Linear relative to the grid size, O(N^2)).
 *
 * This level-by-level expansion is the essence of Breadth-First Search (BFS).
 *
 *
 * 4. TEMPTING WRONG APPROACHES & PITFALLS
 * ------------------------------------------------------------------------------------------
 * Pitfall 1: DFS with Memoization.
 * "I'll just use DFS but cache the shortest path from each cell!"
 * Why it fails: DFS memoization works on Directed Acyclic Graphs (DAGs) like trees or grids
 * where you can only move Right and Down. But here, you can move in 8 directions. You create
 * cyclic dependencies (A depends on B, B depends on C, C depends on A). You can't easily
 * memoize an undirected graph without complex state management.
 *
 * Pitfall 2: Marking visited when POPPING from the queue, instead of when ADDING to it.
 * Why it fails: If you wait to mark a cell visited until you process it, multiple neighbors
 * might see the same cell as "unvisited" and add it to the queue simultaneously. Your queue
 * will explode with duplicate cells, causing an OutOfMemoryError or Time Limit Exceeded.
 * Always mark visited the MOMENT you put it in the queue.
 *
 *
 * 5. ASCII DIAGRAM & DRY RUN OF THE "RIPPLE"
 * ------------------------------------------------------------------------------------------
 * Grid:      Step 1:      Step 2:      Step 3:
 * S 0 0      1 . .        1 2 2        1 2 2
 * 0 1 0  ->  2 x .   ->   2 x 3   ->   2 x 3
 * 0 0 E      2 . .        2 3 .        2 3 4(E)
 * (S=Start, E=End, x=Obstacle, numbers=distance)
 *
 * Notice how the wave of numbers flows around the obstacle. The moment we write '4'
 * on the target cell, we are done. We don't even need to explore the rest of the grid.
 *
 *
 * 6. INTERVIEW SCRIPT: TALKING IT OUT
 * ------------------------------------------------------------------------------------------
 * YOU: "Since we're looking for the shortest path in a grid where every move has the same
 * weight (1 step), this is a classic unweighted shortest-path problem. A Depth-First Search
 * would explore paths blindly and exponentially re-evaluate cells. Instead, I want to use
 * a Breadth-First Search. By exploring level-by-level, the first time we reach the bottom
 * right, we are mathematically guaranteed it's the shortest path."
 *
 * INTERVIEWER: "Makes sense. How will you track visited cells?"
 *
 * YOU: "I could use an N x N boolean array. But if I'm allowed to modify the input grid,
 * I can just flip the 0s to 1s as I visit them to save O(N^2) space. Let's assume for
 * cleanliness and immutability I'll use a visited array, or I can do it in-place. Which
 * do you prefer?" (Always ask before mutating input!)
 *
 *
 * 7. FOLLOW-UPS YOU MIGHT GET
 * ------------------------------------------------------------------------------------------
 * Q: What if some cells are "muddy" and cost 3 steps to cross instead of 1?
 * A: BFS no longer works because steps don't have uniform cost. We'd transition to
 *    Dijkstra's Algorithm, replacing our standard Queue with a PriorityQueue ordered by cost.
 *
 * Q: What if the grid is massive (10,000 x 10,000) and we just want to know if a path exists?
 * A: If the start and end are known, a Bi-directional BFS (searching from start and end
 *    simultaneously until they intersect) drastically cuts down the search space area.
 *    Alternatively, A* search using a heuristic (like Chebyshev distance for 8-way grids)
 *    would guide the search directly toward the target instead of expanding in all directions.
 *
 *
 * 8. THE REUSABLE PATTERN
 * ------------------------------------------------------------------------------------------
 * WHEN YOU SEE: "Shortest path", "Minimum steps", "Fewest operations"
 * AND: All moves/transitions have the SAME COST.
 * THINK: Breadth-First Search (BFS).
 * TRANSFERS TO:
 * - Word Ladder (shortest path between words changing one letter)
 * - Minimum Knight Moves (shortest path for a chess knight)
 * - Rotting Oranges (how long until all oranges rot - a multi-source BFS)
 */

public class ShortestPathBinaryMatrix {

    // 8 directions: Top-Left, Top, Top-Right, Left, Right, Bottom-Left, Bottom, Bottom-Right
    private static final int[][] DIRECTIONS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    /**
     * THE OPTIMAL APPROACH: BREADTH-FIRST SEARCH
     *
     * Complexity Analysis:
     * - Time Complexity: O(N^2). In the worst case, we visit every cell in the N x N grid
     *   exactly once. From each cell, we check 8 directions (constant time). So O(8 * N^2) -> O(N^2).
     * - Space Complexity: O(N^2). The Queue can hold at most O(N^2) elements (specifically,
     *   the maximum perimeter of the BFS wave, which scales with N). If we don't modify the
     *   input grid, a `visited` array also takes O(N^2).
     */
    public int shortestPathBinaryMatrix(int[][] grid) {
        int n = grid.length;

        // Base Case / Edge Case check:
        // If the start or end is blocked, it's impossible to traverse.
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        // We use an ArrayDeque for our Queue. It's faster than LinkedList for standard operations.
        // The array will store [row, col, current_path_length].
        Queue<int[]> queue = new ArrayDeque<>();

        // Start at top-left. The problem defines the starting cell as length 1.
        queue.offer(new int[]{0, 0, 1});

        // Mutating the grid to 1 serves as our "visited" set. This saves O(N^2) memory.
        // We mark it visited IMMEDIATELY upon putting it in the queue.
        grid[0][0] = 1;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];
            int pathLength = current[2];

            // If we've reached the bottom-right corner, we are done.
            // Because this is BFS, we are mathematically guaranteed this is the shortest path.
            if (row == n - 1 && col == n - 1) {
                return pathLength;
            }

            // Explore all 8 adjacent neighbors (the "ripple" expanding)
            for (int[] dir : DIRECTIONS) {
                int nextRow = row + dir[0];
                int nextCol = col + dir[1];

                // Check boundaries and check if the cell is clear/unvisited (value 0)
                if (isValid(nextRow, nextCol, n) && grid[nextRow][nextCol] == 0) {

                    // CRITICAL: Mark visited immediately so other neighbors don't also add it
                    grid[nextRow][nextCol] = 1;

                    // Add the neighbor to the queue, incrementing the path length
                    queue.offer(new int[]{nextRow, nextCol, pathLength + 1});
                }
            }
        }

        // If the queue empties and we never reached (n-1, n-1), no path exists.
        return -1;
    }

    /**
     * Helper method to keep boundary checking clean.
     */
    private boolean isValid(int row, int col, int n) {
        return row >= 0 && row < n && col >= 0 && col < n;
    }

    /*
     * ==========================================================================================
     * SUMMARY & QUICK RECAP
     * ==========================================================================================
     * - CORE PATTERN: Shortest path on unweighted graph = BFS.
     * - KEY OBSERVATION: Expanding outward uniformly guarantees the first time we hit the target,
     *   it's the absolute shortest path, allowing us to never revisit cells.
     * - MEMORIZE VS UNDERSTAND: Don't memorize the while-loop; understand the "ripple" effect.
     *   If you understand the ripple, you will naturally know to use a Queue and track distances.
     * - MOST COMMON TRAP: Marking cells visited when POPPING them instead of when ADDING them.
     * - MENTAL TRIGGER: "Shortest path" + "Grid" = Queue + level-by-level search.
     */

    public static void main(String[] args) {
        ShortestPathBinaryMatrix solver = new ShortestPathBinaryMatrix();

        System.out.println("--- Testing Shortest Path in Binary Matrix ---");

        // Test 1: Simple clear path
        // 0 1
        // 1 0
        int[][] grid1 = {
                {0, 1},
                {1, 0}
        };
        // Expect 2 (Start (0,0) -> End (1,1) diagonally)
        System.out.println("Test 1 (Expected 2): " + solver.shortestPathBinaryMatrix(grid1));

        // Test 2: Blocked path
        // 0 0 0
        // 1 1 1
        // 0 0 0
        int[][] grid2 = {
                {0, 0, 0},
                {1, 1, 1},
                {0, 0, 0}
        };
        // Expect -1 (Wall blocks the way)
        System.out.println("Test 2 (Expected -1): " + solver.shortestPathBinaryMatrix(grid2));

        // Test 3: Winding path
        // 0 0 0
        // 0 1 0
        // 0 0 0
        int[][] grid3 = {
                {0, 0, 0},
                {0, 1, 0},
                {0, 0, 0}
        };
        // Expect 4
        System.out.println("Test 3 (Expected 4): " + solver.shortestPathBinaryMatrix(grid3));

        // Test 4: Edge case - Start is blocked
        int[][] grid4 = {
                {1, 0},
                {0, 0}
        };
        System.out.println("Test 4 (Expected -1): " + solver.shortestPathBinaryMatrix(grid4));

        // Test 5: Edge case - Grid is 1x1
        int[][] grid5 = {{0}};
        System.out.println("Test 5 (Expected 1): " + solver.shortestPathBinaryMatrix(grid5));
    }
}

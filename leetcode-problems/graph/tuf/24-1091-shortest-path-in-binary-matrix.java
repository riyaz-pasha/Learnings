import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

class ShortestPathInBinaryMatrixSolution {

    record Cell(int row, int col) {
    }

    public int shortestPathBinaryMatrix(int[][] grid) {
        int n = grid.length;
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1)
            return -1;

        Integer[][] dist = new Integer[n][n];
        Queue<Cell> queue = new ArrayDeque<>();

        queue.offer(new Cell(0, 0));
        dist[0][0] = 1; // start counts as step 1

        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 },
                { 0, -1 }, { 0, 1 },
                { 1, -1 }, { 1, 0 }, { 1, 1 }
        };

        while (!queue.isEmpty()) {
            Cell cell = queue.poll();
            int row = cell.row();
            int col = cell.col();

            if (row == n - 1 && col == n - 1) {
                return dist[row][col];
            }

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < n &&
                        newCol >= 0 && newCol < n &&
                        grid[newRow][newCol] == 0 &&
                        dist[newRow][newCol] == null) {

                    dist[newRow][newCol] = dist[row][col] + 1;
                    queue.offer(new Cell(newRow, newCol));
                }
            }
        }

        return -1; // no path found
    }

}

class ShortestPathBinaryMatrix {

    // 8 directions: right, left, down, up, and 4 diagonals
    private static final int[][] DIRECTIONS = {
            { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 }, // 4 cardinal directions
            { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } // 4 diagonal directions
    };

    // Solution 1: BFS (Breadth-First Search) - Most Common & Reliable
    // Time Complexity: O(n^2) where n is the grid dimension
    // Space Complexity: O(n^2) for the queue and visited tracking
    public int shortestPathBinaryMatrix(int[][] grid) {
        int n = grid.length;

        // Edge cases
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        // Special case: 1x1 grid
        if (n == 1) {
            return 1;
        }

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { 0, 0, 1 }); // {row, col, distance}
        grid[0][0] = 1; // Mark as visited (reusing grid)

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], dist = curr[2];

            // Try all 8 directions
            for (int[] dir : DIRECTIONS) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                // Check if reached destination
                if (newRow == n - 1 && newCol == n - 1) {
                    return dist + 1;
                }

                // Check bounds and if cell is valid
                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n
                        && grid[newRow][newCol] == 0) {
                    queue.offer(new int[] { newRow, newCol, dist + 1 });
                    grid[newRow][newCol] = 1; // Mark as visited
                }
            }
        }

        return -1; // No path found
    }

    // Solution 2: BFS with Separate Visited Array (Non-Destructive)
    // Time Complexity: O(n^2)
    // Space Complexity: O(n^2)
    public int shortestPathBinaryMatrixNonDestructive(int[][] grid) {
        int n = grid.length;

        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        if (n == 1) {
            return 1;
        }

        boolean[][] visited = new boolean[n][n];
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { 0, 0, 1 });
        visited[0][0] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], dist = curr[2];

            if (row == n - 1 && col == n - 1) {
                return dist;
            }

            for (int[] dir : DIRECTIONS) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n
                        && grid[newRow][newCol] == 0 && !visited[newRow][newCol]) {
                    queue.offer(new int[] { newRow, newCol, dist + 1 });
                    visited[newRow][newCol] = true;
                }
            }
        }

        return -1;
    }

    // Solution 3: A* Algorithm (Heuristic-based optimization)
    // Time Complexity: O(n^2 log n^2) due to priority queue
    // Space Complexity: O(n^2)
    public int shortestPathBinaryMatrixAStar(int[][] grid) {
        int n = grid.length;

        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        if (n == 1) {
            return 1;
        }

        // Priority queue: [row, col, distance, heuristic]
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> (a[2] + a[3]) - (b[2] + b[3])); // f(n) = g(n) + h(n)

        boolean[][] visited = new boolean[n][n];

        int heuristic = Math.max(Math.abs(n - 1 - 0), Math.abs(n - 1 - 0));
        pq.offer(new int[] { 0, 0, 1, heuristic });
        visited[0][0] = true;

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int row = curr[0], col = curr[1], dist = curr[2];

            if (row == n - 1 && col == n - 1) {
                return dist;
            }

            for (int[] dir : DIRECTIONS) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n
                        && grid[newRow][newCol] == 0 && !visited[newRow][newCol]) {

                    // Chebyshev distance (max of absolute differences)
                    int h = Math.max(Math.abs(n - 1 - newRow), Math.abs(n - 1 - newCol));
                    pq.offer(new int[] { newRow, newCol, dist + 1, h });
                    visited[newRow][newCol] = true;
                }
            }
        }

        return -1;
    }

    // Solution 4: Bidirectional BFS (Optimized for large grids)
    // Time Complexity: O(n^2) but typically faster in practice
    // Space Complexity: O(n^2)
    public int shortestPathBinaryMatrixBidirectional(int[][] grid) {
        int n = grid.length;

        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        if (n == 1) {
            return 1;
        }

        // Two queues: one from start, one from end
        Queue<int[]> queueStart = new LinkedList<>();
        Queue<int[]> queueEnd = new LinkedList<>();

        Set<String> visitedStart = new HashSet<>();
        Set<String> visitedEnd = new HashSet<>();

        queueStart.offer(new int[] { 0, 0 });
        queueEnd.offer(new int[] { n - 1, n - 1 });

        visitedStart.add("0,0");
        visitedEnd.add((n - 1) + "," + (n - 1));

        int steps = 1;

        while (!queueStart.isEmpty() || !queueEnd.isEmpty()) {
            // Expand from start
            int sizeStart = queueStart.isEmpty() ? 0 : queueStart.size();
            for (int i = 0; i < sizeStart; i++) {
                int[] curr = queueStart.poll();

                for (int[] dir : DIRECTIONS) {
                    int newRow = curr[0] + dir[0];
                    int newCol = curr[1] + dir[1];
                    String key = newRow + "," + newCol;

                    if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n
                            && grid[newRow][newCol] == 0 && !visitedStart.contains(key)) {

                        if (visitedEnd.contains(key)) {
                            return steps + 1;
                        }

                        queueStart.offer(new int[] { newRow, newCol });
                        visitedStart.add(key);
                    }
                }
            }

            // Expand from end
            int sizeEnd = queueEnd.isEmpty() ? 0 : queueEnd.size();
            for (int i = 0; i < sizeEnd; i++) {
                int[] curr = queueEnd.poll();

                for (int[] dir : DIRECTIONS) {
                    int newRow = curr[0] + dir[0];
                    int newCol = curr[1] + dir[1];
                    String key = newRow + "," + newCol;

                    if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n
                            && grid[newRow][newCol] == 0 && !visitedEnd.contains(key)) {

                        if (visitedStart.contains(key)) {
                            return steps + 1;
                        }

                        queueEnd.offer(new int[] { newRow, newCol });
                        visitedEnd.add(key);
                    }
                }
            }

            steps++;
        }

        return -1;
    }

    // Solution 5: DFS with Memoization (Not optimal for shortest path)
    // Time Complexity: O(n^2 * 4^(n^2)) - exponential without proper pruning
    // Space Complexity: O(n^2)
    // Note: DFS is generally NOT recommended for shortest path problems
    public int shortestPathBinaryMatrixDFS(int[][] grid) {
        int n = grid.length;

        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        if (n == 1) {
            return 1;
        }

        boolean[][] visited = new boolean[n][n];
        int result = dfs(grid, 0, 0, visited, n);
        return result == Integer.MAX_VALUE ? -1 : result;
    }

    private int dfs(int[][] grid, int row, int col, boolean[][] visited, int n) {
        if (row == n - 1 && col == n - 1) {
            return 1;
        }

        visited[row][col] = true;
        int minPath = Integer.MAX_VALUE;

        for (int[] dir : DIRECTIONS) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n
                    && grid[newRow][newCol] == 0 && !visited[newRow][newCol]) {

                int path = dfs(grid, newRow, newCol, visited, n);
                if (path != Integer.MAX_VALUE) {
                    minPath = Math.min(minPath, path + 1);
                }
            }
        }

        visited[row][col] = false; // Backtrack
        return minPath;
    }

    // Helper method to visualize path
    public void visualizePath(int[][] grid) {
        int n = grid.length;
        System.out.println("\nGrid Visualization:");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print(grid[i][j] == 0 ? "□ " : "■ ");
            }
            System.out.println();
        }
    }

    // Test cases
    public static void main(String[] args) {
        ShortestPathBinaryMatrix solution = new ShortestPathBinaryMatrix();

        // Test Case 1
        int[][] grid1 = { { 0, 1 }, { 1, 0 } };
        System.out.println("Example 1:");
        System.out.println("Input: grid = [[0,1],[1,0]]");
        solution.visualizePath(grid1);
        int[][] grid1Copy = copyGrid(grid1);
        System.out.println("Output (BFS): " + solution.shortestPathBinaryMatrix(grid1Copy));
        System.out.println("Output (A*): " + solution.shortestPathBinaryMatrixAStar(copyGrid(grid1)));
        System.out.println("Expected: 2\n");

        // Test Case 2
        int[][] grid2 = { { 0, 0, 0 }, { 1, 1, 0 }, { 1, 1, 0 } };
        System.out.println("Example 2:");
        System.out.println("Input: grid = [[0,0,0],[1,1,0],[1,1,0]]");
        solution.visualizePath(grid2);
        System.out.println("Output: " + solution.shortestPathBinaryMatrix(copyGrid(grid2)));
        System.out.println("Expected: 4\n");

        // Test Case 3
        int[][] grid3 = { { 1, 0, 0 }, { 1, 1, 0 }, { 1, 1, 0 } };
        System.out.println("Example 3:");
        System.out.println("Input: grid = [[1,0,0],[1,1,0],[1,1,0]]");
        solution.visualizePath(grid3);
        System.out.println("Output: " + solution.shortestPathBinaryMatrix(copyGrid(grid3)));
        System.out.println("Expected: -1 (blocked start)\n");

        // Additional Test Cases
        int[][] grid4 = { { 0 } };
        System.out.println("Edge Case (1x1 grid):");
        System.out.println("Output: " + solution.shortestPathBinaryMatrix(copyGrid(grid4)));
        System.out.println("Expected: 1\n");

        int[][] grid5 = {
                { 0, 0, 0, 0, 0 },
                { 1, 1, 0, 1, 0 },
                { 0, 0, 0, 0, 0 },
                { 0, 1, 1, 1, 0 },
                { 0, 0, 0, 0, 0 }
        };
        System.out.println("Complex Path (5x5):");
        solution.visualizePath(grid5);
        System.out.println("Output (BFS): " + solution.shortestPathBinaryMatrix(copyGrid(grid5)));
        System.out.println("Output (A*): " + solution.shortestPathBinaryMatrixAStar(copyGrid(grid5)));
        System.out.println("Output (Bidirectional): " +
                solution.shortestPathBinaryMatrixBidirectional(copyGrid(grid5)));

        // Performance comparison
        System.out.println("\n=== Performance Comparison (10x10 grid) ===");
        int[][] largeGrid = generateRandomGrid(10, 0.3);

        long start = System.nanoTime();
        int result1 = solution.shortestPathBinaryMatrix(copyGrid(largeGrid));
        long end = System.nanoTime();
        System.out.println("BFS: " + result1 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.shortestPathBinaryMatrixAStar(copyGrid(largeGrid));
        end = System.nanoTime();
        System.out.println("A*: " + result2 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result3 = solution.shortestPathBinaryMatrixBidirectional(copyGrid(largeGrid));
        end = System.nanoTime();
        System.out.println("Bidirectional BFS: " + result3 +
                " (Time: " + (end - start) / 1000 + " μs)");
    }

    // Helper methods
    private static int[][] copyGrid(int[][] grid) {
        int n = grid.length;
        int[][] copy = new int[n][n];
        for (int i = 0; i < n; i++) {
            copy[i] = grid[i].clone();
        }
        return copy;
    }

    private static int[][] generateRandomGrid(int n, double obstacleRatio) {
        int[][] grid = new int[n][n];
        Random rand = new Random(42); // Fixed seed for reproducibility
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                grid[i][j] = rand.nextDouble() < obstacleRatio ? 1 : 0;
            }
        }
        grid[0][0] = 0; // Ensure start is clear
        grid[n - 1][n - 1] = 0; // Ensure end is clear
        return grid;
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: BFS (RECOMMENDED)
 * - Time Complexity: O(n^2)
 * In worst case, we visit all cells once
 * Each cell can be added to queue at most once
 * - Space Complexity: O(n^2)
 * Queue can contain up to O(n^2) cells
 * Marks visited cells in-place or uses visited array
 * 
 * Solution 2: BFS with Separate Visited Array
 * - Time Complexity: O(n^2)
 * - Space Complexity: O(n^2)
 * Non-destructive - preserves original grid
 * Uses separate boolean array for visited tracking
 * 
 * Solution 3: A* Algorithm
 * - Time Complexity: O(n^2 log n^2)
 * Priority queue operations: O(log size)
 * Up to n^2 cells in worst case
 * - Space Complexity: O(n^2)
 * Priority queue and visited set
 * Often faster in practice due to heuristic guidance
 * 
 * Solution 4: Bidirectional BFS
 * - Time Complexity: O(n^2)
 * Same as regular BFS asymptotically
 * Often 2x faster in practice (searches from both ends)
 * - Space Complexity: O(n^2)
 * Two queues and two visited sets
 * 
 * Solution 5: DFS (NOT RECOMMENDED for shortest path)
 * - Time Complexity: O(4^(n^2)) without proper pruning
 * Exponential - explores many non-optimal paths
 * - Space Complexity: O(n^2)
 * Recursion stack depth
 * 
 * KEY INSIGHTS:
 * 
 * 1. Why BFS for Shortest Path?
 * - BFS explores level by level
 * - First time we reach destination = shortest path
 * - Guarantees optimal solution for unweighted graphs
 * 
 * 2. 8-Directional Movement:
 * - 4 cardinal: up, down, left, right
 * - 4 diagonal: NE, NW, SE, SW
 * - Total: 8 possible moves from each cell
 * 
 * 3. Edge Cases:
 * - Start or end cell is blocked (grid[0][0] or grid[n-1][n-1] == 1)
 * - 1x1 grid (special case)
 * - No path exists (return -1)
 * 
 * 4. In-Place vs Non-Destructive:
 * - In-place: mark visited by setting grid[i][j] = 1 (destructive)
 * - Non-destructive: use separate boolean[][] visited array
 * 
 * 5. Distance Tracking:
 * - Include distance in queue: {row, col, distance}
 * - OR count levels during BFS traversal
 * - Path length = number of cells visited (not edges)
 * 
 * 6. A* Heuristic:
 * - h(n) = Chebyshev distance to goal
 * - Chebyshev = max(|x1-x2|, |y1-y2|)
 * - Appropriate for 8-directional movement
 * 
 * 7. Bidirectional BFS:
 * - Search from both start and end simultaneously
 * - Meet in the middle → typically faster
 * - Good when start and end are both known
 * 
 * 8. When to Use Each Algorithm:
 * - BFS: Default choice, reliable and simple
 * - A*: When heuristic can guide search effectively
 * - Bidirectional: Large grids with clear start/end
 * - DFS: Never for shortest path (use for connectivity only)
 */
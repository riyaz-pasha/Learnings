
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

/*
 * You are a hiker preparing for an upcoming hike. You are given heights, a 2D
 * array of size rows x columns, where heights[row][col] represents the height
 * of cell (row, col). You are situated in the top-left cell, (0, 0), and you
 * hope to travel to the bottom-right cell, (rows-1, columns-1) (i.e.,
 * 0-indexed). You can move up, down, left, or right, and you wish to find a
 * route that requires the minimum effort.
 * 
 * A route's effort is the maximum absolute difference in heights between two
 * consecutive cells of the route.
 * 
 * Return the minimum effort required to travel from the top-left cell to the
 * bottom-right cell.
 * 
 * Example 1:
 * Input: heights = [[1,2,2],[3,8,2],[5,3,5]]
 * Output: 2
 * Explanation: The route of [1,3,5,3,5] has a maximum absolute difference of 2
 * in consecutive cells.
 * This is better than the route of [1,2,2,2,5], where the maximum absolute
 * difference is 3.
 * 
 * Example 2:
 * Input: heights = [[1,2,3],[3,8,4],[5,3,5]]
 * Output: 1
 * Explanation: The route of [1,2,3,4,5] has a maximum absolute difference of 1
 * in consecutive cells, which is better than route [1,3,5,3,5].
 * 
 * Example 3:
 * Input: heights =
 * [[1,2,1,1,1],[1,2,1,2,1],[1,2,1,2,1],[1,2,1,2,1],[1,1,1,2,1]]
 * Output: 0
 * Explanation: This route does not require any effort.
 */

class PathWithMinimumEffortSolution {

    record CellEfforts(int row, int col, int maxEffort) {
    }

    public int minimumEffortPath(int[][] heights) {
        int rows = heights.length;
        int cols = heights[0].length;

        // diff[r][c] = minimum possible "maximum edge effort" to reach (r,c)
        int[][] diff = new int[rows][cols];
        for (int[] row : diff)
            Arrays.fill(row, Integer.MAX_VALUE);

        PriorityQueue<CellEfforts> pq = new PriorityQueue<>(Comparator.comparingInt(CellEfforts::maxEffort));
        pq.offer(new CellEfforts(0, 0, 0));
        diff[0][0] = 0;

        int[][] directions = new int[][] { { -1, 0 }, { 1, 0 }, { 0, 1 }, { 0, -1 } };

        while (!pq.isEmpty()) {
            CellEfforts current = pq.poll();
            int r = current.row();
            int c = current.col();
            int curMax = current.maxEffort();

            // Skip stale entry: we already found a better way to (r,c)
            if (curMax > diff[r][c])
                continue;

            // If target reached, curMax is the answer
            if (r == rows - 1 && c == cols - 1) {
                return curMax;
            }

            for (int[] dir : directions) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    int edge = Math.abs(heights[r][c] - heights[nr][nc]);
                    int newMax = Math.max(curMax, edge);

                    // If this path gives a smaller "max edge" to (nr,nc), update & push
                    if (newMax < diff[nr][nc]) {
                        diff[nr][nc] = newMax;
                        pq.offer(new CellEfforts(nr, nc, newMax));
                    }
                }
            }
        }

        // For completeness: return computed value (or 0 for 1x1)
        return diff[rows - 1][cols - 1] == Integer.MAX_VALUE ? 0 : diff[rows - 1][cols - 1];
    }

}

class PathWithMinimumEffort {

    // 4 directions: up, down, left, right
    private static final int[][] DIRECTIONS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    // Solution 1: Dijkstra's Algorithm with Priority Queue (RECOMMENDED)
    // Time Complexity: O(m * n * log(m * n))
    // Space Complexity: O(m * n)
    public int minimumEffortPath(int[][] heights) {
        int rows = heights.length;
        int cols = heights[0].length;

        // Track minimum effort to reach each cell
        int[][] effort = new int[rows][cols];
        for (int[] row : effort) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        // Priority queue: {effort, row, col}
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        pq.offer(new int[] { 0, 0, 0 });
        effort[0][0] = 0;

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int currEffort = curr[0];
            int row = curr[1];
            int col = curr[2];

            // If we've reached the destination
            if (row == rows - 1 && col == cols - 1) {
                return currEffort;
            }

            // Skip if we've already found a better path to this cell
            if (currEffort > effort[row][col]) {
                continue;
            }

            // Explore all 4 directions
            for (int[] dir : DIRECTIONS) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    // Calculate effort for this path
                    int newEffort = Math.max(currEffort,
                            Math.abs(heights[newRow][newCol] - heights[row][col]));

                    // If we found a better path to this cell
                    if (newEffort < effort[newRow][newCol]) {
                        effort[newRow][newCol] = newEffort;
                        pq.offer(new int[] { newEffort, newRow, newCol });
                    }
                }
            }
        }

        return effort[rows - 1][cols - 1];
    }

    // Solution 2: Binary Search + BFS
    // Time Complexity: O(m * n * log(max_height))
    // Space Complexity: O(m * n)
    public int minimumEffortPathBinarySearch(int[][] heights) {
        int left = 0;
        int right = 1_000_000; // Max possible difference

        // Find actual max difference for optimization
        int maxDiff = 0;
        for (int i = 0; i < heights.length; i++) {
            for (int j = 0; j < heights[0].length; j++) {
                for (int[] dir : DIRECTIONS) {
                    int ni = i + dir[0], nj = j + dir[1];
                    if (ni >= 0 && ni < heights.length && nj >= 0 && nj < heights[0].length) {
                        maxDiff = Math.max(maxDiff,
                                Math.abs(heights[ni][nj] - heights[i][j]));
                    }
                }
            }
        }
        right = maxDiff;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (canReachWithEffort(heights, mid)) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    private boolean canReachWithEffort(int[][] heights, int maxEffort) {
        int rows = heights.length;
        int cols = heights[0].length;
        boolean[][] visited = new boolean[rows][cols];

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { 0, 0 });
        visited[0][0] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1];

            if (row == rows - 1 && col == cols - 1) {
                return true;
            }

            for (int[] dir : DIRECTIONS) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols
                        && !visited[newRow][newCol]) {

                    int diff = Math.abs(heights[newRow][newCol] - heights[row][col]);
                    if (diff <= maxEffort) {
                        visited[newRow][newCol] = true;
                        queue.offer(new int[] { newRow, newCol });
                    }
                }
            }
        }

        return false;
    }

    // Solution 3: Binary Search + DFS
    // Time Complexity: O(m * n * log(max_height))
    // Space Complexity: O(m * n)
    public int minimumEffortPathBinarySearchDFS(int[][] heights) {
        int left = 0;
        int right = 1_000_000;

        while (left < right) {
            int mid = left + (right - left) / 2;

            boolean[][] visited = new boolean[heights.length][heights[0].length];
            if (canReachWithEffortDFS(heights, 0, 0, mid, visited)) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    private boolean canReachWithEffortDFS(int[][] heights, int row, int col,
            int maxEffort, boolean[][] visited) {
        int rows = heights.length;
        int cols = heights[0].length;

        if (row == rows - 1 && col == cols - 1) {
            return true;
        }

        visited[row][col] = true;

        for (int[] dir : DIRECTIONS) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols
                    && !visited[newRow][newCol]) {

                int diff = Math.abs(heights[newRow][newCol] - heights[row][col]);
                if (diff <= maxEffort) {
                    if (canReachWithEffortDFS(heights, newRow, newCol, maxEffort, visited)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Solution 4: Union-Find (Kruskal's-like approach)
    // Time Complexity: O(m * n * log(m * n))
    // Space Complexity: O(m * n)
    public int minimumEffortPathUnionFind(int[][] heights) {
        int rows = heights.length;
        int cols = heights[0].length;

        if (rows == 1 && cols == 1) {
            return 0;
        }

        // Create list of all edges with their efforts
        List<int[]> edges = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int cell = i * cols + j;

                // Right neighbor
                if (j + 1 < cols) {
                    int neighbor = i * cols + (j + 1);
                    int effort = Math.abs(heights[i][j] - heights[i][j + 1]);
                    edges.add(new int[] { effort, cell, neighbor });
                }

                // Bottom neighbor
                if (i + 1 < rows) {
                    int neighbor = (i + 1) * cols + j;
                    int effort = Math.abs(heights[i][j] - heights[i + 1][j]);
                    edges.add(new int[] { effort, cell, neighbor });
                }
            }
        }

        // Sort edges by effort
        Collections.sort(edges, (a, b) -> a[0] - b[0]);

        UnionFind uf = new UnionFind(rows * cols);
        int start = 0;
        int end = (rows - 1) * cols + (cols - 1);

        for (int[] edge : edges) {
            int effort = edge[0];
            int cell1 = edge[1];
            int cell2 = edge[2];

            uf.union(cell1, cell2);

            if (uf.find(start) == uf.find(end)) {
                return effort;
            }
        }

        return 0;
    }

    // Union-Find data structure
    static class UnionFind {
        int[] parent;
        int[] rank;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
            }
        }
    }

    // Helper method to visualize the grid
    public void visualizeGrid(int[][] heights) {
        System.out.println("\nGrid Visualization:");
        for (int[] row : heights) {
            for (int val : row) {
                System.out.printf("%3d ", val);
            }
            System.out.println();
        }
    }

    // Helper method to find and display the actual path
    public List<int[]> findPath(int[][] heights) {
        int rows = heights.length;
        int cols = heights[0].length;

        int[][] effort = new int[rows][cols];
        int[][] parent = new int[rows][cols];

        for (int[] row : effort) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }
        for (int[] row : parent) {
            Arrays.fill(row, -1);
        }

        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        pq.offer(new int[] { 0, 0, 0 });
        effort[0][0] = 0;

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int currEffort = curr[0];
            int row = curr[1];
            int col = curr[2];

            if (row == rows - 1 && col == cols - 1) {
                break;
            }

            if (currEffort > effort[row][col]) {
                continue;
            }

            for (int[] dir : DIRECTIONS) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    int newEffort = Math.max(currEffort,
                            Math.abs(heights[newRow][newCol] - heights[row][col]));

                    if (newEffort < effort[newRow][newCol]) {
                        effort[newRow][newCol] = newEffort;
                        parent[newRow][newCol] = row * cols + col;
                        pq.offer(new int[] { newEffort, newRow, newCol });
                    }
                }
            }
        }

        // Reconstruct path
        List<int[]> path = new ArrayList<>();
        int curr = (rows - 1) * cols + (cols - 1);

        while (curr != -1) {
            int row = curr / cols;
            int col = curr % cols;
            path.add(0, new int[] { row, col, heights[row][col] });
            curr = parent[row][col];
        }

        return path;
    }

    // Test cases
    public static void main(String[] args) {
        PathWithMinimumEffort solution = new PathWithMinimumEffort();

        // Test Case 1
        int[][] heights1 = { { 1, 2, 2 }, { 3, 8, 2 }, { 5, 3, 5 } };
        System.out.println("Example 1:");
        System.out.println("Input: heights = [[1,2,2],[3,8,2],[5,3,5]]");
        solution.visualizeGrid(heights1);
        System.out.println("Output (Dijkstra): " + solution.minimumEffortPath(heights1));
        System.out.println("Output (Binary Search): " +
                solution.minimumEffortPathBinarySearch(heights1));
        System.out.println("Output (Union-Find): " +
                solution.minimumEffortPathUnionFind(heights1));

        List<int[]> path1 = solution.findPath(heights1);
        System.out.print("Path: ");
        for (int[] p : path1) {
            System.out.print("[" + p[0] + "," + p[1] + "]=" + p[2] + " -> ");
        }
        System.out.println("\nExpected: 2\n");

        // Test Case 2
        int[][] heights2 = { { 1, 2, 3 }, { 3, 8, 4 }, { 5, 3, 5 } };
        System.out.println("Example 2:");
        System.out.println("Input: heights = [[1,2,3],[3,8,4],[5,3,5]]");
        solution.visualizeGrid(heights2);
        System.out.println("Output: " + solution.minimumEffortPath(heights2));

        List<int[]> path2 = solution.findPath(heights2);
        System.out.print("Path: ");
        for (int[] p : path2) {
            System.out.print("[" + p[0] + "," + p[1] + "]=" + p[2] + " -> ");
        }
        System.out.println("\nExpected: 1\n");

        // Test Case 3
        int[][] heights3 = {
                { 1, 2, 1, 1, 1 },
                { 1, 2, 1, 2, 1 },
                { 1, 2, 1, 2, 1 },
                { 1, 2, 1, 2, 1 },
                { 1, 1, 1, 2, 1 }
        };
        System.out.println("Example 3:");
        System.out.println("Input: 5x5 grid");
        solution.visualizeGrid(heights3);
        System.out.println("Output: " + solution.minimumEffortPath(heights3));
        System.out.println("Expected: 0\n");

        // Edge cases
        int[][] heights4 = { { 1 } };
        System.out.println("Edge Case (1x1 grid):");
        System.out.println("Output: " + solution.minimumEffortPath(heights4));
        System.out.println("Expected: 0\n");

        int[][] heights5 = { { 1, 10, 6, 7, 9, 10, 4, 9 } };
        System.out.println("Edge Case (1xN grid):");
        System.out.println("Output: " + solution.minimumEffortPath(heights5));

        // Performance comparison
        System.out.println("\n=== Performance Comparison (10x10 grid) ===");
        int[][] largeGrid = generateRandomGrid(10, 10);

        long start = System.nanoTime();
        int result1 = solution.minimumEffortPath(largeGrid);
        long end = System.nanoTime();
        System.out.println("Dijkstra: " + result1 +
                " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.minimumEffortPathBinarySearch(largeGrid);
        end = System.nanoTime();
        System.out.println("Binary Search + BFS: " + result2 +
                " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result3 = solution.minimumEffortPathBinarySearchDFS(largeGrid);
        end = System.nanoTime();
        System.out.println("Binary Search + DFS: " + result3 +
                " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result4 = solution.minimumEffortPathUnionFind(largeGrid);
        end = System.nanoTime();
        System.out.println("Union-Find: " + result4 +
                " (Time: " + (end - start) / 1000 + " μs)");
    }

    // Helper method to generate random grid
    private static int[][] generateRandomGrid(int rows, int cols) {
        Random rand = new Random(42);
        int[][] grid = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = rand.nextInt(100) + 1;
            }
        }
        return grid;
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: Dijkstra's Algorithm (RECOMMENDED)
 * - Time Complexity: O(m * n * log(m * n))
 * Each cell can be added to priority queue at most once
 * Priority queue operations: O(log(m*n))
 * Total cells: m * n
 * - Space Complexity: O(m * n)
 * Effort array: O(m * n)
 * Priority queue: O(m * n) in worst case
 * 
 * Solution 2: Binary Search + BFS
 * - Time Complexity: O(m * n * log(H))
 * H = maximum possible height difference
 * Binary search: O(log H) iterations
 * Each BFS: O(m * n)
 * - Space Complexity: O(m * n)
 * BFS queue and visited array
 * 
 * Solution 3: Binary Search + DFS
 * - Time Complexity: O(m * n * log(H))
 * Similar to Binary Search + BFS
 * - Space Complexity: O(m * n)
 * Recursion stack and visited array
 * 
 * Solution 4: Union-Find (Kruskal's-like)
 * - Time Complexity: O(m * n * log(m * n))
 * Create edges: O(m * n)
 * Sort edges: O(m * n * log(m * n))
 * Union-Find operations: O(α(m*n)) ≈ O(1) amortized
 * - Space Complexity: O(m * n)
 * Edge list and Union-Find structure
 * 
 * KEY INSIGHTS:
 * 
 * 1. Problem Understanding:
 * - Find path where MAX difference between consecutive cells is MINIMIZED
 * - Different from shortest path (which minimizes SUM)
 * - We want to minimize the "bottleneck" edge
 * 
 * 2. Why Dijkstra Works:
 * - Modified Dijkstra where distance = max effort so far
 * - Instead of adding distances, we take max
 * - effort[new] = max(effort[curr], abs(height[new] - height[curr]))
 * 
 * 3. Binary Search Intuition:
 * - "Can we reach destination with max effort ≤ k?"
 * - Binary search on the answer (0 to max_possible_difference)
 * - For each k, check if path exists using BFS/DFS
 * 
 * 4. Union-Find Approach:
 * - Sort all edges by effort (like Kruskal's MST)
 * - Keep adding edges until start and end are connected
 * - The last edge added determines minimum effort
 * 
 * 5. Comparison of Approaches:
 * Dijkstra:
 * + Best for single query
 * + Finds optimal path directly
 * + Good time complexity
 * 
 * Binary Search:
 * + Good when max height is small
 * + Easy to understand
 * - May do redundant work
 * 
 * Union-Find:
 * + Elegant solution
 * + Good for multiple queries with same grid
 * - Slightly more complex to implement
 * 
 * 6. Edge Cases:
 * - 1x1 grid: return 0
 * - 1xN or Nx1 grid: only one path
 * - All same heights: return 0
 * 
 * 7. Optimization Tips:
 * - In Dijkstra: skip if current effort > recorded effort
 * - In Binary Search: calculate actual max difference for tighter bounds
 * - Early termination when destination is reached
 * 
 * 8. Real-world Applications:
 * - Route planning considering elevation changes
 * - Network routing with bandwidth constraints
 * - Image segmentation with intensity differences
 */

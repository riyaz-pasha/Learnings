import java.util.LinkedList;
import java.util.Queue;

/*
 * You are given an m x n binary matrix grid, where 0 represents a sea cell and
 * 1 represents a land cell.
 * 
 * A move consists of walking from one land cell to another adjacent
 * (4-directionally) land cell or walking off the boundary of the grid.
 * 
 * Return the number of land cells in grid for which we cannot walk off the
 * boundary of the grid in any number of moves.
 * 
 * Example 1:
 * Input: grid = [[0,0,0,0],[1,0,1,0],[0,1,1,0],[0,0,0,0]]
 * Output: 3
 * Explanation: There are three 1s that are enclosed by 0s, and one 1 that is
 * not enclosed because its on the boundary.
 * 
 * Example 2:
 * Input: grid = [[0,1,1,0],[0,0,1,0],[0,0,1,0],[0,0,0,0]]
 * Output: 0
 * Explanation: All 1s are either on the boundary or can reach the boundary.
 */

class NumberOfEnclavesSolution {

    public int numEnclaves(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;

        for (int row = 0; row < rows; row++) {
            if (grid[row][0] == 1) {
                dfs(grid, row, 0);
            }
            if (grid[row][cols - 1] == 1) {
                dfs(grid, row, cols - 1);
            }
        }

        for (int col = 0; col < cols; col++) {
            if (grid[0][col] == 1) {
                dfs(grid, 0, col);
            }
            if (grid[rows - 1][col] == 1) {
                dfs(grid, rows - 1, col);
            }
        }

        int count = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (grid[row][col] == 1) {
                    count++;
                }
            }
        }

        return count;

    }

    private void dfs(int[][] grid, int row, int col) {
        if (row < 0 || row >= grid.length || col < 0 || col >= grid[0].length || grid[row][col] == 0) {
            return;
        }
        grid[row][col] = 0;
        dfs(grid, row + 1, col);
        dfs(grid, row - 1, col);
        dfs(grid, row, col + 1);
        dfs(grid, row, col - 1);
    }

}

class NumberOfEnclaves {

    // =================================================================
    // APPROACH 1: DFS (Mark boundary-connected lands, then count)
    // Time: O(m*n)
    // Space: O(m*n) - recursion stack
    // =================================================================

    public int numEnclavesDFS(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        // Step 1: Mark all boundary-connected lands as 0 (visited)
        // Check first and last rows
        for (int j = 0; j < n; j++) {
            if (grid[0][j] == 1)
                dfs(grid, 0, j);
            if (grid[m - 1][j] == 1)
                dfs(grid, m - 1, j);
        }

        // Check first and last columns
        for (int i = 0; i < m; i++) {
            if (grid[i][0] == 1)
                dfs(grid, i, 0);
            if (grid[i][n - 1] == 1)
                dfs(grid, i, n - 1);
        }

        // Step 2: Count remaining 1s (enclosed lands)
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    count++;
                }
            }
        }

        return count;
    }

    private void dfs(int[][] grid, int i, int j) {
        int m = grid.length;
        int n = grid[0].length;

        // Boundary check
        if (i < 0 || i >= m || j < 0 || j >= n || grid[i][j] == 0) {
            return;
        }

        // Mark as visited (convert to 0)
        grid[i][j] = 0;

        // Explore all 4 directions
        dfs(grid, i - 1, j); // Up
        dfs(grid, i + 1, j); // Down
        dfs(grid, i, j - 1); // Left
        dfs(grid, i, j + 1); // Right
    }

    // =================================================================
    // APPROACH 2: BFS
    // Time: O(m*n)
    // Space: O(min(m,n)) - queue size
    // =================================================================

    public int numEnclavesBFS(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        Queue<int[]> queue = new LinkedList<>();

        // Step 1: Add all boundary lands to queue and mark as visited
        for (int j = 0; j < n; j++) {
            if (grid[0][j] == 1) {
                queue.offer(new int[] { 0, j });
                grid[0][j] = 0;
            }
            if (grid[m - 1][j] == 1) {
                queue.offer(new int[] { m - 1, j });
                grid[m - 1][j] = 0;
            }
        }

        for (int i = 1; i < m - 1; i++) {
            if (grid[i][0] == 1) {
                queue.offer(new int[] { i, 0 });
                grid[i][0] = 0;
            }
            if (grid[i][n - 1] == 1) {
                queue.offer(new int[] { i, n - 1 });
                grid[i][n - 1] = 0;
            }
        }

        // Step 2: BFS to mark all connected lands
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int i = curr[0], j = curr[1];

            for (int[] dir : dirs) {
                int ni = i + dir[0];
                int nj = j + dir[1];

                if (ni >= 0 && ni < m && nj >= 0 && nj < n && grid[ni][nj] == 1) {
                    grid[ni][nj] = 0;
                    queue.offer(new int[] { ni, nj });
                }
            }
        }

        // Step 3: Count remaining 1s
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    count++;
                }
            }
        }

        return count;
    }

    // =================================================================
    // APPROACH 3: DFS Non-Destructive (Use visited array)
    // Time: O(m*n)
    // Space: O(m*n)
    // =================================================================

    public int numEnclavesDFSNonDestructive(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        boolean[][] visited = new boolean[m][n];

        // Step 1: Mark all boundary-connected lands
        for (int j = 0; j < n; j++) {
            if (grid[0][j] == 1)
                dfsVisit(grid, visited, 0, j);
            if (grid[m - 1][j] == 1)
                dfsVisit(grid, visited, m - 1, j);
        }

        for (int i = 0; i < m; i++) {
            if (grid[i][0] == 1)
                dfsVisit(grid, visited, i, 0);
            if (grid[i][n - 1] == 1)
                dfsVisit(grid, visited, i, n - 1);
        }

        // Step 2: Count unvisited lands (enclosed)
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    count++;
                }
            }
        }

        return count;
    }

    private void dfsVisit(int[][] grid, boolean[][] visited, int i, int j) {
        int m = grid.length;
        int n = grid[0].length;

        if (i < 0 || i >= m || j < 0 || j >= n || grid[i][j] == 0 || visited[i][j]) {
            return;
        }

        visited[i][j] = true;

        dfsVisit(grid, visited, i - 1, j);
        dfsVisit(grid, visited, i + 1, j);
        dfsVisit(grid, visited, i, j - 1);
        dfsVisit(grid, visited, i, j + 1);
    }

    // =================================================================
    // APPROACH 4: Union-Find
    // Time: O(m*n * α(m*n))
    // Space: O(m*n)
    // =================================================================

    public int numEnclavesUnionFind(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        // Create dummy node for boundary-connected lands
        int dummyNode = m * n;
        UnionFind uf = new UnionFind(m * n + 1);

        // Step 1: Union all lands with adjacent lands
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int currIndex = i * n + j;

                    // If on boundary, union with dummy node
                    if (i == 0 || i == m - 1 || j == 0 || j == n - 1) {
                        uf.union(currIndex, dummyNode);
                    }

                    // Union with adjacent lands (only check right and down)
                    if (i + 1 < m && grid[i + 1][j] == 1) {
                        uf.union(currIndex, (i + 1) * n + j);
                    }
                    if (j + 1 < n && grid[i][j + 1] == 1) {
                        uf.union(currIndex, i * n + (j + 1));
                    }
                }
            }
        }

        // Step 2: Count lands not connected to dummy node
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1 && !uf.connected(i * n + j, dummyNode)) {
                    count++;
                }
            }
        }

        return count;
    }

    private static class UnionFind {
        private int[] parent;
        private int[] rank;

        public UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
                rank[i] = 1;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
            }
        }

        public boolean connected(int x, int y) {
            return find(x) == find(y);
        }
    }

    // =================================================================
    // APPROACH 5: Single Pass DFS (Count while marking)
    // Time: O(m*n)
    // Space: O(m*n)
    // =================================================================

    public int numEnclavesSinglePass(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        int totalLands = 0;
        int boundaryConnectedLands = 0;

        // Count total lands
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    totalLands++;
                }
            }
        }

        // Count boundary-connected lands
        for (int j = 0; j < n; j++) {
            if (grid[0][j] == 1)
                boundaryConnectedLands += dfsCount(grid, 0, j);
            if (grid[m - 1][j] == 1)
                boundaryConnectedLands += dfsCount(grid, m - 1, j);
        }

        for (int i = 1; i < m - 1; i++) {
            if (grid[i][0] == 1)
                boundaryConnectedLands += dfsCount(grid, i, 0);
            if (grid[i][n - 1] == 1)
                boundaryConnectedLands += dfsCount(grid, i, n - 1);
        }

        return totalLands - boundaryConnectedLands;
    }

    private int dfsCount(int[][] grid, int i, int j) {
        int m = grid.length;
        int n = grid[0].length;

        if (i < 0 || i >= m || j < 0 || j >= n || grid[i][j] == 0) {
            return 0;
        }

        grid[i][j] = 0;
        int count = 1;

        count += dfsCount(grid, i - 1, j);
        count += dfsCount(grid, i + 1, j);
        count += dfsCount(grid, i, j - 1);
        count += dfsCount(grid, i, j + 1);

        return count;
    }

    // =================================================================
    // TEST HELPER
    // =================================================================

    public static void main(String[] args) {
        NumberOfEnclaves sol = new NumberOfEnclaves();

        int[][] grid1 = {
                { 0, 0, 0, 0 },
                { 1, 0, 1, 0 },
                { 0, 1, 1, 0 },
                { 0, 0, 0, 0 }
        };

        System.out.println("Example 1:");
        System.out.println("DFS: " + sol.numEnclavesDFS(copyGrid(grid1)));
        System.out.println("BFS: " + sol.numEnclavesBFS(copyGrid(grid1)));
        System.out.println("DFS Non-Destructive: " + sol.numEnclavesDFSNonDestructive(copyGrid(grid1)));
        System.out.println("Union-Find: " + sol.numEnclavesUnionFind(copyGrid(grid1)));
        System.out.println("Single Pass: " + sol.numEnclavesSinglePass(copyGrid(grid1)));

        int[][] grid2 = {
                { 0, 1, 1, 0 },
                { 0, 0, 1, 0 },
                { 0, 0, 1, 0 },
                { 0, 0, 0, 0 }
        };

        System.out.println("\nExample 2:");
        System.out.println("DFS: " + sol.numEnclavesDFS(copyGrid(grid2)));
        System.out.println("BFS: " + sol.numEnclavesBFS(copyGrid(grid2)));
    }

    private static int[][] copyGrid(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        int[][] copy = new int[m][n];
        for (int i = 0; i < m; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, n);
        }
        return copy;
    }
}

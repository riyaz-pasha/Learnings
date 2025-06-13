import java.util.*;
/*
 * There is an m x n rectangular island that borders both the Pacific Ocean and
 * Atlantic Ocean. The Pacific Ocean touches the island's left and top edges,
 * and the Atlantic Ocean touches the island's right and bottom edges.
 * 
 * The island is partitioned into a grid of square cells. You are given an m x n
 * integer matrix heights where heights[r][c] represents the height above sea
 * level of the cell at coordinate (r, c).
 * 
 * The island receives a lot of rain, and the rain water can flow to neighboring
 * cells directly north, south, east, and west if the neighboring cell's height
 * is less than or equal to the current cell's height. Water can flow from any
 * cell adjacent to an ocean into the ocean.
 * 
 * Return a 2D list of grid coordinates result where result[i] = [ri, ci]
 * denotes that rain water can flow from cell (ri, ci) to both the Pacific and
 * Atlantic oceans.
 * 
 */

// Solution 1: DFS - Two separate traversals (Recommended)
// Time Complexity: O(m * n), Space Complexity: O(m * n)
class Solution {

    public List<List<Integer>> pacificAtlantic(int[][] heights) {
        int m = heights.length;
        int n = heights[0].length;

        // Track which cells can reach each ocean
        boolean[][] pacific = new boolean[m][n];
        boolean[][] atlantic = new boolean[m][n];

        // Start DFS from Pacific Ocean boundaries (top and left edges)
        for (int i = 0; i < m; i++) {
            dfs(heights, pacific, i, 0); // Left edge
        }
        for (int j = 0; j < n; j++) {
            dfs(heights, pacific, 0, j); // Top edge
        }

        // Start DFS from Atlantic Ocean boundaries (bottom and right edges)
        for (int i = 0; i < m; i++) {
            dfs(heights, atlantic, i, n - 1); // Right edge
        }
        for (int j = 0; j < n; j++) {
            dfs(heights, atlantic, m - 1, j); // Bottom edge
        }

        // Find cells that can reach both oceans
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (pacific[i][j] && atlantic[i][j]) {
                    result.add(Arrays.asList(i, j));
                }
            }
        }

        return result;
    }

    private void dfs(int[][] heights, boolean[][] visited, int i, int j) {
        // Mark current cell as reachable
        visited[i][j] = true;

        // Explore all 4 directions
        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int[] dir : directions) {
            int ni = i + dir[0];
            int nj = j + dir[1];

            // Check bounds, not visited, and water can flow (height >= current)
            if (ni >= 0 && ni < heights.length && nj >= 0 && nj < heights[0].length &&
                    !visited[ni][nj] && heights[ni][nj] >= heights[i][j]) {
                dfs(heights, visited, ni, nj);
            }
        }
    }

}

// Solution 2: BFS Approach
// Time Complexity: O(m * n), Space Complexity: O(m * n)
class Solution2 {

    public List<List<Integer>> pacificAtlantic(int[][] heights) {
        int m = heights.length;
        int n = heights[0].length;

        boolean[][] pacific = new boolean[m][n];
        boolean[][] atlantic = new boolean[m][n];

        Queue<int[]> pacificQueue = new LinkedList<>();
        Queue<int[]> atlanticQueue = new LinkedList<>();

        // Add Pacific Ocean boundary cells to queue
        for (int i = 0; i < m; i++) {
            pacificQueue.offer(new int[] { i, 0 });
            pacific[i][0] = true;
        }
        for (int j = 1; j < n; j++) { // j=1 to avoid duplicate corner
            pacificQueue.offer(new int[] { 0, j });
            pacific[0][j] = true;
        }

        // Add Atlantic Ocean boundary cells to queue
        for (int i = 0; i < m; i++) {
            atlanticQueue.offer(new int[] { i, n - 1 });
            atlantic[i][n - 1] = true;
        }
        for (int j = 0; j < n - 1; j++) { // j < n-1 to avoid duplicate corner
            atlanticQueue.offer(new int[] { m - 1, j });
            atlantic[m - 1][j] = true;
        }

        // BFS from Pacific boundaries
        bfs(heights, pacificQueue, pacific);

        // BFS from Atlantic boundaries
        bfs(heights, atlanticQueue, atlantic);

        // Find intersection
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (pacific[i][j] && atlantic[i][j]) {
                    result.add(Arrays.asList(i, j));
                }
            }
        }

        return result;
    }

    private void bfs(int[][] heights, Queue<int[]> queue, boolean[][] visited) {
        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int x = cell[0], y = cell[1];

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < heights.length && ny >= 0 && ny < heights[0].length &&
                        !visited[nx][ny] && heights[nx][ny] >= heights[x][y]) {
                    visited[nx][ny] = true;
                    queue.offer(new int[] { nx, ny });
                }
            }
        }
    }

}

// Solution 3: Single DFS with memoization for each cell
// Time Complexity: O(m * n), Space Complexity: O(m * n)
class Solution3 {

    private int[][] heights;
    private int m, n;
    private Boolean[][] pacificMemo;
    private Boolean[][] atlanticMemo;

    public List<List<Integer>> pacificAtlantic(int[][] heights) {
        this.heights = heights;
        this.m = heights.length;
        this.n = heights[0].length;
        this.pacificMemo = new Boolean[m][n];
        this.atlanticMemo = new Boolean[m][n];

        List<List<Integer>> result = new ArrayList<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (canReachPacific(i, j) && canReachAtlantic(i, j)) {
                    result.add(Arrays.asList(i, j));
                }
            }
        }

        return result;
    }

    private boolean canReachPacific(int i, int j) {
        if (pacificMemo[i][j] != null) {
            return pacificMemo[i][j];
        }

        // Base case: reached Pacific boundary
        if (i == 0 || j == 0) {
            pacificMemo[i][j] = true;
            return true;
        }

        boolean canReach = false;
        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        for (int[] dir : directions) {
            int ni = i + dir[0];
            int nj = j + dir[1];

            if (ni >= 0 && ni < m && nj >= 0 && nj < n &&
                    heights[ni][nj] <= heights[i][j] && canReachPacific(ni, nj)) {
                canReach = true;
                break;
            }
        }

        pacificMemo[i][j] = canReach;
        return canReach;
    }

    private boolean canReachAtlantic(int i, int j) {
        if (atlanticMemo[i][j] != null) {
            return atlanticMemo[i][j];
        }

        // Base case: reached Atlantic boundary
        if (i == m - 1 || j == n - 1) {
            atlanticMemo[i][j] = true;
            return true;
        }

        boolean canReach = false;
        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        for (int[] dir : directions) {
            int ni = i + dir[0];
            int nj = j + dir[1];

            if (ni >= 0 && ni < m && nj >= 0 && nj < n &&
                    heights[ni][nj] <= heights[i][j] && canReachAtlantic(ni, nj)) {
                canReach = true;
                break;
            }
        }

        atlanticMemo[i][j] = canReach;
        return canReach;
    }

}

// Solution 4: Union-Find approach (Advanced)
// Time Complexity: O(m * n * α(m*n)), Space Complexity: O(m * n)
class Solution4 {

    class UnionFind {
        int[] parent;
        int[] rank;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 0;
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

        public boolean connected(int x, int y) {
            return find(x) == find(y);
        }
    }

    public List<List<Integer>> pacificAtlantic(int[][] heights) {
        int m = heights.length;
        int n = heights[0].length;

        // Create two union-find structures for each ocean
        // Extra nodes: m*n for Pacific, m*n+1 for Atlantic
        UnionFind pacificUF = new UnionFind(m * n + 2);
        UnionFind atlanticUF = new UnionFind(m * n + 2);

        int pacificNode = m * n;
        int atlanticNode = m * n + 1;

        // Connect boundary cells to their respective oceans
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int cellId = i * n + j;

                // Connect to Pacific (top and left boundaries)
                if (i == 0 || j == 0) {
                    pacificUF.union(cellId, pacificNode);
                }

                // Connect to Atlantic (bottom and right boundaries)
                if (i == m - 1 || j == n - 1) {
                    atlanticUF.union(cellId, atlanticNode);
                }

                // Connect to adjacent cells if water can flow
                int[][] directions = { { 0, 1 }, { 1, 0 } };
                for (int[] dir : directions) {
                    int ni = i + dir[0];
                    int nj = j + dir[1];

                    if (ni < m && nj < n) {
                        int neighborId = ni * n + nj;

                        // If water can flow from current to neighbor
                        if (heights[ni][nj] <= heights[i][j]) {
                            pacificUF.union(cellId, neighborId);
                            atlanticUF.union(cellId, neighborId);
                        }

                        // If water can flow from neighbor to current
                        if (heights[i][j] <= heights[ni][nj]) {
                            pacificUF.union(neighborId, cellId);
                            atlanticUF.union(neighborId, cellId);
                        }
                    }
                }
            }
        }

        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int cellId = i * n + j;
                if (pacificUF.connected(cellId, pacificNode) &&
                        atlanticUF.connected(cellId, atlanticNode)) {
                    result.add(Arrays.asList(i, j));
                }
            }
        }

        return result;
    }

}

// Example usage and test cases
class TestPacificAtlantic {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int[][] heights1 = {
                { 1, 2, 2, 3, 5 },
                { 3, 2, 3, 4, 4 },
                { 2, 4, 5, 3, 1 },
                { 6, 7, 1, 4, 5 },
                { 5, 1, 1, 2, 4 }
        };
        System.out.println("Test 1: " + solution.pacificAtlantic(heights1));
        // Expected: [[0,4],[1,3],[1,4],[2,2],[3,0],[3,1],[4,0]]

        // Test case 2
        int[][] heights2 = {
                { 2, 1 },
                { 1, 2 }
        };
        System.out.println("Test 2: " + solution.pacificAtlantic(heights2));
        // Expected: [[0,0],[0,1],[1,0],[1,1]]

        // Test case 3 - Single cell
        int[][] heights3 = { { 1 } };
        System.out.println("Test 3: " + solution.pacificAtlantic(heights3));
        // Expected: [[0,0]]

        // Test case 4 - Ascending heights
        int[][] heights4 = {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 8, 9 }
        };
        System.out.println("Test 4: " + solution.pacificAtlantic(heights4));
        // Expected: [[0,2],[1,2],[2,0],[2,1],[2,2]]
    }

}

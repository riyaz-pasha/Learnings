import java.util.*;
/*
 * You are given an m x n binary matrix grid. An island is a group of 1's
 * (representing land) connected 4-directionally (horizontal or vertical.) You
 * may assume all four edges of the grid are surrounded by water.
 * 
 * The area of an island is the number of cells with a value 1 in the island.
 * 
 * Return the maximum area of an island in grid. If there is no island, return
 * 0.
 * Input: grid = [
 * [0,0,1,0,0,0,0,1,0,0,0,0,0],
 * [0,0,0,0,0,0,0,1,1,1,0,0,0],
 * [0,1,1,0,1,0,0,0,0,0,0,0,0],
 * [0,1,0,0,1,1,0,0,1,0,1,0,0],
 * [0,1,0,0,1,1,0,0,1,1,1,0,0],
 * [0,0,0,0,0,0,0,0,0,0,1,0,0],
 * [0,0,0,0,0,0,0,1,1,1,0,0,0],
 * [0,0,0,0,0,0,0,1,1,0,0,0,0]]
 * Output: 6
 * Explanation: The answer is not 11, because the island must be connected
 * 4-directionally.
 */

// Solution 1: DFS - Modify original grid (most efficient)
// Time Complexity: O(m * n), Space Complexity: O(m * n) for recursion stack
class Solution {

    public int maxAreaOfIsland(int[][] grid) {
        int maxArea = 0;
        int m = grid.length;
        int n = grid[0].length;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int currentArea = dfs(grid, i, j);
                    maxArea = Math.max(maxArea, currentArea);
                }
            }
        }

        return maxArea;
    }

    private int dfs(int[][] grid, int i, int j) {
        // Check bounds and if current cell is water or already visited
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length || grid[i][j] == 0) {
            return 0;
        }

        // Mark current cell as visited (convert to water)
        grid[i][j] = 0;

        // Count current cell + all connected cells
        return 1 + dfs(grid, i + 1, j) + // Down
                dfs(grid, i - 1, j) + // Up
                dfs(grid, i, j + 1) + // Right
                dfs(grid, i, j - 1); // Left
    }

}

// Solution 2: DFS with visited array (preserves original grid)
// Time Complexity: O(m * n), Space Complexity: O(m * n)
class Solution2 {

    public int maxAreaOfIsland(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        boolean[][] visited = new boolean[m][n];
        int maxArea = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    int currentArea = dfs(grid, visited, i, j);
                    maxArea = Math.max(maxArea, currentArea);
                }
            }
        }

        return maxArea;
    }

    private int dfs(int[][] grid, boolean[][] visited, int i, int j) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length ||
                visited[i][j] || grid[i][j] == 0) {
            return 0;
        }

        visited[i][j] = true;

        return 1 + dfs(grid, visited, i + 1, j) +
                dfs(grid, visited, i - 1, j) +
                dfs(grid, visited, i, j + 1) +
                dfs(grid, visited, i, j - 1);
    }

}

// Solution 3: BFS Approach
// Time Complexity: O(m * n), Space Complexity: O(m * n) for queue
class Solution3 {
    public int maxAreaOfIsland(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        int maxArea = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int currentArea = bfs(grid, i, j);
                    maxArea = Math.max(maxArea, currentArea);
                }
            }
        }

        return maxArea;
    }

    private int bfs(int[][] grid, int startI, int startJ) {
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { startI, startJ });
        grid[startI][startJ] = 0; // Mark as visited
        int area = 0;

        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int x = cell[0], y = cell[1];
            area++;

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < grid.length && ny >= 0 && ny < grid[0].length &&
                        grid[nx][ny] == 1) {
                    grid[nx][ny] = 0; // Mark as visited
                    queue.offer(new int[] { nx, ny });
                }
            }
        }

        return area;
    }
}

// Solution 4: Iterative DFS using Stack
// Time Complexity: O(m * n), Space Complexity: O(m * n) for stack
class Solution4 {

    public int maxAreaOfIsland(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        int maxArea = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int currentArea = iterativeDFS(grid, i, j);
                    maxArea = Math.max(maxArea, currentArea);
                }
            }
        }

        return maxArea;
    }

    private int iterativeDFS(int[][] grid, int startI, int startJ) {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[] { startI, startJ });
        grid[startI][startJ] = 0;
        int area = 0;

        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        while (!stack.isEmpty()) {
            int[] cell = stack.pop();
            int x = cell[0], y = cell[1];
            area++;

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < grid.length && ny >= 0 && ny < grid[0].length &&
                        grid[nx][ny] == 1) {
                    grid[nx][ny] = 0;
                    stack.push(new int[] { nx, ny });
                }
            }
        }

        return area;
    }

}

// Solution 5: Union-Find (Disjoint Set) approach
// Time Complexity: O(m * n * α(m*n)), Space Complexity: O(m * n)
class Solution5 {

    class UnionFind {
        int[] parent;
        int[] size;
        int maxSize;

        public UnionFind(int n) {
            parent = new int[n];
            size = new int[n];
            maxSize = 0;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
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
                if (size[rootX] < size[rootY]) {
                    int temp = rootX;
                    rootX = rootY;
                    rootY = temp;
                }
                parent[rootY] = rootX;
                size[rootX] += size[rootY];
                maxSize = Math.max(maxSize, size[rootX]);
            }
        }

        public void addNode(int x) {
            maxSize = Math.max(maxSize, size[find(x)]);
        }
    }

    public int maxAreaOfIsland(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        UnionFind uf = new UnionFind(m * n);

        int[][] directions = { { 0, 1 }, { 1, 0 } };

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int current = i * n + j;
                    uf.addNode(current);

                    for (int[] dir : directions) {
                        int ni = i + dir[0];
                        int nj = j + dir[1];

                        if (ni < m && nj < n && grid[ni][nj] == 1) {
                            int neighbor = ni * n + nj;
                            uf.union(current, neighbor);
                        }
                    }
                }
            }
        }

        return uf.maxSize;
    }

}

// Example usage and test cases
class TestMaxAreaIsland {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int[][] grid1 = {
                { 1, 1, 0, 0, 0 },
                { 1, 1, 0, 0, 0 },
                { 0, 0, 0, 1, 1 },
                { 0, 0, 0, 1, 1 }
        };
        System.out.println("Test 1: " + solution.maxAreaOfIsland(grid1)); // Expected: 4

        // Test case 2
        int[][] grid2 = {
                { 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0 },
                { 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
                { 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0 },
                { 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0 }
        };
        System.out.println("Test 2: " + solution.maxAreaOfIsland(grid2)); // Expected: 6

        // Test case 3 - No islands
        int[][] grid3 = {
                { 0, 0, 0, 0, 0, 0, 0, 0 }
        };
        System.out.println("Test 3: " + solution.maxAreaOfIsland(grid3)); // Expected: 0

        // Test case 4 - Single island
        int[][] grid4 = {
                { 1, 1, 1 },
                { 0, 1, 0 },
                { 0, 1, 0 }
        };
        System.out.println("Test 4: " + solution.maxAreaOfIsland(grid4)); // Expected: 5
    }

}

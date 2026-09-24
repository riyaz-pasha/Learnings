/*
 * Given an m x n 2D binary grid grid which represents a map of '1's (land) and
 * '0's (water), return the number of islands.
 * 
 * An island is surrounded by water and is formed by connecting adjacent lands
 * horizontally or vertically. You may assume all four edges of the grid are all
 * surrounded by water.
 * 
 * Example 1:
 * 
 * Input: grid = [
 * ["1","1","1","1","0"],
 * ["1","1","0","1","0"],
 * ["1","1","0","0","0"],
 * ["0","0","0","0","0"]
 * ]
 * Output: 1
 * 
 * Example 2:
 * 
 * Input: grid = [
 * ["1","1","0","0","0"],
 * ["1","1","0","0","0"],
 * ["0","0","1","0","0"],
 * ["0","0","0","1","1"]
 * ]
 * Output: 3
 */

import java.util.LinkedList;
import java.util.Queue;

// Solution 1: Depth-First Search (DFS) - Recursive
// Time Complexity: O(m × n) where m and n are grid dimensions
// Space Complexity: O(m × n) for recursion stack in worst case
class Solution1 {

    public int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0)
            return 0;

        int m = grid.length;
        int n = grid[0].length;
        int count = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    count++;
                    dfs(grid, i, j);
                }
            }
        }

        return count;
    }

    private void dfs(char[][] grid, int i, int j) {
        int m = grid.length;
        int n = grid[0].length;

        // Base case: out of bounds or not land
        if (i < 0 || i >= m || j < 0 || j >= n || grid[i][j] != '1') {
            return;
        }

        // Mark as visited by changing to '0'
        grid[i][j] = '0';

        // Recursively visit all 4 directions
        dfs(grid, i + 1, j); // down
        dfs(grid, i - 1, j); // up
        dfs(grid, i, j + 1); // right
        dfs(grid, i, j - 1); // left
    }

}

// Solution 2: Breadth-First Search (BFS) - Iterative
class Solution2 {

    public int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0)
            return 0;

        int m = grid.length;
        int n = grid[0].length;
        int count = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    count++;
                    bfs(grid, i, j);
                }
            }
        }

        return count;
    }

    private void bfs(char[][] grid, int startI, int startJ) {
        int m = grid.length;
        int n = grid[0].length;

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { startI, startJ });
        grid[startI][startJ] = '0'; // Mark as visited

        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int i = current[0];
            int j = current[1];

            for (int[] dir : directions) {
                int ni = i + dir[0];
                int nj = j + dir[1];

                if (ni >= 0 && ni < m && nj >= 0 && nj < n && grid[ni][nj] == '1') {
                    grid[ni][nj] = '0'; // Mark as visited
                    queue.offer(new int[] { ni, nj });
                }
            }
        }
    }

}

// Solution 3: Union-Find (Disjoint Set Union)
// Time Complexity: O(m × n × α(m × n)) where α is inverse Ackermann function
// Space Complexity: O(m × n)
class Solution3 {

    class UnionFind {
        private int[] parent;
        private int[] rank;
        private int count;

        public UnionFind(char[][] grid) {
            int m = grid.length;
            int n = grid[0].length;
            parent = new int[m * n];
            rank = new int[m * n];
            count = 0;

            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (grid[i][j] == '1') {
                        parent[i * n + j] = i * n + j;
                        count++;
                    }
                    rank[i * n + j] = 0;
                }
            }
        }

        public int find(int i) {
            if (parent[i] != i) {
                parent[i] = find(parent[i]);
            }
            return parent[i];
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
                count--;
            }
        }

        public int getCount() {
            return count;
        }
    }

    public int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0)
            return 0;

        int m = grid.length;
        int n = grid[0].length;
        UnionFind uf = new UnionFind(grid);

        int[][] directions = { { 1, 0 }, { 0, 1 } };

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    for (int[] dir : directions) {
                        int ni = i + dir[0];
                        int nj = j + dir[1];

                        if (ni < m && nj < n && grid[ni][nj] == '1') {
                            uf.union(i * n + j, ni * n + nj);
                        }
                    }
                }
            }
        }

        return uf.getCount();
    }

}

// Solution 4: DFS with separate visited array (preserves original grid)
class Solution4 {

    public int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0)
            return 0;

        int m = grid.length;
        int n = grid[0].length;
        boolean[][] visited = new boolean[m][n];
        int count = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1' && !visited[i][j]) {
                    count++;
                    dfs(grid, visited, i, j);
                }
            }
        }

        return count;
    }

    private void dfs(char[][] grid, boolean[][] visited, int i, int j) {
        int m = grid.length;
        int n = grid[0].length;

        if (i < 0 || i >= m || j < 0 || j >= n ||
                visited[i][j] || grid[i][j] != '1') {
            return;
        }

        visited[i][j] = true;

        dfs(grid, visited, i + 1, j);
        dfs(grid, visited, i - 1, j);
        dfs(grid, visited, i, j + 1);
        dfs(grid, visited, i, j - 1);
    }

}

// Example usage and test cases
class NumberOfIslandsTest {
    public static void main(String[] args) {
        Solution1 solution = new Solution1();

        // Test case 1
        char[][] grid1 = {
                { '1', '1', '1', '1', '0' },
                { '1', '1', '0', '1', '0' },
                { '1', '1', '0', '0', '0' },
                { '0', '0', '0', '0', '0' }
        };
        System.out.println("Test 1: " + solution.numIslands(grid1)); // Expected: 1

        // Test case 2
        char[][] grid2 = {
                { '1', '1', '0', '0', '0' },
                { '1', '1', '0', '0', '0' },
                { '0', '0', '1', '0', '0' },
                { '0', '0', '0', '1', '1' }
        };
        System.out.println("Test 2: " + solution.numIslands(grid2)); // Expected: 3

        // Test case 3 - Single island
        char[][] grid3 = {
                { '1', '1' },
                { '1', '1' }
        };
        System.out.println("Test 3: " + solution.numIslands(grid3)); // Expected: 1

        // Test case 4 - No islands
        char[][] grid4 = {
                { '0', '0' },
                { '0', '0' }
        };
        System.out.println("Test 4: " + solution.numIslands(grid4)); // Expected: 0
    }

}

import java.util.LinkedList;
import java.util.Queue;

/*
 * You are given an m x n matrix board containing letters 'X' and 'O', capture
 * regions that are surrounded:
 * 
 * Connect: A cell is connected to adjacent cells horizontally or vertically.
 * Region: To form a region connect every 'O' cell.
 * Surround: The region is surrounded with 'X' cells if you can connect the
 * region with 'X' cells and none of the region cells are on the edge of the
 * board.
 * To capture a surrounded region, replace all 'O's with 'X's in-place within
 * the original board. You do not need to return anything.
 * 
 * Example 1:
 * Input: board =
 * [["X","X","X","X"],["X","O","O","X"],["X","X","O","X"],["X","O","X","X"]]
 * Output:
 * [["X","X","X","X"],["X","X","X","X"],["X","X","X","X"],["X","O","X","X"]]
 * Explanation:
 * In the above diagram, the bottom region is not captured because it is on the
 * edge of the board and cannot be surrounded.
 * 
 * Example 2:
 * Input: board = [["X"]]
 * Output: [["X"]]
 */

class SurroundedRegionsSolution {

    record Cell(int row, int col) {
    }

    private static final int[][] DIRECTIONS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    public void solve(char[][] board) {
        if (board == null || board.length == 0)
            return;

        int rows = board.length;
        int cols = board[0].length;
        boolean[][] isSafe = new boolean[rows][cols];
        Queue<Cell> queue = new LinkedList<>();

        // Add border 'O's (top & bottom rows)
        for (int c = 0; c < cols; c++) {
            if (board[0][c] == 'O' && !isSafe[0][c]) {
                isSafe[0][c] = true;
                queue.offer(new Cell(0, c));
            }
            if (board[rows - 1][c] == 'O' && !isSafe[rows - 1][c]) {
                isSafe[rows - 1][c] = true;
                queue.offer(new Cell(rows - 1, c));
            }
        }

        // Add border 'O's (left & right columns)
        for (int r = 0; r < rows; r++) {
            if (board[r][0] == 'O' && !isSafe[r][0]) {
                isSafe[r][0] = true;
                queue.offer(new Cell(r, 0));
            }
            if (board[r][cols - 1] == 'O' && !isSafe[r][cols - 1]) {
                isSafe[r][cols - 1] = true;
                queue.offer(new Cell(r, cols - 1));
            }
        }

        // BFS from border 'O's, marking reachable 'O's as safe
        while (!queue.isEmpty()) {
            Cell cell = queue.poll();
            for (int[] dir : DIRECTIONS) {
                int nr = cell.row() + dir[0];
                int nc = cell.col() + dir[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                        && board[nr][nc] == 'O' && !isSafe[nr][nc]) {
                    isSafe[nr][nc] = true;
                    queue.offer(new Cell(nr, nc));
                }
            }
        }

        // Flip all non-safe 'O's to 'X'
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == 'O' && !isSafe[r][c]) {
                    board[r][c] = 'X';
                }
            }
        }
    }
}

class SurroundedRegions {

    // =================================================================
    // APPROACH 1: DFS (Most Common)
    // Time: O(m*n)
    // Space: O(m*n) - recursion stack in worst case
    // =================================================================

    public void solveDFS(char[][] board) {
        if (board == null || board.length == 0)
            return;

        int m = board.length;
        int n = board[0].length;

        // Step 1: Mark all 'O's connected to borders as safe (use 'T' as temporary
        // marker)
        // Check first and last rows
        for (int j = 0; j < n; j++) {
            if (board[0][j] == 'O')
                dfs(board, 0, j);
            if (board[m - 1][j] == 'O')
                dfs(board, m - 1, j);
        }

        // Check first and last columns
        for (int i = 0; i < m; i++) {
            if (board[i][0] == 'O')
                dfs(board, i, 0);
            if (board[i][n - 1] == 'O')
                dfs(board, i, n - 1);
        }

        // Step 2: Flip all remaining 'O's to 'X' (captured)
        // and restore 'T's back to 'O' (safe)
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 'O') {
                    board[i][j] = 'X'; // Captured region
                } else if (board[i][j] == 'T') {
                    board[i][j] = 'O'; // Safe region
                }
            }
        }
    }

    private void dfs(char[][] board, int i, int j) {
        int m = board.length;
        int n = board[0].length;

        // Boundary check
        if (i < 0 || i >= m || j < 0 || j >= n || board[i][j] != 'O') {
            return;
        }

        // Mark as safe (temporary marker)
        board[i][j] = 'T';

        // Explore all 4 directions
        dfs(board, i - 1, j); // Up
        dfs(board, i + 1, j); // Down
        dfs(board, i, j - 1); // Left
        dfs(board, i, j + 1); // Right
    }

    // =================================================================
    // APPROACH 2: BFS
    // Time: O(m*n)
    // Space: O(min(m,n)) - queue size for border traversal
    // =================================================================

    public void solveBFS(char[][] board) {
        if (board == null || board.length == 0)
            return;

        int m = board.length;
        int n = board[0].length;
        Queue<int[]> queue = new LinkedList<>();

        // Step 1: Add all border 'O's to queue
        for (int j = 0; j < n; j++) {
            if (board[0][j] == 'O') {
                queue.offer(new int[] { 0, j });
                board[0][j] = 'T';
            }
            if (board[m - 1][j] == 'O') {
                queue.offer(new int[] { m - 1, j });
                board[m - 1][j] = 'T';
            }
        }

        for (int i = 1; i < m - 1; i++) {
            if (board[i][0] == 'O') {
                queue.offer(new int[] { i, 0 });
                board[i][0] = 'T';
            }
            if (board[i][n - 1] == 'O') {
                queue.offer(new int[] { i, n - 1 });
                board[i][n - 1] = 'T';
            }
        }

        // Step 2: BFS to mark all connected 'O's
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int i = curr[0], j = curr[1];

            for (int[] dir : dirs) {
                int ni = i + dir[0];
                int nj = j + dir[1];

                if (ni >= 0 && ni < m && nj >= 0 && nj < n && board[ni][nj] == 'O') {
                    board[ni][nj] = 'T';
                    queue.offer(new int[] { ni, nj });
                }
            }
        }

        // Step 3: Flip captured 'O's to 'X' and restore 'T's to 'O'
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 'O') {
                    board[i][j] = 'X';
                } else if (board[i][j] == 'T') {
                    board[i][j] = 'O';
                }
            }
        }
    }

    // =================================================================
    // APPROACH 3: Union-Find (Disjoint Set Union)
    // Time: O(m*n * α(m*n)) where α is inverse Ackermann (nearly constant)
    // Space: O(m*n)
    // =================================================================

    public void solveUnionFind(char[][] board) {
        if (board == null || board.length == 0)
            return;

        int m = board.length;
        int n = board[0].length;

        // Create a dummy node to represent all border-connected 'O's
        int dummyNode = m * n;
        UnionFind uf = new UnionFind(m * n + 1);

        // Step 1: Union all 'O's with their adjacent 'O's
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 'O') {
                    int currIndex = i * n + j;

                    // If on border, union with dummy node
                    if (i == 0 || i == m - 1 || j == 0 || j == n - 1) {
                        uf.union(currIndex, dummyNode);
                    }

                    // Union with adjacent 'O's (only check right and down to avoid duplicates)
                    if (i + 1 < m && board[i + 1][j] == 'O') {
                        uf.union(currIndex, (i + 1) * n + j);
                    }
                    if (j + 1 < n && board[i][j + 1] == 'O') {
                        uf.union(currIndex, i * n + (j + 1));
                    }
                }
            }
        }

        // Step 2: Flip 'O's not connected to dummy node
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 'O' && !uf.connected(i * n + j, dummyNode)) {
                    board[i][j] = 'X';
                }
            }
        }
    }

    // Union-Find Data Structure
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
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                // Union by rank
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
    // APPROACH 4: DFS with Explicit Stack (Avoid Stack Overflow)
    // Time: O(m*n)
    // Space: O(m*n)
    // =================================================================

    public void solveDFSIterative(char[][] board) {
        if (board == null || board.length == 0)
            return;

        int m = board.length;
        int n = board[0].length;

        // Process all border 'O's
        for (int j = 0; j < n; j++) {
            if (board[0][j] == 'O')
                dfsIterative(board, 0, j);
            if (board[m - 1][j] == 'O')
                dfsIterative(board, m - 1, j);
        }

        for (int i = 0; i < m; i++) {
            if (board[i][0] == 'O')
                dfsIterative(board, i, 0);
            if (board[i][n - 1] == 'O')
                dfsIterative(board, i, n - 1);
        }

        // Flip captured and restore safe
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 'O') {
                    board[i][j] = 'X';
                } else if (board[i][j] == 'T') {
                    board[i][j] = 'O';
                }
            }
        }
    }

    private void dfsIterative(char[][] board, int startI, int startJ) {
        int m = board.length;
        int n = board[0].length;
        LinkedList<int[]> stack = new LinkedList<>();
        stack.push(new int[] { startI, startJ });
        board[startI][startJ] = 'T';

        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            int i = curr[0], j = curr[1];

            for (int[] dir : dirs) {
                int ni = i + dir[0];
                int nj = j + dir[1];

                if (ni >= 0 && ni < m && nj >= 0 && nj < n && board[ni][nj] == 'O') {
                    board[ni][nj] = 'T';
                    stack.push(new int[] { ni, nj });
                }
            }
        }
    }

    // =================================================================
    // TEST HELPER
    // =================================================================

    public static void main(String[] args) {
        SurroundedRegions sol = new SurroundedRegions();

        char[][] board1 = {
                { 'X', 'X', 'X', 'X' },
                { 'X', 'O', 'O', 'X' },
                { 'X', 'X', 'O', 'X' },
                { 'X', 'O', 'X', 'X' }
        };

        System.out.println("Input:");
        printBoard(board1);

        sol.solveDFS(board1);
        System.out.println("\nOutput (DFS):");
        printBoard(board1);

        // Test BFS
        char[][] board2 = {
                { 'X', 'X', 'X', 'X' },
                { 'X', 'O', 'O', 'X' },
                { 'X', 'X', 'O', 'X' },
                { 'X', 'O', 'X', 'X' }
        };

        sol.solveBFS(board2);
        System.out.println("\nOutput (BFS):");
        printBoard(board2);
    }

    private static void printBoard(char[][] board) {
        for (char[] row : board) {
            for (char c : row) {
                System.out.print(c + " ");
            }
            System.out.println();
        }
    }
}

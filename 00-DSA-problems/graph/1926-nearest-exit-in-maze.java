import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

/*
 * You are given an m x n matrix maze (0-indexed) with empty cells (represented
 * as '.') and walls (represented as '+'). You are also given the entrance of
 * the maze, where entrance = [entrancerow, entrancecol] denotes the row and
 * column of the cell you are initially standing at.
 * 
 * In one step, you can move one cell up, down, left, or right. You cannot step
 * into a cell with a wall, and you cannot step outside the maze. Your goal is
 * to find the nearest exit from the entrance. An exit is defined as an empty
 * cell that is at the border of the maze. The entrance does not count as an
 * exit.
 * 
 * Return the number of steps in the shortest path from the entrance to the
 * nearest exit, or -1 if no such path exists.
 * 
 */

class NearestExit {

    public int nearestExit(char[][] maze, int[] entrance) {
        char wall = '+', emptyCell = '.';
        int rows = maze.length, cols = maze[0].length;
        int[][] directions = new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.offer(entrance);

        int row = entrance[0], col = entrance[1];
        maze[row][col] = wall; // marking visited

        int steps = 0;
        while (!queue.isEmpty()) {
            int n = queue.size();
            for (int i = 0; i < n; i++) {
                int[] pos = queue.poll();
                row = pos[0];
                col = pos[1];
                for (int[] dir : directions) {
                    int newRow = row + dir[0], newCol = col + dir[1];
                    if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols
                            && maze[newRow][newCol] == emptyCell) {
                        if (newRow == 0 || newRow == rows - 1 || newCol == 0 || newCol == cols - 1) {
                            return steps + 1;
                        }
                        maze[newRow][newCol] = wall;
                        queue.offer(new int[] { newRow, newCol });
                    }
                }
            }
            steps++;
        }
        return -1;
    }

}

class NearestExit2 {
    public int nearestExit(char[][] maze, int[] entrance) {
        int rows = maze.length, cols = maze[0].length;
        int[] dirRow = { -1, 1, 0, 0 };
        int[] dirCol = { 0, 0, -1, 1 };

        int[] queueRow = new int[rows * cols];
        int[] queueCol = new int[rows * cols];
        int head = 0, tail = 0;

        queueRow[tail] = entrance[0];
        queueCol[tail++] = entrance[1];
        maze[entrance[0]][entrance[1]] = '+';

        int steps = 0;

        while (head < tail) {
            int size = tail - head;

            for (int i = 0; i < size; i++) {
                int r = queueRow[head];
                int c = queueCol[head++];

                for (int d = 0; d < 4; d++) {
                    int nr = r + dirRow[d];
                    int nc = c + dirCol[d];

                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && maze[nr][nc] == '.') {
                        if (nr == 0 || nr == rows - 1 || nc == 0 || nc == cols - 1) {
                            return steps + 1;
                        }

                        maze[nr][nc] = '+';
                        queueRow[tail] = nr;
                        queueCol[tail++] = nc;
                    }
                }
            }

            steps++;
        }

        return -1;
    }

}

class NearestExit3 {

    /**
     * BFS Solution - Finds shortest path to nearest exit
     * Time: O(m * n), Space: O(m * n)
     */
    public int nearestExit(char[][] maze, int[] entrance) {
        int m = maze.length, n = maze[0].length;
        int[] directions = { -1, 0, 1, 0, -1 }; // up, right, down, left

        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[m][n];

        // Start BFS from entrance
        queue.offer(new int[] { entrance[0], entrance[1], 0 }); // {row, col, steps}
        visited[entrance[0]][entrance[1]] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0], col = current[1], steps = current[2];

            // Try all 4 directions
            for (int i = 0; i < 4; i++) {
                int newRow = row + directions[i];
                int newCol = col + directions[i + 1];

                // Check bounds and if cell is empty and not visited
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n &&
                        maze[newRow][newCol] == '.' && !visited[newRow][newCol]) {

                    // Check if this is an exit (border cell, not entrance)
                    if (isExit(newRow, newCol, m, n, entrance)) {
                        return steps + 1;
                    }

                    visited[newRow][newCol] = true;
                    queue.offer(new int[] { newRow, newCol, steps + 1 });
                }
            }
        }

        return -1; // No exit found
    }

    private boolean isExit(int row, int col, int m, int n, int[] entrance) {
        // Must be on border and not be the entrance
        boolean onBorder = (row == 0 || row == m - 1 || col == 0 || col == n - 1);
        boolean notEntrance = !(row == entrance[0] && col == entrance[1]);
        return onBorder && notEntrance;
    }

    /**
     * Alternative BFS - Mark visited in maze itself
     * Time: O(m * n), Space: O(m * n) for queue only
     */
    public int nearestExitInPlace(char[][] maze, int[] entrance) {
        int m = maze.length, n = maze[0].length;
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { entrance[0], entrance[1], 0 });
        maze[entrance[0]][entrance[1]] = '+'; // Mark entrance as visited

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], steps = curr[2];

            for (int[] dir : dirs) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n &&
                        maze[newRow][newCol] == '.') {

                    // Check if it's an exit
                    if (newRow == 0 || newRow == m - 1 || newCol == 0 || newCol == n - 1) {
                        return steps + 1;
                    }

                    maze[newRow][newCol] = '+'; // Mark as visited
                    queue.offer(new int[] { newRow, newCol, steps + 1 });
                }
            }
        }

        return -1;
    }

    /**
     * BFS with level-by-level processing
     * More intuitive step counting
     */
    public int nearestExitLevelOrder(char[][] maze, int[] entrance) {
        int m = maze.length, n = maze[0].length;
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[m][n];

        queue.offer(new int[] { entrance[0], entrance[1] });
        visited[entrance[0]][entrance[1]] = true;

        int steps = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            steps++;

            // Process all nodes at current level
            for (int i = 0; i < size; i++) {
                int[] curr = queue.poll();
                int row = curr[0], col = curr[1];

                for (int[] dir : dirs) {
                    int newRow = row + dir[0];
                    int newCol = col + dir[1];

                    if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n &&
                            maze[newRow][newCol] == '.' && !visited[newRow][newCol]) {

                        // Check if it's an exit
                        if (newRow == 0 || newRow == m - 1 || newCol == 0 || newCol == n - 1) {
                            return steps;
                        }

                        visited[newRow][newCol] = true;
                        queue.offer(new int[] { newRow, newCol });
                    }
                }
            }
        }

        return -1;
    }

    /**
     * DFS Solution (for comparison - not optimal for shortest path)
     * This will find A path but not necessarily the shortest
     */
    public int nearestExitDFS(char[][] maze, int[] entrance) {
        int m = maze.length, n = maze[0].length;
        boolean[][] visited = new boolean[m][n];
        int[] minSteps = { Integer.MAX_VALUE };

        dfs(maze, entrance[0], entrance[1], 0, visited, entrance, minSteps, m, n);

        return minSteps[0] == Integer.MAX_VALUE ? -1 : minSteps[0];
    }

    private void dfs(char[][] maze, int row, int col, int steps, boolean[][] visited,
            int[] entrance, int[] minSteps, int m, int n) {
        if (row < 0 || row >= m || col < 0 || col >= n ||
                maze[row][col] == '+' || visited[row][col] || steps >= minSteps[0]) {
            return;
        }

        // Check if current cell is an exit
        if ((row == 0 || row == m - 1 || col == 0 || col == n - 1) &&
                !(row == entrance[0] && col == entrance[1])) {
            minSteps[0] = Math.min(minSteps[0], steps);
            return;
        }

        visited[row][col] = true;

        // Explore all 4 directions
        dfs(maze, row - 1, col, steps + 1, visited, entrance, minSteps, m, n);
        dfs(maze, row + 1, col, steps + 1, visited, entrance, minSteps, m, n);
        dfs(maze, row, col - 1, steps + 1, visited, entrance, minSteps, m, n);
        dfs(maze, row, col + 1, steps + 1, visited, entrance, minSteps, m, n);

        visited[row][col] = false; // Backtrack
    }

    // Test cases
    public static void main(String[] args) {
        NearestExit3 sol = new NearestExit3();

        // Test case 1
        char[][] maze1 = {
                { '+', '+', '.', '+' },
                { '.', '.', '.', '+' },
                { '+', '+', '+', '.' }
        };
        int[] entrance1 = { 1, 2 };
        System.out.println("Test 1: " + sol.nearestExit(maze1, entrance1)); // Expected: 1

        // Test case 2
        char[][] maze2 = {
                { '+', '+', '+' },
                { '.', '.', '.' },
                { '+', '+', '+' }
        };
        int[] entrance2 = { 1, 0 };
        System.out.println("Test 2: " + sol.nearestExit(maze2, entrance2)); // Expected: 2

        // Test case 3 - No exit
        char[][] maze3 = {
                { '.', '+' }
        };
        int[] entrance3 = { 0, 0 };
        System.out.println("Test 3: " + sol.nearestExit(maze3, entrance3)); // Expected: -1

        // Test case 4 - Large maze
        char[][] maze4 = {
                { '.', '.', '.', '.', '+' },
                { '+', '+', '+', '.', '+' },
                { '.', '.', '.', '.', '.' },
                { '+', '+', '+', '+', '.' }
        };
        int[] entrance4 = { 0, 0 };
        System.out.println("Test 4: " + sol.nearestExit(maze4, entrance4)); // Expected: 7

        // Test different implementations
        System.out.println("Test 1 (Level Order): " + sol.nearestExitLevelOrder(copyMaze(maze1), entrance1));
    }

    // Helper method to copy maze for testing
    private static char[][] copyMaze(char[][] original) {
        char[][] copy = new char[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }
}

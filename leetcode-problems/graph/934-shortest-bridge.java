
import java.util.LinkedList;
import java.util.Queue;

class ShortestBridge {
    int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    public int shortestBridge(int[][] grid) {
        int n = grid.length;
        boolean[][] visited = new boolean[n][n];
        Queue<int[]> queue = findFirstIsland(grid, visited);
        return getStepsToReachSecondIsland(grid, visited, queue);
    }

    private int getStepsToReachSecondIsland(int[][] grid, boolean[][] visited, Queue<int[]> queue) {
        int n = grid.length;
        int steps = 0;
        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int[] cell = queue.poll();

                for (int[] dir : directions) {
                    int x = cell[0] + dir[0];
                    int y = cell[1] + dir[1];

                    if (x >= 0 && y >= 0 && x < n && y < n && !visited[x][y]) {
                        if (grid[x][y] == 1) {
                            return steps;
                        }

                        visited[x][y] = true;
                        queue.offer(new int[] { x, y });
                    }
                }
            }
            steps++;
        }
        return steps;
    }

    private Queue<int[]> findFirstIsland(int[][] grid, boolean[][] visited) {
        int n = grid.length;
        Queue<int[]> queue = new LinkedList<>();

        // Find first island
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    dfs(grid, i, j, visited, queue);
                    return queue;
                }
            }
        }

        return queue;
    }

    private void dfs(int[][] grid, int i, int j, boolean[][] visited, Queue<int[]> queue) {
        int n = grid.length;
        if (i < 0 || j < 0 || i >= n || j >= n || visited[i][j] || grid[i][j] == 0) {
            return;
        }
        visited[i][j] = true;
        queue.offer(new int[] { i, j });

        for (int[] dir : directions) {
            dfs(grid, i + dir[0], j + dir[1], visited, queue);
        }
    }
}

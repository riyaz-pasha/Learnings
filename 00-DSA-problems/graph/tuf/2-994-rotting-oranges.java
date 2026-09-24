
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class RottingOranges {

    /*
     * You are given an m x n grid where each cell can have one of three values:
     * 
     * 0 representing an empty cell,
     * 1 representing a fresh orange, or
     * 2 representing a rotten orange.
     * Every minute, any fresh orange that is 4-directionally adjacent to a rotten
     * orange becomes rotten.
     * 
     * Return the minimum number of minutes that must elapse until no cell has a
     * fresh orange. If this is impossible, return -1.
     * 
     * Example 1:
     * Input: grid = [[2,1,1],[1,1,0],[0,1,1]]
     * Output: 4
     * 
     * Example 2:
     * Input: grid = [[2,1,1],[0,1,1],[1,0,1]]
     * Output: -1
     * Explanation: The orange in the bottom left corner (row 2, column 0) is never
     * rotten, because rotting only happens 4-directionally.
     * 
     * Example 3:
     * Input: grid = [[0,2]]
     * Output: 0
     * Explanation: Since there are already no fresh oranges at minute 0, the answer
     * is just 0.
     */

    public int orangesRotting(int[][] grid) {
        // first traverse the grid and count total fresh oranges and put rotten oranges
        // in a queue. and also mark these rotten oranges visited
        // next we perform bfs traversal on this queue check if neighbors can be rotten
        // if yes then we mark that as rotten and add it to the queue

        int rows = grid.length;
        int cols = grid[0].length;
        int freshOrangesCount = 0;
        Queue<int[]> queue = new LinkedList<>();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (grid[row][col] == 2) {
                    queue.add(new int[] { row, col });
                }
                if (grid[row][col] == 1) {
                    freshOrangesCount++;
                }
            }
        }

        if (freshOrangesCount == 0)
            return 0; // no fresh oranges => 0 minutes

        int minutes = 0;
        int[][] directions = new int[][] { { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 0 } };

        while (!queue.isEmpty()) {
            int n = queue.size();
            boolean anyRot = false; // track if this minute causes any rot

            for (int i = 0; i < n; i++) {
                int[] pos = queue.poll();
                for (int[] dir : directions) {
                    int newRow = pos[0] + dir[0];
                    int newCol = pos[1] + dir[1];
                    if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols && grid[newRow][newCol] == 1) {
                        grid[newRow][newCol] = 2;
                        queue.add(new int[] { newRow, newCol });
                        anyRot = true;
                        freshOrangesCount--;
                    }
                }
            }
            if (anyRot) {
                minutes++;
            }
            if (freshOrangesCount == 0)
                return minutes;
        }

        return -1;
    }

}

class Solution1 {
    public int orangesRotting(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        Queue<int[]> queue = new LinkedList<>();
        int fresh = 0;

        // Count fresh oranges and add all rotten oranges to queue
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 2) {
                    queue.offer(new int[] { i, j });
                } else if (grid[i][j] == 1) {
                    fresh++;
                }
            }
        }

        // If no fresh oranges, return 0
        if (fresh == 0)
            return 0;

        int minutes = 0;
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        // BFS
        while (!queue.isEmpty()) {
            int size = queue.size();
            boolean rotted = false;

            for (int i = 0; i < size; i++) {
                int[] curr = queue.poll();
                int row = curr[0];
                int col = curr[1];

                // Check all 4 directions
                for (int[] dir : dirs) {
                    int newRow = row + dir[0];
                    int newCol = col + dir[1];

                    // If valid fresh orange, rot it
                    if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                            && grid[newRow][newCol] == 1) {
                        grid[newRow][newCol] = 2;
                        queue.offer(new int[] { newRow, newCol });
                        fresh--;
                        rotted = true;
                    }
                }
            }

            // Only increment time if we actually rotted an orange
            if (rotted)
                minutes++;
        }

        // If there are still fresh oranges, return -1
        return fresh == 0 ? minutes : -1;
    }
}
// Time Complexity: O(m * n) - we visit each cell at most once
// Space Complexity: O(m * n) - queue can contain all cells in worst case

class Solution2 {
    public int orangesRotting(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        int fresh = 0;

        // Count fresh oranges
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1)
                    fresh++;
            }
        }

        if (fresh == 0)
            return 0;

        int minutes = 0;
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        // Keep spreading rot until no more changes
        while (true) {
            List<int[]> newRotten = new ArrayList<>();

            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (grid[i][j] == 2) {
                        // Check adjacent cells
                        for (int[] dir : dirs) {
                            int ni = i + dir[0];
                            int nj = j + dir[1];
                            if (ni >= 0 && ni < m && nj >= 0 && nj < n
                                    && grid[ni][nj] == 1) {
                                newRotten.add(new int[] { ni, nj });
                            }
                        }
                    }
                }
            }

            // If no new rotten oranges, break
            if (newRotten.isEmpty())
                break;

            // Mark new rotten oranges
            for (int[] pos : newRotten) {
                grid[pos[0]][pos[1]] = 2;
                fresh--;
            }

            minutes++;
        }

        return fresh == 0 ? minutes : -1;
    }
}
// Time Complexity: O(m * n * k) where k is the number of minutes
// Space Complexity: O(m * n) in worst case for newRotten list

class Solution3 {
    public int orangesRotting(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        int fresh = 0;

        // Count fresh oranges
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1)
                    fresh++;
            }
        }

        if (fresh == 0)
            return 0;

        int minutes = 0;
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        // Keep spreading rot until no more changes
        while (true) {
            List<int[]> newRotten = new ArrayList<>();

            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (grid[i][j] == 2) {
                        // Check adjacent cells
                        for (int[] dir : dirs) {
                            int ni = i + dir[0];
                            int nj = j + dir[1];
                            if (ni >= 0 && ni < m && nj >= 0 && nj < n
                                    && grid[ni][nj] == 1) {
                                newRotten.add(new int[] { ni, nj });
                            }
                        }
                    }
                }
            }

            // If no new rotten oranges, break
            if (newRotten.isEmpty())
                break;

            // Mark new rotten oranges
            for (int[] pos : newRotten) {
                grid[pos[0]][pos[1]] = 2;
                fresh--;
            }

            minutes++;
        }

        return fresh == 0 ? minutes : -1;
    }
}
// Time Complexity: O(m * n * k) where k is the number of minutes
// Space Complexity: O(m * n) in worst case for newRotten list

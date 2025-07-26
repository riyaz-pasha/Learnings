/*
 * You are given a 0-indexed 2D matrix grid of size n x n, where (r, c)
 * represents:
 * 
 * A cell containing a thief if grid[r][c] = 1
 * An empty cell if grid[r][c] = 0
 * You are initially positioned at cell (0, 0). In one move, you can move to any
 * adjacent cell in the grid, including cells containing thieves.
 * 
 * The safeness factor of a path on the grid is defined as the minimum manhattan
 * distance from any cell in the path to any thief in the grid.
 * 
 * Return the maximum safeness factor of all paths leading to cell (n - 1, n -
 * 1).
 * 
 * An adjacent cell of cell (r, c), is one of the cells (r, c + 1), (r, c - 1),
 * (r + 1, c) and (r - 1, c) if it exists.
 * 
 * The Manhattan distance between two cells (a, b) and (x, y) is equal to |a -
 * x| + |b - y|, where |val| denotes the absolute value of val.
 * 
 * 
 * 
 * Example 1:
 * 
 * 
 * Input: grid = [[1,0,0],[0,0,0],[0,0,1]]
 * Output: 0
 * Explanation: All paths from (0, 0) to (n - 1, n - 1) go through the thieves
 * in cells (0, 0) and (n - 1, n - 1).
 * Example 2:
 * 
 * 
 * Input: grid = [[0,0,1],[0,0,0],[0,0,0]]
 * Output: 2
 * Explanation: The path depicted in the picture above has a safeness factor of
 * 2 since:
 * - The closest cell of the path to the thief at cell (0, 2) is cell (0, 0).
 * The distance between them is | 0 - 0 | + | 0 - 2 | = 2.
 * It can be shown that there are no other paths with a higher safeness factor.
 * Example 3:
 * 
 * 
 * Input: grid = [[0,0,0,1],[0,0,0,0],[0,0,0,0],[1,0,0,0]]
 * Output: 2
 * Explanation: The path depicted in the picture above has a safeness factor of
 * 2 since:
 * - The closest cell of the path to the thief at cell (0, 3) is cell (1, 2).
 * The distance between them is | 0 - 1 | + | 3 - 2 | = 2.
 * - The closest cell of the path to the thief at cell (3, 0) is cell (3, 2).
 * The distance between them is | 3 - 3 | + | 0 - 2 | = 2.
 * It can be shown that there are no other paths with a higher safeness factor.
 */

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

class Solution {

    // Solution 1: Binary Search + BFS (Optimal)
    public int maximumSafenessFactor(List<List<Integer>> grid) {
        int n = grid.size();

        // Step 1: Precompute minimum distance to any thief for each cell
        int[][] dist = computeDistances(grid, n);

        // Step 2: Binary search on the answer
        int left = 0, right = 2 * n; // Max possible distance is 2*n
        int result = 0;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (canReachWithSafeness(dist, n, mid)) {
                result = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    // Multi-source BFS to compute minimum distance to any thief
    private int[][] computeDistances(List<List<Integer>> grid, int n) {
        int[][] dist = new int[n][n];
        Queue<int[]> queue = new LinkedList<>();

        // Initialize distances and add all thieves to queue
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid.get(i).get(j) == 1) {
                    dist[i][j] = 0;
                    queue.offer(new int[] { i, j });
                } else {
                    dist[i][j] = Integer.MAX_VALUE;
                }
            }
        }

        // Multi-source BFS
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int x = curr[0], y = curr[1];

            for (int[] dir : dirs) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < n && ny >= 0 && ny < n &&
                        dist[nx][ny] > dist[x][y] + 1) {
                    dist[nx][ny] = dist[x][y] + 1;
                    queue.offer(new int[] { nx, ny });
                }
            }
        }

        return dist;
    }

    // Check if we can reach (n-1, n-1) with given minimum safeness
    private boolean canReachWithSafeness(int[][] dist, int n, int minSafe) {
        // If start or end doesn't meet minimum safeness
        if (dist[0][0] < minSafe || dist[n - 1][n - 1] < minSafe) {
            return false;
        }

        boolean[][] visited = new boolean[n][n];
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { 0, 0 });
        visited[0][0] = true;

        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int x = curr[0], y = curr[1];

            if (x == n - 1 && y == n - 1) {
                return true;
            }

            for (int[] dir : dirs) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < n && ny >= 0 && ny < n &&
                        !visited[nx][ny] && dist[nx][ny] >= minSafe) {
                    visited[nx][ny] = true;
                    queue.offer(new int[] { nx, ny });
                }
            }
        }

        return false;
    }

}

// Alternative Solution 2: Dijkstra's Algorithm (Also works but slower)
class Solution2 {
    public int maximumSafenessFactor(List<List<Integer>> grid) {
        int n = grid.size();

        // Step 1: Precompute distances
        int[][] dist = computeDistances(grid, n);

        // Step 2: Use Dijkstra to find path with maximum minimum distance
        return dijkstra(dist, n);
    }

    private int[][] computeDistances(List<List<Integer>> grid, int n) {
        int[][] dist = new int[n][n];
        Queue<int[]> queue = new LinkedList<>();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid.get(i).get(j) == 1) {
                    dist[i][j] = 0;
                    queue.offer(new int[] { i, j });
                } else {
                    dist[i][j] = Integer.MAX_VALUE;
                }
            }
        }

        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int x = curr[0], y = curr[1];

            for (int[] dir : dirs) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < n && ny >= 0 && ny < n &&
                        dist[nx][ny] > dist[x][y] + 1) {
                    dist[nx][ny] = dist[x][y] + 1;
                    queue.offer(new int[] { nx, ny });
                }
            }
        }

        return dist;
    }

    private int dijkstra(int[][] dist, int n) {
        // Priority queue: {safeness, x, y} - max heap for safeness
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> b[0] - a[0]);
        boolean[][] visited = new boolean[n][n];

        pq.offer(new int[] { dist[0][0], 0, 0 });

        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int safeness = curr[0];
            int x = curr[1], y = curr[2];

            if (visited[x][y])
                continue;
            visited[x][y] = true;

            if (x == n - 1 && y == n - 1) {
                return safeness;
            }

            for (int[] dir : dirs) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < n && ny >= 0 && ny < n && !visited[nx][ny]) {
                    int newSafeness = Math.min(safeness, dist[nx][ny]);
                    pq.offer(new int[] { newSafeness, nx, ny });
                }
            }
        }

        return 0;
    }
}

// Test class
class TestSafenessFactor {
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1
        List<List<Integer>> grid1 = Arrays.asList(
                Arrays.asList(1, 0, 0),
                Arrays.asList(0, 0, 0),
                Arrays.asList(0, 0, 1));
        System.out.println("Test 1: " + solution.maximumSafenessFactor(grid1)); // Expected: 0

        // Test Case 2
        List<List<Integer>> grid2 = Arrays.asList(
                Arrays.asList(0, 0, 1),
                Arrays.asList(0, 0, 0),
                Arrays.asList(0, 0, 0));
        System.out.println("Test 2: " + solution.maximumSafenessFactor(grid2)); // Expected: 2

        // Test Case 3
        List<List<Integer>> grid3 = Arrays.asList(
                Arrays.asList(0, 0, 0, 1),
                Arrays.asList(0, 0, 0, 0),
                Arrays.asList(0, 0, 0, 0),
                Arrays.asList(1, 0, 0, 0));
        System.out.println("Test 3: " + solution.maximumSafenessFactor(grid3)); // Expected: 2
    }
}
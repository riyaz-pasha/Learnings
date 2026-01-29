import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

class MinTollPathDP {

    public static int minCostPath(int[][] grid) {
        int rows = grid.length;
        if (rows == 0) {
            return 0;
        }
        int cols = grid[0].length;

        int[][] dp = new int[rows][cols];

        dp[0][0] = grid[0][0];

        for (int col = 1; col < cols; col++) {
            dp[0][col] = dp[0][col - 1] + grid[0][col];
        }

        for (int row = 1; row < rows; row++) {
            dp[row][0] = dp[row - 1][0] + grid[row][0];
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                dp[row][col] = Math.min(dp[row - 1][col], dp[row][col - 1]) + grid[row][col];
            }
        }

        return dp[rows - 1][cols - 1];
    }

}

class Node implements Comparable<Node> {
    int cost;
    int row;
    int col;

    public Node(int cost, int row, int col) {
        this.cost = cost;
        this.row = row;
        this.col = col;
    }

    @Override
    public int compareTo(Node other) {
        return Integer.compare(this.cost, other.cost);
    }

}

class MinCostPathDijkstra {

    private int[][] directions = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

    public int minCostPath(int[][] grid) {
        int rows = grid.length;
        if (rows == 0) {
            return 0;
        }
        int cols = grid[0].length;

        int[][] dist = new int[rows][cols];

        for (int[] row : dist) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        PriorityQueue<Node> pq = new PriorityQueue<>();
        // Start from (0, 0)
        dist[0][0] = grid[0][0];
        pq.add(new Node(dist[0][0], 0, 0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int currentCost = current.cost;
            int row = current.row;
            int col = current.col;

            if (currentCost > dist[row][col]) {
                continue;
            }

            if (row == rows - 1 && col == col - 1) {
                return currentCost;
            }

            for (int[] dir : directions) {
                int newRow = row + dir[0], newCol = col + dir[1];
                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    int newCost = currentCost + grid[newRow][newCol];
                    if (newCost < dist[newRow][newCol]) {
                        dist[newRow][newCol] = newCost;
                        pq.offer(new Node(newCost, newRow, newCol));
                    }
                }
            }

        }

        return dist[rows - 1][cols - 1];
    }

}


class MinCostGrid {

    static class Cell {
        int row, col;
        int cost;

        Cell(int r, int c, int cost) {
            this.row = r;
            this.col = c;
            this.cost = cost;
        }
    }

    // Directions: right, down, left, up
    private static final int[][] DIRS = {
        {0, 1}, {1, 0}, {0, -1}, {-1, 0}
    };

    public static int minCost(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        int[][] dist = new int[m][n];
        for (int[] row : dist) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        PriorityQueue<Cell> pq =
            new PriorityQueue<>(Comparator.comparingInt(c -> c.cost));

        dist[0][0] = grid[0][0];
        pq.offer(new Cell(0, 0, grid[0][0]));

        while (!pq.isEmpty()) {
            Cell cur = pq.poll();

            if (cur.row == m - 1 && cur.col == n - 1) {
                return cur.cost; // shortest path found
            }

            if (cur.cost > dist[cur.row][cur.col]) continue;

            for (int[] d : DIRS) {
                int nr = cur.row + d[0];
                int nc = cur.col + d[1];

                if (nr < 0 || nc < 0 || nr >= m || nc >= n) continue;

                int newCost = cur.cost + grid[nr][nc];

                if (newCost < dist[nr][nc]) {
                    dist[nr][nc] = newCost;
                    pq.offer(new Cell(nr, nc, newCost));
                }
            }
        }

        return -1; // unreachable
    }

    // Demo
    public static void main(String[] args) {
        int[][] grid = {
            {1, 3, 1},
            {1, 5, 1},
            {4, 2, 1}
        };

        System.out.println(minCost(grid)); // Output: 7
    }
}

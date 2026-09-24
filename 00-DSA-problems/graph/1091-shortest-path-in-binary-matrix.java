import java.util.*;
/*
 * Given an n x n binary matrix grid, return the length of the shortest clear
 * path in the matrix. If there is no clear path, return -1.
 * 
 * A clear path in a binary matrix is a path from the top-left cell (i.e., (0,
 * 0)) to the bottom-right cell (i.e., (n - 1, n - 1)) such that:
 * 
 * All the visited cells of the path are 0.
 * All the adjacent cells of the path are 8-directionally connected (i.e., they
 * are different and they share an edge or a corner).
 * The length of a clear path is the number of visited cells of this path.
 * 
 * Input: grid = [[0,0,0],[1,1,0],[1,1,0]]
 * Output: 4
 * 
 * Input: grid = [[1,0,0],[1,1,0],[1,1,0]]
 * Output: -1
 */

class ShortestPathBinaryMatrix {

    // Solution 1: BFS with 8-directional movement (Optimal)
    // Time: O(n²), Space: O(n²)
    public int shortestPathBinaryMatrix(int[][] grid) {
        int n = grid.length;

        // Edge cases
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        // Single cell case
        if (n == 1) {
            return 1;
        }

        // 8 directions: up, down, left, right, and 4 diagonals
        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 }, // top-left, top, top-right
                { 0, -1 }, { 0, 1 }, // left, right
                { 1, -1 }, { 1, 0 }, { 1, 1 } // bottom-left, bottom, bottom-right
        };

        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[n][n];

        // Start from top-left
        queue.offer(new int[] { 0, 0, 1 }); // row, col, path_length
        visited[0][0] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], pathLen = curr[2];

            // Check if reached bottom-right
            if (row == n - 1 && col == n - 1) {
                return pathLen;
            }

            // Explore all 8 directions
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (isValid(grid, newRow, newCol, visited)) {
                    visited[newRow][newCol] = true;
                    queue.offer(new int[] { newRow, newCol, pathLen + 1 });
                }
            }
        }

        return -1; // No path found
    }

    private boolean isValid(int[][] grid, int row, int col, boolean[][] visited) {
        int n = grid.length;
        return row >= 0 && row < n && col >= 0 && col < n
                && grid[row][col] == 0 && !visited[row][col];
    }

    // Solution 2: A* Algorithm (More efficient for large grids)
    public int shortestPathBinaryMatrixAStar(int[][] grid) {
        int n = grid.length;

        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        if (n == 1) {
            return 1;
        }

        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 },
                { 0, -1 }, { 0, 1 },
                { 1, -1 }, { 1, 0 }, { 1, 1 }
        };

        // Priority queue for A* (min-heap based on f-score)
        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> a.fScore - b.fScore);
        boolean[][] visited = new boolean[n][n];

        // Start node
        Node start = new Node(0, 0, 1, heuristic(0, 0, n));
        pq.offer(start);
        visited[0][0] = true;

        while (!pq.isEmpty()) {
            Node curr = pq.poll();

            if (curr.row == n - 1 && curr.col == n - 1) {
                return curr.gScore;
            }

            for (int[] dir : directions) {
                int newRow = curr.row + dir[0];
                int newCol = curr.col + dir[1];

                if (isValid(grid, newRow, newCol, visited)) {
                    visited[newRow][newCol] = true;
                    int gScore = curr.gScore + 1;
                    int hScore = heuristic(newRow, newCol, n);
                    pq.offer(new Node(newRow, newCol, gScore, gScore + hScore));
                }
            }
        }

        return -1;
    }

    // Manhattan distance heuristic (admissible for 8-directional movement)
    private int heuristic(int row, int col, int n) {
        return Math.max(Math.abs(row - (n - 1)), Math.abs(col - (n - 1)));
    }

    // Node class for A* algorithm
    static class Node {
        int row, col, gScore, fScore;

        Node(int row, int col, int gScore, int fScore) {
            this.row = row;
            this.col = col;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    // Solution 3: BFS with optimized memory (modifying input grid)
    public int shortestPathBinaryMatrixInPlace(int[][] grid) {
        int n = grid.length;

        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        if (n == 1) {
            return 1;
        }

        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 },
                { 0, -1 }, { 0, 1 },
                { 1, -1 }, { 1, 0 }, { 1, 1 }
        };

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { 0, 0, 1 });
        grid[0][0] = 1; // Mark as visited

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], pathLen = curr[2];

            if (row == n - 1 && col == n - 1) {
                return pathLen;
            }

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n
                        && grid[newRow][newCol] == 0) {
                    grid[newRow][newCol] = 1; // Mark as visited
                    queue.offer(new int[] { newRow, newCol, pathLen + 1 });
                }
            }
        }

        return -1;
    }

    // Solution 4: Bidirectional BFS (Advanced optimization)
    public int shortestPathBinaryMatrixBidirectional(int[][] grid) {
        int n = grid.length;

        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        if (n == 1) {
            return 1;
        }

        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 },
                { 0, -1 }, { 0, 1 },
                { 1, -1 }, { 1, 0 }, { 1, 1 }
        };

        // Two queues for forward and backward search
        Queue<int[]> forwardQueue = new LinkedList<>();
        Queue<int[]> backwardQueue = new LinkedList<>();

        // Two visited sets
        boolean[][] forwardVisited = new boolean[n][n];
        boolean[][] backwardVisited = new boolean[n][n];

        forwardQueue.offer(new int[] { 0, 0 });
        backwardQueue.offer(new int[] { n - 1, n - 1 });
        forwardVisited[0][0] = true;
        backwardVisited[n - 1][n - 1] = true;

        int steps = 1;

        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // Always expand the smaller queue
            if (forwardQueue.size() > backwardQueue.size()) {
                Queue<int[]> temp = forwardQueue;
                forwardQueue = backwardQueue;
                backwardQueue = temp;

                boolean[][] tempVisited = forwardVisited;
                forwardVisited = backwardVisited;
                backwardVisited = tempVisited;
            }

            int size = forwardQueue.size();
            for (int i = 0; i < size; i++) {
                int[] curr = forwardQueue.poll();
                int row = curr[0], col = curr[1];

                for (int[] dir : directions) {
                    int newRow = row + dir[0];
                    int newCol = col + dir[1];

                    if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n
                            && grid[newRow][newCol] == 0) {

                        if (backwardVisited[newRow][newCol]) {
                            return steps + 1;
                        }

                        if (!forwardVisited[newRow][newCol]) {
                            forwardVisited[newRow][newCol] = true;
                            forwardQueue.offer(new int[] { newRow, newCol });
                        }
                    }
                }
            }
            steps++;
        }

        return -1;
    }

    // Test method
    public static void main(String[] args) {
        ShortestPathBinaryMatrix solution = new ShortestPathBinaryMatrix();

        // Test case 1
        int[][] grid1 = { { 0, 0, 0 }, { 1, 1, 0 }, { 1, 1, 0 } };
        System.out.println("Test 1 - Expected: 4, Got: " +
                solution.shortestPathBinaryMatrix(grid1));

        // Test case 2
        int[][] grid2 = { { 0, 1 }, { 1, 0 } };
        System.out.println("Test 2 - Expected: -1, Got: " +
                solution.shortestPathBinaryMatrix(grid2));

        // Test case 3
        int[][] grid3 = { { 0 } };
        System.out.println("Test 3 - Expected: 1, Got: " +
                solution.shortestPathBinaryMatrix(grid3));

        // Test case 4
        int[][] grid4 = { { 0, 0, 0 }, { 1, 1, 0 }, { 1, 1, 0 } };
        System.out.println("Test 4 - A* Algorithm: " +
                solution.shortestPathBinaryMatrixAStar(grid4));

        // Test case 5 - larger grid
        int[][] grid5 = {
                { 0, 0, 0, 0, 0, 0 },
                { 1, 1, 1, 1, 1, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 1, 1, 1, 1, 1 },
                { 0, 0, 0, 0, 0, 0 },
                { 1, 1, 1, 1, 1, 0 }
        };
        System.out.println("Test 5 - Large grid: " +
                solution.shortestPathBinaryMatrix(grid5));
    }

}

class ShortestPathInBinaryMatrix {

    public int shortestPathBinaryMatrix(int[][] matrix) {
        int n = matrix.length;

        // Check if start or end is blocked
        if (matrix[0][0] != 0 || matrix[n - 1][n - 1] != 0) {
            return -1;
        }

        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 },
                { 0, -1 }, { 0, 1 },
                { 1, -1 }, { 1, 0 }, { 1, 1 }
        };

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { 0, 0 });
        matrix[0][0] = 1; // Mark as visited by setting to 1

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int row = cell[0], col = cell[1];
            int distance = matrix[row][col];

            // If reached the bottom-right cell
            if (row == n - 1 && col == n - 1) {
                return distance;
            }

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < n &&
                        newCol >= 0 && newCol < n &&
                        matrix[newRow][newCol] == 0) {
                    queue.offer(new int[] { newRow, newCol });
                    matrix[newRow][newCol] = distance + 1; // Mark as visited with distance
                }
            }
        }

        return -1;
    }

}

class ShortestPathInBinaryMatrix2 {

    public int shortestPathBinaryMatrix(int[][] matrix) {
        int n = matrix.length;
        if (n == 1 && matrix[0][0] == 0)
            return 1;

        if (matrix[0][0] == 1 || matrix[n - 1][n - 1] == 1)
            return -1;

        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 },
                { 0, -1 }, { 0, 1 },
                { 1, -1 }, { 1, 0 }, { 1, 1 }
        };

        Queue<int[]> beginQueue = new LinkedList<>();
        Queue<int[]> endQueue = new LinkedList<>();

        boolean[][] beginVisited = new boolean[n][n];
        boolean[][] endVisited = new boolean[n][n];

        beginQueue.offer(new int[] { 0, 0 });
        endQueue.offer(new int[] { n - 1, n - 1 });
        beginVisited[0][0] = true;
        endVisited[n - 1][n - 1] = true;

        int steps = 1;

        while (!beginQueue.isEmpty() && !endQueue.isEmpty()) {
            // Expand from the smaller frontier
            if (beginQueue.size() > endQueue.size()) {
                Queue<int[]> tempQ = beginQueue;
                beginQueue = endQueue;
                endQueue = tempQ;

                boolean[][] tempV = beginVisited;
                beginVisited = endVisited;
                endVisited = tempV;
            }

            int size = beginQueue.size();
            for (int i = 0; i < size; i++) {
                int[] cell = beginQueue.poll();
                int row = cell[0], col = cell[1];

                for (int[] dir : directions) {
                    int newRow = row + dir[0], newCol = col + dir[1];
                    if (newRow >= 0 && newRow < n &&
                            newCol >= 0 && newCol < n &&
                            matrix[newRow][newCol] == 0) {

                        if (endVisited[newRow][newCol]) {
                            return steps + 1;
                        }

                        if (!beginVisited[newRow][newCol]) {
                            beginQueue.offer(new int[] { newRow, newCol });
                            beginVisited[newRow][newCol] = true;
                        }
                    }
                }
            }
            steps++;
        }

        return -1;
    }

}

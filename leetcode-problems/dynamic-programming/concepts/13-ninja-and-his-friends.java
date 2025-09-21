/*
 * We are given an ‘N*M’ matrix. Every cell of the matrix has some chocolates on
 * it, mat[i][j] gives us the number of chocolates. We have two friends ‘Alice’
 * and ‘Bob’. initially, Alice is standing on the cell(0,0) and Bob is standing
 * on the cell(0, M-1). Both of them can move only to the cells below them in
 * these three directions: to the bottom cell (↓), to the bottom-right cell(↘),
 * or to the bottom-left cell(↙).
 * 
 * When Alica and Bob visit a cell, they take all the chocolates from that cell
 * with them. It can happen that they visit the same cell, in that case, the
 * chocolates need to be considered only once.
 * 
 * They cannot go out of the boundary of the given matrix, we need to return the
 * maximum number of chocolates that Bob and Alice can together collect.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

// Solution 1: Top-Down with Memoization (3D DP)
// Time: O(N*M^2), Space: O(N*M^2)
class Solution1_TopDownMemo {
    public int maximumChocolates(int n, int m, int[][] grid) {
        // memo[row][aliceCol][bobCol] = max chocolates from this state
        Integer[][][] memo = new Integer[n][m][m];
        return dfs(grid, 0, 0, m - 1, memo);
    }

    private int dfs(int[][] grid, int row, int aliceCol, int bobCol, Integer[][][] memo) {
        int n = grid.length;
        int m = grid[0].length;

        // Out of bounds
        if (aliceCol < 0 || aliceCol >= m || bobCol < 0 || bobCol >= m) {
            return Integer.MIN_VALUE;
        }

        // Base case: reached last row
        if (row == n - 1) {
            if (aliceCol == bobCol) {
                return grid[row][aliceCol]; // Same cell, count once
            } else {
                return grid[row][aliceCol] + grid[row][bobCol];
            }
        }

        // Check memo
        if (memo[row][aliceCol][bobCol] != null) {
            return memo[row][aliceCol][bobCol];
        }

        // Current chocolates
        int chocolates = 0;
        if (aliceCol == bobCol) {
            chocolates = grid[row][aliceCol]; // Same cell, count once
        } else {
            chocolates = grid[row][aliceCol] + grid[row][bobCol];
        }

        // Try all combinations of moves for Alice and Bob
        int maxFromBelow = Integer.MIN_VALUE;

        // Alice can move in 3 directions, Bob can move in 3 directions = 9 combinations
        int[] directions = { -1, 0, 1 }; // left, straight, right

        for (int aliceMove : directions) {
            for (int bobMove : directions) {
                int newAliceCol = aliceCol + aliceMove;
                int newBobCol = bobCol + bobMove;

                int result = dfs(grid, row + 1, newAliceCol, newBobCol, memo);
                if (result != Integer.MIN_VALUE) {
                    maxFromBelow = Math.max(maxFromBelow, result);
                }
            }
        }

        memo[row][aliceCol][bobCol] = chocolates + maxFromBelow;
        return memo[row][aliceCol][bobCol];
    }
}

// Solution 2: Bottom-Up Dynamic Programming (3D)
// Time: O(N*M^2), Space: O(N*M^2)
class Solution2_BottomUp3D {
    public int maximumChocolates(int n, int m, int[][] grid) {
        // dp[row][aliceCol][bobCol] = max chocolates from this state
        int[][][] dp = new int[n][m][m];

        // Initialize with negative values to represent impossible states
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                Arrays.fill(dp[i][j], Integer.MIN_VALUE);
            }
        }

        // Base case: last row
        for (int aliceCol = 0; aliceCol < m; aliceCol++) {
            for (int bobCol = 0; bobCol < m; bobCol++) {
                if (aliceCol == bobCol) {
                    dp[n - 1][aliceCol][bobCol] = grid[n - 1][aliceCol];
                } else {
                    dp[n - 1][aliceCol][bobCol] = grid[n - 1][aliceCol] + grid[n - 1][bobCol];
                }
            }
        }

        // Fill DP table from bottom to top
        for (int row = n - 2; row >= 0; row--) {
            for (int aliceCol = 0; aliceCol < m; aliceCol++) {
                for (int bobCol = 0; bobCol < m; bobCol++) {
                    // Current chocolates
                    int chocolates = 0;
                    if (aliceCol == bobCol) {
                        chocolates = grid[row][aliceCol];
                    } else {
                        chocolates = grid[row][aliceCol] + grid[row][bobCol];
                    }

                    // Try all possible moves
                    int maxFromBelow = Integer.MIN_VALUE;
                    int[] directions = { -1, 0, 1 };

                    for (int aliceMove : directions) {
                        for (int bobMove : directions) {
                            int newAliceCol = aliceCol + aliceMove;
                            int newBobCol = bobCol + bobMove;

                            // Check bounds
                            if (newAliceCol >= 0 && newAliceCol < m &&
                                    newBobCol >= 0 && newBobCol < m) {
                                maxFromBelow = Math.max(maxFromBelow, dp[row + 1][newAliceCol][newBobCol]);
                            }
                        }
                    }

                    if (maxFromBelow != Integer.MIN_VALUE) {
                        dp[row][aliceCol][bobCol] = chocolates + maxFromBelow;
                    }
                }
            }
        }

        return dp[0][0][m - 1];
    }
}

// Solution 3: Space Optimized Bottom-Up (2D)
// Time: O(N*M^2), Space: O(M^2)
class Solution3_SpaceOptimized2D {
    public int maximumChocolates(int n, int m, int[][] grid) {
        // Use two 2D arrays for current and next row
        int[][] prev = new int[m][m];
        int[][] curr = new int[m][m];

        // Initialize prev with negative values
        for (int i = 0; i < m; i++) {
            Arrays.fill(prev[i], Integer.MIN_VALUE);
        }

        // Base case: last row
        for (int aliceCol = 0; aliceCol < m; aliceCol++) {
            for (int bobCol = 0; bobCol < m; bobCol++) {
                if (aliceCol == bobCol) {
                    prev[aliceCol][bobCol] = grid[n - 1][aliceCol];
                } else {
                    prev[aliceCol][bobCol] = grid[n - 1][aliceCol] + grid[n - 1][bobCol];
                }
            }
        }

        // Process from bottom to top
        for (int row = n - 2; row >= 0; row--) {
            // Initialize current row
            for (int i = 0; i < m; i++) {
                Arrays.fill(curr[i], Integer.MIN_VALUE);
            }

            for (int aliceCol = 0; aliceCol < m; aliceCol++) {
                for (int bobCol = 0; bobCol < m; bobCol++) {
                    // Current chocolates
                    int chocolates = 0;
                    if (aliceCol == bobCol) {
                        chocolates = grid[row][aliceCol];
                    } else {
                        chocolates = grid[row][aliceCol] + grid[row][bobCol];
                    }

                    // Try all moves
                    int maxFromBelow = Integer.MIN_VALUE;
                    int[] directions = { -1, 0, 1 };

                    for (int aliceMove : directions) {
                        for (int bobMove : directions) {
                            int newAliceCol = aliceCol + aliceMove;
                            int newBobCol = bobCol + bobMove;

                            if (newAliceCol >= 0 && newAliceCol < m &&
                                    newBobCol >= 0 && newBobCol < m) {
                                maxFromBelow = Math.max(maxFromBelow, prev[newAliceCol][newBobCol]);
                            }
                        }
                    }

                    if (maxFromBelow != Integer.MIN_VALUE) {
                        curr[aliceCol][bobCol] = chocolates + maxFromBelow;
                    }
                }
            }

            // Swap arrays
            int[][] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[0][m - 1];
    }
}

// Solution 4: With Path Reconstruction
// Time: O(N*M^2), Space: O(N*M^2)
class Solution4_WithPath {
    public class Result {
        int maxChocolates;
        List<int[]> alicePath; // List of [row, col] positions
        List<int[]> bobPath;

        Result(int chocolates, List<int[]> alicePath, List<int[]> bobPath) {
            this.maxChocolates = chocolates;
            this.alicePath = new ArrayList<>(alicePath);
            this.bobPath = new ArrayList<>(bobPath);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Max Chocolates: ").append(maxChocolates).append("\n");
            sb.append("Alice Path: ");
            for (int i = 0; i < alicePath.size(); i++) {
                int[] pos = alicePath.get(i);
                sb.append("(").append(pos[0]).append(",").append(pos[1]).append(")");
                if (i < alicePath.size() - 1)
                    sb.append(" -> ");
            }
            sb.append("\nBob Path: ");
            for (int i = 0; i < bobPath.size(); i++) {
                int[] pos = bobPath.get(i);
                sb.append("(").append(pos[0]).append(",").append(pos[1]).append(")");
                if (i < bobPath.size() - 1)
                    sb.append(" -> ");
            }
            return sb.toString();
        }
    }

    public Result maximumChocolatesWithPath(int n, int m, int[][] grid) {
        // DP table
        int[][][] dp = new int[n][m][m];
        // Parent tracking: [aliceMove, bobMove] for each state
        int[][][] aliceParent = new int[n][m][m];
        int[][][] bobParent = new int[n][m][m];

        // Initialize
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                Arrays.fill(dp[i][j], Integer.MIN_VALUE);
                Arrays.fill(aliceParent[i][j], -1);
                Arrays.fill(bobParent[i][j], -1);
            }
        }

        // Base case
        for (int aliceCol = 0; aliceCol < m; aliceCol++) {
            for (int bobCol = 0; bobCol < m; bobCol++) {
                if (aliceCol == bobCol) {
                    dp[n - 1][aliceCol][bobCol] = grid[n - 1][aliceCol];
                } else {
                    dp[n - 1][aliceCol][bobCol] = grid[n - 1][aliceCol] + grid[n - 1][bobCol];
                }
            }
        }

        // Fill DP
        int[] directions = { -1, 0, 1 };
        for (int row = n - 2; row >= 0; row--) {
            for (int aliceCol = 0; aliceCol < m; aliceCol++) {
                for (int bobCol = 0; bobCol < m; bobCol++) {
                    int chocolates = (aliceCol == bobCol) ? grid[row][aliceCol]
                            : grid[row][aliceCol] + grid[row][bobCol];

                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            int newAliceCol = aliceCol + directions[i];
                            int newBobCol = bobCol + directions[j];

                            if (newAliceCol >= 0 && newAliceCol < m &&
                                    newBobCol >= 0 && newBobCol < m) {

                                int newValue = chocolates + dp[row + 1][newAliceCol][newBobCol];
                                if (newValue > dp[row][aliceCol][bobCol]) {
                                    dp[row][aliceCol][bobCol] = newValue;
                                    aliceParent[row][aliceCol][bobCol] = i;
                                    bobParent[row][aliceCol][bobCol] = j;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reconstruct paths
        List<int[]> alicePath = new ArrayList<>();
        List<int[]> bobPath = new ArrayList<>();

        int aliceCol = 0, bobCol = m - 1;
        for (int row = 0; row < n; row++) {
            alicePath.add(new int[] { row, aliceCol });
            bobPath.add(new int[] { row, bobCol });

            if (row < n - 1) {
                int aliceMove = aliceParent[row][aliceCol][bobCol];
                int bobMove = bobParent[row][aliceCol][bobCol];

                if (aliceMove != -1 && bobMove != -1) {
                    aliceCol += directions[aliceMove];
                    bobCol += directions[bobMove];
                }
            }
        }

        return new Result(dp[0][0][m - 1], alicePath, bobPath);
    }
}

// Solution 5: Iterative with Queue (BFS-like)
// Time: O(N*M^2), Space: O(M^2)
class Solution5_Iterative {
    private static class State {
        int row, aliceCol, bobCol, chocolates;

        State(int row, int aliceCol, int bobCol, int chocolates) {
            this.row = row;
            this.aliceCol = aliceCol;
            this.bobCol = bobCol;
            this.chocolates = chocolates;
        }

        String getKey() {
            return row + "," + aliceCol + "," + bobCol;
        }
    }

    public int maximumChocolates(int n, int m, int[][] grid) {
        Map<String, Integer> maxChocolates = new HashMap<>();
        Queue<State> queue = new LinkedList<>();

        // Start state
        int initialChocolates = grid[0][0] + grid[0][m - 1];
        if (m == 1)
            initialChocolates = grid[0][0]; // Same position

        queue.offer(new State(0, 0, m - 1, initialChocolates));
        maxChocolates.put("0,0," + (m - 1), initialChocolates);

        int[] directions = { -1, 0, 1 };

        while (!queue.isEmpty()) {
            State current = queue.poll();

            if (current.row == n - 1)
                continue; // Reached end

            // Try all moves
            for (int aliceMove : directions) {
                for (int bobMove : directions) {
                    int newAliceCol = current.aliceCol + aliceMove;
                    int newBobCol = current.bobCol + bobMove;
                    int newRow = current.row + 1;

                    // Check bounds
                    if (newAliceCol >= 0 && newAliceCol < m &&
                            newBobCol >= 0 && newBobCol < m) {

                        int newChocolates = current.chocolates;
                        if (newAliceCol == newBobCol) {
                            newChocolates += grid[newRow][newAliceCol];
                        } else {
                            newChocolates += grid[newRow][newAliceCol] + grid[newRow][newBobCol];
                        }

                        String key = newRow + "," + newAliceCol + "," + newBobCol;

                        if (!maxChocolates.containsKey(key) ||
                                maxChocolates.get(key) < newChocolates) {
                            maxChocolates.put(key, newChocolates);
                            queue.offer(new State(newRow, newAliceCol, newBobCol, newChocolates));
                        }
                    }
                }
            }
        }

        // Find maximum in last row
        int result = 0;
        for (int aliceCol = 0; aliceCol < m; aliceCol++) {
            for (int bobCol = 0; bobCol < m; bobCol++) {
                String key = (n - 1) + "," + aliceCol + "," + bobCol;
                if (maxChocolates.containsKey(key)) {
                    result = Math.max(result, maxChocolates.get(key));
                }
            }
        }

        return result;
    }
}

// Test class with comprehensive testing
class TestChocolatesCollection {
    public static void main(String[] args) {
        Solution1_TopDownMemo sol1 = new Solution1_TopDownMemo();
        Solution2_BottomUp3D sol2 = new Solution2_BottomUp3D();
        Solution3_SpaceOptimized2D sol3 = new Solution3_SpaceOptimized2D();
        Solution4_WithPath sol4 = new Solution4_WithPath();
        Solution5_Iterative sol5 = new Solution5_Iterative();

        // Test case 1: Basic example
        int[][] grid1 = {
                { 2, 3, 1, 2 },
                { 3, 4, 2, 2 },
                { 5, 6, 3, 5 }
        };

        System.out.println("Test 1 - Grid:");
        printGrid(grid1);
        System.out.println("Results:");
        System.out.println("Top-Down Memo: " + sol1.maximumChocolates(3, 4, grid1));
        System.out.println("Bottom-Up 3D: " + sol2.maximumChocolates(3, 4, grid1));
        System.out.println("Space Optimized: " + sol3.maximumChocolates(3, 4, grid1));
        System.out.println("Iterative: " + sol5.maximumChocolates(3, 4, grid1));

        Solution4_WithPath.Result result1 = sol4.maximumChocolatesWithPath(3, 4, grid1);
        System.out.println(result1);

        // Test case 2: Single column
        int[][] grid2 = { { 1 }, { 3 }, { 3 } };

        System.out.println("\nTest 2 - Single column:");
        printGrid(grid2);
        System.out.println("Bottom-Up 3D: " + sol2.maximumChocolates(3, 1, grid2));

        // Test case 3: Single row
        int[][] grid3 = { { 1, 3, 1, 5 } };

        System.out.println("\nTest 3 - Single row:");
        printGrid(grid3);
        System.out.println("Bottom-Up 3D: " + sol2.maximumChocolates(1, 4, grid3));

        // Test case 4: 2x2 grid
        int[][] grid4 = {
                { 1, 3 },
                { 5, 7 }
        };

        System.out.println("\nTest 4 - 2x2 grid:");
        printGrid(grid4);
        System.out.println("Space Optimized: " + sol3.maximumChocolates(2, 2, grid4));

        Solution4_WithPath.Result result4 = sol4.maximumChocolatesWithPath(2, 2, grid4);
        System.out.println(result4);

        // Performance test
        int[][] largeGrid = generateRandomGrid(50, 20, 1, 10);
        long start = System.currentTimeMillis();
        int result = sol3.maximumChocolates(50, 20, largeGrid);
        long end = System.currentTimeMillis();
        System.out.println("\nPerformance Test (50x20): " + result +
                " in " + (end - start) + "ms");
    }

    private static void printGrid(int[][] grid) {
        for (int[] row : grid) {
            System.out.println(Arrays.toString(row));
        }
    }

    private static int[][] generateRandomGrid(int n, int m, int min, int max) {
        Random rand = new Random(42);
        int[][] grid = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                grid[i][j] = rand.nextInt(max - min + 1) + min;
            }
        }
        return grid;
    }

}

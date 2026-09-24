import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * ============================================================================
 * PROBLEM STATEMENT: 01 Matrix (Nearest Zero)
 * Given an m x n binary matrix 'mat', find the distance from each cell to 
 * the nearest 0. The distance between two adjacent cells is 1.
 * Cells left, right, above, and below are considered adjacent.
 * 
 * Constraints:
 * 1 <= mat.row, mat.col <= 50
 * 1 <= mat.row * mat.col <= 2500
 * mat[i][j] is either 0 or 1.
 * There is at least one 0 in mat.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this problem is a phenomenal test of algorithm selection.
 * Before writing code, you should discuss the inherent danger of 4-directional DP:
 * 
 * Q: "Can I use standard Top-Down Recursion with Memoization here?"
 * A: NO! Standard Top-Down DP falls apart on this problem. Why? Because we can 
 *    move in 4 directions (Up, Down, Left, Right). This creates cyclic dependencies. 
 *    (Cell A asks Cell B, Cell B asks Cell A -> Infinite Loop / StackOverflow). 
 *    To fix this in recursion, you need a 'visited' set, which turns it into a 
 *    massive DFS that explores terrible, winding paths before finding the shortest 
 *    one, ruining the time complexity.
 * 
 * CRITICAL SENIOR INSIGHT:
 * "Because we are finding the 'shortest path' in an unweighted grid, the absolute 
 * most natural algorithmic fit is Multi-Source Breadth-First Search (BFS). 
 * However, we can also solve this using a brilliant trick called '2-Pass DP' 
 * (Tabulation), which breaks the cyclic dependency by splitting the 4 directions 
 * into two separate, non-cyclic sweeps across the grid."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * METHOD A (Multi-Source BFS):
 * Instead of running a BFS from every single '1' to find a '0' (which is horribly 
 * slow), we run ONE massive BFS starting from ALL '0's simultaneously. The '0's 
 * act like water dropping into a pond, rippling outwards. The first time a ripple 
 * touches a '1', we guarantee it is the absolute shortest distance.
 * 
 * METHOD B (2-Pass Tabulation DP):
 * We can't check all 4 directions at once in a DP array because future cells 
 * haven't been calculated yet. But we CAN check directions that are already "behind" us:
 * - PASS 1 (Top-Left to Bottom-Right): We only look UP and LEFT.
 * - PASS 2 (Bottom-Right to Top-Left): We only look DOWN and RIGHT.
 * By combining these two passes, every cell effectively gets evaluated against 
 * all 4 of its neighbors.
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING (2-Pass DP)
 * ----------------------------------------------------------------------------
 * Example: 
 * [1, 1, 1]
 * [1, 1, 1]
 * [1, 1, 0]
 * 
 * INITIALIZATION (0s stay 0, 1s become Infinity):
 * [INF, INF, INF]
 * [INF, INF, INF]
 * [INF, INF,   0]
 * 
 * PASS 1 (Top-Left to Bottom-Right) -> Look UP and LEFT:
 * Every cell looks up and left, but everything is INF until we hit the 0.
 * End of Pass 1:
 * [INF, INF, INF]
 * [INF, INF, INF]
 * [INF, INF,   0]
 * 
 * PASS 2 (Bottom-Right to Top-Left) -> Look DOWN and RIGHT:
 * We start at the bottom right and work backward.
 * - Cell (1, 2) looks DOWN at (2, 2) which is 0. Distance becomes 1!
 * - Cell (2, 1) looks RIGHT at (2, 2) which is 0. Distance becomes 1!
 * - Cell (1, 1) looks DOWN (1) and RIGHT (1). Distance becomes 2!
 * End of Pass 2:
 * [4, 3, 2]
 * [3, 2, 1]
 * [2, 1, 0]
 */
public class ZeroOneMatrix {

    // A safe infinity value to prevent integer overflow when adding 1.
    // 2500 is the max cells, so no path can be longer than 2500. 10000 is perfectly safe.
    private static final int INF = 10000;

    /**
     * ========================================================================
     * APPROACH 1: Multi-Source BFS (The standard Graph approach)
     * ========================================================================
     * Idea: Enqueue all '0's. Set all '1's to a placeholder. Dequeue layer 
     * by layer, updating neighbors with current_distance + 1.
     * 
     * Time Complexity: O(m * n) - We visit each cell exactly once.
     * Space Complexity: O(m * n) - In the worst case, the Queue holds all cells.
     */
    public int[][] updateMatrixBFS(int[][] mat) {
        if (mat == null || mat.length == 0) return new int[0][0];
        
        int m = mat.length;
        int n = mat[0].length;
        int[][] dist = new int[m][n];
        Queue<int[]> queue = new LinkedList<>();

        // Seed the Queue with all the 0s (our starting ripples).
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    dist[i][j] = 0;
                    queue.offer(new int[]{i, j});
                } else {
                    // Mark 1s with infinity so we know they are unvisited
                    dist[i][j] = INF;
                }
            }
        }

        // Standard 4-directional array for up, down, left, right
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];

            for (int[] dir : directions) {
                int newR = r + dir[0];
                int newC = c + dir[1];

                // If the neighbor is within bounds AND its current recorded distance 
                // is strictly worse than coming through our current cell...
                if (newR >= 0 && newR < m && newC >= 0 && newC < n) {
                    if (dist[newR][newC] > dist[r][c] + 1) {
                        
                        // We found a faster route to this neighbor! 
                        // Update its distance and add it to the queue to act as a 
                        // new ripple center.
                        dist[newR][newC] = dist[r][c] + 1;
                        queue.offer(new int[]{newR, newC});
                    }
                }
            }
        }

        return dist;
    }

    /**
     * ========================================================================
     * APPROACH 2: 2-Pass DP Tabulation (L4/L5 DP Flex)
     * ========================================================================
     * Idea: We break the 4-directional cyclic dependency by doing two distinct 
     * mathematical sweeps over the grid.
     * 
     * Time Complexity: O(m * n) - Two linear passes over the grid.
     * Space Complexity: O(1) auxiliary - We modify the output array directly, 
     * avoiding the O(m * n) Queue overhead completely!
     */
    public int[][] updateMatrixDP(int[][] mat) {
        if (mat == null || mat.length == 0) return new int[0][0];
        
        int m = mat.length;
        int n = mat[0].length;
        
        // We will build our DP answers directly into a result array.
        int[][] dp = new int[m][n];

        // BASE CASE REASONING (Initialization):
        // If the cell is originally 0, its distance to the nearest 0 is exactly 0.
        // If the cell is 1, we don't know the distance yet. We fill it with our 
        // safe "Infinity" value so it can be overwritten by Math.min() later.
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    dp[i][j] = 0;
                } else {
                    dp[i][j] = INF;
                }
            }
        }

        // --- DETAILED TABULATION EXPLANATION ---
        
        // PASS 1: Top-Left to Bottom-Right
        // In this pass, we are only allowed to look UP and LEFT.
        // We are answering: "What is the shortest distance to a 0 if I am ONLY 
        // allowed to travel from the top-left downward/rightward to reach here?"
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                
                // If it's already 0, we can't get any better. Skip.
                if (dp[i][j] == 0) continue;
                
                // Check the cell directly UP (if it exists)
                if (i > 0) {
                    // The distance would be: distance to nearest 0 from the cell UP, plus 1 step to get here.
                    dp[i][j] = Math.min(dp[i][j], dp[i - 1][j] + 1);
                }
                
                // Check the cell directly LEFT (if it exists)
                if (j > 0) {
                    // The distance would be: distance to nearest 0 from the cell LEFT, plus 1 step to get here.
                    dp[i][j] = Math.min(dp[i][j], dp[i][j - 1] + 1);
                }
            }
        }

        // PASS 2: Bottom-Right to Top-Left
        // In this pass, we are only allowed to look DOWN and RIGHT.
        // We are combining our previous knowledge (Up/Left) with this new knowledge.
        // "What if there is a much closer 0 sitting just below me, or just to my right?"
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                
                if (dp[i][j] == 0) continue;
                
                // Check the cell directly DOWN (if it exists)
                if (i < m - 1) {
                    // Compare our current best distance against walking DOWN.
                    dp[i][j] = Math.min(dp[i][j], dp[i + 1][j] + 1);
                }
                
                // Check the cell directly RIGHT (if it exists)
                if (j < n - 1) {
                    // Compare our current best distance against walking RIGHT.
                    dp[i][j] = Math.min(dp[i][j], dp[i][j + 1] + 1);
                }
            }
        }

        // After both sweeps, every cell has been evaluated against all 4 of its neighbors.
        // The DP matrix now holds the absolute minimum distance for every cell.
        return dp;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new ZeroOneMatrix();
        
        record TestCase(int[][] mat) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[][]{
                {0, 0, 0},
                {0, 1, 0},
                {0, 0, 0}
            }),
            new TestCase(new int[][]{
                {0, 0, 0},
                {0, 1, 0},
                {1, 1, 1}
            }),
            new TestCase(new int[][]{
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 0}
            }) // Stresses the 2-pass propagation
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Input Matrix:");
            for (int[] row : tc.mat) {
                System.out.println(Arrays.toString(row));
            }
            
            int[][] resBFS = solver.updateMatrixBFS(tc.mat);
            System.out.println("\nMulti-Source BFS Result:");
            for (int[] row : resBFS) {
                System.out.println(Arrays.toString(row));
            }
            
            int[][] resDP = solver.updateMatrixDP(tc.mat);
            System.out.println("\n2-Pass DP Tabulation Result:");
            for (int[] row : resDP) {
                System.out.println(Arrays.toString(row));
            }
            System.out.println("\n");
        }
    }
}

/*
 * The demons had captured the princess and imprisoned her in the bottom-right
 * corner of a dungeon. The dungeon consists of m x n rooms laid out in a 2D
 * grid. Our valiant knight was initially positioned in the top-left room and
 * must fight his way through dungeon to rescue the princess.
 * 
 * The knight has an initial health point represented by a positive integer. If
 * at any point his health point drops to 0 or below, he dies immediately.
 * 
 * Some of the rooms are guarded by demons (represented by negative integers),
 * so the knight loses health upon entering these rooms; other rooms are either
 * empty (represented as 0) or contain magic orbs that increase the knight's
 * health (represented by positive integers).
 * 
 * To reach the princess as quickly as possible, the knight decides to move only
 * rightward or downward in each step.
 * 
 * Return the knight's minimum initial health so that he can rescue the
 * princess.
 * 
 * Note that any room can contain threats or power-ups, even the first room the
 * knight enters and the bottom-right room where the princess is imprisoned.
 * 
 * Example 1:
 * Input: dungeon = [[-2,-3,3],[-5,-10,1],[10,30,-5]]
 * Output: 7
 * Explanation: The initial health of the knight must be at least 7 if he
 * follows the optimal path: RIGHT-> RIGHT -> DOWN -> DOWN.
 * 
 * Example 2:
 * Input: dungeon = [[0]]
 * Output: 1
 */

import java.util.Arrays;
import java.util.PriorityQueue;

class Solution {

    /**
     * Approach 1: Dynamic Programming (Bottom-up)
     * Work backwards from princess to knight's starting position
     * 
     * Time Complexity: O(m * n) - visit each cell once
     * Space Complexity: O(m * n) - DP table
     */
    public int calculateMinimumHP(int[][] dungeon) {
        int m = dungeon.length;
        int n = dungeon[0].length;

        // dp[i][j] represents minimum health needed at position (i,j) to reach princess
        int[][] dp = new int[m][n];

        // Start from bottom-right (princess location)
        // Knight needs at least 1 health after entering princess room
        dp[m - 1][n - 1] = Math.max(1, 1 - dungeon[m - 1][n - 1]);

        // Fill last column (can only move down)
        for (int i = m - 2; i >= 0; i--) {
            dp[i][n - 1] = Math.max(1, dp[i + 1][n - 1] - dungeon[i][n - 1]);
        }

        // Fill last row (can only move right)
        for (int j = n - 2; j >= 0; j--) {
            dp[m - 1][j] = Math.max(1, dp[m - 1][j + 1] - dungeon[m - 1][j]);
        }

        // Fill remaining cells
        for (int i = m - 2; i >= 0; i--) {
            for (int j = n - 2; j >= 0; j--) {
                // Choose the path that requires minimum health
                int minHealthNext = Math.min(dp[i + 1][j], dp[i][j + 1]);
                dp[i][j] = Math.max(1, minHealthNext - dungeon[i][j]);
            }
        }

        return dp[0][0];
    }

    public int calculateMinimumHP2(int[][] dungeon) {
        int m = dungeon.length;
        int n = dungeon[0].length;

        int[][] dp = new int[m + 1][n + 1];

        // Fill dp with infinity
        for (int[] row : dp) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        // Initialize the cell after princess's room with 1
        dp[m][n - 1] = 1;
        dp[m - 1][n] = 1;

        // Fill the table backwards
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                int minNeeded = Math.min(dp[i + 1][j], dp[i][j + 1]) - dungeon[i][j];
                dp[i][j] = Math.max(1, minNeeded);
            }
        }

        return dp[0][0];
    }

    /**
     * Approach 2: Space Optimized Dynamic Programming
     * Since we only need the next row and current row, optimize space to O(n)
     * 
     * Time Complexity: O(m * n) - visit each cell once
     * Space Complexity: O(n) - only store one row
     */
    public int calculateMinimumHPOptimized(int[][] dungeon) {
        int m = dungeon.length;
        int n = dungeon[0].length;

        // Only need to store one row at a time
        int[] dp = new int[n];

        // Initialize last row
        dp[n - 1] = Math.max(1, 1 - dungeon[m - 1][n - 1]);

        // Fill last row from right to left
        for (int j = n - 2; j >= 0; j--) {
            dp[j] = Math.max(1, dp[j + 1] - dungeon[m - 1][j]);
        }

        // Process remaining rows from bottom to top
        for (int i = m - 2; i >= 0; i--) {
            // Update rightmost cell (can only come from below)
            dp[n - 1] = Math.max(1, dp[n - 1] - dungeon[i][n - 1]);

            // Update remaining cells from right to left
            for (int j = n - 2; j >= 0; j--) {
                int minHealthNext = Math.min(dp[j], dp[j + 1]); // down, right
                dp[j] = Math.max(1, minHealthNext - dungeon[i][j]);
            }
        }

        return dp[0];
    }

    /**
     * Test the solution with provided examples
     */
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Example 1
        int[][] dungeon1 = { { -2, -3, 3 }, { -5, -10, 1 }, { 10, 30, -5 } };
        System.out.println("Example 1: " + solution.calculateMinimumHP(dungeon1)); // Expected: 7
        System.out.println("Example 1 (Optimized): " + solution.calculateMinimumHPOptimized(dungeon1)); // Expected: 7

        // Example 2
        int[][] dungeon2 = { { 0 } };
        System.out.println("Example 2: " + solution.calculateMinimumHP(dungeon2)); // Expected: 1
        System.out.println("Example 2 (Optimized): " + solution.calculateMinimumHPOptimized(dungeon2)); // Expected: 1

        // Additional test case
        int[][] dungeon3 = { { 1, -3, 3 }, { 0, -2, 0 }, { -3, -3, -3 } };
        System.out.println("Additional test: " + solution.calculateMinimumHP(dungeon3));
        System.out.println("Additional test (Optimized): " + solution.calculateMinimumHPOptimized(dungeon3));
    }
}

/*
 * Algorithm Explanation:
 * 
 * 1. **Why work backwards?**
 * - Working forward is tricky because we don't know the minimum health needed
 * at each step
 * - Working backwards, we know the knight needs at least 1 health when reaching
 * the princess
 * - This allows us to calculate the exact minimum health needed at each
 * position
 * 
 * 2. **DP State Definition:**
 * - dp[i][j] = minimum health needed when entering cell (i,j) to successfully
 * reach the princess
 * - The knight must have at least 1 health after entering any cell
 * 
 * 3. **Recurrence Relation:**
 * - dp[i][j] = max(1, min(dp[i+1][j], dp[i][j+1]) - dungeon[i][j])
 * - We take the minimum of the two possible next moves (down or right)
 * - Subtract the current cell's value (add health if positive, lose health if
 * negative)
 * - Ensure minimum health is always at least 1
 * 
 * 4. **Base Case:**
 * - dp[m-1][n-1] = max(1, 1 - dungeon[m-1][n-1])
 * - At princess location, knight needs enough health to survive the room and
 * have 1 health left
 * 
 * Time Complexity: O(m * n) - we visit each cell exactly once
 * Space Complexity:
 * - Approach 1: O(m * n) for the DP table
 * - Approach 2: O(n) using space optimization since we only need the current
 * and next row
 * 
 * Example 1 walkthrough:
 * dungeon = [[-2,-3,3],[-5,-10,1],[10,30,-5]]
 * 
 * Working backwards:
 * - dp[2][2] = max(1, 1 - (-5)) = max(1, 6) = 6
 * - dp[2][1] = max(1, 6 - 30) = max(1, -24) = 1
 * - dp[2][0] = max(1, 1 - 10) = max(1, -9) = 1
 * - dp[1][2] = max(1, 6 - 1) = 5
 * - dp[1][1] = max(1, min(1, 5) - (-10)) = max(1, 11) = 11
 * - dp[1][0] = max(1, 11 - (-5)) = 16
 * - dp[0][2] = max(1, 5 - 3) = 2
 * - dp[0][1] = max(1, min(11, 2) - (-3)) = max(1, 5) = 5
 * - dp[0][0] = max(1, min(16, 5) - (-2)) = max(1, 7) = 7
 * 
 * Answer: 7
 */

class DungeonGameAllSolutions {

    /**
     * APPROACH 1: TOP-DOWN DYNAMIC PROGRAMMING (MEMOIZATION)
     * Recursive approach with memoization working backwards from princess
     * 
     * Time Complexity: O(m * n) - each cell computed once
     * Space Complexity: O(m * n) - memoization table + recursion stack
     */
    public int calculateMinimumHP_TopDown(int[][] dungeon) {
        int m = dungeon.length, n = dungeon[0].length;
        Integer[][] memo = new Integer[m][n];
        return topDownHelper(dungeon, 0, 0, m, n, memo);
    }

    private int topDownHelper(int[][] dungeon, int i, int j, int m, int n, Integer[][] memo) {
        // Base case: reached princess
        if (i == m - 1 && j == n - 1) {
            return Math.max(1, 1 - dungeon[i][j]);
        }

        if (memo[i][j] != null)
            return memo[i][j];

        int minHealthNeeded = Integer.MAX_VALUE;

        // Move right
        if (j + 1 < n) {
            minHealthNeeded = Math.min(minHealthNeeded, topDownHelper(dungeon, i, j + 1, m, n, memo));
        }

        // Move down
        if (i + 1 < m) {
            minHealthNeeded = Math.min(minHealthNeeded, topDownHelper(dungeon, i + 1, j, m, n, memo));
        }

        memo[i][j] = Math.max(1, minHealthNeeded - dungeon[i][j]);
        return memo[i][j];
    }

    /**
     * APPROACH 2: BOTTOM-UP DYNAMIC PROGRAMMING (TABULATION)
     * Classic DP approach working backwards from princess to knight
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     */
    public int calculateMinimumHP_BottomUp(int[][] dungeon) {
        int m = dungeon.length, n = dungeon[0].length;
        int[][] dp = new int[m][n];

        // Base case: princess location
        dp[m - 1][n - 1] = Math.max(1, 1 - dungeon[m - 1][n - 1]);

        // Fill last column (can only move down)
        for (int i = m - 2; i >= 0; i--) {
            dp[i][n - 1] = Math.max(1, dp[i + 1][n - 1] - dungeon[i][n - 1]);
        }

        // Fill last row (can only move right)
        for (int j = n - 2; j >= 0; j--) {
            dp[m - 1][j] = Math.max(1, dp[m - 1][j + 1] - dungeon[m - 1][j]);
        }

        // Fill remaining cells
        for (int i = m - 2; i >= 0; i--) {
            for (int j = n - 2; j >= 0; j--) {
                int minHealthNext = Math.min(dp[i + 1][j], dp[i][j + 1]);
                dp[i][j] = Math.max(1, minHealthNext - dungeon[i][j]);
            }
        }

        return dp[0][0];
    }

    /**
     * APPROACH 3: SPACE-OPTIMIZED BOTTOM-UP DP
     * Uses only O(n) space by processing row by row
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n)
     */
    public int calculateMinimumHP_SpaceOptimized(int[][] dungeon) {
        int m = dungeon.length, n = dungeon[0].length;
        int[] dp = new int[n];

        // Initialize last row
        dp[n - 1] = Math.max(1, 1 - dungeon[m - 1][n - 1]);
        for (int j = n - 2; j >= 0; j--) {
            dp[j] = Math.max(1, dp[j + 1] - dungeon[m - 1][j]);
        }

        // Process remaining rows from bottom to top
        for (int i = m - 2; i >= 0; i--) {
            dp[n - 1] = Math.max(1, dp[n - 1] - dungeon[i][n - 1]);
            for (int j = n - 2; j >= 0; j--) {
                int minHealthNext = Math.min(dp[j], dp[j + 1]);
                dp[j] = Math.max(1, minHealthNext - dungeon[i][j]);
            }
        }

        return dp[0];
    }

    /**
     * APPROACH 4: SPACE-OPTIMIZED WITH TWO ARRAYS
     * Alternative space optimization using two arrays
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n)
     */
    public int calculateMinimumHP_TwoArrays(int[][] dungeon) {
        int m = dungeon.length, n = dungeon[0].length;
        int[] curr = new int[n];
        int[] next = new int[n];

        // Initialize last row
        next[n - 1] = Math.max(1, 1 - dungeon[m - 1][n - 1]);
        for (int j = n - 2; j >= 0; j--) {
            next[j] = Math.max(1, next[j + 1] - dungeon[m - 1][j]);
        }

        // Process remaining rows
        for (int i = m - 2; i >= 0; i--) {
            curr[n - 1] = Math.max(1, next[n - 1] - dungeon[i][n - 1]);
            for (int j = n - 2; j >= 0; j--) {
                int minHealthNext = Math.min(next[j], curr[j + 1]);
                curr[j] = Math.max(1, minHealthNext - dungeon[i][j]);
            }
            // Swap arrays
            int[] temp = next;
            next = curr;
            curr = temp;
        }

        return next[0];
    }

    /**
     * APPROACH 5: IN-PLACE DYNAMIC PROGRAMMING
     * Modifies the input array to save space (destructive)
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(1)
     */
    public int calculateMinimumHP_InPlace(int[][] dungeon) {
        int m = dungeon.length, n = dungeon[0].length;

        // Base case: princess location
        dungeon[m - 1][n - 1] = Math.max(1, 1 - dungeon[m - 1][n - 1]);

        // Fill last column
        for (int i = m - 2; i >= 0; i--) {
            dungeon[i][n - 1] = Math.max(1, dungeon[i + 1][n - 1] - dungeon[i][n - 1]);
        }

        // Fill last row
        for (int j = n - 2; j >= 0; j--) {
            dungeon[m - 1][j] = Math.max(1, dungeon[m - 1][j + 1] - dungeon[m - 1][j]);
        }

        // Fill remaining cells
        for (int i = m - 2; i >= 0; i--) {
            for (int j = n - 2; j >= 0; j--) {
                int minHealthNext = Math.min(dungeon[i + 1][j], dungeon[i][j + 1]);
                dungeon[i][j] = Math.max(1, minHealthNext - dungeon[i][j]);
            }
        }

        return dungeon[0][0];
    }

    /**
     * APPROACH 6: RECURSIVE WITHOUT MEMOIZATION (BRUTE FORCE)
     * Pure recursive solution - exponential time complexity
     * 
     * Time Complexity: O(2^(m+n)) - exponential
     * Space Complexity: O(m + n) - recursion stack
     */
    public int calculateMinimumHP_Recursive(int[][] dungeon) {
        return recursiveHelper(dungeon, 0, 0, dungeon.length, dungeon[0].length);
    }

    private int recursiveHelper(int[][] dungeon, int i, int j, int m, int n) {
        // Base case: reached princess
        if (i == m - 1 && j == n - 1) {
            return Math.max(1, 1 - dungeon[i][j]);
        }

        int minHealthNeeded = Integer.MAX_VALUE;

        // Move right
        if (j + 1 < n) {
            minHealthNeeded = Math.min(minHealthNeeded, recursiveHelper(dungeon, i, j + 1, m, n));
        }

        // Move down
        if (i + 1 < m) {
            minHealthNeeded = Math.min(minHealthNeeded, recursiveHelper(dungeon, i + 1, j, m, n));
        }

        return Math.max(1, minHealthNeeded - dungeon[i][j]);
    }

    /**
     * APPROACH 7: DIJKSTRA'S ALGORITHM VARIANT
     * Uses priority queue to find minimum initial health path
     * 
     * Time Complexity: O(m * n * log(m * n))
     * Space Complexity: O(m * n)
     */
    public int calculateMinimumHP_Dijkstra(int[][] dungeon) {
        int m = dungeon.length, n = dungeon[0].length;

        // minHealth[i][j] = minimum initial health needed to reach (i,j)
        int[][] minHealth = new int[m][n];
        for (int[] row : minHealth) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        // Priority queue: [health_needed, row, col]
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);

        // Start from princess and work backwards
        int healthAtPrincess = Math.max(1, 1 - dungeon[m - 1][n - 1]);
        minHealth[m - 1][n - 1] = healthAtPrincess;
        pq.offer(new int[] { healthAtPrincess, m - 1, n - 1 });

        int[][] dirs = { { -1, 0 }, { 0, -1 } }; // up, left (working backwards)

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int health = curr[0], row = curr[1], col = curr[2];

            if (health > minHealth[row][col])
                continue;

            for (int[] dir : dirs) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newCol >= 0) {
                    int newHealth = Math.max(1, health - dungeon[newRow][newCol]);

                    if (newHealth < minHealth[newRow][newCol]) {
                        minHealth[newRow][newCol] = newHealth;
                        pq.offer(new int[] { newHealth, newRow, newCol });
                    }
                }
            }
        }

        return minHealth[0][0];
    }

    /**
     * APPROACH 8: BFS WITH PRIORITY QUEUE
     * Alternative graph-based approach using BFS
     * 
     * Time Complexity: O(m * n * log(m * n))
     * Space Complexity: O(m * n)
     */
    public int calculateMinimumHP_BFS(int[][] dungeon) {
        int m = dungeon.length, n = dungeon[0].length;
        boolean[][] visited = new boolean[m][n];

        // Priority queue: [health_needed, row, col]
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);

        // Work backwards from princess
        int initialHealth = Math.max(1, 1 - dungeon[m - 1][n - 1]);
        pq.offer(new int[] { initialHealth, m - 1, n - 1 });

        int[][] dirs = { { -1, 0 }, { 0, -1 } }; // up, left

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int health = curr[0], row = curr[1], col = curr[2];

            if (row == 0 && col == 0)
                return health;
            if (visited[row][col])
                continue;
            visited[row][col] = true;

            for (int[] dir : dirs) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newCol >= 0 && !visited[newRow][newCol]) {
                    int newHealth = Math.max(1, health - dungeon[newRow][newCol]);
                    pq.offer(new int[] { newHealth, newRow, newCol });
                }
            }
        }

        return -1; // Should never reach here
    }

    /**
     * APPROACH 9: BINARY SEARCH + PATH VALIDATION
     * Binary search on the answer with path validation
     * 
     * Time Complexity: O(log(max_health) * m * n)
     * Space Complexity: O(m * n) for validation
     */
    public int calculateMinimumHP_BinarySearch(int[][] dungeon) {
        int left = 1, right = 1000000; // Reasonable upper bound

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (canSurvive(dungeon, mid)) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    private boolean canSurvive(int[][] dungeon, int initialHealth) {
        int m = dungeon.length, n = dungeon[0].length;
        int[][] dp = new int[m][n];

        dp[0][0] = initialHealth + dungeon[0][0];
        if (dp[0][0] <= 0)
            return false;

        // Fill first row
        for (int j = 1; j < n; j++) {
            dp[0][j] = dp[0][j - 1] + dungeon[0][j];
            if (dp[0][j] <= 0)
                dp[0][j] = -1; // Mark as impossible
        }

        // Fill first column
        for (int i = 1; i < m; i++) {
            dp[i][0] = dp[i - 1][0] == -1 ? -1 : dp[i - 1][0] + dungeon[i][0];
            if (dp[i][0] <= 0)
                dp[i][0] = -1;
        }

        // Fill remaining cells
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                int fromUp = dp[i - 1][j];
                int fromLeft = dp[i][j - 1];

                if (fromUp == -1 && fromLeft == -1) {
                    dp[i][j] = -1;
                } else if (fromUp == -1) {
                    dp[i][j] = fromLeft + dungeon[i][j];
                } else if (fromLeft == -1) {
                    dp[i][j] = fromUp + dungeon[i][j];
                } else {
                    dp[i][j] = Math.max(fromUp, fromLeft) + dungeon[i][j];
                }

                if (dp[i][j] <= 0)
                    dp[i][j] = -1;
            }
        }

        return dp[m - 1][n - 1] > 0;
    }

    /**
     * APPROACH 10: FORWARD DP (INCORRECT BUT EDUCATIONAL)
     * This approach doesn't work correctly but shows why forward DP fails
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     */
    public int calculateMinimumHP_ForwardDP_Wrong(int[][] dungeon) {
        // This is intentionally wrong to demonstrate why forward DP doesn't work
        int m = dungeon.length, n = dungeon[0].length;
        int[][] dp = new int[m][n];

        dp[0][0] = dungeon[0][0] >= 0 ? 1 : 1 - dungeon[0][0];

        for (int i = 1; i < m; i++) {
            dp[i][0] = Math.max(1, dp[i - 1][0] - dungeon[i][0]);
        }

        for (int j = 1; j < n; j++) {
            dp[0][j] = Math.max(1, dp[0][j - 1] - dungeon[0][j]);
        }

        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = Math.max(1, Math.min(dp[i - 1][j], dp[i][j - 1]) - dungeon[i][j]);
            }
        }

        return dp[m - 1][n - 1];
        // This gives wrong answer because it doesn't consider future path requirements
    }

    /**
     * Test all approaches
     */
    public static void main(String[] args) {
        DungeonGameAllSolutions solution = new DungeonGameAllSolutions();

        // Test cases
        int[][] dungeon1 = { { -2, -3, 3 }, { -5, -10, 1 }, { 10, 30, -5 } };
        int[][] dungeon2 = { { 0 } };
        int[][] dungeon3 = { { 1, -3, 3 }, { 0, -2, 0 }, { -3, -3, -3 } };

        System.out.println("=== TESTING ALL APPROACHES ===\n");

        // Test each approach
        testApproach(solution, "Top-Down DP", dungeon1, dungeon2, dungeon3, 1);
        testApproach(solution, "Bottom-Up DP", dungeon1, dungeon2, dungeon3, 2);
        testApproach(solution, "Space Optimized", dungeon1, dungeon2, dungeon3, 3);
        testApproach(solution, "Two Arrays", dungeon1, dungeon2, dungeon3, 4);
        testApproach(solution, "In-Place DP", copyArray(dungeon1), copyArray(dungeon2), copyArray(dungeon3), 5);
        testApproach(solution, "Pure Recursive", dungeon1, dungeon2, dungeon3, 6);
        testApproach(solution, "Dijkstra", dungeon1, dungeon2, dungeon3, 7);
        testApproach(solution, "BFS", dungeon1, dungeon2, dungeon3, 8);
        testApproach(solution, "Binary Search", dungeon1, dungeon2, dungeon3, 9);
        testApproach(solution, "Forward DP (Wrong)", dungeon1, dungeon2, dungeon3, 10);
    }

    private static void testApproach(DungeonGameAllSolutions solution, String name,
            int[][] d1, int[][] d2, int[][] d3, int approach) {
        System.out.printf("%-20s: ", name);
        try {
            int r1 = 0, r2 = 0, r3 = 0;
            switch (approach) {
                case 1:
                    r1 = solution.calculateMinimumHP_TopDown(d1);
                    r2 = solution.calculateMinimumHP_TopDown(d2);
                    r3 = solution.calculateMinimumHP_TopDown(d3);
                    break;
                case 2:
                    r1 = solution.calculateMinimumHP_BottomUp(d1);
                    r2 = solution.calculateMinimumHP_BottomUp(d2);
                    r3 = solution.calculateMinimumHP_BottomUp(d3);
                    break;
                case 3:
                    r1 = solution.calculateMinimumHP_SpaceOptimized(d1);
                    r2 = solution.calculateMinimumHP_SpaceOptimized(d2);
                    r3 = solution.calculateMinimumHP_SpaceOptimized(d3);
                    break;
                case 4:
                    r1 = solution.calculateMinimumHP_TwoArrays(d1);
                    r2 = solution.calculateMinimumHP_TwoArrays(d2);
                    r3 = solution.calculateMinimumHP_TwoArrays(d3);
                    break;
                case 5:
                    r1 = solution.calculateMinimumHP_InPlace(d1);
                    r2 = solution.calculateMinimumHP_InPlace(d2);
                    r3 = solution.calculateMinimumHP_InPlace(d3);
                    break;
                case 6:
                    r1 = solution.calculateMinimumHP_Recursive(d1);
                    r2 = solution.calculateMinimumHP_Recursive(d2);
                    r3 = solution.calculateMinimumHP_Recursive(d3);
                    break;
                case 7:
                    r1 = solution.calculateMinimumHP_Dijkstra(d1);
                    r2 = solution.calculateMinimumHP_Dijkstra(d2);
                    r3 = solution.calculateMinimumHP_Dijkstra(d3);
                    break;
                case 8:
                    r1 = solution.calculateMinimumHP_BFS(d1);
                    r2 = solution.calculateMinimumHP_BFS(d2);
                    r3 = solution.calculateMinimumHP_BFS(d3);
                    break;
                case 9:
                    r1 = solution.calculateMinimumHP_BinarySearch(d1);
                    r2 = solution.calculateMinimumHP_BinarySearch(d2);
                    r3 = solution.calculateMinimumHP_BinarySearch(d3);
                    break;
                case 10:
                    r1 = solution.calculateMinimumHP_ForwardDP_Wrong(d1);
                    r2 = solution.calculateMinimumHP_ForwardDP_Wrong(d2);
                    r3 = solution.calculateMinimumHP_ForwardDP_Wrong(d3);
                    break;
            }
            System.out.printf("%d, %d, %d%s\n", r1, r2, r3,
                    approach == 10 ? " (WRONG - for educational purposes)" : "");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static int[][] copyArray(int[][] arr) {
        int[][] copy = new int[arr.length][arr[0].length];
        for (int i = 0; i < arr.length; i++) {
            System.arraycopy(arr[i], 0, copy[i], 0, arr[i].length);
        }
        return copy;
    }
}

/*
 * COMPLEXITY ANALYSIS SUMMARY:
 * 
 * 1. Top-Down DP (Memoization): Time: O(m*n), Space: O(m*n)
 * 2. Bottom-Up DP (Tabulation): Time: O(m*n), Space: O(m*n)
 * 3. Space Optimized DP: Time: O(m*n), Space: O(n)
 * 4. Two Arrays DP: Time: O(m*n), Space: O(n)
 * 5. In-Place DP: Time: O(m*n), Space: O(1)
 * 6. Pure Recursive: Time: O(2^(m+n)), Space: O(m+n)
 * 7. Dijkstra's Algorithm: Time: O(m*n*log(m*n)), Space: O(m*n)
 * 8. BFS with Priority Queue: Time: O(m*n*log(m*n)), Space: O(m*n)
 * 9. Binary Search + Validation: Time: O(log(H)*m*n), Space: O(m*n)
 * 10. Forward DP (Wrong): Time: O(m*n), Space: O(m*n)
 * 
 * RECOMMENDED APPROACHES:
 * - For interviews: Bottom-Up DP (#2) or Space Optimized (#3)
 * - For production: Space Optimized DP (#3) or In-Place DP (#5)
 * - For learning: Top-Down DP (#1) to understand the problem structure
 * 
 * WHY SOME APPROACHES DON'T WORK:
 * - Forward DP fails because it doesn't know future requirements
 * - Pure recursive is too slow due to overlapping subproblems
 * - Graph algorithms work but are overkill with worse time complexity
 * 
 * The key insight is that this problem requires working backwards from the
 * destination
 * because we need to know the minimum health required for the remaining path.
 */

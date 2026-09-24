import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/*
 * Given a triangle array, return the minimum path sum from top to bottom.
 * 
 * For each step, you may move to an adjacent number of the row below. More
 * formally, if you are on index i on the current row, you may move to either
 * index i or index i + 1 on the next row.
 * 
 * Example 1:
 * Input: triangle = [[2],[3,4],[6,5,7],[4,1,8,3]]
 * Output: 11
 * Explanation: The triangle looks like:
 * 2
 * 3 4
 * 6 5 7
 * 4 1 8 3
 * The minimum path sum from top to bottom is 2 + 3 + 5 + 1 = 11 (underlined
 * above).
 * 
 * Example 2:
 * Input: triangle = [[-10]]
 * Output: -10
 */

class MinimumPathSumInTriangularGrid {

    public int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();
        List<List<Integer>> dp = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dp.add(new ArrayList<>());
        }
        dp.add(n - 1, triangle.get(n - 1));
        for (int row = n - 2; row >= 0; row--) {
            List<Integer> currentRow = new ArrayList<>();
            for (int col = 0; col < triangle.get(row).size(); col++) {
                currentRow.add(
                        triangle.get(row).get(col) + Math.min(dp.get(row + 1).get(col), dp.get(row + 1).get(col + 1)));
            }
            dp.add(row, currentRow);
        }

        return dp.get(0).get(0);
    }

}

// Solution 1: Top-Down with Memoization (Recursive)
// Time: O(n^2), Space: O(n^2)
class Solution1_TopDownMemo {
    public int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();
        Integer[][] memo = new Integer[n][];

        // Initialize memo arrays with correct sizes
        for (int i = 0; i < n; i++) {
            memo[i] = new Integer[i + 1];
        }

        return dfs(triangle, 0, 0, memo);
    }

    private int dfs(List<List<Integer>> triangle, int row, int col, Integer[][] memo) {
        // Base case: reached bottom
        if (row == triangle.size() - 1) {
            return triangle.get(row).get(col);
        }

        // Check memo
        if (memo[row][col] != null) {
            return memo[row][col];
        }

        // Two choices: move to same index or index+1
        int down = dfs(triangle, row + 1, col, memo);
        int diagonal = dfs(triangle, row + 1, col + 1, memo);

        memo[row][col] = triangle.get(row).get(col) + Math.min(down, diagonal);
        return memo[row][col];
    }
}

// Solution 2: Bottom-Up Dynamic Programming (2D)
// Time: O(n^2), Space: O(n^2)
class Solution2_BottomUp2D {
    public int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();

        // Create DP table with same structure as triangle
        int[][] dp = new int[n][];
        for (int i = 0; i < n; i++) {
            dp[i] = new int[i + 1];
        }

        // Initialize bottom row
        for (int j = 0; j < triangle.get(n - 1).size(); j++) {
            dp[n - 1][j] = triangle.get(n - 1).get(j);
        }

        // Build up from bottom to top
        for (int i = n - 2; i >= 0; i--) {
            for (int j = 0; j < triangle.get(i).size(); j++) {
                dp[i][j] = triangle.get(i).get(j) + Math.min(dp[i + 1][j], dp[i + 1][j + 1]);
            }
        }

        return dp[0][0];
    }
}

// Solution 3: Space Optimized Bottom-Up (1D)
// Time: O(n^2), Space: O(n)
class Solution3_SpaceOptimized1D {
    public int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();

        // Use array of size n (max width of triangle)
        int[] dp = new int[n];

        // Initialize with bottom row
        List<Integer> lastRow = triangle.get(n - 1);
        for (int j = 0; j < lastRow.size(); j++) {
            dp[j] = lastRow.get(j);
        }

        // Build up from second-to-last row
        for (int i = n - 2; i >= 0; i--) {
            for (int j = 0; j < triangle.get(i).size(); j++) {
                dp[j] = triangle.get(i).get(j) + Math.min(dp[j], dp[j + 1]);
            }
        }

        return dp[0];
    }
}

// Solution 4: In-Place Modification
// Time: O(n^2), Space: O(1)
class Solution4_InPlace {
    public int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();

        // Modify triangle in-place, starting from second-to-last row
        for (int i = n - 2; i >= 0; i--) {
            for (int j = 0; j < triangle.get(i).size(); j++) {
                int current = triangle.get(i).get(j);
                int below = triangle.get(i + 1).get(j);
                int belowRight = triangle.get(i + 1).get(j + 1);
                triangle.get(i).set(j, current + Math.min(below, belowRight));
            }
        }

        return triangle.get(0).get(0);
    }
}

// Solution 5: Top-Down DP (building from top)
// Time: O(n^2), Space: O(n^2)
class Solution5_TopDownDP {
    public int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();

        // Create DP table
        int[][] dp = new int[n][];
        for (int i = 0; i < n; i++) {
            dp[i] = new int[i + 1];
            Arrays.fill(dp[i], Integer.MAX_VALUE);
        }

        // Initialize first element
        dp[0][0] = triangle.get(0).get(0);

        // Fill row by row
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < triangle.get(i).size(); j++) {
                if (dp[i][j] == Integer.MAX_VALUE)
                    continue;

                // Update next row
                dp[i + 1][j] = Math.min(dp[i + 1][j], dp[i][j] + triangle.get(i + 1).get(j));
                dp[i + 1][j + 1] = Math.min(dp[i + 1][j + 1], dp[i][j] + triangle.get(i + 1).get(j + 1));
            }
        }

        // Find minimum in last row
        int result = Integer.MAX_VALUE;
        for (int j = 0; j < triangle.get(n - 1).size(); j++) {
            result = Math.min(result, dp[n - 1][j]);
        }

        return result;
    }
}

// Solution 6: With Path Reconstruction
// Time: O(n^2), Space: O(n^2)
class Solution6_WithPath {
    public class Result {
        int minSum;
        List<Integer> path;

        Result(int sum, List<Integer> path) {
            this.minSum = sum;
            this.path = new ArrayList<>(path);
        }
    }

    public Result minimumTotalWithPath(List<List<Integer>> triangle) {
        int n = triangle.size();

        // DP table for minimum sums
        int[][] dp = new int[n][];
        // Table to track which path to take (0 = down, 1 = diagonal)
        int[][] choice = new int[n][];

        for (int i = 0; i < n; i++) {
            dp[i] = new int[i + 1];
            if (i < n - 1)
                choice[i] = new int[i + 1];
        }

        // Initialize bottom row
        for (int j = 0; j < triangle.get(n - 1).size(); j++) {
            dp[n - 1][j] = triangle.get(n - 1).get(j);
        }

        // Build up from bottom
        for (int i = n - 2; i >= 0; i--) {
            for (int j = 0; j < triangle.get(i).size(); j++) {
                int down = dp[i + 1][j];
                int diagonal = dp[i + 1][j + 1];

                if (down <= diagonal) {
                    dp[i][j] = triangle.get(i).get(j) + down;
                    choice[i][j] = 0; // go down
                } else {
                    dp[i][j] = triangle.get(i).get(j) + diagonal;
                    choice[i][j] = 1; // go diagonal
                }
            }
        }

        // Reconstruct path
        List<Integer> path = new ArrayList<>();
        int row = 0, col = 0;

        while (row < n) {
            path.add(triangle.get(row).get(col));
            if (row < n - 1) {
                col += choice[row][col];
            }
            row++;
        }

        return new Result(dp[0][0], path);
    }
}

// Solution 7: Alternative Top-Down with Different Memoization
// Time: O(n^2), Space: O(n^2)
class Solution7_AltMemo {
    private Map<String, Integer> memo;

    public int minimumTotal(List<List<Integer>> triangle) {
        memo = new HashMap<>();
        return dfs(triangle, 0, 0);
    }

    private int dfs(List<List<Integer>> triangle, int row, int col) {
        if (row == triangle.size() - 1) {
            return triangle.get(row).get(col);
        }

        String key = row + "," + col;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        int current = triangle.get(row).get(col);
        int result = current + Math.min(
                dfs(triangle, row + 1, col),
                dfs(triangle, row + 1, col + 1));

        memo.put(key, result);
        return result;
    }
}

// Solution 8: Using Min-Heap (Dijkstra-like approach)
// Time: O(n^2 * log(n^2)), Space: O(n^2)
class Solution8_MinHeap {
    private static class Node {
        int row, col, sum;

        Node(int row, int col, int sum) {
            this.row = row;
            this.col = col;
            this.sum = sum;
        }
    }

    public int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();

        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> a.sum - b.sum);
        Set<String> visited = new HashSet<>();

        pq.offer(new Node(0, 0, triangle.get(0).get(0)));

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            String key = current.row + "," + current.col;
            if (visited.contains(key))
                continue;
            visited.add(key);

            // If reached bottom row
            if (current.row == n - 1) {
                return current.sum;
            }

            // Add neighbors
            int nextRow = current.row + 1;

            // Down
            pq.offer(new Node(nextRow, current.col,
                    current.sum + triangle.get(nextRow).get(current.col)));

            // Diagonal
            pq.offer(new Node(nextRow, current.col + 1,
                    current.sum + triangle.get(nextRow).get(current.col + 1)));
        }

        return -1; // Should never reach here
    }
}

// Test class with comprehensive testing
class TestTriangleMinSum {
    public static void main(String[] args) {
        // Initialize all solutions
        Solution1_TopDownMemo sol1 = new Solution1_TopDownMemo();
        Solution2_BottomUp2D sol2 = new Solution2_BottomUp2D();
        Solution3_SpaceOptimized1D sol3 = new Solution3_SpaceOptimized1D();
        Solution4_InPlace sol4 = new Solution4_InPlace();
        Solution5_TopDownDP sol5 = new Solution5_TopDownDP();
        Solution6_WithPath sol6 = new Solution6_WithPath();
        Solution7_AltMemo sol7 = new Solution7_AltMemo();
        Solution8_MinHeap sol8 = new Solution8_MinHeap();

        // Test case 1: [[2],[3,4],[6,5,7],[4,1,8,3]]
        List<List<Integer>> triangle1 = Arrays.asList(
                Arrays.asList(2),
                Arrays.asList(3, 4),
                Arrays.asList(6, 5, 7),
                Arrays.asList(4, 1, 8, 3));

        System.out.println("Test 1 - Expected: 11");
        System.out.println("Top-Down Memo: " + sol1.minimumTotal(deepCopy(triangle1)));
        System.out.println("Bottom-Up 2D: " + sol2.minimumTotal(deepCopy(triangle1)));
        System.out.println("Space Optimized: " + sol3.minimumTotal(deepCopy(triangle1)));
        System.out.println("In-Place: " + sol4.minimumTotal(deepCopy(triangle1)));
        System.out.println("Top-Down DP: " + sol5.minimumTotal(deepCopy(triangle1)));
        System.out.println("Alt Memo: " + sol7.minimumTotal(deepCopy(triangle1)));
        System.out.println("Min-Heap: " + sol8.minimumTotal(deepCopy(triangle1)));

        // Show path
        Solution6_WithPath.Result result1 = sol6.minimumTotalWithPath(deepCopy(triangle1));
        System.out.println("Min Sum with Path: " + result1.minSum);
        System.out.println("Path: " + result1.path);

        // Test case 2: [[-10]]
        List<List<Integer>> triangle2 = Arrays.asList(Arrays.asList(-10));

        System.out.println("\nTest 2 - Expected: -10");
        System.out.println("Top-Down Memo: " + sol1.minimumTotal(deepCopy(triangle2)));
        System.out.println("Bottom-Up 2D: " + sol2.minimumTotal(deepCopy(triangle2)));

        // Test case 3: Small triangle
        List<List<Integer>> triangle3 = Arrays.asList(
                Arrays.asList(1),
                Arrays.asList(2, 3));

        System.out.println("\nTest 3 - Expected: 3");
        System.out.println("Space Optimized: " + sol3.minimumTotal(deepCopy(triangle3)));

        // Test case 4: All negative
        List<List<Integer>> triangle4 = Arrays.asList(
                Arrays.asList(-1),
                Arrays.asList(-2, -3),
                Arrays.asList(-4, -5, -6));

        System.out.println("\nTest 4 - Expected: -10 (all negative)");
        System.out.println("Bottom-Up 2D: " + sol2.minimumTotal(deepCopy(triangle4)));

        Solution6_WithPath.Result result4 = sol6.minimumTotalWithPath(deepCopy(triangle4));
        System.out.println("Path: " + result4.path);
    }

    private static List<List<Integer>> deepCopy(List<List<Integer>> original) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> row : original) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }

}
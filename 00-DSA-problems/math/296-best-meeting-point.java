import java.util.ArrayList;
import java.util.List;
/*
 * ## **296. Best Meeting Point**
 * 
 * A group of two or more people wants to meet and minimize the total travel
 * distance. You are given a 2D grid of values `0` or `1`, where each `1` marks
 * the home of someone in the group on a 2D grid.
 * 
 * Return the **minimum total travel distance**.
 ** 
 * Travel distance** is calculated using **Manhattan Distance**, where
 * `distance(p1, p2) = |p1.x - p2.x| + |p1.y - p2.y|`.
 * 
 * ---
 * 
 * ### 🧠 **Key Concepts**
 * 
 * You're allowed to choose **any point** on the grid (not necessarily a house)
 * as the meeting point.
 * The goal is to **minimize the sum of all distances** from that point to all
 * people’s houses.
 * Manhattan Distance makes it easier to solve by treating **rows and columns
 * separately**.
 * 
 * ---
 * 
 * ### 📘 **Example 1:**
 * 
 * ```
 * Input:
 * grid = [
 * [1, 0, 0, 0, 1],
 * [0, 0, 0, 0, 0],
 * [0, 0, 1, 0, 0]
 * ]
 * 
 * Output: 6
 * 
 * Explanation:
 * The point (0,2) is the best meeting point, with total travel distance =
 * |0 - 0| + |0 - 2| +
 * |0 - 4| + |0 - 2| +
 * |2 - 2| + |2 - 2| = 6
 * ```
 * ---
 * ### 📘 **Example 2:**
 * ```
 * Input:
 * grid = [
 * [1, 1]
 * ]
 * 
 * Output: 1
 * ```
 * ---
 * 
 * ### 🔒 Constraints:
 * 
 * `m == grid.length`
 * `n == grid[i].length`
 * `1 <= m, n <= 200`
 * `grid[i][j]` is either `0` or `1`.
 * There will be **at least two** people in the grid.
 */

/**
 * LeetCode 296: Best Meeting Point
 * 
 * Problem: Given a 2D grid where 1 represents people and 0 represents empty
 * spaces,
 * find the point that minimizes the total Manhattan distance for all people to
 * meet.
 * 
 * Manhattan Distance: |x1 - x2| + |y1 - y2|
 * 
 * Example:
 * Input: [[1,0,0,0,1],[0,0,0,0,0],[0,0,1,0,0]]
 * Output: 6
 * People at (0,0), (0,4), (2,2). Best meeting point is (0,2) with total
 * distance 6.
 */

class BestMeetingPoint {

    /**
     * Solution 1: Optimal O(mn) Solution using Median Property
     * 
     * Key Insight: The optimal meeting point is at the median of all x-coordinates
     * and median of all y-coordinates separately. This is because Manhattan
     * distance
     * can be decomposed into separate x and y components.
     * 
     * Time: O(mn) where m = rows, n = cols
     * Space: O(k) where k = number of people
     */
    public int minTotalDistance(int[][] grid) {
        if (grid == null || grid.length == 0)
            return 0;

        List<Integer> rows = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();

        // Collect all people positions
        // Rows are naturally sorted due to iteration order
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == 1) {
                    rows.add(i);
                }
            }
        }

        // Collect columns separately and sort them
        for (int j = 0; j < grid[0].length; j++) {
            for (int i = 0; i < grid.length; i++) {
                if (grid[i][j] == 1) {
                    cols.add(j);
                }
            }
        }

        // Find median positions
        int medianRow = rows.get(rows.size() / 2);
        int medianCol = cols.get(cols.size() / 2);

        // Calculate total distance
        return calculateDistance(rows, medianRow) + calculateDistance(cols, medianCol);
    }

    private int calculateDistance(List<Integer> positions, int median) {
        int distance = 0;
        for (int pos : positions) {
            distance += Math.abs(pos - median);
        }
        return distance;
    }

    /**
     * Solution 2: Alternative implementation with cleaner separation
     */
    public int minTotalDistance2(int[][] grid) {
        List<Integer> rows = collectRows(grid);
        List<Integer> cols = collectCols(grid);

        return minDistance1D(rows) + minDistance1D(cols);
    }

    private List<Integer> collectRows(int[][] grid) {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == 1) {
                    rows.add(i);
                }
            }
        }
        return rows;
    }

    private List<Integer> collectCols(int[][] grid) {
        List<Integer> cols = new ArrayList<>();
        for (int j = 0; j < grid[0].length; j++) {
            for (int i = 0; i < grid.length; i++) {
                if (grid[i][j] == 1) {
                    cols.add(j);
                }
            }
        }
        return cols;
    }

    private int minDistance1D(List<Integer> points) {
        int distance = 0;
        int i = 0, j = points.size() - 1;
        while (i < j) {
            distance += points.get(j--) - points.get(i++);
        }
        return distance;
    }

    /**
     * Solution 3: Brute Force (for understanding) - O(mn * k) where k = number of
     * people
     * This solution tries every possible meeting point and calculates total
     * distance.
     * Not optimal but helps understand the problem.
     */
    public int minTotalDistanceBruteForce(int[][] grid) {
        List<int[]> people = new ArrayList<>();

        // Collect all people positions
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == 1) {
                    people.add(new int[] { i, j });
                }
            }
        }

        int minDistance = Integer.MAX_VALUE;

        // Try every possible meeting point
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                int totalDistance = 0;
                for (int[] person : people) {
                    totalDistance += Math.abs(person[0] - i) + Math.abs(person[1] - j);
                }
                minDistance = Math.min(minDistance, totalDistance);
            }
        }

        return minDistance;
    }

    /**
     * Test cases and main method
     */
    public static void main(String[] args) {
        BestMeetingPoint solution = new BestMeetingPoint();

        // Test case 1
        int[][] grid1 = {
                { 1, 0, 0, 0, 1 },
                { 0, 0, 0, 0, 0 },
                { 0, 0, 1, 0, 0 }
        };
        System.out.println("Test 1 - Expected: 6, Got: " + solution.minTotalDistance(grid1));

        // Test case 2
        int[][] grid2 = {
                { 1, 1 }
        };
        System.out.println("Test 2 - Expected: 1, Got: " + solution.minTotalDistance(grid2));

        // Test case 3
        int[][] grid3 = {
                { 1 },
                { 0 },
                { 0 },
                { 0 },
                { 1 }
        };
        System.out.println("Test 3 - Expected: 4, Got: " + solution.minTotalDistance(grid3));
    }
}

/**
 * Mathematical Explanation:
 * 
 * The key insight is that Manhattan distance = |x1-x2| + |y1-y2| can be
 * optimized
 * separately for x and y coordinates.
 * 
 * For 1D case: Given points on a line, the point that minimizes total distance
 * to all points is the MEDIAN.
 * 
 * Proof: If we have points sorted as p1 ≤ p2 ≤ ... ≤ pn, and we choose meeting
 * point at position x:
 * - If x < median: moving x slightly right reduces distance to more points than
 * it increases
 * - If x > median: moving x left reduces distance to more points than it
 * increases
 * - If x = median: this is optimal
 * 
 * Time Complexity Analysis:
 * - Collecting positions: O(mn)
 * - Sorting is not needed for rows (naturally sorted by iteration)
 * - Sorting is not needed for cols (we iterate column by column)
 * - Calculating distance: O(k) where k = number of people
 * - Total: O(mn)
 * 
 * Space Complexity: O(k) for storing people positions
 */
/*
 * You are given an array of points in the X-Y plane points where points[i] =
 * [xi, yi].
 * 
 * Return the minimum area of a rectangle formed from these points, with sides
 * parallel to the X and Y axes. If there is not any such rectangle, return 0.
 * 
 * Example 1:
 * Input: points = [[1,1],[1,3],[3,1],[3,3],[2,2]]
 * Output: 4
 * 
 * Example 2:
 * Input: points = [[1,1],[1,3],[3,1],[3,3],[4,1],[4,3]]
 * Output: 2
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MinimumAreaRectangle {

    /**
     * Solution 1: Brute Force - Check all possible rectangles
     * Time Complexity: O(n^4) where n is the number of points
     * Space Complexity: O(1) - only using constant extra space
     * 
     * Algorithm:
     * - Try every combination of 4 points
     * - Check if they form a valid rectangle with sides parallel to axes
     * - Keep track of minimum area
     */
    public int minAreaRectBruteForce(int[][] points) {
        int n = points.length;
        if (n < 4)
            return 0;

        int minArea = Integer.MAX_VALUE;

        // Try all combinations of 4 points
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int k = j + 1; k < n; k++) {
                    for (int l = k + 1; l < n; l++) {
                        int area = getRectangleArea(points[i], points[j], points[k], points[l]);
                        if (area > 0) {
                            minArea = Math.min(minArea, area);
                        }
                    }
                }
            }
        }

        return minArea == Integer.MAX_VALUE ? 0 : minArea;
    }

    private int getRectangleArea(int[] p1, int[] p2, int[] p3, int[] p4) {
        // Collect all x and y coordinates
        Set<Integer> xCoords = new HashSet<>();
        Set<Integer> yCoords = new HashSet<>();

        xCoords.add(p1[0]);
        xCoords.add(p2[0]);
        xCoords.add(p3[0]);
        xCoords.add(p4[0]);
        yCoords.add(p1[1]);
        yCoords.add(p2[1]);
        yCoords.add(p3[1]);
        yCoords.add(p4[1]);

        // For a valid rectangle, we need exactly 2 unique x-coordinates and 2 unique
        // y-coordinates
        if (xCoords.size() != 2 || yCoords.size() != 2) {
            return -1;
        }

        Integer[] xArray = xCoords.toArray(new Integer[0]);
        Integer[] yArray = yCoords.toArray(new Integer[0]);

        int width = Math.abs(xArray[1] - xArray[0]);
        int height = Math.abs(yArray[1] - yArray[0]);

        return width * height;
    }

    /**
     * Solution 2: HashSet Optimization - For each pair of diagonal points
     * Time Complexity: O(n^2) where n is the number of points
     * Space Complexity: O(n) for the HashSet to store points
     * 
     * Algorithm:
     * - Store all points in a HashSet for O(1) lookup
     * - For each pair of points that could be diagonal corners
     * - Check if the other two corners exist in the set
     * - Calculate area and keep track of minimum
     */
    public int minAreaRectHashSet(int[][] points) {
        if (points.length < 4)
            return 0;

        Set<String> pointSet = new HashSet<>();
        for (int[] point : points) {
            pointSet.add(point[0] + "," + point[1]);
        }

        int minArea = Integer.MAX_VALUE;

        // Try all pairs of points as potential diagonal corners
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                int x1 = points[i][0], y1 = points[i][1];
                int x2 = points[j][0], y2 = points[j][1];

                // Skip if points are on same line (not diagonal)
                if (x1 == x2 || y1 == y2)
                    continue;

                // Check if other two corners exist
                if (pointSet.contains(x1 + "," + y2) && pointSet.contains(x2 + "," + y1)) {
                    int area = Math.abs(x2 - x1) * Math.abs(y2 - y1);
                    minArea = Math.min(minArea, area);
                }
            }
        }

        return minArea == Integer.MAX_VALUE ? 0 : minArea;
    }

    /**
     * Solution 3: Group by X-coordinate (Most Efficient)
     * Time Complexity: O(n^2) where n is the number of points
     * Space Complexity: O(n) for storing points grouped by x-coordinate
     * 
     * Algorithm:
     * - Group points by their x-coordinates
     * - For each pair of x-coordinates, try all pairs of points
     * - If two points have same y-coordinates, they can form a rectangle
     * - Calculate area and keep track of minimum
     */
    public int minAreaRectGroupByX(int[][] points) {
        Map<Integer, List<Integer>> xToYs = new HashMap<>();

        // Group points by x-coordinate
        for (int[] point : points) {
            xToYs.computeIfAbsent(point[0], k -> new ArrayList<>()).add(point[1]);
        }

        int minArea = Integer.MAX_VALUE;

        // Get all x-coordinates
        Integer[] xCoords = xToYs.keySet().toArray(new Integer[0]);

        // Try all pairs of x-coordinates
        for (int i = 0; i < xCoords.length; i++) {
            for (int j = i + 1; j < xCoords.length; j++) {
                List<Integer> ys1 = xToYs.get(xCoords[i]);
                List<Integer> ys2 = xToYs.get(xCoords[j]);

                // Find common y-coordinates between the two x-coordinates
                for (int y1 : ys1) {
                    for (int y2 : ys2) {
                        if (ys1.contains(y2) && ys2.contains(y1)) {
                            int area = Math.abs(xCoords[j] - xCoords[i]) * Math.abs(y2 - y1);
                            if (area > 0) {
                                minArea = Math.min(minArea, area);
                            }
                        }
                    }
                }
            }
        }

        return minArea == Integer.MAX_VALUE ? 0 : minArea;
    }

    /**
     * Solution 4: Optimized Group by X with Set intersection
     * Time Complexity: O(n^2) average case, O(n^3) worst case
     * Space Complexity: O(n) for storing points grouped by x-coordinate
     * 
     * This is the most practical solution combining efficiency and readability
     */
    public int minAreaRect(int[][] points) {
        Map<Integer, Set<Integer>> xToYs = new HashMap<>();

        // Group points by x-coordinate
        for (int[] point : points) {
            xToYs.computeIfAbsent(point[0], k -> new HashSet<>()).add(point[1]);
        }

        int minArea = Integer.MAX_VALUE;

        // Convert to array for easier iteration
        Integer[] xCoords = xToYs.keySet().toArray(new Integer[0]);

        // Try all pairs of x-coordinates
        for (int i = 0; i < xCoords.length; i++) {
            for (int j = i + 1; j < xCoords.length; j++) {
                Set<Integer> ys1 = xToYs.get(xCoords[i]);
                Set<Integer> ys2 = xToYs.get(xCoords[j]);

                // Find intersection of y-coordinates
                Set<Integer> commonYs = new HashSet<>(ys1);
                commonYs.retainAll(ys2);

                // Need at least 2 common y-coordinates to form a rectangle
                if (commonYs.size() >= 2) {
                    Integer[] yArray = commonYs.toArray(new Integer[0]);

                    // Try all pairs of common y-coordinates
                    for (int p = 0; p < yArray.length; p++) {
                        for (int q = p + 1; q < yArray.length; q++) {
                            int width = Math.abs(xCoords[j] - xCoords[i]);
                            int height = Math.abs(yArray[q] - yArray[p]);
                            int area = width * height;
                            minArea = Math.min(minArea, area);
                        }
                    }
                }
            }
        }

        return minArea == Integer.MAX_VALUE ? 0 : minArea;
    }

    // Test method
    public static void main(String[] args) {
        MinimumAreaRectangle solution = new MinimumAreaRectangle();

        // Test case 1
        int[][] points1 = { { 1, 1 }, { 1, 3 }, { 3, 1 }, { 3, 3 }, { 2, 2 } };
        System.out.println("Test 1 - Expected: 4");
        System.out.println("Brute Force: " + solution.minAreaRectBruteForce(points1));
        System.out.println("HashSet: " + solution.minAreaRectHashSet(points1));
        System.out.println("Group by X: " + solution.minAreaRect(points1));

        // Test case 2
        int[][] points2 = { { 1, 1 }, { 1, 3 }, { 3, 1 }, { 3, 3 }, { 4, 1 }, { 4, 3 } };
        System.out.println("\nTest 2 - Expected: 2");
        System.out.println("Brute Force: " + solution.minAreaRectBruteForce(points2));
        System.out.println("HashSet: " + solution.minAreaRectHashSet(points2));
        System.out.println("Group by X: " + solution.minAreaRect(points2));

        // Test case 3 - No rectangle possible
        int[][] points3 = { { 1, 1 }, { 1, 2 }, { 2, 1 } };
        System.out.println("\nTest 3 - Expected: 0");
        System.out.println("Brute Force: " + solution.minAreaRectBruteForce(points3));
        System.out.println("HashSet: " + solution.minAreaRectHashSet(points3));
        System.out.println("Group by X: " + solution.minAreaRect(points3));
    }
}

/*
 * COMPLEXITY ANALYSIS SUMMARY:
 * 
 * 1. Brute Force Solution:
 * - Time: O(n^4) - tries all combinations of 4 points
 * - Space: O(1) - constant extra space
 * - Pros: Simple to understand and implement
 * - Cons: Very slow for large inputs
 * 
 * 2. HashSet Diagonal Solution:
 * - Time: O(n^2) - tries all pairs as diagonal corners
 * - Space: O(n) - stores all points in HashSet
 * - Pros: Much faster than brute force, intuitive approach
 * - Cons: Still quadratic time complexity
 * 
 * 3. Group by X-coordinate:
 * - Time: O(n^2) average, O(n^3) worst case
 * - Space: O(n) - stores points grouped by x-coordinate
 * - Pros: Often faster in practice, good for sparse data
 * - Cons: Worst case can be cubic if many points share x-coordinates
 * 
 * 4. Optimized Group by X (Recommended):
 * - Time: O(n^2) average case
 * - Space: O(n) - stores points grouped by x-coordinate
 * - Pros: Best balance of efficiency and readability
 * - Cons: Implementation is slightly more complex
 * 
 * For most practical purposes, Solution 4 (minAreaRect) is recommended as it
 * provides
 * the best balance of time complexity, space efficiency, and code
 * maintainability.
 */

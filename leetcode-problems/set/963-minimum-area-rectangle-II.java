/*
 * You are given an array of points in the X-Y plane points where points[i] =
 * [xi, yi].
 * 
 * Return the minimum area of any rectangle formed from these points, with sides
 * not necessarily parallel to the X and Y axes. If there is not any such
 * rectangle, return 0.
 * 
 * Answers within 10-5 of the actual answer will be accepted.
 * 
 * Example 1:
 * Input: points = [[1,2],[2,1],[1,0],[0,1]]
 * Output: 2.00000
 * Explanation: The minimum area rectangle occurs at [1,2],[2,1],[1,0],[0,1],
 * with an area of 2.
 * 
 * Example 2:
 * Input: points = [[0,1],[2,1],[1,1],[1,0],[2,0]]
 * Output: 1.00000
 * Explanation: The minimum area rectangle occurs at [1,0],[1,1],[2,1],[2,0],
 * with an area of 1.
 * 
 * Example 3:
 * Input: points = [[0,3],[1,2],[3,1],[1,3],[2,1]]
 * Output: 0
 * Explanation: There is no possible rectangle to form from these points.
 * 
 * Constraints:
 * 1 <= points.length <= 50
 * points[i].length == 2
 * 0 <= xi, yi <= 4 * 104
 * All the given points are unique.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MinimumAreaRectangleAnyOrientation {

    private static final double EPS = 1e-9;

    /**
     * Solution 1: Brute Force - Check all combinations of 4 points
     * Time Complexity: O(n^4) where n is the number of points
     * Space Complexity: O(1) - only using constant extra space
     * 
     * Algorithm:
     * - Try every combination of 4 points
     * - Check if they form a valid rectangle (opposite sides equal, all angles 90°)
     * - Keep track of minimum area
     */
    public double minAreaFreeRectBruteForce(int[][] points) {
        int n = points.length;
        if (n < 4)
            return 0.0;

        double minArea = Double.MAX_VALUE;

        // Try all combinations of 4 points
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int k = j + 1; k < n; k++) {
                    for (int l = k + 1; l < n; l++) {
                        double area = getRectangleArea(points[i], points[j], points[k], points[l]);
                        if (area > EPS) {
                            minArea = Math.min(minArea, area);
                        }
                    }
                }
            }
        }

        return minArea == Double.MAX_VALUE ? 0.0 : minArea;
    }

    /**
     * Check if 4 points form a rectangle and return its area
     * A rectangle has:
     * 1. Four right angles (90 degrees)
     * 2. Opposite sides are equal
     * 3. Diagonals are equal
     */
    private double getRectangleArea(int[] p1, int[] p2, int[] p3, int[] p4) {
        int[][] points = { p1, p2, p3, p4 };

        // Calculate all 6 distances between the 4 points
        double[] distances = new double[6];
        int idx = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                distances[idx++] = getDistance(points[i], points[j]);
            }
        }

        // Sort distances
        Arrays.sort(distances);

        // For a rectangle, we should have:
        // - 4 equal sides (2 pairs of equal opposite sides)
        // - 2 equal diagonals (longer than sides)
        // After sorting: [side1, side1, side2, side2, diagonal, diagonal]

        // Check if we have 2 pairs of equal sides and 1 pair of equal diagonals
        if (Math.abs(distances[0] - distances[1]) < EPS &&
                Math.abs(distances[2] - distances[3]) < EPS &&
                Math.abs(distances[4] - distances[5]) < EPS &&
                distances[0] > EPS && distances[2] > EPS) {

            // Verify it's actually a rectangle by checking if diagonals are longer
            if (distances[4] > distances[3] + EPS) {
                // Additional check: verify right angles using dot product
                if (isRectangle(points)) {
                    return distances[0] * distances[2]; // area = length * width
                }
            }
        }

        return 0.0;
    }

    /**
     * Check if 4 points form a rectangle by verifying right angles
     */
    private boolean isRectangle(int[][] points) {
        // Find the center point (average of all points)
        double centerX = 0, centerY = 0;
        for (int[] point : points) {
            centerX += point[0];
            centerY += point[1];
        }
        centerX /= 4;
        centerY /= 4;

        // Calculate distances from center to each point
        double[] distFromCenter = new double[4];
        for (int i = 0; i < 4; i++) {
            distFromCenter[i] = Math.sqrt(Math.pow(points[i][0] - centerX, 2) +
                    Math.pow(points[i][1] - centerY, 2));
        }

        // In a rectangle, all vertices are equidistant from the center
        for (int i = 1; i < 4; i++) {
            if (Math.abs(distFromCenter[i] - distFromCenter[0]) > EPS) {
                return false;
            }
        }

        return true;
    }

    /**
     * Solution 2: Optimized approach using center point and rotation
     * Time Complexity: O(n^3) where n is the number of points
     * Space Complexity: O(n) for storing points in sets
     * 
     * Algorithm:
     * - For each pair of points, consider them as diagonal of rectangle
     * - Calculate the other two vertices that would complete the rectangle
     * - Check if those vertices exist in the point set
     * - Calculate area and keep track of minimum
     */
    public double minAreaFreeRect(int[][] points) {
        int n = points.length;
        if (n < 4)
            return 0.0;

        // Store points in a set for O(1) lookup
        Set<String> pointSet = new HashSet<>();
        for (int[] point : points) {
            pointSet.add(point[0] + "," + point[1]);
        }

        double minArea = Double.MAX_VALUE;

        // Try all pairs of points as potential diagonal
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Try all other points as third vertex
                for (int k = 0; k < n; k++) {
                    if (k == i || k == j)
                        continue;

                    int[] p1 = points[i];
                    int[] p2 = points[j];
                    int[] p3 = points[k];

                    // Calculate the fourth point that would make p1, p3, p2, p4 a rectangle
                    // If p1 and p2 are diagonal, and p3 is third vertex,
                    // then p4 = p1 + p2 - p3 (vector addition)
                    int[] p4 = { p1[0] + p2[0] - p3[0], p1[1] + p2[1] - p3[1] };

                    // Check if p4 exists in our point set
                    if (pointSet.contains(p4[0] + "," + p4[1])) {
                        // Verify that p1, p3, p2, p4 actually form a rectangle
                        if (isValidRectangle(p1, p3, p2, p4)) {
                            double area = calculateRectangleArea(p1, p3, p2, p4);
                            if (area > EPS) {
                                minArea = Math.min(minArea, area);
                            }
                        }
                    }
                }
            }
        }

        return minArea == Double.MAX_VALUE ? 0.0 : minArea;
    }

    /**
     * Verify that 4 points form a rectangle by checking:
     * 1. Opposite sides are parallel and equal
     * 2. Adjacent sides are perpendicular
     */
    private boolean isValidRectangle(int[] p1, int[] p2, int[] p3, int[] p4) {
        // Check if adjacent sides are perpendicular
        // Vector from p1 to p2
        double v1x = p2[0] - p1[0];
        double v1y = p2[1] - p1[1];

        // Vector from p1 to p4
        double v2x = p4[0] - p1[0];
        double v2y = p4[1] - p1[1];

        // Dot product should be 0 for perpendicular vectors
        double dotProduct = v1x * v2x + v1y * v2y;
        return Math.abs(dotProduct) < EPS;
    }

    /**
     * Calculate area of rectangle using cross product
     */
    private double calculateRectangleArea(int[] p1, int[] p2, int[] p3, int[] p4) {
        // Two adjacent sides from p1
        double side1x = p2[0] - p1[0];
        double side1y = p2[1] - p1[1];
        double side2x = p4[0] - p1[0];
        double side2y = p4[1] - p1[1];

        // Length of sides
        double len1 = Math.sqrt(side1x * side1x + side1y * side1y);
        double len2 = Math.sqrt(side2x * side2x + side2y * side2y);

        return len1 * len2;
    }

    /**
     * Solution 3: Most efficient approach using complex numbers concept
     * Time Complexity: O(n^2 * log(n)) where n is the number of points
     * Space Complexity: O(n^2) for storing point pairs
     * 
     * Algorithm:
     * - For each pair of points, calculate the center and "complex rotation"
     * - Group pairs that could be opposite corners of the same rectangle
     * - For each group, calculate the minimum area
     */
    public double minAreaFreeRectOptimal(int[][] points) {
        int n = points.length;
        if (n < 4)
            return 0.0;

        double minArea = Double.MAX_VALUE;

        // Map from (center, rotation) to list of point pairs
        Map<String, List<int[][]>> centerRotationMap = new HashMap<>();

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int[] p1 = points[i];
                int[] p2 = points[j];

                // Center of the two points
                double centerX = (p1[0] + p2[0]) / 2.0;
                double centerY = (p1[1] + p2[1]) / 2.0;

                // Distance from center (half diagonal length)
                double dist = Math.sqrt(Math.pow(p2[0] - p1[0], 2) + Math.pow(p2[1] - p1[1], 2)) / 2.0;

                // Rotation angle (using atan2 for proper quadrant)
                double angle = Math.atan2(p2[1] - p1[1], p2[0] - p1[0]);

                // Create a key for grouping
                String key = String.format("%.9f,%.9f,%.9f", centerX, centerY, dist);

                centerRotationMap.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[][] { p1, p2 });
            }
        }

        // For each group of point pairs with same center and distance
        for (List<int[][]> pairs : centerRotationMap.values()) {
            if (pairs.size() >= 2) {
                // Try all combinations of pairs in this group
                for (int i = 0; i < pairs.size(); i++) {
                    for (int j = i + 1; j < pairs.size(); j++) {
                        int[][] pair1 = pairs.get(i);
                        int[][] pair2 = pairs.get(j);

                        // Check if these 4 points form a rectangle
                        double area = getRectangleAreaFromPairs(pair1[0], pair1[1], pair2[0], pair2[1]);
                        if (area > EPS) {
                            minArea = Math.min(minArea, area);
                        }
                    }
                }
            }
        }

        return minArea == Double.MAX_VALUE ? 0.0 : minArea;
    }

    private double getRectangleAreaFromPairs(int[] p1, int[] p2, int[] p3, int[] p4) {
        // Verify these 4 points form a rectangle
        if (isValidRectangle(p1, p3, p2, p4)) {
            return calculateRectangleArea(p1, p3, p2, p4);
        }
        if (isValidRectangle(p1, p4, p2, p3)) {
            return calculateRectangleArea(p1, p4, p2, p3);
        }
        return 0.0;
    }

    // Helper method to calculate distance between two points
    private double getDistance(int[] p1, int[] p2) {
        return Math.sqrt(Math.pow(p2[0] - p1[0], 2) + Math.pow(p2[1] - p1[1], 2));
    }

    // Test method
    public static void main(String[] args) {
        MinimumAreaRectangleAnyOrientation solution = new MinimumAreaRectangleAnyOrientation();

        // Test case 1
        int[][] points1 = { { 1, 2 }, { 2, 1 }, { 1, 0 }, { 0, 1 } };
        System.out.println("Test 1 - Expected: 2.0");
        System.out.println("Brute Force: " + solution.minAreaFreeRectBruteForce(points1));
        System.out.println("Optimized: " + solution.minAreaFreeRect(points1));
        System.out.println("Most Optimal: " + solution.minAreaFreeRectOptimal(points1));

        // Test case 2
        int[][] points2 = { { 0, 1 }, { 2, 1 }, { 1, 1 }, { 1, 0 }, { 2, 0 } };
        System.out.println("\nTest 2 - Expected: 1.0");
        System.out.println("Brute Force: " + solution.minAreaFreeRectBruteForce(points2));
        System.out.println("Optimized: " + solution.minAreaFreeRect(points2));
        System.out.println("Most Optimal: " + solution.minAreaFreeRectOptimal(points2));

        // Test case 3 - No rectangle possible
        int[][] points3 = { { 0, 3 }, { 1, 2 }, { 3, 1 }, { 1, 3 }, { 2, 1 } };
        System.out.println("\nTest 3 - Expected: 0.0");
        System.out.println("Brute Force: " + solution.minAreaFreeRectBruteForce(points3));
        System.out.println("Optimized: " + solution.minAreaFreeRect(points3));
        System.out.println("Most Optimal: " + solution.minAreaFreeRectOptimal(points3));

        // Test case 4 - Simple axis-aligned rectangle
        int[][] points4 = { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 1, 0 } };
        System.out.println("\nTest 4 - Expected: 1.0");
        System.out.println("Brute Force: " + solution.minAreaFreeRectBruteForce(points4));
        System.out.println("Optimized: " + solution.minAreaFreeRect(points4));
        System.out.println("Most Optimal: " + solution.minAreaFreeRectOptimal(points4));
    }
}

/*
 * COMPLEXITY ANALYSIS SUMMARY:
 * 
 * 1. Brute Force Solution (minAreaFreeRectBruteForce):
 * - Time: O(n^4) - tries all combinations of 4 points
 * - Space: O(1) - constant extra space
 * - Pros: Simple to understand, guaranteed to find all rectangles
 * - Cons: Very slow for large inputs, but acceptable given constraint n <= 50
 * 
 * 2. Optimized Diagonal Approach (minAreaFreeRect):
 * - Time: O(n^3) - for each pair as diagonal, try all third points
 * - Space: O(n) - stores all points in HashSet for O(1) lookup
 * - Pros: Faster than brute force, more intuitive approach
 * - Cons: Still cubic complexity, but much better than O(n^4)
 * 
 * 3. Center-Distance Grouping (minAreaFreeRectOptimal):
 * - Time: O(n^2 * log(n)) - O(n^2) pairs, O(log n) for map operations
 * - Space: O(n^2) - stores all point pairs grouped by center and distance
 * - Pros: Most efficient for larger inputs, mathematically elegant
 * - Cons: More complex implementation, uses more memory
 * 
 * KEY INSIGHTS:
 * 
 * Rectangle Properties (any orientation):
 * - All angles are 90 degrees (adjacent sides perpendicular)
 * - Opposite sides are equal and parallel
 * - Diagonals are equal in length
 * - All vertices are equidistant from the center
 * 
 * Mathematical Approach:
 * - Two points can be diagonal corners if we can find two other points
 * - The center of diagonal corners is the center of the rectangle
 * - Distance from center to any vertex is half the diagonal length
 * 
 * RECOMMENDED SOLUTION:
 * Given the constraint n <= 50, the optimized O(n^3) solution (minAreaFreeRect)
 * provides the best balance of:
 * - Efficiency (much better than O(n^4))
 * - Code clarity and maintainability
 * - Reliability in finding correct results
 * 
 * The O(n^2 log n) solution is theoretically better but adds complexity that
 * may not be necessary for the given constraints.
 */

class LargestRotatedRectangle {
    static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Pair {
        Point p1, p2;

        Pair(Point p1, Point p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    public static double largestRectangleArea(int[][] pointsArr) {
        int n = pointsArr.length;
        Point[] points = new Point[n];
        for (int i = 0; i < n; i++) {
            points[i] = new Point(pointsArr[i][0], pointsArr[i][1]);
        }

        // Map from (midpoint_x, midpoint_y, diagonal_length) -> list of pairs
        Map<String, List<Pair>> map = new HashMap<>();

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Point p1 = points[i];
                Point p2 = points[j];
                double midX = (p1.x + p2.x) / 2.0;
                double midY = (p1.y + p2.y) / 2.0;
                int dx = p1.x - p2.x;
                int dy = p1.y - p2.y;
                int distSq = dx * dx + dy * dy;

                String key = midX + "," + midY + "," + distSq;
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(new Pair(p1, p2));
            }
        }

        double maxArea = 0;

        // For each group with same midpoint and diagonal length
        for (List<Pair> pairs : map.values()) {
            int size = pairs.size();
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    Point p1 = pairs.get(i).p1;
                    Point p2 = pairs.get(i).p2;
                    Point p3 = pairs.get(j).p1;
                    Point p4 = pairs.get(j).p2;

                    // Check if p1-p3 and p1-p4 are adjacent sides with right angle
                    double area = rectangleAreaIfValid(p1, p3, p4);
                    if (area > maxArea) {
                        maxArea = area;
                    }
                }
            }
        }
        return maxArea;
    }

    // Returns area if points form rectangle p1,p3,p4 (p1 is common vertex)
    private static double rectangleAreaIfValid(Point p1, Point p2, Point p3) {
        // vectors
        int vx1 = p2.x - p1.x;
        int vy1 = p2.y - p1.y;
        int vx2 = p3.x - p1.x;
        int vy2 = p3.y - p1.y;

        // dot product == 0 means right angle
        int dot = vx1 * vx2 + vy1 * vy2;
        if (dot != 0)
            return 0;

        double side1 = Math.sqrt(vx1 * vx1 + vy1 * vy1);
        double side2 = Math.sqrt(vx2 * vx2 + vy2 * vy2);

        return side1 * side2;
    }

    public static void main(String[] args) {
        int[][] points = {
                { 1, 2 }, { 3, 4 }, { 5, 2 }, { 3, 0 }, { 0, 0 }, { 6, 0 }
        };
        System.out.println("Largest Rotated Rectangle Area: " + largestRectangleArea(points));
    }

}

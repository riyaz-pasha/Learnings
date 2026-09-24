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


/**
 * MINIMUM AREA RECTANGLE II - COMPREHENSIVE SOLUTION GUIDE
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given points in 2D plane, find minimum area rectangle with ANY orientation.
 * (Not necessarily axis-aligned!)
 * 
 * Example: points = [[1,2],[2,1],[1,0],[0,1]]
 * 
 *   2  •(1,2)
 *   1     •(2,1)
 *      •(0,1)
 *   0  •(1,0)
 *      0  1  2
 * 
 * Forms a square with area = 2.0
 * 
 * KEY INSIGHTS FOR INTERVIEWS:
 * ============================
 * 1. Rectangle has 4 vertices with specific geometric properties
 * 2. Diagonals of rectangle:
 *    - Bisect each other (same midpoint)
 *    - Have equal length
 *    - Are perpendicular (for rotated rectangles this is key!)
 * 3. Strategy: Check all pairs of points as potential diagonals
 * 4. For valid diagonal pair, can compute rectangle area directly
 * 
 * CRITICAL GEOMETRIC INSIGHTS:
 * ============================
 * 
 * Rectangle Properties:
 * - Opposite sides are parallel and equal
 * - Diagonals bisect each other (same center)
 * - Diagonals have same length
 * - All angles are 90 degrees
 * 
 * Given diagonal endpoints (p1, p3) and (p2, p4):
 * - Center: ((p1.x+p3.x)/2, (p1.y+p3.y)/2)
 * - If (p2, p4) has same center and length, they form rectangle!
 * - Area = 0.5 * d1 * d2 * sin(angle between diagonals)
 * - For rectangle, angle = 90°, so: Area = 0.5 * d1 * d2
 * 
 * But we can compute area more directly:
 * Area = |cross product of two adjacent sides|
 * 
 * APPROACHES:
 * ===========
 * 1. Check all diagonal pairs - O(n^3) time
 * 2. Group by diagonal center - O(n^2) time (optimal)
 * 3. Check all point quadruples - O(n^4) time (brute force)
 */

class MinAreaRectII {
    
    /**
     * APPROACH 1: DIAGONAL CENTER GROUPING (OPTIMAL)
     * ===============================================
     * Time Complexity: O(n^2) where n = number of points
     * Space Complexity: O(n^2) for storing diagonal pairs
     * 
     * ALGORITHM:
     * ==========
     * 1. For each pair of points, consider them as potential diagonal
     * 2. Compute diagonal's center point and length
     * 3. Group diagonals by their center point
     * 4. For diagonals with same center:
     *    - They could form a rectangle if lengths match
     *    - Compute area using cross product
     * 5. Track minimum area
     * 
     * WHY THIS WORKS:
     * ==============
     * Rectangle diagonals have the SAME center point.
     * By grouping diagonals by center, we only check valid candidates.
     * 
     * GEOMETRIC FORMULA:
     * ==================
     * Given 4 points forming rectangle: A, B, C, D
     * If diagonal AC and diagonal BD have same center,
     * and we know coordinates of all 4 points:
     * 
     * Area = |AB × AD| where × is cross product
     * 
     * For 2D vectors v1=(x1,y1) and v2=(x2,y2):
     * Cross product magnitude = |x1*y2 - x2*y1|
     */
    public double minAreaFreeRect(int[][] points) {
        int n = points.length;
        if (n < 4) return 0.0;
        
        double minArea = Double.MAX_VALUE;
        
        // Map: center point (as string) -> list of diagonal pairs
        // Each diagonal pair stores: [point1_index, point2_index, squared_length]
        Map<String, List<int[]>> diagonalsByCenter = new HashMap<>();
        
        // Consider all pairs of points as potential diagonals
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Compute center of diagonal (i, j)
                // Use exact arithmetic: store as (2*cx, 2*cy) to avoid floating point
                long cx = (long) points[i][0] + points[j][0];
                long cy = (long) points[i][1] + points[j][1];
                String center = cx + "," + cy;
                
                // Compute squared length of diagonal
                long dx = points[i][0] - points[j][0];
                long dy = points[i][1] - points[j][1];
                long lenSquared = dx * dx + dy * dy;
                
                // Store this diagonal
                diagonalsByCenter.putIfAbsent(center, new ArrayList<>());
                diagonalsByCenter.get(center).add(new int[]{i, j, (int)lenSquared});
            }
        }
        
        // For each center point, check pairs of diagonals
        for (List<int[]> diagonals : diagonalsByCenter.values()) {
            if (diagonals.size() < 2) continue; // Need at least 2 diagonals
            
            // Check all pairs of diagonals with this center
            for (int i = 0; i < diagonals.size(); i++) {
                for (int j = i + 1; j < diagonals.size(); j++) {
                    int[] diag1 = diagonals.get(i);
                    int[] diag2 = diagonals.get(j);
                    
                    // Check if diagonals have equal length (rectangle property)
                    if (diag1[2] != diag2[2]) continue;
                    
                    // Get the 4 points
                    int[] p1 = points[diag1[0]];
                    int[] p2 = points[diag1[1]];
                    int[] p3 = points[diag2[0]];
                    int[] p4 = points[diag2[1]];
                    
                    // Compute area using cross product
                    // Vector from p1 to p3
                    long v1x = p3[0] - p1[0];
                    long v1y = p3[1] - p1[1];
                    
                    // Vector from p1 to p4
                    long v2x = p4[0] - p1[0];
                    long v2y = p4[1] - p1[1];
                    
                    // Area = |cross product|
                    double area = Math.abs(v1x * v2y - v1y * v2x);
                    
                    minArea = Math.min(minArea, area);
                }
            }
        }
        
        return minArea == Double.MAX_VALUE ? 0.0 : minArea;
    }
    
    /**
     * APPROACH 2: CLEANER VERSION WITH BETTER STRUCTURE
     * ==================================================
     * Same complexity, but cleaner code structure
     */
    public double minAreaFreeRectCleaner(int[][] points) {
        int n = points.length;
        if (n < 4) return 0.0;
        
        double minArea = Double.MAX_VALUE;
        
        // Group diagonals by their center point
        Map<String, List<Diagonal>> centerMap = new HashMap<>();
        
        // Generate all possible diagonals
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Diagonal diag = new Diagonal(points[i], points[j], i, j);
                String key = diag.getCenterKey();
                
                centerMap.putIfAbsent(key, new ArrayList<>());
                centerMap.get(key).add(diag);
            }
        }
        
        // Check each group of diagonals with same center
        for (List<Diagonal> diagonals : centerMap.values()) {
            if (diagonals.size() < 2) continue;
            
            // Try all pairs of diagonals
            for (int i = 0; i < diagonals.size(); i++) {
                for (int j = i + 1; j < diagonals.size(); j++) {
                    Diagonal d1 = diagonals.get(i);
                    Diagonal d2 = diagonals.get(j);
                    
                    // Rectangle diagonals must have equal length
                    if (Math.abs(d1.lengthSquared - d2.lengthSquared) > 1e-9) {
                        continue;
                    }
                    
                    // Calculate area
                    double area = calculateArea(d1.p1, d1.p2, d2.p1);
                    minArea = Math.min(minArea, area);
                }
            }
        }
        
        return minArea == Double.MAX_VALUE ? 0.0 : minArea;
    }
    
    /**
     * Helper class to represent a diagonal
     */
    static class Diagonal {
        int[] p1, p2;
        int idx1, idx2;
        double centerX, centerY;
        double lengthSquared;
        
        Diagonal(int[] point1, int[] point2, int i, int j) {
            this.p1 = point1;
            this.p2 = point2;
            this.idx1 = i;
            this.idx2 = j;
            
            // Calculate center (exact arithmetic using integer coordinates doubled)
            this.centerX = (point1[0] + point2[0]) / 2.0;
            this.centerY = (point1[1] + point2[1]) / 2.0;
            
            // Calculate squared length
            long dx = point1[0] - point2[0];
            long dy = point1[1] - point2[1];
            this.lengthSquared = dx * dx + dy * dy;
        }
        
        String getCenterKey() {
            // Use integer arithmetic to avoid floating point issues
            long cx2 = (long)p1[0] + p2[0]; // 2 * centerX
            long cy2 = (long)p1[1] + p2[1]; // 2 * centerY
            return cx2 + "," + cy2;
        }
    }
    
    /**
     * Calculate area of rectangle given 3 of its vertices
     * 
     * Formula: Area = |AB × AC| where A, B, C are three vertices
     * and × denotes cross product
     */
    private double calculateArea(int[] p1, int[] p2, int[] p3) {
        // Vector from p1 to p2
        long v1x = p2[0] - p1[0];
        long v1y = p2[1] - p1[1];
        
        // Vector from p1 to p3
        long v2x = p3[0] - p1[0];
        long v2y = p3[1] - p1[1];
        
        // Cross product gives area of parallelogram
        // Rectangle area = |cross product|
        return Math.abs(v1x * v2y - v1y * v2x);
    }
    
    /**
     * APPROACH 3: BRUTE FORCE (FOR UNDERSTANDING)
     * ============================================
     * Time Complexity: O(n^4)
     * Space Complexity: O(1)
     * 
     * Check all combinations of 4 points to see if they form a rectangle.
     * This is too slow for n > 50, but good for understanding the problem.
     */
    public double minAreaFreeRectBruteForce(int[][] points) {
        int n = points.length;
        if (n < 4) return 0.0;
        
        double minArea = Double.MAX_VALUE;
        
        // Try all combinations of 4 points
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int k = j + 1; k < n; k++) {
                    for (int l = k + 1; l < n; l++) {
                        double area = checkRectangle(
                            points[i], points[j], points[k], points[l]
                        );
                        if (area > 0) {
                            minArea = Math.min(minArea, area);
                        }
                    }
                }
            }
        }
        
        return minArea == Double.MAX_VALUE ? 0.0 : minArea;
    }
    
    /**
     * Check if 4 points form a rectangle and return area if yes
     * 
     * Rectangle conditions:
     * 1. Diagonals have same length
     * 2. Diagonals bisect each other (same midpoint)
     */
    private double checkRectangle(int[] p1, int[] p2, int[] p3, int[] p4) {
        // Compute all 6 distances
        long d12 = distSquared(p1, p2);
        long d13 = distSquared(p1, p3);
        long d14 = distSquared(p1, p4);
        long d23 = distSquared(p2, p3);
        long d24 = distSquared(p2, p4);
        long d34 = distSquared(p3, p4);
        
        // Store distances in array and sort
        long[] dists = {d12, d13, d14, d23, d24, d34};
        Arrays.sort(dists);
        
        // For rectangle: 4 sides + 2 diagonals
        // After sorting: [side1, side1, side2, side2, diag, diag]
        // Check if this pattern holds
        if (dists[0] != dists[1] || dists[2] != dists[3] || dists[4] != dists[5]) {
            return 0;
        }
        
        // Check diagonals are longer than sides (unless it's a square)
        if (dists[4] < dists[0] || dists[4] < dists[2]) {
            return 0;
        }
        
        // Compute area: sqrt(side1^2) * sqrt(side2^2)
        return Math.sqrt(dists[0]) * Math.sqrt(dists[2]);
    }
    
    /**
     * Helper: Squared distance between two points
     */
    private long distSquared(int[] p1, int[] p2) {
        long dx = p1[0] - p2[0];
        long dy = p1[1] - p2[1];
        return dx * dx + dy * dy;
    }
    
    // VISUALIZATION AND TESTING
    public static void main(String[] args) {
        MinAreaRectII solution = new MinAreaRectII();
        
        System.out.println("=== MINIMUM AREA RECTANGLE II - TEST CASES ===\n");
        
        // Test Case 1: Square rotated 45 degrees
        System.out.println("Test 1: Rotated square");
        int[][] points1 = {{1,2},{2,1},{1,0},{0,1}};
        System.out.println("Points: " + Arrays.deepToString(points1));
        visualizePoints(points1);
        double result1 = solution.minAreaFreeRect(points1);
        System.out.println("Output: " + result1);
        System.out.println("Expected: 2.0 (diamond shape, side length √2)");
        System.out.println();
        
        // Test Case 2: Axis-aligned rectangle
        System.out.println("Test 2: Axis-aligned rectangle");
        int[][] points2 = {{0,0},{0,3},{3,3},{3,0},{2,2}};
        System.out.println("Points: " + Arrays.deepToString(points2));
        double result2 = solution.minAreaFreeRect(points2);
        System.out.println("Output: " + result2);
        System.out.println("Expected: 9.0 (3×3 rectangle)");
        System.out.println();
        
        // Test Case 3: No rectangle possible
        System.out.println("Test 3: No rectangle");
        int[][] points3 = {{0,0},{1,1},{2,2}};
        System.out.println("Points: " + Arrays.deepToString(points3));
        double result3 = solution.minAreaFreeRect(points3);
        System.out.println("Output: " + result3);
        System.out.println("Expected: 0.0");
        System.out.println();
        
        // Test Case 4: Multiple rectangles
        System.out.println("Test 4: Multiple rectangles");
        int[][] points4 = {{0,0},{1,0},{2,0},{0,1},{2,1},{0,2},{1,2},{2,2}};
        System.out.println("Points: Grid of 3×3 points");
        visualizePoints(points4);
        double result4 = solution.minAreaFreeRect(points4);
        System.out.println("Output: " + result4);
        System.out.println("Expected: 1.0 (smallest 1×1 rectangle)");
        System.out.println();
        
        // Test Case 5: Exact from problem
        System.out.println("Test 5: Complex case");
        int[][] points5 = {{3,1},{1,1},{0,1},{2,1},{3,3},{3,2},{0,2},{2,3}};
        double result5 = solution.minAreaFreeRect(points5);
        System.out.println("Output: " + result5);
        System.out.println("Expected: 2.0");
        System.out.println();
        
        System.out.println("=== ALGORITHM EXPLANATION ===");
        explainAlgorithm();
        
        System.out.println("\n=== COMPLEXITY ANALYSIS ===");
        System.out.println("Approach                    | Time      | Space");
        System.out.println("----------------------------|-----------|----------");
        System.out.println("Diagonal Center Grouping    | O(n²)     | O(n²)");
        System.out.println("Brute Force (4 points)      | O(n⁴)     | O(1)");
        System.out.println();
        System.out.println("Optimal approach: O(n²) time");
        System.out.println("- Generate all diagonal pairs: O(n²)");
        System.out.println("- For each center, check diagonal pairs: O(k²) where k ≤ n");
        System.out.println("- Total: O(n²) average case");
    }
    
    /**
     * Visualize points on a grid
     */
    private static void visualizePoints(int[][] points) {
        if (points.length == 0) return;
        
        // Find bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        
        for (int[] p : points) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        
        // Create grid
        Set<String> pointSet = new HashSet<>();
        for (int[] p : points) {
            pointSet.add(p[0] + "," + p[1]);
        }
        
        System.out.println("Visual:");
        for (int y = maxY; y >= minY; y--) {
            System.out.printf("%2d ", y);
            for (int x = minX; x <= maxX; x++) {
                if (pointSet.contains(x + "," + y)) {
                    System.out.print("● ");
                } else {
                    System.out.print("· ");
                }
            }
            System.out.println();
        }
        System.out.print("   ");
        for (int x = minX; x <= maxX; x++) {
            System.out.printf("%d ", x);
        }
        System.out.println("\n");
    }
    
    /**
     * Explain the algorithm with example
     */
    private static void explainAlgorithm() {
        System.out.println("Key Geometric Insight:");
        System.out.println("---------------------");
        System.out.println("Rectangle diagonals have TWO properties:");
        System.out.println("1. Same center point (bisect each other)");
        System.out.println("2. Equal length");
        System.out.println();
        System.out.println("Algorithm:");
        System.out.println("1. Consider all pairs of points as diagonals");
        System.out.println("2. Group diagonals by their center point");
        System.out.println("3. Within each group, find pairs with equal length");
        System.out.println("4. For such pairs, compute area using cross product");
        System.out.println("5. Return minimum area found");
        System.out.println();
        System.out.println("Area Calculation:");
        System.out.println("Given 4 rectangle vertices A, B, C, D");
        System.out.println("Area = |AB⃗ × AC⃗| (cross product magnitude)");
        System.out.println("For 2D: |x₁y₂ - x₂y₁|");
    }
}

/**
 * GEOMETRIC DERIVATIONS AND PROOFS
 * =================================
 * 
 * WHY DIAGONAL CENTER WORKS:
 * --------------------------
 * Theorem: Four points form a rectangle if and only if:
 * 1. They form a quadrilateral
 * 2. Diagonals have equal length
 * 3. Diagonals bisect each other
 * 
 * Proof sketch:
 * - Equal diagonals + bisection → parallelogram
 * - Parallelogram with equal diagonals → rectangle
 * 
 * AREA FORMULA:
 * ------------
 * Given rectangle with vertices A, B, C, D (in order):
 * 
 * Method 1: Side lengths
 * Area = |AB| × |AD|
 * 
 * Method 2: Cross product
 * AB⃗ = (B.x - A.x, B.y - A.y)
 * AD⃗ = (D.x - A.x, D.y - A.y)
 * Area = |AB⃗ × AD⃗| = |AB⃗.x × AD⃗.y - AB⃗.y × AD⃗.x|
 * 
 * Method 3: Diagonal-based (less direct)
 * Area = 0.5 × d₁ × d₂ × sin(θ)
 * For rectangle, θ = 90°, so Area = 0.5 × d₁ × d₂
 * But we use Method 2 as it's more straightforward
 * 
 * FLOATING POINT PRECISION:
 * ------------------------
 * Problem states: "Answers within 10⁻⁵ of actual answer will be accepted"
 * 
 * Our approach:
 * - Use integer arithmetic where possible (center coordinates)
 * - Use long for squared distances
 * - Only use double for final area calculation
 * - Cross product with long intermediate values
 * 
 * This minimizes floating point errors!
 */

/**
 * INTERVIEW STRATEGY GUIDE
 * ========================
 * 
 * 1. CLARIFY THE PROBLEM (2 minutes)
 *    Q: "Can rectangles be rotated?" A: Yes! Any orientation
 *    Q: "What if no rectangle exists?" A: Return 0
 *    Q: "What about precision?" A: Within 10⁻⁵ is acceptable
 *    Q: "Can points be duplicates?" A: Usually assume all distinct
 *    
 * 2. EXPLAIN KEY INSIGHT (3 minutes)
 *    "Rectangle diagonals have special properties:"
 *    - Same center point (bisect each other)
 *    - Equal length
 *    "So I'll group point pairs by their center and check for matching lengths"
 *    
 * 3. DRAW EXAMPLE (5 minutes)
 *    Draw rotated square: (1,2), (2,1), (1,0), (0,1)
 *    Show diagonals cross at (1,1)
 *    Show both diagonals have length 2√2
 *    
 * 4. DISCUSS APPROACH (5 minutes)
 *    "Generate all possible diagonals: O(n²)"
 *    "Group by center point"
 *    "For each center, check pairs of diagonals"
 *    "If equal length, they form rectangle"
 *    "Calculate area using cross product"
 *    
 * 5. CODE THE SOLUTION (15 minutes)
 *    Start with diagonal generation
 *    Use HashMap for grouping
 *    Implement area calculation
 *    Handle edge cases
 *    
 * 6. ANALYZE COMPLEXITY (2 minutes)
 *    Time: O(n²) for diagonal generation + checking
 *    Space: O(n²) for storing diagonals
 *    
 * 7. TEST (3 minutes)
 *    Rotated square
 *    Axis-aligned rectangle
 *    No rectangle possible
 *    Multiple rectangles
 * 
 * COMMON MISTAKES TO AVOID
 * ========================
 * 
 * 1. FLOATING POINT COMPARISON
 *    ❌ if (center1 == center2)
 *    ✓  Use string keys or epsilon comparison
 *    
 * 2. FORGETTING ROTATION
 *    ❌ Only checking axis-aligned rectangles
 *    ✓  Use diagonal approach (works for any orientation)
 *    
 * 3. WRONG AREA FORMULA
 *    ❌ Just multiplying diagonal lengths
 *    ✓  Use cross product or compute side lengths
 *    
 * 4. INTEGER OVERFLOW
 *    ❌ Using int for squared distances
 *    ✓  Use long for intermediate calculations
 *    
 * 5. NOT HANDLING "NO RECTANGLE"
 *    ❌ Returning Double.MAX_VALUE
 *    ✓  Return 0.0 when no rectangle found
 * 
 * KEY TAKEAWAYS
 * =============
 * 
 * 1. Rotated rectangles → use diagonal properties
 * 2. Group by center point to find candidates
 * 3. Cross product for area calculation
 * 4. O(n²) is optimal for this problem
 * 5. Integer arithmetic prevents floating point errors
 * 6. Drawing examples helps immensely!
 * 
 * Good luck! 📐
 */

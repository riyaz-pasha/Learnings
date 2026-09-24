import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

class MaxPointsOnLine {

    // Solution 1: Slope-based approach with HashMap - OPTIMAL
    // Time: O(n²), Space: O(n)
    public int maxPointsOnALine1(int[][] points) {
        if (points.length <= 2)
            return points.length;

        int maxPoints = 0;

        // For each point as the base point
        for (int i = 0; i < points.length; i++) {
            Map<String, Integer> slopeMap = new HashMap<>();
            int duplicates = 1; // Count the base point itself
            int localMax = 0;

            // Compare with all other points
            for (int j = i + 1; j < points.length; j++) {
                int dx = points[j][0] - points[i][0];
                int dy = points[j][1] - points[i][1];

                // Handle duplicate points
                if (dx == 0 && dy == 0) {
                    duplicates++;
                    continue;
                }

                // Calculate slope in reduced form
                String slope = getSlope(dx, dy);
                slopeMap.put(slope, slopeMap.getOrDefault(slope, 0) + 1);
                localMax = Math.max(localMax, slopeMap.get(slope));
            }

            maxPoints = Math.max(maxPoints, localMax + duplicates);
        }

        return maxPoints;
    }

    // Helper method to get slope in reduced form
    private String getSlope(int dx, int dy) {
        if (dx == 0)
            return "inf"; // Vertical line
        if (dy == 0)
            return "0"; // Horizontal line

        // Reduce fraction to lowest terms
        int gcd = gcd(Math.abs(dx), Math.abs(dy));
        dx /= gcd;
        dy /= gcd;

        // Normalize sign (put negative sign in numerator)
        if (dx < 0) {
            dx = -dx;
            dy = -dy;
        }

        return dy + "/" + dx;
    }

    // Greatest Common Divisor
    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    // Solution 2: Using rational number representation
    // Time: O(n²), Space: O(n)
    public int maxPointsOnALine2(int[][] points) {
        if (points.length <= 2)
            return points.length;

        int maxPoints = 0;

        for (int i = 0; i < points.length; i++) {
            Map<Slope, Integer> slopeCount = new HashMap<>();
            int duplicates = 1;
            int localMax = 0;

            for (int j = i + 1; j < points.length; j++) {
                int dx = points[j][0] - points[i][0];
                int dy = points[j][1] - points[i][1];

                if (dx == 0 && dy == 0) {
                    duplicates++;
                    continue;
                }

                Slope slope = new Slope(dx, dy);
                slopeCount.put(slope, slopeCount.getOrDefault(slope, 0) + 1);
                localMax = Math.max(localMax, slopeCount.get(slope));
            }

            maxPoints = Math.max(maxPoints, localMax + duplicates);
        }

        return maxPoints;
    }

    // Custom class to represent slope as a fraction
    static class Slope {
        int numerator, denominator;

        public Slope(int dx, int dy) {
            if (dx == 0) {
                numerator = 1;
                denominator = 0; // Vertical line
            } else if (dy == 0) {
                numerator = 0;
                denominator = 1; // Horizontal line
            } else {
                int gcd = gcd(Math.abs(dx), Math.abs(dy));
                numerator = dy / gcd;
                denominator = dx / gcd;

                // Normalize sign
                if (denominator < 0) {
                    numerator = -numerator;
                    denominator = -denominator;
                }
            }
        }

        private int gcd(int a, int b) {
            return b == 0 ? a : gcd(b, a % b);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Slope))
                return false;
            Slope other = (Slope) obj;
            return numerator == other.numerator && denominator == other.denominator;
        }

        @Override
        public int hashCode() {
            return Objects.hash(numerator, denominator);
        }

        @Override
        public String toString() {
            if (denominator == 0)
                return "∞";
            if (numerator == 0)
                return "0";
            return numerator + "/" + denominator;
        }
    }

    // Solution 3: Cross product approach (avoiding division)
    // Time: O(n³), Space: O(1)
    public int maxPointsOnALine3(int[][] points) {
        if (points.length <= 2)
            return points.length;

        int maxPoints = 2;

        // Try every pair of points to define a line
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                int count = 2; // Count the two points that define the line

                // Check all other points
                for (int k = 0; k < points.length; k++) {
                    if (k == i || k == j)
                        continue;

                    // Check if point k is collinear with points i and j
                    if (isCollinear(points[i], points[j], points[k])) {
                        count++;
                    }
                }

                maxPoints = Math.max(maxPoints, count);
            }
        }

        return maxPoints;
    }

    // Check if three points are collinear using cross product
    private boolean isCollinear(int[] p1, int[] p2, int[] p3) {
        // Cross product: (p2-p1) × (p3-p1) = 0 for collinear points
        int dx1 = p2[0] - p1[0];
        int dy1 = p2[1] - p1[1];
        int dx2 = p3[0] - p1[0];
        int dy2 = p3[1] - p1[1];

        return dx1 * dy2 == dy1 * dx2;
    }

    // Solution 4: Using long to avoid overflow
    // Time: O(n²), Space: O(n)
    public int maxPointsOnALine4(int[][] points) {
        if (points.length <= 2)
            return points.length;

        int maxPoints = 0;

        for (int i = 0; i < points.length; i++) {
            Map<String, Integer> slopeMap = new HashMap<>();
            int duplicates = 1;
            int localMax = 0;

            for (int j = i + 1; j < points.length; j++) {
                long dx = (long) points[j][0] - points[i][0];
                long dy = (long) points[j][1] - points[i][1];

                if (dx == 0 && dy == 0) {
                    duplicates++;
                    continue;
                }

                String slope = getSlopeLong(dx, dy);
                slopeMap.put(slope, slopeMap.getOrDefault(slope, 0) + 1);
                localMax = Math.max(localMax, slopeMap.get(slope));
            }

            maxPoints = Math.max(maxPoints, localMax + duplicates);
        }

        return maxPoints;
    }

    private String getSlopeLong(long dx, long dy) {
        if (dx == 0)
            return "inf";
        if (dy == 0)
            return "0";

        long gcd = gcdLong(Math.abs(dx), Math.abs(dy));
        dx /= gcd;
        dy /= gcd;

        if (dx < 0) {
            dx = -dx;
            dy = -dy;
        }

        return dy + "/" + dx;
    }

    private long gcdLong(long a, long b) {
        return b == 0 ? a : gcdLong(b, a % b);
    }

    // Demo method to visualize the algorithm
    public void demonstrateAlgorithm(int[][] points) {
        System.out.println("Demonstrating Max Points on Line Algorithm");
        System.out.println("Points: " + Arrays.deepToString(points));
        System.out.println("=".repeat(50));

        if (points.length <= 2) {
            System.out.println("Less than 3 points, return " + points.length);
            return;
        }

        int maxPoints = 0;

        for (int i = 0; i < Math.min(points.length, 3); i++) { // Limit demo to first 3 points
            System.out.println("\nBase point " + i + ": " + Arrays.toString(points[i]));
            Map<String, Integer> slopeMap = new HashMap<>();
            int duplicates = 1;

            for (int j = i + 1; j < points.length; j++) {
                int dx = points[j][0] - points[i][0];
                int dy = points[j][1] - points[i][1];

                System.out.printf("  Point %d: %s, dx=%d, dy=%d",
                        j, Arrays.toString(points[j]), dx, dy);

                if (dx == 0 && dy == 0) {
                    duplicates++;
                    System.out.println(" -> Duplicate point");
                    continue;
                }

                String slope = getSlope(dx, dy);
                slopeMap.put(slope, slopeMap.getOrDefault(slope, 0) + 1);
                System.out.println(" -> Slope: " + slope +
                        ", Count: " + slopeMap.get(slope));
            }

            int localMax = slopeMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            int totalPoints = localMax + duplicates;
            maxPoints = Math.max(maxPoints, totalPoints);

            System.out.println("  Slope counts: " + slopeMap);
            System.out.println("  Max for this base: " + totalPoints +
                    " (localMax: " + localMax + " + duplicates: " + duplicates + ")");
        }

        System.out.println("\nOverall maximum: " + maxPoints);
    }

    // Performance testing
    public void performanceTest(int[][] points) {
        System.out.println("\nPerformance Test (" + points.length + " points):");
        System.out.println("=".repeat(40));

        long start, end;

        // Test Solution 1 (Slope HashMap)
        start = System.nanoTime();
        int result1 = maxPointsOnALine1(points);
        end = System.nanoTime();
        System.out.printf("Solution 1 (Slope HashMap): %d, Time: %.2f ms\n",
                result1, (end - start) / 1_000_000.0);

        // Test Solution 2 (Slope Object)
        start = System.nanoTime();
        int result2 = maxPointsOnALine2(points);
        end = System.nanoTime();
        System.out.printf("Solution 2 (Slope Object): %d, Time: %.2f ms\n",
                result2, (end - start) / 1_000_000.0);

        // Test Solution 3 (Cross Product) - only for smaller inputs
        if (points.length <= 20) {
            start = System.nanoTime();
            int result3 = maxPointsOnALine3(points);
            end = System.nanoTime();
            System.out.printf("Solution 3 (Cross Product): %d, Time: %.2f ms\n",
                    result3, (end - start) / 1_000_000.0);
        } else {
            System.out.println("Solution 3 (Cross Product): Skipped for large input");
        }

        // Test Solution 4 (Long arithmetic)
        start = System.nanoTime();
        int result4 = maxPointsOnALine4(points);
        end = System.nanoTime();
        System.out.printf("Solution 4 (Long arithmetic): %d, Time: %.2f ms\n",
                result4, (end - start) / 1_000_000.0);
    }

    public static void main(String[] args) {
        MaxPointsOnLine solution = new MaxPointsOnLine();

        // Test cases
        int[][][] testCases = {
                { { 1, 1 }, { 2, 2 }, { 3, 3 } }, // Simple collinear
                { { 1, 1 }, { 3, 2 }, { 5, 3 }, { 4, 1 }, { 2, 3 }, { 1, 4 } }, // Mixed points
                { { 0, 0 }, { 1, 1 }, { 0, 0 } }, // Duplicate points
                { { 1, 1 } }, // Single point
                { { 1, 1 }, { 2, 2 } }, // Two points
                { { 0, 0 }, { 1, 0 }, { 2, 0 }, { 0, 1 }, { 0, 2 } }, // Cross pattern
                { { 1, 1 }, { 1, 1 }, { 1, 1 } }, // All duplicates
                { { 0, 0 }, { 1, 1 }, { 2, 2 }, { 3, 3 }, { 1, 0 }, { 2, 0 } }, // Two lines
                { { 84, 250 }, { 0, 0 }, { 1, 0 }, { 0, -70 }, { 0, -70 }, { 1, -1 }, { 21, 10 }, { 42, 90 },
                        { -42, -230 } } // Complex case
        };

        System.out.println("Max Points on Line - Test Results");
        System.out.println("==================================");

        for (int i = 0; i < testCases.length; i++) {
            int[][] points = testCases[i];
            System.out.println("\nTest Case " + (i + 1) + ": " + Arrays.deepToString(points));

            int result1 = solution.maxPointsOnALine1(points);
            int result2 = solution.maxPointsOnALine2(points);
            int result4 = solution.maxPointsOnALine4(points);

            System.out.println("Solution 1 (HashMap): " + result1);
            System.out.println("Solution 2 (Object): " + result2);
            System.out.println("Solution 4 (Long): " + result4);

            if (points.length <= 10) {
                int result3 = solution.maxPointsOnALine3(points);
                System.out.println("Solution 3 (Cross Product): " + result3);
            }

            boolean allMatch = (result1 == result2) && (result2 == result4);
            System.out.println("Results match: " + allMatch);
        }

        // Demonstrate algorithm
        int[][] demoPoints = { { 1, 1 }, { 2, 2 }, { 3, 3 }, { 1, 0 }, { 2, 0 } };
        solution.demonstrateAlgorithm(demoPoints);

        // Performance test
        int[][] largeTest = generateRandomPoints(50);
        solution.performanceTest(largeTest);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("KEY INSIGHTS:");
        System.out.println("1. Use slope to identify collinear points");
        System.out.println("2. Reduce fractions to avoid duplicate slopes");
        System.out.println("3. Handle vertical lines (infinite slope) specially");
        System.out.println("4. Count duplicate points correctly");
        System.out.println("5. For each point, find max points on any line through it");
    }

    // Helper method to generate random test points
    private static int[][] generateRandomPoints(int n) {
        Random rand = new Random(42); // Fixed seed for reproducibility
        int[][] points = new int[n][2];
        for (int i = 0; i < n; i++) {
            points[i][0] = rand.nextInt(100) - 50;
            points[i][1] = rand.nextInt(100) - 50;
        }
        return points;
    }
}

/*
 * Algorithm Analysis:
 * 
 * CORE INSIGHT:
 * - Two points determine a unique line
 * - Multiple points are collinear if they have the same slope relative to a
 * base point
 * - Use each point as a base and count points with same slope
 * 
 * KEY CHALLENGES:
 * 1. Slope representation: Use reduced fractions to avoid floating-point errors
 * 2. Vertical lines: Handle infinite slope specially
 * 3. Duplicate points: Count them correctly for each base point
 * 4. Integer overflow: Use long for large coordinates
 * 
 * ALGORITHM STEPS:
 * 1. For each point i as base point:
 * a. Initialize slope map and duplicate counter
 * b. For each other point j:
 * - Calculate slope from i to j
 * - Handle duplicates and vertical lines
 * - Count points with same slope
 * c. Update global maximum
 * 
 * TIME COMPLEXITY: O(n²) where n is number of points
 * SPACE COMPLEXITY: O(n) for the slope map
 * 
 * SLOPE CALCULATION:
 * - Slope = (y2-y1)/(x2-x1) = dy/dx
 * - Reduce fraction: dy/gcd(dx,dy) : dx/gcd(dx,dy)
 * - Normalize sign: keep negative in numerator
 * 
 * EDGE CASES:
 * 1. n ≤ 2: Return n (all points are collinear)
 * 2. Duplicate points: Count with base point
 * 3. Vertical line: dx = 0, use special marker
 * 4. Horizontal line: dy = 0, slope = 0
 * 5. All points same: Return n
 * 
 * RECOMMENDED SOLUTION: Solution 1 (maxPointsOnALine1)
 * - Optimal O(n²) time complexity
 * - Uses string representation for slopes (simple and effective)
 * - Handles all edge cases correctly
 * - Most interview-friendly approach
 * 
 * The key insight is that we don't need to check every triplet of points
 * (O(n³)),
 * but instead use the mathematical property that collinear points share the
 * same slope
 * from any base point, reducing complexity to O(n²).
 */
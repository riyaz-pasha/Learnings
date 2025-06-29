import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
/*
 * Given an array rectangles where rectangles[i] = [xi, yi, ai, bi] represents
 * an axis-aligned rectangle. The bottom-left point of the rectangle is (xi, yi)
 * and the top-right point of it is (ai, bi).
 * 
 * Return true if all the rectangles together form an exact cover of a
 * rectangular region.
 * 
 * Example 1:
 * Input: rectangles = [[1,1,3,3],[3,1,4,2],[3,2,4,4],[1,3,2,4],[2,3,3,4]]
 * Output: true
 * Explanation: All 5 rectangles together form an exact cover of a rectangular
 * region.
 * 
 * Example 2:
 * Input: rectangles = [[1,1,2,3],[1,3,2,4],[3,1,4,2],[3,2,4,4]]
 * Output: false
 * Explanation: Because there is a gap between the two rectangular regions.
 * 
 * Example 3:
 * Input: rectangles = [[1,1,3,3],[3,1,4,2],[1,3,2,4],[2,2,4,4]]
 * Output: false
 * Explanation: Because two of the rectangles overlap with each other.
 */

class PerfectRectangleSolutions {

    /**
     * Solution 1: Corner Point Counting (Most Efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * Key insight: In a perfect rectangle, each interior point appears in exactly 2
     * or 4 rectangles,
     * while corner points of the large rectangle appear in exactly 1 rectangle.
     */
    public boolean isRectangleCover1(int[][] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            return false;

        // Find the bounds of the large rectangle
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int totalArea = 0;

        // Set to store corner points
        Set<String> cornerPoints = new HashSet<>();

        for (int[] rect : rectangles) {
            int x1 = rect[0], y1 = rect[1], x2 = rect[2], y2 = rect[3];

            // Update bounds
            minX = Math.min(minX, x1);
            minY = Math.min(minY, y1);
            maxX = Math.max(maxX, x2);
            maxY = Math.max(maxY, y2);

            // Add area
            totalArea += (x2 - x1) * (y2 - y1);

            // Add/remove corner points
            String[] corners = {
                    x1 + "," + y1, // bottom-left
                    x1 + "," + y2, // top-left
                    x2 + "," + y1, // bottom-right
                    x2 + "," + y2 // top-right
            };

            for (String corner : corners) {
                if (cornerPoints.contains(corner)) {
                    cornerPoints.remove(corner);
                } else {
                    cornerPoints.add(corner);
                }
            }
        }

        // Check if total area matches expected area
        int expectedArea = (maxX - minX) * (maxY - minY);
        if (totalArea != expectedArea)
            return false;

        // Check if only 4 corner points remain (corners of the large rectangle)
        if (cornerPoints.size() != 4)
            return false;

        // Verify the remaining points are exactly the corners of the large rectangle
        Set<String> expectedCorners = new HashSet<>(Arrays.asList(
                minX + "," + minY,
                minX + "," + maxY,
                maxX + "," + minY,
                maxX + "," + maxY));

        return cornerPoints.equals(expectedCorners);
    }

    /**
     * Solution 2: Sweep Line Algorithm
     * Time Complexity: O(n²)
     * Space Complexity: O(n)
     * 
     * Uses vertical sweep line to check for gaps and overlaps.
     */
    public boolean isRectangleCover2(int[][] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            return false;

        // Find bounds and calculate total area
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int totalArea = 0;

        for (int[] rect : rectangles) {
            minX = Math.min(minX, rect[0]);
            minY = Math.min(minY, rect[1]);
            maxX = Math.max(maxX, rect[2]);
            maxY = Math.max(maxY, rect[3]);
            totalArea += (rect[2] - rect[0]) * (rect[3] - rect[1]);
        }

        // Check total area
        if (totalArea != (maxX - minX) * (maxY - minY))
            return false;

        // Get all vertical lines (x-coordinates)
        Set<Integer> xCoords = new TreeSet<>();
        for (int[] rect : rectangles) {
            xCoords.add(rect[0]);
            xCoords.add(rect[2]);
        }

        List<Integer> sortedX = new ArrayList<>(xCoords);

        // Check each vertical strip
        for (int i = 0; i < sortedX.size() - 1; i++) {
            int x1 = sortedX.get(i);
            int x2 = sortedX.get(i + 1);

            // Get all y-intervals for this x-range
            List<int[]> intervals = new ArrayList<>();
            for (int[] rect : rectangles) {
                if (rect[0] <= x1 && x2 <= rect[2]) {
                    intervals.add(new int[] { rect[1], rect[3] });
                }
            }

            // Check if intervals cover [minY, maxY] exactly
            if (!coversExactly(intervals, minY, maxY)) {
                return false;
            }
        }

        return true;
    }

    private boolean coversExactly(List<int[]> intervals, int start, int end) {
        if (intervals.isEmpty())
            return false;

        // Sort intervals by start point
        intervals.sort((a, b) -> a[0] - b[0]);

        int current = start;
        int i = 0;

        while (current < end && i < intervals.size()) {
            if (intervals.get(i)[0] > current) {
                return false; // gap found
            }

            int maxEnd = intervals.get(i)[1];

            // Find all intervals that start at or before current
            while (i < intervals.size() && intervals.get(i)[0] <= current) {
                maxEnd = Math.max(maxEnd, intervals.get(i)[1]);
                i++;
            }

            current = maxEnd;
        }

        return current == end;
    }

    /**
     * Solution 3: Grid-based Approach
     * Time Complexity: O(n * m) where m is number of unique coordinates
     * Space Complexity: O(m²)
     * 
     * Creates a grid and marks covered cells. Less efficient but intuitive.
     */
    public boolean isRectangleCover3(int[][] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            return false;

        // Get all unique coordinates
        Set<Integer> xCoords = new TreeSet<>();
        Set<Integer> yCoords = new TreeSet<>();

        for (int[] rect : rectangles) {
            xCoords.add(rect[0]);
            xCoords.add(rect[2]);
            yCoords.add(rect[1]);
            yCoords.add(rect[3]);
        }

        List<Integer> xs = new ArrayList<>(xCoords);
        List<Integer> ys = new ArrayList<>(yCoords);

        // Create coordinate mapping
        Map<Integer, Integer> xMap = new HashMap<>();
        Map<Integer, Integer> yMap = new HashMap<>();

        for (int i = 0; i < xs.size(); i++) {
            xMap.put(xs.get(i), i);
        }
        for (int i = 0; i < ys.size(); i++) {
            yMap.put(ys.get(i), i);
        }

        // Create grid
        boolean[][] grid = new boolean[xs.size() - 1][ys.size() - 1];

        // Mark rectangles on grid
        for (int[] rect : rectangles) {
            int x1 = xMap.get(rect[0]);
            int y1 = yMap.get(rect[1]);
            int x2 = xMap.get(rect[2]);
            int y2 = yMap.get(rect[3]);

            // Check for overlap and mark cells
            for (int i = x1; i < x2; i++) {
                for (int j = y1; j < y2; j++) {
                    if (grid[i][j]) {
                        return false; // overlap detected
                    }
                    grid[i][j] = true;
                }
            }
        }

        // Check if all cells are covered
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (!grid[i][j]) {
                    return false; // gap found
                }
            }
        }

        return true;
    }

    /**
     * Solution 4: Mathematical Approach with Detailed Validation
     * Time Complexity: O(n²)
     * Space Complexity: O(n)
     * 
     * Combines area checking with overlap detection.
     */
    public boolean isRectangleCover4(int[][] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            return false;

        // Calculate bounds and total area
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int totalArea = 0;

        for (int[] rect : rectangles) {
            minX = Math.min(minX, rect[0]);
            minY = Math.min(minY, rect[1]);
            maxX = Math.max(maxX, rect[2]);
            maxY = Math.max(maxY, rect[3]);
            totalArea += (rect[2] - rect[0]) * (rect[3] - rect[1]);
        }

        // Check total area
        int expectedArea = (maxX - minX) * (maxY - minY);
        if (totalArea != expectedArea)
            return false;

        // Check for overlaps
        for (int i = 0; i < rectangles.length; i++) {
            for (int j = i + 1; j < rectangles.length; j++) {
                if (hasOverlap(rectangles[i], rectangles[j])) {
                    return false;
                }
            }
        }

        // Additional check: verify coverage using events
        return verifyCoverage(rectangles, minX, minY, maxX, maxY);
    }

    private boolean hasOverlap(int[] rect1, int[] rect2) {
        return rect1[0] < rect2[2] && rect2[0] < rect1[2] &&
                rect1[1] < rect2[3] && rect2[1] < rect1[3];
    }

    private boolean verifyCoverage(int[][] rectangles, int minX, int minY, int maxX, int maxY) {
        // Use sweep line to verify complete coverage
        List<int[]> events = new ArrayList<>();

        for (int[] rect : rectangles) {
            events.add(new int[] { rect[0], rect[1], rect[3], 1 }); // start
            events.add(new int[] { rect[2], rect[1], rect[3], -1 }); // end
        }

        events.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[3] - b[3]);

        int currentX = minX;
        List<int[]> activeIntervals = new ArrayList<>();

        for (int[] event : events) {
            int x = event[0];
            int y1 = event[1];
            int y2 = event[2];
            int type = event[3];

            // Check coverage of current strip
            if (x > currentX && !activeIntervals.isEmpty()) {
                if (!coversExactly(new ArrayList<>(activeIntervals), minY, maxY)) {
                    return false;
                }
            }

            // Update active intervals
            if (type == 1) {
                activeIntervals.add(new int[] { y1, y2 });
            } else {
                activeIntervals.removeIf(interval -> interval[0] == y1 && interval[1] == y2);
            }

            currentX = x;
        }

        return true;
    }

    // Test methods
    public static void main(String[] args) {
        PerfectRectangleSolutions solution = new PerfectRectangleSolutions();

        // Test case 1: Perfect rectangle
        int[][] rectangles1 = { { 1, 1, 3, 3 }, { 3, 1, 4, 2 }, { 3, 2, 4, 4 }, { 1, 3, 2, 4 }, { 2, 3, 3, 4 } };
        System.out.println("Test 1 - Solution 1: " + solution.isRectangleCover1(rectangles1));
        System.out.println("Test 1 - Solution 2: " + solution.isRectangleCover2(rectangles1));
        System.out.println("Test 1 - Solution 3: " + solution.isRectangleCover3(rectangles1));
        System.out.println("Test 1 - Solution 4: " + solution.isRectangleCover4(rectangles1));

        // Test case 2: Gap
        int[][] rectangles2 = { { 1, 1, 2, 3 }, { 1, 3, 2, 4 }, { 3, 1, 4, 2 }, { 3, 2, 4, 4 } };
        System.out.println("\nTest 2 - Solution 1: " + solution.isRectangleCover1(rectangles2));
        System.out.println("Test 2 - Solution 2: " + solution.isRectangleCover2(rectangles2));
        System.out.println("Test 2 - Solution 3: " + solution.isRectangleCover3(rectangles2));
        System.out.println("Test 2 - Solution 4: " + solution.isRectangleCover4(rectangles2));

        // Test case 3: Overlap
        int[][] rectangles3 = { { 1, 1, 3, 3 }, { 3, 1, 4, 2 }, { 1, 3, 2, 4 }, { 2, 2, 4, 4 } };
        System.out.println("\nTest 3 - Solution 1: " + solution.isRectangleCover1(rectangles3));
        System.out.println("Test 3 - Solution 2: " + solution.isRectangleCover2(rectangles3));
        System.out.println("Test 3 - Solution 3: " + solution.isRectangleCover3(rectangles3));
        System.out.println("Test 3 - Solution 4: " + solution.isRectangleCover4(rectangles3));
    }

}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1 (Corner Point Counting):
 * - Time: O(n) - single pass through rectangles
 * - Space: O(n) - storing corner points
 * - BEST SOLUTION: Most efficient, elegant mathematical approach
 * - Key insight: Perfect rectangle has exactly 4 corner points that appear odd
 * number of times
 * 
 * Solution 2 (Sweep Line):
 * - Time: O(n²) - for each vertical strip, check intervals
 * - Space: O(n) - storing coordinates and intervals
 * - Good for understanding geometric algorithms
 * - Handles complex cases well but less efficient
 * 
 * Solution 3 (Grid-based):
 * - Time: O(n * m) where m is number of unique coordinates
 * - Space: O(m²) - grid storage
 * - Most intuitive but least efficient
 * - Can be very slow for large coordinate ranges
 * 
 * Solution 4 (Mathematical with Validation):
 * - Time: O(n²) - overlap checking dominates
 * - Space: O(n) - storing events and intervals
 * - Comprehensive but not optimal
 * - Good for learning different validation techniques
 * 
 * EDGE CASES HANDLED:
 * 1. Empty input
 * 2. Single rectangle
 * 3. Overlapping rectangles
 * 4. Gaps between rectangles
 * 5. Rectangles with zero area
 * 6. Large coordinate values
 * 
 * RECOMMENDED SOLUTION:
 * Solution 1 (Corner Point Counting) is the most efficient and elegant.
 * It's based on the mathematical property that in a perfect rectangle cover,
 * each interior point appears in an even number of rectangles, while
 * the four corners of the large rectangle appear in exactly one rectangle each.
 */

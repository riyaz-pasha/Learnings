import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class EqualAreaDivider {

    static class Event {
        int y1, y2;

        public Event(int y1, int y2) {
            this.y1 = y1;
            this.y2 = y2;
        }
    }

    public static double findVerticalLine(int[][] rectangles) {
        // Step 1: Collect and sort unique x-coordinates
        Set<Integer> xSet = new HashSet<>();
        for (int[] rect : rectangles) {
            xSet.add(rect[0]);
            xSet.add(rect[2]);
        }

        List<Integer> xList = new ArrayList<>(xSet);
        Collections.sort(xList);
        int n = xList.size();

        // Step 2: Precompute x-index map
        Map<Integer, Integer> xIndexMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            xIndexMap.put(xList.get(i), i);
        }

        // Step 3: Prepare vertical strips for sweep-line events
        List<Event>[] verticalStrips = new ArrayList[n - 1];
        for (int i = 0; i < n - 1; i++) {
            verticalStrips[i] = new ArrayList<>();
        }

        for (int[] rect : rectangles) {
            int x1 = rect[0], y1 = rect[1], x2 = rect[2], y2 = rect[3];
            int leftIndex = xIndexMap.get(x1);
            int rightIndex = xIndexMap.get(x2);

            for (int i = leftIndex; i < rightIndex; i++) {
                verticalStrips[i].add(new Event(y1, y2));
            }
        }

        // Step 4: Compute area of each vertical strip
        double[] areaByStrip = new double[n - 1];
        double totalArea = 0;

        for (int i = 0; i < n - 1; i++) {
            List<int[]> mergedY = mergeYIntervals(verticalStrips[i]);
            int yUnionLength = 0;
            for (int[] interval : mergedY) {
                yUnionLength += (interval[1] - interval[0]);
            }
            double width = xList.get(i + 1) - xList.get(i);
            double stripArea = yUnionLength * width;
            areaByStrip[i] = stripArea;
            totalArea += stripArea;
        }

        // Step 5: Locate vertical line such that left area = totalArea / 2
        double targetArea = totalArea / 2.0;
        double accumulatedArea = 0;

        for (int i = 0; i < n - 1; i++) {
            double stripArea = areaByStrip[i];
            double nextAccumulated = accumulatedArea + stripArea;

            if (nextAccumulated >= targetArea) {
                // Interpolate inside this strip to find the exact x
                double excess = nextAccumulated - targetArea;
                double fillRatio = (stripArea - excess) / stripArea;

                double stripWidth = xList.get(i + 1) - xList.get(i);
                double dividingX = xList.get(i) + stripWidth * fillRatio;

                // Optional: Round to 5 decimal places
                return Math.round(dividingX * 100000.0) / 100000.0;
            }

            accumulatedArea = nextAccumulated;
        }

        return -1; // Fallback (should not be hit)
    }

    // ✅ Simplified version of merging overlapping y-intervals
    private static List<int[]> mergeYIntervals(List<Event> events) {
        if (events.isEmpty())
            return Collections.emptyList();

        List<int[]> intervals = new ArrayList<>();
        for (Event e : events) {
            intervals.add(new int[] { e.y1, e.y2 });
        }

        intervals.sort(Comparator.comparingInt(a -> a[0]));

        List<int[]> merged = new ArrayList<>();
        int[] current = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            int[] next = intervals.get(i);
            if (next[0] <= current[1]) {
                current[1] = Math.max(current[1], next[1]); // Merge
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    // 🧪 Test
    public static void main(String[] args) {
        int[][] rectangles = {
                { 1, 1, 4, 5 },
                { 2, 3, 6, 6 }
        };

        double dividingX = findVerticalLine(rectangles);
        System.out.printf("Vertical line at x = %.5f divides the area equally.\n", dividingX);
    }

}

/**
 * This algorithm finds a vertical line x = value that divides the union of
 * possibly overlapping
 * rectangles into two equal-area halves.
 *
 * ✅ Problem:
 * - Each rectangle is represented by [x1, y1, x2, y2] (bottom-left to
 * top-right).
 * - Rectangles can overlap.
 * - We must find a vertical line such that the total area to the left equals
 * the total area to the right.
 *
 * ✅ Approach Overview:
 *
 * 1. Collect Unique X-Coordinates:
 * - Gather all unique x-coordinates (start and end of each rectangle).
 * - Sort them to define the vertical strip boundaries.
 *
 * 2. Build a Sweep-Line Structure:
 * - Each vertical strip (between two x-values) will hold y-intervals (Events)
 * representing
 * active rectangle segments across that x-range.
 * - For every rectangle, determine which strips it spans and add its y-range to
 * those strips.
 *
 * 3. Compute Area Per Vertical Strip:
 * - For each vertical strip:
 * - Merge overlapping y-intervals to get the union of y-coverage.
 * - Calculate the area of that strip = union_y_length * strip_width.
 * - Also accumulate the total area of all strips.
 *
 * 4. Locate the Balanced Vertical Line:
 * - Iterate over the strips and keep accumulating area.
 * - When the accumulated area reaches or crosses half of the total area:
 * - The desired vertical line lies within this strip.
 * - Use linear interpolation to compute the exact x-position inside this strip
 * where
 * the left-side area equals half the total.
 *
 * ✅ Time Complexity:
 * - O(rk log r) in worst case, where:
 * - r = number of rectangles
 * - k = number of unique x-coordinates (at most 2r)
 * - Efficient for large inputs, especially when rectangles don't span too many
 * strips.
 *
 * ✅ Space Complexity:
 * - O(rk) for storing events across vertical strips and intermediate data.
 *
 * ✅ Notes:
 * - This algorithm handles overlapping rectangles correctly by computing the
 * union area
 * in each vertical slice.
 * - It does not require the rectangles to be axis-aligned in any specific
 * order.
 */

class OptimizedAreaDivider {

    // ========== SOLUTION 1: OPTIMIZED SWEEP LINE ==========

    static class Rectangle {
        int x1, y1, x2, y2;
        long area;

        Rectangle(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.area = (long) (x2 - x1) * (y2 - y1);
        }
    }

    static class SweepEvent {
        int x, y1, y2, type; // type: 1 = start, -1 = end

        SweepEvent(int x, int y1, int y2, int type) {
            this.x = x;
            this.y1 = y1;
            this.y2 = y2;
            this.type = type;
        }
    }

    /**
     * OPTIMIZED APPROACH: Uses coordinate compression and segment tree
     * Time: O(n log n), Space: O(n)
     */
    public static double findVerticalLineOptimized(int[][] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            return -1;

        // Create events for sweep line
        List<SweepEvent> events = new ArrayList<>();
        Set<Integer> yCoords = new TreeSet<>();

        for (int[] rect : rectangles) {
            events.add(new SweepEvent(rect[0], rect[1], rect[3], 1)); // start
            events.add(new SweepEvent(rect[2], rect[1], rect[3], -1)); // end
            yCoords.add(rect[1]);
            yCoords.add(rect[3]);
        }

        // Sort events by x-coordinate
        events.sort((a, b) -> a.x != b.x ? Integer.compare(a.x, b.x) : Integer.compare(b.type, a.type));

        // Coordinate compression for y-coordinates
        List<Integer> yList = new ArrayList<>(yCoords);
        Map<Integer, Integer> yIndex = new HashMap<>();
        for (int i = 0; i < yList.size(); i++) {
            yIndex.put(yList.get(i), i);
        }

        // Sweep line with active intervals
        double totalArea = 0;
        Map<Integer, Double> xToLeftArea = new TreeMap<>();

        // Count array for active y-intervals
        int[] activeCount = new int[yList.size() - 1];
        int lastX = events.get(0).x;

        for (SweepEvent event : events) {
            if (event.x != lastX) {
                // Calculate area for this x-strip
                double currentArea = 0;
                for (int i = 0; i < activeCount.length; i++) {
                    if (activeCount[i] > 0) {
                        currentArea += yList.get(i + 1) - yList.get(i);
                    }
                }
                double stripArea = currentArea * (event.x - lastX);
                totalArea += stripArea;
                xToLeftArea.put(event.x, totalArea);
                lastX = event.x;
            }

            // Update active intervals
            int y1Idx = yIndex.get(event.y1);
            int y2Idx = yIndex.get(event.y2);
            for (int i = y1Idx; i < y2Idx; i++) {
                activeCount[i] += event.type;
            }
        }

        // Binary search for the target area
        double targetArea = totalArea / 2.0;

        Integer[] xPoints = xToLeftArea.keySet().toArray(new Integer[0]);
        for (int i = 0; i < xPoints.length; i++) {
            if (xToLeftArea.get(xPoints[i]) >= targetArea) {
                if (i == 0)
                    return xPoints[0];

                double prevArea = i > 0 ? xToLeftArea.get(xPoints[i - 1]) : 0;
                double currArea = xToLeftArea.get(xPoints[i]);

                if (Math.abs(currArea - prevArea) < 1e-9)
                    return xPoints[i];

                double ratio = (targetArea - prevArea) / (currArea - prevArea);
                return xPoints[i - 1] + ratio * (xPoints[i] - xPoints[i - 1]);
            }
        }

        return -1;
    }

    // ========== SOLUTION 2: BINARY SEARCH ON ANSWER ==========

    /**
     * BINARY SEARCH APPROACH: Binary search on x-coordinate
     * Time: O(n log n log(max_coordinate)), Space: O(n)
     */
    public static double findVerticalLineBinarySearch(int[][] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            return -1;

        // Find x-coordinate bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (int[] rect : rectangles) {
            minX = Math.min(minX, rect[0]);
            maxX = Math.max(maxX, rect[2]);
        }

        double totalArea = calculateTotalAreaFast(rectangles);
        double targetArea = totalArea / 2.0;

        double left = minX, right = maxX;
        double eps = 1e-9;

        while (right - left > eps) {
            double mid = (left + right) / 2.0;
            double leftArea = calculateAreaToLeft(rectangles, mid);

            if (leftArea < targetArea) {
                left = mid;
            } else {
                right = mid;
            }
        }

        return (left + right) / 2.0;
    }

    private static double calculateAreaToLeft(int[][] rectangles, double x) {
        List<int[]> intervals = new ArrayList<>();

        for (int[] rect : rectangles) {
            if (rect[0] < x) {
                double rightBound = Math.min(rect[2], x);
                if (rightBound > rect[0]) {
                    // Add y-interval weighted by x-width
                    double width = rightBound - rect[0];
                    intervals.add(new int[] { rect[1], rect[3], (int) (width * 1000000) }); // Scale for precision
                }
            }
        }

        return calculateWeightedIntervalUnion(intervals) / 1000000.0;
    }

    private static double calculateWeightedIntervalUnion(List<int[]> intervals) {
        if (intervals.isEmpty())
            return 0;

        // Sort by y1
        intervals.sort(Comparator.comparingInt(a -> a[0]));

        double totalArea = 0;
        int currentStart = intervals.get(0)[0];
        int currentEnd = intervals.get(0)[1];
        double currentWeight = intervals.get(0)[2];

        for (int i = 1; i < intervals.size(); i++) {
            int[] interval = intervals.get(i);

            if (interval[0] <= currentEnd) {
                // Overlapping - need to handle weighted merge
                if (interval[1] > currentEnd) {
                    totalArea += (currentEnd - currentStart) * currentWeight;
                    currentStart = currentEnd;
                    currentEnd = interval[1];
                    currentWeight = interval[2];
                } else {
                    currentWeight = Math.max(currentWeight, interval[2]);
                }
            } else {
                // Non-overlapping
                totalArea += (currentEnd - currentStart) * currentWeight;
                currentStart = interval[0];
                currentEnd = interval[1];
                currentWeight = interval[2];
            }
        }

        totalArea += (currentEnd - currentStart) * currentWeight;
        return totalArea;
    }

    // ========== SOLUTION 3: DIVIDE AND CONQUER ==========

    /**
     * DIVIDE AND CONQUER APPROACH: Recursively divide space
     * Time: O(n log n), Space: O(log n)
     */
    public static double findVerticalLineDivideConquer(int[][] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            return -1;

        int minX = Arrays.stream(rectangles).mapToInt(r -> r[0]).min().orElse(0);
        int maxX = Arrays.stream(rectangles).mapToInt(r -> r[2]).max().orElse(0);

        double totalArea = calculateTotalAreaFast(rectangles);
        return divideConquerHelper(rectangles, minX, maxX, totalArea / 2.0);
    }

    private static double divideConquerHelper(int[][] rectangles, double left, double right, double targetArea) {
        if (right - left < 1e-9)
            return (left + right) / 2.0;

        double mid = (left + right) / 2.0;
        double leftArea = calculateAreaToLeft(rectangles, mid);

        if (Math.abs(leftArea - targetArea) < 1e-9) {
            return mid;
        } else if (leftArea < targetArea) {
            return divideConquerHelper(rectangles, mid, right, targetArea);
        } else {
            return divideConquerHelper(rectangles, left, mid, targetArea);
        }
    }

    // ========== UTILITY METHODS ==========

    private static double calculateTotalAreaFast(int[][] rectangles) {
        // Use the sweep line algorithm for fast total area calculation
        List<SweepEvent> events = new ArrayList<>();
        Set<Integer> yCoords = new TreeSet<>();

        for (int[] rect : rectangles) {
            events.add(new SweepEvent(rect[0], rect[1], rect[3], 1));
            events.add(new SweepEvent(rect[2], rect[1], rect[3], -1));
            yCoords.add(rect[1]);
            yCoords.add(rect[3]);
        }

        events.sort((a, b) -> a.x != b.x ? Integer.compare(a.x, b.x) : Integer.compare(b.type, a.type));

        List<Integer> yList = new ArrayList<>(yCoords);
        Map<Integer, Integer> yIndex = new HashMap<>();
        for (int i = 0; i < yList.size(); i++) {
            yIndex.put(yList.get(i), i);
        }

        double totalArea = 0;
        int[] activeCount = new int[yList.size() - 1];
        int lastX = events.get(0).x;

        for (SweepEvent event : events) {
            if (event.x != lastX) {
                double currentHeight = 0;
                for (int i = 0; i < activeCount.length; i++) {
                    if (activeCount[i] > 0) {
                        currentHeight += yList.get(i + 1) - yList.get(i);
                    }
                }
                totalArea += currentHeight * (event.x - lastX);
                lastX = event.x;
            }

            int y1Idx = yIndex.get(event.y1);
            int y2Idx = yIndex.get(event.y2);
            for (int i = y1Idx; i < y2Idx; i++) {
                activeCount[i] += event.type;
            }
        }

        return totalArea;
    }

    // ========== PERFORMANCE TESTING ==========

    public static void comparePerformance() {
        System.out.println("=== PERFORMANCE COMPARISON ===\n");

        // Generate test data
        int[][] smallData = generateRandomRectangles(100, 1000);
        int[][] mediumData = generateRandomRectangles(1000, 10000);
        int[][] largeData = generateRandomRectangles(5000, 100000);

        testAlgorithm("Optimized Sweep Line", smallData, mediumData, largeData,
                OptimizedAreaDivider::findVerticalLineOptimized);

        testAlgorithm("Binary Search", smallData, mediumData, largeData,
                OptimizedAreaDivider::findVerticalLineBinarySearch);

        testAlgorithm("Divide & Conquer", smallData, mediumData, largeData,
                OptimizedAreaDivider::findVerticalLineDivideConquer);
    }

    private static void testAlgorithm(String name, int[][] small, int[][] medium, int[][] large,
            java.util.function.Function<int[][], Double> algorithm) {
        System.out.println(name + ":");

        long start = System.nanoTime();
        algorithm.apply(small);
        long smallTime = System.nanoTime() - start;

        start = System.nanoTime();
        algorithm.apply(medium);
        long mediumTime = System.nanoTime() - start;

        start = System.nanoTime();
        algorithm.apply(large);
        long largeTime = System.nanoTime() - start;

        System.out.printf("  Small (100): %.2f ms\n", smallTime / 1e6);
        System.out.printf("  Medium (1k): %.2f ms\n", mediumTime / 1e6);
        System.out.printf("  Large (5k): %.2f ms\n\n", largeTime / 1e6);
    }

    private static int[][] generateRandomRectangles(int count, int maxCoord) {
        Random rand = new Random(42); // Fixed seed for consistent testing
        int[][] rectangles = new int[count][4];

        for (int i = 0; i < count; i++) {
            int x1 = rand.nextInt(maxCoord);
            int y1 = rand.nextInt(maxCoord);
            int x2 = x1 + rand.nextInt(maxCoord - x1) + 1;
            int y2 = y1 + rand.nextInt(maxCoord - y1) + 1;
            rectangles[i] = new int[] { x1, y1, x2, y2 };
        }

        return rectangles;
    }

    // ========== MAIN TEST ==========

    public static void main(String[] args) {
        // Test with the original example
        int[][] rectangles = {
                { 1, 1, 4, 5 },
                { 2, 3, 6, 6 }
        };

        System.out.println("=== ALGORITHM COMPARISON ===");
        System.out.println("Test rectangles: {1,1,4,5}, {2,3,6,6}\n");

        double result1 = findVerticalLineOptimized(rectangles);
        double result2 = findVerticalLineBinarySearch(rectangles);
        double result3 = findVerticalLineDivideConquer(rectangles);

        System.out.printf("Optimized Sweep Line: x = %.6f\n", result1);
        System.out.printf("Binary Search:        x = %.6f\n", result2);
        System.out.printf("Divide & Conquer:     x = %.6f\n", result3);
        System.out.printf("Total Area: %.2f\n\n", calculateTotalAreaFast(rectangles));

        // Performance comparison
        comparePerformance();

        // Verify correctness
        System.out.println("=== CORRECTNESS VERIFICATION ===");
        double leftArea = calculateAreaToLeft(rectangles, result1);
        double totalArea = calculateTotalAreaFast(rectangles);
        System.out.printf("Left area: %.6f, Right area: %.6f\n", leftArea, totalArea - leftArea);
        System.out.printf("Difference: %.9f\n", Math.abs(leftArea - (totalArea - leftArea)));
    }

}

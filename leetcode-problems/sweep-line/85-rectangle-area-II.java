import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/*
 * You are given a 2D array of axis-aligned rectangles. Each rectangle[i] =
 * [xi1, yi1, xi2, yi2] denotes the ith rectangle where (xi1, yi1) are the
 * coordinates of the bottom-left corner, and (xi2, yi2) are the coordinates of
 * the top-right corner.
 * 
 * Calculate the total area covered by all rectangles in the plane. Any area
 * covered by two or more rectangles should only be counted once.
 * 
 * Return the total area. Since the answer may be too large, return it modulo
 * 109 + 7.
 * 
 * 
 * Example 1:
 * Input: rectangles = [[0,0,2,2],[1,0,2,3],[1,0,3,1]]
 * Output: 6
 * Explanation: A total area of 6 is covered by all three rectangles, as
 * illustrated in the picture.
 * From (1,1) to (2,2), the green and red rectangles overlap.
 * From (1,0) to (2,3), all three rectangles overlap.
 * 
 * Example 2:
 * Input: rectangles = [[0,0,1000000000,1000000000]]
 * Output: 49
 * Explanation: The answer is 1018 modulo (109 + 7), which is 49.
 */

class RectangleAreaSolutions {

    private static final int MOD = 1000000007;

    /**
     * Solution 1: Coordinate Compression (Most Efficient)
     * Time Complexity: O(n²)
     * Space Complexity: O(n²)
     * 
     * This is the most efficient approach for this problem.
     * It compresses coordinates and uses a 2D grid to track covered areas.
     */
    public int rectangleArea1(int[][] rectangles) {
        // Get all unique x and y coordinates
        Set<Integer> xCoords = new TreeSet<>();
        Set<Integer> yCoords = new TreeSet<>();

        for (int[] rect : rectangles) {
            xCoords.add(rect[0]);
            xCoords.add(rect[2]);
            yCoords.add(rect[1]);
            yCoords.add(rect[3]);
        }

        // Convert to sorted arrays
        Integer[] xs = xCoords.toArray(new Integer[0]);
        Integer[] ys = yCoords.toArray(new Integer[0]);

        // Create coordinate mapping
        Map<Integer, Integer> xMap = new HashMap<>();
        Map<Integer, Integer> yMap = new HashMap<>();

        for (int i = 0; i < xs.length; i++) {
            xMap.put(xs[i], i);
        }
        for (int i = 0; i < ys.length; i++) {
            yMap.put(ys[i], i);
        }

        // Create grid to mark covered areas
        boolean[][] grid = new boolean[xs.length - 1][ys.length - 1];

        // Mark rectangles on grid
        for (int[] rect : rectangles) {
            int x1 = xMap.get(rect[0]);
            int y1 = yMap.get(rect[1]);
            int x2 = xMap.get(rect[2]);
            int y2 = yMap.get(rect[3]);

            for (int i = x1; i < x2; i++) {
                for (int j = y1; j < y2; j++) {
                    grid[i][j] = true;
                }
            }
        }

        // Calculate total area
        long totalArea = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j]) {
                    long width = (long) xs[i + 1] - xs[i];
                    long height = (long) ys[j + 1] - ys[j];
                    totalArea = (totalArea + (width * height) % MOD) % MOD;
                }
            }
        }

        return (int) totalArea;
    }

    /**
     * Solution 2: Line Sweep Algorithm
     * Time Complexity: O(n² log n)
     * Space Complexity: O(n)
     * 
     * Uses vertical line sweep with interval merging at each x-coordinate.
     */
    public int rectangleArea2(int[][] rectangles) {
        // Create events for vertical sweep
        List<Event> events = new ArrayList<>();

        for (int[] rect : rectangles) {
            events.add(new Event(rect[0], rect[1], rect[3], 1)); // start
            events.add(new Event(rect[2], rect[1], rect[3], -1)); // end
        }

        // Sort events by x-coordinate
        events.sort((a, b) -> a.x - b.x);

        long totalArea = 0;
        int prevX = 0;
        List<int[]> activeIntervals = new ArrayList<>();

        for (Event event : events) {
            // Calculate area from previous x to current x
            if (event.x > prevX && !activeIntervals.isEmpty()) {
                long width = event.x - prevX;
                long height = getTotalHeight(activeIntervals);
                totalArea = (totalArea + (width * height) % MOD) % MOD;
            }

            // Update active intervals
            if (event.type == 1) { // start event
                addInterval(activeIntervals, event.y1, event.y2);
            } else { // end event
                removeInterval(activeIntervals, event.y1, event.y2);
            }

            prevX = event.x;
        }

        return (int) totalArea;
    }

    private static class Event {
        int x, y1, y2, type;

        Event(int x, int y1, int y2, int type) {
            this.x = x;
            this.y1 = y1;
            this.y2 = y2;
            this.type = type;
        }
    }

    private void addInterval(List<int[]> intervals, int start, int end) {
        intervals.add(new int[] { start, end });
    }

    private void removeInterval(List<int[]> intervals, int start, int end) {
        intervals.removeIf(interval -> interval[0] == start && interval[1] == end);
    }

    private long getTotalHeight(List<int[]> intervals) {
        if (intervals.isEmpty())
            return 0;

        // Sort intervals and merge overlapping ones
        List<int[]> merged = new ArrayList<>(intervals);
        merged.sort((a, b) -> a[0] - b[0]);

        long totalHeight = 0;
        int currentStart = merged.get(0)[0];
        int currentEnd = merged.get(0)[1];

        for (int i = 1; i < merged.size(); i++) {
            int[] interval = merged.get(i);
            if (interval[0] <= currentEnd) {
                currentEnd = Math.max(currentEnd, interval[1]);
            } else {
                totalHeight += currentEnd - currentStart;
                currentStart = interval[0];
                currentEnd = interval[1];
            }
        }

        totalHeight += currentEnd - currentStart;
        return totalHeight;
    }

    /**
     * Solution 3: Segment Tree Approach
     * Time Complexity: O(n² log n)
     * Space Complexity: O(n)
     * 
     * Uses segment tree for efficient interval updates and queries.
     * More complex but demonstrates advanced data structure usage.
     */
    public int rectangleArea3(int[][] rectangles) {
        // Coordinate compression for y-coordinates
        Set<Integer> yCoords = new TreeSet<>();
        for (int[] rect : rectangles) {
            yCoords.add(rect[1]);
            yCoords.add(rect[3]);
        }

        List<Integer> sortedY = new ArrayList<>(yCoords);
        Map<Integer, Integer> yIndex = new HashMap<>();
        for (int i = 0; i < sortedY.size(); i++) {
            yIndex.put(sortedY.get(i), i);
        }

        // Create events
        List<Event> events = new ArrayList<>();
        for (int[] rect : rectangles) {
            int y1 = yIndex.get(rect[1]);
            int y2 = yIndex.get(rect[3]);
            events.add(new Event(rect[0], y1, y2, 1));
            events.add(new Event(rect[2], y1, y2, -1));
        }

        events.sort((a, b) -> a.x - b.x);

        // Segment tree for range updates
        SegmentTree segTree = new SegmentTree(sortedY.size() - 1);

        long totalArea = 0;
        int prevX = events.get(0).x;

        for (Event event : events) {
            if (event.x > prevX) {
                long width = event.x - prevX;
                long height = segTree.query();
                totalArea = (totalArea + (width * height) % MOD) % MOD;
            }

            segTree.update(event.y1, event.y2 - 1, event.type);
            prevX = event.x;
        }

        return (int) totalArea;
    }

    private class SegmentTree {
        private int[] tree;
        private int[] lazy;
        private List<Integer> coords;
        private int n;

        public SegmentTree(int size) {
            this.n = size;
            this.tree = new int[4 * size];
            this.lazy = new int[4 * size];
        }

        public void update(int start, int end, int val) {
            update(1, 0, n - 1, start, end, val);
        }

        private void update(int node, int tl, int tr, int ql, int qr, int val) {
            if (ql > qr)
                return;
            if (ql == tl && qr == tr) {
                lazy[node] += val;
                return;
            }

            int tm = (tl + tr) / 2;
            update(2 * node, tl, tm, ql, Math.min(qr, tm), val);
            update(2 * node + 1, tm + 1, tr, Math.max(ql, tm + 1), qr, val);
        }

        public long query() {
            return query(1, 0, n - 1);
        }

        private long query(int node, int tl, int tr) {
            if (lazy[node] > 0) {
                // This segment is completely covered
                return getSegmentLength(tl, tr);
            }

            if (tl == tr) {
                return 0;
            }

            int tm = (tl + tr) / 2;
            return query(2 * node, tl, tm) + query(2 * node + 1, tm + 1, tr);
        }

        private long getSegmentLength(int l, int r) {
            // This would need the actual y-coordinates, simplified for this example
            return r - l + 1;
        }
    }

    /**
     * Solution 4: Inclusion-Exclusion Principle (Advanced)
     * Time Complexity: O(2^n * n)
     * Space Complexity: O(n)
     * 
     * Uses inclusion-exclusion principle but exponential time complexity.
     * Only suitable for small number of rectangles (n <= 20).
     */
    public int rectangleArea4(int[][] rectangles) {
        int n = rectangles.length;
        long totalArea = 0;

        // Apply inclusion-exclusion principle
        for (int mask = 1; mask < (1 << n); mask++) {
            Rectangle intersection = null;
            int bits = 0;

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    bits++;
                    Rectangle current = new Rectangle(rectangles[i][0], rectangles[i][1],
                            rectangles[i][2], rectangles[i][3]);
                    if (intersection == null) {
                        intersection = current;
                    } else {
                        intersection = getIntersection(intersection, current);
                        if (intersection == null)
                            break;
                    }
                }
            }

            if (intersection != null) {
                long area = intersection.getArea();
                if (bits % 2 == 1) {
                    totalArea = (totalArea + area) % MOD;
                } else {
                    totalArea = (totalArea - area + MOD) % MOD;
                }
            }
        }

        return (int) totalArea;
    }

    private static class Rectangle {
        int x1, y1, x2, y2;

        Rectangle(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        long getArea() {
            return ((long) (x2 - x1)) * (y2 - y1);
        }
    }

    private Rectangle getIntersection(Rectangle r1, Rectangle r2) {
        int x1 = Math.max(r1.x1, r2.x1);
        int y1 = Math.max(r1.y1, r2.y1);
        int x2 = Math.min(r1.x2, r2.x2);
        int y2 = Math.min(r1.y2, r2.y2);

        if (x1 >= x2 || y1 >= y2) {
            return null; // No intersection
        }

        return new Rectangle(x1, y1, x2, y2);
    }

    /**
     * Solution 5: Optimized Coordinate Compression with BitSet
     * Time Complexity: O(n²)
     * Space Complexity: O(n²)
     * 
     * Uses BitSet for memory efficiency when dealing with large grids.
     */
    public int rectangleArea5(int[][] rectangles) {
        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();

        for (int[] rect : rectangles) {
            xs.add(rect[0]);
            xs.add(rect[2]);
            ys.add(rect[1]);
            ys.add(rect[3]);
        }

        xs = xs.stream().distinct().sorted().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ys = ys.stream().distinct().sorted().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        Map<Integer, Integer> xMap = new HashMap<>();
        Map<Integer, Integer> yMap = new HashMap<>();

        for (int i = 0; i < xs.size(); i++) {
            xMap.put(xs.get(i), i);
        }
        for (int i = 0; i < ys.size(); i++) {
            yMap.put(ys.get(i), i);
        }

        // Use BitSet for memory efficiency
        BitSet[] grid = new BitSet[xs.size() - 1];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = new BitSet(ys.size() - 1);
        }

        // Mark covered areas
        for (int[] rect : rectangles) {
            int x1 = xMap.get(rect[0]);
            int y1 = yMap.get(rect[1]);
            int x2 = xMap.get(rect[2]);
            int y2 = yMap.get(rect[3]);

            for (int i = x1; i < x2; i++) {
                grid[i].set(y1, y2);
            }
        }

        // Calculate total area
        long totalArea = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = grid[i].nextSetBit(0); j >= 0; j = grid[i].nextSetBit(j + 1)) {
                long width = xs.get(i + 1) - xs.get(i);
                long height = ys.get(j + 1) - ys.get(j);
                totalArea = (totalArea + (width * height) % MOD) % MOD;
            }
        }

        return (int) totalArea;
    }

    // Test methods
    public static void main(String[] args) {
        RectangleAreaSolutions solution = new RectangleAreaSolutions();

        // Test case 1
        int[][] rectangles1 = { { 0, 0, 2, 2 }, { 1, 0, 2, 3 }, { 1, 0, 3, 1 } };
        System.out.println("Test 1:");
        System.out.println("Input: [[0,0,2,2],[1,0,2,3],[1,0,3,1]]");
        System.out.println("Expected: 6");
        System.out.println("Solution 1: " + solution.rectangleArea1(rectangles1));
        System.out.println("Solution 2: " + solution.rectangleArea2(rectangles1));
        System.out.println("Solution 4: " + solution.rectangleArea4(rectangles1));
        System.out.println("Solution 5: " + solution.rectangleArea5(rectangles1));

        // Test case 2
        int[][] rectangles2 = { { 0, 0, 1000000000, 1000000000 } };
        System.out.println("\nTest 2:");
        System.out.println("Input: [[0,0,1000000000,1000000000]]");
        System.out.println("Expected: 49");
        System.out.println("Solution 1: " + solution.rectangleArea1(rectangles2));
        System.out.println("Solution 2: " + solution.rectangleArea2(rectangles2));
        System.out.println("Solution 4: " + solution.rectangleArea4(rectangles2));
        System.out.println("Solution 5: " + solution.rectangleArea5(rectangles2));

        // Test case 3: No overlap
        int[][] rectangles3 = { { 0, 0, 1, 1 }, { 2, 2, 3, 3 } };
        System.out.println("\nTest 3:");
        System.out.println("Input: [[0,0,1,1],[2,2,3,3]]");
        System.out.println("Expected: 2");
        System.out.println("Solution 1: " + solution.rectangleArea1(rectangles3));

        // Test case 4: Complete overlap
        int[][] rectangles4 = { { 0, 0, 2, 2 }, { 0, 0, 2, 2 } };
        System.out.println("\nTest 4:");
        System.out.println("Input: [[0,0,2,2],[0,0,2,2]]");
        System.out.println("Expected: 4");
        System.out.println("Solution 1: " + solution.rectangleArea1(rectangles4));
    }

}

class Solution {
    
    static final int MOD = 1_000_000_007;

    public int rectangleArea(int[][] rectangles) {
        List<Event> events = new ArrayList<>();
        for (int[] rect : rectangles) {
            // Opening event
            events.add(new Event(rect[0], rect[1], rect[3], 1));
            // Closing event
            events.add(new Event(rect[2], rect[1], rect[3], -1));
        }

        // Sort by x-coordinate
        events.sort(Comparator.comparingInt(e -> e.x));

        // Active y-intervals
        List<int[]> active = new ArrayList<>();
        long totalArea = 0;
        int prevX = events.get(0).x;

        for (Event event : events) {
            int currX = event.x;

            // Calculate covered height from active intervals
            long ySum = calcYUnion(active);
            totalArea = (totalArea + ySum * (currX - prevX)) % MOD;

            // Update active intervals
            if (event.type == 1) {
                active.add(new int[]{event.y1, event.y2});
            } else {
                // Remove matching interval
                for (int i = 0; i < active.size(); i++) {
                    int[] a = active.get(i);
                    if (a[0] == event.y1 && a[1] == event.y2) {
                        active.remove(i);
                        break;
                    }
                }
            }

            prevX = currX;
        }

        return (int) totalArea;
    }

    private long calcYUnion(List<int[]> intervals) {
        // Union of intervals on y-axis
        intervals.sort(Comparator.comparingInt(a -> a[0]));
        long total = 0;
        int start = -1, end = -1;

        for (int[] interval : intervals) {
            if (interval[0] > end) {
                total += end - start;
                start = interval[0];
                end = interval[1];
            } else {
                end = Math.max(end, interval[1]);
            }
        }
        total += end - start;
        return total;
    }

    static class Event {
        int x, y1, y2, type;
        Event(int x, int y1, int y2, int type) {
            this.x = x;
            this.y1 = y1;
            this.y2 = y2;
            this.type = type; // 1 = open, -1 = close
        }
    }

}


/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1 (Coordinate Compression):
 * - Time: O(n²) - coordinate compression O(n log n) + grid marking O(n³) in
 * worst case
 * - Space: O(n²) - grid storage
 * - BEST GENERAL SOLUTION: Most efficient for typical inputs
 * 
 * Solution 2 (Line Sweep):
 * - Time: O(n² log n) - sorting events + interval merging for each x
 * - Space: O(n) - storing events and intervals
 * - Good for sparse rectangles
 * 
 * Solution 3 (Segment Tree):
 * - Time: O(n² log n) - segment tree operations
 * - Space: O(n) - segment tree storage
 * - Advanced approach, good for learning data structures
 * 
 * Solution 4 (Inclusion-Exclusion):
 * - Time: O(2^n * n) - exponential in number of rectangles
 * - Space: O(n) - recursion stack
 * - Only suitable for small n (≤ 20)
 * 
 * Solution 5 (BitSet Optimization):
 * - Time: O(n²) - similar to solution 1
 * - Space: O(n²) - but more memory efficient with BitSet
 * - Good for memory-constrained environments
 * 
 * KEY INSIGHTS:
 * 
 * 1. **Coordinate Compression**: Essential for handling large coordinate values
 * efficiently
 * 2. **Modular Arithmetic**: Required due to large potential results (10^18)
 * 3. **Grid Approach**: Most intuitive and efficient for general case
 * 4. **Overlap Handling**: Critical to count overlapping areas only once
 * 
 * EDGE CASES:
 * - Single rectangle
 * - Non-overlapping rectangles
 * - Completely overlapping rectangles
 * - Large coordinate values (up to 10^9)
 * - Maximum number of rectangles
 * 
 * RECOMMENDED SOLUTION:
 * Solution 1 (Coordinate Compression) is recommended for most cases.
 * It provides optimal time complexity and handles all edge cases efficiently.
 * 
 * OPTIMIZATION NOTES:
 * - Use long for intermediate calculations to avoid overflow
 * - Apply modulo operation carefully during multiplication
 * - Consider BitSet for memory optimization when grid is sparse
 */

 /**
 * ✅ Sweep Line + Interval Union Approach: Time & Space Complexity Analysis
 *
 * OVERVIEW:
 * ----------
 * 1. For each rectangle, we create two events:
 *    - One at the left edge (enter event)
 *    - One at the right edge (exit event)
 *    → For n rectangles, we create 2n events
 *
 * 2. Sort all events by x-coordinate
 *
 * 3. Sweep through sorted events:
 *    - At each x-coordinate, calculate the total Y-interval union (vertical coverage)
 *    - Multiply it by delta-x to compute area slice
 *    - Update the active interval set by adding/removing the current rectangle’s y-range
 *
 * TIME COMPLEXITY:
 * ----------------
 * 1. Event Creation:
 *    - We create 2 events per rectangle → 2n total
 *    - Time: O(n)
 *
 * 2. Sorting Events:
 *    - Sort 2n events by x
 *    - Time: O(n log n)
 *
 * 3. Processing Events (Main Loop):
 *    - We process 2n events
 *    - At each event, we compute the Y-interval union:
 *        a. Sorting active intervals by Y-start: O(k log k)
 *        b. Merging overlapping intervals: O(k)
 *        → Total per event: O(k log k), worst-case k = n
 *    - Worst-case per event: O(n log n)
 *    - Worst-case total over all events: O(n^2)
 *
 *    → Why O(n^2)?
 *      In worst-case scenarios (e.g., all rectangles overlapping), at every event
 *      we may need to recalculate union across all n active intervals.
 *
 * TOTAL TIME COMPLEXITY:
 * ----------------------
 * O(n log n) for sorting events +
 * O(n^2) for recalculating active Y-unions in worst case
 * → Total: O(n log n + n^2)
 *
 * SPACE COMPLEXITY:
 * -----------------
 * 1. Events List:
 *    - Stores 2n events → O(n)
 *
 * 2. Active Intervals Set:
 *    - Can grow up to n intervals → O(n)
 *
 * 3. Temporary Sorting:
 *    - Sorting Y-intervals uses space within existing data structures → O(1) extra
 *
 * TOTAL SPACE COMPLEXITY: O(n)
 *
 * OPTIMIZATION (Advanced):
 * ------------------------
 * Using a Segment Tree or Interval Tree can reduce Y-union time to O(log n) per update,
 * bringing total time down to O(n log n). However, implementation becomes more complex.
 */


 class Solution2 {

     static final int MOD = 1_000_000_007;

     public int rectangleArea(int[][] rectangles) {
         // Step 1: Collect all unique x and y coordinates for compression
         Set<Integer> xSet = new TreeSet<>();
         Set<Integer> ySet = new TreeSet<>();

         for (int[] rect : rectangles) {
             xSet.add(rect[0]); // x1
             xSet.add(rect[2]); // x2
             ySet.add(rect[1]); // y1
             ySet.add(rect[3]); // y2
         }

         // Convert to sorted arrays for coordinate compression
         Integer[] xCoords = xSet.toArray(new Integer[0]);
         Integer[] yCoords = ySet.toArray(new Integer[0]);

         // Step 2: Create events for sweep line algorithm
         List<Event> events = new ArrayList<>();
         for (int[] rect : rectangles) {
             int x1 = rect[0], y1 = rect[1], x2 = rect[2], y2 = rect[3];

             // Compress coordinates to indices
             int y1Idx = Arrays.binarySearch(yCoords, y1);
             int y2Idx = Arrays.binarySearch(yCoords, y2);

             // Opening event at x1: add rectangle
             events.add(new Event(x1, y1Idx, y2Idx - 1, 1));
             // Closing event at x2: remove rectangle
             events.add(new Event(x2, y1Idx, y2Idx - 1, -1));
         }

         // Step 3: Sort events by x-coordinate
         events.sort(Comparator.comparingInt(e -> e.x));

         // Step 4: Initialize segment tree with actual y-coordinates
         SegmentTree segTree = new SegmentTree(yCoords);

         long totalArea = 0;
         int prevX = events.get(0).x;

         // Step 5: Process events with sweep line
         for (Event event : events) {
             int currX = event.x;

             // Calculate area contributed by current active rectangles
             if (currX > prevX) {
                 long activeHeight = segTree.query();
                 long width = currX - prevX;
                 totalArea = (totalArea + (activeHeight * width) % MOD) % MOD;
             }

             // Update segment tree based on event type
             segTree.update(event.y1, event.y2, event.type);
             prevX = currX;
         }

         return (int) totalArea;
     }

     // Event class for sweep line algorithm
     static class Event {
         int x; // x-coordinate of the event
         int y1, y2; // y-coordinate range (compressed indices)
         int type; // +1 for opening, -1 for closing

         Event(int x, int y1, int y2, int type) {
             this.x = x;
             this.y1 = y1;
             this.y2 = y2;
             this.type = type;
         }
     }

     // Segment Tree for efficient range updates and coverage queries
     static class SegmentTree {
         private int[] count; // Count of active rectangles covering each segment
         private long[] length; // Total length covered in each subtree
         private Integer[] yCoords; // Compressed y-coordinates
         private int n;

         // Constructor that accepts actual y-coordinates
         public SegmentTree(Integer[] yCoords) {
             this.n = yCoords.length - 1;
             this.count = new int[4 * n];
             this.length = new long[4 * n];
             this.yCoords = yCoords;
         }

         // Update range [l, r] by adding delta
         public void update(int l, int r, int delta) {
             update(1, 0, n - 1, l, r, delta);
         }

         private void update(int node, int start, int end, int l, int r, int delta) {
             if (l > end || r < start)
                 return; // No overlap

             if (l <= start && end <= r) {
                 // Complete overlap
                 count[node] += delta;
             } else {
                 // Partial overlap
                 int mid = (start + end) / 2;
                 update(2 * node, start, mid, l, r, delta);
                 update(2 * node + 1, mid + 1, end, l, r, delta);
             }

             // Update length for this node
             pushUp(node, start, end);
         }

         // Calculate total covered length
         public long query() {
             return length[1];
         }

         // Update length based on children and count
         private void pushUp(int node, int start, int end) {
             if (count[node] > 0) {
                 // This entire segment is covered
                 length[node] = yCoords[end + 1] - yCoords[start];
             } else if (start == end) {
                 // Leaf node with no coverage
                 length[node] = 0;
             } else {
                 // Internal node - sum children's lengths
                 length[node] = length[2 * node] + length[2 * node + 1];
             }
         }
     }

 }

/* 
DETAILED EXPLANATION:

1. COORDINATE COMPRESSION:
   - Collect all unique x and y coordinates from rectangles
   - Sort them to create mapping from coordinate → index
   - This reduces the coordinate space from potentially large values to manageable indices

2. SWEEP LINE ALGORITHM:
   - Process rectangles from left to right (by x-coordinate)
   - At each x-position, maintain set of active y-intervals
   - Calculate area = (covered_height × width) for each x-segment

3. SEGMENT TREE STRUCTURE:
   - Each node represents a y-coordinate range
   - count[node]: number of rectangles covering this range
   - length[node]: total y-length covered in this subtree
   
4. SEGMENT TREE OPERATIONS:
   - update(l, r, delta): add/remove rectangle covering y-range [l, r]
   - query(): return total covered y-length
   - pushUp(): recalculate length based on coverage count and children

5. EVENT PROCESSING:
   - Opening event (x1): add rectangle to active set
   - Closing event (x2): remove rectangle from active set
   - Between events: calculate area contribution

6. TIME COMPLEXITY: O(n log n) where n is number of rectangles
   - Coordinate compression: O(n log n)
   - Event processing: O(n log k) where k is number of unique coordinates
   - Overall: O(n log n)

7. SPACE COMPLEXITY: O(n) for coordinate arrays and segment tree

KEY ADVANTAGES OF SEGMENT TREE APPROACH:
- Efficient range updates in O(log n)
- Handles overlapping rectangles correctly
- Automatically computes union of intervals
- Scales well with large coordinate values through compression
*/

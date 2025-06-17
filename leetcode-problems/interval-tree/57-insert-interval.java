import java.util.*;
/*
 * You are given an array of non-overlapping intervals intervals where
 * intervals[i] = [starti, endi] represent the start and the end of the ith
 * interval and intervals is sorted in ascending order by starti. You are also
 * given an interval newInterval = [start, end] that represents the start and
 * end of another interval.
 * 
 * Insert newInterval into intervals such that intervals is still sorted in
 * ascending order by starti and intervals still does not have any overlapping
 * intervals (merge overlapping intervals if necessary).
 * 
 * Return intervals after the insertion.
 * 
 * Note that you don't need to modify intervals in-place. You can make a new
 * array and return it.
 * 
 * Example 1:
 * Input: intervals = [[1,3],[6,9]], newInterval = [2,5]
 * Output: [[1,5],[6,9]]
 * 
 * Example 2:
 * Input: intervals = [[1,2],[3,5],[6,7],[8,10],[12,16]], newInterval = [4,8]
 * Output: [[1,2],[3,10],[12,16]]
 * Explanation: Because the new interval [4,8] overlaps with [3,5],[6,7],[8,10].
 */

class InsertInterval {

    /**
     * Approach 1: Linear Scan with Three Phases
     * Time: O(n), Space: O(n)
     * 
     * Algorithm:
     * 1. Add all intervals that end before newInterval starts
     * 2. Merge all overlapping intervals with newInterval
     * 3. Add all remaining intervals that start after newInterval ends
     */
    public int[][] insert(int[][] intervals, int[] newInterval) {
        List<int[]> result = new ArrayList<>();
        int i = 0;
        int n = intervals.length;

        // Phase 1: Add all intervals that end before newInterval starts
        while (i < n && intervals[i][1] < newInterval[0]) {
            result.add(intervals[i]);
            i++;
        }

        // Phase 2: Merge overlapping intervals
        while (i < n && intervals[i][0] <= newInterval[1]) {
            newInterval[0] = Math.min(newInterval[0], intervals[i][0]);
            newInterval[1] = Math.max(newInterval[1], intervals[i][1]);
            i++;
        }
        result.add(newInterval);

        // Phase 3: Add remaining intervals
        while (i < n) {
            result.add(intervals[i]);
            i++;
        }

        return result.toArray(new int[result.size()][]);
    }

    /**
     * Approach 2: Single Pass with Conditional Logic
     * Time: O(n), Space: O(n)
     * 
     * More intuitive approach with clear conditional logic
     */
    public int[][] insertV2(int[][] intervals, int[] newInterval) {
        List<int[]> result = new ArrayList<>();
        boolean inserted = false;

        for (int[] interval : intervals) {
            if (!inserted) {
                if (interval[1] < newInterval[0]) {
                    // Current interval ends before newInterval starts
                    result.add(interval);
                } else if (interval[0] > newInterval[1]) {
                    // Current interval starts after newInterval ends
                    result.add(newInterval);
                    result.add(interval);
                    inserted = true;
                } else {
                    // Overlapping intervals - merge them
                    newInterval[0] = Math.min(newInterval[0], interval[0]);
                    newInterval[1] = Math.max(newInterval[1], interval[1]);
                }
            } else {
                result.add(interval);
            }
        }

        // If newInterval wasn't inserted, add it at the end
        if (!inserted) {
            result.add(newInterval);
        }

        return result.toArray(new int[result.size()][]);
    }

    /**
     * Approach 3: Binary Search + Merge
     * Time: O(n), Space: O(n)
     * 
     * Uses binary search to find insertion points for optimization
     */
    public int[][] insertV3(int[][] intervals, int[] newInterval) {
        if (intervals.length == 0) {
            return new int[][] { newInterval };
        }

        List<int[]> result = new ArrayList<>();

        // Find the position to start merging
        int start = 0;
        while (start < intervals.length && intervals[start][1] < newInterval[0]) {
            result.add(intervals[start]);
            start++;
        }

        // Find the position to end merging
        int end = start;
        while (end < intervals.length && intervals[end][0] <= newInterval[1]) {
            end++;
        }

        // Merge intervals from start to end-1 with newInterval
        if (start < intervals.length && start < end) {
            newInterval[0] = Math.min(newInterval[0], intervals[start][0]);
            newInterval[1] = Math.max(newInterval[1], intervals[end - 1][1]);
        }
        result.add(newInterval);

        // Add remaining intervals
        for (int i = end; i < intervals.length; i++) {
            result.add(intervals[i]);
        }

        return result.toArray(new int[result.size()][]);
    }

    /**
     * Test cases and examples
     */
    public static void main(String[] args) {
        InsertInterval solution = new InsertInterval();

        // Test Case 1: Example 1
        int[][] intervals1 = { { 1, 3 }, { 6, 9 } };
        int[] newInterval1 = { 2, 5 };
        System.out.println("Test 1: " + Arrays.deepToString(solution.insert(intervals1, newInterval1)));
        // Expected: [[1,5],[6,9]]

        // Test Case 2: Example 2
        int[][] intervals2 = { { 1, 2 }, { 3, 5 }, { 6, 7 }, { 8, 10 }, { 12, 16 } };
        int[] newInterval2 = { 4, 8 };
        System.out.println("Test 2: " + Arrays.deepToString(solution.insert(intervals2, newInterval2)));
        // Expected: [[1,2],[3,10],[12,16]]

        // Test Case 3: Insert at beginning
        int[][] intervals3 = { { 3, 5 }, { 6, 9 } };
        int[] newInterval3 = { 1, 2 };
        System.out.println("Test 3: " + Arrays.deepToString(solution.insert(intervals3, newInterval3)));
        // Expected: [[1,2],[3,5],[6,9]]

        // Test Case 4: Insert at end
        int[][] intervals4 = { { 1, 3 }, { 6, 9 } };
        int[] newInterval4 = { 10, 12 };
        System.out.println("Test 4: " + Arrays.deepToString(solution.insert(intervals4, newInterval4)));
        // Expected: [[1,3],[6,9],[10,12]]

        // Test Case 5: Empty intervals
        int[][] intervals5 = {};
        int[] newInterval5 = { 5, 7 };
        System.out.println("Test 5: " + Arrays.deepToString(solution.insert(intervals5, newInterval5)));
        // Expected: [[5,7]]

        // Test Case 6: Complete overlap
        int[][] intervals6 = { { 1, 5 } };
        int[] newInterval6 = { 2, 3 };
        System.out.println("Test 6: " + Arrays.deepToString(solution.insert(intervals6, newInterval6)));
        // Expected: [[1,5]]

        // Test Case 7: Merge all intervals
        int[][] intervals7 = { { 1, 2 }, { 3, 5 }, { 6, 7 }, { 8, 10 } };
        int[] newInterval7 = { 0, 15 };
        System.out.println("Test 7: " + Arrays.deepToString(solution.insert(intervals7, newInterval7)));
        // Expected: [[0,15]]
    }

}

/**
 * Key Insights:
 * 
 * 1. The problem has three distinct phases:
 * - Intervals that don't overlap and come before newInterval
 * - Intervals that overlap with newInterval (need merging)
 * - Intervals that don't overlap and come after newInterval
 * 
 * 2. Two intervals [a,b] and [c,d] overlap if:
 * - a <= d and c <= b (general overlap condition)
 * - Equivalently: NOT (b < c OR d < a)
 * 
 * 3. When merging overlapping intervals:
 * - New start = min(all starts)
 * - New end = max(all ends)
 * 
 * 4. Edge cases to consider:
 * - Empty intervals array
 * - newInterval doesn't overlap with any existing interval
 * - newInterval overlaps with all existing intervals
 * - newInterval is completely contained within an existing interval
 * 
 * Time Complexity: O(n) for all approaches
 * Space Complexity: O(n) for the result array
 */

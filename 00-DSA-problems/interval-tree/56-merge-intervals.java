import java.util.*;
/*
 * Given an array of intervals where intervals[i] = [starti, endi], merge all
 * overlapping intervals, and return an array of the non-overlapping intervals
 * that cover all the intervals in the input.
 * 
 * Example 1:
 * Input: intervals = [[1,3],[2,6],[8,10],[15,18]]
 * Output: [[1,6],[8,10],[15,18]]
 * Explanation: Since intervals [1,3] and [2,6] overlap, merge them into [1,6].
 * 
 * Example 2:
 * Input: intervals = [[1,4],[4,5]]
 * Output: [[1,5]]
 * Explanation: Intervals [1,4] and [4,5] are considered overlapping.
 */

class MergeIntervals {

    /**
     * Solution 1: Sort and Merge (Most Common Approach)
     * Time Complexity: O(n log n) due to sorting
     * Space Complexity: O(1) excluding output array
     */
    public int[][] merge1(int[][] intervals) {
        if (intervals == null || intervals.length <= 1) {
            return intervals;
        }

        // Sort intervals by start time
        Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));

        List<int[]> merged = new ArrayList<>();
        int[] currentInterval = intervals[0];
        merged.add(currentInterval);

        for (int i = 1; i < intervals.length; i++) {
            int[] nextInterval = intervals[i];

            // Check if intervals overlap
            if (currentInterval[1] >= nextInterval[0]) {
                // Merge intervals by extending the end time
                currentInterval[1] = Math.max(currentInterval[1], nextInterval[1]);
            } else {
                // No overlap, add new interval
                currentInterval = nextInterval;
                merged.add(currentInterval);
            }
        }

        return merged.toArray(new int[merged.size()][]);
    }

    /**
     * Solution 2: Using LinkedList for better insertion performance
     * Time Complexity: O(n log n) due to sorting
     * Space Complexity: O(1) excluding output array
     */
    public int[][] merge2(int[][] intervals) {
        if (intervals == null || intervals.length <= 1) {
            return intervals;
        }

        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        LinkedList<int[]> merged = new LinkedList<>();

        for (int[] interval : intervals) {
            // If list is empty or no overlap with the last interval
            if (merged.isEmpty() || merged.getLast()[1] < interval[0]) {
                merged.add(interval);
            } else {
                // Merge with the last interval
                merged.getLast()[1] = Math.max(merged.getLast()[1], interval[1]);
            }
        }

        return merged.toArray(new int[merged.size()][]);
    }

    /**
     * Solution 3: In-place sorting with two pointers
     * Time Complexity: O(n log n) due to sorting
     * Space Complexity: O(1) excluding output array
     */
    public int[][] merge3(int[][] intervals) {
        if (intervals == null || intervals.length <= 1) {
            return intervals;
        }

        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        int writeIndex = 0;

        for (int readIndex = 1; readIndex < intervals.length; readIndex++) {
            // Check if current interval overlaps with the last merged interval
            if (intervals[writeIndex][1] >= intervals[readIndex][0]) {
                // Merge intervals
                intervals[writeIndex][1] = Math.max(intervals[writeIndex][1], intervals[readIndex][1]);
            } else {
                // No overlap, move to next position
                writeIndex++;
                intervals[writeIndex] = intervals[readIndex];
            }
        }

        return Arrays.copyOf(intervals, writeIndex + 1);
    }

    /**
     * Solution 4: Using custom comparator and stream API
     * Time Complexity: O(n log n) due to sorting
     * Space Complexity: O(n) for stream operations
     */
    // public int[][] merge4(int[][] intervals) {
    //     if (intervals == null || intervals.length <= 1) {
    //         return intervals;
    //     }

    //     // Sort by start time
    //     Arrays.sort(intervals, Comparator.comparingInt(interval -> interval[0]));

    //     return Arrays.stream(intervals)
    //             .collect(ArrayList::new,
    //                     (list, interval) -> {
    //                         if (list.isEmpty() || list.get(list.size() - 1)[1] < interval[0]) {
    //                             list.add(interval);
    //                         } else {
    //                             list.get(list.size() - 1)[1] = Math.max(list.get(list.size() - 1)[1], interval[1]);
    //                         }
    //                     },
    //                     (list1, list2) -> {
    //                         list1.addAll(list2);
    //                         return list1;
    //                     })
    //             .toArray(new int[0][]);
    // }

    /**
     * Solution 5: Handling edge cases and integer overflow
     * Time Complexity: O(n log n) due to sorting
     * Space Complexity: O(1) excluding output array
     */
    public int[][] merge5(int[][] intervals) {
        // Handle null or empty input
        if (intervals == null || intervals.length == 0) {
            return new int[0][];
        }

        if (intervals.length == 1) {
            return intervals;
        }

        // Sort intervals by start time, handle potential overflow
        Arrays.sort(intervals, (a, b) -> {
            if (a[0] != b[0]) {
                return Integer.compare(a[0], b[0]);
            }
            return Integer.compare(a[1], b[1]);
        });

        List<int[]> result = new ArrayList<>();
        int[] current = new int[] { intervals[0][0], intervals[0][1] };

        for (int i = 1; i < intervals.length; i++) {
            int[] next = intervals[i];

            // Check for overlap (including touching intervals)
            if (current[1] >= next[0]) {
                // Merge intervals - handle potential overflow
                current[1] = Math.max(current[1], next[1]);
            } else {
                // No overlap, add current interval to result
                result.add(current);
                current = new int[] { next[0], next[1] };
            }
        }

        // Add the last interval
        result.add(current);

        return result.toArray(new int[result.size()][]);
    }

    /**
     * Solution 6: Alternative approach without sorting (for special cases)
     * Time Complexity: O(n²) in worst case
     * Space Complexity: O(n)
     * Note: Only efficient when intervals are mostly non-overlapping
     */
    public int[][] mergeWithoutSort(int[][] intervals) {
        if (intervals == null || intervals.length <= 1) {
            return intervals;
        }

        List<int[]> result = new ArrayList<>();
        boolean[] merged = new boolean[intervals.length];

        for (int i = 0; i < intervals.length; i++) {
            if (merged[i])
                continue;

            int[] current = new int[] { intervals[i][0], intervals[i][1] };
            merged[i] = true;

            // Try to merge with all other intervals
            boolean foundMerge = true;
            while (foundMerge) {
                foundMerge = false;
                for (int j = 0; j < intervals.length; j++) {
                    if (merged[j])
                        continue;

                    // Check if intervals overlap
                    if (!(current[1] < intervals[j][0] || intervals[j][1] < current[0])) {
                        current[0] = Math.min(current[0], intervals[j][0]);
                        current[1] = Math.max(current[1], intervals[j][1]);
                        merged[j] = true;
                        foundMerge = true;
                    }
                }
            }

            result.add(current);
        }

        return result.toArray(new int[result.size()][]);
    }

    /**
     * Helper method to print intervals
     */
    private static void printIntervals(int[][] intervals) {
        System.out.print("[");
        for (int i = 0; i < intervals.length; i++) {
            System.out.print("[" + intervals[i][0] + "," + intervals[i][1] + "]");
            if (i < intervals.length - 1)
                System.out.print(",");
        }
        System.out.println("]");
    }

    // Test cases
    public static void main(String[] args) {
        MergeIntervals solution = new MergeIntervals();

        // Test case 1
        int[][] intervals1 = { { 1, 3 }, { 2, 6 }, { 8, 10 }, { 15, 18 } };
        System.out.println("Test Case 1:");
        System.out.print("Input: ");
        printIntervals(intervals1);
        System.out.print("Output: ");
        printIntervals(solution.merge1(intervals1));
        System.out.println("Expected: [[1,6],[8,10],[15,18]]");
        System.out.println();

        // Test case 2
        int[][] intervals2 = { { 1, 4 }, { 4, 5 } };
        System.out.println("Test Case 2:");
        System.out.print("Input: ");
        printIntervals(intervals2);
        System.out.print("Output: ");
        printIntervals(solution.merge1(intervals2));
        System.out.println("Expected: [[1,5]]");
        System.out.println();

        // Test case 3: Single interval
        int[][] intervals3 = { { 1, 4 } };
        System.out.println("Test Case 3:");
        System.out.print("Input: ");
        printIntervals(intervals3);
        System.out.print("Output: ");
        printIntervals(solution.merge1(intervals3));
        System.out.println("Expected: [[1,4]]");
        System.out.println();

        // Test case 4: No overlapping intervals
        int[][] intervals4 = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        System.out.println("Test Case 4:");
        System.out.print("Input: ");
        printIntervals(intervals4);
        System.out.print("Output: ");
        printIntervals(solution.merge1(intervals4));
        System.out.println("Expected: [[1,2],[3,4],[5,6]]");
        System.out.println();

        // Test case 5: All overlapping
        int[][] intervals5 = { { 1, 4 }, { 2, 5 }, { 3, 6 } };
        System.out.println("Test Case 5:");
        System.out.print("Input: ");
        printIntervals(intervals5);
        System.out.print("Output: ");
        printIntervals(solution.merge1(intervals5));
        System.out.println("Expected: [[1,6]]");
        System.out.println();

        // Test case 6: Unsorted intervals
        int[][] intervals6 = { { 2, 6 }, { 1, 3 }, { 8, 10 }, { 15, 18 } };
        System.out.println("Test Case 6:");
        System.out.print("Input: ");
        printIntervals(intervals6);
        System.out.print("Output: ");
        printIntervals(solution.merge1(intervals6));
        System.out.println("Expected: [[1,6],[8,10],[15,18]]");
        System.out.println();

        // Test case 7: Empty array
        int[][] intervals7 = {};
        System.out.println("Test Case 7:");
        System.out.print("Input: ");
        printIntervals(intervals7);
        System.out.print("Output: ");
        printIntervals(solution.merge1(intervals7));
        System.out.println("Expected: []");
    }

}

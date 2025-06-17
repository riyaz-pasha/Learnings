import java.util.*;
/*
 * There are some spherical balloons taped onto a flat wall that represents the
 * XY-plane. The balloons are represented as a 2D integer array points where
 * points[i] = [xstart, xend] denotes a balloon whose horizontal diameter
 * stretches between xstart and xend. You do not know the exact y-coordinates of
 * the balloons.
 * 
 * Arrows can be shot up directly vertically (in the positive y-direction) from
 * different points along the x-axis. A balloon with xstart and xend is burst by
 * an arrow shot at x if xstart <= x <= xend. There is no limit to the number of
 * arrows that can be shot. A shot arrow keeps traveling up infinitely, bursting
 * any balloons in its path.
 * 
 * Given the array points, return the minimum number of arrows that must be shot
 * to burst all balloons.
 * 
 * Example 1:
 * Input: points = [[10,16],[2,8],[1,6],[7,12]]
 * Output: 2
 * Explanation: The balloons can be burst by 2 arrows:
 * - Shoot an arrow at x = 6, bursting the balloons [2,8] and [1,6].
 * - Shoot an arrow at x = 11, bursting the balloons [10,16] and [7,12].
 * 
 * Example 2:
 * Input: points = [[1,2],[3,4],[5,6],[7,8]]
 * Output: 4
 * Explanation: One arrow needs to be shot for each balloon for a total of 4
 * arrows.
 * 
 * Example 3:
 * Input: points = [[1,2],[2,3],[3,4],[4,5]]
 * Output: 2
 * Explanation: The balloons can be burst by 2 arrows:
 * - Shoot an arrow at x = 2, bursting the balloons [1,2] and [2,3].
 * - Shoot an arrow at x = 4, bursting the balloons [3,4] and [4,5].
 */

class Solution {

    /**
     * Approach 1: Greedy Algorithm with Sorting by End Points
     * Time Complexity: O(n log n) - due to sorting
     * Space Complexity: O(1) - only using constant extra space
     */
    public int findMinArrowShots(int[][] points) {
        if (points == null || points.length == 0) {
            return 0;
        }

        // Sort balloons by their end points
        Arrays.sort(points, (a, b) -> Integer.compare(a[1], b[1]));

        int arrows = 1;
        int arrowPos = points[0][1]; // Position the first arrow at the end of first balloon

        for (int i = 1; i < points.length; i++) {
            // If current balloon's start is after the arrow position,
            // we need a new arrow
            if (points[i][0] > arrowPos) {
                arrows++;
                arrowPos = points[i][1]; // Position new arrow at end of current balloon
            }
        }

        return arrows;
    }

    /**
     * Approach 2: Alternative Greedy with Overlap Tracking
     * Time Complexity: O(n log n)
     * Space Complexity: O(1)
     */
    public int findMinArrowShots2(int[][] points) {
        if (points == null || points.length == 0) {
            return 0;
        }

        // Sort by end points
        Arrays.sort(points, (a, b) -> Integer.compare(a[1], b[1]));

        int arrows = 0;
        int i = 0;

        while (i < points.length) {
            arrows++;
            int currentEnd = points[i][1];

            // Skip all balloons that can be burst by current arrow
            while (i < points.length && points[i][0] <= currentEnd) {
                i++;
            }
        }

        return arrows;
    }

    /**
     * Approach 3: Using Interval Merging Concept
     * Time Complexity: O(n log n)
     * Space Complexity: O(1)
     */
    public int findMinArrowShots3(int[][] points) {
        if (points == null || points.length == 0) {
            return 0;
        }

        // Sort by start points
        Arrays.sort(points, (a, b) -> Integer.compare(a[0], b[0]));

        int arrows = 1;
        int end = points[0][1];

        for (int i = 1; i < points.length; i++) {
            if (points[i][0] <= end) {
                // Overlapping balloons - update the common end to minimum
                end = Math.min(end, points[i][1]);
            } else {
                // No overlap - need new arrow
                arrows++;
                end = points[i][1];
            }
        }

        return arrows;
    }

    // Test cases
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int[][] points1 = { { 10, 16 }, { 2, 8 }, { 1, 6 }, { 7, 12 } };
        System.out.println("Test 1: " + solution.findMinArrowShots(points1)); // Expected: 2

        // Test case 2
        int[][] points2 = { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } };
        System.out.println("Test 2: " + solution.findMinArrowShots(points2)); // Expected: 4

        // Test case 3
        int[][] points3 = { { 1, 2 }, { 2, 3 }, { 3, 4 }, { 4, 5 } };
        System.out.println("Test 3: " + solution.findMinArrowShots(points3)); // Expected: 2

        // Edge cases
        int[][] points4 = { { 1, 10 } };
        System.out.println("Test 4 (single balloon): " + solution.findMinArrowShots(points4)); // Expected: 1

        int[][] points5 = { { 1, 5 }, { 1, 5 }, { 1, 5 } };
        System.out.println("Test 5 (identical balloons): " + solution.findMinArrowShots(points5)); // Expected: 1

        // Test with negative coordinates
        int[][] points6 = { { -10, -5 }, { -7, -2 }, { -3, 1 }, { 0, 5 } };
        System.out.println("Test 6 (negative coords): " + solution.findMinArrowShots(points6)); // Expected: 3
    }

}

/**
 * EXPLANATION:
 * 
 * The problem is essentially about finding the minimum number of points that
 * can
 * "cover" all given intervals. This is a classic greedy algorithm problem.
 * 
 * Key Insights:
 * 1. If we shoot an arrow at position x, it will burst all balloons whose
 * intervals
 * contain x (i.e., xstart <= x <= xend).
 * 2. To minimize arrows, we want each arrow to burst as many balloons as
 * possible.
 * 3. The optimal strategy is to always shoot at the rightmost position that
 * still
 * covers the current balloon.
 * 
 * Algorithm (Approach 1 - Recommended):
 * 1. Sort balloons by their end points
 * 2. Shoot first arrow at the end of the first balloon
 * 3. For each subsequent balloon:
 * - If its start is after the current arrow position, shoot a new arrow
 * - Otherwise, the current arrow already covers this balloon
 * 
 * Why sort by end points?
 * - By shooting at the end of the earliest-ending balloon, we maximize the
 * chance
 * of hitting subsequent balloons
 * - This greedy choice is optimal because any arrow position to the right would
 * not hit the current balloon, and any position to the left might miss future
 * balloons
 * 
 * Time Complexity: O(n log n) due to sorting
 * Space Complexity: O(1) excluding the input array
 */

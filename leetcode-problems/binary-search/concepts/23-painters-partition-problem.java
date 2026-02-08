/*
 * Problem Statement: Given an array/list of length ‘N’, where the array/list
 * represents the boards and each element of the given array/list represents the
 * length of each board. Some ‘K’ numbers of painters are available to paint
 * these boards. Consider that each unit of a board takes 1 unit of time to
 * paint. You are supposed to return the area of the minimum time to get this
 * job done of painting all the ‘N’ boards under the constraint that any painter
 * will only paint the continuous sections of boards.
 * 
 * Example 1:
 * Input Format: N = 4, boards[] = {5, 5, 5, 5}, k = 2
 * Result: 10
 * Explanation: We can divide the boards into 2 equal-sized partitions, so each
 * painter gets 10 units of the board and the total time taken is 10.
 * 
 * Example 2:
 * Input Format: N = 4, boards[] = {10, 20, 30, 40}, k = 2
 * Result: 60
 * Explanation: We can divide the first 3 boards for one painter and the last
 * board for the second painter.
 */

import java.util.Collections;
import java.util.List;

class PaintersPartitionProblem {

    public int findLargestMinDistance(List<Integer> boards, int numberOfPainters) {
        if (numberOfPainters > boards.size()) {
            return -1;
        }

        int low = Collections.max(boards);
        int high = boards.stream().mapToInt(Integer::intValue).sum();

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int paintersNeeded = this.getPainterNeededCount(boards, mid);
            if (paintersNeeded > numberOfPainters) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    private int getPainterNeededCount(List<Integer> boards, int maxTime) {
        int paintersNeeded = 1;
        long timeTakenByThePainter = 0;
        for (int boardLen : boards) {
            if (timeTakenByThePainter + boardLen <= maxTime) {
                timeTakenByThePainter += boardLen;
            } else {
                paintersNeeded++;
                timeTakenByThePainter = boardLen;
            }
        }
        return paintersNeeded;
    }

}

class PainterPartition {

    /**
     * Problem:
     * Given boards[] where boards[i] = length/time of board i.
     * We have k painters.
     *
     * Rules:
     * - Each painter paints contiguous boards only.
     * - A board cannot be split between painters.
     *
     * Goal:
     * Minimize the maximum time taken by any painter.
     *
     * Example:
     * boards = [10, 20, 30, 40], k = 2
     *
     * Best partition:
     *   Painter1: [10,20,30] -> 60
     *   Painter2: [40]       -> 40
     *
     * Answer = 60
     *
     * ---------------------------------------------------------
     * Why Binary Search on Answer?
     *
     * We want the MINIMUM possible maxTime.
     *
     * If a maxTime = X is feasible (we can paint with k painters),
     * then any larger maxTime (X+1, X+2, ...) is also feasible.
     *
     * Feasibility is monotonic:
     *   false false false true true true ...
     *
     * So we binary search for the smallest feasible maxTime.
     *
     * ---------------------------------------------------------
     * Search Space:
     * low  = max(boards)  (a painter must at least paint the largest board)
     * high = sum(boards)  (one painter paints everything)
     *
     * Time Complexity:
     *   O(n log(sum(boards)))
     *
     * Space Complexity:
     *   O(1)
     */
    public int findMinTime(int[] boards, int k) {

        int low = 0;   // minimum possible answer
        int high = 0;  // maximum possible answer

        // Compute search boundaries
        for (int board : boards) {
            low = Math.max(low, board); // cannot be less than largest board
            high += board;              // one painter paints everything
        }

        int result = -1;

        // Binary search for FIRST feasible maxTime
        while (low <= high) {

            int mid = low + (high - low) / 2;

            // Can we paint all boards with maxTime = mid?
            if (isPossible(boards, k, mid)) {

                // mid works, store as candidate answer
                result = mid;

                // try smaller maxTime (minimize answer)
                high = mid - 1;
            } else {

                // mid does not work, need more allowed time
                low = mid + 1;
            }
        }

        return result;
    }

    /**
     * Feasibility Check:
     * Can we paint all boards using <= k painters such that
     * no painter paints more than maxTime?
     *
     * Greedy Approach:
     * - Assign boards in order to current painter.
     * - If adding a board exceeds maxTime, assign it to next painter.
     *
     * This greedy strategy uses the minimum number of painters
     * possible for the given maxTime.
     *
     * Example:
     * boards = [10,20,30,40], maxTime = 50
     *
     * Painter1: 10+20 = 30
     * add 30 -> would be 60 > 50 => new painter
     * Painter2: 30
     * add 40 -> would be 70 > 50 => new painter
     * Painter3: 40
     *
     * painters needed = 3
     *
     * If k=2 => NOT possible.
     *
     * Time Complexity: O(n)
     */
    public boolean isPossible(int[] boards, int k, int maxTime) {

        int painterCount = 1; // start with first painter
        int currentTime = 0;  // time assigned to current painter

        for (int board : boards) {

            // If a single board itself is bigger than maxTime,
            // then it is impossible to assign.
            if (board > maxTime) {
                return false;
            }

            // If current painter cannot take this board, assign to next painter
            if (currentTime + board > maxTime) {
                painterCount++;
                currentTime = board; // new painter starts with this board

                // If painters exceed k, not feasible
                if (painterCount > k) {
                    return false;
                }
            } else {
                // assign board to current painter
                currentTime += board;
            }
        }

        return true;
    }
}


/*
 * Time Complexity: O(N * log(sum(arr[])-max(arr[])+1)), where N = size of the
 * array, sum(arr[]) = sum of all array elements, max(arr[]) = maximum of all
 * array elements.
 * Reason: We are applying binary search on [max(arr[]), sum(arr[])]. Inside the
 * loop, we are calling the countPainters() function for the value of ‘mid’.
 * Now, inside the countPainters() function, we are using a loop that runs for N
 * times.
 * 
 * Space Complexity: O(1) as we are not using any extra space to solve this
 * problem.
 */

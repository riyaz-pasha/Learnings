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

    public static boolean isPossible(int[] boards, int k, int maxTime) {
        int painterCount = 1;
        int currentTime = 0;

        for (int board : boards) {
            if (board > maxTime)
                return false;

            if (currentTime + board > maxTime) {
                painterCount++;
                currentTime = board;

                if (painterCount > k)
                    return false;
            } else {
                currentTime += board;
            }
        }

        return true;
    }

    public static int findMinTime(int[] boards, int k) {
        int low = 0, high = 0;
        for (int board : boards) {
            low = Math.max(low, board);
            high += board;
        }

        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (isPossible(boards, k, mid)) {
                result = mid; // try to minimize
                high = mid - 1;
            } else {
                low = mid + 1; // need more time
            }
        }

        return result;
    }

    public static void main(String[] args) {
        int[] boards1 = { 5, 5, 5, 5 };
        int k1 = 2;
        System.out.println(findMinTime(boards1, k1)); // Output: 10

        int[] boards2 = { 10, 20, 30, 40 };
        int k2 = 2;
        System.out.println(findMinTime(boards2, k2)); // Output: 60
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

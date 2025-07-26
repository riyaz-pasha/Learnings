/*
 * Given an integer array arr and a target value target, return the integer
 * value such that when we change all the integers larger than value in the
 * given array to be equal to value, the sum of the array gets as close as
 * possible (in absolute difference) to target.
 * 
 * In case of a tie, return the minimum such integer.
 * 
 * Notice that the answer is not neccesarilly a number from arr.
 * 
 * 
 * 
 * Example 1:
 * 
 * Input: arr = [4,9,3], target = 10
 * Output: 3
 * Explanation: When using 3 arr converts to [3, 3, 3] which sums 9 and that's
 * the optimal answer.
 * Example 2:
 * 
 * Input: arr = [2,3,5], target = 10
 * Output: 5
 * Example 3:
 * 
 * Input: arr = [60864,25176,27249,21296,20204], target = 56803
 * Output: 11361
 */

import java.util.Arrays;

class ArrayValueClosestToTarget {

    public int findBestValue(int[] arr, int target) {
        int low = 0, high = Arrays.stream(arr).max().getAsInt();
        int bestVal = 0;
        int minDiff = Integer.MAX_VALUE;

        while (low <= high) {
            int mid = (low + high) / 2;
            int sum = computeSum(arr, mid);
            int diff = Math.abs(sum - target);

            if (diff < minDiff || (diff == minDiff && mid < bestVal)) {
                minDiff = diff;
                bestVal = mid;
            }

            if (sum < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return bestVal;
    }

    private int computeSum(int[] arr, int value) {
        int sum = 0;
        for (int num : arr) {
            sum += Math.min(num, value);
        }
        return sum;
    }

    // Test it
    public static void main(String[] args) {
        ArrayValueClosestToTarget solver = new ArrayValueClosestToTarget();

        System.out.println(solver.findBestValue(new int[] { 4, 9, 3 }, 10)); // Output: 3
        System.out.println(solver.findBestValue(new int[] { 2, 3, 5 }, 10)); // Output: 5
        System.out.println(solver.findBestValue(new int[] { 60864, 25176, 27249, 21296, 20204 }, 56803)); // Output:
                                                                                                          // 11361
    }

}

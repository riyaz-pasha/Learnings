
import java.util.Arrays;

/*
 * Problem Statement: You are given an array 'arr' of size 'n' which denotes the
 * position of stalls.
 * You are also given an integer 'k' which denotes the number of aggressive
 * cows.
 * You are given the task of assigning stalls to 'k' cows such that the minimum
 * distance between any two of them is the maximum possible.
 * Find the maximum possible minimum distance.
 * 
 * Examples
 * 
 * Example 1:
 * Input Format: N = 6, k = 4, arr[] = {0,3,4,7,10,9}
 * Result: 3
 * Explanation: The maximum possible minimum distance between any two cows will
 * be 3 when 4 cows are placed at positions {0, 3, 7, 10}. Here the distances
 * between cows are 3, 4, and 3 respectively. We cannot make the minimum
 * distance greater than 3 in any ways.
 * 
 * Example 2:
 * Input Format: N = 5, k = 2, arr[] = {4,2,1,3,6}
 * Result: 5
 * Explanation: The maximum possible minimum distance between any two cows will
 * be 5 when 2 cows are placed at positions {1, 6}.
 */


class AggressiveCows {

    /**
     * Problem:
     * We have stall positions on a line.
     * Place k cows in stalls such that the MINIMUM distance between any two cows
     * is maximized.
     *
     * Example:
     * stalls = [1,2,4,8,9], k = 3
     * Best placement: 1,4,8 -> min distance = 3
     *
     * ---------------------------------------------------------
     * Why Binary Search on Answer?
     *
     * We are asked to MAXIMIZE the minimum distance.
     *
     * If we can place k cows with minimum distance = D,
     * then we can also place them with any smaller distance:
     *
     *     D works => (D-1) works => (D-2) works ...
     *
     * So feasibility is monotonic:
     *
     *   true true true true false false false
     *
     * (When distance becomes too large, placement becomes impossible.)
     *
     * That monotonic behavior means we can binary search on distance.
     *
     * ---------------------------------------------------------
     * Search Space:
     * low = 1 (minimum possible distance)
     * high = stalls[n-1] - stalls[0] (max possible distance)
     *
     * ---------------------------------------------------------
     * Time Complexity:
     * Sorting: O(n log n)
     * Binary search: O(log(maxDistance))
     * Feasibility check each time: O(n)
     *
     * Total: O(n log n + n log(maxDistance))
     *
     * Space Complexity: O(1) (ignoring sorting internals)
     */
    public int aggressiveCows(int[] stalls, int k) {

        Arrays.sort(stalls);

        int n = stalls.length;

        int low = 1;
        int high = stalls[n - 1] - stalls[0];

        // We want the maximum distance that is feasible.
        // This is "last true" binary search.
        while (low <= high) {

            int mid = low + (high - low) / 2;

            // Check if we can place k cows with minimum distance = mid
            if (canWePlace(stalls, mid, k)) {

                // mid is possible, so try for a bigger distance
                low = mid + 1;
            } else {

                // mid is too large, reduce distance
                high = mid - 1;
            }
        }

        // high will be the largest feasible distance
        return high;
    }

    /**
     * Feasibility function:
     * Can we place "cows" cows such that each cow is at least "distance" apart?
     *
     * Greedy placement strategy:
     * - Place first cow at the first stall.
     * - For each next stall, place cow only if it is at least "distance"
     *   away from the last placed cow.
     *
     * This greedy method works because:
     * placing cows as early as possible maximizes the chance of placing more cows.
     *
     * Example:
     * stalls = [1,2,4,8,9], distance = 3
     *
     * place at 1
     * skip 2 (too close)
     * place at 4 (4-1 >= 3)
     * place at 8 (8-4 >= 3)
     * => 3 cows placed -> possible
     *
     * Time Complexity: O(n)
     */
    private boolean canWePlace(int[] stalls, int distance, int cows) {

        int cowsPlaced = 1;           // place first cow at first stall
        int lastPosition = stalls[0]; // position of last placed cow

        for (int i = 1; i < stalls.length; i++) {

            // If current stall is far enough, place cow here
            if (stalls[i] - lastPosition >= distance) {
                cowsPlaced++;
                lastPosition = stalls[i];
            }

            // If we placed all cows successfully, return true
            if (cowsPlaced >= cows) {
                return true;
            }
        }

        return false;
    }
}


class AggressiveCowsCount {

    public static boolean canPlaceCows(int[] stalls, int k, int minDist) {
        int count = 1; // First cow is placed at the first stall
        int lastPos = stalls[0];

        for (int i = 1; i < stalls.length; i++) {
            if (stalls[i] - lastPos >= minDist) {
                count++;
                lastPos = stalls[i];
            }

            if (count >= k)
                return true;
        }

        return false;
    }

    public static int maxMinDistance(int[] stalls, int k) {
        Arrays.sort(stalls);

        int low = 1; // Minimum possible distance
        int high = stalls[stalls.length - 1] - stalls[0]; // Max possible distance
        int ans = 0;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (canPlaceCows(stalls, k, mid)) {
                ans = mid; // Try for more distance
                low = mid + 1;
            } else {
                high = mid - 1; // Reduce distance
            }
        }

        return ans;
    }

    public static void main(String[] args) {
        int[] arr1 = { 0, 3, 4, 7, 10, 9 };
        int k1 = 4;
        System.out.println(maxMinDistance(arr1, k1)); // Output: 3

        int[] arr2 = { 4, 2, 1, 3, 6 };
        int k2 = 2;
        System.out.println(maxMinDistance(arr2, k2)); // Output: 5
    }

}

/*
 * Algorithm:
 * 
 * First, we will sort the given stalls[] array.
 * 
 * Place the 2 pointers i.e. low and high: Initially, we will place the
 * pointers. The pointer low will point to 1 and the high will point to
 * (stalls[n-1]-stalls[0]). As the ‘stalls[]’ is sorted, ‘stalls[n-1]’ refers to
 * the maximum, and ‘stalls[0]’ is the minimum element.
 * 
 * Calculate the ‘mid’: Now, inside the loop, we will calculate the value of
 * ‘mid’ using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * 
 * Eliminate the halves based on the boolean value returned by canWePlace():
 * We will pass the potential distance, represented by the variable 'mid', to
 * the ‘canWePlace()' function. This function will return true if it is possible
 * to place all the cows with a minimum distance of ‘mid’.
 * 
 * - If the returned value is true: On satisfying this condition, we can
 * conclude
 * that the number ‘mid’ is one of our possible answers. But we want the maximum
 * number. So, we will eliminate the left half and consider the right half(i.e.
 * low = mid+1).
 * 
 * - Otherwise, the value mid is greater than the distance we want. This means
 * the
 * numbers greater than ‘mid’ should not be considered and the right half of
 * ‘mid’ consists of such numbers. So, we will eliminate the right half and
 * consider the left half(i.e. high = mid-1).
 * 
 * Finally, outside the loop, we will return the value of high as the pointer
 * will be pointing to the answer.
 * 
 * Time Complexity: O(NlogN) + O(N * log(max(stalls[])-min(stalls[]))), where N
 * = size of the array, max(stalls[]) = maximum element in stalls[] array,
 * min(stalls[]) = minimum element in stalls[] array.
 * Reason: O(NlogN) for sorting the array. We are applying binary search on [1,
 * max(stalls[])-min(stalls[])]. Inside the loop, we are calling canWePlace()
 * function for each distance, ‘mid’. Now, inside the canWePlace() function, we
 * are using a loop that runs for N times.
 * 
 * Space Complexity: O(1) as we are not using any extra space to solve this
 * problem.
 */

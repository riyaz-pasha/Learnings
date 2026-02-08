
import java.util.Arrays;

/*
 * Problem Statement: You are the owner of a Shipment company. You use conveyor
 * belts to ship packages from one port to another. The packages must be shipped
 * within 'd' days.
 * The weights of the packages are given in an array 'of weights'. The packages
 * are loaded on the conveyor belts every day in the same order as they appear
 * in the array. The loaded weights must not exceed the maximum weight capacity
 * of the ship.
 * Find out the least-weight capacity so that you can ship all the packages
 * within 'd' days.
 */

class CapacityToShipPackagesWithinDDays {

    public int leastWeightCapacity(int[] weights, int numberOfDays) {
        int low = 1, high = Arrays.stream(weights).max().getAsInt();
        int result = high;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int totalDays = this.getDaysRequired(weights, mid);
            if (totalDays <= numberOfDays) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return result;
    }

    private int getDaysRequired(int[] weights, int capacity) {
        int totalDays = 1; // First day.
        int load = 0;
        for (int weight : weights) {
            if (load + weight > capacity) {
                totalDays++; // move to next day
                load = weight; // load the weight.
            } else {
                // load the weight on the same day.
                load += weight;
            }
        }

        return totalDays;
    }

    public int leastWeightCapacity2(int[] weights, int numberOfDays) {
        int low = 1, high = Arrays.stream(weights).max().getAsInt();

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int totalDays = this.getDaysRequired(weights, mid);
            if (totalDays <= numberOfDays) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return low;
    }

}

/*
 * Algorithm:
 * 
 * First, we will find the maximum element i.e. max(weights[]), and the
 * summation i.e. sum(weights[]) of the given array.
 * 
 * Place the 2 pointers i.e. low and high: Initially, we will place the
 * pointers. The pointer low will point to max(weights[]) and the high will
 * point to sum(weights[]).
 * 
 * Calculate the ‘mid’: Now, inside the loop, we will calculate the value of
 * ‘mid’ using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * 
 * Eliminate the halves based on the number of days required for the capacity
 * ‘mid’:
 * We will pass the potential capacity, represented by the variable 'mid', to
 * the 'findDays()' function. This function will return the number of days
 * required to ship all the weights for the particular capacity, ‘mid’.
 * 
 * - If munerOfDays <= d: On satisfying this condition, we can conclude that the
 * number ‘mid’ is one of our possible answers. But we want the minimum number.
 * So, we will eliminate the right half and consider the left half(i.e. high =
 * mid-1).
 * 
 * - Otherwise, the value mid is smaller than the number we want. This means the
 * numbers greater than ‘mid’ should be considered and the right half of ‘mid’
 * consists of such numbers. So, we will eliminate the left half and consider
 * the right half(i.e. low = mid+1).
 * 
 * Finally, outside the loop, we will return the value of low as the pointer
 * will be pointing to the answer.
 * 
 * Time Complexity: O(N * log(sum(weights[]) - max(weights[]) + 1)), where
 * sum(weights[]) = summation of all the weights, max(weights[]) = maximum of
 * all the weights, N = size of the weights array.
 * Reason: We are applying binary search on the range [max(weights[]),
 * sum(weights[])]. For every possible answer ‘mid’, we are calling findDays()
 * function. Now, inside the findDays() function, we are using a loop that runs
 * for N times.
 * 
 * Space Complexity: O(1) as we are not using any extra space to solve this
 * problem.
 */

class ShipPackages {

    /**
     * Problem:
     * Ship all packages within <= d days.
     * Each day, total shipped weight cannot exceed capacity.
     *
     * Return the MINIMUM capacity required.
     *
     * ---------------------------------------------------------
     * Why Binary Search on Answer?
     *
     * We are minimizing capacity.
     *
     * If capacity C is enough to ship within d days,
     * then any capacity > C will also work.
     *
     * So feasibility is monotonic:
     *    false false false true true true ...
     *
     * Hence we binary search the smallest capacity that works.
     *
     * ---------------------------------------------------------
     * Search space:
     * low  = max(weights)  (must carry the heaviest package)
     * high = sum(weights)  (can carry everything in one day)
     *
     * Time Complexity: O(n log(sum(weights)))
     * Space Complexity: O(1)
     */
    public int shipWithinDays(int[] weights, int d) {

        int low = getMax(weights);
        int high = getSum(weights);

        // result stores the best valid capacity found so far
        int result = high;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // Check if we can ship with capacity = mid
            if (canShip(weights, d, mid)) {

                // mid is a valid capacity
                result = mid;

                // try to find smaller valid capacity
                high = mid - 1;
            } else {
                // mid is too small (need more capacity)
                low = mid + 1;
            }
        }

        return result;
    }

    /**
     * Feasibility check:
     * Can we ship all packages within <= d days with given capacity?
     *
     * Greedy Strategy:
     * - Keep loading packages in current day until capacity exceeds.
     * - Then start a new day.
     *
     * This greedy approach uses minimum possible days for this capacity.
     *
     * Time: O(n)
     */
    private boolean canShip(int[] weights, int d, int capacity) {

        int days = 1;            // start with day 1
        int currentWeight = 0;   // current day's total weight

        for (int w : weights) {

            // If adding this package exceeds capacity, ship next day
            if (currentWeight + w > capacity) {
                days++;
                currentWeight = 0;
            }

            currentWeight += w;

            // If days already exceeded d, no need to continue
            if (days > d) {
                return false;
            }
        }

        return true;
    }

    private int getMax(int[] weights) {
        int max = 0;
        for (int w : weights) {
            max = Math.max(max, w);
        }
        return max;
    }

    private int getSum(int[] weights) {
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        return total;
    }
}

/*
 * Problem Statement: You are given 'N’ roses and you are also given an array
 * 'arr' where 'arr[i]' denotes that the 'ith' rose will bloom on the 'arr[i]th'
 * day.
 * You can only pick already bloomed roses that are adjacent to make a bouquet.
 * You are also told that you require exactly 'k' adjacent bloomed roses to make
 * a single bouquet.
 * Find the minimum number of days required to make at least ‘m' bouquets each
 * containing 'k' roses. Return -1 if it is not possible.
 */

class MinimumDaysToMakeMBouquets {

    public int minDays(int[] nums, int k, int m) {
        int n = nums.length;
        if ((long) (m * k) > n) {
            return -1;
        }

        int[] minAndMax = this.minMax(nums);
        int low = minAndMax[0], high = minAndMax[1];
        int result = high;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (this.possible(nums, k, m, mid)) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    public boolean possible(int[] arr, int m, int k, int day) {
        int n = arr.length; // Size of the array
        int cnt = 0;
        int noOfB = 0;
        // Count the number of bouquets:
        for (int i = 0; i < n; i++) {
            if (arr[i] <= day) {
                cnt++;
            } else {
                noOfB += (cnt / k);
                cnt = 0;
            }
        }
        noOfB += (cnt / k);
        return noOfB >= m;
    }

    private int[] minMax(int[] nums) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int num : nums) {
            min = Math.min(min, num);
            max = Math.max(max, num);
        }
        return new int[] { min, max };
    }

    public int roseGarden(int[] arr, int k, int m) {
        long val = (long) m * k;
        int n = arr.length; // Size of the array
        if (val > n)
            return -1; // Impossible case.
        // Find maximum and minimum:
        int mini = Integer.MAX_VALUE, maxi = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            mini = Math.min(mini, arr[i]);
            maxi = Math.max(maxi, arr[i]);
        }

        // Apply binary search:
        int low = mini, high = maxi;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (this.possible(arr, m, k, mid)) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

}

class RoseBouquets {

    /**
     * Problem:
     * Given bloomDay[i] = day when flower i blooms.
     * We need to make exactly m bouquets.
     * Each bouquet needs k adjacent bloomed flowers.
     *
     * Return the minimum day such that we can make at least m bouquets.
     *
     * -------------------------------------------------------------
     * KEY INTERVIEW INSIGHT (Binary Search on Answer):
     *
     * Keywords / cues that suggest Binary Search on Answer:
     *
     * 1) "minimum day" / "minimum time" / "minimum capacity"
     *      -> We are minimizing a numeric answer.
     *
     * 2) We can check feasibility:
     *      "If I pick a day X, can I make m bouquets?"
     *      -> This gives a boolean function feasible(X).
     *
     * 3) Monotonic property exists:
     *      If day X works, then any day > X will also work,
     *      because more flowers will bloom with more days.
     *
     * This creates a pattern:
     *      false false false true true true true
     * which is exactly where binary search applies.
     *
     * -------------------------------------------------------------
     * Time Complexity:
     *   Binary Search runs log(maxDay) times (~log(1e9) ≈ 30)
     *   Each feasibility check scans the array: O(n)
     *   Total: O(n log 1e9) = O(n log maxDay)
     *
     * Space Complexity:
     *   O(1)
     */
    public int minDays(int[] bloomDay, int m, int k) {

        int n = bloomDay.length;

        // -------------------------------------------------------------
        // Quick impossible check:
        // If total flowers needed (m*k) is greater than available flowers,
        // we can never form m bouquets.
        // -------------------------------------------------------------
        if ((long) m * k > n) {
            return -1;
        }

        // -------------------------------------------------------------
        // Binary Search on Answer:
        //
        // low  = minimum possible day
        // high = maximum possible day
        //
        // We can take:
        // low = 1
        // high = 1e9 (constraints)
        //
        // More optimized:
        // low = min(bloomDay)
        // high = max(bloomDay)
        // (but 1..1e9 also works fine)
        // -------------------------------------------------------------
        int low = 1;
        int high = (int) 1e9;

        int result = -1;

        // -------------------------------------------------------------
        // Binary Search for FIRST TRUE:
        //
        // We want the smallest day such that:
        //   canMakeBouquets(day) == true
        //
        // So whenever mid is possible, we try smaller (high = mid - 1)
        // -------------------------------------------------------------
        while (low <= high) {

            int mid = low + (high - low) / 2;

            // Check if it's possible to make m bouquets by day=mid
            if (canMakeBouquets(bloomDay, m, k, mid)) {

                // mid is a valid day, store it
                result = mid;

                // Try to find an even smaller valid day
                high = mid - 1;
            } else {
                // mid is too small (not enough flowers bloomed yet)
                low = mid + 1;
            }
        }

        return result;
    }

    /**
     * Feasibility function:
     * Returns true if we can make at least m bouquets on or before 'day'.
     *
     * How we check:
     * -------------------------------------------------------------
     * We scan bloomDay array:
     * - if bloomDay[i] <= day, the flower is bloomed, so it can be used.
     * - we count consecutive bloomed flowers (adjacent requirement).
     *
     * Every time consecutive bloomed flowers reach k:
     * - we form 1 bouquet
     * - reset adjacency count to 0 (because those flowers are used)
     *
     * Example:
     * bloomDay = [1,10,3,10,2]
     * day = 3
     *
     * bloomed status = [Y, N, Y, N, Y]
     * adjacency runs:
     *   Y -> 1
     *   N -> reset
     *   Y -> 1
     *   N -> reset
     *   Y -> 1
     *
     * If k=1, bouquets=3
     * If k=2, bouquets=0
     *
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    private boolean canMakeBouquets(int[] bloomDay, int m, int k, int day) {

        int bouquets = 0;
        int adjacent = 0;

        for (int bloom : bloomDay) {

            // flower is bloomed and can be used
            if (bloom <= day) {
                adjacent++;

                // once we collect k adjacent flowers -> one bouquet formed
                if (adjacent == k) {
                    bouquets++;
                    adjacent = 0; // reset for next bouquet

                    // small optimization: if already enough bouquets, stop early
                    if (bouquets >= m) {
                        return true;
                    }
                }
            }
            // flower is not bloomed yet -> adjacency breaks
            else {
                adjacent = 0;
            }
        }

        return bouquets >= m;
    }
}


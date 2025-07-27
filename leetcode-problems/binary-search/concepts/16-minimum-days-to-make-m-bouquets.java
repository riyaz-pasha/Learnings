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

    public static int minDays(int[] arr, int m, int k) {
        int n = arr.length;

        // Check if it's even possible to make m bouquets
        if ((long) m * k > n)
            return -1;

        int low = 1, high = (int) 1e9;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (canMakeBouquets(arr, m, k, mid)) {
                result = mid;
                high = mid - 1; // try smaller days
            } else {
                low = mid + 1; // need more days
            }
        }

        return result;
    }

    private static boolean canMakeBouquets(int[] arr, int m, int k, int day) {
        int count = 0;
        int adjacent = 0;

        for (int bloomDay : arr) {
            if (bloomDay <= day) {
                adjacent++;
                if (adjacent == k) {
                    count++;
                    adjacent = 0;
                }
            } else {
                adjacent = 0;
            }
        }

        return count >= m;
    }

    public static void main(String[] args) {
        int[] arr = { 1, 10, 3, 10, 2 };
        int m = 3, k = 1;
        System.out.println(minDays(arr, m, k)); // Output: 3
    }

}

/*
 * Problem Statement: A monkey is given ‘n’ piles of bananas, whereas the 'ith'
 * pile has ‘a[i]’ bananas. An integer ‘h’ is also given, which denotes the time
 * (in hours) for all the bananas to be eaten.
 * 
 * Each hour, the monkey chooses a non-empty pile of bananas and eats ‘k’
 * bananas. If the pile contains less than ‘k’ bananas, then the monkey consumes
 * all the bananas and won’t eat any more bananas in that hour.
 * 
 * Find the minimum number of bananas ‘k’ to eat per hour so that the monkey can
 * eat all the bananas within ‘h’ hours.
 */

class KokoEatingBananas {

    /*
     * Time Complexity: O(N * log(max(a[]))), where max(a[]) is the maximum element
     * in the array and N = size of the array.
     * Reason: We are applying Binary search for the range [1, max(a[])], and for
     * every value of ‘mid’, we are traversing the entire array inside the function
     * named calculateTotalHours().
     * 
     * Space Complexity: O(1) as we are not using any extra space to solve this
     * problem.
     */
    public int minimumRateToEatBananas(int[] nums, int h) {
        int low = 1, high = this.findMax(nums);
        int ans = high;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int totalHours = this.calculateTotalHours(nums, mid);
            if (totalHours <= h) {
                ans = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return ans;
    }

    public int minimumRateToEatBananas2(int[] v, int h) {
        int low = 1, high = findMax(v);

        // apply binary search:
        while (low <= high) {
            int mid = (low + high) / 2;
            int totalH = this.calculateTotalHours(v, mid);
            if (totalH <= h) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    private int calculateTotalHours(int[] nums, int hourly) {
        int totalHours = 0;
        for (int num : nums) {
            totalHours += (Math.ceil((double) num / (double) hourly));
        }
        return totalHours;
    }

    public int findMax(int[] nums) {
        int maxi = Integer.MIN_VALUE;
        int n = nums.length;
        // find the maximum:
        for (int i = 0; i < n; i++) {
            maxi = Math.max(maxi, nums[i]);
        }
        // return Arrays.stream(nums).max().getAsInt();
        return maxi;
    }

}

class MonkeyBananas {

    public static int minEatingSpeed(int[] piles, int h) {
        int low = 1;
        int high = getMax(piles);
        int result = high;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (canEatAll(piles, mid, h)) {
                result = mid;
                high = mid - 1; // try to minimize k
            } else {
                low = mid + 1; // increase k
            }
        }

        return result;
    }

    // Helper to find max pile size
    private static int getMax(int[] piles) {
        int max = Integer.MIN_VALUE;
        for (int pile : piles) {
            max = Math.max(max, pile);
        }
        return max;
    }

    // Helper to check if monkey can eat all with speed k
    private static boolean canEatAll(int[] piles, int k, int h) {
        int totalTime = 0;
        for (int pile : piles) {
            totalTime += Math.ceil((double) pile / k);
        }
        return totalTime <= h;
    }

    public static void main(String[] args) {
        int[] piles = { 3, 6, 7, 11 };
        int h = 8;
        System.out.println(minEatingSpeed(piles, h)); // Output: 4
    }

}

/*
 * Algorithm:
 * First, we will find the maximum element in the given array i.e. max(a[]).
 * Place the 2 pointers i.e. low and high: Initially, we will place the
 * pointers. The pointer low will point to 1 and the high will point to
 * max(a[]).
 * 
 * Calculate the ‘mid’: Now, inside the loop, we will calculate the value of
 * ‘mid’ using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * 
 * Eliminate the halves based on the time required if Koko eats ‘mid’
 * bananas/hr:
 * We will first calculate the total time(required to consume all the bananas in
 * the array) i.e. totalH using the function calculateTotalHours(a[], mid):
 * 
 * If totalH <= h: On satisfying this condition, we can conclude that the number
 * ‘mid’ is one of our possible answers. But we want the minimum number. So, we
 * will eliminate the right half and consider the left half(i.e. high = mid-1).
 * 
 * Otherwise, the value mid is smaller than the number we want(as the totalH >
 * h). This means the numbers greater than ‘mid’ should be considered and the
 * right half of ‘mid’ consists of such numbers. So, we will eliminate the left
 * half and consider the right half(i.e. low = mid+1).
 * 
 * Finally, outside the loop, we will return the value of low as the pointer
 * will be pointing to the answer.
 * The steps from 2-4 will be inside a loop and the loop will continue until low
 * crosses high.
 */

// | Metric | Value |
// | ---------------- | ------------------- |
// | Time Complexity | O(n \* log(max(a))) |
// | Space Complexity | O(1) |

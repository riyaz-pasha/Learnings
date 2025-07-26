/*
 * Problem Statement: You're given an sorted array arr of n integers and an
 * integer x. Find the floor and ceiling of x in arr[0..n-1].
 * The floor of x is the largest element in the array which is smaller than or
 * equal to x.
 * The ceiling of x is the smallest element in the array greater than or equal
 * to x.
 */

/*
 * The floor of x is the largest element in the array which is smaller than or
 * equal to x( i.e. largest element in the array <= x).
 */

class Floor {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = n;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] <= target) {
                result = mid;
                // look for smaller index on the left
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return result;
    }

}

/*
 * The ceiling of x is the smallest element in the array greater than or equal
 * to x( i.e. smallest element in the array >= x).
 * 
 * Place the 2 pointers i.e. low and high: Initially, we will place the pointers
 * like this: low will point to the first index and high will point to the last
 * index.
 * Calculate the ‘mid’: Now, we will calculate the value of mid using the
 * following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * Compare arr[mid] with x: With comparing arr[mid] to x, we can observe 2
 * different cases:
 * Case 1 - If arr[mid] >= x: This condition means that the index arr[mid] may
 * be an answer. So, we will update the ‘ans’ variable with arr[mid] and search
 * in the left half if there is any smaller number that satisfies the same
 * condition. Here, we are eliminating the right half.
 * Case 2 - If arr[mid] < x: In this case, arr[mid] cannot be our answer and we
 * need to find some bigger element. So, we will eliminate the left half and
 * search in the right half for the answer.
 */

class Ceil {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = n;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] >= target) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

}

/*
 * What is Upper Bound?
 * The upper bound algorithm finds the first or the smallest index in a sorted
 * array where the value at that index is greater than the given key i.e. x.
 * 
 * The upper bound is the smallest index, ind, where arr[ind] > x.
 * 
 * But if any such index is not found, the upper bound algorithm returns n i.e.
 * size of the given array. The main difference between the lower and upper
 * bound is in the condition. For the lower bound the condition was arr[ind] >=
 * x and here, in the case of the upper bound, it is arr[ind] > x.
 */

class UpperBound {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = n;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] > target) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

}

/*
 * Place the 2 pointers i.e. low and high: Initially, we will place the pointers
 * like this: low will point to the first index and high will point to the last
 * index.
 * Calculate the ‘mid’: Now, we will calculate the value of mid using the
 * following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * Compare arr[mid] with x: With comparing arr[mid] to x, we can observe 2
 * different cases:
 * - Case 1 - If arr[mid] > x: This condition means that the index mid may be an
 * answer. So, we will update the ‘ans’ variable with mid and search in the left
 * half if there is any smaller index that satisfies the same condition. Here,
 * we are eliminating the right half.
 * - Case 2 - If arr[mid] <= x: In this case, mid cannot be our answer and we
 * need to find some bigger element. So, we will eliminate the left half and
 * search in the right half for the answer.
 */

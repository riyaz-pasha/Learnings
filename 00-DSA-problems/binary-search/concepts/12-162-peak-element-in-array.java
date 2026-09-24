/*
 * Problem Statement: Given an array of length N. Peak element is defined as the
 * element greater than both of its neighbors. Formally, if 'arr[i]' is the peak
 * element, 'arr[i - 1]' < 'arr[i]' and 'arr[i + 1]' < 'arr[i]'. Find the
 * index(0-based) of a peak element in the array. If there are multiple peak
 * numbers, return the index of any peak number.
 * 
 * Note: For the first element, the previous element should be considered as
 * negative infinity as well as for the last element, the next element should be
 * considered as negative infinity.
 * 
 */

class PeakElementInArray {

    public int findPeak(int[] nums) {
        int n = nums.length;

        if (n == 1) {
            return 0;
        }
        if (nums[0] > nums[1]) {
            return 0;
        }
        if (nums[n - 1] > nums[n - 2]) {
            return n - 1;
        }

        int low = 1, high = n - 2;
        while (low <= high) {
            int mid = low + (high - low) / 2;

            // If nums[mid] is the peak:
            if (nums[mid] > nums[mid - 1] && nums[mid] > nums[mid + 1]) {
                return mid;
            } else if (nums[mid] > nums[mid - 1]) {
                // If we are in the left:
                low = mid + 1;
            } else {
                // If we are in the right:
                // Or, arr[mid] is a common point:
                high = mid - 1;
            }
        }

        return -1;
    }

}

/*
 * The left half of the peak element has an increasing order. This means for
 * every index i, arr[i-1] < arr[i].
 * 
 * On the contrary, the right half of the peak element has a decreasing order.
 * This means for every index i, arr[i+1] < arr[i].
 * 
 * 
 * Now, using the above observation, we can easily identify the left and right
 * halves, just by checking the property of the current index, i, like the
 * following:
 * 
 * If arr[i] > arr[i-1]: we are in the left half.
 * If arr[i] > arr[i+1]: we are in the right half.
 * 
 * 
 * How to eliminate the halves accordingly:
 * 
 * If we are in the left half of the peak element, we have to eliminate this
 * left half (i.e. low = mid+1).
 * Because our peak element appears somewhere on the right side.
 * 
 * If we are in the right half of the peak element, we have to eliminate this
 * right half (i.e. high = mid-1).
 * Because our peak element appears somewhere on the left side.
 */

/*
 * If n == 1: This means the array size is 1. If the array contains only one
 * element, we will return that index i.e. 0.
 * If arr[0] > arr[1]: This means the very first element of the array is the
 * peak element. So, we will return the index 0.
 * If arr[n-1] > arr[n-2]: This means the last element of the array is the peak
 * element. So, we will return the index n-1.
 * Place the 2 pointers i.e. low and high: Initially, we will place the pointers
 * excluding index 0 and n-1 like this: low will point to index 1, and high will
 * point to index n-2 i.e. the second last index.
 * Calculate the ‘mid’: Now, inside a loop, we will calculate the value of ‘mid’
 * using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * Check if arr[mid] is the peak element:
 * If arr[mid] > arr[mid-1] and arr[mid] > arr[mid+1]: If this condition is true
 * for arr[mid], we can conclude arr[mid] is the peak element. We will return
 * the index ‘mid’.
 * If arr[mid] > arr[mid-1]: This means we are in the left half and we should
 * eliminate it as our peak element appears on the right. So, we will do this:
 * low = mid+1.
 * Otherwise, we are in the right half and we should eliminate it as our peak
 * element appears on the left. So, we will do this: high = mid-1. This case
 * also handles the case for the index ‘mid’ being a common point of a
 * decreasing and increasing sequence. It will consider the left peak and
 * eliminate the right peak.
 */
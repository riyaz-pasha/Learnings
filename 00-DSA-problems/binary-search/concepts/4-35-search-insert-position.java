/*
 * Problem Statement: You are given a sorted array arr of distinct values and a
 * target value x. You need to search for the index of the target value in the
 * array.
 * 
 * If the value is present in the array, then return its index. Otherwise,
 * determine the index where it would be inserted in the array while maintaining
 * the sorted order.
 */

class SearchInsertPosition {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = n;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[low] >= target) {
                result = mid;
                // look for smaller index on the left
                high = mid - 1;
            } else {
                // look on the right
                low = mid + 1;
            }
        }
        return result;
    }

}

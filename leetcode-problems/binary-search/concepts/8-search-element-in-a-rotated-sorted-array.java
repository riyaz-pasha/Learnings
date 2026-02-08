/*
 * Problem Statement: Given an integer array arr of size N, sorted in ascending
 * order (with distinct values) and a target value k. Now the array is rotated
 * at some pivot point unknown to you. Find the index at which k is present and
 * if k is not present return -1.
 * 
 * Let's consider a sorted array: {1, 2, 3, 4, 5}. If we rotate this array at
 * index 3, it will become: {4, 5, 1, 2, 3}. In essence, we moved the element at
 * the last index to the front, while shifting the remaining elements to the
 * right. We performed this process twice.
 */

class SearchElementInARotatedSortedArray {

    /**
     * Searches for target in a rotated sorted array.
     *
     * Example rotated array:
     *   [4,5,6,7,0,1,2]
     *
     * Key Property:
     *   At any index mid, at least one side (left half or right half)
     *   must be sorted.
     *
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int binarySearch(int[] nums, int target) {

        int n = nums.length;
        int low = 0;
        int high = n - 1;

        // Standard binary search loop
        while (low <= high) {

            int mid = low + (high - low) / 2;

            // ------------------------------
            // Case 1: Found target directly
            // ------------------------------
            if (nums[mid] == target) {
                return mid;
            }

            // ---------------------------------------------------------
            // Now decide which half is sorted:
            //
            // If nums[low] <= nums[mid], then left half [low..mid] is sorted.
            // Otherwise, right half [mid..high] is sorted.
            // ---------------------------------------------------------

            // ------------------------------
            // Case 2: Left half is sorted
            // ------------------------------
            if (nums[low] <= nums[mid]) {

                // If target lies inside sorted left half range,
                // we shrink search to left side.
                //
                // Sorted left half range is: nums[low] ... nums[mid]
                if (nums[low] <= target && target < nums[mid]) {
                    high = mid - 1;
                }
                // Otherwise target must be in right half
                else {
                    low = mid + 1;
                }

            }
            // ------------------------------
            // Case 3: Right half is sorted
            // ------------------------------
            else {

                // Sorted right half range is: nums[mid] ... nums[high]
                // If target lies in this range, search right.
                if (nums[mid] < target && target <= nums[high]) {
                    low = mid + 1;
                }
                // Otherwise search left.
                else {
                    high = mid - 1;
                }
            }
        }

        // Target not found
        return -1;
    }
}


/*
 * Place the 2 pointers i.e. low and high: Initially, we will place the pointers
 * like this: low will point to the first index, and high will point to the last
 * index.
 * Calculate the ‘mid’: Now, inside a loop, we will calculate the value of ‘mid’
 * using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * Check if arr[mid] == target: If it is, return the index mid.
 * Identify the sorted half, check where the target is located, and then
 * eliminate one half accordingly:
 * If arr[low] <= arr[mid]: This condition ensures that the left part is sorted.
 * If arr[low] <= target && target <= arr[mid]: It signifies that the target is
 * in this sorted half. So, we will eliminate the right half (high = mid-1).
 * Otherwise, the target does not exist in the sorted half. So, we will
 * eliminate this left half by doing low = mid+1.
 * Otherwise, if the right half is sorted:
 * If arr[mid] <= target && target <= arr[high]: It signifies that the target is
 * in this sorted right half. So, we will eliminate the left half (low = mid+1).
 * Otherwise, the target does not exist in this sorted half. So, we will
 * eliminate this right half by doing high = mid-1.
 * Once, the ‘mid’ points to the target, the index will be returned.
 * This process will be inside a loop and the loop will continue until low
 * crosses high. If no index is found, we will return -1.
 */

class RotatedArrayWithDuplicates {

    /**
     * Search in Rotated Sorted Array with Duplicates (LC 81)
     *
     * Array is originally sorted but rotated at some pivot.
     * Duplicates make it hard to identify which half is sorted.
     *
     * Example:
     *   [2,5,6,0,0,1,2]
     *
     * Time Complexity:
     *   Average: O(log n)
     *   Worst case: O(n)  (when many duplicates exist, like [1,1,1,1,1])
     *
     * Space Complexity:
     *   O(1)
     */
    public static boolean search(int[] nums, int target) {

        int left = 0;
        int right = nums.length - 1;

        while (left <= right) {

            int mid = left + (right - left) / 2;

            // --------------------------
            // Case 1: Found target
            // --------------------------
            if (nums[mid] == target) {
                return true;
            }

            // ---------------------------------------------------------
            // Case 2: Ambiguous situation due to duplicates
            //
            // Example:
            //   nums[left] = nums[mid] = nums[right] = 2
            //
            // We cannot determine which half is sorted.
            // So we shrink the search space by moving inward.
            //
            // This is why worst-case becomes O(n).
            // ---------------------------------------------------------
            if (nums[left] == nums[mid] && nums[mid] == nums[right]) {
                left++;
                right--;
            }

            // ---------------------------------------------------------
            // Case 3: Left half is sorted
            //
            // Condition: nums[left] <= nums[mid]
            // Then range [left..mid] is sorted.
            // ---------------------------------------------------------
            else if (nums[left] <= nums[mid]) {

                // Check if target lies in the sorted left half
                if (nums[left] <= target && target < nums[mid]) {
                    right = mid - 1; // Search inside left half
                } else {
                    left = mid + 1;  // Search in right half
                }
            }

            // ---------------------------------------------------------
            // Case 4: Right half is sorted
            //
            // Otherwise, range [mid..right] is sorted.
            // ---------------------------------------------------------
            else {

                // Check if target lies in the sorted right half
                if (nums[mid] < target && target <= nums[right]) {
                    left = mid + 1; // Search inside right half
                } else {
                    right = mid - 1; // Search in left half
                }
            }
        }

        // Target not found
        return false;
    }
}

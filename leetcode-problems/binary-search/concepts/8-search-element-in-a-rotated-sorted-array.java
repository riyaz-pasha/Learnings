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

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            // if mid points to the target
            if (nums[mid] == target) {
                return mid;
            }
            // if left part is sorted
            if (nums[low] <= nums[mid]) {
                if (nums[low] <= target && target <= nums[mid]) {
                    high = mid - 1; // search in left half
                } else {
                    low = mid + 1; // search in right half
                }
            } else { // if right part is sorted
                if (nums[mid] <= target && target <= nums[high]) {
                    low = mid + 1; // search in right half
                } else {
                    high = mid - 1; // search in left half
                }
            }
        }

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

    public static boolean search(int[] nums, int target) {
        int left = 0, right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target)
                return true;

            // If we can't decide which half is sorted (due to duplicates)
            if (nums[left] == nums[mid] && nums[mid] == nums[right]) {
                left++;
                right--;
            }

            // Left half is sorted
            else if (nums[left] <= nums[mid]) {
                if (nums[left] <= target && target < nums[mid]) {
                    right = mid - 1; // Search left half
                } else {
                    left = mid + 1; // Search right half
                }
            }

            // Right half is sorted
            else {
                if (nums[mid] < target && target <= nums[right]) {
                    left = mid + 1; // Search right half
                } else {
                    right = mid - 1; // Search left half
                }
            }
        }

        return false;
    }

    public static void main(String[] args) {
        int[] nums = { 2, 5, 6, 0, 0, 1, 2 };
        int target = 0;

        System.out.println("Found target? " + search(nums, target)); // Output: true
    }

}

/*
 * Problem Statement: Given an integer array arr of size N, sorted in ascending
 * order (with distinct values). Now the array is rotated between 1 to N times
 * which is unknown. Find the minimum element in the array.
 */

class MinimumInRotatedSortedArray {

    public int binarySearch(int[] nums) {
        int n = nums.length;
        int low = 0, high = -1, result = Integer.MAX_VALUE;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            // search space is already sorted
            // then nums[low] will always be
            // the minimum in that search space:
            if (nums[low] <= nums[high]) {
                result = Math.min(result, nums[low]);
                break;
            }

            // if left part is sorted:
            if (nums[low] <= nums[mid]) {
                // keep the minimum:
                result = Math.min(result, nums[low]);
                // Eliminate left half:
                low = mid + 1;
            } else {// if right part is sorted:
                // keep the minimum:
                result = Math.min(result, nums[mid]);
                // Eliminate right half:
                high = mid - 1;
            }
        }

        return result;
    }

    public int binarySearchWithDuplicates(int[] nums) {
        int n = nums.length;
        int low = 0, high = n - 1, result = Integer.MAX_VALUE;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            // search space is already sorted
            // then nums[low] will always be
            // the minimum in that search space:
            if (nums[low] == nums[mid] && nums[mid] == nums[high]) {
                low++;
                high--;
                result = Math.min(result, nums[mid]);
            }
            // if left part is sorted:
            else if (nums[low] <= nums[mid]) {
                // keep the minimum:
                result = Math.min(result, nums[low]);
                // Eliminate left half:
                low = mid + 1;
            } else {// if right part is sorted:
                // keep the minimum:
                result = Math.min(result, nums[mid]);
                // Eliminate right half:
                high = mid - 1;
            }
        }

        return result;
    }

}

/*
 * Place the 2 pointers i.e. low and high: Initially, we will place the pointers
 * like this: low will point to the first index and high will point to the last
 * index.
 * Calculate the ‘mid’: Now, inside a loop, we will calculate the value of ‘mid’
 * using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * Identify the sorted half, and after picking the leftmost element, eliminate
 * that half.
 * If arr[low] <= arr[mid]: This condition ensures that the left part is sorted.
 * So, we will pick the leftmost element i.e. arr[low]. Now, we will compare it
 * with 'ans' and update 'ans' with the smaller value (i.e., min(ans,
 * arr[low])). Now, we will eliminate this left half(i.e. low = mid+1).
 * Otherwise, if the right half is sorted: This condition ensures that the right
 * half is sorted. So, we will pick the leftmost element i.e. arr[mid]. Now, we
 * will compare it with 'ans' and update 'ans' with the smaller value (i.e.,
 * min(ans, arr[mid])). Now, we will eliminate this right half(i.e. high =
 * mid-1).
 * This process will be inside a loop and the loop will continue until low
 * crosses high. Finally, we will return the ‘ans’ variable that stores the
 * minimum element.
 */

class FindMinimumRotatedArray {

    public static int findMin(int[] nums) {
        int left = 0, right = nums.length - 1;

        // If array is not rotated
        if (nums[left] <= nums[right]) {
            return nums[left];
        }

        while (left < right) {
            int mid = left + (right - left) / 2;

            // If mid element is greater than right,
            // minimum lies in the right half
            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                // Else the minimum lies in the left half including mid
                right = mid;
            }
        }

        // At the end of loop, left == right pointing to the smallest element
        return nums[left];
    }

    public static void main(String[] args) {
        int[] nums = { 4, 5, 6, 7, 0, 1, 2 };
        System.out.println("Minimum element is: " + findMin(nums)); // Output: 0
    }

}

class MinInRotatedWithDuplicates {

    public static int findMin(int[] nums) {
        int left = 0, right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else if (nums[mid] < nums[right]) {
                right = mid;
            } else {
                // nums[mid] == nums[right], can't decide, shrink right
                right--;
            }
        }

        return nums[left];
    }

    public static void main(String[] args) {
        int[] nums = { 2, 2, 2, 0, 1 };
        System.out.println("Minimum element: " + findMin(nums)); // Output: 0
    }

}

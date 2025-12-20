/*
 * Problem Statement: Given an integer array arr of size N, sorted in ascending
 * order (with distinct values). Now the array is rotated between 1 to N times
 * which is unknown. Find the minimum element in the array.
 */

class MinimumInRotatedSortedArray {

    public int binarySearch(int[] nums) {
        int n = nums.length;
        int low = 0, high = n - 1, result = Integer.MAX_VALUE;

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

class FindMinimumInRotatedSortedArray {

    // Approach 1: Binary Search (Most Efficient)
    // Time: O(log n), Space: O(1)
    public int findMin1(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // If mid element is greater than right element,
            // minimum is in the right half
            if (nums[mid] > nums[right]) {
                left = mid + 1;
            }
            // Otherwise, minimum is in the left half (including mid)
            else {
                right = mid;
            }
        }

        return nums[left];
    }

    // Approach 2: Binary Search with Edge Case Handling
    // Time: O(log n), Space: O(1)
    public int findMin2(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        // If array is not rotated or has only one element
        if (nums[left] < nums[right] || nums.length == 1) {
            return nums[left];
        }

        while (left < right) {
            int mid = left + (right - left) / 2;

            // Check if mid+1 is the minimum
            if (mid < nums.length - 1 && nums[mid] > nums[mid + 1]) {
                return nums[mid + 1];
            }

            // Check if mid is the minimum
            if (mid > 0 && nums[mid] < nums[mid - 1]) {
                return nums[mid];
            }

            // Decide which half to search
            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return nums[left];
    }

    // Approach 3: Binary Search (Alternative Logic)
    // Time: O(log n), Space: O(1)
    public int findMin3(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // Compare mid with left to decide which half to search
            if (nums[mid] < nums[left]) {
                right = mid;
            } else if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                // Array is not rotated in this range
                return nums[left];
            }
        }

        return nums[left];
    }

    // Approach 4: Recursive Binary Search
    // Time: O(log n), Space: O(log n) due to recursion stack
    public int findMin4(int[] nums) {
        return findMinHelper(nums, 0, nums.length - 1);
    }

    private int findMinHelper(int[] nums, int left, int right) {
        // Base case: only one or two elements
        if (left == right) {
            return nums[left];
        }
        if (right - left == 1) {
            return Math.min(nums[left], nums[right]);
        }

        // Array is not rotated
        if (nums[left] < nums[right]) {
            return nums[left];
        }

        int mid = left + (right - left) / 2;

        // Minimum is in the right half
        if (nums[mid] > nums[right]) {
            return findMinHelper(nums, mid + 1, right);
        }
        // Minimum is in the left half (including mid)
        else {
            return findMinHelper(nums, left, mid);
        }
    }

    // Test cases
    public static void main(String[] args) {
        FindMinimumInRotatedSortedArray solution = new FindMinimumInRotatedSortedArray();

        // Test Case 1
        int[] nums1 = { 3, 4, 5, 1, 2 };
        System.out.println("Test 1: " + solution.findMin1(nums1)); // Output: 1

        // Test Case 2
        int[] nums2 = { 4, 5, 6, 7, 0, 1, 2 };
        System.out.println("Test 2: " + solution.findMin1(nums2)); // Output: 0

        // Test Case 3
        int[] nums3 = { 11, 13, 15, 17 };
        System.out.println("Test 3: " + solution.findMin1(nums3)); // Output: 11

        // Test Case 4: Single element
        int[] nums4 = { 1 };
        System.out.println("Test 4: " + solution.findMin1(nums4)); // Output: 1

        // Test Case 5: Two elements rotated
        int[] nums5 = { 2, 1 };
        System.out.println("Test 5: " + solution.findMin1(nums5)); // Output: 1

        // Test Case 6: Two elements not rotated
        int[] nums6 = { 1, 2 };
        System.out.println("Test 6: " + solution.findMin1(nums6)); // Output: 1
    }
}

/*
 * EXPLANATION:
 * 
 * The key insight is to use binary search by comparing the middle element with
 * the rightmost (or leftmost) element to determine which half contains the
 * minimum.
 * 
 * Approach 1 (Recommended - Simplest and Most Elegant):
 * - Compare nums[mid] with nums[right]
 * - If nums[mid] > nums[right]: minimum is in right half (left = mid + 1)
 * - Otherwise: minimum is in left half including mid (right = mid)
 * - Continue until left == right
 * 
 * Why this works:
 * 1. In a rotated sorted array, one half is always sorted
 * 2. If nums[mid] > nums[right], the rotation point (minimum) is to the right
 * 3. If nums[mid] <= nums[right], the minimum is to the left or at mid
 * 
 * Time Complexity: O(log n) - binary search
 * Space Complexity: O(1) - constant space
 * 
 * Example walkthrough for [4,5,6,7,0,1,2]:
 * - left=0, right=6, mid=3, nums[3]=7 > nums[6]=2 → left=4
 * - left=4, right=6, mid=5, nums[5]=1 < nums[6]=2 → right=5
 * - left=4, right=5, mid=4, nums[4]=0 < nums[5]=1 → right=4
 * - left=4, right=4 → return nums[4]=0
 */

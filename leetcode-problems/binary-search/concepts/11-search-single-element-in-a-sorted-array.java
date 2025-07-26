
/*
 * Problem Statement: Given an array of N integers. Every number in the array
 * except one appears twice. Find the single number in the array.
 */

class SearchSingleElementInASortedArray {

    public int singleNonDuplicate(int[] nums) {
        int n = nums.length;

        if (n == 1) {
            return nums[0];
        }
        if (nums[1] != nums[0]) {
            return nums[0];
        }
        if (nums[n - 1] != nums[n - 2]) {
            return nums[n - 1];
        }

        int low = 1, high = n - 2;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] != nums[mid - 1] && nums[mid] != nums[mid + 1]) {
                return nums[mid];
            }
            if ((mid % 2 == 1 && nums[mid] == nums[mid - 1])
                    || (mid % 2 == 0 && nums[mid] == nums[mid + 1])) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return -1;
    }

}

/*
 * The index sequence of the duplicate numbers in the left half is always (even,
 * odd). That means one of the following conditions will be satisfied if we are
 * in the left half:
 * 
 * If the current index is even, the element at the next odd index will be the
 * same as the current element.
 * 
 * Similarly, If the current index is odd, the element at the preceding even
 * index will be the same as the current element.
 * 
 * 
 * The index sequence of the duplicate numbers in the right half is always (odd,
 * even). That means one of the following conditions will be satisfied if we are
 * in the right half:
 * 
 * If the current index is even, the element at the preceding odd index will be
 * the same as the current element.
 * 
 * Similarly, If the current index is odd, the element at the next even index
 * will be the same as the current element.
 */

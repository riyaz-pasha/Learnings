/*
 * Problem:
 * Given a sorted array where every element appears exactly twice,
 * except one element that appears only once.
 * Return that single element.
 *
 * Example:
 * nums = [1,1,2,3,3,4,4]
 * Answer = 2
 *
 * Key Observation:
 * ---------------------------------------------------------
 * In a sorted array with pairs:
 *
 * Before the single element:
 *   pairs start at EVEN indices:
 *   (0,1), (2,3), (4,5) ...
 *
 * After the single element:
 *   pairs shift and start at ODD indices:
 *   (1,2), (3,4), (5,6) ...
 *
 * So the "pair pattern breaks" at the single element.
 *
 * We use binary search to locate where this shift happens.
 *
 * Time Complexity: O(log n)
 * Space Complexity: O(1)
 */
class SearchSingleElementInASortedArray {

    public int singleNonDuplicate(int[] nums) {

        int n = nums.length;

        // -------------------------------
        // Edge cases (boundaries)
        // -------------------------------
        if (n == 1) return nums[0];

        // If first element is not equal to second, first is single
        if (nums[0] != nums[1]) return nums[0];

        // If last element is not equal to second last, last is single
        if (nums[n - 1] != nums[n - 2]) return nums[n - 1];

        // Now we can safely binary search inside range [1 .. n-2]
        int low = 1;
        int high = n - 2;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // ---------------------------------------------------------
            // Check if nums[mid] is the single element:
            // It must be different from both neighbors.
            // ---------------------------------------------------------
            if (nums[mid] != nums[mid - 1] && nums[mid] != nums[mid + 1]) {
                return nums[mid];
            }

            // ---------------------------------------------------------
            // Now decide which side to move.
            //
            // Pattern rules:
            //
            // If mid is odd:
            //   normally pair should be (mid-1, mid)
            //
            // If mid is even:
            //   normally pair should be (mid, mid+1)
            //
            // If this pairing is correct, we are still on the LEFT side
            // of the single element, so single element is on the RIGHT.
            //
            // Otherwise, we are already on the RIGHT side of the single,
            // so single element lies on the LEFT.
            // ---------------------------------------------------------

            // Case: we are still following correct pairing pattern
            if ((mid % 2 == 1 && nums[mid] == nums[mid - 1]) ||
                (mid % 2 == 0 && nums[mid] == nums[mid + 1])) {

                // Single element must be on the RIGHT side
                low = mid + 1;
            }
            else {
                // Pairing pattern is broken -> single element on LEFT side
                high = mid - 1;
            }
        }

        return -1; // should never happen for valid input
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

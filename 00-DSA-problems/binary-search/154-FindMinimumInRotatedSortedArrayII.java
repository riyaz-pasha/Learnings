class FindMinimumInRotatedSortedArrayII {

    // Approach 1: Binary Search with Duplicate Handling (Optimal)
    // Time: O(log n) average, O(n) worst case, Space: O(1)
    public int findMin1(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                // Minimum is in the right half
                left = mid + 1;
            } else if (nums[mid] < nums[right]) {
                // Minimum is in the left half (including mid)
                right = mid;
            } else {
                // nums[mid] == nums[right]
                // Can't determine which half, so reduce search space by 1
                right--;
            }
        }

        return nums[left];
    }

    // Approach 2: Binary Search with Both Endpoints Comparison
    // Time: O(log n) average, O(n) worst case, Space: O(1)
    public int findMin2(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // If left, mid, and right are all equal, can't determine
            // Shrink from both ends
            if (nums[left] == nums[mid] && nums[mid] == nums[right]) {
                left++;
                right--;
            } else if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return nums[left];
    }

    // Approach 3: Optimized with Early Termination
    // Time: O(log n) average, O(n) worst case, Space: O(1)
    public int findMin3(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            // Early termination: if subarray is sorted
            if (nums[left] < nums[right]) {
                return nums[left];
            }

            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else if (nums[mid] < nums[right]) {
                right = mid;
            } else {
                // Handle duplicates
                right--;
            }
        }

        return nums[left];
    }

    // Approach 4: Find Rotation Index First
    // Time: O(log n) average, O(n) worst case, Space: O(1)
    public int findMin4(int[] nums) {
        int n = nums.length;
        int left = 0;
        int right = n - 1;

        // Find the rotation index (where minimum element is)
        while (left < right) {
            int mid = left + (right - left) / 2;

            // Check if we found the rotation point
            if (mid < n - 1 && nums[mid] > nums[mid + 1]) {
                return nums[mid + 1];
            }
            if (mid > 0 && nums[mid] < nums[mid - 1]) {
                return nums[mid];
            }

            // Handle equal elements
            if (nums[left] == nums[mid] && nums[mid] == nums[right]) {
                // Check if left or right is the minimum
                if (left < n - 1 && nums[left] > nums[left + 1]) {
                    return nums[left + 1];
                }
                left++;
                if (right > 0 && nums[right] < nums[right - 1]) {
                    return nums[right];
                }
                right--;
            } else if (nums[mid] > nums[right] ||
                    (nums[mid] == nums[right] && nums[mid] > nums[left])) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return nums[left];
    }

    // Approach 5: Recursive with Duplicate Handling
    // Time: O(log n) average, O(n) worst case, Space: O(log n)
    public int findMin5(int[] nums) {
        return findMinHelper(nums, 0, nums.length - 1);
    }

    private int findMinHelper(int[] nums, int left, int right) {
        if (left == right) {
            return nums[left];
        }

        // If sorted, return leftmost
        if (nums[left] < nums[right]) {
            return nums[left];
        }

        int mid = left + (right - left) / 2;

        if (nums[mid] > nums[right]) {
            return findMinHelper(nums, mid + 1, right);
        } else if (nums[mid] < nums[right]) {
            return findMinHelper(nums, left, mid);
        } else {
            // Handle duplicates: search both halves
            int leftMin = findMinHelper(nums, left, mid);
            int rightMin = findMinHelper(nums, mid + 1, right);
            return Math.min(leftMin, rightMin);
        }
    }

    // Approach 6: Linear Scan (Fallback for worst case)
    // Time: O(n), Space: O(1)
    public int findMin6(int[] nums) {
        int min = nums[0];
        for (int num : nums) {
            min = Math.min(min, num);
        }
        return min;
    }

    // Test cases
    public static void main(String[] args) {
        FindMinimumInRotatedSortedArrayII solution = new FindMinimumInRotatedSortedArrayII();

        // Test Case 1: With duplicates
        int[] nums1 = { 2, 2, 2, 0, 1 };
        System.out.println("Test 1: " + solution.findMin1(nums1)); // Output: 0

        // Test Case 2: Original example
        int[] nums2 = { 4, 5, 6, 7, 0, 1, 4 };
        System.out.println("Test 2: " + solution.findMin1(nums2)); // Output: 0

        // Test Case 3: All same elements
        int[] nums3 = { 1, 1, 1, 1, 1 };
        System.out.println("Test 3: " + solution.findMin1(nums3)); // Output: 1

        // Test Case 4: Duplicates at rotation point
        int[] nums4 = { 3, 3, 1, 3 };
        System.out.println("Test 4: " + solution.findMin1(nums4)); // Output: 1

        // Test Case 5: More duplicates
        int[] nums5 = { 10, 1, 10, 10, 10 };
        System.out.println("Test 5: " + solution.findMin1(nums5)); // Output: 1

        // Test Case 6: No rotation with duplicates
        int[] nums6 = { 1, 3, 5, 5, 5, 7 };
        System.out.println("Test 6: " + solution.findMin1(nums6)); // Output: 1

        // Test Case 7: Two elements
        int[] nums7 = { 2, 2 };
        System.out.println("Test 7: " + solution.findMin1(nums7)); // Output: 2

        // Test Case 8: Complex case
        int[] nums8 = { 3, 3, 3, 3, 1, 2, 3 };
        System.out.println("Test 8: " + solution.findMin1(nums8)); // Output: 1
    }
}

/*
 * EXPLANATION - KEY DIFFERENCES FROM UNIQUE ELEMENTS:
 * 
 * With duplicates, the problem becomes more complex because when nums[mid] ==
 * nums[right],
 * we cannot determine which half contains the minimum.
 * 
 * APPROACH 1 (RECOMMENDED):
 * Three cases when comparing nums[mid] with nums[right]:
 * 1. nums[mid] > nums[right]: Minimum must be in right half → left = mid + 1
 * 2. nums[mid] < nums[right]: Minimum is in left half (including mid) → right =
 * mid
 * 3. nums[mid] == nums[right]: Cannot determine which half → right--
 * 
 * Why right-- works:
 * - If nums[right] is the minimum, we'll find it as nums[mid] (since they're
 * equal)
 * - If nums[right] is not the minimum, removing it doesn't affect the result
 * - This gradually eliminates duplicates from the right
 * 
 * Time Complexity:
 * - Average case: O(log n) when duplicates are sparse
 * - Worst case: O(n) when all elements are the same (e.g., [1,1,1,1,1])
 * 
 * Space Complexity: O(1)
 * 
 * EXAMPLE WALKTHROUGH for [2,2,2,0,1]:
 * Step 1: left=0, right=4, mid=2, nums[2]=2 == nums[4]=1? No, 2>1 → left=3
 * Step 2: left=3, right=4, mid=3, nums[3]=0 < nums[4]=1 → right=3
 * Step 3: left=3, right=3 → return nums[3]=0
 * 
 * EXAMPLE WALKTHROUGH for [3,3,3,3,1,2,3]:
 * Step 1: left=0, right=6, mid=3, nums[3]=3 == nums[6]=3 → right=5
 * Step 2: left=0, right=5, mid=2, nums[2]=3 > nums[5]=2 → left=3
 * Step 3: left=3, right=5, mid=4, nums[4]=1 < nums[5]=2 → right=4
 * Step 4: left=3, right=4, mid=3, nums[3]=3 > nums[4]=1 → left=4
 * Step 5: left=4, right=4 → return nums[4]=1
 * 
 * COMPLEXITY COMPARISON:
 * - Unique elements: Always O(log n)
 * - With duplicates: O(log n) average, O(n) worst case
 * 
 * The worst case is unavoidable because we need to examine each element when
 * all are equal to distinguish them from a potential different minimum.
 */

/*
 * Given two sorted arrays nums1 and nums2 of size m and n respectively, return
 * the median of the two sorted arrays.
 * 
 * The overall run time complexity should be O(log (m+n)).
 * 
 * 
 * 
 * Example 1:
 * 
 * Input: nums1 = [1,3], nums2 = [2]
 * Output: 2.00000
 * Explanation: merged array = [1,2,3] and median is 2.
 * Example 2:
 * 
 * Input: nums1 = [1,2], nums2 = [3,4]
 * Output: 2.50000
 * Explanation: merged array = [1,2,3,4] and median is (2 + 3) / 2 = 2.5.
 */

class MedianTwoSortedArrays {

    /**
     * Solution 1: Binary Search on Smaller Array (Optimal)
     * Time Complexity: O(log(min(m, n)))
     * Space Complexity: O(1)
     */
    public double findMedianSortedArrays(int[] nums1, int[] nums2) {
        // Ensure nums1 is the smaller array
        if (nums1.length > nums2.length) {
            return findMedianSortedArrays(nums2, nums1);
        }

        int m = nums1.length;
        int n = nums2.length;
        int left = 0, right = m;

        while (left <= right) {
            // Partition indices
            int partitionX = (left + right) / 2;
            int partitionY = (m + n + 1) / 2 - partitionX;

            // Elements on the left and right of partition
            int maxLeftX = (partitionX == 0) ? Integer.MIN_VALUE : nums1[partitionX - 1];
            int minRightX = (partitionX == m) ? Integer.MAX_VALUE : nums1[partitionX];

            int maxLeftY = (partitionY == 0) ? Integer.MIN_VALUE : nums2[partitionY - 1];
            int minRightY = (partitionY == n) ? Integer.MAX_VALUE : nums2[partitionY];

            // Check if we have found the correct partition
            if (maxLeftX <= minRightY && maxLeftY <= minRightX) {
                // Perfect partition found
                if ((m + n) % 2 == 0) {
                    return (Math.max(maxLeftX, maxLeftY) + Math.min(minRightX, minRightY)) / 2.0;
                } else {
                    return Math.max(maxLeftX, maxLeftY);
                }
            }
            // Adjust partition
            else if (maxLeftX > minRightY) {
                // We are too far on right side for partitionX. Go left.
                right = partitionX - 1;
            } else {
                // We are too far on left side for partitionX. Go right.
                left = partitionX + 1;
            }
        }

        throw new IllegalArgumentException("Input arrays are not sorted");
    }

    /**
     * Solution 2: Binary Search with Detailed Comments
     * Time Complexity: O(log(min(m, n)))
     * Space Complexity: O(1)
     */
    public double findMedianSortedArraysDetailed(int[] nums1, int[] nums2) {
        // Always ensure nums1 is smaller for optimization
        if (nums1.length > nums2.length) {
            int[] temp = nums1;
            nums1 = nums2;
            nums2 = temp;
        }

        int m = nums1.length;
        int n = nums2.length;
        int totalLength = m + n;
        int halfLength = (totalLength + 1) / 2;

        int left = 0, right = m;

        while (left <= right) {
            // Cut positions
            int cut1 = (left + right) / 2;
            int cut2 = halfLength - cut1;

            // Elements around cuts
            int left1 = (cut1 == 0) ? Integer.MIN_VALUE : nums1[cut1 - 1];
            int left2 = (cut2 == 0) ? Integer.MIN_VALUE : nums2[cut2 - 1];
            int right1 = (cut1 == m) ? Integer.MAX_VALUE : nums1[cut1];
            int right2 = (cut2 == n) ? Integer.MAX_VALUE : nums2[cut2];

            // Check if cuts are correct
            if (left1 <= right2 && left2 <= right1) {
                if (totalLength % 2 == 0) {
                    // Even total length - average of two middle elements
                    return (Math.max(left1, left2) + Math.min(right1, right2)) / 2.0;
                } else {
                    // Odd total length - the middle element
                    return Math.max(left1, left2);
                }
            } else if (left1 > right2) {
                // Too many elements from nums1, move left
                right = cut1 - 1;
            } else {
                // Too few elements from nums1, move right
                left = cut1 + 1;
            }
        }

        return -1; // Should never reach here for valid input
    }

    /**
     * Solution 3: Recursive Binary Search
     * Time Complexity: O(log(min(m, n)))
     * Space Complexity: O(log(min(m, n))) due to recursion
     */
    public double findMedianSortedArraysRecursive(int[] nums1, int[] nums2) {
        if (nums1.length > nums2.length) {
            return findMedianSortedArraysRecursive(nums2, nums1);
        }

        return findMedianHelper(nums1, nums2, 0, nums1.length);
    }

    private double findMedianHelper(int[] nums1, int[] nums2, int left, int right) {
        int m = nums1.length;
        int n = nums2.length;
        int halfLength = (m + n + 1) / 2;

        if (left > right) {
            throw new IllegalArgumentException("Invalid input");
        }

        int cut1 = (left + right) / 2;
        int cut2 = halfLength - cut1;

        int left1 = (cut1 == 0) ? Integer.MIN_VALUE : nums1[cut1 - 1];
        int left2 = (cut2 == 0) ? Integer.MIN_VALUE : nums2[cut2 - 1];
        int right1 = (cut1 == m) ? Integer.MAX_VALUE : nums1[cut1];
        int right2 = (cut2 == n) ? Integer.MAX_VALUE : nums2[cut2];

        if (left1 <= right2 && left2 <= right1) {
            if ((m + n) % 2 == 0) {
                return (Math.max(left1, left2) + Math.min(right1, right2)) / 2.0;
            } else {
                return Math.max(left1, left2);
            }
        } else if (left1 > right2) {
            return findMedianHelper(nums1, nums2, left, cut1 - 1);
        } else {
            return findMedianHelper(nums1, nums2, cut1 + 1, right);
        }
    }

    /**
     * Solution 4: K-th Element Approach
     * Time Complexity: O(log(min(m, n)))
     * Space Complexity: O(1)
     */
    public double findMedianSortedArraysKth(int[] nums1, int[] nums2) {
        int total = nums1.length + nums2.length;

        if (total % 2 == 1) {
            return findKthElement(nums1, nums2, total / 2 + 1);
        } else {
            return (findKthElement(nums1, nums2, total / 2) +
                    findKthElement(nums1, nums2, total / 2 + 1)) / 2.0;
        }
    }

    private double findKthElement(int[] nums1, int[] nums2, int k) {
        if (nums1.length > nums2.length) {
            return findKthElement(nums2, nums1, k);
        }

        int m = nums1.length;
        int n = nums2.length;

        int left = Math.max(0, k - n);
        int right = Math.min(k, m);

        while (left < right) {
            int cut1 = (left + right) / 2;
            int cut2 = k - cut1;

            if (nums1[cut1] < nums2[cut2 - 1]) {
                left = cut1 + 1;
            } else {
                right = cut1;
            }
        }

        int cut1 = left;
        int cut2 = k - cut1;

        int c1 = (cut1 == 0) ? Integer.MIN_VALUE : nums1[cut1 - 1];
        int c2 = (cut2 == 0) ? Integer.MIN_VALUE : nums2[cut2 - 1];

        return Math.max(c1, c2);
    }

    /**
     * Solution 5: Merge and Find (Brute Force - for comparison)
     * Time Complexity: O(m + n)
     * Space Complexity: O(m + n)
     * Note: This doesn't meet the O(log(m+n)) requirement but included for
     * completeness
     */
    public double findMedianSortedArraysMerge(int[] nums1, int[] nums2) {
        int m = nums1.length;
        int n = nums2.length;
        int[] merged = new int[m + n];

        int i = 0, j = 0, k = 0;

        // Merge arrays
        while (i < m && j < n) {
            if (nums1[i] <= nums2[j]) {
                merged[k++] = nums1[i++];
            } else {
                merged[k++] = nums2[j++];
            }
        }

        while (i < m) {
            merged[k++] = nums1[i++];
        }

        while (j < n) {
            merged[k++] = nums2[j++];
        }

        // Find median
        int total = m + n;
        if (total % 2 == 1) {
            return merged[total / 2];
        } else {
            return (merged[total / 2 - 1] + merged[total / 2]) / 2.0;
        }
    }

    /**
     * Solution 6: Two Pointers Without Extra Space
     * Time Complexity: O(m + n)
     * Space Complexity: O(1)
     * Note: This doesn't meet the O(log(m+n)) requirement but space-optimized
     */
    public double findMedianSortedArraysTwoPointers(int[] nums1, int[] nums2) {
        int m = nums1.length;
        int n = nums2.length;
        int total = m + n;

        int i = 0, j = 0;
        int prev = 0, curr = 0;

        // Find the median position(s)
        for (int count = 0; count <= total / 2; count++) {
            prev = curr;

            if (i < m && (j >= n || nums1[i] <= nums2[j])) {
                curr = nums1[i++];
            } else {
                curr = nums2[j++];
            }
        }

        if (total % 2 == 1) {
            return curr;
        } else {
            return (prev + curr) / 2.0;
        }
    }

    /**
     * Test method to demonstrate all solutions
     */
    public static void main(String[] args) {
        MedianTwoSortedArrays solution = new MedianTwoSortedArrays();

        // Test Case 1: [1,3], [2]
        int[] nums1_1 = { 1, 3 };
        int[] nums2_1 = { 2 };
        System.out.println("Arrays: [1,3], [2]");
        System.out.println("Optimal Binary Search: " + solution.findMedianSortedArrays(nums1_1, nums2_1));
        System.out.println("Detailed Binary Search: " + solution.findMedianSortedArraysDetailed(nums1_1, nums2_1));
        System.out.println("Recursive: " + solution.findMedianSortedArraysRecursive(nums1_1, nums2_1));
        System.out.println("K-th Element: " + solution.findMedianSortedArraysKth(nums1_1, nums2_1));
        System.out.println("Two Pointers: " + solution.findMedianSortedArraysTwoPointers(nums1_1, nums2_1));
        System.out.println();

        // Test Case 2: [1,2], [3,4]
        int[] nums1_2 = { 1, 2 };
        int[] nums2_2 = { 3, 4 };
        System.out.println("Arrays: [1,2], [3,4]");
        System.out.println("Optimal Binary Search: " + solution.findMedianSortedArrays(nums1_2, nums2_2));
        System.out.println("Detailed Binary Search: " + solution.findMedianSortedArraysDetailed(nums1_2, nums2_2));
        System.out.println("Recursive: " + solution.findMedianSortedArraysRecursive(nums1_2, nums2_2));
        System.out.println("K-th Element: " + solution.findMedianSortedArraysKth(nums1_2, nums2_2));
        System.out.println();

        // Test Case 3: Empty array
        int[] nums1_3 = {};
        int[] nums2_3 = { 1 };
        System.out.println("Arrays: [], [1]");
        System.out.println("Optimal Binary Search: " + solution.findMedianSortedArrays(nums1_3, nums2_3));
        System.out.println("Two Pointers: " + solution.findMedianSortedArraysTwoPointers(nums1_3, nums2_3));
        System.out.println();

        // Test Case 4: Different sizes
        int[] nums1_4 = { 1, 2 };
        int[] nums2_4 = { 3, 4, 5, 6 };
        System.out.println("Arrays: [1,2], [3,4,5,6]");
        System.out.println("Optimal Binary Search: " + solution.findMedianSortedArrays(nums1_4, nums2_4));
        System.out.println("Merge Approach: " + solution.findMedianSortedArraysMerge(nums1_4, nums2_4));
        System.out.println();

        // Test Case 5: One element each
        int[] nums1_5 = { 1 };
        int[] nums2_5 = { 2 };
        System.out.println("Arrays: [1], [2]");
        System.out.println("Optimal Binary Search: " + solution.findMedianSortedArrays(nums1_5, nums2_5));
        System.out.println("K-th Element: " + solution.findMedianSortedArraysKth(nums1_5, nums2_5));
    }

}

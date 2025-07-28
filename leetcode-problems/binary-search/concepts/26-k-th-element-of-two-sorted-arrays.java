/*
 * Problem Statement: Given two sorted arrays of size m and n respectively, you
 * are tasked with finding the element that would be at the kth position of the
 * final sorted array.
 */

import java.util.List;

class KthElementOfTwoSortedArrays {

    public int kthElement(List<Integer> first, List<Integer> second, int k) {
        int n1 = first.size(), n2 = second.size();
        if (n1 > n2) {
            return this.kthElement(second, first, k);
        }

        int left = k; // length of left half

        int low = Math.max(0, k - n2), high = Math.min(k, n1);
        while (low <= high) {
            int mid1 = low + (high - low) / 2;
            int mid2 = left - mid1;

            int left1 = mid1 > 0 ? first.get(mid1 - 1) : Integer.MIN_VALUE;
            int left2 = mid2 > 0 ? second.get(mid2 - 2) : Integer.MIN_VALUE;
            int right1 = mid1 < n1 ? first.get(mid1) : Integer.MAX_VALUE;
            int right2 = mid2 < n1 ? second.get(mid2) : Integer.MAX_VALUE;

            if (left1 <= right2 && left2 <= right1) {
                return Math.max(left1, left2);
            } else if (left1 > right2) {
                high = mid1 - 1;
            } else {
                low = mid1 + 1;
            }
        }

        return 0;
    }

}

class KthElementTwoSortedArrays {

    /**
     * BRUTE FORCE APPROACH - Merge and Find Kth Element
     * Algorithm:
     * 1. Merge both sorted arrays into a single sorted array
     * 2. Return the element at (k-1)th index (0-based indexing)
     * 
     * Time Complexity: O(m + n) - merge operation
     * Space Complexity: O(m + n) - for merged array
     */
    public static int findKthElementBruteForce(int[] arr1, int[] arr2, int k) {
        int m = arr1.length;
        int n = arr2.length;
        int[] merged = new int[m + n];

        int i = 0, j = 0, idx = 0;

        // Merge both arrays
        while (i < m && j < n) {
            if (arr1[i] <= arr2[j]) {
                merged[idx++] = arr1[i++];
            } else {
                merged[idx++] = arr2[j++];
            }
        }

        // Add remaining elements from arr1
        while (i < m) {
            merged[idx++] = arr1[i++];
        }

        // Add remaining elements from arr2
        while (j < n) {
            merged[idx++] = arr2[j++];
        }

        // Return kth element (k is 1-indexed)
        return merged[k - 1];
    }

    /**
     * BETTER APPROACH - Merge without Extra Space
     * Algorithm:
     * 1. Use two pointers to traverse both arrays simultaneously
     * 2. Keep track of current position in the merged sequence
     * 3. Stop when we reach the kth position
     * 4. Return the element at kth position
     * 
     * Time Complexity: O(k) - we only need to traverse till kth element
     * Space Complexity: O(1) - no extra space for merging
     */
    public static int findKthElementBetter(int[] arr1, int[] arr2, int k) {
        int m = arr1.length;
        int n = arr2.length;

        int i = 0, j = 0, count = 0;

        while (i < m && j < n) {
            count++;

            if (arr1[i] <= arr2[j]) {
                if (count == k)
                    return arr1[i];
                i++;
            } else {
                if (count == k)
                    return arr2[j];
                j++;
            }
        }

        // Process remaining elements from arr1
        while (i < m) {
            count++;
            if (count == k)
                return arr1[i];
            i++;
        }

        // Process remaining elements from arr2
        while (j < n) {
            count++;
            if (count == k)
                return arr2[j];
            j++;
        }

        return -1; // Should never reach here with valid input
    }

    /**
     * OPTIMAL APPROACH - Binary Search
     * Algorithm:
     * 1. Apply binary search on the smaller array (for efficiency)
     * 2. For each partition of smaller array, calculate corresponding partition of
     * larger array
     * 3. Check if the partition gives us exactly k elements in the left part
     * 4. Ensure the partition is valid (left elements ≤ right elements across both
     * arrays)
     * 5. Key insight: We need to partition both arrays such that:
     * - Left partition has exactly k elements
     * - max(left_part) ≤ min(right_part)
     * 
     * Time Complexity: O(log(min(m, n))) - binary search on smaller array
     * Space Complexity: O(1) - only using variables
     */
    public static int findKthElementOptimal(int[] arr1, int[] arr2, int k) {
        int m = arr1.length;
        int n = arr2.length;

        // Ensure arr1 is the smaller array for efficiency
        if (m > n) {
            return findKthElementOptimal(arr2, arr1, k);
        }

        // Edge case: if k > total elements
        if (k > m + n)
            return -1;

        // Binary search bounds
        int low = Math.max(0, k - n); // Minimum elements we must take from arr1
        int high = Math.min(k, m); // Maximum elements we can take from arr1

        while (low <= high) {
            int cut1 = (low + high) / 2; // Elements taken from arr1 for left partition
            int cut2 = k - cut1; // Elements taken from arr2 for left partition

            // Boundary elements
            int left1 = (cut1 == 0) ? Integer.MIN_VALUE : arr1[cut1 - 1];
            int left2 = (cut2 == 0) ? Integer.MIN_VALUE : arr2[cut2 - 1];
            int right1 = (cut1 == m) ? Integer.MAX_VALUE : arr1[cut1];
            int right2 = (cut2 == n) ? Integer.MAX_VALUE : arr2[cut2];

            // Check if partition is valid
            if (left1 <= right2 && left2 <= right1) {
                // Valid partition found, return the maximum of left part
                return Math.max(left1, left2);
            } else if (left1 > right2) {
                // Too many elements from arr1 in left partition
                high = cut1 - 1;
            } else {
                // Too few elements from arr1 in left partition
                low = cut1 + 1;
            }
        }

        return -1; // Should never reach here with valid input
    }

    /**
     * ALTERNATIVE OPTIMAL APPROACH - Using Recursion (Divide and Conquer)
     * This approach recursively eliminates half of one array at each step
     * 
     * Time Complexity: O(log(m) + log(n))
     * Space Complexity: O(log(m) + log(n)) - recursion stack
     */
    public static int findKthElementRecursive(int[] arr1, int[] arr2, int k) {
        return findKthHelper(arr1, 0, arr1.length - 1, arr2, 0, arr2.length - 1, k);
    }

    private static int findKthHelper(int[] arr1, int start1, int end1,
            int[] arr2, int start2, int end2, int k) {
        // Base cases
        if (start1 > end1) {
            return arr2[start2 + k - 1];
        }
        if (start2 > end2) {
            return arr1[start1 + k - 1];
        }
        if (k == 1) {
            return Math.min(arr1[start1], arr2[start2]);
        }

        // Find middle elements
        int mid1 = start1 + (end1 - start1) / 2;
        int mid2 = start2 + (end2 - start2) / 2;

        int midVal1 = arr1[mid1];
        int midVal2 = arr2[mid2];

        // Count of elements before mid1 and mid2
        int count1 = mid1 - start1 + 1;
        int count2 = mid2 - start2 + 1;

        if (count1 + count2 < k) {
            // kth element is in the right half
            if (midVal1 > midVal2) {
                return findKthHelper(arr1, start1, end1, arr2, mid2 + 1, end2, k - count2);
            } else {
                return findKthHelper(arr1, mid1 + 1, end1, arr2, start2, end2, k - count1);
            }
        } else {
            // kth element is in the left half
            if (midVal1 > midVal2) {
                return findKthHelper(arr1, start1, mid1 - 1, arr2, start2, end2, k);
            } else {
                return findKthHelper(arr1, start1, end1, arr2, start2, mid2 - 1, k);
            }
        }
    }

    // Helper method to demonstrate the binary search logic
    public static void demonstrateBinarySearchLogic(int[] arr1, int[] arr2, int k) {
        System.out.println("=== Binary Search Logic Demonstration ===");
        System.out.println("arr1: " + Arrays.toString(arr1));
        System.out.println("arr2: " + Arrays.toString(arr2));
        System.out.println("k = " + k + " (finding " + k + "th smallest element)");

        int m = arr1.length;
        int n = arr2.length;

        // Ensure arr1 is smaller for demonstration
        if (m > n) {
            int[] temp = arr1;
            arr1 = arr2;
            arr2 = temp;
            int tempLen = m;
            m = n;
            n = tempLen;
        }

        int low = Math.max(0, k - n);
        int high = Math.min(k, m);

        System.out.println("Binary search bounds: low = " + low + ", high = " + high);
        System.out.println("We need exactly " + k + " elements in left partition");
        System.out.println();

        System.out.println("Trying different partitions:");

        // Demonstrate a few partition attempts
        for (int cut1 = low; cut1 <= Math.min(high, low + 2); cut1++) {
            int cut2 = k - cut1;
            if (cut2 < 0 || cut2 > n)
                continue;

            int left1 = (cut1 == 0) ? Integer.MIN_VALUE : arr1[cut1 - 1];
            int left2 = (cut2 == 0) ? Integer.MIN_VALUE : arr2[cut2 - 1];
            int right1 = (cut1 == m) ? Integer.MAX_VALUE : arr1[cut1];
            int right2 = (cut2 == n) ? Integer.MAX_VALUE : arr2[cut2];

            System.out.printf("cut1=%d, cut2=%d: left1=%s, left2=%s, right1=%s, right2=%s",
                    cut1, cut2,
                    (left1 == Integer.MIN_VALUE) ? "-∞" : String.valueOf(left1),
                    (left2 == Integer.MIN_VALUE) ? "-∞" : String.valueOf(left2),
                    (right1 == Integer.MAX_VALUE) ? "+∞" : String.valueOf(right1),
                    (right2 == Integer.MAX_VALUE) ? "+∞" : String.valueOf(right2));

            boolean valid = left1 <= right2 && left2 <= right1;
            System.out.println(" -> " + (valid ? "VALID" : "INVALID"));

            if (valid) {
                System.out.println("   Kth element: " + Math.max(left1, left2));
                break;
            }
        }
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("=== Kth Element in Two Sorted Arrays ===\n");

        // Test case 1
        int[] arr1_1 = { 2, 3, 6, 7, 9 };
        int[] arr2_1 = { 1, 4, 8, 10 };
        int k1 = 5;
        System.out.println("Test Case 1:");
        System.out.println("arr1: " + Arrays.toString(arr1_1));
        System.out.println("arr2: " + Arrays.toString(arr2_1));
        System.out.println("k = " + k1);
        System.out.println("Merged array would be: [1, 2, 3, 4, 6, 7, 8, 9, 10]");
        System.out.println("Expected 5th element: 6");

        int result1_brute = findKthElementBruteForce(arr1_1, arr2_1, k1);
        int result1_better = findKthElementBetter(arr1_1, arr2_1, k1);
        int result1_optimal = findKthElementOptimal(arr1_1, arr2_1, k1);
        int result1_recursive = findKthElementRecursive(arr1_1, arr2_1, k1);

        System.out.println("Brute Force: " + result1_brute);
        System.out.println("Better: " + result1_better);
        System.out.println("Optimal: " + result1_optimal);
        System.out.println("Recursive: " + result1_recursive);
        System.out.println();

        demonstrateBinarySearchLogic(arr1_1, arr2_1, k1);

        // Test case 2
        int[] arr1_2 = { 100, 112, 256, 349, 770 };
        int[] arr2_2 = { 72, 86, 113, 119, 265, 445, 892 };
        int k2 = 7;
        System.out.println("Test Case 2:");
        System.out.println("arr1: " + Arrays.toString(arr1_2));
        System.out.println("arr2: " + Arrays.toString(arr2_2));
        System.out.println("k = " + k2);

        int result2_optimal = findKthElementOptimal(arr1_2, arr2_2, k2);
        System.out.println("7th smallest element: " + result2_optimal);
        System.out.println();

        // Test case 3: Edge case where k is near the end
        int[] arr1_3 = { 1, 2 };
        int[] arr2_3 = { 3, 4, 5, 6 };
        int k3 = 4;
        System.out.println("Test Case 3 (k near end):");
        System.out.println("arr1: " + Arrays.toString(arr1_3));
        System.out.println("arr2: " + Arrays.toString(arr2_3));
        System.out.println("k = " + k3);

        int result3_optimal = findKthElementOptimal(arr1_3, arr2_3, k3);
        System.out.println("4th smallest element: " + result3_optimal);
        System.out.println();

        // Performance comparison
        System.out.println("=== Time Complexity Analysis ===");
        System.out.println("Brute Force: O(m + n) time, O(m + n) space");
        System.out.println("Better: O(k) time, O(1) space");
        System.out.println("Optimal (Binary Search): O(log(min(m, n))) time, O(1) space");
        System.out.println("Recursive: O(log(m) + log(n)) time, O(log(m) + log(n)) space");
        System.out.println();

        System.out.println("=== Key Insights ===");
        System.out.println("1. Binary search works by partitioning arrays systematically");
        System.out.println("2. We always search on the smaller array for efficiency");
        System.out.println("3. Valid partition: max(left_part) ≤ min(right_part)");
        System.out.println("4. Left partition always has exactly k elements");
        System.out.println("5. Kth element is the maximum of the left partition");
        System.out.println("6. Binary search bounds: [max(0, k-n), min(k, m)]");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM UNDERSTANDING:
 * - Given two sorted arrays, find the kth smallest element in their combined
 * sorted sequence
 * - k is 1-indexed (1st element, 2nd element, etc.)
 * - We don't need to actually merge the arrays
 * 
 * BRUTE FORCE APPROACH:
 * - Merge both arrays completely
 * - Return element at (k-1)th index
 * - Simple but uses extra space
 * - Time: O(m+n), Space: O(m+n)
 * 
 * BETTER APPROACH:
 * - Merge conceptually using two pointers
 * - Stop as soon as we reach the kth element
 * - No extra space needed
 * - Time: O(k), Space: O(1)
 * - Better when k is small
 * 
 * OPTIMAL APPROACH (Binary Search):
 * - Key insight: Partition both arrays such that left part has exactly k
 * elements
 * - Binary search on the smaller array for efficiency
 * - For each partition attempt:
 * 1. cut1 = elements from arr1 in left partition
 * 2. cut2 = k - cut1 = elements from arr2 in left partition
 * 3. Check if partition is valid: max(left) ≤ min(right)
 * 4. If valid, return max(left1, left2)
 * 
 * - Binary search bounds:
 * - low = max(0, k-n): minimum elements we must take from arr1
 * - high = min(k, m): maximum elements we can take from arr1
 * 
 * PARTITION LOGIC:
 * - We need exactly k elements in the left partition
 * - If cut1 + cut2 = k, and max(left_part) ≤ min(right_part)
 * - Then max(left_part) is our kth element
 * 
 * BOUNDARY CONDITIONS:
 * - cut1 can range from max(0, k-n) to min(k, m)
 * - This ensures cut2 stays within valid bounds [0, n]
 * 
 * RECURSIVE APPROACH:
 * - Divide and conquer strategy
 * - At each step, eliminate half of one array
 * - Recursively search in the remaining space
 * - Time: O(log(m) + log(n))
 * 
 * The binary search approach is optimal with O(log(min(m,n))) complexity!
 * 
 * COMPARISON WITH MEDIAN PROBLEM:
 * - Median: k = (m+n+1)/2 (middle element)
 * - Kth element: any k from 1 to m+n
 * - Same binary search technique, different target position
 */

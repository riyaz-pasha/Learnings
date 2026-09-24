import java.util.Arrays;

/**
 * Problem: Merge Sorted Array
 * 
 * Statement:
 * You are given two integer arrays, nums1 and nums2, both sorted in non-decreasing order.
 * You are also given two integers, m and n, representing the number of elements in nums1 and nums2.
 * Merge nums2 into nums1 as one sorted array in non-decreasing order.
 * 
 * Constraints:
 * - nums1.length == m + n
 * - nums2.length == n
 * - 0 <= m, n <= 200
 * - 1 <= m + n <= 200
 * - -10^9 <= nums1[i], nums2[j] <= 10^9
 */
class MergeSortedArrays {

    /* ============================================================================
     * APPROACH 1: Brute Force (Merge and Sort)
     * ============================================================================
     * Explanation:
     * The simplest way to solve this is to ignore the fact that the arrays are 
     * already sorted. We can just copy all elements of nums2 into the empty 
     * slots at the end of nums1, and then sort the entire nums1 array.
     * 
     * Time Complexity: O((m+n) log(m+n)) due to the sorting step.
     * Space Complexity: O(1) auxiliary space (or O(m+n) depending on the sort implementation).
     */
    public static void mergeBruteForce(int[] nums1, int m, int[] nums2, int n) {
        // Copy elements from nums2 into the end of nums1
        System.arraycopy(nums2, 0, nums1, m, n);
        
        // Sort the entire array
        Arrays.sort(nums1);
    }

    /* ============================================================================
     * APPROACH 2: Two Pointers (Left to Right) with Extra Space
     * ============================================================================
     * Explanation:
     * Since both arrays are already sorted, we can use two pointers to compare 
     * elements from nums1 and nums2 one by one. However, if we do this from 
     * left to right directly in nums1, we will overwrite elements in nums1 
     * before we have a chance to read them. To fix this, we create a copy of 
     * the valid elements in nums1 first.
     * 
     * Time Complexity: O(m + n)
     * Space Complexity: O(m) to store the copy of nums1.
     */
    public static void mergeWithExtraSpace(int[] nums1, int m, int[] nums2, int n) {
        // Modern Java 'var' for local variables
        var nums1Copy = Arrays.copyOf(nums1, m);
        
        int p1 = 0; // Pointer for nums1Copy
        int p2 = 0; // Pointer for nums2
        int p = 0;  // Pointer for nums1 (where we are writing)

        // Compare elements and insert the smallest into nums1
        while (p1 < m && p2 < n) {
            if (nums1Copy[p1] <= nums2[p2]) {
                nums1[p++] = nums1Copy[p1++];
            } else {
                nums1[p++] = nums2[p2++];
            }
        }

        // If there are leftover elements in nums1Copy, add them
        while (p1 < m) {
            nums1[p++] = nums1Copy[p1++];
        }

        // If there are leftover elements in nums2, add them
        while (p2 < n) {
            nums1[p++] = nums2[p2++];
        }
    }

    /* ============================================================================
     * APPROACH 3: Optimal Two Pointers (Right to Left)
     * ============================================================================
     * Explanation:
     * To achieve O(1) space complexity, we can use two pointers, but start from 
     * the END of the arrays. Since the end of nums1 is empty (filled with 0s), 
     * we can safely overwrite these positions with the largest elements without 
     * losing any data.
     * 
     * Visual Example:
     * nums1 = [1, 2, 3, 0, 0, 0], m = 3
     * nums2 = [2, 5, 6],          n = 3
     * 
     * Initial State:
     * [1, 2, 3, 0, 0, 0]      [2, 5, 6]
     *        ^        ^              ^
     *        p1       p              p2
     * 
     * Step 1: 3 vs 6. 6 is larger, place at p.
     * [1, 2, 3, 0, 0, 6]      [2, 5, 6]
     *        ^     ^              ^
     *        p1    p              p2
     * 
     * Step 2: 3 vs 5. 5 is larger, place at p.
     * [1, 2, 3, 0, 5, 6]      [2, 5, 6]
     *        ^  ^                 ^
     *        p1 p                 p2
     * 
     * ...and so on until p2 < 0.
     * 
     * Time Complexity: O(m + n)
     * Space Complexity: O(1) strictly in-place.
     */
    public static void mergeOptimal(int[] nums1, int m, int[] nums2, int n) {
        int p1 = m - 1;       // Pointer to the last valid element in nums1
        int p2 = n - 1;       // Pointer to the last element in nums2
        int p = m + n - 1;    // Pointer to the last position in nums1

        // Traverse backwards, picking the larger element each time
        while (p1 >= 0 && p2 >= 0) {
            if (nums1[p1] > nums2[p2]) {
                nums1[p] = nums1[p1];
                p1--;
            } else {
                nums1[p] = nums2[p2];
                p2--;
            }
            p--;
        }

        // If nums2 still has elements left, copy them over.
        // (If nums1 has elements left, they are already in the correct place)
        while (p2 >= 0) {
            nums1[p] = nums2[p2];
            p2--;
            p--;
        }
    }

    /* ============================================================================
     * TESTING / MAIN METHOD
     * ============================================================================
     */
    
    /**
     * Using Java 14+ 'record' feature to cleanly define our test cases.
     * A record automatically generates a constructor, getters, equals(), 
     * hashCode(), and toString() methods.
     */
    public record TestCase(int[] nums1, int m, int[] nums2, int n, int[] expected) {
        // Deep copy helper so our multiple algorithms don't mutate the same initial array
        public int[] getNums1Copy() {
            return Arrays.copyOf(nums1, nums1.length);
        }
    }

    public static void main(String[] args) {
        // Setup Test Cases
        var testCases = Arrays.asList(
            new TestCase(
                new int[]{1, 2, 3, 0, 0, 0}, 3, 
                new int[]{2, 5, 6}, 3, 
                new int[]{1, 2, 2, 3, 5, 6}
            ),
            new TestCase(
                new int[]{1}, 1, 
                new int[]{}, 0, 
                new int[]{1}
            ),
            new TestCase(
                new int[]{0}, 0, 
                new int[]{1}, 1, 
                new int[]{1}
            ),
            new TestCase(
                new int[]{4, 5, 6, 0, 0, 0}, 3, 
                new int[]{1, 2, 3}, 3, 
                new int[]{1, 2, 3, 4, 5, 6}
            )
        );

        System.out.println("Running tests for all 3 approaches...\n");

        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            System.out.println("Test Case " + (i + 1) + ":");
            System.out.println("Initial nums1: " + Arrays.toString(tc.nums1));
            System.out.println("Initial nums2: " + Arrays.toString(tc.nums2));

            // Test Approach 1
            var arr1 = tc.getNums1Copy();
            mergeBruteForce(arr1, tc.m, tc.nums2, tc.n);
            boolean pass1 = Arrays.equals(arr1, tc.expected);

            // Test Approach 2
            var arr2 = tc.getNums1Copy();
            mergeWithExtraSpace(arr2, tc.m, tc.nums2, tc.n);
            boolean pass2 = Arrays.equals(arr2, tc.expected);

            // Test Approach 3
            var arr3 = tc.getNums1Copy();
            mergeOptimal(arr3, tc.m, tc.nums2, tc.n);
            boolean pass3 = Arrays.equals(arr3, tc.expected);

            System.out.println("  Brute Force    : " + (pass1 ? "PASS" : "FAIL") + " -> " + Arrays.toString(arr1));
            System.out.println("  Extra Space    : " + (pass2 ? "PASS" : "FAIL") + " -> " + Arrays.toString(arr2));
            System.out.println("  Optimal        : " + (pass3 ? "PASS" : "FAIL") + " -> " + Arrays.toString(arr3));
            System.out.println("-".repeat(50));
        }
    }
}

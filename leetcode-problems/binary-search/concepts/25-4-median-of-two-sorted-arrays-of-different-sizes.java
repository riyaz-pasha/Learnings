/*
 * Problem Statement: Given two sorted arrays arr1 and arr2 of size m and n
 * respectively, return the median of the two sorted arrays. The median is
 * defined as the middle value of a sorted list of numbers. In case the length
 * of the list is even, the median is the average of the two middle elements.
 * 
 * Examples
 * 
 * Example 1:
 * Input Format: n1 = 3, arr1[] = {2,4,6}, n2 = 3, arr2[] = {1,3,5}
 * Result: 3.5
 * Explanation: The array after merging 'a' and 'b' will be { 1, 2, 3, 4, 5, 6
 * }. As the length of the merged list is even, the median is the average of the
 * two middle elements. Here two medians are 3 and 4. So the median will be the
 * average of 3 and 4, which is 3.5.
 * 
 * Example 2:
 * Input Format: n1 = 3, arr1[] = {2,4,6}, n2 = 2, arr2[] = {1,3}
 * Result: 3
 * Explanation: The array after merging 'a' and 'b' will be { 1, 2, 3, 4, 6 }.
 * The median is simply 3.
 */

import java.util.ArrayList;
import java.util.List;

class MedianOfTwoSortedArraysOfDifferentSizes {

    /*
     * Time Complexity: O(n1+n2), where n1 and n2 are the sizes of the given arrays.
     * Reason: We traverse through both arrays linearly.
     * 
     * Space Complexity: O(n1+n2), where n1 and n2 are the sizes of the given
     * arrays.
     * Reason: We are using an extra array of size (n1+n2) to solve this problem.
     */
    public double medianBruteForceSolution(List<Integer> first, List<Integer> second) {
        int n1 = first.size();
        int n2 = second.size();

        List<Integer> mergedArray = new ArrayList<>();

        int firstPointer = 0, secondPointer = 0;
        while (firstPointer < n1 && secondPointer < n2) {
            if (first.get(firstPointer) < second.get(secondPointer)) {
                mergedArray.add(first.get(firstPointer++));
            } else {
                mergedArray.add(second.get(secondPointer++));
            }
        }
        while (firstPointer < n1) {
            mergedArray.add(first.get(firstPointer++));
        }
        while (secondPointer < n2) {
            mergedArray.add(second.get(secondPointer++));
        }

        int n = n1 + n2;
        if (n % 2 == 1) {
            return mergedArray.get(n / 2);
        }
        return (mergedArray.get(n / 2) + mergedArray.get((n / 2) - 1)) / 2.0;
    }

    /*
     * Time Complexity: O(n1+n2), where n1 and n2 are the sizes of the given arrays.
     * Reason: We traverse through both arrays linearly.
     * 
     * Space Complexity: O(1), as we are not using any extra space to solve this
     * problem.
     */
    public double medianBetterSolution(List<Integer> first, List<Integer> second) {
        int n1 = first.size(), n2 = second.size();
        int n = n1 + n2;

        int index2 = n / 2;
        int index1 = index2 - 1;
        int index2Element = -1, index1Element = -1;

        int firstPointer = 0, secondPointer = 0, mergedArrayPointer = 0;
        while (firstPointer < n1 && secondPointer < n2) {
            if (first.get(firstPointer) < second.get(secondPointer)) {
                if (mergedArrayPointer == index1) {
                    index1Element = first.get(firstPointer);
                }
                if (mergedArrayPointer == index2) {
                    index2Element = first.get(firstPointer);
                }
                mergedArrayPointer++;
                firstPointer++;
            } else {
                if (mergedArrayPointer == index1) {
                    index1Element = second.get(secondPointer);
                }
                if (mergedArrayPointer == index2) {
                    index2Element = second.get(secondPointer);
                }
                mergedArrayPointer++;
                secondPointer++;
            }
        }

        while (firstPointer < n1) {
            if (mergedArrayPointer == index1) {
                index1Element = first.get(firstPointer);
            }
            if (mergedArrayPointer == index2) {
                index2Element = first.get(firstPointer);
            }
            mergedArrayPointer++;
            firstPointer++;
        }

        while (secondPointer < n2) {
            if (mergedArrayPointer == index1) {
                index1Element = second.get(secondPointer);
            }
            if (mergedArrayPointer == index2) {
                index2Element = second.get(secondPointer);
            }
            mergedArrayPointer++;
            secondPointer++;
        }

        if (n % 2 == 1) {
            return index2Element;
        }

        return (index1Element + index2Element) / 2.0;
    }

    /*
     * Time Complexity: O(log(min(n1,n2))), where n1 and n2 are the sizes of two
     * given arrays.
     * Reason: We are applying binary search on the range [0, min(n1, n2)].
     * 
     * Space Complexity: O(1) as no extra space is used.
     */
    public double medianOptimalSolution(List<Integer> first, List<Integer> second) {

        int n1 = first.size();
        int n2 = second.size();

        // ---------------------------------------------------------
        // IMPORTANT:
        // Always binary search on the smaller array.
        // This ensures log(min(n1,n2)) complexity and avoids invalid mid2.
        // ---------------------------------------------------------
        if (n1 > n2) {
            return medianOptimalSolution(second, first);
        }

        int total = n1 + n2;

        // ---------------------------------------------------------
        // We want to split both arrays into:
        //
        // left part  = (total+1)/2 elements
        // right part = remaining
        //
        // Why (total+1)/2?
        // - Works for both even and odd total lengths.
        // - If total is odd, left part contains one extra element.
        //
        // Example:
        // total = 5 -> left = 3
        // total = 6 -> left = 3
        // ---------------------------------------------------------
        int leftSize = (total + 1) / 2;

        // ---------------------------------------------------------
        // Binary search space:
        //
        // mid1 = how many elements we take from array "first" into left half
        // mid2 = how many elements we take from array "second" into left half
        //
        // mid1 ranges from 0 to n1 (count, not index)
        //
        // mid2 is derived:
        // mid2 = leftSize - mid1
        // ---------------------------------------------------------
        int low = 0;
        int high = n1;

        while (low <= high) {

            // mid1 = number of elements taken from first array into left partition
            int mid1 = low + (high - low) / 2;

            // mid2 = number of elements taken from second array into left partition
            int mid2 = leftSize - mid1;

            // ---------------------------------------------------------
            // Now we define boundary values around the partition:
            //
            // left1  = last element in first's left partition
            // right1 = first element in first's right partition
            //
            // left2  = last element in second's left partition
            // right2 = first element in second's right partition
            //
            // Example:
            // first  = [1,3,8,9]
            // second = [7,11,18,19]
            //
            // Suppose mid1=2 => left part takes [1,3]
            // then left1=3, right1=8
            //
            // Suppose mid2=2 => left part takes [7,11]
            // then left2=11, right2=18
            // ---------------------------------------------------------

            // If mid1 == 0, first contributes nothing to left side,
            // so left1 is -infinity
            int left1 = (mid1 > 0) ? first.get(mid1 - 1) : Integer.MIN_VALUE;

            // If mid2 == 0, second contributes nothing to left side,
            // so left2 is -infinity
            int left2 = (mid2 > 0) ? second.get(mid2 - 1) : Integer.MIN_VALUE;

            // If mid1 == n1, first contributes everything to left side,
            // so right1 is +infinity
            int right1 = (mid1 < n1) ? first.get(mid1) : Integer.MAX_VALUE;

            // If mid2 == n2, second contributes everything to left side,
            // so right2 is +infinity
            int right2 = (mid2 < n2) ? second.get(mid2) : Integer.MAX_VALUE;

            // ---------------------------------------------------------
            // Correct partition condition:
            //
            // Every element in left half must be <= every element in right half.
            //
            // That means:
            // left1 <= right2
            // left2 <= right1
            //
            // If both conditions hold -> partition is correct.
            // ---------------------------------------------------------
            if (left1 <= right2 && left2 <= right1) {

                // ---------------------------------------------------------
                // If total length is odd:
                // median is the maximum element in the left half.
                //
                // Example:
                // merged = [1,3,7,8,9]
                // left half has 3 elements -> median is max(left half)
                // ---------------------------------------------------------
                if (total % 2 == 1) {
                    return Math.max(left1, left2);
                }

                // ---------------------------------------------------------
                // If total length is even:
                // median is average of:
                // max(left half) and min(right half)
                //
                // Example:
                // merged = [1,3,7,8,9,11]
                // median = (7+8)/2
                // ---------------------------------------------------------
                return (Math.max(left1, left2) + Math.min(right1, right2)) / 2.0;
            }

            // ---------------------------------------------------------
            // If partition is NOT correct:
            //
            // Case 1: left1 > right2
            // That means we took too many elements from first array.
            // So move left in first array:
            // high = mid1 - 1
            // ---------------------------------------------------------
            else if (left1 > right2) {
                high = mid1 - 1;
            }

            // ---------------------------------------------------------
            // Case 2: left2 > right1
            // That means we took too few elements from first array.
            // So move right in first array:
            // low = mid1 + 1
            // ---------------------------------------------------------
            else {
                low = mid1 + 1;
            }
        }

        // For valid sorted inputs, we should never reach here
        return 0;
    }


}

class MedianTwoSortedArrays {

    /**
     * BRUTE FORCE APPROACH - Merge and Find Median
     * Algorithm:
     * 1. Merge both sorted arrays into a single sorted array
     * 2. Find the median from the merged array
     * 3. If total length is odd, return middle element
     * 4. If total length is even, return average of two middle elements
     * 
     * Time Complexity: O(m + n) - merge operation
     * Space Complexity: O(m + n) - for merged array
     */
    public static double findMedianBruteForce(int[] arr1, int[] arr2) {
        int m = arr1.length;
        int n = arr2.length;
        int[] merged = new int[m + n];

        int i = 0, j = 0, k = 0;

        // Merge both arrays
        while (i < m && j < n) {
            if (arr1[i] <= arr2[j]) {
                merged[k++] = arr1[i++];
            } else {
                merged[k++] = arr2[j++];
            }
        }

        // Add remaining elements from arr1
        while (i < m) {
            merged[k++] = arr1[i++];
        }

        // Add remaining elements from arr2
        while (j < n) {
            merged[k++] = arr2[j++];
        }

        // Find median
        int totalLength = m + n;
        if (totalLength % 2 == 1) {
            return merged[totalLength / 2];
        } else {
            int mid1 = totalLength / 2 - 1;
            int mid2 = totalLength / 2;
            return (merged[mid1] + merged[mid2]) / 2.0;
        }
    }

    /**
     * BETTER APPROACH - Merge without Extra Space
     * Algorithm:
     * 1. Use two pointers to traverse both arrays simultaneously
     * 2. Keep track of current position in the merged sequence (without actually
     * merging)
     * 3. Stop when we reach the median position(s)
     * 4. Track the elements at median positions
     * 
     * Time Complexity: O(m + n) - but we can stop at median position
     * Space Complexity: O(1) - no extra space for merging
     */
    public static double findMedianBetter(int[] arr1, int[] arr2) {
        int m = arr1.length;
        int n = arr2.length;
        int totalLength = m + n;

        int i = 0, j = 0, count = 0;
        int median1 = -1, median2 = -1;

        // Find the positions we need to track
        int pos1 = totalLength / 2 - 1; // For even length, first median position
        int pos2 = totalLength / 2; // For odd length, median position; for even, second median position

        while (i < m && j < n) {
            int current;
            if (arr1[i] <= arr2[j]) {
                current = arr1[i++];
            } else {
                current = arr2[j++];
            }

            if (count == pos1)
                median1 = current;
            if (count == pos2)
                median2 = current;

            count++;
            if (count > pos2)
                break; // We have found both medians
        }

        // Process remaining elements if needed
        while (i < m && count <= pos2) {
            if (count == pos1)
                median1 = arr1[i];
            if (count == pos2)
                median2 = arr1[i];
            i++;
            count++;
        }

        while (j < n && count <= pos2) {
            if (count == pos1)
                median1 = arr2[j];
            if (count == pos2)
                median2 = arr2[j];
            j++;
            count++;
        }

        // Calculate result
        if (totalLength % 2 == 1) {
            return median2; // For odd length, median2 is at the middle position
        } else {
            return (median1 + median2) / 2.0; // For even length, average of both
        }
    }

    /**
     * OPTIMAL APPROACH - Binary Search
     * Algorithm:
     * 1. Apply binary search on the smaller array (for efficiency)
     * 2. For each partition of smaller array, calculate corresponding partition of
     * larger array
     * 3. Check if the partition is valid (left elements ≤ right elements across
     * both arrays)
     * 4. If valid, calculate median; if not, adjust partition
     * 5. Key insight: We need to partition both arrays such that:
     * - Left partition has (m+n+1)/2 elements
     * - max(left_part) ≤ min(right_part)
     * 
     * Time Complexity: O(log(min(m, n))) - binary search on smaller array
     * Space Complexity: O(1) - only using variables
     */
    public static double findMedianOptimal(int[] arr1, int[] arr2) {
        int m = arr1.length;
        int n = arr2.length;

        // Ensure arr1 is the smaller array for efficiency
        if (m > n) {
            return findMedianOptimal(arr2, arr1);
        }

        int low = 0, high = m;
        int totalLeft = (m + n + 1) / 2; // Number of elements in left partition

        while (low <= high) {
            int cut1 = (low + high) / 2; // Partition position in arr1
            int cut2 = totalLeft - cut1; // Partition position in arr2

            // Elements at partition boundaries
            int left1 = (cut1 == 0) ? Integer.MIN_VALUE : arr1[cut1 - 1];
            int left2 = (cut2 == 0) ? Integer.MIN_VALUE : arr2[cut2 - 1];
            int right1 = (cut1 == m) ? Integer.MAX_VALUE : arr1[cut1];
            int right2 = (cut2 == n) ? Integer.MAX_VALUE : arr2[cut2];

            // Check if partition is valid
            if (left1 <= right2 && left2 <= right1) {
                // Valid partition found, calculate median
                if ((m + n) % 2 == 1) {
                    // Odd total length
                    return Math.max(left1, left2);
                } else {
                    // Even total length
                    return (Math.max(left1, left2) + Math.min(right1, right2)) / 2.0;
                }
            } else if (left1 > right2) {
                // Too many elements from arr1 in left partition
                high = cut1 - 1;
            } else {
                // Too few elements from arr1 in left partition
                low = cut1 + 1;
            }
        }

        return 1.0; // Should never reach here with valid input
    }

    // Helper method to demonstrate the binary search logic
    public static void demonstrateBinarySearchLogic(int[] arr1, int[] arr2) {
        System.out.println("=== Binary Search Logic Demonstration ===");
        System.out.println("arr1: " + Arrays.toString(arr1));
        System.out.println("arr2: " + Arrays.toString(arr2));

        int m = arr1.length;
        int n = arr2.length;
        int totalLeft = (m + n + 1) / 2;

        System.out.println("Total elements needed in left partition: " + totalLeft);
        System.out.println("Total length: " + (m + n) + " (is " + ((m + n) % 2 == 1 ? "odd" : "even") + ")");
        System.out.println();

        System.out.println("Trying different partitions:");

        // Demonstrate a few partition attempts
        for (int cut1 = 0; cut1 <= Math.min(m, 3); cut1++) {
            int cut2 = totalLeft - cut1;
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
                if ((m + n) % 2 == 1) {
                    System.out.println("   Median: " + Math.max(left1, left2));
                } else {
                    double median = (Math.max(left1, left2) + Math.min(right1, right2)) / 2.0;
                    System.out.println("   Median: " + median);
                }
                break;
            }
        }
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("=== Median of Two Sorted Arrays ===\n");

        // Test case 1: Even total length
        int[] arr1_1 = { 2, 4, 6 };
        int[] arr2_1 = { 1, 3, 5 };
        System.out.println("Test Case 1 (Even total length):");
        System.out.println("arr1: " + Arrays.toString(arr1_1));
        System.out.println("arr2: " + Arrays.toString(arr2_1));
        System.out.println("Expected: 3.5");

        double result1_brute = findMedianBruteForce(arr1_1, arr2_1);
        double result1_better = findMedianBetter(arr1_1, arr2_1);
        double result1_optimal = findMedianOptimal(arr1_1, arr2_1);

        System.out.printf("Brute Force: %.1f\n", result1_brute);
        System.out.printf("Better: %.1f\n", result1_better);
        System.out.printf("Optimal: %.1f\n", result1_optimal);
        System.out.println();

        demonstrateBinarySearchLogic(arr1_1, arr2_1);

        // Test case 2: Odd total length
        int[] arr1_2 = { 2, 4, 6 };
        int[] arr2_2 = { 1, 3 };
        System.out.println("Test Case 2 (Odd total length):");
        System.out.println("arr1: " + Arrays.toString(arr1_2));
        System.out.println("arr2: " + Arrays.toString(arr2_2));
        System.out.println("Expected: 3.0");

        double result2_brute = findMedianBruteForce(arr1_2, arr2_2);
        double result2_better = findMedianBetter(arr1_2, arr2_2);
        double result2_optimal = findMedianOptimal(arr1_2, arr2_2);

        System.out.printf("Brute Force: %.1f\n", result2_brute);
        System.out.printf("Better: %.1f\n", result2_better);
        System.out.printf("Optimal: %.1f\n", result2_optimal);
        System.out.println();

        // Test case 3: One array much smaller
        int[] arr1_3 = { 1, 3 };
        int[] arr2_3 = { 2, 4, 5, 6, 7, 8 };
        System.out.println("Test Case 3 (Unequal sizes):");
        System.out.println("arr1: " + Arrays.toString(arr1_3));
        System.out.println("arr2: " + Arrays.toString(arr2_3));

        double result3_optimal = findMedianOptimal(arr1_3, arr2_3);
        System.out.printf("Optimal: %.1f\n", result3_optimal);
        System.out.println();

        // Performance comparison
        System.out.println("=== Time Complexity Analysis ===");
        System.out.println("Brute Force: O(m + n) time, O(m + n) space");
        System.out.println("Better: O(m + n) time, O(1) space");
        System.out.println("Optimal: O(log(min(m, n))) time, O(1) space");
        System.out.println();

        System.out.println("=== Key Insights ===");
        System.out.println("1. Binary search works because we can partition arrays systematically");
        System.out.println("2. We always search on the smaller array for efficiency");
        System.out.println("3. Valid partition: max(left_part) ≤ min(right_part)");
        System.out.println("4. Left partition always has (m+n+1)/2 elements");
        System.out.println("5. This ensures the median is always in the correct position");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * BRUTE FORCE APPROACH:
 * - Merge both arrays completely
 * - Find median from merged array
 * - Simple but uses extra space
 * - Time: O(m+n), Space: O(m+n)
 * 
 * BETTER APPROACH:
 * - Merge conceptually without extra space
 * - Track only the elements we need for median calculation
 * - Stop early once we find median positions
 * - Time: O(m+n), Space: O(1)
 * 
 * OPTIMAL APPROACH (Binary Search):
 * - Key insight: We don't need to merge arrays, just find the right partition
 * - Partition both arrays such that:
 * 1. Left partition has exactly (m+n+1)/2 elements
 * 2. Every element in left partition ≤ every element in right partition
 * 
 * - Binary search on the smaller array for efficiency
 * - For each partition of smaller array, calculate corresponding partition of
 * larger array
 * - Check if partition is valid: max(left_part) ≤ min(right_part)
 * - If valid, calculate median; if not, adjust partition
 * 
 * PARTITION LOGIC:
 * - cut1: number of elements taken from arr1 for left partition
 * - cut2: number of elements taken from arr2 for left partition
 * - cut2 = totalLeft - cut1 (where totalLeft = (m+n+1)/2)
 * 
 * BOUNDARY ELEMENTS:
 * - left1 = arr1[cut1-1] (rightmost element of arr1's left part)
 * - right1 = arr1[cut1] (leftmost element of arr1's right part)
 * - left2 = arr2[cut2-1] (rightmost element of arr2's left part)
 * - right2 = arr2[cut2] (leftmost element of arr2's right part)
 * 
 * VALIDITY CHECK:
 * - Valid partition: left1 ≤ right2 AND left2 ≤ right1
 * - If left1 > right2: too many elements from arr1 in left, reduce cut1
 * - If left2 > right1: too few elements from arr1 in left, increase cut1
 * 
 * MEDIAN CALCULATION:
 * - Odd total length: max(left1, left2)
 * - Even total length: (max(left1, left2) + min(right1, right2)) / 2
 * 
 * The binary search approach is optimal with O(log(min(m,n))) time complexity!
 */

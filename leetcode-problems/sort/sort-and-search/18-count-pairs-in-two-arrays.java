/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given two arrays, `nums1` and `nums2`, of the same length `n`.
 * We need to find the total number of index pairs (i, j) such that i < j and 
 * the sum of the elements from nums1 is strictly greater than the sum of the 
 * corresponding elements from nums2.
 * 
 * Mathematically: nums1[i] + nums1[j] > nums2[i] + nums2[j]
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the elements in the arrays be zero or negative? 
 *    (Constraints say positive integers >= 1).
 * 2. Does the order of (i, j) matter? 
 *    (The condition is i < j, which means we are looking for unique pairs / 
 *     combinations of 2 elements, not permutations).
 * 3. Can the total count of valid pairs exceed the limit of a 32-bit integer?
 *    (Max N = 1000. Total pairs = 1000 * 999 / 2 = 499,500. This easily fits in 
 *     an `int`. However, if N was up to 10^5, the max pairs would be ~5*10^9, 
 *     which requires a `long` to avoid overflow. We should use `long` to be safe).
 * 4. Is it possible for both arrays to be identical? 
 *    (Yes, in which case no pair would have a strictly greater sum, return 0).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (The "Obvious" Baseline):
 *    - Use two nested loops to generate all pairs (i, j) with i < j.
 *    - Check the condition `nums1[i] + nums1[j] > nums2[i] + nums2[j]`.
 *    - Time: O(N^2), Space: O(1). 
 *    - With N <= 10^3, N^2 is 10^6, which will easily pass. But an interviewer 
 *      will definitely ask, "Can we do better?"
 * 
 * 2. Algebraic Manipulation + Sorting + Two Pointers (The Optimal "Aha!" Moment):
 *    - Let's look at the inequality: 
 *      nums1[i] + nums1[j] > nums2[i] + nums2[j]
 *    - Rearrange it to group the 'i' terms on one side and 'j' terms on the other:
 *      nums1[i] - nums2[i] > nums2[j] - nums1[j]
 *      nums1[i] - nums2[i] > -(nums1[j] - nums2[j])
 *      (nums1[i] - nums2[i]) + (nums1[j] - nums2[j]) > 0
 *    - Let's define a new array `diff` where diff[k] = nums1[k] - nums2[k].
 *    - The condition dramatically simplifies to: diff[i] + diff[j] > 0.
 *    
 *    - Wait, what about the `i < j` constraint? 
 *      Since we just need to find pairs of elements whose sum is > 0, their 
 *      original index order DOES NOT MATTER. Any valid pair in a sorted array 
 *      represents exactly one valid unique pair in the original array!
 *    
 *    - We can calculate the `diff` array, sort it, and use Two Pointers 
 *      (left and right) to count pairs that sum to > 0.
 *    - Time: O(N log N), Space: O(N).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Optimal Two Pointers Approach)
 * ============================================================================
 * Example: nums1 = [2, 1, 2, 1], nums2 = [1, 2, 1, 2]
 * 
 * 1. Calculate diff array (nums1[k] - nums2[k]):
 *    diff = [1, -1, 1, -1]
 * 
 * 2. Sort the diff array:
 *    diff = [-1, -1, 1, 1]
 * 
 * 3. Two Pointers: left = 0, right = 3 (N-1)
 *    - Iteration 1: diff[0] + diff[3] = (-1) + 1 = 0
 *      0 is not > 0. Sum is too small. left++ -> left = 1.
 * 
 *    - Iteration 2: diff[1] + diff[3] = (-1) + 1 = 0
 *      Sum is too small. left++ -> left = 2.
 * 
 *    - Iteration 3: diff[2] + diff[3] = 1 + 1 = 2
 *      2 > 0. VALID! 
 *      Since diff array is sorted, if diff[left] + diff[right] > 0, then 
 *      diff[any_index > left] + diff[right] will ALSO be > 0!
 *      Number of valid pairs with `right` element = right - left = 3 - 2 = 1.
 *      count += 1.
 *      Now decrement right to find pairs for the next largest element. right-- -> 2.
 * 
 *    - Iteration 4: left = 2, right = 2. 
 *      left is no longer strictly < right. Loop terminates.
 * 
 * Final count = 1.
 */

import java.util.Arrays;
import java.util.stream.IntStream;

public class CountPairsWithStrictlyGreaterSum {

    public static void main(String[] args) {
        int[] nums1 = {2, 1, 2, 1};
        int[] nums2 = {1, 2, 1, 2};

        int[] nums1_B = {1, 10, 6, 2};
        int[] nums2_B = {1, 4, 1, 5};

        System.out.println("Test Case 1: nums1 = [2,1,2,1], nums2 = [1,2,1,2]");
        System.out.println("Brute Force:   " + countPairsBruteForce(nums1.clone(), nums2.clone()));
        System.out.println("Two Pointers:  " + countPairsTwoPointers(nums1.clone(), nums2.clone()));
        System.out.println("Binary Search: " + countPairsBinarySearch(nums1.clone(), nums2.clone()));
        System.out.println("--------------------------------------------------");

        System.out.println("Test Case 2: nums1 = [1,10,6,2], nums2 = [1,4,1,5]");
        System.out.println("Brute Force:   " + countPairsBruteForce(nums1_B.clone(), nums2_B.clone()));
        System.out.println("Two Pointers:  " + countPairsTwoPointers(nums1_B.clone(), nums2_B.clone()));
        System.out.println("Binary Search: " + countPairsBinarySearch(nums1_B.clone(), nums2_B.clone()));
    }

    /**
     * SOLUTION 1: Brute Force
     * 
     * Idea: Generate all pairs directly and check the condition.
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(1)
     */
    public static long countPairsBruteForce(int[] nums1, int[] nums2) {
        int n = nums1.length;
        long count = 0;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (nums1[i] + nums1[j] > nums2[i] + nums2[j]) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * SOLUTION 2: Difference Array + Sorting + Two Pointers (Most Optimal)
     * 
     * Idea: Transform the condition into diff[i] + diff[j] > 0.
     * Sort the diff array and use two pointers from both ends to efficiently count.
     * 
     * Time Complexity: O(N log N) - due to sorting the diff array.
     * Space Complexity: O(N) - to store the difference array.
     */
    public static long countPairsTwoPointers(int[] nums1, int[] nums2) {
        int n = nums1.length;
        int[] diff = new int[n];
        
        // 1. Build the difference array
        for (int i = 0; i < n; i++) {
            diff[i] = nums1[i] - nums2[i];
        }
        
        // 2. Sort the difference array
        Arrays.sort(diff);
        
        // 3. Two Pointers traversal
        long count = 0;
        int left = 0;
        int right = n - 1;
        
        while (left < right) {
            if (diff[left] + diff[right] > 0) {
                // If the sum is positive, any element between 'left' and 'right - 1' 
                // added to 'right' will ALSO be positive (because array is sorted).
                // So, all pairs formed by 'right' and any index from 'left' to 'right - 1' are valid.
                count += (right - left);
                
                // Move the right pointer to check the next largest element
                right--;
            } else {
                // Sum is too small (<= 0), we need a larger element from the left
                left++;
            }
        }
        
        return count;
    }

    /**
     * SOLUTION 3: Difference Array + Sorting + Binary Search
     * 
     * Idea: Same mathematical reduction as Solution 2. However, instead of two pointers,
     * for every element diff[i], we binary search for the first element that makes 
     * the sum strictly greater than 0 (i.e., we look for the first element > -diff[i]).
     * 
     * Time Complexity: O(N log N)
     * Space Complexity: O(N)
     */
    public static long countPairsBinarySearch(int[] nums1, int[] nums2) {
        int n = nums1.length;
        int[] diff = new int[n];
        
        for (int i = 0; i < n; i++) {
            diff[i] = nums1[i] - nums2[i];
        }
        
        Arrays.sort(diff);
        long count = 0;
        
        // For each element, find how many valid pairs it can form with elements to its right
        for (int i = 0; i < n - 1; i++) {
            // We need diff[j] > -diff[i] -> which is equivalent to finding 
            // the upper bound of -diff[i].
            int target = -diff[i];
            
            int low = i + 1;
            int high = n - 1;
            int firstValidIndex = n; // Default to 'n' if no valid element is found
            
            while (low <= high) {
                int mid = low + (high - low) / 2;
                if (diff[mid] > target) {
                    firstValidIndex = mid;
                    high = mid - 1; // Keep looking left to find the VERY FIRST valid element
                } else {
                    low = mid + 1;  // Too small, look right
                }
            }
            
            // All elements from 'firstValidIndex' to the end of the array are valid
            count += (n - firstValidIndex);
        }
        
        return count;
    }
}

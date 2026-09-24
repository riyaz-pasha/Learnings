/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of integers `nums`. We need to find how many unique 
 * triplets of indices (i, j, k) exist such that the values at these indices 
 * can form a valid triangle. 
 * Note: A triplet (i, j, k) is considered the same regardless of its order, 
 * so we must assume i < j < k to avoid double counting.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW (Confirm before writing code)
 * ============================================================================
 * 1. What are the rules for a valid triangle?
 *    (The sum of any two sides must be strictly greater than the third side. 
 *     a + b > c, a + c > b, b + c > a).
 * 2. Can the lengths be zero or negative?
 *    (Constraints say nums[i] >= 0. A side of 0 cannot form a triangle, so we 
 *     must ensure our logic inherently rejects triplets with a 0 side).
 * 3. Can the array have fewer than 3 elements?
 *    (Yes, constraints say length >= 1. If length < 3, the answer is always 0).
 * 4. Are there duplicate values in the array?
 *    (Yes. Distinct *indices* matter, not distinct *values*. Two sides can have 
 *     the same length, forming an isosceles or equilateral triangle).
 * 5. Can we modify the input array?
 *    (Sorting it in-place is usually fine, but always good to ask).
 * 6. Can the total count exceed the limit of a 32-bit signed integer?
 *    (Max N is 1000. Max combinations is 1000C3 ≈ 166 million, which easily 
 *     fits in a standard 32-bit int. No need for `long`).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * KEY OBSERVATION (The Math):
 * If we sort the array in ascending order such that a <= b <= c, we ONLY need 
 * to check one condition: a + b > c. 
 * Why? Because if a <= b <= c, then a + c > b and b + c > a are guaranteed to 
 * be true. This observation simplifies the problem drastically!
 * 
 * 1. Brute Force:
 *    - Sort the array.
 *    - Use three nested loops (i, j, k) to pick sides a, b, c.
 *    - If nums[i] + nums[j] > nums[k], increment count.
 *    - Time: O(N^3), Space: O(1). Will likely fail for N=1000 due to 1 billion operations.
 * 
 * 2. Binary Search (The "Good" Approach):
 *    - Sort the array. Time: O(N log N).
 *    - Fix the two smaller sides `nums[i]` and `nums[j]`. 
 *    - We need `nums[k] < nums[i] + nums[j]`. Since the array is sorted, we can 
 *      use Binary Search to find the rightmost index `k` that satisfies this.
 *    - Number of valid triplets for this (i, j) pair is `k - j`.
 *    - Time: O(N^2 log N), Space: O(1).
 * 
 * 3. Two Pointers (The "Optimal" Approach):
 *    - Sort the array.
 *    - Fix the LARGEST side `nums[k]` starting from the end of the array.
 *    - Place two pointers for the remaining sides: `left = 0` and `right = k - 1`.
 *    - If `nums[left] + nums[right] > nums[k]`:
 *      -> Magic! Since the array is sorted, replacing `nums[left]` with ANY element 
 *         to its right (up to `right - 1`) will yield a sum even greater! 
 *      -> So ALL elements between `left` and `right` can pair with `right` to form 
 *         a valid triangle with `k`.
 *      -> We add `(right - left)` to our count, and decrement `right` to check smaller sums.
 *    - If `nums[left] + nums[right] <= nums[k]`:
 *      -> The sum is too small. We need a bigger sum, so we increment `left`.
 *    - Time: O(N^2), Space: O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Two Pointers Approach)
 * ============================================================================
 * Example: nums = [2, 2, 3, 4]
 * 
 * Sorted: [2, 2, 3, 4]
 * Indices: 0  1  2  3
 * 
 * Loop k (largest side) from right to left:
 * 
 * Iteration 1: k = 3 (val 4)
 *   left = 0 (val 2), right = 2 (val 3)
 *   nums[L] + nums[R] = 2 + 3 = 5.
 *   5 > 4 (valid). 
 *   -> All elements from left to right-1 pair with right! (indices 0 and 1).
 *   -> count += (right - left) -> count += (2 - 0) = 2.
 *   -> right-- (right becomes 1).
 *   
 *   left = 0 (val 2), right = 1 (val 2)
 *   nums[L] + nums[R] = 2 + 2 = 4.
 *   4 <= 4 (invalid).
 *   -> left++ (left becomes 1). left == right, loop ends.
 * 
 * Iteration 2: k = 2 (val 3)
 *   left = 0 (val 2), right = 1 (val 2)
 *   nums[L] + nums[R] = 2 + 2 = 4.
 *   4 > 3 (valid).
 *   -> count += (right - left) -> count += (1 - 0) = 1.
 *   -> right-- (right becomes 0). loop ends.
 * 
 * Total count = 2 + 1 = 3.
 */

import java.util.Arrays;

public class ValidTriangleNumber {

    public static void main(String[] args) {
        int[] nums1 = {2, 2, 3, 4};
        int[] nums2 = {4, 2, 3, 4};
        
        System.out.println("Input: [2, 2, 3, 4]");
        System.out.println("Brute Force: " + triangleNumberBruteForce(nums1.clone()));
        System.out.println("Binary Search: " + triangleNumberBinarySearch(nums1.clone()));
        System.out.println("Two Pointers: " + triangleNumberTwoPointers(nums1.clone()));
        System.out.println("--------------------------------------------------");
        
        System.out.println("Input: [4, 2, 3, 4]");
        System.out.println("Brute Force: " + triangleNumberBruteForce(nums2.clone()));
        System.out.println("Binary Search: " + triangleNumberBinarySearch(nums2.clone()));
        System.out.println("Two Pointers: " + triangleNumberTwoPointers(nums2.clone()));
    }

    /**
     * SOLUTION 1: Brute Force
     * 
     * Idea: Test every combination of triplets. By sorting first, we only need to 
     * check one condition, and we can early-exit the innermost loop.
     * 
     * Time Complexity: O(N^3)
     * Space Complexity: O(1)
     */
    public static int triangleNumberBruteForce(int[] nums) {
        if (nums == null || nums.length < 3) return 0;
        
        Arrays.sort(nums);
        int count = 0;
        int n = nums.length;
        
        for (int i = 0; i < n - 2; i++) {
            for (int j = i + 1; j < n - 1; j++) {
                for (int k = j + 1; k < n; k++) {
                    if (nums[i] + nums[j] > nums[k]) {
                        count++;
                    } else {
                        // Because the array is sorted, any subsequent k will also
                        // result in nums[i] + nums[j] <= nums[k], so we can break early.
                        break;
                    }
                }
            }
        }
        return count;
    }

    /**
     * SOLUTION 2: Binary Search
     * 
     * Idea: Fix the two smaller sides `i` and `j`. The third side `k` must be strictly 
     * less than `nums[i] + nums[j]`. We can find the maximum valid index `k` using Binary Search.
     * 
     * Time Complexity: O(N^2 log N)
     * Space Complexity: O(1)
     */
    public static int triangleNumberBinarySearch(int[] nums) {
        if (nums == null || nums.length < 3) return 0;
        
        Arrays.sort(nums);
        int count = 0;
        int n = nums.length;
        
        for (int i = 0; i < n - 2; i++) {
            // Ignore sides of length 0 as they cannot form a triangle
            if (nums[i] == 0) continue; 
            
            for (int j = i + 1; j < n - 1; j++) {
                int target = nums[i] + nums[j];
                
                // Binary search for the largest index k where nums[k] < target
                int left = j + 1;
                int right = n - 1;
                int maxK = j; // default to j if no such k is found
                
                while (left <= right) {
                    int mid = left + (right - left) / 2;
                    if (nums[mid] < target) {
                        maxK = mid;
                        left = mid + 1; // Try to find a larger k
                    } else {
                        right = mid - 1; // Mid is too large, search left half
                    }
                }
                count += maxK - j;
            }
        }
        return count;
    }

    /**
     * SOLUTION 3: Two Pointers (Most Optimal)
     * 
     * Idea: Fix the largest side at index `k`. Use two pointers `left` (0) and 
     * `right` (k-1) to find pairs that sum to more than `nums[k]`.
     * 
     * Time Complexity: O(N^2) - O(N log N) for sort, plus O(N) internal while-loop 
     *                  executed O(N) times.
     * Space Complexity: O(1) - or O(log N) to O(N) for sorting depending on the 
     *                   underlying algorithm in Arrays.sort().
     */
    public static int triangleNumberTwoPointers(int[] nums) {
        if (nums == null || nums.length < 3) return 0;
        
        // 1. Sort the array so that we can leverage the property a <= b <= c
        Arrays.sort(nums);
        
        int count = 0;
        int n = nums.length;
        
        // 2. Iterate backwards, fixing the largest side `c` (nums[k])
        for (int k = n - 1; k >= 2; k--) {
            int left = 0;
            int right = k - 1;
            
            // 3. Two pointer sweep for the remaining elements
            while (left < right) {
                // Check if the sum of the two smaller sides exceeds the largest side
                if (nums[left] + nums[right] > nums[k]) {
                    // AHA! Since array is sorted, nums[left + 1] + nums[right] 
                    // will also be > nums[k]. 
                    // In fact, ALL elements from `left` to `right - 1` paired 
                    // with `right` will form a valid triangle.
                    count += (right - left);
                    
                    // Now, decrease the right side to check smaller potential sums
                    right--;
                } else {
                    // The sum is too small. We must increase the smaller side.
                    left++;
                }
            }
        }
        
        return count;
    }
}

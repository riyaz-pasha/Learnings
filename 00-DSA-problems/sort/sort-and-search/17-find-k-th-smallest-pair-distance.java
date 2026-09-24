/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of integers, `nums`, and an integer `k`.
 * We need to compute the absolute difference (distance) between every possible 
 * pair of elements in the array. 
 * After calculating all n*(n-1)/2 possible distances, we must imagine them 
 * sorted in ascending order. Our goal is to return the k-th smallest distance 
 * from this conceptual sorted list.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the array contain duplicate elements? 
 *    (Yes, and the distance between duplicates will be 0. We must count 0s as valid distances).
 * 2. Are the elements in the array guaranteed to be positive?
 *    (Constraints say 0 <= nums[i] <= 1000. So they are non-negative).
 * 3. Can 'k' be larger than the total number of pairs?
 *    (Constraints guarantee 1 <= k <= n*(n-1)/2, so it will always be valid).
 * 4. Is the array sorted initially?
 *    (No, the input is unsorted. We are allowed to sort it).
 * 5. Should I optimize for time complexity or space complexity?
 *    (This helps guide the decision between an O(N^2) time/O(1) space bucket approach 
 *    vs an O(N log N + N log D) time/O(1) space binary search approach).
 * 6. Can we mutate the original array?
 *    (Yes, sorting the array in-place is generally acceptable and optimal here).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (The "Baseline"):
 *    - Generate all possible pairs using two nested loops.
 *    - Calculate their absolute differences and store them in a list.
 *    - Sort the list and return the element at index `k - 1`.
 *    - Time: O(N^2 log(N^2)), Space: O(N^2). 
 *    - Given N = 1000, N^2 = 1,000,000. Sorting 1 million elements is slow and 
 *      uses a lot of memory, but it establishes a baseline.
 * 
 * 2. Bucket Sort / Counting Sort (Constraint-Hacking O(N^2)):
 *    - Notice the constraint: nums[i] <= 1000. 
 *    - This means the maximum possible distance between any two elements is 1000.
 *    - We can use an array of size 1001 to tally the frequencies of each distance.
 *    - Generate all N^2 pairs, increment the bucket for their distance.
 *    - Iterate through the buckets, keeping a running sum of frequencies, until 
 *      the sum reaches `k`.
 *    - Time: O(N^2), Space: O(Max_Distance) = O(1000) = O(1).
 *    - This is very fast in practice due to low overhead, but optimally we can do better!
 * 
 * 3. Binary Search + Sliding Window / Two Pointers (The "Optimal" Standard):
 *    - Instead of finding the k-th distance directly, let's ask a reverse question: 
 *      "Given a distance D, how many pairs have a distance <= D?"
 *    - If we sort the array first, we can find the answer to this question in O(N) 
 *      time using a sliding window (Two Pointers).
 *    - Since the count of pairs <= D is monotonically increasing as D increases, 
 *      we can Binary Search the answer!
 *    - The minimum possible distance is 0. The maximum is nums[N-1] - nums[0].
 *    - Time: O(N log N + N log(Max_Distance)), Space: O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Binary Search + Two Pointers)
 * ============================================================================
 * Example: nums = [1, 6, 1], k = 3
 * 
 * 1. Sort the array: [1, 1, 6]
 * 2. Identify the Search Space for Distances:
 *    - Min distance (low) = 0
 *    - Max distance (high) = 6 - 1 = 5
 * 
 * 3. Binary Search:
 *    - Iteration 1:
 *      low = 0, high = 5 -> mid = 2
 *      Count pairs with distance <= 2:
 *        Window: [1, 1] -> diff = 0 <= 2. Count += 1.
 *        Window: [1, 1, 6] -> diff = 5 > 2. Left pointer moves.
 *      Total pairs <= 2 is 1. 
 *      We need the 3rd smallest (k=3). Since 1 < 3, `low` becomes mid + 1 = 3.
 * 
 *    - Iteration 2:
 *      low = 3, high = 5 -> mid = 4
 *      Count pairs with distance <= 4:
 *      Total pairs <= 4 is 1 (only the [1,1] pair).
 *      Since 1 < 3, `low` becomes mid + 1 = 5.
 * 
 *    - Iteration 3:
 *      low = 5, high = 5 -> Loop ends.
 * 
 * 4. Result is `low` = 5.
 *    (Verification: pairs are (1,1)->0, (1,6)->5, (1,6)->5. Sorted distances: 0, 5, 5. 
 *     The 3rd smallest is indeed 5).
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KthSmallestPairDistance {

    public static void main(String[] args) {
        int[] nums1 = {1, 3, 1};
        int k1 = 1;

        int[] nums2 = {1, 1, 1};
        int k2 = 2;

        int[] nums3 = {1, 6, 1};
        int k3 = 3;

        System.out.println("--- Test Case 1: nums = [1,3,1], k = 1 ---");
        System.out.println("Brute Force:   " + smallestDistancePairBruteForce(nums1.clone(), k1));
        System.out.println("Bucket Sort:   " + smallestDistancePairBucket(nums1.clone(), k1));
        System.out.println("Binary Search: " + smallestDistancePairBinarySearch(nums1.clone(), k1));
        System.out.println();

        System.out.println("--- Test Case 2: nums = [1,1,1], k = 2 ---");
        System.out.println("Brute Force:   " + smallestDistancePairBruteForce(nums2.clone(), k2));
        System.out.println("Bucket Sort:   " + smallestDistancePairBucket(nums2.clone(), k2));
        System.out.println("Binary Search: " + smallestDistancePairBinarySearch(nums2.clone(), k2));
        System.out.println();

        System.out.println("--- Test Case 3: nums = [1,6,1], k = 3 ---");
        System.out.println("Brute Force:   " + smallestDistancePairBruteForce(nums3.clone(), k3));
        System.out.println("Bucket Sort:   " + smallestDistancePairBucket(nums3.clone(), k3));
        System.out.println("Binary Search: " + smallestDistancePairBinarySearch(nums3.clone(), k3));
    }

    /**
     * SOLUTION 1: Brute Force
     * 
     * Idea: Generate all pairs, store their absolute differences in a List, 
     * sort the List, and return the element at k-1.
     * 
     * Time Complexity: O(N^2 log(N^2))
     * Space Complexity: O(N^2)
     */
    public static int smallestDistancePairBruteForce(int[] nums, int k) {
        int n = nums.length;
        // Total pairs = n * (n - 1) / 2
        int totalPairs = n * (n - 1) / 2;
        List<Integer> distances = new ArrayList<>(totalPairs);

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                distances.add(Math.abs(nums[i] - nums[j]));
            }
        }

        Collections.sort(distances);
        return distances.get(k - 1);
    }

    /**
     * SOLUTION 2: Bucket / Counting Sort
     * 
     * Idea: Since the maximum value in nums is 1000, the maximum distance is 1000.
     * We can use an array of size 1001 to count the frequencies of every distance 
     * generated in O(N^2) time. This avoids the O(N^2 log(N^2)) sorting overhead.
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(M) where M is the maximum possible distance (1000).
     */
    public static int smallestDistancePairBucket(int[] nums, int k) {
        int n = nums.length;
        // Based on constraints: 0 <= nums[i] <= 1000
        int maxDist = 1000;
        int[] distanceCounts = new int[maxDist + 1];

        // Generate all pairs and tally their distances
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int dist = Math.abs(nums[i] - nums[j]);
                distanceCounts[dist]++;
            }
        }

        // Find the k-th smallest distance
        int count = 0;
        for (int dist = 0; dist <= maxDist; dist++) {
            count += distanceCounts[dist];
            if (count >= k) {
                return dist;
            }
        }

        return -1; // Should not be reached given valid inputs
    }

    /**
     * SOLUTION 3: Binary Search + Sliding Window (Optimal)
     * 
     * Idea: Binary search the answer space (the distance). For any guessed distance `mid`,
     * count how many pairs have a distance <= `mid` using a two-pointer sliding window 
     * on the sorted array. Adjust the binary search bounds based on the count.
     * 
     * Time Complexity: O(N log N + N log(Max_Distance))
     * Space Complexity: O(1) or O(N) depending on the sort implementation.
     */
    public static int smallestDistancePairBinarySearch(int[] nums, int k) {
        Arrays.sort(nums);
        int n = nums.length;

        // Minimum possible distance is 0
        int low = 0;
        // Maximum possible distance is the difference between max and min elements
        int high = nums[n - 1] - nums[0];

        while (low < high) {
            int mid = low + (high - low) / 2;

            // Count how many pairs have a distance <= mid
            int count = countPairsWithMaxDistance(nums, mid);

            if (count >= k) {
                // If we have 'k' or more pairs, the k-th smallest distance must 
                // be <= mid. So we move our upper bound down.
                high = mid;
            } else {
                // If we have fewer than 'k' pairs, the k-th smallest distance 
                // must be strictly greater than mid.
                low = mid + 1;
            }
        }

        // When low == high, we have found the exact k-th smallest distance.
        return low;
    }

    /**
     * Helper method for the Binary Search approach.
     * Uses a Sliding Window (Two Pointers) to count the number of pairs 
     * with an absolute difference less than or equal to `targetDistance`.
     * 
     * Requires the `nums` array to be sorted.
     */
    private static int countPairsWithMaxDistance(int[] nums, int targetDistance) {
        int count = 0;
        int left = 0;

        // Iterate through the array treating `right` as the upper bound of the pair
        for (int right = 0; right < nums.length; right++) {
            // If the distance between the elements at `right` and `left` is too large,
            // we must shrink the window by moving `left` forward.
            while (nums[right] - nums[left] > targetDistance) {
                left++;
            }
            // All elements from `left` to `right-1` can form a valid pair with `right`.
            // The number of such pairs is exactly `right - left`.
            count += right - left;
        }

        return count;
    }
}

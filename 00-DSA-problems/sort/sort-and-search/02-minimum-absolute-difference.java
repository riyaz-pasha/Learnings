/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of integers (both positive and negative). 
 * We need to find the smallest possible absolute difference between any two 
 * elements (which reside at different indices).
 * Once we find this minimum difference, we must return all pairs of elements 
 * that have exactly this difference. The pairs must be formatted as [x, y] 
 * where x < y, and the final list of pairs must be sorted in ascending order.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Are all elements in the array guaranteed to be distinct, or can there be duplicates? 
 *    (If there are duplicates, the minimum absolute difference is automatically 0).
 * 2. Are we allowed to sort the array in-place, modifying the original input?
 * 3. What is the expected return type? A list of lists (List<List<Integer>>) or a 2D array?
 * 4. Are there any memory constraints? Can we allocate extra arrays bounded by the value range?
 * 5. Does the order of the pairs in the output matter? (Yes, ascending order).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Intuition: To find the smallest difference between numbers, the numbers must 
 *    be as close to each other in value as possible. 
 * 2. Observation: If we sort the array, elements that are closest in value will 
 *    become adjacent. Therefore, the minimum absolute difference MUST be between 
 *    two adjacent elements in the sorted array.
 * 3. Base Solution (Sort + Two Passes):
 *    - Sort the array: O(N log N).
 *    - Pass 1: Find the minimum difference between adjacent elements.
 *    - Pass 2: Collect all adjacent pairs that match this minimum difference.
 * 4. Optimization (Sort + One Pass):
 *    - We can combine the two passes. As we iterate, if we find a new minimum 
 *      difference, we clear our results list and start fresh. If we find a 
 *      difference equal to the current minimum, we just append to the list.
 * 5. Extreme Optimization (Counting Sort / Bucket approach):
 *    - Since the range is constrained to [-10^6, 10^6], the spread is 2*10^6.
 *    - We can use an array of size 2,000,001 to record the presence of numbers in O(N) time.
 *    - Then, a single pass over this boolean array can find the minimum difference 
 *      and the pairs in O(Range) time.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Sort + One Pass)
 * ============================================================================
 * Example: arr = [4, 2, 1, 3]
 * 
 * 1. Sort the array -> [1, 2, 3, 4]
 * 2. Initialize minDiff = Infinity, result = []
 * 3. Compare 1 and 2: diff = 1. (1 < Inf) -> minDiff = 1, result = [[1, 2]]
 * 4. Compare 2 and 3: diff = 1. (1 == 1)  -> result = [[1, 2], [2, 3]]
 * 5. Compare 3 and 4: diff = 1. (1 == 1)  -> result = [[1, 2], [2, 3], [3, 4]]
 * 
 * Example 2: arr = [3, 8, -10, 23, 19, -4, -14, 27]
 * Sorted: [-14, -10, -4, 3, 8, 19, 23, 27]
 * Diffs:     4,   6,  7, 5, 11,  4,  4
 * minDiff = 4.
 * Pairs with diff 4: [-14, -10], [19, 23], [23, 27]
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MinimumAbsoluteDifference {

    public static void main(String[] args) {
        int[] arr1 = {4, 2, 1, 3};
        int[] arr2 = {3, 8, -10, 23, 19, -4, -14, 27};

        System.out.println("Solution 1 (Two Passes): " + minAbsDifferenceTwoPasses(arr1.clone()));
        System.out.println("Solution 2 (One Pass): " + minAbsDifferenceOnePass(arr2.clone()));
        System.out.println("Solution 3 (Counting): " + minAbsDifferenceCounting(arr2.clone()));
    }

    /**
     * SOLUTION 1: Sort + Two Passes
     * 
     * Idea: Sort the array so closest numbers are adjacent. First loop finds the 
     * absolute minimum difference. The second loop collects all pairs matching it.
     * 
     * Time Complexity: O(N log N) - due to sorting. The linear passes are O(N).
     * Space Complexity: O(1) extra space (ignoring the space required for the output list).
     */
    public static List<List<Integer>> minAbsDifferenceTwoPasses(int[] arr) {
        Arrays.sort(arr);
        
        int minDiff = Integer.MAX_VALUE;
        
        // Pass 1: Find the minimum difference
        for (int i = 1; i < arr.length; i++) {
            minDiff = Math.min(minDiff, arr[i] - arr[i - 1]);
        }
        
        List<List<Integer>> result = new ArrayList<>();
        
        // Pass 2: Collect all pairs with that minimum difference
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] - arr[i - 1] == minDiff) {
                result.add(Arrays.asList(arr[i - 1], arr[i]));
            }
        }
        
        return result;
    }

    /**
     * SOLUTION 2: Sort + One Pass (Dynamic List Management)
     * 
     * Idea: We can optimize the previous solution by doing it in a single pass 
     * over the sorted array. If we find a strictly smaller difference, our old 
     * list is invalid, so we clear it. If we tie the minimum difference, we append.
     * 
     * Time Complexity: O(N log N) - sorting is the bottleneck.
     * Space Complexity: O(1) extra space.
     */
    public static List<List<Integer>> minAbsDifferenceOnePass(int[] arr) {
        Arrays.sort(arr);
        
        List<List<Integer>> result = new ArrayList<>();
        int minDiff = Integer.MAX_VALUE;
        
        for (int i = 1; i < arr.length; i++) {
            int currentDiff = arr[i] - arr[i - 1];
            
            if (currentDiff < minDiff) {
                // Found a new minimum difference. Discard all previous pairs.
                minDiff = currentDiff;
                result.clear();
                result.add(Arrays.asList(arr[i - 1], arr[i]));
            } else if (currentDiff == minDiff) {
                // Found another pair with the same minimum difference.
                result.add(Arrays.asList(arr[i - 1], arr[i]));
            }
        }
        
        return result;
    }

    /**
     * SOLUTION 3: Counting Array (O(N) Time)
     * 
     * Idea: The constraints say elements range from -10^6 to 10^6. 
     * The total possible values are 2,000,001. We can use a boolean array 
     * to map out existence of each number. This avoids O(N log N) sorting.
     * 
     * Time Complexity: O(N + MAX_RANGE) -> O(N + 2*10^6). Faster for huge N.
     * Space Complexity: O(MAX_RANGE) -> Array of size 2,000,001. 
     * Note: Assumes all elements in `arr` are distinct as per problem implications.
     */
    public static List<List<Integer>> minAbsDifferenceCounting(int[] arr) {
        // Offset to handle negative numbers (shifts -10^6 to 0)
        final int OFFSET = 1000000;
        final int RANGE = 2000001; 
        
        boolean[] present = new boolean[RANGE];
        
        // Mark all numbers present
        for (int num : arr) {
            present[num + OFFSET] = true;
        }
        
        int minDiff = Integer.MAX_VALUE;
        int prev = -1; // to keep track of the previous number seen
        List<List<Integer>> result = new ArrayList<>();
        
        // Iterate over the counting array in ascending order
        for (int i = 0; i < RANGE; i++) {
            if (present[i]) {
                if (prev != -1) {
                    int currentDiff = i - prev;
                    
                    // Same clear-and-append logic as Solution 2
                    if (currentDiff < minDiff) {
                        minDiff = currentDiff;
                        result.clear();
                        result.add(Arrays.asList(prev - OFFSET, i - OFFSET));
                    } else if (currentDiff == minDiff) {
                        result.add(Arrays.asList(prev - OFFSET, i - OFFSET));
                    }
                }
                prev = i;
            }
        }
        
        return result;
    }
}

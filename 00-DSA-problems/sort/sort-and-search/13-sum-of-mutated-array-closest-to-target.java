/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of integers and a target sum. We want to find a threshold 
 * 'value' such that if we replace every number in the array that is strictly 
 * greater than 'value' with 'value', the new sum of the array is as close to the 
 * 'target' as possible.
 * 
 * If there are multiple values that yield the same minimum difference from the 
 * target, we must return the smallest 'value'.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW (Confirm before writing code)
 * ============================================================================
 * 1. Can the elements in the array be zero or negative? 
 *    (Constraints say arr[i] >= 1, so all are strictly positive).
 * 2. Can the target be extremely large or small? 
 *    (Target is between 1 and 10^4, which implies standard integer types are safe).
 * 3. Does the threshold 'value' have to be a number that exists in the array?
 *    (No, the problem explicitly states it doesn't have to be in the array).
 * 4. What is the maximum possible answer?
 *    (The maximum useful value is the maximum element in the array. Any value 
 *    larger than the maximum element will not change the array sum further, but 
 *    since we must pick the smaller value in a tie, we'd never pick a value 
 *    greater than the array's max).
 * 5. Can we modify the input array?
 *    (Sorting the array is highly advantageous. If the original order is needed 
 *    later, we should clone it first).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (Iterative Evaluation):
 *    - The maximum possible answer is bounded by the maximum element in the array 
 *      (which is at most 10^4). 
 *    - We can simply test every integer 'v' from 0 up to the maximum element.
 *    - For each 'v', calculate the mutated array sum. 
 *    - Track the minimum absolute difference. Because we iterate from 0 upwards, 
 *      the first 'v' that achieves the minimum difference is naturally the 
 *      smallest value, handling the tie-breaker requirement automatically.
 *    - Time Complexity: O(M * N) where M is max(arr) and N is arr.length. 
 *      With M=10^4 and N=10^3, this is 10^7 operations, which is acceptable.
 * 
 * 2. Binary Search (Optimized Evaluation):
 *    - The mutated array sum is a monotonically increasing function of 'v'. 
 *      As 'v' increases, the sum increases (or stays flat).
 *    - We can use Binary Search on the range [0, max(arr)] to find the ideal 'v'.
 *    - At each step, calculate the mutated sum for 'mid' and update the best 
 *      result. If the sum is smaller than target, search right. If larger, search left.
 *    - Time Complexity: O(N * log M). Faster and very standard.
 * 
 * 3. Sorting + Greedy Math (The Most Optimal):
 *    - Sort the array.
 *    - Iterate through the elements. For element at index i, there are (N - i) 
 *      elements left. 
 *    - What if we made ALL remaining elements equal to some value 'v'? 
 *      The ideal 'v' would be: (Remaining Target) / (Remaining Elements).
 *    - We calculate this ideal 'v' (handling rounding properly). If this ideal 'v' 
 *      is smaller than or equal to the current element arr[i], it means we can 
 *      safely cap all remaining elements at 'v'. We return 'v' immediately.
 *    - If 'v' is larger than arr[i], we must keep arr[i] as is, subtract it from 
 *      the target, and move to the next element.
 *    - Time Complexity: O(N log N) for sorting, O(N) for the pass. Space: O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Math / Greedy Approach)
 * ============================================================================
 * Example: arr = [4, 9, 3], target = 10
 * 
 * 1. Sort the array -> [3, 4, 9]
 * 
 * 2. Process index 0 (val = 3):
 *    - Remaining elements = 3
 *    - Remaining target = 10
 *    - Ideal value = 10 / 3 = 3.333...
 *    - How to round? 
 *      Quotient (q) = 3, Remainder (r) = 1. 
 *      If 2 * r > remaining_elements, round up. Otherwise, round down.
 *      2 * 1 = 2. Since 2 <= 3, we round down to 3.
 *    - Is ideal value (3) <= current element (3)? YES! 
 *    - We can cap everything at 3. Array becomes [3, 3, 3]. Sum = 9. 
 *    - Return 3.
 */

import java.util.Arrays;

public class SumOfMutatedArrayClosestToTarget {

    public static void main(String[] args) {
        int[] arr1 = {4, 9, 3};
        int target1 = 10;
        
        int[] arr2 = {2, 3, 5};
        int target2 = 10;
        
        System.out.println("Input: [4, 9, 3], target: 10");
        System.out.println("Brute Force:   " + findBestValueBruteForce(arr1, target1));
        System.out.println("Binary Search: " + findBestValueBinarySearch(arr1, target1));
        System.out.println("Greedy Math:   " + findBestValueMath(arr1.clone(), target1));
        System.out.println("--------------------------------------------------");
        
        System.out.println("Input: [2, 3, 5], target: 10");
        System.out.println("Brute Force:   " + findBestValueBruteForce(arr2, target2));
        System.out.println("Binary Search: " + findBestValueBinarySearch(arr2, target2));
        System.out.println("Greedy Math:   " + findBestValueMath(arr2.clone(), target2));
    }

    /**
     * SOLUTION 1: Brute Force
     * 
     * Idea: Test every integer 'v' from 0 to the maximum element in the array.
     * Record the sum and find the minimum difference.
     * 
     * Time Complexity: O(M * N) where M is max(arr).
     * Space Complexity: O(1)
     */
    public static int findBestValueBruteForce(int[] arr, int target) {
        int maxElement = 0;
        for (int num : arr) {
            maxElement = Math.max(maxElement, num);
        }
        
        int minDiff = Integer.MAX_VALUE;
        int bestValue = -1;
        
        // Iterate upwards. If there's a tie, the strictly `<` condition 
        // guarantees we keep the smaller 'v'.
        for (int v = 0; v <= maxElement; v++) {
            int currentSum = getMutatedSum(arr, v);
            int diff = Math.abs(currentSum - target);
            
            if (diff < minDiff) {
                minDiff = diff;
                bestValue = v;
            }
        }
        
        return bestValue;
    }

    /**
     * SOLUTION 2: Binary Search
     * 
     * Idea: The sum function is monotonically increasing. We can binary search 
     * the optimal 'v' between 0 and max(arr).
     * 
     * Time Complexity: O(N log M) where M is max(arr).
     * Space Complexity: O(1)
     */
    public static int findBestValueBinarySearch(int[] arr, int target) {
        int maxElement = 0;
        for (int num : arr) {
            maxElement = Math.max(maxElement, num);
        }
        
        int low = 0;
        int high = maxElement;
        
        int minDiff = Integer.MAX_VALUE;
        int bestValue = -1;
        
        while (low <= high) {
            int mid = low + (high - low) / 2;
            int currentSum = getMutatedSum(arr, mid);
            int diff = Math.abs(currentSum - target);
            
            // Check if this 'mid' yields a better difference OR 
            // a tied difference with a smaller value.
            if (diff < minDiff || (diff == minDiff && mid < bestValue)) {
                minDiff = diff;
                bestValue = mid;
            }
            
            if (currentSum == target) {
                return mid; // Exact match found, cannot get better than 0 diff
            } else if (currentSum < target) {
                low = mid + 1; // Sum is too small, try larger v
            } else {
                high = mid - 1; // Sum is too large, try smaller v
            }
        }
        
        return bestValue;
    }

    /**
     * Helper method to calculate the sum of the array if all elements 
     * greater than 'value' are replaced with 'value'.
     */
    private static int getMutatedSum(int[] arr, int value) {
        int sum = 0;
        for (int num : arr) {
            sum += Math.min(num, value);
        }
        return sum;
    }

    /**
     * SOLUTION 3: Sorting + Greedy Math (Optimal)
     * 
     * Idea: Sort the array. Distribute the remaining target evenly among the 
     * remaining elements. If the evenly distributed value fits within the 
     * current element's cap, we've found our ideal threshold.
     * 
     * Time Complexity: O(N log N)
     * Space Complexity: O(1) or O(N) depending on sort implementation
     */
    public static int findBestValueMath(int[] arr, int target) {
        Arrays.sort(arr);
        
        int n = arr.length;
        int currentSum = 0;
        
        for (int i = 0; i < n; i++) {
            int remainingElements = n - i;
            int remainingTarget = target - currentSum;
            
            // Calculate ideal average value for the remaining elements
            int quotient = remainingTarget / remainingElements;
            int remainder = remainingTarget % remainingElements;
            
            // Determine if we should round up.
            // A strict > operator ensures that in the event of an exact half 
            // (e.g., .5), we round down, which satisfies the requirement to 
            // return the smaller value in a tie.
            int idealValue = quotient;
            if (remainder * 2 > remainingElements) {
                idealValue++;
            }
            
            // If the ideal value is achievable (it is less than or equal to 
            // the current element we are examining), we have found the answer.
            if (idealValue <= arr[i]) {
                return idealValue;
            }
            
            // Otherwise, we cannot cap at idealValue because the current element 
            // is smaller than it. We must take the current element as is.
            currentSum += arr[i];
        }
        
        // If we iterate through the entire array and all elements are smaller 
        // than their respective ideal values, the best we can do is just return 
        // the maximum element in the array. Any threshold larger than the max 
        // element yields the exact same array sum, but we want the smallest 
        // threshold that causes that sum.
        return arr[n - 1];
    }
}

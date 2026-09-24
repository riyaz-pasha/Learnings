/**
 * ============================================================================
 * PROBLEM STATEMENT:
 * Given an unsorted integer array 'arr' of size n and an integer k, find the 
 * first k missing positive integers from the array, ignoring all negative numbers 
 * and zeros. If the array doesn't contain enough missing positive numbers, 
 * continue adding the next consecutive positive integers until exactly k 
 * missing numbers are found. Return the list in ascending order.
 * 
 * Constraints:
 * - n == arr.length
 * - 1 <= k <= 10^4
 * - -10^4 <= arr[i] <= 10^4
 * - 0 <= n <= 10^4
 * 
 * ============================================================================
 * INTERVIEW STRATEGY & CLARIFYING QUESTIONS:
 * Before writing code, confirm these edge cases with the interviewer:
 * 1. "Can the array be completely empty?" (Constraints say n >= 0, so yes, we 
 *    must handle an empty array gracefully).
 * 2. "Does the array contain duplicates?" (Yes, standard for these problems; 
 *    our sorting logic must avoid infinite loops on duplicates).
 * 3. "Is it permissible to modify the original array to achieve optimal space?" 
 *    (Yes, using Cyclic Sort in-place is highly preferred here).
 * 4. "If 'k' is very large, can we assume the output list will fit in memory?" 
 *    (Yes, k <= 10^4 is small enough).
 * 
 * ============================================================================
 * RESTATING THE PROBLEM:
 * We need to find exactly 'k' positive integers (1, 2, 3...) that are NOT 
 * present in the input array. We must search sequentially starting from 1. 
 * Any number <= 0 is completely ignored.
 * 
 * ============================================================================
 * IDEA, INTUITION & KEY OBSERVATIONS:
 * 1. This is a direct extension of the "First Missing Positive" problem. 
 *    The core idea remains the same: the PIGEONHOLE PRINCIPLE.
 * 2. Any positive integer 'x' where 1 <= x <= n belongs at index 'x - 1'.
 *    Numbers <= 0 or > n are out of our initial bounds and can be ignored 
 *    during the sorting phase.
 * 3. CYCLIC SORT: We can iterate through the array, swapping numbers to their 
 *    correct indices in O(N) time and O(1) space. 
 * 4. Finding Missing Numbers: After sorting, we iterate through the array. 
 *    If arr[i] != i + 1, then 'i + 1' is missing. We add it to our results 
 *    and decrement k.
 * 5. Tracking Extra Numbers: During our scan, if we encounter a number > n, 
 *    we should store it in a HashSet. Why? Because if we exhaust the range 
 *    [1, n] and still need more numbers (k > 0), we will start proposing 
 *    candidates from n + 1 onwards. The HashSet allows us to check in O(1) 
 *    time if a candidate > n was actually present in the original array.
 * 
 * ============================================================================
 * SOLUTION BREAKDOWN & VISUAL TRACING:
 * Example Array: [2, 3, 4], k = 3. Size n = 3.
 * 
 * Step 1: Cyclic Sort
 * i=0, arr[0]=2. Target index = 1. Swap -> [4, 2, 3]
 * i=0, arr[0]=4. Out of bounds (> n). Move on.
 * i=1, arr[1]=2. Already at correct index. Move on.
 * i=2, arr[2]=3. Already at correct index. Move on.
 * 
 * Step 2: Scan for missing in range [1, n]
 * i=0: arr[0]=4 != 1. 
 *      -> Missing! Add 1 to results. k becomes 2. 
 *      -> arr[0] is 4 (> n). Add 4 to `extraNumbers` Set.
 * i=1: arr[1]=2 == 2. All good.
 * i=2: arr[2]=3 == 3. All good.
 * 
 * Step 3: Extend beyond n (since k is still 2)
 * Our next candidate starts at n + 1, which is 4.
 * candidate = 4: Present in `extraNumbers` Set? Yes! Skip it.
 * candidate = 5: Present? No! Add 5 to results. k becomes 1.
 * candidate = 6: Present? No! Add 6 to results. k becomes 0.
 * 
 * Final Result: [1, 5, 6]
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FindKMissingPositiveNumbers {

    /**
     * Modern Java Record to cleanly encapsulate test cases.
     */
    record TestCase(int[] input, int k, List<Integer> expected, String description) {}

    /**
     * Finds the first k missing positive numbers.
     * 
     * Time Complexity: O(N + K) - Cyclic sort takes O(N), finding elements takes O(N + K).
     * Space Complexity: O(N) to store out-of-bounds numbers in the worst case, 
     *                   plus O(K) for the output list.
     * 
     * @param arr The unsorted input array.
     * @param k The number of missing positive integers to find.
     * @return A sorted list of the first k missing positive integers.
     */
    public static List<Integer> findKMissingPositive(int[] arr, int k) {
        List<Integer> missingNumbers = new ArrayList<>();
        if (arr == null || k <= 0) {
            return missingNumbers;
        }

        int n = arr.length;
        int i = 0;

        // Step 1: Cyclic Sort
        // Place every valid number (1 to n) at its correct index (0 to n-1)
        while (i < n) {
            int correctIndex = arr[i] - 1;
            // Valid range check AND duplicate avoidance check
            if (arr[i] > 0 && arr[i] <= n && arr[i] != arr[correctIndex]) {
                swap(arr, i, correctIndex);
            } else {
                i++;
            }
        }

        // Step 2: Identify missing numbers in the range [1, n]
        // Also keep track of numbers strictly greater than 'n' that are in the array
        Set<Integer> extraNumbers = new HashSet<>();
        for (i = 0; i < n && k > 0; i++) {
            if (arr[i] != i + 1) {
                // Since arr[i] is NOT i + 1, the number i + 1 is missing
                missingNumbers.add(i + 1);
                k--;
                
                // If the displaced number is > n, remember it so we don't accidentally
                // include it when we search beyond n later.
                if (arr[i] > n) {
                    extraNumbers.add(arr[i]);
                }
            }
        }

        // Step 3: If we still need more numbers, start searching from n + 1
        int candidate = n + 1;
        while (k > 0) {
            // Only add the candidate if it wasn't found in our 'extra' elements
            if (!extraNumbers.contains(candidate)) {
                missingNumbers.add(candidate);
                k--;
            }
            candidate++;
        }

        return missingNumbers;
    }

    /**
     * Helper method to swap elements in an array.
     */
    private static void swap(int[] arr, int first, int second) {
        int temp = arr[first];
        arr[first] = arr[second];
        arr[second] = temp;
    }

    /**
     * Main execution using Java Streams for clean testing and validation.
     */
    public static void main(String[] args) {
        
        List<TestCase> testCases = List.of(
            new TestCase(new int[]{2, 3, 4}, 3, List.of(1, 5, 6), "Missing at start and end"),
            new TestCase(new int[]{-2, -3, 4}, 2, List.of(1, 2), "Negative numbers ignored"),
            new TestCase(new int[]{1, 2, 3}, 2, List.of(4, 5), "Perfectly sorted array, extend beyond n"),
            new TestCase(new int[]{}, 5, List.of(1, 2, 3, 4, 5), "Empty array"),
            new TestCase(new int[]{2, 1, 3, 6, 5}, 2, List.of(4, 7), "Missing in middle and at end"),
            new TestCase(new int[]{4, 4, 4}, 3, List.of(1, 2, 3), "Duplicates ignored safely")
        );

        System.out.println("--- Testing Find K Missing Positive Numbers ---");
        
        testCases.forEach(tc -> {
            // Must clone array to avoid mutating test data if tests are run multiple times
            int[] arrCopy = tc.input().clone();
            List<Integer> result = findKMissingPositive(arrCopy, tc.k());
            boolean passed = result.equals(tc.expected());
            
            System.out.printf("[%s] %s%n", passed ? "PASS" : "FAIL", tc.description());
            System.out.printf("   Input: %s, k: %d%n", Arrays.toString(tc.input()), tc.k());
            System.out.printf("   Expected: %s | Got: %s%n%n", tc.expected(), result);
        });
    }
}

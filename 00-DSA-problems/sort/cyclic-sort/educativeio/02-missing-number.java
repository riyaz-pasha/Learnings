/**
 * ============================================================================
 * PROBLEM STATEMENT:
 * Given an array 'nums' containing 'n' distinct numbers in the range [0, n], 
 * return the only number in the range that is missing from the array.
 * 
 * Constraints: n == nums.length, 1 <= n <= 1000, 0 <= nums[i] <= n.
 * There are no duplicates.
 * 
 * ============================================================================
 * INTERVIEW STRATEGY & CLARIFYING QUESTIONS:
 * 1. "Can 'n' be very large? Should I be concerned about integer overflow 
 *     if I calculate the sum of the array?" (Important for the Math approach).
 * 2. "Is the array guaranteed to have exactly one missing number?"
 * 3. "Are there any constraints on time and space complexity? Is O(n) time 
 *     and O(1) space expected?"
 * 4. "Is the input array mutable? Can I modify it?" (Relevant for Cyclic Sort).
 * 5. "What if the missing number is 'n' itself, or '0'?"
 * 
 * ============================================================================
 * RESTATING THE PROBLEM:
 * We are given an array of size 'n'. The elements should theoretically be the 
 * numbers from 0 up to 'n'. However, because the size is 'n', there is room 
 * for only 'n' numbers, meaning exactly one number from the [0, n] range is 
 * missing. We need to identify that missing number efficiently.
 * 
 * ============================================================================
 * IDEA, INTUITION & KEY OBSERVATIONS (3 Different Solutions):
 * 
 * APPROACH 1: Mathematical Formula (Gauss's Formula)
 * - The sum of the first 'n' natural numbers is given by n * (n + 1) / 2.
 * - If we sum all the numbers from 0 to 'n', and then subtract the sum of 
 *   all elements present in our array, the difference will be the missing number.
 * - Pros: Very intuitive, easy to write.
 * - Cons: Potential integer overflow if 'n' is massive (though safe for n=1000).
 * 
 * APPROACH 2: Bit Manipulation (XOR)
 * - The XOR operator (^) has a neat property: a ^ a = 0, and a ^ 0 = a.
 * - If we XOR all the indices (0 to n) and XOR them with all the values in 
 *   the array, every number that appears in the array will cancel out with 
 *   its corresponding index number. The only number left standing will be 
 *   the missing one.
 * - Pros: Completely avoids integer overflow, extremely fast.
 * 
 * APPROACH 3: Cyclic Sort
 * - Since the numbers are in the range [0, n], we can place every number 'x' 
 *   at index 'x'. If x == n, we just ignore it since the array only goes up 
 *   to index n - 1. 
 * - After sorting, we iterate through. The first index 'i' where nums[i] != i 
 *   is our missing number.
 * - Pros: Extends well to problems finding *multiple* missing numbers.
 * 
 * ============================================================================
 * SOLUTION BREAKDOWN & VISUAL TRACING (XOR Approach):
 * Example Array: [3, 0, 1], length n = 3.
 * Missing number range: 0, 1, 2, 3.
 * 
 * Let missing = 3 (this represents 'n' initially)
 * 
 * Step 1: i = 0, nums[0] = 3. 
 *         missing = missing ^ i ^ nums[i]
 *                 = 3 ^ 0 ^ 3 = 0
 * 
 * Step 2: i = 1, nums[1] = 0.
 *         missing = missing ^ i ^ nums[i]
 *                 = 0 ^ 1 ^ 0 = 1
 * 
 * Step 3: i = 2, nums[2] = 1.
 *         missing = missing ^ i ^ nums[i]
 *                 = 1 ^ 2 ^ 1 = 2
 * 
 * Final Answer: 2 is the missing number.
 * ============================================================================
 */

import java.util.Arrays;
import java.util.List;

public class MissingNumber {

    /**
     * Modern Java Record to cleanly define our test cases.
     */
    record TestCase(int[] input, int expected, String description) {}

    /**
     * Approach 1: Mathematical Sum
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public static int missingNumberMath(int[] nums) {
        int n = nums.length;
        // Formula for sum of 0 to n
        int expectedSum = n * (n + 1) / 2;
        
        int actualSum = 0;
        for (int num : nums) {
            actualSum += num;
        }
        
        return expectedSum - actualSum;
    }

    /**
     * Approach 2: Bit Manipulation (XOR)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public static int missingNumberXOR(int[] nums) {
        // Initialize with 'n' because the loop indices will only go up to n - 1
        int missing = nums.length;
        
        for (int i = 0; i < nums.length; i++) {
            // XOR the current index and the current value
            missing ^= i ^ nums[i];
        }
        
        return missing;
    }

    /**
     * Approach 3: Cyclic Sort
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public static int missingNumberCyclic(int[] nums) {
        int i = 0;
        int n = nums.length;
        
        // Sort the array in place
        while (i < n) {
            int correctIndex = nums[i];
            // If the number is within bounds and not at its correct index, swap
            if (nums[i] < n && nums[i] != nums[correctIndex]) {
                swap(nums, i, correctIndex);
            } else {
                i++;
            }
        }
        
        // Find the first mismatch
        for (int j = 0; j < n; j++) {
            if (nums[j] != j) {
                return j;
            }
        }
        
        // If all match, then 'n' itself is the missing number
        return n;
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
     * Main execution using Java Streams for clean testing.
     */
    public static void main(String[] args) {
        
        List<TestCase> testCases = List.of(
            new TestCase(new int[]{3, 0, 1}, 2, "Missing from middle"),
            new TestCase(new int[]{0, 1}, 2, "Missing 'n' (the max)"),
            new TestCase(new int[]{9, 6, 4, 2, 3, 5, 7, 0, 1}, 8, "Larger array"),
            new TestCase(new int[]{1}, 0, "Missing '0' (the min)")
        );

        System.out.println("--- Testing Approach 1: Mathematical Sum ---");
        testCases.forEach(tc -> {
            int result = missingNumberMath(tc.input());
            System.out.printf("[%s] %s -> Expected: %d, Got: %d%n", 
                (result == tc.expected()) ? "PASS" : "FAIL", 
                tc.description(), tc.expected(), result);
        });

        System.out.println("\n--- Testing Approach 2: Bit Manipulation (XOR) ---");
        testCases.forEach(tc -> {
            int result = missingNumberXOR(tc.input());
            System.out.printf("[%s] %s -> Expected: %d, Got: %d%n", 
                (result == tc.expected()) ? "PASS" : "FAIL", 
                tc.description(), tc.expected(), result);
        });

        System.out.println("\n--- Testing Approach 3: Cyclic Sort ---");
        testCases.forEach(tc -> {
            // Must copy array since Cyclic Sort mutates the input
            int[] arrCopy = Arrays.copyOf(tc.input(), tc.input().length);
            int result = missingNumberCyclic(arrCopy);
            System.out.printf("[%s] %s -> Expected: %d, Got: %d%n", 
                (result == tc.expected()) ? "PASS" : "FAIL", 
                tc.description(), tc.expected(), result);
        });
    }
}

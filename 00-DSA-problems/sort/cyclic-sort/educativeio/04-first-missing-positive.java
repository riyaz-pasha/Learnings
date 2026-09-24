/**
 * ============================================================================
 * PROBLEM STATEMENT:
 * Given an unsorted integer array 'nums', return the smallest missing positive integer. 
 * Create an algorithm that runs in O(n) time complexity and uses O(1) extra space.
 * 
 * Constraints:
 * - 1 <= nums.length <= 10^5
 * - -2^31 <= nums[i] <= 2^31 - 1
 * 
 * ============================================================================
 * INTERVIEW STRATEGY & CLARIFYING QUESTIONS:
 * In an interview, do not immediately start coding. Confirm these details:
 * 1. "Can I modify the input array in place?" (Crucial, as O(1) space heavily 
 *    relies on reusing the input array.)
 * 2. "Does 'positive integer' strictly mean numbers > 0?" (Yes, 1 is the 
 *    smallest positive integer, 0 is not positive.)
 * 3. "Can there be duplicate numbers in the array?" (Yes, constraints don't 
 *    say distinct, so our logic must gracefully handle duplicates without looping infinitely.)
 * 4. "The numbers can be very large or negative. Are we expected to handle 
 *    potential integer overflow?" (Since we only care about [1, n], we can safely 
 *    ignore numbers outside this bounds.)
 * 
 * ============================================================================
 * RESTATING THE PROBLEM:
 * We need to find the lowest number starting from 1 that DOES NOT exist in the 
 * array. If the array is [1, 2, 3], the answer is 4. If the array is [-5, 100], 
 * the answer is 1. We must do this without using extra memory (like HashSets) 
 * and in linear time.
 * 
 * ============================================================================
 * IDEA, INTUITION & KEY OBSERVATIONS:
 * 1. The PIGEONHOLE PRINCIPLE is the key here. We are looking for the smallest 
 *    missing positive integer.
 * 2. If the array has 'n' elements, the answer MUST fall in the range [1, n+1]. 
 *    Why? In the absolute best-case scenario, the array contains perfectly 
 *    sequential numbers 1, 2, 3... n. The first missing positive would be n+1. 
 *    If ANY number in that sequence is replaced by a negative number, a duplicate, 
 *    or a massive number, it leaves a "hole" in the [1, n] sequence.
 * 3. Thus, we ONLY care about numbers in the range [1, n]. Any number <= 0 or 
 *    > n is completely irrelevant "garbage" for our specific goal.
 * 
 * APPROACH 1: CYCLIC SORT (Index as Hash Key)
 * - We can reuse the array itself as a hash map. 
 * - We try to place every valid number 'x' (where 1 <= x <= n) at index 'x - 1'.
 * - Iterate through the array. If nums[i] is a valid number and it's not already 
 *   at its correct index, swap it with the number at its target index.
 * - After processing, the first index 'i' where nums[i] != i + 1 represents 
 *   our missing number 'i + 1'.
 * 
 * APPROACH 2: NEGATIVE MARKING (Two Passes)
 * - Step 1: Clean the data. Iterate and replace any irrelevant number 
 *   (nums[i] <= 0 or nums[i] > n) with a dummy value like n + 1.
 * - Step 2: Use the indices to track seen numbers. If we see number 'x', 
 *   we go to index 'x - 1' and make the value there NEGATIVE.
 * - Step 3: Scan the array. The first positive value we encounter is at index 
 *   'i', meaning 'i + 1' was never seen.
 * 
 * ============================================================================
 * SOLUTION BREAKDOWN & VISUAL TRACING (Cyclic Sort):
 * Example Array: [3, 4, -1, 1], n = 4. 
 * Valid range: [1, 4]
 * 
 * Step 1: i=0, nums[0]=3. Valid? Yes (between 1 and 4). 
 *         Target index for 3 is 2. 
 *         Swap nums[0] and nums[2].
 *         Array becomes: [-1, 4, 3, 1]
 * 
 * Step 2: i=0 (still), nums[0]=-1. Valid? No.
 *         Move forward. i=1.
 * 
 * Step 3: i=1, nums[1]=4. Valid? Yes.
 *         Target index for 4 is 3.
 *         Swap nums[1] and nums[3].
 *         Array becomes: [-1, 1, 3, 4]
 * 
 * Step 4: i=1 (still), nums[1]=1. Valid? Yes.
 *         Target index for 1 is 0.
 *         Swap nums[1] and nums[0].
 *         Array becomes: [1, -1, 3, 4]
 * 
 * Step 5: i=1 (still), nums[1]=-1. Valid? No. 
 *         Move forward. i=2, nums[2]=3 (in correct place). 
 *         Move forward. i=3, nums[3]=4 (in correct place).
 * 
 * Final Pass:
 * index 0: 1 (matches 0+1)
 * index 1: -1 (mismatches 1+1). 
 * Missing is 2!
 * ============================================================================
 */

import java.util.Arrays;
import java.util.List;

public class FirstMissingPositive {

    /**
     * Modern Java Record to cleanly define our test cases.
     */
    record TestCase(int[] input, int expected, String description) {}

    /**
     * Approach 1: Cyclic Sort
     * Time Complexity: O(n) - Each number is swapped at most once.
     * Space Complexity: O(1) - Modifies array in place.
     */
    public static int firstMissingPositiveCyclic(int[] nums) {
        int n = nums.length;
        int i = 0;
        
        while (i < n) {
            // Check if nums[i] is in the valid range [1, n]
            if (nums[i] > 0 && nums[i] <= n) {
                int correctIndex = nums[i] - 1;
                // If it's not already at the correct index, swap it.
                // Checking nums[i] != nums[correctIndex] prevents infinite loops on duplicates!
                if (nums[i] != nums[correctIndex]) {
                    swap(nums, i, correctIndex);
                } else {
                    // It's a duplicate that's already in the right place, move on
                    i++;
                }
            } else {
                // Out of bounds (negative, zero, or > n), move on
                i++;
            }
        }
        
        // Scan to find the first index that doesn't have the correct number
        for (int j = 0; j < n; j++) {
            if (nums[j] != j + 1) {
                return j + 1;
            }
        }
        
        // If all 1 to n are present, the answer is n + 1
        return n + 1;
    }

    /**
     * Approach 2: Negative Marking
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public static int firstMissingPositiveNegativeMarking(int[] nums) {
        int n = nums.length;
        
        // Step 1: Clean up the array. 
        // Replace negatives, zeros, and numbers > n with a safe, ignorable value (n + 1)
        for (int i = 0; i < n; i++) {
            if (nums[i] <= 0 || nums[i] > n) {
                nums[i] = n + 1;
            }
        }
        
        // Step 2: Mark visited elements.
        // Use the index as a hash key. Mark the value at index (num - 1) as negative.
        for (int i = 0; i < n; i++) {
            int num = Math.abs(nums[i]);
            // If it's a valid number in our range
            if (num <= n) {
                int targetIndex = num - 1;
                // Make it negative to signify we have seen 'num'
                // Don't flip it again if it's already negative
                if (nums[targetIndex] > 0) {
                    nums[targetIndex] = -nums[targetIndex];
                }
            }
        }
        
        // Step 3: Find the first positive number.
        // The index of the first positive number + 1 is our missing number.
        for (int i = 0; i < n; i++) {
            if (nums[i] > 0) {
                return i + 1;
            }
        }
        
        return n + 1;
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
            new TestCase(new int[]{1, 2, 0}, 3, "Contains zero"),
            new TestCase(new int[]{3, 4, -1, 1}, 2, "Contains negative and gap"),
            new TestCase(new int[]{7, 8, 9, 11, 12}, 1, "All numbers greater than n"),
            new TestCase(new int[]{1, 2, 3, 4, 5}, 6, "Already perfectly sorted [1, n]"),
            new TestCase(new int[]{1, 1, 2, 2}, 3, "Contains duplicates"),
            new TestCase(new int[]{-5, -10, -3}, 1, "All negative numbers")
        );

        System.out.println("--- Testing Approach 1: Cyclic Sort ---");
        testCases.forEach(tc -> {
            int[] arrCopy = Arrays.copyOf(tc.input(), tc.input().length);
            int result = firstMissingPositiveCyclic(arrCopy);
            boolean passed = (result == tc.expected());
            System.out.printf("[%s] %s -> Expected: %d, Got: %d%n", 
                passed ? "PASS" : "FAIL", 
                tc.description(), tc.expected(), result);
        });

        System.out.println("\n--- Testing Approach 2: Negative Marking ---");
        testCases.forEach(tc -> {
            int[] arrCopy = Arrays.copyOf(tc.input(), tc.input().length);
            int result = firstMissingPositiveNegativeMarking(arrCopy);
            boolean passed = (result == tc.expected());
            System.out.printf("[%s] %s -> Expected: %d, Got: %d%n", 
                passed ? "PASS" : "FAIL", 
                tc.description(), tc.expected(), result);
        });
    }
}

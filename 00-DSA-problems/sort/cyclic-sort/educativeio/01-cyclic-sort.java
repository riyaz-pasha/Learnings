/**
 * ============================================================================
 * PROBLEM STATEMENT:
 * Given an integer array 'nums' of size n, where each number is distinct 
 * and falls within the range [1, n]. Sort the array in place with O(n) 
 * time complexity and O(1) extra space.
 * 
 * ============================================================================
 * INTERVIEW STRATEGY & CLARIFYING QUESTIONS:
 * Before writing any code, confirm these details with the interviewer:
 * 1. "Are there any duplicate numbers in the array?" (Confirming distinctness)
 * 2. "Are there any missing numbers in the range [1, n]?" (Confirming exactly 1 to n)
 * 3. "Is 'n' guaranteed to be greater than 0, or do I need to handle empty arrays?"
 * 4. "Are we allowed to modify the input array directly?" (Confirming in-place requirement)
 * 5. "If the array is already sorted, should the algorithm still be optimal?"
 * 6. "What should the function return? The modified array or void?"
 * 7. "Will the array elements always fit within a standard 32-bit signed integer?"
 * 8. "Can I assume the input array is not null?"
 * 
 * ============================================================================
 * RESTATING THE PROBLEM:
 * "We have an array of length N containing a random permutation of numbers 
 * from 1 to N. We need to physically rearrange the elements so they are in 
 * ascending order (1, 2, 3... N). We must do this without creating a new array 
 * (using only O(1) space) and we must do it in linear time (O(N))."
 * 
 * ============================================================================
 * IDEA, INTUITION & KEY OBSERVATIONS:
 * 1. The numbers strictly belong to the continuous range [1, n].
 * 2. The array size is exactly 'n'. 
 * 3. *Crucial Observation*: Because of constraints 1 and 2, every number has 
 *    a pre-destined, exact index where it belongs. 
 *    - The number 1 belongs at index 0.
 *    - The number 2 belongs at index 1.
 *    - The number 'x' belongs at index 'x - 1'.
 * 4. We cannot use standard comparison sorts (like Merge Sort or Quick Sort) 
 *    because their time complexity is O(N log N).
 * 5. We cannot use standard Counting Sort because it requires O(N) extra space 
 *    to store frequencies/output array.
 * 6. *Solution Concept*: CYCLIC SORT. 
 *    We iterate through the array. If the number at our current index 'i' 
 *    is not at its correct position (i.e., nums[i] != i + 1), we swap it 
 *    with the number sitting at its target index. We keep swapping the number 
 *    at index 'i' until the correct number for index 'i' lands there. 
 *    Then, we move to the next index.
 * 
 * ============================================================================
 * SOLUTION BREAKDOWN & VISUAL TRACING:
 * Example Array: [3, 1, 4, 2]
 * Indexes:        0  1  2  3
 * 
 * Step 1: i = 0. nums[0] is 3. 
 *         Does 3 belong at index 0? No, it belongs at index 2 (3 - 1).
 *         Swap nums[0] (3) and nums[2] (4).
 *         Array becomes: [4, 1, 3, 2]
 * 
 * Step 2: i = 0 (still). nums[0] is 4.
 *         Does 4 belong at index 0? No, it belongs at index 3 (4 - 1).
 *         Swap nums[0] (4) and nums[3] (2).
 *         Array becomes: [2, 1, 3, 4]
 * 
 * Step 3: i = 0 (still). nums[0] is 2.
 *         Does 2 belong at index 0? No, it belongs at index 1 (2 - 1).
 *         Swap nums[0] (2) and nums[1] (1).
 *         Array becomes: [1, 2, 3, 4]
 * 
 * Step 4: i = 0 (still). nums[0] is 1.
 *         Does 1 belong at index 0? Yes! Now we can increment i.
 * 
 * Step 5: i = 1. nums[1] is 2. Belongs at index 1. Yes! Increment i.
 * Step 6: i = 2. nums[2] is 3. Belongs at index 2. Yes! Increment i.
 * Step 7: i = 3. nums[3] is 4. Belongs at index 3. Yes! Increment i.
 * 
 * End of loop. Array is sorted.
 * 
 * TIME COMPLEXITY REASONING:
 * Even though there is a nested loop (a while loop inside a for loop, or just 
 * a while loop manipulating 'i'), every element is swapped at most ONCE into 
 * its correct position. Once a number is at its correct index, it is never 
 * moved again. Thus, at most N-1 swaps occur across the entire execution. 
 * Time complexity is strictly O(N). Space is strictly O(1) for the temp variable.
 * ============================================================================
 */

import java.util.Arrays;
import java.util.List;

public class CyclicSort1ToN {

    /**
     * Modern Java feature: Record to encapsulate test cases cleanly.
     * This avoids boilerplate code for getters, setters, and constructors.
     */
    record TestCase(int[] input, int[] expected, String description) {}

    /**
     * Approach 1: Using a 'while' loop to manage index traversal.
     * This is the classic Cyclic Sort implementation.
     * 
     * @param nums The array to be sorted in place.
     */
    public static void cyclicSortUsingWhile(int[] nums) {
        if (nums == null || nums.length <= 1) {
            return;
        }
        
        int i = 0;
        while (i < nums.length) {
            // The index where the current number *should* be
            int correctIndex = nums[i] - 1;
            
            // If the number is not already at its correct index, swap it.
            // (Note: Since elements are distinct, we don't need to worry about infinite loops
            // caused by duplicate elements, but a safer check is nums[i] != nums[correctIndex])
            if (nums[i] != nums[correctIndex]) {
                swap(nums, i, correctIndex);
            } else {
                // The current number is correct for this position, move forward
                i++;
            }
        }
    }

    /**
     * Approach 2: Using a 'for' loop with an inner 'while'.
     * Logically identical to Approach 1, just structured differently. 
     * Some people find this easier to read as it maps to "for every position, 
     * keep swapping until the position holds the right number."
     * 
     * @param nums The array to be sorted in place.
     */
    public static void cyclicSortUsingFor(int[] nums) {
        if (nums == null || nums.length <= 1) {
            return;
        }

        for (int i = 0; i < nums.length; i++) {
            // Keep swapping while the number at index 'i' is not the correct one
            while (nums[i] != i + 1) {
                int correctIndex = nums[i] - 1;
                swap(nums, i, correctIndex);
            }
        }
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
        
        // Define test cases using our Record
        List<TestCase> testCases = List.of(
            new TestCase(new int[]{3, 1, 4, 2}, new int[]{1, 2, 3, 4}, "Standard small array"),
            new TestCase(new int[]{5, 4, 3, 2, 1}, new int[]{1, 2, 3, 4, 5}, "Reverse sorted array"),
            new TestCase(new int[]{1, 2, 3, 4, 5}, new int[]{1, 2, 3, 4, 5}, "Already sorted array"),
            new TestCase(new int[]{2, 1}, new int[]{1, 2}, "Two elements"),
            new TestCase(new int[]{1}, new int[]{1}, "Single element")
        );

        System.out.println("--- Testing Approach 1: While Loop ---");
        testCases.forEach(tc -> {
            int[] arrCopy = Arrays.copyOf(tc.input(), tc.input().length);
            cyclicSortUsingWhile(arrCopy);
            boolean passed = Arrays.equals(arrCopy, tc.expected());
            System.out.printf("[%s] %s -> Result: %s%n", 
                passed ? "PASS" : "FAIL", 
                tc.description(), 
                Arrays.toString(arrCopy));
        });

        System.out.println("\n--- Testing Approach 2: For Loop with Inner While ---");
        testCases.forEach(tc -> {
            int[] arrCopy = Arrays.copyOf(tc.input(), tc.input().length);
            cyclicSortUsingFor(arrCopy);
            boolean passed = Arrays.equals(arrCopy, tc.expected());
            System.out.printf("[%s] %s -> Result: %s%n", 
                passed ? "PASS" : "FAIL", 
                tc.description(), 
                Arrays.toString(arrCopy));
        });
    }
}

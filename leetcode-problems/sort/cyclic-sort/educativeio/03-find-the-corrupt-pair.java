/**
 * ============================================================================
 * PROBLEM STATEMENT:
 * We are given an unsorted array, `nums`, with n elements where each element 
 * is in the range [1, n] inclusive. Due to a data error, one number is 
 * duplicated, which causes another number to be missing. 
 * Find and return the corrupt pair (missing, duplicated).
 * 
 * Constraints:
 * - 2 <= n <= 10^3
 * - 1 <= nums[i] <= n
 * 
 * ============================================================================
 * INTERVIEW STRATEGY & CLARIFYING QUESTIONS:
 * Before jumping into the code, ask the interviewer these questions to 
 * establish boundaries and demonstrate senior-level thinking:
 * 
 * 1. "Can I modify the input array?" 
 *    (Crucial for Cyclic Sort and Negative Marking approaches. If no, we are 
 *    forced into Math or extra-space approaches.)
 * 2. "Should the result be returned as an array or a specific object?"
 *    (Let's assume an int[] of size 2 containing {missing, duplicated}).
 * 3. "Is there any risk of the array size being larger than 10^5? Specifically, 
 *    could the mathematical sum of squares exceed the limit of a 64-bit integer?"
 *    (Shows awareness of integer overflow risks.)
 * 4. "Are the elements strictly positive? Is zero included?"
 *    (Confirmed by constraints: [1, n], so zero is not included.)
 * 5. "What should we return if the input is somehow invalid or smaller than 2?"
 *    (Usually, returning an empty array or throwing an Exception is standard).
 * 
 * ============================================================================
 * RESTATING THE PROBLEM:
 * We have an array of size N that is supposed to contain every number from 
 * 1 to N exactly once. But someone replaced one valid number with a duplicate 
 * of another number already in the array. We need to identify which number 
 * disappeared (the missing) and which one took its place (the duplicate).
 * 
 * ============================================================================
 * IDEA, INTUITION & KEY OBSERVATIONS (3 Different Solutions):
 * 
 * APPROACH 1: CYCLIC SORT (Optimal for In-Place / O(1) Space)
 * - Since the numbers are in the range [1, n], each number `x` belongs at 
 *   index `x - 1`. 
 * - We can iterate through the array and repeatedly swap elements to their 
 *   correct positions.
 * - If we find a number whose target position is already occupied by the same 
 *   number, we've found our duplicate! 
 * - After sorting, a final pass will reveal the index `i` that holds the wrong 
 *   number. `i + 1` is the missing number, and `nums[i]` is the duplicate.
 * - Time: O(N), Space: O(1)
 * 
 * APPROACH 2: MATH (Optimal if Array CANNOT be modified)
 * - Let X be the missing number and Y be the duplicated number.
 * - Sum Expected (1 to n) = n * (n + 1) / 2
 * - Sum Actual = Sum of all elements in nums.
 * - Diff1 = Sum Expected - Sum Actual = X - Y
 * - Sum Sq Expected (1^2 to n^2) = n * (n + 1) * (2n + 1) / 6
 * - Sum Sq Actual = Sum of nums[i]^2
 * - Diff2 = Sum Sq Expected - Sum Sq Actual = X^2 - Y^2
 * - Using algebra: X^2 - Y^2 = (X - Y)(X + Y). 
 *   So, X + Y = Diff2 / Diff1.
 * - Now we have X - Y and X + Y. We can solve for X and Y easily!
 * - Time: O(N), Space: O(1). Requires `long` to prevent overflow.
 * 
 * APPROACH 3: NEGATIVE MARKING (Alternative In-Place / O(1) Space)
 * - We can use the array elements as indices.
 * - For each number `x` in the array, we go to index `|x| - 1` and flip its 
 *   sign to negative.
 * - If the value at `|x| - 1` is ALREADY negative, it means we've seen `x` 
 *   before. Thus, `|x|` is our duplicate.
 * - Finally, the index `i` that still has a positive value indicates that 
 *   the number `i + 1` was never encountered. Thus, `i + 1` is missing.
 * - Time: O(N), Space: O(1)
 * 
 * ============================================================================
 * SOLUTION BREAKDOWN & VISUAL TRACING (Cyclic Sort):
 * Example Array: [3, 1, 2, 5, 3], N = 5
 * Target indices: 0  1  2  3  4 (for numbers 1, 2, 3, 4, 5)
 * 
 * Step 1: i=0, nums[0]=3. Target idx for 3 is 2. 
 *         nums[0] (3) != nums[2] (2). Swap!
 *         Array: [2, 1, 3, 5, 3]
 * 
 * Step 2: i=0, nums[0]=2. Target idx for 2 is 1.
 *         nums[0] (2) != nums[1] (1). Swap!
 *         Array: [1, 2, 3, 5, 3]
 * 
 * Step 3: i=0, nums[0]=1. Target idx for 1 is 0. Matches! Move to i=1.
 * Step 4: i=1, nums[1]=2. Matches target. Move to i=2.
 * Step 5: i=2, nums[2]=3. Matches target. Move to i=3.
 * 
 * Step 6: i=3, nums[3]=5. Target idx for 5 is 4.
 *         nums[3] (5) != nums[4] (3). Swap!
 *         Array: [1, 2, 3, 3, 5]
 * 
 * Step 7: i=3, nums[3]=3. Target idx for 3 is 2.
 *         nums[3] (3) == nums[2] (3). They match! It's a duplicate. 
 *         We break/continue.
 * 
 * Step 8: Final Pass. Check where nums[i] != i + 1.
 *         Index 3 has value 3, but should have 4.
 *         Therefore: Missing = 3 + 1 = 4. Duplicate = nums[3] = 3.
 *         Result: [4, 3]
 * ============================================================================
 */

import java.util.Arrays;
import java.util.List;

public class FindCorruptPair {

    /**
     * Modern Java Record to cleanly define our test cases.
     */
    record TestCase(int[] input, int[] expected, String description) {}

    /**
     * Approach 1: Cyclic Sort
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * Note: Modifies the original array.
     */
    public static int[] findCorruptPairCyclicSort(int[] nums) {
        int i = 0;
        int n = nums.length;
        
        while (i < n) {
            int correctIndex = nums[i] - 1;
            // If the element is not at its correct index, and the target index
            // doesn't already hold the correct element (avoids infinite loop on duplicates)
            if (nums[i] != nums[correctIndex]) {
                swap(nums, i, correctIndex);
            } else {
                i++;
            }
        }
        
        // Find the index that doesn't have the correct number
        for (int j = 0; j < n; j++) {
            if (nums[j] != j + 1) {
                // missing is j + 1, duplicate is nums[j]
                return new int[]{j + 1, nums[j]};
            }
        }
        
        return new int[]{-1, -1};
    }

    /**
     * Approach 2: Mathematical Equations
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * Note: Does NOT modify the array. Perfect for read-only inputs.
     */
    public static int[] findCorruptPairMath(int[] nums) {
        long n = nums.length;
        
        long expectedSum = n * (n + 1) / 2;
        long expectedSumSq = n * (n + 1) * (2 * n + 1) / 6;
        
        long actualSum = 0;
        long actualSumSq = 0;
        
        for (long num : nums) {
            actualSum += num;
            actualSumSq += num * num;
        }
        
        // diff1 = X - Y (Missing - Duplicate)
        long diff1 = expectedSum - actualSum;
        
        // diff2 = X^2 - Y^2
        long diff2 = expectedSumSq - actualSumSq;
        
        // sumXAndY = X + Y = (X^2 - Y^2) / (X - Y) = diff2 / diff1
        long sumXAndY = diff2 / diff1;
        
        // X = (diff1 + sumXAndY) / 2
        int missing = (int) ((diff1 + sumXAndY) / 2);
        
        // Y = sumXAndY - X
        int duplicate = (int) (sumXAndY - missing);
        
        return new int[]{missing, duplicate};
    }

    /**
     * Approach 3: Negative Marking
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * Note: Modifies the original array.
     */
    public static int[] findCorruptPairNegativeMarking(int[] nums) {
        int missing = -1;
        int duplicate = -1;
        
        // Step 1: Mark visited elements as negative
        for (int i = 0; i < nums.length; i++) {
            int val = Math.abs(nums[i]);
            int indexToMark = val - 1;
            
            if (nums[indexToMark] < 0) {
                // If it's already negative, we've found our duplicate
                duplicate = val;
            } else {
                nums[indexToMark] = -nums[indexToMark];
            }
        }
        
        // Step 2: Find the missing element
        // The index that still holds a positive value corresponds to the missing number
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) {
                missing = i + 1;
                break;
            }
        }
        
        return new int[]{missing, duplicate};
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
            new TestCase(new int[]{3, 1, 2, 5, 3}, new int[]{4, 3}, "Basic case"),
            new TestCase(new int[]{1, 2, 2, 4}, new int[]{3, 2}, "Missing in middle"),
            new TestCase(new int[]{2, 2}, new int[]{1, 2}, "Minimum constraints (N=2)"),
            new TestCase(new int[]{4, 3, 4, 5, 1}, new int[]{2, 4}, "Unsorted large")
        );

        System.out.println("--- Testing Approach 1: Cyclic Sort ---");
        testCases.forEach(tc -> {
            int[] arrCopy = Arrays.copyOf(tc.input(), tc.input().length);
            int[] result = findCorruptPairCyclicSort(arrCopy);
            boolean passed = Arrays.equals(result, tc.expected());
            System.out.printf("[%s] %s -> Expected: %s, Got: %s%n", 
                passed ? "PASS" : "FAIL", 
                tc.description(), Arrays.toString(tc.expected()), Arrays.toString(result));
        });

        System.out.println("\n--- Testing Approach 2: Mathematical Sums ---");
        testCases.forEach(tc -> {
            int[] result = findCorruptPairMath(tc.input());
            boolean passed = Arrays.equals(result, tc.expected());
            System.out.printf("[%s] %s -> Expected: %s, Got: %s%n", 
                passed ? "PASS" : "FAIL", 
                tc.description(), Arrays.toString(tc.expected()), Arrays.toString(result));
        });

        System.out.println("\n--- Testing Approach 3: Negative Marking ---");
        testCases.forEach(tc -> {
            int[] arrCopy = Arrays.copyOf(tc.input(), tc.input().length);
            int[] result = findCorruptPairNegativeMarking(arrCopy);
            boolean passed = Arrays.equals(result, tc.expected());
            System.out.printf("[%s] %s -> Expected: %s, Got: %s%n", 
                passed ? "PASS" : "FAIL", 
                tc.description(), Arrays.toString(tc.expected()), Arrays.toString(result));
        });
    }
}

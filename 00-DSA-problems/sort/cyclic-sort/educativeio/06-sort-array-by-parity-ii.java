/**
 * ============================================================================
 * PROBLEM STATEMENT:
 * You are given an integer array 'nums', where exactly half of the elements 
 * are even, and the other half are odd.
 * Rearrange nums such that:
 * - All even numbers are placed at even indexes [0, 2, 4, ...].
 * - All odd numbers are placed at odd indexes [1, 3, 5, ...].
 * You may return any valid arrangement that satisfies these conditions.
 * 
 * Constraints:
 * - 2 <= nums.length <= 1000
 * - nums.length is even.
 * - Half of the integers in nums are even.
 * - 0 <= nums[i] <= 1000
 * 
 * ============================================================================
 * INTERVIEW STRATEGY & CLARIFYING QUESTIONS:
 * Before coding, ask the interviewer these questions to show senior-level 
 * foresight:
 * 1. "Can I modify the input array in place to save memory, or should I 
 *     return a completely new array?" (They will usually prefer in-place).
 * 2. "Does the relative order of the even and odd numbers matter?" 
 *    (e.g., if input is [4, 2, 3, 1], is [4, 3, 2, 1] valid? The prompt says 
 *    "any valid arrangement", so relative order shouldn't matter).
 * 3. "Are there negative numbers?" 
 *    (Constraints say 0 <= nums[i], so no. But if there were, we'd need to 
 *    use 'nums[i] % 2 != 0' instead of 'nums[i] % 2 == 1' for odd checks).
 * 4. "Is it guaranteed that the array has exactly half even and half odd?" 
 *    (Yes, per constraints, so we don't need to handle impossible cases).
 * 
 * ============================================================================
 * RESTATING THE PROBLEM:
 * We have an array with an equal number of evens and odds. We need to seat 
 * them alternately: Even, Odd, Even, Odd. We don't care which even or which 
 * odd goes where, as long as evens are on seats 0, 2, 4 and odds are on 1, 3, 5.
 * 
 * ============================================================================
 * IDEA, INTUITION & KEY OBSERVATIONS (Two Solutions):
 * 
 * APPROACH 1: Two Pointers (In-Place, Optimal Space)
 * - We can use two pointers. Let 'evenPtr' start at index 0 and 'oddPtr' start 
 *   at index 1.
 * - We step 'evenPtr' forward by 2 as long as it points to an even number.
 * - We step 'oddPtr' forward by 2 as long as it points to an odd number.
 * - If 'evenPtr' finds an odd number, and 'oddPtr' finds an even number, 
 *   they are both in the wrong seats! We swap them.
 * - We repeat this until one of the pointers goes out of bounds.
 * - Time: O(n), Space: O(1).
 * 
 * APPROACH 2: Extra Array (Simple and Readable)
 * - Create a new array 'result' of the same size.
 * - Maintain two indices: evenIdx = 0, oddIdx = 1.
 * - Loop through the original array. If the number is even, place it at 
 *   result[evenIdx] and evenIdx += 2. If odd, place it at result[oddIdx] 
 *   and oddIdx += 2.
 * - Time: O(n), Space: O(n).
 * 
 * ============================================================================
 * SOLUTION BREAKDOWN & VISUAL TRACING (Two Pointers):
 * Example Array: [3, 1, 2, 4]
 * Indices:        0  1  2  3
 * 
 * Initial state:
 * evenPtr = 0 (value 3), oddPtr = 1 (value 1)
 * 
 * Step 1:
 * Look at evenPtr (0). nums[0] is 3. 3 is ODD. It is in the wrong place. 
 * evenPtr pauses here.
 * 
 * Step 2:
 * Look at oddPtr (1). nums[1] is 1. 1 is ODD. It is in the correct place!
 * oddPtr += 2 -> oddPtr becomes 3.
 * 
 * Step 3:
 * Look at oddPtr (3). nums[3] is 4. 4 is EVEN. It is in the wrong place.
 * oddPtr pauses here.
 * 
 * Step 4:
 * Both pointers are paused on misplaced elements. 
 * Swap nums[evenPtr] and nums[oddPtr] -> Swap nums[0] and nums[3].
 * Array becomes: [4, 1, 2, 3]
 * 
 * Step 5:
 * Advance both pointers by 2. 
 * evenPtr = 2, oddPtr = 5 (out of bounds).
 * Loop terminates. 
 * 
 * Final Array: [4, 1, 2, 3] -> Valid!
 * ============================================================================
 */

import java.util.Arrays;
import java.util.List;

public class SortArrayByParityII {

    /**
     * Modern Java Record to cleanly encapsulate test cases.
     * Since multiple valid arrangements exist, we don't store an 'expected' array.
     * Instead, we will write a validator method.
     */
    record TestCase(int[] input, String description) {}

    /**
     * Approach 1: Two Pointers In-Place
     * Time Complexity: O(n) - Each pointer travels the array at most once.
     * Space Complexity: O(1) - Sorting is done in place.
     * 
     * @param nums The array to be rearranged.
     * @return The rearranged array.
     */
    public static int[] sortArrayByParityInPlace(int[] nums) {
        int evenPtr = 0;
        int oddPtr = 1;
        int n = nums.length;

        while (evenPtr < n && oddPtr < n) {
            // Move evenPtr forward if it's currently pointing to an even number
            while (evenPtr < n && nums[evenPtr] % 2 == 0) {
                evenPtr += 2;
            }
            
            // Move oddPtr forward if it's currently pointing to an odd number
            while (oddPtr < n && nums[oddPtr] % 2 != 0) {
                oddPtr += 2;
            }
            
            // If both pointers are still within bounds, they found misplaced elements
            if (evenPtr < n && oddPtr < n) {
                swap(nums, evenPtr, oddPtr);
            }
        }
        
        return nums;
    }

    /**
     * Approach 2: Using Extra Space
     * Time Complexity: O(n)
     * Space Complexity: O(n) - Creates a new array.
     */
    public static int[] sortArrayByParityExtraSpace(int[] nums) {
        int[] result = new int[nums.length];
        int evenIdx = 0;
        int oddIdx = 1;

        for (int num : nums) {
            if (num % 2 == 0) {
                result[evenIdx] = num;
                evenIdx += 2;
            } else {
                result[oddIdx] = num;
                oddIdx += 2;
            }
        }

        return result;
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
     * Helper method to validate if an array meets the condition:
     * evens at even indices, odds at odd indices.
     */
    private static boolean isValid(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (i % 2 == 0 && arr[i] % 2 != 0) return false;
            if (i % 2 != 0 && arr[i] % 2 == 0) return false;
        }
        return true;
    }

    /**
     * Main execution using Java Streams and Records for clean testing.
     */
    public static void main(String[] args) {
        
        List<TestCase> testCases = List.of(
            new TestCase(new int[]{4, 2, 5, 7}, "Standard mixed array"),
            new TestCase(new int[]{3, 1, 2, 4}, "Odds first, then evens"),
            new TestCase(new int[]{2, 3}, "Minimum size array"),
            new TestCase(new int[]{1, 3, 5, 2, 4, 6}, "All odds grouped, then all evens")
        );

        System.out.println("--- Testing Approach 1: Two Pointers In-Place (O(1) Space) ---");
        testCases.forEach(tc -> {
            int[] arrCopy = Arrays.copyOf(tc.input(), tc.input().length);
            int[] result = sortArrayByParityInPlace(arrCopy);
            boolean passed = isValid(result);
            
            System.out.printf("[%s] %s%n", passed ? "PASS" : "FAIL", tc.description());
            System.out.printf("   Input:  %s%n", Arrays.toString(tc.input()));
            System.out.printf("   Result: %s%n%n", Arrays.toString(result));
        });

        System.out.println("--- Testing Approach 2: Extra Space (O(N) Space) ---");
        testCases.forEach(tc -> {
            int[] result = sortArrayByParityExtraSpace(tc.input());
            boolean passed = isValid(result);
            
            System.out.printf("[%s] %s%n", passed ? "PASS" : "FAIL", tc.description());
            System.out.printf("   Input:  %s%n", Arrays.toString(tc.input()));
            System.out.printf("   Result: %s%n%n", Arrays.toString(result));
        });
    }
}

/**
 * ============================================================================
 * 1. INTERVIEW PREPARATION
 * ============================================================================
 * 
 * Problem Restatement:
 * We have an array of integers where exactly 50% of the elements are even numbers 
 * and 50% are odd numbers. We need to rearrange the array so that every even 
 * number sits at an even-numbered index (0, 2, 4...) and every odd number sits 
 * at an odd-numbered index (1, 3, 5...). Any valid configuration that meets this 
 * parity rule is acceptable.
 * 
 * Clarifying Questions (A senior candidate MUST ask these before coding):
 * 1. "Is the relative ordering of the elements important?" 
 *    (e.g., should the first even number in the original array be the first 
 *    even number in the output? The prompt says 'any valid arrangement', so no.)
 * 2. "Should the transformation be done strictly in-place to save space, or is 
 *    allocating a new array acceptable?" 
 *    (I will provide both, but clarifying space constraints early is critical).
 * 3. "Are there negative numbers in the input?" 
 *    (Constraints say 0 <= nums[i] <= 1000, so we just use nums[i] % 2 == 0).
 * 4. "Can I assume the input array is always valid (even length, exactly half 
 *    even/odd)?" 
 *    (Constraints guarantee this, but it's good to double check error-handling needs).
 * 
 * Core Intuition:
 * Since exactly half the array is even and half is odd, the number of "misplaced" 
 * even numbers (evens sitting at odd indices) is guaranteed to be exactly equal 
 * to the number of "misplaced" odd numbers (odds sitting at even indices). 
 * Therefore, if we find a misplaced even number and a misplaced odd number, 
 * swapping them fixes BOTH of their positions simultaneously.
 * ============================================================================
 */
public class ParityRearranger {

    /**
     * ========================================================================
     * 2. SOLUTIONS & IMPLEMENTATIONS
     * ========================================================================
     * 
     * APPROACH 1: Brute Force (Bucketing)
     * 
     * Idea: 
     * We can filter the numbers into two separate lists: one for all the even 
     * numbers and one for all the odd numbers. Once segregated, we can iterate 
     * through the original array and overwrite it by pulling from the even list 
     * for even indices, and the odd list for odd indices.
     * 
     * Visual / Tracing:
     * nums = [4, 2, 5, 7]
     * Evens List: [4, 2]
     * Odds List: [5, 7]
     * 
     * Rebuild nums:
     * Index 0 (Even) -> pop Evens -> [4, _, _, _]
     * Index 1 (Odd)  -> pop Odds  -> [4, 5, _, _]
     * Index 2 (Even) -> pop Evens -> [4, 5, 2, _]
     * Index 3 (Odd)  -> pop Odds  -> [4, 5, 2, 7]
     * 
     * Complexity Analysis:
     * - Time Complexity: O(n). We iterate through the array to split elements, 
     *   and iterate again to rebuild it. O(2n) simplifies to O(n).
     * - Space Complexity: O(n). We store all elements in auxiliary lists.
     * 
     * @param nums The array to rearrange.
     * @return The rearranged array.
     */
    public int[] rearrangeBruteForce(int[] nums) {
        java.util.List<Integer> evens = new java.util.ArrayList<>();
        java.util.List<Integer> odds = new java.util.ArrayList<>();
        
        for (int num : nums) {
            if (num % 2 == 0) evens.add(num);
            else odds.add(num);
        }
        
        int evenPtr = 0;
        int oddPtr = 0;
        
        for (int i = 0; i < nums.length; i++) {
            if (i % 2 == 0) {
                nums[i] = evens.get(evenPtr++);
            } else {
                nums[i] = odds.get(oddPtr++);
            }
        }
        return nums;
    }

    /**
     * APPROACH 2: Sub-Optimal (Two Pointers with Extra Array)
     * 
     * Idea: 
     * Instead of using separate lists and doing two passes, we can do this in 
     * a single pass. We create a result array of the same size. We maintain 
     * an 'evenIndex' starting at 0 and an 'oddIndex' starting at 1. We iterate 
     * through the original array: if a number is even, place it at evenIndex 
     * and increment by 2. Do the equivalent for odd.
     * 
     * Visual / Tracing:
     * nums = [4, 2, 5, 7], res = [0, 0, 0, 0]
     * evenIdx = 0, oddIdx = 1
     * 
     * i = 0, num = 4 (Even) -> res[0] = 4, evenIdx += 2 (now 2)
     * i = 1, num = 2 (Even) -> res[2] = 2, evenIdx += 2 (now 4)
     * i = 2, num = 5 (Odd)  -> res[1] = 5, oddIdx += 2 (now 3)
     * i = 3, num = 7 (Odd)  -> res[3] = 7, oddIdx += 2 (now 5)
     * res = [4, 5, 2, 7]
     * 
     * Complexity Analysis:
     * - Time Complexity: O(n). Single pass through the input array.
     * - Space Complexity: O(n). Allocating a new array of size n.
     * 
     * @param nums The array to rearrange.
     * @return A newly allocated, rearranged array.
     */
    public int[] rearrangeOptimalExtraSpace(int[] nums) {
        int[] result = new int[nums.length];
        int evenIndex = 0;
        int oddIndex = 1;
        
        for (int num : nums) {
            if (num % 2 == 0) {
                result[evenIndex] = num;
                evenIndex += 2;
            } else {
                result[oddIndex] = num;
                oddIndex += 2;
            }
        }
        
        return result;
    }

    /**
     * APPROACH 3: Optimal (Two Pointers In-Place)
     * 
     * Idea: 
     * We can achieve O(1) space by modifying the array in-place. We use two 
     * pointers: one iterating over even indices (0, 2, 4...) and one iterating 
     * over odd indices (1, 3, 5...).
     * We scan the array. If the even pointer points to an even number, it's 
     * correct, so we advance it by 2. If the odd pointer points to an odd number, 
     * it's also correct, advance by 2. 
     * If BOTH point to incorrect numbers (even pointer sees an odd, and odd 
     * pointer sees an even), we swap them! They both become correct.
     * 
     * Visual / Tracing:
     * nums = [4, 2, 5, 7]
     * even = 0, odd = 1
     * 
     * Step 1: nums[even(0)] is 4. Correct. even += 2 -> even = 2.
     * Step 2: nums[even(2)] is 5. Incorrect! Pause even pointer.
     * Step 3: nums[odd(1)] is 2. Incorrect! Pause odd pointer.
     * Step 4: Swap nums[2] (5) and nums[1] (2).
     *         nums becomes [4, 5, 2, 7].
     *         even += 2 -> even = 4.
     *         odd += 2 -> odd = 3.
     * Step 5: even = 4 (out of bounds). Loop terminates.
     * 
     * Complexity Analysis:
     * - Time Complexity: O(n). Each element is visited at most once by either 
     *   pointer.
     * - Space Complexity: O(1). Only two pointer variables are used, modifying 
     *   the array directly.
     * 
     * @param nums The array to rearrange in-place.
     * @return The rearranged array (same reference).
     */
    public int[] rearrangeOptimalInPlace(int[] nums) {
        int even = 0;
        int odd = 1;
        int n = nums.length;
        
        while (even < n && odd < n) {
            // Find an odd number sitting at an even index
            while (even < n && nums[even] % 2 == 0) {
                even += 2;
            }
            
            // Find an even number sitting at an odd index
            while (odd < n && nums[odd] % 2 != 0) {
                odd += 2;
            }
            
            // If both pointers are still within bounds, swap the misplaced elements
            if (even < n && odd < n) {
                int temp = nums[even];
                nums[even] = nums[odd];
                nums[odd] = temp;
                
                // Move pointers forward after the swap
                even += 2;
                odd += 2;
            }
        }
        
        return nums;
    }

    /**
     * Main method to demonstrate and test the implementations.
     */
    public static void main(String[] args) {
        var rearranger = new ParityRearranger();
        
        // Test Case 1: Standard out-of-order array
        int[] test1 = {4, 2, 5, 7};
        rearranger.rearrangeOptimalInPlace(test1);
        System.out.println("Test 1 Result (In-Place): " + java.util.Arrays.toString(test1)); 
        // Expected: [4, 5, 2, 7] or similar valid configuration

        // Test Case 2: Already properly configured array
        int[] test2 = {2, 3, 4, 5};
        rearranger.rearrangeOptimalInPlace(test2);
        System.out.println("Test 2 Result (In-Place): " + java.util.Arrays.toString(test2)); 
        // Expected: [2, 3, 4, 5]

        // Test Case 3: Using the Sub-Optimal Extra Space approach
        int[] test3 = {1, 3, 2, 4};
        int[] res3 = rearranger.rearrangeOptimalExtraSpace(test3);
        System.out.println("Test 3 Result (Extra Space): " + java.util.Arrays.toString(res3)); 
        // Expected: [2, 1, 4, 3] or similar valid configuration
    }
}


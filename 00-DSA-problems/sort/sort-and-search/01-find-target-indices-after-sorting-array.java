/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of positive integers and a target integer. 
 * We need to imagine the array sorted in non-decreasing (ascending) order.
 * In this sorted version of the array, we must find all the 0-based indices 
 * where the element is exactly equal to the target. 
 * If the target doesn't exist in the array, we return an empty list.
 * The output list must be sorted in increasing order.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW (Confirm before writing code)
 * ============================================================================
 * 1. Can the input array be empty? (Constraints say length >= 1, but always good to ask).
 * 2. What should we return if the target is not found? (An empty list/array).
 * 3. Are we allowed to modify/mutate the original input array? (Important for in-place sorting).
 * 4. Can the array contain duplicates? (Yes, the problem implies this by asking for a "list of indices").
 * 5. Are the numbers guaranteed to fit in standard integer types? (Constraints say up to 100, so yes).
 * 6. Is time complexity a strict concern? Should I aim for O(N) or is O(N log N) acceptable given N <= 100?
 * 7. Do you prefer the output as a primitive array (int[]) or a dynamic list (List<Integer>)?
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Acknowledge the straightforward approach: Sort the array and then iterate through 
 *    it to find the target indices. State its time complexity: O(N log N).
 * 2. Ask if we can do better. Notice that to find the indices of `target` in a sorted array,
 *    we don't actually need to sort the *entire* array.
 * 3. Formulate the O(N) optimization: We only need to know exactly how many elements are 
 *    strictly smaller than `target`. If there are `k` elements smaller than `target`, 
 *    the first occurrence of `target` in the sorted array will be at index `k`.
 * 4. We also need to know how many times `target` appears in the array to know how many 
 *    indices to return.
 * 5. Present the O(N) Time, O(1) Space (excluding output list) counting solution.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING
 * ============================================================================
 * Example: nums = [1, 2, 5, 2, 3], target = 2
 * 
 * Approach 1: Sorting
 * Original: [ 1,  2,  5,  2,  3 ]
 * Sorted:   [ 1,  2,  2,  3,  5 ]
 * Indices:    0   1   2   3   4
 *                 ^   ^
 *                 Target matches at indices 1 and 2.
 * Return: [1, 2]
 * 
 * Approach 2: Counting (Optimal)
 * We want target = 2.
 * Iterate through nums = [1, 2, 5, 2, 3]:
 * - '1' < 2  --> lessThanCount = 1
 * - '2' == 2 --> equalCount = 1
 * - '5' > 2  --> ignore
 * - '2' == 2 --> equalCount = 2
 * - '3' > 2  --> ignore
 * 
 * Results: lessThanCount = 1, equalCount = 2
 * In a sorted array, the target '2' will start at index `lessThanCount` (index 1).
 * It will appear `equalCount` times (2 times).
 * Therefore, target occupies indices: 1, 1+1 = 2.
 * Return: [1, 2]
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class TargetIndicesAfterSorting {

    public static void main(String[] args) {
        int[] nums = {1, 2, 5, 2, 3};
        int target = 2;

        System.out.println("Input: " + Arrays.toString(nums) + ", Target: " + target);
        
        // Testing Solution 1
        List<Integer> result1 = targetIndicesSorting(nums.clone(), target);
        System.out.println("Solution 1 (Sorting) Output: " + result1);

        // Testing Solution 2
        List<Integer> result2 = targetIndicesCounting(nums, target);
        System.out.println("Solution 2 (Counting) Output: " + result2);

        // Testing Solution 3
        List<Integer> result3 = targetIndicesStreams(nums, target);
        System.out.println("Solution 3 (Streams) Output: " + result3);
    }

    /**
     * SOLUTION 1: Sorting (Brute Force / Intuitive)
     * 
     * Idea: Literally do what the problem says. Sort the array, then iterate
     * to find where nums[i] == target.
     * 
     * Time Complexity: O(N log N) - due to sorting the array.
     * Space Complexity: O(1) or O(N) depending on the sorting algorithm used internally,
     *                   and whether modifying the input array is allowed.
     */
    public static List<Integer> targetIndicesSorting(int[] nums, int target) {
        // 1. Sort the array
        Arrays.sort(nums);
        
        List<Integer> result = new ArrayList<>();
        
        // 2. Iterate and collect indices where element matches target
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == target) {
                result.add(i);
            } else if (nums[i] > target) {
                // Since the array is sorted, we can stop searching early 
                // once we exceed the target.
                break;
            }
        }
        
        return result;
    }

    /**
     * SOLUTION 2: Counting (Optimal O(N) Approach)
     * 
     * Idea: We don't need to sort the array. The final position(s) of the target
     * depend purely on:
     * 1. How many numbers are smaller than the target (this determines the starting index).
     * 2. How many numbers equal the target (this determines how many indices to generate).
     * 
     * Time Complexity: O(N) - single pass through the array.
     * Space Complexity: O(1) - only primitive integer counters are used (excluding output space).
     */
    public static List<Integer> targetIndicesCounting(int[] nums, int target) {
        int lessThanTargetCount = 0;
        int targetCount = 0;

        // 1. Single pass to count occurrences
        for (int num : nums) {
            if (num < target) {
                lessThanTargetCount++;
            } else if (num == target) {
                targetCount++;
            }
        }

        // 2. Generate the result based on counts
        List<Integer> result = new ArrayList<>(targetCount);
        
        // The first index of target will be `lessThanTargetCount`
        // We add `targetCount` consecutive indices.
        for (int i = 0; i < targetCount; i++) {
            result.add(lessThanTargetCount + i);
        }

        return result;
    }

    /**
     * SOLUTION 3: Modern Java (Using Streams)
     * 
     * Idea: Same logic as Solution 2, but utilizing Java Streams for a more 
     * declarative, functional programming style. 
     * 
     * Time Complexity: O(N) - technically two stream passes, so O(2N) which is O(N).
     * Space Complexity: O(1) extra space.
     * 
     * Note: In a highly performance-critical path, traditional loops (Solution 2) 
     * avoid the slight overhead of stream creation, but for cleaner readable code, 
     * this is highly acceptable in modern Java.
     */
    public static List<Integer> targetIndicesStreams(int[] nums, int target) {
        // Count elements strictly less than target
        long lessCount = Arrays.stream(nums)
                               .filter(num -> num < target)
                               .count();
                               
        // Count elements exactly equal to target
        long equalCount = Arrays.stream(nums)
                                .filter(num -> num == target)
                                .count();

        // Generate the sequence of indices
        // IntStream.range(startInclusive, endExclusive)
        return IntStream.range((int) lessCount, (int) (lessCount + equalCount))
                        .boxed() // converts IntStream to Stream<Integer>
                        .toList(); // Java 16+ feature for unmodifiable list
    }
}

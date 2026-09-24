import java.util.*;

class FindMinimumInRotatedArrayWithDuplicates {

    /**
     * ================================================================
     * 🔥 Find Minimum in Rotated Sorted Array (WITH DUPLICATES)
     * ================================================================
     *
     * Core Idea:
     * ----------
     * We use Binary Search, but duplicates introduce ambiguity.
     *
     * Instead of strict partitioning, we:
     *   - Compare nums[mid] with nums[high]
     *   - Decide which side to explore
     *   - Shrink space when duplicates block decision
     *
     * WHY compare with HIGH?
     * ----------------------
     * Because HIGH is always part of a valid sorted region (or equal),
     * making it reliable for comparison.
     *
     * ================================================================
     */

    public static int findMin(int[] nums) {

        int low = 0;
        int high = nums.length - 1;

        int answer = Integer.MAX_VALUE; // Explicit tracking (as per your preference)

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // Update answer (track minimum seen so far)
            answer = Math.min(answer, nums[mid]);

            /**
             * Case 1:
             * nums[mid] < nums[high]
             *
             * Right half is sorted.
             * So minimum must be in LEFT side (including mid).
             */
            if (nums[mid] < nums[high]) {
                high = mid - 1;
            }

            /**
             * Case 2:
             * nums[mid] > nums[high]
             *
             * Minimum lies in RIGHT half (excluding mid).
             */
            else if (nums[mid] > nums[high]) {
                low = mid + 1;
            }

            /**
             * Case 3 (IMPORTANT):
             * nums[mid] == nums[high]
             *
             * We cannot decide the side.
             * So we safely shrink search space.
             *
             * WHY safe?
             * Because nums[high] == nums[mid],
             * removing high does NOT remove the minimum.
             */
            else {
                high--; // shrink
            }
        }

        return answer;
    }

    public static void main(String[] args) {
        int[] nums = {2, 2, 2, 0, 1};
        System.out.println(findMin(nums)); // Output: 0
    }
}

/**
 * Problem Statement:
 * You are given an array `nums` of length n that was originally sorted in ascending order.
 * The array has been rotated between 1 and n times. It MAY contain duplicate elements.
 * Return the minimum element in the array.
 * 
 * Constraints:
 * - 1 <= nums.length <= 1000
 * - -1000 <= nums[i] <= 1000
 * - nums is sorted and rotated between 1 and n times.
 */
class FindMinRotatedSortedArrayDuplicates {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal Average Case)
     * 
     * Time Complexity: O(log N) average, O(N) worst case (if all elements are duplicates).
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * We compare the middle element (`nums[mid]`) with the rightmost element (`nums[high]`).
     * 
     * Array: [ 3, 3, 3, 1, 3 ]
     * 
     * Scenario A: nums[mid] < nums[high]
     * The right half is strictly sorted. The minimum must be `mid` or somewhere to the left.
     * We discard the right half.
     * 
     * Scenario B: nums[mid] > nums[high]
     * The minimum MUST be in the right half because the rotation boundary is there.
     * We discard the left half.
     * 
     * Scenario C: nums[mid] == nums[high]
     * We cannot be sure which half contains the minimum. 
     * E.g., [3, 1, 3, 3, 3] vs [3, 3, 3, 1, 3]
     * However, since nums[mid] == nums[high], we know nums[high] has a duplicate.
     * We can safely discard just `nums[high]` by doing high--. 
     * This shrinks the search space safely without missing the minimum.
     * 
     * Tracking the result:
     * Instead of relying on pointer states at the end, we simply update an explicit 
     * `result` variable with Math.min() at every step.
     */
    public static int findMinIterative(int[] nums) {
        int low = 0;
        int high = nums.length - 1;
        
        // Explicit result variable as requested, initialized to the first element
        int result = nums[0]; 

        while (low <= high) {
            int mid = low + (high - low) / 2;
            
            // Keep track of the minimum element seen so far explicitly
            result = Math.min(result, nums[mid]);

            if (nums[mid] < nums[high]) {
                // Minimum is in the left half, so discard the right half
                high = mid - 1;
            } else if (nums[mid] > nums[high]) {
                // Minimum is in the right half, discard the left half
                low = mid + 1;
            } else {
                // Duplicates found (nums[mid] == nums[high]).
                // We aren't sure which half holds the minimum, but since we 
                // have a duplicate at mid, dropping `high` is entirely safe.
                high--;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search
     * 
     * Time Complexity: O(log N) average, O(N) worst case.
     * Space Complexity: O(log N) average, O(N) worst case - Recursive call stack overhead.
     * 
     * EXPLANATION:
     * Translates the iterative binary search logic into a recursive function.
     * The `result` is explicitly passed and updated through the recursive calls.
     */
    public static int findMinRecursiveWrapper(int[] nums) {
        return findMinRecursive(nums, 0, nums.length - 1, nums[0]);
    }

    private static int findMinRecursive(int[] nums, int low, int high, int currentResult) {
        if (low > high) {
            return currentResult; // Base case: search space exhausted
        }

        int mid = low + (high - low) / 2;
        int result = Math.min(currentResult, nums[mid]); // Explicitly track minimum

        if (nums[mid] < nums[high]) {
            result = findMinRecursive(nums, low, mid - 1, result);
        } else if (nums[mid] > nums[high]) {
            result = findMinRecursive(nums, mid + 1, high, result);
        } else {
            result = findMinRecursive(nums, low, high - 1, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Linear Search (Simple Array Traversal)
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * A straightforward traversal of the array to find the minimum element.
     * While this ignores the binary search optimization, it is highly robust and 
     * actually matches the worst-case time complexity of the optimal solution.
     */
    public static int findMinLinear(int[] nums) {
        int min = nums[0];
        for (int num : nums) {
            if (num < min) {
                min = num;
            }
        }
        return min;
    }

    /**
     * SOLUTION 4: Linear Search using Java Streams
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Demonstrates modern Java functional paradigms using Streams. 
     * It relies on built-in reductions to elegantly find the minimum value.
     */
    public static int findMinStream(int[] nums) {
        return Arrays.stream(nums)
                .min()
                .orElseThrow(() -> new IllegalArgumentException("Array cannot be empty"));
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * A Java Record that neatly groups an input array and its expected output.
     */
    public record TestCase(int[] nums, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases reflecting edge cases, duplicates, and standard rotations
        TestCase[] testCases = {
            new TestCase(new int[]{5, 7, 11, 0, 2, 3, 3}, 0), // Standard rotation with duplicates
            new TestCase(new int[]{3, 3, 3, 1, 3}, 1),        // Duplicate heavy, min near end
            new TestCase(new int[]{3, 1, 3, 3, 3}, 1),        // Duplicate heavy, min near start
            new TestCase(new int[]{2, 2, 2, 2, 2}, 2),        // All elements are the same
            new TestCase(new int[]{1, 2, 3, 4, 5}, 1),        // Array is not rotated
            new TestCase(new int[]{5, 1, 2, 3, 4}, 1),        // Rotated exactly once
            new TestCase(new int[]{1}, 1)                     // Single element array
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterative = findMinIterative(tc.nums());
            int resRecursive = findMinRecursiveWrapper(tc.nums());
            int resLinear    = findMinLinear(tc.nums());
            int resStream    = findMinStream(tc.nums());

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resLinear == tc.expected()) &&
                             (resStream == tc.expected());

            // Limit array printing length for neat terminal output
            String arrStr = Arrays.toString(tc.nums());
            if (arrStr.length() > 30) arrStr = arrStr.substring(0, 27) + "...]";

            System.out.printf("Test %d | Array: %-30s -> Expected: %-2d | Passed: %b%n",
                    i + 1, arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iterative: %d, Recursive: %d, Linear: %d, Stream: %d%n",
                        resIterative, resRecursive, resLinear, resStream);
            }
        }
    }
}

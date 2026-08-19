import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Problem Statement:
 * Given a sorted array of distinct integers `nums`, and an integer `target`.
 * Return the index of target if it exists in the array.
 * If the target is not present, return the index where it should be inserted 
 * to maintain the sorted order.
 * 
 * Constraint: Algorithm must run in O(log n) time.
 */
class SearchInsertPosition {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION:
     * We are essentially finding the "Lower Bound" - the first index where 
     * nums[i] >= target. If no such element exists, it belongs at the very end.
     * 
     * Array: [1, 3, 5, 6], Target: 2
     * 
     * Initial State:
     * [ 1,  3,  5,  6 ]    result = 4 (default, if target > all elements)
     *   L       M       H
     * mid = 1, nums[mid] = 3. 
     * Since 3 >= target(2), 3 is a potential insert spot. 
     * Update result = 1, and search left (high = mid - 1).
     * 
     * Iteration 2:
     * [ 1,  3,  5,  6 ]    result = 1
     *  L/H/M
     * mid = 0, nums[mid] = 1.
     * Since 1 < target(2), it must be inserted after 1.
     * Update L = mid + 1. (L becomes 1, L > H, loop terminates).
     * 
     * Final Result: 1
     */
    public static int searchInsertIterative(int[] nums, int target) {
        int low = 0;
        int high = nums.length - 1;
        
        // Default insert position is at the very end of the array
        int result = nums.length; 

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] >= target) {
                result = mid;   // Potential answer found, store in explicit variable
                high = mid - 1; // Keep looking on the left for an even smaller valid index
            } else {
                low = mid + 1;  // Target is strictly greater, must be inserted further right
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search (O(log N))
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(log N) - Recursive call stack overhead.
     * 
     * EXPLANATION:
     * Translates the iterative logic into a recursive function, tracking the 
     * `result` variable explicitly through the method returns.
     */
    public static int searchInsertRecursiveWrapper(int[] nums, int target) {
        return searchInsertRecursive(nums, target, 0, nums.length - 1, nums.length);
    }

    private static int searchInsertRecursive(int[] nums, int target, int low, int high, int currentResult) {
        int result = currentResult; // Explicit result variable

        if (low > high) {
            return result; // Base case: search space exhausted
        }

        int mid = low + (high - low) / 2;

        if (nums[mid] >= target) {
            // Update result and search left
            result = searchInsertRecursive(nums, target, low, mid - 1, mid);
        } else {
            // Search right, keeping the currently known result
            result = searchInsertRecursive(nums, target, mid + 1, high, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Built-in Java Arrays.binarySearch
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Arrays.binarySearch(array, key) returns:
     * - The index of the key if found (>= 0).
     * - (-(insertion point) - 1) if not found.
     * We can mathematically reverse this to easily find the exact insertion point.
     */
    public static int searchInsertBuiltIn(int[] nums, int target) {
        int result = Arrays.binarySearch(nums, target);
        
        if (result < 0) {
            // Reverse the negative formula to get the exact insertion index
            result = -(result + 1);
        }
        
        return result;
    }

    /**
     * SOLUTION 4: Linear Search with Java Streams (Sub-optimal for sorted arrays)
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1) (excluding Stream overhead)
     * 
     * EXPLANATION:
     * We iterate through the array and find the first index where the element 
     * is greater than or equal to the target. If no such element exists, 
     * it belongs at `nums.length`.
     * Note: This does not satisfy the O(log N) constraint but is provided for completeness.
     */
    public static int searchInsertStream(int[] nums, int target) {
        int result = IntStream.range(0, nums.length)
                .filter(i -> nums[i] >= target)
                .findFirst()
                .orElse(nums.length); // Default to inserting at the end
                
        return result;
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to elegantly map input arrays, targets, and expected outputs.
     */
    public record TestCase(int[] nums, int target, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on problem description and edge cases
        TestCase[] testCases = {
            new TestCase(new int[]{1, 3, 5, 6}, 5, 2),   // Target exists in array
            new TestCase(new int[]{1, 3, 5, 6}, 2, 1),   // Target belongs in the middle
            new TestCase(new int[]{1, 3, 5, 6}, 7, 4),   // Target is larger than all elements
            new TestCase(new int[]{1, 3, 5, 6}, 0, 0),   // Target is smaller than all elements
            new TestCase(new int[]{1}, 0, 0),            // Single element, insert before
            new TestCase(new int[]{1}, 2, 1)             // Single element, insert after
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterative = searchInsertIterative(tc.nums(), tc.target());
            int resRecursive = searchInsertRecursiveWrapper(tc.nums(), tc.target());
            int resBuiltIn   = searchInsertBuiltIn(tc.nums(), tc.target());
            int resStream    = searchInsertStream(tc.nums(), tc.target());

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resBuiltIn == tc.expected()) &&
                             (resStream == tc.expected());

            System.out.printf("Test %d | Target: %d | Array: %-15s -> Expected: %d | Passed: %b%n",
                    i + 1, tc.target(), Arrays.toString(tc.nums()), tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iter: %d, Rec: %d, BuiltIn: %d, Stream: %d%n",
                        resIterative, resRecursive, resBuiltIn, resStream);
            }
        }
    }
}

class SearchInsertPosition2 {

    // =========================================================
    // 🔹 APPROACH 1: Classic Binary Search + Track Answer
    // =========================================================
    /**
     * Intuition:
     * ----------
     * We try to find the target.
     * If found → return index immediately.
     *
     * If not found:
     * - Whenever we see nums[mid] > target,
     *   that mid could be a valid insertion position.
     *
     * So we store it in 'answer' and keep searching left
     * to find a smaller valid position.
     *
     * Think:
     * 👉 "Find the smallest index where nums[i] >= target"
     *
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public static int searchInsertApproach1(int[] nums, int target) {

        int low = 0;
        int high = nums.length - 1;

        // Default: insert at end
        int answer = nums.length;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                // Exact match found
                answer = mid;
                return answer;
            }
            else if (nums[mid] < target) {
                // Target is on right side
                low = mid + 1;
            }
            else {
                // nums[mid] > target → possible insert position
                answer = mid;

                // But maybe there is a better (smaller index) on left
                high = mid - 1;
            }
        }

        return answer;
    }


    // =========================================================
    // 🔹 APPROACH 2: Pure Lower Bound Style (More Generic)
    // =========================================================
    /**
     * Intuition:
     * ----------
     * We DON'T return immediately when we find target.
     *
     * Instead, we continue searching LEFT to ensure we find:
     * 👉 the FIRST position where nums[i] >= target
     *
     * This is called LOWER BOUND.
     *
     * Why useful?
     * - Works even with duplicates
     * - Reusable template for many problems
     *
     * Key Idea:
     * ----------
     * - If nums[mid] >= target → move LEFT
     * - Else → move RIGHT
     *
     * Always maintain answer = possible position
     *
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public static int searchInsertApproach2(int[] nums, int target) {

        int low = 0;
        int high = nums.length - 1;

        // Default: insert at end
        int answer = nums.length;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                // Found target, but keep searching LEFT
                // to maintain lower bound behavior
                answer = mid;
                high = mid - 1;
            }
            else if (target < nums[mid]) {
                // nums[mid] is a valid insert position
                answer = mid;
                high = mid - 1;
            }
            else {
                // target > nums[mid]
                low = mid + 1;
            }
        }

        return answer;
    }
}

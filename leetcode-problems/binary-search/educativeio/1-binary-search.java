import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Problem Statement:
 * Given an array of integers `nums`, sorted in ascending order, and an integer `target`.
 * If the target exists in the array, return its index. If it does not exist, return -1.
 * 
 * Constraints:
 * - 1 <= nums.length <= 10^3
 * - -10^4 <= nums[i], target <= 10^4
 * - All integers in nums are unique.
 * - nums is sorted in ascending order.
 */
class BinarySearchSolutions {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(log N) - Search space is halved every iteration.
     * Space Complexity: O(1) - Only a few pointers are used.
     * 
     * VISUAL EXPLANATION:
     * Array: [-1, 0, 3, 5, 9, 12], Target: 9
     * 
     * Iteration 1:
     * [ -1,  0,  3,  5,  9, 12 ]
     *    L       M           H
     * mid = 2, nums[mid] = 3. Target (9) > 3, so move L to M + 1.
     * 
     * Iteration 2:
     * [ -1,  0,  3,  5,  9, 12 ]
     *                L   M   H
     * mid = 4, nums[mid] = 9. Target (9) == 9. Match found!
     */
    public static int searchIterative(int[] nums, int target) {
        int low = 0;
        int high = nums.length - 1;
        int result = -1; // Explicit result variable as requested

        while (low <= high) {
            // Avoids integer overflow compared to (low + high) / 2
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                result = mid; // Store the answer
                break;        // Exit the loop since elements are unique
            } else if (nums[mid] < target) {
                low = mid + 1; // Discard left half
            } else {
                high = mid - 1; // Discard right half
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search (Optimal, but uses call stack)
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(log N) - Due to recursive call stack overhead.
     * 
     * EXPLANATION:
     * Similar to the iterative approach, but uses method arguments to update 
     * the search boundaries instead of a while loop.
     */
    public static int searchRecursiveWrapper(int[] nums, int target) {
        return searchRecursive(nums, target, 0, nums.length - 1);
    }

    private static int searchRecursive(int[] nums, int target, int low, int high) {
        int result = -1; // Explicit result variable

        // Base case: search space is exhausted
        if (low > high) {
            return result; 
        }

        int mid = low + (high - low) / 2;

        if (nums[mid] == target) {
            result = mid;
        } else if (nums[mid] < target) {
            // Search in the right half
            result = searchRecursive(nums, target, mid + 1, high);
        } else {
            // Search in the left half
            result = searchRecursive(nums, target, low, mid - 1);
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
     * Java provides a highly optimized built-in binary search. 
     * Note: Arrays.binarySearch returns (-(insertion point) - 1) if the target is not found.
     * We convert any negative return value to -1 to match the problem statement.
     */
    public static int searchBuiltIn(int[] nums, int target) {
        int result = Arrays.binarySearch(nums, target);
        return result >= 0 ? result : -1;
    }

    /**
     * SOLUTION 4: Linear Search using Java Streams (Sub-optimal for sorted arrays)
     * 
     * Time Complexity: O(N) - Checks every element one by one.
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Included to demonstrate modern Java features (IntStream). 
     * While this works, it ignores the "sorted" property of the array 
     * and is strictly worse than Binary Search for this problem.
     */
    public static int searchLinearStream(int[] nums, int target) {
        return IntStream.range(0, nums.length)
                .filter(i -> nums[i] == target)
                .findFirst() // returns an OptionalInt
                .orElse(-1); // Explicitly return -1 if empty
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * A Java Record (introduced in Java 14) to neatly hold our test cases.
     * It automatically creates a constructor, getters, equals(), hashCode(), and toString().
     */
    public record TestCase(int[] nums, int target, int expected) {}

    public static void main(String[] args) {
        // Define test cases
        TestCase[] testCases = {
            new TestCase(new int[]{-1, 0, 3, 5, 9, 12}, 9, 4),    // Target exists in right half
            new TestCase(new int[]{-1, 0, 3, 5, 9, 12}, 2, -1),   // Target does not exist
            new TestCase(new int[]{5}, 5, 0),                     // Single element array, target exists
            new TestCase(new int[]{5}, -5, -1),                   // Single element array, target missing
            new TestCase(new int[]{-10000, 10000}, -10000, 0)     // Boundary values
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterative = searchIterative(tc.nums(), tc.target());
            int resRecursive = searchRecursiveWrapper(tc.nums(), tc.target());
            int resBuiltIn = searchBuiltIn(tc.nums(), tc.target());
            int resLinear = searchLinearStream(tc.nums(), tc.target());

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resBuiltIn == tc.expected()) &&
                             (resLinear == tc.expected());

            System.out.printf("Test Case %d: Target %5d in %-25s -> Expected: %2d | Passed: %b%n",
                    i + 1, tc.target(), Arrays.toString(tc.nums()), tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iterative: %d, Recursive: %d, BuiltIn: %d, Linear: %d%n",
                        resIterative, resRecursive, resBuiltIn, resLinear);
            }
        }
    }
}


class BinarySearchSolution {

    /**
     * Classic Binary Search
     *
     * Idea:
     * - Maintain search space [low, high]
     * - Find mid
     * - Eliminate half each time
     *
     * Easy way to think:
     * "Where could the target possibly be?"
     * Reduce possibilities step by step.
     */
    public static int search(int[] nums, int target) {

        int low = 0;
        int high = nums.length - 1;

        // explicit result variable (as per good practice)
        int answer = -1;

        while (low <= high) {

            // Avoid overflow: (low + high) / 2 ❌
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                answer = mid;
                return answer; // found
            }

            // Target lies on left side
            if (target < nums[mid]) {
                high = mid - 1;
            }
            // Target lies on right side
            else {
                low = mid + 1;
            }
        }

        return answer; // -1 if not found
    }

    public static void main(String[] args) {
        int[] nums = {-10, -3, 0, 5, 9, 12};
        int target = 9;

        System.out.println(search(nums, target)); // Output: 4
    }
}

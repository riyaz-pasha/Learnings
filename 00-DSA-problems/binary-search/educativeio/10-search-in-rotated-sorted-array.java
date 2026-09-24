import java.util.*;

class SearchInRotatedSortedArray {

    public static void main(String[] args) {
        int[] nums = {4,5,6,7,0,1,2};
        int target = 0;

        System.out.println(search(nums, target)); // Expected: 4
    }

    /**
     * ============================================================
     * 🔥 SEARCH IN ROTATED SORTED ARRAY (BINARY SEARCH)
     * ============================================================
     *
     * Core Idea:
     * - Array is rotated, but one half is always sorted
     * - Use this property to discard half of the search space
     *
     * Time Complexity:  O(log n)
     * Space Complexity: O(1)
     */
    public static int search(int[] nums, int target) {

        int low = 0;
        int high = nums.length - 1;

        int answerIndex = -1; // Explicit result variable (your preferred style)

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // 🎯 Found target
            if (nums[mid] == target) {
                answerIndex = mid;
                break;
            }

            /**
             * --------------------------------------------------------
             * 🧠 Step 1: Identify which half is sorted
             * --------------------------------------------------------
             */

            // ✅ LEFT HALF SORTED
            if (nums[low] <= nums[mid]) {

                /**
                 * ----------------------------------------------------
                 * 🧠 Step 2: Check if target lies in LEFT sorted half
                 * ----------------------------------------------------
                 */
                if (nums[low] <= target && target < nums[mid]) {
                    // 🎯 Target is in left half → discard right
                    high = mid - 1;
                } else {
                    // ❌ Target not in left → go right
                    low = mid + 1;
                }
            }
            // ✅ RIGHT HALF SORTED
            else {

                /**
                 * ----------------------------------------------------
                 * 🧠 Step 2: Check if target lies in RIGHT sorted half
                 * ----------------------------------------------------
                 */
                if (nums[mid] < target && target <= nums[high]) {
                    // 🎯 Target is in right half → discard left
                    low = mid + 1;
                } else {
                    // ❌ Target not in right → go left
                    high = mid - 1;
                }
            }
        }

        return answerIndex;
    }
}

/**
 * Problem Statement:
 * Given a sorted integer array `nums` that has been rotated by an arbitrary number of positions,
 * and an integer `target`, return the index of `target` in the array. 
 * If `target` does not exist, return -1.
 * 
 * Constraints:
 * - All values in nums are unique.
 * - 1 <= nums.length <= 1000
 * - -10^4 <= nums[i], target <= 10^4
 * - Expected time complexity is O(log N).
 */
class SearchRotatedSortedArray {

    /**
     * SOLUTION 1: One-Pass Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION:
     * When you divide a rotated sorted array into two halves using a `mid` index, 
     * at least ONE of those two halves will ALWAYS be strictly sorted.
     * 
     * Array: [ 176, 188, 199, 200, 210, 222, 1, 10, 20, 47 ] 
     * Target: 20
     * 
     * Iteration 1:
     * L = 0, H = 9, Mid = 4.  nums[mid] = 210.
     * Is the Left Half [176 to 210] sorted? YES (nums[L] <= nums[mid]).
     * Does the target (20) fall in this sorted Left Half (between 176 and 210)? NO.
     * So, the target MUST be in the Right Half. Set L = mid + 1 = 5.
     * 
     * Iteration 2:
     * L = 5, H = 9, Mid = 7. nums[mid] = 10.
     * Is the Left Half [222 to 10] sorted? NO.
     * That means the Right Half [10 to 47] MUST be sorted.
     * Does the target (20) fall in this sorted Right Half (between 10 and 47)? YES.
     * So, set L = mid + 1 = 8.
     * 
     * Iteration 3:
     * L = 8, H = 9, Mid = 8. nums[mid] = 20.
     * Target matches nums[mid]. Result is found!
     */
    public static int searchOnePassIterative(int[] nums, int target) {
        int low = 0;
        int high = nums.length - 1;
        int result = -1; // Explicit result variable as requested

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                result = mid; // Store answer
                break;        // Exit loop
            }

            // Check if the left half is strictly sorted
            if (nums[low] <= nums[mid]) {
                // If target is within the bounds of the sorted left half
                if (target >= nums[low] && target < nums[mid]) {
                    high = mid - 1; // Discard right half
                } else {
                    low = mid + 1;  // Discard left half
                }
            } 
            // If the left half isn't sorted, the right half MUST be sorted
            else {
                // If target is within the bounds of the sorted right half
                if (target > nums[mid] && target <= nums[high]) {
                    low = mid + 1;  // Discard left half
                } else {
                    high = mid - 1; // Discard right half
                }
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Two-Pass Binary Search (Optimal)
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Phase 1: Use Binary Search to find the index of the smallest element (the "pivot").
     * Phase 2: Once the pivot is found, the array is logically split into two fully sorted sub-arrays.
     *          We check which sub-array the target belongs to and perform a standard Binary Search there.
     */
    public static int searchTwoPass(int[] nums, int target) {
        if (nums.length == 0) return -1;
        
        // --- Phase 1: Find Pivot (Index of smallest element) ---
        int low = 0;
        int high = nums.length - 1;
        int pivot = 0; // Default assuming array is not rotated
        
        while (low <= high) {
            int mid = low + (high - low) / 2;
            
            // If mid element is less than the first element, the pivot is to the left (or is mid)
            if (nums[mid] < nums[0]) {
                pivot = mid;
                high = mid - 1;
            } else {
                // Otherwise, pivot is to the right
                low = mid + 1;
            }
        }
        
        // --- Phase 2: Binary Search in the correct sorted half ---
        if (pivot == 0) {
            // Array was not rotated
            return binarySearch(nums, target, 0, nums.length - 1);
        } else if (target >= nums[0] && target <= nums[pivot - 1]) {
            // Target is in the left rotated portion
            return binarySearch(nums, target, 0, pivot - 1);
        } else {
            // Target is in the right rotated portion
            return binarySearch(nums, target, pivot, nums.length - 1);
        }
    }

    // Helper for Solution 2 using the explicit result format
    private static int binarySearch(int[] nums, int target, int low, int high) {
        int result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] == target) {
                result = mid;
                break;
            } else if (nums[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    /**
     * SOLUTION 3: Recursive Binary Search (O(log N))
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(log N) - Recursive call stack overhead.
     * 
     * EXPLANATION:
     * Converts the One-Pass logic into a recursive function, tracking the `result` 
     * explicitly through the method parameters.
     */
    public static int searchRecursiveWrapper(int[] nums, int target) {
        return searchRecursive(nums, target, 0, nums.length - 1, -1);
    }

    private static int searchRecursive(int[] nums, int target, int low, int high, int currentResult) {
        int result = currentResult;

        if (low > high) {
            return result; // Base case: boundaries crossed
        }

        int mid = low + (high - low) / 2;

        if (nums[mid] == target) {
            return mid; // Return immediately upon finding
        }

        if (nums[low] <= nums[mid]) {
            // Left half is sorted
            if (target >= nums[low] && target < nums[mid]) {
                result = searchRecursive(nums, target, low, mid - 1, result);
            } else {
                result = searchRecursive(nums, target, mid + 1, high, result);
            }
        } else {
            // Right half is sorted
            if (target > nums[mid] && target <= nums[high]) {
                result = searchRecursive(nums, target, mid + 1, high, result);
            } else {
                result = searchRecursive(nums, target, low, mid - 1, result);
            }
        }

        return result;
    }

    /**
     * SOLUTION 4: Linear Search using Java Streams (Sub-optimal for sorted/rotated arrays)
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Demonstrates modern Java functional paradigms. While this ignores the O(log N) 
     * time complexity requirement, it is highly concise and robust.
     */
    public static int searchStream(int[] nums, int target) {
        return IntStream.range(0, nums.length)
                .filter(i -> nums[i] == target)
                .findFirst()
                .orElse(-1);
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record introduced to elegantly hold our test parameters.
     */
    public record TestCase(int[] nums, int target, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on constraints and rotations
        TestCase[] testCases = {
            new TestCase(new int[]{176, 188, 199, 200, 210, 222, 1, 10, 20, 47, 59, 63, 75, 88, 99, 107, 120, 133, 155, 162}, 20, 8),
            new TestCase(new int[]{4, 5, 6, 7, 0, 1, 2}, 0, 4), // Target in right portion
            new TestCase(new int[]{4, 5, 6, 7, 0, 1, 2}, 3, -1), // Target does not exist
            new TestCase(new int[]{1}, 0, -1),                   // Single element, missing
            new TestCase(new int[]{1}, 1, 0),                    // Single element, found
            new TestCase(new int[]{5, 1, 3}, 5, 0),              // Target is the first element
            new TestCase(new int[]{1, 2, 3, 4, 5, 6}, 4, 3)      // Not rotated at all
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resOnePass   = searchOnePassIterative(tc.nums(), tc.target());
            int resTwoPass   = searchTwoPass(tc.nums(), tc.target());
            int resRecursive = searchRecursiveWrapper(tc.nums(), tc.target());
            int resStream    = searchStream(tc.nums(), tc.target());

            boolean passed = (resOnePass == tc.expected()) &&
                             (resTwoPass == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resStream == tc.expected());

            // Limit array printing length for neatness
            String arrStr = Arrays.toString(tc.nums());
            if (arrStr.length() > 30) arrStr = arrStr.substring(0, 27) + "...]";

            System.out.printf("Test %d | Target: %-4d | Array: %-30s -> Expected: %-2d | Passed: %b%n",
                    i + 1, tc.target(), arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] OnePass: %d, TwoPass: %d, Recursive: %d, Stream: %d%n",
                        resOnePass, resTwoPass, resRecursive, resStream);
            }
        }
    }
}

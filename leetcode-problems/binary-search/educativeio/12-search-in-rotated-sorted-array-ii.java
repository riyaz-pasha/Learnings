import java.util.*;

class SearchInRotatedArrayWithDuplicates {

    /**
     * 🔥 Problem:
     * Search target in rotated sorted array with duplicates
     *
     * ---------------------------------------------------------
     * 🧠 Core Observations:
     *
     * 1. Normally, one half is sorted:
     *      if (nums[low] <= nums[mid]) → left is sorted
     *      else → right is sorted
     *
     * 2. BUT duplicates break this:
     *      nums[low] == nums[mid] == nums[high]
     *      → cannot determine sorted half
     *
     * 3. Solution:
     *      shrink search space safely:
     *      low++, high--
     *
     * ---------------------------------------------------------
     * ⏱ Time Complexity:
     *   Best / Average → O(log n)
     *   Worst (all duplicates) → O(n)
     *
     * 📦 Space Complexity:
     *   O(1)
     */
    public static boolean search(int[] nums, int target) {

        int low = 0;
        int high = nums.length - 1;

        boolean answer = false; // Explicit result tracking

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // 🎯 Found target
            if (nums[mid] == target) {
                answer = true;
                break;
            }

            /**
             * ❗ Ambiguous case due to duplicates
             *
             * Example:
             * [2, 2, 2, 3, 2]
             *
             * low = mid = high → cannot decide sorted side
             */
            if (nums[low] == nums[mid] && nums[mid] == nums[high]) {
                low++;
                high--;
                continue;
            }

            /**
             * ✅ LEFT HALF IS SORTED
             */
            if (nums[low] <= nums[mid]) {

                // Check if target lies in left sorted portion
                if (nums[low] <= target && target < nums[mid]) {
                    high = mid - 1;
                } else {
                    low = mid + 1;
                }
            }

            /**
             * ✅ RIGHT HALF IS SORTED
             */
            else {

                // Check if target lies in right sorted portion
                if (nums[mid] < target && target <= nums[high]) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
        }

        return answer;
    }

    public static void main(String[] args) {

        int[] arr = {47, 78, 90, 901, 10, 30, 40, 42, 42};

        System.out.println(search(arr, 42)); // true
        System.out.println(search(arr, 100)); // false
    }
}

/**
 * Problem Statement:
 * You are required to find an integer value `t` in an array `arr` of non-distinct integers.
 * The array was sorted in non-descending order, then rotated around some unknown pivot.
 * Return TRUE if `t` exists in the array, and FALSE otherwise.
 * Minimize the number of operations.
 * 
 * Constraints:
 * - 1 <= arr.length <= 1000
 * - -10^4 <= arr[i], t <= 10^4
 * - arr may contain duplicates.
 */
class SearchRotatedSortedArrayDuplicates {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal Average Case)
     * 
     * Time Complexity: O(log N) average, O(N) worst case (if all elements are duplicates).
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * When duplicates are present, dividing the array might give us identical values 
     * at `low`, `mid`, and `high`. 
     * 
     * Example of the problem:
     * Array A: [3, 1, 2, 3, 3, 3, 3] -> arr[low] = 3, arr[mid] = 3, arr[high] = 3
     * Array B: [3, 3, 3, 3, 1, 2, 3] -> arr[low] = 3, arr[mid] = 3, arr[high] = 3
     * In both cases, mid is 3. But in A, the unsorted part is on the left. In B, it's on the right.
     * We cannot decide which way to go!
     * 
     * The Fix:
     * If arr[low] == arr[mid] == arr[high], we simply shrink the search space from both ends 
     * (low++, high--) because we know arr[low] and arr[high] are NOT the target (checked prior), 
     * so it's safe to discard them.
     * 
     * If they are not all equal, at least one half of the array will be strictly sorted, 
     * allowing standard binary search logic to proceed.
     */
    public static boolean searchIterative(int[] arr, int t) {
        int low = 0;
        int high = arr.length - 1;
        
        // Explicit result variable as requested
        boolean result = false; 

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] == t) {
                result = true; // Match found
                break;         // Exit loop immediately
            }

            // CRITICAL STEP FOR DUPLICATES: 
            // If we can't determine which half is sorted, shrink the window.
            if (arr[low] == arr[mid] && arr[mid] == arr[high]) {
                low++;
                high--;
            } 
            // Check if the left half is sorted
            else if (arr[low] <= arr[mid]) {
                // If target falls strictly within the sorted left half
                if (t >= arr[low] && t < arr[mid]) {
                    high = mid - 1; // Discard right half
                } else {
                    low = mid + 1;  // Discard left half
                }
            } 
            // Otherwise, the right half MUST be sorted
            else {
                // If target falls strictly within the sorted right half
                if (t > arr[mid] && t <= arr[high]) {
                    low = mid + 1;  // Discard left half
                } else {
                    high = mid - 1; // Discard right half
                }
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search
     * 
     * Time Complexity: O(log N) average, O(N) worst case.
     * Space Complexity: O(log N) average, O(N) worst case - Recursive call stack.
     * 
     * EXPLANATION:
     * Translates the iterative binary search logic into a recursive function.
     * The boolean `result` is explicitly tracked and passed back through the returns.
     */
    public static boolean searchRecursiveWrapper(int[] arr, int t) {
        return searchRecursive(arr, t, 0, arr.length - 1, false);
    }

    private static boolean searchRecursive(int[] arr, int t, int low, int high, boolean currentResult) {
        boolean result = currentResult;

        if (low > high) {
            return result; // Base case: boundaries crossed
        }

        int mid = low + (high - low) / 2;

        if (arr[mid] == t) {
            return true; // Return true immediately up the call stack
        }

        // Handle duplicates
        if (arr[low] == arr[mid] && arr[mid] == arr[high]) {
            result = searchRecursive(arr, t, low + 1, high - 1, result);
        } 
        // Left half is sorted
        else if (arr[low] <= arr[mid]) {
            if (t >= arr[low] && t < arr[mid]) {
                result = searchRecursive(arr, t, low, mid - 1, result);
            } else {
                result = searchRecursive(arr, t, mid + 1, high, result);
            }
        } 
        // Right half is sorted
        else {
            if (t > arr[mid] && t <= arr[high]) {
                result = searchRecursive(arr, t, mid + 1, high, result);
            } else {
                result = searchRecursive(arr, t, low, mid - 1, result);
            }
        }

        return result;
    }

    /**
     * SOLUTION 3: Linear Search using Java Streams
     * 
     * Time Complexity: O(N) strict
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Demonstrates modern Java functional paradigms. 
     * While this ignores the binary search optimization, it handles the worst-case O(N) 
     * scenario identical to Binary Search but with highly concise syntax.
     */
    public static boolean searchStream(int[] arr, int t) {
        return Arrays.stream(arr).anyMatch(num -> num == t);
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * A Java Record that neatly groups an input array, target, and expected output.
     */
    public record TestCase(int[] arr, int t, boolean expected) {}

    public static void main(String[] args) {
        // Defined Test Cases reflecting edge cases, duplicates, and standard rotations
        TestCase[] testCases = {
            new TestCase(new int[]{47, 78, 90, 901, 10, 30, 40, 42, 42}, 42, true), // Standard rotation
            new TestCase(new int[]{2, 5, 6, 0, 0, 1, 2}, 0, true),                  // Exists in right half
            new TestCase(new int[]{2, 5, 6, 0, 0, 1, 2}, 3, false),                 // Does not exist
            new TestCase(new int[]{1, 0, 1, 1, 1}, 0, true),                        // Duplicate heavy, unsorted left
            new TestCase(new int[]{1, 1, 1, 0, 1}, 0, true),                        // Duplicate heavy, unsorted right
            new TestCase(new int[]{2, 2, 2, 2, 2}, 2, true),                        // All elements are the same
            new TestCase(new int[]{2, 2, 2, 2, 2}, 3, false),                       // All elements same, target missing
            new TestCase(new int[]{1}, 1, true),                                    // Single element
            new TestCase(new int[]{1}, 0, false)                                    // Single element, missing
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            boolean resIterative = searchIterative(tc.arr(), tc.t());
            boolean resRecursive = searchRecursiveWrapper(tc.arr(), tc.t());
            boolean resStream    = searchStream(tc.arr(), tc.t());

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resStream == tc.expected());

            // Limit array printing length for neat terminal output
            String arrStr = Arrays.toString(tc.arr());
            if (arrStr.length() > 30) arrStr = arrStr.substring(0, 27) + "...]";

            System.out.printf("Test %d | Target: %-4d | Array: %-30s -> Expected: %-5b | Passed: %b%n",
                    i + 1, tc.t(), arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iterative: %b, Recursive: %b, Stream: %b%n",
                        resIterative, resRecursive, resStream);
            }
        }
    }
}

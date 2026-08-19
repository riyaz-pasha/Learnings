import java.util.*;

/**
 * ============================================================
 * 🔥 FIND PEAK ELEMENT — BINARY SEARCH (INTERVIEW TEMPLATE)
 * ============================================================
 *
 * Idea:
 * -----
 * We don't search for the peak directly.
 * Instead, we check slope direction:
 *
 * If nums[mid] > nums[mid + 1]:
 *      ↓ descending slope → peak is on LEFT (including mid)
 *
 * If nums[mid] < nums[mid + 1]:
 *      ↑ ascending slope → peak is on RIGHT
 *
 * WHY THIS WORKS?
 * --------------
 * Because array ends with -∞ on both sides,
 * a peak MUST exist.
 *
 * ============================================================
 */
class FindPeakElement {

    public static int findPeakElement(int[] nums) {

        int low = 0;
        int high = nums.length - 1;

        int answerIndex = -1; // stores potential peak

        while (low <= high) {

            int mid = low + (high - low) / 2;

            /**
             * Handle boundary safely:
             * Treat nums[n] as -∞
             */
            boolean isDescending = (mid == nums.length - 1) || (nums[mid] > nums[mid + 1]);

            if (isDescending) {
                /**
                 * We are on descending slope:
                 * Peak exists on LEFT (including mid)
                 */
                answerIndex = mid;   // possible peak
                high = mid - 1;      // try to find earlier peak
            } else {
                /**
                 * Ascending slope:
                 * Peak lies on RIGHT
                 */
                low = mid + 1;
            }
        }

        return answerIndex;
    }

    public static void main(String[] args) {
        int[] nums = {1, 2, 3, 1};

        System.out.println(findPeakElement(nums)); // Output: 2
    }
}

/**
 * Problem Statement:
 * Given a 0-indexed integer array `nums`, find a peak element and return its index.
 * A peak element is an element that is strictly greater than its neighbors.
 * Virtual boundaries exist: nums[-1] = nums[n] = -∞.
 * If there are multiple peaks, returning the index of ANY peak is acceptable.
 * 
 * Target Complexity: O(log n)
 * 
 * Constraints:
 * - 1 <= nums.length <= 1000
 * - -2^31 <= nums[i] <= 2^31 - 1
 * - nums[i] != nums[i + 1] for all valid i.
 */
class FindPeakElement2 {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * How can we use Binary Search on an UNSORTED array? 
     * Because we only need to find a "local" maximum, we can follow the upward slope.
     * 
     * Imagine the array as a terrain profile:
     * nums: [1, 2, 1, 3, 5, 6, 4]
     * 
     * Let's pick a mid point. mid = 3, nums[mid] = 3.
     * Compare it to its right neighbor: nums[mid + 1] = 5.
     * Since 3 < 5, the terrain is SLOPING UPWARD to the right. 
     * Because the boundary on the far right is -∞, if we keep going up, we MUST 
     * eventually hit a peak (either inside the array or at the very last element).
     * So, we discard the left half and move right: low = mid + 1.
     * 
     * If the terrain was SLOPING DOWNWARD (nums[mid] > nums[mid + 1]), 
     * then a peak must exist to the left (or mid itself is the peak).
     * 
     * By explicitly evaluating both neighbors, we can accurately lock in the `result`.
     */
    public static int findPeakIterative(int[] nums) {
        int n = nums.length;
        if (n == 1) return 0; // Single element is always a peak
        
        int low = 0;
        int high = n - 1;
        int result = -1; // Explicit result variable as requested

        while (low <= high) {
            int mid = low + (high - low) / 2;

            // Check if mid is strictly greater than its left and right neighbors (if they exist)
            boolean leftSmaller = (mid == 0 || nums[mid] > nums[mid - 1]);
            boolean rightSmaller = (mid == n - 1 || nums[mid] > nums[mid + 1]);

            if (leftSmaller && rightSmaller) {
                // We found a peak!
                result = mid;
                break;
            } else if (!rightSmaller) {
                // Slope is rising to the right, so a peak MUST exist in the right half
                low = mid + 1;
            } else {
                // Slope is falling to the right (rising to the left), peak MUST exist in left half
                high = mid - 1;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search (O(log n))
     * 
     * Time Complexity: O(log n)
     * Space Complexity: O(log n) - Recursive call stack overhead.
     * 
     * EXPLANATION:
     * Translates the iterative binary search logic into a recursive function.
     * Passes the `result` explicitly down the call stack.
     */
    public static int findPeakRecursiveWrapper(int[] nums) {
        if (nums.length == 1) return 0;
        return findPeakRecursive(nums, 0, nums.length - 1, -1);
    }

    private static int findPeakRecursive(int[] nums, int low, int high, int currentResult) {
        int result = currentResult;

        if (low > high) {
            return result; // Base case
        }

        int n = nums.length;
        int mid = low + (high - low) / 2;

        boolean leftSmaller = (mid == 0 || nums[mid] > nums[mid - 1]);
        boolean rightSmaller = (mid == n - 1 || nums[mid] > nums[mid + 1]);

        if (leftSmaller && rightSmaller) {
            return mid; // Return immediately upon finding a peak
        } else if (!rightSmaller) {
            // Target is to the right
            result = findPeakRecursive(nums, mid + 1, high, result);
        } else {
            // Target is to the left
            result = findPeakRecursive(nums, low, mid - 1, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Linear Search (O(n))
     * 
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Since nums[-1] is -∞, the array starts by either going up or down.
     * We just scan from left to right. The first time the sequence drops 
     * (nums[i] > nums[i+1]), it means we just crested a hill. That index `i` is a peak.
     * If it never drops, the last element is the peak.
     */
    public static int findPeakLinear(int[] nums) {
        int result = nums.length - 1; // Default to the last element

        for (int i = 0; i < nums.length - 1; i++) {
            if (nums[i] > nums[i + 1]) {
                result = i;
                break;
            }
        }

        return result;
    }

    /**
     * SOLUTION 4: Linear Search using Java Streams
     * 
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Demonstrates modern Java functional paradigms using IntStream. 
     * Finds the first index where the element is greater than the next element.
     */
    public static int findPeakStream(int[] nums) {
        return IntStream.range(0, nums.length - 1)
                .filter(i -> nums[i] > nums[i + 1])
                .findFirst()
                .orElse(nums.length - 1); // If no drop is found, the last element is the peak
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * A Java Record that maps out the input array. 
     * Note: Because multiple peaks can exist and algorithms might find different ones, 
     * we don't hardcode an 'expected' index. Instead, we write a verifier method.
     */
    public record TestCase(int[] nums) {}

    /**
     * Verifier method to check if a returned index is a valid peak.
     */
    private static boolean isValidPeak(int[] nums, int index) {
        if (index < 0 || index >= nums.length) return false;
        boolean leftSmaller = (index == 0 || nums[index] > nums[index - 1]);
        boolean rightSmaller = (index == nums.length - 1 || nums[index] > nums[index + 1]);
        return leftSmaller && rightSmaller;
    }

    public static void main(String[] args) {
        // Defined Test Cases reflecting boundaries, multiple peaks, and slopes
        TestCase[] testCases = {
            new TestCase(new int[]{1, 2, 3, 1}),             // Single clear peak in middle
            new TestCase(new int[]{1, 2, 1, 3, 5, 6, 4}),    // Multiple peaks (index 1 and 5)
            new TestCase(new int[]{1, 2, 3, 4, 5}),          // Strictly increasing (last element is peak)
            new TestCase(new int[]{5, 4, 3, 2, 1}),          // Strictly decreasing (first element is peak)
            new TestCase(new int[]{1}),                      // Single element array
            new TestCase(new int[]{2, 1}),                   // Two elements, decreasing
            new TestCase(new int[]{1, 2})                    // Two elements, increasing
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            int[] arr = tc.nums();
            
            int resIterative = findPeakIterative(arr);
            int resRecursive = findPeakRecursiveWrapper(arr);
            int resLinear    = findPeakLinear(arr);
            int resStream    = findPeakStream(arr);

            boolean passed = isValidPeak(arr, resIterative) &&
                             isValidPeak(arr, resRecursive) &&
                             isValidPeak(arr, resLinear) &&
                             isValidPeak(arr, resStream);

            // Formatting output for neatness
            String arrStr = Arrays.toString(arr);
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | Array: %-25s | Iterative Peak Index: %-2d | Passed: %b%n",
                    i + 1, arrStr, resIterative, passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iter: %d (Valid: %b), Rec: %d, Lin: %d, Stream: %d%n",
                        resIterative, isValidPeak(arr, resIterative), resRecursive, resLinear, resStream);
            }
        }
    }
}

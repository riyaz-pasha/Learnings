import java.util.*;

/**
 * ============================================================
 * 🔥 Count Pairs with Sum < Target (Binary Search Approach)
 * ============================================================
 *
 * PROBLEM:
 * Count number of pairs (i, j) such that:
 *   i < j AND nums[i] + nums[j] < target
 *
 * ------------------------------------------------------------
 * 💡 CORE IDEA (MONOTONIC TRANSFORMATION)
 * ------------------------------------------------------------
 * After sorting:
 *   nums[i] is fixed
 *   we need to find max index j such that:
 *
 *      nums[i] + nums[j] < target
 *
 * Transform condition:
 *      nums[j] < target - nums[i]
 *
 * 👉 For fixed i:
 *     nums[j] is increasing (sorted array)
 *     So condition becomes:
 *
 *        F F F F T T T   (INVALID → VALID)
 *
 * ❗ Actually here:
 *     VALID = sum < target
 *     INVALID = sum >= target
 *
 * So pattern is:
 *        T T T T F F F   (VALID → INVALID)
 *
 * 👉 We want LAST TRUE (last valid index)
 *
 * ------------------------------------------------------------
 * 🎯 BINARY SEARCH GOAL
 * ------------------------------------------------------------
 * Find the LARGEST index j such that:
 *      nums[i] + nums[j] < target
 *
 * ------------------------------------------------------------
 * 🧠 INVARIANTS
 * ------------------------------------------------------------
 * answerIndex = stores LAST valid j
 *
 * During search:
 *   if condition TRUE → move RIGHT (try bigger j)
 *   if condition FALSE → move LEFT (reduce j)
 *
 * ------------------------------------------------------------
 * 📌 FINAL COUNT LOGIC
 * ------------------------------------------------------------
 * For each i:
 *   valid indices = (i+1 ... answerIndex)
 *   count = answerIndex - i
 *
 * ============================================================
 */

class CountPairsBinarySearch {

    public static int countPairs(int[] nums, int target) {

        // Step 1: Sort the array → enables binary search
        Arrays.sort(nums);

        int n = nums.length;
        int totalPairs = 0;

        // Step 2: Fix first index i
        for (int i = 0; i < n; i++) {

            int low = i + 1;     // j must be > i
            int high = n - 1;

            // Stores last valid index j such that sum < target
            int answerIndex = i; // default: no valid pair

            /**
             * Binary Search Pattern: LAST TRUE
             *
             * Condition:
             *   nums[i] + nums[mid] < target
             */
            while (low <= high) {

                int mid = low + (high - low) / 2;

                int sum = nums[i] + nums[mid];

                if (sum < target) {
                    /**
                     * ✅ VALID PAIR
                     *
                     * This mid works, but we want the RIGHTMOST valid j
                     * So:
                     *   - store answer
                     *   - move right
                     */
                    answerIndex = mid;
                    low = mid + 1;
                } else {
                    /**
                     * ❌ INVALID PAIR (sum >= target)
                     *
                     * Need smaller values → move left
                     */
                    high = mid - 1;
                }
            }

            /**
             * After Binary Search:
             * answerIndex = last valid j
             *
             * Valid j range:
             *   (i+1 ... answerIndex)
             *
             * Count = answerIndex - i
             */
            totalPairs += (answerIndex - i);
        }

        return totalPairs;
    }

    public static void main(String[] args) {

        int[] nums = {-1, 1, 2, 3, 1};
        int target = 2;

        int result = countPairs(nums, target);
        System.out.println(result); // Expected: 3
    }
}

/**
 * Problem Statement:
 * Given a 0-indexed integer array `nums` of length `n`, and an integer `target`.
 * Find the number of distinct pairs (i, j) where 0 <= i < j < n and 
 * nums[i] + nums[j] < target.
 * 
 * Constraints:
 * - 1 <= nums.length <= 50
 * - -50 <= nums[i], target <= 50
 */
class CountPairsSumLessThanTarget {

    /**
     * SOLUTION 1: Brute Force (Optimal for small constraints like N=50)
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Simply iterate through every possible pair (i, j) where i < j and check 
     * if their sum is less than the target. Because N is at most 50, N^2 is at 
     * most 2500 operations, which runs instantaneously.
     */
    public static int countPairsBruteForce(int[] nums, int target) {
        int count = 0;
        int n = nums.length;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (nums[i] + nums[j] < target) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * SOLUTION 2: Sorting + Two Pointers (Optimal Algorithmic Approach)
     * 
     * Time Complexity: O(N log N) for sorting + O(N) for two pointers.
     * Space Complexity: O(1) auxiliary (or O(N) depending on sorting implementation)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * Sorting the array does not change the number of valid pairs, because a pair 
     * is just any combination of two different elements. 
     * 
     * nums = [-1, 1, 2, 3, 1], target = 2
     * Sorted: [-1, 1, 1, 2, 3]
     * 
     * Pointers at left (L=0, value -1) and right (R=4, value 3).
     * -1 + 3 = 2. Since 2 is NOT < 2, the sum is too large. Move R left (R=3).
     * 
     * L=0, R=3 (values -1, 2). -1 + 2 = 1. Since 1 < 2, this is valid!
     * Because the array is sorted, if nums[L] + nums[R] < target, then nums[L] 
     * added to ANY element between L and R will ALSO be < target.
     * Number of valid pairs for current L = (R - L) = (3 - 0) = 3 pairs.
     * Move L right (L=1).
     */
    public static int countPairsTwoPointers(int[] nums, int target) {
        int[] sortedNums = nums.clone(); // Clone to preserve original array for other tests
        Arrays.sort(sortedNums);
        
        int left = 0;
        int right = sortedNums.length - 1;
        int count = 0;
        
        while (left < right) {
            if (sortedNums[left] + sortedNums[right] < target) {
                // All elements from left+1 to right are valid when paired with left
                count += (right - left);
                left++; // Move left pointer to evaluate the next smallest element
            } else {
                right--; // Sum is too large, need a smaller element
            }
        }
        
        return count;
    }

    /**
     * SOLUTION 3: Sorting + Iterative Binary Search
     * 
     * Time Complexity: O(N log N)
     * Space Complexity: O(1) auxiliary
     * 
     * EXPLANATION:
     * For each element at index `i`, we use Binary Search to find the LARGEST index `j` 
     * (where j > i) such that nums[i] + nums[j] < target.
     * Using an explicit `result` variable ensures we track the furthest valid boundary.
     */
    public static int countPairsBinarySearch(int[] nums, int target) {
        int[] sortedNums = nums.clone();
        Arrays.sort(sortedNums);
        
        int count = 0;
        int n = sortedNums.length;
        
        for (int i = 0; i < n - 1; i++) {
            int low = i + 1;
            int high = n - 1;
            
            // Explicit result variable, defaulting to 'i' meaning no valid 'j' found yet
            int result = i; 
            
            while (low <= high) {
                int mid = low + (high - low) / 2;
                
                if (sortedNums[i] + sortedNums[mid] < target) {
                    result = mid;   // Valid pair found, save index
                    low = mid + 1;  // Try to find a larger index that is still valid
                } else {
                    high = mid - 1; // Sum too large, try a smaller index
                }
            }
            
            // Number of valid pairs for element `i` is the difference between indices
            count += (result - i);
        }
        
        return count;
    }

    /**
     * SOLUTION 4: Sorting + Recursive Binary Search
     * 
     * Time Complexity: O(N log N)
     * Space Complexity: O(log N) - Call stack overhead for recursion.
     * 
     * EXPLANATION:
     * Wraps the binary search logic from Solution 3 into a recursive function, tracking
     * the valid index using `currentResult` parameter.
     */
    public static int countPairsRecursiveBSWrapper(int[] nums, int target) {
        int[] sortedNums = nums.clone();
        Arrays.sort(sortedNums);
        
        int count = 0;
        for (int i = 0; i < sortedNums.length - 1; i++) {
            int validIndex = findLargestValidIndex(sortedNums, target - sortedNums[i], i + 1, sortedNums.length - 1, i);
            count += (validIndex - i);
        }
        
        return count;
    }

    private static int findLargestValidIndex(int[] nums, int maxAllowedValue, int low, int high, int currentResult) {
        int result = currentResult; // Explicit result tracking
        
        if (low > high) {
            return result; // Base case: search space exhausted
        }
        
        int mid = low + (high - low) / 2;
        
        if (nums[mid] < maxAllowedValue) {
            // Found a valid index, record it and search higher
            result = findLargestValidIndex(nums, maxAllowedValue, mid + 1, high, mid);
        } else {
            // Value is too large, search lower
            result = findLargestValidIndex(nums, maxAllowedValue, low, mid - 1, result);
        }
        
        return result;
    }

    /**
     * SOLUTION 5: Java Streams (Functional Approach)
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(1) overhead
     * 
     * EXPLANATION:
     * Uses flatMap to pair every index `i` with every index `j > i`, then filters
     * for sums strictly less than the target and counts them. Elegant but slow for massive N.
     */
    public static int countPairsStream(int[] nums, int target) {
        return (int) IntStream.range(0, nums.length)
                .boxed()
                .flatMap(i -> IntStream.range(i + 1, nums.length)
                        .filter(j -> nums[i] + nums[j] < target)
                        .boxed())
                .count();
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to cleanly map input arrays, targets, and expected outputs.
     */
    public record TestCase(int[] nums, int target, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on problem description and boundaries
        TestCase[] testCases = {
            new TestCase(new int[]{-1, 1, 2, 3, 1}, 2, 3),          // Example 1
            new TestCase(new int[]{-6, 2, 5, -2, -7, -1, 3}, -2, 10), // Example 2
            new TestCase(new int[]{1, 2, 3, 4, 5}, 10, 10),         // All pairs valid
            new TestCase(new int[]{10, 20, 30}, 5, 0),              // No pairs valid
            new TestCase(new int[]{0, 0, 0, 0}, 1, 6)               // Zeros
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resBruteForce = countPairsBruteForce(tc.nums(), tc.target());
            int resTwoPointers = countPairsTwoPointers(tc.nums(), tc.target());
            int resIterativeBS = countPairsBinarySearch(tc.nums(), tc.target());
            int resRecursiveBS = countPairsRecursiveBSWrapper(tc.nums(), tc.target());
            int resStream = countPairsStream(tc.nums(), tc.target());

            boolean passed = (resBruteForce == tc.expected()) &&
                             (resTwoPointers == tc.expected()) &&
                             (resIterativeBS == tc.expected()) &&
                             (resRecursiveBS == tc.expected()) &&
                             (resStream == tc.expected());

            // Limit array printing length for neat terminal output
            String arrStr = Arrays.toString(tc.nums());
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | Target: %-2d | Nums: %-25s -> Expected: %-2d | Passed: %b%n",
                    i + 1, tc.target(), arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Brute: %d, Pointers: %d, IterBS: %d, RecBS: %d, Stream: %d%n",
                        resBruteForce, resTwoPointers, resIterativeBS, resRecursiveBS, resStream);
            }
        }
    }
}

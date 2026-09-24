import java.util.*;

class SingleNonDuplicate {

    /**
     * ============================================================
     * 🔥 Find Single Element in Sorted Array (Binary Search)
     * ============================================================
     *
     * Intuition:
     * ----------
     * - Elements appear in pairs except one.
     * - Before the single element → pairs start at EVEN index.
     * - After the single element → pairs start at ODD index.
     *
     * We find the FIRST index where this pattern breaks.
     *
     * ------------------------------------------------------------
     * Monotonic Function:
     * ------------------------------------------------------------
     * Define:
     *   isValidPair(mid)
     *
     * True  → still in left (valid pairing region)
     * False → pattern broken → answer lies here or left
     *
     * So we are searching for:
     * 👉 FIRST FALSE
     *
     * ------------------------------------------------------------
     * Time  : O(log n)
     * Space : O(1)
     * ============================================================
     */
    public static int singleNonDuplicate(int[] nums) {

        int low = 0;
        int high = nums.length - 1;

        int answer = -1; // explicitly track result (your preferred style)

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // ----------------------------------------------------
            // Edge Case: If mid itself is the answer
            // ----------------------------------------------------
            boolean isLeftSame = (mid > 0 && nums[mid] == nums[mid - 1]);
            boolean isRightSame = (mid < nums.length - 1 && nums[mid] == nums[mid + 1]);

            // If not equal to neighbors → unique element
            if (!isLeftSame && !isRightSame) {
                answer = nums[mid];
                break;
            }

            // ----------------------------------------------------
            // Normalize mid to EVEN index
            // (So we always check first element of pair)
            // ----------------------------------------------------
            if (mid % 2 == 1) {
                mid--; // shift to even index
            }

            // ----------------------------------------------------
            // Check if pair is valid
            // ----------------------------------------------------
            if (nums[mid] == nums[mid + 1]) {
                // Valid pair → unique element is on RIGHT side
                low = mid + 2;
            } else {
                // Broken pair → this could be answer
                answer = nums[mid]; // store candidate
                high = mid - 1;
            }
        }

        return answer;
    }

    // ------------------------------------------------------------
    // Driver
    // ------------------------------------------------------------
    public static void main(String[] args) {

        int[] nums = {1,1,2,3,3,4,4,8,8};

        System.out.println(singleNonDuplicate(nums)); // Output: 2
    }
}

/**
 * Problem Statement:
 * You are given a sorted array of integers, `nums`, where all integers appear twice 
 * except for one. Find and return the single integer that appears only once.
 * 
 * Target Complexity: 
 * Time: O(log N)
 * Space: O(1)
 * 
 * Constraints:
 * - 1 <= nums.length <= 10^3 (The length will always be odd)
 * - 0 <= nums[i] <= 10^3
 */
class SingleElementInSortedArray {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * We can exploit the indices of the array. Since every element appears twice 
     * (except one), the pairs follow a specific index pattern.
     * 
     * Array:   [ 1, 1, 2, 3, 3, 4, 4, 8, 8 ]
     * Indices: [ 0, 1, 2, 3, 4, 5, 6, 7, 8 ]
     * 
     * Notice the index pairs before the single element (2 at index 2):
     * - (0, 1): 1st element of pair is at an EVEN index.
     * 
     * Notice the index pairs after the single element:
     * - (3, 4): 1st element of pair is at an ODD index.
     * - (5, 6): 1st element of pair is at an ODD index.
     * 
     * Rule to find which half the single element is in:
     * If we are at `mid`, and the pair starts at an EVEN index and ends at an ODD index,
     * the sequence is intact. The single element MUST be to the right.
     * Otherwise, the sequence is broken, and the single element is to the left (or is `mid`).
     * 
     * To prevent OutOfBounds when checking mid-1 or mid+1, we evaluate the outer boundaries
     * first, then run binary search strictly from index 1 to n-2.
     */
    public static int singleNonDuplicateIterative(int[] nums) {
        int n = nums.length;
        
        // Edge Cases
        if (n == 1) return nums[0]; // Only one element in the array
        if (nums[0] != nums[1]) return nums[0]; // Single element is at the very beginning
        if (nums[n - 1] != nums[n - 2]) return nums[n - 1]; // Single element is at the very end
        
        // Binary search on the inner elements
        int low = 1;
        int high = n - 2;
        int result = -1; // Explicit result variable

        while (low <= high) {
            int mid = low + (high - low) / 2;

            // If mid element is different from both neighbors, we found the target
            if (nums[mid] != nums[mid - 1] && nums[mid] != nums[mid + 1]) {
                result = nums[mid];
                break;
            }

            // We are on the LEFT side of the single element if:
            // - mid is even AND mid is the first part of a pair (nums[mid] == nums[mid+1])
            // - mid is odd AND mid is the second part of a pair (nums[mid] == nums[mid-1])
            if ((mid % 2 == 0 && nums[mid] == nums[mid + 1]) || 
                (mid % 2 == 1 && nums[mid] == nums[mid - 1])) {
                // The single element is further to the right
                low = mid + 1;
            } else {
                // The single element is on the left
                high = mid - 1;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search (Optimal O(log N))
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(log N) - Recursive call stack overhead.
     * 
     * EXPLANATION:
     * Transforms the iterative parity logic into a recursive function.
     * It uses a wrapper method to handle the boundary conditions before recurring.
     */
    public static int singleNonDuplicateRecursiveWrapper(int[] nums) {
        int n = nums.length;
        if (n == 1) return nums[0];
        if (nums[0] != nums[1]) return nums[0];
        if (nums[n - 1] != nums[n - 2]) return nums[n - 1];

        return singleNonDuplicateRecursive(nums, 1, n - 2, -1);
    }

    private static int singleNonDuplicateRecursive(int[] nums, int low, int high, int currentResult) {
        int result = currentResult; // explicit tracking variable

        if (low > high) {
            return result; // Base case: crossed boundaries
        }

        int mid = low + (high - low) / 2;

        // Check if mid is the single element
        if (nums[mid] != nums[mid - 1] && nums[mid] != nums[mid + 1]) {
            return nums[mid]; // Return immediately when found
        }

        // Parity logic to divide search space
        if ((mid % 2 == 0 && nums[mid] == nums[mid + 1]) || 
            (mid % 2 == 1 && nums[mid] == nums[mid - 1])) {
            result = singleNonDuplicateRecursive(nums, mid + 1, high, result);
        } else {
            result = singleNonDuplicateRecursive(nums, low, mid - 1, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Bitwise XOR (Sub-optimal Time, Optimal Space)
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * While this violates the O(log N) constraint, XOR is the classic mathematical 
     * approach to "find the single number" problems. 
     * Properties of XOR:
     * - A ^ A = 0
     * - A ^ 0 = A
     * By XORing all numbers, every pair cancels out to 0, leaving only the single element.
     */
    public static int singleNonDuplicateXOR(int[] nums) {
        int result = 0;
        for (int num : nums) {
            result ^= num;
        }
        return result;
    }

    /**
     * SOLUTION 4: Java Streams with Reduction (Functional Approach)
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1) overhead
     * 
     * EXPLANATION:
     * Uses Java Streams to apply the bitwise XOR reduction functionally.
     * This iterates over the entire array but provides extremely clean syntax.
     */
    public static int singleNonDuplicateStream(int[] nums) {
        return Arrays.stream(nums)
                .reduce(0, (a, b) -> a ^ b);
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * A Java Record that maps out the input array and the expected output.
     */
    public record TestCase(int[] nums, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on constraints and edge cases
        TestCase[] testCases = {
            new TestCase(new int[]{1, 1, 2, 3, 3, 4, 4, 8, 8}, 2), // Standard case in middle
            new TestCase(new int[]{3, 3, 7, 7, 10, 11, 11}, 10),   // Single element toward the end
            new TestCase(new int[]{1}, 1),                         // Smallest possible array
            new TestCase(new int[]{1, 2, 2}, 1),                   // Single element is the first element
            new TestCase(new int[]{2, 2, 3}, 3),                   // Single element is the last element
            new TestCase(new int[]{1, 1, 2, 2, 3, 3, 4, 4, 5}, 5)  // Large sequence, last is single
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterative = singleNonDuplicateIterative(tc.nums());
            int resRecursive = singleNonDuplicateRecursiveWrapper(tc.nums());
            int resXOR       = singleNonDuplicateXOR(tc.nums());
            int resStream    = singleNonDuplicateStream(tc.nums());

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resXOR == tc.expected()) &&
                             (resStream == tc.expected());

            System.out.printf("Test %d | Array Length: %-2d | Expected: %-2d | Passed: %b%n",
                    i + 1, tc.nums().length, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed on %s] Iterative: %d, Recursive: %d, XOR: %d, Stream: %d%n",
                        Arrays.toString(tc.nums()), resIterative, resRecursive, resXOR, resStream);
            }
        }
    }
}

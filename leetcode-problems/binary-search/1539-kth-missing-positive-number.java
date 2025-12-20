class KthMissingPositive {

    // Approach 1: Binary Search (Optimal)
    // Time: O(log n), Space: O(1)
    public int findKthPositive1(int[] arr, int k) {
        int left = 0;
        int right = arr.length;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // Count of missing numbers at index mid
            int missing = arr[mid] - mid - 1;

            if (missing < k) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        // The kth missing number is at position: left + k
        return left + k;
    }

    // Approach 2: Binary Search with Detailed Comments
    // Time: O(log n), Space: O(1)
    public int findKthPositive2(int[] arr, int k) {
        int left = 0;
        int right = arr.length;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // At index mid:
            // - arr[mid] numbers exist from 1 to arr[mid]
            // - (mid + 1) of them are in the array
            // - Missing count = arr[mid] - (mid + 1)
            int missingCount = arr[mid] - mid - 1;

            if (missingCount < k) {
                // Not enough missing numbers before arr[mid]
                // The kth missing is after this position
                left = mid + 1;
            } else {
                // We have at least k missing numbers
                // The kth missing is at or before this position
                right = mid;
            }
        }

        // After binary search:
        // left is the insertion point where we have k or more missing
        // The kth missing number = left + k
        return left + k;
    }

    // Approach 3: Linear Search (Simple but slower)
    // Time: O(n), Space: O(1)
    public int findKthPositive3(int[] arr, int k) {
        for (int i = 0; i < arr.length; i++) {
            // Count missing numbers before arr[i]
            int missing = arr[i] - i - 1;

            if (missing >= k) {
                // The kth missing is before arr[i]
                // It's the (k)th number after position i-1
                // Which is: i + k (or arr[i-1] + remaining if i > 0)
                return i + k;
            }
        }

        // If we reach here, kth missing is after the array
        return arr.length + k;
    }

    // Approach 4: Brute Force with Actual Missing List
    // Time: O(arr[n-1] + k), Space: O(1)
    public int findKthPositive4(int[] arr, int k) {
        int arrIdx = 0;
        int currentNum = 1;
        int missingCount = 0;

        while (true) {
            if (arrIdx < arr.length && arr[arrIdx] == currentNum) {
                // Current number is in array
                arrIdx++;
            } else {
                // Current number is missing
                missingCount++;
                if (missingCount == k) {
                    return currentNum;
                }
            }
            currentNum++;
        }
    }

    // Approach 5: Mathematical Approach
    // Time: O(log n), Space: O(1)
    public int findKthPositive5(int[] arr, int k) {
        // Binary search to find the position
        int left = 0, right = arr.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int missing = arr[mid] - mid - 1;

            if (missing < k) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        // At this point, right is the last index where missing < k
        // The kth missing is: arr[right] + (k - missing_at_right)
        // Which simplifies to: right + 1 + k
        return left + k;
    }

    // Test cases with detailed explanation
    public static void main(String[] args) {
        KthMissingPositive solution = new KthMissingPositive();

        // Test Case 1
        int[] arr1 = { 2, 3, 4, 7, 11 };
        int k1 = 5;
        int result1 = solution.findKthPositive1(arr1, k1);
        System.out.println("Test 1: " + result1); // Output: 9
        explainSolution(arr1, k1, result1);

        // Test Case 2
        int[] arr2 = { 1, 2, 3, 4 };
        int k2 = 2;
        int result2 = solution.findKthPositive1(arr2, k2);
        System.out.println("\nTest 2: " + result2); // Output: 6
        explainSolution(arr2, k2, result2);

        // Test Case 3: First element not 1
        int[] arr3 = { 5, 6, 7, 8, 9 };
        int k3 = 9;
        int result3 = solution.findKthPositive1(arr3, k3);
        System.out.println("\nTest 3: " + result3); // Output: 14
        explainSolution(arr3, k3, result3);

        // Test Case 4: Large gap
        int[] arr4 = { 1, 10, 21 };
        int k4 = 12;
        int result4 = solution.findKthPositive1(arr4, k4);
        System.out.println("\nTest 4: " + result4); // Output: 14

        // Test Case 5: k larger than all missing
        int[] arr5 = { 1, 2 };
        int k5 = 1;
        int result5 = solution.findKthPositive1(arr5, k5);
        System.out.println("Test 5: " + result5); // Output: 3

        // Verify all approaches give same result
        System.out.println("\nVerifying all approaches for Test 1:");
        System.out.println("Approach 1: " + solution.findKthPositive1(arr1, k1));
        System.out.println("Approach 2: " + solution.findKthPositive2(arr1, k1));
        System.out.println("Approach 3: " + solution.findKthPositive3(arr1, k1));
        System.out.println("Approach 4: " + solution.findKthPositive4(arr1, k1));
        System.out.println("Approach 5: " + solution.findKthPositive5(arr1, k1));
    }

    private static void explainSolution(int[] arr, int k, int result) {
        System.out.println("Array: " + java.util.Arrays.toString(arr));
        System.out.println("k = " + k);

        // Show missing counts at each index
        System.out.println("Missing counts at each index:");
        for (int i = 0; i < arr.length; i++) {
            int missing = arr[i] - i - 1;
            System.out.printf("  Index %d (arr[%d]=%d): %d missing numbers%n",
                    i, i, arr[i], missing);
        }

        // List actual missing numbers
        System.out.print("Missing positive integers: [");
        int count = 0;
        int num = 1;
        int arrIdx = 0;
        while (count < Math.min(k + 5, 20)) {
            if (arrIdx < arr.length && arr[arrIdx] == num) {
                arrIdx++;
            } else {
                if (count > 0)
                    System.out.print(", ");
                if (count == k - 1) {
                    System.out.print("**" + num + "**");
                } else {
                    System.out.print(num);
                }
                count++;
            }
            num++;
        }
        if (count < k) {
            System.out.print(", ...");
        }
        System.out.println("]");
        System.out.println("Answer: " + result);
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * KEY INSIGHT - Missing Count Formula:
 * At any index i in the array:
 * - Numbers from 1 to arr[i] should be: arr[i] numbers total
 * - Numbers actually in array up to index i: (i + 1) numbers
 * - Missing count = arr[i] - (i + 1) = arr[i] - i - 1
 * 
 * Example: arr = [2,3,4,7,11]
 * - Index 0: arr[0]=2, missing = 2 - 0 - 1 = 1 (missing: [1])
 * - Index 1: arr[1]=3, missing = 3 - 1 - 1 = 1 (missing: [1])
 * - Index 2: arr[2]=4, missing = 4 - 2 - 1 = 1 (missing: [1])
 * - Index 3: arr[3]=7, missing = 7 - 3 - 1 = 3 (missing: [1,5,6])
 * - Index 4: arr[4]=11, missing = 11 - 4 - 1 = 6 (missing: [1,5,6,8,9,10])
 * 
 * BINARY SEARCH APPROACH:
 * Since missing counts are monotonically increasing, we can use binary search!
 * 
 * Goal: Find the smallest index where missing count >= k
 * 
 * After binary search, 'left' points to the position where:
 * - Elements before left have < k missing numbers
 * - Elements from left onwards have >= k missing numbers
 * 
 * The kth missing number is at position: left + k
 * 
 * WHY left + k?
 * Think of it as placing k missing numbers starting from position 0:
 * - Position 0-1: would place missing numbers [1, ...]
 * - Position 1-2: would place missing numbers after arr[0]
 * - Position left: is where we need to place the kth missing
 * 
 * EXAMPLE WALKTHROUGH: arr=[2,3,4,7,11], k=5
 * 
 * Binary Search:
 * Initial: left=0, right=5
 * 
 * Iteration 1: mid=2
 * - missing[2] = 4 - 2 - 1 = 1 < 5
 * - left = 3
 * 
 * Iteration 2: left=3, right=5, mid=4
 * - missing[4] = 11 - 4 - 1 = 6 >= 5
 * - right = 4
 * 
 * Iteration 3: left=3, right=4, mid=3
 * - missing[3] = 7 - 3 - 1 = 3 < 5
 * - left = 4
 * 
 * Result: left=4, right=4
 * Answer = left + k = 4 + 5 = 9 ✓
 * 
 * Verification:
 * Missing numbers: [1,5,6,8,9,10,12,...]
 * The 5th missing is 9 ✓
 * 
 * EXAMPLE 2: arr=[1,2,3,4], k=2
 * 
 * All indices have 0 missing (perfectly consecutive from 1)
 * Binary search will end with left=4 (past the array)
 * Answer = 4 + 2 = 6 ✓
 * 
 * Missing numbers: [5,6,7,...]
 * The 2nd missing is 6 ✓
 * 
 * COMPLEXITY ANALYSIS:
 * Approach 1 (Binary Search):
 * - Time: O(log n) - binary search through array
 * - Space: O(1) - constant extra space
 * 
 * Approach 3 (Linear):
 * - Time: O(n) - single pass through array
 * - Space: O(1)
 * 
 * Approach 4 (Brute Force):
 * - Time: O(result) - iterate from 1 to result
 * - Space: O(1)
 * 
 * EDGE CASES:
 * 1. arr starts with 1: some missing are after array end
 * 2. arr starts > 1: first missing numbers are 1, 2, ...
 * 3. k larger than all missing before array end: answer = arr.length + k
 * 4. Large gaps in array: many consecutive missing numbers
 * 5. Single element array: straightforward calculation
 * 
 * WHY BINARY SEARCH IS OPTIMAL:
 * - Array is sorted (monotonic property for missing counts)
 * - We don't need to examine every element
 * - O(log n) vs O(n) is significant for large arrays
 * - Direct mathematical formula for answer
 * 
 * FORMULA DERIVATION:
 * At position left (from binary search):
 * - We have fewer than k missing before this position
 * - The kth missing starts from this position
 * - Since positions 0 to left-1 are "taken" by array elements or earlier
 * missing
 * - The kth missing is at virtual position: left + k
 */

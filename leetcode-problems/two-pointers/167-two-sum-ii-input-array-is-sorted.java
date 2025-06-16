import java.util.*;
/*
 * Given a 1-indexed array of integers numbers that is already sorted in
 * non-decreasing order, find two numbers such that they add up to a specific
 * target number. Let these two numbers be numbers[index1] and numbers[index2]
 * where 1 <= index1 < index2 <= numbers.length.
 * 
 * Return the indices of the two numbers, index1 and index2, added by one as an
 * integer array [index1, index2] of length 2.
 * 
 * The tests are generated such that there is exactly one solution. You may not
 * use the same element twice.
 * 
 * Your solution must use only constant extra space.
 * 
 * Example 1:
 * Input: numbers = [2,7,11,15], target = 9
 * Output: [1,2]
 * Explanation: The sum of 2 and 7 is 9. Therefore, index1 = 1, index2 = 2. We
 * return [1, 2].
 * 
 * Example 2:
 * Input: numbers = [2,3,4], target = 6
 * Output: [1,3]
 * Explanation: The sum of 2 and 4 is 6. Therefore index1 = 1, index2 = 3. We
 * return [1, 3].
 * 
 * Example 3:
 * Input: numbers = [-1,0], target = -1
 * Output: [1,2]
 * Explanation: The sum of -1 and 0 is -1. Therefore index1 = 1, index2 = 2. We
 * return [1, 2].
 */

class TwoSumIISolutions {

    // Solution 1: Two Pointers (OPTIMAL - meets all requirements)
    // Time: O(n), Space: O(1)
    public int[] twoSum1(int[] numbers, int target) {
        int left = 0;
        int right = numbers.length - 1;

        while (left < right) {
            int sum = numbers[left] + numbers[right];

            if (sum == target) {
                // Return 1-indexed positions
                return new int[] { left + 1, right + 1 };
            } else if (sum < target) {
                left++; // Need larger sum, move left pointer right
            } else {
                right--; // Need smaller sum, move right pointer left
            }
        }

        // This should never happen given problem constraints
        return new int[] { -1, -1 };
    }

    // Solution 2: Binary Search approach
    // Time: O(n log n), Space: O(1)
    public int[] twoSum2(int[] numbers, int target) {
        for (int i = 0; i < numbers.length - 1; i++) {
            int complement = target - numbers[i];

            // Binary search for complement in remaining array
            int complementIndex = binarySearch(numbers, i + 1, numbers.length - 1, complement);

            if (complementIndex != -1) {
                return new int[] { i + 1, complementIndex + 1 }; // 1-indexed
            }
        }

        return new int[] { -1, -1 };
    }

    private int binarySearch(int[] arr, int left, int right, int target) {
        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return -1;
    }

    // Solution 3: HashMap approach (violates space constraint but included for
    // comparison)
    // Time: O(n), Space: O(n)
    public int[] twoSum3(int[] numbers, int target) {
        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < numbers.length; i++) {
            int complement = target - numbers[i];

            if (map.containsKey(complement)) {
                return new int[] { map.get(complement) + 1, i + 1 }; // 1-indexed
            }

            map.put(numbers[i], i);
        }

        return new int[] { -1, -1 };
    }

    // Solution 4: Brute Force (for educational comparison)
    // Time: O(n²), Space: O(1)
    public int[] twoSum4(int[] numbers, int target) {
        for (int i = 0; i < numbers.length - 1; i++) {
            for (int j = i + 1; j < numbers.length; j++) {
                if (numbers[i] + numbers[j] == target) {
                    return new int[] { i + 1, j + 1 }; // 1-indexed
                }
                // Optimization: since array is sorted, if sum is too large, break
                if (numbers[i] + numbers[j] > target) {
                    break;
                }
            }
        }

        return new int[] { -1, -1 };
    }

    // Solution 5: Enhanced Two Pointers with early termination optimizations
    // Time: O(n), Space: O(1)
    public int[] twoSum5(int[] numbers, int target) {
        int left = 0;
        int right = numbers.length - 1;

        // Early termination: if smallest two numbers > target
        if (numbers[0] + numbers[1] > target) {
            return new int[] { -1, -1 };
        }

        // Early termination: if largest two numbers < target
        if (numbers[right - 1] + numbers[right] < target) {
            return new int[] { -1, -1 };
        }

        while (left < right) {
            int sum = numbers[left] + numbers[right];

            if (sum == target) {
                return new int[] { left + 1, right + 1 };
            } else if (sum < target) {
                left++;
            } else {
                right--;
            }
        }

        return new int[] { -1, -1 };
    }

    // Helper method to demonstrate the algorithm step by step
    public int[] twoSumDetailed(int[] numbers, int target) {
        int left = 0;
        int right = numbers.length - 1;

        System.out.println("Finding two numbers that sum to: " + target);
        System.out.println("Array: " + Arrays.toString(numbers));
        System.out.println();

        while (left < right) {
            int sum = numbers[left] + numbers[right];

            System.out.printf("left=%d (value=%d), right=%d (value=%d), sum=%d\n",
                    left, numbers[left], right, numbers[right], sum);

            if (sum == target) {
                System.out.println("Found target! Returning indices: " + (left + 1) + ", " + (right + 1));
                return new int[] { left + 1, right + 1 };
            } else if (sum < target) {
                System.out.println("Sum too small, moving left pointer right");
                left++;
            } else {
                System.out.println("Sum too large, moving right pointer left");
                right--;
            }
            System.out.println();
        }

        return new int[] { -1, -1 };
    }

    // Test method
    public static void main(String[] args) {
        TwoSumIISolutions solution = new TwoSumIISolutions();

        // Test cases
        int[][] testArrays = {
                { 2, 7, 11, 15 },
                { 2, 3, 4 },
                { -1, 0 },
                { 1, 2, 3, 4, 4, 9, 56, 90 },
                { 5, 25, 75 }
        };

        int[] targets = { 9, 6, -1, 8, 100 };

        System.out.println("Testing all solutions:\n");

        for (int i = 0; i < testArrays.length; i++) {
            int[] numbers = testArrays[i];
            int target = targets[i];

            System.out.println("Test Case " + (i + 1) + ":");
            System.out.println("Input: numbers = " + Arrays.toString(numbers) + ", target = " + target);

            int[] result1 = solution.twoSum1(numbers, target);
            int[] result2 = solution.twoSum2(numbers, target);
            int[] result3 = solution.twoSum3(numbers, target);
            int[] result4 = solution.twoSum4(numbers, target);
            int[] result5 = solution.twoSum5(numbers, target);

            System.out.println("Two Pointers: " + Arrays.toString(result1));
            System.out.println("Binary Search: " + Arrays.toString(result2));
            System.out.println("HashMap: " + Arrays.toString(result3));
            System.out.println("Brute Force: " + Arrays.toString(result4));
            System.out.println("Enhanced Two Pointers: " + Arrays.toString(result5));
            System.out.println();
        }

        // Detailed walkthrough for first example
        System.out.println("=== Detailed Walkthrough ===");
        solution.twoSumDetailed(new int[] { 2, 7, 11, 15 }, 9);
    }

}

/*
 * ANALYSIS:
 * 
 * Solution 1 - Two Pointers (RECOMMENDED):
 * - Pros: Optimal O(n) time, O(1) space, leverages sorted property
 * - Cons: None - this is the intended solution
 * - Why it works: Sorted array allows us to eliminate half the search space
 * each step
 * 
 * Solution 2 - Binary Search:
 * - Pros: Good use of sorted property, O(1) space
 * - Cons: O(n log n) time complexity, more complex
 * - Use case: When you want to practice binary search
 * 
 * Solution 3 - HashMap:
 * - Pros: O(n) time, familiar approach from Two Sum I
 * - Cons: Violates O(1) space constraint, doesn't use sorted property
 * - Use case: Comparison with unsorted version
 * 
 * Solution 4 - Brute Force:
 * - Pros: Simple to understand, includes optimization for sorted array
 * - Cons: O(n²) time complexity
 * - Use case: Educational baseline
 * 
 * Solution 5 - Enhanced Two Pointers:
 * - Pros: Same as Solution 1 plus early termination optimizations
 * - Cons: Slightly more complex code
 * - Use case: When you want maximum performance
 * 
 * KEY INSIGHTS:
 * 1. The sorted property is crucial - it allows the two-pointer technique
 * 2. When sum is too small, we need a larger number (move left pointer right)
 * 3. When sum is too large, we need a smaller number (move right pointer left)
 * 4. This eliminates the need for extra space that HashMap approach requires
 * 
 * TIME/SPACE COMPLEXITY:
 * - Two Pointers: O(n) time, O(1) space ✓ (meets all requirements)
 * - Binary Search: O(n log n) time, O(1) space
 * - HashMap: O(n) time, O(n) space (violates space constraint)
 * - Brute Force: O(n²) time, O(1) space
 * 
 * INTERVIEW TIP:
 * Always mention that you're leveraging the sorted property of the input array.
 * This shows you're thinking about the problem constraints and optimizations.
 */

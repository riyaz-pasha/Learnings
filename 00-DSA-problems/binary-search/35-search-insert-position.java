/*
 * Given a sorted array of distinct integers and a target value, return the
 * index if the target is found. If not, return the index where it would be if
 * it were inserted in order.
 * 
 * You must write an algorithm with O(log n) runtime complexity.
 * 
 * Example 1:
 * Input: nums = [1,3,5,6], target = 5
 * Output: 2
 * 
 * Example 2:
 * Input: nums = [1,3,5,6], target = 2
 * Output: 1
 * 
 * Example 3:
 * Input: nums = [1,3,5,6], target = 7
 * Output: 4
 */

class Solution {

    // APPROACH 1: Standard Binary Search (Optimal) ⭐
    // Time: O(log n), Space: O(1)
    public int searchInsert(int[] nums, int target) {
        if (nums == null || nums.length == 0)
            return 0;

        int left = 0;
        int right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) {
                return mid;
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        // If not found, left is the insertion position
        return left;
    }

}

// APPROACH 2: Binary Search with Early Termination
// Time: O(log n), Space: O(1)
class SolutionEarlyTermination {

    public int searchInsert(int[] nums, int target) {
        if (nums == null || nums.length == 0)
            return 0;

        // Handle edge cases first
        if (target <= nums[0])
            return 0;
        if (target > nums[nums.length - 1])
            return nums.length;

        int left = 0;
        int right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) {
                return mid;
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return left;
    }

}

// APPROACH 3: Recursive Binary Search
// Time: O(log n), Space: O(log n) due to recursion stack
class SolutionRecursive {

    public int searchInsert(int[] nums, int target) {
        if (nums == null || nums.length == 0)
            return 0;
        return binarySearchRecursive(nums, target, 0, nums.length - 1);
    }

    private int binarySearchRecursive(int[] nums, int target, int left, int right) {
        if (left > right) {
            return left; // Insertion position
        }

        int mid = left + (right - left) / 2;

        if (nums[mid] == target) {
            return mid;
        } else if (nums[mid] < target) {
            return binarySearchRecursive(nums, target, mid + 1, right);
        } else {
            return binarySearchRecursive(nums, target, left, mid - 1);
        }
    }

}

// APPROACH 4: Lower Bound Implementation
// Time: O(log n), Space: O(1)
class SolutionLowerBound {

    public int searchInsert(int[] nums, int target) {
        if (nums == null || nums.length == 0)
            return 0;

        // Find the lower bound (first position where element >= target)
        int left = 0;
        int right = nums.length; // Note: right = length, not length - 1

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

}

// APPROACH 5: Template-based Binary Search
// Time: O(log n), Space: O(1)
class SolutionTemplate {

    public int searchInsert(int[] nums, int target) {
        if (nums == null || nums.length == 0)
            return 0;

        return lowerBound(nums, target);
    }

    // Lower bound: first position where nums[i] >= target
    private int lowerBound(int[] nums, int target) {
        int left = 0, right = nums.length;

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

    // Upper bound: first position where nums[i] > target
    private int upperBound(int[] nums, int target) {
        int left = 0, right = nums.length;

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] <= target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

}

// APPROACH 6: Linear Search (for comparison - not optimal)
// Time: O(n), Space: O(1)
class SolutionLinear {

    public int searchInsert(int[] nums, int target) {
        if (nums == null || nums.length == 0)
            return 0;

        for (int i = 0; i < nums.length; i++) {
            if (nums[i] >= target) {
                return i;
            }
        }

        return nums.length; // Insert at the end
    }

}

// Test class to demonstrate all approaches
class TestSearchInsert {

    public static void main(String[] args) {
        Solution solution = new Solution();
        SolutionLowerBound lowerBound = new SolutionLowerBound();
        SolutionRecursive recursive = new SolutionRecursive();

        // Test cases
        int[][][] testCases = {
                { { 1, 3, 5, 6 }, { 5 } }, // Expected: 2
                { { 1, 3, 5, 6 }, { 2 } }, // Expected: 1
                { { 1, 3, 5, 6 }, { 7 } }, // Expected: 4
                { { 1, 3, 5, 6 }, { 0 } }, // Expected: 0
                { { 1 }, { 1 } }, // Expected: 0
                { { 1 }, { 2 } }, // Expected: 1
                { { 1 }, { 0 } }, // Expected: 0
                { {}, { 1 } }, // Expected: 0 (empty array)
                { { 1, 2, 3, 4, 5 }, { 3 } }, // Expected: 2
                { { 2, 4, 6, 8, 10 }, { 5 } }, // Expected: 2
                { { 2, 4, 6, 8, 10 }, { 1 } }, // Expected: 0
                { { 2, 4, 6, 8, 10 }, { 11 } } // Expected: 5
        };

        for (int i = 0; i < testCases.length; i++) {
            int[] nums = testCases[i][0];
            int target = testCases[i][1][0];

            System.out.println("\nTest Case " + (i + 1) + ":");
            System.out.println("Array: " + java.util.Arrays.toString(nums));
            System.out.println("Target: " + target);

            int result1 = solution.searchInsert(nums, target);
            int result2 = lowerBound.searchInsert(nums, target);
            int result3 = recursive.searchInsert(nums, target);

            System.out.println("Standard Binary Search: " + result1);
            System.out.println("Lower Bound: " + result2);
            System.out.println("Recursive: " + result3);

            // Verify all approaches give same result
            if (result1 != result2 || result2 != result3) {
                System.out.println("⚠️  Results don't match!");
            }

            // Visualize the insertion
            visualizeInsertion(nums, target, result1);
        }

        // Performance comparison
        performanceTest();
    }

    private static void visualizeInsertion(int[] nums, int target, int insertPos) {
        if (nums.length == 0) {
            System.out.println("Insert " + target + " into empty array at position 0");
            return;
        }

        System.out.print("Visualization: [");
        for (int i = 0; i <= nums.length; i++) {
            if (i == insertPos) {
                System.out.print("(" + target + ")");
                if (i < nums.length)
                    System.out.print(", ");
            }
            if (i < nums.length) {
                System.out.print(nums[i]);
                if (i < nums.length - 1)
                    System.out.print(", ");
            }
        }
        System.out.println("]");
    }

    private static void performanceTest() {
        System.out.println("\n--- Performance Test ---");

        // Create large sorted array
        int[] largeArray = new int[1000000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i * 2; // Even numbers: 0, 2, 4, 6, ...
        }

        Solution binarySearch = new Solution();
        SolutionLinear linearSearch = new SolutionLinear();

        int searchTarget = 999999; // Odd number, will need insertion

        // Test binary search
        long start = System.nanoTime();
        int binaryResult = binarySearch.searchInsert(largeArray, searchTarget);
        long binaryTime = System.nanoTime() - start;

        // Test linear search
        start = System.nanoTime();
        int linearResult = linearSearch.searchInsert(largeArray, searchTarget);
        long linearTime = System.nanoTime() - start;

        System.out.println("Array size: " + largeArray.length);
        System.out.println("Target: " + searchTarget);
        System.out.println("Binary Search: " + binaryResult + " (Time: " + binaryTime / 1000 + " μs)");
        System.out.println("Linear Search: " + linearResult + " (Time: " + linearTime / 1000 + " μs)");
        System.out.println("Speed improvement: " + (linearTime / binaryTime) + "x faster");
    }

}

// Utility class for binary search variations
class BinarySearchUtils {

    // Find exact position of target, or -1 if not found
    public static int binarySearch(int[] nums, int target) {
        int left = 0, right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                return mid;
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1; // Not found
    }

    // Find first occurrence of target
    public static int findFirst(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                result = mid;
                right = mid - 1; // Continue searching left
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    // Find last occurrence of target
    public static int findLast(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                result = mid;
                left = mid + 1; // Continue searching right
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    // Find insertion position for target to maintain sorted order
    public static int insertPosition(int[] nums, int target) {
        int left = 0, right = nums.length;

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

}

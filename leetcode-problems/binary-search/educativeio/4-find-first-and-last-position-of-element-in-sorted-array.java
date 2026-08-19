import java.util.*;

/**
 * Problem: Find First and Last Position of Element in Sorted Array
 *
 * Idea:
 *  - Run Binary Search twice:
 *      1. Find first occurrence (left boundary)
 *      2. Find last occurrence (right boundary)
 *
 * WHY IT WORKS?
 *  - Array is sorted → duplicates are adjacent
 *  - Binary search helps us shrink towards boundaries
 *
 * IMPORTANT (Interview Tip):
 *  - Do NOT return immediately when target found
 *  - Instead, update answer and continue searching
 *
 * Time Complexity: O(log n)
 * Space Complexity: O(1)
 */
class FirstLastPosition {

    public static void main(String[] args) {
        int[] nums = {5, 7, 7, 8, 8, 10};
        int target = 8;

        int[] result = searchRange(nums, target);
        System.out.println(Arrays.toString(result)); // [3, 4]
    }

    public static int[] searchRange(int[] nums, int target) {

        int first = findFirst(nums, target);
        int last = findLast(nums, target);

        return new int[]{first, last};
    }

    /**
     * Find FIRST occurrence of target
     *
     * Idea:
     *  - If nums[mid] == target:
     *      → store answer
     *      → move LEFT to find earlier occurrence
     */
    private static int findFirst(int[] nums, int target) {

        int low = 0, high = nums.length - 1;
        int answer = -1; // IMPORTANT: explicit answer variable (your preferred style)

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                answer = mid;      // store possible answer
                high = mid - 1;   // move left to find earlier occurrence
            } else if (nums[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return answer;
    }

    /**
     * Find LAST occurrence of target
     *
     * Idea:
     *  - If nums[mid] == target:
     *      → store answer
     *      → move RIGHT to find later occurrence
     */
    private static int findLast(int[] nums, int target) {

        int low = 0, high = nums.length - 1;
        int answer = -1; // explicit answer variable

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                answer = mid;     // store possible answer
                low = mid + 1;   // move right to find later occurrence
            } else if (nums[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return answer;
    }
}

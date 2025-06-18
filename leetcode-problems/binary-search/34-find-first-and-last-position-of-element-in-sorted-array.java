/*
 * Given an array of integers nums sorted in non-decreasing order, find the
 * starting and ending position of a given target value.
 * 
 * If target is not found in the array, return [-1, -1].
 * 
 * You must write an algorithm with O(log n) runtime complexity.
 * 
 * Example 1:
 * Input: nums = [5,7,7,8,8,10], target = 8
 * Output: [3,4]
 * 
 * Example 2:
 * Input: nums = [5,7,7,8,8,10], target = 6
 * Output: [-1,-1]
 * 
 * Example 3:
 * Input: nums = [], target = 0
 * Output: [-1,-1]
 */

class FindFirstLastPosition {

    /**
     * Solution 1: Two Binary Searches (Find First and Last separately)
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int[] searchRange(int[] nums, int target) {
        int[] result = new int[2];
        result[0] = findFirst(nums, target);
        result[1] = findLast(nums, target);
        return result;
    }

    private int findFirst(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;
        int first = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) {
                first = mid;
                right = mid - 1; // Continue searching left for first occurrence
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return first;
    }

    private int findLast(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;
        int last = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) {
                last = mid;
                left = mid + 1; // Continue searching right for last occurrence
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return last;
    }

    /**
     * Solution 2: Using Lower and Upper Bound Binary Search
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int[] searchRangeWithBounds(int[] nums, int target) {
        int leftBound = lowerBound(nums, target);
        int rightBound = upperBound(nums, target);

        if (leftBound == nums.length || nums[leftBound] != target) {
            return new int[] { -1, -1 };
        }

        return new int[] { leftBound, rightBound - 1 };
    }

    // Find the first position where target can be inserted
    private int lowerBound(int[] nums, int target) {
        int left = 0;
        int right = nums.length;

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

    // Find the first position where target+1 can be inserted
    private int upperBound(int[] nums, int target) {
        int left = 0;
        int right = nums.length;

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

    /**
     * Solution 3: Single Binary Search with Expansion
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int[] searchRangeWithExpansion(int[] nums, int target) {
        if (nums.length == 0) {
            return new int[] { -1, -1 };
        }

        // First find any occurrence of target
        int index = binarySearch(nums, target);

        if (index == -1) {
            return new int[] { -1, -1 };
        }

        // Expand to find first and last positions
        int first = index;
        int last = index;

        // Find first position using binary search
        int left = 0;
        int right = index;
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        first = left;

        // Find last position using binary search
        left = index;
        right = nums.length - 1;
        while (left < right) {
            int mid = left + (right - left + 1) / 2; // Upper mid
            if (nums[mid] == target) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }
        last = left;

        return new int[] { first, last };
    }

    private int binarySearch(int[] nums, int target) {
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

        return -1;
    }

    /**
     * Solution 4: Recursive Approach
     * Time Complexity: O(log n)
     * Space Complexity: O(log n) due to recursion
     */
    public int[] searchRangeRecursive(int[] nums, int target) {
        int[] result = new int[2];
        result[0] = findFirstRecursive(nums, target, 0, nums.length - 1);
        result[1] = findLastRecursive(nums, target, 0, nums.length - 1);
        return result;
    }

    private int findFirstRecursive(int[] nums, int target, int left, int right) {
        if (left > right) {
            return -1;
        }

        int mid = left + (right - left) / 2;

        if (nums[mid] == target) {
            int leftResult = findFirstRecursive(nums, target, left, mid - 1);
            return leftResult != -1 ? leftResult : mid;
        } else if (nums[mid] < target) {
            return findFirstRecursive(nums, target, mid + 1, right);
        } else {
            return findFirstRecursive(nums, target, left, mid - 1);
        }
    }

    private int findLastRecursive(int[] nums, int target, int left, int right) {
        if (left > right) {
            return -1;
        }

        int mid = left + (right - left) / 2;

        if (nums[mid] == target) {
            int rightResult = findLastRecursive(nums, target, mid + 1, right);
            return rightResult != -1 ? rightResult : mid;
        } else if (nums[mid] < target) {
            return findLastRecursive(nums, target, mid + 1, right);
        } else {
            return findLastRecursive(nums, target, left, mid - 1);
        }
    }

    /**
     * Solution 5: Optimized Single Pass (Find range in one traversal)
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int[] searchRangeOptimized(int[] nums, int target) {
        return findRange(nums, target, 0, nums.length - 1);
    }

    private int[] findRange(int[] nums, int target, int left, int right) {
        if (left > right) {
            return new int[] { -1, -1 };
        }

        int mid = left + (right - left) / 2;

        if (nums[mid] == target) {
            // Found target, now find the complete range
            int first = mid;
            int last = mid;

            // Expand left to find first occurrence
            while (first > left && nums[first - 1] == target) {
                first--;
            }
            if (first > left) {
                int leftFirst = findFirst(nums, target, left, first - 1);
                if (leftFirst != -1)
                    first = leftFirst;
            }

            // Expand right to find last occurrence
            while (last < right && nums[last + 1] == target) {
                last++;
            }
            if (last < right) {
                int rightLast = findLast(nums, target, last + 1, right);
                if (rightLast != -1)
                    last = rightLast;
            }

            return new int[] { first, last };
        } else if (nums[mid] < target) {
            return findRange(nums, target, mid + 1, right);
        } else {
            return findRange(nums, target, left, mid - 1);
        }
    }

    private int findFirst(int[] nums, int target, int left, int right) {
        int result = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                result = mid;
                right = mid - 1;
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return result;
    }

    private int findLast(int[] nums, int target, int left, int right) {
        int result = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                result = mid;
                left = mid + 1;
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return result;
    }

    /**
     * Test method to demonstrate all solutions
     */
    public static void main(String[] args) {
        FindFirstLastPosition solution = new FindFirstLastPosition();

        // Test Case 1: [5,7,7,8,8,10], target = 8
        int[] nums1 = { 5, 7, 7, 8, 8, 10 };
        int target1 = 8;
        System.out.println("Array: [5,7,7,8,8,10], Target: 8");
        System.out.println("Two Binary Searches: " + java.util.Arrays.toString(solution.searchRange(nums1, target1)));
        System.out.println("With Bounds: " + java.util.Arrays.toString(solution.searchRangeWithBounds(nums1, target1)));
        System.out.println(
                "With Expansion: " + java.util.Arrays.toString(solution.searchRangeWithExpansion(nums1, target1)));
        System.out.println("Recursive: " + java.util.Arrays.toString(solution.searchRangeRecursive(nums1, target1)));
        System.out.println();

        // Test Case 2: [5,7,7,8,8,10], target = 6
        int[] nums2 = { 5, 7, 7, 8, 8, 10 };
        int target2 = 6;
        System.out.println("Array: [5,7,7,8,8,10], Target: 6");
        System.out.println("Two Binary Searches: " + java.util.Arrays.toString(solution.searchRange(nums2, target2)));
        System.out.println("With Bounds: " + java.util.Arrays.toString(solution.searchRangeWithBounds(nums2, target2)));
        System.out.println();

        // Test Case 3: [], target = 0
        int[] nums3 = {};
        int target3 = 0;
        System.out.println("Array: [], Target: 0");
        System.out.println("Two Binary Searches: " + java.util.Arrays.toString(solution.searchRange(nums3, target3)));
        System.out.println("With Bounds: " + java.util.Arrays.toString(solution.searchRangeWithBounds(nums3, target3)));
        System.out.println();

        // Test Case 4: Single element match
        int[] nums4 = { 1 };
        int target4 = 1;
        System.out.println("Array: [1], Target: 1");
        System.out.println("Two Binary Searches: " + java.util.Arrays.toString(solution.searchRange(nums4, target4)));
        System.out.println();

        // Test Case 5: All elements are the same
        int[] nums5 = { 2, 2, 2, 2, 2 };
        int target5 = 2;
        System.out.println("Array: [2,2,2,2,2], Target: 2");
        System.out.println("Two Binary Searches: " + java.util.Arrays.toString(solution.searchRange(nums5, target5)));
    }

}

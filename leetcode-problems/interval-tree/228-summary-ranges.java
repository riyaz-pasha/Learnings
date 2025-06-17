import java.util.*;
/*
 * You are given a sorted unique integer array nums.
 * A range [a,b] is the set of all integers from a to b (inclusive).
 * 
 * Return the smallest sorted list of ranges that cover all the numbers in the
 * array exactly. That is, each element of nums is covered by exactly one of the
 * ranges, and there is no integer x such that x is in one of the ranges but not
 * in nums.
 * 
 * Each range [a,b] in the list should be output as:
 * 
 * "a->b" if a != b
 * "a" if a == b
 * 
 * Example 1:
 * Input: nums = [0,1,2,4,5,7]
 * Output: ["0->2","4->5","7"]
 * Explanation: The ranges are:
 * [0,2] --> "0->2"
 * [4,5] --> "4->5"
 * [7,7] --> "7"
 * 
 * Example 2:
 * Input: nums = [0,2,3,4,6,8,9]
 * Output: ["0","2->4","6","8->9"]
 * Explanation: The ranges are:
 * [0,0] --> "0"
 * [2,4] --> "2->4"
 * [6,6] --> "6"
 * [8,9] --> "8->9"
 */

class SummaryRanges {

    /**
     * Solution 1: Two-pointer approach (Most intuitive)
     * Time Complexity: O(n)
     * Space Complexity: O(1) excluding output
     */
    public List<String> summaryRanges1(int[] nums) {
        List<String> result = new ArrayList<>();
        if (nums == null || nums.length == 0) {
            return result;
        }

        int start = 0;

        for (int end = 0; end < nums.length; end++) {
            // Check if we've reached the end or found a gap
            if (end == nums.length - 1 || nums[end + 1] != nums[end] + 1) {
                // Add range to result
                if (start == end) {
                    result.add(String.valueOf(nums[start]));
                } else {
                    result.add(nums[start] + "->" + nums[end]);
                }
                start = end + 1;
            }
        }

        return result;
    }

    /**
     * Solution 2: Single pass with tracking (Clean and efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(1) excluding output
     */
    public List<String> summaryRanges2(int[] nums) {
        List<String> result = new ArrayList<>();
        if (nums.length == 0)
            return result;

        int rangeStart = nums[0];

        for (int i = 1; i <= nums.length; i++) {
            // Check if we're at the end or there's a gap
            if (i == nums.length || nums[i] != nums[i - 1] + 1) {
                int rangeEnd = nums[i - 1];
                if (rangeStart == rangeEnd) {
                    result.add(String.valueOf(rangeStart));
                } else {
                    result.add(rangeStart + "->" + rangeEnd);
                }

                // Update start for next range
                if (i < nums.length) {
                    rangeStart = nums[i];
                }
            }
        }

        return result;
    }

    /**
     * Solution 3: StringBuilder optimization (For better string performance)
     * Time Complexity: O(n)
     * Space Complexity: O(1) excluding output
     */
    public List<String> summaryRanges3(int[] nums) {
        List<String> result = new ArrayList<>();
        if (nums.length == 0)
            return result;

        int start = 0;

        for (int i = 1; i <= nums.length; i++) {
            if (i == nums.length || nums[i] != nums[i - 1] + 1) {
                StringBuilder sb = new StringBuilder();
                sb.append(nums[start]);

                if (start != i - 1) {
                    sb.append("->").append(nums[i - 1]);
                }

                result.add(sb.toString());
                start = i;
            }
        }

        return result;
    }

    /**
     * Solution 4: Functional approach using streams (Java 8+)
     * Time Complexity: O(n)
     * Space Complexity: O(n) for intermediate collections
     */
    public List<String> summaryRanges4(int[] nums) {
        if (nums.length == 0)
            return new ArrayList<>();

        List<String> result = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < nums.length; i++) {
            if (i == nums.length - 1 || nums[i + 1] - nums[i] > 1) {
                result.add(formatRange(nums[start], nums[i]));
                start = i + 1;
            }
        }

        return result;
    }

    private String formatRange(int start, int end) {
        return start == end ? String.valueOf(start) : start + "->" + end;
    }

    /**
     * Solution 5: Handle edge cases explicitly
     * Time Complexity: O(n)
     * Space Complexity: O(1) excluding output
     */
    public List<String> summaryRanges5(int[] nums) {
        List<String> result = new ArrayList<>();

        // Handle empty array
        if (nums == null || nums.length == 0) {
            return result;
        }

        // Handle single element
        if (nums.length == 1) {
            result.add(String.valueOf(nums[0]));
            return result;
        }

        int left = 0;

        for (int right = 1; right < nums.length; right++) {
            // Check for gap or overflow protection
            if ((long) nums[right] - nums[right - 1] != 1) {
                addRange(result, nums[left], nums[right - 1]);
                left = right;
            }
        }

        // Add the last range
        addRange(result, nums[left], nums[nums.length - 1]);

        return result;
    }

    private void addRange(List<String> result, int start, int end) {
        if (start == end) {
            result.add(String.valueOf(start));
        } else {
            result.add(start + "->" + end);
        }
    }

    // Test cases
    public static void main(String[] args) {
        SummaryRanges solution = new SummaryRanges();

        // Test case 1
        int[] nums1 = { 0, 1, 2, 4, 5, 7 };
        System.out.println("Input: " + Arrays.toString(nums1));
        System.out.println("Output: " + solution.summaryRanges1(nums1));
        System.out.println("Expected: [0->2, 4->5, 7]");
        System.out.println();

        // Test case 2
        int[] nums2 = { 0, 2, 3, 4, 6, 8, 9 };
        System.out.println("Input: " + Arrays.toString(nums2));
        System.out.println("Output: " + solution.summaryRanges1(nums2));
        System.out.println("Expected: [0, 2->4, 6, 8->9]");
        System.out.println();

        // Test case 3: Single element
        int[] nums3 = { 1 };
        System.out.println("Input: " + Arrays.toString(nums3));
        System.out.println("Output: " + solution.summaryRanges1(nums3));
        System.out.println("Expected: [1]");
        System.out.println();

        // Test case 4: Empty array
        int[] nums4 = {};
        System.out.println("Input: " + Arrays.toString(nums4));
        System.out.println("Output: " + solution.summaryRanges1(nums4));
        System.out.println("Expected: []");
        System.out.println();

        // Test case 5: All consecutive
        int[] nums5 = { 1, 2, 3, 4, 5 };
        System.out.println("Input: " + Arrays.toString(nums5));
        System.out.println("Output: " + solution.summaryRanges1(nums5));
        System.out.println("Expected: [1->5]");
        System.out.println();

        // Test case 6: No consecutive numbers
        int[] nums6 = { 1, 3, 5, 7, 9 };
        System.out.println("Input: " + Arrays.toString(nums6));
        System.out.println("Output: " + solution.summaryRanges1(nums6));
        System.out.println("Expected: [1, 3, 5, 7, 9]");
    }

}

class SummaryRanges2 {

    public List<String> summaryRanges(int[] nums) {
        List<String> summary = new ArrayList<>();
        int index = 0;
        while (index < nums.length) {
            StringBuilder str = new StringBuilder();
            str.append(nums[index]);
            int count = 0;
            while (index < nums.length - 1 && nums[index + 1] == (nums[index] + 1)) {
                index++;
                count++;
            }
            if (count > 0) {
                str.append("->").append(nums[index]);
            }
            summary.add(str.toString());
            index++;
        }
        return summary;
    }

}

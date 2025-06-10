import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class LongestIncreasingSubsequence {

    public int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[][] memo = new int[n][n + 1];
        for (int i = 0; i < n; i++) {
            Arrays.fill(memo[i], -1);
        }
        return helper(nums, 0, -1, memo);
    }

    private int helper(int[] nums, int i, int prevIndex, int[][] memo) {
        if (i == nums.length) {
            return 0;
        }
        if (memo[i][prevIndex + 1] != -1) {
            return memo[i][prevIndex + 1];
        }
        int pick = 0;
        if (prevIndex == -1 || nums[i] > nums[prevIndex]) {
            pick = 1 + helper(nums, i + 1, i, memo);
        }
        int skip = helper(nums, i + 1, prevIndex, memo);
        return memo[i][prevIndex] = Math.max(pick, skip);
    }

}

class LongestIncreasingSubsequence2 {

    // time complexity of O(n²) and a space complexity of O(n)
    public int lengthOfLIS2(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int n = nums.length;
        int[] dp = new int[n];
        int maxLength = 1;

        // Every element is a subsequence of length 1 by itself
        Arrays.fill(dp, 1);

        // Build the dp array
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[i] > nums[j]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLength = Math.max(maxLength, dp[i]);
        }

        return maxLength;
    }

    public int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[] lens = new int[n];
        Arrays.fill(lens, 1);

        for (int i = n - 2; i >= 0; i--) {
            for (int j = i + 1; j < n; j++) {
                if (nums[i] < nums[j]) {
                    lens[i] = Math.max(lens[i], 1 + lens[j]);
                }
            }
        }
        int max = 0;
        for (int len : lens) {
            max = Math.max(max, len);
        }
        return max;
    }

}

class LongestIncreasingSubsequence3 {

    // O(n log n) time complexity using Patience Sorting + Binary Search.
    public int lengthOfLIS(int[] nums) {
        List<Integer> sub = new ArrayList<>();

        for (int num : nums) {
            int idx = Collections.binarySearch(sub, num);

            // If not found, binarySearch returns (-(insertion point) - 1)
            if (idx < 0) {
                idx = -(idx + 1);
            }

            // If idx is equal to length, we need to extend the list
            if (idx == sub.size()) {
                sub.add(num);
            } else {
                sub.set(idx, num);
            }
        }

        return sub.size();
    }

    public static void main(String[] args) {
        LongestIncreasingSubsequence lis = new LongestIncreasingSubsequence();
        int[] nums = { 10, 9, 2, 5, 3, 7, 101, 18 };
        System.out.println("Length of LIS: " + lis.lengthOfLIS(nums));
    }

}

public class LongestIncreasingSubsequence4 {

    /**
     * LONGEST INCREASING SUBSEQUENCE - BINARY SEARCH + GREEDY APPROACH
     * Time Complexity: O(n log n), Space Complexity: O(n)
     * 
     * ===============================================================================
     * KEY CONCEPT:
     * ===============================================================================
     * The tails array stores: tails[i] = smallest ending element of all increasing
     * subsequences of length (i+1)
     * 
     * This is crucial because having a smaller tail gives us more opportunities to
     * extend the subsequence later.
     * 
     * ===============================================================================
     * STEP-BY-STEP TRACE WITH EXAMPLE: [10, 9, 2, 5, 3, 7, 101, 18]
     * ===============================================================================
     * 
     * Initial State:
     * - tails = [_, _, _, _, _, _, _, _] (empty)
     * - size = 0
     * 
     * Processing nums[0] = 10:
     * - Binary search in range [0, 0)
     * - No elements to compare, so left = 0
     * - tails[0] = 10, size = 1
     * - State: tails = [10], LIS length = 1
     * 
     * Processing nums[1] = 9:
     * - Binary search in range [0, 1)
     * - Compare with tails[0] = 10: since 10 >= 9, search left
     * - Found position: left = 0
     * - REPLACE tails[0] = 9 (smaller tail is better!)
     * - State: tails = [9], LIS length = 1
     * 
     * Processing nums[2] = 2:
     * - Binary search in range [0, 1)
     * - Compare with tails[0] = 9: since 9 >= 2, search left
     * - Found position: left = 0
     * - REPLACE tails[0] = 2
     * - State: tails = [2], LIS length = 1
     * 
     * Processing nums[3] = 5:
     * - Binary search in range [0, 1)
     * - Compare with tails[0] = 2: since 2 < 5, search right
     * - Found position: left = 1 (extends sequence!)
     * - EXTEND tails[1] = 5, size = 2
     * - State: tails = [2, 5], LIS length = 2
     * 
     * Processing nums[4] = 3:
     * - Binary search in range [0, 2)
     * - mid = 0: tails[0] = 2 < 3, search right half [1, 2)
     * - mid = 1: tails[1] = 5 >= 3, search left half [1, 1)
     * - Found position: left = 1
     * - REPLACE tails[1] = 3 (better tail for length 2!)
     * - State: tails = [2, 3], LIS length = 2
     * 
     * Processing nums[5] = 7:
     * - Binary search in range [0, 2)
     * - mid = 0: tails[0] = 2 < 7, search right
     * - mid = 1: tails[1] = 3 < 7, search right
     * - Found position: left = 2 (extends sequence!)
     * - EXTEND tails[2] = 7, size = 3
     * - State: tails = [2, 3, 7], LIS length = 3
     * 
     * Processing nums[6] = 101:
     * - Binary search in range [0, 3)
     * - All elements are < 101, so we extend
     * - Found position: left = 3
     * - EXTEND tails[3] = 101, size = 4
     * - State: tails = [2, 3, 7, 101], LIS length = 4
     * 
     * Processing nums[7] = 18:
     * - Binary search in range [0, 4)
     * - mid = 1: tails[1] = 3 < 18, search right [2, 4)
     * - mid = 3: tails[3] = 101 >= 18, search left [2, 3)
     * - mid = 2: tails[2] = 7 < 18, search right [3, 3)
     * - Found position: left = 3
     * - REPLACE tails[3] = 18 (smaller tail for length 4!)
     * - State: tails = [2, 3, 7, 18], LIS length = 4
     * 
     * FINAL RESULT: 4
     * 
     * ===============================================================================
     * WHY THIS WORKS:
     * ===============================================================================
     * 1. Greedy Choice: We always try to keep the smallest possible tail for each
     * length
     * 2. Binary Search: The tails array is always sorted, so we can use binary
     * search
     * 3. Optimal Replacement: When we replace an element, we're making future
     * extensions more likely
     * 
     * The actual LIS could be [2, 3, 7, 18] or [2, 3, 7, 101], but we only care
     * about the length, not the actual subsequence.
     * 
     * ===============================================================================
     * ALGORITHM STEPS:
     * ===============================================================================
     * 1. Initialize tails array and size = 0
     * 2. For each number in input:
     * a. Use binary search to find the leftmost position where we can place the
     * number
     * b. If position == size, we extend the sequence (increment size)
     * c. Otherwise, we replace the element at that position with current number
     * 3. Return size (length of LIS)
     * 
     * ===============================================================================
     * BINARY SEARCH LOGIC:
     * ===============================================================================
     * - We're looking for the leftmost position where tails[pos] >= current_number
     * - If tails[mid] < current_number: search right half
     * - If tails[mid] >= current_number: search left half
     * - This gives us the position to either extend or replace
     */
    public int lengthOfLIS_BinarySearch(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        // tails[i] stores the smallest tail for LIS of length i+1
        int[] tails = new int[nums.length];
        int size = 0;

        for (int num : nums) {
            // Binary search for the position to insert/replace
            int left = 0, right = size;
            while (left < right) {
                int mid = left + (right - left) / 2;
                if (tails[mid] < num) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }

            // Insert or replace
            tails[left] = num;
            if (left == size) {
                size++;
            }
        }

        return size;
    }

    /**
     * Solution 3: Using Collections.binarySearch() - O(n log n) time
     * Cleaner implementation using Java's built-in binary search
     */
    public int lengthOfLIS_Collections(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        java.util.List<Integer> tails = new java.util.ArrayList<>();

        for (int num : nums) {
            int pos = java.util.Collections.binarySearch(tails, num);

            // If not found, binarySearch returns -(insertion point) - 1
            if (pos < 0) {
                pos = -(pos + 1);
            }

            // If pos equals size, we're extending the sequence
            if (pos == tails.size()) {
                tails.add(num);
            } else {
                // Replace the element at pos with smaller value
                tails.set(pos, num);
            }
        }

        return tails.size();
    }

}

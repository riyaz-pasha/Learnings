import java.util.HashMap;
import java.util.Map;

class LongestSubsequenceOfGivenDifference {

    // Time: O(n) — we traverse arr once and do constant-time work per element.
    // Space: O(n) — for the HashMap storing intermediate results.
    public int longestSubsequence(int[] arr, int difference) {
        Map<Integer, Integer> dp = new HashMap<>();
        int maxLength = 0;

        for (int num : arr) {
            int prev = num - difference;
            int len = dp.getOrDefault(prev, 0) + 1;
            dp.put(num, len);
            maxLength = Math.max(maxLength, len);
        }

        return maxLength;
    }

}




/**
 * LONGEST ARITHMETIC SUBSEQUENCE WITH GIVEN DIFFERENCE
 * 
 * Given an integer array arr and an integer difference, return the length of the 
 * longest subsequence in arr which is an arithmetic sequence such that the difference 
 * between adjacent elements in the subsequence equals difference.
 * 
 * ===============================================================================
 * KEY INSIGHT:
 * ===============================================================================
 * For each element arr[i], we need to find if there exists an element (arr[i] - difference)
 * that appeared before it. If yes, we can extend that arithmetic sequence.
 * 
 * We use a HashMap to store: key = element value, value = length of longest 
 * arithmetic sequence ending at that element.
 * 
 * ===============================================================================
 * ALGORITHM APPROACH:
 * ===============================================================================
 * 1. Use HashMap<Integer, Integer> where:
 *    - Key: element value
 *    - Value: length of longest arithmetic subsequence ending with this element
 * 
 * 2. For each element arr[i]:
 *    - Check if (arr[i] - difference) exists in map
 *    - If yes: dp[arr[i]] = dp[arr[i] - difference] + 1
 *    - If no: dp[arr[i]] = 1 (start new sequence)
 *    - Update maximum length
 * 
 * ===============================================================================
 * EXAMPLE TRACE: arr = [1, 2, 3, 4], difference = 1
 * ===============================================================================
 * 
 * Initial: dp = {}, maxLen = 0
 * 
 * Process arr[0] = 1:
 * - Look for (1 - 1) = 0 in dp: NOT FOUND
 * - dp[1] = 1
 * - maxLen = max(0, 1) = 1
 * - State: dp = {1: 1}, maxLen = 1
 * 
 * Process arr[1] = 2:
 * - Look for (2 - 1) = 1 in dp: FOUND dp[1] = 1
 * - dp[2] = dp[1] + 1 = 1 + 1 = 2
 * - maxLen = max(1, 2) = 2
 * - State: dp = {1: 1, 2: 2}, maxLen = 2
 * 
 * Process arr[2] = 3:
 * - Look for (3 - 1) = 2 in dp: FOUND dp[2] = 2
 * - dp[3] = dp[2] + 1 = 2 + 1 = 3
 * - maxLen = max(2, 3) = 3
 * - State: dp = {1: 1, 2: 2, 3: 3}, maxLen = 3
 * 
 * Process arr[3] = 4:
 * - Look for (4 - 1) = 3 in dp: FOUND dp[3] = 3
 * - dp[4] = dp[3] + 1 = 3 + 1 = 4
 * - maxLen = max(3, 4) = 4
 * - State: dp = {1: 1, 2: 2, 3: 3, 4: 4}, maxLen = 4
 * 
 * Result: 4 (sequence: [1, 2, 3, 4])
 * 
 * ===============================================================================
 * EXAMPLE TRACE: arr = [1, 3, 5, 7], difference = 1
 * ===============================================================================
 * 
 * Process arr[0] = 1: dp[1] = 1, maxLen = 1
 * Process arr[1] = 3: Look for 2, NOT FOUND, dp[3] = 1, maxLen = 1
 * Process arr[2] = 5: Look for 4, NOT FOUND, dp[5] = 1, maxLen = 1
 * Process arr[3] = 7: Look for 6, NOT FOUND, dp[7] = 1, maxLen = 1
 * 
 * Result: 1 (no arithmetic sequence possible with difference = 1)
 * 
 * ===============================================================================
 * EXAMPLE TRACE: arr = [1, 5, 7, 8, 5, 3, 4, 2, 1], difference = -2
 * ===============================================================================
 * 
 * Process arr[0] = 1: dp[1] = 1, maxLen = 1
 * Process arr[1] = 5: Look for 7, NOT FOUND, dp[5] = 1, maxLen = 1
 * Process arr[2] = 7: Look for 9, NOT FOUND, dp[7] = 1, maxLen = 1
 * Process arr[3] = 8: Look for 10, NOT FOUND, dp[8] = 1, maxLen = 1
 * Process arr[4] = 5: Already exists, dp[5] = max(1, 1) = 1, maxLen = 1
 * Process arr[5] = 3: Look for 5, FOUND dp[5] = 1, dp[3] = 2, maxLen = 2
 * Process arr[6] = 4: Look for 6, NOT FOUND, dp[4] = 1, maxLen = 2
 * Process arr[7] = 2: Look for 4, FOUND dp[4] = 1, dp[2] = 2, maxLen = 2
 * Process arr[8] = 1: Look for 3, FOUND dp[3] = 2, dp[1] = 3, maxLen = 3
 * 
 * Result: 4 (sequence could be [7, 5, 3, 1] with difference = -2)
 */

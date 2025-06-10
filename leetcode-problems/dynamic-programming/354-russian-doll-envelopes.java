import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RussianDollEnvelopes {

    // Approach 1: Direct 2D DP - O(n^2) time, O(n) space
    public int maxEnvelopes(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) {
            return 0;
        }

        int n = envelopes.length;

        // Sort by width ascending, and if widths are equal, sort by height ascending
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return a[1] - b[1]; // height ascending when widths are equal
            }
            return a[0] - b[0]; // width ascending
        });

        // dp[i] represents the maximum number of envelopes ending at index i
        int[] dp = new int[n];
        Arrays.fill(dp, 1); // Each envelope can form a sequence of length 1

        int maxCount = 1;

        // For each envelope, check all previous envelopes
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // Check if envelope j can fit into envelope i
                if (envelopes[j][0] < envelopes[i][0] && envelopes[j][1] < envelopes[i][1]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxCount = Math.max(maxCount, dp[i]);
        }

        return maxCount;
    }

    // Approach 2: DP with LIS optimization - O(n log n) time
    public int maxEnvelopesOptimized(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) {
            return 0;
        }

        // Sort by width ascending, and if widths are equal, sort by height descending
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return b[1] - a[1]; // height descending when widths are equal
            }
            return a[0] - b[0]; // width ascending
        });

        // Extract heights and find LIS using DP with binary search
        int[] heights = new int[envelopes.length];
        for (int i = 0; i < envelopes.length; i++) {
            heights[i] = envelopes[i][1];
        }

        return lengthOfLIS(heights);
    }

    // DP-based LIS with binary search - O(n log n)
    private int lengthOfLIS(int[] nums) {
        List<Integer> tails = new ArrayList<>();

        for (int num : nums) {
            int pos = binarySearch(tails, num);
            if (pos == tails.size()) {
                tails.add(num);
            } else {
                tails.set(pos, num);
            }
        }

        return tails.size();
    }

    // Binary search to find the position where num should be inserted
    private int binarySearch(List<Integer> tails, int target) {
        int left = 0, right = tails.size();

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (tails.get(mid) < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

}

// Step 1: Sort the envelopes
// Sort by width in ascending order.
// If two envelopes have the same width, sort by height in descending order.
// This prevents envelopes with the same width from being incorrectly nested by height.

// Example Input: [[5,4], [6,4], [6,7], [2,3]]
// After sorting: [[2,3], [5,4], [6,7], [6,4]]
// Notice that for width 6, (6,7) comes before (6,4) because of height descending.

// Step 2: Extract the heights to apply Longest Increasing Subsequence (LIS) on them.
// heights = [3, 4, 7, 4]

// Step 3: Initialize an array 'lis' to store the end elements of potential increasing subsequences.
// Initialize length = 0

// Iterate through each height:
// 1. height = 3
//    - lis is empty. Place 3 at index 0.
//    - lis = [3], length = 1

// 2. height = 4
//    - 4 > last element in lis (3). Append 4.
//    - lis = [3, 4], length = 2

// 3. height = 7
//    - 7 > last element in lis (4). Append 7.
//    - lis = [3, 4, 7], length = 3

// 4. height = 4
//    - 4 is not greater than last element in lis (7).
//    - Binary search gives index 1 (4 replaces existing 4 to keep LIS minimal).
//    - lis remains [3, 4, 7], length = 3

// Final length of LIS is 3 → Maximum envelopes that can be Russian dolled = 3

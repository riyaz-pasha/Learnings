/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given a 2D array of 'envelopes', where each envelope has a width and a 
 * height. We can nest one envelope inside another ONLY IF both its width and 
 * its height are strictly smaller than the other envelope's width and height.
 * 
 * We need to find the maximum number of envelopes we can Russian-doll (nest) 
 * inside one another.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can we rotate the envelopes? (i.e., swap width and height)
 *    (Usually, the answer is no for this specific problem unless specified. We will 
 *    assume no rotation is allowed).
 * 2. Does "strictly smaller" mean we can't fit a 5x5 into a 5x6?
 *    (Correct. The inner envelope's width must be < outer's width, AND inner's 
 *    height must be < outer's height. Equality is not allowed).
 * 3. Can the given array be empty?
 *    (Constraints say length >= 1, so there is at least one envelope. Base case 
 *    can just return 1 if length is 1).
 * 4. Can widths or heights be negative or zero?
 *    (Constraints say 1 <= w, h <= 10^4, so all are strictly positive).
 * 5. Are there duplicates in the input?
 *    (Yes, there could be identical envelopes. Since we need strict inequalities, 
 *    we can only pick one of them in any nested sequence).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * This problem is a 2D version of the classic Longest Increasing Subsequence (LIS) 
 * problem. If we only had one dimension (e.g., just heights), we could just find 
 * the LIS. But we have two dimensions.
 * 
 * 1. The Sorting Trick (Crucial Insight):
 *    - To reduce this to a 1D LIS problem, we must sort the envelopes.
 *    - Sort them by WIDTH in ASCENDING order. This guarantees that as we iterate 
 *      through the sorted array, the width is generally increasing.
 *    - However, what if two envelopes have the SAME width? (e.g., [3, 4] and [3, 5]). 
 *      Since they have the same width, one cannot fit inside the other. 
 *      If we sort heights in ascending order too ([3, 4] then [3, 5]), our LIS 
 *      algorithm might incorrectly pick both because 4 < 5.
 *    - THE FIX: If widths are equal, sort the HEIGHTS in DESCENDING order 
 *      ([3, 5] then [3, 4]). Now, the heights go down. An LIS algorithm looking 
 *      for strictly increasing heights will naturally only be able to pick ONE 
 *      envelope from this identical-width group!
 * 
 * 2. Dynamic Programming (O(N^2)):
 *    - Sort the array as described above.
 *    - Create a dp array where dp[i] is the max envelopes ending at index i.
 *    - For each envelope i, check all previous envelopes j. If heights[j] < heights[i], 
 *      then dp[i] = max(dp[i], dp[j] + 1).
 *    - Time: O(N^2). With N = 1000, N^2 = 1,000,000, which will easily pass.
 * 
 * 3. Binary Search / Patience Sorting (O(N log N)) - Optimal:
 *    - Sort the array as described above.
 *    - We only need to find the LIS of the extracted heights array.
 *    - Maintain an active 'tails' array for the LIS. 
 *    - Iterate through the heights. Use Binary Search to find the position to 
 *      replace in the 'tails' array. If it's larger than all elements, append it.
 *    - Time: O(N log N). Space: O(N).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Binary Search Approach)
 * ============================================================================
 * Example: envelopes = [[5,4], [6,4], [6,7], [2,3]]
 * 
 * 1. Sort the envelopes:
 *    - Sort by width ASC. If widths equal, sort height DESC.
 *    - [2,3]
 *    - [5,4]
 *    - [6,7] and [6,4] -> Widths equal, so height DESC -> [6,7], then [6,4].
 *    - Sorted Array: [[2,3], [5,4], [6,7], [6,4]]
 * 
 * 2. Extract heights: [3, 4, 7, 4]
 * 
 * 3. Find LIS of heights [3, 4, 7, 4]:
 *    - Initialize tails = []
 *    - h = 3: tails = [3]
 *    - h = 4: 4 > 3, append. tails = [3, 4]
 *    - h = 7: 7 > 4, append. tails = [3, 4, 7]
 *    - h = 4: Binary search 4 in [3, 4, 7]. It replaces 4 (or 7 depending on 
 *             strict increasing). Since we want strictly increasing, it replaces 4.
 *             tails = [3, 4, 7]
 * 
 * 4. The length of tails is 3. Max envelopes = 3.
 */

import java.util.Arrays;

public class RussianDollEnvelopes {

    public static void main(String[] args) {
        int[][] envelopes1 = {{5, 4}, {6, 4}, {6, 7}, {2, 3}};
        int[][] envelopes2 = {{1, 1}, {1, 1}, {1, 1}};
        int[][] envelopes3 = {{4, 5}, {4, 6}, {6, 7}, {2, 3}, {1, 1}};

        System.out.println("Test Case 1: [[5,4], [6,4], [6,7], [2,3]]");
        System.out.println("DP Approach:     " + maxEnvelopesDP(clone2DArray(envelopes1)));
        System.out.println("Optimal (NlogN): " + maxEnvelopesOptimal(clone2DArray(envelopes1)));
        System.out.println("--------------------------------------------------");

        System.out.println("Test Case 2: [[1,1], [1,1], [1,1]]");
        System.out.println("DP Approach:     " + maxEnvelopesDP(clone2DArray(envelopes2)));
        System.out.println("Optimal (NlogN): " + maxEnvelopesOptimal(clone2DArray(envelopes2)));
        System.out.println("--------------------------------------------------");

        System.out.println("Test Case 3: [[4,5], [4,6], [6,7], [2,3], [1,1]]");
        System.out.println("DP Approach:     " + maxEnvelopesDP(clone2DArray(envelopes3)));
        System.out.println("Optimal (NlogN): " + maxEnvelopesOptimal(clone2DArray(envelopes3)));
    }

    /**
     * SOLUTION 1: Sorting + Dynamic Programming
     * 
     * Idea: Sort by width ascending, height descending. Then apply the standard 
     * O(N^2) Longest Increasing Subsequence (LIS) algorithm on the heights.
     * 
     * Time Complexity: O(N^2) - Nested loop for DP.
     * Space Complexity: O(N) - To store the DP array.
     */
    public static int maxEnvelopesDP(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) return 0;

        // Custom sort: Width ASC, Height DESC
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return Integer.compare(b[1], a[1]); // Descending heights
            }
            return Integer.compare(a[0], b[0]);     // Ascending widths
        });

        int n = envelopes.length;
        int[] dp = new int[n];
        Arrays.fill(dp, 1); // Each envelope is a sequence of length 1 by itself
        int maxLen = 1;

        // Standard O(N^2) LIS on heights
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (envelopes[i][1] > envelopes[j][1]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }

        return maxLen;
    }

    /**
     * SOLUTION 2: Sorting + Binary Search (Patience Sorting LIS) - Most Optimal
     * 
     * Idea: Sort by width ascending, height descending. Extract the heights and 
     * find the Longest Increasing Subsequence using Binary Search in O(N log N) time.
     * 
     * Time Complexity: O(N log N) - Sorting takes O(N log N), Binary search takes O(N log N).
     * Space Complexity: O(N) - To store the tails array for LIS.
     */
    public static int maxEnvelopesOptimal(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) return 0;

        // Custom sort: Width ASC, Height DESC
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return Integer.compare(b[1], a[1]);
            }
            return Integer.compare(a[0], b[0]);
        });

        int n = envelopes.length;
        int[] tails = new int[n];
        int size = 0; // Tracks the length of the LIS

        // Apply O(N log N) LIS on the heights
        for (int[] env : envelopes) {
            int height = env[1];

            // Binary search for the insertion point of 'height' in the 'tails' array
            int left = 0;
            int right = size;

            while (left < right) {
                int mid = left + (right - left) / 2;
                if (tails[mid] < height) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }

            // 'left' is the insertion point. 
            // Replace the element or append if it's larger than all elements in tails
            tails[left] = height;

            // If we appended to the end, the LIS size grows
            if (left == size) {
                size++;
            }
        }

        return size;
    }

    /**
     * Utility method to deeply clone a 2D array for testing so that in-place 
     * sorting in one solution doesn't affect the input for the next solution.
     */
    private static int[][] clone2DArray(int[][] original) {
        int[][] cloned = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            cloned[i] = original[i].clone();
        }
        return cloned;
    }
}

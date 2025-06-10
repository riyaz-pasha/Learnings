import java.util.Arrays;
import java.util.Comparator;

class MaxLenOfPairChain {

    public int findLongestChain(int[][] pairs) {
        int n = pairs.length;
        Arrays.sort(pairs, (a, b) -> Integer.compare(a[1], b[1]));

        int len = 1;
        int lastEnd = pairs[0][1];
        for (int i = 1; i < n; i++) {
            if (lastEnd < pairs[i][0]) {
                len++;
                lastEnd = pairs[i][1];
            }
        }
        return len;
    }

    // Time: O(n log n) — due to sorting.
    // Space: O(1) — no extra memory used.
    public int findLongestChain2(int[][] pairs) {
        // Sort by end value (right side of the pair)
        Arrays.sort(pairs, Comparator.comparingInt(a -> a[1]));

        int currentEnd = Integer.MIN_VALUE;
        int chainLength = 0;

        for (int[] pair : pairs) {
            if (pair[0] > currentEnd) {
                chainLength++;
                currentEnd = pair[1];
            }
        }

        return chainLength;
    }

    // Time: O(n²) — due to recursion with memoization.
    // Space: O(n²) for memoization table + O(n) recursion stack.
    public int findLongestChain3(int[][] pairs) {
        // Sort pairs by starting value
        Arrays.sort(pairs, Comparator.comparingInt(a -> a[0]));

        int n = pairs.length;
        // memo[i] = longest chain starting from index i
        Integer[][] memo = new Integer[n][n + 1]; // prevIdx can be -1 to n-1

        return dfs(0, -1, pairs, memo);
    }

    private int dfs(int currIdx, int prevIdx, int[][] pairs, Integer[][] memo) {
        if (currIdx == pairs.length) {
            return 0;
        }

        if (memo[currIdx][prevIdx + 1] != null) {
            return memo[currIdx][prevIdx + 1];
        }

        // Option 1: Skip current pair
        int skip = dfs(currIdx + 1, prevIdx, pairs, memo);

        // Option 2: Take current pair if valid
        int take = 0;
        if (prevIdx == -1 || pairs[prevIdx][1] < pairs[currIdx][0]) {
            take = 1 + dfs(currIdx + 1, currIdx, pairs, memo);
        }

        memo[currIdx][prevIdx + 1] = Math.max(skip, take);
        return memo[currIdx][prevIdx + 1];
    }

}

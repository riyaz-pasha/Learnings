import java.util.ArrayList;
import java.util.List;

class TriangleMinPathSum {

    // Time: O(n^2) — visiting each element once
    // Space: O(n) — storing one row at a time (1D DP)
    public int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();
        int[] dp = new int[n];

        // Start with the last row
        for (int i = 0; i < n; i++) {
            dp[i] = triangle.get(n - 1).get(i);
        }

        // Bottom-up DP
        for (int layer = n - 2; layer >= 0; layer--) {
            for (int i = 0; i <= layer; i++) {
                dp[i] = triangle.get(layer).get(i) + Math.min(dp[i], dp[i + 1]);
            }
        }

        return dp[0]; // top element now contains the minimum path sum
    }

    public int minimumTotal3(List<List<Integer>> triangle) {
        int n = triangle.size();
        List<Integer> lastRow = new ArrayList<>(triangle.getLast());
        List<Integer> current;
        int len;
        for (int row = n - 2; row >= 0; row--) {
            current = triangle.get(row);
            len = current.size();
            for (int i = 0; i < len; i++) {
                lastRow.set(i, current.get(i) + Math.min(lastRow.get(i), lastRow.get(i + 1)));
            }
        }
        return lastRow.get(0);
    }

    public int minimumTotal2(List<List<Integer>> triangle) {
        int n = triangle.size();
        List<List<Integer>> pathSums = new ArrayList<>();
        pathSums.add(triangle.get(0));

        for (int i = 1; i < n; i++) {
            List<Integer> currentRow = triangle.get(i);
            List<Integer> prevRow = pathSums.get(i - 1);
            List<Integer> row = new ArrayList<>();
            row.add(currentRow.get(0) + prevRow.get(0));
            int len = triangle.get(i).size();
            for (int col = 1; col < len - 1; col++) {
                row.add(currentRow.get(col) + Math.min(prevRow.get(col - 1), prevRow.get(col)));
            }
            if (len - 1 >= 0) {
                row.add(currentRow.get(len - 1) + prevRow.get(len - 1));
            }
            pathSums.add(row);
        }
        int min = Integer.MAX_VALUE;
        for (Integer sum : pathSums.get(pathSums.size() - 1)) {
            if (sum < min)
                min = sum;
        }
        return min;
    }

}

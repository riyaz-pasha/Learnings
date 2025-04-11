import java.util.ArrayList;
import java.util.List;

class TriangleMinPathSum {
    public int minimumTotal(List<List<Integer>> triangle) {
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

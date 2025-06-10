import java.util.Arrays;

class FindLongestValidObstacleCourse {

    // O(n log n)
    public int[] longestObstacleCourseAtEachPosition(int[] obstacles) {
        int n = obstacles.length;
        int[] result = new int[n];
        int[] lis = new int[n];
        int len = 0;

        for (int i = 0; i < n; i++) {
            int idx = binarySearch(lis, 0, len, obstacles[i]);
            lis[idx] = obstacles[i];
            if (idx == len) {
                len++;
            }
            result[i] = idx + 1;
        }
        return result;
    }

    // Binary search to find first position in lis where value > target
    private int binarySearch(int[] arr, int left, int right, int target) {
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] <= target) { // allow equal values for non-decreasing sequence
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }

    public int[] longestObstacleCourseAtEachPosition2(int[] obstacles) {
        int n = obstacles.length;
        int[] result = new int[n];
        int[] lis = new int[n]; // Stores the smallest tail of all non-decreasing subsequences
        int len = 0;

        for (int i = 0; i < n; i++) {
            int obstacle = obstacles[i];
            // Find the index where the current obstacle can be inserted
            // to maintain a non-decreasing order. binarySearch returns
            // -(insertion point) - 1 if the element is not found.
            int idx = Arrays.binarySearch(lis, 0, len, obstacle);

            if (idx < 0) {
                // Element not found, get the insertion point
                idx = -(idx + 1);
            }

            // Place the current obstacle at the found index
            lis[idx] = obstacle;

            // If the insertion point is at the end of the current LIS,
            // it means we've extended the longest non-decreasing subsequence.
            if (idx == len) {
                len++;
            }

            // The length of the longest non-decreasing subsequence ending at
            // the current position is the insertion index + 1.
            result[i] = idx + 1;
        }

        return result;
    }

    // Time complexity: O(n²).
    // Space complexity: O(n).
    public int[] longestObstacleCourseAtEachPosition3(int[] obstacles) {
        int n = obstacles.length;
        int[] ans = new int[n];

        // For each position i, find the longest non-decreasing subsequence ending at i
        for (int i = 0; i < n; i++) {
            ans[i] = 1; // At minimum, the course at i includes the obstacle i itself
            for (int j = 0; j < i; j++) {
                if (obstacles[j] <= obstacles[i]) {
                    // If obstacles[j] is less than or equal, we can extend subsequence ending at j
                    ans[i] = Math.max(ans[i], ans[j] + 1);
                }
            }
        }

        return ans;
    }

}

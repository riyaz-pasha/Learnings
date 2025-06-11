import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

class PerfectSquares {

    // Time: O(n × √n)
    // Space: O(n)
    public int numSquares(int n) {
        int[] dp = new int[n + 1];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0;

        for (int num = 1; num <= n; num++) {
            for (int i = 1; (i * i) <= num; i++) {
                dp[num] = Math.min(dp[num], 1 + dp[num - (i * i)]);
            }
        }
        return dp[n];
    }

}

class PerfectSquaresBFS {

    public int numSquares(int n) {
        if (n <= 0)
            return 0;

        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[n + 1];
        queue.offer(0);
        visited[0] = true;
        int level = 0;

        while (!queue.isEmpty()) {
            level++; // One more step added
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int curr = queue.poll();

                // Try all square numbers from 1^2 up to the point we don't exceed n
                for (int j = 1; j * j <= n; j++) {
                    int next = curr + j * j;
                    if (next == n)
                        return level;
                    if (next > n)
                        break;

                    if (!visited[next]) {
                        visited[next] = true;
                        queue.offer(next);
                    }
                }
            }
        }

        return -1; // Should never happen
    }

}
// 🔍 Example
// For n = 13:
// - Level 1: 1, 4, 9
// - Level 2: 1+4=5, 4+4=8, 9+1=10, etc.
// - Eventually, 4+9 = 13 → found at level 2
// Output: 2
// 🧠 Time & Space Complexity
// Time Complexity: ~O(n × √n), each level may enqueue many numbers.
// Space Complexity: O(n) for the visited array and queue.

class PerfectSquaresBFS2 {

    public int numSquares(int n) {
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.offer(n);
        int level = 0;

        int currentNum, size, nextNum;
        while (!queue.isEmpty()) {
            size = queue.size();
            level++;
            for (int i = 0; i < size; i++) {
                currentNum = queue.poll();
                for (int j = 1; (j * j) <= currentNum; j++) {
                    nextNum = currentNum - (j * j);
                    if (nextNum == 0)
                        return level;
                    if (!visited.contains(nextNum)) {
                        queue.offer(nextNum);
                        visited.add(nextNum);
                    }
                }
            }
        }
        return level;
    }

}

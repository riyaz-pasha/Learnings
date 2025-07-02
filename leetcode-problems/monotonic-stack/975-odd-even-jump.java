import java.util.Arrays;
import java.util.Stack;
import java.util.TreeMap;
/*
 * You are given an integer array arr. From some starting index, you can make a
 * series of jumps. The (1st, 3rd, 5th, ...) jumps in the series are called
 * odd-numbered jumps, and the (2nd, 4th, 6th, ...) jumps in the series are
 * called even-numbered jumps. Note that the jumps are numbered, not the
 * indices.
 * 
 * You may jump forward from index i to index j (with i < j) in the following
 * way:
 * 
 * During odd-numbered jumps (i.e., jumps 1, 3, 5, ...), you jump to the index j
 * such that arr[i] <= arr[j] and arr[j] is the smallest possible value. If
 * there are multiple such indices j, you can only jump to the smallest such
 * index j.
 * During even-numbered jumps (i.e., jumps 2, 4, 6, ...), you jump to the index
 * j such that arr[i] >= arr[j] and arr[j] is the largest possible value. If
 * there are multiple such indices j, you can only jump to the smallest such
 * index j.
 * It may be the case that for some index i, there are no legal jumps.
 * A starting index is good if, starting from that index, you can reach the end
 * of the array (index arr.length - 1) by jumping some number of times (possibly
 * 0 or more than once).
 * 
 * Return the number of good starting indices.
 * 
 * Example 1:
 * Input: arr = [10,13,12,14,15]
 * Output: 2
 * Explanation:
 * From starting index i = 0, we can make our 1st jump to i = 2 (since arr[2] is
 * the smallest among arr[1], arr[2], arr[3], arr[4] that is greater or equal to
 * arr[0]), then we cannot jump any more.
 * From starting index i = 1 and i = 2, we can make our 1st jump to i = 3, then
 * we cannot jump any more.
 * From starting index i = 3, we can make our 1st jump to i = 4, so we have
 * reached the end.
 * From starting index i = 4, we have reached the end already.
 * In total, there are 2 different starting indices i = 3 and i = 4, where we
 * can reach the end with some number of
 * jumps.
 * 
 * Example 2:
 * Input: arr = [2,3,1,1,4]
 * Output: 3
 * Explanation:
 * From starting index i = 0, we make jumps to i = 1, i = 2, i = 3:
 * During our 1st jump (odd-numbered), we first jump to i = 1 because arr[1] is
 * the smallest value in [arr[1], arr[2], arr[3], arr[4]] that is greater than
 * or equal to arr[0].
 * During our 2nd jump (even-numbered), we jump from i = 1 to i = 2 because
 * arr[2] is the largest value in [arr[2], arr[3], arr[4]] that is less than or
 * equal to arr[1]. arr[3] is also the largest value, but 2 is a smaller index,
 * so we can only jump to i = 2 and not i = 3
 * During our 3rd jump (odd-numbered), we jump from i = 2 to i = 3 because
 * arr[3] is the smallest value in [arr[3], arr[4]] that is greater than or
 * equal to arr[2].
 * We can't jump from i = 3 to i = 4, so the starting index i = 0 is not good.
 * In a similar manner, we can deduce that:
 * From starting index i = 1, we jump to i = 4, so we reach the end.
 * From starting index i = 2, we jump to i = 3, and then we can't jump anymore.
 * From starting index i = 3, we jump to i = 4, so we reach the end.
 * From starting index i = 4, we are already at the end.
 * In total, there are 3 different starting indices i = 1, i = 3, and i = 4,
 * where we can reach the end with some
 * number of jumps.
 * 
 * Example 3:
 * Input: arr = [5,1,3,4,2]
 * Output: 3
 * Explanation: We can reach the end from starting indices 1, 2, and 4.
 */

class OddEvenJumps {

    public int oddEvenJumps(int[] arr) {
        int n = arr.length;

        // dp[i][0] = can reach end from index i starting with odd jump
        // dp[i][1] = can reach end from index i starting with even jump
        boolean[][] dp = new boolean[n][2];

        // Base case: last index can always reach end
        dp[n - 1][0] = dp[n - 1][1] = true;

        // Precompute next jump positions for odd and even jumps
        int[] nextOdd = new int[n]; // next position for odd jump from each index
        int[] nextEven = new int[n]; // next position for even jump from each index

        // Initialize with -1 (no valid jump)
        Arrays.fill(nextOdd, -1);
        Arrays.fill(nextEven, -1);

        // Build nextOdd using monotonic stack
        // For odd jumps: find smallest value >= current that comes after current index
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for (int i = n - 1; i >= 0; i--) {
            // Find ceiling (smallest key >= arr[i])
            Integer key = map.ceilingKey(arr[i]);
            if (key != null) {
                nextOdd[i] = map.get(key);
            }
            map.put(arr[i], i);
        }

        // Build nextEven using monotonic stack
        // For even jumps: find largest value <= current that comes after current index
        map.clear();
        for (int i = n - 1; i >= 0; i--) {
            // Find floor (largest key <= arr[i])
            Integer key = map.floorKey(arr[i]);
            if (key != null) {
                nextEven[i] = map.get(key);
            }
            map.put(arr[i], i);
        }

        // Fill DP table from right to left
        for (int i = n - 2; i >= 0; i--) {
            // Can reach end with odd jump if next odd jump leads to position
            // where we can reach end with even jump
            if (nextOdd[i] != -1) {
                dp[i][0] = dp[nextOdd[i]][1];
            }

            // Can reach end with even jump if next even jump leads to position
            // where we can reach end with odd jump
            if (nextEven[i] != -1) {
                dp[i][1] = dp[nextEven[i]][0];
            }
        }

        // Count starting indices where we can reach end (starting with odd jump)
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (dp[i][0]) {
                count++;
            }
        }

        return count;
    }

}

class OddEvenJump {

    public int oddEvenJumps(int[] arr) {
        int n = arr.length;
        int[] oddNext = computeNextIndices(arr, true);
        int[] evenNext = computeNextIndices(arr, false);

        boolean[] canReachOdd = new boolean[n];
        boolean[] canReachEven = new boolean[n];
        canReachOdd[n - 1] = true;
        canReachEven[n - 1] = true;

        for (int i = n - 2; i >= 0; i--) {
            if (oddNext[i] != -1)
                canReachOdd[i] = canReachEven[oddNext[i]];
            if (evenNext[i] != -1)
                canReachEven[i] = canReachOdd[evenNext[i]];
        }

        int count = 0;
        for (boolean b : canReachOdd) {
            if (b)
                count++;
        }
        return count;
    }

    // Helper to compute next greater-or-equal (odd) or smaller-or-equal (even)
    // jumps
    private int[] computeNextIndices(int[] arr, boolean isOdd) {
        int n = arr.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++)
            idx[i] = i;

        // Sort by value (ascending for odd jumps, descending for even jumps)
        Arrays.sort(idx, (i, j) -> {
            if (arr[i] != arr[j]) {
                return isOdd ? Integer.compare(arr[i], arr[j]) : Integer.compare(arr[j], arr[i]);
            } else {
                return Integer.compare(i, j); // break ties by index
            }
        });

        int[] result = new int[n];
        Arrays.fill(result, -1);
        Stack<Integer> stack = new Stack<>();

        for (int i : idx) {
            while (!stack.isEmpty() && i > stack.peek()) {
                result[stack.pop()] = i;
            }
            stack.push(i);
        }

        return result;
    }

    // Example usage
    public static void main(String[] args) {
        OddEvenJump solver = new OddEvenJump();

        int[] arr1 = { 10, 13, 12, 14, 15 };
        System.out.println("Output: " + solver.oddEvenJumps(arr1)); // Output: 2

        int[] arr2 = { 2, 3, 1, 1, 4 };
        System.out.println("Output: " + solver.oddEvenJumps(arr2)); // Output: 3

        int[] arr3 = { 5, 1, 3, 4, 2 };
        System.out.println("Output: " + solver.oddEvenJumps(arr3)); // Output: 3
    }

}

/*
 * ALTERNATIVE SOLUTION USING STACKS (More Complex but Educational):
 * 
 * class Solution {
 * public int oddEvenJumps(int[] arr) {
 * int n = arr.length;
 * boolean[] canReachOdd = new boolean[n]; // can reach end starting with odd
 * jump
 * boolean[] canReachEven = new boolean[n]; // can reach end starting with even
 * jump
 * 
 * // Base case
 * canReachOdd[n-1] = canReachEven[n-1] = true;
 * 
 * // Create array of indices sorted by value for odd jumps (ascending)
 * Integer[] indices = new Integer[n];
 * for (int i = 0; i < n; i++) indices[i] = i;
 * 
 * // For odd jumps: sort by value ascending, then by index ascending
 * Arrays.sort(indices, (i, j) -> arr[i] != arr[j] ?
 * Integer.compare(arr[i], arr[j]) : Integer.compare(i, j));
 * 
 * int[] nextOdd = makeNext(indices);
 * 
 * // For even jumps: sort by value descending, then by index ascending
 * Arrays.sort(indices, (i, j) -> arr[i] != arr[j] ?
 * Integer.compare(arr[j], arr[i]) : Integer.compare(i, j));
 * 
 * int[] nextEven = makeNext(indices);
 * 
 * // Fill DP arrays
 * for (int i = n - 2; i >= 0; i--) {
 * if (nextOdd[i] != -1) {
 * canReachOdd[i] = canReachEven[nextOdd[i]];
 * }
 * if (nextEven[i] != -1) {
 * canReachEven[i] = canReachOdd[nextEven[i]];
 * }
 * }
 * 
 * // Count good starting indices
 * int count = 0;
 * for (boolean canReach : canReachOdd) {
 * if (canReach) count++;
 * }
 * return count;
 * }
 * 
 * // Use monotonic stack to find next valid jump for each index
 * private int[] makeNext(Integer[] indices) {
 * int n = indices.length;
 * int[] next = new int[n];
 * Arrays.fill(next, -1);
 * 
 * Stack<Integer> stack = new Stack<>();
 * for (int i : indices) {
 * while (!stack.isEmpty() && stack.peek() < i) {
 * next[stack.pop()] = i;
 * }
 * stack.push(i);
 * }
 * return next;
 * }
 * }
 * 
 * Time Complexity: O(n log n)
 * - TreeMap operations: O(log n) per element, done n times = O(n log n)
 * - DP computation: O(n)
 * - Overall: O(n log n)
 * 
 * Space Complexity: O(n)
 * - TreeMap: O(n) in worst case
 * - DP arrays: O(n)
 * - Next jump arrays: O(n)
 * - Overall: O(n)
 * 
 * Key Insights:
 * 1. We work backwards from the end since we know the last position can always
 * reach the end
 * 2. For odd jumps: find the smallest value >= current value at a later index
 * 3. For even jumps: find the largest value <= current value at a later index
 * 4. Use TreeMap's ceilingKey() and floorKey() for efficient range queries
 * 5. DP alternates between odd and even jumps since jump numbers alternate
 * 
 * Example Walkthrough for [10,13,12,14,15]:
 * - Index 4: Can reach end (base case)
 * - Index 3: Odd jump to 4 (14->15), can reach end
 * - Index 2: Odd jump to 3 (12->14), then even jump fails, cannot reach end
 * - Index 1: Odd jump to 3 (13->14), then even jump fails, cannot reach end
 * - Index 0: Odd jump to 2 (10->12), then even jump fails, cannot reach end
 * - Answer: 2 (indices 3 and 4)
 */

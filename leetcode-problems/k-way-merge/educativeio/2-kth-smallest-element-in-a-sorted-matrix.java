import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.List;

/**
 * Problem: Kth Smallest Element in a Sorted Matrix
 * 
 * Statement:
 * Find the kth smallest element in an n x n matrix, where each row and column 
 * is sorted in ascending order.
 * 
 * Constraints:
 * - n == matrix.length == matrix[i].length
 * - 1 <= n <= 100
 * - -1000 <= matrix[i][j] <= 1000
 * - 1 <= k <= n^2
 */
class KthSmallestInSortedMatrix {

    /* ============================================================================
     * APPROACH 1: Brute Force (Flatten and Sort)
     * ============================================================================
     * Explanation:
     * The most straightforward approach is to extract all elements from the 2D 
     * matrix into a 1D array, sort that array, and return the element at index k-1.
     * This completely ignores the fact that the rows and columns are already sorted.
     * 
     * Time Complexity: O(N^2 log(N^2)) where N is the number of rows/cols.
     * Space Complexity: O(N^2) to store the 1D array.
     */
    public static int kthSmallestBruteForce(int[][] matrix, int k) {
        int n = matrix.length;
        int[] flatArray = new int[n * n];
        int index = 0;
        
        for (int[] row : matrix) {
            for (int val : row) {
                flatArray[index++] = val;
            }
        }
        
        Arrays.sort(flatArray);
        return flatArray[k - 1];
    }

    /* ============================================================================
     * APPROACH 2: Min-Heap (Priority Queue)
     * ============================================================================
     * Explanation:
     * Since every row is sorted, we can think of this problem as merging 'n' sorted 
     * lists. We can use a Min-Heap to keep track of the smallest current elements 
     * across all rows. 
     * 1. Insert the first element of each row into the Min-Heap.
     * 2. Extract the minimum element from the heap 'k-1' times. 
     * 3. Whenever we extract an element, we push the next element from its row 
     *    into the heap.
     * 4. The k-th extracted element is our answer.
     * 
     * We use Java 14+ 'record' to neatly store the value, row, and column indices.
     * 
     * Time Complexity: O(K log(min(N, K))). We extract K times, heap size is at most N.
     * Space Complexity: O(min(N, K)) for the Priority Queue.
     */
    
    // A record to store the matrix element and its coordinates
    private record HeapNode(int val, int row, int col) implements Comparable<HeapNode> {
        @Override
        public int compareTo(HeapNode other) {
            return Integer.compare(this.val, other.val);
        }
    }

    public static int kthSmallestMinHeap(int[][] matrix, int k) {
        int n = matrix.length;
        // Heap to store the current smallest elements across rows
        var minHeap = new PriorityQueue<HeapNode>();
        
        // Add the first element of the first min(N, K) rows
        for (int r = 0; r < Math.min(n, k); r++) {
            minHeap.offer(new HeapNode(matrix[r][0], r, 0));
        }
        
        // Extract the minimum k-1 times
        for (int i = 0; i < k - 1; i++) {
            HeapNode node = minHeap.poll();
            // If the row has more elements, add the next element to the heap
            if (node.col() < n - 1) {
                minHeap.offer(new HeapNode(matrix[node.row()], node.row(), node.col() + 1));
            }
        }
        
        // The root of the heap is now the k-th smallest element
        return minHeap.poll().val();
    }

    /* ============================================================================
     * APPROACH 3: Optimal Binary Search on Value Range
     * ============================================================================
     * Explanation:
     * Instead of searching by index, we binary search over the RANGE of values.
     * The smallest value is at matrix[0][0] and the largest is at matrix[n-1][n-1].
     * 
     * For a guessed 'mid' value, we can quickly count how many numbers in the matrix
     * are less than or equal to 'mid'. We do this by starting at the bottom-left 
     * corner of the matrix and walking up or right.
     * 
     * ASCII Visual for Counting Strategy:
     * Let mid = 13.
     * Matrix:
     * [ 1,  5,  9]
     * [10, 11, 13]
     * [12, 13, 15]
     * 
     * Start at bottom-left (row 2, col 0): value is 12.
     * 12 <= 13 (mid)? YES! 
     * -> Since cols are sorted, everything above 12 is also <= 13. 
     * -> Add (row + 1) = 3 to count. Move RIGHT to (row 2, col 1).
     * 
     * value is 13. 13 <= 13? YES!
     * -> Add (row + 1) = 3 to count. Move RIGHT to (row 2, col 2).
     * 
     * value is 15. 15 <= 13? NO!
     * -> Move UP to (row 1, col 2).
     * 
     * value is 13. 13 <= 13? YES!
     * -> Add (row + 1) = 2 to count. Move RIGHT to out of bounds.
     * 
     * If total count < k, our 'mid' was too small. (low = mid + 1)
     * If total count >= k, our 'mid' might be the answer or too large. (high = mid)
     * 
     * Time Complexity: O(N * log(Max_Value - Min_Value)). Count takes O(N).
     * Space Complexity: O(1) auxiliary space.
     */
    public static int kthSmallestBinarySearch(int[][] matrix, int k) {
        int n = matrix.length;
        int low = matrix[0][0];
        int high = matrix[n - 1][n - 1];
        
        while (low < high) {
            int mid = low + (high - low) / 2;
            int count = countLessOrEqual(matrix, mid, n);
            
            if (count < k) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        
        return low;
    }
    
    // Helper method for Binary Search approach
    private static int countLessOrEqual(int[][] matrix, int mid, int n) {
        int count = 0;
        int row = n - 1; // Start at the bottom-left corner
        int col = 0;
        
        while (row >= 0 && col < n) {
            if (matrix[row][col] <= mid) {
                // All elements in this column above the current row are also <= mid
                count += (row + 1);
                col++; // Move right
            } else {
                // Value is too large, move up to a smaller value
                row--;
            }
        }
        
        return count;
    }

    /* ============================================================================
     * TESTING / MAIN METHOD
     * ============================================================================
     */
    
    // Using Java 14+ record for structured test cases
    public record TestCase(int[][] matrix, int k, int expected) {}

    public static void main(String[] args) {
        // Setup Test Cases
        var testCases = List.of(
            new TestCase(
                new int[][]{
                    { 1,  5,  9},
                    {10, 11, 13},
                    {12, 13, 15}
                }, 
                8, 13
            ),
            new TestCase(
                new int[][]{
                    {-5}
                }, 
                1, -5
            ),
            new TestCase(
                new int[][]{
                    { 1,  2},
                    { 1,  3}
                }, 
                2, 1
            ),
            new TestCase(
                new int[][]{
                    {1, 4, 7, 11, 15},
                    {2, 5, 8, 12, 19},
                    {3, 6, 9, 16, 22},
                    {10, 13, 14, 17, 24},
                    {18, 21, 23, 26, 30}
                }, 
                5, 5
            )
        );

        System.out.println("Running tests for all 3 approaches...\n");

        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            System.out.println("Test Case " + (i + 1) + ": (k = " + tc.k + ")");
            
            int ans1 = kthSmallestBruteForce(tc.matrix, tc.k);
            int ans2 = kthSmallestMinHeap(tc.matrix, tc.k);
            int ans3 = kthSmallestBinarySearch(tc.matrix, tc.k);

            boolean pass1 = (ans1 == tc.expected);
            boolean pass2 = (ans2 == tc.expected);
            boolean pass3 = (ans3 == tc.expected);

            System.out.println("  Brute Force   : " + (pass1 ? "PASS" : "FAIL") + " -> Result: " + ans1);
            System.out.println("  Min-Heap      : " + (pass2 ? "PASS" : "FAIL") + " -> Result: " + ans2);
            System.out.println("  Binary Search : " + (pass3 ? "PASS" : "FAIL") + " -> Result: " + ans3);
            System.out.println("-".repeat(50));
        }
    }
}

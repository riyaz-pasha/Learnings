import java.util.*;

class KWeakestRowsHeap {

    /**
     * Record to store:
     * - number of soldiers in a row
     * - row index
     *
     * Used for comparison inside heap
     */
    record Row(int soldiers, int index) {}

    public static int[] kWeakestRows(int[][] matrix, int k) {

        /**
         * MAX HEAP (Strongest row at top)
         *
         * Why max heap?
         * → We only want to keep k weakest rows
         * → If size exceeds k, remove the STRONGEST
         *
         * Ordering logic:
         * 1. More soldiers → stronger → higher priority
         * 2. If equal soldiers → larger index → stronger
         */
        PriorityQueue<Row> maxHeap = new PriorityQueue<>(
                Comparator
                        .comparingInt(Row::soldiers).reversed()  // more soldiers first
                        .thenComparing(Comparator.comparingInt(Row::index).reversed()) // higher index first
        );

        /**
         * Step 1: Process each row
         */
        for (int i = 0; i < matrix.length; i++) {

            int soldiers = countSoldiers(matrix[i]);

            // Add current row into heap
            maxHeap.offer(new Row(soldiers, i));

            /**
             * Maintain heap size = k
             * Remove strongest row if exceeded
             */
            if (maxHeap.size() > k) {
                maxHeap.poll();
            }
        }

        /**
         * Step 2: Extract results
         *
         * IMPORTANT:
         * Heap gives strongest among remaining k first,
         * so we fill result array from BACK
         */
        int[] result = new int[k];

        for (int i = k - 1; i >= 0; i--) {
            result[i] = maxHeap.poll().index();
        }

        return result;
    }

    /**
     * 🔍 Binary Search to count number of soldiers (1s)
     *
     * Pattern: LAST TRUE (last occurrence of 1)
     *
     * Row example:
     * [1,1,1,0,0]
     *
     * Goal:
     * Find last index where value = 1
     *
     * Invariant:
     * - left side = possible 1s
     * - right side = possible 0s
     *
     * answerIndex keeps track of last seen 1
     */
    private static int countSoldiers(int[] row) {

        int low = 0, high = row.length - 1;

        int answerIndex = -1; // stores last index where row[i] == 1

        while (low <= high) {

            int mid = low + (high - low) / 2;

            if (row[mid] == 1) {
                /**
                 * mid is valid (1 found)
                 * move RIGHT to find last occurrence
                 */
                answerIndex = mid;
                low = mid + 1;
            } else {
                /**
                 * mid is 0 → discard right half
                 */
                high = mid - 1;
            }
        }

        /**
         * count = lastIndex + 1
         * if no 1 found → answerIndex = -1 → returns 0
         */
        return answerIndex + 1;
    }

    // Driver
    public static void main(String[] args) {

        int[][] matrix = {
                {1,1,0,0,0},
                {1,1,1,1,0},
                {1,0,0,0,0},
                {1,1,0,0,0},
                {1,1,1,1,1}
        };

        int k = 3;

        System.out.println(Arrays.toString(kWeakestRows(matrix, k)));
    }
}


/**
 * Problem Statement:
 * You are given an m x n binary matrix of 1s (soldiers) and 0s (civilians).
 * The soldiers (1s) are positioned in front of the civilians (0s).
 * A row i is weaker than a row j if:
 * 1. Number of soldiers in row i < Number of soldiers in row j.
 * 2. Both rows have the same number of soldiers and i < j.
 * Return the indexes of the k weakest rows in the matrix ordered from weakest to strongest.
 * 
 * Constraints:
 * - 2 <= m, n <= 100
 * - 1 <= k <= m
 * - matrix[i][j] is either 0 or 1.
 */
class KWeakestRows {

    /**
     * Java Record to neatly encapsulate a row's index and its soldier count.
     * Implements Comparable to define the "weakness" sorting rules natively.
     */
    public record RowInfo(int index, int soldiers) implements Comparable<RowInfo> {
        @Override
        public int compareTo(RowInfo other) {
            if (this.soldiers == other.soldiers) {
                return Integer.compare(this.index, other.index); // Tie-breaker: smaller index is weaker
            }
            return Integer.compare(this.soldiers, other.soldiers); // Primary rule: fewer soldiers is weaker
        }
    }

    /**
     * SOLUTION 1: Iterative Binary Search + Priority Queue (Min-Heap)
     * 
     * Time Complexity: O(M log N + M log K) or O(M log N + M log M)
     * Space Complexity: O(M)
     * 
     * VISUAL EXPLANATION & LOGIC (Binary Search for 1s):
     * Since 1s always appear before 0s, the row is sorted in descending order.
     * We want to find the LAST occurrence of 1.
     * 
     * Row: [1, 1, 1, 0, 0]
     * Indices: 0, 1, 2, 3, 4
     * 
     * Iteration 1:
     * L = 0, H = 4. mid = 2. row[mid] = 1.
     * This 1 might be the last one, so we record result = 2.
     * To check if there are more 1s, we search right: L = 3.
     * 
     * Iteration 2:
     * L = 3, H = 4. mid = 3. row[mid] = 0.
     * It's a 0, we went too far right. Search left: H = 2.
     * 
     * Loop Ends (L > H). The last 1 was at index 2 (result = 2).
     * Therefore, total soldiers = index + 1 = 3.
     */
    public static int[] kWeakestRowsIterativeBS(int[][] matrix, int k) {
        PriorityQueue<RowInfo> minHeap = new PriorityQueue<>();

        for (int i = 0; i < matrix.length; i++) {
            int soldiers = countSoldiersIterative(matrix[i]);
            minHeap.offer(new RowInfo(i, soldiers));
        }

        int[] weakestRows = new int[k];
        for (int i = 0; i < k; i++) {
            weakestRows[i] = minHeap.poll().index();
        }

        return weakestRows;
    }

    private static int countSoldiersIterative(int[] row) {
        int low = 0;
        int high = row.length - 1;
        
        int result = -1; // Explicit result variable initialized to -1 (meaning no soldiers found yet)

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (row[mid] == 1) {
                result = mid; // We found a 1, save its index
                low = mid + 1; // Look further right for the *last* 1
            } else {
                high = mid - 1; // It's a 0, so the last 1 must be to the left
            }
        }

        // The number of soldiers is the index of the last 1 plus one.
        // If no 1s were found, result is -1, so it correctly returns 0.
        return result + 1;
    }

    /**
     * SOLUTION 2: Recursive Binary Search + Array Sorting
     * 
     * Time Complexity: O(M log N + M log M)
     * Space Complexity: O(M) - Array storage and recursive call stack
     * 
     * EXPLANATION:
     * We populate an array of RowInfo using a functional recursive binary search.
     * Then we use standard Arrays.sort() which takes advantage of the Comparable implementation in our Record.
     */
    public static int[] kWeakestRowsRecursiveBS(int[][] matrix, int k) {
        RowInfo[] rowData = new RowInfo[matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            int soldiers = countSoldiersRecursive(matrix[i], 0, matrix[i].length - 1, -1) + 1;
            rowData[i] = new RowInfo(i, soldiers);
        }

        Arrays.sort(rowData);

        int[] weakestRows = new int[k];
        for (int i = 0; i < k; i++) {
            weakestRows[i] = rowData[i].index();
        }

        return weakestRows;
    }

    private static int countSoldiersRecursive(int[] row, int low, int high, int currentResult) {
        int result = currentResult; // Explicit tracking variable

        if (low > high) {
            return result; // Base case: Search space exhausted
        }

        int mid = low + (high - low) / 2;

        if (row[mid] == 1) {
            // Found a 1, update result and search right
            result = countSoldiersRecursive(row, mid + 1, high, mid);
        } else {
            // Found a 0, keep current result and search left
            result = countSoldiersRecursive(row, low, mid - 1, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Java Streams (Functional Approach)
     * 
     * Time Complexity: O(M * N + M log M)
     * Space Complexity: O(M) Overhead
     * 
     * EXPLANATION:
     * This relies entirely on modern Java Streams. It maps each row to a RowInfo object
     * by summing up the elements (since they are 1s and 0s, sum == count of 1s).
     * It then sorts them and limits the output to `k` elements.
     * While technically O(N) per row for counting, for max size N=100, this is incredibly fast and highly readable.
     */
    public static int[] kWeakestRowsStream(int[][] matrix, int k) {
        return IntStream.range(0, matrix.length)
                .mapToObj(i -> new RowInfo(i, Arrays.stream(matrix[i]).sum())) // Map to Record
                .sorted() // Uses our Comparable implementation natively
                .limit(k) // Take only the weakest k
                .mapToInt(RowInfo::index) // Extract just the indices
                .toArray();
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to elegantly structure our test cases.
     */
    public record TestCase(int[][] matrix, int k, int[] expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on standard logic and edge constraints
        TestCase[] testCases = {
            new TestCase(new int[][]{
                {1, 1, 0, 0, 0},
                {1, 1, 1, 1, 0},
                {1, 0, 0, 0, 0},
                {1, 1, 0, 0, 0},
                {1, 1, 1, 1, 1}
            }, 3, new int[]{2, 0, 3}),
            
            new TestCase(new int[][]{
                {1, 0, 0, 0},
                {1, 1, 1, 1},
                {1, 0, 0, 0},
                {1, 0, 0, 0}
            }, 2, new int[]{0, 2}), // Tie breakers: indices 0, 2, 3 all have one '1'. Order must be 0, 2.
            
            new TestCase(new int[][]{
                {0, 0, 0},
                {0, 0, 0},
                {0, 0, 0}
            }, 3, new int[]{0, 1, 2}), // Matrix of pure civilians
            
            new TestCase(new int[][]{
                {1, 1},
                {1, 1},
                {1, 1}
            }, 1, new int[]{0}) // Matrix of pure soldiers
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int[] resIterative = kWeakestRowsIterativeBS(tc.matrix(), tc.k());
            int[] resRecursive = kWeakestRowsRecursiveBS(tc.matrix(), tc.k());
            int[] resStream    = kWeakestRowsStream(tc.matrix(), tc.k());

            boolean passed = Arrays.equals(resIterative, tc.expected()) &&
                             Arrays.equals(resRecursive, tc.expected()) &&
                             Arrays.equals(resStream, tc.expected());

            System.out.printf("Test %d | k: %-2d -> Expected: %-15s | Passed: %b%n",
                    i + 1, tc.k(), Arrays.toString(tc.expected()), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iter: %s, Rec: %s, Stream: %s%n",
                        Arrays.toString(resIterative), Arrays.toString(resRecursive), Arrays.toString(resStream));
            }
        }
    }
}

/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n 2D array (matrix). 
 * We need to determine if it is a "Toeplitz Matrix".
 * A matrix is Toeplitz if every diagonal from top-left to bottom-right has the 
 * exact same elements. 
 * Equivalently, for any element at (i, j), it must be identical to the element 
 * at (i-1, j-1) provided that (i-1, j-1) is within the bounds of the matrix.
 * 
 * We also need to address two follow-up scenarios dealing with scale:
 * 1. The matrix is stored on disk, and we can only load one row at a time.
 * 2. The matrix is so huge we can only load a partial row into memory at once.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW (Confirm before writing code)
 * ============================================================================
 * 1. Can the matrix be empty (0 rows or 0 columns)? 
 *    (Constraints say 1 <= m, n <= 20, so it will always have at least 1 element).
 * 2. Is the matrix guaranteed to be a perfect rectangle? 
 *    (Yes, m x n implies all rows have length n).
 * 3. Can the elements be negative?
 *    (Constraints say 0 <= matrix[i][j] <= 99. Regardless, standard equality checks handle negatives).
 * 4. For the follow-ups, how is the matrix provided? 
 *    (E.g., as an Iterator, a BufferedReader, or a database cursor? We will mock 
 *     this using an Iterable/List for the row-by-row example).
 * 5. Can we modify the matrix in place? 
 *    (Not necessary for this problem, space is O(1) natively anyway).
 * 6. Should we optimize for early termination?
 *    (Yes, the moment we find a mismatch, we should return false immediately).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Standard Approach (In-Memory Array):
 *    - Loop through the matrix starting from row 1 and column 1.
 *    - Compare the current element `matrix[i][j]` with its top-left neighbor `matrix[i-1][j-1]`.
 *    - If any comparison fails, return false. If the loop finishes, return true.
 *    - Time: O(M * N), Space: O(1).
 * 
 * 2. Follow-up 1 Strategy (Memory Limited - One row at a time):
 *    - Keep a reference to the `previousRow`.
 *    - Read the `currentRow` from the disk.
 *    - Compare `currentRow[1...n-1]` with `previousRow[0...n-2]`.
 *    - Replace `previousRow` with `currentRow` and continue.
 *    - Space Complexity: O(N) where N is the width of the row.
 * 
 * 3. Follow-up 2 Strategy (Memory Extremely Limited - Partial rows):
 *    - If we can't even hold a full row, we must process the matrix in blocks or chunks.
 *    - We only ever need to compare `matrix[i][j]` with `matrix[i-1][j-1]`.
 *    - We can load a chunk of the previous row `prevChunk = matrix[i-1][colStart...colEnd-1]` 
 *      and a chunk of the current row `currChunk = matrix[i][colStart+1...colEnd]`.
 *    - Compare them. Do this sequentially for all blocks across the disk.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Standard Approach)
 * ============================================================================
 * Example: matrix = 
 * [
 *   [1, 2, 3, 4],
 *   [5, 1, 2, 3],
 *   [9, 5, 1, 2]
 * ]
 * 
 * i = 1, j = 1: curr = 1, top-left = 1. (Match)
 * i = 1, j = 2: curr = 2, top-left = 2. (Match)
 * i = 1, j = 3: curr = 3, top-left = 3. (Match)
 * i = 2, j = 1: curr = 5, top-left = 5. (Match)
 * i = 2, j = 2: curr = 1, top-left = 1. (Match)
 * i = 2, j = 3: curr = 2, top-left = 2. (Match)
 * 
 * All elements match their top-left counterparts. Result: TRUE.
 */

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class ToeplitzMatrix {

    public static void main(String[] args) {
        int[][] matrix1 = {
            {1, 2, 3, 4},
            {5, 1, 2, 3},
            {9, 5, 1, 2}
        };
        
        int[][] matrix2 = {
            {1, 2},
            {2, 2}
        };

        System.out.println("--- Solution 1: Standard In-Memory ---");
        System.out.println("Matrix 1 is Toeplitz: " + isToeplitzMatrixStandard(matrix1));
        System.out.println("Matrix 2 is Toeplitz: " + isToeplitzMatrixStandard(matrix2));
        
        System.out.println("\n--- Solution 2: Java Streams ---");
        System.out.println("Matrix 1 is Toeplitz: " + isToeplitzMatrixStreams(matrix1));
        
        System.out.println("\n--- Solution 3: Follow-Up 1 (One Row at a Time) ---");
        // Simulating reading from disk using an Iterator over a List of arrays
        List<int[]> diskMatrix = Arrays.asList(matrix1); 
        System.out.println("Matrix 1 is Toeplitz: " + isToeplitzMatrixFollowUp1(diskMatrix.iterator()));
    }

    /**
     * SOLUTION 1: Standard Iterative Approach
     * 
     * Idea: Traverse the matrix starting from row 1, col 1. Compare each 
     * element to the one diagonally top-left of it.
     * 
     * Time Complexity: O(M * N) - We visit every element exactly once (except row 0 and col 0).
     * Space Complexity: O(1) - No extra space used.
     */
    public static boolean isToeplitzMatrixStandard(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return true;
        
        int m = matrix.length;
        int n = matrix[0].length;
        
        // Start from row 1 and col 1.
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                // Check if current element equals the top-left element
                if (matrix[i][j] != matrix[i - 1][j - 1]) {
                    return false; // Early termination on first mismatch
                }
            }
        }
        
        return true;
    }

    /**
     * SOLUTION 2: Modern Java Streams Approach
     * 
     * Idea: Same logic as Solution 1, but written using Java IntStream to 
     * demonstrate modern, declarative programming style.
     * 
     * Time Complexity: O(M * N) - allMatch inherently short-circuits.
     * Space Complexity: O(1)
     */
    public static boolean isToeplitzMatrixStreams(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return true;
        
        int m = matrix.length;
        int n = matrix[0].length;
        
        // IntStream effectively mimics the nested for-loops, and allMatch provides
        // the early termination behavior if a mismatch is found.
        return IntStream.range(1, m).allMatch(i -> 
               IntStream.range(1, n).allMatch(j -> 
                   matrix[i][j] == matrix[i - 1][j - 1]
               )
        );
    }

    /**
     * SOLUTION 3: Follow-Up 1 (Matrix stored on Disk, Load 1 row at a time)
     * 
     * Idea: We simulate a disk read using an Iterator. We only keep the 
     * previously read row in memory. When we read the current row, we shift 
     * our indices to compare `currRow[j]` with `prevRow[j - 1]`.
     * 
     * Time Complexity: O(M * N) - We process each element once.
     * Space Complexity: O(N) - We only store one row (prevRow) in memory at a time.
     */
    public static boolean isToeplitzMatrixFollowUp1(Iterator<int[]> rowIterator) {
        if (!rowIterator.hasNext()) return true;
        
        // Load the first row into memory
        int[] prevRow = rowIterator.next();
        
        // Iterate through the remaining rows one by one
        while (rowIterator.hasNext()) {
            int[] currRow = rowIterator.next();
            
            // Compare the current row with the previous row
            for (int j = 1; j < currRow.length; j++) {
                if (currRow[j] != prevRow[j - 1]) {
                    return false;
                }
            }
            
            // The current row now becomes the previous row for the next iteration
            prevRow = currRow;
        }
        
        return true;
    }
    
    /*
     * EXPLANATION FOR FOLLOW-UP 2 (Load partial rows into memory)
     * 
     * If the matrix is so large that we cannot even load a full row into memory (e.g., millions of columns):
     * 
     * 1. Chunking/Blocks:
     *    We can divide the matrix into vertical slices (e.g., chunks of K columns where K elements fit in RAM).
     * 
     * 2. Overlap Handling:
     *    To check the diagonal correctly, the chunk for the "previous row" needs to be offset by 1 column 
     *    compared to the chunk for the "current row". 
     * 
     * 3. Algorithm:
     *    - Read `prevChunk` from disk: `matrix[i-1][start ... end-1]`.
     *    - Read `currChunk` from disk: `matrix[i][start+1 ... end]`.
     *    - Compare them element by element in memory.
     *    - Proceed chunk by chunk across the row, then move to the next row.
     * 
     * Space Complexity reduces to O(K) where K is the chunk size. 
     * Time Complexity remains O(M * N), though disk I/O latency will dominate.
     */
}

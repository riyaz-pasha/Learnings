import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * =================================================================================================
 * PROBLEM STATEMENT:
 * Given a 0-indexed 1-dimensional (1D) integer array `original` and two integers `m` and `n`, 
 * reshape the array into a 2-dimensional (2D) array with `m` rows and `n` columns while 
 * preserving the order of elements. If impossible (total elements don't match m * n), 
 * return an empty 2D array.
 * 
 * =================================================================================================
 * INTERVIEW PREPARATION & STRATEGY
 * =================================================================================================
 * 
 * [1. Clarifying Questions to ask the interviewer before writing code]
 * Q1: What should be the exact structure of the "empty" 2D array if the conversion is impossible? 
 *     (Confirm if it should be `new int[0][0]` or `new int[m][n]` filled with zeros. Standard is `new int[0][0]`).
 * Q2: Can `m` or `n` be zero or negative? 
 *     (Based on constraints $1 \le m, n \le 33$, they are always positive, but good to confirm).
 * Q3: Can the original array be empty? 
 *     (Constraints say $1 \le \text{original.length} \le 1000$, so it's guaranteed to have at least 1 element).
 * Q4: Does the output array need to be newly allocated, or can we return references to slices of original? 
 *     (In Java, we must allocate a new 2D array).
 * Q5: Are there memory constraints preventing us from allocating a new 2D array?
 *     (No, standard constraints apply).
 * 
 * [2. Restating the Problem in Own Words]
 * We have a flat list of numbers. We want to pack these numbers row by row into a grid of size `m` by `n`. 
 * If the grid has exactly the same number of "slots" as our flat list has numbers, we fill it and return it. 
 * If the slots and numbers don't match, we abort and return an empty grid.
 * 
 * [3. Key Observations & Intuition]
 * - Observation 1: The total capacity of the new 2D array is exactly $m \times n$.
 * - Observation 2: The total number of elements in the 1D array is `original.length`.
 * - Pre-condition Check: If `original.length != m * n`, the operation is impossible. 
 * - Mapping mechanism: We are traversing linearly. The $i$-th element in the 1D array needs a specific 
 *   `(row, col)` coordinate in the 2D array.
 * 
 * [4. Visual Tracing & Reasoning]
 * Let `original = [1, 2, 3, 4]`, `m = 2`, `n = 2`.
 * Total elements = 4. Total capacity = 2 * 2 = 4. (Matches!)
 * 
 * 1D Indices:       0   1   2   3
 * Elements:       [ 1 , 2 , 3 , 4 ]
 * 
 * Target 2D Array: (n = 2 columns)
 * Row 0: [ ?, ? ]  -> indices (0,0) and (0,1)
 * Row 1: [ ?, ? ]  -> indices (1,0) and (1,1)
 * 
 * How to map 1D index `i` to 2D coordinates `(row, col)`?
 * i = 0 -> row 0, col 0  |  (0 / 2 = 0), (0 % 2 = 0)
 * i = 1 -> row 0, col 1  |  (1 / 2 = 0), (1 % 2 = 1)
 * i = 2 -> row 1, col 0  |  (2 / 2 = 1), (2 % 2 = 0)
 * i = 3 -> row 1, col 1  |  (3 / 2 = 1), (3 % 2 = 1)
 * 
 * Formula derived:
 * row = i / n
 * col = i % n
 * 
 * [5. Edge Cases to Consider]
 * - original.length < m * n (e.g., [1,2], m=2, n=2) -> return []
 * - original.length > m * n (e.g., [1,2,3], m=1, n=2) -> return []
 * - 1D to 1D equivalent (e.g., m=1, n=original.length) -> return [[1,2,3...]]
 * - Single element array (e.g., [1], m=1, n=1) -> return [[1]]
 * 
 * =================================================================================================
 */
public class ReshapeArray {

    /**
     * SOLUTION 1: Division and Modulus (The Optimal / Mathematical Approach)
     * 
     * Idea: Use a single loop over the 1D array elements. Use integer division and 
     * modulo arithmetic to calculate the corresponding row and column indices.
     * 
     * Time Complexity: $O(m \times n)$ or $O(N)$ where N is original.length. We visit each element once.
     * Space Complexity: $O(1)$ auxiliary space (excluding the output array).
     */
    public int[][] matrixReshapeMath(int[] original, int m, int n) {
        int length = original.length;
        
        // Step 1: Base Case / Pre-condition validation.
        // If the total elements don't match the required cells (m * n), return an empty 2D array.
        if (length != m * n) {
            // Returning an empty 2D array as per the problem requirements.
            return new int[0][0]; 
        }
        
        // Step 2: Initialize the result array with dimensions m x n.
        int[][] result = new int[m][n];
        
        // Step 3: Iterate through all elements of the 1D array.
        for (int i = 0; i < length; i++) {
            // 'i / n' gives the current row. 
            // Why? Because every 'n' elements form one complete row.
            int row = i / n; 
            
            // 'i % n' gives the current column within that row.
            // Why? Modulo wraps around back to 0 once we hit 'n' elements.
            int col = i % n; 
            
            // Place the element in the calculated coordinates.
            result[row][col] = original[i];
        }
        
        return result;
    }

    /**
     * SOLUTION 2: Two Pointers / Nested Loops Approach
     * 
     * Idea: Keep a pointer for the 1D array. Iterate through rows and columns of 
     * the new 2D array, placing the 1D array element and incrementing the pointer.
     * This avoids division and modulo, which can sometimes be slightly slower CPU instructions.
     * 
     * Time Complexity: $O(m \times n)$
     * Space Complexity: $O(1)$ auxiliary space.
     */
    public int[][] matrixReshapeTwoPointers(int[] original, int m, int n) {
        if (original.length != m * n) {
            return new int[0][0];
        }
        
        int[][] result = new int[m][n];
        
        // Pointer to track our current position in the 'original' 1D array.
        int originalIndex = 0; 
        
        // Iterate through each row of the target 2D array
        for (int row = 0; row < m; row++) {
            // Iterate through each column of the target 2D array
            for (int col = 0; col < n; col++) {
                // Assign the value and post-increment the pointer
                result[row][col] = original[originalIndex++];
            }
        }
        
        return result;
    }

    /**
     * SOLUTION 3: Modern Java / Functional Approach (Using Streams)
     * 
     * Idea: Use Java 8+ Streams (IntStream) to declaratively generate the 2D array.
     * We map an IntStream ranging from 0 to m-1 (representing rows), 
     * and for each row, we extract the corresponding sub-array from the original array.
     * 
     * Note: While cleaner and showcases Java mastery, it involves more overhead due to Stream internals
     * and array copying (Arrays.copyOfRange), so it's slightly less performant than Solution 1 & 2.
     * 
     * Time Complexity: $O(m \times n)$
     * Space Complexity: $O(1)$ auxiliary space.
     */
    public int[][] matrixReshapeStreams(int[] original, int m, int n) {
        if (original.length != m * n) {
            return new int[0][0];
        }
        
        // IntStream.range(0, m) creates a stream of row indices: [0, 1, 2 ..., m-1]
        return IntStream.range(0, m)
                // For each row index, slice the 'original' array to get exactly 'n' elements.
                // Arrays.copyOfRange(array, startInclusive, endExclusive)
                .mapToObj(row -> Arrays.copyOfRange(original, row * n, row * n + n))
                // Collect the stream of 1D arrays into a 2D array
                .toArray(int[][]::new);
    }

    /**
     * =================================================================================================
     * MAIN METHOD: Executing Examples & Edge Cases
     * Utilizing Java 14+ Records to keep test cases concise and readable.
     * =================================================================================================
     */
    public static void main(String[] args) {
        ReshapeArray solution = new ReshapeArray();
        
        // Using a Java Record to structure our test cases cleanly
        record TestCase(String description, int[] original, int m, int n) {}
        
        TestCase[] testCases = {
            new TestCase("Standard valid reshape", new int[]{1, 2, 3, 4}, 2, 2),
            new TestCase("Invalid reshape (too few elements)", new int[]{1, 2}, 1, 1),
            new TestCase("Invalid reshape (too many elements)", new int[]{1, 2, 3, 4, 5}, 2, 2),
            new TestCase("1D to 1D (Flattened row)", new int[]{1, 2, 3}, 1, 3),
            new TestCase("1D to Column vector", new int[]{1, 2, 3}, 3, 1),
            new TestCase("Single element", new int[]{42}, 1, 1)
        };
        
        for (TestCase tc : testCases) {
            System.out.println("--- Test Case: " + tc.description() + " ---");
            System.out.println("Input: " + Arrays.toString(tc.original()) + ", m=" + tc.m() + ", n=" + tc.n());
            
            // Testing Math Solution
            int[][] resMath = solution.matrixReshapeMath(tc.original(), tc.m(), tc.n());
            System.out.println("Output (Math Approach):    " + format2DArray(resMath));
            
            // Testing Two Pointer Solution
            int[][] resPtr = solution.matrixReshapeTwoPointers(tc.original(), tc.m(), tc.n());
            System.out.println("Output (Pointer Approach): " + format2DArray(resPtr));

            // Testing Stream Solution
            int[][] resStream = solution.matrixReshapeStreams(tc.original(), tc.m(), tc.n());
            System.out.println("Output (Stream Approach):  " + format2DArray(resStream));
            
            System.out.println();
        }
    }
    
    /**
     * Helper method to pretty-print 2D arrays since Arrays.deepToString() 
     * is a bit messy for grid visualization.
     */
    private static String format2DArray(int[][] arr) {
        if (arr == null || arr.length == 0) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(Arrays.toString(arr[i]));
            if (i < arr.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}

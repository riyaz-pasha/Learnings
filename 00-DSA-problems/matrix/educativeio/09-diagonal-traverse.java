/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n 2D array (matrix). 
 * Our task is to return a 1D array containing all the elements of the matrix 
 * traversed in a zigzag diagonal order. 
 * - The first diagonal goes Up-Right.
 * - The second diagonal goes Down-Left.
 * - This alternating pattern continues until we visit every element.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the matrix be empty? 
 *    (Constraints state 1 <= m, n <= 10^4, so it will always have at least 1 element).
 * 2. Are the dimensions always a perfect square (m == n)?
 *    (No, m and n can be different, so our boundary logic must handle rectangles).
 * 3. Does the first diagonal always move Up-Right?
 *    (Yes, the standard zigzag diagonal traverse starts by moving Up-Right).
 * 4. Can the total number of elements exceed the 32-bit integer limit?
 *    (Constraints say m * n <= 10^4, so the output array easily fits in standard memory).
 * 5. Can we modify the matrix?
 *    (No need, we only need to read it. Our space complexity should just be O(1) 
 *    auxiliary, ignoring the returned output array).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Single Element: [[5]] -> Output: [5]
 * 2. Single Row (1 x N): [[1, 2, 3]] -> Output: [1, 2, 3]
 *    (The direction alternates, but since there is no vertical movement, it just sweeps right).
 * 3. Single Column (M x 1): [[1], [2], [3]] -> Output: [1, 2, 3]
 * 4. Rectangular Matrix:
 *    [[1, 2, 3],
 *     [4, 5, 6]]
 *    Output: [1, 2, 4, 5, 3, 6]
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Grouping by Diagonals (The Mathematical/Intuitive Approach):
 *    - Key Observation: For any element at `mat[i][j]`, the sum of its indices `i + j` 
 *      represents the diagonal it belongs to. The sum ranges from `0` to `m + n - 2`.
 *    - We can create a map or array of lists to group elements by their `i + j` sum.
 *    - By default, traversing row by row gathers diagonals in the Down-Left direction.
 *    - If the diagonal index (`i + j`) is EVEN, the problem wants us to go Up-Right, 
 *      so we simply reverse the list for that diagonal.
 *    - Time: O(M * N), Space: O(M * N) for the lists/map. Good, but uses extra memory.
 * 
 * 2. Simulation / Boundary Tracking (The Most Optimal Approach):
 *    - We can track our current row `r` and col `c`, and alternate our direction.
 *    - Up-Right Direction: We want to move `r--` and `c++`.
 *      - If we hit the RIGHT boundary (`c == n - 1`), we must move DOWN to the next row (`r++`), 
 *        and flip our direction. (MUST CHECK THIS FIRST)
 *      - Else if we hit the TOP boundary (`r == 0`), we must move RIGHT to the next col (`c++`), 
 *        and flip our direction.
 *      - Otherwise, keep moving `r--, c++`.
 *    - Down-Left Direction: We want to move `r++` and `c--`.
 *      - If we hit the BOTTOM boundary (`r == m - 1`), we must move RIGHT to the next col (`c++`), 
 *        and flip our direction. (MUST CHECK THIS FIRST)
 *      - Else if we hit the LEFT boundary (`c == 0`), we must move DOWN to the next row (`r++`), 
 *        and flip our direction.
 *      - Otherwise, keep moving `r++, c--`.
 *    - Time: O(M * N), Space: O(1) auxiliary. 
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Simulation Approach)
 * ============================================================================
 * Matrix:
 * [1, 2, 3]
 * [4, 5, 6]
 * [7, 8, 9]
 * 
 * Diagonals (i+j sums):
 * d=0: [1]
 * d=1: [2, 4]
 * d=2: [7, 5, 3]
 * d=3: [6, 8]
 * d=4: [9]
 * 
 * Trace:
 * Start: (0, 0), dir = UP-RIGHT. Add 1.
 * Hit Top Wall (r==0). Next: c++, dir = DOWN-LEFT. r=0, c=1.
 * (0, 1): Add 2. Move r++, c-- -> (1, 0).
 * (1, 0): Add 4. Hit Left Wall (c==0). Next: r++, dir = UP-RIGHT. r=2, c=0.
 * (2, 0): Add 7. Move r--, c++ -> (1, 1).
 * (1, 1): Add 5. Move r--, c++ -> (0, 2).
 * (0, 2): Add 3. Hit Top Wall (r==0), but WAIT, we also hit Right Wall!
 *         Priority: Check Right Wall first. Since c == n-1, next: r++, dir = DOWN-LEFT. r=1, c=2.
 * (1, 2): Add 6. Move r++, c-- -> (2, 1).
 * (2, 1): Add 8. Hit Bottom Wall (r==m-1). Next: c++, dir = UP-RIGHT. r=2, c=2.
 * (2, 2): Add 9. Done.
 */

import java.util.*;

public class DiagonalTraverse {

    public static void main(String[] args) {
        int[][] mat1 = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        
        int[][] mat2 = {
            {1, 2},
            {3, 4}
        };
        
        int[][] mat3 = {
            {1, 2, 3} // Single row
        };

        System.out.println("--- Solution 1: Diagonal Grouping ---");
        printArray(findDiagonalOrderGrouping(mat1));
        printArray(findDiagonalOrderGrouping(mat2));

        System.out.println("\n--- Solution 2: Simulation (Optimal) ---");
        printArray(findDiagonalOrderSimulation(mat1));
        printArray(findDiagonalOrderSimulation(mat2));
        printArray(findDiagonalOrderSimulation(mat3));
    }

    /**
     * SOLUTION 1: Grouping by Diagonal Index (i + j)
     * 
     * Idea: Group items by `i + j`. Reverse the lists where `i + j` is even. 
     * Uses extra space but the logic is very simple and mathematically grounded.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(M * N) for the lists
     */
    public static int[] findDiagonalOrderGrouping(int[][] mat) {
        if (mat == null || mat.length == 0) return new int[0];
        
        int m = mat.length;
        int n = mat[0].length;
        
        // We have exactly m + n - 1 diagonals
        List<List<Integer>> diagonals = new ArrayList<>();
        for (int i = 0; i < m + n - 1; i++) {
            diagonals.add(new ArrayList<>());
        }
        
        // Group elements by i + j
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                diagonals.get(i + j).add(mat[i][j]);
            }
        }
        
        int[] result = new int[m * n];
        int idx = 0;
        
        // Build the result
        for (int d = 0; d < diagonals.size(); d++) {
            List<Integer> diag = diagonals.get(d);
            // Even diagonals go Up-Right. Since our loops gathered them Down-Left, we reverse.
            if (d % 2 == 0) {
                Collections.reverse(diag);
            }
            for (int val : diag) {
                result[idx++] = val;
            }
        }
        
        return result;
    }

    /**
     * SOLUTION 2: Simulation (Most Optimal)
     * 
     * Idea: Simulate walking the diagonals. Keep track of direction and strictly 
     * manage what happens when hitting the boundaries.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(1) auxiliary (ignoring the output array).
     */
    public static int[] findDiagonalOrderSimulation(int[][] mat) {
        if (mat == null || mat.length == 0) return new int[0];
        
        int m = mat.length;
        int n = mat[0].length;
        int[] result = new int[m * n];
        
        int r = 0, c = 0;
        
        for (int i = 0; i < result.length; i++) {
            result[i] = mat[r][c];
            
            // The sum of row and col determines the diagonal line.
            // If the sum is EVEN, we are moving UP-RIGHT.
            if ((r + c) % 2 == 0) {
                // Moving UP-RIGHT (row decreases, col increases)
                // Boundary check priority is critical: Check the RIGHT wall first!
                // If we are at the top-right corner, both c == n-1 and r == 0 are true.
                // We MUST process c == n-1 first so we move down instead of off the grid to the right.
                if (c == n - 1) {
                    r++; // Hit right wall, move down
                } else if (r == 0) {
                    c++; // Hit top wall, move right
                } else {
                    // Normal move up-right
                    r--; 
                    c++;
                }
            } 
            // If the sum is ODD, we are moving DOWN-LEFT.
            else {
                // Moving DOWN-LEFT (row increases, col decreases)
                // Check BOTTOM wall first to handle bottom-left corner correctly.
                if (r == m - 1) {
                    c++; // Hit bottom wall, move right
                } else if (c == 0) {
                    r++; // Hit left wall, move down
                } else {
                    // Normal move down-left
                    r++;
                    c--;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Helper method to neatly print an array.
     */
    private static void printArray(int[] arr) {
        System.out.print("[");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + (i < arr.length - 1 ? ", " : ""));
        }
        System.out.println("]");
    }
    
    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if the matrix is jagged (an array of arrays with different lengths)?
     *    Answer: The grouping approach (Solution 1) is incredibly resilient to this. 
     *    You can just loop through whatever `i` and `j` exist, and `map.get(i+j).add()` 
     *    will still group them perfectly on the correct diagonals. Simulation would be 
     *    a nightmare due to tracking the irregular "walls".
     * 
     * 2. What if the matrix is too large to fit in memory and is stored on disk?
     *    Answer: The zigzag pattern jumps heavily across disk pages, causing high I/O 
     *    latency. A better approach would be to read chunks sequentially, determine 
     *    the `i+j` diagonal for each element, and write the elements to diagonal-specific 
     *    temporary files. Finally, read back the files in order (reversing even ones) 
     *    and stitch them together.
     */
}

/**
 * ================================================================
 * 🔥 DIAGONAL TRAVERSE (ZIG-ZAG) — INTERVIEW READY VERSION
 * ================================================================
 *
 * 🧠 CORE IDEA:
 * We traverse the matrix diagonally in a zig-zag manner:
 *
 * Direction 1  → moving UP-RIGHT  (↗)
 * Direction -1 → moving DOWN-LEFT (↙)
 *
 * We simulate movement and change direction when we hit boundaries.
 *
 * ------------------------------------------------
 * 💡 KEY OBSERVATION:
 * Every move is either:
 *   ↗ (row--, col++)
 *   ↙ (row++, col--)
 *
 * When we hit boundaries:
 *   - Top wall → go DOWN, change direction
 *   - Bottom wall → go RIGHT, change direction
 *   - Left wall → go DOWN, change direction
 *   - Right wall → go DOWN, change direction
 *
 * ------------------------------------------------
 * ⏱ Time Complexity:  O(m * n)
 * 🧠 Space Complexity: O(1) (excluding result array)
 * ================================================================
 */
public class DiagonalTraverse {

    public static int[] findDiagonalOrder(int[][] matrix) {

        int totalRows = matrix.length;
        int totalCols = matrix[0].length;

        int[] result = new int[totalRows * totalCols];

        // Current position in matrix
        int currentRow = 0;
        int currentCol = 0;

        // Direction:
        // +1 → moving UP-RIGHT ↗
        // -1 → moving DOWN-LEFT ↙
        int direction = 1;

        int resultIndex = 0;

        /**
         * We will visit every cell exactly once.
         * So loop until result array is filled.
         */
        while (resultIndex < result.length) {

            // STEP 1: Add current element
            result[resultIndex++] = matrix[currentRow][currentCol];

            /**
             * STEP 2: Move in current direction
             */
            if (direction == 1) {
                // Moving UP-RIGHT ↗
                currentRow--;
                currentCol++;

                /**
                 * STEP 3: Boundary Handling
                 */

                // ❗ Case 1: Hit RIGHT boundary
                if (currentCol == totalCols) {
                    currentCol = totalCols - 1; // step back
                    currentRow += 2;            // move down
                    direction = -1;             // change direction
                }
                // ❗ Case 2: Hit TOP boundary
                else if (currentRow < 0) {
                    currentRow = 0;             // step back
                    direction = -1;             // change direction
                }
            } else {
                // Moving DOWN-LEFT ↙
                currentRow++;
                currentCol--;

                /**
                 * STEP 3: Boundary Handling
                 */

                // ❗ Case 3: Hit BOTTOM boundary
                if (currentRow == totalRows) {
                    currentRow = totalRows - 1; // step back
                    currentCol += 2;            // move right
                    direction = 1;              // change direction
                }
                // ❗ Case 4: Hit LEFT boundary
                else if (currentCol < 0) {
                    currentCol = 0;             // step back
                    direction = 1;              // change direction
                }
            }
        }

        return result;
    }

    public static void main(String[] args) {

        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };

        System.out.println(Arrays.toString(findDiagonalOrder(matrix)));
    }
}

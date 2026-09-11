/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an n x n 2D matrix (a square matrix).
 * Our task is to rotate the matrix 90 degrees clockwise.
 * The operation MUST be done in-place, meaning we cannot create a new 
 * n x n matrix to store the rotated values. We must modify the input directly.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Is the matrix guaranteed to be square (n x n)? 
 *    (Yes, constraints specify matrix.length == matrix[i].length).
 * 2. Can the matrix be empty? 
 *    (Constraints say 1 <= matrix.length <= 20, so it's at least 1x1).
 * 3. Does "in-place" strictly mean O(1) space? Can we use a few variables?
 *    (Yes, O(1) space is required. Using a few variables to facilitate swapping 
 *    is perfectly fine).
 * 4. Can elements be negative or zero?
 *    (Yes, constraints say elements range from -1000 to 1000. This just means 
 *    we shouldn't use 0 as a special "unvisited" marker if simulating).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. 1x1 Matrix: [[1]] -> Remains [[1]].
 * 2. 2x2 Matrix (Even dimension):
 *    [[1, 2],       ->      [[3, 1],
 *     [3, 4]]                [4, 2]]
 * 3. 3x3 Matrix (Odd dimension - center stays in place):
 *    [[1, 2, 3],            [[7, 4, 1],
 *     [4, 5, 6],    ->       [8, 5, 2],
 *     [7, 8, 9]]             [9, 6, 3]]
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (O(N^2) Space - Not In-Place):
 *    - To establish baseline understanding, mention that if space wasn't an issue, 
 *      we could map `matrix[i][j]` to `newMatrix[j][n - 1 - i]`.
 *    - This shows you understand the math behind the coordinate shift.
 *    - Time: O(N^2), Space: O(N^2).
 * 
 * 2. Transpose and Reverse (The "Elegant/Math" Approach):
 *    - Rotating a matrix 90 degrees clockwise is mathematically equivalent to:
 *      1. Transposing the matrix (turning rows into columns).
 *      2. Reversing each individual row.
 *    - Transposing is easy: swap matrix[i][j] with matrix[j][i].
 *    - Reversing a row is easy: two pointers from start and end of the row.
 *    - Time: O(N^2) (two passes), Space: O(1). Highly recommended for readability.
 * 
 * 3. Layer by Layer / 4-Way Swap (The "Engineering" Approach):
 *    - Process the matrix in concentric rings, from the outside in.
 *    - For each layer, group elements in sets of 4 (top, right, bottom, left) 
 *      that need to take each other's places.
 *    - Swap them in a cyclic manner using a single temporary variable.
 *    - Time: O(N^2) (one pass), Space: O(1).
 *    - This approach requires careful index tracking but is slightly more 
 *      efficient because it reads/writes exactly once per cell.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Transpose + Reverse Approach)
 * ============================================================================
 * Initial Matrix (3x3):
 * [1, 2, 3]
 * [4, 5, 6]
 * [7, 8, 9]
 * 
 * Step 1: Transpose (Swap elements across the main diagonal: matrix[i][j] <-> matrix[j][i])
 * i=0, j=1: Swap 2 and 4
 * i=0, j=2: Swap 3 and 7
 * i=1, j=2: Swap 6 and 8
 * 
 * Transposed Matrix:
 * [1, 4, 7]
 * [2, 5, 8]
 * [3, 6, 9]
 * 
 * Step 2: Reverse each row (Swap elements horizontally)
 * Row 0: Swap 1 and 7 -> [7, 4, 1]
 * Row 1: Swap 2 and 8 -> [8, 5, 2]
 * Row 2: Swap 3 and 9 -> [9, 6, 3]
 * 
 * Final Rotated Matrix:
 * [7, 4, 1]
 * [8, 5, 2]
 * [9, 6, 3]
 */

import java.util.*;

public class RotateMatrix {

    public static void main(String[] args) {
        int[][] matrix1 = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };

        int[][] matrix2 = {
            { 5,  1,  9, 11},
            { 2,  4,  8, 10},
            {13,  3,  6,  7},
            {15, 14, 12, 16}
        };

        System.out.println("--- Solution 1: Transpose + Reverse (Elegant) ---");
        System.out.println("Original 3x3:");
        printMatrix(matrix1);
        rotateTransposeAndReverse(matrix1);
        System.out.println("Rotated 3x3:");
        printMatrix(matrix1);

        System.out.println("\n--- Solution 2: Layer by Layer (4-Way Swap) ---");
        System.out.println("Original 4x4:");
        printMatrix(matrix2);
        rotateLayerByLayer(matrix2);
        System.out.println("Rotated 4x4:");
        printMatrix(matrix2);
    }

    /**
     * SOLUTION 1: Transpose and Reverse (Most readable and common)
     * 
     * Idea: A 90-degree clockwise rotation is exactly equivalent to reflecting 
     * the matrix across its main diagonal (transposing), and then reflecting it 
     * horizontally (reversing each row).
     * 
     * Time Complexity: O(N^2) - Two separate passes through the matrix.
     * Space Complexity: O(1) - Only a temporary variable used for swapping.
     */
    public static void rotateTransposeAndReverse(int[][] matrix) {
        if (matrix == null || matrix.length <= 1) return;
        int n = matrix.length;

        // Step 1: Transpose the matrix
        // We only loop through the upper triangle (j > i) to avoid double-swapping
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = temp;
            }
        }

        // Step 2: Reverse each row
        for (int i = 0; i < n; i++) {
            int left = 0;
            int right = n - 1;
            while (left < right) {
                int temp = matrix[i][left];
                matrix[i][left] = matrix[i][right];
                matrix[i][right] = temp;
                
                left++;
                right--;
            }
        }
    }

    /**
     * SOLUTION 2: Layer by Layer / 4-Way Swap (Optimal Single Pass)
     * 
     * Idea: Move in concentric circles (layers) from the outside in. For each 
     * layer, pick 4 corresponding elements (top, right, bottom, left) and swap 
     * them in a cyclic pattern.
     * 
     * Time Complexity: O(N^2) - One single pass, touches each element exactly once.
     * Space Complexity: O(1) - Only a temporary variable used for swapping.
     */
    public static void rotateLayerByLayer(int[][] matrix) {
        if (matrix == null || matrix.length <= 1) return;
        int n = matrix.length;
        
        // Loop through each concentric layer.
        // For a 4x4 matrix, layers are 0 and 1. For a 3x3, layer is 0.
        for (int layer = 0; layer < n / 2; layer++) {
            int first = layer;
            int last = n - 1 - layer;
            
            // Loop through the elements in the current layer
            for (int i = first; i < last; i++) {
                // Offset calculates how far we are from the boundary of this layer
                int offset = i - first;
                
                // Save the TOP element
                int top = matrix[first][i];
                
                // Move LEFT element to TOP
                matrix[first][i] = matrix[last - offset][first];
                
                // Move BOTTOM element to LEFT
                matrix[last - offset][first] = matrix[last][last - offset];
                
                // Move RIGHT element to BOTTOM
                matrix[last][last - offset] = matrix[i][last];
                
                // Move saved TOP element to RIGHT
                matrix[i][last] = top;
            }
        }
    }

    /**
     * Helper method to neatly print a 2D matrix.
     */
    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int val : row) {
                System.out.printf("%3d ", val);
            }
            System.out.println();
        }
    }

    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. How would you rotate the matrix 90 degrees ANTI-CLOCKWISE (Counter-Clockwise)?
     *    Answer: The math is very similar. 
     *    Option A: Transpose the matrix, then reverse the COLUMNS (top to bottom).
     *    Option B: Reverse the ROWS (left to right), then Transpose the matrix.
     *    
     * 2. How would you rotate the matrix 180 degrees?
     *    Answer: This is equivalent to reversing the rows (flip horizontally), 
     *    then reversing the columns (flip vertically), which is the same as 
     *    swapping `matrix[i][j]` with `matrix[n - 1 - i][n - 1 - j]`.
     * 
     * 3. What if the matrix is NOT square (e.g., M x N)? Can you do it in-place?
     *    Answer: No, it is generally impossible to do an M x N rotation purely 
     *    in-place in Java without allocating an N x M array, because Java arrays 
     *    have fixed lengths, and the row/column dimensions physically change. 
     *    You would need to allocate `int[][] rotated = new int[n][m];`
     */
}



public class RotateMatrixLayer {

    /**
     * ================================================================
     * 🔥 ROTATE MATRIX 90° CLOCKWISE (IN-PLACE) — LAYER APPROACH
     * ================================================================
     *
     * 🧠 CORE PROBLEM:
     * Given an n x n matrix, rotate it 90° clockwise WITHOUT using extra space.
     *
     * ---------------------------------------------------------------
     * 🔥 INTUITION (MOST IMPORTANT PART)
     * ---------------------------------------------------------------
     *
     * Instead of moving all elements at once (which would overwrite data ❌),
     * we rotate elements in GROUPS OF 4 (cycle rotation).
     *
     * Example (3x3):
     *
     *   1 2 3
     *   4 5 6      →      7 4 1
     *   7 8 9             8 5 2
     *                     9 6 3
     *
     * Focus on corners:
     *
     *   (0,0) → (0,2) → (2,2) → (2,0) → back to (0,0)
     *
     * This is a 4-way cyclic shift:
     *
     *   top → right → bottom → left → top
     *
     * ---------------------------------------------------------------
     * 🧅 WHY "LAYERS" (VERY IMPORTANT)
     * ---------------------------------------------------------------
     *
     * Matrix is processed like an onion:
     *
     *   Layer 0 → outer boundary
     *   Layer 1 → inner boundary
     *   ...
     *
     * Example (4x4):
     *
     *   Layer 0:
     *   1  2  3  4
     *   5        8
     *   9       12
     *   13 14 15 16
     *
     *   Layer 1:
     *      6  7
     *     10 11
     *
     * 👉 Each layer is an independent "ring"
     * 👉 We rotate one ring completely before moving inside
     *
     * WHY needed?
     * ✔ Prevents overwriting values
     * ✔ Ensures every element moves exactly once
     * ✔ Keeps operation in-place (O(1) space)
     *
     * Number of layers = n / 2
     *
     * ---------------------------------------------------------------
     * 🧠 HOW EACH LAYER IS ROTATED
     * ---------------------------------------------------------------
     *
     * For each layer:
     *
     *   first = layer index
     *   last  = n - 1 - layer
     *
     * We iterate from left → right across the top row of the layer.
     *
     * For each position, we rotate 4 elements:
     *
     *   top    = (first, i)
     *   right  = (i, last)
     *   bottom = (last, last - offset)
     *   left   = (last - offset, first)
     *
     * where:
     *   offset = i - first
     *
     * 🔑 WHY THE SAME "offset" WORKS FOR ALL FOUR SIDES:
     * A layer is a square ring, so every one of its four edges has the
     * exact same length (last - first). Walking `offset` steps along the
     * top edge (left→right), the right edge (top→bottom), the bottom edge
     * (right→left), and the left edge (bottom→top) all land you on the
     * four elements of the *same* 4-cycle. That symmetry is what lets one
     * `offset` variable index into all four sides correctly.
     *
     * ---------------------------------------------------------------
     * 🔄 4-WAY SWAP (KEY LOGIC)
     * ---------------------------------------------------------------
     *
     * We perform rotation in this order:
     *
     *   1. Save top
     *   2. left → top
     *   3. bottom → left
     *   4. right → bottom
     *   5. top → right
     *
     * This ensures no data is lost.
     *
     * ---------------------------------------------------------------
     * 🧮 WORKED TRACE (3x3, layer 0 only — the only layer that exists)
     * ---------------------------------------------------------------
     *
     * Start:
     *   1 2 3
     *   4 5 6
     *   7 8 9
     *
     * first = 0, last = 2. Loop runs for i = 0, then i = 1 (i < last, so i = 2 is skipped).
     *
     * i = 0 → offset = 0:
     *   top    = matrix[0][0] = 1
     *   matrix[0][0] = matrix[2][0]        → matrix[0][0] = 7        (left  → top)
     *   matrix[2][0] = matrix[2][2]        → matrix[2][0] = 9        (bottom→ left)
     *   matrix[2][2] = matrix[0][2]        → matrix[2][2] = 3        (right → bottom)
     *   matrix[0][2] = top                 → matrix[0][2] = 1        (top   → right)
     *
     *   Matrix is now:
     *     7 2 1
     *     4 5 6
     *     9 8 3
     *
     * i = 1 → offset = 1:
     *   top    = matrix[0][1] = 2
     *   matrix[0][1] = matrix[1][0]        → matrix[0][1] = 4        (left  → top)
     *   matrix[1][0] = matrix[2][1]        → matrix[1][0] = 8        (bottom→ left)
     *   matrix[2][1] = matrix[1][2]        → matrix[2][1] = 6        (right → bottom)
     *   matrix[1][2] = top                 → matrix[1][2] = 2        (top   → right)
     *
     *   Matrix is now (final result):
     *     7 4 1
     *     8 5 2
     *     9 6 3
     *
     * Matches the expected rotated matrix from the intuition section above. ✔
     *
     * ---------------------------------------------------------------
     * ❓ WHY THE LOOP STOPS AT `i < last` (NOT `i <= last`)
     * ---------------------------------------------------------------
     *
     * At i = last, the "top" element (first, last) is the same element as
     * the "right" element of i = first (offset = 0) — it's the ring's
     * top-right corner, which is already swapped in as part of that first
     * (offset = 0) iteration's "top → right" step. Including i = last
     * would re-process a corner that's already been moved, corrupting it.
     * Each of the 4 corners of a ring is touched by exactly one offset
     * value (offset = 0), so `i` only needs to range over [first, last).
     *
     * ---------------------------------------------------------------
     * 🔁 ALTERNATIVE APPROACH: TRANSPOSE + REVERSE ROWS
     * ---------------------------------------------------------------
     *
     * A simpler (and more commonly recalled under interview pressure)
     * in-place method:
     *
     *   1. Transpose the matrix: matrix[i][j] <-> matrix[j][i] for i < j
     *   2. Reverse each row
     *
     * Example (3x3):
     *   1 2 3        1 4 7        7 4 1
     *   4 5 6   →    2 5 8   →    8 5 2
     *   7 8 9        3 6 9        9 6 3
     *   (original)   (transposed) (rows reversed = final)
     *
     * Same O(n^2) time, O(1) space as the layer method. The layer/4-way-
     * swap approach above does it in a single pass without a second full
     * traversal, which is why it's often asked as the "true" in-place
     * version — but transpose+reverse is worth having ready as a
     * fallback since it's far easier to derive correctly live.
     *
     * ---------------------------------------------------------------
     * ⚠️ EDGE CASES
     * ---------------------------------------------------------------
     *
     *   n = 0 (empty matrix) → n/2 = 0, outer loop never runs, returns as-is. ✔
     *   n = 1 (single cell)  → n/2 = 0, outer loop never runs, returns as-is. ✔
     *   n = 2                → 1 layer, first=0/last=1, inner loop runs once (i=0). ✔
     *   Non-square matrix    → NOT handled; this algorithm assumes n x n.
     *                          A rectangular matrix requires a different (or new) output matrix.
     *
     * ---------------------------------------------------------------
     * ⏱ COMPLEXITY
     * ---------------------------------------------------------------
     * Time  : O(n^2)  — every element is touched exactly once
     * Space : O(1)    — in-place, only a few scalar temp variables
     *
     * ---------------------------------------------------------------
     * 🧠 INTERVIEW THINKING FLOW
     * ---------------------------------------------------------------
     *
     * 1. Try coordinate mapping:
     *      (i, j) → (j, n-1-i)
     *
     * 2. Realize direct mapping overwrites values ❌
     *
     * 3. Think:
     *      "Can I rotate in cycles?"
     *
     * 4. Arrive at:
     *      ✔ 4-way swaps
     *      ✔ Layer-by-layer processing
     *
     * 5. If stuck mid-interview, fall back to transpose + reverse —
     *    it's a valid, easier-to-derive in-place solution too.
     *
     * ================================================================
     */
    public static int[][] rotate(int[][] matrix) {
        int n = matrix.length;

        // ------------------------------------------------------------
        // Process each "layer" (outer → inner)
        // Total layers = n / 2
        // ------------------------------------------------------------
        for (int layer = 0; layer < n / 2; layer++) {

            int first = layer;            // starting index of current layer
            int last = n - 1 - layer;     // ending index of current layer

            // --------------------------------------------------------
            // Traverse elements in the current layer
            // Only go till last - 1 (because last element is handled in cycle)
            // See "WHY THE LOOP STOPS AT i < last" above for the full reasoning.
            // --------------------------------------------------------
            for (int i = first; i < last; i++) {

                int offset = i - first; // distance from starting index

                // ----------------------------------------------------
                // Identify 4 positions involved in rotation
                // ----------------------------------------------------
                int top = matrix[first][i]; // store TOP (will be overwritten)

                // ----------------------------------------------------
                // Perform 4-way rotation
                // ----------------------------------------------------

                // LEFT → TOP
                matrix[first][i] = matrix[last - offset][first];

                // BOTTOM → LEFT
                matrix[last - offset][first] = matrix[last][last - offset];

                // RIGHT → BOTTOM
                matrix[last][last - offset] = matrix[i][last];

                // TOP → RIGHT
                matrix[i][last] = top;
            }
        }

        return matrix; // modified in-place
    }

    public static void main(String[] args) {

        int[][] matrix = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };

        rotate(matrix);

        // ------------------------------------------------------------
        // Print rotated matrix
        // ------------------------------------------------------------
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }
}

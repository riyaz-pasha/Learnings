class Search2DMatrixII {

    // Approach 1: Staircase Search from Top-Right (Optimal)
    // Time: O(m+n), Space: O(1)
    public boolean searchMatrix1(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        int row = 0;
        int col = matrix[0].length - 1; // Start from top-right corner

        while (row < matrix.length && col >= 0) {
            if (matrix[row][col] == target) {
                return true;
            } else if (matrix[row][col] > target) {
                // Current value too large, move left (eliminate column)
                col--;
            } else {
                // Current value too small, move down (eliminate row)
                row++;
            }
        }

        return false;
    }

    // Approach 2: Staircase Search from Bottom-Left (Optimal)
    // Time: O(m+n), Space: O(1)
    public boolean searchMatrix2(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        int row = matrix.length - 1; // Start from bottom-left corner
        int col = 0;

        while (row >= 0 && col < matrix[0].length) {
            if (matrix[row][col] == target) {
                return true;
            } else if (matrix[row][col] > target) {
                // Current value too large, move up (eliminate row)
                row--;
            } else {
                // Current value too small, move right (eliminate column)
                col++;
            }
        }

        return false;
    }

    // Approach 3: Binary Search on Each Row
    // Time: O(m * log(n)), Space: O(1)
    public boolean searchMatrix3(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        // Search each row using binary search
        for (int i = 0; i < matrix.length; i++) {
            // Early termination: if target < first element, skip remaining rows
            if (target < matrix[i][0]) {
                break;
            }
            // Early termination: if target > last element, skip this row
            if (target > matrix[i][matrix[i].length - 1]) {
                continue;
            }

            if (binarySearch(matrix[i], target)) {
                return true;
            }
        }

        return false;
    }

    private boolean binarySearch(int[] arr, int target) {
        int left = 0, right = arr.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) {
                return true;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

    // Approach 4: Divide and Conquer (Recursive)
    // Time: O(m+n), Space: O(log(m+n)) for recursion
    public boolean searchMatrix4(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        return searchHelper(matrix, target, 0, 0,
                matrix.length - 1, matrix[0].length - 1);
    }

    private boolean searchHelper(int[][] matrix, int target,
            int rowStart, int colStart,
            int rowEnd, int colEnd) {
        // Base cases
        if (rowStart > rowEnd || colStart > colEnd) {
            return false;
        }

        if (target < matrix[rowStart][colStart] ||
                target > matrix[rowEnd][colEnd]) {
            return false;
        }

        // Search middle column
        int midCol = colStart + (colEnd - colStart) / 2;
        int row = rowStart;

        while (row <= rowEnd && matrix[row][midCol] <= target) {
            if (matrix[row][midCol] == target) {
                return true;
            }
            row++;
        }

        // Search bottom-left and top-right quadrants
        return searchHelper(matrix, target, row, colStart, rowEnd, midCol - 1) ||
                searchHelper(matrix, target, rowStart, midCol + 1, row - 1, colEnd);
    }

    // Approach 5: Brute Force (For comparison)
    // Time: O(m*n), Space: O(1)
    public boolean searchMatrix5(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                if (matrix[i][j] == target) {
                    return true;
                }
            }
        }

        return false;
    }

    // Test cases with visualization
    public static void main(String[] args) {
        Search2DMatrixII solution = new Search2DMatrixII();

        // Test Case 1
        int[][] matrix1 = {
                { 1, 4, 7, 11, 15 },
                { 2, 5, 8, 12, 19 },
                { 3, 6, 9, 16, 22 },
                { 10, 13, 14, 17, 24 },
                { 18, 21, 23, 26, 30 }
        };
        int target1 = 5;
        boolean result1 = solution.searchMatrix1(matrix1, target1);
        System.out.println("Test 1 - Target " + target1 + ": " + result1); // true
        visualizeSearch(matrix1, target1);

        // Test Case 2
        int target2 = 20;
        boolean result2 = solution.searchMatrix1(matrix1, target2);
        System.out.println("\nTest 2 - Target " + target2 + ": " + result2); // false
        visualizeSearch(matrix1, target2);

        // Test Case 3: Target at corner
        int target3 = 1;
        boolean result3 = solution.searchMatrix1(matrix1, target3);
        System.out.println("\nTest 3 - Target " + target3 + ": " + result3); // true

        // Test Case 4: Target at corner
        int target4 = 30;
        boolean result4 = solution.searchMatrix1(matrix1, target4);
        System.out.println("Test 4 - Target " + target4 + ": " + result4); // true

        // Test Case 5: Small matrix
        int[][] matrix2 = { { -5 } };
        int target5 = -5;
        boolean result5 = solution.searchMatrix1(matrix2, target5);
        System.out.println("Test 5 - Target " + target5 + ": " + result5); // true

        // Compare all approaches
        System.out.println("\nComparing all approaches for target 5:");
        System.out.println("Approach 1 (Top-Right): " +
                solution.searchMatrix1(matrix1, target1));
        System.out.println("Approach 2 (Bottom-Left): " +
                solution.searchMatrix2(matrix1, target1));
        System.out.println("Approach 3 (Binary Search Rows): " +
                solution.searchMatrix3(matrix1, target1));
        System.out.println("Approach 4 (Divide & Conquer): " +
                solution.searchMatrix4(matrix1, target1));
        System.out.println("Approach 5 (Brute Force): " +
                solution.searchMatrix5(matrix1, target1));
    }

    private static void visualizeSearch(int[][] matrix, int target) {
        System.out.println("Matrix:");
        for (int[] row : matrix) {
            System.out.print("[");
            for (int j = 0; j < row.length; j++) {
                System.out.printf("%3d", row[j]);
                if (j < row.length - 1)
                    System.out.print(", ");
            }
            System.out.println("]");
        }

        // Trace the staircase search path from top-right
        System.out.println("\nSearch path from top-right:");
        int row = 0;
        int col = matrix[0].length - 1;
        int step = 1;

        while (row < matrix.length && col >= 0) {
            System.out.printf("Step %d: [%d][%d] = %d",
                    step++, row, col, matrix[row][col]);

            if (matrix[row][col] == target) {
                System.out.println(" ✓ Found!");
                return;
            } else if (matrix[row][col] > target) {
                System.out.println(" > " + target + " → move left");
                col--;
            } else {
                System.out.println(" < " + target + " → move down");
                row++;
            }
        }

        System.out.println("Not found");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM CHARACTERISTICS:
 * - Each row sorted left to right
 * - Each column sorted top to bottom
 * - NOT guaranteed that first element of row > last element of previous row
 * 
 * KEY INSIGHT - STAIRCASE SEARCH:
 * Start from top-right (or bottom-left) corner:
 * - Top-right: smallest in its column, largest in its row
 * - Can eliminate either entire row or entire column with each comparison
 * 
 * WHY TOP-RIGHT WORKS:
 * Position: [row][col] where col = last column
 * 
 * If matrix[row][col] == target: Found!
 * If matrix[row][col] > target:
 * - All elements below in this column are larger
 * - Eliminate entire column, move left: col--
 * If matrix[row][col] < target:
 * - All elements to the left in this row are smaller
 * - Eliminate entire row, move down: row++
 * 
 * Each step eliminates one row or column → O(m+n) total steps
 * 
 * TIME COMPLEXITY PROOF - WHY O(m+n)?
 * 
 * Key observation: We can only make TWO types of moves:
 * 1. Move DOWN (row++): increases row by 1
 * 2. Move LEFT (col--): decreases col by 1
 * 
 * BOUNDS ON MOVES:
 * - Start: row = 0, col = n-1
 * - Maximum DOWN moves: We can go from row 0 to row m-1
 * → At most m DOWN moves
 * - Maximum LEFT moves: We can go from col n-1 to col 0
 * → At most n LEFT moves
 * 
 * TOTAL MOVES = DOWN moves + LEFT moves ≤ m + n
 * 
 * Since each move is O(1), total time = O(m + n)
 * 
 * VISUAL EXAMPLE (5x5 matrix):
 * Starting position: [0][4] (top-right)
 * 
 * Worst case path to bottom-left [4][0]:
 * ↓ ↓ ↓ ↓ ← ← ← ←
 * 4 down moves + 4 left moves = 8 total moves = m + n - 2
 * 
 * Even if we don't reach corners:
 * - Can't move down more than m times (only m rows)
 * - Can't move left more than n times (only n columns)
 * - Maximum possible moves = m + n
 * 
 * IMPORTANT: It's O(m+n), NOT O(m×n) because:
 * - We don't visit every cell (that would be O(m×n))
 * - We eliminate entire rows/columns with each move
 * - We follow a single path through the matrix
 * - Path length is bounded by m + n
 * 
 * MATHEMATICAL PROOF:
 * Let d = number of DOWN moves, l = number of LEFT moves
 * - d ≤ m (can't go down more than m rows)
 * - l ≤ n (can't go left more than n columns)
 * - Total moves = d + l ≤ m + n
 * - Therefore: T(m,n) = O(m + n)
 * 
 * VISUALIZATION for matrix and target=5:
 * 
 * Matrix:
 * [ 1, 4, 7, 11, 15] ← Start here (top-right)
 * [ 2, 5, 8, 12, 19]
 * [ 3, 6, 9, 16, 22]
 * [ 10, 13, 14, 17, 24]
 * [ 18, 21, 23, 26, 30]
 * 
 * Search Path:
 * 1. [0][4] = 15 > 5 → move left
 * 2. [0][3] = 11 > 5 → move left
 * 3. [0][2] = 7 > 5 → move left
 * 4. [0][1] = 4 < 5 → move down
 * 5. [1][1] = 5 = 5 → Found! ✓
 * 
 * STAIRCASE PATTERN:
 * →→→
 * ↓
 * ↓
 * ↓
 * The path forms a staircase shape!
 * 
 * EXAMPLE 2 - Target 20:
 * 1. [0][4] = 15 < 20 → move down
 * 2. [1][4] = 19 < 20 → move down
 * 3. [2][4] = 22 > 20 → move left
 * 4. [2][3] = 16 < 20 → move down
 * 5. [3][3] = 17 < 20 → move down
 * 6. [4][3] = 26 > 20 → move left
 * 7. [4][2] = 23 > 20 → move left
 * 8. [4][1] = 21 > 20 → move left
 * 9. [4][0] = 18 < 20 → move down
 * 10. Out of bounds → Not found
 * 
 * WHY NOT START FROM TOP-LEFT OR BOTTOM-RIGHT?
 * Top-left [0][0]: smallest in row and column
 * - If target > current: could be right OR down (ambiguous!)
 * - Cannot eliminate entire row or column
 * 
 * Bottom-right [m-1][n-1]: largest in row and column
 * - If target < current: could be left OR up (ambiguous!)
 * - Cannot eliminate entire row or column
 * 
 * Only top-right and bottom-left provide decisive elimination!
 * 
 * COMPLEXITY ANALYSIS:
 * 
 * Approach 1 & 2 (Staircase Search):
 * - Time: O(m + n)
 * - Maximum m steps down + n steps left (or vice versa)
 * - Each step: O(1) comparison
 * - Space: O(1) - only track row and col
 * 
 * Approach 3 (Binary Search Each Row):
 * - Time: O(m * log n)
 * - m rows, each binary search takes log n
 * - Space: O(1)
 * 
 * Approach 4 (Divide & Conquer):
 * - Time: O(m + n) average case
 * - Space: O(log(m*n)) - recursion stack
 * 
 * Approach 5 (Brute Force):
 * - Time: O(m * n) - check every element
 * - Space: O(1)
 * 
 * EDGE CASES:
 * 1. Empty matrix: return false
 * 2. Single element: direct comparison
 * 3. Target smaller than min: early termination
 * 4. Target larger than max: early termination
 * 5. Target at corners: handled correctly
 * 6. 1xn or mx1 matrix: degrades to linear search
 * 
 * WHY STAIRCASE IS OPTIMAL:
 * 1. Uses sorted property of both rows and columns
 * 2. Each comparison eliminates maximum elements
 * 3. No backtracking needed
 * 4. No extra space required
 * 5. Simple and elegant implementation
 * 
 * COMPARISON WITH OTHER MATRIX PROBLEMS:
 * 
 * "Search 2D Matrix I":
 * - Each row's first element > previous row's last element
 * - Can treat as sorted 1D array → O(log(m*n))
 * 
 * "Search 2D Matrix II" (this problem):
 * - Weaker constraint (only row/column sorted)
 * - Cannot flatten to 1D → Need staircase search O(m+n)
 * 
 * PRACTICAL APPLICATIONS:
 * - Database indexing with multi-dimensional sorting
 * - Game boards (chess ratings, tournament rankings)
 * - Image processing (gradient-sorted pixels)
 * - Spatial data structures
 * 
 * INTERVIEW TIPS:
 * 1. Clarify matrix properties (sorted rows/columns)
 * 2. Mention why top-right or bottom-left
 * 3. Explain elimination strategy clearly
 * 4. Analyze time/space complexity
 * 5. Handle edge cases (empty, single element)
 * 6. Mention this beats binary search per row!
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Pascal's Triangle
 * Given an integer numRows, generate the first numRows of Pascal's triangle.
 * In Pascal's triangle, each element is formed by adding the two numbers 
 * directly above it from the previous row.
 * 
 * Constraints:
 * 1 <= numRows <= 30
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, although this seems like a simple array manipulation 
 * problem, you can demonstrate system awareness through your clarifying questions:
 * 
 * Q: "For numRows = 30, what is the maximum value in the triangle, and will it 
 *     overflow a 32-bit signed integer?"
 * A: The maximum value in Pascal's triangle for n rows is the middle element 
 *    of the last row. For row 30 (0-indexed row 29), this is 29 Choose 14, 
 *    which is 77,558,760. This safely fits within a standard 32-bit signed 
 *    integer (up to ~2.1 billion). We do not need `long`.
 * 
 * Q: "What are the space complexity constraints?"
 * A: Since we must *return* the entire triangle, the output structure itself 
 *    mandates O(numRows^2) space. However, we can optimize our *auxiliary* 
 *    space (memory used beyond the required output).
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "Pascal's Triangle is fundamentally a dynamic programming problem! 
 * At any given cell at (row, col), I have two dependencies:
 *  1. The cell directly above-left: (row - 1, col - 1)
 *  2. The cell directly above-right: (row - 1, col)
 * 
 * The edges of the triangle (where col == 0 or col == row) act as our base 
 * cases and are always firmly set to 1.
 * 
 * We can solve this top-down (recursively finding each cell's value by 
 * branching upwards) or bottom-up (tabulation, which is the most natural 
 * way humans construct the triangle)."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: numRows = 5
 * 
 * Row 0: [1]
 * Row 1: [1, 1]
 * Row 2: [1, 2, 1]       -> dp[2][1] = dp[1][0] + dp[1][1] = 1 + 1 = 2
 * Row 3: [1, 3, 3, 1]    -> dp[3][1] = dp[2][0] + dp[2][1] = 1 + 2 = 3
 * Row 4: [1, 4, 6, 4, 1] -> dp[4][2] = dp[3][1] + dp[3][2] = 3 + 3 = 6
 */
public class PascalsTriangle {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: To build the triangle, we calculate every single cell recursively.
     * 
     * Time Complexity: O(2^N) for the middle elements. Calculating the whole 
     *                  triangle this way is highly inefficient.
     * Space Complexity: O(N) auxiliary recursion stack depth.
     */
    public List<List<Integer>> generateRecursive(int numRows) {
        List<List<Integer>> result = new ArrayList<>();
        
        for (int i = 0; i < numRows; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                // Recursively fetch the mathematical value for this specific cell
                row.add(getPascalValueRecursive(i, j));
            }
            result.add(row);
        }
        
        return result;
    }

    private int getPascalValueRecursive(int row, int col) {
        // BASE CASE REASONING:
        // If we hit the absolute left edge (col == 0) or the absolute right 
        // edge (col == row), the value is physically bounded to 1.
        if (col == 0 || col == row) {
            return 1;
        }
        
        // Otherwise, it is the sum of the two cells directly above it.
        return getPascalValueRecursive(row - 1, col - 1) + getPascalValueRecursive(row - 1, col);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the recursive cell calculations so we never compute the 
     * same Pascal coordinate twice.
     * 
     * Time Complexity: O(N^2) - We evaluate each cell exactly once.
     * Space Complexity: O(N^2) - For the 2D memo array + call stack.
     */
    public List<List<Integer>> generateMemo(int numRows) {
        List<List<Integer>> result = new ArrayList<>();
        
        // memo[row][col]
        Integer[][] memo = new Integer[numRows][numRows];
        
        for (int i = 0; i < numRows; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                row.add(getPascalValueMemo(i, j, memo));
            }
            result.add(row);
        }
        
        return result;
    }

    private int getPascalValueMemo(int row, int col, Integer[][] memo) {
        // BASE CASES (Same mathematical constraints as above)
        if (col == 0 || col == row) return 1;
        
        if (memo[row][col] != null) {
            return memo[row][col];
        }
        
        memo[row][col] = getPascalValueMemo(row - 1, col - 1, memo) + 
                         getPascalValueMemo(row - 1, col, memo);
                         
        return memo[row][col];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Construct the triangle sequentially. Row 'i' only requires reading 
     * from Row 'i-1'. This is the standard, most practical DP solution.
     * 
     * Time Complexity: O(numRows^2) - We visit each cell exactly once.
     * Space Complexity: O(1) auxiliary - We only use the required output array!
     */
    public List<List<Integer>> generateTabulation(int numRows) {
        List<List<Integer>> result = new ArrayList<>();
        
        if (numRows == 0) return result;

        // BASE CASE REASONING (Seeding the top of the triangle):
        // Row 0 is just a single [1]. We must seed this to anchor the logic 
        // for all subsequent rows.
        List<Integer> firstRow = new ArrayList<>();
        firstRow.add(1);
        result.add(firstRow);

        // We start iterating from Row 1, building down to numRows - 1.
        for (int i = 1; i < numRows; i++) {
            
            // This list will represent our CURRENT row being built.
            List<Integer> currentRow = new ArrayList<>();
            
            // We fetch the PREVIOUS row from our result list to act as our DP history.
            List<Integer> previousRow = result.get(i - 1);
            
            // --- DETAILED TABULATION EXPLANATION ---
            
            // 1. The Left Edge:
            // Every row in Pascal's Triangle strictly begins with a 1.
            currentRow.add(1);
            
            // 2. The Internal Elements:
            // We loop through the inner columns. 
            // Notice we start at j=1 (skipping the left edge) and end at j<i (skipping the right edge).
            for (int j = 1; j < i; j++) {
                
                // We look at the cell directly above-left in the previous row.
                int aboveLeft = previousRow.get(j - 1);
                
                // We look at the cell directly above-right in the previous row.
                int aboveRight = previousRow.get(j);
                
                // The current cell is the sum of these two historical values.
                currentRow.add(aboveLeft + aboveRight);
            }
            
            // 3. The Right Edge:
            // Every row in Pascal's Triangle strictly ends with a 1.
            currentRow.add(1);
            
            // We officially lock our current row into the final result grid.
            result.add(currentRow);
        }

        return result;
    }

    /**
     * ========================================================================
     * APPROACH 4: Combinatorics Math (L4/L5 Target Flex)
     * ========================================================================
     * Idea: In Tabulation, generating a row requires looking at the previous row. 
     * However, Pascal's triangle represents the Binomial Coefficients (n Choose k).
     * 
     * We can generate ANY element in O(1) time based purely on the element before 
     * it in the same row, using the mathematical property:
     * C(n, k) = C(n, k-1) * (n - k + 1) / k
     * 
     * This means we can generate the entire triangle without EVER referencing 
     * previous rows, entirely decoupling the DP states!
     * 
     * Time Complexity: O(numRows^2)
     * Space Complexity: O(1) auxiliary.
     */
    public List<List<Integer>> generateMathOptimized(int numRows) {
        List<List<Integer>> result = new ArrayList<>();
        
        // Loop through each row 'r' (0-indexed)
        for (int r = 0; r < numRows; r++) {
            List<Integer> row = new ArrayList<>();
            
            // The first element is always 1 (r Choose 0)
            long value = 1; 
            row.add((int) value);
            
            // Calculate subsequent elements in the same row iteratively
            for (int c = 1; c <= r; c++) {
                // Apply the combinations formula efficiently:
                // C(r, c) = C(r, c-1) * (r - c + 1) / c
                // Note: We use `long` to prevent overflow during the multiplication 
                // step before the division happens.
                value = value * (r - c + 1) / c;
                
                row.add((int) value);
            }
            
            result.add(row);
        }
        
        return result;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new PascalsTriangle();
        
        List<Integer> testCases = Arrays.asList(1, 5, 10);
        
        for (int numRows : testCases) {
            System.out.println("---- Test Case: numRows = " + numRows + " ----");
            
            System.out.println("Tabulation Result:");
            List<List<Integer>> tabResult = solver.generateTabulation(numRows);
            for (List<Integer> row : tabResult) {
                System.out.println(row);
            }
            
            System.out.println("\nMath Optimized Result:");
            List<List<Integer>> mathResult = solver.generateMathOptimized(numRows);
            for (List<Integer> row : mathResult) {
                System.out.println(row);
            }
            System.out.println("\n");
        }
    }
}

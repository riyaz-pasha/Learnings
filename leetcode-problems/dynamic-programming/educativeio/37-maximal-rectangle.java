import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Maximal Rectangle
 * Given an m x n binary matrix filled with 0's and 1's, find the largest 
 * rectangle containing only 1's and return its area.
 * 
 * Constraints:
 * rows == matrix.length
 * cols == matrix[i].length
 * 1 <= rows, cols <= 200
 * matrix[i][j] is '0' or '1'
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an interview, it is crucial to draw the distinction between this problem 
 * and "Maximal Square". 
 * 
 * Q: "Why doesn't the 3-neighbor DP approach from Maximal Square work here?"
 * A: Squares have a fixed constraint: width == height. This means a single 
 *    variable (side length) perfectly describes the shape. Rectangles have 
 *    independent widths and heights. You cannot confidently combine a 3x1 
 *    rectangle and a 1x3 rectangle using just a neighbor lookup.
 * 
 * CRITICAL SENIOR INSIGHT: 
 * "Because rectangles have two degrees of freedom (width and height), standard 
 * Top-Down Memoization is highly unnatural and inefficient here. Instead, we 
 * must pivot. We can view the 1s in the matrix as columns of blocks, treating 
 * each row as the baseline of a Histogram. 
 * 
 * This beautifully transitions the problem from a standard 2D Grid DP into a 
 * 1D array optimization problem (Largest Rectangle in Histogram)."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "If we iterate row by row, we can maintain a running 'height' of consecutive 
 * 1s looking upwards. 
 * 
 * Example row heights:
 * Row 0: 1 0 1 0 0 -> Heights: [1, 0, 1, 0, 0]
 * Row 1: 1 0 1 1 1 -> Heights: [2, 0, 2, 1, 1]
 * Row 2: 1 1 1 1 1 -> Heights: [3, 1, 3, 2, 2]
 * 
 * For any given row, if we know the height of the bars standing on it, we just 
 * need to figure out how far each bar can expand to the LEFT and RIGHT before 
 * hitting a bar shorter than itself.
 * 
 * Area = height[j] * (right_boundary - left_boundary)
 * 
 * We can solve this with Width-Tracking DP, Boundary-Array DP, or the 
 * ultra-optimized Monotonic Stack."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING (Boundary DP Approach)
 * ----------------------------------------------------------------------------
 * Matrix:
 * 1 0 1
 * 1 1 1
 * 
 * Process Row 0:
 * Heights: [1, 0, 1]
 * Left bounds (first index where height is >= current):  [0, 0, 2]
 * Right bounds (first index where height is < current):  [1, 3, 3]
 * Area = height * (right - left):
 *   Col 0: 1 * (1 - 0) = 1
 *   Col 2: 1 * (3 - 2) = 1
 * Max Area = 1.
 * 
 * Process Row 1:
 * Heights: [2, 1, 2] (added to previous row's heights)
 * Left bounds:  [0, 0, 2]
 * Right bounds: [1, 3, 3]
 * Area = height * (right - left):
 *   Col 0: 2 * (1 - 0) = 2
 *   Col 1: 1 * (3 - 0) = 3 (The 1x3 rectangle across the bottom!)
 *   Col 2: 2 * (3 - 2) = 2
 * Max Area = 3.
 */
public class MaximalRectangle {

    /**
     * ========================================================================
     * APPROACH 1: Brute Force Expansion (Baseline)
     * ========================================================================
     * Idea: Treat every single '1' as the top-left corner of a potential rectangle. 
     * Expand to the right as far as possible to find the max width, then expand 
     * down row by row, shrinking the maximum allowed width if we hit a 0.
     * 
     * Time Complexity: O(M^2 * N) - We check every cell, and for each cell, we 
     * look down and right.
     * Space Complexity: O(1) auxiliary space.
     */
    public int maximalRectangleBruteForce(char[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        int maxArea = 0;
        int m = matrix.length;
        int n = matrix[0].length;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == '1') {
                    // Start expanding a rectangle with top-left at (i, j)
                    int currentMaxWidth = n; // Assume it can go all the way to the right
                    
                    for (int down = i; down < m; down++) {
                        int currentWidth = 0;
                        
                        // Measure the contiguous 1s in this specific row
                        while (currentWidth < currentMaxWidth && j + currentWidth < n && matrix[down][j + currentWidth] == '1') {
                            currentWidth++;
                        }
                        
                        // The rectangle is strictly bottlenecked by the shortest row we've seen so far
                        currentMaxWidth = currentWidth;
                        
                        // If the width drops to 0, we can't expand down any further.
                        if (currentMaxWidth == 0) break;
                        
                        int height = down - i + 1;
                        maxArea = Math.max(maxArea, currentMaxWidth * height);
                    }
                }
            }
        }
        return maxArea;
    }

    /**
     * ========================================================================
     * APPROACH 2: Dynamic Programming - Width Tracking
     * ========================================================================
     * Idea: Precompute the maximum consecutive 1s ending at every cell (moving left). 
     * This turns the innermost while-loop of the Brute Force approach into an O(1) lookup.
     * 
     * Time Complexity: O(M^2 * N) - Still exploring upwards for every cell.
     * Space Complexity: O(M * N) - For the dp array tracking widths.
     */
    public int maximalRectangleDPWidths(char[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int m = matrix.length;
        int n = matrix[0].length;
        int[][] dp = new int[m][n];
        int maxArea = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == '1') {
                    
                    // dp[i][j] stores the consecutive width of 1s ending here.
                    dp[i][j] = (j == 0) ? 1 : dp[i][j - 1] + 1;
                    
                    int width = dp[i][j];
                    
                    // Look UP row by row to build rectangles whose bottom-right is (i, j)
                    for (int k = i; k >= 0; k--) {
                        // The rectangle width is bottlenecked by the narrowest row
                        width = Math.min(width, dp[k][j]);
                        
                        if (width == 0) break;
                        
                        int height = i - k + 1;
                        maxArea = Math.max(maxArea, width * height);
                    }
                }
            }
        }
        return maxArea;
    }

    /**
     * ========================================================================
     * APPROACH 3: Dynamic Programming - Boundary Tracking (Optimal DP)
     * ========================================================================
     * Idea: Maintain 3 separate 1D arrays (`height`, `left`, `right`). 
     * As we move row by row, we calculate the bounds for the histogram bars.
     * 
     * Time Complexity: O(M * N) - We do three linear passes over the columns per row.
     * Space Complexity: O(N) - Storing only the 1D boundary arrays.
     */
    public int maximalRectangleDPBoundaries(char[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int m = matrix.length;
        int n = matrix[0].length;
        
        int[] height = new int[n];
        int[] left = new int[n]; // The leftmost index this bar can extend to
        int[] right = new int[n]; // The rightmost index this bar can extend to (exclusive)
        
        // Initialize right boundaries to 'n' (the absolute right edge)
        Arrays.fill(right, n);
        
        int maxArea = 0;

        for (int i = 0; i < m; i++) {
            int currentLeftBound = 0;
            int currentRightBound = n;
            
            // 1. Update Heights
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == '1') {
                    height[j]++;
                } else {
                    height[j] = 0;
                }
            }
            
            // 2. Update Left Boundaries (Sweep Left -> Right)
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == '1') {
                    // The left boundary is constrained by the tighter of:
                    // - The boundary from the row above (left[j])
                    // - The nearest '0' on the left in the current row (currentLeftBound)
                    left[j] = Math.max(left[j], currentLeftBound);
                } else {
                    // Reset. The next '1' we see will have a left boundary of AT LEAST j+1.
                    left[j] = 0; 
                    currentLeftBound = j + 1;
                }
            }
            
            // 3. Update Right Boundaries (Sweep Right -> Left)
            for (int j = n - 1; j >= 0; j--) {
                if (matrix[i][j] == '1') {
                    right[j] = Math.min(right[j], currentRightBound);
                } else {
                    right[j] = n; 
                    currentRightBound = j;
                }
            }
            
            // 4. Calculate Max Area for the current row
            for (int j = 0; j < n; j++) {
                maxArea = Math.max(maxArea, height[j] * (right[j] - left[j]));
            }
        }
        
        return maxArea;
    }

    /**
     * ========================================================================
     * APPROACH 4: Monotonic Stack / Histogram Optimization (L4/L5 Flex)
     * ========================================================================
     * Idea: We translate the 2D grid into a 1D "Largest Rectangle in Histogram" 
     * problem (LeetCode 84) for every single row. 
     * By using a Monotonic Stack, we can find the left and right boundaries 
     * of every bar in exactly one pass per row!
     * 
     * Time Complexity: O(M * N) - Pushing and popping from the stack takes O(N) amortized.
     * Space Complexity: O(N) - For the heights array and the stack.
     */
    public int maximalRectangleStack(char[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        
        int n = matrix[0].length;
        
        // We add an extra '0' at the end of the heights array. 
        // This acts as a sentinel value, guaranteeing that ALL remaining 
        // bars in the stack are forcefully popped and calculated at the end of the row.
        int[] heights = new int[n + 1];
        int maxArea = 0;

        for (char[] row : matrix) {
            
            // Update the histogram heights for the current row
            for (int j = 0; j < n; j++) {
                if (row[j] == '1') {
                    heights[j]++;
                } else {
                    heights[j] = 0; // The building drops to the ground!
                }
            }
            
            // Run the optimal Monotonic Stack algorithm on this histogram
            maxArea = Math.max(maxArea, calculateHistogramArea(heights));
        }
        
        return maxArea;
    }

    /**
     * Helper method: Standard solution for "Largest Rectangle in Histogram".
     */
    private int calculateHistogramArea(int[] heights) {
        Stack<Integer> stack = new Stack<>();
        int maxArea = 0;
        
        for (int i = 0; i < heights.length; i++) {
            
            // If the current bar is SHORTER than the bar at the top of the stack,
            // the stack's top bar CANNOT extend any further to the right.
            // It is mathematically "locked in", so we pop it and calculate its area.
            while (!stack.isEmpty() && heights[i] < heights[stack.peek()]) {
                
                int lockedHeight = heights[stack.pop()];
                
                // If the stack is empty, it means there were no bars to the left 
                // shorter than this one. Its width stretches all the way to index 0.
                // Otherwise, it stretches to the right of the NEW top of the stack.
                int width = stack.isEmpty() ? i : (i - stack.peek() - 1);
                
                maxArea = Math.max(maxArea, lockedHeight * width);
            }
            
            // We only push INDICES onto the stack, which allows us to calculate widths.
            stack.push(i);
        }
        
        return maxArea;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new MaximalRectangle();
        
        record TestCase(char[][] matrix, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new char[][]{
                {'1', '0', '1', '0', '0'},
                {'1', '0', '1', '1', '1'},
                {'1', '1', '1', '1', '1'},
                {'1', '0', '0', '1', '0'}
            }, 6), // The 2x3 rectangle in the middle
            
            new TestCase(new char[][]{
                {'0'}
            }, 0),
            
            new TestCase(new char[][]{
                {'1'}
            }, 1),
            
            new TestCase(new char[][]{
                {'1', '1', '1'},
                {'1', '1', '1'}
            }, 6)
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Expected Area: " + tc.expected);
            
            System.out.println("Brute Force (Expansion) : " + solver.maximalRectangleBruteForce(tc.matrix));
            System.out.println("DP (Width Tracking)     : " + solver.maximalRectangleDPWidths(tc.matrix));
            System.out.println("DP (Boundary Tracking)  : " + solver.maximalRectangleDPBoundaries(tc.matrix));
            System.out.println("Stack (Histogram DP)    : " + solver.maximalRectangleStack(tc.matrix));
            System.out.println();
        }
    }
}

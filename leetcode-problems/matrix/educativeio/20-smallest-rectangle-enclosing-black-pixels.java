/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n binary matrix where '0' represents a white pixel and 
 * '1' represents a black pixel. 
 * All black pixels form a single connected component.
 * We are given the coordinates (x, y) of one of these black pixels.
 * We need to find the area of the smallest axis-aligned rectangle that entirely 
 * encloses all the black pixels.
 * 
 * CRITICAL CONSTRAINT: The runtime complexity MUST be less than O(m * n).
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. What does it mean for the runtime to be less than O(m * n)?
 *    (It means we cannot simply scan every single pixel in the image using 
 *     standard nested loops, nor can we do a standard BFS/DFS if the '1's 
 *     span the entire matrix).
 * 2. Are the input coordinates (x, y) guaranteed to be a '1'?
 *    (Yes, the problem states they represent one of the black pixels).
 * 3. Can the image be a 1D array?
 *    (Yes, constraints say 1 <= m, n <= 100).
 * 4. Should the coordinates be 0-indexed?
 *    (Yes, x < m and y < n imply standard 0-based indexing).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Single Pixel:
 *    [[1]], x=0, y=0. The smallest rectangle is just 1x1. Area = 1.
 * 2. Entire Matrix is Black:
 *    [[1, 1], [1, 1]], x=0, y=0. Area = 4.
 * 3. Long Strip:
 *    [[0, 0, 0],
 *     [1, 1, 1],
 *     [0, 0, 0]]
 *    x=1, y=1. Area = 1 * 3 = 3.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force / BFS / DFS (O(M * N)):
 *    - Start at (x, y) and perform a DFS/BFS to visit all connected '1's.
 *    - Track the minimum and maximum row and column indices seen.
 *    - Area = (maxRow - minRow + 1) * (maxCol - minCol + 1).
 *    - Why it fails: In the worst case (the entire image is '1's), this visits 
 *      every cell, taking O(M * N) time, violating the strict constraint.
 * 
 * 2. Binary Search (The Optimal "Less than O(M*N)" Approach):
 *    - Since all '1's are connected, if we project them onto the X-axis (rows) 
 *      or Y-axis (cols), they form a continuous segment!
 *    - We can use Binary Search to find the exact boundaries:
 *      a) Top Boundary: Binary search rows between [0, x]. Find the FIRST row 
 *         that contains a '1'.
 *      b) Bottom Boundary: Binary search rows between [x + 1, m]. Find the FIRST 
 *         row that DOES NOT contain a '1'.
 *      c) Left Boundary: Binary search cols between [0, y]. Find the FIRST col 
 *         that contains a '1'.
 *      d) Right Boundary: Binary search cols between [y + 1, n]. Find the FIRST 
 *         col that DOES NOT contain a '1'.
 *    - Checking if a row contains a '1' takes O(N). Binary searching the rows 
 *      takes O(log M). So finding Top/Bottom takes O(N log M).
 *    - Finding Left/Right takes O(M log N).
 *    - Total Time: O(M log N + N log M), which is strictly less than O(M * N).
 *    - Space: O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Binary Search Approach)
 * ============================================================================
 * Image:
 * 0 0 1 0
 * 0 1 1 0
 * 0 1 0 0
 * 
 * Given x = 0, y = 2 (The '1' at the top).
 * 
 * 1. Find TOP (range [0, 0]): 
 *    - Since x=0, top is obviously 0.
 * 2. Find BOTTOM (range [1, 3]):
 *    - mid = 2. Row 2 has a '1'. So bottom is at least 3. (low = mid + 1 -> 3).
 *    - Bound found at 3. (Exclusive bottom bound).
 * 3. Find LEFT (range [0, 2]):
 *    - mid = 1. Col 1 has a '1' (at row 1, 2). So left might be earlier, search [0, 1].
 *    - mid = 0. Col 0 has no '1'. low = mid + 1 = 1.
 *    - Left bound found at 1.
 * 4. Find RIGHT (range [3, 4]):
 *    - mid = 3. Col 3 has no '1'. high = mid = 3.
 *    - Right bound found at 3. (Exclusive right bound).
 * 
 * Area = (Bottom - Top) * (Right - Left)
 * Area = (3 - 0) * (3 - 1) = 3 * 2 = 6.
 */

public class SmallestRectangleEnclosingBlackPixels {

    public static void main(String[] args) {
        int[][] image = {
            {0, 0, 1, 0},
            {0, 1, 1, 0},
            {0, 1, 0, 0}
        };
        int x = 0, y = 2;

        System.out.println("--- Test Case 1 ---");
        System.out.println("Smallest Rectangle Area: " + minArea(image, x, y)); // Expected: 6
    }

    /**
     * SOLUTION: Binary Search for Boundaries
     * 
     * Time Complexity: O(M log N + N log M)
     * Space Complexity: O(1)
     */
    public static int minArea(int[][] image, int x, int y) {
        if (image == null || image.length == 0 || image[0].length == 0) return 0;
        
        int m = image.length;
        int n = image[0].length;
        
        // Search Top: Find the first row in [0, x] that contains a '1'.
        int top = searchRows(image, 0, x, 0, n - 1, true);
        
        // Search Bottom: Find the first row in [x + 1, m] that DOES NOT contain a '1'.
        // (This gives us the exclusive lower bound).
        int bottom = searchRows(image, x + 1, m, 0, n - 1, false);
        
        // Search Left: Find the first column in [0, y] that contains a '1'.
        // Optimization: We only need to check within the rows [top, bottom - 1].
        int left = searchCols(image, 0, y, top, bottom - 1, true);
        
        // Search Right: Find the first column in [y + 1, n] that DOES NOT contain a '1'.
        int right = searchCols(image, y + 1, n, top, bottom - 1, false);
        
        // Area calculation using the exclusive bounds (bottom and right).
        return (bottom - top) * (right - left);
    }

    /**
     * Binary Search on Rows.
     * @param searchFirst If true, finds the FIRST row WITH a '1'.
     *                    If false, finds the FIRST row WITHOUT a '1'.
     */
    private static int searchRows(int[][] image, int low, int high, int left, int right, boolean searchFirst) {
        while (low < high) {
            int mid = low + (high - low) / 2;
            boolean hasBlackPixel = false;
            
            // Check if the current row 'mid' has a black pixel within the column bounds
            for (int j = left; j <= right; j++) {
                if (image[mid][j] == 1) {
                    hasBlackPixel = true;
                    break;
                }
            }
            
            if (hasBlackPixel == searchFirst) {
                // If we are looking for the first '1' and found it, search higher up (smaller index).
                // If we are looking for the first '0' and found a '1', search lower down (larger index).
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    /**
     * Binary Search on Columns.
     * @param searchFirst If true, finds the FIRST col WITH a '1'.
     *                    If false, finds the FIRST col WITHOUT a '1'.
     */
    private static int searchCols(int[][] image, int low, int high, int top, int bottom, boolean searchFirst) {
        while (low < high) {
            int mid = low + (high - low) / 2;
            boolean hasBlackPixel = false;
            
            // Check if the current column 'mid' has a black pixel within the row bounds
            for (int i = top; i <= bottom; i++) {
                if (image[i][mid] == 1) {
                    hasBlackPixel = true;
                    break;
                }
            }
            
            if (hasBlackPixel == searchFirst) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }
    
    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if the black pixels could be disjoint (multiple separate islands)?
     *    Answer: If there are multiple disconnected islands, projecting them to 
     *    the 1D axes no longer guarantees a contiguous block of 1s (e.g., there 
     *    could be a row of entirely 0s between two islands of 1s). In this case, 
     *    Binary Search fails. You would HAVE to use DFS/BFS (which takes O(M*N)) 
     *    to trace the specific connected component attached to (x,y).
     * 
     * 2. Why does checking rows only from 'left' to 'right' work? 
     *    Answer: When finding the Top and Bottom bounds, we scan the full width 
     *    (0 to n-1). However, once Top and Bottom are found, we only need to scan 
     *    the columns between 'top' and 'bottom - 1'. This is a minor but effective 
     *    optimization that limits unnecessary scanning.
     */
}

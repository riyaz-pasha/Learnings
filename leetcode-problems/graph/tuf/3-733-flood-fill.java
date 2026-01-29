import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/*
 * You are given an image represented by an m x n grid of integers image, where
 * image[i][j] represents the pixel value of the image. You are also given three
 * integers sr, sc, and color. Your task is to perform a flood fill on the image
 * starting from the pixel image[sr][sc].
 * 
 * To perform a flood fill:
 * 
 * Begin with the starting pixel and change its color to color.
 * Perform the same process for each pixel that is directly adjacent (pixels
 * that share a side with the original pixel, either horizontally or vertically)
 * and shares the same color as the starting pixel.
 * Keep repeating this process by checking neighboring pixels of the updated
 * pixels and modifying their color if it matches the original color of the
 * starting pixel.
 * The process stops when there are no more adjacent pixels of the original
 * color to update.
 * Return the modified image after performing the flood fill.
 * 
 * Example 1:
 * Input: image = [[1,1,1],[1,1,0],[1,0,1]], sr = 1, sc = 1, color = 2
 * Output: [[2,2,2],[2,2,0],[2,0,1]]
 * Explanation:
 * From the center of the image with position (sr, sc) = (1, 1) (i.e., the red
 * pixel), all pixels connected by a path of the same color as the starting
 * pixel (i.e., the blue pixels) are colored with the new color.
 * 
 * Note the bottom corner is not colored 2, because it is not horizontally or
 * vertically connected to the starting pixel.
 * 
 * Example 2:
 * Input: image = [[0,0,0],[0,0,0]], sr = 0, sc = 0, color = 0
 * Output: [[0,0,0],[0,0,0]]
 * Explanation:
 * The starting pixel is already colored with 0, which is the same as the target
 * color. Therefore, no changes are made to the image.
 */

class FloodFill {

    // 4-directional moves (up, down, right, left)
    private static final int[][] DIRECTIONS = { { -1, 0 }, { 1, 0 }, { 0, 1 }, { 0, -1 } };

    // Simple, immutable position value
    private record Position(int row, int col) {
    }

    /**
     * Performs flood fill on the given image starting from (sr, sc) replacing the
     * connected area of the starting pixel's color with the new color.
     *
     * Returns the modified image (in-place).
     */
    public int[][] floodFill(int[][] image, int sr, int sc, int color) {
        // Guards
        if (image == null || image.length == 0 || image[0].length == 0)
            return image;
        int rows = image.length;
        int cols = image[0].length;
        if (!inBounds(sr, sc, rows, cols))
            return image;

        int targetColor = image[sr][sc];
        // Nothing to do if the new color equals the target color
        if (targetColor == color)
            return image;

        Queue<Position> queue = new LinkedList<>();
        queue.add(new Position(sr, sc));
        image[sr][sc] = color;

        while (!queue.isEmpty()) {
            Position pos = queue.poll();
            for (int[] d : DIRECTIONS) {
                int nr = pos.row() + d[0];
                int nc = pos.col() + d[1];
                if (inBounds(nr, nc, rows, cols) && image[nr][nc] == targetColor) {
                    image[nr][nc] = color;
                    queue.offer(new Position(nr, nc));
                }
            }
        }

        return image;
    }

    private boolean inBounds(int r, int c, int rows, int cols) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

}

// Solution 1: DFS (Depth-First Search) - Recursive
class Solution {
    public int[][] floodFill(int[][] image, int sr, int sc, int color) {
        int originalColor = image[sr][sc];

        // If the starting pixel is already the target color, no need to proceed
        if (originalColor == color) {
            return image;
        }

        dfs(image, sr, sc, originalColor, color);
        return image;
    }

    private void dfs(int[][] image, int row, int col, int originalColor, int newColor) {
        // Boundary checks and color validation
        if (row < 0 || row >= image.length || col < 0 || col >= image[0].length) {
            return;
        }

        if (image[row][col] != originalColor) {
            return;
        }

        // Change the color
        image[row][col] = newColor;

        // Recursively fill all 4 adjacent cells
        dfs(image, row + 1, col, originalColor, newColor); // Down
        dfs(image, row - 1, col, originalColor, newColor); // Up
        dfs(image, row, col + 1, originalColor, newColor); // Right
        dfs(image, row, col - 1, originalColor, newColor); // Left
    }
}

// Solution 2: BFS (Breadth-First Search) - Iterative using Queue
class Solution2 {
    public int[][] floodFill(int[][] image, int sr, int sc, int color) {
        int originalColor = image[sr][sc];

        if (originalColor == color) {
            return image;
        }

        int m = image.length;
        int n = image[0].length;

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { sr, sc });
        image[sr][sc] = color;

        // Direction vectors for 4 adjacent cells (up, down, left, right)
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0];
            int col = curr[1];

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                // Check boundaries and if pixel has original color
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                        && image[newRow][newCol] == originalColor) {
                    image[newRow][newCol] = color;
                    queue.offer(new int[] { newRow, newCol });
                }
            }
        }

        return image;
    }
}

// Solution 3: DFS with explicit stack (Iterative)
class Solution3 {
    public int[][] floodFill(int[][] image, int sr, int sc, int color) {
        int originalColor = image[sr][sc];

        if (originalColor == color) {
            return image;
        }

        int m = image.length;
        int n = image[0].length;

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[] { sr, sc });

        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            int row = curr[0];
            int col = curr[1];

            // Check if already processed or out of bounds
            if (row < 0 || row >= m || col < 0 || col >= n
                    || image[row][col] != originalColor) {
                continue;
            }

            image[row][col] = color;

            // Add all 4 adjacent cells to stack
            for (int[] dir : directions) {
                stack.push(new int[] { row + dir[0], col + dir[1] });
            }
        }

        return image;
    }
}

// Test class
class FloodFillTest {
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int[][] image1 = { { 1, 1, 1 }, { 1, 1, 0 }, { 1, 0, 1 } };
        int[][] result1 = solution.floodFill(image1, 1, 1, 2);
        System.out.println("Test 1:");
        printImage(result1);
        // Expected: [[2,2,2],[2,2,0],[2,0,1]]

        // Test case 2
        int[][] image2 = { { 0, 0, 0 }, { 0, 0, 0 } };
        int[][] result2 = solution.floodFill(image2, 0, 0, 0);
        System.out.println("\nTest 2:");
        printImage(result2);
        // Expected: [[0,0,0],[0,0,0]]
    }

    private static void printImage(int[][] image) {
        for (int[] row : image) {
            System.out.print("[");
            for (int i = 0; i < row.length; i++) {
                System.out.print(row[i]);
                if (i < row.length - 1)
                    System.out.print(",");
            }
            System.out.println("]");
        }
    }
}

/*
 * Complexity Analysis:
 * 
 * All three solutions:
 * - Time Complexity: O(m × n) where m is number of rows and n is number of
 * columns
 * In worst case, we might need to visit all pixels
 * 
 * - Space Complexity:
 * Solution 1 (Recursive DFS): O(m × n) for recursion stack in worst case
 * Solution 2 (BFS): O(m × n) for the queue in worst case
 * Solution 3 (Iterative DFS): O(m × n) for the stack in worst case
 * 
 * Key Points:
 * 1. Always check if starting pixel already has target color (avoid infinite
 * loop)
 * 2. DFS is more memory efficient in practice for typical images
 * 3. BFS processes pixels level by level
 * 4. All solutions modify the image in-place
 */

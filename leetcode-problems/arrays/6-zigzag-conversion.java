import java.util.*;
/*
 * The string "PAYPALISHIRING" is written in a zigzag pattern on a given number
 * of rows like this: (you may want to display this pattern in a fixed font for
 * better legibility)
 * 
 * P A H N
 * A P L S I I G
 * Y I R
 * And then read line by line: "PAHNAPLSIIGYIR"
 * 
 * Write the code that will take a string and make this conversion given a
 * number of rows:
 * 
 * string convert(string s, int numRows);
 * 
 * 
 * Example 1:
 * 
 * Input: s = "PAYPALISHIRING", numRows = 3
 * Output: "PAHNAPLSIIGYIR"
 * Example 2:
 * 
 * Input: s = "PAYPALISHIRING", numRows = 4
 * Output: "PINALSIGYAHRPI"
 * Explanation:
 * P I N
 * A L S I G
 * Y A H R
 * P I
 * Example 3:
 * 
 * Input: s = "A", numRows = 1
 * Output: "A"
 */

class ZigZagConversion {

    /**
     * Solution 1: Row-by-Row Simulation (Most Intuitive)
     * Time Complexity: O(n) where n is the length of the string
     * Space Complexity: O(n) for storing characters in rows
     * 
     * This approach simulates the zigzag pattern by using a list for each row
     * and changing direction when we reach the top or bottom.
     */
    public static String convert1(String s, int numRows) {
        if (numRows == 1 || s.length() <= numRows) {
            return s;
        }

        // Create a list for each row
        List<StringBuilder> rows = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            rows.add(new StringBuilder());
        }

        int currentRow = 0;
        boolean goingDown = false;

        // Fill each row by simulating the zigzag pattern
        for (char c : s.toCharArray()) {
            rows.get(currentRow).append(c);

            // Change direction when we reach top or bottom
            if (currentRow == 0 || currentRow == numRows - 1) {
                goingDown = !goingDown;
            }

            // Move to next row
            currentRow += goingDown ? 1 : -1;
        }

        // Concatenate all rows
        StringBuilder result = new StringBuilder();
        for (StringBuilder row : rows) {
            result.append(row);
        }

        return result.toString();
    }

    /**
     * Solution 2: Mathematical Pattern (Most Efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(1) excluding output string
     * 
     * This approach finds the mathematical pattern for character positions
     * in each row without simulating the zigzag movement.
     */
    public static String convert2(String s, int numRows) {
        if (numRows == 1 || s.length() <= numRows) {
            return s;
        }

        StringBuilder result = new StringBuilder();
        int n = s.length();
        int cycleLen = 2 * numRows - 2; // Length of one complete zigzag cycle

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j + i < n; j += cycleLen) {
                // Add character from current position
                result.append(s.charAt(j + i));

                // Add character from diagonal (middle rows only)
                if (i != 0 && i != numRows - 1 && j + cycleLen - i < n) {
                    result.append(s.charAt(j + cycleLen - i));
                }
            }
        }

        return result.toString();
    }

    /**
     * Solution 3: Index Mapping (Alternative Mathematical Approach)
     * Time Complexity: O(n)
     * Space Complexity: O(n) for result array
     * 
     * This approach maps each character's original index to its zigzag position.
     */
    public static String convert3(String s, int numRows) {
        if (numRows == 1 || s.length() <= numRows) {
            return s;
        }

        char[] result = new char[s.length()];
        int index = 0;
        int cycleLen = 2 * numRows - 2;

        // Process each row
        for (int row = 0; row < numRows; row++) {
            // Find all characters that belong to this row
            for (int i = row; i < s.length(); i += cycleLen) {
                result[index++] = s.charAt(i);

                // Add diagonal character for middle rows
                if (row != 0 && row != numRows - 1) {
                    int diagonalIndex = i + cycleLen - 2 * row;
                    if (diagonalIndex < s.length()) {
                        result[index++] = s.charAt(diagonalIndex);
                    }
                }
            }
        }

        return new String(result);
    }

    /**
     * Solution 4: 2D Array Visualization (For Understanding)
     * Time Complexity: O(n * numRows) in worst case
     * Space Complexity: O(n * numRows) for the 2D array
     * 
     * This approach actually creates a 2D representation of the zigzag pattern.
     * Less efficient but great for visualization and understanding.
     */
    public static String convert4(String s, int numRows) {
        if (numRows == 1 || s.length() <= numRows) {
            return s;
        }

        // Calculate required columns
        int cycles = (s.length() + 2 * numRows - 3) / (2 * numRows - 2);
        int numCols = cycles * (numRows - 1);

        // Create 2D array
        char[][] matrix = new char[numRows][numCols];

        int row = 0, col = 0;
        boolean goingDown = true;

        // Fill the matrix with zigzag pattern
        for (int i = 0; i < s.length(); i++) {
            matrix[row][col] = s.charAt(i);

            if (goingDown) {
                if (row == numRows - 1) {
                    // Reached bottom, start going diagonally up
                    goingDown = false;
                    row--;
                    col++;
                } else {
                    row++;
                }
            } else {
                if (row == 0) {
                    // Reached top, start going down
                    goingDown = true;
                    row++;
                } else {
                    row--;
                    col++;
                }
            }
        }

        // Read row by row
        StringBuilder result = new StringBuilder();
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                if (matrix[r][c] != 0) {
                    result.append(matrix[r][c]);
                }
            }
        }

        return result.toString();
    }

    /**
     * Helper method to visualize the zigzag pattern
     */
    public static void visualizeZigZag(String s, int numRows) {
        if (numRows == 1) {
            System.out.println(s);
            return;
        }

        int cycles = (s.length() + 2 * numRows - 3) / (2 * numRows - 2);
        int numCols = Math.max(cycles * (numRows - 1), s.length());

        char[][] matrix = new char[numRows][numCols];

        // Fill with spaces
        for (int i = 0; i < numRows; i++) {
            Arrays.fill(matrix[i], ' ');
        }

        int row = 0, col = 0;
        boolean goingDown = true;

        for (int i = 0; i < s.length(); i++) {
            matrix[row][col] = s.charAt(i);

            if (goingDown) {
                if (row == numRows - 1) {
                    goingDown = false;
                    row--;
                    col++;
                } else {
                    row++;
                }
            } else {
                if (row == 0) {
                    goingDown = true;
                    row++;
                } else {
                    row--;
                    col++;
                }
            }
        }

        // Print the matrix
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < Math.min(numCols, s.length() + numRows); c++) {
                System.out.print(matrix[r][c]);
            }
            System.out.println();
        }
        System.out.println();
    }

    // Test method
    public static void main(String[] args) {
        // Test cases
        String[] testStrings = { "PAYPALISHIRING", "PAYPALISHIRING", "A", "AB", "ABCDEFGHIJKLMNOP" };
        int[] testRows = { 3, 4, 1, 2, 5 };
        String[] expected = { "PAHNAPLSIIGYIR", "PINALSIGYAHRPI", "A", "AB", "AIBHJPRCEKMOFNLGQD" };

        for (int i = 0; i < testStrings.length; i++) {
            String s = testStrings[i];
            int numRows = testRows[i];
            String exp = expected[i];

            System.out.println("Input: s = \"" + s + "\", numRows = " + numRows);
            System.out.println("Expected: \"" + exp + "\"");

            // Visualize the pattern
            System.out.println("ZigZag Pattern:");
            visualizeZigZag(s, numRows);

            // Test all solutions
            String result1 = convert1(s, numRows);
            String result2 = convert2(s, numRows);
            String result3 = convert3(s, numRows);
            String result4 = convert4(s, numRows);

            System.out.println("Solution 1 (Simulation): \"" + result1 + "\" " +
                    (result1.equals(exp) ? "✓" : "✗"));
            System.out.println("Solution 2 (Mathematical): \"" + result2 + "\" " +
                    (result2.equals(exp) ? "✓" : "✗"));
            System.out.println("Solution 3 (Index Mapping): \"" + result3 + "\" " +
                    (result3.equals(exp) ? "✓" : "✗"));
            System.out.println("Solution 4 (2D Array): \"" + result4 + "\" " +
                    (result4.equals(exp) ? "✓" : "✗"));

            System.out.println("---");
        }

        // Performance comparison for large input
        System.out.println("Performance test with large string:");
        StringBuilder largeString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeString.append((char) ('A' + (i % 26)));
        }

        String large = largeString.toString();
        long start, end;

        start = System.nanoTime();
        convert1(large, 10);
        end = System.nanoTime();
        System.out.println("Solution 1: " + (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        convert2(large, 10);
        end = System.nanoTime();
        System.out.println("Solution 2: " + (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        convert3(large, 10);
        end = System.nanoTime();
        System.out.println("Solution 3: " + (end - start) / 1000000.0 + " ms");
    }

}

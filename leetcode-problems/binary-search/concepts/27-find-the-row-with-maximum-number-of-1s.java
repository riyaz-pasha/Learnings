/*
 * Problem Statement: You have been given a non-empty grid ‘mat’ with 'n' rows
 * and 'm' columns consisting of only 0s and 1s. All the rows are sorted in
 * ascending order.
 * Your task is to find the index of the row with the maximum number of ones.
 * Note: If two rows have the same number of ones, consider the one with a
 * smaller index. If there's no row with at least 1 zero, return -1.
 * 
 * Pre-requisite: Lower Bound implementation, Upper Bound implementation, & Find
 * the first occurrence of a number.
 * 
 * Examples
 * 
 * Example 1:
 * Input Format: n = 3, m = 3,
 * mat[] =
 * 1 1 1
 * 0 0 1
 * 0 0 0
 * Result: 0
 * Explanation: The row with the maximum number of ones is 0 (0 - indexed).
 * 
 * Example 2:
 * Input Format: n = 2, m = 2 ,
 * mat[] =
 * 0 0
 * 0 0
 * Result: -1
 * Explanation: The matrix does not contain any 1. So, -1 is the answer.
 */

class FindTheRowWithMaximumNumberOf1s {

    public int rowWithMax1s(int[][] matrix) {
        int rows = matrix.length, cols = matrix[0].length;
        int count = 0;
        int index = -1;

        for (int row = 0; row < rows; row++) {
            int countOf1s = cols - this.lowerBound(matrix[row], 1);
            if (countOf1s > count) {
                count = countOf1s;
                index = row;
            }
        }

        return index;
    }

    private int lowerBound(int[] row, int num) {
        int low = 0, high = row.length - 1;
        int answer = row.length;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (row[mid] == 1) {
                answer = mid;
                // look for smaller index on the left
                high = mid - 1;
            } else {
                // look on the right
                low = mid + 1;
            }
        }
        return answer;
    }

}

class RowWithMaxOnes {

    /**
     * BRUTE FORCE APPROACH - Count 1s in each row
     * Algorithm:
     * 1. Iterate through each row
     * 2. Count the number of 1s in each row
     * 3. Keep track of the row with maximum 1s
     * 4. If tie, prefer smaller index
     * 
     * Time Complexity: O(n * m) - check every element
     * Space Complexity: O(1) - only using variables
     */
    public static int rowWithMaxOnesBruteForce(int[][] mat) {
        int n = mat.length;
        int m = mat[0].length;

        int maxOnesCount = -1;
        int maxRowIndex = -1;

        for (int i = 0; i < n; i++) {
            int onesCount = 0;

            // Count 1s in current row
            for (int j = 0; j < m; j++) {
                onesCount += mat[i][j];
            }

            // Update if this row has more 1s
            // Note: we want smaller index in case of tie, so use > instead of >=
            if (onesCount > maxOnesCount) {
                maxOnesCount = onesCount;
                maxRowIndex = i;
            }
        }

        // Return -1 if no row has at least 1 one (all rows are all 0s)
        return maxOnesCount == 0 ? -1 : maxRowIndex;
    }

    /**
     * BETTER APPROACH - Binary Search on each row
     * Algorithm:
     * 1. For each row, use binary search to find the first occurrence of 1
     * 2. Calculate number of 1s = m - firstOneIndex
     * 3. Keep track of row with maximum 1s
     * 4. If tie, prefer smaller index
     * 
     * Time Complexity: O(n * log m) - binary search on each row
     * Space Complexity: O(1) - only using variables
     */
    public static int rowWithMaxOnesBetter(int[][] mat) {
        int n = mat.length;
        int m = mat[0].length;

        int maxOnesCount = -1;
        int maxRowIndex = -1;

        for (int i = 0; i < n; i++) {
            // Find first occurrence of 1 using binary search
            int firstOneIndex = findFirstOccurrence(mat[i], 1);

            int onesCount;
            if (firstOneIndex == -1) {
                // No 1 found in this row
                onesCount = 0;
            } else {
                // Number of 1s = total columns - first 1 index
                onesCount = m - firstOneIndex;
            }

            // Update if this row has more 1s
            if (onesCount > maxOnesCount) {
                maxOnesCount = onesCount;
                maxRowIndex = i;
            }
        }

        return maxOnesCount == 0 ? -1 : maxRowIndex;
    }

    /**
     * Helper method to find first occurrence of target in sorted array
     */
    private static int findFirstOccurrence(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (arr[mid] == target) {
                result = mid;
                right = mid - 1; // Continue searching in left half for first occurrence
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    /**
     * OPTIMAL APPROACH - Two Pointers (Staircase Search)
     * Algorithm:
     * 1. Start from top-right corner (row 0, column m-1)
     * 2. If current element is 1, move left (more 1s possible in this row)
     * 3. If current element is 0, move down (no 1s in this row from current
     * position)
     * 4. Keep track of the row with maximum 1s encountered
     * 
     * Key Insight: Since rows are sorted, if we find a 1 at position j,
     * all elements from j to m-1 in that row are 1s.
     * 
     * Time Complexity: O(n + m) - at most n+m moves
     * Space Complexity: O(1) - only using variables
     */
    public static int rowWithMaxOnesOptimal(int[][] mat) {
        int n = mat.length;
        int m = mat[0].length;

        int maxOnesCount = 0;
        int maxRowIndex = -1;

        // Start from top-right corner
        int row = 0, col = m - 1;

        while (row < n && col >= 0) {
            if (mat[row][col] == 1) {
                // Found 1, calculate ones in this row
                int onesCount = m - col;

                // Update if this row has more 1s (or same count with smaller index)
                if (onesCount > maxOnesCount) {
                    maxOnesCount = onesCount;
                    maxRowIndex = row;
                }

                // Move left to check for more 1s in current row
                col--;
            } else {
                // Found 0, move to next row
                row++;
            }
        }

        return maxOnesCount == 0 ? -1 : maxRowIndex;
    }

    /**
     * ALTERNATIVE OPTIMAL APPROACH - Leftmost 1 tracking
     * Algorithm:
     * 1. Keep track of the leftmost position of 1 found so far
     * 2. For each row, use binary search to find first 1
     * 3. If first 1 is at a position left of current leftmost, update
     * 4. This ensures we get the row with maximum 1s (leftmost first 1)
     * 
     * Time Complexity: O(n * log m)
     * Space Complexity: O(1)
     */
    public static int rowWithMaxOnesAlternative(int[][] mat) {
        int n = mat.length;
        int m = mat[0].length;

        int leftmostOneCol = m; // Initialize to beyond last column
        int maxRowIndex = -1;

        for (int i = 0; i < n; i++) {
            // Use binary search to find first occurrence of 1
            int firstOneIndex = findFirstOccurrence(mat[i], 1);

            // If 1 is found and it's to the left of current leftmost
            if (firstOneIndex != -1 && firstOneIndex < leftmostOneCol) {
                leftmostOneCol = firstOneIndex;
                maxRowIndex = i;
            }
        }

        return maxRowIndex;
    }

    // Helper method to demonstrate the staircase search logic
    public static void demonstrateStaircaseSearch(int[][] mat) {
        System.out.println("=== Staircase Search Demonstration ===");
        printMatrix(mat);

        int n = mat.length;
        int m = mat[0].length;
        int row = 0, col = m - 1;

        System.out.println("Starting from top-right corner: mat[" + row + "][" + col + "]");
        System.out.println("Path taken:");

        int maxOnesCount = 0;
        int maxRowIndex = -1;
        int step = 1;

        while (row < n && col >= 0) {
            System.out.printf("Step %d: mat[%d][%d] = %d", step++, row, col, mat[row][col]);

            if (mat[row][col] == 1) {
                int onesCount = m - col;
                System.out.printf(" -> Found 1! This row has %d ones", onesCount);

                if (onesCount > maxOnesCount) {
                    maxOnesCount = onesCount;
                    maxRowIndex = row;
                    System.out.printf(" (New maximum!)");
                }

                System.out.println(" -> Move LEFT");
                col--;
            } else {
                System.out.println(" -> Found 0, move DOWN");
                row++;
            }
        }

        System.out.println("Final result: Row " + maxRowIndex + " with " + maxOnesCount + " ones");
        System.out.println();
    }

    // Helper method to print matrix
    private static void printMatrix(int[][] mat) {
        System.out.println("Matrix:");
        for (int i = 0; i < mat.length; i++) {
            System.out.print("Row " + i + ": ");
            for (int j = 0; j < mat[0].length; j++) {
                System.out.print(mat[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("=== Row with Maximum 1s ===\n");

        // Test case 1: Normal case
        int[][] mat1 = {
                { 0, 0, 1, 1, 1 },
                { 0, 0, 0, 1, 1 },
                { 0, 1, 1, 1, 1 },
                { 0, 0, 0, 0, 1 }
        };

        System.out.println("Test Case 1:");
        printMatrix(mat1);
        System.out.println("Expected: Row 2 (has 4 ones)");

        int result1_brute = rowWithMaxOnesBruteForce(mat1);
        int result1_better = rowWithMaxOnesBetter(mat1);
        int result1_optimal = rowWithMaxOnesOptimal(mat1);
        int result1_alt = rowWithMaxOnesAlternative(mat1);

        System.out.println("Brute Force: Row " + result1_brute);
        System.out.println("Better (Binary Search): Row " + result1_better);
        System.out.println("Optimal (Staircase): Row " + result1_optimal);
        System.out.println("Alternative: Row " + result1_alt);
        System.out.println();

        demonstrateStaircaseSearch(mat1);

        // Test case 2: Tie case (prefer smaller index)
        int[][] mat2 = {
                { 0, 0, 0, 1, 1 },
                { 0, 0, 1, 1, 1 },
                { 0, 0, 1, 1, 1 },
                { 0, 0, 0, 0, 1 }
        };

        System.out.println("Test Case 2 (Tie - prefer smaller index):");
        printMatrix(mat2);
        System.out.println("Expected: Row 1 (rows 1 and 2 both have 3 ones, but row 1 has smaller index)");

        int result2_optimal = rowWithMaxOnesOptimal(mat2);
        System.out.println("Optimal result: Row " + result2_optimal);
        System.out.println();

        // Test case 3: All zeros
        int[][] mat3 = {
                { 0, 0, 0, 0 },
                { 0, 0, 0, 0 },
                { 0, 0, 0, 0 }
        };

        System.out.println("Test Case 3 (All zeros):");
        printMatrix(mat3);
        System.out.println("Expected: -1 (no row has any 1s)");

        int result3_optimal = rowWithMaxOnesOptimal(mat3);
        System.out.println("Optimal result: " + result3_optimal);
        System.out.println();

        // Test case 4: All ones
        int[][] mat4 = {
                { 1, 1, 1, 1 },
                { 1, 1, 1, 1 },
                { 1, 1, 1, 1 }
        };

        System.out.println("Test Case 4 (All ones):");
        printMatrix(mat4);
        System.out.println("Expected: Row 0 (all rows have same count, prefer smaller index)");

        int result4_optimal = rowWithMaxOnesOptimal(mat4);
        System.out.println("Optimal result: Row " + result4_optimal);
        System.out.println();

        // Performance comparison
        System.out.println("=== Time Complexity Analysis ===");
        System.out.println("Brute Force: O(n * m) - check every element");
        System.out.println("Better (Binary Search): O(n * log m) - binary search on each row");
        System.out.println("Optimal (Staircase): O(n + m) - at most n+m moves");
        System.out.println("Alternative: O(n * log m) - binary search approach");
        System.out.println();

        System.out.println("=== Key Insights ===");
        System.out.println("1. Sorted rows property enables binary search and staircase search");
        System.out.println("2. Staircase search is optimal - starts from top-right corner");
        System.out.println("3. When we find 1 at position j, all positions j to m-1 have 1s");
        System.out.println("4. When we find 0, no need to check left in that row");
        System.out.println("5. Two-pointer technique eliminates need to check all elements");
        System.out.println("6. In case of tie, we naturally get smaller index due to top-to-bottom traversal");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM UNDERSTANDING:
 * - Given: n x m matrix with only 0s and 1s, each row sorted in ascending order
 * - Find: index of row with maximum number of 1s
 * - Constraints: If tie, return smaller index; if no 1s exist, return -1
 * 
 * BRUTE FORCE APPROACH:
 * - Count 1s in each row by iterating through all elements
 * - Keep track of maximum count and corresponding row index
 * - Time: O(n*m), Space: O(1)
 * 
 * BETTER APPROACH (Binary Search):
 * - For each row, use binary search to find first occurrence of 1
 * - Number of 1s = m - firstOneIndex
 * - Compare counts across all rows
 * - Time: O(n*log m), Space: O(1)
 * 
 * OPTIMAL APPROACH (Staircase Search):
 * - Start from top-right corner (row 0, col m-1)
 * - Key insight: utilize the sorted property of rows
 * - If current element is 1: move left (more 1s possible in current row)
 * - If current element is 0: move down (no more 1s in current row)
 * - Time: O(n+m), Space: O(1)
 * 
 * WHY STAIRCASE SEARCH WORKS:
 * 1. Since rows are sorted, if mat[i][j] = 0, then mat[i][0...j-1] are all 0s
 * 2. If mat[i][j] = 1, then mat[i][j...m-1] are all 1s
 * 3. Starting from top-right ensures we explore maximum possible 1s
 * 4. Each move eliminates either a row or a column from consideration
 * 
 * MOVEMENT LOGIC:
 * - Found 1: This row might have maximum 1s, but there might be more 1s to the
 * left
 * - Found 0: This row cannot have more 1s than what we've seen, move to next
 * row
 * 
 * ALTERNATIVE APPROACH:
 * - Track the leftmost position where 1 is found
 * - Row with leftmost 1 has maximum count
 * - Use binary search to find first 1 in each row
 * - Time: O(n*log m), Space: O(1)
 * 
 * EDGE CASES:
 * 1. All elements are 0: return -1
 * 2. All elements are 1: return 0 (smallest index)
 * 3. Multiple rows with same max count: return smallest index
 * 4. Single row: return 0 if it has 1s, else -1
 * 
 * The staircase search is the most elegant and efficient solution!
 */

class MaxOnesRow {

    /*
     * Time: O(n + m)
     * (we traverse at most m left moves and n down moves)
     * 
     * Space: O(1)
     */
    public static int rowWithMaxOnes(int[][] mat) {
        int n = mat.length;
        int m = mat[0].length;

        int maxRowIndex = -1;
        int j = m - 1; // start at the top-right corner

        for (int i = 0; i < n; i++) {
            // Move left while there are 1s
            while (j >= 0 && mat[i][j] == 1) {
                maxRowIndex = i;
                j--; // go left
            }
        }

        return maxRowIndex;
    }

    public static void main(String[] args) {
        int[][] mat1 = {
                { 0, 0, 0, 1 },
                { 0, 1, 1, 1 },
                { 0, 0, 1, 1 }
        };
        System.out.println(rowWithMaxOnes(mat1)); // Output: 1

        int[][] mat2 = {
                { 0, 0 },
                { 0, 0 }
        };
        System.out.println(rowWithMaxOnes(mat2)); // Output: -1
    }
}

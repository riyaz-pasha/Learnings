/**
 * ============================================================================
 * WORD SEARCH - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * Imagine playing a game of Boggle, but with slightly different rules. You are 
 * given a grid of letters and a target word. You must find if the word is hidden 
 * in the grid by linking adjacent letters (up, down, left, right). The catch? 
 * You cannot step on the same letter tile twice while spelling the word. 
 * If you can spell the target word, return true. Otherwise, return false.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Can the word wrap around the edges of the board?
 * A: No, we can only move to valid adjacent cells within the grid boundaries.
 * 
 * Q: Are diagonal moves allowed?
 * A: No, the problem specifies only horizontal or vertical neighbors.
 * 
 * Q: Is the search case-sensitive?
 * A: Yes, 'A' is considered different from 'a'. (Constraints say both lower and 
 *    uppercase English letters are present).
 * 
 * Q: Can we modify the board?
 * A: Clarifying this is crucial. Modifying the board in-place saves O(M*N) 
 *    auxiliary space that a 'visited' matrix would consume. Most interviewers 
 *    allow in-place modification as long as you restore it (backtrack).
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - GRAPH TRAVERSAL: This is a pathfinding problem on a grid, making it a 
 *   Graph problem. We need to explore all possible paths, so Depth-First Search 
 *   (DFS) with Backtracking is the natural choice.
 * - BACKTRACKING: As we walk down a path, we mark the current cell as "visited".
 *   If the path turns out to be a dead-end (the next letters don't match), we 
 *   step back, UNMARK the cell, and try a different direction.
 * - IN-PLACE VISITED STATE: Instead of creating a boolean[][] visited matrix, 
 *   we can temporarily change the character on the board to a non-alphabet 
 *   character (like '*') and change it back when backtracking.
 * - PRUNING (ADVANCED): What if the board doesn't even have enough 'A's to 
 *   form the word? Or what if the word starts with 'E' (which appears 50 times 
 *   on the board) but ends with 'Z' (which appears once)? We can count characters 
 *   to fail fast, and even reverse the word to search from the less common end!
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Start with the standard DFS + Backtracking approach. It's the 
 *   expected answer and demonstrates your core understanding of recursion.
 * - Step 2: Discuss space complexity. Highlight that modifying the board 
 *   in-place saves O(M*N) space, reducing space complexity to just O(L) 
 *   for the recursion stack, where L is the length of the word.
 * - Step 3: Once the base solution is written, ask the interviewer if they 
 *   want to see optimizations. Mention frequency counting and word-reversal 
 *   techniques. This will guarantee you stand out.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Board:
 * [ 'A', 'B', 'C', 'E' ]
 * [ 'S', 'F', 'C', 'S' ]
 * [ 'A', 'D', 'E', 'E' ]
 * 
 * Word: "ABCCED"
 * 
 * Traversal:
 * Start at (0,0) 'A' -> matches. Mark as '*'.
 * Neighbors of 'A': 'B' (0,1), 'S' (1,0).
 * Move to (0,1) 'B' -> matches. Mark as '*'.
 * Neighbors of 'B': 'C' (0,2), 'F' (1,1). (Left is '*', invalid).
 * ... Continues until "ABCCED" is fully traced.
 * Result: TRUE
 */

import java.util.*;

public class WordSearch {

    /**
     * SOLUTION 1: Standard Recursive DFS with Backtracking (In-place)
     * ------------------------------------------------------------------------
     * Pros: Clean, easy to read, and exactly what interviewers expect as a baseline.
     * Cons: Can be slow on large grids with many repeating characters (though 
     * our constraints are very small: 6x6 max).
     * 
     * Time Complexity: O(M * N * 3^L) where M*N is the grid size, and L is the 
     * word length. We say 3^L because after the first step (4 directions), we 
     * only have 3 valid directions to explore (can't go back to the previous cell).
     * Space Complexity: O(L) for the recursion call stack.
     */
    public boolean existStandard(char[][] board, String word) {
        int m = board.length;
        int n = board[0].length;
        char[] w = word.toCharArray();

        // Iterate through every cell on the board to find a starting point
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                // If the first character matches, launch the DFS
                if (board[r][c] == w[0] && dfs(board, r, c, w, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfs(char[][] board, int r, int c, char[] word, int index) {
        // Base Case 1: We've matched all characters in the word
        if (index == word.length) {
            return true;
        }

        // Base Case 2: Out of bounds or character mismatch
        if (r < 0 || r >= board.length || c < 0 || c >= board[0].length || board[r][c] != word[index]) {
            return false;
        }

        // Store the original character so we can backtrack later
        char temp = board[r][c];
        
        // Mark the current cell as visited by replacing it with an invalid character
        board[r][c] = '*';

        // Explore all 4 adjacent directions (Up, Down, Left, Right)
        boolean found = dfs(board, r + 1, c, word, index + 1) ||
                        dfs(board, r - 1, c, word, index + 1) ||
                        dfs(board, r, c + 1, word, index + 1) ||
                        dfs(board, r, c - 1, word, index + 1);

        // Backtrack: Restore the original character for other paths to use
        board[r][c] = temp;

        return found;
    }

    /**
     * SOLUTION 2: Optimized DFS with Pruning (For passing extreme edge cases)
     * ------------------------------------------------------------------------
     * Pros: Drastically reduces execution time by failing fast. It checks if 
     * the board even has the necessary letters before starting the heavy DFS.
     * Furthermore, it dynamically reverses the word if the suffix is rarer 
     * than the prefix on the board, minimizing the branching factor.
     * 
     * Time Complexity: O(M * N * 3^L) worst case, but practically much faster.
     * Space Complexity: O(L) for the recursion stack + O(1) for frequency array.
     */
    public boolean existOptimized(char[][] board, String word) {
        int m = board.length;
        int n = board[0].length;
        
        // Step 1: Frequency counting to fail early
        // We use a size 128 array to cover all standard ASCII characters
        int[] boardFreq = new int[128];
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                boardFreq[board[r][c]]++;
            }
        }

        char[] w = word.toCharArray();
        for (char ch : w) {
            boardFreq[ch]--;
            // If a required character is missing from the board, impossible!
            if (boardFreq[ch] < 0) {
                return false;
            }
        }

        // Step 2: Advanced Pruning - Start from the less frequent end.
        // If the first letter appears more times on the board than the last letter,
        // reversing the word means we launch fewer DFS branches initially.
        if (boardFreq[w[0]] > boardFreq[w[w.length - 1]]) {
            reverse(w);
        }

        // Step 3: Execute standard DFS with the pruned/optimized state
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                if (board[r][c] == w[0] && dfs(board, r, c, w, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Helper method to reverse a char array
    private void reverse(char[] arr) {
        int left = 0;
        int right = arr.length - 1;
        while (left < right) {
            char temp = arr[left];
            arr[left++] = arr[right];
            arr[right--] = temp;
        }
    }

    /**
     * UTILITY: Print the board clearly.
     */
    private static void printBoard(char[][] board) {
        for (char[] row : board) {
            System.out.println(Arrays.toString(row));
        }
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        WordSearch solver = new WordSearch();

        char[][] board = {
            {'A', 'B', 'C', 'E'},
            {'S', 'F', 'C', 'S'},
            {'A', 'D', 'E', 'E'}
        };

        String word1 = "ABCCED"; // Exists
        String word2 = "SEE";    // Exists
        String word3 = "ABCB";   // Doesn't exist

        System.out.println("Board:");
        printBoard(board);
        System.out.println();

        System.out.println("--- Testing Solution 1: Standard DFS ---");
        System.out.println("Word '" + word1 + "' exists? " + solver.existStandard(board, word1));
        System.out.println("Word '" + word2 + "' exists? " + solver.existStandard(board, word2));
        System.out.println("Word '" + word3 + "' exists? " + solver.existStandard(board, word3));

        System.out.println("\n--- Testing Solution 2: Optimized DFS ---");
        System.out.println("Word '" + word1 + "' exists? " + solver.existOptimized(board, word1));
        System.out.println("Word '" + word2 + "' exists? " + solver.existOptimized(board, word2));
        System.out.println("Word '" + word3 + "' exists? " + solver.existOptimized(board, word3));
    }
}

/**
 * ============================================================
 * 🔥 WORD SEARCH — VISITED ARRAY VERSION
 * ============================================================
 *
 * Difference from in-place marking:
 * - Instead of modifying board → use boolean visited[][]
 *
 * Pros:
 * ✔️ Cleaner logic (no mutation)
 * ✔️ Safer in interviews (no accidental bugs)
 *
 * Cons:
 * ❌ Extra space O(M * N)
 *
 * Time Complexity  : O(M * N * 4^L)
 * Space Complexity : O(M * N) + O(L recursion stack)
 */
public class WordSearchVisited {

    public static void main(String[] args) {
        char[][] board = {
                {'A','B','C','E'},
                {'S','F','C','S'},
                {'A','D','E','E'}
        };

        String word = "ABCCED";

        System.out.println(exist(board, word)); // true
    }

    public static boolean exist(char[][] board, String word) {
        int m = board.length;
        int n = board[0].length;

        // visited array to track used cells
        boolean[][] visited = new boolean[m][n];

        // Try every cell as starting point
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {

                if (dfs(board, word, i, j, 0, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * DFS + Backtracking
     *
     * index → which character we are matching in word
     */
    private static boolean dfs(char[][] board, String word,
                               int row, int col, int index,
                               boolean[][] visited) {

        // ✅ BASE CASE: entire word matched
        if (index == word.length()) {
            return true;
        }

        // ❌ BOUNDARY + VISITED + MISMATCH CHECK
        if (row < 0 || col < 0 ||
            row >= board.length || col >= board[0].length ||
            visited[row][col] || // already used
            board[row][col] != word.charAt(index)) {
            return false;
        }

        // 🧠 CHOOSE
        visited[row][col] = true;

        // 🔁 EXPLORE (4 directions)
        boolean found =
                dfs(board, word, row + 1, col, index + 1, visited) || // down
                dfs(board, word, row - 1, col, index + 1, visited) || // up
                dfs(board, word, row, col + 1, index + 1, visited) || // right
                dfs(board, word, row, col - 1, index + 1, visited);   // left

        // 🔙 BACKTRACK (important!)
        visited[row][col] = false;

        return found;
    }
}

/*
 * Given an m x n grid of characters board and a string word, return true if
 * word exists in the grid.
 * 
 * The word can be constructed from letters of sequentially adjacent cells,
 * where adjacent cells are horizontally or vertically neighboring. The same
 * letter cell may not be used more than once.
 * 
 * Example 1:
 * Input: board = [["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word
 * = "ABCCED"
 * Output: true
 * 
 * Example 2:
 * Input: board = [["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word
 * = "SEE"
 * Output: true
 * 
 * Example 3:
 * Input: board = [["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word
 * = "ABCB"
 * Output: false
 */

// Solution 1: Basic Backtracking with Visited Array
class Solution1 {

    public boolean exist(char[][] board, String word) {
        if (board == null || board.length == 0 || word == null || word.length() == 0) {
            return false;
        }

        int m = board.length;
        int n = board[0].length;
        boolean[][] visited = new boolean[m][n];

        // Try starting from each cell
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (dfs(board, word, i, j, 0, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfs(char[][] board, String word, int i, int j, int index, boolean[][] visited) {
        // Base case: found the word
        if (index == word.length()) {
            return true;
        }

        // Check bounds and conditions
        if (i < 0 || i >= board.length || j < 0 || j >= board[0].length ||
                visited[i][j] || board[i][j] != word.charAt(index)) {
            return false;
        }

        // Mark as visited
        visited[i][j] = true;

        // Explore all 4 directions
        boolean found = dfs(board, word, i + 1, j, index + 1, visited) ||
                dfs(board, word, i - 1, j, index + 1, visited) ||
                dfs(board, word, i, j + 1, index + 1, visited) ||
                dfs(board, word, i, j - 1, index + 1, visited);

        // Backtrack
        visited[i][j] = false;

        return found;
    }

}

// Solution 2: Optimized - Modify Board Instead of Visited Array
class Solution2 {

    public boolean exist(char[][] board, String word) {
        if (board == null || board.length == 0 || word == null || word.length() == 0) {
            return false;
        }

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (dfs(board, word, i, j, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfs(char[][] board, String word, int i, int j, int index) {
        if (index == word.length()) {
            return true;
        }

        if (i < 0 || i >= board.length || j < 0 || j >= board[0].length ||
                board[i][j] != word.charAt(index)) {
            return false;
        }

        // Mark as visited by modifying the board
        char temp = board[i][j];
        board[i][j] = '#'; // Use a character that won't match any letter

        boolean found = dfs(board, word, i + 1, j, index + 1) ||
                dfs(board, word, i - 1, j, index + 1) ||
                dfs(board, word, i, j + 1, index + 1) ||
                dfs(board, word, i, j - 1, index + 1);

        // Restore the original character
        board[i][j] = temp;

        return found;
    }

}

// Solution 3: With Direction Array for Cleaner Code
class Solution3 {

    private int[][] directions = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };

    public boolean exist(char[][] board, String word) {
        if (board == null || board.length == 0 || word == null || word.length() == 0) {
            return false;
        }

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (dfs(board, word, i, j, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfs(char[][] board, String word, int i, int j, int index) {
        if (index == word.length()) {
            return true;
        }

        if (i < 0 || i >= board.length || j < 0 || j >= board[0].length ||
                board[i][j] != word.charAt(index)) {
            return false;
        }

        char temp = board[i][j];
        board[i][j] = '#';

        for (int[] dir : directions) {
            if (dfs(board, word, i + dir[0], j + dir[1], index + 1)) {
                board[i][j] = temp;
                return true;
            }
        }

        board[i][j] = temp;
        return false;
    }

}

// Solution 4: Early Termination Optimizations
class Solution4 {

    public boolean exist(char[][] board, String word) {
        if (board == null || board.length == 0 || word == null || word.length() == 0) {
            return false;
        }

        // Early termination: check if word is longer than total cells
        if (word.length() > board.length * board[0].length) {
            return false;
        }

        // Count character frequencies in board and word
        int[] boardCount = new int[128];
        int[] wordCount = new int[128];

        for (char[] row : board) {
            for (char c : row) {
                boardCount[c]++;
            }
        }

        for (char c : word.toCharArray()) {
            wordCount[c]++;
        }

        // Check if board has enough characters
        for (int i = 0; i < 128; i++) {
            if (wordCount[i] > boardCount[i]) {
                return false;
            }
        }

        // Optimization: start from the character that appears less frequently
        char firstChar = word.charAt(0);
        char lastChar = word.charAt(word.length() - 1);
        if (boardCount[lastChar] < boardCount[firstChar]) {
            word = new StringBuilder(word).reverse().toString();
        }

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (dfs(board, word, i, j, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfs(char[][] board, String word, int i, int j, int index) {
        if (index == word.length()) {
            return true;
        }

        if (i < 0 || i >= board.length || j < 0 || j >= board[0].length ||
                board[i][j] != word.charAt(index)) {
            return false;
        }

        char temp = board[i][j];
        board[i][j] = '#';

        boolean found = dfs(board, word, i + 1, j, index + 1) ||
                dfs(board, word, i - 1, j, index + 1) ||
                dfs(board, word, i, j + 1, index + 1) ||
                dfs(board, word, i, j - 1, index + 1);

        board[i][j] = temp;
        return found;
    }

}

// Solution 5: Trie-based for Multiple Word Search (Bonus)
class Solution5 {

    class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isWord = false;
    }

    // This version is optimized for searching multiple words
    public boolean exist(char[][] board, String word) {
        TrieNode root = buildTrie(new String[] { word });

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (dfs(board, i, j, root)) {
                    return true;
                }
            }
        }
        return false;
    }

    private TrieNode buildTrie(String[] words) {
        TrieNode root = new TrieNode();
        for (String word : words) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                int index = c - 'a';
                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }
                node = node.children[index];
            }
            node.isWord = true;
        }
        return root;
    }

    private boolean dfs(char[][] board, int i, int j, TrieNode node) {
        if (node.isWord) {
            return true;
        }

        if (i < 0 || i >= board.length || j < 0 || j >= board[0].length ||
                board[i][j] == '#') {
            return false;
        }

        char c = board[i][j];
        int index = c - 'a';
        if (index < 0 || index >= 26 || node.children[index] == null) {
            return false;
        }

        board[i][j] = '#';
        boolean found = dfs(board, i + 1, j, node.children[index]) ||
                dfs(board, i - 1, j, node.children[index]) ||
                dfs(board, i, j + 1, node.children[index]) ||
                dfs(board, i, j - 1, node.children[index]);
        board[i][j] = c;

        return found;
    }

}

// Test class to demonstrate all solutions
class WordSearchTest {
    public static void main(String[] args) {
        Solution1 sol1 = new Solution1();
        Solution2 sol2 = new Solution2();
        Solution3 sol3 = new Solution3();
        Solution4 sol4 = new Solution4();
        Solution5 sol5 = new Solution5();

        // Test case 1
        char[][] board1 = {
                { 'A', 'B', 'C', 'E' },
                { 'S', 'F', 'C', 'S' },
                { 'A', 'D', 'E', 'E' }
        };

        String[] words = { "ABCCED", "SEE", "ABCB" };
        boolean[] expected = { true, true, false };

        System.out.println("Test Results:");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            System.out.println("\nWord: " + word + " (Expected: " + expected[i] + ")");
            System.out.println("Solution 1: " + sol1.exist(copyBoard(board1), word));
            System.out.println("Solution 2: " + sol2.exist(copyBoard(board1), word));
            System.out.println("Solution 3: " + sol3.exist(copyBoard(board1), word));
            System.out.println("Solution 4: " + sol4.exist(copyBoard(board1), word));
            System.out.println("Solution 5: " + sol5.exist(copyBoard(board1), word));
        }
    }

    private static char[][] copyBoard(char[][] original) {
        char[][] copy = new char[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

}

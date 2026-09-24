import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/*
 * Given an m x n board of characters and a list of strings words, return all
 * words on the board.
 * 
 * Each word must be constructed from letters of sequentially adjacent cells,
 * where adjacent cells are horizontally or vertically neighboring. The same
 * letter cell may not be used more than once in a word.
 * 
 * Example 1:
 * Input: board =
 * [["o","a","a","n"],["e","t","a","e"],["i","h","k","r"],["i","f","l","v"]],
 * words = ["oath","pea","eat","rain"]
 * Output: ["eat","oath"]
 * 
 * Example 2:
 * Input: board = [["a","b"],["c","d"]], words = ["abcb"]
 * Output: []
 */

/**
 * Word Search II - Find all words from a dictionary that can be formed on a 2D
 * board
 * Uses Trie + DFS backtracking for efficient multi-word search
 * 
 * Time Complexity: O(M * N * 4^L) where M*N is board size, L is max word length
 * Space Complexity: O(K * L) where K is number of words, L is average word
 * length
 */
class WordSearchII {

    /**
     * TrieNode for building the dictionary trie
     */
    private static class TrieNode {
        TrieNode[] children;
        String word; // Store the complete word at end nodes for easy retrieval

        public TrieNode() {
            children = new TrieNode[26];
            word = null;
        }
    }

    private TrieNode root;
    private List<String> result;
    private char[][] board;
    private int m, n;

    // Directions for exploring adjacent cells (up, down, left, right)
    private static final int[][] DIRECTIONS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    /**
     * Main method to find all words on the board
     */
    public List<String> findWords(char[][] board, String[] words) {
        if (board == null || board.length == 0 || board[0].length == 0 ||
                words == null || words.length == 0) {
            return new ArrayList<>();
        }

        this.board = board;
        this.m = board.length;
        this.n = board[0].length;
        this.result = new ArrayList<>();

        // Build Trie from words
        buildTrie(words);

        // Search for words starting from each cell
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                dfs(i, j, root);
            }
        }

        return result;
    }

    /**
     * Build Trie from the given words array
     */
    private void buildTrie(String[] words) {
        root = new TrieNode();

        for (String word : words) {
            TrieNode current = root;

            for (char ch : word.toCharArray()) {
                int index = ch - 'a';
                if (current.children[index] == null) {
                    current.children[index] = new TrieNode();
                }
                current = current.children[index];
            }

            current.word = word; // Store complete word at the end
        }
    }

    /**
     * DFS to search for words starting from position (i, j)
     */
    private void dfs(int i, int j, TrieNode node) {
        // Check bounds
        if (i < 0 || i >= m || j < 0 || j >= n) {
            return;
        }

        char ch = board[i][j];

        // Check if cell is already visited or character doesn't exist in trie
        if (ch == '#' || node.children[ch - 'a'] == null) {
            return;
        }

        // Move to next node in trie
        node = node.children[ch - 'a'];

        // Check if we found a complete word
        if (node.word != null) {
            result.add(node.word);
            node.word = null; // Avoid duplicate results
        }

        // Mark current cell as visited
        board[i][j] = '#';

        // Explore all 4 directions
        for (int[] dir : DIRECTIONS) {
            int newRow = i + dir[0];
            int newCol = j + dir[1];
            dfs(newRow, newCol, node);
        }

        // Backtrack: restore the original character
        board[i][j] = ch;
    }

    /**
     * Enhanced version with optimizations
     */
    public List<String> findWordsOptimized(char[][] board, String[] words) {
        if (board == null || board.length == 0 || board[0].length == 0 ||
                words == null || words.length == 0) {
            return new ArrayList<>();
        }

        this.board = board;
        this.m = board.length;
        this.n = board[0].length;
        this.result = new ArrayList<>();

        // Build Trie from words
        buildTrieOptimized(words);

        // Search for words starting from each cell
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (root.children[board[i][j] - 'a'] != null) {
                    dfsOptimized(i, j, root);
                }
            }
        }

        return result;
    }

    /**
     * Optimized trie building with pruning
     */
    private void buildTrieOptimized(String[] words) {
        root = new TrieNode();

        for (String word : words) {
            if (word.length() == 0)
                continue;

            TrieNode current = root;

            for (char ch : word.toCharArray()) {
                int index = ch - 'a';
                if (current.children[index] == null) {
                    current.children[index] = new TrieNode();
                }
                current = current.children[index];
            }

            current.word = word;
        }
    }

    /**
     * Optimized DFS with trie pruning
     */
    private void dfsOptimized(int i, int j, TrieNode node) {
        if (i < 0 || i >= m || j < 0 || j >= n) {
            return;
        }

        char ch = board[i][j];

        if (ch == '#' || node.children[ch - 'a'] == null) {
            return;
        }

        node = node.children[ch - 'a'];

        // Found a word
        if (node.word != null) {
            result.add(node.word);
            node.word = null; // Remove to avoid duplicates
        }

        // Early termination: if this node has no children, no need to continue
        boolean hasChildren = false;
        for (TrieNode child : node.children) {
            if (child != null) {
                hasChildren = true;
                break;
            }
        }

        if (!hasChildren) {
            return;
        }

        // Mark as visited
        board[i][j] = '#';

        // Explore neighbors
        for (int[] dir : DIRECTIONS) {
            dfsOptimized(i + dir[0], j + dir[1], node);
        }

        // Backtrack
        board[i][j] = ch;
    }

    /**
     * Helper method to print the board
     */
    public static void printBoard(char[][] board) {
        for (char[] row : board) {
            System.out.println(Arrays.toString(row));
        }
        System.out.println();
    }

    /**
     * Helper method to validate if a word can be formed on the board (for testing)
     */
    public boolean canFormWord(char[][] board, String word) {
        if (board == null || board.length == 0 || word == null || word.isEmpty()) {
            return false;
        }

        int m = board.length;
        int n = board[0].length;
        boolean[][] visited = new boolean[m][n];

        // Try starting from each cell
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == word.charAt(0)) {
                    if (canFormWordDFS(board, word, 0, i, j, visited)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * DFS helper for single word validation
     */
    private boolean canFormWordDFS(char[][] board, String word, int index,
            int i, int j, boolean[][] visited) {
        if (index == word.length()) {
            return true;
        }

        if (i < 0 || i >= board.length || j < 0 || j >= board[0].length ||
                visited[i][j] || board[i][j] != word.charAt(index)) {
            return false;
        }

        visited[i][j] = true;

        boolean found = false;
        for (int[] dir : DIRECTIONS) {
            if (canFormWordDFS(board, word, index + 1, i + dir[0], j + dir[1], visited)) {
                found = true;
                break;
            }
        }

        visited[i][j] = false; // Backtrack
        return found;
    }

    /**
     * Main method with comprehensive testing
     */
    public static void main(String[] args) {
        WordSearchII solution = new WordSearchII();

        System.out.println("=== Word Search II Demo ===\n");

        // Test Case 1: Example from problem
        System.out.println("Test Case 1:");
        char[][] board1 = {
                { 'o', 'a', 'a', 'n' },
                { 'e', 't', 'a', 'e' },
                { 'i', 'h', 'k', 'r' },
                { 'i', 'f', 'l', 'v' }
        };
        String[] words1 = { "oath", "pea", "eat", "rain" };

        System.out.println("Board:");
        printBoard(board1);
        System.out.println("Words to find: " + Arrays.toString(words1));

        List<String> result1 = solution.findWords(board1, words1);
        System.out.println("Found words: " + result1);
        System.out.println("Expected: [eat, oath]\n");

        // Test Case 2: No valid words
        System.out.println("Test Case 2:");
        char[][] board2 = {
                { 'a', 'b' },
                { 'c', 'd' }
        };
        String[] words2 = { "abcb" };

        System.out.println("Board:");
        printBoard(board2);
        System.out.println("Words to find: " + Arrays.toString(words2));

        List<String> result2 = solution.findWords(board2, words2);
        System.out.println("Found words: " + result2);
        System.out.println("Expected: []\n");

        // Test Case 3: Single character words
        System.out.println("Test Case 3:");
        char[][] board3 = {
                { 'a', 'b' },
                { 'c', 'd' }
        };
        String[] words3 = { "a", "b", "c", "d", "ab", "cd", "ac" };

        System.out.println("Board:");
        printBoard(board3);
        System.out.println("Words to find: " + Arrays.toString(words3));

        List<String> result3 = solution.findWords(board3, words3);
        System.out.println("Found words: " + result3);

        // Test Case 4: Complex board with many paths
        System.out.println("Test Case 4:");
        char[][] board4 = {
                { 'o', 'a', 'b', 'n' },
                { 'o', 't', 'a', 'e' },
                { 'a', 'h', 'k', 'r' },
                { 'a', 'f', 'l', 'v' }
        };
        String[] words4 = { "oa", "oat", "oath", "oatha", "oathk" };

        System.out.println("Board:");
        printBoard(board4);
        System.out.println("Words to find: " + Arrays.toString(words4));

        List<String> result4 = solution.findWords(board4, words4);
        System.out.println("Found words: " + result4);

        // Test Case 5: Test optimized version
        System.out.println("Test Case 5 - Optimized Version:");
        List<String> result5 = solution.findWordsOptimized(board1, words1);
        System.out.println("Found words (optimized): " + result5);

        // Validation test
        System.out.println("\n=== Validation Tests ===");
        System.out.println("Can form 'oath': " + solution.canFormWord(board1, "oath"));
        System.out.println("Can form 'eat': " + solution.canFormWord(board1, "eat"));
        System.out.println("Can form 'pea': " + solution.canFormWord(board1, "pea"));
        System.out.println("Can form 'rain': " + solution.canFormWord(board1, "rain"));
        System.out.println("Can form 'abcb': " + solution.canFormWord(board2, "abcb"));

        // Performance test with larger board
        System.out.println("\n=== Performance Test ===");
        char[][] largeBoard = {
                { 'a', 'b', 'c', 'e', 'f', 'g' },
                { 'h', 'i', 'j', 'k', 'l', 'm' },
                { 'n', 'o', 'p', 'q', 'r', 's' },
                { 't', 'u', 'v', 'w', 'x', 'y' },
                { 'z', 'a', 'b', 'c', 'd', 'e' }
        };
        String[] manyWords = { "abc", "hij", "nop", "tuv", "zab", "aei", "bfj",
                "abce", "hijk", "nopq", "tuvw", "zabc", "path", "word" };

        long startTime = System.currentTimeMillis();
        List<String> perfResult = solution.findWordsOptimized(largeBoard, manyWords);
        long endTime = System.currentTimeMillis();

        System.out.println("Large board test completed in " + (endTime - startTime) + "ms");
        System.out.println("Found " + perfResult.size() + " words: " + perfResult);
    }

}

class Solution {

    private static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        String word = null; // store word at the end node
    }

    private void buildTrie(String[] words, TrieNode root) {
        for (String word : words) {
            TrieNode node = root;
            for (char ch : word.toCharArray()) {
                int index = ch - 'a';
                if (node.children[index] == null)
                    node.children[index] = new TrieNode();
                node = node.children[index];
            }
            node.word = word; // mark the complete word
        }
    }

    public List<String> findWords(char[][] board, String[] words) {
        List<String> result = new ArrayList<>();
        TrieNode root = new TrieNode();
        buildTrie(words, root);

        int m = board.length;
        int n = board[0].length;

        for (int row = 0; row < m; row++) {
            for (int col = 0; col < n; col++) {
                dfs(board, row, col, root, result);
            }
        }

        return result;
    }

    private void dfs(char[][] board, int row, int col, TrieNode node, List<String> result) {
        char ch = board[row][col];

        if (ch == '#' || node.children[ch - 'a'] == null)
            return;

        node = node.children[ch - 'a'];

        if (node.word != null) {
            result.add(node.word);
            node.word = null; // avoid duplicates
        }

        board[row][col] = '#'; // mark visited

        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] dir : dirs) {
            int newRow = row + dir[0], newCol = col + dir[1];
            if (newRow >= 0 && newRow < board.length && newCol >= 0 && newCol < board[0].length) {
                dfs(board, newRow, newCol, node, result);
            }
        }

        board[row][col] = ch; // backtrack
    }

}

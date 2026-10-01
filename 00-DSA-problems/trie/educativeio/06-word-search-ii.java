/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: WORD SEARCH II
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Can the same cell be used more than once to construct a single word?"
 *   (Confirms standard Boggle rules: a cell can only be visited once per word path. We need backtracking.)
 * - "If the same word is found through multiple different paths in the grid, should it appear multiple times in the output?"
 *   (Defines if the output should act as a Set. Usually, we just want a unique list of found words.)
 * - "Are we allowed to modify the input grid in-place to mark cells as visited?"
 *   (If yes, we save O(R * C) auxiliary space that a boolean[][] visited matrix would require.)
 * - "Are all characters strictly uppercase A-Z?"
 *   (Crucial for sizing the Trie node array correctly and mapping characters using `ch - 'A'` instead of `ch - 'a'`.)
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * We need to find which strings from a massive list exist as continuous snake-like paths 
 * in a grid. The binding constraint is doing this without repeating path traversals. 
 * Searching the grid once per word is disastrous if we have 3,000 words.
 * 
 * APPROACH 1: DFS for Each Word (The Brute Force)
 * 1. What I'd naturally try: Iterate over the list of words. For each word, scan the 
 *    grid to find its first letter. Once found, launch a Depth-First Search (DFS) 
 *    to find the rest of the letters.
 * 2. Why it works: It exhaustively checks the board for every single word.
 * 3. Why it's too slow: If our dictionary has 500 words starting with 'A', and the 
 *    board has an 'A', we will launch 500 separate DFS traversals from that exact same 
 *    cell, re-exploring the exact same neighbor paths.
 * 4. What work is repeated: Grid traversal. We explore the same physical grid paths 
 *    multiple times for different words that share prefixes.
 * 5. Time Complexity: O(W * R * C * 3^L) 
 *    — W words, looping over R*C cells to find starts, then DFS explores 3 directions 
 *    (excluding where it came from) up to length L.
 * 6. Space Complexity: O(L) 
 *    — Only the recursion stack depth (max length of a word).
 * 
 * APPROACH 2: DFS from Each Cell + HashSet (The Intermediate Improvement)
 * 1. What I'd naturally try next: Let's flip the search direction. Put all 3,000 words 
 *    into a `HashSet<String>`. Then, start a DFS from *every* cell in the grid, 
 *    building a string as we go, and check if it's in the HashSet.
 * 2. Why it works: We only initiate grid traversals R*C times, completely independent 
 *    of the number of words.
 * 3. Why it's still too slow: We have no way to know when to *stop* searching. If our 
 *    current path spells "XYZ", but no word in our set starts with "XYZ", we still blindly 
 *    continue DFS up to the maximum word length, building useless strings.
 * 4. What property removes the bottleneck: The realization that we need prefix-awareness. 
 *    We must be able to ask: "Does ANY word in the dictionary start with the letters 
 *    I've collected so far?" If no, prune the DFS immediately.
 * 5. Time Complexity: O(R * C * 3^L * L) 
 *    — R*C starting points, 3^L paths, and creating/hashing a string of length L at each step.
 * 6. Space Complexity: O(W * L) 
 *    — Storing W words of length L in the HashSet, plus recursion stack.
 * 
 * APPROACH 3: Trie + Backtracking DFS (The Optimal Solution)
 * 1. The defining observation: "Simultaneous Search." If we put all words into a Trie, 
 *    we can let the grid search the dictionary all at once. As we step through the grid, 
 *    we simultaneously step down the Trie. If a Trie node doesn't exist for the grid 
 *    character, we instantly abort that path.
 * 2. Why it works: It prunes the search space aggressively. If the board has "XYZ", 
 *    the Trie instantly tells us there is no child 'X', stopping the 3^L explosion at step 1.
 * 3. Time Complexity: 
 *    - Time Complexity: O(W * L + R * C * 3^L) — where W is words, L is max word length, 
 *      R/C are grid dimensions. We build the Trie in O(W*L). We do DFS from each cell, 
 *      exploring at most 3 directions per step. In practice, Trie pruning makes the 
 *      DFS drastically faster than the worst-case 3^L.
 * 4. Space Complexity: 
 *    - Space Complexity: O(W * L) — because we store the dictionary in a Prefix Tree. 
 *      The DFS recursion stack takes O(L) space, and we modify the board in-place 
 *      (O(1) aux space) to track visited cells.
 * 
 * [What I'd write in an interview]
 * I would exclusively write Approach 3. Word Search II is *the* classic Trie + DFS problem. 
 * Combining grid backtracking with Trie traversal demonstrates command over two major 
 * recursive patterns at once. I would also add a deduplication trick: once a word is 
 * found, set `node.word = null` in the Trie so we never add it to the results list twice.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - Words with shared prefixes: "CAR" and "CARD". We must be able to find "CAR", add it 
 *   to results, and *continue* the DFS to find "CARD".
 * - Duplicates on the board: The grid contains "OATH" twice. Setting `node.word = null` 
 *   after the first find prevents adding "OATH" twice.
 * - Grid 1x1: A single cell. Code must not crash looking for neighbors.
 * - Empty dictionary or empty grid bounds.
 * 
 * 
 * 4. KEY INSIGHT, DRY RUN & PITFALLS
 * ----------------------------------------------------------------------------
 * [Key Insight]
 * Don't pass a `StringBuilder` or `String` prefix into the recursive DFS. It wastes memory. 
 * Instead, store the complete string directly in the Trie's `isEndOfWord` node 
 * (e.g., `node.word = "OATH"`). When you hit that node during DFS, just grab the string!
 * 
 * [Dry Run]
 * Grid: [['O','A'],      Words: ["OAT", "OATH"]
 *        ['T','H']]
 * 
 * 1. Build Trie: root -> 'O' -> 'A' -> 'T'(word="OAT") -> 'H'(word="OATH").
 * 2. DFS starts at Grid[0][0] ('O'). Trie root has 'O'.
 * 3. Mark Grid[0][0] = '#'. 
 * 4. DFS goes Right to Grid[0][1] ('A'). Trie 'O' has child 'A'.
 * 5. Mark Grid[0][1] = '#'. 
 * 6. DFS goes Down to Grid[1][1] ('H'). Trie 'A' does NOT have child 'H' (only 'T'). Path aborts!
 * 7. DFS goes Left to Grid[1][0] ('T'). Trie 'A' has child 'T'.
 *    - `node.word` is "OAT". Add "OAT" to results! 
 *    - Set `node.word = null` to prevent duplicate finding.
 * 8. DFS from 'T' goes Right to Grid[1][1] ('H'). Trie 'T' has child 'H'.
 *    - `node.word` is "OATH". Add "OATH" to results! 
 *    - Set `node.word = null`.
 * 9. Backtrack: restore '#' to original letters as recursion unwinds.
 * 
 * [Pitfalls]
 * - Forgetting to backtrack the visited state. You must restore `board[r][c] = letter` 
 *   after the recursive calls so other DFS paths can use that cell.
 * - Using `ch - 'a'` instead of `ch - 'A'` on uppercase inputs, resulting in out-of-bounds 
 *   array exceptions when accessing `children[]`.
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: How can we further optimize the Trie to speed up the search as words are found?
 * A: Pruning leaf nodes. When we find a word, we can physically delete that leaf node 
 *    from the Trie. If its parent now has no other children, we can delete the parent too. 
 *    This actively shrinks the search space as the algorithm progresses.
 * 
 * Q: What if the grid is massive (10,000 x 10,000) but the dictionary is only 10 words?
 * A: The Trie approach might be overkill if W << R*C. It might be faster to fall back to 
 *    Approach 1 (DFS for each word) or use string matching algorithms (like Aho-Corasick) 
 *    adapted for 2D grids, scanning the grid linearly rather than doing full DFS everywhere.
 */

import java.util.ArrayList;
import java.util.List;

public class WordSearchIIStudyNote {

    // ============================================================================
    // TRIE IMPLEMENTATION
    // ============================================================================
    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        // Instead of boolean isEndOfWord, we store the actual word here.
        // This acts as both the boolean flag AND prevents us from needing 
        // to build strings during the DFS.
        String word = null; 
    }

    // Directional arrays for exploring Up, Down, Left, Right
    private static final int[] ROW_OFFSETS = {-1, 1, 0, 0};
    private static final int[] COL_OFFSETS = {0, 0, -1, 1};

    public List<String> findWords(char[][] board, String[] words) {
        List<String> result = new ArrayList<>();
        if (board == null || board.length == 0 || words == null || words.length == 0) {
            return result;
        }

        // 1. Build the Trie
        TrieNode root = new TrieNode();
        for (String word : words) {
            insert(root, word);
        }

        int rows = board.length;
        int cols = board[0].length;

        // 2. Launch DFS from every single cell
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // If the very first character isn't in the Trie root, don't even start.
                if (root.children[board[r][c] - 'A'] != null) {
                    dfs(board, r, c, root, result);
                }
            }
        }

        return result;
    }

    private void insert(TrieNode root, String word) {
        TrieNode curr = root;
        for (int i = 0; i < word.length(); i++) {
            int index = word.charAt(i) - 'A'; // Note: UPPERCASE letters
            if (curr.children[index] == null) {
                curr.children[index] = new TrieNode();
            }
            curr = curr.children[index];
        }
        curr.word = word; // Store the exact string at the terminal node
    }

    // ============================================================================
    // BACKTRACKING DFS
    // ============================================================================
    private void dfs(char[][] board, int r, int c, TrieNode node, List<String> result) {
        char letter = board[r][c];
        
        // Move to the next node in the Trie
        TrieNode currNode = node.children[letter - 'A'];
        if (currNode == null) {
            return; // Prune: Path doesn't match any prefix in the dictionary
        }

        // Check if we hit a complete word
        if (currNode.word != null) {
            result.add(currNode.word);
            currNode.word = null; // De-duplicate: clear the word so we don't find it again
        }

        // Mark the current cell as visited by altering it to a non-alphabet character
        board[r][c] = '#';

        // Explore all 4 adjacent directions
        for (int i = 0; i < 4; i++) {
            int newRow = r + ROW_OFFSETS[i];
            int newCol = c + COL_OFFSETS[i];

            // Check bounds and ensure the cell isn't already visited ('#')
            if (newRow >= 0 && newRow < board.length && 
                newCol >= 0 && newCol < board[0].length && 
                board[newRow][newCol] != '#') {
                
                dfs(board, newRow, newCol, currNode, result);
            }
        }

        // Backtrack: Restore the cell's original letter so other paths can use it
        board[r][c] = letter;
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        WordSearchIIStudyNote solution = new WordSearchIIStudyNote();

        System.out.println("--- Standard Case ---");
        char[][] board1 = {
            {'O','A','A','N'},
            {'E','T','A','E'},
            {'I','H','K','R'},
            {'I','F','L','V'}
        };
        String[] words1 = {"OATH","PEA","EAT","RAIN"};
        // Expected: [OATH, EAT]
        System.out.println("Found: " + solution.findWords(board1, words1));

        System.out.println("\n--- Edge Case: Overlapping Prefixes ---");
        char[][] board2 = {
            {'C','A','R','D'}
        };
        String[] words2 = {"CAR", "CARD"};
        // Expected: [CAR, CARD] (Must continue DFS even after finding CAR)
        System.out.println("Found: " + solution.findWords(board2, words2));

        System.out.println("\n--- Edge Case: Snake Path & Duplicates ---");
        char[][] board3 = {
            {'A','A'},
            {'A','A'}
        };
        String[] words3 = {"AAAA", "AAAA"};
        // Expected: [AAAA] (Must not return duplicate entries)
        System.out.println("Found: " + solution.findWords(board3, words3));
        
        System.out.println("\n--- Edge Case: No Match ---");
        char[][] board4 = {
            {'X','Y'}
        };
        String[] words4 = {"ZOO"};
        // Expected: []
        System.out.println("Found: " + solution.findWords(board4, words4));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Trie + Matrix Backtracking DFS.
 * Key observation: Instead of iterating words and searching the board, iterate 
 *                  the board and search the Trie simultaneously. It instantly 
 *                  prunes invalid paths.
 * To memorize:     1) Store the string in `node.word` instead of a boolean. 
 *                  2) Mark visited cells in-place with `#`. 
 *                  3) Prevent duplicates by setting `node.word = null` after finding.
 * Common trap:     Using `char - 'a'` on an uppercase board, causing array out 
 *                  of bounds exceptions. Look closely at character constraints!
 * Mental trigger:  "Find multiple words in a grid" -> Trie + DFS.
 * ============================================================================
 */


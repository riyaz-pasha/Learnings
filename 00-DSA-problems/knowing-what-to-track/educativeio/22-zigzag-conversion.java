/**
 * STANDALONE STUDY NOTE: ZIGZAG CONVERSION
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "What should be returned if `numRows == 1`?" (If numRows is 1, the zigzag pattern is just a straight line, 
 *   so we should immediately return the original string unchanged to prevent an infinite loop).
 * - "What if `numRows` is greater than or equal to the length of the string `s`?" (Each character gets its own row, 
 *   so the result is also just the original string).
 * - "Are spaces allowed in the string?" (Constraints state characters include English letters, commas, and periods, 
 *   but the logic handles any character transparently).
 * - "Should we return a new string?" (Yes, returning the concatenated row-by-row string).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to rearrange characters in a zigzag pattern across `numRows` and read them row by row. 
 * The binding constraint on naive approaches is allocating an actual 2D grid matrix in memory, which is sparse, 
 * wasteful, and unnecessary.
 * 
 * --- APPROACH 1: BRUTE FORCE (2D Matrix Simulation) ---
 * 1. What I'd naturally try: Allocate a large 2D character array of size `numRows` by `Cols` (where Cols is calculated 
 *    based on the length of the string), simulate a pointer moving down and diagonally up, place characters into the 
 *    grid cells, and then read out non-empty cells row by row.
 * 2. Why it works: It literally mimics the visual writing of the zigzag pattern.
 * 3. Why it's too slow/costly: It wastes significant memory on empty grid cells (sparsity) and requires two passes 
 *    (one to write, one to read).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(R * C) — where R is `numRows` and C is columns, proportional to string length.
 *    - Space Complexity: O(R * C) — for allocating the sparse 2D character matrix.
 * 
 * --- APPROACH 2: OPTIMAL (Row-by-Row Simulation via StringBuilders) ---
 * 1. The Observation: We don't need a full 2D grid. We only care which *row* each character lands in. 
 *    As we iterate through the string character by character, the row index moves *down* until it hits row 0 or 
 *    row `numRows - 1`, at which point the direction flips (down to up, or up to down).
 * 2. How it leads to the next step: Maintain an array of `numRows` `StringBuilder` objects (`rows[numRows]`). 
 *    Iterate through `s`, append each character to `rows[currentRow]`, and update `currentRow` based on our direction flag. 
 *    Finally, concatenate all `StringBuilder` rows together.
 * 3. Which one I'd write in an interview: Approach 2 (Row-by-Row Simulation). It runs in true O(N) time, uses minimal 
 *    O(N) auxiliary space for the output, and completely avoids sparse matrix allocation.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — where N is the length of string `s`. We visit every character exactly once.
 *    - Space Complexity: O(N) — to store the characters across the StringBuilder rows and construct the final result string.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - `numRows == 1`: `s = "ABC", numRows = 1` -> Returns `"ABC"` immediately (early exit prevents step-direction lockups).
 * - `numRows >= s.length()`: `s = "ABC", numRows = 5` -> Each character gets its own row, returns `"ABC"`.
 * - Empty string: `s = "", numRows = 3` -> Returns `""`.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Row Traversal Direction Flipping:
 * 
 * String: "COMPUTER", numRows = 3
 * 
 * Row 0: C . . . P . . .
 * Row 1: . O . O T . R .
 * Row 2: . . U . . . M .
 * 
 * Direction tracker (`goingDown`):
 * - Start at row 0, `goingDown = false` (or switch when hitting bounds).
 * - Move down: row 0 -> row 1 -> row 2.
 * - Hit bottom (row `numRows - 1`), flip direction to go *up*: row 2 -> row 1 -> row 0.
 * - Hit top (row 0), flip direction to go *down*: row 0 -> row 1 -> row 2.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: s = "AB", numRows = 1
 * 
 * - Early check: numRows == 1 -> Returns "AB" immediately.
 * 
 * Target: s = "PAYPALISHIRING", numRows = 3
 * - rows = [StringBuilder(), StringBuilder(), StringBuilder()]
 * - currentRow = 0, goingDown = false
 * - i=0 ('P'): rows[0].append('P'), currentRow becomes 1, goingDown = true
 * - i=1 ('A'): rows[1].append('A'), currentRow becomes 2, goingDown = true
 * - i=2 ('Y'): rows[2].append('Y'), currentRow becomes 1, goingDown = false (hit bottom!)
 * - i=3 ('P'): rows[1].append('P'), currentRow becomes 0, goingDown = false (hit top!)
 * ...
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "Instead of allocating a wasteful sparse 2D matrix, we can simulate the zigzag pattern by tracking which row 
 * each character belongs to. We maintain an array of `numRows` StringBuilders. As we iterate through the string, 
 * we append each character to the current row's StringBuilder. We also maintain a boolean direction flag: 
 * when we reach the top row (0) or bottom row (`numRows - 1`), we invert our direction. Finally, we concatenate 
 * all rows together. We also include an early exit if `numRows == 1`. This gives us O(N) time complexity and O(N) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: Can this be solved mathematically without simulating rows at all?
 * A: Yes, by analyzing the cycle length (`2 * numRows - 2`), we can compute the exact indices of characters 
 *    belonging to each row mathematically, skipping row simulations entirely. However, the simulation approach 
 *    is cleaner and less prone to off-by-one index bugs under interview pressure.
 */

public class ZigzagConversion {

    /**
     * APPROACH 2: OPTIMAL (Row-by-Row Simulation via StringBuilders)
     * Time: O(N), Space: O(N)
     */
    public String convert(String s, int numRows) {
        // [Why needed]: Edge case protection. If numRows is 1, zigzag is a straight line. 
        // Returning s immediately prevents infinite direction-flipping bugs.
        if (numRows == 1 || numRows >= s.length()) {
            return s;
        }

        // [Why needed]: Array of StringBuilders to hold characters for each of the numRows rows.
        StringBuilder[] rows = new StringBuilder[numRows];
        for (int i = 0; i < numRows; i++) {
            rows[i] = new StringBuilder();
        }

        int currentRow = 0;
        boolean goingDown = false;

        // Phase 1: Distribute characters across rows based on zigzag direction
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            rows[currentRow].append(c);

            // [What it does]: When we hit the top row (0) or bottom row (numRows - 1), 
            // we invert our vertical travel direction.
            if (currentRow == 0 || currentRow == numRows - 1) {
                goingDown = !goingDown;
            }

            // Step down if goingDown is true, step up if goingDown is false
            currentRow += goingDown ? 1 : -1;
        }

        // Phase 2: Combine all rows into a single result string
        StringBuilder result = new StringBuilder();
        for (StringBuilder row : rows) {
            result.append(row);
        }

        return result.toString();
    }

    /**
     * SUMMARY
     * 1. Core pattern: Row simulation with directional bouncing (`goingDown` flag).
     * 2. Key observation: Sparse 2D grids are unnecessary; tracking row index and bouncing between 0 and `numRows - 1` 
     *    is sufficient.
     * 3. Memorize vs Understand: Understand why `numRows == 1` requires an immediate early exit.
     * 4. Most common trap: Forgetting to flip direction when hitting row 0 or `numRows - 1`, causing out-of-bounds errors.
     * 5. Mental trigger: "Zigzag conversion" -> `StringBuilder[] rows` + `goingDown` bounce flag.
     */
    public static void main(String[] args) {
        ZigzagConversion solution = new ZigzagConversion();

        // Test Case 1: Standard Example from prompt
        String s1 = "COMPUTER";
        int rows1 = 3;
        System.out.println("Test 1 (COMPUTER, rows=3): " + solution.convert(s1, rows1)); // Expected: "CUOPTRME"

        // Test Case 2: Classic LeetCode Example
        String s2 = "PAYPALISHIRING";
        int rows2 = 3;
        System.out.println("Test 2 (PAYPALISHIRING, rows=3): " + solution.convert(s2, rows2)); // Expected: "PAHNAPLSIIGYIR"

        // Test Case 3: Single Row Edge Case
        String s3 = "AB";
        int rows3 = 1;
        System.out.println("Test 3 (AB, rows=1): " + solution.convert(s3, rows3)); // Expected: "AB"
    }
}


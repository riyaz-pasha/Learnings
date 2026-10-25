/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design the basic function of Excel and implement the function of the sum formula.
 * 
 * Implement the Excel class:
 * - Excel(int height, char width): Initializes the object with the height and width.
 * - void set(int row, char column, int val): Sets the value at (row, column) to val. 
 *   This clears any existing sum formula for that cell.
 * - int get(int row, char column): Returns the value at (row, column).
 * - int sum(int row, char column, String[] numbers): Sets the value at (row, column) 
 *   to be the sum of cells represented by `numbers` and returns this sum. `numbers` 
 *   can contain individual cells (e.g., "A1") and ranges (e.g., "A1:B2").
 * 
 * Constraints:
 * - 1 <= height <= 26, 'A' <= width <= 'Z'
 * - At most 100 calls will be made to set, get, and sum.
 * - Guaranteed there are no circular dependencies.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Does the sum formula evaluate dynamically?
 *    (Yes, if A1 = B1 + C1, and later B1 is updated via `set`, querying A1 via `get` 
 *    must reflect the new value of B1).
 * 2. Can ranges overlap or contain the same cell multiple times?
 *    (Yes, `numbers = ["A1", "A1:B2"]` means A1 is counted twice. We must track frequencies).
 * 3. Are circular dependencies possible?
 *    (The constraints guarantee no circular references, which saves us from cycle detection logic).
 * 4. Is it a read-heavy or write-heavy system?
 *    (With only 100 calls, either eager or lazy evaluation will pass. We will optimize for simplicity and maintainability).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Handle dynamic data dependencies (formulas) between cells. 
 *   The binding constraint is that a cell's value is no longer static once a 
 *   formula is applied; it forms a Directed Acyclic Graph (DAG) with other cells.
 * 
 * --- Approach 1: Eager Evaluation (Push Model) ---
 * 1. Intuition: When a cell is updated via `set()`, push that update to all cells that depend on it.
 * 2. Try: Maintain a graph of back-edges (e.g., `B1` keeps a list of cells like `A1` that need it). 
 *    On `set(B1)`, trigger a Topological Sort / BFS to update `A1` and any cascading dependents.
 * 3. Why it's painful: If `A1`'s formula is overwritten by a direct `set(A1, 5)`, we have to traverse 
 *    its old dependencies and remove `A1` from their back-edge lists. Managing this reverse graph 
 *    is highly error-prone in a 45-minute interview.
 * 
 * --- Approach 2: Lazy Evaluation (Pull Model) [Optimal for this constraint set] ---
 * 1. Intuition: Don't calculate cascading updates on `set()`. Just store the raw value OR the formula. 
 *    Calculate the final value recursively on-the-fly only when `get()` or `sum()` is called.
 * 2. Try: A `Cell` object either has an integer `val` or a `HashMap` formula (tracking which cells 
 *    it depends on and their frequency). 
 *    - `set`: Drop the map, update `val`.
 *    - `get`: If the map exists, recursively call `get()` on dependent cells, sum them up, and return. 
 *      If not, return `val`.
 * 3. Why it works: It perfectly mirrors an abstract syntax tree evaluation. Overwriting a formula 
 *    is as simple as `formula = null`. No back-edges to maintain.
 * 4. Which to write in an interview?
 *    Approach 2. The grid is at most 26x26 (676 cells), and the total call count is tiny (100 calls). 
 *    Lazy DFS Evaluation is vastly cleaner and demonstrates a solid grasp of state encapsulation.
 * 
 * Time Complexity: 
 * - set: O(1)
 * - get/sum: O(V + E) in the worst-case DAG traversal, bounded by the grid size.
 * Space Complexity: O(H * W) to store the grid and formulas.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Overlapping ranges: `["A1:A2", "A1"]`. The formula map tracks frequencies: `A1` -> 2, `A2` -> 1.
 * - Multi-digit rows: Extracting the row number requires parsing everything after the first char (e.g., "A12" -> row 12).
 * - Dropping formulas: Calling `set` on a cell that previously had a `sum` formula must cleanly obliterate the formula.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "State Encapsulation." 
 *   By forcing every cell to self-evaluate recursively via `getValue(grid)`, the Excel 
 *   board itself acts as a dumb container. The graph traversal logic is beautifully 
 *   hidden inside the Cell object's own behavior.
 * 
 * ASCII DRY RUN (Lazy Evaluation)
 * C1 has formula ["A1", "B1:B2"]
 * 
 * Action: get(C1)
 * C1.getValue() 
 *  |-- requests A1.getValue() -> returns 5
 *  |-- requests B1.getValue() -> returns 2
 *  |-- requests B2.getValue() -> returns 3
 *  |-- returns 5 + 2 + 3 = 10
 * 
 * If A1 is set to 10:
 * A1 drops its formula (if any), sets val = 10.
 * Next time get(C1) is called, it recursively pulls the 10. No push needed!
 * 
 * Pitfalls:
 * - String parsing logic: Do not assume rows are single digits. `"A10"` is valid. 
 *   `Integer.parseInt(ref.substring(1))` safely extracts it.
 * - 0-based vs 1-based indexing: The prompt uses 1-based indexing for rows (`1` to `26`) 
 *   and characters for columns (`'A'` to `'Z'`). Standardize to a 0-based `r` and `c` internally.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if the board was massive (1,000,000 cells) and `get()` was called millions of times?
 * A1: The lazy Pull Model evaluates the same dependencies repeatedly. We would optimize this by 
 *     adding memoization. When `set()` is called, we invalidate the cache for that cell. But 
 *     wait—invalidating the cache requires knowing who depends on this cell, which brings us back 
 *     to the Push Model! For a massive, read-heavy board, we would construct a Dependency Graph 
 *     (Topological Sort) and eagerly evaluate on `set()`.
 * 
 * Q2: How would you detect circular dependencies? (e.g., A1 = B1, B1 = A1)
 * A2: During the recursive `getValue()` DFS, we can pass a `Set<String> visited` path. If we 
 *     encounter a cell that is already in the `visited` set for the current evaluation chain, 
 *     we throw a `CircularDependencyException`.
 */

import java.util.HashMap;
import java.util.Map;

public class ExcelStudy {

    static class Excel {
        
        /**
         * Represents a single cell in the spreadsheet.
         * A cell acts either as a raw value OR a formula.
         */
        private static class Cell {
            int val;
            // Tracks dependent cell keys (e.g., "A1") and how many times they are added.
            // If null, the cell holds a raw static value.
            Map<String, Integer> formula;

            public Cell() {
                this.val = 0;
                this.formula = null;
            }

            /**
             * Assigns a raw value, inherently destroying any previous formula.
             */
            public void setValue(int val) {
                this.formula = null;
                this.val = val;
            }

            /**
             * Generates the formula frequency map based on the input strings.
             */
            public void setFormula(String[] numbers) {
                this.formula = new HashMap<>();
                
                for (String s : numbers) {
                    if (s.contains(":")) {
                        // Handle a range like "A1:B2"
                        String[] range = s.split(":");
                        String start = range[0];
                        String end = range[1];
                        
                        int startRow = Integer.parseInt(start.substring(1));
                        int endRow = Integer.parseInt(end.substring(1));
                        char startCol = start.charAt(0);
                        char endCol = end.charAt(0);
                        
                        // Expand the range into individual cell frequencies
                        for (int r = startRow; r <= endRow; r++) {
                            for (char c = startCol; c <= endCol; c++) {
                                String cellRef = "" + c + r;
                                formula.put(cellRef, formula.getOrDefault(cellRef, 0) + 1);
                            }
                        }
                    } else {
                        // Handle an individual cell like "A1"
                        formula.put(s, formula.getOrDefault(s, 0) + 1);
                    }
                }
            }

            /**
             * Recursively evaluates the cell's value based on its formula.
             */
            public int evaluate(Cell[][] grid) {
                if (this.formula == null) {
                    return this.val;
                }
                
                int sum = 0;
                for (Map.Entry<String, Integer> entry : formula.entrySet()) {
                    String cellRef = entry.getKey();
                    int count = entry.getValue();
                    
                    // Parse back into 0-based grid coordinates
                    int r = Integer.parseInt(cellRef.substring(1)) - 1;
                    int c = cellRef.charAt(0) - 'A';
                    
                    // Recursively evaluate dependencies
                    sum += grid[r][c].evaluate(grid) * count;
                }
                return sum;
            }
        }

        private Cell[][] grid;

        /**
         * Initializes the Excel grid.
         */
        public Excel(int height, char width) {
            // Convert character width to a 0-based integer size
            int cols = width - 'A' + 1;
            grid = new Cell[height][cols];
            
            // Populate the grid with empty cells
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < cols; c++) {
                    grid[r][c] = new Cell();
                }
            }
        }

        /**
         * Sets a raw value at a specific cell.
         */
        public void set(int row, char column, int val) {
            grid[row - 1][column - 'A'].setValue(val);
        }

        /**
         * Retrieves the dynamically evaluated value at a specific cell.
         */
        public int get(int row, char column) {
            return grid[row - 1][column - 'A'].evaluate(grid);
        }

        /**
         * Applies a formula to a cell and returns its newly evaluated sum.
         */
        public int sum(int row, char column, String[] numbers) {
            Cell targetCell = grid[row - 1][column - 'A'];
            targetCell.setFormula(numbers);
            return targetCell.evaluate(grid);
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        // Initialize a 3x3 grid (3 rows, columns A-C)
        Excel excel = new Excel(3, 'C');

        // Set A1 = 1, A2 = 2
        excel.set(1, 'A', 1);
        excel.set(2, 'A', 2);
        
        System.out.println("Get A1: " + excel.get(1, 'A')); // Expected: 1
        System.out.println("Get A2: " + excel.get(2, 'A')); // Expected: 2

        // Set B1 = A1 + A2 -> B1 becomes 3
        String[] formula1 = {"A1", "A2"};
        System.out.println("Sum B1 (A1 + A2): " + excel.sum(1, 'B', formula1)); // Expected: 3

        // Set B2 = A1:B1 -> B2 = A1(1) + B1(3) = 4
        String[] formula2 = {"A1:B1"};
        System.out.println("Sum B2 (A1:B1): " + excel.sum(2, 'B', formula2)); // Expected: 4

        // Update A1 to 10
        System.out.println("Setting A1 to 10...");
        excel.set(1, 'A', 10);

        // B1 dynamically updates to A1(10) + A2(2) = 12
        System.out.println("Get B1 after A1 update: " + excel.get(1, 'B')); // Expected: 12

        // B2 dynamically updates to A1(10) + B1(12) = 22
        System.out.println("Get B2 after A1 update: " + excel.get(2, 'B')); // Expected: 22
        
        // Overwrite formula on B1 with raw value 100
        System.out.println("Setting B1 to raw value 100 (Formula deleted)...");
        excel.set(1, 'B', 100);
        
        // B2 dynamically updates to A1(10) + B1(100) = 110
        System.out.println("Get B2 after B1 overwrite: " + excel.get(2, 'B')); // Expected: 110
    }
}
















































import java.util.*;

class Excel {

    // A cell is represented by zero-based row and column indices.
    private record Cell(int row, int col) {}

    private final int[][] values;

    // formula[cell] = {referenced cell -> number of occurrences}.
    // Example: C1 = A1 + A1 + B1 means {A1 -> 2, B1 -> 1}.
    private final Map<Cell, Map<Cell, Integer>> formulas = new HashMap<>();

    // dependents[source] = {formula cell -> multiplicity}.
    // This reverse graph tells us which cells must change when source changes.
    private final Map<Cell, Map<Cell, Integer>> dependents = new HashMap<>();

    public Excel(int height, char width) {
        values = new int[height][width - 'A' + 1];
    }

    private Cell cell(int row, char column) {
        return new Cell(row - 1, column - 'A');
    }

    private Cell parse(String ref) {
        return new Cell(
            Integer.parseInt(ref.substring(1)) - 1,
            ref.charAt(0) - 'A'
        );
    }

    // Converts cell references and inclusive ranges into a weighted dependency
    // map. Overlapping ranges and duplicate references retain their multiplicity.
    private Map<Cell, Integer> expand(String[] numbers) {
        Map<Cell, Integer> result = new HashMap<>();

        for (String number : numbers) {
            String[] ends = number.split(":");
            Cell start = parse(ends[0]);
            Cell end = ends.length == 1 ? start : parse(ends[1]);

            for (int r = start.row(); r <= end.row(); r++) {
                for (int c = start.col(); c <= end.col(); c++) {
                    result.merge(new Cell(r, c), 1, Integer::sum);
                }
            }
        }

        return result;
    }

    // Remove the outgoing edges of a formula that is being overwritten.
    // The target may itself still have dependents; those edges must remain.
    private void removeOldFormula(Cell target) {
        Map<Cell, Integer> old = formulas.remove(target);
        if (old == null) {
            return;
        }

        for (Cell source : old.keySet()) {
            Map<Cell, Integer> reverse = dependents.get(source);
            reverse.remove(target);

            if (reverse.isEmpty()) {
                dependents.remove(source);
            }
        }
    }

    // Propagate a value change through all downstream formula cells.
    //
    // If source changes by delta and a dependent references it k times,
    // that dependent changes by k * delta. We update its cached value and
    // propagate that change further through the graph.
    //
    // No circular references are guaranteed, so recursion terminates.
    private void propagate(Cell source, int delta) {
        if (delta == 0) {
            return;
        }

        Map<Cell, Integer> next = dependents.get(source);
        if (next == null) {
            return;
        }

        // No dependency edges are modified during propagation, so iterating
        // over this map is safe.
        for (var edge : next.entrySet()) {
            Cell dependent = edge.getKey();
            int weight = edge.getValue();

            int change = delta * weight;
            values[dependent.row()][dependent.col()] += change;

            // A downstream formula must receive the dependent's change too.
            propagate(dependent, change);
        }
    }

    public void set(int row, char column, int val) {
        Cell target = cell(row, column);

        // Remember the old displayed value before replacing a formula.
        int oldValue = values[target.row()][target.col()];

        // A direct assignment permanently replaces this cell's formula.
        removeOldFormula(target);
        values[target.row()][target.col()] = val;

        // Only the difference matters to existing dependents.
        // They already contain contributions from oldValue.
        propagate(target, val - oldValue);
    }

    public int get(int row, char column) {
        // All cached values are kept current after every update.
        return values[row - 1][column - 'A'];
    }

    public int sum(int row, char column, String[] numbers) {
        Cell target = cell(row, column);
        int oldValue = values[target.row()][target.col()];

        // Remove old dependencies first: this call replaces any old formula.
        removeOldFormula(target);

        Map<Cell, Integer> newFormula = expand(numbers);
        formulas.put(target, newFormula);

        int newValue = 0;

        for (var entry : newFormula.entrySet()) {
            Cell source = entry.getKey();
            int weight = entry.getValue();

            // Register the reverse edge so future source changes reach target.
            dependents.computeIfAbsent(source, ignored -> new HashMap<>())
                      .put(target, weight);

            // Use cached source values to calculate the new formula result.
            newValue += values[source.row()][source.col()] * weight;
        }

        values[target.row()][target.col()] = newValue;

        // Changing the formula can change the target's displayed value.
        // Its own dependents must receive that difference.
        propagate(target, newValue - oldValue);

        return newValue;
    }

    /*
     * Complexity analysis:
     *
     * Let V = height * width.
     * Let E = total number of distinct dependency edges stored.
     * Let R = number of individual cells expanded by a sum formula.
     * Let A = number of downstream dependency-edge traversals during
     *         one propagation, counting repeated visits when paths converge.
     *
     * Constructor: O(V) time and O(V) space for the matrix.
     *
     * get(): O(1), because every cell's cached value is current.
     *
     * set(): O(1) expected time for the direct assignment and removal of
     *        the target's old outgoing edges, plus O(A) propagation work.
     *
     * sum(): O(R) to expand references, plus O(R) expected work to register
     *        edges, plus O(A) to propagate the resulting value change.
     *
     * Storage: O(V + E), covering the value matrix, formulas, and reverse
     *          dependency graph. Formula edges are stored in both directions,
     *          which changes only the constant factor.
     *
     * In a DAG, each downstream cell's cached value remains correct after
     * propagation. A shared downstream node may be visited more than once
     * when multiple paths lead to it, but each visit applies the correct
     * contribution from that path. Thus A can exceed V in a general DAG.
     *
     * For this problem, the grid is at most 26 x 26 and there are at most
     * 100 operations, so this design is comfortably efficient.
     */
}


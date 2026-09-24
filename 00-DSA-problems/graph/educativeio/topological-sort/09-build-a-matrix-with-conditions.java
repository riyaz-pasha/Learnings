import java.util.*;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * BUILD A MATRIX WITH CONDITIONS (LeetCode 2392)
 * ============================================================================
 * 
 * STATEMENT:
 * You are given a positive integer k, and two 2D integer arrays: `rowConditions` 
 * and `colConditions`.
 * - rowConditions[i] = [above_i, below_i] means above_i must appear in a row 
 *   strictly above below_i.
 * - colConditions[i] = [left_i, right_i] means left_i must appear in a column 
 *   strictly to the left of right_i.
 * 
 * Build a k x k matrix containing integers from 1 to k exactly once. The 
 * remaining cells can be 0. Return any valid matrix. If no valid matrix 
 * exists (due to contradictions/cycles), return an empty matrix.
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We need to place the numbers 1 through k in a k x k grid. 
 * Notice that the vertical placement (rows) and horizontal placement (columns) 
 * are completely independent of each other. 
 * - The `rowConditions` only dictate the top-to-bottom order.
 * - The `colConditions` only dictate the left-to-right order.
 * 
 * This is a classic Topological Sort problem! We can perform a topological 
 * sort on the row conditions to determine the exact row index for every number. 
 * We do the same for the column conditions to find the column index. If either 
 * sort detects a cycle (e.g., A must be above B, and B must be above A), it's 
 * impossible, and we return an empty matrix.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: What if a number 1 to k does not appear in any condition?
 *    A: It can be placed in any available row/column. Our Topological Sort naturally 
 *       handles this by assigning it an in-degree of 0.
 * 2. Q: Can there be duplicate conditions (e.g., [1, 2] appears twice)?
 *    A: Yes, we should use a Set for our Adjacency List or safely ignore duplicates 
 *       to avoid artificially inflating in-degrees.
 * 3. Q: Can I place multiple numbers in the same row or column?
 *    A: Yes, but since we are placing exactly 'k' numbers in 'k' distinct rows 
 *       and 'k' distinct columns based on our sorted orders, every number will 
 *       get its own unique row and unique column.
 * 4. Q: What should be returned if k=0?
 *    A: Constraints say k >= 2, so we don't need to worry about k=0.
 * 5. Q: Is there a specific format for the empty matrix?
 *    A: Usually, returning `new int[0][0]` is the standard representation of an 
 *       empty 2D array in Java.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Identify the independence: State clearly, "The row conditions and column 
 *    conditions do not affect each other. We can solve for the row order and 
 *    column order independently."
 * 2. Map to Graph Theory: "Both problems are dependency resolution problems, 
 *    which means we need to find a Topological Sort of a Directed Graph."
 * 3. Choose the Algorithm: "I will use Kahn's Algorithm (BFS) because it's 
 *    intuitive, naturally groups independent nodes, and makes cycle detection 
 *    very easy (if the sorted result length < k, there is a cycle)."
 * 4. Matrix Assembly: "Once I have the `rowOrder` and `colOrder` arrays, I 
 *    will map each number 1..k to its index in these arrays, giving me the 
 *    exact (row, col) coordinates to place it in the matrix."
 * 
 * ----------------------------------------------------------------------------
 * COMPLEXITY ANALYSIS:
 * - Time Complexity: O(k^2 + E). 
 *   Building the graphs and running BFS takes O(k + E) where E is the number 
 *   of conditions. Filling the k x k matrix takes O(k^2). 
 *   Total Time = O(k^2 + rowConditions.length + colConditions.length).
 * - Space Complexity: O(k^2 + E). 
 *   The k x k matrix requires O(k^2) space. The adjacency lists require O(k + E) space.
 * 
 * ============================================================================
 */
public class BuildMatrixWithConditions {

    /**
     * Core method to build the matrix.
     */
    public int[][] buildMatrix(int k, int[][] rowConditions, int[][] colConditions) {
        // Step 1: Find the topological order for rows
        int[] rowOrder = topologicalSort(k, rowConditions);
        // If there's a cycle in row conditions, return empty matrix
        if (rowOrder.length == 0) {
            return new int[0][0];
        }

        // Step 2: Find the topological order for columns
        int[] colOrder = topologicalSort(k, colConditions);
        // If there's a cycle in col conditions, return empty matrix
        if (colOrder.length == 0) {
            return new int[0][0];
        }

        // Step 3: Map each number (1 to k) to its assigned row and column index
        // Example: If rowOrder is [3, 1, 2], then number 3 goes to row 0, 1 goes to row 1, etc.
        int[] rowIndexMap = new int[k + 1];
        int[] colIndexMap = new int[k + 1];
        
        for (int i = 0; i < k; i++) {
            rowIndexMap[rowOrder[i]] = i;
            colIndexMap[colOrder[i]] = i;
        }

        // Step 4: Construct the final matrix
        int[][] matrix = new int[k][k];
        for (int num = 1; num <= k; num++) {
            int r = rowIndexMap[num];
            int c = colIndexMap[num];
            matrix[r][c] = num;
        }

        return matrix;
    }

    /**
     * Kahn's Algorithm for Topological Sorting.
     * 
     * @param k The number of nodes (1 to k)
     * @param conditions The directed edges
     * @return An array of length k containing the valid order, or an empty array if a cycle exists.
     */
    private int[] topologicalSort(int k, int[][] conditions) {
        // Use an array of Sets for the Adjacency List to avoid duplicate edges
        List<Set<Integer>> adj = new ArrayList<>();
        for (int i = 0; i <= k; i++) {
            adj.add(new HashSet<>());
        }
        
        int[] inDegree = new int[k + 1];
        
        // Build Graph
        for (int[] edge : conditions) {
            int u = edge[0];
            int v = edge[1];
            // Only increment inDegree if this is a new, unique edge
            if (adj.get(u).add(v)) {
                inDegree[v]++;
            }
        }
        
        // Find nodes with 0 in-degree (no prerequisites)
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 1; i <= k; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        int[] order = new int[k];
        int index = 0;
        
        // Process BFS
        while (!queue.isEmpty()) {
            int current = queue.poll();
            order[index++] = current;
            
            // "Remove" the node by reducing the in-degree of its neighbors
            for (int neighbor : adj.get(current)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        // Cycle Detection: If we didn't add exactly k nodes, a cycle trapped some nodes.
        if (index != k) {
            return new int[0];
        }
        
        return order;
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING (Using modern Java Records & Streams)
     * ========================================================================
     */
    
    record TestCase(int k, int[][] rowConditions, int[][] colConditions, boolean expectValid, String description) {}

    public static void main(String[] args) {
        BuildMatrixWithConditions solver = new BuildMatrixWithConditions();

        var testCases = List.of(
            new TestCase(
                3, 
                new int[][]{{1, 2}, {3, 2}}, 
                new int[][]{{2, 1}, {3, 2}}, 
                true, 
                "Standard Valid Matrix (Multiple valid topologies possible)"
            ),
            new TestCase(
                3, 
                new int[][]{{1, 2}, {2, 3}, {3, 1}, {2, 3}}, 
                new int[][]{{2, 1}}, 
                false, 
                "Row Conditions Contain a Cycle (1->2->3->1)"
            ),
            new TestCase(
                2, 
                new int[][]{}, 
                new int[][]{}, 
                true, 
                "No Conditions (Any diagonal/placement works)"
            )
        );

        System.out.println("Running test cases for Build a Matrix With Conditions...");
        System.out.println("-".repeat(80));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            
            int[][] result = solver.buildMatrix(tc.k(), tc.rowConditions(), tc.colConditions());
            
            boolean isValid = result.length > 0;
            boolean passed = isValid == tc.expectValid();
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("Expected Valid? %b | Got Valid? %b\n", tc.expectValid(), isValid);
            System.out.printf("Status: %s\n", passed ? "✅ PASSED" : "❌ FAILED");
            
            if (isValid) {
                System.out.println("Result Matrix:");
                for (int[] row : result) {
                    System.out.println(Arrays.toString(row));
                }
            } else {
                System.out.println("Result Matrix: [] (Empty)");
            }
            System.out.println("-".repeat(80));
        });
    }
}

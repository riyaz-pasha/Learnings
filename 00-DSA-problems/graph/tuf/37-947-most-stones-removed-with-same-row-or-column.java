/**
 * PROBLEM ANALYSIS:
 * ==================
 * This is a graph connectivity problem disguised as a stone removal problem.
 * 
 * KEY INSIGHT:
 * - A stone can be removed if it shares a row OR column with another stone
 * - We can keep removing stones until we're left with stones that don't share 
 *   rows/columns with any other remaining stones
 * - The maximum number of stones we can remove = Total stones - Number of connected components
 * 
 * WHY THIS WORKS:
 * - Think of stones sharing a row or column as being "connected"
 * - In each connected component, we can remove all stones except one
 * - The last stone in each component cannot be removed (no other stone to reference)
 * - Therefore: removable stones = n - number_of_components
 * 
 * INTERVIEW APPROACH:
 * ===================
 * 1. Start with examples - trace through the removal process
 * 2. Notice that stones in the same row/column form groups
 * 3. Realize it's about finding connected components
 * 4. Choose approach: Union-Find (optimal) or DFS/BFS (also valid)
 * 
 * HOW TO COME UP WITH THIS SOLUTION:
 * ===================================
 * Step 1: Understand what "can be removed" means
 *         - Need at least one other stone in same row OR column
 * 
 * Step 2: Work through examples manually
 *         - Notice you can remove stones in a chain
 *         - The last stone standing in each "group" can't be removed
 * 
 * Step 3: Model as a graph
 *         - Stones are nodes
 *         - Edge exists if stones share row or column
 *         - Each connected component leaves exactly 1 stone
 * 
 * Step 4: Choose algorithm
 *         - Union-Find is perfect for counting components
 *         - DFS/BFS also works but Union-Find is cleaner
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class StoneRemoval {
    
    // ============================================================================
    // APPROACH 1: UNION-FIND (OPTIMAL)
    // ============================================================================
    // Time Complexity: O(n * α(n)) where α is inverse Ackermann (nearly constant)
    // Space Complexity: O(n)
    // This is the best approach for interviews - clean and efficient
    
    /**
     * Union-Find solution treating stones as nodes in a graph.
     * Stones sharing row/column are in the same component.
     */
    public int removeStones(int[][] stones) {
        int n = stones.length;
        UnionFind uf = new UnionFind(n);
        
        // Map to track which stone occupies each row and column
        // Key: row/column identifier, Value: stone index
        Map<Integer, Integer> rowMap = new HashMap<>();
        Map<Integer, Integer> colMap = new HashMap<>();
        
        for (int i = 0; i < n; i++) {
            int row = stones[i][0];
            int col = stones[i][1];
            
            // If we've seen this row before, union current stone with the stone in that row
            if (rowMap.containsKey(row)) {
                uf.union(i, rowMap.get(row));
            }
            rowMap.put(row, i);
            
            // If we've seen this column before, union current stone with the stone in that column
            if (colMap.containsKey(col)) {
                uf.union(i, colMap.get(col));
            }
            colMap.put(col, i);
        }
        
        // Answer = total stones - number of connected components
        return n - uf.getComponentCount();
    }
    
    /**
     * Union-Find data structure with path compression and union by rank.
     * This achieves nearly O(1) operations.
     */
    class UnionFind {
        private int[] parent;
        private int[] rank;
        private int components;
        
        public UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            components = size;
            
            // Initially, each stone is its own component
            for (int i = 0; i < size; i++) {
                parent[i] = i;
                rank[i] = 1;
            }
        }
        
        /**
         * Find with path compression - makes tree flatter for faster future lookups
         */
        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }
        
        /**
         * Union by rank - attach smaller tree under larger tree
         * Returns true if union was performed (elements were in different sets)
         */
        public boolean union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            
            if (rootX == rootY) {
                return false; // Already in same component
            }
            
            // Union by rank: attach smaller tree under larger tree
            if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }
            
            components--; // Merged two components into one
            return true;
        }
        
        public int getComponentCount() {
            return components;
        }
    }
    
    // ============================================================================
    // APPROACH 2: DFS (Graph Traversal)
    // ============================================================================
    // Time Complexity: O(n²) - checking all pairs
    // Space Complexity: O(n) - recursion stack and visited array
    // Good alternative if you're more comfortable with DFS
    
    /**
     * DFS approach: Build adjacency list and count connected components
     */
    public int removeStonesDFS(int[][] stones) {
        int n = stones.length;
        boolean[] visited = new boolean[n];
        int components = 0;
        
        // Count connected components using DFS
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(stones, visited, i);
                components++; // Found a new component
            }
        }
        
        return n - components;
    }
    
    /**
     * DFS to mark all stones in the same connected component
     */
    private void dfs(int[][] stones, boolean[] visited, int index) {
        visited[index] = true;
        
        // Check all other stones
        for (int i = 0; i < stones.length; i++) {
            if (!visited[i]) {
                // If stones share row or column, they're connected
                if (stones[index][0] == stones[i][0] || stones[index][1] == stones[i][1]) {
                    dfs(stones, visited, i);
                }
            }
        }
    }
    
    // ============================================================================
    // APPROACH 3: UNION-FIND WITH ROW-COLUMN MAPPING (ALTERNATIVE)
    // ============================================================================
    // Time Complexity: O(n * α(n))
    // Space Complexity: O(n + unique_rows + unique_cols)
    // Clever approach: treat rows and columns themselves as nodes
    
    /**
     * Advanced Union-Find: rows and columns are nodes in the graph
     * This avoids explicitly connecting stones - instead we connect rows to columns
     */
    public int removeStonesAdvanced(int[][] stones) {
        UnionFindMap uf = new UnionFindMap();
        
        for (int[] stone : stones) {
            int row = stone[0];
            int col = stone[1] + 10001; // Offset to distinguish from rows
            
            // Union the row and column - any stone connects its row to its column
            uf.union(row, col);
        }
        
        // Each stone can be removed except one per component
        // But we need to count stones, not row/column nodes
        Set<Integer> uniqueParents = new HashSet<>();
        for (int[] stone : stones) {
            uniqueParents.add(uf.find(stone[0]));
        }
        
        return stones.length - uniqueParents.size();
    }
    
    /**
     * Union-Find that works with arbitrary integers (for row/column IDs)
     */
    class UnionFindMap {
        private Map<Integer, Integer> parent = new HashMap<>();
        
        public int find(int x) {
            if (!parent.containsKey(x)) {
                parent.put(x, x);
            }
            if (parent.get(x) != x) {
                parent.put(x, find(parent.get(x))); // Path compression
            }
            return parent.get(x);
        }
        
        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            if (rootX != rootY) {
                parent.put(rootX, rootY);
            }
        }
    }
    
    // ============================================================================
    // TEST CASES
    // ============================================================================
    
    public static void main(String[] args) {
        StoneRemoval solution = new StoneRemoval();
        
        // Test Case 1
        int[][] stones1 = {{0,0},{0,1},{1,0},{1,2},{2,1},{2,2}};
        System.out.println("Test 1 - Expected: 5, Got: " + solution.removeStones(stones1));
        // Explanation: All 6 stones are connected, leaving 1 component, so 6-1=5
        
        // Test Case 2
        int[][] stones2 = {{0,0},{0,2},{1,1},{2,0},{2,2}};
        System.out.println("Test 2 - Expected: 3, Got: " + solution.removeStones(stones2));
        // Explanation: Two components {[0,0],[0,2],[2,0],[2,2]} and {[1,1]}, so 5-2=3
        
        // Test Case 3
        int[][] stones3 = {{0,0}};
        System.out.println("Test 3 - Expected: 0, Got: " + solution.removeStones(stones3));
        // Explanation: One stone, one component, 1-1=0
        
        // Test Case 4: All stones in same row
        int[][] stones4 = {{0,0},{0,1},{0,2}};
        System.out.println("Test 4 - Expected: 2, Got: " + solution.removeStones(stones4));
        // Explanation: All share row 0, one component, 3-1=2
        
        // Test Case 5: Disconnected stones
        int[][] stones5 = {{0,0},{1,1},{2,2}};
        System.out.println("Test 5 - Expected: 0, Got: " + solution.removeStones(stones5));
        // Explanation: No stones share row/col, three components, 3-3=0
    }
}

/**
 * INTERVIEW TIPS:
 * ===============
 * 
 * 1. CLARIFYING QUESTIONS:
 *    - Can multiple stones be at the same position? (No, per problem)
 *    - Are coordinates always non-negative? (Check constraints)
 *    - What's the range of coordinates? (Affects space complexity)
 * 
 * 2. PROBLEM-SOLVING STEPS:
 *    a) Work through examples manually
 *    b) Identify the pattern: connected components
 *    c) Recognize it's a graph problem
 *    d) Choose appropriate data structure (Union-Find)
 * 
 * 3. COMMUNICATION:
 *    - Explain the key insight: n - components
 *    - Justify why Union-Find is optimal
 *    - Mention time/space complexity upfront
 * 
 * 4. EDGE CASES TO DISCUSS:
 *    - Single stone (return 0)
 *    - All stones connected (return n-1)
 *    - No stones connected (return 0)
 *    - Stones forming a line (row or column)
 * 
 * 5. OPTIMIZATION DISCUSSION:
 *    - Union-Find with path compression is nearly O(n)
 *    - DFS is O(n²) but easier to implement
 *    - Choose based on interview context
 * 
 * 6. FOLLOW-UP QUESTIONS TO EXPECT:
 *    - "What if we want the actual removal sequence?" (topological sort)
 *    - "What if coordinates can be very large?" (coordinate compression)
 *    - "Can you optimize space?" (use the advanced approach)
 */

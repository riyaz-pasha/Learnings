import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a TREE LEVEL TRAVERSAL problem with INDEXING testing:
 * 1. Understanding of complete binary tree indexing
 * 2. Level-order traversal (BFS)
 * 3. Handling integer overflow (index normalization)
 * 4. Careful tracking of leftmost and rightmost nodes per level
 * 5. Understanding width calculation with null nodes
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. WIDTH DEFINITION:
 *    - Distance between leftmost and rightmost non-null nodes
 *    - INCLUDES null nodes in between
 *    - Like measuring width if tree were complete
 * 
 * 2. COMPLETE BINARY TREE INDEXING:
 *    - Root at index 0 (or 1)
 *    - Left child of node i: 2*i + 1 (or 2*i if 1-indexed)
 *    - Right child of node i: 2*i + 2 (or 2*i + 1 if 1-indexed)
 *    - Width = rightmost_index - leftmost_index + 1
 * 
 * 3. CRITICAL ISSUE: INTEGER OVERFLOW!
 *    - Indices can grow exponentially: 2^depth
 *    - For depth 31: index can exceed Integer.MAX_VALUE
 *    - Solution: Normalize indices at each level (subtract leftmost)
 * 
 * VISUALIZATION:
 * --------------
 * 
 * Example 1:
 *         1           Level 0: index 0
 *        / \          Width = 0 - 0 + 1 = 1
 *       3   2         Level 1: indices 1, 2
 *      / \   \        Width = 2 - 1 + 1 = 2
 *     5   3   9       Level 2: indices 3, 4, 5 (but 5 is null)
 *                     Actually: 3, 4, null, 5
 *                     Wait, let me recalculate...
 * 
 * Let's use 0-indexing:
 * Root 1: index 0
 * Node 3: index 1 (left child of 0: 2*0+1 = 1)
 * Node 2: index 2 (right child of 0: 2*0+2 = 2)
 * Node 5: index 3 (left child of 1: 2*1+1 = 3)
 * Node 3: index 4 (right child of 1: 2*1+2 = 4)
 * Node 9: index 6 (right child of 2: 2*2+2 = 6)
 * 
 * Level 2: leftmost = 3, rightmost = 6
 * Width = 6 - 3 + 1 = 4 ✓
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Clarify the problem
 *    "So width includes null nodes in between, like measuring
 *     the distance in a complete binary tree?"
 * 
 * 2. Explain indexing approach
 *    "I'll use complete binary tree indexing where left child
 *     is 2*i+1 and right child is 2*i+2"
 * 
 * 3. Discuss BFS
 *    "I'll do level-order traversal to process each level"
 * 
 * 4. Address overflow
 *    "Important: indices can overflow, so I'll normalize them
 *     by subtracting the leftmost index at each level"
 * 
 * 5. Walk through example
 *    [Draw tree with indices on board]
 */

// Definition for a binary tree node
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode() {}
    TreeNode(int val) { this.val = val; }
    TreeNode(int val, TreeNode left, TreeNode right) {
        this.val = val;
        this.left = left;
        this.right = right;
    }
}

class MaxWidthOfBinaryTreeSolution {
    
    /**
     * APPROACH 1: BFS WITH INDEX TRACKING - OPTIMAL
     * ==============================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Use BFS (level-order traversal)
     * 2. Assign index to each node (complete binary tree indexing)
     * 3. For each level, track leftmost and rightmost indices
     * 4. Width = rightmost - leftmost + 1
     * 5. CRITICAL: Normalize indices to prevent overflow
     * 
     * COMPLETE BINARY TREE INDEXING:
     * ------------------------------
     * Using 0-based indexing:
     * - Root: index 0
     * - Left child of i: 2*i + 1
     * - Right child of i: 2*i + 2
     * 
     * Example:
     *       0
     *      / \
     *     1   2
     *    / \ / \
     *   3  4 5  6
     * 
     * INDEX NORMALIZATION (KEY OPTIMIZATION):
     * ---------------------------------------
     * Why needed?
     * - At depth d, indices range from 0 to 2^d - 1
     * - At depth 31: 2^31 - 1 > Integer.MAX_VALUE (overflow!)
     * 
     * Solution:
     * - At each level, subtract the leftmost index
     * - This keeps indices small (relative to leftmost)
     * - Width calculation unchanged: (rightmost - leftmost) remains same
     * 
     * Example:
     * Level indices: [1000, 1001, 1005]
     * After normalization: [0, 1, 5]
     * Width: 5 - 0 + 1 = 6 (same as 1005 - 1000 + 1 = 6)
     * 
     * TIME COMPLEXITY: O(N)
     * - Visit each node exactly once in BFS
     * - N nodes → O(N) operations
     * 
     * SPACE COMPLEXITY: O(W)
     * - W = maximum width of tree
     * - Queue stores at most one full level
     * - For complete binary tree: O(N/2) = O(N)
     * - For skewed tree: O(1)
     */
    public int widthOfBinaryTree(TreeNode root) {
        if (root == null) return 0;
        
        int maxWidth = 0;
        
        // Queue stores: [node, index]
        // Using ArrayDeque for better performance than LinkedList
        Queue<Pair> queue = new LinkedList<>();
        queue.offer(new Pair(root, 0));
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            
            // Get leftmost index of this level (for normalization)
            int leftmostIndex = queue.peek().index;
            
            int first = 0, last = 0;  // First and last indices in level
            
            // Process all nodes in current level
            for (int i = 0; i < levelSize; i++) {
                Pair current = queue.poll();
                TreeNode node = current.node;
                
                // NORMALIZE: Subtract leftmost index to prevent overflow
                // This keeps indices relative to leftmost node
                int normalizedIndex = current.index - leftmostIndex;
                
                // Track first and last indices
                if (i == 0) {
                    first = normalizedIndex;
                }
                if (i == levelSize - 1) {
                    last = normalizedIndex;
                }
                
                // Add children with their indices
                if (node.left != null) {
                    // Left child index: 2 * parent_index + 1
                    queue.offer(new Pair(node.left, 2 * normalizedIndex + 1));
                }
                if (node.right != null) {
                    // Right child index: 2 * parent_index + 2
                    queue.offer(new Pair(node.right, 2 * normalizedIndex + 2));
                }
            }
            
            // Calculate width of current level
            int width = last - first + 1;
            maxWidth = Math.max(maxWidth, width);
        }
        
        return maxWidth;
    }
    
    // Helper class to store node with its index
    private static class Pair {
        TreeNode node;
        int index;
        
        Pair(TreeNode node, int index) {
            this.node = node;
            this.index = index;
        }
    }
    
    /**
     * APPROACH 2: DFS WITH LEVEL TRACKING
     * ====================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Use DFS (can be preorder or any)
     * 2. Track leftmost index seen at each level
     * 3. For each node, calculate width using current and leftmost
     * 4. Return maximum width seen
     * 
     * KEY DATA STRUCTURE:
     * - Map<Level, LeftmostIndex>
     * - Or List where index = level
     * 
     * ADVANTAGES:
     * - Less memory than BFS (no queue of full level)
     * - Can be easier to understand for some
     * 
     * DISADVANTAGES:
     * - Need to track level explicitly
     * - Need data structure for leftmost indices
     * 
     * TIME COMPLEXITY: O(N)
     * SPACE COMPLEXITY: O(H) for recursion + O(H) for leftmost map
     * - H = height of tree
     */
    public int widthOfBinaryTreeDFS(TreeNode root) {
        if (root == null) return 0;
        
        // Store leftmost index for each level
        List<Integer> leftmostIndices = new ArrayList<>();
        
        return dfsHelper(root, 0, 0, leftmostIndices);
    }
    
    private int dfsHelper(TreeNode node, int level, int index, 
                          List<Integer> leftmostIndices) {
        if (node == null) return 0;
        
        // First time visiting this level - record leftmost index
        if (level == leftmostIndices.size()) {
            leftmostIndices.add(index);
        }
        
        // Calculate width at current node
        // Width from leftmost of this level to current node
        int currentWidth = index - leftmostIndices.get(level) + 1;
        
        // Recursively check children
        // Normalize index to prevent overflow
        int leftmostOfLevel = leftmostIndices.get(level);
        int normalizedIndex = index - leftmostOfLevel;
        
        int leftWidth = dfsHelper(node.left, level + 1, 
                                  2 * normalizedIndex + 1, 
                                  leftmostIndices);
        int rightWidth = dfsHelper(node.right, level + 1, 
                                   2 * normalizedIndex + 2, 
                                   leftmostIndices);
        
        // Return maximum width found
        return Math.max(currentWidth, Math.max(leftWidth, rightWidth));
    }
    
    /**
     * APPROACH 3: BFS WITHOUT NORMALIZATION (INCORRECT FOR LARGE TREES!)
     * ===================================================================
     * 
     * This approach WILL FAIL for deep trees due to integer overflow!
     * 
     * Included to show the bug and why normalization is critical.
     * 
     * For depth 31+, indices exceed Integer.MAX_VALUE
     * → Overflow → Negative numbers → Wrong answer!
     * 
     * DON'T USE THIS IN PRODUCTION!
     */
    public int widthOfBinaryTreeNoNormalization(TreeNode root) {
        if (root == null) return 0;
        
        int maxWidth = 0;
        Queue<Pair> queue = new LinkedList<>();
        queue.offer(new Pair(root, 0));
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            int first = 0, last = 0;
            
            for (int i = 0; i < levelSize; i++) {
                Pair current = queue.poll();
                TreeNode node = current.node;
                int index = current.index;
                
                if (i == 0) first = index;
                if (i == levelSize - 1) last = index;
                
                // BUG: No normalization! Will overflow for deep trees!
                if (node.left != null) {
                    queue.offer(new Pair(node.left, 2 * index + 1));
                }
                if (node.right != null) {
                    queue.offer(new Pair(node.right, 2 * index + 2));
                }
            }
            
            maxWidth = Math.max(maxWidth, last - first + 1);
        }
        
        return maxWidth;
    }
}

/**
 * DETAILED EXAMPLE WALKTHROUGH
 * =============================
 * 
 * Example 1: [1,3,2,5,3,null,9]
 * 
 * Tree structure:
 *         1
 *        / \
 *       3   2
 *      / \   \
 *     5   3   9
 * 
 * Level 0: node 1, index 0
 *   Width = 0 - 0 + 1 = 1
 * 
 * Level 1: node 3 (index 1), node 2 (index 2)
 *   After normalization (subtract 1): indices 0, 1
 *   Width = 1 - 0 + 1 = 2
 * 
 * Level 2: 
 *   Node 5 (child of 3): parent_index=0, so 2*0+1 = 1
 *   Node 3 (child of 3): parent_index=0, so 2*0+2 = 2
 *   Node 9 (child of 2): parent_index=1, so 2*1+2 = 4
 *   After normalization (subtract 1): indices 0, 1, 3
 *   Width = 3 - 0 + 1 = 4 ✓
 * 
 * Max width = 4
 * 
 * Example 2: [1,3,2,5,null,null,9,6,null,7]
 * 
 * Tree structure:
 *         1
 *        / \
 *       3   2
 *      /     \
 *     5       9
 *    /       /
 *   6       7
 * 
 * Level 0: 1 (index 0), width = 1
 * Level 1: 3 (index 1), 2 (index 2), width = 2
 * Level 2: 5 (index 3), 9 (index 6)
 *   Normalized: 0, 3
 *   Width = 3 - 0 + 1 = 4
 * Level 3: 6 (child of 5: 2*0+1=1), 7 (child of 9: 2*3+1=7)
 *   Normalized: 0, 6
 *   Width = 6 - 0 + 1 = 7 ✓
 * 
 * Max width = 7
 */

/**
 * WHY NORMALIZATION PREVENTS OVERFLOW
 * ====================================
 * 
 * Without normalization:
 * Level 31: rightmost index = 2^31 - 1 = 2,147,483,647 (MAX_INT)
 * Level 32: index = 2 * 2,147,483,647 + 1 = OVERFLOW! (negative)
 * 
 * With normalization:
 * At each level, subtract leftmost index
 * Indices stay relative to leftmost (never exceed width of tree)
 * Width calculation: (last - first) remains correct
 * 
 * Example:
 * Without: [1000000000, 1000000001, 1000000005]
 *   Children: 2*1000000005 + 2 = OVERFLOW!
 * 
 * With: [0, 1, 5] (normalized)
 *   Children: 2*5 + 2 = 12 (safe!)
 *   Width: 5 - 0 + 1 = 6 (correct!)
 */

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach         | Time | Space | Notes
 * -----------------|------|-------|---------------------------
 * BFS + Normalize  | O(N) | O(W)  | Best, handles overflow
 * DFS + Map        | O(N) | O(H)  | Good, less space
 * BFS No Normalize | O(N) | O(W)  | BUGGY for deep trees!
 * 
 * Where:
 * - N = number of nodes
 * - W = maximum width (can be O(N) for complete tree)
 * - H = height of tree
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        MaxWidthOfBinaryTreeSolution sol = new MaxWidthOfBinaryTreeSolution();
        
        System.out.println("=== Test Case 1: Example 1 ===");
        //         1
        //        / \
        //       3   2
        //      / \   \
        //     5   3   9
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(3);
        root1.right = new TreeNode(2);
        root1.left.left = new TreeNode(5);
        root1.left.right = new TreeNode(3);
        root1.right.right = new TreeNode(9);
        
        System.out.println("BFS:      " + sol.widthOfBinaryTree(root1));
        System.out.println("DFS:      " + sol.widthOfBinaryTreeDFS(root1));
        System.out.println("Expected: 4");
        
        System.out.println("\n=== Test Case 2: Example 2 ===");
        //         1
        //        / \
        //       3   2
        //      /     \
        //     5       9
        //    /       /
        //   6       7
        TreeNode root2 = new TreeNode(1);
        root2.left = new TreeNode(3);
        root2.right = new TreeNode(2);
        root2.left.left = new TreeNode(5);
        root2.right.right = new TreeNode(9);
        root2.left.left.left = new TreeNode(6);
        root2.right.right.left = new TreeNode(7);
        
        System.out.println("BFS:      " + sol.widthOfBinaryTree(root2));
        System.out.println("DFS:      " + sol.widthOfBinaryTreeDFS(root2));
        System.out.println("Expected: 7");
        
        System.out.println("\n=== Test Case 3: Example 3 ===");
        //     1
        //    / \
        //   3   2
        //  /
        // 5
        TreeNode root3 = new TreeNode(1);
        root3.left = new TreeNode(3);
        root3.right = new TreeNode(2);
        root3.left.left = new TreeNode(5);
        
        System.out.println("BFS:      " + sol.widthOfBinaryTree(root3));
        System.out.println("DFS:      " + sol.widthOfBinaryTreeDFS(root3));
        System.out.println("Expected: 2");
        
        System.out.println("\n=== Test Case 4: Single Node ===");
        TreeNode root4 = new TreeNode(1);
        System.out.println("Result:   " + sol.widthOfBinaryTree(root4));
        System.out.println("Expected: 1");
        
        System.out.println("\n=== Test Case 5: Left Skewed ===");
        //     1
        //    /
        //   2
        //  /
        // 3
        TreeNode root5 = new TreeNode(1);
        root5.left = new TreeNode(2);
        root5.left.left = new TreeNode(3);
        System.out.println("Result:   " + sol.widthOfBinaryTree(root5));
        System.out.println("Expected: 1");
        
        System.out.println("\n=== Test Case 6: Right Skewed ===");
        // 1
        //  \
        //   2
        //    \
        //     3
        TreeNode root6 = new TreeNode(1);
        root6.right = new TreeNode(2);
        root6.right.right = new TreeNode(3);
        System.out.println("Result:   " + sol.widthOfBinaryTree(root6));
        System.out.println("Expected: 1");
        
        System.out.println("\n=== Test Case 7: Complete Binary Tree ===");
        //       1
        //      / \
        //     2   3
        //    / \ / \
        //   4  5 6  7
        TreeNode root7 = new TreeNode(1);
        root7.left = new TreeNode(2);
        root7.right = new TreeNode(3);
        root7.left.left = new TreeNode(4);
        root7.left.right = new TreeNode(5);
        root7.right.left = new TreeNode(6);
        root7.right.right = new TreeNode(7);
        System.out.println("Result:   " + sol.widthOfBinaryTree(root7));
        System.out.println("Expected: 4");
        
        System.out.println("\n=== Test Case 8: Deep Tree (Testing Overflow Prevention) ===");
        TreeNode deep = new TreeNode(1);
        TreeNode curr = deep;
        for (int i = 0; i < 30; i++) {
            curr.left = new TreeNode(2);
            curr = curr.left;
        }
        System.out.println("Deep left-skewed tree (depth 30)");
        System.out.println("With normalization:    " + sol.widthOfBinaryTree(deep));
        System.out.println("Without normalization: " + sol.widthOfBinaryTreeNoNormalization(deep));
        System.out.println("Both should be: 1");
        System.out.println("(No normalization might overflow for deeper trees)");
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "Width is measured between leftmost and rightmost nodes,
 *    including null nodes in between, like in a complete tree."
 * 
 * 2. "I'll use complete binary tree indexing: root at 0,
 *    left child at 2i+1, right child at 2i+2."
 *    [Draw tree with indices on board]
 * 
 * 3. "I'll do BFS to process level by level. For each level,
 *    I track the first and last indices, and calculate width."
 * 
 * 4. "Critical optimization: index normalization. Without it,
 *    indices can overflow at depth 31+. I subtract the leftmost
 *    index at each level to keep indices relative and small."
 *    [Explain with example: [1000, 1001, 1005] → [0, 1, 5]]
 * 
 * 5. "Time is O(N) - visit each node once. Space is O(W) where
 *    W is max width, since queue stores one full level."
 * 
 * 6. "Alternative: DFS approach with map of leftmost indices
 *    per level. Same time, slightly less space (O(H) vs O(W))."
 * 
 * KEY POINTS TO EMPHASIZE:
 * - Complete binary tree indexing formula
 * - Why normalization prevents overflow
 * - Level-order traversal for processing levels
 * - Width calculation: last - first + 1
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Why normalize indices? [Prevent overflow at depth 31+]
 * - Could you use DFS? [Yes, shown alternative]
 * - What if tree has 10^6 nodes? [Still O(N), works fine]
 * - What's the maximum width? [2^(H-1) for complete tree]
 * - How to handle negative indices? [Use long or normalize]
 */

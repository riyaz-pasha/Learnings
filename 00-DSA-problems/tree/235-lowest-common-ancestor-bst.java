
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lowest Common Ancestor (LCA) in Binary Search Tree - Complete Implementation
 * 
 * PROBLEM UNDERSTANDING:
 * Find the lowest common ancestor of two nodes p and q in a BST.
 * LCA is the lowest node that has both p and q as descendants (a node can be its own descendant).
 * 
 * CRITICAL INSIGHT - BST PROPERTY:
 * This is NOT just a binary tree problem - it's a BST problem!
 * BST Property: left < root < right
 * This allows us to use VALUE COMPARISON instead of exhaustive searching!
 * 
 * KEY OBSERVATIONS:
 * 1. If both p and q are SMALLER than root → LCA is in LEFT subtree
 * 2. If both p and q are GREATER than root → LCA is in RIGHT subtree  
 * 3. If p and q are on DIFFERENT sides → root IS the LCA!
 * 4. If root equals p or q → root IS the LCA (node is ancestor of itself)
 * 
 * Example:
 *         6
 *        / \
 *       2   8
 *      / \ / \
 *     0  4 7  9
 *       / \
 *      3   5
 * 
 * LCA(2, 8) = 6  (different sides of 6)
 * LCA(2, 4) = 2  (both in left, but 2 is ancestor of 4)
 * LCA(0, 5) = 2  (both in left subtree of 6, different sides of 2)
 * 
 * TIME COMPLEXITY: O(h) where h is height
 * SPACE COMPLEXITY: O(h) recursive, O(1) iterative
 */

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode(int val) {
        this.val = val;
    }
}

class LCA_BST {
    
    /**
     * APPROACH 1: RECURSIVE SOLUTION (MOST ELEGANT) ⭐
     * 
     * STRATEGY:
     * Use BST property to determine which subtree to explore.
     * The first node where p and q diverge (or match) is the LCA.
     * 
     * DECISION TREE:
     * - Both < root → Go LEFT
     * - Both > root → Go RIGHT
     * - Otherwise → Found LCA!
     * 
     * WHY THIS WORKS:
     * The moment we find a split point (p and q on different sides),
     * that's the lowest point where both are descendants.
     * Going deeper would exclude one of them!
     * 
     * TIME: O(h) - single path from root to LCA
     * SPACE: O(h) - recursion stack
     */
    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        // Base case: empty tree (shouldn't happen with valid input)
        if (root == null) {
            return null;
        }
        
        /**
         * CASE 1: Both nodes are in LEFT subtree
         * If both p and q are smaller than root, LCA must be in left subtree
         * 
         * Example:     6
         *             /
         *            2
         *           / \
         *          0   4
         * 
         * LCA(0, 4): Both < 6, so recurse left
         */
        if (p.val < root.val && q.val < root.val) {
            return lowestCommonAncestor(root.left, p, q);
        }
        
        /**
         * CASE 2: Both nodes are in RIGHT subtree
         * If both p and q are greater than root, LCA must be in right subtree
         * 
         * Example:     6
         *               \
         *                8
         *               / \
         *              7   9
         * 
         * LCA(7, 9): Both > 6, so recurse right
         */
        if (p.val > root.val && q.val > root.val) {
            return lowestCommonAncestor(root.right, p, q);
        }
        
        /**
         * CASE 3: Split point found OR one node is root
         * This handles three scenarios:
         * 
         * 3a) p < root < q (on different sides)
         * 3b) p == root (root is one of the nodes)
         * 3c) q == root (root is one of the nodes)
         * 
         * In all cases, current root is the LCA!
         * 
         * Example:     6
         *             / \
         *            2   8
         * 
         * LCA(2, 8): 2 < 6 < 8 → Return 6
         * LCA(6, 8): 6 == root → Return 6
         */
        return root;
    }
    
    
    /**
     * APPROACH 2: ITERATIVE SOLUTION (SPACE OPTIMIZED)
     * 
     * STRATEGY:
     * Same logic as recursive, but use iteration to avoid call stack.
     * Traverse from root until we find the split point.
     * 
     * ADVANTAGES:
     * - O(1) space complexity (no recursion stack)
     * - Easier to understand flow for some people
     * - No risk of stack overflow on deep trees
     * 
     * INTERVIEW TIP:
     * Start with recursive (cleaner), mention iterative as optimization.
     * 
     * TIME: O(h)
     * SPACE: O(1)
     */
    public TreeNode lowestCommonAncestor_Iterative(TreeNode root, TreeNode p, TreeNode q) {
        TreeNode current = root;
        
        while (current != null) {
            // Both in left subtree
            if (p.val < current.val && q.val < current.val) {
                current = current.left;
            }
            // Both in right subtree
            else if (p.val > current.val && q.val > current.val) {
                current = current.right;
            }
            // Split point found!
            else {
                return current;
            }
        }
        
        return null;  // Should never reach here with valid input
    }
    
    
    /**
     * APPROACH 3: PATH COMPARISON (WORKS FOR ANY BINARY TREE)
     * 
     * WHEN TO USE:
     * - Follow-up: "What if it was just a binary tree, not BST?"
     * - Interviewer wants to see general solution
     * 
     * STRATEGY:
     * 1. Find path from root to p
     * 2. Find path from root to q
     * 3. Compare paths to find last common node
     * 
     * DISADVANTAGE:
     * - Doesn't utilize BST property
     * - More complex, more space
     * - O(h) space for both paths
     * 
     * TIME: O(h)
     * SPACE: O(h) - two paths stored
     */
    public TreeNode lowestCommonAncestor_PathComparison(TreeNode root, TreeNode p, TreeNode q) {
        List<TreeNode> pathToP = new ArrayList<>();
        List<TreeNode> pathToQ = new ArrayList<>();
        
        // Find both paths
        findPath(root, p, pathToP);
        findPath(root, q, pathToQ);
        
        // Find last common node
        TreeNode lca = null;
        int minLength = Math.min(pathToP.size(), pathToQ.size());
        
        for (int i = 0; i < minLength; i++) {
            if (pathToP.get(i) == pathToQ.get(i)) {
                lca = pathToP.get(i);
            } else {
                break;
            }
        }
        
        return lca;
    }
    
    private boolean findPath(TreeNode root, TreeNode target, List<TreeNode> path) {
        if (root == null) {
            return false;
        }
        
        path.add(root);
        
        if (root == target) {
            return true;
        }
        
        // Use BST property to guide search
        if (target.val < root.val) {
            if (findPath(root.left, target, path)) {
                return true;
            }
        } else {
            if (findPath(root.right, target, path)) {
                return true;
            }
        }
        
        path.remove(path.size() - 1);  // Backtrack
        return false;
    }
    
    
    /**
     * APPROACH 4: WITH PARENT POINTERS (ALTERNATIVE PERSPECTIVE)
     * 
     * SCENARIO:
     * If tree nodes have parent pointers, we can solve this differently.
     * 
     * STRATEGY:
     * 1. Get all ancestors of p in a set
     * 2. Traverse ancestors of q until we find one in the set
     * 
     * This is similar to finding intersection point of two linked lists!
     * 
     * TIME: O(h)
     * SPACE: O(h) for the set
     */
    static class TreeNodeWithParent {
        int val;
        TreeNodeWithParent left, right, parent;
        
        TreeNodeWithParent(int val) {
            this.val = val;
        }
    }
    
    public TreeNodeWithParent lowestCommonAncestor_WithParent(
            TreeNodeWithParent p, TreeNodeWithParent q) {
        
        // Collect all ancestors of p
        Set<TreeNodeWithParent> ancestors = new HashSet<>();
        TreeNodeWithParent current = p;
        
        while (current != null) {
            ancestors.add(current);
            current = current.parent;
        }
        
        // Find first ancestor of q that's also ancestor of p
        current = q;
        while (current != null) {
            if (ancestors.contains(current)) {
                return current;
            }
            current = current.parent;
        }
        
        return null;
    }
    
    
    /**
     * ==================== VISUAL WALKTHROUGH ====================
     * 
     * Example Tree:
     *         6
     *        / \
     *       2   8
     *      / \ / \
     *     0  4 7  9
     *       / \
     *      3   5
     * 
     * SCENARIO 1: LCA(2, 8)
     * 
     * Step 1: Start at root 6
     *         p=2 < 6? Yes
     *         q=8 < 6? No
     *         → Different sides! Return 6 ✓
     * 
     * Answer: 6 (one step!)
     * 
     * ----------------------------------------
     * 
     * SCENARIO 2: LCA(0, 5)
     * 
     * Step 1: Start at root 6
     *         p=0 < 6? Yes
     *         q=5 < 6? Yes
     *         → Both in left, go left to node 2
     * 
     * Step 2: At node 2
     *         p=0 < 2? Yes
     *         q=5 < 2? No
     *         → Different sides! Return 2 ✓
     * 
     * Answer: 2 (two steps)
     * 
     * ----------------------------------------
     * 
     * SCENARIO 3: LCA(3, 5)
     * 
     * Step 1: At root 6
     *         Both < 6 → Go left to 2
     * 
     * Step 2: At node 2
     *         Both > 2 → Go right to 4
     * 
     * Step 3: At node 4
     *         3 < 4? Yes
     *         5 > 4? Yes
     *         → Different sides! Return 4 ✓
     * 
     * Answer: 4 (three steps)
     * 
     * ----------------------------------------
     * 
     * SCENARIO 4: LCA(2, 4)
     * 
     * Step 1: At root 6
     *         Both < 6 → Go left to 2
     * 
     * Step 2: At node 2
     *         p=2 == 2? Yes (p is the current node)
     *         → Return 2 ✓
     * 
     * Answer: 2 (node can be ancestor of itself)
     */
    
    
    /**
     * ==================== COMPLEXITY ANALYSIS ====================
     * 
     * TIME COMPLEXITY: O(h) where h is height of tree
     * 
     * Why O(h)?
     * - We follow a single path from root toward LCA
     * - Never explore both subtrees
     * - Path length ≤ height
     * 
     * Best case: O(1)
     * - LCA is root (p and q on different sides of root)
     * - Example: LCA(2, 8) in the tree above
     * 
     * Average case: O(log n) for balanced BST
     * - Height of balanced tree is log n
     * 
     * Worst case: O(n) for skewed tree
     * - Tree degenerates to linked list
     * - Example:  1
     *              \
     *               2
     *                \
     *                 3
     *                  \
     *                   4
     * 
     * SPACE COMPLEXITY:
     * - Recursive: O(h) for call stack
     * - Iterative: O(1) - only pointers
     * 
     * COMPARISON TO BINARY TREE LCA:
     * - Binary Tree: O(n) time (may need to search entire tree)
     * - BST: O(h) time (use BST property to guide search)
     * - BST solution is MORE EFFICIENT!
     */
    
    
    /**
     * ==================== INTERVIEW STRATEGY GUIDE ====================
     * 
     * STEP 1: CLARIFY REQUIREMENTS (1-2 minutes)
     * Questions to ask:
     * - Are p and q guaranteed to exist in the tree?
     * - Can p or q be the root?
     * - Should we handle null inputs?
     * - Is this a BST or general binary tree?
     * 
     * STEP 2: EXPLAIN THE BST INSIGHT (2-3 minutes)
     * "The key insight is that this is a BST, not just any binary tree.
     * This means we can use value comparisons to guide our search.
     * We only need to traverse one path from root to LCA."
     * 
     * Draw example:
     *       6
     *      / \
     *     2   8
     * 
     * "For LCA(2, 8): Since 2 < 6 < 8, node 6 is the split point,
     * so it's the LCA. We don't need to search further."
     * 
     * STEP 3: EXPLAIN THE ALGORITHM (2 minutes)
     * "Three cases to consider:
     * 1. If both values < root.val → LCA is in left subtree
     * 2. If both values > root.val → LCA is in right subtree
     * 3. Otherwise → current node is the LCA"
     * 
     * STEP 4: CODE THE SOLUTION (5-7 minutes)
     * Write recursive solution (cleanest)
     * Explain while coding
     * 
     * STEP 5: TEST YOUR SOLUTION (3-4 minutes)
     * Test cases:
     * 1. Nodes on different sides of root
     * 2. Both nodes in same subtree
     * 3. One node is ancestor of other
     * 4. Nodes at different depths
     * 5. One node is root
     * 
     * STEP 6: OPTIMIZE & DISCUSS (remaining time)
     * - Mention iterative solution for O(1) space
     * - Discuss binary tree version (more complex)
     * - Time complexity: O(h) vs binary tree O(n)
     */
    
    
    /**
     * ==================== COMMON MISTAKES ====================
     */
    
    // ❌ MISTAKE 1: Not using BST property
    // This treats it like a general binary tree (inefficient)
    public TreeNode lca_Wrong1(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) {
            return root;
        }
        
        TreeNode left = lca_Wrong1(root.left, p, q);
        TreeNode right = lca_Wrong1(root.right, p, q);
        
        if (left != null && right != null) return root;
        return left != null ? left : right;
        
        // ❌ This works but is O(n) instead of O(h)!
        // Explores BOTH subtrees unnecessarily
    }
    
    // ❌ MISTAKE 2: Wrong comparison logic
    public TreeNode lca_Wrong2(TreeNode root, TreeNode p, TreeNode q) {
        // ❌ Using OR instead of AND
        if (p.val < root.val || q.val < root.val) {
            return lca_Wrong2(root.left, p, q);
        }
        // This fails when p < root < q
        return root;
    }
    
    // ❌ MISTAKE 3: Not handling node-is-ancestor case
    public TreeNode lca_Wrong3(TreeNode root, TreeNode p, TreeNode q) {
        if (p.val < root.val && q.val < root.val) {
            return lca_Wrong3(root.left, p, q);
        }
        if (p.val > root.val && q.val > root.val) {
            return lca_Wrong3(root.right, p, q);
        }
        // ❌ Forgot to handle p == root or q == root
        // Should still return root in these cases!
        if (root != p && root != q) {
            return null;  // Wrong!
        }
        return root;
    }
    
    // ❌ MISTAKE 4: Comparing node references instead of values
    public TreeNode lca_Wrong4(TreeNode root, TreeNode p, TreeNode q) {
        // ❌ Comparing nodes, not values
        if (p < root && q < root) {  // This won't compile!
            return lca_Wrong4(root.left, p, q);
        }
        // Should be: p.val < root.val
        return root;
    }
    
    
    /**
     * ==================== FOLLOW-UP QUESTIONS ====================
     */
    
    /**
     * Q1: "What if this was a general binary tree, not a BST?"
     * A: Use postorder traversal approach
     */
    public TreeNode lowestCommonAncestor_BinaryTree(TreeNode root, TreeNode p, TreeNode q) {
        // Base case
        if (root == null || root == p || root == q) {
            return root;
        }
        
        // Search both subtrees
        TreeNode left = lowestCommonAncestor_BinaryTree(root.left, p, q);
        TreeNode right = lowestCommonAncestor_BinaryTree(root.right, p, q);
        
        // If found in both subtrees, current node is LCA
        if (left != null && right != null) {
            return root;
        }
        
        // Return whichever is not null
        return left != null ? left : right;
    }
    
    
    /**
     * Q2: "Find LCA of multiple nodes (not just two)?"
     * A: Extend the logic to find split point for all nodes
     */
    public TreeNode lowestCommonAncestor_Multiple(TreeNode root, List<TreeNode> nodes) {
        if (nodes.isEmpty()) return null;
        if (nodes.size() == 1) return nodes.get(0);
        
        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        
        for (TreeNode node : nodes) {
            minVal = Math.min(minVal, node.val);
            maxVal = Math.max(maxVal, node.val);
        }
        
        TreeNode current = root;
        
        while (current != null) {
            if (current.val < minVal) {
                current = current.right;
            } else if (current.val > maxVal) {
                current = current.left;
            } else {
                return current;  // All nodes diverge from here
            }
        }
        
        return null;
    }
    
    
    /**
     * Q3: "Find distance between two nodes?"
     * A: Find LCA first, then calculate distances
     */
    public int findDistance(TreeNode root, TreeNode p, TreeNode q) {
        TreeNode lca = lowestCommonAncestor(root, p, q);
        
        int distP = distance(lca, p);
        int distQ = distance(lca, q);
        
        return distP + distQ;
    }
    
    private int distance(TreeNode from, TreeNode to) {
        if (from == to) return 0;
        
        if (to.val < from.val) {
            return 1 + distance(from.left, to);
        } else {
            return 1 + distance(from.right, to);
        }
    }
    
    
    /**
     * Q4: "What if nodes might not exist in tree?"
     * A: Verify both nodes exist first
     */
    public TreeNode lowestCommonAncestor_Verified(TreeNode root, TreeNode p, TreeNode q) {
        if (!exists(root, p) || !exists(root, q)) {
            return null;
        }
        return lowestCommonAncestor(root, p, q);
    }
    
    private boolean exists(TreeNode root, TreeNode target) {
        if (root == null) return false;
        if (root == target) return true;
        
        if (target.val < root.val) {
            return exists(root.left, target);
        } else {
            return exists(root.right, target);
        }
    }
    
    
    /**
     * ==================== UTILITY METHODS ====================
     */
    
    // Build sample BST for testing
    public TreeNode buildSampleTree() {
        /*
         *         6
         *        / \
         *       2   8
         *      / \ / \
         *     0  4 7  9
         *       / \
         *      3   5
         */
        TreeNode root = new TreeNode(6);
        root.left = new TreeNode(2);
        root.right = new TreeNode(8);
        root.left.left = new TreeNode(0);
        root.left.right = new TreeNode(4);
        root.right.left = new TreeNode(7);
        root.right.right = new TreeNode(9);
        root.left.right.left = new TreeNode(3);
        root.left.right.right = new TreeNode(5);
        return root;
    }
    
    // Find node by value (for testing)
    public TreeNode findNode(TreeNode root, int val) {
        if (root == null || root.val == val) {
            return root;
        }
        
        if (val < root.val) {
            return findNode(root.left, val);
        } else {
            return findNode(root.right, val);
        }
    }
    
    // Print path from root to node
    public void printPath(TreeNode root, TreeNode target) {
        List<Integer> path = new ArrayList<>();
        getPath(root, target, path);
        System.out.println(path);
    }
    
    private boolean getPath(TreeNode root, TreeNode target, List<Integer> path) {
        if (root == null) return false;
        
        path.add(root.val);
        if (root == target) return true;
        
        if (target.val < root.val) {
            if (getPath(root.left, target, path)) return true;
        } else {
            if (getPath(root.right, target, path)) return true;
        }
        
        path.remove(path.size() - 1);
        return false;
    }
    
    
    /**
     * ==================== TEST CASES ====================
     */
    public static void main(String[] args) {
        LCA_BST solution = new LCA_BST();
        TreeNode root = solution.buildSampleTree();
        
        System.out.println("Tree Structure:");
        System.out.println("        6");
        System.out.println("       / \\");
        System.out.println("      2   8");
        System.out.println("     / \\ / \\");
        System.out.println("    0  4 7  9");
        System.out.println("      / \\");
        System.out.println("     3   5");
        System.out.println();
        
        // Test Case 1: Nodes on different sides of root
        System.out.println("=== Test 1: LCA(2, 8) ===");
        TreeNode p1 = solution.findNode(root, 2);
        TreeNode q1 = solution.findNode(root, 8);
        TreeNode lca1 = solution.lowestCommonAncestor(root, p1, q1);
        System.out.println("Expected: 6, Got: " + lca1.val);
        System.out.print("Path to 2: "); solution.printPath(root, p1);
        System.out.print("Path to 8: "); solution.printPath(root, q1);
        
        // Test Case 2: Both in same subtree
        System.out.println("\n=== Test 2: LCA(0, 5) ===");
        TreeNode p2 = solution.findNode(root, 0);
        TreeNode q2 = solution.findNode(root, 5);
        TreeNode lca2 = solution.lowestCommonAncestor(root, p2, q2);
        System.out.println("Expected: 2, Got: " + lca2.val);
        System.out.print("Path to 0: "); solution.printPath(root, p2);
        System.out.print("Path to 5: "); solution.printPath(root, q2);
        
        // Test Case 3: One node is ancestor of other
        System.out.println("\n=== Test 3: LCA(2, 4) ===");
        TreeNode p3 = solution.findNode(root, 2);
        TreeNode q3 = solution.findNode(root, 4);
        TreeNode lca3 = solution.lowestCommonAncestor(root, p3, q3);
        System.out.println("Expected: 2, Got: " + lca3.val);
        System.out.println("(Node can be ancestor of itself)");
        
        // Test Case 4: Deep nodes
        System.out.println("\n=== Test 4: LCA(3, 5) ===");
        TreeNode p4 = solution.findNode(root, 3);
        TreeNode q4 = solution.findNode(root, 5);
        TreeNode lca4 = solution.lowestCommonAncestor(root, p4, q4);
        System.out.println("Expected: 4, Got: " + lca4.val);
        System.out.print("Path to 3: "); solution.printPath(root, p4);
        System.out.print("Path to 5: "); solution.printPath(root, q4);
        
        // Test Case 5: One node is root
        System.out.println("\n=== Test 5: LCA(6, 9) ===");
        TreeNode p5 = root;
        TreeNode q5 = solution.findNode(root, 9);
        TreeNode lca5 = solution.lowestCommonAncestor(root, p5, q5);
        System.out.println("Expected: 6, Got: " + lca5.val);
        
        // Test iterative solution
        System.out.println("\n=== Test 6: Iterative vs Recursive ===");
        TreeNode p6 = solution.findNode(root, 3);
        TreeNode q6 = solution.findNode(root, 7);
        TreeNode lcaRec = solution.lowestCommonAncestor(root, p6, q6);
        TreeNode lcaIter = solution.lowestCommonAncestor_Iterative(root, p6, q6);
        System.out.println("LCA(3, 7):");
        System.out.println("Recursive: " + lcaRec.val);
        System.out.println("Iterative: " + lcaIter.val);
        System.out.println("Match: " + (lcaRec == lcaIter));
        
        // Test distance calculation
        System.out.println("\n=== Test 7: Distance Between Nodes ===");
        int dist = solution.findDistance(root, p4, q4);  // Distance(3, 5)
        System.out.println("Distance between 3 and 5: " + dist);
        System.out.println("Expected: 2 (3→4 and 5→4)");
    }
}

/**
 * ==================== KEY TAKEAWAYS ====================
 * 
 * 1. **USE THE BST PROPERTY**: This is the main insight!
 *    Value comparison guides search → O(h) instead of O(n)
 * 
 * 2. **Three Simple Cases**:
 *    - Both < root → Go left
 *    - Both > root → Go right
 *    - Otherwise → Found LCA
 * 
 * 3. **One Path Traversal**: Never explore both subtrees
 * 
 * 4. **Node Can Be Own Ancestor**: Don't forget this case!
 * 
 * 5. **Recursive vs Iterative**:
 *    - Recursive: Cleaner code, O(h) space
 *    - Iterative: O(1) space, same logic
 * 
 * 6. **BST vs Binary Tree**:
 *    - BST: O(h) time using value comparison
 *    - Binary Tree: O(n) time, must search both subtrees
 * 
 * INTERVIEW TIPS:
 * - Start with recursive solution (cleanest)
 * - Emphasize BST property in explanation
 * - Draw example showing split point
 * - Mention iterative as space optimization
 * - Know binary tree version for follow-up
 * 
 * RELATED PROBLEMS:
 * - LC 235: LCA of BST (this problem)
 * - LC 236: LCA of Binary Tree (harder)
 * - LC 1644: LCA of Binary Tree II (nodes might not exist)
 * - LC 1650: LCA of Binary Tree III (with parent pointers)
 * - LC 1676: LCA of Binary Tree IV (multiple nodes)
 * 
 * COMMON VARIATIONS:
 * - Find distance between two nodes
 * - LCA with parent pointers
 * - LCA of multiple nodes
 * - Verify nodes exist before finding LCA
 * - Find path between two nodes
 */

import java.util.ArrayList;
import java.util.List;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * PROBLEM: Given a BST and a node p, find the inorder successor of p.
 * The successor is the node with the smallest key greater than p.val.
 * 
 * This is a BINARY SEARCH TREE problem testing:
 * 1. Understanding of inorder traversal (sorted order)
 * 2. BST properties and navigation
 * 3. Two distinct cases for finding successor
 * 4. Optimization using BST structure
 * 5. O(H) solution without parent pointers
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. INORDER TRAVERSAL OF BST = SORTED ORDER
 *    - Successor = next element in sorted order
 *    - Next larger element after p.val
 * 
 * 2. TWO CASES FOR SUCCESSOR:
 *    CASE A: Node has right subtree
 *      → Successor is leftmost node in right subtree
 *    CASE B: Node has no right subtree
 *      → Successor is the first ancestor where we went left
 * 
 * 3. BST PROPERTY ENABLES O(H) SOLUTION:
 *    - Track potential successor while searching for p
 *    - Any node where we go left is a potential successor
 *    - The last such node is the actual successor
 * 
 * 4. NO PARENT POINTERS NEEDED!
 *    - Can solve with just BST search
 *    - Track successor candidate during traversal
 * 
 * VISUALIZATION:
 * --------------
 * 
 * BST:          5
 *              / \
 *             3   6
 *            / \
 *           2   4
 *          /
 *         1
 * 
 * Inorder: [1, 2, 3, 4, 5, 6]
 * 
 * Find successor of 3:
 * - 3 has right subtree (4)
 * - Successor = leftmost of right = 4 ✓
 * 
 * Find successor of 4:
 * - 4 has no right subtree
 * - 4 is right child of 3
 * - Need to go up to ancestor (5) where we went left
 * - Successor = 5 ✓
 * 
 * Find successor of 6:
 * - 6 has no right subtree
 * - 6 is the largest element
 * - No successor, return null ✓
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Clarify requirements
 *    "So I need to find the next element in inorder traversal?"
 *    "Can I assume p is guaranteed to be in the tree?"
 * 
 * 2. Explain the two cases
 *    "There are two cases: if p has a right child, the successor
 *     is the leftmost node in the right subtree. Otherwise, I need
 *     to track the successor while searching for p."
 * 
 * 3. Show optimal solution
 *    "I can solve this in one pass using BST properties."
 * 
 * 4. Walk through example
 *    [Draw tree, show both cases]
 */

// Definition for a binary tree node
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode(int x) { val = x; }
}

class Solution {
    
    /**
     * APPROACH 1: ITERATIVE WITH BST SEARCH - OPTIMAL
     * ================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Search for node p using BST properties
     * 2. While searching, track potential successor:
     *    - Whenever we go LEFT, current node is potential successor
     *    - (Because going left means current > target)
     * 3. After finding p, check if it has right subtree:
     *    - If yes: successor = leftmost in right subtree
     *    - If no: successor = the tracked potential successor
     * 
     * KEY INSIGHT: Why track when going left?
     * ----------------------------------------
     * When we go left from node N, it means:
     * - p.val < N.val (that's why we went left)
     * - N is GREATER than p
     * - N is a potential successor
     * - The LAST such N we encounter is the closest successor
     * 
     * DETAILED WALKTHROUGH:
     * ---------------------
     * BST:          5
     *              / \
     *             3   6
     *            / \
     *           2   4
     *          /
     *         1
     * 
     * Example 1: Find successor of 2
     * Start at 5: 2 < 5, successor = 5, go left
     * At 3: 2 < 3, successor = 3, go left
     * At 2: found p!
     * p has no right subtree
     * Return tracked successor = 3 ✓
     * 
     * Example 2: Find successor of 3
     * Start at 5: 3 < 5, successor = 5, go left
     * At 3: found p!
     * p has right subtree (4)
     * Return leftmost of right = 4 ✓
     * 
     * Example 3: Find successor of 6
     * Start at 5: 6 > 5, go right
     * At 6: found p!
     * p has no right subtree
     * No successor was tracked (we never went left)
     * Return null ✓
     * 
     * TIME COMPLEXITY: O(H)
     * - H = height of tree
     * - Search for p: O(H)
     * - Find leftmost in right: O(H)
     * - Total: O(H)
     * 
     * SPACE COMPLEXITY: O(1)
     * - Only using a few pointers
     * - No recursion, no stack
     * 
     * THIS IS THE OPTIMAL SOLUTION!
     */
    public TreeNode inorderSuccessor(TreeNode root, TreeNode p) {
        TreeNode successor = null;
        TreeNode current = root;
        
        // Search for node p, tracking potential successor
        while (current != null) {
            if (p.val < current.val) {
                // Going left: current is potential successor
                // (current.val > p.val, so current could be next)
                successor = current;
                current = current.left;
            } else {
                // Going right or at p: current is not successor
                // (current.val <= p.val)
                current = current.right;
            }
        }
        
        return successor;
    }
    
    /**
     * APPROACH 2: TWO-STEP EXPLICIT - MORE INTUITIVE
     * ===============================================
     * 
     * ALGORITHM:
     * ----------
     * Step 1: Find node p
     * Step 2: Determine successor based on two cases:
     *   Case A: p has right child
     *     → Find leftmost node in right subtree
     *   Case B: p has no right child
     *     → Find first ancestor where we went left
     * 
     * This is more explicit about the two cases, making it
     * easier to understand and explain in interviews.
     * 
     * TIME: O(H), SPACE: O(1)
     */
    public TreeNode inorderSuccessorExplicit(TreeNode root, TreeNode p) {
        // CASE A: Node has right subtree
        if (p.right != null) {
            return findLeftmost(p.right);
        }
        
        // CASE B: Node has no right subtree
        // Find first ancestor where we went left
        TreeNode successor = null;
        TreeNode current = root;
        
        while (current != null) {
            if (p.val < current.val) {
                successor = current;  // Potential successor
                current = current.left;
            } else {
                current = current.right;
            }
        }
        
        return successor;
    }
    
    // Helper: Find leftmost (minimum) node in subtree
    private TreeNode findLeftmost(TreeNode node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }
    
    /**
     * APPROACH 3: USING INORDER TRAVERSAL - BRUTE FORCE
     * ==================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Do inorder traversal, collect all nodes
     * 2. Find p in the list
     * 3. Return next node
     * 
     * This works but is inefficient - doesn't use BST properties!
     * 
     * TIME: O(N) - traverse entire tree
     * SPACE: O(N) - store all nodes
     * 
     * Included to show naive approach and why BST solution is better.
     */
    public TreeNode inorderSuccessorNaive(TreeNode root, TreeNode p) {
        List<TreeNode> inorder = new ArrayList<>();
        inorderTraversal(root, inorder);
        
        for (int i = 0; i < inorder.size() - 1; i++) {
            if (inorder.get(i) == p) {
                return inorder.get(i + 1);
            }
        }
        
        return null; // p is last element or not found
    }
    
    private void inorderTraversal(TreeNode node, List<TreeNode> result) {
        if (node == null) return;
        inorderTraversal(node.left, result);
        result.add(node);
        inorderTraversal(node.right, result);
    }
    
    /**
     * APPROACH 4: RECURSIVE WITH VALUE COMPARISON
     * ============================================
     * 
     * ALGORITHM:
     * ----------
     * Recursively search BST, tracking successor
     * 
     * TIME: O(H), SPACE: O(H) for recursion
     */
    public TreeNode inorderSuccessorRecursive(TreeNode root, TreeNode p) {
        if (root == null) return null;
        
        // If p.val >= root.val, successor must be in right subtree
        if (p.val >= root.val) {
            return inorderSuccessorRecursive(root.right, p);
        }
        
        // If p.val < root.val, root is potential successor
        // But there might be a closer one in left subtree
        TreeNode leftSuccessor = inorderSuccessorRecursive(root.left, p);
        return leftSuccessor != null ? leftSuccessor : root;
    }
}

/**
 * THE TWO CASES VISUALIZED
 * =========================
 * 
 * CASE A: Node has right subtree
 * -------------------------------
 *       p
 *      / \
 *     L   R
 *        /
 *       S  ← Successor (leftmost of right)
 * 
 * Example:
 *       3
 *      / \
 *     2   4  ← Successor of 3
 *        / \
 *       ?   5
 * 
 * Successor = leftmost of right subtree = 4
 * 
 * 
 * CASE B: Node has no right subtree
 * ----------------------------------
 *         G  ← First ancestor where we went left = Successor
 *        /
 *       P
 *      /
 *     p
 * 
 * Example:
 *       5  ← Successor of 4
 *      / \
 *     3   6
 *    / \
 *   2   4  ← p (no right child)
 * 
 * Path from root to 4: 5 (left) → 3 (right) → 4
 * Last node where we went left = 5
 * Successor = 5
 * 
 * 
 * CASE C: No successor exists
 * ---------------------------
 *       5
 *      / \
 *     3   6  ← p (largest element)
 * 
 * Path from root to 6: 5 (right) → 6
 * Never went left → no successor
 * Return null
 */

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach         | Time | Space | Notes
 * -----------------|------|-------|-------------------------
 * Iterative BST    | O(H) | O(1)  | OPTIMAL - Best solution
 * Explicit Cases   | O(H) | O(1)  | Clearer, same complexity
 * Inorder Traverse | O(N) | O(N)  | Naive, doesn't use BST
 * Recursive        | O(H) | O(H)  | Elegant but uses stack
 * 
 * RECOMMENDATION:
 * - Interview: Iterative BST (Approach 1) - optimal and clear
 * - If confused: Explicit Cases (Approach 2) - easier to explain
 * - Avoid: Naive inorder traversal - doesn't leverage BST
 */

/**
 * COMMON MISTAKES & EDGE CASES
 * =============================
 * 
 * MISTAKES:
 * 1. Forgetting the "no right subtree" case
 * 2. Not handling when p is the largest element (no successor)
 * 3. Only checking right subtree and missing ancestor case
 * 4. Using O(N) solution when O(H) is possible
 * 5. Assuming parent pointers exist (they don't in this problem)
 * 
 * EDGE CASES:
 * 1. p is the largest element → return null
 * 2. p is the smallest element → return next element
 * 3. p has right child → use leftmost of right
 * 4. p has no right child → use ancestor
 * 5. Single node tree → no successor
 * 6. p is root with no right child
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        System.out.println("=== Test Case 1: Standard BST ===");
        //       5
        //      / \
        //     3   6
        //    / \
        //   2   4
        //  /
        // 1
        TreeNode root1 = new TreeNode(5);
        root1.left = new TreeNode(3);
        root1.right = new TreeNode(6);
        root1.left.left = new TreeNode(2);
        root1.left.right = new TreeNode(4);
        root1.left.left.left = new TreeNode(1);
        
        System.out.println("Inorder: [1, 2, 3, 4, 5, 6]");
        
        TreeNode p1 = root1.left; // Node 3
        TreeNode succ1 = sol.inorderSuccessor(root1, p1);
        System.out.println("Successor of 3: " + (succ1 != null ? succ1.val : "null"));
        System.out.println("Expected: 4 (has right subtree)");
        
        TreeNode p2 = root1.left.right; // Node 4
        TreeNode succ2 = sol.inorderSuccessor(root1, p2);
        System.out.println("\nSuccessor of 4: " + (succ2 != null ? succ2.val : "null"));
        System.out.println("Expected: 5 (no right, ancestor)");
        
        TreeNode p3 = root1.right; // Node 6
        TreeNode succ3 = sol.inorderSuccessor(root1, p3);
        System.out.println("\nSuccessor of 6: " + (succ3 != null ? succ3.val : "null"));
        System.out.println("Expected: null (largest element)");
        
        TreeNode p4 = root1.left.left.left; // Node 1
        TreeNode succ4 = sol.inorderSuccessor(root1, p4);
        System.out.println("\nSuccessor of 1: " + (succ4 != null ? succ4.val : "null"));
        System.out.println("Expected: 2 (smallest element)");
        
        System.out.println("\n=== Test Case 2: Simple BST ===");
        //   2
        //  / \
        // 1   3
        TreeNode root2 = new TreeNode(2);
        root2.left = new TreeNode(1);
        root2.right = new TreeNode(3);
        
        TreeNode succ5 = sol.inorderSuccessor(root2, root2.left);
        System.out.println("Successor of 1: " + (succ5 != null ? succ5.val : "null"));
        System.out.println("Expected: 2");
        
        TreeNode succ6 = sol.inorderSuccessor(root2, root2);
        System.out.println("Successor of 2: " + (succ6 != null ? succ6.val : "null"));
        System.out.println("Expected: 3");
        
        System.out.println("\n=== Test Case 3: Right-Skewed BST ===");
        // 1
        //  \
        //   2
        //    \
        //     3
        TreeNode root3 = new TreeNode(1);
        root3.right = new TreeNode(2);
        root3.right.right = new TreeNode(3);
        
        TreeNode succ7 = sol.inorderSuccessor(root3, root3);
        System.out.println("Successor of 1: " + (succ7 != null ? succ7.val : "null"));
        System.out.println("Expected: 2");
        
        System.out.println("\n=== Verify All Approaches ===");
        TreeNode p = root1.left.right; // Node 4
        System.out.println("Testing successor of 4:");
        System.out.println("  Iterative:   " + sol.inorderSuccessor(root1, p).val);
        System.out.println("  Explicit:    " + sol.inorderSuccessorExplicit(root1, p).val);
        System.out.println("  Naive:       " + sol.inorderSuccessorNaive(root1, p).val);
        System.out.println("  Recursive:   " + sol.inorderSuccessorRecursive(root1, p).val);
        System.out.println("  All return:  5 ✓");
        
        System.out.println("\n=== Visual Walkthrough ===");
        System.out.println("BST:       5");
        System.out.println("          / \\");
        System.out.println("         3   6");
        System.out.println("        / \\");
        System.out.println("       2   4");
        System.out.println("      /");
        System.out.println("     1");
        System.out.println();
        System.out.println("Inorder: [1, 2, 3, 4, 5, 6]");
        System.out.println();
        System.out.println("Finding successor of 4:");
        System.out.println("  1. Start at 5: 4 < 5, successor = 5, go left");
        System.out.println("  2. At 3: 4 > 3, go right");
        System.out.println("  3. At 4: found target!");
        System.out.println("  4. No right subtree");
        System.out.println("  5. Return tracked successor = 5 ✓");
        System.out.println();
        System.out.println("Finding successor of 3:");
        System.out.println("  1. Start at 5: 3 < 5, successor = 5, go left");
        System.out.println("  2. At 3: found target!");
        System.out.println("  3. Has right subtree (4)");
        System.out.println("  4. Return leftmost of right = 4 ✓");
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "The inorder successor is the next element in sorted order,
 *    since inorder traversal of BST gives sorted sequence."
 * 
 * 2. "There are two cases: If the node has a right child, the
 *    successor is the leftmost node in the right subtree.
 *    Otherwise, it's the first ancestor where we went left."
 *    [Draw both cases on board]
 * 
 * 3. "I can solve both cases in one pass by tracking a potential
 *    successor while searching for p. Whenever I go left, the
 *    current node becomes a candidate successor."
 *    [Write iterative solution]
 * 
 * 4. "Time complexity is O(H) where H is height - we traverse
 *    one path from root. Space is O(1) with just a few pointers."
 * 
 * 5. "Edge cases: p is the largest element (return null), p is
 *    smallest (return next), and p is root."
 * 
 * 6. "A naive approach would be to do full inorder traversal and
 *    find next element, but that's O(N) time and space. Using
 *    BST properties, we get O(H) which is much better."
 * 
 * KEY POINTS TO EMPHASIZE:
 * - Inorder traversal gives sorted order
 * - Two distinct cases for successor
 * - Track successor while searching (elegant!)
 * - O(H) time, O(1) space is optimal
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - How would you find predecessor? [Mirror logic]
 * - What if you had parent pointers? [Easier, can go up]
 * - Can you do it recursively? [Yes, shown above]
 * - What about successor in BST II with parent? [Different problem]
 * - How to find kth successor? [Iterate k times]
 */

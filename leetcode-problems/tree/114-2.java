import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a TREE MANIPULATION problem testing:
 * 1. Understanding of preorder traversal
 * 2. In-place tree modification
 * 3. Pointer manipulation skills
 * 4. Multiple creative approaches with different trade-offs
 * 5. Space complexity optimization
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. PREORDER ORDER: ROOT → LEFT → RIGHT
 *    - Must preserve this order in flattened list
 *    - First element = root, then left subtree, then right subtree
 * 
 * 2. "LINKED LIST" FORMAT:
 *    - Use right pointer as "next"
 *    - All left pointers set to null
 *    - Still uses TreeNode class
 * 
 * 3. IN-PLACE MODIFICATION:
 *    - Modify existing nodes, don't create new ones
 *    - Original tree is destroyed
 *    - Challenge: Need right subtree but will overwrite right pointer!
 * 
 * 4. MULTIPLE APPROACHES:
 *    - Recursive: Elegant but needs return value or global variable
 *    - Iterative: More complex but clear step-by-step
 *    - Morris-like: O(1) space using threading
 * 
 * VISUALIZATION:
 * --------------
 * 
 * Input tree:
 *       1
 *      / \
 *     2   5
 *    / \   \
 *   3   4   6
 * 
 * Preorder: 1 → 2 → 3 → 4 → 5 → 6
 * 
 * Flattened (using right pointers):
 * 1 → 2 → 3 → 4 → 5 → 6
 * (all left pointers are null)
 * 
 * CHALLENGE: When processing node 1:
 * - Need to flatten left subtree (2,3,4)
 * - Need to flatten right subtree (5,6)
 * - Attach flattened left to 1's right
 * - Attach flattened right to end of flattened left
 * - Set 1's left to null
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Clarify requirements
 *    "So I need to modify in-place using right pointers as next,
 *     and the order should be preorder, correct?"
 * 
 * 2. Explain the challenge
 *    "The key challenge is that I need to save the right subtree
 *     before overwriting the right pointer."
 * 
 * 3. Start with recursive approach
 *    "I'll recursively flatten left and right, then connect them."
 * 
 * 4. Optimize if asked
 *    "For O(1) space, I can use a Morris-like approach with threading."
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

class Solution {
    
    /**
     * APPROACH 1: RECURSIVE WITH RETURN VALUE - MOST ELEGANT
     * =======================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Recursively flatten left subtree
     * 2. Recursively flatten right subtree
     * 3. Connect: root → flattened_left → flattened_right
     * 4. Return tail of the list (for parent to connect)
     * 
     * KEY INSIGHT: Return the TAIL so parent knows where to attach next part!
     * 
     * DETAILED STEPS:
     * ---------------
     * For each node:
     * 1. Save right subtree (will be overwritten)
     * 2. Flatten left subtree, get its tail
     * 3. Move flattened left to right
     * 4. Set left to null
     * 5. Connect tail to saved right subtree
     * 6. Flatten right subtree, get its tail
     * 7. Return final tail
     * 
     * EXAMPLE:
     *       1
     *      / \
     *     2   5
     *    / \   \
     *   3   4   6
     * 
     * Process node 1:
     *   - Flatten left (2,3,4): returns node 4 as tail
     *   - Flatten right (5,6): returns node 6 as tail
     *   - Result: 1→2→3→4→5→6, return 6
     * 
     * Process node 2:
     *   - Flatten left (3): returns node 3 as tail
     *   - Flatten right (4): returns node 4 as tail
     *   - Result: 2→3→4, return 4
     * 
     * TIME COMPLEXITY: O(N)
     * - Visit each node exactly once
     * 
     * SPACE COMPLEXITY: O(H)
     * - Recursion stack depth = height
     * - Balanced: O(log N)
     * - Skewed: O(N)
     */
    public void flatten(TreeNode root) {
        flattenHelper(root);
    }
    
    /**
     * Flattens tree rooted at node
     * Returns the tail of the flattened list (rightmost node)
     */
    private TreeNode flattenHelper(TreeNode node) {
        if (node == null) return null;
        
        // Base case: leaf node
        if (node.left == null && node.right == null) {
            return node;
        }
        
        // Save right subtree before overwriting
        TreeNode rightSubtree = node.right;
        
        // Flatten left subtree
        TreeNode leftTail = flattenHelper(node.left);
        
        // Move flattened left subtree to right
        if (node.left != null) {
            node.right = node.left;
            node.left = null;
            
            // Connect tail of left subtree to right subtree
            leftTail.right = rightSubtree;
        }
        
        // Flatten right subtree and return its tail
        TreeNode rightTail = flattenHelper(rightSubtree);
        
        // Return the rightmost node (tail of entire flattened list)
        return rightTail != null ? rightTail : leftTail;
    }
    
    /**
     * APPROACH 2: ITERATIVE WITH STACK - SIMULATING PREORDER
     * =======================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Use stack for preorder traversal
     * 2. Keep track of previous node
     * 3. For each node, set previous.right = current
     * 4. Set previous.left = null
     * 5. Push right child first, then left (for correct preorder)
     * 
     * ADVANTAGE: Clear step-by-step process
     * 
     * EXAMPLE:
     *       1
     *      / \
     *     2   5
     *    / \   \
     *   3   4   6
     * 
     * Stack operations:
     * Initial: stack = [1]
     * Pop 1, push 5, 2: stack = [5, 2], connect prev→1
     * Pop 2, push 4, 3: stack = [5, 4, 3], connect 1→2
     * Pop 3: stack = [5, 4], connect 2→3
     * Pop 4: stack = [5], connect 3→4
     * Pop 5, push 6: stack = [6], connect 4→5
     * Pop 6: stack = [], connect 5→6
     * 
     * TIME COMPLEXITY: O(N)
     * SPACE COMPLEXITY: O(H) for stack
     */
    public void flattenIterative(TreeNode root) {
        if (root == null) return;
        
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        TreeNode prev = null;
        
        while (!stack.isEmpty()) {
            TreeNode current = stack.pop();
            
            // Connect previous node to current
            if (prev != null) {
                prev.left = null;
                prev.right = current;
            }
            
            // Push right first (so left is processed first - preorder)
            if (current.right != null) {
                stack.push(current.right);
            }
            if (current.left != null) {
                stack.push(current.left);
            }
            
            prev = current;
        }
    }
    
    /**
     * APPROACH 3: MORRIS-LIKE THREADING - O(1) SPACE
     * ===============================================
     * 
     * ALGORITHM:
     * ----------
     * 1. For each node with left subtree:
     *    - Find rightmost node of left subtree
     *    - Attach right subtree to rightmost of left
     *    - Move left subtree to right
     *    - Set left to null
     * 2. Move to right (next in preorder)
     * 
     * KEY INSIGHT: We can use the tree structure itself to store
     * the threading information, avoiding extra space!
     * 
     * VISUALIZATION:
     *       1              1
     *      / \      →       \
     *     2   5              2
     *    / \   \            / \
     *   3   4   6          3   4
     *                           \
     *                            5
     *                             \
     *                              6
     * 
     * Step by step:
     * 1. At node 1:
     *    - Find rightmost of left (2,3,4) = node 4
     *    - Attach right (5,6) to node 4
     *    - Move left to right
     *    Result: 1→2→3→4→5→6
     * 
     * TIME COMPLEXITY: O(N)
     * - Each node visited at most twice
     * - Finding rightmost: amortized O(1) per node
     * 
     * SPACE COMPLEXITY: O(1)
     * - No recursion, no stack
     * - Only a few pointers!
     * 
     * THIS IS THE OPTIMAL SOLUTION!
     */
    public void flattenMorris(TreeNode root) {
        TreeNode current = root;
        
        while (current != null) {
            // If left subtree exists
            if (current.left != null) {
                // Find the rightmost node of left subtree
                TreeNode rightmost = current.left;
                while (rightmost.right != null) {
                    rightmost = rightmost.right;
                }
                
                // Attach current's right subtree to rightmost of left
                rightmost.right = current.right;
                
                // Move left subtree to right
                current.right = current.left;
                current.left = null;
            }
            
            // Move to next node (now in right)
            current = current.right;
        }
    }
    
    /**
     * APPROACH 4: REVERSE POSTORDER - CLEVER TRICK
     * =============================================
     * 
     * ALGORITHM:
     * ----------
     * Visit in REVERSE preorder (RIGHT → LEFT → ROOT)
     * Keep track of previously processed node
     * For each node, set right to prev, left to null
     * 
     * WHY THIS WORKS:
     * Reverse of preorder [1,2,3,4,5,6] is [6,5,4,3,2,1]
     * Processing in reverse builds list from tail to head!
     * 
     * EXAMPLE:
     * Process 6: prev = null, 6.right = null
     * Process 5: prev = 6, 5.right = 6
     * Process 4: prev = 5, 4.right = 5
     * Process 3: prev = 4, 3.right = 4
     * Process 2: prev = 3, 2.right = 3
     * Process 1: prev = 2, 1.right = 2
     * 
     * Result: 1→2→3→4→5→6 ✓
     * 
     * TIME: O(N), SPACE: O(H) for recursion
     */
    private TreeNode prev = null;
    
    public void flattenReversePostorder(TreeNode root) {
        if (root == null) return;
        
        // Process in reverse: RIGHT → LEFT → ROOT
        flattenReversePostorder(root.right);
        flattenReversePostorder(root.left);
        
        // Connect current to previously processed
        root.right = prev;
        root.left = null;
        prev = root;
    }
}

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach            | Time | Space | Difficulty | Best For
 * --------------------|------|-------|------------|------------------
 * Recursive w/ Return | O(N) | O(H)  | Medium     | Clarity, interviews
 * Iterative + Stack   | O(N) | O(H)  | Easy       | Step-by-step
 * Morris Threading    | O(N) | O(1)  | Hard       | Space optimization
 * Reverse Postorder   | O(N) | O(H)  | Medium     | Clever trick
 * 
 * RECOMMENDATION:
 * - Interview: Recursive or Iterative (easy to explain)
 * - Production: Morris if space matters, otherwise Recursive
 * - Show-off: Reverse Postorder (creative)
 */

/**
 * VISUAL STEP-BY-STEP (MORRIS APPROACH)
 * ======================================
 * 
 * Original:
 *       1
 *      / \
 *     2   5
 *    / \   \
 *   3   4   6
 * 
 * Step 1: At node 1
 *   Find rightmost of left (2): node 4
 *   Attach 5,6 to node 4
 *   Move 2 to right of 1
 *       1
 *        \
 *         2
 *        / \
 *       3   4
 *            \
 *             5
 *              \
 *               6
 * 
 * Step 2: At node 2
 *   Find rightmost of left (3): node 3
 *   Attach 4,5,6 to node 3
 *   Move 3 to right of 2
 *       1
 *        \
 *         2
 *          \
 *           3
 *            \
 *             4
 *              \
 *               5
 *                \
 *                 6
 * 
 * Steps 3-6: Process 3,4,5,6 (all have no left child)
 * Final: 1→2→3→4→5→6 ✓
 */

/**
 * COMMON MISTAKES & EDGE CASES
 * =============================
 * 
 * MISTAKES:
 * 1. Forgetting to save right subtree before overwriting
 * 2. Not setting left pointers to null
 * 3. Losing track of the tail when connecting subtrees
 * 4. Infinite loop when using Morris (wrong threading)
 * 5. Off-by-one errors when finding rightmost
 * 
 * EDGE CASES:
 * 1. Null tree
 * 2. Single node
 * 3. Left-skewed tree (all left children)
 * 4. Right-skewed tree (all right children)
 * 5. Complete binary tree
 * 6. Already flattened tree
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        System.out.println("=== Test Case 1: Normal Tree ===");
        //       1
        //      / \
        //     2   5
        //    / \   \
        //   3   4   6
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(5);
        root1.left.left = new TreeNode(3);
        root1.left.right = new TreeNode(4);
        root1.right.right = new TreeNode(6);
        
        System.out.println("Before: " + treeToString(root1));
        sol.flatten(root1);
        System.out.println("After:  " + listToString(root1));
        System.out.println("Expected: 1->2->3->4->5->6");
        
        System.out.println("\n=== Test Case 2: Using Morris (O(1) space) ===");
        TreeNode root2 = buildTree1();
        sol.flattenMorris(root2);
        System.out.println("Result: " + listToString(root2));
        System.out.println("Expected: 1->2->3->4->5->6");
        
        System.out.println("\n=== Test Case 3: Null Tree ===");
        TreeNode root3 = null;
        sol.flatten(root3);
        System.out.println("Result: " + (root3 == null ? "null" : "not null"));
        System.out.println("Expected: null");
        
        System.out.println("\n=== Test Case 4: Single Node ===");
        TreeNode root4 = new TreeNode(1);
        sol.flatten(root4);
        System.out.println("Result: " + listToString(root4));
        System.out.println("Expected: 1");
        
        System.out.println("\n=== Test Case 5: Left-Skewed ===");
        //     1
        //    /
        //   2
        //  /
        // 3
        TreeNode root5 = new TreeNode(1);
        root5.left = new TreeNode(2);
        root5.left.left = new TreeNode(3);
        sol.flatten(root5);
        System.out.println("Result: " + listToString(root5));
        System.out.println("Expected: 1->2->3");
        
        System.out.println("\n=== Test Case 6: Right-Skewed (Already Flat) ===");
        // 1
        //  \
        //   2
        //    \
        //     3
        TreeNode root6 = new TreeNode(1);
        root6.right = new TreeNode(2);
        root6.right.right = new TreeNode(3);
        sol.flatten(root6);
        System.out.println("Result: " + listToString(root6));
        System.out.println("Expected: 1->2->3");
        
        System.out.println("\n=== Test Case 7: Complex Tree ===");
        //         1
        //        / \
        //       2   5
        //      /   / \
        //     3   6   7
        //    /
        //   4
        TreeNode root7 = new TreeNode(1);
        root7.left = new TreeNode(2);
        root7.right = new TreeNode(5);
        root7.left.left = new TreeNode(3);
        root7.left.left.left = new TreeNode(4);
        root7.right.left = new TreeNode(6);
        root7.right.right = new TreeNode(7);
        System.out.println("Preorder: 1,2,3,4,5,6,7");
        sol.flatten(root7);
        System.out.println("Result:   " + listToString(root7));
        System.out.println("Expected: 1->2->3->4->5->6->7");
        
        System.out.println("\n=== Verify All Approaches Give Same Result ===");
        TreeNode t1 = buildTree1(); sol.flatten(t1);
        TreeNode t2 = buildTree1(); sol.flattenIterative(t2);
        TreeNode t3 = buildTree1(); sol.flattenMorris(t3);
        
        String r1 = listToString(t1);
        String r2 = listToString(t2);
        String r3 = listToString(t3);
        
        System.out.println("Recursive:  " + r1);
        System.out.println("Iterative:  " + r2);
        System.out.println("Morris:     " + r3);
        System.out.println("All match:  " + (r1.equals(r2) && r2.equals(r3)));
        
        System.out.println("\n=== Visual Demonstration ===");
        System.out.println("Original tree:");
        System.out.println("      1");
        System.out.println("     / \\");
        System.out.println("    2   5");
        System.out.println("   / \\   \\");
        System.out.println("  3   4   6");
        System.out.println();
        System.out.println("Preorder traversal: 1 -> 2 -> 3 -> 4 -> 5 -> 6");
        System.out.println();
        System.out.println("Flattened (using right pointers):");
        System.out.println("1 -> 2 -> 3 -> 4 -> 5 -> 6 -> null");
        System.out.println("(all left pointers are null)");
    }
    
    // Helper to build standard test tree
    private static TreeNode buildTree1() {
        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(5);
        root.left.left = new TreeNode(3);
        root.left.right = new TreeNode(4);
        root.right.right = new TreeNode(6);
        return root;
    }
    
    // Helper to convert tree to string (preorder)
    private static String treeToString(TreeNode root) {
        if (root == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append(root.val);
        if (root.left != null || root.right != null) {
            sb.append("(").append(treeToString(root.left)).append(",");
            sb.append(treeToString(root.right)).append(")");
        }
        return sb.toString();
    }
    
    // Helper to convert flattened list to string
    private static String listToString(TreeNode head) {
        StringBuilder sb = new StringBuilder();
        TreeNode current = head;
        while (current != null) {
            sb.append(current.val);
            if (current.right != null) sb.append("->");
            
            // Verify left is null
            if (current.left != null) {
                sb.append("[ERROR: left not null!]");
            }
            
            current = current.right;
        }
        return sb.toString();
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "The result should be in preorder (ROOT→LEFT→RIGHT), using
 *    right pointers as 'next' and all left pointers set to null."
 * 
 * 2. "The main challenge is that I need to save the right subtree
 *    before overwriting the right pointer when moving the left
 *    subtree over."
 *    [Draw diagram on board]
 * 
 * 3. "I'll use a recursive approach: flatten left, flatten right,
 *    then connect root → left_tail → right. I return the tail so
 *    the parent knows where to attach the next part."
 *    [Walk through example]
 * 
 * 4. "Time is O(N) as I visit each node once. Space is O(H) for
 *    the recursion stack."
 * 
 * 5. "For O(1) space, there's a Morris-like approach where I find
 *    the rightmost of the left subtree, attach the right subtree
 *    there, then move left to right."
 *    [Show if time permits]
 * 
 * 6. "Edge cases: null tree, single node, already flattened tree,
 *    and ensuring all left pointers are null."
 * 
 * KEY POINTS TO EMPHASIZE:
 * - Preorder traversal order
 * - In-place modification
 * - Saving right subtree before overwriting
 * - Connecting parts in correct order
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Can you do it iteratively? [Yes, with stack]
 * - Can you optimize space to O(1)? [Morris approach]
 * - What if we wanted postorder? [Similar but different connection]
 * - How would you restore the original tree? [Need to save structure]
 * - Can you do it without recursion? [Yes, iterative or Morris]
 * - What if values can be modified? [Same approach, values don't matter]
 */


class FlattenBinaryTree {
    
    // Approach 1: Using Preorder Traversal List (Intuitive)
    // Time: O(n), Space: O(n)
    public void flattenWithList(TreeNode root) {
        if (root == null) return;
        
        // Collect nodes in preorder
        List<TreeNode> nodes = new ArrayList<>();
        preorder(root, nodes);
        
        // Link them together
        for (int i = 0; i < nodes.size() - 1; i++) {
            nodes.get(i).left = null;
            nodes.get(i).right = nodes.get(i + 1);
        }
        nodes.get(nodes.size() - 1).left = null;
        nodes.get(nodes.size() - 1).right = null;
    }
    
    private void preorder(TreeNode node, List<TreeNode> nodes) {
        if (node == null) return;
        nodes.add(node);
        preorder(node.left, nodes);
        preorder(node.right, nodes);
    }
    
    // Approach 2: Recursive with Global Pointer
    // Time: O(n), Space: O(h)
    private TreeNode prev = null;
    
    public void flattenRecursiveGlobal(TreeNode root) {
        if (root == null) return;
        
        // Process in REVERSE preorder (right, left, root)
        flattenRecursiveGlobal(root.right);
        flattenRecursiveGlobal(root.left);
        
        // Link current node
        root.right = prev;
        root.left = null;
        prev = root;
    }
    
    // Approach 3: Recursive Returning Tail (Clean)
    // Time: O(n), Space: O(h)
    public void flatten(TreeNode root) {
        flattenHelper(root);
    }
    
    private TreeNode flattenHelper(TreeNode node) {
        if (node == null) return null;
        
        // Flatten left and right subtrees
        TreeNode leftTail = flattenHelper(node.left);
        TreeNode rightTail = flattenHelper(node.right);
        
        // If left subtree exists, insert it between node and right
        if (leftTail != null) {
            leftTail.right = node.right;
            node.right = node.left;
            node.left = null;
        }
        
        // Return the tail of the flattened tree
        if (rightTail != null) return rightTail;
        if (leftTail != null) return leftTail;
        return node;
    }
    
    // Approach 4: Iterative with Stack (Preorder)
    // Time: O(n), Space: O(h)
    public void flattenIterativeStack(TreeNode root) {
        if (root == null) return;
        
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        TreeNode prev = null;
        
        while (!stack.isEmpty()) {
            TreeNode curr = stack.pop();
            
            // Link previous node to current
            if (prev != null) {
                prev.right = curr;
                prev.left = null;
            }
            
            // Push right first (stack is LIFO, we want left first)
            if (curr.right != null) stack.push(curr.right);
            if (curr.left != null) stack.push(curr.left);
            
            prev = curr;
        }
    }
    
    // Approach 5: Morris Traversal (OPTIMAL - O(1) space)
    // Time: O(n), Space: O(1)
    public void flattenMorris(TreeNode root) {
        TreeNode curr = root;
        
        while (curr != null) {
            if (curr.left != null) {
                // Find the rightmost node in left subtree
                TreeNode rightmost = curr.left;
                while (rightmost.right != null) {
                    rightmost = rightmost.right;
                }
                
                // Connect rightmost to current's right
                rightmost.right = curr.right;
                
                // Move left subtree to right
                curr.right = curr.left;
                curr.left = null;
            }
            
            // Move to next node
            curr = curr.right;
        }
    }
    
    // Approach 6: Iterative In-Place (No Extra Space)
    // Time: O(n), Space: O(1)
    public void flattenInPlace(TreeNode root) {
        TreeNode curr = root;
        
        while (curr != null) {
            if (curr.left != null) {
                // Find predecessor (rightmost in left subtree)
                TreeNode predecessor = curr.left;
                while (predecessor.right != null) {
                    predecessor = predecessor.right;
                }
                
                // Rewire
                predecessor.right = curr.right;
                curr.right = curr.left;
                curr.left = null;
            }
            curr = curr.right;
        }
    }
    
    // Approach 7: Divide and Conquer
    // Time: O(n), Space: O(h)
    public void flattenDivideConquer(TreeNode root) {
        flattenAndReturnTail(root);
    }
    
    private TreeNode flattenAndReturnTail(TreeNode node) {
        if (node == null) return null;
        if (node.left == null && node.right == null) return node;
        
        TreeNode leftTail = flattenAndReturnTail(node.left);
        TreeNode rightTail = flattenAndReturnTail(node.right);
        
        if (node.left != null) {
            TreeNode temp = node.right;
            node.right = node.left;
            node.left = null;
            leftTail.right = temp;
        }
        
        return rightTail != null ? rightTail : leftTail;
    }
    
    // Helper: Build tree from array
    public static TreeNode buildTree(Integer[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null) return null;
        
        TreeNode root = new TreeNode(arr[0]);
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        int i = 1;
        while (!queue.isEmpty() && i < arr.length) {
            TreeNode node = queue.poll();
            
            if (i < arr.length && arr[i] != null) {
                node.left = new TreeNode(arr[i]);
                queue.offer(node.left);
            }
            i++;
            
            if (i < arr.length && arr[i] != null) {
                node.right = new TreeNode(arr[i]);
                queue.offer(node.right);
            }
            i++;
        }
        
        return root;
    }
    
    // Helper: Visualize tree structure
    private static void visualizeTree(TreeNode root, String label) {
        System.out.println("\n" + label);
        if (root == null) {
            System.out.println("null");
            return;
        }
        
        List<List<String>> levels = new ArrayList<>();
        buildLevels(root, 0, levels);
        
        for (int i = 0; i < levels.size(); i++) {
            System.out.println("Level " + i + ": " + String.join(" ", levels.get(i)));
        }
    }
    
    private static void buildLevels(TreeNode node, int level, List<List<String>> levels) {
        if (node == null) return;
        
        if (levels.size() == level) {
            levels.add(new ArrayList<>());
        }
        
        String nodeStr = node.val + " (L:" + (node.left != null ? node.left.val : "null") + 
                         " R:" + (node.right != null ? node.right.val : "null") + ")";
        levels.get(level).add(nodeStr);
        
        buildLevels(node.left, level + 1, levels);
        buildLevels(node.right, level + 1, levels);
    }
    
    // Helper: Print flattened list
    private static void printFlattened(TreeNode root) {
        System.out.print("Flattened: ");
        TreeNode curr = root;
        while (curr != null) {
            System.out.print(curr.val);
            if (curr.right != null) System.out.print(" -> ");
            if (curr.left != null) {
                System.out.print(" [ERROR: left not null!]");
            }
            curr = curr.right;
        }
        System.out.println();
    }
    
    // Helper: Get preorder sequence
    private static List<Integer> getPreorder(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorderList(root, result);
        return result;
    }
    
    private static void preorderList(TreeNode node, List<Integer> result) {
        if (node == null) return;
        result.add(node.val);
        preorderList(node.left, result);
        preorderList(node.right, result);
    }
    
    // Helper: Clone tree
    private static TreeNode cloneTree(TreeNode root) {
        if (root == null) return null;
        TreeNode clone = new TreeNode(root.val);
        clone.left = cloneTree(root.left);
        clone.right = cloneTree(root.right);
        return clone;
    }
    
    // Test cases
    public static void main(String[] args) {
        FlattenBinaryTree solver = new FlattenBinaryTree();
        
        // Test Case 1: Standard tree
        System.out.println("=".repeat(70));
        System.out.println("TEST CASE 1: Standard Tree");
        System.out.println("=".repeat(70));
        
        Integer[] arr1 = {1, 2, 5, 3, 4, null, 6};
        TreeNode root1 = buildTree(arr1);
        
        visualizeTree(root1, "Original Tree:");
        List<Integer> expectedPreorder = getPreorder(root1);
        System.out.println("Expected preorder: " + expectedPreorder);
        
        TreeNode clone1 = cloneTree(root1);
        solver.flatten(clone1);
        printFlattened(clone1);
        
        // Test Case 2: Left skewed
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE 2: Left Skewed Tree");
        System.out.println("=".repeat(70));
        
        Integer[] arr2 = {1, 2, null, 3, null, 4};
        TreeNode root2 = buildTree(arr2);
        
        visualizeTree(root2, "Original Tree:");
        TreeNode clone2 = cloneTree(root2);
        solver.flatten(clone2);
        printFlattened(clone2);
        
        // Test Case 3: Right skewed
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE 3: Right Skewed Tree");
        System.out.println("=".repeat(70));
        
        Integer[] arr3 = {1, null, 2, null, 3};
        TreeNode root3 = buildTree(arr3);
        
        visualizeTree(root3, "Original Tree:");
        TreeNode clone3 = cloneTree(root3);
        solver.flatten(clone3);
        printFlattened(clone3);
        
        // Test Case 4: Single node
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE 4: Single Node");
        System.out.println("=".repeat(70));
        
        TreeNode root4 = new TreeNode(1);
        solver.flatten(root4);
        printFlattened(root4);
        
        // Step-by-step visualization
        System.out.println("\n" + "=".repeat(70));
        System.out.println("STEP-BY-STEP: Morris Traversal Approach");
        System.out.println("=".repeat(70));
        stepByStepMorris(buildTree(arr1));
        
        // Compare all approaches
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPARING ALL APPROACHES");
        System.out.println("=".repeat(70));
        compareApproaches(buildTree(arr1));
        
        // Algorithm comparison
        System.out.println("\n" + "=".repeat(70));
        System.out.println("ALGORITHM COMPARISON");
        System.out.println("=".repeat(70));
        compareAlgorithms();
        
        // Detailed explanation
        System.out.println("\n" + "=".repeat(70));
        System.out.println("DETAILED EXPLANATION");
        System.out.println("=".repeat(70));
        explainApproaches();
    }
    
    private static void stepByStepMorris(TreeNode root) {
        System.out.println("Original tree preorder: " + getPreorder(root));
        System.out.println("\nProcessing steps:\n");
        
        TreeNode curr = root;
        int step = 1;
        
        while (curr != null) {
            System.out.println("Step " + step + ": Current = " + curr.val);
            
            if (curr.left != null) {
                // Find rightmost in left subtree
                TreeNode rightmost = curr.left;
                System.out.print("  Left subtree exists. Finding rightmost: " + curr.left.val);
                
                while (rightmost.right != null) {
                    rightmost = rightmost.right;
                    System.out.print(" -> " + rightmost.val);
                }
                System.out.println();
                
                System.out.println("  Connect rightmost (" + rightmost.val + 
                                 ") to current's right (" + 
                                 (curr.right != null ? curr.right.val : "null") + ")");
                
                rightmost.right = curr.right;
                curr.right = curr.left;
                curr.left = null;
                
                System.out.println("  Move left subtree to right, set left = null");
            } else {
                System.out.println("  No left subtree, move to next");
            }
            
            curr = curr.right;
            step++;
            System.out.println();
        }
        
        System.out.println("Flattening complete!");
    }
    
    private static void compareApproaches(TreeNode original) {
        FlattenBinaryTree solver = new FlattenBinaryTree();
        
        System.out.println("\nOriginal preorder: " + getPreorder(original));
        
        // Test each approach
        TreeNode clone1 = cloneTree(original);
        solver.flattenWithList(clone1);
        System.out.println("\n1. With List:        " + getFlattenedList(clone1));
        
        solver.prev = null;
        TreeNode clone2 = cloneTree(original);
        solver.flattenRecursiveGlobal(clone2);
        System.out.println("2. Recursive Global: " + getFlattenedList(clone2));
        
        TreeNode clone3 = cloneTree(original);
        solver.flatten(clone3);
        System.out.println("3. Recursive Clean:  " + getFlattenedList(clone3));
        
        TreeNode clone4 = cloneTree(original);
        solver.flattenIterativeStack(clone4);
        System.out.println("4. Iterative Stack:  " + getFlattenedList(clone4));
        
        TreeNode clone5 = cloneTree(original);
        solver.flattenMorris(clone5);
        System.out.println("5. Morris Traversal: " + getFlattenedList(clone5));
        
        TreeNode clone6 = cloneTree(original);
        solver.flattenInPlace(clone6);
        System.out.println("6. In-Place:         " + getFlattenedList(clone6));
        
        TreeNode clone7 = cloneTree(original);
        solver.flattenDivideConquer(clone7);
        System.out.println("7. Divide & Conquer: " + getFlattenedList(clone7));
    }
    
    private static List<Integer> getFlattenedList(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        TreeNode curr = root;
        while (curr != null) {
            result.add(curr.val);
            curr = curr.right;
        }
        return result;
    }
    
    private static void compareAlgorithms() {
        System.out.println("\n┌──────────────────────┬──────────────┬──────────────┬─────────────────┐");
        System.out.println("│ Approach             │ Time         │ Space        │ Notes           │");
        System.out.println("├──────────────────────┼──────────────┼──────────────┼─────────────────┤");
        System.out.println("│ With List            │ O(n)         │ O(n)         │ Simple          │");
        System.out.println("│ Recursive Global     │ O(n)         │ O(h)         │ Reverse preorder│");
        System.out.println("│ Recursive Clean      │ O(n)         │ O(h)         │ Returns tail    │");
        System.out.println("│ Iterative Stack      │ O(n)         │ O(h)         │ Explicit stack  │");
        System.out.println("│ Morris Traversal     │ O(n)         │ O(1)         │ OPTIMAL         │");
        System.out.println("│ In-Place             │ O(n)         │ O(1)         │ Same as Morris  │");
        System.out.println("│ Divide & Conquer     │ O(n)         │ O(h)         │ Clean recursion │");
        System.out.println("└──────────────────────┴──────────────┴──────────────┴─────────────────┘");
        
        System.out.println("\nBest: Morris Traversal (O(1) space)");
        System.out.println("Most Intuitive: Recursive with tail return");
    }
    
    private static void explainApproaches() {
        System.out.println("\n1. WITH LIST (Intuitive)");
        System.out.println("   - Collect all nodes in preorder");
        System.out.println("   - Link them sequentially");
        System.out.println("   - Simple but uses O(n) extra space");
        
        System.out.println("\n2. RECURSIVE GLOBAL (Clever)");
        System.out.println("   - Process in REVERSE preorder (right, left, root)");
        System.out.println("   - Maintain global prev pointer");
        System.out.println("   - Link each node to prev");
        System.out.println("   - Why reverse? So we process leaves first!");
        
        System.out.println("\n3. RECURSIVE CLEAN (Recommended)");
        System.out.println("   - Flatten left and right subtrees");
        System.out.println("   - Insert left subtree between node and right");
        System.out.println("   - Return tail for linking");
        System.out.println("   - Clean and easy to understand");
        
        System.out.println("\n4. ITERATIVE STACK");
        System.out.println("   - Simulate preorder with stack");
        System.out.println("   - Push right before left (LIFO)");
        System.out.println("   - Link nodes as we visit them");
        
        System.out.println("\n5. MORRIS TRAVERSAL (OPTIMAL)");
        System.out.println("   - For each node with left child:");
        System.out.println("     1. Find rightmost in left subtree");
        System.out.println("     2. Connect it to current's right");
        System.out.println("     3. Move left subtree to right");
        System.out.println("     4. Set left to null");
        System.out.println("   - O(1) space! No recursion, no stack");
        
        System.out.println("\n6. DIVIDE & CONQUER");
        System.out.println("   - Flatten left and right recursively");
        System.out.println("   - Combine: root -> flattened_left -> flattened_right");
        System.out.println("   - Return tail for parent to use");
        
        System.out.println("\n\nKEY INSIGHT:");
        System.out.println("The challenge is to maintain preorder while");
        System.out.println("restructuring the tree in place.");
        System.out.println("\nMorris achieves O(1) space by temporarily");
        System.out.println("using the tree structure itself for threading!");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Flatten Binary Tree to Linked List

Convert tree to linked list using right pointers only.
Order: Preorder (root, left, right)

EXAMPLE:

Tree:      1
         /   \
        2     5
       / \     \
      3   4     6

Preorder: 1, 2, 3, 4, 5, 6

Flattened:  1 -> 2 -> 3 -> 4 -> 5 -> 6
            (all right pointers, left = null)

APPROACH 1: WITH LIST
=====================

Simple two-pass solution:
1. Collect nodes in preorder
2. Link them together

Time: O(n), Space: O(n)

APPROACH 2: RECURSIVE WITH GLOBAL POINTER
==========================================

Key insight: Process in REVERSE preorder!

Why? Consider: 1 -> 2 -> 3 -> 4 -> 5 -> 6

If we process forward (1, 2, 3...):
- Hard to maintain next pointer

If we process backward (6, 5, 4...):
- Easy! Each node points to prev

Algorithm:
```java
prev = null

process(node):
    process(right)  // Process right first
    process(left)   // Then left
    // Now process current
    node.right = prev
    node.left = null
    prev = node
```

For tree [1,2,3,4,5,6]:

Process 6: prev = null → 6.right = null, prev = 6
Process 5: prev = 6 → 5.right = 6, prev = 5
Process 4: prev = 5 → 4.right = 5, prev = 4
Process 3: prev = 4 → 3.right = 4, prev = 3
Process 2: prev = 3 → 2.right = 3, prev = 2
Process 1: prev = 2 → 1.right = 2, prev = 1

Result: 1->2->3->4->5->6 ✓

APPROACH 3: RECURSIVE RETURNING TAIL
=====================================

More intuitive recursion:

Algorithm:
```java
flattenHelper(node):
    if node == null: return null
    
    leftTail = flattenHelper(left)
    rightTail = flattenHelper(right)
    
    if leftTail exists:
        // Insert left subtree between node and right
        leftTail.right = node.right
        node.right = node.left
        node.left = null
    
    return rightTail or leftTail or node
```

Example: Node 1 with left=2, right=5

1. Flatten left (2): returns tail of [2,3,4] = 4
2. Flatten right (5): returns tail of [5,6] = 6
3. Insert left between 1 and 5:
   - 4.right = 5
   - 1.right = 2
   - 1.left = null
4. Return 6 (right tail)

APPROACH 4: ITERATIVE WITH STACK
=================================

Simulate preorder with explicit stack:

```java
stack.push(root)
while stack not empty:
    curr = stack.pop()
    
    if prev: prev.right = curr
    
    if curr.right: stack.push(curr.right)
    if curr.left: stack.push(curr.left)
    
    prev = curr
```

Push right before left (LIFO order).

APPROACH 5: MORRIS TRAVERSAL (OPTIMAL)
=======================================

O(1) space solution!

Key insight: Use tree structure itself.

Algorithm:
```java
curr = root
while curr:
    if curr.left exists:
        // Find rightmost in left subtree
        rightmost = curr.left
        while rightmost.right:
            rightmost = rightmost.right
        
        // Connect rightmost to curr's right
        rightmost.right = curr.right
        
        // Move left to right
        curr.right = curr.left
        curr.left = null
    
    curr = curr.right
```

DETAILED TRACE:

Tree:      1
         /   \
        2     5
       / \     \
      3   4     6

Step 1: curr = 1, has left (2)
  Rightmost in left = 4
  4.right = 5
  1.right = 2
  1.left = null
  
  State:  1
           \
            2
           / \
          3   4
               \
                5
                 \
                  6

Step 2: curr = 2, has left (3)
  Rightmost in left = 3
  3.right = 4
  2.right = 3
  2.left = null
  
  State:  1 -> 2 -> 3 -> 4 -> 5 -> 6

Continue for remaining nodes...

WHY THIS WORKS:

By connecting rightmost of left subtree to right subtree,
we're essentially "threading" the tree to maintain preorder!

COMPLEXITY ANALYSIS:

Morris Traversal:
Time: O(n)
  - Each node visited at most twice
  - Finding rightmost: amortized O(1)
Space: O(1)
  - No recursion, no stack!

Other approaches:
Time: O(n)
Space: O(h) for recursion/stack or O(n) for list

EDGE CASES:

1. Null root: return immediately
2. Single node: already flattened
3. Left-skewed: works fine
4. Right-skewed: already in correct form
5. Complete tree: all approaches work

KEY OBSERVATIONS:

1. PREORDER PRESERVATION
   - Must maintain root -> left -> right order
   - All left subtrees move to right

2. IN-PLACE REQUIREMENT
   - Modify existing nodes
   - Don't create new nodes

3. THREADING TECHNIQUE
   - Morris uses temporary links
   - These become permanent structure

INTERVIEW STRATEGY:

1. Start with simple list approach
2. Optimize to recursive with tail
3. Explain Morris for O(1) space
4. Walk through example carefully
5. Discuss all edge cases

COMMON MISTAKES:

1. Losing reference to right subtree
2. Not setting left to null
3. Wrong order in processing
4. Infinite loops in Morris
5. Not handling null properly

RELATED PROBLEMS:

1. Binary Tree Preorder Traversal
2. Morris Traversal
3. Serialize and Deserialize Tree
4. Convert BST to Sorted Doubly Linked List
5. Binary Tree Right Side View

This problem teaches:
- Tree restructuring
- In-place algorithms
- Morris traversal
- Space optimization
*/

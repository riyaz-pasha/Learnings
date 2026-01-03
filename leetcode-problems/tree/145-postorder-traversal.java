import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a TREE TRAVERSAL problem testing:
 * 1. Understanding of postorder traversal pattern
 * 2. Ability to implement iteratively (harder than inorder!)
 * 3. Knowledge of two-stack technique
 * 4. Understanding of when to use postorder
 * 5. Modified preorder trick for elegant solution
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. POSTORDER means: LEFT → RIGHT → ROOT
 *    - Process children before parent
 *    - Bottom-up traversal
 *    - Useful for deletion, tree height, postfix evaluation
 * 
 * 2. ITERATIVE IS TRICKY!
 *    - Unlike inorder, can't simply use one stack
 *    - Need to track if we've visited children
 *    - Multiple elegant approaches exist
 * 
 * 3. CLEVER INSIGHT: Reverse of modified preorder!
 *    - Preorder: ROOT → LEFT → RIGHT
 *    - Modified: ROOT → RIGHT → LEFT
 *    - Reverse: LEFT → RIGHT → ROOT (postorder!)
 * 
 * VISUALIZATION:
 * --------------
 *       1
 *      / \
 *     2   3
 *    / \
 *   4   5
 * 
 * Postorder: [4, 5, 2, 3, 1]
 * Process: 4 → 5 → 2 → 3 → 1 (leaves first, then root)
 * 
 * Why postorder?
 * - Calculate tree height: height = max(left, right) + 1
 * - Delete tree: delete children before parent
 * - Evaluate postfix expression
 * - Generate postfix notation
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Start with recursive (easy)
 *    "The recursive solution is straightforward..."
 * 
 * 2. For iterative, explain the challenge
 *    "Postorder is trickier iteratively because we need to ensure
 *     both children are processed before the parent."
 * 
 * 3. Show elegant two-stack solution
 *    "I can use two stacks or reverse a modified preorder."
 * 
 * 4. Alternative: one stack with visited tracking
 *    "Or track which nodes we've visited with a Set."
 * 
 * 5. Discuss applications
 *    - Tree deletion, height calculation, dependency resolution
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

class BinaryTreePostOrderTraversals {
    
    /**
     * APPROACH 1: RECURSIVE - THE SIMPLE SOLUTION
     * ============================================
     * 
     * ALGORITHM:
     * ----------
     * Postorder: LEFT → RIGHT → ROOT
     * 
     * 1. Recursively traverse left subtree
     * 2. Recursively traverse right subtree
     * 3. Visit current node (add to result)
     * 
     * BASE CASE: If node is null, return
     * 
     * VISUALIZATION OF RECURSION:
     * ---------------------------
     *       1
     *      / \
     *     2   3
     *    / \
     *   4   5
     * 
     * Call stack:
     * postorder(1)
     *   → postorder(2)
     *     → postorder(4)
     *       → postorder(null) [left of 4]
     *       → postorder(null) [right of 4]
     *       → add 4 to result [4]
     *     → postorder(5)
     *       → postorder(null) [left of 5]
     *       → postorder(null) [right of 5]
     *       → add 5 to result [4, 5]
     *     → add 2 to result [4, 5, 2]
     *   → postorder(3)
     *     → postorder(null) [left of 3]
     *     → postorder(null) [right of 3]
     *     → add 3 to result [4, 5, 2, 3]
     *   → add 1 to result [4, 5, 2, 3, 1]
     * 
     * Result: [4, 5, 2, 3, 1]
     * 
     * TIME COMPLEXITY: O(N)
     * - Visit each node exactly once
     * 
     * SPACE COMPLEXITY: O(H)
     * - H = height of tree
     * - Recursion stack depth
     * - Balanced: O(log N)
     * - Skewed: O(N)
     */
    public List<Integer> postorderTraversalRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        postorderHelper(root, result);
        return result;
    }
    
    private void postorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) {
            return;
        }
        
        // Postorder: LEFT → RIGHT → ROOT
        postorderHelper(node.left, result);   // 1. Process left subtree
        postorderHelper(node.right, result);  // 2. Process right subtree
        result.add(node.val);                 // 3. Visit current node (LAST!)
    }
    
    /**
     * APPROACH 2: ITERATIVE TWO STACKS - ELEGANT SOLUTION
     * ====================================================
     * 
     * ALGORITHM:
     * ----------
     * Key insight: Postorder is REVERSE of modified preorder!
     * 
     * Modified Preorder: ROOT → RIGHT → LEFT
     * Reverse it: LEFT → RIGHT → ROOT (Postorder!)
     * 
     * Steps:
     * 1. Do modified preorder (ROOT → RIGHT → LEFT) into stack1
     * 2. Pop from stack1 and push to stack2 (reverses order)
     * 3. Pop all from stack2 to get postorder
     * 
     * Or simpler: Use one stack + reverse at the end!
     * 
     * VISUALIZATION:
     * --------------
     *       1
     *      / \
     *     2   3
     *    / \
     *   4   5
     * 
     * Modified Preorder (ROOT → RIGHT → LEFT):
     * Visit order: 1, 3, 2, 5, 4
     * 
     * Reverse: [4, 5, 2, 3, 1] ← This is postorder!
     * 
     * DETAILED WALKTHROUGH:
     * ---------------------
     * 
     * Using modified preorder with one stack:
     * 
     * stack = [1]
     * result = []
     * 
     * Pop 1, add to result [1]
     * Push left(2) then right(3): stack = [2, 3]
     * 
     * Pop 3, add to result [1, 3]
     * 3 has no children: stack = [2]
     * 
     * Pop 2, add to result [1, 3, 2]
     * Push left(4) then right(5): stack = [4, 5]
     * 
     * Pop 5, add to result [1, 3, 2, 5]
     * 5 has no children: stack = [4]
     * 
     * Pop 4, add to result [1, 3, 2, 5, 4]
     * 4 has no children: stack = []
     * 
     * Result: [1, 3, 2, 5, 4]
     * Reverse: [4, 5, 2, 3, 1] ← Postorder!
     * 
     * TIME COMPLEXITY: O(N)
     * - Visit each node once: O(N)
     * - Reverse list: O(N)
     * - Total: O(N)
     * 
     * SPACE COMPLEXITY: O(N)
     * - Stack: O(H) at any time
     * - Result list: O(N)
     * - Total: O(N)
     * 
     * WHY THIS WORKS:
     * ---------------
     * Preorder: ROOT → LEFT → RIGHT
     * If we visit: ROOT → RIGHT → LEFT (mirror)
     * Then reverse: LEFT → RIGHT → ROOT (postorder!)
     * 
     * It's like doing preorder backwards!
     */
    public List<Integer> postorderTraversalTwoStacks(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;
        
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        
        // Do modified preorder: ROOT → RIGHT → LEFT
        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            result.add(node.val);
            
            // Push LEFT first, then RIGHT
            // So RIGHT is popped first (giving us ROOT → RIGHT → LEFT)
            if (node.left != null) {
                stack.push(node.left);
            }
            if (node.right != null) {
                stack.push(node.right);
            }
        }
        
        // Reverse to get postorder
        Collections.reverse(result);
        return result;
    }
    
    /**
     * APPROACH 3: ITERATIVE ONE STACK WITH TRACKING
     * ==============================================
     * 
     * ALGORITHM:
     * ----------
     * Use one stack + track last visited node
     * 
     * Key insight: Process node only when:
     * 1. It's a leaf (no children), OR
     * 2. We've already processed both children
     * 
     * Track last visited node to know if we came from right child
     * 
     * DETAILED LOGIC:
     * ---------------
     * 
     * For each node:
     * - If it has unvisited children, explore them first
     * - If no children OR both children visited, process it
     * 
     * How to know if children are visited?
     * - Track "last visited" node
     * - If last visited was right child, both children are done
     * 
     * WALKTHROUGH:
     * ------------
     *       1
     *      / \
     *     2   3
     *    / \
     *   4   5
     * 
     * curr = 1, last = null
     * → Has left child, go left
     * 
     * curr = 2, last = null
     * → Has left child, go left
     * 
     * curr = 4, last = null
     * → No children, process 4
     * → result = [4], last = 4
     * 
     * curr = 2 (from stack), last = 4
     * → Left child visited, has right child
     * → Go right
     * 
     * curr = 5, last = 4
     * → No children, process 5
     * → result = [4, 5], last = 5
     * 
     * curr = 2 (from stack), last = 5
     * → Right child visited, process 2
     * → result = [4, 5, 2], last = 2
     * 
     * curr = 1 (from stack), last = 2
     * → Left child visited, has right child
     * → Go right
     * 
     * curr = 3, last = 2
     * → No children, process 3
     * → result = [4, 5, 2, 3], last = 3
     * 
     * curr = 1 (from stack), last = 3
     * → Right child visited, process 1
     * → result = [4, 5, 2, 3, 1], last = 1
     * 
     * TIME COMPLEXITY: O(N)
     * - Each node visited at most twice
     * 
     * SPACE COMPLEXITY: O(H)
     * - Stack stores at most H nodes
     * 
     * PROS:
     * - No reversing needed
     * - More intuitive than two-stack
     * 
     * CONS:
     * - More complex logic
     * - Need to track last visited
     */
    public List<Integer> postorderTraversalOneStack(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;
        
        Stack<TreeNode> stack = new Stack<>();
        TreeNode curr = root;
        TreeNode lastVisited = null;
        
        while (curr != null || !stack.isEmpty()) {
            
            // Go as far left as possible
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }
            
            // Peek at top of stack (don't pop yet)
            TreeNode peekNode = stack.peek();
            
            // If right child exists AND we haven't visited it yet
            // Go to right child
            if (peekNode.right != null && lastVisited != peekNode.right) {
                curr = peekNode.right;
            }
            // Otherwise, we can process this node
            else {
                stack.pop();
                result.add(peekNode.val);
                lastVisited = peekNode;
            }
        }
        
        return result;
    }
    
    /**
     * APPROACH 4: MORRIS TRAVERSAL - O(1) SPACE
     * ==========================================
     * 
     * ALGORITHM:
     * ----------
     * Use threading for O(1) space (most complex)
     * 
     * Similar to inorder Morris but with different threading logic
     * 
     * TIME COMPLEXITY: O(N)
     * SPACE COMPLEXITY: O(1)
     * 
     * This is the most complex approach and rarely needed.
     * Included for completeness but two-stack is usually preferred.
     */
    public List<Integer> postorderTraversalMorris(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        TreeNode dummy = new TreeNode(0);
        dummy.left = root;
        TreeNode curr = dummy;
        
        while (curr != null) {
            if (curr.left == null) {
                curr = curr.right;
            } else {
                TreeNode predecessor = curr.left;
                while (predecessor.right != null && predecessor.right != curr) {
                    predecessor = predecessor.right;
                }
                
                if (predecessor.right == null) {
                    predecessor.right = curr;
                    curr = curr.left;
                } else {
                    // Reverse nodes from curr.left to predecessor
                    TreeNode node = curr.left;
                    reverseAddNodes(node, predecessor, result);
                    predecessor.right = null;
                    curr = curr.right;
                }
            }
        }
        
        return result;
    }
    
    private void reverseAddNodes(TreeNode from, TreeNode to, List<Integer> result) {
        reverse(from, to);
        TreeNode node = to;
        while (true) {
            result.add(node.val);
            if (node == from) break;
            node = node.right;
        }
        reverse(to, from);
    }
    
    private void reverse(TreeNode from, TreeNode to) {
        if (from == to) return;
        TreeNode prev = from;
        TreeNode curr = from.right;
        while (prev != to) {
            TreeNode next = curr.right;
            curr.right = prev;
            prev = curr;
            curr = next;
        }
    }
}

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach      | Time | Space | Complexity | Best For
 * --------------|------|-------|------------|-------------------------
 * Recursive     | O(N) | O(H)  | Simple     | Default, clean code
 * Two Stacks    | O(N) | O(N)  | Easy       | Elegant iterative
 * One Stack     | O(N) | O(H)  | Medium     | Space-efficient iterative
 * Morris        | O(N) | O(1)  | Complex    | O(1) space requirement
 * 
 * RECOMMENDATION:
 * - Interview: Start with recursive, show two-stack if asked for iterative
 * - Production: Two-stack for clarity, one-stack for space efficiency
 */

/**
 * COMPARISON WITH OTHER TRAVERSALS
 * =================================
 * 
 * Given tree:
 *       1
 *      / \
 *     2   3
 *    / \
 *   4   5
 * 
 * PREORDER (ROOT → LEFT → RIGHT):  [1, 2, 4, 5, 3]
 * INORDER (LEFT → ROOT → RIGHT):   [4, 2, 5, 1, 3]
 * POSTORDER (LEFT → RIGHT → ROOT): [4, 5, 2, 3, 1]
 * LEVEL ORDER (BFS):               [[1], [2, 3], [4, 5]]
 * 
 * Notice:
 * - Postorder visits leaves first, root last
 * - Reverse of modified preorder
 * - Good for bottom-up processing
 */

/**
 * APPLICATIONS OF POSTORDER TRAVERSAL
 * ====================================
 * 
 * 1. DELETING A TREE
 *    - Must delete children before parent
 *    - Postorder ensures this
 * 
 * 2. CALCULATING TREE HEIGHT
 *    - height(node) = max(height(left), height(right)) + 1
 *    - Need children's heights first
 * 
 * 3. EVALUATING POSTFIX EXPRESSIONS
 *    - Expression tree evaluation
 *    - Process operands before operator
 * 
 * 4. DEPENDENCY RESOLUTION
 *    - Process dependencies before dependents
 *    - Build systems, package managers
 * 
 * 5. BOTTOM-UP COMPUTATION
 *    - Any computation needing child results first
 *    - Tree DP problems
 */

// Example applications
class PostorderApplications {
    
    /**
     * APPLICATION 1: Calculate tree height
     */
    public int maxDepth(TreeNode root) {
        if (root == null) return 0;
        
        // Postorder: need children's heights first
        int leftHeight = maxDepth(root.left);
        int rightHeight = maxDepth(root.right);
        
        // Then compute current height
        return Math.max(leftHeight, rightHeight) + 1;
    }
    
    /**
     * APPLICATION 2: Delete tree (free memory)
     */
    public void deleteTree(TreeNode root) {
        if (root == null) return;
        
        // Postorder: delete children first
        deleteTree(root.left);
        deleteTree(root.right);
        
        // Then delete current (in languages with manual memory management)
        root.left = null;
        root.right = null;
        // In C++: delete root;
    }
    
    /**
     * APPLICATION 3: Check if tree is balanced
     */
    public boolean isBalanced(TreeNode root) {
        return checkHeight(root) != -1;
    }
    
    private int checkHeight(TreeNode node) {
        if (node == null) return 0;
        
        // Postorder: check children first
        int leftHeight = checkHeight(node.left);
        if (leftHeight == -1) return -1;
        
        int rightHeight = checkHeight(node.right);
        if (rightHeight == -1) return -1;
        
        // Then check current
        if (Math.abs(leftHeight - rightHeight) > 1) return -1;
        return Math.max(leftHeight, rightHeight) + 1;
    }
    
    /**
     * APPLICATION 4: Serialize tree
     */
    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }
    
    private void serializeHelper(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append("null,");
            return;
        }
        
        // Postorder serialization
        serializeHelper(node.left, sb);
        serializeHelper(node.right, sb);
        sb.append(node.val).append(",");
    }
}

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        BinaryTreePostOrderTraversals sol = new BinaryTreePostOrderTraversals();
        
        System.out.println("=== Test Case 1: Example Tree ===");
        //       1
        //      / \
        //     2   3
        //    / \
        //   4   5
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.left.left = new TreeNode(4);
        root1.left.right = new TreeNode(5);
        
        System.out.println("Recursive:  " + sol.postorderTraversalRecursive(root1));
        System.out.println("Two Stacks: " + sol.postorderTraversalTwoStacks(root1));
        System.out.println("One Stack:  " + sol.postorderTraversalOneStack(root1));
        System.out.println("Morris:     " + sol.postorderTraversalMorris(root1));
        System.out.println("Expected:   [4, 5, 2, 3, 1]");
        
        System.out.println("\n=== Test Case 2: Empty Tree ===");
        System.out.println("Result:   " + sol.postorderTraversalRecursive(null));
        System.out.println("Expected: []");
        
        System.out.println("\n=== Test Case 3: Single Node ===");
        TreeNode root3 = new TreeNode(1);
        System.out.println("Result:   " + sol.postorderTraversalRecursive(root3));
        System.out.println("Expected: [1]");
        
        System.out.println("\n=== Test Case 4: Left-Skewed Tree ===");
        //     3
        //    /
        //   2
        //  /
        // 1
        TreeNode root4 = new TreeNode(3);
        root4.left = new TreeNode(2);
        root4.left.left = new TreeNode(1);
        System.out.println("Result:   " + sol.postorderTraversalRecursive(root4));
        System.out.println("Expected: [1, 2, 3]");
        
        System.out.println("\n=== Test Case 5: Right-Skewed Tree ===");
        // 1
        //  \
        //   2
        //    \
        //     3
        TreeNode root5 = new TreeNode(1);
        root5.right = new TreeNode(2);
        root5.right.right = new TreeNode(3);
        System.out.println("Result:   " + sol.postorderTraversalRecursive(root5));
        System.out.println("Expected: [3, 2, 1]");
        
        System.out.println("\n=== Test Case 6: Complete Binary Tree ===");
        //       4
        //      / \
        //     2   6
        //    / \ / \
        //   1  3 5  7
        TreeNode root6 = new TreeNode(4);
        root6.left = new TreeNode(2);
        root6.right = new TreeNode(6);
        root6.left.left = new TreeNode(1);
        root6.left.right = new TreeNode(3);
        root6.right.left = new TreeNode(5);
        root6.right.right = new TreeNode(7);
        System.out.println("Result:   " + sol.postorderTraversalRecursive(root6));
        System.out.println("Expected: [1, 3, 2, 5, 7, 6, 4]");
        
        System.out.println("\n=== Comparing All Traversal Orders ===");
        System.out.println("Tree structure:");
        System.out.println("       1");
        System.out.println("      / \\");
        System.out.println("     2   3");
        System.out.println("    / \\");
        System.out.println("   4   5");
        System.out.println();
        System.out.println("Preorder:   [1, 2, 4, 5, 3] (ROOT first)");
        System.out.println("Inorder:    [4, 2, 5, 1, 3] (ROOT middle)");
        System.out.println("Postorder:  [4, 5, 2, 3, 1] (ROOT last)");
        
        System.out.println("\n=== Demonstrating Applications ===");
        PostorderApplications apps = new PostorderApplications();
        System.out.println("Tree height: " + apps.maxDepth(root1));
        System.out.println("Is balanced: " + apps.isBalanced(root1));
        System.out.println("Serialized:  " + apps.serialize(root1));
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "Postorder means LEFT → RIGHT → ROOT. We process children
 *    before the parent, which is useful for bottom-up computations."
 * 
 * 2. "The recursive solution is straightforward - just change the
 *    order compared to inorder or preorder."
 *    [Write recursive solution]
 * 
 * 3. "For iterative, postorder is trickier than inorder because we
 *    need to ensure both children are processed first."
 * 
 * 4. "The elegant approach: Do modified preorder (ROOT → RIGHT → LEFT)
 *    and reverse it. This gives us LEFT → RIGHT → ROOT!"
 *    [Write two-stack solution]
 * 
 * 5. "Applications include: tree deletion (delete children first),
 *    calculating height (need children's heights), and evaluating
 *    postfix expressions."
 * 
 * 6. "Time is O(N) for all approaches. Space is O(H) for recursive
 *    and one-stack, O(N) for two-stack due to the result list."
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Why is iterative postorder harder than preorder/inorder?
 * - How would you use this to calculate tree height?
 * - Can you do it with O(1) space? [Morris]
 * - What's the relationship between postorder and expression evaluation?
 * - How does this compare to other traversals?
 */

class BinaryTreePostorderTraversal {

    public List<Integer> postorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        this.postorderTraversal(root, result);
        return result;
    }

    private void postorderTraversal(TreeNode root, List<Integer> result) {
        if (root == null) {
            return;
        }
        this.postorderTraversal(root.left, result);
        this.postorderTraversal(root.right, result);
        result.add(root.val);
    }

}

class BinaryTreePostorderIterativeTraversal {

    public List<Integer> postOrderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null)
            return result;

        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {

            TreeNode current = stack.pop();
            result.addFirst(current.val);

            if (current.left != null) {
                stack.push(current.left);
            }
            if (current.right != null) {
                stack.push(current.right);
            }
        }
        return result;
    }

}

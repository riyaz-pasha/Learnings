import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a TREE TRAVERSAL problem testing:
 * 1. Understanding of binary tree structure
 * 2. Knowledge of recursive and iterative approaches
 * 3. Stack-based simulation of recursion
 * 4. Morris Traversal for O(1) space
 * 5. Fundamental algorithm that appears everywhere
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. INORDER means: LEFT → ROOT → RIGHT
 *    - For BST: gives sorted order!
 *    - Most commonly used traversal
 * 
 * 2. THREE MAIN APPROACHES:
 *    - Recursive: Simple, elegant, O(h) space
 *    - Iterative with stack: Simulates recursion, O(h) space
 *    - Morris Traversal: Threading, O(1) space (advanced)
 * 
 * 3. TRAVERSAL TYPES (all important):
 *    - Inorder: LEFT → ROOT → RIGHT
 *    - Preorder: ROOT → LEFT → RIGHT
 *    - Postorder: LEFT → RIGHT → ROOT
 *    - Level-order: BFS, level by level
 * 
 * VISUALIZATION:
 * --------------
 *       1
 *        \
 *         2
 *        /
 *       3
 * 
 * Inorder: [1, 3, 2]
 * Process: 1 (left=null) → 1 → right(2) → 2.left(3) → 3 → 2
 * 
 * Another example:
 *       4
 *      / \
 *     2   6
 *    / \ / \
 *   1  3 5  7
 * 
 * Inorder: [1, 2, 3, 4, 5, 6, 7] ← SORTED for BST!
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Start with recursive solution
 *    - Simplest and most intuitive
 *    - Show understanding of tree recursion
 * 
 * 2. If asked for iterative, use stack
 *    - Demonstrate understanding of how recursion works
 *    - Explain call stack simulation
 * 
 * 3. If asked for O(1) space, mention Morris
 *    - Advanced technique
 *    - Shows deep algorithmic knowledge
 * 
 * 4. Discuss follow-ups
 *    - Preorder/postorder variations
 *    - BST validation using inorder
 *    - Recovering BST from traversals
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

class BinaryTreeInorderTraversalSolutions {
    
    /**
     * APPROACH 1: RECURSIVE - THE ELEGANT SOLUTION
     * =============================================
     * 
     * ALGORITHM:
     * ----------
     * Inorder: LEFT → ROOT → RIGHT
     * 
     * 1. Recursively traverse left subtree
     * 2. Visit current node (add to result)
     * 3. Recursively traverse right subtree
     * 
     * BASE CASE: If node is null, return
     * 
     * VISUALIZATION OF RECURSION:
     * ---------------------------
     *       4
     *      / \
     *     2   6
     *    / \
     *   1   3
     * 
     * Call stack:
     * inorder(4)
     *   → inorder(2)
     *     → inorder(1)
     *       → inorder(null) [left of 1]
     *       → add 1 to result
     *       → inorder(null) [right of 1]
     *     → add 2 to result
     *     → inorder(3)
     *       → inorder(null) [left of 3]
     *       → add 3 to result
     *       → inorder(null) [right of 3]
     *   → add 4 to result
     *   → inorder(6)
     *     → inorder(null) [left of 6]
     *     → add 6 to result
     *     → inorder(null) [right of 6]
     * 
     * Result: [1, 2, 3, 4, 6]
     * 
     * TIME COMPLEXITY: O(N)
     * - Visit each node exactly once
     * - N nodes → N recursive calls
     * 
     * SPACE COMPLEXITY: O(H)
     * - H = height of tree
     * - Recursion stack depth = height
     * - Balanced tree: O(log N)
     * - Skewed tree: O(N)
     * 
     * PROS:
     * - Clean, readable code
     * - Easy to understand
     * - Natural for tree problems
     * 
     * CONS:
     * - Uses recursion stack space
     * - Can cause stack overflow for very deep trees
     */
    public List<Integer> inorderTraversalRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorderHelper(root, result);
        return result;
    }
    
    private void inorderHelper(TreeNode node, List<Integer> result) {
        // Base case: empty node
        if (node == null) {
            return;
        }
        
        // Inorder: LEFT → ROOT → RIGHT
        inorderHelper(node.left, result);   // 1. Process left subtree
        result.add(node.val);                // 2. Visit current node
        inorderHelper(node.right, result);   // 3. Process right subtree
    }
    
    /**
     * APPROACH 2: ITERATIVE WITH STACK - SIMULATING RECURSION
     * ========================================================
     * 
     * ALGORITHM:
     * ----------
     * Use explicit stack to simulate recursive call stack
     * 
     * Key insight: In inorder, we need to:
     * 1. Go as far left as possible (pushing nodes onto stack)
     * 2. When can't go left, process current node
     * 3. Then go right
     * 
     * DETAILED WALKTHROUGH:
     * ---------------------
     *       4
     *      / \
     *     2   6
     *    / \
     *   1   3
     * 
     * Step-by-step:
     * 
     * curr = 4, stack = []
     * → Push 4, go left
     * 
     * curr = 2, stack = [4]
     * → Push 2, go left
     * 
     * curr = 1, stack = [4, 2]
     * → Push 1, go left
     * 
     * curr = null, stack = [4, 2, 1]
     * → Pop 1, add to result [1]
     * → Go right (1.right = null)
     * 
     * curr = null, stack = [4, 2]
     * → Pop 2, add to result [1, 2]
     * → Go right (2.right = 3)
     * 
     * curr = 3, stack = [4]
     * → Push 3, go left
     * 
     * curr = null, stack = [4, 3]
     * → Pop 3, add to result [1, 2, 3]
     * → Go right (3.right = null)
     * 
     * curr = null, stack = [4]
     * → Pop 4, add to result [1, 2, 3, 4]
     * → Go right (4.right = 6)
     * 
     * curr = 6, stack = []
     * → Push 6, go left
     * 
     * curr = null, stack = [6]
     * → Pop 6, add to result [1, 2, 3, 4, 6]
     * → Go right (6.right = null)
     * 
     * curr = null, stack = []
     * → Done!
     * 
     * TIME COMPLEXITY: O(N)
     * - Visit each node exactly once
     * - Each node pushed and popped once
     * 
     * SPACE COMPLEXITY: O(H)
     * - Stack stores at most H nodes (height)
     * - Same as recursive approach
     * 
     * WHEN TO USE:
     * - When recursion depth is a concern
     * - When you need explicit control over traversal
     * - In languages without tail call optimization
     */
    public List<Integer> inorderTraversalIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Stack<TreeNode> stack = new Stack<>();
        TreeNode curr = root;
        
        // Continue while we have nodes to process
        // Either current node exists OR stack has nodes
        while (curr != null || !stack.isEmpty()) {
            
            // Phase 1: Go as far left as possible
            // Push all left nodes onto stack
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }
            
            // Phase 2: Process node (we've gone as far left as possible)
            curr = stack.pop();
            result.add(curr.val);
            
            // Phase 3: Go right
            // If right exists, we'll process it and its left subtree
            // If right is null, we'll pop next node from stack
            curr = curr.right;
        }
        
        return result;
    }
    
    /**
     * APPROACH 3: MORRIS TRAVERSAL - O(1) SPACE SOLUTION
     * ===================================================
     * 
     * ALGORITHM:
     * ----------
     * Use THREADING to avoid stack/recursion
     * 
     * Key idea: Temporarily modify tree structure
     * 1. For each node, find its inorder predecessor
     * 2. Create temporary link: predecessor.right = current
     * 3. Use this link to return to current after processing left subtree
     * 4. Clean up links as we go
     * 
     * THREADING VISUALIZATION:
     * ------------------------
     * Original tree:
     *       4
     *      / \
     *     2   6
     *    / \
     *   1   3
     * 
     * Step 1: At 4, predecessor of 4 is 3
     * Create thread: 3.right = 4
     *       4
     *      / \
     *     2   6
     *    / \
     *   1   3
     *        \
     *         4 (thread)
     * 
     * Step 2: Process left subtree using threads
     * Step 3: Remove threads after use
     * 
     * DETAILED ALGORITHM:
     * -------------------
     * 1. If current.left is null:
     *    - Process current
     *    - Move to right
     * 
     * 2. If current.left exists:
     *    - Find predecessor (rightmost node in left subtree)
     *    
     *    a) If predecessor.right is null:
     *       - Create thread: predecessor.right = current
     *       - Move to left
     *    
     *    b) If predecessor.right == current (thread exists):
     *       - Remove thread: predecessor.right = null
     *       - Process current
     *       - Move to right
     * 
     * TIME COMPLEXITY: O(N)
     * - Each node visited at most 3 times
     * - Finding predecessor: amortized O(1)
     * 
     * SPACE COMPLEXITY: O(1)
     * - No stack or recursion!
     * - Only a few pointers
     * - Tree is restored to original state
     * 
     * WHEN TO USE:
     * - When O(1) space is critical requirement
     * - When tree can be temporarily modified
     * - Showing advanced algorithm knowledge in interviews
     * 
     * PROS:
     * - O(1) space complexity
     * - No stack overflow risk
     * 
     * CONS:
     * - More complex logic
     * - Temporarily modifies tree
     * - Harder to understand and debug
     */
    public List<Integer> inorderTraversalMorris(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        TreeNode curr = root;
        
        while (curr != null) {
            
            // Case 1: No left subtree
            // Process current and go right
            if (curr.left == null) {
                result.add(curr.val);
                curr = curr.right;
            }
            // Case 2: Left subtree exists
            else {
                // Find inorder predecessor (rightmost node in left subtree)
                TreeNode predecessor = curr.left;
                while (predecessor.right != null && predecessor.right != curr) {
                    predecessor = predecessor.right;
                }
                
                // Sub-case 2a: Thread doesn't exist yet
                // Create thread and explore left subtree
                if (predecessor.right == null) {
                    predecessor.right = curr;  // Create thread
                    curr = curr.left;          // Go left
                }
                // Sub-case 2b: Thread exists (we've processed left subtree)
                // Remove thread, process current, go right
                else {
                    predecessor.right = null;  // Remove thread
                    result.add(curr.val);      // Process current
                    curr = curr.right;         // Go right
                }
            }
        }
        
        return result;
    }
}

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach   | Time | Space    | Complexity | When to Use
 * -----------|------|----------|------------|---------------------------
 * Recursive  | O(N) | O(H)     | Simple     | Default choice, clean code
 * Iterative  | O(N) | O(H)     | Medium     | Avoid recursion limits
 * Morris     | O(N) | O(1)     | Complex    | O(1) space requirement
 * 
 * Where H = height of tree
 * - Balanced: H = O(log N)
 * - Skewed: H = O(N)
 */

/**
 * OTHER TRAVERSAL TYPES
 * ======================
 * 
 * For completeness, here are other common traversals:
 */
class TraversalVariations {
    
    /**
     * PREORDER: ROOT → LEFT → RIGHT
     * Use case: Creating a copy of tree, prefix expression evaluation
     */
    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorderHelper(root, result);
        return result;
    }
    
    private void preorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        result.add(node.val);           // ROOT first
        preorderHelper(node.left, result);
        preorderHelper(node.right, result);
    }
    
    /**
     * POSTORDER: LEFT → RIGHT → ROOT
     * Use case: Deleting tree, postfix expression evaluation
     */
    public List<Integer> postorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        postorderHelper(root, result);
        return result;
    }
    
    private void postorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        postorderHelper(node.left, result);
        postorderHelper(node.right, result);
        result.add(node.val);           // ROOT last
    }
    
    /**
     * LEVEL ORDER: Level by level (BFS)
     * Use case: Level-based processing, finding minimum depth
     */
    public List<List<Integer>> levelOrderTraversal(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> level = new ArrayList<>();
            
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);
                
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            
            result.add(level);
        }
        
        return result;
    }
}

/**
 * APPLICATIONS OF INORDER TRAVERSAL
 * ==================================
 * 
 * 1. BST Validation
 *    - Inorder of BST is sorted
 *    - Check if result is in ascending order
 * 
 * 2. Finding Kth Smallest in BST
 *    - Inorder traversal gives sorted order
 *    - Return kth element
 * 
 * 3. BST to Sorted Array
 *    - Direct inorder traversal
 * 
 * 4. Recover BST
 *    - Two elements swapped in BST
 *    - Use inorder to find them
 * 
 * 5. BST Iterator
 *    - Implement next() using inorder
 *    - Use stack-based approach
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        BinaryTreeInorderTraversalSolutions sol = new BinaryTreeInorderTraversalSolutions();
        
        System.out.println("=== Test Case 1: Example Tree ===");
        //       1
        //        \
        //         2
        //        /
        //       3
        TreeNode root1 = new TreeNode(1);
        root1.right = new TreeNode(2);
        root1.right.left = new TreeNode(3);
        
        System.out.println("Recursive: " + sol.inorderTraversalRecursive(root1));
        System.out.println("Iterative: " + sol.inorderTraversalIterative(root1));
        System.out.println("Morris: " + sol.inorderTraversalMorris(root1));
        System.out.println("Expected: [1, 3, 2]");
        
        System.out.println("\n=== Test Case 2: Empty Tree ===");
        TreeNode root2 = null;
        System.out.println("Recursive: " + sol.inorderTraversalRecursive(root2));
        System.out.println("Expected: []");
        
        System.out.println("\n=== Test Case 3: Single Node ===");
        TreeNode root3 = new TreeNode(1);
        System.out.println("Recursive: " + sol.inorderTraversalRecursive(root3));
        System.out.println("Expected: [1]");
        
        System.out.println("\n=== Test Case 4: Complete Binary Tree ===");
        //       4
        //      / \
        //     2   6
        //    / \ / \
        //   1  3 5  7
        TreeNode root4 = new TreeNode(4);
        root4.left = new TreeNode(2);
        root4.right = new TreeNode(6);
        root4.left.left = new TreeNode(1);
        root4.left.right = new TreeNode(3);
        root4.right.left = new TreeNode(5);
        root4.right.right = new TreeNode(7);
        
        System.out.println("Recursive: " + sol.inorderTraversalRecursive(root4));
        System.out.println("Iterative: " + sol.inorderTraversalIterative(root4));
        System.out.println("Morris: " + sol.inorderTraversalMorris(root4));
        System.out.println("Expected: [1, 2, 3, 4, 5, 6, 7] (sorted - it's a BST!)");
        
        System.out.println("\n=== Test Case 5: Left-Skewed Tree ===");
        //     3
        //    /
        //   2
        //  /
        // 1
        TreeNode root5 = new TreeNode(3);
        root5.left = new TreeNode(2);
        root5.left.left = new TreeNode(1);
        
        System.out.println("Recursive: " + sol.inorderTraversalRecursive(root5));
        System.out.println("Expected: [1, 2, 3]");
        
        System.out.println("\n=== Test Case 6: Right-Skewed Tree ===");
        // 1
        //  \
        //   2
        //    \
        //     3
        TreeNode root6 = new TreeNode(1);
        root6.right = new TreeNode(2);
        root6.right.right = new TreeNode(3);
        
        System.out.println("Recursive: " + sol.inorderTraversalRecursive(root6));
        System.out.println("Expected: [1, 2, 3]");
        
        // Demonstrate other traversals
        System.out.println("\n=== Comparing All Traversal Types ===");
        System.out.println("Tree structure:");
        System.out.println("       4");
        System.out.println("      / \\");
        System.out.println("     2   6");
        System.out.println("    / \\ / \\");
        System.out.println("   1  3 5  7");
        
        TraversalVariations tv = new TraversalVariations();
        System.out.println("\nInorder (L-Root-R): " + sol.inorderTraversalRecursive(root4));
        System.out.println("Preorder (Root-L-R): " + tv.preorderTraversal(root4));
        System.out.println("Postorder (L-R-Root): " + tv.postorderTraversal(root4));
        System.out.println("Level Order: " + tv.levelOrderTraversal(root4));
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "Inorder traversal means LEFT → ROOT → RIGHT. For a BST,
 *    this gives us elements in sorted order, which is very useful."
 * 
 * 2. "I'll start with the recursive solution as it's the most
 *    intuitive and clean."
 *    [Write recursive solution]
 * 
 * 3. "The time complexity is O(N) as we visit each node once.
 *    Space is O(H) for the recursion stack, where H is height."
 * 
 * 4. "If you'd like an iterative solution, I can use a stack
 *    to simulate the recursion. This has the same complexity
 *    but gives us more explicit control."
 *    [Write iterative solution if asked]
 * 
 * 5. "There's also Morris Traversal which achieves O(1) space
 *    by threading the tree. It's more complex but interesting
 *    if you'd like me to explain it."
 * 
 * 6. "For follow-ups: This pattern extends to preorder and postorder
 *    by changing the order of operations. For BST problems,
 *    inorder is especially powerful since it gives sorted order."
 * 
 * 7. "Applications include BST validation, finding kth smallest
 *    element, and recovering corrupted BSTs."
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Can you do it iteratively? [Show stack approach]
 * - Can you do it with O(1) space? [Show Morris]
 * - How would you do preorder/postorder? [Change order]
 * - How would you validate a BST using this? [Check sorted]
 * - What if tree has parent pointers? [Can use them instead]
 */

class BinaryTreeTraversal {
    public List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        this.inorderTraversal(root, result);
        return result;
    }

    private void inorderTraversal(TreeNode root, List<Integer> result) {
        if (root == null)
            return;
        this.inorderTraversal(root.left, result);
        result.add(root.val);
        this.inorderTraversal(root.right, result);
    }
}

class BinaryTreeIterativeTraversal {

    public List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null)
            return result;

        Stack<TreeNode> stack = new Stack<>();
        TreeNode current = root;
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.add(current);
                current = current.left;
            }
            current = stack.pop();
            result.add(current.val);
            current = current.right;
        }
        return result;
    }
}

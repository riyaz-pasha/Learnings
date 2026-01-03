import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a TREE TRAVERSAL problem testing:
 * 1. Understanding of preorder traversal pattern
 * 2. Knowledge of DFS (Depth-First Search)
 * 3. Stack-based iterative implementation
 * 4. Morris Traversal for O(1) space
 * 5. When and why to use preorder
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. PREORDER means: ROOT → LEFT → RIGHT
 *    - Process parent BEFORE children
 *    - Top-down traversal
 *    - Natural for DFS, tree copying, prefix expressions
 * 
 * 2. EASIEST ITERATIVE TRAVERSAL!
 *    - Unlike postorder, very straightforward with stack
 *    - Process node immediately when we visit it
 *    - Push right first, then left (so left is popped first)
 * 
 * 3. MOST COMMONLY USED TRAVERSAL
 *    - DFS in graphs uses preorder logic
 *    - Tree serialization typically uses preorder
 *    - Creating deep copies uses preorder
 * 
 * VISUALIZATION:
 * --------------
 *       1
 *      / \
 *     2   3
 *    / \
 *   4   5
 * 
 * Preorder: [1, 2, 4, 5, 3]
 * Process: 1 → 2 → 4 → 5 → 3 (root first, then children)
 * 
 * Think of it as: "Visit each node as you see it going down"
 * 
 * Why preorder?
 * - Copy tree: create node, then copy children
 * - Serialize tree: save node, then children
 * - Prefix expression: operator before operands
 * - DFS exploration: process node before going deeper
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Start with recursive (simple)
 *    "The most natural approach is recursive..."
 * 
 * 2. Show iterative with stack
 *    "For iterative, I'll use a stack. This is actually simpler
 *     than inorder or postorder because we process nodes immediately."
 * 
 * 3. Explain the order
 *    "I push right child first, then left, so left is popped first."
 * 
 * 4. Mention Morris if asked
 *    "For O(1) space, there's Morris Traversal..."
 * 
 * 5. Discuss applications
 *    - Tree serialization, copying, DFS, expression trees
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

class BinaryTreePreorderTraversalSolutions {
    
    /**
     * APPROACH 1: RECURSIVE - THE NATURAL SOLUTION
     * =============================================
     * 
     * ALGORITHM:
     * ----------
     * Preorder: ROOT → LEFT → RIGHT
     * 
     * 1. Visit current node (add to result) - FIRST!
     * 2. Recursively traverse left subtree
     * 3. Recursively traverse right subtree
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
     * Call stack and order of visiting:
     * 
     * preorder(1)
     *   → add 1 to result [1]
     *   → preorder(2)
     *     → add 2 to result [1, 2]
     *     → preorder(4)
     *       → add 4 to result [1, 2, 4]
     *       → preorder(null) [left of 4]
     *       → preorder(null) [right of 4]
     *     → preorder(5)
     *       → add 5 to result [1, 2, 4, 5]
     *       → preorder(null) [left of 5]
     *       → preorder(null) [right of 5]
     *   → preorder(3)
     *     → add 3 to result [1, 2, 4, 5, 3]
     *     → preorder(null) [left of 3]
     *     → preorder(null) [right of 3]
     * 
     * Result: [1, 2, 4, 5, 3]
     * 
     * Notice: We add to result IMMEDIATELY upon visiting each node!
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
     * - Simplest and most intuitive
     * - Matches natural thinking process
     * - Easy to understand and implement
     * 
     * CONS:
     * - Uses recursion stack space
     * - Can cause stack overflow for very deep trees
     */
    public List<Integer> preorderTraversalRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorderHelper(root, result);
        return result;
    }
    
    private void preorderHelper(TreeNode node, List<Integer> result) {
        // Base case: empty node
        if (node == null) {
            return;
        }
        
        // Preorder: ROOT → LEFT → RIGHT
        result.add(node.val);                // 1. Visit current node FIRST!
        preorderHelper(node.left, result);   // 2. Process left subtree
        preorderHelper(node.right, result);  // 3. Process right subtree
    }
    
    /**
     * APPROACH 2: ITERATIVE WITH STACK - SIMPLEST ITERATIVE TRAVERSAL!
     * =================================================================
     * 
     * ALGORITHM:
     * ----------
     * Use explicit stack to simulate recursive call stack
     * 
     * Key insight: Unlike inorder/postorder, preorder is SIMPLE!
     * - Process node immediately when we pop it
     * - Push right child first, then left (so left is processed first)
     * 
     * DETAILED WALKTHROUGH:
     * ---------------------
     *       1
     *      / \
     *     2   3
     *    / \
     *   4   5
     * 
     * Step-by-step execution:
     * 
     * Initial: stack = [1], result = []
     * 
     * Pop 1, add to result [1]
     * Push right(3), then left(2): stack = [3, 2]
     * 
     * Pop 2, add to result [1, 2]
     * Push right(5), then left(4): stack = [3, 5, 4]
     * 
     * Pop 4, add to result [1, 2, 4]
     * No children: stack = [3, 5]
     * 
     * Pop 5, add to result [1, 2, 4, 5]
     * No children: stack = [3]
     * 
     * Pop 3, add to result [1, 2, 4, 5, 3]
     * No children: stack = []
     * 
     * Done! Result: [1, 2, 4, 5, 3]
     * 
     * WHY PUSH RIGHT FIRST?
     * ---------------------
     * Stack is LIFO (Last In, First Out)
     * - We want: ROOT → LEFT → RIGHT
     * - We push: RIGHT, then LEFT
     * - We pop: LEFT, then RIGHT ✓
     * 
     * This ensures correct preorder!
     * 
     * TIME COMPLEXITY: O(N)
     * - Visit each node exactly once
     * - Each node pushed and popped once
     * 
     * SPACE COMPLEXITY: O(H)
     * - Stack stores at most H nodes (height)
     * - Worst case (skewed): O(N)
     * - Best case (balanced): O(log N)
     * 
     * COMPARISON TO OTHER TRAVERSALS:
     * - Preorder: EASIEST iterative (process immediately)
     * - Inorder: MEDIUM (need to go left first, then process)
     * - Postorder: HARDEST (need to ensure children done first)
     * 
     * WHEN TO USE:
     * - Default iterative approach for preorder
     * - When recursion depth is a concern
     * - When you need explicit control over traversal
     */
    public List<Integer> preorderTraversalIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;
        
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        
        while (!stack.isEmpty()) {
            // Pop and process immediately (that's preorder!)
            TreeNode node = stack.pop();
            result.add(node.val);
            
            // Push RIGHT first, then LEFT
            // So LEFT is popped first (LIFO)
            if (node.right != null) {
                stack.push(node.right);
            }
            if (node.left != null) {
                stack.push(node.left);
            }
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
     * For preorder, we process node when we first visit it
     * 
     * Key steps:
     * 1. If no left child: process current, go right
     * 2. If left child exists:
     *    a) Find predecessor (rightmost node in left subtree)
     *    b) If no thread: create thread, PROCESS CURRENT, go left
     *    c) If thread exists: remove thread, go right
     * 
     * DIFFERENCE FROM INORDER MORRIS:
     * -------------------------------
     * - Inorder: Process node after returning from left
     * - Preorder: Process node BEFORE going left
     * 
     * VISUALIZATION:
     * --------------
     *       1
     *      / \
     *     2   3
     *    / \
     *   4   5
     * 
     * Step 1: At 1, create thread from 5 to 1, process 1 → [1]
     * Step 2: At 2, create thread from 4 to 2, process 2 → [1, 2]
     * Step 3: At 4, no left child, process 4 → [1, 2, 4]
     * Step 4: Follow thread back to 2, remove thread
     * Step 5: At 5, no left child, process 5 → [1, 2, 4, 5]
     * Step 6: Follow thread back to 1, remove thread
     * Step 7: At 3, no left child, process 3 → [1, 2, 4, 5, 3]
     * 
     * TIME COMPLEXITY: O(N)
     * - Each node visited at most 3 times
     * - Finding predecessor: amortized O(1)
     * 
     * SPACE COMPLEXITY: O(1)
     * - No stack or recursion!
     * - Only a few pointers
     * - Tree restored to original state
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
    public List<Integer> preorderTraversalMorris(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        TreeNode curr = root;
        
        while (curr != null) {
            
            // Case 1: No left child
            // Process current and go right
            if (curr.left == null) {
                result.add(curr.val);
                curr = curr.right;
            }
            // Case 2: Left child exists
            else {
                // Find inorder predecessor (rightmost node in left subtree)
                TreeNode predecessor = curr.left;
                while (predecessor.right != null && predecessor.right != curr) {
                    predecessor = predecessor.right;
                }
                
                // Sub-case 2a: Thread doesn't exist yet
                // PROCESS CURRENT (key difference from inorder!)
                // Create thread and explore left subtree
                if (predecessor.right == null) {
                    result.add(curr.val);      // Process BEFORE going left!
                    predecessor.right = curr;  // Create thread
                    curr = curr.left;          // Go left
                }
                // Sub-case 2b: Thread exists (we've processed left subtree)
                // Remove thread, go right (don't process current again!)
                else {
                    predecessor.right = null;  // Remove thread
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
 * Approach   | Time | Space | Complexity | When to Use
 * -----------|------|-------|------------|---------------------------
 * Recursive  | O(N) | O(H)  | Simple     | Default choice, clean code
 * Iterative  | O(N) | O(H)  | Simple     | Avoid recursion, very easy!
 * Morris     | O(N) | O(1)  | Complex    | O(1) space requirement
 * 
 * NOTE: Preorder iterative is THE EASIEST iterative traversal!
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
 * PREORDER (ROOT → LEFT → RIGHT):  [1, 2, 4, 5, 3] ← ROOT first!
 * INORDER (LEFT → ROOT → RIGHT):   [4, 2, 5, 1, 3] ← ROOT middle
 * POSTORDER (LEFT → RIGHT → ROOT): [4, 5, 2, 3, 1] ← ROOT last
 * 
 * Preorder visits nodes in the order you'd encounter them
 * doing a DFS walk down the left side of the tree.
 * 
 * PATTERN:
 * - First element: Always the root
 * - For BST: NOT sorted (unlike inorder)
 * - Natural for top-down operations
 */

/**
 * APPLICATIONS OF PREORDER TRAVERSAL
 * ===================================
 * 
 * 1. TREE SERIALIZATION
 *    - Save tree structure for later reconstruction
 *    - Preorder makes it easy to rebuild
 * 
 * 2. COPYING A TREE
 *    - Create current node first
 *    - Then copy children
 * 
 * 3. PREFIX EXPRESSION EVALUATION
 *    - Operator comes before operands
 *    - e.g., + * 2 3 4 = (2 * 3) + 4
 * 
 * 4. DFS IN GRAPHS/TREES
 *    - Standard DFS uses preorder logic
 *    - Process node when first visiting
 * 
 * 5. DIRECTORY TRAVERSAL
 *    - Print directory, then its contents
 *    - Natural top-down structure
 * 
 * 6. EXPRESSION TREE PRINTING
 *    - Print operator, then operands
 *    - Gives prefix notation
 */

// Example applications
class PreorderApplications {
    
    /**
     * APPLICATION 1: Serialize tree (save to string)
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
        
        // Preorder: save current, then children
        sb.append(node.val).append(",");
        serializeHelper(node.left, sb);
        serializeHelper(node.right, sb);
    }
    
    /**
     * APPLICATION 2: Deep copy tree
     */
    public TreeNode copyTree(TreeNode root) {
        if (root == null) return null;
        
        // Preorder: create current node first
        TreeNode newNode = new TreeNode(root.val);
        
        // Then copy children
        newNode.left = copyTree(root.left);
        newNode.right = copyTree(root.right);
        
        return newNode;
    }
    
    /**
     * APPLICATION 3: Print directory structure
     */
    public void printDirectory(TreeNode root, int depth) {
        if (root == null) return;
        
        // Preorder: print current directory first
        System.out.println("  ".repeat(depth) + root.val);
        
        // Then print subdirectories
        printDirectory(root.left, depth + 1);
        printDirectory(root.right, depth + 1);
    }
    
    /**
     * APPLICATION 4: Check if tree contains a path sum
     * (Natural for preorder - track sum as we go down)
     */
    public boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) return false;
        
        // Preorder: check current node first
        if (root.left == null && root.right == null) {
            return root.val == targetSum;
        }
        
        // Then check children with reduced sum
        int remaining = targetSum - root.val;
        return hasPathSum(root.left, remaining) || 
               hasPathSum(root.right, remaining);
    }
    
    /**
     * APPLICATION 5: Flatten tree to linked list (preorder)
     * Follow-up problem: Flatten Binary Tree to Linked List
     */
    public void flatten(TreeNode root) {
        if (root == null) return;
        
        // Preorder: process current
        TreeNode left = root.left;
        TreeNode right = root.right;
        
        // Flatten children
        flatten(left);
        flatten(right);
        
        // Reconstruct: root → left subtree → right subtree
        root.left = null;
        root.right = left;
        
        // Find end of left subtree and attach right
        TreeNode curr = root;
        while (curr.right != null) {
            curr = curr.right;
        }
        curr.right = right;
    }
    
    /**
     * APPLICATION 6: Convert to prefix expression string
     */
    public String toPrefix(TreeNode root) {
        if (root == null) return "";
        
        // Preorder: operator first
        String result = root.val + " ";
        result += toPrefix(root.left);
        result += toPrefix(root.right);
        
        return result.trim();
    }
}

/**
 * DFS COMPARISON
 * ==============
 * 
 * DFS (Depth-First Search) in graphs uses preorder logic:
 */
class DFSComparison {
    
    // Standard DFS uses preorder pattern
    public void dfs(TreeNode node, Set<TreeNode> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        
        // PREORDER: Process node when first visiting
        visited.add(node);
        System.out.println("Visiting: " + node.val);
        
        // Then explore neighbors
        dfs(node.left, visited);
        dfs(node.right, visited);
    }
}

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        BinaryTreePreorderTraversalSolutions sol = new BinaryTreePreorderTraversalSolutions();
        
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
        
        System.out.println("Recursive: " + sol.preorderTraversalRecursive(root1));
        System.out.println("Iterative: " + sol.preorderTraversalIterative(root1));
        System.out.println("Morris:    " + sol.preorderTraversalMorris(root1));
        System.out.println("Expected:  [1, 2, 4, 5, 3]");
        
        System.out.println("\n=== Test Case 2: Empty Tree ===");
        System.out.println("Result:   " + sol.preorderTraversalRecursive(null));
        System.out.println("Expected: []");
        
        System.out.println("\n=== Test Case 3: Single Node ===");
        TreeNode root3 = new TreeNode(1);
        System.out.println("Result:   " + sol.preorderTraversalRecursive(root3));
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
        System.out.println("Result:   " + sol.preorderTraversalRecursive(root4));
        System.out.println("Expected: [3, 2, 1]");
        
        System.out.println("\n=== Test Case 5: Right-Skewed Tree ===");
        // 1
        //  \
        //   2
        //    \
        //     3
        TreeNode root5 = new TreeNode(1);
        root5.right = new TreeNode(2);
        root5.right.right = new TreeNode(3);
        System.out.println("Result:   " + sol.preorderTraversalRecursive(root5));
        System.out.println("Expected: [1, 2, 3]");
        
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
        System.out.println("Result:   " + sol.preorderTraversalRecursive(root6));
        System.out.println("Expected: [4, 2, 1, 3, 6, 5, 7]");
        
        System.out.println("\n=== Comparing All Traversal Orders ===");
        System.out.println("Tree structure:");
        System.out.println("       1");
        System.out.println("      / \\");
        System.out.println("     2   3");
        System.out.println("    / \\");
        System.out.println("   4   5");
        System.out.println();
        System.out.println("Preorder:  [1, 2, 4, 5, 3] ← ROOT FIRST (top-down)");
        System.out.println("Inorder:   [4, 2, 5, 1, 3] ← ROOT middle");
        System.out.println("Postorder: [4, 5, 2, 3, 1] ← ROOT last (bottom-up)");
        
        System.out.println("\n=== Demonstrating Applications ===");
        PreorderApplications apps = new PreorderApplications();
        System.out.println("Serialized: " + apps.serialize(root1));
        
        TreeNode copy = apps.copyTree(root1);
        System.out.println("Copy created: " + sol.preorderTraversalRecursive(copy));
        
        System.out.println("\nDirectory structure (depth-first):");
        apps.printDirectory(root1, 0);
        
        System.out.println("\nPrefix expression: " + apps.toPrefix(root1));
        
        System.out.println("\n=== Iterative Simplicity Demo ===");
        System.out.println("Preorder iterative is the SIMPLEST!");
        System.out.println("- Just pop, process, push right, push left");
        System.out.println("- Much simpler than inorder or postorder iterative");
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "Preorder means ROOT → LEFT → RIGHT. We process the parent
 *    before its children, which is natural for top-down operations."
 * 
 * 2. "The recursive solution is straightforward - just visit the
 *    node first, then recurse on children."
 *    [Write recursive solution]
 * 
 * 3. "For iterative, preorder is actually the easiest traversal!
 *    We just pop nodes, process them immediately, and push children."
 *    [Write iterative solution]
 * 
 * 4. "The key trick: Push right child first, then left child.
 *    Since stack is LIFO, this ensures left is processed first."
 * 
 * 5. "Applications include tree serialization, making deep copies,
 *    DFS in graphs, and prefix expression evaluation."
 * 
 * 6. "Time is O(N), space is O(H) for both recursive and iterative.
 *    There's also Morris for O(1) space if needed."
 * 
 * 7. "Compared to other traversals: preorder is simplest iteratively,
 *    inorder is medium, and postorder is hardest."
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Why push right before left in iterative? [LIFO property]
 * - How does this relate to DFS? [DFS uses preorder logic]
 * - Can you serialize and deserialize a tree? [Use preorder]
 * - How would you copy a tree? [Preorder: create node, copy children]
 * - What's the difference from inorder? [When we process node]
 * - Can you do it with O(1) space? [Morris traversal]
 */

class BinaryTreePreorderTraversal {

    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        this.preorderTraversal(root, result);
        return result;
    }

    private void preorderTraversal(TreeNode root, List<Integer> result) {
        if (root == null)
            return;
        result.add(root.val);
        this.preorderTraversal(root.left, result);
        this.preorderTraversal(root.right, result);
    }
}

class BinaryTreePreorderIterativeTraversal {

    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null)
            return result;

        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode current = stack.pop();
            result.add(current.val);
            if (current.right != null) {
                stack.push(current.right);
            }
            if (current.left != null) {
                stack.push(current.left);
            }
        }
        return result;
    }

}

class BinaryTreePreorderTraversalDequeSolution {

    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null)
            return result;

        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            result.add(node.val);

            if (node.right != null)
                stack.push(node.right);
            if (node.left != null)
                stack.push(node.left);
        }

        return result;
    }

}

class BinaryTreePreorderTraversalMorrisSolution {

    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        TreeNode current = root;

        while (current != null) {
            if (current.left == null) {
                result.add(current.val); // Visit root
                current = current.right;
            } else {
                TreeNode predecessor = current.left;
                while (predecessor.right != null && predecessor.right != current) {
                    predecessor = predecessor.right;
                }

                if (predecessor.right == null) {
                    result.add(current.val); // Visit before going left
                    predecessor.right = current;
                    current = current.left;
                } else {
                    predecessor.right = null;
                    current = current.right;
                }
            }
        }
        return result;
    }

}

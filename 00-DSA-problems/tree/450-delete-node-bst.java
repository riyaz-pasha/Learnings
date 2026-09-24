/**
 * BST Node Deletion - Complete Implementation with Analysis
 * 
 * PROBLEM UNDERSTANDING:
 * Given a Binary Search Tree and a key, delete the node with that key while maintaining BST properties.
 * BST Property: For any node N, all values in left subtree < N.val < all values in right subtree
 * 
 * KEY INSIGHT:
 * The challenge isn't finding the node (simple BST search), but maintaining BST properties after deletion.
 * Different cases require different handling strategies.
 * 
 * TIME COMPLEXITY: O(h) where h is height of tree
 * - Average case: O(log n) for balanced BST
 * - Worst case: O(n) for skewed tree
 * SPACE COMPLEXITY: O(h) due to recursion stack
 */

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode(int val) {
        this.val = val;
    }
}

class BSTNodeDeletion {
    
    /**
     * APPROACH 1: RECURSIVE SOLUTION (MOST ELEGANT)
     * 
     * INTERVIEW APPROACH:
     * 1. Start by identifying the 3 deletion cases
     * 2. Draw them out on whiteboard
     * 3. Explain the recursive structure
     * 4. Code while explaining edge cases
     * 
     * CORE STRATEGY:
     * - Use recursion to navigate to the target node
     * - Return the replacement node at each level
     * - Parent automatically reconnects to the replacement
     */
    public TreeNode deleteNode(TreeNode root, int key) {
        // BASE CASE: Empty tree or node not found
        if (root == null) {
            return null;
        }
        
        // PHASE 1: SEARCH - Navigate to the target node
        if (key < root.val) {
            // Key is in left subtree
            // Recursively delete from left, then reconnect
            root.left = deleteNode(root.left, key);
        } else if (key > root.val) {
            // Key is in right subtree
            // Recursively delete from right, then reconnect
            root.right = deleteNode(root.right, key);
        } else {
            // PHASE 2: DELETE - Found the node to delete
            // Now handle the 3 critical cases
            
            /**
             * CASE 1: Node has no left child (or is leaf)
             * Solution: Replace with right child (could be null)
             * 
             * Example:    5           5
             *            / \    →      \
             *           3   7           7
             * Delete 3: Simply return node 7 to replace 3
             */
            if (root.left == null) {
                return root.right;
            }
            
            /**
             * CASE 2: Node has no right child
             * Solution: Replace with left child
             * 
             * Example:    5           3
             *            / \    →    /
             *           3   7       1
             *          /
             *         1
             * Delete 5: Return node 3 to replace 5
             */
            if (root.right == null) {
                return root.left;
            }
            
            /**
             * CASE 3: Node has both children (MOST COMPLEX)
             * Solution: Replace with inorder successor or predecessor
             * 
             * WHY SUCCESSOR/PREDECESSOR?
             * - They maintain BST property when moved
             * - Successor: smallest node in right subtree (leftmost)
             * - Predecessor: largest node in left subtree (rightmost)
             * 
             * Example using SUCCESSOR:
             *        5                6
             *       / \              / \
             *      3   7      →     3   7
             *         / \              / \
             *        6   9            *   9
             * Delete 5: Replace with 6 (successor)
             * 
             * STRATEGY:
             * 1. Find inorder successor (minimum in right subtree)
             * 2. Copy successor's value to current node
             * 3. Delete the successor from right subtree
             */
            
            // Find minimum value in right subtree (inorder successor)
            TreeNode successor = findMin(root.right);
            
            // Replace current node's value with successor's value
            root.val = successor.val;
            
            // Delete the successor from right subtree
            // This recursive call will hit Case 1 or 2 (successor has no left child)
            root.right = deleteNode(root.right, successor.val);
        }
        
        return root;
    }
    
    /**
     * Helper: Find minimum node (leftmost) in a subtree
     * Used to find inorder successor
     */
    private TreeNode findMin(TreeNode node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }
    
    
    /**
     * APPROACH 2: ITERATIVE SOLUTION (MORE COMPLEX, BUT O(1) SPACE)
     * 
     * WHEN TO USE:
     * - Interviewer asks for iterative solution
     * - Need to optimize space complexity
     * - Want to demonstrate advanced understanding
     * 
     * CHALLENGES:
     * - Must manually track parent node
     * - More complex pointer manipulation
     * - Harder to reason about edge cases
     */
    public TreeNode deleteNodeIterative(TreeNode root, int key) {
        TreeNode parent = null;
        TreeNode current = root;
        
        // PHASE 1: Find the node and its parent
        while (current != null && current.val != key) {
            parent = current;
            if (key < current.val) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        
        // Node not found
        if (current == null) {
            return root;
        }
        
        // PHASE 2: Delete the node
        TreeNode replacement = null;
        
        // CASE 1: No left child
        if (current.left == null) {
            replacement = current.right;
        }
        // CASE 2: No right child
        else if (current.right == null) {
            replacement = current.left;
        }
        // CASE 3: Both children exist
        else {
            // Find successor and its parent
            TreeNode successorParent = current;
            TreeNode successor = current.right;
            
            // Find leftmost node in right subtree
            while (successor.left != null) {
                successorParent = successor;
                successor = successor.left;
            }
            
            // Copy successor value
            current.val = successor.val;
            
            // Remove successor node
            // CRITICAL: Check if successor is immediate right child
            if (successorParent == current) {
                // Successor is current's immediate right child
                successorParent.right = successor.right;
            } else {
                // Successor is deeper in the tree
                successorParent.left = successor.right;
            }
            
            // No replacement needed, we modified current node's value
            return root;
        }
        
        // PHASE 3: Connect parent to replacement
        if (parent == null) {
            // Deleting root node
            return replacement;
        } else if (parent.left == current) {
            parent.left = replacement;
        } else {
            parent.right = replacement;
        }
        
        return root;
    }
    
    
    /**
     * APPROACH 3: USING PREDECESSOR INSTEAD OF SUCCESSOR
     * 
     * TRADE-OFF ANALYSIS:
     * - Both successor and predecessor maintain BST property
     * - Successor: minimum of right subtree
     * - Predecessor: maximum of left subtree
     * - Choice doesn't affect correctness, only tree balance
     * 
     * WHEN TO USE PREDECESSOR:
     * - To balance deletion operations
     * - Alternate between successor/predecessor for better balance
     */
    public TreeNode deleteNodeWithPredecessor(TreeNode root, int key) {
        if (root == null) {
            return null;
        }
        
        if (key < root.val) {
            root.left = deleteNodeWithPredecessor(root.left, key);
        } else if (key > root.val) {
            root.right = deleteNodeWithPredecessor(root.right, key);
        } else {
            // Node found
            if (root.left == null) {
                return root.right;
            }
            if (root.right == null) {
                return root.left;
            }
            
            // Use PREDECESSOR instead of successor
            TreeNode predecessor = findMax(root.left);
            root.val = predecessor.val;
            root.left = deleteNodeWithPredecessor(root.left, predecessor.val);
        }
        
        return root;
    }
    
    /**
     * Helper: Find maximum node (rightmost) in a subtree
     * Used to find inorder predecessor
     */
    private TreeNode findMax(TreeNode node) {
        while (node.right != null) {
            node = node.right;
        }
        return node;
    }
    
    
    /**
     * ==================== INTERVIEW STRATEGY GUIDE ====================
     * 
     * STEP 1: CLARIFY REQUIREMENTS (2 minutes)
     * Questions to ask:
     * - Can the key be absent from the tree?
     * - Are there duplicate values?
     * - Should I optimize for time or space?
     * - Is the tree balanced or can it be skewed?
     * 
     * STEP 2: EXPLAIN THE 3 CASES (3 minutes)
     * Draw on whiteboard:
     * 1. Node with no children (leaf) - simply remove
     * 2. Node with one child - replace with that child
     * 3. Node with two children - replace with successor/predecessor
     * 
     * STEP 3: CHOOSE APPROACH (1 minute)
     * - Recursive: More elegant, easier to explain
     * - Iterative: Better space complexity, more impressive
     * Recommend: Start with recursive, mention iterative if time permits
     * 
     * STEP 4: CODE WITH COMMENTARY (10 minutes)
     * - Explain as you code
     * - Handle edge cases explicitly
     * - Walk through an example
     * 
     * STEP 5: TEST YOUR SOLUTION (4 minutes)
     * Test cases:
     * 1. Empty tree
     * 2. Single node tree
     * 3. Delete leaf node
     * 4. Delete node with one child
     * 5. Delete node with two children
     * 6. Delete root node
     * 
     * COMMON MISTAKES TO AVOID:
     * ❌ Forgetting to reconnect parent after deletion
     * ❌ Not handling null cases
     * ❌ Incorrect successor finding (not going left enough)
     * ❌ Memory leaks in languages requiring manual memory management
     * ❌ Modifying tree while traversing in Case 3
     */
    
    
    /**
     * ==================== COMPLEXITY ANALYSIS ====================
     * 
     * TIME COMPLEXITY:
     * - Best Case: O(log n) - balanced tree, finding node near root
     * - Average Case: O(log n) - balanced BST
     * - Worst Case: O(n) - skewed tree (like linked list)
     * 
     * Why O(h) where h is height?
     * - Search phase: O(h) to find node
     * - Delete phase: O(h) to find successor (worst case)
     * - Total: O(h) + O(h) = O(h)
     * 
     * SPACE COMPLEXITY:
     * - Recursive: O(h) for call stack
     * - Iterative: O(1) only using pointers
     * 
     * OPERATIONS BREAKDOWN:
     * 1. Finding node: O(h)
     * 2. Finding successor: O(h) worst case
     * 3. Deletion: O(1)
     * 4. Reconnection: O(1)
     */
    
    
    /**
     * ==================== VISUAL WALKTHROUGH ====================
     * 
     * Example: Delete node 5 from this BST
     * 
     *         8
     *        / \
     *       3   10
     *      / \    \
     *     1   6    14
     *        / \   /
     *       4   7 13
     *      /
     *     5
     * 
     * Step 1: Navigate to node 5
     * Path: 8 → 3 → 6 → 4 (5 is left child of 4)
     * 
     * Step 2: Identify case
     * Node 5 is a leaf (no children) → CASE 1
     * 
     * Step 3: Delete
     * Return null to parent (node 4)
     * 
     * Result:
     *         8
     *        / \
     *       3   10
     *      / \    \
     *     1   6    14
     *        / \   /
     *       4   7 13
     * 
     * ----------------------------------------
     * 
     * Example 2: Delete node 3 (has both children)
     * 
     *         8
     *        / \
     *       3   10      Step 1: Find node 3
     *      / \    \     Step 2: Has both children → CASE 3
     *     1   6    14   Step 3: Find successor = 4 (min in right subtree)
     *        / \   /    Step 4: Replace 3's value with 4
     *       4   7 13    Step 5: Delete original 4 from right subtree
     *      /
     *     5
     * 
     * Result:
     *         8
     *        / \
     *       4   10      Node 3 replaced by successor 4
     *      / \    \     Original 4's position taken by its right child 5
     *     1   6    14
     *        / \   /
     *       5   7 13
     */
    
    
    /**
     * ==================== ALTERNATIVE OPTIMIZATIONS ====================
     */
    
    /**
     * OPTIMIZATION 1: Balanced deletion
     * Alternate between successor and predecessor to maintain better balance
     */
    public TreeNode deleteNodeBalanced(TreeNode root, int key) {
        return deleteHelper(root, key, true); // true = use successor first
    }
    
    private TreeNode deleteHelper(TreeNode root, int key, boolean useSuccessor) {
        if (root == null) return null;
        
        if (key < root.val) {
            root.left = deleteHelper(root.left, key, useSuccessor);
        } else if (key > root.val) {
            root.right = deleteHelper(root.right, key, useSuccessor);
        } else {
            if (root.left == null) return root.right;
            if (root.right == null) return root.left;
            
            // Alternate between successor and predecessor
            if (useSuccessor) {
                TreeNode successor = findMin(root.right);
                root.val = successor.val;
                root.right = deleteHelper(root.right, successor.val, !useSuccessor);
            } else {
                TreeNode predecessor = findMax(root.left);
                root.val = predecessor.val;
                root.left = deleteHelper(root.left, predecessor.val, !useSuccessor);
            }
        }
        return root;
    }
    
    
    /**
     * ==================== UTILITY METHODS FOR TESTING ====================
     */
    
    // Build sample BST for testing
    public TreeNode buildSampleTree() {
        TreeNode root = new TreeNode(5);
        root.left = new TreeNode(3);
        root.right = new TreeNode(6);
        root.left.left = new TreeNode(2);
        root.left.right = new TreeNode(4);
        root.right.right = new TreeNode(7);
        return root;
    }
    
    // Inorder traversal to verify BST property
    public void inorderTraversal(TreeNode root) {
        if (root == null) return;
        inorderTraversal(root.left);
        System.out.print(root.val + " ");
        inorderTraversal(root.right);
    }
    
    // Validate BST property
    public boolean isValidBST(TreeNode root) {
        return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    private boolean validate(TreeNode node, long min, long max) {
        if (node == null) return true;
        if (node.val <= min || node.val >= max) return false;
        return validate(node.left, min, node.val) && 
               validate(node.right, node.val, max);
    }
    
    
    /**
     * ==================== TEST CASES ====================
     */
    public static void main(String[] args) {
        BSTNodeDeletion solution = new BSTNodeDeletion();
        
        System.out.println("Test Case 1: Delete leaf node");
        TreeNode root1 = solution.buildSampleTree();
        System.out.print("Before: ");
        solution.inorderTraversal(root1);
        System.out.println();
        root1 = solution.deleteNode(root1, 2);
        System.out.print("After deleting 2: ");
        solution.inorderTraversal(root1);
        System.out.println("\nValid BST: " + solution.isValidBST(root1));
        
        System.out.println("\n\nTest Case 2: Delete node with one child");
        TreeNode root2 = solution.buildSampleTree();
        root2 = solution.deleteNode(root2, 6);
        System.out.print("After deleting 6: ");
        solution.inorderTraversal(root2);
        System.out.println("\nValid BST: " + solution.isValidBST(root2));
        
        System.out.println("\n\nTest Case 3: Delete node with two children");
        TreeNode root3 = solution.buildSampleTree();
        root3 = solution.deleteNode(root3, 3);
        System.out.print("After deleting 3: ");
        solution.inorderTraversal(root3);
        System.out.println("\nValid BST: " + solution.isValidBST(root3));
        
        System.out.println("\n\nTest Case 4: Delete root node");
        TreeNode root4 = solution.buildSampleTree();
        root4 = solution.deleteNode(root4, 5);
        System.out.print("After deleting root 5: ");
        solution.inorderTraversal(root4);
        System.out.println("\nValid BST: " + solution.isValidBST(root4));
        
        System.out.println("\n\nTest Case 5: Delete non-existent node");
        TreeNode root5 = solution.buildSampleTree();
        root5 = solution.deleteNode(root5, 10);
        System.out.print("After trying to delete 10: ");
        solution.inorderTraversal(root5);
        System.out.println("\nValid BST: " + solution.isValidBST(root5));
    }
}

/**
 * ==================== KEY TAKEAWAYS ====================
 * 
 * 1. MASTER THE 3 CASES - This is the core of the problem
 * 
 * 2. RECURSIVE IS ELEGANT - Easier to understand and code
 * 
 * 3. UNDERSTAND SUCCESSOR/PREDECESSOR - Both work, but understand why
 * 
 * 4. PRACTICE EDGE CASES - Empty tree, single node, root deletion
 * 
 * 5. EXPLAIN AS YOU CODE - Show your thought process
 * 
 * 6. VALIDATE YOUR SOLUTION - Walk through examples
 * 
 * 7. KNOW THE COMPLEXITY - O(h) time, O(h) or O(1) space
 * 
 * RELATED PROBLEMS TO PRACTICE:
 * - Insert into BST
 * - Search in BST
 * - Validate BST
 * - Lowest Common Ancestor in BST
 * - Inorder Successor in BST
 * - Convert Sorted Array to BST
 * 
 * VARIATIONS TO CONSIDER:
 * - What if duplicate values are allowed?
 * - How would you delete all nodes with a given value?
 * - Can you implement lazy deletion (mark as deleted)?
 * - How would this work with a self-balancing tree (AVL, Red-Black)?
 */

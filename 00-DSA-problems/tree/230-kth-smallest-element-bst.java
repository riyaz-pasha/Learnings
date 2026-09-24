/**
 * Kth Smallest Element in BST - Complete Implementation with Analysis
 * 
 * PROBLEM UNDERSTANDING:
 * Given a BST and integer k, return the kth smallest value (1-indexed).
 * 
 * KEY INSIGHT:
 * BST's inorder traversal produces values in SORTED ORDER!
 * This is the fundamental property that makes this problem elegant.
 * 
 * Example BST:
 *       5
 *      / \
 *     3   6
 *    / \
 *   2   4
 *  /
 * 1
 * 
 * Inorder: 1, 2, 3, 4, 5, 6 (sorted!)
 * k=3 → answer is 3
 * 
 * TIME COMPLEXITY COMPARISON:
 * - Approach 1 (Inorder): O(n) time, O(n) space
 * - Approach 2 (Early Stop): O(k) time, O(h) space ← OPTIMAL
 * - Approach 3 (Iterative): O(k) time, O(h) space
 * - Approach 4 (Morris): O(n) time, O(1) space ← SPACE OPTIMAL
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode(int val) {
        this.val = val;
    }
}

class KthSmallestBST {
    
    /**
     * APPROACH 1: STANDARD INORDER TRAVERSAL (EASIEST TO UNDERSTAND)
     * 
     * STRATEGY:
     * 1. Perform complete inorder traversal
     * 2. Store all values in array/list
     * 3. Return kth element (index k-1)
     * 
     * PROS:
     * - Simple and intuitive
     * - Easy to code in interview
     * - Works for finding multiple kth elements
     * 
     * CONS:
     * - Uses O(n) space even if k is small
     * - Visits all nodes even if k=1
     * 
     * TIME: O(n) - visit all nodes
     * SPACE: O(n) - store all values + O(h) recursion stack
     */
    public int kthSmallest_Standard(TreeNode root, int k) {
        List<Integer> values = new ArrayList<>();
        inorderTraversal(root, values);
        
        // k is 1-indexed, array is 0-indexed
        return values.get(k - 1);
    }
    
    private void inorderTraversal(TreeNode node, List<Integer> values) {
        if (node == null) {
            return;
        }
        
        // Inorder: LEFT → ROOT → RIGHT
        inorderTraversal(node.left, values);
        values.add(node.val);  // Process current node
        inorderTraversal(node.right, values);
    }
    
    
    /**
     * APPROACH 2: OPTIMIZED INORDER WITH EARLY STOPPING (RECOMMENDED FOR INTERVIEWS)
     * 
     * STRATEGY:
     * - Use counter to track how many nodes we've visited
     * - Stop as soon as we reach the kth node
     * - No need to visit remaining nodes!
     * 
     * CRITICAL INSIGHT:
     * Since inorder gives sorted order, the kth node visited is the answer.
     * We don't need to see all nodes!
     * 
     * INTERVIEW TIP:
     * This is the solution interviewers want to see - shows optimization thinking.
     * 
     * TIME: O(h + k) where h is height
     * - Best case: O(k) when tree is balanced
     * - Worst case: O(n) when k = n or tree is skewed
     * SPACE: O(h) for recursion stack only
     */
    
    private int count = 0;  // Tracks how many nodes we've visited
    private int result = 0; // Stores the answer when we find it
    
    public int kthSmallest(TreeNode root, int k) {
        // Reset for multiple calls
        count = 0;
        result = 0;
        
        inorderWithCounter(root, k);
        return result;
    }
    
    private void inorderWithCounter(TreeNode node, int k) {
        if (node == null) {
            return;
        }
        
        // LEFT: Go to smallest values first
        inorderWithCounter(node.left, k);
        
        // ROOT: Process current node
        count++;  // Increment counter
        
        if (count == k) {
            // Found the kth smallest!
            result = node.val;
            return;  // Early termination - no need to continue
        }
        
        // RIGHT: Only visit if we haven't found answer yet
        // OPTIMIZATION: Could add if (count < k) here
        inorderWithCounter(node.right, k);
    }
    
    
    /**
     * APPROACH 3: ITERATIVE INORDER (NO RECURSION)
     * 
     * WHEN TO USE:
     * - Interviewer asks for iterative solution
     * - Want to avoid recursion stack overflow for very deep trees
     * - Need explicit control over traversal
     * 
     * STRATEGY:
     * - Use stack to simulate recursion
     * - Process nodes in inorder manner
     * - Stop when we reach kth node
     * 
     * TIME: O(h + k)
     * SPACE: O(h) for explicit stack
     */
    public int kthSmallest_Iterative(TreeNode root, int k) {
        Stack<TreeNode> stack = new Stack<>();
        TreeNode current = root;
        int count = 0;
        
        /**
         * ITERATIVE INORDER PATTERN (MEMORIZE THIS):
         * 1. Go as far left as possible, pushing nodes onto stack
         * 2. Pop a node, process it, move to its right child
         * 3. Repeat until stack is empty and current is null
         */
        while (current != null || !stack.isEmpty()) {
            
            // PHASE 1: Go left, pushing all nodes onto stack
            while (current != null) {
                stack.push(current);
                current = current.left;
            }
            
            // PHASE 2: Process node (this is the "inorder" visit)
            current = stack.pop();
            count++;
            
            // Check if this is the kth node
            if (count == k) {
                return current.val;
            }
            
            // PHASE 3: Move to right subtree
            current = current.right;
        }
        
        // Should never reach here if k is valid
        return -1;
    }
    
    
    /**
     * APPROACH 4: MORRIS TRAVERSAL (MOST ADVANCED - O(1) SPACE)
     * 
     * WHEN TO MENTION:
     * - After solving with standard approach
     * - Interviewer asks about space optimization
     * - Want to show advanced knowledge
     * 
     * STRATEGY:
     * - Modify tree structure temporarily to create "threaded" links
     * - Use these threads to traverse without stack/recursion
     * - Restore original tree structure as we go
     * 
     * HOW IT WORKS:
     * For each node, create temporary link from its inorder predecessor to itself.
     * This allows us to "return" to the node after visiting left subtree.
     * 
     * TIME: O(n) - each edge traversed at most twice
     * SPACE: O(1) - no stack, no recursion, just pointers!
     * 
     * TRADE-OFF:
     * - Most space-efficient
     * - More complex to code
     * - Temporarily modifies tree (but restores it)
     */
    public int kthSmallest_Morris(TreeNode root, int k) {
        int count = 0;
        TreeNode current = root;
        
        while (current != null) {
            
            if (current.left == null) {
                // No left subtree: process current node
                count++;
                if (count == k) {
                    return current.val;
                }
                // Move to right
                current = current.right;
                
            } else {
                // Find inorder predecessor (rightmost node in left subtree)
                TreeNode predecessor = current.left;
                
                // Go to rightmost node, but stop if we find the thread
                while (predecessor.right != null && predecessor.right != current) {
                    predecessor = predecessor.right;
                }
                
                if (predecessor.right == null) {
                    // Create thread: predecessor → current
                    predecessor.right = current;
                    current = current.left;
                    
                } else {
                    // Thread exists: we've finished left subtree
                    // Remove thread and process current
                    predecessor.right = null;  // Restore original structure
                    count++;
                    if (count == k) {
                        return current.val;
                    }
                    current = current.right;
                }
            }
        }
        
        return -1;
    }
    
    
    /**
     * APPROACH 5: WITH AUGMENTED BST (FOLLOW-UP OPTIMIZATION)
     * 
     * SCENARIO:
     * "What if we need to find kth smallest element multiple times?"
     * 
     * SOLUTION:
     * Augment each node with size of its subtree.
     * Then we can find kth element in O(h) time!
     * 
     * NODE STRUCTURE:
     * class AugmentedNode {
     *     int val;
     *     int leftSize;  // Number of nodes in left subtree
     *     AugmentedNode left, right;
     * }
     * 
     * ALGORITHM:
     * if (k <= leftSize) → answer is in left subtree
     * if (k == leftSize + 1) → current node is the answer
     * if (k > leftSize + 1) → answer is (k - leftSize - 1)th in right subtree
     * 
     * TIME: O(h) per query after O(n) preprocessing
     * SPACE: O(n) for size information
     * 
     * WHEN TO USE:
     * - Multiple queries on same tree
     * - Tree doesn't change frequently
     */
    
    // Augmented node class
    static class AugmentedNode {
        int val;
        int leftSize;  // Count of nodes in left subtree
        AugmentedNode left, right;
        
        AugmentedNode(int val) {
            this.val = val;
            this.leftSize = 0;
        }
    }
    
    public int kthSmallest_Augmented(AugmentedNode root, int k) {
        if (root == null) {
            return -1;
        }
        
        int leftSize = root.leftSize;
        
        if (k <= leftSize) {
            // Answer is in left subtree
            return kthSmallest_Augmented(root.left, k);
            
        } else if (k == leftSize + 1) {
            // Current node is the kth smallest
            return root.val;
            
        } else {
            // Answer is in right subtree
            // Adjust k: we've "used up" leftSize + 1 smallest elements
            return kthSmallest_Augmented(root.right, k - leftSize - 1);
        }
    }
    
    // Helper: Build augmented tree (preprocessing)
    private int buildAugmented(TreeNode node, AugmentedNode augNode) {
        if (node == null) {
            return 0;
        }
        
        // Recursively build and get sizes
        int leftCount = buildAugmented(node.left, augNode.left);
        int rightCount = buildAugmented(node.right, augNode.right);
        
        augNode.leftSize = leftCount;
        return leftCount + rightCount + 1;  // Total nodes in this subtree
    }
    
    
    /**
     * ==================== COMPLEXITY COMPARISON ====================
     * 
     * | Approach        | Time       | Space      | When to Use                    |
     * |-----------------|------------|------------|--------------------------------|
     * | Standard        | O(n)       | O(n)       | Quick solution, small trees    |
     * | Early Stop      | O(h + k)   | O(h)       | **BEST for interviews**        |
     * | Iterative       | O(h + k)   | O(h)       | No recursion needed            |
     * | Morris          | O(n)       | O(1)       | Space optimization required    |
     * | Augmented       | O(h)       | O(n)       | Multiple queries on same tree  |
     * 
     * RECOMMENDATION FOR INTERVIEWS:
     * 1. Start with Approach 2 (Early Stop) - optimal and clean
     * 2. Mention Approach 3 (Iterative) if they want no recursion
     * 3. Discuss Approach 5 (Augmented) for follow-up optimization
     * 4. Only mention Morris if they specifically ask for O(1) space
     */
    
    
    /**
     * ==================== INTERVIEW STRATEGY GUIDE ====================
     * 
     * STEP 1: CLARIFY (1-2 minutes)
     * Questions to ask:
     * - Is k guaranteed to be valid (1 ≤ k ≤ n)?
     * - Can the tree be empty?
     * - Will there be multiple queries?
     * - Any constraints on tree size or height?
     * 
     * STEP 2: EXPLAIN KEY INSIGHT (2 minutes)
     * "The crucial insight is that inorder traversal of a BST gives us elements
     * in sorted order. So the kth node we visit during inorder traversal is our answer."
     * 
     * Draw example:
     *     3
     *    / \
     *   1   4
     *    \
     *     2
     * 
     * Inorder: 1, 2, 3, 4
     * k=2 → answer is 2
     * 
     * STEP 3: PROPOSE SOLUTION (1 minute)
     * "I'll use recursive inorder traversal with a counter.
     * When counter reaches k, that's our answer. This gives us O(h + k) time
     * and O(h) space, which is optimal for a single query."
     * 
     * STEP 4: CODE (8-10 minutes)
     * Write Approach 2 (Early Stop)
     * Explain as you code
     * 
     * STEP 5: TEST (3-4 minutes)
     * Test cases:
     * 1. k = 1 (smallest element - leftmost)
     * 2. k = n (largest element - rightmost)
     * 3. k in middle
     * 4. Single node tree
     * 5. Skewed tree
     * 
     * STEP 6: OPTIMIZE & FOLLOW-UPS (remaining time)
     * - "For multiple queries, we could augment each node with subtree size"
     * - "We could also use iterative approach to avoid recursion overhead"
     * - "For extreme space constraints, Morris traversal gives O(1) space"
     */
    
    
    /**
     * ==================== VISUAL WALKTHROUGH ====================
     * 
     * Example: Find 3rd smallest in this BST
     * 
     *         5
     *        / \
     *       3   7
     *      / \   \
     *     2   4   8
     *    /
     *   1
     * 
     * Inorder Traversal Path:
     * 
     * Step 1: Start at root (5), go left
     *         5
     *        /
     *       3
     *      /
     *     2
     *    /
     *   1 ← Visit node 1 (count = 1)
     * 
     * Step 2: Backtrack to 2
     *   1
     *    \
     *     2 ← Visit node 2 (count = 2)
     * 
     * Step 3: No right child, backtrack to 3
     *     2
     *      \
     *       3 ← Visit node 3 (count = 3) ✓ ANSWER!
     * 
     * We stop here! No need to visit 4, 5, 7, 8
     * 
     * Full inorder would be: 1, 2, 3, 4, 5, 7, 8
     * But we only visited: 1, 2, 3 (much more efficient!)
     */
    
    
    /**
     * ==================== COMMON MISTAKES & PITFALLS ====================
     */
    
    // ❌ MISTAKE 1: Off-by-one error with k being 1-indexed
    // WRONG:
    public int kthSmallest_Wrong1(TreeNode root, int k) {
        List<Integer> values = new ArrayList<>();
        inorderTraversal(root, values);
        return values.get(k);  // ❌ Should be k-1!
    }
    
    // ❌ MISTAKE 2: Not resetting global variables
    private int globalCount = 0;
    public int kthSmallest_Wrong2(TreeNode root, int k) {
        // ❌ If called multiple times, globalCount won't reset!
        // Always reset in main method
        globalCount = 0;  // ✓ Correct
        // ... rest of code
        return 0;
    }
    
    // ❌ MISTAKE 3: Continuing traversal after finding answer
    private void inorder_Wrong(TreeNode node, int k, int[] count, int[] result) {
        if (node == null) return;
        
        inorder_Wrong(node.left, k, count, result);
        count[0]++;
        if (count[0] == k) {
            result[0] = node.val;
            // ❌ Should return here to avoid unnecessary traversal!
        }
        inorder_Wrong(node.right, k, count, result);
    }
    
    // ❌ MISTAKE 4: Using preorder or postorder instead of inorder
    // Only INORDER gives sorted values in BST!
    
    
    /**
     * ==================== FOLLOW-UP QUESTIONS ====================
     */
    
    /**
     * Q1: "What if k can be larger than the number of nodes?"
     * A: Add validation to check if k is valid
     */
    public int kthSmallest_Validated(TreeNode root, int k) {
        int treeSize = countNodes(root);
        if (k < 1 || k > treeSize) {
            throw new IllegalArgumentException("k is out of valid range");
        }
        return kthSmallest(root, k);
    }
    
    private int countNodes(TreeNode node) {
        if (node == null) return 0;
        return 1 + countNodes(node.left) + countNodes(node.right);
    }
    
    
    /**
     * Q2: "How would you find the kth largest instead?"
     * A: Reverse inorder traversal (RIGHT → ROOT → LEFT)
     */
    public int kthLargest(TreeNode root, int k) {
        count = 0;
        result = 0;
        reverseInorder(root, k);
        return result;
    }
    
    private void reverseInorder(TreeNode node, int k) {
        if (node == null) return;
        
        reverseInorder(node.right, k);  // RIGHT first (larger values)
        count++;
        if (count == k) {
            result = node.val;
            return;
        }
        reverseInorder(node.left, k);   // LEFT last (smaller values)
    }
    
    
    /**
     * Q3: "What if the BST is modified frequently?"
     * A: Use augmented BST with subtree sizes.
     *    Update sizes during insert/delete operations.
     *    Each query then takes O(h) time.
     */
    
    
    /**
     * Q4: "Find kth smallest in a range [low, high]"
     * A: Modify inorder to skip nodes outside range
     */
    public int kthSmallestInRange(TreeNode root, int k, int low, int high) {
        List<Integer> inRange = new ArrayList<>();
        inorderInRange(root, low, high, inRange);
        return k <= inRange.size() ? inRange.get(k - 1) : -1;
    }
    
    private void inorderInRange(TreeNode node, int low, int high, List<Integer> result) {
        if (node == null) return;
        
        if (node.val > low) {
            inorderInRange(node.left, low, high, result);
        }
        if (node.val >= low && node.val <= high) {
            result.add(node.val);
        }
        if (node.val < high) {
            inorderInRange(node.right, low, high, result);
        }
    }
    
    
    /**
     * ==================== UTILITY METHODS ====================
     */
    
    // Build sample BST for testing
    public TreeNode buildSampleTree() {
        /*
         *         5
         *        / \
         *       3   6
         *      / \
         *     2   4
         *    /
         *   1
         */
        TreeNode root = new TreeNode(5);
        root.left = new TreeNode(3);
        root.right = new TreeNode(6);
        root.left.left = new TreeNode(2);
        root.left.right = new TreeNode(4);
        root.left.left.left = new TreeNode(1);
        return root;
    }
    
    // Verify answer by comparing all approaches
    public void verifyAllApproaches(TreeNode root, int k) {
        int result1 = kthSmallest_Standard(root, k);
        int result2 = kthSmallest(root, k);
        int result3 = kthSmallest_Iterative(root, k);
        int result4 = kthSmallest_Morris(root, k);
        
        System.out.println("Standard:  " + result1);
        System.out.println("Optimized: " + result2);
        System.out.println("Iterative: " + result3);
        System.out.println("Morris:    " + result4);
        
        if (result1 == result2 && result2 == result3 && result3 == result4) {
            System.out.println("✓ All approaches agree!");
        } else {
            System.out.println("✗ Results differ - check implementation!");
        }
    }
    
    
    /**
     * ==================== TEST CASES ====================
     */
    public static void main(String[] args) {
        KthSmallestBST solution = new KthSmallestBST();
        
        System.out.println("=== Test Case 1: Standard BST ===");
        TreeNode root1 = solution.buildSampleTree();
        System.out.println("Tree structure:");
        System.out.println("      5");
        System.out.println("     / \\");
        System.out.println("    3   6");
        System.out.println("   / \\");
        System.out.println("  2   4");
        System.out.println(" /");
        System.out.println("1");
        System.out.println("\nInorder: 1, 2, 3, 4, 5, 6");
        
        for (int k = 1; k <= 6; k++) {
            System.out.println("\nk = " + k + ":");
            solution.verifyAllApproaches(root1, k);
        }
        
        System.out.println("\n=== Test Case 2: Skewed Tree (Left) ===");
        TreeNode root2 = new TreeNode(5);
        root2.left = new TreeNode(4);
        root2.left.left = new TreeNode(3);
        root2.left.left.left = new TreeNode(2);
        root2.left.left.left.left = new TreeNode(1);
        System.out.println("Tree: 5 → 4 → 3 → 2 → 1");
        System.out.println("k = 1 (should be 1):");
        solution.verifyAllApproaches(root2, 1);
        System.out.println("\nk = 5 (should be 5):");
        solution.verifyAllApproaches(root2, 5);
        
        System.out.println("\n=== Test Case 3: Single Node ===");
        TreeNode root3 = new TreeNode(42);
        System.out.println("Tree: [42]");
        System.out.println("k = 1 (should be 42):");
        solution.verifyAllApproaches(root3, 1);
        
        System.out.println("\n=== Test Case 4: Perfect Binary Tree ===");
        TreeNode root4 = new TreeNode(4);
        root4.left = new TreeNode(2);
        root4.right = new TreeNode(6);
        root4.left.left = new TreeNode(1);
        root4.left.right = new TreeNode(3);
        root4.right.left = new TreeNode(5);
        root4.right.right = new TreeNode(7);
        System.out.println("Tree:");
        System.out.println("       4");
        System.out.println("      / \\");
        System.out.println("     2   6");
        System.out.println("    / \\ / \\");
        System.out.println("   1  3 5  7");
        System.out.println("\nk = 4 (should be 4 - middle element):");
        solution.verifyAllApproaches(root4, 4);
        
        System.out.println("\n=== Test Case 5: Kth Largest ===");
        System.out.println("Using tree from Test Case 1");
        System.out.println("2nd largest (should be 5):");
        System.out.println("Result: " + solution.kthLargest(root1, 2));
    }
}

/**
 * ==================== KEY TAKEAWAYS ====================
 * 
 * 1. **Master Inorder Traversal**: This is THE fundamental pattern for BST problems
 *    Left → Root → Right = Sorted Order
 * 
 * 2. **Optimize Early**: Don't visit all nodes if you don't need to
 *    Stop at the kth node!
 * 
 * 3. **Know Your Approaches**:
 *    - Recursive with counter (interview default)
 *    - Iterative (no recursion)
 *    - Augmented (multiple queries)
 *    - Morris (space optimization)
 * 
 * 4. **Handle Follow-ups**:
 *    - kth largest: reverse inorder
 *    - Multiple queries: augmented tree
 *    - Range queries: conditional inorder
 * 
 * 5. **Common Patterns**:
 *    - Use global/instance variables carefully (reset them!)
 *    - Remember k is 1-indexed
 *    - Early termination saves time
 * 
 * RELATED PROBLEMS:
 * - LC 230: Kth Smallest Element in BST (this problem)
 * - LC 671: Second Minimum Node in Binary Tree
 * - LC 530: Minimum Absolute Difference in BST
 * - LC 538: Convert BST to Greater Tree
 * - LC 98: Validate BST
 * - LC 285: Inorder Successor in BST
 * 
 * VARIATIONS TO PRACTICE:
 * - Find kth largest
 * - Find median of BST
 * - Find closest value to target
 * - Find kth smallest in range [L, R]
 * - Two Sum in BST
 * 
 * COMPLEXITY CHEAT SHEET:
 * - Inorder traversal: Always O(n) time in worst case
 * - Early stopping: O(h + k) average, O(n) worst
 * - Space: O(h) for recursion/stack, O(1) for Morris
 * - BST height: O(log n) balanced, O(n) skewed
 */

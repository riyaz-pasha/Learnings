/**
 * Recover Binary Search Tree - Complete Implementation
 * 
 * PROBLEM UNDERSTANDING:
 * Two nodes in a BST were swapped by mistake. Recover the tree without changing structure.
 * 
 * CRITICAL INSIGHTS:
 * 
 * 1. BST PROPERTY VIOLATION:
 *    Inorder traversal of valid BST = strictly increasing sequence
 *    Swapped nodes cause violations in this sequence
 * 
 * 2. TWO VIOLATION PATTERNS:
 * 
 *    a) ADJACENT NODES SWAPPED:
 *       Normal:   [1, 2, 3, 4, 5]
 *       Swapped:  [1, 3, 2, 4, 5]
 *                     ↑  ↑
 *       ONE violation: 3 > 2
 *       Swap: 3 and 2
 * 
 *    b) NON-ADJACENT NODES SWAPPED:
 *       Normal:   [1, 2, 3, 4, 5]
 *       Swapped:  [1, 4, 3, 2, 5]
 *                     ↑     ↑
 *       TWO violations: 4 > 3 and 3 > 2
 *       First violation: larger is first node
 *       Second violation: smaller is second node
 *       Swap: 4 and 2
 * 
 * 3. DETECTION STRATEGY:
 *    During inorder traversal, track when current < previous
 *    - First violation: mark larger value (previous)
 *    - Second violation: mark smaller value (current)
 *    - If only one violation: swap adjacent pair
 * 
 * Example 1: Adjacent swap
 *     3 (should be 1)
 *    / \
 *   1   4
 *    \
 *     2 (should be 3)
 * 
 * Inorder: [1, 3, 2, 4] → 3 > 2 violation
 * Swap nodes 3 and 2
 * 
 * Example 2: Non-adjacent swap
 *     3
 *    / \
 *   1   4
 *  /
 * 2 (should be in right subtree)
 * 
 * Inorder: [2, 1, 3, 4] → 2 > 1 violation
 * First = 2, Second = 1
 * Swap nodes 2 and 1
 * 
 * TIME COMPLEXITY: O(n) - single inorder traversal
 * SPACE COMPLEXITY: O(h) recursive, O(1) with Morris traversal
 */

import java.util.ArrayList;
import java.util.Collections;
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

class RecoverBST {
    
    /**
     * APPROACH 1: RECURSIVE INORDER (MOST INTUITIVE) ⭐
     * 
     * STRATEGY:
     * 1. Perform inorder traversal
     * 2. Track previous node
     * 3. Detect violations (current < previous)
     * 4. Mark the two swapped nodes
     * 5. Swap their values
     * 
     * KEY PATTERN:
     * - First violation: previous node is first swapped node
     * - Second violation (if exists): current node is second swapped node
     * - If only one violation: both nodes are adjacent in inorder
     * 
     * TIME: O(n) - visit each node once
     * SPACE: O(h) - recursion stack
     */
    
    private TreeNode firstNode = null;   // First node to be swapped
    private TreeNode secondNode = null;  // Second node to be swapped
    private TreeNode prevNode = null;    // Previous node in inorder
    
    public void recoverTree(TreeNode root) {
        // Reset for multiple calls
        firstNode = null;
        secondNode = null;
        prevNode = null;
        
        // Find the two swapped nodes
        inorderDetect(root);
        
        // Swap their values
        if (firstNode != null && secondNode != null) {
            int temp = firstNode.val;
            firstNode.val = secondNode.val;
            secondNode.val = temp;
        }
    }
    
    private void inorderDetect(TreeNode node) {
        if (node == null) {
            return;
        }
        
        // LEFT
        inorderDetect(node.left);
        
        // ROOT (process current node)
        /**
         * VIOLATION DETECTION:
         * In valid BST inorder: each value > previous value
         * If current < previous → violation!
         */
        if (prevNode != null && node.val < prevNode.val) {
            /**
             * FIRST VIOLATION:
             * The larger value (prevNode) is out of place
             * This is definitely one of the swapped nodes
             */
            if (firstNode == null) {
                firstNode = prevNode;
                /**
                 * IMPORTANT: Also tentatively set secondNode to current
                 * If this is the ONLY violation (adjacent swap),
                 * current is the second swapped node
                 */
                secondNode = node;
            } else {
                /**
                 * SECOND VIOLATION:
                 * The smaller value (node) is the actual second swapped node
                 * Update secondNode (overwrite the tentative value)
                 */
                secondNode = node;
            }
        }
        
        // Update previous for next iteration
        prevNode = node;
        
        // RIGHT
        inorderDetect(node.right);
    }
    
    
    /**
     * APPROACH 2: ITERATIVE INORDER (NO RECURSION)
     * 
     * STRATEGY:
     * Same logic as recursive, but use explicit stack for traversal.
     * 
     * ADVANTAGE:
     * - No recursion stack overhead
     * - Easier to debug step-by-step
     * 
     * TIME: O(n)
     * SPACE: O(h) - explicit stack
     */
    public void recoverTree_Iterative(TreeNode root) {
        Stack<TreeNode> stack = new Stack<>();
        TreeNode current = root;
        TreeNode prev = null;
        TreeNode first = null;
        TreeNode second = null;
        
        while (current != null || !stack.isEmpty()) {
            // Go left as far as possible
            while (current != null) {
                stack.push(current);
                current = current.left;
            }
            
            // Process node
            current = stack.pop();
            
            // Detect violation
            if (prev != null && current.val < prev.val) {
                if (first == null) {
                    first = prev;
                    second = current;  // Tentative
                } else {
                    second = current;  // Confirmed
                }
            }
            
            prev = current;
            current = current.right;
        }
        
        // Swap values
        if (first != null && second != null) {
            int temp = first.val;
            first.val = second.val;
            second.val = temp;
        }
    }
    
    
    /**
     * APPROACH 3: MORRIS TRAVERSAL (O(1) SPACE) ⭐⭐⭐
     * 
     * STRATEGY:
     * Use Morris traversal to achieve O(1) space.
     * Create temporary links (threads) to enable traversal without stack.
     * 
     * MORRIS TRAVERSAL CONCEPT:
     * For each node:
     * 1. Find inorder predecessor (rightmost node in left subtree)
     * 2. Create thread: predecessor.right = current
     * 3. Use thread to return to current after processing left subtree
     * 4. Remove thread after use (restore tree structure)
     * 
     * BRILLIANCE:
     * - No stack, no recursion
     * - O(1) extra space (only pointers)
     * - Temporarily modifies tree but restores it
     * 
     * TIME: O(n) - each edge traversed at most twice
     * SPACE: O(1) - only pointer variables!
     */
    public void recoverTree_Morris(TreeNode root) {
        TreeNode current = root;
        TreeNode prev = null;
        TreeNode first = null;
        TreeNode second = null;
        
        while (current != null) {
            if (current.left == null) {
                // No left subtree: process current node
                
                // Detect violation
                if (prev != null && current.val < prev.val) {
                    if (first == null) {
                        first = prev;
                        second = current;
                    } else {
                        second = current;
                    }
                }
                
                prev = current;
                current = current.right;
                
            } else {
                // Find inorder predecessor
                TreeNode predecessor = current.left;
                
                // Go to rightmost node in left subtree
                while (predecessor.right != null && predecessor.right != current) {
                    predecessor = predecessor.right;
                }
                
                if (predecessor.right == null) {
                    // Create thread
                    predecessor.right = current;
                    current = current.left;
                } else {
                    // Thread exists: we've finished left subtree
                    // Remove thread and process current
                    predecessor.right = null;
                    
                    // Detect violation
                    if (prev != null && current.val < prev.val) {
                        if (first == null) {
                            first = prev;
                            second = current;
                        } else {
                            second = current;
                        }
                    }
                    
                    prev = current;
                    current = current.right;
                }
            }
        }
        
        // Swap values
        if (first != null && second != null) {
            int temp = first.val;
            first.val = second.val;
            second.val = temp;
        }
    }
    
    
    /**
     * APPROACH 4: STORE INORDER AND FIND SWAPPED (EXPLICIT ARRAY)
     * 
     * STRATEGY:
     * 1. Store complete inorder traversal
     * 2. Find the two violations in array
     * 3. Map back to tree nodes and swap
     * 
     * ADVANTAGE:
     * - Very clear logic
     * - Easy to understand and debug
     * 
     * DISADVANTAGE:
     * - O(n) extra space for arrays
     * - Two passes through tree
     * 
     * TIME: O(n)
     * SPACE: O(n) - store all nodes and values
     */
    public void recoverTree_ExplicitArray(TreeNode root) {
        List<TreeNode> nodes = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        
        // Get inorder traversal
        inorderCollect(root, nodes, values);
        
        // Find violations in values array
        int first = -1, second = -1;
        
        for (int i = 0; i < values.size() - 1; i++) {
            if (values.get(i) > values.get(i + 1)) {
                if (first == -1) {
                    first = i;
                    second = i + 1;  // Tentative
                } else {
                    second = i + 1;  // Confirmed
                }
            }
        }
        
        // Swap values in nodes
        if (first != -1 && second != -1) {
            int temp = nodes.get(first).val;
            nodes.get(first).val = nodes.get(second).val;
            nodes.get(second).val = temp;
        }
    }
    
    private void inorderCollect(TreeNode node, List<TreeNode> nodes, List<Integer> values) {
        if (node == null) return;
        
        inorderCollect(node.left, nodes, values);
        nodes.add(node);
        values.add(node.val);
        inorderCollect(node.right, nodes, values);
    }
    
    
    /**
     * ==================== VISUAL WALKTHROUGH ====================
     * 
     * EXAMPLE 1: ADJACENT NODES SWAPPED
     * 
     * Original Tree (correct):
     *       2
     *      / \
     *     1   3
     * 
     * After Swap (wrong):
     *       3
     *      / \
     *     1   2
     * 
     * Inorder Traversal:
     * Position: 0   1   2
     * Values:  [1,  3,  2]
     * Prev:    -   1   3
     * 
     * Step 1: value=1, prev=null → no violation
     * Step 2: value=3, prev=1 (3 > 1) → no violation
     * Step 3: value=2, prev=3 (2 < 3) → VIOLATION!
     *         firstNode = prev = 3
     *         secondNode = current = 2
     * 
     * Only ONE violation → adjacent swap
     * Swap: 3 ↔ 2
     * 
     * Recovered Tree:
     *       2
     *      / \
     *     1   3
     * 
     * ----------------------------------------
     * 
     * EXAMPLE 2: NON-ADJACENT NODES SWAPPED
     * 
     * Original Tree (correct):
     *       3
     *      / \
     *     1   4
     *      \
     *       2
     * 
     * After Swap (wrong - swap 1 and 4):
     *       3
     *      / \
     *     4   1
     *      \
     *       2
     * 
     * Inorder Traversal:
     * Position: 0   1   2   3
     * Values:  [4,  2,  3,  1]
     * Prev:    -   4   2   3
     * 
     * Step 1: value=4, prev=null → no violation
     * Step 2: value=2, prev=4 (2 < 4) → FIRST VIOLATION!
     *         firstNode = prev = 4
     *         secondNode = current = 2 (tentative)
     * Step 3: value=3, prev=2 (3 > 2) → no violation
     * Step 4: value=1, prev=3 (1 < 3) → SECOND VIOLATION!
     *         secondNode = current = 1 (confirmed)
     * 
     * TWO violations → non-adjacent swap
     * firstNode = 4, secondNode = 1
     * Swap: 4 ↔ 1
     * 
     * Recovered Tree:
     *       3
     *      / \
     *     1   4
     *      \
     *       2
     * 
     * ----------------------------------------
     * 
     * PATTERN RECOGNITION:
     * 
     * Sorted:    [1, 2, 3, 4, 5]
     * 
     * Adjacent:  [1, 3, 2, 4, 5]
     *                ↑  ↑
     *            ONE violation
     *            Swap the pair
     * 
     * Far apart: [1, 4, 3, 2, 5]
     *                ↑     ↑
     *            TWO violations
     *            First: larger value (4)
     *            Second: smaller value (2)
     */
    
    
    /**
     * ==================== COMPLEXITY ANALYSIS ====================
     * 
     * TIME COMPLEXITY: O(n) for all approaches
     * - Must visit each node at least once
     * - Detection happens during single traversal
     * 
     * SPACE COMPLEXITY:
     * 
     * Recursive:       O(h) - call stack
     * Iterative:       O(h) - explicit stack
     * Morris:          O(1) - only pointers ← OPTIMAL! ⭐
     * Explicit Array:  O(n) - store all nodes
     * 
     * MORRIS TRAVERSAL ADVANTAGE:
     * 
     * For balanced tree with 1,000,000 nodes:
     * - Recursive: ~20 stack frames (log n)
     * - Morris: 3-4 pointer variables
     * 
     * For skewed tree with 1,000,000 nodes:
     * - Recursive: 1,000,000 stack frames (might overflow!)
     * - Morris: Still just 3-4 pointer variables ⭐
     * 
     * Morris is the ONLY approach guaranteed not to overflow!
     */
    
    
    /**
     * ==================== INTERVIEW STRATEGY GUIDE ====================
     * 
     * STEP 1: CLARIFY (1-2 minutes)
     * Questions to ask:
     * - Exactly two nodes swapped? (Yes)
     * - Can we modify node values? (Yes - that's the solution)
     * - Should we maintain structure? (Yes - no restructuring)
     * - Empty tree possible? (Usually no, but good to ask)
     * 
     * STEP 2: EXPLAIN THE INSIGHT (3-4 minutes)
     * "The key insight is that a BST's inorder traversal should be strictly
     * increasing. When two nodes are swapped, this creates violations.
     * We can detect these violations and identify which nodes to swap back."
     * 
     * Draw example:
     *     3           Inorder: [1, 3, 2, 4]
     *    / \                      ↑  ↑
     *   1   4          Violation: 3 > 2
     *    \
     *     2
     * 
     * "In this case, we see one violation where 3 > 2 when it should be
     * 2 < 3. The violated pair tells us which nodes to swap."
     * 
     * STEP 3: DISCUSS PATTERNS (2 minutes)
     * "There are two patterns:
     * 1. Adjacent swap: ONE violation in inorder
     * 2. Non-adjacent swap: TWO violations in inorder
     * 
     * For case 1: swap the violated pair
     * For case 2: first violation gives larger node,
     *            second violation gives smaller node"
     * 
     * STEP 4: CODE (8-10 minutes)
     * Write recursive solution (clearest)
     * Explain violation detection logic
     * 
     * STEP 5: TEST (3-4 minutes)
     * Test cases:
     * 1. Adjacent swap
     * 2. Non-adjacent swap
     * 3. Root swapped with leaf
     * 4. Two children of same parent swapped
     * 
     * STEP 6: OPTIMIZE (remaining time)
     * - Mention Morris traversal for O(1) space
     * - Discuss iterative approach
     * - Explain time/space tradeoffs
     */
    
    
    /**
     * ==================== COMMON MISTAKES ====================
     */
    
    // ❌ MISTAKE 1: Only marking first violation
    private TreeNode wrong_first = null;
    private TreeNode wrong_second = null;
    private TreeNode wrong_prev = null;
    
    private void wrongDetect(TreeNode node) {
        if (node == null) return;
        
        wrongDetect(node.left);
        
        if (wrong_prev != null && node.val < wrong_prev.val) {
            // ❌ Only set once!
            if (wrong_first == null) {
                wrong_first = wrong_prev;
                wrong_second = node;
                return;  // ❌ Wrong! Need to continue for second violation
            }
        }
        
        wrong_prev = node;
        wrongDetect(node.right);
    }
    
    
    // ❌ MISTAKE 2: Not handling adjacent case
    private void wrongDetect2(TreeNode node) {
        if (node == null) return;
        
        wrongDetect2(node.left);
        
        if (wrong_prev != null && node.val < wrong_prev.val) {
            if (wrong_first == null) {
                wrong_first = wrong_prev;
                // ❌ Didn't set secondNode tentatively!
                // Adjacent case will fail
            } else {
                wrong_second = node;
            }
        }
        
        wrong_prev = node;
        wrongDetect2(node.right);
    }
    
    
    // ❌ MISTAKE 3: Swapping nodes instead of values
    public void wrongSwap(TreeNode root) {
        // Find nodes...
        
        // ❌ Trying to swap node references
        TreeNode temp = firstNode;
        firstNode = secondNode;
        secondNode = temp;
        
        // This doesn't work! We need to swap VALUES
        // The nodes stay in their positions in the tree
    }
    
    
    // ❌ MISTAKE 4: Comparing values instead of previous node
    private void wrongDetect3(TreeNode node) {
        if (node == null) return;
        
        wrongDetect3(node.left);
        
        // ❌ Comparing with root's value or some fixed value
        if (node.val < node.left.val) {  // Wrong!
            // This doesn't work for inorder sequence
        }
        
        // ✓ Should compare with previous node in inorder
        if (wrong_prev != null && node.val < wrong_prev.val) {
            // Correct
        }
        
        wrongDetect3(node.right);
    }
    
    
    /**
     * ==================== FOLLOW-UP QUESTIONS ====================
     */
    
    /**
     * Q1: "What if more than two nodes are swapped?"
     * A: The problem guarantees exactly two. For general case,
     *    would need to sort inorder and rebuild tree.
     */
    public void recoverMultipleSwaps(TreeNode root) {
        List<TreeNode> nodes = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        
        inorderCollect(root, nodes, values);
        
        // Sort values
        Collections.sort(values);
        
        // Assign sorted values back to nodes
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).val = values.get(i);
        }
    }
    
    
    /**
     * Q2: "Can you detect if a BST has been corrupted?"
     * A: Check if inorder is strictly increasing
     */
    public boolean isValidBST(TreeNode root) {
        List<Integer> inorder = new ArrayList<>();
        getInorder(root, inorder);
        
        for (int i = 0; i < inorder.size() - 1; i++) {
            if (inorder.get(i) >= inorder.get(i + 1)) {
                return false;
            }
        }
        return true;
    }
    
    private void getInorder(TreeNode node, List<Integer> result) {
        if (node == null) return;
        getInorder(node.left, result);
        result.add(node.val);
        getInorder(node.right, result);
    }
    
    
    /**
     * Q3: "Find all violations in a corrupted tree?"
     * A: Track all positions where prev > current
     */
    public List<int[]> findAllViolations(TreeNode root) {
        List<int[]> violations = new ArrayList<>();
        List<Integer> inorder = new ArrayList<>();
        getInorder(root, inorder);
        
        for (int i = 0; i < inorder.size() - 1; i++) {
            if (inorder.get(i) > inorder.get(i + 1)) {
                violations.add(new int[]{inorder.get(i), inorder.get(i + 1)});
            }
        }
        
        return violations;
    }
    
    
    /**
     * ==================== UTILITY METHODS ====================
     */
    
    // Validate BST
    public boolean validate(TreeNode root) {
        return validateHelper(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    private boolean validateHelper(TreeNode node, long min, long max) {
        if (node == null) return true;
        if (node.val <= min || node.val >= max) return false;
        return validateHelper(node.left, min, node.val) &&
               validateHelper(node.right, node.val, max);
    }
    
    // Print inorder
    public List<Integer> printInorder(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorderPrint(root, result);
        return result;
    }
    
    private void inorderPrint(TreeNode node, List<Integer> result) {
        if (node == null) return;
        inorderPrint(node.left, result);
        result.add(node.val);
        inorderPrint(node.right, result);
    }
    
    // Clone tree
    public TreeNode cloneTree(TreeNode root) {
        if (root == null) return null;
        TreeNode newNode = new TreeNode(root.val);
        newNode.left = cloneTree(root.left);
        newNode.right = cloneTree(root.right);
        return newNode;
    }
    
    
    /**
     * ==================== TEST CASES ====================
     */
    public static void main(String[] args) {
        RecoverBST solution = new RecoverBST();
        
        System.out.println("=== Test 1: Adjacent Nodes Swapped ===");
        // Correct tree: [1,2,3] Inorder: [1,2,3]
        // Swapped: [1,3,2] Inorder: [1,3,2]
        TreeNode test1 = new TreeNode(3);
        test1.left = new TreeNode(1);
        test1.right = new TreeNode(2);
        
        System.out.println("Before: " + solution.printInorder(test1));
        System.out.println("Valid: " + solution.validate(test1));
        
        solution.recoverTree(test1);
        
        System.out.println("After:  " + solution.printInorder(test1));
        System.out.println("Valid: " + solution.validate(test1));
        
        System.out.println("\n=== Test 2: Non-Adjacent Nodes Swapped ===");
        // Correct tree inorder: [1,2,3,4]
        // Swapped: 1 and 4
        TreeNode test2 = new TreeNode(3);
        test2.left = new TreeNode(4);
        test2.right = new TreeNode(1);
        test2.left.right = new TreeNode(2);
        
        System.out.println("Before: " + solution.printInorder(test2));
        System.out.println("Valid: " + solution.validate(test2));
        
        TreeNode test2_clone = solution.cloneTree(test2);
        solution.recoverTree(test2);
        
        System.out.println("After:  " + solution.printInorder(test2));
        System.out.println("Valid: " + solution.validate(test2));
        
        System.out.println("\n=== Test 3: Morris Traversal ===");
        solution.recoverTree_Morris(test2_clone);
        System.out.println("Morris: " + solution.printInorder(test2_clone));
        System.out.println("Valid: " + solution.validate(test2_clone));
        
        System.out.println("\n=== Test 4: Root and Leaf Swapped ===");
        TreeNode test3 = new TreeNode(2);
        test3.left = new TreeNode(3);
        test3.right = new TreeNode(1);
        
        System.out.println("Before: " + solution.printInorder(test3));
        solution.recoverTree(test3);
        System.out.println("After:  " + solution.printInorder(test3));
        System.out.println("Valid: " + solution.validate(test3));
        
        System.out.println("\n=== Test 5: Larger Tree ===");
        TreeNode test4 = new TreeNode(5);
        test4.left = new TreeNode(3);
        test4.right = new TreeNode(7);
        test4.left.left = new TreeNode(2);
        test4.left.right = new TreeNode(4);
        test4.right.left = new TreeNode(6);
        test4.right.right = new TreeNode(1);  // Should be 8
        
        System.out.println("Before: " + solution.printInorder(test4));
        List<int[]> violations = solution.findAllViolations(test4);
        System.out.println("Violations found: " + violations.size());
        
        solution.recoverTree(test4);
        System.out.println("After:  " + solution.printInorder(test4));
        System.out.println("Valid: " + solution.validate(test4));
    }
}

/**
 * ==================== KEY TAKEAWAYS ====================
 * 
 * 1. **INORDER VIOLATION DETECTION**: Core technique
 * 
 * 2. **TWO PATTERNS**: Adjacent vs non-adjacent swap
 * 
 * 3. **TENTATIVE SECOND NODE**: Set on first violation,
 *    update on second violation (if exists)
 * 
 * 4. **SWAP VALUES, NOT NODES**: Maintain tree structure
 * 
 * 5. **MORRIS FOR O(1) SPACE**: Advanced but optimal
 * 
 * 6. **SINGLE PASS SOLUTION**: Efficient detection
 * 
 * INTERVIEW STRATEGY:
 * - Start with recursive (clearest)
 * - Explain two violation patterns
 * - Draw examples for both cases
 * - Mention Morris if asked about space
 * 
 * RELATED PROBLEMS:
 * - LC 99: Recover BST (this problem)
 * - LC 98: Validate BST
 * - LC 94: Binary Tree Inorder Traversal
 * - LC 285: Inorder Successor in BST
 * - LC 230: Kth Smallest in BST
 * - LC 530: Minimum Absolute Difference in BST
 * 
 * PATTERN:
 * Inorder traversal problems in BST:
 * - Always produces sorted sequence
 * - Track previous node for comparisons
 * - Violations indicate BST property issues
 */

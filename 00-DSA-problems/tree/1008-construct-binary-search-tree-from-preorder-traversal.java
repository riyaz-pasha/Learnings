/**
 * Construct Binary Search Tree from Preorder Traversal - Complete Implementation
 * 
 * PROBLEM UNDERSTANDING:
 * Given a preorder traversal array, reconstruct the original BST.
 * 
 * CRITICAL INSIGHTS:
 * 
 * 1. PREORDER = ROOT → LEFT → RIGHT
 *    - First element is always the root
 *    - Elements < root go to left subtree
 *    - Elements > root go to right subtree
 * 
 * 2. BST PROPERTY:
 *    - Left subtree: all values < root
 *    - Right subtree: all values > root
 *    - This gives us a natural way to partition the array!
 * 
 * 3. RANGE-BASED APPROACH:
 *    Each node has a valid value range [min, max]
 *    - Root: [-∞, +∞]
 *    - Left child: [parent's min, parent's value]
 *    - Right child: [parent's value, parent's max]
 * 
 * Example:
 * preorder = [8, 5, 1, 7, 10, 12]
 * 
 * Tree:       8
 *            / \
 *           5   10
 *          / \    \
 *         1   7   12
 * 
 * Preorder traversal: 8 → 5 → 1 → 7 → 10 → 12
 * 
 * COMPARISON TO OTHER TRAVERSALS:
 * - Preorder alone: Can construct BST ✓ (BST property helps)
 * - Inorder alone: Cannot construct unique tree ✗
 * - Preorder + Inorder: Can construct any binary tree ✓
 * 
 * TIME COMPLEXITY: O(n) optimal, O(n²) naive
 * SPACE COMPLEXITY: O(n) for tree, O(h) for recursion
 */

import java.util.*;

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode(int val) {
        this.val = val;
    }
}

class ConstructBSTFromPreorder {
    
    /**
     * APPROACH 1: RECURSIVE WITH RANGE (OPTIMAL) ⭐
     * 
     * STRATEGY:
     * Use upper and lower bounds to determine which elements belong to current subtree.
     * Process elements in order, skipping those outside valid range.
     * 
     * KEY INSIGHT:
     * Each recursive call consumes elements from the global index as long as
     * they fall within the valid range [lower, upper].
     * 
     * VISUALIZATION:
     * preorder = [8, 5, 1, 7, 10, 12]
     * 
     * Build(8, -∞, +∞):
     *   8 is in range → create node 8
     *   Build left(-, -∞, 8):
     *     5 is in range → create node 5
     *     Build left(-, -∞, 5):
     *       1 is in range → create node 1
     *       Build left(-, -∞, 1): nothing in range
     *       Build right(-, 1, 5): 7 is NOT in range, skip
     *     Build right(-, 5, 8):
     *       7 is in range → create node 7
     *   Build right(-, 8, +∞):
     *     10 is in range → create node 10
     *     Build left(-, 8, 10): nothing in range
     *     Build right(-, 10, +∞):
     *       12 is in range → create node 12
     * 
     * TIME: O(n) - each element processed once
     * SPACE: O(h) - recursion stack depth
     */
    
    private int index = 0;  // Global index tracking current position
    
    public TreeNode bstFromPreorder(int[] preorder) {
        index = 0;  // Reset for multiple calls
        return buildTree(preorder, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    private TreeNode buildTree(int[] preorder, int lower, int upper) {
        // Base case: processed all elements or current element out of range
        if (index >= preorder.length) {
            return null;
        }
        
        int val = preorder[index];
        
        /**
         * CRITICAL CHECK: Is current value in valid range?
         * 
         * If NO: This element belongs to a different subtree (backtrack)
         * If YES: This element belongs to current subtree (consume it)
         * 
         * Example: When building left subtree of 8
         * - Range is [-∞, 8]
         * - When we see 10, it's > 8, so it's NOT for left subtree
         * - Don't consume it, return null, let parent's right call handle it
         */
        if (val < lower || val > upper) {
            return null;  // Out of range, don't consume
        }
        
        // Value is in range: consume it and create node
        index++;
        TreeNode root = new TreeNode(val);
        
        /**
         * BUILD LEFT SUBTREE:
         * Range: [lower, val]
         * All values must be < current node's value
         * 
         * Example: For node 8
         * Left range: [-∞, 8]
         * Values like 5, 1, 7 will be consumed
         */
        root.left = buildTree(preorder, lower, val);
        
        /**
         * BUILD RIGHT SUBTREE:
         * Range: [val, upper]
         * All values must be > current node's value
         * 
         * Example: For node 8
         * Right range: [8, +∞]
         * Values like 10, 12 will be consumed
         */
        root.right = buildTree(preorder, val, upper);
        
        return root;
    }
    
    
    /**
     * APPROACH 2: USING STACK (ITERATIVE) ⭐
     * 
     * STRATEGY:
     * Use a stack to track the path from root to current node.
     * For each new value:
     * - If < stack top: it's a left child
     * - If > stack top: pop until we find its parent, then it's a right child
     * 
     * INTUITION:
     * Stack maintains the "rightmost path" - nodes waiting for right children.
     * When we see a larger value, we pop smaller values until we find where it belongs.
     * 
     * VISUALIZATION:
     * preorder = [8, 5, 1, 7, 10, 12]
     * 
     * Process 8: Stack = [8], Tree = 8
     * 
     * Process 5 (< 8): Stack = [8, 5], Tree = 8
     *                                           /
     *                                          5
     * 
     * Process 1 (< 5): Stack = [8, 5, 1], Tree = 8
     *                                            /
     *                                           5
     *                                          /
     *                                         1
     * 
     * Process 7 (> 1, < 5): Pop 1, 7 is right child of 1
     *                       Stack = [8, 5, 7]
     *                       Tree = 8
     *                             /
     *                            5
     *                           / \
     *                          1   7
     * 
     * Process 10 (> 7, > 5, < 8): Pop 7, 5; 10 is right child of 8
     *                             Stack = [8, 10]
     * 
     * Process 12 (> 10): 12 is right child of 10
     *                    Stack = [8, 10, 12]
     * 
     * TIME: O(n) - each element pushed and popped at most once
     * SPACE: O(h) - stack depth
     */
    public TreeNode bstFromPreorder_Stack(int[] preorder) {
        if (preorder == null || preorder.length == 0) {
            return null;
        }
        
        Stack<TreeNode> stack = new Stack<>();
        TreeNode root = new TreeNode(preorder[0]);
        stack.push(root);
        
        for (int i = 1; i < preorder.length; i++) {
            TreeNode node = new TreeNode(preorder[i]);
            TreeNode parent = null;
            
            /**
             * FIND CORRECT PARENT:
             * Pop all nodes smaller than current value.
             * The last popped node is the parent (for right child).
             */
            while (!stack.isEmpty() && stack.peek().val < node.val) {
                parent = stack.pop();
            }
            
            if (parent != null) {
                // Current value is larger: it's a right child
                parent.right = node;
            } else {
                // Current value is smaller: it's a left child of stack top
                stack.peek().left = node;
            }
            
            stack.push(node);
        }
        
        return root;
    }
    
    
    /**
     * APPROACH 3: RECURSIVE WITH ARRAY PARTITIONING (INTUITIVE BUT SLOWER)
     * 
     * STRATEGY:
     * 1. First element is root
     * 2. Find first element > root (start of right subtree)
     * 3. Recursively build left and right subtrees
     * 
     * VISUALIZATION:
     * preorder = [8, 5, 1, 7, 10, 12]
     *             ↑  └─────┘ └────┘
     *           root  left   right
     * 
     * Root = 8
     * Left = [5, 1, 7] (all < 8)
     * Right = [10, 12] (all > 8)
     * 
     * Recurse on [5, 1, 7]:
     *   Root = 5
     *   Left = [1] (< 5)
     *   Right = [7] (> 5)
     * 
     * TIME: O(n²) worst case (skewed tree)
     *       O(n log n) average case (balanced tree)
     * SPACE: O(n) for array copies + O(h) recursion
     * 
     * NOTE: Less efficient than Approach 1, but easier to understand initially
     */
    public TreeNode bstFromPreorder_Partition(int[] preorder) {
        if (preorder == null || preorder.length == 0) {
            return null;
        }
        
        return buildWithPartition(preorder, 0, preorder.length - 1);
    }
    
    private TreeNode buildWithPartition(int[] preorder, int start, int end) {
        if (start > end) {
            return null;
        }
        
        // First element is root
        TreeNode root = new TreeNode(preorder[start]);
        
        if (start == end) {
            return root;  // Leaf node
        }
        
        /**
         * FIND PARTITION POINT:
         * First index where value > root.val
         * All elements before this go to left subtree
         * All elements from this onward go to right subtree
         */
        int rightStart = start + 1;
        while (rightStart <= end && preorder[rightStart] < root.val) {
            rightStart++;
        }
        
        // Build subtrees
        root.left = buildWithPartition(preorder, start + 1, rightStart - 1);
        root.right = buildWithPartition(preorder, rightStart, end);
        
        return root;
    }
    
    
    /**
     * APPROACH 4: USING INORDER (IF WE COULD DERIVE IT)
     * 
     * INSIGHT:
     * If we sort the preorder array, we get the inorder traversal!
     * (BST inorder is always sorted)
     * 
     * Then use standard "build tree from preorder + inorder" approach.
     * 
     * STRATEGY:
     * 1. inorder = sorted(preorder)
     * 2. Use preorder and inorder to build tree
     * 
     * TIME: O(n log n) for sorting + O(n) for building = O(n log n)
     * SPACE: O(n) for inorder array
     * 
     * NOTE: This is SLOWER than Approach 1, but shows an interesting connection
     */
    public TreeNode bstFromPreorder_WithInorder(int[] preorder) {
        if (preorder == null || preorder.length == 0) {
            return null;
        }
        
        // Create inorder by sorting
        int[] inorder = preorder.clone();
        Arrays.sort(inorder);
        
        return buildFromPreorderInorder(preorder, inorder, 0, 0, inorder.length);
    }
    
    private TreeNode buildFromPreorderInorder(int[] preorder, int[] inorder,
                                               int preStart, int inStart, int inEnd) {
        if (preStart >= preorder.length || inStart >= inEnd) {
            return null;
        }
        
        TreeNode root = new TreeNode(preorder[preStart]);
        
        // Find root in inorder
        int inIndex = inStart;
        while (inIndex < inEnd && inorder[inIndex] != root.val) {
            inIndex++;
        }
        
        int leftSize = inIndex - inStart;
        
        root.left = buildFromPreorderInorder(preorder, inorder,
                                              preStart + 1, inStart, inIndex);
        root.right = buildFromPreorderInorder(preorder, inorder,
                                               preStart + 1 + leftSize, inIndex + 1, inEnd);
        
        return root;
    }
    
    
    /**
     * ==================== VISUAL WALKTHROUGH ====================
     * 
     * DETAILED EXAMPLE: preorder = [8, 5, 1, 7, 10, 12]
     * 
     * Expected Tree:
     *         8
     *        / \
     *       5   10
     *      / \    \
     *     1   7   12
     * 
     * APPROACH 1 TRACE (Range-based):
     * 
     * Call 1: build(idx=0, range=[-∞, +∞])
     *   val = 8 (in range) → Create node 8, idx++
     *   Left: build(idx=1, range=[-∞, 8])
     *     val = 5 (in range) → Create node 5, idx++
     *     Left: build(idx=2, range=[-∞, 5])
     *       val = 1 (in range) → Create node 1, idx++
     *       Left: build(idx=3, range=[-∞, 1])
     *         val = 7 (NOT in range, 7 > 1) → return null
     *       Right: build(idx=3, range=[1, 5])
     *         val = 7 (in range) → Create node 7, idx++
     *         Left: build(idx=4, range=[1, 7])
     *           val = 10 (NOT in range, 10 > 7) → return null
     *         Right: build(idx=4, range=[7, 5])
     *           Invalid range → return null
     *       Return 1 with no children
     *     Right: build(idx=3, range=[5, 8])
     *       Already processed at idx=3
     *     Return 5 with left=1, right=7
     *   Right: build(idx=4, range=[8, +∞])
     *     val = 10 (in range) → Create node 10, idx++
     *     Left: build(idx=5, range=[8, 10])
     *       val = 12 (NOT in range, 12 > 10) → return null
     *     Right: build(idx=5, range=[10, +∞])
     *       val = 12 (in range) → Create node 12, idx++
     *       Left/Right: build(idx=6, ...) → idx >= length, return null
     *     Return 10 with right=12
     *   Return 8 with left=5, right=10
     * 
     * Final tree matches expected structure! ✓
     * 
     * ==========================================
     * 
     * APPROACH 2 TRACE (Stack-based):
     * 
     * Init: root = 8, stack = [8]
     * 
     * i=1, val=5:
     *   5 < 8 (stack top) → 5 is left child of 8
     *   stack = [8, 5]
     * 
     * i=2, val=1:
     *   1 < 5 (stack top) → 1 is left child of 5
     *   stack = [8, 5, 1]
     * 
     * i=3, val=7:
     *   7 > 1 → pop 1 (parent = 1)
     *   7 < 5 → stop popping
     *   7 is right child of 1
     *   stack = [8, 5, 7]
     * 
     * i=4, val=10:
     *   10 > 7 → pop 7 (parent = 7)
     *   10 > 5 → pop 5 (parent = 5)
     *   10 > 8 → pop 8 (parent = 8)
     *   stack empty → 10 is right child of last popped (8)
     *   stack = [10]
     * 
     * i=5, val=12:
     *   12 > 10 → pop 10 (parent = 10)
     *   stack empty → 12 is right child of 10
     *   stack = [12]
     * 
     * Final tree constructed correctly! ✓
     */
    
    
    /**
     * ==================== COMPLEXITY ANALYSIS ====================
     * 
     * TIME COMPLEXITY COMPARISON:
     * 
     * Approach 1 (Range): O(n)
     * - Each element processed exactly once
     * - No backtracking or re-processing
     * - OPTIMAL ✓
     * 
     * Approach 2 (Stack): O(n)
     * - Each element pushed once
     * - Each element popped at most once
     * - Total operations: 2n = O(n)
     * - OPTIMAL ✓
     * 
     * Approach 3 (Partition): O(n²) worst, O(n log n) average
     * - Finding partition point: O(n)
     * - Recurse on left and right
     * - Worst case (skewed): T(n) = T(n-1) + O(n) = O(n²)
     * - Average case (balanced): T(n) = 2T(n/2) + O(n) = O(n log n)
     * - SUBOPTIMAL ✗
     * 
     * Approach 4 (With Inorder): O(n log n)
     * - Sorting: O(n log n)
     * - Building: O(n)
     * - Total: O(n log n)
     * - SUBOPTIMAL ✗
     * 
     * SPACE COMPLEXITY:
     * All approaches: O(h) for recursion/stack + O(n) for tree
     * 
     * RECOMMENDATION: Approach 1 (Range) or Approach 2 (Stack)
     * Both are O(n) time and optimal!
     */
    
    
    /**
     * ==================== INTERVIEW STRATEGY GUIDE ====================
     * 
     * STEP 1: CLARIFY (1-2 minutes)
     * Questions to ask:
     * - Is the array guaranteed to be a valid preorder of a BST?
     * - Can the array be empty?
     * - Are there duplicate values?
     * - What's the expected size of the array?
     * 
     * STEP 2: EXPLAIN PREORDER + BST PROPERTIES (2-3 minutes)
     * "Preorder means Root → Left → Right, so the first element is always the root.
     * BST property means left < root < right, which helps us partition the array.
     * We can use valid ranges to determine which elements belong to each subtree."
     * 
     * Draw example:
     * [8, 5, 1, 7, 10, 12]
     *  ↑ root
     * 
     * Elements < 8: [5, 1, 7] → left subtree
     * Elements > 8: [10, 12] → right subtree
     * 
     * STEP 3: PROPOSE APPROACH (2 minutes)
     * "I'll use a range-based recursive approach:
     * - Each node has a valid range [min, max]
     * - Process elements in order, creating nodes when they fit the range
     * - Left subtree range: [parent's min, parent's value]
     * - Right subtree range: [parent's value, parent's max]
     * This gives us O(n) time since each element is processed once."
     * 
     * STEP 4: CODE (8-10 minutes)
     * Write Approach 1 (Range-based)
     * Explain the range checks while coding
     * 
     * STEP 5: TEST (3-4 minutes)
     * Test cases:
     * 1. Single element: [5] → Tree with one node
     * 2. Left skewed: [5, 4, 3, 2, 1]
     * 3. Right skewed: [1, 2, 3, 4, 5]
     * 4. Balanced: [4, 2, 1, 3, 6, 5, 7]
     * 5. Given example: [8, 5, 1, 7, 10, 12]
     * 
     * STEP 6: OPTIMIZE & ALTERNATIVES (remaining time)
     * - Mention stack-based approach (also O(n))
     * - Discuss why sorting to get inorder is suboptimal
     * - Explain partition approach and its complexity
     */
    
    
    /**
     * ==================== COMMON MISTAKES ====================
     */
    
    // ❌ MISTAKE 1: Not using global index correctly
    public TreeNode wrong1(int[] preorder) {
        return build(preorder, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    private TreeNode build(int[] preorder, int index, int lower, int upper) {
        // ❌ Passing index as parameter doesn't work!
        // Each recursive call needs to know what the NEXT element to process is
        // This requires a shared/global index that all calls update
        if (index >= preorder.length || preorder[index] < lower || preorder[index] > upper) {
            return null;
        }
        
        TreeNode root = new TreeNode(preorder[index]);
        index++;  // ❌ This only updates local copy!
        
        root.left = build(preorder, index, lower, root.val);
        root.right = build(preorder, index, root.val, upper);
        
        return root;
    }
    
    // ❌ MISTAKE 2: Wrong range boundaries
    private TreeNode buildWrong2(int[] preorder, int lower, int upper) {
        // ...
        TreeNode root = new TreeNode(val);
        
        // ❌ Using <= and >= allows duplicates
        root.left = buildWrong2(preorder, lower, val);      // Should be: val (exclusive)
        root.right = buildWrong2(preorder, val, upper);      // Correct
        
        // ❌ This could place equal values in left subtree
        return root;
    }
    
    // ❌ MISTAKE 3: Creating new arrays in partition approach
    public TreeNode buildWrong3(int[] preorder) {
        if (preorder.length == 0) return null;
        
        TreeNode root = new TreeNode(preorder[0]);
        
        // ❌ Creating new arrays is expensive!
        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();
        
        for (int i = 1; i < preorder.length; i++) {
            if (preorder[i] < root.val) {
                left.add(preorder[i]);
            } else {
                right.add(preorder[i]);
            }
        }
        
        // ❌ This works but wastes space: O(n) extra per level = O(n²) total!
        // Better: Use indices to refer to subarrays
        return root;
    }
    
    // ❌ MISTAKE 4: Stack approach with wrong parent logic
    public TreeNode buildWrong4(int[] preorder) {
        Stack<TreeNode> stack = new Stack<>();
        TreeNode root = new TreeNode(preorder[0]);
        stack.push(root);
        
        for (int i = 1; i < preorder.length; i++) {
            TreeNode node = new TreeNode(preorder[i]);
            
            // ❌ Not tracking parent correctly
            if (!stack.isEmpty() && node.val < stack.peek().val) {
                stack.peek().left = node;
            } else {
                stack.peek().right = node;  // ❌ Wrong! Need to pop first
            }
            
            stack.push(node);
        }
        
        return root;
    }
    
    
    /**
     * ==================== FOLLOW-UP QUESTIONS ====================
     */
    
    /**
     * Q1: "What if we had postorder instead of preorder?"
     * A: Process array from right to left, build Right → Root → Left
     */
    public TreeNode bstFromPostorder(int[] postorder) {
        index = postorder.length - 1;
        return buildFromPostorder(postorder, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    private TreeNode buildFromPostorder(int[] postorder, int lower, int upper) {
        if (index < 0 || postorder[index] < lower || postorder[index] > upper) {
            return null;
        }
        
        int val = postorder[index--];
        TreeNode root = new TreeNode(val);
        
        // Note: Build RIGHT first for postorder!
        root.right = buildFromPostorder(postorder, val, upper);
        root.left = buildFromPostorder(postorder, lower, val);
        
        return root;
    }
    
    
    /**
     * Q2: "Verify if a preorder array is valid for a BST?"
     * A: Try to build the tree; if successful, it's valid
     * Or use monotonic stack to verify
     */
    public boolean verifyPreorder(int[] preorder) {
        Stack<Integer> stack = new Stack<>();
        int lowerBound = Integer.MIN_VALUE;
        
        for (int val : preorder) {
            // If value is less than lower bound, invalid
            if (val < lowerBound) {
                return false;
            }
            
            // Pop all smaller values, update lower bound
            while (!stack.isEmpty() && val > stack.peek()) {
                lowerBound = stack.pop();
            }
            
            stack.push(val);
        }
        
        return true;
    }
    
    
    /**
     * Q3: "Build BST with minimum height?"
     * A: Preorder already determines structure; for minimum height,
     *    start from sorted array (inorder)
     */
    public TreeNode balancedBST(int[] sorted) {
        return buildBalanced(sorted, 0, sorted.length - 1);
    }
    
    private TreeNode buildBalanced(int[] arr, int left, int right) {
        if (left > right) return null;
        
        int mid = left + (right - left) / 2;
        TreeNode root = new TreeNode(arr[mid]);
        
        root.left = buildBalanced(arr, left, mid - 1);
        root.right = buildBalanced(arr, mid + 1, right);
        
        return root;
    }
    
    
    /**
     * ==================== UTILITY METHODS ====================
     */
    
    // Verify tree is valid BST
    public boolean isValidBST(TreeNode root) {
        return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    private boolean validate(TreeNode node, long min, long max) {
        if (node == null) return true;
        if (node.val <= min || node.val >= max) return false;
        return validate(node.left, min, node.val) &&
               validate(node.right, node.val, max);
    }
    
    // Get preorder traversal (for verification)
    public List<Integer> getPreorder(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorderTraversal(root, result);
        return result;
    }
    
    private void preorderTraversal(TreeNode node, List<Integer> result) {
        if (node == null) return;
        result.add(node.val);
        preorderTraversal(node.left, result);
        preorderTraversal(node.right, result);
    }
    
    // Print tree structure
    public void printTree(TreeNode root, String prefix, boolean isLeft) {
        if (root == null) return;
        
        System.out.println(prefix + (isLeft ? "├── " : "└── ") + root.val);
        
        if (root.left != null || root.right != null) {
            if (root.left != null) {
                printTree(root.left, prefix + (isLeft ? "│   " : "    "), true);
            }
            if (root.right != null) {
                printTree(root.right, prefix + (isLeft ? "│   " : "    "), false);
            }
        }
    }
    
    
    /**
     * ==================== TEST CASES ====================
     */
    public static void main(String[] args) {
        ConstructBSTFromPreorder solution = new ConstructBSTFromPreorder();
        
        System.out.println("=== Test 1: Standard BST ===");
        int[] preorder1 = {8, 5, 1, 7, 10, 12};
        System.out.println("Preorder: " + Arrays.toString(preorder1));
        
        TreeNode tree1a = solution.bstFromPreorder(preorder1);
        TreeNode tree1b = solution.bstFromPreorder_Stack(preorder1);
        TreeNode tree1c = solution.bstFromPreorder_Partition(preorder1);
        
        System.out.println("\nRange-based approach:");
        solution.printTree(tree1a, "", false);
        System.out.println("Valid BST: " + solution.isValidBST(tree1a));
        System.out.println("Preorder matches: " + solution.getPreorder(tree1a).equals(Arrays.asList(8, 5, 1, 7, 10, 12)));
        
        System.out.println("\nStack-based approach:");
        solution.printTree(tree1b, "", false);
        System.out.println("Valid BST: " + solution.isValidBST(tree1b));
        
        System.out.println("\nPartition-based approach:");
        solution.printTree(tree1c, "", false);
        System.out.println("Valid BST: " + solution.isValidBST(tree1c));
        
        System.out.println("\n=== Test 2: Left Skewed ===");
        int[] preorder2 = {5, 4, 3, 2, 1};
        System.out.println("Preorder: " + Arrays.toString(preorder2));
        TreeNode tree2 = solution.bstFromPreorder(preorder2);
        solution.printTree(tree2, "", false);
        System.out.println("Valid BST: " + solution.isValidBST(tree2));
        
        System.out.println("\n=== Test 3: Right Skewed ===");
        int[] preorder3 = {1, 2, 3, 4, 5};
        System.out.println("Preorder: " + Arrays.toString(preorder3));
        TreeNode tree3 = solution.bstFromPreorder(preorder3);
        solution.printTree(tree3, "", false);
        System.out.println("Valid BST: " + solution.isValidBST(tree3));
        
        System.out.println("\n=== Test 4: Balanced Tree ===");
        int[] preorder4 = {4, 2, 1, 3, 6, 5, 7};
        System.out.println("Preorder: " + Arrays.toString(preorder4));
        TreeNode tree4 = solution.bstFromPreorder(preorder4);
        solution.printTree(tree4, "", false);
        System.out.println("Valid BST: " + solution.isValidBST(tree4));
        
        System.out.println("\n=== Test 5: Single Node ===");
        int[] preorder5 = {42};
        System.out.println("Preorder: " + Arrays.toString(preorder5));
        TreeNode tree5 = solution.bstFromPreorder(preorder5);
        solution.printTree(tree5, "", false);
        System.out.println("Valid BST: " + solution.isValidBST(tree5));
        
        System.out.println("\n=== Test 6: Verify Preorder ===");
        int[] valid = {8, 5, 1, 7, 10, 12};
        int[] invalid = {8, 5, 10, 7, 1, 12};  // Invalid: 10 before 7
        System.out.println("Valid preorder " + Arrays.toString(valid) + ": " +
                          solution.verifyPreorder(valid));
        System.out.println("Invalid preorder " + Arrays.toString(invalid) + ": " +
                          solution.verifyPreorder(invalid));
        
        System.out.println("\n=== Test 7: From Postorder ===");
        int[] postorder = {1, 7, 5, 12, 10, 8};
        System.out.println("Postorder: " + Arrays.toString(postorder));
        TreeNode tree7 = solution.bstFromPostorder(postorder);
        solution.printTree(tree7, "", false);
        System.out.println("Valid BST: " + solution.isValidBST(tree7));
        System.out.println("Preorder: " + solution.getPreorder(tree7));
    }
}

/**
 * ==================== KEY TAKEAWAYS ====================
 * 
 * 1. **TWO OPTIMAL APPROACHES**: Range-based and Stack-based (both O(n))
 * 
 * 2. **Range-Based is Cleanest**: Use [min, max] bounds to guide construction
 * 
 * 3. **Global Index Pattern**: Track position across all recursive calls
 * 
 * 4. **Preorder = Root First**: First element is always root of current subtree
 * 
 * 5. **BST Property**: Elements < root go left, elements > root go right
 * 
 * 6. **Don't Create New Arrays**: Use indices to avoid O(n²) space
 * 
 * 7. **Stack Approach**: Maintains "rightmost path", pop to find parent
 * 
 * INTERVIEW TIPS:
 * - Start with range-based (most elegant)
 * - Explain the valid range concept clearly
 * - Draw example showing how ranges guide construction
 * - Mention stack approach as alternative
 * - Avoid partition approach (too slow)
 * 
 * RELATED PROBLEMS:
 * - LC 1008: Construct BST from Preorder (this problem)
 * - LC 105: Construct Binary Tree from Preorder and Inorder
 * - LC 106: Construct Binary Tree from Inorder and Postorder
 * - LC 889: Construct Binary Tree from Preorder and Postorder
 * - LC 255: Verify Preorder Sequence in BST
 * - LC 331: Verify Preorder Serialization of Binary Tree
 * 
 * PATTERN:
 * Tree construction problems often use:
 * - Range-based recursion for BST
 * - Index mapping for general binary trees
 * - Stack for iterative approaches
 */

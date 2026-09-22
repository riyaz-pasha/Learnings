import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * STUDY NOTE: CONSTRUCT BINARY TREE FROM PREORDER AND INORDER TRAVERSAL
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Are all node values strictly unique?" 
 *   (Crucial: If values are not unique, we cannot definitively locate the root in the inorder array, making the tree ambiguous).
 * - "Is it guaranteed that pOrder and iOrder represent the exact same valid tree?" 
 *   (Prevents us from needing to write extensive validation/error-handling logic for mismatched inputs).
 * - "Can the arrays be empty?" 
 *   (Constraints say length >= 1, but clarifying ensures we establish a safe base case).
 * - "Do we need to worry about the tree's depth causing a StackOverflowError?" 
 *   (Max length is 1000, so a skewed tree will recurse 1000 times. In Java, default stack handles ~7000-10000 frames, so recursion is perfectly safe here).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * A tree's structure is a 2D relationship, but traversals flatten it into 1D arrays. 
 * - Preorder (Node-Left-Right) guarantees the *first* element is ALWAYS the root.
 * - Inorder (Left-Node-Right) guarantees that once you find the root, everything to its left 
 *   belongs to the left subtree, and everything to its right belongs to the right subtree.
 * The binding constraint is how efficiently we can repeatedly isolate these left/right 
 * boundaries for every single subtree without duplicating work.
 *
 * Approach 1: Array Slicing & Linear Search (The Naive Brute Force)
 * What I'd naturally try: Grab the first element of `pOrder`. Scan `iOrder` sequentially to find it. 
 * Once found, literally slice `pOrder` and `iOrder` into smaller physical sub-arrays for the left 
 * and right subtrees, and recursively pass those new arrays down.
 * Why it works: It perfectly mirrors the mathematical definition of breaking down the tree.
 * Why it's too slow/costly: Finding the root takes O(N) time. Copying the sub-arrays takes O(N) time. 
 * We do this at every level of the tree.
 * - Time Complexity: O(N^2) — because for each of the N nodes, we do a linear scan and array copy.
 * - Space Complexity: O(N^2) — because we instantiate massive amounts of temporary sub-arrays 
 *   at every recursive step, thrashing the garbage collector.
 * 
 * Approach 2: Pointer Passing with Linear Search (Intermediate)
 * What I'd naturally try: Stop copying arrays! I can just pass `inStart` and `inEnd` index pointers 
 * to define the logical boundaries of the subtrees, while keeping a global `preIndex` that just 
 * ticks forward by 1 every time we create a node.
 * Why it works: It eliminates the massive memory overhead of array slicing.
 * What work is being repeated: We still execute a `for` loop to scan `iOrder` to find the root's 
 * index at every single recursive step.
 * - Time Complexity: O(N^2) worst case (on a skewed tree), O(N log N) balanced.
 * - Space Complexity: O(N) — bounded entirely by the recursion stack.
 *
 * Approach 3: Pointer Passing with HashMap (The Optimal Choice)
 * What I'd naturally try: How do I stop searching for the root index? Since all values are UNIQUE, 
 * I can pre-compute their exact locations. Before recursing, I'll dump every value of `iOrder` 
 * and its index into a HashMap.
 * Why it works: The O(N) linear search is replaced by an O(1) hash lookup.
 * What property removes the bottleneck: Pre-computation and memory-time tradeoff. We pay O(N) space 
 * upfront to guarantee O(1) time operations down the line.
 * - Time Complexity: O(N) — because we process each node exactly once, doing O(1) map lookups.
 * - Space Complexity: O(N) — because the HashMap stores N key-value pairs, plus O(N) worst-case 
 *   for the recursion stack.
 *
 * The Interview Choice:
 * Always write Approach 3 (HashMap + Pointers). It is the canonical, optimal solution that proves 
 * you know how to map 1D structures to 2D relationships while intelligently caching lookups.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Single Node Tree -> `pOrder` and `iOrder` have length 1. Loop terminates instantly.
 * 2. Fully Left-Skewed Tree -> `pOrder` is roughly the reverse of `iOrder`. Tests if `inStart > inEnd` bounds work.
 * 3. Fully Right-Skewed Tree -> `pOrder` and `iOrder` are identical.
 *
 *
 * 4. DRY RUN (Optimal HashMap Approach)
 * ----------------------------------------------------------------------------
 * pOrder: [3, 9, 20, 15, 7]
 * iOrder: [9, 3, 15, 20, 7]
 * 
 * Pre-computation: map = {9:0, 3:1, 15:2, 20:3, 7:4}
 * Global preIndex = 0
 * 
 * Call Stack Trace (inStart=0, inEnd=4):
 * 1. build(0, 4):
 *    - rootVal = pOrder[0] = 3. (preIndex becomes 1).
 *    - Create Node(3).
 *    - inIndex = map.get(3) = 1.
 *    - Recurse Left: build(inStart=0, inEnd=0)   // Boundaries for '9'
 *    - Recurse Right: build(inStart=2, inEnd=4)  // Boundaries for '15, 20, 7'
 * 
 * 2. [Left Branch] build(0, 0):
 *    - rootVal = pOrder[1] = 9. (preIndex becomes 2).
 *    - Create Node(9).
 *    - inIndex = map.get(9) = 0.
 *    - Recurse Left: build(0, -1) -> Base case hits (start > end), returns null.
 *    - Recurse Right: build(1, 0) -> Base case hits, returns null.
 *    - Returns Node(9) up to Node(3).
 * 
 * 3. [Right Branch] build(2, 4):
 *    - rootVal = pOrder[2] = 20. (preIndex becomes 3).
 *    - Create Node(20).
 *    - inIndex = map.get(20) = 3.
 *    - Recurse Left: build(2, 2) -> creates Node(15), returns to 20.
 *    - Recurse Right: build(4, 4) -> creates Node(7), returns to 20.
 *    - Returns Node(20) up to Node(3).
 * 
 * Final Result: Tree rooted at 3.
 *
 * Pitfalls: 
 * - Using local `preIndex` passed as a method parameter. In Java, primitives are passed by value. 
 *   If the left subtree consumes 3 elements from `pOrder`, the right subtree's stack frame won't 
 *   know that `preIndex` advanced! You MUST use an instance variable or a 1-element array.
 * - Getting the recursive boundaries wrong. Left is `inStart` to `inIndex - 1`. 
 *   Right is `inIndex + 1` to `inEnd`.
 *
 * Pattern Recognition: 
 * "When constructing a tree from traversals -> Pre/Post-order identifies the ROOT. Inorder identifies the SIZE of the subtrees."
 *
 * Interview Script:
 * "Preorder traversal always gives us the root of the current subtree at its current index. Once we 
 * know the root, we can find it in the inorder array. Everything left of that index forms the left 
 * subtree, and everything right forms the right subtree. To avoid O(N) searches for the root in the 
 * inorder array, I'll cache all inorder values and their indices in a HashMap upfront. We then 
 * recursively build the tree by passing logical boundaries. This takes O(N) time and O(N) space."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if the arrays could contain duplicate values?
 * A1: The problem becomes unsolvable for a definitive single tree. E.g., Preorder [1, 1], Inorder [1, 1]. 
 *     The second '1' could be a left child or a right child; both traversals would look identical.
 *
 * Q2: How would this change if you were given POSTORDER and INORDER arrays?
 * A2: Postorder is (Left-Right-Node). The root is at the VERY END of the array. The logic is identical, 
 *     but we initialize our global index to `postOrder.length - 1`, decrement it, and crucially, 
 *     we must build the RIGHT subtree before the LEFT subtree.
 */

public class BuildTreeFromTraversalsStudy {

    // Definition for a binary tree node.
    public static class TreeNode {
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

    /**
     * APPROACH 3: OPTIMAL HASHMAP + POINTERS (The professional standard)
     * Time: O(N) | Space: O(N)
     */
    
    // Global pointer tracking our progression through the pOrder array.
    // It must be global/instance so that deeper recursive calls advance the SAME pointer.
    private int preorderIndex;
    private Map<Integer, Integer> inorderIndexMap;

    public TreeNode buildTreeOptimal(int[] pOrder, int[] iOrder) {
        // Reset state for repeated calls in the same instance
        preorderIndex = 0;
        inorderIndexMap = new HashMap<>();

        // Pre-compute O(1) lookups for the inorder array
        // e.g., Value 15 is at index 2
        for (int i = 0; i < iOrder.length; i++) {
            inorderIndexMap.put(iOrder[i], i);
        }

        return constructSubtree(pOrder, 0, iOrder.length - 1);
    }

    private TreeNode constructSubtree(int[] pOrder, int inStart, int inEnd) {
        // Base case: If there are no elements to construct the tree from
        if (inStart > inEnd) {
            return null;
        }

        // 1. The current root is ALWAYS the next element in pOrder
        int rootValue = pOrder[preorderIndex];
        TreeNode root = new TreeNode(rootValue);
        
        // Advance the global pointer for the next recursive calls
        preorderIndex++;

        // 2. Find where this root is physically located in the inorder sequence
        int inIndex = inorderIndexMap.get(rootValue);

        // 3. Construct subtrees
        // Everything strictly to the left of inIndex goes to the left child
        root.left = constructSubtree(pOrder, inStart, inIndex - 1);
        
        // Everything strictly to the right of inIndex goes to the right child
        root.right = constructSubtree(pOrder, inIndex + 1, inEnd);

        return root;
    }


    /**
     * APPROACH 2: POINTERS WITH LINEAR SEARCH (Intermediate)
     * Time: O(N^2) | Space: O(N)
     * Included to demonstrate the O(N) bottleneck that the HashMap fixes.
     */
    private int naivePreIndex = 0;

    public TreeNode buildTreeNaive(int[] pOrder, int[] iOrder) {
        naivePreIndex = 0;
        return naiveConstruct(pOrder, iOrder, 0, iOrder.length - 1);
    }

    private TreeNode naiveConstruct(int[] pOrder, int[] iOrder, int inStart, int inEnd) {
        if (inStart > inEnd) return null;

        int rootVal = pOrder[naivePreIndex++];
        TreeNode root = new TreeNode(rootVal);

        // The Bottleneck: An O(N) linear scan inside every recursive call
        int inIndex = -1;
        for (int i = inStart; i <= inEnd; i++) {
            if (iOrder[i] == rootVal) {
                inIndex = i;
                break;
            }
        }

        root.left = naiveConstruct(pOrder, iOrder, inStart, inIndex - 1);
        root.right = naiveConstruct(pOrder, iOrder, inIndex + 1, inEnd);

        return root;
    }


    /**
     * Helper method to verify trees in main() via Postorder traversal.
     * If the tree was built correctly from Pre/In, its Postorder will match perfectly.
     */
    private static void printPostOrder(TreeNode node) {
        if (node == null) return;
        printPostOrder(node.left);
        printPostOrder(node.right);
        System.out.print(node.val + " ");
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        BuildTreeFromTraversalsStudy study = new BuildTreeFromTraversalsStudy();

        // Standard Case: 
        //       3
        //      / \
        //     9  20
        //       /  \
        //      15   7
        int[] pOrder1 = {3, 9, 20, 15, 7};
        int[] iOrder1 = {9, 3, 15, 20, 7};
        
        System.out.println("Standard Case Postorder verification:");
        TreeNode rootOptimal = study.buildTreeOptimal(pOrder1, iOrder1);
        System.out.print("Optimal: ");
        printPostOrder(rootOptimal); // Expected: 9 15 7 20 3
        System.out.println();

        TreeNode rootNaive = study.buildTreeNaive(pOrder1, iOrder1);
        System.out.print("Naive:   ");
        printPostOrder(rootNaive); // Expected: 9 15 7 20 3
        System.out.println("\n");

        // Edge Case 1: Single Node
        int[] pOrder2 = {42};
        int[] iOrder2 = {42};
        System.out.print("Single Node (Optimal): ");
        printPostOrder(study.buildTreeOptimal(pOrder2, iOrder2)); // Expected: 42
        System.out.println();

        // Edge Case 2: Fully Left-Skewed Tree
        //     1
        //    /
        //   2
        //  /
        // 3
        int[] pOrder3 = {1, 2, 3};
        int[] iOrder3 = {3, 2, 1}; // Inorder of a left-skewed tree is reverse of preorder
        System.out.print("Left-Skewed (Optimal): ");
        printPostOrder(study.buildTreeOptimal(pOrder3, iOrder3)); // Expected: 3 2 1
        System.out.println();
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Traversal Divide & Conquer.
 * Key Observation: Preorder[0] is the root. Finding that root in Inorder splits the 
 *                  problem into definitive left and right subtree boundaries.
 * Memorize: Cache Inorder indices in a HashMap `Map<Value, Index>`. Use a global `preIndex`. 
 *           `root.left = build(inStart, inIndex - 1);`
 * Most Common Trap: Trying to pass the `preorderIndex` as a primitive int parameter in 
 *                   the recursive function. Sibling branches will overwrite each other's 
 *                   progress. It MUST be an instance variable.
 * One-Line Mental Trigger: "Preorder gives the Root, Inorder gives the Size. Hash the Inorder."
 * ============================================================================
 */

import ds_v1.BinaryTree.TreeNode;
import java.util.*;

public class Solution {

    public static TreeNode<Integer> buildTree(int[] preorder, int[] inorder) {

        // Map value -> index in inorder for O(1) lookup
        Map<Integer, Integer> inorderIndexMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderIndexMap.put(inorder[i], i);
        }

        return build(preorder, 0, preorder.length - 1,
                     inorder, 0, inorder.length - 1,
                     inorderIndexMap);
    }

    private static TreeNode<Integer> build(int[] preorder,
                                           int preStart,
                                           int preEnd,
                                           int[] inorder,
                                           int inStart,
                                           int inEnd,
                                           Map<Integer, Integer> inorderMap) {

        // Base case: no elements
        if (preStart > preEnd || inStart > inEnd) return null;

        /*
         * 🔥 KEY IDEA:
         * preorder[preStart] is ALWAYS the root
         */
        int rootVal = preorder[preStart];
        TreeNode<Integer> root = new TreeNode<>(rootVal);

        /*
         * Find root index in inorder
         * This splits inorder into:
         * LEFT  subtree  -> inStart ... rootIdx-1
         * RIGHT subtree  -> rootIdx+1 ... inEnd
         */
        int rootIdx = inorderMap.get(rootVal);

        /*
         * Number of nodes in left subtree
         * This helps us split preorder correctly
         */
        int leftSize = rootIdx - inStart;

        /*
         * 🔁 Recursively build LEFT subtree
         *
         * preorder:
         *   skip root → start from preStart+1
         *   take leftSize elements
         *
         * inorder:
         *   left portion
         */
        root.left = build(preorder,
                          preStart + 1,
                          preStart + leftSize,
                          inorder,
                          inStart,
                          rootIdx - 1,
                          inorderMap);

        /*
         * 🔁 Recursively build RIGHT subtree
         *
         * preorder:
         *   skip root + left subtree
         *
         * inorder:
         *   right portion
         */
        root.right = build(preorder,
                           preStart + leftSize + 1,
                           preEnd,
                           inorder,
                           rootIdx + 1,
                           inEnd,
                           inorderMap);

        return root;
    }
}

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ============================================================================
 * STUDY NOTE: DELETE NODES AND RETURN FOREST
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Can the original root of the tree be one of the deleted nodes?" 
 *   (Crucial: If the root is deleted, we must not add it to the final forest list, meaning the wrapper function needs a special check).
 * - "Is the `deleteNodes` array guaranteed to only contain values that actually exist in the tree?" 
 *   (Defensive programming: handling values that aren't in the tree ensures we don't crash or behave unexpectedly).
 * - "Does the order of the roots in the returned list matter?" 
 *   (The prompt says "in any order," so we can safely add new roots dynamically during the traversal without sorting).
 * - "Since node values are strictly bounded [1, 1000], can I use a boolean array instead of a HashSet for lookups?" 
 *   (This flags you as an engineer who reads constraints carefully to optimize performance over default object collections).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * Deleting a node does two things simultaneously: it severs the connection from its parent, 
 * and it promotes its surviving children into brand-new independent roots. The bottleneck 
 * is severing the parent-to-child pointer cleanly without requiring a secondary traversal 
 * or maintaining a messy map of parent pointers.
 *
 * Approach 1: Top-Down DFS with Parent Pointers (The Intuitive Mess)
 * What I'd naturally try: I'd pass the `parent` node and an `isLeftChild` boolean down the 
 * recursive calls. When I hit a node that needs deletion, I'd say: `if (isLeftChild) parent.left = null;`. 
 * Then, I'd add its non-null children to the results list.
 * Why it works: It manually snips the wires from above.
 * Why it's costly and error-prone: Managing parent state is incredibly messy. What if the root 
 * itself is deleted? It has no parent, requiring a messy edge-case `if (parent != null)` check 
 * everywhere. 
 * - Time Complexity: O(N * D) — because scanning the raw `deleteNodes` array takes O(D) time for every single node.
 * - Space Complexity: O(H) — bounded by the recursion stack.
 * 
 * Approach 2: Bottom-Up Post-Order DFS with HashSet (The Elegant Standard)
 * What I'd naturally try: Instead of the parent reaching down to snip the wire, what if the child 
 * just reports back to the parent whether it survived? I can convert `deleteNodes` to a HashSet. 
 * Using a Post-Order traversal (children first), a child returns its own node to the parent. 
 * If it was deleted, it returns `null`. The parent simply accepts the return value: `node.left = dfs(node.left)`.
 * Why it works: It completely eliminates the need to track parent pointers. The tree dynamically 
 * rewires itself as the recursion unwinds.
 * What property removes the bottleneck: Leveraging the return value of the recursive function to 
 * naturally prune dead branches.
 * - Time Complexity: O(N + D) — because we dump the D delete nodes into a HashSet, and visit N nodes exactly once with O(1) lookups.
 * - Space Complexity: O(N + D) — because the HashSet requires O(D) space, the result list requires O(N) space, and the call stack takes O(H) space.
 *
 * Approach 3: Bottom-Up DFS with Boolean Array (The Performance Optimization)
 * What I'd naturally try: The constraints state that node values never exceed 1000. A HashSet 
 * allocates Object wrappers (Integer) and computes hash codes. I can replace it entirely with 
 * a simple `boolean[1001]` array, where `toDelete[val] = true`.
 * Why it works: Array index lookups are the fastest possible O(1) operation on a CPU, completely 
 * bypassing the memory overhead of the Java Collections Framework.
 * What property removes the bottleneck: Exploiting the tightly bounded constraints of the problem 
 * domain to shift from heap-allocated objects to a primitive lookup table.
 * - Time Complexity: O(N + D) — same asymptotic time, but with a massive constant-factor speedup.
 * - Space Complexity: O(H) for the call stack, plus a strict O(1) space (exactly 1001 booleans) for the lookup table, regardless of how many nodes are deleted.
 *
 * The Interview Choice:
 * Always write Approach 3 (Bottom-Up DFS with Boolean Array). It demonstrates an elite combination 
 * of algorithmic elegance (Post-Order self-pruning) and systems-level optimization (primitive arrays 
 * over HashSets).
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Root itself is deleted -> The wrapper method must manually check if the returned root is null before adding it.
 * 2. Cascading deletions -> A parent AND its child are both deleted. Post-order handles this flawlessly by processing the child first.
 * 3. Empty `deleteNodes` array -> Code should just return the original root as a single-item list.
 * 4. Tree becomes entirely empty -> Every node is deleted, should return an empty list.
 *
 *
 * 4. DRY RUN (Bottom-Up Post-Order DFS)
 * ----------------------------------------------------------------------------
 * Tree: 
 *       1
 *      / \
 *     2   3
 *    / \   \
 *   4   5   6
 * 
 * deleteNodes: [3, 5]
 * Boolean array: toDelete[3]=true, toDelete[5]=true
 * 
 * Trace (Post-Order = process leaves first):
 * 1. dfs(4): toDelete[4] is false. Returns Node(4).
 * 2. dfs(5): toDelete[5] is TRUE! 
 *    - Node 5 is deleted. It has no children. 
 *    - Returns `null`.
 * 3. dfs(2): receives left=Node(4), right=`null` (from 5). 
 *    - Node 2 is safe. Returns Node(2).
 * 4. dfs(6): toDelete[6] is false. Returns Node(6).
 * 5. dfs(3): toDelete[3] is TRUE!
 *    - Node 3 is deleted. Its left is null, right is Node(6).
 *    - Because Node 3 is deleted, any non-null child becomes a new root! 
 *    - Adds Node(6) to `forest`.
 *    - Returns `null`.
 * 6. dfs(1): receives left=Node(2), right=`null` (from 3).
 *    - Node 1 is safe. Returns Node(1).
 * 7. Main Wrapper: Root(1) returned safely. Adds Node(1) to `forest`.
 * 
 * Final Forest List: [Node(6), Node(1)].
 *
 * Pitfalls: 
 * - Using Pre-Order (Top-Down) instead of Post-Order. If you evaluate a node's deletion before 
 *   processing its children, you must manually pass down a "is parent deleted" flag to let the 
 *   children know if they are new roots. Post-order is vastly cleaner.
 * - Forgetting the main wrapper check: `if (dfs(root) != null)` -> add to forest. The recursion 
 *   only adds *children* of deleted nodes to the forest. It never evaluates adding the absolute 
 *   top-level root.
 *
 * Pattern Recognition: 
 * "When a tree needs to be modified or pruned from the bottom up (e.g., removing leaves, severing 
 * branches) -> Think Post-Order DFS, using the return value to rewire the parent's pointers."
 *
 * Interview Script:
 * "To avoid the mess of tracking parent pointers, I'll use a bottom-up Post-Order traversal. A child 
 * will evaluate itself and return its own node to the parent, or return null if it's slated for deletion. 
 * The parent simply assigns `left = dfs(left)`. If a node is deleted, it checks if it has surviving 
 * children, and if so, adds them to the forest result list. Because the node constraints are tight 
 * (up to 1000), I'll use a boolean array instead of a HashSet for O(1) lookups to save memory overhead."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if the node values were unbounded (e.g., full 32-bit integers)?
 * A1: The boolean array optimization fails due to memory limits. I would revert to using a `HashSet<Integer>` 
 *     to store the `deleteNodes`, restoring O(D) space complexity while maintaining O(1) lookups.
 *
 * Q2: What if we needed to return the forest sorted by the root node values?
 * A2: I would change the `List<TreeNode>` to a `PriorityQueue<TreeNode>` ordered by `node.val`. 
 *     At the end, I would poll all elements into a List. This adds an O(K log K) factor where K 
 *     is the number of remaining roots.
 */

public class DeleteNodesReturnForestStudy {

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
     * APPROACH 3: BOTTOM-UP DFS WITH BOOLEAN ARRAY (The Optimal / Production Choice)
     * Time: O(N + D) | Space: O(H) for stack + O(1) lookup array
     */
    public List<TreeNode> delNodesOptimal(TreeNode root, int[] to_delete) {
        List<TreeNode> forest = new ArrayList<>();
        
        // Optimization: Use a primitive array instead of HashSet because max value is 1000.
        // This grants ultra-fast O(1) lookups with strict O(1) space.
        boolean[] toDeleteMap = new boolean[1001];
        for (int val : to_delete) {
            toDeleteMap[val] = true;
        }

        // Kick off the post-order traversal
        TreeNode finalRoot = processNodeOptimal(root, toDeleteMap, forest);

        // The recursive function adds orphans to the forest. 
        // It does NOT add the absolute top-level root, so we must check it manually here.
        if (finalRoot != null) {
            forest.add(finalRoot);
        }

        return forest;
    }

    private TreeNode processNodeOptimal(TreeNode node, boolean[] toDeleteMap, List<TreeNode> forest) {
        // Base case: hit a null leaf pointer
        if (node == null) {
            return null;
        }

        // 1. Traverse Left and Right first (Post-Order)
        // The children will return 'null' if they were deleted, seamlessly rewiring the tree.
        node.left = processNodeOptimal(node.left, toDeleteMap, forest);
        node.right = processNodeOptimal(node.right, toDeleteMap, forest);

        // 2. Process the Current Node
        if (toDeleteMap[node.val]) {
            // This node is slated for deletion.
            // If its children survived (they didn't return null), they are now orphans.
            // Promote them to roots by adding them to the forest.
            if (node.left != null) {
                forest.add(node.left);
            }
            if (node.right != null) {
                forest.add(node.right);
            }
            
            // Return null to tell OUR parent that we no longer exist
            return null;
        }

        // If not deleted, return ourselves so our parent can maintain the connection
        return node;
    }


    /**
     * APPROACH 2: BOTTOM-UP DFS WITH HASHSET (The Standard Solution)
     * Time: O(N + D) | Space: O(H) + O(D)
     * Included to show how to handle unbounded values if constraints were larger.
     */
    public List<TreeNode> delNodesHashSet(TreeNode root, int[] to_delete) {
        List<TreeNode> forest = new ArrayList<>();
        Set<Integer> toDeleteSet = new HashSet<>();
        
        for (int val : to_delete) {
            toDeleteSet.add(val);
        }

        if (processNodeHashSet(root, toDeleteSet, forest) != null) {
            forest.add(root);
        }

        return forest;
    }

    private TreeNode processNodeHashSet(TreeNode node, Set<Integer> toDeleteSet, List<TreeNode> forest) {
        if (node == null) return null;

        node.left = processNodeHashSet(node.left, toDeleteSet, forest);
        node.right = processNodeHashSet(node.right, toDeleteSet, forest);

        if (toDeleteSet.contains(node.val)) {
            if (node.left != null) forest.add(node.left);
            if (node.right != null) forest.add(node.right);
            return null;
        }

        return node;
    }


    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        DeleteNodesReturnForestStudy study = new DeleteNodesReturnForestStudy();

        // Standard Case: The example from the DRY RUN
        //       1
        //      / \
        //     2   3
        //    / \   \
        //   4   5   6
        TreeNode root1 = new TreeNode(1,
                new TreeNode(2, new TreeNode(4), new TreeNode(5)),
                new TreeNode(3, null, new TreeNode(6))
        );
        int[] delete1 = {3, 5};
        
        System.out.println("Standard Case (Optimal):");
        List<TreeNode> forest1 = study.delNodesOptimal(root1, delete1);
        for (TreeNode newRoot : forest1) {
            System.out.print(newRoot.val + " "); // Expected output order varies, but should contain 6, 1
        }
        System.out.println();


        // Edge Case 1: Root itself is deleted
        //   1 (del)
        //  / \
        // 2   3
        TreeNode root2 = new TreeNode(1, new TreeNode(2), new TreeNode(3));
        int[] delete2 = {1};
        
        System.out.println("\nRoot Deleted Case (Optimal):");
        List<TreeNode> forest2 = study.delNodesOptimal(root2, delete2);
        for (TreeNode newRoot : forest2) {
            System.out.print(newRoot.val + " "); // Expected: 2, 3
        }
        System.out.println();


        // Edge Case 2: Cascading Deletions (Parent and Child both deleted)
        //     1 (del)
        //    /
        //   2 (del)
        //  /
        // 3
        TreeNode root3 = new TreeNode(1, new TreeNode(2, new TreeNode(3), null), null);
        int[] delete3 = {1, 2};
        
        System.out.println("\nCascading Deletions (Optimal):");
        List<TreeNode> forest3 = study.delNodesOptimal(root3, delete3);
        for (TreeNode newRoot : forest3) {
            System.out.print(newRoot.val + " "); // Expected: 3
        }
        System.out.println();
        
        
        // Edge Case 3: Empty Delete array
        TreeNode root4 = new TreeNode(42);
        int[] delete4 = {};
        System.out.println("\nNo Deletions (Optimal):");
        List<TreeNode> forest4 = study.delNodesOptimal(root4, delete4);
        for (TreeNode newRoot : forest4) {
            System.out.print(newRoot.val + " "); // Expected: 42
        }
        System.out.println();
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Bottom-Up (Post-Order) DFS with Return-Value Rewiring.
 * Key Observation: A parent does not need to know if its child was deleted beforehand. 
 *                  If the child processes itself first, it can just return 'null' to the 
 *                  parent, and the parent blindly assigns `left = dfs(left)`.
 * Memorize: `node.left = dfs(node.left); if (deleted) { add children to forest; return null; } return node;`
 * Most Common Trap: Using Pre-Order traversal. If you process the parent first, you must 
 *                   pass messy boolean flags down to the children so they know they are now 
 *                   roots. Post-order completely eliminates this state-tracking.
 * One-Line Mental Trigger: "Delete Tree Nodes? Post-order DFS. Deleted nodes return null."
 * ============================================================================
 */

import java.util.*;

/**
 * Definition for binary tree node.
 */
class TreeNode {
    int val;
    TreeNode left, right;

    TreeNode(int val) {
        this.val = val;
    }
}

public class DeleteNodesAndReturnForest {

    /**
     * Main function to delete nodes and return forest
     */
    public List<TreeNode> delNodes(TreeNode root, int[] deleteNodes) {

        /**
         * Step 1: Convert deleteNodes array into a HashSet
         * Why?
         * -> We need O(1) lookup to check if a node should be deleted
         */
        Set<Integer> deleteSet = new HashSet<>();
        for (int val : deleteNodes) {
            deleteSet.add(val);
        }

        /**
         * This will store roots of all trees in the resulting forest
         */
        List<TreeNode> forest = new ArrayList<>();

        /**
         * Step 2: Handle root separately
         *
         * If root is NOT deleted → it remains a valid tree root
         * If root IS deleted → we don't add it now,
         * children (if any) will be added during DFS
         */
        if (!deleteSet.contains(root.val)) {
            forest.add(root);
        }

        /**
         * Step 3: Start DFS traversal
         */
        dfs(root, deleteSet, forest);

        return forest;
    }

    /**
     * DFS function (Postorder traversal)
     *
     * VERY IMPORTANT:
     * We process children FIRST, then decide about current node
     *
     * Why Postorder?
     * -> Because if current node is deleted,
     *    we must already know its updated children
     *
     * @return:
     *   - null → if this node is deleted (parent should disconnect it)
     *   - node → if this node remains in tree
     */
    private TreeNode dfs(TreeNode node, Set<Integer> deleteSet, List<TreeNode> forest) {

        /**
         * Base Case:
         * If node is null → nothing to process
         */
        if (node == null) return null;

        /**
         * Step 1: Recursively process left and right subtree
         *
         * IMPORTANT:
         * We update node.left and node.right directly
         * because child might get deleted (return null)
         */
        node.left = dfs(node.left, deleteSet, forest);
        node.right = dfs(node.right, deleteSet, forest);

        /**
         * Step 2: Check if current node should be deleted
         */
        if (deleteSet.contains(node.val)) {

            /**
             * If current node is deleted:
             *
             * Its children (if exist) become NEW ROOTS in forest
             */

            if (node.left != null) {
                forest.add(node.left); // left child becomes new tree
            }

            if (node.right != null) {
                forest.add(node.right); // right child becomes new tree
            }

            /**
             * Return null to parent:
             * -> This effectively removes current node
             * -> Parent will disconnect this node
             */
            return null;
        }

        /**
         * If node is NOT deleted → return it as-is
         */
        return node;
    }
}


import java.util.*;

/**
 * Delete Nodes and Return Forest
 *
 * Given the root of a binary tree with unique values and a list of values
 * to delete, delete those nodes and return the roots of all remaining trees.
 *
 * ------------------------------------------------------------
 * Example
 * ------------------------------------------------------------
 *
 * Input:
 *
 *              1
 *            /   \
 *           2     3
 *          / \   / \
 *         4   5 6   7
 *
 * deleteNodes = [3, 5]
 *
 * After deleting 3:
 *
 *              1
 *             /
 *            2
 *           / \
 *          4   5
 *
 * Node 6 and 7 become separate trees.
 *
 * After deleting 5:
 *
 *              1          6          7
 *             / 
 *            2
 *           /
 *          4
 *
 * Forest roots = [1, 6, 7]
 *
 * ------------------------------------------------------------
 *
 * Main idea:
 *
 * When processing a node:
 *
 * 1. First process its children.
 * 2. Then decide whether the current node should be deleted.
 *
 * Why postorder?
 *
 * Suppose we delete node 3.
 *
 *              3
 *             / \
 *            6   7
 *
 * We need 6 and 7 as new forest roots.
 *
 * Therefore, while deleting 3, we need to know about its children first.
 *
 * This naturally leads to POSTORDER traversal:
 *
 *      left -> right -> current
 *
 * ------------------------------------------------------------
 */
public class DeleteNodesAndReturnForest {

    // ------------------------------------------------------------
    // Tree Node
    // ------------------------------------------------------------

    static class TreeNode {
        int value;
        TreeNode left;
        TreeNode right;

        TreeNode(int value) {
            this.value = value;
        }

        TreeNode(int value, TreeNode left, TreeNode right) {
            this.value = value;
            this.left = left;
            this.right = right;
        }
    }

    // ============================================================
    // APPROACH 1: Recursive Postorder DFS
    // ============================================================
    //
    // This is the cleanest and most common solution.
    //
    // Time:  O(N)
    // Space: O(N) in the worst case
    //        O(H) recursion stack + O(K) delete set/result
    //
    // N = number of nodes
    // H = tree height
    // K = number of nodes to delete
    //
    // ------------------------------------------------------------
    //
    // Important idea:
    //
    // We pass an extra boolean:
    //
    //     parentDeleted
    //
    // It tells us whether the parent of the current node was deleted.
    //
    // If:
    //
    //     parentDeleted == true
    //     current node is NOT deleted
    //
    // then current node becomes a NEW ROOT of the forest.
    //
    // ------------------------------------------------------------

    static class RecursiveSolution {

        private final Set<Integer> nodesToDelete = new HashSet<>();
        private final List<TreeNode> forestRoots = new ArrayList<>();

        public List<TreeNode> deleteNodes(
                TreeNode root,
                int[] deleteNodes
        ) {
            // Put all values to delete into a HashSet.
            //
            // This gives O(1) average-time lookup.
            for (int value : deleteNodes) {
                nodesToDelete.add(value);
            }

            // The original root has no parent.
            //
            // We can conceptually treat its parent as deleted so that
            // if root itself is NOT deleted, it gets added to the forest.
            processNode(root, true);

            return forestRoots;
        }

        /**
         * Postorder DFS.
         *
         * Returns:
         *     The current node if it survives deletion.
         *     null if the current node is deleted.
         *
         * parentDeleted tells us whether the current node is
         * starting a new tree in the forest.
         */
        private TreeNode processNode(
                TreeNode currentNode,
                boolean parentDeleted
        ) {
            if (currentNode == null) {
                return null;
            }

            boolean currentNodeDeleted =
                    nodesToDelete.contains(currentNode.value);

            // ----------------------------------------------------
            // Process children first.
            // ----------------------------------------------------
            //
            // If current node is deleted, then its surviving
            // children need to become roots of separate trees.
            //
            currentNode.left = processNode(
                    currentNode.left,
                    currentNodeDeleted
            );

            currentNode.right = processNode(
                    currentNode.right,
                    currentNodeDeleted
            );

            // ----------------------------------------------------
            // Current node survives.
            // ----------------------------------------------------
            //
            // If its parent was deleted, this node has now become
            // the root of a new tree.
            //
            if (!currentNodeDeleted) {

                if (parentDeleted) {
                    forestRoots.add(currentNode);
                }

                return currentNode;
            }

            // ----------------------------------------------------
            // Current node is deleted.
            // ----------------------------------------------------
            //
            // Returning null makes the parent disconnect from this
            // node.
            //
            return null;
        }
    }

    // ============================================================
    // APPROACH 2: Recursive DFS Using "Is Root" Concept
    // ============================================================
    //
    // This version uses a slightly different mental model:
    //
    //     isRoot = "Could this node become a root of a tree?"
    //
    // At the beginning:
    //
    //     root is a possible root.
    //
    // For children:
    //
    //     If parent survives:
    //         child is NOT a root.
    //
    //     If parent is deleted:
    //         child CAN become a root.
    //
    // This is essentially the same algorithm but can be easier
    // to explain in an interview.
    //
    // Time:  O(N)
    // Space: O(N) worst case
    // ============================================================

    static class IsRootSolution {

        private Set<Integer> nodesToDelete;
        private List<TreeNode> forestRoots;

        public List<TreeNode> deleteNodes(
                TreeNode root,
                int[] deleteNodes
        ) {
            nodesToDelete = new HashSet<>();

            for (int value : deleteNodes) {
                nodesToDelete.add(value);
            }

            forestRoots = new ArrayList<>();

            removeNodes(root, true);

            return forestRoots;
        }

        /**
         * isRoot means:
         *
         * "If this node survives, should it be added to the answer?"
         */
        private TreeNode removeNodes(
                TreeNode currentNode,
                boolean isRoot
        ) {
            if (currentNode == null) {
                return null;
            }

            boolean shouldDelete =
                    nodesToDelete.contains(currentNode.value);

            // If current node is deleted, its children should
            // be considered as possible roots.
            boolean childrenShouldBeRoots = shouldDelete;

            currentNode.left = removeNodes(
                    currentNode.left,
                    childrenShouldBeRoots
            );

            currentNode.right = removeNodes(
                    currentNode.right,
                    childrenShouldBeRoots
            );

            // Current node survives.
            if (!shouldDelete) {

                if (isRoot) {
                    forestRoots.add(currentNode);
                }

                return currentNode;
            }

            // Current node is deleted.
            return null;
        }
    }

    // ============================================================
    // APPROACH 3: Iterative Postorder Traversal
    // ============================================================
    //
    // Useful if recursion depth could become a problem.
    //
    // The recursive solution is simpler, but in Java a highly
    // skewed tree with a very large number of nodes could cause
    // StackOverflowError.
    //
    // Here we explicitly maintain the traversal state.
    //
    // However, for this problem's constraint of <= 100 nodes,
    // recursion is completely reasonable.
    //
    // This implementation uses:
    //
    //     Stack<TreeNode>
    //     Map<TreeNode, TreeNode>
    //
    // to simulate parent relationships and process nodes.
    //
    // Time:  O(N)
    // Space: O(N)
    // ============================================================

    static class IterativeSolution {

        public List<TreeNode> deleteNodes(
                TreeNode root,
                int[] deleteNodes
        ) {
            List<TreeNode> forestRoots = new ArrayList<>();

            if (root == null) {
                return forestRoots;
            }

            Set<Integer> nodesToDelete = new HashSet<>();

            for (int value : deleteNodes) {
                nodesToDelete.add(value);
            }

            /*
             * We use two stacks to generate a postorder traversal.
             *
             * First stack:
             *     Creates a reverse-postorder ordering.
             *
             * Second stack:
             *     Reverses it into postorder.
             */
            Stack<TreeNode> traversalStack = new Stack<>();
            Stack<TreeNode> postorderStack = new Stack<>();

            traversalStack.push(root);

            while (!traversalStack.isEmpty()) {
                TreeNode currentNode = traversalStack.pop();

                postorderStack.push(currentNode);

                if (currentNode.left != null) {
                    traversalStack.push(currentNode.left);
                }

                if (currentNode.right != null) {
                    traversalStack.push(currentNode.right);
                }
            }

            /*
             * Parent information is needed because after deleting
             * a node, its surviving children become forest roots.
             *
             * Build a parent map first.
             */
            Map<TreeNode, TreeNode> parentMap = new HashMap<>();

            Stack<TreeNode> stack = new Stack<>();
            stack.push(root);

            while (!stack.isEmpty()) {
                TreeNode currentNode = stack.pop();

                if (currentNode.left != null) {
                    parentMap.put(currentNode.left, currentNode);
                    stack.push(currentNode.left);
                }

                if (currentNode.right != null) {
                    parentMap.put(currentNode.right, currentNode);
                    stack.push(currentNode.right);
                }
            }

            /*
             * Process nodes in postorder.
             *
             * Because children are processed before their parent,
             * when we delete a node, its child references have
             * already been updated.
             */
            Set<TreeNode> deletedNodes = new HashSet<>();

            while (!postorderStack.isEmpty()) {
                TreeNode currentNode = postorderStack.pop();

                if (!nodesToDelete.contains(currentNode.value)) {
                    continue;
                }

                deletedNodes.add(currentNode);

                // Disconnect from parent.
                TreeNode parent = parentMap.get(currentNode);

                if (parent != null) {
                    if (parent.left == currentNode) {
                        parent.left = null;
                    }

                    if (parent.right == currentNode) {
                        parent.right = null;
                    }
                }
            }

            /*
             * Now find all surviving nodes whose parent was deleted.
             *
             * Such nodes are roots of the forest.
             */
            for (TreeNode currentNode : parentMap.keySet()) {

                if (deletedNodes.contains(currentNode)) {
                    continue;
                }

                TreeNode parent = parentMap.get(currentNode);

                if (deletedNodes.contains(parent)) {
                    forestRoots.add(currentNode);
                }
            }

            /*
             * The original root is also a forest root if it survives.
             */
            if (!deletedNodes.contains(root)) {
                forestRoots.add(root);
            }

            return forestRoots;
        }
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    /**
     * Prints a tree in preorder.
     *
     * Useful for verifying the forest.
     */
    static void printPreorder(TreeNode root) {
        if (root == null) {
            return;
        }

        System.out.print(root.value + " ");
        printPreorder(root.left);
        printPreorder(root.right);
    }

    /**
     * Prints every tree in the forest.
     */
    static void printForest(List<TreeNode> forestRoots) {
        System.out.println("Forest:");

        for (TreeNode root : forestRoots) {
            System.out.print("Tree rooted at " + root.value + ": ");
            printPreorder(root);
            System.out.println();
        }
    }

    // ============================================================
    // Example / Dry Run
    // ============================================================

    public static void main(String[] args) {

        /*
         * Build:
         *
         *              1
         *            /   \
         *           2     3
         *          / \   / \
         *         4   5 6   7
         */

        TreeNode root =
                new TreeNode(
                        1,
                        new TreeNode(
                                2,
                                new TreeNode(4),
                                new TreeNode(5)
                        ),
                        new TreeNode(
                                3,
                                new TreeNode(6),
                                new TreeNode(7)
                        )
                );

        int[] deleteNodes = {3, 5};

        RecursiveSolution solution = new RecursiveSolution();

        List<TreeNode> forest =
                solution.deleteNodes(root, deleteNodes);

        printForest(forest);

        /*
         * Expected:
         *
         * Forest:
         * Tree rooted at 6: 6
         * Tree rooted at 7: 7
         * Tree rooted at 1: 1 2 4
         *
         * Order does not matter.
         */
    }
}


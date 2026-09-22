import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * STUDY NOTE: BOUNDARY OF BINARY TREE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "If the tree has only one node, does it count as the root, a leaf, or both?" 
 *   (Crucial: The prompt says the root is 'never considered a leaf in this problem', but if it's the ONLY node, we must return just `[root.val]` to avoid duplicating it).
 * - "Does the boundary mean the 'visual perimeter' (e.g., the leftmost/rightmost node at each horizontal level), or specific structural paths?" 
 *   (Determines the entire algorithm. The prompt defines it rigidly by structural paths: root -> left child path -> leaves -> right child path. A node might be visually on the perimeter but not part of these paths).
 * - "If a node on the left boundary has no left child, do we follow its right child?" 
 *   (Yes, the prompt specifies falling back to the right child. This zig-zagging is what makes a simple DFS tricky).
 * - "Should the left and right boundaries overlap with the leaves?" 
 *   (No, the prompt explicitly states leaves are excluded from the left and right boundary paths).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * The "boundary" is not a single contiguous line. It is a Frankenstein sequence stitched 
 * together from 4 distinct rules (Root, Left-Path, Leaves, Right-Path-Reversed). 
 * The bottleneck is trying to satisfy all 4 conflicting rules simultaneously without accidentally 
 * duplicating nodes or missing zig-zagging path elements.
 *
 * Approach 1: Level-Order Traversal (The BFS Visual Perimeter Trap)
 * What I'd naturally try: When I hear "boundary", I picture the visual outline of the tree. 
 * I'd use a BFS (Queue). For every level, I grab the first node (left boundary) and the last 
 * node (right boundary).
 * Why it's flawed: The problem doesn't ask for a visual outline; it asks for strict structural paths. 
 * If the left boundary zig-zags inwards, a BFS might grab an entirely unrelated leaf node just because 
 * it happens to be the leftmost node on that horizontal level.
 * - Time Complexity: O(N) — because we visit every node level-by-level.
 * - Space Complexity: O(N) — because the Queue stores the widest level of the tree.
 * 
 * Approach 2: Monolithic Pre-order DFS (The Overcomplicated State Machine)
 * What I'd naturally try: I know DFS can trace paths. I'll write one massive recursive function: 
 * `dfs(node, isLeftBoundary, isRightBoundary)`. If `isLeft`, I add the node pre-order. If `isRight`, 
 * I add it post-order. If it's a leaf, I add it.
 * Why it works: It traverses the tree exactly once, using flags to turn behaviors on and off.
 * Why it's too costly/buggy: State management becomes a nightmare. If you are on the left boundary, 
 * but there is no left child, you must pass `isLeftBoundary = true` down to the RIGHT child. But if 
 * you aren't on the boundary, you pass `false`. The branching logic becomes so dense it is nearly 
 * impossible to write bug-free in 45 minutes.
 * - Time Complexity: O(N) — because every node is visited exactly once.
 * - Space Complexity: O(H) — bounded strictly by the height of the recursive call stack.
 *
 * Approach 3: Modular Path Decomposition (The Optimal & Clean Standard)
 * What I'd naturally try: Stop trying to be clever with a single pass. The problem defines exactly 
 * 4 discrete components. I will write 4 separate, dead-simple helper blocks:
 *   1. Add Root.
 *   2. Trace Left Boundary (Iteratively going left, fallback right).
 *   3. Collect all Leaves (Standard recursive DFS).
 *   4. Trace Right Boundary (Iteratively going right, fallback left, reverse it).
 * Why it works: Separation of concerns. Each helper strictly obeys one rule of the prompt without 
 * interfering with the others.
 * What property removes the bottleneck: By doing three targeted passes (left spine, whole tree for 
 * leaves, right spine), we trade a tiny bit of theoretical redundancy (visiting the spines twice) 
 * for massive architectural clarity.
 * - Time Complexity: O(N) — because the left boundary takes O(H), leaves take O(N), and right 
 *   boundary takes O(H). O(H) + O(N) + O(H) simplifies to O(N).
 * - Space Complexity: O(N) — because the recursion stack for the leaves is O(H), and the output 
 *   list requires O(N) space.
 *
 * The Interview Choice:
 * Always write Approach 3 (Modular Decomposition). It shows maturity. Interviewers hate "clever" 
 * unreadable code. Writing 3 tiny, perfectly scoped helper functions proves you know how to break 
 * complex business logic into maintainable pieces.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Single Node Tree -> The root is a leaf. It must be added exactly once.
 * 2. Left-Skewed Tree (Linked List) -> Left boundary collects everything except the final leaf. 
 *    Right boundary is entirely empty.
 * 3. Zig-Zag Boundary -> E.g., Root -> Left -> Right -> Left. The loop must fallback correctly.
 *
 *
 * 4. DRY RUN (Modular Decomposition)
 * ----------------------------------------------------------------------------
 * Tree:
 *         1
 *        / \
 *       2   3
 *        \   \
 *         4   5
 *        /     \
 *       6       7
 * 
 * 1. Root: Add 1. Result = [1].
 * 
 * 2. Left Boundary (starts at root.left = 2):
 *    - curr = 2 (not leaf). Add 2. Left is null, fallback to Right -> curr = 4.
 *    - curr = 4 (not leaf). Add 4. Left is 6 -> curr = 6.
 *    - curr = 6 (IS LEAF). Ignore it! Loop breaks.
 *    - Result = [1, 2, 4].
 * 
 * 3. Leaves (starts at root = 1):
 *    - DFS finds all leaves left-to-right.
 *    - Finds 6. Add 6.
 *    - Finds 7. Add 7.
 *    - Result = [1, 2, 4, 6, 7].
 * 
 * 4. Right Boundary (starts at root.right = 3):
 *    - curr = 3 (not leaf). Temp = [3]. Right is 5 -> curr = 5.
 *    - curr = 5 (not leaf). Temp = [3, 5]. Right is 7 -> curr = 7.
 *    - curr = 7 (IS LEAF). Ignore it! Loop breaks.
 *    - Reverse Temp and append. Temp reversed = [5, 3].
 *    - Result = [1, 2, 4, 6, 7, 5, 3].
 *
 * Pitfalls: 
 * - Accidentally including leaves in the boundary paths. You MUST check `!isLeaf(curr)` before adding.
 * - Collecting the right boundary top-down and forgetting to reverse it. It MUST be bottom-up.
 *
 * Pattern Recognition: 
 * "When a problem defines a rigid set of conflicting rules for different parts of a data structure -> 
 *  Decompose the algorithm into independent passes rather than a monolithic state machine."
 *
 * Interview Script:
 * "Trying to do this in a single DFS requires complex flags that are prone to bugs. Instead, since 
 * the prompt explicitly defines four parts of the boundary, I'll modularize the solution. I'll add 
 * the root, trace the left path iteratively, run a standard DFS to find leaves, and trace the right 
 * path iteratively before reversing it. This keeps the logic perfectly isolated, running in O(N) 
 * total time and O(N) space."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: How would you change this if we wanted the boundary collected counter-clockwise?
 * A1: The algorithm as designed already IS counter-clockwise! Root -> Left -> Leaves (L to R) -> Right (Bottom to Top).
 *
 * Q2: What if we wanted the "Visual Silhouette" instead of this structural path?
 * A2: I would switch to BFS. For every horizontal level, I'd put the first node in a `leftList` 
 *     and the last node in a `rightList`. At the end, I'd concatenate Root + leftList + (bottom level) + reversed(rightList).
 */

public class BoundaryOfBinaryTreeStudy {

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
     * APPROACH 3: MODULAR PATH DECOMPOSITION (The Optimal / Standard Solution)
     * Time: O(N) | Space: O(N)
     */
    public List<Integer> boundaryOfBinaryTree(TreeNode root) {
        List<Integer> boundary = new ArrayList<>();
        if (root == null) return boundary;

        // 1. Add the root
        // If it's a single-node tree, we add it and return immediately to prevent 
        // the leaf-collector from duplicating it.
        if (isLeaf(root)) {
            boundary.add(root.val);
            return boundary;
        } else {
            boundary.add(root.val);
        }

        // 2. Add Left Boundary
        addLeftBoundary(root.left, boundary);

        // 3. Add all Leaves
        addLeaves(root, boundary);

        // 4. Add Right Boundary (reversed)
        addRightBoundary(root.right, boundary);

        return boundary;
    }

    // Helper: A node is a leaf strictly if both children are null.
    private boolean isLeaf(TreeNode node) {
        return node != null && node.left == null && node.right == null;
    }

    // Helper: Iteratively trace the left boundary path.
    private void addLeftBoundary(TreeNode curr, List<Integer> boundary) {
        while (curr != null) {
            // Only add non-leaf nodes (leaves are handled separately)
            if (!isLeaf(curr)) {
                boundary.add(curr.val);
            }
            
            // Core path logic: always prefer left, fallback to right
            if (curr.left != null) {
                curr = curr.left;
            } else {
                curr = curr.right;
            }
        }
    }

    // Helper: Standard Pre-Order DFS to grab leaves left-to-right.
    private void addLeaves(TreeNode node, List<Integer> boundary) {
        if (node == null) return;
        
        if (isLeaf(node)) {
            boundary.add(node.val);
            return; // No children to explore
        }
        
        addLeaves(node.left, boundary);
        addLeaves(node.right, boundary);
    }

    // Helper: Iteratively trace the right boundary path, then append reversed.
    private void addRightBoundary(TreeNode curr, List<Integer> boundary) {
        List<Integer> tempStack = new ArrayList<>();
        
        while (curr != null) {
            if (!isLeaf(curr)) {
                tempStack.add(curr.val);
            }
            
            // Core path logic: always prefer right, fallback to left
            if (curr.right != null) {
                curr = curr.right;
            } else {
                curr = curr.left;
            }
        }
        
        // Reverse the collected nodes to simulate bottom-to-top order
        for (int i = tempStack.size() - 1; i >= 0; i--) {
            boundary.add(tempStack.get(i));
        }
    }


    /**
     * Main method to cross-check approach against the dry run and edge cases.
     */
    public static void main(String[] args) {
        BoundaryOfBinaryTreeStudy study = new BoundaryOfBinaryTreeStudy();

        // Standard Case: The Zig-Zag from the DRY RUN
        //         1
        //        / \
        //       2   3
        //        \   \
        //         4   5
        //        /     \
        //       6       7
        TreeNode standard = new TreeNode(1,
                new TreeNode(2, null, new TreeNode(4, new TreeNode(6), null)),
                new TreeNode(3, null, new TreeNode(5, null, new TreeNode(7)))
        );
        System.out.println("Standard Zig-Zag Case: " + study.boundaryOfBinaryTree(standard)); 
        // Expected: [1, 2, 4, 6, 7, 5, 3]


        // Edge Case 1: Single Node Tree
        TreeNode single = new TreeNode(42);
        System.out.println("Single Node Case: " + study.boundaryOfBinaryTree(single)); 
        // Expected: [42]


        // Edge Case 2: Left-Skewed Tree
        //     1
        //    /
        //   2
        //  /
        // 3
        TreeNode leftSkewed = new TreeNode(1, new TreeNode(2, new TreeNode(3), null), null);
        System.out.println("Left-Skewed Case: " + study.boundaryOfBinaryTree(leftSkewed)); 
        // Expected: [1, 2, 3] (Root=1, LeftBoundary=2, Leaves=3, RightBoundary=Empty)


        // Edge Case 3: Tricky Inner Leaves
        //       1
        //      / \
        //     2   3
        //    /     \
        //   4       5
        //    \     /
        //     6   7
        TreeNode innerLeaves = new TreeNode(1,
                new TreeNode(2, new TreeNode(4, null, new TreeNode(6)), null),
                new TreeNode(3, null, new TreeNode(5, new TreeNode(7), null))
        );
        System.out.println("Inner Leaves Case: " + study.boundaryOfBinaryTree(innerLeaves)); 
        // Expected: [1, 2, 4, 6, 7, 5, 3]
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Modular Path Decomposition.
 * Key Observation: Do not try to solve complex, multi-rule pathing problems in a single 
 *                  recursive pass. Break it into independent iterative and recursive pieces.
 * Memorize: `if (!isLeaf) add(); if (left) curr=left; else curr=right;` (The Zig-Zag path rule).
 * Most Common Trap: Using BFS. The boundary is defined by strict parent-child structural 
 *                   paths, NOT by which node sits on the absolute left edge of a horizontal level.
 * One-Line Mental Trigger: "Tree Boundary? 4 steps: Root, Left-Iterative, Leaves-DFS, Right-Iterative-Reversed."
 * ============================================================================
 */

import java.util.*;
import ds_v1.BinaryTree.TreeNode;

/**
 * Boundary Traversal of Binary Tree
 *
 * Boundary = Root
 *          + Left Boundary (excluding leaves)
 *          + All Leaves (left → right)
 *          + Right Boundary (excluding leaves, reversed)
 *
 * Key Rules:
 * 1. Root is always included once
 * 2. Leaves should NOT be duplicated (avoid adding in left/right boundary)
 * 3. Right boundary must be added in reverse order
 */
class Solution {

    public List<Integer> boundaryOfBinaryTree(TreeNode<Integer> root) {

        // Final result list
        List<Integer> result = new ArrayList<>();

        // Edge Case: Empty tree
        if (root == null) return result;

        /**
         * Edge Case: Single node tree
         * If root itself is a leaf → it is the only boundary
         * Important: avoids duplicate addition later
         */
        if (isLeaf(root)) {
            return List.of(root.data);
        }

        /**
         * STEP 1: Add ROOT
         * Root is always part of boundary (only once)
         */
        result.add(root.data);

        /**
         * STEP 2: Add LEFT BOUNDARY (excluding leaves)
         *
         * Strategy:
         * - Start from root.left
         * - Always prefer LEFT child
         * - If no left child → go RIGHT
         * - Skip leaf nodes (handled separately)
         */
        addLeftBoundary(root.left, result);

        /**
         * STEP 3: Add ALL LEAF NODES (left → right)
         *
         * Strategy:
         * - Perform DFS traversal
         * - Add only leaf nodes
         * - This ensures leftmost → rightmost order
         */
        addLeaves(root, result);

        /**
         * STEP 4: Add RIGHT BOUNDARY (excluding leaves, reversed)
         *
         * Strategy:
         * - Start from root.right
         * - Always prefer RIGHT child
         * - If no right child → go LEFT
         * - Store in temp list
         * - Reverse before adding (bottom → top)
         */
        addRightBoundary(root.right, result);

        return result;
    }

    /**
     * Adds LEFT boundary nodes (excluding leaf nodes)
     *
     * Example:
     *     1
     *    /
     *   2
     *    \
     *     4
     *
     * Left Boundary: 2 → 4 (if 4 is NOT leaf)
     */
    private void addLeftBoundary(TreeNode<Integer> node, List<Integer> result) {

        while (node != null) {

            // Add only if NOT a leaf (to avoid duplication with leaf traversal)
            if (!isLeaf(node)) {
                result.add(node.data);
            }

            /**
             * Move down:
             * - Prefer LEFT child
             * - Else go RIGHT
             *
             * This ensures we stay on boundary
             */
            if (node.left != null) {
                node = node.left;
            } else {
                node = node.right;
            }
        }
    }

    /**
     * Adds all LEAF nodes using DFS
     *
     * Why DFS?
     * - Naturally gives left → right order
     *
     * Important:
     * - Root is already handled separately
     * - This method only adds actual leaf nodes
     */
    private void addLeaves(TreeNode<Integer> node, List<Integer> result) {

        // Base case
        if (node == null) return;

        // If current node is leaf → add to result
        if (isLeaf(node)) {
            result.add(node.data);
            return;
        }

        // Traverse left subtree first (ensures left → right order)
        addLeaves(node.left, result);

        // Then traverse right subtree
        addLeaves(node.right, result);
    }

    /**
     * Adds RIGHT boundary nodes (excluding leaves)
     * in REVERSE order
     *
     * Example:
     *     1
     *      \
     *       2
     *      /
     *     4
     *
     * Right Boundary: 2 → 4 (but we add as 4 → 2)
     */
    private void addRightBoundary(TreeNode<Integer> node, List<Integer> result) {

        // Temporary list to store boundary (top → bottom)
        List<Integer> temp = new ArrayList<>();

        while (node != null) {

            // Add only if NOT leaf
            if (!isLeaf(node)) {
                temp.add(node.data);
            }

            /**
             * Move down:
             * - Prefer RIGHT child
             * - Else go LEFT
             */
            if (node.right != null) {
                node = node.right;
            } else {
                node = node.left;
            }
        }

        /**
         * IMPORTANT:
         * We collected nodes from TOP → DOWN
         * But need to add from BOTTOM → UP
         */
        Collections.reverse(temp);

        // Append reversed right boundary
        result.addAll(temp);
    }

    /**
     * Utility function to check if a node is LEAF
     *
     * Leaf = no left child AND no right child
     */
    private boolean isLeaf(TreeNode<Integer> node) {
        return node.left == null && node.right == null;
    }
}


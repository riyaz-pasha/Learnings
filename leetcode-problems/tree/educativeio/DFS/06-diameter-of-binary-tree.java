/**
 * ============================================================================
 * STUDY NOTE: DIAMETER OF BINARY TREE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Are we counting nodes or edges for the diameter?" 
 *   (Crucial: The prompt says edges. A path of 3 nodes has 2 edges. This changes our +1 logic).
 * - "Can the longest path exist entirely within a left or right subtree without passing through the root?" 
 *   (Yes, which means we must check the potential diameter at EVERY node, not just the global root).
 * - "Are the node values relevant at all?" 
 *   (No, the values [-100, 100] are a distraction. This is a purely structural problem).
 * - "Is thread-safety a concern for this method?" 
 *   (Important if we plan to use a shared/global instance variable to track the maximum diameter).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * The longest path (diameter) passes through *some* node acting as its peak/root. 
 * The length of that path is simply `longest_path_left + longest_path_right`. 
 * The constraint is that we don't know *which* node is the peak, so we must evaluate this 
 * sum for every node without re-traversing the same branches repeatedly.
 *
 * Approach 1: Top-Down Brute Force
 * What I'd naturally try: I know how to find the max depth of a tree. To find the diameter, 
 * I can write a function that calculates `depth(node.left) + depth(node.right)` for the current 
 * node, and then recursively asks the left and right children to do the same, keeping the maximum.
 * Why it works: It exhaustive evaluates every single node as a potential "peak" of the diameter.
 * Why it's too slow/costly: Massive duplicate work. When evaluating the root, `depth(root.left)` 
 * visits every node in the left subtree. Then, we recursively call `diameter(root.left)`, which 
 * calls `depth(root.left.left)`, visiting those exact same nodes again.
 * - Time Complexity: O(N^2) — because in a worst-case skewed tree, we traverse N nodes for the root, 
 *   N-1 for the child, N-2 for the grandchild, resulting in quadratic time.
 * - Space Complexity: O(N) — because the call stack for the recursion can go N levels deep.
 * 
 * Approach 2: Bottom-Up Post-Order DFS (Optimal)
 * What I'd naturally try: How do I stop repeating work? When I ask a child for its depth, it already 
 * visits all its descendants. What if the child could just calculate its own local diameter *while* 
 * it's figuring out its depth, and update a global high-score?
 * Why it works: We change from top-down to bottom-up (Post-Order). A node waits for its left and 
 * right children to report their depths. Once it has both depths, it does TWO things:
 *   1. Updates the global `maxDiameter` with `leftDepth + rightDepth`.
 *   2. Returns `max(leftDepth, rightDepth) + 1` up to its parent.
 * What property removes the bottleneck: By tracking the max diameter as a side-effect during a 
 * standard depth calculation, we fold two N-traversals into a single pass.
 * - Time Complexity: O(N) — because we visit every node exactly once.
 * - Space Complexity: O(N) — because the recursion stack depth is bounded by the tree height 
 *   (O(N) for skewed, O(log N) for balanced).
 *
 * The Interview Choice:
 * Always write Approach 2. It is the textbook O(N) solution. However, experienced candidates will 
 * point out that using an instance variable (`private int max`) is bad practice in concurrent environments. 
 * I would offer to either pass a 1-element array `int[] max` down the recursion, or return a custom 
 * object/record containing both `[depth, maxDiameter]` to keep the function pure.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Single Node (Root only) -> Depth is 1 (node), but edges = 0. Must return 0.
 * 2. Tree where the longest path doesn't pass through the root -> e.g., a massive left subtree 
 *    that splits into two deep branches, while the right subtree is null.
 * 3. Fully skewed tree (Linked List) -> Diameter should be N-1.
 *
 *
 * 4. DRY RUN (Bottom-Up Post-Order DFS)
 * ----------------------------------------------------------------------------
 * Tree:
 *         1
 *        / \
 *       2   3
 *      / \
 *     4   5
 * 
 * Call Stack Trace (maxDiameter initially 0):
 * 1. dfs(4) [Leaf]: 
 *    left=0, right=0. 
 *    Local diameter = 0+0 = 0. maxDiameter = max(0, 0) = 0.
 *    Returns max(0,0) + 1 = 1.
 * 2. dfs(5) [Leaf]:
 *    left=0, right=0. 
 *    Local diameter = 0+0 = 0. maxDiameter = max(0, 0) = 0.
 *    Returns max(0,0) + 1 = 1.
 * 3. dfs(2) [Internal]:
 *    Receives left=1 (from 4), right=1 (from 5).
 *    Local diameter = 1+1 = 2. maxDiameter = max(0, 2) = 2.
 *    Returns max(1,1) + 1 = 2.
 * 4. dfs(3) [Leaf]:
 *    left=0, right=0.
 *    Returns 1.
 * 5. dfs(1) [Root]:
 *    Receives left=2 (from 2), right=1 (from 3).
 *    Local diameter = 2+1 = 3. maxDiameter = max(2, 3) = 3.
 *    Returns max(2,1) + 1 = 3. (Return value of root is ignored by main wrapper).
 * Final maxDiameter = 3.
 *
 * Pitfalls: 
 * - Counting nodes instead of edges. If you calculate `leftDepth + rightDepth + 1`, you are 
 *   calculating the number of *nodes* in the path. The problem asks for *edges*, which is just 
 *   `leftDepth + rightDepth`.
 * - Returning the diameter to the parent instead of the depth. The parent needs the depth of 
 *   its child's longest branch to continue building the path upwards.
 *
 * Pattern Recognition: 
 * "When a tree problem asks for a maximum path that can peak at ANY node -> Think Post-Order DFS 
 * returning depths, while updating a global maximum as a side-effect." (Same pattern applies to 
 * Maximum Path Sum).
 *
 * Interview Script:
 * "The naive approach recalculates depths repeatedly, giving O(N^2) time. I can optimize this to O(N) 
 * by computing the depth bottom-up. At each node, I'll calculate the longest path passing through 
 * it by adding its left and right depths. I'll update a reference max-tracker, and then return 
 * the node's depth to its parent so the parent can do the same calculation. This requires O(N) space 
 * for the call stack."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: How would you solve this without using any global or instance variables?
 * A1: I would change the DFS return type from `int` to an `int[]` or Java `record` holding two values: 
 *     `[current_depth, max_diameter_found_so_far]`. The parent takes the max of its children's 
 *     diameters and its own local diameter, and bubbles that up.
 *
 * Q2: What if we needed to return the actual PATH (List of nodes), not just the length?
 * A2: Instead of just returning depth integers, the DFS would return the actual `List<TreeNode>` 
 *     representing the longest path down. At each node, we concatenate the left list + current node + 
 *     reversed right list. We check its size against our global maximum and save the list if it's longer.
 *
 * Q3: How does this change for an N-ary tree?
 * A3: Instead of just `left + right`, a node could have 5 children. The diameter passing through 
 *     that node is the sum of its TOP TWO deepest children. We'd maintain the top 2 max depths 
 *     while iterating through the children array.
 */

public class DiameterOfBinaryTreeStudy {

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
     * APPROACH 2: OPTIMAL BOTTOM-UP DFS (Using instance variable)
     * Time: O(N) | Space: O(N)
     */
    private int globalMaxDiameter = 0;

    public int diameterOfBinaryTree(TreeNode root) {
        globalMaxDiameter = 0; // Reset for repeated calls
        calculateDepth(root);
        return globalMaxDiameter;
    }

    private int calculateDepth(TreeNode node) {
        // Base case: A null node has a depth of 0.
        // It contributes 0 edges to any path passing through its parent.
        if (node == null) {
            return 0;
        }

        // 1. Post-order: Process children first
        int leftDepth = calculateDepth(node.left);
        int rightDepth = calculateDepth(node.right);

        // 2. Process current node: What is the max diameter passing through ME?
        // Since depth is measured in nodes, leftDepth + rightDepth exactly equals 
        // the number of edges bridging the left and right furthest leaves.
        int localDiameter = leftDepth + rightDepth;
        globalMaxDiameter = Math.max(globalMaxDiameter, localDiameter);

        // 3. Return to parent: What is the max depth going down from ME?
        // My longest branch is the max of my children, plus 1 for myself.
        return Math.max(leftDepth, rightDepth) + 1;
    }

    /**
     * APPROACH 3: OPTIMAL (Thread-Safe / Functional Approach)
     * Time: O(N) | Space: O(N)
     * Avoids instance variables by bubbling up both depth and max diameter.
     */
    public record TreeInfo(int depth, int maxDiameter) {}

    public int diameterOfBinaryTreePure(TreeNode root) {
        return dfsPure(root).maxDiameter();
    }

    private TreeInfo dfsPure(TreeNode node) {
        if (node == null) {
            return new TreeInfo(0, 0);
        }

        TreeInfo left = dfsPure(node.left);
        TreeInfo right = dfsPure(node.right);

        // Local diameter peaking at this node
        int localDiameter = left.depth() + right.depth();

        // The max diameter found in this subtree is the max of:
        // 1. The local diameter passing through this node
        // 2. The highest diameter completely contained in the left subtree
        // 3. The highest diameter completely contained in the right subtree
        int bestDiameter = Math.max(localDiameter, Math.max(left.maxDiameter(), right.maxDiameter()));
        
        // Max depth extending downwards from this node
        int currentDepth = Math.max(left.depth(), right.depth()) + 1;

        return new TreeInfo(currentDepth, bestDiameter);
    }

    /**
     * APPROACH 1: BRUTE FORCE (Top-Down)
     * Time: O(N^2) | Space: O(N)
     * Included to demonstrate what NOT to do in the interview, but how one might naively think.
     */
    public int diameterOfBinaryTreeBruteForce(TreeNode root) {
        if (root == null) return 0;

        // Calculate diameter passing strictly through THIS root
        int rootDiameter = getDepthForBruteForce(root.left) + getDepthForBruteForce(root.right);

        // Calculate diameters hidden strictly in the left or right subtrees
        int leftSubtreeDiameter = diameterOfBinaryTreeBruteForce(root.left);
        int rightSubtreeDiameter = diameterOfBinaryTreeBruteForce(root.right);

        // Return the max of all 3 possibilities
        return Math.max(rootDiameter, Math.max(leftSubtreeDiameter, rightSubtreeDiameter));
    }

    private int getDepthForBruteForce(TreeNode node) {
        if (node == null) return 0;
        return Math.max(getDepthForBruteForce(node.left), getDepthForBruteForce(node.right)) + 1;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        DiameterOfBinaryTreeStudy study = new DiameterOfBinaryTreeStudy();

        // Edge Case 1: Single Node
        TreeNode single = new TreeNode(1);
        System.out.println("Single Node (Optimal): " + study.diameterOfBinaryTree(single)); // Expected: 0
        System.out.println("Single Node (Pure):    " + study.diameterOfBinaryTreePure(single)); // Expected: 0

        // Standard Case: 
        //       1
        //      / \
        //     2   3
        //    / \
        //   4   5
        TreeNode standard = new TreeNode(1,
                new TreeNode(2, new TreeNode(4), new TreeNode(5)),
                new TreeNode(3)
        );
        System.out.println("Standard Tree (Optimal): " + study.diameterOfBinaryTree(standard)); // Expected: 3 (4->2->1->3)
        System.out.println("Standard Tree (Pure):    " + study.diameterOfBinaryTreePure(standard)); // Expected: 3
        System.out.println("Standard Tree (Brute):   " + study.diameterOfBinaryTreeBruteForce(standard)); // Expected: 3

        // Edge Case 2: Longest path does NOT pass through the root.
        //           1
        //          / 
        //         2   
        //        / \
        //       3   4
        //      /     \
        //     5       6
        //    /         \
        //   7           8
        // The root is 1, but the longest path is 7->5->3->2->4->6->8 (length 6)
        TreeNode displacedPeak = new TreeNode(1,
                new TreeNode(2,
                        new TreeNode(3, new TreeNode(5, new TreeNode(7), null), null),
                        new TreeNode(4, null, new TreeNode(6, null, new TreeNode(8)))
                ),
                null
        );
        
        System.out.println("Displaced Peak (Optimal): " + study.diameterOfBinaryTree(displacedPeak)); // Expected: 6
        System.out.println("Displaced Peak (Brute):   " + study.diameterOfBinaryTreeBruteForce(displacedPeak)); // Expected: 6
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Post-Order DFS with Side-Effects.
 * Key Observation: The depth function natively traverses exactly what we need to calculate 
 *                  the diameter. We can compute `leftDepth + rightDepth` dynamically at every 
 *                  node on our way back up, capturing the maximum without re-traversing.
 * Memorize: `globalMax = Math.max(globalMax, left + right); return Math.max(left, right) + 1;`
 * Most Common Trap: Returning the local diameter to the parent instead of the depth. The parent 
 *                   can only use a straight line (depth) to continue building a valid path.
 * One-Line Mental Trigger: "Diameter = Max of (left_depth + right_depth) seen anywhere."
 * ============================================================================
 */

import ds_v1.BinaryTree.TreeNode;

public class Solution {

    public static int diameterOfBinaryTree(TreeNode<Integer> root) {
        // Wrapper object to hold result (avoids static/global variable)
        Result result = new Result();

        dfs(root, result);

        return result.diameter;
    }

    /*
     * Helper class to store diameter
     * (Better than static/global for interview + thread-safe)
     */
    private static class Result {
        int diameter = 0;
    }

    /*
     * Returns height in terms of EDGES
     *
     * Definition:
     * - Height = number of edges from node to deepest leaf
     *
     * So:
     * - null → height = -1
     * - leaf → height = 0
     */
    private static int dfs(TreeNode<Integer> node, Result result) {

        // Base case: null node has height -1 (important for edge-based calculation)
        if (node == null) return -1;

        // 1. Compute left & right heights
        int leftHeight = dfs(node.left, result);
        int rightHeight = dfs(node.right, result);

        /*
         * 2. Compute diameter passing through current node
         *
         * Path = left subtree + right subtree + 2 edges (to connect current node)
         *
         * Example:
         *      leftHeight = 2
         *      rightHeight = 1
         *      diameter = 2 + 1 + 2 = 5 edges
         */
        int currentDiameter = leftHeight + rightHeight + 2;

        // Update global max
        result.diameter = Math.max(result.diameter, currentDiameter);

        /*
         * 3. Return height to parent
         *
         * height = 1 + max(leftHeight, rightHeight)
         */
        return 1 + Math.max(leftHeight, rightHeight);
    }
}

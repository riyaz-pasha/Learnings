/**
 * ============================================================================
 * STUDY NOTE: BINARY TREE MAXIMUM PATH SUM
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Can a valid path consist of just a single node?" 
 *   (Crucial: If the tree contains entirely negative numbers, the maximum path is just the single node with the highest negative value, not 0.)
 * - "Does the path have to go through the root, or can it be entirely isolated in a subtree?" 
 *   (The prompt says 'any non-empty path', confirming the peak of the path can be ANY node).
 * - "Are we allowed to use instance variables for tracking state?" 
 *   (Standard LeetCode solutions use a global/instance max variable, but asking shows you understand the thread-safety implications in production environments).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * A valid path can go UP to a parent and DOWN to another child (a "V-shape"). However, a path 
 * cannot branch. This means a node can act as the "peak" of the V-shape, connecting its left 
 * and right subtrees. BUT, when this node reports back to ITS parent, it can only offer ONE 
 * straight downward line. The bottleneck is calculating both the V-shape (for the global answer) 
 * and the straight-line (for the recursion) without traversing the tree quadratically.
 *
 * Approach 1: Top-Down Double DFS (The Brute Force)
 * What I'd naturally try: I'd write a helper function `maxStraightPath(node)` that recursively 
 * finds the max straight line down. Then, I'd write a main DFS that visits every node, calculating 
 * `maxStraightPath(left) + maxStraightPath(right) + node.val`, and tracks the maximum seen.
 * Why it works: It exhaustively considers every single node as the potential peak of the path.
 * Why it's too costly: Massive duplicate work. When evaluating the root, we calculate straight 
 * paths for the entire tree. When we move to the children, we calculate straight paths for the 
 * exact same descendants again.
 * - Time Complexity: O(N^2) — because for each of the N nodes, we do a full O(N) exploration downward.
 * - Space Complexity: O(N) — because the dual recursion stacks can go N levels deep.
 * 
 * Approach 2: Bottom-Up Post-Order DFS (The Optimal Masterpiece)
 * What I'd naturally try: How do I stop recalculating the straight paths? I should calculate them 
 * bottom-up. When a child finishes its straight-path calculation, it hands it up to the parent.
 * Why it works: At any given node, we wait for the left and right children to report their max 
 * straight downward paths. At this moment, we have all the information needed to evaluate this 
 * node as a "peak" (left + right + node.val). We update a global maximum, and then return just 
 * ONE straight path up to our parent (`max(left, right) + node.val`).
 * What property removes the bottleneck: By tracking the V-shape max as a "side effect" of calculating 
 * the straight-path max, we fold two N-traversals into a single bottom-up pass.
 * - Time Complexity: O(N) — because we visit every node exactly once.
 * - Space Complexity: O(H) — bounded strictly by the height of the recursive call stack (O(N) for 
 *   skewed, O(log N) for balanced).
 *
 * The Interview Choice:
 * Always write Approach 2. It is an industry-standard algorithm that evaluates your ability to 
 * multiplex data in recursion (returning one thing, mutating another). 
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. All Negative Nodes -> The algorithm must not default to 0. It must return the highest negative number.
 * 2. Single Node Tree -> Straightforwardly returns that node's value.
 * 3. Negative Branches -> If a child's max straight path is negative, including it would drag down 
 *    the sum. We must clamp negative branch sums to 0 (effectively deciding not to walk down that branch).
 *
 *
 * 4. DRY RUN (Optimal Recursive DFS)
 * ----------------------------------------------------------------------------
 * Tree:
 *       -10
 *       /  \
 *      9   20
 *         /  \
 *        15   7
 *
 * Call Stack Trace (Global Max initially -Infinity):
 * 1. dfs(-10): calls dfs(9) and dfs(20).
 * 2. [Left Branch] dfs(9): Leaves are null (return 0).
 *    - Left branch: max(0, 0) = 0. Right branch: max(0, 0) = 0.
 *    - Local V-shape peak = 0 + 0 + 9 = 9. Global Max = 9.
 *    - Returns 9 + max(0, 0) = 9 to dfs(-10).
 * 3. [Right Branch] dfs(20): calls dfs(15) and dfs(7).
 * 4. [Right-Left] dfs(15): Leaves null. Local peak = 15. Global Max = max(9, 15) = 15. Returns 15.
 * 5. [Right-Right] dfs(7): Leaves null. Local peak = 7. Global Max = max(15, 7) = 15. Returns 7.
 * 6. Back at dfs(20): receives left=15, right=7.
 *    - Local V-shape peak = 15 + 7 + 20 = 42. Global Max = max(15, 42) = 42.
 *    - Returns 20 + max(15, 7) = 35 to dfs(-10).
 * 7. Back at dfs(-10): receives left=9, right=35.
 *    - Local V-shape peak = 9 + 35 + (-10) = 34. Global Max = max(42, 34) = 42.
 *    - Returns -10 + max(9, 35) = 25 (ignored by main wrapper).
 * Final Result: 42.
 *
 * Pitfalls: 
 * - Forgetting to ignore negative branches! If `dfs(node.left)` returns -5, and you add it to 
 *   your path, you are making a suboptimal choice. You should have just stopped the path at the 
 *   current node. You must do `Math.max(0, dfs(left))`.
 * - Returning the V-shape sum instead of the straight-line sum. If a node returns `left + right + node.val` 
 *   to its parent, it creates a branched, invalid path for the ancestor.
 *
 * Pattern Recognition: 
 * "When finding a maximum path/diameter that can peak ANYWHERE in a tree -> Post-Order traversal 
 *  returning straight depths/sums, while updating a global max as a side-effect."
 *
 * Interview Script:
 * "A valid path can only branch at exactly one node—its peak. For any node, the maximum path peaking 
 * there is its value plus the best downward path from its left and right children. If a downward path 
 * is negative, we just ignore it by bounding it to 0. I'll use a post-order traversal where each node 
 * calculates this peak to update a global maximum, but crucially, it only returns a single straight 
 * path to its parent. This evaluates the whole tree in O(N) time and O(H) space."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: How would you make this thread-safe without using instance/global variables?
 * A1: I would pass a 1-element array `int[] globalMax = new int[]{Integer.MIN_VALUE}` down through 
 *     the recursive calls, allowing all stack frames to mutate the same heap reference safely. 
 *     Alternatively, return a custom `record/class` holding both `straightMax` and `globalMaxSoFar`.
 *
 * Q2: What if we needed to return the actual PATH (List of nodes), not just the sum?
 * A2: The return type changes from an integer to an Object holding `sum` and `List<TreeNode> path`. 
 *     When deciding whether to return the left or right branch, we return the List associated with 
 *     the larger sum, appending the current node to it. We'd track the `bestVPath` globally in the 
 *     same way.
 *
 * Q3: How does this change for an N-ary tree instead of a Binary Tree?
 * A3: Instead of just `left + right`, a node could have many children. The max path peaking at this 
 *     node would be `node.val` plus the sum of its TOP TWO non-negative child paths. The node would 
 *     return `node.val` plus its TOP ONE non-negative child path.
 */

public class BinaryTreeMaxPathSumStudy {

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
     * APPROACH 2: OPTIMAL POST-ORDER DFS (Using instance variable)
     * Time: O(N) | Space: O(H)
     */
    private int globalMaxSum;

    public int maxPathSum(TreeNode root) {
        // Initialize to minimum possible integer so even a single negative node tree works
        globalMaxSum = Integer.MIN_VALUE;
        calculateStraightPath(root);
        return globalMaxSum;
    }

    private int calculateStraightPath(TreeNode node) {
        // Base case: null nodes contribute 0 to the path sum
        if (node == null) {
            return 0;
        }

        // Post-Order: Ask children for their max straight downward paths.
        // CRUCIAL: If a child returns a negative path, we are better off NOT taking it.
        // We use Math.max(0, ...) to effectively "snip" negative branches.
        int leftMax = Math.max(0, calculateStraightPath(node.left));
        int rightMax = Math.max(0, calculateStraightPath(node.right));

        // Evaluate the current node as the PEAK of the path (the V-shape)
        // This connects the left branch, the current node, and the right branch.
        int localPeakSum = leftMax + rightMax + node.val;

        // Update the global record if this peak is the best we've seen anywhere
        globalMaxSum = Math.max(globalMaxSum, localPeakSum);

        // Tell the parent what the best STRAIGHT line path is going down from this node.
        // The parent cannot branch both left and right from this node, so we only 
        // offer it the single best branch, plus this node's value.
        return node.val + Math.max(leftMax, rightMax);
    }

    /**
     * APPROACH 2B: THREAD-SAFE OPTIMAL (No instance variables)
     * Time: O(N) | Space: O(H)
     * Uses a 1-element array to pass the global max reference through the stack.
     */
    public int maxPathSumThreadSafe(TreeNode root) {
        int[] maxTracker = new int[] { Integer.MIN_VALUE };
        dfsThreadSafe(root, maxTracker);
        return maxTracker[0];
    }

    private int dfsThreadSafe(TreeNode node, int[] maxTracker) {
        if (node == null) return 0;

        int left = Math.max(0, dfsThreadSafe(node.left, maxTracker));
        int right = Math.max(0, dfsThreadSafe(node.right, maxTracker));

        maxTracker[0] = Math.max(maxTracker[0], left + right + node.val);

        return node.val + Math.max(left, right);
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        BinaryTreeMaxPathSumStudy study = new BinaryTreeMaxPathSumStudy();

        // Standard Case: The example from the DRY RUN
        //       -10
        //       /  \
        //      9   20
        //         /  \
        //        15   7
        TreeNode standard = new TreeNode(-10,
                new TreeNode(9),
                new TreeNode(20, new TreeNode(15), new TreeNode(7))
        );
        System.out.println("Standard Tree (Optimal): " + study.maxPathSum(standard)); // Expected: 42 (15 -> 20 -> 7)
        System.out.println("Standard Tree (Thread-Safe): " + study.maxPathSumThreadSafe(standard)); // Expected: 42

        // Edge Case 1: All Negative Nodes
        //       -3
        //       /
        //     -5
        // If we didn't use Integer.MIN_VALUE or if we forced inclusion, this would fail.
        TreeNode allNegative = new TreeNode(-3, new TreeNode(-5), null);
        System.out.println("All Negative Nodes: " + study.maxPathSum(allNegative)); // Expected: -3

        // Edge Case 2: Snipping Negative Branches
        //       2
        //      / \
        //    -1  -2
        // Peak is just 2. Left and Right branches are negative, so they get clamped to 0.
        TreeNode negBranches = new TreeNode(2, new TreeNode(-1), new TreeNode(-2));
        System.out.println("Negative Branches: " + study.maxPathSum(negBranches)); // Expected: 2

        // Edge Case 3: Single Node
        TreeNode single = new TreeNode(42);
        System.out.println("Single Node: " + study.maxPathSum(single)); // Expected: 42
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Post-Order DFS returning straight paths while mutating global V-shape max.
 * Key Observation: A path can only branch once. At any node, you calculate the "peak" sum 
 *                  using BOTH branches to update the global max, but you return only the 
 *                  BEST SINGLE branch up to the parent.
 * Memorize: `int l = Math.max(0, dfs(L)); int r = Math.max(0, dfs(R));` 
 *           `max = Math.max(max, l + r + val); return val + Math.max(l, r);`
 * Most Common Trap: Forgetting `Math.max(0, ...)` for the children. If a subtree evaluates 
 *                   to a negative sum, it will actively destroy your path sum. You must 
 *                   conceptually sever that branch by treating its sum as 0.
 * One-Line Mental Trigger: "Max Path Sum = Max(0, left/right) -> global max V-shape -> return straight max."
 * ============================================================================
 */

import java.util.*;

/**
 * Binary Tree Maximum Path Sum
 *
 * Given a binary tree, find the maximum sum of any non-empty path.
 *
 * A path:
 * - Can start and end at ANY node.
 * - Does NOT have to include the root.
 * - Cannot visit a node more than once.
 *
 * ------------------------------------------------------------
 * Example
 * ------------------------------------------------------------
 *
 *              -10
 *              /  \
 *             9    20
 *                 /  \
 *                15   7
 *
 * Maximum path:
 *
 *             15
 *              \
 *               20
 *                 \
 *                  7
 *
 * Sum = 15 + 20 + 7 = 42
 *
 * Answer = 42
 *
 * ------------------------------------------------------------
 */
public class BinaryTreeMaximumPathSum {

    // ============================================================
    // Tree Node
    // ============================================================

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
    // APPROACH 1: Brute Force
    // ============================================================
    //
    // One way to think about the problem is:
    //
    // "What if I considered every possible path?"
    //
    // Unfortunately, the number of possible paths can be very large.
    //
    // We could:
    //
    // 1. Start a path from every node.
    // 2. Explore every possible path from that node.
    // 3. Calculate its sum.
    // 4. Keep the maximum.
    //
    // This is not practical because the number of possible paths
    // can grow very quickly.
    //
    // We therefore need to exploit the TREE structure.
    //
    // ------------------------------------------------------------
    //
    // Instead of explicitly generating paths, we can ask:
    //
    // "What is the maximum contribution this subtree can give
    //  to a path going through its parent?"
    //
    // That leads to the optimal solution.
    //
    // ============================================================


    // ============================================================
    // APPROACH 2: Recursive DFS - Optimal
    // ============================================================
    //
    // This is the standard solution.
    //
    // Time:  O(N)
    // Space: O(H)
    //
    // N = number of nodes
    // H = height of tree
    //
    // ------------------------------------------------------------
    //
    // The crucial distinction:
    //
    // There are TWO different values we care about.
    //
    // ------------------------------------------------------------
    //
    // 1. BEST PATH STARTING AT CURRENT NODE AND GOING UP
    //
    // This value is returned to the parent.
    //
    // A parent can only continue through ONE child.
    //
    // Example:
    //
    //                 10
    //                /  \
    //               5    8
    //
    // If we're calculating what 10 can contribute to its parent,
    // we cannot return:
    //
    //       5 + 10 + 8
    //
    // because then 10 has two branches and there is no way for
    // the parent to continue the path.
    //
    // We must choose ONE:
    //
    //       10 + max(5, 8)
    //
    // ------------------------------------------------------------
    //
    // 2. BEST PATH PASSING THROUGH CURRENT NODE
    //
    // This value can use BOTH children:
    //
    //       left + current + right
    //
    // This represents a complete path:
    //
    //             left
    //               \
    //              current
    //                 \
    //                right
    //
    // Such a path might be the final answer.
    //
    // Therefore:
    //
    //       pathThroughCurrent =
    //           leftContribution
    //         + current.value
    //         + rightContribution
    //
    // We update the global answer using this value.
    //
    // But we return only:
    //
    //       current.value + max(left, right)
    //
    // to the parent.
    //
    // ============================================================

    static class OptimalSolution {

        /*
         * Global answer.
         *
         * We initialize it to Integer.MIN_VALUE rather than 0.
         *
         * Why?
         *
         * The problem requires a NON-EMPTY path.
         *
         * Consider:
         *
         *       -5
         *
         * Answer must be -5, not 0.
         */
        private int maximumPathSum = Integer.MIN_VALUE;

        public int maxPathSum(TreeNode root) {

            calculateMaximumContribution(root);

            return maximumPathSum;
        }

        /**
         * Returns the maximum sum of a path that:
         *
         * - starts at currentNode
         * - may continue into ONE child
         * - must be able to connect to currentNode's parent
         *
         * At the same time, we update the global answer with a
         * path that can use BOTH children.
         */
        private int calculateMaximumContribution(TreeNode currentNode) {

            if (currentNode == null) {
                return 0;
            }

            // ----------------------------------------------------
            // Calculate the best contribution from each subtree.
            // ----------------------------------------------------
            //
            // If a subtree gives us a negative contribution, we
            // are better off NOT including it.
            //
            // Therefore:
            //
            //     max(0, subtreeContribution)
            //
            // means:
            //
            //     "Take this subtree only if it helps."
            //
            int leftContribution = Math.max(
                    0,
                    calculateMaximumContribution(currentNode.left)
            );

            int rightContribution = Math.max(
                    0,
                    calculateMaximumContribution(currentNode.right)
            );

            // ----------------------------------------------------
            // A complete path passing through currentNode can use
            // BOTH sides.
            // ----------------------------------------------------
            //
            //             left
            //               \
            //              current
            //                 \
            //                right
            //
            int pathThroughCurrent =
                    leftContribution
                    + currentNode.value
                    + rightContribution;

            // This path might be the best path anywhere in the tree.
            maximumPathSum = Math.max(
                    maximumPathSum,
                    pathThroughCurrent
            );

            // ----------------------------------------------------
            // Return only ONE branch to the parent.
            // ----------------------------------------------------
            //
            // We cannot return both left and right because then the
            // parent could not connect to both branches without
            // revisiting currentNode.
            //
            int bestBranchToParent = Math.max(
                    leftContribution,
                    rightContribution
            );

            return currentNode.value + bestBranchToParent;
        }
    }


    // ============================================================
    // APPROACH 3: Explicit Result Object
    // ============================================================
    //
    // Instead of using a class-level/global variable, we can
    // explicitly return two pieces of information from each
    // subtree.
    //
    // This is useful for understanding exactly what the recursion
    // is computing.
    //
    // For every node we calculate:
    //
    // 1. maxContribution
    //    Best path starting at this node and going upward.
    //
    // 2. maxPathInSubtree
    //    Best complete path anywhere inside this subtree.
    //
    // Time:  O(N)
    // Space: O(H)
    //
    // This is slightly more verbose than the previous solution,
    // but avoids mutable global state.
    // ============================================================

    static class Result {

        int maxContribution;
        int maximumPathSum;

        Result(int maxContribution, int maximumPathSum) {
            this.maxContribution = maxContribution;
            this.maximumPathSum = maximumPathSum;
        }
    }

    static class ResultObjectSolution {

        public int maxPathSum(TreeNode root) {
            return calculate(root).maximumPathSum;
        }

        private Result calculate(TreeNode currentNode) {

            if (currentNode == null) {
                /*
                 * There is no path in an empty subtree.
                 *
                 * Integer.MIN_VALUE is used for the "best path"
                 * so that an actual negative node is still preferred.
                 *
                 * Contribution is 0 because an absent child should
                 * not hurt its parent.
                 */
                return new Result(
                        0,
                        Integer.MIN_VALUE
                );
            }

            Result leftResult = calculate(currentNode.left);
            Result rightResult = calculate(currentNode.right);

            // Negative child contributions should not be included.
            int leftContribution = Math.max(
                    0,
                    leftResult.maxContribution
            );

            int rightContribution = Math.max(
                    0,
                    rightResult.maxContribution
            );

            // Complete path passing through this node.
            int pathThroughCurrent =
                    leftContribution
                    + currentNode.value
                    + rightContribution;

            // Best path starting here and going toward the parent.
            int maxContribution =
                    currentNode.value
                    + Math.max(
                            leftContribution,
                            rightContribution
                    );

            // Best path anywhere in this subtree.
            int maximumPathSum = Math.max(
                    pathThroughCurrent,
                    Math.max(
                            leftResult.maximumPathSum,
                            rightResult.maximumPathSum
                    )
            );

            return new Result(
                    maxContribution,
                    maximumPathSum
            );
        }
    }


    // ============================================================
    // Helper: Print Tree
    // ============================================================

    static void printTreePreorder(TreeNode root) {

        if (root == null) {
            return;
        }

        System.out.print(root.value + " ");

        printTreePreorder(root.left);
        printTreePreorder(root.right);
    }


    // ============================================================
    // Example / Dry Run
    // ============================================================

    public static void main(String[] args) {

        /*
         * Example 1
         *
         *              -10
         *              /  \
         *             9    20
         *                 /  \
         *                15   7
         *
         * Answer = 42
         *
         * Path:
         *
         *        15 -> 20 -> 7
         */

        TreeNode root =
                new TreeNode(
                        -10,
                        new TreeNode(9),
                        new TreeNode(
                                20,
                                new TreeNode(15),
                                new TreeNode(7)
                        )
                );

        OptimalSolution solution = new OptimalSolution();

        int answer = solution.maxPathSum(root);

        System.out.println("Maximum Path Sum = " + answer);

        /*
         * Expected:
         *
         * Maximum Path Sum = 42
         */


        // --------------------------------------------------------
        // Example 2: All negative values
        // --------------------------------------------------------
        //
        //              -3
        //             /  \
        //           -2   -1
        //
        // We MUST choose a non-empty path.
        //
        // Answer = -1
        //
        // This is why maximumPathSum cannot start at 0.

        TreeNode negativeTree =
                new TreeNode(
                        -3,
                        new TreeNode(-2),
                        new TreeNode(-1)
                );

        OptimalSolution negativeSolution =
                new OptimalSolution();

        System.out.println(
                "Maximum Path Sum = "
                        + negativeSolution.maxPathSum(negativeTree)
        );

        /*
         * Expected:
         *
         * Maximum Path Sum = -1
         */
    }
}

import java.util.*;
import ds_v1.BinaryTree.TreeNode;

public class Solution {

    // Global variable to track maximum path sum across entire tree
    private static int globalMax;

    public static int maxPathSum(TreeNode<Integer> root) {

        // Edge case (though constraints say >=1 node)
        if (root == null) return 0;

        // Initialize with minimum possible value
        globalMax = Integer.MIN_VALUE;

        // Start DFS
        dfs(root);

        return globalMax;
    }

    /**
     * DFS function returns:
     *  -> Maximum path sum starting from this node and going DOWNWARD
     *     (only one direction allowed: either left OR right)
     */
    private static int dfs(TreeNode<Integer> node) {

        // Base case
        if (node == null) return 0;

        // Recursively compute left and right subtree contributions
        int left = dfs(node.left);
        int right = dfs(node.right);

        /**
         * IMPORTANT:
         * If subtree gives negative contribution → ignore it
         * Because including it will reduce the path sum
         */
        int leftGain = Math.max(0, left);
        int rightGain = Math.max(0, right);

        /**
         * CASE 1: Path passes through this node
         * This includes BOTH left and right contributions
         *
         * Example:
         *    left + node + right
         *
         * This is a candidate for global maximum
         */
        int currentPathSum = leftGain + node.data + rightGain;

        // Update global maximum
        globalMax = Math.max(globalMax, currentPathSum);

        /**
         * CASE 2: Return value to parent
         * We can ONLY take one side (to maintain valid path)
         */
        return node.data + Math.max(leftGain, rightGain);
    }
}


import java.util.*;
import ds_v1.BinaryTree.TreeNode;

public class Solution {

    public static int maxPathSum(TreeNode<Integer> root) {

        if (root == null) return 0;

        int globalMax = Integer.MIN_VALUE;

        /**
         * Stack for postorder traversal
         */
        Deque<TreeNode<Integer>> stack = new ArrayDeque<>();

        /**
         * Map to store computed results (like recursion return values)
         * dp[node] = max path sum from this node going downward (one side)
         */
        Map<TreeNode<Integer>, Integer> dp = new HashMap<>();

        TreeNode<Integer> curr = root;
        TreeNode<Integer> lastVisited = null;

        /**
         * Standard iterative postorder traversal
         */
        while (curr != null || !stack.isEmpty()) {

            // Go LEFT as much as possible
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }

            TreeNode<Integer> node = stack.peek();

            // If right subtree exists and not processed → go right
            if (node.right != null && lastVisited != node.right) {
                curr = node.right;
            } else {
                // Process node (POSTORDER position)

                int left = dp.getOrDefault(node.left, 0);
                int right = dp.getOrDefault(node.right, 0);

                // Ignore negative contributions
                int leftGain = Math.max(0, left);
                int rightGain = Math.max(0, right);

                /**
                 * Case 1: Path passing through node
                 */
                int currentPath = leftGain + node.data + rightGain;

                globalMax = Math.max(globalMax, currentPath);

                /**
                 * Case 2: Store result for parent
                 */
                int returnValue = node.data + Math.max(leftGain, rightGain);

                dp.put(node, returnValue);

                // Mark node as processed
                lastVisited = stack.pop();
            }
        }

        return globalMax;
    }
}


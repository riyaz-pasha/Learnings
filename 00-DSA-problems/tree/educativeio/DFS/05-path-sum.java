import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ============================================================================
 * STUDY NOTE: PATH SUM (Root to Leaf)
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Can node values be negative?" 
 *   (Crucial: If values were only positive, we could short-circuit and stop searching once 
 *   our current sum exceeds the target. With negatives, a path sum can decrease, so we must 
 *   always reach the leaves).
 * - "What should I return if the tree is empty?" 
 *   (Since there are no nodes, there are no leaves. Thus, no root-to-leaf path can exist. Return false).
 * - "If an internal node's path equals the targetSum, does that count?" 
 *   (The problem explicitly specifies 'root-to-leaf'. Internal nodes do not count, which 
 *   changes how we write the base case).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We must maintain a running tally of our sum as we traverse downward, but we can ONLY evaluate 
 * success when we physically stand on a leaf node. The challenge is passing this state accurately 
 * along branching paths without letting one branch's sum corrupt another's.
 *
 * Approach 1: Recursive DFS (Top-Down with Subtraction)
 * What I'd naturally try: I need to check paths. I'll traverse down the tree and keep adding up 
 * the node values. Actually, it's cleaner to just subtract the current node's value from the 
 * `targetSum` as I go down. When I hit a leaf, I just check if the remaining `targetSum` exactly 
 * equals the leaf's value.
 * Why it works: Recursion isolates state. When we branch left and right, the call stack creates 
 * two separate frames, each with its own independent copy of the remaining `targetSum`.
 * What work is being repeated: None. We visit each node exactly once.
 * What property removes the bottleneck: The implicit call stack tracks both the traversal path 
 * and the running target sum simultaneously without extra data structures.
 * - Time Complexity: O(N) — because we may need to visit every single node in the tree to find the 
 *   valid path (or to confirm none exists).
 * - Space Complexity: O(N) — because in a worst-case fully skewed tree (a straight line), the 
 *   recursion stack will hold N frames. O(log N) in the best/balanced case.
 * 
 * Approach 2: Iterative DFS with Stacks (Production-Safe)
 * What I'd naturally try: If the tree is 10,000 nodes deep, recursion will throw a StackOverflowError. 
 * I need to move the state tracking to the heap. I'll use two explicit Stacks: one to hold the 
 * TreeNodes, and a parallel Stack to hold the remaining `targetSum` for that specific node.
 * Why it works: We manually simulate exactly what the OS call stack was doing. When we pop a node, 
 * we simultaneously pop its corresponding current sum.
 * What property removes the bottleneck: Allocating tracking stacks on the heap circumvents thread 
 * stack limits, making the algorithm crash-proof for extreme inputs.
 * - Time Complexity: O(N) — because every node and its corresponding sum is pushed and popped at most once.
 * - Space Complexity: O(N) — because the explicit stacks will grow to the maximum depth of the tree, 
 *   which is N in the worst-case skewed tree.
 *
 * The Interview Choice:
 * Write the Recursive DFS. It's incredibly concise (about 5 lines) and demonstrates a solid 
 * grasp of tree traversals and state passing. Mentioning the Iterative approach proves production 
 * awareness, but don't code it unless explicitly asked, as parallel stacks can get visually messy.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Empty Tree -> Returns false immediately.
 * 2. Tree with negative values -> Traversal must continue even if sum drops below/above target.
 * 3. The "False Positive" Internal Node -> A path sum hits the target at a node with ONE child. 
 *    This is not a leaf, so it must return false and continue searching.
 * 4. Zero Target & Zero Values -> Tests logic strictly checking sum differences.
 *
 *
 * 4. DRY RUN (Recursive DFS)
 * ----------------------------------------------------------------------------
 * Tree:
 *       5
 *      / \
 *     4   8
 *    /   / \
 *   11  13  4
 *  /  \      \
 * 7    2      1
 * 
 * Target Sum: 22
 *
 * Call Stack Trace (target = 22):
 * 1. hasPathSum(node=5, target=22): Not a leaf. 
 *    - Recurse Left: hasPathSum(node=4, target=17) (22 - 5)
 *    - Recurse Right: hasPathSum(node=8, target=17) (22 - 5)
 * 2. [Left Branch] hasPathSum(node=4, target=17): Not a leaf.
 *    - Recurse Left: hasPathSum(node=11, target=13) (17 - 4)
 * 3. [Left Branch] hasPathSum(node=11, target=13): Not a leaf.
 *    - Recurse Left: hasPathSum(node=7, target=2) (13 - 11)
 *    - Recurse Right: hasPathSum(node=2, target=2) (13 - 11)
 * 4. [Left-Left] hasPathSum(node=7, target=2): IS A LEAF!
 *    - Check: 7 == 2? FALSE.
 * 5. [Left-Right] hasPathSum(node=2, target=2): IS A LEAF!
 *    - Check: 2 == 2? TRUE!
 * 6. The `true` bubbles all the way up through the `||` operators.
 * Final Result: true.
 *
 * Pitfalls: 
 * - The "Null Leaf" Trap: 
 *   Writing the base case like this: `if (root == null) return targetSum == 0;`
 *   WHY IT FAILS: If a node has a value of 22 (the target), but it has a left child and a null 
 *   right child. The code visits the null right child, sees `targetSum == 0`, and returns true. 
 *   But the node wasn't a leaf! You must explicitly check `node.left == null && node.right == null`.
 *
 * Pattern Recognition: 
 * "When aggregating state down a tree path -> Pass the modified state as a parameter in DFS."
 * "When looking for ANY valid path -> Chain recursive calls with logical OR (`||`)."
 *
 * Interview Script:
 * "To find if a root-to-leaf path exists, I'll use a top-down recursive DFS. Instead of keeping a 
 * running total, it's cleaner to subtract the current node's value from the targetSum as we traverse 
 * down. When we reach a leaf, we just check if the remaining target matches the leaf's value. 
 * This requires O(N) time to visit nodes and O(H) space for the call stack."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if we needed to return ALL valid paths, not just a boolean? (Path Sum II)
 * A1: I would change the return type to `List<List<Integer>>`. Instead of just returning boolean, 
 *     I'd pass a `List<Integer> currentPath` down the recursion. I'd add the node to `currentPath`, 
 *     check for a leaf match (and if so, add a copy of `currentPath` to the global results list), 
 *     recurse, and finally backtrack by removing the last node from `currentPath` before returning.
 *
 * Q2: What if the path doesn't need to start at the root or end at a leaf? (Path Sum III)
 * A2: That becomes a prefix-sum problem on a tree. We'd traverse the tree while maintaining a 
 *     HashMap of all accumulated prefix sums up to the current node. At each node, we check if 
 *     `(currentSum - targetSum)` exists in the map to find valid sub-paths.
 */

public class PathSumStudy {

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
     * APPROACH 1: RECURSIVE DFS (The elegant, optimal choice)
     * Time: O(N) | Space: O(N) worst case, O(log N) best case
     */
    public boolean hasPathSumRecursive(TreeNode root, int targetSum) {
        // Base case 1: Empty tree or we traversed into a null child. 
        // This is not a leaf, so no valid path ends here.
        if (root == null) {
            return false;
        }

        // Base case 2: We are exactly on a LEAF node.
        // A leaf is defined as having NO left and NO right child.
        if (root.left == null && root.right == null) {
            // Did the accumulated subtractions leave us with exactly the leaf's value?
            return targetSum == root.val;
        }

        // If not a leaf, we subtract the current node's value from our target
        int remainingSum = targetSum - root.val;

        // Recurse down both branches. We use '||' because we only need ONE valid path.
        // If the left branch finds it, Java short-circuits and skips the right branch.
        return hasPathSumRecursive(root.left, remainingSum) 
            || hasPathSumRecursive(root.right, remainingSum);
    }

    /**
     * APPROACH 2: ITERATIVE DFS WITH PARALLEL STACKS (The robust choice for massive trees)
     * Time: O(N) | Space: O(N)
     */
    public boolean hasPathSumIterative(TreeNode root, int targetSum) {
        if (root == null) {
            return false;
        }

        // We use two parallel stacks. 
        // nodeStack holds the tree traversal state.
        Deque<TreeNode> nodeStack = new ArrayDeque<>();
        // sumStack holds the CURRENT remaining sum needed when we process that node.
        Deque<Integer> sumStack = new ArrayDeque<>();

        nodeStack.push(root);
        sumStack.push(targetSum - root.val); // Pre-subtract the root's value

        while (!nodeStack.isEmpty()) {
            TreeNode currentNode = nodeStack.pop();
            int currentRemainingSum = sumStack.pop();

            // Check if it's a leaf node AND if the remaining sum exactly hit 0
            if (currentNode.left == null && currentNode.right == null && currentRemainingSum == 0) {
                return true; // Found a valid path!
            }

            // Push right child and its state
            if (currentNode.right != null) {
                nodeStack.push(currentNode.right);
                sumStack.push(currentRemainingSum - currentNode.right.val);
            }
            
            // Push left child and its state
            // (Pushed last so it pops first, maintaining left-to-right Preorder)
            if (currentNode.left != null) {
                nodeStack.push(currentNode.left);
                sumStack.push(currentRemainingSum - currentNode.left.val);
            }
        }

        // Traversed the entire tree and found no valid root-to-leaf path
        return false;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        PathSumStudy study = new PathSumStudy();

        // Edge Case 1: Empty Tree
        System.out.println("Empty Tree: " + study.hasPathSumRecursive(null, 0)); // Expected: false

        // Edge Case 2: Single Node Tree (target matches)
        TreeNode singleHit = new TreeNode(5);
        System.out.println("Single Node (Hit): " + study.hasPathSumRecursive(singleHit, 5)); // Expected: true

        // Edge Case 3: Single Node Tree (target misses)
        TreeNode singleMiss = new TreeNode(5);
        System.out.println("Single Node (Miss): " + study.hasPathSumRecursive(singleMiss, 10)); // Expected: false

        // Standard Case: The example from the DRY RUN
        //       5
        //      / \
        //     4   8
        //    /   / \
        //   11  13  4
        //  /  \      \
        // 7    2      1
        TreeNode standard = new TreeNode(5,
                new TreeNode(4,
                        new TreeNode(11, new TreeNode(7), new TreeNode(2)),
                        null),
                new TreeNode(8,
                        new TreeNode(13),
                        new TreeNode(4, null, new TreeNode(1)))
        );
        
        System.out.println("Standard Tree (target 22, DFS): " + study.hasPathSumRecursive(standard, 22)); // Expected: true (5->4->11->2)
        System.out.println("Standard Tree (target 22, Iterative): " + study.hasPathSumIterative(standard, 22)); // Expected: true
        
        System.out.println("Standard Tree (target 26, DFS): " + study.hasPathSumRecursive(standard, 26)); // Expected: true (5->8->13)
        System.out.println("Standard Tree (target 18, DFS): " + study.hasPathSumRecursive(standard, 18)); // Expected: false

        // Edge Case 4: The "False Positive" Internal Node trap
        //       1
        //      /
        //     2
        // Target: 1. 
        // If we incorrectly return true when (target == 0) on null children, 
        // node 1's right null child would trigger a false positive.
        TreeNode internalTrap = new TreeNode(1, new TreeNode(2), null);
        System.out.println("Internal Trap (target 1): " + study.hasPathSumRecursive(internalTrap, 1)); // Expected: false (leaf is 2, path sum is 3)
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Top-Down DFS State Passing.
 * Key Observation: Rather than summing upwards from zero, subtract downwards from the target. 
 *                  Evaluate success ONLY when BOTH left and right children are null (a leaf).
 * Memorize: `if (root.left == null && root.right == null) return targetSum == root.val;`
 * Most Common Trap: Using `if (root == null) return targetSum == 0;`. This will trigger 
 *                   incorrectly on an internal node that has one valid child and one null child.
 * One-Line Mental Trigger: "Path Sum = Is it a leaf? Yes: Check val. No: Subtract and recurse ||."
 * ============================================================================
 */

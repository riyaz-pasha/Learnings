import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * ============================================================================
 * STUDY NOTE: BINARY TREE RIGHT SIDE VIEW
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "If a node on the right is null, but there is a node on the left at the same depth, is the left node visible?"
 *   (Crucial: Yes. A common beginner mistake is assuming 'right side view' means ONLY the right children. It means the rightmost node at EVERY level, regardless of its parentage).
 * - "What should I return if the tree is completely empty?"
 *   (Establishes the base state: an empty list, not a null reference).
 * - "Can node values be negative or duplicates?"
 *   (Yes, constraints say [-100, 100]. Values are irrelevant to the structural logic, but it confirms we don't use them as flags).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint:
 * We need to extract exactly ONE node per depth level of the tree—specifically, the last node 
 * on the right. The bottleneck is traversing the tree in a way that guarantees we can identify 
 * which node is the "rightmost" for any given depth without redundantly overwriting our answer 
 * or losing track of deeper left-leaning branches.
 *
 * Approach 1: The Rookie Mistake (Right-Spine Only) - FAILS
 * What I'd naturally try: I just want the right side! So I'll write a `while(node != null)` loop 
 * and just do `node = node.right`, adding values to a list.
 * Why it fails: It completely ignores asymmetric trees. If the tree has a deep left branch and a 
 * shallow right branch, looking from the right side means you see the shallow right branch, and 
 * BELOW it, you see the bottom of the deep left branch. This approach misses the bottom entirely.
 *
 * Approach 2: Level-Order Traversal / BFS (The Intuitive Safe Choice)
 * What I'd naturally try: Since I need one node per level, I should just process the tree 
 * level-by-level using a Queue. For every level, I'll loop through all its nodes. The very last 
 * node I pop for that level is the rightmost one, so I'll add it to my result list.
 * Why it works: BFS naturally groups nodes by depth. By iterating `size` times per level, we 
 * know exactly when a level ends.
 * What property removes the bottleneck: The Queue structure perfectly preserves depth layers.
 * - Time Complexity: O(N) — because every node is enqueued and dequeued exactly once.
 * - Space Complexity: O(N) — because the Queue must hold an entire horizontal layer of the tree 
 *   simultaneously. In a perfectly balanced tree, the bottom leaf level holds N/2 nodes.
 *
 * Approach 3: Reverse Pre-Order DFS (The Elegant Optimal)
 * What I'd naturally try: BFS takes O(N) heap space. Can I do this with recursion? A standard 
 * Pre-Order traversal goes Root -> Left -> Right. What if I do Root -> Right -> Left? I will 
 * visit the rightmost nodes of every level FIRST. 
 * Why it works: If I keep track of my `currentDepth`, and I check the `size()` of my result list, 
 * the FIRST time `currentDepth == list.size()`, I am guaranteed to be standing on the rightmost 
 * node of that depth! I add it to the list, and for all subsequent nodes at this depth (the left 
 * ones), `currentDepth` will be less than the list size, so I just ignore them.
 * What property removes the bottleneck: Reversing the traversal order ensures the rightmost node 
 * wins the race to the list. We use the result list's own size as a state-tracker.
 * - Time Complexity: O(N) — because we still must visit every node (to ensure no deep left branches are hiding).
 * - Space Complexity: O(H) — bounded strictly by the height of the recursive call stack (O(N) 
 *   for skewed, O(log N) for balanced), eliminating the O(N) wide Queue.
 *
 * The Interview Choice:
 * Write Approach 3 (Reverse Pre-Order DFS). It's roughly 8 lines of code and demonstrates a 
 * profound understanding of how DFS traversal orders interact with state tracking. Mention BFS 
 * verbally as the natural starting point.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Empty Tree -> Returns `[]`.
 * 2. Left-Skewed Tree -> Every node is technically the "rightmost" for its level. Must return the whole left spine.
 * 3. The "Hidden" Branch -> A deep left subtree stretching below a shallow right subtree.
 *
 *
 * 4. DRY RUN (Reverse Pre-Order DFS)
 * ----------------------------------------------------------------------------
 * Tree ("Hidden" Branch scenario):
 *       1
 *      / \
 *     2   3
 *    /
 *   4
 *
 * Target View: [1, 3, 4]
 * 
 * Trace (list = [], depth = 0):
 * 1. dfs(node=1, depth=0):
 *    - depth (0) == list.size (0). Add 1. list = [1].
 *    - Recurse Right: dfs(3, 1).
 *    - Recurse Left: dfs(2, 1).
 *
 * 2. [Right Branch] dfs(node=3, depth=1):
 *    - depth (1) == list.size (1). Add 3. list = [1, 3].
 *    - Recurse Right: dfs(null, 2).
 *    - Recurse Left: dfs(null, 2).
 *    - Returns to Node 1.
 * 
 * 3. [Left Branch] dfs(node=2, depth=1):
 *    - depth (1) != list.size (2). SKIP adding to list! (Node 3 already claimed depth 1).
 *    - Recurse Right: dfs(null, 2).
 *    - Recurse Left: dfs(4, 2).
 *
 * 4. [Left-Left Branch] dfs(node=4, depth=2):
 *    - depth (2) == list.size (2). Add 4. list = [1, 3, 4]. (Node 4 is visible below Node 3!)
 *    - Leaves are null. Returns to Node 2.
 * 
 * Final Result: [1, 3, 4].
 *
 * Pitfalls: 
 * - Using `depth > list.size()` instead of `depth == list.size()`. Since depth increases exactly 
 *   by 1 per level, they will always match exactly when hitting a new level.
 * - Trying to modify standard Pre-Order (Left-first) and overwriting values in a Map. It works, 
 *   but it's inefficient because you repeatedly write to the same level key. Right-first avoids overwrites entirely.
 *
 * Pattern Recognition: 
 * "When you need the 'first', 'last', 'leftmost', or 'rightmost' node of every level -> Think 
 * DFS with a specific traversal order (Left-first vs Right-first) using `depth == size()` to lock in the winner."
 *
 * Interview Script:
 * "A naive approach is to just follow the right child pointers, but that fails if the left subtree 
 * is deeper than the right. We could use BFS level-order traversal and grab the last node of each 
 * queue layer, taking O(N) space. Instead, I'll use a modified DFS. By traversing Root, then Right, 
 * then Left, we guarantee that the first node we visit at any depth is the rightmost one. I'll 
 * use the result list's size to track which depths we've already satisfied. This takes O(N) time 
 * and O(H) space for the recursion stack."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: How would you change this to get the LEFT-side view?
 * A1: Simply swap the recursive calls. Go Left first, then Right. The first node hit at any depth 
 *     will now be the leftmost node.
 *
 * Q2: What if we needed the BOTTOM view of the tree? (Looking from underneath)
 * A2: This requires a completely different approach. We'd use a horizontal distance mapping (root is 0, 
 *     left is -1, right is +1). We do a BFS, continuously overwriting a `Map<Distance, NodeVal>`. 
 *     Because BFS reads top-to-bottom, the LAST node written for any horizontal distance is the 
 *     bottom-most one.
 */

public class RightSideViewStudy {

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
     * APPROACH 3: REVERSE PRE-ORDER DFS (The elegant, optimal choice)
     * Time: O(N) | Space: O(H)
     */
    public List<Integer> rightSideViewDFS(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        // Kick off DFS starting at depth 0
        dfsRightFirst(root, 0, result);
        return result;
    }

    private void dfsRightFirst(TreeNode node, int currentDepth, List<Integer> result) {
        // Base case: fall off a leaf
        if (node == null) {
            return;
        }

        // The core insight: Because we visit Right children before Left children,
        // the FIRST time we reach a new depth, it is guaranteed to be the rightmost node.
        // We use the list's size to know if we've been to this depth before.
        if (currentDepth == result.size()) {
            result.add(node.val);
        }

        // 1. Visit Right branch FIRST
        dfsRightFirst(node.right, currentDepth + 1, result);
        
        // 2. Visit Left branch SECOND
        // Left nodes will only be added if they extend deeper than the right branch.
        dfsRightFirst(node.left, currentDepth + 1, result);
    }

    /**
     * APPROACH 2: LEVEL-ORDER TRAVERSAL BFS (The robust iterative alternative)
     * Time: O(N) | Space: O(N)
     */
    public List<Integer> rightSideViewBFS(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        // Deque used as a Queue for BFS
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            // Lock in the number of nodes currently at this depth level
            int nodesInCurrentLevel = queue.size();

            for (int i = 0; i < nodesInCurrentLevel; i++) {
                TreeNode current = queue.poll();

                // If this is the LAST node in the current level's iteration,
                // it is the rightmost node visible from this depth.
                if (i == nodesInCurrentLevel - 1) {
                    result.add(current.val);
                }

                // Enqueue next level. (Left then Right)
                if (current.left != null) {
                    queue.offer(current.left);
                }
                if (current.right != null) {
                    queue.offer(current.right);
                }
            }
        }

        return result;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        RightSideViewStudy study = new RightSideViewStudy();

        // Standard Case (The "Hidden" Branch from the DRY RUN):
        //       1
        //      / \
        //     2   3
        //    /
        //   4
        TreeNode standard = new TreeNode(1,
                new TreeNode(2, new TreeNode(4), null),
                new TreeNode(3)
        );
        System.out.println("Standard Case (DFS): " + study.rightSideViewDFS(standard)); // Expected: [1, 3, 4]
        System.out.println("Standard Case (BFS): " + study.rightSideViewBFS(standard)); // Expected: [1, 3, 4]

        // Edge Case 1: Empty Tree
        System.out.println("\nEmpty Tree (DFS): " + study.rightSideViewDFS(null)); // Expected: []

        // Edge Case 2: Left-Skewed Tree (Only left children)
        //     1
        //    /
        //   2
        //  /
        // 3
        TreeNode leftSkewed = new TreeNode(1, new TreeNode(2, new TreeNode(3), null), null);
        System.out.println("Left-Skewed Tree (DFS): " + study.rightSideViewDFS(leftSkewed)); // Expected: [1, 2, 3]

        // Edge Case 3: Right-Skewed Tree (Only right children)
        // 1
        //  \
        //   2
        //    \
        //     3
        TreeNode rightSkewed = new TreeNode(1, null, new TreeNode(2, null, new TreeNode(3)));
        System.out.println("Right-Skewed Tree (DFS): " + study.rightSideViewDFS(rightSkewed)); // Expected: [1, 2, 3]
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Traversal Order + Depth vs Size State Tracking.
 * Key Observation: "Right side view" just means the first node you touch at any depth 
 *                  when traversing right-to-left.
 * Memorize: `if (depth == list.size()) list.add(node.val); dfs(right); dfs(left);`
 * Most Common Trap: Thinking you can just follow `node.right` down the tree. If the right 
 *                   branch stops but the left branch continues downward, the left branch 
 *                   becomes visible from the right side. You MUST traverse the whole tree.
 * One-Line Mental Trigger: "Right view = DFS Root-Right-Left, grab when depth == size."
 * ============================================================================
 */


import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ============================================================================
 * STUDY NOTE: MAXIMUM DEPTH OF BINARY TREE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "The constraints say minimum 1 node, but should I handle a null root defensively anyway?" 
 *   (Demonstrates production readiness where inputs don't always respect theoretical constraints).
 * - "Are we counting nodes or edges for the depth?" 
 *   (Crucial: The prompt specifies "count of nodes", meaning a single node has depth 1. Edge-counting would be depth 0).
 * - "Are there extreme memory constraints or extremely unbalanced trees expected in production?" 
 *   (Helps decide between DFS recursion which uses thread stack, and BFS iteration which uses heap memory).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We cannot know the maximum depth without exploring every single root-to-leaf path. 
 * The constraint is how we navigate and track the depth state without redundant work, 
 * balancing between thread stack space (recursion) and heap space (iteration).
 *
 * Approach 1: Recursive DFS (Bottom-Up)
 * What I'd naturally try: A tree's max depth is simply 1 (for the root) plus the maximum of the 
 * depths of its left and right subtrees. I can write a recursive function to ask the children.
 * Why it works: It relies on the recursive "leap of faith". If a node is null, its depth is 0. 
 * Otherwise, the node waits for its left and right subtrees to report their depths, picks the larger 
 * one, adds 1 for itself, and returns it up the chain.
 * What work is being repeated: Nothing. Every node is visited exactly once.
 * What property removes the bottleneck: The call stack implicitly tracks our path and current depth.
 * - Time Complexity: O(N) — because we must visit every single node exactly once to know if it leads to a deeper path.
 * - Space Complexity: O(N) in the worst case (a fully skewed tree/linked list) because the recursion 
 *   stack will hold N frames. O(log N) in the best/average case (a perfectly balanced tree).
 * 
 * Approach 2: Iterative BFS (Level-Order Traversal)
 * What I'd naturally try: If recursion might cause a StackOverflowError on a tree with 1,000,000 nodes, 
 * I should peel the tree layer by layer (level by level) using a Queue.
 * Why it works: We process all nodes at depth 1, then all nodes at depth 2, etc. We increment a 
 * depth counter every time we move to a new level. When the queue is empty, the counter holds the max depth.
 * What property removes the bottleneck: By counting layers broadly instead of diving deep, we eliminate 
 * the recursion stack entirely and rely on heap-allocated Queue memory.
 * - Time Complexity: O(N) — because every node is enqueued and dequeued exactly once.
 * - Space Complexity: O(N) — because in the worst case (a perfectly balanced tree), the bottom-most 
 *   level contains N/2 nodes. The Queue must hold all of them simultaneously.
 *
 * The Interview Choice:
 * I would immediately write the Recursive DFS. It is 3 lines of code, mathematically elegant, and 
 * the universal standard for this problem. However, I would verbally mention: "I'll use recursion, 
 * but if this were a deeply nested tree in a production JVM, I would rewrite this using BFS to avoid 
 * thread stack exhaustion."
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Empty Tree (root == null) -> Must return 0 immediately (defensive programming).
 * 2. Single Node -> Should return 1 without traversing null children.
 * 3. Fully Skewed Tree (Left or Right) -> Tests the worst-case depth (depth = N).
 * 4. Perfectly Balanced Tree -> Tests worst-case breadth (for BFS queue size).
 *
 *
 * 4. DRY RUN (Recursive DFS Bottom-Up)
 * ----------------------------------------------------------------------------
 * Tree:
 *       3
 *      / \
 *     9  20
 *        / \
 *       15  7
 *
 * Call Stack Trace (depth(node)):
 * depth(3) calls depth(9) and depth(20)
 *   depth(9) has no children. Returns max(0, 0) + 1 = 1.
 *   depth(20) calls depth(15) and depth(7)
 *     depth(15) has no children. Returns max(0, 0) + 1 = 1.
 *     depth(7) has no children. Returns max(0, 0) + 1 = 1.
 *   depth(20) receives 1 and 1. Returns max(1, 1) + 1 = 2.
 * depth(3) receives 1 (from 9) and 2 (from 20). Returns max(1, 2) + 1 = 3.
 * Final Result: 3.
 *
 * Pitfalls: 
 * - Misunderstanding the definition of depth vs. height, or nodes vs. edges. If the problem asked 
 *   for edges, a single node would be 0, but here it is 1.
 * - In BFS: Forgetting to capture `queue.size()` at the *start* of the level loop, leading to an 
 *   infinite loop of adding children and miscounting the layers.
 *
 * Pattern Recognition: 
 * "When a problem asks for tree depth, height, or bubbling up values from leaves -> Think Post-order DFS."
 * "When a problem asks for shortest path or processing layer-by-layer -> Think BFS Queue."
 *
 * Interview Script:
 * "The maximum depth of a tree is fundamentally a recursive property: it's 1 plus the maximum depth 
 * of its left and right subtrees. I'll write a bottom-up DFS that visits each node once, giving us 
 * O(N) time and O(H) space for the call stack, where H is the height of the tree."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: How would this code change for an N-ary tree?
 * A1: Instead of `Math.max(depth(left), depth(right))`, I would initialize a `maxChildDepth = 0`, 
 *     iterate over `node.children`, update `maxChildDepth = Math.max(maxChildDepth, depth(child))`, 
 *     and finally return `maxChildDepth + 1`.
 *
 * Q2: What if we wanted the MINIMUM depth instead? Can we just change Math.max to Math.min?
 * A2: No. If a node has only one child, `Math.min` would see the null child as 0 and incorrectly 
 *     report the minimum depth as 1. We only calculate min depth when both children exist, or we 
 *     take the depth of the existing child if one is missing.
 *
 * Q3: If the tree is huge and skewed, recursion crashes. If it's huge and balanced, BFS uses too much memory. What do you do?
 * A3: Use Iterative DFS (simulating the call stack with a heap-allocated Stack). It mimics the memory footprint 
 *     of recursion (bounded by height O(H)) but allocates it on the heap, avoiding StackOverflowErrors.
 */

public class MaxDepthBinaryTreeStudy {

    // Definition for a binary tree node.
    public static class TreeNode {
        int data;
        TreeNode left;
        TreeNode right;
        TreeNode() {}
        TreeNode(int data) { this.data = data; }
        TreeNode(int data, TreeNode left, TreeNode right) {
            this.data = data;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * APPROACH 1: RECURSIVE DFS (The elegant, optimal choice)
     * Time: O(N) | Space: O(N) worst case, O(log N) best case
     */
    public int maxDepthRecursive(TreeNode root) {
        // Base case: We've fallen off a leaf node. 
        // A non-existent node contributes 0 to the depth count.
        if (root == null) {
            return 0;
        }

        // Ask the left child for its max depth
        int leftDepth = maxDepthRecursive(root.left);
        
        // Ask the right child for its max depth
        int rightDepth = maxDepthRecursive(root.right);

        // The depth of the current tree is the max of the subtrees, plus 1 for the current root node.
        return Math.max(leftDepth, rightDepth) + 1;
    }

    /**
     * APPROACH 2: ITERATIVE BFS (The robust choice for deep trees)
     * Time: O(N) | Space: O(N)
     */
    public int maxDepthBFS(TreeNode root) {
        // Defensive check: empty tree has 0 depth
        if (root == null) {
            return 0;
        }

        // ArrayDeque is faster than LinkedList for Queue operations and doesn't allow nulls
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);
        
        int depth = 0; // Tracks the number of layers we've peeled

        while (!queue.isEmpty()) {
            // CRITICAL: We must lock in the size of the queue at the start of this level.
            // This represents exactly how many nodes exist at the current depth layer.
            int nodesInCurrentLevel = queue.size();

            // Process all nodes in the current layer
            for (int i = 0; i < nodesInCurrentLevel; i++) {
                TreeNode current = queue.poll();
                
                // Enqueue the next layer (children of the current layer)
                if (current.left != null) {
                    queue.offer(current.left);
                }
                if (current.right != null) {
                    queue.offer(current.right);
                }
            }
            
            // We finished processing one entire horizontal layer, so depth increases by 1
            depth++;
        }

        return depth;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        MaxDepthBinaryTreeStudy study = new MaxDepthBinaryTreeStudy();

        // Edge Case 1: Empty Tree (Constraints say 1-500 nodes, but testing defensively)
        System.out.println("Empty Tree (DFS): " + study.maxDepthRecursive(null)); // Expected: 0
        System.out.println("Empty Tree (BFS): " + study.maxDepthBFS(null));       // Expected: 0

        // Edge Case 2: Single Node
        TreeNode single = new TreeNode(42);
        System.out.println("Single Node (DFS): " + study.maxDepthRecursive(single)); // Expected: 1
        System.out.println("Single Node (BFS): " + study.maxDepthBFS(single));       // Expected: 1

        // Standard Case: 
        //       3
        //      / \
        //     9  20
        //        / \
        //       15  7
        TreeNode standard = new TreeNode(3,
                new TreeNode(9),
                new TreeNode(20, new TreeNode(15), new TreeNode(7))
        );
        System.out.println("Standard Tree (DFS): " + study.maxDepthRecursive(standard)); // Expected: 3
        System.out.println("Standard Tree (BFS): " + study.maxDepthBFS(standard));       // Expected: 3

        // Edge Case 3: Fully Skewed Tree (Linked List essentially)
        // 1 -> 2 -> 3 -> 4
        TreeNode skewed = new TreeNode(1, null, 
                            new TreeNode(2, null, 
                                new TreeNode(3, null, 
                                    new TreeNode(4))));
        System.out.println("Skewed Tree (DFS): " + study.maxDepthRecursive(skewed)); // Expected: 4
        System.out.println("Skewed Tree (BFS): " + study.maxDepthBFS(skewed));       // Expected: 4
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Bottom-Up Tree Recursion (DFS) or Level-Order Traversal (BFS).
 * Key Observation: The depth of any node is inherently 1 + Math.max(leftDepth, rightDepth).
 * Memorize: The 3-line recursive DFS. It is the gold standard for tree metrics.
 * Most Common Trap: Initializing the base case to return -1 or 1 incorrectly based on edge vs. node counting. 
 *                   For node counts, null returns 0.
 * One-Line Mental Trigger: "Tree Depth = max(left, right) + 1."
 * ============================================================================
 */

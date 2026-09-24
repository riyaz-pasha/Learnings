/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given the root of a binary tree, return the minimum depth of the tree.
 * The minimum depth is the number of nodes along the shortest path from the 
 * root down to the nearest leaf node. 
 * Note: A leaf node is defined as a node with neither a left child nor a right child.
 * 
 * Constraints:
 * - The number of nodes in the tree is in the range [0, 10^5].
 * - -1000 <= Node.val <= 1000
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "If a node has a left child but no right child, is the right side considered depth 0?"
 *   Crucial question! No. A leaf must have ZERO children. If a node has one child, 
 *   it is not a leaf, so we cannot terminate the path there. (This is the most common bug).
 * - "What is the depth of an empty tree (root == null)?"
 *   Usually 0. Clarifying this prevents NullPointerExceptions.
 * - "Is the tree guaranteed to be balanced?"
 *   No. It could be a linked list of 10^5 nodes, which heavily dictates whether we 
 *   should use DFS (risk of StackOverflow) or BFS.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We need the *shortest* path to a specific condition (a leaf node). 
 * 
 * --- APPROACH 1: Recursive Depth-First Search (DFS) (The "Textbook" Try) ---
 * 1. What I'd naturally try: Write a recursive function. If the node is null, return 0. 
 *    Otherwise, find the min depth of the left subtree, the min depth of the right 
 *    subtree, and return 1 + Math.min(left, right).
 * 2. Why it works: It exhaustive explores the tree and calculates the height of all paths.
 * 3. Why it's flawed (The Pitfall): If a node only has a left child, `Math.min(left, right)` 
 *    will evaluate to `Math.min(depth, 0) == 0`. The function will incorrectly claim the 
 *    minimum depth is 1, even though this node isn't a leaf! We must add logic to ignore 
 *    null children.
 * 4. Why it's costly: Even if fixed, DFS explores *deeply*. If the left subtree has 
 *    99,999 nodes and the right subtree is a single leaf, DFS might traverse all 99,999 
 *    left nodes before checking the right node.
 * 5. Time Complexity: O(N) — because we may have to visit every node to find the leaves.
 * 6. Space Complexity: O(N) — because in a highly skewed tree (like a linked list), the 
 *    recursion stack will grow to N, risking a StackOverflowError.
 * 
 * [The Core Observation]
 * We are looking for the *nearest* leaf. Depth-First Search prioritizes going as far away 
 * from the root as possible before checking neighbors. Breadth-First Search (BFS) prioritizes 
 * checking everything close to the root before moving deeper.
 * 
 * --- APPROACH 2: Level-Order Breadth-First Search (BFS) (The Optimal Way) ---
 * 1. How it works: Use a Queue. Push the root. Check level 1. If any node has no children, 
 *    WE ARE DONE. Return the current depth. If not, push their children and go to level 2.
 * 2. Why it's optimal: Early Exit. The very first time we see a leaf node, we are guaranteed 
 *    it is the shallowest leaf in the tree. We instantly return, completely abandoning 
 *    any deeper subtrees. 
 * 3. Time Complexity: O(N) in the worst case (a perfectly balanced tree where all leaves 
 *    are at the bottom), but O(1) in the best case (root has a leaf as a direct child, 
 *    skipping massive subtrees on the other side).
 * 4. Space Complexity: O(N) — specifically O(W) where W is the maximum width of the tree. 
 *    For a perfect binary tree, the bottom level holds roughly N/2 nodes. 
 * 
 * [Which one I'd write in an interview]
 * Approach 2 (BFS). Not only does it avoid recursion depth limits, but the "early exit" 
 * optimization shows the interviewer you understand the functional difference between 
 * BFS (shortest path) and DFS (exhaustive search).
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Empty Tree: Root is null -> return 0.
 * - Single Node: Root has no children -> return 1.
 * - Skewed Tree ("Linked List"): 1 -> 2 -> 3 -> null. The BFS queue size remains 1, 
 *   safely returning 3 without hitting stack limits.
 * - Unbalanced Tree: Left side is 10,000 nodes deep, right side is 1 node deep. 
 *   BFS returns 2 after checking just 3 total nodes!
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * A null child does NOT mean the current node is a leaf, unless BOTH children are null.
 * 
 * [Examples & Diagram]
 * Tree:
 *          1
 *         / \
 *        2   3
 *       /     
 *      4       
 *       \
 *        5
 * 
 * [Dry Run (BFS)]
 * Init: Queue = [1], depth = 1
 * 
 * Iteration 1 (Level 1):
 * - Pop 1. Is it a leaf? (Left=2, Right=3). No.
 * - Push 2, Push 3. Queue = [2, 3]
 * - depth becomes 2.
 * 
 * Iteration 2 (Level 2):
 * - Pop 2. Is it a leaf? (Left=4, Right=null). No. (Notice it's not a leaf!)
 * - Push 4. Queue = [3, 4]
 * - Pop 3. Is it a leaf? (Left=null, Right=null). YES!
 * - First leaf found. Return current depth: 2.
 * (Notice we completely ignored nodes 4 and 5!).
 * 
 * [Pitfalls]
 * - DFS Trap: `return 1 + Math.min(minDepth(root.left), minDepth(root.right))`. 
 *   On node 2 in the diagram above, this evaluates to `1 + Math.min(depth of 4, depth of null)`. 
 *   Since depth of null is 0, it claims node 2 has a min depth of 1, resulting in a total 
 *   answer of 2 down the left path, which is mathematically false.
 * 
 * [Pattern Recognition]
 * When you see: "Shortest path", "Nearest", "Minimum steps to a target".
 * Think: Breadth-First Search (BFS).
 * 
 * [Interview Script]
 * "We could use DFS to find the minimum depth, but that forces us to explore every single 
 * path to the bottom before we know which is shortest. Instead, I'll use BFS. BFS expands 
 * level by level, meaning the very first leaf node we encounter is mathematically guaranteed 
 * to be on the shortest path. We can just return the depth immediately and short-circuit 
 * the rest of the search, saving massive amounts of time on unbalanced trees."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: If the tree is perfectly balanced, which is better: DFS or BFS?
 * A: In a perfectly balanced tree, all leaves are at the exact same depth. Both will have 
 *    to explore O(N) nodes. DFS is actually slightly better here because the BFS Queue will 
 *    hold N/2 nodes at the final level, whereas DFS only uses O(log N) space for the stack.
 * 
 * Q: How would you find the MAXIMUM depth?
 * A: For max depth, the BFS early exit no longer applies—we must check every node anyway. 
 *    In that case, a simple recursive DFS (`1 + Math.max(left, right)`) is cleaner and 
 *    generally preferred unless the tree is deep enough to cause a StackOverflow.
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.LinkedList;
import java.util.Queue;

public class MinimumDepthOfBinaryTree {

    // Standard Binary Tree Node definition
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int val) { this.val = val; }
    }

    /**
     * APPROACH 2: Iterative Breadth-First Search (The Optimal Way)
     */
    public static int minDepthBFS(TreeNode root) {
        // Edge case: Empty tree has depth 0
        if (root == null) {
            return 0;
        }

        // Standard BFS setup
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        // Depth starts at 1 because the root exists
        int depth = 1;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();

            // Process all nodes at the current depth
            for (int i = 0; i < levelSize; i++) {
                TreeNode current = queue.poll();

                // THE EARLY EXIT: If this node has NO children, it is a leaf.
                // Because we are using BFS, this is guaranteed to be the nearest leaf.
                if (current.left == null && current.right == null) {
                    return depth;
                }

                // Otherwise, queue up the children for the next depth level
                if (current.left != null) {
                    queue.offer(current.left);
                }
                if (current.right != null) {
                    queue.offer(current.right);
                }
            }
            
            // We finished a horizontal level without finding a leaf. 
            // Increment depth and move down.
            depth++;
        }

        return depth;
    }

    /**
     * APPROACH 1: Recursive Depth-First Search (Included for contrast)
     */
    public static int minDepthDFS(TreeNode root) {
        if (root == null) return 0;

        // If both children are null, we found a leaf
        if (root.left == null && root.right == null) {
            return 1;
        }

        // PITFALL FIX: If one child is null, we MUST NOT use Math.min(), 
        // otherwise we will calculate a depth of 0 for the missing child.
        // We force the search down the path that actually exists.
        if (root.left == null) {
            return 1 + minDepthDFS(root.right);
        }
        if (root.right == null) {
            return 1 + minDepthDFS(root.left);
        }

        // If both children exist, find the minimum of the two paths
        return 1 + Math.min(minDepthDFS(root.left), minDepthDFS(root.right));
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test Case 1: Standard balanced-ish tree
        //       3
        //      / \
        //     9  20
        //        / \
        //       15  7
        TreeNode root1 = new TreeNode(3);
        root1.left = new TreeNode(9);
        root1.right = new TreeNode(20);
        root1.right.left = new TreeNode(15);
        root1.right.right = new TreeNode(7);
        
        System.out.println("Test 1 (BFS Optimal): " + minDepthBFS(root1)); // Expected: 2 (Node 9)
        
        // Test Case 2: The Skewed Line Trap
        // 2
        //  \
        //   3
        //    \
        //     4
        //      \
        //       5
        //        \
        //         6
        TreeNode root2 = new TreeNode(2);
        root2.right = new TreeNode(3);
        root2.right.right = new TreeNode(4);
        root2.right.right.right = new TreeNode(5);
        root2.right.right.right.right = new TreeNode(6);
        
        System.out.println("Test 2 (Skewed trap BFS): " + minDepthBFS(root2)); // Expected: 5
        System.out.println("Test 2 (Skewed trap DFS): " + minDepthDFS(root2)); // Expected: 5

        // Test Case 3: Empty Tree
        System.out.println("Test 3 (Empty): " + minDepthBFS(null)); // Expected: 0
        
        // Test Case 4: Single Node
        System.out.println("Test 4 (Single): " + minDepthBFS(new TreeNode(42))); // Expected: 1
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Breadth-First Search (BFS) for unweighted shortest paths.
 * - Key observation: BFS can short-circuit and exit the moment it hits a leaf, 
 *   potentially saving O(N) operations in a highly unbalanced tree.
 * - Most common trap: In DFS, returning `1 + Math.min(left, right)` when one 
 *   child is null. A node with one child is NOT a leaf!
 * - Mental trigger: "Minimum depth" / "Nearest target" -> Queue-based BFS.
 */


/**
 * DFS Recursive Solution
 *
 * Time Complexity: O(N) -> Visit every node once
 * Space Complexity: O(H) -> recursion stack (H = height)
 */
class MinDepthDFS {

    public int minDepth(TreeNode root) {

        // Base case
        if (root == null) return 0;

        // If leaf node → depth = 1
        if (root.left == null && root.right == null) {
            return 1;
        }

        // If one side is null → we MUST go through the other side
        if (root.left == null) {
            // Only right subtree exists
            return 1 + minDepth(root.right);
        }

        if (root.right == null) {
            // Only left subtree exists
            return 1 + minDepth(root.left);
        }

        // Both children exist → take minimum
        return 1 + Math.min(
                minDepth(root.left),
                minDepth(root.right)
        );
    }
}

import java.util.*;

/**
 * BFS Level Order Solution (Optimal for Minimum Depth)
 *
 * Time Complexity: O(N)
 * Space Complexity: O(N)
 */
class MinDepthBFS {

    public int minDepth(TreeNode root) {

        if (root == null) return 0;

        Queue<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);

        int depth = 1; // start from root

        while (!queue.isEmpty()) {

            int size = queue.size();

            for (int i = 0; i < size; i++) {

                TreeNode node = queue.poll();

                // 🎯 FIRST leaf → answer
                if (node.left == null && node.right == null) {
                    return depth;
                }

                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }

            depth++; // move to next level
        }

        return depth;
    }
}


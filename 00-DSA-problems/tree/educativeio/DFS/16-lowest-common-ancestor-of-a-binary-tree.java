import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * STUDY NOTE: LOWEST COMMON ANCESTOR (LCA) OF A BINARY TREE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Are p and q guaranteed to exist in the tree?" 
 *   (Crucial: If they might not exist, the standard optimal O(N) solution will falsely return p if it finds p but q isn't in the tree).
 * - "Is this a Binary Search Tree (BST) or a regular Binary Tree?" 
 *   (If it's a BST, we can solve this in O(H) time without visiting the whole tree by just comparing values to the root).
 * - "Do the tree nodes have a pointer to their parent?" 
 *   (If yes, this ceases to be a tree traversal problem and becomes the 'Intersection of Two Linked Lists' problem).
 * - "Can we assume p and q are given as actual node references, not just integer values?" 
 *   (Ensures we use `==` for object reference comparison rather than just `.val` checks, which is safer).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We are forced to search top-down (because pointers only go to children), but the concept 
 * of an "ancestor" requires aggregating information bottom-up. The bottleneck is how we 
 * search for two distinct targets and reliably bubble up their intersection point without 
 * repeatedly traversing the same branches.
 *
 * Approach 1: Path Tracing (The Intuitive Brute Force)
 * What I'd naturally try: I know how to find the path from the root to any node. I'll write a 
 * DFS that records the exact path from `root` to `p` in one list, and `root` to `q` in another list. 
 * Then, I'll iterate through both lists side-by-side until the nodes diverge. The last matching 
 * node is the LCA.
 * Why it works: It visually reconstructs the lineage of both nodes to find the divergence point.
 * Why it's costly: We are storing entire paths in auxiliary arrays.
 * - Time Complexity: O(N) — because we traverse the tree to find p, traverse again to find q, and compare lists.
 * - Space Complexity: O(N) — because we allocate two Lists that could each hold N nodes in a fully skewed tree.
 * 
 * Approach 2: Iterative with Parent Map (The Intermediate Strategy)
 * What I'd naturally try: If the nodes had parent pointers, I could just walk upwards from `p` 
 * and `q` until they collide. Since they don't, I'll traverse the tree (BFS or DFS) and build a 
 * `HashMap<TreeNode, TreeNode>` mapping every node to its parent. I stop once both `p` and `q` are in the map. 
 * Then I trace `p`'s ancestors into a `HashSet`, and walk `q` up until it hits that Set.
 * Why it works: It artificially creates the parent pointers we wish we had, turning a tree problem 
 * into an intersecting linked-list problem.
 * What work is being repeated: Building the map creates significant memory overhead and autoboxing overhead.
 * - Time Complexity: O(N) — because we visit nodes to build the map, and then trace upwards.
 * - Space Complexity: O(N) — because the HashMap stores up to N nodes and their parents.
 *
 * Approach 3: Bottom-Up Recursive DFS (The Elegant Optimal)
 * What I'd naturally try: I don't need to save paths. I just need to ask a question: "Is p or q in your subtree?" 
 * A node asks its left and right children. If BOTH the left and right children say "Yes, I found one!", 
 * then this current node MUST be the divergence point (the LCA). If only one child says "Yes", the 
 * node just passes that "Yes" up to its own parent.
 * Why it works: It leverages the recursive unwinding of the call stack. The very first node that 
 * receives non-null responses from BOTH left and right branches is mathematically guaranteed to be the LCA.
 * What property removes the bottleneck: The implicit call stack tracks our lineage perfectly, completely 
 * eliminating the need for HashMaps or path arrays.
 * - Time Complexity: O(N) — because in the worst case (targets are at the deep leaves), we visit every node exactly once.
 * - Space Complexity: O(H) — bounded strictly by the height of the recursive call stack (O(N) for skewed, O(log N) for balanced). 
 *   Requires zero auxiliary data structures.
 *
 * The Interview Choice:
 * Always write Approach 3 (Bottom-Up Recursive DFS). It is a legendary ~10-line algorithm that 
 * evaluates your deep understanding of Post-Order recursion and state-bubbling. 
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. p is an ancestor of q (or vice versa) -> The traversal hits p first, short-circuits, and returns p. 
 *    Since the prompt guarantees both exist, this short-circuit perfectly yields the correct LCA.
 * 2. Tree only has 2 nodes -> One is the root, one is the child. Safely returns the root.
 * 3. Left-Skewed or Right-Skewed tree -> Tests the maximum stack depth.
 *
 *
 * 4. DRY RUN (Optimal Recursive DFS)
 * ----------------------------------------------------------------------------
 * Tree:
 *        3
 *       / \
 *      5   1
 *     / \   \
 *    6   2   8
 *       / \
 *      7   4
 * 
 * Targets: p = 6, q = 4
 *
 * Call Stack Trace (Post-Order):
 * 1. dfs(3): calls dfs(5) and dfs(1)
 * 2. [Left Branch] dfs(5): calls dfs(6) and dfs(2)
 * 3. [Left-Left] dfs(6): `node == p`! Returns Node(6) immediately to dfs(5).
 * 4. [Left-Right] dfs(2): calls dfs(7) and dfs(4)
 * 5. [Left-Right-Left] dfs(7): leaves are null, returns `null` to dfs(2).
 * 6. [Left-Right-Right] dfs(4): `node == q`! Returns Node(4) immediately to dfs(2).
 * 7. Back at dfs(2): receives left=`null`, right=Node(4). 
 *    - Only one child found something. Returns Node(4) to dfs(5).
 * 8. Back at dfs(5): receives left=Node(6), right=Node(4).
 *    - BOTH children returned non-null! 
 *    - This means 5 is the divergence point. Returns Node(5) to dfs(3).
 * 9. [Right Branch] dfs(1): calls dfs(null), dfs(8), all return `null`. Returns `null` to dfs(3).
 * 10. Back at dfs(3): receives left=Node(5), right=`null`.
 *     - Only one child found something. Returns Node(5) up to main.
 * Final Result: Node(5).
 *
 * Pitfalls: 
 * - Searching the entire tree unnecessarily. If `node == p || node == q`, you MUST return `node` 
 *   immediately. If you continue searching its children, you are wasting cycles.
 * - Trying to compare node values instead of node references. If nodes have duplicate values (though 
 *   constraints say they don't here), `node.val == p.val` will fail. Always use `node == p`.
 *
 * Pattern Recognition: 
 * "When aggregating a boolean-like state from children to identify a singular junction -> Think 
 * Post-Order Traversal where a node acts if BOTH children return true/non-null."
 *
 * Interview Script:
 * "Since we don't have parent pointers, I'll use a bottom-up DFS. At each node, I'll check if it is 
 * p or q. If it is, I return it. Otherwise, I recursively ask the left and right children. If a node 
 * receives non-null answers from BOTH its left and right children, it means one target is on the left 
 * and one is on the right, making this current node the Lowest Common Ancestor. If it only receives 
 * one non-null answer, it passes that answer up. This takes O(N) time and O(H) space."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if it is NOT guaranteed that both p and q exist in the tree?
 * A1: The optimal solution fails because it short-circuits. If it finds `p`, it returns it, assuming 
 *     `q` is somewhere underneath it. If `q` actually doesn't exist, it falsely returns `p`. We must 
 *     traverse the FULL tree to ensure both are found. We can do this by returning an integer state 
 *     (0, 1, or 2 nodes found), or by doing a boolean check `boolean foundP, foundQ` across the whole tree.
 *
 * Q2: What if we needed to find the LCA of 'K' nodes instead of just 2?
 * A2: The exact same recursive logic applies. If `node == target_node`, return it. If a node gets 
 *     non-null from more than 1 child, it's the LCA. (Actually, if we aggregate returned nodes, any 
 *     node that gets >= 2 non-null returns from its children, OR is a target and gets 1 non-null 
 *     from a child, is the LCA).
 *
 * Q3: What if this was a Binary Search Tree (BST)?
 * A3: We don't need to search both branches. We can just check the values. If `p` and `q` are BOTH 
 *     smaller than `root`, go left. If BOTH are larger, go right. The first moment they split 
 *     (one is larger, one is smaller, or one equals the root), that root is the LCA. This takes 
 *     strictly O(H) time and O(1) space iteratively.
 */

public class LowestCommonAncestorStudy {

    // Definition for a binary tree node.
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int x) { val = x; }
        
        // Custom toString for easier debugging
        @Override
        public String toString() {
            return "Node(" + val + ")";
        }
    }

    /**
     * APPROACH 3: BOTTOM-UP RECURSIVE DFS (The elegant, optimal choice)
     * Time: O(N) | Space: O(H)
     */
    public TreeNode lowestCommonAncestorRecursive(TreeNode root, TreeNode p, TreeNode q) {
        // Base case: If we reach a null leaf, return null.
        // Short-circuit: If we find p or q, we immediately return that node to the caller.
        // We do NOT need to search its children because if the other target IS a child, 
        // the current node is rightfully the LCA anyway.
        if (root == null || root == p || root == q) {
            return root;
        }

        // 1. Ask the left subtree: "Did you find p or q?"
        TreeNode leftSearchResult = lowestCommonAncestorRecursive(root.left, p, q);
        
        // 2. Ask the right subtree: "Did you find p or q?"
        TreeNode rightSearchResult = lowestCommonAncestorRecursive(root.right, p, q);

        // 3. Process the current node's findings.
        // If BOTH left and right found a target, p and q are split across this node.
        // This node is mathematically the Lowest Common Ancestor.
        if (leftSearchResult != null && rightSearchResult != null) {
            return root;
        }

        // If only ONE side found a target, pass that target up to our parent.
        // If neither found a target, both are null, so this safely returns null.
        return leftSearchResult != null ? leftSearchResult : rightSearchResult;
    }

    /**
     * APPROACH 2: ITERATIVE WITH PARENT MAP (The robust iterative alternative)
     * Time: O(N) | Space: O(N)
     */
    public TreeNode lowestCommonAncestorIterative(TreeNode root, TreeNode p, TreeNode q) {
        // Map to store <Node, Parent>
        Map<TreeNode, TreeNode> parentMap = new HashMap<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        
        parentMap.put(root, null);
        stack.push(root);

        // Iterate until we have definitively found and mapped the parents of BOTH p and q
        while (!parentMap.containsKey(p) || !parentMap.containsKey(q)) {
            TreeNode current = stack.pop();

            if (current.left != null) {
                parentMap.put(current.left, current);
                stack.push(current.left);
            }
            if (current.right != null) {
                parentMap.put(current.right, current);
                stack.push(current.right);
            }
        }

        // Trace p's entire lineage up to the root, storing all ancestors in a Set
        Set<TreeNode> pAncestors = new HashSet<>();
        while (p != null) {
            pAncestors.add(p);
            p = parentMap.get(p);
        }

        // Trace q's lineage upwards. The FIRST node we hit that is already in p's ancestor set is the LCA.
        while (!pAncestors.contains(q)) {
            q = parentMap.get(q);
        }

        return q;
    }

    /**
     * APPROACH 1: PATH TRACING (The Intuitive Brute Force)
     * Time: O(N) | Space: O(N)
     * Included to demonstrate the mental starting point.
     */
    public TreeNode lowestCommonAncestorPathTracing(TreeNode root, TreeNode p, TreeNode q) {
        List<TreeNode> pathP = new ArrayList<>();
        List<TreeNode> pathQ = new ArrayList<>();

        // Populate both paths
        findPath(root, p, pathP);
        findPath(root, q, pathQ);

        // Compare paths to find the last common node
        TreeNode lca = null;
        int i = 0;
        while (i < pathP.size() && i < pathQ.size()) {
            if (pathP.get(i) == pathQ.get(i)) {
                lca = pathP.get(i);
            } else {
                break; // Paths diverged
            }
            i++;
        }

        return lca;
    }

    // Helper for Approach 1
    private boolean findPath(TreeNode root, TreeNode target, List<TreeNode> path) {
        if (root == null) return false;

        path.add(root);

        if (root == target) return true;

        if (findPath(root.left, target, path) || findPath(root.right, target, path)) {
            return true;
        }

        // Backtrack if target not found in this branch
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        LowestCommonAncestorStudy study = new LowestCommonAncestorStudy();

        // Standard Case: 
        //        3
        //       / \
        //      5   1
        //     / \   \
        //    6   2   8
        //       / \
        //      7   4
        TreeNode node7 = new TreeNode(7);
        TreeNode node4 = new TreeNode(4);
        TreeNode node6 = new TreeNode(6);
        TreeNode node2 = new TreeNode(2);
        node2.left = node7;
        node2.right = node4;
        
        TreeNode node5 = new TreeNode(5);
        node5.left = node6;
        node5.right = node2;
        
        TreeNode node8 = new TreeNode(8);
        TreeNode node1 = new TreeNode(1);
        node1.right = node8;
        
        TreeNode root = new TreeNode(3);
        root.left = node5;
        root.right = node1;

        // Test 1: LCA of 5 and 1 is 3 (divergence at root)
        System.out.println("Test 1 (Optimal): " + study.lowestCommonAncestorRecursive(root, node5, node1)); // Expected: Node(3)
        System.out.println("Test 1 (Iterative): " + study.lowestCommonAncestorIterative(root, node5, node1)); 

        // Test 2: LCA of 6 and 4 is 5
        System.out.println("\nTest 2 (Optimal): " + study.lowestCommonAncestorRecursive(root, node6, node4)); // Expected: Node(5)
        System.out.println("Test 2 (Path Tracing): " + study.lowestCommonAncestorPathTracing(root, node6, node4)); 

        // Test 3 (Edge Case): LCA of 5 and 4 is 5 (p is ancestor of q)
        // Here, finding 5 short-circuits the search, returning 5.
        System.out.println("\nTest 3 (Optimal - Ancestor Trap): " + study.lowestCommonAncestorRecursive(root, node5, node4)); // Expected: Node(5)
        System.out.println("Test 3 (Iterative): " + study.lowestCommonAncestorIterative(root, node5, node4)); 
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Post-Order State Bubbling.
 * Key Observation: The LCA is uniquely the first node from the bottom where BOTH 
 *                  left and right recursive calls return a non-null target.
 * Memorize: `if (root == null || root == p || root == q) return root;`
 *           `if (left != null && right != null) return root; return left != null ? left : right;`
 * Most Common Trap: Assuming this works if p or q might NOT be in the tree. The 
 *                   short-circuit `root == p` hides whether `q` exists lower down.
 * One-Line Mental Trigger: "LCA = Did left find one? Did right find one? If both yes, I am the LCA."
 * ============================================================================
 */


import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * STUDY NOTE: INORDER SUCCESSOR IN BST
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Do the tree nodes have parent pointers?" 
 *   (Crucial: If parent pointers exist, we don't even need the root to find the successor, which changes the optimal algorithm entirely).
 * - "What should I return if the node is the absolute maximum value in the tree?" 
 *   (Confirms understanding that the maximum node has no successor and should safely return null).
 * - "Are we searching by node reference or node value?" 
 *   (Since values are unique, either works, but clarifying ensures we compare `node.val` correctly instead of object addresses).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We need to find the node with the smallest value that is strictly greater than `p.val`. 
 * While a standard tree traversal works, it completely ignores the structural guarantee of a 
 * Binary Search Tree (left < node < right). The constraint is solving this without inspecting 
 * nodes that are mathematically impossible to be the successor.
 *
 * Approach 1: Full In-Order Traversal (The Brute Force)
 * What I'd naturally try: An In-Order traversal (Left-Node-Right) visits a BST in strictly 
 * ascending order. I'll traverse the entire tree, dump all nodes into an ArrayList, find `p` 
 * in the list, and just return the element right after it.
 * Why it works: It perfectly flattens the 2D tree into a 1D sorted array.
 * Why it's too slow/costly: It does full O(N) work even if `p` is the root and its successor 
 * is just one step to the right. 
 * - Time Complexity: O(N) — because every node is visited to build the list.
 * - Space Complexity: O(N) — because we allocate an array of size N, plus the call stack.
 *
 * Approach 2: Recursive In-Order with Early Exit (The Step-Up)
 * What I'd naturally try: I don't need a full array. I'll do a recursive In-Order traversal 
 * but keep a boolean flag `foundP`. Once I visit `p`, I set the flag to true. The *very next* 
 * node I visit is guaranteed to be the successor, so I'll save it and stop traversing.
 * Why it works: It leverages the same sorted-order property but avoids processing the whole tree.
 * Why it's still suboptimal: It completely ignores the BST property. If `p` is on the far right, 
 * this approach still visits the entire left half of the tree pointlessly.
 * - Time Complexity: O(N) worst-case — if `p` is the largest element, we still visit every node.
 * - Space Complexity: O(H) — bounded by the recursion stack.
 *
 * Approach 3: Iterative BST Search (The Optimal & Elegant Choice)
 * What I'd naturally try: Let's actually use the BST rules! I want the smallest value > `p.val`. 
 * I start at the root. If `current.val > p.val`, this node is a valid *candidate* for the successor. 
 * I'll save it, but maybe there's a smaller valid candidate? So I'll go Left to check. 
 * If `current.val <= p.val`, this node is too small, so the successor MUST be to the Right.
 * Why it works: We use binary search logic, halving the search space at every step.
 * What property removes the bottleneck: By only walking a single root-to-leaf path and updating a 
 * candidate tracker, we eliminate all recursive overhead and unnecessary branch exploration.
 * - Time Complexity: O(H) — because we traverse exactly one path from root to a leaf, where H is 
 *   tree height (O(log N) balanced, O(N) skewed).
 * - Space Complexity: O(1) — because we only use two pointers (`current` and `successor`), 
 *   requiring strictly constant extra memory.
 *
 * The Interview Choice:
 * Always write Approach 3 (Iterative BST Search). It is shockingly concise (about 10 lines) and 
 * proves you understand both binary search mechanics and O(1) space optimization.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Node `p` is the maximum element -> Traversal falls off the tree, returns null.
 * 2. Node `p` has a right child -> The successor is the absolute leftmost node in that right subtree.
 * 3. Node `p` has no right child -> The successor is an ancestor (the lowest ancestor where `p` is in its left subtree). The iterative approach naturally tracks this without needing parent pointers!
 *
 *
 * 4. DRY RUN (Optimal Iterative Approach)
 * ----------------------------------------------------------------------------
 * Tree:
 *         20
 *        /  \
 *       10   30
 *      /  \
 *     5   15
 *           \
 *            17
 * 
 * Target Node: p = 15
 *
 * State Tracker (successor = null, current = 20):
 * 1. current = 20. 
 *    - Is 20 > 15? YES. 
 *    - 20 is a potential successor. successor = 20.
 *    - Go left to find a tighter match: current = 10.
 * 2. current = 10.
 *    - Is 10 > 15? NO.
 *    - Go right to find larger values: current = 15.
 * 3. current = 15.
 *    - Is 15 > 15? NO. (We want strictly greater).
 *    - Go right: current = 17.
 * 4. current = 17.
 *    - Is 17 > 15? YES.
 *    - 17 is a better potential successor. successor = 17.
 *    - Go left: current = null.
 * 5. current == null. Loop ends.
 * Final Result: Node(17).
 *
 * Pitfalls: 
 * - Using `>=` instead of `>`. The successor must be strictly greater than `p`.
 * - Attempting to use a standard tree traversal (DFS/BFS). If you aren't using the binary search 
 *   property (comparing `node.val` to `p.val` to decide left/right), you are wasting time.
 *
 * Pattern Recognition: 
 * "When finding a successor, predecessor, floor, or ceiling in a BST -> Use a while loop with a 
 * single tracking variable, discarding half the tree at every step."
 *
 * Interview Script:
 * "Since this is a Binary Search Tree, we don't need to do a full traversal. I can find the 
 * successor in O(H) time by using binary search logic. I'll maintain a 'successor' candidate. 
 * Starting at the root, if the current node is greater than p, it might be the successor, so I 
 * save it and go left to find a closer match. If the current node is less than or equal to p, 
 * I just go right. Because this only requires two pointers, the space complexity is O(1)."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: How would you find the Inorder PREDECESSOR instead?
 * A1: The logic is perfectly mirrored. If `current.val < p.val`, the current node is a potential 
 *     predecessor, so we save it and go Right. If `current.val >= p.val`, we go Left.
 *
 * Q2: What if the nodes had a `parent` pointer? How does that change the algorithm?
 * A2: We wouldn't even need the `root` parameter. 
 *     1. If `p` has a right child, the successor is the leftmost node in that right subtree.
 *     2. If `p` has NO right child, we walk up the parent pointers. The successor is the first 
 *        parent we reach by moving up a LEFT branch.
 *     This takes O(H) worst case, but O(1) average time, and strictly O(1) space.
 */

public class InorderSuccessorStudy {

    // Definition for a binary tree node.
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int x) { val = x; }

        @Override
        public String toString() {
            return "Node(" + val + ")";
        }
    }

    /**
     * APPROACH 3: ITERATIVE BST SEARCH (The Optimal, Professional Choice)
     * Time: O(H) | Space: O(1)
     */
    public TreeNode inorderSuccessorOptimal(TreeNode root, TreeNode p) {
        TreeNode successor = null;
        TreeNode current = root;

        // Traverse down the tree using binary search properties
        while (current != null) {
            if (current.val > p.val) {
                // current is strictly greater than p. 
                // This makes it a valid candidate for the successor.
                successor = current;
                
                // But we want the SMALLEST valid candidate, so we search the left subtree.
                current = current.left;
            } else {
                // current is less than or equal to p.
                // The successor MUST be larger, so we discard the left side and search the right.
                current = current.right;
            }
        }

        return successor;
    }

    /**
     * APPROACH 2: RECURSIVE IN-ORDER WITH EARLY EXIT 
     * Time: O(N) | Space: O(H)
     * Included to demonstrate state tracking and the inefficiency of ignoring BST rules.
     */
    private boolean foundP = false;
    private TreeNode recursiveSuccessor = null;

    public TreeNode inorderSuccessorRecursive(TreeNode root, TreeNode p) {
        foundP = false;
        recursiveSuccessor = null;
        inorderDFS(root, p);
        return recursiveSuccessor;
    }

    private void inorderDFS(TreeNode node, TreeNode p) {
        // Base case or early exit if we already found the answer
        if (node == null || recursiveSuccessor != null) {
            return;
        }

        // 1. Traverse Left
        inorderDFS(node.left, p);

        // 2. Process Node
        // If the successor is already found, short-circuit processing
        if (recursiveSuccessor != null) return;
        
        if (foundP) {
            recursiveSuccessor = node; // The very first node processed after finding p is the successor
            return;
        }
        
        if (node == p) {
            foundP = true;
        }

        // 3. Traverse Right
        inorderDFS(node.right, p);
    }

    /**
     * APPROACH 1: BRUTE FORCE (List Collection)
     * Time: O(N) | Space: O(N)
     * Shows exactly what we are avoiding.
     */
    public TreeNode inorderSuccessorBruteForce(TreeNode root, TreeNode p) {
        List<TreeNode> sortedNodes = new ArrayList<>();
        buildList(root, sortedNodes);
        
        for (int i = 0; i < sortedNodes.size(); i++) {
            if (sortedNodes.get(i) == p && i + 1 < sortedNodes.size()) {
                return sortedNodes.get(i + 1);
            }
        }
        
        return null;
    }

    private void buildList(TreeNode node, List<TreeNode> list) {
        if (node == null) return;
        buildList(node.left, list);
        list.add(node);
        buildList(node.right, list);
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        InorderSuccessorStudy study = new InorderSuccessorStudy();

        // Standard Case: The example from the DRY RUN
        //         20
        //        /  \
        //       10   30
        //      /  \
        //     5   15
        //           \
        //            17
        TreeNode root = new TreeNode(20);
        TreeNode node10 = new TreeNode(10);
        TreeNode node30 = new TreeNode(30);
        TreeNode node5 = new TreeNode(5);
        TreeNode node15 = new TreeNode(15);
        TreeNode node17 = new TreeNode(17);

        root.left = node10;
        root.right = node30;
        node10.left = node5;
        node10.right = node15;
        node15.right = node17;

        // Case 1: Target has a right child
        System.out.println("Target 10 (Optimal): " + study.inorderSuccessorOptimal(root, node10)); // Expected: 15
        
        // Case 2: Target has NO right child (Successor is an ancestor)
        System.out.println("Target 17 (Optimal): " + study.inorderSuccessorOptimal(root, node17)); // Expected: 20
        System.out.println("Target 17 (Recursive): " + study.inorderSuccessorRecursive(root, node17)); // Expected: 20
        
        // Case 3: Target is the absolute maximum node
        System.out.println("Target 30 (Optimal): " + study.inorderSuccessorOptimal(root, node30)); // Expected: null
        
        // Case 4: Target is the absolute minimum node
        System.out.println("Target 5 (Optimal): " + study.inorderSuccessorOptimal(root, node5));   // Expected: 10
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Binary Search narrowing with a Candidate Tracker.
 * Key Observation: The successor is the smallest value greater than p. Every time you move left 
 *                  from a node strictly greater than p, that node becomes your new best guess.
 * Memorize: `if (curr.val > p.val) { succ = curr; curr = curr.left; } else curr = curr.right;`
 * Most Common Trap: Assuming the successor is always just the leftmost node of p's right subtree. 
 *                   If p has no right subtree, the successor sits above it in the ancestry path. 
 *                   The optimal iterative approach automatically handles both scenarios flawlessly.
 * One-Line Mental Trigger: "Inorder Successor? BST search, save when greater, then go left."
 * ============================================================================
 */


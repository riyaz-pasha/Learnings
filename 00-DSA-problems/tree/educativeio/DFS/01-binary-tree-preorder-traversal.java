import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * ============================================================================
 * STUDY NOTE: BINARY TREE PREORDER TRAVERSAL
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "What should the method return if the root is null?" 
 *   (Prevents NullPointerException and establishes the base empty state: an empty list, not null.)
 * - "Is it guaranteed that the tree contains no cycles?" 
 *   (Validates we are working with a strict tree, meaning standard traversal won't infinitely loop.)
 * - "Are we allowed to modify the tree's structure temporarily?" 
 *   (Determines if a Morris Traversal is an acceptable approach, as it modifies pointers.)
 * - "Is thread-safety or concurrent traversal a concern here?" 
 *   (If yes, pointer-modifying algorithms like Morris are automatically disqualified.)
 * - "What is the maximum depth of the tree?" 
 *   (Determines if the recursive approach is safe. Given max 100 nodes, recursion is perfectly safe, but asking shows production awareness regarding StackOverflowErrors).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * A binary tree's structure only provides pointers going downward (to children). Once we travel 
 * down a left branch, we inherently lose access to the right branch unless we "remember" it. 
 * The binding constraint of tree traversal is how we pay memory to remember these deferred branches.
 *
 * Approach 1: Recursive (Natural Intuition)
 * What I'd naturally try: Preorder means "Me first, then Left, then Right". I can process the 
 * current node, then recursively call the same function on the left child, then the right child. 
 * Why it works: The operating system's call stack handles the "remembering" for us. When the left 
 * subtree finishes, the call stack unwinds and picks up exactly where it left off to execute the right subtree.
 * Why it's costly: In production, deep trees crash threads.
 * - Time Complexity: O(N) — because every node is visited exactly once to append its value.
 * - Space Complexity: O(N) — because in the worst case (a one-sided skewed tree), the recursion 
 *   stack will hold N frames simultaneously before unwinding.
 * 
 * Approach 2: Iterative with Stack (Intermediate)
 * What I'd naturally try: To avoid a `StackOverflowError`, I need to take memory management away 
 * from the OS and handle it locally. I can use an explicit stack data structure.
 * Why it works: A stack is Last-In-First-Out (LIFO). To process Left before Right, I must push the 
 * Right child onto the stack FIRST, then the Left child. When I pop, Left comes out first.
 * What property removes the bottleneck: The explicit stack guarantees our application won't crash 
 * the JVM thread. It also allows us to optimize: we only push non-null children.
 * - Time Complexity: O(N) — because we push and pop every node exactly once from our explicit stack.
 * - Space Complexity: O(N) — because if the tree is an unbalanced zigzag (every node on the left spine 
 *   has a right child), we continuously defer right children. The stack grows to size O(Depth), bounded by N.
 *
 * Approach 3: Pointer Threading / Morris Traversal (Optimal Space)
 * What I'd naturally try: Look at the memory we are wasting. In any binary tree, exactly half of 
 * the pointers are `null` (the leaf nodes). What if we repurpose those empty pointers?
 * Why it works: Before we descend into a left subtree, we find the "predecessor" (the rightmost node 
 * of that left subtree). Its right pointer is always null. We can draw a temporary line from that leaf 
 * back up to the current node. This tells us how to get back! When we traverse the tree and hit 
 * this custom link, we know we've finished the left subtree, so we erase the link and move right.
 * What property removes the bottleneck: We eliminate the need for *any* stack by mapping our return 
 * path directly into the data structure itself.
 * - Time Complexity: O(N) — because finding predecessors traverses some edges twice, but asymptotically 
 *   each edge is visited at most 2 times. 2N is still linear.
 * - Space Complexity: O(1) — because we only allocate a couple of pointer variables, reusing the 
 *   tree's existing nodes to store traversal state, eliminating the stack entirely.
 *
 * The Interview Choice:
 * In a standard 45-minute interview, I would write the Iterative Stack approach. Recursion is too 
 * trivial (often a red flag if it's the *only* thing you can write), and Morris traversal is highly 
 * error-prone under time pressure and mutates state. Iterative proves strong fundamentals and memory control.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Empty Tree (root == null) -> Must return empty list, not crash.
 * 2. Single Node -> Should process and return without pushing nulls.
 * 3. Left-Skewed Tree -> Tests maximum stack depth.
 * 4. Right-Skewed Tree -> Tests stack pop-empty logic (stack stays extremely shallow).
 *
 *
 * 4. DRY RUN (Iterative Stack)
 * ----------------------------------------------------------------------------
 * Tree:
 *       1
 *      / \
 *     2   3
 *    / \
 *   4   5
 *
 * State Tracker (res = result list, stack = right side is TOP):
 * Step 1: Pop 1. res=[1]. Push Right(3), Push Left(2).       -> stack=[3, 2]
 * Step 2: Pop 2. res=[1, 2]. Push Right(5), Push Left(4).    -> stack=[3, 5, 4]
 * Step 3: Pop 4. res=[1, 2, 4]. No children.                 -> stack=[3, 5]
 * Step 4: Pop 5. res=[1, 2, 4, 5]. No children.              -> stack=[3]
 * Step 5: Pop 3. res=[1, 2, 4, 5, 3]. No children.           -> stack=[]
 * Done.
 *
 * Pitfalls: 
 * - Pushing Left then Right onto the stack. Since it's LIFO, this results in Node-Right-Left order.
 * - Pushing nulls. This forces you to add a `if (node == null) continue;` at the top of the loop. 
 *   It's cleaner to just conditionally push.
 *
 * Pattern Recognition: 
 * "When you need to simulate recursion safely -> Think explicit Stack (LIFO)." 
 * This translates directly to DFS (Depth First Search) in graphs and backtracking problems.
 *
 * Interview Script:
 * "Since Preorder is Node-Left-Right, recursion is trivial, but to make this production-ready and 
 * immune to StackOverflow, I'll handle memory explicitly with a Stack. Because a Stack is LIFO, 
 * to visit the left child first, I need to push the right child onto the stack *before* the left. 
 * This gives us O(N) time and O(N) space bounded by the tree's depth."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if the tree is millions of nodes, but the caller only wants to process one node at a time?
 * A1: Instead of building a `List<Integer>`, I would wrap the iterative stack inside a custom `Iterator<Integer>`. 
 *     The `hasNext()` checks if the stack is empty. `next()` pops the stack, pushes right then left, 
 *     and returns the popped value. This avoids holding O(N) integers in memory at once.
 *
 * Q2: How does this change for an N-ary tree?
 * A2: Instead of pushing `right` then `left`, I would iterate through the node's children array backwards, 
 *     pushing from `children.length - 1` down to `0`.
 */

public class PreorderTraversalStudy {

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
     * APPROACH 2: ITERATIVE WITH STACK (The recommended interview solution)
     */
    public List<Integer> preorderTraversalIterative(TreeNode root) {
        // e.g. empty tree returns [] immediately instead of crashing
        List<Integer> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        // Deque is the modern, thread-unsynchronized replacement for java.util.Stack.
        // e.g. if we are at root(1)->(2,3), stack will temporarily hold [3, 2]
        Deque<TreeNode> stack = new ArrayDeque<>();
        
        // Start the engine
        stack.push(root);

        while (!stack.isEmpty()) {
            // Process the current node (The "Node" in Node-Left-Right)
            TreeNode current = stack.pop();
            result.add(current.data);

            // Why push right FIRST?
            // Because stack is LIFO. We want left to be popped off first on the next iteration.
            // e.g. push(3) then push(2) -> stack=[3, 2] -> pop() gets 2.
            if (current.right != null) {
                stack.push(current.right);
            }
            if (current.left != null) {
                stack.push(current.left);
            }
        }

        return result;
    }

    /**
     * APPROACH 1: RECURSIVE (For completeness)
     */
    public List<Integer> preorderTraversalRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        traverse(root, result);
        return result;
    }

    private void traverse(TreeNode node, List<Integer> result) {
        if (node == null) return;
        
        result.add(node.data);    // Node
        traverse(node.left, result);  // Left
        traverse(node.right, result); // Right
    }

    /**
     * APPROACH 3: MORRIS TRAVERSAL (The O(1) space flex)
     */
    public List<Integer> preorderTraversalMorris(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        
        // e.g. acts as our runner pointer tracing through the tree
        TreeNode current = root;

        while (current != null) {
            if (current.left == null) {
                // No left side to explore. Process current and move right.
                result.add(current.data);
                current = current.right;
            } else {
                // We have a left side. Find the rightmost node of this left subtree.
                // e.g. if current is 1, and left subtree is 2->(null, 5), predecessor is 5.
                TreeNode predecessor = current.left;
                while (predecessor.right != null && predecessor.right != current) {
                    predecessor = predecessor.right;
                }

                if (predecessor.right == null) {
                    // 1st time visiting: Thread the predecessor back to current.
                    // Because it's Preorder, we process the node BEFORE moving left.
                    result.add(current.data);
                    predecessor.right = current;
                    current = current.left;
                } else {
                    // 2nd time visiting: We found our own thread. Left subtree is fully processed.
                    // Erase the thread to restore the tree, and move right.
                    predecessor.right = null;
                    current = current.right;
                }
            }
        }
        return result;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        PreorderTraversalStudy study = new PreorderTraversalStudy();

        // Edge Case 1: Empty Tree
        System.out.println("Empty Tree: " + study.preorderTraversalIterative(null)); // Expected: []

        // Edge Case 2: Single Element
        System.out.println("Single Node: " + study.preorderTraversalIterative(new TreeNode(42))); // Expected: [42]

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
        System.out.println("Standard Iterative: " + study.preorderTraversalIterative(standard)); // [1, 2, 4, 5, 3]
        System.out.println("Standard Morris:    " + study.preorderTraversalMorris(standard));    // [1, 2, 4, 5, 3]

        // Edge Case 3: Right-skewed (Tests stack behavior)
        // 1 -> 2 -> 3
        TreeNode skewed = new TreeNode(1, null, new TreeNode(2, null, new TreeNode(3)));
        System.out.println("Right Skewed: " + study.preorderTraversalIterative(skewed)); // Expected: [1, 2, 3]
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Simulated Recursion (Stack) / Tree Threading (Morris).
 * Key Observation: You must save pointers to right branches before walking down left branches.
 * Memorize: For iterative preorder, push RIGHT child, then LEFT child. 
 * Most Common Trap: Pushing left first. A stack reverses order, so left must go in last to come out first.
 * One-Line Mental Trigger: "Preorder Iterative = Pop, Add, Push Right, Push Left."
 * ============================================================================
 */


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * ============================================================================
 * BINARY TREE - PREORDER TRAVERSAL
 * ============================================================================
 *
 * Preorder traversal visits nodes in this order:
 *
 *      1. Node
 *      2. Left subtree
 *      3. Right subtree
 *
 *                 1
 *                / \
 *               2   3
 *              / \
 *             4   5
 *
 * Preorder:
 *
 *      1 -> 2 -> 4 -> 5 -> 3
 *
 * ============================================================================
 * INTERVIEW THINKING
 * ============================================================================
 *
 * Before coding, clarify:
 *
 * 1. What should happen if root == null?
 *    -> Return an empty list.
 *
 * 2. Are we allowed to modify the tree?
 *    -> If no, Morris traversal may not be appropriate.
 *
 * 3. Can the tree be extremely deep?
 *    -> If yes, recursion can cause StackOverflowError.
 *
 * 4. What is the expected complexity?
 *    -> Every node must be visited, so O(N) time is unavoidable.
 *
 * ============================================================================
 *
 * IMPORTANT IDEA
 * ============================================================================
 *
 * The interesting part of iterative traversal is:
 *
 *      "How do I remember where I need to go next?"
 *
 * Recursion solves this automatically using the call stack.
 *
 * Iterative traversal solves it explicitly using our own stack.
 *
 * Morris traversal solves it by temporarily modifying pointers in the tree.
 *
 * ============================================================================
 */
public class PreorderTraversalStudy {

    /**
     * Basic binary tree node.
     */
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

    // ========================================================================
    // APPROACH 1: RECURSION
    // ========================================================================

    /**
     * Preorder traversal using recursion.
     *
     * PREORDER = Node -> Left -> Right
     *
     * Example:
     *
     *              1
     *             / \
     *            2   3
     *           / \
     *          4   5
     *
     * Execution:
     *
     * visit(1)
     *     -> add 1
     *     -> visit(2)
     *          -> add 2
     *          -> visit(4)
     *               -> add 4
     *          -> visit(5)
     *               -> add 5
     *     -> visit(3)
     *          -> add 3
     *
     * Result:
     *      [1, 2, 4, 5, 3]
     *
     * Time:
     *      O(N)
     *
     * Space:
     *      O(H)
     *
     * where H = height of the tree.
     *
     * Best case:
     *      Balanced tree -> O(log N)
     *
     * Worst case:
     *      Skewed tree -> O(N)
     *
     * IMPORTANT:
     *
     * The O(H) space is not because of our result list.
     * It is because of the recursion call stack.
     */
    public List<Integer> preorderRecursive(TreeNode root) {

        List<Integer> traversalResult = new ArrayList<>();

        collectPreorder(root, traversalResult);

        return traversalResult;
    }

    /**
     * Recursive helper.
     *
     * The order of these three statements IS the preorder traversal:
     *
     *      1. Process current node
     *      2. Process left subtree
     *      3. Process right subtree
     */
    private void collectPreorder(
            TreeNode currentNode,
            List<Integer> traversalResult) {

        // Base case:
        // There is nothing to process in an empty subtree.
        if (currentNode == null) {
            return;
        }

        // 1. NODE
        traversalResult.add(currentNode.value);

        // 2. LEFT
        collectPreorder(currentNode.left, traversalResult);

        // 3. RIGHT
        collectPreorder(currentNode.right, traversalResult);
    }

    // ========================================================================
    // APPROACH 2: ITERATIVE USING STACK
    // ========================================================================

    /**
     * Preorder traversal without recursion.
     *
     * This is usually the safest interview solution.
     *
     * Why?
     *
     * Recursion:
     *
     *      JVM manages the call stack for us.
     *
     * Iterative:
     *
     *      We manage the stack ourselves.
     *
     * This avoids StackOverflowError for extremely deep trees.
     *
     * ------------------------------------------------------------------------
     *
     * THE KEY TRICK
     * ------------------------------------------------------------------------
     *
     * Preorder requires:
     *
     *      Node -> Left -> Right
     *
     * But our stack is LIFO:
     *
     *      Last In -> First Out
     *
     * Therefore, if we want:
     *
     *      Left
     *      Right
     *
     * to come out in that order, we must push:
     *
     *      Right FIRST
     *      Left SECOND
     *
     * Example:
     *
     *      stack.push(Right);
     *      stack.push(Left);
     *
     * Stack:
     *
     *      [Right, Left]
     *
     * pop()
     *      -> Left
     *
     * Exactly what we want.
     *
     * ------------------------------------------------------------------------
     *
     * Time:  O(N)
     * Space: O(H)
     *
     * H = height of the tree.
     *
     * In the worst case H = N.
     */
    public List<Integer> preorderIterative(TreeNode root) {

        List<Integer> traversalResult = new ArrayList<>();

        // Empty tree:
        // There is nothing to traverse.
        if (root == null) {
            return traversalResult;
        }

        /*
         * Deque is preferred over the old java.util.Stack class.
         *
         * We use it as a stack:
         *
         *      push() -> add to top
         *      pop()  -> remove from top
         */
        Deque<TreeNode> nodeStack = new ArrayDeque<>();

        // The root is the first node we need to process.
        nodeStack.push(root);

        while (!nodeStack.isEmpty()) {

            /*
             * Remove the next node to process.
             *
             * Because this is a stack, the most recently pushed node
             * is processed first.
             */
            TreeNode currentNode = nodeStack.pop();

            // --------------------------------------------------------------
            // 1. NODE
            // --------------------------------------------------------------
            //
            // Preorder means we process the current node immediately.
            //
            traversalResult.add(currentNode.value);

            // --------------------------------------------------------------
            // 2. RIGHT
            // --------------------------------------------------------------
            //
            // Push RIGHT first.
            //
            // Why?
            //
            // Stack = LIFO.
            //
            // We want LEFT to come out before RIGHT.
            //
            // So:
            //
            //      push(Right)
            //      push(Left)
            //
            // results in:
            //
            //      pop() -> Left
            //
            if (currentNode.right != null) {
                nodeStack.push(currentNode.right);
            }

            // --------------------------------------------------------------
            // 3. LEFT
            // --------------------------------------------------------------
            //
            // Push LEFT last so that it is popped first.
            //
            if (currentNode.left != null) {
                nodeStack.push(currentNode.left);
            }
        }

        return traversalResult;
    }

    // ========================================================================
    // APPROACH 3: MORRIS TRAVERSAL
    // ========================================================================

    /**
     * Preorder traversal using Morris Traversal.
     *
     * Goal:
     *
     *      O(N) time
     *      O(1) extra space
     *
     * No recursion.
     * No explicit stack.
     *
     * ------------------------------------------------------------------------
     *
     * THE PROBLEM
     * ------------------------------------------------------------------------
     *
     * In recursive traversal, after going:
     *
     *              1
     *             /
     *            2
     *
     * we need to remember:
     *
     *      "After finishing node 2's subtree,
     *       I need to come back to node 1."
     *
     * Normally recursion/stack stores that information.
     *
     * Morris traversal temporarily stores that information INSIDE
     * the tree itself.
     *
     * ------------------------------------------------------------------------
     *
     * THE IDEA
     * ------------------------------------------------------------------------
     *
     * Suppose we are at:
     *
     *              1
     *             /
     *            2
     *             \
     *              5
     *
     * Node 5 is the rightmost node of node 1's left subtree.
     *
     * Normally:
     *
     *      5.right == null
     *
     * We temporarily change it to:
     *
     *      5.right = 1
     *
     * This creates a temporary "thread" back to node 1.
     *
     * After finishing the left subtree, we encounter this thread,
     * remove it, and continue to the right subtree.
     *
     * ------------------------------------------------------------------------
     *
     * Time:  O(N)
     * Space: O(1)
     *
     * IMPORTANT:
     *
     * Morris temporarily modifies the tree.
     *
     * It restores the original structure before returning.
     */
    public List<Integer> preorderMorris(TreeNode root) {

        List<Integer> traversalResult = new ArrayList<>();

        TreeNode currentNode = root;

        while (currentNode != null) {

            // ================================================================
            // CASE 1:
            // Current node has NO left subtree.
            // ================================================================

            if (currentNode.left == null) {

                /*
                 * There is no left subtree to process.
                 *
                 * Therefore, in preorder we can process this node now
                 * and move directly to the right.
                 */
                traversalResult.add(currentNode.value);

                currentNode = currentNode.right;

                continue;
            }

            // ================================================================
            // CASE 2:
            // Current node HAS a left subtree.
            // ================================================================

            /*
             * Find the rightmost node in the left subtree.
             *
             * This node is called the INORDER PREDECESSOR of currentNode.
             *
             * Example:
             *
             *              1
             *             /
             *            2
             *             \
             *              5
             *
             * The predecessor of 1 is 5.
             */
            TreeNode predecessor = currentNode.left;

            /*
             * Keep moving right until:
             *
             * 1. We reach the rightmost node, OR
             *
             * 2. We discover that a temporary thread already exists.
             *
             * The second condition is important because:
             *
             *      predecessor.right == currentNode
             *
             * means we have already processed the left subtree and
             * returned to currentNode.
             */
            while (predecessor.right != null
                    && predecessor.right != currentNode) {

                predecessor = predecessor.right;
            }

            // ================================================================
            // CASE 2A:
            // First time reaching currentNode.
            // ================================================================

            if (predecessor.right == null) {

                /*
                 * We are visiting currentNode for the FIRST time.
                 *
                 * Since this is PREORDER:
                 *
                 *      Node -> Left -> Right
                 *
                 * we process currentNode BEFORE going into the left subtree.
                 */
                traversalResult.add(currentNode.value);

                /*
                 * Create a temporary thread:
                 *
                 *      predecessor -> currentNode
                 *
                 * This gives us a way to return after completing
                 * the left subtree.
                 */
                predecessor.right = currentNode;

                /*
                 * Now explore the left subtree.
                 */
                currentNode = currentNode.left;

            } else {

                // ============================================================
                // CASE 2B:
                // Second time reaching currentNode.
                // ============================================================

                /*
                 * If we reach here:
                 *
                 *      predecessor.right == currentNode
                 *
                 * Therefore, we have finished traversing the left subtree.
                 *
                 * Remove the temporary thread.
                 */
                predecessor.right = null;

                /*
                 * Left subtree is done.
                 *
                 * Preorder now moves to:
                 *
                 *      Right subtree
                 */
                currentNode = currentNode.right;
            }
        }

        return traversalResult;
    }

    // ========================================================================
    // DRY RUN
    // ========================================================================

    /**
     * Tree used in the examples:
     *
     *              1
     *             / \
     *            2   3
     *           / \
     *          4   5
     *
     * Preorder:
     *
     *      1 -> 2 -> 4 -> 5 -> 3
     */
    private static TreeNode createExampleTree() {

        TreeNode node4 = new TreeNode(4);
        TreeNode node5 = new TreeNode(5);

        TreeNode node2 = new TreeNode(2, node4, node5);

        TreeNode node3 = new TreeNode(3);

        return new TreeNode(1, node2, node3);
    }

    // ========================================================================
    // MAIN
    // ========================================================================

    public static void main(String[] args) {

        PreorderTraversalStudy solution =
                new PreorderTraversalStudy();

        TreeNode exampleTree = createExampleTree();

        System.out.println(
                "Recursive : "
                        + solution.preorderRecursive(exampleTree)
        );

        System.out.println(
                "Iterative : "
                        + solution.preorderIterative(exampleTree)
        );

        System.out.println(
                "Morris    : "
                        + solution.preorderMorris(exampleTree)
        );

        // ------------------------------------------------------------
        // Edge Case 1: Empty tree
        // ------------------------------------------------------------

        System.out.println(
                "Empty tree: "
                        + solution.preorderIterative(null)
        );

        // Expected:
        // []

        // ------------------------------------------------------------
        // Edge Case 2: Single node
        // ------------------------------------------------------------

        TreeNode singleNode = new TreeNode(42);

        System.out.println(
                "Single node: "
                        + solution.preorderIterative(singleNode)
        );

        // Expected:
        // [42]

        // ------------------------------------------------------------
        // Edge Case 3: Left-skewed tree
        // ------------------------------------------------------------

        /*
         *      1
         *     /
         *    2
         *   /
         *  3
         *
         * Preorder:
         *
         *      [1, 2, 3]
         */
        TreeNode leftSkewedTree =
                new TreeNode(
                        1,
                        new TreeNode(
                                2,
                                new TreeNode(3),
                                null
                        ),
                        null
                );

        System.out.println(
                "Left skewed: "
                        + solution.preorderIterative(leftSkewedTree)
        );

        // ------------------------------------------------------------
        // Edge Case 4: Right-skewed tree
        // ------------------------------------------------------------

        /*
         *  1
         *   \
         *    2
         *     \
         *      3
         *
         * Preorder:
         *
         *      [1, 2, 3]
         */
        TreeNode rightSkewedTree =
                new TreeNode(
                        1,
                        null,
                        new TreeNode(
                                2,
                                null,
                                new TreeNode(3)
                        )
                );

        System.out.println(
                "Right skewed: "
                        + solution.preorderIterative(rightSkewedTree)
        );
    }
}

/**
 * ============================================================================
 * INTERVIEW CHEAT SHEET
 * ============================================================================
 *
 * PREORDER:
 *
 *      Node -> Left -> Right
 *
 *
 * RECURSIVE:
 *
 *      visit(node)
 *          add node
 *          visit(left)
 *          visit(right)
 *
 *      Time:  O(N)
 *      Space: O(H)
 *
 *
 * ITERATIVE:
 *
 *      stack.push(root)
 *
 *      while stack not empty:
 *          node = stack.pop()
 *          add node
 *          push RIGHT
 *          push LEFT
 *
 *      Why RIGHT first?
 *
 *      Stack = LIFO
 *
 *      push(Right)
 *      push(Left)
 *
 *      pop() -> Left
 *
 *
 * MORRIS:
 *
 *      No stack.
 *      No recursion.
 *
 *      Temporarily create:
 *
 *          predecessor.right = current
 *
 *      Use that temporary pointer to remember where to return.
 *
 *      Time:  O(N)
 *      Space: O(1)
 *
 *
 * ============================================================================
 * MOST IMPORTANT PATTERN
 * ============================================================================
 *
 * If recursion is:
 *
 *      process(node)
 *      recurse(left)
 *      recurse(right)
 *
 * then the iterative version usually needs a stack that remembers
 * the nodes that still need to be processed.
 *
 *
 * For PREORDER:
 *
 *      Pop
 *      Process
 *      Push Right
 *      Push Left
 *
 *
 * MEMORIZE:
 *
 *      "Preorder = Pop, Add, Right, Left"
 *
 * ============================================================================
 */

/*
================================================================================
MORRIS PREORDER TRAVERSAL (DETAILED VISUAL TRACE WITH THREADING & BACKTRACKING)
================================================================================

Goal:
------
Perform Preorder Traversal (Root → Left → Right) WITHOUT recursion and WITHOUT stack.
We achieve this by temporarily modifying the tree using "threads".

Key Idea:
----------
We create a temporary link (thread) from the predecessor node back to the current node.
This helps us return after finishing the left subtree.

--------------------------------------------------------------------------------
EXAMPLE TREE:
--------------------------------------------------------------------------------

            1
           / \
          2   3
         / \
        4   5

Expected Preorder Output:
------------------------
[1, 2, 4, 5, 3]

--------------------------------------------------------------------------------
DEFINITION:
--------------------------------------------------------------------------------
Predecessor = Rightmost node in the LEFT subtree

--------------------------------------------------------------------------------
ALGORITHM FLOW:
--------------------------------------------------------------------------------

For each node:
1. If left == null:
      → Visit node
      → Move to right

2. Else:
      → Find predecessor (rightmost node in left subtree)

      IF predecessor.right == null:
          → CREATE THREAD (predecessor.right = current)
          → Visit current (PREORDER → before going left)
          → Move to left

      ELSE (thread exists):
          → REMOVE THREAD (restore tree)
          → Move to right

--------------------------------------------------------------------------------
STEP-BY-STEP TRACE WITH ASCII DIAGRAMS
--------------------------------------------------------------------------------

-------------------------
STEP 1: current = 1
-------------------------

Tree:
            1
           / \
          2   3
         / \
        4   5

Left exists → Find predecessor of 1

Left subtree = rooted at 2
Rightmost node = 5

Thread Creation:
----------------
5.right → 1   (temporary link created)

Visual:
            1
           / \
          2   3
         / \
        4   5
             \
              1  <-- THREAD

Visit node (PREORDER):
result = [1]

Move:
current = 2


-------------------------
STEP 2: current = 2
-------------------------

Left exists → Find predecessor

Left subtree = rooted at 4
Rightmost node = 4

Thread Creation:
----------------
4.right → 2

Visual:
            1
           / \
          2   3
         / \
        4   5
         \
          2   <-- THREAD

Visit node:
result = [1, 2]

Move:
current = 4


-------------------------
STEP 3: current = 4
-------------------------

No left child

Visit node:
result = [1, 2, 4]

Move:
current = 4.right → 2 (via THREAD)


-------------------------
STEP 4: current = 2 (BACKTRACK)
-------------------------

We came via thread → detect:
predecessor.right == current (4.right == 2)

Thread Removal:
----------------
4.right = null  (restore original tree)

Visual:
            1
           / \
          2   3
         / \
        4   5

Move:
current = 2.right → 5


-------------------------
STEP 5: current = 5
-------------------------

No left child

Visit node:
result = [1, 2, 4, 5]

Move:
current = 5.right → 1 (via THREAD)


-------------------------
STEP 6: current = 1 (BACKTRACK)
-------------------------

Detect thread:
predecessor.right == current (5.right == 1)

Thread Removal:
----------------
5.right = null

Visual:
            1
           / \
          2   3
         / \
        4   5

Move:
current = 1.right → 3


-------------------------
STEP 7: current = 3
-------------------------

No left child

Visit node:
result = [1, 2, 4, 5, 3]

Move:
current = null

Traversal ends.

--------------------------------------------------------------------------------
FINAL RESULT:
--------------------------------------------------------------------------------

[1, 2, 4, 5, 3]

--------------------------------------------------------------------------------
IMPORTANT OBSERVATIONS (INTERVIEW GOLD)
--------------------------------------------------------------------------------

1. WHY THREADING?
   → To simulate recursion stack without extra space
   → It creates a "return path" back to the parent

2. WHY VISIT BEFORE GOING LEFT?
   → Because it's PREORDER (Root first)

3. HOW DO WE KNOW BACKTRACK?
   → If predecessor.right == current → thread already exists

4. TREE IS RESTORED:
   → Every thread we create is removed later
   → Final tree remains unchanged

--------------------------------------------------------------------------------
MENTAL MODEL (VERY IMPORTANT)
--------------------------------------------------------------------------------

Think:
"Whenever I go left, I leave a breadcrumb (thread) to come back."

Thread = "Come back to me later"

--------------------------------------------------------------------------------
COMPLEXITY
--------------------------------------------------------------------------------

Time Complexity: O(n)
  → Each node is visited at most 2 times

Space Complexity: O(1)
  → No recursion stack, no extra DS

--------------------------------------------------------------------------------
COMMON MISTAKE
--------------------------------------------------------------------------------

❌ Forgetting to remove thread → corrupts tree
❌ Visiting node in wrong place (Preorder vs Inorder confusion)

--------------------------------------------------------------------------------
PREORDER vs INORDER (QUICK DIFFERENCE)
--------------------------------------------------------------------------------

PREORDER:
    Visit BEFORE going left

INORDER:
    Visit AFTER coming back (when removing thread)

--------------------------------------------------------------------------------
END
--------------------------------------------------------------------------------
*/

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ============================================================================
 * STUDY NOTE: RECOVER A TREE FROM PREORDER TRAVERSAL
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Can node values be negative?" 
 *   (Crucial: If values can be negative, a minus sign `-` could be confused with a depth dash `-`. The constraints say 1 <= data <= 10^3, meaning all positive, so dashes are strictly for depth).
 * - "Can node values be multiple digits?" 
 *   (Yes. The string `1-2` is easy, but `10-200` means we must parse characters iteratively until we hit a non-digit).
 * - "If a node has only one child, is it always the left child?" 
 *   (The prompt guarantees this. This saves us from ambiguity, meaning the first time we attach a child to a node, it will ALWAYS go to the left).
 * - "Is the input string guaranteed to be a valid preorder traversal?"
 *   (Assumes we don't need extensive regex validation or error-throwing for malformed strings).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We are given a single flat string where a node's depth is encoded by the number of preceding dashes. 
 * Because it is Preorder (Node -> Left -> Right), the string inherently flows down a path to a leaf, 
 * and then "jumps" back up to a previous ancestor to start the right branch. The bottleneck is 
 * identifying *which* ancestor a new node belongs to when these jumps occur.
 *
 * Approach 1: Recursive DFS (Top-Down with Global Pointer)
 * What I'd naturally try: Preorder implies recursion. I can maintain a global index to read the string. 
 * At any recursive step (expecting a certain `depth`), I'll "peek" ahead to count the dashes. 
 * If the dash count matches my expected `depth`, I consume the dashes, parse the number, build the node, 
 * and then recursively call for `depth + 1` (left child) and `depth + 1` (right child).
 * Why it works: The call stack perfectly models the tree depth. If we expect depth 3, but only see 2 dashes, 
 * it means this branch is a dead-end (null), and the recursive frame naturally unwinds back to depth 2!
 * What work is being repeated: Nothing. We parse the string strictly left-to-right.
 * What property removes the bottleneck: The implicit call stack tracks our current path.
 * - Time Complexity: O(N) — where N is the length of the string. We look at each character at most twice (once to peek, once to consume).
 * - Space Complexity: O(H) — where H is the height of the tree (max depth). The recursion stack stores H frames.
 * 
 * Approach 2: Iterative Stack (The Production-Safe State Tracker)
 * What I'd naturally try: Instead of relying on the JVM call stack, I can manually maintain the current 
 * path from the root to the deepest node using an explicit Stack of `TreeNode` objects.
 * Why it works: When I parse a new node and its depth `D`, I know that a node at depth `D` MUST be a child 
 * of a node at depth `D - 1`. Since the Stack holds the current path, the depth of a node in the path is exactly 
 * its index in the Stack! (Root is size 1 (depth 0), child is size 2 (depth 1)). 
 * If the stack size is larger than `D`, I just pop nodes until `stack.size() == D`. The top of the stack 
 * is now the correct parent. Attach left if empty, otherwise attach right, then push the new node.
 * What property removes the bottleneck: The Stack dynamically maintains the active ancestral lineage, 
 * letting us handle massive upward "jumps" in O(1) amortized time by just popping elements.
 * - Time Complexity: O(N) — string is parsed left-to-right exactly once. Each node is pushed and popped at most once.
 * - Space Complexity: O(H) — the Stack never exceeds the maximum depth of the tree.
 *
 * The Interview Choice:
 * Write the Iterative Stack (Approach 2). String parsing mixed with recursion requires a global/instance 
 * index variable, which is messy and non-thread-safe. The iterative approach keeps all state beautifully 
 * contained within a single method and is immune to StackOverflow errors on extreme edge cases.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Single Node Tree -> "1". No dashes. Loop completes, returns the single node.
 * 2. Multi-Digit Values -> "100-200--300". The `while (Character.isDigit)` loop handles variable integer widths.
 * 3. Massive Ancestral Jump -> "1-2--3---4-5". Node 4 is depth 3. Node 5 is depth 1. The stack must 
 *    pop multiple times to safely return to Node 1 before attaching Node 5.
 *
 *
 * 4. DRY RUN (Iterative Stack Approach)
 * ----------------------------------------------------------------------------
 * Input String: "1-2--3--4-5"
 * 
 * Step 1: i = 0.
 *         Count dashes: 0. depth = 0.
 *         Parse num: "1". val = 1.
 *         Create Node(1). 
 *         Stack is empty. Push Node(1). Stack: [1].
 * 
 * Step 2: i = 1 (at first '-').
 *         Count dashes: "-". depth = 1.
 *         Parse num: "2". val = 2.
 *         Create Node(2).
 *         Stack size (1) == depth (1) -> No popping needed. Parent is 1.
 *         Parent(1).left is null -> 1.left = Node(2).
 *         Push Node(2). Stack: [1, 2].
 * 
 * Step 3: i = 3 (at '--').
 *         Count dashes: "--". depth = 2.
 *         Parse num: "3". val = 3.
 *         Create Node(3).
 *         Stack size (2) == depth (2) -> No popping needed. Parent is 2.
 *         Parent(2).left is null -> 2.left = Node(3).
 *         Push Node(3). Stack: [1, 2, 3].
 * 
 * Step 4: i = 6 (at '--').
 *         Count dashes: "--". depth = 2.
 *         Parse num: "4". val = 4.
 *         Create Node(4).
 *         Wait, Stack size is 3! But depth is 2.
 *         We must POP until Stack size == 2. Pop Node(3). Stack: [1, 2].
 *         Parent is 2.
 *         Parent(2).left is NOT null (it's 3). -> 2.right = Node(4).
 *         Push Node(4). Stack: [1, 2, 4].
 * 
 * Step 5: i = 9 (at '-').
 *         Count dashes: "-". depth = 1.
 *         Parse num: "5". val = 5.
 *         Create Node(5).
 *         Stack size is 3. Depth is 1.
 *         POP until Stack size == 1. Pop Node(4), Pop Node(2). Stack: [1].
 *         Parent is 1.
 *         Parent(1).left is NOT null (it's 2). -> 1.right = Node(5).
 *         Push Node(5). Stack: [1, 5].
 * 
 * String ends. Return stack bottom (Node 1).
 *
 * Pitfalls: 
 * - Using `Integer.parseInt(String.valueOf(char))` inside a loop without shifting. A multi-digit 
 *   number must be parsed as `val = val * 10 + (char - '0')` to efficiently accumulate the integer.
 * - Checking if a node is the right child via string lookahead. The problem explicitly states that a 
 *   single child is ALWAYS the left child. So we literally just check `if (parent.left == null) attachLeft else attachRight`.
 *
 * Pattern Recognition: 
 * "When reconstructing a hierarchical structure from a flat string with depth indicators -> Use a Stack. 
 *  Pop from the stack until the stack size represents the current depth's parent."
 *
 * Interview Script:
 * "Parsing string structures dynamically is best handled with an iterative stack. A node's depth corresponds 
 * exactly to its ancestral lineage. I'll traverse the string left-to-right, counting dashes to get the depth 
 * and parsing the digits to get the value. I'll maintain a stack of active nodes. If I parse a node at depth D, 
 * I simply pop my stack until its size is exactly D, exposing the correct parent at the top. Since single 
 * children are guaranteed to be on the left, I attach to the left if empty, otherwise right. This parses 
 * the tree in O(N) time and O(H) space."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if negative numbers were allowed, so `-` could mean depth OR a negative sign?
 * A1: The parsing logic would need to peek at the character *after* a sequence of dashes. If we hit `D` 
 *     dashes, and the first digit character is actually another `-` followed by a number, we'd have to treat 
 *     the last dash as part of the integer. We'd track whether the last dash was preceded by a dash or a number.
 *
 * Q2: How does the algorithm change if the traversal was Postorder instead of Preorder?
 * A2: Postorder is (Left -> Right -> Node). This breaks the stack paradigm because we read children before 
 *     we know who their parent is. Depth dashes wouldn't help us easily attach nodes as we read them. 
 *     We'd likely have to parse backwards from the end of the string, treating it effectively as a reversed Preorder.
 */

public class RecoverTreeFromPreorderStudy {

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
        
        // Helper to visualize in main
        @Override
        public String toString() {
            return "Node(" + val + ")";
        }
    }

    /**
     * APPROACH 2: ITERATIVE STACK (The Professional, Thread-Safe Choice)
     * Time: O(N) | Space: O(H)
     */
    public TreeNode recoverFromPreorderIterative(String traversal) {
        // Deque acts as our active path stack
        Deque<TreeNode> stack = new ArrayDeque<>();
        int i = 0;
        int n = traversal.length();

        while (i < n) {
            // 1. Calculate the Depth
            int depth = 0;
            while (i < n && traversal.charAt(i) == '-') {
                depth++;
                i++;
            }

            // 2. Parse the Multi-Digit Number
            int val = 0;
            while (i < n && Character.isDigit(traversal.charAt(i))) {
                // Efficient base-10 accumulation without String allocations
                val = val * 10 + (traversal.charAt(i) - '0');
                i++;
            }

            TreeNode node = new TreeNode(val);

            // 3. Reconcile the Stack
            // A node at depth D MUST be the child of a node at depth D-1.
            // If our stack is deeper than D, it means we've finished a branch and are 
            // jumping back up the tree. Pop until the stack size equals D.
            while (stack.size() > depth) {
                stack.pop();
            }

            // 4. Attach the Node
            if (!stack.isEmpty()) {
                TreeNode parent = stack.peek();
                // The prompt guarantees that if a node has one child, it's the left child.
                // So we always prioritize filling the left slot first.
                if (parent.left == null) {
                    parent.left = node;
                } else {
                    parent.right = node;
                }
            }

            // 5. Push the new node as it is now part of the active path
            stack.push(node);
        }

        // To return the root, we must pop all the way to the bottom of the stack
        while (stack.size() > 1) {
            stack.pop();
        }

        return stack.isEmpty() ? null : stack.peek();
    }

    /**
     * APPROACH 1: RECURSIVE DFS (The Elegant but State-Heavy Alternative)
     * Time: O(N) | Space: O(H)
     * Included to demonstrate top-down implicit stack tracking.
     */
    private int globalIndex = 0;

    public TreeNode recoverFromPreorderRecursive(String traversal) {
        globalIndex = 0; // Reset for repeated calls
        return dfs(traversal, 0);
    }

    private TreeNode dfs(String traversal, int expectedDepth) {
        // Count dashes without consuming them yet (peeking)
        int numDashes = 0;
        int tempIndex = globalIndex;
        
        while (tempIndex < traversal.length() && traversal.charAt(tempIndex) == '-') {
            numDashes++;
            tempIndex++;
        }

        // If the dashes don't match our expected depth, this branch is empty (null)
        // We do NOT update globalIndex, effectively "pushing" the dashes back for the caller.
        if (numDashes != expectedDepth) {
            return null;
        }

        // We confirmed this is our node. Consume the dashes.
        globalIndex = tempIndex;

        // Parse the value
        int val = 0;
        while (globalIndex < traversal.length() && Character.isDigit(traversal.charAt(globalIndex))) {
            val = val * 10 + (traversal.charAt(globalIndex) - '0');
            globalIndex++;
        }

        TreeNode node = new TreeNode(val);

        // Preorder recursion
        node.left = dfs(traversal, expectedDepth + 1);
        node.right = dfs(traversal, expectedDepth + 1);

        return node;
    }

    /**
     * Helper method to verify the tree structure matches expectations
     */
    private static void printPreorderDetailed(TreeNode node, int depth) {
        if (node == null) return;
        System.out.println("-".repeat(depth) + node.val);
        printPreorderDetailed(node.left, depth + 1);
        printPreorderDetailed(node.right, depth + 1);
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        RecoverTreeFromPreorderStudy study = new RecoverTreeFromPreorderStudy();

        System.out.println("--- STANDARD ZIG-ZAG CASE ---");
        // "1-2--3--4-5--6--7"
        // Expected tree:
        //        1
        //       / \
        //      2   5
        //     / \ / \
        //    3  4 6  7
        String s1 = "1-2--3--4-5--6--7";
        TreeNode root1 = study.recoverFromPreorderIterative(s1);
        printPreorderDetailed(root1, 0);


        System.out.println("\n--- MULTI-DIGIT CASE ---");
        // "10-200--3000"
        // Expected tree:
        //       10
        //      /
        //    200
        //    /
        // 3000
        String s2 = "10-200--3000";
        TreeNode root2 = study.recoverFromPreorderIterative(s2);
        printPreorderDetailed(root2, 0);


        System.out.println("\n--- ASYMMETRIC DEEP JUMP CASE ---");
        // "1-401--349---90--88"
        // After depth 3 (90), it jumps back to depth 2 (88). The stack must pop accurately.
        //        1
        //       /
        //     401
        //     / \
        //   349  88
        //   /
        //  90
        String s3 = "1-401--349---90--88";
        TreeNode root3 = study.recoverFromPreorderRecursive(s3); // Testing recursive approach here
        printPreorderDetailed(root3, 0);


        System.out.println("\n--- SINGLE NODE CASE ---");
        String s4 = "42";
        TreeNode root4 = study.recoverFromPreorderIterative(s4);
        printPreorderDetailed(root4, 0);
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Active Path Iterative Stack.
 * Key Observation: In a preorder string, the number of dashes is exactly the depth 
 *                  of the node. If you maintain a stack of active ancestors, the 
 *                  correct parent for a node at depth D is exposed when you pop 
 *                  everything until the stack size equals D.
 * Memorize: `while (stack.size() > depth) stack.pop(); if (parent.left == null) left = node; else right = node; stack.push(node);`
 * Most Common Trap: Trying to split the string by dashes. Since dashes occur in 
 *                   variable blocks ("-", "--", "---"), `String.split()` destroys 
 *                   the depth context or creates messy empty array elements. 
 *                   Sequential char-by-char parsing is infinitely cleaner.
 * One-Line Mental Trigger: "Recover Preorder Tree? Parse dashes for depth, pop stack to depth, attach, push."
 * ============================================================================
 */

import java.util.*;
import ds_v1.BinaryTree.TreeNode;

public class RecoverTreeFromPreorder {

    /**
     * Main function to recover tree
     */
    public TreeNode<Integer> recoverFromPreorder(String traversal) {

        int i = 0;
        int n = traversal.length();

        // Stack to maintain path from root to current node
        Deque<TreeNode<Integer>> stack = new ArrayDeque<>();

        while (i < n) {

            // -------------------------------
            // 1. Calculate depth (count dashes)
            // -------------------------------
            int depth = 0;
            while (i < n && traversal.charAt(i) == '-') {
                depth++;
                i++;
            }

            // -------------------------------
            // 2. Extract node value
            // -------------------------------
            int value = 0;
            while (i < n && Character.isDigit(traversal.charAt(i))) {
                value = value * 10 + (traversal.charAt(i) - '0');
                i++;
            }

            TreeNode<Integer> node = new TreeNode<>(value);

            // -------------------------------
            // 3. Fix stack to match depth
            // -------------------------------
            // Stack size should be equal to depth
            while (stack.size() > depth) {
                stack.pop();
            }

            // -------------------------------
            // 4. Attach node to parent
            // -------------------------------
            if (!stack.isEmpty()) {
                TreeNode<Integer> parent = stack.peek();

                // If left is empty → attach left
                if (parent.left == null) {
                    parent.left = node;
                } else {
                    // otherwise attach right
                    parent.right = node;
                }
            }

            // -------------------------------
            // 5. Push current node
            // -------------------------------
            stack.push(node);
        }

        // -------------------------------
        // 6. Bottom-most node is root
        // -------------------------------
        while (stack.size() > 1) {
            stack.pop();
        }

        return stack.peek();
    }
}

/**
 * ============================================================================
 * BINARY TREE PATHS - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We are standing at the top (root) of a branching tree. We need to walk down 
 * every possible trail until we reach a dead end (a leaf node, which has no 
 * further branches). For every complete trail we find, we need to record the 
 * journey by writing down the values of the nodes we visited, separated by 
 * arrows (e.g., "1->2->5"). We need to return a list of all these recorded trails.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: What if the tree has only one node (just the root)?
 * A: We should return a single string with just that node's value (e.g., ["1"]).
 * 
 * Q: What format exactly should the arrow take? 
 * A: The problem specifies an arrow, typically represented as "->" in strings 
 *    to avoid unicode encoding issues across different systems.
 * 
 * Q: Are node values guaranteed to be positive?
 * A: No, the constraints explicitly state -10^4 <= node.data <= 10^4, so we 
 *    must handle negative numbers (e.g., "-10->5").
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - A path is only complete when we hit a LEAF node (left AND right are null).
 * - A null child itself is not a leaf; it's just an empty branch. We only record 
 *   the path when the current node is a valid node without any children.
 * - As we traverse down the tree, we need to carry the "history" of our path 
 *   with us. 
 * - When using recursion, String concatenation (path + "->" + node.val) is 
 *   easy to write but creates many temporary String objects. Using a 
 *   StringBuilder with "backtracking" is the optimal way for memory.
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Clarify what defines a leaf node (both children are null).
 * - Step 2: Propose the standard Recursive DFS approach first, as tree problems 
 *   naturally align with recursion. 
 * - Step 3: Discuss the inefficiency of String concatenation inside loops/recursion.
 *   Offer to write an optimized DFS using StringBuilder and Backtracking.
 * - Step 4: Mention that Iterative BFS (Level Order) or Iterative DFS are also 
 *   valid using modern data structures if we want to avoid the JVM call stack.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Tree:
 *        1
 *      /   \
 *     2     3
 *      \
 *       5
 * 
 * Traversal Steps:
 * 1. Start at 1. Path: "1"
 * 2. Go left to 2. Path: "1->2"
 * 3. 2 has no left, go right to 5. Path: "1->2->5". 
 *    Node 5 is a leaf! ADD to result.
 * 4. Backtrack to 1. Go right to 3. Path: "1->3".
 *    Node 3 is a leaf! ADD to result.
 * 
 * Result: ["1->2->5", "1->3"]
 */

import java.util.*;

// Standard Binary Tree Node definition
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) { this.val = val; }
    
    TreeNode(int val, TreeNode left, TreeNode right) {
        this.val = val;
        this.left = left;
        this.right = right;
    }
}

class BinaryTreePaths {

    /**
     * SOLUTION 1: Optimized Recursive DFS with Backtracking (StringBuilder)
     * ------------------------------------------------------------------------
     * Pros: Highly optimal for memory and speed. Instead of creating a new 
     * String object at every step, we mutate a single StringBuilder and 
     * "undo" our changes when we backtrack up the tree.
     * Cons: Slightly more complex to write than simple String concatenation.
     * 
     * Time Complexity: O(N) to visit every node.
     * Space Complexity: O(H) for the call stack, where H is the tree height.
     */
    public List<String> binaryTreePathsDFS(TreeNode root) {
        List<String> result = new ArrayList<>();
        if (root == null) return result;
        
        dfs(root, new StringBuilder(), result);
        return result;
    }

    private void dfs(TreeNode node, StringBuilder currentPath, List<String> result) {
        // Record the length of the StringBuilder before we modify it.
        // This is the key to our backtracking!
        int originalLength = currentPath.length();
        
        // Append the current node's value
        currentPath.append(node.val);
        
        // Base Case: If it's a leaf node, add the completed path to our result
        if (node.left == null && node.right == null) {
            result.add(currentPath.toString());
        } else {
            // If not a leaf, append the arrow and continue DFS
            currentPath.append("->");
            if (node.left != null) dfs(node.left, currentPath, result);
            if (node.right != null) dfs(node.right, currentPath, result);
        }
        
        // Backtrack: Remove the current node's additions from the StringBuilder 
        // before returning to the parent caller.
        currentPath.setLength(originalLength);
    }

    /**
     * SOLUTION 2: Iterative Breadth-First Search (BFS) using Queue & Records
     * ------------------------------------------------------------------------
     * Pros: Avoids recursion. Explores the tree level by level.
     * Feature Highlight: We use Java 14+ `record` to cleanly pair the TreeNode 
     * with its corresponding string path up to that point. This removes the 
     * need for parallel queues or clumsy wrapper classes.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(N) for the maximum width of the tree stored in the queue.
     */
    public List<String> binaryTreePathsBFS(TreeNode root) {
        List<String> result = new ArrayList<>();
        if (root == null) return result;

        // Modern Java Feature: Record for clean, immutable data carriers
        record NodePathPair(TreeNode node, String path) {}

        Queue<NodePathPair> queue = new LinkedList<>();
        queue.offer(new NodePathPair(root, String.valueOf(root.val)));

        while (!queue.isEmpty()) {
            NodePathPair current = queue.poll();
            TreeNode node = current.node();
            String path = current.path();

            // If it's a leaf node, add to result
            if (node.left == null && node.right == null) {
                result.add(path);
            }

            // If left exists, append arrow and enqueue
            if (node.left != null) {
                queue.offer(new NodePathPair(node.left, path + "->" + node.left.val));
            }
            
            // If right exists, append arrow and enqueue
            if (node.right != null) {
                queue.offer(new NodePathPair(node.right, path + "->" + node.right.val));
            }
        }
        return result;
    }

    /**
     * SOLUTION 3: Iterative Depth-First Search (DFS) using Stack & Records
     * ------------------------------------------------------------------------
     * Pros: Simulates the recursion call stack but uses memory on the heap.
     * Useful if tree is extremely deep (skewed) and threatens StackOverflowError.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(H) worst-case stack depth (H = height of tree).
     */
    public List<String> binaryTreePathsIterativeDFS(TreeNode root) {
        List<String> result = new ArrayList<>();
        if (root == null) return result;

        record NodePathPair(TreeNode node, String path) {}
        
        // Deque is the modern Java replacement for Stack
        Deque<NodePathPair> stack = new ArrayDeque<>();
        stack.push(new NodePathPair(root, String.valueOf(root.val)));

        while (!stack.isEmpty()) {
            NodePathPair current = stack.pop();
            TreeNode node = current.node();
            String path = current.path();

            if (node.left == null && node.right == null) {
                result.add(path);
            }

            // Note on Stack DFS: To process left children first (like standard DFS),
            // we must push the right child onto the stack first.
            if (node.right != null) {
                stack.push(new NodePathPair(node.right, path + "->" + node.right.val));
            }
            if (node.left != null) {
                stack.push(new NodePathPair(node.left, path + "->" + node.left.val));
            }
        }
        return result;
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        BinaryTreePaths solver = new BinaryTreePaths();

        /*
         * Constructing the following tree:
         *        1
         *      /   \
         *     2     3
         *      \
         *       5
         */
        TreeNode root = new TreeNode(1,
            new TreeNode(2, null, new TreeNode(5)),
            new TreeNode(3)
        );

        System.out.println("--- Testing Solution 1: Recursive DFS ---");
        List<String> dfsResult = solver.binaryTreePathsDFS(root);
        dfsResult.forEach(System.out::println);

        System.out.println("\n--- Testing Solution 2: Iterative BFS ---");
        List<String> bfsResult = solver.binaryTreePathsBFS(root);
        bfsResult.forEach(System.out::println);

        System.out.println("\n--- Testing Solution 3: Iterative DFS ---");
        List<String> iterDfsResult = solver.binaryTreePathsIterativeDFS(root);
        iterDfsResult.forEach(System.out::println);
    }
}

/**
 * ============================================================
 * 🌳 Binary Tree Paths — DFS + Backtracking (MASTER SOLUTION)
 * ============================================================
 *
 * Idea:
 * - Use DFS to explore all root-to-leaf paths
 * - Build path string during traversal
 * - When leaf is reached → add path to result
 *
 * Why Backtracking?
 * - Because same path object is reused for multiple branches
 *
 * Time Complexity  : O(N)
 * Space Complexity : O(H) recursion stack + O(N) output
 *   where H = height of tree
 */
public class BinaryTreePaths {

    // Definition for binary tree node
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public static List<String> binaryTreePaths(TreeNode root) {
        List<String> result = new ArrayList<>();

        if (root == null) return result;

        // Start DFS
        dfs(root, new StringBuilder(), result);

        return result;
    }

    private static void dfs(TreeNode node, StringBuilder path, List<String> result) {
        if (node == null) return;

        int lengthBefore = path.length(); // ⭐ important for backtracking

        // Add current node to path
        if (path.length() != 0) {
            path.append("->");
        }
        path.append(node.val);

        // ✅ Leaf node → store result
        if (node.left == null && node.right == null) {
            result.add(path.toString());
        } else {
            // Explore children
            dfs(node.left, path, result);
            dfs(node.right, path, result);
        }

        // 🔥 BACKTRACK: restore previous state
        path.setLength(lengthBefore);
    }

    // ------------------ DRIVER ------------------
    public static void main(String[] args) {
        /*
                 1
                / \
               2   3
                \
                 5

        Expected Output:
        ["1->2->5", "1->3"]
        */

        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(3);
        root.left.right = new TreeNode(5);

        System.out.println(binaryTreePaths(root));
    }
}

/**
 * ============================================================
 * 🌳 Binary Tree Paths — DFS + Backtracking using LIST
 * ============================================================
 *
 * 🧠 CORE IDEA:
 * ------------------------------------------------------------
 * Instead of building string directly,
 * we maintain a LIST representing current path.
 *
 * Example Path Representation:
 *   [1, 2, 5]  → convert → "1->2->5"
 *
 * WHY THIS IS GOOD:
 * ------------------------------------------------------------
 * - More intuitive (mirrors actual path)
 * - Classic backtracking pattern (add → recurse → remove)
 * - Easier to extend for other problems (sum, constraints etc.)
 *
 * ------------------------------------------------------------
 * ⏱ Time Complexity:
 *   O(N * L)
 *   - N = number of nodes
 *   - L = average path length (for string conversion)
 *
 * ⏱ Space Complexity:
 *   O(H) recursion stack
 *   O(H) path list
 *
 *   where H = height of tree
 * ============================================================
 */
public class BinaryTreePaths_ListVersion {

    // Tree definition
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public static List<String> binaryTreePaths(TreeNode root) {
        List<String> result = new ArrayList<>();

        if (root == null) return result;

        // 🔥 Path list stores current traversal path
        List<Integer> path = new ArrayList<>();

        dfs(root, path, result);

        return result;
    }

    private static void dfs(TreeNode node, List<Integer> path, List<String> result) {
        if (node == null) return;

        // =====================================================
        // 🟢 STEP 1: CHOOSE (Add current node to path)
        // =====================================================
        path.add(node.val);

        // =====================================================
        // 🟢 STEP 2: CHECK (Leaf node condition)
        // =====================================================
        if (node.left == null && node.right == null) {

            // Convert path list → string
            // Example: [1,2,5] → "1->2->5"
            result.add(buildPathString(path));

        } else {
            // =================================================
            // 🟢 STEP 3: EXPLORE (Go deeper)
            // =================================================
            dfs(node.left, path, result);
            dfs(node.right, path, result);
        }

        // =====================================================
        // 🔴 STEP 4: UNDO (Backtracking)
        // =====================================================
        // Remove last element before returning to parent
        // This ensures path is correct for next branch
        path.remove(path.size() - 1);
    }

    /**
     * Helper method to convert list → string
     *
     * Example:
     *   [1,2,5] → "1->2->5"
     *
     * WHY separate method?
     * - Cleaner DFS logic
     * - Reusable
     */
    private static String buildPathString(List<Integer> path) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append("->");
            sb.append(path.get(i));
        }

        return sb.toString();
    }

    // ------------------ DRIVER ------------------
    public static void main(String[] args) {

        /*
                 1
                / \
               2   3
                \
                 5

        Expected Output:
        ["1->2->5", "1->3"]
        */

        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(3);
        root.left.right = new TreeNode(5);

        System.out.println(binaryTreePaths(root));
    }
}

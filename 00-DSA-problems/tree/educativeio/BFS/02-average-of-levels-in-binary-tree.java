/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * You are given the root of a binary tree. Your task is to return the average 
 * value of the nodes on each level in the form of an array (or list).
 * Note: Only answers within 10^-5 of the actual answer will be accepted.
 * 
 * Constraints:
 * - The number of nodes in the tree is in the range [1, 10^4].
 * - -2^31 <= Node.data <= 2^31 - 1
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Since node values can be up to 2^31 - 1, can the sum of a level exceed the 32-bit integer limit?"
 *   Yes, it absolutely will. This prevents the silent bug of integer overflow. We must use 64-bit `long` or `double` for the sum.
 * - "Will the tree ever be null/empty?"
 *   The constraints state [1, 10^4] nodes, so the tree is guaranteed to have at least one node. 
 *   This saves us from an unnecessary `if (root == null)` check, though it's good defensive practice to keep it.
 * - "Should the return type be an array `double[]` or a `List<Double>`?"
 *   Since we don't know the depth of the tree upfront, a dynamically resizing `List<Double>` is standard in Java.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We need to aggregate data (sum and count) based on a node's *depth* (level). The physical 
 * tree links traverse parent-to-child, but our math must run horizontally across siblings and cousins.
 * 
 * --- APPROACH 1: DFS (Depth-First Search) with HashMaps/Lists ---
 * 1. What I'd naturally try: Traverse the tree using a standard recursive DFS. Pass a `depth` 
 *    integer down. I'll maintain two global lists: `sums` and `counts`. When I visit a node at 
 *    `depth d`, I add its value to `sums.get(d)` and increment `counts.get(d)`.
 * 2. Why it works: DFS guarantees we visit every node. By carrying the depth along, we can 
 *    accumulate the required data for each horizontal layer, and calculate the averages at the very end.
 * 3. Why it's costly (though acceptable): It requires two passes—one to gather the sums and counts, 
 *    and a second to compute the averages. It also logically separates nodes that belong to the same 
 *    level, constantly bouncing between indices in our tracker lists.
 * 4. What work is being repeated: We have to manage auxiliary data structures (sums and counts) and 
 *    re-iterate over them after the traversal finishes.
 * 5. Time Complexity: O(N) — because we visit each of the N nodes exactly once, then iterate through 
 *    the H levels (H <= N) to compute the final averages.
 * 6. Space Complexity: O(H) — because we store lists of size H (where H is the tree height), AND we 
 *    consume O(H) space on the recursion call stack. Worst case (skewed tree) this is O(N).
 * 
 * [The Core Observation]
 * If we can traverse the tree strictly level by level, we won't need to maintain global lists of sums 
 * and counts. We can just keep a running sum and count for the *current* level, compute the average 
 * immediately, and move on.
 * 
 * --- APPROACH 2: Iterative BFS (Breadth-First Search) (The Optimal Way) ---
 * 1. How it works: Use a Queue. Start with the root. For each level, check `size = queue.size()`. 
 *    This `size` snapshot tells us exactly how many nodes are on the current level. Loop exactly `size` 
 *    times: pop a node, add its value to a level sum, and push its children. Divide the sum by `size` 
 *    and store the result.
 * 2. Time Complexity: O(N) — because every node is enqueued exactly once, dequeued exactly once, 
 *    and arithmetic operations take O(1) time.
 * 3. Space Complexity: O(M) — where M is the maximum number of nodes at any single level. In a balanced 
 *    tree, the bottom level holds roughly N/2 nodes, so space is O(N). No recursion stack is used, and 
 *    we don't need intermediate `sums` and `counts` lists.
 * 
 * [Which one I'd write in an interview]
 * Approach 2 (BFS). Computing level-based statistics naturally maps to level-order traversal. It allows 
 * us to compute the average on the fly, eliminating the need for extra tracking lists and post-processing.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Massive Node Values (Integer Overflow): A level could have 10,000 nodes, each with value 2,147,483,647.
 *   Summing these in a 32-bit `int` wraps around to negative garbage. The sum variable MUST be a `double` or `long`.
 * - Deep Skewed Tree (e.g., linked list shape): DFS might hit a StackOverflowError if depth approaches 10^4. 
 *   BFS Queue handles this gracefully (the queue size will just stay at 1).
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * In BFS, the Queue state inherently boundaries levels *if you capture its size before popping*. 
 * If you just loop `while(!queue.isEmpty())` and pop one by one, levels bleed together. By capturing 
 * `int levelSize = queue.size()` at the start of the outer loop, you freeze the level's boundary.
 * 
 * [Examples & Diagram]
 * Tree:
 *         3
 *        / \
 *       9  20
 *          / \
 *         15  7
 * 
 * [Dry Run (BFS)]
 * Init: Queue = [3], Result = []
 * Outer Loop 1:
 *   - levelSize = 1, sum = 0.0
 *   - Inner Loop (runs 1 time):
 *       - Pop 3. sum = 3.0. Push 9, Push 20. Queue = [9, 20]
 *   - Avg = 3.0 / 1 = 3.0. Result = [3.0]
 * Outer Loop 2:
 *   - levelSize = 2, sum = 0.0
 *   - Inner Loop (runs 2 times):
 *       - Pop 9. sum = 9.0. (No children). Queue = [20]
 *       - Pop 20. sum = 29.0. Push 15, Push 7. Queue = [15, 7]
 *   - Avg = 29.0 / 2 = 14.5. Result = [3.0, 14.5]
 * Outer Loop 3:
 *   - levelSize = 2, sum = 0.0
 *   - Inner Loop (runs 2 times):
 *       - Pop 15. sum = 15.0. (No children). Queue = [7]
 *       - Pop 7. sum = 22.0. (No children). Queue = []
 *   - Avg = 22.0 / 2 = 11.0. Result = [3.0, 14.5, 11.0]
 * Output: [3.0, 14.5, 11.0]
 * 
 * [Pitfalls]
 * - Missing the Integer Overflow: `long sum = 0` or `double sum = 0` is required. If you use `int sum`, 
 *   you fail on large test cases.
 * - Modifying queue size in the loop header: Writing `for (int i = 0; i < queue.size(); i++)` is a fatal 
 *   bug because `queue.size()` changes as you push children! You must lock it in: `int size = queue.size();`
 * 
 * [Pattern Recognition]
 * When you see: "Process tree by levels" or "Find the max/min/average/rightmost node per level".
 * Think: BFS with an inner `for` loop bounded by the snapshot of `queue.size()`.
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: How would you do this if you wanted the average of levels from bottom to top?
 * A: I would still use BFS, but instead of appending to the end of a List, I would insert at the 
 *    front `result.add(0, average)` or use a `LinkedList` as a Deque, returning a reversed view.
 * 
 * Q: What if memory is strictly limited and the tree is extremely wide?
 * A: Standard BFS fails because queue size grows to O(N). We could switch to the DFS Approach (Approach 1). 
 *    DFS only requires O(H) space. If H (height) is much smaller than the maximum width, DFS is much more 
 *    memory efficient at the cost of managing the intermediate tracking lists.
 * 
 * ============================================================================
 * 6. JAVA CODE (Contains both BFS and DFS implementations)
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AverageOfLevelsInBinaryTree {

    // Standard Binary Tree Node definition
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int val) { this.val = val; }
    }

    /**
     * APPROACH 2: Iterative BFS (Optimal and cleanest for level-based statistics).
     */
    public static List<Double> averageOfLevelsBFS(TreeNode root) {
        List<Double> result = new ArrayList<>();
        if (root == null) return result; // Defensive check

        // Queue manages nodes chronologically. 
        // Concrete state: initially holds just [Node(3)]
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        // Process the tree layer by layer
        while (!queue.isEmpty()) {
            // CRITICAL STEP: Take a snapshot of the queue size.
            // This isolates the current level's nodes from their children which 
            // will be added to the queue during the inner loop.
            int levelSize = queue.size();
            
            // Use double to prevent integer overflow when summing up to 10^4 nodes 
            // that can each be up to 2^31 - 1. 
            // A 64-bit double can precisely hold integers up to 9 * 10^15.
            double levelSum = 0;

            // Process exactly `levelSize` nodes (the entire current horizontal tier)
            for (int i = 0; i < levelSize; i++) {
                // Remove the front node
                TreeNode currentNode = queue.poll();
                
                // Add its value to the running total for this level
                levelSum += currentNode.val;

                // Enqueue the next generation (children) for the NEXT outer loop iteration.
                // We strictly check for null to keep the queue clean.
                if (currentNode.left != null) {
                    queue.offer(currentNode.left);
                }
                if (currentNode.right != null) {
                    queue.offer(currentNode.right);
                }
            }

            // The inner loop finished. We have the sum and count (levelSize) for this layer.
            // Compute the average and append it to our result list.
            result.add(levelSum / levelSize);
        }

        return result;
    }

    /**
     * APPROACH 1: Recursive DFS (Included for comparison / follow-up scenarios).
     */
    public static List<Double> averageOfLevelsDFS(TreeNode root) {
        // We need two parallel lists to keep track of the running sum and count per depth.
        // Index `i` represents depth `i`.
        List<Double> sums = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        
        // Traverse the tree, populating the lists
        dfsHelper(root, 0, sums, counts);
        
        // Post-processing: Calculate the average for each level
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < sums.size(); i++) {
            result.add(sums.get(i) / counts.get(i));
        }
        
        return result;
    }

    private static void dfsHelper(TreeNode node, int depth, List<Double> sums, List<Integer> counts) {
        if (node == null) return;
        
        // If this is the first time we've reached this depth, initialize the buckets
        if (depth == sums.size()) {
            sums.add(0.0);
            counts.add(0);
        }
        
        // Update the running sum and count for the current depth
        sums.set(depth, sums.get(depth) + node.val);
        counts.set(depth, counts.get(depth) + 1);
        
        // Recurse down, incrementing the depth
        dfsHelper(node.left, depth + 1, sums, counts);
        dfsHelper(node.right, depth + 1, sums, counts);
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test Case 1: Standard Tree from problem example
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
        
        System.out.println("Test 1 (BFS Optimal): " + averageOfLevelsBFS(root1)); 
        // Expected: [3.0, 14.5, 11.0]
        System.out.println("Test 1 (DFS Alterna): " + averageOfLevelsDFS(root1)); 
        // Expected: [3.0, 14.5, 11.0]

        // Test Case 2: Overflow Danger (Max Integer values)
        //         2147483647
        //           /    \
        // 2147483647      2147483647
        TreeNode root2 = new TreeNode(Integer.MAX_VALUE);
        root2.left = new TreeNode(Integer.MAX_VALUE);
        root2.right = new TreeNode(Integer.MAX_VALUE);
        
        System.out.println("Test 2 (Overflow Danger BFS): " + averageOfLevelsBFS(root2));
        // Expected: [2147483647.0, 2147483647.0] 
        // If an `int` sum was used, the second level would output an incorrect negative average.
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: BFS using a Queue.
 * - Key observation: Freezing `levelSize = queue.size()` boundaries the levels perfectly.
 * - Most common trap: Using `int` for `levelSum` resulting in integer overflow.
 * - Mental trigger: "Statistics per level" -> BFS with size snapshot loop + `double` math.
 */

import java.util.*;

/**
 * Definition for a binary tree node.
 */
class TreeNode {
    int val;
    TreeNode left, right;

    TreeNode(int val) {
        this.val = val;
    }
}

public class AverageOfLevels {

    /**
     * BFS Level Order Traversal
     *
     * Time Complexity: O(N)
     *  - Every node is visited exactly once
     *
     * Space Complexity: O(W)
     *  - W = max width of tree (queue size)
     */
    public static List<Double> averageOfLevels(TreeNode root) {

        List<Double> result = new ArrayList<>();

        // Edge case
        if (root == null) return result;

        Queue<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);

        // Process level by level
        while (!queue.isEmpty()) {

            int levelSize = queue.size(); // number of nodes in current level
            long levelSum = 0; // use long to avoid overflow

            for (int i = 0; i < levelSize; i++) {

                TreeNode current = queue.poll();
                levelSum += current.val;

                // Add children for next level
                if (current.left != null) queue.offer(current.left);
                if (current.right != null) queue.offer(current.right);
            }

            // Compute average
            result.add(levelSum * 1.0 / levelSize);
        }

        return result;
    }
}

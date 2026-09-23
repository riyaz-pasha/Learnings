/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given a binary tree, return its zigzag level order traversal. The zigzag 
 * level order traversal corresponds to traversing nodes from left to right for 
 * one level, and then right to left for the next level, and so on, reversing 
 * direction after every level.
 * 
 * Constraints:
 * - The number of nodes in the tree is in the range 0 to 500.
 * - -100 <= node.data <= 100
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Should the return type be a single flat list or a list of lists (one per level)?"
 *   Crucial for defining the method signature. The standard is `List<List<Integer>>`, so I'll assume that.
 * - "What should be returned for an empty tree (0 nodes)?"
 *   Prevents NullPointerExceptions. Usually, an empty list `[]` is expected, not `null`.
 * - "Does the first level (the root) strictly go Left-to-Right?"
 *   Confirms the starting state of our directional toggle (yes, L-to-R first).
 * - "Are we allowed to modify the tree nodes?"
 *   Ensures we know if we can use structural pointers or must rely purely on auxiliary memory. (Assume no).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We need to group nodes horizontally (by level) but alternate the *reading order* of 
 * those groups. Normal tree traversal explores purely top-down/left-right. 
 * 
 * --- APPROACH 1: Standard BFS + Post-Level Reversal ---
 * 1. What I'd naturally try: Do a normal Breadth-First Search (BFS) using a Queue. I'll use a 
 *    boolean flag `leftToRight` starting at `true`. I'll collect all nodes for the current level 
 *    into a standard list. If `!leftToRight`, I'll use `Collections.reverse(list)` before adding 
 *    it to the final result. Then I toggle the flag.
 * 2. Why it works: BFS inherently groups nodes by level. Reversing the list after it's built perfectly 
 *    simulates reading it from right to left.
 * 3. Why it's sub-optimal: We are iterating over the nodes of every even level twice—once to build 
 *    the list, and a second time to reverse it in place.
 * 4. What work is being repeated: The secondary pass over the level's array just to reorder items 
 *    we already hold in memory.
 * 5. Time Complexity: O(N) — because we visit every node once (O(N)), and reverse half the nodes. 
 *    Reversing K elements takes O(K) time. O(N + N/2) simplifies to O(N).
 * 6. Space Complexity: O(N) — because the Queue stores up to N/2 nodes at the widest level, and 
 *    the final result stores all N nodes.
 * 
 * [The Core Observation]
 * If we know we need to reverse the order *while* we are building the list, we don't need a second 
 * pass. We just need a data structure for the level that allows O(1) insertions at the *front* 
 * as well as the *back*. A Double-Ended Queue (Deque) or a LinkedList provides exactly this.
 * 
 * --- APPROACH 2: BFS with a Double-Ended Queue (Deque) for Level Construction ---
 * 1. How it works: We keep the exact same BFS queue to *discover* nodes left-to-right. But for 
 *    storing the *values* of the current level, we use a `LinkedList`. If `leftToRight` is true, 
 *    we append to the back (`addLast`). If false, we prepend to the front (`addFirst`). 
 * 2. Time Complexity: O(N) — because every node is enqueued once, dequeued once, and inserted 
 *    into the level list in O(1) time. No secondary reversal pass is needed.
 * 3. Space Complexity: O(N) — because the BFS Queue takes O(W) where W is max width (up to N/2), 
 *    and we allocate O(N) total space across the sub-lists for the final result.
 * 
 * [Which one I'd write in an interview]
 * Approach 2. It shows mastery of data structures (knowing that a LinkedList allows O(1) head 
 * insertions). It cleanly decouples the *discovery* mechanism (which stays strictly L-to-R) from 
 * the *recording* mechanism (which toggles).
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Empty Tree: 0 nodes. Must return `[]`, avoiding null access on `root.left`.
 * - Single Node Tree: Returns `[[root.val]]`. The loop runs once and terminates safely.
 * - Skewed Tree (e.g., all left children): The width is 1. The result will just be lists of 
 *   size 1, and the toggle effectively does nothing, but the logic must not break.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * Never try to alter the order you push children into the BFS Queue. If you try to push right-child 
 * then left-child on alternate levels, you corrupt the discovery order for the *next* level down. 
 * ALWAYS traverse the tree exactly the same way (Left then Right). ONLY change how you insert the 
 * *values* into your result list.
 * 
 * [Examples & Diagram]
 * Tree:
 *          1
 *        /   \
 *       2     3
 *      / \   / \
 *     4   5 6   7
 * 
 * [Dry Run (Optimal Approach)]
 * Init: Queue = [1], Result = [], leftToRight = true
 * 
 * Loop 1 (Level 0):
 * - Size: 1. levelList = LinkedList()
 * - Pop 1. `leftToRight` is true -> `levelList.addLast(1)`. List: [1]
 * - Push children: 2, 3. Queue = [2, 3]
 * - Add [1] to Result. Toggle `leftToRight` -> false
 * 
 * Loop 2 (Level 1):
 * - Size: 2. levelList = LinkedList()
 * - Pop 2. `leftToRight` is false -> `levelList.addFirst(2)`. List: [2]
 * - Push children: 4, 5. Queue = [3, 4, 5]
 * - Pop 3. `leftToRight` is false -> `levelList.addFirst(3)`. List: [3, 2]  <-- ZIGZAG!
 * - Push children: 6, 7. Queue = [4, 5, 6, 7]
 * - Add [3, 2] to Result. Toggle `leftToRight` -> true
 * 
 * Result so far: [[1], [3, 2]]
 * 
 * [Pitfalls]
 * - Using `ArrayList.add(0, val)`: While this works logically, `ArrayList` shifts all elements 
 *   in memory every time you add to the front, degrading the time complexity to O(N^2) for a level. 
 *   You MUST use a `LinkedList` or `Deque` for the level container to keep O(1) front insertions.
 * 
 * [Pattern Recognition]
 * When you see: "Alternate directions", "Snake pattern", "Spiral traversal".
 * Think: Standard BFS Queue for iteration + a Deque/LinkedList with a boolean toggle for formatting.
 * 
 * [Interview Script]
 * "To achieve the zigzag pattern, we shouldn't change how we traverse the tree—that gets messy quickly. 
 * Instead, I'll use standard BFS to visit nodes left-to-right on every level. To handle the zigzag, 
 * I'll use a LinkedList for each level's results. I'll maintain a boolean toggle. If it's a left-to-right 
 * level, I append values to the back of the LinkedList. If it's right-to-left, I prepend values to the 
 * front, which naturally reverses them in O(1) time per node. Then I toggle the boolean for the next level. 
 * This keeps Time and Space at strictly O(N)."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: Could we do this with Depth-First Search (DFS)?
 * A: Yes! We can pass a `depth` variable down the recursive calls. We ensure the `Result` list has 
 *    an initialized `LinkedList` at index `depth`. If `depth % 2 == 0`, we do `addLast()`, otherwise 
 *    `addFirst()`. It uses O(H) auxiliary space for the stack, though the output still requires O(N).
 * 
 * Q: What if we don't want to use a Java LinkedList because object overhead is too high?
 * A: We could pre-allocate an array of size `levelSize` (since we know the size before the inner loop), 
 *    and use two pointers or simply place elements at index `i` (for L-to-R) or `levelSize - 1 - i` 
 *    (for R-to-L). This is extremely memory-efficient and avoids linked node allocation.
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ZigzagLevelOrder {

    // Standard Binary Tree Node definition
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int val) { this.val = val; }
    }

    /**
     * Executes a zigzag level order traversal of the binary tree.
     * 
     * @param root The root of the binary tree.
     * @return A list of lists containing the zigzag level order node values.
     */
    public static List<List<Integer>> zigzagLevelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        
        // Edge Case: Empty tree. Fast-fail and return the empty result array.
        if (root == null) {
            return result;
        }

        // Standard BFS Queue to maintain strictly top-down, left-to-right discovery order.
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        // Toggle to control whether we append or prepend to the current level's list.
        // The problem states the first level (root) is Left-to-Right.
        boolean leftToRight = true;

        while (!queue.isEmpty()) {
            // Snapshot the size to boundary the current level.
            int levelSize = queue.size();
            
            // CRITICAL: We use a LinkedList specifically because it implements Deque.
            // This guarantees that addFirst() and addLast() are O(1) operations.
            // (If we used ArrayList, add(0, val) would be an O(K) operation).
            LinkedList<Integer> currentLevelValues = new LinkedList<>();

            for (int i = 0; i < levelSize; i++) {
                // Poll always happens from the front of the BFS Queue (Left-to-Right).
                TreeNode currentNode = queue.poll();

                // Format the output based on the current level's required direction.
                if (leftToRight) {
                    currentLevelValues.addLast(currentNode.val);
                } else {
                    currentLevelValues.addFirst(currentNode.val);
                }

                // Discovery is ALWAYS Left-child then Right-child, regardless of the toggle.
                // This ensures the next level's nodes are queued in the correct geographical order.
                if (currentNode.left != null) {
                    queue.offer(currentNode.left);
                }
                if (currentNode.right != null) {
                    queue.offer(currentNode.right);
                }
            }

            // Level complete. Add the formatted list to our final result.
            result.add(currentLevelValues);
            
            // Flip the toggle for the next horizontal layer.
            leftToRight = !leftToRight;
        }

        return result;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test Case 1: Standard Tree from explanation
        //          1
        //        /   \
        //       2     3
        //      / \   / \
        //     4   5 6   7
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.left.left = new TreeNode(4);
        root1.left.right = new TreeNode(5);
        root1.right.left = new TreeNode(6);
        root1.right.right = new TreeNode(7);
        
        System.out.println("Test 1 (Standard): " + zigzagLevelOrder(root1)); 
        // Expected: [[1], [3, 2], [4, 5, 6, 7]]

        // Test Case 2: Empty Tree Edge Case
        TreeNode root2 = null;
        System.out.println("Test 2 (Empty): " + zigzagLevelOrder(root2)); 
        // Expected: []

        // Test Case 3: Single Node
        TreeNode root3 = new TreeNode(42);
        System.out.println("Test 3 (Single Node): " + zigzagLevelOrder(root3)); 
        // Expected: [[42]]

        // Test Case 4: Left-Skewed Tree (Width of 1)
        //    1
        //   /
        //  2
        //  /
        // 3
        TreeNode root4 = new TreeNode(1);
        root4.left = new TreeNode(2);
        root4.left.left = new TreeNode(3);
        System.out.println("Test 4 (Skewed): " + zigzagLevelOrder(root4)); 
        // Expected: [[1], [2], [3]] - Toggle works seamlessly even when size is 1.
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: BFS with a size-snapshot loop.
 * - Key observation: Decouple the *discovery* of nodes (always Left-to-Right Queue) 
 *   from the *recording* of values (LinkedList `addFirst` vs `addLast`).
 * - Most common trap: Altering the Queue enqueue order (e.g., pushing right then left) 
 *   which catastrophically scrambles the geography for the subsequent levels. Also, 
 *   using `ArrayList.add(0, val)` which creates an invisible O(N^2) bottleneck.
 * - Mental trigger: "Alternating Pattern" = Standard Queue + LinkedList(Deque) Insertion Toggle.
 */

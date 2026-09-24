/**
 * ============================================================================
 * STUDY NOTE: CLOSEST BINARY SEARCH TREE VALUE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "What should be returned if the tree is empty?" 
 *   (Constraints say nodes range [1, 10^4], so we can assume at least one node, but asking shows defensive programming).
 * - "The target is a float/double, but values are integers. Can there be exact ties in distance?" 
 *   (Yes, e.g., target is 3.5, and nodes 3 and 4 exist. The prompt explicitly requires returning the smaller integer in this case).
 * - "Are we guaranteed that the tree is balanced?" 
 *   (No. A BST with 10^4 nodes could be a straight line (linked list). This immediately flags recursion as a StackOverflow risk).
 * - "Is strict floating-point precision a concern for ties?" 
 *   (Since node values are integers, the only way distances perfectly tie is if the target is exactly halfway between them, e.g., x.5. Standard IEEE 754 double comparison '==' handles this reliably).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We are looking for a value in a sorted structure, but the exact value might not exist. 
 * The bottleneck isn't finding an exact match; it's remembering the "best seen so far" while 
 * efficiently discarding halves of the tree that cannot possibly contain a closer value.
 *
 * Approach 1: In-Order Traversal to List (The Brute Force)
 * What I'd naturally try: I know an In-Order traversal of a BST gives a perfectly sorted list. 
 * I could just traverse the whole tree, put all integers in an array, and then do a linear scan 
 * (or binary search) on the array to find the closest value to the target.
 * Why it works: It completely flattens the problem into a simple 1D array search.
 * Why it's too slow/costly: It completely wastes the structural advantage of the BST. If the root 
 * is 500,000 and the target is 2.5, this approach still visits all the nodes on the right side of the tree.
 * - Time Complexity: O(N) — because we visit every single node to build the list.
 * - Space Complexity: O(N) — because we allocate an entirely new array/list to hold all N node values.
 * 
 * Approach 2: Recursive Binary Search (The Intuitive BST Way)
 * What I'd naturally try: Let's actually use the BST rules! At any node, I compare its value to 
 * my target. I'll update a global `closest` variable if this node is closer. Then, if the target 
 * is less than the current node, I only need to search the left subtree. If greater, the right.
 * Why it works: The BST property guarantees that if target < node.val, all values in the right 
 * subtree will be EVEN LARGER, hence further away. We can safely ignore them.
 * What work is being repeated: No nodes are repeated, but we are paying for call stack memory.
 * - Time Complexity: O(H) — because we only visit one node per depth level.
 * - Space Complexity: O(H) — because the recursive call stack takes memory. In a skewed tree, H = N, 
 *   meaning O(N) space, which could crash the JVM for 10,000 nodes.
 *
 * Approach 3: Iterative Binary Search (The Optimal / Production Choice)
 * What I'd naturally try: Since a BST path-search is just a simple "go left or go right" decision, 
 * I don't need the call stack to remember where I came from. I can just use a `while(current != null)` loop.
 * Why it works: We maintain a `closest` variable. At each step, we calculate the distance. We update 
 * `closest` if necessary (handling the tie-breaker), and then move our `current` pointer left or right.
 * What property removes the bottleneck: By moving the pointer iteratively, we completely eliminate 
 * the memory overhead of the recursion stack.
 * - Time Complexity: O(H) — because we traverse exactly one root-to-leaf path in the worst case, 
 *   where H is the height of the tree (O(log N) balanced, O(N) skewed).
 * - Space Complexity: O(1) — because we only use two primitive variables (`closest` and `current`), 
 *   requiring strictly constant extra memory regardless of tree size.
 *
 * The Interview Choice:
 * Always write Approach 3 (Iterative Binary Search). It is shockingly simple (about 10 lines of code), 
 * handles massive skewed trees without crashing, and proves you understand how to navigate a BST 
 * without relying on recursion as a crutch.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Target is a perfect midpoint tie (e.g., Target: 3.5, Nodes: 3 and 4) -> Must return 3.
 * 2. Target is far outside the tree bounds (e.g., Target: -9999, Min Node: 0) -> Safely traverses the left spine and returns the minimum node.
 * 3. Tree is a completely skewed Linked List -> Validates the O(1) space iterative approach won't crash.
 * 4. Exact match found -> (e.g., Target: 5.0, Node: 5) -> Distance is 0, should ideally short-circuit or just naturally win.
 *
 *
 * 4. DRY RUN (Iterative Binary Search)
 * ----------------------------------------------------------------------------
 * Tree:
 *       4
 *      / \
 *     2   5
 *    / \
 *   1   3
 * 
 * Target: 3.714
 * 
 * State Tracker (closest initialized to root.val = 4):
 * 1. current = 4. 
 *    Dist to 4: |4 - 3.714| = 0.286. Best dist is 0.286. 
 *    Target (3.714) < 4, so current = current.left (2).
 * 2. current = 2. 
 *    Dist to 2: |2 - 3.714| = 1.714. 
 *    1.714 > 0.286. `closest` remains 4.
 *    Target (3.714) > 2, so current = current.right (3).
 * 3. current = 3. 
 *    Dist to 3: |3 - 3.714| = 0.714.
 *    0.714 > 0.286. `closest` remains 4.
 *    Target (3.714) > 3, so current = current.right (null).
 * 4. current == null. Loop ends. 
 * Final Result: 4.
 *
 * Pitfalls: 
 * - Ignoring the tie-breaker condition. If you only check `currentDist < closestDist`, 
 *   a target of `3.5` hitting node `4` (dist 0.5) then node `3` (dist 0.5) will fail to update 
 *   `closest` to `3`. You must explicitly check for `==` distance and take `Math.min`.
 * - Updating the pointer incorrectly. The decision to go left or right depends strictly on 
 *   `target < current.val`, NEVER on the distances themselves. The BST is sorted by values, not distances.
 *
 * Pattern Recognition: 
 * "When searching a BST for a specific state (exact, closest, floor, ceiling) -> Use a while loop 
 * with a single 'best_so_far' tracking variable."
 *
 * Interview Script:
 * "Because this is a Binary Search Tree, we don't need to check every node. I'll use an iterative 
 * approach to avoid call stack memory overhead. I'll maintain a 'closest' variable, updating it 
 * as I traverse. If the target is smaller than the current node, I go left; if larger, I go right. 
 * I'll also add a specific check for when distances tie, ensuring we save the smaller value. 
 * This gives us O(H) time and O(1) space."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if instead of ONE closest value, I wanted the 'K' closest values? (LeetCode 272)
 * A1: The O(1) space iterative approach doesn't easily scale to K. I would do an In-Order 
 *     traversal to maintain a sorted sequence, but use a Sliding Window (Deque) of size K. 
 *     As I visit nodes, if the queue size is < K, I add the node. If it's K, I check if the 
 *     current node is closer than the FIRST element in the deque. If yes, pop front, push back. 
 *     If no, we can actually terminate early because any future nodes will be even further away!
 *
 * Q2: What if the tree is modified frequently (inserts/deletes) and we need to query closest() very often?
 * A2: If it's a standard BST, a skewed tree makes queries O(N). To guarantee fast queries, we must 
 *     ensure the BST self-balances (e.g., using a Red-Black Tree or AVL tree), which strictly 
 *     bounds the height to O(log N) for both modifications and our closest-value queries.
 */

public class ClosestBSTValueStudy {

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
     * APPROACH 3: ITERATIVE BINARY SEARCH (The optimal, professional choice)
     * Time: O(H) | Space: O(1)
     */
    public int closestValue(TreeNode root, double target) {
        // We know constraints guarantee at least 1 node, so root is not null.
        int closest = root.val;
        TreeNode current = root;

        while (current != null) {
            // Calculate absolute distances
            double currentDistance = Math.abs(current.val - target);
            double closestDistance = Math.abs(closest - target);

            // Update the closest value if we found a strictly better one
            if (currentDistance < closestDistance) {
                closest = current.val;
            } 
            // The Tie-Breaker: Distances are identical.
            // e.g. target 3.5, comparing 4 (old closest) and 3 (current). 
            // We must take the smaller integer.
            else if (currentDistance == closestDistance) {
                closest = Math.min(closest, current.val);
            }

            // Early exit optimization: If we hit exact match, distance is 0. 
            // We can't possibly get closer than 0.
            if (currentDistance == 0.0) {
                return current.val;
            }

            // Standard BST traversal logic: 
            // Where should we look next to potentially find a closer value?
            if (target < current.val) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        return closest;
    }

    /**
     * APPROACH 2: RECURSIVE BINARY SEARCH 
     * Time: O(H) | Space: O(H)
     * Included to demonstrate the recursive mental model, but Iterative is preferred.
     */
    public int closestValueRecursive(TreeNode root, double target) {
        return dfs(root, target, root.val);
    }

    private int dfs(TreeNode node, double target, int closest) {
        if (node == null) {
            return closest;
        }

        double currentDist = Math.abs(node.val - target);
        double closestDist = Math.abs(closest - target);

        if (currentDist < closestDist) {
            closest = node.val;
        } else if (currentDist == closestDist) {
            closest = Math.min(closest, node.val);
        }

        if (target < node.val) {
            return dfs(node.left, target, closest);
        } else {
            return dfs(node.right, target, closest);
        }
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        ClosestBSTValueStudy study = new ClosestBSTValueStudy();

        // Standard Case: The example from the DRY RUN
        //       4
        //      / \
        //     2   5
        //    / \
        //   1   3
        TreeNode standard = new TreeNode(4,
                new TreeNode(2, new TreeNode(1), new TreeNode(3)),
                new TreeNode(5)
        );
        
        System.out.println("Standard (Target: 3.714): " + study.closestValue(standard, 3.714)); // Expected: 4

        // Edge Case 1: The Exact Tie Breaker
        // Target is 3.5. Nodes 3 and 4 are exactly 0.5 away.
        // The rule says return the smaller node.
        System.out.println("Tie Breaker (Target: 3.5): " + study.closestValue(standard, 3.5)); // Expected: 3

        // Edge Case 2: Out of bounds (Target is massively negative)
        // Should traverse down the left spine and return the absolute minimum: 1.
        System.out.println("Far Left (Target: -999.0): " + study.closestValue(standard, -999.0)); // Expected: 1

        // Edge Case 3: Out of bounds (Target is massively positive)
        // Should traverse down the right spine and return the absolute maximum: 5.
        System.out.println("Far Right (Target: 999.0): " + study.closestValue(standard, 999.0)); // Expected: 5
        
        // Edge Case 4: Exact Match
        System.out.println("Exact Match (Target: 2.0): " + study.closestValue(standard, 2.0)); // Expected: 2

        // Edge Case 5: Single Node Tree
        TreeNode single = new TreeNode(42);
        System.out.println("Single Node (Target: 0.0): " + study.closestValue(single, 0.0)); // Expected: 42
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: BST Path Traversal with a "Best-Seen-So-Far" tracker.
 * Key Observation: You decide to go left or right based strictly on `target < node.val`, 
 *                  NOT on the distances. The BST structural rules guide you, and you 
 *                  just record the closest value encountered along that specific path.
 * Memorize: `if (dist == bestDist) closest = Math.min(closest, node.val);` (The tie-breaker).
 * Most Common Trap: Using recursion without realizing it could trigger a StackOverflowError 
 *                   on a skewed tree of 10,000 nodes. A `while` loop is vastly superior here.
 * One-Line Mental Trigger: "Closest BST = While loop, track best, target < val ? left : right."
 * ============================================================================
 */

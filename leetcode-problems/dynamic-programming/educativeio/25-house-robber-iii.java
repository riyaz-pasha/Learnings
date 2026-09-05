import java.util.*;

/**
 * ============================================================================
 * PROBLEM STATEMENT: House Robber III (Binary Tree)
 * The thief has found a new neighborhood shaped like a binary tree.
 * Each node represents a house, and the value is the money inside.
 * The thief cannot rob two directly connected houses (parent and child).
 * Return the maximum amount of money the thief can rob without alerting police.
 * 
 * Constraints:
 * 1 <= Number of nodes <= 500
 * 0 <= node.data <= 10^4
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, point out how the data structure completely changes 
 * the DP strategy:
 * 
 * Q: "Can house values be negative?"
 * A: Constraint says 0 <= node.data <= 10^4. We never lose money by robbing, 
 *    so we always want to maximize our haul.
 * 
 * Q: "Is this a Binary Search Tree (BST) or just a Binary Tree?"
 * A: Just a binary tree. We cannot rely on any sorted properties.
 * 
 * CRITICAL SENIOR INSIGHT - TREE DP:
 * "In standard DP, we use arrays for Tabulation (Bottom-Up). However, trees 
 * do not have linear indices. For Tree DP, the 'Bottom-Up' approach is achieved 
 * via a Post-Order Traversal. We evaluate the leaves first and pass their 
 * optimal states UP to the parents."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given node (house), I have exactly two choices:
 *  1. ROB THIS NODE: If I rob this house, I am strictly forbidden from robbing 
 *     its direct left and right children. But I CAN rob its grandchildren.
 *  2. SKIP THIS NODE: If I skip this house, the alarms won't trigger if I rob 
 *     its children. I can freely choose whatever the optimal combination was 
 *     for the left child (whether it was robbed or skipped) and the right child.
 * 
 * Because calculating the optimal haul for a parent requires the optimal hauls 
 * of its subtrees, we have overlapping subproblems and optimal substructure."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example Tree:
 *      3
 *     / \
 *    2   3
 *     \   \
 *      3   1
 * 
 * Let's trace the Bottom-Up Post-Order (Space Optimized) approach:
 * State returned: [Max if we ROB this node, Max if we SKIP this node]
 * 
 * - Leaf 3 (left bottom):
 *   Rob = 3, Skip = 0 -> Returns [3, 0]
 * 
 * - Node 2 (left mid):
 *   Rob = 2 + (left skip 0) + (right skip 0 from Leaf 3) = 2
 *   Skip = max(left rob/skip 0) + max(right rob 3, skip 0) = 3
 *   Returns [2, 3]
 * 
 * - Leaf 1 (right bottom):
 *   Rob = 1, Skip = 0 -> Returns [1, 0]
 * 
 * - Node 3 (right mid):
 *   Rob = 3 + (left skip 0) + (right skip 0 from Leaf 1) = 3
 *   Skip = max(left 0) + max(right rob 1, skip 0) = 1
 *   Returns [3, 1]
 * 
 * - Root 3 (top):
 *   Rob = 3 + (left skip 3 from Node 2) + (right skip 1 from Node 3) = 7
 *   Skip = max(left rob 2, skip 3) + max(right rob 3, skip 1) = 3 + 3 = 6
 *   Returns [7, 6]
 * 
 * Max of [7, 6] is 7. (Path: Root 3 + Leaf 3 + Leaf 1).
 */
public class HouseRobberIII {

    /**
     * Standard Definition for a binary tree node.
     */
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int val) { this.val = val; }
    }

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: From the root, calculate the money if we rob it (adding grandchildren)
     * versus if we skip it (adding children).
     * 
     * Time Complexity: O(2^N) - We recalculate the same subtrees multiple times 
     * (once as a child, once as a grandchild).
     * Space Complexity: O(H) - Maximum depth of the recursion tree (height H).
     */
    public int robRecursive(TreeNode root) {
        // BASE CASE REASONING:
        // If the node is null, there is no physical house here. 
        // A non-existent house contains exactly $0.
        if (root == null) return 0;

        // Choice 1: ROB this node. 
        // We get this node's value, PLUS the optimal hauls from its 4 grandchildren.
        int rob = root.val;
        if (root.left != null) {
            rob += robRecursive(root.left.left) + robRecursive(root.left.right);
        }
        if (root.right != null) {
            rob += robRecursive(root.right.left) + robRecursive(root.right.right);
        }

        // Choice 2: SKIP this node.
        // We can safely take the optimal hauls from its 2 direct children.
        int skip = robRecursive(root.left) + robRecursive(root.right);

        return Math.max(rob, skip);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: The brute force method visits nodes repeatedly. We can use a HashMap 
     * to cache the maximum money we can rob starting from each specific node.
     * 
     * Time Complexity: O(N) - We evaluate each node exactly once.
     * Space Complexity: O(N) - HashMap stores N nodes, recursion stack takes up to H.
     */
    public int robMemo(TreeNode root) {
        return solveMemo(root, new HashMap<>());
    }

    private int solveMemo(TreeNode root, Map<TreeNode, Integer> memo) {
        // BASE CASE REASONING (Same physical logic as brute force)
        // No house = no money.
        if (root == null) return 0;

        // Return cached result if we've evaluated this house before
        if (memo.containsKey(root)) {
            return memo.get(root);
        }

        int rob = root.val;
        if (root.left != null) {
            rob += solveMemo(root.left.left, memo) + solveMemo(root.left.right, memo);
        }
        if (root.right != null) {
            rob += solveMemo(root.right.left, memo) + solveMemo(root.right.right, memo);
        }

        int skip = solveMemo(root.left, memo) + solveMemo(root.right, memo);

        int maxMoney = Math.max(rob, skip);
        memo.put(root, maxMoney);
        
        return maxMoney;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Tree DP (Space-Optimized / L4-L5 Target)
     * ========================================================================
     * Note on Tabulation: Traditional array-based 2D tabulation is highly 
     * unnatural for trees. Instead, "Bottom-Up" is achieved by using a 
     * Post-Order Traversal.
     * 
     * Idea: Instead of asking "What is the max from this node?", we return a 
     * compound state containing TWO answers for every node:
     * 1. The max money if we ROB this node.
     * 2. The max money if we SKIP this node.
     * 
     * By passing this exact state up from the leaves to the root, we eliminate 
     * the need to skip levels (look at grandchildren), collapsing the logic 
     * to true O(N) time without a HashMap.
     * 
     * Time Complexity: O(N) - Single post-order pass.
     * Space Complexity: O(H) - Purely for the recursion stack (H is tree height). 
     * No external HashMaps needed!
     */
    
    // Using Java 14+ Record for ultra-clean state management
    private record RobState(int rob, int skip) {}

    public int robOptimal(TreeNode root) {
        RobState finalState = solveOptimal(root);
        return Math.max(finalState.rob(), finalState.skip());
    }

    private RobState solveOptimal(TreeNode root) {
        // BASE CASE REASONING:
        // A null node is a patch of empty dirt. 
        // If we "rob" it, we get 0. If we "skip" it, we get 0.
        if (root == null) {
            return new RobState(0, 0);
        }

        // Post-Order Traversal: Go all the way to the bottom leaves first.
        RobState leftState = solveOptimal(root.left);
        RobState rightState = solveOptimal(root.right);

        // --- DETAILED STATE TRANSITION EXPLANATION ---
        
        // UNIVERSE 1: We choose to ROB this current node.
        // If we rob this house, the alarms are active. We are mathematically 
        // FORCED to have skipped both the left and right direct children.
        // So we take our value, plus the guaranteed 'skip' values of our children.
        int robThisNode = root.val + leftState.skip() + rightState.skip();

        // UNIVERSE 2: We choose to SKIP this current node.
        // Since we didn't rob this house, the alarms are silent. 
        // We have total freedom for the children. 
        // For the left child, was it better to rob it or skip it? We take the max.
        // For the right child, was it better to rob it or skip it? We take the max.
        int skipThisNode = Math.max(leftState.rob(), leftState.skip()) 
                         + Math.max(rightState.rob(), rightState.skip());

        // Package our answers for this room and hand them up to the parent.
        return new RobState(robThisNode, skipThisNode);
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new HouseRobberIII();
        
        // Test Case 1: [3,2,3,null,3,null,1]
        TreeNode root1 = new TreeNode(3);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.left.right = new TreeNode(3);
        root1.right.right = new TreeNode(1);
        
        // Test Case 2: [3,4,5,1,3,null,1]
        TreeNode root2 = new TreeNode(3);
        root2.left = new TreeNode(4);
        root2.right = new TreeNode(5);
        root2.left.left = new TreeNode(1);
        root2.left.right = new TreeNode(3);
        root2.right.right = new TreeNode(1);
        
        System.out.println("---- Test Case 1 ----");
        System.out.println("Expected: 7");
        System.out.println("Recursive (Brute) : " + solver.robRecursive(root1));
        System.out.println("Memoization       : " + solver.robMemo(root1));
        System.out.println("Tree DP (Optimal) : " + solver.robOptimal(root1));
        System.out.println();
        
        System.out.println("---- Test Case 2 ----");
        System.out.println("Expected: 9"); // Rob 4 and 5 -> 9
        System.out.println("Recursive (Brute) : " + solver.robRecursive(root2));
        System.out.println("Memoization       : " + solver.robMemo(root2));
        System.out.println("Tree DP (Optimal) : " + solver.robOptimal(root2));
        System.out.println();
    }
}

/*
 * ================================================================================================
 * GOOGLE-STYLE MOCK ONSITE — HOUSE ROBBER III (LeetCode 337)
 * ================================================================================================
 * Single-file, fully compilable Java 21 solution covering the entire interview lifecycle:
 * restatement -> clarifying questions -> examples -> paradigm sweep -> multiple approaches ->
 * comparison -> recommended approach -> production implementation -> dry run -> closing summary
 * -> follow-ups -> common mistakes.
 *
 * Run with:  java HouseRobberIII.java   (Java single-file source-launch, no javac needed)
 * ================================================================================================
 */
public class HouseRobberIII {

    /*
     * ============================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ============================================================================================
     * In my own words: We're given the root of a binary tree. Each node holds a non-negative
     * integer "money" value. We want to select a subset of nodes to "rob" such that no two
     * selected nodes are directly connected by an edge (i.e., we can never pick both a parent
     * and its direct child). Among all such valid subsets, we want the one with maximum total
     * value, and we return that maximum sum (a single integer), not the actual set of nodes.
     *
     * Key facts:
     *   - Input: root of a binary tree, TreeNode.val in [0, 10^4], node count in [1, 500].
     *   - Output: a single int — the maximum sum achievable under the "no adjacent nodes" rule.
     *   - Adjacency is defined by direct parent-child edges. Grandparent-grandchild is fine to
     *     rob together; it's only direct parent-child pairs that trigger the alarm.
     *   - Since node count <= 500, even a mildly inefficient polynomial solution is safe, but the
     *     naive exponential recursion (Section 4, Approach 1) can still blow up in the worst case
     *     for adversarially shaped trees, so it's worth discussing regardless.
     *   - This is structurally identical to "House Robber" (LeetCode 198, array version) and
     *     "House Robber II" (circular array version), except the adjacency structure is a binary
     *     tree instead of a line/cycle, which pushes us toward tree DP via postorder traversal.
     */

    /*
     * ============================================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ============================================================================================
     * 1. Q: Can node values be zero or is that just a lower bound placeholder?
     *    A: Yes, 0 is a valid value (constraint says 0 <= node.data). Zero-value houses still
     *       count as normal nodes for adjacency purposes; they just don't add to the sum.
     *
     * 2. Q: Is the tree guaranteed to be non-empty?
     *    A: Yes — constraint says node count is in [1, 500], so root is never null. I'll still add
     *       a defensive null check in the production version for robustness/API safety.
     *
     * 3. Q: Is the tree guaranteed to be a valid binary tree (no cycles, single parent per node)?
     *    A: Yes, stated explicitly: "each house has only one parent house."
     *
     * 4. Q: Do we need to return which houses were robbed, or just the maximum total?
     *    A: Just the maximum total sum (matches LeetCode 337's contract). If asked for the actual
     *       set, I'd extend the DP to backtrack a chosen/skipped flag per node — I'll mention this
     *       as a follow-up extension.
     *
     * 5. Q: Are there duplicate values across nodes, and does that matter?
     *    A: Duplicates are allowed and don't affect correctness — we operate on node identity via
     *       tree structure, not on value uniqueness, so this doesn't change the algorithm at all.
     *
     * 6. Q: Is this tree balanced, or could it be a degenerate skewed tree (essentially a linked
     *    list)?
     *    A: No balance guarantee. I should design for the worst case: a completely skewed tree of
     *       depth up to 500, which affects recursion depth / stack usage for any DFS-based
     *       approach, and it's exactly why my naive brute-force is dangerous in the worst case.
     *
     * 7. Q: Is thread-safety or concurrent access to the tree a concern?
     *    A: No — assume single-threaded, read-only traversal of an immutable tree for this
     *       problem. I'll note in follow-ups how I'd adapt if concurrent mutation were allowed.
     *
     * 8. Q: Should the solution be recursive or is an iterative solution preferred (e.g., to avoid
     *    stack overflow on deep trees)?
     *    A: Recursive (postorder DFS) is idiomatic and clean here since node count <= 500 keeps
     *       recursion depth bounded and safe. I'll mention an iterative postorder alternative as a
     *       follow-up if arbitrarily large/skewed trees were allowed.
     */

    /*
     * ============================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ============================================================================================
     *
     * Example 1 — Normal case:
     *          3
     *         / \
     *        2   3
     *         \   \
     *          3   1
     *   Optimal: rob nodes {3(root's left-child's child), 3(root's right-child's child), 1} ...
     *   actually the optimal set is {2's child "3", 3's child "1", and root "3"} is NOT valid
     *   since root(3) is adjacent to nothing we picked here — let's be precise:
     *   Rob {node(3) at root, node(3) leaf under 2, node(1) leaf under right 3} => not adjacent to
     *   each other (root's direct children are 2 and 3, neither robbed), sum = 3 + 3 + 1 = 7.
     *   Expected output: 7.
     *
     * Example 2 — Boundary / tie-breaking case (root inclusion is suboptimal):
     *            3
     *           / \
     *          4   5
     *         / \    \
     *        1   3    1
     *   Option A (rob root + grandchildren): 3 + 1 + 3 + 1 = 8
     *   Option B (rob root's two children instead): 4 + 5 = 9
     *   Max(8, 9) = 9. This demonstrates that "always rob the root" is NOT a safe greedy rule —
     *   the algorithm must genuinely compare both branches at every node.
     *   Expected output: 9.
     *
     * Example 3 — Minimal edge case (single node):
     *          5
     *   Only one house, nothing adjacent to skip. Expected output: 5.
     *
     * Example 4 — Edge case with an all-zero-value skewed chain (tests correctness, not just
     * happy path):
     *          0
     *           \
     *            0
     *             \
     *              0
     *   Expected output: 0 (every combination sums to zero; also validates we never crash on
     *   single-child chains / skewed shapes).
     */

    /*
     * ============================================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES (paradigm sweep)
     * ============================================================================================
     * Applicable paradigms, in the order I'd narrate them out loud:
     *
     *   - Brute force / naive recursion        -> APPLICABLE (Approach 1)
     *   - Hashing (memoization)                -> APPLICABLE (Approach 2)
     *   - Dynamic programming (tree DP)         -> APPLICABLE (Approach 2 & 3)
     *   - Tree traversal (postorder DFS)         -> APPLICABLE (all approaches; the traversal
     *                                              order itself is what makes DP correct here)
     *   - Divide & conquer                       -> APPLICABLE in spirit: Approach 3 splits the
     *                                              problem into independent left/right subtree
     *                                              subproblems and merges their results in O(1)
     *                                              at each node — this IS the D&C paradigm applied
     *                                              to a tree; I fold it into Approach 3 rather than
     *                                              listing it separately since it's the same code.
     *   - Greedy                                 -> NOT SAFE / NOT APPLICABLE AS A STANDALONE
     *                                              STRATEGY. A rule like "always rob alternating
     *                                              levels" or "always rob the root" fails — see
     *                                              Example 2, where robbing the root is strictly
     *                                              worse than skipping it. Any correct greedy-
     *                                              sounding rule secretly requires comparing both
     *                                              options at every node, which is just DP wearing
     *                                              a greedy costume — so I present it as DP, not
     *                                              greedy.
     *   - Sorting-based                          -> NOT APPLICABLE: there's no ordering over
     *                                              node values that helps; adjacency is defined
     *                                              by tree structure, not by value magnitude, so
     *                                              sorting destroys the structural information we
     *                                              need.
     *   - Two pointer / sliding window            -> NOT APPLICABLE: those techniques rely on a
     *                                              linear/contiguous index space (arrays/strings);
     *                                              a tree has no single linear order that preserves
     *                                              adjacency semantics.
     *   - Binary search                          -> NOT APPLICABLE: there's no monotonic
     *                                              predicate over a sorted search space to exploit
     *                                              here; we aren't searching for a threshold value.
     *   - Monotonic stack / deque                 -> NOT APPLICABLE: those solve "next greater/
     *                                              smaller element" style problems over sequences;
     *                                              there's no such relation being queried here.
     *   - Trie / segment tree / advanced structures -> NOT APPLICABLE: no string-prefix structure
     *                                              (trie) and no range-query workload (segment
     *                                              tree) exists in this problem.
     *
     * I'll now implement the two paradigms that matter (naive recursion, and DP/hashing/tree-
     * traversal — split into memoized top-down and optimal bottom-up) as three concrete
     * approaches.
     */

    /* ---------------------------------------------------------------------------------------- */
    /* Shared binary tree node definition used by every approach below.                            */
    /* ---------------------------------------------------------------------------------------- */
    static final class TreeNode {
        final int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    /*
     * ============================================================================================
     * APPROACH 1: Naive Recursion (Brute Force)
     * ============================================================================================
     * Core idea: For every node, the answer is the max of two choices:
     *   (a) Rob this node -> we may NOT rob its direct children, so we add the best result from
     *       its GRANDCHILDREN (node.left.left, node.left.right, node.right.left, node.right.right).
     *   (b) Don't rob this node -> we take the best independent result from node.left and
     *       node.right, each solved via the same two-choice recursion.
     * Data structure / paradigm: plain recursion, no memoization, over the tree.
     * The bug: computing choice (a) explicitly recurses into grandchildren, but choice (b) will
     * independently re-derive the same grandchildren results as part of solving node.left and
     * node.right from scratch. Every node's subtree gets solved repeatedly by multiple ancestors,
     * exactly like naive recursive Fibonacci — the classic "overlapping subproblems without
     * memoization" trap.
     *
     * Time Complexity: Exponential in the worst case, O(2^n) — each node's subtree can be
     * re-solved by every ancestor at even AND odd distances via the two branches, so work does
     * not collapse to linear/polynomial without memoization.
     * Space Complexity: O(h) auxiliary stack space for recursion depth (h = tree height), not
     * counting the exponential time blowup which is purely CPU work, not heap memory.
     *
     * Pros: Trivial to state and code correctly in under a minute; a great "warm-up" answer that
     *       shows you understand the problem's recursive structure before optimizing.
     * Cons: Exponential blowup makes it unusable beyond tiny trees (fails at scale well before
     *       n = 500 for adversarial/deep trees); repeats identical subtree computations.
     * When to use: Only as a stepping stone verbally/on a whiteboard to motivate memoization —
     *       never as a final answer, and never in production.
     */
    static final class Approach1NaiveRecursion {
        int rob(TreeNode root) {
            if (root == null) {
                return 0; // an empty subtree contributes nothing
            }

            // Choice A: rob this node, skip its direct children, recurse into grandchildren.
            int robThisNode = root.val;
            if (root.left != null) {
                robThisNode += rob(root.left.left) + rob(root.left.right);
            }
            if (root.right != null) {
                robThisNode += rob(root.right.left) + rob(root.right.right);
            }

            // Choice B: skip this node, independently solve both direct children subtrees.
            int skipThisNode = rob(root.left) + rob(root.right);

            return Math.max(robThisNode, skipThisNode);
        }
    }

    /*
     * ============================================================================================
     * APPROACH 2: Top-Down Recursion + Memoization (Hashing-based tree DP)
     * ============================================================================================
     * Core idea: identical recursive structure to Approach 1, but we cache the result for each
     * TreeNode reference in a HashMap so that once a node's answer is computed, every future
     * ancestor that would have re-derived it instead gets an O(1) cache hit.
     * Data structure / paradigm: recursion + hashing (HashMap<TreeNode, Integer>), i.e., classic
     * "memoized top-down DP."
     *
     * Time Complexity: O(n) — each of the n nodes is fully computed exactly once; every
     * subsequent reference to that node's result is an O(1) map lookup.
     * Space Complexity: O(n) for the memo map entries, plus O(h) recursion stack space.
     *
     * Pros: Easy, minimal-diff fix over the brute force (just add a cache); correct and efficient;
     *       very natural to explain as "add memoization to the recursion I already had."
     * Cons: O(n) extra space for the map is unnecessary — the truly optimal approach (Approach 3)
     *       achieves O(n) time with only O(h) space by restructuring what each call returns.
     *       Hashing on object identity (TreeNode reference equality) also means this approach
     *       silently breaks if TreeNode ever gets a custom equals()/hashCode() based on value,
     *       since duplicate-valued distinct nodes could then collide in the map.
     * When to use: Great as an intermediate step in an interview to show you can spot and patch
     *       overlapping subproblems quickly; fine in production when code simplicity outweighs the
     *       modest extra memory cost.
     */
    static final class Approach2MemoizedRecursion {
        private final Map<TreeNode, Integer> memo = new HashMap<>();

        int rob(TreeNode root) {
            if (root == null) {
                return 0;
            }
            if (memo.containsKey(root)) {
                return memo.get(root); // previously solved by an earlier ancestor call
            }

            int robThisNode = root.val;
            if (root.left != null) {
                robThisNode += rob(root.left.left) + rob(root.left.right);
            }
            if (root.right != null) {
                robThisNode += rob(root.right.left) + rob(root.right.right);
            }

            int skipThisNode = rob(root.left) + rob(root.right);

            int best = Math.max(robThisNode, skipThisNode);
            memo.put(root, best);
            return best;
        }
    }

    /*
     * ============================================================================================
     * APPROACH 3 (OPTIMAL): Postorder DFS returning a (rob, skip) pair per node
     * ============================================================================================
     * Core idea: instead of asking "what's the best answer for this subtree" as a single number
     * (which forces us to re-derive grandchildren separately from children), have every recursive
     * call return BOTH quantities at once for its own subtree:
     *   - robThisNode : best total if we DO rob the current node
     *   - skipThisNode: best total if we do NOT rob the current node
     * Then at the parent:
     *   - parent.robThisNode  = parent.val + child.skipThisNode (for each child) — because if we
     *     rob the parent, we are forced to skip each direct child.
     *   - parent.skipThisNode = max(child.robThisNode, child.skipThisNode) (for each child) —
     *     because if we skip the parent, each child is independently free to be robbed or not,
     *     whichever is better.
     * This is a bottom-up, postorder tree DP: we fully solve both children before combining their
     * results into the parent's pair — each node is visited exactly once, and no node's result is
     * ever recomputed. It is simultaneously an instance of divide-and-conquer (solve left and
     * right independently, merge in O(1)).
     * Data structure / paradigm: tree DP via postorder DFS, merged via a small fixed-size pair
     * (I use a Java record for clarity).
     *
     * Time Complexity: O(n) — exactly one constant-time merge step per node, visited once each.
     * Space Complexity: O(h) — only recursion call-stack space; h is tree height (worst case O(n)
     * for a totally skewed tree, O(log n) for a balanced tree). No auxiliary map needed at all.
     *
     * Pros: Optimal in both time and space among all correct approaches; no hashing/object-
     *       identity concerns; the cleanest final answer to present as "production" code.
     * Cons: Slightly less obvious to invent from scratch than "just memoize the naive version" —
     *       it requires realizing you should return two values instead of one. Still very
     *       standard tree-DP once you've seen the pattern.
     * When to use: This is what I'd ship. Always prefer this once you've walked the interviewer
     *       through Approaches 1 and 2 to demonstrate the optimization journey.
     */
    static final class Approach3OptimalPairDP {

        /**
         * Pair of best achievable sums for a subtree rooted at some node:
         * robRoot  — maximum sum if the subtree's root IS robbed.
         * skipRoot — maximum sum if the subtree's root is NOT robbed.
         * A Java 21 record gives us an immutable, self-documenting value type for free.
         */
        record RobResult(int robRoot, int skipRoot) {
            int best() {
                return Math.max(robRoot, skipRoot);
            }
        }

        int rob(TreeNode root) {
            RobResult result = solve(root);
            return result.best();
        }

        private RobResult solve(TreeNode node) {
            if (node == null) {
                // An empty subtree contributes 0 whether or not its (non-existent) root is robbed.
                return new RobResult(0, 0);
            }

            RobResult leftResult = solve(node.left);
            RobResult rightResult = solve(node.right);

            // If we rob this node, both children must be skipped.
            int robThisNode = node.val + leftResult.skipRoot() + rightResult.skipRoot();

            // If we skip this node, each child independently picks its own best option.
            int skipThisNode = leftResult.best() + rightResult.best();

            return new RobResult(robThisNode, skipThisNode);
        }
    }

    /*
     * ============================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ============================================================================================
     * Approach                          | Time      | Space   | Best For                          | Limitations
     * ----------------------------------|-----------|---------|-----------------------------------|--------------------------------------------
     * 1. Naive Recursion                | O(2^n)    | O(h)    | Warm-up / motivating memoization  | Exponential blowup, unusable at real scale
     * 2. Top-Down Memoized Recursion    | O(n)      | O(n)+O(h)| Quick correct fix over Approach 1 | Extra map memory; identity-hashing caveat
     * 3. Bottom-Up Pair DP (optimal)     | O(n)      | O(h)    | Final production answer           | Slightly less obvious to derive from scratch
     */

    /*
     * ============================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ============================================================================================
     * I would present Approach 1 verbally in about 30 seconds purely to establish the recursive
     * structure and immediately flag the overlapping-subproblem issue, then say "let's fix that"
     * and go straight to coding Approach 3 (the pair-returning postorder DP) as my submitted
     * solution — skipping writing out Approach 2 in full unless the interviewer specifically
     * wants to see the memoization intermediate step. Rationale:
     *   - Clarity: the (rob, skip) pair formulation is easy to explain and leaves no ambiguity
     *     about correctness once the recurrence is stated.
     *   - Coding speed: it's a short, self-contained postorder DFS — fast to write cleanly under
     *     time pressure, with no auxiliary data structure to manage.
     *   - Interviewer expectations: for a "medium" tree-DP problem like this, interviewers expect
     *     candidates to reach the O(n) time / O(h) space solution, not stop at memoized O(n) extra
     *     space — going the extra step signals strong mastery of tree DP.
     *   - Optimality: O(n) time and O(h) space is asymptotically optimal — we must look at every
     *     node at least once, and O(h) space is unavoidable for any recursive tree traversal.
     */

    /*
     * ============================================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ============================================================================================
     * (Same algorithm as Approach 3 above, restated here as a standalone, defensively-coded,
     * fully-Javadoc'd production class, as I would hand off in a real codebase.)
     */
    static final class HouseRobberSolver {

        /**
         * Computes the maximum sum of node values that can be "robbed" from a binary tree such
         * that no two robbed nodes are directly connected by a parent-child edge.
         *
         * <p>Algorithm: postorder DFS tree DP. Each recursive call returns a pair
         * {@code (robRoot, skipRoot)} describing, for the subtree rooted at the current node, the
         * best achievable sum if that node is robbed versus if it is not. Parents combine their
         * children's pairs in O(1) time, so the overall algorithm runs in O(n) time using O(h)
         * auxiliary stack space, where n is the node count and h is the tree height.
         *
         * @param root the root of the binary tree; per problem constraints this is guaranteed
         *             non-null (tree has at least 1 node), but {@code null} is handled
         *             defensively and simply yields 0.
         * @return the maximum robbable sum; always &gt;= 0 since node values are non-negative.
         */
        int maxRob(TreeNode root) {
            RobPair result = solve(root);
            return Math.max(result.robRoot, result.skipRoot);
        }

        /**
         * Immutable pair describing the best sums for a subtree when its root is robbed versus
         * skipped. Kept as a private static nested class (rather than a top-level record) to keep
         * this production class fully self-contained.
         */
        private static final class RobPair {
            final int robRoot;
            final int skipRoot;

            RobPair(int robRoot, int skipRoot) {
                this.robRoot = robRoot;
                this.skipRoot = skipRoot;
            }
        }

        private RobPair solve(TreeNode node) {
            // Base case: an absent child contributes nothing either way.
            if (node == null) {
                return new RobPair(0, 0);
            }

            // Postorder: fully resolve children before combining into this node's pair.
            RobPair leftPair = solve(node.left);
            RobPair rightPair = solve(node.right);

            // Robbing this node forces both direct children to be skipped.
            int robThisNode = node.val + leftPair.skipRoot + rightPair.skipRoot;

            // Skipping this node lets each child independently choose its own best option.
            int skipThisNode = Math.max(leftPair.robRoot, leftPair.skipRoot)
                    + Math.max(rightPair.robRoot, rightPair.skipRoot);

            return new RobPair(robThisNode, skipThisNode);
        }
    }

    /*
     * ============================================================================================
     * SECTION 10: DRY RUN / TRACE (Example 2 — the tie-breaking / boundary case)
     * ============================================================================================
     *            3
     *           / \
     *          4   5
     *         / \    \
     *        1   3    1
     *
     * Postorder evaluation order: node(1,left-left), node(3,left-right), node(4), node(1,right-
     * right), node(5), node(3,root).
     *
     * 1. solve(leaf "1" under node-4's left):   left=null->(0,0), right=null->(0,0)
     *      robThisNode  = 1 + 0 + 0 = 1
     *      skipThisNode = max(0,0) + max(0,0) = 0
     *      -> RobPair(rob=1, skip=0)
     *
     * 2. solve(leaf "3" under node-4's right):  left=null->(0,0), right=null->(0,0)
     *      robThisNode  = 3 + 0 + 0 = 3
     *      skipThisNode = 0
     *      -> RobPair(rob=3, skip=0)
     *
     * 3. solve(node "4"):  leftPair=(rob=1,skip=0) from step 1, rightPair=(rob=3,skip=0) from 2
     *      robThisNode  = 4 + skip(left)=0 + skip(right)=0            = 4
     *      skipThisNode = max(1,0) + max(3,0)                          = 1 + 3 = 4
     *      -> RobPair(rob=4, skip=4)
     *
     * 4. solve(leaf "1" under node-5's right): -> RobPair(rob=1, skip=0)   (same as step 1 logic)
     *
     * 5. solve(node "5"): left=null->(0,0), rightPair=(rob=1,skip=0) from step 4
     *      robThisNode  = 5 + skip(left)=0 + skip(right)=0            = 5
     *      skipThisNode = max(0,0) + max(1,0)                          = 0 + 1 = 1
     *      -> RobPair(rob=5, skip=1)
     *
     * 6. solve(root "3"): leftPair=(rob=4,skip=4) from step 3, rightPair=(rob=5,skip=1) from 5
     *      robThisNode  = 3 + skip(left)=4 + skip(right)=1             = 3 + 4 + 1 = 8
     *      skipThisNode = max(4,4) + max(5,1)                          = 4 + 5     = 9
     *      -> RobPair(rob=8, skip=9)
     *
     * Final answer = max(robThisNode=8, skipThisNode=9) = 9.
     * This matches the expected output and concretely shows why robbing the root (8) loses to
     * skipping it in favor of both of its children (9) — exactly the trap a naive "always rob the
     * root" greedy rule would fall into.
     */

    /*
     * ============================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ============================================================================================
     * - Approach 1 (naive recursion) establishes correctness of the recurrence but is exponential
     *   time due to unmemoized overlapping subproblems — never acceptable as a final answer.
     * - Approach 2 (top-down memoization) fixes the time complexity to O(n) at the cost of O(n)
     *   extra hashmap space, keyed on node identity.
     * - Approach 3 / production solver (bottom-up pair DP) is optimal: O(n) time, O(h) space, no
     *   hashing needed, and is what I'd submit as my final answer.
     * - Known assumptions/limitations of the final solution: assumes the tree is a genuine tree
     *   (no cycles, single parent per node, as guaranteed) and that node count (<=500) keeps
     *   recursion depth safely within default JVM stack limits even in the worst-case skewed
     *   shape; for arbitrarily large or adversarially deep trees an iterative postorder traversal
     *   with an explicit stack would be the safer production choice.
     */

    /*
     * ============================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ============================================================================================
     * 1. "What if the tree could have up to 10^6 nodes and be arbitrarily skewed?" -> discuss
     *    converting the recursive postorder DFS to an iterative version with an explicit stack
     *    (or a Morris-traversal-style O(1) extra space technique) to avoid stack overflow.
     * 2. "What if we also needed to return WHICH houses were robbed, not just the total?" ->
     *    extend RobPair to also store the chosen child decisions, then do a second top-down pass
     *    to reconstruct the actual set of robbed nodes.
     * 3. "What if the graph were a general graph instead of a tree (cycles allowed)?" -> this
     *    becomes the NP-hard Maximum Weight Independent Set problem in general graphs; tree DP no
     *    longer applies directly, and we'd need approximation algorithms or exploit special
     *    structure (e.g., bounded treewidth) for efficient exact solutions.
     * 4. "What if 'adjacent' meant distance <= 2 instead of direct parent-child edges only?" ->
     *    the DP state would need to expand from a 2-state pair (rob/skip) to track more history,
     *    e.g., whether a node's parent or grandparent was robbed.
     * 5. "How would you parallelize this for a very wide (bushy) tree?" -> since left and right
     *    subtree computations are fully independent, they can be solved concurrently (e.g., via
     *    ForkJoinPool / RecursiveTask) and merged once both complete, though for n <= 500 the
     *    overhead of parallelism would likely outweigh the benefit.
     * 6. "What if node values could be negative?" -> since robbing a negative node never helps
     *    (skipping is always at least as good), correctness is unaffected, but I'd double check
     *    that skipThisNode logic (max of rob/skip per child) still handles it — it does, since we
     *    always take the max, and would never choose to rob a strictly negative node.
     */

    /*
     * ============================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ============================================================================================
     * 1. Jumping straight to "always rob every other level" as a greedy shortcut — this fails
     *    (see Example 2 dry run: robbing the root nets 8, but skipping it nets 9), because the
     *    optimal choice at one node depends on optimal choices at BOTH children, not a fixed
     *    parity pattern.
     * 2. Forgetting that "rob this node" must add children's SKIP values, not their overall best
     *    values — mixing this up silently allows two directly-connected nodes to both be robbed,
     *    which violates the core constraint and produces an inflated, incorrect answer.
     * 3. Using node VALUE equality/hashing (e.g., a HashMap keyed by value or a custom equals())
     *    instead of node IDENTITY when memoizing in Approach 2 — since duplicate values are
     *    allowed across distinct nodes, value-based keys silently corrupt the memo table by
     *    conflating unrelated nodes.
     * 4. Off-by-one / null-handling slip-ups on single-child nodes — e.g., in the naive approach,
     *    forgetting the null checks before descending into node.left.left / node.left.right (or
     *    the mirrored right-side calls) causes a NullPointerException the moment a node has only
     *    one child instead of two.
     */

    /*
     * ============================================================================================
     * TEST HARNESS — cross-validates all three approaches against each other and against known
     * expected outputs for every example from Section 3.
     * ============================================================================================
     */
    public static void main(String[] args) {
        Approach1NaiveRecursion approach1 = new Approach1NaiveRecursion();
        Approach2MemoizedRecursion approach2 = new Approach2MemoizedRecursion();
        Approach3OptimalPairDP approach3 = new Approach3OptimalPairDP();
        HouseRobberSolver productionSolver = new HouseRobberSolver();

        // Example 1: normal case, expected 7.
        //          3
        //         / \
        //        2   3
        //         \   \
        //          3   1
        TreeNode example1 = new TreeNode(3,
                new TreeNode(2, null, new TreeNode(3)),
                new TreeNode(3, null, new TreeNode(1)));

        // Example 2: tie-breaking / boundary case, expected 9.
        //            3
        //           / \
        //          4   5
        //         / \    \
        //        1   3    1
        TreeNode example2 = new TreeNode(3,
                new TreeNode(4, new TreeNode(1), new TreeNode(3)),
                new TreeNode(5, null, new TreeNode(1)));

        // Example 3: minimal single-node case, expected 5.
        TreeNode example3 = new TreeNode(5);

        // Example 4: all-zero skewed chain, expected 0.
        TreeNode example4 = new TreeNode(0, null, new TreeNode(0, null, new TreeNode(0)));

        TreeNode[] examples = { example1, example2, example3, example4 };
        int[] expected = { 7, 9, 5, 0 };

        for (int index = 0; index < examples.length; index++) {
            TreeNode tree = examples[index];

            int result1 = approach1.rob(tree);
            int result2 = approach2.rob(tree);
            int result3 = approach3.rob(tree);
            int resultProduction = productionSolver.maxRob(tree);

            System.out.printf(
                    "Example %d -> expected=%d | naive=%d | memoized=%d | optimalPairDP=%d | production=%d%n",
                    index + 1, expected[index], result1, result2, result3, resultProduction);

            if (result1 != expected[index] || result2 != expected[index]
                    || result3 != expected[index] || resultProduction != expected[index]) {
                throw new AssertionError("Mismatch detected on example " + (index + 1));
            }
        }

        System.out.println("All approaches agree with expected outputs on all examples.");

        // Lightweight randomized stress test: build random small trees, confirm all four
        // implementations agree with each other (cross-validation without a separate oracle,
        // since Approach 1 itself already serves as the brute-force oracle here).
        java.util.Random random = new java.util.Random(42);
        for (int trial = 0; trial < 200; trial++) {
            TreeNode randomTree = buildRandomTree(random, 10, 0.6);
            int naive = approach1.rob(randomTree);
            int memoized = new Approach2MemoizedRecursion().rob(randomTree);
            int optimal = approach3.rob(randomTree);
            int production = productionSolver.maxRob(randomTree);

            if (naive != memoized || naive != optimal || naive != production) {
                throw new AssertionError("Stress test disagreement on trial " + trial);
            }
        }
        System.out.println("Randomized stress test (200 trials): all approaches agree.");
    }

    /** Builds a small random binary tree for stress testing; values in [0, 10]. */
    private static TreeNode buildRandomTree(java.util.Random random, int maxNodesRemaining,
            double childProbability) {
        if (maxNodesRemaining <= 0 || random.nextDouble() > childProbability) {
            return null;
        }
        TreeNode node = new TreeNode(random.nextInt(11));
        node.left = buildRandomTree(random, maxNodesRemaining - 1, childProbability * 0.9);
        node.right = buildRandomTree(random, maxNodesRemaining - 1, childProbability * 0.9);
        return node;
    }
}

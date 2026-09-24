import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * STUDY NOTE: PATH SUM III
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Node values can reach 10^9, meaning a path of just three nodes can exceed the 32-bit 
 *    integer limit. Should I use a 64-bit `long` to track the running sum?" 
 *   (Crucial: Identifies a silent overflow trap in the constraints).
 * - "Do paths have to start at the root and end at a leaf?" 
 *   (The prompt says no, but confirming ensures we don't mistakenly use the `Path Sum I` base cases).
 * - "Are negative values present in the tree?" 
 *   (Yes, down to -10^9. This means we cannot stop searching early when a sum exceeds the target, 
 *    as a negative value could bring it back down to the target later in the path).
 * - "Is the tree structure static, or are we processing multiple targets concurrently?" 
 *   (If static and sequential, we can mutate a shared HashMap and backtrack. If concurrent, 
 *    we'd need to clone the state or use ThreadLocal maps).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We are looking for sub-paths. A valid path can start at Node C and end at Node F. 
 * The bottleneck is finding these internal sub-paths without initiating a brand-new, 
 * full-depth traversal starting from every single node in the tree.
 *
 * Approach 1: Double DFS (The Brute Force)
 * What I'd naturally try: I'd write a helper function `countPaths(node, target)` that checks 
 * all paths starting strictly from `node`. Then, I'd use a main DFS to visit every node in 
 * the tree, calling `countPaths` on each one as if it were the new root.
 * Why it works: It exhaustively tests every possible starting point and every possible downward path.
 * Why it's too slow/costly: Massive duplicate work. When we start at Root(A) and go to B then C, 
 * we traverse C. Later, the main DFS moves to B, and starts a new path search that traverses C again.
 * - Time Complexity: O(N^2) worst case on a skewed tree (a straight line). O(N log N) on a perfectly 
 *   balanced tree because the work halves at each level.
 * - Space Complexity: O(N) — because the dual recursion stacks can go N levels deep.
 * 
 * Approach 2: Prefix Sum with HashMap (The Optimal Choice)
 * What I'd naturally try: In 1D arrays, the "Subarray Sum Equals K" problem is solved efficiently 
 * by keeping a running total (prefix sum) from the start. If I am at index 5 with a sum of 20, 
 * and my target is 8, I just look backwards: "Have I ever seen a prefix sum of 12?" If yes, 
 * the sub-array between that point and here sums to exactly 8 (20 - 12 = 8). I can apply 
 * this exact 1D logic to the tree by treating every root-to-leaf path as an array!
 * Why it works: As we walk down, we record every running sum we've seen in a HashMap. We check 
 * if `currentSum - targetSum` exists in the map.
 * What property removes the bottleneck: The HashMap provides O(1) lookup to instantly know if a 
 * valid sub-path exists above us, completely eliminating the second DFS traversal.
 * - Time Complexity: O(N) — because we visit each node exactly once, doing O(1) map operations.
 * - Space Complexity: O(N) — because the HashMap stores up to N distinct sums, and the call stack 
 *   goes O(N) deep in the worst case.
 *
 * The Crucial Catch (Backtracking): 
 * A tree branches. If the left branch creates a prefix sum of 15, the right branch shouldn't 
 * "see" that sum, because they don't form a single downward path. We MUST remove our `currentSum` 
 * from the HashMap before returning up the call stack to the parent.
 *
 * The Interview Choice:
 * Always write Approach 2 (Prefix Sum with HashMap). It proves you can adapt a classic 1D array 
 * pattern to a graph structure, and demonstrates mastery of state backtracking.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Empty Tree (root == null) -> Return 0 immediately.
 * 2. Prefix Sum matches Target perfectly from the root -> Requires pre-loading the map with `(0, 1)`.
 * 3. Overflow paths -> `10^9 + 10^9` exceeds integer limits. Prefix sum MUST be a `long`.
 * 4. Zero values -> A node of 0 doesn't change the sum, meaning a single path can hit the target 
 *    multiple times (e.g., target=5, path: 5 -> 0 -> 0. This contains 3 valid paths). 
 *    The HashMap frequency counting naturally handles this.
 *
 *
 * 4. DRY RUN (Optimal Prefix Sum)
 * ----------------------------------------------------------------------------
 * Tree:
 *       10
 *      /  \
 *     5   -3
 *    / \    \
 *   3   2    11
 * 
 * Target: 8
 *
 * State Tracker: prefixMap = {0: 1} (Base case for paths starting directly at the root)
 *
 * 1. dfs(node=10, currentSum=0):
 *    currentSum = 0 + 10 = 10.
 *    Look for: 10 - 8 = 2. Map has no 2. count = 0.
 *    Update map: {0:1, 10:1}.
 *    Recurse Left (5)
 *
 * 2. dfs(node=5, currentSum=10):
 *    currentSum = 10 + 5 = 15.
 *    Look for: 15 - 8 = 7. Map has no 7. count = 0.
 *    Update map: {0:1, 10:1, 15:1}.
 *    Recurse Left (3)
 *
 * 3. dfs(node=3, currentSum=15):
 *    currentSum = 15 + 3 = 18.
 *    Look for: 18 - 8 = 10. Map HAS 10 (freq 1)! count = 0 + 1 = 1.
 *    Update map: {0:1, 10:1, 15:1, 18:1}.
 *    Children are null.
 *    Backtrack: decrement freq of 18. Map: {0:1, 10:1, 15:1}.
 *    Return 1 to parent (5).
 *
 * 4. Back at 5, recurse Right (2):
 *    dfs(node=2, currentSum=15):
 *    currentSum = 15 + 2 = 17.
 *    Look for: 17 - 8 = 9. Map has no 9. count = 0.
 *    Children are null.
 *    Backtrack: decrement freq of 17. Map: {0:1, 10:1, 15:1}.
 *    Return 0 to parent (5).
 * 
 * ...and so on. Total paths found = 3.
 *
 * Pitfalls: 
 * - Forgetting `map.put(0L, 1)`. If a path from the root EXACTLY matches the target, `currentSum - target` 
 *   equals 0. If 0 isn't in the map, it won't be counted.
 * - Forgetting to backtrack the map. If you don't decrement the frequency after processing a node's 
 *   children, parallel branches will cross-contaminate each other's prefix sums.
 *
 * Pattern Recognition: 
 * "When finding sub-arrays or sub-paths that sum to K -> Think Prefix Sum HashMap."
 * "When passing global state down a tree that diverges -> Think Backtracking."
 *
 * Interview Script:
 * "The naive approach of treating every node as a root takes O(N^2) time. I can optimize this to O(N) 
 * by realizing a downward tree path is just an array. I'll maintain a running prefix sum and a HashMap 
 * storing the frequencies of sums I've seen. At each node, if (currentSum - targetSum) exists in the map, 
 * it means a valid sub-path ends here. I must backtrack by removing the current sum from the map when 
 * returning to the parent, ensuring parallel branches don't see each other's paths."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if the paths could go UP and DOWN (e.g., child -> parent -> other child)?
 * A1: This completely breaks downward prefix sums. The problem becomes an undirected graph problem. 
 *     We would convert the tree to an adjacency list, then run BFS/DFS from every node, effectively 
 *     reverting to the O(N^2) brute force, or use Lowest Common Ancestor (LCA) algorithms.
 *
 * Q2: How would this change if we only wanted the MAXIMUM path sum between any two nodes?
 * A2: That is the "Binary Tree Maximum Path Sum" problem. We wouldn't use prefix sums at all. 
 *     Instead, we'd use a Bottom-Up Post-Order DFS, where each node returns its max downward 
 *     branch to its parent, while globally tracking `left + right + node` as a potential peak.
 */

public class PathSumIIIStudy {

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
     * APPROACH 2: OPTIMAL PREFIX SUM WITH HASHMAP
     * Time: O(N) | Space: O(N)
     */
    public int pathSum(TreeNode root, int targetSum) {
        if (root == null) return 0;
        
        // Map stores <Prefix Sum, Frequency>
        // Long is strictly required to prevent overflow when summing nodes up to 10^9
        Map<Long, Integer> prefixMap = new HashMap<>();
        
        // Base case: A prefix sum of 0 has occurred exactly 1 time (the "empty" path before the root).
        // Without this, paths starting directly from the root that equal targetSum will be missed.
        prefixMap.put(0L, 1);
        
        return dfsPrefixSum(root, 0L, targetSum, prefixMap);
    }

    private int dfsPrefixSum(TreeNode node, long currentSum, int targetSum, Map<Long, Integer> prefixMap) {
        if (node == null) {
            return 0;
        }

        // 1. Update the running sum for the current root-to-node path
        currentSum += node.val;

        // 2. Check if a sub-path exists that equals targetSum.
        // We do this BEFORE adding currentSum to the map, so a target of 0 doesn't falsely count itself.
        long sumToFind = currentSum - targetSum;
        int numValidPaths = prefixMap.getOrDefault(sumToFind, 0);

        // 3. Add the current sum to the map so our children can see it
        prefixMap.put(currentSum, prefixMap.getOrDefault(currentSum, 0) + 1);

        // 4. Recurse down to explore both subtrees
        numValidPaths += dfsPrefixSum(node.left, currentSum, targetSum, prefixMap);
        numValidPaths += dfsPrefixSum(node.right, currentSum, targetSum, prefixMap);

        // 5. BACKTRACK (Crucial Step)
        // Remove the current sum from the map before returning to the parent.
        // This ensures the right branch doesn't see prefix sums generated strictly in the left branch.
        prefixMap.put(currentSum, prefixMap.get(currentSum) - 1);

        return numValidPaths;
    }


    /**
     * APPROACH 1: BRUTE FORCE (Double DFS)
     * Time: O(N^2) | Space: O(N)
     * Included to demonstrate the naive thought process.
     */
    public int pathSumBruteForce(TreeNode root, int targetSum) {
        if (root == null) return 0;
        
        // Check paths starting at THIS root
        int pathsFromRoot = countPathsDown(root, targetSum);
        
        // Check paths starting in the left and right subtrees
        int pathsFromLeft = pathSumBruteForce(root.left, targetSum);
        int pathsFromRight = pathSumBruteForce(root.right, targetSum);
        
        return pathsFromRoot + pathsFromLeft + pathsFromRight;
    }

    private int countPathsDown(TreeNode node, long remainingTarget) {
        if (node == null) return 0;
        
        int pathCount = 0;
        if (node.val == remainingTarget) {
            pathCount++; 
            // We DO NOT return here, because negative numbers could bring the sum back to target later
        }
        
        pathCount += countPathsDown(node.left, remainingTarget - node.val);
        pathCount += countPathsDown(node.right, remainingTarget - node.val);
        
        return pathCount;
    }


    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        PathSumIIIStudy study = new PathSumIIIStudy();

        // Standard Case: The example from the DRY RUN
        //       10
        //      /  \
        //     5   -3
        //    / \    \
        //   3   2    11
        //  / \   \
        // 3  -2   1
        TreeNode standard = new TreeNode(10,
                new TreeNode(5,
                        new TreeNode(3, new TreeNode(3), new TreeNode(-2)),
                        new TreeNode(2, null, new TreeNode(1))
                ),
                new TreeNode(-3, null, new TreeNode(11))
        );
        
        System.out.println("Standard Tree (Optimal): " + study.pathSum(standard, 8)); // Expected: 3
        System.out.println("Standard Tree (Brute): " + study.pathSumBruteForce(standard, 8)); // Expected: 3

        // Edge Case 1: Target achieved multiple times on same downward path
        //       1
        //      /
        //     -1
        //    /
        //   1
        // Target: 1. 
        // Paths: (1), (1 -> -1 -> 1), (-1 -> 1), (1) -> Wait, let's trace exactly.
        // Node1(1) = 1. Node2(-1): Node1+Node2 = 0. Node3(1): Node1+2+3 = 1. Node3 alone = 1.
        // Valid paths: Node1, Node3, Node1->Node2->Node3, Node2->Node3
        TreeNode zerosTree = new TreeNode(1, new TreeNode(-1, new TreeNode(1), null), null);
        System.out.println("Zero-Sum Segments (Optimal): " + study.pathSum(zerosTree, 1)); // Expected: 4

        // Edge Case 2: Integer Overflow bounds
        // Constraints specify nodes up to 10^9. Target = 1000.
        // Path: 1,000,000,000 -> 1,000,000,000 -> -2,000,000,000 + 1000
        // The running sum reaches 2 billion, near the 32-bit ceiling, but could exceed it.
        TreeNode overflowTree = new TreeNode(1_000_000_000, 
                                    new TreeNode(1_000_000_000, 
                                        new TreeNode(-2_000_000_000 + 1000), 
                                    null), 
                                null);
        
        System.out.println("Overflow Tree (Optimal): " + study.pathSum(overflowTree, 1000)); // Expected: 1
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Prefix Sum + HashMap + DFS Backtracking.
 * Key Observation: A tree path from root-to-leaf is fundamentally a 1D array. We can adapt 
 *                  the O(N) Subarray Sum strategy by tracking a running total. To handle branching, 
 *                  we just backtrack (remove state) as the recursion unwinds.
 * Memorize: `map.put(currentSum, freq + 1); recurse(left); recurse(right); map.put(currentSum, freq - 1);`
 * Most Common Trap: Not using a `long` for the running sum. Node values can be `10^9`. 
 *                   Adding just three of them will silently overflow an `int`, returning wild bugs. 
 *                   Also, forgetting `map.put(0L, 1)` means paths starting precisely at the root fail.
 * One-Line Mental Trigger: "Path Sum III = Subarray sum logic down a tree, but backtrack the map."
 * ============================================================================
 */

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * PATH SUM III
 * ============================================================================
 *
 * Given a binary tree and a targetSum, count the number of DISTINCT
 * downward paths whose node values add up exactly to targetSum.
 *
 * A valid path:
 *
 *  1. Must contain at least one node.
 *  2. Must move downward only.
 *  3. Can start at ANY node.
 *  4. Can end at ANY node.
 *  5. Does NOT need to reach a leaf.
 *
 *
 * Example:
 *
 *              10
 *             /  \
 *            5   -3
 *           / \    \
 *          3   2   11
 *         / \   \
 *        3  -2   1
 *
 * targetSum = 8
 *
 * Valid paths include:
 *
 *      5 -> 3
 *      5 -> 2 -> 1
 *      -3 -> 11
 *
 * Answer = 3
 *
 *
 * ============================================================================
 * THE IMPORTANT OBSERVATION
 * ============================================================================
 *
 * The path does NOT have to start at the root.
 *
 * This means:
 *
 *              10
 *             /
 *            5
 *           /
 *          3
 *
 * target = 8
 *
 * The path:
 *
 *      5 -> 3
 *
 * is valid even though it does not start at 10.
 *
 *
 * So we need to consider paths starting from EVERY node.
 *
 *
 * ============================================================================
 * APPROACHES
 * ============================================================================
 *
 * 1. Brute Force
 *      Start a new DFS from every node.
 *
 *      Time:  O(N²) in the worst case
 *      Space: O(H)
 *
 * 2. Prefix Sum + HashMap
 *      During one DFS, remember prefix sums seen on the current path.
 *
 *      Time:  O(N)
 *      Space: O(H)
 *
 * This is the approach we want to understand deeply.
 *
 *
 * ============================================================================
 */
public class PathSumIII {

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
    // APPROACH 1: BRUTE FORCE
    // ========================================================================

    /**
     * Brute-force solution.
     *
     * ------------------------------------------------------------------------
     * IDEA
     * ------------------------------------------------------------------------
     *
     * Every valid path has some starting node.
     *
     * So:
     *
     *      1. Visit every node.
     *      2. Treat that node as the START of a path.
     *      3. From that node, explore every downward path.
     *      4. Count paths whose sum equals targetSum.
     *
     *
     * Example:
     *
     *              1
     *             / \
     *            2   3
     *
     * We consider:
     *
     * Start at 1:
     *      1
     *      1 -> 2
     *      1 -> 3
     *
     * Start at 2:
     *      2
     *
     * Start at 3:
     *      3
     *
     * This guarantees that paths starting at any node are considered.
     *
     *
     * ------------------------------------------------------------------------
     * TIME COMPLEXITY
     * ------------------------------------------------------------------------
     *
     * O(N²) worst case.
     *
     * Why?
     *
     * We potentially start a new downward traversal from every node.
     *
     * For a highly skewed tree:
     *
     *      1
     *       \
     *        2
     *         \
     *          3
     *           \
     *            4
     *             \
     *              ...
     *
     * Starting from:
     *
     * node 1 -> visits N nodes
     * node 2 -> visits N-1 nodes
     * node 3 -> visits N-2 nodes
     *
     * Total:
     *
     *      N + (N-1) + (N-2) + ...
     *
     *      = O(N²)
     *
     *
     * ------------------------------------------------------------------------
     * SPACE COMPLEXITY
     * ------------------------------------------------------------------------
     *
     * O(H) for recursion.
     *
     * H = height of the tree.
     */
    public int countPathsBruteForce(
            TreeNode root,
            long targetSum) {

        if (root == null) {
            return 0;
        }

        /*
         * Start a path from the current node.
         *
         * Then recursively do the same thing for the left and right
         * subtrees because paths may start there as well.
         */
        return countPathsStartingFromNode(root, targetSum)
                + countPathsBruteForce(root.left, targetSum)
                + countPathsBruteForce(root.right, targetSum);
    }

    /**
     * Counts paths that MUST start at currentNode.
     *
     * Notice that this method is different from countPathsBruteForce().
     *
     * This method does NOT choose a new starting point.
     *
     * It only extends paths downward from currentNode.
     */
    private int countPathsStartingFromNode(
            TreeNode currentNode,
            long remainingSum) {

        if (currentNode == null) {
            return 0;
        }

        /*
         * Include the current node in the path.
         */
        long newRemainingSum =
                remainingSum - currentNode.value;

        /*
         * If the remaining sum becomes zero, we found a valid path.
         *
         * IMPORTANT:
         *
         * We return 1 here, but we DO NOT stop.
         *
         * Why?
         *
         * Because node values can be negative or zero.
         *
         * Example:
         *
         *      target = 5
         *
         *      5
         *       \
         *       -2
         *         \
         *          2
         *
         * The path [5] is valid, but another path might become valid
         * later depending on the values.
         */
        int pathsFound = newRemainingSum == 0 ? 1 : 0;

        /*
         * Continue extending the path downward.
         */
        pathsFound += countPathsStartingFromNode(
                currentNode.left,
                newRemainingSum
        );

        pathsFound += countPathsStartingFromNode(
                currentNode.right,
                newRemainingSum
        );

        return pathsFound;
    }


    // ========================================================================
    // APPROACH 2: PREFIX SUM + HASHMAP
    // ========================================================================

    /**
     * OPTIMAL SOLUTION
     *
     * Uses:
     *
     *      Prefix Sum + HashMap
     *
     * ------------------------------------------------------------------------
     * THE CORE IDEA
     * ------------------------------------------------------------------------
     *
     * Suppose the current root-to-node path has:
     *
     *      prefixSum = 18
     *
     * and:
     *
     *      targetSum = 8
     *
     * We want a path ending at the current node whose sum is 8.
     *
     * If some earlier prefix had sum:
     *
     *      10
     *
     * then:
     *
     *      18 - 10 = 8
     *
     * Therefore:
     *
     *      currentPrefixSum - previousPrefixSum = targetSum
     *
     * Rearranging:
     *
     *      previousPrefixSum =
     *          currentPrefixSum - targetSum
     *
     *
     * So at every node:
     *
     *      1. Calculate current prefix sum.
     *      2. Look for:
     *
     *             currentPrefixSum - targetSum
     *
     *         in a HashMap.
     *
     *      3. Every occurrence represents one valid path ending
     *         at the current node.
     *
     *
     * =========================================================================
     * WHY DOES THIS HANDLE PATHS THAT START ANYWHERE?
     * =========================================================================
     *
     * Consider:
     *
     *              10
     *             /
     *            5
     *           /
     *          3
     *
     * targetSum = 8
     *
     * Root-to-current prefix sums:
     *
     *      before 10 -> 0
     *      at 10     -> 10
     *      at 5      -> 15
     *      at 3      -> 18
     *
     * At node 3:
     *
     *      currentPrefix = 18
     *
     *      target = 8
     *
     * We need:
     *
     *      18 - 8 = 10
     *
     * Prefix sum 10 occurred at node 10.
     *
     * Therefore:
     *
     *      18 - 10 = 8
     *
     * which represents:
     *
     *      5 -> 3
     *
     * Notice that we effectively removed the prefix:
     *
     *      10
     *
     * leaving:
     *
     *      5 -> 3
     *
     * This is exactly why prefix sums solve the
     * "path can start anywhere" requirement.
     *
     *
     * =========================================================================
     * WHY A HASHMAP?
     * =========================================================================
     *
     * We need to answer:
     *
     *      "Have I seen prefixSum X before?"
     *
     * A HashMap gives us approximately O(1) lookup.
     *
     * But we store a COUNT, not just a boolean.
     *
     * Why?
     *
     * Because the same prefix sum can occur multiple times.
     *
     * Example:
     *
     *      prefix sums:
     *
     *      0
     *      5
     *      5
     *      10
     *
     * If we need prefixSum 5:
     *
     * there are TWO possible starting positions.
     *
     * Therefore:
     *
     *      prefixSum -> frequency
     *
     *
     * =========================================================================
     * THE MOST IMPORTANT DETAIL
     * =========================================================================
     *
     * We initialize:
     *
     *      prefixSumFrequency.put(0L, 1);
     *
     * Why?
     *
     * Consider:
     *
     *      5
     *     /
     *    3
     *
     * target = 5
     *
     * At node 5:
     *
     *      currentPrefix = 5
     *
     * We need:
     *
     *      5 - 5 = 0
     *
     * There must be one prefix sum of 0 representing:
     *
     *      "before the root"
     *
     * This allows paths that START at the root to be counted naturally.
     *
     *
     * =========================================================================
     * BACKTRACKING
     * =========================================================================
     *
     * The HashMap must contain prefix sums only from the
     * CURRENT root-to-node path.
     *
     * Therefore:
     *
     *      Before going into child:
     *          add current prefix sum
     *
     *      After returning from child:
     *          remove current prefix sum
     *
     * This is the same idea as backtracking.
     *
     *
     * Time:  O(N)
     * Space: O(H)
     *
     * H = tree height.
     */
    public int countPathsPrefixSum(
            TreeNode root,
            long targetSum) {

        /*
         * prefixSumFrequency:
         *
         *      key   = prefix sum
         *      value = number of times this prefix sum has appeared
         *              on the current root-to-node path
         *
         * Start with prefix sum 0 occurring once.
         *
         * This represents the imaginary position BEFORE the root.
         */
        Map<Long, Integer> prefixSumFrequency = new HashMap<>();

        prefixSumFrequency.put(0L, 1);

        return countPathsUsingPrefixSum(
                root,
                0L,
                targetSum,
                prefixSumFrequency
        );
    }

    /**
     * DFS helper for the prefix-sum solution.
     */
    private int countPathsUsingPrefixSum(
            TreeNode currentNode,
            long currentPrefixSum,
            long targetSum,
            Map<Long, Integer> prefixSumFrequency) {

        if (currentNode == null) {
            return 0;
        }

        // ====================================================================
        // STEP 1: Extend the prefix sum with the current node.
        // ====================================================================

        currentPrefixSum += currentNode.value;

        // ====================================================================
        // STEP 2: Find how many valid paths END at currentNode.
        // ====================================================================

        /*
         * We know:
         *
         *      pathSum =
         *          currentPrefixSum - previousPrefixSum
         *
         * We want:
         *
         *      pathSum = targetSum
         *
         * Therefore:
         *
         *      previousPrefixSum =
         *          currentPrefixSum - targetSum
         */
        long requiredPreviousPrefixSum =
                currentPrefixSum - targetSum;

        /*
         * Every occurrence of requiredPreviousPrefixSum gives us
         * one different path ending at currentNode.
         */
        int pathsEndingHere =
                prefixSumFrequency.getOrDefault(
                        requiredPreviousPrefixSum,
                        0
                );

        // ====================================================================
        // STEP 3: Record the current prefix sum.
        // ====================================================================

        /*
         * Future nodes may use this prefix sum as their
         * "previous prefix".
         */
        prefixSumFrequency.merge(
                currentPrefixSum,
                1,
                Integer::sum
        );

        // ====================================================================
        // STEP 4: Explore left and right subtrees.
        // ====================================================================

        int pathsInLeftSubtree =
                countPathsUsingPrefixSum(
                        currentNode.left,
                        currentPrefixSum,
                        targetSum,
                        prefixSumFrequency
                );

        int pathsInRightSubtree =
                countPathsUsingPrefixSum(
                        currentNode.right,
                        currentPrefixSum,
                        targetSum,
                        prefixSumFrequency
                );

        // ====================================================================
        // STEP 5: BACKTRACK
        // ====================================================================

        /*
         * The current node is no longer part of the path when we return
         * to the parent.
         *
         * Therefore, remove its prefix sum.
         *
         * This is VERY important.
         *
         * Without this step, prefix sums from one branch could incorrectly
         * be used with nodes from another branch.
         *
         * Example:
         *
         *              1
         *             / \
         *            2   3
         *
         * A prefix from the left subtree must never be combined with
         * a node from the right subtree.
         */
        prefixSumFrequency.put(
                currentPrefixSum,
                prefixSumFrequency.get(currentPrefixSum) - 1
        );

        // ====================================================================
        // STEP 6: Return all paths discovered below this node.
        // ====================================================================

        return pathsEndingHere
                + pathsInLeftSubtree
                + pathsInRightSubtree;
    }


    // ========================================================================
    // TESTING
    // ========================================================================

    public static void main(String[] args) {

        PathSumIII solution = new PathSumIII();

        /*
         * Example tree:
         *
         *              10
         *             /  \
         *            5   -3
         *           / \    \
         *          3   2   11
         *         / \   \
         *        3  -2   1
         *
         * targetSum = 8
         *
         * Valid paths:
         *
         *      5 -> 3
         *      5 -> 2 -> 1
         *      -3 -> 11
         *
         * Answer = 3
         */
        TreeNode root =
                new TreeNode(
                        10,
                        new TreeNode(
                                5,
                                new TreeNode(
                                        3,
                                        new TreeNode(3),
                                        new TreeNode(-2)
                                ),
                                new TreeNode(
                                        2,
                                        null,
                                        new TreeNode(1)
                                )
                        ),
                        new TreeNode(
                                -3,
                                null,
                                new TreeNode(11)
                        )
                );

        long targetSum = 8;

        System.out.println(
                "Brute Force : "
                        + solution.countPathsBruteForce(
                                root,
                                targetSum
                        )
        );

        System.out.println(
                "Prefix Sum  : "
                        + solution.countPathsPrefixSum(
                                root,
                                targetSum
                        )
        );

        // Expected:
        //
        // Brute Force : 3
        // Prefix Sum  : 3
    }
}


/**
 * ============================================================================
 * INTERVIEW CHEAT SHEET
 * ============================================================================
 *
 * PROBLEM:
 *
 * Count downward paths whose sum == targetSum.
 *
 * Important:
 *
 *      Path can start at ANY node.
 *
 *
 * ============================================================================
 * BRUTE FORCE
 * ============================================================================
 *
 * For every node:
 *
 *      "What paths start from here?"
 *
 * DFS downward and count matching sums.
 *
 * Time:  O(N²)
 * Space: O(H)
 *
 *
 * ============================================================================
 * OPTIMAL: PREFIX SUM
 * ============================================================================
 *
 * At every node:
 *
 *      currentPrefixSum += node.value
 *
 * Need:
 *
 *      currentPrefixSum - previousPrefixSum = targetSum
 *
 * Therefore:
 *
 *      previousPrefixSum =
 *          currentPrefixSum - targetSum
 *
 * Ask HashMap:
 *
 *      "How many times have I seen this prefix sum?"
 *
 * Add that count to the answer.
 *
 *
 * ============================================================================
 * HASHMAP
 * ============================================================================
 *
 *      prefixSum -> frequency
 *
 * Initialize:
 *
 *      map.put(0L, 1)
 *
 * Why?
 *
 * Represents the prefix before the root.
 *
 *
 * ============================================================================
 * BACKTRACKING
 * ============================================================================
 *
 * Enter node:
 *
 *      add prefix sum
 *
 * Explore children.
 *
 * Leave node:
 *
 *      remove prefix sum
 *
 * This ensures the map represents ONLY the current
 * root-to-node path.
 *
 *
 * ============================================================================
 * COMPLEXITY
 * ============================================================================
 *
 * Prefix Sum solution:
 *
 *      Time:  O(N)
 *      Space: O(H)
 *
 * Balanced tree:
 *
 *      H = log N
 *
 * Skewed tree:
 *
 *      H = N
 *
 *
 * ============================================================================
 * ONE-LINE MEMORY TRIGGER
 * ============================================================================
 *
 * "Current prefix - target = previous prefix."
 *
 *
 * ============================================================================
 * INTERVIEW EXPLANATION
 * ============================================================================
 *
 * "I'll use prefix sums.
 *
 * For every node, I maintain the sum from the root of the current
 * DFS path to that node.
 *
 * If the current prefix sum is P, then a path ending at this node
 * has sum targetSum when there was an earlier prefix sum of
 * P - targetSum.
 *
 * I store prefix sums and their frequencies in a HashMap so I can
 * find those earlier prefixes in O(1) average time.
 *
 * I also initialize prefix sum 0 with frequency 1 so paths that
 * start at the root are handled naturally.
 *
 * Finally, because the map should represent only the current
 * root-to-node path, I remove the current prefix sum when
 * backtracking.
 *
 * Therefore the overall complexity is O(N) time and O(H) space."
 *
 * ============================================================================
 */

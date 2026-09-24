/**
 * All Possible Full Binary Trees - Merge Multiple BSTs
 * 
 * PROBLEM UNDERSTANDING:
 * Given n BSTs (each with ≤3 nodes), merge them into one valid BST by replacing
 * leaf nodes with matching root values.
 * 
 * CRITICAL INSIGHTS:
 * 
 * 1. MATCHING RULE:
 *    Leaf value in tree[i] must equal root value in tree[j]
 *    Replace leaf with entire tree[j]
 * 
 * 2. BST PROPERTY MUST BE MAINTAINED:
 *    After replacement, the merged tree must still be a valid BST
 *    This means the subtree being attached must fit in the valid range
 * 
 * 3. GRAPH PERSPECTIVE:
 *    This is a GRAPH PROBLEM disguised as a tree problem!
 *    - Nodes = BST roots
 *    - Edges = "leaf value matches root value"
 *    - Goal = Find Hamiltonian path (visit all nodes exactly once)
 *    - Start node = final root (has no incoming edges in valid solution)
 * 
 * 4. SMALL TREE CONSTRAINT:
 *    Each BST has ≤3 nodes → at most 2 leaves
 *    This limits branching factor and makes DFS feasible
 * 
 * Example:
 * trees = [[2,1], [3,2,5], [5,4]]
 * 
 * Tree 0:  2        Tree 1:    3        Tree 2:  5
 *         /                   / \               /
 *        1                   2   5             4
 * 
 * Step 1: Leaf 5 in tree[1] matches root of tree[2]
 *         Replace leaf 5 with tree[2]
 *         
 *         3
 *        / \
 *       2   5
 *          /
 *         4
 * 
 * Step 2: Leaf 2 in merged tree matches root of tree[0]
 *         Replace leaf 2 with tree[0]
 *         
 *         3
 *        / \
 *       2   5
 *      /   /
 *     1   4
 * 
 * Result: Valid BST!
 * 
 * TIME COMPLEXITY: O(n! * n) worst case - try all permutations
 * SPACE COMPLEXITY: O(n) for recursion and tracking
 */

import java.util.*;

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode(int val) {
        this.val = val;
    }
}

class CanMergeAllBSTs {
    
    /**
     * APPROACH 1: DFS WITH BACKTRACKING (OPTIMAL) ⭐⭐⭐
     * 
     * STRATEGY:
     * 1. Try each tree as the root of final BST
     * 2. For each choice, recursively merge matching leaves
     * 3. Validate merged BST at the end
     * 4. Backtrack if merge fails or validation fails
     * 
     * KEY OBSERVATIONS:
     * - Final root has no incoming edges (no other tree has leaf with its value)
     * - At most one valid final root exists
     * - Use DFS to explore all merge possibilities
     * 
     * OPTIMIZATION:
     * - Find potential roots first (roots with no incoming edges)
     * - Only try those as starting points
     * 
     * TIME: O(n! * n) worst case, but pruned heavily in practice
     * SPACE: O(n) for recursion and visited set
     */
    public TreeNode canMerge(List<TreeNode> trees) {
        if (trees == null || trees.isEmpty()) {
            return null;
        }
        
        if (trees.size() == 1) {
            // Single tree - just validate it
            return isValidBST(trees.get(0)) ? trees.get(0) : null;
        }
        
        // Build map: root value -> tree
        Map<Integer, TreeNode> rootMap = new HashMap<>();
        Set<Integer> leafValues = new HashSet<>();
        
        for (TreeNode tree : trees) {
            rootMap.put(tree.val, tree);
            
            // Collect leaf values
            if (tree.left != null) {
                if (tree.left.left == null && tree.left.right == null) {
                    leafValues.add(tree.left.val);
                }
            }
            if (tree.right != null) {
                if (tree.right.left == null && tree.right.right == null) {
                    leafValues.add(tree.right.val);
                }
            }
        }
        
        /**
         * FIND POTENTIAL ROOTS:
         * A tree can only be the final root if its root value
         * is NOT a leaf value in any other tree
         * 
         * Why? Because if it's a leaf value, it will be replaced
         * and won't be the final root!
         */
        for (TreeNode tree : trees) {
            if (!leafValues.contains(tree.val)) {
                // This could be the final root
                TreeNode result = tryMerge(tree, rootMap, new HashSet<>());
                
                if (result != null && isValidBST(result)) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Try to merge all trees starting from the given root
     * Returns merged tree if successful, null otherwise
     */
    private TreeNode tryMerge(TreeNode root, Map<Integer, TreeNode> rootMap, 
                              Set<Integer> used) {
        if (root == null) {
            return null;
        }
        
        // Mark this tree as used
        used.add(root.val);
        
        // Try to merge left leaf
        if (root.left != null) {
            if (isLeaf(root.left)) {
                int leafVal = root.left.val;
                
                // Check if there's a matching tree to merge
                if (rootMap.containsKey(leafVal) && !used.contains(leafVal)) {
                    TreeNode subtree = rootMap.get(leafVal);
                    
                    // Clone the subtree before merging
                    TreeNode clonedSubtree = cloneTree(subtree);
                    
                    // Try merging
                    Set<Integer> newUsed = new HashSet<>(used);
                    TreeNode merged = tryMerge(clonedSubtree, rootMap, newUsed);
                    
                    if (merged != null) {
                        root.left = merged;
                        used.addAll(newUsed);
                    }
                }
            } else {
                // Recursively process non-leaf
                root.left = tryMerge(root.left, rootMap, used);
            }
        }
        
        // Try to merge right leaf
        if (root.right != null) {
            if (isLeaf(root.right)) {
                int leafVal = root.right.val;
                
                if (rootMap.containsKey(leafVal) && !used.contains(leafVal)) {
                    TreeNode subtree = rootMap.get(leafVal);
                    TreeNode clonedSubtree = cloneTree(subtree);
                    
                    Set<Integer> newUsed = new HashSet<>(used);
                    TreeNode merged = tryMerge(clonedSubtree, rootMap, newUsed);
                    
                    if (merged != null) {
                        root.right = merged;
                        used.addAll(newUsed);
                    }
                }
            } else {
                root.right = tryMerge(root.right, rootMap, used);
            }
        }
        
        // Check if we've used all trees
        if (used.size() == rootMap.size()) {
            return root;
        }
        
        return null;
    }
    
    
    /**
     * APPROACH 2: GREEDY WITH VALIDATION (CLEANER) ⭐⭐
     * 
     * STRATEGY:
     * 1. Find the root (value not appearing as leaf)
     * 2. Build map of value -> tree
     * 3. DFS through final tree, replacing leaves with matching trees
     * 4. Validate final BST
     * 
     * This is cleaner but similar logic to approach 1
     * 
     * TIME: O(n²)
     * SPACE: O(n)
     */
    public TreeNode canMerge_Greedy(List<TreeNode> trees) {
        if (trees.size() == 1) {
            return isValidBST(trees.get(0)) ? trees.get(0) : null;
        }
        
        Map<Integer, TreeNode> treeMap = new HashMap<>();
        Set<Integer> nonRoots = new HashSet<>();
        
        // Build map and collect non-roots (leaf values)
        for (TreeNode tree : trees) {
            treeMap.put(tree.val, tree);
        }
        
        for (TreeNode tree : trees) {
            if (tree.left != null) nonRoots.add(tree.left.val);
            if (tree.right != null) nonRoots.add(tree.right.val);
        }
        
        // Find the root (appears as root but not as leaf)
        TreeNode root = null;
        for (TreeNode tree : trees) {
            if (!nonRoots.contains(tree.val)) {
                if (root != null) {
                    return null;  // Multiple possible roots
                }
                root = tree;
            }
        }
        
        if (root == null) {
            return null;  // No valid root found (circular dependency)
        }
        
        // Remove root from map
        treeMap.remove(root.val);
        
        // Merge all trees into root
        if (!merge(root, treeMap)) {
            return null;
        }
        
        // Check if all trees were used
        if (!treeMap.isEmpty()) {
            return null;
        }
        
        // Validate final BST
        return isValidBST(root) ? root : null;
    }
    
    private boolean merge(TreeNode node, Map<Integer, TreeNode> treeMap) {
        if (node == null) {
            return true;
        }
        
        // Process left child
        if (node.left != null) {
            if (treeMap.containsKey(node.left.val)) {
                TreeNode subtree = treeMap.remove(node.left.val);
                node.left = subtree;
            }
            if (!merge(node.left, treeMap)) {
                return false;
            }
        }
        
        // Process right child
        if (node.right != null) {
            if (treeMap.containsKey(node.right.val)) {
                TreeNode subtree = treeMap.remove(node.right.val);
                node.right = subtree;
            }
            if (!merge(node.right, treeMap)) {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * APPROACH 3: GRAPH-BASED (CONCEPTUAL)
     * 
     * STRATEGY:
     * Model as directed graph:
     * - Nodes = BST roots
     * - Edge i→j if leaf in tree[i] equals root of tree[j]
     * - Find node with in-degree 0 and out-degree leading to all others
     * 
     * This approach helps understand the problem structure
     * but is similar in implementation to approaches 1 and 2
     */
    
    
    /**
     * ==================== HELPER METHODS ====================
     */
    
    /**
     * Check if node is a leaf (no children)
     */
    private boolean isLeaf(TreeNode node) {
        return node != null && node.left == null && node.right == null;
    }
    
    /**
     * Clone a tree (deep copy)
     */
    private TreeNode cloneTree(TreeNode root) {
        if (root == null) {
            return null;
        }
        
        TreeNode newNode = new TreeNode(root.val);
        newNode.left = cloneTree(root.left);
        newNode.right = cloneTree(root.right);
        return newNode;
    }
    
    /**
     * Validate if tree is a valid BST
     * Critical for final verification!
     */
    private boolean isValidBST(TreeNode root) {
        return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    private boolean validate(TreeNode node, long min, long max) {
        if (node == null) {
            return true;
        }
        
        if (node.val <= min || node.val >= max) {
            return false;
        }
        
        return validate(node.left, min, node.val) &&
               validate(node.right, node.val, max);
    }
    
    /**
     * Count total nodes in tree
     */
    private int countNodes(TreeNode root) {
        if (root == null) {
            return 0;
        }
        return 1 + countNodes(root.left) + countNodes(root.right);
    }
    
    /**
     * Get all leaf values in a tree
     */
    private void getLeaves(TreeNode node, List<Integer> leaves) {
        if (node == null) {
            return;
        }
        
        if (isLeaf(node)) {
            leaves.add(node.val);
            return;
        }
        
        getLeaves(node.left, leaves);
        getLeaves(node.right, leaves);
    }
    
    
    /**
     * ==================== VISUAL WALKTHROUGH ====================
     * 
     * Example: trees = [[2,1], [3,2,5], [5,4]]
     * 
     * Step 0: Initial State
     * 
     * Tree 0 (root=2):     Tree 1 (root=3):     Tree 2 (root=5):
     *      2                    3                     5
     *     /                    / \                   /
     *    1                    2   5                 4
     *    ↑                    ↑   ↑                 ↑
     *  leaf                 leaf leaf             leaf
     * 
     * Analysis:
     * - Leaf values: {1, 2, 5, 4}
     * - Root values: {2, 3, 5}
     * - Value 3 is NOT a leaf → potential final root ✓
     * - Value 2 IS a leaf in tree[1] → not final root
     * - Value 5 IS a leaf in tree[1] → not final root
     * 
     * Start with tree[1] (root=3)
     * 
     * ----------------------------------------
     * 
     * Step 1: Process tree[1]
     * Current:     3
     *             / \
     *            2   5
     * 
     * Left leaf = 2 → matches tree[0] root
     * Right leaf = 5 → matches tree[2] root
     * 
     * ----------------------------------------
     * 
     * Step 2: Replace left leaf (2) with tree[0]
     * 
     * Before:      3           After:       3
     *             / \                      / \
     *            2   5                    2   5
     *                                    /
     *                                   1
     * 
     * Tree[0] merged ✓
     * 
     * ----------------------------------------
     * 
     * Step 3: Replace right leaf (5) with tree[2]
     * 
     * Before:      3           After:       3
     *             / \                      / \
     *            2   5                    2   5
     *           /                        /   /
     *          1                        1   4
     * 
     * Tree[2] merged ✓
     * 
     * ----------------------------------------
     * 
     * Step 4: Validation
     * 
     * Final tree:      3
     *                 / \
     *                2   5
     *               /   /
     *              1   4
     * 
     * Inorder: [1, 2, 3, 4, 5] ✓ Strictly increasing
     * 
     * BST Properties:
     * - 1 < 2 < 3 ✓
     * - 4 < 5 ✓
     * - Left subtree (1,2) < 3 ✓
     * - Right subtree (4,5) > 3 ✓
     * 
     * Valid BST! Return root (3)
     * 
     * ----------------------------------------
     * 
     * IMPOSSIBLE CASE:
     * 
     * trees = [[2,1,3], [3,4]]
     * 
     * Tree 0:   2          Tree 1:   3
     *          / \                     \
     *         1   3                     4
     * 
     * Leaf value 3 matches tree[1] root
     * Replace leaf 3 with tree[1]:
     * 
     *      2
     *     / \
     *    1   3
     *         \
     *          4
     * 
     * Check BST property:
     * - Right subtree has values [3, 4]
     * - But 3 is NOT > 2! ✗
     * 
     * Invalid BST → return null
     */
    
    
    /**
     * ==================== COMPLEXITY ANALYSIS ====================
     * 
     * TIME COMPLEXITY:
     * 
     * Best Case: O(n)
     * - Find root: O(n)
     * - Merge operation: O(n) visiting each tree once
     * - Validation: O(n)
     * 
     * Worst Case: O(n!)
     * - If we try all permutations of merge orders
     * - Each tree has ≤2 leaves → at most 2 choices per level
     * - With backtracking: potentially exponential
     * 
     * Practical: O(n²)
     * - With good pruning (finding root first)
     * - Most merges fail quickly due to BST constraint
     * 
     * SPACE COMPLEXITY: O(n)
     * - Recursion depth: O(n)
     * - Hash maps and sets: O(n)
     * - Cloned trees during exploration: O(n)
     * 
     * OPTIMIZATION NOTES:
     * - Finding the root first eliminates most invalid paths
     * - BST validation provides early termination
     * - Small tree constraint (≤3 nodes) limits branching
     */
    
    
    /**
     * ==================== INTERVIEW STRATEGY GUIDE ====================
     * 
     * STEP 1: CLARIFY (2-3 minutes)
     * Questions to ask:
     * - Each tree has at most 3 nodes? (Yes)
     * - Values are unique across all trees? (Not necessarily within, but roots are)
     * - Must use all trees? (Yes, n-1 operations for n trees)
     * - Can same tree be used multiple times? (No)
     * 
     * STEP 2: IDENTIFY PATTERN (3-4 minutes)
     * "This is a graph problem disguised as a tree problem:
     * - Each BST is a node in the graph
     * - Edge exists if a leaf matches another root
     * - We need to find a path visiting all nodes exactly once
     * - The starting node (final root) has no incoming edges"
     * 
     * STEP 3: EXPLAIN STRATEGY (2-3 minutes)
     * "My approach:
     * 1. Find potential roots (values that aren't leaves in other trees)
     * 2. Try merging from each potential root
     * 3. Replace leaves with matching trees recursively
     * 4. Validate the final BST
     * 5. Return if valid, null otherwise"
     * 
     * STEP 4: EDGE CASES (2 minutes)
     * - Single tree → just validate it
     * - No valid root → circular dependency
     * - Multiple potential roots → try each
     * - Merge creates invalid BST → reject
     * 
     * STEP 5: CODE (10-12 minutes)
     * Write greedy approach (cleaner)
     * 
     * STEP 6: TEST (3-4 minutes)
     * Test with given example
     * Test impossible case
     * 
     * STEP 7: OPTIMIZE (remaining time)
     * Discuss graph perspective
     * Mention backtracking approach
     */
    
    
    /**
     * ==================== COMMON MISTAKES ====================
     */
    
    // ❌ MISTAKE 1: Not validating final BST
    public TreeNode wrong1(List<TreeNode> trees) {
        // ... merge logic ...
        return root;  // ❌ Didn't validate!
        
        // Even if all trees merge, might not be valid BST
    }
    
    // ❌ MISTAKE 2: Modifying original trees
    public TreeNode wrong2(List<TreeNode> trees) {
        // Directly modifying trees in list
        trees.get(0).left = trees.get(1);
        
        // ❌ Problem: can't backtrack if this fails
        // Should clone trees before modification
    }
    
    // ❌ MISTAKE 3: Not checking if all trees used
    public TreeNode wrong3(List<TreeNode> trees) {
        TreeNode root = trees.get(0);
        // ... some merging ...
        return root;
        
        // ❌ Might return even if some trees weren't merged
        // Must verify all n trees were used
    }
    
    // ❌ MISTAKE 4: Assuming unique root
    public TreeNode wrong4(List<TreeNode> trees) {
        TreeNode root = null;
        for (TreeNode tree : trees) {
            if (/* some condition */) {
                root = tree;
                break;  // ❌ Found one, assumed it's the only one
            }
        }
        // Should check for multiple possible roots
    }
    
    
    /**
     * ==================== UTILITY FOR TESTING ====================
     */
    
    // Build tree from array (null represents missing nodes)
    public TreeNode buildTree(Integer[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null) {
            return null;
        }
        
        TreeNode root = new TreeNode(arr[0]);
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        int i = 1;
        while (!queue.isEmpty() && i < arr.length) {
            TreeNode node = queue.poll();
            
            if (i < arr.length && arr[i] != null) {
                node.left = new TreeNode(arr[i]);
                queue.offer(node.left);
            }
            i++;
            
            if (i < arr.length && arr[i] != null) {
                node.right = new TreeNode(arr[i]);
                queue.offer(node.right);
            }
            i++;
        }
        
        return root;
    }
    
    // Print tree
    public void printTree(TreeNode root, String prefix, boolean isLeft) {
        if (root == null) return;
        
        System.out.println(prefix + (isLeft ? "├── " : "└── ") + root.val);
        
        if (root.left != null || root.right != null) {
            if (root.left != null) {
                printTree(root.left, prefix + (isLeft ? "│   " : "    "), true);
            } else {
                System.out.println(prefix + (isLeft ? "│   " : "    ") + "├── null");
            }
            
            if (root.right != null) {
                printTree(root.right, prefix + (isLeft ? "│   " : "    "), false);
            } else {
                System.out.println(prefix + (isLeft ? "│   " : "    ") + "└── null");
            }
        }
    }
    
    
    /**
     * ==================== TEST CASES ====================
     */
    public static void main(String[] args) {
        CanMergeAllBSTs solution = new CanMergeAllBSTs();
        
        System.out.println("=== Test 1: Valid Merge ===");
        List<TreeNode> trees1 = new ArrayList<>();
        trees1.add(solution.buildTree(new Integer[]{2, 1}));
        trees1.add(solution.buildTree(new Integer[]{3, 2, 5}));
        trees1.add(solution.buildTree(new Integer[]{5, 4}));
        
        System.out.println("Input trees:");
        for (int i = 0; i < trees1.size(); i++) {
            System.out.println("Tree " + i + ":");
            solution.printTree(trees1.get(i), "", false);
        }
        
        TreeNode result1 = solution.canMerge_Greedy(trees1);
        System.out.println("\nResult:");
        if (result1 != null) {
            solution.printTree(result1, "", false);
            System.out.println("Valid BST: " + solution.isValidBST(result1));
        } else {
            System.out.println("null - Cannot merge");
        }
        
        System.out.println("\n=== Test 2: Invalid Merge (BST violation) ===");
        List<TreeNode> trees2 = new ArrayList<>();
        trees2.add(solution.buildTree(new Integer[]{5, 3, 8}));
        trees2.add(solution.buildTree(new Integer[]{3, 2, 6}));
        
        TreeNode result2 = solution.canMerge_Greedy(trees2);
        System.out.println("Result: " + (result2 != null ? "Valid tree" : "null - Cannot merge"));
        
        System.out.println("\n=== Test 3: Single Tree ===");
        List<TreeNode> trees3 = new ArrayList<>();
        trees3.add(solution.buildTree(new Integer[]{2, 1, 3}));
        
        TreeNode result3 = solution.canMerge_Greedy(trees3);
        System.out.println("Result:");
        if (result3 != null) {
            solution.printTree(result3, "", false);
        }
        
        System.out.println("\n=== Test 4: Impossible (Circular) ===");
        List<TreeNode> trees4 = new ArrayList<>();
        trees4.add(solution.buildTree(new Integer[]{2, 1, 3}));
        trees4.add(solution.buildTree(new Integer[]{3, 2}));
        
        TreeNode result4 = solution.canMerge_Greedy(trees4);
        System.out.println("Result: " + (result4 != null ? "Valid tree" : "null - Circular dependency"));
    }
}

/**
 * ==================== KEY TAKEAWAYS ====================
 * 
 * 1. **GRAPH PERSPECTIVE**: Recognize graph problem in tree disguise
 * 
 * 2. **FIND THE ROOT**: Value not appearing as leaf is the answer
 * 
 * 3. **VALIDATE FINAL RESULT**: Merge might succeed but create invalid BST
 * 
 * 4. **SMALL CONSTRAINT**: ≤3 nodes per tree limits complexity
 * 
 * 5. **USE ALL TREES**: Must verify n-1 merges completed
 * 
 * 6. **BST PROPERTY**: Crucial - merged subtrees must fit valid range
 * 
 * INTERVIEW TIPS:
 * - Start by explaining graph perspective
 * - Identify root finding as key optimization
 * - Don't forget final BST validation
 * - Handle edge cases (single tree, circular deps)
 * 
 * RELATED PROBLEMS:
 * - LC 1932: Merge BSTs to Create Single BST
 * - LC 98: Validate BST
 * - LC 108: Convert Sorted Array to BST
 * - LC 297: Serialize and Deserialize Binary Tree
 * - Hamiltonian Path in Directed Graph
 * 
 * PATTERNS:
 * - Graph modeling of tree problems
 * - Finding roots in dependency graphs
 * - Backtracking with validation
 * - Tree merging with constraints
 */

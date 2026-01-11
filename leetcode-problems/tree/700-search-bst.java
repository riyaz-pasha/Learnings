/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a BINARY SEARCH TREE (BST) problem testing:
 * 1. Understanding of BST properties
 * 2. Binary search in tree structure
 * 3. Recursive vs iterative approaches
 * 4. Simplicity and efficiency
 * 5. One of the SIMPLEST tree problems!
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. BST PROPERTY:
 *    - Left subtree: all values < root
 *    - Right subtree: all values > root
 *    - This property holds recursively for all subtrees
 * 
 * 2. SEARCH STRATEGY:
 *    - If val == node.val: FOUND! Return node
 *    - If val < node.val: Search LEFT subtree
 *    - If val > node.val: Search RIGHT subtree
 * 
 * 3. THIS IS BINARY SEARCH!
 *    - Same as binary search in sorted array
 *    - O(log N) for balanced BST
 *    - O(N) worst case for skewed BST
 * 
 * 4. RETURN SUBTREE, NOT JUST NODE
 *    - Return the node itself (which is root of subtree)
 *    - All children automatically come with it
 * 
 * VISUALIZATION:
 * --------------
 * 
 * BST:        4
 *            / \
 *           2   7
 *          / \
 *         1   3
 * 
 * Search for 2:
 * - Start at 4: 2 < 4, go left
 * - Reach 2: 2 == 2, FOUND!
 * - Return node 2 and its subtree:
 *         2
 *        / \
 *       1   3
 * 
 * Search for 5:
 * - Start at 4: 5 > 4, go right
 * - Reach 7: 5 < 7, go left
 * - Reach null: NOT FOUND
 * - Return null
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Confirm BST property
 *    "Just to confirm: this is a valid BST where left < root < right?"
 * 
 * 2. Explain binary search
 *    "I'll use BST property to do binary search - compare val
 *     with current node and go left or right accordingly."
 * 
 * 3. Start with recursive (cleaner)
 *    "Recursive solution is very clean for this."
 * 
 * 4. Show iterative if asked
 *    "I can also do it iteratively with the same logic."
 * 
 * 5. Discuss complexity
 *    "O(h) time where h is height, O(1) space for iterative."
 */

// Definition for a binary tree node
class TreeNode {
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

class SearchBST {
    
    /**
     * APPROACH 1: RECURSIVE - MOST ELEGANT
     * =====================================
     * 
     * ALGORITHM:
     * ----------
     * 1. If root is null: return null (not found)
     * 2. If val == root.val: return root (found!)
     * 3. If val < root.val: search left subtree
     * 4. If val > root.val: search right subtree
     * 
     * BASE CASES:
     * - root == null: value not in tree
     * - val == root.val: found the target
     * 
     * RECURSIVE CASES:
     * - val < root.val: answer is in left subtree
     * - val > root.val: answer is in right subtree
     * 
     * WALKTHROUGH:
     * ------------
     * BST:        4
     *            / \
     *           2   7
     *          / \
     *         1   3
     * 
     * Search for 1:
     * searchBST(4, 1):
     *   1 < 4 → searchBST(2, 1)
     *     1 < 2 → searchBST(1, 1)
     *       1 == 1 → return node(1)
     * 
     * Search for 5:
     * searchBST(4, 5):
     *   5 > 4 → searchBST(7, 5)
     *     5 < 7 → searchBST(null, 5)
     *       null → return null
     * 
     * TIME COMPLEXITY: O(H)
     * - H = height of tree
     * - Balanced BST: O(log N)
     * - Skewed BST: O(N)
     * - Each recursive call goes down one level
     * 
     * SPACE COMPLEXITY: O(H)
     * - Recursion stack depth = height
     * - Balanced: O(log N)
     * - Skewed: O(N)
     * 
     * WHY THIS IS ELEGANT:
     * - Very concise (3 lines!)
     * - Mirrors binary search logic
     * - Natural for tree problems
     * - Easy to read and understand
     */
    public TreeNode searchBST(TreeNode root, int val) {
        // Base case: not found
        if (root == null) {
            return null;
        }
        
        // Base case: found!
        if (val == root.val) {
            return root;
        }
        
        // Recursive case: search left or right
        if (val < root.val) {
            return searchBST(root.left, val);
        } else {
            return searchBST(root.right, val);
        }
    }
    
    /**
     * APPROACH 1b: RECURSIVE - EVEN MORE CONCISE
     * ===========================================
     * 
     * Same logic but compressed into one-liner conditions
     * 
     * This is the "show-off" version - demonstrates mastery
     * of ternary operators and recursive thinking
     * 
     * TIME: O(H), SPACE: O(H)
     */
    public TreeNode searchBSTConcise(TreeNode root, int val) {
        if (root == null || val == root.val) {
            return root;
        }
        
        return val < root.val ? searchBST(root.left, val) 
                              : searchBST(root.right, val);
    }
    
    /**
     * APPROACH 2: ITERATIVE - OPTIMAL SPACE
     * ======================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Start at root
     * 2. While current is not null:
     *    - If val == current.val: return current
     *    - If val < current.val: go left
     *    - If val > current.val: go right
     * 3. If we exit loop: not found, return null
     * 
     * VISUALIZATION:
     * --------------
     * BST:        4
     *            / \
     *           2   7
     *          / \
     *         1   3
     * 
     * Search for 3:
     * current = 4: 3 < 4, go left
     * current = 2: 3 > 2, go right
     * current = 3: 3 == 3, FOUND!
     * 
     * Search for 6:
     * current = 4: 6 > 4, go right
     * current = 7: 6 < 7, go left
     * current = null: NOT FOUND
     * 
     * TIME COMPLEXITY: O(H)
     * - Same as recursive
     * - Follow one path from root to target
     * 
     * SPACE COMPLEXITY: O(1)
     * - Only using one pointer (current)
     * - No recursion stack!
     * - BETTER than recursive for space
     * 
     * WHEN TO USE:
     * - When stack overflow is a concern
     * - When you need O(1) space
     * - In production for very deep trees
     * - Shows you can avoid recursion when needed
     */
    public TreeNode searchBSTIterative(TreeNode root, int val) {
        TreeNode current = root;
        
        while (current != null) {
            // Found it!
            if (val == current.val) {
                return current;
            }
            
            // Go left or right based on comparison
            if (val < current.val) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        
        // Not found
        return null;
    }
    
    /**
     * APPROACH 2b: ITERATIVE - ULTRA CONCISE
     * =======================================
     * 
     * Same logic but even more compact
     * 
     * TIME: O(H), SPACE: O(1)
     */
    public TreeNode searchBSTIterativeConcise(TreeNode root, int val) {
        while (root != null && root.val != val) {
            root = val < root.val ? root.left : root.right;
        }
        return root;
    }
}

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach   | Time | Space | Lines | Best For
 * -----------|------|-------|-------|--------------------
 * Recursive  | O(H) | O(H)  | 8     | Clarity, interviews
 * Iterative  | O(H) | O(1)  | 10    | Space optimization
 * 
 * Where H = height of tree:
 * - Balanced BST: H = O(log N)
 * - Skewed BST: H = O(N)
 * 
 * RECOMMENDATION:
 * - Interview: Either works, recursive is cleaner
 * - Production: Iterative for guaranteed O(1) space
 * - Show both to demonstrate versatility
 */

/**
 * BST PROPERTIES REVIEW
 * ======================
 * 
 * VALID BST:
 * For every node:
 * - All nodes in left subtree < node.val
 * - All nodes in right subtree > node.val
 * - Both left and right subtrees are also BSTs
 * 
 * Example VALID BST:
 *       4
 *      / \
 *     2   6
 *    / \ / \
 *   1  3 5  7
 * 
 * At node 4: left (2,1,3) < 4 < right (6,5,7) ✓
 * At node 2: left (1) < 2 < right (3) ✓
 * At node 6: left (5) < 6 < right (7) ✓
 * 
 * Example INVALID BST:
 *       4
 *      / \
 *     2   6
 *    / \
 *   1   5  ✗ 5 > 4 but in left subtree!
 * 
 * INORDER TRAVERSAL OF BST = SORTED ARRAY
 * BST: [1, 2, 3, 4, 5, 6, 7] (sorted!)
 */

/**
 * COMPARISON WITH ARRAY BINARY SEARCH
 * ====================================
 * 
 * Array Binary Search:
 * int binarySearch(int[] arr, int target) {
 *     int left = 0, right = arr.length - 1;
 *     while (left <= right) {
 *         int mid = left + (right - left) / 2;
 *         if (arr[mid] == target) return mid;
 *         if (target < arr[mid]) right = mid - 1;
 *         else left = mid + 1;
 *     }
 *     return -1;
 * }
 * 
 * BST Search:
 * TreeNode searchBST(TreeNode root, int val) {
 *     while (root != null) {
 *         if (root.val == val) return root;
 *         if (val < root.val) root = root.left;
 *         else root = root.right;
 *     }
 *     return null;
 * }
 * 
 * SIMILARITIES:
 * - Both use binary search logic
 * - Both eliminate half the search space each step
 * - Both are O(log N) for balanced structure
 * 
 * DIFFERENCES:
 * - Array: Random access, use indices
 * - BST: Pointer-based, follow links
 * - Array: Always balanced (same depth both sides)
 * - BST: Can be unbalanced (skewed)
 */

/**
 * EDGE CASES & TESTING
 * =====================
 * 
 * EDGE CASES:
 * 1. Empty tree (null root)
 * 2. Single node tree
 * 3. Value is root
 * 4. Value is leaf node
 * 5. Value not in tree
 * 6. Duplicate values (BST shouldn't have, but handle gracefully)
 * 7. Minimum value (leftmost)
 * 8. Maximum value (rightmost)
 */

// Comprehensive test suite
class SearchBSTMain {
    public static void main(String[] args) {
        SearchBST sol = new SearchBST();
        
        System.out.println("=== Test Case 1: Normal BST ===");
        //       4
        //      / \
        //     2   7
        //    / \
        //   1   3
        TreeNode root1 = new TreeNode(4);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(7);
        root1.left.left = new TreeNode(1);
        root1.left.right = new TreeNode(3);
        
        System.out.println("Search for 2:");
        TreeNode result1 = sol.searchBST(root1, 2);
        System.out.println("  Found: " + (result1 != null));
        System.out.println("  Subtree root: " + (result1 != null ? result1.val : "null"));
        System.out.println("  Left child: " + (result1 != null && result1.left != null ? result1.left.val : "null"));
        System.out.println("  Right child: " + (result1 != null && result1.right != null ? result1.right.val : "null"));
        
        System.out.println("\nSearch for 5 (not in tree):");
        TreeNode result2 = sol.searchBST(root1, 5);
        System.out.println("  Found: " + (result2 != null));
        
        System.out.println("\n=== Test Case 2: Empty Tree ===");
        TreeNode root2 = null;
        TreeNode result3 = sol.searchBST(root2, 1);
        System.out.println("Result: " + (result3 == null ? "null" : "not null"));
        
        System.out.println("\n=== Test Case 3: Single Node ===");
        TreeNode root3 = new TreeNode(5);
        System.out.println("Search for 5: " + (sol.searchBST(root3, 5) != null));
        System.out.println("Search for 3: " + (sol.searchBST(root3, 3) != null));
        
        System.out.println("\n=== Test Case 4: Search for Root ===");
        TreeNode result4 = sol.searchBST(root1, 4);
        System.out.println("Found root: " + (result4 != null && result4.val == 4));
        
        System.out.println("\n=== Test Case 5: Search for Leaf ===");
        TreeNode result5 = sol.searchBST(root1, 1);
        System.out.println("Found leaf 1: " + (result5 != null && result5.val == 1));
        System.out.println("Is leaf (no children): " + (result5 != null && result5.left == null && result5.right == null));
        
        System.out.println("\n=== Test Case 6: Left-Skewed BST ===");
        //     5
        //    /
        //   3
        //  /
        // 1
        TreeNode root6 = new TreeNode(5);
        root6.left = new TreeNode(3);
        root6.left.left = new TreeNode(1);
        System.out.println("Search for 1 in skewed tree: " + (sol.searchBST(root6, 1) != null));
        
        System.out.println("\n=== Test Case 7: Right-Skewed BST ===");
        // 1
        //  \
        //   3
        //    \
        //     5
        TreeNode root7 = new TreeNode(1);
        root7.right = new TreeNode(3);
        root7.right.right = new TreeNode(5);
        System.out.println("Search for 5 in skewed tree: " + (sol.searchBST(root7, 5) != null));
        
        System.out.println("\n=== Compare Recursive vs Iterative ===");
        System.out.println("Recursive search for 2: " + (sol.searchBST(root1, 2) != null));
        System.out.println("Iterative search for 2: " + (sol.searchBSTIterative(root1, 2) != null));
        System.out.println("Both give same result: " + 
            (sol.searchBST(root1, 2) == sol.searchBSTIterative(root1, 2)));
        
        System.out.println("\n=== Visual Demonstration ===");
        System.out.println("BST Structure:");
        System.out.println("       4");
        System.out.println("      / \\");
        System.out.println("     2   7");
        System.out.println("    / \\");
        System.out.println("   1   3");
        System.out.println();
        System.out.println("Search for 1:");
        System.out.println("  Start at 4: 1 < 4 → go left");
        System.out.println("  At 2: 1 < 2 → go left");
        System.out.println("  At 1: 1 == 1 → FOUND!");
        System.out.println("  Return subtree rooted at 1");
        System.out.println();
        System.out.println("Search for 5:");
        System.out.println("  Start at 4: 5 > 4 → go right");
        System.out.println("  At 7: 5 < 7 → go left");
        System.out.println("  At null → NOT FOUND");
        System.out.println("  Return null");
        
        System.out.println("\n=== Complexity Analysis ===");
        System.out.println("Balanced BST (height = log N):");
        System.out.println("  Time: O(log N)");
        System.out.println("  Space: O(log N) recursive, O(1) iterative");
        System.out.println();
        System.out.println("Skewed BST (height = N):");
        System.out.println("  Time: O(N)");
        System.out.println("  Space: O(N) recursive, O(1) iterative");
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "This is a binary search tree, so I can use the BST property:
 *    values less than root are in left subtree, values greater
 *    than root are in right subtree."
 * 
 * 2. "This is essentially binary search - at each node, I compare
 *    the target value with the current node and go left or right."
 *    [Draw tree and show path]
 * 
 * 3. "I'll show the recursive solution first as it's very clean,
 *    just 3 cases: null, found, or recurse left/right."
 *    [Write recursive solution]
 * 
 * 4. "Time is O(h) where h is height - O(log n) for balanced BST,
 *    O(n) for skewed. Space is O(h) for recursion."
 * 
 * 5. "I can also do it iteratively with O(1) space, using the
 *    same logic but with a while loop instead of recursion."
 *    [Write iterative if asked]
 * 
 * 6. "Edge cases: empty tree, value not found, single node,
 *    and searching for root or leaf nodes."
 * 
 * KEY POINTS TO EMPHASIZE:
 * - BST property enables binary search
 * - O(log N) average case for balanced tree
 * - Return entire subtree, not just value
 * - Simple problem, focus on clean code
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Can you do it iteratively? [Yes, O(1) space]
 * - What if BST is unbalanced? [O(N) worst case]
 * - How would you insert a value? [Similar logic]
 * - How would you validate it's a BST? [Inorder traversal]
 * - What's the difference from binary tree search? [BST is O(log N) avg]
 * - How would you find min/max? [Leftmost/rightmost]
 */

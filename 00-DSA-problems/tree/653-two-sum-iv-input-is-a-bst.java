/**
 * Two Sum IV - Input is a BST - Complete Implementation
 * 
 * PROBLEM UNDERSTANDING:
 * Given a BST and target k, find if there exist two distinct elements that sum to k.
 * 
 * CRITICAL INSIGHTS:
 * 
 * 1. THIS IS A BST, NOT JUST ANY BINARY TREE!
 *    - Inorder traversal gives SORTED array
 *    - Can use two-pointer technique on sorted data
 *    - BST property enables efficient searching
 * 
 * 2. RELATIONSHIP TO CLASSIC TWO SUM:
 *    - Array Two Sum: Use HashMap → O(n) time, O(n) space
 *    - BST Two Sum: Multiple approaches available
 *    - Can leverage sorted property for O(1) space!
 * 
 * 3. DIFFERENT STRATEGIES:
 *    - HashSet: Like array two sum
 *    - Inorder + Two pointers: Leverage BST property
 *    - BST Iterator: Space-optimized two pointers
 *    - DFS + Search: Use BST search property
 * 
 * Example:
 *       5
 *      / \
 *     3   6
 *    / \   \
 *   2   4   7
 * 
 * k = 9:
 * Inorder: [2, 3, 4, 5, 6, 7]
 * Two pointers: 2 + 7 = 9 ✓
 * 
 * k = 28:
 * Max possible: 6 + 7 = 13 < 28 ✗
 * 
 * TIME COMPLEXITY: O(n) for all optimal approaches
 * SPACE COMPLEXITY: O(n) or O(h) depending on approach
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

class TwoSumBST {
    
    /**
     * APPROACH 1: HASHSET (SIMPLEST - LIKE ARRAY TWO SUM) ⭐
     * 
     * STRATEGY:
     * Traverse tree (any order), for each node check if complement exists in set.
     * 
     * INTUITION:
     * This is exactly like the classic Two Sum problem.
     * Don't need to leverage BST property, but it works!
     * 
     * ALGORITHM:
     * 1. For each node with value x
     * 2. Check if (k - x) exists in set
     * 3. If yes → found pair
     * 4. If no → add x to set and continue
     * 
     * PROS:
     * - Simple to code
     * - Easy to explain in interview
     * - Works for any binary tree (not just BST)
     * 
     * CONS:
     * - Doesn't leverage BST property
     * - O(n) space for set
     * 
     * TIME: O(n) - visit each node once
     * SPACE: O(n) - HashSet stores up to n elements
     */
    public boolean findTarget_HashSet(TreeNode root, int k) {
        Set<Integer> seen = new HashSet<>();
        return findWithSet(root, k, seen);
    }
    
    private boolean findWithSet(TreeNode node, int k, Set<Integer> seen) {
        if (node == null) {
            return false;
        }
        
        // Check if complement exists
        int complement = k - node.val;
        if (seen.contains(complement)) {
            return true;  // Found pair!
        }
        
        // Add current value to set
        seen.add(node.val);
        
        // Search both subtrees
        return findWithSet(node.left, k, seen) || findWithSet(node.right, k, seen);
    }
    
    
    /**
     * APPROACH 2: INORDER + TWO POINTERS (LEVERAGES BST PROPERTY) ⭐⭐
     * 
     * STRATEGY:
     * 1. Get inorder traversal (sorted array)
     * 2. Use two-pointer technique on sorted array
     * 
     * KEY INSIGHT:
     * Inorder traversal of BST = SORTED array!
     * Two pointers on sorted array is classic and elegant.
     * 
     * ALGORITHM:
     * Left pointer at start, right pointer at end:
     * - If sum < k → move left pointer right (increase sum)
     * - If sum > k → move right pointer left (decrease sum)
     * - If sum == k → found pair!
     * 
     * PROS:
     * - Leverages BST property
     * - Clean two-pointer logic
     * - Easy to understand and debug
     * 
     * CONS:
     * - O(n) space for array
     * - Two passes (inorder + two pointers)
     * 
     * TIME: O(n) - O(n) for inorder + O(n) for two pointers
     * SPACE: O(n) - array stores all values
     */
    public boolean findTarget_TwoPointers(TreeNode root, int k) {
        // Step 1: Get sorted array via inorder traversal
        List<Integer> sorted = new ArrayList<>();
        inorderTraversal(root, sorted);
        
        // Step 2: Two pointer technique
        int left = 0;
        int right = sorted.size() - 1;
        
        while (left < right) {
            int sum = sorted.get(left) + sorted.get(right);
            
            if (sum == k) {
                return true;  // Found pair!
            } else if (sum < k) {
                left++;  // Need larger sum
            } else {
                right--;  // Need smaller sum
            }
        }
        
        return false;  // No pair found
    }
    
    private void inorderTraversal(TreeNode node, List<Integer> result) {
        if (node == null) {
            return;
        }
        
        inorderTraversal(node.left, result);
        result.add(node.val);
        inorderTraversal(node.right, result);
    }
    
    
    /**
     * APPROACH 3: BST ITERATOR (SPACE OPTIMIZED TWO POINTERS) ⭐⭐⭐
     * 
     * STRATEGY:
     * Use iterators to simulate two pointers without storing entire array.
     * - Forward iterator: gives values in ascending order
     * - Backward iterator: gives values in descending order
     * 
     * KEY INSIGHT:
     * We don't need to store entire array!
     * Use stack-based iterators to get next/prev values on demand.
     * 
     * BRILLIANT OPTIMIZATION:
     * - Inorder iterator for ascending (left pointer)
     * - Reverse inorder iterator for descending (right pointer)
     * - Each maintains its own stack
     * 
     * ALGORITHM:
     * 1. Initialize two iterators at extremes
     * 2. Similar to two-pointer, but call next()/prev() instead of array access
     * 3. Check sum and move appropriate pointer
     * 
     * PROS:
     * - O(h) space instead of O(n)!
     * - Leverages BST property fully
     * - Impressive optimization for interviews
     * 
     * CONS:
     * - More complex to implement
     * - Need to implement BST iterators
     * 
     * TIME: O(n) - each node visited at most once by each iterator
     * SPACE: O(h) - two stacks of height h
     */
    public boolean findTarget_Iterator(TreeNode root, int k) {
        if (root == null) {
            return false;
        }
        
        BSTIterator forward = new BSTIterator(root, true);   // Ascending
        BSTIterator backward = new BSTIterator(root, false); // Descending
        
        int left = forward.next();
        int right = backward.next();
        
        while (left < right) {
            int sum = left + right;
            
            if (sum == k) {
                return true;
            } else if (sum < k) {
                left = forward.next();  // Move left pointer right
            } else {
                right = backward.next();  // Move right pointer left
            }
        }
        
        return false;
    }
    
    /**
     * BST Iterator - supports both forward (inorder) and backward (reverse inorder)
     */
    class BSTIterator {
        private Stack<TreeNode> stack;
        private boolean forward;  // true = ascending, false = descending
        
        public BSTIterator(TreeNode root, boolean forward) {
            this.stack = new Stack<>();
            this.forward = forward;
            pushAll(root);
        }
        
        public int next() {
            TreeNode node = stack.pop();
            
            // Move to next node in sequence
            if (forward) {
                pushAll(node.right);  // For ascending: go right
            } else {
                pushAll(node.left);   // For descending: go left
            }
            
            return node.val;
        }
        
        public boolean hasNext() {
            return !stack.isEmpty();
        }
        
        // Push all nodes in one direction
        private void pushAll(TreeNode node) {
            while (node != null) {
                stack.push(node);
                if (forward) {
                    node = node.left;   // For ascending: go all the way left
                } else {
                    node = node.right;  // For descending: go all the way right
                }
            }
        }
    }
    
    
    /**
     * APPROACH 4: DFS + BST SEARCH (LEVERAGES SEARCH PROPERTY)
     * 
     * STRATEGY:
     * For each node, search for its complement in the tree.
     * Use BST search property for O(h) search time.
     * 
     * ALGORITHM:
     * 1. For each node with value x
     * 2. Search for (k - x) in the tree using BST search
     * 3. Make sure we don't count same node twice
     * 
     * PROS:
     * - O(h) space (recursion only)
     * - Leverages BST search property
     * 
     * CONS:
     * - O(n * h) time in worst case
     * - Less efficient than other approaches
     * 
     * TIME: O(n * h) - for each of n nodes, search takes O(h)
     *       Average: O(n log n), Worst: O(n²)
     * SPACE: O(h) - recursion depth
     */
    public boolean findTarget_Search(TreeNode root, int k) {
        return dfsSearch(root, root, k);
    }
    
    private boolean dfsSearch(TreeNode current, TreeNode root, int k) {
        if (current == null) {
            return false;
        }
        
        int complement = k - current.val;
        
        // Search for complement (excluding current node itself)
        if (complement != current.val && search(root, complement)) {
            return true;
        }
        
        // Search in both subtrees
        return dfsSearch(current.left, root, k) || dfsSearch(current.right, root, k);
    }
    
    // BST search
    private boolean search(TreeNode node, int target) {
        if (node == null) {
            return false;
        }
        
        if (node.val == target) {
            return true;
        } else if (target < node.val) {
            return search(node.left, target);
        } else {
            return search(node.right, target);
        }
    }
    
    
    /**
     * APPROACH 5: CONVERT TO ARRAY (BRUTE FORCE)
     * 
     * STRATEGY:
     * Extract all values, then use nested loops to find pair.
     * 
     * NOT RECOMMENDED - just for completeness
     * 
     * TIME: O(n²) - nested loops
     * SPACE: O(n) - array
     */
    public boolean findTarget_BruteForce(TreeNode root, int k) {
        List<Integer> values = new ArrayList<>();
        collectValues(root, values);
        
        // Nested loop to find pair
        for (int i = 0; i < values.size(); i++) {
            for (int j = i + 1; j < values.size(); j++) {
                if (values.get(i) + values.get(j) == k) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void collectValues(TreeNode node, List<Integer> values) {
        if (node == null) return;
        values.add(node.val);
        collectValues(node.left, values);
        collectValues(node.right, values);
    }
    
    
    /**
     * ==================== VISUAL WALKTHROUGH ====================
     * 
     * Example Tree:
     *       5
     *      / \
     *     3   6
     *    / \   \
     *   2   4   7
     * 
     * k = 9
     * 
     * APPROACH 1: HASHSET
     * 
     * Visit 5: complement = 4, set = {}       → add 5
     * Visit 3: complement = 6, set = {5}      → add 3
     * Visit 2: complement = 7, set = {5,3}    → add 2
     * Visit 4: complement = 5, set = {5,3,2}  → FOUND! ✓
     * 
     * Result: true (4 + 5 = 9)
     * 
     * ----------------------------------------
     * 
     * APPROACH 2: TWO POINTERS
     * 
     * Step 1: Inorder traversal
     * [2, 3, 4, 5, 6, 7]
     * 
     * Step 2: Two pointers
     * left = 0 (val = 2), right = 5 (val = 7)
     * sum = 2 + 7 = 9 ✓
     * 
     * Result: true
     * 
     * ----------------------------------------
     * 
     * APPROACH 3: ITERATORS
     * 
     * Forward iterator: 2 → 3 → 4 → 5 → 6 → 7
     * Backward iterator: 7 → 6 → 5 → 4 → 3 → 2
     * 
     * Step 1: left = 2, right = 7
     *         sum = 2 + 7 = 9 ✓
     * 
     * Result: true
     * 
     * ----------------------------------------
     * 
     * Example with k = 28:
     * 
     * TWO POINTERS:
     * [2, 3, 4, 5, 6, 7]
     *  ↑              ↑
     * 
     * sum = 2 + 7 = 9 < 28 → left++
     * sum = 3 + 7 = 10 < 28 → left++
     * sum = 4 + 7 = 11 < 28 → left++
     * sum = 5 + 7 = 12 < 28 → left++
     * sum = 6 + 7 = 13 < 28 → left++
     * left >= right → no pair found
     * 
     * Result: false
     */
    
    
    /**
     * ==================== COMPLEXITY ANALYSIS ====================
     * 
     * COMPARISON TABLE:
     * 
     * | Approach         | Time      | Space    | Pros                        | Cons               |
     * |------------------|-----------|----------|-----------------------------|--------------------|
     * | HashSet          | O(n)      | O(n)     | Simple, works for any tree  | Doesn't use BST    |
     * | Two Pointers     | O(n)      | O(n)     | Clean, leverages BST        | Extra array space  |
     * | Iterator         | O(n)      | O(h)     | Space optimal, elegant      | Complex to code    |
     * | DFS + Search     | O(n*h)    | O(h)     | Uses BST search             | Slower             |
     * | Brute Force      | O(n²)     | O(n)     | None                        | Too slow           |
     * 
     * RECOMMENDATION FOR INTERVIEWS:
     * 1. Start with HashSet (fastest to code, easiest to explain)
     * 2. Mention Two Pointers (shows you understand BST property)
     * 3. Discuss Iterator approach if they want space optimization
     * 
     * WHY O(h) SPACE FOR ITERATOR IS BETTER:
     * - Balanced BST: h = log n → O(log n) space
     * - Much better than O(n) for large trees
     * - In perfectly balanced tree of 1 million nodes:
     *   O(h) = O(20) vs O(n) = O(1,000,000)!
     */
    
    
    /**
     * ==================== INTERVIEW STRATEGY GUIDE ====================
     * 
     * STEP 1: CLARIFY (1-2 minutes)
     * Questions to ask:
     * - Can we use the same element twice? (No - two distinct elements)
     * - Can the tree be empty? (Yes - return false)
     * - Are values unique? (Usually yes in BST)
     * - Should we optimize for time or space?
     * 
     * STEP 2: EXPLAIN INSIGHT (2 minutes)
     * "This is similar to the classic Two Sum problem, but with a BST.
     * The key insight is that inorder traversal of a BST gives us a sorted array.
     * We can either use a HashSet like the original problem, or leverage the
     * sorted property with two pointers."
     * 
     * STEP 3: PROPOSE APPROACH (1 minute)
     * "I'll start with the HashSet approach since it's simplest and O(n).
     * Then I can optimize space using BST iterators if needed."
     * 
     * STEP 4: CODE (8-10 minutes)
     * Write HashSet solution first
     * 
     * STEP 5: TEST (3-4 minutes)
     * Test cases:
     * 1. Pair exists: k = 9 in example
     * 2. Pair doesn't exist: k = 28
     * 3. Single node: root = [5], k = 10
     * 4. Two nodes: root = [3, 2], k = 5
     * 5. Same element twice: root = [2, 1, 3], k = 4
     * 
     * STEP 6: OPTIMIZE (remaining time)
     * - Explain two-pointer approach
     * - Discuss iterator-based solution for O(h) space
     * - Mention time/space tradeoffs
     */
    
    
    /**
     * ==================== COMMON MISTAKES ====================
     */
    
    // ❌ MISTAKE 1: Using same node twice
    public boolean findWrong1(TreeNode root, int k) {
        Set<Integer> set = new HashSet<>();
        return dfs(root, k, set);
    }
    
    private boolean dfs(TreeNode node, int k, Set<Integer> set) {
        if (node == null) return false;
        
        // ❌ Check complement before adding to set
        // This allows node.val + node.val = k
        if (set.contains(k - node.val)) {
            return true;  // Wrong if k = 2 * node.val!
        }
        
        set.add(node.val);
        return dfs(node.left, k, set) || dfs(node.right, k, set);
    }
    
    // FIX: Add to set AFTER checking (or ensure complement != current)
    
    
    // ❌ MISTAKE 2: Wrong two-pointer logic
    public boolean findWrong2(TreeNode root, int k) {
        List<Integer> sorted = new ArrayList<>();
        inorderTraversal(root, sorted);
        
        int left = 0, right = sorted.size() - 1;
        
        while (left <= right) {  // ❌ Should be left < right
            int sum = sorted.get(left) + sorted.get(right);
            if (sum == k) return true;
            else if (sum < k) left++;
            else right--;
        }
        
        return false;
        // ❌ When left == right, we're using same element twice!
    }
    
    
    // ❌ MISTAKE 3: Not handling null tree
    public boolean findWrong3(TreeNode root, int k) {
        Set<Integer> set = new HashSet<>();
        // ❌ No null check!
        return find(root, k, set);
    }
    // Should check: if (root == null) return false;
    
    
    /**
     * ==================== FOLLOW-UP QUESTIONS ====================
     */
    
    /**
     * Q1: "Find ALL pairs that sum to k?"
     * A: Modify to collect pairs instead of returning boolean
     */
    public List<int[]> findAllPairs(TreeNode root, int k) {
        List<int[]> pairs = new ArrayList<>();
        List<Integer> sorted = new ArrayList<>();
        inorderTraversal(root, sorted);
        
        int left = 0, right = sorted.size() - 1;
        
        while (left < right) {
            int sum = sorted.get(left) + sorted.get(right);
            
            if (sum == k) {
                pairs.add(new int[]{sorted.get(left), sorted.get(right)});
                left++;
                right--;
            } else if (sum < k) {
                left++;
            } else {
                right--;
            }
        }
        
        return pairs;
    }
    
    
    /**
     * Q2: "Find pair with sum closest to k?"
     * A: Track minimum difference
     */
    public int[] findClosestPair(TreeNode root, int k) {
        List<Integer> sorted = new ArrayList<>();
        inorderTraversal(root, sorted);
        
        int left = 0, right = sorted.size() - 1;
        int minDiff = Integer.MAX_VALUE;
        int[] result = new int[2];
        
        while (left < right) {
            int sum = sorted.get(left) + sorted.get(right);
            int diff = Math.abs(sum - k);
            
            if (diff < minDiff) {
                minDiff = diff;
                result[0] = sorted.get(left);
                result[1] = sorted.get(right);
            }
            
            if (sum < k) {
                left++;
            } else {
                right--;
            }
        }
        
        return result;
    }
    
    
    /**
     * Q3: "Three Sum in BST?"
     * A: Fix one element, use Two Sum for remaining
     */
    public boolean threeSum(TreeNode root, int k) {
        List<Integer> sorted = new ArrayList<>();
        inorderTraversal(root, sorted);
        
        for (int i = 0; i < sorted.size() - 2; i++) {
            int target = k - sorted.get(i);
            
            // Two sum on remaining elements
            int left = i + 1, right = sorted.size() - 1;
            
            while (left < right) {
                int sum = sorted.get(left) + sorted.get(right);
                
                if (sum == target) {
                    return true;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return false;
    }
    
    
    /**
     * ==================== UTILITY METHODS ====================
     */
    
    // Build sample BST
    public TreeNode buildSampleTree() {
        /*
         *       5
         *      / \
         *     3   6
         *    / \   \
         *   2   4   7
         */
        TreeNode root = new TreeNode(5);
        root.left = new TreeNode(3);
        root.right = new TreeNode(6);
        root.left.left = new TreeNode(2);
        root.left.right = new TreeNode(4);
        root.right.right = new TreeNode(7);
        return root;
    }
    
    // Print tree
    public void printTree(TreeNode root, String prefix, boolean isLeft) {
        if (root == null) return;
        
        System.out.println(prefix + (isLeft ? "├── " : "└── ") + root.val);
        
        if (root.left != null || root.right != null) {
            if (root.left != null) {
                printTree(root.left, prefix + (isLeft ? "│   " : "    "), true);
            }
            if (root.right != null) {
                printTree(root.right, prefix + (isLeft ? "│   " : "    "), false);
            }
        }
    }
    
    
    /**
     * ==================== TEST CASES ====================
     */
    public static void main(String[] args) {
        TwoSumBST solution = new TwoSumBST();
        TreeNode root = solution.buildSampleTree();
        
        System.out.println("Tree Structure:");
        solution.printTree(root, "", false);
        System.out.println("\nInorder: [2, 3, 4, 5, 6, 7]");
        
        // Test different approaches
        int[] testCases = {9, 28, 5, 11, 13, 8, 1};
        
        for (int k : testCases) {
            System.out.println("\n=== k = " + k + " ===");
            
            boolean result1 = solution.findTarget_HashSet(root, k);
            boolean result2 = solution.findTarget_TwoPointers(root, k);
            boolean result3 = solution.findTarget_Iterator(root, k);
            boolean result4 = solution.findTarget_Search(root, k);
            
            System.out.println("HashSet:      " + result1);
            System.out.println("Two Pointers: " + result2);
            System.out.println("Iterator:     " + result3);
            System.out.println("DFS+Search:   " + result4);
            
            if (result1 == result2 && result2 == result3 && result3 == result4) {
                System.out.println("✓ All approaches agree!");
            } else {
                System.out.println("✗ Results differ!");
            }
            
            // Show pairs if exists
            if (result1) {
                List<int[]> pairs = solution.findAllPairs(root, k);
                System.out.print("Pairs: ");
                for (int[] pair : pairs) {
                    System.out.print("[" + pair[0] + "," + pair[1] + "] ");
                }
                System.out.println();
            }
        }
        
        // Test edge cases
        System.out.println("\n=== Edge Cases ===");
        
        System.out.println("\nSingle node [5], k = 10:");
        TreeNode single = new TreeNode(5);
        System.out.println("Result: " + solution.findTarget_HashSet(single, 10));
        
        System.out.println("\nTwo nodes [3,2], k = 5:");
        TreeNode two = new TreeNode(3);
        two.left = new TreeNode(2);
        System.out.println("Result: " + solution.findTarget_HashSet(two, 5));
        
        System.out.println("\nEmpty tree, k = 5:");
        System.out.println("Result: " + solution.findTarget_HashSet(null, 5));
        
        // Test follow-ups
        System.out.println("\n=== Follow-up: Closest Pair ===");
        int[] closest = solution.findClosestPair(root, 10);
        System.out.println("Pair closest to 10: [" + closest[0] + "," + closest[1] + "]");
        System.out.println("Sum: " + (closest[0] + closest[1]));
        
        System.out.println("\n=== Follow-up: Three Sum ===");
        System.out.println("Three sum to 12: " + solution.threeSum(root, 12));
        System.out.println("(e.g., 2 + 3 + 7 = 12)");
    }
}

/**
 * ==================== KEY TAKEAWAYS ====================
 * 
 * 1. **LEVERAGE BST PROPERTY**: Inorder = sorted array
 * 
 * 2. **MULTIPLE VALID APPROACHES**:
 *    - HashSet: O(n) time, O(n) space (simplest)
 *    - Two Pointers: O(n) time, O(n) space (clean)
 *    - Iterator: O(n) time, O(h) space (optimal)
 * 
 * 3. **TWO-POINTER PATTERN**: Works on sorted data
 * 
 * 4. **SPACE OPTIMIZATION**: Iterators reduce O(n) to O(h)
 * 
 * 5. **SIMILAR TO CLASSIC TWO SUM**: But with tree structure
 * 
 * INTERVIEW STRATEGY:
 * - Start with HashSet (fast, simple)
 * - Mention Two Pointers (shows BST understanding)
 * - Discuss Iterator if asked about space
 * 
 * RELATED PROBLEMS:
 * - LC 1: Two Sum (array version)
 * - LC 653: Two Sum IV - BST (this problem)
 * - LC 167: Two Sum II - Sorted Array
 * - LC 170: Two Sum III - Data Structure Design
 * - LC 15: Three Sum
 * - LC 18: Four Sum
 * - LC 530: Minimum Absolute Difference in BST
 * 
 * PATTERNS TO RECOGNIZE:
 * - BST + Two Sum → Inorder + Two Pointers
 * - Space optimization → Use iterators instead of array
 * - Sorted data → Two pointers is natural choice
 */

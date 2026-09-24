import java.util.ArrayList;
import java.util.List;

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode(int val) { this.val = val; }
}

class BSTOperations {
    
    // ============================================================
    // MINIMUM VALUE (Leftmost node)
    // ============================================================
    
    // Approach 1: Recursive
    // Time: O(h), Space: O(h)
    public int findMinRecursive(TreeNode root) {
        if (root == null) {
            throw new IllegalArgumentException("Tree is empty");
        }
        
        if (root.left == null) {
            return root.val;
        }
        
        return findMinRecursive(root.left);
    }
    
    // Approach 2: Iterative (Preferred)
    // Time: O(h), Space: O(1)
    public int findMin(TreeNode root) {
        if (root == null) {
            throw new IllegalArgumentException("Tree is empty");
        }
        
        TreeNode curr = root;
        while (curr.left != null) {
            curr = curr.left;
        }
        
        return curr.val;
    }
    
    // ============================================================
    // MAXIMUM VALUE (Rightmost node)
    // ============================================================
    
    // Approach 1: Recursive
    // Time: O(h), Space: O(h)
    public int findMaxRecursive(TreeNode root) {
        if (root == null) {
            throw new IllegalArgumentException("Tree is empty");
        }
        
        if (root.right == null) {
            return root.val;
        }
        
        return findMaxRecursive(root.right);
    }
    
    // Approach 2: Iterative (Preferred)
    // Time: O(h), Space: O(1)
    public int findMax(TreeNode root) {
        if (root == null) {
            throw new IllegalArgumentException("Tree is empty");
        }
        
        TreeNode curr = root;
        while (curr.right != null) {
            curr = curr.right;
        }
        
        return curr.val;
    }
    
    // ============================================================
    // CEIL (Smallest value >= target)
    // ============================================================
    
    // Approach 1: Recursive
    // Time: O(h), Space: O(h)
    public Integer findCeilRecursive(TreeNode root, int target) {
        return findCeilHelper(root, target, null);
    }
    
    private Integer findCeilHelper(TreeNode node, int target, Integer ceil) {
        if (node == null) {
            return ceil;
        }
        
        if (node.val == target) {
            return node.val;
        }
        
        if (node.val < target) {
            // Current is smaller, go right
            return findCeilHelper(node.right, target, ceil);
        } else {
            // Current is larger, it could be ceil
            // But check left for smaller ceil
            return findCeilHelper(node.left, target, node.val);
        }
    }
    
    // Approach 2: Iterative (Preferred)
    // Time: O(h), Space: O(1)
    public Integer findCeil(TreeNode root, int target) {
        Integer ceil = null;
        TreeNode curr = root;
        
        while (curr != null) {
            if (curr.val == target) {
                return curr.val;  // Exact match
            }
            
            if (curr.val < target) {
                // Current is too small, go right
                curr = curr.right;
            } else {
                // Current is >= target, it's a candidate
                ceil = curr.val;
                // But check left for potentially smaller ceil
                curr = curr.left;
            }
        }
        
        return ceil;
    }
    
    // ============================================================
    // FLOOR (Largest value <= target)
    // ============================================================
    
    // Approach 1: Recursive
    // Time: O(h), Space: O(h)
    public Integer findFloorRecursive(TreeNode root, int target) {
        return findFloorHelper(root, target, null);
    }
    
    private Integer findFloorHelper(TreeNode node, int target, Integer floor) {
        if (node == null) {
            return floor;
        }
        
        if (node.val == target) {
            return node.val;
        }
        
        if (node.val > target) {
            // Current is larger, go left
            return findFloorHelper(node.left, target, floor);
        } else {
            // Current is smaller, it could be floor
            // But check right for larger floor
            return findFloorHelper(node.right, target, node.val);
        }
    }
    
    // Approach 2: Iterative (Preferred)
    // Time: O(h), Space: O(1)
    public Integer findFloor(TreeNode root, int target) {
        Integer floor = null;
        TreeNode curr = root;
        
        while (curr != null) {
            if (curr.val == target) {
                return curr.val;  // Exact match
            }
            
            if (curr.val > target) {
                // Current is too large, go left
                curr = curr.left;
            } else {
                // Current is <= target, it's a candidate
                floor = curr.val;
                // But check right for potentially larger floor
                curr = curr.right;
            }
        }
        
        return floor;
    }
    
    // ============================================================
    // COMBINATION: Find all four values at once
    // ============================================================
    
    public BSTStats findAllStats(TreeNode root, int target) {
        BSTStats stats = new BSTStats();
        
        stats.min = findMin(root);
        stats.max = findMax(root);
        stats.ceil = findCeil(root, target);
        stats.floor = findFloor(root, target);
        stats.target = target;
        
        return stats;
    }
    
    static class BSTStats {
        int target;
        int min;
        int max;
        Integer ceil;
        Integer floor;
        
        @Override
        public String toString() {
            return String.format(
                "Target: %d\nMin: %d\nMax: %d\nCeil: %s\nFloor: %s",
                target, min, max, 
                ceil != null ? ceil : "null",
                floor != null ? floor : "null"
            );
        }
    }
    
    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    // Build BST from sorted array
    public static TreeNode buildBST(int[] values) {
        TreeNode root = null;
        for (int val : values) {
            root = insert(root, val);
        }
        return root;
    }
    
    private static TreeNode insert(TreeNode node, int val) {
        if (node == null) {
            return new TreeNode(val);
        }
        
        if (val < node.val) {
            node.left = insert(node.left, val);
        } else if (val > node.val) {
            node.right = insert(node.right, val);
        }
        
        return node;
    }
    
    // Visualize BST
    private static void visualizeBST(TreeNode root) {
        System.out.println("\nBST Structure:");
        printTree(root, "", true);
    }
    
    private static void printTree(TreeNode node, String prefix, boolean isTail) {
        if (node == null) return;
        
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node.val);
        
        if (node.left != null || node.right != null) {
            if (node.right != null) {
                printTree(node.right, prefix + (isTail ? "    " : "│   "), false);
            } else {
                System.out.println(prefix + (isTail ? "    " : "│   ") + "├── null");
            }
            
            if (node.left != null) {
                printTree(node.left, prefix + (isTail ? "    " : "│   "), true);
            } else {
                System.out.println(prefix + (isTail ? "    " : "│   ") + "└── null");
            }
        }
    }
    
    // Inorder traversal
    private static List<Integer> inorder(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorderHelper(root, result);
        return result;
    }
    
    private static void inorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        inorderHelper(node.left, result);
        result.add(node.val);
        inorderHelper(node.right, result);
    }
    
    // ============================================================
    // TEST CASES
    // ============================================================
    
    public static void main(String[] args) {
        BSTOperations ops = new BSTOperations();
        
        // Build sample BST:     8
        //                     /   \
        //                    3     10
        //                   / \      \
        //                  1   6      14
        //                     / \    /
        //                    4   7  13
        
        int[] values = {8, 3, 10, 1, 6, 14, 4, 7, 13};
        TreeNode root = buildBST(values);
        
        System.out.println("=".repeat(70));
        System.out.println("BINARY SEARCH TREE OPERATIONS");
        System.out.println("=".repeat(70));
        
        visualizeBST(root);
        System.out.println("\nInorder (sorted): " + inorder(root));
        
        // Test Min and Max
        System.out.println("\n" + "=".repeat(70));
        System.out.println("MIN AND MAX");
        System.out.println("=".repeat(70));
        System.out.println("Minimum value: " + ops.findMin(root));
        System.out.println("Maximum value: " + ops.findMax(root));
        
        // Test Ceil and Floor with various targets
        System.out.println("\n" + "=".repeat(70));
        System.out.println("CEIL AND FLOOR OPERATIONS");
        System.out.println("=".repeat(70));
        
        int[] targets = {0, 2, 5, 6, 9, 11, 15};
        
        for (int target : targets) {
            System.out.println("\nTarget: " + target);
            System.out.println("  Ceil:  " + ops.findCeil(root, target));
            System.out.println("  Floor: " + ops.findFloor(root, target));
        }
        
        // All stats at once
        System.out.println("\n" + "=".repeat(70));
        System.out.println("ALL STATISTICS FOR TARGET = 5");
        System.out.println("=".repeat(70));
        BSTStats stats = ops.findAllStats(root, 5);
        System.out.println(stats);
        
        // Detailed trace
        System.out.println("\n" + "=".repeat(70));
        System.out.println("DETAILED TRACE: Finding Ceil of 5");
        System.out.println("=".repeat(70));
        traceCeil(root, 5);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("DETAILED TRACE: Finding Floor of 5");
        System.out.println("=".repeat(70));
        traceFloor(root, 5);
        
        // Edge cases
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EDGE CASES");
        System.out.println("=".repeat(70));
        testEdgeCases();
        
        // Comparison table
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPARISON TABLE");
        System.out.println("=".repeat(70));
        comparisonTable(root);
        
        // Algorithm comparison
        System.out.println("\n" + "=".repeat(70));
        System.out.println("ALGORITHM COMPLEXITY");
        System.out.println("=".repeat(70));
        algorithmComplexity();
        
        // Key concepts
        System.out.println("\n" + "=".repeat(70));
        System.out.println("KEY CONCEPTS");
        System.out.println("=".repeat(70));
        explainConcepts();
    }
    
    private static void traceCeil(TreeNode root, int target) {
        System.out.println("Finding ceil of " + target);
        System.out.println("Ceil = smallest value >= " + target);
        System.out.println("\nBST values: " + inorder(root));
        System.out.println("\nTraversal:");
        
        Integer ceil = null;
        TreeNode curr = root;
        int step = 1;
        
        while (curr != null) {
            System.out.printf("\nStep %d: At node %d\n", step, curr.val);
            
            if (curr.val == target) {
                System.out.println("  Found exact match! Ceil = " + curr.val);
                ceil = curr.val;
                break;
            }
            
            if (curr.val < target) {
                System.out.println("  " + curr.val + " < " + target + " → Go RIGHT");
                curr = curr.right;
            } else {
                System.out.println("  " + curr.val + " >= " + target + " → Potential ceil!");
                System.out.println("  Update ceil = " + curr.val);
                System.out.println("  Go LEFT to find smaller ceil");
                ceil = curr.val;
                curr = curr.left;
            }
            
            step++;
        }
        
        System.out.println("\nFinal Ceil: " + ceil);
    }
    
    private static void traceFloor(TreeNode root, int target) {
        System.out.println("Finding floor of " + target);
        System.out.println("Floor = largest value <= " + target);
        System.out.println("\nBST values: " + inorder(root));
        System.out.println("\nTraversal:");
        
        Integer floor = null;
        TreeNode curr = root;
        int step = 1;
        
        while (curr != null) {
            System.out.printf("\nStep %d: At node %d\n", step, curr.val);
            
            if (curr.val == target) {
                System.out.println("  Found exact match! Floor = " + curr.val);
                floor = curr.val;
                break;
            }
            
            if (curr.val > target) {
                System.out.println("  " + curr.val + " > " + target + " → Go LEFT");
                curr = curr.left;
            } else {
                System.out.println("  " + curr.val + " <= " + target + " → Potential floor!");
                System.out.println("  Update floor = " + curr.val);
                System.out.println("  Go RIGHT to find larger floor");
                floor = curr.val;
                curr = curr.right;
            }
            
            step++;
        }
        
        System.out.println("\nFinal Floor: " + floor);
    }
    
    private static void testEdgeCases() {
        BSTOperations ops = new BSTOperations();
        
        // Single node
        System.out.println("\n1. Single Node Tree (value = 5):");
        TreeNode single = new TreeNode(5);
        System.out.println("   Min: " + ops.findMin(single));
        System.out.println("   Max: " + ops.findMax(single));
        System.out.println("   Ceil(5): " + ops.findCeil(single, 5));
        System.out.println("   Ceil(3): " + ops.findCeil(single, 3));
        System.out.println("   Ceil(7): " + ops.findCeil(single, 7));
        System.out.println("   Floor(5): " + ops.findFloor(single, 5));
        System.out.println("   Floor(3): " + ops.findFloor(single, 3));
        System.out.println("   Floor(7): " + ops.findFloor(single, 7));
        
        // Left skewed
        System.out.println("\n2. Left Skewed Tree [5,3,1]:");
        int[] leftSkewed = {5, 3, 1};
        TreeNode leftTree = buildBST(leftSkewed);
        System.out.println("   Min: " + ops.findMin(leftTree));
        System.out.println("   Max: " + ops.findMax(leftTree));
        System.out.println("   Ceil(2): " + ops.findCeil(leftTree, 2));
        System.out.println("   Floor(2): " + ops.findFloor(leftTree, 2));
        
        // Right skewed
        System.out.println("\n3. Right Skewed Tree [1,3,5]:");
        int[] rightSkewed = {1, 3, 5};
        TreeNode rightTree = buildBST(rightSkewed);
        System.out.println("   Min: " + ops.findMin(rightTree));
        System.out.println("   Max: " + ops.findMax(rightTree));
        System.out.println("   Ceil(2): " + ops.findCeil(rightTree, 2));
        System.out.println("   Floor(2): " + ops.findFloor(rightTree, 2));
    }
    
    private static void comparisonTable(TreeNode root) {
        BSTOperations ops = new BSTOperations();
        List<Integer> sorted = inorder(root);
        
        System.out.println("\nBST values (sorted): " + sorted);
        System.out.println("\n┌────────┬──────┬───────┬────────────────────────────┐");
        System.out.println("│ Target │ Ceil │ Floor │ Explanation                │");
        System.out.println("├────────┼──────┼───────┼────────────────────────────┤");
        
        int[] testTargets = {0, 2, 5, 6, 9, 11, 15};
        
        for (int target : testTargets) {
            Integer ceil = ops.findCeil(root, target);
            Integer floor = ops.findFloor(root, target);
            
            String explanation;
            if (sorted.contains(target)) {
                explanation = "Exact match in BST";
            } else {
                explanation = "Between " + 
                    (floor != null ? floor : "-∞") + " and " + 
                    (ceil != null ? ceil : "+∞");
            }
            
            System.out.printf("│   %2d   │ %-4s │  %-4s │ %-26s │%n",
                target,
                ceil != null ? ceil : "null",
                floor != null ? floor : "null",
                explanation);
        }
        
        System.out.println("└────────┴──────┴───────┴────────────────────────────┘");
    }
    
    private static void algorithmComplexity() {
        System.out.println("\n┌─────────────────┬──────────────┬──────────────┬─────────────┐");
        System.out.println("│ Operation       │ Time         │ Space        │ Notes       │");
        System.out.println("├─────────────────┼──────────────┼──────────────┼─────────────┤");
        System.out.println("│ Find Min (Iter) │ O(h)         │ O(1)         │ Go left     │");
        System.out.println("│ Find Min (Rec)  │ O(h)         │ O(h)         │ Stack space │");
        System.out.println("│ Find Max (Iter) │ O(h)         │ O(1)         │ Go right    │");
        System.out.println("│ Find Max (Rec)  │ O(h)         │ O(h)         │ Stack space │");
        System.out.println("│ Find Ceil (Iter)│ O(h)         │ O(1)         │ Best        │");
        System.out.println("│ Find Ceil (Rec) │ O(h)         │ O(h)         │ Recursion   │");
        System.out.println("│ Find Floor(Iter)│ O(h)         │ O(1)         │ Best        │");
        System.out.println("│ Find Floor(Rec) │ O(h)         │ O(h)         │ Recursion   │");
        System.out.println("└─────────────────┴──────────────┴──────────────┴─────────────┘");
        System.out.println("\nh = height of tree");
        System.out.println("Balanced BST: h = O(log n)");
        System.out.println("Skewed BST: h = O(n)");
    }
    
    private static void explainConcepts() {
        System.out.println("\n1. MINIMUM");
        System.out.println("   - Leftmost node in BST");
        System.out.println("   - Keep going left until no left child");
        System.out.println("   - Example: [8,3,10,1,6] → Min = 1");
        
        System.out.println("\n2. MAXIMUM");
        System.out.println("   - Rightmost node in BST");
        System.out.println("   - Keep going right until no right child");
        System.out.println("   - Example: [8,3,10,1,6,14] → Max = 14");
        
        System.out.println("\n3. CEIL (Smallest >= target)");
        System.out.println("   - If node.val >= target: potential ceil, go left");
        System.out.println("   - If node.val < target: go right");
        System.out.println("   - Example: [1,3,6,7,8] ceil(5) = 6");
        System.out.println("   - Why go left? To find smaller value still >= target");
        
        System.out.println("\n4. FLOOR (Largest <= target)");
        System.out.println("   - If node.val <= target: potential floor, go right");
        System.out.println("   - If node.val > target: go left");
        System.out.println("   - Example: [1,3,6,7,8] floor(5) = 3");
        System.out.println("   - Why go right? To find larger value still <= target");
        
        System.out.println("\n5. BST PROPERTY");
        System.out.println("   - Left subtree: all values < root");
        System.out.println("   - Right subtree: all values > root");
        System.out.println("   - This enables O(log n) operations in balanced trees");
        
        System.out.println("\n6. KEY INSIGHT");
        System.out.println("   - Ceil: Track candidates while searching for smaller");
        System.out.println("   - Floor: Track candidates while searching for larger");
        System.out.println("   - Both use BST property to prune search space");
    }
}

/*
DETAILED EXPLANATION:

MINIMUM IN BST
==============
The minimum value is the LEFTMOST node.

Algorithm:
  curr = root
  while curr.left exists:
      curr = curr.left
  return curr.val

Time: O(h), Space: O(1)

Example:      8
            /   \
           3     10
          / \
         1   6

Min = 1 (leftmost)

MAXIMUM IN BST
==============
The maximum value is the RIGHTMOST node.

Algorithm:
  curr = root
  while curr.right exists:
      curr = curr.right
  return curr.val

Time: O(h), Space: O(1)

Example:      8
            /   \
           3     10
                   \
                    14

Max = 14 (rightmost)

CEIL (Smallest >= target)
==========================
Find the smallest value that is >= target.

Key insight:
- If current >= target: it's a candidate, but check left for smaller
- If current < target: go right to find larger values

Algorithm:
  ceil = null
  curr = root
  
  while curr:
      if curr.val == target:
          return curr.val  // Exact match
      
      if curr.val < target:
          curr = curr.right  // Need larger
      else:
          ceil = curr.val    // Candidate
          curr = curr.left   // Try to find smaller ceil

Time: O(h), Space: O(1)

Example: BST = [1, 3, 6, 7, 8, 10, 14], target = 5

Step 1: At 8
  8 >= 5 → ceil = 8, go left

Step 2: At 3
  3 < 5 → go right

Step 3: At 6
  6 >= 5 → ceil = 6, go left

Step 4: null
  Return ceil = 6 ✓

FLOOR (Largest <= target)
==========================
Find the largest value that is <= target.

Key insight:
- If current <= target: it's a candidate, but check right for larger
- If current > target: go left to find smaller values

Algorithm:
  floor = null
  curr = root
  
  while curr:
      if curr.val == target:
          return curr.val  // Exact match
      
      if curr.val > target:
          curr = curr.left   // Need smaller
      else:
          floor = curr.val   // Candidate
          curr = curr.right  // Try to find larger floor

Time: O(h), Space: O(1)

Example: BST = [1, 3, 6, 7, 8, 10, 14], target = 5

Step 1: At 8
  8 > 5 → go left

Step 2: At 3
  3 <= 5 → floor = 3, go right

Step 3: At 6
  6 > 5 → go left

Step 4: null
  Return floor = 3 ✓

WHY THE DIRECTIONS?

Ceil:
- When node >= target: it's a candidate
- Go LEFT to find potentially smaller ceil
- We want smallest value >= target

Floor:
- When node <= target: it's a candidate
- Go RIGHT to find potentially larger floor
- We want largest value <= target

EDGE CASES:

1. Exact match: Return immediately
2. Target < min: ceil = min, floor = null
3. Target > max: ceil = null, floor = max
4. Empty tree: Return null
5. Single node: Both ceil and floor might be that node

COMPARISON:

Target = 5 in BST [1, 3, 6, 7, 8, 10, 14]

Ceil(5) = 6  (smallest >= 5)
Floor(5) = 3 (largest <= 5)

Target = 6 (exists in BST)

Ceil(6) = 6  (exact match)
Floor(6) = 6 (exact match)

Target = 0 (less than min)

Ceil(0) = 1  (min value)
Floor(0) = null

Target = 15 (greater than max)

Ceil(15) = null
Floor(15) = 14 (max value)

COMPLEXITY:

All operations: O(h) time
- Balanced BST: O(log n)
- Skewed BST: O(n)

Space:
- Iterative: O(1)
- Recursive: O(h) for stack

INTERVIEW TIPS:

1. Clarify if exact match should return that value
2. Ask about null/empty tree handling
3. Explain BST property usage
4. Walk through example step by step
5. Discuss iterative vs recursive trade-offs
6. Mention complexity for balanced vs skewed

COMMON MISTAKES:

1. Confusing ceil and floor directions
2. Not handling exact match
3. Not initializing result to null
4. Wrong comparison operators
5. Not considering edge cases

RELATED PROBLEMS:

1. Kth Smallest Element in BST
2. Closest Binary Search Tree Value
3. Search in BST
4. Inorder Successor in BST
5. Binary Search Tree Iterator
*/

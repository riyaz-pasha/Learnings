# DIY: Diameter of Binary Tree

## Problem statement

Given a binary tree, compute the length of its diameter — the length of the longest path between any two nodes in the tree. This path may or may not pass through the root. The length of a path is measured by the number of edges between its two end nodes.

### Input

```
        1
       / \
      2   3
     / \
    4   5
   /
  6
```

### Output

```java
4
```

(The path `6 → 4 → 2 → 1 → 3` has 4 edges.)

## Coding exercise

Implement `diameterOfBinaryTree(root)`, returning the diameter as an integer.

This is the exact same pattern as [Feature #6: Longest Route](06-feature-6-longest-route.md) — there, Uber found the longest route through the city's checkpoint tree; here it's the bare pattern with no story attached. Compute each node's height recursively, and at every node check whether the sum of its left and right subtree heights beats the best diameter seen so far.

## Solution

```java
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }
}

class Solution {
    private static int diameter;

    public static int diameterOfBinaryTree(TreeNode root) {
        diameter = 0;
        height(root);
        return diameter;
    }

    private static int height(TreeNode node) {
        if (node == null) return 0;

        int leftHeight = height(node.left);
        int rightHeight = height(node.right);

        diameter = Math.max(diameter, leftHeight + rightHeight);

        return Math.max(leftHeight, rightHeight) + 1;
    }

    public static void main(String[] args) {
        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(3);
        root.left.left = new TreeNode(4);
        root.left.right = new TreeNode(5);
        root.left.left.left = new TreeNode(6);

        System.out.println(diameterOfBinaryTree(root));
        // 4
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree.

- **Time:** `O(n)` — each node's height is computed exactly once.
- **Space:** `O(n)` in the worst case (a skewed tree) for the recursive call stack, `O(log n)` if balanced.

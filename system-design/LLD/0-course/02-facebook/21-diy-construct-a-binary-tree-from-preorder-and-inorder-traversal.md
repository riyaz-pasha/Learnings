# DIY: Construct a Binary Tree from Preorder and Inorder Traversal

## Problem statement

Given a `preorder` and an `inorder` traversal of a binary tree, construct and return the tree.

### Input

```java
preorder = [3, 9, 20, 15, 7]
inorder  = [9, 3, 15, 20, 7]
```

### Output (level-order, to show the shape)

```java
[3, 9, 20, 15, 7]
```

## Coding exercise

Implement `buildBinaryTree(preorder, inorder)`.

This is exactly [Feature #9: Recreating the Decision Tree](09-feature-9-recreating-the-decision-tree.md) — same recursive split (preorder's first element is the root; its position in `inorder` splits the rest into left/right subtrees) — just with integer node values instead of strings.

## Solution

```java
import java.util.*;

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode() {}
    TreeNode(int val) { this.val = val; }
}

class Solution {
    private static int preorderIndex;
    private static Map<Integer, Integer> inorderValueToIndex;
    private static int[] preorder;

    public static TreeNode buildBinaryTree(int[] preorderArr, int[] inorderArr) {
        preorder = preorderArr;
        preorderIndex = 0;

        inorderValueToIndex = new HashMap<>();
        for (int i = 0; i < inorderArr.length; i++) {
            inorderValueToIndex.put(inorderArr[i], i);
        }

        return build(0, inorderArr.length - 1);
    }

    private static TreeNode build(int left, int right) {
        if (left > right) {
            return null;
        }

        int rootVal = preorder[preorderIndex++];
        TreeNode root = new TreeNode(rootVal);

        int rootIndexInInorder = inorderValueToIndex.get(rootVal);

        root.left = build(left, rootIndexInInorder - 1);
        root.right = build(rootIndexInInorder + 1, right);

        return root;
    }

    // Helper: level-order traversal, just to display the constructed tree's shape.
    private static List<Integer> levelOrder(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            result.add(node.val);
            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }
        return result;
    }

    public static void main(String[] args) {
        int[] preorder = {3, 9, 20, 15, 7};
        int[] inorder = {9, 3, 15, 20, 7};

        TreeNode root = buildBinaryTree(preorder, inorder);
        System.out.println(levelOrder(root)); // [3, 9, 20, 15, 7]
    }
}
```

## Complexity measures

Let **n** be the number of nodes.

- **Time:** `O(n)`.
- **Space:** `O(n)` for the map, plus recursion depth up to `O(n)`.

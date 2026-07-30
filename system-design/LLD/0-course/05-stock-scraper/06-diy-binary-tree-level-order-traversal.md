# DIY: Binary Tree Level Order Traversal

## Problem statement

Given a binary tree, populate an array representing its level-by-level traversal. Values should read left to right within each level, then be joined into a single string.

### Input

```java
    3
   / \
  9  20
    /  \
   15   7
```

### Output

```java
"3 9 20 15 7"
```

## Coding exercise

Implement `traverse(root)`, where `root` is the root node of the binary tree. Return the level-by-level values as a single space-separated string.

Exactly [Feature #1: Traversing DOM Tree](01-feature-1-traversing-dom-tree.md) — a binary tree is just an n-ary tree where every node happens to have at most two children, so the same queue-based BFS applies directly.

## Solution

```java
import java.util.*;

class TreeNode {
    int data;
    TreeNode left;
    TreeNode right;

    TreeNode(int d) {
        data = d;
        left = null;
        right = null;
    }
}

class Solution {

    public static String traverse(TreeNode root) {
        StringBuilder result = new StringBuilder();
        if (root == null) {
            return result.toString();
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(node.data);
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
        }

        return result.toString();
    }

    public static void main(String[] args) {
        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(9);
        root.right = new TreeNode(20);
        root.right.left = new TreeNode(15);
        root.right.right = new TreeNode(7);

        System.out.println(traverse(root)); // "3 9 20 15 7"
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree.

- **Time:** `O(n)` — every node is enqueued and dequeued once.
- **Space:** `O(n)` — the queue can hold up to `O(n)` nodes at the tree's widest level, and the result string holds all `n` values.

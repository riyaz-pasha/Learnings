# DIY: Lowest Common Ancestor of a Binary Tree

## Problem statement

Given a binary tree, find the lowest common ancestor (LCA) of two given nodes in the tree.

### Input

```java
    3
   / \
  9  20
    /  \
   15   7

input = [3, 15, 7]   // root, node1, node2
```

### Output

```java
20
```

Node `20` is the LCA of nodes `15` and `7`.

## Coding exercise

Implement `LCA(root, node1, node2)`, where `root` is the root of the binary tree. Return the value of the LCA of `node1` and `node2`.

Same core idea as [Feature #2: Locating Stock Data](02-feature-2-locating-stock-data.md) — finding the ancestor where two nodes' paths converge. Since only the root is given here (no parent pointers), the natural approach is a bottom-up recursion instead of the parent-map walk used in the feature lesson.

## Solution

```java
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

    public static int LCA(TreeNode root, TreeNode node1, TreeNode node2) {
        TreeNode result = findLCA(root, node1, node2);
        return result == null ? -1 : result.data;
    }

    private static TreeNode findLCA(TreeNode node, TreeNode node1, TreeNode node2) {
        if (node == null || node == node1 || node == node2) {
            return node;
        }

        TreeNode left = findLCA(node.left, node1, node2);
        TreeNode right = findLCA(node.right, node1, node2);

        // node1 and node2 were found on opposite sides — this node is the LCA.
        if (left != null && right != null) {
            return node;
        }

        // Otherwise, whichever side found something is the answer so far.
        return left != null ? left : right;
    }

    public static void main(String[] args) {
        TreeNode n3 = new TreeNode(3);
        TreeNode n9 = new TreeNode(9);
        TreeNode n20 = new TreeNode(20);
        TreeNode n15 = new TreeNode(15);
        TreeNode n7 = new TreeNode(7);
        n3.left = n9;
        n3.right = n20;
        n20.left = n15;
        n20.right = n7;

        System.out.println(LCA(n3, n15, n7)); // 20
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree.

- **Time:** `O(n)` — in the worst case, the recursion visits every node once.
- **Space:** `O(h)` where `h` is the tree's height, for the recursion stack (up to `O(n)` for a skewed tree).

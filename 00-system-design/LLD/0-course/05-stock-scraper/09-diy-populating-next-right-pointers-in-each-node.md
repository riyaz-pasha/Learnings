# DIY: Populating Next Right Pointers in Each Node

## Problem statement

You are given a **perfect binary tree** (every leaf is on the same level, and every parent has exactly two children). The tree's node has an extra `next` pointer, initially `null` for every node.

Populate every node's `next` pointer to point at its immediate right neighbor on the same level. If there is no such neighbor, `next` stays `null`.

### Input

```java
     3
   /   \
  9     20
 / \    /  \
1   2 15   7
```

### Output

```java
     3 - null
   /   \
  9  -  20 - null
 / \    /  \
1 - 2-15 - 7 - null
```

## Coding exercise

Implement `traverse(root)`, returning the same root with every `next` pointer connected.

This is the DIY match for [Feature #3: Traversing DOM Tree II](03-feature-3-traversing-dom-tree-ii.md). Since the tree here is perfect (always exactly two children), it allows an even leaner version of that lesson's trick: once a level's `next` pointers are set, walk that level and directly wire `curr.left.next = curr.right`, and `curr.right.next = curr.next.left` when a right neighbor exists.

## Solution

```java
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode next;

    TreeNode(int v) {
        val = v;
    }
}

class Solution {

    public static TreeNode traverse(TreeNode root) {
        if (root == null) {
            return null;
        }

        TreeNode leftmost = root;
        while (leftmost.left != null) {
            TreeNode curr = leftmost;
            while (curr != null) {
                curr.left.next = curr.right;
                if (curr.next != null) {
                    curr.right.next = curr.next.left;
                }
                curr = curr.next;
            }
            leftmost = leftmost.left;
        }

        return root;
    }

    public static void main(String[] args) {
        TreeNode n3 = new TreeNode(3);
        TreeNode n9 = new TreeNode(9);
        TreeNode n20 = new TreeNode(20);
        TreeNode n1 = new TreeNode(1);
        TreeNode n2 = new TreeNode(2);
        TreeNode n15 = new TreeNode(15);
        TreeNode n7 = new TreeNode(7);
        n3.left = n9; n3.right = n20;
        n9.left = n1; n9.right = n2;
        n20.left = n15; n20.right = n7;

        traverse(n3);
        System.out.println(n3.next);          // null
        System.out.println(n9.next.val);      // 20
        System.out.println(n1.next.val);      // 2
        System.out.println(n2.next.val);      // 15
        System.out.println(n15.next.val);     // 7
        System.out.println(n7.next);          // null
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree.

- **Time:** `O(n)` — every node is visited once to wire up its neighbors.
- **Space:** `O(1)` extra — only a few pointers are used, no queue and no recursion stack.

# DIY: Populating Next Right Pointers in Each Node II

## Problem statement

You are given a binary tree — this time, **not necessarily perfect**: any node may have zero, one, or two children. As before, each node has a `next` pointer, initially `null`.

Populate every node's `next` pointer to point at its immediate right neighbor on the same level. If there is no such neighbor, `next` stays `null`.

### Input

```java
    3
   / \
  9  20
```

### Output

```java
    3 - null
   / \
  9--20 - null
```

## Coding exercise

Implement `traverse(root)`, returning the same root with every `next` pointer connected.

Same underlying pattern as [Feature #3: Traversing DOM Tree II](03-feature-3-traversing-dom-tree-ii.md) and its previous DIY sibling — but since nodes here can be missing a child, the shortcut of directly wiring `curr.left.next = curr.right` no longer works (one of them might not exist). This calls back to the general, structure-agnostic technique from Feature #3: sweep the current level using `next` pointers already set on it, and build up the next level's `next` chain (via a `prev`/leftmost tracker) as children are discovered, in whatever order they actually exist.

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
        TreeNode leftmost = root;

        while (leftmost != null) {
            TreeNode dummy = new TreeNode(0); // placeholder head for this level's child chain
            TreeNode tail = dummy;
            TreeNode curr = leftmost;

            while (curr != null) {
                if (curr.left != null) {
                    tail.next = curr.left;
                    tail = tail.next;
                }
                if (curr.right != null) {
                    tail.next = curr.right;
                    tail = tail.next;
                }
                curr = curr.next;
            }

            leftmost = dummy.next; // first node of the next level, if any
        }

        return root;
    }

    public static void main(String[] args) {
        TreeNode n3 = new TreeNode(3);
        TreeNode n9 = new TreeNode(9);
        TreeNode n20 = new TreeNode(20);
        n3.left = n9;
        n3.right = n20;

        traverse(n3);
        System.out.println(n3.next);      // null
        System.out.println(n9.next.val);  // 20
        System.out.println(n20.next);     // null
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree.

- **Time:** `O(n)` — every node is visited once, and each node contributes at most two pointer updates.
- **Space:** `O(1)` extra, aside from the constant-size `dummy` node created per level — no queue and no recursion stack.

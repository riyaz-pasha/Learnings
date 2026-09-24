# DIY: Range Sum of BST

## Problem statement

You are given the root of a binary search tree and two integers, `low` and `high`. Return the sum of the values of all nodes whose value falls between `low` and `high`, inclusive.

### Input

```java
//         15
//        /  \
//       7    20
//      / \   / \
//     3   9 16  25

low = 5
high = 20
```

### Output

```java
67
```

(The nodes in range `[5, 20]` are `7`, `9`, `15`, `16`, and `20` — `3` is below `low` and `25` is above `high`, so both are excluded. `7 + 9 + 15 + 16 + 20 = 67`.)

## Coding exercise

Implement `rangeSum(root, low, high)`.

This is the exact same pattern as [Feature #9: Products in Price Range](09-feature-9-products-in-price-range.md) — there, Amazon let a customer filter a product list by min and max price; here it's the bare pattern with no story attached. The BST ordering lets you prune whole subtrees: if the current node's value is below `low`, nothing in its left subtree can be in range, so only recurse right; if it's above `high`, only recurse left. Otherwise, add the node's value and recurse both ways.

## Solution

```java
class Solution {
    static class Node {
        int val;
        Node left, right;
        Node(int val) { this.val = val; }
    }

    public static int rangeSum(Node root, int low, int high) {
        if (root == null) {
            return 0;
        }
        if (root.val < low) {
            // Everything in the left subtree is even smaller — skip it.
            return rangeSum(root.right, low, high);
        }
        if (root.val > high) {
            // Everything in the right subtree is even bigger — skip it.
            return rangeSum(root.left, low, high);
        }
        return root.val + rangeSum(root.left, low, high) + rangeSum(root.right, low, high);
    }

    public static void main(String[] args) {
        Node root = new Node(15);
        root.left = new Node(7);
        root.right = new Node(20);
        root.left.left = new Node(3);
        root.left.right = new Node(9);
        root.right.left = new Node(16);
        root.right.right = new Node(25);

        System.out.println(rangeSum(root, 5, 20));
        // 67
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree.

- **Time:** `O(n)` worst case (a degenerate tree where every node is in range), but the BST pruning skips whole subtrees outside `[low, high]` in the average case.
- **Space:** `O(h)` — recursion stack depth, where `h` is the tree's height.

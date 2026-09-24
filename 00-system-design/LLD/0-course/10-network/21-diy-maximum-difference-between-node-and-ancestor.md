# DIY: Maximum Difference Between Node and Ancestor

## Problem statement

Given the root of a binary tree, find the maximum value `X` such that `X = |A.val - B.val|` for some two different nodes `A` and `B`, where `A` is an ancestor of `B`.

### Input

```java
    1
   / \
  2   0
 /
5
```

### Output

```
4
```

The maximum ancestor-descendant difference is `|1 - 5| = 4` (node `1` is an ancestor of node `5`).

## Coding exercise

Implement `maxAncestorDiff(root)`, returning the maximum absolute difference between any ancestor-descendant pair.

This is the exact same pattern as [Feature #8: Maximum Clock Skew](08-feature-8-maximum-clock-skew.md) — there, we found the largest clock-time gap between any two routers on a forwarding path; here it's the bare pattern, no networking story. A single top-down DFS carrying the max and min values seen along the current root-to-node path is enough — the largest gap always involves either that max or that min.

## Solution

```java
class TreeNode {
    int val;
    TreeNode left, right;
    TreeNode(int val) { this.val = val; }
}

class Solution {
    public static int maxAncestorDiff(TreeNode root) {
        return dfs(root, root.val, root.val);
    }

    private static int dfs(TreeNode node, int maxVal, int minVal) {
        if (node == null) {
            return maxVal - minVal;
        }
        maxVal = Math.max(maxVal, node.val);
        minVal = Math.min(minVal, node.val);
        return Math.max(dfs(node.left, maxVal, minVal), dfs(node.right, maxVal, minVal));
    }

    public static void main(String[] args) {
        TreeNode n1 = new TreeNode(1);
        TreeNode n2 = new TreeNode(2);
        TreeNode n0 = new TreeNode(0);
        TreeNode n5 = new TreeNode(5);
        n1.left = n2;
        n1.right = n0;
        n2.left = n5;

        System.out.println(maxAncestorDiff(n1));
        // 4
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree.

- **Time:** `O(n)` — a single DFS pass visits every node once.
- **Space:** `O(n)` — dominated by the recursion stack, up to n deep in the worst case (a tree shaped like a linked list).

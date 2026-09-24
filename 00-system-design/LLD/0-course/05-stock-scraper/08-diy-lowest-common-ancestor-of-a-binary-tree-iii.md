# DIY: Lowest Common Ancestor of a Binary Tree III

## Problem statement

Given two nodes of a binary tree, `node1` and `node2`, find their lowest common ancestor (LCA) — the lowest node that has both as descendants (a node counts as its own descendant). Every node has a reference to its `parent`, and node values are unique and positive.

Trees below are shown as their level-order array, with `-1` marking a missing node.

### Input

```java
// Sample Input 1:
//           3
//         /    \
//        5      1
//      /   \   /   \
//     6     2 0     8
//          / \
//         7   4
root = [3,5,1,6,2,0,8,-1,-1,7,4], node1 = 5, node2 = 1

// Sample Input 2:
//     1
//    /
//   2
root = [1,2], node1 = 1, node2 = 2
```

### Output

```java
// Sample Output 1:
3
// Sample Output 2:
1
```

## Coding exercise

Implement `lowestCommonAncestor(node1, node2)`, returning a reference to the LCA node.

This is the DIY match for [Feature #2: Locating Stock Data](02-feature-2-locating-stock-data.md) — since parent pointers are given here (just like the DOM nodes in that feature), the same "walk up from one node into a set, then walk up from the other until you land in that set" technique applies almost unchanged.

## Solution

```java
import java.util.*;

class Node {
    int val;
    Node left;
    Node right;
    Node parent;

    Node(int v) {
        val = v;
    }
}

class Solution {

    public static Node lowestCommonAncestor(Node node1, Node node2) {
        Set<Node> ancestorsOfNode1 = new HashSet<>();

        Node curr = node1;
        while (curr != null) {
            ancestorsOfNode1.add(curr);
            curr = curr.parent;
        }

        curr = node2;
        while (!ancestorsOfNode1.contains(curr)) {
            curr = curr.parent;
        }

        return curr;
    }

    public static void main(String[] args) {
        Node n3 = new Node(3);
        Node n5 = new Node(5);
        Node n1 = new Node(1);
        Node n6 = new Node(6);
        Node n2 = new Node(2);
        Node n0 = new Node(0);
        Node n8 = new Node(8);
        Node n7 = new Node(7);
        Node n4 = new Node(4);

        n3.left = n5; n3.right = n1; n5.parent = n3; n1.parent = n3;
        n5.left = n6; n5.right = n2; n6.parent = n5; n2.parent = n5;
        n1.left = n0; n1.right = n8; n0.parent = n1; n8.parent = n1;
        n2.left = n7; n2.right = n4; n7.parent = n2; n4.parent = n2;

        System.out.println(lowestCommonAncestor(n5, n1).val); // 3

        Node a1 = new Node(1);
        Node a2 = new Node(2);
        a1.left = a2;
        a2.parent = a1;
        System.out.println(lowestCommonAncestor(a1, a2).val); // 1
    }
}
```

## Complexity measures

Let **h** be the height of the tree.

- **Time:** `O(h)` — each upward walk follows parent pointers at most `h` steps, and the set lookup is `O(1)`.
- **Space:** `O(h)` — the ancestors set holds at most one entry per node from `node1` up to the root.

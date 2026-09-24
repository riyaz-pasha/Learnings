# DIY: All Nodes Distance K in Binary Tree

## Problem statement

Given a binary tree (with root node `root`), a `target` node, and an integer `K`, return an array of the values of all nodes that are exactly `K` distance from `target`.

### Input

```java
        3
       / \
      5   1
     /   / \
    2  15   7
   / \
  7   4

target = 5
K = 2
```

### Output

```
{7, 4, 1}
```

The nodes at distance 2 from target node `5` have the values `7`, `4`, and `1`.

## Coding exercise

Implement `distanceK(root, target, K)`, returning the values of all nodes exactly `K` distance from `target`.

This is the exact same pattern as [Feature #2: TTL Expiry](02-feature-2-ttl-expiry.md) — there, a message's TTL determined which devices it reached exactly TTL hops from a server; here it's the bare pattern, no networking story. A binary tree only lets you walk down to children, but distance has to account for walking up through parents too — so first convert the tree into an undirected graph, then run layered BFS from the target for exactly `K` layers.

## Solution

```java
import java.util.*;

class TreeNode {
    int val;
    TreeNode left, right;
    TreeNode(int val) { this.val = val; }
}

class Solution {
    public static List<Integer> distanceK(TreeNode root, TreeNode target, int K) {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        buildGraph(root, null, graph);

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> frontier = new ArrayDeque<>();
        frontier.add(target.val);
        visited.add(target.val);

        int distance = 0;
        while (!frontier.isEmpty()) {
            if (distance == K) {
                return new ArrayList<>(frontier);
            }
            int size = frontier.size();
            for (int i = 0; i < size; i++) {
                int node = frontier.poll();
                for (int neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                    if (visited.add(neighbor)) {
                        frontier.add(neighbor);
                    }
                }
            }
            distance++;
        }
        return new ArrayList<>(); // K is farther than any node in the tree
    }

    private static void buildGraph(TreeNode node, TreeNode parent, Map<Integer, List<Integer>> graph) {
        if (node == null) {
            return;
        }
        graph.putIfAbsent(node.val, new ArrayList<>());
        if (parent != null) {
            graph.get(node.val).add(parent.val);
            graph.get(parent.val).add(node.val);
        }
        buildGraph(node.left, node, graph);
        buildGraph(node.right, node, graph);
    }

    public static void main(String[] args) {
        TreeNode n3 = new TreeNode(3);
        TreeNode n5 = new TreeNode(5);
        TreeNode n1 = new TreeNode(1);
        TreeNode n2 = new TreeNode(2);
        TreeNode n15 = new TreeNode(15);
        TreeNode n1right = new TreeNode(7);
        TreeNode n7 = new TreeNode(7);
        TreeNode n4 = new TreeNode(4);
        n3.left = n5; n3.right = n1;
        n5.left = n2;
        n1.left = n15; n1.right = n1right;
        n2.left = n7; n2.right = n4;

        System.out.println(distanceK(n3, n5, 2));
        // [1, 7, 4]
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree.

- **Time:** `O(n)` — building the graph visits every node once, and the layered BFS visits every node at most once.
- **Space:** `O(n)` — for the graph's adjacency lists, the visited set, and the BFS queue.

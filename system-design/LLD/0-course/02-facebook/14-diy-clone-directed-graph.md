# DIY: Clone Directed Graph

## Problem statement

Given a reference to a node in a connected graph, return a deep copy (clone) of the whole graph. Each node has an `int data` and a `List<Node> neighbors`:

```java
class Node {
    public int data;
    public List<Node> neighbors = new ArrayList<Node>();
}
```

### Input

```java
G = {{2,4}, {1,3}, {2,4}, {1,3}}  // adjacency list, node i's neighbors listed at index i
```

### Output

```java
G' = {{2,4}, {1,3}, {2,4}, {1,3}}  // same structure, entirely new node objects
```

## Coding exercise

Implement `clone(rootNode)`.

Identical to [Feature #2: Copy Connections](02-feature-2-copy-connections.md) — DFS plus a `HashMap<originalNode, clonedNode>` to break cycles and avoid cloning any node twice.

## Solution

```java
import java.util.*;

class Node {
    public int data;
    public List<Node> neighbors = new ArrayList<>();

    public Node(int data) {
        this.data = data;
    }
}

class Graph {

    public static Node clone(Node rootNode) {
        return cloneRec(rootNode, new HashMap<>());
    }

    private static Node cloneRec(Node node, Map<Node, Node> completed) {
        if (node == null) {
            return null;
        }
        if (completed.containsKey(node)) {
            return completed.get(node);
        }

        Node copy = new Node(node.data);
        completed.put(node, copy);

        for (Node neighbor : node.neighbors) {
            copy.neighbors.add(cloneRec(neighbor, completed));
        }

        return copy;
    }

    public static void main(String[] args) {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        n1.neighbors.addAll(List.of(n2, n4));
        n2.neighbors.addAll(List.of(n1, n3));
        n3.neighbors.addAll(List.of(n2, n4));
        n4.neighbors.addAll(List.of(n1, n3));

        Node clonedRoot = clone(n1);
        System.out.println(clonedRoot != n1);                 // true
        System.out.println(clonedRoot.data);                  // 1
        System.out.println(clonedRoot.neighbors.get(0).data);  // 2
        System.out.println(clonedRoot.neighbors.get(0) != n2); // true
    }
}
```

## Complexity measures

Let **n** be the number of nodes reachable from the root.

- **Time:** `O(n)` — each node is cloned exactly once.
- **Space:** `O(n)` — the map, plus recursion depth up to `O(n)`.

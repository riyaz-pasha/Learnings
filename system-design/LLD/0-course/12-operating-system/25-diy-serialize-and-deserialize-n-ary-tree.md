# DIY: Serialize and Deserialize N-ary Tree

## Problem statement

Given an N-ary tree, implement:

- `serialize(root)` — encodes the tree into a string.
- `deserialize(data)` — decodes that string back into the original tree.

There's no fixed format required — any scheme works, as long as `deserialize(serialize(root))` reproduces the original tree.

### Input / Output

```java
// A tree rooted at 1, with children 3 (itself having children 5, 6), 2, and 4.
serialize(root)                      // some string, e.g. "1,3,2,4,5,6|3,2,0,0,0,0"
deserialize("1,3,2,4,5,6|3,2,0,0,0,0")  // reconstructs the same tree
```

## Coding exercise

Implement `serialize(root)` and `deserialize(data)`.

This is the exact same pattern as [Feature #7: Serialize and Deserialize File System](07-feature-7-serialize-and-deserialize-file-system.md) — there, the OS needed to sync a directory tree to a remote machine; here it's the bare N-ary tree version with no story attached. The approach is identical: a BFS level-order traversal recording each node's value and its children count, then replaying that same bookkeeping in reverse to rebuild the tree.

## Solution

```java
import java.util.*;

class Node {
    int val;
    List<Node> children;
    Node(int val) {
        this.val = val;
        children = new ArrayList<>();
    }
}

class Codec {
    public String serialize(Node root) {
        if (root == null) return "";
        List<String> vals = new ArrayList<>();
        List<String> counts = new ArrayList<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            vals.add(String.valueOf(cur.val));
            counts.add(String.valueOf(cur.children.size()));
            queue.addAll(cur.children);
        }
        return String.join(",", vals) + "|" + String.join(",", counts);
    }

    public Node deserialize(String data) {
        if (data.isEmpty()) return null;
        String[] parts = data.split("\\|", -1);
        String[] vals = parts[0].split(",");
        String[] counts = parts[1].split(",");

        Node root = new Node(Integer.parseInt(vals[0]));
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        int valIdx = 1, countIdx = 0;

        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            int childCount = Integer.parseInt(counts[countIdx++]);
            for (int i = 0; i < childCount; i++) {
                Node child = new Node(Integer.parseInt(vals[valIdx++]));
                cur.children.add(child);
                queue.add(child);
            }
        }
        return root;
    }

    public static void main(String[] args) {
        Node n5 = new Node(5), n6 = new Node(6);
        Node n3 = new Node(3);
        n3.children.addAll(Arrays.asList(n5, n6));
        Node root = new Node(1);
        root.children.addAll(Arrays.asList(n3, new Node(2), new Node(4)));

        Codec codec = new Codec();
        String data = codec.serialize(root);
        System.out.println(data);
        // 1,3,2,4,5,6|3,2,0,0,0,0

        Node restored = codec.deserialize(data);
        System.out.println(codec.serialize(restored));
        // 1,3,2,4,5,6|3,2,0,0,0,0 (round trip matches)
    }
}
```

## Complexity measures

Let **n** be the total number of nodes.

- **Time:** `O(n)` for both directions — a single BFS pass over the tree either way.
- **Space:** `O(n)` for both directions — the BFS queue and the serialized string both scale with the node count.

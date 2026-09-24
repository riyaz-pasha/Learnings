# DIY: Copy List with Random Pointer

## Problem statement

You are given the head of a linked list where each node has an extra `random` pointer, which can point to any node in the list (or to `null`). Create a deep copy of the list and return the head of the copy.

Each `Node` has an integer `val`, a `next` pointer, and a `random` pointer.

### Input

```java
// val -> random
// 7  -> null
// 13 -> 7
// 11 -> 10
// 10 -> 7
```

### Output

```java
// A deep copy: same vals and same random-target relationships,
// but every node (including random targets) is a brand-new object.
// 7  -> null
// 13 -> 7'
// 11 -> 10'
// 10 -> 7'
```

(The copy's `random` pointers must target nodes *within the copy*, not the original list.)

## Coding exercise

Implement `copyRandomList(head)`, returning the head of a fully independent deep copy of the list.

This is the exact same pattern as [Feature #4: Copy Product Data](04-feature-4-copy-product-data.md) — there, Amazon needed to clone product records that referenced other related products; here it's the bare pattern with no story attached. Make one pass to create a clone of every node (mapping original to copy in a `HashMap`), then a second pass to wire up each clone's `next` and `random` using that map.

## Solution

```java
import java.util.*;

class Solution {
    static class Node {
        int val;
        Node next;
        Node random;
        Node(int val) { this.val = val; }
    }

    public static Node copyRandomList(Node head) {
        if (head == null) return null;

        // First pass: create a bare clone for every original node.
        Map<Node, Node> originalToCopy = new HashMap<>();
        Node curr = head;
        while (curr != null) {
            originalToCopy.put(curr, new Node(curr.val));
            curr = curr.next;
        }

        // Second pass: wire up next/random using the map, since every
        // clone we need already exists in it.
        curr = head;
        while (curr != null) {
            Node copy = originalToCopy.get(curr);
            copy.next = originalToCopy.get(curr.next);
            copy.random = originalToCopy.get(curr.random);
            curr = curr.next;
        }

        return originalToCopy.get(head);
    }

    public static void main(String[] args) {
        Node n1 = new Node(7);
        Node n2 = new Node(13);
        Node n3 = new Node(11);
        Node n4 = new Node(10);
        n1.next = n2; n2.next = n3; n3.next = n4;
        n1.random = null;
        n2.random = n1;
        n3.random = n4;
        n4.random = n1;

        Node copyHead = copyRandomList(n1);

        Node orig = n1, copy = copyHead;
        while (orig != null) {
            System.out.println(
                "val=" + copy.val
                + " random=" + (copy.random == null ? "null" : copy.random.val)
                + " isDistinctNode=" + (copy != orig)
            );
            orig = orig.next;
            copy = copy.next;
        }
        // val=7 random=null isDistinctNode=true
        // val=13 random=7 isDistinctNode=true
        // val=11 random=10 isDistinctNode=true
        // val=10 random=7 isDistinctNode=true
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the list.

- **Time:** `O(n)` — two linear passes: one to clone nodes, one to wire pointers.
- **Space:** `O(n)` — the `HashMap` holds one entry per original node.

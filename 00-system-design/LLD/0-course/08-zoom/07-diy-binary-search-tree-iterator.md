# DIY: Binary Search Tree Iterator

## Problem statement

Implement a binary search tree iterator class called `BSTIterator`. The iterator is initialized with the root node of a BST, and supports two operations:

- `next()`: returns the next smallest number in the BST.
- `hasNext()`: returns true if a next smallest number exists.

### Input

The constructor takes the root node of a BST. `next()` and `hasNext()` take no arguments. Given this BST:

```
        10
       /  \
      5    12
       \
        7
```

and this sequence of calls:

```java
iterator = new BSTIterator(root);
iterator.hasNext();
iterator.next();
iterator.hasNext();
iterator.next();
iterator.hasNext();
iterator.next();
iterator.hasNext();
```

### Output

```
true
5
true
7
true
10
true
```

(`next()` returns the tree's values in ascending order: 5, 7, 10, 12 — one at a time, as `hasNext()`/`next()` are interleaved.)

## Coding exercise

Implement the `BSTIterator` class, given a `Node` with `int val`, `Node left`, and `Node right`.

This is the exact same pattern as [Feature #1: Display Meeting Lobby](01-feature-1-display-meeting-lobby.md) — there, Zoom paginated participant names ten at a time using an in-order traversal that could pause and resume; here it's the bare pattern, returning one value at a time instead of a page of ten. Keep a stack primed with the leftmost branch, pop for the next-smallest value, and push the popped node's right subtree's leftmost branch.

## Solution

```java
import java.util.*;

class BSTIterator {
    static class Node {
        int val;
        Node left, right;
        Node(int val) { this.val = val; }
    }

    private final Deque<Node> stack = new ArrayDeque<>();

    public BSTIterator(Node root) {
        pushAll(root);
    }

    private void pushAll(Node node) {
        while (node != null) {
            stack.push(node);
            node = node.left;
        }
    }

    public boolean hasNext() {
        return !stack.isEmpty();
    }

    public int next() {
        Node node = stack.pop();
        pushAll(node.right);
        return node.val;
    }

    public static void main(String[] args) {
        Node root = new Node(10);
        root.left = new Node(5);
        root.left.right = new Node(7);
        root.right = new Node(12);

        BSTIterator iterator = new BSTIterator(root);
        System.out.println(iterator.hasNext()); // true
        System.out.println(iterator.next());    // 5
        System.out.println(iterator.hasNext()); // true
        System.out.println(iterator.next());    // 7
        System.out.println(iterator.hasNext()); // true
        System.out.println(iterator.next());    // 10
        System.out.println(iterator.hasNext()); // true
    }
}
```

## Complexity measures

Let **n** be the number of nodes in the tree and **h** its height.

- **Time:** `O(1)` amortized per `next()`/`hasNext()` call — across the iterator's full lifetime, every node is pushed and popped exactly once, so the total work for a full traversal is `O(n)`, spread evenly across n calls.
- **Space:** `O(h)` — the stack only ever holds nodes along a root-to-leaf path, so its size is bounded by the tree's height (worst case `O(n)` for a fully skewed tree).

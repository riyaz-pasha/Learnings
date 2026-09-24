# DIY: My Calendar

## Problem statement

Implement `MyCalendar` with `book(start, end)`, returning whether the event was booked successfully (no conflict with an existing booking).

### Input

```java
MyCalendar.book(2, 4)
MyCalendar.book(6, 8)
MyCalendar.book(3, 5)
```

### Output

```java
true
true
false
```

## Coding exercise

Implement the `MyCalendar` class.

Exactly [Feature #3: Check if Meeting is Possible](03-feature-3-check-if-meeting-is-possible.md) — a BST of booked ranges, where each insertion attempt walks left/right based on non-overlap, and fails on genuine overlap.

## Solution

```java
class Node {
    int start;
    int end;
    Node left;
    Node right;

    Node(int start, int end) {
        this.start = start;
        this.end = end;
    }
}

class MyCalendar {
    private Node root;

    public MyCalendar() {
    }

    public boolean book(int start, int end) {
        Node newNode = new Node(start, end);
        if (root == null) {
            root = newNode;
            return true;
        }
        return addNode(root, newNode);
    }

    private boolean addNode(Node currentNode, Node newNode) {
        if (newNode.start >= currentNode.end) {
            if (currentNode.right == null) {
                currentNode.right = newNode;
                return true;
            }
            return addNode(currentNode.right, newNode);
        } else if (newNode.end <= currentNode.start) {
            if (currentNode.left == null) {
                currentNode.left = newNode;
                return true;
            }
            return addNode(currentNode.left, newNode);
        }
        return false;
    }

    public static void main(String[] args) {
        MyCalendar calendar = new MyCalendar();
        System.out.println(calendar.book(2, 4)); // true
        System.out.println(calendar.book(6, 8)); // true
        System.out.println(calendar.book(3, 5)); // false -- overlaps [2,4)
    }
}
```

## Complexity measures

Let **n** be the number of bookings made so far.

- **Time:** `O(n)` worst case, `O(log n)` with a balanced tree.
- **Space:** `O(n)`.

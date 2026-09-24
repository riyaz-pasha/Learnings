# DIY: LRU Cache

## Problem statement

Build an [LRU (least recently used)](https://en.wikipedia.org/wiki/Cache_replacement_policies#LRU) cache: a fixed-capacity structure that supports fast reads and writes, and when full, evicts the least recently used entry to make room for a new one.

## Coding exercise

Implement `Set(key, value)` (insert or update) and `Get(key)` (fetch, or signal "not found").

### Sample input

```java
lru = LRUCache(2)     // Initialize the cache
lru.Set(10, 20)        // Set the key 10 with value 20
lru.Get(10)            // Get the value against key 10
lru.print()            // Return the cached key-value pairs
```

### Sample output

```java
lru.Get(10) ==> 20
lru.print() ==> "(10, 20)"
```

This is the exact same design as [Feature #5: Fetch Most Recently Watched Titles](05-feature-5-fetch-most-recently-watched-titles.md), stripped of the Netflix story: a HashMap for O(1) lookup, paired with a doubly linked list ordered by recency (head = least recent, tail = most recent).

## Solution

```java
import java.util.HashMap;

class LinkedListNode {
    int key;
    int value;
    LinkedListNode prev;
    LinkedListNode next;

    LinkedListNode(int key, int value) {
        this.key = key;
        this.value = value;
    }
}

class MyLinkedList {
    private final LinkedListNode head;
    private final LinkedListNode tail;

    MyLinkedList() {
        head = new LinkedListNode(-1, -1);
        tail = new LinkedListNode(-1, -1);
        head.next = tail;
        tail.prev = head;
    }

    void addToTail(LinkedListNode node) {
        LinkedListNode prevTail = tail.prev;
        prevTail.next = node;
        node.prev = prevTail;
        node.next = tail;
        tail.prev = node;
    }

    void remove(LinkedListNode node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    LinkedListNode removeHead() {
        if (head.next == tail) {
            return null;
        }
        LinkedListNode first = head.next;
        remove(first);
        return first;
    }

    // Head-to-tail order: least recent first, most recent last.
    java.util.List<LinkedListNode> toList() {
        java.util.List<LinkedListNode> nodes = new java.util.ArrayList<>();
        for (LinkedListNode n = head.next; n != tail; n = n.next) {
            nodes.add(n);
        }
        return nodes;
    }
}

class LRUCache {
    private final int capacity;
    private final HashMap<Integer, LinkedListNode> cache;
    private final MyLinkedList order;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        cache = new HashMap<>(capacity);
        order = new MyLinkedList();
    }

    public Integer Get(int key) {
        if (!cache.containsKey(key)) {
            return null;
        }
        LinkedListNode node = cache.get(key);
        order.remove(node);
        order.addToTail(node);
        return node.value;
    }

    public void Set(int key, int value) {
        if (cache.containsKey(key)) {
            LinkedListNode node = cache.get(key);
            node.value = value;
            order.remove(node);
            order.addToTail(node);
            return;
        }

        if (cache.size() == capacity) {
            LinkedListNode evicted = order.removeHead();
            if (evicted != null) {
                cache.remove(evicted.key);
            }
        }

        LinkedListNode node = new LinkedListNode(key, value);
        order.addToTail(node);
        cache.put(key, node);
    }

    public String print() {
        StringBuilder sb = new StringBuilder();
        for (LinkedListNode node : order.toList()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("(").append(node.key).append(", ").append(node.value).append(")");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        LRUCache lru = new LRUCache(2);
        lru.Set(10, 20);
        System.out.println(lru.Get(10));  // 20
        System.out.println(lru.print());  // (10, 20)
    }
}
```

## Complexity measures

Let **k** be the cache's capacity.

- **`Get` / `Set`:** `O(1)` — HashMap lookup plus a known-node linked-list splice.
- **Space:** `O(k)`.

# DIY: LFU Cache

## Problem statement

Build an [LFU (least frequently used)](https://en.wikipedia.org/wiki/Least_frequently_used) cache: fixed capacity, and when full, evict the least *frequently* accessed entry (ties broken by least recent) to make room.

## Coding exercise

Implement `Set(key, value)` and `Get(key)`.

### Sample input

```java
lfu = LFUCache(2)     // Initialize the cache
lfu.Set(10, 20)        // Set the key 10 with value 20
lfu.Get(10)            // Get the value against key 10
lfu.print()            // Return the cached key-value pairs
```

### Sample output

```java
lfu.Get(10) ==> 20
lfu.print() ==> "(10, 20)"
```

Same design as [Feature #6: Fetch Most Frequently Watched Titles](06-feature-6-fetch-most-frequently-watched-titles.md): a `keyDict` for O(1) lookup, plus `freqDict` bucketing nodes by frequency (each bucket a recency-ordered linked list), plus a `minFreq` pointer so eviction never has to search.

## Solution

```java
import java.util.HashMap;

class LinkedListNode {
    int key;
    int value;
    int freq = 1;
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
    private int size = 0;

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
        size++;
    }

    void remove(LinkedListNode node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        size--;
    }

    LinkedListNode peekHead() {
        return head.next == tail ? null : head.next;
    }

    boolean isEmpty() {
        return size == 0;
    }
}

class LFUCache {
    private final int capacity;
    private int size;
    private int minFreq;
    private final HashMap<Integer, LinkedListNode> keyDict;
    private final HashMap<Integer, MyLinkedList> freqDict;

    public LFUCache(int capacity) {
        this.capacity = capacity;
        keyDict = new HashMap<>();
        freqDict = new HashMap<>();
    }

    public Integer Get(int key) {
        if (!keyDict.containsKey(key)) {
            return null;
        }
        LinkedListNode node = keyDict.get(key);
        touch(node);
        return node.value;
    }

    public void Set(int key, int value) {
        if (capacity == 0) {
            return;
        }

        if (keyDict.containsKey(key)) {
            LinkedListNode node = keyDict.get(key);
            node.value = value;
            touch(node);
            return;
        }

        if (size == capacity) {
            MyLinkedList minBucket = freqDict.get(minFreq);
            LinkedListNode evicted = minBucket.peekHead();
            minBucket.remove(evicted);
            keyDict.remove(evicted.key);
            size--;
        }

        LinkedListNode node = new LinkedListNode(key, value);
        keyDict.put(key, node);
        freqDict.computeIfAbsent(1, f -> new MyLinkedList()).addToTail(node);
        minFreq = 1;
        size++;
    }

    private void touch(LinkedListNode node) {
        int oldFreq = node.freq;
        MyLinkedList oldBucket = freqDict.get(oldFreq);
        oldBucket.remove(node);

        if (oldBucket.isEmpty() && minFreq == oldFreq) {
            minFreq++;
        }

        node.freq++;
        freqDict.computeIfAbsent(node.freq, f -> new MyLinkedList()).addToTail(node);
    }

    public String print() {
        StringBuilder sb = new StringBuilder();
        for (LinkedListNode node : keyDict.values()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("(").append(node.key).append(", ").append(node.value).append(")");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        LFUCache lfu = new LFUCache(2);
        lfu.Set(10, 20);
        System.out.println(lfu.Get(10)); // 20
        System.out.println(lfu.print()); // (10, 20)
    }
}
```

## Complexity measures

Let **n** be the cache's capacity.

- **`Get` / `Set`:** `O(1)` — HashMap lookups plus linked-list splices at known nodes.
- **Space:** `O(n)`.

/* Implement an LRU cache with time-based expiry for each entry. */

import java.util.HashMap;
import java.util.Map;

class Node {

    int key;
    int value;
    Node prev;
    Node next;
    long expiryTime;

    Node(int key, int value, long expiryTime) {
        this.key = key;
        this.value = value;
        this.expiryTime = expiryTime;
    }

}

class LRUCacheWithTimeBasedExpiry {

    private final int capacity;
    private final Node head;
    private final Node tail;
    private final Map<Integer, Node> keyToNodeMap;

    public LRUCacheWithTimeBasedExpiry(int capacity) {
        this.capacity = capacity;
        this.keyToNodeMap = new HashMap<>(capacity);

        // Create dummy head and tail nodes
        this.head = new Node(0, 0, 0);
        this.tail = new Node(0, 0, 0);

        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    public Integer get(int key) {
        Node node = this.keyToNodeMap.getOrDefault(key, null);
        if (node == null) {
            return null;
        }
        if (System.currentTimeMillis() > node.expiryTime) {
            this.removeNode(node);
            this.keyToNodeMap.remove(key);
            return null;
        }

        this.moveToHead(node);
        return node.value;
    }

    public void put(int key, int value, long expiryMillis) {
        long expiryTimeMillis = System.currentTimeMillis() + expiryMillis;

        if (this.keyToNodeMap.containsKey(key)) {
            Node node = this.keyToNodeMap.get(key);
            node.value = value;
            node.expiryTime = expiryTimeMillis;
            this.moveToHead(node);
        } else {
            if (this.keyToNodeMap.size() >= this.capacity) {
                Node lruNode = tail.prev;
                this.keyToNodeMap.remove(lruNode.key);
                this.removeNode(lruNode);
            }
            Node newNode = new Node(key, value, expiryTimeMillis);
            this.keyToNodeMap.put(key, newNode);
            this.addToHead(newNode);
        }
    }

    private void moveToHead(Node node) {
        this.removeNode(node);
        this.addToHead(node);
    }

    private void addToHead(Node node) {
        node.next = head.next;
        node.prev = head;

        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

}

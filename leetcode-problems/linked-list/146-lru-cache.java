import java.util.HashMap;
/*
 * Design a data structure that follows the constraints of a Least Recently Used
 * (LRU) cache.
 * 
 * Implement the LRUCache class:
 * 
 * LRUCache(int capacity) Initialize the LRU cache with positive size capacity.
 * int get(int key) Return the value of the key if the key exists, otherwise
 * return -1.
 * void put(int key, int value) Update the value of the key if the key exists.
 * Otherwise, add the key-value pair to the cache. If the number of keys exceeds
 * the capacity from this operation, evict the least recently used key.
 * The functions get and put must each run in O(1) average time complexity.
 * 
 * Example 1:
 * Input
 * ["LRUCache", "put", "put", "get", "put", "get", "put", "get", "get", "get"]
 * [[2], [1, 1], [2, 2], [1], [3, 3], [2], [4, 4], [1], [3], [4]]
 * Output
 * [null, null, null, 1, null, -1, null, -1, 3, 4]
 * Explanation
 * LRUCache lRUCache = new LRUCache(2);
 * lRUCache.put(1, 1); // cache is {1=1}
 * lRUCache.put(2, 2); // cache is {1=1, 2=2}
 * lRUCache.get(1); // return 1
 * lRUCache.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
 * lRUCache.get(2); // returns -1 (not found)
 * lRUCache.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
 * lRUCache.get(1); // return -1 (not found)
 * lRUCache.get(3); // return 3
 * lRUCache.get(4); // return 4
 */

class LRUCache {
    // Node class for doubly linked list
    class Node {
        int key;
        int value;
        Node prev;
        Node next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final HashMap<Integer, Node> cache;
    private final Node head;
    private final Node tail;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>(capacity);

        // Create dummy head and tail nodes
        this.head = new Node(0, 0);
        this.tail = new Node(0, 0);
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        Node node = cache.get(key);
        if (node == null) {
            return -1;
        }

        // Move the accessed node to head (most recently used)
        moveToHead(node);
        return node.value;
    }

    public void put(int key, int value) {
        Node existingNode = cache.get(key);

        if (existingNode != null) {
            // Update existing node's value and move to head
            existingNode.value = value;
            moveToHead(existingNode);
        } else {
            // Create new node
            Node newNode = new Node(key, value);

            if (cache.size() >= capacity) {
                // Remove least recently used node (tail's previous)
                Node lru = removeTail();
                cache.remove(lru.key);
            }

            // Add new node to head and cache
            cache.put(key, newNode);
            addToHead(newNode);
        }
    }

    // Helper method to add node right after head
    private void addToHead(Node node) {
        node.prev = head; // Link back to dummy head
        node.next = head.next; // Point forward to the current first node
        head.next.prev = node; // Let the old first node link back to new node
        head.next = node; // Head now links forward to new node
    }

    // Helper method to remove a node from linked list
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    // Helper method to move node to head
    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    // Helper method to remove tail node (LRU)
    private Node removeTail() {
        Node lru = tail.prev;
        removeNode(lru);
        return lru;
    }

}

// Example usage and test
class LRUCacheExample {

    public static void main(String[] args) {
        LRUCache lRUCache = new LRUCache(2);

        lRUCache.put(1, 1); // cache is {1=1}
        lRUCache.put(2, 2); // cache is {1=1, 2=2}
        System.out.println(lRUCache.get(1)); // return 1
        lRUCache.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
        System.out.println(lRUCache.get(2)); // returns -1 (not found)
        lRUCache.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
        System.out.println(lRUCache.get(1)); // return -1 (not found)
        System.out.println(lRUCache.get(3)); // return 3
        System.out.println(lRUCache.get(4)); // return 4
    }

}

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
/**
 * LRU Cache with TTL (Heap-based Expiration Eviction)
 *
 * Supports:
 *   get(key) -> O(1) amortized
 *   put(key, value, ttlMillis) -> O(log n) amortized
 *
 * Data Structures:
 *   1) HashMap for O(1) lookup
 *   2) Doubly Linked List for O(1) LRU ordering
 *   3) MinHeap (PriorityQueue) ordered by expireAt for TTL cleanup
 *
 * Key trick:
 *   Heap entries can become stale when the same key is updated.
 *   We use a version field per key to detect stale heap records.
 */
class LRUCacheWithTTLHeap {

    // Doubly Linked List Node (LRU)
    private static class Node {
        int key;
        int value;
        long expireAt;
        long version; // increments every time key is updated

        Node prev;
        Node next;

        Node(int key, int value, long expireAt, long version) {
            this.key = key;
            this.value = value;
            this.expireAt = expireAt;
            this.version = version;
        }
    }

    // Heap record (TTL tracking)
    private static class ExpiryEntry {
        int key;
        long expireAt;
        long version;

        ExpiryEntry(int key, long expireAt, long version) {
            this.key = key;
            this.expireAt = expireAt;
            this.version = version;
        }
    }

    private final int capacity;

    // key -> Node (for O(1) access)
    private final Map<Integer, Node> map;

    // LRU doubly linked list dummy nodes
    private final Node head;
    private final Node tail;

    // MinHeap ordered by earliest expireAt
    private final PriorityQueue<ExpiryEntry> expiryHeap;

    public LRUCacheWithTTLHeap(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();

        // Dummy nodes for easier LRU operations
        head = new Node(-1, -1, -1, -1);
        tail = new Node(-1, -1, -1, -1);

        head.next = tail;
        tail.prev = head;

        expiryHeap = new PriorityQueue<>(Comparator.comparingLong(e -> e.expireAt));
    }

    /**
     * Returns the value if present and not expired.
     * Otherwise returns -1.
     *
     * Time: O(1) amortized + cleanup cost
     */
    public int get(int key) {
        cleanupExpired();

        Node node = map.get(key);
        if (node == null) return -1;

        if (isExpired(node)) {
            removeNode(node);
            map.remove(key);
            return -1;
        }

        // Mark as most recently used
        moveToFront(node);

        return node.value;
    }

    /**
     * Insert or update key with TTL.
     *
     * ttlMillis: duration from now the key remains valid.
     *
     * Time: O(log n) amortized
     */
    public void put(int key, int value, long ttlMillis) {
        cleanupExpired();

        long expireAt = System.currentTimeMillis() + ttlMillis;

        // Update existing key
        if (map.containsKey(key)) {
            Node node = map.get(key);

            node.value = value;
            node.expireAt = expireAt;
            node.version++; // invalidate older heap entries

            // Push new expiry record into heap
            expiryHeap.offer(new ExpiryEntry(key, expireAt, node.version));

            // Mark as most recently used
            moveToFront(node);

            return;
        }

        // If capacity full, evict LRU (after cleanup)
        if (map.size() >= capacity) {
            evictLRU();
        }

        // Create new node
        Node node = new Node(key, value, expireAt, 0);

        map.put(key, node);
        addToFront(node);

        // Add expiry record into heap
        expiryHeap.offer(new ExpiryEntry(key, expireAt, node.version));
    }

    // -------------------------------------------------------
    // Core TTL cleanup logic (Heap based)
    // -------------------------------------------------------

    /**
     * Removes expired keys from cache by checking the heap.
     *
     * Heap gives earliest expiration first, so we can pop
     * until top is not expired.
     *
     * We also handle stale heap entries using version checks.
     */
    private void cleanupExpired() {
        long now = System.currentTimeMillis();

        while (!expiryHeap.isEmpty()) {
            ExpiryEntry top = expiryHeap.peek();

            // If earliest expiry is still in the future, stop cleanup
            if (top.expireAt > now) {
                break;
            }

            expiryHeap.poll();

            Node node = map.get(top.key);

            // Key already removed (maybe evicted earlier)
            if (node == null) continue;

            // Stale heap entry (node updated after this entry was inserted)
            if (node.version != top.version) continue;

            // If actually expired, remove from LRU + map
            if (node.expireAt <= now) {
                removeNode(node);
                map.remove(node.key);
            }
        }
    }

    private boolean isExpired(Node node) {
        return System.currentTimeMillis() > node.expireAt;
    }

    // -------------------------------------------------------
    // LRU Helpers
    // -------------------------------------------------------

    private void evictLRU() {
        Node lru = tail.prev;
        if (lru == head) return;

        removeNode(lru);
        map.remove(lru.key);

        // NOTE:
        // We do NOT remove it from heap explicitly.
        // Its heap entry will become stale and ignored later.
    }

    private void moveToFront(Node node) {
        removeNode(node);
        addToFront(node);
    }

    private void addToFront(Node node) {
        Node first = head.next;

        head.next = node;
        node.prev = head;

        node.next = first;
        first.prev = node;
    }

    private void removeNode(Node node) {
        Node p = node.prev;
        Node n = node.next;

        p.next = n;
        n.prev = p;

        node.prev = null;
        node.next = null;
    }
}


/**
 * LRU Cache with TTL (Time To Live)
 *
 * get(key): O(1)
 * put(key,value,ttl): O(1) amortized
 *
 * Data Structures:
 * - HashMap for O(1) lookup
 * - Doubly Linked List for O(1) LRU ordering
 *
 * Each entry has expireAt timestamp.
 */
class LRUCacheWithTTL {

    private static class Node {
        int key;
        int value;
        long expireAt;

        Node prev;
        Node next;

        Node(int key, int value, long expireAt) {
            this.key = key;
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private final int capacity;
    private final Map<Integer, Node> map;

    // Dummy head/tail for easier linked list operations
    private final Node head;
    private final Node tail;

    public LRUCacheWithTTL(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();

        head = new Node(-1, -1, -1);
        tail = new Node(-1, -1, -1);

        head.next = tail;
        tail.prev = head;
    }

    /**
     * Returns value if key exists and not expired.
     * If expired, removes it and returns -1.
     */
    public int get(int key) {
        Node node = map.get(key);

        if (node == null) {
            return -1;
        }

        // If expired, remove from cache
        if (isExpired(node)) {
            removeNode(node);
            map.remove(key);
            return -1;
        }

        // Move to MRU position
        moveToFront(node);

        return node.value;
    }

    /**
     * Insert or update key with TTL.
     * ttlMillis defines how long this key is valid from now.
     */
    public void put(int key, int value, long ttlMillis) {
        long expireAt = System.currentTimeMillis() + ttlMillis;

        // If key exists, update and move to front
        if (map.containsKey(key)) {
            Node node = map.get(key);

            node.value = value;
            node.expireAt = expireAt;

            moveToFront(node);
            return;
        }

        // Cleanup expired entries from LRU side before eviction
        cleanupExpiredFromTail();

        // If still full, evict LRU
        if (map.size() >= capacity) {
            evictLRU();
        }

        Node newNode = new Node(key, value, expireAt);
        map.put(key, newNode);
        addToFront(newNode);
    }

    // ---------------- Helper Methods ----------------

    private boolean isExpired(Node node) {
        return System.currentTimeMillis() > node.expireAt;
    }

    /**
     * Removes expired nodes starting from LRU end.
     * This is efficient because expired keys tend to accumulate near tail.
     */
    private void cleanupExpiredFromTail() {
        Node curr = tail.prev;

        while (curr != head && isExpired(curr)) {
            Node prev = curr.prev;
            removeNode(curr);
            map.remove(curr.key);
            curr = prev;
        }
    }

    private void evictLRU() {
        Node lru = tail.prev;
        if (lru == head)
            return;

        removeNode(lru);
        map.remove(lru.key);
    }

    private void moveToFront(Node node) {
        removeNode(node);
        addToFront(node);
    }

    private void addToFront(Node node) {
        Node first = head.next;

        head.next = node;
        node.prev = head;

        node.next = first;
        first.prev = node;
    }

    private void removeNode(Node node) {
        Node p = node.prev;
        Node n = node.next;

        p.next = n;
        n.prev = p;

        node.prev = null;
        node.next = null;
    }
}

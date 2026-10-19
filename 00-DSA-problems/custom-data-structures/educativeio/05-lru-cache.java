/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Implement an LRU (Least Recently Used) cache.
 * - Init(capacity): Initializes the cache with a fixed capacity.
 * - Set(key, value): Adds a new pair or updates an existing key. If full, evicts LRU.
 * - Get(key): Returns the value, or -1 if not found. (Also marks key as recently used).
 * Constraints: capacity in [1, 3000], keys/values >= 0, up to 200,000 calls.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. When I `Set` a key that already exists, does that count as a "use" that updates its LRU status?
 *    (Yes, updating a value makes it the Most Recently Used).
 * 2. Is `capacity` guaranteed to be strictly greater than 0?
 *    (Constraint says >= 1, so we don't have to handle 0-capacity edge cases).
 * 3. Should we expect concurrent access from multiple threads?
 *    (Usually no for standard algorithms interviews, but if yes, we'd need ReentrantLocks or ConcurrentHashMap).
 * 4. Since values are >= 0, is it safe to return -1 for a cache miss?
 *    (Yes, -1 unambiguously indicates "not found").
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement: We need O(1) key-value lookup AND O(1) ordering updates. 
 *   The binding constraint is the tension between fast lookups (Hash Maps have no order) 
 *   and fast reordering (Arrays/Lists take O(N) to find or shift elements).
 * 
 * --- Approach 1: Array of Objects with Timestamps (Brute Force) ---
 * 1. Intuition: Store elements in a list/array. Tag each with a "last used time".
 * 2. Try: Use `class Node { key, value, timestamp }` in an array of size `capacity`. 
 *    Global `int time = 0`. On `Set`/`Get`, increment `time` and update the node's timestamp.
 * 3. Why it works: It definitively tracks what was used least recently (smallest timestamp).
 * 4. Why it's too costly: To `Get`, we must linearly scan the array to find the key. 
 *    To evict, we must linearly scan to find the minimum timestamp.
 * 5. Bottleneck: Linear search for keys and linear search for the oldest element.
 * 
 * Time Complexity: Get: O(C) — must scan capacity C to find the key. Set: O(C) — must scan to find key or find LRU.
 * Space Complexity: O(C) — Array of size C.
 * 
 * --- Approach 2: HashMap + Standard Queue/List (Intermediate) ---
 * 1. Intuition: Fix the lookup bottleneck. A HashMap gives O(1) lookups.
 * 2. Try: Use a `HashMap<Integer, Integer>` for values, and a `LinkedList<Integer>` 
 *    or `ArrayList<Integer>` for order.
 *    `Get`: Check map. If exists, find key in list, remove it, append to end.
 * 3. Why it works: Lookups are instantaneous. 
 * 4. Why it's too costly: The `LinkedList.remove(Object key)` method in Java is O(N) 
 *    because it traverses from the head to find the node. `ArrayList.remove(index)` 
 *    is also O(N) because it shifts all subsequent elements.
 * 5. Bottleneck: Finding and severing a node from the middle of a standard list takes O(N).
 * 6. Observation: To remove an element from a linked list in O(1) time, we need a pointer 
 *    DIRECTLY to the node, AND the node must know its `prev` and `next` neighbors.
 * 
 * Time Complexity: Get: O(C) — Map is O(1), but shifting the list element to MRU is O(C). Set: O(C).
 * Space Complexity: O(C) — Map and List each store up to C elements.
 * 
 * --- Approach 3: HashMap + Doubly Linked List (Optimal) ---
 * 1. Intuition: Let the HashMap store pointers DIRECTLY to Doubly Linked List nodes.
 * 2. Try: `HashMap<Integer, Node>`. Node has `prev` and `next`. 
 *    `Get`: Map lookup gets the Node in O(1). Because it's doubly linked, we can sever 
 *    it from its neighbors in O(1) (`node.prev.next = node.next`) and move it to the head.
 * 3. Why it works: It perfectly bridges O(1) retrieval with O(1) arbitrary repositioning.
 * 4. Which to write in an interview?
 *    Approach 3. It is the gold standard for LRU Cache. Approach 2 is a common trap 
 *    that candidates fall into thinking Java's `LinkedList` will magically give them O(1).
 * 
 * Time Complexity: O(1) for both Get and Set — HashMap lookup is O(1), DLL pointer swaps are O(1).
 * Space Complexity: O(C) — Map and DLL store at most C elements.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Capacity of 1: Evictions happen immediately upon a second insertion.
 * - Overwriting an existing key: Must update the value AND bump it to Most Recently Used. 
 *   (Do not increase the cache size/count).
 * - Getting a non-existent key: Simply return -1. Do not crash or alter the list.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & PITFALLS
 * ============================================================================
 * > Key Insight: "Map the Key to the Node, not the Value." 
 *   If the HashMap points to the node in the DLL, you jump directly to memory and manipulate 
 *   its `prev` and `next` pointers without ever traversing the list.
 * 
 * Diagram: The Dummy Nodes Strategy
 * To avoid endless `if (head == null)` or `if (node.prev != null)` checks, 
 * we sandwich the real data between a DummyHead and DummyTail.
 * MRU (Most Recently Used) is right after DummyHead.
 * LRU (Least Recently Used) is right before DummyTail.
 * 
 * [Head] <-> [Node(1,1)] <-> [Node(2,2)] <-> [Tail]
 *   ^MRU                           ^LRU
 * 
 * Pitfalls:
 * - Storing ONLY the `value` in the Node. When eviction happens, you delete the node 
 *   next to the Tail. But wait! You must also delete it from the HashMap. If the Node 
 *   doesn't store its own `key`, you won't know which key to remove from the map.
 * - Forgetting to move the node to MRU when updating an existing key via `Set`.
 * 
 * Pattern Recognition: "O(1) access + O(1) ordering based on recency/frequency" 
 * -> HashMap + Doubly Linked List.
 * Transfers to: LFU Cache (requires Map + DLL of DLLs), Design In-Memory File System, Browser History.
 * 
 * Interview Script:
 * "To achieve O(1) for both get and set, I need a HashMap to find items instantly, and a 
 * Doubly Linked List to reorder items instantly. The HashMap will map integer keys directly 
 * to DLL Nodes. To make removing nodes clean, I'll use dummy head and tail nodes. When a key 
 * is accessed or added, I'll move it right behind the head. When capacity is breached, I'll 
 * drop the node right before the tail and remove its corresponding key from the map."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: How would you make this thread-safe?
 * A1: Use a `ConcurrentHashMap` for the map, but the DLL still requires synchronization. 
 *     I'd wrap the `Get` and `Set` methods in a `ReentrantReadWriteLock` or use a standard `synchronized` 
 *     block. Alternatively, split the cache into segments (like Guava Cache) to reduce lock contention.
 * 
 * Q2: How is an LFU (Least Frequently Used) cache different?
 * A2: LFU tracks counts, not just time. We would need a Map of Keys -> Nodes, AND a 
 *     Map of Frequencies -> DoublyLinkedLists. It is significantly more complex to maintain 
 *     in strictly O(1) time.
 * 
 * Q3: What if we want entries to expire after 5 minutes? (TTL)
 * A3: We'd store an expiration timestamp in the Node. On `Get`, we check if `now() > node.expiry`. 
 *     If yes, we remove it and return -1. For memory cleanup, a background thread could sweep the 
 *     tail periodically, or we do lazy cleanup upon insertions.
 */

import java.util.HashMap;
import java.util.Map;

public class LRUCacheStudy {

    /**
     * Our Doubly Linked List Node.
     * We MUST store the `key` here so that when we evict from the list,
     * we know which key to delete from the HashMap.
     */
    private class Node {
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
    // Maps the key directly to the DLL Node for O(1) access
    private final Map<Integer, Node> cache;
    
    // Dummy pointers to avoid null edge cases when adding/removing
    private final Node head;
    private final Node tail;

    /**
     * Init(capacity): Initializes an LRU cache with the capacity size.
     */
    public LRUCacheStudy(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        
        // Initialize dummy head and tail and link them together
        this.head = new Node(-1, -1);
        this.tail = new Node(-1, -1);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Get(key): Returns the value of the key, or -1 if the key does not exist.
     */
    public int get(int key) {
        if (!cache.containsKey(key)) {
            return -1;
        }
        
        Node node = cache.get(key);
        // It was recently used, so we remove it from its current position...
        removeNode(node);
        // ...and move it to the MRU position (right after head)
        moveToHead(node);
        
        return node.value;
    }

    /**
     * Set(key, value): Adds a new key-value pair or updates an existing key.
     */
    public void set(int key, int value) {
        if (cache.containsKey(key)) {
            // Update existing node
            Node node = cache.get(key);
            node.value = value;
            
            // Mark as most recently used
            removeNode(node);
            moveToHead(node);
        } else {
            // Create a new node
            Node newNode = new Node(key, value);
            cache.put(key, newNode);
            moveToHead(newNode);
            
            // If we exceeded capacity, evict the LRU node
            if (cache.size() > capacity) {
                // The LRU node is the one right before the dummy tail
                Node lruNode = tail.prev;
                removeNode(lruNode);
                
                // This is WHY we store the key in the Node class! 
                // We need to remove it from the HashMap as well.
                cache.remove(lruNode.key);
            }
        }
    }

    // ============================================================================
    // HELPER METHODS (The core of the DLL operations)
    // ============================================================================
    
    /**
     * Removes a node from the doubly linked list.
     * Time: O(1)
     * e.g., A <-> Node <-> B becomes A <-> B
     */
    private void removeNode(Node node) {
        Node prevNode = node.prev;
        Node nextNode = node.next;
        
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
    }

    /**
     * Inserts a node right after the dummy head (MRU position).
     * Time: O(1)
     * e.g., Head <-> A becomes Head <-> Node <-> A
     */
    private void moveToHead(Node node) {
        Node oldFirst = head.next;
        
        // Link node to head and oldFirst
        node.prev = head;
        node.next = oldFirst;
        
        // Link head and oldFirst back to node
        head.next = node;
        oldFirst.prev = node;
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Edge Case Testing
    // ============================================================================
    public static void main(String[] args) {
        System.out.println("--- Testing LRU Cache (Capacity 2) ---");
        LRUCacheStudy lru = new LRUCacheStudy(2);

        // 1. Basic Set and Get
        lru.set(1, 10);
        lru.set(2, 20);
        System.out.println("Get 1 (Expect 10): " + lru.get(1)); 
        // DLL is now: Head <-> 1 <-> 2 <-> Tail. (1 is MRU)

        // 2. Trigger Eviction
        lru.set(3, 30); // Cache is full. Key 1 is MRU, Key 2 is LRU. Evicts 2.
        System.out.println("Get 2 (Expect -1, evicted): " + lru.get(2)); 
        
        // 3. Update Existing Key (Must update value AND move to MRU)
        lru.set(1, 100); 
        // DLL is now: Head <-> 1 <-> 3 <-> Tail. (1 is MRU again)
        lru.set(4, 40); // Evicts 3, since 1 was just updated.
        
        System.out.println("Get 1 (Expect 100): " + lru.get(1));
        System.out.println("Get 3 (Expect -1, evicted): " + lru.get(3));
        System.out.println("Get 4 (Expect 40): " + lru.get(4));

        System.out.println("\n--- Testing Edge Case: Capacity 1 ---");
        LRUCacheStudy singleLru = new LRUCacheStudy(1);
        singleLru.set(1, 10);
        singleLru.set(2, 20); // Immediately evicts 1
        System.out.println("Get 1 (Expect -1): " + singleLru.get(1));
        System.out.println("Get 2 (Expect 20): " + singleLru.get(2));
    }
}

/**
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core Pattern: HashMap + Doubly Linked List with Dummy Nodes.
 * Key Observation: Java's standard LinkedList `.remove(Object)` is O(N). To achieve O(1), 
 * we must own the Node class and map directly to memory addresses via the HashMap.
 * What to Memorize: The node MUST store both `key` and `value`. Dummy Head/Tail prevent null checks.
 * Most Common Trap: Forgetting to delete the evicted node's key from the HashMap.
 * Mental Trigger: "O(1) Eviction by order" -> "Map to DLL".
 * ============================================================================
 */









import java.util.HashMap;
import java.util.Map;

class LRUCache {

    /*
     * Each node must preserve BOTH the key and the value.
     *
     * Why?
     * - The value is returned by get(key).
     * - The key is required to remove an evicted entry from the HashMap.
     */
    private static class CacheEntry {
        int key;
        int value;

        CacheEntry(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    /*
     * LinkedList stores CacheEntry objects.
     *
     * HEAD = most recently used (MRU).
     * TAIL = least recently used (LRU).
     *
     * Your map directly points to each entry's linked-list node.
     */
    private final LinkedList<CacheEntry> list;
    private final Map<Integer, LinkedListNode<CacheEntry>> map;
    private final int capacity;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be positive");
        }

        this.capacity = capacity;
        this.list = new LinkedList<>();
        this.map = new HashMap<>();
    }

    public int get(int key) {

        // Step 1: Find the node directly through the HashMap.
        LinkedListNode<CacheEntry> node = map.get(key);

        // Step 2: Return -1 if the key is absent.
        if (node == null) {
            return -1;
        }

        // Step 3: A successful access makes this entry MRU.
        list.remove(node);
        list.addFirst(node);

        // Step 4: Return the VALUE, not the key.
        return node.data.value;

        /*
         * Time: O(1) average.
         * HashMap lookup, node removal, and insertion at the head
         * all take O(1) time.
         */
    }

    public void set(int key, int value) {

        // CASE 1: The key already exists.
        if (map.containsKey(key)) {

            LinkedListNode<CacheEntry> node = map.get(key);

            // Update only the value; preserve the key.
            node.data.value = value;

            // Updating also makes the entry most recently used.
            list.remove(node);
            list.addFirst(node);

            // No eviction is needed because the size did not increase.
            return;
        }

        // CASE 2: New key and cache is full.
        // Use >= as a defensive check before inserting a new entry.
        if (list.size() >= capacity) {

            // The tail contains the least recently used entry.
            LinkedListNode<CacheEntry> lastNode = list.getLast();

            // Remove the evicted KEY from the map.
            map.remove(lastNode.data.key);

            // Remove the corresponding node from the list.
            list.removeLast();
        }

        // CASE 3: Insert the new entry at the MRU position.
        CacheEntry entry = new CacheEntry(key, value);
        list.insertAtHead(entry);

        // The new head is the node associated with this key.
        map.put(key, list.getFirst());

        /*
         * Time: O(1) average.
         * Space: O(1) additional per insertion; total cache storage
         * remains O(capacity).
         */
    }


    public class LinkedListNode<T> {
        public T data;
        public LinkedListNode<T> next;
        public LinkedListNode<T> prev;

        public LinkedListNode(T dataVal) {
            this.data = dataVal;
            this.next = null;
            this.prev = null;
        }
    }

    public class LinkedList<T> {
        private LinkedListNode<T> head;
        private LinkedListNode<T> tail;
        private int length;

        public LinkedList() {
            this.head = null;
            this.tail = null;
        }

        public int size() {
            return this.length;
        }

        public void insertAtHead(T data) {
            LinkedListNode<T> newNode = new LinkedListNode<T>(data);
            if (this.head == null) {
                this.head = newNode;
                this.tail = newNode;
            } else {
                newNode.next = this.head;
                this.head.prev = newNode;
                this.head = newNode;
            }
            this.length++;
        }

        public void insertAtTail(T data) {
            LinkedListNode<T> newNode = new LinkedListNode<T>(data);
            if (this.head == null) {
                this.head = newNode;
                this.tail = newNode;
            } else {
                this.tail.next = newNode;
                newNode.prev = this.tail;
                this.tail = newNode;
            }
            this.length++;
        }

        public void addFirst(LinkedListNode<T> newNode) {
            if (this.head == null) {
                this.head = newNode;
                this.tail = newNode;
            } else {
                newNode.next = this.head;
                this.head.prev = newNode;
                this.head = newNode;
            }
            this.length++;
        }

        public void addLast(LinkedListNode<T> newNode) {
            if (this.head == null) {
                this.head = newNode;
                this.tail = newNode;
            } else {
                this.tail.next = newNode;
                newNode.prev = this.tail;
                this.tail = newNode;
            }
            this.length++;
        }

        public void remove(T data) {
            LinkedListNode<T> tmp = this.head;
            while (tmp != null) {
                if (tmp.data == data) {
                    this.remove(tmp);
                    return;
                }
                tmp = tmp.next;
            }
        }

        public void remove(LinkedListNode<T> node) {
            if (node == null)
                return;
            if (node.prev != null)
                node.prev.next = node.next;
            if (node.next != null)
                node.next.prev = node.prev;
            if (node == this.head)
                this.head = this.head.next;
            if (node == this.tail) {
                this.tail = this.tail.prev;
                if (this.tail != null)
                    this.tail.next = null;
            }
            this.length--;
            node = null;
        }

        public void removeFirst() {
            this.remove(this.head);
        }

        public void removeLast() {
            this.remove(this.tail);
        }

        public LinkedListNode<T> getFirst() {
            return this.head;
        }

        public LinkedListNode<T> getLast() {
            return this.tail;
        }
    }


}



import java.util.HashMap;
import java.util.Map;

class LRUCache {

    /*
     * Each node represents one key-value pair in the cache.
     *
     * We use a doubly linked list because:
     * 1. We need to move recently accessed nodes to the front.
     * 2. We need to remove any known node in O(1) time.
     * 3. We need to evict the least recently used node from the end.
     *
     * prev points to the previous node.
     * next points to the next node.
     */
    private static class Node {
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

    /*
     * HashMap provides direct access to a node using its key.
     *
     * Example:
     * cache.get(10) -> Node(key=10, value=100)
     *
     * Without this map, we would need to traverse the linked list
     * to find a key, which would take O(n) time.
     *
     * Average lookup, insertion, and removal: O(1).
     */
    private final Map<Integer, Node> cache;

    /*
     * Dummy nodes simplify linked-list operations.
     *
     * head.next = Most Recently Used (MRU) node.
     * tail.prev = Least Recently Used (LRU) node.
     *
     * The dummy nodes do not represent actual cache entries.
     *
     * Empty list:
     *
     * head <-> tail
     *
     * List containing three entries:
     *
     * head <-> [MRU] <-> [Node] <-> [LRU] <-> tail
     */
    private final Node head;
    private final Node tail;

    public LRUCache(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be positive");
        }

        this.capacity = capacity;

        /*
         * Initially, the map is empty.
         *
         * The initial capacity helps size the HashMap, but it does not
         * change the O(1) average complexity of cache operations.
         */
        this.cache = new HashMap<>(capacity);

        // Create dummy boundary nodes.
        this.head = new Node(0, 0);
        this.tail = new Node(0, 0);

        /*
         * Connect the dummy nodes to create an empty doubly linked list.
         *
         * head <-> tail
         *
         * This eliminates special cases when inserting the first entry
         * or removing the last entry.
         */
        head.next = tail;
        tail.prev = head;

        /*
         * Time: O(1)
         * Auxiliary space: O(1) for the two dummy nodes.
         */
    }

    public int get(int key) {

        /*
         * STEP 1: Find the node using the HashMap.
         *
         * We do not traverse the linked list.
         *
         * Average lookup time: O(1).
         */
        Node node = cache.get(key);

        /*
         * STEP 2: Handle a cache miss.
         *
         * If the key does not exist, return -1 as required.
         *
         * The linked-list order remains unchanged because a missing
         * key was not accessed successfully.
         */
        if (node == null) {
            return -1;
        }

        /*
         * STEP 3: Mark the accessed node as most recently used.
         *
         * A successful get counts as usage, so this node must move
         * to the front of the list.
         *
         * Example before get(2):
         *
         * head <-> [1] <-> [2] <-> [3] <-> tail
         *          MRU              LRU
         *
         * Example after get(2):
         *
         * head <-> [2] <-> [1] <-> [3] <-> tail
         *          MRU              LRU
         *
         * The map does not need to change because the node itself
         * remains the same; only its linked-list position changes.
         */
        moveToHead(node);

        /*
         * STEP 4: Return the stored value.
         *
         * Time complexity:
         * - HashMap lookup: O(1) average.
         * - Move node to head: O(1).
         * - Return value: O(1).
         *
         * Total: O(1) average time.
         *
         * Auxiliary space: O(1); no new nodes are created.
         */
        return node.value;
    }

    public void put(int key, int value) {

        /*
         * First check whether the key already exists.
         *
         * This lookup lets us distinguish between updating an existing
         * entry and inserting a completely new entry.
         */
        Node existingNode = cache.get(key);

        /*
         * CASE 1: The key already exists.
         *
         * Example:
         * put(1, 100) when key 1 already stores value 50.
         *
         * We must update the value, not create a duplicate node.
         * Updating the entry also makes it the most recently used.
         */
        if (existingNode != null) {

            // Update the existing entry's value.
            existingNode.value = value;

            // Refresh its recency by moving it to the front.
            moveToHead(existingNode);

            /*
             * No eviction is necessary because the number of entries
             * has not increased.
             *
             * Time: O(1) average.
             * Auxiliary space: O(1).
             */
            return;
        }

        /*
         * CASE 2: The key is new.
         *
         * Create a new node to represent the key-value pair.
         * We will insert this node at the front because a newly
         * inserted entry is the most recently used entry.
         */
        Node newNode = new Node(key, value);

        /*
         * CASE 2A: The cache is full.
         *
         * cache.size() >= capacity checks whether we need to evict
         * an entry before inserting the new one.
         *
         * Because the list is ordered by recency:
         *
         * head.next = MRU
         * tail.prev = LRU
         *
         * Therefore, removeTail() identifies the eviction candidate
         * directly without searching the list.
         */
        if (cache.size() >= capacity) {

            // Remove the least recently used node from the list.
            Node lru = removeTail();

            /*
             * Remove the evicted key from the HashMap as well.
             *
             * Both structures must remain consistent.
             * If we remove only the list node, the map would still
             * contain a reference to an entry that was evicted.
             */
            cache.remove(lru.key);

            /*
             * Eviction complexity:
             * - Remove the tail node: O(1).
             * - Remove the map entry: O(1) average.
             *
             * Total: O(1) average.
             */
        }

        /*
         * CASE 2B: Insert the new entry.
         *
         * Add it to the HashMap so future get(key) calls can find
         * the node directly.
         */
        cache.put(key, newNode);

        /*
         * Insert the node immediately after the dummy head.
         *
         * This makes the new entry the MRU entry.
         *
         * Example:
         *
         * Before:
         * head <-> [A] <-> [B] <-> tail
         *
         * After inserting C:
         * head <-> [C] <-> [A] <-> [B] <-> tail
         *
         * C is MRU; B is LRU.
         */
        addToHead(newNode);

        /*
         * Total put(key, value) complexity:
         *
         * Existing key:
         * - HashMap lookup: O(1) average.
         * - Value update: O(1).
         * - Move to head: O(1).
         *
         * New key:
         * - HashMap lookup: O(1) average.
         * - Optional eviction: O(1) average.
         * - HashMap insertion: O(1) average.
         * - List insertion: O(1).
         *
         * Overall: O(1) average time.
         *
         * Auxiliary space: O(1) for the newly created node.
         * Total cache space: O(capacity).
         */
    }

    /*
     * Insert a node immediately after the dummy head.
     *
     * The node becomes the MRU entry.
     *
     * Before:
     *
     * head <-> A <-> tail
     *
     * After addToHead(B):
     *
     * head <-> B <-> A <-> tail
     *
     * Four pointer updates are sufficient, regardless of list size.
     */
    private void addToHead(Node node) {

        // Save the node currently occupying the MRU position.
        Node firstNode = head.next;

        // Connect the new node backward to the dummy head.
        node.prev = head;

        // Connect the new node forward to the old first node.
        node.next = firstNode;

        // Make the old first node point backward to the new node.
        firstNode.prev = node;

        // Make the dummy head point forward to the new node.
        head.next = node;

        /*
         * Time: O(1), because we update a fixed number of pointers.
         * Space: O(1), because we reuse the supplied node.
         */
    }

    /*
     * Remove a known node from the doubly linked list.
     *
     * Before:
     *
     * A <-> node <-> B
     *
     * After:
     *
     * A <-> B
     *
     * Since node already has prev and next references, we do not
     * need to traverse the list to find it.
     */
    private void removeNode(Node node) {

        // Connect the predecessor directly to the successor.
        node.prev.next = node.next;

        // Connect the successor directly to the predecessor.
        node.next.prev = node.prev;

        /*
         * The node is now detached from the list.
         *
         * Time: O(1).
         * Space: O(1).
         *
         * We do not need to update the HashMap because moving a node
         * or removing it from the list does not automatically imply
         * that the key is being evicted.
         */
    }

    /*
     * Move an existing node to the MRU position.
     *
     * We first detach the node from its current position and then
     * insert it immediately after head.
     *
     * Example:
     *
     * Before:
     * head <-> A <-> B <-> C <-> tail
     *
     * moveToHead(B):
     * head <-> B <-> A <-> C <-> tail
     *
     * This method is used by both get() and put() when the key exists.
     */
    private void moveToHead(Node node) {

        // Remove the node from its current position.
        removeNode(node);

        // Reinsert it as the most recently used entry.
        addToHead(node);

        /*
         * Time: O(1) + O(1) = O(1).
         * Auxiliary space: O(1).
         */
    }

    /*
     * Remove and return the least recently used node.
     *
     * The LRU entry is always tail.prev.
     *
     * Example:
     *
     * head <-> A <-> B <-> C <-> tail
     *                         ^
     *                         LRU
     *
     * removeTail() removes C and returns its node.
     *
     * The caller is responsible for removing the returned node's
     * key from the HashMap.
     */
    private Node removeTail() {

        // Identify the least recently used real node.
        Node lru = tail.prev;

        /*
         * This helper is called only when the cache is full and
         * capacity is positive, so the list contains a real node.
         */
        removeNode(lru);

        // Return the evicted node so the caller can remove its key.
        return lru;

        /*
         * Time: O(1), because tail.prev directly identifies the LRU.
         * Auxiliary space: O(1).
         */
    }
}

/*
 * FINAL COMPLEXITY SUMMARY
 *
 * Let C = cache capacity.
 *
 * Constructor:
 *   Time: O(1)
 *
 * get(key):
 *   Average time: O(1)
 *
 * put(key, value):
 *   Average time: O(1), including eviction.
 *
 * Total space:
 *   O(C), because the map and linked list store at most C entries.
 *
 * WHY THIS IS OPTIMAL:
 *
 * 1. HashMap finds a node by key without traversing the list.
 * 2. Doubly linked list moves any known node in O(1).
 * 3. Dummy head and tail make boundary operations simple.
 * 4. The least recently used entry is always available at tail.prev.
 *
 * Note: HashMap operations are O(1) on average, so the overall
 * operation guarantees are average-case rather than unconditional
 * worst-case guarantees.
 */


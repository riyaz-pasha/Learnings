/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design and implement a Least Frequently Used (LFU) cache.
 * 
 * Implement the LFUCache class:
 * - LFUCache(capacity): Initializes the object with a given capacity.
 * - int Get(key): Returns the value if it exists, otherwise -1.
 * - void Put(key, value): Updates the value if the key exists, or inserts it. 
 *   If the cache reaches capacity, evict the least frequently used key. 
 *   If there is a tie in frequency, evict the least RECENTLY used (LRU) key among them.
 * 
 * Target: O(1) average time complexity for both Get and Put.
 * Constraints: 0 <= capacity <= 10^4, 0 <= key, value <= 10^5, max 2000 calls.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Can the capacity be 0? 
 *    (Yes, the constraints say 0 <= capacity. If 0, we can never store anything; Put must do nothing and Get must return -1).
 * 2. When updating an existing key via Put(), does that count as a "use" that increments its frequency? 
 *    (Yes, updating a value is interacting with the key, so its frequency increases by 1 and it becomes the most recently used for that frequency).
 * 3. Does inserting a new key initialize its frequency to 1 or 0?
 *    (The prompt specifies its UseCounter is set to 1 upon insertion).
 * 4. Can values be negative? 
 *    (Constraints say 0 <= value, so returning -1 for "not found" is perfectly safe).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We must track both Frequency (LFU) and Recency (LRU) simultaneously 
 *   in O(1) time. The binding constraint is that sorting by frequency dynamically takes 
 *   O(log N), so we cannot use a standard Priority Queue if we want strict O(1).
 * 
 * --- Approach 1: HashMap + Brute Force Search ---
 * 1. Intuition: Just keep track of frequencies and timestamps on each node.
 * 2. Try: Use `HashMap<Integer, Node> cache`. `Node` holds `{key, value, freq, lastAccessTime}`. 
 *    `Get/Put` updates `freq` and `lastAccessTime` in O(1).
 * 3. Why it works: Simple to implement. Exact tracking of LFU/LRU.
 * 4. Why it's too costly: When capacity is reached, we must loop over EVERY node in the map 
 *    to find the one with the lowest `freq` (and oldest `lastAccessTime` on ties). 
 * 5. Bottleneck: O(N) eviction time.
 * 
 * Time Complexity: Get/Put: O(1) for existing keys. Put (when full): O(N) — scanning all items to evict.
 * Space Complexity: O(N) — storing N elements.
 * 
 * --- Approach 2: HashMap + Min-Heap (Priority Queue) ---
 * 1. Intuition: A Min-Heap naturally keeps the smallest element at the top. 
 * 2. Try: Map nodes for O(1) access. Store nodes in a `PriorityQueue` sorted first by `freq`, 
 *    then by `lastAccessTime`. 
 * 3. Why it works: Finding the eviction candidate is O(1) (top of the heap). 
 * 4. Why it's suboptimal: Every `Get` or `Put` modifies a node's frequency, meaning its 
 *    position in the heap must change. Updating an element in a heap takes O(log N). 
 * 5. Bottleneck: Heap sift-up/sift-down overhead on every single access.
 * 
 * Time Complexity: Get/Put: O(log N) — because we must rebalance the heap on every access.
 * Space Complexity: O(N) — storing N elements in Map and Heap.
 * 
 * --- Approach 3: Two HashMaps + Doubly Linked Lists (Strict O(1) Optimal) ---
 * 1. Intuition: In LRU, we use one Map + one Doubly Linked List (DLL). For LFU, we need 
 *    to group elements by frequency. What if we use multiple DLLs, one for each frequency?
 * 2. Try: 
 *    - `Map<Integer, Node> cache` (for O(1) lookup).
 *    - `Map<Integer, DoublyLinkedList> freqMap` (maps a frequency to a DLL of nodes with that frequency).
 *    - `int minFreq` variable to instantly know which frequency DLL to evict from.
 * 3. Walkthrough:
 *    - When a node is accessed, it moves from `freqMap.get(f)` to `freqMap.get(f + 1)`. 
 *    - The DLL maintains LRU automatically (append to head, evict from tail).
 *    - If we evict, we go to `freqMap.get(minFreq)`, sever its tail, and remove from `cache`.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It's the only one that genuinely achieves O(1) average time. It is complex 
 *    but demonstrates mastery of composite data structures.
 * 
 * Time Complexity: Get/Put: O(1) — HashMap lookups and DLL pointer manipulations.
 * Space Complexity: O(N) — Storing N nodes and up to N frequency lists.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Capacity == 0: Must instantly return -1 for Get, and do absolutely nothing for Put.
 * - `minFreq` updating: When the last node of a frequency is promoted (e.g., from freq 1 to freq 2), 
 *   the DLL for freq 1 becomes empty. If `minFreq` WAS 1, it must be updated to 2.
 * - Put on an existing key: It updates the value, BUT it also must increase the frequency 
 *   and reset its LRU status (move to the head of the new frequency's DLL).
 * 
 * ============================================================================
 * 4. ## Key Insights, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "A Map of Frequencies to Doubly Linked Lists." 
 *   LRU uses one DLL to maintain time. LFU uses N DLLs to maintain time, grouped by frequency. 
 *   By tracking `minFreq`, we instantly jump to the correct DLL to evict the oldest node.
 * 
 * ASCII DRY RUN
 * Capacity = 2
 * 
 * 1. Put(1, 1)
 *    cache: {1=Node(1)}
 *    freqMap: { 1 -> [Node(1)] }
 *    minFreq: 1
 * 
 * 2. Put(2, 2)
 *    cache: {1=Node(1), 2=Node(2)}
 *    freqMap: { 1 -> [Node(2), Node(1)] }  <-- Node 2 at head (MRU), Node 1 at tail (LRU)
 *    minFreq: 1
 * 
 * 3. Get(1) -> returns 1
 *    Node(1) promoted to freq 2.
 *    cache: {1=Node(1), 2=Node(2)}
 *    freqMap: { 1 -> [Node(2)], 2 -> [Node(1)] }
 *    minFreq: 1
 * 
 * 4. Put(3, 3) -> Eviction needed!
 *    minFreq = 1. Tail of freqMap(1) is Node(2). Evict Node(2)!
 *    cache: {1=Node(1), 3=Node(3)}
 *    freqMap: { 1 -> [Node(3)], 2 -> [Node(1)] }
 *    minFreq: 1
 * 
 * Pitfalls:
 * - Forgetting to remove the evicted key from the `cache` HashMap. The DLL nodes MUST 
 *   store their `key` so that when we pop the tail, we know what key to delete from the map.
 * - Incrementing size when `Put` updates an EXISTING key. Existing keys do not increase total size.
 * - Missing the `capacity == 0` check, leading to `NullPointerException` or incorrect states.
 * 
 * Pattern Recognition: "Multi-level caching / Eviction by dual criteria" -> Map of DLLs.
 * 
 * Interview Script snippet:
 * "To achieve strict O(1) time, we can't use a Min-Heap which costs O(log N). Instead, I'll 
 * combine the LRU concept with frequency bucketing. I'll maintain a HashMap pointing keys to 
 * Nodes for O(1) retrieval, and a second HashMap pointing Frequencies to Doubly Linked Lists. 
 * A global `minFreq` variable will let me instantly find the LFU bucket, and the tail of that 
 * Doubly Linked List gives me the LRU node to evict."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if the cache capacity is huge (e.g., 100M items) and frequencies cluster tightly?
 * A1: The O(1) approach scales perfectly time-wise, but managing millions of small DLL objects 
 *     creates GC pressure. We might optimize by using parallel arrays or a flattened structure 
 *     in languages like C++ or Rust.
 * 
 * Q2: How does LFU behave poorly compared to LRU in real-world systems?
 * A2: LFU suffers from "cache pollution". An item accessed 1000 times on day 1 gets a massive 
 *     frequency score and might never be evicted, even if it's completely useless on day 2. 
 *     Modern caches use W-TinyLFU or age-decaying LFU (where frequencies halve periodically) 
 *     to solve this.
 */

import java.util.HashMap;
import java.util.Map;

public class LFUCacheStudy {

    /**
     * Internal Node for the Doubly Linked List.
     * Must store 'key' so we can remove it from the cache map during eviction.
     */
    private static class Node {
        int key;
        int val;
        int freq;
        Node prev;
        Node next;

        Node(int key, int val) {
            this.key = key;
            this.val = val;
            this.freq = 1; // Always starts at frequency 1 upon creation
        }
    }

    /**
     * Internal Doubly Linked List to maintain LRU order for a specific frequency.
     * Head is Most Recently Used. Tail is Least Recently Used.
     */
    private static class DoublyLinkedList {
        Node head;
        Node tail;
        int size;

        DoublyLinkedList() {
            // Dummy nodes prevent tedious null checks
            head = new Node(-1, -1);
            tail = new Node(-1, -1);
            head.next = tail;
            tail.prev = head;
            size = 0;
        }

        // Add node to the front (MRU position)
        void addFirst(Node node) {
            Node oldFirst = head.next;
            node.next = oldFirst;
            node.prev = head;
            head.next = node;
            oldFirst.prev = node;
            size++;
        }

        // Remove node from anywhere in the list
        void remove(Node node) {
            Node prevNode = node.prev;
            Node nextNode = node.next;
            prevNode.next = nextNode;
            nextNode.prev = prevNode;
            size--;
        }

        // Pop and return the LRU node (right before dummy tail)
        Node removeLast() {
            if (size > 0) {
                Node lruNode = tail.prev;
                remove(lruNode);
                return lruNode;
            }
            return null;
        }
    }

    // --- LFUCache Fields ---
    private final int capacity;
    private int size;
    private int minFreq;

    // Maps Key -> Node
    private final Map<Integer, Node> cache;
    // Maps Frequency -> Doubly Linked List of Nodes
    private final Map<Integer, DoublyLinkedList> freqMap;

    /**
     * Initializes the object with the capacity of the data structure.
     */
    public LFUCacheStudy(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.minFreq = 0;
        this.cache = new HashMap<>();
        this.freqMap = new HashMap<>();
    }

    /**
     * Gets the value of the key if it exists, otherwise -1.
     * Time: O(1)
     */
    public int get(int key) {
        if (!cache.containsKey(key)) {
            return -1;
        }
        
        Node node = cache.get(key);
        updateNodeFrequency(node); // Promotes the node to the next frequency bracket
        
        return node.val;
    }

    /**
     * Updates the value if present, or inserts the key. Evicts LFU/LRU if full.
     * Time: O(1)
     */
    public void put(int key, int value) {
        // Edge case: cache size is strictly 0
        if (capacity == 0) {
            return;
        }

        // Case 1: Key already exists. 
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.val = value; // Update value
            updateNodeFrequency(node); // Promote frequency
            return;
        }

        // Case 2: New Key. We must insert. Check capacity first.
        if (size == capacity) {
            // Cache is full. Evict the LRU node from the DLL of the minimum frequency.
            DoublyLinkedList minFreqList = freqMap.get(minFreq);
            Node evictedNode = minFreqList.removeLast();
            
            // Remove the evicted node from the main cache map
            cache.remove(evictedNode.key);
            size--;
        }

        // Insert the new key
        Node newNode = new Node(key, value);
        cache.put(key, newNode);
        
        // A new node always starts at frequency 1
        minFreq = 1;
        
        // Ensure the DLL for frequency 1 exists, then add the new node to its head
        freqMap.computeIfAbsent(1, k -> new DoublyLinkedList()).addFirst(newNode);
        size++;
    }

    /**
     * Helper Method: Moves a node from its current frequency list to the f+1 list.
     */
    private void updateNodeFrequency(Node node) {
        int oldFreq = node.freq;
        DoublyLinkedList oldList = freqMap.get(oldFreq);
        
        // 1. Remove node from its current frequency bucket
        oldList.remove(node);
        
        // 2. Crucial Step: If the bucket it just left was the minimum frequency bucket, 
        // and it is now empty, we must increment minFreq!
        if (oldFreq == minFreq && oldList.size == 0) {
            minFreq++;
        }
        
        // 3. Increment the node's frequency score
        node.freq++;
        
        // 4. Add it to the front of its new frequency bucket
        freqMap.computeIfAbsent(node.freq, k -> new DoublyLinkedList()).addFirst(node);
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        System.out.println("--- Testing LFU Cache (Capacity 2) ---");
        LFUCacheStudy lfu = new LFUCacheStudy(2);

        // Put 1, freq=1
        lfu.put(1, 1);
        
        // Put 2, freq=1
        lfu.put(2, 2);
        
        // Get 1 -> returns 1. freq of 1 becomes 2.
        System.out.println("Get 1 (Expect 1): " + lfu.get(1)); 
        
        // Put 3 -> capacity is 2, must evict. 
        // minFreq is 1. '2' has freq 1, '1' has freq 2. Evict 2.
        lfu.put(3, 3);
        
        // Get 2 -> returns -1 (evicted)
        System.out.println("Get 2 (Expect -1): " + lfu.get(2));
        
        // Get 3 -> returns 3. freq of 3 becomes 2.
        System.out.println("Get 3 (Expect 3): " + lfu.get(3));
        
        // Put 4 -> capacity full. minFreq is 2. 
        // Both 1 and 3 have freq 2. 
        // '1' was accessed earlier than '3', so '1' is the LRU among freq 2. Evict 1.
        lfu.put(4, 4);
        
        // Get 1 -> returns -1 (evicted)
        System.out.println("Get 1 (Expect -1): " + lfu.get(1));
        
        // Get 3 -> returns 3.
        System.out.println("Get 3 (Expect 3): " + lfu.get(3));
        
        // Get 4 -> returns 4.
        System.out.println("Get 4 (Expect 4): " + lfu.get(4));
        
        System.out.println("\n--- Testing Edge Case: Capacity 0 ---");
        LFUCacheStudy emptyCache = new LFUCacheStudy(0);
        emptyCache.put(1, 1); // Should do nothing gracefully
        System.out.println("Get 1 (Expect -1): " + emptyCache.get(1));
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: HashMap (Cache) + HashMap of Doubly Linked Lists (Frequencies).
 * Key Observation: Grouping elements into separate Doubly Linked Lists by their frequency 
 * allows strict O(1) addition, promotion, and LRU eviction within a frequency tie.
 * What to Memorize: The `minFreq` variable tracks the LFU bucket. It increments ONLY when 
 * `oldFreq == minFreq` AND the DLL for `oldFreq` becomes empty during a promotion.
 * Most Common Trap: Forgetting to store the original `key` inside the DLL Node, making it 
 * impossible to remove the evicted tail from the main lookup map.
 * Mental Trigger: "O(1) tracking by two variables (time + count)" -> "Map of DLLs".
 * ============================================================================
 */





import java.util.HashMap;
import java.util.Map;

public class LFUCache {

    /*
     * NODE:
     *
     * Each cached key has exactly one node. The node belongs to exactly
     * one frequency bucket, so its frequency and list membership must
     * always remain synchronized.
     *
     * prev/next permit O(1) unlinking when a key changes frequency
     * or is evicted. A singly linked list would not support arbitrary
     * O(1) removal without searching for the predecessor.
     */
    private static final class Node {
        final int key;
        int value;
        int frequency;
        Node prev;
        Node next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
            this.frequency = 1;
        }
    }

    /*
     * DOUBLY LINKED LIST:
     *
     * Each list contains only nodes having the same frequency.
     *
     * head.next is the MRU (most recently used) node.
     * tail.prev is the LRU (least recently used) node.
     *
     * Dummy head and tail sentinels eliminate special cases for
     * inserting/removing the first or last real node.
     *
     * addFirst: O(1)
     * remove:   O(1), provided the node belongs to this list
     * removeLast: O(1)
     */
    private static final class DoublyLinkedList {
        private final Node head = new Node(-1, -1);
        private final Node tail = new Node(-1, -1);
        private int size;

        DoublyLinkedList() {
            head.next = tail;
            tail.prev = head;
        }

        boolean isEmpty() {
            return size == 0;
        }

        void addFirst(Node node) {
            Node first = head.next;

            node.prev = head;
            node.next = first;
            head.next = node;
            first.prev = node;

            size++;
        }

        void remove(Node node) {
            // The node's neighbors are known; no search is needed.
            node.prev.next = node.next;
            node.next.prev = node.prev;

            // Detach the node so stale links cannot accidentally be reused.
            node.prev = null;
            node.next = null;

            size--;
        }

        Node removeLast() {
            if (isEmpty()) {
                return null;
            }

            Node lru = tail.prev;
            remove(lru);
            return lru;
        }
    }

    /*
     * keyToNode:
     *   Key -> exact cache node, for average O(1) lookup.
     *
     * frequencyToList:
     *   Frequency -> recency-ordered list for that frequency.
     *
     * minFrequency:
     *   The smallest frequency among all currently cached nodes.
     *
     * The crucial invariant is:
     * minFrequency always identifies a non-empty frequency bucket
     * whenever the cache is non-empty.
     */
    private final int capacity;
    private final Map<Integer, Node> keyToNode = new HashMap<>();
    private final Map<Integer, DoublyLinkedList> frequencyToList =
            new HashMap<>();

    private int minFrequency;

    public LFUCache(int capacity) {
        // The problem permits capacity zero.
        this.capacity = capacity;
    }

    public int get(int key) {
        Node node = keyToNode.get(key);

        if (node == null) {
            // Missing reads neither change frequency nor recency.
            return -1;
        }

        // A successful get counts as usage.
        increaseFrequency(node);

        return node.value;
    }

    public void put(int key, int value) {
        if (capacity == 0) {
            // Never attempt eviction from an empty zero-capacity cache.
            return;
        }

        Node existing = keyToNode.get(key);

        if (existing != null) {
            // Updating an existing key is a usage event too.
            existing.value = value;
            increaseFrequency(existing);
            return;
        }

        if (keyToNode.size() == capacity) {
            /*
             * EVICTION:
             *
             * minFrequency directly identifies the correct frequency.
             * Within that bucket, removeLast() chooses the LRU node.
             * Both operations take O(1) time.
             */
            DoublyLinkedList lruList =
                    frequencyToList.get(minFrequency);

            Node victim = lruList.removeLast();
            keyToNode.remove(victim.key);

            // Remove empty buckets to keep the map representative
            // of frequencies that actually exist.
            if (lruList.isEmpty()) {
                frequencyToList.remove(minFrequency);
            }
        }

        /*
         * INSERTION:
         *
         * Every newly inserted key starts at frequency 1.
         * It is the newest node in frequency bucket 1.
         *
         * Reset minFrequency to 1 because the new node guarantees
         * that frequency 1 is now the minimum existing frequency.
         */
        Node node = new Node(key, value);

        keyToNode.put(key, node);
        frequencyToList.computeIfAbsent(
                1, ignored -> new DoublyLinkedList()
        ).addFirst(node);

        minFrequency = 1;
    }

    private void increaseFrequency(Node node) {
        int oldFrequency = node.frequency;

        // First unlink the node from its current frequency bucket.
        DoublyLinkedList oldList = frequencyToList.get(oldFrequency);
        oldList.remove(node);

        /*
         * MINIMUM-FREQUENCY MAINTENANCE:
         *
         * If this node was the last node at minFrequency, that bucket
         * becomes empty after removal.
         *
         * Since this operation only moves a node from f to f+1,
         * no frequencies below f can suddenly become populated.
         * Therefore, when the old minimum bucket empties, the new
         * minimum is exactly oldFrequency + 1.
         *
         * This reasoning depends on oldFrequency being the current
         * minimum. If a non-minimum bucket empties, minFrequency
         * remains unchanged.
         */
        if (oldList.isEmpty()) {
            frequencyToList.remove(oldFrequency);

            if (minFrequency == oldFrequency) {
                minFrequency = oldFrequency + 1;
            }
        }

        // The usage counter increases exactly once.
        node.frequency++;

        /*
         * Add to the front of the new bucket because this access is
         * the most recent usage among all nodes at the new frequency.
         */
        frequencyToList.computeIfAbsent(
                node.frequency, ignored -> new DoublyLinkedList()
        ).addFirst(node);
    }

    /*
     * COMPLEXITY ANALYSIS:
     *
     * Let n = number of cached entries, with n <= capacity.
     *
     * get(key):
     *   - Hash-map lookup: average O(1).
     *   - Unlink from old list: O(1).
     *   - Update frequency and possibly minFrequency: O(1).
     *   - Insert into new list: O(1).
     *   Total: average O(1).
     *
     * put(key, value):
     *   - Existing key: update and increase frequency in average O(1).
     *   - New key without eviction: average O(1).
     *   - New key with eviction: locate minFrequency bucket and remove
     *     its LRU node in O(1), then insert in average O(1).
     *   Total: average O(1).
     *
     * SPACE:
     *   - keyToNode holds O(n) nodes/references.
     *   - frequencyToList holds at most O(n) non-empty buckets.
     *   - All frequency lists together hold O(n) real nodes and
     *     O(n) sentinel nodes.
     *   Total auxiliary space: O(n).
     *
     * This is average O(1), rather than worst-case O(1), because
     * HashMap operations have average constant-time complexity.
     */
}



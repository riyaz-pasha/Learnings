/**
 * ============================================================================
 * L E E T C O D E   7 0 6 :   D E S I G N   H A S H M A P
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Are the keys strictly integers, or do I need to design for generic objects like Strings?" 
 *    (Prevents over-engineering; constraints say keys are 0 to 10^6, so simple modulo hashing works).
 * 2. "Is memory severely constrained, or can I allocate a large array upfront?"
 *    (If memory is loose and max key is 10^6, a simple 1-million-size array works. If tight, we must use hashing/chaining).
 * 3. "Do we need to implement dynamic resizing (load factor checking), or can I assume a fixed bucket array size?"
 *    (At most 10^4 operations means a fixed bucket array size of ~2000 will have tiny chains, avoiding complex resizing logic).
 * 4. "Is thread safety a concern?"
 *    (If yes, we'd need segment locking like ConcurrentHashMap. Assuming no for standard algorithms interviews).
 * 5. "What should `get` return if a value isn't found? And can `value` legitimately be negative?"
 *    (Constraints say values >= 0, so returning -1 for 'not found' is safe and unambiguous).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need O(1) average time for insertions, lookups, and deletions. 
 * > A standard list lookup is O(N). A sorted array lookup is O(log N). To get O(1), we *must* 
 * > use an array's index-based random access. The challenge is mapping large or sparse keys into 
 * > a smaller, dense array without losing data when two keys want the same index (collisions).
 * 
 * ### Approach 1: The Giant Array (Direct Addressing)
 * 1. What I'd naturally try: The max key is 10^6. Why not just make an array of size 1,000,001?
 * 2. Why it works: Every key maps exactly to its own index. `arr[5] = value`. No collisions ever.
 * 3. Why it's too costly: 
 *    - Time Complexity: O(1) — because we instantly jump to the exact memory address `arr[key]`.
 *    - Space Complexity: O(K) where K is the max key range (10^6) — because we allocate 1,000,001 
 *      integers (4MB) even if the user only ever calls `put()` 3 times. It's a massive waste of memory 
 *      for sparse datasets, which defeats the real-world purpose of a HashMap.
 * 4. What property/observation removes the bottleneck: We only have at most 10^4 operations. We only 
 *    need space proportional to the *number of entries*, not the *maximum possible key space*.
 * 
 * ### Approach 2: Array of Linked Lists (Chaining - Fixed Size)
 * 1. What I'd naturally try: Map the large key space into a smaller array (e.g., size 2069) using modulo arithmetic 
 *    (`index = key % 2069`).
 * 2. Why it works: We save massive space. But `key 5` and `key 2074` both map to index 5. To prevent overwriting, 
 *    we make `arr[5]` point to a Linked List: `[5, val1] -> [2074, val2]`.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(1) average, O(N) worst-case — because keys are uniformly distributed across 2069 
 *      buckets, keeping lists extremely short. In the worst case (all keys hash to the same bucket), we traverse N elements.
 *    - Space Complexity: O(M + B) — where M is the number of unique inserted keys, and B is the number of 
 *      buckets (2069). We only allocate memory for elements actually inserted, plus a small base array overhead.
 * 
 * ### Approach 3: Dynamic Resizing (The True HashMap)
 * 1. What's the flaw in Approach 2?: If we insert 1 million elements into 2069 buckets, every list is ~500 nodes long. O(1) degrades to O(500).
 * 2. The solution: Track a `loadFactor = size / buckets.length`. When it exceeds 0.75, double the bucket array 
 *    and re-hash everything.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(1) amortized — because the expensive O(N) rehashing step happens exponentially rarely.
 *    - Space Complexity: O(M) — perfectly scales with the data.
 * 
 * > **Interview Strategy:** In a 45-minute interview, I would explicitly *describe* Approach 3, but write 
 * > Approach 2 (Fixed Size Chaining) unless the interviewer demands resizing. Implementing dynamic array 
 * > doubling + rehashing + linked list unlinking under time pressure introduces huge risk for off-by-one errors.
 * 
 * 
 * ## 3. Edge cases
 * - `key == 0`: Modulo of 0 is 0. Works fine, but good to mentally check.
 * - `put` on an *existing* key: We must update the value, NOT append a duplicate key to the list!
 * - `remove` the *head* of a linked list: Standard linked list gotcha. (Hint: Use a dummy head node to make 
 *   deletions effortless without needing special `if (head == target)` logic).
 * - Hash Collisions: Keys 5 and 2074 mapping to the same bucket.
 * 
 * 
 * ## 4. Key Insight & Diagram
 * **Key Insight:** "Hashing is just bucketing. Collision resolution is just deciding how to share a bucket."
 * If you use dummy head nodes in your bucket arrays, you never have to worry about head-pointer manipulation 
 * when removing elements.
 * 
 * **ASCII Diagram (Chaining):**
 * Modulo 5 array:
 * Index 0: [Dummy] -> null
 * Index 1: [Dummy] -> [Key:11, Val:A] -> null
 * Index 2: [Dummy] -> [Key:7, Val:B] -> [Key:22, Val:C] -> null   <-- Collision handled beautifully
 * Index 3: [Dummy] -> null
 * Index 4: [Dummy] -> [Key:9, Val:D] -> null
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if we couldn't use LinkedLists (Chaining)? 
 *   **A:** We could use Open Addressing (Linear Probing). If bucket 5 is full, try 6, then 7. 
 *   Pros: Better CPU cache locality (arrays vs objects scattered in heap). 
 *   Cons: Susceptible to clustering; deletions require ugly "tombstone" markers so we don't break probing chains.
 * - **Q:** What happens in Java's real HashMap if the bucket chain gets too long?
 *   **A:** Since Java 8, if a bucket gets more than 8 elements, the LinkedList transforms into a Red-Black Tree. 
 *   This drops the worst-case lookup from O(N) down to O(log N) for catastrophic hash collisions.
 * - **Q:** How would you handle generic Object keys instead of int?
 *   **A:** Rely on `key.hashCode()`. Crucially, we must handle negative hash codes (e.g., `Math.abs(key.hashCode() % SIZE)`), 
 *   and we must use `.equals()` instead of `==` when traversing the chain to verify a key match.
 * 
 * ============================================================================
 */

import java.util.Arrays;

public class DesignHashMapStudy {

    /**
     * Common interface just so we can test both in our main method.
     */
    interface MyHashMap {
        void put(int key, int value);
        int get(int key);
        void remove(int key);
    }

    // ========================================================================
    // APPROACH 2: CHAINING HASHMAP (The optimal interview solution)
    // ========================================================================
    static class ChainingHashMap implements MyHashMap {
        
        /**
         * A standard LinkedList Node.
         * Concrete example: node representing {key: 15, value: 100} 
         * would have key=15, val=100, and next pointing to the next collision.
         */
        private static class Node {
            int key, val;
            Node next;
            
            Node(int key, int val) {
                this.key = key;
                this.val = val;
            }
        }
        
        // 2069 is a prime number. Using primes for array sizes reduces collisions 
        // when hash codes share common factors with the array size.
        private final int SIZE = 2069;
        
        /**
         * Array of LinkedLists.
         * Concrete example: buckets[5] -> [Dummy Node] -> [Key: 5, Val: A] -> [Key: 2074, Val: B]
         */
        private final Node[] buckets;

        public ChainingHashMap() {
            buckets = new Node[SIZE];
            // Initialize every bucket with a Dummy Head node.
            // WHY? Because when we remove an element, having a dummy head guarantees 
            // the element we want to remove always has a `prev` node. It eliminates 
            // the ugly edge case of "what if the node to delete is the head of the list?"
            for (int i = 0; i < SIZE; i++) {
                buckets[i] = new Node(-1, -1);
            }
        }
        
        private int hash(int key) {
            return key % SIZE;
        }

        @Override
        public void put(int key, int value) {
            int idx = hash(key);
            Node prev = find(buckets[idx], key);
            
            // What does `find()` actually return? 
            // It returns the node *right before* the target key, OR the last node 
            // in the list if the target key doesn't exist.
            
            if (prev.next == null) {
                // The key wasn't in the list. `prev` is the tail. Append new node.
                prev.next = new Node(key, value);
            } else {
                // The key exists. `prev.next` is the node holding our key. Update its value.
                prev.next.val = value;
            }
        }

        @Override
        public int get(int key) {
            int idx = hash(key);
            Node prev = find(buckets[idx], key);
            
            // If prev.next is null, the key wasn't found.
            return prev.next == null ? -1 : prev.next.val;
        }

        @Override
        public void remove(int key) {
            int idx = hash(key);
            Node prev = find(buckets[idx], key);
            
            // If prev.next is not null, the key exists, so we bypass it to remove it.
            if (prev.next != null) {
                prev.next = prev.next.next;
            }
        }
        
        /**
         * Helper method: Traverses the linked list to find the node containing `key`.
         * Returns the PREVIOUS node. 
         * 
         * DRY RUN of `find(bucket_head, 2074)` on a list: [Dummy] -> [Key:5] -> [Key:2074]
         * 1. node = [Dummy]. next is [Key:5]. key != 2074. Move forward.
         * 2. node = [Key:5]. next is [Key:2074]. key == 2074. Loop breaks!
         * 3. Returns [Key:5]. (We use this returned node to either update next.val, or skip next).
         */
        private Node find(Node bucket, int key) {
            Node curr = bucket;
            Node prev = null;
            // Traverse as long as we have a next node, and the next node ISN'T our target.
            while (curr != null && curr.key != key) {
                prev = curr;
                curr = curr.next;
            }
            // `curr` is now either the node holding our key, or null.
            // But we actually want to return `prev` so the caller can manipulate pointers!
            // Wait, looking at the loop above: if curr.key == key, we break. 
            // Then `prev` is the node *before* `curr`.
            
            // Actually, let's write it cleaner. Since we have a dummy head, 
            // we can just look one step ahead (`curr.next.key`) to find the target.
            Node node = bucket;
            while (node.next != null && node.next.key != key) {
                node = node.next;
            }
            return node;
        }
    }


    // ========================================================================
    // APPROACH 1: GIANT ARRAY (Brute Force Space - Good for knowing why it works)
    // ========================================================================
    static class GiantArrayHashMap implements MyHashMap {
        // 1,000,001 indices. 
        // Concrete example: map[5] = 100 means {key: 5, value: 100}
        private final int[] map;

        public GiantArrayHashMap() {
            map = new int[1000001];
            Arrays.fill(map, -1);
        }

        @Override
        public void put(int key, int value) {
            map[key] = value; // Direct addressing. O(1) time. O(K) space.
        }

        @Override
        public int get(int key) {
            return map[key];
        }

        @Override
        public void remove(int key) {
            map[key] = -1;
        }
    }


    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        System.out.println("Testing Chaining HashMap (Optimal):");
        MyHashMap map = new ChainingHashMap();
        runTests(map);
        
        System.out.println("\nTesting Giant Array HashMap (Brute Force Space):");
        MyHashMap map2 = new GiantArrayHashMap();
        runTests(map2);
    }
    
    private static void runTests(MyHashMap map) {
        // Test basic put and get
        map.put(1, 1);
        map.put(2, 2);
        System.out.println("Get 1 (Expect 1): " + map.get(1));
        System.out.println("Get 3 (Expect -1): " + map.get(3));
        
        // Test updating existing key
        map.put(2, 10);
        System.out.println("Get 2 after update (Expect 10): " + map.get(2));
        
        // Test collision (Assuming ChainingHashMap with SIZE=2069)
        // 5 and 2074 (5 + 2069) will map to the exact same bucket index!
        map.put(5, 100);
        map.put(2074, 200);
        System.out.println("Get 5 (Expect 100): " + map.get(5));
        System.out.println("Get 2074 (Expect 200): " + map.get(2074));
        
        // Test removal
        map.remove(2);
        System.out.println("Get 2 after removal (Expect -1): " + map.get(2));
        
        // Test removing non-existent
        map.remove(999);
        System.out.println("Remove 999 didn't crash.");
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Array + LinkedLists (Chaining). 
 * - Key observation: The array size doesn't need to match the key space, it only 
 *   needs to handle the number of expected operations (or resize dynamically).
 * - What's worth memorizing: The trick of using a **Dummy Head Node** in every bucket. 
 *   It makes pointer manipulation during insertions and deletions trivially simple 
 *   by removing the `head == null` or `delete head` edge cases.
 * - Most common trap: When `put`ting a key that already exists, you MUST update its 
 *   value, not blindly append a new node to the bucket's list.
 * - Mental trigger: "Hash collisions = Shared bucket. Linked list keeps them organized."
 * ============================================================================
 */



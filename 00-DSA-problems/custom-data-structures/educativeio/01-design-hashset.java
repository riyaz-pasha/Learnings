/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Design a MyHashSet class without using any built-in hash table libraries.
 * Implement: add(key), contains(key), remove(key).
 * Constraints: 0 <= key <= 10^6, at most 10^4 calls.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Are the keys uniformly distributed, or could there be malicious clustering?
 *    (Dictates if we need an anti-collision safeguard like a BST instead of a linked list in our buckets).
 * 2. What is the expected ratio of reads (`contains`) to writes (`add`/`remove`)?
 *    (If it's extremely read-heavy with few writes, we might optimize bucket size or memory layout for cache locality).
 * 3. Is the maximum key strictly bounded to 1,000,000 as stated, or could it scale to 10^9 in the future?
 *    (A 10^6 boolean array is 1MB, which is fine. A 10^9 array is 1GB, which forces us to use true hashing).
 * 4. Are we operating in a multi-threaded environment?
 *    (Prevents ConcurrentModificationException; tells us if we need bucket-level locking or synchronized methods).
 * 5. Can we dynamically resize the bucket array, or should we use a fixed size?
 *    (Since max operations are 10^4, fixed size is optimal here, but dynamic resizing is expected in production sets).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement: Map an unpredictable, potentially sparse set of keys into a 
 *   predictable, constrained memory space, while keeping lookups near instantaneous.
 *   The binding constraint is space-time tradeoff: we can't allocate infinite memory, 
 *   nor can we afford linear O(N) scanning for every operation.
 * 
 * --- Approach 1: The Direct Addressing (Brute Force / Naive Array) ---
 * 1. Intuition: The key range is fixed (0 to 10^6). What if the key itself is the index?
 * 2. Try: Create a `boolean[] present = new boolean[1000001]`. 
 *    `add(k)` -> `present[k] = true`. 
 *    `contains(k)` -> `return present[k]`.
 * 3. Why it works: Array lookups are hardware-level O(1).
 * 4. Why it's too costly: It completely ignores the "sparse" nature of the operations. 
 *    The constraints say at most 10^4 calls. We are allocating space for 10^6 elements 
 *    even if the user only inserts one element.
 * 5. Bottleneck: Wasted memory. The space scales with the MAX_KEY, not the number of entries.
 * 6. Observation: We need a structure whose size scales with the *number of elements* (N), 
 *    not the *domain of possible keys* (U).
 * 
 * Time Complexity: O(1) — because direct array access via index requires no traversal.
 * Space Complexity: O(U) — because we allocate an array of size U (where U is the maximum possible key value, 10^6), regardless of how many elements are actually inserted.
 * 
 * --- Approach 2: The Unordered List (Append-Only with Linear Scan) ---
 * 1. Intuition: If we want space to scale with N, let's just store the items we actually see.
 * 2. Try: Use a single `ArrayList<Integer>`. 
 *    `add(k)` -> Scan to ensure no duplicate, then append.
 *    `contains(k)` -> Scan the whole list.
 * 3. Why it works: We only store what we need. Space is strictly proportional to usage.
 * 4. Why it's too costly: Time! Every operation now requires scanning the entire list.
 * 5. Bottleneck: Repetitive linear scanning. To find if '5' exists, we might have to check 9,999 other numbers first.
 * 6. Observation: We need a middle ground. We can't use 1 huge array, and 1 single list is too slow. 
 *    What if we used *many smaller lists* and a mathematical rule to know exactly which list a number belongs to?
 * 
 * Time Complexity: O(N) — because every add, remove, and contains requires checking every single inserted element to verify existence.
 * Space Complexity: O(N) — because we only allocate nodes/memory for the exact N items currently in the set.
 * 
 * --- Approach 3: Array of Buckets (Chaining) [OPTIMAL] ---
 * 1. Intuition: Divide the elements across a fixed number of buckets (e.g., 1000). 
 * 2. Try: Create an array of 1000 Linked Lists. 
 *    Hash function: `bucketIndex = key % 1000`.
 *    To `add(k)`: Go to `array[k % 1000]` and append to that specific linked list (if not present).
 * 3. Why it works: We combine the O(1) array access with O(N) space scaling. 
 *    By distributing 10,000 max elements into 1000 buckets, each list is incredibly short (average length 10).
 * 4. Cost checks: Searching a list of 10 elements is practically instantaneous.
 * 5. Which to write in a 45-min interview?
 *    Approach 3. Approach 1 is a cop-out for this specific problem (unless the interviewer explicitly allows it), 
 *    while Approach 3 demonstrates actual systems-level data structure knowledge (hash functions, collision resolution via chaining, dummy heads).
 * 
 * Time Complexity: Expected O(1), Worst O(N) — Expected because elements distribute evenly, leaving tiny lists (e.g. length 10). Worst case occurs if *all* inserted keys map to the exact same bucket (e.g. 1000, 2000, 3000), forcing a linear scan of that single list.
 * Space Complexity: O(B + N) — because we store an array of B bucket pointers, plus N individual linked list nodes for the inserted elements.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - key = 0: Modulo `0 % 1000` is 0. Must ensure Bucket 0 handles it correctly.
 * - key = 10^6: Modulo `1000000 % 1000` is 0. Collision with 0.
 * - Duplicate adds: `add(5)`, `add(5)`. Must scan the bucket before blindly appending to avoid memory leaks/logic bugs.
 * - Removing non-existent key: `remove(99)` when set is empty. Should fail gracefully (no NullPointerException).
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & PITFALLS
 * ============================================================================
 * > Key Insight: "Hashing is just deterministic routing." 
 *   You aren't magically creating O(1) search. You are routing an item to a very specific, 
 *   very small pile, so that the resulting linear search is imperceptibly short.
 * 
 * Pitfalls:
 * - Omitting the dummy head in the LinkedList: If you don't use a dummy node at the start 
 *   of each bucket's list, removing the *very first* element requires messy edge-case code 
 *   (`if (head.key == key) buckets[idx] = head.next;`).
 * - Using a bad modulo: Using an even number or a power of 2 for modulo can cause clustering 
 *   if inputs have patterns. Prime numbers (like 769 or 1009) distribute bits better.
 * - `==` on Integer wrappers: If you used `LinkedList<Integer>`, `list.remove((Integer) key)` 
 *   works, but `list.remove(key)` removes by INDEX. Implementing your own Node prevents this trap.
 * 
 * ASCII DRY RUN (Optimal Approach):
 * Let Bucket Count B = 5.
 * 1. add(3)  -> 3 % 5 = 3. Bucket 3: [Dummy] -> [3]
 * 2. add(8)  -> 8 % 5 = 3. Bucket 3: [Dummy] -> [3] -> [8]
 * 3. add(12) -> 12 % 5 = 2. Bucket 2: [Dummy] -> [12]
 * 4. remove(3) -> Go to Bucket 3. 
 *    Scan: prev=Dummy, curr=3. Match! 
 *    prev.next = curr.next. 
 *    Bucket 3: [Dummy] -> [8]. (3 is bypassed and garbage collected).
 * 
 * Pattern Recognition: "When you see 'custom lookup collection', think Hash + Collision strategy."
 * Transfers to: Design HashMap (LeetCode 706), LFU/LRU Cache components, Distributed Consistent Hashing.
 * 
 * Interview Script snippet:
 * "To avoid O(N) space waste, I won't use a flat boolean array. Instead, I'll build an array of 
 * LinkedLists. I'll take the key, modulo it by a prime like 1009 to find the bucket index, and then 
 * traverse that specific list. Since maximum operations are 10,000, our load factor will be at most 10, 
 * meaning the linked lists remain tiny. That gives us expected O(1) time and strictly O(K) space where 
 * K is the number of inserted elements."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if the number of `add` calls was 100,000,000 instead of 10,000?
 * A1: Our fixed bucket size of 1000 would result in lists of length 100,000, degrading to O(N) time. 
 *     We'd need a **dynamic resizing** array (like Java's HashMap). We track the 'load factor' (N / Buckets). 
 *     When it hits 0.75, we create a new array double the size and rehash every element.
 * 
 * Q2: What if a malicious user intentionally adds keys that all hash to the same bucket?
 * A2: This is a Hash DoS attack. To fix it, we upgrade the buckets from LinkedLists to 
 *     Balanced Binary Search Trees (like Red-Black Trees). Then worst-case time drops from O(N) to O(log N).
 *     Java 8's HashMap does exactly this when a bucket exceeds 8 elements.
 */

public class MyHashSetStudy {

    /**
     * Our custom LinkedList Node.
     * We don't use Java's built-in LinkedList to demonstrate foundational understanding.
     */
    private static class Node {
        int key;
        Node next;

        Node(int key) {
            this.key = key;
        }
    }

    // Number of buckets. We choose an arbitrary prime near 1000.
    // Why prime? Prime modulos reduce collisions if input keys follow a stride/pattern 
    // (e.g., if inputs are all multiples of 10, a bucket size of 1000 would only use 10% of buckets. 
    // A prime forces distribution).
    private final int BUCKETS = 1009; 
    
    /* 
     * Concrete example of `buckets`: 
     * buckets[0] -> DummyNode -> [Node: 1009] -> null
     * buckets[1] -> DummyNode -> null
     * buckets[2] -> DummyNode -> [Node: 2] -> [Node: 1011] -> null
     */
    private Node[] buckets;

    public MyHashSetStudy() {
        buckets = new Node[BUCKETS];
        
        // Initialize every bucket with a "Dummy Head".
        // WHY? If we don't have a dummy head, deleting the *first* actual element 
        // in the list requires a special if-statement: `if (head.key == target) ...`.
        // With a dummy head, every valid node always has a `prev` node before it,
        // so deletion is uniformly `prev.next = prev.next.next;`.
        for (int i = 0; i < BUCKETS; i++) {
            buckets[i] = new Node(-1);
        }
    }

    /**
     * Hash function maps the domain of keys [0, 10^6] into array bounds [0, 1008].
     */
    private int hash(int key) {
        return key % BUCKETS;
    }

    /**
     * Inserts the value key into the HashSet.
     */
    public void add(int key) {
        int index = hash(key);
        Node curr = buckets[index]; // starts at the dummy head
        
        // Scan to prevent duplicate insertions
        while (curr.next != null) {
            if (curr.next.key == key) {
                return; // Key already exists, do nothing
            }
            curr = curr.next;
        }
        
        // If we reach here, key doesn't exist. `curr` is the last node.
        // Append it to the tail. (Appending to the head is also valid and O(1), 
        // but we already traversed to check for duplicates, so we're at the tail anyway).
        curr.next = new Node(key);
    }

    /**
     * Removes the value key if it exists in the HashSet.
     */
    public void remove(int key) {
        int index = hash(key);
        Node prev = buckets[index]; // starts at the dummy head
        
        // We look ONE NODE AHEAD (prev.next). 
        // This is necessary because to delete a node in a singly linked list,
        // you must have a reference to the node *before* the one you want to delete.
        while (prev.next != null) {
            if (prev.next.key == key) {
                // Bypass the target node. 
                // e.g., Dummy -> 3 -> 8. To delete 3: Dummy.next = 3.next (which is 8).
                prev.next = prev.next.next;
                return; // Element found and removed, we can exit
            }
            prev = prev.next;
        }
    }

    /**
     * Returns TRUE if the key exists in the HashSet, FALSE otherwise.
     */
    public boolean contains(int key) {
        int index = hash(key);
        Node curr = buckets[index].next; // Skip the dummy head for scanning
        
        while (curr != null) {
            if (curr.key == key) {
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    // ============================================================================
    // MAIN METHOD: Testing Approach + Edge Cases
    // ============================================================================
    public static void main(String[] args) {
        MyHashSetStudy hashSet = new MyHashSetStudy();

        // 1. Basic happy path
        hashSet.add(1);
        hashSet.add(2);
        System.out.println("Contains 1? " + hashSet.contains(1)); // true
        System.out.println("Contains 3? " + hashSet.contains(3)); // false
        
        // 2. Add duplicate
        hashSet.add(2);
        System.out.println("Contains 2? " + hashSet.contains(2)); // true
        hashSet.remove(2);
        System.out.println("Contains 2 after removal? " + hashSet.contains(2)); // false
        
        // 3. Remove non-existent element (Should not crash)
        hashSet.remove(999);
        
        // 4. Collision Edge Case
        // 1009 and 2018 both modulo to 0 (using BUCKETS = 1009)
        int collisionKey1 = 1009;
        int collisionKey2 = 2018;
        
        hashSet.add(collisionKey1);
        hashSet.add(collisionKey2);
        
        // Both should exist
        System.out.println("Contains 1009 (collided)? " + hashSet.contains(collisionKey1)); // true
        System.out.println("Contains 2018 (collided)? " + hashSet.contains(collisionKey2)); // true
        
        // Remove one colliding key, ensure the other still exists
        hashSet.remove(collisionKey1);
        System.out.println("Contains 1009 after remove? " + hashSet.contains(collisionKey1)); // false
        System.out.println("Contains 2018 after sibling remove? " + hashSet.contains(collisionKey2)); // true

        // 5. Zero and Upper Boundary Edge Cases
        hashSet.add(0);
        hashSet.add(1_000_000);
        System.out.println("Contains 0? " + hashSet.contains(0)); // true
        System.out.println("Contains 1,000,000? " + hashSet.contains(1_000_000)); // true
    }
}

/**
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core Pattern: Chaining (Array of Linked Lists).
 * Key Observation: Space should scale with the number of inputs (N), not the domain of inputs (U). 
 * What to Memorize: The Dummy Head trick for linked lists. It effortlessly solves the "deleting the head" edge case.
 * Most Common Trap: Forgetting to check for duplicates before appending during `add()`. 
 * Mental Trigger: "Custom Map/Set" -> "Modulo Buckets + Linked Lists + Dummy Heads".
 * ============================================================================
 */









import java.util.ArrayList;
import java.util.List;

public class MyHashSet {

    /*
     * keyRange is the number of buckets in our hash table.
     *
     * We use 769, a prime number, to distribute keys among buckets.
     * A good bucket count helps reduce collisions for many key patterns.
     *
     * This is NOT the maximum allowed key. Keys can still be as large
     * as 1_000_000. The hash() method maps them into [0, 768].
     */
    private final int keyRange = 769;

    /*
     * Each position stores a Bucket, and each Bucket owns a BST.
     *
     * We initialize every bucket in the constructor so hash(key) can
     * always safely access bucketArray.get(hash(key)).
     */
    private final List<Bucket> bucketArray;

    public MyHashSet() {
        bucketArray = new ArrayList<>(keyRange);

        // ArrayList capacity alone does not create elements.
        // Therefore, explicitly create one Bucket for each index.
        for (int i = 0; i < keyRange; i++) {
            bucketArray.add(new Bucket());
        }
    }

    /*
     * Insert the key into the bucket determined by its hash.
     *
     * Example:
     * keyRange = 769
     * key = 1000
     * hash(1000) = 1000 % 769 = 231
     *
     * The BST in bucket 231 handles the actual insertion.
     */
    public void add(int key) {
        int index = hash(key);
        bucketArray.get(index).add(key);
    }

    /*
     * Remove the key from its corresponding bucket.
     *
     * If the key does not exist, the BST removal operation leaves
     * the bucket unchanged.
     */
    public void remove(int key) {
        int index = hash(key);
        bucketArray.get(index).remove(key);
    }

    /*
     * Search only the bucket to which the key maps.
     *
     * We do not search all 769 buckets because the hash function tells
     * us exactly where the key would be stored.
     */
    public boolean contains(int key) {
        int index = hash(key);
        return bucketArray.get(index).contains(key);
    }

    /*
     * Convert a key into a valid bucket index.
     *
     * Since keys are nonnegative, key % keyRange is always in
     * [0, keyRange - 1].
     */
    private int hash(int key) {
        return key % keyRange;
    }
}


/*
 * A Bucket handles collisions by storing all of its keys in a BST.
 *
 * For example, with keyRange = 769:
 *   5, 774, and 1543 all map to bucket 5.
 *
 * These keys remain distinguishable because the BST stores their
 * original values, not just their hash indexes.
 */
class Bucket {

    private final BinarySearchTree bst;

    public Bucket() {
        bst = new BinarySearchTree();
    }

    /*
     * The BST insert method returns the root of the resulting tree.
     *
     * IMPORTANT:
     * Always assign that returned root back to bst.root.
     * This matters especially when inserting into an initially empty
     * bucket, because its root changes from null to a new node.
     */
    public void add(int value) {
        bst.root = bst.insert(bst.root, value);
    }

    /*
     * BST deletion may replace the root node or make the tree empty.
     * Therefore, we must also save the returned root here.
     */
    public void remove(int value) {
        bst.root = bst.remove(bst.root, value);
    }

    /*
     * find() returns the matching TreeNode when the value exists.
     * If it does not exist, find() returns null.
     */
    public boolean contains(int value) {
        return bst.find(bst.root, value) != null;
    }
}


/*
 * A standard Binary Search Tree.
 *
 * BST ordering rule:
 *   Every value in the left subtree is smaller than the node's value.
 *   Every value in the right subtree is greater than the node's value.
 *
 * This ordering lets us eliminate one subtree at each comparison.
 *
 * Important complexity detail:
 * This is an ordinary, unbalanced BST. It can become a chain, so its
 * operations are O(h), where h is the tree height, and can be O(n)
 * in the worst case. It does NOT guarantee O(log n).
 */
class BinarySearchTree {

    public TreeNode root;

    public BinarySearchTree() {
        root = null;
    }

    /*
     * SEARCH
     *
     * If the current node is null, the value does not exist.
     * If the current value matches, we have found it.
     * Otherwise:
     *   - Search left if the target is smaller.
     *   - Search right if the target is larger.
     *
     * Time: O(h), where h is the BST height.
     * Extra space: O(h) due to recursive calls.
     */
    public TreeNode find(TreeNode node, int value) {
        if (node == null || value == node.val) {
            return node;
        }

        return value < node.val
                ? find(node.left, value)
                : find(node.right, value);
    }

    /*
     * INSERT
     *
     * Find the correct empty position according to the BST ordering.
     *
     * When node == null, create a new node.
     *
     * When value equals node.val, do nothing. This is essential:
     * a set must not contain duplicate values.
     *
     * Time: O(h).
     * Extra space: O(h) due to recursion.
     */
    public TreeNode insert(TreeNode node, int value) {
        if (node == null) {
            return new TreeNode(value);
        }

        if (value > node.val) {
            node.right = insert(node.right, value);
        } else if (value < node.val) {
            node.left = insert(node.left, value);
        }

        // Return the original root of this subtree.
        return node;
    }

    /*
     * SUCCESSOR
     *
     * Find the smallest value in the right subtree.
     *
     * Starting at node.right, keep moving left.
     * The leftmost node is the in-order successor.
     *
     * This helper is called only when the node has a right child.
     *
     * Time: O(h) in the worst case.
     */
    private int findSuccessor(TreeNode node) {
        node = node.right;

        while (node.left != null) {
            node = node.left;
        }

        return node.val;
    }

    /*
     * PREDECESSOR
     *
     * Find the largest value in the left subtree.
     *
     * Starting at node.left, keep moving right.
     * The rightmost node is the in-order predecessor.
     *
     * This helper is called only when the node has a left child.
     *
     * Time: O(h) in the worst case.
     */
    private int findPredecessor(TreeNode node) {
        node = node.left;

        while (node.right != null) {
            node = node.right;
        }

        return node.val;
    }

    /*
     * DELETE
     *
     * There are three cases when the target node is found:
     *
     * CASE 1: No children
     *         Return null to remove the leaf.
     *
     * CASE 2: At least one right child
     *         Replace the node's value with its in-order successor,
     *         then delete that successor from the right subtree.
     *
     * CASE 3: No right child, but a left child exists
     *         Replace the node's value with its in-order predecessor,
     *         then delete that predecessor from the left subtree.
     *
     * Why use a successor or predecessor?
     * They preserve the BST ordering after the target value is removed.
     *
     * Why return the updated node?
     * Deletion may change the root of any subtree, including the
     * entire tree. Callers must reconnect the updated subtree.
     *
     * Time: O(h).
     * Extra space: O(h) due to recursive calls.
     */
    public TreeNode remove(TreeNode node, int key) {

        // The key was not found. There is nothing to delete.
        if (node == null) {
            return null;
        }

        // The target must be in the right subtree.
        if (key > node.val) {
            node.right = remove(node.right, key);
        }

        // The target must be in the left subtree.
        else if (key < node.val) {
            node.left = remove(node.left, key);
        }

        // The target has been found.
        else {

            // CASE 1: Leaf node.
            // Removing it simply makes this subtree empty.
            if (node.left == null && node.right == null) {
                return null;
            }

            // CASE 2: Right subtree exists.
            // Use the successor to preserve the BST ordering.
            else if (node.right != null) {
                node.val = findSuccessor(node);

                // Remove the duplicate successor value from its
                // original location in the right subtree.
                node.right = remove(node.right, node.val);
            }

            // CASE 3: No right subtree, but a left subtree exists.
            // Use the predecessor to preserve the BST ordering.
            else {
                node.val = findPredecessor(node);

                // Remove the duplicate predecessor value from its
                // original location in the left subtree.
                node.left = remove(node.left, node.val);
            }
        }

        return node;
    }
}


/*
 * A single node in the BST.
 *
 * val   -> the stored key
 * left  -> smaller keys
 * right -> larger keys
 *
 * The fields are public here to match the supplied implementation.
 * In production code, they could be private.
 */
class TreeNode {

    public int val;
    public TreeNode left;
    public TreeNode right;

    public TreeNode(int val) {
        this.val = val;
        left = null;
        right = null;
    }
}

























import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MyHashSet {

    /*
     * STEP 1: Choose the number of buckets.
     *
     * We create 1,000 buckets to distribute keys across the array.
     *
     * The maximum key can be 1,000,000, but we do NOT need
     * 1,000,001 buckets because multiple keys can share a bucket.
     *
     * For example:
     *   hash(5)    = 5 % 1000 = 5
     *   hash(1005) = 1005 % 1000 = 5
     *
     * Both keys go into bucket 5. The linked list in that bucket
     * will distinguish between the actual keys.
     *
     * Space for the bucket structure: O(SIZE).
     */
    private static final int SIZE = 1000;

    /*
     * STEP 2: Create the bucket array.
     *
     * Each bucket contains a LinkedList of Integer keys.
     *
     * Why a list inside each bucket?
     * Because different keys can produce the same hash index.
     * This situation is called a HASH COLLISION.
     *
     * We use separate chaining to resolve collisions:
     * every bucket stores all keys mapped to that bucket.
     *
     * We are NOT using a built-in hash table. ArrayList and
     * LinkedList are ordinary collection structures.
     */
    private final List<LinkedList<Integer>> buckets;

    /*
     * CONSTRUCTOR
     *
     * Create the array of buckets and initialize every bucket
     * with an empty linked list.
     *
     * Important:
     * new ArrayList<>(SIZE) reserves capacity but does not create
     * SIZE elements. Therefore, we explicitly add each list.
     *
     * Time complexity: O(SIZE)
     * - The loop runs exactly SIZE times.
     * - Creating each empty linked list takes O(1) time.
     *
     * Space complexity: O(SIZE)
     * - We store SIZE bucket references and create SIZE lists.
     * - The bucket structure exists even when the set is empty.
     */
    public MyHashSet() {
        buckets = new ArrayList<>(SIZE);

        for (int i = 0; i < SIZE; i++) {
            buckets.add(new LinkedList<>());
        }
    }

    /*
     * HASH FUNCTION
     *
     * Convert a key into a valid bucket index.
     *
     * Since keys are between 0 and 1,000,000:
     *
     *   key % SIZE
     *
     * always returns an index between 0 and SIZE - 1.
     *
     * Example:
     *   hash(25)   = 25 % 1000   = 25
     *   hash(1025) = 1025 % 1000 = 25
     *
     * Both keys map to bucket 25.
     *
     * Time complexity: O(1)
     * - One modulo operation.
     *
     * Space complexity: O(1)
     * - No additional data structure is created.
     */
    private int hash(int key) {
        return key % SIZE;
    }

    /*
     * ADD OPERATION
     *
     * Insert the key into the set if it does not already exist.
     *
     * Step 1: Calculate the bucket index using the hash function.
     * Step 2: Get the linked list at that index.
     * Step 3: Search the list to avoid inserting a duplicate.
     * Step 4: If the key is absent, append it to the list.
     *
     * Example:
     *   add(5)    -> bucket 5 contains [5]
     *   add(1005) -> bucket 5 contains [5, 1005]
     *   add(5)    -> bucket 5 remains [5, 1005]
     *
     * Why check for duplicates?
     * A set must store each distinct key only once.
     *
     * Time complexity:
     * - Hash calculation: O(1).
     * - Accessing a bucket: O(1).
     * - bucket.contains(key): O(k), where k is the number of
     *   elements in this particular bucket.
     * - Appending to the linked list: O(1) for Java LinkedList.
     *
     * Total: O(1 + k) = O(k).
     *
     * Average case: O(1), assuming keys are distributed reasonably
     * evenly and bucket lists remain small.
     *
     * Worst case: O(n), where n is the total number of stored keys.
     * This can happen when all keys collide into one bucket.
     *
     * Extra space per operation: O(1) auxiliary space.
     * If the key is new, one list node is allocated, so the
     * additional storage for that insertion is O(1).
     */
    public void add(int key) {
        int index = hash(key);

        LinkedList<Integer> bucket = buckets.get(index);

        // Do not insert a duplicate key.
        if (!bucket.contains(key)) {
            bucket.add(key);
        }
    }

    /*
     * REMOVE OPERATION
     *
     * Remove the key if it exists.
     *
     * Step 1: Calculate the bucket index.
     * Step 2: Access the linked list for that bucket.
     * Step 3: Search for the key and remove its node if found.
     *
     * IMPORTANT JAVA DETAIL:
     *
     * bucket.remove(Integer.valueOf(key))
     *
     * removes the Integer value.
     *
     * If we instead wrote bucket.remove(key), Java would choose
     * remove(int index), because key is an int. That would attempt
     * to remove the element at the specified list index, not the
     * element whose value equals key.
     *
     * If the key does not exist, remove(Integer.valueOf(key))
     * simply returns false and does nothing.
     *
     * Time complexity:
     * - Hash calculation: O(1).
     * - Bucket access: O(1).
     * - Searching for the key: O(k).
     * - Unlinking the matching node: O(1) once found.
     *
     * Total: O(k).
     *
     * Average case: O(1), assuming a good key distribution and
     * a reasonably small bucket.
     *
     * Worst case: O(n), when the relevant bucket contains all keys.
     *
     * Extra space per operation: O(1) auxiliary space.
     * Removing a node does not require creating another collection.
     */
    public void remove(int key) {
        int index = hash(key);

        LinkedList<Integer> bucket = buckets.get(index);

        // Remove the matching value, not the element at index key.
        bucket.remove(Integer.valueOf(key));
    }

    /*
     * CONTAINS OPERATION
     *
     * Return true if the key exists; otherwise, return false.
     *
     * Step 1: Calculate the bucket index.
     * Step 2: Access only that bucket.
     * Step 3: Search its linked list for the key.
     *
     * We do NOT search all buckets. The hash function tells us
     * which bucket can contain the requested key.
     *
     * Time complexity:
     * - Hash calculation: O(1).
     * - Bucket access: O(1).
     * - Linked-list search: O(k).
     *
     * Total: O(k).
     *
     * Average case: O(1), assuming keys are distributed reasonably
     * evenly among buckets.
     *
     * Worst case: O(n), if every key is in the same bucket.
     *
     * Extra space per operation: O(1).
     * The search does not create any additional data structure.
     */
    public boolean contains(int key) {
        int index = hash(key);

        LinkedList<Integer> bucket = buckets.get(index);

        return bucket.contains(key);
    }
}


/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a Skiplist without using any built-in libraries.
 * 
 * A skiplist is a probabilistic data structure that allows O(log(n)) search, 
 * add, and erase operations. It consists of multiple layers of sorted linked 
 * lists, where top layers act as "express lanes" to skip over elements.
 * 
 * Implement the Skiplist class:
 * - Skiplist(): Initializes the object.
 * - bool search(int target): Returns true if target exists.
 * - void add(int num): Inserts num.
 * - bool erase(int num): Removes num and returns true. If num doesn't exist, 
 *   returns false. (If duplicates exist, removing any one is fine).
 * 
 * Constraints:
 * - 0 <= num, target <= 2 * 10^4
 * - At most 5 * 10^4 calls will be made in total.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. What is the maximum number of elements we expect? 
 *    (50,000 elements. This dictates our MAX_LEVEL. Since 2^16 = 65536, 
 *    a MAX_LEVEL of 16 is mathematically optimal to achieve O(log N)).
 * 2. Are duplicates allowed, and how should they be handled?
 *    (Yes, the prompt allows duplicates. Erase should just remove the *first* 
 *    instance it finds and leave the rest).
 * 3. Can we use dynamic arrays (ArrayList) for the node's forward pointers?
 *    (The prompt says "no built-in libraries", but usually standard arrays `[]` 
 *    are perfectly fine and much more performant).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Provide O(log N) search, insert, and delete on a dynamic 
 *   dataset without the immense code complexity of a Red-Black Tree or AVL Tree.
 * 
 * --- Approach 1: Sorted Linked List ---
 * 1. Intuition: Just keep a single linked list sorted.
 * 2. Why it fails: Finding the insertion point requires traversing node-by-node, 
 *    taking O(N) time. With 50,000 calls, O(N^2) total time causes TLE.
 * 
 * --- Approach 2: Balanced BST (Red-Black Tree) ---
 * 1. Intuition: A BST guarantees O(log N) operations.
 * 2. Why it's suboptimal (pedagogically): Implementing a Red-Black tree from 
 *    scratch in a 45-minute interview is notoriously difficult. The rotation 
 *    and recoloring logic, especially for deletion, requires hundreds of lines 
 *    of precise, error-prone code.
 * 
 * --- Approach 3: Skiplist (Probabilistic Express Lanes) [Optimal] ---
 * 1. Intuition: What if a linked list had a subway system? Layer 0 is the local 
 *    train (visits every node). Layer 1 is an express train (visits every 2nd node). 
 *    Layer 2 is a super-express (visits every 4th node). 
 * 2. The probabilistic trick: Instead of rigidly enforcing "every 2nd node", we 
 *    flip a coin when inserting. Every node is added to Layer 0. If it lands heads (50%), 
 *    we also add it to Layer 1. Heads again? Layer 2. This loosely mimics a perfectly 
 *    balanced binary tree but requires zero rotations or rebalancing!
 * 3. Why it works: 
 *    - `search`: Start at the highest layer. Move right if the next node is smaller. 
 *      If it's bigger, drop down a layer. Repeat until at Layer 0.
 *    - `add` / `erase`: We just need a "breadcrumb trail" (the `update` array) of 
 *      the last node we visited at each layer before dropping down. We use this trail 
 *      to wire up our new node's pointers across multiple levels.
 * 4. Which to write in an interview? Approach 3. It achieves O(log N) using simple 
 *    linked list mechanics.
 * 
 * Time Complexity: Amortized O(log N) for search, add, erase. (Worst case O(N) 
 *   if the coin flips terribly, but probabilistically impossible at scale).
 * Space Complexity: O(N) average.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Erasing a non-existent element: Handled safely; the bottom layer check `curr.val == num` fails.
 * - Erasing a duplicate: We just find the first instance. Because layers are contiguous, 
 *   if we unlink *that specific instance* from all layers, the duplicates remain untouched.
 * - Coin flip goes crazy: We strictly cap `randomLevel()` at `MAX_LEVEL` (16) to prevent 
 *   memory out-of-bounds exceptions or infinite loops.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "The Update Array (Breadcrumb Trail)." 
 *   When traversing a skiplist, you step RIGHT or DOWN. Every time you step DOWN, 
 *   you record the node you are standing on in an `update` array. This array represents 
 *   the inner corners of the staircase you walked. If you need to insert or delete a 
 *   node, the `update` array gives you immediate O(1) access to the exact nodes 
 *   whose pointers need to be rewired at every single level!
 * 
 * ASCII DRY RUN (The Breadcrumb Trail)
 * Searching for 45 to insert it.
 * 
 * L3: [head] ---------------------------------> [50]
 * L2: [head] ---------> [30] -----------------> [50]
 * L1: [head] ---------> [30] -------> [40] ---> [50]
 * L0: [head] -> [10] -> [30] -> [35]->[40] ---> [50]
 * 
 * Path taken: 
 * - L3: head -> right is 50. 50 > 45. Drop down. `update[3] = head`.
 * - L2: head -> right is 30. 30 < 45. Move to 30.
 * - L2: 30 -> right is 50. 50 > 45. Drop down. `update[2] = 30`.
 * - L1: 30 -> right is 40. 40 < 45. Move to 40.
 * - L1: 40 -> right is 50. 50 > 45. Drop down. `update[1] = 40`.
 * - L0: 40 -> right is 50. 50 > 45. Drop down. `update[0] = 40`.
 * 
 * Resulting Update Array: [40, 40, 30, head]
 * Now, if coin flips level=3, we simply link newNode to update[0], update[1], and update[2].
 * 
 * Pitfalls:
 * - Over-complicating the Node class. Don't use `down` and `right` pointers explicitly 
 *   as a 2D grid of separate objects. Just give each `Node` an array of `forward` 
 *   pointers. `node.forward[i]` is the next node at level `i`.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: Why would a real-world system use a Skiplist instead of a Red-Black Tree?
 * A1: Skiplists are famously used in Redis for its Sorted Sets (ZSET). 
 *     1. They are vastly easier to implement and debug.
 *     2. Range queries (e.g., "give me top 50 elements") are trivial: just traverse Layer 0.
 *     3. Concurrency! In a Red-Black tree, a rebalancing rotation locks huge portions of 
 *        the tree. In a Skiplist, inserts are extremely localized, making fine-grained 
 *        locking or lock-free implementations much easier.
 */

public class SkiplistStudy {

    static class Skiplist {

        // Maximum level of the skiplist. 2^16 = 65536, which covers the 50,000 constraint.
        private static final int MAX_LEVEL = 16;
        // Probability for coin flips
        private static final double P = 0.5;

        /**
         * Represents a single element in the Skiplist.
         * Contains an array of forward pointers, where forward[i] points 
         * to the next node at level i.
         */
        private static class Node {
            int val;
            Node[] forward;

            public Node(int val, int level) {
                this.val = val;
                this.forward = new Node[level];
            }
        }

        // The dummy head node. It acts as the absolute left boundary across all levels.
        private final Node head;

        /**
         * Initializes the object of the skiplist.
         */
        public Skiplist() {
            // Head holds a dummy value and is permanently at MAX_LEVEL
            head = new Node(-1, MAX_LEVEL);
        }

        /**
         * Simulates flipping a coin to determine how many levels a new node spans.
         */
        private int randomLevel() {
            int level = 1;
            // 50% chance to grow a level, strictly capped at MAX_LEVEL
            while (Math.random() < P && level < MAX_LEVEL) {
                level++;
            }
            return level;
        }

        /**
         * Returns true if the integer target exists in the Skiplist.
         * Time Complexity: O(log N)
         */
        public boolean search(int target) {
            Node curr = head;
            
            // Traverse from the top express lane down to the local lane (0)
            for (int i = MAX_LEVEL - 1; i >= 0; i--) {
                // Move forward in the current lane as long as the next value is strictly less
                while (curr.forward[i] != null && curr.forward[i].val < target) {
                    curr = curr.forward[i];
                }
                // Once the next value is >= target, the loop ends, and we drop down one level (i--)
            }
            
            // We are now at level 0, standing on the node strictly smaller than target.
            // The very next node must be the target, if it exists.
            curr = curr.forward[0];
            
            return curr != null && curr.val == target;
        }

        /**
         * Inserts the value num into the SkipList.
         * Time Complexity: O(log N)
         */
        public void add(int num) {
            // The breadcrumb trail of the last node visited at each level before dropping down
            Node[] update = new Node[MAX_LEVEL];
            Node curr = head;

            // 1. Search for the insertion point, recording our staircase drops
            for (int i = MAX_LEVEL - 1; i >= 0; i--) {
                while (curr.forward[i] != null && curr.forward[i].val < num) {
                    curr = curr.forward[i];
                }
                update[i] = curr;
            }

            // 2. Generate a random height for the new node
            int lvl = randomLevel();
            Node newNode = new Node(num, lvl);

            // 3. Splice the new node into the linked lists at all applicable levels
            for (int i = 0; i < lvl; i++) {
                newNode.forward[i] = update[i].forward[i];
                update[i].forward[i] = newNode;
            }
        }

        /**
         * Removes the value num from the Skiplist and returns true.
         * Time Complexity: O(log N)
         */
        public boolean erase(int num) {
            Node[] update = new Node[MAX_LEVEL];
            Node curr = head;

            // 1. Search for the exact node, recording our staircase drops
            for (int i = MAX_LEVEL - 1; i >= 0; i--) {
                while (curr.forward[i] != null && curr.forward[i].val < num) {
                    curr = curr.forward[i];
                }
                update[i] = curr;
            }

            // The node we want to delete should be immediately after 'curr' at level 0
            curr = curr.forward[0];

            // 2. If we found it, unlink it
            if (curr != null && curr.val == num) {
                for (int i = 0; i < MAX_LEVEL; i++) {
                    // Since a node's levels are contiguous from 0 upwards, 
                    // if it doesn't exist at level i, it won't exist at i+1.
                    if (update[i].forward[i] != curr) {
                        break; 
                    }
                    // Splice it out of the linked list at level i
                    update[i].forward[i] = curr.forward[i];
                }
                return true; // Successfully deleted
            }

            return false; // Not found
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        Skiplist skiplist = new Skiplist();
        
        System.out.println("Adding 1, 2, 3...");
        skiplist.add(1);
        skiplist.add(2);
        skiplist.add(3);
        
        System.out.println("Search 0: " + skiplist.search(0)); // Expected: false
        
        System.out.println("Adding 4...");
        skiplist.add(4);
        
        System.out.println("Search 1: " + skiplist.search(1)); // Expected: true
        
        System.out.println("Erase 0: " + skiplist.erase(0));   // Expected: false (not in skiplist)
        System.out.println("Erase 1: " + skiplist.erase(1));   // Expected: true
        
        System.out.println("Search 1 after erase: " + skiplist.search(1)); // Expected: false
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Multi-level Linked List + Breadcrumb Trail Array.
 * Key Observation: Instead of complex binary tree rotations, we can achieve 
 * O(log N) operations by stacking linked lists on top of each other. The density 
 * of each layer decreases probabilistically, creating natural "express lanes".
 * What to Memorize: The `update` array paradigm. Every operation starts with a 
 * top-down search loop that populates `update[i] = curr` right before dropping 
 * down a level. This cleanly isolates the search logic from the mutation logic.
 * Most Common Trap: Forgetting that a deleted node might not span all the way 
 * to `MAX_LEVEL`. During deletion, you must `break` early if `update[i].forward[i] != curr`.
 * Mental Trigger: "O(log N) dynamic data without trees" -> "Skiplist / Express Lanes".
 * ============================================================================
 */










































import java.util.concurrent.ThreadLocalRandom;

class Skiplist {

    /*
     * APPROACH 2: RANDOMIZED SKIPLIST
     *
     * Core idea:
     * Maintain multiple sorted linked-list levels so that searching
     * can jump over many values instead of visiting them one by one.
     *
     * Level 0 contains every node.
     * Level 1 contains some of the level-0 nodes.
     * Level 2 contains some of the level-1 nodes.
     * And so on.
     *
     * Each higher-level node also belongs to every lower level.
     *
     * MAX_LEVEL = 32 is a fixed upper bound. With at most 50,000
     * operations, this is comfortably large for expected performance.
     *
     * Random promotion probability = 1/2.
     * Therefore, the expected number of forward pointers per node
     * is bounded by a constant.
     */

    private static final int MAX_LEVEL = 32;

    /*
     * Node represents one value in the Skiplist.
     *
     * next[i] is the forward pointer at level i.
     *
     * Examples:
     *   next.length == 1  -> node appears only at level 0.
     *   next.length == 2  -> node appears at levels 0 and 1.
     *   next.length == 3  -> node appears at levels 0, 1, and 2.
     *
     * A node's height is determined when it is inserted and does not
     * change during its lifetime.
     */
    private static final class Node {
        final int value;
        final Node[] next;

        Node(int value, int level) {
            this.value = value;
            this.next = new Node[level];
        }
    }

    /*
     * The head is a sentinel node, not a real stored value.
     *
     * It has pointers for every possible level, allowing us to start
     * a search at the highest level without special cases.
     *
     * Its value is never used to decide whether a real value exists.
     */
    private final Node head = new Node(-1, MAX_LEVEL);

    /*
     * Randomly choose the node's height.
     *
     * Every node gets at least level 0.
     * Each successful coin flip adds one more level.
     * Stop when the coin flip fails or MAX_LEVEL is reached.
     *
     * For promotion probability p = 1/2:
     *   P(height >= 1) = 1
     *   P(height >= 2) = 1/2
     *   P(height >= 3) = 1/4
     *   P(height >= 4) = 1/8
     *
     * This geometric distribution makes higher levels increasingly
     * sparse, creating useful express lanes.
     *
     * Time: O(MAX_LEVEL), which is O(1) for our fixed bound.
     * Extra space: O(1).
     */
    private int randomLevel() {
        int level = 1;

        while (level < MAX_LEVEL
                && ThreadLocalRandom.current().nextBoolean()) {
            level++;
        }

        return level;
    }

    public boolean search(int target) {
        /*
         * MENTAL MODEL:
         * Move RIGHT when the next value is smaller than target.
         * Move DOWN when moving right would overshoot target.
         *
         * Start at the head's highest level.
         *
         * At each level:
         *   1. Advance while the next value is < target.
         *   2. If we cannot advance, descend one level.
         *
         * Because every level is sorted, skipping a node at an upper
         * level cannot cause us to miss target: that node is also
         * represented at all lower levels.
         *
         * After descending to level 0, current is the last node
         * strictly smaller than target.
         * Therefore, current.next[0] is the first possible match.
         *
         * Example: search(45) in
         *
         * Level 2: HEAD ----------------> 50
         * Level 1: HEAD ------> 30 -----> 50
         * Level 0: HEAD -> 10 -> 30 -> 40 -> 50
         *
         * At level 2, 50 > 45, so descend.
         * At level 1, move to 30; 50 > 45, so descend.
         * At level 0, move to 40; 50 > 45, so stop.
         * The next node is 50, not 45, so return false.
         *
         * Expected time: O(log n).
         * Worst-case time: O(n), if the random levels are badly
         * distributed and the structure behaves almost like one list.
         * Extra space: O(1).
         */
        Node current = head;

        for (int level = MAX_LEVEL - 1; level >= 0; level--) {
            while (current.next[level] != null
                    && current.next[level].value < target) {
                current = current.next[level];
            }
        }

        current = current.next[0];

        return current != null && current.value == target;
    }

    public void add(int num) {
        /*
         * THE MOST IMPORTANT INSERTION IDEA:
         *
         * Before inserting, find the predecessor at EVERY level.
         *
         * update[i] stores the last node at level i whose value
         * is strictly smaller than num.
         *
         * Why do we need an update array?
         * A new node may participate in multiple levels. At each
         * of those levels, we need to know which existing pointer
         * must be redirected to the new node.
         *
         * STEP 1: Find all predecessors.
         *
         * Traverse from the highest level down to level 0.
         * At every level, move right while the next value < num.
         * Save the resulting node in update[level].
         *
         * Because we use < rather than <=, a duplicate is inserted
         * before existing equal values. This is valid because the
         * problem permits duplicates and does not require stable order.
         */
        Node[] update = new Node[MAX_LEVEL];
        Node current = head;

        for (int level = MAX_LEVEL - 1; level >= 0; level--) {
            while (current.next[level] != null
                    && current.next[level].value < num) {
                current = current.next[level];
            }

            update[level] = current;
        }

        /*
         * STEP 2: Randomly choose the new node's height.
         *
         * If its height is 3, it participates in levels 0, 1, and 2.
         * No pointers at levels 3 and above need to be changed.
         */
        int nodeLevel = randomLevel();
        Node newNode = new Node(num, nodeLevel);

        /*
         * STEP 3: Splice the new node into each level it participates in.
         *
         * At a particular level i, the old connection is:
         *
         *     update[i] ------------> successor
         *
         * We change it to:
         *
         *     update[i] -> newNode -> successor
         *
         * The order of the two assignments matters:
         * First preserve the old successor in newNode.next[i].
         * Then redirect update[i].next[i] to newNode.
         *
         * Every insertion always updates level 0, ensuring that
         * the value is stored even if its random height is 1.
         *
         * Expected time: O(log n).
         * Worst-case time: O(n), due to predecessor traversal.
         *
         * Extra temporary space: O(MAX_LEVEL), which is O(1) with
         * our fixed maximum level. The new node adds its own pointers.
         */
        for (int level = 0; level < nodeLevel; level++) {
            newNode.next[level] = update[level].next[level];
            update[level].next[level] = newNode;
        }
    }

    public boolean erase(int num) {
        /*
         * Deletion uses almost the same traversal as insertion.
         *
         * STEP 1: Find the predecessor at every level.
         *
         * update[i] is the node immediately before the position
         * where num would appear at level i.
         *
         * We stop before the first equal value, so duplicates are
         * handled by removing just one occurrence.
         */
        Node[] update = new Node[MAX_LEVEL];
        Node current = head;

        for (int level = MAX_LEVEL - 1; level >= 0; level--) {
            while (current.next[level] != null
                    && current.next[level].value < num) {
                current = current.next[level];
            }

            update[level] = current;
        }

        /*
         * STEP 2: Check the bottom level for the actual target.
         *
         * We must check equality, not merely whether a next node
         * exists. If the next value is greater, num is absent.
         *
         * No pointers have been changed yet, so an unsuccessful
         * deletion leaves the structure exactly as it was.
         */
        Node target = update[0].next[0];

        if (target == null || target.value != num) {
            return false;
        }

        /*
         * STEP 3: Unlink the target at every level where it appears.
         *
         * Before:
         *
         *     predecessor ------> target ------> successor
         *
         * After:
         *
         *     predecessor --------------------> successor
         *
         * IMPORTANT:
         * We cannot blindly access target.next[level] for all levels.
         * A short node might have only next[0] and not next[1].
         *
         * The condition update[level].next[level] == target is safe:
         * it can be true only at a level where target actually exists.
         * Java's short-circuit evaluation then guarantees that
         * target.next[level] is read only when that pointer exists.
         *
         * The target is removed from every level that points to it,
         * while other duplicate nodes remain untouched.
         *
         * Expected time: O(log n).
         * Worst-case time: O(n).
         * Extra temporary space: O(MAX_LEVEL), or O(1) for this
         * fixed maximum height.
         */
        for (int level = 0; level < MAX_LEVEL; level++) {
            if (update[level].next[level] == target) {
                update[level].next[level] = target.next[level];
            }
        }

        return true;
    }
}

/*
 * COMPLEXITY SUMMARY
 *
 * Let n be the number of stored elements.
 *
 * Sorted linked list:
 *   search: O(n) worst case
 *   add:    O(n) worst case
 *   erase:  O(n) worst case
 *   space:  O(n)
 *
 * Randomized Skiplist:
 *   search: O(log n) expected, O(n) worst case
 *   add:    O(log n) expected, O(n) worst case
 *   erase:  O(log n) expected, O(n) worst case
 *   space:  O(n) expected total space
 *
 * Why is expected space O(n)?
 * With promotion probability 1/2, the expected number of pointers
 * per node is bounded by a constant (approximately 2 when the height
 * is not capped). Therefore, n nodes need O(n) total pointers on
 * average. The fixed MAX_LEVEL also bounds every individual node's
 * pointer array.
 */


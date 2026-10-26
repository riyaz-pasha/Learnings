/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a Phone Directory which supports the following operations:
 * 
 * Implement the PhoneDirectory class:
 * - PhoneDirectory(int maxNumbers): Initializes the phone directory with the 
 *   number of available numbers maxNumbers, ranging from 0 to maxNumbers - 1.
 * - int get(): Provides a number that is not assigned to anyone. Returns -1 
 *   if no number is available.
 * - boolean check(int number): Checks if a number is available or not.
 * - void release(int number): Recycles or releases a number.
 * 
 * Constraints:
 * - 1 <= maxNumbers <= 10^4
 * - 0 <= number < maxNumbers
 * - At most 2 * 10^4 calls will be made to get, check, and release.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Can `release` be called on a number that is already available?
 *    (Yes. If a number is already available, releasing it again should have no 
 *    effect. We must prevent it from being duplicated in our available pool).
 * 2. Does `get()` need to return the smallest available number?
 *    (No, any available number is acceptable. This gives us freedom to use 
 *    data structures like standard Queues or HashSets instead of Min-Heaps).
 * 3. Are there concurrent accesses?
 *    (Assuming single-threaded for standard LeetCode. For production, we'd 
 *    need synchronization blocks).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need O(1) time to find a free number, O(1) time to 
 *   check if a specific number is free, and O(1) time to free a number. The 
 *   binding constraint is preventing duplicates when releasing.
 * 
 * --- Approach 1: Boolean Array (Brute Force) ---
 * 1. Intuition: Keep an array `boolean[] available` of size `maxNumbers`.
 * 2. Try: 
 *    - `check`: Return `available[num]`. (O(1))
 *    - `release`: `available[num] = true`. (O(1))
 *    - `get`: Loop from 0 to maxNumbers-1 to find the first `true`, set it 
 *      to `false`, and return it. (O(N))
 * 3. Why it fails to scale: `get()` takes O(N) time. With 10^4 numbers and 
 *    20,000 operations, this can cause Time Limit Exceeded (TLE).
 * 4. Bottleneck: Linearly searching for available slots.
 * 
 * --- Approach 2: HashSet + Counter ---
 * 1. Intuition: Track the `used` numbers in a HashSet.
 * 2. Try: 
 *    - Use an integer `next` starting at 0. 
 *    - `get`: If `next < maxNumbers`, return `next++`. If `next` is exhausted, 
 *      we look into a `released` pool (e.g., a Queue).
 * 3. Why it works: Great for lazy initialization, saving memory if `maxNumbers` 
 *    is huge but few are requested. 
 * 4. Drawback: Slightly more complex state management to balance `next` and 
 *    the recycled pool.
 * 
 * --- Approach 3: Queue + Boolean Array (Optimal & Cleanest) ---
 * 1. Intuition: A Queue perfectly dispenses available items in O(1). A boolean 
 *    array perfectly checks availability in O(1). Combining them solves everything.
 * 2. Try: 
 *    - `Queue<Integer> availableQueue`: initially populated with 0 to maxNumbers-1.
 *    - `boolean[] isAvailable`: initially all `true`.
 *    - `get`: `poll` from queue, mark `false` in array.
 *    - `check`: return array lookup.
 *    - `release`: If `!isAvailable[num]`, mark `true` in array and `offer` to queue.
 * 3. Why it works: Completely decouples the act of "finding" an item from "checking" 
 *    its state.
 * 4. Which to write in an interview?
 *    Approach 3. It is universally understood, guarantees strict O(1) time for all 
 *    operations, and trivially prevents the "duplicate release" bug.
 * 
 * Time Complexity: O(1) for get, check, and release. (O(N) initialization).
 * Space Complexity: O(N) to store the Queue and Boolean Array.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Exhausting the directory: Calling `get()` when the queue is empty correctly 
 *   returns `-1`.
 * - The Duplicate Release Trap: Calling `release(5)` twice. Our boolean array 
 *   check `if (!isAvailable[number])` stops the second call from adding `5` to 
 *   the queue a second time, avoiding corrupted state.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Separation of Duties." 
 *   The Queue's only job is to hand you a number without making you search for it. 
 *   The Array's only job is to act as the source of truth for state. When releasing, 
 *   always consult the Array before updating the Queue.
 * 
 * ASCII DRY RUN (Queue + Boolean Array)
 * Init(3) -> Queue: [0, 1, 2], isAvail: [T, T, T]
 * 
 * 1. get() -> polls 0.
 *    Queue: [1, 2], isAvail: [F, T, T]. Returns 0.
 * 
 * 2. get() -> polls 1.
 *    Queue: [2], isAvail: [F, F, T]. Returns 1.
 * 
 * 3. check(1) -> looks at isAvail[1]. Returns False.
 * 
 * 4. release(0) -> is 0 false? Yes. Add to Queue.
 *    Queue: [2, 0], isAvail: [T, F, T].
 * 
 * 5. release(0) -> is 0 false? No. DO NOTHING.
 *    (If we didn't check, Queue would become [2, 0, 0]!).
 * 
 * Pitfalls:
 * - Not checking availability before releasing. This pushes duplicate numbers 
 *   into the queue. Later, `get()` would hand out the same phone number to 
 *   two different users!
 * 
 * Pattern Recognition: "O(1) allocation/deallocation from a fixed pool" -> Queue + Boolean Array.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you optimize this if maxNumbers is 10^9?
 * A1: We cannot initialize an array or queue of size 10^9 (requires gigabytes of RAM). 
 *     We would use a Balanced BST (TreeSet in Java) to track *ranges* of available 
 *     numbers (e.g., `[0, 10^9 - 1]`). 
 *     - `get`: Pull the first interval, return its start, and push back `[start+1, end]`.
 *     - `release`: Add `[num, num]` and merge with adjacent intervals. 
 *     This changes time complexity to O(log R) where R is the number of fragmented ranges.
 * 
 * Q2: How can we implement this using a single Array (without a Queue)?
 * A2: We can build an implicit Linked List inside an array. An array `next[]` where 
 *     `next[i] = i + 1`. We maintain a `head` pointer. 
 *     - `get`: Take `head`, update `head = next[head]`.
 *     - `release`: `next[num] = head`, then `head = num`.
 *     This achieves O(1) time and cuts space by 50% compared to Queue + Array, but 
 *     is slightly trickier to implement perfectly under pressure.
 */

import java.util.LinkedList;
import java.util.Queue;

public class PhoneDirectoryStudy {

    static class PhoneDirectory {
        // The queue provides O(1) access to an available number
        private Queue<Integer> availableQueue;
        
        // The boolean array prevents O(1) duplicate releases
        private boolean[] isAvailable;

        /**
         * Initializes the phone directory with the number of available numbers.
         * Time Complexity: O(maxNumbers) for initialization.
         */
        public PhoneDirectory(int maxNumbers) {
            availableQueue = new LinkedList<>();
            isAvailable = new boolean[maxNumbers];
            
            // Populate the queue and set all numbers as available
            for (int i = 0; i < maxNumbers; i++) {
                availableQueue.offer(i);
                isAvailable[i] = true;
            }
        }
        
        /**
         * Provides a number that is not assigned to anyone.
         * Returns -1 if no number is available.
         * Time Complexity: O(1)
         */
        public int get() {
            // Guard clause: Directory is full / exhausted
            if (availableQueue.isEmpty()) {
                return -1;
            }
            
            // Get the next available number from the queue
            int assignedNumber = availableQueue.poll();
            
            // Update the source of truth
            isAvailable[assignedNumber] = false;
            
            return assignedNumber;
        }
        
        /**
         * Checks if a number is available or not.
         * Time Complexity: O(1)
         */
        public boolean check(int number) {
            // Out of bounds check is implicit based on constraints, 
            // but good practice if constraints were dynamic.
            if (number < 0 || number >= isAvailable.length) {
                return false;
            }
            return isAvailable[number];
        }
        
        /**
         * Recycles or releases a number.
         * Time Complexity: O(1)
         */
        public void release(int number) {
            // CRITICAL CHECK: Prevent duplicate releases.
            // If the number is already available, we do absolutely nothing.
            if (number >= 0 && number < isAvailable.length && !isAvailable[number]) {
                isAvailable[number] = true;
                availableQueue.offer(number);
            }
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        // Initialize directory with 3 numbers: 0, 1, 2
        PhoneDirectory directory = new PhoneDirectory(3);

        // Get two numbers
        System.out.println("Get: " + directory.get()); // Expected: 0 (or any available)
        System.out.println("Get: " + directory.get()); // Expected: 1 (or any available)
        
        // Check availability of 2
        System.out.println("Check 2: " + directory.check(2)); // Expected: true
        
        // Get the last number
        System.out.println("Get: " + directory.get()); // Expected: 2
        
        // Check availability of 2 again
        System.out.println("Check 2 after get: " + directory.check(2)); // Expected: false
        
        // Attempt to get a number when full
        System.out.println("Get when full: " + directory.get()); // Expected: -1
        
        // Release number 2
        System.out.println("Releasing 2...");
        directory.release(2);
        
        // Check availability of 2
        System.out.println("Check 2 after release: " + directory.check(2)); // Expected: true
        
        // Duplicate release edge case test
        System.out.println("Releasing 2 again (duplicate)...");
        directory.release(2); // Should be a no-op
        
        // Get should return 2, and the queue should be empty again
        System.out.println("Get: " + directory.get()); // Expected: 2
        System.out.println("Get to ensure no duplicates: " + directory.get()); // Expected: -1
    }
}










































import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

class PhoneDirectory {
    private final Queue<Integer> availableQueue = new ArrayDeque<>();
    private final Set<Integer> availableSet = new HashSet<>();

    public PhoneDirectory(int maxNumbers) {
        // Initially, every number is available.
        // Insert each number into BOTH structures.
        for (int number = 0; number < maxNumbers; number++) {
            availableQueue.offer(number);
            availableSet.add(number);
        }
    }

    public int get() {
        // Poll returns null when the queue is empty.
        Integer number = availableQueue.poll();

        if (number == null) {
            return -1;
        }

        // The number is now assigned, so remove it from the set too.
        availableSet.remove(number);

        return number;
    }

    public boolean check(int number) {
        // The set is our fast source of truth for availability.
        return availableSet.contains(number);
    }

    public void release(int number) {
        // CRITICAL CHECK:
        // If the number is already available, do nothing.
        // Otherwise, repeated release calls could put duplicate copies
        // into the queue, allowing the same number to be allocated twice.
        if (availableSet.add(number)) {
            // Set.add returns true only when the number was absent.
            availableQueue.offer(number);
        }
    }
}

/*
 * Complexity, where n = maxNumbers:
 *
 * Constructor: O(n) time and O(n) space.
 * get():      O(1) average time.
 * check():    O(1) average time.
 * release():  O(1) average time.
 *
 * HashSet operations are average O(1), not guaranteed worst-case O(1).
 * Space is O(n) overall; the queue and set store the available numbers.
 *
 * Invariant:
 * The queue and set contain exactly the same available numbers.
 *
 * Why not just use the queue?
 * check(number) would otherwise require a search through the queue,
 * taking O(n) time. The hash set avoids that scan.
 */











































class PhoneDirectory {
    private final int[] next;
    private final boolean[] available;
    private int head;

    public PhoneDirectory(int maxNumbers) {
        next = new int[maxNumbers];
        available = new boolean[maxNumbers];

        // Build a linked list of every available number:
        // 0 -> 1 -> 2 -> ... -> maxNumbers - 1 -> -1
        for (int number = 0; number < maxNumbers; number++) {
            next[number] = number + 1;
            available[number] = true;
        }

        // The final number has no successor.
        next[maxNumbers - 1] = -1;

        // Initially, the first number is available.
        head = 0;
    }

    public int get() {
        // An empty free list means all numbers are assigned.
        if (head == -1) {
            return -1;
        }

        // Allocate the first available number.
        int number = head;

        // Remove it from the free list by advancing the head.
        head = next[number];

        // Track the state separately so check() and release()
        // can determine whether this number is currently available.
        available[number] = false;

        return number;
    }

    public boolean check(int number) {
        // Direct array lookup: no traversal is required.
        return available[number];
    }

    public void release(int number) {
        // If already free, do nothing. This prevents duplicate nodes
        // in our array-based free list.
        if (available[number]) {
            return;
        }

        // Insert this number at the front of the free list.
        next[number] = head;
        head = number;

        // The number is free again.
        available[number] = true;
    }
}

/*
 * Complexity, where n = maxNumbers:
 *
 * Constructor: O(n) time to initialize the arrays and free list.
 *             O(n) space for next[] and available[].
 *
 * get():      O(1) worst-case time: inspect head and advance it.
 * check():    O(1) worst-case time: inspect one boolean entry.
 * release():  O(1) worst-case time: inspect availability and update
 *             at most two array entries plus one boolean entry.
 *
 * Why this is the strongest of these three implementations:
 * 1. No scanning is needed.
 * 2. No HashSet or Queue is required.
 * 3. No boxed Integer objects or linked-list node objects are needed.
 * 4. Operations have deterministic O(1) time, assuming valid inputs.
 *
 * Trade-off:
 * The linked list returns the most recently released number first.
 * That is allowed because the problem permits any available number.
 */

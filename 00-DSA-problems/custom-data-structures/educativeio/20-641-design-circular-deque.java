/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design an implementation of a Circular Double-Ended Queue (Deque).
 * A circular deque allows insertion and deletion at both ends (front and rear),
 * reusing freed spaces by wrapping around a fixed-size array.
 * 
 * Implement the MyCircularDeque class:
 * - MyCircularDeque(k): Initializes the deque with a maximum size of k.
 * - insertFront(): Adds an item at the front. Returns true if successful.
 * - insertLast(): Adds an item at the rear. Returns true if successful.
 * - deleteFront(): Deletes an item from the front. Returns true if successful.
 * - deleteLast(): Deletes an item from the rear. Returns true if successful.
 * - getFront(): Returns the front item, or -1 if empty.
 * - getRear(): Returns the last item, or -1 if empty.
 * - isEmpty(): Returns true if empty.
 * - isFull(): Returns true if full.
 * 
 * Constraints: 1 <= k <= 1000, 0 <= value <= 1000. Maximum 2000 calls.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Can we dynamically resize the deque when it gets full?
 *    (No, the prompt explicitly requires it to have a maximum size `k` and reject overflowing inserts).
 * 2. Are we allowed to use the standard library `java.util.Deque` or `LinkedList`?
 *    (No, the spirit of this design problem is to implement the underlying mechanism from scratch).
 * 3. Does Java's modulo operator handle negative numbers nicely?
 *    (No, in Java `-1 % 5` evaluates to `-1`, not `4`. We must account for this when moving the head pointer backwards).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Provide O(1) access, insertion, and deletion at both ends 
 *   of a bounded linear structure, continuously recycling discarded space.
 * 
 * --- Approach 1: ArrayList (Shifting memory) ---
 * 1. Intuition: Use a standard dynamic array or a built-in list.
 * 2. Try: `insertLast` is O(1) by appending. `insertFront` uses `list.add(0, val)`.
 * 3. Why it works: It physically mimics a double-ended queue.
 * 4. Why it's too costly: `list.add(0, val)` forces every subsequent element to shift right, 
 *    resulting in O(N) time complexity. Deleting from the front also takes O(N).
 * 5. Bottleneck: Memory shifting destroys the O(1) requirement for deque operations.
 * 
 * Time Complexity: insertLast: O(1). insertFront/deleteFront: O(k).
 * Space Complexity: O(k).
 * 
 * --- Approach 2: Double Pointers (Front and Rear) ---
 * 1. Intuition: Use a fixed-size array and track both ends with `front` and `rear` pointers.
 * 2. Try: When inserting front, decrement `front`. When inserting last, increment `rear`.
 * 3. Why it fails to be elegant: The notorious "Full vs Empty" ambiguity. If `front == rear`, 
 *    is the deque completely full or completely empty? You have to sacrifice one array slot 
 *    just to tell the difference, or maintain confusing boolean flags.
 * 4. Bottleneck: Code complexity and high risk of off-by-one errors under interview pressure.
 * 
 * --- Approach 3: Modulo Array with Head + Count (Optimal) ---
 * 1. Intuition: Instead of tracking both ends, just track the `head` and the exact `count` 
 *    of elements. The `rear` can always be mathematically derived!
 * 2. Try: 
 *    - `insertLast`: Place at `(head + count) % k`. Increment `count`.
 *    - `insertFront`: Move `head` backwards cyclically: `(head - 1 + k) % k`. Place item. Increment `count`.
 *    - `deleteFront`: Move `head` forwards cyclically: `(head + 1) % k`. Decrement `count`.
 *    - `deleteLast`: Just decrement `count`! The tail logically shrinks.
 * 3. Why it works: It completely eliminates the ambiguity of overlapping pointers. 
 *    `isFull` is simply `count == k` and `isEmpty` is `count == 0`.
 * 4. Which to write in an interview?
 *    Approach 3. It reduces state variables from 3 (`front`, `rear`, `size`) to 2 (`head`, `count`), 
 *    drastically simplifying the boundary logic.
 * 
 * Time Complexity: O(1) across all operations.
 * Space Complexity: O(k) — allocating exactly k slots.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Moving the head backwards from 0: If `head = 0` and we call `insertFront()`, 
 *   doing `0 - 1 = -1`. In Java, `-1 % k` is `-1`, which throws an `ArrayIndexOutOfBoundsException`.
 *   We fix this using `(head - 1 + k) % k`.
 * - Calling `deleteLast()` on an empty deque: Must return false immediately to prevent 
 *   `count` from becoming negative.
 * - Retrieving from an empty deque: The constraints allow `value` up to 1000, meaning 
 *   `-1` safely represents an error/empty state without overlapping with valid data.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Track Length, Calculate Tail." 
 *   In a circular structure, if you know where the snake's head is, and you know 
 *   how long the snake is, you immediately know where the snake's tail is. 
 *   `tail_index = (head + count - 1) % capacity`.
 * 
 * ASCII DRY RUN (k = 3)
 * Initial: 
 * data: [_, _, _], head = 0, count = 0
 * 
 * 1. insertLast(1)
 *    index = (0 + 0) % 3 = 0.
 *    data: [1, _, _], head = 0, count = 1
 * 
 * 2. insertLast(2)
 *    index = (0 + 1) % 3 = 1.
 *    data: [1, 2, _], head = 0, count = 2
 * 
 * 3. insertFront(3)
 *    head = (0 - 1 + 3) % 3 = 2.
 *    data: [1, 2, 3], head = 2, count = 3
 * 
 * 4. getRear()
 *    tail_index = (2 + 3 - 1) % 3 = 4 % 3 = 1.
 *    data[1] is 2! (Correct, the items from front to rear are 3, 1, 2).
 * 
 * 5. deleteLast()
 *    count = count - 1 = 2.
 *    The array physically holds [1, 2, 3], but logically it only cares about head=2, count=2.
 *    The next time we insertLast, it will overwrite index 1!
 * 
 * Pitfalls:
 * - Forgetting to validate `isEmpty()` inside `getFront()` and `getRear()`. Returning 
 *   stale data (like returning `2` after the deque was emptied) is a fatal bug.
 * - Trying to explicitly clear array elements (e.g. `data[tail] = 0`). It's unnecessary 
 *   work since tracking `count` logically masks deleted items, and they will be naturally 
 *   overwritten on the next cycle.
 * 
 * Pattern Recognition: "Two-ended continuous operations" -> Circular Array with Head + Count.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you make this deque dynamically resizeable?
 * A1: When `count == k`, we allocate a new array of size `k * 2`. We cannot just use 
 *     `System.arraycopy` because the data might be wrapped around (e.g. elements at 
 *     indices 4, 0, 1). We would have to manually iterate `i` from `0` to `count - 1`, 
 *     reading from `(head + i) % k` in the old array, and writing to index `i` in the 
 *     new array. Finally, we reset `head = 0`.
 * 
 * Q2: How does a Deque compare to a Stack or Queue?
 * A2: A Deque is a superset. You can strictly implement a Stack by only using 
 *     `insertFront` and `deleteFront`. You can strictly implement a Queue by using 
 *     `insertLast` and `deleteFront`. In Java, `ArrayDeque` is the recommended 
 *     replacement for the legacy `Stack` class.
 */

public class MyCircularDequeStudy {

    static class MyCircularDeque {
        // The fixed-size array to hold the deque's data
        private final int[] data;
        
        // Points to the index of the front element
        private int head;
        
        // Tracks exactly how many elements are currently active
        private int count;
        
        // The maximum capacity of the deque
        private final int k;

        /**
         * Initializes the deque with a maximum size of k.
         */
        public MyCircularDeque(int k) {
            this.k = k;
            this.data = new int[k];
            this.head = 0;
            this.count = 0;
        }
        
        /**
         * Returns true if the deque is empty.
         * Time: O(1)
         */
        public boolean isEmpty() {
            return count == 0;
        }
        
        /**
         * Returns true if the deque is full.
         * Time: O(1)
         */
        public boolean isFull() {
            return count == k;
        }

        /**
         * Adds an item at the front of Deque.
         * Returns true if the operation is successful.
         * Time: O(1)
         */
        public boolean insertFront(int value) {
            if (isFull()) {
                return false;
            }
            
            // Move head backwards. 
            // We add 'k' before taking modulo to ensure we don't get a negative index in Java.
            head = (head - 1 + k) % k;
            data[head] = value;
            count++;
            
            return true;
        }

        /**
         * Adds an item at the rear of Deque.
         * Returns true if the operation is successful.
         * Time: O(1)
         */
        public boolean insertLast(int value) {
            if (isFull()) {
                return false;
            }
            
            // The next available spot at the rear is exactly 'count' steps away from the head.
            int tail = (head + count) % k;
            data[tail] = value;
            count++;
            
            return true;
        }

        /**
         * Deletes an item from the front of Deque.
         * Returns true if the operation is successful.
         * Time: O(1)
         */
        public boolean deleteFront() {
            if (isEmpty()) {
                return false;
            }
            
            // Moving head forward logically deletes the current front item.
            head = (head + 1) % k;
            count--;
            
            return true;
        }

        /**
         * Deletes an item from the rear of Deque.
         * Returns true if the operation is successful.
         * Time: O(1)
         */
        public boolean deleteLast() {
            if (isEmpty()) {
                return false;
            }
            
            // We don't need to move the head pointer or explicitly erase array data.
            // Shrinking the count organically removes the tail from logical existence.
            count--;
            
            return true;
        }

        /**
         * Returns the front item from the Deque. Returns -1 if empty.
         * Time: O(1)
         */
        public int getFront() {
            if (isEmpty()) {
                return -1;
            }
            return data[head];
        }

        /**
         * Returns the last item from Deque. Returns -1 if empty.
         * Time: O(1)
         */
        public int getRear() {
            if (isEmpty()) {
                return -1;
            }
            
            // The rear item is (count - 1) steps away from the head.
            int tail = (head + count - 1) % k;
            return data[tail];
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        // Run Example 1 from the prompt
        MyCircularDeque myCircularDeque = new MyCircularDeque(3);
        
        System.out.println("insertLast(1): " + myCircularDeque.insertLast(1));   // Expected: true
        System.out.println("insertLast(2): " + myCircularDeque.insertLast(2));   // Expected: true
        System.out.println("insertFront(3): " + myCircularDeque.insertFront(3)); // Expected: true
        
        // Deque is physically [1, 2, 3], but logically starts at head=2.
        // Elements from front to rear: 3 -> 1 -> 2
        
        System.out.println("insertFront(4): " + myCircularDeque.insertFront(4)); // Expected: false (full)
        System.out.println("getRear(): " + myCircularDeque.getRear());           // Expected: 2
        System.out.println("isFull(): " + myCircularDeque.isFull());             // Expected: true
        
        // Delete the last element (which is 2)
        System.out.println("deleteLast(): " + myCircularDeque.deleteLast());     // Expected: true
        
        // Deque elements from front to rear are now: 3 -> 1
        
        System.out.println("insertFront(4): " + myCircularDeque.insertFront(4)); // Expected: true
        
        // Deque elements from front to rear are now: 4 -> 3 -> 1
        
        System.out.println("getFront(): " + myCircularDeque.getFront());         // Expected: 4
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Fixed Array + Modulo Arithmetic (`(head - 1 + k) % k`).
 * Key Observation: Modulo arithmetic elegantly connects the boundaries of an array, 
 * but Java's negative modulo behavior requires `+ k` protection. By defining the 
 * deque using strictly a `head` index and a `count`, you bypass the confusing 
 * "Is it full or empty?" edge cases associated with dual-pointer (`head` & `tail`) designs.
 * What to Memorize: 
 * - Move left circularly: `index = (index - 1 + k) % k`
 * - Move right circularly: `index = (index + 1) % k`
 * Most Common Trap: Trying to explicitly clear old data (e.g. `data[tail] = -1`). 
 * Let the `count` variable act as your strict viewport; garbage data outside the 
 * viewport is mathematically invisible and overwritten seamlessly.
 * Mental Trigger: "Circular buffer / Both ends" -> "Modulo Array with Head + Count".
 * ============================================================================
 */









































class MyCircularDeque {

    // Stores all deque values in a fixed-size circular array.
    private final int[] elements;

    // Index of the first logical element.
    // When the deque is empty, this index does not identify a valid value.
    private int front;

    // Number of elements currently stored, from 0 through capacity.
    private int size;

    public MyCircularDeque(int capacity) {
        elements = new int[capacity];
        front = 0;
        size = 0;
    }

    public boolean insertFront(int value) {
        // Never overwrite an existing element when the deque is full.
        if (isFull()) {
            return false;
        }

        /*
         * The new value must occupy the position immediately BEFORE front.
         *
         * Example with capacity = 5:
         *   front = 2  -> new front should be 1
         *   front = 0  -> new front should be 4 (wrap around)
         *
         * We add capacity before modulo because Java's % operator can
         * return a negative remainder for negative operands.
         *
         * This moves the pointer, not the existing elements.
         */
        front = (front - 1 + elements.length) % elements.length;

        // Write the new first element at the updated front position.
        elements[front] = value;

        // One additional logical position is now occupied.
        size++;
        return true;
    }

    public boolean insertLast(int value) {
        if (isFull()) {
            return false;
        }

        /*
         * Occupied positions begin at front and span size elements.
         *
         * Therefore, the next free logical position is front + size.
         * Modulo wraps that position back to index 0 when necessary.
         *
         * Example:
         *   capacity = 5, front = 3, size = 2
         *   occupied positions = 3, 4
         *   next position = (3 + 2) % 5 = 0
         */
        int insertionIndex =
                (front + size) % elements.length;

        elements[insertionIndex] = value;
        size++;
        return true;
    }

    public boolean deleteFront() {
        if (isEmpty()) {
            return false;
        }

        /*
         * The current front element is logically removed.
         *
         * Instead of clearing the array slot or shifting elements,
         * advance front to the next occupied position.
         *
         * Modulo handles the wrap-around from the last index to index 0.
         */
        front = (front + 1) % elements.length;

        // One fewer position belongs to the deque.
        size--;
        return true;
    }

    public boolean deleteLast() {
        if (isEmpty()) {
            return false;
        }

        /*
         * The rear index is (front + size - 1) % capacity.
         *
         * We do not need to calculate it or move any pointer explicitly.
         * Decreasing size excludes the old rear from the logical deque.
         *
         * front remains unchanged because the first element was not removed.
         */
        size--;
        return true;
    }

    public int getFront() {
        // front identifies the first element whenever size > 0.
        return isEmpty() ? -1 : elements[front];
    }

    public int getRear() {
        if (isEmpty()) {
            return -1;
        }

        /*
         * Derive the last occupied position:
         *
         * front + size - 1
         *   - front: index of the first element
         *   - size - 1: number of steps from the first to the last
         *   - modulo: wrap around the circular array if needed
         */
        int rearIndex =
                (front + size - 1) % elements.length;

        return elements[rearIndex];
    }

    public boolean isEmpty() {
        // No logical elements are stored.
        return size == 0;
    }

    public boolean isFull() {
        // Every array position belongs to the deque.
        return size == elements.length;
    }
}

/*
 * COMPLEXITY ANALYSIS
 *
 * Let k = elements.length, the fixed capacity.
 *
 * Constructor:
 *   Time:  O(k), because Java initializes the int array to zero.
 *   Space: O(k), for the preallocated array.
 *
 * Every public operation after construction:
 *   Time: O(1), because it performs a constant number of calculations,
 *         reads, writes, and/or comparisons.
 *
 * Auxiliary space per operation:
 *   O(1), because no additional structure grows with the input.
 *
 * Total storage:
 *   O(k), for the array and a constant number of integer fields.
 *
 * IMPORTANT:
 *   We intentionally do not clear deleted array positions. The logical
 *   contents are determined by front and size, not by whether an array
 *   cell contains an old value.
 *
 *   We must update size consistently after every successful insertion
 *   and deletion. Failed operations leave all state unchanged.
 */



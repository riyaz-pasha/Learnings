/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design an implementation of a Circular Queue (Ring Buffer).
 * A circular queue reuses the spaces freed by dequeued elements by wrapping 
 * around to the beginning of the array.
 * 
 * Implement the MyCircularQueue class:
 * - MyCircularQueue(k): Initializes the object with size k.
 * - Front(): Gets the front item, or -1 if empty.
 * - Rear(): Gets the last item, or -1 if empty.
 * - enQueue(value): Inserts an element. Returns true if successful.
 * - deQueue(): Deletes an element. Returns true if successful.
 * - isEmpty(): Returns true if empty.
 * - isFull(): Returns true if full.
 * 
 * Constraints: 1 <= k <= 1000, 0 <= value <= 1000. Do not use built-in queue libraries.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Can we dynamically resize the queue if it gets full?
 *    (No, the prompt explicitly defines it with a fixed capacity `k`. Returning `false` on full is required).
 * 2. Is this queue going to be accessed by multiple threads?
 *    (If yes, we would need to make it thread-safe using `ReentrantLock` or `synchronized` blocks. Standard algorithm problems assume single-threaded).
 * 3. Can `value` be negative?
 *    (Constraints say 0 <= value <= 1000. This is helpful because we can safely return -1 as an error code for `Front()` and `Rear()`).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Maintain a FIFO structure in a fixed-size memory footprint 
 *   where old, discarded space is continuously reclaimed. The binding constraint 
 *   is doing this strictly in O(1) time without allocating new memory objects.
 * 
 * --- Approach 1: The Naive Array (Drifting Pointers) ---
 * 1. Intuition: Use an array of size `k` and two pointers: `head` and `tail`. 
 *    Enqueue moves `tail` right. Dequeue moves `head` right.
 * 2. Why it fails: "Pointer Drift". If we enqueue 3 items, then dequeue 3 items, 
 *    both `head` and `tail` are at index 3. The array is technically empty, but 
 *    if we try to enqueue again, `tail` goes out of bounds. The space at indices 
 *    0, 1, and 2 is permanently wasted.
 * 3. Bottleneck: Reusability of memory.
 * 
 * Time Complexity: O(1)
 * Space Complexity: O(k) — but practically fails the circular requirement.
 * 
 * --- Approach 2: The Shifting Array ---
 * 1. Intuition: To prevent drifting, keep `head` fixed at index 0. 
 * 2. Try: Enqueue adds to `tail`. Dequeue removes from index 0, and then shifts 
 *    every remaining element one position to the left.
 * 3. Why it fails: Shifting elements takes O(N) time. If we have 1,000 items, 
 *    every single dequeue shifts 999 items. 
 * 4. Bottleneck: Array shifting destroys the O(1) requirement for queues.
 * 
 * Time Complexity: enQueue: O(1). deQueue: O(N).
 * Space Complexity: O(k).
 * 
 * --- Approach 3: Modulo Arithmetic (Ring Buffer / Optimal) ---
 * 1. Intuition: Instead of moving the data back to index 0, what if the pointers 
 *    just seamlessly jump back to index 0 when they hit the end?
 * 2. Try: Use the modulo operator `% k`. 
 *    If `k = 5` and `tail = 4`, the next position is `(4 + 1) % 5 = 0`.
 * 3. Why it works: Pointers run laps around the array infinitely. 
 *    We just need a way to distinguish between "full" and "empty". We can do 
 *    this elegantly by explicitly tracking a `count` variable.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It is the definitive industry-standard way to write a Ring Buffer, 
 *    used natively in OS kernels and networking stacks.
 * 
 * Time Complexity: O(1) for all operations.
 * Space Complexity: O(k) for the fixed array.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Queue of size 1 (k = 1): `head` and `tail` will constantly be the same index (0). 
 *   The `count` variable perfectly handles differentiating full vs empty here.
 * - Calling `deQueue` on an empty queue: Must check `isEmpty()` and return false 
 *   immediately so we don't accidentally decrement `count` to -1.
 * - Negative modulo behavior: In Java, `-1 % 5` is `-1`. Since our `tail` formula 
 *   adds numbers, we must ensure we don't create negative indices when trying to 
 *   find the `Rear()` element. `(head + count - 1) % k` works safely because 
 *   `count >= 1` when `Rear()` is valid.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Track Length, Calculate Tail." 
 *   Instead of managing two independent, wrap-around pointers (`head` and `tail`), 
 *   track the `head` pointer and the `count` of elements. The `tail` is always 
 *   mathematically predictable: `(head + count) % capacity`. This eliminates the 
 *   notorious "Is it full or empty?" ambiguity when head == tail.
 * 
 * ASCII DRY RUN (k = 3)
 * Initial: 
 * Array: [ _, _, _ ]   head: 0, count: 0
 * 
 * 1. enQueue(10): tail = (0 + 0) % 3 = 0
 * Array: [10, _, _ ]   head: 0, count: 1
 * 
 * 2. enQueue(20): tail = (0 + 1) % 3 = 1
 * Array: [10, 20, _]   head: 0, count: 2
 * 
 * 3. deQueue(): head moves
 * Array: [ _, 20, _]   head: (0 + 1) % 3 = 1, count: 1
 * 
 * 4. enQueue(30), enQueue(40): 
 * Array: [40, 20, 30]  head: 1, count: 3
 * Notice how 40 wrapped around to index 0! 
 * tail was (1 + 2) % 3 = 0.
 * 
 * Pitfalls:
 * - Tracking `head` and `tail` directly without a `count` variable. 
 *   If `head == tail`, is the queue completely full, or completely empty? 
 *   You have to either waste 1 array slot as a "buffer" to tell the difference, 
 *   or use complex boolean flags. Tracking `count` is drastically cleaner and 
 *   less error-prone in an interview.
 * 
 * Pattern Recognition: "Fixed-size continuous data stream" -> Modulo Array (Ring Buffer).
 * 
 * Interview Script snippet:
 * "To implement this efficiently, I'll use a fixed-size array and modulo arithmetic 
 * to wrap the pointers around. To avoid the classic ambiguity of whether `head == tail` 
 * means full or empty, I will only track the `head` index and the current `count` of 
 * elements. The enqueue position is always `(head + count) % capacity`. This makes 
 * `isFull` and `isEmpty` trivial O(1) checks against the count."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you make this thread-safe for a Producer-Consumer scenario?
 * A1: I would add a `ReentrantLock`. I would also use two `Condition` variables: 
 *     `notFull` and `notEmpty`. When a producer tries to enqueue on a full queue, 
 *     it calls `notFull.await()`. When a consumer dequeues, it calls `notFull.signal()`. 
 *     This is exactly how Java's `ArrayBlockingQueue` is implemented.
 * 
 * Q2: What if we wanted to support dynamic resizing?
 * A2: If `count == capacity`, we allocate a new array of size `capacity * 2`. 
 *     However, we cannot just `System.arraycopy` directly if the queue has wrapped 
 *     around. We must iterate from `i = 0` to `count`, calculating the old index 
 *     as `(head + i) % capacity`, and place it at index `i` in the new array. 
 *     Then reset `head = 0`.
 */

public class MyCircularQueueStudy {

    static class MyCircularQueue {
        // The fixed-size array to hold the data
        private final int[] data;
        
        // Points to the index of the first valid element
        private int head;
        
        // Tracks exactly how many elements are currently in the queue
        private int count;
        
        // The maximum number of elements the queue can hold
        private final int capacity;

        /**
         * Initializes the object with the size of the queue to be k.
         */
        public MyCircularQueue(int k) {
            capacity = k;
            data = new int[capacity];
            head = 0;
            count = 0;
        }
        
        /**
         * Checks whether the circular queue is empty or not.
         * Time: O(1)
         */
        public boolean isEmpty() {
            return count == 0;
        }
        
        /**
         * Checks whether the circular queue is full or not.
         * Time: O(1)
         */
        public boolean isFull() {
            return count == capacity;
        }

        /**
         * Inserts an element into the circular queue. 
         * Return true if the operation is successful.
         * Time: O(1)
         */
        public boolean enQueue(int value) {
            // Guard clause: Cannot enqueue if the buffer is full
            if (isFull()) {
                return false;
            }
            
            // Calculate where the new element should go.
            // If head is 2, and we have 1 element, the next slot is (2 + 1) = 3.
            // The modulo ensures it wraps around if it exceeds capacity.
            int tailIndex = (head + count) % capacity;
            
            data[tailIndex] = value;
            count++; // Successfully added one element
            
            return true;
        }

        /**
         * Deletes an element from the circular queue. 
         * Return true if the operation is successful.
         * Time: O(1)
         */
        public boolean deQueue() {
            // Guard clause: Cannot dequeue if the buffer is already empty
            if (isEmpty()) {
                return false;
            }
            
            // Advance the head pointer circularly
            head = (head + 1) % capacity;
            count--; // Successfully removed one element
            
            return true;
        }

        /**
         * Gets the front item from the queue. If the queue is empty, return -1.
         * Time: O(1)
         */
        public int Front() {
            if (isEmpty()) {
                return -1;
            }
            return data[head];
        }

        /**
         * Gets the last item from the queue. If the queue is empty, return -1.
         * Time: O(1)
         */
        public int Rear() {
            if (isEmpty()) {
                return -1;
            }
            
            // The last inserted element is at (head + count - 1).
            // We apply modulo to ensure we read the correct wrapped-around index.
            int tailIndex = (head + count - 1) % capacity;
            return data[tailIndex];
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        // Run Example 1 from the prompt
        MyCircularQueue myCircularQueue = new MyCircularQueue(3);
        
        System.out.println("enQueue(1): " + myCircularQueue.enQueue(1)); // Expected: true
        System.out.println("enQueue(2): " + myCircularQueue.enQueue(2)); // Expected: true
        System.out.println("enQueue(3): " + myCircularQueue.enQueue(3)); // Expected: true
        
        // Queue is now physically [1, 2, 3]. Head = 0, Count = 3.
        
        System.out.println("enQueue(4) on full: " + myCircularQueue.enQueue(4)); // Expected: false
        
        System.out.println("Rear: " + myCircularQueue.Rear());     // Expected: 3
        System.out.println("isFull: " + myCircularQueue.isFull());   // Expected: true
        
        // Dequeue removes '1'. Head shifts to index 1. Count becomes 2.
        System.out.println("deQueue: " + myCircularQueue.deQueue());  // Expected: true
        
        // Enqueue '4'. Tail index becomes (1 + 2) % 3 = 0.
        // Queue is physically [4, 2, 3]. Head = 1, Count = 3.
        System.out.println("enQueue(4) wrapping around: " + myCircularQueue.enQueue(4)); // Expected: true
        
        System.out.println("Rear after wrap: " + myCircularQueue.Rear());     // Expected: 4
        
        // Verify Front is correct after wrap around
        System.out.println("Front after wrap: " + myCircularQueue.Front());   // Expected: 2
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Fixed Array + Modulo Arithmetic (`index % capacity`).
 * Key Observation: A ring buffer's hardest problem is differentiating between full 
 * and empty when pointers collide. By abandoning a dedicated `tail` pointer and 
 * instead storing `head` and `count`, ambiguity is destroyed and bounds checking 
 * becomes trivially mathematical.
 * What to Memorize: `tail_index = (head + count) % capacity` and 
 * `last_element_index = (head + count - 1) % capacity`.
 * Most Common Trap: Trying to explicitly check for array bounds (e.g., `if (tail == k)`). 
 * Let the `%` operator do the wrapping for you instantly.
 * Mental Trigger: "Circular buffer / Reusable queue space" -> "Modulo indexing with Head + Count".
 * ============================================================================
 */










































class MyCircularQueue {

    /*
     * OPTIMAL APPROACH: CIRCULAR ARRAY
     *
     * Mental model:
     * - frontIndex identifies the first element in the logical queue.
     * - size tells us how many elements currently belong to the queue.
     * - The array is treated as a circle: after the last index comes index 0.
     *
     * Example: capacity = 5, frontIndex = 3, size = 3
     *
     * Physical array:
     * Index:       0    1    2    3    4
     *             [C]  [_]  [_]  [A]  [B]
     *              ^              ^
     *             Rear           Front
     *
     * Logical queue: A -> B -> C
     *
     * IMPORTANT INVARIANTS:
     * 1. 0 <= size <= capacity.
     * 2. If size > 0, elements[frontIndex] is the front element.
     * 3. If size > 0, the last existing element is at:
     *       (frontIndex + size - 1) % capacity
     * 4. The next insertion position, if the queue is not full, is:
     *       (frontIndex + size) % capacity
     *
     * Why are the last two formulas different?
     * - The first element is 0 steps from the front.
     * - Therefore, the last of 'size' elements is size - 1 steps away.
     * - The next insertion position is one step beyond that, or size steps away.
     *
     * TIME COMPLEXITY:
     * - Constructor: O(k) to allocate the array.
     * - enQueue, deQueue, Front, Rear, isEmpty, isFull: O(1) each.
     *
     * SPACE COMPLEXITY: O(k), for the fixed-size array.
     */

    private final int[] elements;
    private int frontIndex;
    private int size;

    public MyCircularQueue(int k) {
        elements = new int[k];

        // Initially the queue has no elements.
        // Index 0 will be the front position when the first element arrives.
        frontIndex = 0;
        size = 0;
    }

    public boolean enQueue(int value) {

        // Never overwrite an existing element.
        // If size equals capacity, every array position is occupied logically.
        if (isFull()) {
            return false;
        }

        /*
         * Find the NEXT INSERTION POSITION.
         *
         * The existing elements occupy size consecutive circular positions:
         * frontIndex, frontIndex + 1, ..., frontIndex + size - 1.
         *
         * So the next available position is size steps from the front.
         *
         * Example:
         * frontIndex = 3, size = 3, capacity = 5
         *
         * Existing indices: 3, 4, 0
         * Next insertion:   (3 + 3) % 5 = 1
         *
         * Modulo wraps the index around when it passes the array's end.
         */
        int insertionIndex = (frontIndex + size) % elements.length;

        // Store the new element at the next logical rear position.
        elements[insertionIndex] = value;

        // The new element now belongs to the queue.
        size++;

        return true;
    }

    public boolean deQueue() {

        // An empty queue has no element to remove.
        if (isEmpty()) {
            return false;
        }

        /*
         * Remove the front logically by advancing its index.
         *
         * We DO NOT shift the remaining elements.
         * The old front slot becomes available for reuse after the queue wraps.
         *
         * Example:
         * frontIndex = 4, capacity = 5
         * New frontIndex = (4 + 1) % 5 = 0
         */
        frontIndex = (frontIndex + 1) % elements.length;

        // Exactly one logical element has been removed.
        size--;

        return true;
    }

    public int Front() {

        // The problem requires -1 when the queue contains no elements.
        // Otherwise, the front element is directly available at frontIndex.
        return isEmpty() ? -1 : elements[frontIndex];
    }

    public int Rear() {

        // Do not calculate or access a rear element when the queue is empty.
        if (isEmpty()) {
            return -1;
        }

        /*
         * Find the LAST EXISTING ELEMENT.
         *
         * There are size elements, but the first element is already at
         * offset 0. Therefore, the last element is size - 1 steps from front.
         *
         * Example:
         * frontIndex = 3, size = 3, capacity = 5
         *
         * Rear index = (3 + 3 - 1) % 5
         *            = 0
         *
         * The logical elements occupy indices 3, 4, 0.
         * Therefore, index 0 contains the last element.
         */
        int lastElementIndex =
                (frontIndex + size - 1) % elements.length;

        return elements[lastElementIndex];
    }

    public boolean isEmpty() {

        // No logical elements exist when size is zero.
        return size == 0;
    }

    public boolean isFull() {

        // Every position is occupied logically when size reaches capacity.
        return size == elements.length;
    }
}




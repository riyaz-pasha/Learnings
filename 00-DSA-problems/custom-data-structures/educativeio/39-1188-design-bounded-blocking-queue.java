/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a thread-safe bounded blocking queue.
 * 
 * Implement the BoundedBlockingQueue class:
 * - BoundedBlockingQueue(int capacity): Initializes the queue with a maximum capacity.
 * - void enqueue(int element): Adds an element to the front of the queue. If the 
 *   queue is full, the calling thread blocks until it is no longer full.
 * - int dequeue(): Returns the element at the front of the queue and removes it. 
 *   If the queue is empty, the calling thread blocks until it is no longer empty.
 * - int size(): Returns the number of elements currently in the queue.
 * 
 * Your implementation will be tested with multiple threads concurrently calling 
 * enqueue and dequeue.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Are we allowed to use Java's built-in concurrent collections like `ArrayBlockingQueue`?
 *    (No, the purpose of this problem is to design the internal synchronization mechanism from scratch using locks or monitors).
 * 2. Does the queue need to guarantee fairness (i.e., the longest waiting thread gets served first)?
 *    (Standard LeetCode concurrency problems do not strictly require fairness unless specified, but it's a great follow-up discussion).
 * 3. Can multiple threads call `size()` concurrently with modifications?
 *    (Yes, so `size()` must also be thread-safe to prevent reading a stale or volatile state).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Protect a shared data structure from race conditions while 
 *   efficiently putting threads to sleep (blocking) and waking them up (signaling) 
 *   based on the queue's capacity.
 * 
 * --- Approach 1: synchronized, wait(), notifyAll() (The Monitor Pattern) ---
 * 1. Intuition: Use a standard `LinkedList` and make every method `synchronized`.
 * 2. Try: 
 *    - `enqueue`: `while (queue.size() == capacity) { wait(); } queue.add(val); notifyAll();`
 *    - `dequeue`: `while (queue.isEmpty()) { wait(); } val = queue.poll(); notifyAll();`
 * 3. Why it works: Java's intrinsic locks guarantee mutually exclusive access.
 * 4. Why it's suboptimal: `notifyAll()` causes the "Thundering Herd" problem. 
 *    When a consumer dequeues an item, it calls `notifyAll()`, which wakes up EVERY 
 *    waiting thread—including other consumers who just go right back to sleep. This wastes CPU cycles.
 * 
 * --- Approach 2: ReentrantLock + Two Conditions (Optimal & Industry Standard) ---
 * 1. Intuition: We need two separate "waiting rooms": one for Producers (waiting for space) 
 *    and one for Consumers (waiting for items).
 * 2. Try: Use a `ReentrantLock` with two `Condition` variables: `notFull` and `notEmpty`.
 * 3. Why it works: 
 *    - When `enqueue` finishes, it calls `notEmpty.signal()`. This *only* wakes up a waiting Consumer.
 *    - When `dequeue` finishes, it calls `notFull.signal()`. This *only* wakes up a waiting Producer.
 * 4. Which to write in an interview?
 *    Approach 2. This is exactly how Java's actual `java.util.concurrent.ArrayBlockingQueue` 
 *    is implemented under the hood. It demonstrates advanced concurrency knowledge.
 * 
 * Time Complexity: O(1) for all operations.
 * Space Complexity: O(capacity) to store the elements in the underlying queue.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Spurious Wakeups: A waiting thread can sometimes wake up without receiving a signal 
 *   (a known OS-level quirk). This is why we MUST check the condition in a `while` loop 
 *   (`while (queue.size() == capacity) await();`), NOT an `if` statement. If it wakes up spuriously, 
 *   the `while` loop forces it to re-check the condition and go back to sleep.
 * - Thread Interruption: If a thread is interrupted while blocked, `await()` natively 
 *   throws `InterruptedException`. We simply bubble this up in the method signature, 
 *   which is standard practice for blocking operations.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Separation of Waiting Rooms." 
 *   Using a single monitor (`wait/notify`) clumps all blocked threads together. 
 *   By generating multiple `Condition` objects from a single `Lock`, we create 
 *   distinct queues of sleeping threads, allowing us to surgically wake up exactly 
 *   the type of thread that can now proceed.
 * 
 * ASCII Architecture Diagram:
 * 
 *                    [ ReentrantLock ]
 *                           |
 *             -----------------------------
 *             |                           |
 *      [ Condition: notFull ]    [ Condition: notEmpty ]
 *      (Producers sleep here)    (Consumers sleep here)
 *             |                           |
 *         enqueue()                   dequeue()
 *      -> if full, await()         -> if empty, await()
 *      -> add item                 -> remove item
 *      -> signal notEmpty!         -> signal notFull!
 * 
 * Pitfalls:
 * - Forgetting the `finally` block: You must ALWAYS release a `ReentrantLock` in a 
 *   `finally` block. If `queue.offer()` throws a RuntimeException and `lock.unlock()` 
 *   is skipped, the lock is permanently held and the entire application deadlocks.
 * 
 * Pattern Recognition: "Thread-safe Producer/Consumer" -> ReentrantLock + Conditions.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you guarantee that the thread waiting the longest gets served first?
 * A1: We can pass `true` to the lock's constructor: `new ReentrantLock(true)`. 
 *     This creates a "Fair Lock", which grants access to the longest-waiting thread. 
 *     However, fairness significantly drops throughput (transactions per second) due to 
 *     OS thread scheduling overhead, so it defaults to `false`.
 * 
 * Q2: Can this be implemented without Locks using Semaphores?
 * A2: Yes. We would use two Semaphores: `Semaphore spaceAvailable = new Semaphore(capacity)` 
 *     and `Semaphore itemsAvailable = new Semaphore(0)`. `enqueue` would call 
 *     `spaceAvailable.acquire()` and `itemsAvailable.release()`. We would still need a 
 *     mutex (or `synchronized`) to protect the actual queue mutation.
 */

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedBlockingQueueStudy {

    static class BoundedBlockingQueue {
        
        // The underlying non-thread-safe data structure
        private final Queue<Integer> queue;
        private final int capacity;
        
        // The single lock that protects all access to the queue
        private final ReentrantLock lock;
        
        // Condition for Producer threads to wait when the queue is full
        private final Condition notFull;
        
        // Condition for Consumer threads to wait when the queue is empty
        private final Condition notEmpty;

        /**
         * Initializes the queue with a maximum capacity.
         */
        public BoundedBlockingQueue(int capacity) {
            this.capacity = capacity;
            this.queue = new LinkedList<>();
            
            this.lock = new ReentrantLock();
            this.notFull = lock.newCondition();
            this.notEmpty = lock.newCondition();
        }

        /**
         * Adds an element to the front of the queue.
         * Blocks if the queue is at capacity.
         */
        public void enqueue(int element) throws InterruptedException {
            // Acquire the lock before checking or modifying state
            lock.lock();
            try {
                // MUST be a while loop to protect against spurious wakeups
                while (queue.size() == capacity) {
                    // Put this producer thread to sleep in the 'notFull' waiting room.
                    // await() atomically releases the lock so other threads can proceed.
                    notFull.await(); 
                }
                
                // State is safe to modify
                queue.offer(element);
                
                // Wake up ONE sleeping consumer thread in the 'notEmpty' waiting room
                notEmpty.signal(); 
                
            } finally {
                // ALWAYS unlock in a finally block to prevent permanent deadlocks
                lock.unlock();
            }
        }

        /**
         * Returns the element at the front of the queue and removes it.
         * Blocks if the queue is empty.
         */
        public int dequeue() throws InterruptedException {
            lock.lock();
            try {
                // MUST be a while loop to protect against spurious wakeups
                while (queue.isEmpty()) {
                    // Put this consumer thread to sleep in the 'notEmpty' waiting room
                    notEmpty.await();
                }
                
                int element = queue.poll();
                
                // Wake up ONE sleeping producer thread in the 'notFull' waiting room
                notFull.signal();
                
                return element;
                
            } finally {
                lock.unlock();
            }
        }

        /**
         * Returns the number of elements currently in the queue.
         */
        public int size() {
            // size() must also acquire the lock! 
            // If we don't lock, we might read a partially updated state.
            lock.lock();
            try {
                return queue.size();
            } finally {
                lock.unlock();
            }
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing (Concurrent Simulation)
    // ============================================================================
    public static void main(String[] args) throws InterruptedException {
        // Initialize a queue with capacity of 2
        BoundedBlockingQueue bq = new BoundedBlockingQueue(2);
        
        // Create a Producer Thread
        Thread producer = new Thread(() -> {
            try {
                System.out.println("Producer: Enqueueing 1");
                bq.enqueue(1);
                System.out.println("Producer: Enqueueing 2");
                bq.enqueue(2);
                
                System.out.println("Producer: Enqueueing 3 (Will Block...)");
                // This will block because capacity is 2
                bq.enqueue(3);
                System.out.println("Producer: 3 Enqueued successfully!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Create a Consumer Thread
        Thread consumer = new Thread(() -> {
            try {
                // Sleep for 1 second to guarantee the producer hits the capacity block
                Thread.sleep(1000);
                
                System.out.println("Consumer: Dequeueing -> " + bq.dequeue());
                System.out.println("Consumer: Dequeueing -> " + bq.dequeue());
                System.out.println("Consumer: Dequeueing -> " + bq.dequeue());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
        
        System.out.println("Final Queue Size: " + bq.size()); // Expected: 0
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: ReentrantLock + Condition Variables.
 * Key Observation: A bounded queue has two distinct blocking states: Full (blocks 
 * producers) and Empty (blocks consumers). Using a single monitor (`notifyAll()`) 
 * forces the system to wake up every sleeping thread indiscriminately. By extracting 
 * multiple `Condition` objects from a `ReentrantLock`, we can surgically wake up 
 * exactly the right type of thread using `.signal()`, vastly improving throughput.
 * What to Memorize: `while (condition) conditionVariable.await();`
 * Most Common Trap: Using `if (queue.size() == capacity)` instead of `while`. 
 * Operating systems occasionally wake up waiting threads for no reason (spurious wakeups). 
 * If you don't loop back to check the condition, the thread will proceed to mutate 
 * the queue beyond its capacity.
 * Mental Trigger: "Thread-safe wait/notify" -> "ReentrantLock + Conditions".
 * ============================================================================
 */






































import java.util.ArrayDeque;
import java.util.Deque;

class BoundedBlockingQueue {
    private final int capacity;
    private final Deque<Integer> queue;

    public BoundedBlockingQueue(int capacity) {
        // Store the maximum number of elements the queue may contain.
        this.capacity = capacity;
        this.queue = new ArrayDeque<>(capacity);
    }

    public synchronized void enqueue(int element)
            throws InterruptedException {

        // WHY while?
        // The queue might still be full after this thread wakes up:
        // another producer may have inserted first, or this may be
        // a spurious wakeup. Always recheck the actual condition.
        while (queue.size() == capacity) {
            // wait() releases this object's monitor while blocking.
            // That allows a consumer to enter dequeue(), remove an
            // element, and notify waiting producers.
            wait();
        }

        // We own the monitor and know there is space.
        // offerLast() inserts at the tail, preserving FIFO order.
        queue.offerLast(element);

        // A consumer may be waiting because the queue was empty.
        // Wake waiting threads so they can recheck their conditions.
        // notifyAll() is used because both producers and consumers
        // wait on this same monitor; notify() could wake the wrong kind.
        notifyAll();
    }

    public synchronized int dequeue() throws InterruptedException {
        // WHY while?
        // The queue may be empty again when this thread reacquires
        // the monitor, even if it was non-empty when notified.
        while (queue.isEmpty()) {
            // Release the monitor and wait for a producer to enqueue.
            wait();
        }

        // Remove the oldest element from the head.
        int element = queue.removeFirst();

        // Removing an element creates capacity.
        // Wake waiting producers so they can recheck whether space exists.
        notifyAll();

        return element;
    }

    public synchronized int size() {
        // The monitor guarantees a consistent snapshot of the size.
        return queue.size();
    }
}



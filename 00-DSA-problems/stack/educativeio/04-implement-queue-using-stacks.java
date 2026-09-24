/**
 * ============================================================================
 * IMPLEMENT QUEUE USING STACKS - INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Design a queue using only two standard stacks. You must implement 
 * push (enqueue), pop (dequeue), peek (front), and empty.
 * 
 * Simple Explanation for Interviewer:
 * "A Queue is First-In-First-Out (FIFO) like a line at a store, but a Stack is 
 * Last-In-First-Out (LIFO) like a stack of plates. If I push elements into one 
 * stack, they come out in reverse order. But if I pop those elements and push 
 * them into a SECOND stack, their order reverses again. Two reversals restore 
 * the original FIFO order!"
 * 
 * Core Idea & Intuition:
 * Stack 1 is for receiving new elements. Stack 2 is for serving elements. 
 * When Stack 2 is empty, we "pour" everything from Stack 1 into Stack 2. 
 * This flip puts the oldest element at the top of Stack 2, ready for O(1) removal.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFICATION
 * ----------------------------------------------------------------------------
 * 1. Q: "Can I use Java's Deque interface, or do I need to build a Stack from scratch?"
 *    Why: Ensures you use idiomatic Java (`ArrayDeque`) rather than the legacy `Stack`.
 * 
 * 2. Q: "What should pop() or peek() do if the queue is empty?"
 *    Why: Defines error handling. (Constraint says they will only be called 
 *    on non-empty stacks, so we can skip throwing exceptions, but mentioning 
 *    it shows defensive programming).
 * 
 * 3. Q: "Is the focus on optimizing the `push` operation or the `pop`/`peek` operations?"
 *    Why: There are two valid ways to solve this. One makes `push` O(N) and 
 *    `pop` O(1). The other makes `push` O(1) and `pop` Amortized O(1). The 
 *    latter is far superior in practice.
 * 
 * 4. Q: "Are there memory constraints or concurrent access requirements?"
 *    Why: Multi-threading might require synchronization, which alters the design.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Scenario: push(1), push(2), peek(), pop(), empty()
 * 
 * Step 1: push(1), push(2) -> always go to Stack 1 (Inbox)
 * Stack 1 (Top -> Bottom): [2, 1]
 * Stack 2 (Top -> Bottom): []
 * 
 * Step 2: peek() -> Stack 2 is empty, so "pour" Stack 1 into Stack 2.
 * Stack 1 pops 2, pushes to Stack 2.
 * Stack 1 pops 1, pushes to Stack 2.
 * Stack 1: []
 * Stack 2: [1, 2]  <- Look! 1 is now at the top, perfectly FIFO.
 * returns 1.
 * 
 * Step 3: pop() -> Stack 2 is NOT empty, just pop it.
 * Stack 2: [2]
 * returns 1.
 * 
 * Step 4: empty() -> Both stacks are NOT empty (Stack 2 has '2').
 * returns FALSE.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Acknowledge the core conflict (FIFO vs LIFO).
 * 2. Clarify: Confirm it's okay to assume non-empty pops as per constraints.
 * 3. Approach 1 (Push-Heavy): Explain how you could move everything back and 
 *    forth on every push. Acknowledge this is O(N) for every single insertion.
 * 4. Approach 2 (Optimal Amortized): Explain the "Inbox/Outbox" concept. 
 *    Data enters the inbox (Stack 1) and leaves the outbox (Stack 2). We only 
 *    move data when the outbox is empty.
 * 5. Code: Implement the Optimal approach.
 * 6. Explain Amortized Time: This is the crucial part. Explain *why* `pop` 
 *    is O(1) on average even though it has a `while` loop.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Empty Queue: `empty()` must check BOTH stacks, not just one.
 * - Continuous Pushes & Pops: `push(1), pop(), push(2), pop()`. The logic 
 *   must handle moving data, returning it, and accepting new data smoothly.
 * 
 * Common Mistakes:
 * - Pouring when Stack 2 is NOT empty. If Stack 2 has elements, pouring Stack 1 
 *   on top of it destroys the FIFO order. You MUST wait until Stack 2 is fully 
 *   empty before transferring elements.
 * - Using `java.util.Stack`. It extends `Vector` and adds synchronization locks, 
 *   making it heavily unoptimized. `Deque<Integer> stack = new ArrayDeque<>();` 
 *   is the modern standard.
 * 
 * ============================================================================
 * 6. DETAILED TRACING (Optimal Amortized Solution)
 * ----------------------------------------------------------------------------
 * Operations: push(10), push(20), pop(), push(30), pop()
 * 
 * Init: s1 = [], s2 = []
 * push(10): s1 = [10], s2 = []
 * push(20): s1 = [20, 10], s2 = [] (20 is top)
 * pop(): 
 *   - peek() is called. s2 is empty.
 *   - while(!s1.isEmpty()) -> pop 20, push to s2. pop 10, push to s2.
 *   - s1 = [], s2 = [10, 20] (10 is top)
 *   - return s2.pop() -> returns 10. s2 is now [20].
 * push(30): s1 = [30], s2 = [20]
 * pop():
 *   - peek() is called. s2 is NOT empty. Loop skipped.
 *   - return s2.pop() -> returns 20. s2 is now [].
 * 
 * Final state perfectly mirrored a Queue's behavior.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class ImplementQueueUsingStacks {

    public static void main(String[] args) {
        System.out.println("--- Testing Optimal Queue ---");
        MyQueue optimalQueue = new MyQueue();
        optimalQueue.push(1);
        optimalQueue.push(2);
        System.out.println("Peek: " + optimalQueue.peek());   // Expected 1
        System.out.println("Pop: " + optimalQueue.pop());     // Expected 1
        System.out.println("Empty: " + optimalQueue.empty()); // Expected false
        optimalQueue.push(3);
        System.out.println("Pop: " + optimalQueue.pop());     // Expected 2
        System.out.println("Pop: " + optimalQueue.pop());     // Expected 3
        System.out.println("Empty: " + optimalQueue.empty()); // Expected true

        System.out.println("\n--- Testing Push-Heavy Queue ---");
        MyQueuePushHeavy pushHeavyQueue = new MyQueuePushHeavy();
        pushHeavyQueue.push(1);
        pushHeavyQueue.push(2);
        System.out.println("Pop: " + pushHeavyQueue.pop());   // Expected 1
    }

    /**
     * SOLUTION 1: OPTIMAL APPROACH (Amortized O(1) Pop/Peek)
     * ------------------------------------------------------------------------
     * Idea: Maintain two stacks, `s1` (Inbox) and `s2` (Outbox). 
     * All new elements are pushed directly to `s1` in O(1) time.
     * When an element needs to be read or removed, we serve it from `s2`. 
     * If `s2` is empty, we pop everything from `s1` and push it to `s2`.
     * 
     * Why it works: Moving elements from one stack to another reverses their 
     * order. Since they were initially in LIFO order, the reversal puts them 
     * in FIFO order inside `s2`.
     * 
     * Time Complexity: 
     * - Push: O(1)
     * - Pop/Peek: Amortized O(1). In the worst case (when s2 is empty), we 
     *   move N elements, making it O(N). However, each element is moved exactly 
     *   once from s1 to s2, and popped exactly once. Across N operations, the 
     *   average cost is purely O(1).
     * Space Complexity: O(N) to store the elements across both stacks.
     */
    static class MyQueue {
        // We use Deque as the idiomatic, fast Java Stack implementation.
        private final Deque<Integer> s1; // Inbox for new elements
        private final Deque<Integer> s2; // Outbox for removing elements

        public MyQueue() {
            s1 = new ArrayDeque<>();
            s2 = new ArrayDeque<>();
        }

        public void push(int x) {
            s1.push(x);
        }

        public int pop() {
            // Calling peek() ensures s2 has the elements ready to pop.
            // This prevents duplicate code.
            peek(); 
            return s2.pop();
        }

        public int peek() {
            // Only pour from s1 to s2 if s2 is empty.
            // Pouring while s2 has elements would bury the oldest elements 
            // under newer ones, breaking the FIFO order.
            if (s2.isEmpty()) {
                while (!s1.isEmpty()) {
                    s2.push(s1.pop());
                }
            }
            return s2.peek();
        }

        public boolean empty() {
            // The queue is only empty if BOTH the inbox and outbox are empty.
            return s1.isEmpty() && s2.isEmpty();
        }
    }

    /**
     * SOLUTION 2: PUSH-HEAVY APPROACH (O(N) Push, O(1) Pop)
     * ------------------------------------------------------------------------
     * Idea: Ensure Stack 1 always maintains the exact FIFO order of a queue. 
     * To do this, every time a new element arrives, we move everything out of 
     * Stack 1 to Stack 2, put the new element at the bottom of Stack 1, and 
     * move everything back.
     * 
     * Time Complexity: Push is strictly O(N) because we move all elements twice. 
     * Pop/Peek is strictly O(1).
     * Space Complexity: O(N).
     * 
     * When to present: Mention this as your "Brute Force" or initial thought 
     * to show you can formulate a basic logic before moving to the Optimal.
     */
    static class MyQueuePushHeavy {
        private final Deque<Integer> s1;
        private final Deque<Integer> s2;

        public MyQueuePushHeavy() {
            s1 = new ArrayDeque<>();
            s2 = new ArrayDeque<>();
        }

        public void push(int x) {
            // Move everything out of s1 to expose the bottom
            while (!s1.isEmpty()) {
                s2.push(s1.pop());
            }
            
            // Put the new element at the absolute bottom
            s1.push(x);
            
            // Pour everything back on top of it
            while (!s2.isEmpty()) {
                s1.push(s2.pop());
            }
        }

        public int pop() {
            return s1.pop();
        }

        public int peek() {
            return s1.peek();
        }

        public boolean empty() {
            return s1.isEmpty();
        }
    }

    /**
     * ========================================================================
     * 7. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time (Push)| Time (Pop)   | Space | Interview Rec.
     * ------------------------------------------------------------------------
     * Push-Heavy     | O(N)       | O(1)         | O(N)  | Mention only.
     * Amortized Opt. | O(1)       | O(1) Amort.  | O(N)  | ⭐ PRESENT THIS.
     * 
     * Why the Amortized is better: In real-world systems, we usually enqueue 
     * things much faster/more often than we process them. Having O(1) enqueue 
     * prevents bottlenecks on ingestion. 
     * 
     * ========================================================================
     * 8. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What does Amortized O(1) really mean?"
     * A1: It means that while a single operation (like `pop()`) might occasionally 
     *     take O(N) time (when moving 100 elements from s1 to s2), the cost is 
     *     spread out over the next 100 `pop()` calls which will strictly take O(1). 
     *     So mathematically, (O(N) + 99 * O(1)) / 100 calls = O(1) average time.
     * 
     * Q2: "Can you make BOTH Push and Pop strictly O(1) using only two stacks?"
     * A2: No. Because a queue and stack process data in fundamentally opposite 
     *     directions, reversing the direction takes at least O(N) work at some 
     *     point in the lifecycle of the data. 
     * 
     * Q3: "Is this class Thread-Safe?"
     * A3: No. If multiple threads call `push()` and `pop()` simultaneously, `s1` 
     *     and `s2` can become corrupted. To fix this, we would use `synchronized` 
     *     blocks or switch to a `ConcurrentLinkedQueue` if stacks weren't forced.
     * 
     * ========================================================================
     * 9. FINAL TAKEAWAYS
     * ========================================================================
     * - The "Inbox/Outbox" pattern is an incredibly common systems design concept.
     * - Reversing a collection twice restores its original order. (Stack -> Stack = Queue).
     * - Always ensure your "Outbox" is completely empty before refilling it from 
     *   the "Inbox" to prevent newer items from burying older ones.
     * - Know how to confidently explain "Amortized Time Complexity" as interviewers 
     *   love to probe on it.
     */
}

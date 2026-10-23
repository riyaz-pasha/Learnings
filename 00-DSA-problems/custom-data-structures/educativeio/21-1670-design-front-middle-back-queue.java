/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a queue that supports push and pop operations in the front, middle, 
 * and back.
 * 
 * Implement the FrontMiddleBack class:
 * - FrontMiddleBack() Initializes the queue.
 * - pushFront(val): Adds val to the front of the queue.
 * - pushMiddle(val): Adds val to the middle of the queue.
 * - pushBack(val): Adds val to the back of the queue.
 * - popFront(): Removes and returns the front element, or -1 if empty.
 * - popMiddle(): Removes and returns the middle element, or -1 if empty.
 * - popBack(): Removes and returns the back element, or -1 if empty.
 * 
 * Notice that when there are two middle choices, the operation is performed 
 * on the frontmost middle choice.
 * 
 * Constraints: 1 <= val <= 10^9, At most 1000 calls.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. What exactly constitutes the "middle" when inserting vs. removing?
 *    (For even size arrays, pushMiddle inserts exactly in the center. popMiddle 
 *    removes the element just to the left of the center).
 * 2. Can the queue hold primitive `int` values, or are there nullable constraints?
 *    (Values are positive 1 to 10^9, allowing us to safely return -1 as an error code).
 * 3. Does the structure need to scale to millions of operations?
 *    (Constraints say 1000 calls, meaning O(N) array shifts would pass, but 
 *    design questions usually expect optimal O(1) performance per operation).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Provide O(1) insertion and deletion at three specific 
 *   locations: the absolute front, the absolute back, and the exact middle. 
 *   The binding constraint is that the "middle" shifts continuously as elements 
 *   are added or removed from the ends.
 * 
 * --- Approach 1: Single List (Brute Force / Naive) ---
 * 1. Intuition: Just use an `ArrayList` or `LinkedList`.
 * 2. Try: 
 *    `pushFront` -> `list.add(0, val)`
 *    `pushMiddle` -> `list.add(size / 2, val)`
 *    `popFront` -> `list.remove(0)`
 * 3. Why it works: Java lists seamlessly handle internal shifting and indexing.
 * 4. Why it's suboptimal: Shifting elements in an `ArrayList` takes O(N) time. 
 *    Navigating to the middle of a `LinkedList` also takes O(N) time.
 * 5. Bottleneck: The memory shifting or node traversal overhead.
 * 
 * Time Complexity: O(N) per middle/front operation.
 * Space Complexity: O(N).
 * 
 * --- Approach 2: Two Deques (Optimal) ---
 * 1. Intuition: A standard queue or Deque gives O(1) ends. If we cut the queue 
 *    in half and store it in TWO Deques, the "middle" of the combined queue 
 *    becomes the inner ends of the two halves!
 * 2. Try: Maintain `left` Deque and `right` Deque.
 *    - `left` represents the front half, `right` represents the back half.
 *    - Enforce a strict balancing rule: `left.size()` is always equal to 
 *      `right.size()`, OR `right` has exactly 1 more element than `left`.
 * 3. Why it works: 
 *    - The absolute front is `left.peekFirst()`.
 *    - The absolute back is `right.peekLast()`.
 *    - The middle is either `left.peekLast()` or `right.peekFirst()` depending on parity!
 * 4. Which to write in a 45-min interview?
 *    Approach 2. It demonstrates the ability to compose basic data structures 
 *    into complex, invariant-driven systems. It achieves strict O(1) time.
 * 
 * Time Complexity: O(1) for all operations (additions, removals, balancing).
 * Space Complexity: O(N) to store the elements across the two deques.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Total size is 1: `left` is empty, `right` has 1 element. `popFront()` must 
 *   check if `left` is empty and intelligently pull from `right.pollFirst()`.
 * - Pushing to middle on empty: Both deques are size 0. Adding to `right` maintains 
 *   the invariant that `right` handles the odd excess element.
 * - Popping from empty: Must immediately return -1 without triggering 
 *   `NullPointerException` when unboxing.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "The middle is just two touching ends." 
 *   By severing the queue down the exact center, we expose the middle elements 
 *   as standard O(1) head/tail operations on the resulting halves. 
 *   A dedicated `balance()` method automatically repairs the cut after every operation.
 * 
 * ASCII DRY RUN
 * Push [1, 2, 3, 4]
 * 1. pushBack(1):  left=[], right=[1]
 * 2. pushBack(2):  left=[1], right=[2]  (Balanced!)
 * 3. pushMiddle(3):
 *    Sizes are equal (1 == 1). Rule: push to right's front.
 *    left=[1], right=[3, 2]
 * 4. pushMiddle(4):
 *    left is smaller (1 < 2). Rule: push to left's back.
 *    left=[1, 4], right=[3, 2]
 * 
 * Action: popMiddle()
 *    Sizes are equal (2 == 2). Tie-breaker says remove frontmost middle choice.
 *    We pop from left's back!
 *    Pop 4 -> left=[1], right=[3, 2].
 * 
 * Pitfalls:
 * - Incorrect tie-breaking on `popMiddle`: The prompt states "popping middle from 
 *   [1, 2, 3, 4] returns 2". This means when sizes are equal, the middle element is 
 *   the LAST element of the `left` deque.
 * - Forgetting to call `balance()` after a front/back push/pop, leaving the halves 
 *   skewed by more than 1 element and corrupting the middle pointers.
 * 
 * Pattern Recognition: "Operations on ends AND exact middle" -> Split into Two Deques.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you make this thread-safe?
 * A1: I would wrap all public methods in standard `synchronized` blocks or use a 
 *     `ReentrantReadWriteLock`. I cannot just use thread-safe deques (like `ConcurrentLinkedDeque`) 
 *     because balancing requires atomically modifying BOTH deques together.
 * 
 * Q2: How could you optimize memory overhead?
 * A2: Java's `ArrayDeque` is backed by dynamic arrays and is memory-efficient, but 
 *     resizing creates latency spikes. For a strictly bounded environment, two circular 
 *     arrays (using head/tail pointers and modulo arithmetic) would provide zero-allocation 
 *     O(1) operations.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class FrontMiddleBackQueueStudy {

    static class FrontMiddleBackQueue {
        // Represents the first half of the queue
        private Deque<Integer> left;
        // Represents the second half of the queue
        private Deque<Integer> right;

        /**
         * Initializes the queue.
         */
        public FrontMiddleBackQueue() {
            left = new ArrayDeque<>();
            right = new ArrayDeque<>();
        }

        /**
         * Maintains the invariant:
         * 1. left.size() == right.size() (Even total size)
         * 2. right.size() == left.size() + 1 (Odd total size)
         */
        private void balance() {
            // If left has more elements, move the excess to right
            if (left.size() > right.size()) {
                right.offerFirst(left.pollLast());
            } 
            // If right has more than 1 excess element, move it to left
            else if (right.size() > left.size() + 1) {
                left.offerLast(right.pollFirst());
            }
        }

        /**
         * Adds val to the front of the queue.
         * Time: O(1)
         */
        public void pushFront(int val) {
            left.offerFirst(val);
            balance();
        }

        /**
         * Adds val to the middle of the queue.
         * Time: O(1)
         */
        public void pushMiddle(int val) {
            // If left is smaller, adding to left perfectly balances the sizes
            if (left.size() < right.size()) {
                left.offerLast(val);
            } 
            // If sizes are equal, adding to right maintains the "right holds odd excess" rule
            else {
                right.offerFirst(val);
            }
            balance(); // Defensive call, though logic above prevents imbalance
        }

        /**
         * Adds val to the back of the queue.
         * Time: O(1)
         */
        public void pushBack(int val) {
            right.offerLast(val);
            balance();
        }

        /**
         * Removes the front element of the queue and returns it.
         * Time: O(1)
         */
        public int popFront() {
            if (right.isEmpty()) { // Both are empty
                return -1;
            }
            
            int val;
            // Edge case: Total size is 1. Left is empty, right holds the element.
            if (left.isEmpty()) {
                val = right.pollFirst();
            } else {
                val = left.pollFirst();
            }
            
            balance();
            return val;
        }

        /**
         * Removes the middle element of the queue and returns it.
         * Time: O(1)
         */
        public int popMiddle() {
            if (right.isEmpty()) {
                return -1;
            }
            
            int val;
            // Tie-breaker rule: if sizes are equal, remove frontmost middle choice.
            // E.g., [1, 2] | [3, 4] -> pop 2 (which is left's tail)
            if (left.size() == right.size()) {
                val = left.pollLast();
            } 
            // Odd rule: right holds the true middle
            // E.g., [1, 2] | [3, 4, 5] -> pop 3 (which is right's head)
            else {
                val = right.pollFirst();
            }
            
            balance();
            return val;
        }

        /**
         * Removes the back element of the queue and returns it.
         * Time: O(1)
         */
        public int popBack() {
            if (right.isEmpty()) {
                return -1;
            }
            
            // Because right is always >= left, the absolute back is always in right
            int val = right.pollLast();
            balance();
            return val;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        FrontMiddleBackQueue q = new FrontMiddleBackQueue();
        
        q.pushFront(1);   // Queue: [1]
        q.pushBack(2);    // Queue: [1, 2]
        
        // Equal sizes. Pushes to right.
        q.pushMiddle(3);  // Queue: [1, 3, 2]
        
        // Left is smaller. Pushes to left.
        q.pushMiddle(4);  // Queue: [1, 4, 3, 2]
        
        System.out.println("popFront: " + q.popFront());   // Expected 1. Queue: [4, 3, 2]
        System.out.println("popMiddle: " + q.popMiddle()); // Expected 3. Queue: [4, 2]
        System.out.println("popMiddle: " + q.popMiddle()); // Expected 4. Queue: [2]
        System.out.println("popBack: " + q.popBack());     // Expected 2. Queue: []
        System.out.println("popFront: " + q.popFront());   // Expected -1. Queue: []
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Two Deques + Continuous Balancing.
 * Key Observation: You can access the physical middle of a stream in O(1) time 
 * by cutting the stream into two distinct Deque objects. The middle is naturally 
 * exposed at the inner-facing boundaries (`left.pollLast()` and `right.pollFirst()`).
 * What to Memorize: The balance invariant: `left` always equals `right`, except 
 * when the total size is odd, in which case `right` is exactly 1 larger.
 * Most Common Trap: `popFront` on a queue of size 1. If size is 1, `left` is empty 
 * and `right` holds the single element. You must check `left.isEmpty()` and poll 
 * from `right` instead, avoiding a silent null return.
 * Mental Trigger: "O(1) Middle operations" -> "Slice queue into two Deques".
 * ============================================================================
 */











































import java.util.ArrayDeque;
import java.util.Deque;

class FrontMiddleBackQueue {

    /*
     * The complete queue is always:
     *
     *     left followed by right
     *
     * Example:
     *
     *     left = [1, 2, 3]
     *     right = [4, 5]
     *
     *     Complete queue = [1, 2, 3, 4, 5]
     *
     * Why use two deques?
     *
     * A single deque cannot directly access its middle in O(1).
     * By maintaining the split at the middle, the middle element we need
     * is always the last element of left.
     *
     * INVARIANT:
     *
     *     left.size() == right.size()
     *     OR
     *     left.size() == right.size() + 1
     *
     * Thus, left contains either the same number of elements as right
     * or exactly one extra element.
     *
     * Consequently:
     * - Front       = left.peekFirst()
     * - Middle      = left.peekLast()
     * - Back        = right.peekLast(), if right is nonempty;
     *                 otherwise left.peekLast()
     */
    private final Deque<Integer> left = new ArrayDeque<>();
    private final Deque<Integer> right = new ArrayDeque<>();

    public FrontMiddleBackQueue() {
        // Both deques start empty.
    }

    public void pushFront(int val) {
        // The new element belongs at the very front of the complete queue.
        left.addFirst(val);

        // Adding to left may make it too large.
        // Move at most one boundary element if needed.
        rebalance();
    }

    public void pushMiddle(int val) {
        /*
         * We want to insert at index floor(n / 2).
         *
         * Case 1: Both halves have equal size.
         *
         *     left  = [1, 2]
         *     right = [3, 4]
         *
         *     Add 5 to the end of left:
         *
         *     left  = [1, 2, 5]
         *     right = [3, 4]
         *
         *     Result = [1, 2, 5, 3, 4]
         *
         * This is the correct middle insertion.
         *
         * Case 2: Left has one extra element.
         *
         *     left  = [1, 2, 3]
         *     right = [4, 5]
         *
         *     First move 3, the last element of left, to the front of right:
         *
         *     left  = [1, 2]
         *     right = [3, 4, 5]
         *
         *     Then append the new value 6 to left:
         *
         *     left  = [1, 2, 6]
         *     right = [3, 4, 5]
         *
         *     Result = [1, 2, 6, 3, 4, 5]
         *
         * Both cases leave the halves balanced.
         */
        if (left.size() > right.size()) {
            right.addFirst(left.removeLast());
        }

        // Inserting at the end of left places val at index floor(n / 2).
        left.addLast(val);
    }

    public void pushBack(int val) {
        // Append to the end of the complete queue.
        right.addLast(val);

        // If right becomes larger than left, transfer its first element
        // to the end of left so the invariant is restored.
        rebalance();
    }

    public int popFront() {
        if (left.isEmpty()) {
            // Both halves are empty, so the complete queue is empty.
            return -1;
        }

        // The first element of left is the first element of the full queue.
        int value = left.removeFirst();

        // Removing from left might make right larger than left.
        // If so, move the first element of right to the end of left.
        rebalance();

        return value;
    }

    public int popMiddle() {
        if (left.isEmpty()) {
            // An empty queue has no middle element.
            return -1;
        }

        /*
         * The last element of left is always the frontmost middle element.
         *
         * Odd size:
         *     left  = [1, 2, 3]
         *     right = [4, 5]
         *     Remove 3, the unique middle.
         *
         * Even size:
         *     left  = [1, 2, 3]
         *     right = [4, 5, 6]
         *     Remove 3, the frontmost of the two middle elements.
         *
         * Since left has either the same size as right or one extra element,
         * removing its last element always implements the required rule.
         *
         * No rebalance is necessary:
         * - If the sizes were equal, left becomes one smaller.
         * - If left had one extra element, the sizes become equal.
         *
         * Wait: in the equal-size case, removing from left leaves right
         * larger by one, which is allowed only after balancing. Therefore
         * we must rebalance after removal.
         */
        int value = left.removeLast();

        rebalance();

        return value;
    }

    public int popBack() {
        if (left.isEmpty()) {
            // Both halves are empty.
            return -1;
        }

        /*
         * The last element of right is the back of the complete queue
         * whenever right is nonempty.
         *
         * If right is empty, the queue contains only left, so its last
         * element is also the back of the complete queue.
         */
        int value;

        if (!right.isEmpty()) {
            value = right.removeLast();
        } else {
            value = left.removeLast();
        }

        // Removing from right may leave left too large.
        // Move one element from left to right if necessary.
        rebalance();

        return value;
    }

    private void rebalance() {
        /*
         * Restore the invariant:
         *
         *     left.size() == right.size()
         *     OR
         *     left.size() == right.size() + 1
         *
         * If left is too large, move its last element to right's front.
         *
         *     left  = [1, 2, 3, 4]
         *     right = [5, 6]
         *
         *     Becomes:
         *
         *     left  = [1, 2, 3]
         *     right = [4, 5, 6]
         *
         * If right is larger, move its first element to left's back.
         *
         *     left  = [1, 2]
         *     right = [3, 4, 5]
         *
         *     Becomes:
         *
         *     left  = [1, 2, 3]
         *     right = [4, 5]
         *
         * Why are two independent if statements sufficient?
         *
         * Every public operation changes the total size by at most one,
         * and the invariant was true before that operation.
         * Therefore, the sizes can become unbalanced by at most one element.
         * At most one transfer restores the invariant.
         *
         * The second if checks the current sizes after the first possible
         * transfer, keeping the method robust and easy to reason about.
         */
        if (left.size() > right.size() + 1) {
            right.addFirst(left.removeLast());
        }

        if (left.size() < right.size()) {
            left.addLast(right.removeFirst());
        }
    }

    /*
     * TIME COMPLEXITY:
     *
     * Every public operation performs a constant number of deque operations.
     * ArrayDeque.addFirst, addLast, removeFirst, and removeLast are O(1)
     * amortized operations.
     *
     * pushFront:  O(1)
     * pushMiddle: O(1)
     * pushBack:   O(1)
     * popFront:   O(1)
     * popMiddle:  O(1)
     * popBack:    O(1)
     *
     * rebalance: O(1), because at most one element moves between the deques.
     *
     * SPACE COMPLEXITY:
     *
     * O(n), where n is the number of elements currently stored.
     * Each element belongs to exactly one of the two deques.
     */
}






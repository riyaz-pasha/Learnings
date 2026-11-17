/**
 * 0. ## Problem Statement
 * Given a stream of integers and a window size, calculate the moving average of all 
 * integers in the sliding window.
 * 
 * Example: size = 3
 * next(1)  -> 1.0
 * next(10) -> (1 + 10) / 2 = 5.5
 * next(3)  -> (1 + 10 + 3) / 3 = 4.66667
 * next(5)  -> (10 + 3 + 5) / 3 = 6.0
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can the sum of the elements in the window exceed the 32-bit signed integer limit?"
 *    -> Prevents overflow bugs; tells us if we need a `long` or `double` for the running sum.
 * 2. "Is the window size guaranteed to be at least 1?"
 *    -> Prevents a divide-by-zero exception on the first call.
 * 3. "Are we optimizing for maximum throughput in a high-frequency trading system, or is standard application logic fine?"
 *    -> Changes the approach from a standard Queue object (which creates Node allocations) to a pre-allocated primitive array.
 * 4. "Is this class accessed by multiple threads concurrently?"
 *    -> Determines if we need synchronization around updating the queue and the sum simultaneously.
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: The stream is theoretically infinite, but memory is finite, and we only 
 * care about the most recent `N` elements. A naive approach recalculates things that haven't changed.
 * 
 * --- Approach 1: The "Keep Everything" Brute Force ---
 * Idea: Store every number that comes in using an `ArrayList`. When `next()` is called, look at 
 * the size of the list, take the last `min(size, N)` elements, and loop through them to find the sum.
 * - Why it works: It perfectly preserves the sequence of the stream.
 * - Why it's too slow/costly: We are storing data we will never read again, and re-adding the same 
 *   numbers over and over for the sum.
 * Time Complexity: O(N) per call — because we iterate backwards up to N times to compute the sum.
 * Space Complexity: O(M) — because we store the entire history of M elements in the stream, causing an eventual OutOfMemoryError.
 * 
 * --- Approach 2: Fixed-Size Shifted Array ---
 * Idea: Keep an array of exactly size N. When a new number arrives, shift every element one index 
 * to the left (dropping the oldest), place the new number at the end, and sum the array.
 * - Why it works: Solves the O(M) memory problem by bounding storage to N.
 * - What work is repeated: We are spending O(N) time copying elements just to move them over, and 
 *   another O(N) time recalculating the sum from scratch.
 * Time Complexity: O(N) per call — because shifting the elements and re-summing them touches every element in the window.
 * Space Complexity: O(N) — because we strictly allocate an array of size N and nothing more.
 * 
 * --- Approach 3: Queue with a Running Sum (The standard optimal) ---
 * Idea: Use a FIFO `Queue` (like `ArrayDeque`). Maintain a `runningSum` variable. When a new number `X` 
 * arrives, add it to `runningSum` and push it to the queue. If the queue size exceeds N, `pop()` the 
 * oldest element and subtract it from `runningSum`. 
 * - Why it works: It removes the O(N) iteration by relying on the mathematical property that 
 *   `newSum = oldSum + newElement - droppedElement`.
 * Time Complexity: O(1) per call — because queue insertion/deletion and basic arithmetic are constant time operations.
 * Space Complexity: O(N) — because the queue holds exactly N elements dynamically.
 * 
 * --- Approach 4: Circular Array (The low-latency optimal) ---
 * Idea: The `ArrayDeque` in Approach 3 manages a dynamic array under the hood. We can cut out the 
 * object overhead entirely by maintaining our own `int[]` array of size N, and using a `head` pointer 
 * that increments and wraps around using modulo arithmetic: `head = (head + 1) % N`.
 * - Why it works: Overwrites the oldest element directly without shifting anything, avoiding object wrappers.
 * Time Complexity: O(1) per call — because array indexing by modulo and math are O(1).
 * Space Complexity: O(N) — because we pre-allocate exactly one primitive array of size N.
 * 
 * Verdict: I would write Approach 3 (Queue) in a 45-minute interview. It maps perfectly to human 
 * intuition (first-in, first-out), the built-in `Deque` handles resizing and bounds safely, and it is 
 * extremely easy to get right without off-by-one errors on a whiteboard. I would verbally mention 
 * Approach 4 (Circular Array) to demonstrate I understand memory locality and object-allocation overhead.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - `size = 1`: The queue immediately pops the old element on every insertion. The average is just the current number.
 * - `Stream length < window size`: The denominator for the average must be the *current* count, not the fixed `size`.
 * - Fractional precision: A window of [1, 2] must yield 1.5, not 1 (requires `double` division, avoiding integer truncation).
 * - Overflow: If the window is 10,000 and numbers are `Integer.MAX_VALUE`, an `int` sum will overflow into negative numbers.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * "Do I need to recalculate the whole block, or just process the delta?" 
 * A sliding window moving forward by 1 step only changes at its boundaries: 1 enters, 1 leaves. 
 * `CurrentWindow = PreviousWindow + Entering - Leaving`.
 * 
 * -- ASCII Diagram (Circular Array Approach) --
 * size = 3, incoming stream: [10, 20, 30, 40]
 * 
 * Initial array: [ 0,  0,  0]   (head points at 0, count = 0)
 * Insert 10:     [10,  0,  0]   (head = 0, count = 1) -> Avg = 10/1
 * Insert 20:     [10, 20,  0]   (head = 1, count = 2) -> Avg = 30/2
 * Insert 30:     [10, 20, 30]   (head = 2, count = 3) -> Avg = 60/3
 * Insert 40:     [40, 20, 30]   (head = 0 (wrapped!), count = 3) 
 *                   ^ Oldest (10) overwritten by 40! Sum = 60 - 10 + 40 = 90. Avg = 90/3
 * 
 * -- Dry run (Queue Approach) --
 * size = 3, calls: next(1), next(10), next(3), next(5)
 * 1. next(1):
 *    - add 1 to Queue: [1]
 *    - add 1 to sum: sum = 1
 *    - Queue size (1) <= 3, nothing to pop.
 *    - Avg = sum (1) / size (1) = 1.0
 * 2. next(10):
 *    - add 10 to Queue: [1, 10]
 *    - add 10 to sum: sum = 11
 *    - Queue size (2) <= 3, nothing to pop.
 *    - Avg = sum (11) / size (2) = 5.5
 * 3. next(3):
 *    - add 3 to Queue: [1, 10, 3]
 *    - add 3 to sum: sum = 14
 *    - Queue size (3) <= 3, nothing to pop.
 *    - Avg = sum (14) / size (3) = 4.66667
 * 4. next(5):
 *    - add 5 to Queue: [1, 10, 3, 5]
 *    - Queue size (4) > 3! Must pop oldest.
 *    - pop 1. Queue is now: [10, 3, 5]
 *    - subtract popped from sum: sum = 14 - 1 = 13
 *    - add 5 to sum: sum = 13 + 5 = 18
 *    - Avg = sum (18) / size (3) = 6.0
 * 
 * -- Pitfalls --
 * 1. Integer division trap: `return sum / queue.size();` where both are integers. Java truncates `5 / 2` to `2` instead of `2.5`.
 * 2. Denominator trap: Using the fixed `size` as the denominator before the window is full (e.g., dividing by 3 when only 2 elements exist).
 * 
 * -- Pattern recognition --
 * When you see "moving average", "last N elements", or "recent activity", think **Sliding Window + Deque/Queue**.
 * Transfers to: Sliding Window Maximum (Leetcode 239), Number of Recent Calls (Leetcode 933).
 * 
 * -- Interview script --
 * "Since we only care about the last N elements, we can think of this as a sliding window. 
 * Instead of summing the entire window every time, we can optimize this by keeping a running sum.
 * When a new number arrives, we add it to a Queue and to our sum. If our Queue exceeds size N, 
 * we simply dequeue the oldest number and subtract it from our sum. This turns an O(N) scan into 
 * an O(1) delta calculation. That gives us O(1) time and O(N) space."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: How would you handle a dynamically changing window size?
 * A: The Queue approach adapts effortlessly. If `resize(newSize)` is called, we just loop `while (queue.size() > newSize)` 
 *    and keep popping/subtracting. A Circular Array would be harder here, requiring a new array allocation and array-copy.
 * 
 * Q: What if instead of the average, we wanted the Moving Median?
 * A: A running sum wouldn't work because the median isn't an aggregate value that can be updated via delta. 
 *    We'd need a Two-Heap approach (Max-Heap for lower half, Min-Heap for upper half), plus a way to lazily 
 *    remove elements as they fall out of the window. Time complexity would drop from O(1) to O(log N).
 * 
 * Q: If this stream is producing millions of events per second, how do we reduce Garbage Collection (GC) pressure?
 * A: The `ArrayDeque` in Approach 3 allocates `Integer` wrapper objects (autoboxing). At high throughput, 
 *    this generates massive GC churn. We'd switch to Approach 4 (Circular Array) using a primitive `int[]` 
 *    to guarantee zero allocations after initialization.
 * =========================================================================================
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class MovingAverageStudyNote {

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH 1: Queue with Running Sum (Best for Whiteboard/Interview)
    // ----------------------------------------------------------------------
    static class MovingAverageQueue {
        // Defines the maximum size of our sliding window
        // E.g., if maxWindowSize = 3, we never hold more than 3 elements.
        private final int maxWindowSize;
        
        // Holds the actual elements in a First-In-First-Out (FIFO) manner.
        // ArrayDeque is preferred over LinkedList as it uses a resizable array 
        // internally, avoiding Node object allocation overhead per element.
        private final Deque<Integer> window;
        
        // Tracks the sum of the elements currently in the `window`.
        // Declared as `double` to prevent Integer overflow on massive sums, 
        // and to ensure floating-point division when calculating the average.
        // E.g., if window contains [10, 5], windowSum = 15.0
        private double windowSum;

        public MovingAverageQueue(int size) {
            this.maxWindowSize = size;
            this.window = new ArrayDeque<>();
            this.windowSum = 0.0;
        }

        public double next(int val) {
            // 1. Add the new incoming value to our running sum and window
            window.offer(val);
            windowSum += val;

            // 2. Check the constraint: Did we exceed the allowed window size?
            // If so, remove the oldest element (at the front) and subtract it from sum.
            if (window.size() > maxWindowSize) {
                // `poll()` removes and returns the first element
                int oldestValue = window.poll();
                windowSum -= oldestValue;
            }

            // 3. Calculate average. 
            // Crucial detail: Divide by the CURRENT size of the window, not maxWindowSize,
            // because the window might not be full yet (e.g., first few calls).
            return windowSum / window.size();
        }
    }

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH 2: Circular Array (Best for High-Performance / Low Latency)
    // ----------------------------------------------------------------------
    static class MovingAverageCircular {
        // Pre-allocated array to hold exactly `size` primitive ints. Zero GC churn.
        private final int[] window;
        
        // Points to the index where the NEXT element should be written.
        // Wraps around to 0 when it hits the end of the array.
        private int head;
        
        // Tracks how many elements we've seen in total.
        // We use this to know if the array is full yet, and to handle the denominator.
        private int count;
        
        // Running sum. Double prevents overflow and ensures correct division.
        private double windowSum;

        public MovingAverageCircular(int size) {
            this.window = new int[size];
            this.head = 0;
            this.count = 0;
            this.windowSum = 0.0;
        }

        public double next(int val) {
            count++;
            
            // If the window is full (count > window.length), the value currently residing 
            // at `window[head]` is the oldest value. We must subtract it before overwriting.
            // E.g., if array is [10, 20, 30] and head is 0, window[0] is 10 (the oldest).
            if (count > window.length) {
                windowSum -= window[head];
            }
            
            // Overwrite the oldest slot with the new value
            window[head] = val;
            
            // Add the new value to our sum
            windowSum += val;
            
            // Move the head forward, wrapping around using modulo arithmetic.
            // E.g., (2 + 1) % 3 = 0.
            head = (head + 1) % window.length;
            
            // The denominator is bounded by the array size. 
            // If count is 2 (not full), divide by 2. If count is 5 (full size 3), divide by 3.
            int divisor = Math.min(count, window.length);
            
            return windowSum / divisor;
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("Testing Queue Approach (Size = 3):");
        MovingAverageQueue mq = new MovingAverageQueue(3);
        System.out.println("next(1)  -> " + mq.next(1) + " (Expected: 1.0)");
        System.out.println("next(10) -> " + mq.next(10) + " (Expected: 5.5)");
        System.out.println("next(3)  -> " + mq.next(3) + " (Expected: 4.666666666666667)");
        System.out.println("next(5)  -> " + mq.next(5) + " (Expected: 6.0)");

        System.out.println("\nTesting Circular Array Approach (Size = 3):");
        MovingAverageCircular mc = new MovingAverageCircular(3);
        System.out.println("next(1)  -> " + mc.next(1) + " (Expected: 1.0)");
        System.out.println("next(10) -> " + mc.next(10) + " (Expected: 5.5)");
        System.out.println("next(3)  -> " + mc.next(3) + " (Expected: 4.666666666666667)");
        System.out.println("next(5)  -> " + mc.next(5) + " (Expected: 6.0)");

        System.out.println("\nTesting Edge Case - Size = 1:");
        MovingAverageQueue m1 = new MovingAverageQueue(1);
        System.out.println("next(10) -> " + m1.next(10) + " (Expected: 10.0)");
        System.out.println("next(20) -> " + m1.next(20) + " (Expected: 20.0)");
        System.out.println("next(30) -> " + m1.next(30) + " (Expected: 30.0)");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Fixed-size Sliding Window / Queue state tracking.
 * - Key observation: Average updates are just delta calculations (add incoming, subtract outgoing). 
 *   You never need to re-sum the whole array.
 * - Worth memorizing: The formula `average = sum / current_size`, and ensuring variables use `double` 
 *   to avoid integer division truncation.
 * - Most common trap: Dividing by `maxSize` before the window is fully populated.
 * - One-line mental trigger: "Data stream average? -> ArrayDeque + running double sum."
 * =========================================================================================
 */









































import java.util.ArrayDeque;
import java.util.Deque;

class MovingAverage {

    private final int size;

    // Queue maintains only the latest 'size' elements.
    private final Deque<Integer> window;

    // Maintains the sum of all elements currently in the queue.
    private long windowSum;

    public MovingAverage(int size) {

        // Store the maximum window capacity.
        this.size = size;

        // ArrayDeque provides efficient insertion and removal
        // at both ends. We use it as a FIFO queue.
        this.window = new ArrayDeque<>();

        // Initially, the window is empty.
        this.windowSum = 0;
    }

    public double next(int val) {

        // STEP 1:
        // If the window is already full, remove its oldest element.
        //
        // Why remove BEFORE insertion?
        // Because the maximum allowed window size is 'size'.
        //
        // Example:
        // size = 3
        // window = [1, 10, 3]
        //
        // Incoming value = 5
        //
        // Remove 1 first:
        // window = [10, 3]
        // windowSum = 13
        if (window.size() == size) {

            // pollFirst() removes and returns the oldest element.
            int oldest = window.pollFirst();

            // Since that element is no longer part of the window,
            // subtract it from our running sum.
            windowSum -= oldest;
        }

        // STEP 2:
        // Add the incoming element to the rear of the queue.
        window.offerLast(val);

        // Update the running sum in O(1).
        windowSum += val;

        // STEP 3:
        // Calculate the average using the CURRENT window size.
        //
        // Important:
        // During the initial calls, the window might not be full.
        // Therefore, we must divide by window.size(),
        // NOT by the configured maximum capacity.
        //
        // Casting to double ensures floating-point division.
        return (double) windowSum / window.size();
    }
}

/*
TIME COMPLEXITY:

Constructor:
    O(1)

next():
    O(1)

    At most:
      1. Remove the oldest element: O(1).
      2. Insert the new element: O(1).
      3. Update the running sum: O(1).
      4. Calculate the average: O(1).

    Therefore, each next() call takes O(1).

    For q incoming elements:
    O(q) total processing time.

SPACE COMPLEXITY:

    O(size)

    The queue never contains more than 'size' elements.

    We additionally maintain one long variable for the sum.

WHY THIS IS OPTIMAL:

    We eliminate repeated summation.

    We also discard elements that are no longer relevant.

    Each incoming element enters the queue once and leaves
    the queue at most once.

    This is the standard sliding-window + running-sum pattern.
*/


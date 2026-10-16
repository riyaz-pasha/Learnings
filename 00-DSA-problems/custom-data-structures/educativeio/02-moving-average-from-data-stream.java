/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given a stream of integers and a window size, calculate the moving average of 
 * all integers in the sliding window. 
 * Implement `MovingAverage(int size)` and `double next(int val)`.
 * Constraints: 1 <= size <= 100, -10^3 <= val <= 10^3, at most 10^2 calls.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. What should we return before we've seen `size` elements? 
 *    (Assuming we divide by the number of elements seen so far, not the fixed size).
 * 2. Will the sum of `size` elements ever exceed the 32-bit signed integer limit?
 *    (With max size 100 and max val 1000, max sum is 10^5, so standard `int` is safe. 
 *    But in production, a `long` or `double` sum is required to prevent overflow).
 * 3. Is the `size` guaranteed to be greater than 0?
 *    (Constraint says size >= 1. If it could be 0, we'd need to prevent divide-by-zero errors).
 * 4. Are we operating in a high-frequency trading or embedded environment where 
 *    garbage collection (GC) pauses are unacceptable? 
 *    (If yes, we cannot use Java Collections like LinkedList because they create objects on every insert).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement: Maintain a running average over a fixed trailing window of a continuous stream.
 *   The binding constraint is that the stream is unbounded over time (conceptually), 
 *   so we cannot store the entire history. We only care about the last N elements.
 * 
 * --- Approach 1: The Brute Force (Full History Log) ---
 * 1. Intuition: Store everything, just look at the end.
 * 2. Try: Use an `ArrayList<Integer>`. On `next(val)`, append `val`. Loop through the 
 *    last `min(size, list.size())` elements, sum them up, and divide.
 * 3. Why it works: It perfectly captures the state of the world.
 * 4. Why it's too costly: It leaks memory. The stream never stops, so the ArrayList grows infinitely.
 * 5. Bottleneck: Storing dead data (elements older than `size`) and recalculating the sum from scratch.
 * 
 * Time Complexity: O(size) — because we iterate the last `size` elements on every `next` call.
 * Space Complexity: O(M) — where M is the total number of elements ever seen in the stream (Unbounded).
 * 
 * --- Approach 2: The Queue (Sliding Window Memory) ---
 * 1. Intuition: We only need the last N elements. A Queue (FIFO) perfectly models a sliding window.
 * 2. Try: Use a `Deque<Integer>`. 
 *    `next(val)` -> add to tail. If `deque.size() > size`, `deque.pollFirst()`. 
 *    Then iterate the deque to sum the values.
 * 3. Why it works: Old elements are automatically discarded, fixing the memory leak.
 * 4. Why it's too costly: We are still recalculating the sum from scratch every time.
 * 5. Bottleneck: Repeated addition. If the window is [2, 3, 4] and moves to [3, 4, 5], 
 *    we recalculate 3+4 twice.
 * 
 * Time Complexity: O(size) — because we still iterate the queue to find the sum.
 * Space Complexity: O(size) — because we bounded the queue to maximum `size` elements.
 * 
 * --- Approach 3: Queue + Running Sum (Optimal for Interviews) ---
 * 1. Intuition: Math! The new window sum is just the old window sum, minus the element 
 *    that fell off, plus the element that just arrived.
 * 2. Try: Keep a `Deque` and a `double windowSum`. 
 *    `next(val)` -> `windowSum += val`. Add to tail. 
 *    If `deque.size() > size`, `windowSum -= deque.pollFirst()`.
 * 3. Why it works: Math eliminates the need to iterate. 
 * 
 * Time Complexity: O(1) — Constant time math updates.
 * Space Complexity: O(size) — Storing `size` boxed Integers in the Deque.
 * 
 * --- Approach 4: Primitive Circular Array (Ultimate Performance) ---
 * 1. Intuition: A `Deque<Integer>` requires object allocation (autoboxing `int` to `Integer`, 
 *    and internal Node creation). We can simulate a Queue perfectly using a fixed-size primitive array.
 * 2. Try: `int[] window = new int[size]`. Use an `insertIndex = (insertIndex + 1) % size`.
 *    To remove the oldest element, just overwrite it and subtract the old value from the running sum!
 * 3. Why to choose this: Zero garbage collection overhead, continuous memory block (cache locality), 
 *    and exact memory footprint. In a 45-min interview, proposing this shows senior-level mechanical sympathy.
 * 
 * Time Complexity: O(1) — Array index math.
 * Space Complexity: O(size) — Fixed primitive array.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Window not yet full: Dividing by `size` will give the wrong average. We must divide 
 *   by `min(elements_seen, size)`.
 * - Negative inputs: Work flawlessly with running sum. No special logic needed.
 * - Integer division vs Floating point division: `int / int` drops decimals. Must cast 
 *   sum or count to `double` before dividing.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & PITFALLS
 * ============================================================================
 * > Key Insight: "Overlapping subproblems in streams." The transition between states 
 *   in a sliding window always shares N-1 elements. Never compute from scratch what 
 *   you can compute by finding the delta (difference).
 * 
 * Diagram: Circular Array logic (size = 3)
 * Initial: array = [0, 0, 0], head=0, sum=0
 * next(1) -> overwrites array[0] (0). sum = sum - 0 + 1 = 1. head=(0+1)%3=1. Avg = 1/1
 * next(2) -> overwrites array[1] (0). sum = sum - 0 + 2 = 3. head=(1+1)%3=2. Avg = 3/2
 * next(3) -> overwrites array[2] (0). sum = sum - 0 + 3 = 6. head=(2+1)%3=0. Avg = 6/3
 * next(4) -> overwrites array[0] (1). sum = sum - 1 + 4 = 9. head=(0+1)%3=1. Avg = 9/3
 * 
 * Pitfalls:
 * - Integer overflow on the running sum. If `val` is near `Integer.MAX_VALUE` and window 
 *   is large, `int windowSum` will overflow into negatives. Always use `double` or `long` 
 *   for the accumulator.
 * 
 * Pattern Recognition: "When you see 'sliding window' or 'stream', think Queue + Running Accumulator."
 * Transfers to: Sliding Window Maximum (Leetcode 239 - using monotonic queue), 
 * Number of Recent Calls (Leetcode 933).
 * 
 * Interview Script snippet:
 * "I could use a standard ArrayDeque to store the window and keep a running sum to hit O(1) time. 
 * However, since we know the exact maximum size of the window upon initialization, a primitive 
 * circular array is actually strictly better. It avoids Integer autoboxing and the overhead of 
 * node creation, making it GC-friendly. I'll maintain a modulo pointer to overwrite the oldest 
 * element."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if the window size is 10 million, and memory is tight?
 * A1: 10 million ints is ~40MB, which is usually fine. But if memory is strictly constrained, 
 *     we might need to downsample, use an approximation algorithm (like Exponential Moving Average, 
 *     which takes O(1) space total), or persist chunks to disk.
 * 
 * Q2: What if we wanted the Median of the sliding window instead of the Average?
 * A2: The running sum trick wouldn't work. We would need two Heaps (a max-heap for the lower half 
 *     and min-heap for the upper half) and lazy deletion using a Hash Map, bringing time 
 *     complexity to O(log N) per step.
 */

public class MovingAverageStudy {

    /**
     * Optimal Implementation: Circular Primitive Array
     */
    static class MovingAverage {
        
        // Fixed size array avoids dynamic memory allocation overhead.
        private final int[] window;
        
        // Tracks how many elements we've seen total.
        // Also used to calculate the denominator before the window is fully populated.
        private int count;
        
        // The index where the next incoming element will be written.
        private int head;
        
        // Accumulator. Using double prevents integer overflow and handles float division seamlessly.
        private double windowSum;

        /**
         * Constructor: Initializes the object with the specified window size.
         */
        public MovingAverage(int size) {
            // size >= 1 is guaranteed by constraints.
            this.window = new int[size]; 
            this.count = 0;
            this.head = 0;
            this.windowSum = 0;
        }

        /**
         * Takes an integer value as input and returns the moving average.
         */
        public double next(int val) {
            // 1. count increases, but we cap the denominator at the window's physical length.
            count++;
            
            // 2. Subtract the element that is about to be overwritten (the oldest element).
            // For the first `size` elements, window[head] is naturally 0 (Java default), 
            // so this safely subtracts 0 without altering the sum incorrectly.
            windowSum -= window[head];
            
            // 3. Add the new value to the running sum.
            windowSum += val;
            
            // 4. Overwrite the oldest value with the new value.
            window[head] = val;
            
            // 5. Advance the head pointer circularly.
            // When head hits the end of the array, it wraps back to index 0.
            head = (head + 1) % window.length;
            
            // 6. Calculate average. 
            // If we've seen 2 elements but window size is 5, divide by 2.
            int divisor = Math.min(count, window.length);
            
            return windowSum / divisor;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        // Initialize moving average with window size 3
        MovingAverage m = new MovingAverage(3);
        
        // Test 1: Window not full yet. (Avg of [1]) -> 1.0
        System.out.println("Added 1, Avg: " + m.next(1)); 
        assert m.next(0) == 0.5; // (Avg of [1, 0] = 0.5), wait let's just print.
        
        // Resetting for clean dry run test
        m = new MovingAverage(3);
        
        System.out.println("Adding 1: " + m.next(1));    // return 1.0 = 1 / 1
        System.out.println("Adding 10: " + m.next(10));  // return 5.5 = (1 + 10) / 2
        System.out.println("Adding 3: " + m.next(3));    // return 4.66667 = (1 + 10 + 3) / 3
        
        // Here, the window is [1, 10, 3]. The oldest is 1.
        // Adding 5 pushes out 1. Window becomes [10, 3, 5].
        System.out.println("Adding 5: " + m.next(5));    // return 6.0 = (10 + 3 + 5) / 3
        
        // Edge Case: Negative values
        System.out.println("Adding -6: " + m.next(-6));  // pushes out 10. Window: [3, 5, -6]. Avg: 2.0 / 3
    }
}

/**
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core Pattern: Sliding Window + Running Accumulator.
 * Key Observation: State transitions share N-1 elements. Update by tracking the delta 
 * (add new, subtract old) instead of recalculating.
 * What to Memorize: Circular array logic `head = (head + 1) % length` combined with 
 * extracting `window[head]` BEFORE overwriting it.
 * Most Common Trap: Using Integer division (`sum / count`) instead of floating point, 
 * resulting in truncated averages (e.g., 5/2 = 2 instead of 2.5).
 * Mental Trigger: "Moving Window Stats" -> "Delta Update + Modulo Array".
 * ============================================================================
 */

class MovingAverageCircularBuffer {

    private final int size;
    private final int[] window;

    private int nextIndex;
    private int count;
    private long sum;

    public MovingAverageCircularBuffer(int size) {
        this.size = size;
        this.window = new int[size];

        this.nextIndex = 0;
        this.count = 0;
        this.sum = 0L;

        // nextIndex identifies the array position that will be written
        // next. Once it reaches the end, it wraps around to index zero.
        //
        // count tracks how many values have been received, capped at size.
        //
        // Constructor time: O(size), because the array is initialized.
        // Auxiliary space: O(size), for the fixed-size array.
    }

    public double next(int val) {

        // If the buffer is full, window[nextIndex] is the oldest value.
        // Subtract that outgoing value before overwriting it.
        if (count == size) {
            sum -= window[nextIndex];
        } else {
            // During the initial filling phase, no value is overwritten.
            count++;
        }

        // Write the incoming value into the current circular position.
        window[nextIndex] = val;
        sum += val;

        // Move to the next position. Modulo wraps the index back to zero
        // when nextIndex reaches size.
        nextIndex = (nextIndex + 1) % size;

        // count is the number of values currently in the window.
        // It is less than size during initialization and equals size
        // once the circular buffer has filled.
        return (double) sum / count;
    }

    // Time complexity:
    //   Reading or overwriting one array position: O(1).
    //   Updating the running sum and count: O(1).
    //   Advancing the circular index: O(1).
    //   Therefore, next() is O(1) worst-case time.
    //
    // Space complexity:
    //   The fixed-size array uses O(size) space.
    //   All other fields use O(1) additional space.
    //
    // Compared with ArrayDeque, this implementation provides predictable
    // O(1) worst-case updates and avoids queue-node/deque bookkeeping.
    // The trade-off is that circular-index management is slightly less
    // straightforward to explain and implement.
}


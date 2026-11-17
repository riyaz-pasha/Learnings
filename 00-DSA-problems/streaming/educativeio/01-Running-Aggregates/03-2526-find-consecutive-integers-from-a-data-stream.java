/**
 * 0. ## Problem Statement
 * Implement a data structure that receives a stream of integers.
 * - `DataStream(int value, int k)`: Initializes the object with a target `value` and window size `k`.
 * - `boolean consec(int num)`: Adds `num` to the stream. Returns `true` if the last `k` integers 
 *   parsed are equal to `value`. If fewer than `k` integers have been parsed, returns `false`.
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can `k` or `value` change dynamically during the stream, or are they fixed?"
 *    -> Confirms whether we can bake these into final instance variables or if we need a more flexible state.
 * 2. "Will `k` ever be 0?"
 *    -> Constraint says `1 <= k <= 10^5`, preventing meaningless empty-window queries.
 * 3. "Do we need to store the actual stream for other queries later, or is this the ONLY query?"
 *    -> Determines if we need a memory-heavy Queue/List or if we can discard historical data.
 * 4. "Is `num` guaranteed to be a valid integer?"
 *    -> Constraints say up to 10^9, so it fits nicely in a standard 32-bit `int`.
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to 10^5 calls to `consec`. If we use an O(K) operation per call, we 
 * hit 10^10 operations, which will cause a Time Limit Exceeded (TLE). We need an O(1) time approach.
 * 
 * --- Approach 1: The Brute Force Window ---
 * Idea: Keep an `ArrayList` or `LinkedList`. On every `consec(num)` call, append `num`. 
 * Then, look at the last `k` elements and loop through them. If they all equal `value`, return true.
 * - Why it works: Literally simulates the problem statement.
 * - Why it's too slow/costly: We are re-checking elements we already know match (or don't match).
 * Time Complexity: O(k) per call — because we iterate backward `k` times on every insertion.
 * Space Complexity: O(N) — because we store the entire history of `N` streamed elements.
 * 
 * --- Approach 2: Sliding Window with a Match Counter ---
 * Idea: Limit memory by using a Queue of strictly size `k`. To avoid the O(k) loop, keep a `matchCount` 
 * variable. When a new number arrives: if it equals `value`, `matchCount++`. When the queue exceeds 
 * size `k`, pop the oldest. If the popped number equals `value`, `matchCount--`. Return `matchCount == k`.
 * - Why it works: We maintain a running tally of matches in our current window, updating via deltas.
 * - What work is being repeated: We are storing elements in a Queue just to eventually pop them, but 
 *   we already *know* exactly what they are (they are either `value` or not).
 * Time Complexity: O(1) per call — because enqueue, dequeue, and counter updates take constant time.
 * Space Complexity: O(k) — because we store the last `k` elements in the Queue.
 * 
 * --- Approach 3: The Pure Streak Counter (Optimal) ---
 * Idea: How do we discover the O(1) space solution? Ask: *What actually breaks the condition?*
 * ANY number that is not `value` completely ruins the chance of the last `k` elements matching. 
 * A non-matching number acts as an absolute wall. Therefore, we don't need a Queue at all. We just 
 * need to count how many times `value` has appeared *consecutively* without interruption. 
 * If `num == value`, `streak++`. If `num != value`, `streak = 0`. Return `streak >= k`.
 * - Why it works: It leverages the "consecutive" requirement. A single mismatch resets progress to zero.
 * Time Complexity: O(1) per call — because we only do a basic equality check and increment a counter.
 * Space Complexity: O(1) — because we only store two integers (`streak` and the initial configurations).
 * 
 * Verdict: I would write Approach 3 immediately. It is perfectly optimal in both time and space, 
 * trivially easy to code without risk of off-by-one bounds errors on a Queue, and demonstrates 
 * strong analytical simplification (recognizing that historical data can be safely discarded).
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - `k = 1`: The streak only needs to hit 1. Should instantly return `true` on the first match.
 * - Stream has exactly `k-1` correct values, then 1 wrong value: Verifies that the reset instantly 
 *   prevents a false positive.
 * - Stream length < `k`: Should consistently return `false` even if all numbers match `value`.
 * - Massive consecutive matches (`streak` >> `k`): Verifies we use `streak >= k` rather than `streak == k`.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * "Do I actually need to remember the past?" 
 * In problems demanding consecutive identical states, a single failure invalidates the whole sequence. 
 * This means you only ever need to track the *current continuous streak*.
 * 
 * -- ASCII Diagram (Streak Tracker) --
 * value = 4, k = 3. Stream: [4, 4, 3, 4, 4, 4, 4]
 * 
 * Num | Action       | Streak | Return (streak >= 3)
 * ---------------------------------------------------
 *  4  | Match! (+1)  |   1    | False (1 < 3)
 *  4  | Match! (+1)  |   2    | False (2 < 3)
 *  3  | WRONG! (=0)  |   0    | False (0 < 3)  <-- The "Wall"
 *  4  | Match! (+1)  |   1    | False (1 < 3)
 *  4  | Match! (+1)  |   2    | False (2 < 3)
 *  4  | Match! (+1)  |   3    | TRUE  (3 >= 3) <-- Window satisfied!
 *  4  | Match! (+1)  |   4    | TRUE  (4 >= 3)
 * 
 * -- Dry run (Optimal Approach) --
 * DataStream(4, 3) -> value=4, k=3, streak=0
 * 1. consec(4): num == 4. streak becomes 1. 1 >= 3 is False. Returns False.
 * 2. consec(4): num == 4. streak becomes 2. 2 >= 3 is False. Returns False.
 * 3. consec(4): num == 4. streak becomes 3. 3 >= 3 is True. Returns True.
 * 4. consec(3): num != 4. streak becomes 0. 0 >= 3 is False. Returns False.
 * 
 * -- Pitfalls --
 * 1. Over-engineering: Blindly using a Queue or Deque because the problem mentions a "stream" and 
 *    the "last k elements". Always challenge if the history is actually needed.
 * 2. `streak == k` vs `streak >= k`: If you use `==`, the (k+1)th matching element will incorrectly 
 *    return false because streak became k+1. You must use `>=`.
 * 
 * -- Pattern recognition --
 * When you see "consecutive sequence of X" or "stream without interruptions", think **State Machine / Streak Counter**.
 * Transfers to: Max Consecutive Ones (LeetCode 485), Longest Subarray of 1s (LeetCode 1493).
 * 
 * -- Interview script --
 * "Since we only care if the last `k` elements exactly match a specific `value`, any number that isn't `value` 
 * breaks the streak completely. Instead of storing the last `k` numbers in a Queue, which takes O(k) space, 
 * we can just track the current unbroken streak of `value`. Every time we see `value`, we increment our streak. 
 * Every time we see anything else, we reset the streak to 0. If the streak is greater than or equal to `k`, 
 * we return true. This yields an O(1) time and O(1) space solution."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if we want to check if the last `k` integers are strictly INCREASING instead of a fixed value?
 * A: The streak logic still applies! Instead of checking `num == value`, we check `num > previousNum`. 
 *    If true, `streak++`. If false, `streak = 1`. We'd just need to store `previousNum` as state. Time O(1), Space O(1).
 * 
 * Q: What if we need to check if the last `k` integers match a *specific pattern* (e.g., [1, 2, 3])?
 * A: Now we need historical context. A simple streak counter fails. We would need a Queue of size `k` to track 
 *    the window, and likely use a rolling hash (Rabin-Karp) or KMP algorithm over the stream to achieve O(1) time.
 * 
 * Q: What if we have multiple threads calling `consec` concurrently?
 * A: The `streak` variable would suffer from race conditions. We'd need to use an `AtomicInteger` for the streak, 
 *    or synchronize the `consec` method, though synchronization might create a bottleneck if throughput is high.
 * =========================================================================================
 */

public class DataStreamStudyNote {

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH: Pure Streak Counter (O(1) Time, O(1) Space)
    // ----------------------------------------------------------------------
    static class DataStream {
        
        // The target value we are looking for
        private final int targetValue;
        
        // The required window size
        private final int k;
        
        // Tracks the current unbroken chain of matching values
        // E.g., if we see 4, 4, 4 and target is 4, streak = 3.
        private int currentStreak;

        public DataStream(int value, int k) {
            this.targetValue = value;
            this.k = k;
            this.currentStreak = 0;
        }
        
        public boolean consec(int num) {
            // 1. Update the state
            if (num == targetValue) {
                // Number matches, extend the streak
                currentStreak++;
            } else {
                // The "Wall". Any mismatch completely ruins the sequence.
                // We wipe out the streak back to 0.
                currentStreak = 0;
            }
            
            // 2. Evaluate condition
            // If our unbroken streak is at least k, it means the last k elements
            // (and possibly more) were all exactly `targetValue`.
            return currentStreak >= k;
        }
    }

    // ----------------------------------------------------------------------
    // SUB-OPTIMAL APPROACH: Queue with Match Counter (O(1) Time, O(k) Space)
    // Included to show the logical bridge between Brute Force and Optimal
    // ----------------------------------------------------------------------
    static class DataStreamQueue {
        private final int targetValue;
        private final int k;
        
        // Stores the actual last `k` elements
        private final java.util.Deque<Integer> window;
        
        // Tracks how many elements inside `window` equal `targetValue`
        private int matchesInWindow;

        public DataStreamQueue(int value, int k) {
            this.targetValue = value;
            this.k = k;
            this.window = new java.util.ArrayDeque<>();
            this.matchesInWindow = 0;
        }
        
        public boolean consec(int num) {
            // Add new element and update match count
            window.offer(num);
            if (num == targetValue) {
                matchesInWindow++;
            }
            
            // Enforce window size limit
            if (window.size() > k) {
                int oldest = window.poll();
                if (oldest == targetValue) {
                    matchesInWindow--;
                }
            }
            
            // The condition is met only if the window is fully populated
            // AND every element in it is a match.
            return window.size() == k && matchesInWindow == k;
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("Testing Optimal Streak Approach (Value = 4, k = 3):");
        DataStream optimalStream = new DataStream(4, 3);
        System.out.println("consec(4) -> " + optimalStream.consec(4) + " (Expected: false, count=1)");
        System.out.println("consec(4) -> " + optimalStream.consec(4) + " (Expected: false, count=2)");
        System.out.println("consec(4) -> " + optimalStream.consec(4) + " (Expected: true, count=3)");
        System.out.println("consec(3) -> " + optimalStream.consec(3) + " (Expected: false, streak broken!)");
        System.out.println("consec(4) -> " + optimalStream.consec(4) + " (Expected: false, count=1)");

        System.out.println("\nTesting Queue Approach (Value = 4, k = 3):");
        DataStreamQueue queueStream = new DataStreamQueue(4, 3);
        System.out.println("consec(4) -> " + queueStream.consec(4) + " (Expected: false)");
        System.out.println("consec(4) -> " + queueStream.consec(4) + " (Expected: false)");
        System.out.println("consec(4) -> " + queueStream.consec(4) + " (Expected: true)");
        System.out.println("consec(3) -> " + queueStream.consec(3) + " (Expected: false)");

        System.out.println("\nTesting Edge Case - k = 1 (Value = 10, k = 1):");
        DataStream edgeStream = new DataStream(10, 1);
        System.out.println("consec(10) -> " + edgeStream.consec(10) + " (Expected: true instantly)");
        System.out.println("consec(5)  -> " + edgeStream.consec(5) + " (Expected: false)");
        System.out.println("consec(10) -> " + edgeStream.consec(10) + " (Expected: true instantly)");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Streak / State Tracking.
 * - Key observation: In an equality sequence requirement, a single non-matching element is 
 *   a complete reset. Historical elements behind the mismatch are irrelevant.
 * - Worth memorizing: Bypassing a Queue entirely when the problem only cares about continuous streaks.
 * - Most common trap: Using `currentStreak == k` instead of `>= k`, causing valid subsequent 
 *   elements to erroneously return `false`.
 * - One-line mental trigger: "Are all last K identical? Just count the current streak."
 * =========================================================================================
 */










































class DataStream {

    private final int value;
    private final int k;

    // Number of consecutive occurrences of value
    // ending at the current position in the stream.
    private int count;

    public DataStream(int value, int k) {

        this.value = value;
        this.k = k;

        // Initially, the stream is empty.
        this.count = 0;
    }

    public boolean consec(int num) {

        // Case 1:
        // The incoming number matches our target value.
        // Therefore, the current consecutive sequence extends.
        if (num == value) {
            count++;
        }

        // Case 2:
        // The incoming number is different from our target.
        // Therefore, the consecutive sequence is broken.
        // Reset the counter because no valid sequence can end here.
        else {
            count = 0;
        }

        // We need at least k consecutive occurrences of value.
        //
        // Use >= instead of == because once we have reached k,
        // additional occurrences of value must continue returning true.
        return count >= k;
    }
}


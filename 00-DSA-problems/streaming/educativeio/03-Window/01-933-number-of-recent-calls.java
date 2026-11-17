/**
 * 0. ## Problem Statement
 * You have a RecentCounter class which counts the number of recent requests within a certain time frame.
 * 
 * Implement the RecentCounter class:
 * - RecentCounter() Initializes the counter with zero recent requests.
 * - int ping(int t) Adds a new request at time t (in milliseconds), and returns the number of 
 *   requests that have happened in the inclusive range [t - 3000, t].
 * 
 * It is guaranteed that every call to ping uses a strictly larger value of t than the previous call.
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can multiple requests arrive at the exact same millisecond?"
 *    -> The problem guarantees strictly increasing `t`, but asking this prevents bugs in a real-world scenario (where we might need a frequency map or allow duplicate timestamps in a queue).
 * 2. "Will this system run indefinitely, processing millions of pings?"
 *    -> Crucial for space complexity. If it runs forever, we absolutely cannot store the entire history of pings. We must eagerly evict stale data.
 * 3. "Is the 3000ms window fixed, or could it change per query?"
 *    -> If fixed, a simple Queue is perfect. If dynamic, we might need a binary searchable structure (like an ArrayList or segment tree) depending on eviction rules.
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: The system receives up to 10^4 pings (or practically infinite in the real world). 
 * If we scan the whole history for every ping, it's an O(N) operation per ping, which is too slow.
 * 
 * --- Approach 1: The Infinite History (Brute Force) ---
 * Idea: Keep all timestamps in an `ArrayList`. When `ping(t)` is called, append `t`. Then iterate 
 * backward from the end of the list until you hit a timestamp smaller than `t - 3000`. Count the steps.
 * - Why it works: Simulates the exact definition of the problem.
 * - Why it's too slow/costly: We store data we will never need again, and we iterate over potentially 
 *   thousands of elements per call if the pings are dense.
 * Time Complexity: O(N) per call — because we might scan back through N valid elements in a dense window.
 * Space Complexity: O(M) — where M is the TOTAL number of pings ever received. Causes Memory Leaks.
 * 
 * --- Approach 2: Binary Search over History ---
 * Idea: Since `t` is strictly increasing, the `ArrayList` is naturally sorted. We can binary search 
 * for the lower bound (`t - 3000`). The number of valid pings is `list.size() - foundIndex`.
 * - Why it works: Reduces the search time over a massive array.
 * - Why it's still flawed: It solves the time problem but fails the space problem. We are still storing 
 *   every ping forever.
 * Time Complexity: O(log M) per call — because binary search halves the search space of M total elements.
 * Space Complexity: O(M) — because we still store the entire history of the application.
 * 
 * --- Approach 3: Sliding Window / Queue (Optimal) ---
 * Idea: The chronological guarantee ("strictly increasing `t`") means the oldest pings are ALWAYS 
 * at the front. We can use a First-In-First-Out (FIFO) `Queue`. 
 * On `ping(t)`, enqueue `t`. Then, look at the front of the queue. If it is older than `t - 3000`, 
 * dequeue and discard it. Repeat until the front is within the 3000ms window. Return `queue.size()`.
 * - Why it works: It naturally drops stale data the exact moment it expires.
 * - What work is avoided: We never search through the data, and we never hold data longer than 3 seconds.
 * Time Complexity: Amortized O(1) per call — because while one `ping` might trigger multiple `poll()` calls, every ping is enqueued exactly once and dequeued at most once over its lifetime.
 * Space Complexity: O(W) — where W is the maximum number of pings that can fit in a 3000ms window. Much safer than O(M).
 * 
 * Verdict: I would immediately write Approach 3 (Queue). It perfectly exploits the chronological 
 * nature of the input, directly mirrors the sliding window requirement, and avoids memory leaks.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - The `t - 3000` evaluates to a negative number: e.g., `ping(1)`. Window is `[-2999, 1]`. 
 *   The queue handles this effortlessly since `peek()` (which is 1) is NOT less than -2999.
 * - Exact boundary condition: `queue.peek() == t - 3000`. The problem says inclusive `[t-3000, t]`. 
 *   We must NOT pop elements that are exactly 3000ms old. Use `<` strictly, not `<=`.
 * - Massive time gaps: e.g., `ping(1)`, then `ping(10000)`. The `while` loop will completely drain 
 *   the queue before adding the new ping. Handled gracefully.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * Whenever a problem deals with a "time window" and guarantees that events arrive in chronological order, 
 * a Queue is almost always the answer. The order of arrival perfectly matches the order of expiration.
 * 
 * -- ASCII Diagram (Queue Eviction) --
 * Window: 3000ms.
 * 
 * ping(1)    -> Queue: [1]                     -> return 1
 * ping(100)  -> Queue: [1, 100]                -> return 2
 * ping(3001) -> Queue: [1, 100, 3001]          -> return 3  (1 is exactly 3000ms old. INCLUSIVE. Keep it!)
 * ping(3002) -> (3002 - 3000 = 2). Threshold is 2.
 *               Is front (1) < 2? YES. -> POP!
 *               Is front (100) < 2? NO. -> STOP!
 *               Queue: [100, 3001, 3002]       -> return 3
 * 
 * -- Dry run --
 * Input: ping(1), ping(100), ping(3001), ping(3002)
 * 1. t = 1. Add 1. Front = 1. Threshold = 1 - 3000 = -2999. 
 *    1 < -2999 is False. Size = 1.
 * 2. t = 100. Add 100. Front = 1. Threshold = 100 - 3000 = -2900. 
 *    1 < -2900 is False. Size = 2.
 * 3. t = 3001. Add 3001. Front = 1. Threshold = 3001 - 3000 = 1. 
 *    1 < 1 is False (inclusive!). Size = 3.
 * 4. t = 3002. Add 3002. Front = 1. Threshold = 3002 - 3000 = 2. 
 *    1 < 2 is True. Pop 1.
 *    Front is now 100. 100 < 2 is False. Size = 3.
 * 
 * -- Pitfalls --
 * 1. Using `<=` instead of `<` in the eviction check. This drops the element sitting exactly on the boundary, resulting in an off-by-one error.
 * 2. Using an `ArrayList` and calling `list.remove(0)`. This causes an O(N) shift of all remaining elements every time an old ping expires, destroying the performance.
 * 
 * -- Pattern recognition --
 * When you see "Recent within X timeframe" + "Ordered by time", think **Queue (Sliding Window)**.
 * Transfers to: Logger Rate Limiter (LeetCode 359), Moving Average from Data Stream (LeetCode 346).
 * 
 * -- Interview script --
 * "Since the requests are guaranteed to arrive in chronological order, this is a classic sliding window problem. 
 * The oldest requests will always be the first to expire. A Queue models this perfectly. Whenever a new ping 
 * comes in, I'll enqueue it, and then I'll look at the front of the queue. If the oldest ping is less than 
 * `t - 3000`, I dequeue it. I keep doing this until the front is within the 3000ms window. Then I just 
 * return the size of the queue. This gives us Amortized O(1) time and bounds our memory to just the active window."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if the timestamps can arrive out of order (e.g., due to network lag)?
 * A: A simple Queue breaks because an older timestamp might arrive late and get stuck behind newer ones, 
 *    ruining the FIFO eviction. We would need a Min-Heap (PriorityQueue) or a balanced BST (TreeMap) 
 *    to keep elements sorted by time, changing our eviction time to O(log W).
 * 
 * Q: What if the range wasn't fixed at 3000ms, but passed in as an argument `ping(t, window)`?
 * A: We could no longer safely evict elements! If `ping(5000, 10)` evicts everything before 4990, 
 *    and the next call is `ping(5001, 10000)`, we've lost the historical data needed for the wider window.
 *    We would have to keep all data (Approach 2 with Binary Search).
 * =========================================================================================
 */

import java.util.LinkedList;
import java.util.Queue;

public class NumberOfRecentCallsStudyNote {

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH: Queue / Sliding Window
    // ----------------------------------------------------------------------
    static class RecentCounter {
        
        // The queue will strictly hold the timestamps that fall within the current 3000ms window.
        // We use LinkedList as it implements Queue and allows O(1) append and removal from the ends.
        private final Queue<Integer> activePings;

        public RecentCounter() {
            activePings = new LinkedList<>();
        }
        
        public int ping(int t) {
            // 1. Add the current ping to the end of our chronological queue.
            activePings.offer(t);
            
            // 2. Evict stale pings from the front of the queue.
            // The problem states the range is INCLUSIVE: [t - 3000, t].
            // Therefore, a ping is only stale if it is STRICTLY LESS than t - 3000.
            // (e.g., if t=3001, threshold is 1. If queue.peek() == 1, 1 < 1 is false. It stays.)
            while (!activePings.isEmpty() && activePings.peek() < t - 3000) {
                activePings.poll();
            }
            
            // 3. The queue now contains EXACTLY the pings in the 3000ms window.
            // Returning the size is an O(1) operation.
            return activePings.size();
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        RecentCounter counter = new RecentCounter();
        
        System.out.println("ping(1)    -> " + counter.ping(1) + " (Expected: 1)");
        System.out.println("ping(100)  -> " + counter.ping(100) + " (Expected: 2)");
        
        // 3001 - 3000 = 1. Since range is inclusive, '1' is still valid!
        System.out.println("ping(3001) -> " + counter.ping(3001) + " (Expected: 3)");
        
        // 3002 - 3000 = 2. '1' is less than 2, so it expires and is popped.
        System.out.println("ping(3002) -> " + counter.ping(3002) + " (Expected: 3, since 1 dropped out)");
        
        // 10000 - 3000 = 7000. All previous pings (100, 3001, 3002) expire.
        System.out.println("ping(10000)-> " + counter.ping(10000) + " (Expected: 1, completely fresh window)");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Queue / Sliding Window over Time.
 * - Key observation: Chronological input guarantees that the First-In (oldest ping) is always 
 *   the First-Out (first to expire). A Queue perfectly mirrors this real-world flow.
 * - Worth memorizing: The eviction loop: `while(peek() < t - window) poll();`. 
 * - Most common trap: Using `<=` instead of `<` and accidentally evicting a valid boundary element.
 * - One-line mental trigger: "Stream of chronologically expiring events? -> Push, Peek, Pop, Size."
 * =========================================================================================
 */









































import java.util.ArrayDeque;
import java.util.Deque;

class RecentCounter {

    private final Deque<Integer> timestamps = new ArrayDeque<>();

    public RecentCounter() {
        // ArrayDeque is used as a FIFO queue.
    }

    public int ping(int t) {

        // Add the newest request at the rear.
        timestamps.addLast(t);

        // Calculate the earliest valid timestamp.
        int boundary = t - 3000;

        // Remove expired timestamps from the front.
        while (!timestamps.isEmpty()
                && timestamps.peekFirst() < boundary) {

            timestamps.removeFirst();
        }

        // All remaining timestamps are valid.
        return timestamps.size();
    }
}

/*
TIME COMPLEXITY:
Constructor: O(1)
ping(): Amortized O(1).
Worst-case individual ping(): O(N).

SPACE COMPLEXITY:
O(W), where W is the maximum number of requests
within a 3000-millisecond window.
*/











































class RecentCounter {

    // The sliding window [t - 3000, t] contains 3001
    // possible integer timestamps.
    private static final int WINDOW_SIZE = 3001;

    // window[i] = number of requests at the timestamp
    // currently mapped to circular index i.
    private final int[] window;

    // timestamp[i] = actual timestamp represented by index i.
    // Needed because multiple timestamps can map to the same index.
    private final int[] timestamp;

    // Total requests inside the current sliding window.
    private int count;

    public RecentCounter() {
        window = new int[WINDOW_SIZE];
        timestamp = new int[WINDOW_SIZE];
        count = 0;
    }

    public int ping(int t) {

        // Map timestamp to its circular-array position.
        int index = t % WINDOW_SIZE;

        // Case 1: The slot already represents timestamp t.
        // (Useful if multiple requests share the same timestamp.)
        if (timestamp[index] == t) {

            window[index]++;
            count++;

        } else {

            // Case 2: The slot represents an older timestamp.
            //
            // Because requests arrive in strictly increasing order,
            // the old timestamp must be outside [t - 3000, t].
            //
            // Remove its contribution from the running count.
            count -= window[index];

            // Reuse this circular-array slot for timestamp t.
            window[index] = 1;
            timestamp[index] = t;

            // Include the newly arrived request.
            count++;
        }

        return count;
    }
}


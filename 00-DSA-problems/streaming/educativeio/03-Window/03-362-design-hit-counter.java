/**
 * 0. ## Problem Statement
 * Design a hit counter which counts the number of hits received in the past 5 minutes 
 * (i.e., the past 300 seconds).
 * 
 * Your system should accept a timestamp parameter (in seconds), and you may assume 
 * that calls are being made to the system in chronological order (i.e., timestamp is monotonically increasing). 
 * Several hits may arrive at the same timestamp.
 * 
 * Implement the `HitCounter` class:
 * - `HitCounter()`: Initializes the object of the hit counter system.
 * - `void hit(int timestamp)`: Records a hit at `timestamp` (in seconds).
 * - `int getHits(int timestamp)`: Returns the number of hits in the past 5 minutes 
 *   from `timestamp` (i.e., in the range `(timestamp - 300, timestamp]`).
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can multiple hits arrive at the exact same timestamp?"
 *    -> Yes. This dictates whether we can just push distinct timestamps or if we need to track frequencies.
 * 2. "What is the expected scale of hits? Are we talking 10 hits per second or 10 million?"
 *    -> Crucial! If it's millions, a simple Queue storing every hit will trigger an OutOfMemoryError. 
 *       We must optimize space to be independent of the traffic volume.
 * 3. "Are the queries to `getHits()` also guaranteed to be monotonically increasing?"
 *    -> Generally yes in this problem, but if they aren't, the Queue approach might break if it aggressively 
 *       deletes old data that a subsequent, older `getHits` query might need.
 * 4. "Do we need to worry about concurrent thread access?"
 *    -> In a real system design interview, absolutely. We'd need locks or atomic arrays. For the basic algorithm, no.
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: A hit counter needs to be extremely fast and memory-efficient. A 5-minute 
 * window means we only care about the last 300 seconds. If traffic spikes to 1,000,000 requests/second, 
 * O(N) memory where N is the number of hits will crash the server.
 * 
 * --- Approach 1: The Intuitive Queue ---
 * Idea: Store every single hit's timestamp in a `Queue`. 
 * On `hit(t)`, enqueue `t`. 
 * On `getHits(t)`, peek at the front of the queue. While the front is `<= t - 300`, dequeue it. 
 * Then return the queue's size.
 * - Why it works: Naturally slides the window forward, exactly like the "Number of Recent Calls" problem.
 * - Why it's too costly: If we get 10 million hits in one second, we push 10 million integers to the 
 *   queue. Memory scales with the *volume* of traffic, not the time window.
 * Time Complexity: `hit`: O(1). `getHits`: Amortized O(1) per call — because each hit is enqueued and dequeued exactly once.
 * Space Complexity: O(N) — because we store every single hit in the 300-second window, where N can be massive.
 * 
 * --- Approach 2: Queue with Frequency Pairs ---
 * Idea: Instead of pushing 10 million separate integers, push a `[timestamp, count]` pair. If the 
 * new hit matches the timestamp at the back of the queue, just increment the count.
 * - Why it works: Compresses the data. We only ever store at most 300 nodes in the queue.
 * - Why it's still slightly flawed: It requires extra object allocations (Nodes/Arrays) and iterating 
 *   through the queue to sum up the counts on every `getHits` call.
 * Time Complexity: `hit`: O(1). `getHits`: O(300) = O(1) — because we sum at most 300 nodes.
 * Space Complexity: O(300) = O(1) — bounded by the number of seconds in 5 minutes.
 * 
 * --- Approach 3: Circular Array / Buckets (Optimal) ---
 * Idea: We only care about 300 distinct seconds. Let's create two fixed-size arrays of size 300: 
 * `times[300]` and `hits[300]`. 
 * When a hit arrives at `t`, we find its bucket using `idx = t % 300`. 
 * If `times[idx] == t`, we are in the same second, so `hits[idx]++`. 
 * If `times[idx] != t`, this bucket is from 5+ minutes ago! We overwrite it: `times[idx] = t`, `hits[idx] = 1`.
 * On `getHits(t)`, we loop through all 300 buckets. If `t - times[i] < 300`, we add `hits[i]` to our total.
 * - Why it works: It completely decouples memory from traffic volume using modulo arithmetic.
 * - What work is avoided: Zero object allocations after initialization. No queue nodes, no resizing.
 * Time Complexity: `hit`: O(1) — array indexing is constant. `getHits`: O(300) = O(1) — because we always loop exactly 300 times, which is a constant.
 * Space Complexity: O(300) = O(1) — because we allocate exactly two integer arrays of size 300, taking mere bytes regardless of traffic.
 * 
 * Verdict: I would explain Approach 1 (Queue) quickly to show I understand sliding windows, but I would 
 * implement Approach 3 (Buckets) because the "massive scale" follow-up is universally asked for this problem. 
 * Approach 3 preempts the follow-up and shows deep system design intuition.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Exact 300-second boundary: Hit at `t=1`. `getHits` at `t=300`. 300 - 1 = 299 < 300 (Valid, included). 
 *   `getHits` at `t=301`. 301 - 1 = 300 < 300 is FALSE. (Correctly excluded).
 * - Massive time gaps: Hit at `t=1`. Next hit at `t=1000`. The modulo arithmetic maps 1000 to 
 *   bucket 100. It sees `times[100] == 1 != 1000`, so it safely wipes the old data and starts fresh.
 * - Multiple hits same timestamp: Handled gracefully by `hits[idx]++`.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * When tracking time-series data over a FIXED rolling window with granularity (e.g., per second), 
 * modulo arithmetic over fixed-size arrays is vastly superior to dynamic queues.
 * 
 * -- ASCII Diagram (Circular Array / Buckets) --
 * Window: 5 seconds (for easy drawing), Buckets = 5.
 * 
 * Hit at t=1, t=2, t=2, t=3:
 * Index:     [0]   [1]   [2]   [3]   [4]
 * Times:     [0]   [1]   [2]   [3]   [0]
 * Hits:      [0]   [1]   [2]   [1]   [0]
 * 
 * Now, Hit at t=6:
 * idx = 6 % 5 = 1. 
 * times[1] is currently 1. 6 != 1, so overwrite!
 * Index:     [0]   [1]   [2]   [3]   [4]
 * Times:     [0]   [6]   [2]   [3]   [0]
 * Hits:      [0]   [1]   [2]   [1]   [0]
 * 
 * getHits(6): Check all buckets. 
 * times[1]=6 (6-6<5 -> Add 1). times[2]=2 (6-2<5 -> Add 2). times[3]=3 (6-3<5 -> Add 1). Total = 4.
 * 
 * -- Dry run (Buckets Approach) --
 * window = 300.
 * hit(1), hit(2), hit(300), getHits(300), getHits(301)
 * 1. hit(1): idx = 1%300 = 1. times[1]=1, hits[1]=1.
 * 2. hit(2): idx = 2%300 = 2. times[2]=2, hits[2]=1.
 * 3. hit(300): idx = 300%300 = 0. times[0]=300, hits[0]=1.
 * 4. getHits(300):
 *    - loop i=0..299:
 *    - i=0: t=300. 300-300 = 0 < 300 (YES -> sum += 1)
 *    - i=1: t=1. 300-1 = 299 < 300 (YES -> sum += 1)
 *    - i=2: t=2. 300-2 = 298 < 300 (YES -> sum += 1)
 *    - returns 3.
 * 5. getHits(301):
 *    - i=0: t=300. 301-300 = 1 < 300 (YES -> sum += 1)
 *    - i=1: t=1. 301-1 = 300 < 300 (NO! Excluded)
 *    - i=2: t=2. 301-2 = 299 < 300 (YES -> sum += 1)
 *    - returns 2.
 * 
 * -- Pitfalls --
 * 1. Summing the entire `hits` array blindly in `getHits()`. You MUST verify `timestamp - times[i] < 300` 
 *    first, because the array might still contain data from 20 minutes ago that hasn't been overwritten yet.
 * 2. Not scaling. If an interviewer asks "what if there are millions of hits", and you only have the Queue 
 *    approach, you fail the system design aspect of the question.
 * 
 * -- Pattern recognition --
 * When you see "fixed time window" and "massive volume", think **Circular Array / Buckets**.
 * 
 * -- Interview script --
 * "A standard queue works, popping old timestamps when `getHits` is called. But if we receive a million 
 * hits a second, our queue consumes massive memory. Since the window is strictly 300 seconds, we only 
 * care about 300 distinct buckets of time. I can use an array of size 300 mapped by `timestamp % 300`. 
 * If the timestamp matches the bucket's timestamp, I increment. If it's different, it means 5 minutes have 
 * passed, so I overwrite it. On read, I just sum the valid buckets. This reduces space from O(Hits) to O(1)."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if the number of hits per second could be huge? Does your design scale?
 * A: Yes! The Circular Array approach perfectly handles huge traffic. Whether we get 1 hit or 10 billion hits 
 *    at `t=5`, we only update `hits[5]++`. The memory stays exactly the same (two 300-length arrays).
 * 
 * Q: What if `hit()` and `getHits()` are called concurrently by multiple threads?
 * A: We would have a race condition on `hits[idx]++`. We could fix this by making `hits` an 
 *    `AtomicIntegerArray` and doing Compare-And-Swap (CAS) operations, or by using a `ReadWriteLock` 
 *    around the bucket updates and reads.
 * 
 * Q: What if we needed to track hits per minute, per hour, and per day simultaneously?
 * A: We would use a multi-tiered architecture (like a Time-Series Database). We roll up data: the 
 *    second-level buckets flush into minute-level buckets, which flush into hour-level buckets, etc.
 * =========================================================================================
 */

import java.util.LinkedList;
import java.util.Queue;

public class HitCounterStudyNote {

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH: Circular Array / Buckets (O(1) Time, O(1) Space)
    // ----------------------------------------------------------------------
    static class HitCounter {
        // We only care about the last 300 seconds.
        private final int[] times;
        private final int[] hits;

        public HitCounter() {
            times = new int[300];
            hits = new int[300];
        }

        public void hit(int timestamp) {
            // Find which of the 300 buckets this timestamp maps to
            int idx = timestamp % 300;
            
            // If the bucket is currently holding data for this EXACT second, 
            // we just increment the hit count.
            if (times[idx] == timestamp) {
                hits[idx]++;
            } 
            // Otherwise, this bucket is holding stale data from 5+ minutes ago.
            // We overwrite it with the new timestamp and reset the count to 1.
            else {
                times[idx] = timestamp;
                hits[idx] = 1;
            }
        }

        public int getHits(int timestamp) {
            int totalHits = 0;
            
            // Look at every single bucket
            for (int i = 0; i < 300; i++) {
                // We MUST check if the timestamp stored in the bucket is actually 
                // within the last 300 seconds. If a bucket hasn't been overwritten 
                // recently, it might hold data from an hour ago.
                if (timestamp - times[i] < 300) {
                    totalHits += hits[i];
                }
            }
            
            return totalHits;
        }
    }

    // ----------------------------------------------------------------------
    // NAIVE APPROACH: The Queue (O(1) Amortized Time, O(N) Space)
    // Included to show the logical bridge. Do not use if scale is huge.
    // ----------------------------------------------------------------------
    static class HitCounterQueue {
        private final Queue<Integer> queue;

        public HitCounterQueue() {
            queue = new LinkedList<>();
        }

        public void hit(int timestamp) {
            queue.offer(timestamp);
        }

        public int getHits(int timestamp) {
            // Evict hits older than 300 seconds
            while (!queue.isEmpty() && timestamp - queue.peek() >= 300) {
                queue.poll();
            }
            return queue.size();
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("Testing Optimal Bucket Approach:");
        HitCounter counter = new HitCounter();
        
        counter.hit(1);
        counter.hit(2);
        counter.hit(3);
        
        System.out.println("getHits(4) -> " + counter.getHits(4) + " (Expected: 3)");
        
        counter.hit(300);
        System.out.println("getHits(300) -> " + counter.getHits(300) + " (Expected: 4)");
        
        // At t=301, the hit at t=1 expires.
        System.out.println("getHits(301) -> " + counter.getHits(301) + " (Expected: 3, hit at t=1 dropped)");
        
        // Massive gap testing
        counter.hit(1000);
        // At t=1000, hits at 2, 3, 300 all expire. Only t=1000 remains.
        System.out.println("getHits(1000) -> " + counter.getHits(1000) + " (Expected: 1)");

        System.out.println("\nTesting Massive Concurrent Hits at same timestamp:");
        HitCounter massiveCounter = new HitCounter();
        for (int i = 0; i < 1000000; i++) {
            massiveCounter.hit(150);
        }
        System.out.println("getHits(150) -> " + massiveCounter.getHits(150) + " (Expected: 1000000)");
        // Memory used? Exactly two 300-length arrays. The Queue approach would have instantiated 1,000,000 Node objects!
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Fixed Sliding Window / Circular Array (Buckets).
 * - Key observation: When the time window is fixed (300 seconds), memory should map to the 
 *   time boundaries, not the volume of events. 
 * - Worth memorizing: `idx = timestamp % 300`. This simple modulo perfectly maps an infinite 
 *   chronological timeline into a finite circular buffer.
 * - Most common trap: Forgetting to validate `timestamp - times[i] < 300` in the `getHits` loop. 
 *   Just because data is in the array doesn't mean it's fresh; it might be stale data that 
 *   hasn't been overwritten yet.
 * - One-line mental trigger: "Fixed time window + massive traffic? -> Modulo Buckets."
 * =========================================================================================
 */










































import java.util.*;

class HitCounterQueue {

    // Queue stores every individual hit in chronological order.
    //
    // ArrayDeque is preferred over LinkedList for queue operations
    // because it provides efficient operations at both ends.
    private final Deque<Integer> hits = new ArrayDeque<>();

    public void hit(int timestamp) {

        // Add the new hit at the rear.
        //
        // Since timestamps arrive in non-decreasing order,
        // the queue remains chronologically sorted.
        hits.offerLast(timestamp);
    }

    public int getHits(int timestamp) {

        // Current valid window:
        // [timestamp - 299, timestamp]
        //
        // A hit expires when:
        // timestamp - hitTime >= 300.
        //
        // Equivalently:
        // hitTime <= timestamp - 300.
        //
        // Therefore, remove all expired hits from the front.

        while (!hits.isEmpty()
                && timestamp - hits.peekFirst() >= 300) {

            // The oldest hit has expired.
            // Remove it permanently because timestamps are ordered.
            hits.pollFirst();
        }

        // Every remaining hit belongs to the current window.
        return hits.size();
    }

    /*
     * COMPLEXITY:
     *
     * hit():
     *   Time: O(1).
     *   Adding an element to the rear of ArrayDeque is O(1)
     *   amortized.
     *
     * getHits():
     *   Time: O(1) amortized.
     *
     *   Important distinction:
     *   A single getHits() call might remove many expired hits,
     *   making that individual call O(H).
     *
     *   However, every hit is inserted exactly once and removed
     *   at most once.
     *
     *   Across N operations, the total number of removals cannot
     *   exceed the total number of insertions.
     *
     *   Therefore, the amortized cost per operation is O(1).
     *
     * Space: O(H).
     *
     *   In the worst case, many hits can occur within 300 seconds.
     *   We store every individual hit that has not yet expired.
     *
     * ADVANTAGES:
     *   - No scanning of the 300-second interval.
     *   - Expired hits are removed efficiently.
     *   - Supports multiple hits at the same timestamp.
     *
     * IMPORTANT ASSUMPTION:
     *   Hit timestamps and query timestamps must follow
     *   non-decreasing chronological order.
     */
}








































import java.util.Arrays;

class HitCounterCircularArray {

    private static final int WINDOW = 300;

    // Each index represents one second in the circular window.
    private final int[] times = new int[WINDOW];

    // Number of hits recorded at the corresponding timestamp.
    private final int[] hits = new int[WINDOW];

    public HitCounterCircularArray() {

        // Initialize every slot with -1.
        //
        // This distinguishes an unused slot from a valid timestamp.
        Arrays.fill(times, -1);
    }

    public void hit(int timestamp) {

        // Map the timestamp to one of the 300 circular slots.
        int index = timestamp % WINDOW;

        // Check whether this slot belongs to the current timestamp.
        //
        // If not, it contains either:
        // 1. An unused slot, or
        // 2. An older timestamp that has expired.
        //
        // Reset the slot before recording the new timestamp.
        if (times[index] != timestamp) {

            // Associate this slot with the new timestamp.
            times[index] = timestamp;

            // Reset the previous timestamp's hit count.
            hits[index] = 0;
        }

        // Record this hit.
        //
        // If multiple hits occur at the same timestamp,
        // the count is incremented instead of overwritten.
        hits[index]++;
    }

    public int getHits(int timestamp) {

        // Current valid interval:
        // [timestamp - 299, timestamp]
        int lowerBound = timestamp - WINDOW + 1;

        int totalHits = 0;

        // Inspect all 300 circular slots.
        for (int i = 0; i < WINDOW; i++) {

            // A slot contributes only when its actual timestamp
            // belongs to the current 300-second window.
            //
            // Checking the actual timestamp is essential:
            // index equality alone cannot distinguish timestamps
            // separated by multiples of 300.
            if (times[i] >= lowerBound
                    && times[i] <= timestamp) {

                totalHits += hits[i];
            }
        }

        return totalHits;
    }

    /*
     * COMPLEXITY:
     *
     * hit():
     *   Time: O(1).
     *   One modulo operation, one slot validation, and one increment.
     *
     * getHits():
     *   Time: O(300) = O(1).
     *   We inspect exactly 300 array positions.
     *
     * Space: O(300) = O(1).
     *   Two arrays of fixed size 300.
     *
     * ADVANTAGES:
     *   - Constant-time hit recording.
     *   - Constant-time hit counting.
     *   - Fixed memory regardless of the total number of hits.
     *   - No queue or dynamic collection is required.
     *
     * IMPORTANT:
     *   This implementation assumes timestamps arrive in
     *   non-decreasing order and queries follow chronological order.
     *
     * WHY DOES CIRCULAR OVERWRITING WORK?
     *
     *   Two timestamps map to the same index only when their
     *   difference is a multiple of 300.
     *
     *   For a monotonically advancing stream, an older timestamp
     *   occupying that slot has already expired when a newer
     *   timestamp 300 seconds later replaces it.
     *
     *   The times[] array prevents stale values from being
     *   mistakenly counted during getHits().
     */
}



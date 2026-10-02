/**
 * ============================================================================
 * L O G G E R   R A T E   L I M I T E R
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "If a message is rejected (not displayed), does it still reset the S-second timer?"
 *    (Crucial for correctness: Usually no. Only successfully displayed messages reset the clock.
 *     If I assume yes, a spammer sending every 1 second could suppress a message forever.)
 * 2. "Are timestamps strictly monotonically increasing, or can logs arrive out of order?"
 *    (The constraint says 'ascending order', which allows us to use sliding-window/queue 
 *     optimizations. If out-of-order, queue-based eviction breaks.)
 * 3. "Is this a long-running system (e.g., a server running for months)?"
 *    (Changes the optimal approach. A standard HashMap will grow infinitely and cause an 
 *     OutOfMemoryError if unique messages are unbounded.)
 * 4. "Can multiple identical messages arrive at the exact same timestamp?"
 *    (Yes, the prompt says 'different timestamps' but also implies identical messages can 
 *     happen. We just process them sequentially as if they arrived in the given order).
 * 5. "What is the value of S?"
 *    (I will design the system to take 'S' as a configurable parameter, rather than hardcoding.)
 *
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need to instantly know the *last time a specific string was printed*. 
 * > The bottleneck is searching through history.
 *
 * ### Approach 1: The Infinite Ledger (Brute Force)
 * 1. What I'd naturally try: Keep a `List` of all successfully printed `(timestamp, message)` pairs.
 * 2. Why it works: To check a new message, I just iterate backward through the list until I 
 *    find the message or hit a timestamp older than `current - S`.
 * 3. Why it's too costly: 
 *    - Time Complexity: O(N) per request — because in the worst case (a brand new message), 
 *      we scan backwards through the entire history of printed messages.
 *    - Space Complexity: O(N) — because we store every single printed message forever.
 * 4. What work is being repeated: We are linearly scanning past thousands of unrelated messages 
 *    just to find the one we care about. 
 * 5. What property removes the bottleneck: We only care about the *latest* timestamp for 
 *    a specific string. This screams for a Key-Value lookup.
 *
 * ### Approach 2: The Timestamp Dictionary (HashMap)
 * 1. What I'd naturally try: Use a `HashMap<String, Integer>` storing `message -> lastPrintedTime`.
 * 2. Why it works: Instant lookup. `if (timestamp - map.get(message) >= S)`, we update and print.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(1) — because HashMap lookups and updates are constant time.
 *    - Space Complexity: O(U) where U is the number of UNIQUE messages — because we store one 
 *      integer per unique string.
 * 4. The hidden flaw: Space is O(U), but if the system runs for 5 years generating unique error 
 *    IDs, the HashMap never deletes old entries. It's a massive memory leak.
 * 5. What property removes the bottleneck: Because timestamps arrive strictly in ascending order, 
 *    any message printed more than `S` seconds ago is completely irrelevant to the system state today.
 *
 * ### Approach 3: The Sliding Window (Queue + HashSet)
 * 1. What I'd naturally try: Maintain a Queue of printed messages to track time-order, and a 
 *    HashSet for O(1) lookups.
 * 2. Why it works: When a new log arrives at time `T`, we pop elements off the front of the Queue 
 *    until the front is >= `T - S`. We also remove them from the HashSet.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(1) amortized — because while the `while` loop might evict many elements 
 *      at once, every message is added and removed from the queue exactly once.
 *    - Space Complexity: O(W) where W is the max number of unique messages strictly within an 
 *      `S`-second window — because we aggressively delete historical data. No memory leaks!
 *
 * > **Interview Strategy:** In a 45-minute interview, I would confidently write Approach 2 (HashMap) 
 * > first because it's 10 lines of foolproof code. I would *verbally* point out the memory leak, 
 * > and if the interviewer says "How do we fix the memory leak?", I immediately write Approach 3.
 *
 * 
 * ## 3. Edge cases
 * - Empty string message: perfectly valid, handle like any other string.
 * - Same timestamp, same message: First one prints, second one gets blocked (time diff = 0 < S).
 * - Massive time jumps: e.g., t=1 then t=10000. The Queue approach must efficiently clear out 
 *   everything without breaking.
 *
 * 
 * ## 4. Key Insight & Dry Run
 * **Key Insight:** "A rejected message does NOT reset the timer."
 * If we reject a message, we must *not* add it to our Queue or update our HashMap. 
 * If we do, a spammer sending a message every 1 second (where S=10) would result in the message 
 * being printed at t=1, and then blocked forever, because the timer keeps resetting.
 *
 * **Dry Run of Approach 3 (Queue+Set):** S = 10
 * 1. [t=1, "foo"] -> Queue is empty. Print. 
 *    State: Queue=[(1,"foo")], Set={"foo"}
 * 2. [t=5, "foo"] -> "foo" is in Set. Reject. (Do NOT enqueue!).
 *    State: Queue=[(1,"foo")], Set={"foo"}
 * 3. [t=11, "bar"]-> Clean Queue: 11 - 1 >= 10. Evict (1,"foo"). Set remove "foo".
 *    "bar" not in Set. Print.
 *    State: Queue=[(11,"bar")], Set={"bar"}
 * 4. [t=12, "foo"]-> Clean Queue: 12 - 11 = 1 < 10. Stop evicting.
 *    "foo" not in Set. Print.
 *    State: Queue=[(11,"bar"), (12,"foo")], Set={"bar", "foo"}
 *
 * 
 * ## 5. Follow-ups
 * - **Q:** What if timestamps can arrive out of order (e.g., delayed packets in a distributed system)?
 *   **A:** Approach 3 (Queue) breaks entirely because we rely on the queue being strictly sorted by time 
 *   to safely evict. We would have to fall back to Approach 2 (HashMap), adding a check to ensure 
 *   we don't overwrite a newer timestamp with an older delayed one.
 * - **Q:** What if memory is extremely tight, even O(W) is too big?
 *   **A:** We could use a Bloom Filter synced to time intervals, though it introduces false positives 
 *   (accidentally blocking a valid log). Alternatively, we hash the strings (e.g., MD5 or SHA-1) 
 *   so we only store 16-byte hashes instead of variable-length strings.
 *
 * ============================================================================
 */

import java.util.*;

public class LoggerRateLimiterStudy {

    /**
     * Common interface so we can test all approaches easily.
     */
    interface Logger {
        boolean shouldPrintMessage(int timestamp, String message);
    }

    // ========================================================================
    // APPROACH 3: SLIDING WINDOW (Optimal for Space & Long-running systems)
    // ========================================================================
    static class SlidingWindowLogger implements Logger {
        
        // A simple record (or class) to hold pairs in the queue.
        // E.g., represents the event: "Message 'error' successfully printed at time 5"
        private static class Log {
            int time;
            String msg;
            Log(int time, String msg) { this.time = time; this.msg = msg; }
        }

        private final int timeLimit;
        
        // Tracks the time-order of successfully printed messages.
        // Front of queue is the oldest message.
        private final Queue<Log> queue;
        
        // Provides O(1) lookups to check if a message is currently in the active window.
        // E.g., if "db_timeout" is in this set, it means it was printed within the last S seconds.
        private final Set<String> printedInWindow;

        public SlidingWindowLogger(int timeLimit) {
            this.timeLimit = timeLimit;
            this.queue = new LinkedList<>();
            this.printedInWindow = new HashSet<>();
        }

        @Override
        public boolean shouldPrintMessage(int timestamp, String message) {
            // 1. Eviction Phase: Clean up messages older than the time limit.
            // Why? If timestamp is 15 and limit is 10, anything at time 5 or older is expired.
            while (!queue.isEmpty() && timestamp - queue.peek().time >= timeLimit) {
                // Remove the oldest log from the queue, and consequently its string from the set.
                Log oldest = queue.poll();
                printedInWindow.remove(oldest.msg);
            }

            // 2. Decision Phase: Is it in the active window?
            if (printedInWindow.contains(message)) {
                return false; // It was printed recently. Reject.
            }

            // 3. Update Phase: It wasn't in the window, so we accept and print it.
            // Notice we ONLY add to queue/set if we actually decide to print it.
            queue.offer(new Log(timestamp, message));
            printedInWindow.add(message);
            
            return true;
        }
    }

    // ========================================================================
    // APPROACH 2: HASH MAP (Optimal for Code Simplicity in Interviews)
    // ========================================================================
    static class HashMapLogger implements Logger {
        
        private final int timeLimit;
        
        // Maps a message to the EXACT timestamp it was last successfully printed.
        // E.g., {"foo" -> 1, "bar" -> 11}
        private final Map<String, Integer> messageToLastPrintTime;

        public HashMapLogger(int timeLimit) {
            this.timeLimit = timeLimit;
            this.messageToLastPrintTime = new HashMap<>();
        }

        @Override
        public boolean shouldPrintMessage(int timestamp, String message) {
            // 1. Check if we've ever seen it, and if so, was it recent?
            if (messageToLastPrintTime.containsKey(message)) {
                int lastPrinted = messageToLastPrintTime.get(message);
                
                if (timestamp - lastPrinted < timeLimit) {
                    // Too soon! Reject.
                    // CRITICAL PITFALL AVOIDED: Do NOT update the map here!
                    return false;
                }
            }
            
            // 2. It's either a brand new message, or an old one past the time limit.
            // Update the map with the NEW successful print time.
            messageToLastPrintTime.put(message, timestamp);
            return true;
        }
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (For demonstrating the initial journey)
    // ========================================================================
    static class BruteForceLogger implements Logger {
        
        private static class Log {
            int time;
            String msg;
            Log(int time, String msg) { this.time = time; this.msg = msg; }
        }

        private final int timeLimit;
        private final List<Log> history; // The Infinite Ledger

        public BruteForceLogger(int timeLimit) {
            this.timeLimit = timeLimit;
            this.history = new ArrayList<>();
        }

        @Override
        public boolean shouldPrintMessage(int timestamp, String message) {
            // Scan backwards from the most recent.
            for (int i = history.size() - 1; i >= 0; i--) {
                Log pastLog = history.get(i);
                
                // If we've scanned so far back that the logs are outside the time limit, 
                // we can stop searching. It's safe to print.
                if (timestamp - pastLog.time >= timeLimit) {
                    break;
                }
                
                // If we find the same message within the time limit, reject.
                if (pastLog.msg.equals(message)) {
                    return false;
                }
            }
            
            // If we didn't return false, it's valid. Add to history.
            history.add(new Log(timestamp, message));
            return true;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        System.out.println("Testing Sliding Window Logger (Optimal Space):");
        runTests(new SlidingWindowLogger(10));
        
        System.out.println("\nTesting HashMap Logger (Optimal Simplicity):");
        runTests(new HashMapLogger(10));
        
        System.out.println("\nTesting Brute Force Logger (Historical Array):");
        runTests(new BruteForceLogger(10));
    }
    
    private static void runTests(Logger logger) {
        // Expected: true
        System.out.println("t=1, 'foo' -> " + logger.shouldPrintMessage(1, "foo"));
        
        // Expected: true (Different message, same time)
        System.out.println("t=2, 'bar' -> " + logger.shouldPrintMessage(2, "bar"));
        
        // Expected: false (Only 2 seconds passed for 'foo', needs 10)
        System.out.println("t=3, 'foo' -> " + logger.shouldPrintMessage(3, "foo"));
        
        // Expected: false (Only 6 seconds passed for 'bar', needs 10)
        System.out.println("t=8, 'bar' -> " + logger.shouldPrintMessage(8, "bar"));
        
        // Expected: false (Exact boundary testing: 10 - 1 = 9 < 10)
        System.out.println("t=10, 'foo' -> " + logger.shouldPrintMessage(10, "foo"));
        
        // Expected: true (Boundary passed: 11 - 1 = 10 >= 10)
        System.out.println("t=11, 'foo' -> " + logger.shouldPrintMessage(11, "foo"));
        
        // Edge Case: Same timestamp, multiple identical messages
        System.out.println("t=15, 'baz' -> " + logger.shouldPrintMessage(15, "baz")); // true
        System.out.println("t=15, 'baz' -> " + logger.shouldPrintMessage(15, "baz")); // false
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Dictionary/Hash Map for instantly resolving past state.
 * - Key observation: In a continuously running system, storing infinite history 
 *   causes memory leaks. Since logs are ordered chronologically, we only need to 
 *   remember the last S seconds.
 * - What's worth memorizing: The Queue + Set pattern. It's a foundational 
 *   technique for sliding window streaming problems.
 * - Most common trap: Updating the timestamp/history even when a message is 
 *   rejected. Only successfully processed items should update the system state!
 * - Mental trigger: "Time-limited uniqueness = Sliding Window Set."
 * ============================================================================
 */


import java.util.*;

/**
 * Logger Rate Limiter
 *
 * Core Idea:
 * - Maintain a map: message -> last printed timestamp
 * - For each request:
 *      If message not present OR enough time has passed → allow
 *      Else → reject
 */
public class LoggerRateLimiter {

    // Map to store last printed timestamp for each message
    private final Map<String, Integer> lastPrintedTime;

    // Time window S
    private final int window;

    public LoggerRateLimiter(int window) {
        this.window = window;
        this.lastPrintedTime = new HashMap<>();
    }

    /**
     * Returns TRUE if message should be printed, else FALSE
     */
    public boolean shouldPrintMessage(int timestamp, String message) {

        // Case 1: message never seen before
        if (!lastPrintedTime.containsKey(message)) {
            lastPrintedTime.put(message, timestamp);
            return true;
        }

        int lastTime = lastPrintedTime.get(message);

        // Case 2: check if enough time has passed
        if (timestamp - lastTime >= window) {
            lastPrintedTime.put(message, timestamp); // update timestamp
            return true;
        }

        // Case 3: duplicate within window
        return false;
    }
}


import java.util.*;

class RequestLogger {

    private final Map<String, Integer> map;
    private final int timeLimit;

    public RequestLogger(int timeLimit) {
        this.map = new HashMap<>();
        this.timeLimit = timeLimit;
    }

    /**
     * Returns TRUE if request should be allowed, else FALSE
     */
    public boolean messageRequestDecision(int timestamp, String request) {

        // Case 1: First time seeing this message
        if (!map.containsKey(request)) {
            map.put(request, timestamp);
            return true;
        }

        int lastTimeStamp = map.get(request);

        // Case 2: Check time window
        if (timestamp - lastTimeStamp >= timeLimit) {
            map.put(request, timestamp); // update timestamp
            return true;
        }

        // Case 3: Duplicate within window
        return false;
    }
}


/**
 * Optimized Logger with cleanup using Queue + Map
 */
public class LoggerRateLimiterOptimized {

    private final Map<String, Integer> map;
    private final Queue<Map.Entry<String, Integer>> queue;
    private final int window;

    public LoggerRateLimiterOptimized(int window) {
        this.window = window;
        this.map = new HashMap<>();
        this.queue = new ArrayDeque<>();
    }

    public boolean shouldPrintMessage(int timestamp, String message) {

        // 🧹 Step 1: Cleanup old messages (out of window)
        while (!queue.isEmpty() && timestamp - queue.peek().getValue() >= window) {
            var entry = queue.poll();
            map.remove(entry.getKey());
        }

        // Step 2: Check if message exists
        if (map.containsKey(message)) {
            return false;
        }

        // Step 3: Add new message
        map.put(message, timestamp);
        queue.offer(Map.entry(message, timestamp));

        return true;
    }
}


/**
 * 0. ## Problem Statement
 * Design a logger system that receives a stream of messages along with their timestamps.
 * Each unique message should only be printed at most every 10 seconds.
 * 
 * Implement the `Logger` class:
 * - `Logger()`: Initializes the logger object.
 * - `boolean shouldPrintMessage(int timestamp, String message)`: Returns `true` if the 
 *   message should be printed in the given timestamp, otherwise returns `false`.
 * 
 * Note: Timestamps are given in chronological order (monotonically non-decreasing).
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Are timestamps strictly increasing, or just monotonically non-decreasing?"
 *    -> Determines if multiple messages can arrive at the exact same timestamp. 
 *       (Standard LeetCode states monotonically non-decreasing, so duplicates at t=x are possible).
 * 2. "Does a rejected message reset the 10-second timer?"
 *    -> Crucial business logic. If 'foo' prints at t=1, and is rejected at t=5, can it print at t=11? 
 *       Yes. The timer is strictly based on the LAST PRINTED timestamp, not the last seen.
 * 3. "Is this a long-running system (like weeks or months)?"
 *    -> If yes, a simple HashMap will eventually run out of memory storing old, never-seen-again messages. 
 *       We'd need an eviction strategy (Garbage Collection).
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: O(1) time processing per message so the logger doesn't bottleneck the 
 * main application. 
 * 
 * --- Approach 1: The Infinite History (HashMap) ---
 * Idea: Map each `message` to the `timestamp` it was last *printed*. When a new message comes, 
 * check if it exists in the map. If it doesn't, or if `current_timestamp - last_printed >= 10`, 
 * update the map with the new timestamp and return `true`. Otherwise, return `false`.
 * - Why it works: Direct O(1) memory of the exact threshold for every message.
 * - Why it's costly (space): We never delete entries. A message printed once and never seen 
 *   again stays in the HashMap forever, causing a memory leak in a long-running system.
 * Time Complexity: O(1) per call — because HashMap get/put are constant time.
 * Space Complexity: O(U) — where U is the total number of UNIQUE messages seen over the entire lifetime of the application.
 * 
 * --- Approach 2: Sliding Window / Garbage Collection (Queue + HashSet) ---
 * Idea: Since timestamps arrive in chronological order, we only ever care about the history 
 * of the *last 10 seconds*. We can keep a `Queue` of `[message, printed_timestamp]` and a 
 * `HashSet` of currently locked messages. 
 * On every call, peek at the queue. If the oldest printed message is older than `current_timestamp - 10`, 
 * `poll()` it and remove it from the `HashSet`. Repeat until the queue only holds the last 10 seconds.
 * Then, if the new message is in the Set, return `false`. If not, add to Set, add to Queue, return `true`.
 * - Why it works: It leverages the chronological nature of the stream to naturally evict old data.
 * - What work is avoided: We bound our memory usage strictly to the active window.
 * Time Complexity: Amortized O(1) per call — a single call might trigger multiple pops if a lot of time has passed, but every printed message is enqueued and dequeued exactly once.
 * Space Complexity: O(W) — where W is the number of UNIQUE messages printed in any 10-second window. This is significantly smaller and safer than O(U).
 * 
 * Verdict: I would write Approach 1 (HashMap) if asked for the fastest possible whiteboard code. 
 * But I would immediately bring up the memory leak issue, and if the interviewer asks for a production-ready 
 * scalable solution, I would implement Approach 2 (Queue + Set). Here, both are incredibly useful to understand.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Exact boundary (t = 10): If last printed at t=1, printing at t=11 is allowed (11 - 1 >= 10).
 * - Identical timestamp: Same message arrives twice at t=1. First returns true, second returns false.
 * - Massive time gaps: Messages arrive at t=1, then nothing until t=10000. Approach 2 will flush 
 *   the entire queue instantly.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * **Rejections don't exist in state**: Do NOT update the timestamp in the HashMap or push to the Queue 
 * if you are returning `false`. You only track successful prints.
 * 
 * -- ASCII Diagram (Queue + Set GC Mechanism) --
 * Window = 10s.
 * 
 * 1. msg("A", t=1) -> Queue: [ (A, 1) ], Set: {A} -> TRUE
 * 2. msg("B", t=2) -> Queue: [ (A, 1), (B, 2) ], Set: {A, B} -> TRUE
 * 3. msg("A", t=5) -> In Set? Yes. Reject. -> FALSE
 * 4. msg("C", t=12)-> Check Queue. (A, 1) is expired (12 - 1 >= 10). POP (A) and remove from Set!
 *                     Queue: [ (B, 2) ], Set: {B}
 *                     Add (C, 12). 
 *                     Queue: [ (B, 2), (C, 12) ], Set: {B, C} -> TRUE
 * 
 * -- Dry run (The common trap) --
 * "What if I update the timestamp even on failure?"
 * - msg("error", t=1) -> prints (next allowed at t=11).
 * - msg("error", t=8) -> rejected. If you updated the map here to t=8, the next allowed becomes t=18!
 * - msg("error", t=12) -> rejected because 12 < 18. (BUG! It should have printed since 12 >= 11).
 * 
 * -- Pitfalls --
 * 1. The trap mentioned above (updating state on rejection).
 * 2. Not realizing the memory leak of a simple HashMap in system design interviews.
 * 
 * -- Pattern recognition --
 * When you see "expire after X seconds" and "timestamps are chronological", think **Queue + HashSet**.
 * Transfers to: Hit Counter (LeetCode 362), Number of Recent Calls (LeetCode 933).
 * 
 * -- Interview script --
 * "A simple HashMap storing the last printed timestamp works in O(1) time, but it leaks memory 
 * infinitely in a long-running system. Since timestamps arrive chronologically, we can cap our memory 
 * usage to just the last 10 seconds. I'll use a Queue to track what we printed and when, and a HashSet 
 * to answer 'is this currently locked?' in O(1). On every new message, I'll first clean up the Queue 
 * by popping anything older than 10 seconds and removing it from the Set. This guarantees our memory 
 * scales with the active throughput, not the historical lifetime of the application."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if timestamps are NOT guaranteed to be strictly chronological? (e.g., distributed system lag)
 * A: The Queue approach breaks because an old message arriving late would be stuck behind a newer message, 
 *    blocking eviction. We would have to revert to the HashMap approach, or use a `PriorityQueue` (Min-Heap) 
 *    based on timestamps, which degrades eviction performance from O(1) to O(log W).
 * 
 * Q: How would you scale this if the logger runs across multiple servers?
 * A: We can't rely on local Java memory. We'd use an in-memory datastore like Redis. We could use a 
 *    Redis `SETNX` (Set if Not eXists) with an automatic TTL (Time To Live) of 10 seconds. If `SETNX` 
 *    returns 1, print it. If 0, reject it. Redis handles the eviction automatically.
 * =========================================================================================
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class LoggerRateLimiterStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: HashMap (Best for quick LC acceptance, O(1) Time, O(U) Space)
    // ----------------------------------------------------------------------
    static class LoggerMap {
        
        // Maps a unique message string to the exact timestamp it was last PRINTED.
        private final Map<String, Integer> messageToLastPrintTime;

        public LoggerMap() {
            messageToLastPrintTime = new HashMap<>();
        }

        public boolean shouldPrintMessage(int timestamp, String message) {
            // If we've never printed it, or the time elapsed is at least 10 seconds
            if (!messageToLastPrintTime.containsKey(message) || 
                timestamp - messageToLastPrintTime.get(message) >= 10) {
                
                // CRITICAL: We ONLY update the map when we actually PRINT the message.
                messageToLastPrintTime.put(message, timestamp);
                return true;
            }
            
            // Rejections do not affect the stored timestamp
            return false;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Queue + Set GC (Best for Production/System Design, O(W) Space)
    // ----------------------------------------------------------------------
    static class LoggerQueue {
        
        // Helper object to tie a message string to the timestamp it was printed
        private static class Log {
            String msg;
            int timestamp;
            
            Log(String msg, int timestamp) {
                this.msg = msg;
                this.timestamp = timestamp;
            }
        }
        
        // Queue tracks chronological order of successful prints
        private final Queue<Log> recentLogs;
        
        // Set provides O(1) lookup to see if a message is currently locked
        private final Set<String> lockedMessages;

        public LoggerQueue() {
            recentLogs = new LinkedList<>();
            lockedMessages = new HashSet<>();
        }

        public boolean shouldPrintMessage(int timestamp, String message) {
            // 1. Garbage Collection (Eviction phase)
            // While the oldest printed log is 10 or more seconds in the past...
            while (!recentLogs.isEmpty() && timestamp - recentLogs.peek().timestamp >= 10) {
                // Remove it from the queue
                Log expiredLog = recentLogs.poll();
                // Unlock it so it can be printed again
                lockedMessages.remove(expiredLog.msg);
            }
            
            // 2. Evaluation phase
            if (lockedMessages.contains(message)) {
                // It's still in the active 10-second window
                return false;
            } else {
                // It's not locked. Lock it and record it.
                lockedMessages.add(message);
                recentLogs.offer(new Log(message, timestamp));
                return true;
            }
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("Testing HashMap Approach:");
        LoggerMap loggerMap = new LoggerMap();
        System.out.println("t=1,  'foo' -> " + loggerMap.shouldPrintMessage(1, "foo") + " (Expected: true)");
        System.out.println("t=2,  'bar' -> " + loggerMap.shouldPrintMessage(2, "bar") + " (Expected: true)");
        System.out.println("t=3,  'foo' -> " + loggerMap.shouldPrintMessage(3, "foo") + " (Expected: false)");
        System.out.println("t=8,  'bar' -> " + loggerMap.shouldPrintMessage(8, "bar") + " (Expected: false)");
        System.out.println("t=10, 'foo' -> " + loggerMap.shouldPrintMessage(10, "foo") + " (Expected: false, only 9s elapsed)");
        System.out.println("t=11, 'foo' -> " + loggerMap.shouldPrintMessage(11, "foo") + " (Expected: true)");

        System.out.println("\nTesting Queue+Set Approach:");
        LoggerQueue loggerQueue = new LoggerQueue();
        System.out.println("t=1,  'foo' -> " + loggerQueue.shouldPrintMessage(1, "foo") + " (Expected: true)");
        System.out.println("t=2,  'bar' -> " + loggerQueue.shouldPrintMessage(2, "bar") + " (Expected: true)");
        System.out.println("t=3,  'foo' -> " + loggerQueue.shouldPrintMessage(3, "foo") + " (Expected: false)");
        System.out.println("t=8,  'bar' -> " + loggerQueue.shouldPrintMessage(8, "bar") + " (Expected: false)");
        System.out.println("t=10, 'foo' -> " + loggerQueue.shouldPrintMessage(10, "foo") + " (Expected: false, only 9s elapsed)");
        
        // Under the hood here, t=11 causes 'foo' at t=1 to be evicted from the queue and set
        System.out.println("t=11, 'foo' -> " + loggerQueue.shouldPrintMessage(11, "foo") + " (Expected: true)");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Rate Limiting / Sliding Window.
 * - Key observation: You only update the state when an action is SUCCESSFUL. Rejections are stateless.
 * - Worth memorizing: The trade-off between the infinite memory growth of `HashMap` and the naturally 
 *   bounded, production-ready memory footprint of `Queue + HashSet`.
 * - Most common trap: Updating the timestamp on a failed check, artificially extending the cooldown period forever.
 * - One-line mental trigger: "Evicting old timestamps? Chronological stream -> Queue clears the old, Set checks the new."
 * =========================================================================================
 */











































import java.util.*;

class Logger {

    // WHY: Each message has an independent rate limit.
    // Store only the timestamp of its last accepted occurrence.
    //
    // Key   = Message
    // Value = Last accepted timestamp
    //
    // We do not need to store rejected occurrences.
    private final Map<String, Integer> lastPrinted = new HashMap<>();

    public Logger() {
    }

    public boolean shouldPrintMessage(int timestamp, String message) {

        // WHY: HashMap.get() returns null when the message
        // has never been accepted before.
        Integer previousTimestamp = lastPrinted.get(message);

        // CASE 1: First occurrence of this message.
        if (previousTimestamp == null) {

            // WHY: First occurrence is always eligible.
            // Record its accepted timestamp.
            lastPrinted.put(message, timestamp);

            return true;
        }

        // CASE 2: Message was accepted previously.
        //
        // WHY: At least 10 seconds must have elapsed
        // since the last ACCEPTED occurrence.
        //
        // Use >= because exactly 10 seconds is allowed.
        if (timestamp - previousTimestamp >= 10) {

            // WHY: Update only when accepting the message.
            // Rejected messages must not reset the rate limit.
            lastPrinted.put(message, timestamp);

            return true;
        }

        // CASE 3: Less than 10 seconds have elapsed.
        //
        // WHY: Reject without updating the timestamp.
        return false;
    }
}




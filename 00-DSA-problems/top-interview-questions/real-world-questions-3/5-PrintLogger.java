import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/*
 * PrintLogger (Sliding Window Unique Message Printer)
 *
 * WINDOW = 10 seconds
 *
 * Behavior:
 * - PRINT(message)  when message appears exactly once in the window
 * - RETRACT(message) when message becomes non-unique (count goes from 1 -> 2)
 *
 * When old events expire, a message may become unique again => PRINT again.
 *
 * This is like a streaming system with "retractions".
 *
 * Time Complexity:
 * - process(): amortized O(1)
 *   (each event is inserted once and removed once)
 *
 * Space Complexity:
 * - O(number of events in last WINDOW seconds)
 */
class PrintLogger {

    private static final long WINDOW = 10;

    private static class Event {
        long timestamp;
        String message;

        Event(long timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
    }

    // Maintains all events currently in window in arrival order
    private final Deque<Event> windowQueue = new ArrayDeque<>();

    // message -> frequency count inside current window
    private final Map<String, Integer> freq = new HashMap<>();

    public void process(long timestamp, String message) {

        // Step 1: Expire old events
        expireOldEvents(timestamp);

        // Step 2: Add new event
        windowQueue.offerLast(new Event(timestamp, message));

        int oldCount = freq.getOrDefault(message, 0);
        int newCount = oldCount + 1;
        freq.put(message, newCount);

        // Step 3: Apply frequency transition rule
        if (oldCount == 0 && newCount == 1) {
            print(message);              // becomes unique
        } else if (oldCount == 1 && newCount == 2) {
            retract(message);            // no longer unique
        }
    }

    private void expireOldEvents(long timestamp) {

        while (!windowQueue.isEmpty() &&
               windowQueue.peekFirst().timestamp <= timestamp - WINDOW) {

            Event old = windowQueue.pollFirst();
            String msg = old.message;

            int oldCount = freq.get(msg);
            int newCount = oldCount - 1;

            if (newCount == 0) {
                freq.remove(msg);
            } else {
                freq.put(msg, newCount);
            }

            /*
             * Apply transition rules due to expiry:
             *
             * oldCount=2 -> newCount=1 means message becomes unique again => PRINT
             * oldCount=1 -> newCount=0 means message disappears => nothing
             */
            if (oldCount == 2 && newCount == 1) {
                print(msg);
            }
        }
    }

    private void print(String message) {
        System.out.println("PRINT: " + message);
    }

    private void retract(String message) {
        System.out.println("RETRACT: " + message);
    }
}




/*
 * PrintLogger (Delayed Unique Printing)
 *
 * Rule:
 * Print a message ONLY if it appeared exactly once in the last 10 seconds.
 *
 * IMPORTANT:
 * - We cannot print immediately on first arrival.
 * - Because a duplicate may arrive within 10 seconds.
 *
 * So we delay printing until the event becomes "older than 10 seconds"
 * and we confirm its frequency in that window is still 1.
 *
 * Example:
 * t=1  A
 * t=5  A
 * t=12 X
 *
 * When processing t=12:
 * - event at t=1 expires (12-1 > 10)
 * - freq(A)=2 inside [1..11] so we DO NOT print A
 *
 * Time Complexity: O(1) amortized per event
 * Space Complexity: O(events in last 10 seconds)
 */
class PrintLoggerDelayed {

    private static final long WINDOW = 10;

    private static class Event {
        long timestamp;
        String message;

        Event(long timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
    }

    // Stores events in arrival order (sliding window)
    private final Deque<Event> queue = new ArrayDeque<>();

    // message -> frequency in current window
    private final Map<String, Integer> freq = new HashMap<>();

    public void process(long timestamp, String message) {

        // Step 1: expire old events (and decide printing for them)
        expireOldEvents(timestamp);

        // Step 2: insert current event
        queue.offerLast(new Event(timestamp, message));
        freq.put(message, freq.getOrDefault(message, 0) + 1);
    }

    private void expireOldEvents(long timestamp) {

        while (!queue.isEmpty() &&
               queue.peekFirst().timestamp <= timestamp - WINDOW) {

            Event old = queue.pollFirst();
            String msg = old.message;

            /*
             * At the moment of expiration, old event is leaving the 10-sec window.
             * Before removing its count, we check:
             *
             * If freq(msg) == 1, it means this message appeared exactly once
             * in the entire window => SAFE TO PRINT now.
             *
             * If freq(msg) > 1, then it was duplicated => DO NOT PRINT.
             */
            if (freq.get(msg) == 1) {
                print(old.message, old.timestamp);
            }

            // Now remove this old event from frequency map
            int newCount = freq.get(msg) - 1;

            if (newCount == 0) {
                freq.remove(msg);
            } else {
                freq.put(msg, newCount);
            }
        }
    }

    private void print(String message, long originalTimestamp) {
        System.out.println("PRINT: " + message + " (eventTime=" + originalTimestamp + ")");
    }
}

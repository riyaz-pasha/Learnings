import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * StatusLogger:
 * 
 * Prints a message only if it appears exactly once
 * within its 10-second window.
 *
 * If a duplicate appears within 10 seconds,
 * both the original and duplicate are invalid.
 */
public class StatusLogger {

    /**
     * Represents one incoming event
     */
    private static class LogEvent {
        int timestamp;
        String message;

        LogEvent(int timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
    }

    // Sliding window queue (stores only last 10 seconds of events)
    private Queue<LogEvent> windowQueue;

    // Frequency map for messages inside the 10-second window
    private Map<String, Integer> frequency;

    // Window size in seconds
    private static final int WINDOW_SIZE = 10;

    public StatusLogger() {
        windowQueue = new LinkedList<>();
        frequency = new HashMap<>();
    }

    /**
     * Processes an incoming message.
     *
     * @param timestamp monotonically increasing timestamp
     * @param message   status message
     */
    public void process(int timestamp, String message) {

        // -------------------------------------------
        // STEP 1: Remove expired events
        // -------------------------------------------
        // Any event older than (timestamp - 10)
        // is no longer in the sliding window.
        while (!windowQueue.isEmpty() &&
               windowQueue.peek().timestamp <= timestamp - WINDOW_SIZE) {

            LogEvent oldEvent = windowQueue.poll();

            int count = frequency.get(oldEvent.message);

            if (count == 1) {
                // This message occurred exactly once
                // in its 10-second window → valid
                print(oldEvent.message);
                frequency.remove(oldEvent.message);
            } else {
                // It appeared more than once → invalid
                frequency.put(oldEvent.message, count - 1);
            }
        }

        // -------------------------------------------
        // STEP 2: Add new event to window
        // -------------------------------------------
        windowQueue.offer(new LogEvent(timestamp, message));
        frequency.put(message, frequency.getOrDefault(message, 0) + 1);
    }

    /**
     * Prints the valid message.
     * (Can be replaced with actual logging logic)
     */
    private void print(String message) {
        System.out.println(message);
    }
}

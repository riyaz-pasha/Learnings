import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

/**
 * SlidingWindowPercentile (Exact 95th percentile over last 10 minutes)
 *
 * Stream input: (timestamp, latency)
 *
 * Requirement:
 * - Maintain only events in the last 10 minutes (600 seconds)
 * - Support percentile query at any time
 *
 * Key Data Structures:
 * --------------------
 * 1) Deque<Event> window
 *    - Stores events in arrival order (time order)
 *    - Helps expire old events in O(1) amortized
 *
 * 2) TreeMap<Integer, Integer> freqMap
 *    - Acts like a sorted multiset: latency -> frequency
 *    - Supports insert/delete in O(log U)
 *    - Always sorted so percentile can be found by cumulative count
 *
 * Important Note:
 * ---------------
 * This solution computes percentile EXACTLY.
 *
 * Time Complexity:
 * ---------------
 * process(): O(log U) per insertion/removal
 * get95thPercentile(): O(U) worst case (scan TreeMap)
 *
 * Space Complexity:
 * ----------------
 * O(W) where W = number of events in last 10 minutes
 */
class SlidingWindowPercentile {

    private static final long WINDOW_SECONDS = 600; // 10 minutes

    /**
     * Represents a single event in the stream.
     */
    private static class Event {
        long timestamp;
        int latency;

        Event(long timestamp, int latency) {
            this.timestamp = timestamp;
            this.latency = latency;
        }
    }

    // Maintains events in timestamp order for sliding expiry
    private final Deque<Event> window = new ArrayDeque<>();

    // Sorted multiset: latency -> frequency count
    private final TreeMap<Integer, Integer> freqMap = new TreeMap<>();

    // Total events currently inside the 10-minute window
    private int totalCount = 0;

    /**
     * Process a new latency event.
     *
     * Steps:
     * 1) Expire events older than (timestamp - 600)
     * 2) Insert new event
     */
    public void process(long timestamp, int latency) {

        // Expire all events outside sliding window
        while (!window.isEmpty() &&
                window.peekFirst().timestamp < timestamp - WINDOW_SECONDS) {

            Event old = window.pollFirst();
            removeLatency(old.latency);
        }

        // Add new event
        window.addLast(new Event(timestamp, latency));
        addLatency(latency);
    }

    /**
     * Adds a latency value into the sorted frequency map.
     *
     * Time: O(log U)
     */
    private void addLatency(int latency) {
        freqMap.put(latency, freqMap.getOrDefault(latency, 0) + 1);
        totalCount++;
    }

    /**
     * Removes a latency value from the sorted frequency map.
     *
     * Time: O(log U)
     */
    private void removeLatency(int latency) {

        int count = freqMap.get(latency);

        if (count == 1) {
            freqMap.remove(latency);
        } else {
            freqMap.put(latency, count - 1);
        }

        totalCount--;
    }

    /**
     * Returns the EXACT 95th percentile of the current window.
     *
     * Definition:
     * - 95th percentile = smallest value X such that
     *   at least 95% of values are <= X.
     *
     * Example:
     * totalCount = 20
     * k = ceil(0.95 * 20) = ceil(19) = 19
     * We return the 19th smallest element.
     *
     * Time: O(U) in worst case (scan TreeMap)
     */
    public int get95thPercentile() {

        if (totalCount == 0) {
            throw new IllegalStateException("No data in sliding window");
        }

        // kth smallest element position (1-indexed)
        int k = (int) Math.ceil(0.95 * totalCount);

        int cumulative = 0;

        // Walk sorted latencies until cumulative frequency reaches k
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {

            cumulative += entry.getValue();

            if (cumulative >= k) {
                return entry.getKey();
            }
        }

        // Should never happen unless bug exists
        throw new IllegalStateException("Percentile calculation failed");
    }
}

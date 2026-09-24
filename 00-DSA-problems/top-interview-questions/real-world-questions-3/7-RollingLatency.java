import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/*
 * RollingLatency (Rolling Average Latency per Service)
 *
 * Requirement:
 * - Maintain average latency for each service in the last 5 minutes.
 *
 * Key Idea:
 * - For each service, store events in a queue (timestamp, latency).
 * - Evict events older than WINDOW seconds.
 * - Maintain running sum and count so getAverage() is O(1).
 *
 * Time Complexity:
 * - process(): amortized O(1)
 *   (each event is inserted once and removed once)
 *
 * - getAverage(): O(1)
 *
 * Space Complexity:
 * - O(total number of events in last 5 minutes across all services)
 */
class RollingLatency {

    private static final long WINDOW = 300; // 5 minutes in seconds

    private static class Event {
        long timestamp;
        int latency;

        Event(long timestamp, int latency) {
            this.timestamp = timestamp;
            this.latency = latency;
        }
    }

    /*
     * Stores sliding window state for a service:
     * - queue of recent events
     * - running sum of latencies
     * - count of events
     */
    private static class ServiceWindow {
        Deque<Event> events = new ArrayDeque<>();
        long sum = 0;
        int count = 0;
    }

    // serviceName -> window data
    private final Map<String, ServiceWindow> serviceMap = new HashMap<>();

    public void process(long timestamp, String service, int latency) {

        serviceMap.putIfAbsent(service, new ServiceWindow());
        ServiceWindow window = serviceMap.get(service);

        /*
         * Expire old events outside the window.
         *
         * If WINDOW=300 and current timestamp=1000,
         * valid events should be in range (700, 1000].
         *
         * So event.timestamp <= 700 must be removed.
         */
        while (!window.events.isEmpty() &&
                window.events.peekFirst().timestamp <= timestamp - WINDOW) {

            Event old = window.events.pollFirst();
            window.sum -= old.latency;
            window.count--;
        }

        // Add new event into the sliding window
        window.events.offerLast(new Event(timestamp, latency));
        window.sum += latency;
        window.count++;
    }

    /*
     * Returns rolling average latency of a service for last 5 minutes.
     *
     * Time: O(1)
     */
    public double getAverage(String service) {

        ServiceWindow window = serviceMap.get(service);

        if (window == null || window.count == 0) {
            return 0.0;
        }

        return (double) window.sum / window.count;
    }
}

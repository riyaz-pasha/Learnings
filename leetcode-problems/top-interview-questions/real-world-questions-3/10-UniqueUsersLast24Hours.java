import java.util.*;

/**
 * UniqueUsersLast24Hours
 *
 * Stream input: (timestamp, userId)
 *
 * Requirement:
 * - Return number of unique users active in the last 24 hours.
 *
 * Key Idea:
 * - Maintain a sliding window of events (timestamp, userId).
 * - Use HashMap to track frequency of each user inside the window.
 *
 * Time Complexity:
 * - process(): O(1) amortized
 * - getUniqueCount(): O(1)
 *
 * Space Complexity:
 * - O(W) where W = number of events in last 24 hours
 */
class UniqueUsersLast24Hours {

    private static final long WINDOW_SECONDS = 24 * 60 * 60; // 86400 seconds

    private static class Event {
        long timestamp;
        String userId;

        Event(long timestamp, String userId) {
            this.timestamp = timestamp;
            this.userId = userId;
        }
    }

    // Stores events in time order (oldest at front)
    private final Deque<Event> window = new ArrayDeque<>();

    // userId -> number of events still inside window
    private final Map<String, Integer> freq = new HashMap<>();

    /**
     * Process a new event: userId seen at timestamp.
     */
    public void process(long timestamp, String userId) {

        // Step 1: Expire old events
        expireOld(timestamp);

        // Step 2: Add new event into window
        window.addLast(new Event(timestamp, userId));

        // Step 3: Update frequency map
        freq.put(userId, freq.getOrDefault(userId, 0) + 1);
    }

    /**
     * Returns number of unique users in last 24 hours.
     */
    public int getUniqueUsersCount(long timestamp) {

        // Ensure window is updated before answering
        expireOld(timestamp);

        return freq.size();
    }

    /**
     * Remove expired events from front of deque.
     */
    private void expireOld(long timestamp) {

        long expiryTime = timestamp - WINDOW_SECONDS;

        while (!window.isEmpty() && window.peekFirst().timestamp < expiryTime) {

            Event old = window.pollFirst();

            int count = freq.get(old.userId) - 1;

            if (count == 0) {
                freq.remove(old.userId); // user completely expired
            } else {
                freq.put(old.userId, count);
            }
        }
    }
}

/**
 * MemoryBoundedUniqueCounter (Approx Unique Users in Last 24 Hours)
 *
 * Uses:
 * - 24 hourly buckets
 * - Each bucket stores HyperLogLog summary for that hour
 *
 * Why HLL?
 * - Exact counting needs storing userIds (impossible at billions scale)
 * - HLL gives approximate unique count using fixed memory
 *
 * Sliding Window Logic:
 * - Each hour has one HLL bucket
 * - When hour changes, bucket resets
 * - Query merges only buckets that belong to last 24 hours
 *
 * Time Complexity:
 * - process(): O(1)
 * - getUniqueLast24Hours(): O(24 * HLL_merge_cost) ~ O(1)
 *
 * Space Complexity:
 * - O(24 * HLL_size) -> constant memory
 */
class MemoryBoundedUniqueCounter {

    private static final int HOURS = 24;
    private static final int SECONDS_PER_HOUR = 3600;

    private static class Bucket {
        long hourStart; // which hour this bucket represents
        HyperLogLog hll; // summary structure

        Bucket(long hourStart, int precision) {
            this.hourStart = hourStart;
            this.hll = new HyperLogLog(precision);
        }

        void reset(long newHour, int precision) {
            this.hourStart = newHour;
            this.hll = new HyperLogLog(precision);
        }
    }

    private final Bucket[] buckets = new Bucket[HOURS];
    private final int precision;

    public MemoryBoundedUniqueCounter(int precision) {
        this.precision = precision;

        for (int i = 0; i < HOURS; i++) {
            buckets[i] = new Bucket(-1, precision);
        }
    }

    /**
     * Process an event (timestamp, userId).
     *
     * We map timestamp -> hour bucket.
     */
    public void process(long timestamp, String userId) {

        long hour = timestamp / SECONDS_PER_HOUR;
        int index = (int) (hour % HOURS);

        Bucket bucket = buckets[index];

        // If bucket belongs to an older hour, reset it
        if (bucket.hourStart != hour) {
            bucket.reset(hour, precision);
        }

        bucket.hll.add(userId);
    }

    /**
     * Returns approximate unique users in last 24 hours from nowTimestamp.
     *
     * Only buckets with hourStart in [currentHour-23 ... currentHour] are merged.
     */
    public long getUniqueLast24Hours(long nowTimestamp) {

        long currentHour = nowTimestamp / SECONDS_PER_HOUR;
        long oldestValidHour = currentHour - (HOURS - 1);

        HyperLogLog merged = new HyperLogLog(precision);

        for (Bucket bucket : buckets) {

            // Skip stale buckets (older than last 24 hours)
            if (bucket.hourStart < oldestValidHour || bucket.hourStart > currentHour) {
                continue;
            }

            merged.merge(bucket.hll);
        }

        return merged.estimate();
    }
}

import java.util.ArrayDeque;
import java.util.Deque;

class HitCounter {

    // Each entry represents one hit.
    // ArrayDeque provides efficient insertion at the back and removal at the front.
    private final Deque<Integer> hitTimestamps = new ArrayDeque<>();

    public HitCounter() {
        // An empty queue means that no hits have been recorded.
    }

    public void hit(int timestamp) {
        // Calls arrive chronologically, so each new hit belongs at the back.
        // Store duplicate timestamps too: each call represents a separate hit.
        hitTimestamps.offerLast(timestamp);
    }

    public int getHits(int timestamp) {
        // A hit is valid only if timestamp - hitTime < 300.
        // Equivalently, hitTime must be strictly greater than timestamp - 300.
        int expirationBoundary = timestamp - 300;

        // Remove every hit that is exactly 300 seconds old or older.
        // The empty check prevents accessing the front of an empty queue.
        while (!hitTimestamps.isEmpty()
                && hitTimestamps.peekFirst() <= expirationBoundary) {
            hitTimestamps.pollFirst();
        }

        // Every remaining timestamp belongs to the current 300-second window.
        return hitTimestamps.size();
    }

    /*
     * TIME COMPLEXITY:
     *
     * hit(): O(1).
     * - Appending one timestamp to the deque takes O(1).
     *
     * getHits(): O(k) for one call in the worst case, where k is the
     * number of expired entries removed during that call.
     * - Each removal takes O(1).
     * - A single query can remove many old entries if time jumps forward.
     *
     * AMORTIZED COMPLEXITY:
     * - O(1) per operation over a sequence of calls.
     * - Every hit is appended exactly once and removed at most once.
     * - Across N calls, the total number of removals is at most the
     *   total number of hits, so cleanup work is O(N) overall.
     *
     * SPACE COMPLEXITY:
     * - O(h), where h is the number of hits retained in the queue.
     * - After getHits(t), retained hits are strictly newer than t - 300.
     * - If queries are infrequent, the queue can temporarily contain
     *   old hits until the next query performs cleanup.
     *
     * CORRECTNESS:
     * - Calls arrive chronologically, so the queue is ordered by timestamp.
     * - Expired hits form a prefix of the queue.
     * - Removing that prefix leaves exactly the hits in the valid window.
     * - The queue size therefore equals the required hit count.
     */
}











































class HitCounterCircularArray {

    private static final int WINDOW_SECONDS = 300;

    // Each index represents one of the 300 possible circular buckets.
    private final int[] timestamps = new int[WINDOW_SECONDS];
    private final int[] hitCounts = new int[WINDOW_SECONDS];

    public void hit(int timestamp) {
        int index = timestamp % WINDOW_SECONDS;

        // The bucket may belong to an earlier second that shares this index.
        // If so, overwrite that stale second and start its new count at 1.
        if (timestamps[index] != timestamp) {
            timestamps[index] = timestamp;
            hitCounts[index] = 1;
        } else {
            // Multiple hits during the same second share the same bucket.
            hitCounts[index]++;
        }
    }

    public int getHits(int timestamp) {
        int totalHits = 0;
        int earliestValidTimestamp = timestamp - WINDOW_SECONDS + 1;

        // Examine all 300 buckets.
        for (int i = 0; i < WINDOW_SECONDS; i++) {
            // Only count buckets whose actual timestamp lies in the window.
            // Checking the actual timestamp prevents counting stale data
            // left behind by circular-index reuse.
            if (timestamps[i] >= earliestValidTimestamp
                    && timestamps[i] <= timestamp) {
                totalHits += hitCounts[i];
            }
        }

        return totalHits;
    }

    /*
     * TIME COMPLEXITY:
     * hit(): O(1).
     * - Compute the bucket index and update at most one bucket.
     *
     * getHits(): O(300) = O(1).
     * - Always examines exactly 300 buckets, independent of hit volume.
     *
     * SPACE COMPLEXITY:
     * O(300) = O(1).
     * - Two fixed-size arrays, regardless of how many hits occur.
     *
     * WHY THIS SCALES BETTER:
     * - A million hits in the same second require only one bucket.
     * - The queue solution would store a million separate timestamps.
     *
     * TRADE-OFF:
     * - Queries inspect all 300 buckets instead of returning a queue size.
     * - For this fixed window, that is a small, constant amount of work.
     */
}













































import java.util.NavigableMap;
import java.util.TreeMap;

class HitCounterOutOfOrder {

    // Key = event timestamp; value = number of hits at that timestamp.
    private final NavigableMap<Integer, Long> hitsByTimestamp =
            new TreeMap<>();

    public void hit(int timestamp) {
        // merge handles duplicate timestamps and out-of-order arrivals.
        hitsByTimestamp.merge(timestamp, 1L, Long::sum);
    }

    public long getHits(int timestamp) {
        int earliestValidSecond = timestamp - 299;

        // Obtain only timestamps in the inclusive interval
        // [timestamp - 299, timestamp].
        var relevantHits = hitsByTimestamp.subMap(
                earliestValidSecond, true,
                timestamp, true);

        long total = 0;

        for (long count : relevantHits.values()) {
            total += count;
        }

        return total;
    }

    /*
     * hit(): O(log n), where n is the number of distinct stored timestamps.
     *
     * getHits(): O(log n + k), where k is the number of distinct timestamps
     * in the requested interval.
     *
     * Space: O(n), because this version retains the entire timestamp history.
     *
     * Advantages:
     * - Hits can arrive in arbitrary timestamp order.
     * - Duplicate timestamps are aggregated.
     * - Queries can target a timestamp earlier than the latest arrival.
     *
     * Limitation:
     * - This simple version never deletes old history.
     * - It can support arbitrary historical queries only while that history
     *   is retained.
     */
}


import java.util.NavigableMap;
import java.util.TreeMap;

class HitCounterTreeMapRange {

    // Key   = timestamp of a hit.
    // Value = number of hits recorded at that timestamp.
    //
    // TreeMap keeps timestamps sorted automatically.
    // NavigableMap exposes useful range-query operations.
    private final NavigableMap<Integer, Long> hitsByTimestamp =
            new TreeMap<>();

    public void hit(int timestamp) {
        // If this timestamp already exists, increase its count by one.
        // Otherwise, insert it with an initial count of one.
        //
        // merge() handles both cases without a separate containsKey() check.
        hitsByTimestamp.merge(timestamp, 1L, Long::sum);
    }

    public long getHits(int timestamp) {
        // For a 300-second window, timestamps must be strictly greater
        // than timestamp - 300 and less than or equal to timestamp.
        //
        // Since timestamps are integers, the inclusive lower bound is t - 299.
        int firstValidTimestamp = timestamp - 299;

        // subMap(from, true, to, true) returns the inclusive timestamp range:
        // [timestamp - 299, timestamp].
        //
        // This range excludes old events and also excludes future events
        // that arrived early but have timestamps greater than the query time.
        var hitsInWindow = hitsByTimestamp.subMap(
                firstValidTimestamp, true,
                timestamp, true);

        long total = 0;

        // Iterate only through timestamps inside the requested window.
        // Each value is already an aggregated count for that second.
        for (long count : hitsInWindow.values()) {
            total += count;
        }

        return total;
    }

    /*
     * TIME COMPLEXITY:
     *
     * hit():
     * - O(log n), because TreeMap insertion/search takes logarithmic time.
     *
     * getHits():
     * - O(log n + k), where n is the number of distinct timestamps stored
     *   and k is the number of distinct timestamps inside the query window.
     * - O(log n) locates the range boundaries.
     * - O(k) sums the matching counts.
     *
     * SPACE COMPLEXITY:
     * - O(n), where n is the number of distinct timestamps stored.
     * - Duplicate hits at the same timestamp consume one map entry.
     *
     * BEST FOR:
     * - Out-of-order arrivals.
     * - Historical queries.
     * - Efficient range queries over sorted timestamps.
     *
     * TRADE-OFF:
     * - Every distinct timestamp adds a TreeMap node.
     * - The entire history remains in memory.
     */
}








import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

class HitCounterTreeMapNavigation {

    private final NavigableMap<Integer, Long> hitsByTimestamp =
            new TreeMap<>();

    public void hit(int timestamp) {
        // Aggregate hits that share a timestamp.
        hitsByTimestamp.merge(timestamp, 1L, Long::sum);
    }

    public long getHits(int timestamp) {
        int firstValidTimestamp = timestamp - 299;
        long total = 0;

        // Find the first key greater than or equal to the lower boundary.
        // If no such key exists, there are no hits in the window.
        Map.Entry<Integer, Long> current =
                hitsByTimestamp.ceilingEntry(firstValidTimestamp);

        // Move forward through sorted keys until the query's upper boundary.
        while (current != null && current.getKey() <= timestamp) {
            total += current.getValue();

            // Find the next key strictly greater than the current key.
            current = hitsByTimestamp.higherEntry(current.getKey());
        }

        return total;
    }

    /*
     * TIME COMPLEXITY:
     *
     * hit():
     * - O(log n).
     *
     * getHits():
     * - O(log n + k), where k is the number of distinct timestamps
     *   in the requested range.
     * - ceilingEntry() finds the starting point in O(log n).
     * - Each higherEntry() lookup takes O(log n), so a strict analysis of
     *   this particular implementation gives O(log n + k log n).
     *
     * SPACE COMPLEXITY:
     * - O(n).
     *
     * IMPORTANT COMPARISON:
     * - This version is useful for learning ordered-map navigation.
     * - The subMap() version is generally cleaner and can iterate over
     *   the range in O(log n + k) time.
     *
     * BEST FOR:
     * - Situations where you need to find a starting key and navigate
     *   forward using NavigableMap methods.
     */
}




































import java.util.ArrayList;
import java.util.List;

class HitCounterSortedList {

    // One entry per distinct timestamp, sorted in ascending order.
    private record HitBucket(int timestamp, long count) {}

    private final List<HitBucket> buckets = new ArrayList<>();

    public void hit(int timestamp) {
        // Find the first position whose timestamp is >= the new timestamp.
        int index = lowerBound(timestamp);

        if (index < buckets.size()
                && buckets.get(index).timestamp() == timestamp) {
            // The timestamp already exists: replace its immutable record
            // with a new record whose count is incremented.
            HitBucket old = buckets.get(index);
            buckets.set(index,
                    new HitBucket(timestamp, old.count() + 1));
        } else {
            // Insert at the sorted position.
            // ArrayList shifts later elements to make room.
            buckets.add(index, new HitBucket(timestamp, 1));
        }
    }

    public long getHits(int timestamp) {
        int firstValidTimestamp = timestamp - 299;
        long total = 0;

        // Binary search skips every timestamp earlier than the window.
        int index = lowerBound(firstValidTimestamp);

        // Only visit buckets within the requested time interval.
        while (index < buckets.size()) {
            HitBucket bucket = buckets.get(index);

            // Sorted order guarantees all subsequent timestamps are larger.
            if (bucket.timestamp() > timestamp) {
                break;
            }

            total += bucket.count();
            index++;
        }

        return total;
    }

    private int lowerBound(int targetTimestamp) {
        int left = 0;
        int right = buckets.size();

        // Find the first index whose timestamp is >= targetTimestamp.
        // If all timestamps are smaller, return buckets.size().
        while (left < right) {
            int middle = left + (right - left) / 2;

            if (buckets.get(middle).timestamp() < targetTimestamp) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }

        return left;
    }

    /*
     * TIME COMPLEXITY:
     *
     * hit():
     * - O(log n) to find the insertion position.
     * - O(n) worst case to shift entries during insertion.
     * - Overall: O(n) worst case.
     *
     * getHits():
     * - O(log n + k), where k is the number of distinct timestamps
     *   in the requested interval.
     *
     * SPACE COMPLEXITY:
     * - O(n).
     *
     * ADVANTAGES:
     * - Sorted storage and efficient range queries.
     * - Compact array-backed representation.
     *
     * DISADVANTAGES:
     * - Out-of-order insertions may shift many elements.
     * - Repeated insertions can make this significantly slower than TreeMap.
     *
     * BEST FOR:
     * - Small datasets.
     * - Read-heavy workloads with relatively few insertions.
     * - Situations where array-backed storage is desirable.
     */
}


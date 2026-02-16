import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/*
 * TrendingKeywords (Last 60 Minutes)
 *
 * We want to count keyword frequency in the last 60 minutes sliding window.
 *
 * Key Design:
 * - Use circular array of 60 buckets (one per minute).
 * - Each bucket stores counts of keywords for that minute.
 * - Maintain a globalCounts map for total frequency in last 60 minutes.
 *
 * When a bucket is reused (minute changes):
 * - subtract its old counts from globalCounts
 * - clear bucket and reuse for current minute
 *
 * process() is O(number of unique keywords in overwritten bucket)
 * getTopK() is O(N log K), where N = unique keywords in last 60 minutes
 *
 * Space:
 * - buckets store at most last 60 minutes of keyword counts
 * - globalCounts stores unique keywords in last 60 minutes
 */
class TrendingKeywords {

    private static final int WINDOW_MINUTES = 60;

    private static class Bucket {
        long minute = -1; // which minute this bucket currently represents
        Map<String, Integer> counts = new HashMap<>();
    }

    private final Bucket[] buckets = new Bucket[WINDOW_MINUTES];

    // keyword -> total frequency in last 60 minutes
    private final Map<String, Integer> globalCounts = new HashMap<>();

    /*
     * Process an incoming keyword event.
     *
     * Assumption:
     * - timestamp is in SECONDS.
     * - if timestamp is in milliseconds, use timestamp / 60_000 instead.
     *
     * Time Complexity:
     * - O(1) average per call (amortized)
     * - but when overwriting an old bucket, we subtract all its stored keywords
     */
    public void process(long timestampSeconds, String keyword) {

        long minute = timestampSeconds / 60; // current minute bucket
        int index = (int) (minute % WINDOW_MINUTES);

        if (buckets[index] == null) {
            buckets[index] = new Bucket();
        }

        Bucket bucket = buckets[index];

        /*
         * If bucket belongs to a different minute,
         * it means it is stale (from an old window).
         *
         * We must remove its contribution from globalCounts before reusing it.
         */
        if (bucket.minute != minute) {

            // Remove old counts from globalCounts
            for (Map.Entry<String, Integer> entry : bucket.counts.entrySet()) {

                String oldKeyword = entry.getKey();
                int oldCount = entry.getValue();

                int updated = globalCounts.getOrDefault(oldKeyword, 0) - oldCount;

                if (updated <= 0) {
                    globalCounts.remove(oldKeyword);
                } else {
                    globalCounts.put(oldKeyword, updated);
                }
            }

            // Reset bucket for current minute
            bucket.counts.clear();
            bucket.minute = minute;
        }

        // Add keyword to this minute bucket
        bucket.counts.put(keyword, bucket.counts.getOrDefault(keyword, 0) + 1);

        // Add keyword to global counts
        globalCounts.put(keyword, globalCounts.getOrDefault(keyword, 0) + 1);
    }

    /*
     * Return top K trending keywords in last 60 minutes.
     *
     * Approach:
     * - Use minHeap of size K.
     *
     * Time Complexity: O(N log K), N = unique keywords in globalCounts
     * Space Complexity: O(K)
     */
    public List<String> getTopK(int k) {

        PriorityQueue<Map.Entry<String, Integer>> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<String, Integer> entry : globalCounts.entrySet()) {
            minHeap.offer(entry);

            if (minHeap.size() > k) {
                minHeap.poll(); // remove smallest
            }
        }

        // Extract results from heap (reverse order)
        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll().getKey());
        }

        Collections.reverse(result);
        return result;
    }
}

import java.util.*;

/**
 * HotKeyDetector
 *
 * Detect keys accessed more than THRESHOLD times in the last 60 seconds.
 *
 * Stream input: (timestampSeconds, key)
 *
 * Key Idea:
 * - Use a ring buffer of 60 buckets (1 per second).
 * - Each bucket stores counts for that second.
 * - Maintain a globalCounts map for total counts across last 60 seconds.
 *
 * Time Complexity:
 * - process(): O(1) average
 * - isHot(key): O(1)
 *
 * Space Complexity:
 * - O(K) where K = number of distinct keys accessed in last 60 seconds
 */
class HotKeyDetector {

    private static final int WINDOW_SECONDS = 60;
    private static final int THRESHOLD = 1000;

    private static class Bucket {
        long second; // which second this bucket represents
        Map<String, Integer> counts = new HashMap<>();
    }

    private final Bucket[] buckets = new Bucket[WINDOW_SECONDS];

    // key -> total access count in last 60 seconds
    private final Map<String, Integer> globalCounts = new HashMap<>();

    public HotKeyDetector() {
        for (int i = 0; i < WINDOW_SECONDS; i++) {
            buckets[i] = new Bucket();
            buckets[i].second = -1;
        }
    }

    /**
     * Process one access event.
     */
    public void process(long timestampSeconds, String key) {

        long sec = timestampSeconds;
        int index = (int) (sec % WINDOW_SECONDS);

        Bucket bucket = buckets[index];

        // If bucket is stale (belongs to an old second), reset it
        if (bucket.second != sec) {

            // Remove old bucket counts from globalCounts
            for (Map.Entry<String, Integer> entry : bucket.counts.entrySet()) {
                String oldKey = entry.getKey();
                int oldCount = entry.getValue();

                globalCounts.put(oldKey, globalCounts.get(oldKey) - oldCount);

                if (globalCounts.get(oldKey) == 0) {
                    globalCounts.remove(oldKey);
                }
            }

            // Reset bucket for new second
            bucket.counts.clear();
            bucket.second = sec;
        }

        // Add event into bucket
        bucket.counts.put(key, bucket.counts.getOrDefault(key, 0) + 1);

        // Add event into globalCounts
        globalCounts.put(key, globalCounts.getOrDefault(key, 0) + 1);
    }

    /**
     * Returns true if key is accessed more than 1000 times in last 60 seconds.
     */
    public boolean isHot(String key) {
        return globalCounts.getOrDefault(key, 0) > THRESHOLD;
    }

    /**
     * Optional: return all hot keys right now.
     */
    public List<String> getAllHotKeys() {

        List<String> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : globalCounts.entrySet()) {
            if (entry.getValue() > THRESHOLD) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    /*
     * “We use a 60-second ring buffer. Each bucket stores counts for that second.
     * When a bucket is reused, we subtract its old counts from globalCounts. This
     * way we always maintain exact counts for last 60 seconds in O(1).”
     */
}


/**
 * HotKeyDetectorUsingCountMinSketch
 *
 * Requirement:
 * Detect keys accessed more than 1000 times in last 60 seconds.
 *
 * Constraint:
 * - Millions of unique keys per minute (HashMap would explode in memory)
 *
 * Solution:
 * - Use Sliding Window (60 buckets, 1 per second)
 * - Each bucket stores Count-Min Sketch (CMS) instead of HashMap
 *
 * Why CMS?
 * - Fixed memory usage
 * - O(1) update per event
 * - Approximate frequency estimation
 * - Can handle extremely large cardinality
 *
 * Accuracy:
 * - CMS overestimates counts due to collisions
 * - No false negatives (hot keys will not be missed)
 * - May have false positives (some keys may look hot due to collision)
 *
 * Time Complexity:
 * - process(): O(d)   where d = number of hash functions (small constant like 4)
 * - estimate(): O(d * WINDOW) = O(60 * d) ~ O(1)
 *
 * Space Complexity:
 * - O(WINDOW * d * w) = constant memory
 */
class HotKeyDetectorUsingCountMinSketch {

    private static final int WINDOW_SECONDS = 60;
    private static final int THRESHOLD = 1000;

    // Count-Min Sketch parameters
    // d = number of hash functions (rows)
    // w = number of buckets per row (columns)
    private static final int D = 4;
    private static final int W = 20000;  // tune based on memory & collision tolerance

    /**
     * A single Count-Min Sketch data structure.
     */
    private static class CountMinSketch {

        // table[d][w]
        private final int[][] table = new int[D][W];

        // random seeds for hash functions
        private final int[] seeds = new int[D];

        CountMinSketch(int baseSeed) {
            Random rand = new Random(baseSeed);
            for (int i = 0; i < D; i++) {
                seeds[i] = rand.nextInt();
            }
        }

        /**
         * Add one occurrence of key.
         * Time: O(D)
         */
        void add(String key) {
            for (int i = 0; i < D; i++) {
                int idx = hash(key, seeds[i]);
                table[i][idx]++;
            }
        }

        /**
         * Estimate frequency of key.
         *
         * CMS rule:
         * - True count <= estimate
         * - estimate = min across all hash rows
         *
         * Time: O(D)
         */
        int estimate(String key) {
            int min = Integer.MAX_VALUE;

            for (int i = 0; i < D; i++) {
                int idx = hash(key, seeds[i]);
                min = Math.min(min, table[i][idx]);
            }

            return min;
        }

        /**
         * Reset sketch (used when bucket becomes stale).
         */
        void clear() {
            for (int i = 0; i < D; i++) {
                Arrays.fill(table[i], 0);
            }
        }

        /**
         * Hash function:
         * - We use built-in hashCode combined with seed.
         * - Then mod by W to map into table column.
         */
        private int hash(String key, int seed) {
            int h = key.hashCode() ^ seed;

            // Ensure non-negative
            h = h & 0x7fffffff;

            return h % W;
        }
    }

    /**
     * Each bucket corresponds to one second.
     * It stores:
     * - which second it represents
     * - a Count-Min Sketch of accesses during that second
     */
    private static class Bucket {
        long second;
        CountMinSketch cms;

        Bucket(int seed) {
            this.second = -1;
            this.cms = new CountMinSketch(seed);
        }

        void reset(long newSecond) {
            this.second = newSecond;
            this.cms.clear();
        }
    }

    // Ring buffer of 60 buckets
    private final Bucket[] buckets = new Bucket[WINDOW_SECONDS];

    // Optional: track candidate hot keys only (reduces query scanning)
    private final Set<String> candidates = new HashSet<>();

    public HotKeyDetectorUsingCountMinSketch() {
        for (int i = 0; i < WINDOW_SECONDS; i++) {
            buckets[i] = new Bucket(1000 + i); // different seed per bucket
        }
    }

    /**
     * Process one access event.
     *
     * Input timestamp is in SECONDS.
     *
     * Steps:
     * 1) Find bucket index = timestamp % 60
     * 2) If bucket is stale, reset it
     * 3) Add key into that bucket CMS
     * 4) Optionally add key to candidates if it looks frequent
     */
    public void process(long timestampSeconds, String key) {

        long sec = timestampSeconds;
        int index = (int) (sec % WINDOW_SECONDS);

        Bucket bucket = buckets[index];

        // If this bucket represents an older second, reset it
        if (bucket.second != sec) {
            bucket.reset(sec);
        }

        // Add key access into this second's CMS
        bucket.cms.add(key);

        /*
         * Candidate Optimization:
         *
         * If key estimate in last 60 seconds is approaching threshold,
         * store it in candidates so we can check it frequently.
         *
         * This avoids scanning millions of keys.
         */
        if (estimateCount(sec, key) > THRESHOLD / 2) {
            candidates.add(key);
        }
    }

    /**
     * Estimate access count of key in the last 60 seconds.
     *
     * We sum estimates across all valid buckets in the window.
     *
     * NOTE:
     * - This is an approximation.
     * - Because each CMS may overestimate, the sum may also overestimate.
     */
    public int estimateCount(long nowSeconds, String key) {

        long currentSec = nowSeconds;
        long oldestValid = currentSec - (WINDOW_SECONDS - 1);

        int sum = 0;

        for (Bucket bucket : buckets) {

            // Ignore stale buckets outside sliding window
            if (bucket.second < oldestValid || bucket.second > currentSec) {
                continue;
            }

            sum += bucket.cms.estimate(key);
        }

        return sum;
    }

    /**
     * Check if a key is hot (approx).
     */
    public boolean isHot(long nowSeconds, String key) {
        return estimateCount(nowSeconds, key) > THRESHOLD;
    }

    /**
     * Returns list of hot keys (approx).
     *
     * We only scan candidate keys, not all possible keys.
     */
    public List<String> getHotKeys(long nowSeconds) {

        List<String> hotKeys = new ArrayList<>();

        Iterator<String> it = candidates.iterator();

        while (it.hasNext()) {
            String key = it.next();

            int est = estimateCount(nowSeconds, key);

            if (est > THRESHOLD) {
                hotKeys.add(key);
            }

            // If key is far below threshold, remove from candidates
            if (est < THRESHOLD / 4) {
                it.remove();
            }
        }

        return hotKeys;
    }
}

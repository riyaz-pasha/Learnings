/* Given a stream of tweets, return top N hashtags by frequency. */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

class TrendingHashtags {

    /*
     * Let:
     * T = number of tweets processed
     * W = average number of words per tweet
     * H = number of unique hashtags encountered
     * N = number of top hashtags to return (getTopNHashtags(n))
     */

    private final Map<String, Integer> hashtagToFreqMap;

    TrendingHashtags() {
        this.hashtagToFreqMap = new HashMap<>();
    }

    /*
     * ⏱ Time Complexity:
     * Splitting the tweet: O(W) where W = words in tweet
     * For each word:
     * - startsWith(), length() = O(1)
     * - toLowerCase() = O(L) for word length L
     * - Map.getOrDefault() and Map.put() = O(1) average
     * 
     * ✅ So overall:
     * Time = O(W × L) per tweet (but since hashtags are short, this is effectively
     * O(W))
     * 
     * 🧠 Space Complexity:
     * Only affects hashtagToFreqMap, which grows as new hashtags are encountered.
     * 
     * ✅ Space = O(H), where H is number of unique hashtags across all tweets.
     */
    public void processTweet(String tweet) {
        for (String word : tweet.split("\\s+")) {
            if (word.startsWith("#") && word.length() > 1) {
                String hashtag = word.toLowerCase();
                this.hashtagToFreqMap.put(hashtag, this.hashtagToFreqMap.getOrDefault(hashtag, 0) + 1);
                // this.hashtagToFreqMap.merge(hashtag, 1, Integer::sum);
                // this.hashtagToFreqMap.compute(hashtag, (k, v) -> v == null ? 1 : v + 1);
            }
        }
    }

    /*
     * ⏱ Time Complexity:
     * Iterate through all entries in the map: O(H)
     * Each offer() and poll() in heap: O(log n)
     * 
     * For H entries, with heap size capped at n:
     * ✅ Time = O(H × log n)
     * 
     * 🧠 Space Complexity:
     * minHeap holds at most n elements at a time.
     * ✅ Space = O(n) for the heap, and O(n) for result list.
     */
    public List<String> getTopNHashtags(int n) {

        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>((a, b) -> {
            if (a.getValue().equals(b.getValue())) {
                return b.getKey().compareTo(a.getKey());
            }
            return Integer.compare(a.getValue(), b.getValue());

        });

        for (Map.Entry<String, Integer> entry : hashtagToFreqMap.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > n) {
                minHeap.poll();
            }
        }

        List<String> result = new LinkedList<>();
        while (!minHeap.isEmpty()) {
            result.addFirst(minHeap.poll().getKey());
        }

        return result;
    }
}

class HashtagEntry implements Comparable<HashtagEntry> {
    String hashtag;
    int frequency;

    HashtagEntry(String hashtag, int frequency) {
        this.hashtag = hashtag;
        this.frequency = frequency;
    }

    @Override
    public int compareTo(HashtagEntry other) {
        // Higher freq comes first. Break ties by lexicographical order.
        if (this.frequency != other.frequency) {
            return Integer.compare(other.frequency, this.frequency);
        }
        return this.hashtag.compareTo(other.hashtag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof HashtagEntry))
            return false;
        HashtagEntry other = (HashtagEntry) o;
        return this.hashtag.equals(other.hashtag);
    }

    @Override
    public int hashCode() {
        return this.hashtag.hashCode();
    }
}

class PersistentTrendingHashtags {

    private final Map<String, Integer> hashtagToFreqMap;
    private final TreeSet<HashtagEntry> topN;
    private final int n;

    public PersistentTrendingHashtags(int n) {
        this.hashtagToFreqMap = new HashMap<>();
        this.topN = new TreeSet<>();
        this.n = n;
    }

    /*
     * Split + iteration: O(W)
     * For each hashtag:
     * Remove from TreeSet: O(log N)
     * Add updated entry to TreeSet: O(log N)
     * Map update: O(1)
     * 
     * ✅ Overall per hashtag: O(log N)
     * → Per tweet: O(W × log N)
     */
    public void processTweet(String tweet) {
        for (String word : tweet.split("\\s+")) {
            if (word.startsWith("#") && word.length() > 1) {
                String hashtag = word.toLowerCase();

                // Get old frequency
                int oldFreq = hashtagToFreqMap.getOrDefault(hashtag, 0);
                int newFreq = oldFreq + 1;

                // Remove old entry from TreeSet if it exists
                if (oldFreq > 0) {
                    topN.remove(new HashtagEntry(hashtag, oldFreq));
                }

                // Update map
                hashtagToFreqMap.put(hashtag, newFreq);

                // Add new entry
                topN.add(new HashtagEntry(hashtag, newFreq));

                // Keep TreeSet size ≤ N
                if (topN.size() > n) {
                    topN.pollLast(); // remove lowest priority entry
                }
            }
        }
    }

    /* Traverse TreeSet: O(N) */
    public List<String> getTopNHashtags() {
        List<String> result = new ArrayList<>();
        for (HashtagEntry entry : topN) {
            result.add(entry.hashtag);
        }
        return result;
    }

}

/*
 * /*
 * Comparison of Solutions: Heap-Based (Current) vs. TreeSet-Based Optimized
 *
 * ✅ Current Heap-Based Solution:
 * --------------------------------
 * - Maintains: HashMap<String, Integer> to track frequencies.
 * - On getTopNHashtags(n):
 * → Builds a min-heap of size N from scratch using all hashtag frequencies.
 * → Time Complexity: O(H log N), where H = total unique hashtags.
 * - Tweet Processing Time: O(W), where W = number of words in tweet.
 * - Space Complexity: O(H)
 * - Pros:
 * ✔️ Simpler to implement.
 * ✔️ Efficient if top-N queries are infrequent.
 * - Cons:
 * ❌ Slower if top-N is queried frequently.
 * ❌ Recomputes heap every time.
 *
 * ✅ TreeSet-Based Optimized Solution:
 * -------------------------------------
 * - Maintains:
 * → HashMap<String, Integer> for frequencies.
 * → TreeSet<HashtagEntry> to always maintain top-N hashtags.
 * - Tweet Processing Time: O(W log N) per tweet (due to TreeSet insert/remove).
 * - getTopNHashtags(n): O(N) → directly returns from TreeSet.
 * - Space Complexity: O(H + N)
 * - Pros:
 * ✔️ Top-N queries are fast and real-time.
 * ✔️ Efficient for frequent or continuous queries (e.g., live UI).
 * - Cons:
 * ❌ Slightly more complex.
 * ❌ TreeSet maintenance overhead per tweet.
 *
 * ✅ When to Use Which?
 * ---------------------
 * - Use Heap-Based:
 * → When top-N queries are rare.
 * → When simplicity and lower memory usage are preferred.
 * - Use TreeSet-Based:
 * → When top-N results are needed frequently or continuously.
 * → In real-time systems (e.g., live trending hashtags).
 *
 * ✅ Summary:
 * ----------
 * - Heap-Based: Best for batch mode or low query frequency.
 * - TreeSet-Based: Best for real-time and high query frequency use cases.
 */

class SlidingWindowHashtags {

    class Tweet {

        Set<String> hashtags;
        long timestamp;

    }

    private final long windowSizeMillis;
    /*
     * ✅ Why use a Deque at all?
     * We use a Deque (double-ended queue) because:
     * - We need to add new tweets at the end (offerLast()).
     * - We need to evict old tweets from the front (pollFirst()) as they age out of
     * the time window.
     * This is a classic sliding window use case that benefits from:
     * - O(1) insertion/removal from both ends
     */
    private final Deque<Tweet> tweets;
    private final Map<String, Integer> hashtagToFreqMap;

    public SlidingWindowHashtags(long windowSizeMillis) {
        this.hashtagToFreqMap = new HashMap<>();
        this.tweets = new ArrayDeque<>();
        this.windowSizeMillis = windowSizeMillis;
    }

    public List<String> getTopNHashtags(int n) {

        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>((a, b) -> {
            if (a.getValue().equals(b.getValue())) {
                return b.getKey().compareTo(a.getKey());
            }
            return Integer.compare(a.getValue(), b.getValue());
        });

        for (Map.Entry<String, Integer> entry : this.hashtagToFreqMap.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > n) {
                minHeap.poll();
            }
        }

        List<String> result = new LinkedList<>();
        /*
         * the total time complexity of that loop is O(n log n) due to polling from the
         * heap (each poll() is O(log n)), but each addFirst() is just O(1).
         */
        while (!minHeap.isEmpty()) {
            result.addFirst(minHeap.poll().getKey());
        }

        return result;
    }

    public void addTweet(Tweet tweet) {
        this.tweets.offerLast(tweet);
        this.updateHashtags(tweet.hashtags);
        this.cleanOldTweets(tweet.timestamp);
    }

    private void cleanOldTweets(long currentTimestamp) {
        while (!this.tweets.isEmpty() && currentTimestamp - this.tweets.peekFirst().timestamp > this.windowSizeMillis) {
            Tweet oldTweet = this.tweets.pollFirst();
            for (String hashtag : oldTweet.hashtags) {
                // this.hashtagToFreqMap.put(hashtag, this.hashtagToFreqMap.get(hashtag) - 1);
                this.hashtagToFreqMap.computeIfPresent(hashtag, (k, v) -> v - 1);
                if (this.hashtagToFreqMap.get(hashtag) == 0) {
                    this.hashtagToFreqMap.remove(hashtag);
                }
                // this.hashtagToFreqMap.computeIfPresent(hashtag, (key, value) -> value == 0 ?
                // null : value);
                // this.hashtagToFreqMap.remove(hashtag, 0);
            }
        }
    }

    private void updateHashtags(Set<String> hashtags) {
        for (String hashtag : hashtags) {
            this.hashtagToFreqMap.put(hashtag, this.hashtagToFreqMap.getOrDefault(hashtag, 0) + 1);
        }
    }

}

/*
 * What is the time window? Should we find the top N hashtags of all time, or in
 * the last hour, day, week? This is a crucial distinction for a stream. If
 * there's no time window, the problem is much simpler.
 * 
 */

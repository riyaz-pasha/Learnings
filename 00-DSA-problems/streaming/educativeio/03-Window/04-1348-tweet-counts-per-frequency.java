/**
 * 0. ## Problem Statement
 * A social media company wants to monitor tweet activity over partitioned time chunks 
 * based on a given frequency: "minute" (60s), "hour" (3600s), or "day" (86400s).
 * 
 * Implement `TweetCounts`:
 * - `TweetCounts()`: Initializes the object.
 * - `void recordTweet(String tweetName, int time)`: Records a tweet at a given time (in seconds).
 * - `List<Integer> getTweetCountsPerFrequency(String freq, String tweetName, int startTime, int endTime)`: 
 *   Returns a list of counts of tweets per time chunk in the range [startTime, endTime].
 * 
 * Note: Timestamps in `recordTweet` are NOT guaranteed to be in chronological order.
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Are the recorded times guaranteed to arrive in chronological order?"
 *    -> The example explicitly shows 0, 60, then 10. They arrive out of order. We must handle sorting or dynamic ordering.
 * 2. "Can multiple tweets with the same name arrive at the exact same timestamp?"
 *    -> Yes. This dictates that we can't use a simple `TreeSet`; we need a `TreeMap` with a frequency count or a `List`.
 * 3. "If there are no tweets for a given user in a requested chunk, should that chunk show 0 or be omitted?"
 *    -> The problem expects every chunk from `startTime` to `endTime` to be present in the returned list, filling gaps with 0s.
 * 4. "Is `endTime` inclusive?"
 *    -> Yes, the window is exactly `[startTime, endTime]`.
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to 10,000 queries total, and times can arrive out of order.
 * 
 * --- Approach 1: The Brute Force HashMap of Lists ---
 * Idea: Map `tweetName -> ArrayList<Integer>`. On `recordTweet`, simply `list.add(time)`.
 * On `getTweetCounts...`, allocate a result array, then iterate through the ENTIRE list of times for 
 * that user. If `time >= startTime && time <= endTime`, calculate its chunk index and increment.
 * - Why it works: Simple to write, ignores ordering during insertion.
 * - Why it's slightly inefficient: We scan thousands of timestamps that might be completely outside 
 *   the requested `[startTime, endTime]` window. 
 * Time Complexity: `record`: O(1). `query`: O(N) where N is the total number of tweets by that user.
 * Space Complexity: O(N) to store all tweets.
 * 
 * --- Approach 2: TreeMap for Time Range Extraction (Optimal) ---
 * Idea: Map `tweetName -> TreeMap<Integer, Integer>`, mapping the exact Timestamp to its Frequency Count.
 * On `recordTweet`, update the count in the TreeMap. 
 * On `getTweetCounts...`, use the highly optimized `TreeMap.subMap(startTime, true, endTime, true)` 
 * to fetch ONLY the timestamps that fall within the requested window. 
 * - Why it works: `subMap` is practically instantaneous, providing a bounded view. We only iterate 
 *   over the valid timestamps, ignoring years of historical data outside the window.
 * - What work is avoided: Scanning the entire history of a user's tweets.
 * Time Complexity: `record`: O(log N) — because TreeMap insertion takes logarithmic time.
 * `query`: O(K) — where K is the number of UNIQUE timestamps inside the requested time window, plus O(log N) to find the boundaries.
 * Space Complexity: O(N) — where N is the number of unique timestamps per user.
 * 
 * Verdict: I would write Approach 2 (TreeMap). Although Approach 1 technically passes because the 
 * constraints (10,000 calls) are small, Approach 2 demonstrates a deeper understanding of how to 
 * optimize range queries in Java (`subMap`) and handles theoretically massive histories efficiently.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - `startTime == endTime`: Handled flawlessly. The math gives `((0) / freq) + 1 = 1` bucket.
 * - Multiple tweets at the exact same timestamp: The TreeMap uses counts (`map.put(time, count + 1)`), 
 *   preventing duplicates from overwriting each other.
 * - `tweetName` hasn't tweeted at all: Return a list of 0s sized exactly to the requested chunks.
 * - Chunk doesn't neatly divide the window: The problem states "the last chunk may be shorter". 
 *   Our math `(time - startTime) / freq` naturally dumps trailing times into the final bucket without caring about its exact physical length.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * **Chunk Index Math**: You don't need complex `if/else` boundaries to figure out where a time belongs. 
 * Because all chunks (except potentially the last one) are exactly `interval` seconds wide, 
 * the index for any time `t` relative to `startTime` is simply `(t - startTime) / interval`.
 * 
 * -- ASCII Diagram (Chunk Indexing) --
 * Window: startTime = 10, endTime = 140. freq = "minute" (60s).
 * Number of chunks = ((140 - 10) / 60) + 1 = (130 / 60) + 1 = 2 + 1 = 3 buckets.
 * 
 * Bucket 0: [10,  69] 
 * Bucket 1: [70, 129]
 * Bucket 2: [130, 140]  <-- Shorter trailing chunk
 * 
 * Tweet at t = 10:   (10 - 10) / 60 = 0   -> Bucket 0
 * Tweet at t = 69:   (69 - 10) / 60 = 0   -> Bucket 0
 * Tweet at t = 75:   (75 - 10) / 60 = 1   -> Bucket 1
 * Tweet at t = 140: (140 - 10) / 60 = 2   -> Bucket 2
 * 
 * -- Dry run (TreeMap Approach) --
 * recordTweet("tweet3", 0), record("tweet3", 60), record("tweet3", 10)
 * map state: "tweet3" -> { 0:1, 10:1, 60:1 }
 * 
 * getTweetCounts("minute", "tweet3", 0, 59)
 * 1. interval = 60
 * 2. size = ((59 - 0) / 60) + 1 = 1 bucket. buckets = [0]
 * 3. subMap(0, 59) yields -> { 0:1, 10:1 }
 * 4. t=0,  idx = (0-0)/60 = 0. buckets[0] += 1 (buckets = [1])
 * 5. t=10, idx = (10-0)/60 = 0. buckets[0] += 1 (buckets = [2])
 * 6. Returns [2]
 * 
 * -- Pitfalls --
 * 1. Blindly iterating over a user's entire tweet history for every query. If a user has a million 
 *    tweets, querying a 60-second window shouldn't take a million loop iterations.
 * 2. `TreeSet` instead of `TreeMap`. If you use a Set, identical timestamps for the same tweet 
 *    collapse into a single entry, losing data.
 * 
 * -- Pattern recognition --
 * When you see "Extract data in a specific range `[start, end]`" and data arrives unordered, 
 * think **TreeMap / Red-Black Tree** and its `.subMap()` method.
 * 
 * -- Interview script --
 * "Since the times arrive out of order and queries request specific time windows, iterating over a 
 * user's entire history for a narrow time-window query is inefficient. I'll use a `HashMap` mapping 
 * the tweet name to a `TreeMap`. The `TreeMap` keeps timestamps sorted and maps them to their frequency count. 
 * During a query, I determine the interval size and create a pre-sized array for the chunks. Then, I use 
 * `TreeMap.subMap(startTime, endTime)` to iterate *only* over the tweets that actually fall in the window. 
 * I calculate the chunk index using `(time - startTime) / interval` and populate the array."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if `endTime - startTime` could be massive (e.g., millions of seconds) but the user only tweeted twice?
 * A: Our result array would be huge (mostly zeros) just to return two tweets. We might need to change 
 *    the API to return a sparse representation (e.g., `Map<ChunkIndex, Count>`) rather than a dense array.
 * 
 * Q: How would you handle this in a highly concurrent environment?
 * A: I would replace the `HashMap` with a `ConcurrentHashMap` and the `TreeMap` with a 
 *    `ConcurrentSkipListMap`, which allows lock-free concurrent reads and updates to the sorted map.
 * =========================================================================================
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TweetCountsStudyNote {

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH: HashMap of TreeMaps (O(K) Queries using subMap)
    // ----------------------------------------------------------------------
    static class TweetCounts {
        
        // Maps a Tweet Name to a TreeMap.
        // The TreeMap maps a specific Timestamp (Integer) to the Count of tweets at that exact time (Integer).
        // Using a TreeMap automatically keeps timestamps sorted and allows O(log N) range extractions.
        private final Map<String, TreeMap<Integer, Integer>> userTweets;

        public TweetCounts() {
            userTweets = new HashMap<>();
        }

        public void recordTweet(String tweetName, int time) {
            // Get or create the TreeMap for this specific tweetName
            userTweets.putIfAbsent(tweetName, new TreeMap<>());
            TreeMap<Integer, Integer> timeMap = userTweets.get(tweetName);
            
            // Increment the count for this specific timestamp to handle identical simultaneous tweets
            timeMap.put(time, timeMap.getOrDefault(time, 0) + 1);
        }

        public List<Integer> getTweetCountsPerFrequency(String freq, String tweetName, int startTime, int endTime) {
            // 1. Determine the exact length of the time chunk in seconds
            int intervalSeconds = 60; // Default to minute
            if (freq.equals("hour")) {
                intervalSeconds = 3600;
            } else if (freq.equals("day")) {
                intervalSeconds = 86400;
            }
            
            // 2. Calculate the total number of chunks needed.
            // Example: [0, 59] / 60 = 0. We need 1 bucket. +1 fixes the 0-index.
            // Example: [0, 60] / 60 = 1. We need 2 buckets (one for 0-59, one for 60).
            int totalChunks = ((endTime - startTime) / intervalSeconds) + 1;
            int[] buckets = new int[totalChunks];
            
            // 3. Early exit if this tweetName has never been recorded
            if (!userTweets.containsKey(tweetName)) {
                return arrayToList(buckets);
            }
            
            TreeMap<Integer, Integer> timeMap = userTweets.get(tweetName);
            
            // 4. Extract ONLY the relevant window. 
            // subMap(fromKey, fromInclusive, toKey, toInclusive) is an O(log N) operation.
            Map<Integer, Integer> validWindow = timeMap.subMap(startTime, true, endTime, true);
            
            // 5. Populate the buckets
            for (Map.Entry<Integer, Integer> entry : validWindow.entrySet()) {
                int tweetTime = entry.getKey();
                int count = entry.getValue();
                
                // Pure math: relative distance from start divided by chunk size gives the exact array index.
                int bucketIndex = (tweetTime - startTime) / intervalSeconds;
                buckets[bucketIndex] += count;
            }
            
            return arrayToList(buckets);
        }
        
        // Helper method to convert the primitive int array into the required List<Integer>
        private List<Integer> arrayToList(int[] arr) {
            List<Integer> list = new ArrayList<>(arr.length);
            for (int val : arr) {
                list.add(val);
            }
            return list;
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        TweetCounts tweetCounts = new TweetCounts();
        
        System.out.println("Recording tweets at t=0, t=60, t=10...");
        tweetCounts.recordTweet("tweet3", 0);
        tweetCounts.recordTweet("tweet3", 60);
        tweetCounts.recordTweet("tweet3", 10);
        
        // Query: [0, 59]
        // Chunks: [0, 59] (contains t=0, t=10) -> count = 2
        System.out.println("Minute freq [0, 59]: " + 
            tweetCounts.getTweetCountsPerFrequency("minute", "tweet3", 0, 59) + 
            " (Expected: [2])");
        
        // Query: [0, 60]
        // Chunks: [0, 59] (contains 0, 10 -> count 2), [60, 60] (contains 60 -> count 1)
        System.out.println("Minute freq [0, 60]: " + 
            tweetCounts.getTweetCountsPerFrequency("minute", "tweet3", 0, 60) + 
            " (Expected: [2, 1])");
            
        System.out.println("Recording tweet at t=120...");
        tweetCounts.recordTweet("tweet3", 120);
        
        // Query: hour [0, 210]. 
        // 1 hour is 3600 seconds. The entire range [0, 210] falls securely in Bucket 0.
        // It contains 0, 10, 60, 120 -> count = 4.
        System.out.println("Hour freq [0, 210]: " + 
            tweetCounts.getTweetCountsPerFrequency("hour", "tweet3", 0, 210) + 
            " (Expected: [4])");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: TreeMap Range Queries + Index Math.
 * - Key observation: When data arrives out of order but queries demand localized ranges, 
 *   a `TreeMap` allows you to extract just the active window, avoiding full O(N) scans.
 * - Worth memorizing: The bucketing math: `bucketIndex = (time - startTime) / intervalSize`. 
 *   It effortlessly groups events into time chunks without a single `if/else` boundary check.
 * - Most common trap: Using a `TreeSet` to store timestamps, which overwrites/deletes duplicate 
 *   tweets occurring at the same exact second.
 * - One-line mental trigger: "Out of order events queried by range? -> TreeMap + subMap()."
 * =========================================================================================
 */










































import java.util.*;

class TweetCounts {

    // Each tweet name maps to its recorded timestamps.
    //
    // Example:
    // {
    //     "tweet3" -> [0, 60, 10, 120],
    //     "tweet5" -> [10, 30, 90]
    // }
    //
    // HashMap provides O(1) average lookup by tweet name.
    private final Map<String, List<Integer>> tweets = new HashMap<>();

    public TweetCounts() {
        // No initialization is necessary.
    }

    public void recordTweet(String tweetName, int time) {

        // If the tweet name does not exist, create its timestamp list.
        // Otherwise, retrieve the existing list.
        //
        // Append the timestamp in O(1) amortized time.
        //
        // We intentionally do not maintain sorted order because
        // timestamps can arrive out of chronological order.
        tweets.computeIfAbsent(tweetName, key -> new ArrayList<>())
              .add(time);
    }

    public List<Integer> getTweetCountsPerFrequency(
            String freq,
            String tweetName,
            int startTime,
            int endTime) {

        // Convert frequency into seconds.
        //
        // switch expression is available in modern Java.
        int chunkSize = switch (freq) {
            case "minute" -> 60;
            case "hour" -> 3600;
            case "day" -> 86400;
            default -> throw new IllegalArgumentException(
                    "Unsupported frequency: " + freq
            );
        };

        // Calculate how many buckets are needed.
        //
        // Formula:
        // bucketCount = (endTime - startTime) / chunkSize + 1
        //
        // Why +1?
        // Because both startTime and endTime are inclusive.
        //
        // Example:
        // startTime = 0, endTime = 60, chunkSize = 60
        //
        // Bucket 0: [0, 59]
        // Bucket 1: [60, 60]
        //
        // Therefore, we need 2 buckets.
        int bucketCount = (endTime - startTime) / chunkSize + 1;

        // Initialize all buckets to zero.
        //
        // This is important because some intervals may not contain tweets.
        // We must still return zero for those intervals.
        int[] buckets = new int[bucketCount];

        // Retrieve timestamps for the requested tweet name.
        //
        // If no tweets exist for this name, return all-zero buckets.
        List<Integer> timestamps = tweets.get(tweetName);

        if (timestamps == null) {

            // Arrays.stream(...).boxed() converts int[] into Stream<Integer>.
            // The returned list contains one zero for every bucket.
            return Arrays.stream(buckets)
                         .boxed()
                         .toList();
        }

        // Process every recorded timestamp exactly once.
        for (int timestamp : timestamps) {

            // Ignore tweets outside the requested inclusive range.
            if (timestamp < startTime || timestamp > endTime) {
                continue;
            }

            // Calculate which bucket contains this timestamp.
            //
            // Integer division automatically maps timestamps to chunks.
            //
            // Example:
            // startTime = 0, chunkSize = 60
            //
            // timestamp = 10  => bucket 0
            // timestamp = 59  => bucket 0
            // timestamp = 60  => bucket 1
            // timestamp = 120 => bucket 2
            //
            // Subtracting startTime is essential because buckets
            // are relative to the query's starting timestamp.
            int bucketIndex = (timestamp - startTime) / chunkSize;

            // Increment the corresponding bucket.
            buckets[bucketIndex]++;
        }

        // Convert int[] into List<Integer>.
        //
        // toList() returns an unmodifiable list, which is perfectly
        // acceptable because the problem only requires returning counts.
        return Arrays.stream(buckets)
                     .boxed()
                     .toList();
    }
}




































import java.util.*;

class TweetCounts {

    // TreeMap keeps timestamps sorted automatically.
    //
    // timestamp -> number of tweets at that timestamp
    //
    // Example:
    // {
    //     0   -> 1,
    //     10  -> 1,
    //     60  -> 2,
    //     120 -> 1
    // }
    //
    // The count handles multiple tweets at the same timestamp.
    private final Map<String, NavigableMap<Integer, Integer>> tweets =
            new HashMap<>();

    public TweetCounts() {
    }

    public void recordTweet(String tweetName, int time) {

        // Create a TreeMap for a new tweet name.
        NavigableMap<Integer, Integer> timeline =
                tweets.computeIfAbsent(
                        tweetName,
                        key -> new TreeMap<>()
                );

        // Increment the frequency of the timestamp.
        //
        // TreeMap insertion/update takes O(log M),
        // where M is the number of distinct timestamps for this name.
        timeline.merge(time, 1, Integer::sum);
    }

    public List<Integer> getTweetCountsPerFrequency(
            String freq,
            String tweetName,
            int startTime,
            int endTime) {

        int chunkSize = switch (freq) {
            case "minute" -> 60;
            case "hour" -> 3600;
            case "day" -> 86400;
            default -> throw new IllegalArgumentException(
                    "Unsupported frequency: " + freq
            );
        };

        int bucketCount = (endTime - startTime) / chunkSize + 1;

        int[] buckets = new int[bucketCount];

        NavigableMap<Integer, Integer> timeline = tweets.get(tweetName);

        if (timeline == null) {
            return Arrays.stream(buckets).boxed().toList();
        }

        // subMap includes both startTime and endTime.
        //
        // TreeMap navigates to the requested range efficiently.
        //
        // Its range view contains only timestamps:
        // startTime <= timestamp <= endTime.
        NavigableMap<Integer, Integer> range =
                timeline.subMap(startTime, true, endTime, true);

        // Iterate only through timestamps inside the requested range.
        for (Map.Entry<Integer, Integer> entry : range.entrySet()) {

            int timestamp = entry.getKey();

            // Number of tweets at this exact timestamp.
            int count = entry.getValue();

            // Calculate the appropriate bucket.
            int bucketIndex = (timestamp - startTime) / chunkSize;

            // Add all tweets recorded at this timestamp.
            buckets[bucketIndex] += count;
        }

        return Arrays.stream(buckets).boxed().toList();
    }
}


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

class BruteForceTopK {

    private Map<String, Integer> frequencyMap;
    private int k;

    public BruteForceTopK(int k) {
        this.k = k;
        this.frequencyMap = new HashMap<>();
    }

    public void addHashtag(String hashtag) {
        frequencyMap.put(hashtag, frequencyMap.getOrDefault(hashtag, 0) + 1);
    }

    public List<String> getTopK() {
        // Create a list from the map entries
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(frequencyMap.entrySet());

        // Sort the list by frequency in descending order
        entryList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // Extract the top k hashtags
        List<String> topKHashtags = new ArrayList<>();
        for (int i = 0; i < Math.min(k, entryList.size()); i++) {
            topKHashtags.add(entryList.get(i).getKey());
        }
        return topKHashtags;
    }

    /*
     * Time Complexity:
     * - addHashtag(): O(1) on average for a hash map.
     * - getTopK(): The dominant operation is sorting the list of all unique
     * hashtags. Let N be the total number of unique hashtags seen so far. Sorting
     * takes O(N logN). This is inefficient, as the number of unique hashtags can
     * grow very large.
     * 
     * Space Complexity:
     * O(N) to store the frequencyMap and the entryList for sorting.
     */

}

class OptimalTopK {

    private Map<String, Integer> frequencyMap;
    private int k;

    public OptimalTopK(int k) {
        this.k = k;
        this.frequencyMap = new HashMap<>();
    }

    public void addHashtag(String hashtag) {
        frequencyMap.put(hashtag, frequencyMap.getOrDefault(hashtag, 0) + 1);
    }

    public List<String> getTopK() {
        // A min-heap to store the top k frequent hashtags.
        // The comparator ensures that the entry with the smallest frequency is at the
        // top.
        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>(
                (e1, e2) -> e1.getValue().compareTo(e2.getValue()));

        // Iterate through the frequency map
        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            minHeap.offer(entry);
            // If the heap size exceeds k, remove the element with the lowest frequency
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        // Extract the hashtags from the min-heap
        List<String> topKHashtags = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            topKHashtags.add(minHeap.poll().getKey());
        }

        // The order of hashtags from the heap is not sorted by frequency.
        // If we want a sorted list, we can reverse it.
        Collections.reverse(topKHashtags);

        return topKHashtags;
    }

    /*
     * Time Complexity:
     * - addHashtag(): O(1) on average.
     * - getTopK():
     * -- Iterating through all unique hashtags takes O(N) time, where N is the
     * number of unique hashtags.
     * -- For each of these N hashtags, we perform offer() and potentially poll() on
     * the heap. Both of these operations take O(logk) time.
     * -- Therefore, the total time for getTopK() is O(N logk). This is
     * significantly better than the brute-force O(N logN) when k<<N.
     * 
     * Space Complexity:
     * - O(N) for the frequencyMap to store all unique hashtags.
     * - O(k) for the minHeap.
     * The total space complexity is dominated by the frequencyMap, so it's O(N).
     */

}

class SlidingWindowTopK {

    private Map<String, Integer> frequencyMap;
    private Queue<String> hashtagWindow;
    private int k;
    private int windowSize;

    public SlidingWindowTopK(int k, int windowSize) {
        this.k = k;
        this.windowSize = windowSize;
        this.frequencyMap = new HashMap<>();
        this.hashtagWindow = new LinkedList<>();
    }

    public void addHashtag(String hashtag) {
        // Add the new hashtag to the window
        hashtagWindow.offer(hashtag);
        frequencyMap.put(hashtag, frequencyMap.getOrDefault(hashtag, 0) + 1);

        // Check if the window is full and needs to slide
        if (hashtagWindow.size() > windowSize) {
            String oldestHashtag = hashtagWindow.poll();
            int currentCount = frequencyMap.get(oldestHashtag);
            if (currentCount > 1) {
                frequencyMap.put(oldestHashtag, currentCount - 1);
            } else {
                // If count is 1, remove it completely
                frequencyMap.remove(oldestHashtag);
            }
        }
    }

    public List<String> getTopK() {
        // Create a min-heap to find the top K from the current window
        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>(
                (e1, e2) -> e1.getValue().compareTo(e2.getValue()));

        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        // Extract and reverse the list for sorted output
        List<String> topKHashtags = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            topKHashtags.add(minHeap.poll().getKey());
        }
        Collections.reverse(topKHashtags);

        return topKHashtags;
    }

    /*
     * Time Complexity:
     * - addHashtag(): O(1) on average. All operations (offer, put, poll, get,
     * remove)
     * are constant time for a hash map and a linked list queue.
     * - getTopK():
     * -- Let N be the number of unique hashtags in the current window. In the worst
     * case, N is at most windowSize.
     * -- Iterating through frequencyMap takes O(N).
     * -- Each heap operation takes O(logk).
     * -- The total time is O(N logk), where N <= windowSize. This is efficient
     * because N is now bounded, unlike in the infinite stream case.
     * 
     * Space Complexity:
     * -- frequencyMap: Stores at most windowSize unique hashtags. So, O(
     * textwindowSize).
     * -- hashtagWindow: Stores up to windowSize total hashtags. So, O(
     * textwindowSize).
     * -- minHeap: Stores up to k elements. So, O(k).
     * -- The total space complexity is O(
     * textwindowSize).
     */

}

class TrendingHashtagsFastGet {
    private final int k;
    private final Map<String, Integer> freqMap = new HashMap<>();
    private final PriorityQueue<Hashtag> minHeap;
    private final Set<String> inHeap = new HashSet<>();
    private final List<String> cachedTopK = new ArrayList<>();

    private static class Hashtag {
        String tag;
        int freq;

        Hashtag(String tag, int freq) {
            this.tag = tag;
            this.freq = freq;
        }
    }

    public TrendingHashtagsFastGet(int k) {
        this.k = k;
        this.minHeap = new PriorityQueue<>((a, b) -> {
            int cmp = Integer.compare(a.freq, b.freq);
            if (cmp == 0)
                return b.tag.compareTo(a.tag); // reverse lex
            return cmp;
        });
    }

    public void insert(String hashtag) {
        String tag = hashtag.toLowerCase();
        int newFreq = freqMap.getOrDefault(tag, 0) + 1;
        freqMap.put(tag, newFreq);

        // Remove old entry if it exists in heap
        if (inHeap.contains(tag)) {
            minHeap.removeIf(h -> h.tag.equals(tag)); // O(K), acceptable for small K
            inHeap.remove(tag);
        }

        // Add new or updated hashtag
        if (minHeap.size() < k) {
            minHeap.offer(new Hashtag(tag, newFreq));
            inHeap.add(tag);
        } else if (minHeap.peek().freq < newFreq ||
                (minHeap.peek().freq == newFreq && tag.compareTo(minHeap.peek().tag) < 0)) {
            Hashtag removed = minHeap.poll();
            inHeap.remove(removed.tag);

            minHeap.offer(new Hashtag(tag, newFreq));
            inHeap.add(tag);
        }

        // Update cached top K list (sorted)
        cachedTopK.clear();
        List<Hashtag> list = new ArrayList<>(minHeap);
        list.sort((a, b) -> {
            int cmp = Integer.compare(b.freq, a.freq);
            if (cmp == 0)
                return a.tag.compareTo(b.tag);
            return cmp;
        });
        for (Hashtag h : list) {
            cachedTopK.add(h.tag);
        }
    }

    public List<String> getTopK() {
        return new ArrayList<>(cachedTopK); // O(1) from user’s POV
    }

    /*
     * | Operation | Time Complexity | Notes |
     * | ----------- | --------------- | ------------------------------- |
     * | `insert()` | O(K) | Because of `minHeap.removeIf` |
     * | `getTopK()` | O(1) | Instant return from cached list |
     * | Space | O(N + K) | N = unique hashtags |
     */

}

/* Merge multiple user feeds sorted by recency into one combined sorted feed. */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

class FeedItem {

    String content;
    long timestamp;

}

class FeedPointer implements Comparable<FeedPointer> {

    FeedItem feedItem;
    int feedIndex;
    int itemIndex;

    FeedPointer(FeedItem feedItem, int feedIndex, int itemIndex) {
        this.feedItem = feedItem;
        this.feedIndex = feedIndex;
        this.itemIndex = itemIndex;
    }

    @Override
    public int compareTo(FeedPointer other) {
        return Long.compare(other.feedItem.timestamp, this.feedItem.timestamp);
    }

}

class FeedMerger {

    /*
     * Let:
     * k = number of feeds
     * n = total number of feed items across all feeds (i.e. n = sum of lengths of
     * all feeds)
     */

    public List<FeedItem> mergeFeeds(List<List<FeedItem>> feeds) {
        List<FeedItem> mergedFeed = new ArrayList<>(); // Space: O(n)

        PriorityQueue<FeedPointer> maxHeap = new PriorityQueue<>(); // Space: O(k)

        /*
         * You add at most one item per feed into the heap initially → O(k log k) (since
         * each insert into heap is O(log k) and you do it k times).
         */
        for (int feedIndex = 0; feedIndex < feeds.size(); feedIndex++) {
            if (!feeds.get(feedIndex).isEmpty()) {
                maxHeap.offer(new FeedPointer(feeds.get(feedIndex).get(0), feedIndex, 0));
            }
        }

        /*
         * You process each item exactly once from all feeds → n items total.
         * For each item, you:
         * - poll from the heap → O(log k)
         * - maybe insert the next item from that feed into the heap → O(log k)
         * So each item involves up to two heap operations → total cost: O(n log k)
         */
        while (!maxHeap.isEmpty()) {
            FeedPointer latest = maxHeap.poll();
            mergedFeed.add(latest.feedItem);

            int nextIndex = latest.itemIndex + 1;
            if (nextIndex < feeds.get(latest.feedIndex).size()) {
                FeedItem nextFeedItem = feeds.get(latest.feedIndex).get(nextIndex);
                maxHeap.offer(new FeedPointer(nextFeedItem, latest.feedIndex, nextIndex));
            }
        }

        return mergedFeed;
    }

}

// A wrapper class for heap nodes, now storing an iterator reference
class HeapNodeWithIterator implements Comparable<HeapNodeWithIterator> {

    FeedItem item;
    Iterator<FeedItem> iterator;

    public HeapNodeWithIterator(FeedItem item, Iterator<FeedItem> iterator) {
        this.item = item;
        this.iterator = iterator;
    }

    @Override
    public int compareTo(HeapNodeWithIterator other) {
        return Long.compare(other.item.timestamp, this.item.timestamp);
    }

}

class StreamBasedFeedMerger {

    public List<FeedItem> mergeFeeds(List<Iterator<FeedItem>> feedIterators) {
        List<FeedItem> mergedFeed = new ArrayList<>();

        PriorityQueue<HeapNodeWithIterator> maxHeap = new PriorityQueue<>(); // Space: O(k)

        // Populate the heap with the first item from each iterator
        for (Iterator<FeedItem> iterator : feedIterators) {
            if (iterator.hasNext()) {
                maxHeap.offer(new HeapNodeWithIterator(iterator.next(), iterator));
            }
        }

        while (!maxHeap.isEmpty()) {
            HeapNodeWithIterator latestNode = maxHeap.poll();
            mergedFeed.add(latestNode.item);

            if (latestNode.iterator.hasNext()) {
                maxHeap.offer(new HeapNodeWithIterator(latestNode.iterator.next(), latestNode.iterator));
            }
        }

        return mergedFeed;
    }
}

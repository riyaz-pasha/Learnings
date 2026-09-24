import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

class Post {
    long timestamp;
    String content;
}

class FeedNode {
    Post post;
    int feedIndex;
    int elementIndex;

    public FeedNode(Post post, int feedIndex, int elementIndex) {
        this.post = post;
        this.feedIndex = feedIndex;
        this.elementIndex = elementIndex;
    }
}

class FeedMerger {
    public List<Post> mergeFeeds(List<List<Post>> feeds, int pageSize) {
        // Max-Heap because we want the most recent (highest timestamp) first
        PriorityQueue<FeedNode> maxHeap = new PriorityQueue<>(
            (a, b) -> Long.compare(b.post.timestamp, a.post.timestamp)
        );

        // 1. Initial Fill: Add the first post from each feed
        for (int i = 0; i < feeds.size(); i++) {
            if (!feeds.get(i).isEmpty()) {
                maxHeap.offer(new FeedNode(feeds.get(i).get(0), i, 0));
            }
        }

        List<Post> mergedFeed = new ArrayList<>();
        
        // 2. Merge until pageSize is reached or heap is empty
        while (!maxHeap.isEmpty() && mergedFeed.size() < pageSize) {
            FeedNode current = maxHeap.poll();
            mergedFeed.add(current.post);

            // 3. Move to the next item in the SAME feed
            int nextElementIdx = current.elementIndex + 1;
            if (nextElementIdx < feeds.get(current.feedIndex).size()) {
                maxHeap.offer(new FeedNode(
                    feeds.get(current.feedIndex).get(nextElementIdx), 
                    current.feedIndex, 
                    nextElementIdx
                ));
            }
        }

        return mergedFeed;
    }
}

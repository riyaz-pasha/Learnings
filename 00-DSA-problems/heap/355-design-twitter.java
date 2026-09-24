import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a DESIGN problem testing:
 * 1. Object-oriented design skills
 * 2. Data structure selection
 * 3. Understanding of time/space complexity trade-offs
 * 4. Ability to handle follow/unfollow relationships
 * 5. Merge operation for getting recent tweets
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. We need a TIMESTAMP or COUNTER for tweets to order them chronologically
 *    - Can't rely on tweetId alone as it may not be sequential
 *    - Use a global counter that increments with each tweet
 * 
 * 2. User follows form a GRAPH structure
 *    - Each user has a set of followees
 *    - Need O(1) lookup for follow/unfollow operations → Use HashSet
 * 
 * 3. Getting news feed requires MERGING multiple sorted lists
 *    - Each user's tweets are in chronological order
 *    - Need to merge tweets from user + all followees
 *    - This is a classic K-way merge problem → Use PriorityQueue (max heap)
 * 
 * 4. Edge cases to consider:
 *    - User following themselves (problem states this won't happen)
 *    - Unfollowing someone not followed
 *    - Getting feed when no tweets exist
 *    - Getting feed with < 10 tweets available
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Start by clarifying requirements
 *    - Do tweets have unique IDs? (Yes)
 *    - Can users follow themselves? (No)
 *    - What's the max number of followees?
 *    - What happens if we unfollow someone we don't follow?
 * 
 * 2. Discuss data structures
 *    - Explain why HashMap for users
 *    - Explain why HashSet for followers
 *    - Explain why List for tweets per user
 *    - Explain why PriorityQueue for merging
 * 
 * 3. Walk through time complexity
 *    - postTweet: O(1)
 *    - follow/unfollow: O(1)
 *    - getNewsFeed: O(N log K) where N=total tweets, K=number of users
 * 
 * 4. Discuss optimizations
 *    - Could use a global sorted list but would make posting O(N)
 *    - Current approach optimizes for posting (most common operation)
 *    - Could cache news feeds but would need invalidation logic
 */

class Twitter {
    // Tweet class to store tweet information
    // Each tweet needs: tweetId, timestamp (for ordering), and userId (for filtering)
    private static class Tweet {
        int tweetId;
        int timestamp;  // Global counter to maintain chronological order
        int userId;
        
        public Tweet(int tweetId, int timestamp, int userId) {
            this.tweetId = tweetId;
            this.timestamp = timestamp;
            this.userId = userId;
        }
    }
    
    // Global timestamp counter - ensures chronological ordering across all tweets
    // Static ensures it's shared across all instances (though we typically have one)
    private int timestamp;
    
    // Map from userId to list of their tweets
    // Using ArrayList because:
    // - Need to maintain insertion order (chronological)
    // - Fast iteration for getNewsFeed
    // - Don't need random access or deletions
    private Map<Integer, List<Tweet>> userTweets;
    
    // Map from userId to set of followee IDs
    // Using HashSet because:
    // - O(1) add/remove for follow/unfollow
    // - O(1) contains check
    // - Don't care about order
    private Map<Integer, Set<Integer>> userFollows;
    
    /**
     * TIME: O(1) - just initializing empty data structures
     * SPACE: O(1) - constant space for initialization
     */
    public Twitter() {
        this.timestamp = 0;
        this.userTweets = new HashMap<>();
        this.userFollows = new HashMap<>();
    }
    
    /**
     * Posts a new tweet
     * 
     * TIME: O(1) - HashMap put, List add are both O(1) amortized
     * SPACE: O(1) - storing one tweet
     * 
     * DESIGN CHOICE: Store tweets per user rather than globally
     * - Makes getNewsFeed more complex but postTweet simpler
     * - Since posting is likely more frequent than reading, this is a good trade-off
     */
    public void postTweet(int userId, int tweetId) {
        // Create tweet with current timestamp
        Tweet tweet = new Tweet(tweetId, timestamp++, userId);
        
        // Initialize user's tweet list if first tweet
        // computeIfAbsent is cleaner than checking null
        userTweets.computeIfAbsent(userId, k -> new ArrayList<>()).add(tweet);
    }
    
    /**
     * Retrieves the 10 most recent tweets in user's news feed
     * 
     * ALGORITHM: K-way merge using max heap
     * 1. Collect all relevant users (self + followees)
     * 2. For each user, get their most recent tweet
     * 3. Use max heap to always get the most recent tweet
     * 4. After taking a tweet, add the next tweet from that user
     * 5. Repeat until we have 10 tweets or run out
     * 
     * TIME: O(N + K log K) where:
     *   - N = total number of tweets to consider
     *   - K = number of users (self + followees)
     *   - In worst case: O(N log K) for heap operations
     * 
     * SPACE: O(K) for the heap and result list
     * 
     * WHY MAX HEAP?
     * - We want most recent tweets first
     * - Heap gives us O(log K) for insert/extract
     * - Alternative would be to collect all tweets and sort: O(N log N)
     * - Our approach is better when we only need top 10
     */
    public List<Integer> getNewsFeed(int userId) {
        List<Integer> feed = new ArrayList<>();
        
        // Max heap - orders by timestamp (most recent first)
        // Using negative timestamp for max heap behavior with min heap
        PriorityQueue<Tweet> maxHeap = new PriorityQueue<>((a, b) -> b.timestamp - a.timestamp);
        
        // Map to track current index for each user's tweets
        // This allows us to iterate through each user's tweets
        Map<Integer, Integer> userTweetIndex = new HashMap<>();
        
        // Collect all relevant users: self + followees
        Set<Integer> relevantUsers = new HashSet<>();
        relevantUsers.add(userId);  // Always include own tweets
        
        // Add all followees if this user follows anyone
        if (userFollows.containsKey(userId)) {
            relevantUsers.addAll(userFollows.get(userId));
        }
        
        // Initialize heap with most recent tweet from each relevant user
        // WHY? Each user's tweets are already in chronological order
        // So we only need to track the "current" tweet from each user
        for (int user : relevantUsers) {
            List<Tweet> tweets = userTweets.get(user);
            if (tweets != null && !tweets.isEmpty()) {
                // Start from the end (most recent)
                int lastIndex = tweets.size() - 1;
                maxHeap.offer(tweets.get(lastIndex));
                userTweetIndex.put(user, lastIndex);
            }
        }
        
        // Extract up to 10 most recent tweets
        while (!maxHeap.isEmpty() && feed.size() < 10) {
            Tweet currentTweet = maxHeap.poll();
            feed.add(currentTweet.tweetId);
            
            // Add next tweet from this user (if exists)
            int currentIndex = userTweetIndex.get(currentTweet.userId);
            if (currentIndex > 0) {
                // Move to previous tweet (older)
                int nextIndex = currentIndex - 1;
                List<Tweet> userTweetList = userTweets.get(currentTweet.userId);
                maxHeap.offer(userTweetList.get(nextIndex));
                userTweetIndex.put(currentTweet.userId, nextIndex);
            }
        }
        
        return feed;
    }
    
    /**
     * User starts following another user
     * 
     * TIME: O(1) - HashSet add operation
     * SPACE: O(1) - storing one relationship
     * 
     * EDGE CASE: Problem guarantees followerId != followeeId
     * In production, we'd add: if (followerId == followeeId) return;
     */
    public void follow(int followerId, int followeeId) {
        // Initialize follower's set if first follow
        userFollows.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
    }
    
    /**
     * User stops following another user
     * 
     * TIME: O(1) - HashSet remove operation
     * SPACE: O(1) - no additional space
     * 
     * EDGE CASE: Safe to remove even if not following (Set.remove handles this)
     */
    public void unfollow(int followerId, int followeeId) {
        if (userFollows.containsKey(followerId)) {
            userFollows.get(followerId).remove(followeeId);
        }
    }
}

/**
 * COMPLEXITY SUMMARY
 * ==================
 * postTweet:    O(1) time, O(1) space
 * follow:       O(1) time, O(1) space
 * unfollow:     O(1) time, O(1) space
 * getNewsFeed:  O(N log K) time, O(K) space
 *   where N = total tweets to consider, K = number of relevant users
 * 
 * ALTERNATIVE APPROACHES
 * ======================
 * 
 * 1. GLOBAL SORTED LIST
 *    - Keep all tweets in one sorted list
 *    - getNewsFeed would be O(N) to filter by user
 *    - postTweet would be O(1) append
 *    - Trade-off: Simpler but less efficient for large N
 * 
 * 2. CACHING NEWS FEEDS
 *    - Cache each user's news feed
 *    - Invalidate on follow/unfollow/new tweet from followee
 *    - Trade-off: Fast reads but complex invalidation logic
 *    - Memory intensive
 * 
 * 3. MERGE WITHOUT HEAP
 *    - Collect all tweets, sort, take top 10
 *    - Time: O(N log N) where N = total tweets
 *    - Space: O(N)
 *    - Trade-off: Simpler code but less efficient
 * 
 * SCALABILITY CONSIDERATIONS
 * ===========================
 * 
 * For a real Twitter-like system:
 * 1. Use database with indexes on userId and timestamp
 * 2. Implement pagination for feeds
 * 3. Cache frequently accessed feeds (Redis)
 * 4. Use fan-out on write for users with few followers
 * 5. Use fan-out on read for users with many followers (celebrities)
 * 6. Implement rate limiting
 * 7. Consider eventual consistency for follower counts
 * 
 * TESTING STRATEGY
 * ================
 * 
 * Test cases to consider:
 * 1. Empty feed (user with no tweets, no follows)
 * 2. Feed with < 10 tweets
 * 3. Feed with > 10 tweets (verify only 10 returned)
 * 4. Follow/unfollow same user multiple times
 * 5. Multiple users posting interleaved tweets
 * 6. User's own tweets appear in feed
 * 7. Tweets ordered correctly after follow/unfollow
 */

// Example usage and test
class Main {
    public static void main(String[] args) {
        Twitter twitter = new Twitter();
        
        // Test case from problem
        twitter.postTweet(1, 5);
        System.out.println(twitter.getNewsFeed(1)); // [5]
        
        twitter.follow(1, 2);
        twitter.postTweet(2, 6);
        System.out.println(twitter.getNewsFeed(1)); // [6, 5]
        
        twitter.unfollow(1, 2);
        System.out.println(twitter.getNewsFeed(1)); // [5]
        
        // Additional test: multiple tweets
        twitter.postTweet(1, 7);
        twitter.postTweet(1, 8);
        twitter.postTweet(1, 9);
        System.out.println(twitter.getNewsFeed(1)); // [9, 8, 7, 5]
    }
}

class Twitter2 {

    private static class Tweet {
        int tweetId;
        long time;

        Tweet(int tweetId, long time) {
            this.tweetId = tweetId;
            this.time = time;
        }
    }

    private long time = 0;

    private final Map<Integer, Set<Integer>> followeesMap = new HashMap<>();
    private final Map<Integer, List<Tweet>> tweetsMap = new HashMap<>();

    public void postTweet(int userId, int tweetId) {
        tweetsMap
            .computeIfAbsent(userId, _ -> new ArrayList<>())
            .add(new Tweet(tweetId, time++));
    }

    public List<Integer> getNewsFeed(int userId) {
        // Min-heap by time
        PriorityQueue<Tweet> pq =
            new PriorityQueue<>(Comparator.comparingLong(t -> t.time));

        // Include self
        Set<Integer> users = new HashSet<>(followeesMap.getOrDefault(userId, Set.of()));
        users.add(userId);

        for (int uid : users) {
            for (Tweet tweet : tweetsMap.getOrDefault(uid, List.of())) {
                pq.offer(tweet);
                if (pq.size() > 10) {
                    pq.poll(); // remove oldest
                }
            }
        }

        List<Integer> result = new ArrayList<>();
        while (!pq.isEmpty()) {
            result.add(pq.poll().tweetId);
        }

        // reverse to get newest first
        Collections.reverse(result);
        return result;
    }

    public void follow(int followerId, int followeeId) {
        if (followerId == followeeId) return;
        followeesMap
            .computeIfAbsent(followerId, _ -> new HashSet<>())
            .add(followeeId);
    }

    public void unfollow(int followerId, int followeeId) {
        followeesMap
            .getOrDefault(followerId, Set.of())
            .remove(followeeId);
    }
}

/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a simplified version of Twitter with the following operations:
 * - postTweet(userId, tweetId): User posts a new tweet.
 * - getNewsFeed(userId): Retrieves the 10 most recent tweet IDs in the user's 
 *   news feed (from users they follow, or themselves), ordered from most to least recent.
 * - follow(followerId, followeeId): User follows another user.
 * - unfollow(followerId, followeeId): User unfollows another user.
 * 
 * Constraints: userId, tweetId <= 500. Up to 30,000 calls will be made.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Can a user unfollow themselves, or do they implicitly follow themselves?
 *    (Assuming a user implicitly follows themselves simplifies the news feed logic. We must block self-unfollowing).
 * 2. Is there a global timestamp, or do we rely on the order of `postTweet` calls?
 *    (Since tweets from multiple users must be merged chronologically, a global integer timestamp counter is required).
 * 3. What happens if `getNewsFeed` is called for a user who doesn't exist yet?
 *    (We should lazily create the user and return an empty list).
 * 4. Can a user follow someone who doesn't exist?
 *    (Yes, we should lazily instantiate users when they are interacted with).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Maintain relationships (graph) and historical events (tweets), 
 *   and efficiently merge chronologically sorted streams for a "news feed". The binding 
 *   constraint is handling the fan-out dilemma: do we do heavy work when writing a tweet, 
 *   or heavy work when reading the feed?
 * 
 * --- Approach 1: The Global Timeline (Brute Force) ---
 * 1. Intuition: Just keep a massive global list of all tweets ever posted.
 * 2. Try: `List<Tweet> globalTimeline`. 
 *    `postTweet` appends to the list. 
 *    `getNewsFeed` loops backwards through `globalTimeline`. If the tweet belongs to 
 *    the user or someone they follow, add it. Stop at 10.
 * 3. Why it works: Naturally preserves chronological order.
 * 4. Why it's too costly: If Twitter has millions of tweets, `getNewsFeed` requires 
 *    scanning massive amounts of irrelevant tweets just to find 10 valid ones.
 * 5. Bottleneck: O(N) read time (where N is total tweets).
 * 
 * Time Complexity: postTweet: O(1). getNewsFeed: O(N).
 * Space Complexity: O(N) for global list.
 * 
 * --- Approach 2: Fan-Out on Write (Push Model) ---
 * 1. Intuition: Pre-compute the news feed. When I tweet, push it to all my followers' feeds.
 * 2. Try: Every user has a `List<Tweet> myFeed`. 
 *    `postTweet(user, tweet)` -> add to `user.myFeed`, then iterate `user.followers` 
 *    and add to their `myFeed`. `getNewsFeed` just returns the first 10.
 * 3. Why it fails: The "Justin Bieber" problem. If a celebrity has 100 million followers, 
 *    a single `postTweet` takes O(100M) time. Furthermore, `unfollow` is a nightmare: 
 *    you have to dig through the unfollower's feed and manually delete the unfollowee's tweets.
 * 4. Bottleneck: O(F) write time (where F is # of followers) and catastrophic `unfollow` costs.
 * 
 * --- Approach 3: Fan-Out on Read (Pull Model / K-Way Merge) [Optimal] ---
 * 1. Intuition: Users only store their *own* tweets. A news feed is just the merging of 
 *    K sorted lists (where K is the number of people you follow).
 * 2. Try: Store a user's tweets as a Linked List (newest at the head). 
 *    `getNewsFeed` gathers the head tweet from all followed users. We put these K heads 
 *    into a Max-Heap (sorted by time). Pop the newest tweet, add its `.next` to the heap, 
 *    repeat 10 times.
 * 3. Why it works: We do minimal work on `postTweet` (O(1)). We do minimal work on 
 *    `getNewsFeed` because we only merge the very top elements of the relevant lists.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It expertly demonstrates object-oriented design and the classic 
 *    "Merge K Sorted Lists" algorithm.
 * 
 * Time Complexity: 
 * - postTweet: O(1) — Prepend to linked list.
 * - follow/unfollow: O(1) — HashSet add/remove.
 * - getNewsFeed: O(K + 10 log K) — where K is the number of followees. We add K items to the heap, then pop/push 10 times.
 * Space Complexity: O(T + U + F) — Storing T tweets, U users, and F follow relationships.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Unfollowing oneself: We must silently ignore `unfollow(x, x)` so the user doesn't lose their own tweets in their feed.
 * - Unfollowing someone you don't follow: Handled safely by `HashSet.remove()`.
 * - Requesting feed with 0 followees and 0 tweets: Max-Heap will be empty, cleanly returns empty list.
 * - Calling methods on non-existent users: Lazily instantiate them via a `getUser()` helper to avoid NullPointerExceptions.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "A user's timeline is just merging K sorted Linked Lists."
 *   By storing tweets as a Linked List with the newest at the head, every user's personal 
 *   timeline is already perfectly sorted. The news feed is simply finding the top 10 elements 
 *   among K sorted lists using a PriorityQueue.
 * 
 * ASCII DRY RUN (getNewsFeed via Max-Heap)
 * User 1 follows User 2.
 * User 1's tweets: [t1: time 5] -> [t2: time 2]
 * User 2's tweets: [t3: time 6] -> [t4: time 1]
 * 
 * 1. Initialize Max-Heap. Add heads: Heap = [t3 (time 6), t1 (time 5)]
 * 2. Pop t3. Add to result. Push t3.next (t4): Heap = [t1 (time 5), t4 (time 1)]
 * 3. Pop t1. Add to result. Push t1.next (t2): Heap = [t2 (time 2), t4 (time 1)]
 * 4. Pop t2. Add to result. Push t2.next (null): Heap = [t4 (time 1)]
 * 5. Pop t4. Add to result. Push t4.next (null): Heap = []
 * Result: [t3, t1, t2, t4] -> Chronological order!
 * 
 * Pitfalls:
 * - Not using a global timestamp. Real-world tweet IDs are not guaranteed to be strictly sequential or chronological across distributed systems (though Twitter's Snowflake IDs usually are). A simulated global integer timestamp is required here.
 * - Trying to hold all a user's tweets in a List/Array. You can, but a Linked List Node (`Tweet next`) makes the K-way heap merge incredibly elegant because you can just pass `tweet.next` to the heap.
 * 
 * Pattern Recognition: "Top K elements from multiple sorted streams" -> PriorityQueue (K-Way Merge).
 * 
 * Interview Script snippet:
 * "In a real system, Twitter uses a hybrid Push/Pull model, but for this constraint set, 
 * Fan-Out on Read (Pull Model) is optimal. I'll design a `User` class holding a HashSet of 
 * followees and a Linked List head for their personal tweets. To generate a news feed, 
 * I'll gather the Linked List heads of everyone the user follows, place them in a Max-Heap 
 * sorted by timestamp, and execute a K-way merge for the top 10 elements."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How does real Twitter handle this?
 * A1: Real Twitter uses a Hybrid Model. For average users, it uses "Fan-out on Write" (Push), 
 *     meaning your tweet is immediately inserted into all your followers' cached Redis timelines. 
 *     For celebrities (e.g., Elon Musk), it uses "Fan-out on Read" (Pull). When you load your 
 *     feed, the system pulls your cached feed AND merges it dynamically with the tweets of 
 *     any celebrities you follow. This prevents the celebrity's tweet from stalling the caching tier.
 * 
 * Q2: What if we needed pagination? (e.g., load the NEXT 10 tweets)
 * A2: We would return a pagination token. Instead of dropping the heap, we could serialize the 
 *     current pointers of the heap. Or, more commonly, pass a `max_timestamp`, and the next 
 *     query initializes the heap using the first tweet *older* than that timestamp for each followee.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class TwitterStudy {

    // Global timestamp to enforce chronological order across all users
    private static int timestamp = 0;

    /**
     * Internal Tweet class acting as a Singly Linked List Node.
     * Newest tweets are prepended to the head.
     */
    private static class Tweet {
        int id;
        int time;
        Tweet next;

        public Tweet(int id) {
            this.id = id;
            this.time = timestamp++;
            this.next = null;
        }
    }

    /**
     * Internal User class holding their relationships and their personal tweet chain.
     */
    private static class User {
        int id;
        Set<Integer> followed;
        Tweet tweetHead;

        public User(int id) {
            this.id = id;
            this.followed = new HashSet<>();
            this.tweetHead = null;
            // Crucial: A user implicitly follows themselves so their own tweets appear in their feed
            follow(id); 
        }

        public void follow(int id) {
            followed.add(id);
        }

        public void unfollow(int id) {
            // Prevent a user from unfollowing themselves
            if (this.id != id) {
                followed.remove(id);
            }
        }

        // Prepends the new tweet to the head of this user's linked list
        public void post(int id) {
            Tweet t = new Tweet(id);
            t.next = tweetHead;
            tweetHead = t;
        }
    }

    // Maps userId to their corresponding User object
    private Map<Integer, User> userMap;

    /** 
     * Initializes your twitter object. 
     */
    public TwitterStudy() {
        userMap = new HashMap<>();
    }

    /**
     * Helper to lazily instantiate users if they don't exist yet.
     */
    private User getUser(int userId) {
        userMap.putIfAbsent(userId, new User(userId));
        return userMap.get(userId);
    }

    /** 
     * Composes a new tweet with ID tweetId by the user userId.
     * Time: O(1)
     */
    public void postTweet(int userId, int tweetId) {
        User u = getUser(userId);
        u.post(tweetId);
    }

    /** 
     * Retrieves the 10 most recent tweet IDs in the user's news feed.
     * Time: O(K + 10 log K) where K is number of followees.
     */
    public List<Integer> getNewsFeed(int userId) {
        List<Integer> feed = new ArrayList<>();
        
        if (!userMap.containsKey(userId)) {
            return feed; // User doesn't exist, feed is empty
        }

        // Max-Heap to sort tweets by most recent time
        PriorityQueue<Tweet> maxHeap = new PriorityQueue<>((a, b) -> b.time - a.time);

        // 1. Gather the head of the tweet list from everyone the user follows
        User u = getUser(userId);
        for (int followeeId : u.followed) {
            User followee = getUser(followeeId);
            if (followee.tweetHead != null) {
                maxHeap.offer(followee.tweetHead);
            }
        }

        // 2. K-Way Merge: Pop the newest tweet, push the next tweet in that specific chain
        while (!maxHeap.isEmpty() && feed.size() < 10) {
            Tweet newestTweet = maxHeap.poll();
            feed.add(newestTweet.id);
            
            // If the user has older tweets, add the next one to the heap tournament
            if (newestTweet.next != null) {
                maxHeap.offer(newestTweet.next);
            }
        }

        return feed;
    }

    /** 
     * The user with ID followerId started following the user with ID followeeId.
     * Time: O(1)
     */
    public void follow(int followerId, int followeeId) {
        User follower = getUser(followerId);
        // Ensure the followee exists in the system even if they haven't tweeted
        getUser(followeeId); 
        
        follower.follow(followeeId);
    }

    /** 
     * The user with ID followerId started unfollowing the user with ID followeeId.
     * Time: O(1)
     */
    public void unfollow(int followerId, int followeeId) {
        if (!userMap.containsKey(followerId) || !userMap.containsKey(followeeId)) {
            return; 
        }
        User follower = getUser(followerId);
        follower.unfollow(followeeId);
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing (Matches Example 1)
    // ============================================================================
    public static void main(String[] args) {
        TwitterStudy twitter = new TwitterStudy();
        
        System.out.println("User 1 posts tweet 5");
        twitter.postTweet(1, 5); 
        
        System.out.println("User 1 feed: " + twitter.getNewsFeed(1)); // Expected: [5]
        
        System.out.println("User 1 follows User 2");
        twitter.follow(1, 2); 
        
        System.out.println("User 2 posts tweet 6");
        twitter.postTweet(2, 6); 
        
        // Expected: [6, 5]. Tweet 6 is newer than Tweet 5.
        System.out.println("User 1 feed after follow: " + twitter.getNewsFeed(1)); 
        
        System.out.println("User 1 unfollows User 2");
        twitter.unfollow(1, 2);  
        
        // Expected: [5]. User 1 is no longer following User 2.
        System.out.println("User 1 feed after unfollow: " + twitter.getNewsFeed(1)); 
        
        // Edge Case: Unfollowing oneself should be ignored
        twitter.unfollow(1, 1);
        System.out.println("User 1 feed after attempting self-unfollow: " + twitter.getNewsFeed(1)); // Expected: [5]
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Object-Oriented Graph + PriorityQueue (K-Way Merge).
 * Key Observation: Storing tweets in a continuous array forces O(N) scanning. By modeling 
 * tweets as a Linked List prepended onto user objects, we instantly have perfectly 
 * reverse-chronological data streams. Merging them becomes the classic "Merge K Sorted Lists" 
 * problem, allowing O(K + 10 log K) feed generation.
 * What to Memorize: `PriorityQueue<Tweet> maxHeap = new PriorityQueue<>((a, b) -> b.time - a.time);`
 * plus initializing the heap with `followee.tweetHead`.
 * Most Common Trap: Forgetting to make a user implicitly follow themselves in their 
 * constructor, leading to complex conditional logic inside `getNewsFeed` to handle 
 * personal tweets vs followed tweets.
 * Mental Trigger: "News Feed / Timeline" -> "Pull Model + K-Way Heap Merge".
 * ============================================================================
 */











































import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

class Twitter {

    /*
     * A tweet's ID is what the API returns.
     * Its sequence number establishes when it was posted.
     *
     * Java record components are final, making Tweet a suitable
     * immutable value object.
     *
     * We use long for sequence because the number of posting events
     * may exceed the range of an int in a long-running system.
     */
    private record Tweet(int id, long sequence) {}

    /*
     * A cursor points to one tweet in one author's timeline.
     *
     * timelineIndex is the position of the tweet in that author's
     * ArrayList. Because timelines are oldest-to-newest, decrementing
     * this index moves to an older tweet.
     *
     * Keeping the author ID lets us retrieve the corresponding
     * timeline after removing a cursor from the heap.
     */
    private record TweetCursor(
            int authorId,
            int timelineIndex,
            Tweet tweet
    ) {}

    /*
     * FOLLOW GRAPH
     *
     * Key: follower ID.
     * Value: set of users followed by that user.
     *
     * HashSet ensures that repeated follow calls do not duplicate
     * an author's timeline in the news feed.
     *
     * Expected lookup, insertion, and removal: O(1).
     */
    private final Map<Integer, Set<Integer>> following = new HashMap<>();

    /*
     * TIMELINES
     *
     * Key: author ID.
     * Value: tweets posted by that author, oldest to newest.
     *
     * ArrayList is suitable because:
     * 1. Posting appends at the end.
     * 2. The newest tweet is at size - 1.
     * 3. We can move backward by decrementing an index.
     *
     * Each post costs O(1) amortized.
     */
    private final Map<Integer, List<Tweet>> tweetsByUser = new HashMap<>();

    /*
     * Globally increasing posting sequence.
     *
     * We must not compare tweet IDs because IDs are identifiers,
     * not guaranteed timestamps.
     */
    private long nextSequence = 0;

    public Twitter() {
        /*
         * No preallocation is needed.
         * Users and timelines are created lazily as operations occur.
         *
         * Time: O(1).
         * Space: O(1) initially.
         */
    }

    public void postTweet(int userId, int tweetId) {
        /*
         * Create the user's timeline on their first post.
         * computeIfAbsent avoids a separate containsKey/get/put sequence.
         *
         * Appending preserves chronological order because each call
         * receives a newer sequence number.
         *
         * Time: O(1) amortized.
         * Space: O(1) additional per tweet, excluding ArrayList growth.
         */
        List<Tweet> timeline = tweetsByUser.computeIfAbsent(
                userId,
                ignored -> new ArrayList<>()
        );

        timeline.add(new Tweet(tweetId, nextSequence++));
    }

    public List<Integer> getNewsFeed(int userId) {
        /*
         * STEP 1: Identify all relevant authors.
         *
         * A user's own tweets must appear even when they follow nobody.
         * Adding userId explicitly handles this without relying on
         * the user following themselves.
         *
         * A Set also protects against duplicate authors.
         *
         * Let F = number of followees and A = F + 1.
         * Time: O(A) expected.
         * Auxiliary space: O(A).
         */
        Set<Integer> authors = new HashSet<>(
                following.getOrDefault(userId, Set.of())
        );
        authors.add(userId);

        /*
         * STEP 2: Initialize a max-heap.
         *
         * Java PriorityQueue is a min-heap by default, so reverse the
         * sequence comparator. The greatest sequence is removed first.
         *
         * We keep only one cursor per nonempty timeline.
         * Therefore, heap size never exceeds A.
         *
         * Time to initialize the empty heap: O(1).
         */
        PriorityQueue<TweetCursor> newestFirst = new PriorityQueue<>(
                Comparator.comparingLong(
                        (TweetCursor cursor) -> cursor.tweet().sequence()
                ).reversed()
        );

        /*
         * Insert the newest tweet from each relevant author's timeline.
         *
         * The last index is timeline.size() - 1, NOT size().
         * Lists use zero-based indexing, so size() itself is out of bounds.
         *
         * Why only one tweet per author?
         *
         * Every older tweet in the same timeline has a smaller sequence.
         * Until the newest tweet is removed, none of those older tweets
         * can be the newest remaining tweet from that timeline.
         *
         * If author i has n_i tweets, retrieving their newest tweet
         * takes O(1), while inserting it into the heap takes O(log A).
         *
         * Total initialization time: O(A log A).
         * Heap space: O(A).
         */
        for (int authorId : authors) {
            List<Tweet> timeline = tweetsByUser.get(authorId);

            if (timeline == null || timeline.isEmpty()) {
                continue;
            }

            int newestIndex = timeline.size() - 1;
            Tweet newestTweet = timeline.get(newestIndex);

            newestFirst.offer(
                    new TweetCursor(authorId, newestIndex, newestTweet)
            );
        }

        /*
         * STEP 3: Repeatedly select the globally newest available tweet.
         *
         * Each poll removes the newest tweet among the current candidates.
         * Its author's next older tweet becomes the next candidate.
         *
         * This is the k-way merge pattern:
         *
         *       Author A timeline ----\
         *       Author B timeline -----+--> max-heap --> feed
         *       Author C timeline ----/
         *
         * The heap always exposes the newest unprocessed tweet from
         * each timeline. Thus its maximum is the next correct feed item.
         *
         * Poll: O(log A).
         * Optional insertion of the next older tweet: O(log A).
         *
         * At most 10 tweets are emitted, so the merge takes O(10 log A).
         * More generally, for a feed limit L, it takes O(L log A).
         */
        List<Integer> feed = new ArrayList<>(10);

        while (!newestFirst.isEmpty() && feed.size() < 10) {
            /*
             * The heap's root is the newest tweet across all current
             * timeline candidates.
             */
            TweetCursor newestCursor = newestFirst.poll();

            feed.add(newestCursor.tweet().id());

            /*
             * Move one position backward in the SAME author's timeline.
             *
             * If the current index is 4, the next index is 3.
             * Index -1 means there are no older tweets remaining.
             */
            int olderIndex = newestCursor.timelineIndex() - 1;

            if (olderIndex >= 0) {
                List<Tweet> authorTimeline =
                        tweetsByUser.get(newestCursor.authorId());

                Tweet olderTweet = authorTimeline.get(olderIndex);

                newestFirst.offer(
                        new TweetCursor(
                                newestCursor.authorId(),
                                olderIndex,
                                olderTweet
                        )
                );
            }
        }

        /*
         * The feed contains at most 10 tweet IDs, ordered newest first.
         *
         * Time: O(min(10, M) * log A) for the merge,
         * plus O(A log A) for initialization.
         *
         * The result uses O(min(10, M)) space.
         */
        return feed;
    }

    public void follow(int followerId, int followeeId) {
        /*
         * Store the relationship in a set.
         *
         * Repeated calls are harmless because Set.add does not create
         * duplicate relationships.
         *
         * Self-following is harmless too: getNewsFeed uses a Set and
         * adds the requesting user exactly once.
         *
         * Time: O(1) expected.
         * Space: O(1) expected for a new relationship.
         */
        following.computeIfAbsent(
                followerId,
                ignored -> new HashSet<>()
        ).add(followeeId);
    }

    public void unfollow(int followerId, int followeeId) {
        /*
         * A missing follower entry or missing relationship is a no-op.
         *
         * Do not remove the user's own tweets or timeline.
         * The user's own timeline is added independently in getNewsFeed.
         *
         * Time: O(1) expected.
         * Auxiliary space: O(1).
         */
        Set<Integer> followees = following.get(followerId);

        if (followees != null) {
            followees.remove(followeeId);
        }
    }
}





import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

class Twitter {

    private static final int NEWS_FEED_LIMIT = 10;

    /*
     * Represents an immutable tweet.
     *
     * id       : the tweet ID returned by getNewsFeed().
     * timestamp: a logical posting order, NOT the tweet ID.
     *
     * We use a monotonically increasing long instead of wall-clock time.
     * This guarantees an unambiguous order for sequential postTweet calls,
     * even if two posts would otherwise have the same clock timestamp.
     */
    private record Tweet(int id, long timestamp) {}

    /*
     * Represents the current candidate from one author's timeline.
     *
     * userId        : identifies which timeline this candidate belongs to.
     * tweetIndex    : position of the tweet in that timeline.
     * tweet         : the candidate tweet itself.
     *
     * When this candidate is removed from the heap, we move to
     * tweetIndex - 1 to reveal the next older tweet from that author.
     *
     * This record is immutable: instead of modifying a cursor, we create
     * a new one for the next position. That keeps heap entries simple.
     */
    private record TimelineCursor(
            int userId,
            int tweetIndex,
            Tweet tweet
    ) {}

    /*
     * FOLLOW RELATIONSHIPS
     *
     * Map key   : follower's user ID.
     * Map value : set of users that follower follows.
     *
     * A Set is important because the same follow relationship should
     * not be stored twice. Otherwise, the same timeline could enter
     * the heap twice and duplicate tweets in the feed.
     */
    private final Map<Integer, Set<Integer>> followeesByUser =
            new HashMap<>();

    /*
     * USER TIMELINES
     *
     * Map key   : author's user ID.
     * Map value : tweets by that author, ordered oldest to newest.
     *
     * ArrayList is a good fit because:
     * - Posting appends to the end in O(1) amortized time.
     * - Random access by index takes O(1) time.
     * - The newest tweet is always at size() - 1.
     *
     * We retain old tweets when someone unfollows an author.
     * Unfollowing changes visibility, not the tweet's existence.
     */
    private final Map<Integer, List<Tweet>> tweetsByUser =
            new HashMap<>();

    /*
     * Every post gets a strictly increasing logical timestamp.
     * A larger timestamp means a newer post.
     */
    private long nextTimestamp = 0;

    public Twitter() {
        // No initialization is needed: maps and timelines are created lazily.
    }

    public void postTweet(int userId, int tweetId) {
        /*
         * Get the author's timeline, creating it on the first post.
         *
         * Appending preserves the timeline's oldest-to-newest ordering.
         * Consequently, getNewsFeed() can traverse it backward by index.
         *
         * Time: O(1) amortized.
         * Additional space: O(1) per tweet.
         */
        List<Tweet> timeline = tweetsByUser.computeIfAbsent(
                userId,
                ignored -> new ArrayList<>()
        );

        timeline.add(new Tweet(tweetId, nextTimestamp++));
    }

    public List<Integer> getNewsFeed(int userId) {
        /*
         * STEP 1: Determine which authors are allowed in this feed.
         *
         * The feed includes:
         *   1. The requesting user.
         *   2. Every user the requester follows.
         *
         * We add the requester explicitly rather than relying on a
         * self-follow relationship. The Set also ensures that a
         * self-follow, if present, cannot duplicate the author.
         *
         * Let A be the number of distinct relevant authors.
         *
         * Time: O(A) expected.
         * Space: O(A) for this local set.
         */
        Set<Integer> relevantAuthors = new HashSet<>(
                followeesByUser.getOrDefault(userId, Set.of())
        );

        relevantAuthors.add(userId);

        /*
         * STEP 2: Create a max-heap of timeline candidates.
         *
         * Java's PriorityQueue is a min-heap by default. Reversing
         * the timestamp comparator makes the newest tweet the root.
         *
         * The heap stores at most ONE tweet per author at any moment.
         * This is the key optimization: we do not insert every historical
         * tweet, because an older tweet cannot be the next answer while
         * a newer tweet from the same timeline remains unprocessed.
         *
         * Time to create an empty heap: O(1).
         */
        PriorityQueue<TimelineCursor> newestTweets = new PriorityQueue<>(
                Comparator.comparingLong(
                        (TimelineCursor cursor) -> cursor.tweet().timestamp()
                ).reversed()
        );

        /*
         * STEP 3: Seed the heap with each author's newest tweet.
         *
         * We iterate over the relevant authors. For each one:
         *   - Find their timeline.
         *   - Ignore authors who have never posted.
         *   - Insert the final (newest) timeline element.
         *
         * Why use size() - 1?
         *
         * ArrayList indexes start at 0. A list of size 3 has indexes
         * 0, 1, and 2; index 3 is invalid. Therefore, the last index
         * is always size() - 1.
         *
         * Each heap insertion costs O(log A), so seeding takes
         * O(A log A) time and O(A) heap space.
         */
        for (int authorId : relevantAuthors) {
            List<Tweet> timeline = tweetsByUser.get(authorId);

            if (timeline == null || timeline.isEmpty()) {
                continue;
            }

            int newestTweetIndex = timeline.size() - 1;
            Tweet newestTweet = timeline.get(newestTweetIndex);

            newestTweets.offer(
                    new TimelineCursor(
                            authorId,
                            newestTweetIndex,
                            newestTweet
                    )
            );
        }

        /*
         * STEP 4: Perform a k-way merge, newest tweet first.
         *
         * The heap invariant is:
         *
         * "For every relevant timeline that still has unprocessed
         * tweets, the heap contains its newest unprocessed tweet."
         *
         * Therefore, polling the heap always returns the globally
         * newest unprocessed tweet among all relevant timelines.
         *
         * After selecting that tweet, we move one position backward
         * in its author's timeline and insert the next older tweet.
         *
         * We stop when:
         *   - The feed contains 10 tweets, OR
         *   - The heap becomes empty (no eligible tweets remain).
         *
         * Each poll and insertion costs O(log A).
         */
        List<Integer> newsFeed = new ArrayList<>(NEWS_FEED_LIMIT);

        while (!newestTweets.isEmpty()
                && newsFeed.size() < NEWS_FEED_LIMIT) {

            /*
             * Remove the globally newest available candidate.
             * The heap restores its ordering after this removal.
             */
            TimelineCursor newestCursor = newestTweets.poll();

            newsFeed.add(newestCursor.tweet().id());

            /*
             * Advance backward within the SAME author's timeline.
             *
             * Example:
             *   Timeline: [5, 8, 12]
             *   Index:       0  1   2
             *
             * After selecting tweet 12 at index 2, the next candidate
             * from this author is tweet 8 at index 1.
             *
             * We don't need a candidate from this author if the index
             * becomes -1 because no older tweets remain.
             */
            int nextOlderIndex = newestCursor.tweetIndex() - 1;

            if (nextOlderIndex >= 0) {
                List<Tweet> authorTimeline =
                        tweetsByUser.get(newestCursor.userId());

                Tweet nextOlderTweet = authorTimeline.get(nextOlderIndex);

                newestTweets.offer(
                        new TimelineCursor(
                                newestCursor.userId(),
                                nextOlderIndex,
                                nextOlderTweet
                        )
                );
            }
        }

        /*
         * The result contains at most 10 tweet IDs, newest first.
         *
         * If fewer than 10 eligible tweets exist, the loop naturally
         * returns all of them. No special case is needed.
         *
         * Total time: O(A log A + L log A), where L = 10.
         * Auxiliary space: O(A + L).
         */
        return newsFeed;
    }

    public void follow(int followerId, int followeeId) {
        /*
         * Create the follower's set if necessary, then add the followee.
         *
         * Set.add() is idempotent, so repeated follow calls do not create
         * duplicate relationships or duplicate tweets in a feed.
         *
         * Time: O(1) expected.
         */
        followeesByUser.computeIfAbsent(
                followerId,
                ignored -> new HashSet<>()
        ).add(followeeId);
    }

    public void unfollow(int followerId, int followeeId) {
        /*
         * If the follower has no relationship set, there is nothing to do.
         * Otherwise, remove the relationship if it exists.
         *
         * We intentionally do not remove any tweets. The next call to
         * getNewsFeed() simply stops including this author's timeline.
         *
         * Time: O(1) expected.
         */
        Set<Integer> followees = followeesByUser.get(followerId);

        if (followees != null) {
            followees.remove(followeeId);
        }
    }
}


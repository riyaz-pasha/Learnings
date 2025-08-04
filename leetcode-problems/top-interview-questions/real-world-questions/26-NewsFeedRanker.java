import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

/* Design a ranking algorithm to sort a user’s news feed by relevance. */

class Post {

    UUID id;
    long publicationTime;
    int likes;
    int comments;
    int shares;
    UUID authorId;
    String contentType; // video, text, etc
    Set<String> topics;

}

class User {

    UUID id;
    Set<UUID> followedAuthors;
    Map<String, Double> topicInterestScore; // e.g., {"sports": 0.8, "tech": 0.2}
    Map<String, Double> contentTypeInterestScore; // e.g., {"video": 1.0, "photo": 0.4}

    public double getTopicAffinityScore(Set<String> topics) {
        double score = 0;
        for (String topic : topics) {
            score += topicInterestScore.getOrDefault(topic, 0.0);
        }
        return score;
    }

    public double getContentTypePreferenceScore(String contentType) {
        return this.contentTypeInterestScore.getOrDefault(contentType, 0.0);
    }

    public double followsAuthor(UUID authorId) {
        return this.followedAuthors.contains(authorId) ? 1.0 : 0.0;
    }

}

class PostScore {

    Post post;
    double score;

    PostScore(Post post, double score) {
        this.post = post;
        this.score = score;
    }

}

class NewsFeedRanker {

    /* O(N log K) using min-heap O(K) for heap */
    public List<Post> rankNewsFeed(List<Post> posts, User user, int topK) {
        PriorityQueue<PostScore> minHeap = new PriorityQueue<>(Comparator.comparingDouble(ps -> ps.score));

        for (Post post : posts) {
            double score = this.calculateScore(post, user);
            minHeap.offer(new PostScore(post, score));
            if (minHeap.size() > topK) {
                minHeap.poll();
            }
        }

        List<Post> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll().post);
        }
        Collections.reverse(result); // highest score first
        return result;
    }

    private double calculateScore(Post post, User user) {
        double score = 0;

        score += (0.3 * this.freshnessScore(post));
        score += (0.2 * this.getSocialEngagementScore(post));
        score += (0.3 * user.getTopicAffinityScore(post.topics));
        score += (0.1 * user.getContentTypePreferenceScore(post.contentType));
        score += (0.1 * user.followsAuthor(post.authorId));

        return score;
    }

    private double freshnessScore(Post post) {
        long timeDifferenceMillis = System.currentTimeMillis() - post.publicationTime;
        double freshness = 1.0 / (1 + (timeDifferenceMillis) / (60 * 60 * 1000)); // inverse hour decay
        return freshness;
    }

    public double getSocialEngagementScore(Post post) {
        return (0.5 * post.likes) + (0.3 * post.comments) + (0.2 * post.shares);
    }

}

/* Suggest top N friend recommendations based on mutual friends and profile similarity. */

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

class Interest {
}

class User {

    private final int id;
    private final Set<User> friends;
    private final Set<Interest> interests;

    public User(int id, Set<User> friends, Set<Interest> interests) {
        this.id = id;
        this.friends = friends;
        this.interests = interests;
    }

    public int getId() {
        return this.id;
    }

    public Set<User> getFriends() {
        return this.friends;
    }

    public Set<Interest> getInterests() {
        return this.interests;
    }
}

class Recommendation implements Comparable<Recommendation> {
    private final User user;
    private final double score;

    public Recommendation(User user, double score) {
        this.user = user;
        this.score = score;
    }

    public User getUser() {
        return user;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(Recommendation other) {
        // Sort in descending order of score
        return Double.compare(other.score, this.score);
    }
}

class FriendRecommender {

    public FriendRecommender() {
    }

    public List<User> recommendFriends(User user,
            int topN,
            double weightMutualFriends,
            double weightProfileSimilarity) {

        Set<User> friends = user.getFriends();
        Set<User> visited = new HashSet<>();
        PriorityQueue<Recommendation> minHeap = new PriorityQueue<>();

        for (User friend : friends) {
            for (User friendOfFriend : friend.getFriends()) {
                if (!friendOfFriend.equals(user)
                        && !friends.contains(friendOfFriend)
                        && visited.add(friendOfFriend)) { // also adds to visited
                    int mutualFriendsCount = this.countMutualFriends(user, friendOfFriend);
                    double profileScore = this.calculateProfileSimilarity(user, friendOfFriend);

                    double combinedScore = (weightMutualFriends * mutualFriendsCount)
                            + (weightProfileSimilarity * profileScore);

                    minHeap.offer(new Recommendation(friendOfFriend, combinedScore));
                    if (minHeap.size() > topN) {
                        minHeap.poll();
                    }
                }
            }
        }

        LinkedList<User> suggestions = new LinkedList<>();
        while (!minHeap.isEmpty()) {
            suggestions.addFirst(minHeap.poll().getUser());
        }

        return suggestions;
    }

    private double calculateProfileSimilarity(User user1, User user2) {
        Set<Interest> commonInterests = new HashSet<>(user1.getInterests());
        commonInterests.retainAll(user2.getInterests());

        Set<Interest> allInterests = new HashSet<>(user1.getInterests());
        allInterests.addAll(user2.getInterests());

        if (allInterests.isEmpty()) {
            return 0.0;
        }

        return (double) commonInterests.size() / allInterests.size();
    }

    private int countMutualFriends(User user1, User user2) {
        Set<User> mutualFriends = new HashSet<>(user1.getFriends());
        mutualFriends.retainAll(user2.getFriends());
        return mutualFriends.size();
    }

    public List<User> recommendFriends(User user,
            int topN,
            double baseMutualWeight,
            double baseProfileWeight,
            int maxDepth,
            double decayFactor) {

        Set<User> directFriends = user.getFriends();
        Set<User> visited = new HashSet<>();
        PriorityQueue<Recommendation> minHeap = new PriorityQueue<>();
        Queue<User> queue = new LinkedList<>();
        Map<User, Integer> depthMap = new HashMap<>();

        queue.offer(user);
        depthMap.put(user, 0);
        visited.add(user);

        while (!queue.isEmpty()) {
            User current = queue.poll();
            int depth = depthMap.get(current);

            // Stop at maxDepth
            if (depth >= maxDepth)
                continue;

            for (User neighbor : current.getFriends()) {
                if (visited.contains(neighbor))
                    continue;
                visited.add(neighbor);
                depthMap.put(neighbor, depth + 1);
                queue.offer(neighbor);

                // Only consider as recommendation if not direct friend and not self
                if (!directFriends.contains(neighbor) && neighbor != user) {
                    int mutualFriends = countMutualFriends(user, neighbor);
                    double profileSim = calculateProfileSimilarity(user, neighbor);

                    double decay = Math.pow(decayFactor, depth); // decay from depth 1 (friend) → depth 2+ (indirect)
                    double combinedScore = (baseMutualWeight * mutualFriends * decay) +
                            (baseProfileWeight * profileSim * decay);

                    minHeap.offer(new Recommendation(neighbor, combinedScore));
                    if (minHeap.size() > topN) {
                        minHeap.poll();
                    }
                }
            }
        }

        LinkedList<User> recommendations = new LinkedList<>();
        while (!minHeap.isEmpty()) {
            recommendations.addFirst(minHeap.poll().getUser());
        }
        return recommendations;
    }

    public List<User> recommendFriendsOptimized(User user,
            int topN,
            double baseMutualWeight,
            double baseProfileWeight,
            int maxDepth,
            double decayFactor) {

        Set<User> directFriends = user.getFriends();
        Map<User, Integer> depthMap = new HashMap<>();
        PriorityQueue<Recommendation> minHeap = new PriorityQueue<>();
        Queue<User> queue = new LinkedList<>();

        queue.offer(user);
        depthMap.put(user, 0);

        while (!queue.isEmpty()) {
            User current = queue.poll();
            int depth = depthMap.get(current);

            if (depth >= maxDepth)
                continue;

            for (User neighbor : current.getFriends()) {
                if (depthMap.containsKey(neighbor))
                    continue;
                depthMap.put(neighbor, depth + 1);
                queue.offer(neighbor);

                // Recommendation condition
                if (!directFriends.contains(neighbor) && neighbor != user) {
                    int mutualFriends = countMutualFriends(user, neighbor);
                    double profileSim = calculateProfileSimilarity(user, neighbor);

                    double decay = Math.pow(decayFactor, depth); // depth 1 -> decay^1 = decay
                    double combinedScore = (baseMutualWeight * mutualFriends * decay) +
                            (baseProfileWeight * profileSim * decay);

                    minHeap.offer(new Recommendation(neighbor, combinedScore));
                    if (minHeap.size() > topN) {
                        minHeap.poll();
                    }
                }
            }
        }

        LinkedList<User> recommendations = new LinkedList<>();
        while (!minHeap.isEmpty()) {
            recommendations.addFirst(minHeap.poll().getUser());
        }
        return recommendations;
    }

}

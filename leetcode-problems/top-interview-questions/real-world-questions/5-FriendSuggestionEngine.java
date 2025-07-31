/* Suggest new friends to a user based on mutual friends. */

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

class User {

    private final String name;

    // override equals and hashcode

    User(String name) {
        this.name = name;
    }

}

class Suggestion implements Comparable<Suggestion> {

    User user;
    int mutualFriendCount;

    public Suggestion(User user, int mutualFriendCount) {
        this.mutualFriendCount = mutualFriendCount;
        this.user = user;
    }

    @Override
    public int compareTo(Suggestion other) {
        return Integer.compare(this.mutualFriendCount, other.mutualFriendCount);
    }
}

class FriendSuggestionEngine {

    private final Map<User, Set<User>> adjList;

    public FriendSuggestionEngine() {
        this.adjList = new HashMap<>();
    }

    public void addUser(User user) {
        this.adjList.putIfAbsent(user, new HashSet<>());
    }

    public void addFriendship(User user1, User user2) {
        this.addUser(user1);
        this.addUser(user2);
        this.adjList.get(user1).add(user2);
        this.adjList.get(user2).add(user1);
    }

    public List<User> getTopSuggestions(User user, int n) {
        List<User> suggestions = new LinkedList<>();

        Set<User> friends = this.adjList.get(user);
        Map<User, Integer> mutualFriendsCount = new HashMap<>();

        for (User friend : friends) {
            Set<User> frindsOfFriend = this.adjList.getOrDefault(friend, new HashSet<>());

            for (User friendOfFriend : frindsOfFriend) {
                if (!friendOfFriend.equals(user) && !friends.contains(friendOfFriend)) {
                    mutualFriendsCount.put(friendOfFriend, mutualFriendsCount.getOrDefault(friendOfFriend, 0) + 1);
                }
            }

        }

        PriorityQueue<Suggestion> minHeap = new PriorityQueue<>();

        for (Map.Entry<User, Integer> entry : mutualFriendsCount.entrySet()) {
            Suggestion suggestion = new Suggestion(entry.getKey(), entry.getValue());
            minHeap.offer(suggestion);
            if (minHeap.size() > n) {
                minHeap.poll();
            }
        }

        while (!minHeap.isEmpty()) {
            suggestions.addFirst(minHeap.poll().user);
        }

        return suggestions;
    }

}

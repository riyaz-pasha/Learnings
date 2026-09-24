/* Suggest new friends to a user based on mutual friends. */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

class User {
    private final String name;

    public User(String name) {
        this.name = name;
    }

    // ✅ Required for Map and Set keys
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User))
            return false;
        User other = (User) o;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    @Override
    public String toString() {
        return name;
    }
}

class Suggestion implements Comparable<Suggestion> {
    User user;
    int mutualFriendCount;

    public Suggestion(User user, int mutualFriendCount) {
        this.user = user;
        this.mutualFriendCount = mutualFriendCount;
    }

    @Override
    public int compareTo(Suggestion other) {
        int cmp = Integer.compare(other.mutualFriendCount, this.mutualFriendCount); // Descending
        if (cmp == 0) {
            return this.user.toString().compareTo(other.user.toString()); // Break ties
        }
        return cmp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Suggestion))
            return false;
        Suggestion that = (Suggestion) o;
        return Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return user.hashCode();
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
        Set<User> friends = this.adjList.getOrDefault(user, Collections.emptySet());
        Map<User, Integer> mutualFriendsCount = new HashMap<>();

        for (User friend : friends) {
            for (User fof : this.adjList.getOrDefault(friend, Collections.emptySet())) {
                if (!fof.equals(user) && !friends.contains(fof)) {
                    mutualFriendsCount.put(fof, mutualFriendsCount.getOrDefault(fof, 0) + 1);
                }
            }
        }

        // TreeSet<Suggestion> topSuggestions = new TreeSet<>();

        // for (Map.Entry<User, Integer> entry : mutualFriendsCount.entrySet()) {
        // Suggestion suggestion = new Suggestion(entry.getKey(), entry.getValue());

        // if (topSuggestions.contains(suggestion)) {
        // topSuggestions.remove(suggestion);
        // topSuggestions.add(suggestion); // update frequency
        // } else if (topSuggestions.size() < n) {
        // topSuggestions.add(suggestion);
        // } else {
        // Suggestion lowest = topSuggestions.last();
        // if (suggestion.compareTo(lowest) < 0) {
        // // Lower than existing top-N
        // } else {
        // topSuggestions.pollLast(); // remove lowest
        // topSuggestions.add(suggestion);
        // }
        // }
        // }

        TreeSet<Suggestion> topSuggestions = new TreeSet<>();

        for (Map.Entry<User, Integer> entry : mutualFriendsCount.entrySet()) {
            Suggestion suggestion = new Suggestion(entry.getKey(), entry.getValue());

            if (topSuggestions.size() < n) {
                topSuggestions.add(suggestion);
            } else {
                Suggestion lowest = topSuggestions.last(); // weakest in top N

                if (suggestion.compareTo(lowest) > 0) {
                    // Better than current lowest → replace
                    topSuggestions.pollLast();
                    topSuggestions.add(suggestion);
                }
            }
        }

        List<User> result = new ArrayList<>();
        for (Suggestion s : topSuggestions) {
            result.add(s.user);
        }

        return result;
    }

    /*
     * topSuggestions.size() → O(1)
     * topSuggestions.last() → O(log N)
     * topSuggestions.add() → O(log N)
     * topSuggestions.pollLast() → O(log N)
     */

    /*
     * Operation Time Complexity
     * add(E e) O(log N)
     * remove(Object o) O(log N)
     * contains(Object o) O(log N)
     * first() / last() O(log N)
     * size() O(1)
     * pollFirst() / pollLast() O(log N)
     * iterator() (in order) O(N)
     * 
     * Where N is the number of elements currently in the TreeSet.
     */

}

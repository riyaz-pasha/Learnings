import java.util.*;

/**
 * LeaderboardTopK
 *
 * Supports:
 *  - updateScore(userId, delta)
 *  - getTopK()
 *
 * Approach:
 * ----------
 * We maintain:
 *
 * 1) HashMap<String, Integer> scores
 *    - stores the latest total score of each user
 *
 * 2) TreeSet<Entry> sortedSet
 *    - keeps all users sorted by score descending
 *    - if tie, userId ascending
 *
 * Why TreeSet?
 * -----------
 * TreeSet maintains sorted order automatically.
 * So retrieving top K users is simply iterating first K elements.
 *
 * Important:
 * ----------
 * When a user's score changes:
 * - We must remove the old Entry from TreeSet
 * - Update the score
 * - Insert new Entry
 *
 * Time Complexity:
 * ---------------
 * updateScore(): O(log N)
 * getTopK(): O(K)
 *
 * Space Complexity:
 * ----------------
 * O(N) for storing all users.
 */
class LeaderboardTopK {

    private static class Entry {
        String userId;
        int score;

        Entry(String userId, int score) {
            this.userId = userId;
            this.score = score;
        }
    }

    // Stores current score per user
    private final Map<String, Integer> scores = new HashMap<>();

    // Sorted users by score desc, then userId asc
    private final TreeSet<Entry> sortedSet = new TreeSet<>((a, b) -> {
        if (a.score != b.score) {
            return Integer.compare(b.score, a.score); // higher score first
        }
        return a.userId.compareTo(b.userId); // tie-breaker
    });

    /**
     * Update score for a user.
     * delta can be positive or negative.
     */
    public void updateScore(String userId, int delta) {

        int oldScore = scores.getOrDefault(userId, 0);
        int newScore = oldScore + delta;

        // If user existed before, remove old entry from TreeSet
        if (scores.containsKey(userId)) {
            sortedSet.remove(new Entry(userId, oldScore));
        }

        // Update map
        scores.put(userId, newScore);

        // Add updated entry back
        sortedSet.add(new Entry(userId, newScore));
    }

    /**
     * Return top K users by score.
     */
    public List<String> getTopK(int k) {

        List<String> result = new ArrayList<>();
        int count = 0;

        for (Entry entry : sortedSet) {
            result.add(entry.userId + " (" + entry.score + ")");
            count++;

            if (count == k) break;
        }

        return result;
    }
}

class LeaderboardTopK2 {

    private static class Entry {
        String userId;
        int score;

        Entry(String userId, int score) {
            this.userId = userId;
            this.score = score;
        }
    }

    private final Map<String, Entry> userMap = new HashMap<>();

    private final TreeSet<Entry> sortedSet = new TreeSet<>((a, b) -> {
        if (a.score != b.score) return Integer.compare(b.score, a.score);
        return a.userId.compareTo(b.userId);
    });

    public void updateScore(String userId, int delta) {

        Entry entry = userMap.get(userId);

        if (entry != null) {
            // remove old version from TreeSet
            sortedSet.remove(entry);

            // update score
            entry.score += delta;
        } else {
            entry = new Entry(userId, delta);
            userMap.put(userId, entry);
        }

        // insert updated entry
        sortedSet.add(entry);
    }

    public List<String> getTopK(int k) {

        List<String> result = new ArrayList<>();
        int count = 0;

        for (Entry entry : sortedSet) {
            result.add(entry.userId);
            count++;

            if (count == k) break;
        }

        return result;
    }
}

/*
 * updateScore(): O(log N)  (remove + add into TreeSet)
 * getTopK():     O(K)
 * Space:         O(N)
 */


/**
 * LeaderboardTopKHeap
 *
 * Optimized for:
 * - Millions of users (cannot keep all users in TreeSet)
 * - Need only Top K users at any time
 *
 * Core Idea:
 * -----------
 * Maintain only Top K users inside a minHeap.
 *
 * Challenge:
 * ----------
 * When a user's score updates, the heap may still contain old score entry.
 * Heap cannot update arbitrary element efficiently.
 *
 * Solution:
 * ---------
 * Use Lazy Deletion with Versioning.
 *
 * Each user has a version number:
 * - Every score update increments version.
 * - Heap nodes store snapshot (score, version).
 * - When we pop/peek heap, if node.version != latest version, it's stale.
 *
 * Time Complexity:
 * ---------------
 * updateScore(): O(log K) amortized
 * getTopK(): O(K log K) to return sorted output
 *
 * Space Complexity:
 * ----------------
 * O(K + N) for maps + heap
 */
class LeaderboardTopKHeap {

    private static class Node {
        String userId;
        long scoreSnapshot;
        int versionSnapshot;

        Node(String userId, long scoreSnapshot, int versionSnapshot) {
            this.userId = userId;
            this.scoreSnapshot = scoreSnapshot;
            this.versionSnapshot = versionSnapshot;
        }
    }

    private final int k;

    // userId -> latest total score
    private final Map<String, Long> scoreMap = new HashMap<>();

    // userId -> version (increments on every update)
    private final Map<String, Integer> versionMap = new HashMap<>();

    /**
     * MinHeap stores only Top K users.
     *
     * Ordering:
     * - smaller score first
     * - tie break by userId
     */
    private final PriorityQueue<Node> minHeap;

    public LeaderboardTopKHeap(int k) {
        this.k = k;

        this.minHeap = new PriorityQueue<>((a, b) -> {
            if (a.scoreSnapshot != b.scoreSnapshot) {
                return Long.compare(a.scoreSnapshot, b.scoreSnapshot); // min score first
            }
            return a.userId.compareTo(b.userId);
        });
    }

    /**
     * Update user score by delta (can be positive or negative).
     */
    public void updateScore(String userId, int delta) {

        long oldScore = scoreMap.getOrDefault(userId, 0L);
        long newScore = oldScore + delta;
        scoreMap.put(userId, newScore);

        // increment version
        int newVersion = versionMap.getOrDefault(userId, 0) + 1;
        versionMap.put(userId, newVersion);

        // push updated snapshot into heap
        minHeap.offer(new Node(userId, newScore, newVersion));

        // cleanup stale nodes at heap top
        cleanTop();

        // maintain heap size <= K
        if (minHeap.size() > k) {
            minHeap.poll();  // remove smallest from Top K
            cleanTop();
        }
    }

    /**
     * Returns Top K users by score.
     *
     * Note:
     * Heap doesn't store in sorted descending order,
     * so we copy and sort before returning.
     */
    public List<String> getTopK() {

        cleanTop();

        List<Node> list = new ArrayList<>(minHeap);

        // Sort descending by score
        list.sort((a, b) -> {
            if (a.scoreSnapshot != b.scoreSnapshot) {
                return Long.compare(b.scoreSnapshot, a.scoreSnapshot);
            }
            return a.userId.compareTo(b.userId);
        });

        List<String> result = new ArrayList<>();
        for (Node node : list) {
            result.add(node.userId + " (" + node.scoreSnapshot + ")");
        }

        return result;
    }

    /**
     * Remove stale entries from heap top.
     *
     * Stale means:
     * - heap node's version != latest version in versionMap
     * OR
     * - heap node's score != latest score in scoreMap
     *
     * We only clean the TOP because heap guarantees only top is minimum.
     *
     * This is the key "lazy deletion" trick.
     */
    private void cleanTop() {

        while (!minHeap.isEmpty()) {

            Node top = minHeap.peek();

            int latestVersion = versionMap.getOrDefault(top.userId, 0);
            long latestScore = scoreMap.getOrDefault(top.userId, 0L);

            // If this heap node is outdated, discard it
            if (top.versionSnapshot != latestVersion || top.scoreSnapshot != latestScore) {
                minHeap.poll();
            } else {
                break; // top is valid
            }
        }
    }
}

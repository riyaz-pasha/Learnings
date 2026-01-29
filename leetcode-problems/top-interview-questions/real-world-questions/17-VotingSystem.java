import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

class Leader {
}

class Vote {

    Leader leader;
    long timestamp;

}

class VotingSystem {

    private final Map<Leader, Integer> voteCounts;
    private Optional<Leader> leadingCandidate;
    private int maxVotes;

    public VotingSystem() {
        this.voteCounts = new ConcurrentHashMap<>();
        this.leadingCandidate = Optional.empty();
        this.maxVotes = 0;
    }

    public void initializeSystem(List<Leader> leaders) {
        leaders.forEach(leader -> this.voteCounts.put(leader, 0));
    }

    public void vote(Vote vote) {
        if (!this.voteCounts.containsKey(vote.leader)) {
            throw new Error("Invalid leader");
        }

        int count = this.voteCounts.getOrDefault(vote.leader, 0) + 1;
        this.voteCounts.put(vote.leader, count);

        if (count > maxVotes) {
            leadingCandidate = Optional.of(vote.leader);
            maxVotes = count;
        }

    }

    public Optional<Leader> getLeadingCandidate() {
        return this.leadingCandidate;
    }

}

class VotingSystemOneVotePerPerson {

    private static class VoteRecord {
        int timestamp;
        String leader;

        VoteRecord(int timestamp, String leader) {
            this.timestamp = timestamp;
            this.leader = leader;
        }
    }

    private final Map<String, Integer> candidateVotes = new HashMap<>();
    private final Map<String, Integer> voterTimestamps = new HashMap<>();
    private final List<VoteRecord> timeline = new ArrayList<>();
    private String currentLeader = null;
    private int maxVotes = 0;

    public boolean vote(String voterId, String candidate, int timestamp) {
        if (voterTimestamps.containsKey(voterId)) {
            return false; // This voter has already voted
        }

        voterTimestamps.put(voterId, timestamp);

        int count = candidateVotes.getOrDefault(candidate, 0) + 1;
        candidateVotes.put(candidate, count);

        if (count >= maxVotes) {
            if (!candidate.equals(currentLeader)) {
                currentLeader = candidate;
                timeline.add(new VoteRecord(timestamp, currentLeader));
            }
            maxVotes = count;
        }

        return true; // Vote accepted
    }

    public String getLeader(int timestamp) {
        int left = 0, right = timeline.size() - 1;
        String leader = null;

        while (left <= right) {
            int mid = (left + right) / 2;
            if (timeline.get(mid).timestamp <= timestamp) {
                leader = timeline.get(mid).leader;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return leader;
    }

}

class VotingSystemOneVotePerPersonTreeMap {

    private final Map<String, Integer> candidateVotes = new HashMap<>();
    private final Map<String, Integer> voterTimestamps = new HashMap<>();
    private final TreeMap<Integer, String> timeline = new TreeMap<>();
    private String currentLeader = null;
    private int maxVotes = 0;

    // Returns false if voter already voted
    public boolean vote(String voterId, String candidate, int timestamp) {
        if (voterTimestamps.containsKey(voterId)) {
            return false; // Voter already voted
        }

        voterTimestamps.put(voterId, timestamp);

        int count = candidateVotes.getOrDefault(candidate, 0) + 1;
        candidateVotes.put(candidate, count);

        if (count >= maxVotes) {
            if (!candidate.equals(currentLeader)) {
                currentLeader = candidate;
                timeline.put(timestamp, currentLeader);
            }
            maxVotes = count;
        }

        return true;
    }

    // Returns leader at or before given timestamp
    public String getLeader(int timestamp) {
        Map.Entry<Integer, String> entry = timeline.floorEntry(timestamp);
        return entry != null ? entry.getValue() : null;
    }

    public static void main(String[] args) {
        VotingSystemOneVotePerPersonTreeMap system = new VotingSystemOneVotePerPersonTreeMap();

        system.vote("voter1", "Alice", 5);
        system.vote("voter2", "Bob", 10);
        system.vote("voter3", "Alice", 15);
        system.vote("voter4", "Bob", 20);
        system.vote("voter5", "Bob", 25);

        System.out.println(system.getLeader(0)); // null (no votes)
        System.out.println(system.getLeader(5)); // Alice
        System.out.println(system.getLeader(12)); // Bob
        System.out.println(system.getLeader(18)); // Alice
        System.out.println(system.getLeader(30)); // Bob
    }

    /*
     * How this works
     * timeline.put(timestamp, leader) logs leader changes.
     * 
     * floorEntry(timestamp) efficiently finds the latest leader at or before the
     * timestamp.
     * 
     * Voter vote history still enforced via voterTimestamps.
     */

    /*
     * ## 1. **vote(voterId, candidate, timestamp)**
     * 
     * **Checking if voter already voted:**
     * `voterTimestamps.containsKey(voterId)` — O(1) average, since it's a HashMap
     * lookup.
     * 
     * **Updating voter timestamp:**
     * `voterTimestamps.put(voterId, timestamp)` — O(1) average.
     * 
     * **Updating candidate vote count:**
     * `candidateVotes.getOrDefault(candidate, 0)` and `put` — O(1) average.
     * 
     * **Checking and updating leader:**
     * 
     * Comparison: O(1)
     * Updating `timeline.put(timestamp, leader)` — **O(log N)** where N = number of
     * entries in timeline (because TreeMap is a balanced BST).
     * 
     * **Overall per vote:**
     ** O(log N)** dominated by TreeMap insertion.
     * 
     * ---
     * 
     * ## 2. **getLeader(timestamp)**
     * 
     * `timeline.floorEntry(timestamp)` — O(log N), because TreeMap does a balanced
     * tree lookup.
     * 
     * ---
     * 
     * ## 3. **Space Complexity**
     * 
     * `voterTimestamps` — stores each voter once → **O(V)** where V = total
     * distinct voters.
     * 
     * `candidateVotes` — stores counts per candidate → **O(C)** where C = total
     * distinct candidates.
     * 
     * `timeline` — stores an entry only when leader changes, at most one per vote →
     * **O(N)** where N = total votes.
     * 
     * ---
     * 
     * ## **Summary**
     * 
     * | Operation | Time Complexity | Space Complexity |
     * | --------------- | --------------- | -------------------------- |
     * | **vote()** | O(log N) | O(V + C + N) (accumulated) |
     * | **getLeader()** | O(log N) | — (no extra space) |
     * 
     * ---
     * 
     * ### Notes:
     * 
     * If votes arrive mostly in order and leader changes infrequently, timeline
     * size can be smaller than total votes.
     * 
     * HashMaps give O(1) average, but worst case can be higher (rare).
     * 
     */

}


class VotingSystem4 {
    // Candidate -> Current Votes
    private final Map<String, Integer> votes = new HashMap<>();
    
    // Votes -> Set of Candidates with that many votes
    // We use TreeMap to keep the vote counts sorted
    private final TreeMap<Integer, Set<String>> leaderboard = new TreeMap<>();

    public void vote(String candidateId) {
        int oldCount = votes.getOrDefault(candidateId, 0);
        int newCount = oldCount + 1;
        
        // 1. Update Candidate Map
        votes.put(candidateId, newCount);
        
        // 2. Update Leaderboard (TreeMap)
        if (oldCount > 0) {
            leaderboard.get(oldCount).remove(candidateId);
            if (leaderboard.get(oldCount).isEmpty()) {
                leaderboard.remove(oldCount);
            }
        }
        
        leaderboard.computeIfAbsent(newCount, k -> new HashSet<>()).add(candidateId);
    }

    public String getLeadingCandidate() {
        if (leaderboard.isEmpty()) return null;
        
        // Get the highest vote count (last entry in TreeMap)
        Map.Entry<Integer, Set<String>> topEntry = leaderboard.lastEntry();
        
        // Return any candidate from the set of leaders
        return topEntry.getValue().iterator().next();
    }
}
